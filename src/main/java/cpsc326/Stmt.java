package cpsc326;

import java.util.List;

abstract class Stmt {
// Block : List<Stmt>
// Expression : Expr             DONE
// If : Expr, Stmt, Stmt
// Print : Stmt                  DONE
// Var : Token, Expr             DONE
// While : Expr, Stmt
        interface Visitor<R> {
            R visitExpressionStatement(Stmt.Expression stmt);
            R visitPrintStatement(Stmt.Print stmt);
            R visitVarDeclaration(Stmt.VarDecl stmt);
        }

        static class Expression extends Stmt {
            final Expr expression;

            Expression(Expr expr) {
                expression = expr;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitExpressionStatement(this);
            }
        }

        static class Print extends Stmt {
            final Expr expression;

            Print(Expr expr) {
                expression = expr;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitPrintStatement(this);
            }
        }

        static class VarDecl extends Stmt {
            final Token name;
            final Expr initializer;

            VarDecl(Token id, Expr expr) {
                name = id;
                initializer = expr;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitVarDeclaration(this);
            }
        }

        abstract <R> R accept(Visitor<R> visitor);
    }