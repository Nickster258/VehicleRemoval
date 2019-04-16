package org.stonecipher;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class CartRemovalTask implements Listener {

    private List<Entry<Vehicle, Long>> cartPurgeQueue = new ArrayList();
    private final JavaPlugin plugin;
    private int delay; // In Milliseconds

    public CartRemovalTask(JavaPlugin plugin, int delay) {
        this.plugin = plugin;
        this.delay = delay * 1000;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChunkLoader(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Minecart) {
                Block blockBelow = entity.getLocation().getBlock();
                if (entity.getPassengers().size() == 0) {
                    if ((blockBelow.getType() == Material.RAIL) ||
                            (blockBelow.getType() == Material.DETECTOR_RAIL) ||
                            (blockBelow.getType() == Material.ACTIVATOR_RAIL) ||
                            (blockBelow.getType() == Material.POWERED_RAIL)) {
                        removeFromPurgeQueue((Vehicle) entity.getVehicle());
                    } else {
                        //this.plugin.getLogger().info("Unloading cart ON CHUNK LOAD since no rail associated.");
                        addVehicleToPurge((Vehicle) entity.getVehicle());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onCartMove(VehicleMoveEvent e) {
        if (e.getVehicle() instanceof Minecart) {
            Block blockBelow = e.getTo().getBlock();
            if ((blockBelow.getType() == Material.RAIL) ||
                    (blockBelow.getType() == Material.DETECTOR_RAIL) ||
                    (blockBelow.getType() == Material.ACTIVATOR_RAIL) ||
                    (blockBelow.getType() == Material.POWERED_RAIL)) {
                removeFromPurgeQueue(e.getVehicle());
            } else {
                addVehicleToPurge(e.getVehicle());
            }
        }
    }

    @EventHandler
    public void onPlayerEnterCart(VehicleEnterEvent e) {
        if ((e.getVehicle() instanceof Minecart) && (e.getEntered() instanceof Player)) {
            // this.plugin.getLogger().info("Entered cart, clearing from purge...");
            removeFromPurgeQueue(e.getVehicle());
        }
    }

    @EventHandler
    public void onEntityLeaveCart(VehicleExitEvent e) {
        if ((e.getVehicle() instanceof Minecart) && (e.getExited() instanceof Player)) {
            Block blockBelow = e.getVehicle().getLocation().getBlock();
            if ((blockBelow.getType() == Material.RAIL) ||
                    (blockBelow.getType() == Material.DETECTOR_RAIL) ||
                    (blockBelow.getType() == Material.ACTIVATOR_RAIL) ||
                    (blockBelow.getType() == Material.POWERED_RAIL)) {
                addVehicleToPurge(e.getVehicle());
                // this.plugin.getLogger().info("Exited cart, attempting purge...");
            }
        }
    }

    @EventHandler
    public void onCartCreationEvent(VehicleCreateEvent e) {
        if (e.getVehicle() instanceof Minecart) {
            Block blockBelow = e.getVehicle().getLocation().getBlock();
            if ((blockBelow.getType() != Material.RAIL) ||
                    (blockBelow.getType() != Material.DETECTOR_RAIL) ||
                    (blockBelow.getType() != Material.ACTIVATOR_RAIL) ||
                    (blockBelow.getType() != Material.POWERED_RAIL)) {
                addVehicleToPurge(e.getVehicle());
                // this.plugin.getLogger().info("Created cart, attempting purge...");
            }
        }
    }

    private void addVehicleToPurge(Vehicle v) {
        for (Entry<Vehicle, Long> tuple : cartPurgeQueue) {
            Vehicle vehicle = (Vehicle) tuple.getKey();
            if (vehicle.equals(v)) {
                return;
            }
        }
        cartPurgeQueue.add(new SimpleEntry(v, System.currentTimeMillis()));
        addPurgeTask();
    }

    private void addPurgeTask() {
        new BukkitRunnable() {
            //@Override
            public void run() {
                removeUnusedCarts();
            }
        }.runTaskLater(this.plugin, this.delay / 50 + 1);
        // important to have the extra tick here because otherwise it
        // would be seemingly random on whether or not the delay check
        // would pass
    }

    private void removeFromPurgeQueue(Vehicle v) {
        // this.plugin.getLogger().info("Count: " + cartPurgeQueue.size());
        List<Entry<Vehicle, Long>> toRemove = new ArrayList();
        for (Entry<Vehicle, Long> tuple : cartPurgeQueue) {
            Vehicle vehicle = (Vehicle) tuple.getKey();
            if (vehicle == v) {
                // this.plugin.getLogger().info("Found a thing to purge, adding it...");
                toRemove.add(tuple);
            }
        }
        cartPurgeQueue.removeAll(toRemove);
        // this.plugin.getLogger().info("Count: " + cartPurgeQueue.size());
    }

    private void removeUnusedCarts() {
        // this.plugin.getLogger().info("Count: " + cartPurgeQueue.size());
        List<Entry<Vehicle, Long>> toRemove = new ArrayList();
        for (Entry<Vehicle, Long> tuple : cartPurgeQueue) {
            Vehicle vehicle = (Vehicle) tuple.getKey();
            Long time = (Long) tuple.getValue();
            if ((time + delay < System.currentTimeMillis()) && vehicle.isEmpty()) {
                if (vehicle instanceof Minecart) {
                    Block blockBelow = vehicle.getLocation().getBlock();
                    if ((blockBelow.getType() != Material.RAIL) &&
                            (blockBelow.getType() != Material.DETECTOR_RAIL) &&
                            (blockBelow.getType() != Material.ACTIVATOR_RAIL) &&
                            (blockBelow.getType() != Material.POWERED_RAIL)) {
                        // this.plugin.getLogger().info("Found a thing to purge, adding it...");
                        vehicle.remove();
                        toRemove.add(tuple);
                    }
                } else {
                    // this.plugin.getLogger().info("Found a thing to purge, adding it...");
                    vehicle.remove();
                    toRemove.add(tuple);
                }
            }
        }
        cartPurgeQueue.removeAll(toRemove);
        // this.plugin.getLogger().info("Count: " + cartPurgeQueue.size());
    }
}