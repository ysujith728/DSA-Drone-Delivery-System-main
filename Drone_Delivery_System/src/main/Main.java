package main;

import models.*;
import services.*;

import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Drone Delivery Simulator (DSA-first) ===");

        DatabaseService DB = new DatabaseService();
        SegmentTreeService SEG = new SegmentTreeService(0, 500, 0, 500, 0, 1000); // grid bounds
        RouteService RT = new RouteService(DB, SEG);
        DroneService DS = new DroneService(DB, SEG, RT);
        WeatherService WS = new WeatherService(SEG);

        // Preload dataset (>= 15 entities for each type)
        DB.PSD(); // loads warehouses, customers, chargers, noflyzones (many entries)

        Scanner SC = new Scanner(System.in);
        boolean RUNNING = true;
        while (RUNNING) {
            PM();
            String CHOICE = SC.nextLine().trim();
            switch (CHOICE) {
                case "1":
                    LA(DB);
                    break;
                case "2":
                    CD(DS, DB, SC);
                    break;
                case "3":
                    SW(WS, SC);
                    break;
                case "4":
                    QA(SEG, SC);
                    break;
                case "5":
                    CC(SEG);
                    break;
                case "6":
                    RFTS(DS, DB, RT, WS);
                    break;
                case "0":
                    RUNNING = false;
                    System.out.println("Exiting. Goodbye.");
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }

        SC.close();
    }

    private static void PM() {
        System.out.println("\n--- MENU ---");
        System.out.println("1) List all data (warehouses / delivery points / chargers / no-fly)");
        System.out.println("2) Create Delivery (create drone and plan delivery)");
        System.out.println("3) Simulate Weather (mark region unsafe)");
        System.out.println("4) Query Airspace (count drones / unsafe cells)");
        System.out.println("5) Run Collision Check (report overlaps)");
        System.out.println("6) Run Full Test Suite (automated)");
        System.out.println("0) Exit");
        System.out.print("Choice: ");
    }

    private static void LA(DatabaseService DB) 
    {
        System.out.println("\nWarehouses:");
        for (Warehouse w : DB.LW()) 
            System.out.println("  " + w);
            
        System.out.println("\nDelivery Points:");
        for (DeliveryPoint d : DB.LDP())
            System.out.println("  " + d);

        System.out.println("\nCharging Stations:");
        for (ChargingStation c : DB.LCS()) 
            System.out.println("  " + c);

        System.out.println("\nNo-Fly Zones:");
        for (NoFlyZone n : DB.LNFZ()) 
            System.out.println("  " + n);
    }

    private static void CD(DroneService DS, DatabaseService DB, Scanner SC) 
    {
        System.out.println("\nAvailable Warehouses:");
        for (Warehouse w : DB.LW()) 
        {
            System.out.println("  " + w.ID + ": " + w.NM + " at (" + w.X + "," + w.Y + "," + w.Z + ")");
        }

        System.out.print("Select Warehouse ID: ");
        String WH_ID = SC.nextLine().trim();
        
        System.out.println("\nAvailable Delivery Points:");
        for (DeliveryPoint dp : DB.LDP()) 
        {
            System.out.println("  " + dp.ID + ": " + dp.ADDR + " at (" + dp.X + "," + dp.Y + "," + dp.Z + ")");
        }

        System.out.print("Select Delivery Point ID: ");
        String DP_ID = SC.nextLine().trim();
        
        // Create drone at selected warehouse
        Drone drone = DS.CDW(null, WH_ID);
        if (drone == null) 
        {
            System.out.println("Error: Invalid warehouse ID");
            return;
        }
        
        System.out.println("Created drone: " + drone);
        
        // Plan delivery
        boolean success = DS.PD(drone.ID, DP_ID);
        if (success) 
        {
            System.out.println("Delivery planned successfully!");
            System.out.println("Drone status: " + drone);
            System.out.println("Reserved path length: " + drone.RES_PATH.size());
        } 
        else 
        {
            System.out.println("Failed to plan delivery - check no-fly zones or weather conditions");
        }
    }

    private static void SW(WeatherService WS, Scanner SC) 
    {
        System.out.print("Enter region bounds (x1 x2 y1 y2 z1 z2): ");
        String[] P = SC.nextLine().trim().split("\\s+");

        if (P.length != 6) 
        {
            System.out.println("Invalid input. Please enter 6 numbers.");
            return;
        }

        int X1 = Integer.parseInt(P[0]), X2 = Integer.parseInt(P[1]);
        int Y1 = Integer.parseInt(P[2]), Y2 = Integer.parseInt(P[3]);
        int Z1 = Integer.parseInt(P[4]), Z2 = Integer.parseInt(P[5]);
        WS.MS(X1, X2, Y1, Y2, Z1, Z2);

        System.out.println("Marked weather (unsafe) region.");
    }

    private static void QA(SegmentTreeService SEG, Scanner SC) 
    {
        System.out.print("Enter region to query (x1 x2 y1 y2 z1 z2): ");
        String[] P = SC.nextLine().trim().split("\\s+");

        if (P.length != 6) 
        {
            System.out.println("Invalid input. Please enter 6 numbers.");
            return;
        }
        int X1 = Integer.parseInt(P[0]), X2 = Integer.parseInt(P[1]);
        int Y1 = Integer.parseInt(P[2]), Y2 = Integer.parseInt(P[3]);
        int Z1 = Integer.parseInt(P[4]), Z2 = Integer.parseInt(P[5]);
        int CNT = SEG.CNT_DR_IN_REG(X1, X2, Y1, Y2, Z1, Z2);
        int UNSAFE = SEG.CNT_UNSAFE_IN_REG(X1, X2, Y1, Y2, Z1, Z2);

        System.out.println("Drones in region: " + CNT + ", Unsafe cells: " + UNSAFE);
    }

    private static void CC(SegmentTreeService SEG) 
    {
        List<String> OVERLAPS = SEG.FO();
        if (OVERLAPS.isEmpty()) System.out.println("No overlaps detected.");
        else 
        {
            System.out.println("Overlaps detected:");
            for (String S : OVERLAPS) System.out.println("  " + S);
        }
    }

    private static void RFTS(DroneService DS, DatabaseService DB, RouteService RT, WeatherService WS) 
    {
        System.out.println("Running automated test suite (multiple scenarios)...");
        testcases.RUN_ALL(DS, DB, RT, WS);
        System.out.println("Automated test suite finished.");
    }
}