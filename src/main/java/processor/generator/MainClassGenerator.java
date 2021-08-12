package processor.generator;

import processor.statement.InsertStatement;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import util.ClassWriter;
import java.util.*;

public class MainClassGenerator extends ClassGenerator {
    private int localCnt = 0;
    private Local ref;

    public void generateClass(SootClass oldClass) {
        Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);
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
            ArrayList<String[]> staticToBeReplaced = new ArrayList<>();
            Unit initPred = null;
            Unit connPred = null;
            Unit clinitPred = null;
            ArrayList<Unit> toRemove = new ArrayList<>();
            method.setDeclared(false);
            Body activeBody = method.retrieveActiveBody();
            UnitPatchingChain units = activeBody.getUnits();
            Iterator<Unit> unitIterator =units.iterator();
            boolean init = false;
            boolean clinit = false;
            setRef(processedClass, activeBody);



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

                else if (method.getName().equals("<clinit>")) {
                    Optional<ValueBox> valueBox;
                    valueBox = unit.getDefBoxes().stream().findFirst();
                    if(valueBox.isPresent()) {
                        if(valueBox.get().getValue().toString().contains("<" + oldClass.getName() + ":")) {
                            String[] data = unit.toString().split(">");
                            String[] fieldAndAssignment = new String[2];
                            fieldAndAssignment[0] = data[0].split(" ")[2];
                            fieldAndAssignment[1] = data[1].split("= ")[1];
                            staticToBeReplaced.add(fieldAndAssignment);
                            toRemove.add(unit);
                            clinitPred = unit;
                        }
                    }

                }

                else if (methodContent.contains("getConnection")){
                    connPred = unit;
                    toRemove.add(unit);
                    //connectionLocal = unit.toString().split(" ")[0];
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

             if (clinitPred != null) {
                 for(int i = 0; i < staticToBeReplaced.size(); i++) {
                     String[] current = staticToBeReplaced.get(i);
                     processClinit(clinitPred, units, activeBody, processedClass, current[0], current[1]);
                 }
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
        newUnits.add(Jimple.v().newIdentityStmt(activeBody.getThisLocal(), Jimple.v().newThisRef(processedClass.getType())));

        units.insertAfter(newUnits, pred);
    }

    private  void processClinit(Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass, String field, String assignment){
        ArrayList<Unit> newUnits = new ArrayList<>();
        Type type = pred.getDefBoxes().stream().findFirst().get().getValue().getType();
        StaticFieldRef instanceFieldRef = Jimple.v().newStaticFieldRef((processedClass.getFieldByName(field).makeRef()));
        switch (type.toString()) {
            case "int":
                newUnits.add(Jimple.v().newAssignStmt(instanceFieldRef, IntConstant.v(Integer.valueOf(assignment))));
            case "java.lang.String":
                newUnits.add(Jimple.v().newAssignStmt(instanceFieldRef, StringConstant.v(assignment.replaceAll("\"", ""))));
        }
        localCnt++;
        units.insertAfter(newUnits, pred);
    }

    private void setRef(SootClass processedClass,Body activeBody) {
        ref = soot.jimple.Jimple.v().newLocal("this", processedClass.getType());
        activeBody.getLocals().add(ref);
    }


}
