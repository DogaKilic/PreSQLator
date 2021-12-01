package util;

import edu.kit.joana.ui.annotations.EntryPoint;

import java.util.ArrayList;

public abstract class Row {
    public ArrayList<String> parameterList = new ArrayList<>();
    private void addParameter(java.lang.String i) {
        parameterList.add(i);
    }

    public String getParameter(int i) {
        return parameterList.get(i);
    }

    public void setParameter(int i, java.lang.String o) {
        parameterList.set(i, o);
    }
}
