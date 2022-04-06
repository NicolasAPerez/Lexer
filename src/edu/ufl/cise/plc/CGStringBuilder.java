package edu.ufl.cise.plc;


import edu.ufl.cise.plc.ast.Types.Type;

import java.util.Vector;

public class CGStringBuilder {
    StringBuilder str;
    int indexImport;
    Vector<String> imported;

    CGStringBuilder(){
        str = new StringBuilder();
        indexImport = 0;
        imported = new Vector<>();
    }
    public  CGStringBuilder append(String s){
        str.append(s);
        return this;
    }
    public  CGStringBuilder append(int i){
        str.append(i);
        return this;
    }
    public  CGStringBuilder append(float f){
        str.append(f);
        return this;
    }
    public  CGStringBuilder append(boolean b){
        str.append(b);
        return this;
    }
    public String toString(){
        return str.toString();
    }
    public CGStringBuilder semi(){
        str.append(';');
        return this;
    }
    public CGStringBuilder comma(){
        str.append(", ");
        return this;
    }
    public CGStringBuilder lBracket(){
        str.append('{');
        return this;
    }
    public CGStringBuilder rBracket(){
        str.append('}');
        return this;
    }
    public CGStringBuilder lParen(){
        str.append('(');
        return this;
    }
    public CGStringBuilder rParen(){
        str.append(')');
        return this;
    }
    public CGStringBuilder tab(){
        str.append('\t');
        return this;
    }
    public CGStringBuilder newLine(){
        str.append('\n');
        return this;
    }
    public CGStringBuilder semiEnd(){
        semi().newLine();
        return this;
    }

    public CGStringBuilder packager(String pack){
        str.append("package ");
        str.append(pack);
        semiEnd();
        indexImport = str.length();
        return this;
    }

    public CGStringBuilder importer(String imp){
        if (!imported.contains(imp)) {
            str.insert(indexImport, "import " + imp + "; \n");
            imported.add(imp);
        }

        return this;
    }

    public CGStringBuilder importer(String... imp){
        for (String i : imp){
            if (!imported.contains(i)) {
                str.insert(indexImport, "import " + i + "; \n");
                imported.add(i);
            }
        }
        return this;
    }


    public CGStringBuilder classStarter(String className){
        append("public class ").append(className).lBracket().newLine().tab();
        return this;
    }

    public CGStringBuilder appendType(String type){
        switch (type){
            case "int", "boolean", "float" ->{
                append(type);
            }
            case "string" ->{
                append("String");
            }

        }
        return this;
    }
    public CGStringBuilder appendType(Type type){
        switch (type){
            case INT ->{
                append("int");
            }
            case STRING ->{
                append("String");
            }
            case FLOAT -> {
                append("float");
            }
            case BOOLEAN -> {
                append("boolean");
            }

        }
        return this;
    }
    public CGStringBuilder appendObjType(Type type){
        switch (type){
            case INT ->{
                append("Integer");
            }
            case STRING ->{
                append("String");
            }
            case FLOAT -> {
                append("Float");
            }
            case BOOLEAN -> {
                append("Boolean");
            }

        }
        return this;
    }
    public CGStringBuilder coerceTo(Type original, Type coerce){
        if (coerce != null){
            if (original != coerce){
                lParen().appendType(coerce).rParen().append(" ");
            }
        }
       return this;
    }
}
