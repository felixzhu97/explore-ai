import { Component, ChangeDetectionStrategy, signal, computed, inject } from '@angular/core';
import { I18nService } from '../../../i18n';
import { SegmentedControlComponent, SegmentedControlOption } from '../../segmented-control/segmented-control.component';
import { AgentPanelComponent } from '../agent-panel/agent-panel.component';
import { AgentChatComponent } from '../../agents/agent-chat/agent-chat.component';
import { StatusBadgeComponent } from '../../agents/status-badge/status-badge.component';

type SubTabKey = 'supervisor' | 'k8s' | 'monitoring' | 'model' | 'llmops' | 'aiops' | 'vectordb';

@Component({
  selector: 'app-ai-infra-panel',
  standalone: true,
  imports: [SegmentedControlComponent, AgentPanelComponent, AgentChatComponent, StatusBadgeComponent],
  template: `
    <div class="container">
      <div class="tab-header">
        <app-segmented-control
          [options]="subTabOptions()"
          [value]="activeSubTab()"
          (changed)="onTabChange($event)"
        />
      </div>

      <div class="tab-section">
        <app-agent-panel
          [title]="activeConfig().title"
          [description]="activeConfig().description"
        >
          <app-status-badge slot="headerRight" status="online" />
          <app-agent-chat
            [agentInfo]="agentInfo()"
            [apiEndpoint]="activeConfig().apiEndpoint"
            [quickPrompts]="activeConfig().quickPrompts"
          />
        </app-agent-panel>
      </div>
    </div>
  `,
  styles: [`
    .container {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .tab-header {
      display: flex;
      justify-content: center;
      padding: 16px 0;
      overflow-x: auto;
      -webkit-overflow-scrolling: touch;
      scrollbar-width: none;
      -ms-overflow-style: none;

      &::-webkit-scrollbar {
        display: none;
      }

      @media (max-width: 640px) {
        justify-content: flex-start;
      }
    }

    .tab-section {
      animation: fadeIn 0.3s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(8px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AIInfraPanelComponent {
  private readonly i18n = inject(I18nService);

  activeSubTab = signal<SubTabKey>('supervisor');

  private readonly subTabConfigs = computed(() => {
    const t = this.i18n.t();
    return {
      supervisor: {
        title: t.nav.supervisor,
        description: t.agents.descriptions.supervisor,
        apiEndpoint: '/api/agents/supervisor/invoke',
        quickPrompts: t.agents.quickPrompts.supervisor,
      },
      k8s: {
        title: t.nav.kubernetes,
        description: t.agents.descriptions.k8s,
        apiEndpoint: '/api/agents/kubernetes/invoke',
        quickPrompts: t.agents.quickPrompts.k8s,
      },
      monitoring: {
        title: t.nav.monitoring,
        description: t.agents.descriptions.monitoring,
        apiEndpoint: '/api/agents/monitoring/invoke',
        quickPrompts: t.agents.quickPrompts.monitoring,
      },
      model: {
        title: t.nav.model,
        description: t.agents.descriptions.model,
        apiEndpoint: '/api/agents/model/invoke',
        quickPrompts: t.agents.quickPrompts.model,
      },
      llmops: {
        title: t.nav.llmops,
        description: t.agents.descriptions.llmops,
        apiEndpoint: '/api/agents/llmops/invoke',
        quickPrompts: t.agents.quickPrompts.llmops,
      },
      aiops: {
        title: t.nav.aiops,
        description: t.agents.descriptions.aiops,
        apiEndpoint: '/api/agents/aiops/invoke',
        quickPrompts: t.agents.quickPrompts.aiops,
      },
      vectordb: {
        title: t.nav.vectordb,
        description: t.agents.descriptions.vectordb,
        apiEndpoint: '/api/agents/vector/invoke',
        quickPrompts: t.agents.quickPrompts.vectordb,
      },
    };
  });

  subTabOptions = computed<SegmentedControlOption<SubTabKey>[]>(() => {
    const t = this.i18n.t();
    return [
      { value: 'supervisor', label: t.nav.supervisor },
      { value: 'k8s', label: t.nav.kubernetes },
      { value: 'monitoring', label: t.nav.monitoring },
      { value: 'model', label: t.nav.model },
      { value: 'llmops', label: t.nav.llmops },
      { value: 'aiops', label: t.nav.aiops },
      { value: 'vectordb', label: t.nav.vectordb },
    ];
  });

  activeConfig = computed(() => this.subTabConfigs()[this.activeSubTab()]);

  agentInfo = computed(() => ({
    name: this.activeConfig().title,
    description: this.activeConfig().description,
    status: 'online' as const,
  }));

  onTabChange(value: SubTabKey) {
    this.activeSubTab.set(value);
  }
}
