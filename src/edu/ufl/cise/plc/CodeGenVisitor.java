package edu.ufl.cise.plc;

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

import static edu.ufl.cise.plc.ast.Types.Type.*;

public class CodeGenVisitor implements  ASTVisitor{

    String packageName;
    CGStringBuilder javaP;

    public CodeGenVisitor(String packageName) {
        this.packageName = packageName;
        javaP = new CGStringBuilder();

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

        System.out.println(javaP.toString());

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
        return null;
    }

    @Override
    public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
        javaP.importer("edu.ufl.cise.plc.runtime.ConsoleIO");
        javaP.lParen().appendObjType(consoleExpr.getCoerceTo()).rParen().append(" ");
        javaP.append("ConsoleIO.readValueFromConsole").lParen().append("\"").append(consoleExpr.getCoerceTo().toString().toUpperCase()).append("\"").comma();
        javaP.append("\"Enter ").appendObjType(consoleExpr.getCoerceTo()).append(": \"").rParen();
        return javaP;
    }

    @Override
    public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpression, Object arg) throws Exception {
        javaP.lParen().append(unaryExpression.getOp().getText());
        unaryExpression.getExpr().visit(this,arg);
        javaP.rParen();
        return javaP;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
        javaP.lParen();
        binaryExpr.getLeft().visit(this,arg);
        javaP.append(" ").append(binaryExpr.getOp().getText()).append(" ");
        binaryExpr.getRight().visit(this, arg);
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
        javaP.lParen();
        conditionalExpr.getCondition().visit(this, arg);
        javaP.rParen().append("? ");
        conditionalExpr.getTrueCase().visit(this, arg);
        javaP.append(" : ");
        conditionalExpr.getFalseCase().visit(this, arg);
        return javaP;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
        javaP.append(assignmentStatement.getName()).append(" = ");
        assignmentStatement.getExpr().visit(this, arg);
        javaP.semiEnd();
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
        return null;
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
        if (declaration.getOp() == null){
            javaP.semiEnd();
        }
        else {
            javaP.append(" = ");
            declaration.getExpr().visit(this, arg);
            javaP.semiEnd();
        }
        return javaP;
    }

    @Override
    public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
        return null;
    }
}
