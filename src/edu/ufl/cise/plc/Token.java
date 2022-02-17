package edu.ufl.cise.plc;

public class Token implements IToken{
    final Kind kind;
    final String val;
    final SourceLocation pos;
    final int length;


    Token(Kind kind, String val, SourceLocation pos, int length){
        this.kind = kind;
        this.val = val;
        this.pos = pos;
        this.length = length;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getText() {
        return val;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return pos;
    }

    @Override
    public int getIntValue() {
        if (kind == Kind.INT_LIT) {
            return Integer.parseInt(val);
        }
        else {
            //ERROR ENCOUNTERED
            return -1;
        }
    }

    @Override
    public float getFloatValue() {
        if (kind == Kind.FLOAT_LIT){
            return Float.parseFloat(val);
        }
        else {
            //ERROR ENCOUNTERED
            return -1;
        }
    }

    @Override
    public boolean getBooleanValue() {
        if (kind == Kind.BOOLEAN_LIT) {
            return Boolean.parseBoolean(val);
        }
        else {
            //ERROR ENCOUNTERED
            return false;
        }
    }

    @Override
    public String getStringValue() {
        String stringValue = "";
        for (int i = 0; i < val.length(); i++){
            if (val.charAt(i) == '\\'){
                switch (val.charAt(i + 1)){
                    case 'b' ->{
                        stringValue = stringValue + '\b';
                    }
                    case 't' ->{
                        stringValue = stringValue + '\t';

                    }
                    case 'n' ->{
                        stringValue = stringValue + '\n';

                    }
                    case 'f' ->{
                        stringValue = stringValue + '\f';

                    }
                    case 'r' ->{
                        stringValue = stringValue + '\r';

                    }
                    case '"' ->{
                        stringValue = stringValue + '\"';

                    }
                    case '\'' ->{
                        stringValue = stringValue + '\'';

                    }
                    case '\\' ->{
                        stringValue = stringValue + '\\';
                    }
                }
                i++;
            }
            else if (val.charAt(i) != '"'){
                stringValue = stringValue + val.charAt(i);
            }

        }
        return stringValue;
    }
}
