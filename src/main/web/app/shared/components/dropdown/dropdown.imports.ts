import { ZardDropdownItemComponent } from './dropdown-item.component';
import { ZardDropdownMenuContentComponent } from './dropdown-menu-content.component';
import { ZardDropdownTriggerDirective } from './dropdown-trigger.directive';
import { ZardDropdownComponent } from './dropdown.component';
import { ZardMenuLabelComponent } from '../menu/menu-label.component';

export const ZardDropdownImports = [
  ZardDropdownComponent,
  ZardDropdownItemComponent,
  ZardMenuLabelComponent,
  ZardDropdownMenuContentComponent,
  ZardDropdownTriggerDirective,
] as const;
