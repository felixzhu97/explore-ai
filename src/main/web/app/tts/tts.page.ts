import {
  Component,
  signal,
  inject,
  type OnInit,
  type OnDestroy,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideDownload, lucidePause, lucidePlay } from '@ng-icons/lucide';
import { I18nService } from '../i18n';
import { TtsService, type Voice } from './tts.service';
import { ZardAlertComponent } from '../ui/alert';
import { ZardButtonComponent } from '../ui/button';
import { ZardInputDirective } from '../ui/input';
import { ZardProgressBarComponent } from '../ui/progress-bar';
import { ZardSelectImports } from '../ui/select/select.imports';
import { ZardSliderComponent } from '../ui/slider';

@Component({
  selector: 'app-tts-page',
  imports: [
    FormsModule,
    NgIcon,
    ZardAlertComponent,
    ZardButtonComponent,
    ZardInputDirective,
    ZardProgressBarComponent,
    ZardSliderComponent,
    ...ZardSelectImports,
  ],
  templateUrl: './tts.page.html',
  providers: [provideIcons({ lucidePlay, lucidePause, lucideDownload })],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TtsPageComponent implements OnInit, OnDestroy {
  readonly #tts = inject(TtsService);
  protected readonly i18n = inject(I18nService);

  readonly text = signal('');
  readonly voice = signal('alloy');
  readonly speed = signal(1.0);
  readonly availableVoices = signal<Voice[]>([]);
  readonly isSynthesizing = signal(false);
  readonly error = signal<string | null>(null);
  readonly audioUrl = signal<string | null>(null);
  readonly audioBlob = signal<Blob | null>(null);
  readonly isPlaying = signal(false);
  readonly progress = signal(0);

  #audioElement: HTMLAudioElement | null = null;

  ngOnInit() {
    this.loadVoices();
  }

  ngOnDestroy() {
    if (this.#audioElement) {
      this.#audioElement.pause();
    }
    if (this.audioUrl()) {
      URL.revokeObjectURL(this.audioUrl()!);
    }
  }

  loadVoices() {
    this.#tts.getVoices().subscribe({
      next: (voices) => {
        this.availableVoices.set(voices);
        const defaultVoice = voices.find((v: Voice) => v.isDefault) || voices[0];
        if (defaultVoice) {
          this.voice.set(defaultVoice.id);
        }
      },
      error: () => {
        this.availableVoices.set([
          {
            id: 'alloy',
            name: 'Alloy',
            language: 'en',
            provider: 'openai',
            isDefault: true,
          },
        ]);
      },
    });
  }

  setText(text: string) {
    this.text.set(text);
  }

  setVoice(voice: string) {
    this.voice.set(voice);
  }

  setSpeed(speed: number) {
    this.speed.set(speed);
  }

  synthesize() {
    if (!this.text().trim() || this.isSynthesizing()) {
      return;
    }

    this.isSynthesizing.set(true);
    this.error.set(null);

    this.#tts
      .synthesizeSpeech({
        text: this.text(),
        voice: this.voice() || undefined,
        speed: this.speed(),
        outputFormat: 'mp3',
      })
      .subscribe({
        next: (blob: Blob) => {
          if (this.audioUrl()) {
            URL.revokeObjectURL(this.audioUrl()!);
          }

          const url = URL.createObjectURL(blob);
          this.audioUrl.set(url);
          this.audioBlob.set(blob);

          this.#audioElement = new Audio(url);
          this.#audioElement.addEventListener('ended', () => {
            this.isPlaying.set(false);
          });
          this.#audioElement.addEventListener('timeupdate', () => {
            if (this.#audioElement) {
              const duration = this.#audioElement.duration;
              const progressValue = duration > 0
                ? (this.#audioElement.currentTime / duration) * 100
                : 0;
              this.progress.set(progressValue);
            }
          });
        },
        error: (error: unknown) => {
          this.error.set(error instanceof Error ? error.message : 'Synthesis failed');
          this.isSynthesizing.set(false);
        },
        complete: () => {
          this.isSynthesizing.set(false);
        },
      });
  }

  togglePlayPause() {
    if (!this.#audioElement) {
      return;
    }

    if (this.isPlaying()) {
      this.#audioElement.pause();
      this.isPlaying.set(false);
    } else {
      this.isPlaying.set(true);
      this.#audioElement.play().catch((error: unknown) => {
        this.isPlaying.set(false);
        this.error.set(error instanceof Error ? error.message : 'Playback failed');
      });
    }
  }

  download() {
    const blob = this.audioBlob();
    if (blob) {
      this.#tts.download(blob, `speech_${Date.now()}.mp3`);
    }
  }
}
