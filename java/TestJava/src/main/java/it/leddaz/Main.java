package it.leddaz;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner s = new Scanner(System.in);
        while(true) {
            System.out.println("--- Calcolo fabbisogni ---\n\n");
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
                    nuovoOrdine(s.nextInt());
                    break;
                case 2:
                    //calcolaFabbisogno();
                    break;
                case 3:
                    //visualizzaFabbisogno();
                    break;
                case 4:
                    //scaricoMagazzino();
                    break;
                case 5:
                    System.exit(0);
                    break;
            }
        }
    }

    private static void nuovoOrdine(int aId) {

    }
}
