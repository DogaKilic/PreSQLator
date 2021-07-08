package util;

import soot.Printer;
import soot.SootClass;
import soot.SourceLocator;
import soot.baf.BafASMBackend;
import soot.options.Options;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

public class ClassWriter {

    public static void writeAsClassFile(SootClass classToWrite) {
        try {
            int java_version = Options.v().java_version();
            String fileName = SourceLocator.v().getFileNameFor(classToWrite, Options.output_format_class);
            OutputStream streamOut = new FileOutputStream(fileName);
            BafASMBackend backend = new BafASMBackend(classToWrite, java_version);
            backend.generateClassFile(streamOut);
            streamOut.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }


    public static void writeAsJimpleFile(SootClass classToWrite) {
        try {
            String fileName2 = SourceLocator.v().getFileNameFor(classToWrite, Options.output_format_jimple);
            OutputStream streamOut2 = new FileOutputStream(fileName2);
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut2));
            Printer.v().printTo(classToWrite, writerOut);
            writerOut.flush();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}