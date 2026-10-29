import {
  Component,
  type OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { disabled, form, FormField } from '@angular/forms/signals';
import { NotificationService } from '../ui/notification.service';
import { I18nService } from '../i18n';
import {
  AgentsService,
  type CustomAgent,
  type UpdateCustomAgentRequest,
} from './agents.service';
import type { AgentInfoResponse } from '../pipelines/pipelines.service';
import { ZardButtonComponent } from '../ui/button';
import { requiredText } from '../forms/required-text';
import { hasText } from '../shared/presence';
import { AgentsAskPanelComponent } from './agents-ask-panel.component';

const TOOL_KEYS = ['web', 'weather', 'datetime', 'document'] as const;

interface AgentDraft {
  agentType: string;
  name: string;
  description: string;
  systemPrompt: string;
  tools: string[];
}

interface GalleryAgent {
  type: string;
  name: string;
  description: string;
  tools: string[];
  source: 'builtin' | 'custom';
  custom?: CustomAgent;
}

interface ChatTurn {
  role: 'user' | 'assistant';
  text: string;
}

const EMPTY_DRAFT: AgentDraft = {
  agentType: '',
  name: '',
  description: '',
  systemPrompt: '',
  tools: [],
};

@Component({
  selector: 'app-agents-page',
  imports: [FormField, ZardButtonComponent, AgentsAskPanelComponent],
  templateUrl: './agents.page.html',
  host: {
    class:
      'flex min-h-0 w-full flex-1 flex-col overflow-x-hidden overflow-y-auto bg-surface px-4 py-6 lg:overflow-hidden',
  },
})
export class AgentsPageComponent implements OnInit {
  readonly #agentsApi = inject(AgentsService);
  readonly #notifications = inject(NotificationService);
  protected readonly i18n = inject(I18nService);

  readonly error = signal<string | null>(null);

  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly isFormAgentTypeLocked = signal(false);
  readonly #draft = signal<AgentDraft>(EMPTY_DRAFT);

  protected readonly draftForm = form(this.#draft, (path) => {
    disabled(path.agentType, { when: () => this.isFormAgentTypeLocked() });
    requiredText(path.agentType, () => !hasText(this.editingId()));
    requiredText(path.name);
    requiredText(path.systemPrompt);
  });

  readonly availableTools = TOOL_KEYS;
  readonly isSaving = signal(false);

  readonly isLoading = signal(true);
  readonly catalog = signal<AgentInfoResponse[]>([]);
  readonly customAgents = signal<CustomAgent[]>([]);

  readonly builtins = computed((): GalleryAgent[] => this.catalog()
    .filter(agent => !agent.supervisor)
    .map(agent => ({
      type: agent.type,
      name: agent.name,
      description: agent.description,
      tools: [...agent.tools],
      source: 'builtin' as const,
    })),
  );

  readonly library = computed((): GalleryAgent[] => this.customAgents().map(agent => ({
    type: agent.agentType,
    name: agent.name,
    description: agent.description,
    tools: [...agent.tools],
    source: 'custom' as const,
    custom: agent,
  })),
  );

  readonly selected = signal<GalleryAgent | null>(null);
  readonly composer = signal('');
  readonly turns = signal<ChatTurn[]>([]);
  readonly isAsking = signal(false);
  readonly partnerReady = signal(false);
  readonly partnerSkills = signal<string[]>([]);
  readonly partnerSkillsLine = computed(() => this.partnerSkills().join(' · '));

  readonly suggestions = computed(() => {
    const agent = this.selected();
    if (agent === null) {
      return [] as string[];
    }
    const map = this.i18n.t().agents.suggestions;
    const typed = agent.type as keyof typeof map;
    if (Object.hasOwn(map, typed)) {
      return map[typed];
    }
    return map.default;
  });

  ngOnInit(): void {
    this.reload();
    this.#agentsApi.getAgentCard().subscribe({
      next: (card) => {
        this.partnerReady.set(true);
        this.partnerSkills.set(
          (card.skills ?? []).map(skill => skill.name !== '' ? skill.name : skill.id,
          ),
        );
      },
      error: () => this.partnerReady.set(false),
    });
  }

  /** Opens the create form with an empty draft. */
  startCreate(): void {
    this.editingId.set(null);
    this.isFormAgentTypeLocked.set(false);
    this.#draft.set(EMPTY_DRAFT);
    this.showForm.set(true);
  }

  /** Tells whether the draft includes the tool. */
  isToolSelected(toolKey: string): boolean {
    return this.#draft().tools.includes(toolKey);
  }

  /** Adds or removes the tool in the draft. */
  toggleTool(toolKey: string): void {
    this.draftForm.tools().value.update(current => (current.includes(toolKey)
      ? current.filter(key => key !== toolKey)
      : [...current, toolKey]));
  }

  /** Creates or updates the agent from the draft. */
  save(): void {
    if (this.draftForm().invalid()) {
      this.error.set(this.i18n.t().agents.errors.nameRequired);
      return;
    }
    const draft = this.#draft();
    const agentType = draft.agentType.trim().toLowerCase();
    const request: UpdateCustomAgentRequest = {
      name: draft.name.trim(),
      description: draft.description.trim(),
      systemPrompt: draft.systemPrompt.trim(),
      tools: [...draft.tools],
    };
    this.isSaving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request$ = hasText(id)
      ? this.#agentsApi.updateAgent(id, request)
      : this.#agentsApi.createAgent({ ...request, agentType });
    request$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.showForm.set(false);
        this.#notifications.showSuccess(this.i18n.t().common.success);
        this.reload();
      },
      error: () => {
        this.error.set(this.i18n.t().agents.errors.saveFailed);
        this.isSaving.set(false);
      },
    });
  }

  /** Closes the form without saving. */
  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  /** Selects an agent and opens the ask panel. */
  selectAgent(agent: GalleryAgent): void {
    const current = this.selected();
    if (
      current !== null
      && current.type === agent.type
      && current.source === agent.source
    ) {
      return;
    }
    this.selected.set(agent);
    this.turns.set([]);
    this.composer.set('');
    this.error.set(null);
  }

  /** Whether this gallery card is the active selection. */
  isSelected(agent: GalleryAgent): boolean {
    const current = this.selected();
    return current !== null
      && current.type === agent.type
      && current.source === agent.source;
  }

  /** Fills the composer from a suggestion chip. */
  useSuggestion(text: string): void {
    this.composer.set(text);
  }

  /** Sends the composer text to the selected agent. */
  ask(): void {
    const agent = this.selected();
    const prompt = this.composer().trim();
    if (agent === null || !hasText(prompt) || this.isAsking()) {
      return;
    }
    this.turns.update(turns => [...turns, { role: 'user', text: prompt }]);
    this.composer.set('');
    this.isAsking.set(true);
    this.error.set(null);
    this.#agentsApi.invokePublished(agent.type, prompt).subscribe({
      next: (response) => {
        this.turns.update(turns => [...turns, { role: 'assistant', text: response.reply }]);
        this.isAsking.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t().agents.errors.tryRunFailed);
        this.isAsking.set(false);
      },
    });
  }

  /** Open form to override a builtin: create or edit its custom agent. */
  customizeBuiltin(agent: AgentInfoResponse): void {
    const existing = this.findCustomAgentForType(agent.type);
    if (existing !== undefined) {
      this.startEditCustomAgent(existing);
      return;
    }
    this.editingId.set(null);
    this.isFormAgentTypeLocked.set(true);
    this.#draft.set({
      agentType: agent.type,
      name: agent.name,
      description: agent.description,
      systemPrompt: agent.systemPrompt,
      tools: [...agent.tools],
    });
    this.showForm.set(true);
  }

  /** Customize from a gallery card (builtin or library). */
  customizeFromGallery(agent: GalleryAgent): void {
    if (agent.source === 'custom' && agent.custom !== undefined) {
      this.startEditCustomAgent(agent.custom);
      return;
    }
    const builtin = this.catalog().find(item => item.type === agent.type);
    if (builtin !== undefined) {
      this.customizeBuiltin(builtin);
    }
  }

  /** Opens the form to edit a custom agent. */
  startEditCustomAgent(agent: CustomAgent): void {
    this.editingId.set(agent.id);
    this.isFormAgentTypeLocked.set(true);
    this.#draft.set({
      agentType: agent.agentType,
      name: agent.name,
      description: agent.description,
      systemPrompt: agent.systemPrompt,
      tools: [...agent.tools],
    });
    this.showForm.set(true);
  }

  /** Turns a custom agent on or off. */
  toggleEnabled(agent: CustomAgent): void {
    this.#agentsApi.setEnabled(agent.id, !agent.enabled).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().agents.errors.updateFailed),
    });
  }

  /** Deletes a custom agent after confirmation. */
  delete(agent: CustomAgent): void {
    const message = this.i18n.t().agents.deleteConfirm.replace('{name}', agent.name);
    if (!globalThis.confirm(message)) {
      return;
    }
    this.#agentsApi.deleteAgent(agent.id).subscribe({
      next: () => {
        if (this.selected()?.custom?.id === agent.id) {
          this.selected.set(null);
          this.turns.set([]);
        }
        this.reload();
      },
      error: () => this.error.set(this.i18n.t().agents.errors.deleteFailed),
    });
  }

  /** Loads the agent catalog and custom agents. */
  reload(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.#agentsApi.listCatalog().subscribe({
      next: (catalog) => {
        this.catalog.set(catalog);
        this.isLoading.set(false);
        const current = this.selected();
        if (current === null) {
          const first = catalog.find(agent => !agent.supervisor);
          if (first !== undefined) {
            this.selectAgent({
              type: first.type,
              name: first.name,
              description: first.description,
              tools: [...first.tools],
              source: 'builtin',
            });
          }
        }
      },
      error: () => {
        this.error.set(this.i18n.t().agents.errors.loadFailed);
        this.isLoading.set(false);
      },
    });
    this.#agentsApi.listCustomAgents().subscribe({
      next: customAgents => this.customAgents.set(customAgents),
      error: () => undefined,
    });
  }

  /** Finds the custom agent for the agent type. */
  findCustomAgentForType(agentType: string): CustomAgent | undefined {
    return this.customAgents().find(item => item.agentType === agentType);
  }
}
