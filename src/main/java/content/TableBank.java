package content;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TableBank {


    private static final LinkedList<TableContent> tables = new LinkedList<>();


    public static void addTable(TableContent table) {
        tables.add(table);
    }

    public static LinkedList<TableContent> getTables() {
        return tables;
    }

    public static boolean hasTable(String tableName) {
        for (TableContent i : tables) {
            if (i.getTableName().equals(tableName)) {
                return true;
            }
        }
        return false;
    }

    public static TableContent getTable(String tableName) {
        for (TableContent i : tables) {
            if (i.getTableName().equals(tableName)) {
                return i;
            }
        }
        return null;
    }

    public static void setColumns(String tableName, LinkedList<String> columns) {
        for (TableContent i : tables) {
            if (i.getTableName().equals(tableName)) {
                i.setColumns(columns);
                break;
            }
        }
    }

    public static ArrayList<String[]> getColumnContent(String tableName) {
        return getTable(tableName).getColumnContent();
    }

    public static void addPreparedInsertStatement(String tableName) {
        for (TableContent i : tables) {
            if (i.getTableName().equals(tableName)) {
                i.addPreparedInsert();
            }
        }
    }

    public static void addPreparedSelectQuery(String tableName, List<String> query) {
        for (TableContent i : tables) {
            if (i.getTableName().equals(tableName)) {
                if (query.stream().count() == 1) {
                    i.addQuery(query.get(0));
                }
                else {
                    int cnt = 0;
                    String output = "";
                    for (String k : query){
                        if(cnt == 0){
                            output = output + k;
                        }
                        else {
                            output = output + "," + k;
                        }
                        cnt++;
                    }
                    i.addQuery(output);
                }
            }
        }
    }

    public static void addSelectWhereResults(String tableName, ArrayList<String> whereResults) {
        for (TableContent i : tables) {
            if (i.getTableName().equals(tableName)) {
                i.addSelectWhere((ArrayList<String>) whereResults.clone());
            }
        }
    }

    public static void addPreparedDelete(String tableName, String where) {
        for (TableContent i : tables) {
            if( i.getTableName().equals(tableName)) {
                i.addDeleteWhere(where);
                break;
            }
        }
    }

    public static void addPreparedUpdate(String tableName, String where, ArrayList<String> assignments) {
        for (TableContent i : tables) {
            if( i.getTableName().equals(tableName)) {
                i.addUpdateWhere(where);

                if (assignments.stream().count() == 1) {
                    i.addUpdateAssignments(assignments.get(0));
                }
                else {
                    int cnt = 0;
                    String output = "";
                    for (String k : assignments){
                        if(cnt == 0){
                            output = output + k;
                        }
                        else {
                            output = output + "," + k;
                        }
                        cnt++;
                    }
                    i.addUpdateAssignments(output);
                }
                break;
            }
        }
    }
}
