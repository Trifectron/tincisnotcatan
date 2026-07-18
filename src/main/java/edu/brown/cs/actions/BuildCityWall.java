package edu.brown.cs.actions;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.City;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Settings;

/**
 * Action that builds a city wall on one of a player's cities (Cities &amp;
 * Knights). A wall lets its owner hold extra cards before discarding on a 7.
 *
 */
public class BuildCityWall implements Action {

  public static final String ID = "buildCityWall";
  private Player _player;
  private Intersection _intersection;
  private Referee _ref;

  public BuildCityWall(Referee ref, int playerID, IntersectionCoordinate i) {
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
          "City walls are only available in Cities & Knights games.", null));
    }
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You cannot build when it is not your turn.", null));
    }
    if (!(_intersection.getBuilding() instanceof City)
        || !_intersection.getBuilding().getPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You can only build a wall on your own city.", null));
    }
    City city = (City) _intersection.getBuilding();
    if (city.hasWall()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "That city already has a wall.", null));
    }
    if (!KnightActions.canAfford(_player, Settings.CITY_WALL_COST)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have the resources to build a city wall.", null));
    }

    // The Action:
    KnightActions.pay(_player, Settings.CITY_WALL_COST, _ref);
    city.buildWall();

    return KnightActions.broadcast(_ref, _player, "You built a city wall.",
        String.format("%s built a city wall.", _player.getName()));
  }
}
