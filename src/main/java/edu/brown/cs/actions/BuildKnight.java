package edu.brown.cs.actions;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Action that builds a new basic knight at an intersection (Cities &amp;
 * Knights).
 *
 */
public class BuildKnight implements Action {

  public static final String ID = "buildKnight";
  private Player _player;
  private Intersection _intersection;
  private Referee _ref;

  public BuildKnight(Referee ref, int playerID, IntersectionCoordinate i) {
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
          "You cannot build when it is not your turn.", null));
    }
    if (!KnightActions.canAfford(_player, Settings.KNIGHT_COST)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have the resources to build a knight.", null));
    }
    if (!_intersection.canPlaceKnight(_player.getID())) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You cannot place a knight at that location.", null));
    }

    // The Action:
    KnightActions.pay(_player, Settings.KNIGHT_COST, _ref);
    _intersection.placeKnight(_player);

    return KnightActions.broadcast(_ref, _player,
        "You built a knight.",
        String.format("%s built a knight.", _player.getName()));
  }
}
