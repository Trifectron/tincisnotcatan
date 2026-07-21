package edu.brown.cs.board;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Setup;

/**
 * Class paths between intersection.
 *
 * @author anselvahle
 *
 */
public class Path {
  private Intersection _start;
  private Intersection _end;
  private Road _road;
  // Seafarers expansion: a Path may host a Ship instead of a Road.
  private Ship _ship;
  // True if this path borders at least one water tile (ships may sail here).
  private boolean _isMaritime;
  // True if this path borders water on BOTH sides (open sea: no roads here).
  private boolean _isPureSea;

  /**
   * Constructor for the class.
   *
   * @param start
   *          Intersection where the path begins.
   * @param end
   *          Intersection where the path ends.
   */
  public Path(Intersection start, Intersection end) {
    _start = start;
    _end = end;
    _road = null;
    _ship = null;
    _isMaritime = false;
    _isPureSea = false;
    start.addPath(this);
    end.addPath(this);
  }

  /**
   * Gets the road on this path.
   *
   * @return the road, else null.
   */
  public Road getRoad() {
    return _road;
  }

  /**
   * Gets the ship on this path (Seafarers).
   *
   * @return the ship, else null.
   */
  public Ship getShip() {
    return _ship;
  }

  /**
   * @return whether ships may be placed on this path (it borders water).
   */
  public boolean isMaritime() {
    return _isMaritime;
  }

  /**
   * @return whether this path is open sea (water on both sides; no roads).
   */
  public boolean isPureSea() {
    return _isPureSea;
  }

  /**
   * Marks whether this path borders water (a ship-legal edge).
   *
   * @param maritime
   *          whether at least one adjacent tile is water.
   */
  public void setMaritime(boolean maritime) {
    _isMaritime = maritime;
  }

  /**
   * Marks whether this path is open sea (water on both sides). Roads may not be
   * built on open-sea paths.
   *
   * @param pureSea
   *          whether both adjacent tiles are water.
   */
  public void setPureSea(boolean pureSea) {
    _isPureSea = pureSea;
  }

  // Whether the given path hosts a piece (road or ship) owned by the player.
  private static boolean isOwnedBy(Path p, Player player) {
    if (p.getRoad() != null && p.getRoad().getPlayer().equals(player)) {
      return true;
    }
    if (p.getShip() != null && p.getShip().getPlayer().equals(player)) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return 1;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Path other = (Path) obj;
    if (_end == null || _start == null) {
      if (other._end != null || other._start != null) {
        return false;
      }
    }
    if (((_end.equals(other._end) && _start.equals(other._start)))
        || (_end.equals(other._start) && _start.equals(other._end))) {
      return true;
    }
    return false;
  }

  /**
   * Finds the longest road associated with the input player.
   *
   * @param player
   *          Player whose roads to evaluate.
   * @return Int that is the length of the longest road for this player.
   */
  public int getLongestPath(Player player) {
    Collection<Path> visited = new ArrayList<>();
    List<Path> queue = new ArrayList<>();
    Map<Path, Integer> counts = new HashMap<>();
    queue.add(this);
    counts.put(this, 0);
    // Per the official Catan rulebook, an opponent's settlement or city
    // breaks the longest-road chain — so when walking through an
    // intersection, refuse to continue if the *next* intersection
    // (i.e., the one beyond the new path) has an opponent building
    // sitting on it. The player's own buildings don't block.
    while (!queue.isEmpty()) {
      Path toVisit = queue.remove(0);
      visited.add(toVisit);
      int curr = counts.get(toVisit) + 1;
      for (Path p : toVisit.getStart().getPaths()) {
        if (!chainIsUnblocked(p, player, toVisit)) {
          continue;
        }
        if (!visited.contains(p)) {
          visited.add(p);
          counts.put(p, curr);
          queue.add(0, p);
        } else if (curr - counts.get(p) == 5) {
          counts.put(p, curr);
        }
      }
      for (Path p : toVisit.getEnd().getPaths()) {
        if (!chainIsUnblocked(p, player, toVisit)) {
          continue;
        }
        if (!visited.contains(p)) {
          visited.add(p);
          counts.put(p, curr);
          queue.add(0, p);
        } else if (curr - counts.get(p) == 5) {
          counts.put(p, curr);
        }
      }
    }
    int max = 0;
    for (Path p : visited) {
      int longest = counts.get(p);
      if (longest > max) {
        max = longest;
      }
    }
    return max;
  }

  // True when p is one of `player`'s roads/ships AND the intersection at
  // the *far end* of p (relative to `toVisit`) is not blocked by an
  // opponent's settlement or city. Used by getLongestPath to enforce
  // the official longest-road breaks-across-opponent-settlements rule.
  private static boolean chainIsUnblocked(Path p, Player player,
      Path toVisit) {
    if (!isOwnedBy(p, player)) {
      return false;
    }
    // The far intersection of p (the endpoint away from toVisit) must be
    // open — no opponent building may sit there. We don't yet know
    // which endpoint is "shared" with toVisit, but since p is a road
    // segment between two intersections, both endpoints are candidates.
    // For each intersection on p, if it's not shared with toVisit, it
    // is the candidate far end.
    Intersection farEnd = null;
    if (p.getStart() == toVisit.getStart() || p.getStart() == toVisit.getEnd()) {
      farEnd = p.getEnd();
    } else {
      farEnd = p.getStart();
    }
    if (farEnd == null) {
      return false;
    }
    Building b = farEnd.getBuilding();
    return b == null || b.getPlayer().equals(player);
  }

  /**
   * States whether or not a road can be placed in this location during setup.
   *
   * @param setup
   *          Setup to be evaluated.
   * @return A boolean stating whether or not a road can be built here.
   */
  public boolean canPlaceSetupRoad(Setup setup) {
    if (setup.getLastBuiltSettlement()!= null) {
      if (getStart().equals(setup.getLastBuiltSettlement())
          || getEnd().equals(setup.getLastBuiltSettlement())) {
        return true;
      }
    }
    return false;
  }

  /**
   * A boolean stating whether or not the input player can build a road on this
   * path.
   *
   * @param p
   *          Player who wants to build a road.
   * @return A boolean stating whether or not the player can build a road on
   *         this path.
   */
  public boolean canPlaceRoad(Player p) {
    if (_road != null || _ship != null) {
      return false;
    }
    // Roads cannot be built on open sea (Seafarers). Coastal and inland paths
    // are unaffected; in the base game no path is ever open sea.
    if (_isPureSea) {
      return false;
    }

    if (_start.getBuilding() != null
        && _start.getBuilding().getPlayer().equals(p)) {
      return true;
    } else if (_start.getBuilding() == null) {
      for (Path path : _start.getPaths()) {
        if (path.getRoad() != null && path.getRoad().getPlayer().equals(p)) {
          return true;
        }
      }
    }
    if (_end.getBuilding() != null && _end.getBuilding().getPlayer().equals(p)) {
      return true;
    } else if (_end.getBuilding() == null) {
      for (Path path : _end.getPaths()) {
        if (path.getRoad() != null && path.getRoad().getPlayer().equals(p)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Places a road associated with the input player on this path.
   *
   * @param p
   *          Player who is trying to build a road on this path.
   */
  public void placeRoad(Player p) {
    if (canPlaceRoad(p)) {
      _road = new Road(p);
    }
  }

  /**
   * Removes the road on this path (Cities &amp; Knights Diplomat progress
   * card), returning it to the owner's supply.
   */
  public void removeRoad() {
    if (_road != null) {
      _road.getPlayer().returnRoad();
      _road = null;
    }
  }

  /**
   * States whether the input player can build a ship on this path (Seafarers).
   * A ship is legal on a water/coastal path that is empty and connected to the
   * player's network via an adjacent settlement/city or another of the player's
   * ships.
   *
   * @param p
   *          Player who wants to build a ship.
   * @return whether the player may build a ship here.
   */
  public boolean canPlaceShip(Player p) {
    if (!_isMaritime) {
      return false;
    }
    if (_road != null || _ship != null) {
      return false;
    }
    if (_start.getBuilding() != null
        && _start.getBuilding().getPlayer().equals(p)) {
      return true;
    }
    if (_end.getBuilding() != null
        && _end.getBuilding().getPlayer().equals(p)) {
      return true;
    }
    for (Path path : _start.getPaths()) {
      if (path.getShip() != null && path.getShip().getPlayer().equals(p)) {
        return true;
      }
    }
    for (Path path : _end.getPaths()) {
      if (path.getShip() != null && path.getShip().getPlayer().equals(p)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Places a ship associated with the input player on this path (Seafarers).
   *
   * @param p
   *          Player who is trying to build a ship on this path.
   */
  public void placeShip(Player p) {
    if (canPlaceShip(p)) {
      _ship = new Ship(p);
    }
  }

  /**
   * Gets the start of the path.
   *
   * @return the starting Intersection of the path.
   */
  public Intersection getStart() {
    return _start;
  }

  /**
   * Gets the end of this path.
   *
   * @return the ending intersection of this path.
   */
  public Intersection getEnd() {
    return _end;
  }

  /**
   * Gets the end opposite the input end.
   *
   * @param end
   *          Endpoint that you want the opposite of.
   * @return The intersection that is the on the opposite side of the path, else
   *         null.
   */
  public Intersection getOtherEnd(Intersection end) {
    if (end.equals(_start)) {
      return _end;
    } else if (end.equals(_end)) {
      return _start;
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public String toString() {
    return _start + " <-->" + _end;
  }

}
