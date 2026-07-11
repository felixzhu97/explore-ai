import { Injectable, inject, signal } from '@angular/core';
import { ApiService } from '@core/services/api.service';

const MUTE_STORAGE_KEY = 'chat.voice.muted';

@Injectable({ providedIn: 'root' })
export class VoicePlaybackService {
  private readonly api = inject(ApiService);

  readonly isMuted = signal(this.readMutedPreference());
  readonly isPlaying = signal(false);

  private audioElement: HTMLAudioElement | null = null;
  private objectUrl: string | null = null;

  toggleMuted(): void {
    const next = !this.isMuted();
    this.isMuted.set(next);
    sessionStorage.setItem(MUTE_STORAGE_KEY, String(next));
    if (next) {
      this.stop();
    }
  }

  speak(text: string): void {
    const trimmed = text.trim();
    if (!trimmed || this.isMuted()) {
      return;
    }
    this.stop();
    this.api.synthesizeSpeech({ text: trimmed }).subscribe({
      next: (blob) => {
        this.objectUrl = URL.createObjectURL(blob);
        this.audioElement = new Audio(this.objectUrl);
        this.isPlaying.set(true);
        this.audioElement.onended = () => this.stop();
        this.audioElement.onerror = () => this.stop();
        void this.audioElement.play().catch(() => this.stop());
      },
    });
  }

  stop(): void {
    if (this.audioElement) {
      this.audioElement.pause();
      this.audioElement = null;
    }
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
    this.isPlaying.set(false);
  }

  private readMutedPreference(): boolean {
    return sessionStorage.getItem(MUTE_STORAGE_KEY) === 'true';
  }
}
