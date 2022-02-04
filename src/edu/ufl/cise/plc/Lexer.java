package edu.ufl.cise.plc;

import java.util.Map;

public class Lexer implements ILexer{

    private final String toTokens;
    private int position;
    private int line;
    private State state;

    Lexer(String characters){
        toTokens = characters;
        position = 0;
    }

    private static enum State {
        START,
        HAVE_ZERO,
        HAVE_DECIMAL,
        IS_INT,
        STRING_START,
        STRING_SPECIALCHAR,
        IS_STRING,
        IDENTIFIER,
        COMMENT
    }
    private static final Map<String, IToken.Kind> reservedWords = Map.ofEntries(
            Map.entry("int", IToken.Kind.TYPE),
            Map.entry("string", IToken.Kind.TYPE),
            Map.entry("float", IToken.Kind.TYPE),
            Map.entry("boolean", IToken.Kind.TYPE),
            Map.entry("color", IToken.Kind.TYPE),
            Map.entry("image", IToken.Kind.TYPE),
            Map.entry("getWidth", IToken.Kind.IMAGE_OP),
            Map.entry("getHeight", IToken.Kind.IMAGE_OP),
            Map.entry("getRed", IToken.Kind.COLOR_OP),
            Map.entry("getGreen", IToken.Kind.COLOR_OP),
            Map.entry("getBlue", IToken.Kind.COLOR_OP),
            Map.entry("BLACK", IToken.Kind.COLOR_CONST),
            Map.entry("BLUE", IToken.Kind.COLOR_CONST),
            Map.entry("CYAN", IToken.Kind.COLOR_CONST),
            Map.entry("DARK_GRAY", IToken.Kind.COLOR_CONST),
            Map.entry("GRAY", IToken.Kind.COLOR_CONST),
            Map.entry("GREEN", IToken.Kind.COLOR_CONST),
            Map.entry("LIGHT_GRAY", IToken.Kind.COLOR_CONST),
            Map.entry("MAGENTA", IToken.Kind.COLOR_CONST),
            Map.entry("ORANGE", IToken.Kind.COLOR_CONST),
            Map.entry("PINK", IToken.Kind.COLOR_CONST),
            Map.entry("RED", IToken.Kind.COLOR_CONST),
            Map.entry("WHITE", IToken.Kind.COLOR_CONST),
            Map.entry("YELLOW", IToken.Kind.COLOR_CONST),
            Map.entry("true", IToken.Kind.BOOLEAN_LIT),
            Map.entry("false", IToken.Kind.BOOLEAN_LIT),
            Map.entry("if", IToken.Kind.KW_IF),
            Map.entry("void", IToken.Kind.KW_VOID),
            Map.entry("else", IToken.Kind.KW_ELSE),
            Map.entry("fi", IToken.Kind.KW_FI),
            Map.entry("write", IToken.Kind.KW_WRITE),
            Map.entry("console", IToken.Kind.KW_CONSOLE)
    );

    boolean isNumber(char x){
        switch (x){
            case '0','1','2','3','4','5','6','7','8','9':
                return true;
        }
        return false;
    }


    @Override
    public IToken next() throws LexicalException {
        return null;
    }

    @Override
    public IToken peek() throws LexicalException {
        IToken.Kind kind = null;
        String val = "";
        IToken.SourceLocation pos = new IToken.SourceLocation(line,position);
        int length = 0;
        state = State.START;

        switch (state){
            case START:
                switch (toTokens.charAt(position))


        }


        return new Token(kind,val,pos,length);
    }
}
