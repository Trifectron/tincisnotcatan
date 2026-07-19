package edu.brown.cs.board;

import edu.brown.cs.catan.Resource;

/**
 * Enum for the different types of tiles.
 *
 * @author anselvahle
 *
 */
public enum TileType {

  WHEAT(Resource.WHEAT), SHEEP(Resource.SHEEP), ORE(Resource.ORE), WOOD(
      Resource.WOOD), BRICK(Resource.BRICK), DESERT(null), SEA(null),
  // Expansion tile types. WATER is an in-play sea hex (ships travel on it),
  // distinct from the decorative SEA border. GOLD produces a player-chosen
  // resource. DESERT_ISLAND is a non-producing land hex on island scenarios.
  WATER(null), GOLD(null), DESERT_ISLAND(null);

  private Resource _resType;

  // private constructor for associated resource
  private TileType(Resource res) {
    _resType = res;
  }

  /**
   * Gets the resource type associated with the tileType.
   *
   * @return Resource.
   */
  public Resource getType() {
    return _resType;
  }

}
