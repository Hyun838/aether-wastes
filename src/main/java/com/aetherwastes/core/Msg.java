package com.aetherwastes.core;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Сообщения игроку. */
public final class Msg {
    private Msg() {}

    public static void bar(Player p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
    }

    public static void bar(Player p, ChatFormatting color, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    public static void chat(Player p, ChatFormatting color, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(color), false);
    }

    public static void title(ServerPlayer p, Component title, Component subtitle) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }
}
