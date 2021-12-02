package util;

import soot.Printer;
import soot.SootClass;
import soot.SourceLocator;
import soot.baf.BafASMBackend;
import soot.options.Options;

import java.io.*;

public class ClassWriter {

    public static String dir;

    public static void setDir(String newDir) {
        dir = newDir;
    }

    public static void writeAsClassFile(SootClass classToWrite) {
        try {
            int java_version = Options.v().java_version();
            File folderFile = new File(dir);
            folderFile.mkdirs();
            String fileName = dir + classToWrite.getName() + ".class";
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
            File folderFile = new File(dir);
            folderFile.mkdirs();
            String fileName = dir + classToWrite.getName() + ".jimple";
            OutputStream streamOut2 = new FileOutputStream(fileName);
            PrintWriter writerOut = new PrintWriter(new OutputStreamWriter(streamOut2));
            Printer.v().printTo(classToWrite, writerOut);
            writerOut.flush();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

}