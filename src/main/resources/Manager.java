import java.sql.*;
import java.util.logging.Logger;

public class Manager {
    private static final String PORT = "5432";
    private static final String HOST = "localhost";
    private static int secretId = 5564;
    private Connection c;

    public Manager() {
        try {
            c = DriverManager.getConnection("jdbc:postgresql://" + HOST + ":" + PORT + "/" + "privacycrashcam", "postgres", "pccdata");
        }
        catch (Exception e) {
            Logger.getGlobal().warning("Connection to database failed!");
        }
    }

    public boolean write (String output) {
        System.out.println(output);
        return true;
    }


    public boolean saveProcessedVideoAndMeta(int videoId, String videoName, String metaName) {
        try {
            PreparedStatement insert1 = c.prepareStatement("insert into video values (?, ?, ?)");
            insert1.setInt(1, videoId);
            insert1.setString(2, videoName);
            insert1.setString(3, metaName);
            insert1.executeUpdate();
        } catch (NullPointerException | SQLException e) {
            //Logger.getGlobal().warning("Inserting video and meta in database failed!");
            return true;
        }
        return true;
    }

    public ResultSet getVideoInfo() {
        ResultSet rs;
        try {
            PreparedStatement select = c.prepareStatement("select video_name, id from video");
            rs = select.executeQuery();
            return rs;
        } catch (NullPointerException | SQLException e) {
            Logger.getGlobal().warning("Select SQL command has not been executed successfully: ");
        }
        return null;
    }

    public static void main(String[] args) {
        Manager test = new Manager();
        test.saveProcessedVideoAndMeta(secretId, args[0], args[1]);
        ResultSet result = test.getVideoInfo();
        try {
            while (result.next()) {
                String output = "Video ID = " + result.getInt("video_id") + ", video name = " + result.getString("video_name");
                test.write(output);
            }
        }
        catch (Exception ex) {

        }
    }
}