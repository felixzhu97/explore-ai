import { Service, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { type Observable, map, catchError, of } from 'rxjs';
import { API_BASE_URL } from '../http/api.constants';
import { downloadBlob } from '../ui/download';
import { DEFAULT_VOICES } from './tts.constants';

export interface Voice {
  id: string;
  name: string;
  language: string;
  languageName?: string;
  gender?: string;
  provider?: string;
  isDefault?: boolean;
}

/** POST /api/audio/speech */
export interface TtsRequest {
  text: string;
  voice?: string | undefined;
  speed?: number;
  outputFormat?: string;
}

@Service()
export class TtsService {
  readonly #http = inject(HttpClient);

  getVoices(): Observable<Voice[]> {
    return this.#http
      .get<{ voices: (Voice | string)[] }>(`${API_BASE_URL}/audio/voices`)
      .pipe(
        map(response => this.#normalizeVoices(response.voices)),
        catchError(() => of(DEFAULT_VOICES)),
      );
  }

  synthesizeSpeech(params: TtsRequest): Observable<Blob> {
    return this.#http.post<Blob>(
      `${API_BASE_URL}/audio/speech`,
      {
        text: params.text,
        voice: params.voice,
        speed: params.speed,
        outputFormat: params.outputFormat,
      },
      {
        responseType: 'blob' as 'json',
      },
    );
  }

  download(blob: Blob, filename: string): void {
    downloadBlob(blob, filename);
  }

  #normalizeVoices(voices: (Voice | string)[]): Voice[] {
    if (!voices.length) {
      return DEFAULT_VOICES;
    }

    return voices.map((voice, index) => {
      if (typeof voice === 'string') {
        return {
          id: voice,
          name: voice.charAt(0).toUpperCase() + voice.slice(1),
          language: 'en',
          provider: 'openai',
          isDefault: index === 0,
        };
      }
      return {
        ...voice,
        provider: voice.provider ?? 'openai',
        isDefault: voice.isDefault ?? index === 0,
      };
    });
  }
}
