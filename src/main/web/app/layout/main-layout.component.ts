import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from '@shared/components/toast/toast.component';
import { SidebarComponent } from './sidebar.component';
import { HeaderComponent } from './header.component';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, ToastComponent, SidebarComponent, HeaderComponent],
  standalone: true,
  template: `
      <app-toast />
      <app-sidebar />
      <app-header (openSidebar)="openSidebar()" />
      <main class="
        ml-60 flex min-h-screen justify-center transition-all duration-250
        md:ml-60
      ">
          <router-outlet/>
      </main>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'min-h-screen bg-gray-100',
  },
})
export class MainLayoutComponent {
  openSidebar(): void {
    // Sidebar is managed by SidebarService
  }
}
