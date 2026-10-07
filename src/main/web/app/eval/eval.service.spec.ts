import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import { EvalService } from './eval.service';

describe('EvalService', () => {
  let service: EvalService;
  let httpMock: HttpTestingController;

  afterEach(() => {
    httpMock.verify();
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [EvalService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EvalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should post evaluation request', () => {
    service.evaluateChat({
      userMessage: 'hello',
      assistantResponse: 'hi',
    }).subscribe((response) => {
      expect(response.overallScore).toBe(0.9);
    });

    const request = httpMock.expectOne('/api/eval/chat');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      userMessage: 'hello',
      assistantResponse: 'hi',
    });
    request.flush({
      coherenceScore: 0.9,
      relevanceScore: 0.9,
      helpfulnessScore: 0.9,
      factualityScore: null,
      factualityAvailable: false,
      overallScore: 0.9,
      hasSafetyIssues: false,
      safetyFlags: [],
      suggestions: [],
    });
  });
});
