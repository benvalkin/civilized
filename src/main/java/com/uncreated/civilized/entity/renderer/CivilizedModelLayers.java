package com.uncreated.civilized.entity.renderer;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Adds slight tweaks to the villager's player model scale. Changes in this class are mostly related to fixing problems
 * with layered textures
 */
public final class CivilizedModelLayers {

   /** Slightly inflate the hair model otherwise its outer layer z-fights with the outfit model's outer layer */
   // correction: was not actually caused by this
   private static final CubeDeformation HAIR_DEFORMATION = new CubeDeformation(0.0F);

   public static final ModelLayerLocation HAIR = layer("hair");
   public static final ModelLayerLocation HAIR_SLIM = layer("hair_slim");

   private CivilizedModelLayers() {
   }

   public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
      event.registerLayerDefinition(
            HAIR,
            () -> LayerDefinition.create(PlayerModel.createMesh(HAIR_DEFORMATION, false), 64, 64));
      event.registerLayerDefinition(
            HAIR_SLIM,
            () -> LayerDefinition.create(PlayerModel.createMesh(HAIR_DEFORMATION, true), 64, 64));
   }

   private static ModelLayerLocation layer(String name) {
      return new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(CIVILIZED_MOD_ID, "civilized_villager"),
            name);
   }
}
