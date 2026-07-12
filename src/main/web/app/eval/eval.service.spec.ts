import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
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
      imports: [HttpClientTestingModule],
      providers: [EvalService],
    });
    service = TestBed.inject(EvalService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('should_postEvaluationRequest', () => {
    service.evaluate({
      userMessage: 'hello',
      assistantResponse: 'hi',
    }).subscribe((response) => {
      expect(response.overallScore).toBe(0.9);
    });

    const req = httpMock.expectOne('/api/eval/chat');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      userMessage: 'hello',
      assistantResponse: 'hi',
    });
    req.flush({
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
