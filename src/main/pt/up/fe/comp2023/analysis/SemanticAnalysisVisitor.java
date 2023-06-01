package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import java.util.*;

import static pt.up.fe.comp2023.analysis.AnalysisUtils.getType;
import static pt.up.fe.comp2023.analysis.AnalysisUtils.isVariableDeclared;

public class SemanticAnalysisVisitor extends AJmmVisitor<SymbolTableInit, List<Report>> {

    static final List<String> PRIMITIVES = Arrays.asList("int", "void", "boolean");
    static final List<String> ARITHMETIC_OP = Arrays.asList("+", "-", "*", "/");
    static final List<String> COMPARISON_OP = List.of("<");
    static final List<String> LOGICAL_OP = List.of("&&");


    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::defaultVisit);

        addVisit("BinaryOp", this::visitBinaryOp);
        addVisit("Identifier", this::visitIdentifier);
        addVisit("MethodCallExpr", this::visitMethodCallExpr);
        addVisit("ArrayAccessExpr", this::visitArrayAccessExpr);
        addVisit("MethodStmt", this::visitMethodStmt);
        addVisit("MainMethodStmt", this::visitMainMethodStmt);
        addVisit("AssignStmt", this::visitAssignStmt);
        addVisit("ArrayAssignStmt", this::visitAssignStmt);
        addVisit("ExprStmt", this::visitExprStmt);

        addVisit("IfStmt", this::visitIfWhileStmt);
        addVisit("WhileStmt", this::visitIfWhileStmt);


        addVisit("ReturnStmt", this::visitReturnStmt);
    }

    private List<Report> visitExprStmt(JmmNode jmmNode, SymbolTableInit symbolTableInit){
        List<Report> reports = new ArrayList<>();

        for(JmmNode child : jmmNode.getChildren()){
            List<Report> childReports = visit(child, symbolTableInit);
            reports.addAll(childReports);
        }
        return reports;

    }


    private List<Report> visitMainMethodStmt(JmmNode jmmNode, SymbolTableInit symbolTableInit){
        List<Report> reports = new ArrayList<>();

        AnalysisUtils.currentMethod = "main";


        for(JmmNode child : jmmNode.getChildren()){
            List<Report> childReports = visit(child, symbolTableInit);
            reports.addAll(childReports);
        }
        return reports;
    }
    private List<Report> visitMethodStmt(JmmNode jmmNode, SymbolTableInit symbolTableInit){
        List<Report> reports = new ArrayList<>();
        JmmNode child = jmmNode.getJmmChild(0);
        AnalysisUtils.currentMethod = child.get("value");
        if(jmmNode.get("isStatic").equals("true")){
            AnalysisUtils.isMethodStatic = true;
        }

        for (JmmNode childs : jmmNode.getChildren()) {
            List<Report> childReports = visit(childs, symbolTableInit);
            reports.addAll(childReports);
        }
        return reports;

    }


    private List<Report> visitArrayAccessExpr(JmmNode jmmNode, SymbolTableInit symbolTable) {
        List<Report> reports = new ArrayList<>();

        JmmNode arrayNode = jmmNode.getJmmChild(0);
        JmmNode indexNode = jmmNode.getJmmChild(1);

        // Check if the array variable is declared
        List<Report> arrayReports = visit(arrayNode, symbolTable);
        reports.addAll(arrayReports);

        Type arrayType = getType(arrayNode);

        // Get the type names from the symbol table if possible
        if(arrayType.getName().equals("#UNKNOWN") && arrayNode.hasAttribute("value")){
            if(AnalysisUtils.isVariableDeclared(arrayNode.get("value"), symbolTable)){
                arrayType = AnalysisUtils.getSymbolByName(arrayNode.get("value"), symbolTable).getType();
            } else {
                reports.add(ReportUtils.undefinedArray(jmmNode, arrayNode.get("value")));
                return reports;
            }
        }

        // Check if the arrayNode is a variable before checking if it's an array
        if (arrayNode.getKind().equals("Identifier")) {
            if (!arrayType.isArray() && hasArrayAccess(jmmNode)) {
                reports.add(ReportUtils.arrayAccessOnNonArray(jmmNode, arrayNode.get("value")));
                return reports;
            }
        }

        Type indexType = getType(indexNode);

        if(indexType.getName().equals("#UNKNOWN") && indexNode.hasAttribute("value")){
            if(AnalysisUtils.isVariableDeclared(indexNode.get("value"), symbolTable)){
                indexType = AnalysisUtils.getSymbolByName(indexNode.get("value"), symbolTable).getType();
            } else {
                reports.add(ReportUtils.undefinedArray(jmmNode, indexNode.get("value")));
                return reports;
            }
        }

        if (!indexType.getName().equals("int") && !indexNode.getKind().equals("ArrayAccessExpr")) {
            reports.add(ReportUtils.invalidArrayIndexType(jmmNode, indexType.print()));
            return reports;
        }

        return reports;
    }




    private List<Report> visitBinaryOp(JmmNode jmmNode, SymbolTableInit symbolTable) {
        List<Report> reports = new ArrayList<>();

        Stack<JmmNode> stack = new Stack<>();
        stack.push(jmmNode);

        while(!stack.isEmpty()){
            jmmNode = stack.pop();

            Type lhsType = getType(jmmNode.getJmmChild(0));
            Type rhsType = getType(jmmNode.getJmmChild(1));

            JmmNode lhsNode = jmmNode.getJmmChild(0);
            JmmNode rhsNode = jmmNode.getJmmChild(1);

            if(jmmNode.getJmmParent().getKind().equals("MethodCallExpr")){
                JmmNode methodNode = jmmNode.getJmmParent();

                String methodName = methodNode.get("value");
                rhsType = symbolTable.getReturnType(methodName);
            }

            // Update the types of the operands if they are method calls on 'this'
            if (lhsNode.getKind().equals("MethodCallExpr")){
                lhsType = symbolTable.getReturnType(lhsNode.get("value"));
            }
            if (rhsNode.getKind().equals("MethodCallExpr")) {
                rhsType = symbolTable.getReturnType(rhsNode.get("value"));
            }

            if (lhsNode.getKind().equals("ArrayAccessExpr")) {
                lhsType = new Type("int", false);
            }
            if (rhsNode.getKind().equals("ArrayAccessExpr")) {
                rhsType = new Type("int", false);
            }

            if(lhsNode.getKind().equals("BinaryOp")){
                JmmNode rhsChildNode = lhsNode.getJmmChild(1);
                Type rhsChildType = getType(rhsChildNode);

                if (rhsType.getName().equals("#UNKNOWN") || rhsChildType.getName().equals("#UNKNOWN")) {
                    // Get the type names from the symbol table if possible
                    rhsType = rhsType.getName().equals("#UNKNOWN") && AnalysisUtils.isVariableDeclared(rhsNode.get("value"), symbolTable)
                            ? AnalysisUtils.getSymbolByName(rhsNode.get("value"), symbolTable).getType()
                            : rhsType;

                    rhsChildType = rhsChildType.getName().equals("#UNKNOWN") && AnalysisUtils.isVariableDeclared(rhsChildNode.get("value"), symbolTable)
                            ? AnalysisUtils.getSymbolByName(rhsChildNode.get("value"), symbolTable).getType()
                            : rhsChildType;

                }

                if (!rhsType.getName().equals(rhsChildType.getName()) && !(rhsNode.getJmmParent().getKind().equals("ArrayAccessExpr") || rhsNode.getJmmParent().getJmmParent().getKind().equals("ArrayAccessExpr"))) {
                    putUnknownType(jmmNode);
                    reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                    return reports;
                }
            }


            if(lhsNode.getKind().equals("ArrayAccessExpr") || rhsNode.getKind().equals("ArrayAccessExpr")){
                List<Report> childReports = visit(lhsNode, symbolTable);
                reports.addAll(childReports);
                return reports;
            }


            if (lhsType.getName().equals("#UNKNOWN") || rhsType.getName().equals("#UNKNOWN")) {
                // Get the type names from the symbol table if possible
                if (!lhsNode.getKind().equals("BinaryOp")) {
                    lhsType = lhsType.getName().equals("#UNKNOWN") && AnalysisUtils.isVariableDeclared(lhsNode.get("value"), symbolTable)
                            ? AnalysisUtils.getSymbolByName(lhsNode.get("value"), symbolTable).getType()
                            : lhsType;
                }

                if (!rhsNode.getKind().equals("BinaryOp")) {
                    rhsType = rhsType.getName().equals("#UNKNOWN") && AnalysisUtils.isVariableDeclared(rhsNode.get("value"), symbolTable)
                            ? AnalysisUtils.getSymbolByName(rhsNode.get("value"), symbolTable).getType()
                            : rhsType;
                }
            }



            if (jmmNode.getJmmParent() != null && (jmmNode.getJmmParent().getKind().equals("IfStmt") || jmmNode.getJmmParent().getKind().equals("WhileStmt"))) {
                if (!lhsType.getName().equals("boolean") && !COMPARISON_OP.contains(jmmNode.get("op"))){
                    putUnknownType(jmmNode);
                    reports.add(ReportUtils.invalidConditionTypeReport(jmmNode.getJmmParent(), rhsType.print()));
                    return reports;
                }
            }


            // If the current child is a BinaryOp, add it to the stack
            if (lhsNode.getKind().equals("BinaryOp")) {
                stack.push(lhsNode);
            } else {
                if (ARITHMETIC_OP.contains(jmmNode.get("op")) || LOGICAL_OP.contains(jmmNode.get("op")) || COMPARISON_OP.contains(jmmNode.get("op"))) {
                    if (lhsType.isArray()) {
                        putUnknownType(jmmNode);
                        reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                        return reports;
                    }
                }


                if (ARITHMETIC_OP.contains(jmmNode.get("op")) || COMPARISON_OP.contains(jmmNode.get("op"))) {
                    if (!lhsType.getName().equals("int")) {
                        putUnknownType(jmmNode);
                        reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                        return reports;
                    }
                }

                if (LOGICAL_OP.contains(jmmNode.get("op"))) {
                    if (!lhsType.getName().equals("boolean")) {
                        putUnknownType(jmmNode);
                        reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                        return reports;
                    }
                }

                if (!PRIMITIVES.contains(lhsType.getName())) {
                    putUnknownType(jmmNode);
                    reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                    return reports;
                }

                if ("<".contains(jmmNode.get("op"))) {
                    putType(jmmNode, new Type("boolean", false));
                } else {
                    putType(jmmNode, lhsType);
                }

                if (!lhsType.equals(rhsType) && !(rhsNode.getJmmParent().getKind().equals("ArrayAccessExpr") || rhsNode.getJmmParent().getJmmParent().getKind().equals("ArrayAccessExpr"))) {
                    putUnknownType(jmmNode);
                    reports.add(ReportUtils.operatorCannotBeAppliedReport(jmmNode, jmmNode.get("op"), lhsType.print(), rhsType.print()));
                    return reports;
                }
            }
        }

        return reports;
    }


    private List<Report> defaultVisit(JmmNode jmmNode, SymbolTableInit symbolTable){
        List<Report> reports = new ArrayList<>();

        for (JmmNode child : jmmNode.getChildren()) {
            List<Report> childReports = visit(child, symbolTable);
            reports.addAll(childReports);
        }

        return reports;
    }

    private void putType(JmmNode jmmNode, Type type) {
        jmmNode.put("type", type.getName());
        jmmNode.put("isArray", String.valueOf(type.isArray()));
    }

    private void putUnknownType(JmmNode jmmNode) {
        jmmNode.put("type", "#UNKNOWN");
    }

    private List<Report> visitIdentifier(JmmNode jmmNode, SymbolTableInit symbolTable) {
        List<Report> reports = new ArrayList<>();

        String identifier = jmmNode.get("value");

        if (symbolTable.hasImport(identifier)) {
            // Do not generate a report for imported class method calls
            return reports;
        }

        for (Symbol field : symbolTable.getFields()) {
            if (field.getName().equals(identifier) && AnalysisUtils.currentMethod != "#UNKNOWN" && (AnalysisUtils.isMethodStatic || AnalysisUtils.currentMethod == "main")) {
                reports.add(ReportUtils.nonStaticInStaticContext(jmmNode, jmmNode.get("value")));
                return reports;
            }
        }

        if (!AnalysisUtils.isVariableDeclared(identifier, symbolTable)) {
            reports.add(ReportUtils.cannotFindSymbolReport(jmmNode, identifier));
            return reports;
        }

        Type identifierType = AnalysisUtils.getSymbolByName(jmmNode.get("value"), symbolTable).getType();

        if (hasArrayAccess(jmmNode) && !identifierType.isArray()) {
            reports.add(ReportUtils.arrayAccessOnNonArray(jmmNode, identifier));
            return reports;
        }

        return reports;
    }



    private boolean hasArrayAccess(JmmNode jmmNode) {
        JmmNode parent = jmmNode.getJmmParent();
        if (parent == null) {
            return false;
        }

        if (parent.getKind().equals("ArrayAccessExpr") || parent.getJmmParent().getKind().equals("ArrayAccessExpr")) {
            return true;
        }

        return false;
    }


    private List<Report> visitIfWhileStmt(JmmNode jmmNode, SymbolTableInit symbolTable) {
        List<Report> reports = new ArrayList<>();


        JmmNode conditionNode = jmmNode.getJmmChild(0);

        if(conditionNode.getKind().equals("BinaryOp") || conditionNode.getKind().equals("MethodCallExpr")){
            List<Report> childReports = visit(conditionNode, symbolTable);
            reports.addAll(childReports);
            return reports;
        }
        Type conditionType = getType(conditionNode);

        if (conditionType.getName().equals("#UNKNOWN") && conditionNode.hasAttribute("value")) {
            if(AnalysisUtils.isVariableDeclared(conditionNode.get("value"), symbolTable)){
                conditionType = AnalysisUtils.getSymbolByName(conditionNode.get("value"), symbolTable).getType();
            }
        }

        for (JmmNode child : jmmNode.getChildren()) {
            List<Report> childReports = visit(child, symbolTable);
            reports.addAll(childReports);
        }

        if (!conditionType.getName().equals("boolean") && !conditionNode.getKind().equals("ArrayAccessExpr")) {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.invalidConditionTypeReport(jmmNode, conditionType.print()));
        }

        for (JmmNode child : jmmNode.getChildren()) {
            List<Report> childReports = visit(child, symbolTable);
            reports.addAll(childReports);
        }

        return reports;
    }


    private List<Report> visitReturnStmt(JmmNode jmmNode, SymbolTableInit symbolTable) {
        List<Report> reports = new ArrayList<>();

        String methodName = AnalysisUtils.currentMethod;

        if(methodName.equals("main")){
            reports.add(ReportUtils.returnInMainMethodReport(jmmNode));
            return reports;
        }

        // Get the method return type
        Type methodReturnType = symbolTable.getReturnType(methodName);

        // Get the return expression type
        JmmNode returnExpression = jmmNode.getChildren().get(0);
        if (returnExpression.getKind().equals("BinaryOp")) {
            List<Report> childReports = visit(returnExpression, symbolTable);
            reports.addAll(childReports);
        }

        Type expressionType = getType(returnExpression);

        if (symbolTable.hasMethod(expressionType.getName())) {
            expressionType = symbolTable.getReturnType(expressionType.getName());
        }

        if (returnExpression.getKind().equals("MethodCallExpr")) {
            List<Report> childReports = visit(returnExpression, symbolTable);
            reports.addAll(childReports);
        }
        // Check if the return type is compatible
        if (!expressionType.equals(methodReturnType)) {
            // If the expression is a method call on an imported class, assume it's valid
            if (returnExpression.getKind().equals("MethodCallExpr")) {
                JmmNode methodCallTarget = returnExpression.getChildren().get(0);

                String variableName = methodCallTarget.get("value");
                Type variableType = null;
                String className;

                if (AnalysisUtils.isVariableDeclared(variableName, symbolTable)) {
                    variableType = AnalysisUtils.getSymbolByName(variableName, symbolTable).getType();
                    className = variableType.getName();
                } else {
                    className = methodCallTarget.get("value");
                }

                if (symbolTable.hasImport(className)) {
                    // Do not generate a report for imported class method calls
                    return reports;
                } else {
                    reports.add(ReportUtils.classNotImportedReport(jmmNode, className));
                }
            }
            else if(returnExpression.getKind().equals("Identifier") || returnExpression.getKind().equals("ArrayAccessExpr")){
                List<Report> childReports = visit(returnExpression, symbolTable);
                reports.addAll(childReports);
            }
        }


        return reports;
    }





    private List<Report> visitMethodCallExpr(JmmNode jmmNode, SymbolTableInit symbolTable) {
        List<Report> reports = new ArrayList<>();

        String methodName = jmmNode.get("value");

        if(AnalysisUtils.currentMethod.equals("main") && jmmNode.getJmmChild(0).getKind().equals("Object")){
            if(jmmNode.getJmmChild(0).get("value").equals("this")){
                reports.add(ReportUtils.thisInMainMethodReport(jmmNode));
                return reports;
            }
        }

        if(AnalysisUtils.isMethodStatic && jmmNode.getJmmChild(0).getKind().equals("Object")){
            if(jmmNode.getJmmChild(0).get("value").equals("this")){
                reports.add(ReportUtils.invalidStaticMethodCall(jmmNode, jmmNode.get("value")));
            }
        }



        // Check if the method call is on an object
        if (jmmNode.getNumChildren() > 0) {
            JmmNode firstChild = jmmNode.getChildren().get(0);
            if (firstChild.getKind().equals("Identifier") || firstChild.getKind().equals("Object")) {
                String varName = firstChild.get("value");

                // Check if the variable exists in the symbol table
                if (AnalysisUtils.isVariableDeclared(varName, symbolTable) || varName.equals("this")) {
                    // Add this check to report error when accessing instance fields from a static method
                    if (symbolTable.hasMethod(methodName)) {

                        // Get the method parameters
                        List<Symbol> methodParameters = symbolTable.getParameters(methodName);
                        if(methodParameters != null){
                            // Check the number of arguments
                            int numArgs = jmmNode.getChildren().size() - 1;
                            if (numArgs != methodParameters.size()) {
                                reports.add(ReportUtils.wrongArgumentsReport(jmmNode, methodName));
                            } else {
                                // Check the types of the arguments
                                for (int i = 0; i < numArgs; i++) {
                                    JmmNode argNode = jmmNode.getChildren().get(i + 1);
                                    Type argType = getType(argNode);

                                    if (argType.getName().equals("#UNKNOWN")) {
                                        // Get the type names from the symbol table if possible
                                        argType = argType.getName().equals("#UNKNOWN") && AnalysisUtils.isVariableDeclared(argNode.get("value"), symbolTable)
                                                ? AnalysisUtils.getSymbolByName(argNode.get("value"), symbolTable).getType()
                                                : argType;
                                    }

                                    Type paramType = methodParameters.get(i).getType();


                                    if (!argType.equals(paramType) && !argType.getName().equals("this")) {
                                        reports.add(ReportUtils.incompatibleArgumentReport(jmmNode, methodName, i, paramType.getName(), argType.getName()));
                                    }
                                }
                            }
                        } else {
                            reports.add(ReportUtils.methodNotDeclaredReport(jmmNode, methodName));
                        }
                    } else {
                        if(!symbolTable.hasImport(varName)){
                            if(!symbolTable.hasMethod(methodName)){
                                //Check if the class extends another class
                                if(symbolTable.getSuper() == null)
                                    reports.add(ReportUtils.methodNotDeclaredReport(jmmNode, methodName));
                            }
                        }

                        if (!symbolTable.hasMethod(methodName)) {
                            boolean foundInImports = false;
                            for (String importedClass : symbolTable.getImports()) {
                                if (symbolTable.hasImport(importedClass)) {
                                    foundInImports = true;
                                    break;
                                }
                            }

                            // Check if the class extends another class
                            if (!foundInImports && symbolTable.getSuper() != null) {
                                foundInImports = true;
                            }

                            if (!foundInImports) {
                                reports.add(ReportUtils.methodNotDeclaredReport(jmmNode, methodName));
                            }
                        }

                    }
                }
            }
        }


        for (JmmNode child : jmmNode.getChildren()) {
            List<Report> childReports = visit(child, symbolTable);
            reports.addAll(childReports);
        }

        return reports;
    }


    private List<Report> visitAssignStmt(JmmNode jmmNode, SymbolTableInit symbolTable){
        List<Report> reports = new ArrayList<>();

        JmmNode assignmentNode;
        Type identifierType;

        String identifier = jmmNode.get("value");

        boolean isLocalVariable = false;

        if(AnalysisUtils.currentMethod != "#UNKNOWN"){
            for(Symbol var : symbolTable.getLocalVariables(AnalysisUtils.currentMethod)){
                if(var.getName().equals(identifier)){
                    isLocalVariable = true;
                }
            }
        }

        if(!isLocalVariable){
            for (Symbol field : symbolTable.getFields()) {
                if (field.getName().equals(identifier) && AnalysisUtils.currentMethod != "#UNKNOWN" && (AnalysisUtils.isMethodStatic || AnalysisUtils.currentMethod == "main")) {
                    reports.add(ReportUtils.nonStaticInStaticContext(jmmNode, jmmNode.get("value")));
                    return reports;
                }
            }
        }

        if(jmmNode.getKind().equals("ArrayAssignStmt")){
            JmmNode arrayNode = jmmNode;
            JmmNode indexNode = jmmNode.getJmmChild(0);

            Type arrayType = new Type("#UNKNOWN", true);

            // Get the type names from the symbol table if possible
            if(arrayType.getName().equals("#UNKNOWN") && arrayNode.hasAttribute("value")){
                if(AnalysisUtils.isVariableDeclared(arrayNode.get("value"), symbolTable)){
                    arrayType = AnalysisUtils.getSymbolByName(arrayNode.get("value"), symbolTable).getType();
                } else {
                    reports.add(ReportUtils.undefinedArray(jmmNode, arrayNode.get("value")));
                    return reports;
                }
            }


            if(!arrayType.equals(new Type("int", true))){
                reports.add(ReportUtils.incompatibleTypeReport(jmmNode, arrayType.print(), new Type("int", true).print()));
                return reports;
            }

            // Check if the arrayNode is a variable before checking if it's an array
            if (arrayNode.getKind().equals("Identifier")) {
                List<Report> childReports = visit(arrayNode, symbolTable);
                reports.addAll(childReports);
                if (!arrayType.isArray() && hasArrayAccess(jmmNode)) {
                    reports.add(ReportUtils.arrayAccessOnNonArray(jmmNode, arrayNode.get("value")));
                    return reports;
                }
            }


            Type indexType = getType(indexNode);
            if(indexType.getName().equals("#UNKNOWN") && indexNode.hasAttribute("value")){
                if(AnalysisUtils.isVariableDeclared(indexNode.get("value"), symbolTable)){
                    indexType = AnalysisUtils.getSymbolByName(indexNode.get("value"), symbolTable).getType();
                } else {
                    reports.add(ReportUtils.undefinedArray(jmmNode, indexNode.get("value")));
                    return reports;
                }
            }
            if (!indexType.getName().equals("int")) {
                reports.add(ReportUtils.invalidArrayIndexType(jmmNode, indexType.print()));
                return reports;
            }

            assignmentNode = jmmNode.getJmmChild(1);
            identifierType = arrayType;
        } else {

            String lhsName = jmmNode.get("value");
            identifierType = AnalysisUtils.isVariableDeclared(lhsName, symbolTable)
                    ? AnalysisUtils.getSymbolByName(lhsName, symbolTable).getType()
                    : new Type("#UNKNOWN", false);




            if (jmmNode.getJmmChild(0).getKind().equals("BinaryOp")) {
                List<Report> childReports = visit(jmmNode.getJmmChild(0), symbolTable);
                reports.addAll(childReports);
                return reports;
            }

            if(jmmNode.getJmmChild(0).getKind().equals("ArrayAccessExpr")){
                assignmentNode = jmmNode.getJmmChild(0).getJmmChild(0);
                List<Report> childReports = visit(jmmNode.getJmmChild(0), symbolTable);
                reports.addAll(childReports);
            } else {
                assignmentNode = jmmNode.getJmmChild(0); // RHS
            }

        }


        // Traverse nested BinaryOp nodes
        Stack<JmmNode> stack = new Stack<>();
        stack.push(assignmentNode);

        while (!stack.isEmpty()) {
            JmmNode currentNode = stack.pop();

            if (currentNode.getKind().equals("BinaryOp") || currentNode.getKind().equals("ArrayAccessExpr")) {
                List<Report> childReports = visit(currentNode, symbolTable);
                reports.addAll(childReports);

                // Add children to the stack to check them as well
                stack.push(currentNode.getJmmChild(0));
                stack.push(currentNode.getJmmChild(1));
            }

            assignmentNode = currentNode;
        }

        Type assignmentType;

        if(assignmentNode.getKind().equals("MethodCallExpr")){
            if(assignmentNode.getJmmChild(0).getKind().equals("BinaryOp")){
                return reports;
            }
            List<Report> childReports = visit(assignmentNode, symbolTable);
            reports.addAll(childReports);

            String methodName = assignmentNode.get("value");


            assignmentType = symbolTable.getReturnType(methodName);


        } else {
            assignmentType = getType(assignmentNode);
        }

        if(assignmentType == null) return reports;


        if (identifierType.getName().equals("#UNKNOWN") || assignmentType.getName().equals("#UNKNOWN")) {
            // Get the type names from the symbol table if possible
            identifierType = identifierType.getName().equals("#UNKNOWN") && AnalysisUtils.isVariableDeclared(jmmNode.get("value"), symbolTable)
                    ? AnalysisUtils.getSymbolByName(jmmNode.get("value"), symbolTable).getType()
                    : identifierType;
            assignmentType = assignmentType.getName().equals("#UNKNOWN") && AnalysisUtils.isVariableDeclared(assignmentNode.get("value"), symbolTable)
                    ? AnalysisUtils.getSymbolByName(assignmentNode.get("value"), symbolTable).getType()
                    : assignmentType;

        }


        if (!typeIsCompatibleWith(identifierType, assignmentType, symbolTable)) {
            putUnknownType(jmmNode);
            reports.add(ReportUtils.incompatibleTypeReport(jmmNode, identifierType.print(), assignmentType.print()));
            return reports;
        }



        return reports;
    }






    private boolean typeIsCompatibleWith(Type type1, Type type2, SymbolTableInit symbolTable) {
        if (type1.equals(type2)) return true;
        if ((type1.isArray() != type2.isArray()) && (type1.getName().equals("int") && type2.getName().equals("int"))) return true;
        if (PRIMITIVES.contains(type1.getName()) || PRIMITIVES.contains(type2.getName())) return false;
        if (type2.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper() == null) return false;
        if (symbolTable.getSuper() == null) return true;
        return !(type1.getName().equals(symbolTable.getClassName()) && symbolTable.getSuper().equals(type2.getName()));
    }


}
