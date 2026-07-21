import {
  booleanAttribute,
  computed,
  Directive,
  input,
} from '@angular/core';
import type { ClassValue } from 'clsx';

import { mergeClasses } from '../../utils/merge-classes';

import { sidebarMenuButtonVariants } from './sidebar-menu-button.variants';

@Directive({
  selector: 'button[z-sidebar-menu-button], a[z-sidebar-menu-button]',
  host: {
    '[class]': 'classes()',
    '[attr.data-active]': 'zActive() ? true : null',
  },
})
export class ZardSidebarMenuButtonDirective {
  readonly zActive = input(false, { transform: booleanAttribute });
  readonly zIconOnly = input(false, { transform: booleanAttribute });
  readonly zFull = input(true, { transform: booleanAttribute });
  readonly class = input<ClassValue>('');

  protected readonly classes = computed(() => mergeClasses(
    sidebarMenuButtonVariants({
      zIconOnly: this.zIconOnly(),
      zFull: this.zFull(),
    }),
    this.class(),
  ),
  );
}
