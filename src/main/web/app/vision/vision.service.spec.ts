import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { httpErrorInterceptor } from '../http/http-error.interceptor';
import { NotificationService } from '../ui/notification.service';
import { I18nService } from '../i18n';
import { ImageZoomService } from '../ui/image-zoom.service';
import { VisionService } from './vision.service';

describe('VisionService', () => {
  let service: VisionService;
  let httpMock: HttpTestingController;
  let imageZoom: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    imageZoom = { open: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([httpErrorInterceptor])),
        provideHttpClientTesting(),
        { provide: NotificationService, useValue: { showError: vi.fn() } },
        VisionService,
        { provide: ImageZoomService, useValue: imageZoom },
        I18nService,
      ],
    });

    service = TestBed.inject(VisionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should start with caption task active', () => {
    expect(service.activeTask()).toBe('caption');
  });

  it('should reject non-image files', () => {
    const file = new File(['data'], 'doc.txt', { type: 'text/plain' });
    service.processFile(file);

    expect(service.currentState().error).toBeTruthy();
    expect(service.currentState().file).toBeNull();
  });

  it('should reject files larger than 50 mb', () => {
    const file = new File([new ArrayBuffer(51 * 1024 * 1024)], 'large.png', {
      type: 'image/png',
    });
    service.processFile(file);

    expect(service.currentState().error).toBeTruthy();
  });

  it('should load image preview for valid files', async () => {
    const file = new File(['data'], 'photo.png', { type: 'image/png' });
    service.processFile(file);

    await vi.waitFor(() => {
      expect(service.currentState().file).toBe(file);
      expect(service.currentState().image).toMatch(/^data:/);
    });
  });

  it('should clear image state', async () => {
    const file = new File(['data'], 'photo.png', { type: 'image/png' });
    service.processFile(file);
    await vi.waitFor(() => expect(service.currentState().file).toBe(file));

    service.clearImage();

    expect(service.currentState().file).toBeNull();
    expect(service.currentState().image).toBeNull();
  });

  it('should report can analyze when file is loaded', async () => {
    expect(service.canAnalyze()).toBe(false);

    service.processFile(new File(['data'], 'photo.png', { type: 'image/png' }));
    await vi.waitFor(() => expect(service.canAnalyze()).toBe(true));
  });

  it('should call caption api for caption task', async () => {
    const file = new File(['data'], 'photo.png', { type: 'image/png' });

    service.processFile(file);
    await vi.waitFor(() => expect(service.currentState().file).toBe(file));

    service.analyze();

    const req = httpMock.expectOne('/api/vision/caption');
    expect(req.request.method).toBe('POST');
    req.flush({ caption: 'A cat', processingTimeMs: 120 });

    await vi.waitFor(() => {
      expect(service.currentState().result).toEqual({ task: 'caption', caption: 'A cat', processingTimeMs: 120 });
      expect(service.isLoading()).toBe(false);
    });
  });

  it('should map provider unavailable errors', async () => {
    const file = new File(['data'], 'photo.png', { type: 'image/png' });

    service.processFile(file);
    await vi.waitFor(() => expect(service.currentState().file).toBe(file));

    service.analyze();

    httpMock.expectOne('/api/vision/caption').flush(
      { message: 'Vision provider unavailable', errorCode: 'VISION_PROVIDER_UNAVAILABLE', timestamp: '2026-01-01T00:00:00Z' },
      { status: 503, statusText: 'Unavailable' },
    );

    await vi.waitFor(() => {
      expect(service.currentState().error).toContain('unavailable');
      expect(service.isLoading()).toBe(false);
    });
  });

  it('should open zoom dialog via image zoom service', () => {
    service.openZoom('data:image/png;base64,abc');
    expect(imageZoom.open).toHaveBeenCalledWith('data:image/png;base64,abc', expect.any(String));
  });
});
