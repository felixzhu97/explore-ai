import { Component, ChangeDetectionStrategy, inject, OnDestroy } from '@angular/core';
import { AsrService } from './asr.service';
import { ZardButtonComponent } from '../shared/components/button';

@Component({
  selector: 'app-asr-page',
  imports: [ZardButtonComponent],
  templateUrl: './asr.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class AsrPageComponent implements OnDestroy {
  protected readonly asr = inject(AsrService);

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
