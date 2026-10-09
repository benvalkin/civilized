package com.uncreated.civilized.entity.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uncreated.civilized.entity.renderer.CivilizedModelLayers;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderState;
import com.uncreated.civilized.entity.renderer.CivilizedVillagerRenderer;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Draws the villager's hair over its clothing. The hair has its own player model which is very slightly bigger than the
 * villager's, so that it's always drawn just outside the clothing (outer layers z-fight otherwise).
 */
public class HairLayer extends RenderLayer<CivilizedVillagerRenderState, HumanoidModel<CivilizedVillagerRenderState>> {

   private final HumanoidModel<CivilizedVillagerRenderState> wideModel;
   private final HumanoidModel<CivilizedVillagerRenderState> slimModel;

   public HairLayer(CivilizedVillagerRenderer renderer, EntityModelSet entityModelSet) {
      super(renderer);
      this.wideModel = new HumanoidModel<>(entityModelSet.bakeLayer(CivilizedModelLayers.HAIR));
      this.slimModel = new HumanoidModel<>(entityModelSet.bakeLayer(CivilizedModelLayers.HAIR_SLIM));
   }

   @Override
   public void render(
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight,
         CivilizedVillagerRenderState renderState,
         float yRot,
         float xRot) {

      HumanoidModel<CivilizedVillagerRenderState> model = renderState.useSlimArmsPlayerModel ? slimModel : wideModel;
      // inherit the main model's pose, otherwise pixels on the hair texture may not move with the rest of the player
      // model (?)
      getParentModel().copyPropertiesTo(model);

      RenderLayer.renderColoredCutoutModel(
            model,
            renderState.hair,
            poseStack,
            bufferSource,
            packedLight,
            renderState,
            0xffffff);
   }
}
