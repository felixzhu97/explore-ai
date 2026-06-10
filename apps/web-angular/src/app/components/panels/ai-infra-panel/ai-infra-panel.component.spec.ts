import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AIInfraPanelComponent } from './ai-infra-panel.component';
import { SegmentedControlComponent } from '../../segmented-control/segmented-control.component';
import { AgentPanelComponent } from '../agent-panel/agent-panel.component';
import { AgentChatComponent } from '../../agents/agent-chat/agent-chat.component';
import { StatusBadgeComponent } from '../../agents/status-badge/status-badge.component';
import { I18nService } from '../../../i18n/i18n.service';
import { signal } from '@angular/core';

describe('AIInfraPanelComponent', () => {
  let fixture: ComponentFixture<AIInfraPanelComponent>;
  let component: AIInfraPanelComponent;

  const mockTranslations = {
    nav: {
      supervisor: 'Supervisor',
      kubernetes: 'Kubernetes',
      monitoring: 'Monitoring',
      model: 'Model',
      llmops: 'LLMOps',
      aiops: 'AIOps',
      vectordb: 'Vector DB',
    },
    agents: {
      descriptions: {
        supervisor: 'Supervisor Agent',
        k8s: 'Kubernetes Agent',
        monitoring: 'Monitoring Agent',
        model: 'Model Agent',
        llmops: 'LLMOps Agent',
        aiops: 'AIOps Agent',
        vectordb: 'Vector DB Agent',
      },
      quickPrompts: {
        supervisor: ['How are you?'],
        k8s: ['Check pods'],
        monitoring: ['Status?'],
        model: ['Model info'],
        llmops: ['LLMOps info'],
        aiops: ['AIOps info'],
        vectordb: ['Vector DB info'],
      },
    },
  };

  const mockI18nService = {
    t: vi.fn().mockReturnValue(mockTranslations),
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        AIInfraPanelComponent,
        SegmentedControlComponent,
        AgentPanelComponent,
        StatusBadgeComponent,
      ],
      providers: [{ provide: I18nService, useValue: mockI18nService }],
    }).compileComponents();

    fixture = TestBed.createComponent(AIInfraPanelComponent);
    component = fixture.componentInstance;
  });

  describe('component creation', () => {
    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should have default active sub tab as supervisor', () => {
      expect(component.activeSubTab()).toBe('supervisor');
    });
  });

  describe('subTabOptions computed', () => {
    it('should return 7 tab options', () => {
      expect(component.subTabOptions().length).toBe(7);
    });

    it('should have correct tab values', () => {
      const options = component.subTabOptions();
      const values = options.map((o) => o.value);
      expect(values).toContain('supervisor');
      expect(values).toContain('k8s');
      expect(values).toContain('monitoring');
      expect(values).toContain('model');
      expect(values).toContain('llmops');
      expect(values).toContain('aiops');
      expect(values).toContain('vectordb');
    });

    it('should have labels from i18n', () => {
      const options = component.subTabOptions();
      expect(options[0].label).toBe('Supervisor');
      expect(options[1].label).toBe('Kubernetes');
    });
  });

  describe('subTabConfigs computed', () => {
    it('should return configs for all tabs', () => {
      const configs = component['subTabConfigs']();
      expect(Object.keys(configs)).toContain('supervisor');
      expect(Object.keys(configs)).toContain('k8s');
      expect(Object.keys(configs)).toContain('monitoring');
      expect(Object.keys(configs)).toContain('model');
      expect(Object.keys(configs)).toContain('llmops');
      expect(Object.keys(configs)).toContain('aiops');
      expect(Object.keys(configs)).toContain('vectordb');
    });

    it('should have apiEndpoint for each config', () => {
      const configs = component['subTabConfigs']();
      expect(configs.supervisor.apiEndpoint).toBe('/api/agents/supervisor/invoke/sse');
      expect(configs.k8s.apiEndpoint).toBe('/api/agents/kubernetes/invoke/sse');
    });

    it('should have quickPrompts for each config', () => {
      const configs = component['subTabConfigs']();
      expect(configs.supervisor.quickPrompts).toBeDefined();
    });
  });

  describe('activeConfig computed', () => {
    it('should return supervisor config by default', () => {
      expect(component.activeConfig().title).toBe('Supervisor');
    });

    it('should return correct config when tab changes', () => {
      component.activeSubTab.set('k8s');
      expect(component.activeConfig().title).toBe('Kubernetes');
    });
  });

  describe('agentInfo computed', () => {
    it('should return agent info from active config', () => {
      const info = component.agentInfo();
      expect(info.name).toBe('Supervisor');
      expect(info.description).toBe('Supervisor Agent');
      expect(info.status).toBe('online');
    });

    it('should update when tab changes', () => {
      component.activeSubTab.set('monitoring');
      const info = component.agentInfo();
      expect(info.name).toBe('Monitoring');
    });
  });

  describe('onTabChange', () => {
    it('should update active sub tab', () => {
      component.onTabChange('k8s');
      expect(component.activeSubTab()).toBe('k8s');
    });

    it('should work for all tab values', () => {
      const tabs: Array<'supervisor' | 'k8s' | 'monitoring' | 'model' | 'llmops' | 'aiops' | 'vectordb'> = [
        'supervisor', 'k8s', 'monitoring', 'model', 'llmops', 'aiops', 'vectordb',
      ];
      tabs.forEach((tab) => {
        component.onTabChange(tab);
        expect(component.activeSubTab()).toBe(tab);
      });
    });
  });

  describe('template rendering', () => {
    it('should render tab header', () => {
      fixture.detectChanges();
      const header = fixture.nativeElement.querySelector('.tab-header');
      expect(header).toBeTruthy();
    });

    it('should render segmented control', () => {
      fixture.detectChanges();
      const control = fixture.nativeElement.querySelector('app-segmented-control');
      expect(control).toBeTruthy();
    });

    it('should render agent panel', () => {
      fixture.detectChanges();
      const panel = fixture.nativeElement.querySelector('app-agent-panel');
      expect(panel).toBeTruthy();
    });

    it('should render status badge in header right slot', () => {
      fixture.detectChanges();
      const badge = fixture.nativeElement.querySelector('app-status-badge');
      expect(badge).toBeTruthy();
    });

    it('should render agent chat', () => {
      fixture.detectChanges();
      const chat = fixture.nativeElement.querySelector('app-agent-chat');
      expect(chat).toBeTruthy();
    });
  });
});
