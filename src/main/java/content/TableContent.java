package content;

import java.util.ArrayList;
import java.util.LinkedList;

public class TableContent implements ITableContent {

    private String tableName;
    private Boolean hasPreparedInsert;
    private final ArrayList<String> columns;
    private final ArrayList<String> selects;
    private final ArrayList<ArrayList<String>> selectWheres;
    private final ArrayList<String> deleteWheres;
    private final ArrayList<String[]> columnContent;


    public TableContent(String tableName) {
        setTableName(tableName);
        hasPreparedInsert = false;
        columns = new ArrayList<>();
        selects = new ArrayList<>();
        deleteWheres = new ArrayList<>();
        selectWheres = new ArrayList<>();
        columnContent = new ArrayList<>();
    }


    public void addQuery(String query) {
        selects.add(query);
    }

    public void addSelectWhere(ArrayList<String> where) { selectWheres.add(where); }


    public void addDeleteWhere(String where) { deleteWheres.add(where);}

    public int getDeleteWheresSize() {
        return deleteWheres.size();
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


    public String getSelect(int i) {
        return selects.get(i);
    }


    public ArrayList<String> getSelectWheres(int i) { return selectWheres.get(i); }

    public String getDeleteWhere(int i) {return deleteWheres.get(i);}

    public int getSelectWheresSize() { return selectWheres.size();}

    public ArrayList<String[]> getColumnContent() {
        return columnContent;
    }

    public void testPrint() {
        System.out.println("***********************************************************************");
        System.out.println("Table name: " + tableName);
        System.out.println("Table columns: " + columns.toString());
        System.out.println("Table queries: " + selects.toString());
        System.out.println("Delete statements: " + deleteWheres.toString());
        System.out.println("***********************************************************************");
    }


}
