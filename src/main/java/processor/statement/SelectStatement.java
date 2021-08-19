package processor.statement;

import soot.Unit;

public class SelectStatement implements IStatement{
    private String localName;
    private String tableName;
    private String queries;
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

    public SelectStatement(String localName, String tableName, String queries) {
        this.localName = localName;
        this.tableName = tableName;
        this.queries = queries;
        this.assignedLocal = "";
        this.pred = null;
    }
}
