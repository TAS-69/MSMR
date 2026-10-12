# MSMR 0.11.0 audit

**12 October 2026 · Minecraft 1.21.1 · Fabric and NeoForge**

The audit covered production Java sources, mixins, loader entrypoints, networking, persistence, inventory planning, UI state and existing tests. The resulting builds pass **69 unit tests and 561 in-game assertions** across the environments below. Counts include repeated checks across environments; they are not counts of distinct features.

## Fixed issues

| Area | Finding and correction |
| --- | --- |
| Pickup rules after respawn | Minecraft replaces `ServerPlayer` on respawn. Transfer the acknowledged rules and revision to the replacement player so remote pickup rejection continues without reconnecting. |
| Claim permissions | The first-editor policy ignored claim authority. Supported providers now govern access and editing, with the original owner retained only as unclaimed fallback. Any provider denial wins, including against operators. |
| Open chest permissions | Recheck access for every custom server action and shared-state request. Revoked access no longer permits transfers or exposes saved filter profiles through an already-open menu. |
| Deposit and withdrawal | Query insertion and extraction permissions separately. Double chests require permission for both physical halves. Unsupported virtual storage menus use ordinary clicks. |
| Creative placement | Creative palette packets bypass normal slot placement. Server packet handling now rejects prohibited placement and excessive stack counts, while preserving permitted placement and clearing. |
| Number-key swaps | Apply whole-inventory caps to both sides of swaps instead of checking item type alone. |
| Advanced edits | Preserve an existing whole-inventory stack limit when editing its mode, entries or switches. |
| Oversized edits | Encode a new rule snapshot before consuming its revision. Reverting a rejected oversized edit can now restore synchronization. |
| Delayed messages | Ignore old connection/player callbacks and unsolicited chest responses; track outstanding state requests and menu identity. Keep chest editing read-only until authoritative permission arrives. |
| Lost acknowledgements | After the initial rule retries, continue at a slower interval instead of abandoning synchronization permanently. Item-moving requests retain their bounded, duplicate-safe retry behavior. |
| Insertion restrictions | Preserve the destination's own placement checks and sided hopper restrictions when applying MSMR caps. Simulation returns the exact unchanged remainder without mutating inventory. |
| Payload parsing | Validate strict JSON iteratively before recursive decoding, with a depth limit in addition to compressed and expanded size limits. Malformed nested payloads cannot force unbounded recursion. |
| Excessive requests | Add per-player weighted request budgets and rate-limited invalid-request/protection warnings. |
| Settings writes | Retry failed saves after a delay while retaining pending changes; log the initial failure without repeating it every frame. |
| Keyboard lookup | Restore search-field focus when lookup results rebuild. |

## Reduced repeated work

- Compile rule matchers once while retaining live tag membership, so tag reloads remain effective.
- Refresh quick-profile metadata when the profile book changes rather than copying it every render.
- Reuse chest serialization and unchanged encoded state replies. Permission checks still run on every request.
- Build server arrangements without constructing the client click script; update only slots whose contents change.

These remove identifiable repeated work. No frame-rate or throughput improvement is claimed without a benchmark.

## In-game verification

| Environment | Passed assertions |
| --- | ---: |
| [Fabric singleplayer regression, no optional API](verification/0.11.0/fabric-regression-singleplayer.txt) | 87 |
| [NeoForge singleplayer regression](verification/0.11.0/neoforge-regression-singleplayer.txt) | 87 |
| [Fabric singleplayer with OPAC and Fabric API](verification/0.11.0/fabric-native-opac-and-features.txt) | 79 |
| [NeoForge singleplayer with FTB Chunks and OPAC](verification/0.11.0/neoforge-native-claims-and-features.txt) | 83 |
| [Fabric singleplayer automation without Fabric API](verification/0.11.0/fabric-no-api-automation.txt) | 71 |
| [Fabric dedicated MSMR server, networking faults](verification/0.11.0/fabric-multiplayer.txt) | 50 |
| [NeoForge dedicated MSMR server, networking faults](verification/0.11.0/neoforge-multiplayer.txt) | 50 |
| [Fabric client on vanilla server, no Fabric API](verification/0.11.0/fabric-vanilla-server.txt) | 27 |
| [NeoForge client on vanilla server](verification/0.11.0/neoforge-vanilla-server.txt) | 27 |

**Pickup and movement:** actual Survival drops; all four modes; pickup/movement switches; slot caps and partial stacks; number-key swaps; Creative rejection, exact caps and clearing; death and respawn on both dedicated servers.

**Profiles and UI:** save, copy, delete and switch; independent chest rules; compact layouts; five shortcuts, defaults, custom letter/item icon and colour, visibility and persistence; Advanced categories and independent toggles. Mining/Combat swaps, missing reserved items, full-inventory swaps and impossible arrangements preserve items.

**Chests and automation:** distinct chests, barrels, trapped chests, double-chest ordering and splitting, dimensions, replacement blocks, reloads, reconnects and block NBT. Real ticking hoppers, vanilla transfer helpers, another non-operator player, Fabric transfer transactions and rollback, and NeoForge wrappers and simulation obey shared insertion rules and exact remainders.

**Networking:** deliberately lost rule acknowledgements, shared-edit replies and operation replies; identical-request retries; stale state, revisions and sessions; immediate closing; oversized edits and recovery. Transfers are compared with authoritative saved player data after disconnecting, then checked again after reconnecting, including counts and components.

**Protection:** synthetic providers exercise denied access, separate editing permission, double ownership policy, API failures and operator denial. Actual FTB Chunks 2101.1.22 and OPAC 0.32.7 claims verify editing by an authorized non-owner, foreign claim replacement while a menu remains open, denial despite operator status, and return to unclaimed fallback after removal. OPAC runs on both loaders; FTB Chunks runs on NeoForge.

The public unit tests cover rules, tags and lookup, profiles, layout, settings validation, count-conserving sorting, transfer/pickup planning, protocol bounds, provider decisions, request budgets and optional-method resolution. Build with JDK 21:

```sh
./gradlew :common:test :fabric:build :neoforge:build
```

In-game assertions use isolated development instances and disposable worlds. The private runner, test mods, worlds, credentials and local tooling are excluded from distribution archives and the source repository.

## Compatibility boundaries

- WorldGuard, GriefPrevention, LWC, Bolt and LockettePro have optional public-API adapters, but **have not been live tested on a hybrid server**. Treat these adapters as experimental. Plain Paper/Spigot cannot load this Fabric/NeoForge mod.
- Other claim systems need a `ServerProtection.Provider` integration. An open menu alone is not proof that every plugin permits a custom action. See the [protection guide](https://github.com/TAS-69/MSMR/wiki/Protection-and-Claims).
- A detected provider that throws or exposes incompatible methods refuses MSMR actions safely. This can make shortcuts unavailable until the integration is corrected.
- MSMR does not assign a player identity to hoppers or mobs. Saved insertion filters apply; claim systems retain their own automation policies. Mods that directly rewrite inventory lists or implement custom storage can bypass standard inventory hooks and require explicit integration.
- Client-only installations cannot prevent ground pickups or enforce rules on other players and automation. Unsupported servers retain local tools and ordinary-click transfers.
- Minecraft versions beyond 1.21.1, cross-loader networking, arbitrary plugin/mod combinations, public-server anti-cheat restrictions and physical screen-reader operation are not verified.
- Automatic refill, arbitrary NBT/component predicates and general container sorting beyond reserved player-slot arrangement remain outside the implemented feature set. Existing item components are preserved by supported transfers and arrangements.

No unresolved item-loss or duplication failure occurred in the completed suites. These results establish the tested scenarios, not universal compatibility with every server or inventory implementation.
