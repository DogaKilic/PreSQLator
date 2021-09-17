package util;

import content.TableBank;
import processor.generator.MainClassGenerator;
import soot.*;
import soot.jimple.Jimple;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;


public class ConnectionHelper {

    public Iterator<Row> processQuery(ArrayList<Row> list, String className, String query, String where) {
        if (where.equals("")) {
            if (query.equals("*")) {
                return list.iterator();
            } else {
                SootClass listClass = Scene.v().getSootClass(className);
                String[] queries = query.split(",");
                ArrayList<Integer> toRemove = new ArrayList<>();
                ArrayList<String> columnNames = new ArrayList<>();
                for(SootField i : listClass.getFields()){
                    columnNames.add(i.getName());
                }
                for (String s : queries) {
                    for(int i = 0; i < columnNames.size(); i++) {
                        if(columnNames.get(i).equals(s)){
                            toRemove.add(i);
                            break;
                        }
                    }
                }
                for (Integer i : toRemove) {
                    for (Row r : list) {
                        r.setParameter(i, null);
                    }
                }
                return list.iterator();
            }
        }
        else{
            ArrayList currentList = (ArrayList) list.clone();
            SootClass rowClass = Scene.v().getSootClass(className);
            ArrayList<String> columnNames = new ArrayList<>();
            for(SootField i : rowClass.getFields()){
                columnNames.add(i.getName());
            }
            String[] wheres = where.split(":");
            String[] queries = query.split(",");
            Chain<SootField> fieldChain = rowClass.getFields();
            Stream<Row> afterWhere = list.stream();
            for (int i = 0; i < wheres.length; i++) {
                String current = wheres[i];

                if (current.contains("=") || current.contains("!=") || current.contains("<>")) {
                    boolean eq = current.contains("=");
                    int paramNumb = 0;
                    String[] data = current.split("=");
                    String equals;
                    int cnt = 0;
                    for(String[] content : TableBank.getColumnContent(className.split("Row")[0].toLowerCase())) {
                        if(data[0].equals(content[0])){
                            paramNumb = cnt;
                            break;
                        }
                        cnt++;
                    }
                    if(data[1].contains("\'")) {
                        equals = data[1].replaceAll("\'","");
                        int finalParamNumb = paramNumb;

                        if(eq) {

                            afterWhere = afterWhere.filter(x -> x.getParameter(finalParamNumb).equals(equals));
                        }
                        else {
                            afterWhere = afterWhere.filter(x -> !x.getParameter(finalParamNumb).equals(equals));
                        }
                    }
                    else{
                        equals = data[1];
                        int finalParamNumb = paramNumb;
                        if(eq) {
                            //afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) == Integer.valueOf(equals));
                        }
                        else {
                               // afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) != Integer.valueOf(equals));
                            }
                    }

                }
                else if (current.contains("<") || current.contains(">")) {
                    int type;
                    String[] data;
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
                    int cnt = 0;
                    for(String[] content : TableBank.getColumnContent(className.split("Row")[0].toLowerCase())) {
                        if(data[0].equals(content[0])){
                            paramNumb = cnt;
                            break;
                        }
                        cnt++;
                    }
                    int finalParamNumb = paramNumb;
                    if (type == 0){
                        //afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) <= Integer.valueOf(data[1]));
                    }
                    else if (type == 1) {
                        //afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) < Integer.valueOf(data[1]));
                    }
                    else if (type == 2) {
                        //afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) >= Integer.valueOf(data[1]));
                    }
                    else {
                        //afterWhere = afterWhere.filter(x -> ((int) x.getParameter(finalParamNumb)) > Integer.valueOf(data[1]));
                    }

                }
            }
            if (query == "*") {
                return afterWhere.iterator();
            } else {
                ArrayList<Integer> toRemove = new ArrayList<>();
                for (String s : queries) {
                    for(int i = 0; i < columnNames.size(); i++) {
                        if(columnNames.get(i).equals(s)){
                            toRemove.add(i);
                            break;
                        }
                    }
                }
                for (Integer i : toRemove) {
                    for (Row r : (Row[]) afterWhere.toArray()) {
                        r.setParameter(i, null);
                    }
                }
                return afterWhere.iterator();
            }
        }
    }
}

