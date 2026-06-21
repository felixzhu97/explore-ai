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
    <main class="p-8 max-sm:p-4">
      <div class="max-w-[680px] mx-auto">
        <router-outlet />
      </div>
    </main>
  `,
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
      background: #f5f5f7;
    }
  `],
})
export class MainLayoutComponent {}
