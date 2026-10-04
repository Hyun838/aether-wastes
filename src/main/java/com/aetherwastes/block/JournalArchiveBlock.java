package com.aetherwastes.block;

import com.aetherwastes.base.Devices;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashSet;
import java.util.Set;

/** Хранилище Журнала: общая библиотека Озарений и трав для всех, кто живёт у Якоря. */
public class JournalArchiveBlock extends BaseEntityBlock {
    public static final MapCodec<JournalArchiveBlock> CODEC = simpleCodec(JournalArchiveBlock::new);

    public JournalArchiveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!oldState.is(this)) Devices.placed(level, pos, Devices.ARCHIVE);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(this)) Devices.removed(level, pos);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof Entity be)) return InteractionResult.SUCCESS;
        ServerPlayer p = (ServerPlayer) player;
        PlayerData d = Data.get(p);
        int before = d.insights.size() + d.knownHerbs.size();
        be.insights.addAll(d.insights);
        be.herbs.addAll(d.knownHerbs);
        d.insights.addAll(be.insights);
        d.knownHerbs.addAll(be.herbs);
        int gained = d.insights.size() + d.knownHerbs.size() - before;
        be.setChanged();
        level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable("archive.aetherwastes.synced", gained, be.insights.size())
                .withStyle(ChatFormatting.AQUA), false);
        EraManager.check(p);
        Sync.journal(p);
        return InteractionResult.SUCCESS;
    }

    public static class Entity extends BlockEntity {
        final Set<String> insights = new HashSet<>();
        final Set<String> herbs = new HashSet<>();

        public Entity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.JOURNAL_ARCHIVE.get(), pos, state);
        }

        @Override
        public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            ListTag a = new ListTag();
            for (String s : insights) a.add(StringTag.valueOf(s));
            tag.put("insights", a);
            ListTag b = new ListTag();
            for (String s : herbs) b.add(StringTag.valueOf(s));
            tag.put("herbs", b);
        }

        @Override
        public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            insights.clear();
            herbs.clear();
            ListTag a = tag.getList("insights", Tag.TAG_STRING);
            for (int i = 0; i < a.size(); i++) insights.add(a.getString(i));
            ListTag b = tag.getList("herbs", Tag.TAG_STRING);
            for (int i = 0; i < b.size(); i++) herbs.add(b.getString(i));
        }
    }
}
