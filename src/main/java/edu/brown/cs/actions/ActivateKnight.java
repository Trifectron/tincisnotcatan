package edu.brown.cs.actions;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Knight;
import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Settings;

/**
 * Action that activates one of a player's knights, spending 1 grain + 1
 * commodity (Cities & Knights; the player picks which commodity).
 *
 */
public class ActivateKnight implements Action {

  public static final String ID = "activateKnight";
  private final Referee _ref;
  private final Player _player;
  private final Intersection _intersection;
  private final Commodity _commodity;

  public ActivateKnight(Referee ref, int playerID, IntersectionCoordinate i,
      Commodity commodity) {
    assert ref != null && i != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _intersection = _ref.getBoard().getIntersections().get(i);
    _commodity = commodity;
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    if (_intersection == null) {
      throw new IllegalArgumentException("The intersection could not be found.");
    }
  }

  // Backward-compatible constructor when no commodity is supplied (used by
  // tests and the original API). Production API paths should pass a commodity
  // for C&K games.
  public ActivateKnight(Referee ref, int playerID, IntersectionCoordinate i) {
    this(ref, playerID, i, null);
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCitiesAndKnights) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "Knights are only available in Cities & Knights games.", null));
    }
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You cannot activate a knight when it is not your turn.", null));
    }
    Knight knight = _intersection.getKnight();
    if (knight == null || !knight.getPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have a knight at that location.", null));
    }
    if (knight.isActive()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "That knight is already active.", null));
    }
    if (!KnightActions.canAfford(_player, Settings.ACTIVATE_KNIGHT_COST)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have the grain to activate a knight.", null));
    }
    // Per the official C&K rules, activating a knight also requires spending
    // 1 commodity of any color (paper, cloth, or coin). The player picks
    // which one when they trigger this action.
    Commodity commodity = _commodity != null ? _commodity : defaultCommodity();
    if (commodity == null) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "Activating a knight requires you to spend 1 commodity of any color "
              + "(paper, cloth, or coin).", null));
    }
    if (_player.getCommodities().getOrDefault(commodity, 0.0) < 1.0) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          String.format(
              "You do not have a %s commodity to activate a knight.",
              commodity.toString()),
          null));
    }

    // The Action:
    KnightActions.pay(_player, Settings.ACTIVATE_KNIGHT_COST, _ref);
    _player.removeCommodity(commodity, 1);
    knight.activate();

    return KnightActions.broadcast(_ref, _player, "You activated a knight.",
        String.format("%s activated a knight.", _player.getName()));
  }

  private Commodity defaultCommodity() {
    for (Commodity c : Commodity.values()) {
      if (_player.getCommodities().getOrDefault(c, 0.0) >= 1.0) {
        return c;
      }
    }
    return null;
  }
}
