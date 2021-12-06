package de.uka.ipd.sdq.dsexplore.analysis.confidentiality;

import org.palladiosimulator.solver.models.PCMInstance;

import de.uka.ipd.sdq.workflow.jobs.JobFailedException;

/**
 * This {@link FilteringAnalysis} implementation discards candidates based on their numeric ID.
 * Candidates with an odd ID are discarded, while candidates with an even ID are kept.
 * @author Oliver
 *
 */
public class ByIDFilteringAnalysis implements FilteringAnalysis {

	@Override
	public void init() {
		// no initialisation necessary
	}

	@Override
	public boolean runAnalysis(PCMInstance pcmInstance, String genotypeId, long numericId) throws JobFailedException {
		if ((numericId % 2) == 1) {
			return false;
		} else {
			return true;
		}
	}

}
