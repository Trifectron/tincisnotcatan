package edu.brown.cs.catan;

/**
 * A Cities &amp; Knights city-improvement track. Each track is advanced by
 * spending its matching commodity: Trade with cloth, Politics with coin, and
 * Science with paper. Advancing to level N costs N commodities.
 *
 */
public enum CityImprovement {

  TRADE(Commodity.CLOTH), POLITICS(Commodity.COIN), SCIENCE(Commodity.PAPER);

  /** Highest attainable level on an improvement track. */
  public static final int MAX_LEVEL = 5;

  private final Commodity _commodity;

  private CityImprovement(Commodity commodity) {
    _commodity = commodity;
  }

  /**
   * @return The commodity spent to advance this track.
   */
  public Commodity getCommodity() {
    return _commodity;
  }

  /**
   * @param commodity
   *          A commodity.
   * @return The improvement track advanced by that commodity.
   */
  public static CityImprovement fromCommodity(Commodity commodity) {
    for (CityImprovement improvement : values()) {
      if (improvement._commodity == commodity) {
        return improvement;
      }
    }
    throw new IllegalArgumentException(String.format(
        "No improvement track for commodity %s.", commodity));
  }

  /**
   * @param str
   *          A track name (case-insensitive), e.g. "trade".
   * @return The matching CityImprovement.
   */
  public static CityImprovement fromString(String str) {
    return valueOf(str.toUpperCase());
  }
}
