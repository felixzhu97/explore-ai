import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from '@shared/components/toast/toast.component';
import { SidebarComponent } from './sidebar.component';
import { HeaderComponent } from './header.component';
import { SidebarService } from './sidebar.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, ToastComponent, SidebarComponent, HeaderComponent],
  standalone: true,
  template: `
      <app-toast />
      <app-sidebar />
      <app-header (openSidebar)="openSidebar()" />
      <main class="flex min-h-0 w-full min-w-0 flex-1 flex-col transition-all duration-250"
            [class.md:pl-60]="!sidebar.collapsed()"
            [class.md:pl-16]="sidebar.collapsed()"
      >
          <router-outlet/>
      </main>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class:
      'flex min-h-dvh flex-col overflow-x-hidden bg-gray-100 max-md:h-dvh max-md:max-h-dvh md:min-h-screen',
  },
})
export class MainLayoutComponent {
  readonly sidebar = inject(SidebarService);

  openSidebar(): void {
    this.sidebar.open();
  }
}
