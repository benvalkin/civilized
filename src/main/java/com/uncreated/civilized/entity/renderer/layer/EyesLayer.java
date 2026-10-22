package com.uncreated.civilized.entity.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderState;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public class EyesLayer
      extends RenderLayer<CivilizedVillagerRenderState, HumanoidModel<CivilizedVillagerRenderState>> {

   public EyesLayer(CivilizedVillagerRenderer renderer) {
      super(renderer);
   }

   @Override
   public void render(
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight,
         CivilizedVillagerRenderState renderState,
         float yRot,
         float xRot) {

      RenderLayer.renderColoredCutoutModel(
            getParentModel(),
            renderState.eyes,
            poseStack,
            bufferSource,
            packedLight,
            renderState,
            0xffffff);
   }
}
