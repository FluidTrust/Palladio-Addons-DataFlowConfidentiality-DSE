package de.uka.ipd.sdq.dsexplore.analysis.confidentiality;

import java.util.ArrayList
import java.util.Arrays
import java.util.Collection
import java.util.HashMap
import java.util.Map
import org.apache.log4j.Logger
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation
import org.palladiosimulator.dataflow.confidentiality.pcm.model.profile.ProfileConstants
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflowFactory
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDToPrologJobBuilder
import org.palladiosimulator.pcm.allocation.Allocation
import org.palladiosimulator.pcm.parameter.VariableCharacterisation
import org.palladiosimulator.pcm.parameter.VariableUsage
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall
import org.palladiosimulator.pcm.usagemodel.UsageModel
import org.palladiosimulator.solver.models.PCMInstance
import org.palladiosimulator.supporting.prolog.PrologStandaloneSetup
import org.prolog4j.Prover
import org.prolog4j.Solution
import org.prolog4j.swicli.DefaultSWIPrologExecutableProvider
import org.prolog4j.swicli.SWIPrologCLIProverFactory
import org.prolog4j.swicli.SWIPrologCLIProverFactory.SWIPrologExecutableProviderStandalone
import org.prolog4j.swicli.enabler.SWIPrologEmbeddedFallbackExecutableProvider
import tools.mdsd.library.standalone.initialization.StandaloneInitializerBuilder
import tools.mdsd.library.standalone.initialization.emfprofiles.EMFProfileInitializationTask
import tools.mdsd.library.standalone.initialization.log4j.Log4jInitilizationTask

/**
 * This class enables running confidentiality tests for a PCM model that is given via parameters.
 * This class merges, extends and generalises the functionality of multiple classes
 * from the {@code org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test} project.
 */
class PCMConfidentialityAnalysis implements FilteringAnalysis {
	
	static Logger logger = Logger.getLogger("de.uka.ipd.sdq.dsexplore");
	static boolean isInitialized = false
	static SWIPrologCLIProverFactory proverFactory
	Prover prover
	
	static String[] confidentialityVariables = #["P", "PIN", "ROLES", "REQ", "S"]
	static String   confidentialityQuery     = '''
			inputPin(P, PIN),
			setof(R, nodeCharacteristic(P, ?CTROLES, R), ROLES),
			setof_characteristics(P, PIN, ?CTRIGHTS, REQ, S),
			intersection(REQ, ROLES, []).
		'''
	
	/**
	 * adapted from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.TestBase#init}
	 */
	new() {
		init();
	}
	
	/**
	 * Runs a confidentiality analysis on the PCMInstance to determine whether the sample being
	 * analysed has confidentiality violations and should therefore be discarded.
	 * 
	 * @param pcmInstance the PCMInstance of the PCMPhenotype
	 * @param genotypeId the Genotype ID of the PCMPhenotype
	 * @param numericId the numeric ID of the PCMPhenotype
	 * @return <b>true</b> if this sample should be kept;<br>
	 * 		   <b>false</b> if this sample should be discarded due to confidentiality issues
	 */
	override runAnalysis(PCMInstance pcmInstance, String genotypeId, long numericId) {
		var allocation = pcmInstance.getAllocation();
		var usageModel = pcmInstance.getUsageModel();
		this.runTest(usageModel, allocation)
	}
	
	/**
	 * Helper method to facilitate testing of analysis without creating a new PCMInstance.
	 */
	def runAnalysis(UsageModel usageModel, Allocation allocation) {
		this.runTest(usageModel, allocation)
	}
	
	/**
	 * Set the variables for the confidentiality analysis. If not set prior to execution, defaults to:<br>
	 * {@code #["P", "PIN", "ROLES", "REQ", "S"]}
	 */
	def setVariables(String[] vars) {
		PCMConfidentialityAnalysis.confidentialityVariables = vars
	}
	
	/**
	 * Get the variables for the confidentiality analysis.
	 */
	def getVariables() {
		return PCMConfidentialityAnalysis.confidentialityVariables
	}
	
	/**
	 * Set the Prolog query for the confidentiality analysis. If not set prior to execution, defaults to:<br>
	 * {@code inputPin(P, PIN), setof(R, nodeCharacteristic(P, ?CTROLES, R), ROLES),
	 * 	setof_characteristics(P, PIN, ?CTRIGHTS, REQ, S), intersection(REQ, ROLES, []).}
	 */
	def setQuery(String query) {
		PCMConfidentialityAnalysis.confidentialityQuery = query
	}
	
	/**
	 * Get the Prolog query for the confidentiality analysis.
	 */
	def getQuery() {
		return PCMConfidentialityAnalysis.confidentialityQuery
	}
	
	/**
	 * Runs a confidentiality analysis on a phenotype with the passed usage and allocation models.
	 * 
	 * @param usageModel the Usage Model of the phenotype
	 * @param allocation the Allocation Model of the phenotype
	 * @return 	<b>true</b>: either the analysis found no confidentiality issues in the phenotype, or
	 * 						 the model has no confidentiality annotations<br>
	 * 			<b>false</b>: the analysis found confidentiality issues in the phenotype
	 */
	private def runTest(UsageModel usageModel, Allocation allocation) {
		if (usageModel === null || allocation === null) {
			PCMConfidentialityAnalysis.logger.warn("Cannot perform confidentiality analysis: UsageModel or AllocationModel is null")
			throw new NullPointerException();
		}
		
		// usage models WITHOUT profiles can also be analysed successfully. Success depends rather on existence of
		//  variableCharacterisation_VariableUsage xsi:type="confidentiality:ConfidentialityVariableCharacterisation"
		val list = new ArrayList<VariableCharacterisation>()
		usageModel.usageScenario_UsageModel.forEach[
			us | us.scenarioBehaviour_UsageScenario.actions_ScenarioBehaviour.filter(EntryLevelSystemCall).forEach[
				elsc | elsc.outputParameterUsages_EntryLevelSystemCall.filter(VariableUsage).forEach[
					vu | vu.variableCharacterisation_VariableUsage.filter(ConfidentialityVariableCharacterisation).forEach[
						vc | list.add(vc)
					]
				]
			]
		];
		if (list.empty) {
			PCMConfidentialityAnalysis.logger.debug("No confidentiality annotations found for this instance. Skipping confidentiality analysis")
			return true
		}
		
		// adapted from org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.RBAC_TestBase#runTest
		prover = proverFactory.createProver
		val solution = deriveSolution(usageModel, allocation)
		return assertNumberOfSolutions(solution, 0, this.getVariables)
	}
	
	/**
	 * adapted from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.RBAC_TestBase#deriveSolution}
	 */
	private def deriveSolution(UsageModel usageModel, Allocation allocation) {
		
		val job = new TransformPCMDFDToPrologJobBuilder().addSerializeModelToString.addUsageModels(usageModel).
			addAllocationModel(allocation).build
		val workflow = TransformPCMDFDToPrologWorkflowFactory.createWorkflow(job)
		workflow.run
		
		val resultingProgram = workflow.prologProgram
		
		val traceWrapper = workflow.trace
		val trace = traceWrapper.get
		val ctRights = trace.getFactId([ct | ct.name == "AllowedRoles"]).findFirst[true]
		val ctRoles = trace.getFactId([ct | ct.name == "OwnedRoles"]).findFirst[true]

		prover.addTheory(resultingProgram.get)
		
		val query = prover.query(this.getQuery)
		query.bind("CTROLES", ctRoles)
		query.bind("CTRIGHTS", ctRights)
		query.solve()
	}
	
	/**
	 * adapted from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.TestBase#assertNumberOfSolutions}
	 */
	private def boolean assertNumberOfSolutions(Solution<Object> solution, int expectedAmount, Iterable<String> variableNames) {
		
		// if the solution failed, that means that the prolog query failed,
		// i.e. no violations found
		if (expectedAmount == 0 && !solution.isSuccess) {
			return true;
		}

		// use first given variable as starting point
		var variableIter = variableNames.iterator();
		if (variableIter.hasNext()) {
			solution.on(variableIter.next());
		}

		val Collection<Map<String, Object>> solutions = new ArrayList
		for (var iter = solution.iterator(); iter.hasNext(); iter.next()) {
			val solutionVariables = new HashMap();
			for (String variableName : variableNames) {
				solutionVariables.put(variableName, iter.get(variableName))
			}
			solutions += solutionVariables
		}

		var solutionCounter = 0;
		var debugMessage = "";
		for (aSolution : solutions) {
			debugMessage += "solution " + solutionCounter + ":\n";
			for (variableName : variableNames) {
				debugMessage += "\t" + variableName + ": " + aSolution.get(variableName).toString + "\n";				
			}
			solutionCounter++;
		}
		
		PCMConfidentialityAnalysis.logger.debug(String.format("Expected solutions: %d Actual solutions: %d", expectedAmount, solutionCounter))
		if (expectedAmount != solutionCounter) {
			PCMConfidentialityAnalysis.logger.debug(debugMessage)
		}
		return expectedAmount == solutionCounter
	}
	
	/**
	 * adapted from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.StandaloneUtil#init}
	 * and {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.TestBase#init}
	 */
	override init() {
		if (isInitialized) {
			return;
		}
		
		val initializer = StandaloneInitializerBuilder.builder()
            .registerProjectURI(typeof(Activator), "de.uka.ipd.sdq.dsexplore.analysis.confidentiality")
            .registerProjectURI(typeof(ProfileConstants),
                    "org.palladiosimulator.dataflow.confidentiality.pcm.model.profile")
            .addCustomTask(new Log4jInitilizationTask())
            .addCustomTask([PrologStandaloneSetup::doSetup])
            .addCustomTask(new EMFProfileInitializationTask("org.palladiosimulator.dataflow.confidentiality.pcm.model.profile",
                  "profile.emfprofile_diagram"))
            .build();
        initializer.init();
        
		var factory = new SWIPrologCLIProverFactory(
			Arrays.asList(new SWIPrologExecutableProviderStandalone(new DefaultSWIPrologExecutableProvider(), 2),
				new SWIPrologExecutableProviderStandalone(new SWIPrologEmbeddedFallbackExecutableProvider(), 1)));
		proverFactory = factory;
		
        isInitialized = true
	}
	
}
