package it.leddaz.testspringboot;

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
  private static final String connStr =
      "jdbc:sqlserver://localhost:1433;databaseName=TestJava;user=sa;password=Password0+;encrypt=true;trustServerCertificate=true";
  private static Connection conn;

  /**
   * Handles new order requests.
   *
   * @param request The request data
   */
  @PostMapping("/api/post/newOrder")
  public void newOrder(@RequestBody NewOrderRequest request) {
    try {
      logger.info("Connecting to SQL Server...");
      DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
      conn = DriverManager.getConnection(connStr);
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
}
