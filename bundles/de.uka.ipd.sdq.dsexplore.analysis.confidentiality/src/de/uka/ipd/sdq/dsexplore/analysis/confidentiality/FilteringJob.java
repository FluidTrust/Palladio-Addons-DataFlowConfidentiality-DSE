package de.uka.ipd.sdq.dsexplore.analysis.confidentiality;

import java.util.Objects;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.solver.models.PCMInstance;

import de.uka.ipd.sdq.workflow.Workflow;
import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;

/**
 * This class handles the calling of filtering analyses. It implements {@link IJob},
 * meaning it can be added and run in a {@link Workflow}. Filtering analyses are classes
 * that implement the {@link FilteringAnalysis} interface.
 * @author Oliver
 *
 * @param <T> the type of the analysis to run on the phenotype
 */
public class FilteringJob<T extends FilteringAnalysis> implements IJob {
	
	/** Logger for log4j. */
	private static Logger logger = Logger.getLogger("de.uka.ipd.sdq.dsexplore");
	
	private final T analysisInstance;
	private final PCMInstance pcmInstance;
	private final String genotypeId;
	private final long numericId;
	
	
	/**
	 * Create a new Filtering Job that will run an analysis of type {@code T}. This constructor will internally create a new 
	 * instance of the analysis class. Use {@link #FilteringJob(T, PCMInstance, String, long)} if you want
	 * to pass an analysis instance instead.
	 * @param analysis
	 * @param pcmInstance
	 * @param genotypeId
	 * @param numericId
	 * @throws JobFailedException
	 */
	public FilteringJob(Class<T> analysis, PCMInstance pcmInstance, String genotypeId, long numericId) throws InstantiationException {
		this.pcmInstance = pcmInstance;
		this.genotypeId = genotypeId;
		this.numericId = numericId;
		
		T analysisInstance = null;
		try {
			analysisInstance = analysis.getDeclaredConstructor().newInstance();
			Objects.requireNonNull(analysisInstance);
		} catch (Exception e) {
			FilteringJob.logger.warn("Skipping filter of type " + analysis.getCanonicalName() + " due to reflective instantiation error");
			e.printStackTrace();
			throw new InstantiationException("Could not instantiate a new filtering analysis from class " + analysis.getCanonicalName());
		}
		
		this.analysisInstance = analysisInstance;
	}
	
	/**
	 * Create a new Filtering Job that will run an analysis using an instance of a FilteringAnalysis implementation passed
	 * via {@code analysisInstance}. Use {@link #FilteringJob(Class, PCMInstance, String, long)} if you don't want to
	 * instantiate the analysis class yourself.
	 * @param analysisInstance
	 * @param pcmInstance
	 * @param genotypeId
	 * @param numericId
	 */
	public FilteringJob(T analysisInstance, PCMInstance pcmInstance, String genotypeId, long numericId) {
		this.pcmInstance = pcmInstance;
		this.genotypeId = genotypeId;
		this.numericId = numericId;
		this.analysisInstance = analysisInstance;
	}

	/**
	 * Execute a filtering analysis of type T with the parameters passed at instantiation.
	 * NOTE: This method is called automatically by the workflow and does not support returning of analysis results
	 * as booleans. Use {@link #executeOutsideOfWorkflow()} for that purpose.
	 * @throws JobFailedException the analysis failed <b>or</b> this candidate is supposed to be discarded if the <b>cause</b>
	 * of the JobFailedException is a {@link FilteringException}.
	 */
	@Override
	public void execute(IProgressMonitor monitor) throws JobFailedException, UserCanceledException {
		
		FilteringJob.logger.debug("Running analysis of type " + this.analysisInstance.getClass().getCanonicalName());		
		boolean result = this.analysisInstance.runAnalysis(pcmInstance, genotypeId, numericId);
		
		if (!result) {
			FilteringJob.logger.warn(String.format("Analysis %s discarded phenotype with ID %d and genotypeId %s",
					this.analysisInstance.getClass().getCanonicalName(), this.numericId, this.genotypeId));
			FilteringException fe = new FilteringException();
			throw new JobFailedException("Analysis discarded this candidate", fe);
		}
		
	}
	
	/**
	 * Execute a filtering analysis of type T with the parameters passed at instantiation.
	 * NOTE: This method is to be called outside of a workflow and returns the analysis results as booleans.
	 * To start an analysis in a workflow, add this class as a job to a workflow, which calls {@link #execute(IProgressMonitor)}
	 * internally and automatically.
	 * @return {@code true} if the candidate should be <b>kept</b><br>
	 *         {@code false} if the candidate should be <b>discarded</b>
	 * @throws JobFailedException the analysis failed
	 */
	public boolean executeOutsideOfWorkflow() throws JobFailedException {
		
		FilteringJob.logger.debug("Running analysis of type " + this.analysisInstance.getClass().getCanonicalName());		
		return this.analysisInstance.runAnalysis(pcmInstance, genotypeId, numericId);

	}
	
	@Override
	public void cleanup(IProgressMonitor monitor) throws CleanupFailedException {
		// no cleanup required
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

}
