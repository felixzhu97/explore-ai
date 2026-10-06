import {
  Component,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map, startWith } from 'rxjs';
import type { SafeHtml } from '@angular/platform-browser';
import { I18nService } from '../i18n';
import type {
  ModuleNavSection,
  ModuleNavTab,
} from './module-nav.config';
import { ZardSidebarMenuButtonDirective } from '../ui/layout/sidebar-menu-button.directive';

@Component({
  selector: 'app-sidebar-more-menu',
  imports: [RouterLink, ZardSidebarMenuButtonDirective],
  template: `
    <div
      class="group/more relative overflow-visible"
      data-sidebar-more
      [class.is-more-open]="isPinned()"
    >
      <button
        type="button"
        z-sidebar-menu-button
        [zIconOnly]="isCollapsed()"
        [zFull]="!isCollapsed()"
        [zActive]="moreActive()"
        [class]="getTriggerClass()"
        [attr.aria-expanded]="isPinned()"
        [attr.aria-haspopup]="'menu'"
        [attr.aria-label]="t().nav.more"
        [title]="t().nav.more"
        (click)="togglePinned($event)"
      >
        <span class="size-3.5 shrink-0" aria-hidden="true">
          <svg
            class="size-3.5"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="12" cy="12" r="1" />
            <circle cx="19" cy="12" r="1" />
            <circle cx="5" cy="12" r="1" />
          </svg>
        </span>
        @if (!isCollapsed()) {
          <span class="min-w-0 flex-1 overflow-hidden text-left text-ellipsis">
            {{ t().nav.more }}
          </span>
        }
      </button>

      <div
        class="
          pointer-events-none invisible absolute top-0 left-full z-110 -translate-x-1
          pl-1 opacity-0
          transition-[opacity,visibility,transform] duration-200 ease-out
          group-hover/more:pointer-events-auto group-hover/more:visible
          group-hover/more:translate-x-0 group-hover/more:opacity-100
          group-[.is-more-open]/more:pointer-events-auto
          group-[.is-more-open]/more:visible
          group-[.is-more-open]/more:translate-x-0 group-[.is-more-open]/more:opacity-100
        "
        role="menu"
        [attr.aria-label]="t().nav.more"
      >
        <div
          class="
            w-48 max-w-[min(12rem,calc(100vw-1rem))] rounded-lg border border-sidebar-border
            bg-sidebar p-1 shadow-md
          "
        >
          @for (tab of flatTabs(); track tab.key) {
            <a
              z-sidebar-menu-button
              role="menuitem"
              [routerLink]="tab.path"
              [zActive]="isTabActive(tab.path)"
              [class]="itemClass"
              [title]="t().nav[tab.labelKey]"
              (click)="onNavigate()"
            >
              <span class="size-3.5 shrink-0" [innerHTML]="iconFor()(tab.key)"></span>
              <span class="min-w-0 flex-1 truncate text-left">
                {{ t().nav[tab.labelKey] }}
              </span>
            </a>
          }
        </div>
      </div>
    </div>
  `,
  host: {
    class: 'relative block overflow-visible',
    '[hidden]': 'sections().length === 0',
    '(document:pointerdown)': 'onDocumentPointerDown($event)',
  },
})
export class SidebarMoreMenuComponent {
  readonly #i18n = inject(I18nService);
  readonly #router = inject(Router);

  readonly sections = input.required<ModuleNavSection[]>();
  readonly isCollapsed = input(false);
  readonly iconFor = input.required<(key: string) => SafeHtml>();
  readonly navigated = output<void>();

  readonly isPinned = signal(false);

  readonly flatTabs = computed<ModuleNavTab[]>(() => {
    return this.sections().flatMap(section => section.tabs);
  });

  readonly moreActive = computed(() => {
    return this.flatTabs().some(tab => this.isTabActive(tab.path));
  });

  get t() {
    return this.#i18n.t;
  }

  readonly itemClass = `
    !h-auto min-h-8 !items-center gap-1.5 !rounded-md !px-2 !py-1.5
  `;

  readonly #currentUrl = toSignal(
    this.#router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map(() => this.#router.url),
      startWith(this.#router.url),
    ),
    { initialValue: this.#router.url },
  );

  getTriggerClass(): string {
    return this.isCollapsed()
      ? '!size-7 !justify-center'
      : '!h-auto min-h-8 !items-center gap-1.5 !rounded-md !px-2 !py-1.5';
  }

  togglePinned(event: MouseEvent): void {
    event.stopPropagation();
    this.isPinned.update(open => !open);
  }

  isTabActive(path: string): boolean {
    const [url = ''] = this.#currentUrl().split('?');
    return url === path || url.startsWith(`${path}/`);
  }

  onNavigate(): void {
    this.isPinned.set(false);
    this.navigated.emit();
  }

  onDocumentPointerDown(event: PointerEvent): void {
    if (!this.isPinned()) {
      return;
    }
    if ((event.target as Element).closest('[data-sidebar-more]') === null) {
      this.isPinned.set(false);
    }
  }
}
