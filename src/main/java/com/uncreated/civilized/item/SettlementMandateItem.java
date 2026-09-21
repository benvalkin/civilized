package com.uncreated.civilized.item;

import java.util.Optional;

import com.uncreated.civilized.core.building.Building;
import com.uncreated.civilized.core.building.BuildingScreenOpener;
import com.uncreated.civilized.core.building.ServerBuildingsStore;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class SettlementMandateItem extends Item {

   public SettlementMandateItem(Properties properties) {
      super(properties);
   }

   public InteractionResult useOn(UseOnContext context) {

      if (context.getLevel().isClientSide || !(context.getPlayer() instanceof ServerPlayer serverPlayer))
         return InteractionResult.PASS;

      BlockPos clicked = context.getClickedPos();
      BlockPos clickedAir = clicked.mutable().move(context.getClickedFace());

      Optional<Building> enclosingBuilding =
            ServerBuildingsStore.INSTANCE.findEnclosingBuilding(clickedAir, context.getLevel());
      if (enclosingBuilding.isPresent()) {

         // this used to open the old crop chooser menu for whichever building was clicked. That menu has become a tab
         // on the crop farm screen, so the building's own screen is opened instead
         BuildingScreenOpener.open(serverPlayer, enclosingBuilding.get());

         return InteractionResult.SUCCESS;
      }

      return InteractionResult.SUCCESS;
   }
}
