import { Component, ChangeDetectionStrategy, computed, inject, OnDestroy } from '@angular/core';
import {
  SpeechToTextService,
  type SpeechToTextConnectionState,
} from '../services/speech-to-text.service';
import { ZardButtonComponent } from '../../../shared/components/button';
import { I18nService } from '../../../core/i18n';

const SPEECH_TO_TEXT_ERROR_KEYS = ['connectionFailed', 'notConnected', 'generic'] as const;
type SpeechToTextErrorKey = (typeof SPEECH_TO_TEXT_ERROR_KEYS)[number];

@Component({
  selector: 'app-speech-to-text-page',
  imports: [ZardButtonComponent],
  templateUrl: './speech-to-text.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class SpeechToTextPageComponent implements OnDestroy {
  protected readonly speechToText = inject(SpeechToTextService);
  protected readonly i18n = inject(I18nService);

  readonly connectionStateLabel = computed(() => {
    const state = this.speechToText.connectionState() as SpeechToTextConnectionState;
    return this.i18n.t().speechToText.connectionState[state];
  });

  readonly errorMessage = computed(() => {
    const error = this.speechToText.error();
    if (!error) {
      return null;
    }
    if (isSpeechToTextErrorKey(error)) {
      return this.i18n.t().speechToText.errors[error];
    }
    return error;
  });

  ngOnDestroy(): void {
    this.speechToText.disconnect();
  }

  connect(): void {
    this.speechToText.connect();
  }

  disconnect(): void {
    this.speechToText.disconnect();
  }

  sendTestPayload(): void {
    this.speechToText.sendTestAudioPayload();
  }

  sendStop(): void {
    this.speechToText.sendStop();
  }
}

function isSpeechToTextErrorKey(value: string): value is SpeechToTextErrorKey {
  return (SPEECH_TO_TEXT_ERROR_KEYS as readonly string[]).includes(value);
}
