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
        class="overlay"
        (click)="close()"
        (keydown.escape)="close()"
        tabindex="-1"
        role="dialog"
        aria-modal="true"
        aria-label="Image zoom viewer"
      >
        <div
          class="image-container"
          (click)="$event.stopPropagation()"
        >
          <img
            class="image"
            [src]="src()"
            [alt]="alt() || 'Zoomed image'"
          />
          @if (alt()) {
            <div class="caption">{{ alt() }}</div>
          }
          <button
            class="close-button"
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
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
        </div>
      </div>
    }
  `,
  styles: [`
    :host {
      display: block;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
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

    .overlay {
      position: fixed;
      inset: 0;
      background: rgba(0, 0, 0, 0.85);
      backdrop-filter: blur(8px);
      -webkit-backdrop-filter: blur(8px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      padding: 32px;
      animation: fadeIn 0.2s ease;
      cursor: zoom-out;
    }

    .image-container {
      position: relative;
      max-width: 90vw;
      max-height: 90vh;
      animation: slideIn 0.25s ease;
      cursor: default;
    }

    .image {
      max-width: 100%;
      max-height: 90vh;
      object-fit: contain;
      border-radius: 8px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
    }

    .caption {
      text-align: center;
      color: #86868b;
      font-size: 14px;
      margin-top: 16px;
    }

    .close-button {
      position: absolute;
      top: -16px;
      right: -16px;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: #ffffff;
      border: 1px solid #d1d1d6;
      color: #1d1d1f;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.15s ease;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);

      &:hover {
        background: #f5f5f7;
        transform: scale(1.1);
      }

      &:focus-visible {
        outline: none;
        box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.3);
      }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageZoomModalComponent implements OnDestroy {
  private document = inject(DOCUMENT);

  src = input.required<string>();
  alt = input<string>('');

  closed = output<void>();

  protected isOpen = signal(true);

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
