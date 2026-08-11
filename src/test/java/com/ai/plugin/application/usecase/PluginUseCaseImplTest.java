package com.ai.plugin.application.usecase;

import com.ai.plugin.domain.exception.PluginAlreadyInstalledException;
import com.ai.plugin.domain.exception.PluginNotFoundException;
import com.ai.plugin.domain.model.PluginInstallation;
import com.ai.plugin.domain.repository.PluginCatalogRepository;
import com.ai.plugin.domain.repository.PluginInstallationRepository;
import com.ai.plugin.domain.repository.PluginToolGateway;
import com.ai.plugin.domain.vo.PluginDefinition;
import com.ai.plugin.domain.vo.PluginHealthStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PluginUseCaseImpl")
class PluginUseCaseImplTest {

    private static final String OWNER = "owner-1";

    private FakeCatalogRepository catalog;
    private FakeInstallationRepository installations;
    private FakeToolGateway tools;
    private PluginUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        catalog = new FakeCatalogRepository();
        installations = new FakeInstallationRepository();
        tools = new FakeToolGateway();
        useCase = new PluginUseCaseImpl(catalog, installations, tools);
    }

    @Test
    @DisplayName("should auto install builtin when listing installed")
    void shouldAutoInstallBuiltinWhenListingInstalled() {
        List<PluginInstallation> listed = useCase.listInstalled(OWNER);

        assertThat(listed).hasSize(1);
        assertThat(listed.getFirst().isBuiltin()).isTrue();
        assertThat(listed.getFirst().isEnabled()).isTrue();
        assertThat(listed.getFirst().getHealthStatus()).isEqualTo(PluginHealthStatus.HEALTHY);
    }

    @Test
    @DisplayName("should install remote plugin when endpoint provided")
    void shouldInstallRemotePluginWhenEndpointProvided() {
        tools.pingResult = true;

        PluginInstallation installed = useCase.install(
                OWNER,
                "github",
                "https://example.com/mcp",
                "secret",
                null);

        assertThat(installed.getDefinitionId()).isEqualTo("github");
        assertThat(installed.getEndpoint()).isEqualTo("https://example.com/mcp");
        assertThat(installed.getAuthToken()).isEqualTo("secret");
        assertThat(installed.getHealthStatus()).isEqualTo(PluginHealthStatus.HEALTHY);
    }

    @Test
    @DisplayName("should throw when remote plugin already installed")
    void shouldThrowWhenRemotePluginAlreadyInstalled() {
        useCase.install(OWNER, "github", "https://example.com/mcp", null, null);

        assertThatThrownBy(() -> useCase.install(OWNER, "github", "https://other/mcp", null, null))
                .isInstanceOf(PluginAlreadyInstalledException.class);
    }

    @Test
    @DisplayName("should disable builtin instead of deleting when uninstall")
    void shouldDisableBuiltinInsteadOfDeletingWhenUninstall() {
        PluginInstallation builtin = useCase.ensureBuiltinInstalled(OWNER);

        useCase.uninstall(OWNER, builtin.getId().value());

        PluginInstallation stored = installations.findByIdAndOwnerKey(builtin.getId().value(), OWNER).orElseThrow();
        assertThat(stored.isEnabled()).isFalse();
        assertThat(installations.findAllByOwnerKey(OWNER)).hasSize(1);
    }

    @Test
    @DisplayName("should throw when catalog definition missing")
    void shouldThrowWhenCatalogDefinitionMissing() {
        assertThatThrownBy(() -> useCase.install(OWNER, "missing", "https://x", null, null))
                .isInstanceOf(PluginNotFoundException.class);
    }

    private static final class FakeCatalogRepository implements PluginCatalogRepository {
        private final Map<String, PluginDefinition> byId = new LinkedHashMap<>();

        FakeCatalogRepository() {
            seed(new PluginDefinition(
                    "explore-ai",
                    "Explore AI",
                    "Built-in tools",
                    "featured",
                    "explore-ai",
                    true,
                    true,
                    ""));
            seed(new PluginDefinition(
                    "github",
                    "GitHub",
                    "Repos and issues",
                    "developer",
                    "github",
                    true,
                    false,
                    "https://docs.github.com"));
            seed(new PluginDefinition(
                    "custom",
                    "Custom Plugin",
                    "Bring your own endpoint",
                    "developer",
                    "custom",
                    false,
                    false,
                    ""));
        }

        private void seed(PluginDefinition definition) {
            byId.put(definition.id(), definition);
        }

        @Override
        public List<PluginDefinition> findAll() {
            return List.copyOf(byId.values());
        }

        @Override
        public Optional<PluginDefinition> findById(String id) {
            return Optional.ofNullable(byId.get(id));
        }
    }

    private static final class FakeInstallationRepository implements PluginInstallationRepository {
        private final Map<String, PluginInstallation> byId = new ConcurrentHashMap<>();

        @Override
        public List<PluginInstallation> findAllByOwnerKey(String ownerKey) {
            return byId.values().stream()
                    .filter(row -> row.getOwnerKey().equals(ownerKey))
                    .toList();
        }

        @Override
        public Optional<PluginInstallation> findByIdAndOwnerKey(String id, String ownerKey) {
            return Optional.ofNullable(byId.get(id))
                    .filter(row -> row.getOwnerKey().equals(ownerKey));
        }

        @Override
        public Optional<PluginInstallation> findByOwnerKeyAndDefinitionId(String ownerKey, String definitionId) {
            return byId.values().stream()
                    .filter(row -> row.getOwnerKey().equals(ownerKey))
                    .filter(row -> row.getDefinitionId().equals(definitionId))
                    .findFirst();
        }

        @Override
        public PluginInstallation save(PluginInstallation installation) {
            byId.put(installation.getId().value(), installation);
            return installation;
        }

        @Override
        public void deleteByIdAndOwnerKey(String id, String ownerKey) {
            findByIdAndOwnerKey(id, ownerKey).ifPresent(row -> byId.remove(row.getId().value()));
        }
    }

    private static final class FakeToolGateway implements PluginToolGateway {
        boolean pingResult = false;

        @Override
        public List<ToolCallback> resolveEnabledToolCallbacks(String ownerKey) {
            return List.of();
        }

        @Override
        public List<String> listRemoteToolNames(String endpoint, String authToken) {
            return List.of("remote_tool");
        }

        @Override
        public boolean pingRemote(String endpoint, String authToken) {
            return pingResult;
        }
    }
}
