package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import edu.brown.cs.board.City;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;

import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCardType;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Action responsible for rolling the dice.
 * 
 * @author anselvahle
 *
 */
public class RollDice implements FollowUpAction {

  private Player _player;
  private final int _playerID;
  private Referee _ref;
  private static final String VERB = "start the next turn.";
  public static final String ID = "rollDice";
  private boolean _isSetUp = false;
  private Integer _forcedRoll;

  public RollDice(Referee ref, int playerID) {
    assert ref != null;
    _ref = ref;
    _playerID = playerID;
    _player = _ref.getPlayerByID(playerID);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    if (!ref.currentPlayer().equals(_player)) {
      throw new IllegalArgumentException();
    }
    _isSetUp = true;
  }

  public RollDice(int playerID) {
    _playerID = playerID;
  }

  /**
   * Overrides this roll's production total (Cities &amp; Knights Alchemist
   * progress card). The event die and any 7-handling still run normally;
   * only the production number changes. Must be called before
   * {@link #execute()}.
   *
   * @param roll
   *          The production total to use instead of a random roll.
   */
  public void forceRoll(int roll) {
    _forcedRoll = roll;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetUp) {
      throw new UnsupportedOperationException(
          "A FollowUpAction must be setup before executed.");
    }
    Random r = new Random();
    PrimitiveIterator.OfInt rolls = r.ints(1, 7).iterator();
    int redDie = rolls.nextInt();
    // Cities & Knights Alchemist: if the player played Alchemist before
    // rolling this turn, the chosen value overrides any random 2d6. We
    // always physically roll the event/red die so its side effect
    // (progress cards on 0/yellow, etc.) still fires naturally.
    int yellowDie = rolls.nextInt();
    int diceRoll;
    if (_forcedRoll != null) {
      diceRoll = _forcedRoll;
    } else {
      int currentPlayerID = (_ref.currentPlayer() == null) ? -1
          : _ref.currentPlayer().getID();
      if (currentPlayerID == _playerID
          && _ref.getGameSettings().isCitiesAndKnights) {
        Integer alchemised = _ref.getAlchemisedRoll();
        if (alchemised != null) {
          diceRoll = alchemised;
        } else {
          diceRoll = redDie + yellowDie;
        }
      } else {
        diceRoll = redDie + yellowDie;
      }
    }
    _ref.getGameStats().addRoll(diceRoll);
    Map<Integer, Map<Resource, Integer>> playerResourceCount = new HashMap<>();
    Map<Integer, ActionResponse> toRet = new HashMap<>();

    // Cities & Knights: roll the event die and hand out progress cards. This is
    // independent of the number rolled, so it happens on any roll (including 7).
    if (_ref.getGameSettings().isCitiesAndKnights) {
      handleEventDie(r, redDie);
    }

    if (diceRoll != 7) {
      Collection<Tile> tiles = _ref.getBoard().getTiles();
      // Iterate through tiles on the board
      for (Tile t : tiles) {
        // If the tile matches the roll and does not have the robber
        if (t.getRollNumber() == diceRoll && !t.hasRobber()) {
          boolean citiesAndKnights = _ref.getGameSettings().isCitiesAndKnights;
          // Cities & Knights: cities also produce commodities. Credit them
          // directly; there is no commodity bank scarcity yet.
          // ponytail: no commodity bank, add when token limits matter.
          if (citiesAndKnights) {
            Map<Integer, Map<Commodity, Integer>> fromTileCommodities = t
                .notifyCommodities();
            for (int playerID : fromTileCommodities.keySet()) {
              Map<Commodity, Integer> commodityCount = fromTileCommodities
                  .get(playerID);
              for (Commodity commodity : commodityCount.keySet()) {
                _ref.getPlayerByID(playerID).addCommodity(commodity,
                    commodityCount.get(commodity));
              }
            }
            // Per official C&K rule, the merchant placed on a hex also
            // earns its owner 1 of the hex's resource on every roll of
            // that hex's number (separate from the 2:1 trade benefit).
            int merchantOwner = t.getMerchantOwner();
            if (merchantOwner >= 0
                && t.getType().getType() != null) {
              _ref.getPlayerByID(merchantOwner).addResource(
                  t.getType().getType(), 1, _ref.getBank());
              if (!playerResourceCount.containsKey(merchantOwner)) {
                playerResourceCount.put(merchantOwner,
                    new HashMap<Resource, Integer>());
              }
              Map<Resource, Integer> bonusCount = playerResourceCount
                  .get(merchantOwner);
              Resource hexResource = t.getType().getType();
              bonusCount.merge(hexResource, 1, Integer::sum);
            }
          }
          // Find out who should collect what from the intersections
          Map<Integer, Map<Resource, Integer>> fromTile = t
              .notifyIntersections(citiesAndKnights);
          // Iterate through this and consolidate collections for each person
          for (int playerID : fromTile.keySet()) {
            if (!playerResourceCount.containsKey(playerID)) {
              playerResourceCount.put(playerID,
                  new HashMap<Resource, Integer>());
            }
            Map<Resource, Integer> resourceCount = fromTile.get(playerID);
            Map<Resource, Integer> playerCount = playerResourceCount
                .get(playerID);
            for (Resource res : resourceCount.keySet()) {
              if (playerCount.containsKey(res)) {
                // Update the count
                playerCount.replace(res,
                    playerCount.get(res) + resourceCount.get(res));
              } else {
                playerCount.put(res, resourceCount.get(res));
              }
              // Make sure the player collects the resource
              _ref.getPlayerByID(playerID).addResource(res,
                  resourceCount.get(res), _ref.getBank());
            }
          }
        }
      }
      for (Integer playerID : playerResourceCount.keySet()) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("%d was rolled", diceRoll));
        Map<Resource, Integer> resourceCount = playerResourceCount
            .get(playerID);
        for (Resource res : resourceCount.keySet()) {
          switch (res) {
          case WHEAT:
            message.append(String.format(", you received %d wheat",
                resourceCount.get(res)));
            break;
          case SHEEP:
            message.append(String.format(", you received %d sheep",
                resourceCount.get(res)));
            break;
          case ORE:
            message.append(String.format(", you received %d ore",
                resourceCount.get(res)));
            break;
          case BRICK:
            message.append(String.format(", you received %d brick",
                resourceCount.get(res)));
            break;
          case WOOD:
            message.append(String.format(", you received %d wood",
                resourceCount.get(res)));
            break;
          default:
            message.append(".");
            break;
          }
        }
        message.append(".");
        ActionResponse toAdd = new ActionResponse(true, message.toString(),
            resourceCount);
        toRet.put(playerID, toAdd);
      }
      for (Player p : _ref.getPlayers()) {
        if (!toRet.containsKey(p.getID())) {
          ActionResponse toAdd = new ActionResponse(true, String.format(
              "%d was rolled.", diceRoll), new HashMap<Resource, Integer>());
          toRet.put(p.getID(), toAdd);
        }
      }
    } else {
      // 7 is rolled:
      Map<Integer, Double> playersToDrop = new HashMap<>();
      Map<Integer, JsonObject> jsonToSend = new HashMap<>();
      String message = "7 was rolled.";
      for (Player p : _ref.getPlayers()) {
        if (p.getNumResourceCards() > discardThreshold(p)) {
          double numToDrop = p.getNumResourceCards() / 2.0;
          if (!_ref.getGameSettings().isDecimal) {
            numToDrop = Math.floor(numToDrop);
          }
          playersToDrop.put(p.getID(), numToDrop);
          message += String.format(" %s must discard cards", p.getName());
          JsonObject jsonForPlayer = new JsonObject();
          jsonForPlayer.addProperty("numToDrop", numToDrop);
          jsonToSend.put(p.getID(), jsonForPlayer);
        }
      }
      if (playersToDrop.size() > 0) {
        Collection<FollowUpAction> followUps = new ArrayList<>();
        message += ".";
        for (Player p : _ref.getPlayers()) {
          if (playersToDrop.containsKey(p.getID())) {
            followUps
                .add(new DropCards(p.getID(), playersToDrop.get(p.getID())));
            toRet.put(p.getID(),
                new ActionResponse(true,
                    "7 was rolled. You must drop half of your cards.",
                    jsonToSend.get(p.getID())));
          } else {
            toRet.put(p.getID(), new ActionResponse(true, message, null));
          }
        }
        _ref.addFollowUp(followUps);
      } else {
        ActionResponse respToAll = new ActionResponse(true,
            "7 was rolled. No one has more than 7 cards.", null);
        ActionResponse respToPlayer = new ActionResponse(true,
            "7 was rolled. You get to move the Robber.", null);
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), respToPlayer);
          } else {
            toRet.put(p.getID(), respToAll);
          }
        }
      }
      // Follow up MoveRobber action:
      _ref.addFollowUp(ImmutableList.of(new MoveRobber(_player.getID(), false, true)));
    }
    _ref.removeFollowUp(this);
    return toRet;
  }

  @Override
  public JsonObject getData() {
    JsonObject toRet = new JsonObject();
    toRet.addProperty("message", "Please roll the dice");
    return toRet;
  }

  @Override
  public String getID() {
    return ID;
  }

  @Override
  public int getPlayerID() {
    return _playerID;
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject params) {
    if (playerID != _playerID) {
      throw new IllegalArgumentException();
    }
    assert ref != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    if (!ref.currentPlayer().equals(_player)) {
      throw new IllegalArgumentException();
    }
    _isSetUp = true;
  }

  @Override
  public String getVerb() {
    return VERB;
  }

  /**
   * Cities &amp; Knights event die. Three of its six faces advance the barbarian
   * fleet (handled by the barbarian subsystem in a later step); the other three
   * each name an improvement track. When a track is shown, every player whose
   * improvement level on that track is at least the red die's value draws a
   * progress card from that track's deck.
   *
   * @param r
   *          The dice random source.
   * @param redDie
   *          The value of the red production die (1-6).
   */
  // The card count a player may hold before discarding on a 7. Cities & Knights
  // city walls raise it by CITY_WALL_CARD_BONUS each.
  private double discardThreshold(Player player) {
    double threshold = Settings.DROP_CARDS_THRESH;
    if (_ref.getGameSettings().isCitiesAndKnights) {
      threshold += Settings.CITY_WALL_CARD_BONUS * countWalls(player.getID());
    }
    return threshold;
  }

  private int countWalls(int playerID) {
    int walls = 0;
    for (Intersection i : _ref.getBoard().getIntersections().values()) {
      if (i.getBuilding() instanceof City
          && i.getBuilding().getPlayer().getID() == playerID
          && ((City) i.getBuilding()).hasWall()) {
        walls++;
      }
    }
    return walls;
  }

  private void handleEventDie(Random r, int redDie) {
    int eventDie = r.nextInt(6) + 1;
    CityImprovement track;
    switch (eventDie) {
    case 4:
      track = CityImprovement.TRADE;
      break;
    case 5:
      track = CityImprovement.SCIENCE;
      break;
    case 6:
      track = CityImprovement.POLITICS;
      break;
    default:
      // 1-3: the barbarian fleet advances; it attacks on reaching the island.
      _ref.getBarbarianTrack().advance();
      if (_ref.getBarbarianTrack().hasReachedIsland()) {
        _ref.resolveBarbarianAttack();
      }
      return;
    }
    for (Player p : _ref.getPlayers()) {
      if (p.getImprovementLevel(track) >= redDie) {
        ProgressCardType card = _ref.drawProgressCard(track);
        if (card != null) {
          _ref.getPlayerByID(p.getID()).addProgressCard(card);
          // C&K hand-limit rule: drawing a 5th progress card on another
          // player's turn forces an immediate discard (we discard the card
          // just drawn; UI choice can be wired in later).
          if (_ref.currentPlayer().getID() != p.getID()
              && p.getProgressCards().size() > Settings.PROGRESS_CARD_HAND_LIMIT) {
            p.removeProgressCard(card);
          }
        }
      }
    }
  }
}
