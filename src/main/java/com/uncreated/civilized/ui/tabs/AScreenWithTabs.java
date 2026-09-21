package com.uncreated.civilized.ui.tabs;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.ui.components.IRefreshableUI;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

public abstract class AScreenWithTabs extends Screen implements IRefreshableUI, ITabHost {
   private TabController tabController;

   protected int leftPos;
   protected int topPos;

   protected final int imageWidth;
   protected final int imageHeight;

   public AScreenWithTabs(Component title, int imageWidth, int imageHeight) {
      super(title);
      this.imageWidth = imageWidth;
      this.imageHeight = imageHeight;
   }

   protected abstract List<ATab> createTabs();

   @Override
   protected void init() {
      super.init();
      this.leftPos = (this.width - this.imageWidth) / 2;
      this.topPos = (this.height - this.imageHeight) / 2;
      tabController = new TabController(createTabs(), this::addWidget, this::removeWidget);
      // WARNING: this line seems to cause misaligned button clicking for some reason
   }

   @Override
   public ATab changeTab(int newTabIndex) {
      return tabController.changeTab(newTabIndex);
   }

   public @Nullable ATab getCurrentTab() {
      return tabController.getCurrentTab();
   }

   public abstract int getFirstTabButtonX();

   public abstract int getFirstTabButtonY();

   public ATab changeToDefaultTabIfNotSet() {
      return tabController.changeToDefaultTabIfNotSet();
   }

   public void renderCurrentTabContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      tabController.renderCurrentTabContents(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public void render(GuiGraphics graphics, int mouseX, int mouseY, float idkSomeNumber) {
      super.render(graphics, mouseX, mouseY, idkSomeNumber);
      renderCurrentTabContents(graphics, mouseX, mouseY, idkSomeNumber);
      graphics.hLine(RenderType.guiOverlay(), 0, width, height / 2, Colors.VALIDATION_ERROR);

      // this.renderTooltip(graphics, mouseX, mouseY);
   }

   @Override
   public void renderBackground(GuiGraphics p_295206_, int p_295457_, int p_294596_, float p_296351_) {
      this.renderTransparentBackground(p_295206_);
      this.renderBg(p_295206_, p_296351_, p_295457_, p_294596_);
   }

   protected void renderBg(GuiGraphics p_283137_, float p_282476_, int p_281600_, int p_283194_) {
   }

   @Override
   public void refresh() {
      tabController.refresh();
   }
}
