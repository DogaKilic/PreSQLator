import processor.JDBCProcessor;


public class Main {

    public static void main(String[] args) {
        JDBCProcessor jdbcProcessor = new JDBCProcessor();
        //absolute path, change as you need
        jdbcProcessor.processClass("C:\\Users\\dogas\\IdeaProjects\\TestProj\\src\\main\\resources", "Scratch");
    }
}