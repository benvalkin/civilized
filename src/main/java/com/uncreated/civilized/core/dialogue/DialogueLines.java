package com.uncreated.civilized.core.dialogue;

import static com.uncreated.civilized.CivilizedMod.CIVILIZED_MOD_ID;

import java.util.ArrayList;
import java.util.List;

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

   /** How many entries a file holding a list has. */
   public static int entryCount(String file) {
      String key = key(file);
      int count = 0;
      while (isEntry(key + "." + (count + 1)))
         count++;
      return count;
   }

   /** The first page of a random entry, for files whose entries are all one page long. */
   public static Component randomLine(String file, Object... args) {
      return randomPages(file, args).getFirst();
   }

   /** Always the same line for the same villager. */
   public static Component randomLinePerVillager(CivilizedVillager villager, String file, Object... args) {
      return randomPagesPerVillager(villager, file, args).getFirst();
   }

   public static List<Component> randomPages(String file, Object... args) {
      return pages(file, RandomSource.create(), args);
   }

   public static List<Component> randomPages(CivilizedVillager villager, String file, Object... args) {
      return pages(file, villager.getRandom(), args);
   }

   /** Always the same entry for the same villager. */
   public static List<Component> randomPagesPerVillager(CivilizedVillager villager, String file, Object... args) {
      return pages(file, villager.perLifetimeRandom(1), args);
   }

   /**
    * Picks one of the numbered sets in {@code folder} ({@code folder/1}, {@code folder/2}, ...), always the same one
    * for the same villager. Sets are counted by which of them contain {@code fileInEverySet}.
    *
    * @return the chosen set's path, e.g. {@code "dialogue/vagrant/2"}
    */
   public static String chooseSetPerVillager(CivilizedVillager villager, String folder, String fileInEverySet) {
      int setCount = 0;
      while (entryCount(folder + "/" + (setCount + 1) + "/" + fileInEverySet) > 0)
         setCount++;

      int set = setCount == 0 ? 1 : villager.perLifetimeRandom().nextInt(setCount) + 1;
      return folder + "/" + set;
   }

   private static boolean isEntry(String entryKey) {
      return Language.getInstance().has(entryKey) || Language.getInstance().has(entryKey + ".1");
   }

   private static List<Component> pages(String file, RandomSource random, Object... args) {
      int count = entryCount(file);
      // a missing file shows its key, which makes it easy to spot
      String entryKey = key(file) + "." + (count == 0 ? 1 : random.nextInt(count) + 1);
      if (!Language.getInstance().has(entryKey + ".1"))
         return List.of(Component.translatable(entryKey, args));

      List<Component> pages = new ArrayList<>();
      for (int page = 1; Language.getInstance().has(entryKey + "." + page); page++)
         pages.add(Component.translatable(entryKey + "." + page, args));
      return pages;
   }
}
