package processor.statement;

import soot.Unit;

import java.util.ArrayList;

public class InsertStatement implements IStatement {

    private void increment() {
        if (currentCount < parameterCount) {
            currentCount++;
            if (currentCount == parameterCount){
                ready = true;
            }
        }
    }

    public void addParameter(int pos, String param, String type) {
        parameters[pos - 1] = param;
        parameterTypes[pos - 1] = type;
        increment();
    }

    public String getParameter(int pos) {
        return parameters[pos];
    }

    public String getType(int pos) {
        return parameterTypes[pos];
    }

    public String getLocalName() {
        return localName;
    }

    public String getTableName() {
        return tableName;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public boolean isReady() {
        return ready;
    }


    public Unit getPred() {
        return pred;
    }

    public void setPred(Unit pred) {
        this.pred = pred;
    }


    private String localName;
    private String tableName;
    private String[] parameters;
    private String[] parameterTypes;
    private int parameterCount;
    private int currentCount;
    private boolean ready;
    private Unit pred;

    public InsertStatement(String localName, String tableName, int parameterCount) {
        this.localName = localName;
        this.tableName = tableName;
        this.parameterCount = parameterCount;
        this.currentCount = 0;
        this.parameters = new String[parameterCount];
        this.parameterTypes = new String[parameterCount];
        this.pred = null;
    }


}
