"""Renumbers villager skin, hair and eye textures to 1..n with no gaps (keeping their order), then rewrites the
texture registries to list exactly the files on disk. Use it after adding or removing textures.

   python3 scripts/renumber_textures.py
"""
import os, re

# the project root, so the script can be run from anywhere
os.chdir(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

tex = "src/main/resources/assets/civilized/textures/entity/civilized_villager"
registries = [
   ("SkinTextureRegistry.java", "CLOTHING_TEXTURES", "skins"),
   ("HairTextureRegistry.java", "CLOTHING_TEXTURES", "hair"),
   ("EyeTextureRegistry.java", "EYE_TEXTURES", "eyes"),
]
genders = ["male", "female"]

def numbered(d):
   return sorted(int(f[:-4]) for f in os.listdir(d) if re.fullmatch(r"\d+\.png", f))

for _, _, kind in registries:
   for gender in genders:
      d = os.path.join(tex, kind, "default", gender)
      others = [f for f in os.listdir(d) if not re.fullmatch(r"\d+\.png", f) and f != ".DS_Store"]
      assert not others, (d, others)
      nums = numbered(d)
      moves = [(n, i + 1) for i, n in enumerate(nums) if n != i + 1]
      if not moves:
         print(f"{kind}/{gender}: 1-{len(nums)}, no gaps")
         continue
      # via temporary names, so no file overwrites another part way through
      for old, _ in moves:
         os.rename(os.path.join(d, f"{old}.png"), os.path.join(d, f".renumber_{old}.png"))
      for old, new in moves:
         target = os.path.join(d, f"{new}.png")
         assert not os.path.exists(target), target
         os.rename(os.path.join(d, f".renumber_{old}.png"), target)
      print(f"{kind}/{gender}: now 1-{len(nums)}; " + ", ".join(f"{o}->{n}" for o, n in moves))

base = "src/main/java/com/uncreated/civilized/entity/stats/"
for fname, mapname, kind in registries:
   p = base + fname
   s = open(p).read()
   m = re.search(
      r"(   private static final Object2ObjectArrayMap<String, Int2ObjectMap<ResourceLocation>> " + mapname
      + r" =\n         Util\.make\(new Object2ObjectArrayMap<>\(\), map -> \{\n)(.*?)(         \}\);)", s, re.S)
   assert m, fname
   counts = {g: len(numbered(os.path.join(tex, kind, "default", g))) for g in genders}
   lines = [f'            add(map, DEFAULT_CULTURE, "{g}", "{n}");\n' for g in genders for n in range(1, counts[g] + 1)]
   open(p, "w").write(s[:m.start(2)] + "".join(lines) + s[m.end(2):])
   print(fname, counts)
