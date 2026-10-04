package com.aetherwastes.threats;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.Insights;
import com.aetherwastes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Немезида: моб, убивший игрока, получает имя, растёт в силе, запоминает, чем его ранили,
 * и возвращается за игроком. Победа даёт трофей и Эхо Немезиды.
 */
public final class Nemesis {
    public static final String TAG = "aw_nemesis";
    public static final String OF = "aw_nemesis_of";
    public static final String LAST_CAT = "aw_last_cat";

    private static final String[] NAMES = {"Грул", "Морх", "Скарн", "Вельд", "Тракс", "Ург", "Шорн", "Дрег", "Зорт", "Ракх"};
    private static final String[] EPITHETS = {"Пожиратель", "Ломатель костей", "Тень Пустошей", "Терпеливый",
            "Неумолимый", "Пепельный", "Хромой", "Шепчущий", "Кровавый", "Стеклянный клык"};
    private static final ResourceLocation LEVEL_ID = AetherWastes.id("nemesis_level");

    private Nemesis() {}

    public static void onPlayerKilled(ServerPlayer p, Mob killer) {
        if (!AetherConfig.NEMESIS.get() || killer.getTags().contains("aw_temp")) return;
        PlayerData d = Data.get(p);
        CompoundTag pd = killer.getPersistentData();
        int level;
        String name;
        if (p.getStringUUID().equals(pd.getString(OF))) {
            level = pd.getInt("aw_nemesis_level") + 1;
            name = pd.getString("aw_nemesis_name");
        } else {
            level = 1;
            name = NAMES[killer.getRandom().nextInt(NAMES.length)] + " " + EPITHETS[killer.getRandom().nextInt(EPITHETS.length)];
        }
        String resist = pd.contains(LAST_CAT) ? pd.getString(LAST_CAT) : "melee";
        mark(killer, p, name, level, resist);
        killer.heal(killer.getMaxHealth());

        CompoundTag n = new CompoundTag();
        n.putString("type", BuiltInRegistries.ENTITY_TYPE.getKey(killer.getType()).toString());
        n.putString("name", name);
        n.putInt("level", level);
        n.putString("resist", resist);
        d.nemesis = n;
        Msg.chat(p, ChatFormatting.DARK_RED, "nemesis.aetherwastes.born", name, level);
        Sync.journal(p);
    }

    private static void mark(Mob mob, ServerPlayer p, String name, int level, String resist) {
        mob.addTag(TAG);
        CompoundTag pd = mob.getPersistentData();
        pd.putString(OF, p.getStringUUID());
        pd.putString("aw_nemesis_name", name);
        pd.putInt("aw_nemesis_level", level);
        pd.putString(Hunters.RESIST, resist);
        mob.setPersistenceRequired();
        mob.setCustomName(Component.literal(name + " [" + level + "]").withStyle(ChatFormatting.DARK_RED));
        mob.setCustomNameVisible(true);
        AttributeInstance hp = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(LEVEL_ID);
            hp.addPermanentModifier(new AttributeModifier(LEVEL_ID, 10.0 * level, AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance dmg = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmg != null) {
            dmg.removeModifier(LEVEL_ID);
            dmg.addPermanentModifier(new AttributeModifier(LEVEL_ID, 1.5 * level, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    /** Немезида возвращается ночью, если её нет рядом. */
    public static void tickReturn(ServerPlayer p, PlayerData d) {
        if (!AetherConfig.NEMESIS.get() || !d.nemesis.contains("type")) return;
        if (p.level().dimension() != Level.OVERWORLD || p.level().isDay()) return;
        long now = p.level().getGameTime();
        if (now - d.lastNemesisReturn < 24000L || p.getRandom().nextFloat() > 0.25f) return;
        boolean present = !p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(96),
                m -> m.getTags().contains(TAG) && p.getStringUUID().equals(m.getPersistentData().getString(OF))).isEmpty();
        if (present) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(d.nemesis.getString("type")));
        if (!(type.create(p.level()) instanceof Mob)) return;
        @SuppressWarnings("unchecked")
        EntityType<? extends Mob> mobType = (EntityType<? extends Mob>) type;
        Mob mob = Pulse.spawnHostile(p.serverLevel(), p, mobType, 22, "aw_spawned");
        if (mob == null) return;
        mark(mob, p, d.nemesis.getString("name"), d.nemesis.getInt("level"), d.nemesis.getString("resist"));
        mob.heal(mob.getMaxHealth());
        d.lastNemesisReturn = now;
        Msg.chat(p, ChatFormatting.DARK_RED, "nemesis.aetherwastes.returns", d.nemesis.getString("name"));
        Pulse.sound(p);
    }

    public static void onKilledBy(ServerPlayer p, Mob mob) {
        if (!mob.getTags().contains(TAG)) return;
        CompoundTag pd = mob.getPersistentData();
        String name = pd.getString("aw_nemesis_name");
        mob.spawnAtLocation(new ItemStack(ModItems.NEMESIS_TROPHY.get()));
        ItemStack echo = new ItemStack(ModItems.NEMESIS_ECHO.get());
        echo.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(name));
        mob.spawnAtLocation(echo);
        PlayerData d = Data.get(p);
        if (p.getStringUUID().equals(pd.getString(OF))) {
            d.nemesis = new CompoundTag();
            Msg.chat(p, ChatFormatting.GOLD, "nemesis.aetherwastes.slain", name);
            Insights.add(p, "creature", "nemesis_" + name);
            Sync.journal(p);
        }
    }
}
