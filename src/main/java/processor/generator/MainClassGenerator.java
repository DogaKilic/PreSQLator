package processor.generator;

import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.util.Chain;
import util.ClassWriter;

import java.util.Arrays;
import java.util.List;

public class MainClassGenerator extends ClassGenerator {
    public void generateClass(SootClass oldClass) {
        SootClass processedClass = new SootClass(oldClass.getName() + "Processed", Modifier.PUBLIC);
        processedClass.setSuperclass(Scene.v().getSootClass("java.lang.Object"));
        Scene.v().addClass(processedClass);

        List<SootMethod> methodList = oldClass.getMethods();
        System.out.println(oldClass.getFields().toString());
        for(SootField field : oldClass.getFields()){
            field.setDeclared(false);
            processedClass.addField(field);
            field.setDeclared(true);
        }

        for(SootMethod method : methodList) {
            method.setDeclared(false);
            SootMethod newMethod = new SootMethod(method.getName(), method.getParameterTypes(), method.getReturnType(), method.getModifiers());


            Body activeBody = method.retrieveActiveBody();
            UnitPatchingChain units = activeBody.getUnits();
            processedClass.addMethod(method);
            method.setDeclared(true);
        }

        /************************************************
        //fill final class
        SootMethod main = new SootMethod("main",
                Arrays.asList(new Type[]{ArrayType.v(RefType.v("java.lang.String"), 1)}),
                VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        processedClass.addMethod(main);
        JimpleBody body = Jimple.v().newBody(main);
        main.setActiveBody(body);
        Chain units = body.getUnits();
        units.add(Jimple.v().newReturnVoidStmt());
        ****************************************************/

        ClassWriter.writeAsClassFile(processedClass);
        ClassWriter.writeAsJimpleFile(processedClass);

    }
}
