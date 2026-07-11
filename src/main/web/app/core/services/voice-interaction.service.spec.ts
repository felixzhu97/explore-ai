import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { VoiceInteractionService } from './voice-interaction.service';
import { VoiceRecorderService } from './voice-recorder.service';
import { VoiceTranscriptionService } from './voice-transcription.service';
import { NotificationService } from './notification.service';
import { I18nService } from '@core/i18n';

describe('VoiceInteractionService', () => {
  let service: VoiceInteractionService;
  let recorder: {
    start: ReturnType<typeof vi.fn>;
    stop: ReturnType<typeof vi.fn>;
  };
  let transcription: {
    connect: ReturnType<typeof vi.fn>;
    disconnect: ReturnType<typeof vi.fn>;
    sendAudioChunk: ReturnType<typeof vi.fn>;
    stopAndWaitForFinal: ReturnType<typeof vi.fn>;
    partialText: { (): string; set: ReturnType<typeof vi.fn> };
    isConnected: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    recorder = {
      start: vi.fn().mockResolvedValue(undefined),
      stop: vi.fn(),
    };
    transcription = {
      connect: vi.fn(),
      disconnect: vi.fn(),
      sendAudioChunk: vi.fn(),
      stopAndWaitForFinal: vi.fn().mockResolvedValue('hello there'),
      partialText: Object.assign(vi.fn(() => ''), { set: vi.fn() }),
      isConnected: vi.fn(() => true),
    };

    TestBed.configureTestingModule({
      providers: [
        VoiceInteractionService,
        { provide: VoiceRecorderService, useValue: recorder },
        { provide: VoiceTranscriptionService, useValue: transcription },
        NotificationService,
        I18nService,
      ],
    });

    service = TestBed.inject(VoiceInteractionService);
  });

  it('should_trigger_final_callback_when_recording_stops', async () => {
    const onFinal = vi.fn();
    service.setOnFinal(onFinal);

    await service.onRecordingChange(true);
    expect(service.isRecording()).toBe(true);

    await service.onRecordingChange(false);

    expect(recorder.stop).toHaveBeenCalled();
    expect(onFinal).toHaveBeenCalledWith('hello there');
    expect(service.isTranscribing()).toBe(false);
  });

  it('should_not_start_recording_when_already_transcribing', async () => {
    service.isRecording.set(true);
    await service.onRecordingChange(true);

    expect(recorder.start).not.toHaveBeenCalled();
  });
});
