import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastComponent } from '@shared/components/toast/toast.component';
import { HeaderComponent } from './header.component';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, ToastComponent, HeaderComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-toast />
    <app-header />
    <main class="main-content">
      <div class="content-wrapper">
        <router-outlet />
      </div>
    </main>
  `,
  styles: [
    `
      :host {
        display: block;
        min-height: 100vh;
        background: #f5f5f7;
      }

      .main-content {
        padding: 32px;
      }

      .content-wrapper {
        max-width: 680px;
        margin: 0 auto;
      }

      @media (max-width: 640px) {
        .main-content {
          padding: 16px;
        }
      }
    `,
  ],
})
export class MainLayoutComponent {}
