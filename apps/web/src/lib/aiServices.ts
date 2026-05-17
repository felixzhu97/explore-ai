/**
 * AI Services API Client
 * Unified client for text, image, and speech services
 */

const TEXT_SERVICE_URL = import.meta.env.VITE_TEXT_SERVICE_URL || 'http://localhost:8006';
const VISION_SERVICE_URL = import.meta.env.VITE_VISION_SERVICE_URL || 'http://localhost:8000';
const SPEECH_SERVICE_URL = import.meta.env.VITE_SPEECH_SERVICE_URL || 'http://localhost:8013';

// ==================== Service Configuration ====================

export interface ServiceConfig {
  textService: string;
  visionService: string;
  speechService: string;
}

export interface ServiceStatus {
  name: string;
  url: string;
  status: 'online' | 'offline' | 'degraded';
  message?: string;
  details?: Record<string, unknown>;
}

export function getServiceConfig(): ServiceConfig {
  return {
    textService: TEXT_SERVICE_URL,
    visionService: VISION_SERVICE_URL,
    speechService: SPEECH_SERVICE_URL,
  };
}

export async function checkAllServicesHealth(): Promise<ServiceStatus[]> {
  const results = await Promise.allSettled([
    checkTextServiceHealth(),
    checkVisionServiceHealth(),
    checkSpeechServiceHealth(),
  ]);

  return [
    results[0].status === 'fulfilled' 
      ? results[0].value 
      : { name: 'Text Service', url: TEXT_SERVICE_URL, status: 'offline', message: 'Unavailable' },
    results[1].status === 'fulfilled' 
      ? results[1].value 
      : { name: 'Vision Service', url: VISION_SERVICE_URL, status: 'offline', message: 'Unavailable' },
    results[2].status === 'fulfilled' 
      ? results[2].value 
      : { name: 'Speech Service', url: SPEECH_SERVICE_URL, status: 'offline', message: 'Unavailable' },
  ];
}

async function checkTextServiceHealth(): Promise<ServiceStatus> {
  try {
    const response = await fetch(`${TEXT_SERVICE_URL}/api/text/health`, { 
      signal: AbortSignal.timeout(5000) 
    });
    if (response.ok) {
      const data = await response.json();
      return {
        name: 'Text Service',
        url: TEXT_SERVICE_URL,
        status: 'online',
        details: data,
      };
    }
    return {
      name: 'Text Service',
      url: TEXT_SERVICE_URL,
      status: 'degraded',
      message: `HTTP ${response.status}`,
    };
  } catch (error) {
    return {
      name: 'Text Service',
      url: TEXT_SERVICE_URL,
      status: 'offline',
      message: error instanceof Error ? error.message : 'Connection failed',
    };
  }
}

async function checkVisionServiceHealth(): Promise<ServiceStatus> {
  try {
    const response = await fetch(`${VISION_SERVICE_URL}/health`, { 
      signal: AbortSignal.timeout(5000) 
    });
    if (response.ok) {
      const data = await response.json();
      return {
        name: 'Vision Service',
        url: VISION_SERVICE_URL,
        status: 'online',
        details: data,
      };
    }
    return {
      name: 'Vision Service',
      url: VISION_SERVICE_URL,
      status: 'degraded',
      message: `HTTP ${response.status}`,
    };
  } catch (error) {
    return {
      name: 'Vision Service',
      url: VISION_SERVICE_URL,
      status: 'offline',
      message: error instanceof Error ? error.message : 'Connection failed',
    };
  }
}

async function checkSpeechServiceHealth(): Promise<ServiceStatus> {
  try {
    const response = await fetch(`${SPEECH_SERVICE_URL}/tts/health`, { 
      signal: AbortSignal.timeout(5000) 
    });
    if (response.ok) {
      const data = await response.json();
      return {
        name: 'Speech Service',
        url: SPEECH_SERVICE_URL,
        status: data.provider_status === 'healthy' ? 'online' : 'degraded',
        details: data,
      };
    }
    return {
      name: 'Speech Service',
      url: SPEECH_SERVICE_URL,
      status: 'degraded',
      message: `HTTP ${response.status}`,
    };
  } catch (error) {
    return {
      name: 'Speech Service',
      url: SPEECH_SERVICE_URL,
      status: 'offline',
      message: error instanceof Error ? error.message : 'Connection failed',
    };
  }
}

// ==================== Types ====================

export interface ModelInfo {
  name: string;
  provider: string;
  description?: string;
  max_tokens?: number;
}

export interface ProviderInfo {
  name: string;
  display_name: string;
  models: string[];
  status: 'available' | 'configured' | 'unavailable';
  supported_languages?: string[];
  features?: string[];
}

export interface ChatMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface ChatRequest {
  messages: ChatMessage[];
  session_id?: string;
  system_prompt?: string;
  temperature?: number;
  max_tokens?: number;
  provider?: string;
  model?: string;
}

export interface ChatResponse {
  text: string;
  provider: string;
  model: string;
  session_id: string;
  usage?: Record<string, number>;
  finish_reason?: string;
}

export interface ImageGenerationRequest {
  prompt: string;
  negative_prompt?: string;
  width?: number;
  height?: number;
  num_inference_steps?: number;
  guidance_scale?: number;
  seed?: number;
  num_images?: number;
  style_preset?: string;
}

export interface ImageGenerationResponse {
  images: string[];
  seed: number;
  model: string;
  prompt: string;
  inference_steps: number;
  guidance_scale: number;
  width: number;
  height: number;
  processing_time_ms: number;
  metadata?: Record<string, unknown>;
}

export interface Voice {
  id: string;
  name: string;
  language: string;
  language_name?: string;
  gender?: string;
  provider: string;
  is_default: boolean;
}

export interface SynthesizeRequest {
  text: string;
  voice?: string;
  language?: string;
  speed?: number;
  pitch?: number;
  output_format?: 'mp3' | 'wav' | 'ogg' | 'flac';
}

export interface HealthResponse {
  status: string;
  provider: string;
  model?: string;
  version: string;
}

// ==================== Text Service (Chat) ====================

export async function chat(request: ChatRequest): Promise<ChatResponse> {
  const response = await fetch(`${TEXT_SERVICE_URL}/api/text/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ detail: 'Chat failed' }));
    throw new Error(error.detail || `HTTP ${response.status}`);
  }

  return response.json();
}

export async function chatStream(
  request: ChatRequest,
  onChunk: (text: string) => void,
  onDone?: () => void,
  onError?: (error: Error) => void
): Promise<void> {
  const response = await fetch(`${TEXT_SERVICE_URL}/api/text/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ detail: 'Chat stream failed' }));
    throw new Error(error.detail || `HTTP ${response.status}`);
  }

  const reader = response.body?.getReader();
  if (!reader) throw new Error('Response body not available');

  const decoder = new TextDecoder();
  let buffer = '';
  let currentEvent = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      if (line.startsWith('event: ')) {
        currentEvent = line.slice(7).trim();
      } else if (line.startsWith('data: ')) {
        const data = line.slice(6);
        
        if (data === '[DONE]') {
          onDone?.();
          break;
        }
        
        if (currentEvent === 'done') {
          onDone?.();
        } else if (currentEvent === 'error') {
          try {
            const errorData = JSON.parse(data);
            onError?.(new Error(errorData.error || 'Stream error'));
          } catch {
            onError?.(new Error('Stream error'));
          }
        } else if (!currentEvent || currentEvent === 'meta') {
          // This is a token
          try {
            const parsed = JSON.parse(data);
            if (parsed.token) {
              onChunk(parsed.token);
            }
          } catch {
            // Skip non-JSON data
          }
        }
      } else if (line.trim() === '') {
        currentEvent = '';
      }
    }
  }
}

export async function getTextServiceHealth(): Promise<HealthResponse> {
  const response = await fetch(`${TEXT_SERVICE_URL}/api/text/health`);
  if (!response.ok) throw new Error('Text service unavailable');
  return response.json();
}

export async function getModels(provider?: string): Promise<ModelInfo[]> {
  const url = provider 
    ? `${TEXT_SERVICE_URL}/api/text/models?provider=${encodeURIComponent(provider)}`
    : `${TEXT_SERVICE_URL}/api/text/models`;
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch models');
  return response.json();
}

export async function getProviders(): Promise<ProviderInfo[]> {
  const response = await fetch(`${TEXT_SERVICE_URL}/api/text/providers`);
  if (!response.ok) throw new Error('Failed to fetch providers');
  return response.json();
}

// ==================== Vision Service (Image Generation) ====================

export async function generateImage(request: ImageGenerationRequest): Promise<ImageGenerationResponse> {
  const response = await fetch(`${VISION_SERVICE_URL}/image-gen/generate`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ detail: 'Image generation failed' }));
    throw new Error(error.detail || `HTTP ${response.status}`);
  }

  const data = await response.json();
  // Map num_inference_steps from media-gen service to inference_steps expected by frontend
  return {
    ...data,
    inference_steps: data.num_inference_steps ?? data.inference_steps ?? 30,
  };
}

export async function getVisionServiceHealth(): Promise<{
  status: string;
  device: string;
  cuda_available: boolean;
}> {
  const response = await fetch(`${VISION_SERVICE_URL}/health`);
  if (!response.ok) throw new Error('Vision service unavailable');
  return response.json();
}

// ==================== Speech Service (TTS) ====================

export async function synthesizeSpeech(request: SynthesizeRequest): Promise<Blob> {
  const response = await fetch(`${SPEECH_SERVICE_URL}/tts/synthesize`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ detail: 'Synthesis failed' }));
    throw new Error(error.detail || `HTTP ${response.status}`);
  }

  return response.blob();
}

export async function streamSpeech(
  request: SynthesizeRequest,
  onChunk?: (chunk: ArrayBuffer) => void
): Promise<void> {
  const response = await fetch(`${SPEECH_SERVICE_URL}/tts/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const error = await response.json().catch(() => ({ detail: 'Streaming failed' }));
    throw new Error(error.detail || `HTTP ${response.status}`);
  }

  const reader = response.body?.getReader();
  if (!reader) throw new Error('Response body not available');

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    onChunk?.(value.buffer);
  }
}

export async function getVoices(language?: string): Promise<Voice[]> {
  const url = language 
    ? `${SPEECH_SERVICE_URL}/tts/voices?language=${encodeURIComponent(language)}`
    : `${SPEECH_SERVICE_URL}/tts/voices`;
    
  const response = await fetch(url);
  if (!response.ok) throw new Error('Failed to fetch voices');
  return response.json();
}

export async function getSpeechServiceHealth(): Promise<{
  status: string;
  provider: string;
  provider_status: string;
  version: string;
}> {
  const response = await fetch(`${SPEECH_SERVICE_URL}/tts/health`);
  if (!response.ok) throw new Error('Speech service unavailable');
  return response.json();
}

export async function getSpeechProviders(): Promise<ProviderInfo[]> {
  const response = await fetch(`${SPEECH_SERVICE_URL}/tts/providers`);
  if (!response.ok) throw new Error('Failed to fetch providers');
  return response.json();
}

// ==================== Utility Functions ====================

export function base64ToBlob(base64: string, mimeType: string = 'image/png'): Blob {
  const byteCharacters = atob(base64);
  const byteNumbers = new Array(byteCharacters.length);
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i);
  }
  const byteArray = new Uint8Array(byteNumbers);
  return new Blob([byteArray], { type: mimeType });
}

export function downloadBlob(blob: Blob, filename: string): void {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

export function downloadBase64Image(base64: string, filename: string = 'image.png'): void {
  const blob = base64ToBlob(base64);
  downloadBlob(blob, filename);
}
