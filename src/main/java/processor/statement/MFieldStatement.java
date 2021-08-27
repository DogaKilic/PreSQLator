package processor.statement;

import soot.Unit;

public class MFieldStatement implements IStatement{
    public void setFieldLocal(String fieldLocal) {
        this.fieldLocal = fieldLocal;
    }

    private String fieldLocal;
    private String assignedLocal;

    public String getLocalType() {
        return localType;
    }

    public void setLocalType(String localType) {
        this.localType = localType;
    }

    private String localType;

    public String getValueLocal() {
        return valueLocal;
    }

    public void setValueLocal(String valueLocal) {
        this.valueLocal = valueLocal;
    }

    private String valueLocal;

    public String getField() {
        return field;
    }

    public int getType() {
        return type;
    }

    private String field;
    private int type;
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

    public String getFieldLocal() {
        return fieldLocal;
    }


    public MFieldStatement(String assignedLocal, String field, int type) {
        this.type = type;
        this.field = field;
        this.assignedLocal = assignedLocal;
        this.pred = null;
    }
}
