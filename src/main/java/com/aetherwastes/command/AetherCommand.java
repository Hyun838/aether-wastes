package com.aetherwastes.command;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.item.JournalItem;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.progression.School;
import com.aetherwastes.survival.Mutations;
import com.aetherwastes.survival.PlayerStats;
import com.aetherwastes.threats.Wanderers;
import com.aetherwastes.world.HeartManager;
import com.aetherwastes.world.Underside;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Arrays;

/**
 * /aether status | ending &lt;heal|absorb|seal&gt;
 * Для операторов (тесты): era, school, insights, vessel, clarity, mutation, wanderer, underside, tide.
 */
@EventBusSubscriber(modid = AetherWastes.MODID)
public final class AetherCommand {
    private AetherCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("aether")
                .then(Commands.literal("status").executes(ctx -> {
                    JournalItem.show(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("ending")
                        .then(Commands.argument("choice", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[]{"heal", "absorb", "seal"}, b))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    String choice = StringArgumentType.getString(ctx, "choice");
                                    if (!Arrays.asList("heal", "absorb", "seal").contains(choice)) return 0;
                                    String err = HeartManager.choose(p, choice);
                                    if (err != null) {
                                        p.displayClientMessage(Component.translatable(err).withStyle(ChatFormatting.RED), false);
                                        return 0;
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("era").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 5)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            int n = IntegerArgumentType.getInteger(ctx, "n");
                            PlayerData pd = Data.get(p);
                            if (n > pd.era) EraManager.advance(p, n);
                            else {
                                pd.era = n;
                                Sync.all(p);
                            }
                            return 1;
                        })))
                .then(Commands.literal("school").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(School.values()).map(School::id), b))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    School s = School.byId(StringArgumentType.getString(ctx, "id"));
                                    if (s == null) return 0;
                                    Data.get(p).schools.add(s.id());
                                    Sync.journal(p);
                                    return 1;
                                })))
                .then(Commands.literal("vessel").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("v", FloatArgumentType.floatArg(0, 120)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            PlayerStats.setVessel(p, FloatArgumentType.getFloat(ctx, "v"));
                            Sync.stats(p);
                            return 1;
                        })))
                .then(Commands.literal("clarity").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("v", FloatArgumentType.floatArg(0, 100)).executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            PlayerStats.setClarity(p, FloatArgumentType.getFloat(ctx, "v"));
                            Sync.stats(p);
                            return 1;
                        })))
                .then(Commands.literal("mutation").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Mutations.gainRandom(p, Data.get(p));
                    return 1;
                }))
                .then(Commands.literal("wanderer").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    String v = EraManager.wandererFor(Data.get(p));
                    Wanderers.spawnNear(p, v == null ? EraManager.SPARK : v, 8);
                    return 1;
                }))
                .then(Commands.literal("underside").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    Underside.travel(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("tide").requires(s -> s.hasPermission(2)).executes(ctx -> {
                    var level = ctx.getSource().getLevel();
                    long day = level.getDayTime() / 24000L;
                    long target = (day - day % 7 + 5) * 24000L + 14000L;
                    if (target < level.getDayTime()) target += 7 * 24000L;
                    level.setDayTime(target);
                    ctx.getSource().sendSuccess(() -> Component.literal("Tide: " + EtherField.isTide(level)), false);
                    return 1;
                }))
        );
    }
}
