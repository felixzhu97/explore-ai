import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  HostListener,
  computed,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { I18nService, languageNames, SUPPORTED_LANGUAGES, Language } from '@core/i18n';
import { SidebarService } from './sidebar.service';
import { SessionItemComponent } from './components/session-item/session-item.component';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

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
export class SidebarComponent {
  private readonly router = inject(Router);
  private readonly sanitizer = inject(DomSanitizer);
  protected readonly i18n = inject(I18nService);
  readonly sidebar = inject(SidebarService);

  readonly collapsed = this.sidebar.collapsed;
  readonly dropdownOpen = signal(false);
  readonly pinnedExpanded = signal(true);
  readonly recentsExpanded = signal(true);

  private readonly isMobile = signal(false);

  readonly sidebarClasses = computed(() => {
    const mobile = this.isMobile();
    const collapsed = this.collapsed();
    const mobileOpen = this.sidebar.mobileOpen();

    const classes: string[] = [];

    // Width
    if (mobile || !collapsed) {
      classes.push('w-[240px]');
    }
    if (!mobile && collapsed) {
      classes.push('w-[64px]');
    }

    // Mobile translate (always visible on desktop)
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
    { key: 'vision', labelKey: 'visionAI', path: '/vision' },
    { key: 'chat', labelKey: 'aiHub', path: '/chat' },
  ];

  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  readonly languageNames = languageNames;

  constructor() {
    this.updateMobileState();
  }

  private updateMobileState(): void {
    this.isMobile.set(window.innerWidth < 768);
  }

  @HostListener('window:resize')
  onResize(): void {
    this.updateMobileState();
  }

  get t() {
    return this.i18n.t;
  }

  isActiveTab(path: string): boolean {
    return this.router.url === path;
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
    this.sidebar.addSession();
  }

  onSessionSelect(sessionId: string): void {
    this.sidebar.setActiveSession(sessionId);
  }

  onSessionPin(sessionId: string): void {
    this.sidebar.togglePin(sessionId);
  }

  onSessionDelete(sessionId: string): void {
    this.sidebar.removeSession(sessionId);
  }

  getIcon(key: string): SafeHtml {
    const icons: Record<string, string> = {
      rag: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14,2 14,8 20,8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><line x1="10" y1="9" x2="8" y2="9"/></svg>`,
      vision: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>`,
      chat: `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7h1a1 1 0 0 1 1 1v3a1 1 0 0 1-1 1h-1v1a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-1H2a1 1 0 0 1-1-1v-3a1 1 0 0 1 1-1h1a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/><circle cx="8" cy="14" r="1"/><circle cx="16" cy="14" r="1"/></svg>`,
    };
    const iconSvg = icons[key] || `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>`;
    return this.sanitizer.bypassSecurityTrustHtml(iconSvg);
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.relative')) {
      this.dropdownOpen.set(false);
    }
  }
}
