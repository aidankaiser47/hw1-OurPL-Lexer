package cpsc326;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Parser2MiniTest {

    @BeforeEach
    void resetFlags() {
        OurPL.hadError = false;
        OurPL.hadRuntimeError = false;
    }

    private List<Token> lex(String source) {
        return new Lexer(source).scanTokens();
    }

    private List<Stmt> parse(String source) {
        OurPL.hadError = false;
        return new Parser(lex(source)).parse();
    }

    private Stmt parseSingleStatement(String source) {
        List<Stmt> statements = parse(source);
        assertNotNull(statements);
        assertEquals(1, statements.size());
        assertNotNull(statements.get(0));
        return statements.get(0);
    }

    private ParseOutcome parseWithCapturedErr(String source) {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(err));
        try {
            return new ParseOutcome(parse(source), err.toString().trim());
        } finally {
            System.setErr(originalErr);
        }
    }

    private EvalOutcome interpret(String source) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
        System.setErr(new PrintStream(err));
        try {
            new Interpreter().interpret(parse(source));
            return new EvalOutcome(out.toString().trim(), err.toString().trim());
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    void parsesVariableDeclarationWithInitializer() {
        Stmt stmt = parseSingleStatement("var answer = 42;");
        assertTrue(stmt instanceof Stmt.Var);

        Stmt.Var varStmt = (Stmt.Var) stmt;
        assertEquals("answer", varStmt.name.lexeme);
        assertTrue(varStmt.initializer instanceof Expr.Literal);
        assertEquals(42.0, ((Expr.Literal) varStmt.initializer).value);
    }

    @Test
    void parsesAssignmentExpression() {
        Stmt stmt = parseSingleStatement("value = 3;");
        assertTrue(stmt instanceof Stmt.Expression);

        Expr expr = ((Stmt.Expression) stmt).expression;
        assertTrue(expr instanceof Expr.Assign);

        Expr.Assign assign = (Expr.Assign) expr;
        assertEquals("value", assign.name.lexeme);
        assertTrue(assign.value instanceof Expr.Literal);
        assertEquals(3.0, ((Expr.Literal) assign.value).value);
    }

    @Test
    void parsesIfElseStatement() {
        Stmt stmt = parseSingleStatement("if (flag) print 1; else print 2;");
        assertTrue(stmt instanceof Stmt.If);

        Stmt.If ifStmt = (Stmt.If) stmt;
        assertTrue(ifStmt.condition instanceof Expr.Variable);
        assertEquals("flag", ((Expr.Variable) ifStmt.condition).name.lexeme);
        assertTrue(ifStmt.thenBranch instanceof Stmt.Print);
        Expr thenExpr = ((Stmt.Print) ifStmt.thenBranch).expression;
        assertTrue(thenExpr instanceof Expr.Literal);
        assertEquals(1.0, ((Expr.Literal) thenExpr).value);
        assertTrue(ifStmt.elseBranch instanceof Stmt.Print);
        Expr elseExpr = ((Stmt.Print) ifStmt.elseBranch).expression;
        assertTrue(elseExpr instanceof Expr.Literal);
        assertEquals(2.0, ((Expr.Literal) elseExpr).value);
    }

    @Test
    void bindsElseToNearestIf() {
        Stmt stmt = parseSingleStatement("if (a) if (b) print 1; else print 2;");
        assertTrue(stmt instanceof Stmt.If);

        Stmt.If outer = (Stmt.If) stmt;
        assertTrue(outer.condition instanceof Expr.Variable);
        assertEquals("a", ((Expr.Variable) outer.condition).name.lexeme);
        assertNull(outer.elseBranch);
        assertTrue(outer.thenBranch instanceof Stmt.If);

        Stmt.If inner = (Stmt.If) outer.thenBranch;
        assertTrue(inner.condition instanceof Expr.Variable);
        assertEquals("b", ((Expr.Variable) inner.condition).name.lexeme);
        assertTrue(inner.thenBranch instanceof Stmt.Print);
        assertTrue(inner.elseBranch instanceof Stmt.Print);

        Expr thenExpr = ((Stmt.Print) inner.thenBranch).expression;
        assertTrue(thenExpr instanceof Expr.Literal);
        assertEquals(1.0, ((Expr.Literal) thenExpr).value);

        Expr elseExpr = ((Stmt.Print) inner.elseBranch).expression;
        assertTrue(elseExpr instanceof Expr.Literal);
        assertEquals(2.0, ((Expr.Literal) elseExpr).value);
    }

    @Test
    void parsesWhileStatement() {
        Stmt stmt = parseSingleStatement("while (x < 3) print x;");
        assertTrue(stmt instanceof Stmt.While);

        Stmt.While whileStmt = (Stmt.While) stmt;
        assertTrue(whileStmt.condition instanceof Expr.Binary);
        Expr.Binary condition = (Expr.Binary) whileStmt.condition;
        assertEquals(TokenType.LESS, condition.operator.type);
        assertTrue(condition.left instanceof Expr.Variable);
        assertEquals("x", ((Expr.Variable) condition.left).name.lexeme);
        assertTrue(condition.right instanceof Expr.Literal);
        assertEquals(3.0, ((Expr.Literal) condition.right).value);
        assertTrue(whileStmt.body instanceof Stmt.Print);
        Expr printExpr = ((Stmt.Print) whileStmt.body).expression;
        assertTrue(printExpr instanceof Expr.Variable);
        assertEquals("x", ((Expr.Variable) printExpr).name.lexeme);
    }

    @Test
    void parsesForLoopIntoInitializerAndWhile() {
        Stmt stmt = parseSingleStatement("for (var i = 0; i < 2; i = i + 1) print i;");
        assertTrue(stmt instanceof Stmt.Block);

        Stmt.Block block = (Stmt.Block) stmt;
        assertEquals(2, block.statements.size());
        assertTrue(block.statements.get(0) instanceof Stmt.Var);
        Stmt.Var initializer = (Stmt.Var) block.statements.get(0);
        assertEquals("i", initializer.name.lexeme);
        assertTrue(initializer.initializer instanceof Expr.Literal);
        assertEquals(0.0, ((Expr.Literal) initializer.initializer).value);
        assertTrue(block.statements.get(1) instanceof Stmt.While);

        Stmt.While loop = (Stmt.While) block.statements.get(1);
        assertTrue(loop.condition instanceof Expr.Binary);
        Expr.Binary condition = (Expr.Binary) loop.condition;
        assertEquals(TokenType.LESS, condition.operator.type);
        assertTrue(condition.left instanceof Expr.Variable);
        assertEquals("i", ((Expr.Variable) condition.left).name.lexeme);
        assertTrue(condition.right instanceof Expr.Literal);
        assertEquals(2.0, ((Expr.Literal) condition.right).value);
        assertTrue(loop.body instanceof Stmt.Block);

        Stmt.Block body = (Stmt.Block) loop.body;
        assertEquals(2, body.statements.size());
        assertTrue(body.statements.get(0) instanceof Stmt.Print);
        Expr printed = ((Stmt.Print) body.statements.get(0)).expression;
        assertTrue(printed instanceof Expr.Variable);
        assertEquals("i", ((Expr.Variable) printed).name.lexeme);
        assertTrue(body.statements.get(1) instanceof Stmt.Expression);
        Expr increment = ((Stmt.Expression) body.statements.get(1)).expression;
        assertTrue(increment instanceof Expr.Assign);
        Expr.Assign assign = (Expr.Assign) increment;
        assertEquals("i", assign.name.lexeme);
        assertTrue(assign.value instanceof Expr.Binary);
        Expr.Binary sum = (Expr.Binary) assign.value;
        assertEquals(TokenType.PLUS, sum.operator.type);
        assertTrue(sum.left instanceof Expr.Variable);
        assertEquals("i", ((Expr.Variable) sum.left).name.lexeme);
        assertTrue(sum.right instanceof Expr.Literal);
        assertEquals(1.0, ((Expr.Literal) sum.right).value);
    }

    @Test
    void parsesForLoopWithoutClausesAsInfiniteLoop() {
        Stmt stmt = parseSingleStatement("for (;; ) print 1;");
        assertTrue(stmt instanceof Stmt.While);

        Stmt.While whileStmt = (Stmt.While) stmt;
        assertTrue(whileStmt.condition instanceof Expr.Literal);
        assertEquals(true, ((Expr.Literal) whileStmt.condition).value);
        assertTrue(whileStmt.body instanceof Stmt.Print);
        Expr bodyExpr = ((Stmt.Print) whileStmt.body).expression;
        assertTrue(bodyExpr instanceof Expr.Literal);
        assertEquals(1.0, ((Expr.Literal) bodyExpr).value);
    }

    @Test
    void reportsMissingSemicolonAfterVarDeclaration() {
        ParseOutcome out = parseWithCapturedErr("var a = 1");
        assertTrue(OurPL.hadError);
        assertFalse(out.stderr.isBlank());
        assertNull(out.statements.get(0));
    }

    @Test
    void reportsMissingLeftParenAfterIf() {
        ParseOutcome out = parseWithCapturedErr("if true) print 1;");
        assertTrue(OurPL.hadError);
        assertFalse(out.stderr.isBlank());
        assertNull(out.statements.get(0));
    }

    @Test
    void reportsMissingSemicolonInsideForHeader() {
        ParseOutcome out = parseWithCapturedErr("for (var i = 0 i < 2; i = i + 1) print i;");
        assertTrue(OurPL.hadError);
        assertFalse(out.stderr.isBlank());
        assertNull(out.statements.get(0));
    }

    @Test
    void parserRecoversAfterBrokenDeclaration() {
        ParseOutcome out = parseWithCapturedErr("var x = ; print 2;");
        assertTrue(OurPL.hadError);
        assertEquals(2, out.statements.size());
        assertNull(out.statements.get(0));
        assertTrue(out.statements.get(1) instanceof Stmt.Print);
    }

    @Test
    void interpretsVariableAssignment() {
        EvalOutcome out = interpret("var value = 1; value = value + 2; print value;");
        assertFalse(OurPL.hadError);
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("3", out.stdout);
    }

    @Test
    void interpretsForLoop() {
        EvalOutcome out = interpret("for (var i = 0; i < 3; i = i + 1) print i;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("0\n1\n2", out.stdout);
    }

    @Test
    void interpretsForLoopWithoutIncrementWhenBodyUpdatesVariable() {
        EvalOutcome out = interpret("var i = 0; for (; i < 2; ) { print i; i = i + 1; }");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("0\n1", out.stdout);
    }

    @Test
    void logicalOrShortCircuitsDuringInterpretation() {
        EvalOutcome out = interpret("var a = true; if (a or missing) print 1;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("1", out.stdout);
        assertTrue(out.stderr.isEmpty());
    }

    @Test
    void logicalAndShortCircuitsDuringInterpretation() {
        EvalOutcome out = interpret("var a = false; if (a and missing) print 1; else print 2;");
        assertFalse(OurPL.hadRuntimeError);
        assertEquals("2", out.stdout);
        assertTrue(out.stderr.isEmpty());
    }

    private static final class ParseOutcome {
        final List<Stmt> statements;
        final String stderr;

        ParseOutcome(List<Stmt> statements, String stderr) {
            this.statements = statements;
            this.stderr = stderr;
        }
    }

    private static final class EvalOutcome {
        final String stdout;
        final String stderr;

        EvalOutcome(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }
}
