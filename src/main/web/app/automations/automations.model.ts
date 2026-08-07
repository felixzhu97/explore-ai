export interface AutomationSchedule {
  id: string;
  name: string;
  cronExpression: string;
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
  cronExpression: string;
  timezone: string;
  workflowTemplateId: string;
  recipientEmail: string;
  brief: string;
}

export type FrequencyPreset = 'daily' | 'weekly' | 'custom';

export function cronForPreset(preset: FrequencyPreset, customCron: string): string {
  switch (preset) {
    case 'daily':
      return '0 0 9 * * *';
    case 'weekly':
      return '0 0 9 * * MON';
    case 'custom':
      return customCron.trim();
  }
}

export function presetFromCron(cron: string): FrequencyPreset {
  if (cron === '0 0 9 * * *') {
    return 'daily';
  }
  if (cron === '0 0 9 * * MON') {
    return 'weekly';
  }
  return 'custom';
}
