package io.github.bl3rune.blu3printPlugin.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import io.github.bl3rune.blu3printPlugin.Blu3PrintPlugin;
import io.github.bl3rune.blu3printPlugin.config.GlobalConfig;
import io.github.bl3rune.blu3printPlugin.config.PlayerBlu3printConfig;
import io.github.bl3rune.blu3printPlugin.config.PlayerConfig;
import io.github.bl3rune.blu3printPlugin.enums.Alignment;
import io.github.bl3rune.blu3printPlugin.enums.Orientation;
import io.github.bl3rune.blu3printPlugin.enums.Rotation;
import io.github.bl3rune.blu3printPlugin.enums.Turn;
import io.github.bl3rune.blu3printPlugin.utils.EdgeCaseBlockUtils;
import io.github.bl3rune.blu3printPlugin.utils.EncodingUtils;
import io.github.bl3rune.blu3printPlugin.utils.Pair;

import static io.github.bl3rune.blu3printPlugin.Blu3PrintPlugin.logger;

public abstract class Blu3printData {

    protected static List<String> globalMaterialIgnoreList = new ArrayList<>();

    protected MaterialData[][][] selectionGrid; // [z] [y] [x]
    protected Map<String, Integer> ingredientsCount; // key: material, value: count
    protected Map<String, String> ingredientsMap; // key: material, value: encoded
    protected Map<String, String> complexDataMap; // key: complex-mapping, value: complex-encoding
    protected List<String> materialIgnoreList; // list of materials to ignore
    protected ManipulatablePosition position;
    protected String encoded;

    public MaterialData[][][] getSelectionGrid() {
        return selectionGrid;
    }

    public Map<String, Integer> getIngredientsCount() {
        return ingredientsCount;
    }

    public Map<String, String> getIngredientsMap() {
        return ingredientsMap;
    }

    public Map<String, String> getComplexDataMap() {
        return complexDataMap;
    }

    public ManipulatablePosition getPosition() {
        return position;
    }

    public String getEncodedString() {
        return encoded;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(ChatColor.WHITE).append("Ingredients:").append("\n").append(ChatColor.GRAY);
        ingredientsCount.forEach((k, v) -> sb.append(" - ")
                .append(k.replace("_", " "))
                .append(": ")
                .append(v * position.getScalingIngredientsMultiplier()).append("\n"));

        sb.append(ChatColor.WHITE).append("Position:").append("\n").append(ChatColor.GRAY);
        sb.append(" - X:Y:Z Sizes: ").append(position.getXSize() * position.getScale()).append(" : ");
        sb.append(position.getYSize() * position.getScale()).append(" : ");
        sb.append(position.getZSize() * position.getScale()).append("\n");
        sb.append(" - Orientation: ").append(position.getOrientation().name()).append("\n");
        sb.append(" - Rotation: ").append(position.getRotation().name()).append("\n");
        sb.append(" - Scale: ").append(position.getScale()).append("\n");
        return sb.toString();
    }

    // PLACING BLU3PRINT SECTION

    public void placeBlocks(Player player, Location location, boolean forced, boolean onTop, String blu3printUUID) {
        if (!playerAllowedToUse(player)) {
            return;
        }
        materialIgnoreList = buildMaterialIgnoreList(player, blu3printUUID);

        Function<Location, Location> calculateFinalLocation = buildCalculateFinalLocationFunction(player, location,
                onTop);
        Map<String, Integer> blocksUnableToPlace = checkSpaceIsClear(calculateFinalLocation);

        Map<String, Integer> missingBlocks = checkPlayerHasBLocksInInventory(player, false, blocksUnableToPlace);
        if (!missingBlocks.isEmpty()) {
            sendMessage(player, ChatColor.RED + "Missing these blocks to place the blu3print:");
            missingBlocks
                    .forEach((k, v) -> sendMessage(player, ChatColor.RED + " - " + k.replace("_", " ") + " : " + v));
            return;
        }

        if (!blocksUnableToPlace.isEmpty()) {
            if (forced && GlobalConfig.isForcePlacementMessageEnabled()) {
                sendMessage(player, ChatColor.AQUA + "Forcing placing blu3print despite blocks in the way.");
            } else if (!forced) {
                sendMessage(player, ChatColor.RED + "You can't place the blu3print here. There are blocks in the way.");
                sendMessage(player,
                        ChatColor.RED + "To force placement of the blu3print, sneak while using the blu3print.");
                return;
            }
        }

        checkPlayerHasBLocksInInventory(player, true, blocksUnableToPlace);

        int[] coords = position.next(true);
        while (coords != null) {

            int scale = position.getScale();
            MaterialData data = selectionGrid[coords[0] / scale][coords[1] / scale][coords[2] / scale];
            if (data == null || data.getMaterial() == null || isIgnorable(data.getMaterial())) {
                coords = position.next(true);
                continue;
            }

            Location placeLocation = calculateFinalLocation
                    .apply(new Location(location.getWorld(), coords[2], coords[1], coords[0]));
            Block block = placeLocation.getBlock();
            if (block != null && !isIgnorable(block.getType())) {
                coords = position.next(true);
                continue;
            }

            placeBlock(player, placeLocation, data);
            coords = position.next(true);
        }

    }

    public Function<Location, Location> buildCalculateFinalLocationFunction(Player player, Location location,
            boolean onTop) {
        final Alignment align = GlobalConfig.getAlignment();
        final boolean relative = GlobalConfig.getRelativePlacement();
        final BlockFace playerFacing = Orientation.getCartesianBlockFace(player.getFacing());
        Double x = location.getX();
        Double y = location.getY() + (onTop ? 1 : 0);
        Double z = location.getZ();
        double xSize = position.getXSize();
        double xSizeScaled = xSize * position.getScale();

        if (relative) {
            switch (playerFacing) {
                case NORTH:
                    if (xSize > 1 && align == Alignment.LEFT) {
                        x = x + xSizeScaled - 1;
                    } else if (xSize > 1 && align == Alignment.CENTER) {
                        x = x + (xSizeScaled / 2);
                    }
                    break;
                case EAST:
                    if (xSize > 1 && align == Alignment.LEFT) {
                        z = z + xSizeScaled - 1;
                    } else if (xSize > 1 && align == Alignment.CENTER) {
                        z = z + (xSizeScaled / 2);
                    }
                    break;
                case SOUTH:
                default:
                    if (xSize > 1 && align == Alignment.LEFT) {
                        x = x - xSizeScaled + 1;
                    } else if (xSize > 1 && align == Alignment.CENTER) {
                        x = x - (xSizeScaled / 2);
                    }
                    break;
                case WEST:
                    if (xSize > 1 && align == Alignment.LEFT) {
                        z = z - xSizeScaled + 1;
                    } else if (xSize > 1 && align == Alignment.CENTER) {
                        z = z - (xSizeScaled / 2);
                    }
            }
        } else { // NON RELATIVE IS ALWAYS FROM PLAYER LOOKING SOUTH WITH RIGHT ALIGNMENT AS
                 // DEFAULT
            if (xSize > 1 && align == Alignment.LEFT) {
                x = x - xSizeScaled;
            } else if (xSize > 1 && align == Alignment.CENTER) {
                x = x - (xSizeScaled / 2);
            }
        }

        final double xx = x.doubleValue();
        final double yy = y.doubleValue();
        final double zz = z.doubleValue();

        return (Location coords) -> {
            Double cx = coords.getX();
            Double cy = coords.getY() + yy;
            Double cz = coords.getZ();

            if (relative) {
                switch (playerFacing) {
                    case NORTH:
                        cx = xx - cx - 0.5;
                        cz = zz - cz;
                        break;
                    case EAST:
                        double ex = xx + cz;
                        double ez = zz - cx - 0.5;
                        cx = ex;
                        cz = ez;
                        break;
                    case SOUTH:
                    default:
                        cx = xx + cx;
                        cz = zz + cz;
                        break;
                    case WEST:
                        double wx = xx - cz;
                        double wz = zz + cx;
                        cx = wx;
                        cz = wz;
                }
            } else { // PLAYER FACING SOUTH DEFAULT
                cx = xx + cx;
                cz = zz + cz;
            }

            return new Location(location.getWorld(), cx.intValue(), cy.intValue(), cz.intValue());
        };
    }

    private Map<String, Integer> checkSpaceIsClear(Function<Location, Location> calculateFinalLocation) {
        int scale = position.getScale();
        int[] coords = position.next(true);
        Map<String, Integer> blocksUnableToPlace = new HashMap<>();
        while (coords != null) {
            MaterialData materialData = this.selectionGrid[coords[0] / scale][coords[1] / scale][coords[2] / scale];
            if (materialData == null || materialData.getName() == null || isIgnorable(materialData.getMaterial())) {
                coords = position.next(true);
                continue;
            }
            Location loc = calculateFinalLocation.apply(new Location(null, coords[2], coords[1], coords[0]));
            Block block = loc.getBlock();
            if (block != null && !isIgnorable(block.getType())) {
                Integer count = blocksUnableToPlace.getOrDefault(materialData.getMaterial().name(), 0);
                blocksUnableToPlace.put(materialData.getMaterial().name(), count + 1);
            }
            coords = position.next(true);
        }
        return blocksUnableToPlace;
    }

    private Map<String, Integer> checkPlayerHasBLocksInInventory(Player player, boolean removeBlocks,
            Map<String, Integer> blocksUnableToPlace) {
        if (player.getGameMode() == GameMode.CREATIVE || player.hasPermission("blu3print.no-block-cost")) {
            if (removeBlocks && GlobalConfig.isFreePlacementMessageEnabled()) {
                sendMessage(player, ChatColor.GREEN + "Placing Blu3print for free!");
            }
            return new HashMap<>();
        }

        Map<String, Integer> ingCountCopy = new HashMap<>(ingredientsCount);

        // Discount blocks unable to place
        boolean forcePlacePenalty = GlobalConfig.getForcePlacePenaltyEnabled();
        if (player.isSneaking() && (player.hasPermission("blu3print.force-place-discount") || !forcePlacePenalty)) {
            if (GlobalConfig.isDiscountPlacementMessageEnabled() && forcePlacePenalty) {
                sendMessage(player, ChatColor.GREEN + "Placing Blu3print for discount as blocks in the way!");
            }
            blocksUnableToPlace.forEach((material, amount) -> {
                Integer count = ingCountCopy.getOrDefault(material, 0);
                count = count - amount;
                if (count < 1) {
                    ingCountCopy.remove(material);
                } else {
                    ingCountCopy.put(material, amount);
                }
            });
        }
        Map<Integer, ItemStack> inventoryBlocks = new HashMap<>();
        Map<Integer, ItemStack> storageBlocks = new HashMap<>();
        int inventoryIndex = 0;
        boolean endOfInventory = false;
        Inventory inventory = player.getInventory();

        // Filter out storage blocks to a separate list
        while (!endOfInventory && inventoryIndex < inventory.getSize()) {
            try {
                ItemStack itemStack = inventory.getItem(inventoryIndex);
                if (itemStack == null || itemStack.getAmount() == 0 || itemStack.getType() == Material.AIR) {
                    inventoryIndex++;
                    continue;
                }
                if (itemStack.getItemMeta() instanceof BlockStateMeta) {
                    BlockStateMeta bsm = (BlockStateMeta) itemStack.getItemMeta();
                    if (bsm.getBlockState() instanceof Container) {
                        storageBlocks.put(inventoryIndex, itemStack);
                    } else {
                        inventoryBlocks.put(inventoryIndex, itemStack);
                    }
                } else {
                    inventoryBlocks.put(inventoryIndex, itemStack);
                }
                inventoryIndex++;
            } catch (Exception e) {
                logger().warning("ERROR checkPlayerHasBLocksInInventory INV SCAN");
                logger().warning(e.getMessage());
                e.printStackTrace();
                endOfInventory = true;
            }
        }

        // Process inventory blocks
        inventoryBlocks.forEach((k, v) -> {
            String blockName = v.getType().name();
            if (ingCountCopy.containsKey(blockName)) {
                int count = ingCountCopy.get(blockName);
                int stillNeeded = count - v.getAmount();
                if (stillNeeded < 1) {
                    ingCountCopy.remove(blockName);
                    if (removeBlocks) {
                        v.setAmount(v.getAmount() - count);
                        inventory.setItem(k, v);
                    }
                } else {
                    ingCountCopy.put(blockName, count);
                    if (removeBlocks) {
                        inventory.setItem(k, null);
                    }
                }
            }
        });

        if (ingCountCopy.isEmpty())
            return ingCountCopy;

        // process storage blocks
        storageBlocks.forEach((k, v) -> {
            BlockStateMeta bsm = (BlockStateMeta) v.getItemMeta();
            Container container = (Container) bsm.getBlockState();
            Inventory containerInventory = container.getInventory();
            Map<Integer, ItemStack> storageInventoryBlocks = new HashMap<>();
            int storageInventoryIndex = 0;
            boolean endOfStorageInventory = false;
            while (!endOfStorageInventory && storageInventoryIndex < containerInventory.getSize()) {
                try {
                    ItemStack itemStack = containerInventory.getItem(storageInventoryIndex);
                    if (itemStack == null || itemStack.getAmount() == 0 || itemStack.getType() == Material.AIR) {
                        storageInventoryIndex++;
                        continue;
                    }
                    storageInventoryBlocks.put(storageInventoryIndex, itemStack);
                    storageInventoryIndex++;
                } catch (Exception e) {
                    logger().warning("ERROR checkPlayerHasBLocksInInventory BLOCK INV SCAN POSITION " + k + " BLOCK " + v.getType().name());
                    logger().warning(e.getMessage());
                    e.printStackTrace();
                    endOfStorageInventory = true;
                }
            }

            storageInventoryBlocks.forEach((ik, iv) -> {
                String blockName = iv.getType().name();
                if (ingCountCopy.containsKey(blockName)) {
                    int count = ingCountCopy.get(blockName);
                    int stillNeeded = count - iv.getAmount();
                    if (stillNeeded < 1) {
                        ingCountCopy.remove(blockName);
                        if (removeBlocks) {
                            iv.setAmount(iv.getAmount() - count);
                            containerInventory.setItem(ik, iv);
                        }
                    } else {
                        ingCountCopy.put(blockName, count);
                        if (removeBlocks) {
                            containerInventory.setItem(ik, null);
                        }
                    }
                }
            });
            bsm.setBlockState(container);
            v.setItemMeta(bsm);
            inventory.setItem(k, v); // update container item in inventory
            if (ingCountCopy.isEmpty()) {
                return;
            }
        });

        return ingCountCopy;
    }

    private boolean placeBlock(Player player, Location location, MaterialData materialData) {

        World world = Bukkit.getWorld(location.getWorld().getName());
        if (world == null) {
            logger().info("World not found: " + location.getWorld().getName());
            return false;
        }

        String complexData = materialData.getComplexData();
        if (EdgeCaseBlockUtils.isEdgeCaseBlock(materialData)) {
            EdgeCaseBlockUtils.handleEdgeCasePlacement(player, location, materialData);
        } else if (complexData != null) {
            try {
                BlockData blockData = Bukkit.createBlockData(complexData);
                world.setBlockData(location, blockData);
            } catch (Exception e) {
                /* Tried to apply invalid block data */
                // e.printStackTrace();
            }
        } else {
            world.setType(location, materialData.getMaterial());
        }
        return true;
    }

    // EDITING BLU3PRINT SECTION

    public String updateEncodingWithTurn(Turn turn) {
        int s = position.getScale();
        Pair<Orientation, Rotation> turned = position.calculateTurn(turn);
        int[] newSizes = position.getNewSizes(turned.getA());
        newSizes = position.getNewSizes(turned.getB(), newSizes);
        updateDirectionalEncodings(turned.getA(), turned.getB());
        return updateManipulatablePosition(
                new ManipulatablePosition(newSizes[0], newSizes[1], newSizes[2], turned.getA(), turned.getB(), s));
    }

    public String updateEncodingWithOrientation(Orientation newOrientation) {
        Rotation r = position.getRotation();
        int s = position.getScale();
        int[] newSizes = position.getNewSizes(newOrientation);
        updateDirectionalEncodings(newOrientation, r);
        return updateManipulatablePosition(
                new ManipulatablePosition(newSizes[0], newSizes[1], newSizes[2], newOrientation, r, s));
    }

    public String updateEncodingWithRotation(Rotation newRotation) {
        Orientation o = position.getOrientation();
        int s = position.getScale();
        int[] newSizes = position.getNewSizes(newRotation);
        updateDirectionalEncodings(o, newRotation);
        return updateManipulatablePosition(
                new ManipulatablePosition(newSizes[0], newSizes[1], newSizes[2], o, newRotation, s));
    }

    public String updateEncodingWithScale(Player player, int newScale) {
        Integer maxScale = GlobalConfig.getMaxScale();
        if (player != null && maxScale != null && newScale > maxScale) {
            if (!player.hasPermission("blu3print.no-scale-limit")) {
                sendMessage(player, ChatColor.RED
                        + "You do not have permission to increase scale over the max scale limit of " + maxScale + "!");
                return null;
            }
        }

        Integer maxOverallSize = GlobalConfig.getMaxOverallSize();
        if (player != null && maxOverallSize != null && ((position.getXSize() * newScale) > maxOverallSize ||
                (position.getYSize() * newScale) > maxOverallSize ||
                (position.getZSize() * newScale) > maxOverallSize)) {
            if (!player.hasPermission("blu3print.no-scale-limit") && !player.hasPermission("blu3print.no-size-limit")) {
                sendMessage(player, ChatColor.RED
                        + "You do not have permission to increase size over the max overall size limit of "
                        + maxOverallSize + "!");
                return null;
            }
        }

        int scale = newScale / position.getScale();
        if (scale != 1) {
            Map<String, Integer> newCount = new HashMap<>();
            this.ingredientsCount.forEach((k, v) -> newCount.put(k, v * scale));
            this.ingredientsCount = newCount;
        }
        return updateManipulatablePosition(new ManipulatablePosition(position, newScale));
    }

    private void updateDirectionalEncodings(Orientation o, Rotation r) {
        // Map<String, String> ingredientsMapCopy = new HashMap<>(ingredientsMap);
        // DO NOTHING FOR NOW
        // this.ingredientsMap = ingredientsMapCopy;
    }

    private String updateManipulatablePosition(ManipulatablePosition newPosition) {
        String bodyString = EncodingUtils.getBodyFromEncoding(encoded);
        String newHeader = EncodingUtils
                .buildHeaderWithPerspective(EncodingUtils.ingredientsMapToString(ingredientsMap), newPosition);
        String newEncoding = EncodingUtils.buildEncodedString(newHeader, bodyString);
        sendMessage(null, "Updated blu3print: " + newEncoding);
        return newEncoding;
    }

    // UTILITY METHODS

    protected void sendMessage(Player player, String message) {
        if (player == null) {
            Blu3PrintPlugin.logger().info(message);
        } else {
            player.sendMessage(message);
        }
    }

    protected List<String> buildMaterialIgnoreList(Player player, String blu3printUUID) {
        List<String> ignoreList = new ArrayList<>();
        if (globalMaterialIgnoreList.isEmpty()) {
            globalMaterialIgnoreList = GlobalConfig.getIgnoredMaterials();
        }
        if (player == null) {
            return ignoreList;
        }
        String playerUUID = player.getUniqueId().toString();
        PlayerBlu3printConfig pbc = Blu3PrintPlugin.getPlayerBlu3printConfig(playerUUID);
        if (pbc != null) {
            if (pbc.uuidMatches(blu3printUUID)) {
                ignoreList.addAll(pbc.getIgnoredMaterials());
            } else {
                player.sendMessage(ChatColor.RED + "Cleared blu3print config as using different blu3print!");
                Blu3PrintPlugin.setPlayerBlu3printConfig(playerUUID, null);
            }
        }
        PlayerConfig pc = Blu3PrintPlugin.getPlayerConfig(playerUUID);
        if (pc != null) {
            ignoreList.addAll(pc.getIgnoredMaterials());
        } 
        return ignoreList;
    }

    protected boolean isIgnorable(Material material) {
        return material == null || material.isAir()
            || materialIgnoreList.contains(material.name().toUpperCase())
            || globalMaterialIgnoreList.contains(material.name().toUpperCase());
    }

    
    protected boolean playerAllowedToUse(Player player) {
        int scale = position.getScale();
        int [] sizes = new int [] { position.getXSize(), position.getYSize(), position.getZSize() };

        Integer maxSize = GlobalConfig.getMaxSize();
        if (player != null && maxSize != null && sizesExceedLimit(sizes, 1, maxSize)) {
            if (!player.hasPermission("blu3print.no-size-limit")) {
                sendMessage(player,ChatColor.RED + "You do not have permission to set size over the max size limit of " + maxSize + "!");
                return false;
            }
        }

        Integer maxScale = GlobalConfig.getMaxScale();
        if (player != null && maxScale != null && scale > maxScale) {
            if (!player.hasPermission("blu3print.no-scale-limit")) {
                sendMessage(player,ChatColor.RED + "You do not have permission to increase scale over the max scale limit of " + maxScale + "!");
                return false;
            }
        }

        Integer maxOverallSize = GlobalConfig.getMaxOverallSize();
        if (player != null && maxOverallSize != null && sizesExceedLimit(sizes, scale, maxOverallSize)) {
            if (!player.hasPermission("blu3print.no-scale-limit") && !player.hasPermission("blu3print.no-size-limit")) {
                sendMessage(player,ChatColor.RED + "You do not have permission to increase size over the max overall size limit of " + maxOverallSize + "!");
                return false;
            }
        }

        return true;
    }

    protected boolean sizesExceedLimit(int[] sizes, int scale, int max) {
        for (int size : sizes) {
            if (size * scale > max) {
                return true;
            }
        }
        return false;
    }

}
