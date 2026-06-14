import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { TtsTabComponent } from './tts-tab.component';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@i18n';
import { of, throwError } from 'rxjs';

describe('TtsTabComponent', () => {
  let fixture: ComponentFixture<TtsTabComponent>;
  let component: TtsTabComponent;
  let mockApiService: Partial<ApiService>;

  const mockI18nService = {
    t: () => ({
      aiHub: {
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
      },
    }),
  };

  const createMockApiService = () => {
    mockApiService = {
      getVoices: vi.fn().mockReturnValue(
        of([
          {
            id: 'en-US',
            name: 'English (US)',
            language: 'en-US',
            provider: 'default',
            is_default: true,
          },
          {
            id: 'en-GB',
            name: 'English (UK)',
            language: 'en-GB',
            provider: 'default',
            is_default: false,
          },
          {
            id: 'zh-CN',
            name: 'Chinese',
            language: 'zh-CN',
            provider: 'default',
            is_default: false,
          },
        ])
      ),
      synthesizeSpeech: vi.fn().mockReturnValue(of(new Blob(['audio'], { type: 'audio/mp3' }))),
      downloadBlob: vi.fn(),
    };
    return mockApiService;
  };

  const createFixture = () => {
    fixture = TestBed.createComponent(TtsTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    createMockApiService();
    await TestBed.configureTestingModule({
      imports: [TtsTabComponent, HttpClientTestingModule],
      providers: [
        { provide: ApiService, useValue: mockApiService },
        { provide: I18nService, useValue: mockI18nService },
      ],
    }).compileComponents();
  });

  describe('component creation', () => {
    it('should create', () => {
      createFixture();
      expect(component).toBeTruthy();
    });

    it('should initialize with empty text', () => {
      createFixture();
      expect(component.text()).toBe('');
    });

    it('should initialize with default voice', () => {
      createFixture();
      expect(component.voice()).toBeTruthy();
    });

    it('should initialize with default speed', () => {
      createFixture();
      expect(component.speed()).toBe(1.0);
    });

    it('should initialize isSynthesizing as false', () => {
      createFixture();
      expect(component.isSynthesizing()).toBe(false);
    });

    it('should initialize error as null', () => {
      createFixture();
      expect(component.error()).toBeNull();
    });

    it('should initialize audioUrl as null', () => {
      createFixture();
      expect(component.audioUrl()).toBeNull();
    });

    it('should initialize isPlaying as false', () => {
      createFixture();
      expect(component.isPlaying()).toBe(false);
    });
  });

  describe('ngOnInit', () => {
    it('should call loadVoices on init', () => {
      createFixture();
      expect(mockApiService.getVoices).toHaveBeenCalled();
    });

    it('should set available voices from API', () => {
      createFixture();
      expect(component.availableVoices().length).toBeGreaterThan(0);
    });

    it('should set default voice', () => {
      createFixture();
      expect(component.voice()).toBeTruthy();
    });
  });

  describe('ngOnDestroy', () => {
    it('should pause audio element if playing', () => {
      createFixture();
      const mockAudio = {
        pause: vi.fn(),
      };
      (component as any).audioElement = mockAudio;

      component.ngOnDestroy();

      expect(mockAudio.pause).toHaveBeenCalled();
    });

    it('should revoke object URL if exists', () => {
      createFixture();
      const revokeSpy = vi.spyOn(URL, 'revokeObjectURL');
      component.audioUrl.set('blob:http://localhost/test');
      (component as any).audioElement = null;

      component.ngOnDestroy();

      expect(revokeSpy).toHaveBeenCalled();
    });
  });

  describe('setText', () => {
    it('should set text value', () => {
      createFixture();
      component.setText('Hello world');
      expect(component.text()).toBe('Hello world');
    });

    it('should handle empty string', () => {
      createFixture();
      component.setText('');
      expect(component.text()).toBe('');
    });
  });

  describe('setVoice', () => {
    it('should set voice value', () => {
      createFixture();
      component.setVoice('en-GB');
      expect(component.voice()).toBe('en-GB');
    });
  });

  describe('setSpeed', () => {
    it('should set speed value', () => {
      createFixture();
      component.setSpeed(1.5);
      expect(component.speed()).toBe(1.5);
    });

    it('should handle minimum speed', () => {
      createFixture();
      component.setSpeed(0.5);
      expect(component.speed()).toBe(0.5);
    });

    it('should handle maximum speed', () => {
      createFixture();
      component.setSpeed(2.0);
      expect(component.speed()).toBe(2.0);
    });
  });

  describe('synthesize', () => {
    it('should not synthesize if text is empty', () => {
      createFixture();
      component.text.set('');
      component.synthesize();
      expect(mockApiService.synthesizeSpeech).not.toHaveBeenCalled();
    });

    it('should not synthesize if text is whitespace only', () => {
      createFixture();
      component.text.set('   ');
      component.synthesize();
      expect(mockApiService.synthesizeSpeech).not.toHaveBeenCalled();
    });

    it('should not synthesize if already synthesizing', () => {
      createFixture();
      component.text.set('Hello');
      component.isSynthesizing.set(true);
      component.synthesize();
      expect(mockApiService.synthesizeSpeech).not.toHaveBeenCalled();
    });

    it('should call synthesizeSpeech API', () => {
      createFixture();
      component.text.set('Hello world');
      component.synthesize();
      expect(mockApiService.synthesizeSpeech).toHaveBeenCalledWith({
        text: 'Hello world',
        voice: component.voice() || undefined,
        speed: component.speed(),
        output_format: 'mp3',
      });
    });
  });

  describe('synthesize success handling', () => {
    it('should create audio URL on success', async () => {
      createFixture();
      component.text.set('Hello');
      component.synthesize();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.audioUrl()).toBeTruthy();
    });

    it('should set audio blob on success', async () => {
      createFixture();
      component.text.set('Hello');
      component.synthesize();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.audioBlob()).toBeTruthy();
    });
  });

  describe('synthesize error handling', () => {
    it('should set error message on failure', async () => {
      createFixture();
      component.text.set('Hello');
      (mockApiService.synthesizeSpeech as any).mockReturnValue(
        throwError(() => new Error('Synthesis failed'))
      );
      component.synthesize();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.error()).toBeTruthy();
      expect(component.error()).toContain('Synthesis failed');
    });

    it('should set generic error for non-Error objects', async () => {
      createFixture();
      component.text.set('Hello');
      (mockApiService.synthesizeSpeech as any).mockReturnValue(throwError(() => 'String error'));
      component.synthesize();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.error()).toBe('Synthesis failed');
    });

    it('should set isSynthesizing to false on error', async () => {
      createFixture();
      component.text.set('Hello');
      (mockApiService.synthesizeSpeech as any).mockReturnValue(
        throwError(() => new Error('Failed'))
      );
      component.synthesize();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.isSynthesizing()).toBe(false);
    });
  });

  describe('togglePlayPause', () => {
    it('should do nothing if no audio element', () => {
      createFixture();
      (component as any).audioElement = null;
      component.togglePlayPause();
      expect(component.isPlaying()).toBe(false);
    });

    it('should pause if currently playing', () => {
      createFixture();
      const mockAudio = {
        pause: vi.fn(),
        play: vi.fn(),
      };
      (component as any).audioElement = mockAudio;
      component.isPlaying.set(true);

      component.togglePlayPause();

      expect(mockAudio.pause).toHaveBeenCalled();
      expect(component.isPlaying()).toBe(false);
    });

    it('should play if currently paused', async () => {
      createFixture();
      const mockAudio = {
        pause: vi.fn(),
        play: vi.fn().mockResolvedValue(undefined),
      };
      (component as any).audioElement = mockAudio;
      component.isPlaying.set(false);

      component.togglePlayPause();

      expect(mockAudio.play).toHaveBeenCalled();
      expect(component.isPlaying()).toBe(true);
    });
  });

  describe('download', () => {
    it('should not call downloadBlob if no blob', () => {
      createFixture();
      component.audioBlob.set(null);
      component.download();
      expect(mockApiService.downloadBlob).not.toHaveBeenCalled();
    });
  });

  describe('progress tracking', () => {
    it('should initialize progress as 0', () => {
      createFixture();
      expect(component.progress()).toBe(0);
    });
  });

  describe('available voices fallback', () => {
    it('should have default voices when API fails', async () => {
      (mockApiService.getVoices as any).mockReturnValue(
        throwError(() => new Error('Network error'))
      );
      createFixture();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.availableVoices().length).toBeGreaterThan(0);
    });
  });
});
