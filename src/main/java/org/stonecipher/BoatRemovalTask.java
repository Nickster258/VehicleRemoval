package org.stonecipher;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Chunk;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class BoatRemovalTask implements Listener {

    private List<Entry<Vehicle, Long>> boatPurgeQueue = new ArrayList();
    private final JavaPlugin plugin;
    private int delay; // In Milliseconds

    public BoatRemovalTask(JavaPlugin plugin, int delay) {
        this.plugin = plugin;
        this.delay = delay * 1000;
        this.plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onChunkLoader(ChunkLoadEvent e) {
        Chunk chunk = e.getChunk();
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Boat) {
                if (entity.getPassengers().size() == 0) {
                    entity.remove();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerEnterBoat(VehicleEnterEvent e) {
        if ((e.getVehicle() instanceof Boat) && (e.getEntered() instanceof Player)) {
            // this.plugin.getLogger().info("Entered boat, clearing from purge...");
            removeFromPurgeQueue(e.getVehicle());
        }
    }

    @EventHandler
    public void onEntityLeaveBoat(VehicleExitEvent e) {
        if ((e.getVehicle() instanceof Boat) && (e.getExited() instanceof Player)) {
            addVehicleToPurge(e.getVehicle());
            // this.plugin.getLogger().info("Exited boat, attempting purge...");
        }
    }

    @EventHandler
    public void onBoatCreationEvent(VehicleCreateEvent e) {
        if (e.getVehicle() instanceof Boat) {
            addVehicleToPurge(e.getVehicle());
            // this.plugin.getLogger().info("Created boat, attempting purge...");
        }
    }

    private void addVehicleToPurge(Vehicle v) {
        if (v == null) {
            return;
        }
        for (Entry<Vehicle, Long> tuple : boatPurgeQueue) {
            Vehicle vehicle = (Vehicle) tuple.getKey();
            if (vehicle.equals(v)) {
                return;
            }
        }
        boatPurgeQueue.add(new SimpleEntry(v, System.currentTimeMillis()));
        addPurgeTask();
    }

    private void addPurgeTask() {
        new BukkitRunnable() {
            //@Override
            public void run() {
                removeUnusedBoats();
            }
        }.runTaskLater(this.plugin, this.delay / 50 + 1);
        // important to have the extra tick here because otherwise it
        // would be seemingly random on whether or not the delay check
        // would pass
    }

    private void removeFromPurgeQueue(Vehicle v) {
        // this.plugin.getLogger().info("Count: " + boatPurgeQueue.size());
        List<Entry<Vehicle, Long>> toRemove = new ArrayList();
        for (Entry<Vehicle, Long> tuple : boatPurgeQueue) {
            Vehicle vehicle = (Vehicle) tuple.getKey();
            if (vehicle == v) {
                // this.plugin.getLogger().info("Found a thing to purge, adding it...");
                toRemove.add(tuple);
            }
        }
        boatPurgeQueue.removeAll(toRemove);
        // this.plugin.getLogger().info("Count: " + boatPurgeQueue.size());
    }

    private void removeUnusedBoats() {
        // this.plugin.getLogger().info("Count: " + boatPurgeQueue.size());
        List<Entry<Vehicle, Long>> toRemove = new ArrayList();
        for (Entry<Vehicle, Long> tuple : boatPurgeQueue) {
            Vehicle vehicle = (Vehicle) tuple.getKey();
            Long time = (Long) tuple.getValue();
            if ((time + delay < System.currentTimeMillis()) && vehicle.isEmpty()) {
                // this.plugin.getLogger().info("Found a thing to purge, adding it...");
                vehicle.remove();
                toRemove.add(tuple);
            }
        }
        boatPurgeQueue.removeAll(toRemove);
        // this.plugin.getLogger().info("Count: " + boatPurgeQueue.size());
    }
}
