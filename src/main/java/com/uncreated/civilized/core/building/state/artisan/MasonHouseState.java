package com.uncreated.civilized.core.building.state.artisan;

import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.common.Tags;

public class MasonHouseState extends ArtisanHouseState {
   public MasonHouseState(Building building) {
      super(building);
   }

   @Override
   protected List<ProductionType> getSupportedProductionTypes() {
      return List.of(ProductionTypes.CRAFTING, ProductionTypes.SMELTING);
   }

   @Override
   protected List<ProductionBill> getDefaultProductionBills(ProductionType productionType) {
      if (productionType.is(ProductionTypes.CRAFTING))
         return List.of(
               createDefaultBill(
                     "minecraft:stone_bricks",
                     ProductionTypes.CRAFTING,
                     List.of(
                           new ItemStack(Items.STONE),
                           new ItemStack(Items.STONE),
                           ItemStack.EMPTY,
                           new ItemStack(Items.STONE),
                           new ItemStack(Items.STONE),
                           ItemStack.EMPTY,
                           ItemStack.EMPTY,
                           ItemStack.EMPTY,
                           ItemStack.EMPTY),
                     new ItemStack(Items.STONE_BRICKS)),
               createDefaultBill(
                     "minecraft:polished_andesite",
                     ProductionTypes.CRAFTING,
                     List.of(
                           new ItemStack(Items.ANDESITE),
                           new ItemStack(Items.ANDESITE),
                           ItemStack.EMPTY,
                           new ItemStack(Items.ANDESITE),
                           new ItemStack(Items.ANDESITE),
                           ItemStack.EMPTY,
                           ItemStack.EMPTY,
                           ItemStack.EMPTY,
                           ItemStack.EMPTY),
                     new ItemStack(Items.POLISHED_ANDESITE)));
      if (productionType.is(ProductionTypes.SMELTING))
         return List.of(
               createDefaultBill(
                     "minecraft:stone",
                     ProductionTypes.SMELTING,
                     List.of(new ItemStack(Items.COBBLESTONE)),
                     new ItemStack(Items.STONE)),
               createDefaultBill(
                     "minecraft:brick",
                     ProductionTypes.SMELTING,
                     List.of(new ItemStack(Items.CLAY_BALL)),
                     new ItemStack(Items.BRICK)),
               createDefaultBill(
                     "minecraft:glass",
                     ProductionTypes.SMELTING,
                     List.of(new ItemStack(Items.SAND)),
                     new ItemStack(Items.GLASS)));
      return List.of();
   }

   private static final List<TagKey<Item>> ALLOWED_INGREDIENT_TAGS =
         List.of(Tags.Items.BRICKS, Tags.Items.COBBLESTONES, Tags.Items.STONES, Tags.Items.SANDS);
   private static final List<Item> ALLOWED_INGREDIENT_ITEMS = List.of(Items.CLAY, Items.CLAY_BALL);
   private static final List<TagKey<Item>> FORBIDDEN_OUTPUT_TAGS = List.of(Tags.Items.TOOLS);

   @Override
   public boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level) {
      return (recipeHasAtLeastOneIngredientWithMatchingTag(recipeInput, ALLOWED_INGREDIENT_TAGS)
            || itemIsOneOf(resultItem, ALLOWED_INGREDIENT_ITEMS))
            && itemDoesNotHaveMatchingTag(resultItem, FORBIDDEN_OUTPUT_TAGS);
   }

   @Override
   public Component getAllowedRecipeHelp() {
      return Component.translatable(
                  "menu.building.residence.production_bills.edit_recipe.tooltip.allowed_recipe_help.mason");
   }
}
