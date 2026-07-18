package edu.brown.cs.catan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.actions.MoveRobber;
import edu.brown.cs.board.City;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Knight;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;

/**
 * The catalog of Cities &amp; Knights progress cards. Each card belongs to one
 * improvement deck (Trade, Politics, or Science) and carries its own effect.
 *
 * Cards that need a player-chosen target take it as a string in
 * {@link #play(Referee, Player, String)}: a resource name (Resource
 * Monopoly, Trade Monopoly, Merchant Fleet), one tile coordinate
 * (Merchant), or two semicolon-separated tile coordinates (Inventor);
 * cards that don't need a target ignore the parameter. Alchemist
 * (re-rolled dice) and Diplomat (which targets a road the player doesn't
 * yet own, needing UI beyond a text field) are not wired yet.
 *
 */
public enum ProgressCardType {

  // Science deck.
  PRINTER(CityImprovement.SCIENCE, "Printer", (ref, player, target) -> {
    player.addVictoryPoints(1);
    return "You played Printer and gained a victory point.";
  }),

  IRRIGATION(CityImprovement.SCIENCE, "Irrigation", (ref, player, target) -> {
    int wheat = 0;
    for (Tile tile : ref.getBoard().getTiles()) {
      if (tile.getType() != TileType.WHEAT) {
        continue;
      }
      for (Intersection i : tile.getIntersections()) {
        if (i.getBuilding() == null || !i.getBuilding().getPlayer()
            .equals(player)) {
          continue;
        }
        wheat += i.getBuilding() instanceof City ? 4 : 2;
      }
    }
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
    int firstNum = firstTile.getRollNumber();
    firstTile.setRollNumber(secondTile.getRollNumber());
    secondTile.setRollNumber(firstNum);
    return "You played Inventor and swapped two tiles' numbers.";
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
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)
          || other.numVictoryPoints() <= player.numVictoryPoints()) {
        continue;
      }
      for (int i = 0; i < 2; i++) {
        Resource biggest = null;
        double max = 0;
        for (Map.Entry<Resource, Double> entry : other.getResources()
            .entrySet()) {
          if (entry.getValue() > max) {
            max = entry.getValue();
            biggest = entry.getKey();
          }
        }
        if (biggest == null) {
          break;
        }
        other.removeResource(biggest, 1, ref.getBank());
        player.addResource(biggest, 1, ref.getBank());
        received++;
      }
    }
    return received > 0
        ? String.format("You played Wedding and received %d resource card(s).",
            received)
        : "You played Wedding but no player has more victory points than you.";
  }),

  BISHOP(CityImprovement.POLITICS, "Bishop", (ref, player, target) -> {
    Collection<FollowUpAction> followUp = new ArrayList<>();
    followUp.add(new MoveRobber(player.getID()));
    ref.addFollowUp(followUp);
    return "You played Bishop. Move the robber; whoever ends up on that "
        + "hex must give you a card.";
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
    int previousOwner = previousTile == null ? -1
        : previousTile.getMerchantOwner();
    if (previousTile != null) {
      previousTile.setMerchantOwner(-1);
    }
    destination.setMerchantOwner(player.getID());
    // ponytail: the real rule requires the hex be adjacent to one of your
    // settlements/cities; skipped since no other targeted card enforces
    // board-position eligibility either. Add if this gets exploited.
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
    Player richest = null;
    double richestCount = 0;
    for (Player other : ref.getPlayers()) {
      if (other.equals(player)) {
        continue;
      }
      double count = 0;
      for (double n : other.getResources().values()) {
        count += n;
      }
      // ponytail: ties keep the first player found; the real rule lets the
      // card's player break ties, which needs a target parameter to support.
      if (count > richestCount) {
        richestCount = count;
        richest = other;
      }
    }
    if (richest == null) {
      return "You played Master Merchant but no other player holds any cards.";
    }
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
        "You played Master Merchant and took %d resource card(s) from %s.",
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

  // Parses "x,y,z" into a HexCoordinate, for Inventor's tile targets.
  private static HexCoordinate parseHexCoordinate(String str) {
    String[] parts = str.split(",");
    return new HexCoordinate(Integer.parseInt(parts[0]),
        Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
  }
}
