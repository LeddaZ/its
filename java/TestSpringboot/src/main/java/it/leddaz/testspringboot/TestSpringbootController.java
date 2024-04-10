package it.leddaz.testspringboot;

import it.leddaz.testspringboot.requests.NewOrderRequest;
import it.leddaz.testspringboot.requests.OrderIdRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for the application.
 *
 * @author Leonardo Ledda (LeddaZ)
 */
@RestController
public class TestSpringbootController {

  private static final Logger logger = LoggerFactory.getLogger(TestSpringbootController.class);
  private static final String CONN_STRING =
      "jdbc:sqlserver://localhost:1433;databaseName=TestJava;user=sa;password=Password0+;encrypt=true;trustServerCertificate=true";
  private static final String CONNECTING = "Connecting to SQL Server...";
  private static final String READY = "Ready!";
  private static final String ORDER_NOT_EXISTING = "The order does not exist.";
  private static Connection conn;

  private TestSpringbootController() {
    // Private constructor to hide the implicit public one
  }

  /**
   * Handles new order requests.
   *
   * @param request The request data
   * @return The result of the operation.
   */
  @PostMapping("/api/post/newOrder")
  public static String newOrder(@RequestBody NewOrderRequest request) {
    try {
      logger.info(CONNECTING);
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(CONN_STRING);
      logger.info(READY);
      String isSemifinished = "SELECT * FROM TArticoli WHERE ArticoloID = ?";
      PreparedStatement ps = conn.prepareStatement(isSemifinished);
      ps.setInt(1, request.getItemId());
      ResultSet rs = ps.executeQuery();
      int rowCount = 0;
      while (rs.next()) {
        rowCount++;
        if (!Objects.equals(rs.getString("Tipologia"), "SL")) {
          String msg = "The item is not semifinished.";
          logger.error(msg);
          return msg;
        }
      }
      if (rowCount == 0) {
        String msg = "The item does not exist.";
        logger.error(msg);
        return msg;
      }
      String query =
          "INSERT INTO TOrdini (ArticoloID, QuantitaDaProdurre) VALUES ("
              + request.getItemId()
              + ", "
              + request.getQuantity()
              + ")";
      ps = conn.prepareStatement(query);
      ps.execute();
      String msg = "Order added!";
      logger.info(msg);
      ps.close();
      rs.close();
      conn.close();
      return msg;
    } catch (SQLException e) {
      logger.error(e.getMessage());
      return e.getMessage();
    }
  }

  /**
   * Calculates the needs for an order.
   *
   * @param request The request data
   * @return The result of the operation.
   */
  @PostMapping("/api/post/calculateNeeds")
  public static String calculateNeeds(@RequestBody OrderIdRequest request) {
    try {
      logger.info(CONNECTING);
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(CONN_STRING);
      logger.info(READY);

      String deleteExisting = "DELETE FROM TFabbisogni WHERE OrdineID = " + request.getOrderId();
      PreparedStatement ps = conn.prepareStatement(deleteExisting);
      ps.execute();

      String itemQuantity = "SELECT ArticoloID, QuantitaDaProdurre FROM TOrdini WHERE OrdineID = ?";
      ps = conn.prepareStatement(itemQuantity);
      ps.setInt(1, request.getOrderId());
      ResultSet rs = ps.executeQuery();
      int itemId = 0;
      int quantity = 0;
      int rowCount = 0;
      if (rs.next()) {
        rowCount++;
        itemId = rs.getInt("ArticoloID");
        quantity = rs.getInt("QuantitaDaProdurre");
      }
      if (rowCount == 0) {
        logger.error(ORDER_NOT_EXISTING);
        return ORDER_NOT_EXISTING;
      }

      String legami =
          "SELECT ArticoloID_padre, CoefficienteFabbisogno FROM TLegami WHERE ArticoloID_figlio ="
              + " ?";
      ps = conn.prepareStatement(legami);
      ps.setInt(1, itemId);
      rs = ps.executeQuery();
      while (rs.next()) {
        rowCount++;
        int parentItem = rs.getInt("ArticoloID_padre");
        int needsCoefficient = rs.getInt("CoefficienteFabbisogno");
        int needsQuantity = needsCoefficient * quantity;
        String insertNeeds =
            "INSERT INTO TFabbisogni (OrdineID, ArticoloID, QuantitaFabbisogno) VALUES (?,?,?)";
        ps = conn.prepareStatement(insertNeeds);
        ps.setInt(1, request.getOrderId());
        ps.setInt(2, parentItem);
        ps.setInt(3, needsQuantity);
        ps.execute();
      }
      String msg = "Needs calculated!";
      logger.info(msg);
      ps.close();
      rs.close();
      conn.close();
      return msg;
    } catch (SQLException e) {
      logger.error(e.getMessage());
      return e.getMessage();
    }
  }

  /**
   * Gets the needs for an order.
   *
   * @param request The request data
   * @return The needs for the order.
   */
  @GetMapping("/api/get/getNeeds")
  public static String getNeeds(@RequestBody OrderIdRequest request) {
    try {
      logger.info(CONNECTING);
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(CONN_STRING);
      logger.info(READY);

      // Execute a query to fetch the needs of the order
      String query = "SELECT ArticoloID, QuantitaFabbisogno FROM TFabbisogni WHERE OrdineID = ?";
      PreparedStatement ps = conn.prepareStatement(query);
      ps.setInt(1, request.getOrderId());
      ResultSet rs = ps.executeQuery();

      // Display the needs of the order
      StringBuilder needs = new StringBuilder();
      int rowCount = 0;
      while (rs.next()) {
        rowCount++;
        int itemId = Integer.parseInt(rs.getString("ArticoloID"));
        int quantity = rs.getInt("QuantitaFabbisogno");
        needs
            .append("ID articolo: ")
            .append(itemId)
            .append(", Quantità necessaria: ")
            .append(quantity)
            .append("\n");
      }
      if (rowCount == 0) {
        logger.error(ORDER_NOT_EXISTING);
        return ORDER_NOT_EXISTING;
      }
      ps.close();
      rs.close();
      conn.close();
      return needs.toString();
    } catch (SQLException e) {
      logger.error(e.getMessage());
      return e.getMessage();
    }
  }

  /**
   * Processes an order.
   *
   * @param request The request data
   * @return The result of the operation.
   */
  @PatchMapping("/api/patch/processOrder")
  public static String processOrder(@RequestBody OrderIdRequest request) {
    String msg = "The order has been processed!";
    try {
      logger.info(CONNECTING);
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(CONN_STRING);
      logger.info(READY);

      String checkOrder = "SELECT ScaricoEffettuato FROM TOrdini WHERE OrdineID = ?";
      PreparedStatement ps = conn.prepareStatement(checkOrder);
      ps.setInt(1, request.getOrderId());
      ResultSet rs = ps.executeQuery();

      if (rs.next() && rs.getBoolean("ScaricoEffettuato")) {
        String msg2 = "The order has already been processed.";
        logger.error(msg2);
        return msg2;
      }

      String checkNeeds =
          "SELECT ArticoloID, QuantitaFabbisogno FROM TFabbisogni WHERE OrdineID = ?";
      ps = conn.prepareStatement(checkNeeds);
      ps.setInt(1, request.getOrderId());
      rs = ps.executeQuery();
      int rowCount = 0;
      while (rs.next()) {
        rowCount++;
        int itemId = Integer.parseInt(rs.getString("ArticoloID"));
        int quantity = rs.getInt("QuantitaFabbisogno");
        String decreaseQuantity =
            "UPDATE TArticoli SET Giacenza = TArticoli.Giacenza - ? WHERE ArticoloID = ?";
        ps = conn.prepareStatement(decreaseQuantity);
        ps.setInt(1, quantity);
        ps.setInt(2, itemId);
        ps.executeUpdate();

        String updateOrder = "UPDATE TOrdini SET ScaricoEffettuato = 1 WHERE OrdineID = ?";
        ps = conn.prepareStatement(updateOrder);
        ps.setInt(1, request.getOrderId());
        ps.executeUpdate();
        logger.info(msg);
      }
      if (rowCount == 0) {
        String msg3 = "Order not found.";
        logger.error(msg3);
        return msg3;
      }
      ps.close();
      rs.close();
      conn.close();
      return msg;
    } catch (SQLException e) {
      logger.error(e.getMessage());
      return e.getMessage();
    }
  }
}
