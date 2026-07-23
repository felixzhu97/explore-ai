import {
  Component,
  signal,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgIcon, provideIcons } from '@ng-icons/core';
import { lucideDownload, lucidePause, lucidePlay } from '@ng-icons/lucide';
import { I18nService } from '../../core/i18n';
import { TtsService } from './tts.service';
import { ZardAlertComponent } from '../../shared/components/alert';
import { ZardButtonComponent } from '../../shared/components/button';
import { ZardInputDirective } from '../../shared/components/input';
import { ZardProgressBarComponent } from '../../shared/components/progress-bar';
import { ZardSelectImports } from '../../shared/components/select/select.imports';
import { ZardSliderComponent } from '../../shared/components/slider';
import type { Voice } from './tts.model';

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
export class TtsPage implements OnInit, OnDestroy {
  private readonly tts = inject(TtsService);
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

  private audioElement: HTMLAudioElement | null = null;

  ngOnInit() {
    this.loadVoices();
  }

  ngOnDestroy() {
    if (this.audioElement) {
      this.audioElement.pause();
    }
    if (this.audioUrl()) {
      URL.revokeObjectURL(this.audioUrl()!);
    }
  }

  loadVoices() {
    this.tts.getVoices().subscribe({
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
    if (!this.text().trim() || this.isSynthesizing()) return;

    this.isSynthesizing.set(true);
    this.error.set(null);

    this.tts
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

          this.audioElement = new Audio(url);
          this.audioElement.addEventListener('ended', () => {
            return this.isPlaying.set(false);
          });
          this.audioElement.addEventListener('timeupdate', () => {
            if (this.audioElement) {
              const duration = this.audioElement.duration;
              const progressValue = duration > 0
                ? (this.audioElement.currentTime / duration) * 100
                : 0;
              return this.progress.set(progressValue);
            }
          });
        },
        error: (err: unknown) => {
          this.error.set(err instanceof Error ? err.message : 'Synthesis failed');
          this.isSynthesizing.set(false);
        },
        complete: () => {
          this.isSynthesizing.set(false);
        },
      });
  }

  togglePlayPause() {
    if (!this.audioElement) return;

    if (this.isPlaying()) {
      this.audioElement.pause();
      this.isPlaying.set(false);
    } else {
      this.audioElement.play();
      this.isPlaying.set(true);
    }
  }

  download() {
    const blob = this.audioBlob();
    if (blob) {
      this.tts.download(blob, `speech_${Date.now()}.mp3`);
    }
  }
}
