package processor.generator;

import soot.*;
import soot.jimple.InstanceFieldRef;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.SpecialInvokeExpr;
import soot.util.Chain;
import util.ClassWriter;

import java.util.ArrayList;
import java.util.LinkedList;

public class TableClassGenerator extends ClassGenerator {


    private static final ArrayList<SootClass> tableClasses = new ArrayList<>();


    public void generateClass(String tableName) {

        String className = tableName.substring(0, 1).toUpperCase() + (tableName + "Table").substring(1);
        String rowClassName = tableName.substring(0, 1).toUpperCase() + (tableName + "Row").substring(1);
        SootClass tableClass = new SootClass(className);
        tableClasses.add(tableClass);
        tableClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(tableClass);
        SootField listField = new SootField("rows", RefType.v("java.util.LinkedList<" + rowClassName + ">"), Modifier.PUBLIC);
        tableClass.addField(listField);
        SootMethod init = new SootMethod(SootMethod.constructorName, null, VoidType.v(), Modifier.PUBLIC);
        tableClass.addMethod(init);
        JimpleBody initBody = Jimple.v().newBody(init);
        init.setActiveBody(initBody);
        Chain units = initBody.getUnits();
        Local listNew;
        listNew = soot.jimple.Jimple.v().newLocal("listNew", Scene.v().getRefType("java.util.LinkedList"));
        initBody.getLocals().add(listNew);
        Local ref;
        ref = soot.jimple.Jimple.v().newLocal("this", tableClass.getType());
        initBody.getLocals().add(ref);
        SpecialInvokeExpr refInv = Jimple.v().newSpecialInvokeExpr(ref, Scene.v().getSootClass("java.lang.Object").getMethod("<init>", new LinkedList<Type>()).makeRef());
        units.add(Jimple.v().newIdentityStmt(ref, Jimple.v().newThisRef(tableClass.getType())));
        units.add(Jimple.v().newInvokeStmt(refInv));
        units.add(Jimple.v().newAssignStmt(listNew, Jimple.v().newNewExpr(Scene.v().getRefType("java.util.LinkedList"))));
        SpecialInvokeExpr listInv = Jimple.v().newSpecialInvokeExpr(listNew, Scene.v().getSootClass("java.util.LinkedList").getMethod("<init>", new LinkedList<Type>()).makeRef());
        units.add(Jimple.v().newInvokeStmt(listInv));
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(ref, tableClass.getFields().getFirst().makeRef());
        units.add(Jimple.v().newAssignStmt(instanceFieldRef, listNew));
        units.add(Jimple.v().newReturnVoidStmt());

        //create class content
        ClassWriter.writeAsClassFile(tableClass);
        ClassWriter.writeAsJimpleFile(tableClass);

    }

    public static ArrayList<SootClass> getTableClasses() {
        return tableClasses;
    }
}
