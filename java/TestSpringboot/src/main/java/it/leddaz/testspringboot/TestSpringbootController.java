package it.leddaz.testspringboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.leddaz.testspringboot.requests.ItemIdRequest;
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
  private static final String ITEM_ID_COL = "ArticoloID";
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
        if (Objects.equals(rs.getString("Tipologia"), "SL")) {
          String msg = "The item is not a finished product.";
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
        itemId = rs.getInt(ITEM_ID_COL);
        quantity = rs.getInt("QuantitaDaProdurre");
      }
      if (rowCount == 0) {
        logger.error(ORDER_NOT_EXISTING);
        return ORDER_NOT_EXISTING;
      }

      String relationship =
          "SELECT ArticoloID_figlio, CoefficienteFabbisogno FROM TLegami WHERE ArticoloID_padre ="
              + " ?";
      ps = conn.prepareStatement(relationship);
      ps.setInt(1, itemId);
      rs = ps.executeQuery();
      while (rs.next()) {
        rowCount++;
        int parentItem = rs.getInt("ArticoloID_figlio");
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
  public static ArrayNode getNeeds(@RequestBody OrderIdRequest request) {
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
      int rowCount = 0;
      ObjectMapper mapper = new ObjectMapper();
      ArrayNode jsonArray = mapper.createArrayNode();
      while (rs.next()) {
        rowCount++;
        int itemId = Integer.parseInt(rs.getString(ITEM_ID_COL));
        int quantity = rs.getInt("QuantitaFabbisogno");
        ObjectNode jo = mapper.createObjectNode();
        jo.put("itemId", itemId);
        jo.put("quantity", quantity);
        jsonArray.add(jo);
      }
      if (rowCount == 0) {
        logger.error(ORDER_NOT_EXISTING);
        ObjectNode jErr = mapper.createObjectNode();
        jErr.put("error", ORDER_NOT_EXISTING);
        jsonArray.add(jErr);
        return jsonArray;
      }
      ps.close();
      rs.close();
      conn.close();
      return jsonArray;
    } catch (SQLException e) {
      logger.error(e.getMessage());
      ObjectMapper mapper = new ObjectMapper();
      ArrayNode jsonArray = mapper.createArrayNode();
      ObjectNode jErr = mapper.createObjectNode();
      jErr.put("error", e.getMessage());
      jsonArray.add(jErr);
      return jsonArray;
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
        int itemId = Integer.parseInt(rs.getString(ITEM_ID_COL));
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

  @PatchMapping("/api/patch/updateFpPrice")
  public static String updateFpPrice(@RequestBody ItemIdRequest request) {
    try {
      logger.info(CONNECTING);
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(CONN_STRING);
      logger.info(READY);

      String isFinishedProduct = "SELECT * FROM TArticoli WHERE ArticoloID = ?";
      PreparedStatement ps = conn.prepareStatement(isFinishedProduct);
      ps.setInt(1, request.getItemId());
      ResultSet rs = ps.executeQuery();
      int rowCount = 0;
      while (rs.next()) {
        rowCount++;
        if (Objects.equals(rs.getString("Tipologia"), "SL")) {
          String msg = "The item is not a finished product.";
          logger.error(msg);
          return msg;
        }
      }
      if (rowCount == 0) {
        String msg = "The item does not exist.";
        logger.error(msg);
        return msg;
      }

      String relationship = "SELECT ArticoloID_figlio FROM TLegami WHERE ArticoloID_padre = ?";
      ps = conn.prepareStatement(relationship);
      ps.setInt(1, request.getItemId());
      rs = ps.executeQuery();
      double price = 0;
      while (rs.next()) {
        int childItem = rs.getInt("ArticoloID_figlio");
        String sfPrice = "SELECT CostoUnitario FROM TArticoli WHERE ArticoloID = ?";
        ps = conn.prepareStatement(sfPrice);
        ps.setInt(1, childItem);
        ResultSet res = ps.executeQuery();
        res.next();
        price += res.getDouble("CostoUnitario");
        res.close();
      }

      String updateFpPrice = "UPDATE TArticoli SET CostoUnitario = ? WHERE ArticoloID = ?";
      ps = conn.prepareStatement(updateFpPrice);
      ps.setDouble(1, price);
      ps.setInt(2, request.getItemId());
      ps.execute();
      String msg = "Price updated!";
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

  @PatchMapping("/api/patch/updateSfPrice")
  public static String updateSfPrice(@RequestBody OrderIdRequest request) {
    try {
      logger.info(CONNECTING);
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(CONN_STRING);
      logger.info(READY);

      String parentItem = "SELECT ArticoloID FROM TOrdini WHERE OrdineID = ?";
      PreparedStatement ps = conn.prepareStatement(parentItem);
      ps.setInt(1, request.getOrderId());
      ResultSet rs = ps.executeQuery();
      int itemId = 0;
      if (rs.next()) itemId = rs.getInt(ITEM_ID_COL);
      else {
        logger.error(ORDER_NOT_EXISTING);
        return ORDER_NOT_EXISTING;
      }

      String itemPrice = "SELECT CostoUnitario FROM TArticoli WHERE ArticoloID = ?";
      ps = conn.prepareStatement(itemPrice);
      ps.setInt(1, itemId);
      rs = ps.executeQuery();
      rs.next();
      double price = rs.getDouble("CostoUnitario");

      String orderQuantitySql = "SELECT QuantitaDaProdurre FROM TOrdini WHERE OrdineID = ?";
      ps = conn.prepareStatement(orderQuantitySql);
      ps.setInt(1, request.getOrderId());
      rs = ps.executeQuery();
      rs.next();
      int quantity = rs.getInt("QuantitaDaProdurre");

      String updateSfPriceSql = "UPDATE TOrdini SET CostoSL = ? WHERE OrdineID = ?";
      ps = conn.prepareStatement(updateSfPriceSql);
      ps.setDouble(1, price * quantity);
      ps.setInt(2, request.getOrderId());
      ps.execute();
      ps.close();
      rs.close();
      conn.close();
      String msg = "Price updated!";
      logger.info(msg);
      return msg;
    } catch (SQLException e) {
      logger.error(e.getMessage());
      return e.getMessage();
    }
  }
}
