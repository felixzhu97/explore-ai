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

interface NavTab {
  key: string;
  labelKey: keyof import('@core/i18n').Translations['nav'];
  path: string;
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, CommonModule],
  standalone: true,
  templateUrl: './sidebar.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  private readonly router = inject(Router);
  protected readonly i18n = inject(I18nService);
  readonly sidebar = inject(SidebarService);

  readonly collapsed = signal(false);
  readonly dropdownOpen = signal(false);

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
    { key: 'ai-infra', labelKey: 'aiinfra', path: '/ai-infra' },
    { key: 'rag', labelKey: 'documentQA', path: '/rag' },
    { key: 'vision', labelKey: 'visionAI', path: '/vision' },
    { key: 'ai-hubs', labelKey: 'aiHub', path: '/ai-hubs' },
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

  getIcon(key: string): string {
    const icons: Record<string, string> = {
      'ai-infra': '⚙️',
      rag: '📄',
      vision: '👁️',
      'ai-hubs': '🧠',
    };
    return icons[key] || '📋';
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.relative')) {
      this.dropdownOpen.set(false);
    }
  }
}
