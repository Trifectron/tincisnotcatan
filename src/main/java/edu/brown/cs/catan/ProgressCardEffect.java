package edu.brown.cs.catan;

/**
 * The effect of a Cities &amp; Knights progress card. One implementation per
 * card; the strategy is invoked when the card is played.
 *
 */
@FunctionalInterface
public interface ProgressCardEffect {

  /**
   * Applies this card's effect.
   *
   * @param ref
   *          The referee for the game.
   * @param player
   *          The player who played the card.
   * @param target
   *          An optional player-chosen target (e.g. a resource name for
   *          Resource Monopoly); null for cards that don't need one.
   * @return A message describing what happened, shown to the player.
   */
  String apply(Referee ref, Player player, String target);
}
