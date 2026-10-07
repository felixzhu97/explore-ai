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

const TOOL_KEYS = ['web', 'weather', 'datetime', 'document'] as const;

interface AgentDraft {
  typeKey: string;
  name: string;
  description: string;
  systemPrompt: string;
  toolKeys: string[];
}

const EMPTY_DRAFT: AgentDraft = {
  typeKey: '',
  name: '',
  description: '',
  systemPrompt: '',
  toolKeys: [],
};

@Component({
  selector: 'app-agents-page',
  imports: [FormField, ZardButtonComponent],
  templateUrl: './agents.page.html',
  host: {
    class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6',
  },
})
export class AgentsPageComponent implements OnInit {
  readonly #agentsApi = inject(AgentsService);
  readonly #notifications = inject(NotificationService);
  protected readonly i18n = inject(I18nService);

  readonly error = signal<string | null>(null);

  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly isFormTypeKeyLocked = signal(false);
  readonly #draft = signal<AgentDraft>(EMPTY_DRAFT);

  protected readonly draftForm = form(this.#draft, (path) => {
    disabled(path.typeKey, { when: () => this.isFormTypeKeyLocked() });
    requiredText(path.typeKey, () => !hasText(this.editingId()));
    requiredText(path.name);
    requiredText(path.systemPrompt);
  });

  readonly availableToolKeys = TOOL_KEYS;
  readonly isSaving = signal(false);

  readonly isLoading = signal(true);
  readonly catalog = signal<AgentInfoResponse[]>([]);

  readonly builtins = computed(() => this.catalog().filter(agent => !agent.supervisor),
  );

  readonly customAgents = signal<CustomAgent[]>([]);

  ngOnInit(): void {
    this.reload();
  }

  /** Opens the create form with an empty draft. */
  startCreate(): void {
    this.editingId.set(null);
    this.isFormTypeKeyLocked.set(false);
    this.#draft.set(EMPTY_DRAFT);
    this.showForm.set(true);
  }

  /** Tells whether the draft includes the tool. */
  isToolSelected(toolKey: string): boolean {
    return this.#draft().toolKeys.includes(toolKey);
  }

  /** Adds or removes the tool in the draft. */
  toggleTool(toolKey: string): void {
    this.draftForm.toolKeys().value.update(current => (current.includes(toolKey)
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
    const typeKey = draft.typeKey.trim().toLowerCase();
    const request: UpdateCustomAgentRequest = {
      name: draft.name.trim(),
      description: draft.description.trim(),
      systemPrompt: draft.systemPrompt.trim(),
      toolKeys: [...draft.toolKeys],
    };
    this.isSaving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request$ = hasText(id)
      ? this.#agentsApi.updateAgent(id, request)
      : this.#agentsApi.createAgent({ ...request, typeKey });
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

  /** Open form to override a builtin: create or edit its custom agent. */
  customizeBuiltin(agent: AgentInfoResponse): void {
    const existing = this.findCustomAgentForType(agent.type);
    if (existing !== undefined) {
      this.startEditCustomAgent(existing);
      return;
    }
    this.editingId.set(null);
    this.isFormTypeKeyLocked.set(true);
    this.#draft.set({
      typeKey: agent.type,
      name: agent.name,
      description: agent.description,
      systemPrompt: agent.systemPrompt,
      toolKeys: [...agent.toolKeys],
    });
    this.showForm.set(true);
  }

  /** Opens the form to edit a custom agent. */
  startEditCustomAgent(agent: CustomAgent): void {
    this.editingId.set(agent.id);
    this.isFormTypeKeyLocked.set(true);
    this.#draft.set({
      typeKey: agent.typeKey,
      name: agent.name,
      description: agent.description,
      systemPrompt: agent.systemPrompt,
      toolKeys: [...agent.toolKeys],
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
      next: () => this.reload(),
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
  findCustomAgentForType(typeKey: string): CustomAgent | undefined {
    return this.customAgents().find(item => item.typeKey === typeKey);
  }
}
