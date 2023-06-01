package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;

public class AnalysisUtils {

    static String currentMethod = "#UNKNOWN";
    static boolean isMethodStatic = false;


    public static Type getType(JmmNode jmmNode) {
        boolean isArray = jmmNode.getAttributes().contains("isArray") && jmmNode.get("isArray").equals("true");
        String nodeKind = jmmNode.getKind();
        Type type;

        switch (nodeKind) {
            case "Boolean":
                type = new Type("boolean", isArray);
                return type;
            case "Identifier":
                type = new Type("#UNKNOWN", isArray);
                return type;
            case "Integer":
                type = new Type("int", isArray);
                return type;
            case "NewArray":
                JmmNode arrayTypeNode = jmmNode.getJmmChild(0);
                type = getType(arrayTypeNode);
                return new Type(type.getName(), true);
            case "ArrayAccessExpr":
                JmmNode arrayNode = jmmNode.getJmmChild(0);
                type = getType(arrayNode);
                return new Type(type.getName(), false); // Array element type
            case "BinaryOp":
                JmmNode leftOperand = jmmNode.getJmmChild(0);
                JmmNode rightOperand = jmmNode.getJmmChild(1);
                Type leftType = getType(leftOperand);
                Type rightType = getType(rightOperand);

                // Determine the resulting type based on the actual operation type
                if (leftType.getName().equals("int") && rightType.getName().equals("int")) {
                    type = new Type("int", false); // Change isArray to false
                } else if (leftType.getName().equals("boolean") && rightType.getName().equals("boolean")) {
                    type = new Type("boolean", false); // Change isArray to false
                } else if(leftOperand.getKind().equals("BinaryOp")) {
                    type = new Type(rightType.getName(), false); // Change isArray to false
                }
                else {
                    // Handle other cases if necessary
                    type = new Type("#UNKNOWN", isArray);
                }
                return type;
            default:
                if (jmmNode.getOptional("value").isPresent()) {
                    type = new Type(jmmNode.get("value"), isArray);
                    return type;
                } else if (jmmNode.getOptional("type").isPresent()) {
                    type = new Type(jmmNode.get("type"), isArray);
                    return type;
                }

                return new Type(jmmNode.get("value"), isArray);
        }
    }



    public static Symbol getSymbol(JmmNode jmmNode) {
        String name = jmmNode.get("value");
        Type type = getType(jmmNode.getJmmChild(0));
        return new Symbol(type, name);
    }


    public static boolean isVariableDeclared(String varName, SymbolTableInit symbolTable) {
        // Check if the variable exists in the fields
        for (Symbol field : symbolTable.getFields()) {
            if (field.getName().equals(varName)) {
                return true;
            }
        }

        // Check if the variable exists in the parameters or local variables in the current method
        String methodName = AnalysisUtils.currentMethod;
        if(methodName == "#UNKNOWN") return false;
        var methodParameters = symbolTable.getParameters(methodName);
        if(methodParameters == null) return false;
        var methodLocalVars = symbolTable.getLocalVariables(methodName);

        for (Symbol parameter : methodParameters) {
            if (parameter.getName().equals(varName)) {
                return true;
            }
        }

        for (Symbol localVar : methodLocalVars) {
            if (localVar.getName().equals(varName)) {
                return true;
            }
        }

        return false;
    }

    public static Symbol getSymbolByName(String varName, SymbolTableInit symbolTable) {

        // Check if the variable exists in the parameters or local variables of any method
        String methodName = currentMethod;
        var methodParameters = symbolTable.getParameters(methodName);
        var methodLocalVars = symbolTable.getLocalVariables(methodName);

        for (Symbol parameter : methodParameters) {
            if (parameter.getName().equals(varName)) {
                return parameter;
            }
        }

        for (Symbol localVar : methodLocalVars) {
            if (localVar.getName().equals(varName)) {
                return localVar;
            }
        }

        // Finally, Check if the variable exists in the fields
        for (Symbol field : symbolTable.getFields()) {
            if (field.getName().equals(varName)) {
                return field;
            }
        }

        return null;
    }

}