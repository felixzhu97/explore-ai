import {
  Component,
  computed,
  input,
  linkedSignal,
  model,
  output,
} from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { ZardButtonComponent } from '../ui/button';
import { ZardCardComponent } from '../ui/card';
import { ZardInputDirective } from '../ui/input';
import { ZardSegmentedComponent } from '../ui/segmented';
import type { ImageSize } from './image.service';

@Component({
  selector: 'app-image-gen-form',
  imports: [
    FormField,
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
            rows="4"
            [formField]="promptField"
            [placeholder]="promptPlaceholder()"
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
            [formField]="sizeLabelField"
            (zChange)="onSizeLabelChange($event)"
          />
        </div>

        <button
          type="button"
          z-button
          zFull
          zSize="lg"
          [zDisabled]="prompt().trim() === '' || isGenerating()"
          (click)="generateRequested.emit()"
        >
          @if (isGenerating()) {
            {{ generatingLabel() }}
          } @else {
            {{ generateLabel() }}
          }
        </button>
      </div>
    </z-card>
  `,
})
export class ImageGenFormComponent {
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly promptLabel = input.required<string>();
  readonly promptPlaceholder = input.required<string>();
  readonly sizeLabel = input.required<string>();
  readonly generateLabel = input.required<string>();
  readonly generatingLabel = input.required<string>();
  readonly prompt = model.required<string>();
  readonly sizes = input.required<ImageSize[]>();
  readonly selectedSize = input.required<ImageSize>();
  readonly isGenerating = input(false);
  readonly sizeSelected = output<ImageSize>();
  readonly generateRequested = output<void>();

  protected readonly promptField = form(this.prompt);

  readonly sizeOptions = computed(() => this.sizes().map(size => ({
    value: size.label,
    label: size.label,
  })));

  readonly #sizeLabel = linkedSignal(() => this.selectedSize().label);
  protected readonly sizeLabelField = form(this.#sizeLabel);

  onSizeLabelChange(label: string): void {
    const size = this.sizes().find(item => item.label === label);
    if (size !== undefined) {
      this.sizeSelected.emit(size);
    }
  }
}
