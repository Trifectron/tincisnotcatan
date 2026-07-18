package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.board.Intersection;

public class BarbarianTest {

  private static MasterReferee cnkReferee() {
    JsonObject json = new JsonObject();
    json.addProperty("isCitiesAndKnights", true);
    json.addProperty("numPlayers", 2);
    return new MasterReferee(new GameSettings(json));
  }

  @Test
  public void trackAdvancesResetsAndCaps() {
    BarbarianTrack track = new BarbarianTrack();
    assertEquals(0, track.getPosition());
    for (int i = 0; i < Settings.BARBARIAN_TRACK_LENGTH; i++) {
      assertFalse(track.hasReachedIsland());
      track.advance();
    }
    assertTrue(track.hasReachedIsland());
    assertEquals(Settings.BARBARIAN_TRACK_LENGTH, track.getPosition());
    // Cannot advance past the island.
    track.advance();
    assertEquals(Settings.BARBARIAN_TRACK_LENGTH, track.getPosition());
    track.reset();
    assertEquals(0, track.getPosition());
  }

  @Test
  public void defendersWinWhenKnightsMeetBarbarians() {
    assertTrue(BarbarianAttack.defendersWin(3, 3));
    assertTrue(BarbarianAttack.defendersWin(3, 5));
    assertFalse(BarbarianAttack.defendersWin(4, 3));
  }

  @Test
  public void defenderOfCatanIsTheLoneStrongest() {
    Map<Integer, Integer> strength = new HashMap<>();
    strength.put(0, 3);
    strength.put(1, 2);
    strength.put(2, 0);
    assertEquals(0, BarbarianAttack.defenderOfCatan(strength));

    // A tie for strongest yields no Defender.
    strength.put(1, 3);
    assertEquals(-1, BarbarianAttack.defenderOfCatan(strength));

    // Nobody contributed.
    Map<Integer, Integer> none = new HashMap<>();
    none.put(0, 0);
    none.put(1, 0);
    assertEquals(-1, BarbarianAttack.defenderOfCatan(none));
  }

  @Test
  public void weakestDefendersAreThoseWithFewestKnights() {
    Map<Integer, Integer> strength = new HashMap<>();
    strength.put(0, 3);
    strength.put(1, 0);
    strength.put(2, 0);
    Set<Integer> weakest = BarbarianAttack.weakestDefenders(strength);
    assertEquals(2, weakest.size());
    assertTrue(weakest.contains(1));
    assertTrue(weakest.contains(2));
    assertFalse(weakest.contains(0));
  }

  @Test
  public void resolveRewardsTheStrongestDefender() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    int p1 = ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);
    Player pb = ref.getPlayerByID(p1);

    Iterator<Intersection> it = ref.getBoard().getIntersections().values()
        .iterator();
    // p0 gets an active tier-2 knight; p1 gets a city (barbarian strength 1).
    Intersection knightInt = it.next();
    knightInt.placeKnight(pa);
    knightInt.getKnight().activate();
    knightInt.getKnight().upgrade();
    Intersection cityInt = it.next();
    cityInt.placeSettlement(pb);
    cityInt.placeCity(pb);

    int vpBefore = pa.numVictoryPoints();
    ref.resolveBarbarianAttack();

    // Knights (2) >= barbarians (1): p0 is the lone Defender of Catan.
    assertEquals(vpBefore + 1, pa.numVictoryPoints());
    // The attack deactivates knights and resets the fleet.
    assertFalse(knightInt.getKnight().isActive());
    assertEquals(0, ref.getBarbarianTrack().getPosition());
  }

  @Test
  public void resolveDowngradesTheWeakestDefendersCity() {
    MasterReferee ref = cnkReferee();
    int p0 = ref.addPlayer("A", "#000000");
    ref.addPlayer("B", "#111111");
    Player pa = ref.getPlayerByID(p0);

    Intersection cityInt = ref.getBoard().getIntersections().values()
        .iterator().next();
    cityInt.placeSettlement(pa);
    cityInt.placeCity(pa);
    assertTrue(cityInt.getBuilding() instanceof edu.brown.cs.board.City);

    // No active knights: barbarians win and the weakest defenders lose a city.
    ref.resolveBarbarianAttack();
    assertTrue(cityInt.getBuilding() instanceof edu.brown.cs.board.Settlement);
  }
}
