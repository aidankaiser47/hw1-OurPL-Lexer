package cpsc326;

import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    private Environment environment = new Environment();

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) { // goes through our parser statements
                execute(statement); // executes one by one
            }
        } catch (RuntimeError error) {
            OurPL.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        if (stmt != null) {
        stmt.accept(this);
        }
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
        }

        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name); // returns variable value from env
    }
    
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value); // sets variable value in env
        return value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left); // looks at left first for short-circuiting

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) {
                return left; // short circtuit: return left if true
            }
        } 
        else {
            if (!isTruthy(left)) {
                return left; // short-circuit: return false if left is false
            }
        }

        return evaluate(expr.right);
    }

    @Override
    public Void visitExpressionStatement(Stmt.Expression stmt) {
        evaluate(stmt.expression); // evaluate the expression but ignore the result
        return null;
    }

    @Override
    public Void visitPrintStatement(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression); // evaluate the expression and print the result
        System.out.print(stringify(value) + "\n"); // use stringify to convert the value to a string before printing
        return null;
    }

    @Override
    public Void visitVarDeclaration(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) { // sets var value to the initialized value if it eists
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitBlockStatement(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment)); // creates a new environment for the block, with the current environment as its enclosing environment
        return null;
    }

    @Override
    public Void visitIfStatement(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) { // if the condition is true, execute the then branch
            execute(stmt.thenBranch);
        }
        else if (stmt.elseBranch != null) { // if the condition is false and there is an else branch, execute the else branch
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStatement(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) { // while the condition is true, execute the body of the while loop
            execute(stmt.body);
        }
        return null;
    }

    void executeBlock(List<Stmt> statements, Environment env) {
        Environment previous = this.environment; // save the previous environment
        try {
            this.environment = env; // set the current environment to the new environment
            for (Stmt statement : statements) { // execute each statement in the block
                if (statement != null) {
                    execute(statement);
                }
            }
        } 
        finally { // runs this even if the try throws an execption
            this.environment = previous; // restore the previous environment after executing the block
        }
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator,"Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator,"Operands must be numbers.");
    }

    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean)object;
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null) return true;
        if (left == null) return false;

        return left.equals(right);
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }

        if (object instanceof Double) {
            String text = object.toString();
            if(text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type){
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;
            case BANG_EQUAL:
                return !isEqual(left,right);
            case EQUAL_EQUAL:
                return isEqual(left,right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double)left + (double)right;
                }
                if (left instanceof String && right instanceof String) {
                    return (String)left + (String)right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;
            
        }

        return null;
    }
}
