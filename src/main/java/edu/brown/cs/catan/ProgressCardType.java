package edu.brown.cs.catan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.brown.cs.actions.DropCards;
import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.actions.MoveRobber;
import edu.brown.cs.actions.PlaceRoad;
import edu.brown.cs.actions.RollDice;
import edu.brown.cs.board.City;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Knight;
import edu.brown.cs.board.Path;
import edu.brown.cs.board.PathCoordinate;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;

/**
 * The catalog of Cities & Knights progress cards. Each card belongs to one
 * improvement deck (Trade, Politics, or Science) and carries its own effect.
 *
 * Cards that need a player-chosen target take it as a string in
 * {@link #play(Referee, Player, String)}: a resource name (Resource
 * Monopoly, Trade Monopoly, Merchant Fleet), an improvement track name
 * (Crane), one tile coordinate (Merchant), two semicolon-separated tile
 * coordinates (Inventor), a roll total from 2-12 (Alchemist), one
 * intersection of three pipe-separated tile coordinates (Medicine), or two
 * semicolon-separated intersections, each three pipe-separated tile
 * coordinates (Diplomat, Smith); cards that don't need a target ignore the
 * parameter.
 *
 */
public enum ProgressCardType {

  // Science deck.
  PRINTER(CityImprovement.SCIENCE, "Printer", (ref, player, target) -> {
    player.addVictoryPoints(1);
    return "You played Printer and gained a victory point.";
  }),

  // Irrigation gives 2 grain for each grain hex adjacent to at least one of
  // the player's settlements or cities, counted once per hex.
  IRRIGATION(CityImprovement.SCIENCE, "Irrigation", (ref, player, target) -> {
    int hexes = 0;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getType() != TileType.WHEAT) {
        continue;
      }
      for (Intersection i : tile.getIntersections()) {
        if (i.getBuilding() != null
            && i.getBuilding().getPlayer().equals(player)) {
          hexes++;
          break;
        }
      }
    }
    int wheat = hexes * 2;
    if (wheat > 0) {
      player.addResource(Resource.WHEAT, wheat, ref.getBank());
    }
    return wheat > 0
        ? String.format("You played Irrigation and received %d wheat.", wheat)
        : "You played Irrigation but have no settlements or cities on a wheat hex.";
  }),

  ENGINEER(CityImprovement.SCIENCE, "Engineer", (ref, player, target) -> {
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() instanceof City && i.getBuilding().getPlayer()
          .equals(player) && !((City) i.getBuilding()).hasWall()) {
        ((City) i.getBuilding()).buildWall();
        return "You played Engineer and built a free city wall.";
      }
    }
    return "You played Engineer but have no unwalled city to fortify.";
  }),

  // Inventor's target is two tile coordinates, "x1,y1,z1;x2,y2,z2".
  INVENTOR(CityImprovement.SCIENCE, "Inventor", (ref, player, target) -> {
    String[] coords = target.split(";");
    HexCoordinate first = parseHexCoordinate(coords[0]);
    HexCoordinate second = parseHexCoordinate(coords[1]);
    Tile firstTile = null;
    Tile secondTile = null;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getCoordinate().equals(first)) {
        firstTile = tile;
      } else if (tile.getCoordinate().equals(second)) {
        secondTile = tile;
      }
    }
    if (firstTile == null || secondTile == null) {
      return "You played Inventor but named a tile that isn't on the board.";
    }
    if (isUnswappableNumber(firstTile.getRollNumber())
        || isUnswappableNumber(secondTile.getRollNumber())) {
      return "You played Inventor but can't move a 2, 6, 8, or 12 token.";
    }
    int firstNum = firstTile.getRollNumber();
    firstTile.setRollNumber(secondTile.getRollNumber());
    secondTile.setRollNumber(firstNum);
    return "You played Inventor and swapped two tiles' numbers.";
  }),

  // Alchemist's target is the roll total to use instead of a random roll,
  // e.g. "8". Cities & Knights rulebook: "Played before rolling dice; pick
  // a value from 2-12." So either the player is sitting on their turn with
  // no roll yet (pre-roll case -> store the forced roll on the Turn; the
  // next RollDice.execute() for this turn will pick it up so the actual
  // production happens through normal channels), or production is already
  // pending (Ride-or-post case -> the previously-queued RollDice for the
  // player can be forced and executed immediately).
  ALCHEMIST(CityImprovement.SCIENCE, "Alchemist", (ref, player, target) -> {
    int roll;
    try {
      roll = Integer.parseInt(target);
    } catch (NumberFormatException e) {
      return "You played Alchemist but didn't name a valid roll.";
    }
    if (roll < 2 || roll > 12) {
      return "You played Alchemist but named a roll outside 2-12.";
    }
    FollowUpAction pending = ref.getNextFollowUp(player.getID());
    if (pending instanceof RollDice) {
      RollDice rollAction = (RollDice) pending;
      rollAction.setupAction(ref, player.getID(), new JsonObject());
      rollAction.forceRoll(roll);
      rollAction.execute();
      return String.format(
          "You played Alchemist and forced the dice to produce %d.", roll);
    }
    // Pre-roll case: stash the forced roll on the current Turn so the
    // upcoming RollDice.execute() will consume it rather than calling
    // the RNG (Cities & Knights rulebook: Alchemist is "played before
    // rolling dice; pick a value from 2-12"). We do not gate on the
    // caller's identity here — the player playing the card is the one
    // whose dice will be forced; RollDice will only honour the flag
    // when its own player-ID matches the current player at execution
    // time, so a card played by a non-current player can't be smuggled
    // across turns.
    ref.setAlchemisedRoll(roll);
    return String.format(
        "You played Alchemist; the next dice roll you make will produce %d.",
        roll);
  }),

  // Crane's target is the name of an improvement track ("trade", "politics",
  // or "science"). Advances the player's level on that track for 1 fewer
  // commodity than normal (a first-level improvement becomes free).
  CRANE(CityImprovement.SCIENCE, "Crane", (ref, player, target) -> {
    CityImprovement track;
    try {
      track = CityImprovement.fromString(target);
    } catch (Exception e) {
      return "You played Crane but didn't name a valid track.";
    }
    int level = player.getImprovementLevel(track);
    if (level >= CityImprovement.MAX_LEVEL) {
      return "You played Crane but that track is already maximized.";
    }
    int discountedCost = level;
    if (!player.hasCommodity(track.getCommodity(), discountedCost)) {
      return "You played Crane but can't afford the discounted improvement.";
    }
    player.removeCommodity(track.getCommodity(), discountedCost);
    player.freeAdvanceImprovement(track);
    return String.format(
        "You played Crane and upgraded %s to level %d for 1 fewer commodity.",
        track.toString().toLowerCase(),
        player.getImprovementLevel(track));
  }),

  // Medicine's target is one intersection, three pipe-separated tile
  // coordinates, naming a settlement of the player's to upgrade to a city
  // for 2 ore and 1 grain instead of the normal 3 ore and 2 grain.
  MEDICINE(CityImprovement.SCIENCE, "Medicine", (ref, player, target) -> {
    IntersectionCoordinate coord;
    try {
      coord = parseIntersectionCoordinate(target);
    } catch (Exception e) {
      return "You played Medicine but didn't name a valid intersection.";
    }
    Intersection settlement = ref.getBoard().getIntersections().get(coord);
    if (settlement == null || !settlement.canPlaceCity(player)) {
      return "You played Medicine but that isn't your settlement.";
    }
    if (player.numCities() <= 0) {
      return "You played Medicine but have no cities left to build.";
    }
    if (player.getResources().get(Resource.ORE) < 2
        || player.getResources().get(Resource.WHEAT) < 1) {
      return "You played Medicine but can't afford the discounted upgrade.";
    }
    player.removeResource(Resource.ORE, 2);
    player.removeResource(Resource.WHEAT, 1);
    player.useCity();
    settlement.placeCity(player);
    return "You played Medicine and upgraded a settlement to a city for 2 "
        + "ore and 1 grain.";
  }),

  // Mining gives 2 ore for each ore hex adjacent to at least one of the
  // player's settlements or cities, counted once per hex.
  MINING(CityImprovement.SCIENCE, "Mining", (ref, player, target) -> {
    int hexes = 0;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getType() != TileType.ORE) {
        continue;
      }
      for (Intersection i : tile.getIntersections()) {
        if (i.getBuilding() != null
            && i.getBuilding().getPlayer().equals(player)) {
          hexes++;
          break;
        }
      }
    }
    int ore = hexes * 2;
    if (ore > 0) {
      player.addResource(Resource.ORE, ore, ref.getBank());
    }
    return ore > 0
        ? String.format("You played Mining and received %d ore.", ore)
        : "You played Mining but have no settlements or cities on an ore hex.";
  }),

  // Smith's target is two semicolon-separated intersections, each three
  // pipe-separated tile coordinates, naming two of the player's knights.
  // Promotes both one tier each for free; the mighty-knight (tier 3)
  // Politics-3 gate from UpgradeKnight still applies.
  SMITH(CityImprovement.SCIENCE, "Smith", (ref, player, target) -> {
    String[] parts = target.split(";");
    if (parts.length != 2) {
      return "You played Smith but didn't name two knights.";
    }
    Intersection[] spots = new Intersection[2];
    for (int i = 0; i < 2; i++) {
      IntersectionCoordinate coord;
      try {
        coord = parseIntersectionCoordinate(parts[i]);
      } catch (Exception e) {
        return "You played Smith but didn't name a valid intersection.";
      }
      spots[i] = ref.getBoard().getIntersections().get(coord);
    }
    if (spots[0] == spots[1]) {
      return "You played Smith but named the same knight twice.";
    }
    Knight[] knights = new Knight[2];
    for (int i = 0; i < 2; i++) {
      Knight knight = spots[i] == null ? null : spots[i].getKnight();
      if (knight == null || !knight.getPlayer().equals(player)) {
        return "You played Smith but named a location with no knight of "
            + "yours.";
      }
      if (!knight.canUpgrade()) {
        return "You played Smith but one of those knights is already at "
            + "the highest tier.";
      }
      if (knight.getTier() == Settings.MAX_KNIGHT_TIER - 1
          && player.getImprovementLevel(CityImprovement.POLITICS) < 4) {
        return "You played Smith but mighty knights require a level-4 "
            + "Politics city improvement.";
      }
      knights[i] = knight;
    }
    // Per-tier supply cap: each player has exactly 2 knights per tier.
    // Simulate the two promotions sequentially to catch the case where both
    // land on the same target tier.
    int[] currentCounts = new int[4]; // tiers 1-3; index 0 unused
    for (int i = 1; i <= 3; i++) {
      currentCounts[i] = countKnightsAtTier(ref, player, i);
    }
    for (int i = 0; i < 2; i++) {
      int targetTier = knights[i].getTier() + 1;
      if (currentCounts[targetTier] >= 2) {
        return String.format(
            "You played Smith but already have 2 tier-%d knights (per-tier cap).",
            targetTier);
      }
      // Simulate this promotion for the next iteration's check.
      currentCounts[knights[i].getTier()]--;
      currentCounts[targetTier]++;
    }
    knights[0].upgrade();
    knights[1].upgrade();
    return "You played Smith and promoted two knights one tier each, for "
        + "free.";
  }),

  // Road Building lets the player place two free roads, just like the
  // base-game development card.
  ROAD_BUILDING(CityImprovement.SCIENCE, "Road Building", (ref, player,
      target) -> {
    boolean canPlace = false;
    for (Path p : ref.getBoard().getPaths().values()) {
      if (p.canPlaceRoad(player)) {
        canPlace = true;
        break;
      }
    }
    if (!canPlace) {
      return "You played Road Building but have no legal road placements.";
    }
    ref.addFollowUp(ImmutableList.of(new PlaceRoad(player.getID(), false)));
    ref.addFollowUp(ImmutableList.of(new PlaceRoad(player.getID(), false)));
    return "You played Road Building. Place two free roads.";
  }),

  // Politics deck.
  CONSTITUTION(CityImprovement.POLITICS, "Constitution", (ref, player, target) -> {
    player.addVictoryPoints(1);
    return "You played Constitution and gained a victory point.";
  }),

  INTRIGUE(CityImprovement.POLITICS, "Intrigue", (ref, player, target) -> {
    Knight strongest = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      Knight k = i.getKnight();
      if (k != null && k.isActive() && !k.getPlayer().equals(player)
          && (strongest == null || k.getTier() > strongest.getTier())) {
        strongest = k;
      }
    }
    if (strongest == null) {
      return "You played Intrigue but no opposing knight is active.";
    }
    strongest.deactivate();
    return String.format(
        "You played Intrigue and deactivated %s's tier-%d knight.",
        strongest.getPlayer().getName(), strongest.getTier());
  }),

  WEDDING(CityImprovement.POLITICS, "Wedding", (ref, player, target) -> {
    int received = 0;
    int resourcesTaken = 0;
    int commoditiesTaken = 0;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)
          || other.numVictoryPoints() <= player.numVictoryPoints()) {
        continue;
      }
      for (int i = 0; i < 2; i++) {
        // Prefer to take the victim's most-plentiful resource; if they
        // hold no resources at all, fall back to their most-plentiful
        // commodity per the official Cities & Knights rule.
        Resource biggestRes = null;
        double maxRes = 0;
        for (Map.Entry<Resource, Double> entry : other.getResources()
            .entrySet()) {
          if (entry.getValue() > maxRes) {
            maxRes = entry.getValue();
            biggestRes = entry.getKey();
          }
        }
        if (biggestRes != null) {
          other.removeResource(biggestRes, 1, ref.getBank());
          player.addResource(biggestRes, 1, ref.getBank());
          received++;
          resourcesTaken++;
          continue;
        }
        Commodity biggestCom = null;
        double maxCom = 0;
        for (Map.Entry<Commodity, Double> entry : other.getCommodities()
            .entrySet()) {
          if (entry.getValue() > maxCom) {
            maxCom = entry.getValue();
            biggestCom = entry.getKey();
          }
        }
        if (biggestCom == null) {
          break;
        }
        other.removeCommodity(biggestCom, 1);
        player.addCommodity(biggestCom, 1);
        received++;
        commoditiesTaken++;
      }
    }
    if (received == 0) {
      return "You played Wedding but no player has more victory points than you.";
    }
    if (commoditiesTaken == 0) {
      return String.format(
          "You played Wedding and received %d resource card(s).",
          resourcesTaken);
    }
    if (resourcesTaken == 0) {
      return String.format(
          "You played Wedding and received %d commodity card(s).",
          commoditiesTaken);
    }
    return String.format(
        "You played Wedding and received %d card(s) (%d resource, %d commodity).",
        received, resourcesTaken, commoditiesTaken);
  }),

  BISHOP(CityImprovement.POLITICS, "Bishop", (ref, player, target) -> {
    Collection<FollowUpAction> followUp = new ArrayList<>();
    followUp.add(MoveRobber.stealFromAll(player.getID()));
    ref.addFollowUp(followUp);
    return "You played Bishop. Move the robber; everyone on that hex must "
        + "give you a card.";
  }),

  // Diplomat's target is two semicolon-separated intersections, each three
  // pipe-separated tile coordinates: "x,y,z|x,y,z|x,y,z;x,y,z|x,y,z|x,y,z".
  DIPLOMAT(CityImprovement.POLITICS, "Diplomat", (ref, player, target) -> {
    String[] intersections = target.split(";");
    if (intersections.length != 2) {
      return "You played Diplomat but didn't name a road.";
    }
    IntersectionCoordinate start = parseIntersectionCoordinate(
        intersections[0]);
    IntersectionCoordinate end = parseIntersectionCoordinate(intersections[1]);
    Path path = ref.getBoard().getPaths().get(new PathCoordinate(start, end));
    if (path == null || path.getRoad() == null) {
      return "You played Diplomat but named a location with no road.";
    }
    // ponytail: the real rule requires the road be "open" (not sealed between
    // two buildings on both ends); exact official wording is uncertain, so
    // this treats a road with at least one building-free endpoint as open.
    if (path.getStart().getBuilding() != null
        && path.getEnd().getBuilding() != null) {
      return "You played Diplomat but that road is sealed between two "
          + "buildings.";
    }
    Player owner = path.getRoad().getPlayer();
    path.removeRoad();
    return String.format("You played Diplomat and removed %s's road.",
        owner.getName());
  }),

  // Deserter's target is an opponent's name, optionally followed by
  // ";x,y,z|x,y,z|x,y,z" naming an intersection to place a replacement
  // knight of your own. The opponent loses their weakest knight (standing
  // in for "their choice"), which you may replace at equal strength.
  DESERTER(CityImprovement.POLITICS, "Deserter", (ref, player, target) -> {
    String[] parts = target == null ? new String[0] : target.split(";", 2);
    Player opponent = parts.length > 0 ? findPlayerByName(ref, parts[0])
        : null;
    if (opponent == null || opponent.equals(player)) {
      return "You played Deserter but didn't name a valid opponent.";
    }
    Intersection removedSpot = null;
    Knight removed = null;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      Knight k = i.getKnight();
      if (k != null && k.getPlayer().equals(opponent)
          && (removed == null || k.getTier() < removed.getTier())) {
        removed = k;
        removedSpot = i;
      }
    }
    if (removed == null) {
      return String.format("You played Deserter but %s has no knights.",
          opponent.getName());
    }
    int tier = removed.getTier();
    removedSpot.removeKnight();
    if (parts.length == 2) {
      Intersection newSpot;
      try {
        newSpot = ref.getBoard().getIntersections()
            .get(parseIntersectionCoordinate(parts[1]));
      } catch (Exception e) {
        newSpot = null;
      }
      if (newSpot != null && newSpot.canPlaceKnight(player.getID())) {
        Knight replacement = new Knight(player);
        while (replacement.getTier() < tier) {
          replacement.upgrade();
        }
        newSpot.setKnight(replacement);
        return String.format(
            "You played Deserter, removed %s's tier-%d knight, and placed "
                + "your own tier-%d knight.",
            opponent.getName(), tier, tier);
      }
    }
    return String.format(
        "You played Deserter and removed %s's tier-%d knight.",
        opponent.getName(), tier);
  }),

  // Saboteur has no target: every player with at least as many victory
  // points as you must discard half their resource cards (rounded up),
  // chosen by them via the same DropCards flow as a rolled 7. Per the
  // Mayfair rulebook, the discard count is ceil(n / 2) (rounding up),
  // with a minimum of 1 card dropped for any player holding at least
  // one resource card.
  // ponytail: matches the codebase's existing resource-only discard
  // convention (RollDice's 7-discard also ignores commodities).
  SABOTEUR(CityImprovement.POLITICS, "Saboteur", (ref, player, target) -> {
    Collection<FollowUpAction> followUps = new ArrayList<>();
    int affected = 0;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)
          || other.numVictoryPoints() < player.numVictoryPoints()) {
        continue;
      }
      double hasCards = other.getNumResourceCards();
      if (hasCards <= 0) {
        continue; // already empty-handed, no discard needed.
      }
      double numToDrop = Math.max(1, Math.ceil(hasCards / 2.0));
      followUps.add(new DropCards(other.getID(), numToDrop));
      affected++;
    }
    if (!followUps.isEmpty()) {
      ref.addFollowUp(followUps);
    }
    return affected > 0
        ? String.format(
            "You played Saboteur. %d player(s) must discard half their "
                + "cards (rounded up).", affected)
        : "You played Saboteur but no player has as many victory points as "
            + "you.";
  }),

  // Spy's target is "opponentName;cardName". Steals one named progress card
  // from the opponent's hand.
  SPY(CityImprovement.POLITICS, "Spy", (ref, player, target) -> {
    String[] parts = target == null ? new String[0] : target.split(";", 2);
    Player opponent = parts.length > 0 ? findPlayerByName(ref, parts[0])
        : null;
    if (opponent == null || opponent.equals(player)) {
      return "You played Spy but didn't name a valid opponent.";
    }
    if (parts.length != 2) {
      return "You played Spy but didn't name a card to steal.";
    }
    ProgressCardType stolen;
    try {
      stolen = fromString(parts[1]);
    } catch (Exception e) {
      return "You played Spy but didn't name a valid progress card.";
    }
    if (!opponent.removeProgressCard(stolen)) {
      return String.format("You played Spy but %s doesn't hold a %s card.",
          opponent.getName(), stolen.getName());
    }
    player.addProgressCard(stolen);
    return String.format("You played Spy and stole a %s card from %s.",
        stolen.getName(), opponent.getName());
  }),

  // Warlord has no target: activates every one of your knights for free.
  WARLORD(CityImprovement.POLITICS, "Warlord", (ref, player, target) -> {
    int activated = 0;
    for (Intersection i : ref.getBoard().getIntersections().values()) {
      Knight k = i.getKnight();
      if (k != null && k.getPlayer().equals(player) && !k.isActive()) {
        k.activate();
        activated++;
      }
    }
    return activated > 0
        ? String.format(
            "You played Warlord and activated %d knight(s) for free.",
            activated)
        : "You played Warlord but have no inactive knights to activate.";
  }),

  // Trade deck.
  // Merchant's target is one tile coordinate, "x,y,z".
  MERCHANT(CityImprovement.TRADE, "Merchant", (ref, player, target) -> {
    HexCoordinate coord = parseHexCoordinate(target);
    Tile destination = null;
    Tile previousTile = null;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getCoordinate().equals(coord)) {
        destination = tile;
      }
      if (tile.getMerchantOwner() >= 0) {
        previousTile = tile;
      }
    }
    if (destination == null) {
      return "You played Merchant but named a tile that isn't on the board.";
    }
    if (destination.getType().getType() == null) {
      return "You played Merchant but that hex doesn't produce a resource.";
    }
    boolean adjacentToOwnBuilding = false;
    for (Intersection i : destination.getIntersections()) {
      if (i.getBuilding() != null && i.getBuilding().getPlayer()
          .equals(player)) {
        adjacentToOwnBuilding = true;
        break;
      }
    }
    if (!adjacentToOwnBuilding) {
      return "You played Merchant but that hex isn't adjacent to one of "
          + "your settlements or cities.";
    }
    int previousOwner = previousTile == null ? -1
        : previousTile.getMerchantOwner();
    if (previousTile != null) {
      previousTile.setMerchantOwner(-1);
    }
    destination.setMerchantOwner(player.getID());
    if (previousOwner != player.getID()) {
      if (previousOwner >= 0) {
        ref.getPlayerByID(previousOwner).addVictoryPoints(-1);
      }
      player.addVictoryPoints(1);
    }
    return String.format(
        "You played Merchant and can trade %s 2:1 with the bank as long as "
            + "your merchant remains on that hex.",
        destination.getType().getType());
  }),

  MASTER_MERCHANT(CityImprovement.TRADE, "Master Merchant", (ref, player, target) -> {
    // Per the Mayfair rulebook, Master Merchant targets the opponent with
    // the most victory points (public), not the most resource cards.
    Player victimVP = null;
    int highestVP = -1;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)) {
        continue;
      }
      int publicVP = ref.getNumPublicPoints(other.getID());
      if (victimVP == null || publicVP > highestVP) {
        highestVP = publicVP;
        victimVP = other;
      }
    }
    if (victimVP == null) {
      return "You played Master Merchant but no other player is holding any "
          + "victory points.";
    }
    Player richest = victimVP;
    int received = 0;
    for (int i = 0; i < 2; i++) {
      Resource biggest = null;
      double max = 0;
      for (Map.Entry<Resource, Double> entry : richest.getResources()
          .entrySet()) {
        if (entry.getValue() > max) {
          max = entry.getValue();
          biggest = entry.getKey();
        }
      }
      if (biggest == null) {
        break;
      }
      richest.removeResource(biggest, 1, ref.getBank());
      player.addResource(biggest, 1, ref.getBank());
      received++;
    }
    return String.format(
        "You played Master Merchant and took %d resource card(s) from %s "
            + "(highest VP).",
        received, richest.getName());
  }),

  RESOURCE_MONOPOLY(CityImprovement.TRADE, "Resource Monopoly", (ref, player,
      target) -> {
    Resource res = Resource.stringToResource(target);
    double received = 0;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)) {
        continue;
      }
      double has = other.getResources().get(res);
      if (has <= 0) {
        continue;
      }
      other.removeResource(res, has, ref.getBank());
      player.addResource(res, has, ref.getBank());
      received += has;
    }
    return received > 0
        ? String.format(
            "You played Resource Monopoly and took all %s %s card(s).",
            (int) received, res.toString())
        : String.format(
            "You played Resource Monopoly but no other player has any %s.",
            res.toString());
  }),

  TRADE_MONOPOLY(CityImprovement.TRADE, "Trade Monopoly", (ref, player,
      target) -> {
    Resource res = Resource.stringToResource(target);
    int received = 0;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)) {
        continue;
      }
      double has = other.getResources().get(res);
      if (has <= 0) {
        continue;
      }
      other.removeResource(res, 1, ref.getBank());
      player.addResource(res, 1, ref.getBank());
      received++;
    }
    return received > 0
        ? String.format("You played Trade Monopoly and took %d %s card(s).",
            received, res.toString())
        : String.format(
            "You played Trade Monopoly but no other player has any %s.",
            res.toString());
  }),

  MERCHANT_FLEET(CityImprovement.TRADE, "Merchant Fleet", (ref, player,
      target) -> {
    Resource res = Resource.stringToResource(target);
    ref.setMerchantFleetResource(res);
    return String.format(
        "You played Merchant Fleet and can trade %s 2:1 with the bank "
            + "for the rest of your turn.", res.toString());
  });

  private final CityImprovement _deck;
  private final String _name;
  private final ProgressCardEffect _effect;

  private ProgressCardType(CityImprovement deck, String name,
      ProgressCardEffect effect) {
    _deck = deck;
    _name = name;
    _effect = effect;
  }

  /**
   * @return The improvement deck this card is drawn from.
   */
  public CityImprovement getDeck() {
    return _deck;
  }

  /**
   * @return The card's display name.
   */
  public String getName() {
    return _name;
  }

  /**
   * Plays this card with no target, applying its effect. Only valid for
   * cards that don't need a player-chosen target.
   *
   * @param ref
   *          The referee for the game.
   * @param player
   *          The player playing the card.
   * @return A message describing what happened.
   */
  public String play(Referee ref, Player player) {
    return play(ref, player, null);
  }

  /**
   * Plays this card, applying its effect.
   *
   * @param ref
   *          The referee for the game.
   * @param player
   *          The player playing the card.
   * @param target
   *          A player-chosen target (e.g. a resource name for Resource
   *          Monopoly); ignored by cards that don't need one.
   * @return A message describing what happened.
   */
  public String play(Referee ref, Player player, String target) {
    return _effect.apply(ref, player, target);
  }

  /**
   * @param name
   *          A card display name (case-insensitive).
   * @return The matching card.
   */
  public static ProgressCardType fromString(String name) {
    for (ProgressCardType card : values()) {
      if (card._name.equalsIgnoreCase(name)) {
        return card;
      }
    }
    throw new IllegalArgumentException(String.format(
        "No progress card named %s.", name));
  }

  // Inventor may not touch the 2, 6, 8, or 12 tokens.
  private static boolean isUnswappableNumber(int rollNumber) {
    return rollNumber == 2 || rollNumber == 6 || rollNumber == 8
        || rollNumber == 12;
  }

  // Parses "x,y,z" into a HexCoordinate, for Inventor's tile targets.
  private static HexCoordinate parseHexCoordinate(String str) {
    String[] parts = str.split(",");
    return new HexCoordinate(Integer.parseInt(parts[0]),
        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
  }

  // Counts how many knights the given player has on the board at the
  // specified tier. Used by Smith to enforce the 2-per-tier supply cap on
  // simultaneous promotions.
  private static int countKnightsAtTier(Referee ref, Player player, int tier) {
    int count = 0;
    for (edu.brown.cs.board.Intersection i : ref.getBoard()
        .getIntersections().values()) {
      edu.brown.cs.board.Knight k = i.getKnight();
      if (k != null && k.getPlayer().equals(player) && k.getTier() == tier) {
        count++;
      }
    }
    return count;
  }

  // Parses "x,y,z|x,y,z|x,y,z" into an IntersectionCoordinate, for
  // Diplomat's road targets.
  private static IntersectionCoordinate parseIntersectionCoordinate(
      String str) {
    String[] parts = str.split("\\|");
    return new IntersectionCoordinate(parseHexCoordinate(parts[0]),
        parseHexCoordinate(parts[1]), parseHexCoordinate(parts[2]));
  }

  // Finds a player by display name (case-insensitive), for cards that
  // target a specific opponent (Deserter, Spy).
  private static Player findPlayerByName(Referee ref, String name) {
    for (Player p : ref.getPlayers()) {
      if (p.getName().equalsIgnoreCase(name)) {
        return p;
      }
    }
    return null;
  }
}
