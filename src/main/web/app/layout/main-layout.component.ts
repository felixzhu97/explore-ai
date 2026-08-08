import { Component, ChangeDetectionStrategy, inject, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ZardToastComponent } from '../shared/components/toast';
import { SidebarComponent } from './sidebar.component';
import { HeaderComponent } from './header.component';
import { SidebarService } from './sidebar.service';
import { ConsentBannerComponent } from '../privacy/consent-banner.component';
import { AccountService } from '../core/account.service';

@Component({
  selector: 'app-main-layout',
  imports: [
    RouterOutlet,
    ZardToastComponent,
    SidebarComponent,
    HeaderComponent,
    ConsentBannerComponent,
  ],
  template: `
      <z-toaster position="top-right" [richColors]="true" [closeButton]="true" />
      <app-sidebar />
      <app-header (openSidebar)="openSidebar()" />
      <main
        class="flex min-h-0 w-full min-w-0 flex-1 flex-col overflow-hidden transition-all duration-250"
        [class.md:pl-[240px]]="!sidebar.collapsed()"
        [class.md:pl-16]="sidebar.collapsed()"
      >
          <router-outlet/>
      </main>
      <app-consent-banner />
    `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class:
      'flex h-dvh min-h-0 flex-col overflow-x-hidden bg-gray-100 max-md:max-h-dvh max-md:overflow-hidden',
  },
})
export class MainLayoutComponent implements OnInit {
  readonly sidebar = inject(SidebarService);
  private readonly account = inject(AccountService);

  ngOnInit(): void {
    this.account.consumeLoginReturn();
  }

  openSidebar(): void {
    this.sidebar.open();
  }
}
