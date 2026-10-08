package de.autobroadcast;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class AutoBroadcast extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private BukkitTask task;
    private List<String> messages = new ArrayList<>();
    private String prefix = "";
    private boolean randomOrder;
    private int minPlayers;
    private int index = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        start();
    }

    @Override
    public void onDisable() {
        stop();
    }

    private void start() {
        reloadConfig();
        stop();

        messages = new ArrayList<>(getConfig().getStringList("messages"));
        prefix = getConfig().getString("prefix", "");
        randomOrder = getConfig().getBoolean("random-order", false);
        minPlayers = Math.max(0, getConfig().getInt("min-players", 1));
        index = 0;

        double minutes = getConfig().getDouble("interval-minutes", 5.0);
        if (minutes <= 0) {
            getLogger().warning("interval-minutes muss größer als 0 sein - verwende 5.");
            minutes = 5.0;
        }
        long ticks = Math.max(1L, Math.round(minutes * 60.0 * 20.0));

        if (messages.isEmpty()) {
            getLogger().warning("Keine Nachrichten in der config.yml gefunden - es wird nichts gesendet.");
            return;
        }

        task = Bukkit.getScheduler().runTaskTimer(this, this::broadcastNext, ticks, ticks);
        getLogger().info("AutoBroadcast gestartet: " + messages.size()
                + " Nachricht(en), Intervall " + minutes + " Minute(n).");
    }

    private void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void broadcastNext() {
        if (messages.isEmpty()) return;
        if (Bukkit.getOnlinePlayers().size() < minPlayers) return;

        String raw;
        if (randomOrder) {
            raw = messages.get(ThreadLocalRandom.current().nextInt(messages.size()));
        } else {
            if (index >= messages.size()) index = 0;
            raw = messages.get(index++);
        }

        for (String line : raw.split("\\\\n")) {
            Component component = MM.deserialize(prefix + line);
            Bukkit.broadcast(component);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            start();
            sender.sendMessage(MM.deserialize("<green>AutoBroadcast wurde neu geladen.</green>"));
            return true;
        }
        sender.sendMessage(MM.deserialize("<yellow>Benutzung: /" + label + " reload</yellow>"));
        return true;
    }
}
