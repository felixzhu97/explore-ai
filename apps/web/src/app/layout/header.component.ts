import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  HostListener,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { I18nService, languageNames, SUPPORTED_LANGUAGES, Language } from '@core/i18n';

interface NavTab {
  key: string;
  labelKey: keyof import('@core/i18n').Translations['nav'];
  path: string;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <nav class="sticky top-0 z-[100] h-[52px] bg-white/80 backdrop-blur-xl border-b border-black/8 flex items-center">
      <div class="w-full max-w-[960px] mx-auto px-8 flex items-center justify-between max-sm:px-4">
        <div class="text-[17px] font-semibold text-text flex items-center">AI</div>

        <div class="flex gap-0.5 max-sm:gap-0">
          @for (tab of tabs; track tab.key) {
            <a
              class="px-4 py-2 text-sm font-medium font-inherit border-none cursor-pointer transition-colors duration-200 text-text-secondary bg-transparent no-underline max-sm:px-2.5 max-sm:text-xs"
              [class.text-primary]="isActiveTab(tab.path)"
              [class.text-text]="!isActiveTab(tab.path)"
              [routerLink]="tab.path"
              [title]="t().nav[tab.labelKey]"
            >
              {{ t().nav[tab.labelKey] }}
            </a>
          }
        </div>

        <div class="relative flex justify-end">
          <button
            class="flex items-center gap-1 px-3 py-1.5 text-xs font-inherit text-text-secondary bg-transparent border-none cursor-pointer transition-colors duration-200 rounded-md hover:text-text"
            (click)="toggleDropdown()"
            [attr.aria-expanded]="dropdownOpen()"
          >
            {{ i18n.languageName() }}
            <span class="inline-block w-0 h-0 border-l-4 border-r-4 border-t-4 border-l-transparent border-r-transparent transition-transform duration-200" [class]="dropdownOpen() ? 'rotate-180' : ''" [style.border-top-color]="'currentColor'"></span>
          </button>
          @if (dropdownOpen()) {
            <div class="absolute top-full right-0 mt-1 min-w-[140px] bg-surface border border-[--color-border] rounded-xl shadow-elevated overflow-hidden animate-[dropdownFadeIn_0.15s_ease]">
              @for (lang of supportedLanguages; track lang) {
                <button
                  class="w-full px-4 py-2.5 text-xs font-inherit text-left text-text bg-transparent border-none cursor-pointer transition-colors duration-150 hover:bg-white/50"
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
    </nav>
  `,
  styles: [`
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
  `],
})
export class HeaderComponent {
  protected readonly i18n = inject(I18nService);
  private readonly router = inject(Router);

  readonly tabs: NavTab[] = [
    { key: 'ai-infra', labelKey: 'aiinfra', path: '/ai-infra' },
    { key: 'rag', labelKey: 'documentQA', path: '/rag' },
    { key: 'vision', labelKey: 'visionAI', path: '/vision' },
    { key: 'aihubs', labelKey: 'aiHub', path: '/aihubs' },
  ];

  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  readonly languageNames = languageNames;

  readonly dropdownOpen = signal(false);

  get t() {
    return this.i18n.t;
  }

  isActiveTab(path: string): boolean {
    return this.router.url === path;
  }

  toggleDropdown(): void {
    this.dropdownOpen.update((v) => !v);
  }

  selectLanguage(lang: Language): void {
    this.i18n.setLanguage(lang);
    this.dropdownOpen.set(false);
  }

  @HostListener('document:mousedown', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.relative')) {
      this.dropdownOpen.set(false);
    }
  }
}
