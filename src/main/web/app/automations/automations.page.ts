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
import { PipelinesService } from '../pipelines/pipelines.service';
import type { SavedWorkflowTemplate } from '../pipelines/pipelines.model';
import { ZardButtonComponent } from '../shared/components/button';
import { AutomationsService } from './automations.service';
import {
  cronForPreset,
  presetFromCron,
  type AutomationRun,
  type AutomationSchedule,
  type FrequencyPreset,
} from './automations.model';

@Component({
  selector: 'app-automations-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './automations.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'flex flex-1 min-h-0 w-full flex-col overflow-hidden bg-surface' },
})
export class AutomationsPageComponent implements OnInit {
  private readonly automationsApi = inject(AutomationsService);
  private readonly pipelinesApi = inject(PipelinesService);
  private readonly notifications = inject(NotificationService);
  protected readonly i18n = inject(I18nService);

  readonly schedules = signal<AutomationSchedule[]>([]);
  readonly workflows = signal<SavedWorkflowTemplate[]>([]);
  readonly runs = signal<AutomationRun[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly historyScheduleId = signal<string | null>(null);

  readonly formName = signal('');
  readonly formEmail = signal('');
  readonly formTimezone = signal(Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC');
  readonly formWorkflowId = signal('');
  readonly formBrief = signal('');
  readonly formPreset = signal<FrequencyPreset>('daily');
  readonly formCustomCron = signal('0 0 9 * * *');

  readonly enabledWorkflows = computed(() => {
    return this.workflows().filter(workflow => workflow.enabled);
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.pipelinesApi.listLibrary().subscribe({
      next: workflows => this.workflows.set(workflows),
      error: () => this.workflows.set([]),
    });
    this.automationsApi.list().subscribe({
      next: (schedules) => {
        this.schedules.set(schedules);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t().automationsPage.loadFailed);
        this.loading.set(false);
      },
    });
  }

  startCreate(): void {
    this.editingId.set(null);
    this.formName.set('');
    this.formEmail.set('');
    this.formTimezone.set(Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC');
    this.formWorkflowId.set(this.enabledWorkflows()[0]?.id ?? '');
    this.formBrief.set('Follow the configured agent pipeline for the user task.');
    this.formPreset.set('daily');
    this.formCustomCron.set('0 0 9 * * *');
    this.showForm.set(true);
  }

  startEdit(schedule: AutomationSchedule): void {
    this.editingId.set(schedule.id);
    this.formName.set(schedule.name);
    this.formEmail.set(schedule.recipientEmail);
    this.formTimezone.set(schedule.timezone);
    this.formWorkflowId.set(schedule.workflowTemplateId);
    this.formBrief.set(schedule.brief);
    this.formPreset.set(presetFromCron(schedule.cronExpression));
    this.formCustomCron.set(schedule.cronExpression);
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  save(): void {
    const name = this.formName().trim();
    const email = this.formEmail().trim();
    const workflowTemplateId = this.formWorkflowId();
    const brief = this.formBrief().trim();
    const t = this.i18n.t().automationsPage;
    if (!name) {
      this.notifications.showError(t.nameRequired);
      return;
    }
    if (!email) {
      this.notifications.showError(t.emailRequired);
      return;
    }
    if (!workflowTemplateId) {
      this.notifications.showError(t.workflowRequired);
      return;
    }
    if (!brief) {
      this.notifications.showError(t.briefRequired);
      return;
    }
    const request = {
      name,
      cronExpression: cronForPreset(this.formPreset(), this.formCustomCron()),
      timezone: this.formTimezone().trim() || 'UTC',
      workflowTemplateId,
      recipientEmail: email,
      brief,
    };
    this.saving.set(true);
    const editingId = this.editingId();
    const request$ = editingId
      ? this.automationsApi.update(editingId, request)
      : this.automationsApi.create(request);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.showForm.set(false);
        this.notifications.showSuccess(this.i18n.t().common.success);
        this.reload();
      },
      error: () => {
        this.saving.set(false);
        this.notifications.showError(t.saveFailed);
      },
    });
  }

  toggleEnabled(schedule: AutomationSchedule): void {
    this.automationsApi.setEnabled(schedule.id, !schedule.enabled).subscribe({
      next: () => this.reload(),
      error: () => this.notifications.showError(this.i18n.t().automationsPage.saveFailed),
    });
  }

  remove(schedule: AutomationSchedule): void {
    if (!confirm(this.i18n.t().automationsPage.deleteConfirm)) {
      return;
    }
    this.automationsApi.delete(schedule.id).subscribe({
      next: () => this.reload(),
      error: () => {
        this.notifications.showError(this.i18n.t().automationsPage.deleteFailed);
      },
    });
  }

  showHistory(schedule: AutomationSchedule): void {
    this.historyScheduleId.set(schedule.id);
    this.automationsApi.listRuns(schedule.id).subscribe({
      next: runs => this.runs.set(runs),
      error: () => {
        this.runs.set([]);
        this.notifications.showError(this.i18n.t().automationsPage.loadFailed);
      },
    });
  }

  workflowName(id: string): string {
    return this.workflows().find(workflow => workflow.id === id)?.name ?? id;
  }

  formatInstant(value: string | null): string {
    if (!value) {
      return '—';
    }
    try {
      return new Date(value).toLocaleString();
    } catch {
      return value;
    }
  }
}
