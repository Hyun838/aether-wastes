package com.aetherwastes.craft;

import com.aetherwastes.registry.ModComponents;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** Эликсир: стеклянный пузырёк + Осколок Эфира + 1–3 разные травы (в любом порядке). */
public class ElixirRecipe extends CustomRecipe {
    public ElixirRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return result(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        List<String> herbs = result(input);
        if (herbs == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(ModItems.ELIXIR.get());
        out.set(ModComponents.HERBS.get(), List.copyOf(herbs));
        return out;
    }

    private static List<String> result(CraftingInput input) {
        int bottles = 0, shards = 0;
        List<String> herbs = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(Items.GLASS_BOTTLE)) bottles++;
            else if (s.is(ModItems.ETHER_SHARD.get())) shards++;
            else if (Alchemy.isHerb(s.getItem())) {
                String id = Alchemy.herbId(s.getItem());
                if (herbs.contains(id)) return null;
                herbs.add(id);
            } else return null;
        }
        if (bottles != 1 || shards != 1 || herbs.isEmpty() || herbs.size() > 3) return null;
        return herbs;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ELIXIR.get();
    }
}
