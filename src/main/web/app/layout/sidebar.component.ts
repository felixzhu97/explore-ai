import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  HostListener,
  computed,
  OnInit,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { I18nService, languageNames, SUPPORTED_LANGUAGES, Language } from '@core/i18n';
import { SidebarService } from './sidebar.service';
import { SessionItemComponent } from './components/session-item/session-item.component';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { ChatService } from '../ai-hub/chat.service';
import type { Session } from './sidebar.service';

interface NavTab {
  key: string;
  labelKey: keyof import('@core/i18n').Translations['nav'];
  path: string;
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, CommonModule, SessionItemComponent],
  standalone: true,
  templateUrl: './sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  protected readonly i18n = inject(I18nService);
  readonly sidebar = inject(SidebarService);
  readonly chatService = inject(ChatService);

  readonly collapsed = this.sidebar.collapsed;
  readonly dropdownOpen = signal(false);
  readonly recentsExpanded = signal(true);

  private readonly isMobile = signal(false);

  readonly displaySessions = computed<Session[]>(() => this.toDisplaySessions());

  private toDisplaySessions(): Session[] {
    return this.chatService.sessions().map(session => ({
      id: session.sessionId,
      title: session.title,
      timestamp: new Date(session.lastActivityAt),
      pinned: false,
    }));
  }

  readonly sidebarClasses = computed(() => {
    const mobile = this.isMobile();
    const collapsed = this.collapsed();
    const mobileOpen = this.sidebar.mobileOpen();

    const classes: string[] = [];

    if (mobile) {
      classes.push('w-[min(88vw,20rem)]');
    } else if (!collapsed) {
      classes.push('w-[240px]');
    }
    if (!mobile && collapsed) {
      classes.push('w-[64px]');
    }

    if (!mobile) {
      classes.push('translate-x-0');
    } else if (mobileOpen) {
      classes.push('translate-x-0');
    } else {
      classes.push('-translate-x-full');
    }

    return classes.join(' ');
  });

  readonly tabs: NavTab[] = [
    { key: 'rag', labelKey: 'documentQA', path: '/rag' },
    { key: 'vision', labelKey: 'imageAnalysis', path: '/vision' },
    { key: 'chat', labelKey: 'chat', path: '/chat' },
    { key: 'generate', labelKey: 'generation', path: '/generate' },
  ];

  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  readonly languageNames = languageNames;

  constructor() {
    this.updateMobileState();
  }

  ngOnInit(): void {
    this.chatService.initializeSessions();
  }

  private updateMobileState(): void {
    const mobile = window.innerWidth < 768;
    this.isMobile.set(mobile);
    if (mobile) {
      this.sidebar.collapsed.set(false);
    }
  }

  @HostListener('window:resize')
  onResize(): void {
    this.updateMobileState();
  }

  get t() {
    return this.i18n.t;
  }

  isActiveTab(path: string): boolean {
    const url = this.router.url.split('?')[0];
    return url === path || url.startsWith(`${path}/`);
  }

  toggleCollapse(): void {
    if (!this.isMobile()) {
      this.collapsed.update(v => !v);
    }
  }

  onNavClick(): void {
    this.sidebar.close();
  }

  toggleDropdown(): void {
    this.dropdownOpen.update(v => !v);
  }

  selectLanguage(lang: Language): void {
    this.i18n.setLanguage(lang);
    this.dropdownOpen.set(false);
  }

  newChat(): void {
    this.chatService.createSession();
    if (this.router.url !== '/chat') {
      void this.router.navigate(['/chat']);
    }
    this.sidebar.close();
  }

  onSessionSelect(sessionId: string): void {
    this.chatService.selectSession(sessionId);
    if (this.router.url !== '/chat') {
      void this.router.navigate(['/chat']);
    }
    this.sidebar.close();
  }

  onSessionPin(): void {
    // Pin is client-only; server sessions use recents list for now
  }

  onSessionDelete(sessionId: string): void {
    this.chatService.deleteSession(sessionId);
  }

  getIcon(key: string): SafeHtml {
    const icons: Record<string, string> = {
      rag: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14,2 14,8 20,8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>`,
      vision: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>`,
      chat: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>`,
      generate: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"/><path d="M12 2v4M12 18v4M2 12h4M18 12h4"/></svg>`,
    };
    const iconSvg = icons[key] || `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>`;
    return this.sanitizer.bypassSecurityTrustHtml(iconSvg);
  }

  @HostListener('document:pointerdown', ['$event'])
  onDocumentPointerDown(event: PointerEvent): void {
    const target = event.target as Element;
    const isOutsideMenu =
      !target || typeof target.closest !== 'function' || !target.closest('[data-language-menu]');
    const isOutsideSidebar =
      !target || typeof target.closest !== 'function' || !target.closest('[data-sidebar-panel]');

    if (this.dropdownOpen() && isOutsideMenu) {
      this.dropdownOpen.set(false);
    }

    if (this.sidebar.mobileOpen() && isOutsideSidebar) {
      this.sidebar.close();
    }
  }
}
