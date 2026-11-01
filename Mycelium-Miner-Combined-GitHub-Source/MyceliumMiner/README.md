# Mycelium Miner — combined source project (Minecraft Java 1.21.11)

**Experimental, untested source.** Build in GitHub Actions; a successful Gradle build does not guarantee the macro works in-game. Back up your single-player world before testing.

## Included
- O opens a multi-page HUB: Mycelium, Stone Generator, Settings, Help.
- Mycelium: manual route/jump/mining points, F7 route recording, F6 start/pause, smooth turning, mining priority over route steering.
- Stone Generator: a separate macro that cycles through individually selected block coordinates, allowing an arbitrary concave shape. It does **not** automatically detect the generator outline. Stand within reach and with a clear line of sight. Add only blocks you intend to break.
- Stop controls, adjustable turning speed, movement key restoration when stopped.

## GitHub
1. Create a new **empty** GitHub repository.
2. Extract this ZIP, open the `MyceliumMiner` folder, and upload **all contents**, including `.github` (the dot-prefixed folder), to the repository root.
3. Open **Actions** > **Build Mycelium Miner** > **Run workflow**.
4. When the run succeeds, open it and download the `mycelium-miner-1.21.11` artifact. Extract the artifact ZIP to find the mod JAR.
5. Install only the JAR (not the source ZIP) in your Fabric 1.21.11 mods/custom-source setup.

## Limitations
- Only one macro runs at a time. The generator macro does not navigate around obstacles or move to unreachable blocks.
- Route/mining targets and stone selections are **not saved between game restarts**.
- Mining requires a clear crosshair ray hit; it may need further tuning on Dawn Client.
- Do not run both old and new copies of this mod at the same time.
- Intended for offline/single-player use; follow server rules elsewhere.
