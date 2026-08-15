import dr.inference.mcmc.MCMC;
import org.phylospec.errors.Error;
import org.phylospec.runner.PhyloSpecRunner;
import org.phylospec.tiling.tiles.CandidateTile;
import org.xml.sax.SAXException;
import tiles.BeastXTileLibraries;
import tiling.BeastXModel;
import tiling.BeastXState;
import tiling.runner.RunMode;
import tiling.runner.BeastXRunResult;
import tiling.runner.RunnerOptions;
import tiling.runner.XmlRunResult;
import tiling.runner.XmlRunnerOptions;
import tiling.runner.FileRunPaths;
import tiling.runner.BeastXRunPipeline;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/*
* Entry point for running PhyloSpec models with the BEAST X backend.
*
* This class coordinates the full pipeline:
* PhyloSpec source -> lexer/parser -> AST transforms -> type resolution
* BEAST X tiling -> BeastXModel -> MCMC or XML execution.
*
* The lexer/parser/AST-transform/type-resolution/tiling front half is shared with other engines
* via PhyloSpecRunner (see parseAndResolve/tile); everything past that (BeastXModel, MCMC, and the
* XML-generation paths) is BEAST X-specific and lives entirely in this class and tiling.runner.*.
* */
public class BeastXRunner extends PhyloSpecRunner<BeastXState, MCMC> {

    private final BeastXRunPipeline runPipeline;

    /**
     * Creates a runner for the given PhyloSpec source string.
     */
    public BeastXRunner(String source) {
        super(source);

        this.runPipeline =
                new BeastXRunPipeline();
    }

    /* --- Static convenient methods for easy creation of a runner.  --- */

    /**
     * Creates a runner from a PhyloSpec source file using UTF-8 encoding.
     */
    public static BeastXRunner fromFile(Path sourcePath)
            throws IOException {
        if (sourcePath == null) {
            throw new IllegalArgumentException("sourcePath must not be null.");
        }

        return new BeastXRunner(
                Files.readString(sourcePath, StandardCharsets.UTF_8)
        );
    }

    /**
     * Builds an XML run for a source file and writes it to the requested path.
     */
    public static XmlRunResult buildXmlRunFromFile(
            Path sourcePath,
            Path xmlPath
    ) throws Exception {
        String runName =
                FileRunPaths.defaultRunName(sourcePath);

        return fromFile(sourcePath)
                .buildXmlRun(
                        XmlRunnerOptions.builder(runName, xmlPath)
                                .build()
                );
    }

    /**
     * Executes the default XML run for a source file under target/beastx-runs.
     */
    public static XmlRunResult executeDefaultXmlRunFromFile(Path sourcePath)
            throws Exception {
        FileRunPaths paths = FileRunPaths.forSource(
                sourcePath,
                Path.of("target", "beastx-runs")
        );

        return fromFile(sourcePath)
                .executeXmlRun(
                        XmlRunnerOptions.builder(
                                        paths.runName(),
                                        paths.xmlPath()
                                )
                                .execute(true)
                                .build()
                );
    }

    /* --- Public methods executing different parts of the pipeline. --- */

    /**
     * Runs the PhyloSpec-to-BEAST X pipeline according to the requested run mode.
     */
    public BeastXRunResult run(RunnerOptions options)
            throws IOException, ParserConfigurationException, SAXException {
        BeastXState beastState =
                this.tile(this.parseAndResolve(), options.runName());

        return this.runPipeline
                .run(beastState, options);
    }

    /**
     * Parses, resolves, and tiles the PhyloSpec source into a BEAST X state.
     */
    public BeastXState buildState(String runName)
            throws IOException, ParserConfigurationException, SAXException {
        return this.tile(this.parseAndResolve(), runName);
    }

    /**
     * Builds a BEAST X model from the PhyloSpec source.
     */
    public BeastXModel buildModel(String runName)
            throws IOException, ParserConfigurationException, SAXException {
        BeastXState beastState =
                buildState(runName);

        return this.runPipeline
                .buildModel(beastState, false);
    }

    /**
     * Builds a BEAST X model with materialized PhyloCTMC likelihoods.
     */
    public BeastXModel buildMaterializedModel(String runName)
            throws IOException, ParserConfigurationException, SAXException {
        BeastXState beastState =
                buildState(runName);

        return this.runPipeline
                .buildModel(beastState, true);
    }

    /**
     * Builds an MCMC object for the source using the requested chain length.
     */
    public MCMC buildMCMC(String runName, long chainLength)
            throws IOException, ParserConfigurationException, SAXException {
        BeastXModel model =
                buildModel(runName);

        return this.runPipeline
                .buildMCMC(model, chainLength);
    }

    /**
     * Builds an MCMC run with materialized PhyloCTMC likelihoods without executing it.
     */
    public BeastXRunResult buildMaterializedRun(String runName)
            throws IOException, ParserConfigurationException, SAXException {
        return run(
                RunnerOptions.builder(runName)
                        .mode(RunMode.BUILD_MCMC)
                        .materializePhyloCTMC(true)
                        .build()
        );
    }

    /**
     * Builds and executes an in-memory BEAST X MCMC run.
     */
    public MCMC runMCMC(String runName)
            throws IOException, ParserConfigurationException, SAXException {
        return run(
                RunnerOptions.builder(runName)
                        .mode(RunMode.EXECUTE_MCMC)
                        .build()
        ).mcmc();
    }

    /**
     * Writes XML, parses it through the BEAST X XML parser, and optionally executes it.
     */
    public XmlRunResult runXml(XmlRunnerOptions options)
            throws Exception {
        BeastXModel model =
                options.materializePhyloCTMC()
                        ? buildMaterializedModel(options.runName())
                        : buildModel(options.runName());

        return this.runPipeline
                .runXml(model, options);
    }

    /**
     * Writes the generated BEAST X XML to disk and returns the model used for export.
     */
    public BeastXModel writeXml(
            String runName,
            Path xmlPath
    ) throws IOException, ParserConfigurationException, SAXException {
        BeastXModel model =
                buildModel(runName);

        this.runPipeline
                .writeXml(model, xmlPath);

        return model;
    }

    /**
     * Writes BEAST X XML and executes the parsed XML MCMC.
     */
    public MCMC writeAndRunXmlMCMC(
            String runName,
            Path xmlPath
    ) throws Exception {
        writeXml(runName, xmlPath);

        return this.runPipeline
                .runXmlMCMC(xmlPath);
    }

    /**
     * Writes BEAST X XML and parses it into an XML run without executing it.
     */
    public XmlRunResult buildXmlRun(
            String runName,
            Path xmlPath
    ) throws Exception {
        BeastXModel model =
                writeXml(runName, xmlPath);

        MCMC mcmc =
                this.runPipeline
                        .parseXmlMCMC(xmlPath);

        return new XmlRunResult(
                runName,
                model,
                xmlPath,
                mcmc,
                false
        );
    }

    /**
     * Writes and parses an XML run without executing it.
     */
    public XmlRunResult buildXmlRun(XmlRunnerOptions options)
            throws Exception {
        return runXml(
                options.toBuilder()
                        .execute(false)
                        .build()
        );
    }

    /**
     * Writes, parses, and executes a BEAST X XML run.
     */
    public XmlRunResult executeXmlRun(
            String runName,
            Path xmlPath
    ) throws Exception {
        XmlRunResult run =
                buildXmlRun(runName, xmlPath);

        run.mcmc().run();

        return run.asExecuted();
    }

    /**
     * Writes, parses, and executes a BEAST X XML run.
     */
    public XmlRunResult executeXmlRun(XmlRunnerOptions options)
            throws Exception {
        return runXml(
                options.toBuilder()
                        .execute(true)
                        .build()
        );
    }

    /* --- Engine-specific hooks required by PhyloSpecRunner. --- */

    @Override
    protected BeastXState createState(String runName) {
        return new BeastXState(runName);
    }

    @Override
    protected List<CandidateTile<BeastXState>> getTileLibrary() {
        return BeastXTileLibraries.loadAll();
    }

    /**
     * Builds the default (non-materialized) BEAST X model and MCMC. Used by the inherited
     * one-shot {@link PhyloSpecRunner#runPhyloSpec}; callers wanting the staged/XML capabilities
     * (materialized likelihoods, XML export, partial builds, ...) should use the dedicated public
     * methods above instead.
     */
    @Override
    protected MCMC buildEngineObjects(BeastXState state) {
        return this.runPipeline.buildMCMC(this.runPipeline.buildModel(state, false));
    }

    @Override
    protected void runEngineObjects(MCMC mcmc) {
        mcmc.run();
    }

    /**
     * Converts frontend or tiling errors into runner-level exceptions with
     * source-location information.
     */
    @Override
    public void errorDetected(Error error) {
        throw new PhyloSpecRunnerException(error.toStdOutString(this.source));
    }
}
