# Adaptive Tools

NeoForge mod for Minecraft Java Edition. Tiered tools, armor, and a wireless power network. This repository currently contains the first playable slice: alloy ingots, pickaxes, FE storage, area mining, and the Tool Settings screen.

- Minecraft `26.1.2`
- Java `25`
- NeoForge `26.1.2.126`
- Mod id `adaptivetools`

## Build

```
./gradlew build
```

The mod jar is written to `build/libs/adaptivetools-0.1.0.jar`.

Game tests:

```
./gradlew runGameTestServer
```

Refresh generated item models and tags:

```
./gradlew runData
```

Client and dedicated server:

```
./gradlew runClient
./gradlew runServer
```

Ingot and pickaxe textures are placeholders. Module, wrench, and GUI textures are the approved finals.
