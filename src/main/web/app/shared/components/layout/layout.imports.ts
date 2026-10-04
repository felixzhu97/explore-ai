import { ZardContentComponent } from './content.component';
import { ZardFooterComponent } from './footer.component';
import { ZardHeaderComponent } from './header.component';
import { ZardLayoutComponent } from './layout.component';
import { ZardSidebarComponent } from './sidebar.component';
import { ZardSidebarGroupComponent } from './sidebar-group.component';
import { ZardSidebarGroupLabelComponent } from './sidebar-group-label.component';
import { ZardSidebarMenuButtonDirective } from './sidebar-menu-button.directive';

export const LayoutImports = [
  ZardLayoutComponent,
  ZardHeaderComponent,
  ZardFooterComponent,
  ZardContentComponent,
  ZardSidebarComponent,
  ZardSidebarGroupComponent,
  ZardSidebarGroupLabelComponent,
  ZardSidebarMenuButtonDirective,
] as const;
