import { Component, ChangeDetectionStrategy, inject, computed } from '@angular/core';
import { Router } from '@angular/router';
import { I18nService } from '@core/i18n';
import { FeatureCardComponent, FeatureCardData } from '../components/feature-card/feature-card.component';
import { EmptyStateComponent } from '../components/empty-state/empty-state.component';

@Component({
  selector: 'app-hub-dashboard',
  imports: [FeatureCardComponent, EmptyStateComponent],
  standalone: true,
  template: `
    <div class="animate-page-enter">
      @if (features().length > 0) {
        <div class="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
          @for (feature of features(); track feature.id) {
            <app-feature-card
              [data]="feature"
              (selected)="onFeatureSelect($event)"
            />
          }
        </div>
      } @else {
        <app-empty-state
          [title]="t().aiHub.emptyState.title"
          [message]="t().aiHub.emptyState.message"
          [showCta]="true"
          [ctaLabel]="t().aiHub.emptyState.getStarted"
          (ctaClick)="navigateToDefault()"
        />
      }
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

  readonly features = computed<FeatureCardData[]>(() => {
    const cards = this.t().aiHub.cards;
    return [
      {
        id: 'chat',
        title: cards.chat.title,
        description: cards.chat.description,
        icon: 'chat',
        route: '/ai-hubs/chat',
      },
      {
        id: 'image',
        title: cards.image.title,
        description: cards.image.description,
        icon: 'image',
        route: '/ai-hubs/image',
      },
      {
        id: 'tts',
        title: cards.tts.title,
        description: cards.tts.description,
        icon: 'audio',
        route: '/ai-hubs/tts',
      },
    ];
  });

  onFeatureSelect(id: FeatureCardData['id']): void {
    const routes: Record<FeatureCardData['id'], string> = {
      chat: '/ai-hubs/chat',
      image: '/ai-hubs/image',
      tts: '/ai-hubs/tts',
    };
    this.router.navigateByUrl(routes[id]);
  }

  navigateToDefault(): void {
    this.router.navigateByUrl('/ai-hubs/chat');
  }
}
