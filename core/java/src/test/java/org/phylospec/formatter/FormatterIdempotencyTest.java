package org.phylospec.formatter;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.phylospec.ast.Stmt;
import org.phylospec.lexer.Lexer;
import org.phylospec.lexer.Token;
import org.phylospec.parser.Parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormatterIdempotencyTest {

    /**
     * Goes through every .phylospec file in the parser test folder, formats it once,
     * then formats the result again and asserts both outputs are identical.
     */
    @TestFactory
    public Iterable<DynamicTest> testFormatterIsIdempotent() throws IOException {
        List<Path> psFiles = findPsFiles(Paths.get("src/test/java/org/phylospec/parser"));
        psFiles.sort(Comparator.comparing(Path::toString));

        List<DynamicTest> tests = new ArrayList<>();
        for (Path psFile : psFiles) {
            tests.add(assertFormatterIsIdempotent(psFile));
        }
        return tests;
    }

    private List<Path> findPsFiles(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".phylospec"))
                    .collect(Collectors.toList());
        }
    }

    private DynamicTest assertFormatterIsIdempotent(Path psPath) throws IOException {
        String source = Files.readString(psPath, StandardCharsets.UTF_8);

        return DynamicTest.dynamicTest(psPath.getFileName().toString(), () -> {
            String firstPass = format(source);
            String secondPass = format(firstPass);
            assertEquals(firstPass, secondPass, "Formatter is not idempotent for: " + psPath);
        });
    }

    private String format(String source) {
        Lexer lexer = new Lexer(source);
        List<Token> tokens = lexer.scanTokens();
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        return new Formatter().format(statements, lexer, parser);
    }
}
