import {
  Component,
  signal,
  inject,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@i18n';
import { Voice } from '@shared/models';

@Component({
  selector: 'app-tts-tab',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="panel">
      <div class="panel-header">
        <div>
          <h3 class="panel-title">{{ i18n.t().aiHub.tts.title }}</h3>
          <p class="panel-description">{{ i18n.t().aiHub.tts.description }}</p>
        </div>
      </div>
      <div class="panel-content">
        <div class="tts-section">
          <div class="input-group">
            <label class="input-label">{{ i18n.t().aiHub.tts.textLabel }}</label>
            <textarea
              class="text-input"
              [ngModel]="text()"
              (ngModelChange)="setText($event)"
              placeholder="{{ i18n.t().aiHub.tts.textPlaceholder }}"
              rows="4"
            ></textarea>
          </div>

          <div class="control-row">
            <div class="input-group">
              <label class="input-label">{{ i18n.t().aiHub.tts.voiceLabel }}</label>
              <select
                class="text-select"
                [ngModel]="voice()"
                (ngModelChange)="setVoice($event)"
              >
                @if (availableVoices().length > 0) {
                  @for (v of availableVoices(); track v.id) {
                    <option [value]="v.id">{{ v.name }} ({{ v.language }})</option>
                  }
                } @else {
                  <option value="en-US">English (US)</option>
                  <option value="en-GB">English (UK)</option>
                  <option value="zh-CN">中文</option>
                  <option value="ja-JP">日本語</option>
                }
              </select>
            </div>

            <div class="slider-container">
              <label class="input-label">{{ i18n.t().aiHub.tts.speedLabel }}: {{ speed().toFixed(1) }}x</label>
              <input
                type="range"
                class="slider"
                min="0.5"
                max="2.0"
                step="0.1"
                [ngModel]="speed()"
                (ngModelChange)="setSpeed($event)"
              />
            </div>
          </div>

          <button
            class="action-button primary"
            (click)="synthesize()"
            [disabled]="!text().trim() || isSynthesizing()"
          >
            @if (isSynthesizing()) {
              <span class="btn-spinner"></span> {{ i18n.t().aiHub.tts.synthesizing }}
            } @else {
              {{ i18n.t().aiHub.tts.synthesizeButton }}
            }
          </button>

          @if (error()) {
            <div class="error-message">{{ error() }}</div>
          }

          @if (audioUrl()) {
            <div class="audio-player">
              <div class="audio-controls">
                <button
                  class="play-button"
                  [class.playing]="isPlaying()"
                  (click)="togglePlayPause()"
                >
                  {{ isPlaying() ? '⏸' : '▶' }}
                </button>
                <div class="audio-info">
                  <span class="audio-label">{{ i18n.t().aiHub.tts.audioReady }}</span>
                  <div class="audio-bar">
                    <div class="audio-progress" [style.width.%]="progress()"></div>
                  </div>
                </div>
              </div>
              <button class="download-link" (click)="download()">
                ⬇️ {{ i18n.t().aiHub.tts.downloadAudio }}
              </button>
            </div>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .panel {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: 14px;
    }

    .panel-title {
      font-size: 17px;
      font-weight: 600;
      color: #1d1d1f;
      margin: 0;
    }

    .panel-description {
      font-size: 14px;
      color: #86868b;
      margin: 4px 0 0 0;
    }

    .panel-content {
      background: #ffffff;
      border: 1px solid var(--color-border);
      border-radius: 14px;
      padding: 24px;
    }

    .tts-section {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .input-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .input-label {
      font-size: 14px;
      font-weight: 500;
      color: #86868b;
    }

    .text-input {
      padding: 12px 16px;
      font-size: 15px;
      font-family: inherit;
      border: 1px solid var(--color-border);
      border-radius: 10px;
      background: #ffffff;
      color: #1d1d1f;
      resize: vertical;
      min-height: 80px;
      transition: border-color 0.15s, box-shadow 0.15s;
    }

    .text-input:focus {
      outline: none;
      border-color: #007aff;
      box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.2);
    }

    .text-input::placeholder {
      color: #86868b;
    }

    .text-select {
      padding: 12px 16px;
      font-size: 15px;
      font-family: inherit;
      border: 1px solid var(--color-border);
      border-radius: 10px;
      background: #ffffff;
      color: #1d1d1f;
      cursor: pointer;
      min-width: 200px;
    }

    .text-select:focus {
      outline: none;
      border-color: #007aff;
    }

    .control-row {
      display: flex;
      gap: 24px;
      align-items: flex-end;
      flex-wrap: wrap;
    }

    @media (max-width: 640px) {
      .control-row {
        flex-direction: column;
      }
    }

    .slider-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
      flex: 1;
      min-width: 150px;
    }

    .slider {
      width: 100%;
      height: 4px;
      background: #e5e5e5;
      border-radius: 2px;
      outline: none;
      -webkit-appearance: none;
    }

    .slider::-webkit-slider-thumb {
      -webkit-appearance: none;
      width: 18px;
      height: 18px;
      background: #0071e3;
      border-radius: 50%;
      cursor: pointer;
      transition: transform 0.15s ease;
    }

    .slider::-webkit-slider-thumb:hover {
      transform: scale(1.15);
    }

    .action-button {
      padding: 12px 24px;
      font-size: 15px;
      font-weight: 500;
      font-family: inherit;
      background: #ffffff;
      color: #1d1d1f;
      border: 1px solid var(--color-border);
      border-radius: 10px;
      cursor: pointer;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .action-button.primary {
      background: #007aff;
      color: white;
      border: transparent;
    }

    .action-button.primary:hover:not(:disabled) {
      background: #0071e3;
    }

    .action-button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .error-message {
      padding: 12px;
      background: #ffebee;
      color: #c62828;
      border-radius: 8px;
      font-size: 14px;
      animation: fadeIn 0.2s ease;
      border: 1px solid #ffcdd2;
    }

    .btn-spinner {
      display: inline-block;
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.7s linear infinite;
    }

    .audio-player {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 24px;
      background: #f5f5f7;
      border: 1px solid #0071e333;
      border-radius: 12px;
      animation: fadeIn 0.3s ease;
    }

    .audio-controls {
      display: flex;
      align-items: center;
      gap: 16px;
    }

    .play-button {
      width: 52px;
      height: 52px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #0071e3;
      color: white;
      border: none;
      border-radius: 12px;
      cursor: pointer;
      font-size: 18px;
      transition: all 0.2s ease;
    }

    .play-button.playing {
      background: #34c759;
    }

    .play-button:hover {
      opacity: 0.9;
      transform: scale(1.02);
    }

    .play-button:active {
      transform: scale(0.95);
    }

    .audio-info {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .audio-label {
      font-size: 15px;
      font-weight: 500;
      color: #1d1d1f;
    }

    .audio-bar {
      height: 6px;
      background: #e5e5e5;
      border-radius: 3px;
      overflow: hidden;
    }

    .audio-progress {
      height: 100%;
      width: 0%;
      background: #0071e3;
      transition: width 0.1s linear;
      border-radius: 3px;
    }

    .download-link {
      padding: 8px 16px;
      font-size: 14px;
      font-weight: 500;
      background: #ffffff;
      color: #0071e3;
      border: 1px solid #e5e5e5;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.15s ease;
      display: flex;
      align-items: center;
      gap: 4px;
      width: fit-content;
    }

    .download-link:hover {
      background: #f5f5f7;
      border-color: #0071e3;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: translateY(0); }
    }

    @keyframes spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }
  `]
})
export class TtsTabComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  protected readonly i18n = inject(I18nService);

  text = signal('');
  voice = signal('en-US');
  speed = signal(1.0);
  availableVoices = signal<Voice[]>([]);
  isSynthesizing = signal(false);
  error = signal<string | null>(null);
  audioUrl = signal<string | null>(null);
  audioBlob = signal<Blob | null>(null);
  isPlaying = signal(false);
  progress = signal(0);

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
    this.api.getVoices().subscribe({
      next: (voices) => {
        this.availableVoices.set(voices);
        const defaultVoice = voices.find((v: Voice) => v.is_default) || voices[0];
        if (defaultVoice) {
          this.voice.set(defaultVoice.id);
        }
      },
      error: () => {
        this.availableVoices.set([
          { id: 'en-US', name: 'English (US)', language: 'en-US', provider: 'default', is_default: true },
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

    this.api
      .synthesizeSpeech({
        text: this.text(),
        voice: this.voice() || undefined,
        speed: this.speed(),
        output_format: 'mp3',
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
          this.audioElement.addEventListener('ended', () => this.isPlaying.set(false));
          this.audioElement.addEventListener('timeupdate', () => {
            if (this.audioElement) {
              this.progress.set((this.audioElement.currentTime / this.audioElement.duration) * 100);
            }
          });
        },
        error: (err: unknown) => {
          this.error.set(err instanceof Error ? err.message : 'Synthesis failed');
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
      this.api.downloadBlob(blob, `speech_${Date.now()}.mp3`);
    }
  }
}
