import java.sql.*;
import java.util.LinkedList;


public class MultipleColumns {

    private static int secretId = 123456;


    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string, age integer)");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?, ?)");
            PreparedStatement insert2 = connection.prepareStatement("insert into person values(?, ?, ?)");
            PreparedStatement select = connection.prepareStatement("select id,age from person");

            insert.setInt(1, secretId);
            insert.setString(2, "Hans");
            insert.setInt(3, 25);
            insert.executeUpdate();

            insert2.setInt(1, 654321);
            insert2.setString(2, "Joe");
            insert2.setInt(3, 45);
            insert2.executeUpdate();

            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                System.out.println("name = " + rs.getInt("id" + ", age = " + rs.getInt("age")));
            }
        } catch (SQLException e) {
        }
    }
}