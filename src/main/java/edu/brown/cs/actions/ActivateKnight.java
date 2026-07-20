package edu.brown.cs.actions;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Knight;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Settings;

/**
 * Action that activates one of a player's knights, spending 1 grain (Cities
 * &amp; Knights).
 *
 */
public class ActivateKnight implements Action {

  public static final String ID = "activateKnight";
  private final Referee _ref;
  private final Player _player;
  private final Intersection _intersection;

  public ActivateKnight(Referee ref, int playerID, IntersectionCoordinate i) {
    assert ref != null && i != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _intersection = _ref.getBoard().getIntersections().get(i);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    if (_intersection == null) {
      throw new IllegalArgumentException("The intersection could not be found.");
    }
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

    // The Action:
    KnightActions.pay(_player, Settings.ACTIVATE_KNIGHT_COST, _ref);
    knight.activate();

    return KnightActions.broadcast(_ref, _player, "You activated a knight.",
        String.format("%s activated a knight.", _player.getName()));
  }
}
