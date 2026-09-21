package com.uncreated.civilized.core.building.production.bills;

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
   private final IEditRecipeProductionMenuSupplier menuSupplier;

   public ProductionType(
         ResourceLocation resourceLocation,
         RecipeType<?> recipeType,
         Supplier<RecipeProductionMachine<?>> createRecipeProductionMachine,
         IEditRecipeProductionMenuSupplier menuSupplier) {
      this.resourceLocation = resourceLocation;
      this.recipeType = recipeType;
      this.createRecipeProductionMachine = createRecipeProductionMachine;
      this.menuSupplier = menuSupplier;
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
