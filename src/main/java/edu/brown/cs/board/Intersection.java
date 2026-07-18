package edu.brown.cs.board;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;

public class Intersection {
  private List<Path> _paths;
  private Building _building;
  private Knight _knight;
  private Port _port;
  private IntersectionCoordinate _position;

  /**
   * Constructor for the class.
   *
   * @param position
   *          Intersection Coordinate that represents this intersection's
   *          position on the board.
   */
  public Intersection(IntersectionCoordinate position) {
    _position = position;
    _building = null;
    _knight = null;
    _port = null;
    _paths = new ArrayList<Path>();
  }

  @Override
  public String toString() {
    return "Intersection [_position=" + _position + "]";
  }

  /**
   * Gets the port associated with this intersection.
   *
   * @return The port associated with this intersection, else null.
   */
  public Port getPort() {
    return _port;
  }

  /**
   * Gets the coordinate of this intersection.
   *
   * @return IntersectionCoordinate for this intersection.
   */
  public IntersectionCoordinate getPosition() {
    return _position;
  }

  /**
   * Tells the building to tell the player who is associated with it to collect
   * a resource of the input type.
   *
   * @param res
   *          Type of resource to collect.
   * @return A map of the player id to a map of resources that they collected
   *         and how many of them.
   */
  public Map<Integer, Map<Resource, Integer>> notifyBuilding(Resource res) {
    return notifyBuilding(res, false);
  }

  /**
   * Cities &amp; Knights variant of {@link #notifyBuilding(Resource)}.
   *
   * @param res
   *          Type of resource to collect.
   * @param citiesAndKnights
   *          Whether this is a Cities &amp; Knights game.
   * @return A map of player id to the resources collected.
   */
  public Map<Integer, Map<Resource, Integer>> notifyBuilding(Resource res,
      boolean citiesAndKnights) {
    Map<Integer, Map<Resource, Integer>> toRet = new HashMap<Integer, Map<Resource, Integer>>();
    if (_building != null) {
      toRet = _building.collectResource(res, citiesAndKnights);
    }
    return toRet;
  }

  /**
   * Tells the building to collect any Cities &amp; Knights commodities it
   * produces from the given resource hex.
   *
   * @param res
   *          Type of resource the hex produces.
   * @return A map of player id to the commodities collected (empty if none).
   */
  public Map<Integer, Map<Commodity, Integer>> notifyBuildingCommodity(
      Resource res) {
    Map<Integer, Map<Commodity, Integer>> toRet = new HashMap<Integer, Map<Commodity, Integer>>();
    if (_building != null) {
      toRet = _building.collectCommodity(res);
    }
    return toRet;
  }

  /**
   * Places a settlement on this intersection associated with the input player.
   *
   * @param p
   *          Player who is wanting to build a settlement on the intersection.
   */
  public void placeSettlement(Player p) {
    if (_building == null) {
      _building = new Settlement(p);
    }
  }

  /**
   * Places a city on this intersection associated with the input player.
   *
   * @param p
   *          Player who is wanting to build a city on the intersection.
   */
  public void placeCity(Player p) {
    if (canPlaceCity(p)) {
      _building = new City(p);
    }
  }

  // States whether or not there is a settlement on an intersection 1 road
  // length away.
  private boolean hasAdjacentSettlement() {
    for (Path p : _paths) {
      if (p.getOtherEnd(this).getBuilding() != null) {
        return true;
      }
    }
    return false;
  }

  /**
   * States whether or not a player can place a settlement on this intersection.
   *
   * @param r
   *          The referee of the game.
   * @param playerID
   *          ID of the player who is wanting to build a settlement
   * @return A boolean stating whether or not the player can build on this
   *         intersection.
   */
  public boolean canPlaceSettlement(Referee r, int playerID) {
    if (_building == null && !hasAdjacentSettlement()) {
      if (r.getGameStatus() != GameStatus.PROGRESS) {
        return true;
      } else {
        for (Path p : _paths) {
          if (p.getRoad() != null
              && p.getRoad().getPlayer().getID() == playerID) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * States whether or not a player can place a city on this intersection.
   *
   * @param p
   *          the player who is wanting to build a city
   * @return A boolean stating whether or not the player can build on this
   *         intersection.
   */
  public boolean canPlaceCity(Player p) {
    if (_building == null) {
      return false;
    } else if (_building.getPlayer().equals(p)
        && _building instanceof Settlement) {
      return true;
    }

    return false;
  }

  /**
   * Adds a path associated with this intersection.
   * 
   * @param p
   */
  public void addPath(Path p) {
    _paths.add(p);
  }

  /**
   * Gets the paths associated with this intersection.
   *
   * @return List of the paths associated with this intersection.
   */
  public List<Path> getPaths() {
    return _paths;
  }

  /**
   * Sets the port of this intersection.
   *
   * @param p
   *          Port to set.
   */
  public void setPort(Port p) {
    _port = p;
  }

  /**
   * Getter for the building on this intersection.
   *
   * @return The building on this intersection, else null.
   */
  public Building getBuilding() {
    return _building;
  }

  /**
   * @return The knight on this intersection, else null.
   */
  public Knight getKnight() {
    return _knight;
  }

  /**
   * @return Whether a knight occupies this intersection.
   */
  public boolean hasKnight() {
    return _knight != null;
  }

  /**
   * Whether a player may place a knight here: the intersection is empty (no
   * building, no knight) and touches one of the player's roads. Unlike
   * settlements, knights have no distance rule.
   *
   * @param playerID
   *          The player wanting to place a knight.
   * @return Whether placement is legal.
   */
  public boolean canPlaceKnight(int playerID) {
    if (_building != null || _knight != null) {
      return false;
    }
    for (Path p : _paths) {
      if (p.getRoad() != null && p.getRoad().getPlayer().getID() == playerID) {
        return true;
      }
    }
    return false;
  }

  /**
   * Places a new basic knight for the player if the intersection is empty.
   *
   * @param p
   *          The knight's owner.
   */
  public void placeKnight(Player p) {
    if (_building == null && _knight == null) {
      _knight = new Knight(p);
    }
  }

  /**
   * Puts an existing knight on this intersection (used when moving a knight).
   *
   * @param knight
   *          The knight to place.
   */
  public void setKnight(Knight knight) {
    _knight = knight;
  }

  /**
   * Removes the knight from this intersection.
   */
  public void removeKnight() {
    _knight = null;
  }

  /**
   * Downgrades a city on this intersection back to a settlement (e.g. after a
   * lost barbarian attack). No-op if there is no city here.
   */
  public void downgradeCity() {
    if (_building instanceof City) {
      _building = new Settlement(_building.getPlayer());
    }
  }

  @Override
  public int hashCode() {
    return _position.hashCode();
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
    Intersection other = (Intersection) obj;
    if (_position == null) {
      if (other._position != null) {
        return false;
      }
    } else if (!_position.equals(other._position)) {
      return false;
    }
    return true;
  }

}
