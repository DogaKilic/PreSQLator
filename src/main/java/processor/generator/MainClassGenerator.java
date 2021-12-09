package processor.generator;

import content.TableBank;
import processor.replace.*;
import soot.*;
import soot.jimple.*;
import util.ClassWriter;
import java.util.*;

public class MainClassGenerator extends ClassGenerator {
    public static Unit currentPred;
    public static UnitPatchingChain currentUnits;
    public static Body currentBody;
    private ArrayList<String> processedRsMethods = new ArrayList<>();
    private ArrayList<String> processedRsTableName = new ArrayList<>();
    private Local ref;
    private Local connectionLocal;

    public void generateClass(SootClass oldClass) {
        SootClass processedClass = new SootClass(oldClass.getName() + "Processed", Modifier.PUBLIC);
        processedClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(processedClass);

        List<SootMethod> methodList = oldClass.getMethods();
        List<String> oldMethods;
        for(SootField field : oldClass.getFields()){
            field.setDeclared(false);
            if (field.getType().toString().equals("java.sql.Connection")){
                field.setType(Scene.v().getRefType("Connection"));
            }
            processedClass.addField(field);
            field.setDeclared(true);
        }


        for (SootMethod method : methodList) {
            if (method.getReturnType().equals(Scene.v().getRefType("java.sql.ResultSet"))){
                processedRsMethods.add(method.getName());
                Iterator<Unit> unitIterator = method.retrieveActiveBody().getUnits().iterator();
                while (unitIterator.hasNext()) {
                    Unit unit = unitIterator.next();
                    String methodContent = unit.toString();
                    if (methodContent.contains("prepareStatement")) {
                        String[] unitData = unit.toString().split("\"");
                        String[] nameTypeAndTable = new String[4];
                        String[] queryData = unitData[1].split(" ");

                        if (unitData[0].split(" ")[0].equals("interfaceinvoke")){
                            continue;
                        }

                        else {
                            nameTypeAndTable[0] = unitData[0].split(" ")[0];
                        }
                        if ((queryData[0]).equals("select")){
                            int tableIndex = 0;
                            String result = "";
                            if(queryData[1] == "*"){
                                nameTypeAndTable[1] = "*";
                                tableIndex = 3;
                            }
                            else {
                                for (int i = 1; i < queryData.length; i++) {

                                    if (queryData[i].endsWith(",")) {
                                        result += queryData[i].split(",")[0] + ",";
                                    } else {
                                        result += queryData[i];
                                        tableIndex = i + 2;
                                        break;
                                    }
                                }
                            }
                            nameTypeAndTable[2] = queryData[tableIndex];
                            processedRsTableName.add(nameTypeAndTable[2]);
                            break;
                        }
                    }
                }
            }
        }

        if (processedRsMethods.size() > processedRsTableName.size()) {
            int diff = processedRsMethods.size() - processedRsTableName.size();
            for (int i = 0; i < diff; i++) {
                processedRsTableName.add(processedRsTableName.get(0));
            }
        }


        for(SootMethod method : methodList) {
            ArrayList<InsertReplace> insertReplaces = new ArrayList<>();
            ArrayList<SelectReplace> selectReplaces = new ArrayList<>();
            ArrayList<DeleteReplace> deleteReplaces = new ArrayList<>();
            ArrayList<UpdateReplace> updateReplaces = new ArrayList<>();
            ArrayList<String[]> staticToBeReplaced = new ArrayList<>();
            ArrayList<MFieldReplace> mFieldToBeReplaced = new ArrayList<>();
            ArrayList<String[]> initFieldToBeReplaced = new ArrayList<>();
            ArrayList<InsertReplace> insertToBeReplaced = new ArrayList<>();
            ArrayList<SelectReplace> selectToBeReplaced = new ArrayList<>();
            ArrayList<DeleteReplace> deleteToBeReplaced = new ArrayList<>();
            ArrayList<UpdateReplace> updateToBeReplaced = new ArrayList<>();
            ArrayList<RSReplace> rsReplaces = new ArrayList<>();
            ArrayList<RSReplace> nextToBeReplaced = new ArrayList<>();
            ArrayList<RSReplace> getToBeReplaced = new ArrayList<>();
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



            if (processedRsMethods.contains(method.getName())) {
                method.setReturnType(Scene.v().getRefType("java.util.Iterator"));
            }

            if (activeBody.getLocals().stream().filter(x -> x.getType().equals(oldClass.getType())).findFirst().isPresent()
                    || activeBody.getLocals().stream().filter(x -> x.getType().equals(Scene.v().getRefType("java.sql.Connection"))).findFirst().isPresent()) {
                Iterator<Local> localIterator = activeBody.getLocals().iterator();
                while (localIterator.hasNext()) {
                    Local local = localIterator.next();
                    if (local.getType().equals(oldClass.getType())) {
                        local.setType(processedClass.getType());
                    }
                    if (local.getType().equals(Scene.v().getRefType("java.sql.Connection"))) {
                        local.setType(Scene.v().getRefType("Connection"));
                    }
                }
            }


             boolean skipNext = false;

             while (unitIterator.hasNext()) {

                 Unit unit = unitIterator.next();
                 String methodContent = unit.toString();



                if (processedRsMethods.stream().filter(x -> methodContent.contains(x)).findFirst().isPresent()) {
                    Iterator<String> stringIterator = processedRsMethods.stream().iterator();
                    int index = 0;
                    while (stringIterator.hasNext()) {
                        String current = stringIterator.next();
                        if (methodContent.contains(current)) {
                            break;
                        }
                        else {
                            index++;
                        }
                    }
                    String local = methodContent.split(" =")[0];
                    RSReplace statement = new RSReplace(local);
                    statement.setTableName(processedRsTableName.get(index));
                    rsReplaces.add(statement);
                }

                 if (methodContent.contains("goto")) {}

                else if (!methodContent.contains("java.sql.Connection getConnection") && method.getName().equals("<init>")) {

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
                        InsertReplace newStatement = new InsertReplace(nameTypeAndTable[0], nameTypeAndTable[2], count);
                        insertReplaces.add(newStatement);
                    }

                    else if ((queryData[0]).equals("select")){
                        int tableIndex = 0;
                        String result = "";
                        if(queryData[1] == "*"){
                            nameTypeAndTable[1] = "*";
                            tableIndex = 3;
                        }
                        else {
                            for (int i = 1; i < queryData.length; i++) {

                                if (queryData[i].endsWith(",")) {
                                    result += queryData[i].split(",")[0] + ",";
                                } else {
                                    result += queryData[i];
                                    tableIndex = i + 2;
                                    break;
                                }
                            }
                            nameTypeAndTable[1] = result;
                        }
                        nameTypeAndTable[2] = queryData[tableIndex];
                        nameTypeAndTable[3] = "";
                        SelectReplace newStatement = new SelectReplace(nameTypeAndTable[0], nameTypeAndTable[2], nameTypeAndTable[1]);
                        selectReplaces.add(newStatement);
                    }
                    else if ((queryData[0]).equals("delete")) {
                        String table = queryData[2];
                        DeleteReplace newStatement = new DeleteReplace(nameTypeAndTable[0], table);
                        deleteReplaces.add(newStatement);
                    }
                    else if ((queryData[0]).equals("update")) {
                        String table = queryData[1];
                        UpdateReplace update = new UpdateReplace(nameTypeAndTable[0], table);
                        updateReplaces.add(update);
                    }
                }

                else if (methodContent.contains("<" + oldClass.getName() + ":")) {
                    boolean assignment = methodContent.contains("=");
                        if (assignment && methodContent.split("=")[1].contains("java.sql.Connection") && connectionLocal == null) {
                            String temp = methodContent.split(" =")[0];
                            Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(temp)).findFirst().get();
                            connectionLocal = assigned;
                        }
                        if (!assignment || methodContent.split("=")[1].contains("<" + oldClass.getName() + ":")) {
                            if (methodContent.split("<")[0].contains(".")) {
                                String assignedLocal;
                                if (assignment) {
                                    assignedLocal = methodContent.split(" =")[0];
                                }
                                else { assignedLocal = "";}
                                String fieldLocal;
                                String field;
                                if (methodContent.contains("virtualinvoke") || methodContent.contains("specialinvoke")) {
                                    MFieldReplace statement = null;
                                    if(assignment) {
                                        fieldLocal = methodContent.split(" ")[3].split("\\.")[0];
                                        field = methodContent.split(" ")[5].split("\\(")[0];
                                    }
                                    else {
                                        fieldLocal = methodContent.split(" ")[1].split("\\.")[0];
                                        field = methodContent.split(" ")[3].split("\\(")[0];
                                    }
                                    if (methodContent.contains("specialinvoke")) {

                                        statement = new MFieldReplace(assignedLocal, field, 3);
                                    } else if (methodContent.contains("virtualinvoke")) {
                                        statement = new MFieldReplace(assignedLocal, field, 5);
                                    }
                                    if (methodContent.split(">\\(").length > 1 && !methodContent.split(">\\(")[1].equals(")")) {
                                        String[] content = methodContent.split(">\\(")[1].split("\\)")[0].split(",");
                                        String[] types = methodContent.split("\\)>")[0].split("\\(")[1].split(",");
                                        for (int i = 0; i < content.length; i++) {
                                            String result = content[i].replaceAll(" ", "");
                                            Optional<Local> exists = activeBody.getLocals().stream().filter(x -> x.getName().equals(result)).findFirst();
                                            if (exists.isPresent()) {
                                                statement.addParameter((exists.get()));
                                            } else {
                                                String temp = types[i].replaceAll(" ", "");
                                                switch (temp) {
                                                    case "int":
                                                        statement.addParameter(IntConstant.v(Integer.parseInt(result)));
                                                        break;
                                                    case "java.lang.String":
                                                        statement.addParameter(StringConstant.v(result));
                                                        break;
                                                }
                                            }

                                        }
                                    }
                                    statement.setFieldLocal(fieldLocal);
                                    statement.setPred(unit);
                                    mFieldToBeReplaced.add(statement);
                                } else {
                                    fieldLocal = methodContent.split(" ")[2].split("\\.")[0];
                                    field = methodContent.split(" ")[4].split(">")[0];
                                    MFieldReplace statement = new MFieldReplace(assignedLocal, field, 4);
                                    statement.setFieldLocal(fieldLocal);
                                    statement.setPred(unit);
                                    statement.setLocalType(methodContent.split(" ")[3]);
                                    mFieldToBeReplaced.add(statement);
                                }
                                toRemove.add(unit);
                            }
                            else if (methodContent.contains("staticinvoke")) {
                            }
                            else {
                                String[] fieldAndAssignment = new String[3];
                                fieldAndAssignment[0] = methodContent.split(" ")[4].replaceAll(">", "");
                                fieldAndAssignment[1] = methodContent.split(" =")[0];
                                fieldAndAssignment[2] = methodContent.split(" ")[3];
                                MFieldReplace statement = new MFieldReplace(fieldAndAssignment[1], fieldAndAssignment[0], 1);
                                statement.setPred(unit);
                                mFieldToBeReplaced.add(statement);
                                toRemove.add(unit);
                            }
                        } else if (methodContent.split("=")[0].contains("<" + oldClass.getName() + ":")) {
                            String field = methodContent.split(" ")[2].replaceAll(">", "");
                            MFieldReplace statement = new MFieldReplace("", field, 2);
                            statement.setFieldLocal(methodContent.split("\\.")[0]);
                            statement.setValueLocal(methodContent.split("= ")[1]);
                            statement.setLocalType(methodContent.split(" ")[1]);
                            statement.setPred(unit);
                            mFieldToBeReplaced.add(statement);
                            toRemove.add(unit);
                        }
                }

                else if (methodContent.contains(oldClass.getName())) {
                    if(methodContent.contains("= new")) {
                        String assigned = methodContent.split(" =")[0];
                        MFieldReplace statement = new MFieldReplace(assigned, "", 6);
                        statement.setPred(unit);
                        mFieldToBeReplaced.add(statement);
                        toRemove.add(unit);
                        toRemove.add(unitIterator.next());
                        continue;
                    }

                 }

                else if (insertReplaces.stream().anyMatch(x -> !methodContent.replace(x.getLocalName() + ".", "").contains(".<"))) {
                    InsertReplace statement = insertReplaces.stream().filter(x -> !methodContent.replace(x.getLocalName() + ".", "").contains(".<")).findFirst().get();

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

                else if (selectReplaces.stream().anyMatch(x -> !methodContent.replace(x.getLocalName() + ".", "").contains(".<"))) {
                    if (methodContent.contains("java.sql.ResultSet executeQuery()")) {
                        SelectReplace statement = selectReplaces.stream().filter(x -> !methodContent.replace(x.getLocalName() + ".", "").contains(".<")).findFirst().get();
                        statement.setAssignedLocal(methodContent.split(" ")[0]);
                        RSReplace rsReplace = new RSReplace(statement.getAssignedLocal());
                        rsReplace.setTableName(statement.getTableName());
                        rsReplaces.add(rsReplace);
                        statement.setPred(unit);
                        selectToBeReplaced.add(statement);
                        toRemove.add(unit);
                    }
                }

                else if (deleteReplaces.stream().anyMatch(x -> !methodContent.replace(x.getLocalName() + ".", "").contains(".<"))) {
                     if (methodContent.contains("executeUpdate()")) {
                         DeleteReplace statement = deleteReplaces.stream().filter(x -> !methodContent.replace(x.getLocalName() + ".", "").contains(".<")).findFirst().get();
                         statement.setPred(unit);
                         deleteToBeReplaced.add(statement);
                         toRemove.add(unit);
                     }
                 }

                 else if (updateReplaces.stream().anyMatch(x -> !methodContent.replace(x.getLocalName() + ".", "").contains(".<"))) {
                     if (methodContent.contains("executeUpdate()")) {
                         UpdateReplace statement = updateReplaces.stream().filter(x -> !methodContent.replace(x.getLocalName() + ".", "").contains(".<")).findFirst().get();
                         statement.setPred(unit);
                         updateToBeReplaced.add(statement);
                         toRemove.add(unit);
                     }
                 }

                else if (rsReplaces.stream().anyMatch(x -> methodContent.contains(x.getLocalName()))) {
                    RSReplace statement = rsReplaces.stream().filter(x -> methodContent.contains(x.getLocalName())).findFirst().get();
                    if (methodContent.contains("java.sql.ResultSet: boolean next()") && !methodContent.contains("goto")) {
                        statement.setPredNext(unit);
                        statement.setAssignedLocalNameNext(methodContent.split(" ")[0]);
                        nextToBeReplaced.add(statement);
                        toRemove.add(unit);
                    }
                    else if (methodContent.contains("java.sql.ResultSet") && (methodContent.contains("getInt") || methodContent.contains("getString"))) {
                        statement.addPredGet(unit);
                        statement.addAssignedLocalNameGet(methodContent.split(" ")[0]);
                        statement.addParamsGet(methodContent.split(">\\(")[1].split("\\)")[0]);
                        if(!getToBeReplaced.contains(statement)) {
                            getToBeReplaced.add(statement);
                        }
                        toRemove.add(unit);
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
                 processConnection(connPred, units, activeBody, connLocal, init, processedClass);
             }

             if (!mFieldToBeReplaced.isEmpty()) {
                 for(int i = 0; i < mFieldToBeReplaced.size(); i++) {
                     processMethodField(mFieldToBeReplaced.get(i),units, activeBody, processedClass);
                 }
             }

             if (!insertToBeReplaced.isEmpty()) {
                 for (int i = 0; i < insertToBeReplaced.size(); i++) {
                     InsertReplace current =  insertToBeReplaced.get(i);
                     processInsertStatement(current, units, activeBody, processedClass);
                 }
             }

             if (!deleteToBeReplaced.isEmpty()) {
                 for (int i = 0; i < deleteToBeReplaced.size(); i++) {
                     DeleteReplace current =  deleteToBeReplaced.get(i);
                     processDeleteStatement(current, units, activeBody, processedClass);
                 }
             }

            if (!updateToBeReplaced.isEmpty()) {
                for (int i = 0; i < updateToBeReplaced.size(); i++) {
                    UpdateReplace current =  updateToBeReplaced.get(i);
                    processUpdateStatement(current, units, activeBody, processedClass);
                }
            }

             if (!selectToBeReplaced.isEmpty()) {
                 for (int i = 0; i < selectToBeReplaced.size(); i++) {
                     SelectReplace current = selectToBeReplaced.get(i);
                     currentPred = current.getPred();
                     currentUnits = units;
                     currentBody = activeBody;
                     processSelectStatement(current, units, activeBody, processedClass);
                 }
             }

             if (!nextToBeReplaced.isEmpty()) {
                 for (int i = 0; i < nextToBeReplaced.size(); i++) {
                     RSReplace current = nextToBeReplaced.get(i);
                     processNext(current, units, activeBody, processedClass);
                 }
             }

             if(!getToBeReplaced.isEmpty()) {
                 for (int i = 0; i < getToBeReplaced.size(); i++) {
                     RSReplace current = getToBeReplaced.get(i);
                     processGet(current, units, activeBody, processedClass);
                 }
             }


            units.removeAll(toRemove);
             toRemove.clear();
            method.setDeclaringClass(processedClass);
            processedClass.addMethod(method);
            method.setDeclared(true);
            activeBody.validate();

        }



        ClassWriter.writeAsClassFile(processedClass);
        ClassWriter.writeAsJimpleFile(processedClass);

    }



    private void  processConnection(Unit pred, UnitPatchingChain units, Body activeBody, String connLocal, boolean init, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(connLocal)).findFirst().get();
        assigned.setType(Scene.v().getRefType("Connection"));
        newUnits.add(Jimple.v().newAssignStmt(assigned, Jimple.v().newNewExpr(Scene.v().getRefType("Connection"))));
        SpecialInvokeExpr listInv = Jimple.v().newSpecialInvokeExpr(assigned, Scene.v().getSootClass("Connection").getMethod("<init>", new LinkedList<Type>()).makeRef());
        newUnits.add(Jimple.v().newInvokeStmt(listInv));
        connectionLocal = assigned;
        if (init) {
            newUnits.add(Jimple.v().newAssignStmt(Jimple.v().newInstanceFieldRef(activeBody.getThisLocal(), processedClass.getFieldByName("c").makeRef()), assigned));
        }

        units.insertAfter(newUnits, pred);
    }


    private void processInit(Unit pred, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        newUnits.add(Jimple.v().newIdentityStmt(activeBody.getThisLocal(), Jimple.v().newThisRef(processedClass.getType())));


        units.insertAfter(newUnits, pred);
    }

    private void processMethodField (MFieldReplace statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
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
            Local fieldLocal = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getFieldLocal())).findFirst().get();
            SpecialInvokeExpr inv = Jimple.v().newSpecialInvokeExpr(fieldLocal, processedClass.getMethodByName(statement.getField()).makeRef());
            if(statement.getAssignedLocal().equals("")) {
                Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
                newUnits.add(Jimple.v().newAssignStmt(assigned, inv));
            }
            else {
                newUnits.add(Jimple.v().newInvokeStmt(inv));
            }
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
            Local fieldLocal = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getFieldLocal())).findFirst().get();
            VirtualInvokeExpr inv = Jimple.v().newVirtualInvokeExpr(fieldLocal, processedClass.getMethodByName(statement.getField()).makeRef(), statement.getParameters());
            if (!statement.getAssignedLocal().equals("")) {
                Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
                newUnits.add(Jimple.v().newAssignStmt(assigned, inv));
            }
            else {
                newUnits.add(Jimple.v().newInvokeStmt(inv));
            }
        }
        else if (statement.getType() == 6) {
            Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
            newUnits.add(Jimple.v().newAssignStmt(assigned, Jimple.v().newNewExpr(processedClass.getType())));
            newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(assigned, processedClass.getMethodByName(SootMethod.constructorName).makeRef())));
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

    private void processInsertStatement(InsertReplace statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
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

    private void processSelectStatement(SelectReplace statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocal())).findFirst().get();
        assigned.setType(Scene.v().getRefType("java.util.Iterator"));
        SootMethod toCall = Scene.v().getSootClass("Connection").getMethodByName(statement.getTableName() + "SelectStatement" + statement.getLocalCount());
        newUnits.add(Jimple.v().newAssignStmt(assigned, Jimple.v().newVirtualInvokeExpr(connectionLocal, toCall.makeRef())));
        units.insertAfter(newUnits, statement.getPred());
    }

    private void processDeleteStatement(DeleteReplace statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        SootMethod toCall = Scene.v().getSootClass("Connection").getMethodByName(statement.getTableName() + "DeleteStatement" + statement.getLocalCount());
        newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(connectionLocal, toCall.makeRef())));
        units.insertAfter(newUnits, statement.getPred());
    }

    private void processUpdateStatement(UpdateReplace statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        SootMethod toCall = Scene.v().getSootClass("Connection").getMethodByName(statement.getTableName() + "UpdateStatement" + statement.getLocalCount());
        newUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(connectionLocal, toCall.makeRef())));
        units.insertAfter(newUnits, statement.getPred());
    }


    private void processNext(RSReplace statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Unit> newUnits = new ArrayList<>();
        Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getAssignedLocalNameNext())).findFirst().get();
        Local rs = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getLocalName())).findFirst().get();
        SootMethod toCall = Scene.v().getSootClass("java.util.Iterator").getMethodByName("hasNext");
        newUnits.add(Jimple.v().newAssignStmt(assigned, Jimple.v().newInterfaceInvokeExpr(rs, toCall.makeRef())));
        units.insertAfter(newUnits, statement.getPredNext());
    }

    private void processGet (RSReplace statement, UnitPatchingChain units, Body activeBody, SootClass processedClass) {
        ArrayList<Local> rsLocals = new ArrayList<>();
        for (int i = 0; i < statement.getGetSize(); i++) {
            ArrayList<Unit> newUnits = new ArrayList<>();
            final String cnt = statement.getAssignedLocalNameGet(i);
            Local assigned = activeBody.getLocals().stream().filter(x -> x.getName().equals(cnt)).findFirst().get();
            Local rs = activeBody.getLocals().stream().filter(x -> x.getName().equals(statement.getLocalName())).findFirst().get();
            int temp = 0;
            int paramNumb = 0;
            String finalString = statement.getParamsGet(i).replaceAll("\"", "");
            for (String[] content : TableBank.getColumnContent(statement.getTableName())) {
                if (finalString.equals(content[0])) {
                    paramNumb = temp;
                    break;
                }
                temp++;
            }
            Local rowLocal;
            if (!rsLocals.contains(rs)) {
                rsLocals.add(rs);
                SootMethod toCallRow = Scene.v().getSootClass("java.util.Iterator").getMethodByName("next");
                rowLocal = Jimple.v().newLocal("rowLocal" + rs.getName(), RefType.v("util.Row"));
                activeBody.getLocals().add(rowLocal);
                newUnits.add(Jimple.v().newAssignStmt(rowLocal, Jimple.v().newInterfaceInvokeExpr(rs, toCallRow.makeRef())));
            }
            else {
                rowLocal = activeBody.getLocals().stream().filter(x -> x.getName().equals("rowLocal" + rs.getName())).findFirst().get();
            }
            SootMethod toCallParam = Scene.v().getSootClass("util.Row").getMethodByName("getParameter");
            ArrayList<Value> args = new ArrayList<>();
            int finalParamNumb = paramNumb;
            args.add(IntConstant.v(finalParamNumb));
            newUnits.add(Jimple.v().newAssignStmt(assigned, Jimple.v().newVirtualInvokeExpr(rowLocal, toCallParam.makeRef(), args)));
            units.insertAfter(newUnits, statement.getPredGet(i));
        }
    }

    private void setRef(SootClass processedClass,Body activeBody) {
        ref = soot.jimple.Jimple.v().newLocal("this", processedClass.getType());
        activeBody.getLocals().add(ref);
    }
}