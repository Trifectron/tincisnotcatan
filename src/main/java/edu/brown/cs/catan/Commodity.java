package edu.brown.cs.catan;

/**
 * Represents a Cities &amp; Knights commodity. Commodities (paper, cloth, coin)
 * are a second currency produced by cities alongside resources and spent on
 * city improvements.
 *
 */
public enum Commodity {

  PAPER("paper", "a paper"), CLOTH("cloth", "a cloth"), COIN("coin", "a coin");

  private final String _description;
  private final String _withArticle;

  private Commodity(String description, String withArticle) {
    _description = description;
    _withArticle = withArticle;
  }

  @Override
  public String toString() {
    return _description;
  }

  public String stringWithArticle() {
    return _withArticle;
  }

  public static Commodity stringToCommodity(String str) {
    switch (str) {
    case "paper":
      return PAPER;
    case "cloth":
      return CLOTH;
    case "coin":
      return COIN;
    default:
      throw new IllegalArgumentException(String.format(
          "The commodity %s does not exist.", str));
    }
  }

  /**
   * The commodity a city produces from a given resource hex under Cities &amp;
   * Knights: ore produces coin, wool (sheep) produces cloth, lumber (wood)
   * produces paper. Brick and grain (wheat) yield no commodity.
   *
   * @param res
   *          The resource the hex produces.
   * @return The mapped Commodity, or null if the resource yields no commodity.
   */
  public static Commodity fromResource(Resource res) {
    switch (res) {
    case ORE:
      return COIN;
    case SHEEP:
      return CLOTH;
    case WOOD:
      return PAPER;
    default:
      return null;
    }
  }
}
