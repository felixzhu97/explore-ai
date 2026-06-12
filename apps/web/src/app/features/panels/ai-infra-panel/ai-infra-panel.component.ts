import { Component, ChangeDetectionStrategy, signal, computed, inject } from '@angular/core';
import { environment } from '@env/environment';
import { I18nService } from '@i18n';
import {
  SegmentedControlComponent,
  SegmentedControlOption,
} from '@shared/components/ui/segmented-control/segmented-control.component';
import { AgentPanelComponent } from '@features/panels/agent-panel/agent-panel.component';
import { AgentChatComponent } from '@features/agents/agent-chat/agent-chat.component';
import { StatusBadgeComponent } from '@features/agents/status-badge/status-badge.component';

type SubTabKey = 'supervisor' | 'k8s' | 'monitoring' | 'model' | 'llmops' | 'aiops' | 'vectordb';

@Component({
  selector: 'app-ai-infra-panel',
  standalone: true,
  imports: [
    SegmentedControlComponent,
    AgentPanelComponent,
    AgentChatComponent,
    StatusBadgeComponent,
  ],
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
        <app-agent-panel [title]="activeConfig().title" [description]="activeConfig().description">
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
  styles: [
    `
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
        from {
          opacity: 0;
          transform: translateY(8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }
    `,
  ],
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
        apiEndpoint: environment.agents.supervisor,
        quickPrompts: t.agents.quickPrompts.supervisor,
      },
      k8s: {
        title: t.nav.kubernetes,
        description: t.agents.descriptions.k8s,
        apiEndpoint: environment.agents.kubernetes,
        quickPrompts: t.agents.quickPrompts.k8s,
      },
      monitoring: {
        title: t.nav.monitoring,
        description: t.agents.descriptions.monitoring,
        apiEndpoint: environment.agents.monitoring,
        quickPrompts: t.agents.quickPrompts.monitoring,
      },
      model: {
        title: t.nav.model,
        description: t.agents.descriptions.model,
        apiEndpoint: environment.agents.model,
        quickPrompts: t.agents.quickPrompts.model,
      },
      llmops: {
        title: t.nav.llmops,
        description: t.agents.descriptions.llmops,
        apiEndpoint: environment.agents.llmops,
        quickPrompts: t.agents.quickPrompts.llmops,
      },
      aiops: {
        title: t.nav.aiops,
        description: t.agents.descriptions.aiops,
        apiEndpoint: environment.agents.aiops,
        quickPrompts: t.agents.quickPrompts.aiops,
      },
      vectordb: {
        title: t.nav.vectordb,
        description: t.agents.descriptions.vectordb,
        apiEndpoint: environment.agents.vector,
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
