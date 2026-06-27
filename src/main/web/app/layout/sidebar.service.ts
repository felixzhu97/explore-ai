import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SidebarService {
  readonly mobileOpen = signal(false);

  open() {
    this.mobileOpen.set(true);
  }

  close() {
    this.mobileOpen.set(false);
  }

  toggle() {
    this.mobileOpen.update(v => !v);
  }
}
