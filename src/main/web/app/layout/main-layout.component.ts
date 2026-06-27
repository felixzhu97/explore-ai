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
    <div class="min-h-screen bg-[#f5f5f7]">
      <app-toast />
      <app-sidebar />
      <app-header (openSidebar)="openSidebar()" />
      <main class="ml-60 min-h-screen transition-all duration-250 md:ml-60">
        <router-outlet />
      </main>
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  openSidebar() {}
}
