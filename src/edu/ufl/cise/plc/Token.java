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
        return Integer.parseInt(val);
    }

    @Override
    public float getFloatValue() {
        return Float.parseFloat(val);
    }

    @Override
    public boolean getBooleanValue() {
        return Boolean.parseBoolean(val);
    }

    @Override
    public String getStringValue() {
        return val; //TODO: Fix this thing to properly remove exit characters
    }
}
