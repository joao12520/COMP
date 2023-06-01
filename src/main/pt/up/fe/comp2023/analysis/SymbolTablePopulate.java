package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.report.Report;

import java.util.ArrayList;
import java.util.List;

import static pt.up.fe.comp2023.analysis.AnalysisUtils.getSymbol;
import static pt.up.fe.comp2023.analysis.AnalysisUtils.getType;

public class SymbolTablePopulate extends PreorderJmmVisitor<SymbolTableInit, Integer> {

    @Override
    protected void buildVisitor() {

        setDefaultVisit(this::defaultVisit);


        addVisit("ImportStmt", this::importDeclarationVisit);
        addVisit("ClassStmt", this::classDeclarationVisit);
        addVisit("MethodStmt", this::methodDeclarationVisit);
        addVisit("MainMethodStmt", this::mainMethodVisit);

    }

    private Integer defaultVisit(JmmNode jmmNode, SymbolTableInit symbolTable){
        return 0;
    }

    private Integer importDeclarationVisit(JmmNode importDeclaration, SymbolTableInit symbolTable){

        var names = importDeclaration.get("names");
        var importS = String.join(".", names);
        symbolTable.addImport(importS);

        return 0;
    }

    private Integer classDeclarationVisit(JmmNode classDeclaration, SymbolTableInit symbolTable){
        List<Report> reports = new ArrayList<>();

        symbolTable.setClassName(classDeclaration.get("value"));
        if(classDeclaration.getAttributes().contains("superClassName"))
            symbolTable.setSuperClassName(classDeclaration.get("superClassName"));

        for(var varDeclarationNode : classDeclaration.getChildren()){
            if(varDeclarationNode.getKind().equals("VarStmt")){
                String varName = varDeclarationNode.get("value");
                boolean isArray = varDeclarationNode.getAttributes().contains("isArray") && varDeclarationNode.get("isArray").equals("true");

                JmmNode typeNode = varDeclarationNode.getChildren().get(0);
                var type = typeNode.get("value");
                Type varType = new Type(type, isArray);
                Symbol symbol = new Symbol(varType, varName);

                symbolTable.addField(symbol);

            }
        }

        return 0;
    }

    private Integer methodDeclarationVisit(JmmNode methodDeclarationNode, SymbolTableInit symbolTable){
        JmmNode signatureMethodNode = methodDeclarationNode.getChildren().get(0);
        var methodSignature = signatureMethodNode.get("value");
        JmmNode typeNode = signatureMethodNode.getChildren().get(0);
        Type returnType = getType(typeNode);
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();

        for(JmmNode child : methodDeclarationNode.getChildren()){
            if(child.getKind().equals("Parameter")){
                parameters.add(getSymbol(child));
            } else if (child.getKind().equals("VarStmt")) {
                variables.add(getSymbol(child));
                }
        }

        symbolTable.addMethod(methodSignature, returnType, parameters, variables);
        return 0;
    }

    private Integer mainMethodVisit(JmmNode mainMethodNode, SymbolTableInit symbolTable){
        String methodName = "main";
        Type returnType = new Type("static void", false);
        List<Symbol> parameters = new ArrayList<>();
        List<Symbol> variables = new ArrayList<>();

        for(JmmNode child : mainMethodNode.getChildren()){
            if(child.getKind().equals("Parameter")){
                parameters.add(getSymbol(child));
            } else if (child.getKind().equals("VarStmt")) {
                variables.add(getSymbol(child));
            }
        }


        symbolTable.addMethod(methodName, returnType, parameters, variables);
        return 0;
    }


}