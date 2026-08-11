import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { I18nService } from '../core/i18n';
import { ZardButtonComponent } from '../shared/components/button';
import type { PluginDefinition, PluginInstallation } from './plugins.model';
import { PluginsService } from './plugins.service';

@Component({
  selector: 'app-plugins-page',
  imports: [FormsModule, ZardButtonComponent],
  templateUrl: './plugins.page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    class: 'block min-h-0 flex-1 overflow-y-auto px-4 py-6 md:px-6',
  },
})
export class PluginsPageComponent implements OnInit {
  protected readonly i18n = inject(I18nService);
  private readonly pluginsApi = inject(PluginsService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly catalog = signal<PluginDefinition[]>([]);
  readonly installed = signal<PluginInstallation[]>([]);
  readonly installingId = signal<string | null>(null);
  readonly menuOpenId = signal<string | null>(null);
  readonly installFormOpen = signal(false);
  readonly installDefinition = signal<PluginDefinition | null>(null);
  readonly installEndpoint = signal('');
  readonly installAuthToken = signal('');
  readonly installCustomName = signal('');
  readonly toolNames = signal<string[]>([]);
  readonly toolsForId = signal<string | null>(null);

  readonly featured = computed(() => {
    return this.catalog().filter(item => item.featured && !item.builtin);
  });

  readonly byCategory = computed(() => {
    const groups = new Map<string, PluginDefinition[]>();
    for (const item of this.catalog()) {
      if (item.featured || item.builtin) {
        continue;
      }
      const list = groups.get(item.category) ?? [];
      list.push(item);
      groups.set(item.category, list);
    }
    return [...groups.entries()];
  });

  readonly installedDefinitionIds = computed(() => {
    return new Set(
      this.installed()
        .filter(row => row.definitionId !== 'custom')
        .map(row => row.definitionId),
    );
  });

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.pluginsApi.listCatalog().subscribe({
      next: (catalog) => {
        this.catalog.set(catalog);
        this.pluginsApi.listInstalled().subscribe({
          next: (installed) => {
            this.installed.set(installed);
            this.loading.set(false);
          },
          error: () => {
            this.error.set(this.i18n.t().pluginsPage.errors.loadFailed);
            this.loading.set(false);
          },
        });
      },
      error: () => {
        this.error.set(this.i18n.t().pluginsPage.errors.loadFailed);
        this.loading.set(false);
      },
    });
  }

  isInstalled(definition: PluginDefinition): boolean {
    return this.installedDefinitionIds().has(definition.id);
  }

  startInstall(definition: PluginDefinition): void {
    this.menuOpenId.set(null);
    if (!definition.requiresEndpoint) {
      this.installingId.set(definition.id);
      this.pluginsApi.install({ definitionId: definition.id }).subscribe({
        next: () => {
          this.installingId.set(null);
          this.reload();
        },
        error: () => {
          this.installingId.set(null);
          this.error.set(this.i18n.t().pluginsPage.errors.installFailed);
        },
      });
      return;
    }
    this.installDefinition.set(definition);
    this.installEndpoint.set('');
    this.installAuthToken.set('');
    this.installCustomName.set(definition.id === 'custom' ? '' : definition.name);
    this.installFormOpen.set(true);
  }

  cancelInstallForm(): void {
    this.installFormOpen.set(false);
    this.installDefinition.set(null);
  }

  confirmInstall(): void {
    const definition = this.installDefinition();
    if (!definition) {
      return;
    }
    const endpoint = this.installEndpoint().trim();
    if (!endpoint) {
      this.error.set(this.i18n.t().pluginsPage.errors.endpointRequired);
      return;
    }
    this.installingId.set(definition.id);
    this.pluginsApi.install({
      definitionId: definition.id,
      endpoint,
      authToken: this.installAuthToken().trim() || undefined,
      customName: this.installCustomName().trim() || undefined,
    }).subscribe({
      next: () => {
        this.installingId.set(null);
        this.cancelInstallForm();
        this.reload();
      },
      error: () => {
        this.installingId.set(null);
        this.error.set(this.i18n.t().pluginsPage.errors.installFailed);
      },
    });
  }

  toggleMenu(id: string): void {
    this.menuOpenId.update(current => (current === id ? null : id));
  }

  setEnabled(installation: PluginInstallation, enabled: boolean): void {
    this.menuOpenId.set(null);
    this.pluginsApi.setEnabled(installation.id, enabled).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().pluginsPage.errors.updateFailed),
    });
  }

  remove(installation: PluginInstallation): void {
    this.menuOpenId.set(null);
    this.pluginsApi.uninstall(installation.id).subscribe({
      next: () => this.reload(),
      error: () => this.error.set(this.i18n.t().pluginsPage.errors.removeFailed),
    });
  }

  showTools(installation: PluginInstallation): void {
    this.menuOpenId.set(null);
    this.toolsForId.set(installation.id);
    this.pluginsApi.listTools(installation.id).subscribe({
      next: tools => this.toolNames.set(tools),
      error: () => {
        this.toolNames.set([]);
        this.error.set(this.i18n.t().pluginsPage.errors.toolsFailed);
      },
    });
  }

  categoryLabel(category: string): string {
    const labels = this.i18n.t().pluginsPage.categories;
    return (labels as Record<string, string>)[category] ?? category;
  }

  iconUrl(iconKey: string): string {
    const known = new Set([
      'explore-ai',
      'github',
      'notion',
      'linear',
      'slack',
      'google-drive',
      'gmail',
      'figma',
      'custom',
    ]);
    const key = known.has(iconKey) ? iconKey : 'custom';
    return `assets/plugins/${key}.svg`;
  }
}
