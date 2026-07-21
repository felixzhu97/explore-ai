import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { I18nService } from '../core/i18n';
import { ZardSegmentedComponent } from '../shared/components/segmented';

type GenerateTab = 'image' | 'tts';

@Component({
  selector: 'app-generate',
  imports: [RouterOutlet, FormsModule, ZardSegmentedComponent],
  template: `
    <div class="flex items-center justify-center border-b border-black/8 bg-white px-4 py-2.5">
      <z-segmented
        [zOptions]="tabOptions()"
        [ngModel]="activeTab()"
        (ngModelChange)="onTabChange($event)"
      />
    </div>
    <div class="flex-1 overflow-x-hidden overflow-y-auto bg-surface px-4 py-6">
      <router-outlet />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex h-full min-h-0 w-full min-w-0 flex-col' },
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
