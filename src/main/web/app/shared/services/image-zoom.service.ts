import { Injectable, inject } from '@angular/core';
import { ZardDialogService } from '@/shared/components/dialog';
import { ImageZoomDialogComponent } from '@/shared/components/image-zoom/image-zoom-dialog.component';

@Injectable({ providedIn: 'root' })
export class ImageZoomService {
  private readonly dialog = inject(ZardDialogService);

  open(src: string, alt = ''): void {
    this.dialog.create({
      zContent: ImageZoomDialogComponent,
      zData: { src, alt },
      zHideFooter: true,
      zWidth: '90vw',
      zMaskClosable: true,
      zCustomClasses: 'border-none bg-background/95 shadow-2xl',
    });
  }
}
