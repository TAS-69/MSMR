# Releases

## 0.7.0 · First public release

**Minecraft 1.21.1 · Fabric & NeoForge**

- **Drop filtering:** Block, Only, All, and None modes; automatic saving; pause control.
- **Visual selection:** searchable item pictures, selection markers, and a held-item shortcut.
- **Advanced rules:** items, tags, categories, individual slots, stack caps, and separate pickup/movement switches.
- **Profiles:** save, switch, copy, and delete player-inventory setups.
- **Inventory tools:** copy/paint slot rules, filtered shift-click transfers, and fill reserved slots.
- **Pickup support:** singleplayer without Fabric API; supported servers with MSMR and compatible networking.

Player rules persist per world/server. Container rules last for the open-container session. Hopper automation, automatic refill, arbitrary NBT/component predicates, and general-purpose sorting are outside this build.

**Verified:** 33 unit tests and 254 in-game checks across Fabric/NeoForge singleplayer, an unsupported server, and an MSMR-enabled server.
