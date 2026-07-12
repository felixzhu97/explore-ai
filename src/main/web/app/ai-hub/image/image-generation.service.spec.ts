import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ApiMediaService } from '@core/services/api-media.service';
import { DEFAULT_IMAGE_SIZES } from '../image.model';
import { ImageZoomService } from '@shared/services/image-zoom.service';
import { ImageGenerationService } from './image-generation.service';

describe('ImageGenerationService', () => {
  let service: ImageGenerationService;
  let imageZoom: { open: ReturnType<typeof vi.fn> };
  let api: {
    getImageCatalog: ReturnType<typeof vi.fn>;
    generateImage: ReturnType<typeof vi.fn>;
    downloadBase64Image: ReturnType<typeof vi.fn>;
    downloadBlob: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    api = {
      getImageCatalog: vi.fn().mockReturnValue(of({
        models: ['flux'],
        sizes: ['512x512', '1024x1024'],
        qualities: ['standard'],
      })),
      generateImage: vi.fn(),
      downloadBase64Image: vi.fn(),
      downloadBlob: vi.fn(),
    };

    imageZoom = { open: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        ImageGenerationService,
        { provide: ApiMediaService, useValue: api },
        { provide: ImageZoomService, useValue: imageZoom },
      ],
    });

    service = TestBed.inject(ImageGenerationService);
  });

  it('should load catalog on init', () => {
    expect(api.getImageCatalog).toHaveBeenCalled();
    expect(service.models()).toEqual(['flux']);
    expect(service.sizes().map(s => s.label)).toEqual(['512x512', '1024x1024']);
  });

  it('should keep default sizes when catalog is empty', () => {
    api.getImageCatalog.mockReturnValue(of({ models: [], sizes: [], qualities: [] }));
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        ImageGenerationService,
        { provide: ApiMediaService, useValue: api },
      ],
    });
    const freshService = TestBed.inject(ImageGenerationService);
    expect(freshService.sizes()).toEqual(DEFAULT_IMAGE_SIZES);
  });

  it('should not generate with empty prompt', () => {
    service.setPrompt('   ');
    service.generate();
    expect(api.generateImage).not.toHaveBeenCalled();
  });

  it('should generate image from URL response', async () => {
    service.setPrompt('A sunset');
    api.generateImage.mockReturnValue(of({
      imageUrl: 'https://example.com/image.png',
    }));

    service.generate();

    expect(api.generateImage).toHaveBeenCalledWith(expect.objectContaining({
      prompt: 'A sunset',
      width: expect.any(Number),
      height: expect.any(Number),
    }));

    await vi.waitFor(() => {
      expect(service.generatedImage()).toBe('https://example.com/image.png');
      expect(service.isGenerating()).toBe(false);
    });
  });

  it('should generate image from base64 response', async () => {
    service.setPrompt('A mountain');
    api.generateImage.mockReturnValue(of({ imageBase64: 'abc123' }));

    service.generate();

    await vi.waitFor(() => {
      expect(service.generatedImage()).toBe('data:image/png;base64,abc123');
    });
  });

  it('should set error on generation failure', async () => {
    service.setPrompt('Fail case');
    api.generateImage.mockReturnValue(throwError(() => new Error('Provider down')));

    service.generate();

    await vi.waitFor(() => {
      expect(service.error()).toBe('Provider down');
      expect(service.isGenerating()).toBe(false);
    });
  });

  it('should open zoom dialog when image is generated', async () => {
    service.setPrompt('Zoom test');
    api.generateImage.mockReturnValue(of({ imageUrl: 'https://example.com/z.png' }));
    service.generate();
    await vi.waitFor(() => expect(service.generatedImage()).toBeTruthy());

    service.openZoom();
    expect(imageZoom.open).toHaveBeenCalledWith('https://example.com/z.png');
  });

  it('should download base64 image', async () => {
    service.setPrompt('Download test');
    api.generateImage.mockReturnValue(of({ imageBase64: 'abc123' }));
    service.generate();
    await vi.waitFor(() => expect(service.hasGeneratedImage()).toBe(true));

    service.download();

    expect(api.downloadBase64Image).toHaveBeenCalledWith('abc123', expect.stringContaining('ai_generated_'));
  });
});
