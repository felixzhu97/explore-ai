import { beforeEach, describe, expect, it } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AiHubPage } from './ai-hub.page';
import { SegmentedControlComponent } from '@shared/components/ui/segmented-control/segmented-control.component';
import { I18nService } from '@core/i18n';

describe('AiHubComponent', () => {
  let fixture: ComponentFixture<AiHubPage>;
  let component: AiHubPage;

  const mockTranslations = {
    aiHub: {
      tabs: { chat: 'Chat', image: 'Image', tts: 'TTS' },
      chat: { title: 'AI Chat' },
      image: { title: 'Image Generation' },
      tts: { title: 'Text to Speech' },
    },
  };

  const mockI18nService = {
    language: '',
    t: () => mockTranslations,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiHubPage, SegmentedControlComponent],
      providers: [{ provide: I18nService, useValue: mockI18nService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AiHubPage);
    component = fixture.componentInstance;
  });

  describe('component creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default tab as chat', () => {
      expect(component.activeTab()).toBe('chat');
    });

    it('should have zoomedImage as null initially', () => {
      expect(component.zoomedImage()).toBeNull();
    });
  });

  describe('tabs computed', () => {
    it('should return 3 tabs', () => {
      const tabs = component.tabs();
      expect(tabs.length).toBe(3);
    });

    it('should have chat, image, and tts tabs', () => {
      const tabs = component.tabs();
      expect(tabs.map(t => t.value)).toEqual(['chat', 'image', 'tts']);
    });

    it('should have correct tab labels from i18n', () => {
      const tabs = component.tabs();
      expect(tabs[0].label).toBe('Chat');
      expect(tabs[1].label).toBe('Image');
      expect(tabs[2].label).toBe('TTS');
    });
  });

  describe('tab navigation', () => {
    it('should set active tab', () => {
      component.setTab('image');
      expect(component.activeTab()).toBe('image');
    });

    it('should switch from chat to tts', () => {
      component.setTab('tts');
      expect(component.activeTab()).toBe('tts');
    });

    it('should switch from tts to chat', () => {
      component.setTab('tts');
      component.setTab('chat');
      expect(component.activeTab()).toBe('chat');
    });
  });

  describe('image zoom functionality', () => {
    it('should set zoomed image', () => {
      component.onImageZoom('https://example.com/image.jpg');
      expect(component.zoomedImage()).toBe('https://example.com/image.jpg');
    });

    it('should close zoom and clear image', () => {
      component.zoomedImage.set('https://example.com/image.jpg');
      component.closeZoom();
      expect(component.zoomedImage()).toBeNull();
    });
  });

  describe('template rendering', () => {
    it('should render the component container', () => {
      expect(
        fixture.nativeElement.querySelector('.ai-hub') || fixture.nativeElement.firstChild,
      ).toBeTruthy();
    });
  });
});
