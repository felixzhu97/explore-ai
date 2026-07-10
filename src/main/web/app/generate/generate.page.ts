import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { I18nService } from '@core/i18n';
import { SegmentedControlComponent } from '@shared/components/ui/segmented-control/segmented-control.component';

type GenerateTab = 'image' | 'tts';

@Component({
  selector: 'app-generate',
  imports: [RouterOutlet, SegmentedControlComponent],
  template: `
    <div class="flex items-center justify-center border-b border-black/8 bg-white px-4 py-2.5">
      <app-segmented-control
        [options]="tabOptions()"
        [value]="activeTab()"
        (changed)="onTabChange($event)"
      />
    </div>
    <div class="flex-1 overflow-y-auto bg-surface px-4 py-6">
      <router-outlet />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex h-screen flex-col h-full w-full' },
})
export class GeneratePage {
  private readonly router = inject(Router);
  protected readonly i18n = inject(I18nService);

  private readonly currentPath = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(event => event.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  readonly activeTab = computed<GenerateTab>(() => this.currentPath().includes('/tts') ? 'tts' : 'image',
  );

  readonly tabOptions = computed(() => {
    const tabs = this.i18n.t().generate.tabs;
    return [
      { value: 'image' as GenerateTab, label: tabs.image },
      { value: 'tts' as GenerateTab, label: tabs.tts },
    ];
  });

  onTabChange(tab: GenerateTab): void {
    void this.router.navigate(['/generate', tab]);
  }
}
