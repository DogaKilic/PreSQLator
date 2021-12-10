import java.sql.*;

public class ModificationFalse {

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
            Statement statement = connection.createStatement();
            statement.executeUpdate("create table person (id integer)");
            statement.executeUpdate("create table animal (id integer)");

            PreparedStatement insert = connection.prepareStatement("insert into person values(?)");
            insert.setInt(1, 111111);
            insert.executeUpdate();


            PreparedStatement insert2 = connection.prepareStatement("insert into person values(?)");
            insert2.setInt(1, 22222);
            insert2.executeUpdate();

            if (Integer.valueOf(args[0]) > 0) {
                PreparedStatement update = connection.prepareStatement("update person set id=333333 where id=111111");
                update.executeUpdate();
            }
            else {
                PreparedStatement update = connection.prepareStatement("update person set id=333333 where id=222222");
                update.executeUpdate();
            }


            PreparedStatement select = connection.prepareStatement("select * from animal");
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