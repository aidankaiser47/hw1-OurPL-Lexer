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
        List<Stmt> declarations = new ArrayList<Stmt>(); // everything is now a statement
        while (!isAtEnd()) {
            Stmt stmt = declaration();
                declarations.add(stmt);
        }
        return declarations; // returns list for use in interpreter
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
        return new Stmt.Var(name, initializer);
    }

    private Stmt statement() {
        if (match(IF)) {
            return ifStatement();
        }

        if (match(FOR)) {
            return forStatement();
        }

        if (match(WHILE)) {
            return whileStatement();
        }

        if (match(LEFT_BRACE)) {
            return new Stmt.Block(block());
        }

        if (match(PRINT)) {
            return printStatement();
        }

        return expressionStatement();
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'for'.");

        // initializer part
        Stmt initializer; // different cases to check
        if (match(SEMICOLON)) {
            initializer = null; // no initializer: for (; ...)
        } 
        else if (match(VAR)) {
            initializer = varDeclaration(); // for (var i = 0; ...)
        } 
        else {
            initializer = expressionStatement(); // for (i = 0; ...)
        }

        // condition part
        Expr condition = null;
        if (!check(SEMICOLON)) { // if semicolon, no condition
            condition = expression();
        }
        consume(SEMICOLON, "Expect ';' after loop condition.");

        // increment part
        Expr increment = null;
        if (!check(RIGHT_PAREN)) { // if right paren, no increment included
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expect ')' after for clauses.");

        Stmt body = statement();

        // desugaring
        if (increment != null) { // if theres an increment section...
            List<Stmt> bodyWithIncrement = new ArrayList<>(); // block with body and increment part
            bodyWithIncrement.add(body);
            bodyWithIncrement.add(new Stmt.Expression(increment));
            body = new Stmt.Block(bodyWithIncrement);
        }

        if (condition == null) { // if no condition, use true as condition for infinite loop
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body); // add it to the while loop construction

        if (initializer != null) { // if theres an itializer...
            List<Stmt> outerBlock = new ArrayList<>(); // block with initializer and while loop
            outerBlock.add(initializer);
            outerBlock.add(body);
            body = new Stmt.Block(outerBlock);
        }

        return body; // returns a while loop statement
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<Stmt>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) { // while no right brace and not end of file...
            Stmt stmt = declaration(); // add statements into the block
                statements.add(stmt);
        }

        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
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
        Expr expr = logicalOr(); // parse left side first

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment(); // parse right side recursively
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }

        return expr;
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
        if (match(FALSE)) {
            return new Expr.Literal(false);
        }
        if (match(TRUE)) {
            return new Expr.Literal(true);
        }
        if (match(NIL)) {
            return new Expr.Literal(null);
        }

        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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
