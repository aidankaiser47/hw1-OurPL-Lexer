package cpsc326;

import java.util.List;
import static cpsc326.TokenType.*;

class Parser {
    private static class ParseError extends RuntimeException{ }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        // currently only converts into equality
        return equality();
    }

    private Expr equality() {
        // converts to a comparison, with potentially == or != 0 or more times
        Expr expr = comparison();
        while (match (BANG_EQUAL, EQUAL_EQUAL)) { // checks to see if next is != or ==
            Token operator = previous(); // if it matches, consume operator
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
        // we can match on a number, string, boolean, nil, or create a grouping with an expression 
        if (match(NUMBER, STRING, TRUE, FALSE, NIL)) {
            // if we match on these tokens, we make a new literal object using the 'literal' part of the token.
            // good thing we have that
            return new Expr.Literal(previous().literal);
        }
        if (match(LEFT_PAREN)) { // if we see a parenthesis, assume its a grouping
            Expr expr = expression(); // grab the expression from inside
            consume(RIGHT_PAREN, "Missing ')' after expression."); // check if parentheses end.
            return new Expr.Grouping(expr); // create the grouping object with the expression inside
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
