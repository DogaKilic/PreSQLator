package processor.statement;

import java.util.ArrayList;

public class InsertStatement implements IStatement {

    public void increment() {
        if (currentCount < parameterCount) {
            currentCount++;
            if (currentCount == parameterCount){
                ready = true;
            }
        }
    }

    public void addParameter(int pos, String param) {
        parameters[pos] = param;
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

    private String localName;
    private String tableName;
    private String[] parameters;
    private int parameterCount;
    private int currentCount;
    private boolean ready;

    public InsertStatement(String localName, String tableName, int parameterCount) {
        this.localName = localName;
        this.tableName = tableName;
        this.parameterCount = parameterCount;
        this.currentCount = 0;
        this.parameters = new String[parameterCount];
    }


}
