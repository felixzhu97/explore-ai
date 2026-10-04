import {
  ChangeDetectionStrategy,
  Component,
  computed,
  type ElementRef,
  input,
  viewChild,
  ViewEncapsulation,
} from '@angular/core';

import type { ClassValue } from 'clsx';

import { mergeClasses } from '../merge-classes';

import { sliderOrientationVariants, sliderThumbVariants } from './slider.variants';

@Component({
  selector: 'z-slider-thumb',
  imports: [],
  standalone: true,
  template: `
    <span
      #thumb
      data-slot="slider-thumb"
      [attr.role]="'slider'"
      [attr.aria-valuemin]="min()"
      [attr.aria-valuemax]="max()"
      [attr.aria-valuenow]="value()"
      [attr.aria-disabled]="disabled() ? true : null"
      [class]="classes()"
      tabindex="0"
    ></span>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  host: {
    '[class]': 'orientationClasses()',
    '[style.left]': 'orientation() === "horizontal" ? "calc(" + percent() + "% + " + offset() + "px)" : null',
    '[style.bottom]': 'orientation() === "vertical" ? "calc(" + percent() + "% + " + offset() + "px)" : null',
  },
})
export class ZardSliderThumbComponent {
  readonly value = input(0);
  readonly min = input(0);
  readonly max = input(100);
  readonly disabled = input(false);
  readonly percent = input(0);
  readonly offset = input(0);

  readonly orientation = input<'horizontal' | 'vertical'>('horizontal');
  readonly class = input<ClassValue>('');

  protected readonly classes = computed(() => {
    return mergeClasses(
      sliderThumbVariants({ disabled: this.disabled() }),
      this.class(),
    );
  });

  protected readonly orientationClasses = computed(() => {
    return mergeClasses(
      sliderOrientationVariants({ zOrientation: this.orientation() }),
    );
  });

  private readonly thumbEl = viewChild.required<ElementRef<HTMLElement>>('thumb');

  get nativeElement(): HTMLElement {
    return this.thumbEl().nativeElement;
  }
}
