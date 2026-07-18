package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProgressCardTest {

  @Test
  public void cardMetadata() {
    assertEquals(CityImprovement.SCIENCE, ProgressCardType.PRINTER.getDeck());
    assertEquals(CityImprovement.POLITICS,
        ProgressCardType.CONSTITUTION.getDeck());
    assertEquals(ProgressCardType.PRINTER,
        ProgressCardType.fromString("printer"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void unknownCardNameThrows() {
    ProgressCardType.fromString("Sorcery");
  }

  @Test
  public void deckHoldsOnlyItsTrackAndDrainsToEmpty() {
    ProgressCardDeck science = new ProgressCardDeck(CityImprovement.SCIENCE);
    assertEquals(1, science.size());
    assertEquals(ProgressCardType.PRINTER, science.draw());
    assertTrue(science.isEmpty());
    assertNull(science.draw());

    // A track with no wired cards yields an empty deck, not an error.
    assertTrue(new ProgressCardDeck(CityImprovement.TRADE).isEmpty());
  }

  @Test
  public void playerHoldsAndPlaysCards() {
    Player p = new HumanPlayer(0, "test", "#000000");
    assertTrue(p.getProgressCards().isEmpty());

    p.addProgressCard(ProgressCardType.PRINTER);
    assertEquals(1, p.getProgressCards().size());
    assertTrue(p.getProgressCards().contains(ProgressCardType.PRINTER));

    int before = p.numVictoryPoints();
    // The wired effects do not use the referee, so null is safe here.
    ProgressCardType.PRINTER.play(null, p);
    assertEquals(before + 1, p.numVictoryPoints());

    assertTrue(p.removeProgressCard(ProgressCardType.PRINTER));
    assertTrue(p.getProgressCards().isEmpty());
    assertFalse(p.removeProgressCard(ProgressCardType.PRINTER));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void readOnlyPlayerCannotAddCards() {
    Player p = new HumanPlayer(0, "test", "#000000").getImmutableCopy();
    p.addProgressCard(ProgressCardType.PRINTER);
  }
}
