package com.uncreated.civilized.entity.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderState;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderer;

import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class VillagerArmorLayer
      extends RenderLayer<CivilizedVillagerRenderState, HumanoidModel<CivilizedVillagerRenderState>> {

   private final HumanoidArmorLayer<CivilizedVillagerRenderState, HumanoidModel<CivilizedVillagerRenderState>, HumanoidArmorModel<CivilizedVillagerRenderState>> wideArmor;
   private final HumanoidArmorLayer<CivilizedVillagerRenderState, HumanoidModel<CivilizedVillagerRenderState>, HumanoidArmorModel<CivilizedVillagerRenderState>> slimArmor;

   public VillagerArmorLayer(CivilizedVillagerRenderer renderer, EntityRendererProvider.Context context) {
      super(renderer);
      wideArmor =
            new HumanoidArmorLayer<>(
                  renderer,
                  new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                  new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                  context.getEquipmentRenderer());
      slimArmor =
            new HumanoidArmorLayer<>(
                  renderer,
                  new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                  new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
                  context.getEquipmentRenderer());
   }

   @Override
   public void render(
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight,
         CivilizedVillagerRenderState renderState,
         float yRot,
         float xRot) {
      HumanoidArmorLayer<CivilizedVillagerRenderState, ?, ?> armor =
            renderState.useSlimArmsPlayerModel ? slimArmor : wideArmor;
      armor.render(poseStack, bufferSource, packedLight, renderState, yRot, xRot);
   }
}
