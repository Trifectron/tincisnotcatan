package edu.brown.cs.board;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Resource;

/**
 * Generic Tile implementation of the BoardTile interface.
 *
 * @author anselvahle
 *
 */
public class Tile implements BoardTile {
  private final Collection<Intersection> _intersections;
  private int _rollNum;
  private final TileType _type;
  private final HexCoordinate _coordinate;
  private boolean _hasRobber;
  private int _merchantOwner = -1;
  private List<IntersectionCoordinate> _portLocations;
  private Resource _portType;

  /**
   * Constructor for the class.
   *
   * @param rollNum
   *          Number that will trigger this tile.
   * @param coordinate
   *          Location of the tile on the board.
   * @param intersections
   *          Map of the intersections on the board.
   * @param paths
   *          Map of the paths on the board.
   * @param type
   *          The type of resource associated with this tile.
   */
  public Tile(int rollNum, HexCoordinate coordinate,
      Map<IntersectionCoordinate, Intersection> intersections,
      Map<PathCoordinate, Path> paths, TileType type) {
    _type = type;
    _rollNum = rollNum;
    _coordinate = coordinate;
    _hasRobber = false;
    _intersections = new ArrayList<Intersection>();
    _portLocations = new ArrayList<IntersectionCoordinate>();
    fillEdges(intersections, paths);
  }

  /**
   * Constructor for the class.
   *
   * @param rollNum
   *          Number that will trigger this tile.
   * @param coordinate
   *          Location of the tile on the board.
   * @param intersections
   *          Map of the intersections on the board.
   * @param paths
   *          Map of the paths on the board.
   * @param type
   *          The type of resource associated with this tile.
   * @param hasRobber
   *          Boolean stating whether or not the robber is on this tile.
   */
  public Tile(int rollNum, HexCoordinate coordinate,
      Map<IntersectionCoordinate, Intersection> intersections,
      Map<PathCoordinate, Path> paths, TileType type, boolean hasRobber) {
    _type = type;
    _rollNum = rollNum;
    _coordinate = coordinate;
    _hasRobber = hasRobber;
    _intersections = new ArrayList<Intersection>();
    _portLocations = new ArrayList<IntersectionCoordinate>();
    fillEdges(intersections, paths);
  }

  /**
   * Constructor for the class (Seat tile specific).
   *
   * @param coordinate
   *          Location of the tile on the board.
   * @param type
   *          The tile type associated with this tile.
   * @param intersections
   *          Map of the intersections on the board.
   */
  public Tile(HexCoordinate coordinate, TileType type,
      Map<IntersectionCoordinate, Intersection> intersections) {
    _type = type;
    _rollNum = 0;
    _coordinate = coordinate;
    _hasRobber = false;
    _intersections = new ArrayList<Intersection>();
    _portLocations = new ArrayList<IntersectionCoordinate>();
    fillSeaTile(intersections);
  }

  /**
   * Determines where the intersections on Sea Tiles are and which two will be
   * the port locations.
   *
   * @param intersections
   *          Map of the intersections on the board.
   */
  private void fillSeaTile(Map<IntersectionCoordinate, Intersection> intersections) {
    PriorityQueue<IntersectionCoordinate> closestIntersections =
        new PriorityQueue<>(6, new IntersectionComparator());

    HexCoordinate upLeftTile = new HexCoordinate(_coordinate.getX(),
        _coordinate.getY(), _coordinate.getZ() + 1);
    HexCoordinate upRightTile = new HexCoordinate(_coordinate.getX(),
        _coordinate.getY() + 1, _coordinate.getZ() + 1);
    HexCoordinate rightTile = new HexCoordinate(_coordinate.getX(),
        _coordinate.getY() + 1, _coordinate.getZ());
    HexCoordinate lowerRightTile = new HexCoordinate(_coordinate.getX() + 1,
        _coordinate.getY() + 1, _coordinate.getZ());
    HexCoordinate lowerLeftTile = new HexCoordinate(_coordinate.getX() + 1,
        _coordinate.getY(), _coordinate.getZ());
    HexCoordinate leftTile = new HexCoordinate(_coordinate.getX() + 1,
        _coordinate.getY(), _coordinate.getZ() + 1);

    closestIntersections.add(new IntersectionCoordinate(_coordinate,
        upLeftTile, upRightTile));
    closestIntersections.add(new IntersectionCoordinate(_coordinate,
        upRightTile, rightTile));
    closestIntersections.add(new IntersectionCoordinate(_coordinate,
        rightTile, lowerRightTile));
    closestIntersections.add(new IntersectionCoordinate(_coordinate,
        lowerRightTile, lowerLeftTile));
    closestIntersections.add(new IntersectionCoordinate(_coordinate,
        lowerLeftTile, leftTile));
    closestIntersections.add(new IntersectionCoordinate(_coordinate,
        leftTile, upLeftTile));

    // Add the two intersections closest to the origin
    IntersectionCoordinate toAdd = closestIntersections.poll();
    assert (intersections.containsKey(toAdd));
    _intersections.add(intersections.get(toAdd));

    toAdd = closestIntersections.poll();
    assert (intersections.containsKey(toAdd));
    _intersections.add(intersections.get(toAdd));
  }

  /**
   * Private class made to compare the distance of intersections from the center
   * of the board.
   * 
   * @author anselvahle
   *
   */
  private static class IntersectionComparator implements
      Comparator<IntersectionCoordinate> {

    @Override
    public int compare(IntersectionCoordinate o1, IntersectionCoordinate o2) {
      double o1X = averagePositionX(o1);
      double o2X = averagePositionX(o2);
      double o1Y = averagePositionY(o1);
      double o2Y = averagePositionY(o2);

      return Double.compare(Math.pow(o1X, 2) + Math.pow(o1Y, 2),
          Math.pow(o2X, 2) + Math.pow(o2Y, 2));
    }

    // Averages the hex coordinates for the x position.
    private double averagePositionX(IntersectionCoordinate coord) {
      double x1 = coord.getCoord1().cartesianX();
      double x2 = coord.getCoord2().cartesianX();
      double x3 = coord.getCoord3().cartesianX();
      return (x1 + x2 + x3) / 3.0;
    }

    // Averages the hex coordinates for the y position.
    private double averagePositionY(IntersectionCoordinate coord) {
      double y1 = coord.getCoord1().cartesianY();
      double y2 = coord.getCoord2().cartesianY();
      double y3 = coord.getCoord3().cartesianY();
      return (y1 + y2 + y3) / 3.0;
    }

  }

  // Fills the edges of a tile using the relative positions of the nearby tiles.
  // First by using a cache and then by creation.
  private void fillEdges(
      Map<IntersectionCoordinate, Intersection> intersections, Map<PathCoordinate, Path> paths) {
    HexCoordinate upLeftTile = new HexCoordinate(_coordinate.getX(),
        _coordinate.getY(), _coordinate.getZ() + 1);
    HexCoordinate upRightTile = new HexCoordinate(_coordinate.getX(),
        _coordinate.getY() + 1, _coordinate.getZ() + 1);
    HexCoordinate rightTile = new HexCoordinate(_coordinate.getX(),
        _coordinate.getY() + 1, _coordinate.getZ());
    HexCoordinate lowerRightTile = new HexCoordinate(_coordinate.getX() + 1,
        _coordinate.getY() + 1, _coordinate.getZ());
    HexCoordinate lowerLeftTile = new HexCoordinate(_coordinate.getX() + 1,
        _coordinate.getY(), _coordinate.getZ());
    HexCoordinate leftTile = new HexCoordinate(_coordinate.getX() + 1,
        _coordinate.getY(), _coordinate.getZ() + 1);

    IntersectionCoordinate top = new IntersectionCoordinate(_coordinate,
        upLeftTile, upRightTile);
    IntersectionCoordinate upRight = new IntersectionCoordinate(_coordinate,
        upRightTile, rightTile);
    IntersectionCoordinate lowerRight = new IntersectionCoordinate(_coordinate,
        rightTile, lowerRightTile);
    IntersectionCoordinate bottom = new IntersectionCoordinate(_coordinate,
        lowerRightTile, lowerLeftTile);
    IntersectionCoordinate lowerLeft = new IntersectionCoordinate(_coordinate,
        lowerLeftTile, leftTile);
    IntersectionCoordinate upLeft = new IntersectionCoordinate(_coordinate,
        leftTile, upLeftTile);

    fillIntersections(intersections, top);
    fillIntersections(intersections, upRight);
    fillIntersections(intersections, lowerRight);
    fillIntersections(intersections, bottom);
    fillIntersections(intersections, lowerLeft);
    fillIntersections(intersections, upLeft);

    fillPaths(intersections.get(top), intersections.get(upRight), paths);
    fillPaths(intersections.get(upRight), intersections.get(lowerRight), paths);
    fillPaths(intersections.get(lowerRight), intersections.get(bottom), paths);
    fillPaths(intersections.get(bottom), intersections.get(lowerLeft), paths);
    fillPaths(intersections.get(lowerLeft), intersections.get(upLeft), paths);
    fillPaths(intersections.get(upLeft), intersections.get(top), paths);

  }

  // Fills the intersections of the tile, first by using a cache and then by
  // creation.
  private void fillIntersections(
      Map<IntersectionCoordinate, Intersection> intersections,
      IntersectionCoordinate coord) {
    if(intersections.containsKey(coord)) {
      _intersections.add(intersections.get(coord));
    } else {
      Intersection newIntersect = new Intersection(coord);
      intersections.put(coord, newIntersect);
      _intersections.add(newIntersect);
    }
  }

  // Fills the paths using the cache and then creation
  private void fillPaths(Intersection start, Intersection end,
      Map<PathCoordinate, Path> paths) {
    PathCoordinate path = new PathCoordinate(start.getPosition(), end.getPosition());
    if(!paths.containsKey(path)) {
      paths.put(path, new Path(start, end));
    }
  }

  @Override
  public TileType getType() {
    return _type;
  }

  /**
   * Tells the intersections when to collect resources.
   *
   * @return A map of the player id to a map of resources that they collected
   *         and how many of them.
   */
  public Map<Integer, Map<Resource, Integer>> notifyIntersections() {
    return notifyIntersections(false);
  }

  /**
   * Cities &amp; Knights variant of {@link #notifyIntersections()}: cities on
   * ore/wool/lumber hexes yield one fewer resource (the balance is produced as
   * a commodity by {@link #notifyCommodities()}).
   *
   * @param citiesAndKnights
   *          Whether this is a Cities &amp; Knights game.
   * @return A map of player id to the resources collected.
   */
  public Map<Integer, Map<Resource, Integer>> notifyIntersections(
      boolean citiesAndKnights) {
    Map<Integer, Map<Resource, Integer>> playerResourceCount = new HashMap<Integer, Map<Resource, Integer>>();
    assert (_type.getType() != null);
    for (Intersection i : _intersections) {
      Map<Integer, Map<Resource, Integer>> fromInter = i.notifyBuilding(_type
          .getType(), citiesAndKnights);
      for (int playerID : fromInter.keySet()) {
        if (!playerResourceCount.containsKey(playerID)) {
          playerResourceCount.put(playerID, new HashMap<Resource, Integer>());
        }
        Map<Resource, Integer> resourceCount = fromInter.get(playerID);
        Map<Resource, Integer> playerCount = playerResourceCount.get(playerID);
        for(Resource res : resourceCount.keySet()) {
          if (playerCount.containsKey(res)) {
            playerCount.replace(res,
                playerCount.get(res) + resourceCount.get(res));
          } else {
            playerCount.put(res, resourceCount.get(res));
          }
        }
      }
    }

    return playerResourceCount;
  }

  /**
   * Collects the Cities &amp; Knights commodities produced by the cities on this
   * tile when its number is rolled.
   *
   * @return A map of player id to the commodities they collected (empty if the
   *         tile produces no commodities).
   */
  public Map<Integer, Map<Commodity, Integer>> notifyCommodities() {
    Map<Integer, Map<Commodity, Integer>> playerCommodityCount = new HashMap<Integer, Map<Commodity, Integer>>();
    if (_type.getType() == null) {
      return playerCommodityCount;
    }
    for (Intersection i : _intersections) {
      Map<Integer, Map<Commodity, Integer>> fromInter = i
          .notifyBuildingCommodity(_type.getType());
      for (int playerID : fromInter.keySet()) {
        Map<Commodity, Integer> commodityCount = fromInter.get(playerID);
        Map<Commodity, Integer> playerCount = playerCommodityCount
            .computeIfAbsent(playerID, k -> new HashMap<Commodity, Integer>());
        for (Commodity commodity : commodityCount.keySet()) {
          playerCount.merge(commodity, commodityCount.get(commodity),
              Integer::sum);
        }
      }
    }
    return playerCommodityCount;
  }

  /**
   * Gold-hex production (Seafarers): maps each player with an adjacent building
   * to the number of resources they may choose (settlement = 1, city = 2). Gold
   * hexes produce a resource of the player's choice, resolved by a
   * ChooseGoldResource follow-up, so they cannot use {@link #notifyIntersections}
   * (which requires a fixed resource type).
   *
   * @return A map of player id to the number of resources they may choose.
   */
  public Map<Integer, Integer> goldProduction() {
    Map<Integer, Integer> counts = new HashMap<>();
    for (Intersection i : _intersections) {
      Building b = i.getBuilding();
      if (b != null) {
        int pips = (b instanceof City) ? 2 : 1;
        counts.merge(b.getPlayer().getID(), pips, Integer::sum);
      }
    }
    return counts;
  }

  /**
   * Sets the port for this tile.
   *
   * @param p
   *          Port to set on this tile.
   */
  public void setPorts(Port p) {
    assert (_type == TileType.SEA);
    for (Intersection i : _intersections) {
      i.setPort(p);
      _portLocations.add(i.getPosition());
    }
    _portType = p.getResource();
  }

  @Override
  public Resource getPortType() {
    return _portType;
  }

  @Override
  public HexCoordinate getCoordinate() {
    return _coordinate;
  }

  /**
   * Getter for the intersections on this tile.
   *
   * @return Collection of the intersections on this tile.
   */
  public Collection<Intersection> getIntersections() {
    return _intersections;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Tile) {
      Tile other = (Tile) obj;
      if (other.getCoordinate().equals(_coordinate)) {
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return _coordinate.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder toRet = new StringBuilder();
    toRet.append("TILETYPE: " + _type);
    toRet.append(" ROLL NUM: " + _rollNum + "\n");
    return toRet.toString();
  }

  @Override
  public boolean hasRobber() {
    return _hasRobber;
  }

  /**
   * Setter for the hasRobber boolean.
   *
   * @param _hasRobber
   *          boolean stating whether or not the robber is on this tile.
   */
  public void hasRobber(boolean _hasRobber) {
    this._hasRobber = _hasRobber;
  }

  @Override
  public int getRollNumber() {
    return _rollNum;
  }

  /**
   * Sets the number that triggers this tile's production (Cities &amp; Knights
   * Inventor progress card, which swaps two tiles' numbers).
   *
   * @param rollNum
   *          The new roll number.
   */
  public void setRollNumber(int rollNum) {
    _rollNum = rollNum;
  }

  /**
   * @return The id of the player whose Merchant (Cities &amp; Knights) is on
   *         this tile, or -1 if none.
   */
  public int getMerchantOwner() {
    return _merchantOwner;
  }

  /**
   * Sets which player's Merchant (Cities &amp; Knights) occupies this tile.
   *
   * @param playerID
   *          The owning player's id, or -1 to clear it.
   */
  public void setMerchantOwner(int playerID) {
    _merchantOwner = playerID;
  }

  @Override
  public List<IntersectionCoordinate> getPortLocations() {
    return _portLocations;
  }

  @Override
  public Set<Integer> getPlayersOnTile() {
    Set<Integer> players = new HashSet<>();
    for (Intersection i : _intersections) {
      if (i.getBuilding() != null) {
        players.add(i.getBuilding().getPlayer().getID());
      }
    }
    Set<Integer> toRet = new ImmutableSet.Builder<Integer>()
        .addAll(players).build();
    return toRet;
  }

}
