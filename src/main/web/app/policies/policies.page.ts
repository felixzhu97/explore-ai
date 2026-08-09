import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { map } from 'rxjs/operators';
import { I18nService } from '../core/i18n';
import {
  policiesHubCopy,
  policyDocCopy,
  resolvePolicySlug,
} from './policies.page.copy';

@Component({
  selector: 'app-policies-page',
  imports: [RouterLink],
  template: `
    <div class="mx-auto w-full max-w-(--container-2xl) px-6 py-12 md:py-16">
      @if (doc(); as d) {
        <article>
          <p class="text-[13px] font-medium text-[#8F8F8F]">{{ d.updated }}</p>
          <h1
            class="
              mt-3 text-[2rem] leading-tight font-medium tracking-[-0.03em] text-[#0D0D0D]
              md:text-[2.5rem]
            "
          >
            {{ d.title }}
          </h1>
          <p class="mt-4 text-[15px] leading-relaxed text-[#5C5C5C]">{{ d.subtitle }}</p>

          <div class="mt-12 space-y-10">
            @for (section of d.sections; track section.heading) {
              <section>
                <h2 class="text-xl leading-snug font-medium tracking-[-0.02em] text-[#0D0D0D]">
                  {{ section.heading }}
                </h2>
                @for (paragraph of section.paragraphs; track $index) {
                  <p class="mt-3 text-[15px] leading-relaxed text-[#0D0D0D]/80">
                    {{ paragraph }}
                  </p>
                }
              </section>
            }
          </div>
        </article>
      } @else {
        <header class="w-full max-w-(--container-xl)">
          <h1
            class="
              text-[2rem] leading-tight font-medium tracking-[-0.03em] text-[#0D0D0D]
              md:text-[2.75rem]
            "
          >
            {{ hub().title }}
          </h1>
          <p class="mt-4 text-[15px] leading-relaxed text-[#5C5C5C]">
            {{ hub().indexHint }}
          </p>
        </header>

        <nav class="mt-12" [attr.aria-label]="hub().title">
          <ul class="divide-y divide-black/8 border-y border-black/8">
            @for (link of hub().links; track link.slug) {
              <li>
                <a
                  class="
                    group flex flex-col gap-1 py-5 no-underline transition-opacity
                    hover:opacity-70
                  "
                  [routerLink]="['/policies', link.slug]"
                >
                  <span
                    class="
                      text-[17px] font-medium tracking-[-0.01em] text-[#0D0D0D]
                      underline-offset-4 group-hover:underline
                    "
                  >
                    {{ link.label }}
                  </span>
                  <span class="text-[14px] leading-relaxed text-[#8F8F8F]">
                    {{ link.summary }}
                  </span>
                </a>
              </li>
            }
            <li>
              <a
                class="
                  group flex flex-col gap-1 py-5 no-underline transition-opacity
                  hover:opacity-70
                "
                routerLink="/privacy"
              >
                <span
                  class="
                    text-[17px] font-medium tracking-[-0.01em] text-[#0D0D0D]
                    underline-offset-4 group-hover:underline
                  "
                >
                  {{ hub().controlsLink }}
                </span>
                <span class="text-[14px] leading-relaxed text-[#8F8F8F]">
                  {{ hub().controlsSummary }}
                </span>
              </a>
            </li>
          </ul>
        </nav>

        <p class="mt-10 text-[13px] leading-relaxed text-[#8F8F8F]">
          {{ hub().disclaimer }}
        </p>
      }

      <footer class="mt-14 flex flex-wrap gap-x-6 gap-y-2 border-t border-black/8 pt-6">
        <a
          routerLink="/chat"
          class="text-[13px] text-[#8F8F8F] no-underline underline-offset-4 hover:text-[#0D0D0D] hover:underline"
        >
          {{ hub().backToChat }}
        </a>
        <a
          routerLink="/privacy"
          class="text-[13px] text-[#8F8F8F] no-underline underline-offset-4 hover:text-[#0D0D0D] hover:underline"
        >
          {{ hub().controlsLink }}
        </a>
        @if (doc()) {
          <a
            routerLink="/policies"
            class="text-[13px] text-[#8F8F8F] no-underline underline-offset-4 hover:text-[#0D0D0D] hover:underline"
          >
            {{ hub().backToHub }}
          </a>
        }
      </footer>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'flex min-h-0 w-full flex-1 flex-col overflow-y-auto bg-white',
  },
})
export class PoliciesPage {
  private readonly i18n = inject(I18nService);
  private readonly route = inject(ActivatedRoute);

  private readonly rawSlug = toSignal(
    this.route.paramMap.pipe(map(params => params.get('slug'))),
    { initialValue: null as string | null },
  );

  readonly hub = computed(() => policiesHubCopy(this.i18n.language()));

  readonly doc = computed(() => {
    const slug = resolvePolicySlug(this.rawSlug());
    return slug ? policyDocCopy(slug, this.i18n.language()) : null;
  });
}
