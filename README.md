# Adaptive Tools

NeoForge mod for Minecraft Java Edition. Tiered tools, armor, and a wireless power network. The current build has alloy ingots, pickaxes, area mining, and Light, Medium, and Heavy cables.

- Minecraft `26.1.2`
- Java `25`
- NeoForge `26.1.2.126`
- Mod id `adaptivetools`

## Build

```
./gradlew build
```

The mod jar is written to `build/libs/adaptivetools-0.2.1.jar`.

Game tests:

```
./gradlew runGameTestServer
```

Refresh generated models, tags, and recipes:

```
./gradlew runData
```

Client and dedicated server:

```
./gradlew runClient
./gradlew runServer
```

Item, armor, block, and cable textures in `assets/adaptivetools/textures` are the approved finals. Axes, shovels, hoes, swords, armor, smithing templates, and the charging blocks are art only until those items and blocks are added. Baby armor and text-free screen backgrounds are not in this set.
