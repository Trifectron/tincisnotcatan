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
 * Action that promotes one of a player's knights one tier, spending ore + wool
 * (Cities &amp; Knights).
 *
 * ponytail: does not yet gate tier-3 promotion on the Politics improvement
 * level; add that check when the improvement gating lands.
 *
 */
public class UpgradeKnight implements Action {

  public static final String ID = "upgradeKnight";
  private Player _player;
  private Intersection _intersection;
  private Referee _ref;

  public UpgradeKnight(Referee ref, int playerID, IntersectionCoordinate i) {
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
          "You cannot upgrade a knight when it is not your turn.", null));
    }
    Knight knight = _intersection.getKnight();
    if (knight == null || !knight.getPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have a knight at that location.", null));
    }
    if (!knight.canUpgrade()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "That knight is already at the highest tier.", null));
    }
    if (!KnightActions.canAfford(_player, Settings.KNIGHT_COST)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have the resources to upgrade a knight.", null));
    }

    // The Action:
    KnightActions.pay(_player, Settings.KNIGHT_COST, _ref);
    knight.upgrade();

    return KnightActions.broadcast(_ref, _player, "You upgraded a knight.",
        String.format("%s upgraded a knight.", _player.getName()));
  }
}
