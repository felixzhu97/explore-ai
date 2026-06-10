import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  HostListener,
  ElementRef,
} from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { I18nService, languageNames, SUPPORTED_LANGUAGES, Language, Translations } from './i18n';

interface Tab {
  key: string;
  labelKey: keyof Translations['nav'];
  path: string;
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-container">
      <nav class="navbar">
        <div class="nav-content">
          <div class="logo">AI</div>

          <div class="nav-tabs">
            @for (tab of tabs; track tab.key) {
              <a
                class="nav-tab"
                [class.active]="isActiveTab(tab.path)"
                [routerLink]="tab.path"
                [title]="t().nav[tab.labelKey]"
              >
                {{ t().nav[tab.labelKey] }}
              </a>
            }
          </div>

          <div class="language-selector" #dropdownRef>
            <button
              class="language-button"
              (click)="toggleDropdown()"
              [attr.aria-expanded]="dropdownOpen()"
            >
              {{ i18n.languageName() }}
              <span class="chevron" [class.open]="dropdownOpen()"></span>
            </button>
            @if (dropdownOpen()) {
              <div class="dropdown">
                @for (lang of supportedLanguages; track lang) {
                  <button
                    class="dropdown-item"
                    [class.active]="lang === i18n.language()"
                    (click)="selectLanguage(lang)"
                  >
                    {{ languageNames[lang] }}
                  </button>
                }
              </div>
            }
          </div>
        </div>
      </nav>

      <main class="main-content">
        <div class="content-wrapper">
          <router-outlet />
        </div>
      </main>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      background: #f5f5f7;
    }

    .navbar {
      position: sticky;
      top: 0;
      z-index: 100;
      height: 52px;
      background: rgba(255, 255, 255, 0.8);
      backdrop-filter: blur(20px) saturate(180%);
      -webkit-backdrop-filter: blur(20px) saturate(180%);
      border-bottom: 1px solid rgba(0, 0, 0, 0.08);
      display: flex;
      align-items: center;
    }

    .nav-content {
      width: 100%;
      max-width: 960px;
      margin: 0 auto;
      padding: 0 32px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .logo {
      font-size: 17px;
      font-weight: 600;
      color: #1d1d1f;
      display: flex;
      align-items: center;
    }

    .nav-tabs {
      display: flex;
      gap: 2px;
    }

    .nav-tab {
      padding: 8px 16px;
      font-size: 14px;
      font-weight: 500;
      font-family: inherit;
      border: none;
      cursor: pointer;
      transition: color 0.2s ease;
      color: #86868b;
      background: transparent;
      text-decoration: none;
    }

    .nav-tab:hover {
      color: #1d1d1f;
    }

    .nav-tab.active {
      color: #007aff;
    }

    .language-selector {
      position: relative;
      display: flex;
      justify-content: flex-end;
    }

    .language-button {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 6px 12px;
      font-size: 12px;
      font-family: inherit;
      color: #86868b;
      background: transparent;
      border: none;
      cursor: pointer;
      transition: color 0.2s ease;
      border-radius: 6px;
    }

    .language-button:hover {
      color: #1d1d1f;
    }

    .chevron {
      display: inline-block;
      width: 0;
      height: 0;
      border-left: 4px solid transparent;
      border-right: 4px solid transparent;
      border-top: 4px solid currentColor;
      transition: transform 0.2s ease;
    }

    .chevron.open {
      transform: rotate(180deg);
    }

    .dropdown {
      position: absolute;
      top: 100%;
      right: 0;
      margin-top: 4px;
      min-width: 140px;
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: 10px;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
      overflow: hidden;
      animation: dropdownFadeIn 0.15s ease;
    }

    @keyframes dropdownFadeIn {
      from {
        opacity: 0;
        transform: translateY(-8px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .dropdown-item {
      width: 100%;
      padding: 10px 16px;
      font-size: 12px;
      font-family: inherit;
      text-align: left;
      color: #1d1d1f;
      background: transparent;
      border: none;
      cursor: pointer;
      transition: background 0.15s ease;
    }

    .dropdown-item:hover {
      background: rgba(255, 255, 255, 0.54);
    }

    .dropdown-item.active {
      color: #007aff;
      background: rgba(0, 122, 255, 0.12);
    }

    .dropdown-item.active:hover {
      background: rgba(0, 122, 255, 0.12);
    }

    .main-content {
      padding: 32px;
    }

    .content-wrapper {
      max-width: 680px;
      margin: 0 auto;
    }

    @media (max-width: 640px) {
      .nav-content {
        padding: 0 16px;
      }

      .nav-tabs {
        gap: 0;
      }

      .nav-tab {
        padding: 8px 10px;
        font-size: 12px;
      }

      .main-content {
        padding: 16px;
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {
  readonly i18n = inject(I18nService);
  readonly router = inject(Router);

  readonly tabs: Tab[] = [
    { key: 'ai-infra', labelKey: 'aiinfra', path: '/ai-infra' },
    { key: 'rag', labelKey: 'documentQA', path: '/rag' },
    { key: 'vision', labelKey: 'visionAI', path: '/vision' },
    { key: 'aihubs', labelKey: 'aiHub', path: '/aihubs' },
  ];

  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  readonly languageNames = languageNames;

  readonly dropdownOpen = signal(false);
  private dropdownRef: ElementRef | null = null;

  get t() {
    return this.i18n.t;
  }

  isActiveTab(path: string): boolean {
    return this.router.url === path;
  }

  toggleDropdown(): void {
    this.dropdownOpen.update(v => !v);
  }

  selectLanguage(lang: Language): void {
    this.i18n.setLanguage(lang);
    this.dropdownOpen.set(false);
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.language-selector')) {
      this.dropdownOpen.set(false);
    }
  }
}
