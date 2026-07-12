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

    let timeoutId: ReturnType<typeof setTimeout>;
    const timeoutPromise = new Promise<never>((_, reject) => {
      timeoutId = setTimeout(() => reject(new Error('LaunchDarkly init timeout')), 5000);
    });

    try {
      await Promise.race([client.waitForInitialization(), timeoutPromise]);
    } catch {
      this.flags.set(environment.featureFlagFallback);
      return;
    } finally {
      clearTimeout(timeoutId!);
    }

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
