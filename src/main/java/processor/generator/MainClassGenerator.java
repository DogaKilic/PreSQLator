package processor.generator;

import processor.statement.InsertStatement;
import processor.statement.MFieldStatement;
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
            if (field.getType().toString().equals("java.sql.Connection")){
                field.setType(Scene.v().getRefType("Connection"));
            }
            processedClass.addField(field);
            field.setDeclared(true);
        }

        for(SootMethod method : methodList) {

            ArrayList<InsertStatement> insertStatements = new ArrayList<>();
            ArrayList<SelectStatement> selectStatements = new ArrayList<>();
            ArrayList<String[]> staticToBeReplaced = new ArrayList<>();
            ArrayList<MFieldStatement> mFieldToBeReplaced = new ArrayList<>();
            ArrayList<String[]> initFieldToBeReplaced = new ArrayList<>();
            ArrayList<InsertStatement> insertToBeReplaced = new ArrayList<>();
            ArrayList<SelectStatement> selectToBeReplaced = new ArrayList<>();
            ArrayList<RSStatement> rsStatements = new ArrayList<>();
            ArrayList<RSStatement> nextToBeReplaced = new ArrayList<>();
            ArrayList<RSStatement> getToBeReplaced = new ArrayList<>();
            Unit initPred = null;
            Unit thisPred = null;
            Unit connPred = null;
            Unit clinitPred = null;
            String connLocal = "";
            connectionLocal = null;
            ArrayList<Unit> toRemove = new ArrayList<>();
            method.setDeclared(false);
            Body activeBody = method.retrieveActiveBody();
            UnitPatchingChain units = activeBody.getUnits();
            Iterator<Unit> unitIterator =units.iterator();
            boolean init = false;
            setRef(processedClass, activeBody);


            if (method.getReturnType().equals(Scene.v().getRefType("java.sql.ResultSet"))){
                method.setReturnType(Scene.v().getRefType("java.util.Iterator"));
            }

             while (unitIterator.hasNext()) {
                 Unit unit = unitIterator.next();
                 String methodContent = unit.toString();
                if (methodContent.contains("goto")) {

                 }
                else if (method.getName().equals("<init>")) {

                    if (unit.equals(activeBody.getThisUnit())) {
                        toRemove.add(unit);
                    }

                    if (!init) {
                        activeBody.getThisLocal().setType(processedClass.getType());
                        initPred = unit;
                        init = true;
                    }

                    Optional<ValueBox> valueBox;
                    valueBox = unit.getDefBoxes().stream().findFirst();

                    if(valueBox.isPresent()) {

                        if(valueBox.get().getValue().toString().contains("<" + oldClass.getName() + ":")) {
                            String[] data = unit.toString().split(">");
                            String[] fieldAndAssignment = new String[3];
                            fieldAndAssignment[0] = data[0].split(" ")[2];
                            fieldAndAssignment[1] = data[1].split("= ")[1];
                            fieldAndAssignment[2] = data[0].split(" ")[1];
                            initFieldToBeReplaced.add(fieldAndAssignment);
                            toRemove.add(unit);
                        }
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

                else if (methodContent.contains("@this")) {
                    activeBody.getThisLocal().setType(processedClass.getType());
                    thisPred = unit;
                    toRemove.add(unit);
                }

                else if (methodContent.contains("getConnection")){
                    connPred = unit;
                    connLocal = methodContent.split(" =")[0];
                    String temp = connLocal;
                    Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(temp)).findFirst().get();
                    connectionLocal = assigned;
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
                    if (methodContent.split("=")[1].contains("java.sql.Connection") && connectionLocal == null) {
                        String temp = methodContent.split(" =")[0];
                        Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(temp)).findFirst().get();
                        connectionLocal = assigned;
                    }
                    if (methodContent.split("=")[1].contains("<" + oldClass.getName() + ":")) {
                        if (methodContent.split("<")[0].contains(".")){
                            String assignedLocal = methodContent.split(" =")[0];
                            String fieldLocal;
                            String field;
                            if(methodContent.contains("virtualinvoke") || methodContent.contains("specialinvoke")) {
                                MFieldStatement statement = null;
                                fieldLocal = methodContent.split(" ")[3].split("\\.")[0];
                                field = methodContent.split(" ")[5].split("\\(")[0];
                                if (methodContent.contains("specialinvoke")) {

                                    statement = new MFieldStatement(assignedLocal, field, 3);
                                }
                                else if(methodContent.contains("virtualinvoke")) {
                                    statement = new MFieldStatement(assignedLocal, field, 5);
                                }
                                if (methodContent.split(">\\(").length > 1 && !methodContent.split(">\\(")[1].equals(")")) {
                                    String[] content = methodContent.split(">\\(")[1].split("\\)")[0].split(",");
                                    String[] types = methodContent.split("\\)>")[0].split("\\(")[1].split(",");
                                    for(int i = 0; i < content.length; i++) {
                                        String result = content[i].replaceAll(" ","");
                                        Optional<Local> exists = activeBody.getLocals().stream().filter(x -> x.getName().equals(result)).findFirst();
                                        if(exists.isPresent()) {
                                            statement.addParameter((exists.get()));
                                        }
                                        else {
                                            String temp = types[i].replaceAll(" ","");
                                            switch (temp) {
                                                case "int":
                                                    statement.addParameter(IntConstant.v(Integer.parseInt(temp)));
                                                    break;
                                                case "java.lang.String":
                                                    statement.addParameter(StringConstant.v(temp));
                                                    break;
                                            }
                                        }

                                    }
                                }
                                statement.setFieldLocal(fieldLocal);
                                statement.setPred(unit);
                                mFieldToBeReplaced.add(statement);
                            }
                            else {
                                fieldLocal = methodContent.split(" ")[2].split("\\.")[0];
                                field = methodContent.split(" ")[4].split(">")[0];
                                MFieldStatement statement = new MFieldStatement(assignedLocal, field, 4);
                                statement.setFieldLocal(fieldLocal);
                                statement.setPred(unit);
                                statement.setLocalType(methodContent.split(" ")[3]);
                                mFieldToBeReplaced.add(statement);
                            }
                            toRemove.add(unit);
                        }
                        else {
                            String[] fieldAndAssignment = new String[3];
                            fieldAndAssignment[0] = methodContent.split(" ")[4].replaceAll(">", "");
                            fieldAndAssignment[1] = methodContent.split(" =")[0];
                            fieldAndAssignment[2] = methodContent.split(" ")[3];
                            MFieldStatement statement = new MFieldStatement(fieldAndAssignment[1], fieldAndAssignment[0], 1);
                            statement.setPred(unit);
                            mFieldToBeReplaced.add(statement);
                            toRemove.add(unit);
                        }
                    }
                    else if (methodContent.split("=")[0].contains("<" + oldClass.getName() + ":")) {
                        String field = methodContent.split(" ")[2].replaceAll(">", "");
                        MFieldStatement statement = new MFieldStatement("", field,2);
                        statement.setFieldLocal(methodContent.split("\\.")[0]);
                        statement.setValueLocal(methodContent.split("= ")[1]);
                        statement.setLocalType(methodContent.split(" ")[1]);
                        statement.setPred(unit);
                        mFieldToBeReplaced.add(statement);
                        toRemove.add(unit);
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
                 for (int i = 0; i < initFieldToBeReplaced.size(); i++) {
                     String[] current = initFieldToBeReplaced.get(i);
                     processInitFields(initPred, units, activeBody, processedClass, current[0], current[1], current[2]);
                 }
                 processInit(initPred, units, activeBody, processedClass);
             }
             else {
                 if (thisPred != null) {
                     processInit(thisPred, units, activeBody, processedClass);
                 }
             }

             if (clinitPred != null) {
                 for(int i = 0; i < staticToBeReplaced.size(); i++) {
                     String[] current = staticToBeReplaced.get(i);
                     processClinit(clinitPred, units, activeBody, processedClass, current[0], current[1], current[2]);
                 }
             }


             if (connPred != null) {
                 processConnection(connPred, units, activeBody, connLocal);
             }

             if (!mFieldToBeReplaced.isEmpty()) {
                 for(int i = 0; i < mFieldToBeReplaced.size(); i++) {
                     processMethodField(mFieldToBeReplaced.get(i),units, activeBody, processedClass);
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

    private void  processConnection(Unit pred, UnitPatchingChain units, Body activeBody, String connLocal) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(connLocal)).findFirst().get();
        assigned.setType(Scene.v().getRefType("Connection"));
        newUnits.add(Jimple.v().newAssignStmt(assigned, Jimple.v().newNewExpr(Scene.v().getRefType("Connection"))));
        SpecialInvokeExpr listInv = Jimple.v().newSpecialInvokeExpr(assigned, Scene.v().getSootClass("Connection").getMethod("<init>", new LinkedList<Type>()).makeRef());
        newUnits.add(Jimple.v().newInvokeStmt(listInv));
        connectionLocal = assigned;

        units.insertAfter(newUnits, pred);
    }


    private void processInit(Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        newUnits.add(Jimple.v().newIdentityStmt(activeBody.getThisLocal(), Jimple.v().newThisRef(processedClass.getType())));


        units.insertAfter(newUnits, pred);
    }

    private void processMethodField (MFieldStatement statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        FieldRef fieldRef;
        if (statement.getType() == 1) {
            Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
            fieldRef = Jimple.v().newStaticFieldRef((processedClass.getFieldByName(statement.getField()).makeRef()));
            newUnits.add(Jimple.v().newAssignStmt(assigned, fieldRef));
        }
        else if (statement.getType() == 2) {
            Local fieldLocal = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getFieldLocal())).findFirst().get();
            fieldRef = Jimple.v().newInstanceFieldRef(fieldLocal, processedClass.getFieldByName(statement.getField()).makeRef());
            if(activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getValueLocal())).findFirst().isPresent()) {
                Local valueLocal = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getValueLocal())).findFirst().get();
                newUnits.add(Jimple.v().newAssignStmt(fieldRef, valueLocal));
            }
            else{
                if (statement.getValueLocal().equals("null")) {
                    newUnits.add(Jimple.v().newAssignStmt(fieldRef, NullConstant.v()));
                }
                switch (statement.getLocalType()) {
                    case "int":
                        newUnits.add(Jimple.v().newAssignStmt(fieldRef, IntConstant.v(Integer.parseInt(statement.getValueLocal()))));
                        break;
                    case "java.lang.String":
                        newUnits.add(Jimple.v().newAssignStmt(fieldRef, StringConstant.v(statement.getValueLocal().replaceAll("\"", ""))));
                        break;
                }
            }
        }
        else if (statement.getType() == 3){
            Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
            Local fieldLocal = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getFieldLocal())).findFirst().get();
            SpecialInvokeExpr inv = Jimple.v().newSpecialInvokeExpr(fieldLocal, processedClass.getMethodByName(statement.getField()).makeRef());
            newUnits.add(Jimple.v().newAssignStmt(assigned, inv));
        }

        else if (statement.getType() == 4) {
            Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
            Local fieldLocal = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getFieldLocal())).findFirst().get();
            if (assigned.getType().toString().equals("java.sql.Connection")) {
                assigned.setType(Scene.v().getRefType("Connection"));
            }
            fieldRef = Jimple.v().newInstanceFieldRef(fieldLocal, processedClass.getFieldByName(statement.getField()).makeRef());
            newUnits.add(Jimple.v().newAssignStmt(assigned, fieldRef));

        }
        else if (statement.getType() == 5) {
            Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
            Local fieldLocal = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getFieldLocal())).findFirst().get();
            VirtualInvokeExpr inv = Jimple.v().newVirtualInvokeExpr(fieldLocal, processedClass.getMethodByName(statement.getField()).makeRef(), statement.getParameters());
            newUnits.add(Jimple.v().newAssignStmt(assigned, inv));
        }
        units.insertAfter(newUnits, statement.getPred());
    }

    private void processClinit(Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass, String field, String assignment, String type){
        ArrayList<Unit> newUnits = new ArrayList<>();
        StaticFieldRef instanceFieldRef = Jimple.v().newStaticFieldRef((processedClass.getFieldByName(field).makeRef()));
        if (assignment.equals("null")) {
            newUnits.add(Jimple.v().newAssignStmt(instanceFieldRef, NullConstant.v()));
        }
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

    private void processInitFields(Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass, String field, String assignment, String type) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(activeBody.getThisLocal(), processedClass.getFieldByName(field).makeRef());
        if (assignment.equals("null")) {
            newUnits.add(Jimple.v().newAssignStmt(instanceFieldRef, NullConstant.v()));
        }
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