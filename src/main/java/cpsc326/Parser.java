package cpsc326;

import java.util.List;
import java.util.ArrayList;
import static cpsc326.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException{ }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        try {
            List<Stmt> declarations = new ArrayList<Stmt>();
            while (!isAtEnd()) {
                declarations.add(declaration());
            }
            return declarations;
        } catch (ParseError error) {
            return null;
        }
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) { // if match on VAR, then it is a variable declaration
                return varDeclaration();
            }
            return statement(); // else, a basic statement
        } 
        catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name."); // consumes variable name

        Expr initializer = null; // option to have initializer. If there is not one, it will be null
        if (match(EQUAL)) { // if there is an equal sign, then there is an initializer
            initializer = expression();
        }

        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.VarDecl(name, initializer);
    }

    private Stmt statement() {
        // WILL NEED TO ADD OTHER TYPES OF STATEMENTS STILL

        if (match(PRINT)) { // if the next token is print, consume it and return a print statement
            return printStatement();
        }
        else {
            return expressionStatement();
        }
    }

    private Stmt printStatement() {
        Expr expr = expression(); // converts the expression after print into an Expr object
        if (match(SEMICOLON)) {  // if there is a semicolon, consume it and return the print statement with the expression
            return new Stmt.Print(expr);
        }
        throw error(peek(), "Expect ';' after value.");
    }

    private Stmt expressionStatement() {
        Expr expr = expression(); // converts the expression into an Expr object, which will be used in the expression statement
        if (match(SEMICOLON)) { // if there is a semicolon, consume it and return the expression statement
            return new Stmt.Expression(expr);
        }
        throw error(peek(), "Expect ';' after expression.");
    }

    private Expr expression() {
        // converts into assignment
        return assignment();
    }

    private Expr assignment() {
        // either an identifier = assignment, or a logical or
        if (match(EQUAL)) {
            Token name = previous();
            Expr value = assignment();
            return new Expr.Assign(name, value);
        }
        return logicalOr();

    }

    private Expr logicalOr() {
        // converts to a logical and, with potentially or 0 or more times
        Expr expr = logicalAnd();
        while (match(OR)) { 
            Token operator = previous();
            Expr right = logicalAnd(); // set the right side as another logical and
            expr = new Expr.Logical(expr, operator, right); // creates a new logical expression with all the goods
        }
        return expr;
    }

    private Expr logicalAnd() {
        //converts to an equality, with a potential for and 0 or more times
        Expr expr = equality();
        while (match(AND)) { 
            Token operator = previous();
            Expr right = equality(); // set the right side as another equality
            expr = new Expr.Logical(expr, operator, right); // creates a new logical expression with all the goods
        }
        return expr;
    }
    private Expr equality() {
        // converts to a comparison, with potentially == or != 0 or more times
        Expr expr = comparison();
        while (match (BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous(); // get operator
            Expr right = comparison(); // set the right side as a comparison
            expr = new Expr.Binary(expr, operator, right); // create a binary (comparison == more comparison)
        }
        return expr;
    }

    private Expr comparison() {
        // converts to a term, with potentially >, >=, < or <= 0 or more times
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) { // matches on all possible equality operators
            Token operator = previous(); // if a match, consume the operator
            Expr right = term(); // set the right side as another term
            expr = new Expr.Binary(expr, operator, right); // create our new binary object
            // binaries work here because we can nest them, so even if there are more than 2 terms, 
            // each still has left and right with an operator.
        }
        return expr;
    }

    private Expr term() {
        // converts to a factor, with potentially + or - 0 or more times
        Expr expr = factor();
        while (match(PLUS, MINUS)) { // match on + or -
            Token operator = previous(); // if a match, consume the operator
            Expr right = factor(); // sets right side as another factor
            expr = new Expr.Binary(expr, operator, right); // creates new binary object with all the goods
        }
        return expr;
    }

    private Expr factor() {
        // converts to a unary object, with potentiall / or * 0 or more times
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous(); // if a match, consume the operator
            Expr right = unary(); // sets right side as another unary
            expr = new Expr.Binary(expr, operator, right); // creates new binary object with all the goods
        }
        return expr;
    }

    private Expr unary() {
        // converts to either another unary with a ! or - in front, or a primary
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary(); // when we run this function, it will check again for another ! or -, so there can be multiple
            return new Expr.Unary(operator, right); // creates a unary object this time because there is only 1 term
        }
        return primary(); // if it does not match on the operator, it will be a primary
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Missing ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
    }

    private boolean match (TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    
    private Token consume(TokenType type, String message) {
        if(check(type)) return advance();

        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        OurPL.error(token.line, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();
        while(!isAtEnd()) {
            if (previous().type == SEMICOLON) return;
            switch(peek().type) {
                case STRUCT:
                case FOR:
                case FUN:
                case IF:
                case PRINT:
                case RETURN:
                case VAR:
                case WHILE:
                    return;
            }
            
            advance();
        }
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if(!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }
}
