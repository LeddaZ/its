package it.leddaz.testspringboot.requests;

/**
 * Stores data for requests with an order ID.
 *
 * @author Leonardo Ledda (LeddaZ)
 */
public class OrderIdRequest {

  private int orderId;

  /**
   * Returns the order ID.
   *
   * @return The order ID
   */
  public int getOrderId() {
    return orderId;
  }
}
