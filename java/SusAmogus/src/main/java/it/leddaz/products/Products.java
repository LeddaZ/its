package it.leddaz.products;

import java.sql.*;
import java.util.Scanner;

/**
 * Sample SQL interactions
 *
 * @author Leonardo Ledda (LeddaZ)
 */
public class Products {

    private static final String connStr = "jdbc:sqlserver://localhost;databaseName=caramba;integratedSecurity=true;encrypt=true;trustServerCertificate=true;";
    private static final Scanner scanner = new Scanner(System.in);
    private static Connection conn;

    public static void main(String[] args) {
        try {
            System.out.println("Connecting...");
            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
            conn = DriverManager.getConnection(connStr);
            System.out.println("The driver has been registered!");
            System.out.println("Ready");
            System.out.println("Choose an option: ");
            System.out.println("1) List all products");
            System.out.println("2) Insert a new article");
            System.out.println("3) Delete an article");
            System.out.println("4) Update an article's description");
            System.out.println("5) Exit");
            int option = scanner.nextInt();
            scanner.nextLine();
            switch (option) {
                case 1:
                    list();
                    break;
                case 2:
                    insert();
                    break;
                case 3:
                    delete();
                    break;
                case 4:
                    updateDesc();
                    break;
                case 5:
                    System.out.println("Good bye");
                    return;
                default:
                    System.out.println("Invalid option");
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Lists all products
     */
    public static void list() {
        try {
            Statement myStatement = conn.createStatement();
            String query = "SELECT * FROM TabArticoli";
            ResultSet result = myStatement.executeQuery(query);
            ResultSetMetaData rsmd = result.getMetaData();
            while (result.next()) {
                System.out.println("----------");
                for (int i = 1; i <= 6; i++) {
                    System.out.print(rsmd.getColumnName(i) + ": ");
                    System.out.println(result.getString(i));
                }
            }
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Inserts a new product
     */
    public static void insert() {
        System.out.print("Name: ");
        String name = scanner.nextLine();
        System.out.print("Description: ");
        String desc = scanner.nextLine();
        System.out.print("Price: ");
        double price = scanner.nextDouble();
        System.out.print("Stock: ");
        int stock = scanner.nextInt();
        System.out.print("VAT: ");
        int vat = scanner.nextInt();
        try {
            String query = "INSERT INTO TabArticoli (Nome, Descrizione, Prezzo, Giacenza, IVA) "
                    + "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement prep = conn.prepareStatement(query);
            prep.setString(1, name);
            prep.setString(2, desc);
            prep.setDouble(3, price);
            prep.setInt(4, stock);
            prep.setInt(5, vat);
            prep.executeUpdate();
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Deletes a product
     */
    public static void delete() {
        System.out.print("Product ID: ");
        int id = scanner.nextInt();
        try {
            String query = "DELETE FROM TabArticoli WHERE ID = ?";
            PreparedStatement prep = conn.prepareStatement(query);
            prep.setInt(1, id);
            prep.executeUpdate();
            System.out.println("Done");
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Updates product description
     */
    public static void updateDesc() {
        System.out.print("Product ID: ");
        int id = scanner.nextInt();
        scanner.nextLine();
        System.out.print("New description: ");
        String newDesc = scanner.nextLine();
        try {
            String query = "UPDATE TabArticoli SET Descrizione = ? WHERE ID = ?";
            PreparedStatement prep = conn.prepareStatement(query);
            prep.setString(1, newDesc);
            prep.setInt(2, id);
            prep.executeUpdate();
            System.out.println("Done");
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
