import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  standalone: true,
  template: `<router-outlet />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block h-dvh' },
})
export class AppComponent {}
