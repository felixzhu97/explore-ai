import { Component, ChangeDetectionStrategy, inject, input, viewChild } from '@angular/core';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideChevronDown } from '@ng-icons/lucide';
import { I18nService, languageNames, SUPPORTED_LANGUAGES, type Language } from '@core/i18n';
import { ZardButtonComponent } from '@/shared/components/button';
import { ZardDropdownMenuComponent } from '@/shared/components/dropdown/dropdown.component';
import { ZardDropdownImports } from '@/shared/components/dropdown/dropdown.imports';

@Component({
  selector: 'app-language-picker',
  imports: [NgIcon, ZardButtonComponent, ...ZardDropdownImports],
  template: `
    <z-dropdown-menu #dropdownMenu="zDropdownMenu">
      <button
        dropdown-trigger
        type="button"
        z-button
        zType="ghost"
        [class]="triggerClass()"
        aria-label="Language"
      >
        <span class="font-semibold">{{ i18n.language().toUpperCase() }}</span>
        @if (showLabel()) {
          <span class="flex-1 truncate text-left">{{ i18n.languageName() }}</span>
        }
        @if (showChevron()) {
          <ng-icon name="lucideChevronDown" class="size-3.5 opacity-60" />
        }
      </button>

      @for (lang of supportedLanguages; track lang) {
        <z-dropdown-menu-item
          [class.font-medium]="lang === i18n.language()"
          [class.bg-sidebar-accent]="lang === i18n.language()"
          [class.text-sidebar-accent-foreground]="lang === i18n.language()"
          (click)="selectLanguage(lang)"
        >
          {{ languageNames[lang] }}
        </z-dropdown-menu-item>
      }
    </z-dropdown-menu>
  `,
  providers: [provideIcons({ lucideChevronDown })],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguagePickerComponent {
  protected readonly i18n = inject(I18nService);
  private readonly dropdownMenu = viewChild.required(ZardDropdownMenuComponent);

  readonly showLabel = input(true);
  readonly showChevron = input(true);
  readonly fullWidth = input(false);

  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  readonly languageNames = languageNames;

  triggerClass(): string {
    return this.fullWidth()
      ? 'h-auto w-full justify-start gap-2 px-3 py-2 text-xs'
      : 'gap-1 px-2.5 py-1.5 text-xs';
  }

  selectLanguage(lang: Language): void {
    this.i18n.setLanguage(lang);
    this.dropdownMenu().close();
  }
}
