import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { Router } from '@angular/router';
import { I18nService } from '@core/i18n';
import { EmptyStateComponent } from '../components/empty-state/empty-state.component';

@Component({
  selector: 'app-hub-dashboard',
  imports: [EmptyStateComponent],
  standalone: true,
  template: `
    <div class="animate-page-enter">
      <app-empty-state
        [title]="t().aiHub.emptyState.title"
        [message]="t().aiHub.emptyState.message"
        [showCta]="true"
        [ctaLabel]="t().aiHub.emptyState.getStarted"
        (ctaClick)="goToChat()"
      />
    </div>
  `,
  styles: [
    `
      @keyframes pageEnter {
        from {
          opacity: 0;
          transform: translateY(12px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      .animate-page-enter {
        animation: pageEnter 0.35s cubic-bezier(0.4, 0, 0.2, 1) forwards;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HubDashboardComponent {
  private readonly router = inject(Router);
  protected readonly i18n = inject(I18nService);

  get t() {
    return this.i18n.t;
  }

  goToChat(): void {
    this.router.navigateByUrl('/ai-hubs/chat');
  }
}
