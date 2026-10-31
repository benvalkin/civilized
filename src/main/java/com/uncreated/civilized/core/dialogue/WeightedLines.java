package com.uncreated.civilized.core.dialogue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import com.uncreated.civilized.core.dialogue.context.DialogueContext;

import net.minecraft.network.chat.Component;

/**
 * Picks one of several dialogue line file variants from a weighted list, and then chooses a random dialogue line from
 * it. Files that don't exist are skipped.
 *
 * <pre>
 * weighted().lines("grunt", 2)
 *       .lines(context -> "occupation/" + context.getVillager().getInfo().getOccupation().name(), 3)
 *       .lines("hungry", context -> isHungry(context) ? 4 : 0)
 * </pre>
 */
public class WeightedLines implements DialogueText {

   private record Pool(Function<DialogueContext, String> file, ToIntFunction<DialogueContext> weight) {
   }

   private final List<Pool> pools = new ArrayList<>();

   public static WeightedLines weighted() {
      return new WeightedLines();
   }

   public WeightedLines lines(String file, int weight) {
      return lines(context -> file, context -> weight);
   }

   public WeightedLines lines(Function<DialogueContext, String> file, int weight) {
      return lines(file, context -> weight);
   }

   public WeightedLines lines(String file, ToIntFunction<DialogueContext> weight) {
      return lines(context -> file, weight);
   }

   public WeightedLines lines(Function<DialogueContext, String> file, ToIntFunction<DialogueContext> weight) {
      pools.add(new Pool(file, weight));
      return this;
   }

   @Override
   public List<Component> pages(DialogueScript script, DialogueContext context) {
      List<String> files = new ArrayList<>();
      List<Integer> weights = new ArrayList<>();
      int totalWeight = 0;
      for (Pool pool : pools) {
         int weight = pool.weight().applyAsInt(context);
         if (weight <= 0)
            continue;

         String file = script.linesFileName(context, pool.file().apply(context));
         if (DialogueLines.entryCount(file) == 0)
            continue;

         files.add(file);
         weights.add(weight);
         totalWeight += weight;
      }

      // shows the first file's missing key, which makes it easy to spot
      if (files.isEmpty())
         return DialogueLines.randomPages(
               context.getVillager(),
               script.linesFileName(context, pools.getFirst().file().apply(context)));

      int roll = context.getVillager().getRandom().nextInt(totalWeight);
      int chosen = 0;
      while (roll >= weights.get(chosen)) {
         roll -= weights.get(chosen);
         chosen++;
      }
      return DialogueLines.randomPages(context.getVillager(), files.get(chosen));
   }
}
