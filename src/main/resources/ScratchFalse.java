import java.sql.*;
import java.util.LinkedList;


public class ScratchFalse {

    private static int secretId = 123456;
    private static String secretName = "Hans";


    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer, name string)");
            statement.executeUpdate("create table video (id integer, name string)");
            PreparedStatement insert = connection.prepareStatement("insert into person values(?, ?)");
            PreparedStatement personSelect = connection.prepareStatement("select * from person");
            PreparedStatement videoSelect = connection.prepareStatement("select * from video");

            insert.setInt(1, secretId);
            insert.setString(2, secretName);
            insert.executeUpdate();

            ResultSet rsVideo = videoSelect.executeQuery();
            while (rsVideo.next()) {
                System.out.println("name = " + rsVideo.getString("name") + ", id = " + rsVideo.getInt("id"));
            }
        } catch (SQLException e) {
        }
    }
}