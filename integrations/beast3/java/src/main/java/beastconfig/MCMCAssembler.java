package beastconfig;

import beast.base.inference.CompoundDistribution;
import beast.base.inference.MCMC;
import beast.base.inference.State;
import beast.base.inference.StateNode;

import java.util.ArrayList;
import java.util.List;

/// Materializes a fully-tiled {@link BEASTState} into a runnable {@link MCMC} object.
///
/// Builds the BEAST {@code State} and prior/likelihood/posterior distributions, fills in any
/// operators and loggers the tiles didn't already choose explicitly (via {@link OperatorSelector}
/// and {@link LoggerSelector}), and wires everything into an {@code MCMC} object. Does not run
/// the MCMC; see {@link PhyloSpecRunner#runEngineObjects}.
public final class MCMCAssembler {

    private MCMCAssembler() {
    }

    /**
     * Builds a runnable (but not yet run) MCMC object from the given BEAST state.
     */
    public static MCMC assemble(BEASTState beastState) {
        // add state

        State state = new State();
        beastState.setInput(state, state.stateNodeInput, new ArrayList<>(beastState.stateNodes.keySet()));

        // add distribution

        CompoundDistribution prior = new CompoundDistribution();
        prior.setID(beastState.getAvailableID("prior"));
        beastState.setInput(prior, prior.pDistributions, new ArrayList<>(beastState.priorDistributions.values()));

        CompoundDistribution likelihood = new CompoundDistribution();
        likelihood.setID(beastState.getAvailableID("likelihood"));
        beastState.setInput(likelihood, likelihood.pDistributions, beastState.likelihoodDistributions);

        CompoundDistribution posterior = new CompoundDistribution();
        posterior.setID(beastState.getAvailableID("posterior"));
        beastState.setInput(posterior, posterior.pDistributions, List.of(prior, likelihood));

        // add operators

        for (StateNode stateNode : beastState.stateNodes.keySet()) {
            OperatorSelector.addDefaultOperators(stateNode, beastState);
        }

        // add loggers

        LoggerSelector.addMissingLoggers(beastState, posterior, prior, likelihood);

        // create MCMC object

        MCMC mcmc = new MCMC();
        beastState.setInput(mcmc, mcmc.chainLengthInput, beastState.chainLength);
        beastState.setInput(mcmc, mcmc.startStateInput, state);
        beastState.setInput(mcmc, mcmc.posteriorInput, posterior);
        beastState.setInput(mcmc, mcmc.operatorsInput, new ArrayList<>(beastState.operators.keySet()));
        beastState.setInput(mcmc, mcmc.loggersInput, beastState.getLoggers());

        // initialize

        beastState.initializeBEASTObjects();

        return mcmc;
    }

}
