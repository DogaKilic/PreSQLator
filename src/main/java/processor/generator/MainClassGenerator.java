package processor.generator;

import processor.statement.IStatement;
import processor.statement.InsertStatement;
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

            ArrayList<InsertStatement> insertStatements = new ArrayList<>();
            ArrayList<String[]> selectStatements = new ArrayList<>();
            String connectionLocal = "";
            Unit initPred = null;
            Unit connPred = null;
            ArrayList<Unit> toRemove = new ArrayList<>();
            method.setDeclared(false);
            Body activeBody = method.retrieveActiveBody();
            UnitPatchingChain units = activeBody.getUnits();
            Iterator<Unit> unitIterator =units.iterator();
            boolean init = false;
            boolean clinit = false;

             while (unitIterator.hasNext()) {
                 Unit unit = unitIterator.next();
                 String methodContent = unit.toString();
                System.out.println(unit.toString());

                if (method.getName().equals("<init>")) {
                    if (unit.equals(activeBody.getThisUnit())) {
                        toRemove.add(unit);
                    }
                    if (!init) {
                        activeBody.getThisLocal().setType(processedClass.getType());
                        initPred = unit;
                        init = true;
                    }

                }

                else if (methodContent.contains("getConnection")){
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
                    String[] nameTypeAndTable = new String[4];
                    String[] queryData = unitData[1].split(" ");

                    if (unitData[0].split(" ")[0].equals("interfaceinvoke")){
                        continue;
                    }
                    else {
                        nameTypeAndTable[0] = unitData[0].split(" ")[0];
                    }

                    if((queryData[0]).equals("insert")){
                        nameTypeAndTable[1] = "insert";
                        nameTypeAndTable[2] = queryData[2];
                        int count = (unitData[1].split("\\?").length - 1);
                        InsertStatement newStatement = new InsertStatement(nameTypeAndTable[0], nameTypeAndTable[2], count);
                        insertStatements.add(newStatement);
                    }

                    else if((queryData[0]).equals("select")){
                        int tableIndex = 0;
                        String result = "";
                        for(int i = 1; i < queryData.length; i++) {
                            if (queryData[i].endsWith(",")) {
                                result += queryData[i].split(",")[0];
                            }
                            else {
                                result += queryData[i];
                                tableIndex = i + 2;
                                break;
                            }
                        }
                        nameTypeAndTable[1] = result;
                        nameTypeAndTable[2] = queryData[tableIndex];
                        nameTypeAndTable[3] = "";
                        selectStatements.add(nameTypeAndTable);
                    }
                }

                else if (insertStatements.stream().anyMatch(x -> methodContent.contains(x.getLocalName()))) {

                }
                else if (selectStatements.stream().anyMatch(x -> methodContent.contains(x[0]))) {

                }

            }


             if (init) {
                 processInıt(initPred, units, activeBody, processedClass);
             }

             if (connPred != null) {
                 processConnection(connPred, units, activeBody);
             }
            units.removeAll(toRemove);
             toRemove.clear();
            processedClass.addMethod(method);
            activeBody.validate();
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

    private void processLocalField(Unit pred, UnitPatchingChain units, Body activeBody) {

    }

    private void processInıt(Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        //Local ref = soot.jimple.Jimple.v().newLocal("this", processedClass.getType());
        //activeBody.getLocals().add(ref);
        //SpecialInvokeExpr refInv = Jimple.v().newSpecialInvokeExpr(ref, Scene.v().getSootClass("java.lang.Object").getMethod("<init>", new LinkedList<Type>()).makeRef());
        newUnits.add(Jimple.v().newIdentityStmt(activeBody.getThisLocal(), Jimple.v().newThisRef(processedClass.getType())));
        //newUnits.add(Jimple.v().newInvokeStmt(refInv));

        units.insertAfter(newUnits, pred);

    }

}
