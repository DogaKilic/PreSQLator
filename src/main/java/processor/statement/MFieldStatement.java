package processor.statement;

import soot.Unit;
import soot.Value;
import soot.jimple.Constant;

import java.util.ArrayList;

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
    private ArrayList<Value> parameters;

    public void addParameter(Value value){
        parameters.add(value);
    }

    public int getParameterCount(){
        return parameters.size();
    }

    public ArrayList<Value> getParameters() {
        return parameters;
    }

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
        this.parameters = new ArrayList<>();
    }
}
