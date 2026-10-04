package com.uncreated.civilized.ui.menu.building.widgets;

import java.util.List;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.building.requirement.IBuildingRequirementResult;
import com.uncreated.civilized.ui.components.IListViewBuilder;
import com.uncreated.civilized.ui.components.ScrollListView;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractContainerWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;


public class BuildingRequirementsView extends AbstractContainerWidget {

   private static final int HEADING_HEIGHT = 15;
   private static final int ELEMENT_SPACING = 14;

   private final Font font;
   /** Null while the requirements are being checked. */
   private @Nullable List<IBuildingRequirementResult> results;
   private @Nullable ScrollListView<IBuildingRequirementResult, BuildingRequirementWidget> scrollView;

   public BuildingRequirementsView(int x, int y, int width, int height, Font font) {
      super(x, y, width, height, Component.translatable("menu.building.management.requirements.heading"));
      this.font = font;
   }

   /**
    * @param results
    *           the checked requirements, or null to show that they're being checked again
    */
   public void setResults(@Nullable List<? extends IBuildingRequirementResult> results) {
      this.results = results == null ? null : List.copyOf(results);

      if (this.results == null) {
         scrollView = null;
         return;
      }

      List<IBuildingRequirementResult> shown =
            this.results.stream().filter(r -> !(r.hideIfSatisfied() && r.isSatisfied())).toList();
      scrollView = createScrollView(shown);
   }

   public boolean isChecking() {
      return results == null;
   }

   /** Whether every requirement is satisfied. False while they're still being checked. */
   public boolean allSatisfied() {
      return results != null && results.stream().allMatch(IBuildingRequirementResult::isSatisfied);
   }

   /**
    * Only lets the button be pressed once every requirement is met, with a tooltip saying why it can't be otherwise.
    */
   public void updateConfirmButton(Button button) {
      button.active = allSatisfied();

      if (isChecking())
         button.setTooltip(Tooltip.create(Component.translatable("menu.building.management.requirements.checking")));
      else if (!allSatisfied())
         button.setTooltip(
               Tooltip.create(
                     Component.translatable("menu.building.management.requirements.tooltip.not_satisfied_hint")
                           .withColor(Colors.VALIDATION_ERROR)));
      else
         button.setTooltip(null);
   }

   @Override
   protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      graphics.drawString(
            font,
            getMessage().copy().withStyle(ChatFormatting.UNDERLINE),
            getX(),
            getY(),
            Colors.MENU_TEXT_DARK,
            false);

      int listY = getY() + HEADING_HEIGHT;
      if (results == null)
         drawNote(graphics, Component.translatable("menu.building.management.requirements.checking"), listY);
      else if (results.isEmpty())
         drawNote(graphics, Component.translatable("menu.building.management.requirements.none"), listY);
      else if (scrollView != null)
         scrollView.render(graphics, mouseX, mouseY, partialTick);
   }

   private void drawNote(GuiGraphics graphics, Component note, int y) {
      graphics.drawString(font, note, getX(), y, Colors.MENU_TEXT_DARK, false);
   }

   private ScrollListView<IBuildingRequirementResult, BuildingRequirementWidget> createScrollView(
         List<IBuildingRequirementResult> shown) {
      return new ScrollListView<>(
            getX(),
            getY() + HEADING_HEIGHT,
            width,
            height - HEADING_HEIGHT,
            ELEMENT_SPACING,
            new IListViewBuilder<>() {
               @Override
               public List<IBuildingRequirementResult> provideModelData() {
                  return shown;
               }

               @Override
               public BuildingRequirementWidget buildElementWidgetFromModel(
                     int elementIndex,
                     IBuildingRequirementResult result,
                     int elementX,
                     int elementY,
                     int elementWidth,
                     int elementHeight,
                     int elementSpacing) {
                  return new BuildingRequirementWidget(
                        elementX,
                        elementY,
                        elementWidth,
                        elementHeight,
                        9,
                        font,
                        result);
               }
            });
   }

   @Override
   public List<? extends GuiEventListener> children() {
      return scrollView == null ? List.of() : List.of(scrollView);
   }

   @Override
   protected int contentHeight() {
      return height;
   }

   @Override
   protected double scrollRate() {
      return 1;
   }

   @Override
   protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

   }
}
