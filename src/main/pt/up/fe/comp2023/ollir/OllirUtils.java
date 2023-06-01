package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;


public class OllirUtils {

    public static String toOllir(Symbol symbol) {
        return String.format("%s.%s", symbol.getName(), toOllir(symbol.getType()));
    }

    public static String toOllir(Type type) {
        return toOllir(type.getName(), type.isArray());
    }

    public static String toOllir(JmmNode node) {
        return toOllir(node.get("type"), node.getAttributes().contains("arr"));
    }

    public static String generateFields(List<Symbol> fields) {
        StringBuilder Code = new StringBuilder();
        for (var field: fields) {
            Code.append(String.format(".field private %s;\n", toOllir(field)));
        }
        Code.append("\n");
        return Code.toString();
    }

    public static String toOllir(String type, boolean array) {
        String ollirType = "";
        if (array) {
            ollirType += "array.";
        }
        switch (type) {
            case "int":
                ollirType += "i32";
                break;
            case "void":
            case ".Any":
                ollirType += "V";
                break;
            case "boolean":
                ollirType += "bool";
                break;
            default:
                ollirType += type;
                break;
        };
        return ollirType;
    }

}
