import { OverlayModule } from '@angular/cdk/overlay';
import { PortalModule } from '@angular/cdk/portal';

import { ZardButtonComponent } from '../button';
import { ZardDialogComponent } from './dialog.component';

export const ZardDialogImports = [
  ZardButtonComponent,
  ZardDialogComponent,
  OverlayModule,
  PortalModule,
] as const;
