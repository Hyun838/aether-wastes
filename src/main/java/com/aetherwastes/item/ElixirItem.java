package com.aetherwastes.item;

import com.aetherwastes.craft.Alchemy;
import com.aetherwastes.registry.ModComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/** Эликсир из трав. Эффекты неизвестны, пока их не попробуешь. Подсказка — в клиентском обработчике. */
public class ElixirItem extends Item {
    public ElixirItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer p) {
            List<String> herbs = stack.get(ModComponents.HERBS.get());
            if (herbs != null) Alchemy.drink(p, herbs, 1);
            if (!p.getAbilities().instabuild) {
                stack.shrink(1);
                if (stack.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!p.addItem(bottle)) p.drop(bottle, false);
            }
        }
        return stack;
    }
}
