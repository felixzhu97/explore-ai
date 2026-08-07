export type ScheduleKind = 'CRON' | 'ONCE';

export interface AutomationSchedule {
  id: string;
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression: string | null;
  runAt: string | null;
  timezone: string;
  enabled: boolean;
  actionType: string;
  workflowTemplateId: string;
  recipientEmail: string;
  brief: string;
  nextRunAt: string;
  lastRunAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AutomationRun {
  id: string;
  scheduleId: string;
  startedAt: string;
  finishedAt: string | null;
  status: string;
  errorMessage: string | null;
  resultExcerpt: string | null;
  emailStatus: string;
}

export interface AutomationScheduleWriteRequest {
  name: string;
  scheduleKind: ScheduleKind;
  cronExpression?: string | null;
  runAt?: string | null;
  timezone: string;
  workflowTemplateId: string;
  recipientEmail: string;
  brief: string;
}

export type FrequencyPreset = 'daily' | 'weekly' | 'custom';

export function cronForPreset(preset: FrequencyPreset): string {
  switch (preset) {
    case 'daily':
      return '0 0 9 * * *';
    case 'weekly':
      return '0 0 9 * * MON';
    case 'custom':
      return '';
  }
}

export function presetFromSchedule(schedule: AutomationSchedule): FrequencyPreset {
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
export function defaultRunAtDate(): Date {
  const date = new Date(Date.now() + 5 * 60 * 1000);
  date.setMilliseconds(0);
  return date;
}
