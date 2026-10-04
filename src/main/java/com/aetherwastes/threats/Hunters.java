package com.aetherwastes.threats;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Охотник Пульса: если игрок полагается на одну тактику (70% убийств одним типом урона)
 * или охотится на одних и тех же мобов, приходит элитный враг с сопротивлением этой тактике.
 */
public final class Hunters {
    public static final String TAG = "aw_hunter";
    public static final String RESIST = "aw_resist";

    private Hunters() {}

    public static void tick(ServerPlayer p, PlayerData d) {
        if (d.killsSinceHunter < 30 || p.level().dimension() != Level.OVERWORLD || p.level().isDay()) return;
        long now = p.level().getGameTime();
        if (now - d.lastHunter < 24000L) return;
        int total = 0;
        for (int v : d.killCats.values()) total += v;
        String cat = Pulse.dominant(d.killCats, total, 0.7f);
        String type = Pulse.dominant(d.killTypes, total, 0.6f);
        if (cat == null && type == null) return;
        String resist = cat != null ? cat : "melee";

        Mob hunter = Pulse.spawnHostile(p.serverLevel(), p, d.era >= 4 ? EntityType.PILLAGER : EntityType.VINDICATOR, 20, TAG);
        if (hunter == null) return;
        hunter.getPersistentData().putString(RESIST, resist);
        hunter.addTag("aw_spawned");
        hunter.setPersistenceRequired();
        AttributeInstance hp = hunter.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.addPermanentModifier(new AttributeModifier(AetherWastes.id("hunter_health"), 20.0 * d.era,
                    AttributeModifier.Operation.ADD_VALUE));
            hunter.setHealth(hunter.getMaxHealth());
        }
        hunter.setCustomName(Component.translatable("entity.aetherwastes.hunter",
                Component.translatable("damage_cat.aetherwastes." + resist)).withStyle(ChatFormatting.RED));
        hunter.setCustomNameVisible(true);

        d.killCats.clear();
        d.killTypes.clear();
        d.killsSinceHunter = 0;
        d.lastHunter = now;
        Msg.chat(p, ChatFormatting.RED, "pulse.aetherwastes.hunter", Component.translatable("damage_cat.aetherwastes." + resist));
        Pulse.sound(p);
    }
}
