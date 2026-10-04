import {
  Component,
  type OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../ui/notification.service';
import { I18nService } from '../i18n';
import {
  AgentsService,
  type SavedAgent,
  type SavedAgentWriteRequest,
} from './agents.service';
import type { AgentType } from '../pipelines/pipelines.service';
import { ZardButtonComponent } from '../ui/button';

const TOOL_KEYS = ['web', 'weather', 'datetime', 'document'] as const;

@Component({
  selector: 'app-agents-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './agents.page.html',
  host: {
    class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6',
  },
})
export class AgentsPageComponent implements OnInit {
  readonly #agentsApi = inject(AgentsService);
  readonly #notifications = inject(NotificationService);
  protected readonly i18n = inject(I18nService);

  readonly catalog = signal<AgentType[]>([]);
  readonly savedAgents = signal<SavedAgent[]>([]);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly formTypeKey = signal('');
  readonly isFormTypeKeyLocked = signal(false);
  readonly formName = signal('');
  readonly formDescription = signal('');
  readonly formSystemPrompt = signal('');
  readonly formToolKeys = signal<string[]>([]);

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
    this.formTypeKey.set('');
    this.formName.set('');
    this.formDescription.set('');
    this.formSystemPrompt.set('');
    this.formToolKeys.set([]);
    this.showForm.set(true);
  }

  startEditSavedAgent(agent: SavedAgent): void {
    this.editingId.set(agent.id);
    this.isFormTypeKeyLocked.set(true);
    this.formTypeKey.set(agent.typeKey);
    this.formName.set(agent.name);
    this.formDescription.set(agent.description);
    this.formSystemPrompt.set(agent.systemPrompt);
    this.formToolKeys.set([...agent.toolKeys]);
    this.showForm.set(true);
  }

  /** Open form to override a builtin: create or edit its saved agent. */
  customizeBuiltin(agent: AgentType): void {
    const existing = this.savedAgentForType(agent.type);
    if (existing) {
      this.startEditSavedAgent(existing);
      return;
    }
    this.editingId.set(null);
    this.isFormTypeKeyLocked.set(true);
    this.formTypeKey.set(agent.type);
    this.formName.set(agent.name);
    this.formDescription.set(agent.description);
    this.formSystemPrompt.set(agent.systemPrompt ?? '');
    this.formToolKeys.set([...(agent.toolKeys ?? [])]);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  isToolSelected(toolKey: string): boolean {
    return this.formToolKeys().includes(toolKey);
  }

  toggleTool(toolKey: string): void {
    const current = this.formToolKeys();
    if (current.includes(toolKey)) {
      this.formToolKeys.set(current.filter(key => key !== toolKey));
      return;
    }
    this.formToolKeys.set([...current, toolKey]);
  }

  save(): void {
    const name = this.formName().trim();
    const systemPrompt = this.formSystemPrompt().trim();
    const typeKey = this.formTypeKey().trim().toLowerCase();
    if (!name || !systemPrompt || (!this.editingId() && !typeKey)) {
      this.error.set(this.i18n.t().agents.errors.nameRequired);
      return;
    }
    const request: SavedAgentWriteRequest = {
      name,
      description: this.formDescription().trim(),
      systemPrompt,
      toolKeys: [...this.formToolKeys()],
    };
    this.isSaving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request$ = id
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
