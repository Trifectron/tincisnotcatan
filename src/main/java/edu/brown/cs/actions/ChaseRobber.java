package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Knight;
import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;

/**
 * Cities & Knights "chase away the robber" action: a player deactivates an
 * active knight of theirs that is adjacent to the robber's hex to immediately
 * move the robber to a different hex and steal one random resource card from
 * any victim on the new hex. The knight is sacrificed for the turn (it is
 * deactivated until reactivated).
 *
 */
public class ChaseRobber implements Action {

  public static final String ID = "chaseRobber";
  private final Player _player;
  private final Intersection _knightAt;
  private final HexCoordinate _destination;
  private final Referee _ref;

  public ChaseRobber(Referee ref, int playerID,
      edu.brown.cs.board.IntersectionCoordinate knightAt,
      HexCoordinate destination) {
    assert ref != null && knightAt != null && destination != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _knightAt = _ref.getBoard().getIntersections().get(knightAt);
    _destination = destination;
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
    if (_knightAt == null) {
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
          "You cannot chase the robber when it is not your turn.", null));
    }
    Knight knight = _knightAt.getKnight();
    if (knight == null || !knight.getPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You do not have a knight at that location.", null));
    }
    if (!knight.isActive()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "Only an active knight can chase the robber.", null));
    }
    if (!adjacentToRobber(_knightAt)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "Your knight must be adjacent to the robber's hex.", null));
    }
    if (_destination.equals(_ref.getBoard().findRobber())) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "The robber must be moved to a different hex.", null));
    }
    if (!isOnBoard(_destination)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "The destination hex isn't on the board.", null));
    }
    // Cities & Knights: the robber cannot be moved until the barbarian
    // fleet has reached the island for the first time. The knight chase
    // rule still applies, but if the gate is closed we reject.
    if (_ref.getBarbarianTrack() != null
        && !_ref.getBarbarianTrack().hasEverReachedIsland()) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "The robber cannot be moved until the barbarians have reached "
              + "the island for the first time.", null));
    }

    // Deactivate the knight (the cost) and move the robber.
    knight.deactivate();
    Set<Integer> victims = _ref.getBoard().moveRobber(_destination);

    // Steal one random resource from a single victim on the new hex.
    int stolen = 0;
    for (int candidate : victims) {
      if (candidate == _player.getID()) {
        continue;
      }
      Player victim = _ref.getPlayerByID(candidate);
      java.util.List<Resource> pool = new ArrayList<>();
      for (Map.Entry<Resource, Double> entry : victim.getResources()
          .entrySet()) {
        for (int i = 0; i < entry.getValue(); i++) {
          pool.add(entry.getKey());
        }
      }
      if (pool.isEmpty()) {
        continue;
      }
      Collections.shuffle(pool);
      Resource taken = pool.get(0);
      victim.removeResource(taken, 1, _ref.getBank());
      _player.addResource(taken, 1, _ref.getBank());
      stolen++;
      break;
    }

    String toThief = stolen > 0
        ? "You chased the robber and stole a resource."
        : "You chased the robber but no one had a card to give up.";
    String toOthers = String.format("%s chased the robber.",
        _player.getName());
    ActionResponse respToThief = new ActionResponse(true, toThief, null);
    ActionResponse respToOthers = new ActionResponse(true, toOthers, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toReturn.put(p.getID(),
          p.getID() == _player.getID() ? respToThief : respToOthers);
    }
    return toReturn;
  }

  // True if the given intersection is adjacent to the tile currently
  // hosting the robber.
  private boolean adjacentToRobber(Intersection intersection) {
    HexCoordinate robberCoord = _ref.getBoard().findRobber();
    if (robberCoord == null) {
      return false;
    }
    for (Tile t : _ref.getBoard().getTiles()) {
      if (t.getCoordinate().equals(robberCoord)) {
        return t.getIntersections().contains(intersection);
      }
    }
    return false;
  }

  private boolean isOnBoard(HexCoordinate coord) {
    for (Tile t : _ref.getBoard().getTiles()) {
      if (t.getCoordinate().equals(coord)) {
        return true;
      }
    }
    return false;
  }
}
