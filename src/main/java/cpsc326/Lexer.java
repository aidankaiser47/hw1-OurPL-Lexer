package cpsc326;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cpsc326.TokenType.*;

class Lexer {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords;

    Lexer(String source) {
        this.source = source;
    }

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("or", OR);
        keywords.put("struct", STRUCT);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("true", TRUE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("this", THIS);
        keywords.put("while", WHILE);
        keywords.put("var", VAR);
        
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        return source.charAt(current++);
    }

    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != expected) return false;
        current++;
        return true;
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

     private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private boolean isAlphaNumeric(char c) {
        return c == '_' || isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type,null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private void string() {
        // may use this for error reporting, but not allowed with current tests
        // int stringStartLine = line;

        while (peek() != '"' && !isAtEnd()) { // if not string end or file end
            if (peek() == '\n') {
                line++; // if string has a newline
            }
            
            advance();
        }

        if (isAtEnd()) {
            System.err.println("Unterminated string.");
            OurPL.hadError = true;
            return;
        }

        advance();

        addToken(STRING, source.substring(start + 1, current - 1));
    }

    private void number() {
        while (isDigit(peek())) { // take in the whole number
            advance();
        }

        // look for a fractional part. ensures not just a dot after the number
        if (peek() == '.' && isDigit(peekNext())) {
            advance(); // take in the '.'
            while (isDigit(peek())) {
                advance();
            }
        }

        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }


    private void identifier() {
        if (source.charAt(start) == '_') { // cant start identifier with underscore
            System.err.println("Unexpected character.");
            OurPL.hadError = true;
            return;
        }

        while (isAlphaNumeric(peek())) { // take in whole identifier
            advance();
        }

        String text = source.substring(start, current); 
        TokenType type = keywords.get(text); // check if identifier matches a keyword
        if (type == null) { // if not, identifier
            type = IDENTIFIER;
        }

        addToken(type);
    }

    private void scanToken() {
        char curr_char = advance();
        switch (curr_char) {
            case '(': 
                addToken(LEFT_PAREN);
                break;

            case ')': 
                addToken(RIGHT_PAREN);
                break;

            case '{': 
                addToken(LEFT_BRACE);
                break;

            case '}': 
                addToken(RIGHT_BRACE);
                break;

            case ',': 
                addToken(COMMA);
                break;

            case '.': 
                addToken(DOT);
                break;

            case '/': 
                addToken(SLASH);
                break;

            case '-': 
                addToken(MINUS);
                break;

            case '+': 
                addToken(PLUS);
                break;

            case ';': 
                addToken(SEMICOLON);
                break;

            case '*': 
                addToken(STAR);
                break;

            case '!': // if next is an equal, return BANG_EQUAL, else return BANG
                addToken(match('=') ? BANG_EQUAL : BANG);
                break;

            case '=': // if next is an equal, return EQUAL_EQUAL, else return EQUAL
                addToken(match('=') ? EQUAL_EQUAL : EQUAL);
                break;

            case '<': // if next is an equal, return LESS_EQUAL, else return LESS
                addToken(match('=') ? LESS_EQUAL : LESS);
                break;

            case '>': // if next is an equal, return GREATER_EQUAL, else return GREATER
                addToken(match('=') ? GREATER_EQUAL : GREATER);
                break;

            case '#': // comment, ignores the rest of the line
                while (peek() != '\n' && !isAtEnd()) {
                    advance();
                }
                break;

            // whitespace gets ignored
            case ' ': //space
            case '\r': // carriage return - like a newline but doesn't increment line number
            case '\t': // tab
                break;

            case '\n': // on newline, increment line number, then go next
                line++;
                break;
            
            case '\"': // start of a string
                string();
                break;

            default:
                    if (isDigit(curr_char)) { // number
                        number();
                    } 
                    else if (isAlpha(curr_char) || curr_char == '_') {
                        identifier();
                    } 
                    else {
                        // unexpected character
                        System.err.println("Unexpected character.");
                        OurPL.hadError = true;
                    }
                break;
            
        }
    }
}
