package com.uncreated.civilized.core.dialogue.questline.migrant;

import static com.uncreated.civilized.core.dialogue.DialogueLines.text;
import static com.uncreated.civilized.core.dialogue.ResponseOption.option;
import static com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions.runOnServer;

import java.util.Optional;

import com.uncreated.civilized.core.dialogue.Dialogue;
import com.uncreated.civilized.core.dialogue.ResponseOption;
import com.uncreated.civilized.core.dialogue.actions.server.ServerDialogueActions;
import com.uncreated.civilized.core.dialogue.questline.migrant.context.CanJoinSettlementCheck;
import com.uncreated.civilized.core.dialogue.questline.migrant.context.WelcomeCheck;
import com.uncreated.civilized.core.settlement.ClientSettlementsStore;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/** Responses shared by the dialogues of visitors who can be recruited. */
public final class RecruitmentResponses {

   private RecruitmentResponses() {
   }

   /** "Welcome to <settlement>!", which only players with a settlement can say. */
   public static Optional<ResponseOption> welcome(Player player, Dialogue reply) {
      return ClientSettlementsStore.INSTANCE.findFromOwner(player.getUUID())
            .map(
                  settlement -> option(
                        text("recruitment/response_options", "welcome", settlement.displayNameTranslation()))
                        .shouldBeVisible(new WelcomeCheck())
                        .onSelectGoTo(runOnServer(ServerDialogueActions.WELCOME), reply));
   }

   public static ResponseOption join(Component playerSpeech, Dialogue accepted) {
      return option(playerSpeech).shouldBeEnabled(new CanJoinSettlementCheck())
            .onSelectGoTo(runOnServer(ServerDialogueActions.JOIN_SETTLEMENT), accepted);
   }
}
