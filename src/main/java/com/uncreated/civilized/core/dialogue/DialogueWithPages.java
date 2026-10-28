package com.uncreated.civilized.core.dialogue;

import java.util.List;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import lombok.Getter;
import net.minecraft.network.chat.Component;

public class DialogueWithPages {

   @Getter
   private int pageCount;
   private @Nullable Dialogue firstPage;
   private @Nullable Dialogue currentPage;

   protected DialogueWithPages() {
      this.pageCount = 0;
   }

   public DialogueWithPages page(Dialogue page) {
      if (firstPage == null)
         firstPage = page;

      if (currentPage != null)
         currentPage.nextPage(page);

      currentPage = page;

      pageCount++;
      return this;
   }

   /**
    * Adds a page for each of {@code speech}, with a "next page" response on all but the last, which
    * {@code lastPage} adds the responses to.
    */
   public DialogueWithPages pages(List<Component> speech, UnaryOperator<Dialogue> lastPage) {
      for (int i = 0; i < speech.size() - 1; i++)
         page(Dialogue.simplePage(speech.get(i)));

      return page(lastPage.apply(Dialogue.dialogue(speech.getLast())));
   }

   public int pageCount() {
      return pageCount;
   }

   public static DialogueWithPages dialogueWithpages() {
      return new DialogueWithPages();
   }

   public Dialogue create() {
      if (firstPage == null)
         throw new IllegalStateException("DialogueWithPages must be given at least one page_");

      return firstPage;
   }
}
