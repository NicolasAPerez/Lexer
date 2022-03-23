package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.Declaration;
import edu.ufl.cise.plc.ast.Program;

import java.util.*;

public class SymbolTable {

    //Methods used for adding, removing and lookup so that the Map could be replaced
    //if another implementation should be used (Such as a stack for scope) without replacing the Vistor code
    private Map<String, Declaration> symbolTable;
    private String programName;
    private Program program;

    SymbolTable(){
        symbolTable = new HashMap<>();
        programName = "";
        program = null;
    }

    boolean addItem(String ident, Program pro){
        programName = ident;
        program = pro;
        return true;
    }

    boolean addItem(String ident, Declaration dec){
        if (symbolTable.containsKey(ident) || ident.equals(programName)){
            return false;
        }
        symbolTable.put(ident, dec);
        return true;
    }

    boolean removeItem(String ident){
        if (!symbolTable.containsKey(ident)){
            return false;
        }
        symbolTable.remove(ident);
        return true;
    }

    Declaration lookupItem(String ident){
        return symbolTable.get(ident);
    }

    boolean existsItem(String ident){
        return symbolTable.containsKey(ident) || ident.equals(programName);
    }





}
