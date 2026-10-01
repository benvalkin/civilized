package com.uncreated.civilized.client.toast;

import java.util.List;

import com.uncreated.civilized.core.notifications.Severity;
import com.uncreated.civilized.ui.style.Colors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

public class SettlementNotificationToast implements Toast {

   private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/advancement");
   private static final int BACKGROUND_WIDTH = 160;
   private static final int BACKGROUND_HEIGHT = 32;
   /** Half of the background, which is drawn as a top and bottom with a plain strip repeated in between. */
   private static final int BACKGROUND_HALF = BACKGROUND_HEIGHT / 2;
   /** A slice from the middle of the background with nothing but the sides in it, so it can be repeated. */
   private static final int BACKGROUND_STRIP_V = 8;

   private static final int ICON_X = 8;
   private static final int ICON_Y = 8;
   private static final int TEXT_X_WITH_ICON = 30;
   private static final int TEXT_X_WITHOUT_ICON = 8;
   private static final int TEXT_RIGHT_PADDING = 6;
   private static final int HEADLINE_Y = 7;
   private static final int DETAIL_Y = 18;
   private static final int LINE_HEIGHT = 9;
   private static final int BOTTOM_PADDING = 5;
   /** Longer details are cut short, so a single notification can't fill the screen. */
   private static final int MAX_DETAIL_LINES = 4;

   private static final int DETAIL_COLOR = Colors.TEXT_LIGHT_MUTED;

   private final Component headline;
   private final Severity severity;
   private final ItemStack icon;

   private final int textX;
   private final List<FormattedCharSequence> detailLines;
   private final int height;
   private Toast.Visibility wantedVisibility = Toast.Visibility.SHOW;

   public SettlementNotificationToast(Component headline, Component detail, Severity severity, ItemStack icon) {
      this.headline = headline;
      this.severity = severity;
      this.icon = icon;

      textX = icon.isEmpty() ? TEXT_X_WITHOUT_ICON : TEXT_X_WITH_ICON;
      List<FormattedCharSequence> lines =
            Minecraft.getInstance().font.split(detail, BACKGROUND_WIDTH - textX - TEXT_RIGHT_PADDING);
      detailLines = lines.subList(0, Math.min(lines.size(), MAX_DETAIL_LINES));
      // vanilla toasts require us to pre-calculate the toast widget height using our font + number of lines
      height = Math.max(BACKGROUND_HEIGHT, DETAIL_Y + detailLines.size() * LINE_HEIGHT + BOTTOM_PADDING);
   }

   @Override
   public int width() {
      return BACKGROUND_WIDTH;
   }

   @Override
   public int height() {
      return height;
   }

   @Override
   public Toast.Visibility getWantedVisibility() {
      return wantedVisibility;
   }

   @Override
   public void update(ToastManager toastManager, long visibilityTime) {
      // the multiplier is vanilla's accessibility setting for how long toasts stay up
      double displayTime = displayTimeMillis(severity) * toastManager.getNotificationDisplayTimeMultiplier();
      wantedVisibility = visibilityTime >= displayTime ? Toast.Visibility.HIDE : Toast.Visibility.SHOW;
   }

   private static long displayTimeMillis(Severity severity) {
      return switch (severity) {
      case POSITIVE, INFO -> 8000L;
      case MINOR -> 15000L;
      case MAJOR -> 15000L;
      case CRITICAL -> 15000L;
      };
   }

   @Override
   public void render(GuiGraphics graphics, Font font, long visibilityTime) {
      renderBackground(graphics);

      if (!icon.isEmpty())
         graphics.renderFakeItem(icon, ICON_X, ICON_Y);

      graphics.drawString(font, headline, textX, HEADLINE_Y, severityColor(severity), false);
      for (int i = 0; i < detailLines.size(); i++)
         graphics.drawString(font, detailLines.get(i), textX, DETAIL_Y + i * LINE_HEIGHT, DETAIL_COLOR, false);
   }

   /** The top and bottom halves of the background, with its plain middle repeated between them to fill the height. */
   private void renderBackground(GuiGraphics graphics) {
      blitBackgroundSlice(graphics, 0, 0, BACKGROUND_HALF);

      for (int y = BACKGROUND_HALF; y < height - BACKGROUND_HALF; y += BACKGROUND_HALF)
         blitBackgroundSlice(graphics, BACKGROUND_STRIP_V, y, Math.min(BACKGROUND_HALF, height - BACKGROUND_HALF - y));

      blitBackgroundSlice(graphics, BACKGROUND_HALF, height - BACKGROUND_HALF, BACKGROUND_HALF);
   }

   private static void blitBackgroundSlice(GuiGraphics graphics, int v, int y, int sliceHeight) {
      graphics.blitSprite(
            RenderType::guiTextured,
            BACKGROUND_SPRITE,
            BACKGROUND_WIDTH,
            BACKGROUND_HEIGHT,
            0,
            v,
            0,
            y,
            BACKGROUND_WIDTH,
            sliceHeight);
   }

   private static int severityColor(Severity severity) {
      int rgb = switch (severity) {
      case POSITIVE -> Colors.VALIDATION_SUCCESS;
      case INFO -> Colors.INFO;
      case MINOR -> Colors.WARNING_MINOR;
      case MAJOR -> Colors.WARNING_MAJOR;
      case CRITICAL -> Colors.VALIDATION_ERROR;
      };
      // text drawn without an alpha channel would be invisible
      return 0xFF000000 | rgb;
   }
}
