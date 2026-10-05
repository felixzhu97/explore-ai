import {
  Component,
  signal,
  inject,
  type OnInit,
  type OnDestroy,
} from '@angular/core';
import { Instant } from '@js-joda/core';
import { form, FormField } from '@angular/forms/signals';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideDownload, lucidePause, lucidePlay } from '@ng-icons/lucide';
import { I18nService } from '../i18n';
import { TtsService, type VoiceResponse } from './tts.service';
import { DEFAULT_VOICES } from './tts.constants';
import { ZardAlertComponent } from '../ui/alert';
import { ZardButtonComponent } from '../ui/button';
import { ZardInputDirective } from '../ui/input';
import { ZardProgressBarComponent } from '../ui/progress-bar';
import { ZardSelectImports } from '../ui/select/select.imports';
import { ZardSliderComponent } from '../ui/slider';
import { hasText } from '../shared/presence';

@Component({
  selector: 'app-tts-page',
  imports: [
    FormField,
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
})
export class TtsPageComponent implements OnInit, OnDestroy {
  readonly #tts = inject(TtsService);
  protected readonly i18n = inject(I18nService);

  readonly text = signal('');
  protected readonly textField = form(this.text);
  readonly voice = signal('alloy');
  readonly availableVoices = signal<VoiceResponse[]>([]);
  readonly speed = signal(1.0);
  readonly isSynthesizing = signal(false);
  readonly error = signal<string | null>(null);
  readonly audioUrl = signal<string | null>(null);
  readonly isPlaying = signal(false);
  readonly progress = signal(0);

  readonly audioBlob = signal<Blob | null>(null);
  #audioElement: HTMLAudioElement | null = null;

  ngOnInit() {
    this.loadVoices();
  }

  ngOnDestroy() {
    if (this.#audioElement !== null) {
      this.#audioElement.pause();
    }
    const audioUrl = this.audioUrl();
    if (hasText(audioUrl)) {
      URL.revokeObjectURL(audioUrl);
    }
  }

  setVoice(voice: string | string[]) {
    if (typeof voice === 'string') {
      this.voice.set(voice);
    }
  }

  setSpeed(speed: number) {
    this.speed.set(speed);
  }

  synthesize() {
    if (this.text().trim() === '' || this.isSynthesizing()) {
      return;
    }

    this.isSynthesizing.set(true);
    this.error.set(null);

    this.#tts
      .synthesizeSpeech({
        text: this.text(),
        ...(hasText(this.voice()) ? { voice: this.voice() } : {}),
        speed: this.speed(),
        outputFormat: 'mp3',
      })
      .subscribe({
        next: (blob: Blob) => {
          const previousUrl = this.audioUrl();
          if (hasText(previousUrl)) {
            URL.revokeObjectURL(previousUrl);
          }

          const url = URL.createObjectURL(blob);
          this.audioUrl.set(url);
          this.audioBlob.set(blob);

          this.#audioElement = new Audio(url);
          this.#audioElement.addEventListener('ended', () => {
            this.isPlaying.set(false);
          });
          this.#audioElement.addEventListener('timeupdate', () => {
            if (this.#audioElement !== null) {
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
    if (this.#audioElement === null) {
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
    if (blob !== null) {
      this.#tts.download(blob, `speech_${String(Instant.now().toEpochMilli())}.mp3`);
    }
  }

  loadVoices() {
    this.#tts.getVoices().subscribe({
      next: (voices) => {
        this.availableVoices.set(voices);
        const defaultVoice = voices[0];
        if (defaultVoice !== undefined) {
          this.voice.set(defaultVoice.id);
        }
      },
      error: () => {
        this.availableVoices.set(DEFAULT_VOICES);
      },
    });
  }
}
