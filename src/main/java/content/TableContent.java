package content;

import java.util.ArrayList;
import java.util.LinkedList;

public class TableContent implements ITableContent {

    private String tableName;
    private Boolean hasPreparedInsert;
    private final ArrayList<String> columns;
    private final ArrayList<String> queries;
    private final ArrayList<String[]> columnContent;


    public TableContent(String tableName) {
        setTableName(tableName);
        hasPreparedInsert = false;
        columns = new ArrayList<>();
        queries = new ArrayList<>();
        columnContent = new ArrayList<>();
    }


    public void addQuery(String query) {
        queries.add(query);
    }


    public String getTableName() {
        return tableName;
    }

    public void addPreparedInsert() {
        hasPreparedInsert = true;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setColumns(LinkedList<String> columns) {
        if (this.columns.isEmpty()) {
            for (String i : columns) {
                this.columns.add(i);
                columnContent.add(i.split(":"));
            }
        }
    }

    public Boolean hasPreparedInsert() {
        return hasPreparedInsert;
    }

    public ArrayList<String> getColumns() {
        return columns;
    }

    public ArrayList<String> getQueries() {
        return queries;
    }

    public ArrayList<String[]> getColumnContent() {
        return columnContent;
    }

    public void testPrint() {
        System.out.println("***********************************************************************");
        System.out.println("Table name: " + tableName);
        System.out.println("Table columns: " + columns.toString());
        System.out.println("Table queries: " + queries.toString());
        System.out.println("***********************************************************************");
    }


}
