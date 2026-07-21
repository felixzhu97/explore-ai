import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { I18nService } from '../core/i18n';
import { ImageZoomService } from '../shared/services/image-zoom.service';
import { VisionService } from './vision.service';

describe('VisionService', () => {
  let service: VisionService;
  let httpMock: HttpTestingController;
  let imageZoom: { open: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    imageZoom = { open: vi.fn() };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
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

  it('should reject files larger than 50MB', () => {
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

  it('should report canAnalyze when file is loaded', async () => {
    expect(service.canAnalyze()).toBe(false);

    service.processFile(new File(['data'], 'photo.png', { type: 'image/png' }));
    await vi.waitFor(() => expect(service.canAnalyze()).toBe(true));
  });

  it('should call caption API for caption task', async () => {
    const file = new File(['data'], 'photo.png', { type: 'image/png' });

    service.processFile(file);
    await vi.waitFor(() => expect(service.currentState().file).toBe(file));

    service.analyze();

    const req = httpMock.expectOne('/api/vision/caption');
    expect(req.request.method).toBe('POST');
    req.flush({ caption: 'A cat', processingTimeMs: 120 });

    await vi.waitFor(() => {
      expect(service.currentState().result?.caption).toBe('A cat');
      expect(service.isLoading()).toBe(false);
    });
  });

  it('should map provider unavailable errors', async () => {
    const file = new File(['data'], 'photo.png', { type: 'image/png' });

    service.processFile(file);
    await vi.waitFor(() => expect(service.currentState().file).toBe(file));

    service.analyze();

    httpMock.expectOne('/api/vision/caption').flush(
      { errorCode: 'VISION_PROVIDER_UNAVAILABLE' },
      { status: 503, statusText: 'Unavailable' },
    );

    await vi.waitFor(() => {
      expect(service.currentState().error).toContain('unavailable');
      expect(service.isLoading()).toBe(false);
    });
  });

  it('should open zoom dialog via ImageZoomService', () => {
    service.openZoom('data:image/png;base64,abc');
    expect(imageZoom.open).toHaveBeenCalledWith('data:image/png;base64,abc', expect.any(String));
  });
});
