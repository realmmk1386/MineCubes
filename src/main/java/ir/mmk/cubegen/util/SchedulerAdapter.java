package ir.mmk.cubegen.util;

import ir.mmk.cubegen.CubeGen;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Universal scheduler adapter supporting both Folia (region-threaded 26.x)
 * and legacy Spigot/Paper single-thread event loops.
 */
public class SchedulerAdapter {

    private final CubeGen plugin;
    private final boolean isFolia;

    public SchedulerAdapter(CubeGen plugin) {
        this.plugin = plugin;
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            folia = false;
        }
        this.isFolia = folia;
    }

    public boolean isFolia() {
        return isFolia;
    }

    public void runTask(Runnable runnable) {
        if (isFolia) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runMethod = globalScheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, Consumer.class);
                Consumer<Object> consumer = task -> runnable.run();
                runMethod.invoke(globalScheduler, plugin, consumer);
                return;
            } catch (Exception e) {
                // fallback
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public void runTaskAtLocation(Location loc, Runnable runnable) {
        if (loc == null || loc.getWorld() == null) {
            runTask(runnable);
            return;
        }

        if (isFolia) {
            try {
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                Method executeMethod = regionScheduler.getClass().getMethod("execute", org.bukkit.plugin.Plugin.class, Location.class, Runnable.class);
                executeMethod.invoke(regionScheduler, plugin, loc, runnable);
                return;
            } catch (Exception e) {
                // fallback
            }
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public TaskHandle runTimerAtLocation(Location loc, Runnable runnable, long delayTicks, long periodTicks) {
        if (loc == null || loc.getWorld() == null) {
            return runGlobalTimer(runnable, delayTicks, periodTicks);
        }

        if (isFolia) {
            try {
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                Method runAtFixedRateMethod = regionScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        org.bukkit.plugin.Plugin.class,
                        Location.class,
                        Consumer.class,
                        long.class,
                        long.class
                );
                Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = runAtFixedRateMethod.invoke(regionScheduler, plugin, loc, consumer, Math.max(1, delayTicks), Math.max(1, periodTicks));
                return new FoliaTaskHandle(scheduledTask);
            } catch (Exception e) {
                // fallback
            }
        }

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        return new BukkitTaskHandle(bukkitTask);
    }

    public TaskHandle runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (isFolia) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runAtFixedRateMethod = globalScheduler.getClass().getMethod(
                        "runAtFixedRate",
                        org.bukkit.plugin.Plugin.class,
                        Consumer.class,
                        long.class,
                        long.class
                );
                Consumer<Object> consumer = task -> runnable.run();
                Object scheduledTask = runAtFixedRateMethod.invoke(globalScheduler, plugin, consumer, Math.max(1, delayTicks), Math.max(1, periodTicks));
                return new FoliaTaskHandle(scheduledTask);
            } catch (Exception e) {
                // fallback
            }
        }

        BukkitTask bukkitTask = Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
        return new BukkitTaskHandle(bukkitTask);
    }

    public void runAsync(Runnable runnable) {
        if (isFolia) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runNowMethod = asyncScheduler.getClass().getMethod("runNow", org.bukkit.plugin.Plugin.class, Consumer.class);
                Consumer<Object> consumer = task -> runnable.run();
                runNowMethod.invoke(asyncScheduler, plugin, consumer);
                return;
            } catch (Exception e) {
                // fallback
            }
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    public interface TaskHandle {
        void cancel();
        boolean isCancelled();
    }

    private static class BukkitTaskHandle implements TaskHandle {
        private final BukkitTask task;
        public BukkitTaskHandle(BukkitTask task) { this.task = task; }
        @Override public void cancel() { if (task != null) task.cancel(); }
        @Override public boolean isCancelled() { return task == null || task.isCancelled(); }
    }

    private static class FoliaTaskHandle implements TaskHandle {
        private final Object task;
        public FoliaTaskHandle(Object task) { this.task = task; }
        @Override public void cancel() {
            if (task != null) {
                try {
                    task.getClass().getMethod("cancel").invoke(task);
                } catch (Exception ignored) {}
            }
        }
        @Override public boolean isCancelled() {
            if (task == null) return true;
            try {
                return (boolean) task.getClass().getMethod("isCancelled").invoke(task);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
