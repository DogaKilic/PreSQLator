import java.sql.*;
import java.util.LinkedList;


public class ScratchDirect {

    private static int secretId = 123456;
    private static String secretName = "Hans";


    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string)");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement select = connection.prepareStatement("select * from person");

            ResultSet rs = select.executeQuery();

            insert.setInt(1, secretId);
            insert.setString(2, secretName);
            insert.executeUpdate();

            while (rs.next()) {
                System.out.println("name = " + rs.getString("name") + ", id = " + rs.getInt("id"));
            }
        } catch (SQLException e) {
        }
    }
}