package edu.brown.cs.catan;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pure combat math for a Cities &amp; Knights barbarian attack. The board-facing
 * orchestration (counting cities and knights, downgrading cities, awarding the
 * victory point) lives in the referee; this class only decides outcomes so the
 * rules are easy to test in isolation.
 *
 */
public final class BarbarianAttack {

  private BarbarianAttack() {
  }

  /**
   * @param barbarianStrength
   *          The barbarian fleet's strength (the number of cities on the board).
   * @param totalKnightStrength
   *          The combined strength of all active knights.
   * @return Whether the defenders repel the attack.
   */
  public static boolean defendersWin(int barbarianStrength,
      int totalKnightStrength) {
    return totalKnightStrength >= barbarianStrength;
  }

  /**
   * The lone strongest contributor (Defender of Catan) when the defenders win.
   *
   * @param knightStrength
   *          Map of player id to that player's active knight strength.
   * @return The unique player id with the greatest strength (which must be
   *         positive), or -1 if nobody contributed or there is a tie.
   */
  public static int defenderOfCatan(Map<Integer, Integer> knightStrength) {
    int best = 0;
    int bestPlayer = -1;
    boolean tie = false;
    for (Map.Entry<Integer, Integer> entry : knightStrength.entrySet()) {
      int strength = entry.getValue();
      if (strength > best) {
        best = strength;
        bestPlayer = entry.getKey();
        tie = false;
      } else if (strength == best && strength > 0) {
        tie = true;
      }
    }
    return tie ? -1 : bestPlayer;
  }

  /**
   * The players who lose a city when the defenders lose: those with the fewest
   * knights (ties included, zero counts).
   *
   * @param knightStrength
   *          Map of player id to that player's active knight strength.
   * @return The set of player ids with the minimum strength.
   */
  public static Set<Integer> weakestDefenders(
      Map<Integer, Integer> knightStrength) {
    Set<Integer> weakest = new HashSet<>();
    int min = Integer.MAX_VALUE;
    for (int strength : knightStrength.values()) {
      min = Math.min(min, strength);
    }
    for (Map.Entry<Integer, Integer> entry : knightStrength.entrySet()) {
      if (entry.getValue() == min) {
        weakest.add(entry.getKey());
      }
    }
    return weakest;
  }
}
