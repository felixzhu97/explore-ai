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
  template: `
    <aside
      class="fixed left-0 top-0 h-screen bg-surface border-r border-border z-90 overflow-hidden transition-all duration-250 ease-out"
      [ngClass]="sidebarClasses()"
    >
      <div class="flex items-center justify-between px-4 h-[52px] border-b border-border-light">
        <span class="text-[17px] font-semibold text-text whitespace-nowrap overflow-hidden">
          {{ collapsed() ? 'AI' : 'AI Explore' }}
        </span>
        <button
          type="button"
          class="hidden md:flex items-center justify-center w-6 h-6 rounded-md text-text-secondary bg-transparent border-none cursor-pointer transition-all duration-150 hover:bg-surface-secondary hover:text-text flex-shrink-0"
          (click)="toggleCollapse()"
          [attr.aria-label]="collapsed() ? 'Expand sidebar' : 'Collapse sidebar'"
        >
          <svg
            class="w-4 h-4 transition-transform duration-250"
            [class.rotate-180]="collapsed()"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
          >
            <path d="M15 18l-6-6 6-6" />
          </svg>
        </button>
        <button
          type="button"
          class="flex md:hidden items-center justify-center w-6 h-6 rounded-md text-text-secondary bg-transparent border-none cursor-pointer transition-all duration-150 hover:bg-surface-secondary hover:text-text flex-shrink-0"
          (click)="sidebar.close()"
          aria-label="Close menu"
        >
          ✕
        </button>
      </div>

      <nav class="flex-1 px-2 py-2 overflow-y-auto overflow-x-hidden">
        @for (tab of tabs; track tab.key) {
          <a
            class="flex items-center gap-3 px-3 py-2.5 rounded-lg text-text-secondary no-underline text-sm font-medium transition-all duration-150 whitespace-nowrap mb-0.5
                   hover:bg-surface-secondary hover:text-text"
            [class.bg-primary-light]="isActiveTab(tab.path)"
            [class.text-primary]="isActiveTab(tab.path)"
            [routerLink]="tab.path"
            [title]="t().nav[tab.labelKey]"
            (click)="onNavClick()"
          >
            <span class="text-base w-6 text-center flex-shrink-0">{{ getIcon(tab.key) }}</span>
            @if (!collapsed()) {
              <span class="overflow-hidden text-ellipsis">
                {{ t().nav[tab.labelKey] }}
              </span>
            }
          </a>
        }
      </nav>

      <div class="px-2 py-2 border-t border-border-light">
        <div class="relative" [class.collapsed]="collapsed()">
          <button
            type="button"
            class="flex items-center gap-2 w-full px-3 py-2 rounded-lg text-text-secondary bg-transparent border-none cursor-pointer text-[13px] font-medium transition-all duration-150
                   hover:bg-surface-secondary hover:text-text"
            (click)="toggleDropdown()"
            [attr.aria-expanded]="dropdownOpen()"
            aria-haspopup="listbox"
          >
            <span class="w-6 text-center font-semibold flex-shrink-0">{{ i18n.language().toUpperCase() }}</span>
            @if (!collapsed()) {
              <span class="flex-1 text-left overflow-hidden text-ellipsis">
                {{ i18n.languageName() }}
              </span>
              <span
                class="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-t-[4px] border-t-current transition-transform duration-200"
                [class.rotate-180]="dropdownOpen()"
              ></span>
            }
          </button>
          @if (dropdownOpen() && !collapsed()) {
            <div class="absolute bottom-full left-0 right-0 mb-1 bg-surface border border-border rounded-lg shadow-elevated overflow-hidden animate-dropdown-fade-in">
              @for (lang of supportedLanguages; track lang) {
                <button
                  type="button"
                  class="block w-full px-3 py-2 text-left text-[13px] text-text-secondary bg-transparent border-none cursor-pointer transition-all duration-150
                         hover:bg-surface-secondary hover:text-text"
                  [class.text-primary]="lang === i18n.language()"
                  [class.bg-primary-light]="lang === i18n.language()"
                  (click)="selectLanguage(lang)"
                >
                  {{ languageNames[lang] }}
                </button>
              }
            </div>
          }
        </div>
      </div>
    </aside>

    <div
      class="fixed inset-0 bg-black/40 z-85 opacity-0 pointer-events-none transition-opacity duration-250"
      [class.md:opacity-100]="sidebar.mobileOpen()"
      [class.md:pointer-events-auto]="sidebar.mobileOpen()"
      [class.opacity-100]="sidebar.mobileOpen()"
      [class.pointer-events-auto]="sidebar.mobileOpen()"
      (click)="sidebar.close()"
      (keydown.escape)="sidebar.close()"
      tabindex="-1"
      role="button"
      aria-label="Close overlay"
    ></div>
  `,
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
