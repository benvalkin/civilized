package com.uncreated.civilized.core.dialogue;

import javax.annotation.Nullable;

import com.uncreated.civilized.core.dialogue.context.ResponseOptionContext;
import com.uncreated.civilized.ui.style.Colors;

import lombok.Getter;
import net.minecraft.network.chat.Component;

@Getter
public class ResponseOption {
   private @Nullable String key;
   private Component playerSpeech;
   private IResponseOptionVisibleCheck visibleCheck;
   private IResponseOptionEnabledCheck enabledCheck;
   private IOnResponseSelectedAction onPress;
   private @Nullable Dialogue nextDialogue;
   @Nullable
   private Component tooltip;

   private ResponseOption(Component playerSpeech) {
      this.playerSpeech = playerSpeech;
      this.visibleCheck = IResponseOptionVisibleCheck.alwaysVisible();
      this.enabledCheck = IResponseOptionEnabledCheck.alwaysEnabled();
   }

   public ResponseOption key(String key) {
      this.key = key;
      return this;
   }

   public ResponseOption onSelect(IOnResponseSelectedAction onSelect) {
      this.onPress = onSelect;
      return this;
   }

   public ResponseOption shouldBeVisible(IResponseOptionVisibleCheck visibleCheck) {
      this.visibleCheck = visibleCheck;
      return this;
   }

   public ResponseOption shouldBeEnabled(IResponseOptionEnabledCheck enabledCheck) {
      this.enabledCheck = enabledCheck;
      return this;
   }

   public ResponseOption onSelectDoNothing() {
      this.onPress = context -> SelectedAction.DO_NOTHING;
      return this;
   }

   public ResponseOption onSelectCloseDialogue() {
      this.onPress = context -> SelectedAction.CLOSE_DIALOGUE;
      return this;
   }

   public ResponseOption onSelectGoNextPage() {
      this.onPress = context -> SelectedAction.GO_NEXT;
      return this;
   }

   public ResponseOption onSelectGoTo(Dialogue anotherDialogue) {
      this.nextDialogue = anotherDialogue;
      this.onPress = context -> SelectedAction.GO_NEXT;
      return this;
   }

   public static ResponseOption option(Component playerSpeech) {
      return new ResponseOption(playerSpeech).onSelectGoNextPage();
   }

   public ResponseOption withTooltip(Component tooltip) {
      this.tooltip = tooltip;
      return this;
   }

   @Getter
   public enum SelectedAction {
      DO_NOTHING, GO_NEXT, CLOSE_DIALOGUE;
   }

   public static ResponseOption nextPage() {
      return ResponseOption
            .option(
                  Component.translatable("villager.dialogue.response.next_page")
                        .withColor(Colors.MENU_TEXT_VILLAGER_DIALOGUE_ACTION))
            .onSelectGoNextPage();
   }

   public static ResponseOption nextPage(Component playerSpeech) {
      return ResponseOption.option(playerSpeech).onSelectGoNextPage();
   }

   public static ResponseOption closeDialogue() {
      return ResponseOption
            .option(
                  Component.translatable("villager.dialogue.response.close")
                        .withColor(Colors.MENU_TEXT_VILLAGER_DIALOGUE_ACTION))
            .onSelectCloseDialogue();
   }

   public interface IOnResponseSelectedAction {
      SelectedAction onOptionSelected(ResponseOptionContext context);
   }

   public interface IResponseOptionEnabledCheck {
      EnabledCheckResult isOptionEnabled(ResponseOptionContext context);

      static IResponseOptionEnabledCheck alwaysEnabled() {
         return context -> new EnabledCheckResult(true, null);
      }
   }

   public interface IResponseOptionVisibleCheck {
      boolean isOptionVisible(ResponseOptionContext context);

      static IResponseOptionVisibleCheck alwaysVisible() {
         return context -> true;
      }
   }

   @Getter
   public static class EnabledCheckResult {
      private final boolean isEnabled;
      private final @Nullable Component tooltip;

      private EnabledCheckResult(boolean isEnabled, @Nullable Component tooltip) {
         this.isEnabled = isEnabled;
         this.tooltip = tooltip;
      }

      public static EnabledCheckResult success(Component tooltip) {
         return new EnabledCheckResult(true, tooltip);
      }

      public static EnabledCheckResult success() {
         return new EnabledCheckResult(true, null);
      }

      public static EnabledCheckResult failed(Component tooltip) {
         return new EnabledCheckResult(false, tooltip);
      }

      public static EnabledCheckResult failed() {
         return new EnabledCheckResult(false, null);
      }
   }
}
