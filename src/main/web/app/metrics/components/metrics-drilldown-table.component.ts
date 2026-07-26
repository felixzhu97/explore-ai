import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import type { InvocationEvent } from '../metrics.model';

@Component({
  selector: 'app-metrics-drilldown-table',
  imports: [DatePipe],
  template: `
    <div class="flex flex-col gap-2">
      <div class="flex items-center justify-between gap-2">
        <h3 class="text-sm font-medium text-foreground">{{ heading() }}</h3>
        <p class="text-xs text-muted-foreground">{{ total() }} events</p>
      </div>
      @if (items().length === 0) {
        <p class="rounded-xl border border-border px-4 py-10 text-center text-sm text-muted-foreground">
          {{ emptyText() }}
        </p>
      } @else {
        <div class="overflow-x-auto rounded-xl border border-border">
          <table class="min-w-full text-left text-sm">
            <thead class="bg-card text-xs tracking-wide text-muted-foreground uppercase">
              <tr>
                <th class="px-3 py-2 font-medium">Time</th>
                <th class="px-3 py-2 font-medium">Operation</th>
                <th class="px-3 py-2 font-medium">Outcome</th>
                <th class="px-3 py-2 font-medium">Latency</th>
                <th class="px-3 py-2 font-medium">Model</th>
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
  readonly items = input<InvocationEvent[]>([]);
  readonly total = input(0);
  readonly heading = input('Invocation events');
  readonly emptyText = input('No invocation events for this filter');
  readonly rowClick = output<InvocationEvent>();
}
