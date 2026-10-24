package com.uncreated.civilized.ui.menu.villager.events;

import com.uncreated.civilized.CivilizedMod;

import com.uncreated.civilized.ui.menu.villager.VillagerScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** Hides the crosshair while talking to a villager. */
@EventBusSubscriber(value = Dist.CLIENT, modid = CivilizedMod.CIVILIZED_MOD_ID)
public final class VillagerScreenHudEvents {

   private VillagerScreenHudEvents() {
   }

   @SubscribeEvent
   public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
      if (event.getName().equals(VanillaGuiLayers.CROSSHAIR) && Minecraft.getInstance().screen instanceof VillagerScreen)
         event.setCanceled(true);
   }
}
