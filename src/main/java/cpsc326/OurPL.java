package cpsc326;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class OurPL {
    
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: OurPl [script]");
            System.exit(64);
        }
        else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }
    
    public static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
    }

    static boolean hadError = false;
    static boolean hadRuntimeError = false;

    public static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);

        for (;;) {
            System.out.print("> ");
            String line = reader.readLine();
            if(line == null) {
                break;
            }
            run(line);
            hadError = false;
        }
    }

    public static void run(String source) {
        // runs the lexer
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();

        // removed for now, no need to print each token anymore.
        // for (Token token : tokens){
        //     System.out.println(token);
        // }

        // runs the parser
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        // ASTPrinter printer = new ASTPrinter();
        // for (Stmt statement : statements) {
        //     System.out.println(printer.print(statement));
        // }

        // runs the interpreter
        Interpreter interpreter = new Interpreter();
        interpreter.interpret(statements);
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void runtimeError(RuntimeError error) {
        report(error.token.line, "", error.getMessage());
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }
}
