import {
  Component,
  type OnInit,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { form, FormField } from '@angular/forms/signals';
import { NotificationService } from '../ui/notification.service';
import { I18nService } from '../i18n';
import {
  SkillsService,
  type Skill,
  type SkillTemplateResponse,
  type CreateSkillRequest,
} from './skills.service';
import { ZardButtonComponent } from '../ui/button';
import { requiredText } from '../forms/required-text';
import { hasText } from '../shared/presence';

interface SkillDraft {
  name: string;
  description: string;
  instructions: string;
}

const EMPTY_DRAFT: SkillDraft = {
  name: '',
  description: '',
  instructions: '',
};

@Component({
  selector: 'app-skills-page',
  imports: [FormField, ZardButtonComponent],
  templateUrl: './skills.page.html',
  host: {
    class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6',
  },
})
export class SkillsPageComponent implements OnInit {
  readonly #skillsApi = inject(SkillsService);
  readonly #notifications = inject(NotificationService);
  protected readonly i18n = inject(I18nService);

  readonly error = signal<string | null>(null);

  readonly templates = signal<SkillTemplateResponse[]>([]);
  readonly addingTemplateId = signal<string | null>(null);

  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly #draft = signal<SkillDraft>(EMPTY_DRAFT);

  protected readonly draftForm = form(this.#draft, (path) => {
    requiredText(path.name);
    requiredText(path.instructions);
  });

  readonly isSaving = signal(false);

  readonly isLoading = signal(true);
  readonly skills = signal<Skill[]>([]);

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

  /** Opens the create form with an empty draft. */
  startCreate(): void {
    this.editingId.set(null);
    this.#draft.set(EMPTY_DRAFT);
    this.showForm.set(true);
  }

  /** Tells whether the template is already in the library. */
  isInLibrary(template: SkillTemplateResponse): boolean {
    const owned = this.ownedNames();
    const aliases = [template.name, ...template.nameAliases]
      .map(name => name.trim().toLowerCase())
      .filter(Boolean);
    return aliases.some(alias => owned.has(alias) || [...owned].some(ownedName => ownedName.startsWith(`${alias} (`)),
    );
  }

  /** Adds a template to the library. */
  addFromTemplate(template: SkillTemplateResponse): void {
    if (hasText(this.addingTemplateId())) {
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

  /** Opens the create form filled from a template. */
  customizeTemplate(template: SkillTemplateResponse): void {
    this.editingId.set(null);
    this.#draft.set({
      name: template.name,
      description: template.description,
      instructions: template.instructions,
    });
    this.showForm.set(true);
  }

  /** Creates or updates the skill from the draft. */
  save(): void {
    if (this.draftForm().invalid()) {
      this.error.set(this.i18n.t().skills.errors.nameRequired);
      return;
    }
    const draft = this.#draft();
    const request: CreateSkillRequest = {
      name: draft.name.trim(),
      description: draft.description.trim(),
      instructions: draft.instructions.trim(),
      allowedTools: [],
    };
    this.isSaving.set(true);
    this.error.set(null);
    const id = this.editingId();
    const request$ = hasText(id)
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

  /** Closes the form without saving. */
  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  /** Opens the form to edit a skill. */
  startEdit(skill: Skill): void {
    this.editingId.set(skill.id);
    this.#draft.set({
      name: skill.name,
      description: skill.description,
      instructions: skill.instructions,
    });
    this.showForm.set(true);
  }

  /** Turns a skill on or off. */
  toggleEnabled(skill: Skill): void {
    this.#skillsApi.setEnabled(skill.id, !skill.enabled).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().skills.errors.updateFailed),
    });
  }

  /** Deletes a skill after confirmation. */
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

  /** Loads the skills and templates. */
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
}
