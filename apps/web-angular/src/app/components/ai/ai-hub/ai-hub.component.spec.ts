import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DomSanitizer } from '@angular/platform-browser';
import { AiHubComponent } from './ai-hub.component';
import { ApiService, ProviderInfo, ModelInfo, Voice } from '../services/api.service';
import { I18nService } from '../../../i18n/i18n.service';
import { SegmentedControlComponent } from '../../segmented-control/segmented-control.component';

describe('AiHubComponent', () => {
  let fixture: ComponentFixture<AiHubComponent>;
  let component: AiHubComponent;

  const mockTranslations = {
    aiHub: {
      tabs: { chat: 'Chat', image: 'Image', tts: 'TTS' },
      chat: {
        title: 'AI Chat',
        description: 'Chat with AI models',
        provider: 'Provider',
        model: 'Model',
        inputPlaceholder: 'Type your message...',
        thinking: 'Thinking...',
      },
      image: {
        title: 'Image Generation',
        description: 'Generate images with AI',
        promptLabel: 'Prompt',
        promptPlaceholder: 'Enter your image prompt...',
        negativePromptLabel: 'Negative Prompt',
        negativePromptPlaceholder: 'What to avoid...',
        sizeLabel: 'Size',
        generateButton: 'Generate',
        generating: 'Generating...',
        emptyState: 'No image generated yet',
        download: 'Download',
      },
      tts: {
        title: 'Text to Speech',
        description: 'Convert text to speech',
        textLabel: 'Text',
        textPlaceholder: 'Enter text to synthesize...',
        voiceLabel: 'Voice',
        speedLabel: 'Speed',
        synthesizeButton: 'Synthesize',
        synthesizing: 'Synthesizing...',
        audioReady: 'Audio ready',
        downloadAudio: 'Download',
      },
      quickPrompts: {
        greeting: 'Hello',
        help: 'Help me',
        creative: 'Write a story',
      },
    },
    agents: { startConversation: 'Start a conversation' },
  };

  const mockProviders: ProviderInfo[] = [
    { name: 'openai', display_name: 'OpenAI', models: ['gpt-4o', 'gpt-4o-mini'], status: 'available' },
    { name: 'anthropic', display_name: 'Anthropic Claude', models: ['claude-sonnet-4'], status: 'available' },
  ];

  const mockModels: ModelInfo[] = [
    { name: 'gpt-4o', provider: 'openai' },
    { name: 'gpt-4o-mini', provider: 'openai' },
  ];

  const mockVoices: Voice[] = [
    { id: 'en-US', name: 'English (US)', language: 'en-US', provider: 'default', is_default: true },
    { id: 'en-GB', name: 'English (UK)', language: 'en-GB', provider: 'default', is_default: false },
  ];

  const mockI18nService = {
    language: vi.fn().mockReturnValue('en'),
    t: vi.fn().mockReturnValue(mockTranslations),
  };

  const createMockApiService = () => ({
    getProviders: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
    getModels: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
    chatStream: vi.fn(),
    generateImage: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
    getVoices: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
    synthesizeSpeech: vi.fn().mockReturnValue({ subscribe: vi.fn() }),
    downloadBase64Image: vi.fn(),
    downloadBlob: vi.fn(),
  });

  let mockApiService: ReturnType<typeof createMockApiService>;

  beforeEach(async () => {
    vi.useFakeTimers();

    mockApiService = createMockApiService();

    await TestBed.configureTestingModule({
      imports: [AiHubComponent, SegmentedControlComponent],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: I18nService, useValue: mockI18nService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AiHubComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.restoreAllMocks();
  });

  describe('component creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default tab as chat', () => {
      expect(component.activeTab()).toBe('chat');
    });

    it('should initialize with empty chat messages', () => {
      expect(component.chatMessages()).toEqual([]);
    });

    it('should initialize with empty chat input', () => {
      expect(component.chatInput()).toBe('');
    });

    it('should initialize with isChatLoading as false', () => {
      expect(component.isChatLoading()).toBe(false);
    });

    it('should have correct image sizes', () => {
      expect(component.imageSizes.length).toBe(3);
      expect(component.imageSizes[0].label).toBe('512x512');
      expect(component.imageSizes[2].label).toBe('1024x1024');
    });
  });

  describe('tabs computed', () => {
    it('should return 3 tabs', () => {
      expect(component.tabs().length).toBe(3);
    });

    it('should have chat, image, and tts tabs', () => {
      const tabs = component.tabs();
      expect(tabs.map((t) => t.value)).toEqual(['chat', 'image', 'tts']);
    });
  });

  describe('tab navigation', () => {
    it('should set active tab', () => {
      component.setActiveTab('image');
      expect(component.activeTab()).toBe('image');
    });

    it('should switch from chat to tts', () => {
      component.setActiveTab('tts');
      expect(component.activeTab()).toBe('tts');
    });

    it('should switch from tts to chat', () => {
      component.setActiveTab('tts');
      component.setActiveTab('chat');
      expect(component.activeTab()).toBe('chat');
    });
  });

  describe('chat functionality', () => {
    describe('loadProviders', () => {
      it('should load providers successfully', () => {
        mockApiService.getProviders.mockReturnValue({
          subscribe: vi.fn((callbacks) => {
            callbacks.next(mockProviders);
          }),
        });

        component.loadProviders();

        expect(component.providers()).toEqual(mockProviders);
      });

      it('should load models when providers are loaded', () => {
        mockApiService.getProviders.mockReturnValue({
          subscribe: vi.fn((callbacks) => {
            callbacks.next(mockProviders);
          }),
        });

        component.loadProviders();

        expect(mockApiService.getModels).toHaveBeenCalledWith('openai');
      });
    });

    describe('loadModelsForProvider', () => {
      it('should load models for selected provider', () => {
        mockApiService.getModels.mockReturnValue({
          subscribe: vi.fn((callbacks) => {
            callbacks.next(mockModels);
            callbacks.complete();
          }),
        });

        component.loadModelsForProvider('openai');

        expect(component.models()).toEqual(mockModels);
        expect(component.isLoadingModels()).toBe(false);
      });
    });

    describe('onProviderChange', () => {
      it('should update selected provider and load models', () => {
        mockApiService.getModels.mockReturnValue({
          subscribe: vi.fn((callbacks) => {
            callbacks.next(mockModels);
            callbacks.complete();
          }),
        });

        component.onProviderChange('anthropic');

        expect(component.selectedProvider()).toBe('anthropic');
        expect(mockApiService.getModels).toHaveBeenCalledWith('anthropic');
      });
    });

    describe('setSelectedModel', () => {
      it('should update selected model', () => {
        component.setSelectedModel('gpt-4o');
        expect(component.selectedModel()).toBe('gpt-4o');
      });
    });

    describe('setChatInput', () => {
      it('should update chat input', () => {
        component.setChatInput('Hello world');
        expect(component.chatInput()).toBe('Hello world');
      });

      it('should clear chat input', () => {
        component.setChatInput('Hello');
        component.setChatInput('');
        expect(component.chatInput()).toBe('');
      });
    });

    describe('onChatKeyDown', () => {
      it('should call sendMessage on Enter without shift', () => {
        vi.spyOn(component, 'sendMessage');
        const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: false });
        component.onChatKeyDown(event);
        expect(component.sendMessage).toHaveBeenCalled();
      });

      it('should not call sendMessage on Enter with shift', () => {
        vi.spyOn(component, 'sendMessage');
        const event = new KeyboardEvent('keydown', { key: 'Enter', shiftKey: true });
        component.onChatKeyDown(event);
        expect(component.sendMessage).not.toHaveBeenCalled();
      });
    });

    describe('sendMessage', () => {
      beforeEach(() => {
        component.setChatInput('Test message');
      });

      it('should not send if input is empty', () => {
        component.setChatInput('');
        const chatStreamSpy = vi.spyOn(component['api'], 'chatStream');
        component.sendMessage();
        expect(chatStreamSpy).not.toHaveBeenCalled();
      });

      it('should not send if already loading', () => {
        component.isChatLoading.set(true);
        const chatStreamSpy = vi.spyOn(component['api'], 'chatStream');
        component.sendMessage();
        expect(chatStreamSpy).not.toHaveBeenCalled();
      });

      it('should add user message to chat', () => {
        mockApiService.chatStream = vi.fn();
        component.sendMessage();

        const messages = component.chatMessages();
        const userMessage = messages.find((m) => m.role === 'user');
        expect(userMessage).toBeDefined();
        expect(userMessage?.content).toBe('Test message');
      });

      it('should add assistant placeholder message', () => {
        mockApiService.chatStream = vi.fn();
        component.sendMessage();

        const assistantMessage = component.chatMessages().find((m) => m.role === 'assistant');
        expect(assistantMessage).toBeDefined();
      });

      it('should clear chat input after sending', () => {
        mockApiService.chatStream = vi.fn();
        component.sendMessage();
        expect(component.chatInput()).toBe('');
      });

      it('should call chatStream with correct parameters', () => {
        mockApiService.chatStream = vi.fn();
        component.sendMessage();
        expect(mockApiService.chatStream).toHaveBeenCalled();
      });
    });
  });

  describe('image generation', () => {
    describe('setImagePrompt', () => {
      it('should update image prompt', () => {
        component.setImagePrompt('A beautiful sunset');
        expect(component.imagePrompt()).toBe('A beautiful sunset');
      });
    });

    describe('setImageNegativePrompt', () => {
      it('should update negative prompt', () => {
        component.setImageNegativePrompt('blur, low quality');
        expect(component.imageNegativePrompt()).toBe('blur, low quality');
      });
    });

    describe('setImageSize', () => {
      it('should update image size', () => {
        const newSize = component.imageSizes[0];
        component.setImageSize(newSize);
        expect(component.imageSize()).toBe(newSize);
      });
    });

    describe('generateImage', () => {
      it('should not generate if prompt is empty', () => {
        component.setImagePrompt('');
        const generateImageSpy = vi.spyOn(component['api'], 'generateImage');
        component.generateImage();
        expect(generateImageSpy).not.toHaveBeenCalled();
      });
    });

    describe('downloadImage', () => {
      it('should call downloadBase64Image with correct data', () => {
        component.generatedImage.set('data:image/png;base64,SGVsbG8=');
        component.downloadImage();
        expect(mockApiService.downloadBase64Image).toHaveBeenCalledWith(
          'SGVsbG8=',
          expect.stringContaining('ai_generated_')
        );
      });

      it('should not download if no image', () => {
        component.generatedImage.set(null);
        component.downloadImage();
        expect(mockApiService.downloadBase64Image).not.toHaveBeenCalled();
      });
    });

    describe('zoomImage', () => {
      it('should set zoomed image', () => {
        component.zoomImage('image-url');
        expect(component.zoomedImage()).toBe('image-url');
      });
    });

    describe('closeZoom', () => {
      it('should clear zoomed image', () => {
        component.zoomedImage.set('some-image');
        component.closeZoom();
        expect(component.zoomedImage()).toBeNull();
      });
    });
  });

  describe('TTS functionality', () => {
    describe('loadVoices', () => {
      it('should load voices successfully', () => {
        mockApiService.getVoices.mockReturnValue({
          subscribe: vi.fn((callbacks) => {
            callbacks.next(mockVoices);
          }),
        });

        component.loadVoices();
        expect(component.availableVoices()).toEqual(mockVoices);
      });

      it('should set default voice', () => {
        mockApiService.getVoices.mockReturnValue({
          subscribe: vi.fn((callbacks) => {
            callbacks.next(mockVoices);
          }),
        });

        component.loadVoices();
        expect(component.ttsVoice()).toBe('en-US');
      });
    });

    describe('setTtsText', () => {
      it('should update TTS text', () => {
        component.setTtsText('Hello world');
        expect(component.ttsText()).toBe('Hello world');
      });
    });

    describe('setTtsVoice', () => {
      it('should update TTS voice', () => {
        component.setTtsVoice('en-GB');
        expect(component.ttsVoice()).toBe('en-GB');
      });
    });

    describe('setTtsSpeed', () => {
      it('should update TTS speed', () => {
        component.setTtsSpeed(1.5);
        expect(component.ttsSpeed()).toBe(1.5);
      });
    });

    describe('downloadAudio', () => {
      it('should call downloadBlob with correct data', () => {
        const mockBlob = new Blob(['audio'], { type: 'audio/mp3' });
        component.audioBlob.set(mockBlob);
        component.downloadAudio();
        expect(mockApiService.downloadBlob).toHaveBeenCalledWith(
          mockBlob,
          expect.stringContaining('speech_')
        );
      });

      it('should not download if no blob', () => {
        component.audioBlob.set(null);
        component.downloadAudio();
        expect(mockApiService.downloadBlob).not.toHaveBeenCalled();
      });
    });
  });

  describe('utilities', () => {
    describe('formatTime', () => {
      it('should format timestamp to time string', () => {
        const timestamp = new Date('2024-01-01T12:30:00').getTime();
        const formatted = component.formatTime(timestamp);
        expect(formatted).toContain('12:30');
      });
    });

    describe('renderMarkdown', () => {
      it('should render headers', () => {
        const html = component.renderMarkdown('# Header');
        expect(html).toContain('<h1>Header</h1>');
      });

      it('should render bold text', () => {
        const html = component.renderMarkdown('**bold**');
        expect(html).toContain('<strong>bold</strong>');
      });

      it('should render italic text', () => {
        const html = component.renderMarkdown('*italic*');
        expect(html).toContain('<em>italic</em>');
      });

      it('should render inline code', () => {
        const html = component.renderMarkdown('`code`');
        expect(html).toContain('<code>code</code>');
      });

      it('should render code blocks', () => {
        const html = component.renderMarkdown('```js\nconsole.log("hi")\n```');
        expect(html).toContain('<pre><code>');
      });

      it('should render blockquotes', () => {
        const html = component.renderMarkdown('> Quote');
        expect(html).toContain('<blockquote>Quote</blockquote>');
      });

      it('should render lists', () => {
        const html = component.renderMarkdown('- Item 1\n- Item 2');
        expect(html).toContain('<li>Item 1</li>');
      });
    });
  });

  describe('ngOnDestroy', () => {
    it('should abort chat controller', () => {
      component['chatAbortController'] = { abort: vi.fn() } as any;
      component.ngOnDestroy();
      expect((component['chatAbortController'] as any).abort).toHaveBeenCalled();
    });

    it('should pause audio element', () => {
      const mockAudio = { pause: vi.fn() } as any;
      component.audioElement = mockAudio;
      component.ngOnDestroy();
      expect(mockAudio.pause).toHaveBeenCalled();
    });
  });

  describe('template rendering', () => {
    it('should render tab header', () => {
      fixture.detectChanges();
      const tabHeader = fixture.nativeElement.querySelector('.tab-header');
      expect(tabHeader).toBeTruthy();
    });

    it('should render chat panel when chat tab is active', () => {
      fixture.detectChanges();
      const chatPanel = fixture.nativeElement.querySelector('.panel');
      expect(chatPanel).toBeTruthy();
    });

    it('should show empty state when no messages', () => {
      fixture.detectChanges();
      const emptyState = fixture.nativeElement.querySelector('.empty-state');
      expect(emptyState).toBeTruthy();
    });

    it('should show quick action buttons', () => {
      fixture.detectChanges();
      const quickActions = fixture.nativeElement.querySelectorAll('.quick-action');
      expect(quickActions.length).toBe(3);
    });
  });
});
