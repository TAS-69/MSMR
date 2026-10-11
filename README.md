<div align="center">

<img src="common/src/main/resources/assets/msmr/textures/gui/icon.png" alt="MSMR" width="256">

# My Slots, My Rules

**Keep the loot. Leave the clutter.**

Minecraft **1.21.1** · **Fabric & NeoForge** · **0.10.0**

**[⬇ Fabric](https://github.com/TAS-69/MSMR/releases/download/v0.10.0/msmr-fabric-1.21.1-0.10.0.jar) · [⬇ NeoForge](https://github.com/TAS-69/MSMR/releases/download/v0.10.0/msmr-neoforge-1.21.1-0.10.0.jar) · [📖 Wiki](https://github.com/TAS-69/MSMR/wiki)**

</div>

## Does this annoy you?

You throw out sticks or rotten flesh, then walk straight back into them. **Inventory full. Again.**

**MSMR blocks unwanted drops before they enter your inventory.** Choose the items you want to leave behind. They stay on the ground.

![Actual MSMR pickup screen: Block mode with unwanted drops selected](docs/images/pickup-filters.png)

### Pick it. Block it. Done.

**①** Open your inventory → click the **MSMR tab** on the left.<br>
**②** Click **+ Add items** → select their pictures.<br>
**③** Leave the mode on **Block**. Your filter is already active.

**Changes save automatically.**

| − **Block** | ✓ **Only** | + **All** | × **None** |
| :---: | :---: | :---: | :---: |
| Skip selected drops | Collect only selected drops | Collect everything | Collect nothing |

![Actual item picker with searchable item pictures and selection markers](docs/images/item-picker.png)

**Search by name. Click to select. Click again to remove.** Pause the whole-inventory pickup filter whenever you need it.

## Have you ever wanted a pickaxe-only hotbar slot?

Or one setup for mining, another for building, and a third for farming?

Open **Advanced** for more control:

- **Slot rules** — reserve individual slots, set stack caps, and copy or paint filters.
- **Tags & categories** — choose all swords, seeds, pickaxes, logs, or matching blocks.
- **Profiles** — save, switch, copy, and delete your player-inventory setups.
- **Movement rules** — filter manual placement and supported shift-click transfers separately from pickups.

![Actual slot editor with a pickaxes-only hotbar rule](docs/images/slot-rules.png)

## Mining pickaxe. Combat sword. One click.

**Up to five coloured profile shortcuts, right beside your inventory.**

Click **Mining** and your pickaxe goes into its saved slot. Click **Combat** and your sword takes its place. Missing the reserved item? MSMR clears that slot when there is room to move its contents safely.

![Quick profile shortcuts beside the actual inventory](docs/images/quick-profiles.png)

New profiles get their first letter and a random background colour. Prefer **Z**, a diamond sword, or your own colour? Customize each shortcut in **Advanced → Profiles**.

## Chest says no? Transfer all listens.

**↑ Inventory → chest. ↓ Chest → inventory. Only allowed items move.**

The small arrows above an open chest respect its saved filters, your player filters, slot rules, and stack limits. Blocked items and anything that will not fit stay where they are.

![Filter-aware transfer arrows above an open chest](docs/images/quick-transfers.png)

**Your choice:** toggle each transfer direction in **Advanced → Transfers & sorting**. Toggle the quick panel and automatic arrangement in **Advanced → Profiles**. [Sorting & transfers →](https://github.com/TAS-69/MSMR/wiki/Sorting-and-Transfers)

## A different job for every chest

**Building blocks here. Farming supplies there. Each chest remembers its own rules.**

Open a chest → click the **MSMR tab** → choose what belongs. Whole-chest and slot filters save automatically, separately from your player inventory.

![Chest-specific filter with automatic saving](docs/images/chest-filters.png)

**Close it. Reconnect. Restart. Your chest setup stays put.** Double chests, trapped chests, and barrels are supported too. [Chest guide →](https://github.com/TAS-69/MSMR/wiki/Chest-Filters)

Player and chest rules persist per world/server. Other supported container rules last while the container is open.

## Hoppers read the room.

**Shared chest rules protect against unwanted insertions from hoppers, droppers, other players, and supported modded inventory tools.** Install MSMR on the server to make the chest's rules apply to everyone. Singleplayer already includes this support.

Rules save with the chest. The first player to set them becomes the filter owner; the owner and server operators can edit them. Everyone can still take items out.

**Busy chest? Safe moves.** Supported servers handle sorting and transfers against live inventory contents. Acknowledgements, identical-request retries, and stale-state checks prevent delayed replies from repeating a move. [Multiplayer & automation →](https://github.com/TAS-69/MSMR/wiki/Multiplayer-and-Automation)

## Install & play

Download **one JAR** for your loader, put it in your instance's **`mods`** folder, and launch Minecraft **1.21.1**.

| Playing where? | Pickup filtering and shared chest automation need |
| --- | --- |
| **Singleplayer** | MSMR on your client. **No Fabric API required.** |
| **Multiplayer** | MSMR on the client **and server**. Fabric endpoints also need Fabric API; NeoForge includes networking. |

**You can join unsupported servers.** Local inventory tools still work; ground-pickup filtering shows **Unavailable**, and chest rules remain local.

Requires Java **21**, Fabric Loader **0.16.9+** or NeoForge **21.1.80+**. [Installation help →](https://github.com/TAS-69/MSMR/wiki/Installation)

**[Pickup guide](https://github.com/TAS-69/MSMR/wiki/Pickup-Filters) · [Advanced tools](https://github.com/TAS-69/MSMR/wiki/Advanced-Filters) · [Profiles](https://github.com/TAS-69/MSMR/wiki/Profiles) · [Report a bug](https://github.com/TAS-69/MSMR/issues/new)**

<details>
<summary><strong>Build from source</strong></summary>

Use JDK 21 and the included wrapper:

```sh
./gradlew :common:test :fabric:build :neoforge:build
```

Windows: use `gradlew.bat`. Installable JARs appear in each loader's `build/libs` directory. [Development guide →](https://github.com/TAS-69/MSMR/wiki/Development)

</details>

[CC0 1.0](LICENSE) · [Release notes](CHANGELOG.md)
