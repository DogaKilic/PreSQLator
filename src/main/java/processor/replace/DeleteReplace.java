package processor.replace;

import soot.Unit;

import java.util.ArrayList;

public class DeleteReplace implements IReplace {
    private static ArrayList<String> count = new ArrayList<>();


    public int getLocalCount() {
        return localCount;
    }

    private int localCount;
    private String localName;
    private String tableName;



    private String assignedLocal;
    private Unit pred;

    public String getAssignedLocal() {
        return assignedLocal;
    }

    public void setAssignedLocal(String assignedLocal) {
        this.assignedLocal = assignedLocal;
    }

    public Unit getPred() {
        return pred;
    }

    public void setPred(Unit pred) {
        this.pred = pred;
    }

    public String getLocalName() {
        return localName;
    }

    public String getTableName() {
        return tableName;
    }

    public DeleteReplace(String localName, String tableName) {
        this.localName = localName;
        this.tableName = tableName;
        this.assignedLocal = "";
        this.pred = null;
        this.localCount = count.stream().filter(x -> x.equals(tableName)).toArray().length;
        count.add(tableName);
    }
}
