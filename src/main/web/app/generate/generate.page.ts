import { Component, inject, computed, linkedSignal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { I18nService } from '../i18n';
import { ZardSegmentedComponent } from '../ui/segmented';

type GenerateTab = 'image' | 'tts';

@Component({
  selector: 'app-generate-page',
  imports: [RouterOutlet, FormField, ZardSegmentedComponent],
  template: `
    <div class="flex items-center justify-center border-b border-black/8 bg-white px-4 py-2.5">
      <z-segmented
        [zOptions]="tabOptions()"
        [formField]="tabField"
        (zChange)="onTabChange($event)"
      />
    </div>
    <div class="flex-1 overflow-x-hidden overflow-y-auto bg-surface px-4 py-6">
      <router-outlet />
    </div>
  `,
  host: { class: 'flex h-full min-h-0 w-full min-w-0 flex-col' },
})
export class GeneratePageComponent {
  readonly #router = inject(Router);
  protected readonly i18n = inject(I18nService);

  readonly tabOptions = computed(() => {
    const tabs = this.i18n.t().generate.tabs;
    return [
      { value: 'image', label: tabs.image },
      { value: 'tts', label: tabs.tts },
    ];
  });

  readonly #currentPath = toSignal(
    this.#router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(event => event.urlAfterRedirects),
    ),
    { initialValue: this.#router.url },
  );

  readonly activeTab = computed<GenerateTab>(() => this.#currentPath().includes('/tts') ? 'tts' : 'image',
  );

  readonly #selectedTab = linkedSignal(() => this.activeTab());
  protected readonly tabField = form(this.#selectedTab);

  /** Navigates to the selected tab. */
  onTabChange(value: string): void {
    const tab: GenerateTab = value === 'tts' ? 'tts' : 'image';
    void this.#router.navigate(['/generate', tab]);
  }
}
