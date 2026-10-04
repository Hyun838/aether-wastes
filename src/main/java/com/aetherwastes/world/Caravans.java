package com.aetherwastes.world;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.magic.Glyph;
import com.aetherwastes.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

/**
 * Блуждающие Караваны: странствующие торговцы продают редкие глифы и материалы Пустошей.
 * Караван, погибший в пути, оставляет разорённый лагерь с трофеями.
 */
@EventBusSubscriber(modid = AetherWastes.MODID)
public final class Caravans {
    private Caravans() {}

    @SubscribeEvent
    public static void onTrades(WandererTradesEvent event) {
        event.getGenericTrades().add((trader, rng) -> new MerchantOffer(new ItemCost(Items.EMERALD, 2),
                new ItemStack(ModItems.ETHER_SHARD.get(), 4), 8, 1, 0.05f));
        event.getGenericTrades().add((trader, rng) -> new MerchantOffer(new ItemCost(Items.EMERALD, 3),
                new ItemStack(ModItems.SALT.get(), 6), 8, 1, 0.05f));
        event.getGenericTrades().add((trader, rng) -> new MerchantOffer(new ItemCost(Items.EMERALD, 3),
                new ItemStack(ModItems.RESIN.get(), 4), 8, 1, 0.05f));
        event.getGenericTrades().add((trader, rng) -> new MerchantOffer(new ItemCost(Items.EMERALD, 5),
                new ItemStack(ModItems.BANDAGE.get(), 3), 6, 1, 0.05f));
        event.getRareTrades().add((trader, rng) -> {
            Glyph[] rare = {Glyph.WAVE, Glyph.RUNE, Glyph.SUMMON, Glyph.DELAY, Glyph.CHAIN, Glyph.QUIET, Glyph.ECHO};
            Glyph g = rare[rng.nextInt(rare.length)];
            return new MerchantOffer(new ItemCost(Items.EMERALD, 12 + rng.nextInt(8)), new ItemStack(ModItems.glyph(g)), 2, 5, 0.05f);
        });
        event.getRareTrades().add((trader, rng) -> new MerchantOffer(new ItemCost(Items.EMERALD, 10),
                new ItemStack(ModItems.STAR_IRON_INGOT.get(), 2), 3, 5, 0.05f));
        event.getRareTrades().add((trader, rng) -> new MerchantOffer(new ItemCost(Items.EMERALD, 8),
                new ItemStack(ModItems.SOUL_ESSENCE.get(), 2), 3, 5, 0.05f));
    }

    public static void onTraderDeath(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).canBeReplaced() || level.getBlockState(pos.below()).isAir()) return;
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(pos) instanceof ChestBlockEntity cb) {
            cb.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, AetherWastes.id("chests/caravan_camp")), level.random.nextLong());
        }
        BlockPos fire = pos.east();
        if (level.getBlockState(fire).canBeReplaced() && !level.getBlockState(fire.below()).isAir()) {
            level.setBlockAndUpdate(fire, Blocks.CAMPFIRE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
        }
    }
}
