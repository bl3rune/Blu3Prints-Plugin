package io.github.bl3rune.blu3printPlugin.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.bl3rune.blu3printPlugin.Blu3PrintPlugin;
import io.github.bl3rune.blu3printPlugin.config.GlobalConfig;
import io.github.bl3rune.blu3printPlugin.config.PlayerBlu3printConfig;
import io.github.bl3rune.blu3printPlugin.enums.Config;
import io.github.bl3rune.blu3printPlugin.utils.InventoryUtils;

public class ConfigCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Config config = null;
            try {
                String c = args[0];
                config = Config.valueOf(c.toUpperCase());
            } finally {
                if (config == null) {
                    StringBuilder sb = new StringBuilder();
                    for (Config cc : Config.values()) {
                        sb.append(cc.name()).append(" ");
                    }
                    player.sendMessage(ChatColor.RED + "Not valid subcommand try : " + sb.toString());
                    return true;
                }
            }

            ItemStack blu3print = InventoryUtils.getHeldBlu3print(player, false);
            if (blu3print == null || !blu3print.hasItemMeta()) {
                sender.sendMessage(ChatColor.RED + "You must be holding a blu3print to duplicate it.");
                return true;
            }
            ItemMeta meta = blu3print.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null || lore.size() < 2) {
                player.sendMessage(ChatColor.RED + "Blu3print missing ID");
                return true;
            }
            String blu3printUuid = lore.get(1);

            String playerUUID = player.getUniqueId().toString();
            PlayerBlu3printConfig ppbc = Blu3PrintPlugin.getPlayerBlu3printConfig(playerUUID);
            if (ppbc == null) {
                ppbc = new PlayerBlu3printConfig(blu3printUuid);
            }
            switch (config) {
                case CLEAR:
                    Blu3PrintPlugin.setPlayerBlu3printConfig(playerUUID, null);
                    player.sendMessage(ChatColor.GREEN + "Player blu3print config reset!");
                    return true;
                case HOLOGRAM_VIEW_XYZ:
                case HOLOGRAM_VIEW_X:
                case HOLOGRAM_VIEW_Y:
                case HOLOGRAM_VIEW_Z:
                    ppbc = setHologramViewLayers(ppbc, args, config, player);
                    break;
                case IGNORE_MATERIAL:
                case ALLOW_MATERIAL:
                    ppbc = modifyMaterialIgnoreList(ppbc, args, config, player);
                    break;
                case SCALE_X:
                case SCALE_Y:
                case SCALE_Z:
                case SCALE_XYZ:
                    ppbc = setScale(ppbc, args, config, player);
                default:
                    break;
            }
            if (ppbc != null) {
                Blu3PrintPlugin.setPlayerBlu3printConfig(playerUUID, ppbc);
            }
        }
        return true;
    }

    private PlayerBlu3printConfig setHologramViewLayers(PlayerBlu3printConfig ppbc, String[] args, Config config,
            Player player) {
        if (args.length < 2 || (config == Config.HOLOGRAM_VIEW_XYZ && args.length < 4)) {
            player.sendMessage(ChatColor.RED + "Not enough arguments for setting hologram view layers");
            return null;
        }
        int[][] layers = ppbc.getHologramViewLayers();
        switch (config) {
            case HOLOGRAM_VIEW_XYZ:
                layers[0] = extractHologramViewLayers(args[1], player);
                layers[1] = extractHologramViewLayers(args[2], player);
                layers[2] = extractHologramViewLayers(args[3], player);
                break;
            case HOLOGRAM_VIEW_X:
                layers[0] = extractHologramViewLayers(args[1], player);
                break;
            case HOLOGRAM_VIEW_Y:
                layers[1] = extractHologramViewLayers(args[1], player);
                break;
            case HOLOGRAM_VIEW_Z:
                layers[2] = extractHologramViewLayers(args[1], player);
            default:
        }
        StringBuilder sb = new StringBuilder();
        boolean restricted = false;
        sb.append(ChatColor.GREEN);
        sb.append("Hologram will now show");
        if (layers[0] != null) {
            restricted = true;
            sb.append("\n - only X layers: " + arrayToString(layers[0]));
        }
        if (layers[1] != null) {
            restricted = true;
            sb.append("\n - only Y layers: " + arrayToString(layers[1]));
        }
        if (layers[2] != null) {
            restricted = true;
            sb.append("\n - only Z layers: " + arrayToString(layers[2]));
        }
        if (!restricted) {
            sb.append(" completely");
        }
        player.sendMessage(sb.toString());
        ppbc.setHologramViewLayers(layers);

        return ppbc;
    }

    private int[] extractHologramViewLayers(String arg, Player player) {
        if (arg == null || arg.isBlank())
            return null;
        String[] seperated = arg.split(Pattern.quote(","));
        List<Integer> values = new ArrayList<>();
        for (String v : seperated) {
            try {
                if (v.contains("-")) {
                    String[] range = v.split(Pattern.quote("-"));
                    IntStream.rangeClosed(Integer.parseInt(range[0]), Integer.parseInt(range[1])).forEach(i -> {
                        values.add(i);
                    });
                } else {
                    Integer i = Integer.parseInt(v);
                    if (i != null) {
                        values.add(i);
                    }
                }
            } catch (Exception e) {
                player.sendMessage("Poorly formed argument : " + arg);
            }
        }
        return values.stream().mapToInt((a) -> a).toArray();
    }

    private String arrayToString(int[] array) {
        if (array == null)
            return "no limit";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i : array) {
            if (!first) {
                sb.append(",");
            }
            sb.append(i);
            first = false;
        }
        return sb.toString();
    }

    private PlayerBlu3printConfig modifyMaterialIgnoreList(PlayerBlu3printConfig ppbc, String[] args, Config config,
            Player player) {
        if (args.length < 2) {
            player.sendMessage("Usage: /blu3print.config IGNORE_MATERIALS [material]");
            return ppbc;
        }
        Material material; 
        try {
            material = Material.matchMaterial(args[1]);
            if (material == null) {
                player.sendMessage(ChatColor.RED + "Invalid material : " + args[1]);
                return ppbc;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Invalid material : " + args[1]);
            return ppbc;
        }
        
        List<String> ignoredMaterials = ppbc.getIgnoredMaterials();
        if (config == Config.IGNORE_MATERIAL) {
            if (GlobalConfig.isVerboseLogging()) {
                player.sendMessage(ChatColor.GREEN + "Added " + material.name() + " to ignore list");
            }
            ignoredMaterials.add(material.name().toUpperCase());
        } else {
            if (GlobalConfig.isVerboseLogging()) {
                player.sendMessage(ChatColor.RED + "Remooved " + material.name() + " from ignore list");
            }
            ignoredMaterials.removeIf(m -> m.equalsIgnoreCase(material.name()));
        }
        ppbc.setIgnoredMaterials(ignoredMaterials);
        return ppbc;
    }

    private PlayerBlu3printConfig setScale(PlayerBlu3printConfig ppbc, String[] args, Config config, Player player) {
        if (args.length < 2) {
            player.sendMessage("Usage: /blu3print.config SCALE_X [scale]");
            return ppbc;
        }

        if (config == Config.SCALE_XYZ) {
            if (args.length < 4) {
                player.sendMessage("Usage: /blu3print.config SCALE_XYZ [scale X] [scale Y] [scale Z]");
                return ppbc;
            }
            int [] newScales = new int[] {
                Integer.parseInt(args[1]),
                Integer.parseInt(args[2]),
                Integer.parseInt(args[3])
            };

            ppbc.setXyzScale(newScales);
            if (GlobalConfig.isVerboseLogging()) {
                player.sendMessage(ChatColor.GREEN + "Scale is now " + newScales[0] + ", " + newScales[1] + ", " + newScales[2]);
            }
            return ppbc;
        }

        int [] xyz = ppbc.getXyzScale();
        int scale = Integer.parseInt(args[1]);
        switch (config) {
            case SCALE_X:
                xyz[0] = scale;
                break;
            case SCALE_Y:
                xyz[1] = scale;
            default:
                xyz[2] = scale;
                break;
        }
        ppbc.setXyzScale(xyz);
        if (GlobalConfig.isVerboseLogging()) {
            player.sendMessage(ChatColor.GREEN + "Scale is now " + xyz[0] + ", " + xyz[1] + ", " + xyz[2]);
        }
        return ppbc;
    }

}
