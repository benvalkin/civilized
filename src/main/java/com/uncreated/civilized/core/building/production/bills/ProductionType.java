package com.uncreated.civilized.core.building.production.bills;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import com.uncreated.civilized.core.building.production.RecipeProductionMachine;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeType;

@Getter
@Accessors(fluent = true)
public class ProductionType {

   private final ResourceLocation resourceLocation;
   private final RecipeType<?> recipeType;
   private final Supplier<RecipeProductionMachine<?>> createRecipeProductionMachine;
   private final Supplier<ItemFilters> createDefaultItemFilters;
   /** The kinds of item this type's recipes consume, one per "recipe slot", e.g. ingredients and fuel. */
   private final List<RecipeSlotType> recipeSlots;
   /** How many input slots wide the recipe grid is, e.g. 3 for crafting and 1 for cooking. */
   private final int inputGridWidth;
   private final int inputGridHeight;
   private final IProductionRecipeLookup recipeLookup;

   public ProductionType(
         ResourceLocation resourceLocation,
         RecipeType<?> recipeType,
         Supplier<RecipeProductionMachine<?>> createRecipeProductionMachine,
         Supplier<ItemFilters> createDefaultItemFilters,
         List<RecipeSlotType> recipeSlots,
         int inputGridWidth,
         int inputGridHeight,
         IProductionRecipeLookup recipeLookup) {
      this.resourceLocation = resourceLocation;
      this.recipeType = recipeType;
      this.createRecipeProductionMachine = createRecipeProductionMachine;
      this.createDefaultItemFilters = createDefaultItemFilters;
      this.recipeSlots = recipeSlots;
      this.inputGridWidth = inputGridWidth;
      this.inputGridHeight = inputGridHeight;
      this.recipeLookup = recipeLookup;
   }

   /** Filters that let everything through, one per recipe slot. */
   public ItemFilters createDefaultIngredientFilters() {
      return createDefaultItemFilters.get();
   }

   public int inputSlotCount() {
      return inputGridWidth * inputGridHeight;
   }

   public String name() {
      return resourceLocation.getPath();
   }

   public boolean is(ProductionType other) {
      return this.equals(other);
   }

   public String translationKey() {
      return resourceLocation.getNamespace() + ".production_type." + resourceLocation.getPath();
   }

   public MutableComponent translation() {
      return Component.translatableWithFallback(translationKey(), resourceLocation.getPath().replace("_", " "))
            .withColor(Colors.BUILDING_LIGHT);
   }

   public MutableComponent heading() {
      return Component
            .translatableWithFallback(translationKey() + ".heading", resourceLocation.getPath().replace("_", " "));
   }

   @Override
   public boolean equals(Object o) {
      if (o == null || getClass() != o.getClass())
         return false;
      ProductionType that = (ProductionType) o;
      return Objects.equals(resourceLocation, that.resourceLocation);
   }

   @Override
   public int hashCode() {
      return Objects.hashCode(resourceLocation);
   }

   @Override
   public String toString() {
      return resourceLocation.toString();
   }
}
