import edu.kit.joana.ui.annotations.Sink;
import processor.JDBCProcessor;
import javax.tools.*;
import java.io.File;


public class Main {
    public static void main(String[] args) {
        JDBCProcessor jdbcProcessor = new JDBCProcessor();
        String path;
        if(args.length == 0) {
            path = "";
        }
        else {
            path = args[0];
        }
        String processPath = "C:\\Users\\dogas\\IdeaProjects\\TestProj\\src\\main\\resources";
        String className = "Deletion";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        //compiler.run(null, null, null, processPath + "\\" + className + ".java");
        jdbcProcessor.processClass(processPath, className, path);
    }
}