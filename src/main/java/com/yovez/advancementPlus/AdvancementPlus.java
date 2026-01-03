package com.yovez.advancementPlus;

import com.yovez.advancementPlus.listeners.AdvancementListener;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementDisplay;
import org.bukkit.advancement.AdvancementDisplayType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class AdvancementPlus extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(new AdvancementListener(this), this);
        getCommand("advancementplus").setExecutor((sender, cmd, label, args) -> {
            if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
                sender.sendMessage("§9AchievementPlus §fCommands:");
                sender.sendMessage("/§f" + label + " help");
                sender.sendMessage("/§7" + label + " reload");
                sender.sendMessage("/§f" + label + " test <advancement/challenge> [player]");
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                sender.sendMessage("§aReloaded AdvancementsPlus config file.");
                return true;
            }
            if (args[0].equalsIgnoreCase("test")) {
                if (args.length < 2 || args.length > 3) {
                    sender.sendMessage("§eTry /" + label + " test advancement [player] or /" + label + " test challenge [player]");
                    return true;
                }
                Advancement advancement = getRandomAdvancement(args[1].equalsIgnoreCase("advancement") ? AdvancementDisplayType.TASK : AdvancementDisplayType.CHALLENGE);
                if (advancement == null) {
                    sender.sendMessage("§eCouldn't find an advancement/challenge to randomly test, please try again.");
                    return true;
                }
                AdvancementDisplay advancementDisplay = advancement.getDisplay();
                if (advancementDisplay == null) {
                    sender.sendMessage("§eAdvancement Display is null???");
                    return true;
                } else {
                    if (!(sender instanceof Player p)) {
                        sender.sendMessage("§eYou must specific a player to test advancements as console.");
                        return true;
                    }
                    if (args.length == 3) {
                        p = Bukkit.getPlayer(args[2]);
                    }
                    if (p == null) {
                        sender.sendMessage("§ePlayer not found for " + args[2]);
                        return true;
                    }
                    boolean isChallenge = advancementDisplay.getType().equals(AdvancementDisplayType.CHALLENGE);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%player%", p.getName());
                    placeholders.put("%totalCompleted%", String.valueOf(getCompletedAdvancements(p)));
                    placeholders.put("%totalRemaining%", String.valueOf(getTotalAdvancements() - getCompletedAdvancements(p)));
                    placeholders.put("%totalAdvancements%", String.valueOf(getTotalAdvancements()));
                    placeholders.put("%advancement%", advancementDisplay.getType().getColor() + advancementDisplay.getTitle());
                    placeholders.put("%advancementDescription%", advancementDisplay.getType().getColor() + advancement.getDisplay().getDescription());
                    placeholders.put("%advancementNoFormat%", advancementDisplay.getTitle());
                    List<String> commands = getConfig().getStringList("AdvancementPlus." + (isChallenge ? "challenge" : "advancement") + "-rewards");
                    if (!commands.isEmpty()) {
                        for (String command : commands) {
                            if (command.startsWith("ap-broadcast")) {
                                String message = command.substring("ap-broadcast ".length());

                                TextComponent component = parseAdvancementMessage(
                                        message,
                                        placeholders,
                                        placeholders.get("%advancement%"),
                                        placeholders.get("%advancementDescription%")
                                );

                                for (Player online : Bukkit.getOnlinePlayers()) {
                                    online.spigot().sendMessage(component);
                                }
                            } else {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parseCommand(command, placeholders));
                            }
                        }
                    }
                }
            }
            return true;
        });
        getCommand("advancementsplus").setTabCompleter((sender, cmd, label, args) -> {
            if (args.length == 1) {
                return Arrays.asList("help", "reload", "test");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("test")) {
                return Arrays.asList("advancement", "challenge");
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("test")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
                return names;
            }
            return Collections.emptyList();
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public String parseCommand(String message, Map<String, String> placeholders) {
        String parsed = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            parsed = parsed.replace(entry.getKey(), entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', parsed);
    }

    public TextComponent parseAdvancementMessage(
            String message,
            Map<String, String> placeholders,
            String advancementText,
            String advancementHover
    ) {
        // Replace all placeholders EXCEPT %advancement%
        String processed = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (!entry.getKey().equalsIgnoreCase("%advancement%")) {
                processed = processed.replace(entry.getKey(), entry.getValue());
            }
        }

        // Split message around %advancement%
        String[] parts = processed.split("%advancement%", -1);

        TextComponent root = new TextComponent();

        // Before %advancement%
        if (!parts[0].isEmpty()) {
            root.addExtra(new TextComponent(
                    TextComponent.fromLegacyText(
                            ChatColor.translateAlternateColorCodes('&', parts[0])
                    )
            ));
        }

        // %advancement% component
        TextComponent advancementComponent = new TextComponent(
                TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', advancementText)
                )
        );

        advancementComponent.setHoverEvent(
                new HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        new Text(ChatColor.translateAlternateColorCodes('&', advancementHover))
                )
        );

        root.addExtra(advancementComponent);

        // After %advancement%
        if (parts.length > 1 && !parts[1].isEmpty()) {
            root.addExtra(new TextComponent(
                    TextComponent.fromLegacyText(
                            ChatColor.translateAlternateColorCodes('&', parts[1])
                    )
            ));
        }

        return root;
    }

    public Advancement getRandomAdvancement(AdvancementDisplayType advancementDisplayType) {
        Random random = new Random();
        int randomAdvancement = random.nextInt(getTotalAdvancements(advancementDisplayType));
        int advancementCounter = 0;
        for (Iterator<Advancement> it = Bukkit.getServer().advancementIterator(); it.hasNext(); ) {
            Advancement advancement = it.next();
            if (advancement.getDisplay() != null) {
                if (advancement.getDisplay().shouldAnnounceChat()) {
                    if (advancement.getDisplay().getType().equals(advancementDisplayType)) {
                        advancementCounter++;
                        if (advancementCounter == randomAdvancement) {
                            return advancement;
                        }
                    }
                }
            }
        }
        return null;
    }

    public int getTotalAdvancements() {
        int totalAdvancements = 0;
        for (Iterator<Advancement> it = Bukkit.getServer().advancementIterator(); it.hasNext(); ) {
            Advancement advancement = it.next();
            if (advancement.getDisplay() != null) {
                if (advancement.getDisplay().shouldAnnounceChat()) {
                    totalAdvancements++;
                }
            }
        }
        return totalAdvancements;
    }

    public int getTotalAdvancements(AdvancementDisplayType advancementDisplayType) {
        int totalAdvancements = 0;
        for (Iterator<Advancement> it = Bukkit.getServer().advancementIterator(); it.hasNext(); ) {
            Advancement advancement = it.next();
            if (advancement.getDisplay() != null) {
                if (advancement.getDisplay().shouldAnnounceChat()) {
                    if (advancement.getDisplay().getType().equals(advancementDisplayType)) {
                    totalAdvancements++;
                    }
                }
            }
        }
        return totalAdvancements;
    }

    public int getCompletedAdvancements(Player p) {
        int completedAdvancements = 0;
        for (Iterator<Advancement> it = Bukkit.getServer().advancementIterator(); it.hasNext(); ) {
            Advancement advancement = it.next();
            if (advancement.getDisplay() != null) {
                if (advancement.getDisplay().shouldAnnounceChat()) {
                    if (p.getAdvancementProgress(advancement).isDone()) {
                        completedAdvancements++;
                    }
                }
            }
        }
        return completedAdvancements;
    }
}
