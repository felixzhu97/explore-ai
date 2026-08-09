import { describe, expect, it, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TtsService } from './tts.service';
import { DEFAULT_VOICES } from './tts.constants';
import { API_BASE_URL } from '../../core/api.constants';
import * as downloadUtils from '../../shared/utils/download';

describe('TtsService', () => {
  let service: TtsService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [TtsService],
    });
    service = TestBed.inject(TtsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should return normalized voices from api', () => {
    service.getVoices().subscribe((voices) => {
      expect(voices[0]).toEqual(expect.objectContaining({
        id: 'alloy',
        name: 'Alloy',
        provider: 'openai',
        isDefault: true,
      }));
    });

    const req = httpMock.expectOne(`${API_BASE_URL}/audio/voices`);
    req.flush({ voices: ['alloy', { id: 'nova', name: 'Nova', language: 'en' }] });
  });

  it('should return default voices when api fails', () => {
    service.getVoices().subscribe((voices) => {
      expect(voices).toEqual(DEFAULT_VOICES);
    });

    const req = httpMock.expectOne(`${API_BASE_URL}/audio/voices`);
    req.error(new ProgressEvent('error'));
  });

  it('should return default voices when api empty', () => {
    service.getVoices().subscribe((voices) => {
      expect(voices).toEqual(DEFAULT_VOICES);
    });

    httpMock.expectOne(`${API_BASE_URL}/audio/voices`).flush({ voices: [] });
  });

  it('should synthesize speech', () => {
    const blob = new Blob(['audio'], { type: 'audio/mpeg' });

    service.synthesizeSpeech({
      text: 'Hello',
      voice: 'alloy',
      speed: 1,
      outputFormat: 'mp3',
    }).subscribe((result) => {
      expect(result).toBe(blob);
    });

    const req = httpMock.expectOne(`${API_BASE_URL}/audio/speak`);
    expect(req.request.body).toEqual({
      text: 'Hello',
      voice: 'alloy',
      speed: 1,
      outputFormat: 'mp3',
    });
    req.flush(blob);
  });

  it('should delegate download to download blob', () => {
    const downloadSpy = vi.spyOn(downloadUtils, 'downloadBlob');
    const blob = new Blob(['audio']);

    service.download(blob, 'speech.mp3');

    expect(downloadSpy).toHaveBeenCalledWith(blob, 'speech.mp3');
  });
});
