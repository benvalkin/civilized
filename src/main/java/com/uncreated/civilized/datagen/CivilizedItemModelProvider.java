package com.uncreated.civilized.datagen;

import java.util.Map;

import com.uncreated.civilized.CivilizedMod;
import com.uncreated.civilized.core.building.BuildingType;
import com.uncreated.civilized.core.building.BuildingTypes;
import com.uncreated.civilized.neoforge.registration.ItemRegistry;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Registry;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.registries.DeferredItem;

@OnlyIn(Dist.CLIENT)
public class CivilizedItemModelProvider extends ModelProvider {

   public CivilizedItemModelProvider(PackOutput output) {
      super(output, CivilizedMod.CIVILIZED_MOD_ID);
   }

   @Override
   protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {

      // Generate models and associated files here
      for (Map.Entry<BuildingType, DeferredItem<Item>> item : ItemRegistry.BUILDING_DEEDS.entrySet()) {
         itemModels.generateFlatItem(item.getValue().get(), ModelTemplates.FLAT_ITEM);
      }

      itemModels.generateFlatItem(ItemRegistry.COIN.get(), ModelTemplates.FLAT_ITEM);
      itemModels.generateFlatItem(ItemRegistry.COIN_STACK.get(), ModelTemplates.FLAT_ITEM);
      itemModels.generateFlatItem(ItemRegistry.SETTLEMENT_MANDATE.get(), ModelTemplates.FLAT_ITEM);
   }
}
