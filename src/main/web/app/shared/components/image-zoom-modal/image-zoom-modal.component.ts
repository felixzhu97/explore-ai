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
  template: `
    @if (isOpen()) {
      <div
        class="
          fixed inset-0 z-1000 flex animate-fade-in cursor-zoom-out items-center
          justify-center bg-black/85 p-8 backdrop-blur-md
        "
        (click)="close()"
        (keydown.escape)="close()"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        aria-label="Image zoom viewer"
      >
        <div
          class="
            relative max-h-[90vh] max-w-[90vw] animate-slide-in cursor-default
          "
          (click)="$event.stopPropagation()"
          (keydown.escape)="close()"
          tabindex="0"
          role="button"
        >
          <img
            class="
              max-h-[90vh] max-w-full rounded-lg object-contain
              shadow-2xl
            "
            [src]="src()"
            [alt]="alt() || 'Zoomed image'"
          />
          @if (alt()) {
            <div class="mt-4 text-center text-sm text-text-secondary">{{ alt() }}</div>
          }
          <button
            class="
              absolute -top-4 -right-4 flex size-9 items-center justify-center
              rounded-full border border-border bg-surface text-text
              shadow-sm transition-all duration-150
              hover:scale-110 hover:bg-surface-secondary
              focus:outline-none
              focus-visible:ring-2 focus-visible:ring-blue-500/30
            "
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
