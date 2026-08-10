package net.nexarys.alielevator.objects;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import lombok.Getter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Getter
public final class Config {

    private final JavaPlugin plugin;
    private final Path file;

    private final String resourcePath;

    private final Yaml yaml;

    private Map<String, Object> root = new LinkedHashMap<>();

    public Config(@Nonnull JavaPlugin plugin, @Nonnull Path file, @Nonnull String resourcePath) {
        this.plugin = plugin;
        this.file = file;
        this.resourcePath = normalize(resourcePath);
        this.yaml = createYaml();
        load();
    }

    public void load() {
        try {
            Files.createDirectories(file.getParent());

            if (!Files.exists(file)) {
                copyDefaultIfPresent();
                if (!Files.exists(file)) {
                    root = new LinkedHashMap<>();
                    save();
                }
            }

            Map<String, Object> externalData = readYamlFile(file);
            Map<String, Object> internalData = readInternalDefault();

            boolean changed = updateDefaults(externalData, internalData);
            if (changed) saveConfig(externalData);

            root = externalData;
        } catch (Throwable t) {
            root = new LinkedHashMap<>();
        }
    }

    public void reload() {
        load();
    }

    public void save() {
        saveConfig(root);
    }

    public String getString(String path, String def) {
        Object v = get(path);
        if (v == null) return def;
        if (v instanceof String s) return s;
        return String.valueOf(v);
    }

    public int getInt(String path, int def) {
        Object v = get(path);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Throwable ignored) {
            }
        }
        return def;
    }

    public double getDouble(String path, double def) {
        Object v = get(path);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (Throwable ignored) {
            }
        }
        return def;
    }

    public boolean getBoolean(String path, boolean def) {
        Object v = get(path);
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        if (v instanceof String s) {
            String t = s.trim().toLowerCase(Locale.ROOT);
            if (t.equals("true") || t.equals("yes") || t.equals("on") || t.equals("1")) return true;
            if (t.equals("false") || t.equals("no") || t.equals("off") || t.equals("0")) return false;
        }
        return def;
    }

    public List<String> getStringList(String path) {
        Object v = get(path);
        if (v == null) return List.of();

        if (v instanceof List<?> list) {
            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) {
                if (o != null) out.add(String.valueOf(o));
            }
            return out;
        }

        return List.of(String.valueOf(v));
    }

    public Map<String, Object> getSection(String path) {
        Object v = get(path);
        if (v instanceof Map<?, ?> m) return castToStringMap(m);
        return new LinkedHashMap<>();
    }

    public boolean contains(String path) {
        return get(path) != null;
    }

    public Object get(String path) {
        return getPath(root, path);
    }

    public void set(String path, Object value) {
        setPath(root, path, value);
    }

    public void remove(String path) {
        removePath(root, path);
    }

    public Map<String, Object> asMap() {
        return root;
    }

    public Map<String, Object> getToMap() {
        return root;
    }

    public Map<String, Object> getMap(String path, Map<String, Object> def) {
        Object v = get(path);
        if (v instanceof Map<?, ?> m) return castToStringMap(m);
        return def;
    }

    public Set<String> getKeys(String path) {
        Object v = get(path);
        if (v instanceof Map<?, ?> m) {
            Set<String> out = new LinkedHashSet<>();
            for (Object k : m.keySet()) if (k != null) out.add(k.toString());
            return out;
        }
        return Collections.emptySet();
    }

    private void copyDefaultIfPresent() {
        try (InputStream in = openDefaultStream()) {
            if (in == null) return;
            Files.createDirectories(file.getParent());
            Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable ignored) {
        }
    }

    private Map<String, Object> readInternalDefault() {
        try (InputStream in = openDefaultStream()) {
            if (in == null) return new LinkedHashMap<>();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                Object o = yaml.load(r);
                if (o instanceof Map<?, ?> m) return castToStringMap(m);
                return new LinkedHashMap<>();
            }
        } catch (Throwable ignored) {
            return new LinkedHashMap<>();
        }
    }

    private InputStream openDefaultStream() {
        ClassLoader cl = plugin.getClass().getClassLoader();

        InputStream in = cl.getResourceAsStream(resourcePath);
        if (in != null) return in;

        String fallback = file.getFileName().toString();
        return cl.getResourceAsStream(fallback);
    }

    private Map<String, Object> readYamlFile(Path path) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Object o = yaml.load(r);
            if (o instanceof Map<?, ?> m) return castToStringMap(m);
            return new LinkedHashMap<>();
        }
    }

    private void saveConfig(Map<String, Object> data) {
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                yaml.dump(data, w);
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo guardar config skibidi sad: " + file, e);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean updateDefaults(Map<String, Object> current, Map<String, Object> defaults) {
        boolean changed = false;
        if (defaults == null) return false;

        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            String k = e.getKey();
            Object defVal = e.getValue();

            if (!current.containsKey(k)) {
                current.put(k, deepCopy(defVal));
                changed = true;
                continue;
            }

            Object curVal = current.get(k);
            if (curVal instanceof Map<?, ?> cm && defVal instanceof Map<?, ?> dm) {
                changed |= updateDefaults((Map<String, Object>) cm, (Map<String, Object>) dm);
            }
        }

        return changed;
    }

    private static Object deepCopy(Object v) {
        if (v instanceof Map<?, ?> m) return castToStringMap(m);
        if (v instanceof List<?> l) return deepList(l);
        return v;
    }

    private static Map<String, Object> castToStringMap(Map<?, ?> in) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : in.entrySet()) {
            String k = String.valueOf(e.getKey());
            Object v = e.getValue();
            if (v instanceof Map<?, ?> m) out.put(k, castToStringMap(m));
            else if (v instanceof List<?> l) out.put(k, deepList(l));
            else out.put(k, v);
        }
        return out;
    }

    private static List<Object> deepList(List<?> in) {
        List<Object> out = new ArrayList<>(in.size());
        for (Object v : in) {
            if (v instanceof Map<?, ?> m) out.add(castToStringMap(m));
            else if (v instanceof List<?> l) out.add(deepList(l));
            else out.add(v);
        }
        return out;
    }

    private static Object getPath(Map<String, Object> root, String path) {
        if (path == null || path.isBlank()) return root;
        String[] parts = path.split("\\.");
        Object cur = root;
        for (String p : parts) {
            if (!(cur instanceof Map<?, ?> m)) return null;
            cur = m.get(p);
            if (cur == null) return null;
        }
        return cur;
    }

    @SuppressWarnings("unchecked")
    private static void setPath(Map<String, Object> root, String path, Object value) {
        if (path == null || path.isBlank()) return;

        String[] parts = path.split("\\.");
        Map<String, Object> cur = root;

        for (int i = 0; i < parts.length - 1; i++) {
            String p = parts[i];
            Object next = cur.get(p);
            if (next instanceof Map<?, ?>) {
                cur = (Map<String, Object>) next;
            } else {
                Map<String, Object> created = new LinkedHashMap<>();
                cur.put(p, created);
                cur = created;
            }
        }

        cur.put(parts[parts.length - 1], value);
    }

    @SuppressWarnings("unchecked")
    private static void removePath(Map<String, Object> root, String path) {
        if (path == null || path.isBlank()) return;

        String[] parts = path.split("\\.");
        Map<String, Object> cur = root;

        for (int i = 0; i < parts.length - 1; i++) {
            Object next = cur.get(parts[i]);
            if (!(next instanceof Map<?, ?>)) return;
            cur = (Map<String, Object>) next;
        }

        cur.remove(parts[parts.length - 1]);
    }

    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setAllowUnicode(true);
        return new Yaml(options);
    }

    private static String normalize(String p) {
        String s = p.replace('\\', '/').replaceAll("^/+", "");
        while (s.startsWith("./")) s = s.substring(2);
        return s;
    }
}
