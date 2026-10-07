import { Service, signal } from '@angular/core';

@Service()
export class SidebarService {
  readonly isMobileOpen = signal(false);
  readonly isCollapsed = signal(false);

  #mobileResizeHandler: (() => void) | null = null;

  /** Opens the sidebar and locks page scroll on mobile. */
  openSidebar() {
    if (window.innerWidth < 768) {
      this.isCollapsed.set(false);
      this.#lockBodyScroll();
    }
    this.isMobileOpen.set(true);
  }

  /** Closes the mobile sidebar and unlocks page scroll. */
  closeSidebar() {
    this.isMobileOpen.set(false);
    this.#unlockBodyScroll();
  }

  /** Opens or closes the mobile sidebar. */
  toggleSidebar() {
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
        this.closeSidebar();
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
