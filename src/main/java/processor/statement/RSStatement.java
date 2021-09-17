package processor.statement;

import soot.Unit;

import java.util.ArrayList;

public class RSStatement {
    public String getLocalName() {
        return localName;
    }

    private String localName;

    public String getAssignedLocalNameNext() {
        return assignedLocalNameNext;
    }

    public void setAssignedLocalNameNext(String assignedLocalName) {
        this.assignedLocalNameNext = assignedLocalName;
    }

    private String assignedLocalNameNext;

    public String getTableName() {
        return this.tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    private String tableName;
    private ArrayList<String> assignedLocalNameGet;
    private Unit predNext;
    private ArrayList<Unit> predGet;
    private ArrayList<String> paramsGet;
    private ArrayList<Boolean> nextCalled;

    public void addAssignedLocalNameGet (String assignedLocalName) { this.assignedLocalNameGet.add(assignedLocalName); }

    public String getAssignedLocalNameGet (int i) {return this.assignedLocalNameGet.get(i); }

    public void addParamsGet (String param) { this.paramsGet.add(param);}

    public String getParamsGet (int i) { return this.paramsGet.get(i);}

    public void addPredGet (Unit pred) { this.predGet.add(pred);}

    public Unit getPredGet (int i) { return this.predGet.get(i);}

    public Boolean nextCalled (int i ) { return this.nextCalled.get(i);}

    public void addNextCalled() {this.nextCalled.add(false);}

    public int getGetSize() { return this.assignedLocalNameGet.size();}

    public Unit getPredNext() {
        return predNext;
    }

    public void setPredNext(Unit pred) {
        this.predNext = pred;
    }

    public RSStatement (String localName) {
        this.assignedLocalNameGet = new ArrayList<>();
        this.nextCalled = new ArrayList<>();
        this.paramsGet = new ArrayList<>();
        this.predGet = new ArrayList<>();
        this.assignedLocalNameNext = "";
        this.localName = localName;
        this.predNext = null;
    }
}
