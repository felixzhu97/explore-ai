import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { AgentPanelComponent } from './agent-panel.component';

describe('AgentPanelComponent', () => {
  @Component({
    imports: [AgentPanelComponent],
    standalone: true,
    template: `<app-agent-panel title="Test" description="Description">
      <span class="projected">Content</span>
    </app-agent-panel>`,
  })
  class HostWithInputsComponent {}

  let fixture: ComponentFixture<HostWithInputsComponent>;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [HostWithInputsComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HostWithInputsComponent);
    fixture.detectChanges();
  });

  describe('component creation', () => {
    it('should create host component', () => {
      expect(fixture.componentInstance).toBeTruthy();
    });

    it('should create child AgentPanel component', () => {
      const panel = fixture.debugElement.children[0].componentInstance;
      expect(panel).toBeTruthy();
    });
  });

  describe('template rendering', () => {
    it('should render panel container', () => {
      const container = fixture.nativeElement.querySelector('.panel-container');
      expect(container).toBeTruthy();
    });

    it('should render panel header', () => {
      const header = fixture.nativeElement.querySelector('.panel-header');
      expect(header).toBeTruthy();
    });

    it('should display title in header', () => {
      const title = fixture.nativeElement.querySelector('.panel-header__title');
      expect(title).toBeTruthy();
      expect(title.textContent).toContain('Test');
    });

    it('should display description in header', () => {
      const desc = fixture.nativeElement.querySelector('.panel-header__description');
      expect(desc).toBeTruthy();
      expect(desc.textContent).toContain('Description');
    });

    it('should render panel content', () => {
      const content = fixture.nativeElement.querySelector('.panel-content');
      expect(content).toBeTruthy();
    });

    it('should render panel header right slot', () => {
      const right = fixture.nativeElement.querySelector('.panel-header__right');
      expect(right).toBeTruthy();
    });

    it('should project content via ng-content', () => {
      const projected = fixture.nativeElement.querySelector('.projected');
      expect(projected).toBeTruthy();
      expect(projected.textContent).toContain('Content');
    });
  });
});

describe('AgentPanelComponent hideHeader', () => {
  @Component({
    imports: [AgentPanelComponent],
    standalone: true,
    template: `<app-agent-panel title="No Header" [hideHeader]="true">
      <span>No header content</span>
    </app-agent-panel>`,
  })
  class HostNoHeaderComponent {}

  let fixture: ComponentFixture<HostNoHeaderComponent>;

  beforeEach(async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [HostNoHeaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(HostNoHeaderComponent);
    fixture.detectChanges();
  });

  it('should not render panel header when hideHeader is true', () => {
    const header = fixture.nativeElement.querySelector('.panel-header');
    expect(header).toBeFalsy();
  });

  it('should still render panel container when header is hidden', () => {
    const container = fixture.nativeElement.querySelector('.panel-container');
    expect(container).toBeTruthy();
  });

  it('should still render panel content when header is hidden', () => {
    const content = fixture.nativeElement.querySelector('.panel-content');
    expect(content).toBeTruthy();
  });
});
