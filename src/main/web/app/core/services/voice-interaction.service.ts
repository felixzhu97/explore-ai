import { Injectable, inject, signal } from '@angular/core';
import { I18nService } from '@core/i18n';
import { NotificationService } from '@core/services/notification.service';
import { VoiceRecorderService } from '@core/services/voice-recorder.service';
import { VoiceTranscriptionService } from '@core/services/voice-transcription.service';

@Injectable({ providedIn: 'root' })
export class VoiceInteractionService {
  private readonly recorder = inject(VoiceRecorderService);
  private readonly transcription = inject(VoiceTranscriptionService);
  private readonly notifications = inject(NotificationService);
  private readonly i18n = inject(I18nService);

  readonly isRecording = signal(false);
  readonly isTranscribing = signal(false);
  readonly partialText = this.transcription.partialText;

  private onFinalCallback: ((text: string) => void) | null = null;

  setOnFinal(callback: (text: string) => void): void {
    this.onFinalCallback = callback;
  }

  async onRecordingChange(recording: boolean): Promise<void> {
    if (recording) {
      await this.startRecording();
      return;
    }
    await this.stopRecording();
  }

  private async startRecording(): Promise<void> {
    if (this.isRecording() || this.isTranscribing()) {
      return;
    }
    try {
      this.transcription.connect();
      await this.waitForSocketOpen();
      await this.recorder.start(chunk => this.transcription.sendAudioChunk(chunk));
      this.isRecording.set(true);
    } catch (error) {
      this.handleRecordingError(error);
    }
  }

  private async stopRecording(): Promise<void> {
    if (!this.isRecording()) {
      return;
    }
    this.isRecording.set(false);
    this.isTranscribing.set(true);
    this.recorder.stop();

    try {
      const text = (await this.transcription.stopAndWaitForFinal()).trim();
      if (!text) {
        this.notifications.showError(this.i18n.t().chat.asrUnavailable);
        return;
      }
      this.onFinalCallback?.(text);
    } catch (error) {
      const message = error instanceof Error
        ? error.message
        : this.i18n.t().chat.asrUnavailable;
      if (
        message.toLowerCase().includes('permission')
        || message.toLowerCase().includes('notallowed')
      ) {
        this.notifications.showError(this.i18n.t().chat.micPermissionDenied);
      } else {
        this.notifications.showError(message);
      }
    } finally {
      this.isTranscribing.set(false);
      this.transcription.disconnect();
    }
  }

  private waitForSocketOpen(): Promise<void> {
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(
        () => reject(new Error(this.i18n.t().chat.asrUnavailable)),
        5_000,
      );
      const check = () => {
        if (this.transcription.isConnected()) {
          clearTimeout(timeout);
          resolve();
          return;
        }
        requestAnimationFrame(check);
      };
      check();
    });
  }

  private handleRecordingError(error: unknown): void {
    this.isRecording.set(false);
    this.transcription.disconnect();
    const message = error instanceof Error ? error.message : '';
    if (message.includes('Permission') || message.includes('NotAllowed')) {
      this.notifications.showError(this.i18n.t().chat.micPermissionDenied);
      return;
    }
    this.notifications.showError(this.i18n.t().chat.micPermissionDenied);
  }
}
