package de.uka.ipd.sdq.dsexplore.analysis.confidentiality;

import org.palladiosimulator.solver.models.PCMInstance;

import de.uka.ipd.sdq.workflow.jobs.JobFailedException;

/**
 * This {@link FilteringAnalysis} implementation discards all candidates.
 * {@link #runAnalysis} always returns false.
 * @author Oliver
 *
 */
public class OnlyDiscardFilteringAnalysis implements FilteringAnalysis {

	@Override
	public void init() {
		// no initialisation necessary
	}

	@Override
	public boolean runAnalysis(PCMInstance pcmInstance, String genotypeId, long numericId) throws JobFailedException {
		return false;
	}

}
