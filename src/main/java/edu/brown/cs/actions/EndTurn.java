package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.City;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Settings;

/**
 * Action responsible for ending a turn.
 *
 * @author anselvahle
 *
 */
public class EndTurn implements Action {
  public static final String ID = "endTurn";
  private Player _player;
  private Referee _ref;
  private boolean _hasKnight = false;

  public EndTurn(Referee ref, int playerID) {
    assert ref != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }

  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "It is not your turn.", null));
    }
    // Per the official Settlers of Catan rulebook: at the end of your turn,
    // if you hold more than 7 cards (or 7 + 2 per city wall in Cities &
    // Knights), you must discard down to the threshold before play passes
    // to the next player. We compute the appropriate threshold (identical
    // to RollDice's 7-discard threshold) and queue a DropCards follow-up
    // if the acting player exceeds it.
    double threshold = discardThreshold(_player);
    double cardsHeld = _player.getNumResourceCards();
    Collection<FollowUpAction> handLimitDrop = null;
    if (cardsHeld > threshold) {
      double excess = cardsHeld - threshold;
      handLimitDrop = new ArrayList<>();
      handLimitDrop.add(new DropCards(_player.getID(), excess));
    }

    _ref.startNextTurn();
    Player nextPlayer = _ref.currentPlayer();
    if (nextPlayer.getDevCards().get(DevelopmentCard.KNIGHT) != 0) {
      _hasKnight = true;
    }
    Map<Integer, ActionResponse> toRet = new HashMap<Integer, ActionResponse>();

    for (Player p : _ref.getPlayers()) {
      if (!p.equals(nextPlayer) && !p.equals(_player)) {
        String message = String.format(
            "%s just ended their turn. It is now %s's turn.",
            _player.getName(), nextPlayer.getName());
        ActionResponse toAdd = new ActionResponse(true, message, null);
        toRet.put(p.getID(), toAdd);
      } else if (!p.equals(nextPlayer) && p.equals(_player)) {
        String message = String.format(
            "You just ended your turn. It is now %s's turn.",
            nextPlayer.getName());
        ActionResponse toAdd = new ActionResponse(true, message, null);
        toRet.put(p.getID(), toAdd);
      } else {
        FollowUpAction toDoNext = null;
        if (!_hasKnight) {
          toDoNext = new RollDice(nextPlayer.getID());
        } else {
          toDoNext = new KnightOrDice(nextPlayer.getID());
        }
        Collection<FollowUpAction> followUp = new ArrayList<FollowUpAction>();
        // Hand-limit applies first; the player must resolve the discard
        // before the next player's roll-and-production begins.
        if (handLimitDrop != null) {
          followUp.addAll(handLimitDrop);
        }
        followUp.add(toDoNext);
        _ref.addFollowUp(followUp);
        String message = String
            .format("%s just ended their turn. It is now your turn.",
                _player.getName());
        ActionResponse toAdd = new ActionResponse(true, message, null);
        toRet.put(p.getID(), toAdd);
      }
    }
    return toRet;
  }

  // Same threshold RollDice uses for the 7-discard rule, factoring in city
  // walls (Cities & Knights).
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
}
