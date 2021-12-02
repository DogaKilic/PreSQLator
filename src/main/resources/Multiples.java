import java.sql.*;
import java.util.LinkedList;


public class Multiples {

    private static String secretName = "Hans";


    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string)");

            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement insert2 = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement insert3 = connection.prepareStatement("insert into person values(?, ?)");


            insert.setInt(1, 1);
            insert.setString(2, secretName);
            insert.executeUpdate();

            insert2.setInt(1, 2);
            insert2.setString(2, secretName);
            insert2.executeUpdate();

            insert3.setInt(1, 3);
            insert3.setString(2, secretName);
            insert3.executeUpdate();

            PreparedStatement update = connection.prepareStatement("update person set name = 'Kevin' where id=1");
            update.executeUpdate();

            PreparedStatement update2 = connection.prepareStatement("update person set name = 'Alex' where id=2");
            update2.executeUpdate();

            PreparedStatement update3 = connection.prepareStatement("update person set name = 'Niko' where id=3");
            update3.executeUpdate();

            PreparedStatement select = connection.prepareStatement("select * from person");
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                System.out.println("name = " + rs.getString("name"));
            }
        } catch (SQLException e) {
        }
    }
}