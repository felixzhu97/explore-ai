import {
  Component,
  type OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { email, form, FormField, required } from '@angular/forms/signals';
import { ChronoUnit, Instant } from '@js-joda/core';
import { NotificationService } from '../ui/notification.service';
import { I18nService } from '../i18n';
import {
  PipelinesService,
  type PipelineTemplate,
} from '../pipelines/pipelines.service';
import { ZardButtonComponent } from '../ui/button';
import { requiredText } from '../forms/required-text';
import { DATE_TIME, formatInstant, ONCE_TERMINAL_NEXT } from '../time/instant-format';
import { InstantPickerComponent } from '../time/instant-picker.component';
import { systemZoneName } from '../time/native-date';
import {
  AutomationsService,
  type AutomationRun,
  type AutomationSchedule,
  type AutomationScheduleWriteRequest,
} from './automations.service';

type FrequencyPreset = 'daily' | 'weekly' | 'custom';

function isFrequencyPreset(value: string): value is FrequencyPreset {
  return value === 'daily' || value === 'weekly' || value === 'custom';
}

function cronForPreset(preset: FrequencyPreset): string {
  switch (preset) {
    case 'daily':
      return '0 0 9 * * *';
    case 'weekly':
      return '0 0 9 * * MON';
    case 'custom':
      return '';
  }
}

function presetFromSchedule(schedule: AutomationSchedule): FrequencyPreset {
  if (schedule.scheduleKind === 'ONCE') {
    return 'custom';
  }
  const cron = schedule.cronExpression ?? '';
  if (cron === '0 0 9 * * *') {
    return 'daily';
  }
  if (cron === '0 0 9 * * MON') {
    return 'weekly';
  }
  return 'daily';
}

/** Default run-at: now + 5 minutes, truncated to seconds. */
function defaultRunAt(): Instant {
  return Instant.now().plus(5, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.SECONDS);
}

function isOnceTerminal(instant: Instant): boolean {
  return !instant.isBefore(ONCE_TERMINAL_NEXT);
}

interface AutomationDraft {
  name: string;
  email: string;
  timezone: string;
  templateId: string;
  brief: string;
  preset: FrequencyPreset;
  runAt: Instant | null;
}

function emptyDraft(): AutomationDraft {
  return {
    name: '',
    email: '',
    timezone: systemZoneName(),
    templateId: '',
    brief: '',
    preset: 'daily',
    runAt: defaultRunAt(),
  };
}

@Component({
  selector: 'app-automations-page',
  imports: [FormField, ZardButtonComponent, InstantPickerComponent],
  templateUrl: './automations.page.html',
  styleUrl: './automations.page.css',
  host: {
    class: 'flex flex-1 min-h-0 w-full flex-col overflow-y-auto bg-surface px-4 py-6',
  },
})
export class AutomationsPageComponent implements OnInit {
  readonly #automationsApi = inject(AutomationsService);
  readonly #pipelinesApi = inject(PipelinesService);
  readonly #notifications = inject(NotificationService);
  protected readonly i18n = inject(I18nService);

  readonly schedules = signal<AutomationSchedule[]>([]);
  readonly templates = signal<PipelineTemplate[]>([]);
  readonly runs = signal<AutomationRun[]>([]);
  readonly isLoading = signal(true);
  readonly isSaving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly historyScheduleId = signal<string | null>(null);

  readonly #draft = signal<AutomationDraft>(emptyDraft());
  protected readonly draftForm = form(this.#draft, (path) => {
    requiredText(path.name);
    requiredText(path.email);
    email(path.email);
    requiredText(path.templateId);
    requiredText(path.brief);
    required(path.runAt, { when: field => field.valueOf(path.preset) === 'custom' });
  });

  readonly enabledTemplates = computed(() => {
    return this.templates().filter(template => template.enabled);
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.isLoading.set(true);
    this.error.set(null);
    this.#pipelinesApi.listTemplates().subscribe({
      next: templates => this.templates.set(templates),
      error: () => this.templates.set([]),
    });
    this.#automationsApi.list().subscribe({
      next: (schedules) => {
        this.schedules.set(schedules);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t().automations.errors.loadFailed);
        this.isLoading.set(false);
      },
    });
  }

  startCreate(): void {
    this.editingId.set(null);
    const first = this.enabledTemplates()[0];
    this.#draft.set({
      ...emptyDraft(),
      templateId: first?.id ?? '',
      brief: this.#defaultBriefForTemplate(first),
    });
    this.showForm.set(true);
  }

  onTemplateChange(templateId: string): void {
    this.draftForm.templateId().value.set(templateId);
    const template = this.enabledTemplates().find(item => item.id === templateId);
    if (!template) {
      return;
    }
    const brief = this.draftForm.brief().value();
    if (!brief.trim() || this.#isGenericPlaceholder(brief)) {
      this.draftForm.brief().value.set(this.#defaultBriefForTemplate(template));
    }
  }

  startEdit(schedule: AutomationSchedule): void {
    this.editingId.set(schedule.id);
    const templateId = schedule.pipelineTemplateId;
    const template = this.enabledTemplates().find(item => item.id === templateId)
      ?? this.templates().find(item => item.id === templateId);
    this.#draft.set({
      name: schedule.name,
      email: schedule.recipientEmail,
      timezone: schedule.timezone,
      templateId,
      brief: this.#isGenericPlaceholder(schedule.brief)
        ? this.#defaultBriefForTemplate(template)
        : schedule.brief,
      preset: presetFromSchedule(schedule),
      runAt: this.#runAtForEdit(schedule),
    });
    this.showForm.set(true);
  }

  #runAtForEdit(schedule: AutomationSchedule): Instant {
    if (schedule.scheduleKind !== 'ONCE' || this.isOnceCompleted(schedule)) {
      return defaultRunAt();
    }
    return schedule.runAt ?? schedule.nextRunAt;
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  onPresetChange(preset: string): void {
    if (!isFrequencyPreset(preset)) {
      return;
    }
    this.#draft.update(draft => ({
      ...draft,
      preset,
      runAt: preset === 'custom' && draft.runAt === null ? defaultRunAt() : draft.runAt,
    }));
  }

  save(): void {
    const t = this.i18n.t().automations;
    const invalidMessage = this.#firstInvalidMessage();
    if (invalidMessage) {
      this.#notifications.showError(invalidMessage);
      return;
    }
    const draft = this.#draft();
    const name = draft.name.trim();
    const email = draft.email.trim();
    const pipelineTemplateId = draft.templateId;
    const brief = draft.brief.trim();
    const preset = draft.preset;
    const timezone = draft.timezone.trim() || 'UTC';
    let request: AutomationScheduleWriteRequest;
    if (preset === 'custom') {
      const runAt = draft.runAt;
      if (runAt === null) {
        this.#notifications.showError(t.errors.runAtRequired);
        return;
      }
      if (!runAt.isAfter(Instant.now())) {
        this.#notifications.showError(t.errors.runAtPast);
        return;
      }
      request = {
        name,
        scheduleKind: 'ONCE',
        runAt: runAt.toString(),
        timezone,
        pipelineTemplateId,
        recipientEmail: email,
        brief,
      };
    } else {
      request = {
        name,
        scheduleKind: 'CRON',
        cronExpression: cronForPreset(preset),
        timezone,
        pipelineTemplateId,
        recipientEmail: email,
        brief,
      };
    }
    this.isSaving.set(true);
    const editingId = this.editingId();
    const request$ = editingId
      ? this.#automationsApi.update(editingId, request)
      : this.#automationsApi.create(request);
    request$.subscribe({
      next: () => {
        this.isSaving.set(false);
        this.showForm.set(false);
        this.#notifications.showSuccess(this.i18n.t().common.success);
        this.reload();
      },
      error: () => {
        this.isSaving.set(false);
        this.#notifications.showError(t.errors.saveFailed);
      },
    });
  }

  #firstInvalidMessage(): string | null {
    const errors = this.i18n.t().automations.errors;
    const fields = this.draftForm;
    if (fields.name().invalid()) {
      return errors.nameRequired;
    }
    if (fields.email().invalid()) {
      return this.#draft().email.trim() ? errors.emailInvalid : errors.emailRequired;
    }
    if (fields.templateId().invalid()) {
      return errors.pipelineTemplateRequired;
    }
    if (fields.brief().invalid()) {
      return errors.briefRequired;
    }
    if (fields.runAt().invalid()) {
      return errors.runAtRequired;
    }
    return null;
  }

  toggleEnabled(schedule: AutomationSchedule): void {
    if (this.isOnceCompleted(schedule) && !schedule.enabled) {
      this.#notifications.showWarning(this.i18n.t().automations.onceCompletedHint);
      this.startEdit(schedule);
      return;
    }
    this.#automationsApi.setEnabled(schedule.id, !schedule.enabled).subscribe({
      next: () => this.reload(),
      error: () => {
        this.#notifications.showError(this.i18n.t().automations.errors.saveFailed);
      },
    });
  }

  remove(schedule: AutomationSchedule): void {
    if (!confirm(this.i18n.t().automations.deleteConfirm)) {
      return;
    }
    this.#automationsApi.delete(schedule.id).subscribe({
      next: () => this.reload(),
      error: () => {
        this.#notifications.showError(this.i18n.t().automations.errors.deleteFailed);
      },
    });
  }

  showHistory(schedule: AutomationSchedule): void {
    this.historyScheduleId.set(schedule.id);
    this.#automationsApi.listRuns(schedule.id).subscribe({
      next: runs => this.runs.set(runs),
      error: () => {
        this.runs.set([]);
        this.#notifications.showError(this.i18n.t().automations.errors.loadFailed);
      },
    });
  }

  templateName(id: string): string {
    return this.templates().find(template => template.id === id)?.name ?? id;
  }

  #defaultBriefForTemplate(template: PipelineTemplate | undefined): string {
    if (!template) {
      return '';
    }
    const topic = template.shortTopic.trim();
    if (topic) {
      return topic;
    }
    return template.briefPrompt.trim();
  }

  #isGenericPlaceholder(brief: string): boolean {
    return brief.trim().toLowerCase()
      === 'follow the configured agent pipeline for the user task.';
  }

  /** One-shot schedules auto-disable; nextRunAt becomes a far-future sentinel. */
  isOnceCompleted(schedule: AutomationSchedule): boolean {
    if (schedule.scheduleKind !== 'ONCE') {
      return false;
    }
    if (schedule.lastRunAt !== null && !schedule.enabled) {
      return true;
    }
    return isOnceTerminal(schedule.nextRunAt);
  }

  statusLabel(schedule: AutomationSchedule): string {
    const t = this.i18n.t().automations;
    if (this.isOnceCompleted(schedule)) {
      return t.statusCompleted;
    }
    return schedule.enabled ? t.statusEnabled : t.statusDisabled;
  }

  scheduleSummary(schedule: AutomationSchedule): string {
    if (schedule.scheduleKind === 'ONCE') {
      const when = this.isOnceCompleted(schedule)
        ? schedule.lastRunAt
        : (schedule.runAt ?? schedule.nextRunAt);
      return `${this.i18n.t().automations.frequencyCustom}: ${this.displayInstant(when)}`;
    }
    return schedule.cronExpression ?? '—';
  }

  formatNextRun(schedule: AutomationSchedule): string {
    if (this.isOnceCompleted(schedule)) {
      return this.i18n.t().automations.nextRunNone;
    }
    return this.displayInstant(schedule.nextRunAt);
  }

  displayInstant(value: Instant | null): string {
    if (value === null) {
      return '—';
    }
    if (isOnceTerminal(value)) {
      return this.i18n.t().automations.nextRunNone;
    }
    return formatInstant(value, DATE_TIME);
  }
}
