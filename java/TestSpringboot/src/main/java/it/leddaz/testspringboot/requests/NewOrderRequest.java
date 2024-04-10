package it.leddaz.testspringboot.requests;

/**
 * Stores data for new order requests.
 *
 * @author Leonardo Ledda (LeddaZ)
 */
public class NewOrderRequest {

  private int itemId;
  private int quantity;

  public int getItemId() {
    return itemId;
  }

  public int getQuantity() {
    return quantity;
  }
}
