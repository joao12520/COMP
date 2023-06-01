package pt.up.fe.comp2023.jasmin;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import static org.specs.comp.ollir.CallType.invokestatic;

public class JasminInstructions {


    public JasminInstructions(){

    }

    String getJasminCode(UnaryOpInstruction instruction, JasminGenerator jasminGenerator) {
        StringBuilder jasminCode = new StringBuilder();
        OperationType opType = instruction.getOperation().getOpType();
        if (opType == OperationType.NOTB) {
            Operand op = (Operand) instruction.getOperand();
            jasminCode.append(jasminGenerator.jasminUtils.loadElement(op, jasminGenerator))
                    .append("\tifne Then").append(jasminGenerator.labelCounter).append('\n')
                    .append("\ticonst_1\n").append("\tgoto EndIf").append(jasminGenerator.labelCounter).append('\n')
                    .append("Then").append(jasminGenerator.labelCounter).append(":\n")
                    .append("\ticonst_0\n")
                    .append("EndIf").append(jasminGenerator.labelCounter).append(":\n");
            jasminGenerator.labelCounter++;
        }

        return jasminCode.toString();
    }

    String getJasminCode(BinaryOpInstruction instruction, JasminGenerator jasminGenerator) {
        StringBuilder jasminCode = new StringBuilder();

        Element leftOperand = instruction.getLeftOperand();
        Element rightOperand = instruction.getRightOperand();

        jasminCode.append(jasminGenerator.jasminUtils.loadElement(leftOperand, jasminGenerator))
                .append(jasminGenerator.jasminUtils.loadElement(rightOperand, jasminGenerator));

        OperationType opType = instruction.getOperation().getOpType();

        switch (opType) {
            case ADD:
                jasminCode.append("\tiadd\n");
                break;
            case SUB:
                jasminCode.append("\tisub\n");
                break;
            case MUL:
                jasminCode.append("\timul\n");
                break;
            case DIV:
                jasminCode.append("\tidiv\n");
                break;
            case LTH:
            case LTE:
            case GTH:
            case GTE:
            case EQ:
            case NEQ:
                String comparisonType = jasminGenerator.jasminUtils.compares(instruction.getOperation());
                jasminCode.append("\tisub\n\t")
                        .append(comparisonType)
                        .append(jasminGenerator.jasminUtils.compareLabels(jasminGenerator));
                jasminGenerator.stack.decStackCounter();
                break;
            case ANDB:
                jasminCode.append("\tiadd\n")
                        .append("\ticonst_2\n")
                        .append("\tisub\n")
                        .append("\tiflt ComparisonThen").append(jasminGenerator.labelCounter).append('\n')
                        .append("\ticonst_1\n")
                        .append("\tgoto ComparisonEndIf").append(jasminGenerator.labelCounter).append("\n")
                        .append("\tComparisonThen").append(jasminGenerator.labelCounter).append(":\n")
                        .append("\ticonst_0\n")
                        .append("\tComparisonEndIf").append(jasminGenerator.labelCounter++).append(":\n");
                break;
            default:
                throw new NotImplementedException(opType);
        }

        jasminGenerator.stack.decStackCounter();

        return jasminCode.toString();
    }

    String getJasminCode(GetFieldInstruction instruction, JasminGenerator jasminGenerator) {
        Operand firstOperand = (Operand) instruction.getFirstOperand();
        Operand secondOperand = (Operand) instruction.getSecondOperand();

        StringBuilder jasminCode = new StringBuilder();
        jasminCode.append(jasminGenerator.jasminUtils.loadElement(firstOperand, jasminGenerator))
                .append("\tgetfield ")
                .append(jasminGenerator.imports.getOrDefault(jasminGenerator.ollirClass.getClassName(), jasminGenerator.ollirClass.getClassName()))
                .append("/")
                .append(secondOperand.getName())
                .append(" ")
                .append(jasminGenerator.jasminUtils.getType(secondOperand.getType(), jasminGenerator))
                .append('\n');
        return jasminCode.toString();
    }

    String getJasminCode(PutFieldInstruction instruction, JasminGenerator jasminGenerator) {
        StringBuilder jasminCode = new StringBuilder();

        Operand instanceOperand = (Operand) instruction.getFirstOperand();
        Operand fieldOperand = (Operand) instruction.getSecondOperand();
        Element valueOperand = instruction.getThirdOperand();

        jasminCode.append(jasminGenerator.jasminUtils.loadElement(instanceOperand, jasminGenerator))
                .append(jasminGenerator.jasminUtils.loadElement(valueOperand, jasminGenerator))
                .append("\tputfield ");

        if (instanceOperand.getName().equals("this")) {
            jasminCode.append(jasminGenerator.ollirClass.getClassName());
        } else {
            jasminCode.append(instanceOperand.getName());
        }

        jasminCode.append("/").append(fieldOperand.getName())
                .append(" ").append(jasminGenerator.jasminUtils.getType(fieldOperand.getType(), jasminGenerator)).append('\n');

        jasminGenerator.stack.decStackCounter(2);

        return jasminCode.toString();
    }

    String getJasminCode(SingleOpInstruction instruction, JasminGenerator jasminGenerator) {
        return jasminGenerator.jasminUtils.loadElement(instruction.getSingleOperand(), jasminGenerator);
    }

    String getJasminCode(GotoInstruction instruction) {
        return "\tgoto " + instruction.getLabel() + "\n";
    }

    String getJasminCode(ReturnInstruction i, JasminGenerator jasminGenerator) {
        if (!i.hasReturnValue()) {
            return "\treturn\n";
        }

        ElementType returnType = i.getOperand().getType().getTypeOfElement();
        StringBuilder jasminCode = new StringBuilder(jasminGenerator.jasminUtils.loadElement(i.getOperand(), jasminGenerator));

        if (returnType == ElementType.INT32 || returnType == ElementType.BOOLEAN) {
            jasminCode.append("\tireturn\n");
        } else {
            jasminCode.append("\tareturn\n");
        }

        jasminGenerator.stack.decStackCounter();

        return jasminCode.toString();
    }

    String getJasminCode(CallInstruction instruction, JasminGenerator jasminGenerator) {
        switch (instruction.getInvocationType()) {
            case NEW:
                return parseNewCall(instruction, jasminGenerator);
            case invokespecial:
            case invokevirtual:
            case invokestatic:
                return getTypeInvoke(instruction, jasminGenerator);
            case ldc:
                return jasminGenerator.jasminUtils.loadElement(instruction.getFirstArg(), jasminGenerator);
            case arraylength:
                return jasminGenerator.jasminUtils.loadElement(instruction.getFirstArg(), jasminGenerator) + "\tarraylength\n";
            default:
                return "Not implemented yet";
        }
    }

    private String getIncrementation(BinaryOpInstruction instruction, Operand toIncrement, JasminGenerator jasminGenerator) {
        String toIncrementName = toIncrement.getName();
        Element leftElem = instruction.getLeftOperand();
        Element rightElem = instruction.getRightOperand();
        Integer literal = null;

        boolean leftIsLiteral = leftElem.isLiteral();
        boolean rightIsLiteral = rightElem.isLiteral();
        boolean leftIsOperand = leftElem instanceof Operand && ((Operand) leftElem).getName().equals(toIncrementName);
        boolean rightIsOperand = rightElem instanceof Operand && ((Operand) rightElem).getName().equals(toIncrementName);

        if (leftIsOperand && rightIsLiteral) {
            literal = Integer.parseInt(((LiteralElement) rightElem).getLiteral());
        } else if (leftIsLiteral && rightIsOperand) {
            literal = Integer.parseInt(((LiteralElement) leftElem).getLiteral());
        }

        if (literal != null && -128 < literal && literal < 513) {
            String varReg = String.valueOf(jasminGenerator.varTable.get(toIncrementName).getVirtualReg());
            return "\tiinc " + varReg + ' ' + literal + '\n';
        }

        return "";
    }

    private String parseNewCall(CallInstruction instruction, JasminGenerator jasminGenerator) {
        StringBuilder jasminCode = new StringBuilder();

        ElementType returnType = instruction.getReturnType().getTypeOfElement();
        if (returnType == ElementType.OBJECTREF) {
            jasminCode.append("\tnew ")
                    .append(((Operand) instruction.getFirstArg()).getName())
                    .append("\n");
            jasminGenerator.stack.incStackCounter();
        } else if (returnType == ElementType.ARRAYREF) {
            for (Element e : instruction.getListOfOperands()) {
                jasminCode.append(jasminGenerator.jasminUtils.loadElement(e, jasminGenerator));
            }
            jasminCode.append("\tnewarray ");
            if (((ArrayType) instruction.getReturnType()).getArrayType() == ElementType.INT32) {
                jasminCode.append("int\n");
            } else {
                return "Not implemented yet";
            }
        } else {
            return "Not implemented yet";
        }
        jasminGenerator.stack.decStackCounter(instruction.getListOfOperands().size() - 1);
        return jasminCode.toString();
    }

    String parseInvokeSpecialCall(CallInstruction instruction, JasminGenerator jasminGenerator) {

        StringBuilder jasminCode = new StringBuilder(jasminGenerator.jasminUtils.loadElement(instruction.getFirstArg(), jasminGenerator));

        for (Element e : instruction.getListOfOperands()) {
            jasminCode.append(jasminGenerator.jasminUtils.loadElement(e, jasminGenerator));
        }

        jasminCode.append("\tinvokespecial ")
                .append(
                        (instruction.getFirstArg().getType().getTypeOfElement() == ElementType.THIS) ?
                                jasminGenerator.imports.getOrDefault(jasminGenerator.ollirClass.getSuperClass(), "java/lang/Object")
                                :
                                jasminGenerator.imports.getOrDefault(
                                        ((ClassType) instruction.getFirstArg().getType()).getName(),
                                        ((ClassType) instruction.getFirstArg().getType()).getName())
                )
                .append("/<init>(");

        for (Element e : instruction.getListOfOperands())
            jasminCode.append(jasminGenerator.jasminUtils.getType(e.getType(), jasminGenerator));

        jasminCode.append(")").append(jasminGenerator.jasminUtils.getType(instruction.getReturnType(), jasminGenerator)).append("\n");

        return jasminCode.toString();
    }

    private String parseInvokeVirtualCall(CallInstruction instruction, JasminGenerator jasminGenerator) {
        StringBuilder jasminCode = new StringBuilder(jasminGenerator.jasminUtils.loadElement(instruction.getFirstArg(), jasminGenerator));

        for (Element e : instruction.getListOfOperands()) {
            jasminCode.append(jasminGenerator.jasminUtils.loadElement(e, jasminGenerator));
        }

        String className = (instruction.getFirstArg().getType().getTypeOfElement() == ElementType.THIS)
                ? jasminGenerator.ollirClass.getClassName()
                : jasminGenerator.imports.getOrDefault(((ClassType) instruction.getFirstArg().getType()).getName(),
                ((ClassType) instruction.getFirstArg().getType()).getName());

        jasminCode.append("\tinvokevirtual ")
                .append(className)
                .append("/")
                .append(((LiteralElement) instruction.getSecondArg()).getLiteral().replace("\"", ""))
                .append("(");

        for (Element e : instruction.getListOfOperands()) {
            jasminCode.append(jasminGenerator.jasminUtils.getType(e.getType(), jasminGenerator));
        }

        jasminCode.append(")")
                .append(jasminGenerator.jasminUtils.getType(instruction.getReturnType(), jasminGenerator))
                .append("\n");

        return jasminCode.toString();
    }

    private String parseInvokeStaticCall(CallInstruction instruction, JasminGenerator jasminGenerator) {
        StringBuilder jasminCode = new StringBuilder();

        for (Element e : instruction.getListOfOperands()) {
            jasminCode.append(jasminGenerator.jasminUtils.loadElement(e, jasminGenerator));
        }

        Operand firstArg = (Operand) instruction.getFirstArg();
        String firstArgName = firstArg.getName();
        String className = (firstArgName.equals("this")) ? jasminGenerator.ollirClass.getClassName() : firstArgName;
        LiteralElement secondArg = (LiteralElement) instruction.getSecondArg();
        String methodName = secondArg.getLiteral().replace("\"", "");

        jasminCode.append("\tinvokestatic ")
                .append(className)
                .append("/")
                .append(methodName)
                .append("(");

        for (Element e : instruction.getListOfOperands()) {
            jasminCode.append(jasminGenerator.jasminUtils.getType(e.getType(), jasminGenerator));
        }

        jasminCode.append(")")
                .append(jasminGenerator.jasminUtils.getType(instruction.getReturnType(), jasminGenerator))
                .append("\n");

        return jasminCode.toString();
    }

    private String getTypeInvoke(CallInstruction instruction, JasminGenerator jasminGenerator) {
        boolean hasReturnValue = instruction.getReturnType().getTypeOfElement() != ElementType.VOID;

        String jasminCode;

        switch (instruction.getInvocationType()) {
            case invokespecial:
                jasminCode = parseInvokeSpecialCall(instruction, jasminGenerator);
                break;
            case invokevirtual:
                jasminCode = parseInvokeVirtualCall(instruction, jasminGenerator);
                break;
            case invokestatic:
                jasminCode = parseInvokeStaticCall(instruction, jasminGenerator);
                break;
            default:
                throw new NotImplementedException(instruction.getInvocationType());
        }

        if (hasReturnValue && !jasminGenerator.insideAnAssignment) {
            jasminCode += "\tpop\n";
        }

        invokeInfluenceOnStack(instruction, jasminGenerator);

        return jasminCode;
    }

    String getJasminCode(AssignInstruction assignInstruction, JasminUtils jasminUtils, JasminGenerator jasminGenerator) {
        StringBuilder jasminCode = new StringBuilder();

        Operand destOperand = (Operand) assignInstruction.getDest();
        Instruction rhsInstruction = assignInstruction.getRhs();


        if (rhsInstruction.getInstType() == InstructionType.BINARYOPER) {
            BinaryOpInstruction binaryOp = ((BinaryOpInstruction) rhsInstruction);
            if (binaryOp.getOperation().getOpType() == OperationType.ADD) {
                String temp = getIncrementation(binaryOp, destOperand, jasminGenerator);
                if (!temp.isEmpty()) {
                    return temp;
                }
            }
        }

        Descriptor descriptor = jasminGenerator.varTable.get(destOperand.getName());

        if (descriptor.getVarType().getTypeOfElement() == ElementType.ARRAYREF
                && destOperand.getType().getTypeOfElement() != ElementType.ARRAYREF) {
            ArrayOperand arrayOperand = (ArrayOperand) destOperand;
            Element indexElement = arrayOperand.getIndexOperands().get(0);

            jasminCode.append(jasminUtils.loadDescriptor(descriptor, jasminGenerator))
                    .append(jasminUtils.loadElement(indexElement, jasminGenerator));
        }

        jasminGenerator.insideAnAssignment = true;
        String rhsCode = getJasminCode(rhsInstruction, jasminGenerator);
        jasminGenerator.insideAnAssignment = false;

        if (destOperand.getType() instanceof ArrayType) {
            ArrayType arrayType = (ArrayType) destOperand.getType();
            if (destOperand instanceof ArrayOperand) {
                ArrayOperand arrayOperand = (ArrayOperand) destOperand;
                jasminCode.append(jasminUtils.loadElement(arrayOperand, jasminGenerator))
                        .append(jasminUtils.loadElement(arrayOperand.getIndexOperands().get(0), jasminGenerator))
                        .append(rhsCode)
                        .append((arrayType.getArrayType() == ElementType.INT32 || arrayType.getArrayType() == ElementType.BOOLEAN)
                                ? "\tiastore\n"
                                : "\taastore\n"
                        );
                jasminGenerator.stack.decStackCounter(3);

                return jasminCode.toString();
            }
        }

        int virtualReg = descriptor.getVirtualReg();
        jasminCode.append(rhsCode);

        if (destOperand.getType().getTypeOfElement() == ElementType.INT32 || destOperand.getType().getTypeOfElement() == ElementType.BOOLEAN)
            jasminCode.append("\tistore");
        else {
            jasminCode.append("\tastore");
        }

        jasminGenerator.stack.decStackCounter();

        jasminCode.append((virtualReg <= 3) ? "_" : " ").append(virtualReg).append("\n");

        return jasminCode.toString();
    }

    String getJasminCode(CondBranchInstruction condBranchInstruction, JasminUtils jasminUtils, JasminGenerator jasminGenerator) {
        StringBuilder jasminCode = new StringBuilder();

        Instruction condition = condBranchInstruction.getCondition();
        if (condition instanceof OpInstruction) {
            OpInstruction opInst = (OpInstruction) condition;
            Element leftElem = opInst.getOperands().get(0);
            Element rightElem = opInst.getOperands().size() > 1 ? opInst.getOperands().get(1) : null;

            switch (opInst.getOperation().getOpType()) {
                case EQ:
                    jasminCode.append(jasminUtils.stageComparison(leftElem, rightElem, jasminGenerator))
                            .append("\tifeq ").append(condBranchInstruction.getLabel()).append('\n');
                    jasminGenerator.stack.decStackCounter();
                    break;
                case ANDB:
                    jasminCode.append(jasminUtils.loadElement(leftElem, jasminGenerator))
                            .append("\tifeq FalseAND").append(jasminGenerator.labelCounter).append('\n')
                            .append(jasminUtils.loadElement(rightElem,jasminGenerator))
                            .append("\tifeq FalseAND").append(jasminGenerator.labelCounter).append('\n')
                            .append("\tgoto ").append(condBranchInstruction.getLabel()).append('\n')
                            .append("\tFalseAND").append(jasminGenerator.labelCounter++).append(":\n");
                    break;
                case ORB:
                    jasminCode.append(jasminUtils.loadElement(leftElem, jasminGenerator))
                            .append("\tifne ").append(condBranchInstruction.getLabel()).append('\n')
                            .append(jasminUtils.loadElement(rightElem, jasminGenerator))
                            .append("\tifne ").append(condBranchInstruction.getLabel()).append('\n');
                    break;
                case NOTB:
                    jasminCode.append(jasminUtils.loadElement(leftElem, jasminGenerator))
                            .append("\tifeq ").append(condBranchInstruction.getLabel()).append('\n');
                    break;
                case NEQ:
                    jasminCode.append(jasminUtils.stageComparison(leftElem, rightElem, jasminGenerator))
                            .append("\tifne ").append(condBranchInstruction.getLabel()).append('\n');
                    jasminGenerator.stack.decStackCounter();
                    break;
                case LTH:
                case LTE:
                case GTH:
                case GTE:
                    jasminCode.append(jasminUtils.stageComparison(leftElem, rightElem, jasminGenerator))
                            .append('\t').append(jasminUtils.compares(opInst.getOperation())).append(' ')
                            .append(condBranchInstruction.getLabel()).append('\n');
                    jasminGenerator.stack.decStackCounter();
                    break;
                default:
                    throw new NotImplementedException(opInst.getOperation().getOpType());
            }
        } else if (condBranchInstruction instanceof SingleOpCondInstruction) {
            SingleOpCondInstruction singleOp = (SingleOpCondInstruction) condBranchInstruction;
            jasminCode.append(jasminUtils.loadElement(singleOp.getOperands().get(0), jasminGenerator))
                    .append("\tifne ").append(condBranchInstruction.getLabel()).append("\n");
        } else {
            throw new NotImplementedException(condBranchInstruction.toString());
        }

        jasminGenerator.stack.decStackCounter();

        return jasminCode.toString();
    }

    String getJasminCode(Instruction instruction, JasminGenerator jasminGenerator) {

        if (instruction instanceof CallInstruction) {
            return getJasminCode((CallInstruction) instruction, jasminGenerator);
        }

        if (instruction instanceof AssignInstruction) {
            return getJasminCode((AssignInstruction) instruction, jasminGenerator.jasminUtils, jasminGenerator);
        }

        if (instruction instanceof GotoInstruction) {
            return getJasminCode((GotoInstruction) instruction);
        }

        if (instruction instanceof ReturnInstruction) {
            jasminGenerator.hasReturnInstruction = true;
            return getJasminCode((ReturnInstruction) instruction, jasminGenerator);
        }

        if (instruction instanceof SingleOpInstruction) {
            return getJasminCode((SingleOpInstruction) instruction, jasminGenerator);
        }

        if (instruction instanceof PutFieldInstruction) {
            return getJasminCode((PutFieldInstruction) instruction, jasminGenerator);
        }

        if (instruction instanceof GetFieldInstruction) {
            return getJasminCode((GetFieldInstruction) instruction, jasminGenerator);
        }

        if (instruction instanceof BinaryOpInstruction) {
            return getJasminCode((BinaryOpInstruction) instruction, jasminGenerator);
        }

        if (instruction instanceof UnaryOpInstruction) {
            return getJasminCode((UnaryOpInstruction) instruction, jasminGenerator);
        }

        if (instruction instanceof CondBranchInstruction) {
            return getJasminCode((CondBranchInstruction) instruction, jasminGenerator.jasminUtils, jasminGenerator);
        }

        return "Not implemented yet";
    }

    String getConstructorCode(JasminGenerator jasminGenerator) {
        return SpecsIo.getResource("jasminTemplate/constructor.template")
                .replace("${SUPER_NAME}", jasminGenerator.superClass) + '\n';
    }

    void invokeInfluenceOnStack(CallInstruction instruction, JasminGenerator jasminGenerator) {
        boolean hasLoadedOperands = instruction.getListOfOperands().size() != 0;
        boolean hasReturnValue = instruction.getReturnType().getTypeOfElement() != ElementType.VOID;
        int decrementValue = 0;

        if (hasLoadedOperands && hasReturnValue) {
            decrementValue = instruction.getListOfOperands().size();
        } else if (hasLoadedOperands && !hasReturnValue) {
            decrementValue = instruction.getListOfOperands().size() + 1;
        } else if (!hasLoadedOperands && !hasReturnValue) {
            decrementValue = 1;
        }

        if (instruction.getInvocationType() == invokestatic) {
            decrementValue -= 1;
        }

        jasminGenerator.stack.decStackCounter(decrementValue);
    }
}
