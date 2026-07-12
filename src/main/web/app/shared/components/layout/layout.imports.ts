import { ContentComponent } from '@/shared/components/layout/content.component';
import { FooterComponent } from '@/shared/components/layout/footer.component';
import { HeaderComponent } from '@/shared/components/layout/header.component';
import { LayoutComponent } from '@/shared/components/layout/layout.component';
import {
  SidebarComponent,
  SidebarGroupComponent,
  SidebarGroupLabelComponent,
} from '@/shared/components/layout/sidebar.component';
import { ZardSidebarMenuButtonDirective } from '@/shared/components/layout/sidebar-menu-button.directive';

export const LayoutImports = [
  LayoutComponent,
  HeaderComponent,
  FooterComponent,
  ContentComponent,
  SidebarComponent,
  SidebarGroupComponent,
  SidebarGroupLabelComponent,
  ZardSidebarMenuButtonDirective,
] as const;
