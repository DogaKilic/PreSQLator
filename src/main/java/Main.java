import processor.JDBCProcessor;

import java.io.File;
import java.io.FileReader;
import java.util.Scanner;


public class Main {

    public static void main(String[] args) {
        JDBCProcessor jdbcProcessor = new JDBCProcessor();
        String path = args[0];
        File cf = new File(path);

        //absolute path, change as you need
        jdbcProcessor.processClass("C:\\Users\\dogas\\IdeaProjects\\TestProj\\src\\main\\resources", "Scratch2", args[0]);
    }
}