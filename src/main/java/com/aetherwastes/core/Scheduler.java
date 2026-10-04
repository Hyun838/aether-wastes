package com.aetherwastes.core;

import com.aetherwastes.AetherWastes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Отложенные действия (Отсрочка, Эхо, предупреждения Скитальцев). Живут только пока сервер запущен. */
@EventBusSubscriber(modid = AetherWastes.MODID)
public final class Scheduler {
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private Scheduler() {}

    public static void after(int ticks, Runnable action) {
        synchronized (PENDING) {
            PENDING.add(new Task(ticks, action));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        synchronized (PENDING) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        Iterator<Task> it = TASKS.iterator();
        List<Runnable> due = new ArrayList<>();
        while (it.hasNext()) {
            Task t = it.next();
            if (--t.ticks <= 0) {
                due.add(t.action);
                it.remove();
            }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (Exception e) {
                AetherWastes.LOGGER.error("Aether Wastes: ошибка отложенного действия", e);
            }
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        TASKS.clear();
        synchronized (PENDING) {
            PENDING.clear();
        }
    }

    private static final class Task {
        int ticks;
        final Runnable action;

        Task(int ticks, Runnable action) {
            this.ticks = ticks;
            this.action = action;
        }
    }
}
