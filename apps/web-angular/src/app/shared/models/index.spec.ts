import { describe, it, expect } from 'vitest';
import {
  ChatMessage,
  Agent,
  ImageAnalysisResult,
  DetectedObject,
  Document,
  RAGSource,
  ImageGenerationRequest,
  ImageGenerationResult,
  TTSRequest,
  TTSResult,
  ServiceStatus,
} from './index';

describe('shared/models', () => {
  describe('ChatMessage', () => {
    it('should define required properties', () => {
      const message: ChatMessage = {
        id: 'msg-1',
        role: 'user',
        content: 'Hello',
        timestamp: new Date(),
      };

      expect(message.id).toBe('msg-1');
      expect(message.role).toBe('user');
      expect(message.content).toBe('Hello');
      expect(message.timestamp).toBeInstanceOf(Date);
    });

    it('should accept all valid roles', () => {
      const roles: ChatMessage['role'][] = ['user', 'assistant', 'system'];
      roles.forEach((role) => {
        const message: ChatMessage = {
          id: '1',
          role,
          content: 'test',
          timestamp: new Date(),
        };
        expect(message.role).toBe(role);
      });
    });

    it('should allow optional isLoading property', () => {
      const message: ChatMessage = {
        id: 'msg-1',
        role: 'assistant',
        content: 'Thinking...',
        timestamp: new Date(),
        isLoading: true,
      };

      expect(message.isLoading).toBe(true);
    });
  });

  describe('Agent', () => {
    it('should define required properties', () => {
      const agent: Agent = {
        id: 'agent-1',
        name: 'K8s Agent',
        description: 'Manages Kubernetes clusters',
        endpoint: '/api/agents/k8s',
      };

      expect(agent.id).toBe('agent-1');
      expect(agent.name).toBe('K8s Agent');
      expect(agent.description).toBe('Manages Kubernetes clusters');
      expect(agent.endpoint).toBe('/api/agents/k8s');
    });

    it('should allow optional icon property', () => {
      const agent: Agent = {
        id: 'agent-1',
        name: 'K8s Agent',
        description: 'Manages Kubernetes clusters',
        endpoint: '/api/agents/k8s',
        icon: 'kubernetes',
      };

      expect(agent.icon).toBe('kubernetes');
    });
  });

  describe('DetectedObject', () => {
    it('should define required properties', () => {
      const obj: DetectedObject = {
        class: 'person',
        confidence: 0.95,
        bbox: [100, 200, 50, 100],
      };

      expect(obj.class).toBe('person');
      expect(obj.confidence).toBe(0.95);
      expect(obj.bbox).toHaveLength(4);
      expect(obj.bbox[0]).toBe(100);
      expect(obj.bbox[1]).toBe(200);
      expect(obj.bbox[2]).toBe(50);
      expect(obj.bbox[3]).toBe(100);
    });

    it('should have bbox as [x, y, width, height]', () => {
      const obj: DetectedObject = {
        class: 'car',
        confidence: 0.87,
        bbox: [0, 0, 1920, 1080],
      };

      expect(obj.bbox[0]).toBeLessThanOrEqual(obj.bbox[2]);
      expect(obj.bbox[1]).toBeLessThanOrEqual(obj.bbox[3]);
    });
  });

  describe('ImageAnalysisResult', () => {
    it('should allow optional caption', () => {
      const result: ImageAnalysisResult = {
        caption: 'A sunset over mountains',
      };

      expect(result.caption).toBe('A sunset over mountains');
    });

    it('should allow optional objects array', () => {
      const result: ImageAnalysisResult = {
        objects: [
          { class: 'person', confidence: 0.9, bbox: [0, 0, 100, 200] },
          { class: 'car', confidence: 0.8, bbox: [100, 50, 150, 100] },
        ],
      };

      expect(result.objects).toHaveLength(2);
    });

    it('should allow optional text (OCR result)', () => {
      const result: ImageAnalysisResult = {
        text: 'Hello World!',
      };

      expect(result.text).toBe('Hello World!');
    });

    it('should allow all properties together', () => {
      const result: ImageAnalysisResult = {
        caption: 'Test image',
        objects: [{ class: 'dog', confidence: 0.95, bbox: [10, 20, 50, 60] }],
        text: 'Sample text',
      };

      expect(result.caption).toBeDefined();
      expect(result.objects).toBeDefined();
      expect(result.text).toBeDefined();
    });
  });

  describe('Document', () => {
    it('should define required properties', () => {
      const doc: Document = {
        id: 'doc-1',
        name: 'report.pdf',
        size: 1024000,
        uploadedAt: new Date('2024-01-15'),
      };

      expect(doc.id).toBe('doc-1');
      expect(doc.name).toBe('report.pdf');
      expect(doc.size).toBe(1024000);
      expect(doc.uploadedAt).toBeInstanceOf(Date);
    });

    it('should handle size in bytes', () => {
      const smallDoc: Document = {
        id: 'small',
        name: 'tiny.txt',
        size: 100,
        uploadedAt: new Date(),
      };

      const largeDoc: Document = {
        id: 'large',
        name: 'huge.zip',
        size: 1073741824,
        uploadedAt: new Date(),
      };

      expect(smallDoc.size).toBeLessThan(largeDoc.size);
    });
  });

  describe('RAGSource', () => {
    it('should define required properties', () => {
      const source: RAGSource = {
        documentId: 'doc-1',
        documentName: 'annual-report.pdf',
        content: 'Revenue grew by 20%...',
        similarity: 0.85,
      };

      expect(source.documentId).toBe('doc-1');
      expect(source.documentName).toBe('annual-report.pdf');
      expect(source.content).toBe('Revenue grew by 20%...');
      expect(source.similarity).toBe(0.85);
    });

    it('should allow optional pageNumber', () => {
      const source: RAGSource = {
        documentId: 'doc-1',
        documentName: 'report.pdf',
        content: 'Page 42 content',
        similarity: 0.9,
        pageNumber: 42,
      };

      expect(source.pageNumber).toBe(42);
    });

    it('should have similarity between 0 and 1', () => {
      const sources: RAGSource[] = [
        { documentId: '1', documentName: 'a', content: 'x', similarity: 0.0 },
        { documentId: '2', documentName: 'b', content: 'y', similarity: 0.5 },
        { documentId: '3', documentName: 'c', content: 'z', similarity: 1.0 },
      ];

      sources.forEach((s) => {
        expect(s.similarity).toBeGreaterThanOrEqual(0);
        expect(s.similarity).toBeLessThanOrEqual(1);
      });
    });
  });

  describe('ImageGenerationRequest', () => {
    it('should define required properties', () => {
      const request: ImageGenerationRequest = {
        prompt: 'A beautiful sunset',
        width: 1024,
        height: 1024,
      };

      expect(request.prompt).toBe('A beautiful sunset');
      expect(request.width).toBe(1024);
      expect(request.height).toBe(1024);
    });

    it('should allow optional negativePrompt', () => {
      const request: ImageGenerationRequest = {
        prompt: 'A cat',
        negativePrompt: 'blurry, low quality',
        width: 512,
        height: 512,
      };

      expect(request.negativePrompt).toBe('blurry, low quality');
    });
  });

  describe('ImageGenerationResult', () => {
    it('should define required properties', () => {
      const result: ImageGenerationResult = {
        imageUrl: 'https://example.com/generated.png',
      };

      expect(result.imageUrl).toBe('https://example.com/generated.png');
    });

    it('should allow optional seed for reproducibility', () => {
      const result: ImageGenerationResult = {
        imageUrl: 'https://example.com/image.png',
        seed: 12345,
      };

      expect(result.seed).toBe(12345);
    });
  });

  describe('TTSRequest', () => {
    it('should define required properties', () => {
      const request: TTSRequest = {
        text: 'Hello, world!',
        voice: 'en-US-Neural',
        speed: 1.0,
      };

      expect(request.text).toBe('Hello, world!');
      expect(request.voice).toBe('en-US-Neural');
      expect(request.speed).toBe(1.0);
    });

    it('should support different speed values', () => {
      const slowRequest: TTSRequest = {
        text: 'Speaking slowly',
        voice: 'en-US',
        speed: 0.5,
      };

      const fastRequest: TTSRequest = {
        text: 'Speaking fast',
        voice: 'en-US',
        speed: 2.0,
      };

      expect(slowRequest.speed).toBeLessThan(fastRequest.speed);
    });
  });

  describe('TTSResult', () => {
    it('should define required properties', () => {
      const result: TTSResult = {
        audioUrl: 'https://example.com/audio.mp3',
      };

      expect(result.audioUrl).toBe('https://example.com/audio.mp3');
    });

    it('should allow optional duration', () => {
      const result: TTSResult = {
        audioUrl: 'https://example.com/audio.mp3',
        duration: 5.5,
      };

      expect(result.duration).toBe(5.5);
    });
  });

  describe('ServiceStatus', () => {
    it('should define required properties', () => {
      const status: ServiceStatus = {
        name: 'Vision Service',
        status: 'online',
      };

      expect(status.name).toBe('Vision Service');
      expect(status.status).toBe('online');
    });

    it('should accept all valid statuses', () => {
      const statuses: ServiceStatus['status'][] = ['online', 'offline', 'error'];

      statuses.forEach((s) => {
        const status: ServiceStatus = {
          name: 'Test Service',
          status: s,
        };
        expect(status.status).toBe(s);
      });
    });

    it('should allow optional latency', () => {
      const status: ServiceStatus = {
        name: 'Fast Service',
        status: 'online',
        latency: 50,
      };

      expect(status.latency).toBe(50);
    });
  });

  describe('type export completeness', () => {
    // TypeScript interfaces are compile-time constructs
    // We verify the types work by using them directly in other tests
    // These tests confirm the module exports are valid TypeScript interfaces

    it('should export all interface types that can be used in type annotations', () => {
      // ChatMessage interface validation
      const validChatMessage: ChatMessage = {
        id: 'test-1',
        role: 'user',
        content: 'Hello',
        timestamp: new Date(),
      };
      expect(validChatMessage.id).toBe('test-1');

      // Agent interface validation
      const validAgent: Agent = {
        id: 'agent-1',
        name: 'Test Agent',
        description: 'Test',
        endpoint: '/api/test',
      };
      expect(validAgent.id).toBe('agent-1');

      // ImageAnalysisResult interface validation
      const validImageResult: ImageAnalysisResult = {
        caption: 'Test caption',
      };
      expect(validImageResult.caption).toBe('Test caption');

      // DetectedObject interface validation
      const validDetectedObject: DetectedObject = {
        class: 'person',
        confidence: 0.9,
        bbox: [0, 0, 100, 100],
      };
      expect(validDetectedObject.class).toBe('person');

      // Document interface validation
      const validDocument: Document = {
        id: 'doc-1',
        name: 'test.pdf',
        size: 1024,
        uploadedAt: new Date(),
      };
      expect(validDocument.name).toBe('test.pdf');

      // RAGSource interface validation
      const validRAGSource: RAGSource = {
        documentId: 'doc-1',
        documentName: 'test.pdf',
        content: 'Test content',
        similarity: 0.8,
      };
      expect(validRAGSource.similarity).toBe(0.8);

      // ImageGenerationRequest interface validation
      const validImageGenReq: ImageGenerationRequest = {
        prompt: 'Test prompt',
        width: 512,
        height: 512,
      };
      expect(validImageGenReq.width).toBe(512);

      // ImageGenerationResult interface validation
      const validImageGenRes: ImageGenerationResult = {
        imageUrl: 'https://example.com/image.png',
      };
      expect(validImageGenRes.imageUrl).toBe('https://example.com/image.png');

      // TTSRequest interface validation
      const validTTSReq: TTSRequest = {
        text: 'Test text',
        voice: 'en-US',
        speed: 1.0,
      };
      expect(validTTSReq.text).toBe('Test text');

      // TTSResult interface validation
      const validTTSRes: TTSResult = {
        audioUrl: 'https://example.com/audio.mp3',
      };
      expect(validTTSRes.audioUrl).toBe('https://example.com/audio.mp3');

      // ServiceStatus interface validation
      const validServiceStatus: ServiceStatus = {
        name: 'Test Service',
        status: 'online',
      };
      expect(validServiceStatus.status).toBe('online');
    });
  });
});
