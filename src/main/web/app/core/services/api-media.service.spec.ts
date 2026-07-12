import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiMediaService } from './api-media.service';

describe('ApiMediaService', () => {
  let service: ApiMediaService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiMediaService],
    });
    service = TestBed.inject(ApiMediaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should generate an image', () => {
    service.generateImage({
      prompt: 'sunset',
      model: 'dall-e-3',
      quality: 'standard',
      width: 1024,
      height: 1024,
      n: 1,
    }).subscribe((result) => {
      expect(result.prompt).toBe('sunset');
    });
    const req = httpMock.expectOne('/api/images/generate');
    req.flush({ model: 'dall-e-3', prompt: 'sunset', imageUrl: 'http://example.com/img.png' });
  });

  it('should convert base64 to blob', () => {
    const blob = service.base64ToBlob('aGVsbG8=', 'text/plain');
    expect(blob.type).toBe('text/plain');
    expect(blob.size).toBeGreaterThan(0);
  });
});
