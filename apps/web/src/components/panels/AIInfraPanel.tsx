import { useState } from 'react';
import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';
import { SegmentedControl } from '../SegmentedControl';
import { AgentChat } from '../agents/AgentChat';
import { StatusBadge } from '../agents/StatusBadge';
import { AgentPanel } from './AgentPanel';
import { useI18n } from '../../i18n';
import { spacing } from '../../theme';

type SubTabKey = 'supervisor' | 'k8s' | 'monitoring' | 'model' | 'llmops' | 'aiops' | 'vectordb';

interface SubTabConfig {
  title: string;
  description: string;
  apiEndpoint: string;
  quickPrompts: string[];
}

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${spacing.md};
`;

const TabSection = styled.div``;

const fadeIn = keyframes`
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
`;

const TabHeader = styled.div`
  display: flex;
  justify-content: center;
  padding: ${spacing.md} 0;
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
`;

const AGENT_STATUS = { status: 'online' as const };

export function AIInfraPanel() {
  const { t } = useI18n();
  const [activeSubTab, setActiveSubTab] = useState<SubTabKey>('supervisor');

  const subTabOptions: { value: SubTabKey; label: string }[] = [
    { value: 'supervisor', label: t.nav.supervisor },
    { value: 'k8s', label: t.nav.kubernetes },
    { value: 'monitoring', label: t.nav.monitoring },
    { value: 'model', label: t.nav.model },
    { value: 'llmops', label: t.nav.llmops },
    { value: 'aiops', label: t.nav.aiops },
    { value: 'vectordb', label: t.nav.vectordb },
  ];

  const subTabConfigs: Record<SubTabKey, SubTabConfig> = {
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

  const activeConfig = subTabConfigs[activeSubTab];

  return (
    <Container>
      <TabHeader>
        <SegmentedControl
          options={subTabOptions}
          value={activeSubTab}
          onChange={setActiveSubTab}
        />
      </TabHeader>
      <TabSection key={activeSubTab} css={{ animation: `${fadeIn} 0.3s ease` }}>
        <AgentPanel
          title={activeConfig.title}
          description={activeConfig.description}
          headerRight={<StatusBadge status={AGENT_STATUS.status} />}
        >
          <AgentChat
            agentInfo={{ name: activeConfig.title, description: activeConfig.description }}
            apiEndpoint={activeConfig.apiEndpoint}
            quickPrompts={activeConfig.quickPrompts}
          />
        </AgentPanel>
      </TabSection>
    </Container>
  );
}
