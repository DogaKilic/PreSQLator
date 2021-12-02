package processor;

import antlr.*;
import content.TableBank;
import content.TableContent;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import processor.generator.ConnectionClassGenerator;
import processor.generator.MainClassGenerator;
import processor.generator.RowClassGenerator;
import processor.generator.TableClassGenerator;
import soot.*;
import soot.options.Options;
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
    private MainClassGenerator mainGen = new MainClassGenerator();

    public void processClass(String processPath, String className, String arg) {
        //soot setup
        Options.v().set_prepend_classpath(true);
        Options.v().set_process_dir(Arrays.asList(processPath.split(File.pathSeparator)));
        Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);
        String javaPath = System.getProperty("java.class.path");
        String sootClassPath = javaPath;
        Options.v().set_soot_classpath(sootClassPath);
        Scene.v().loadClassAndSupport("java.lang.Object");
        Scene.v().addBasicClass("java.util.Collection", SIGNATURES);
        Scene.v().addBasicClass("java.util.stream.Stream", SIGNATURES);
        Scene.v().addBasicClass("java.util.LinkedList", SIGNATURES);
        Scene.v().addBasicClass("java.util.ArrayList", SIGNATURES);
        Scene.v().addBasicClass("java.util.Iterator", SIGNATURES);
        Scene.v().addBasicClass("java.util.List", SIGNATURES);
        Scene.v().addBasicClass("util.ConnectionHelper", SIGNATURES);
        Scene.v().addBasicClass("util.Row", SIGNATURES);
        Scene.v().addBasicClass("java.util.function.Predicate", SIGNATURES);
        //load class, get method list
        SootClass appClass = Scene.v().loadClassAndSupport(className);
        List<SootMethod> methods = appClass.getMethods();
        Scene.v().loadNecessaryClasses();
        ClassWriter.setDir("sootOutput/" + className + "/" );
        //set up predicates to filter text
        Predicate<String> basicPredicate = PredicateGenerator.generateBasicPredicate();
        //filter text to get information on sqlite statements
        String finalStatementString = "";
        for (int i = 0; i < methods.size(); i++) {
        Stream<String> statementStream = methods.get(i).retrieveActiveBody().getUnits().stream().map(j -> j.toString()).filter(basicPredicate).map(j -> j.split("\"")[1]);
        Iterator<String> statementIterator = statementStream.iterator();
        while (statementIterator.hasNext()) {
            finalStatementString = finalStatementString + statementIterator.next() + "\n";
        }
    }
        finalStatementString = finalStatementString.replaceAll("\\\\","");



        //configure antlr and start walking
        SQLiteLexer sqLiteLexer = new SQLiteLexer(CharStreams.fromString(finalStatementString));
        CommonTokenStream tokens = new CommonTokenStream(sqLiteLexer);
        SQLiteParser parser = new SQLiteParser(tokens);
        ParseTree tree = parser.parse();
        SQLiteSootListener sqLiteSootListener = new SQLiteSootListener();
        ParseTreeWalker walker = new ParseTreeWalker();

        if ( arg != "") {
            String sum = "";
            try {
                FileInputStream fstream = null;
                fstream = new FileInputStream(arg);
                String strLine;
                BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
                while ((strLine = br.readLine()) != null) {
                    sum += "\n" + strLine;
                }
                SQLiteLexer createLexer = new SQLiteLexer(CharStreams.fromString(sum));
                CommonTokenStream createTokens = new CommonTokenStream(createLexer);
                SQLiteParser createParser = new SQLiteParser(createTokens);
                ParseTree createTree = createParser.parse();
                walker.walk(sqLiteSootListener, createTree);
                fstream.close();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        walker.walk(sqLiteSootListener, tree);

        for (TableContent i : TableBank.getTables()) {
            i.testPrint();
        }




        //Create classes for tables and rows and prepare them
        for (TableContent i : TableBank.getTables()) {
            rowGen.generateClass(i.getTableName());
            tableGen.generateClass(i.getTableName());
        }

        connectGen.generateClass(TableBank.getTables());

        ClassWriter.writeAsClassFile(appClass);
        ClassWriter.writeAsJimpleFile(appClass);

        //Create new class
        mainGen.generateClass(appClass);

    }
}
