package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;


public class OllirGenerator extends AJmmVisitor<String, List<String>> {

    private StringBuilder ollirCode;
    private SymbolTable symbolTable;
    private int num = 0;
    private int labels = 0;

    public OllirGenerator(SymbolTable symbolTable){
        this.ollirCode = new StringBuilder();
        this.symbolTable = symbolTable;
        addVisitors();
    }

    private void addVisitors() {
        addVisit("Program", this::visitProgram);
        addVisit("ClassDeclaration", this::visitClassDeclaration);
        addVisit("MainMethodDeclaration", this::visitMainMethod);
        addVisit("MethodDeclaration", this::visitMethod);
        addVisit("FieldDeclaration", this::visitField);
        addVisit("VarDeclaration", this::defaultVisit);
        addVisit("ExprStmt", this::visitExprStmt);
        addVisit("Integer", (node, dummy) -> Arrays.asList(String.format("%s.%s", node.get("image"), "i32"), "Constant", node.get("image")));
        addVisit("False", (node, dummy) -> Arrays.asList("0" + ".bool", "Constant", "0"));
        addVisit("True", (node, dummy) -> Arrays.asList("1" + ".bool", "Constant", "1"));
        addVisit("BinOp", this::visitBinOp);
        addVisit("Type", this::defaultVisit);
        addVisit("CreateObj", this::visitCreateObj);
        addVisit("CreateArrObj", this::visitCreateArrObj);
        addVisit("ReturnStatement", this::visitReturnStatement);
        addVisit("ReturnType", this::defaultVisit);
        addVisit("MethodArgsList", this::visitMethodArgsList);
        addVisit("Length", this::visitLength);
        addVisit("Identifier", this::visitId);
        addVisit("IfStmt", this::visitIfStm);
        addVisit("ArrayDeclaration", this::visitArrayDeclaration);
        this.setDefaultVisit(this::defaultVisit);
    }

    public String getCode() {
        return ollirCode.toString();
    }

    @Override
    protected void buildVisitor() {}

    private List<String> defaultVisit(JmmNode jmmNode, String s) {
        return null;
    }

    private List<String> visitProgram(JmmNode jmmNode, String s) {
        for (var importStmt: symbolTable.getImports()) {
            ollirCode.append(String.format("import %s;\n", importStmt));
        }
        ollirCode.append("\n");

        for (var child: jmmNode.getChildren()) {
            visit(child);
        }
        return null;
    }

    private List<String> visitClassDeclaration(JmmNode jmmNode, String s) {
        String str = symbolTable.getSuper() != null ? String.format("extends %s", symbolTable.getSuper()) : "";
        ollirCode.append(String.format("public %s %s {\n", symbolTable.getClassName(), str));
        ollirCode.append(OllirUtils.generateFields(symbolTable.getFields()));
        ollirCode.append(defaultConstructor(symbolTable.getClassName()));
        for (var child: jmmNode.getChildren()) {
            visit(child);
        }
        ollirCode.append("}");
        return null;
    }

    private List<String> visitExprStmt(JmmNode jmmNode, String s) {
        StringBuilder ret = new StringBuilder();
        for (JmmNode node : jmmNode.getChildren()) {
            ret.append(visit(node, ".V"));
        }
        ollirCode.append(ret + ";\n");
        return null;
    }

    private List<String> visitBinOp(JmmNode node, String s) {
        List<String> finalLhs = visit(node.getJmmChild(0), s);
        List<String> finalRhs = visit(node.getJmmChild(1), s);
        var lhs = finalLhs.get(0);
        var rhs = finalRhs.get(0);
        var op = node.get("op");

        var expressionVariability = "Not Constant";
        Integer value = null;
        if (finalLhs.size() > 2 && finalRhs.size() > 2 && finalLhs.get(1).equals("Constant") && finalRhs.get(1).equals("Constant")) {
            value = switch (op) {
                case "ADD" -> Integer.parseInt(finalLhs.get(2)) + Integer.parseInt(finalRhs.get(2));
                case "MUL" -> Integer.parseInt(finalLhs.get(2)) * Integer.parseInt(finalRhs.get(2));
                case "DIV" -> Integer.parseInt(finalLhs.get(2)) / Integer.parseInt(finalRhs.get(2));
                case "LT" -> (Integer.parseInt(finalLhs.get(2)) < Integer.parseInt(finalRhs.get(2))) ? 1 : 0;
                case "SUB" -> Integer.parseInt(finalLhs.get(2)) - Integer.parseInt(finalRhs.get(2));
                case "AND" -> ((Integer.parseInt(finalLhs.get(2)) >= 1) && (Integer.parseInt(finalRhs.get(2)) >= 1)) ? 1 : 0;
                default -> throw new RuntimeException("Unsupported binary operator: " + op);
            };
            expressionVariability = "Constant";
        }

        if (value != null) {
            String valueString = value.toString();
            return Arrays.asList(valueString, expressionVariability, valueString);
        }

        String Var = generateTempVar();
        String ollirOperator = OllirUtils.toOllir(node);
        ollirCode.append(String.format("%s.%s :=.%s %s %s.%s %s;\n",
                Var,
                ollirOperator,
                ollirOperator,
                lhs,
                op,
                ollirOperator,
                rhs
        ));
        return Arrays.asList(String.format("%s.%s", Var, ollirOperator), expressionVariability, null);
    }

    private List<String> visitCreateObj(JmmNode node, String s) {
        String Var = generateTempVar();
        String type = OllirUtils.toOllir(node);

        ollirCode.append(String.format("%s := new %s;\n", Var, type));

        ollirCode.append(String.format("invokespecial(%s, \"<init>\").V;\n", Var));

        return Collections.singletonList(String.format("%s", Var));
    }

    private List<String> visitField(JmmNode jmmNode, String s) {
        for (JmmNode node : jmmNode.getChildren()){
            visit(node);
        }
        return null;

    }

    private List<String> visitCreateArrObj(JmmNode node, String s) {
        String size = visit(node.getJmmChild(0), s).get(0);
        String Var = generateTempVar();
        String elementType = OllirUtils.toOllir(JmmNode.fromJson(node.get("type")));
        ollirCode.append(String.format("%s.%s :=.%s newarray(%s, %s).%s;\n",
                Var,
                OllirUtils.toOllir(node),
                OllirUtils.toOllir(node),
                elementType,
                size,
                OllirUtils.toOllir(node)
        ));
        return Collections.singletonList(String.format("%s.%s", Var, OllirUtils.toOllir(node)));
    }

    private List<String> visitReturnStatement(JmmNode node, String s) {
        String retValue = visit(node.getJmmChild(0), s).get(0);
        ollirCode.append(String.format("ret.%s %s;\n", OllirUtils.toOllir(symbolTable.getReturnType(s)), retValue));
        return null;
    }

    private List<String> visitId(JmmNode jmmNode, String s) {
        String id = jmmNode.get("name");
        ollirCode.append(id);

        return null;

    }

    private List<String> visitMethodArgsList(JmmNode node, String s) {
        if (node.getChildren().isEmpty()) {
            return null;
        }
        ollirCode.append(visit(node.getJmmChild(0), s).get(0));
        for (int i = 1; i < node.getChildren().size(); i++) {
            ollirCode.append(String.format(", %s", visit(node.getJmmChild(i), s).get(0)));
        }
        return null;
    }

    private List<String> visitMainMethod(JmmNode node, String s) {
        num = 0;
        String mainName = node.getJmmChild(0).get("image");
        String[] paramTypes = { mainName, "array", "String" };
        String params = String.join(".", paramTypes);
        ollirCode.append(String.format(".method public static main(%s).V {\n", params));
        visit(node.getJmmChild(1), "main");
        ollirCode.append("ret.V;\n");
        ollirCode.append("}\n");
        return null;
    }

    private List<String> visitMethod(JmmNode node, String s) {
        num = 0;
        Optional<JmmNode> methodNode = node.getChildren().stream()
                .filter(child -> "MethodDeclarator".equals(child.getKind()))
                .findFirst();
        if (methodNode.isEmpty()) {
            return null;
        }
        String methodName = methodNode.get().get("image");
        ollirCode.append(String.format(".method public %s (", methodName));
        visit(node.getJmmChild(2), methodName);
        Type returnType = symbolTable.getReturnType(methodName);
        ollirCode.append(String.format(").%s {\n", OllirUtils.toOllir(returnType)));
        visit(node.getJmmChild(3), methodName);
        visit(node.getJmmChild(4), methodName);
        ollirCode.append("}\n");
        return null;
    }


    private List<String> visitLength(JmmNode node, String s) {
        String array = visit(node.getJmmChild(0), s).get(0);
        String Var = generateTempVar() + ".i32";
        ollirCode.append(String.format("%s :=.i32 arraylength(%s).i32;\n", Var, array));
        return List.of(Var);
    }

    private String generateTempVar() {
        return String.format("t%d", this.num++);
    }

    private static String defaultConstructor(String s) {
        return String.format(".construct %s().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n", s);
    }

    private List<String> visitIfStm(JmmNode node, String scope) {
        int labelIdx = labels++;
        String condition = visit(node.getJmmChild(0), scope).get(0);
        ollirCode.append(String.format("if (!.bool %s) goto Else%d;\n", condition, labelIdx));

        for (var child : node.getJmmChild(1).getChildren()) {
            visit(child, scope);
        }
        ollirCode.append(String.format("goto EndIf%d;\n", labelIdx));
        ollirCode.append(String.format("Else%d: \n", labelIdx));

        for (var child : node.getJmmChild(2).getChildren()) {
            visit(child, scope);
        }
        ollirCode.append(String.format("EndIf%d: \n", labelIdx));

        return null;
    }

    private List<String> visitArrayDeclaration(JmmNode jmmNode, String s) {
        List<String> result = new ArrayList<>();
        StringBuilder ret = new StringBuilder();
        ret.append("new(array, ").append(visit(jmmNode.getJmmChild(0), s)).append(")").append(s);
        result.add(ret.toString());
        return result;
    }
}
