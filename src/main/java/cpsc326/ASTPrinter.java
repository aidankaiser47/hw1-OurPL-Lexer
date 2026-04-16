package cpsc326;

class ASTPrinter implements Expr.Visitor<String>, Stmt.Visitor<String>{
    String print(Stmt stmt) {
        return stmt.accept(this);
    }

    @Override
    public String visitBinaryExpr(Expr.Binary expr) {
        return parenthesize(expr.operator.lexeme, expr.left, expr.right);
    }

    @Override
    public String visitGroupingExpr(Expr.Grouping expr) {
        return parenthesize("group", expr.expression);
    }

    @Override
    public String visitLiteralExpr(Expr.Literal expr) {
        if(expr.value == null) return "nil";
        return expr.value.toString();
    }

    @Override
    public String visitUnaryExpr(Expr.Unary expr) {
        return parenthesize(expr.operator.lexeme, expr.right);
    }

    @Override
    public String visitExpressionStatement(Stmt.Expression stmt) {
        return parenthesize("expr-stmt", stmt.expression);
    }

    @Override
    public String visitPrintStatement(Stmt.Print stmt) {
        return parenthesize("print", stmt.expression);
    }

    @Override
    public String visitVarDeclaration(Stmt.VarDecl stmt) {
        if (stmt.initializer != null) { // if there is an initializer, include it in the output
            return parenthesize("var " + stmt.name.lexeme, stmt.initializer);
        } else { // if there is no initializer, just print the variable name
            return parenthesize("var " + stmt.name.lexeme);
        }
    }

    private String parenthesize(String name, Expr... exprs) {
        StringBuilder builder = new StringBuilder();
        builder.append("(").append(name);
        for(Expr expr : exprs) {
            builder.append(" ");
            builder.append(expr.accept(this));
        }
        builder.append(")");

        return builder.toString();
    }
}
