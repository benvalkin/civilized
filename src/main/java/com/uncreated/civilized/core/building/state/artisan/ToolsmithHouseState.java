package com.uncreated.civilized.core.building.state.artisan;

import java.util.List;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.production.bills.ProductionBill;
import com.uncreated.civilized.core.building.production.bills.ProductionType;
import com.uncreated.civilized.core.building.production.bills.ProductionTypes;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.common.Tags;

public class ToolsmithHouseState extends ArtisanHouseState {
   public ToolsmithHouseState(Building building) {
      super(building);
   }

   @Override
   protected List<ProductionType> getSupportedProductionTypes() {
      return List.of(ProductionTypes.CRAFTING);
   }

   @Override
   protected List<ProductionBill> getDefaultProductionBills(ProductionType productionType) {
      if (productionType.is(ProductionTypes.CRAFTING))
         return List.of(
               createDefaultBill(
                     "minecraft:stone_axe",
                     ProductionTypes.CRAFTING,
                     List.of(
                           new ItemStack(Items.COBBLESTONE),
                           new ItemStack(Items.COBBLESTONE),
                           ItemStack.EMPTY,
                           new ItemStack(Items.COBBLESTONE),
                           new ItemStack(Items.STICK),
                           ItemStack.EMPTY,
                           ItemStack.EMPTY,
                           new ItemStack(Items.STICK),
                           ItemStack.EMPTY),
                     new ItemStack(Items.STONE_AXE)),
               createDefaultBill(
                     "minecraft:stone_pickaxe",
                     ProductionTypes.CRAFTING,
                     List.of(
                           new ItemStack(Items.COBBLESTONE),
                           new ItemStack(Items.COBBLESTONE),
                           new ItemStack(Items.COBBLESTONE),
                           ItemStack.EMPTY,
                           new ItemStack(Items.STICK),
                           ItemStack.EMPTY,
                           ItemStack.EMPTY,
                           new ItemStack(Items.STICK),
                           ItemStack.EMPTY),
                     new ItemStack(Items.STONE_PICKAXE)));
      return List.of();
   }

   private static final List<TagKey<Item>> ALLOWED_OUTPUT_TAGS = List.of(Tags.Items.TOOLS);
   private static final List<TagKey<Item>> FORBIDDEN_OUTPUT_TAGS =
         List.of(
               ItemTags.SWORDS,
               Tags.Items.TOOLS_BOW,
               Tags.Items.TOOLS_CROSSBOW,
               Tags.Items.TOOLS_SHIELD,
               Tags.Items.TOOLS_MACE);

   @Override
   public boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level) {
      return itemHasMatchingTag(resultItem, ALLOWED_OUTPUT_TAGS)
            && !itemHasMatchingTag(resultItem, FORBIDDEN_OUTPUT_TAGS);
   }

   @Override
   public Component getAllowedRecipeHelp() {
      return Component.translatable(
                  "menu.building.residence.production_bills.edit_recipe.tooltip.allowed_recipe_help.toolsmith");
   }
}
