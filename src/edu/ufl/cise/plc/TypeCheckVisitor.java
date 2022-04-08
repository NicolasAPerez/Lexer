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

public class TypeCheckVisitor implements ASTVisitor {
	//TODO: Fix all the exception throwing messages to be more helpful
	SymbolTable symbolTable = new SymbolTable();  
	Program root;
	
	record Pair<T0,T1>(T0 t0, T1 t1){};  //may be useful for constructing lookup tables.
	
	private void check(boolean condition, ASTNode node, String message) throws TypeCheckException {
		if (!condition) {
			throw new TypeCheckException(message, node.getSourceLoc());
		}
	}

	private boolean compatibleAssignmentNotIMG(Type variable, Type expression){
		return (variable == expression) ||
				(variable == INT && expression == FLOAT) ||
				(variable == FLOAT && expression == INT) ||
				(variable == COLOR && expression == INT) ||
				(variable == INT && expression == COLOR);

	}
	
	//The type of a BooleanLitExpr is always BOOLEAN.  
	//Set the type in AST Node for later passes (code generation)
	//Return the type for convenience in this visitor.  
	@Override
	public Object visitBooleanLitExpr(BooleanLitExpr booleanLitExpr, Object arg) throws Exception {
		booleanLitExpr.setType(Type.BOOLEAN);
		return Type.BOOLEAN;
	}

	@Override
	public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws Exception {
		stringLitExpr.setType(STRING);
		return STRING;
	}

	@Override
	public Object visitIntLitExpr(IntLitExpr intLitExpr, Object arg) throws Exception {
		intLitExpr.setType(Type.INT);
		return Type.INT;
	}

	@Override
	public Object visitFloatLitExpr(FloatLitExpr floatLitExpr, Object arg) throws Exception {
		floatLitExpr.setType(Type.FLOAT);
		return Type.FLOAT;
	}

	@Override
	public Object visitColorConstExpr(ColorConstExpr colorConstExpr, Object arg) throws Exception {
		colorConstExpr.setType(COLOR);
		return COLOR;
	}

	@Override
	public Object visitConsoleExpr(ConsoleExpr consoleExpr, Object arg) throws Exception {
		consoleExpr.setType(Type.CONSOLE);
		return Type.CONSOLE;
	}
	
	//Visits the child expressions to get their type (and ensure they are correctly typed)
	//then checks the given conditions.
	@Override
	public Object visitColorExpr(ColorExpr colorExpr, Object arg) throws Exception {
		Type redType = (Type) colorExpr.getRed().visit(this, arg);
		Type greenType = (Type) colorExpr.getGreen().visit(this, arg);
		Type blueType = (Type) colorExpr.getBlue().visit(this, arg);
		check(redType == greenType && redType == blueType, colorExpr, "color components must have same type");
		check(redType == Type.INT || redType == Type.FLOAT, colorExpr, "color component type must be int or float");
		Type exprType = (redType == Type.INT) ? Type.COLOR : Type.COLORFLOAT;
		colorExpr.setType(exprType);
		return exprType;
	}	

	
	
	//Maps forms a lookup table that maps an operator expression pair into result type.  
	//This more convenient than a long chain of if-else statements. 
	//Given combinations are legal; if the operator expression pair is not in the map, it is an error. 
	Map<Pair<Kind,Type>, Type> unaryExprs = Map.of(
			new Pair<Kind,Type>(Kind.BANG,BOOLEAN), BOOLEAN,
			new Pair<Kind,Type>(Kind.MINUS, FLOAT), FLOAT,
			new Pair<Kind,Type>(Kind.MINUS, INT),INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,INT), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,COLOR), INT,
			new Pair<Kind,Type>(Kind.COLOR_OP,IMAGE), IMAGE,
			new Pair<Kind,Type>(Kind.IMAGE_OP,IMAGE), INT
			);
	
	//Visits the child expression to get the type, then uses the above table to determine the result type
	//and check that this node represents a legal combination of operator and expression type. 
	@Override
	public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws Exception {
		// !, -, getRed, getGreen, getBlue
		Kind op = unaryExpr.getOp().getKind();
		Type exprType = (Type) unaryExpr.getExpr().visit(this, arg);
		//Use the lookup table above to both check for a legal combination of operator and expression, and to get result type.
		Type resultType = unaryExprs.get(new Pair<Kind,Type>(op,exprType));
		check(resultType != null, unaryExpr, "incompatible types for unaryExpr");
		//Save the type of the unary expression in the AST node for use in code generation later. 
		unaryExpr.setType(resultType);
		//return the type for convenience in this visitor.
		return resultType;
	}


	//This method has several cases. Work incrementally and test as you go. 
	@Override
	public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws Exception {
		Expr left = binaryExpr.getLeft();
		Type leftType = (Type) binaryExpr.getLeft().visit(this, arg);
		if (left instanceof IdentExpr){
			IdentExpr temp = (IdentExpr) left;
			check(temp.getDec().isInitialized(), binaryExpr, "Var used as value not initialized");
		}

		Expr right = binaryExpr.getRight();
		Type rightType = (Type) binaryExpr.getRight().visit(this, arg);
		if (right instanceof IdentExpr){
			IdentExpr temp = (IdentExpr) right;
			check(temp.getDec().isInitialized(), binaryExpr, "Var used as value not initialized");
		}

		Kind operator = binaryExpr.getOp().getKind();

		switch (operator){
			case AND, OR ->{
				check(leftType == BOOLEAN, binaryExpr, "Left Type is not Boolean!");
				check(rightType == BOOLEAN, binaryExpr, "Right Type is not Boolean!");
				binaryExpr.setType(BOOLEAN);
			}
			case EQUALS, NOT_EQUALS ->{
				check(leftType == rightType, binaryExpr, "Left and Right types do not match!");
				binaryExpr.setType(BOOLEAN);
			}
			case PLUS, MINUS ->{
				if (leftType == INT && rightType == INT){
					binaryExpr.setType(INT);
				}
				else if ((leftType == FLOAT && rightType == FLOAT) || (leftType == INT && rightType == FLOAT) || (rightType == INT && leftType == FLOAT)){
					if (leftType == INT) left.setCoerceTo(FLOAT);
					if (rightType == INT) right.setCoerceTo(FLOAT);
					binaryExpr.setType(FLOAT);
				}
				else if (leftType == COLOR && rightType == COLOR){
					binaryExpr.setType(COLOR);
				}
				else if ((leftType == COLORFLOAT && rightType == COLORFLOAT) || (leftType == COLOR && rightType == COLORFLOAT) || (rightType == COLOR && leftType == COLORFLOAT)){
					if (leftType == COLOR) left.setCoerceTo(COLORFLOAT);
					if (rightType == COLOR) right.setCoerceTo(COLORFLOAT);
					binaryExpr.setType(COLORFLOAT);
				}
				else{
					check(leftType == IMAGE && rightType == IMAGE, binaryExpr, "Types aren't compatible with operator");
					binaryExpr.setType(IMAGE);
				}
			}
			case TIMES, DIV, MOD ->{
				if (leftType == INT && rightType == INT){
					binaryExpr.setType(INT);
				}
				else if ((leftType == FLOAT && rightType == FLOAT) || (leftType == INT && rightType == FLOAT) || (rightType == INT && leftType == FLOAT)){
					if (leftType == INT) left.setCoerceTo(FLOAT);
					if (rightType == INT) right.setCoerceTo(FLOAT);
					binaryExpr.setType(FLOAT);
				}
				else if ((leftType == COLOR && rightType == COLOR) || (leftType == INT && rightType == COLOR) || (leftType == COLOR && rightType == INT)){
					if (leftType == INT) left.setCoerceTo(COLOR);
					if (rightType == INT) right.setCoerceTo(COLOR);
					binaryExpr.setType(COLOR);
				}
				else if ((leftType == COLORFLOAT && rightType == COLORFLOAT) || (leftType == COLOR && rightType == COLORFLOAT) || (rightType == COLOR && leftType == COLORFLOAT)){
					if (leftType == COLOR) left.setCoerceTo(COLORFLOAT);
					if (rightType == COLOR) right.setCoerceTo(COLORFLOAT);
					binaryExpr.setType(COLORFLOAT);
				}
				else if ((leftType == IMAGE && rightType == IMAGE) || (leftType == IMAGE && rightType == INT) || (leftType == IMAGE && rightType == FLOAT)){
					binaryExpr.setType(IMAGE);
				}
				else if ((leftType == FLOAT && rightType == COLOR) || (leftType == COLOR && rightType == FLOAT)){
					left.setCoerceTo(COLORFLOAT);
					right.setCoerceTo(COLORFLOAT);
					binaryExpr.setType(COLORFLOAT);
				}
			}
			case LT, LE, GT, GE ->{
				if (leftType == INT && rightType == INT){
					binaryExpr.setType(BOOLEAN);
				}
				else if ((leftType == FLOAT && rightType == FLOAT) || (leftType == INT && rightType == FLOAT) || (rightType == INT && leftType == FLOAT)){
					if (leftType == INT) left.setCoerceTo(FLOAT);
					if (rightType == INT) right.setCoerceTo(FLOAT);
					binaryExpr.setType(BOOLEAN);
				}
			}
			default -> {
				throw new TypeCheckException("Unknown operator in BinaryExpr");
			}
		}
		return binaryExpr.getType();
	}

	@Override
	public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws Exception{
		if (!symbolTable.existsItem(identExpr.getFirstToken().getText())){
			throw new TypeCheckException("ERROR: Ident has not been declared!");
		}
		identExpr.setDec(symbolTable.lookupItem(identExpr.getFirstToken().getText()));
		identExpr.setType(identExpr.getDec().getType());
		return identExpr.getDec().getType();
	}

	@Override
	public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws Exception {
		Type condition = (Type) conditionalExpr.getCondition().visit(this, arg);
		Type trueCase = (Type) conditionalExpr.getTrueCase().visit(this, arg);
		Type falseCase = (Type) conditionalExpr.getFalseCase().visit(this, arg);

		check(condition == BOOLEAN, conditionalExpr, "Type of Condition must be boolean");
		check(trueCase == falseCase, conditionalExpr, "Type of true case does not equal the type of the false case");
		conditionalExpr.setType(trueCase);
		return conditionalExpr.getType();

	}

	@Override
	public Object visitDimension(Dimension dimension, Object arg) throws Exception {
		Type heightType = (Type) dimension.getHeight().visit(this, arg);
		check(heightType == INT,dimension.getHeight(), "Only INT can be Dimension components");
		Type widthType = (Type) dimension.getWidth().visit(this, arg);
		check(widthType == INT,dimension.getHeight(), "Only INT can be Dimension components");
		return null;
	}

	@Override
	//This method can only be used to check PixelSelector objects on the right hand side of an assignment. 
	//Either modify to pass in context info and add code to handle both cases, or when on left side
	//of assignment, check fields from parent assignment statement.
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		Type xType = (Type) pixelSelector.getX().visit(this, arg);
		check(xType == Type.INT, pixelSelector.getX(), "only ints as pixel selector components");
		Type yType = (Type) pixelSelector.getY().visit(this, arg);
		check(yType == Type.INT, pixelSelector.getY(), "only ints as pixel selector components");
		return null;
	}

	@Override
	//This method several cases--you don't have to implement them all at once.
	//Work incrementally and systematically, testing as you go.  
	public Object visitAssignmentStatement(AssignmentStatement assignmentStatement, Object arg) throws Exception {
		if (!symbolTable.existsItem(assignmentStatement.getName())){
				throw new TypeCheckException("Read statement variable has not been declared!");
		}
		assignmentStatement.setTargetDec(symbolTable.lookupItem(assignmentStatement.getName()));
		Type targetType = assignmentStatement.getTargetDec().getType();
		Type sourceType;
		assignmentStatement.getTargetDec().setInitialized(true);

		if (assignmentStatement.getExpr() instanceof IdentExpr){
			sourceType = (Type) assignmentStatement.getExpr().visit(this, arg);
			IdentExpr temp = (IdentExpr) assignmentStatement.getExpr();
			check(temp.getDec().isInitialized(), assignmentStatement, "Var used as value not initialized");
		}

		if (targetType != IMAGE){
			sourceType = (Type) assignmentStatement.getExpr().visit(this, arg);

			check(assignmentStatement.getSelector() == null, assignmentStatement, "Non-Image types can not have a pixel selector!");

			if (targetType != sourceType && compatibleAssignmentNotIMG(targetType, sourceType)){
				assignmentStatement.getExpr().setCoerceTo(targetType);
			}
			else {
				check(sourceType == targetType, assignmentStatement, "Dec types are not compatible");
			}
		}
		else if (assignmentStatement.getSelector() == null){
			sourceType = (Type) assignmentStatement.getExpr().visit(this, arg);


			switch (sourceType){
				case INT -> {
					assignmentStatement.getExpr().setCoerceTo(COLOR);
				}
				case FLOAT -> {
					assignmentStatement.getExpr().setCoerceTo(COLORFLOAT);
				}
				case COLOR, COLORFLOAT, IMAGE ->{

				}
				default -> {
					throw new TypeCheckException("Source type not compatible with Image!");
				}
			}
		}
		else {
			//Can not visit pixel selector since the variables are implictly declared rather than explicit
			//assignmentStatement.getSelector().visit(this, arg);
			Expr leftTemp = assignmentStatement.getSelector().getX();
			Expr rightTemp = assignmentStatement.getSelector().getY();

			check(leftTemp instanceof IdentExpr && rightTemp instanceof IdentExpr, assignmentStatement, "Pixel Selector X and Y must be Idents");
			IdentExpr left = (IdentExpr) leftTemp;
			IdentExpr right = (IdentExpr) rightTemp;
			left.setType(INT);
			right.setType(INT);
			check(!symbolTable.existsItem( left.getFirstToken().getText()) && !symbolTable.existsItem(right.getFirstToken().getText()), assignmentStatement, "Variable already exists");

			NameDef leftTempDef = new NameDef(left.getFirstToken(), "int", left.getFirstToken().getText());
			NameDef rightTempDef = new NameDef(rightTemp.getFirstToken(), "int", right.getFirstToken().getText());

			leftTempDef.setInitialized(true);
			rightTempDef.setInitialized(true);
			symbolTable.addItem(left.getFirstToken().getText(), leftTempDef);
			symbolTable.addItem(right.getFirstToken().getText(), rightTempDef);

			sourceType = (Type) assignmentStatement.getExpr().visit(this, arg);

			switch (sourceType){
				case INT,FLOAT,COLORFLOAT -> {
					assignmentStatement.getExpr().setCoerceTo(COLOR);
				}
				case COLOR ->{

				}
				default -> {
					throw new TypeCheckException("Source type not compatible with Image!");
				}
			}
			symbolTable.removeItem(left.getFirstToken().getText());
			symbolTable.removeItem(right.getFirstToken().getText());


		}
		return assignmentStatement.getTargetDec().getType();
	}


	@Override
	public Object visitWriteStatement(WriteStatement writeStatement, Object arg) throws Exception {
		Type sourceType = (Type) writeStatement.getSource().visit(this, arg);
		Type destType = (Type) writeStatement.getDest().visit(this, arg);
		check(destType == Type.STRING || destType == Type.CONSOLE, writeStatement,
				"illegal destination type for write");
		check(sourceType != Type.CONSOLE, writeStatement, "illegal source type for write");
		return null;
	}

	@Override
	public Object visitReadStatement(ReadStatement readStatement, Object arg) throws Exception {
		if (!symbolTable.existsItem(readStatement.getName())){
			throw new TypeCheckException("Read statement variable has not been declared!");
		}
		Type targetType = symbolTable.lookupItem(readStatement.getName()).getType();

		Type sourceType = (Type) readStatement.getSource().visit(this, arg);
		if (readStatement.getSelector() != null){
			throw new TypeCheckException("Read Statements cannot have a Pixel Selector");
		}
		check(sourceType == STRING || sourceType == CONSOLE, readStatement, "Read statement source must be String or Console");

		if (sourceType == CONSOLE){
			readStatement.getSource().setCoerceTo(targetType);
		}

		symbolTable.lookupItem(readStatement.getName()).setInitialized(true);
		return null;
	}

	@Override
	public Object visitVarDeclaration(VarDeclaration declaration, Object arg) throws Exception {
		Type nameDefType = (Type) declaration.getNameDef().visit(this, arg);
		Type initialType;
		if (declaration.getOp() != null){
			//If it has an operator and therefore an initializer

			initialType = (Type) declaration.getExpr().visit(this, arg);
			declaration.getNameDef().setInitialized(true);

			if (nameDefType == IMAGE && initialType != IMAGE){
				throw new TypeCheckException("ERROR: Image has an initializer but initializer is not of type Image");
			}

			if (declaration.getOp().getKind() == Kind.LARROW){
				//Treat as Read Statement
				check(initialType == STRING || initialType == CONSOLE, declaration, "Read declaration source must be String or Console");

				if (initialType == CONSOLE){
					declaration.getExpr().setCoerceTo(nameDefType);
				}


			}
			else if (declaration.getOp().getKind() == Kind.ASSIGN){
				//Treat as Assign Statement
				if (declaration.getExpr() instanceof IdentExpr){
					IdentExpr temp = (IdentExpr) declaration.getExpr();
					check(temp.getDec().isInitialized(), declaration, "Var used as value not initialized");
				}

				if (initialType != nameDefType && compatibleAssignmentNotIMG(nameDefType, initialType)){
					declaration.getExpr().setCoerceTo(nameDefType);
				}
				else {
					check(initialType == nameDefType, declaration, "Dec types are not compatible");
				}


			}
			else {
				throw new TypeCheckException("Declaration has an Operator but is not of kind ASSIGN or LARROW");
			}
		}
		else {
			//If the Dec has no Initializer
			if (nameDefType == IMAGE){
				check(declaration.getDim() != null,declaration,"ERROR: Image has no initializer and no dimension");
				declaration.getDim().visit(this, arg);
			}

		}
		return nameDefType;
	}


	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {		

		//Save root of AST so return type can be accessed in return statements
		root = program;
		symbolTable.addItem(program.getName(), program);

		List<NameDef> parameters = program.getParams();
		for (NameDef node : parameters){
			node.visit(this,arg);
			node.setInitialized(true);
		}
		
		//Check declarations and statements
		List<ASTNode> decsAndStatements = program.getDecsAndStatements();
		for (ASTNode node : decsAndStatements) {
			node.visit(this, arg);
		}
		return program;
	}

	@Override
	public Object visitNameDef(NameDef nameDef, Object arg) throws Exception {
		Type nameDefType = nameDef.getType();

		if (!symbolTable.addItem(nameDef.getName(), nameDef)){
			throw new TypeCheckException("Global Variable already exists");
		}
		return nameDefType;
	}

	@Override
	public Object visitNameDefWithDim(NameDefWithDim nameDefWithDim, Object arg) throws Exception {
		Type nameDefType = nameDefWithDim.getType();
		nameDefWithDim.getDim().visit(this, arg);
		if (!symbolTable.addItem(nameDefWithDim.getName(), nameDefWithDim)){
			throw new TypeCheckException("Global Variable already exists");
		}
		return nameDefType;
	}
 
	@Override
	public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws Exception {
		Type returnType = root.getReturnType();  //This is why we save program in visitProgram.
		Type expressionType = (Type) returnStatement.getExpr().visit(this, arg);

		if (returnStatement.getExpr() instanceof IdentExpr){
			IdentExpr temp = (IdentExpr) returnStatement.getExpr();
			check(temp.getDec().isInitialized(), returnStatement, "Var used as value not initialized");
		}

		check(returnType == expressionType, returnStatement, "return statement with invalid type");
		return returnType;
	}

	@Override
	public Object visitUnaryExprPostfix(UnaryExprPostfix unaryExprPostfix, Object arg) throws Exception {
		Type expType = (Type) unaryExprPostfix.getExpr().visit(this, arg);
		check(expType == Type.IMAGE, unaryExprPostfix, "pixel selector can only be applied to image");
		unaryExprPostfix.getSelector().visit(this, arg);
		unaryExprPostfix.setType(Type.INT);
		unaryExprPostfix.setCoerceTo(COLOR);
		return Type.COLOR;
	}

}
