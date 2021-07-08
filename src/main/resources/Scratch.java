import java.sql.*;
import java.util.LinkedList;

public class Scratch {
    public LinkedList<String> list = new LinkedList<>();
    public static int secretId = 1;
    public static String secretName = "Hans";

    public static void main(String[] args) {
        try {

            this.list.add(new PersonRow(12, "asd"));
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string)");
            statement.executeUpdate("create table dog (petid integer, age integer , name string)");
            PreparedStatement dogInsert = connection.prepareStatement("insert into dog values(?, ?, ?)");
            PreparedStatement dogSelect = connection.prepareStatement("select * from dog");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement select = connection.prepareStatement("select * from person");
            // do some work to obtain the secret
            // ...
            insert.setInt(1, secretId);
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