
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Nina Eriksson
 * Date: 2021-02-19
 * Time: 07:45
 * Project: webbshop
 * Copyright: MIT
 */

public class Webbshop {

    static Scanner scanner = new Scanner(System.in);
    static String userNameFromInput;
    static String passwordFromInput;
    static int userId;
    static String userInput;
    static boolean exit = false;


    public static void main(String[] args) throws SQLException {
        printLogInMenu();
        do {
            if (checkUser(userNameFromInput, passwordFromInput) == true) {
                printMenu();
                userInput = scanner.nextLine();

                if (userInput.equals("1")) {
                    createOrder();
                    userInput = "";
                }
                if (userInput.equals("2")) {
                    printOrder();
                }
                if (userInput.equals("3")) {
                    exit = true;
                    System.out.println("Programmet avslutas!");
                    System.exit(0);
                }
            } else {
                printLogInMenu();
            }
        } while (!exit);
    }

    public static boolean checkUser(String userNameFromInput, String passwordFromInput) {
        ResultSet rs;
        Boolean validUser = null;

        String query =
                "SELECT id, fullname, cpassword" + " FROM customer" +
                        " WHERE fullname = ? AND cpassword = ?";

        try (Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/webbshop_uppg2?serverTimezone=UTC&useSSL=false", "nina", "nina");
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setString(1, userNameFromInput + "");
            stmt.setString(2, passwordFromInput + "");
            rs = stmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("Felaktigt användarnamn och/eller lösenord!");
                validUser = false;
            } else {
                while (rs.next()) {
                    userId = rs.getInt("id");
                    validUser = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return validUser;
    }


    public static void printLogInMenu() {
        userNameFromInput = "";
        passwordFromInput = "";
        System.out.println("------------------------");
        System.out.println("Ange för- och efternamn:");
        userNameFromInput = scanner.nextLine();
        System.out.println("Ange lösenord:");
        passwordFromInput = scanner.nextLine();
    }

    public static void printMenu() {
        System.out.println("1. Gör en beställning.");
        System.out.println("2. Skriv ut beställning.");
        System.out.println("3. Avsluta.");
    }


    public static void createOrder() {
        ResultSet rs;
        int shoeIdInDatabase = 0;
        int orderIdInDatabase = 0;
        int rowsBeforeOrder = 0;
        int rowsAfterOrder = 0;
        List<Shoes> tempShoes = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/webbshop_uppg2?serverTimezone=UTC&useSSL=false", "nina", "nina")) {
            Statement stmt = con.createStatement();

            rs = stmt.executeQuery("SELECT COUNT(*) AS rowcount FROM orderrow");
            rs.next();
            rowsBeforeOrder = rs.getInt("rowcount");
            //System.out.println("rowsBeforeOrder: " + rowsBeforeOrder);

            rs = stmt.executeQuery("SELECT shoes.id, shoes.brand, shoes.color, shoes.size, shoes.price, shoes.total" +
                    " FROM shoes");

            int itemNumber = 2;
            while (rs.next()) {
                Shoes temp = new Shoes();
                temp.setId(rs.getInt("shoes.id"));
                temp.setBrand(rs.getString("shoes.brand"));
                temp.setColor(rs.getString("shoes.color"));
                temp.setSize(rs.getInt("shoes.size"));
                temp.setPrice(rs.getInt("shoes.price"));
                temp.setTotal(rs.getInt("shoes.total"));

                if (temp.getTotal() > 0) { //om det finns skor i lager, lägg till artikelID och spara i lista
                    temp.setitemNumber(itemNumber);
                    tempShoes.add(temp);
                    itemNumber = itemNumber + 2;
                }
            }

            if (tempShoes.size() == 0) {
                System.out.println("Skolagret är tomt!");
            } else {
                System.out.println("Skor att beställa:");
                for (Shoes s : tempShoes) {
                    System.out.println(s.getitemNumber() + ". " + s.getBrand() + ", " + s.getColor() +
                            ", strl " + s.getSize() + " (" + s.getTotal() + " par)");
                }

                System.out.println("Välj artikelnummer:");
                userInput = "";
                userInput = scanner.nextLine();

                //kolla upp skons id i databasen
                for (Shoes s : tempShoes) {
                    if (s.getitemNumber() == Integer.parseInt(userInput)) {
                        shoeIdInDatabase = s.getId();
                    }
                }

                //kolla om order finns
                String query =
                        "SELECT shoeorder.id" +
                                " FROM shoeorder" +
                                " WHERE customerId = ?";

                PreparedStatement pstmt = con.prepareStatement(query);
                pstmt.setString(1, String.valueOf(userId));
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    orderIdInDatabase = rs.getInt("id");
                } else {
                    orderIdInDatabase = 0;//ingen order finns
                }

                //System.out.println("userId=" + userId +
                //      "  orderIdInDatabase=" + orderIdInDatabase + "  shoeIdInDatabase=" + shoeIdInDatabase);

                //lägg in order
                CallableStatement stm = con.prepareCall("CALL AddToCart(?,?,?)");
                stm.setInt(1, userId);
                stm.setInt(2, orderIdInDatabase);
                stm.setInt(3, shoeIdInDatabase);
                stm.execute();

                rs = stmt.executeQuery("SELECT COUNT(*) AS rowcount FROM orderrow");
                rs.next();
                rowsAfterOrder = rs.getInt("rowcount");
                //System.out.println("rowsAfterOrder: " + rowsAfterOrder);
                if (rowsAfterOrder > rowsBeforeOrder) {
                    System.out.println("Ordern har nu blivit tillagd!");
                } else {
                    System.out.println("Något gick fel, ordern har inte blivit tillagd!");
                }
            }
        } catch (SQLException e) {
            System.out.println("Något gick fel, ordern har inte blivit tillagd!");
            //e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Något gick fel, ordern har inte blivit tillagd!");
        }
    }


    public static void printOrder() {
        ResultSet rs;
        String query =
                "SELECT customer.fullname, shoes.brand, shoes.color, shoes.size" +
                        " FROM customer" +
                        " JOIN shoeorder ON customer.id = shoeorder.customerid" +
                        " JOIN orderrow ON shoeorder.id = orderrow.shoeorderid" +
                        " JOIN shoes ON orderrow.shoesid = shoes.id" +
                        " WHERE fullname = ?";

        try (Connection con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/webbshop_uppg2?serverTimezone=UTC&useSSL=false", "nina", "nina");
             PreparedStatement stmt = con.prepareStatement(query)) {

            stmt.setString(1, userNameFromInput);
            rs = stmt.executeQuery();

            if (!rs.isBeforeFirst()) {
                System.out.println("Det finns inga beställningar för " + userNameFromInput);
            } else {
                System.out.println(userNameFromInput + "s beställning:");
                while (rs.next()) {
                    System.out.println(rs.getString("shoes.brand") + ", " + rs.getString("shoes.color")
                            + ", strl: " + rs.getString("shoes.size"));
                }
                System.out.println("");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}