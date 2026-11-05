package ir.mmk.cubegen.util;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static Object miniMessageInstance = null;
    private static Method deserializeMethod = null;
    private static Method sendMessageComponentMethod = null;

    static {
        try {
            Class<?> mmClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            Method mmMethod = mmClass.getMethod("miniMessage");
            miniMessageInstance = mmMethod.invoke(null);
            deserializeMethod = mmClass.getMethod("deserialize", String.class);
            Class<?> compClass = Class.forName("net.kyori.adventure.text.Component");
            sendMessageComponentMethod = CommandSender.class.getMethod("sendMessage", compClass);
        } catch (Throwable ignored) {
            miniMessageInstance = null;
        }
    }

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder(message.length() + 32);
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }

        if (miniMessageInstance != null && message.contains("<") && message.contains(">")) {
            try {
                Object comp = deserializeMethod.invoke(miniMessageInstance, message);
                sendMessageComponentMethod.invoke(sender, comp);
                return;
            } catch (Throwable ignored) {}
        }

        sender.sendMessage(colorize(message));
    }
}
