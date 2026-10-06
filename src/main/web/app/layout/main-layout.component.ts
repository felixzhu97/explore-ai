import { Component, inject, type OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ZardToastComponent } from '../ui/toast';
import { AppSidebarComponent } from './sidebar.component';
import { AppHeaderComponent } from './header.component';
import { SidebarService } from './sidebar.service';
import { ConsentBannerComponent } from '../privacy/consent-banner.component';
import { AccountService } from '../account/account.service';

@Component({
  selector: 'app-main-layout',
  imports: [
    RouterOutlet,
    ZardToastComponent,
    AppSidebarComponent,
    AppHeaderComponent,
    ConsentBannerComponent,
  ],
  template: `
      <z-toaster position="top-right" [richColors]="true" [closeButton]="true" />
      <app-sidebar />
      <app-header (sidebarOpenRequested)="openSidebar()" />
      <main
        class="flex min-h-0 w-full min-w-0 flex-1 flex-col overflow-hidden transition-all duration-250"
        [class.md:pl-[240px]]="!sidebar.isCollapsed()"
        [class.md:pl-16]="sidebar.isCollapsed()"
      >
          <router-outlet/>
      </main>
      <app-consent-banner />
    `,
  host: {
    class:
      'flex h-dvh min-h-0 flex-col overflow-x-hidden bg-gray-100 max-md:max-h-dvh max-md:overflow-hidden',
  },
})
export class MainLayoutComponent implements OnInit {
  readonly #account = inject(AccountService);
  readonly sidebar = inject(SidebarService);

  ngOnInit(): void {
    this.#account.consumeLoginReturn();
  }

  /** Opens the sidebar. */
  openSidebar(): void {
    this.sidebar.open();
  }
}
