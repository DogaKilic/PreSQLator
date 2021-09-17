package processor.generator;

import content.TableBank;
import content.TableContent;
import fj.test.Bool;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;
import util.ClassWriter;
import util.ConnectionHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConnectionClassGenerator extends ClassGenerator {

    public void generateClass(List<TableContent> contents) {
        ArrayList<String> tableClassNames = new ArrayList<>();
        ArrayList<String> rowClassNames = new ArrayList<>();
        ArrayList<String> tableFieldNames = new ArrayList<>();
        ArrayList<SootField> tableFields = new ArrayList<>();
        SootClass connectionClass = new SootClass("Connection");
        connectionClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(connectionClass);
        for (TableContent i : contents) {
            String tableClassName = i.getTableName().substring(0, 1).toUpperCase() + (i.getTableName() + "Table").substring(1);
            String rowClassName = i.getTableName().substring(0, 1).toUpperCase() + (i.getTableName() + "Row").substring(1);
            String tableFieldName = i.getTableName() + "Table";
            SootField tableField = new SootField(tableFieldName, RefType.v(tableClassName), Modifier.PUBLIC);
            tableFields.add(tableField);
            tableClassNames.add(tableClassName);
            rowClassNames.add(rowClassName);
            tableFieldNames.add(tableFieldName);
            connectionClass.addField(tableField);
        }
        SootField helper = new SootField("helper", RefType.v("util.ConnectionHelper"), Modifier.PUBLIC);
        connectionClass.addField(helper);
        //create <init>
        SootMethod init = new SootMethod("<init>", null, VoidType.v(), Modifier.PUBLIC);
        connectionClass.addMethod(init);
        JimpleBody initBody = Jimple.v().newBody(init);
        init.setActiveBody(initBody);
        Chain units = initBody.getUnits();
        //create ref := @this
        Local ref;
        ref = Jimple.v().newLocal("this", connectionClass.getType());
        initBody.getLocals().add(ref);
        SpecialInvokeExpr refInv = Jimple.v().newSpecialInvokeExpr(ref, Scene.v().getSootClass("java.lang.Object").getMethod("<init>", new LinkedList<Type>()).makeRef());
        units.add(Jimple.v().newIdentityStmt(ref, Jimple.v().newThisRef(connectionClass.getType())));
        units.add(Jimple.v().newInvokeStmt(refInv));
        int cnt = 0;
        for (TableContent i : contents) {
            Local table;
            table = Jimple.v().newLocal("table" + cnt, Scene.v().getRefType(tableClassNames.get(cnt)));
            initBody.getLocals().add(table);
            units.add(Jimple.v().newAssignStmt(table, Jimple.v().newNewExpr(Scene.v().getRefType(tableClassNames.get(cnt)))));
            SpecialInvokeExpr tableInv = Jimple.v().newSpecialInvokeExpr(table, Scene.v().getSootClass(tableClassNames.get(cnt)).getMethod("<init>", new LinkedList<Type>()).makeRef());
            units.add(Jimple.v().newInvokeStmt(tableInv));
            InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(ref, connectionClass.getFieldByName(tableFieldNames.get(cnt)).makeRef());
            units.add(Jimple.v().newAssignStmt(instanceFieldRef, table));
            cnt++;
        }

        Local helperLocal;
        helperLocal = Jimple.v().newLocal("helper", Scene.v().getRefType("util.ConnectionHelper"));
        initBody.getLocals().add(helperLocal);
        units.add(Jimple.v().newAssignStmt(helperLocal, Jimple.v().newNewExpr(Scene.v().getRefType("util.ConnectionHelper"))));
        SpecialInvokeExpr helperTableInv = Jimple.v().newSpecialInvokeExpr(helperLocal, Scene.v().getSootClass("util.ConnectionHelper").getMethod("<init>", new LinkedList<Type>()).makeRef());
        units.add(Jimple.v().newInvokeStmt(helperTableInv));
        InstanceFieldRef helperInstanceFieldRef = Jimple.v().newInstanceFieldRef(ref, connectionClass.getFieldByName("helper").makeRef());
        units.add(Jimple.v().newAssignStmt(helperInstanceFieldRef, helperLocal));
        //add insertStatement methods
        cnt = 0;
        for (TableContent i : contents) {
                ArrayList<Local> parameterList = new ArrayList<>();
                ArrayList<Type> types = new ArrayList<>();
                for (String[] data : i.getColumnContent()) {
                    Type type = getType(data[1]);
                    types.add(type);
                }
                SootMethod insert = new SootMethod(i.getTableName() + "InsertStatement", types, VoidType.v(), Modifier.PUBLIC);
                connectionClass.addMethod(insert);
                JimpleBody insertBody = Jimple.v().newBody(insert);
                insert.setActiveBody(insertBody);
                Chain insertUnits = insertBody.getUnits();
                int parmCount = 0;
                Local insertRef = Jimple.v().newLocal("thisRef", connectionClass.getType());
                insertBody.getLocals().add(insertRef);
                insertUnits.add(Jimple.v().newIdentityStmt(insertRef, Jimple.v().newThisRef(connectionClass.getType())));
                for (String[] data : i.getColumnContent()) {
                    Local parm;
                    parm = Jimple.v().newLocal("parm" + parmCount, getType(data[1]));
                    insertBody.getLocals().add(parm);
                    insertUnits.add(Jimple.v().newIdentityStmt(parm,
                            Jimple.v().newParameterRef(getType(data[1]), parmCount)));
                    parmCount++;
                    parameterList.add(parm);
                }

                Local newRow = Jimple.v().newLocal("newRow" + cnt, Scene.v().getRefType(rowClassNames.get(cnt)));
                insertBody.getLocals().add(newRow);
                insertUnits.add(Jimple.v().newAssignStmt(newRow, Jimple.v().newNewExpr(Scene.v().getRefType(rowClassNames.get(cnt)))));
                SpecialInvokeExpr listInv = Jimple.v().newSpecialInvokeExpr(newRow, Scene.v().getSootClass(rowClassNames.get(cnt)).getMethodByName(SootMethod.constructorName).makeRef(), parameterList);
                insertUnits.add(Jimple.v().newInvokeStmt(listInv));
                Local tableClass = Jimple.v().newLocal("tableClass", RefType.v("ArrayList<>"));
                insertBody.getLocals().add(tableClass);
                Local table = Jimple.v().newLocal("table", RefType.v(tableClassNames.get(cnt)));
                insertBody.getLocals().add(table);
                insertUnits.add(Jimple.v().newAssignStmt(table, Jimple.v().newInstanceFieldRef(insertRef, connectionClass.getFieldByName(i.getTableName() + "Table").makeRef())));
                insertUnits.add(Jimple.v().newAssignStmt(tableClass, Jimple.v().newInstanceFieldRef(table, Scene.v().getSootClass(tableClassNames.get(cnt)).getFields().getFirst().makeRef())));
                SootMethod toCall = Scene.v().getSootClass("java.util.ArrayList").getMethods().get(21);
                insertUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(tableClass, toCall.makeRef(), newRow)));

                insertUnits.add(Jimple.v().newReturnVoidStmt());
                cnt++;
                insertBody.validate();

        }
        //add selectStatement methods
        cnt = 0;
        int nmb = 0;
            for (TableContent i : contents) {
                for (int j = 0; j < i.getWheresSize(); j++) {

                    ArrayList<String> where = i.getWheres(j);
                    String query = i.getQuery(j);
                    SootMethod select = new SootMethod(i.getTableName() + "SelectStatement" + j,  null, RefType.v("Iterator<" + rowClassNames.get(cnt) + ">"), Modifier.PUBLIC);
                    connectionClass.addMethod(select);
                    JimpleBody selectBody = Jimple.v().newBody(select);
                    select.setActiveBody(selectBody);
                    Chain selectUnits = selectBody.getUnits();
                    Local selectRef = Jimple.v().newLocal("thisRef" + nmb, connectionClass.getType());
                    selectBody.getLocals().add(selectRef);
                    selectUnits.add(Jimple.v().newIdentityStmt(selectRef, Jimple.v().newThisRef(connectionClass.getType())));
                    Local table = Jimple.v().newLocal("table" + nmb, RefType.v(tableClassNames.get(cnt)));
                    selectBody.getLocals().add(table);
                    Local tableList = Jimple.v().newLocal("tableClass" + nmb, RefType.v("ArrayList<>"));
                    selectBody.getLocals().add(tableList);
                    selectUnits.add(Jimple.v().newAssignStmt(table, Jimple.v().newInstanceFieldRef(selectRef, connectionClass.getFieldByName(i.getTableName() + "Table").makeRef())));
                    selectUnits.add(Jimple.v().newAssignStmt(tableList, Jimple.v().newInstanceFieldRef(table, Scene.v().getSootClass(tableClassNames.get(cnt)).getFields().getFirst().makeRef())));
                    if (where.isEmpty()){
                            SootMethod toCall = Scene.v().getSootClass("java.util.ArrayList").getMethodByName("iterator");
                            Local iterator = Jimple.v().newLocal("iterator" + nmb, RefType.v("Iterator"));
                            selectBody.getLocals().add(iterator);
                            selectUnits.add(Jimple.v().newAssignStmt(iterator, Jimple.v().newVirtualInvokeExpr(tableList, toCall.makeRef())));
                            selectUnits.add(Jimple.v().newReturnStmt(iterator));
                    }
                    else {
                        String current = where.get(0);
                        String[] data;
                            SootClass predicateClass = new SootClass(rowClassNames.get(cnt) + "Predicate" + j);
                            predicateClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
                            predicateClass.addInterface(Scene.v().getSootClass("java.util.function.Predicate"));
                            Scene.v().addClass(predicateClass);
                            ArrayList<Type> types = new ArrayList<>();
                            types.add(RefType.v("util.Row"));
                            SootMethod test = new SootMethod("test", types, BooleanType.v(), Modifier.PUBLIC);
                            predicateClass.addMethod(test);
                            JimpleBody testBody = Jimple.v().newBody(test);
                            test.setActiveBody(testBody);
                            Chain testUnits = testBody.getUnits();
                            Local rowLocal = Jimple.v().newLocal("row", RefType.v("util.Row"));
                            testBody.getLocals().add(rowLocal);
                            testUnits.add(Jimple.v().newIdentityStmt(rowLocal, Jimple.v().newParameterRef(RefType.v("util.row"), 0)));

                                if (current.contains("=") || current.contains("!=")) {
                                    boolean neq = current.contains("!=");
                                    String equals;
                                    int paramNumb = 0;
                                    int temp = 0;
                                    if (!neq) {
                                        data = current.split("=");
                                    }
                                    else {
                                        data = current.split("!=");
                                    }
                                    for(String[] content : TableBank.getColumnContent(rowClassNames.get(cnt).split("Row")[0].toLowerCase())) {
                                        if(data[0].equals(content[0])){
                                            paramNumb = temp;
                                            break;
                                        }
                                        temp++;
                                    }
                                    if (data[1].contains("\'")) {
                                        equals = data[1].replaceAll("\'", "");
                                    }
                                    else{
                                        equals = data[1];
                                    }
                                    int finalParamNumb = paramNumb;
                                    Local get = Jimple.v().newLocal("get", RefType.v("java.lang.String"));
                                    testBody.getLocals().add(get);
                                    SootMethod toCall = Scene.v().getSootClass("util.Row").getMethodByName("getParameter");
                                    testUnits.add(Jimple.v().newAssignStmt(get, Jimple.v().newVirtualInvokeExpr(rowLocal, toCall.makeRef(), IntConstant.v(finalParamNumb))));
                                    SootMethod equalsToCall = Scene.v().getSootClass("java.lang.Object").getMethodByName("equals");
                                    Local boolLocal = Jimple.v().newLocal("bool", BooleanType.v());
                                    testBody.getLocals().add(boolLocal);
                                    testUnits.add(Jimple.v().newAssignStmt(boolLocal, Jimple.v().newVirtualInvokeExpr(get, equalsToCall.makeRef(), StringConstant.v(equals))));

                                        if (!neq) {
                                            testUnits.add(Jimple.v().newReturnStmt(boolLocal));
                                        } else {
                                            ArrayList<Value> boolParms = new ArrayList<>();
                                            SootMethod v = Scene.v().getSootClass("java.lang.Boolean").getMethodByName("logicalXor");
                                            boolParms.add(boolLocal);
                                            Local one = Jimple.v().newLocal("one", BooleanType.v());
                                            testBody.getLocals().add(one);
                                            testUnits.add(Jimple.v().newAssignStmt(one, IntConstant.v(1)));
                                            boolParms.add(one);
                                            testUnits.add(Jimple.v().newAssignStmt(boolLocal, Jimple.v().newStaticInvokeExpr(v.makeRef(), boolParms)));
                                            testUnits.add(Jimple.v().newReturnStmt(boolLocal));
                                        }

                                }
                                else if (current.contains("<") || current.contains(">")) {
                                    int type;
                                    if (current.contains("<")){
                                        if (current.contains("<=")) {
                                            type = 0;
                                            data = current.split("<=");
                                        }
                                        else{
                                            type = 1;
                                            data = current.split("<");
                                        }
                                    }
                                    else {
                                        if (current.contains(">=")) {
                                            type = 2;
                                            data = current.split(">=");
                                        }
                                        else {
                                            type = 3;
                                            data = current.split(">");
                                        }
                                    }
                                    int paramNumb = 0;
                                    int temp = 0;
                                    for(String[] content : TableBank.getColumnContent(rowClassNames.get(cnt).split("Row")[0].toLowerCase())) {
                                        if(data[0].equals(content[0])){
                                            paramNumb = temp;
                                            break;
                                        }
                                        temp++;
                                    }
                                    String equals = data[1];
                                    int finalParamNumb = paramNumb;
                                    Local get = Jimple.v().newLocal("get", RefType.v("java.lang.String"));
                                    testBody.getLocals().add(get);
                                    SootMethod toCall = Scene.v().getSootClass("util.Row").getMethodByName("getParameter");
                                    testUnits.add(Jimple.v().newAssignStmt(get, Jimple.v().newVirtualInvokeExpr(rowLocal, toCall.makeRef(), IntConstant.v(finalParamNumb))));
                                    Local boolLocal = Jimple.v().newLocal("bool", BooleanType.v());
                                    testBody.getLocals().add(boolLocal);
                                    Local intLocal = Jimple.v().newLocal("intLoc", IntType.v());
                                    testBody.getLocals().add(intLocal);
                                    ArrayList<Local> tempList = new ArrayList<>();
                                    tempList.add(get);
                                    ArrayList<Type> tempTypes = new ArrayList<>();
                                    tempTypes.add(RefType.v("java.lang.String"));
                                    testUnits.add(Jimple.v().newAssignStmt(intLocal, Jimple.v().newStaticInvokeExpr(Scene.v().getSootClass("java.lang.Integer").getMethod("valueOf", tempTypes).makeRef(), tempList)));
                                    Local equalsInt = Jimple.v().newLocal("equalsInt", IntType.v());
                                    testBody.getLocals().add(equalsInt);
                                    testUnits.add(Jimple.v().newAssignStmt(equalsInt,IntConstant.v(Integer.valueOf(equals))));
                                    Unit end = Jimple.v().newReturnStmt(boolLocal);
                                    Unit un = Jimple.v().newAssignStmt(boolLocal, IntConstant.v(1));
                                    Unit un2 = Jimple.v().newAssignStmt(boolLocal, IntConstant.v(0));
                                    UnitBox box = Jimple.v().newStmtBox(un);
                                    Unit gotoUn = Jimple.v().newGotoStmt(end);
                                    testUnits.add(un2);

                                    if (type == 0){
                                        testUnits.add(Jimple.v().newIfStmt(Jimple.v().newLeExpr(intLocal,equalsInt), box));
                                        //afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) <= Integer.valueOf(data[1]));
                                    }
                                    else if (type == 1) {

                                        testUnits.add(Jimple.v().newIfStmt(Jimple.v().newLtExpr(intLocal,equalsInt), box));

                                    }
                                    else if (type == 2) {
                                        testUnits.add(Jimple.v().newIfStmt(Jimple.v().newGeExpr(intLocal,equalsInt), box));
                                        //afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) >= Integer.valueOf(data[1]));
                                    }
                                    else {
                                        testUnits.add(Jimple.v().newIfStmt(Jimple.v().newGtExpr(intLocal,equalsInt), box));
                                        //afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) > Integer.valueOf(data[1]));
                                    }
                                    testUnits.add(gotoUn);
                                    testUnits.add(un);
                                    testUnits.add(end);
                                }
                            predicateClass.validate();
                            ClassWriter.writeAsClassFile(predicateClass);
                        SootMethod toCallStream = Scene.v().getSootClass("java.util.Collection").getMethodByName("stream");
                        Local stream = Jimple.v().newLocal("stream" + nmb, RefType.v("java.util.stream.Stream"));
                        selectBody.getLocals().add(stream);
                        selectUnits.add(Jimple.v().newAssignStmt(stream, Jimple.v().newInterfaceInvokeExpr(tableList, toCallStream.makeRef())));
                        SootMethod toCallFilter = Scene.v().getSootClass("java.util.stream.Stream").getMethodByName("filter");
                        Local newStream = Jimple.v().newLocal("newStream" + nmb, RefType.v("java.util.stream.Stream"));
                        selectBody.getLocals().add(newStream);
                        Local filterParam = Jimple.v().newLocal("filterParm", RefType.v(predicateClass));
                        selectBody.getLocals().add(filterParam);
                        selectUnits.add(Jimple.v().newAssignStmt(filterParam, Jimple.v().newNewExpr(RefType.v(predicateClass))));
                        ArrayList<Local> filterList = new ArrayList<>();
                        filterList.add(filterParam);
                        selectUnits.add(Jimple.v().newAssignStmt(newStream, Jimple.v().newInterfaceInvokeExpr(stream, toCallFilter.makeRef(), filterList)));
                        Local iterator = Jimple.v().newLocal("iterator"+ nmb, RefType.v("java.util.Iterator"));
                        selectBody.getLocals().add(iterator);
                        SootMethod toCallIterator = Scene.v().getSootClass("java.util.stream.BaseStream").getMethodByName("iterator");
                        selectUnits.add(Jimple.v().newAssignStmt(iterator, Jimple.v().newInterfaceInvokeExpr(newStream, toCallIterator.makeRef())));
                        selectUnits.add(Jimple.v().newReturnStmt(iterator));
                    }
                        }

            cnt++;
        }


        units.add(Jimple.v().newReturnVoidStmt());
        //create class content
        ClassWriter.writeAsClassFile(connectionClass);
        ClassWriter.writeAsJimpleFile(connectionClass);
    }
}
