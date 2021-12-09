import java.sql.*;
import java.util.LinkedList;


public class DoubleSet {

    private static int secretId = 123456;


    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer)");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?)");
            PreparedStatement select = connection.prepareStatement("select * from person");

            insert.setInt(1, secretId);
            insert.setInt(1, 5555555);
            insert.executeUpdate();

            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                System.out.println("name = " + rs.getString("name") + ", id = " + rs.getInt("id"));
            }
        } catch (SQLException e) {
        }
    }
}