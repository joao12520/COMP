package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;

public class JasminUtils {
    public JasminUtils() {
    }

    String getLabels(List<String> labels) {
        StringBuilder jasminCode = new StringBuilder();

        for (String label : labels) {
            jasminCode.append("\t").append(label).append(":\n");
        }

        return jasminCode.toString();
    }

    public String compareLabels(JasminGenerator jasminGenerator) {
        jasminGenerator.stack.incStackCounter();
        String beginThenLabel = " ComparisonThen" + jasminGenerator.labelCounter;
        String thenLabel = "ComparisonThen" + jasminGenerator.labelCounter;
        String midEndLabel = "ComparisonEndIf" + jasminGenerator.labelCounter;
        String endLabel = "ComparisonEndIf" + jasminGenerator.labelCounter++;
        return  beginThenLabel + "\n" +
                "\ticonst_0\n" +
                "\tgoto " + midEndLabel + '\n' +
                thenLabel + ":\n" +
                "\ticonst_1\n" +
                endLabel + ":\n";
    }

    public String loadDescriptor(Descriptor descriptor, JasminGenerator jasminGenerator) {
        jasminGenerator.stack.incStackCounter();
        ElementType elementType = descriptor.getVarType().getTypeOfElement();
        if (elementType == ElementType.THIS) {
            return "\taload_0\n";
        }

        String loadInstruction = (elementType == ElementType.INT32 || elementType == ElementType.BOOLEAN) ? "iload" : "aload";
        String regString = (descriptor.getVirtualReg() <= 3) ? "_" + descriptor.getVirtualReg() : " " + descriptor.getVirtualReg();
        return "\t" + loadInstruction + regString + "\n";
    }

    String loadElement(Element e, JasminGenerator jasminGenerator) {
        if (e.isLiteral()) {
            return loadLiteralElement((LiteralElement) e, jasminGenerator);
        }

        Operand op = (Operand) e;
        Descriptor d = jasminGenerator.varTable.get(op.getName());
        if (d == null) {
            throw new NotImplementedException(op.getName());
        }

        if (e.getType().getTypeOfElement() != ElementType.ARRAYREF
                && d.getVarType().getTypeOfElement() == ElementType.ARRAYREF) {
            ArrayOperand arrayOp = (ArrayOperand) e;
            Element index = arrayOp.getIndexOperands().get(0);

            jasminGenerator.stack.decStackCounter();
            return loadDescriptor(d, jasminGenerator) + loadElement(index, jasminGenerator) + "\tiaload\n";
        }

        return loadDescriptor(d, jasminGenerator);
    }

    private String loadLiteralElement(LiteralElement element, JasminGenerator jasminGenerator) {
        jasminGenerator.stack.incStackCounter();

        int literal;
        try {
            literal = Integer.parseInt(element.getLiteral());
        } catch (NumberFormatException e) {
            return "\tldc " + element.getLiteral() + '\n';
        }

        ElementType elementType = element.getType().getTypeOfElement();
        String jasminCode = "\t";
        switch (elementType) {
            case INT32, BOOLEAN:
                if (literal <= 5 && literal >= -1) {
                    jasminCode += "iconst_" + literal + "\n";
                } else if (literal >= Byte.MIN_VALUE && literal <= Byte.MAX_VALUE) {
                    jasminCode += "bipush " + literal + "\n";
                } else if (literal >= Short.MIN_VALUE && literal <= Short.MAX_VALUE) {
                    jasminCode += "sipush " + literal + "\n";
                } else {
                    jasminCode += "ldc " + literal + "\n";
                }
                break;
            case STRING:
                jasminCode += "ldc " + element.getLiteral() + "\n";
                break;
            default:
                throw new NotImplementedException(element.getType());
        }

        return jasminCode;
    }

    String getType(Type type, JasminGenerator jasminGenerator) {
        ElementType elementType = type.getTypeOfElement();

        switch (elementType) {
            case INT32:
                return "I";
            case BOOLEAN:
                return "Z";
            case STRING:
                return "Ljava/lang/String;";
            case VOID:
                return "V";
            case ARRAYREF:
                return "[" + getType(new Type(((ArrayType) type).getArrayType()), jasminGenerator);
            case OBJECTREF:
                String className = ((ClassType) type).getName();
                String importedName = jasminGenerator.imports.getOrDefault(className, className);
                return "L" + importedName + ";";
            default:
                throw new NotImplementedException(type);
        }
    }

    String stageComparison(Element first, Element second, JasminGenerator jasminGenerator) {
        return loadElement(first, jasminGenerator) + loadElement(second, jasminGenerator) + "\tisub\n";
    }

    String compares(Operation operation) {
        String comparison;
        switch (operation.getOpType()) {
            case GTE:
                comparison = "ifge";
                break;
            case GTH:
                comparison = "ifgt";
                break;
            case LTE:
                comparison = "ifle";
                break;
            case LTH:
                comparison = "iflt";
                break;
            case EQ:
                comparison = "ifeq";
                break;
            case NOTB:
            case NEQ:
                comparison = "ifne";
                break;
            default:
                throw new NotImplementedException(operation.getOpType());
        }
        return comparison;
    }

    int getLocals(JasminGenerator jasminGenerator) {
        return jasminGenerator.varTable.values()
                .stream()
                .mapToInt(Descriptor::getVirtualReg)
                .max()
                .orElse(0) + 1;
    }

}
