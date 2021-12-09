import java.sql.*;
import java.util.LinkedList;


public class WrongColumn {

    private static int secretId = 123456;


    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string)");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement select = connection.prepareStatement("select name from person");

            insert.setInt(1, secretId);
            insert.setString(2, "Hans");
            insert.executeUpdate();

            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                System.out.println("name = " + rs.getString("name"));
            }
        } catch (SQLException e) {
        }
    }
}