package edu.brown.cs.board;

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

/**
 * Implementation of Building for the City.
 *
 * @author anselvahle
 *
 */
public class City implements Building {
  private Player _player;
  private boolean _hasWall;

  /**
   * Constructor for the class.
   *
   * @param player
   *          Player to be associated with the the building.
   */
  public City(Player player) {
    _player = player;
    _hasWall = false;
  }

  /**
   * @return Whether this city has a city wall (Cities &amp; Knights).
   */
  public boolean hasWall() {
    return _hasWall;
  }

  /**
   * Builds a city wall on this city.
   */
  public void buildWall() {
    _hasWall = true;
  }

  /**
   * Destroys this city's wall (Cities & Knights) — e.g. as a buffer against
   * a barbarian attack. No-op if there is no wall.
   */
  public void destroyWall() {
    _hasWall = false;
  }

  @Override
  public Map<Integer, Map<Resource, Integer>> collectResource(Resource resource) {
    Map<Resource, Integer> resourceCount = new HashMap<Resource, Integer>();
    resourceCount.put(resource, 2);
    Map<Integer, Map<Resource, Integer>> playerResource = new HashMap<Integer, Map<Resource, Integer>>();
    playerResource.put(_player.getID(), resourceCount);
    return playerResource;
  }

  @Override
  public Map<Integer, Map<Resource, Integer>> collectResource(Resource resource,
      boolean citiesAndKnights) {
    // Under C&K a city on an ore/wool/lumber hex yields 1 resource + 1
    // commodity (the commodity comes from collectCommodity); on brick/grain it
    // still yields the base 2 resources.
    int count = 2;
    if (citiesAndKnights && Commodity.fromResource(resource) != null) {
      count = 1;
    }
    Map<Resource, Integer> resourceCount = new HashMap<Resource, Integer>();
    resourceCount.put(resource, count);
    Map<Integer, Map<Resource, Integer>> playerResource = new HashMap<Integer, Map<Resource, Integer>>();
    playerResource.put(_player.getID(), resourceCount);
    return playerResource;
  }

  @Override
  public Map<Integer, Map<Commodity, Integer>> collectCommodity(
      Resource resource) {
    Map<Integer, Map<Commodity, Integer>> playerCommodity = new HashMap<Integer, Map<Commodity, Integer>>();
    Commodity commodity = Commodity.fromResource(resource);
    if (commodity != null) {
      Map<Commodity, Integer> commodityCount = new HashMap<Commodity, Integer>();
      commodityCount.put(commodity, 1);
      playerCommodity.put(_player.getID(), commodityCount);
    }
    return playerCommodity;
  }

  @Override
  public Player getPlayer() {
    return _player;
  }

}
