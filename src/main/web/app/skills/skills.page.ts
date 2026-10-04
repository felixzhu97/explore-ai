import {
  Component,
  type OnInit,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../ui/notification.service';
import { I18nService } from '../i18n';
import {
  SkillsService,
  type Skill,
  type SkillTemplate,
  type SkillWriteRequest,
} from './skills.service';
import { ZardButtonComponent } from '../ui/button';

const EMPTY_FORM: SkillWriteRequest = {
  name: '',
  description: '',
  instructions: '',
  allowedTools: [],
};

@Component({
  selector: 'app-skills-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './skills.page.html',
  host: {
    class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6',
  },
})
export class SkillsPageComponent implements OnInit {
  readonly #skillsApi = inject(SkillsService);
  readonly #notifications = inject(NotificationService);
  protected readonly i18n = inject(I18nService);

  readonly skills = signal<Skill[]>([]);
  readonly templates = signal<SkillTemplate[]>([]);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly addingTemplateId = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly editingId = signal<string | null>(null);
  readonly formName = signal('');
  readonly formDescription = signal('');
  readonly formInstructions = signal('');
  readonly showForm = signal(false);

  readonly ownedNames = computed(() => {
    const names = new Set<string>();
    for (const skill of this.skills()) {
      names.add(skill.name.trim().toLowerCase());
    }
    return names;
  });

  constructor() {
    effect(() => {
      this.i18n.language();
      this.#reloadTemplates();
    });
  }

  ngOnInit(): void {
    this.reload();
  }

  isInLibrary(template: SkillTemplate): boolean {
    const owned = this.ownedNames();
    const aliases = [template.name, ...(template.nameAliases ?? [])]
      .map(name => name.trim().toLowerCase())
      .filter(Boolean);
    return aliases.some(alias => owned.has(alias) || [...owned].some(ownedName => ownedName.startsWith(`${alias} (`)),
    );
  }

  reload(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.#skillsApi.list().subscribe({
      next: (skills) => {
        this.skills.set(skills);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t().skills.errors.loadFailed);
        this.isLoading.set(false);
      },
    });
    this.#reloadTemplates();
  }

  #reloadTemplates(): void {
    this.#skillsApi.listTemplates().subscribe({
      next: templates => this.templates.set(templates),
      error: () => undefined,
    });
  }

  startCreate(): void {
    this.editingId.set(null);
    this.#applyForm(EMPTY_FORM);
    this.showForm.set(true);
  }

  startEdit(skill: Skill): void {
    this.editingId.set(skill.id);
    this.#applyForm({
      name: skill.name,
      description: skill.description,
      instructions: skill.instructions,
      allowedTools: skill.allowedTools,
    });
    this.showForm.set(true);
  }

  customizeTemplate(template: SkillTemplate): void {
    this.editingId.set(null);
    this.#applyForm({
      name: template.name,
      description: template.description,
      instructions: template.instructions,
      allowedTools: template.allowedTools,
    });
    this.showForm.set(true);
  }

  addFromTemplate(template: SkillTemplate): void {
    if (this.addingTemplateId()) {
      return;
    }
    this.addingTemplateId.set(template.id);
    this.error.set(null);
    this.#skillsApi.createFromTemplate(template.id).subscribe({
      next: () => {
        this.addingTemplateId.set(null);
        this.#notifications.showSuccess(this.i18n.t().skills.added);
        this.reload();
      },
      error: () => {
        this.addingTemplateId.set(null);
        this.error.set(this.i18n.t().skills.errors.saveFailed);
      },
    });
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    const request = this.#readForm();
    if (!request.name.trim() || !request.instructions.trim()) {
      this.error.set(this.i18n.t().skills.errors.nameRequired);
      return;
    }
    this.isSaving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request$ = id
      ? this.#skillsApi.update(id, request)
      : this.#skillsApi.create(request);
    request$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.showForm.set(false);
        this.#notifications.showSuccess(this.i18n.t().common.success);
        this.reload();
      },
      error: () => {
        this.error.set(this.i18n.t().skills.errors.saveFailed);
        this.isSaving.set(false);
      },
    });
  }

  toggleEnabled(skill: Skill): void {
    this.#skillsApi.setEnabled(skill.id, !skill.enabled).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().skills.errors.updateFailed),
    });
  }

  remove(skill: Skill): void {
    const message = this.i18n.tReplace(this.i18n.t().skills.deleteConfirm, {
      name: skill.name,
    });
    if (!confirm(message)) {
      return;
    }
    this.#skillsApi.delete(skill.id).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().skills.errors.deleteFailed),
    });
  }

  #applyForm(request: SkillWriteRequest): void {
    this.formName.set(request.name);
    this.formDescription.set(request.description);
    this.formInstructions.set(request.instructions);
  }

  #readForm(): SkillWriteRequest {
    return {
      name: this.formName().trim(),
      description: this.formDescription().trim(),
      instructions: this.formInstructions().trim(),
      allowedTools: [],
    };
  }
}
