package edu.ufl.cise.plc;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.ufl.cise.plc.IToken.Kind;
import edu.ufl.cise.plc.ast.ASTNode;
import edu.ufl.cise.plc.ast.ASTVisitor;
import edu.ufl.cise.plc.ast.AssignmentStatement;
import edu.ufl.cise.plc.ast.BinaryExpr;
import edu.ufl.cise.plc.ast.BooleanLitExpr;
import edu.ufl.cise.plc.ast.ColorConstExpr;
import edu.ufl.cise.plc.ast.ColorExpr;
import edu.ufl.cise.plc.ast.ConditionalExpr;
import edu.ufl.cise.plc.ast.ConsoleExpr;
import edu.ufl.cise.plc.ast.Declaration;
import edu.ufl.cise.plc.ast.Dimension;
import edu.ufl.cise.plc.ast.Expr;
import edu.ufl.cise.plc.ast.FloatLitExpr;
import edu.ufl.cise.plc.ast.IdentExpr;
import edu.ufl.cise.plc.ast.IntLitExpr;
import edu.ufl.cise.plc.ast.NameDef;
import edu.ufl.cise.plc.ast.NameDefWithDim;
import edu.ufl.cise.plc.ast.PixelSelector;
import edu.ufl.cise.plc.ast.Program;
import edu.ufl.cise.plc.ast.ReadStatement;
import edu.ufl.cise.plc.ast.ReturnStatement;
import edu.ufl.cise.plc.ast.StringLitExpr;
import edu.ufl.cise.plc.ast.Types.Type;
import edu.ufl.cise.plc.ast.UnaryExpr;
import edu.ufl.cise.plc.ast.UnaryExprPostfix;
import edu.ufl.cise.plc.ast.VarDeclaration;
import edu.ufl.cise.plc.ast.WriteStatement;
import edu.ufl.cise.plc.runtime.ImageOps;

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class CodeGenVisitor implements  ASTVisitor{

    String packageName;
    CGStringBuilder javaP;
    int tabLoc;

    private boolean checkType(Expr ex, Type t){
        return (ex.getType() == t || ex.getCoerceTo() == t);
    }
    private String translateKind(Kind k){
        switch (k){
            case PLUS -> { return "ImageOps.OP.PLUS";}
            case MINUS -> { return "ImageOps.OP.MINUS";}
            case TIMES -> { return "ImageOps.OP.TIMES";}
            case DIV -> { return "ImageOps.OP.DIV";}
            case MOD -> { return "ImageOps.OP.MOD";}
            case EQUALS -> {return "ImageOps.BoolOP.EQUALS";}
            case NOT_EQUALS -> {return "ImageOps.BoolOP.NOT_EQUALS";}
            default -> {return null;}
        }
    }

    public CodeGenVisitor(String packageName) {
        this.packageName = packageName;
        javaP = new CGStringBuilder();
        tabLoc = 0;

    }

    @Override
    public Object visitProgram(Program program, Object arg) throws Exception {
        javaP.packager(packageName);
        javaP.classStarter(program.getName());

        javaP.append("public static ").appendType(program.getReturnType()).append(" apply").lParen();

        List<NameDef> parameters = program.getParams();


        for (int i = 0; i < parameters.size(); i++){
            parameters.get(i).visit(this, arg);
            if (i < parameters.size() - 1){
                javaP.comma();
            }
        }
        javaP.rParen().lBracket().newLine();

        List<ASTNode> decsAndStatements = program.getDecsAndStatements();

        for (ASTNode node : decsAndStatements) {
            javaP.tab().tab();
            node.visit(this, arg);
        }
        javaP.tab().rBracket().newLine();
        javaP.rBracket().newLine();


        return javaP.toString();
    }

    @Override
    public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
        javaP.append(booleanLitExpr.getValue());
        return javaP;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
        javaP.append("\"\"\"").newLine();
        javaP.append(stringLitExpr.getValue());
        javaP.append("\"\"\"");
        return javaP;
    }

    @Override
    public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
        javaP.coerceTo(intLitExpr.getType(), intLitExpr.getCoerceTo());
        javaP.append(intLitExpr.getValue());
        return javaP;
    }

    @Override
    public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
        javaP.coerceTo(floatLitExpr.getType(), floatLitExpr.getCoerceTo());
        javaP.append(floatLitExpr.getValue()).append("f");
        return javaP;
    }

    @Override
    public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
        javaP.append("ColorTuple.unpack").lParen().append("Color.");
        javaP.append(colorConstExpr.getText());
        javaP.append(".getRGB()").rParen();
        javaP.importer("java.awt.Color");
        javaP.importer("edu.ufl.cise.plc.runtime.ColorTuple");
        return javaP;

    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        javaP.importer("edu.ufl.cise.plc.runtime.ConsoleIO");
        javaP.lParen().appendObjType(consoleExpr.getCoerceTo()).rParen().append(" ");
        javaP.append("ConsoleIO.readValueFromConsole").lParen().append("\"").append(consoleExpr.getCoerceTo().toString().toUpperCase()).append("\"").comma();
        if (consoleExpr.getCoerceTo() != COLOR) {
            javaP.append("\"Enter ").appendObjType(consoleExpr.getCoerceTo()).append(": \"").rParen();
        }
        else {
            javaP.append("\"Enter RED, GREEN, BLUE").append(": \"").rParen();

        }
        return javaP;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        if (colorExpr.getRed().getType() == INT) {
            javaP.append("new ColorTuple").lParen();
        }
        else {
            javaP.append("new ColorTupleFloat").lParen();
        }
        colorExpr.getRed().visit(this, arg);
        javaP.comma();
        colorExpr.getGreen().visit(this, arg);
        javaP.comma();
        colorExpr.getBlue().visit(this, arg);
        javaP.rParen();
        javaP.importer("edu.ufl.cise.plc.runtime.ColorTuple");
        return javaP;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        javaP.coerceTo(unaryExpression.getType(), unaryExpression.getCoerceTo());
        javaP.lParen().append(unaryExpression.getOp().getText());
        unaryExpression.getExpr().visit(this,arg);
        javaP.rParen();
        return javaP;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        javaP.coerceTo(binaryExpr.getType(), binaryExpr.getCoerceTo());
        javaP.lParen();
        if (checkType(binaryExpr.getLeft(), COLOR) || checkType(binaryExpr.getLeft(), COLORFLOAT) ){
            javaP.append("ImageOps.binaryTupleOp").lParen().append(translateKind(binaryExpr.getOp().getKind()));
            javaP.comma();
            binaryExpr.getLeft().visit(this, arg);
            javaP.comma();
            binaryExpr.getRight().visit(this, arg);
            javaP.rParen();
            javaP.importer("edu.ufl.cise.plc.runtime.ImageOps");

        }
        else if (checkType(binaryExpr.getLeft(), IMAGE)){
            if (checkType(binaryExpr.getRight(), IMAGE)){
                javaP.append("ImageOps.binaryImageImageOp").lParen().append(translateKind(binaryExpr.getOp().getKind()));
                javaP.comma();
                binaryExpr.getLeft().visit(this, arg);
                javaP.comma();
                binaryExpr.getRight().visit(this, arg);
                javaP.rParen();
                javaP.importer("edu.ufl.cise.plc.runtime.ImageOps");
            }
            else {
                javaP.append("ImageOps.binaryImageScalarOp").lParen().append(translateKind(binaryExpr.getOp().getKind()));
                javaP.comma();
                binaryExpr.getLeft().visit(this, arg);
                javaP.comma();
                binaryExpr.getRight().visit(this, arg);
                javaP.rParen();
                javaP.importer("edu.ufl.cise.plc.runtime.ImageOps");
            }
        }
        else {
            binaryExpr.getLeft().visit(this, arg);
            if (binaryExpr.getLeft().getType() == STRING && binaryExpr.getRight().getType() == STRING && binaryExpr.getOp().getText().equals("==")) {
                javaP.append(".equals").lParen();
                binaryExpr.getRight().visit(this, arg);
                javaP.rParen();

            } else {
                javaP.append(" ").append(binaryExpr.getOp().getText()).append(" ");
                binaryExpr.getRight().visit(this, arg);

            }
        }
        javaP.rParen();
        return javaP;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception {
        javaP.coerceTo(identExpr.getType(), identExpr.getCoerceTo());
        javaP.append(identExpr.getFirstToken().getText());
        return javaP;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
        if (conditionalExpr.getType() != conditionalExpr.getCoerceTo()){
            javaP.coerceTo(conditionalExpr.getType(), conditionalExpr.getCoerceTo()).lParen();
        }
        javaP.lParen();
        conditionalExpr.getCondition().visit(this, arg);
        javaP.rParen().append("? ");
        conditionalExpr.getTrueCase().visit(this, arg);
        javaP.append(" : ");
        conditionalExpr.getFalseCase().visit(this, arg);

        if (conditionalExpr.getType() != conditionalExpr.getCoerceTo()){
            javaP.rParen();
        }
        return javaP;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        dimension.getWidth().visit(this, arg);
        javaP.comma();
        dimension.getHeight().visit(this, arg);
        return javaP;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        //Left side handled in Assignment op
        pixelSelector.getX().visit(this, arg);
        javaP.comma();
        pixelSelector.getY().visit(this, arg);
        return javaP;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        if (assignmentStatement.getSelector() == null) {
            if (assignmentStatement.getTargetDec().getType() != IMAGE) {
                javaP.append(assignmentStatement.getName()).append(" = ");
                assignmentStatement.getExpr().visit(this, arg);
                javaP.semiEnd();
            }
            else{
                javaP.append(assignmentStatement.getName()).append(" = ");

                javaP.importer("edu.ufl.cise.plc.runtime.ImageOps");

                if (assignmentStatement.getTargetDec().getDim() != null) {
                    javaP.append("resize").lParen();
                    assignmentStatement.getExpr().visit(this, arg);
                    javaP.comma();
                    assignmentStatement.getTargetDec().getDim().visit(this, arg);
                    javaP.rParen().semiEnd();
                }
                else {
                    javaP.append("clone").lParen();
                    assignmentStatement.getExpr().visit(this, arg);
                    javaP.rParen().semiEnd();
                }


            }
        }
        else{
            String x = assignmentStatement.getSelector().getX().getFirstToken().getText();
            String y = assignmentStatement.getSelector().getY().getFirstToken().getText();

            javaP.append("for (int ").append(x).append(" = 0; ").append(x).append(" < ").append(assignmentStatement.getName()).append(".getWidth(); ").append(x).append("++)").newLine();
            javaP.tabTo(3).append("for (int ").append(y).append(" = 0; ").append(y).append(" < ").append(assignmentStatement.getName()).append(".getHeight(); ").append(y).append("++)").newLine();;
            javaP.tabTo(4).append("ImageOps.setColor(").append(assignmentStatement.getName()).comma().append(x).comma().append(y).comma();
            assignmentStatement.getExpr().visit(this, arg);
            javaP.rParen().semiEnd();
            javaP.importer("edu.ufl.cise.plc.runtime.ImageOps");
        }
        return javaP;
    }

    @Override
    public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
        javaP.importer("edu.ufl.cise.plc.runtime.ConsoleIO");
        javaP.append("ConsoleIO.console.println").lParen();
        writeStatement.getSource().visit(this, arg);
        javaP.rParen();
        javaP.semiEnd();
        return javaP;
    }

    @Override
    public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
        javaP.append(readStatement.getName()).append(" = ");
        readStatement.getSource().visit(this, arg);
        javaP.semiEnd();
        return javaP;
    }



    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
        javaP.appendType(nameDef.getType()).append(" ").append(nameDef.getName());
        return javaP;
    }

    @Override
    public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
        javaP.appendType(nameDefWithDim.getType()).append(" ").append(nameDefWithDim.getName());
        return javaP;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
        Expr expr = returnStatement.getExpr();
        javaP.append("return ");
        expr.visit(this, arg);
        javaP.semiEnd();
        return javaP;
    }

    @Override
    public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
        declaration.getNameDef().visit(this, arg);

        if (declaration.getType() != IMAGE) {
            if (declaration.getOp() == null) {
                javaP.semiEnd();
            }
            else {
                javaP.append(" = ");
                declaration.getExpr().visit(this, arg);
                javaP.semiEnd();
            }
        }
        else{
            if (declaration.getNameDef() instanceof NameDefWithDim){
                if (declaration.getExpr() != null){
                    javaP.importer("edu.ufl.cise.plc.runtime.FileURLIO");
                    javaP.append(" = ");
                    javaP.append("FileURLIO.readImage").lParen();
                    declaration.getExpr().visit(this, arg);
                    javaP.comma();
                    declaration.getDim().visit(this, arg);
                    javaP.rParen().semiEnd();
                }
                else {
                    javaP.append(" = ");
                    javaP.append("new BufferedImage").lParen();
                    declaration.getDim().visit(this, arg);
                    javaP.comma().append("BufferedImage.TYPE_INT_RGB");
                    javaP.rParen().semiEnd();
                }
            }
            else {
                if (declaration.getOp() == null){
                    javaP.semiEnd();
                }
                else {
                    if (checkType(declaration.getExpr(), IMAGE)) {
                        javaP.append(" = ");
                        declaration.getExpr().visit(this, arg);
                        javaP.semiEnd();
                    }
                    else {
                        javaP.importer("edu.ufl.cise.plc.runtime.FileURLIO");
                        javaP.append(" = ");
                        javaP.append("FileURLIO.readImage").lParen();
                        declaration.getExpr().visit(this, arg);
                        javaP.rParen().semiEnd();
                    }
                }
            }
        }
        return javaP;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        return null;
    }
}
