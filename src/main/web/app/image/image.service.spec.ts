import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { httpErrorInterceptor } from '../http/http-error.interceptor';
import { NotificationService } from '../ui/notification.service';
import { ImageZoomService } from '../ui/image-zoom.service';
import { DEFAULT_IMAGE_SIZES, ImageService } from './image.service';

describe('ImageService', () => {
  let service: ImageService;
  let httpMock: HttpTestingController;
  let imageZoom: { openZoom: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    imageZoom = { openZoom: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: { showError: vi.fn() } },
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
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: { showError: vi.fn() } },
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
    service.generateImage();
    httpMock.expectNone('/api/images/generate');
  });

  it('should generate image from url response', async () => {
    service.setPrompt('A sunset');
    service.generateImage();

    const request = httpMock.expectOne('/api/images/generate');
    expect(request.request.body).toEqual(expect.objectContaining({
      prompt: 'A sunset',
      width: expect.any(Number),
      height: expect.any(Number),
    }));
    request.flush({ imageUrl: 'https://example.com/image.png', status: 'ok' });

    await vi.waitFor(() => {
      expect(service.generatedImage()).toBe('https://example.com/image.png');
      expect(service.isGenerating()).toBe(false);
    });
  });

  it('should generate image from base64 response', async () => {
    service.setPrompt('A mountain');
    service.generateImage();
    httpMock.expectOne('/api/images/generate').flush({ imageBase64: 'abc123', status: 'ok' });

    await vi.waitFor(() => {
      expect(service.generatedImage()).toBe('data:image/png;base64,abc123');
    });
  });

  it('should set error on generation failure', async () => {
    service.setPrompt('Fail case');
    service.generateImage();
    httpMock.expectOne('/api/images/generate').flush(
      {
        message: 'Image provider not configured',
        errorCode: 'IMAGE_PROVIDER_NOT_CONFIGURED',
        timestamp: '2026-01-01T00:00:00Z',
      },
      { status: 503, statusText: 'Service Unavailable' },
    );

    await vi.waitFor(() => {
      expect(service.error()).toBe('Image provider not configured');
      expect(service.isGenerating()).toBe(false);
    });
  });

  it('should open zoom dialog when image is generated', async () => {
    service.setPrompt('Zoom test');
    service.generateImage();
    httpMock.expectOne('/api/images/generate').flush({
      imageUrl: 'https://example.com/z.png',
      status: 'ok',
    });
    await vi.waitFor(() => expect(service.generatedImage()).toBeTruthy());

    service.openZoom();
    expect(imageZoom.openZoom).toHaveBeenCalledWith('https://example.com/z.png');
  });
});
