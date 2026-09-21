package com.uncreated.civilized.ui.tabs;

import java.util.List;
import java.util.function.Consumer;

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

   private final List<ATab> tabs;
   private final Consumer<ATab> attachTabToScreen;
   private final Consumer<ATab> detachTabFromScreen;

   @Getter
   @Nullable
   private ATab currentTab;

   public TabController(List<ATab> tabs, Consumer<ATab> attachTabToScreen, Consumer<ATab> detachTabFromScreen) {
      if (tabs.isEmpty())
         throw new IllegalArgumentException("Tabs list cannot be empty.");

      this.tabs = tabs;
      this.attachTabToScreen = attachTabToScreen;
      this.detachTabFromScreen = detachTabFromScreen;
   }

   public ATab changeTab(int newTabIndex) {
      if (currentTab != null) {
         currentTab.onClose();
         detachTabFromScreen.accept(currentTab);
      }

      currentTab = tabs.get(newTabIndex);
      currentTab.init();
      refresh();
      attachTabToScreen.accept(currentTab);

      return currentTab;
   }

   public ATab changeToDefaultTabIfNotSet() {
      if (currentTab == null)
         changeTab(0);

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
