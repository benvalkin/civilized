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

public class WeaponsmithHouseState extends ArtisanHouseState {
   public WeaponsmithHouseState(Building building) {
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
                     "minecraft:iron_sword",
                     ProductionTypes.CRAFTING,
                     List.of(
                           ItemStack.EMPTY,
                           new ItemStack(Items.IRON_INGOT),
                           ItemStack.EMPTY,
                           ItemStack.EMPTY,
                           new ItemStack(Items.IRON_INGOT),
                           ItemStack.EMPTY,
                           ItemStack.EMPTY,
                           new ItemStack(Items.STICK),
                           ItemStack.EMPTY),
                     new ItemStack(Items.IRON_SWORD)));
      return List.of();
   }

   private static final List<TagKey<Item>> ALLOWED_OUTPUT_TAGS = List.of(Tags.Items.MELEE_WEAPON_TOOLS);

   @Override
   public boolean recipeAllowed(
         ProductionType productionType,
         RecipeInput recipeInput,
         ItemStack resultItem,
         ServerLevel level) {
      return itemHasMatchingTag(resultItem, ALLOWED_OUTPUT_TAGS);
   }

   @Override
   public Component getAllowedRecipeHelp() {
      return Component.translatable(
                  "menu.building.residence.production_bills.edit_recipe.tooltip.allowed_recipe_help.weaponsmith");
   }
}
