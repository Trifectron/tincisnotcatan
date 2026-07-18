package edu.brown.cs.board;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.brown.cs.catan.HumanPlayer;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Settings;

public class KnightTest {

  private final Player player = new HumanPlayer(0, "test", "#000000");

  @Test
  public void newKnightIsBasicAndInactive() {
    Knight k = new Knight(player);
    assertEquals(1, k.getTier());
    assertFalse(k.isActive());
    assertEquals(player, k.getPlayer());
  }

  @Test
  public void activateAndDeactivate() {
    Knight k = new Knight(player);
    k.activate();
    assertTrue(k.isActive());
    k.deactivate();
    assertFalse(k.isActive());
  }

  @Test
  public void upgradeStopsAtMaxTier() {
    Knight k = new Knight(player);
    for (int i = 0; i < Settings.MAX_KNIGHT_TIER + 2; i++) {
      k.upgrade();
    }
    assertEquals(Settings.MAX_KNIGHT_TIER, k.getTier());
    assertFalse(k.canUpgrade());
  }

  @Test
  public void intersectionHoldsAndRemovesKnight() {
    IntersectionCoordinate coord = new IntersectionCoordinate(
        new HexCoordinate(0, 0, 0), new HexCoordinate(1, 0, 0),
        new HexCoordinate(1, 1, 0));
    Intersection i = new Intersection(coord);
    assertFalse(i.hasKnight());
    i.placeKnight(player);
    assertTrue(i.hasKnight());
    assertEquals(player, i.getKnight().getPlayer());
    i.removeKnight();
    assertFalse(i.hasKnight());
    assertNull(i.getKnight());
  }

  @Test
  public void downgradeCityBecomesSettlement() {
    IntersectionCoordinate coord = new IntersectionCoordinate(
        new HexCoordinate(0, 0, 0), new HexCoordinate(1, 0, 0),
        new HexCoordinate(1, 1, 0));
    Intersection i = new Intersection(coord);
    i.placeSettlement(player);
    i.placeCity(player);
    assertTrue(i.getBuilding() instanceof City);
    i.downgradeCity();
    assertTrue(i.getBuilding() instanceof Settlement);
    assertEquals(player, i.getBuilding().getPlayer());
  }
}
