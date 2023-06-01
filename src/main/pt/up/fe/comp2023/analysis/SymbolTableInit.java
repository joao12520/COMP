package pt.up.fe.comp2023.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.*;


public class SymbolTableInit implements SymbolTable {

    private final List<String> imports = new ArrayList<>();
    private String className;
    private String superClassName;
    private final List<Symbol> fields = new ArrayList<>();
    private final List<String> methods = new ArrayList<>();
    private final Map<String, Type> returnTypes = new HashMap<>();
    private final Map<String, List<Symbol>> parameters = new HashMap<>();
    private final Map<String, List<Symbol>> localVars = new HashMap<>();


    public void setClassName(String className) {
        this.className = className;
    }

    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }

    @Override
    public List<String> getImports() {
        return this.imports;
    }

    public void addImport(String importS){
        this.imports.add(importS);
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClassName;
    }

    public void addField(Symbol field) {
        this.fields.add(field);
    }
    @Override
    public List<Symbol> getFields() {
        return this.fields;
    }

    @Override
    public List<String> getMethods() {
        return this.methods;
    }

    public void addMethod(String methodName) {
        methods.add(methodName);
    }

    @Override
    public Type getReturnType(String methodName) {
        return this.returnTypes.get(methodName);
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        return this.parameters.get(methodName);
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        return this.localVars.get(methodName);
    }

    public void addMethod(String methodName, Type returnType, List<Symbol> params, List<Symbol> localVars) {
        this.methods.add(methodName);
        this.returnTypes.put(methodName, returnType);
        this.parameters.put(methodName, params);
        this.localVars.put(methodName, localVars);
    }

    public boolean hasImport(String id) {
        for (String importStr : getImports()) {
            var importList = importStr.split("\\.");
            if (importList[importList.length-1].equals(importStr)){
                return true;
            }
        }
        return false;
    }


    public boolean hasMethod(String methodSignature) {
        return this.methods.contains(methodSignature);
    }

    public boolean hasField(String varName){
        return this.fields.contains(varName);
    }

    public boolean hasVariable(String varName) {
        return this.fields.stream().anyMatch(symbol -> symbol.getName().equals(varName));
    }

    public boolean isImportedClass(String className) {
        return this.imports.contains(className);
    }



}