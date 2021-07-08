package processor.generator;

import content.TableBank;
import soot.*;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.util.Chain;
import util.ClassWriter;

import java.util.ArrayList;
import java.util.LinkedList;

public class RowClassGenerator extends ClassGenerator {


    private static final LinkedList<SootClass> rowClasses = new LinkedList<>();

    public void generateClass(String tableName) {
        String className = tableName.substring(0, 1).toUpperCase() + (tableName + "Row").substring(1);
        SootClass rowClass = new SootClass(className);
        rowClasses.add(rowClass);
        rowClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(rowClass);
        ArrayList<String[]> columnContent = TableBank.getColumnContent(tableName);
        ArrayList<Type> types = new ArrayList<>();
        for (String[] data : columnContent) {
            Type type = getType(data[1]);
            types.add(type);
            SootField newField = new SootField(data[0], type, Modifier.PUBLIC);
            rowClass.addField(newField);
        }

        SootMethod constructor = new SootMethod(SootMethod.constructorName, types, VoidType.v(), Modifier.PUBLIC | Modifier.CONSTRUCTOR);
        rowClass.addMethod(constructor);
        JimpleBody mainBody = Jimple.v().newBody(constructor);
        constructor.setActiveBody(mainBody);
        Chain units = mainBody.getUnits();
        ArrayList<Local> parameterList = new ArrayList<>();
        int parmCount = 0;
        Local ref;
        ref = soot.jimple.Jimple.v().newLocal("this", rowClass.getType());  // create "this" like you create other locals
        mainBody.getLocals().add(ref);
        units.add(Jimple.v().newIdentityStmt(ref,
                Jimple.v().newThisRef(rowClass.getType())));
        for (String[] data : columnContent) { // identity statements should come first
            Local parm;
            parm = Jimple.v().newLocal("parm" + parmCount, getType(data[1]));
            mainBody.getLocals().add(parm);
            units.add(Jimple.v().newIdentityStmt(parm,
                    Jimple.v().newParameterRef(getType(data[1]), parmCount)));
            parmCount++;
            parameterList.add(parm);
        }
        parmCount = 0;
        for (String[] data : columnContent) {
            Local parm;
            parm = parameterList.get(parmCount);
            InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(ref, rowClass.getFieldByName(data[0]).makeRef());
            units.add(Jimple.v().newAssignStmt(instanceFieldRef, parm));
            parmCount++;
        }

        units.add(Jimple.v().newReturnVoidStmt());
        mainBody.validate();

        ClassWriter.writeAsClassFile(rowClass);
        ClassWriter.writeAsJimpleFile(rowClass);
    }

    public static LinkedList<SootClass> getRowClasses() {
        return rowClasses;
    }
}
