import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

export interface DomainHealthItem {
  domain: string;
  label: string;
  status: string;
  detail: string;
}

@Component({
  selector: 'app-metrics-domain-health',
  template: `
    <div class="flex flex-col gap-2">
      <h3 class="text-sm font-medium text-foreground">{{ heading() }}</h3>
      <div class="overflow-hidden rounded-xl border border-black/10">
        @for (item of items(); track item.domain) {
          <button
            type="button"
            class="flex w-full items-center justify-between gap-3 border-b border-black/10 px-4 py-3 text-left last:border-b-0 hover:bg-accent"
            (click)="domainClick.emit(item.domain)"
          >
            <div>
              <p class="text-sm font-medium text-foreground">{{ item.label }}</p>
              <p class="text-xs text-muted-foreground">{{ item.detail }}</p>
            </div>
            <span
              class="text-xs font-medium tracking-wide uppercase"
              [class.text-emerald-600]="item.status === 'UP'"
              [class.text-amber-600]="item.status !== 'UP'"
            >
              {{ item.status }}
            </span>
          </button>
        } @empty {
          <p class="px-4 py-6 text-center text-sm text-muted-foreground">{{ emptyText() }}</p>
        }
      </div>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MetricsDomainHealthComponent {
  readonly items = input<DomainHealthItem[]>([]);
  readonly heading = input('Domain health');
  readonly emptyText = input('No domain health data');
  readonly domainClick = output<string>();
}
