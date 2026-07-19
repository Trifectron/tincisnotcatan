package edu.brown.cs.catan;

/**
 * Tracks the Cities &amp; Knights barbarian fleet's progress toward the island.
 * The fleet advances one step each time the event die shows a barbarian ship;
 * when it reaches the island an attack is resolved and the track resets.
 *
 */
public class BarbarianTrack {

  private int _position;
  // Latches to true the first time the fleet reaches the island. The
  // official Cities & Knights rules forbid moving the robber (via any
  // means) until the barbarians have reached the island for the first
  // time; after that, robber moves are always allowed.
  private boolean _hasReachedIslandOnce;

  public BarbarianTrack() {
    _position = 0;
    _hasReachedIslandOnce = false;
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
    if (_position >= Settings.BARBARIAN_TRACK_LENGTH) {
      _hasReachedIslandOnce = true;
    }
  }

  /**
   * @return Whether the fleet has reached the island and an attack is due.
   */
  public boolean hasReachedIsland() {
    return _position >= Settings.BARBARIAN_TRACK_LENGTH;
  }

  /**
   * @return Whether the fleet has ever reached the island. Used to gate
   *         robber movement per the Cities & Knights rules.
   */
  public boolean hasEverReachedIsland() {
    return _hasReachedIslandOnce;
  }

  /**
   * Resets the fleet to its starting position.
   */
  public void reset() {
    _position = 0;
  }
}
