import java.sql.*;
import java.util.LinkedList;
import java.util.Random;

public class Scratch3 {
    public static String secretName = "Hans";
    //public static HelloWorld helloWorld = new HelloWorld();

    public static void main(String[] args) {
        try {
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string)");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement select = connection.prepareStatement("select * from person");
            PreparedStatement alienInsert = connection.prepareStatement("insert into alien values(?, ?)");
            PreparedStatement alienSelect = connection.prepareStatement("select alienId from alien");
            ResultSet alienRs = alienSelect.executeQuery();
            int random = (int) Math.random();
            switch (random % 3) {
                case 0:
                    insert.setInt(1, random);
                    break;
                case 1:
                    insert.setInt(1, random  -1);
                    break;
                case 2:
                    insert.setInt(1, random - 2);
                    break;
            }
            String alienCode = "";

            while (alienRs.next()) {
                alienCode += alienRs.getString("ufoPlate") + alienRs.getInt("alienID") + alienRs.getInt("age");
            }
            insert.setString(2, secretName);
            insert.executeUpdate();
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                System.out.println("name = " + rs.getString("name"));
                System.out.println(alienCode);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}