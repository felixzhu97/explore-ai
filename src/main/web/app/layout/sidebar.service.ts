import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SidebarService {
  readonly mobileOpen = signal(false);
  readonly collapsed = signal(false);

  #mobileResizeHandler: (() => void) | null = null;

  open() {
    if (window.innerWidth < 768) {
      this.collapsed.set(false);
      this.#lockBodyScroll();
    }
    this.mobileOpen.set(true);
  }

  close() {
    this.mobileOpen.set(false);
    this.#unlockBodyScroll();
  }

  toggle() {
    const isMobile = window.innerWidth < 768;
    this.mobileOpen.update((open) => {
      const next = !open;
      if (isMobile) {
        if (next) {
          this.collapsed.set(false);
          this.#lockBodyScroll();
        } else {
          this.#unlockBodyScroll();
        }
      }
      return next;
    });
  }

  #lockBodyScroll(): void {
    document.body.classList.add('overflow-hidden');
    this.#removeMobileResizeListener();
    this.#mobileResizeHandler = () => {
      if (window.innerWidth >= 768) {
        this.close();
      }
    };
    window.addEventListener('resize', this.#mobileResizeHandler);
  }

  #unlockBodyScroll(): void {
    document.body.classList.remove('overflow-hidden');
    this.#removeMobileResizeListener();
  }

  #removeMobileResizeListener(): void {
    if (this.#mobileResizeHandler) {
      window.removeEventListener('resize', this.#mobileResizeHandler);
      this.#mobileResizeHandler = null;
    }
  }
}
