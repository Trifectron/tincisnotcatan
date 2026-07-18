package edu.brown.cs.board;

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

/**
 * Interface for the Buildings on the board.
 *
 * @author anselvahle
 *
 */
public interface Building {

  /**
   * Tells the building to tell the player who is associated with it to collect
   * a resource of the input type.
   *
   * @param resource
   *          Type of resource to collect.
   * @return A map of the player id to a map of resources that they collected
   *         and how many of them.
   */
  Map<Integer, Map<Resource, Integer>> collectResource(Resource resource);

  /**
   * Cities &amp; Knights variant of {@link #collectResource(Resource)}. Under
   * Cities &amp; Knights a city on an ore/wool/lumber hex yields one resource
   * plus one commodity (see {@link #collectCommodity(Resource)}) instead of two
   * resources. Defaults to the base-game behavior.
   *
   * @param resource
   *          Type of resource to collect.
   * @param citiesAndKnights
   *          Whether the game is a Cities &amp; Knights game.
   * @return A map of player id to the resources collected.
   */
  default Map<Integer, Map<Resource, Integer>> collectResource(
      Resource resource, boolean citiesAndKnights) {
    return collectResource(resource);
  }

  /**
   * The commodities this building produces from the given resource hex under
   * Cities &amp; Knights. Only cities produce commodities, and only on
   * ore/wool/lumber hexes; every other building produces none.
   *
   * @param resource
   *          Type of resource the hex produces.
   * @return A map of player id to the commodities collected (empty if none).
   */
  default Map<Integer, Map<Commodity, Integer>> collectCommodity(
      Resource resource) {
    return new HashMap<>();
  }

  /**
   * Gets the player asscoiated with this building.
   *
   * @return the Player.
   */
  Player getPlayer();

}
