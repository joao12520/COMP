package pt.up.fe.comp2023.analysis;

import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;

public class JmmAnalyser implements JmmAnalysis {

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        List<Report> reports = new ArrayList<>();
        var symbolTable = new SymbolTableInit();

        SymbolTablePopulate symbolTablePopulate = new SymbolTablePopulate();
        symbolTablePopulate.visit(parserResult.getRootNode(), symbolTable);

        // Create a new instance of SemanticAnalysisVisitor
        SemanticAnalysisVisitor semanticAnalysisVisitor = new SemanticAnalysisVisitor();

        // Visit the root node with the semanticAnalysisVisitor
        List<Report> semanticReports = semanticAnalysisVisitor.visit(parserResult.getRootNode(), symbolTable);

        // Add the semantic analysis reports to the main reports list
        reports.addAll(semanticReports);

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
