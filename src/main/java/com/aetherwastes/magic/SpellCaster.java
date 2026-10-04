package com.aetherwastes.magic;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.Scheduler;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.craft.Traits;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.progression.Mastery;
import com.aetherwastes.progression.School;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.survival.PlayerStats;
import com.aetherwastes.threats.Pulse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

/**
 * Произнесение заклинаний: проверка знаний, оплата (Эфир, у Безмолвия — Ясность,
 * у Эха Пепельного Завета — Эссенция души), сила от источника школы и мастерства,
 * Шум, Отсрочка и Эхо.
 */
public final class SpellCaster {
    private SpellCaster() {}

    public static boolean cast(ServerPlayer p, SpellData spell) {
        PlayerData d = Data.get(p);
        ServerLevel level = p.serverLevel();
        List<School> schools = spell.schools();
        if (schools.isEmpty()) return false;

        for (School s : schools) {
            if (!d.knows(s.id())) {
                Msg.bar(p, ChatFormatting.RED, "message.aetherwastes.spell.unknown_school",
                        Component.translatable(s.key()));
                return false;
            }
        }
        if (spell.isHybrid() && d.era < 3 && !d.archivist) {
            Msg.bar(p, ChatFormatting.RED, "message.aetherwastes.spell.hybrid_locked");
            return false;
        }
        if (level.getGameTime() < d.silencedUntil) {
            Msg.bar(p, ChatFormatting.DARK_GRAY, "message.aetherwastes.spell.silenced");
            return false;
        }

        float pressure = EtherField.pressure(level, p.blockPosition());
        float cost = spell.baseCost();
        if (pressure <= EtherField.LOW) cost *= 2f;

        float etherCost = cost;
        float clarityCost = cost * 0.05f;
        if (spell.has(School.SILENCE)) {
            etherCost = cost * 0.6f;
            clarityCost = cost * 0.4f;
        }
        boolean needsSoul = spell.form() == Glyph.SUMMON && spell.has(School.ASH);

        boolean creative = p.getAbilities().instabuild;
        float vessel = PlayerStats.vessel(p);
        float clarity = PlayerStats.clarity(p);
        if (!creative) {
            if (vessel < etherCost) {
                Msg.bar(p, ChatFormatting.RED, "message.aetherwastes.spell.no_ether", Math.round(etherCost), Math.round(vessel));
                level.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.6f);
                return false;
            }
            if (spell.has(School.SILENCE) && clarity < clarityCost + 5f) {
                Msg.bar(p, ChatFormatting.RED, "message.aetherwastes.spell.no_clarity");
                return false;
            }
            if (needsSoul && !consumeOne(p, ModItems.SOUL_ESSENCE.get())) {
                Msg.bar(p, ChatFormatting.RED, "message.aetherwastes.spell.no_soul");
                return false;
            }
        }

        float power = power(p, d, spell);

        if (!creative) {
            PlayerStats.setVessel(p, vessel - etherCost);
            PlayerStats.setClarity(p, clarity - clarityCost);
        }

        float noise = cost * 0.5f * (spell.has(School.STONE) ? 3f : 1f);
        for (int i = 0; i < spell.count(Glyph.QUIET); i++) noise *= 0.3f;
        Pulse.addNoise(p, noise);

        int xp = Math.max(1, Math.round(cost / 6f));
        for (School s : schools) Mastery.add(p, s, xp);

        int delay = spell.count(Glyph.DELAY) > 0 ? 40 : 0;
        final float pw = power;
        if (delay > 0) {
            Msg.bar(p, ChatFormatting.LIGHT_PURPLE, "message.aetherwastes.spell.delayed");
            Scheduler.after(delay, () -> SpellEffects.execute(p, spell, pw));
        } else {
            SpellEffects.execute(p, spell, pw);
        }
        for (int i = 1; i <= spell.count(Glyph.ECHO); i++) {
            Scheduler.after(delay + 40 * i, () -> SpellEffects.execute(p, spell, pw * 0.6f));
        }

        if (!creative && spell.has(School.ROOT) && PlayerStats.vessel(p) < 10f) {
            witherPlants(level, p.blockPosition());
            Msg.bar(p, ChatFormatting.DARK_GREEN, "message.aetherwastes.spell.root_overspend");
        }
        Sync.stats(p);
        return true;
    }

    /** Итоговая сила заклинания. */
    public static float power(ServerPlayer p, PlayerData d, SpellData spell) {
        List<School> schools = spell.schools();
        float source = 0f, mastery = 0f;
        for (School s : schools) {
            source += sourceBonus(p, s);
            mastery += Mastery.powerBonus(d, s);
        }
        source /= schools.size();
        mastery /= schools.size();

        float power = spell.amplify() * source * mastery;
        if (schools.size() > 1 && School.conflict(schools.get(0), schools.get(1)) && !d.reconciled && !d.archivist) {
            power *= 0.75f;
        }
        if (WorldState.ENDING_HEAL.equals(WorldState.get(p.getServer()).ending)) power *= 0.7f;
        power *= 1f + 0.05f * Traits.count(p, Traits.CONDUCTIVE);
        return power;
    }

    /** Источник силы школы — то, что маг делает и где стоит. */
    public static float sourceBonus(ServerPlayer p, School s) {
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        return switch (s) {
            case FORGE -> nearHeat(level, pos, 5) ? 1.3f : 1f;
            case FLOW -> (p.isInWaterRainOrBubble() || countBlocks(level, pos, 3, st -> st.is(Blocks.WATER)) > 0) ? 1.3f : 1f;
            case ROOT -> countBlocks(level, pos, 4, st -> st.is(BlockTags.CROPS) || st.is(BlockTags.SAPLINGS)
                    || st.is(BlockTags.LEAVES) || st.is(BlockTags.FLOWERS)) >= 8 ? 1.3f : 1f;
            case SILENCE -> {
                float b = level.getMaxLocalRawBrightness(pos) < 5 ? 1.3f : 1f;
                if (PlayerStats.clarity(p) < 40f) b *= 1.2f;
                yield b;
            }
            case STONE -> pos.getY() < -32 ? 1.5f : pos.getY() < 0 ? 1.3f : 1f;
            case ASH -> countItem(p, ModItems.SOUL_ESSENCE.get()) >= 4 ? 1.3f : 1f;
            case STAR -> {
                boolean sky = level.canSeeSky(pos.above());
                if (!level.isDay() && sky) yield level.getMoonPhase() == 0 ? 1.75f : 1.5f;
                yield sky ? 0.6f : 0.4f;
            }
        };
    }

    public static boolean nearHeat(ServerLevel level, BlockPos pos, int r) {
        return countBlocks(level, pos, r, st -> st.is(Blocks.LAVA) || st.is(Blocks.FIRE) || st.is(ModBlocks.FORGE_HEARTH.get())
                || ((st.getBlock() instanceof AbstractFurnaceBlock || st.getBlock() instanceof CampfireBlock)
                && st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT))) > 0;
    }

    public static int countBlocks(ServerLevel level, BlockPos center, int r, java.util.function.Predicate<BlockState> test) {
        int n = 0;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -r; z <= r; z++) {
                    m.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    if (test.test(level.getBlockState(m))) n++;
                }
        return n;
    }

    public static int countItem(ServerPlayer p, net.minecraft.world.item.Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(item)) n += s.getCount();
        return n;
    }

    public static boolean consumeOne(ServerPlayer p, net.minecraft.world.item.Item item) {
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    private static void witherPlants(ServerLevel level, BlockPos center) {
        int done = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 1, 4))) {
            if (done >= 4) break;
            if (level.getBlockState(pos).is(Blocks.GRASS_BLOCK) && level.random.nextFloat() < 0.3f) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                done++;
            }
        }
    }
}
