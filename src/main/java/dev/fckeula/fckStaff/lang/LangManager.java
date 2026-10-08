package dev.fckeula.fckStaff.lang;

import dev.fckeula.fckStaff.FckStaff;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LangManager {

    private final FckStaff plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration messages;

    // Caches parsed Components for calls with no placeholders (e.g. static icons like
    // ✔/✘), since MiniMessage.deserialize() re-parses the whole string every call and
    // that cost adds up fast for messages sent on a repeating timer. Placeholder-based
    // calls aren't cached here since their result depends on the arguments passed in.
    private final Map<String, Component> componentCache = new HashMap<>();
    private final Map<String, List<Component>> listCache = new HashMap<>();

    public LangManager(FckStaff plugin) {
        this.plugin = plugin;
        load();
    }

    /**
     * Copies the default languages if they don't exist and loads the configured language.
     * Can be called again to reload (e.g. via /fckstaff reload).
     */
    public void load() {
        saveDefaultLang("es_ES.yml");
        saveDefaultLang("en_US.yml");

        String language = plugin.getConfig().getString("language", "es_ES");
        File langFile = new File(plugin.getDataFolder(), "langs/" + language + ".yml");

        if (!langFile.exists()) {
            plugin.getLogger().warning("Language '" + language + "' not found, falling back to es_ES.");
            language = "es_ES";
            langFile = new File(plugin.getDataFolder(), "langs/es_ES.yml");
        }

        this.messages = YamlConfiguration.loadConfiguration(langFile);

        // If the admin only partially edited the file, missing keys fall back to the embedded resource
        try (InputStream defStream = plugin.getResource("langs/" + language + ".yml")) {
            if (defStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8));
                messages.setDefaults(defConfig);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load the embedded default language: " + e.getMessage());
        }

        componentCache.clear();
        listCache.clear();
    }

    private void saveDefaultLang(String fileName) {
        File file = new File(plugin.getDataFolder(), "langs/" + fileName);
        if (!file.exists()) {
            plugin.saveResource("langs/" + fileName, false);
        }
    }

    public Component get(String path, TagResolver... placeholders) {
        if (placeholders.length == 0) {
            return componentCache.computeIfAbsent(path, this::deserialize);
        }
        return deserialize(path, placeholders);
    }

    public List<Component> getList(String path, TagResolver... placeholders) {
        if (placeholders.length == 0) {
            return listCache.computeIfAbsent(path, this::deserializeList);
        }
        return deserializeList(path, placeholders);
    }

    private Component deserialize(String path, TagResolver... placeholders) {
        String raw = messages.getString(path);
        if (raw == null) {
            return Component.text("Missing message: " + path);
        }
        return miniMessage.deserialize(raw, placeholders);
    }

    private List<Component> deserializeList(String path, TagResolver... placeholders) {
        List<String> rawList = messages.getStringList(path);
        List<Component> components = new ArrayList<>();
        for (String raw : rawList) {
            components.add(miniMessage.deserialize(raw, placeholders));
        }
        return components;
    }
}
