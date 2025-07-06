package io.github.bl3rune.blu3printPlugin.commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import io.github.bl3rune.blu3printPlugin.enums.Config;

public class PlayerConfigTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList(Config.values()).stream().filter(c -> c.isPlayerLevelConfig()).map(c -> c.name()).collect(Collectors.toList());
        }
        if (args.length == 2) {
            Config config = null;
            try {
                String c = args[0];
                config = Config.valueOf(c.toUpperCase());
            } finally {
                if (config == null) {
                    return null;
                }
            }
            
            switch (config) {
                case ALLOW_MATERIAL:
                case IGNORE_MATERIAL:
                    return List.of("STONE", "SAND", "GRAVEL", "...");
                default:
                    break;
            }
        }
        return null;
    }

}
