import org.phylospec.ast.transformers.EvaluateScalarFunctions;
import org.phylospec.tiling.EvaluateTiles;
import org.phylospec.tiling.errors.TileApplicationError;
import org.phylospec.tiling.tiles.CandidateTile;
import org.phylospec.ast.Stmt;
import org.phylospec.ast.transformers.EvaluateLiterals;
import org.phylospec.ast.transformers.RemoveGroupings;
import org.phylospec.components.ComponentLibrary;
import org.phylospec.components.ComponentResolver;
import org.phylospec.errors.Error;
import org.phylospec.errors.ErrorEventListener;
import org.phylospec.lexer.Lexer;
import org.phylospec.lexer.Range;
import org.phylospec.lexer.Token;
import org.phylospec.parser.Parser;
import org.phylospec.typeresolver.StochasticityResolver;
import org.phylospec.typeresolver.TypeError;
import org.phylospec.typeresolver.TypeResolver;
import org.phylospec.typeresolver.VariableResolver;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Top-level entry point for running a PhyloSpec model against a given engine.
///
/// Orchestrates the engine-agnostic front half of the pipeline: lexing and parsing the source,
/// simplifying the AST, resolving variables and types, and applying tiles to build up an
/// engine-specific state object {@code S}. The engine-specific back half — turning that state
/// into runnable objects {@code E} and executing them — is left to subclasses via
/// {@link #buildEngineObjects} and {@link #runEngineObjects}.
///
/// @param <S> the state type accumulated during tiling (e.g. {@code BEASTState})
/// @param <E> the runnable engine object type produced from that state (e.g. {@code MCMC})
public abstract class PhyloSpecRunner<S, E> implements ErrorEventListener {

    /**
     * Default chain length used when none is passed to the constructor.
     * Individual PhyloSpec scripts may still override this via a {@code chainLength} assignment.
     */
    public static final long DEFAULT_CHAIN_LENGTH = 10_00_000;

    protected final String source;
    protected final long defaultChainLength;

    /**
     * Constructs a runner for the given PhyloSpec source code, using the default chain length.
     */
    protected PhyloSpecRunner(String source) {
        this(source, DEFAULT_CHAIN_LENGTH);
    }

    /**
     * Constructs a runner for the given PhyloSpec source code, using the given default chain length.
     */
    protected PhyloSpecRunner(String source, long defaultChainLength) {
        this.source = source;
        this.defaultChainLength = defaultChainLength;
    }

    /**
     * Runs the full PhyloSpec-to-engine pipeline for the given run name.
     * Any error detected during lexing, parsing, type resolution, or tiling is reported
     * via {@link #errorDetected} and terminates the process immediately.
     *
     * <p>The pipeline has four steps: parse and resolve the source (shared), apply tiles to
     * build up the engine-specific state (shared), build the engine's runnable objects from
     * that state ({@link #buildEngineObjects}, engine-specific), and run them
     * ({@link #runEngineObjects}, engine-specific).
     */
    public final void runPhyloSpec(String runName) throws IOException, ParserConfigurationException, SAXException {
        ParsedPhyloSpec parsed = this.parseAndResolve();
        S state = this.tile(parsed, runName);
        E engine = this.buildEngineObjects(state);
        this.runEngineObjects(engine);
    }

    /* --- Engine-specific hooks, implemented by subclasses. --- */

    /**
     * Creates a fresh, empty state object for the given run name. Called once per run, before
     * any tile is applied.
     */
    protected abstract S createState(String runName);

    /**
     * Returns the tile library to use when tiling the parsed PhyloSpec AST into the state
     * object.
     */
    protected abstract List<CandidateTile<S>> getTileLibrary();

    /**
     * Builds the engine's runnable objects from the fully-tiled state.
     * Does not run anything; see {@link #runEngineObjects}.
     */
    protected abstract E buildEngineObjects(S state);

    /**
     * Runs the previously built engine objects.
     */
    protected abstract void runEngineObjects(E engine);

    /* --- Shared front half of the pipeline. --- */

    /**
     * Lexes and parses the source, simplifies the AST, and resolves variables, types, and
     * stochasticity. Shared by all engines.
     */
    private ParsedPhyloSpec parseAndResolve() {
        ComponentResolver componentResolver = loadComponentResolver();

        // run lexer

        Lexer lexer = new Lexer(this.source);
        lexer.registerEventListener(this);
        List<Token> tokens = lexer.scanTokens();

        // run parser

        Parser parser = new Parser(tokens);
        parser.registerEventListener(this);
        List<Stmt> statements = parser.parse();

        // simplify graph

        statements = new RemoveGroupings().transform(statements);
        statements = new EvaluateLiterals().transform(statements);
        statements = new EvaluateScalarFunctions().transform(statements);

        // run variable resolver

        VariableResolver variableResolver = new VariableResolver(statements);

        // run type resolver

        TypeResolver typeResolver = new TypeResolver(componentResolver);
        typeResolver.registerEventListener(this);

        try {
            typeResolver.visitStatements(statements);
        } catch (TypeError error) {
            Range range = parser.getRangeForAstNode(error.getAstNode());
            this.errorDetected(error.toError(range));
        }

        StochasticityResolver stochasticityResolver = new StochasticityResolver();
        stochasticityResolver.visitStatements(statements);

        return new ParsedPhyloSpec(parser, statements, variableResolver, stochasticityResolver);
    }

    /**
     * Applies this engine's tile library to the resolved PhyloSpec AST, building up the
     * engine-specific state object. Shared by all engines.
     */
    private S tile(ParsedPhyloSpec parsed, String runName) {
        EvaluateTiles<S> applyTiles = new EvaluateTiles<>(
                this.getTileLibrary(), new ArrayList<>(), parsed.variableResolver, parsed.stochasticityResolver);

        S state = this.createState(runName);

        try {
            applyTiles.getBestTiling(parsed.statements);
            return applyTiles.applyBestTiling(state);
        } catch (TileApplicationError error) {
            Range range = parsed.parser.getRangeForAstNode(error.getAstNode());
            this.errorDetected(error.toError(range));
            throw new IllegalStateException("Unreachable after errorDetected.");
        }
    }

    /**
     * Loads the core component libraries (built-in types and generators) and returns a
     * resolver backed by them.
     */
    private static ComponentResolver loadComponentResolver() {
        try {
            List<ComponentLibrary> componentLibraries = ComponentResolver.loadCoreComponentLibraries();
            return new ComponentResolver(componentLibraries);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints the error to standard output and exits the process.
     */
    @Override
    public void errorDetected(Error error) {
        System.out.println(error.toStdOutString(this.source));
        System.exit(1);
    }

    /**
     * Prints the warning to standard output but does not exit the process.
     */
    @Override
    public void warningDetected(Error warning) {
        System.out.println(warning.toStdOutString(this.source, true));
    }
    /**
     * Internal container for the parsed and resolved PhyloSpec program.
     * It keeps the parser so that later tiling errors can still be mapped back to source-code
     * ranges.
     */
    private record ParsedPhyloSpec(
            Parser parser,
            List<Stmt> statements,
            VariableResolver variableResolver,
            StochasticityResolver stochasticityResolver
    ) {
    }
}
