import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs/operators';
import { I18nService } from '../core/i18n';
import { LegalDocId, legalDocCopy, legalHubCopy } from './legal.page.copy';

@Component({
  selector: 'app-legal-page',
  imports: [RouterLink],
  template: `
    <div class="mx-auto flex w-full max-w-3xl flex-col gap-8">
      <header>
        <p class="text-xs tracking-[0.16em] text-muted-foreground uppercase">ExploreAI</p>
        <h1 class="mt-1 text-2xl font-semibold tracking-tight">{{ title() }}</h1>
        <p class="mt-1 text-sm text-muted-foreground">{{ subtitle() }}</p>
        @if (doc(); as d) {
          <p class="mt-2 text-xs text-muted-foreground">{{ d.updated }}</p>
        }
      </header>

      @if (doc(); as d) {
        <div class="space-y-5">
          @for (section of d.sections; track section.heading) {
            <section class="rounded-xl border border-black/8 bg-background p-5">
              <h2 class="text-base font-semibold">{{ section.heading }}</h2>
              <p class="mt-2 text-sm leading-relaxed text-muted-foreground">{{ section.body }}</p>
            </section>
          }
        </div>
      } @else {
        <nav class="rounded-xl border border-black/8 bg-background p-5">
          <ul class="space-y-3 text-sm">
            @for (link of hub().links; track link.id) {
              <li>
                <a
                  class="font-medium underline underline-offset-2"
                  [routerLink]="['/legal', link.id]"
                >
                  {{ link.label }}
                </a>
              </li>
            }
          </ul>
        </nav>
      }

      <p class="flex flex-wrap gap-4 text-xs text-muted-foreground">
        <a routerLink="/chat" class="underline underline-offset-2">{{ hub().backToChat }}</a>
        <a routerLink="/privacy" class="underline underline-offset-2">{{ hub().controlsLink }}</a>
        @if (docId()) {
          <a routerLink="/legal" class="underline underline-offset-2">{{ hub().title }}</a>
        }
      </p>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6',
  },
})
export class LegalPage {
  private readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);

  protected readonly docId = toSignal(
    this.route.paramMap.pipe(map(params => params.get('doc') as LegalDocId | null)),
    { initialValue: null },
  );

  readonly hub = computed(() => legalHubCopy(this.i18n.language()));
  readonly doc = computed(() => {
    const id = this.docId();
    if (!id || !['terms', 'privacy', 'cookies', 'subprocessors'].includes(id)) {
      return null;
    }
    return legalDocCopy(id);
  });

  readonly title = computed(() => this.doc()?.title ?? this.hub().title);
  readonly subtitle = computed(() => this.doc()?.subtitle ?? this.hub().subtitle);
}
