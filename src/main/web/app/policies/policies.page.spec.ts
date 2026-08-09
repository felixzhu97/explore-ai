import { describe, expect, it } from 'vitest';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, RouterLink } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { PoliciesPage } from './policies.page';
import {
  LEGACY_LEGAL_TO_SLUG,
  resolvePolicySlug,
} from './policies.page.copy';
import { I18nService } from '../core/i18n';

describe('resolvePolicySlug', () => {
  it('should resolve modern policy slugs', () => {
    expect(resolvePolicySlug('terms-of-use')).toBe('terms-of-use');
    expect(resolvePolicySlug('privacy-policy')).toBe('privacy-policy');
  });

  it('should map legacy legal ids to policy slugs', () => {
    expect(resolvePolicySlug('terms')).toBe(LEGACY_LEGAL_TO_SLUG.terms);
    expect(resolvePolicySlug('privacy')).toBe(LEGACY_LEGAL_TO_SLUG.privacy);
    expect(resolvePolicySlug('cookies')).toBe(LEGACY_LEGAL_TO_SLUG.cookies);
  });

  it('should return null for unknown slugs', () => {
    expect(resolvePolicySlug('unknown')).toBeNull();
    expect(resolvePolicySlug(null)).toBeNull();
  });
});

describe('PoliciesPage', () => {
  async function setup(slug: string | null) {
    TestBed.configureTestingModule({
      imports: [PoliciesPage, RouterLink],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap(slug ? { slug } : {})),
          },
        },
        {
          provide: I18nService,
          useValue: {
            language: signal('en'),
            t: signal({
              nav: {},
            }),
          },
        },
      ],
    });

    const fixture = TestBed.createComponent(PoliciesPage);
    fixture.detectChanges();
    return fixture;
  }

  it('should render the policies hub with document summaries', async () => {
    const fixture = await setup(null);
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Policies');
    expect(text).toContain('Terms of Use');
    expect(text).toContain('Privacy Policy');
    expect(text).toContain('Privacy controls');
    expect(fixture.componentInstance.doc()).toBeNull();
  });

  it('should render terms of use sections when slug is valid', async () => {
    const fixture = await setup('terms-of-use');
    expect(fixture.componentInstance.doc()?.title).toBe('Terms of Use');
    expect(fixture.componentInstance.doc()?.sections.length).toBeGreaterThan(3);
    const headings = fixture.debugElement.queryAll(By.css('h2'));
    expect(headings.some(h => h.nativeElement.textContent.includes('Agreement'))).toBe(true);
  });

  it('should render localized policy body when language is zh', async () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [PoliciesPage, RouterLink],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: of(convertToParamMap({ slug: 'terms-of-use' })),
          },
        },
        {
          provide: I18nService,
          useValue: {
            language: signal('zh'),
            t: signal({ nav: {} }),
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(PoliciesPage);
    fixture.detectChanges();
    expect(fixture.componentInstance.doc()?.title).toBe('使用条款');
    const headings = fixture.debugElement.queryAll(By.css('h2'));
    expect(headings.some(h => h.nativeElement.textContent.includes('协议'))).toBe(true);
  });

  it('should fall back to hub for unknown slug', async () => {
    const fixture = await setup('not-a-real-policy');
    expect(fixture.componentInstance.doc()).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Terms of Use');
  });
});
