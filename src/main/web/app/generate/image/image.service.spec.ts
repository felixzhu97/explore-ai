import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ImageZoomService } from '../../shared/services/image-zoom.service';
import { DEFAULT_IMAGE_SIZES } from './image.model';
import { ImageService } from './image.service';

describe('ImageService', () => {
  let service: ImageService;
  let httpMock: HttpTestingController;
  let imageZoom: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    imageZoom = { open: vi.fn() };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        ImageService,
        { provide: ImageZoomService, useValue: imageZoom },
      ],
    });

    httpMock = TestBed.inject(HttpTestingController);
    service = TestBed.inject(ImageService);

    // Flush constructor catalog requests
    httpMock.expectOne('/api/images/models').flush({ models: ['flux'] });
    httpMock.expectOne('/api/images/sizes').flush({ sizes: ['512x512', '1024x1024'] });
    httpMock.expectOne('/api/images/qualities').flush({ qualities: ['standard'] });
  });

  it('should load catalog on init', () => {
    expect(service.models()).toEqual(['flux']);
    expect(service.sizes().map(s => s.label)).toEqual(['512x512', '1024x1024']);
  });

  it('should keep default sizes when catalog is empty', () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        ImageService,
        { provide: ImageZoomService, useValue: imageZoom },
      ],
    });
    const freshHttp = TestBed.inject(HttpTestingController);
    const freshService = TestBed.inject(ImageService);
    freshHttp.expectOne('/api/images/models').flush({ models: [] });
    freshHttp.expectOne('/api/images/sizes').flush({ sizes: [] });
    freshHttp.expectOne('/api/images/qualities').flush({ qualities: [] });
    expect(freshService.sizes()).toEqual(DEFAULT_IMAGE_SIZES);
  });

  it('should not generate with empty prompt', () => {
    service.setPrompt('   ');
    service.generate();
    httpMock.expectNone('/api/images/generate');
  });

  it('should generate image from URL response', async () => {
    service.setPrompt('A sunset');
    service.generate();

    const req = httpMock.expectOne('/api/images/generate');
    expect(req.request.body).toEqual(expect.objectContaining({
      prompt: 'A sunset',
      width: expect.any(Number),
      height: expect.any(Number),
    }));
    req.flush({ imageUrl: 'https://example.com/image.png', status: 'ok' });

    await vi.waitFor(() => {
      expect(service.generatedImage()).toBe('https://example.com/image.png');
      expect(service.isGenerating()).toBe(false);
    });
  });

  it('should generate image from base64 response', async () => {
    service.setPrompt('A mountain');
    service.generate();
    httpMock.expectOne('/api/images/generate').flush({ imageBase64: 'abc123', status: 'ok' });

    await vi.waitFor(() => {
      expect(service.generatedImage()).toBe('data:image/png;base64,abc123');
    });
  });

  it('should set error on generation failure', async () => {
    service.setPrompt('Fail case');
    service.generate();
    httpMock.expectOne('/api/images/generate').flush(
      { message: 'Provider down' },
      { status: 500, statusText: 'Error' },
    );

    await vi.waitFor(() => {
      expect(service.error()).toBeTruthy();
      expect(service.isGenerating()).toBe(false);
    });
  });

  it('should open zoom dialog when image is generated', async () => {
    service.setPrompt('Zoom test');
    service.generate();
    httpMock.expectOne('/api/images/generate').flush({
      imageUrl: 'https://example.com/z.png',
      status: 'ok',
    });
    await vi.waitFor(() => expect(service.generatedImage()).toBeTruthy());

    service.openZoom();
    expect(imageZoom.open).toHaveBeenCalledWith('https://example.com/z.png');
  });
});
