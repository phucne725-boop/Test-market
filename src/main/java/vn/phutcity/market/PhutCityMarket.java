package vn.phutcity.market;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PhutCityMarket extends JavaPlugin implements Listener {

    private static final int[] PRODUCT_SLOTS = {11, 13, 15};

    private final Map<UUID, String> awaitingPrice =
            new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        saveConfig();
        getLogger().info("PhutCityMarket 0.1.0 enabled!");
    }

    @Override
    public void onDisable() {
        saveConfig();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used in-game.");
            return true;
        }

        if (!player.hasPermission("phutcity.market.admin")) {
            player.sendMessage(ChatColor.RED + "You do not have permission.");
            return true;
        }

        Block target = player.getTargetBlockExact(6);

        if (target == null || !isShelf(target)) {
            player.sendMessage(
                    ChatColor.YELLOW +
                    "Look at a Shelf block within 6 blocks, then run /marketadmin."
            );
            return true;
        }

        openManager(player, target.getLocation());
        return true;
    }

    @EventHandler
    public void onShelfInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();

        if (block == null || !isShelf(block)) {
            return;
        }

        Player player = event.getPlayer();

        event.setCancelled(true);

        if (!player.hasPermission("phutcity.market.admin")) {
            return;
        }

        openManager(player, block.getLocation());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder()
                instanceof ShelfHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory top = event.getView().getTopInventory();

        if (event.getClickedInventory() == top) {
            int productIndex = productIndex(event.getRawSlot());

            if (productIndex < 0) {
                return;
            }

            String base = shelfKey(holder.location);
            String path = base + ".products." + productIndex;

            ItemStack stored = getConfig().getItemStack(path + ".item");

            if (stored == null || stored.getType().isAir()) {
                return;
            }

            if (event.getClick() == ClickType.RIGHT
                    || event.getClick() == ClickType.SHIFT_RIGHT) {

                getConfig().set(path, null);
                saveConfig();

                player.sendMessage(
                        ChatColor.GREEN + "Removed product from shelf."
                );

                refreshManager(player, holder.location);
            } else {
                awaitingPrice.put(player.getUniqueId(), path);

                player.closeInventory();

                player.sendMessage(
                        ChatColor.YELLOW +
                        "Type the new price in chat. Type 'cancel' to cancel."
                );
            }

            return;
        }

        if (event.getClickedInventory()
                == event.getView().getBottomInventory()
                && event.isShiftClick()) {

            ItemStack clicked = event.getCurrentItem();

            if (clicked == null || clicked.getType().isAir()) {
                return;
            }

            String base = shelfKey(holder.location);

            for (int i = 0; i < PRODUCT_SLOTS.length; i++) {
                String path = base + ".products." + i;

                ItemStack existing =
                        getConfig().getItemStack(path + ".item");

                if (existing == null || existing.getType().isAir()) {
                    ItemStack copy = clicked.clone();
                    copy.setAmount(1);

                    getConfig().set(path + ".item", copy);
                    getConfig().set(path + ".price", 0.0);

                    saveConfig();

                    awaitingPrice.put(player.getUniqueId(), path);

                    player.closeInventory();

                    player.sendMessage(
                            ChatColor.GREEN +
                            "Added " + copy.getType().name() +
                            ". Type its price in chat."
                    );

                    return;
                }
            }

            player.sendMessage(
                    ChatColor.RED + "This shelf already has 3 products."
            );
        }
    }

    @EventHandler
    @SuppressWarnings("deprecation")
    public void onPriceChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        String path = awaitingPrice.get(uuid);

        if (path == null) {
            return;
        }

        event.setCancelled(true);

        String message = event.getMessage().trim();

        Bukkit.getScheduler().runTask(this, () -> {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null) {
                awaitingPrice.remove(uuid);
                return;
            }

            if (message.equalsIgnoreCase("cancel")) {
                awaitingPrice.remove(uuid);

                player.sendMessage(
                        ChatColor.YELLOW + "Price editing cancelled."
                );
                return;
            }

            try {
                double price = Double.parseDouble(
                        message.replace(',', '.')
                );

                if (!Double.isFinite(price) || price < 0) {
                    throw new NumberFormatException();
                }

                getConfig().set(path + ".price", price);
                saveConfig();

                awaitingPrice.remove(uuid);

                player.sendMessage(
                        ChatColor.GREEN + "Saved product price: " + price
                );

            } catch (NumberFormatException exception) {
                player.sendMessage(
                        ChatColor.RED +
                        "Invalid price. Enter a number or 'cancel'."
                );
            }
        });
    }

    private void openManager(Player player, Location location) {
        Inventory inventory = Bukkit.createInventory(
                new ShelfHolder(location),
                27,
                ChatColor.DARK_GREEN + "Shelf Manager"
        );

        String base = shelfKey(location);

        for (int i = 0; i < PRODUCT_SLOTS.length; i++) {
            String path = base + ".products." + i;

            ItemStack item =
                    getConfig().getItemStack(path + ".item");

            if (item == null || item.getType().isAir()) {
                ItemStack empty = new ItemStack(Material.BARRIER);

                var meta = empty.getItemMeta();

                if (meta != null) {
                    meta.setDisplayName(
                            ChatColor.RED + "Empty product slot " + (i + 1)
                    );
                    empty.setItemMeta(meta);
                }

                inventory.setItem(PRODUCT_SLOTS[i], empty);

            } else {
                ItemStack display = item.clone();

                var meta = display.getItemMeta();

                if (meta != null) {
                    meta.setLore(List.of(
                            ChatColor.GRAY + "Price: " +
                                    getConfig().getDouble(path + ".price"),
                            ChatColor.YELLOW + "Left-click: set price",
                            ChatColor.RED + "Right-click: remove"
                    ));

                    display.setItemMeta(meta);
                }

                inventory.setItem(PRODUCT_SLOTS[i], display);
            }
        }

        ItemStack info = new ItemStack(Material.PAPER);

        var infoMeta = info.getItemMeta();

        if (infoMeta != null) {
            infoMeta.setDisplayName(
                    ChatColor.GOLD + "Shelf instructions"
            );

            infoMeta.setLore(List.of(
                    ChatColor.YELLOW +
                            "Shift-click an item in your inventory to add it.",
                    ChatColor.YELLOW +
                            "Then enter its price in chat.",
                    ChatColor.YELLOW +
                            "Left-click a product to change price.",
                    ChatColor.YELLOW +
                            "Right-click a product to remove it."
            ));

            info.setItemMeta(infoMeta);
        }

        inventory.setItem(4, info);

        player.openInventory(inventory);
    }

    private void refreshManager(Player player, Location location) {
        Bukkit.getScheduler().runTask(
                this,
                () -> openManager(player, location)
        );
    }

    private boolean isShelf(Block block) {
        return block.getType().name().endsWith("SHELF");
    }

    private int productIndex(int slot) {
        for (int i = 0; i < PRODUCT_SLOTS.length; i++) {
            if (PRODUCT_SLOTS[i] == slot) {
                return i;
            }
        }

        return -1;
    }

    private String shelfKey(Location location) {
        return "shelves."
                + location.getWorld().getName() + "."
                + location.getBlockX() + "."
                + location.getBlockY() + "."
                + location.getBlockZ();
    }

    private static final class ShelfHolder
            implements InventoryHolder {

        private final Location location;

        private ShelfHolder(Location location) {
            this.location = location.clone();
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
          }
