package it.leddaz.testspringboot;

import it.leddaz.testspringboot.requests.CalculateNeedsRequest;
import it.leddaz.testspringboot.requests.NewOrderRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private static Connection conn;

  private TestSpringbootController() {
    // Private constructor to hide the implicit public one
  }

  /**
   * Handles new order requests.
   *
   * @param request The request data
   */
  @PostMapping("/api/post/newOrder")
  public static void newOrder(@RequestBody NewOrderRequest request) {
    try {
      logger.info("Connecting to SQL Server...");
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(CONN_STRING);
      logger.info("Ready!");
      String isSemifinished = "SELECT * FROM TArticoli WHERE ArticoloID = ?";
      PreparedStatement ps = conn.prepareStatement(isSemifinished);
      ps.setInt(1, request.getItemId());
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        if (!Objects.equals(rs.getString("Tipologia"), "SL")) {
          logger.error("The item is not semifinished.");
          return;
        }
      }
      String query =
          "INSERT INTO TOrdini (ArticoloID, QuantitaDaProdurre) VALUES ("
              + request.getItemId()
              + ", "
              + request.getQuantity()
              + ")";
      ps = conn.prepareStatement(query);
      ps.execute();
      logger.info("Order added!");
      ps.close();
      conn.close();
    } catch (SQLException e) {
      logger.error(e.getMessage());
    }
  }

  /**
   * Calculates the needs for an order.
   *
   * @param request The request data
   */
  @PostMapping("/api/post/calculateNeeds")
  public static void calculateNeeds(@RequestBody CalculateNeedsRequest request) {
    try {
      logger.info("Connecting to SQL Server...");
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(CONN_STRING);
      logger.info("Ready!");

      String deleteExisting = "DELETE FROM TFabbisogni WHERE OrdineID = " + request.getOrderId();
      PreparedStatement ps = conn.prepareStatement(deleteExisting);
      ps.execute();

      String itemQuantity = "SELECT ArticoloID, QuantitaDaProdurre FROM TOrdini WHERE OrdineID = ?";
      ps = conn.prepareStatement(itemQuantity);
      ps.setInt(1, request.getOrderId());
      ResultSet rs = ps.executeQuery();
      int itemId = 0;
      int quantity = 0;
      if (rs.next()) {
        itemId = rs.getInt("ArticoloID");
        quantity = rs.getInt("QuantitaDaProdurre");
      }

      String legami =
          "SELECT ArticoloID_figlio, CoefficienteFabbisogno FROM TLegami WHERE ArticoloID_padre ="
              + " ?";
      ps = conn.prepareStatement(legami);
      ps.setInt(1, itemId);
      rs = ps.executeQuery();
      while (rs.next()) {
        int childItem = rs.getInt("ArticoloID_figlio");
        int needsCoefficient = rs.getInt("CoefficienteFabbisogno");
        int needsQuantity = needsCoefficient * quantity;
        String insertNeeds =
            "INSERT INTO TFabbisogni (OrdineID, ArticoloID, QuantitaFabbisogno) VALUES (?,?,?)";
        ps = conn.prepareStatement(insertNeeds);
        ps.setInt(1, request.getOrderId());
        ps.setInt(2, childItem);
        ps.setInt(3, needsQuantity);
        ps.execute();
      }
      logger.info("Needs calculated!");
      conn.close();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
