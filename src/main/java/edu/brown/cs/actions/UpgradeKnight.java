package edu.brown.cs.actions;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Knight;
import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Action that promotes one of a player's knights one tier, spending ore + wool
 * (Cities & Knights).
 *
 * Mighty (tier-3) knights require a level-3 Politics city improvement, per the
 * official Cities & Knights rules.
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
    // Promoting to mighty (tier 3) requires a level-4 Politics improvement
    // (the metropolis level), per the official Cities & Knights rules.
    if (knight.getTier() == Settings.MAX_KNIGHT_TIER - 1
        && _player.getImprovementLevel(CityImprovement.POLITICS) < 4) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "Mighty knights require a level-4 Politics city improvement.", null));
    }
    // Each player has exactly 2 knights per tier. The target tier after
    // upgrade is knight.getTier() + 1.
    int targetTier = knight.getTier() + 1;
    if (KnightActions.countKnightsAtTier(_ref, _player, targetTier) >= 2) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          String.format("You already have 2 tier-%d knights on the board (per-tier cap).", targetTier),
          null));
    }
    // Strong -> mighty (tier-2 -> tier-3) costs 2 ore + 1 wool; basic ->
    // strong (tier-1 -> tier-2) costs 1 ore + 1 wool. Per the rulebook.
    Map<Resource, Double> upgradeCost = knight.getTier() == Settings.MAX_KNIGHT_TIER - 1
        ? Settings.KNIGHT_MIGHTY_PROMOTE_COST : Settings.KNIGHT_UPGRADE_COST;
    if (!KnightActions.canAfford(_player, upgradeCost)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have the resources to upgrade a knight.", null));
    }

    // The Action:
    KnightActions.pay(_player, upgradeCost, _ref);
    knight.upgrade();

    return KnightActions.broadcast(_ref, _player, "You upgraded a knight.",
        String.format("%s upgraded a knight.", _player.getName()));
  }
}
