package processor.statement;

public class InsertStatement implements IStatement {

    public void increment() {
        if (currentCount < parameterCount) {
            currentCount++;
            if (currentCount == parameterCount){
                ready = true;
            }
        }
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
    private int parameterCount;
    private int currentCount;
    private boolean ready;

    public InsertStatement(String localName, String tableName, int parameterCount) {
        this.localName = localName;
        this.tableName = tableName;
        this.parameterCount = parameterCount;
        this.currentCount = 0;
    }


}
