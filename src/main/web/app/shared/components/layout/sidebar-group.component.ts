import { ChangeDetectionStrategy, Component, computed, input, ViewEncapsulation } from '@angular/core';

import type { ClassValue } from 'clsx';

import { mergeClasses } from '../../utils/merge-classes';
import { sidebarGroupVariants } from './layout.variants';

@Component({
  selector: 'z-sidebar-group',
  template: `
    <div [class]="classes()">
      <ng-content />
    </div>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  encapsulation: ViewEncapsulation.None,
  exportAs: 'zSidebarGroup',
})
export class ZardSidebarGroupComponent {
  readonly class = input<ClassValue>('');

  protected readonly classes = computed(() => {
    return mergeClasses(sidebarGroupVariants(), this.class());
  });
}
