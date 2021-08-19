import java.sql.*;
import java.util.LinkedList;

public class Scratch {
    public static int secretId = 1;
    public static String secretName = "Hans";
    //public static HelloWorld helloWorld = new HelloWorld();

    public static void main(String[] args) {
        try {
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string)");
            statement.executeUpdate("create table dog (petid integer, age integer , name string)");
            PreparedStatement dogInsert = connection.prepareStatement("insert into dog values(?, ?, ?)");
            PreparedStatement dogSelect = connection.prepareStatement("select age, name from dog");
			PreparedStatement dogSelect2 = connection.prepareStatement("select * from dog");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement select = connection.prepareStatement("select * from person");
            // do some work to obtain the secret
            // ...
            insert.setInt(1, 5);
            insert.setString(2, secretName);
            insert.executeUpdate();
            // ...
            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                System.out.println("name = " + rs.getString("name"));
            }
        } catch (SQLException e) {
        }
    }
}