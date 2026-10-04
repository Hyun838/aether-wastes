package com.aetherwastes.core;

import com.aetherwastes.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;

/** Короткий доступ к данным игрока. */
public final class Data {
    private Data() {}

    public static PlayerData get(Player player) {
        return player.getData(ModAttachments.PLAYER_DATA);
    }
}
