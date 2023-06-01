package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.ollir.*;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;

import java.util.Collections;

public class JmmOptimizer implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {
        var ollirGenerator = new OllirGenerator(semanticsResult.getSymbolTable());
        ollirGenerator.visit(semanticsResult.getRootNode());
        String ollirCode = ollirGenerator.getCode();

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }
}
