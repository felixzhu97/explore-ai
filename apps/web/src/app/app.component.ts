import { Component, ChangeDetectionStrategy } from '@angular/core';
import { MainLayoutComponent } from './layout';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [MainLayoutComponent],
  template: `<app-main-layout />`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent {}
