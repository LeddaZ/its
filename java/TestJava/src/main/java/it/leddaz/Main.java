package it.leddaz;

import java.sql.*;
import java.util.Objects;
import java.util.Scanner;

public class Main {

    private static final String connStr = "jdbc:sqlserver://localhost:1433;databaseName=TestJava;user=sa;password=Password0+;encrypt=true;trustServerCertificate=true";
    private static Connection conn;

    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        while (true) {
            System.out.println("--- Calcolo fabbisogni ---\n");
            System.out.println("Scegli un opzione: ");
            System.out.println("1) Nuovo ordine");
            System.out.println("2) Calcola fabbisogno ordine");
            System.out.println("3) Visualizza fabbisogno ordine");
            System.out.println("4) Scarico magazzino");
            System.out.println("5) Esci");
            int opt = s.nextInt();
            s.nextLine();
            switch (opt) {
                case 1:
                    System.out.print("ID articolo: ");
                    int aId = s.nextInt();
                    System.out.print("Quantità: ");
                    int q = s.nextInt();
                    newOrder(aId, q);
                    break;
                case 2:
                    System.out.print("ID ordine: ");
                    calculateNeeds(s.nextInt());
                    break;
                case 3:
                    System.out.print("ID ordine: ");
                    getNeeds(s.nextInt());
                    break;
                case 4:
                    System.out.print("ID ordine: ");
                    processOrder(s.nextInt());
                    break;
                case 5:
                    System.out.println("Ciao");
                    System.exit(0);
                    break;
            }
        }
    }

    private static void newOrder(int aId, int q) {
        try {
            System.out.println("Connessione al DB...");
            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
            conn = DriverManager.getConnection(connStr);
            System.out.println("Pronti!");
            String isSemilavorato = "SELECT * FROM TArticoli WHERE ArticoloID = ?";
            PreparedStatement ps = conn.prepareStatement(isSemilavorato);
            ps.setInt(1, aId);
            ResultSet rs = ps.executeQuery();
            while(rs.next()) {
                if (!Objects.equals(rs.getString("Tipologia"), "SL")) {
                    System.out.println("L'oggetto non è un semilavorato.");
                    return;
                }
            }
            String query = "INSERT INTO TOrdini (ArticoloID, QuantitaDaProdurre) VALUES (" + aId + ", " + q + ")";
            ps = conn.prepareStatement(query);
            ps.execute();
            System.out.println("Ordine inserito!");
            conn.close();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void calculateNeeds(int oId) {
        try {
            System.out.println("Connessione al DB...");
            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
            conn = DriverManager.getConnection(connStr);
            System.out.println("Pronti!");

            String deleteExisting = "DELETE FROM TFabbisogni WHERE OrdineID = " + oId;
            PreparedStatement ps = conn.prepareStatement(deleteExisting);
            ps.execute();

            String qArticoli = "SELECT ArticoloID, QuantitaDaProdurre FROM TOrdini WHERE OrdineID = ?";
            ps = conn.prepareStatement(qArticoli);
            ps.setInt(1, oId);
            ResultSet rs = ps.executeQuery();
            int ArticoloID = 0, QuantitaDaProdurre = 0;
            if (rs.next()) {
                ArticoloID = rs.getInt("ArticoloID");
                QuantitaDaProdurre = rs.getInt("QuantitaDaProdurre");
            }

            String legami = "SELECT ArticoloID_figlio, CoefficienteFabbisogno FROM TLegami WHERE ArticoloID_padre = ?";
            ps = conn.prepareStatement(legami);
            ps.setInt(1, ArticoloID);
            rs = ps.executeQuery();
            while (rs.next()) {
                int ArticoloFiglio = rs.getInt("ArticoloID_figlio");
                int CoefficienteFabbisogno = rs.getInt("CoefficienteFabbisogno");
                int QuantitaFabbisogno = CoefficienteFabbisogno * QuantitaDaProdurre;
                String insertFabbisogni = "INSERT INTO TFabbisogni (OrdineID, ArticoloID, QuantitaFabbisogno) VALUES (?,?,?)";
                ps = conn.prepareStatement(insertFabbisogni);
                ps.setInt(1, oId);
                ps.setInt(2, ArticoloFiglio);
                ps.setInt(3, QuantitaFabbisogno);
                ps.execute();
            }
            System.out.println("Fabbisogno calcolato!");
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void getNeeds(int oId) {
        try {
            System.out.println("Connessione al DB...");
            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
            conn = DriverManager.getConnection(connStr);
            System.out.println("Pronti!");

            // Execute a query to fetch the needs of the order
            String query = "SELECT ArticoloID, QuantitaFabbisogno FROM TFabbisogni WHERE OrdineID = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setInt(1, oId);
            ResultSet rs = ps.executeQuery();

            // Display the needs of the order
            while (rs.next()) {
                int aId = Integer.parseInt(rs.getString("ArticoloID"));
                int q = rs.getInt("QuantitaFabbisogno");
                System.out.println("ID articolo: " + aId + ", Quantità necessaria: " + q);
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void processOrder(int oId) {
        try {
            System.out.println("Connessione al DB...");
            DriverManager.registerDriver(new com.microsoft.sqlserver.jdbc.SQLServerDriver());
            conn = DriverManager.getConnection(connStr);
            System.out.println("Pronti!");

            String checkOrder = "SELECT ScaricoEffettuato FROM TOrdini WHERE OrdineID = ?";
            PreparedStatement ps = conn.prepareStatement(checkOrder);
            ps.setInt(1, oId);
            ResultSet rs = ps.executeQuery();

            if (rs.next() && rs.getBoolean("ScaricoEffettuato")) {
                System.out.println("The order has already been processed.");
                return;
            }

            String checkNeeds = "SELECT ArticoloID, QuantitaFabbisogno FROM TFabbisogni WHERE OrdineID = ?";
            ps = conn.prepareStatement(checkNeeds);
            ps.setInt(1, oId);
            rs = ps.executeQuery();

            while (rs.next()) {
                int aId = Integer.parseInt(rs.getString("ArticoloID"));
                int quantity = rs.getInt("QuantitaFabbisogno");
                String decreaseQuantity = "UPDATE TArticoli SET Giacenza = TArticoli.Giacenza - ? WHERE ArticoloID = ?";
                ps = conn.prepareStatement(decreaseQuantity);
                ps.setInt(1, quantity);
                ps.setInt(2, aId);
                ps.executeUpdate();

                String updateOrder = "UPDATE TOrdini SET ScaricoEffettuato = 1 WHERE OrdineID = ?";
                ps = conn.prepareStatement(updateOrder);
                ps.setInt(1, oId);
                ps.executeUpdate();
                System.out.println("Ordine processato!");
            }
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
