import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../core/notification.service';
import { I18nService } from '../core/i18n';
import { AgentsService } from './agents.service';
import type { SavedAgent, SavedAgentWriteRequest } from './agents.model';
import type { AgentInfo } from '../pipelines/pipelines.model';

const TOOL_KEYS = ['web', 'weather', 'datetime', 'document'] as const;

@Component({
  selector: 'app-agents-page',
  imports: [FormsModule],
  templateUrl: './agents.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class AgentsPageComponent implements OnInit {
  private readonly agentsApi = inject(AgentsService);
  private readonly notifications = inject(NotificationService);
  protected readonly i18n = inject(I18nService);

  readonly catalog = signal<AgentInfo[]>([]);
  readonly library = signal<SavedAgent[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly formTypeKey = signal('');
  readonly formTypeKeyLocked = signal(false);
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
    this.loading.set(true);
    this.error.set(null);
    this.agentsApi.listCatalog().subscribe({
      next: (catalog) => {
        this.catalog.set(catalog);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t().agentsPage.loadFailed);
        this.loading.set(false);
      },
    });
    this.agentsApi.listLibrary().subscribe({
      next: library => this.library.set(library),
      error: () => undefined,
    });
  }

  libraryEntryForType(typeKey: string): SavedAgent | undefined {
    return this.library().find(item => item.typeKey === typeKey);
  }

  startCreate(): void {
    this.editingId.set(null);
    this.formTypeKeyLocked.set(false);
    this.formTypeKey.set('');
    this.formName.set('');
    this.formDescription.set('');
    this.formSystemPrompt.set('');
    this.formToolKeys.set([]);
    this.showForm.set(true);
  }

  startEditLibrary(agent: SavedAgent): void {
    this.editingId.set(agent.id);
    this.formTypeKeyLocked.set(true);
    this.formTypeKey.set(agent.typeKey);
    this.formName.set(agent.name);
    this.formDescription.set(agent.description);
    this.formSystemPrompt.set(agent.systemPrompt);
    this.formToolKeys.set([...(agent.toolKeys ?? [])]);
    this.showForm.set(true);
  }

  /** Open form to override a builtin (create or edit library row with same typeKey). */
  customizeBuiltin(agent: AgentInfo): void {
    const existing = this.libraryEntryForType(agent.type);
    if (existing) {
      this.startEditLibrary(existing);
      return;
    }
    this.editingId.set(null);
    this.formTypeKeyLocked.set(true);
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
      this.error.set(this.i18n.t().agentsPage.nameRequired);
      return;
    }
    const request: SavedAgentWriteRequest = {
      name,
      description: this.formDescription().trim(),
      systemPrompt,
      toolKeys: [...this.formToolKeys()],
    };
    this.saving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request$ = id
      ? this.agentsApi.update(id, request)
      : this.agentsApi.create({ ...request, typeKey });
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.notifications.showSuccess(this.i18n.t().common.success);
        this.reload();
      },
      error: () => {
        this.error.set(this.i18n.t().agentsPage.saveFailed);
        this.saving.set(false);
      },
    });
  }

  toggleEnabled(agent: SavedAgent): void {
    this.agentsApi.setEnabled(agent.id, !agent.enabled).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().agentsPage.updateFailed),
    });
  }

  delete(agent: SavedAgent): void {
    const message = this.i18n.t().agentsPage.deleteConfirm.replace('{name}', agent.name);
    if (!globalThis.confirm(message)) {
      return;
    }
    this.agentsApi.delete(agent.id).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().agentsPage.deleteFailed),
    });
  }
}
