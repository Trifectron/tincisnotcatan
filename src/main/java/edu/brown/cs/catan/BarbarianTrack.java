package edu.brown.cs.catan;

/**
 * Tracks the Cities &amp; Knights barbarian fleet's progress toward the island.
 * The fleet advances one step each time the event die shows a barbarian ship;
 * when it reaches the island an attack is resolved and the track resets.
 *
 */
public class BarbarianTrack {

  private int _position;

  public BarbarianTrack() {
    _position = 0;
  }

  public int getPosition() {
    return _position;
  }

  /**
   * Advances the fleet one step, capped at the island.
   */
  public void advance() {
    if (_position < Settings.BARBARIAN_TRACK_LENGTH) {
      _position++;
    }
  }

  /**
   * @return Whether the fleet has reached the island and an attack is due.
   */
  public boolean hasReachedIsland() {
    return _position >= Settings.BARBARIAN_TRACK_LENGTH;
  }

  /**
   * Resets the fleet to its starting position.
   */
  public void reset() {
    _position = 0;
  }
}
