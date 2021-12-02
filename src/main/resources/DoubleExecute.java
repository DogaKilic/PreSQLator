import java.sql.*;

public class DoubleExecute {

    private static int secretId = 123456;

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer)");

            PreparedStatement insert = connection.prepareStatement("insert into person values(?)");
            insert.setInt(1, secretId);
            insert.executeUpdate();
            insert.executeUpdate();


            PreparedStatement select = connection.prepareStatement("select * from person");
            ResultSet rs = select.executeQuery();

            int cnt = 0;
            while (rs.next()) {
                if (rs.getInt("id") == secretId) {
                    cnt++;
                }
            }
            System.out.println(cnt);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}