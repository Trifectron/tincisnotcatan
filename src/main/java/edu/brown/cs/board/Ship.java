package edu.brown.cs.board;

import edu.brown.cs.catan.Player;

/**
 * Class that is the representation of ships (Seafarers expansion). A ship is the
 * maritime equivalent of a {@link Road}: it occupies a {@link Path} and belongs
 * to a single player. Ships may only be placed on water/coastal paths and count
 * toward a player's longest trade route alongside roads.
 *
 */
public class Ship {
  private Player _player;

  /**
   * Constructor for the class.
   *
   * @param player
   *          Player who is associated with this ship.
   */
  public Ship(Player player) {
    _player = player;
  }

  /**
   * Gets the player associated with this ship.
   *
   * @return The player whose ship this is.
   */
  public Player getPlayer() {
    return _player;
  }

}
