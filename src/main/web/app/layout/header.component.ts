import { Component, ChangeDetectionStrategy, Output, EventEmitter, inject, signal } from '@angular/core';
import { I18nService, languageNames, SUPPORTED_LANGUAGES, Language } from '@core/i18n';

@Component({
  selector: 'app-header',
  standalone: true,
  template: `
    <header class="flex md:hidden items-center justify-between h-[52px] px-4 bg-white border-b border-black/8 sticky top-0 z-80">
      <button class="flex flex-col gap-[5px] p-2 bg-transparent border-none cursor-pointer" (click)="openSidebar.emit()" aria-label="Open menu">
        <span class="w-[18px] h-[2px] bg-text rounded-[1px] transition-all duration-200"></span>
        <span class="w-[18px] h-[2px] bg-text rounded-[1px] transition-all duration-200"></span>
        <span class="w-[18px] h-[2px] bg-text rounded-[1px] transition-all duration-200"></span>
      </button>
      <div class="text-[17px] font-semibold text-text">AI</div>
      <div class="relative">
        <button class="flex items-center gap-1 px-[10px] py-[6px] rounded-md text-xs font-semibold text-text-secondary bg-transparent border-none cursor-pointer" (click)="toggleDropdown()" [attr.aria-expanded]="dropdownOpen()">
          {{ i18n.language().toUpperCase() }}
          <span class="w-0 h-0 border-l-[4px] border-l-transparent border-r-[4px] border-r-transparent border-t-[4px] border-t-current transition-transform duration-200" [class.rotate-180]="dropdownOpen()"></span>
        </button>
        @if (dropdownOpen()) {
          <div class="absolute top-full right-0 mt-1 min-w-[140px] bg-white border border-black/8 rounded-lg shadow-elevated overflow-hidden animate-[dropdownFadeIn_0.15s_ease]">
            @for (lang of supportedLanguages; track lang) {
              <button
                class="w-full px-3 py-2 text-xs text-left text-text-secondary bg-transparent border-none cursor-pointer transition-colors duration-150 hover:bg-white/50"
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
    </header>
  `,
  styles: [`
    @keyframes dropdownFadeIn {
      from {
        opacity: 0;
        transform: translateY(-4px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeaderComponent {
  @Output() openSidebar = new EventEmitter<void>();

  protected readonly i18n = inject(I18nService);
  readonly dropdownOpen = signal(false);

  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  readonly languageNames = languageNames;

  toggleDropdown(): void {
    this.dropdownOpen.update((v: boolean) => !v);
  }

  selectLanguage(lang: Language): void {
    this.i18n.setLanguage(lang);
    this.dropdownOpen.set(false);
  }
}
