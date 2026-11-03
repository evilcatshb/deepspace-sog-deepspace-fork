package com.deepspace.compat.superchat;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Live BFCR adapter for BFCR 1.0.3 (bfcrmod-1.20.1-3.0.0.jar).
 * ConfigValue.get() is authoritative; TOML is never parsed here.
 */
public final class SuperChatBfcrAdapter {
    private static final Logger LOGGER = LogManager.getLogger("deepspace-superchat-compat");
    private static final AtomicBoolean READ_WARNED = new AtomicBoolean();
    private static final AtomicBoolean FORMAT_WARNED = new AtomicBoolean();
    private SuperChatBfcrAdapter() {}

    public static Component wrap(Component original) {
        if (!ModList.get().isLoaded("bfcrmod")) return original;
        try {
            State state = readLiveState();
            if (!state.enabled) return original;
            Compatibility compatibility = analyze(state.chatFormat);
            if (compatibility == null) {
                warnFormat(state.chatFormat);
                return original;
            }
            String timestamp = new SimpleDateFormat(state.timestampFormat).format(new Date());
            String outerText = compatibility.outerTemplate.replace("$time", timestamp);
            MutableComponent outer = formatWithBfcr(outerText);
            outer.append(original);
            return outer;
        } catch (Throwable t) {
            warnRead(t);
            return original;
        }
    }

    private static State readLiveState() throws ReflectiveOperationException {
        Class<?> handler = Class.forName("com.jeremiahbl.bfcrmod.config.ConfigHandler", false,
                SuperChatBfcrAdapter.class.getClassLoader());
        Object config = handler.getField("config").get(null);
        Object enabled = readConfigValue(config, "enableTimestamp");
        Object timestamp = readConfigValue(config, "timestampFormat");
        Object format = readConfigValue(config, "chatMessageFormat");
        Object color = readConfigValue(config, "chatMessageColor");
        if (!(enabled instanceof Boolean) || !(timestamp instanceof String) ||
                !(format instanceof String) || !(color instanceof String) || ((String) timestamp).isEmpty()) {
            throw new IllegalStateException("BFCR live config returned invalid types");
        }
        return new State((Boolean) enabled, (String) timestamp, (String) format, (String) color);
    }

    private static Object readConfigValue(Object config, String fieldName) throws ReflectiveOperationException {
        Field field = config.getClass().getField(fieldName);
        Object configValue = field.get(config);
        Method get = configValue.getClass().getMethod("get");
        return get.invoke(configValue);
    }

    private static MutableComponent formatWithBfcr(String text) throws ReflectiveOperationException {
        Class<?> formatter = Class.forName("com.jeremiahbl.bfcrmod.TextFormatter", false,
                SuperChatBfcrAdapter.class.getClassLoader());
        Method method = formatter.getMethod("stringToFormattedText", String.class);
        Object result = method.invoke(null, text);
        if (!(result instanceof MutableComponent)) throw new IllegalStateException("BFCR TextFormatter returned unexpected type");
        return (MutableComponent) result;
    }
    private static Compatibility analyze(String format) {
        if (format == null) return null;
        if (count(format, "$time") != 1 || count(format, "$name") != 1 || count(format, "$msg") != 1) return null;
        int time = format.indexOf("$time");
        int name = format.indexOf("$name");
        int msg = format.indexOf("$msg");
        if (time < 0 || name < 0 || msg < 0 || !(time < name && name < msg)) return null;
        if (msg + 4 != format.length()) return null;
        if (!": ".equals(format.substring(name + 5, msg))) return null;
        String outer = format.substring(0, time) + "$time" + format.substring(time + 5, name);
        return new Compatibility(outer);
    }

    private static int count(String value, String token) {
        int count = 0, at = 0;
        while ((at = value.indexOf(token, at)) >= 0) { count++; at += token.length(); }
        return count;
    }

    private static void warnRead(Throwable t) {
        if (READ_WARNED.compareAndSet(false, true)) LOGGER.warn("[SuperChatCompat] BFCR live configuration could not be read safely; preserving Super Chat component unchanged.", t);
    }

    private static void warnFormat(String format) {
        if (FORMAT_WARNED.compareAndSet(false, true)) LOGGER.warn("[SuperChatCompat] Unsupported BFCR chatMessageFormat '{}'; preserving Super Chat component unchanged.", format);
    }
    private static final class Compatibility {
        final String outerTemplate;
        Compatibility(String outerTemplate) { this.outerTemplate = outerTemplate; }
    }

    private static final class State {
        final boolean enabled;
        final String timestampFormat;
        final String chatFormat;
        final String color;
        State(boolean enabled, String timestampFormat, String chatFormat, String color) {
            this.enabled = enabled;
            this.timestampFormat = timestampFormat;
            this.chatFormat = chatFormat;
            this.color = color;
        }
    }
}
