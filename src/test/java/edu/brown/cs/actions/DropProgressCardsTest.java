package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCardType;
import edu.brown.cs.catan.Referee;

public class DropProgressCardsTest {

  @Test
  public void testNotSetUp() {
    try {
      FollowUpAction drop = new DropProgressCards(0, 1);
      drop.execute();
      assertTrue(false);
    } catch (UnsupportedOperationException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testDroppingCardsNotHeld() {
    Referee ref = new MasterReferee();
    Player p = ref.getPlayerByID(ref.addPlayer("Sean", "Red"));
    p.addProgressCard(ProgressCardType.SPY);
    JsonObject set = new JsonObject();
    JsonArray toDrop = new JsonArray();
    toDrop.add(new JsonPrimitive("WARLORD"));
    set.add("toDrop", toDrop);
    FollowUpAction drop = new DropProgressCards(0, 1);
    drop.setupAction(ref, 0, set);
    Map<Integer, ActionResponse> response = drop.execute();
    assertTrue(response.get(0).getSuccess() == false);
  }

  @Test
  public void testDroppingImproperNumber() {
    Referee ref = new MasterReferee();
    Player p = ref.getPlayerByID(ref.addPlayer("Sean", "Red"));
    p.addProgressCard(ProgressCardType.SPY);
    p.addProgressCard(ProgressCardType.WARLORD);
    JsonObject set = new JsonObject();
    JsonArray toDrop = new JsonArray();
    toDrop.add(new JsonPrimitive("SPY"));
    set.add("toDrop", toDrop);
    FollowUpAction drop = new DropProgressCards(0, 2);
    drop.setupAction(ref, 0, set);
    Map<Integer, ActionResponse> response = drop.execute();
    assertTrue(response.get(0).getSuccess() == false);
  }

  @Test
  public void testBasicDrop() {
    Referee ref = new MasterReferee();
    Player p = ref.getPlayerByID(ref.addPlayer("Sean", "Red"));
    p.addProgressCard(ProgressCardType.SPY);
    p.addProgressCard(ProgressCardType.SPY);
    p.addProgressCard(ProgressCardType.WARLORD);
    JsonObject set = new JsonObject();
    JsonArray toDrop = new JsonArray();
    toDrop.add(new JsonPrimitive("SPY"));
    set.add("toDrop", toDrop);

    FollowUpAction drop = new DropProgressCards(0, 1);
    Collection<FollowUpAction> follow = new ArrayList<>();
    follow.add(drop);
    ref.addFollowUp(follow);
    drop.setupAction(ref, 0, set);
    Map<Integer, ActionResponse> response = drop.execute();

    assertTrue(response.get(0).getSuccess());
    assertEquals(2, p.getProgressCards().size());
    assertEquals(1, p.getProgressCards().stream()
        .filter(c -> c == ProgressCardType.SPY).count());
    assertEquals(1, p.getProgressCards().stream()
        .filter(c -> c == ProgressCardType.WARLORD).count());
  }
}
