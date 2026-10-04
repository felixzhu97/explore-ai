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
  type SavedAgent,
  type UpdateSavedAgentRequest,
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

  readonly catalog = signal<AgentInfoResponse[]>([]);
  readonly savedAgents = signal<SavedAgent[]>([]);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
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

  readonly builtins = computed(() => this.catalog().filter(agent => !agent.supervisor),
  );

  ngOnInit(): void {
    this.reload();
  }

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
    this.#agentsApi.listSavedAgents().subscribe({
      next: savedAgents => this.savedAgents.set(savedAgents),
      error: () => undefined,
    });
  }

  savedAgentForType(typeKey: string): SavedAgent | undefined {
    return this.savedAgents().find(item => item.typeKey === typeKey);
  }

  startCreate(): void {
    this.editingId.set(null);
    this.isFormTypeKeyLocked.set(false);
    this.#draft.set(EMPTY_DRAFT);
    this.showForm.set(true);
  }

  startEditSavedAgent(agent: SavedAgent): void {
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

  /** Open form to override a builtin: create or edit its saved agent. */
  customizeBuiltin(agent: AgentInfoResponse): void {
    const existing = this.savedAgentForType(agent.type);
    if (existing !== undefined) {
      this.startEditSavedAgent(existing);
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

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  isToolSelected(toolKey: string): boolean {
    return this.#draft().toolKeys.includes(toolKey);
  }

  toggleTool(toolKey: string): void {
    this.draftForm.toolKeys().value.update(current => (current.includes(toolKey)
      ? current.filter(key => key !== toolKey)
      : [...current, toolKey]));
  }

  save(): void {
    if (this.draftForm().invalid()) {
      this.error.set(this.i18n.t().agents.errors.nameRequired);
      return;
    }
    const draft = this.#draft();
    const typeKey = draft.typeKey.trim().toLowerCase();
    const request: UpdateSavedAgentRequest = {
      name: draft.name.trim(),
      description: draft.description.trim(),
      systemPrompt: draft.systemPrompt.trim(),
      toolKeys: [...draft.toolKeys],
    };
    this.isSaving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request$ = hasText(id)
      ? this.#agentsApi.update(id, request)
      : this.#agentsApi.create({ ...request, typeKey });
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

  toggleEnabled(agent: SavedAgent): void {
    this.#agentsApi.setEnabled(agent.id, !agent.enabled).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().agents.errors.updateFailed),
    });
  }

  delete(agent: SavedAgent): void {
    const message = this.i18n.t().agents.deleteConfirm.replace('{name}', agent.name);
    if (!globalThis.confirm(message)) {
      return;
    }
    this.#agentsApi.delete(agent.id).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().agents.errors.deleteFailed),
    });
  }
}
