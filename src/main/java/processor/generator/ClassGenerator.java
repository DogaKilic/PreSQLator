package processor.generator;

import soot.IntType;
import soot.LongType;
import soot.RefType;
import soot.Type;

abstract class ClassGenerator {


    protected Type getType(String input) {
        switch (input) {
            case "integer":
                return IntType.v();
            case "string":
                return RefType.v("java.lang.String");
            default:
                break;
        }
        return LongType.v();
    }
}
