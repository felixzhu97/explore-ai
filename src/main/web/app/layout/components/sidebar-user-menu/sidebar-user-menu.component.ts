import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import {
  I18nService,
  languageNames,
  SUPPORTED_LANGUAGES,
  type Language,
} from '../../../core/i18n';
import { AccountDialogService } from '../../../account/account-dialog.service';
import { AccountService } from '../../../core/account.service';
import { ZardSidebarMenuButtonDirective } from '../../../shared/components/layout/sidebar-menu-button.directive';
import { SidebarService } from '../../sidebar.service';

@Component({
  selector: 'app-sidebar-user-menu',
  imports: [RouterLink, ZardSidebarMenuButtonDirective],
  template: `
    <div class="relative overflow-visible">
      @if (menuOpen()) {
        <div
          class="
            absolute bottom-full left-0 z-100 mb-1 w-44
            max-w-[min(11rem,calc(100vw-1rem))] rounded-lg border border-sidebar-border
            bg-sidebar p-1 shadow-md
          "
          role="menu"
          [attr.aria-label]="t().account.menuLabel"
        >
          <div class="group/help relative" [class.is-help-open]="helpPinned()">
            <button
              type="button"
              role="menuitem"
              z-sidebar-menu-button
              [class]="primaryItemClass"
              [attr.aria-expanded]="helpPinned()"
              [attr.aria-haspopup]="'menu'"
              (click)="toggleHelpPinned($event)"
            >
              <span class="opacity-60" [innerHTML]="helpIcon"></span>
              <span class="min-w-0 flex-1 truncate text-left">{{ t().account.help }}</span>
              <span class="shrink-0 opacity-50" aria-hidden="true" [innerHTML]="chevronIcon"></span>
            </button>

            <div
              class="
                pointer-events-none invisible absolute bottom-0 left-full z-110 pl-1
                opacity-0 transition-[opacity,visibility] duration-100
                group-hover/help:pointer-events-auto group-hover/help:visible
                group-hover/help:opacity-100
                group-[.is-help-open]/help:pointer-events-auto
                group-[.is-help-open]/help:visible group-[.is-help-open]/help:opacity-100
              "
              role="menu"
              [attr.aria-label]="t().account.help"
            >
              <div
                class="w-40 rounded-lg border border-sidebar-border bg-sidebar p-1 shadow-md"
              >
                <a
                  z-sidebar-menu-button
                  role="menuitem"
                  routerLink="/policies"
                  [class]="secondaryItemClass"
                  (click)="onItemClick()"
                >
                  <span class="opacity-60" [innerHTML]="legalIcon"></span>
                  <span>{{ t().nav.policies }}</span>
                </a>
                <a
                  z-sidebar-menu-button
                  role="menuitem"
                  routerLink="/privacy"
                  [class]="secondaryItemClass"
                  (click)="onItemClick()"
                >
                  <span class="opacity-60" [innerHTML]="privacyIcon"></span>
                  <span>{{ t().nav.privacy }}</span>
                </a>
              </div>
            </div>
          </div>

          <div class="group/lang relative" [class.is-lang-open]="langPinned()">
            <button
              type="button"
              role="menuitem"
              z-sidebar-menu-button
              [class]="primaryItemClass"
              [attr.aria-expanded]="langPinned()"
              [attr.aria-haspopup]="'menu'"
              (click)="toggleLangPinned($event)"
            >
              <span class="opacity-60" [innerHTML]="languageIcon"></span>
              <span class="min-w-0 flex-1 truncate text-left">{{ t().account.language }}</span>
              <span class="shrink-0 text-[11px] text-muted-foreground">
                {{ i18n.language().toUpperCase() }}
              </span>
              <span class="shrink-0 opacity-50" aria-hidden="true" [innerHTML]="chevronIcon"></span>
            </button>

            <div
              class="
                pointer-events-none invisible absolute bottom-0 left-full z-110 pl-1
                opacity-0 transition-[opacity,visibility] duration-100
                group-hover/lang:pointer-events-auto group-hover/lang:visible
                group-hover/lang:opacity-100
                group-[.is-lang-open]/lang:pointer-events-auto
                group-[.is-lang-open]/lang:visible group-[.is-lang-open]/lang:opacity-100
              "
              role="menu"
              [attr.aria-label]="t().account.language"
            >
              <div
                class="w-36 rounded-lg border border-sidebar-border bg-sidebar p-1 shadow-md"
              >
                @for (lang of supportedLanguages; track lang) {
                  <button
                    type="button"
                    role="menuitemradio"
                    z-sidebar-menu-button
                    [class]="langItemClass(lang)"
                    [attr.aria-checked]="lang === i18n.language()"
                    (click)="selectLanguage(lang)"
                  >
                    <span class="w-5 text-[11px] font-semibold opacity-70">
                      {{ lang.toUpperCase() }}
                    </span>
                    <span>{{ languageNames[lang] }}</span>
                  </button>
                }
              </div>
            </div>
          </div>

          @if (showLogin() || showLogout()) {
            <div class="my-1 border-t border-sidebar-border" role="separator"></div>
          }

          @if (showLogin()) {
            <button
              type="button"
              role="menuitem"
              z-sidebar-menu-button
              [class]="primaryItemClass"
              (click)="onLogin()"
            >
              <span class="opacity-60" [innerHTML]="loginIcon"></span>
              <span class="min-w-0 flex-1 truncate text-left">{{ t().account.login }}</span>
            </button>
          }

          @if (showLogout()) {
            <button
              type="button"
              role="menuitem"
              z-sidebar-menu-button
              [class]="primaryItemClass"
              (click)="onLogout()"
            >
              <span class="opacity-60" [innerHTML]="logoutIcon"></span>
              <span class="min-w-0 flex-1 truncate text-left">{{ t().account.logout }}</span>
            </button>
          }
        </div>
      }

      <button
        type="button"
        z-sidebar-menu-button
        [zIconOnly]="false"
        [zFull]="true"
        [class]="userTriggerClass()"
        [attr.aria-expanded]="menuOpen()"
        [attr.aria-haspopup]="'menu'"
        [attr.aria-label]="t().account.menuLabel"
        (click)="toggleMenu($event)"
      >
        <span
          class="
            flex size-8 shrink-0 items-center justify-center rounded-full
            bg-[#007AFF] text-xs font-semibold text-white
          "
          aria-hidden="true"
        >
          {{ avatarLetter() }}
        </span>
        @if (!collapsed()) {
          <span class="flex min-w-0 flex-1 flex-col items-start gap-0.5 text-left leading-snug">
            <span class="w-full truncate text-sm font-medium">{{ displayName() }}</span>
            <span class="w-full truncate text-[11px] text-muted-foreground">
              {{ planLabel() }}
            </span>
          </span>
        }
      </button>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'relative block w-full overflow-visible',
    '(document:pointerdown)': 'onDocumentPointerDown($event)',
  },
})
export class SidebarUserMenuComponent {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly sidebar = inject(SidebarService);
  private readonly accountService = inject(AccountService);
  private readonly accountDialog = inject(AccountDialogService);
  protected readonly i18n = inject(I18nService);

  readonly collapsed = input(false);

  readonly menuOpen = signal(false);
  readonly helpPinned = signal(false);
  readonly langPinned = signal(false);

  readonly account = this.accountService.account;
  readonly showLogin = this.accountService.showLogin;
  readonly showLogout = this.accountService.showLogout;

  readonly supportedLanguages = SUPPORTED_LANGUAGES;
  readonly languageNames = languageNames;

  readonly helpIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><path d="M12 17h.01"/></svg>`,
  );

  readonly loginIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" x2="3" y1="12" y2="12"/></svg>`,
  );

  readonly logoutIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" x2="9" y1="12" y2="12"/></svg>`,
  );

  readonly privacyIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/><path d="m9 12 2 2 4-4"/></svg>`,
  );

  readonly legalIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6"/><path d="M16 13H8"/><path d="M16 17H8"/><path d="M10 9H8"/></svg>`,
  );

  readonly languageIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/></svg>`,
  );

  readonly chevronIcon = this.sanitizer.bypassSecurityTrustHtml(
    `<svg class="size-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m9 18 6-6-6-6"/></svg>`,
  );

  readonly displayName = computed(() => {
    const me = this.account();
    if (me?.mode === 'authenticated') {
      const email = me.email?.trim();
      return email || this.i18n.t().account.signedIn;
    }
    return this.i18n.t().account.guest;
  });

  readonly planLabel = computed(() => {
    const plan = this.account()?.plan ?? 'free';
    const pretty = plan.charAt(0).toUpperCase() + plan.slice(1).toLowerCase();
    return this.i18n.t().account.plan.replace('{plan}', pretty);
  });

  readonly avatarLetter = computed(() => {
    const name = this.displayName();
    return (name.trim().charAt(0) || 'G').toUpperCase();
  });

  readonly primaryItemClass = '!h-8 !py-1.5 !text-xs';
  readonly secondaryItemClass = '!h-7 !gap-1.5 !px-2 !py-1 !text-xs';

  /** Override default h-8 / py-1.5 so hover background has real vertical padding. */
  readonly userTriggerClass = computed(() => this.collapsed()
    ? 'h-auto min-h-0 !size-10 !justify-center !p-1.5'
    : 'h-auto min-h-0 !h-auto !px-2.5 !py-2',
  );

  langItemClass(lang: Language): string {
    const selected = lang === this.i18n.language() ? ' bg-sidebar-accent' : '';
    return `${this.secondaryItemClass}${selected}`;
  }

  get t() {
    return this.i18n.t;
  }

  toggleMenu(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.menuOpen.update((open) => {
      const next = !open;
      if (!next) {
        this.closeSubmenus();
      }
      return next;
    });
  }

  toggleHelpPinned(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.langPinned.set(false);
    this.helpPinned.update(open => !open);
  }

  toggleLangPinned(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.helpPinned.set(false);
    this.langPinned.update(open => !open);
  }

  selectLanguage(lang: Language): void {
    this.i18n.setLanguage(lang);
    this.closeSubmenus();
    this.menuOpen.set(false);
  }

  onItemClick(): void {
    this.closeSubmenus();
    this.menuOpen.set(false);
    this.sidebar.close();
  }

  onLogin(): void {
    this.closeSubmenus();
    this.menuOpen.set(false);
    this.accountDialog.openLogin();
  }

  onLogout(): void {
    this.closeSubmenus();
    this.menuOpen.set(false);
    this.accountDialog.openLogout({
      email: this.account()?.email ?? null,
      displayName: this.displayName(),
    });
  }

  onDocumentPointerDown(event: PointerEvent): void {
    const target = event.target as Element | null;
    if (!target?.closest?.('app-sidebar-user-menu')) {
      this.menuOpen.set(false);
      this.closeSubmenus();
    }
  }

  private closeSubmenus(): void {
    this.helpPinned.set(false);
    this.langPinned.set(false);
  }
}
