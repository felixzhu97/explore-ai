import type { Translations } from '../../../core/i18n/translations.types';
import type { AgentInfo } from '../../../agents/agents.model';
import type { FeatureFlagService } from '../../../core/feature-flag.service';
import type { ToolCatalogEntryDto } from './tools-catalog.service';
import type { SenderActionGroup, SenderActionItem } from './sender-action.model';

export type SenderCatalogScope = 'full' | 'rag' | 'agents';

export function buildSenderActionGroups(options: {
  t: Translations;
  tools: ToolCatalogEntryDto[];
  agents: AgentInfo[];
  featureFlags: Pick<FeatureFlagService, 'isEnabled'>;
  scope?: SenderCatalogScope;
}): SenderActionGroup[] {
  const scope = options.scope ?? 'full';
  const s = options.t.sender;
  const groups: SenderActionGroup[] = [];

  if (scope === 'full' || scope === 'rag') {
    const toolItems: SenderActionItem[] = options.tools.map(tool => ({
      id: `tool:${tool.name}`,
      kind: 'tool',
      label: tool.name,
      description: tool.description || `${tool.source}`,
    }));
    if (toolItems.length > 0) {
      groups.push({ id: 'tools', label: s.groups.tools, items: toolItems });
    }
  }

  if (scope === 'full' || scope === 'agents') {
    const agentItems: SenderActionItem[] = [
      {
        id: 'agent:open',
        kind: 'agent',
        label: s.actions.openAgentPipeline,
        description: s.actions.openAgentPipelineHint,
        path: '/agents',
      },
      ...options.agents
        .filter(agent => !agent.supervisor)
        .map(agent => ({
          id: `agent:${agent.type}`,
          kind: 'agent' as const,
          label: agent.name,
          description: agent.description,
          agentType: agent.type,
        })),
    ];
    groups.push({ id: 'agents', label: s.groups.agents, items: agentItems });
  }

  const sessionItems: SenderActionItem[] = [];
  if (scope === 'full' || scope === 'agents') {
    sessionItems.push({
      id: 'session:newChat',
      kind: 'session',
      label: s.actions.newChat,
    });
  }
  if (scope === 'full') {
    sessionItems.push({
      id: 'session:toggleTools',
      kind: 'session',
      label: s.actions.toggleTools,
    });
  }
  if (sessionItems.length > 0) {
    groups.push({ id: 'session', label: s.groups.session, items: sessionItems });
  }

  return groups;
}

export function filterSenderGroups(
  groups: SenderActionGroup[],
  query: string,
): SenderActionGroup[] {
  const q = query.trim().toLowerCase();
  if (!q) {
    return groups;
  }
  return groups
    .map(group => ({
      ...group,
      items: group.items.filter((item) => {
        return item.label.toLowerCase().includes(q)
          || item.id.toLowerCase().includes(q)
          || (item.description?.toLowerCase().includes(q) ?? false);
      }),
    }))
    .filter(group => group.items.length > 0);
}
