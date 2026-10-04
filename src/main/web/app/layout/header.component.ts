import { Component, ChangeDetectionStrategy, inject, output } from '@angular/core';
import { ZardButtonComponent } from '../ui/button';
import { LanguagePickerComponent } from './language-picker.component';
import { I18nService } from '../i18n';

@Component({
  selector: 'app-header',
  imports: [ZardButtonComponent, LanguagePickerComponent],
  template: `
    <header class="
      sticky top-0 z-80 flex h-13 shrink-0 items-center justify-between border-b
      border-black/8 bg-white px-4
      md:hidden
    ">
      <button
        type="button"
        z-button
        zType="ghost"
        zSize="icon"
        class="flex flex-col gap-1 p-2"
        [attr.aria-label]="i18n.t().sidebar.openMenu"
        (click)="sidebarOpenRequested.emit()"
      >
        <span class="h-px w-4.5 rounded-sm bg-text"></span>
        <span class="h-px w-4.5 rounded-sm bg-text"></span>
        <span class="h-px w-4.5 rounded-sm bg-text"></span>
      </button>
      <div class="text-lg font-semibold text-text">AI</div>
      <app-language-picker [showLabel]="false" />
    </header>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppHeaderComponent {
  protected readonly i18n = inject(I18nService);
  readonly sidebarOpenRequested = output<void>();
}
