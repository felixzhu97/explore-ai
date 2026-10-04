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

import { mergeClasses } from '../../utils/merge-classes';

import { sliderTrackVariants } from './slider.variants';

@Component({
  selector: 'z-slider-track',
  imports: [],
  standalone: true,
  template: `
    <span #track data-slot="slider-track" [attr.data-orientation]="orientation()" [class]="classes()">
      <ng-content />
    </span>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  host: {
    '[class]': '"data-[orientation=horizontal]:w-full data-[orientation=vertical]:h-full"',
    '[attr.data-orientation]': 'orientation()',
  },
})
export class ZardSliderTrackComponent {
  readonly orientation = input<'horizontal' | 'vertical'>('horizontal');
  readonly class = input<ClassValue>('');

  protected readonly classes = computed(() => mergeClasses(
    sliderTrackVariants({ zOrientation: this.orientation() }),
    this.class(),
  ),
  );

  private readonly trackEl = viewChild.required<ElementRef<HTMLElement>>('track');

  get nativeElement(): HTMLElement {
    return this.trackEl().nativeElement;
  }
}
