import { describe, expect, it } from 'vitest';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { By } from '@angular/platform-browser';
import { DomSanitizer } from '@angular/platform-browser';
import { SidebarMoreMenuComponent } from './sidebar-more-menu.component';
import type { ModuleNavSection } from '../../../core/config/module-nav.config';

@Component({ standalone: true, template: '' })
class BlankHostComponent {}

describe('SidebarMoreMenuComponent', () => {
  const sections: ModuleNavSection[] = [
    {
      group: 'create',
      tabs: [
        { key: 'generate', labelKey: 'generation', path: '/generate', group: 'create' },
      ],
    },
    {
      group: 'lab',
      tabs: [
        { key: 'vision', labelKey: 'imageAnalysis', path: '/vision', group: 'lab' },
      ],
    },
  ];

  async function setup(url = '/chat') {
    TestBed.configureTestingModule({
      imports: [SidebarMoreMenuComponent],
      providers: [
        provideRouter([
          { path: 'chat', component: BlankHostComponent },
          { path: 'generate', component: BlankHostComponent },
          { path: 'vision', component: BlankHostComponent },
        ]),
      ],
    });

    const fixture = TestBed.createComponent(SidebarMoreMenuComponent);
    const sanitizer = TestBed.inject(DomSanitizer);
    fixture.componentRef.setInput('sections', sections);
    fixture.componentRef.setInput('collapsed', false);
    fixture.componentRef.setInput('iconFor', () => sanitizer.bypassSecurityTrustHtml('<svg></svg>'));
    const router = TestBed.inject(Router);
    await router.navigateByUrl(url);
    fixture.detectChanges();
    return fixture;
  }

  it('should render create and lab links in the more panel', async () => {
    const fixture = await setup();
    const links = fixture.debugElement.queryAll(By.css('a[role="menuitem"]'));
    const hrefs = links.map(link => link.attributes['href'] ?? link.nativeElement.getAttribute('href'));
    expect(hrefs).toContain('/generate');
    expect(hrefs).toContain('/vision');
  });

  it('should mark trigger active when current route is in more sections', async () => {
    const fixture = await setup('/generate');
    expect(fixture.componentInstance.moreActive()).toBe(true);
    const trigger = fixture.debugElement.query(By.css('button[aria-haspopup="menu"]'));
    expect(trigger.nativeElement.getAttribute('data-active')).toBe('true');
  });

  it('should highlight the active more menu item like hover', async () => {
    const fixture = await setup('/generate');
    const generateLink = fixture.debugElement
      .queryAll(By.css('a[role="menuitem"]'))
      .find(link => (link.attributes['href'] ?? link.nativeElement.getAttribute('href')) === '/generate');
    expect(generateLink?.nativeElement.getAttribute('data-active')).toBe('true');
  });

  it('should pin panel on trigger click', async () => {
    const fixture = await setup();
    const trigger = fixture.debugElement.query(By.css('button[aria-haspopup="menu"]'));
    trigger.triggerEventHandler('click', new MouseEvent('click'));
    fixture.detectChanges();
    expect(fixture.componentInstance.pinned()).toBe(true);
  });
});
