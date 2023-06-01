package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.specs.util.SpecsIo;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;

public class JasminGenerator implements JasminBackend {
    public final JasminUtils jasminUtils = new JasminUtils();
    private final JasminInstructions jasminInstructions = new JasminInstructions();
    public ClassUnit ollirClass;
    private boolean hasConstructor;
    public boolean hasReturnInstruction;
    public boolean insideAnAssignment;
    public String superClass = "java/lang/Object";
    public HashMap<String, String> imports;
    public HashMap<String, Descriptor> varTable;
    public int labelCounter = 0;

    public StackManager stack;

    public JasminGenerator() {
    }

    @Override
    public JasminResult toJasmin(OllirResult result) {
        this.hasConstructor = false;
        this.ollirClass = result.getOllirClass();
        File file = new File("./jasmin/" + this.ollirClass.getClassName() + ".j");
        StringBuilder jasminCode = new StringBuilder();

        parseImports();

        jasminCode.append(parseHeader());
        jasminCode.append(parseFields());
        jasminCode.append(parseMethods(jasminUtils));

        if (!this.hasConstructor) {
            jasminCode.append(jasminInstructions.getConstructorCode(this));
        }


        SpecsIo.write(file, jasminCode.toString());

        return new JasminResult(result, jasminCode.toString(), Collections.emptyList());
    }

    private void parseImports() {
        this.imports = new HashMap<>();
        for (String importString : this.ollirClass.getImports()) {
            String[] parts = importString.split("\\.");
            String lastName = parts[parts.length - 1];
            String importPath = String.join("/", parts);
            this.imports.put(lastName, importPath);
        }
    }

    private String parseHeader() {
        StringBuilder jasminCode = new StringBuilder(".class ");

        AccessModifiers access = this.ollirClass.getClassAccessModifier();
        if (access != AccessModifiers.DEFAULT) {
            jasminCode.append(access.toString().toLowerCase()).append(" ");
        }

        jasminCode.append(this.ollirClass.getClassName()).append('\n').append(".super ");

        if (this.ollirClass.getSuperClass() != null) {
            this.superClass = this.imports.getOrDefault(this.ollirClass.getSuperClass(), this.ollirClass.getSuperClass());
            jasminCode.append(this.superClass);
        } else {
            jasminCode.append("java/lang/Object");
        }

        jasminCode.append("\n\n");
        return jasminCode.toString();
    }

    private String parseFields() {
        StringBuilder jasminCode = new StringBuilder();

        for (Field field : this.ollirClass.getFields()) {
            jasminCode.append(".field ");

            AccessModifiers access = field.getFieldAccessModifier();

            if (access != AccessModifiers.DEFAULT) {
                jasminCode.append(access.toString().toLowerCase()).append(" ");
            }

            if (field.isStaticField()) {
                jasminCode.append("static ");
            }

            if (field.isFinalField()) {
                jasminCode.append("final ");
            }

            jasminCode.append(field.getFieldName()).append(" ");
            jasminCode.append(jasminUtils.getType(field.getFieldType(), this));
            jasminCode.append('\n');
        }

        return jasminCode.toString();
    }

    private String parseMethods(JasminUtils jasminUtils) {
        StringBuilder jasminCode = new StringBuilder();
        for (Method method : this.ollirClass.getMethods()) {
            jasminCode.append(parseMethod(method, jasminUtils));
        }
        jasminCode.append('\n');

        return jasminCode.toString();

    }

    private String parseMethod(Method method, JasminUtils jasminUtils) {
        StringBuilder jasminCode = new StringBuilder(".method ");
        this.hasReturnInstruction = false;
        this.stack = new StackManager();

        AccessModifiers access = method.getMethodAccessModifier();
        if (access != AccessModifiers.DEFAULT) {
            jasminCode.append(access.toString().toLowerCase()).append(' ');
        }
        if (method.isStaticMethod()) {
            jasminCode.append("static ");
        }
        if (method.isFinalMethod()) {
            jasminCode.append("final ");
        }

        if (method.isConstructMethod()) {
            this.hasConstructor = true;
            jasminCode.append("<init>(");
        } else {
            jasminCode.append(method.getMethodName()).append('(');
        }

        for (Element param : method.getParams()) {
            jasminCode.append(this.jasminUtils.getType(param.getType(), this));
        }
        jasminCode.append(')')
                .append(this.jasminUtils.getType(method.getReturnType(), this))
                .append('\n');

        method.buildVarTable();
        this.varTable = method.getVarTable();

        /*jasminCode.append("\t.limit stack ")
                .append(this.stack.getStackLimit())
                .append("\n\t.limit locals ")
                .append(jasminUtils.getLocals(this))
                .append("\n");*/

        jasminCode.append("\t.limit stack ${STACK_COUNTER}\n\t.limit locals ")
                .append(jasminUtils.getLocals(this)).append("\n");

        for (Instruction inst : method.getInstructions()) {
            jasminCode.append(jasminUtils.getLabels(method.getLabels(inst)))
                    .append(jasminInstructions.getJasminCode(inst, this));
        }

        if (!this.hasReturnInstruction) {
            jasminCode.append("\treturn\n");
        }
        jasminCode.append(".end method\n\n");

        return jasminCode.toString().replace("${STACK_COUNTER}", Integer.toString(this.stack.getStackLimit()));
    }

}
