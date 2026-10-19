package probe;

import com.dre.brewery.BPlayer;
import com.dre.brewery.Brew;
import com.dre.brewery.BSealer;
import com.dre.brewery.recipe.BRecipe;
import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Config;
import com.dre.brewery.listeners.InventoryListener;
import java.lang.reflect.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.block.Barrel;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;

/** Run only on an isolated offline regression server. Uses actual plugin listeners and core inventories. */
public final class BreweryProbe extends JavaPlugin implements Listener {
    private final java.util.concurrent.atomic.AtomicInteger failures = new java.util.concurrent.atomic.AtomicInteger();
    private final ThreadLocal<Item> spawned = new ThreadLocal<>();
    @EventHandler public void spawn(ItemSpawnEvent event) { spawned.set(event.getEntity()); }
    @Override public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PROBE_READY " + Bukkit.getVersion());
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.setOp(true);
        player.setGameMode(GameMode.CREATIVE);
        player.getScheduler().runDelayed(this, task -> player.teleportAsync(new Location(player.getWorld(), player.getName().endsWith("2") ? 2048.5 : 0.5, 81, 0.5)), null, 10);
    }
    private void check(String name, boolean condition) {
        if (!condition) failures.incrementAndGet();
        getLogger().info((condition ? "PASS " : "FAIL ") + name);
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;
        if (args[0].equals("enable")) {
            ConfigManager.getConfig(Config.class).setEnablePuke(true);
            getLogger().info("PUKE_ENABLED");
        } else if (args[0].equals("disable")) {
            ConfigManager.getConfig(Config.class).setEnablePuke(false);
            getLogger().info("PUKE_DISABLED");
        } else if (args[0].equals("summary")) {
            getLogger().info("PROBE_FAILURES=" + failures.get());
        } else if (sender instanceof Player player) {
            try {
                switch (args[0]) {
                    case "holder" -> holder(player);
                    case "identities" -> identities(player);
                    case "fixture-write" -> fixtures(true);
                    case "fixture-read" -> fixtures(false);
                    case "puke" -> { BPlayer.addPuke(player, 4); getLogger().info("PUKE_REQUEST " + player.getName()); }
                    case "direct" -> { BPlayer.puke(player); check("direct-puke", true); }
                    case "metadata" -> {
                        boolean clean = true;
                        for (int i = 0; i < 128; i++) {
                            BPlayer.puke(player);
                            Item item = spawned.get();
                            item.remove();
                            clean &= !item.hasMetadata("brewery_puke");
                        }
                        spawned.remove();
                        check("128-removed-puke-items-no-metadata", clean);
                    }
                    case "one-tick" -> {
                        Config config = ConfigManager.getConfig(Config.class);
                        int old = config.getPukeDespawntime();
                        try { config.setPukeDespawntime(1); BPlayer.puke(player); check("one-tick-puke", true); }
                        finally { config.setPukeDespawntime(old); }
                    }
                    case "drunk" -> {
                        BPlayer bp = BPlayer.get(player);
                        if (bp == null) bp = BPlayer.addPlayer(player);
                        bp.setDrunkenness(100);
                        getLogger().info("DRUNK " + player.getName());
                    }
                }
            } catch (Throwable error) { failures.incrementAndGet(); getLogger().log(java.util.logging.Level.SEVERE, "PROBE_EXCEPTION " + args[0], error); }
        }
        return true;
    }
    private static String brewState(Brew brew) {
        return brew.getQuality() + "|" + brew.getAlc() + "|" + brew.getDistillRuns() + "|" + brew.getAgeTime()
            + "|" + brew.getWood() + "|" + brew.getCurrentRecipe().getRecipeName() + "|" + brew.isStatic()
            + "|" + brew.isStripped() + "|" + brew.isUnlabeled();
    }
    private void fixtures(boolean write) throws Exception {
        var folder = getDataFolder().toPath();
        java.nio.file.Files.createDirectories(folder);
        var manifest = folder.resolve("legacy-fixtures.properties");
        Properties expected = new Properties();
        if (write) {
            BRecipe recipe = BRecipe.getAllRecipes().stream().sorted(Comparator.comparing(BRecipe::getRecipeName)).findFirst().orElseThrow();
            for (int quality : new int[]{2, 6, 10}) {
                ItemStack item = recipe.create(quality);
                String name = "brew-" + quality;
                expected.setProperty(name, brewState(Brew.get(item)));
                java.nio.file.Files.write(folder.resolve(name + ".bin"), item.serializeAsBytes());
            }
            try (var out = java.nio.file.Files.newOutputStream(manifest)) { expected.store(out, BreweryPlugin.getInstance().getDescription().getVersion()); }
            check("legacy-fixtures-written", expected.size() == 3);
        } else {
            try (var in = java.nio.file.Files.newInputStream(manifest)) { expected.load(in); }
            for (String name : expected.stringPropertyNames()) {
                ItemStack item = ItemStack.deserializeBytes(java.nio.file.Files.readAllBytes(folder.resolve(name + ".bin")));
                Brew brew = Brew.get(item);
                check("legacy-load-" + name, expected.getProperty(name).equals(brewState(brew)));
                var meta = item.getItemMeta();
                brew.save(meta);
                item.setItemMeta(meta);
                check("legacy-resave-" + name, expected.getProperty(name).equals(brewState(Brew.get(item))));
            }
        }
    }
    private void identities(Player player) {
        BSealer sealer = new BSealer(player);
        check("actual-sealer-holder-identity", sealer.getInventory().getHolder(false) == sealer);
        var block = player.getLocation().add(1, 0, 0).getBlock();
        com.dre.brewery.Barrel barrel = new com.dre.brewery.Barrel(block, (byte) 0,
            new com.dre.brewery.utility.BoundingBox(block.getX(), block.getY(), block.getZ(), block.getX()+2, block.getY()+2, block.getZ()+2),
            (ItemStack[]) null, 0, UUID.randomUUID(), true);
        check("actual-custom-barrel-holder-identity", barrel.getInventory().getHolder(false) == barrel);
        block.setType(Material.BREWING_STAND);
        BrewerInventory brewer = ((org.bukkit.block.BrewingStand) block.getState(false)).getInventory();
        check("actual-brewer-holder-is-live", !((org.bukkit.block.BrewingStand) brewer.getHolder(false)).isSnapshot());
        new InventoryListener().onHopperMove(new InventoryMoveItemEvent(brewer, new ItemStack(Material.STONE), Bukkit.createInventory(null, 9), true));
        check("brewer-hopper-listener", true);
        block.setType(Material.AIR);
    }
    private void holder(Player player) throws Exception {
        var block = player.getLocation().add(1, 0, 0).getBlock();
        block.setType(Material.BARREL);
        Inventory real = ((Barrel) block.getState(false)).getInventory();
        real.setItem(0, new ItemStack(Material.DIAMOND, 7));
        byte[] before = real.getItem(0).serializeAsBytes();
        int[] snapshots = {0};
        Inventory wrapped = (Inventory) Proxy.newProxyInstance(getClassLoader(), new Class[]{Inventory.class}, (proxy, method, arguments) -> {
            if (method.getName().equals("getHolder") && (arguments == null || (boolean) arguments[0])) snapshots[0]++;
            try { return method.invoke(real, arguments); } catch (InvocationTargetException e) { throw e.getCause(); }
        });
        InventoryView actual = player.openInventory(real);
        InventoryView view = (InventoryView) Proxy.newProxyInstance(getClassLoader(), new Class[]{InventoryView.class}, (proxy, method, arguments) -> {
            if (method.getName().equals("getTopInventory")) return wrapped;
            try { return method.invoke(actual, arguments); } catch (InvocationTargetException e) { throw e.getCause(); }
        });
        InventoryListener listener = new InventoryListener();
        InventoryClickEvent click = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.LEFT, InventoryAction.NOTHING);
        listener.onInventoryClick(click);
        check("barrel-click-no-snapshot", snapshots[0] == 0); snapshots[0] = 0;
        listener.onInventoryClickBSealer(click);
        check("sealer-listener-no-snapshot", snapshots[0] == 0); snapshots[0] = 0;
        listener.onInventoryDrag(new InventoryDragEvent(view, new ItemStack(Material.STONE), new ItemStack(Material.STONE, 2), false, Map.of(0, new ItemStack(Material.STONE))));
        check("barrel-drag-no-snapshot", snapshots[0] == 0); snapshots[0] = 0;
        listener.onInventoryClose(new InventoryCloseEvent(view));
        check("barrel-close-no-snapshot", snapshots[0] == 0);
        check("barrel-content-preserved", Arrays.equals(before, real.getItem(0).serializeAsBytes()));
        player.closeInventory();
    }
}
