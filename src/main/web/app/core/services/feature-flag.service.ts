import { Injectable, signal } from '@angular/core';
import { createClient, type LDClient } from '@launchdarkly/js-client-sdk';
import { environment } from '@env/environment';
import {
  FEATURE_FLAG_KEYS,
  type FeatureFlagKey,
  MODULE_FLAG_FALLBACK,
} from '@core/config/feature-flag-keys';

const FLAG_KEYS = Object.values(FEATURE_FLAG_KEYS);

@Injectable({ providedIn: 'root' })
export class FeatureFlagService {
  private client: LDClient | null = null;
  private readonly flags = signal<Record<FeatureFlagKey, boolean>>(MODULE_FLAG_FALLBACK);

  async initialize(): Promise<void> {
    const clientSideId = environment.launchDarklyClientSideId;
    if (!clientSideId) {
      this.flags.set(environment.featureFlagFallback);
      return;
    }

    const client = createClient(clientSideId, {
      kind: 'user',
      key: 'explore-ai-browser',
      anonymous: true,
    });

    client.on('change', () => this.syncFlags(client));
    client.start();

    try {
      await client.waitForInitialization({ timeout: 5 });
    } catch {
      this.flags.set(environment.featureFlagFallback);
      return;
    }

    this.client = client;
    this.syncFlags(client);
  }

  isEnabled(key: FeatureFlagKey): boolean {
    return this.flags()[key] ?? environment.featureFlagFallback[key] ?? false;
  }

  private syncFlags(client: LDClient): void {
    const values = {} as Record<FeatureFlagKey, boolean>;
    for (const key of FLAG_KEYS) {
      values[key] = client.boolVariation(key, environment.featureFlagFallback[key]);
    }
    this.flags.set(values);
  }
}
