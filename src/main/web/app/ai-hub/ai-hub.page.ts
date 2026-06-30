import {
  Component,
  signal,
  ChangeDetectionStrategy,
} from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-ai-hub',
  imports: [RouterOutlet],
  standalone: true,
  template: `
    <div class="flex flex-col gap-4">
      <router-outlet />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiHubPage {
  readonly zoomedImage = signal<string | null>(null);

  onImageZoom(src: string) {
    this.zoomedImage.set(src);
  }

  closeZoom() {
    this.zoomedImage.set(null);
  }
}
