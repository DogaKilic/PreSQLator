package processor.generator;

import soot.*;

abstract class ClassGenerator {


    protected Type getType(String input) {
        switch (input) {
            case "integer":
                return IntType.v();
            case "string":
                return RefType.v("java.lang.String");
            case "byte":
                return ByteType.v();
            case "long":
                return  LongType.v();
            case "double":
                return  DoubleType.v();
            case "boolean":
                return BooleanType.v();
            case "float":
                return FloatType.v();
            case "short":
                return ShortType.v();
            case "char":
                return CharType.v();
            default:
                break;
        }
        return LongType.v();
    }
}
