package com.uncreated.civilized.entity.renderer;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.uncreated.civilized.core.villagerinfo.VillagerOccupations;
import com.uncreated.civilized.entity.CivilizedVillager;
import com.uncreated.civilized.entity.renderer.layer.ClothingLayer;
import com.uncreated.civilized.entity.renderer.layer.HairLayer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.phys.Vec3;

public class CivilizedVillagerRenderer extends
      HumanoidMobRenderer<CivilizedVillager, CivilizedVillagerRenderState, HumanoidModel<CivilizedVillagerRenderState>> {

   public static final boolean DEBUG = true;

   public CivilizedVillagerRenderer(EntityRendererProvider.Context context) {
      super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 1);

      this.addLayer(new ClothingLayer(this, context.getModelSet()));
      this.addLayer(new HairLayer(this, context.getModelSet())); // hair renders over clothing
   }

   @Override
   public CivilizedVillagerRenderState createRenderState() {
      return new CivilizedVillagerRenderState();
   }

   @Override
   public void extractRenderState(CivilizedVillager villager, CivilizedVillagerRenderState state, float partialTick) {
      super.extractRenderState(villager, state, partialTick);
      state.villagerName = villager.getInfo().getFullNameComponent();
      state.occupation = villager.getInfo().getOccupation();
      state.skin = villager.getSkin();
      state.hair = villager.getHair();
      state.clothing = villager.getClothing();

      if (villager.getInfo().getOccupation().is(VillagerOccupations.UNEMPLOYED))
         state.occupationName = villager.getInfo().getOccupation().translation();
      else
         state.occupationName = null;

      if (DEBUG) {
         state.debugBehavioursList = villager.getEntityData().get(CivilizedVillager.CURRENT_WORK_BEHAVIOUR);
      }
   }

   @Override
   protected HumanoidModel.ArmPose getArmPose(CivilizedVillager villager, HumanoidArm arm) {
      InteractionHand hand = arm == villager.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
      if (villager.isAggressive() && villager.getItemInHand(hand).getItem() instanceof BowItem)
         return HumanoidModel.ArmPose.BOW_AND_ARROW;

      return super.getArmPose(villager, arm);
   }

   @Override
   public ResourceLocation getTextureLocation(CivilizedVillagerRenderState renderState) {
      return renderState.skin;
   }

   @Override
   protected boolean shouldShowName(CivilizedVillager p_115506_, double p_364446_) {
      return false;
   }

   @Override
   public void render(
         CivilizedVillagerRenderState renderState,
         PoseStack pose,
         MultiBufferSource bufferSource,
         int packedLight) {
      super.render(renderState, pose, bufferSource, packedLight);

      if (renderState.villagerName != null)
         renderNameTag(renderState, renderState.villagerName, pose, bufferSource, packedLight);
      if (renderState.occupationName != null)
         renderJobTag(renderState, renderState.occupationName, pose, bufferSource, packedLight);

      if (DEBUG)
         renderDebugInfo(renderState, pose, bufferSource, packedLight);
   }

   protected void renderNameTag(
         CivilizedVillagerRenderState renderState,
         Component displayName,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight) {
      Vec3 vec3 = new Vec3(0, 2.1, 0);
      boolean flag = !renderState.isDiscrete;
      int i = "deadmau5".equals(displayName.getString()) ? -10 : 0;
      poseStack.pushPose();
      poseStack.translate(vec3.x, vec3.y + (double) 0.5F, vec3.z);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.scale(0.021F, -0.021F, 0.021F);
      Matrix4f matrix4f = poseStack.last().pose();
      Font font = this.getFont();
      float f = (float) (-font.width(displayName)) / 2.0F;
      int j = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
      font.drawInBatch(
            displayName,
            f,
            (float) i,
            -2130706433,
            false,
            matrix4f,
            bufferSource,
            flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
            j,
            packedLight);
      if (flag) {
         font.drawInBatch(
               displayName,
               f,
               (float) i,
               -1,
               false,
               matrix4f,
               bufferSource,
               Font.DisplayMode.NORMAL,
               0,
               LightTexture.lightCoordsWithEmission(packedLight, 2));
      }

      poseStack.popPose();

   }

   protected void renderJobTag(
         CivilizedVillagerRenderState renderState,
         Component displayName,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight) {
      Vec3 vec3 = new Vec3(0, 1.8, 0);
      boolean flag = !renderState.isDiscrete;
      poseStack.pushPose();
      poseStack.translate(vec3.x, vec3.y + (double) 0.5F, vec3.z);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.scale(0.018F, -0.018F, 0.018F);
      Matrix4f matrix4f = poseStack.last().pose();
      Font font = this.getFont();
      float width = (float) (-font.width(displayName)) / 2.0F;
      int backgroundColor = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
      font.drawInBatch(
            displayName,
            width,
            0,
            0xcccccc,
            false,
            matrix4f,
            bufferSource,
            flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
            backgroundColor,
            packedLight);

      poseStack.popPose();
   }

   protected void renderDebugInfo(
         CivilizedVillagerRenderState renderState,
         PoseStack poseStack,
         MultiBufferSource bufferSource,
         int packedLight) {

      Vec3 vec3 = new Vec3(0, 2.4, 0);
      boolean flag = !renderState.isDiscrete;
      poseStack.pushPose();
      poseStack.translate(vec3.x, vec3.y + (double) 0.5F, vec3.z);
      poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
      poseStack.scale(0.018F, -0.018F, 0.018F);
      Matrix4f matrix4f = poseStack.last().pose();
      Font font = this.getFont();
      float width = (float) (-font.width(renderState.debugBehavioursList)) / 2.0F;
      int backgroundColor = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
      font.drawInBatch(
            renderState.debugBehavioursList,
            width,
            0,
            0xcccccc,
            false,
            matrix4f,
            bufferSource,
            flag ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
            backgroundColor,
            packedLight);

      poseStack.popPose();
   }
}
