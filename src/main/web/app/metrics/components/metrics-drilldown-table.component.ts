import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output } from '@angular/core';
import { I18nService } from '../../core/i18n';
import type { InvocationEvent } from '../metrics.model';

@Component({
  selector: 'app-metrics-drilldown-table',
  imports: [DatePipe],
  template: `
    <div class="flex flex-col gap-2">
      <div class="flex items-center justify-between gap-2">
        <h3 class="text-sm font-medium text-foreground">{{ heading() }}</h3>
        <p class="text-xs text-muted-foreground">{{ eventsCountLabel() }}</p>
      </div>
      @if (items().length === 0) {
        <p class="rounded-xl border border-border px-4 py-10 text-center text-sm text-muted-foreground">
          {{ emptyText() || i18n.t().metricsPage.drilldown.empty }}
        </p>
      } @else {
        <div class="overflow-x-auto rounded-xl border border-border">
          <table class="min-w-full text-left text-sm">
            <thead class="bg-card text-xs tracking-wide text-muted-foreground uppercase">
              <tr>
                <th class="px-3 py-2 font-medium">{{ i18n.t().metricsPage.drilldown.time }}</th>
                <th class="px-3 py-2 font-medium">{{ i18n.t().metricsPage.drilldown.operation }}</th>
                <th class="px-3 py-2 font-medium">{{ i18n.t().metricsPage.drilldown.outcome }}</th>
                <th class="px-3 py-2 font-medium">{{ i18n.t().metricsPage.drilldown.latency }}</th>
                <th class="px-3 py-2 font-medium">{{ i18n.t().metricsPage.drilldown.model }}</th>
              </tr>
            </thead>
            <tbody>
              @for (item of items(); track item.id) {
                <tr
                  class="cursor-pointer border-t border-border hover:bg-accent"
                  (click)="rowClick.emit(item)"
                >
                  <td class="px-3 py-2 whitespace-nowrap text-muted-foreground">
                    {{ item.occurredAt | date: 'yyyy-MM-dd HH:mm:ss' }}
                  </td>
                  <td class="px-3 py-2 text-foreground">{{ item.operation }}</td>
                  <td
                    class="px-3 py-2"
                    [class.text-emerald-600]="item.outcome === 'success'"
                    [class.text-rose-600]="item.outcome !== 'success'"
                  >
                    {{ item.outcome }}
                  </td>
                  <td class="px-3 py-2 text-muted-foreground">
                    {{ item.latencyMs }} ms
                  </td>
                  <td class="px-3 py-2 text-muted-foreground">
                    {{ item.model || item.agentType || item.toolName || '—' }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MetricsDrilldownTableComponent {
  protected readonly i18n = inject(I18nService);

  readonly items = input<InvocationEvent[]>([]);
  readonly total = input(0);
  readonly heading = input('');
  readonly emptyText = input('');
  readonly rowClick = output<InvocationEvent>();

  readonly eventsCountLabel = computed(() => {
    const template = this.i18n.t().metricsPage.drilldown.eventsCount;
    return this.i18n.tReplace(template, { total: this.total() });
  });
}
