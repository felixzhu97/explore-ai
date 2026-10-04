import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable, map, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import { downloadBlob } from '../ui/download';
import { DEFAULT_VOICES } from './tts.constants';
import { hasItems } from '../shared/presence';

export interface VoiceResponse {
  id: string;
  name: string;
  language: string;
  gender: string;
}

export interface VoicesResponse {
  voices: VoiceResponse[];
}

/** POST /api/audio/speech */
export interface TextToSpeechRequest {
  text: string;
  voice?: string;
  speed?: number;
  outputFormat?: string;
}

@Service()
export class TtsService {
  readonly #http = inject(HttpClient);

  getVoices(): Observable<VoiceResponse[]> {
    return this.#http
      .get<VoicesResponse>(`${API_BASE_URL}/audio/voices`)
      .pipe(
        map(response => (hasItems(response.voices) ? response.voices : DEFAULT_VOICES)),
        catchError(() => of(DEFAULT_VOICES)),
      );
  }

  synthesizeSpeech(request: TextToSpeechRequest): Observable<Blob> {
    return this.#http.post(`${API_BASE_URL}/audio/speech`, request, { responseType: 'blob' });
  }

  download(blob: Blob, filename: string): void {
    downloadBlob(blob, filename);
  }
}
