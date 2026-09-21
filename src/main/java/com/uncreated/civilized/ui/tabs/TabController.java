package com.uncreated.civilized.ui.tabs;

import java.util.function.Consumer;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import com.uncreated.civilized.ui.components.IRefreshableUI;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Keeps track of a screen's tabs and which one is showing.
 **/
public class TabController implements IRefreshableUI {

   // It made much more sense to have a separate TabController class for keeping track of tabs instead of a vase
   // `ScreenWithTabs` class, simply because menu's already extend AbstractContainerMenu.

   private final Consumer<ATab> attachTabToScreen;
   private final Consumer<ATab> detachTabFromScreen;
   private final Function<ITabHost, ATab> defaultTab;

   @Getter
   @Nullable
   private ATab currentTab;

   public TabController(
         Function<ITabHost, ATab> defaultTab,
         Consumer<ATab> attachTabToScreen,
         Consumer<ATab> detachTabFromScreen) {
      this.defaultTab = defaultTab;
      this.attachTabToScreen = attachTabToScreen;
      this.detachTabFromScreen = detachTabFromScreen;
   }

   public ATab changeTab(ATab newTab) {
      if (currentTab != null) {
         currentTab.onClose();
         detachTabFromScreen.accept(currentTab);
      }

      currentTab = newTab;
      currentTab.init();
      refresh();
      attachTabToScreen.accept(currentTab);

      return currentTab;
   }

   public ATab changeToDefaultTabIfNotSet(ITabHost tabHost) {
      if (currentTab == null) {
         ATab defaultTab = this.defaultTab.apply(tabHost);
         changeTab(defaultTab);
      }

      return currentTab;
   }

   public void renderCurrentTabContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      if (currentTab != null)
         currentTab.render(graphics, mouseX, mouseY, partialTick);
   }

   @Override
   public void refresh() {
      if (currentTab != null)
         currentTab.refresh();
   }
}
