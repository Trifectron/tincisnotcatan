package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * FollowUpAction responsible for letting a player choose which resources a gold
 * hex produces (Seafarers). It is queued by {@link RollDice} for every player
 * with a settlement/city adjacent to a triggered gold hex, and modeled on
 * {@link DropCards}: the player must choose exactly {@code numToChoose}
 * resources, which are then drawn from the bank.
 *
 */
public class ChooseGoldResource implements FollowUpAction {

  private boolean _isSetup;
  private double _numToChoose;
  private int _requiredPlayer;

  private Referee _ref;
  private Player _player;
  private Map<Resource, Double> _chosen;
  private static final double TOLERANCE = 0.001;
  public static final String ID = "chooseGoldResource";
  private static final String VERB = "choose gold-hex resources";

  public ChooseGoldResource(int playerID, double numToChoose) {
    _isSetup = false;
    _numToChoose = CatanFormats.round(numToChoose);
    _requiredPlayer = playerID;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException(
          "A FollowUpAction must be setup before it is executed.");
    }
    // Validation:
    double chosenCards = 0.0;
    for (Map.Entry<Resource, Double> res : _chosen.entrySet()) {
      if (res.getValue() < 0) {
        return ImmutableMap.of(_player.getID(), new ActionResponse(false,
            "You cannot choose a negative number of resources.", null));
      }
      chosenCards += res.getValue();
    }
    if (Math.abs(chosenCards - _numToChoose) > TOLERANCE) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You must choose exactly the number of resources your gold hexes "
              + "produce. Please try again.", null));
    }

    // Action: draw the chosen resources from the bank.
    for (Map.Entry<Resource, Double> res : _chosen.entrySet()) {
      if (res.getValue() > 0) {
        _player.addResource(res.getKey(), res.getValue(), _ref.getBank());
      }
    }
    _ref.removeFollowUp(this);

    // Formulate Responses:
    ActionResponse toPlayer = new ActionResponse(true,
        "You collected your gold-hex resources.", null);
    String message = String.format("%s collected gold-hex resources.",
        _player.getName());
    ActionResponse toAll = new ActionResponse(true, message, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (p.equals(_player)) {
        toReturn.put(p.getID(), toPlayer);
      } else {
        toReturn.put(p.getID(), toAll);
      }
    }
    return toReturn;
  }

  @Override
  public JsonObject getData() {
    JsonObject toReturn = new JsonObject();
    toReturn.addProperty("numToChoose", _numToChoose);
    toReturn.addProperty("message",
        "A gold hex produced! Please choose your resources.");
    return toReturn;
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject json) {
    assert playerID == _requiredPlayer;
    JsonObject params = json.get("resources").getAsJsonObject();
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    _isSetup = true;
    if (_player == null) {
      throw new IllegalArgumentException("No player exists with the given ID.");
    }
    try {
      _chosen = new HashMap<Resource, Double>();
      for (Resource res : Resource.values()) {
        if (res != Resource.WILDCARD) {
          _chosen.put(res, params.get(res.toString()).getAsDouble());
        }
      }
    } catch (JsonSyntaxException | NullPointerException e) {
      throw new IllegalArgumentException("Missing a resource or bad JSON input");
    }
  }

  @Override
  public String getID() {
    return ChooseGoldResource.ID;
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
