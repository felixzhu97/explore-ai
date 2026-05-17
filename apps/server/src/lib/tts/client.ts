/**
 * TTS Client for TTS Service.
 * Provides typed interface for voice generation API calls.
 */

export interface VoiceGenerateRequest {
  text: string;
  voice?: string;
  language?: string;
  speed?: number;
  pitch?: number;
  output_format?: 'mp3' | 'wav' | 'ogg' | 'flac';
}

export interface Voice {
  id: string;
  name: string;
  language: string;
  language_name?: string;
  gender?: string;
  provider: string;
  is_default?: boolean;
}

export interface ProviderInfo {
  name: string;
  display_name: string;
  supported_languages: string[];
  features: string[];
}

export interface HealthResponse {
  status: 'healthy' | 'degraded' | 'unhealthy';
  provider: string;
  provider_status: string;
  version: string;
}

export interface TTSClientOptions {
  baseUrl?: string;
  timeout?: number;
}

const DEFAULT_TTS_SERVICE_URL = 'http://localhost:8004';

export class TTSClient {
  private readonly baseUrl: string;
  private readonly timeout: number;

  constructor(options: TTSClientOptions = {}) {
    this.baseUrl = options.baseUrl ?? DEFAULT_TTS_SERVICE_URL;
    this.timeout = options.timeout ?? 30000;
  }

  /**
   * Generate speech from text.
   * @param request - Voice generation parameters
   * @returns Audio data as ArrayBuffer
   */
  async generate(request: VoiceGenerateRequest): Promise<ArrayBuffer> {
    const response = await fetch(`${this.baseUrl}/tts/synthesize`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        text: request.text,
        voice: request.voice ?? 'zh-CN-XiaoxiaoNeural',
        language: request.language,
        speed: request.speed ?? 1.0,
        pitch: request.pitch ?? 0,
        output_format: request.output_format ?? 'mp3',
      }),
      signal: AbortSignal.timeout(this.timeout),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new TTSClientError(
        `Voice generation failed: ${response.status} ${response.statusText}`,
        response.status,
        error
      );
    }

    return response.arrayBuffer();
  }

  /**
   * Stream speech synthesis.
   * @param request - Voice generation parameters
   * @returns Async iterable stream of audio chunks
   */
  async *stream(request: VoiceGenerateRequest): AsyncIterable<Uint8Array> {
    const response = await fetch(`${this.baseUrl}/tts/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        text: request.text,
        voice: request.voice ?? 'zh-CN-XiaoxiaoNeural',
        language: request.language,
        speed: request.speed ?? 1.0,
        output_format: request.output_format ?? 'mp3',
      }),
      signal: AbortSignal.timeout(this.timeout),
    });

    if (!response.ok) {
      const error = await response.text();
      throw new TTSClientError(
        `Voice streaming failed: ${response.status} ${response.statusText}`,
        response.status,
        error
      );
    }

    if (!response.body) {
      throw new TTSClientError('Response body is null', 500);
    }

    const reader = response.body.getReader();

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        yield value;
      }
    } finally {
      reader.releaseLock();
    }
  }

  /**
   * List available voices.
   * @param language - Optional language filter
   */
  async listVoices(language?: string): Promise<Voice[]> {
    const url = new URL(`${this.baseUrl}/tts/voices`);
    if (language) {
      url.searchParams.set('language', language);
    }

    const response = await fetch(url.toString(), {
      signal: AbortSignal.timeout(this.timeout),
    });

    if (!response.ok) {
      throw new TTSClientError(
        `Failed to list voices: ${response.status}`,
        response.status
      );
    }

    return response.json() as Promise<Voice[]>;
  }

  /**
   * Get current provider information.
   */
  async getProvider(): Promise<ProviderInfo> {
    const response = await fetch(`${this.baseUrl}/tts/provider`, {
      signal: AbortSignal.timeout(this.timeout),
    });

    if (!response.ok) {
      throw new TTSClientError(
        `Failed to get provider: ${response.status}`,
        response.status
      );
    }

    return response.json() as Promise<ProviderInfo>;
  }

  /**
   * Health check for TTS service.
   */
  async health(): Promise<HealthResponse> {
    const response = await fetch(`${this.baseUrl}/tts/health`, {
      signal: AbortSignal.timeout(this.timeout),
    });

    if (!response.ok) {
      throw new TTSClientError(
        `Health check failed: ${response.status}`,
        response.status
      );
    }

    return response.json() as Promise<HealthResponse>;
  }
}

export class TTSClientError extends Error {
  constructor(
    message: string,
    public readonly statusCode: number,
    public readonly response?: string
  ) {
    super(message);
    this.name = 'TTSClientError';
  }
}

/** Popular Edge TTS voice presets */
export const EDGE_VOICES = {
  ZH_XIAOXIAO: 'zh-CN-XiaoxiaoNeural',
  ZH_YUNXI: 'zh-CN-YunxiNeural',
  ZH_YUNYANG: 'zh-CN-YunyangNeural',
  EN_JENNY: 'en-US-JennyNeural',
  EN_GUY: 'en-US-GuyNeural',
  EN_ARIA: 'en-US-AriaNeural',
  EN_SONIA: 'en-GB-SoniaNeural',
  JA_NANAMI: 'ja-JP-NanamiNeural',
  JA_KEITA: 'ja-JP-KeitaNeural',
  KO_SUNHI: 'ko-KR-SunHiNeural',
  FR_DENISE: 'fr-FR-DeniseNeural',
  DE_KATJA: 'de-DE-KatjaNeural',
  ES_ELVIRA: 'es-ES-ElviraNeural',
} as const;

export type EdgeVoiceId = (typeof EDGE_VOICES)[keyof typeof EDGE_VOICES];
