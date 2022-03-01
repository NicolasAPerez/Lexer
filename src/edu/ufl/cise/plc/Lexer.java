package edu.ufl.cise.plc;

import java.util.Map;

public class Lexer implements ILexer{

    private final char[] toTokens;
    private int column;
    private int line;
    private int position;

    private int peekPos;
    private int peekLine;
    private int peekCol;

    Lexer(String characters){
        //Tilde used as Sentential
        toTokens = (characters + '\0').toCharArray();
        column = 0;
        line = 0;
        position = 0;
    }

    private static enum State {
        START,
        HAVE_ZERO,
        HAVE_DOT,
        HAVE_EQUAL,
        HAVE_LARROW,
        HAVE_RARROW,
        HAVE_EXCLAIM,
        HAVE_MINUS,
        IS_INT,
        IS_STRING,
        STRING_ESCAPE,
        IS_FLOAT,
        IDENTIFIER,
        COMMENT,
        ENDTOKEN
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
        return switch (x) {
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true;
            default -> false;
        };
    }


    @Override
    public IToken next() throws LexicalException {
        IToken temp = peek();
        line = peekLine;
        column = peekCol;
        position = peekPos;
        return temp;
    }

    @Override
    public IToken peek() throws LexicalException {
        //Save old values to reset at the end
        int oldLine = line;
        int oldCol = column;
        int oldPos = position;

        IToken.Kind kind = null;
        String val = "";
        IToken.SourceLocation pos = new IToken.SourceLocation(line,column);
        int length = 0;
        int tokenPos = position;
        State state = State.START;

        do {
            switch (state) {
                case START -> {

                    switch (toTokens[tokenPos]) {
                        case '=' -> {
                            state = State.HAVE_EQUAL;
                            kind = IToken.Kind.ASSIGN;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '(' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.LPAREN;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case ')' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.RPAREN;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '[' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.LSQUARE;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case ']' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.RSQUARE;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '<' -> {
                            state = State.HAVE_LARROW;
                            kind = IToken.Kind.LT;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '>' -> {
                            state = State.HAVE_RARROW;
                            kind = IToken.Kind.GT;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '+' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.PLUS;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '-' -> {
                            state = State.HAVE_MINUS;
                            kind = IToken.Kind.MINUS;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '*' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.TIMES;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '/' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.DIV;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '%' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.MOD;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '&' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.AND;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '|' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.OR;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '!' -> {
                            state = State.HAVE_EXCLAIM;
                            kind = IToken.Kind.BANG;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case ';' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.SEMI;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case ',' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.COMMA;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '^' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.RETURN;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '0' -> {
                            state = State.HAVE_ZERO;
                            kind = IToken.Kind.INT_LIT;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '"' -> {
                            state = State.IS_STRING;
                            kind = IToken.Kind.INT_LIT;
                            length++;
                            val = val + toTokens[tokenPos];
                        }
                        case '#' -> {
                            state = State.COMMENT;
                            length++;
                        }
                        case '\0' -> {
                            state = State.ENDTOKEN;
                            kind = IToken.Kind.EOF;
                        }
                        case '\t', ' ' -> {
                            state = State.START;
                            length = 0;
                            pos = new IToken.SourceLocation(line,column+1);
                        }
                        case '\n' ->{
                            length = 0;
                            state = State.START;
                            line++;
                            column = -1;
                            pos = new IToken.SourceLocation(line,column+1);
                        }
                        case '\r' ->{
                        }

                        default -> {
                            if (Character.isJavaIdentifierStart(toTokens[tokenPos])) {
                                state = State.IDENTIFIER;
                                kind = IToken.Kind.IDENT;
                                length++;
                                val = val + toTokens[tokenPos];

                            } else if (isNumber(toTokens[tokenPos])) {
                                state = State.IS_INT;
                                kind = IToken.Kind.INT_LIT;
                                length++;
                                val = val + toTokens[tokenPos];
                            }
                            else {
                                throw new LexicalException("ERROR: char '" + toTokens[tokenPos] + "' does not start a valid token");
                            }
                        }
                    }
                }

                case COMMENT -> {
                    switch (toTokens[tokenPos]){
                        case '\r' ->{
                            isNumber('1');
                            //Empty for later since \n is next
                            length++;
                        }
                        case '\n' ->{
                            length = 0;
                            state = State.START;
                            line++;
                            column = -1;
                            pos = new IToken.SourceLocation(line,column+1);
                        }
                        case '\0' ->{
                            state = State.START;
                            tokenPos--;
                            column--;
                        }

                        default -> {
                            //Anything else until a newline
                            length++;
                        }
                    }
                }
                case HAVE_ZERO -> {
                    if (toTokens[tokenPos] == '.'){
                        state = State.HAVE_DOT;
                        length++;
                        val = val + toTokens[tokenPos];
                    }
                    else {
                        kind = IToken.Kind.INT_LIT;
                        val = "0";
                        state = State.ENDTOKEN;
                        tokenPos--;
                        column--;
                    }
                }
                case HAVE_DOT -> {
                    if (isNumber(toTokens[tokenPos])){
                        state = State.IS_FLOAT;
                        length++;
                        val = val + toTokens[tokenPos];
                    }
                    else {
                        throw new LexicalException("ERROR: Float has decimal but is missing digits");

                    }

                }
                case HAVE_EXCLAIM -> {
                    if (toTokens[tokenPos] == '='){
                        state = State.ENDTOKEN;
                        val = val + toTokens[tokenPos];
                        length++;
                        kind = IToken.Kind.NOT_EQUALS;
                    }
                    else {
                        kind = IToken.Kind.BANG;
                        state = State.ENDTOKEN;
                        tokenPos--;
                        column--;
                    }
                }
                case HAVE_LARROW -> {
                    if (toTokens[tokenPos] == '<'){
                        val = val + toTokens[tokenPos];
                        state = State.ENDTOKEN;
                        length++;
                        kind = IToken.Kind.LANGLE;
                    }
                    else if (toTokens[tokenPos] == '='){
                        val = val + toTokens[tokenPos];
                        state = State.ENDTOKEN;
                        length++;
                        kind = IToken.Kind.LE;
                    }
                    else if (toTokens[tokenPos] == '-'){
                        val = val + toTokens[tokenPos];
                        state = State.ENDTOKEN;
                        length++;
                        kind = IToken.Kind.LARROW;
                    }
                    else {
                        kind = IToken.Kind.LT;
                        state = State.ENDTOKEN;
                        tokenPos--;
                        column--;
                    }
                }
                case HAVE_RARROW -> {
                    if (toTokens[tokenPos] == '>'){
                        val = val + toTokens[tokenPos];
                        state = State.ENDTOKEN;
                        length++;
                        kind = IToken.Kind.RANGLE;
                    }
                    else if (toTokens[tokenPos] == '='){
                        val = val + toTokens[tokenPos];
                        state = State.ENDTOKEN;
                        length++;
                        kind = IToken.Kind.GE;
                    }
                    else {
                        kind = IToken.Kind.GT;
                        state = State.ENDTOKEN;
                        tokenPos--;
                        column--;
                    }
                }
                case HAVE_MINUS -> {
                    if (toTokens[tokenPos] == '>'){
                        val = val + toTokens[tokenPos];
                        state = State.ENDTOKEN;
                        length++;
                        kind = IToken.Kind.RARROW;
                    }
                    else {
                        kind = IToken.Kind.MINUS;
                        state = State.ENDTOKEN;
                        tokenPos--;
                        column--;
                    }
                }
                case HAVE_EQUAL -> {
                    if (toTokens[tokenPos] == '='){
                        val = val + toTokens[tokenPos];
                        state = State.ENDTOKEN;
                        length++;
                        kind = IToken.Kind.EQUALS;
                    }
                    else {
                        kind = IToken.Kind.ASSIGN;
                        state = State.ENDTOKEN;
                        tokenPos--;
                        column--;
                    }
                }
                case IDENTIFIER -> {
                    if (Character.isJavaIdentifierPart(toTokens[tokenPos]) && toTokens[tokenPos] != '\0'){
                        length++;
                        val = val + toTokens[tokenPos];
                    }
                    else {
                        if (reservedWords.containsKey(val)){
                            kind = reservedWords.get(val);
                            state = State.ENDTOKEN;
                            tokenPos--;
                            column--;
                        }
                        else {
                            kind = IToken.Kind.IDENT;
                            state = State.ENDTOKEN;
                            tokenPos--;
                            column--;
                        }
                    }

                }
                case IS_INT -> {
                    if (isNumber(toTokens[tokenPos])){
                        length++;
                        val = val + toTokens[tokenPos];
                    }
                    else if(toTokens[tokenPos] == '.'){
                        state = State.HAVE_DOT;
                        length++;
                        val = val + toTokens[tokenPos];
                    }
                    else {
                        try{
                            Integer.parseInt(val);
                        }
                        catch (Exception e){
                            throw new LexicalException("ERROR: " + val + " exceeds max int val");
                        }
                        kind = IToken.Kind.INT_LIT;
                        state = State.ENDTOKEN;
                        tokenPos--;
                        column--;
                    }

                }
                case IS_STRING -> {
                    switch (toTokens[tokenPos]){
                        case '"' ->{
                            state = State.ENDTOKEN;
                            length++;
                            kind = IToken.Kind.STRING_LIT;
                            val = val + toTokens[tokenPos];
                        }
                        case '\0' ->{
                            throw new LexicalException("ERROR: String is unbounded");
                        }
                        case '\\' ->{
                            val = val + toTokens[tokenPos];
                            length++;
                            state = State.STRING_ESCAPE;
                        }
                        case '\n'->{
                            column = -1;
                            line++;
                            val = val + toTokens[tokenPos];
                            length++;
                        }
                        default -> {
                            val = val + toTokens[tokenPos];
                            length++;
                        }
                    }

                }
                case STRING_ESCAPE -> {
                    switch (toTokens[tokenPos]){
                        case 'b','t', 'n','f', 'r', '"', '\'','\\'->{
                            val = val + toTokens[tokenPos];
                            length++;
                            state = State.IS_STRING;
                        }
                        default -> {
                            throw new LexicalException("ERROR: Escape char has no valid following char");
                        }
                    }
                }
                case IS_FLOAT -> {
                    if (isNumber(toTokens[tokenPos])){
                        length++;
                        val = val + toTokens[tokenPos];
                    }
                    else {
                        try{
                            Float.parseFloat(val);
                        }
                        catch (Exception e){
                            throw new LexicalException("ERROR: " + val + " has failed float casting");
                        }
                        kind = IToken.Kind.FLOAT_LIT;
                        state = State.ENDTOKEN;
                        tokenPos--;
                        column--;
                    }
                }

            }
            column++;
            tokenPos++;
        } while (state != State.ENDTOKEN);
        peekCol = column;
        peekPos = tokenPos;
        peekLine = line;

        column = oldCol;
        position = oldPos;
        line = oldLine;


        return new Token(kind,val,pos,length);
    }
}
