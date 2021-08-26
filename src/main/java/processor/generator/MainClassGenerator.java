package processor.generator;

import processor.statement.InsertStatement;
import processor.statement.RSStatement;
import processor.statement.SelectStatement;
import soot.*;
import soot.jimple.*;
import util.ClassWriter;
import java.util.*;

public class MainClassGenerator extends ClassGenerator {
    private Local ref;
    private Local connectionLocal;

    public void generateClass(SootClass oldClass) {
        SootClass processedClass = new SootClass(oldClass.getName() + "Processed", Modifier.PUBLIC);
        processedClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(processedClass);

        List<SootMethod> methodList = oldClass.getMethods();
        for(SootField field : oldClass.getFields()){
            field.setDeclared(false);
            processedClass.addField(field);
            field.setDeclared(true);
        }

        for(SootMethod method : methodList) {

            ArrayList<InsertStatement> insertStatements = new ArrayList<>();
            ArrayList<SelectStatement> selectStatements = new ArrayList<>();
            ArrayList<String[]> staticToBeReplaced = new ArrayList<>();
            ArrayList<String[]> mFieldToBeReplaced = new ArrayList<>();
            ArrayList<String[]> initFieldToBeReplaced = new ArrayList<>();
            ArrayList<InsertStatement> insertToBeReplaced = new ArrayList<>();
            ArrayList<SelectStatement> selectToBeReplaced = new ArrayList<>();
            ArrayList<RSStatement> rsStatements = new ArrayList<>();
            ArrayList<RSStatement> nextToBeReplaced = new ArrayList<>();
            ArrayList<RSStatement> getToBeReplaced = new ArrayList<>();
            Unit initPred = null;
            Unit connPred = null;
            Unit clinitPred = null;
            Unit mFieldPred = null;
            ArrayList<Unit> toRemove = new ArrayList<>();
            method.setDeclared(false);
            Body activeBody = method.retrieveActiveBody();
            UnitPatchingChain units = activeBody.getUnits();
            Iterator<Unit> unitIterator =units.iterator();
            boolean init = false;
            setRef(processedClass, activeBody);



             while (unitIterator.hasNext()) {
                 Unit unit = unitIterator.next();
                 String methodContent = unit.toString();

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
                            String[] fieldAndAssignment = new String[3];
                            fieldAndAssignment[0] = data[0].split(" ")[2];
                            fieldAndAssignment[1] = data[1].split("= ")[1];
                            fieldAndAssignment[2] = data[0].split(" ")[1];
                            staticToBeReplaced.add(fieldAndAssignment);
                            toRemove.add(unit);
                            clinitPred = unit;
                        }
                    }

                }

                else if (methodContent.contains("getConnection")){
                    connPred = unit;
                    toRemove.add(unit);
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

                    if ((queryData[0]).equals("insert")){
                        nameTypeAndTable[1] = "insert";
                        nameTypeAndTable[2] = queryData[2];
                        int count = (unitData[1].split("\\?").length - 1);
                        InsertStatement newStatement = new InsertStatement(nameTypeAndTable[0], nameTypeAndTable[2], count);
                        insertStatements.add(newStatement);
                    }

                    else if ((queryData[0]).equals("select")){
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
                        SelectStatement newStatement = new SelectStatement(nameTypeAndTable[0], nameTypeAndTable[2], nameTypeAndTable[1]);
                        selectStatements.add(newStatement);
                    }
                }

                else if (methodContent.contains("<" + oldClass.getName() + ":")) {
                    String data = unit.toString();
                    String[] fieldAndAssignment = new String[3];
                    fieldAndAssignment[0] = data.split(" ")[4].replaceAll(">", "");
                    fieldAndAssignment[1] = data.split(" =")[0];
                    fieldAndAssignment[2] = data.split(" ")[3];
                    mFieldToBeReplaced.add(fieldAndAssignment);
                    toRemove.add(unit);

                    if (mFieldPred == null) {
                        mFieldPred = unit;
                    }
                }

                else if (insertStatements.stream().anyMatch(x -> methodContent.contains(x.getLocalName()))) {
                    InsertStatement statement = insertStatements.stream().filter(x -> methodContent.contains(x.getLocalName())).findFirst().get();

                    if(methodContent.contains("void set")) {
                        String[] data = unit.toString().split("\\(");
                        String type = data[1].split(",")[1].split("\\)")[0];
                        int pos = Integer.valueOf(data[2].split(",")[0]);
                        String value = data[2].split(",")[1].split("\\)")[0].substring(1);
                        statement.addParameter(pos, value, type);
                        toRemove.add(unit);
                    }

                    else if (methodContent.contains("executeUpdate()")) {
                        statement.setPred(unit);
                        insertToBeReplaced.add(statement);
                        toRemove.add(unit);
                    }
                }

                else if (selectStatements.stream().anyMatch(x -> methodContent.contains(x.getLocalName()))) {
                    if (methodContent.contains("java.sql.ResultSet executeQuery()")) {
                        SelectStatement statement = selectStatements.stream().filter(x -> methodContent.contains(x.getLocalName())).findFirst().get();
                        statement.setAssignedLocal(methodContent.split(" ")[0]);
                        RSStatement rsStatement = new RSStatement(statement.getAssignedLocal());
                        rsStatements.add(rsStatement);
                        statement.setPred(unit);
                        selectToBeReplaced.add(statement);
                        toRemove.add(unit);
                    }
                }

                else if (rsStatements.stream().anyMatch(x -> methodContent.contains(x.getLocalName()))) {
                    RSStatement statement = rsStatements.stream().filter(x -> methodContent.contains(x.getLocalName())).findFirst().get();
                    if (methodContent.contains("java.sql.ResultSet: boolean next()") && !methodContent.contains("goto")) {
                        statement.setPred(unit);
                        statement.setAssignedLocalName(methodContent.split(" ")[0]);
                        nextToBeReplaced.add(statement);
                        toRemove.add(unit);
                    }
                    else if (methodContent.contains("java.sql.ResultSet") && methodContent.contains("get")) {

                    }
                }

            }

             if (init) {
                 processInit(initPred, units, activeBody, processedClass);
             }

             if (clinitPred != null) {
                 for(int i = 0; i < staticToBeReplaced.size(); i++) {
                     String[] current = staticToBeReplaced.get(i);
                     processClinit(clinitPred, units, activeBody, processedClass, current[0], current[1], current[2]);
                 }
             }

             if (connPred != null) {
                 processConnection(connPred, units, activeBody);
             }

             if (!mFieldToBeReplaced.isEmpty()) {
                 for(int i = 0; i < mFieldToBeReplaced.size(); i++) {
                     String[] current = mFieldToBeReplaced.get(i);
                     processMethodField(mFieldPred, units, activeBody, processedClass, current[0], current[1], current[2]);
                 }
             }

             if (!insertToBeReplaced.isEmpty()) {
                 for (int i = 0; i < insertToBeReplaced.size(); i++) {
                     InsertStatement current =  insertToBeReplaced.get(i);
                     processInsertStatement(current, units, activeBody, processedClass);
                 }
             }

             if (!selectToBeReplaced.isEmpty()) {
                 for (int i = 0; i < selectToBeReplaced.size(); i++) {
                     SelectStatement current = selectToBeReplaced.get(i);
                     processSelectStatement(current, units, activeBody, processedClass);
                 }
             }

             if (!nextToBeReplaced.isEmpty()) {
                 for (int i = 0; i < nextToBeReplaced.size(); i++) {
                     RSStatement current = nextToBeReplaced.get(i);
                     processNext(current, units, activeBody, processedClass);
                 }
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
        connectionLocal = newConn;

        units.insertAfter(newUnits, pred);
    }


    private void processInit(Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        newUnits.add(Jimple.v().newIdentityStmt(activeBody.getThisLocal(), Jimple.v().newThisRef(processedClass.getType())));

        units.insertAfter(newUnits, pred);
    }

    private void processMethodField (Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass, String field, String assignment, String type) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(assignment)).findFirst().get();
        StaticFieldRef instanceFieldRef = Jimple.v().newStaticFieldRef((processedClass.getFieldByName(field).makeRef()));
        newUnits.add(Jimple.v().newAssignStmt(assigned, instanceFieldRef));
        units.insertAfter(newUnits, pred);

    }

    private void processClinit(Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass, String field, String assignment, String type){
        ArrayList<Unit> newUnits = new ArrayList<>();
        StaticFieldRef instanceFieldRef = Jimple.v().newStaticFieldRef((processedClass.getFieldByName(field).makeRef()));
        switch (type) {
            case "int":
                newUnits.add(Jimple.v().newAssignStmt(instanceFieldRef, IntConstant.v(Integer.parseInt(assignment))));
                break;
            case "java.lang.String":
                newUnits.add(Jimple.v().newAssignStmt(instanceFieldRef, StringConstant.v(assignment.replaceAll("\"", ""))));
                break;
        }
        units.insertAfter(newUnits, pred);
    }

    private void processInsertStatement(InsertStatement statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        ArrayList<Local> parameterList = new ArrayList<>();
        for (int i = 0; i < statement.getParameterCount(); i++) {
            String value = statement.getParameter(i);
            Optional<Local> exists = activeBody.getLocals().stream().filter(x -> x.getName().equals(value)).findFirst();
            if(exists.isPresent()) {
                parameterList.add(exists.get());
            }
            else {
                switch (statement.getType(i)) {
                    case "int":
                        Local intLocal = Jimple.v().newLocal("param", IntType.v());
                        activeBody.getLocals().add(intLocal);
                        newUnits.add(Jimple.v().newAssignStmt(intLocal, IntConstant.v(Integer.parseInt(statement.getParameter(i)))));
                        parameterList.add(intLocal);
                        break;
                    case "java.lang.String":
                        Local stringLocal = Jimple.v().newLocal("param", Scene.v().getRefType("java.lang.String"));
                        activeBody.getLocals().add(stringLocal);
                        newUnits.add(Jimple.v().newAssignStmt(stringLocal, StringConstant.v(statement.getParameter(i))));
                        parameterList.add(stringLocal);
                        break;
                }
            }
        }
        SootMethod toCall = Scene.v().getSootClass("Connection").getMethodByName(statement.getTableName() + "InsertStatement");
        newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(connectionLocal, toCall.makeRef(), parameterList)));
        units.insertAfter(newUnits, statement.getPred());
    }

    private void processSelectStatement(SelectStatement statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
        assigned.setType(Scene.v().getRefType("java.util.Iterator"));
        SootMethod toCall = Scene.v().getSootClass("Connection").getMethodByName(statement.getTableName() + "SelectStatement");
        newUnits.add(Jimple.v().newAssignStmt(assigned, Jimple.v().newVirtualInvokeExpr(connectionLocal, toCall.makeRef())));
        units.insertAfter(newUnits, statement.getPred());
    }

    private void processNext(RSStatement statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocalName())).findFirst().get();
        Local rs = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getLocalName())).findFirst().get();
        SootMethod toCall = Scene.v().getSootClass("java.util.Iterator").getMethodByName("hasNext");
        newUnits.add(Jimple.v().newAssignStmt(assigned, Jimple.v().newInterfaceInvokeExpr(rs, toCall.makeRef())));
        units.insertAfter(newUnits, statement.getPred());
    }

    private void setRef(SootClass processedClass,Body activeBody) {
        ref = soot.jimple.Jimple.v().newLocal("this", processedClass.getType());
        activeBody.getLocals().add(ref);
    }
}
