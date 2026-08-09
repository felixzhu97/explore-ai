import { Component, ChangeDetectionStrategy, computed, inject, OnDestroy } from '@angular/core';
import { AsrService } from './asr.service';
import { ZardButtonComponent } from '../shared/components/button';
import type { AsrConnectionState } from './asr.model';

const ASR_ERROR_KEYS = ['connectionFailed', 'notConnected', 'generic'] as const;
type AsrErrorKey = (typeof ASR_ERROR_KEYS)[number];

@Component({
  selector: 'app-asr-page',
  imports: [ZardButtonComponent],
  templateUrl: './asr.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class AsrPageComponent implements OnDestroy {
  protected readonly asr = inject(AsrService);

  /** Bound once; `$localize` is resolved at bootstrap after `loadTranslations`. */
  protected readonly emptyTranscript = $localize`:@@asr.empty:Connect to start live transcription.`;

  readonly connectionStateLabel = computed(() => {
    const state = this.asr.connectionState() as AsrConnectionState;
    return connectionStateLabel(state);
  });

  readonly errorMessage = computed(() => {
    const error = this.asr.error();
    if (!error) {
      return null;
    }
    if (isAsrErrorKey(error)) {
      return asrErrorLabel(error);
    }
    return error;
  });

  ngOnDestroy(): void {
    this.asr.disconnect();
  }

  connect(): void {
    this.asr.connect();
  }

  disconnect(): void {
    this.asr.disconnect();
  }

  sendTestPayload(): void {
    this.asr.sendTestAudioPayload();
  }

  sendStop(): void {
    this.asr.sendStop();
  }
}

function connectionStateLabel(state: AsrConnectionState): string {
  switch (state) {
    case 'disconnected':
      return $localize`:@@asr.state.disconnected:disconnected`;
    case 'connecting':
      return $localize`:@@asr.state.connecting:connecting`;
    case 'connected':
      return $localize`:@@asr.state.connected:connected`;
    case 'error':
      return $localize`:@@asr.state.error:error`;
  }
}

function asrErrorLabel(key: AsrErrorKey): string {
  switch (key) {
    case 'connectionFailed':
      return $localize`:@@asr.error.connectionFailed:WebSocket connection failed`;
    case 'notConnected':
      return $localize`:@@asr.error.notConnected:WebSocket is not connected`;
    case 'generic':
      return $localize`:@@asr.error.generic:ASR error`;
  }
}

function isAsrErrorKey(value: string): value is AsrErrorKey {
  return (ASR_ERROR_KEYS as readonly string[]).includes(value);
}
