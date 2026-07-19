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
  // Seafarers expansion tile types:
  // WATER is navigable open sea (ships may sail on its edges).
  // GOLD produces a resource of the player's choice (see ChooseGoldResource).
  WATER(null), GOLD(null);

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
