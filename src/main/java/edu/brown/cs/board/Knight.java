package edu.brown.cs.board;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Settings;

/**
 * A Cities &amp; Knights knight piece, placed at an intersection. A knight has a
 * tier (1 basic, 2 strong, 3 mighty) and an activated/deactivated state; only
 * active knights defend against the barbarians and may move.
 *
 */
public class Knight {

  private final Player _player;
  private int _tier;
  private boolean _active;

  /**
   * Creates a new basic (tier 1), deactivated knight for the given player.
   *
   * @param player
   *          The owner of the knight.
   */
  public Knight(Player player) {
    _player = player;
    _tier = 1;
    _active = false;
  }

  public Player getPlayer() {
    return _player;
  }

  public int getTier() {
    return _tier;
  }

  public boolean isActive() {
    return _active;
  }

  public void activate() {
    _active = true;
  }

  public void deactivate() {
    _active = false;
  }

  /**
   * @return Whether this knight is below the maximum tier and can be upgraded.
   */
  public boolean canUpgrade() {
    return _tier < Settings.MAX_KNIGHT_TIER;
  }

  /**
   * Promotes this knight one tier, up to the maximum.
   */
  public void upgrade() {
    if (canUpgrade()) {
      _tier++;
    }
  }
}
