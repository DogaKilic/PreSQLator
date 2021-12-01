import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Logger;

public class Scratch2 {

    private static final String PORT = "5432";
    private static final String HOST = "localhost";
    private static final String DB_NAME = "privacycrashcam";
    private static final String USER = "postgres";
    private static final String PASSWORD = "pccdata";

    private Connection c = null;


    private boolean connectDatabase() {
        c = null;
        try {
            c = DriverManager
                    .getConnection("jdbc:postgresql://" + HOST + ":" + PORT + "/" + DB_NAME, USER, PASSWORD);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.getGlobal().severe("No connection to database!");
            return false;
        }
        return true;
    }


    public boolean saveProcessedVideoAndMeta(String videoName, String metaName) {
        if (!connectDatabase()) {return false;}
        // send sql command and catch possible exeptions
        try {
            // sql command
            PreparedStatement insert1 = c.prepareStatement("insert into video values (?, ?, ?)");
            insert1.setInt(1, 534);
            insert1.setString(2, videoName);
            insert1.setString(3, metaName);
            insert1.executeUpdate();
        } catch (NullPointerException | SQLException e) {
            Logger.getGlobal().warning("Inserting video and meta in database failed!");
        }
        return true;
    }

    public String getVideoInfo(int videoId) {
        String vI = null;
        if (!connectDatabase()) {return null;}
        // execute sql command and insert result in ArrayList
        try {
            PreparedStatement select = c.prepareStatement("select video_name, id from video");
            ResultSet rs = select.executeQuery();
            // insert result in ArrayList
            while (rs.next()) {
                String video_name = rs.getString("video_name");
                int id = Integer.parseInt(rs.getString("id"));
                vI = video_name + ":" + id;
            }
        } catch (NullPointerException | SQLException e) {
            Logger.getGlobal().warning("Select SQL command has not been executed successfully: ");
        }
        return vI;
    }
}