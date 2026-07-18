package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCardType;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Action that plays a Cities &amp; Knights progress card from a player's hand,
 * applying the card's effect.
 *
 */
public class PlayProgressCard implements Action {

  private Player _player;
  private Referee _ref;
  private ProgressCardType _card;
  public static final String ID = "playProgressCard";

  public PlayProgressCard(Referee ref, int playerID, String card) {
    assert ref != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    try {
      _card = ProgressCardType.fromString(card);
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new IllegalArgumentException(String.format(
          "Unknown progress card: %s", card));
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCitiesAndKnights) {
      ActionResponse resp = new ActionResponse(false,
          "Progress cards are only available in Cities & Knights games.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      ActionResponse resp = new ActionResponse(false,
          "You cannot play a progress card when it is not your turn.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }
    if (!_player.getProgressCards().contains(_card)) {
      ActionResponse resp = new ActionResponse(false, String.format(
          "You do not have the %s progress card.", _card.getName()), null);
      return ImmutableMap.of(_player.getID(), resp);
    }

    // The Action:
    String playerMessage = _card.play(_ref, _player);
    _player.removeProgressCard(_card);

    // Formatting the response:
    ActionResponse respToPlayer = new ActionResponse(true, playerMessage, null);
    String message = String.format("%s played the %s progress card.",
        _player.getName(), _card.getName());
    ActionResponse respToAll = new ActionResponse(true, message, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : _ref.getPlayers()) {
      if (player.equals(_player)) {
        toReturn.put(player.getID(), respToPlayer);
      } else {
        toReturn.put(player.getID(), respToAll);
      }
    }
    return toReturn;
  }
}
