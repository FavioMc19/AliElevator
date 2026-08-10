package net.nexarys.alielevator.objects;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigService {

    private final JavaPlugin plugin;

    @Getter
    private final Path dataFolder;

    private final Map<String, Config> cache = new ConcurrentHashMap<>();

    public ConfigService(@Nonnull JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = resolveDataFolder(plugin);
    }

    public Config getConfig(@Nonnull String relativePath) {
        String key = normalize(relativePath);
        return cache.computeIfAbsent(key, p -> new Config(plugin, dataFolder.resolve(p), p));
    }

    public File[] getFiles(@Nonnull String relativePath) {
        String key = normalize(relativePath);
        Path resolve = dataFolder.resolve(key);

        if (!Files.exists(resolve) || !Files.isDirectory(resolve)) {
            return new File[0];
        }

        File dir = resolve.toFile();
        File[] files = dir.listFiles();
        return files != null ? files : new File[0];
    }

    public void reloadAll() {
        for (Config cfg : cache.values()) {
            cfg.reload();
        }
    }

    private static String normalize(String p) {
        String s = p.replace('\\', '/').replaceAll("^/+", "");
        while (s.startsWith("./")) s = s.substring(2);
        return s;
    }

    private static Path resolveDataFolder(JavaPlugin plugin) {
        Path mods = tryResolveModsFolder(plugin);
        return Objects.requireNonNullElseGet(mods, () -> Path.of("mods")).resolve(plugin.getManifest().getName());
    }

    private static Path tryResolveModsFolder(JavaPlugin plugin) {
        try {
            var uri = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            Path jarPath = java.nio.file.Paths.get(uri);
            return Files.isDirectory(jarPath) ? jarPath : jarPath.getParent();
        } catch (Throwable ignored) {
            return null;
        }
    }
}
