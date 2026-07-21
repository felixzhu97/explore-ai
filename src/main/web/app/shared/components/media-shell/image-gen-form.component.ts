import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ZardButtonComponent } from '../button';
import { ZardCardComponent } from '../card';
import { ZardInputDirective } from '../input';
import { ZardSegmentedComponent } from '../segmented';
import type { ImageSize } from '../../../generate/image/image.model';

@Component({
  selector: 'app-image-gen-form',
  imports: [
    FormsModule,
    ZardButtonComponent,
    ZardCardComponent,
    ZardInputDirective,
    ZardSegmentedComponent,
  ],
  template: `
    <z-card
      class="gap-4 py-4 shadow-card"
      [zTitle]="title()"
      [zDescription]="description()"
    >
      <div class="flex flex-col gap-4">
        <div class="flex flex-col gap-1.5">
          <label class="text-sm font-medium text-muted-foreground" for="prompt-input">
            {{ promptLabel() }}
          </label>
          <textarea
            id="prompt-input"
            z-input
            class="min-h-24 resize-y"
            [ngModel]="prompt()"
            (ngModelChange)="promptChange.emit($event)"
            [placeholder]="promptPlaceholder()"
            rows="4"
          ></textarea>
        </div>

        <div class="flex flex-col gap-1.5">
          <span class="text-sm font-medium text-muted-foreground" aria-hidden="true">
            {{ sizeLabel() }}
          </span>
          <z-segmented
            zSize="sm"
            class="h-auto flex-wrap"
            [zOptions]="sizeOptions()"
            [zAriaLabel]="sizeLabel()"
            [ngModel]="selectedSize().label"
            (ngModelChange)="onSizeLabelChange($event)"
          />
        </div>

        <button
          type="button"
          z-button
          zFull
          zSize="lg"
          [zDisabled]="!prompt().trim() || generating()"
          (click)="generateRequested.emit()"
        >
          @if (generating()) {
            {{ generatingLabel() }}
          } @else {
            {{ generateLabel() }}
          }
        </button>
      </div>
    </z-card>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageGenFormComponent {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly promptLabel = input.required<string>();
  readonly promptPlaceholder = input.required<string>();
  readonly sizeLabel = input.required<string>();
  readonly generateLabel = input.required<string>();
  readonly generatingLabel = input.required<string>();
  readonly prompt = input.required<string>();
  readonly sizes = input.required<ImageSize[]>();
  readonly selectedSize = input.required<ImageSize>();
  readonly generating = input(false);

  readonly promptChange = output<string>();
  readonly sizeSelected = output<ImageSize>();
  readonly generateRequested = output<void>();

  readonly sizeOptions = computed(() => this.sizes().map(size => ({
    value: size.label,
    label: size.label,
  })));

  onSizeLabelChange(label: string): void {
    const size = this.sizes().find(item => item.label === label);
    if (size) {
      this.sizeSelected.emit(size);
    }
  }
}
