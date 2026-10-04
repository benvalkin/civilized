package com.uncreated.civilized.ui.menu.building;

import com.uncreated.civilized.ui.context.BuildingScreenContext;

import net.minecraft.client.Minecraft;

public final class AlterBuildingPermissions {

   private AlterBuildingPermissions() {
   }

   public static boolean hasPermission(BuildingScreenContext context) {
      return Minecraft.getInstance().player != null
            && context.permissions().hasCreateBuildingsPermission(Minecraft.getInstance().player.getUUID());
   }
}
