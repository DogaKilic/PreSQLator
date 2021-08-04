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
            ArrayList<Unit> toRemove = new ArrayList<>();
            ArrayList<Unit[]> toAddInsert = new ArrayList<>();
            ArrayList<Unit[]> toAddSelect = new ArrayList<>();
            method.setDeclared(false);
            Body activeBody = method.retrieveActiveBody();
            UnitPatchingChain units = activeBody.getUnits();
            Iterator<Unit> unitIterator =units.iterator();
             while (unitIterator.hasNext()) {
                 Unit unit = unitIterator.next();
                 String methodContent = unit.toString();
                System.out.println(unit.toString());
                if (methodContent.contains("getConnection")){
                    connPred = unit;
                    toRemove.add(unit);
                    connectionLocal = unit.toString().split(" ")[0];
                }
                else if (methodContent.contains("create table") || methodContent.contains("createStatement")) {
                    toRemove.add(unit);
                }
                else if (methodContent.contains("prepareStatement")) {
                    toRemove.add(unit);
                    String[] unitData = unit.toString().split("\"");
                    String[] queryData = unitData[1].split(" ");
                    String[] nameTypeAndTable = new String[3];
                    nameTypeAndTable[0] = unitData[0].split(" ")[0];
                    if((queryData[0]).equals("insert")){
                        nameTypeAndTable[2] = queryData[2];
                    }
                    else if((queryData[0]).equals("select")){
                        nameTypeAndTable[2] = queryData[3];
                    }

                }

            }
             if (connPred != null) {
                 processConnection(connPred, units, activeBody);
             }
            //units.removeAll(toRemove);
             toRemove.clear();
             activeBody.validate();
            processedClass.addMethod(method);
            method.setDeclared(true);
        }


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
