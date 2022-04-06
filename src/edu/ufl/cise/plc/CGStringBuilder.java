package edu.ufl.cise.plc;


import edu.ufl.cise.plc.ast.Types.Type;

public class CGStringBuilder {
    StringBuilder str;

    CGStringBuilder(){
        str = new StringBuilder();
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
        return this;
    }

    public CGStringBuilder importer(String imp){
        str.append("import ");
        str.append(imp);
        semiEnd();
        return this;
    }

    public CGStringBuilder importer(String... imp){
        for (String i : imp){
            str.append("import ");
            str.append(i);
            semiEnd();
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
}
