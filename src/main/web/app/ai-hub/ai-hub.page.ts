import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-ai-hub',
  imports: [RouterOutlet],
  standalone: true,
  template: `
    <router-outlet />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'h-full w-full' },
})
export class AiHubPage {}
