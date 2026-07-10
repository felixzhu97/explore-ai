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
      <main class="flex min-h-screen w-full flex-1 transition-all duration-250"
            [class.md:ml-60]="!sidebar.collapsed()"
            [class.md:ml-16]="sidebar.collapsed()"
      >
          <router-outlet/>
      </main>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'min-h-screen bg-gray-100',
  },
})
export class MainLayoutComponent {
  readonly sidebar = inject(SidebarService);

  openSidebar(): void {
    this.sidebar.open();
  }
}
