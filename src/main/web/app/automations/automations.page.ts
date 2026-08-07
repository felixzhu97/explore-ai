import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NzDatePickerModule } from 'ng-zorro-antd/date-picker';
import { NotificationService } from '../core/notification.service';
import { I18nService } from '../core/i18n';
import { PipelinesService } from '../pipelines/pipelines.service';
import type { SavedWorkflowTemplate } from '../pipelines/pipelines.model';
import { ZardButtonComponent } from '../shared/components/button';
import { AutomationsService } from './automations.service';
import {
  cronForPreset,
  defaultRunAtDate,
  presetFromSchedule,
  type AutomationRun,
  type AutomationSchedule,
  type AutomationScheduleWriteRequest,
  type FrequencyPreset,
} from './automations.model';

@Component({
  selector: 'app-automations-page',
  imports: [FormsModule, ZardButtonComponent, NzDatePickerModule],
  templateUrl: './automations.page.html',
  styleUrl: './automations.page.css',
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
  readonly formRunAt = signal<Date | null>(defaultRunAtDate());

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
    const first = this.enabledWorkflows()[0];
    this.formWorkflowId.set(first?.id ?? '');
    this.formBrief.set(this.defaultBriefForWorkflow(first));
    this.formPreset.set('daily');
    this.formRunAt.set(defaultRunAtDate());
    this.showForm.set(true);
  }

  onWorkflowChange(workflowId: string): void {
    this.formWorkflowId.set(workflowId);
    const workflow = this.enabledWorkflows().find(item => item.id === workflowId);
    if (!workflow) {
      return;
    }
    if (!this.formBrief().trim() || this.isGenericPlaceholder(this.formBrief())) {
      this.formBrief.set(this.defaultBriefForWorkflow(workflow));
    }
  }

  startEdit(schedule: AutomationSchedule): void {
    this.editingId.set(schedule.id);
    this.formName.set(schedule.name);
    this.formEmail.set(schedule.recipientEmail);
    this.formTimezone.set(schedule.timezone);
    this.formWorkflowId.set(schedule.workflowTemplateId);
    const workflowId = schedule.workflowTemplateId;
    const workflow = this.enabledWorkflows().find(item => item.id === workflowId)
      ?? this.workflows().find(item => item.id === workflowId);
    if (this.isGenericPlaceholder(schedule.brief)) {
      this.formBrief.set(this.defaultBriefForWorkflow(workflow));
    } else {
      this.formBrief.set(schedule.brief);
    }
    this.formPreset.set(presetFromSchedule(schedule));
    if (schedule.scheduleKind === 'ONCE') {
      if (this.isOnceCompleted(schedule)) {
        this.formRunAt.set(defaultRunAtDate());
      } else {
        const source = schedule.runAt ?? schedule.nextRunAt;
        this.formRunAt.set(source ? new Date(source) : defaultRunAtDate());
      }
    } else {
      this.formRunAt.set(defaultRunAtDate());
    }
    this.showForm.set(true);
  }

  cancelForm(): void {
    this.showForm.set(false);
    this.editingId.set(null);
  }

  onPresetChange(preset: FrequencyPreset): void {
    this.formPreset.set(preset);
    if (preset === 'custom' && !this.formRunAt()) {
      this.formRunAt.set(defaultRunAtDate());
    }
  }

  disabledDate = (current: Date): boolean => {
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    return current.getTime() < start.getTime();
  };

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
    const preset = this.formPreset();
    let request: AutomationScheduleWriteRequest;
    if (preset === 'custom') {
      const runAt = this.formRunAt();
      if (!runAt || Number.isNaN(runAt.getTime())) {
        this.notifications.showError(t.runAtRequired);
        return;
      }
      if (runAt.getTime() <= Date.now()) {
        this.notifications.showError(t.runAtPast);
        return;
      }
      request = {
        name,
        scheduleKind: 'ONCE',
        runAt: runAt.toISOString(),
        timezone: this.formTimezone().trim() || 'UTC',
        workflowTemplateId,
        recipientEmail: email,
        brief,
      };
    } else {
      request = {
        name,
        scheduleKind: 'CRON',
        cronExpression: cronForPreset(preset),
        timezone: this.formTimezone().trim() || 'UTC',
        workflowTemplateId,
        recipientEmail: email,
        brief,
      };
    }
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
    if (this.isOnceCompleted(schedule) && !schedule.enabled) {
      this.notifications.showWarning(this.i18n.t().automationsPage.onceCompletedHint);
      this.startEdit(schedule);
      return;
    }
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

  private defaultBriefForWorkflow(workflow: SavedWorkflowTemplate | undefined): string {
    if (!workflow) {
      return '';
    }
    const topic = workflow.shortTopic?.trim();
    if (topic) {
      return topic;
    }
    return workflow.briefPrompt?.trim() ?? '';
  }

  private isGenericPlaceholder(brief: string): boolean {
    return brief.trim().toLowerCase()
      === 'follow the configured agent pipeline for the user task.';
  }

  /** One-shot schedules auto-disable; nextRunAt becomes a far-future sentinel. */
  isOnceCompleted(schedule: AutomationSchedule): boolean {
    if (schedule.scheduleKind !== 'ONCE') {
      return false;
    }
    if (schedule.lastRunAt && !schedule.enabled) {
      return true;
    }
    const next = Date.parse(schedule.nextRunAt);
    return Number.isFinite(next) && next >= Date.parse('9999-01-01T00:00:00Z');
  }

  statusLabel(schedule: AutomationSchedule): string {
    const t = this.i18n.t().automationsPage;
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
      return `${this.i18n.t().automationsPage.frequencyCustom}: ${this.formatInstant(when)}`;
    }
    return schedule.cronExpression ?? '—';
  }

  formatNextRun(schedule: AutomationSchedule): string {
    if (this.isOnceCompleted(schedule)) {
      return this.i18n.t().automationsPage.nextRunNone;
    }
    return this.formatInstant(schedule.nextRunAt);
  }

  formatInstant(value: string | null): string {
    if (!value) {
      return '—';
    }
    try {
      const ms = Date.parse(value);
      if (Number.isFinite(ms) && ms >= Date.parse('9999-01-01T00:00:00Z')) {
        return this.i18n.t().automationsPage.nextRunNone;
      }
      return new Date(value).toLocaleString();
    } catch {
      return value;
    }
  }
}
