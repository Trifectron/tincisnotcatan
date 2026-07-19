# Plan: Adding Seafarers + Cities & Knights to tincisnotcatan

## Current status of repo (as of 2026-07-18)

**Read this section first — most of "Track 2 — Cities & Knights" below is
already built and the task breakdown under it is stale.** Seafarers (Track 1)
has not been started; ignore that track for now. This section is the source
of truth for what's actually implemented; the tracks below are historical
planning notes from before Cities & Knights work began.

### Already implemented (Cities & Knights)
- Commodities (`Commodity.java`), city improvement tracks and metropolises
  (`CityImprovement.java`, `Player.improveCity`/`freeAdvanceImprovement`).
- Knights: `board/Knight.java` (tier 1-3, active/inactive), `BuildKnight`,
  `ActivateKnight`, `UpgradeKnight` (gates tier-3 on Politics level 3), and
  `MoveKnight` (currently only moves to an *empty* intersection — see "Next
  task" below).
- City walls (`board/City.java` wall flag + `BuildCityWall`).
- Barbarian attacks: `BarbarianAttack.java`, `BarbarianTrack.java`, fully
  wired into `MasterReferee`/`RollDice`/`Intersection`/`Knight`.
- All 20 progress cards across all three decks (`ProgressCardType.java`):
  Science — Printer, Irrigation, Engineer, Inventor, Alchemist, Crane,
  Medicine, Mining, Smith, Road Building. Politics — Constitution, Intrigue,
  Wedding, Bishop, Diplomat, Deserter, Saboteur, Spy, Warlord (the last four
  landed 2026-07-18). Trade — Merchant, Master Merchant, Resource Monopoly,
  Trade Monopoly, Merchant Fleet.
- `api/CatanConverter.java` serializes commodities, knights, walls,
  improvement levels, merchant/merchant-fleet state, and barbarian track
  position into the game-state JSON.

### Known gaps (real C&K rules not yet matched by the code)
Ordered roughly by how self-contained the fix is:
1. ~~**Knight displacement in `MoveKnight.java`**~~ — done (`bebfe30`,
   `a7579b0`). `MoveKnightTest.java` is green.
2. **Progress-card deck multiplicities** — the real Politics/Science/Trade
   decks have multiple copies of most cards (e.g. 3x Spy, 2x each of most
   others); `ProgressCardDeck` currently holds exactly one of each type.
3. **Per-tier knight supply cap** — in progress, see "Next task" below.
4. **4-card progress-card hand limit** — real rules force an immediate
   discard when a 5th progress card is drawn (unless it's your turn). Not
   implemented anywhere.
5. **"Chase away the robber" with an active knight** — a whole missing
   player action: an active knight adjacent to the robber's hex can be
   deactivated to move the robber and steal a card, without spending a
   turn's dice-roll trigger.
6. **Wedding's commodity fallback** — real Wedding lets a victim with no
   resource cards give a commodity instead; current implementation only
   ever takes resources.
7. **Robber-move timing gate** — real rules forbid moving the robber (via
   any means) until the barbarians have reached the island for the first
   time. Not enforced.

### Next task (in progress): per-player, per-tier knight supply cap
**Spec (verified against the official rulebook, not guessed):** each player
has exactly 6 physical knight pieces — **2 basic, 2 strong, 2 mighty** — a
hard cap of 2 per tier, per player. This is *not* just "max 2 mighty
knights"; the same 2-piece cap applies to basic (build time) and strong
(first upgrade) too. Any action that would give a player a 3rd knight at a
tier must be rejected, leaving all state unchanged (no partial payment, no
partial promotion).

**Files to change (3, one gap each):**
1. `src/main/java/edu/brown/cs/actions/BuildKnight.java` — `execute()` must
   reject if the player already has 2 basic-tier (tier-1) knights on the
   board before paying/placing.
2. `src/main/java/edu/brown/cs/actions/UpgradeKnight.java` — `execute()`
   must reject if the player already has 2 knights at the *target* tier
   (i.e. 2 at tier 2 blocks a tier-1→2 upgrade; 2 at tier 3 blocks a
   tier-2→3 upgrade) before paying/upgrading. This is a different check
   from the existing Politics-level-3 gate — both must hold independently.
3. `src/main/java/edu/brown/cs/catan/ProgressCardType.java`'s `SMITH` card
   — promotes two named knights for free in one call, and currently
   validates both before applying either. The cap check must account for
   **both target knights landing on the same tier in the same call**: e.g.
   a player with 1 existing tier-3 knight who plays Smith naming two
   separate tier-2 knights to promote to tier-3 must be rejected (1 + 2 = 3
   > cap), even though a naive "check each knight against the board's
   current count" would wrongly allow it since the board hasn't changed
   yet when the second knight is checked. Simulate sequentially: as each
   named knight is validated in turn, decrement a local count at its
   current tier and increment one at its target tier, and check the running
   count against the cap — don't just re-query the live board per knight.

**Suggested shared helper:** `BuildKnight`/`UpgradeKnight` are both in
`edu.brown.cs.actions`, so a `KnightActions.countKnightsAtTier(Referee,
Player, int tier)` static helper (alongside the existing `canAfford`/`pay`/
`broadcast` helpers in that class) is the natural, DRY spot for the shared
board-scan. `ProgressCardType.java` lives in `edu.brown.cs.catan` and can't
reach package-private `KnightActions` — give `SMITH` its own local scan,
consistent with how it (and Deserter/Warlord/Spy) already do their own
board scans rather than importing across packages.

**Executable spec (all new/changed tests currently fail against the
unmodified code, confirmed via `mvn -q test` on 2026-07-18 — 4 failures,
no other regressions, 68/72 passing):**
- `src/test/java/edu/brown/cs/actions/BuildKnightTest.java` (new file) —
  `rejectsBuildingAThirdBasicKnight`: builds 2 basic knights for one player
  (both succeed), a 3rd at a distinct legal spot must fail and leave that
  spot knight-less.
- `src/test/java/edu/brown/cs/actions/UpgradeKnightTest.java` (existing
  file, 2 new tests appended) — `rejectsAThirdKnightUpgradeToStrongTier`:
  3 basic knights, upgrade the first 2 to tier 2 (both succeed), the 3rd
  upgrade attempt must fail and leave that knight at tier 1.
  `rejectsAThirdKnightUpgradeToMightyTier`: same shape one tier up (3
  knights pre-set to tier 2 directly via `Knight.upgrade()`, Politics
  advanced to level 3, first 2 upgrades to tier 3 succeed, 3rd must fail
  and leave that knight at tier 2).
- `src/test/java/edu/brown/cs/catan/ProgressCardTest.java` (existing file,
  1 new test appended) — `smithRespectsThePerTierMightyKnightCap`: the
  simultaneous-same-tier-promotion edge case described above. Asserts the
  message contains `"cap"` (case-insensitive) — pick wording that satisfies
  that, doesn't need to match exactly.

**Verify with:**
`mvn -q test -Dtest=BuildKnightTest,UpgradeKnightTest,ProgressCardTest` for
a fast loop, then the full `mvn -q test` (run twice) before considering it
done — this repo's convention is two clean consecutive runs to catch
flakiness before a commit.

---

Based on the actual repo structure (`seansegal/tincisnotcatan`): a Java backend
(`edu.brown.cs.board`, `edu.brown.cs.catan`, `edu.brown.cs.actions`,
`edu.brown.cs.api`, `edu.brown.cs.networking`) driving a JSON game-state API,
rendered by a jQuery/Freemarker frontend (`static/js/*.js`, `spark/template/freemarker/*.ftl`).

Team of 3. Work is split so each dev owns a vertical slice (backend model →
actions → frontend) rather than "all backend" / "all frontend", so nobody is
blocked waiting on someone else's package.

- **Dev A — Board & Shared Foundation, then Seafarers**
- **Dev B — Game Rules Engine, then Cities & Knights**
- **Dev C — API/Networking glue + Frontend for both expansions**

---

## Phase 0 — Shared Foundation (Dev A leads, ~3-5 days, blocks everyone)

Both expansions need the same three things done first: expansion toggles,
irregular boards, and a generalized "piece" model. Do this before splitting.

**Files to modify:**
- `src/main/java/edu/brown/cs/catan/Settings.java`, `GameSettings.java`
  — add `isSeafarers`, `isCitiesAndKnights` booleans (mirrors existing
  `isDecimal`/`isDynamic`/`isStandard` pattern).
- `src/main/java/edu/brown/cs/board/TileType.java`
  — add `WATER`, `GOLD`, `DESERT_ISLAND` (extend the existing enum).
- `src/main/java/edu/brown/cs/board/Board.java`
  — board generation currently assumes a fixed hex layout; needs a
  layout-selection path (`standard`, `seafarers-<scenario>`) instead of one
  hardcoded shape. This is the highest-risk file change in the whole project.
- `src/main/java/edu/brown/cs/catan/Setup.java`
  — dispatch to the right board generator based on `Settings`.
- `src/main/java/edu/brown/cs/board/HexCoordinate.java`
  — verify coordinate math supports non-contiguous/irregular placement
    (islands with gaps). Likely fine as-is, needs test coverage.

**New files:**
- `src/main/java/edu/brown/cs/board/BoardLayout.java` — interface/enum for
  selectable board shapes (standard hex vs. Seafarers scenario maps).
- `src/main/java/edu/brown/cs/catan/Expansion.java` — enum
  `{BASE, SEAFARERS, CITIES_AND_KNIGHTS}` used by Settings + Referee to gate
  rule branches.

**Frontend:**
- `src/main/resources/static/js/board.js` — rendering currently assumes the
  standard hex grid; needs to draw from whatever tile coordinate list the API
  sends rather than a fixed shape.

**Exit criteria:** a game can be created with `isStandard=false` and render an
irregular board shape end-to-end (even with placeholder water tiles), and
existing base-game tests still pass.

---

## Track 1 — Seafarers (Dev A)

### New concepts
Ships (movable roads over water), water/coastal tiles, gold-resource hexes,
non-contiguous island boards, "longest road" reachability across ships.

### Files to modify
| File | Change |
|---|---|
| `board/Board.java` | Generate island layouts; track water vs. land adjacency for building legality (settlements still need land). |
| `board/BoardTile.java` | Support `WATER`/`GOLD` tile behavior (gold hexes trigger a resource-choice FollowUp on production instead of a fixed resource). |
| `board/Path.java`, `board/PathCoordinate.java` | A path can now host a `Ship` instead of a `Road`; need a `pieceType` distinction. |
| `catan/MasterReferee.java` | Longest-road calculation must include ship chains; road-building rules (can't build road past a settlement) apply analogously to ships; add gold-hex resource-choice trigger on dice roll. |
| `api/CatanConverter.java` | Serialize water tiles, ships, and gold-tile pending-choice state into the JSON game state. |
| `api/ActionFactory.java` | Register new action types (see below). |
| `graph/Graphs.java` | Longest-road/ship pathfinding must treat ship and road edges compatibly (or as a combined graph with a "must connect to a settlement to switch types" rule — the actual Seafarers constraint). |

### New files (backend)
- `board/Ship.java` — extends/parallels `Road.java`; movable (can relocate an
  open-ended ship each turn) unlike roads.
- `board/GoldHex.java` (or a flag on `BoardTile`) — resource-choice production.
- `actions/BuildShip.java`, `actions/MoveShip.java` — new `Action`
  implementations, modeled directly on `actions/BuildRoad.java`.
- `actions/ChooseGoldResource.java` — new `FollowUpAction`, modeled on
  `actions/PlayYearOfPlenty.java` (same "pick N resources" shape).
- `catan/SeafarersSetup.java` — scenario board loader (start with one
  scenario, e.g. "Heading for New Shores," before generalizing).

### Frontend
- `static/js/ship.js` — new file, parallel to `static/js/path.js`, renders
  ship pieces and drag/relocate interaction.
- `static/js/tile.js` — extend to render water tiles and gold hexes.
- `static/js/board.js` — support non-hex-grid (island) layouts.
- `spark/template/freemarker/board.ftl` — build-tab button for "Build Ship."

### Assets needed
- `icon-ship.svg`
- Water tile texture/sprite (`images/tile-water.png` or SVG pattern)
- Gold hex tile texture (`images/tile-gold.png`)
- Optional: small "pirate ship" icon if the scenario’s pirate-blocking rule
  is implemented (can be deferred — see Out of Scope below).

### Logic checklist
- [ ] Ships placed only on water/coastal paths adjacent to a settlement or another ship.
- [ ] Ships can be "opened" and relocated once per turn if not enclosed by two settlements/other ships.
- [ ] Longest-road counts continuous road+ship chains.
- [ ] Gold hex production triggers a `ChooseGoldResource` FollowUp per player who has an adjacent settlement/city.
- [ ] New board generator produces a valid, connected land/water scenario map.

### Out of scope for v1 (call out explicitly in plan)
Pirate ship robber-equivalent, individual scenario special rules (Fog Islands,
Through the Desert, etc.) — ship one scenario, generalize later.

---

## Track 2 — Cities & Knights (Dev B)

### New concepts
Commodities (paper/cloth/coin) alongside resources, progress cards (in place
of dev cards), knights (activatable defensive units), city walls, city
improvements (Trade/Politics/Science tracks → metropolises), and the barbarian
attack — a turn-independent trigger, not a player action.

### Files to modify
| File | Change |
|---|---|
| `catan/Resource.java` | Add commodity types (paper, cloth, coin) as a parallel enum or a `Commodity` sibling class. |
| `catan/DevelopmentCard.java` | Either extend or replace with `ProgressCard.java` (Politics/Trade/Science decks) — recommend a new class + keep old one for base-game compatibility via the `Expansion` flag from Phase 0. |
| `catan/Player.java`, `HumanPlayer.java` | Track knight count/tier, city walls, improvement levels per track, metropolis ownership. |
| `catan/MasterReferee.java` | Biggest change: barbarian attack strength calculation (sum of active knights vs. barbarian fleet size) fires on specific dice rolls (7s in C&K replace the robber trigger with a "black die" mechanic) — this needs its own subsystem, not a bolt-on. |
| `catan/Turn.java` | Add "activate knight" and "improve city" as in-turn actions. |
| `board/City.java` | Add wall flag (increases hand-size limit / defense). |
| `api/CatanConverter.java` | Serialize commodities, knight state, wall state, improvement tracks, barbarian fleet position. |
| `api/ActionFactory.java` | Register new actions/follow-ups. |

### New files (backend)
- `catan/Commodity.java` — parallel to `Resource.java`.
- `catan/ProgressCard.java` + `catan/ProgressCardDeck.java` — three decks
  (Politics, Trade, Science), each with distinct effects; modeled on
  `catan/DevelopmentCard.java` but effect-per-card rather than 5 fixed types.
- `board/Knight.java` — piece with tier (1-3) and activated/deactivated state.
- `board/CityWall.java` (or boolean+field on `City.java`).
- `catan/BarbarianTrack.java` — tracks fleet progress toward the island,
  independent of turn order (advances on certain dice rolls).
- `actions/BuildKnight.java`, `actions/ActivateKnight.java`,
  `actions/UpgradeKnight.java`, `actions/MoveKnight.java`.
- `actions/BuildCityWall.java`.
- `actions/ImproveCity.java` (spend commodities on a track).
- `actions/PlayProgressCard.java`.
- `actions/BarbarianAttack.java` — new `FollowUpAction`, fires automatically
  from `MasterReferee`, not from a player-initiated `Action`.

### Frontend
- `static/js/knight.js` — new file, renders knight pieces at intersections
  (tier/activation state).
- `static/js/player.js` — extend player tab with commodity counts, knight
  count, improvement track progress bars.
- `static/js/board.js` — render city walls, barbarian fleet position/track.
- `spark/template/freemarker/board.ftl` — new build-tab entries (knight,
  city wall, improve city) and a barbarian-track UI element.

### Assets needed
- `icon-knight-tier1.svg`, `-tier2.svg`, `-tier3.svg` (basic/strong/mighty)
- `icon-commodity-paper.svg`, `-cloth.svg`, `-coin.svg`
- `icon-city-wall.svg`
- `icon-progress-card-politics.svg`, `-trade.svg`, `-science.svg` (card backs)
- Barbarian ship/fleet icon + track UI graphic
- Metropolis building sprite (per improvement track color)

### Logic checklist
- [ ] Barbarian fleet advances on a defined trigger, tracked server-side, independent of `EndTurn`.
- [ ] Attack resolution: total active knight strength vs. fleet size, per-player consequences (weakest defender loses a city→settlement) on loss, strongest contributor rewarded on win.
- [ ] Knights can be activated (cost 1 commodity), upgraded (tier), and moved along roads.
- [ ] Progress cards resolve per-card effects (this is the largest single chunk of bespoke logic — recommend a `ProgressCardEffect` strategy interface, one implementation per card).
- [ ] City improvement tracks gate which progress cards a player can draw from which deck.
- [ ] Metropolis granted to the player with the highest level (4+) on a track; worth bonus VPs; can be lost if outleveled.

### Out of scope for v1
Aqueduct/Cathedral-style scenario variants, "Robber Knight" edge cases,
event-die tie-breaking rules beyond the basic majority rule.

---

## Track 3 — API/Networking + Cross-Cutting Frontend (Dev C)

Dev C doesn't own new game rules but owns making both expansions actually
reachable end-to-end: settings UI, state sync, and the shared UI chrome. This
person is the integration point between A and B and should pair with each
briefly when their action types land.

### Files to modify
| File | Change |
|---|---|
| `api/CatanAPI.java` | Wire new `Expansion` setting through game creation. |
| `api/ActionProcessor.java` | Ensure new FollowUpActions (`ChooseGoldResource`, `BarbarianAttack`) are dispatched correctly — these are FollowUps that can be *server-triggered*, not just client-initiated, which is a new pattern the existing processor doesn't have. |
| `api/GetGameStateProcessor.java` | Include new board/player/commodity fields in outgoing state. |
| `spark/template/freemarker/home.ftl`, `static/js/home.js` | Game-creation form: add "Seafarers" / "Cities & Knights" checkboxes next to the existing decimal/dynamic-rate options. |
| `static/js/main.js` | Route new server push events (barbarian attack resolution, gold-choice prompt) to the right UI popups, same pattern as existing Monopoly/Year-of-Plenty dialogs. |
| `static/css/main.css` | Styling for new tabs/tracks/pieces. |

### New files
- `static/js/expansion-settings.js` — home-screen expansion toggle logic (only show C&K improvement tracks / Seafarers ship button if that expansion is enabled for the game).
- `spark/template/freemarker/board.ftl` sections for: barbarian track panel,
  improvement track panel, ship build button (conditionally rendered based on
  which expansions are active).

### Logic checklist
- [ ] Game settings correctly gate which board generator and rule branches are used (`Expansion.BASE` games must be byte-for-byte unaffected).
- [ ] Both expansions can be enabled simultaneously without conflicting (shared board must support both water tiles and knights/walls).
- [ ] All new FollowUpActions round-trip correctly over the existing websocket `Networking` layer with no changes needed to `networking/*.java` (it's transport-agnostic, so this should mostly be an integration-test task, not new code).

---


## Testing
Mirror the existing `src/test/java/edu/brown/cs/...` structure:
- `test/java/edu/brown/cs/board/ShipTest.java`, `GoldHexTest.java`
- `test/java/edu/brown/cs/catan/BarbarianTrackTest.java`, `ProgressCardTest.java`
- `test/java/edu/brown/cs/actions/BuildShipTest.java`, `ActivateKnightTest.java`, etc. (one per new `Action`, following the existing one-test-per-action convention already in the repo).
