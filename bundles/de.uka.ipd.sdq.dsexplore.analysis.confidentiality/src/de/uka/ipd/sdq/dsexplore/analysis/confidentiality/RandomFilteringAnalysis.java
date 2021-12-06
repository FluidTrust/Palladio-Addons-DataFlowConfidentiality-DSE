package de.uka.ipd.sdq.dsexplore.analysis.confidentiality;

import java.util.Random;

import org.palladiosimulator.solver.models.PCMInstance;

import de.uka.ipd.sdq.workflow.jobs.JobFailedException;

/**
 * This {@link FilteringAnalysis} implementation discards candidates based on {@link Random#nextBoolean()}.
 * @author Oliver
 *
 */
public class RandomFilteringAnalysis implements FilteringAnalysis {

	private static Random rand;
	
	public RandomFilteringAnalysis() {
		init();
	}
	
	@Override
	public void init() {
		RandomFilteringAnalysis.rand = new Random();
	}

	@Override
	public boolean runAnalysis(PCMInstance pcmInstance, String genotypeId, long numericId) throws JobFailedException {
		if (RandomFilteringAnalysis.rand == null) {
			throw new JobFailedException("not initialized");
		}
		return RandomFilteringAnalysis.rand.nextBoolean();
	}

}
