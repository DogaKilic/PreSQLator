package content;

import java.util.ArrayList;
import java.util.LinkedList;

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

    public static void addPreparedSelectQuery(String tableName, String query) {
        for (TableContent i : tables) {
            if (i.getTableName().equals(tableName)) {
                i.addQuery(query);
            }
        }
    }
}
