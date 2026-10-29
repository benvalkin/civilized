package com.uncreated.civilized.core.dialogue.actions.server;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.networking.packets.RunDialogueAction;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ServerDialogueActions {

   private static final Map<ResourceLocation, IServerDialogueAction> ACTIONS = new HashMap<>();

   public static final int MINIMUM_FOOD_TO_GIVE = 4;

   public static final ResourceLocation GIVE_FOOD = register("give_food", new GiveFoodAction(MINIMUM_FOOD_TO_GIVE));

   private ServerDialogueActions() {
   }

   private static ResourceLocation register(String name, IServerDialogueAction action) {
      ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, name);
      ACTIONS.put(id, action);
      return id;
   }

   public static @Nullable IServerDialogueAction get(ResourceLocation id) {
      return ACTIONS.get(id);
   }

   public static void runOnServer(ResourceLocation actionId) {
      PacketDistributor.sendToServer(new RunDialogueAction(actionId));
   }
}
