package com.uncreated.civilized.core.dialogue;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import com.uncreated.civilized.entity.CivilizedVillager;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * Text from the translation files under {@code src/main/lang/<lang>/}, referred to by file path, e.g.
 * {@code "dialogue/vagrant/1/grunt"}. The {@code mergeLangFiles} task in build.gradle turns those paths into keys.
 */
public final class DialogueLines {

   private DialogueLines() {
   }

   public static String key(String file) {
      return CIVILIZED_MOD_ID + "." + file.replace('/', '.');
   }

   /** An entry of a file holding an object. */
   public static Component text(String file, String name, Object... args) {
      return Component.translatable(key(file) + "." + name, args);
   }

   /** How many lines a file holding a list has. */
   public static int lineCount(String file) {
      String key = key(file);
      int count = 0;
      while (Language.getInstance().has(key + "." + (count + 1)))
         count++;
      return count;
   }

   public static Component randomLine(String file, Object... args) {
      return line(file, RandomSource.create(), args);
   }

   /** Always the same line for the same villager. */
   public static Component randomLinePerVillager(CivilizedVillager villager, String file, Object... args) {
      return line(file, villager.perLifetimeRandom(1), args);
   }

   /**
    * Picks one of the numbered sets in {@code folder} ({@code folder/1}, {@code folder/2}, ...), always the same one
    * for the same villager. Sets are counted by which of them contain {@code fileInEverySet}.
    *
    * @return the chosen set's path, e.g. {@code "dialogue/vagrant/2"}
    */
   public static String chooseSetPerVillager(CivilizedVillager villager, String folder, String fileInEverySet) {
      int setCount = 0;
      while (lineCount(folder + "/" + (setCount + 1) + "/" + fileInEverySet) > 0)
         setCount++;

      int set = setCount == 0 ? 1 : villager.perLifetimeRandom().nextInt(setCount) + 1;
      return folder + "/" + set;
   }

   private static Component line(String file, RandomSource random, Object... args) {
      int count = lineCount(file);
      // a missing file shows its key, which makes it easy to spot
      int line = count == 0 ? 1 : random.nextInt(count) + 1;
      return Component.translatable(key(file) + "." + line, args);
   }
}
