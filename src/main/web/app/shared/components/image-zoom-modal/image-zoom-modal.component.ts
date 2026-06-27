import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  effect,
  inject,
  HostListener,
  OnDestroy,
  DOCUMENT,
} from '@angular/core';

@Component({
  selector: 'app-image-zoom-modal',
  standalone: true,
  template: `
    @if (isOpen()) {
      <div
        class="fixed inset-0 z-[1000] flex items-center justify-center p-8 bg-black/85 backdrop-blur-md cursor-zoom-out animate-fade-in"
        style="-webkit-backdrop-filter: blur(8px)"
        (click)="close()"
        (keydown.escape)="close()"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        aria-label="Image zoom viewer"
      >
        <div
          class="relative max-w-[90vw] max-h-[90vh] cursor-default animate-slide-in"
          (click)="$event.stopPropagation()"
          (keydown.escape)="close()"
          tabindex="0"
          role="button"
        >
          <img
            class="max-w-full max-h-[90vh] object-contain rounded-lg shadow-[0_20px_60px_rgba(0,0,0,0.5)]"
            [src]="src()"
            [alt]="alt() || 'Zoomed image'"
          />
          @if (alt()) {
            <div class="mt-4 text-sm text-center text-text-secondary">{{ alt() }}</div>
          }
          <button
            class="absolute w-9 h-9 -top-4 -right-4 flex items-center justify-center text-text bg-surface border border-border rounded-full shadow-[0_2px_8px_rgba(0,0,0,0.2)] transition-all duration-150 hover:bg-[#f5f5f7] hover:scale-110 focus:outline-none focus-visible:ring-[3px] focus-visible:ring-blue-500/30"
            (click)="close()"
            aria-label="Close"
            type="button"
          >
            <svg
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>
      </div>
    }
  `,
  styles: [
    `
      @keyframes fadeIn {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }

      @keyframes slideIn {
        from {
          opacity: 0;
          transform: scale(0.95);
        }
        to {
          opacity: 1;
          transform: scale(1);
        }
      }

      :host {
        display: block;
      }

      .animate-fade-in {
        animation: fadeIn 0.2s ease;
      }

      .animate-slide-in {
        animation: slideIn 0.25s ease;
      }
    `,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageZoomModalComponent implements OnDestroy {
  private document = inject(DOCUMENT);

  readonly src = input.required<string>();
  readonly alt = input<string>('');

  closed = output<void>();

  protected readonly isOpen = signal(true);

  constructor() {
    effect(() => {
      if (this.src()) {
        this.isOpen.set(true);
        this.document.body.style.overflow = 'hidden';
      }
    });
  }

  ngOnDestroy(): void {
    this.document.body.style.overflow = '';
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.isOpen()) {
      this.close();
    }
  }

  close(): void {
    this.isOpen.set(false);
    this.document.body.style.overflow = '';
    this.closed.emit();
  }
}
