package com.aetherwastes.craft;

import com.aetherwastes.block.HolderBlockEntity;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.survival.Mutations;
import com.aetherwastes.threats.Scar;
import com.aetherwastes.threats.Wanderers;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Ритуальные круги. Фокус в центре, 4 Постамента в 2 блоках по сторонам света.
 * Неверный узор выпускает Эфир мини-бурей. Ночь и Прилив усиливают результат.
 */
public final class Rituals {
    private Rituals() {}

    private record Recipe(String id, Item focus, List<Item> pedestals, boolean anyDamageableFocus) {}

    private static List<Recipe> recipes() {
        return List.of(
                new Recipe("reconciliation", ModItems.ETHER_SHARD.get(),
                        List.of(ModItems.SALT.get(), ModItems.RESIN.get(), ModItems.SOUL_ESSENCE.get(), ModItems.CHARGED_SHARD.get()), false),
                new Recipe("purification", Items.GOLDEN_APPLE,
                        List.of(ModItems.SALT.get(), ModItems.SALT.get(), ModItems.RESIN.get(), ModItems.ETHER_SHARD.get()), false),
                new Recipe("underside_portal", ModItems.ANCHOR_CORE.get(),
                        List.of(ModItems.STAR_IRON_INGOT.get(), ModItems.CHARGED_SHARD.get(), ModItems.SOUL_ESSENCE.get(), ModItems.WANDERER_SHARD.get()), false),
                new Recipe("call_wanderer", ModItems.SOUL_ESSENCE.get(),
                        List.of(ModItems.ETHER_SHARD.get(), ModItems.ETHER_SHARD.get(), ModItems.ETHER_SHARD.get(), ModItems.ETHER_SHARD.get()), false),
                new Recipe("relic", Items.AIR,
                        List.of(ModItems.STAR_IRON_INGOT.get(), ModItems.STAR_IRON_INGOT.get(), ModItems.CHARGED_SHARD.get(), ModItems.CHARGED_SHARD.get()), true),
                new Recipe("echo_porter", ModItems.SOUL_ESSENCE.get(),
                        List.of(Items.AMETHYST_SHARD, ModItems.ETHER_SHARD.get(), ModItems.RESIN.get(), ModItems.SALT.get()), false),
                new Recipe("purifying_salt", ModItems.SALT.get(),
                        List.of(Items.QUARTZ, Items.QUARTZ, ModItems.ETHER_SHARD.get(), ModItems.ETHER_SHARD.get()), false)
        );
    }

    public static void perform(ServerPlayer p, BlockPos focusPos) {
        ServerLevel level = p.serverLevel();
        if (!(level.getBlockEntity(focusPos) instanceof HolderBlockEntity focus)) return;

        List<HolderBlockEntity> pedestals = new ArrayList<>();
        for (BlockPos off : new BlockPos[]{focusPos.north(2), focusPos.south(2), focusPos.east(2), focusPos.west(2)}) {
            if (level.getBlockState(off).is(ModBlocks.RITUAL_PEDESTAL.get()) && level.getBlockEntity(off) instanceof HolderBlockEntity pe) {
                pedestals.add(pe);
            }
        }
        if (pedestals.size() < 4) {
            Msg.bar(p, ChatFormatting.RED, "ritual.aetherwastes.need_pedestals");
            return;
        }
        List<Item> placed = new ArrayList<>();
        for (HolderBlockEntity pe : pedestals) placed.add(pe.item().getItem());

        Recipe match = null;
        for (Recipe r : recipes()) {
            boolean focusOk = r.anyDamageableFocus ? focus.item().isDamageableItem() : focus.item().is(r.focus);
            if (focusOk && sameMultiset(placed, r.pedestals)) {
                match = r;
                break;
            }
        }
        if (match == null) {
            fail(p, level, focusPos);
            return;
        }
        if (!run(p, level, focusPos, focus, match)) return;

        for (HolderBlockEntity pe : pedestals) {
            Vec3 c = Vec3.atCenterOf(pe.getBlockPos());
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 0.7, c.z, 15, 0.2, 0.3, 0.2, 0.02);
            pe.setItem(ItemStack.EMPTY);
        }
        Vec3 c = Vec3.atCenterOf(focusPos);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 1, c.z, 60, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, focusPos, com.aetherwastes.registry.ModSounds.RITUAL.get(), SoundSource.BLOCKS, 1.5f, 1f);
        Msg.chat(p, ChatFormatting.LIGHT_PURPLE, "ritual.aetherwastes.done." + match.id);
        Sync.journal(p);
    }

    /** Выполнить ритуал. false — условия не выполнены, ничего не тратится. */
    private static boolean run(ServerPlayer p, ServerLevel level, BlockPos pos, HolderBlockEntity focus, Recipe r) {
        PlayerData d = Data.get(p);
        boolean night = !level.isDay();
        boolean tide = EtherField.isTide(level);
        switch (r.id) {
            case "reconciliation" -> {
                d.reconciled = true;
                focus.setItem(ItemStack.EMPTY);
            }
            case "purification" -> {
                String removed = Mutations.removeLast(p, d);
                d.traumas.clear();
                if (removed != null) {
                    Msg.chat(p, ChatFormatting.GREEN, "ritual.aetherwastes.mutation_removed",
                            Component.translatable("mutation.aetherwastes." + removed));
                }
                focus.setItem(ItemStack.EMPTY);
            }
            case "underside_portal" -> {
                if (d.era < 3) {
                    Msg.bar(p, ChatFormatting.RED, "ritual.aetherwastes.need_era", 3);
                    return false;
                }
                focus.setItem(new ItemStack(ModItems.UNDERSIDE_PORTAL.get()));
            }
            case "call_wanderer" -> {
                String variant = EraManager.wandererFor(d);
                if (variant == null) {
                    Msg.bar(p, ChatFormatting.GRAY, "ritual.aetherwastes.no_wanderer");
                    return false;
                }
                focus.setItem(ItemStack.EMPTY);
                Wanderers.spawnNear(p, variant, 12);
            }
            case "relic" -> {
                if (!night) {
                    Msg.bar(p, ChatFormatting.RED, "ritual.aetherwastes.need_night");
                    return false;
                }
                ItemStack item = focus.item().copy();
                Quality.set(item, Quality.MASTER);
                Traits.add(item, Traits.random(level.random));
                if (tide) Traits.add(item, Traits.random(level.random));
                focus.setItem(item);
            }
            case "echo_porter" -> {
                Allay allay = EntityType.ALLAY.create(level);
                if (allay != null) {
                    allay.moveTo(pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0, 0);
                    allay.setCustomName(Component.translatable("entity.aetherwastes.echo_porter").withStyle(ChatFormatting.AQUA));
                    allay.setPersistenceRequired();
                    level.addFreshEntity(allay);
                }
                focus.setItem(ItemStack.EMPTY);
                Scar.seedNear(level, pos, 10);
            }
            case "purifying_salt" -> focus.setItem(new ItemStack(ModItems.PURIFYING_OBELISK.get()));
            default -> {
                return false;
            }
        }
        return true;
    }

    private static void fail(ServerPlayer p, ServerLevel level, BlockPos pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(pos.above()));
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        p.hurt(p.damageSources().magic(), 4f);
        level.sendParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 80, 2, 1, 2, 0.1);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS, 0.8f, 1.4f);
        if (level.random.nextFloat() < 0.3f) Scar.seedNear(level, pos, 6);
        Msg.chat(p, ChatFormatting.DARK_PURPLE, "ritual.aetherwastes.failed");
    }

    private static boolean sameMultiset(List<Item> a, List<Item> b) {
        if (a.size() != b.size()) return false;
        List<Item> copy = new ArrayList<>(b);
        for (Item i : a) if (!copy.remove(i)) return false;
        return copy.isEmpty();
    }
}
