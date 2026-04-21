package cpsc326;

import java.util.List;

abstract class Stmt {

    // handles all types of statements in OurPL
        interface Visitor<R> {
            R visitExpressionStatement(Stmt.Expression stmt);
            R visitPrintStatement(Stmt.Print stmt);
            R visitVarDeclaration(Stmt.Var stmt);
            R visitBlockStatement(Stmt.Block stmt);
            R visitIfStatement(Stmt.If stmt);
            R visitWhileStatement(Stmt.While stmt);
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

        static class Var extends Stmt {
            final Token name;
            final Expr initializer;

            Var(Token id, Expr expr) {
                name = id;
                initializer = expr;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitVarDeclaration(this);
            }
        }

        static class Block extends Stmt {
            final List<Stmt> statements;

            Block(List<Stmt> stmts) {
                statements = stmts;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitBlockStatement(this);
            }
        }

        static class If extends Stmt {
            final Expr condition;
            final Stmt thenBranch;
            final Stmt elseBranch;

            If(Expr cond, Stmt thenBr, Stmt elseBr) {
                condition = cond;
                thenBranch = thenBr;
                elseBranch = elseBr;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitIfStatement(this);
            }
        }

        // handles both While and For statements
        static class While extends Stmt {
            final Expr condition;
            final Stmt body;

            While(Expr cond, Stmt body) {
                condition = cond;
                this.body = body;
            }

            @Override
            <R> R accept(Visitor<R> visitor) {
                return visitor.visitWhileStatement(this);
            }
        }

        abstract <R> R accept(Visitor<R> visitor);
    }