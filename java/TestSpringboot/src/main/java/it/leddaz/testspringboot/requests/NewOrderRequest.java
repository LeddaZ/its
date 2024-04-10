package it.leddaz.testspringboot.requests;

/**
 * Stores data for new order requests.
 *
 * @author Leonardo Ledda (LeddaZ)
 */
public class NewOrderRequest {

  private int itemId;
  private int quantity;

  /**
   * Returns the item ID.
   *
   * @return The item ID
   */
  public int getItemId() {
    return itemId;
  }

  /**
   * Returns the quantity.
   *
   * @return The quantity
   */
  public int getQuantity() {
    return quantity;
  }
}
