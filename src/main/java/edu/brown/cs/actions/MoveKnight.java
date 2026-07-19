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
 * Action that moves an active knight along one of the player's roads (Cities
 * & Knights). The destination may be either empty or held by a strictly
 * weaker opposing knight; in the latter case the weaker knight is displaced
 * to an open intersection reachable via its own owner's roads, or removed
 * from the board entirely if no such intersection exists. All successful
 * moves turn the moving knight to its inactive side.
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
    if (_to.getBuilding() != null) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "A knight can only move to an empty intersection.", null));
    }
    if (!connectedByOwnRoad()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "A knight can only move along your own road.", null));
    }

    Knight displaced = null;
    if (_to.hasKnight()) {
      // A weaker opponent's knight already on the destination is bumped to
      // a relocation spot reached via its own owner's roads; if no such
      // spot exists it is removed from the board entirely.
      displaced = _to.getKnight();
      if (displaced.getPlayer().equals(_player)) {
        return ImmutableMap.of(_player.getID(), new ActionResponse(false,
            "You cannot displace one of your own knights.", null));
      }
      if (displaced.getTier() >= knight.getTier()) {
        return ImmutableMap.of(_player.getID(), new ActionResponse(false,
            "You can only displace a strictly weaker knight.", null));
      }
      Intersection relocation = findRelocation(displaced.getPlayer());
      if (relocation != null) {
        relocation.setKnight(displaced);
      }
      // else: the displaced knight falls off the board.
    }

    // The Action:
    _to.setKnight(knight);
    _from.removeKnight();
    knight.deactivate();

    String message;
    if (displaced != null) {
      message = String.format("You displaced %s's tier-%d knight.",
          displaced.getPlayer().getName(), displaced.getTier());
    } else {
      message = "You moved a knight.";
    }
    return KnightActions.broadcast(_ref, _player, message,
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

  // Finds an open intersection (no building, no knight) adjacent to the
  // destination along a road belonging to the displaced knight's owner;
  // null if no such spot exists (in which case the displaced knight is
  // removed from the board).
  private Intersection findRelocation(Player displacedOwner) {
    for (Path p : _to.getPaths()) {
      Intersection adj = p.getOtherEnd(_to);
      if (adj.getBuilding() != null || adj.hasKnight()) {
        continue;
      }
      if (p.getRoad() == null
          || p.getRoad().getPlayer().getID() != displacedOwner.getID()) {
        continue;
      }
      return adj;
    }
    return null;
  }
}
