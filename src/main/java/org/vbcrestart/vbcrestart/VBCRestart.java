// 1.0-BETA
package org.vbcrestart.vbcrestart;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class VBCRestart extends JavaPlugin implements CommandExecutor, TabCompleter, Listener {

    private BukkitTask restartTask;
    private BossBar activeBar;
    private boolean isInCage = false;
    private Location cageLocation;

    @Override
    public void onEnable() {
        if (getCommand("vbc") != null) {
            getCommand("vbc").setExecutor(this);
            getCommand("vbc").setTabCompleter(this);
        }
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) return false;


        if (args[0].equalsIgnoreCase("s")) {
            if (restartTask == null) {
                sender.sendMessage("§cНет активной перезагрузки!");
                return true;
            }
            stopRestart();
            Bukkit.broadcastMessage("§a[VBC] Перезагрузка отменена администратором.");
            return true;
        }


        if (args[0].equalsIgnoreCase("restart") && args.length >= 2) {
            if (restartTask != null) {
                sender.sendMessage("§cПерезагрузка уже идет!");
                return true;
            }
            int delay;
            try {
                delay = Integer.parseInt(args[1]);
            } catch (Exception e) {
                sender.sendMessage("§cВведите время числом!");
                return true;
            }
            startRestartTimer(delay);
            sender.sendMessage("§aТаймер перезагрузки запущен!");
            return true;
        }
        return true;
    }

    private void startRestartTimer(int seconds) {
        activeBar = Bukkit.createBossBar("§6Рестарт через §l" + seconds + " §6сек.", BarColor.RED, BarStyle.SOLID);
        Bukkit.getOnlinePlayers().forEach(activeBar::addPlayer);

        restartTask = new BukkitRunnable() {
            int timeLeft = seconds;
            final double step = 1.0 / seconds;

            @Override
            public void run() {

                if (timeLeft == 30) {
                    createGlassCage();
                    isInCage = true;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.teleport(cageLocation.clone().add(0.5, 1, 0.5));
                        p.setInvulnerable(true);
                        p.sendTitle("§c§lВНИМАНИЕ!", "§6Ожидание рестарта в небе", 10, 60, 10);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    }
                }


                if (timeLeft == 60 || timeLeft == 10) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§6Рестарт через", "§c§l" + timeLeft + " §6сек", 10, 40, 10);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                }


                if (timeLeft <= 5 && timeLeft > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("§c§l" + timeLeft, "§6Приготовьтесь...", 0, 21, 0);
                        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.5f);
                    }
                }


                if (timeLeft <= 0) {
                    String kickReason = "§c§lСервер Перезагружается.\n§6Зайдите позже.";
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.setInvulnerable(false);
                        p.kickPlayer(kickReason);
                    }
                    removeGlassCage();
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "restart");
                    this.cancel();
                    return;
                }

                activeBar.setTitle("§6Рестарт через §l" + timeLeft + " §6сек.");
                activeBar.setProgress(Math.max(0.0, timeLeft * step));
                timeLeft--;
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void stopRestart() {
        if (restartTask != null) restartTask.cancel();
        if (activeBar != null) activeBar.removeAll();
        if (isInCage) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.setInvulnerable(false);

                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 600, 1));
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
            removeGlassCage();
        }
        isInCage = false;
        restartTask = null;
        activeBar = null;
    }

    private void createGlassCage() {
        World world = Bukkit.getWorlds().get(0);
        cageLocation = new Location(world, 0.5, 200, 0.5);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 3; y++) {
                    if (y == 0 || x == -3 || x == 3 || z == -3 || z == 3) {
                        cageLocation.clone().add(x, y, z).getBlock().setType(Material.GLASS);
                    }
                }
            }
        }
    }

    private void removeGlassCage() {
        if (cageLocation == null) return;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                for (int y = 0; y <= 3; y++) {
                    cageLocation.clone().add(x, y, z).getBlock().setType(Material.AIR);
                }
            }
        }
    }


    @EventHandler
    public void onLand(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.hasPotionEffect(PotionEffectType.SLOW_FALLING)) {
            if (p.getLocation().subtract(0, 0.1, 0).getBlock().getType().isSolid()) {
                p.removePotionEffect(PotionEffectType.SLOW_FALLING);
            }
        }

        if (isInCage) {
            if (e.getTo().getY() < 198 || e.getTo().getY() > 205) {
                e.getPlayer().teleport(cageLocation.clone().add(0.5, 1, 0.5));
            }
        }
    }

    @EventHandler public void onBreak(BlockBreakEvent e) { if (isInCage) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (isInCage) e.setCancelled(true); }
    @EventHandler public void onDamage(EntityDamageEvent e) { if (isInCage && e.getEntity() instanceof Player) e.setCancelled(true); }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) return Arrays.asList("restart", "s");
        return new ArrayList<>();
    }
}
