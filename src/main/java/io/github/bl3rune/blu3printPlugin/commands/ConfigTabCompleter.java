package io.github.bl3rune.blu3printPlugin.commands;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import io.github.bl3rune.blu3printPlugin.enums.Config;

public class ConfigTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList(Config.values()).stream().map(c -> c.name()).collect(Collectors.toList());
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
                case SCALE_X:
                case SCALE_Y:
                case SCALE_Z:
                case SCALE_XYZ:
                    return List.of("1","2","3","4","5","6","7","8","9", "...");
                case ALLOW_MATERIAL:
                case IGNORE_MATERIAL:
                    return List.of("STONE", "SAND", "GRAVEL", "...");
                case HOLOGRAM_VIEW_X:
                case HOLOGRAM_VIEW_Y:
                case HOLOGRAM_VIEW_Z:
                case HOLOGRAM_VIEW_XYZ:
                    return List.of("0,1,2", "0", "1", "2", "1-2", "...");
                default:
                    break;
            }
        }
        return null;
    }

}
