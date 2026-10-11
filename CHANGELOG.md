# Releases

## 0.9.0 · Switch gear. Move the right stuff.

- **Transfer all:** arrows above a chest move allowed items in either direction, respecting destination profiles, slot rules, capacity, and stack caps.
- **Five quick profiles:** new saved profiles automatically join a compact panel beside the inventory, up to five shortcuts.
- **Your shortcut style:** choose one letter or an item icon, plus a palette or custom background colour. Defaults use the first letter and a random colour.
- **Automatic arrangement:** switching profiles puts matching items in saved slots and clears missing-item reservations when enough allowed space exists.
- **Safe planning:** full-inventory tool swaps work without an empty buffer slot. Impossible arrangements leave items in place; counts and components are preserved.
- **Organized Advanced menu:** Filters & slots, Profiles, and Transfers & sorting. Both transfer directions, the quick panel, and arrangement on switching have separate toggles.

## 0.8.0 · Every chest gets its own rules

- **Automatic chest profiles:** whole-chest filters and individual slot rules save to each storage block's world, dimension, and position.
- **Independent setups:** chest rules stay separate from player rules and named player profiles.
- **Chest controls:** visual item selection, four modes, pause, tag/category lookup, and a reset for the open chest.
- **Double chests:** opening either half preserves slot ownership; splitting retains the surviving half's setup.
- **More storage:** normal chests, trapped chests, and barrels retain rules after closing, reconnecting, and reloading settings.
- **Clear on removal:** an observed server block-removal update clears that block's saved rules.
- **Layout:** keep chest backgrounds, labels, and slots aligned beside the filter drawer.

Chest persistence works with client-only installation on remote servers. Rules are stored locally per player; hopper automation and other players' actions remain server-controlled.

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
