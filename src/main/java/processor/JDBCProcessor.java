package processor;

import antlr.*;
import content.TableBank;
import content.TableContent;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import processor.generator.ConnectionClassGenerator;
import processor.generator.RowClassGenerator;
import processor.generator.TableClassGenerator;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.util.Chain;
import util.ClassWriter;
import util.PredicateGenerator;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static soot.SootClass.SIGNATURES;

public class JDBCProcessor implements IProcessor {
    private TableClassGenerator tableGen = new TableClassGenerator();
    private RowClassGenerator rowGen = new RowClassGenerator();
    private ConnectionClassGenerator connectGen = new ConnectionClassGenerator();

    public void processClass(String processPath, String className) {
        //soot setup
        Options.v().set_prepend_classpath(true);
        Options.v().set_process_dir(Arrays.asList(processPath.split(File.pathSeparator)));
        String javaPath = System.getProperty("java.class.path");
        String sootClassPath = javaPath;
        Options.v().set_soot_classpath(sootClassPath);
        Scene.v().loadClassAndSupport("java.lang.Object");
        Scene.v().addBasicClass("java.util.LinkedList", SIGNATURES);
        Scene.v().addBasicClass("java.util.ArrayList", SIGNATURES);
        Scene.v().addBasicClass("java.util.Iterator", SIGNATURES);
        Scene.v().addBasicClass("java.util.List", SIGNATURES);
        //load class, get method list
        SootClass appClass = Scene.v().loadClassAndSupport(className);
        List<SootMethod> methods = appClass.getMethods();
        Scene.v().loadNecessaryClasses();
        //set up predicates to filter text
        Predicate<String> basicPredicate = PredicateGenerator.generateBasicPredicate();
        //filter text to get information on sqlite statements
        System.out.println(methods.get(1).retrieveActiveBody().getUnits().toString() + "\n");
        Stream<String> statementStream = methods.get(1).retrieveActiveBody().getUnits().stream().map(i -> i.toString()).filter(basicPredicate).map(i -> i.split("\"")[1]);
        String finalStatementString = "";
        Iterator<String> statementIterator = statementStream.iterator();
        while (statementIterator.hasNext()) {
            finalStatementString = finalStatementString + statementIterator.next() + "\n";
        }

        System.out.println(finalStatementString);

        //configure antlr and start walking
        SQLiteLexer sqLiteLexer = new SQLiteLexer(CharStreams.fromString(finalStatementString));
        CommonTokenStream tokens = new CommonTokenStream(sqLiteLexer);
        SQLiteParser parser = new SQLiteParser(tokens);
        ParseTree tree = parser.parse();
        SQLiteSootListener sqLiteSootListener = new SQLiteSootListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(sqLiteSootListener, tree);

        for (TableContent i : TableBank.getTables()) {
            i.testPrint();
        }

        //add required imports to Scene


        //Create new class
        SootClass processedClass = new SootClass(className + "Processed", Modifier.PUBLIC);
        processedClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(processedClass);

        //Create classes for tables and rows and prepare them
        for (TableContent i : TableBank.getTables()) {
            rowGen.generateClass(i.getTableName());
            tableGen.generateClass(i.getTableName());
        }
        connectGen.generateClass(TableBank.getTables());

        //fill final class
        SootMethod main = new SootMethod("main",
                Arrays.asList(new Type[]{ArrayType.v(RefType.v("java.lang.String"), 1)}),
                VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        processedClass.addMethod(main);
        JimpleBody body = Jimple.v().newBody(main);
        main.setActiveBody(body);
        Chain units = body.getUnits();
        units.add(Jimple.v().newReturnVoidStmt());

        //save the class as a .class file
        ClassWriter.writeAsClassFile(processedClass);
        ClassWriter.writeAsJimpleFile(processedClass);
        ClassWriter.writeAsClassFile(appClass);
        ClassWriter.writeAsJimpleFile(appClass);
    }
}
