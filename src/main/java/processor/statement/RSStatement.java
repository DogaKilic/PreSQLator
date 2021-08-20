package processor.statement;

import soot.Unit;

public class RSStatement {
    public String getLocalName() {
        return localName;
    }

    private String localName;

    public String getAssignedLocalName() {
        return assignedLocalName;
    }

    public void setAssignedLocalName(String assignedLocalName) {
        this.assignedLocalName = assignedLocalName;
    }

    private String assignedLocalName;
    private Unit pred;

    public Unit getPred() {
        return pred;
    }

    public void setPred(Unit pred) {
        this.pred = pred;
    }

    public RSStatement (String localName) {
        this.localName = localName;
        this.pred = null;
    }
}
