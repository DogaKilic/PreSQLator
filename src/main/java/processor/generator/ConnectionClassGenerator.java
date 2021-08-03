package processor.generator;

import content.TableContent;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;
import util.ClassWriter;

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
        //add insertStatement methods
        cnt = 0;
        for (TableContent i : contents) {
            if (i.hasPreparedInsert()) {
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
        }
        //add selectStatement methods
        cnt = 0;
        int nmb = 0;
        for (TableContent i : contents) {
            //for(String query : i.getQueries()) {
                SootMethod select = new SootMethod(i.getTableName() + "SelectStatement" , null, RefType.v("Iterator<" + rowClassNames.get(cnt) + ">"), Modifier.PUBLIC);
                connectionClass.addMethod(select);
                JimpleBody selectBody = Jimple.v().newBody(select);
                select.setActiveBody(selectBody);
                Chain selectUnits = selectBody.getUnits();
                Local selectRef = Jimple.v().newLocal("thisRef" + nmb, connectionClass.getType());
                selectBody.getLocals().add(selectRef);
                selectUnits.add(Jimple.v().newIdentityStmt(selectRef, Jimple.v().newThisRef(connectionClass.getType())));
                Local tableClass = Jimple.v().newLocal("tableClass" + nmb, RefType.v("ArrayList<>"));
                selectBody.getLocals().add(tableClass);
                Local table = Jimple.v().newLocal("table" + nmb, RefType.v(tableClassNames.get(cnt)));
                selectBody.getLocals().add(table);
                selectUnits.add(Jimple.v().newAssignStmt(table, Jimple.v().newInstanceFieldRef(selectRef, connectionClass.getFieldByName(i.getTableName() + "Table").makeRef())));
                selectUnits.add(Jimple.v().newAssignStmt(tableClass, Jimple.v().newInstanceFieldRef(table, Scene.v().getSootClass(tableClassNames.get(cnt)).getFields().getFirst().makeRef())));
                SootMethod toCall = Scene.v().getSootClass("java.util.ArrayList").getMethodByName("iterator");
                    Local iterator = Jimple.v().newLocal("iterator" + nmb, RefType.v("Iterator<" + rowClassNames.get(cnt) + ">"));
                    selectBody.getLocals().add(iterator);
                    selectUnits.add(Jimple.v().newAssignStmt(iterator, Jimple.v().newVirtualInvokeExpr(tableClass, toCall.makeRef())));
                    selectUnits.add(Jimple.v().newReturnStmt(iterator));
                /***********************************************************
                else {
                    System.out.println(query);
                    Local stream = Jimple.v().newLocal("Stream" + nmb, RefType.v("Stream<" + rowClassNames.get(cnt) + ">"));
                    selectBody.getLocals().add(stream);
                    SootMethod toCallStream = Scene.v().getSootClass("java.util.Collection").getMethodByName("stream");
                    selectUnits.add(Jimple.v().newAssignStmt(stream, Jimple.v().newInterfaceInvokeExpr(tableClass, toCallStream.makeRef())));

                    Local map = Jimple.v().newLocal("Stream" + nmb, RefType.v("Stream<" + rowClassNames.get(cnt) + ">"));
                    selectBody.getLocals().add(map);
                    SootMethod toCallMap = Scene.v().getSootClass("java.util.stream.Stream").getMethodByName("map");
                    SootMethod toCallConcat = Scene.v().getSootClass("java.lang.String").getMethodByName("concat");
                    Stream<String> toDeleteStream = i.getColumnContent().stream().filter(p -> !Arrays.stream(query.split(",")).anyMatch(y -> y.equals(p[0]))).map(z -> z[0]);
                    List<String> results = toDeleteStream.collect(Collectors.toList());
                    for(String result : results) {
                        Local toDeleteLocal = Jimple.v().newLocal("toDelete" + nmb, RefType.v("java.lang.String"));
                        selectBody.getLocals().add(toDeleteLocal);
                        selectUnits.add(Jimple.v().newAssignStmt(toDeleteLocal, Jimple.v().newNewExpr(Scene.v().getRefType("java.lang.String"))));
                        System.out.println("java.lang.String(\"(i -> i." + result+ " = null)\")");
                        //selectUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(toDeleteLocal, toCallConcat.makeRef(), "(i -> i." + result+ " = null")));

                        //selectUnits.add(Jimple.v().newAssignStmt(toDeleteLocal, "(i -> i." + result+ " = null"));
                        //selectUnits.add(Jimple.v().newInvokeStmt(Jimple.v().newInterfaceInvokeExpr(stream, toCallMap.makeRef(), "(i -> i." + result+ " = null")));
                    }






                    Local iterator = Jimple.v().newLocal("iterator" + nmb, RefType.v("Iterator<" + rowClassNames.get(cnt) + ">"));
                    selectBody.getLocals().add(iterator);
                    selectUnits.add(Jimple.v().newAssignStmt(iterator, Jimple.v().newVirtualInvokeExpr(tableClass, toCall.makeRef())));
                    selectUnits.add(Jimple.v().newReturnStmt(iterator));
                }
                selectBody.validate();
                nmb++;
                 }
                 ****************************************/

        }
            cnt++;



        units.add(Jimple.v().newReturnVoidStmt());
        //create class content
        ClassWriter.writeAsClassFile(connectionClass);
        ClassWriter.writeAsJimpleFile(connectionClass);
    }
}
