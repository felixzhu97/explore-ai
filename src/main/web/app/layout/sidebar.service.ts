import { Service, signal } from '@angular/core';

@Service()
export class SidebarService {
  readonly isMobileOpen = signal(false);
  readonly isCollapsed = signal(false);

  #mobileResizeHandler: (() => void) | null = null;

  open() {
    if (window.innerWidth < 768) {
      this.isCollapsed.set(false);
      this.#lockBodyScroll();
    }
    this.isMobileOpen.set(true);
  }

  close() {
    this.isMobileOpen.set(false);
    this.#unlockBodyScroll();
  }

  toggle() {
    const isMobile = window.innerWidth < 768;
    this.isMobileOpen.update((open) => {
      const next = !open;
      if (isMobile) {
        if (next) {
          this.isCollapsed.set(false);
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
    if (this.#mobileResizeHandler !== null) {
      window.removeEventListener('resize', this.#mobileResizeHandler);
      this.#mobileResizeHandler = null;
    }
  }
}
