import java.sql.*;
import java.util.LinkedList;
import java.util.Random;

public class ScratchSwitch {

    private static int secretId = 123456;
    private static String secretName = "Hans";

    public static void main(String[] args) {
        try {
            // create a database connection
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string)");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement select = connection.prepareStatement("select * from person");

            switch (secretId % 3) {
                case 0:
                    insert.setInt(1, secretId);
                    break;
                case 1:
                    insert.setInt(1, secretId - 1);
                    break;
                case 2:
                    insert.setInt(1, secretId - 2);
                    break;
            }

            insert.setString(2, secretName);
            insert.executeUpdate();

            ResultSet rs = select.executeQuery();
            while (rs.next()) {
                System.out.println("name = " + rs.getString("name") + ", id = " + rs.getInt("id"));
            }
        } catch (SQLException e) {
        }
    }
}