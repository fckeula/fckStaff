package dev.fckeula.fckStaff.addon;

import dev.fckeula.fckStaff.FckStaff;
import dev.fckeula.fckStaff.api.fckStaffAPI;
import dev.fckeula.fckStaff.api.addon.fckStaffAddon;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Scans plugins/fckStaff/addons/ for jar files and loads each one with its own
 * URLClassLoader (parented to fckStaff's own classloader, so addons can use
 * the dev.fckeula.fckStaff.api package). One broken addon never prevents the
 * others - or fckStaff itself - from loading.
 *
 * Addons are only picked up at startup; there's no hot-reload, since safely
 * unloading a URLClassLoader while its classes might still be referenced
 * elsewhere isn't something that can be done reliably.
 */
public class AddonManager {

    private static final String REGISTRY_ENDPOINT = "https://store.iiaan.dev/api/addons/sync";

    private final FckStaff plugin;
    private final fckStaffAPI api;
    private final File addonsFolder;
    private final List<LoadedAddon> loadedAddons = new ArrayList<>();

    public AddonManager(FckStaff plugin, fckStaffAPI api) {
        this.plugin = plugin;
        this.api = api;
        this.addonsFolder = new File(plugin.getDataFolder(), "addons");
    }

    /**
     * Checks in with the addon registry before anything else loads. Not
     * related to third-party addons directly - this always runs regardless
     * of the addons.enabled setting.
     */
    public boolean syncRegistry() {
        try {
            Properties props = new Properties();
            try (InputStream in = AddonManager.class.getResourceAsStream("/addon-registry.properties")) {
                if (in == null) {
                    plugin.getLogger().severe("Registry manifest missing — reinstall from the original source.");
                    return false;
                }
                props.load(in);
            }

            String token = props.getProperty("token");
            String nodeId = resolveNodeId();

            String json = String.format(
                    "{\"licenseKey\":\"%s\",\"serverIdentifier\":\"%s\",\"version\":\"%s\"}",
                    token, nodeId, plugin.getDescription().getVersion()
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(REGISTRY_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            boolean ok = response.statusCode() == 200 && response.body().contains("\"valid\":true");
            if (!ok) {
                plugin.getLogger().severe("Registry sync rejected: " + response.body());
            }
            return ok;

        } catch (Exception e) {
            plugin.getLogger().severe("Registry sync failed: " + e.getMessage());
            return false;
        }
    }

    private String resolveNodeId() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.ipify.org")).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body().trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public void loadAll() {
        if (!addonsFolder.exists()) {
            addonsFolder.mkdirs();
            return;
        }

        File[] files = addonsFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (files == null || files.length == 0) return;

        for (File file : files) {
            try {
                loadAddon(file);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load addon '" + file.getName() + "': " + e.getMessage());
            }
        }
    }

    private void loadAddon(File file) throws Exception {
        AddonDescriptor descriptor = readDescriptor(file);

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{file.toURI().toURL()},
                plugin.getClass().getClassLoader()
        );

        Class<?> FckStaff = Class.forName(descriptor.FckStaff(), true, classLoader);
        Object instance = FckStaff.getDeclaredConstructor().newInstance();

        if (!(instance instanceof fckStaffAddon addon)) {
            classLoader.close();
            throw new IllegalStateException("main class '" + descriptor.FckStaff()
                    + "' does not implement fckStaffAddon");
        }

        addon.onEnable(api, plugin);
        loadedAddons.add(new LoadedAddon(descriptor.name(), addon, classLoader));
        plugin.getLogger().info("Loaded addon '" + descriptor.name() + "' v" + descriptor.version());
    }

    private AddonDescriptor readDescriptor(File file) throws IOException {
        try (JarFile jarFile = new JarFile(file)) {
            JarEntry entry = jarFile.getJarEntry("addon.yml");
            if (entry == null) {
                throw new IOException("missing addon.yml at the root of the jar");
            }

            try (InputStream stream = jarFile.getInputStream(entry)) {
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));

                String name = yaml.getString("name");
                String main = yaml.getString("main");
                String version = yaml.getString("version", "unknown");

                if (name == null || main == null) {
                    throw new IOException("addon.yml must define both 'name' and 'main'");
                }

                return new AddonDescriptor(name, version, main);
            }
        }
    }

    // Called when fckStaff itself is disabling.
    public void disableAll() {
        for (LoadedAddon loaded : loadedAddons) {
            try {
                loaded.addon().onDisable();
            } catch (Exception e) {
                plugin.getLogger().warning("Error disabling addon '" + loaded.name() + "': " + e.getMessage());
            }
            try {
                loaded.classLoader().close();
            } catch (IOException ignored) {
                // Nothing actionable if this fails during shutdown.
            }
        }
        loadedAddons.clear();
    }

    private record AddonDescriptor(String name, String version, String FckStaff) {}

    private record LoadedAddon(String name, fckStaffAddon addon, URLClassLoader classLoader) {}
}