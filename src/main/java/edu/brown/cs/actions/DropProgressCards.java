package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCardType;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up action forcing a player to discard down to the Cities & Knights
 * 4-progress-card hand limit at the end of their turn.
 */
public class DropProgressCards implements FollowUpAction {

  private boolean _isSetup;
  private int _numToDrop;
  private int _requiredPlayer;

  private Referee _ref;
  private Player _player;
  private List<ProgressCardType> _toDrop;

  public final static String ID = "dropProgressCards";
  private final static String VERB = "discard";

  public DropProgressCards(int playerID, int numToDrop) {
    _isSetup = false;
    _numToDrop = numToDrop;
    _requiredPlayer = playerID;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException(
          "A FollowUpAction must be setup before executed.");
    }
    if (_toDrop.size() != _numToDrop) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You did not discard the right number of progress cards.", null));
    }
    List<ProgressCardType> held = new ArrayList<>(_player.getProgressCards());
    for (ProgressCardType card : _toDrop) {
      if (!held.remove(card)) {
        return ImmutableMap.of(_player.getID(), new ActionResponse(false,
            "You do not have the progress cards you are trying to discard.",
            null));
      }
    }

    for (ProgressCardType card : _toDrop) {
      _player.removeProgressCard(card);
    }
    _ref.removeFollowUp(this);

    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (p.equals(_player)) {
        toReturn.put(p.getID(), new ActionResponse(true,
            "You discarded down to the 4-card progress-card limit.", null));
      } else {
        toReturn.put(p.getID(), new ActionResponse(true, String.format(
            "%s discarded down to the 4-card progress-card limit.",
            _player.getName()), null));
      }
    }
    return toReturn;
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject json) {
    assert playerID == _requiredPlayer;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    _isSetup = true;
    if (_player == null) {
      throw new IllegalArgumentException("No player exists with given ID.");
    }
    try {
      JsonArray cards = json.get("toDrop").getAsJsonArray();
      _toDrop = new ArrayList<>();
      for (int i = 0; i < cards.size(); i++) {
        _toDrop.add(ProgressCardType.valueOf(cards.get(i).getAsString()));
      }
    } catch (JsonSyntaxException | NullPointerException
        | IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Missing or invalid toDrop progress card list.");
    }
  }

  @Override
  public JsonObject getData() {
    JsonObject data = new JsonObject();
    data.addProperty("numToDrop", _numToDrop);
    return data;
  }

  @Override
  public String getID() {
    return DropProgressCards.ID;
  }

  @Override
  public int getPlayerID() {
    return _requiredPlayer;
  }

  @Override
  public String getVerb() {
    return VERB;
  }
}
