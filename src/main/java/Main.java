import processor.JDBCProcessor;
import javax.tools.*;


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
        String processPath = "src/main/resources";
        String className = "Manager";

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, processPath + "\\" + className + ".java");
        jdbcProcessor.processClass(processPath, className, path);
    }
}