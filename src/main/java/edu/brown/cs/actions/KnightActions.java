package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Shared helpers for the Cities &amp; Knights knight actions (paying costs and
 * building responses).
 *
 */
final class KnightActions {

  private KnightActions() {
  }

  /**
   * @return Whether the player holds every resource in the given cost.
   */
  static boolean canAfford(Player player, Map<Resource, Double> cost) {
    for (Map.Entry<Resource, Double> entry : cost.entrySet()) {
      if (!player.hasResource(entry.getKey(), entry.getValue())) {
        return false;
      }
    }
    return true;
  }

  /**
   * Pays the given cost from the player's hand back to the bank.
   */
  static void pay(Player player, Map<Resource, Double> cost, Referee ref) {
    for (Map.Entry<Resource, Double> entry : cost.entrySet()) {
      player.removeResource(entry.getKey(), entry.getValue(), ref.getBank());
    }
  }

  /**
   * Builds an action response map: {@code ownMessage} to the acting player,
   * {@code othersMessage} to everyone else.
   */
  static Map<Integer, ActionResponse> broadcast(Referee ref, Player actor,
      String ownMessage, String othersMessage) {
    ActionResponse toActor = new ActionResponse(true, ownMessage, null);
    ActionResponse toOthers = new ActionResponse(true, othersMessage, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : ref.getPlayers()) {
      toReturn.put(player.getID(),
          player.equals(actor) ? toActor : toOthers);
    }
    return toReturn;
  }

  /**
   * Counts how many knights the given player has on the board at the specified
   * tier. Used to enforce the 2-per-tier supply cap.
   */
  static int countKnightsAtTier(Referee ref, Player player, int tier) {
    int count = 0;
    for (edu.brown.cs.board.Intersection i : ref.getBoard()
        .getIntersections().values()) {
      edu.brown.cs.board.Knight k = i.getKnight();
      if (k != null && k.getPlayer().equals(player) && k.getTier() == tier) {
        count++;
      }
    }
    return count;
  }
}
