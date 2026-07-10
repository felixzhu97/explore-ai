import { describe, expect, it } from 'vitest';
import type {
  ChatStreamRequest,
  ProviderInfo,
  RagQuery,
  SourceDocument,
  DocumentListItem,
  ImageGenerateParams,
  VisionResult,
  Detection,
  TtsRequest,
  Voice,
} from './index';

describe('Shared domain models', () => {
  it('should accept aligned chat stream request fields', () => {
    const request: ChatStreamRequest = {
      messages: [{ role: 'user', content: 'Hello' }],
      sessionId: 'session-1',
      provider: 'openai',
      model: 'gpt-4o-mini',
      toolsEnabled: true,
    };

    expect(request.sessionId).toBe('session-1');
    expect(request.toolsEnabled).toBe(true);
  });

  it('should accept aligned provider info fields', () => {
    const provider: ProviderInfo = {
      name: 'openai',
      displayName: 'OpenAI',
      models: ['gpt-4o'],
      status: 'available',
    };

    expect(provider.displayName).toBe('OpenAI');
  });

  it('should accept aligned rag query fields', () => {
    const query: RagQuery = {
      query: 'What is RAG?',
      sessionId: 'session-1',
      topK: 5,
      docIds: ['doc-1'],
      images: [],
    };

    expect(query.docIds).toEqual(['doc-1']);
  });

  it('should accept aligned document list item fields', () => {
    const document: DocumentListItem = {
      id: 'doc-1',
      title: 'Guide.pdf',
      status: 'READY',
      chunkCount: 12,
    };

    expect(document.chunkCount).toBe(12);
  });

  it('should accept aligned source document fields', () => {
    const source: SourceDocument = {
      text: 'excerpt',
      score: 0.92,
      metadata: { title: 'Guide.pdf' },
    };

    expect(source.score).toBe(0.92);
  });

  it('should accept aligned image generate params', () => {
    const params: ImageGenerateParams = {
      prompt: 'A cat',
      model: 'dall-e-3',
      width: 1024,
      height: 1024,
      n: 1,
    };

    expect(params.prompt).toBe('A cat');
  });

  it('should accept aligned vision result fields', () => {
    const detection: Detection = {
      className: 'person',
      confidence: 0.95,
      bbox: [0, 0, 100, 100],
    };
    const result: VisionResult = {
      caption: 'A person',
      detections: [detection],
      fullText: 'Hello',
      processingTimeMs: 120,
    };

    expect(result.fullText).toBe('Hello');
    expect(result.detections?.[0].className).toBe('person');
  });

  it('should accept aligned tts request fields', () => {
    const voice: Voice = {
      id: 'alloy',
      name: 'Alloy',
      language: 'en',
      isDefault: true,
    };
    const request: TtsRequest = {
      text: 'Hello',
      voice: 'alloy',
      speed: 1,
      outputFormat: 'mp3',
    };

    expect(voice.isDefault).toBe(true);
    expect(request.outputFormat).toBe('mp3');
  });
});
