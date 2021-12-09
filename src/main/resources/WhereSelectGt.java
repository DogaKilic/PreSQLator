import java.sql.*;

public class WhereSelectGt {

    private static int secretId = 123456;

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer)");

            PreparedStatement insert = connection.prepareStatement("insert into person values(?)");
            insert.setInt(1, secretId);
            insert.executeUpdate();

            PreparedStatement insert2 = connection.prepareStatement("insert into person values(?)");
            insert2.setInt(1, 1234567);
            insert2.executeUpdate();

            PreparedStatement select = connection.prepareStatement("select * from person where id > 123456");
            ResultSet rs = select.executeQuery();

            while (rs.next()) {
                System.out.println(rs.getInt("id"));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}