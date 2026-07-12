import { Component, ChangeDetectionStrategy, inject, signal, output } from '@angular/core';
import { I18nService, languageNames, SUPPORTED_LANGUAGES, Language } from '@core/i18n';

@Component({
  selector: 'app-header',
  template: `
    <header class="
      sticky top-0 z-80 flex h-13 shrink-0 items-center justify-between border-b
      border-black/8 bg-white px-4
      md:hidden
    ">
      <button
        type="button"
        class="
          flex cursor-pointer flex-col gap-1 border-none bg-transparent p-2
        "
        (click)="openSidebar.emit()"
        aria-label="Open menu"
      >
        <span class="
          h-px w-4.5 rounded-sm bg-text transition-all duration-200
        "></span>
        <span class="
          h-px w-4.5 rounded-sm bg-text transition-all duration-200
        "></span>
        <span class="
          h-px w-4.5 rounded-sm bg-text transition-all duration-200
        "></span>
      </button>
      <div class="text-lg font-semibold text-text">AI</div>
      <div class="relative" data-language-menu>
        <button
          type="button"
          class="
            flex cursor-pointer items-center gap-1 rounded-md border-none
            bg-transparent px-2.5 py-1.5 text-xs font-semibold
            text-text-secondary
          "
          (click)="toggleDropdown()"
          [attr.aria-expanded]="dropdownOpen()"
          aria-haspopup="listbox"
        >
          {{ i18n.language().toUpperCase() }}
          <span class="
            size-0 border-x-4 border-t-4 border-x-transparent border-t-current
            transition-transform duration-200
          " [class.rotate-180]="dropdownOpen()"></span>
        </button>
        @if (dropdownOpen()) {
          <div class="
            absolute top-full right-0 mt-1 min-w-36
            animate-dropdown-fade-in overflow-hidden rounded-lg border
            border-black/8 bg-white shadow-elevated
          ">
            @for (lang of supportedLanguages; track lang) {
              <button
                type="button"
                class="
                  w-full cursor-pointer border-none bg-transparent px-3 py-2
                  text-left text-xs text-text-secondary transition-colors
                  duration-150
                  hover:bg-white/50
                "
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
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:pointerdown)': 'onDocumentPointerDown($event)',
  },
})
export class HeaderComponent {
  readonly openSidebar = output<void>();

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

  onDocumentPointerDown(event: PointerEvent): void {
    if (!this.dropdownOpen()) return;
    const target = event.target as Element;
    if (!target || typeof target.closest !== 'function' || !target.closest('[data-language-menu]')) {
      this.dropdownOpen.set(false);
    }
  }
}
