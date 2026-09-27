package com.uncreated.civilized.item;

import com.uncreated.civilized.client.BuildingDeedClientHandler;
import com.uncreated.civilized.core.building.BuildingType;

import lombok.Getter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BuildingDeedItem extends Item {

   @Getter
   private final BuildingType buildingType;

   public BuildingDeedItem(Properties properties, BuildingType buildingType) {
      super(properties);
      this.buildingType = buildingType;
   }

   @Override
   public InteractionResult use(Level level, Player player, InteractionHand hand) {
      if (!level.isClientSide)
         return InteractionResult.PASS;

      return BuildingDeedClientHandler.use(buildingType, level, player);
   }
}
