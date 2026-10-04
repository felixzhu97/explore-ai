import { ChangeDetectionStrategy, Component, computed, input, ViewEncapsulation } from '@angular/core';

import type { ClassValue } from 'clsx';

import { mergeClasses } from '../../utils/merge-classes';

import { sliderRangeVariants } from './slider.variants';

@Component({
  selector: 'z-slider-range',
  imports: [],
  standalone: true,
  template: `
    <span
      data-slot="slider-range"
      [attr.data-orientation]="orientation()"
      [class]="classes()"
      [style.left]="orientation() === 'horizontal' ? '0' : null"
      [style.right]="orientation() === 'horizontal' ? 100 - percent() + '%' : null"
      [style.bottom]="orientation() === 'vertical' ? '0' : null"
      [style.top]="orientation() === 'vertical' ? 100 - percent() + '%' : null"
    ></span>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
})
export class ZardSliderRangeComponent {
  readonly percent = input(0);

  readonly orientation = input<'horizontal' | 'vertical'>('horizontal');
  readonly class = input<ClassValue>('');

  protected readonly classes = computed(() => mergeClasses(
    sliderRangeVariants({ zOrientation: this.orientation() }),
    this.class(),
  ),
  );
}
