package edu.ufl.cise.plc;

import edu.ufl.cise.plc.ast.*;

public class Parser implements IParser {
    Lexer lex;
    int sourcePos;
    IToken parsing;


    Parser(String source) {
        lex = new Lexer(source);
        sourcePos = 0;

    }

    @Override
    public ASTNode parse() throws PLCException {
        parsing = lex.next();

        return expr();
    }

    //HELPERS
    protected boolean isKind(IToken.Kind kind) {
        return parsing.getKind() == kind;
    }
    protected boolean isKind(IToken.Kind... kinds) {
        for (IToken.Kind kind : kinds){
            if (parsing.getKind() == kind){
                return true;
            }
        }
        return false;
    }
    void consume() throws LexicalException {
        parsing = lex.next();
    }
    void match(IToken.Kind kind) throws LexicalException, SyntaxException {
        if (isKind(kind)){
            parsing = lex.next();
        }
        else {
            throw new SyntaxException("ERROR: " + parsing.getText() + " is not of Kind: " + kind.name());
        }
    }

    protected boolean parsePeek(IToken.Kind kind) throws LexicalException {
        return lex.peek().getKind() == kind;
    }
    protected boolean parsePeek(IToken.Kind... kinds) throws LexicalException {
        for (IToken.Kind kind : kinds){
            if (lex.peek().getKind() == kind){
                return true;
            }
        }
        return false;
    }

    //Non-Terminal Check Funcs (used to condense checking)
    private boolean isPrimaryExpr(){
        return isKind(IToken.Kind.BOOLEAN_LIT, IToken.Kind.STRING_LIT, IToken.Kind.INT_LIT, IToken.Kind.FLOAT_LIT, IToken.Kind.IDENT, IToken.Kind.LPAREN);
    }
    private boolean isUnaryExpr(){
        return isKind(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.COLOR_OP, IToken.Kind.IMAGE_OP) || isPrimaryExpr();
    }


    //Non-terminals
    Expr expr() throws LexicalException, SyntaxException {
        if (isUnaryExpr()){
            return logicalOrExpr();
        }
        else if(isKind(IToken.Kind.KW_IF)){
            return conditionalExpr();
        }
        else {
            throw new SyntaxException("ERROR: " + parsing.getText() + " is not a " + "Expr");
        }
    }

    Expr primaryExpr() throws LexicalException, SyntaxException {
        IToken first = parsing;
        Expr expression = null;


        if (isKind(IToken.Kind.BOOLEAN_LIT)){
            expression = new BooleanLitExpr(first);
            consume();
        }
        else if (isKind(IToken.Kind.STRING_LIT)){
            expression = new StringLitExpr(first);
            consume();
        }
        else if (isKind(IToken.Kind.INT_LIT)){
            expression = new IntLitExpr(first);
            consume();

        }
        else if (isKind(IToken.Kind.FLOAT_LIT)){
            expression = new FloatLitExpr(first);
            consume();

        }
        else if (isKind(IToken.Kind.IDENT)){
            expression = new IdentExpr(first);
            consume();

        }
        else if (isKind(IToken.Kind.LPAREN)){
            consume();
            expression = expr();
            match(IToken.Kind.RPAREN);
        }
        else{
            throw new SyntaxException("ERROR: " + first.getText() + " is not a " + "PrimaryExpr");
        }
        return expression;

    }

    PixelSelector pixelSelector() throws LexicalException, SyntaxException {
        IToken first = parsing;
        Expr left = null;
        Expr right = null;


        if (isKind(IToken.Kind.LSQUARE)){
            consume();
            left = expr();
            match(IToken.Kind.COMMA);
            right = expr();
            match(IToken.Kind.RSQUARE);
        }
        else{
            throw new SyntaxException("ERROR: " + first.getText() + " is not a " + "PixelSelector");
        }
        return new PixelSelector(first,left,right);
    }

    Expr unaryExprPostfix() throws LexicalException, SyntaxException {
        IToken first = parsing;
        Expr left = primaryExpr();
        PixelSelector right = null;
        if (isKind(IToken.Kind.LSQUARE)){
            right = pixelSelector();
            left = new UnaryExprPostfix(first,left,right);
        }
        return left;
    }
    Expr unaryExpr() throws LexicalException, SyntaxException {
        IToken first = parsing;
        IToken op = null;
        Expr expression = null;

        if (isKind(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.COLOR_OP, IToken.Kind.IMAGE_OP)){
            op = parsing;
            consume();
            expression = unaryExpr();
            expression = new UnaryExpr(first,op,expression);
        }
        else if (isPrimaryExpr()){
            expression = unaryExprPostfix();
        }
        else {
            throw new SyntaxException("ERROR: " + first.getText() + " is not a " + "UnaryExpr");
        }
        return expression;
    }

    Expr multiplicativeExpr() throws LexicalException, SyntaxException {
        IToken first = parsing;
        IToken op = null;
        Expr left = unaryExpr();
        Expr right = null;

        while (isKind(IToken.Kind.TIMES, IToken.Kind.DIV, IToken.Kind.MOD)){
            op = parsing;
            consume();
            right = unaryExpr();
            left = new BinaryExpr(first,left,op,right);
        }

        return left;
    }

    Expr additiveExpr() throws LexicalException, SyntaxException {
        IToken first = parsing;
        IToken op = null;
        Expr left = multiplicativeExpr();
        Expr right = null;

        while (isKind(IToken.Kind.PLUS, IToken.Kind.MINUS)){
            op = parsing;
            consume();
            right = multiplicativeExpr();
            left = new BinaryExpr(first,left,op,right);
        }

        return left;
    }

    Expr comparisonExpr() throws LexicalException, SyntaxException {
        IToken first = parsing;
        IToken op = null;
        Expr left = additiveExpr();
        Expr right = null;

        while (isKind(IToken.Kind.LT, IToken.Kind.GT, IToken.Kind.EQUALS, IToken.Kind.NOT_EQUALS, IToken.Kind.GE, IToken.Kind.LE)){
            op = parsing;
            consume();
            right = additiveExpr();
            left = new BinaryExpr(first,left,op,right);
        }

        return left;
    }

    Expr logicalAndExpr() throws LexicalException, SyntaxException {
        IToken first = parsing;
        IToken op = null;
        Expr left = comparisonExpr();
        Expr right = null;

        while (isKind(IToken.Kind.AND)){
            op = parsing;
            consume();
            right = comparisonExpr();
            left = new BinaryExpr(first,left,op,right);
        }

        return left;
    }

    Expr logicalOrExpr() throws LexicalException, SyntaxException {
        IToken first = parsing;
        IToken op = null;
        Expr left = logicalAndExpr();
        Expr right = null;

        while (isKind(IToken.Kind.OR)){
            op = parsing;
            consume();
            right = logicalAndExpr();
            left = new BinaryExpr(first,left,op,right);
        }

        return left;
    }

    Expr conditionalExpr() throws LexicalException, SyntaxException {
        IToken first = parsing;
        Expr condition = null;
        Expr ifCond = null;
        Expr elseCond = null;
        match(IToken.Kind.KW_IF);
        match(IToken.Kind.LPAREN);
        condition = expr();
        match(IToken.Kind.RPAREN);
        ifCond = expr();
        match(IToken.Kind.KW_ELSE);
        elseCond = expr();
        match(IToken.Kind.KW_FI);
        return new ConditionalExpr(first,condition,ifCond,elseCond);
    }







}