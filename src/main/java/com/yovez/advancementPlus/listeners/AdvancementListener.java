package com.yovez.advancementPlus.listeners;

import com.yovez.advancementPlus.AdvancementPlus;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementDisplay;
import org.bukkit.advancement.AdvancementDisplayType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdvancementListener implements Listener {

    private final AdvancementPlus plugin;

    public AdvancementListener(AdvancementPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdvancementGain(PlayerAdvancementDoneEvent e) {
        Advancement advancement = e.getAdvancement();
        Player p = e.getPlayer();

        AdvancementDisplay advancementDisplay = advancement.getDisplay();
        if (advancementDisplay != null) {
            boolean isChallenge = advancementDisplay.getType().equals(AdvancementDisplayType.CHALLENGE);
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("%player%", p.getName());
            placeholders.put("%totalCompleted%", String.valueOf(plugin.getCompletedAdvancements(p)));
            placeholders.put("%totalRemaining%", String.valueOf(plugin.getTotalAdvancements() - plugin.getCompletedAdvancements(p)));
            placeholders.put("%totalAdvancements%", String.valueOf(plugin.getTotalAdvancements()));
            placeholders.put("%advancement%", advancementDisplay.getType().getColor() + advancementDisplay.getTitle());
            placeholders.put("%advancementDescription%", advancementDisplay.getType().getColor() + advancement.getDisplay().getDescription());
            placeholders.put("%advancementNoFormat%", advancementDisplay.getTitle());
            List<String> commands = plugin.getConfig().getStringList("AdvancementPlus.rewards" + (isChallenge ? "challenge" : "advancement"));
            if (!commands.isEmpty()) {
                for (String command : commands) {
                    if (command.startsWith("ap-broadcast")) {
                        String message = command.substring("ap-broadcast ".length());

                        TextComponent component = plugin.parseAdvancementMessage(
                                message,
                                placeholders,
                                placeholders.get("%advancement%"),
                                placeholders.get("%advancementDescription%")
                        );

                        for (Player online : Bukkit.getOnlinePlayers()) {
                            online.spigot().sendMessage(component);
                        }
                    } else {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.parseCommand(command, placeholders));
                    }
                }
            }
            if (plugin.getCompletedAdvancements(p) == plugin.getTotalAdvancements()) {
                List<String> completionCommands = plugin.getConfig().getStringList("AdvancementPlus.rewards.completion");
                if (!completionCommands.isEmpty()) {
                    for (String command : completionCommands) {
                        if (command.startsWith("ap-broadcast")) {
                            String message = command.substring("ap-broadcast ".length());
                            Bukkit.broadcastMessage(plugin.parseCommand(message, placeholders));
                        } else {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.parseCommand(command, placeholders));
                        }
                    }
                }
            }
        }
    }

}
