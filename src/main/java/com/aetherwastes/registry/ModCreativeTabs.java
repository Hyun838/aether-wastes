package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AetherWastes.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aetherwastes"))
                    .icon(() -> new ItemStack(ModItems.ETHER_SHARD.get()))
                    .displayItems((params, out) -> {
                        for (var entry : ModItems.ITEMS.getEntries()) out.accept(entry.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
