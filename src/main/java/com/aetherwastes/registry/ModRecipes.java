package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.craft.ElixirRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AetherWastes.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ElixirRecipe>> ELIXIR =
            SERIALIZERS.register("elixir", () -> new SimpleCraftingRecipeSerializer<>(ElixirRecipe::new));

    private ModRecipes() {}
}
