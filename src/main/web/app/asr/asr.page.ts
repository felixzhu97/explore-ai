import { Component, ChangeDetectionStrategy, computed, inject, OnDestroy } from '@angular/core';
import { AsrService } from './asr.service';
import { ZardButtonComponent } from '../shared/components/button';
import { I18nService } from '../core/i18n';
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
  protected readonly i18n = inject(I18nService);

  readonly connectionStateLabel = computed(() => {
    const state = this.asr.connectionState() as AsrConnectionState;
    return this.i18n.t().asrPage.connectionState[state];
  });

  readonly errorMessage = computed(() => {
    const error = this.asr.error();
    if (!error) {
      return null;
    }
    if (isAsrErrorKey(error)) {
      return this.i18n.t().asrPage.errors[error];
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

function isAsrErrorKey(value: string): value is AsrErrorKey {
  return (ASR_ERROR_KEYS as readonly string[]).includes(value);
}
