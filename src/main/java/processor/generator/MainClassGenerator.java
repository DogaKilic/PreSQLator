package processor.generator;

import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.SpecialInvokeExpr;
import soot.util.Chain;
import util.ClassWriter;

import java.util.*;
import java.util.stream.Stream;

public class MainClassGenerator extends ClassGenerator {
    public void generateClass(SootClass oldClass) {
        SootClass processedClass = new SootClass(oldClass.getName() + "Processed", Modifier.PUBLIC);
        processedClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(processedClass);

        List<SootMethod> methodList = oldClass.getMethods();
        System.out.println(oldClass.getFields().toString());
        for(SootField field : oldClass.getFields()){
            field.setDeclared(false);
            processedClass.addField(field);
            field.setDeclared(true);
        }

        for(SootMethod method : methodList) {
            String connectionLocal = "";
            Unit connPred = null;
            ArrayList<Unit> toRemoveConnection = new ArrayList<>();
            method.setDeclared(false);
            Body activeBody = method.retrieveActiveBody();
            UnitPatchingChain units = activeBody.getUnits();
            Iterator<Unit> unitIterator =units.iterator();
             while (unitIterator.hasNext()) {
                 Unit unit = unitIterator.next();
                System.out.println(unit.toString());
                if (unit.toString().contains("getConnection")){
                    connPred = unit;
                    toRemoveConnection.add(unit);
                    connectionLocal = unit.toString().split(" ")[0];

                }

            }
             if (connPred != null) {
                 processConnection(connPred, units, activeBody);
             }
            //units.removeAll(toRemoveConnection);
             toRemoveConnection.clear();
            processedClass.addMethod(method);
            method.setDeclared(true);
        }

        /************************************************
        //fill final class
        SootMethod main = new SootMethod("main",
                Arrays.asList(new Type[]{ArrayType.v(RefType.v("java.lang.String"), 1)}),
                VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        processedClass.addMethod(main);
        JimpleBody body = Jimple.v().newBody(main);
        main.setActiveBody(body);
        Chain units = body.getUnits();
        units.add(Jimple.v().newReturnVoidStmt());
        ****************************************************/

        ClassWriter.writeAsClassFile(processedClass);
        ClassWriter.writeAsJimpleFile(processedClass);

    }

    private void  processConnection(Unit pred, UnitPatchingChain units, Body activeBody) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        Local newConn = Jimple.v().newLocal("connection", Scene.v().getRefType("Connection"));
        activeBody.getLocals().add(newConn);
        newUnits.add(Jimple.v().newAssignStmt(newConn, Jimple.v().newNewExpr(Scene.v().getRefType("Connection"))));
        SpecialInvokeExpr listInv = Jimple.v().newSpecialInvokeExpr(newConn, Scene.v().getSootClass("Connection").getMethod("<init>", new LinkedList<Type>()).makeRef());
        newUnits.add(Jimple.v().newInvokeStmt(listInv));

        units.insertAfter(newUnits, pred);
    }
}
