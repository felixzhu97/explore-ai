import { ChangeDetectionStrategy, Component, computed, input, ViewEncapsulation } from '@angular/core';

import type { ClassValue } from 'clsx';

import { mergeClasses } from '../../utils/merge-classes';
import { sidebarGroupLabelVariants } from './layout.variants';

@Component({
  selector: 'z-sidebar-group-label',
  template: `
    <div [class]="classes()">
      <ng-content />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  exportAs: 'zSidebarGroupLabel',
})
export class ZardSidebarGroupLabelComponent {
  readonly class = input<ClassValue>('');

  protected readonly classes = computed(() => {
    return mergeClasses(sidebarGroupLabelVariants(), this.class());
  });
}
