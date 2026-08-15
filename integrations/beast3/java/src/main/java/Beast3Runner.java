import beast.base.inference.MCMC;
import beastconfig.BEASTState;
import beastconfig.MCMCAssembler;
import org.phylospec.errors.Error;
import org.phylospec.runner.PhyloSpecRunner;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

/// Runs a PhyloSpec model against the BEAST 3 engine.
///
/// Builds up a {@link BEASTState} by tiling the PhyloSpec AST, hands it to
/// {@link MCMCAssembler} to materialize a runnable {@link MCMC} object, and then runs it.
public class Beast3Runner extends PhyloSpecRunner<BEASTState, MCMC> {

    /**
     * Constructs a runner for the given PhyloSpec source code, using the default chain length.
     */
    public Beast3Runner(String source) {
        super(source);
    }

    /**
     * Constructs a runner for the given PhyloSpec source code, using the given default chain length.
     */
    public Beast3Runner(String source, long defaultChainLength) {
        super(source, defaultChainLength);
    }

    @Override
    protected BEASTState createState(String runName) {
        return new BEASTState(runName, this.defaultChainLength);
    }

    @Override
    protected Class<BEASTState> getStateClass() {
        return BEASTState.class;
    }

    @Override
    protected MCMC buildEngineObjects(BEASTState state) {
        return MCMCAssembler.assemble(state);
    }

    @Override
    protected void runEngineObjects(MCMC mcmc) {
        try {
            mcmc.run();
        } catch (IOException | SAXException | ParserConfigurationException e) {
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
}
