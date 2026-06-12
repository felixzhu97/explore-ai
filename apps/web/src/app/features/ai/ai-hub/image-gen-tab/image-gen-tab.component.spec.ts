import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { ImageGenTabComponent } from './image-gen-tab.component';
import { ApiService } from '@core/services/api.service';
import { I18nService } from '@i18n';
import { of, throwError } from 'rxjs';

describe('ImageGenTabComponent', () => {
  let fixture: ComponentFixture<ImageGenTabComponent>;
  let component: ImageGenTabComponent;
  let mockApiService: Partial<ApiService>;

  const mockI18nService = {
    t: () => ({
      aiHub: {
        image: {
          title: 'Image Generation',
          description: 'Generate images with AI',
          promptLabel: 'Prompt',
          promptPlaceholder: 'Describe the image...',
          negativePromptLabel: 'Negative Prompt',
          negativePromptPlaceholder: 'What to avoid...',
          sizeLabel: 'Size',
          generateButton: 'Generate',
          generating: 'Generating...',
          emptyState: 'Generate an image to see it here',
          download: 'Download',
        },
      },
    }),
  };

  const createMockApiService = () => {
    mockApiService = {
      generateImage: vi.fn().mockReturnValue(
        of({
          images: ['base64imagedata123'],
          seed: 12345,
        })
      ),
      downloadBase64Image: vi.fn(),
    };
    return mockApiService;
  };

  const createFixture = () => {
    fixture = TestBed.createComponent(ImageGenTabComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  };

  beforeEach(async () => {
    createMockApiService();
    await TestBed.configureTestingModule({
      imports: [ImageGenTabComponent, HttpClientTestingModule],
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

    it('should initialize with empty prompt', () => {
      createFixture();
      expect(component.prompt()).toBe('');
    });

    it('should initialize with empty negative prompt', () => {
      createFixture();
      expect(component.negativePrompt()).toBe('');
    });

    it('should initialize isGenerating as false', () => {
      createFixture();
      expect(component.isGenerating()).toBe(false);
    });

    it('should initialize error as null', () => {
      createFixture();
      expect(component.error()).toBeNull();
    });

    it('should initialize generatedImage as null', () => {
      createFixture();
      expect(component.generatedImage()).toBeNull();
    });
  });

  describe('sizes', () => {
    it('should have predefined size options', () => {
      createFixture();
      expect(component.sizes.length).toBe(3);
    });

    it('should have 512x512 size option', () => {
      createFixture();
      expect(component.sizes).toContainEqual({ label: '512x512', width: 512, height: 512 });
    });

    it('should have 768x768 size option', () => {
      createFixture();
      expect(component.sizes).toContainEqual({ label: '768x768', width: 768, height: 768 });
    });

    it('should have 1024x1024 size option', () => {
      createFixture();
      expect(component.sizes).toContainEqual({ label: '1024x1024', width: 1024, height: 1024 });
    });

    it('should default to 1024x1024', () => {
      createFixture();
      expect(component.selectedSize().label).toBe('1024x1024');
    });
  });

  describe('setPrompt', () => {
    it('should set prompt value', () => {
      createFixture();
      component.setPrompt('A beautiful sunset');
      expect(component.prompt()).toBe('A beautiful sunset');
    });

    it('should handle empty string', () => {
      createFixture();
      component.setPrompt('');
      expect(component.prompt()).toBe('');
    });

    it('should handle multiline text', () => {
      createFixture();
      const multiline = 'A cat\nsitting on a table\nby the window';
      component.setPrompt(multiline);
      expect(component.prompt()).toBe(multiline);
    });
  });

  describe('setNegativePrompt', () => {
    it('should set negative prompt value', () => {
      createFixture();
      component.setNegativePrompt('blurry, low quality');
      expect(component.negativePrompt()).toBe('blurry, low quality');
    });

    it('should handle empty string', () => {
      createFixture();
      component.setNegativePrompt('');
      expect(component.negativePrompt()).toBe('');
    });
  });

  describe('setSize', () => {
    it('should update selected size', () => {
      createFixture();
      component.setSize({ label: '512x512', width: 512, height: 512 });
      expect(component.selectedSize().label).toBe('512x512');
    });

    it('should update width and height', () => {
      createFixture();
      component.setSize({ label: '768x768', width: 768, height: 768 });
      expect(component.selectedSize().width).toBe(768);
      expect(component.selectedSize().height).toBe(768);
    });
  });

  describe('generate', () => {
    it('should not generate if prompt is empty', () => {
      createFixture();
      component.prompt.set('');
      component.generate();
      expect(mockApiService.generateImage).not.toHaveBeenCalled();
    });

    it('should not generate if prompt is whitespace only', () => {
      createFixture();
      component.prompt.set('   ');
      component.generate();
      expect(mockApiService.generateImage).not.toHaveBeenCalled();
    });

    it('should not generate if already generating', () => {
      createFixture();
      component.prompt.set('A cat');
      component.isGenerating.set(true);
      component.generate();
      expect(mockApiService.generateImage).not.toHaveBeenCalled();
    });

    it('should call generateImage API with prompt', () => {
      createFixture();
      component.prompt.set('A beautiful sunset');
      component.generate();
      expect(mockApiService.generateImage).toHaveBeenCalledWith(
        expect.objectContaining({
          prompt: 'A beautiful sunset',
        })
      );
    });

    it('should call generateImage API with negative prompt', () => {
      createFixture();
      component.prompt.set('A cat');
      component.negativePrompt.set('blurry, low quality');
      component.generate();
      expect(mockApiService.generateImage).toHaveBeenCalledWith(
        expect.objectContaining({
          negative_prompt: 'blurry, low quality',
        })
      );
    });

    it('should call generateImage API with selected size', () => {
      createFixture();
      component.prompt.set('A cat');
      component.setSize({ label: '512x512', width: 512, height: 512 });
      component.generate();
      expect(mockApiService.generateImage).toHaveBeenCalledWith(
        expect.objectContaining({
          width: 512,
          height: 512,
        })
      );
    });

    it('should request num_images as 1', () => {
      createFixture();
      component.prompt.set('A cat');
      component.generate();
      expect(mockApiService.generateImage).toHaveBeenCalledWith(
        expect.objectContaining({
          num_images: 1,
        })
      );
    });
  });

  describe('generate success handling', () => {
    it('should set generated image URL on success', async () => {
      createFixture();
      component.prompt.set('A cat');
      component.generate();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.generatedImage()).toBeTruthy();
      expect(component.generatedImage()).toContain('data:image/png;base64,');
    });

    it('should set isGenerating to false on complete', async () => {
      createFixture();
      component.prompt.set('A cat');
      component.generate();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.isGenerating()).toBe(false);
    });
  });

  describe('generate error handling', () => {
    it('should set error message on failure', async () => {
      createFixture();
      component.prompt.set('A cat');
      (mockApiService.generateImage as any).mockReturnValue(
        throwError(() => new Error('Generation failed'))
      );
      component.generate();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.error()).toBeTruthy();
      expect(component.error()).toContain('Generation failed');
    });

    it('should set generic error for non-Error objects', async () => {
      createFixture();
      component.prompt.set('A cat');
      (mockApiService.generateImage as any).mockReturnValue(
        throwError(() => 'String error')
      );
      component.generate();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.error()).toBe('Image generation failed');
    });

    it('should set isGenerating to false on error', async () => {
      createFixture();
      component.prompt.set('A cat');
      (mockApiService.generateImage as any).mockReturnValue(
        throwError(() => new Error('Failed'))
      );
      component.generate();
      await new Promise<void>((resolve) => setTimeout(resolve, 10));
      expect(component.isGenerating()).toBe(false);
    });
  });

  describe('download', () => {
    it('should not call downloadBase64Image if no image', () => {
      createFixture();
      component.download();
      expect(mockApiService.downloadBase64Image).not.toHaveBeenCalled();
    });
  });

  describe('zoom output', () => {
    it('should have zoom output emitter', () => {
      createFixture();
      expect(component.zoom).toBeDefined();
    });
  });

  describe('size selection interactions', () => {
    it('should mark selected size option', () => {
      createFixture();
      component.setSize({ label: '512x512', width: 512, height: 512 });
      fixture.detectChanges();
      const selectedButton = fixture.nativeElement.querySelector('.size-option.selected');
      expect(selectedButton?.textContent).toContain('512x512');
    });
  });
});
