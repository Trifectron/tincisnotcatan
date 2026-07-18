package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Action that advances one of a player's Cities &amp; Knights city-improvement
 * tracks (Trade, Politics, or Science) by spending the matching commodity.
 *
 */
public class ImproveCity implements Action {

  private Player _player;
  private Referee _ref;
  private CityImprovement _improvement;
  public static final String ID = "improveCity";

  public ImproveCity(Referee ref, int playerID, String improvement) {
    assert ref != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    try {
      _improvement = CityImprovement.fromString(improvement);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException(String.format(
          "Unknown city improvement: %s", improvement));
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCitiesAndKnights) {
      ActionResponse resp = new ActionResponse(false,
          "City improvements are only available in Cities & Knights games.",
          null);
      return ImmutableMap.of(_player.getID(), resp);
    }
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      ActionResponse resp = new ActionResponse(false,
          "You cannot improve a city when it is not your turn.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }
    if (_player.getImprovementLevel(_improvement) >= CityImprovement.MAX_LEVEL) {
      ActionResponse resp = new ActionResponse(false,
          "That improvement track is already at the maximum level.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }
    if (!_player.canImproveCity(_improvement)) {
      ActionResponse resp = new ActionResponse(false,
          "You do not have enough commodities to improve that track.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }

    // The Action:
    _player.improveCity(_improvement);
    int newLevel = _player.getImprovementLevel(_improvement);

    // Formatting the response:
    ActionResponse respToPlayer = new ActionResponse(true, String.format(
        "You improved your %s track to level %d.", _improvement, newLevel),
        null);
    String message = String.format("%s improved their %s track to level %d.",
        _player.getName(), _improvement, newLevel);
    ActionResponse respToAll = new ActionResponse(true, message, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : _ref.getPlayers()) {
      if (player.equals(_player)) {
        toReturn.put(player.getID(), respToPlayer);
      } else {
        toReturn.put(player.getID(), respToAll);
      }
    }
    return toReturn;
  }
}
