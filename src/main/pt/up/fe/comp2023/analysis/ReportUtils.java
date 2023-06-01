package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

public class ReportUtils {

    public static Report baseReport(JmmNode at, ReportType type, Stage stage, String message) {
        return new Report(type, stage, Integer.parseInt(at.get("lineStart")), Integer.parseInt(at.get("colStart")), message);
    }

    public static Report cyclicInheritance(JmmNode at, String type) {
        StringBuilder message = new StringBuilder();
        message.append("cyclic inheritance involving ");
        message.append(type);
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report nonStaticInStaticContext(JmmNode at, String entity) {
        StringBuilder message = new StringBuilder();
        message.append("non-static ");
        message.append(entity);
        message.append(" cannot be referenced from a static context");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report symbolAlreadyDefinedReport(JmmNode at, String symbolType, String symbol, String locationType, String location) {
        StringBuilder message = new StringBuilder();
        message.append(symbolType);
        message.append(" ");
        message.append(symbol);
        message.append(" is already defined in ");
        message.append(locationType);
        message.append(" ");
        message.append(location);

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report cannotBeDereferencedReport(JmmNode at, String type) {
        StringBuilder message = new StringBuilder();
        message.append("incompatible types: ");
        message.append(type);
        message.append(" cannot be dereferenced");
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report methodNotDeclaredReport(JmmNode jmmNode, String methodName) {
        String message = "Method " + methodName + " is not declared";
        return baseReport(jmmNode, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }


    public static Report incompatibleTypeReport(JmmNode at, String actual, String expected) {
        StringBuilder message = new StringBuilder();
        message.append("incompatible types: ");
        message.append(actual);
        message.append(" cannot be converted to ");
        message.append(expected);
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report incompatibleTypesReport(JmmNode at, String leftType, String rightType, String operator) {
        StringBuilder message = new StringBuilder();
        message.append("incompatible types for ");
        message.append(operator);
        message.append(" operation: ");
        message.append(leftType);
        message.append(" and ");
        message.append(rightType);
        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report invalidStaticMethodCall(JmmNode node, String methodName) {
        String message = "Invalid static method call: Cannot call a non-static method '" + methodName + "' from a static method.";
        return baseReport(node, ReportType.ERROR, Stage.SEMANTIC, message);
    }



    public static Report invalidArrayIndexType(JmmNode node, String indexType) {
        String message = "Invalid array index type: " + indexType + ". The index must be of type int.";
        return baseReport(node, ReportType.ERROR, Stage.SEMANTIC, message);
    }


    public static Report operatorCannotBeAppliedReport(JmmNode at, String operator, String lhs, String rhs) {
        StringBuilder message = new StringBuilder();
        message.append("operator '");
        message.append(operator);
        message.append("' cannot be applied to '");
        message.append(lhs);
        message.append("' and '");
        message.append(rhs);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report undefinedArray(JmmNode jmmNode, String arrayName) {
        return baseReport(jmmNode,
                ReportType.ERROR,
                Stage.SEMANTIC,
                String.format("Undefined array: %s", arrayName)
        );
    }

    public static Report thisInMainMethodReport(JmmNode node) {
        return baseReport(node, ReportType.ERROR, Stage.SEMANTIC, "Cannot use 'this' in static context. Main method is static.");
    }

    public static Report returnInMainMethodReport(JmmNode node) {
        return baseReport(node, ReportType.ERROR, Stage.SEMANTIC, "Main method should not have a return statement.");
    }

    public static Report invalidConditionTypeReport(JmmNode jmmNode, String conditionType) {
        String message = "Invalid condition type in " + jmmNode.getKind() + " statement: Expected boolean, but found " + conditionType;
        return baseReport(jmmNode, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }


    public static Report arrayRequiredReport(JmmNode at, String found) {
        StringBuilder message = new StringBuilder();
        message.append("array required, but ");
        message.append(found);
        message.append(" found");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report cannotFindSymbolReport(JmmNode at, String symbol) {
        StringBuilder message = new StringBuilder();
        message.append("cannot find symbol '");
        message.append(symbol);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report ambiguousMethodCallReport(JmmNode at, String methodname) {
        StringBuilder message = new StringBuilder();
        message.append("reference to ");
        message.append(methodname);
        message.append(" is ambiguous");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report alreadyImported(JmmNode at, String lastName, String other) {
        StringBuilder message = new StringBuilder();
        message.append("a type with the same simple name '");
        message.append(lastName);
        message.append("' has already been imported from '");
        message.append(other);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report arrayAccessOnNonArray(JmmNode at, String variable) {
        StringBuilder message = new StringBuilder();
        message.append("array access on non-array variable '");
        message.append(variable);
        message.append("'");

        return baseReport(at, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report wrongArgumentsReport(JmmNode node, String methodName) {
        String message = "Wrong number of arguments for method '" + methodName + "'";
        return baseReport(node, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report incompatibleArgumentReport(JmmNode node, String methodName, int index, String expectedType, String actualType) {
        String message = "Incompatible argument type for parameter " + (index + 1) + " of method '" + methodName + "': expected '" + expectedType + "', found '" + actualType + "'";
        return baseReport(node, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report incompatibleReturnTypeReport(JmmNode jmmNode, String methodName, String expectedType, String actualType) {
        String message = "Incompatible return type in method '" + methodName + "'. Expected '" + expectedType + "', found '" + actualType;
        return baseReport(jmmNode, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

    public static Report classNotImportedReport(JmmNode node, String className) {
        String message = "Class '" + className + "' not imported.";
        return baseReport(node, ReportType.ERROR, Stage.SEMANTIC, message.toString());
    }

}