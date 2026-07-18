package edu.brown.cs.actions;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Knight;
import edu.brown.cs.board.Path;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Action that moves an active knight to an adjacent empty intersection along one
 * of the player's roads (Cities &amp; Knights).
 *
 * ponytail: does not yet support displacing a weaker enemy knight; only moves
 * to empty intersections. Add displacement when the rule is needed.
 *
 */
public class MoveKnight implements Action {

  public static final String ID = "moveKnight";
  private Player _player;
  private Intersection _from;
  private Intersection _to;
  private Referee _ref;

  public MoveKnight(Referee ref, int playerID, IntersectionCoordinate from,
      IntersectionCoordinate to) {
    assert ref != null && from != null && to != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _from = _ref.getBoard().getIntersections().get(from);
    _to = _ref.getBoard().getIntersections().get(to);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    if (_from == null || _to == null) {
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
          "You cannot move a knight when it is not your turn.", null));
    }
    Knight knight = _from.getKnight();
    if (knight == null || !knight.getPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have a knight at that location.", null));
    }
    if (!knight.isActive()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "Only an active knight can move.", null));
    }
    if (_to.getBuilding() != null || _to.hasKnight()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "A knight can only move to an empty intersection.", null));
    }
    if (!connectedByOwnRoad()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "A knight can only move along your own road.", null));
    }

    // The Action:
    _to.setKnight(knight);
    _from.removeKnight();

    return KnightActions.broadcast(_ref, _player, "You moved a knight.",
        String.format("%s moved a knight.", _player.getName()));
  }

  // Whether 'from' and 'to' are joined by a path carrying this player's road.
  private boolean connectedByOwnRoad() {
    for (Path p : _from.getPaths()) {
      if (p.getOtherEnd(_from).equals(_to) && p.getRoad() != null
          && p.getRoad().getPlayer().getID() == _player.getID()) {
        return true;
      }
    }
    return false;
  }
}
