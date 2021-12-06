package de.uka.ipd.sdq.dsexplore.analysis.confidentiality;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import de.uka.ipd.sdq.dsexplore.analysis.confidentiality.Activator;
import de.uka.ipd.sdq.dsexplore.analysis.confidentiality.FilteringAnalysis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.apache.log4j.Logger;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.xbase.lib.Conversions;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xbase.lib.Functions.Function0;
import org.eclipse.xtext.xbase.lib.Functions.Function1;
import org.eclipse.xtext.xbase.lib.IterableExtensions;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.ConfidentialityVariableCharacterisation;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.profile.ProfileConstants;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflow;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflowFactory;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransitiveTransformationTrace;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDToPrologJobBuilder;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDtoPrologJob;
import org.palladiosimulator.dataflow.confidentiality.transformation.workflow.blackboards.KeyValueMDSDBlackboard;
import org.palladiosimulator.dataflow.dictionary.characterized.DataDictionaryCharacterized.CharacteristicType;
import org.palladiosimulator.pcm.allocation.Allocation;
import org.palladiosimulator.pcm.parameter.VariableCharacterisation;
import org.palladiosimulator.pcm.parameter.VariableUsage;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.UsageModel;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.solver.models.PCMInstance;
import org.palladiosimulator.supporting.prolog.PrologStandaloneSetup;
import org.prolog4j.Prover;
import org.prolog4j.Query;
import org.prolog4j.Solution;
import org.prolog4j.SolutionIterator;
import org.prolog4j.swicli.DefaultSWIPrologExecutableProvider;
import org.prolog4j.swicli.SWIPrologCLIProverFactory;
import org.prolog4j.swicli.enabler.SWIPrologEmbeddedFallbackExecutableProvider;
import tools.mdsd.library.standalone.initialization.InitializationTask;
import tools.mdsd.library.standalone.initialization.StandaloneInitializer;
import tools.mdsd.library.standalone.initialization.StandaloneInitializerBuilder;
import tools.mdsd.library.standalone.initialization.emfprofiles.EMFProfileInitializationTask;
import tools.mdsd.library.standalone.initialization.log4j.Log4jInitilizationTask;

/**
 * This class enables running confidentiality tests for a PCM model that is given via parameters.
 * This class merges, extends and generalises the functionality of multiple classes
 * from the {@code org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test} project.
 */
@SuppressWarnings("all")
public class PCMConfidentialityAnalysis implements FilteringAnalysis {
  private static Logger logger = Logger.getLogger("de.uka.ipd.sdq.dsexplore");
  
  private static boolean isInitialized = false;
  
  private static SWIPrologCLIProverFactory proverFactory;
  
  private Prover prover;
  
  private static String[] confidentialityVariables = { "P", "PIN", "ROLES", "REQ", "S" };
  
  private static String confidentialityQuery = new Function0<String>() {
    @Override
    public String apply() {
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("inputPin(P, PIN),");
      _builder.newLine();
      _builder.append("setof(R, nodeCharacteristic(P, ?CTROLES, R), ROLES),");
      _builder.newLine();
      _builder.append("setof_characteristics(P, PIN, ?CTRIGHTS, REQ, S),");
      _builder.newLine();
      _builder.append("intersection(REQ, ROLES, []).");
      _builder.newLine();
      return _builder.toString();
    }
  }.apply();
  
  /**
   * adapted from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.TestBase#init}
   */
  public PCMConfidentialityAnalysis() {
    this.init();
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
  @Override
  public boolean runAnalysis(final PCMInstance pcmInstance, final String genotypeId, final long numericId) {
    boolean _xblockexpression = false;
    {
      Allocation allocation = pcmInstance.getAllocation();
      UsageModel usageModel = pcmInstance.getUsageModel();
      _xblockexpression = this.runTest(usageModel, allocation);
    }
    return _xblockexpression;
  }
  
  /**
   * Helper method to facilitate testing of analysis without creating a new PCMInstance.
   */
  public boolean runAnalysis(final UsageModel usageModel, final Allocation allocation) {
    return this.runTest(usageModel, allocation);
  }
  
  /**
   * Set the variables for the confidentiality analysis. If not set prior to execution, defaults to:<br>
   * {@code #["P", "PIN", "ROLES", "REQ", "S"]}
   */
  public String[] setVariables(final String[] vars) {
    return PCMConfidentialityAnalysis.confidentialityVariables = vars;
  }
  
  /**
   * Get the variables for the confidentiality analysis.
   */
  public String[] getVariables() {
    return PCMConfidentialityAnalysis.confidentialityVariables;
  }
  
  /**
   * Set the Prolog query for the confidentiality analysis. If not set prior to execution, defaults to:<br>
   * {@code inputPin(P, PIN), setof(R, nodeCharacteristic(P, ?CTROLES, R), ROLES),
   * 	setof_characteristics(P, PIN, ?CTRIGHTS, REQ, S), intersection(REQ, ROLES, []).}
   */
  public String setQuery(final String query) {
    return PCMConfidentialityAnalysis.confidentialityQuery = query;
  }
  
  /**
   * Get the Prolog query for the confidentiality analysis.
   */
  public String getQuery() {
    return PCMConfidentialityAnalysis.confidentialityQuery;
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
  private boolean runTest(final UsageModel usageModel, final Allocation allocation) {
    if (((usageModel == null) || (allocation == null))) {
      PCMConfidentialityAnalysis.logger.warn("Cannot perform confidentiality analysis: UsageModel or AllocationModel is null");
      throw new NullPointerException();
    }
    final ArrayList<VariableCharacterisation> list = new ArrayList<VariableCharacterisation>();
    final Consumer<UsageScenario> _function = (UsageScenario us) -> {
      final Consumer<EntryLevelSystemCall> _function_1 = (EntryLevelSystemCall elsc) -> {
        final Consumer<VariableUsage> _function_2 = (VariableUsage vu) -> {
          final Consumer<ConfidentialityVariableCharacterisation> _function_3 = (ConfidentialityVariableCharacterisation vc) -> {
            list.add(vc);
          };
          Iterables.<ConfidentialityVariableCharacterisation>filter(vu.getVariableCharacterisation_VariableUsage(), ConfidentialityVariableCharacterisation.class).forEach(_function_3);
        };
        Iterables.<VariableUsage>filter(elsc.getOutputParameterUsages_EntryLevelSystemCall(), VariableUsage.class).forEach(_function_2);
      };
      Iterables.<EntryLevelSystemCall>filter(us.getScenarioBehaviour_UsageScenario().getActions_ScenarioBehaviour(), EntryLevelSystemCall.class).forEach(_function_1);
    };
    usageModel.getUsageScenario_UsageModel().forEach(_function);
    boolean _isEmpty = list.isEmpty();
    if (_isEmpty) {
      PCMConfidentialityAnalysis.logger.debug("No confidentiality annotations found for this instance. Skipping confidentiality analysis");
      return true;
    }
    this.prover = PCMConfidentialityAnalysis.proverFactory.createProver();
    final Solution<Object> solution = this.deriveSolution(usageModel, allocation);
    return this.assertNumberOfSolutions(solution, 0, ((Iterable<String>)Conversions.doWrapArray(this.getVariables())));
  }
  
  /**
   * adapted from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.RBAC_TestBase#deriveSolution}
   */
  private Solution<Object> deriveSolution(final UsageModel usageModel, final Allocation allocation) {
    Solution<Object> _xblockexpression = null;
    {
      final TransformPCMDFDtoPrologJob<? extends KeyValueMDSDBlackboard> job = new TransformPCMDFDToPrologJobBuilder().addSerializeModelToString().addUsageModels(usageModel).addAllocationModel(allocation).build();
      final TransformPCMDFDToPrologWorkflow workflow = TransformPCMDFDToPrologWorkflowFactory.createWorkflow(job);
      workflow.run();
      final Optional<String> resultingProgram = workflow.getPrologProgram();
      final Optional<TransitiveTransformationTrace> traceWrapper = workflow.getTrace();
      final TransitiveTransformationTrace trace = traceWrapper.get();
      final Predicate<CharacteristicType> _function = (CharacteristicType ct) -> {
        String _name = ct.getName();
        return Objects.equal(_name, "AllowedRoles");
      };
      final Function1<String, Boolean> _function_1 = (String it) -> {
        return Boolean.valueOf(true);
      };
      final String ctRights = IterableExtensions.<String>findFirst(trace.getFactId(_function), _function_1);
      final Predicate<CharacteristicType> _function_2 = (CharacteristicType ct) -> {
        String _name = ct.getName();
        return Objects.equal(_name, "OwnedRoles");
      };
      final Function1<String, Boolean> _function_3 = (String it) -> {
        return Boolean.valueOf(true);
      };
      final String ctRoles = IterableExtensions.<String>findFirst(trace.getFactId(_function_2), _function_3);
      this.prover.addTheory(resultingProgram.get());
      final Query query = this.prover.query(this.getQuery());
      query.bind("CTROLES", ctRoles);
      query.bind("CTRIGHTS", ctRights);
      _xblockexpression = query.<Object>solve();
    }
    return _xblockexpression;
  }
  
  /**
   * adapted from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.TestBase#assertNumberOfSolutions}
   */
  private boolean assertNumberOfSolutions(final Solution<Object> solution, final int expectedAmount, final Iterable<String> variableNames) {
    if (((expectedAmount == 0) && (!solution.isSuccess()))) {
      return true;
    }
    Iterator<String> variableIter = variableNames.iterator();
    boolean _hasNext = variableIter.hasNext();
    if (_hasNext) {
      solution.<Object>on(variableIter.next());
    }
    final Collection<Map<String, Object>> solutions = new ArrayList<Map<String, Object>>();
    for (SolutionIterator<Object> iter = solution.iterator(); iter.hasNext(); iter.next()) {
      {
        final HashMap<String, Object> solutionVariables = new HashMap<String, Object>();
        for (final String variableName : variableNames) {
          solutionVariables.put(variableName, iter.<Object>get(variableName));
        }
        solutions.add(solutionVariables);
      }
    }
    int solutionCounter = 0;
    String debugMessage = "";
    for (final Map<String, Object> aSolution : solutions) {
      {
        String _debugMessage = debugMessage;
        debugMessage = (_debugMessage + (("solution " + Integer.valueOf(solutionCounter)) + ":\n"));
        for (final String variableName : variableNames) {
          String _debugMessage_1 = debugMessage;
          String _string = aSolution.get(variableName).toString();
          String _plus = ((("\t" + variableName) + ": ") + _string);
          String _plus_1 = (_plus + "\n");
          debugMessage = (_debugMessage_1 + _plus_1);
        }
        solutionCounter++;
      }
    }
    PCMConfidentialityAnalysis.logger.debug(String.format("Expected solutions: %d Actual solutions: %d", Integer.valueOf(expectedAmount), Integer.valueOf(solutionCounter)));
    if ((expectedAmount != solutionCounter)) {
      PCMConfidentialityAnalysis.logger.debug(debugMessage);
    }
    return (expectedAmount == solutionCounter);
  }
  
  /**
   * adapted from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.StandaloneUtil#init}
   * and {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.test.cases.impl.TestBase#init}
   */
  @Override
  public void init() {
    try {
      if (PCMConfidentialityAnalysis.isInitialized) {
        return;
      }
      StandaloneInitializerBuilder _registerProjectURI = StandaloneInitializerBuilder.builder().registerProjectURI(Activator.class, "de.uka.ipd.sdq.dsexplore.analysis.confidentiality").registerProjectURI(ProfileConstants.class, 
        "org.palladiosimulator.dataflow.confidentiality.pcm.model.profile");
      Log4jInitilizationTask _log4jInitilizationTask = new Log4jInitilizationTask();
      final InitializationTask _function = () -> {
        PrologStandaloneSetup.doSetup();
      };
      StandaloneInitializerBuilder _addCustomTask = _registerProjectURI.addCustomTask(_log4jInitilizationTask).addCustomTask(_function);
      EMFProfileInitializationTask _eMFProfileInitializationTask = new EMFProfileInitializationTask("org.palladiosimulator.dataflow.confidentiality.pcm.model.profile", 
        "profile.emfprofile_diagram");
      final StandaloneInitializer initializer = _addCustomTask.addCustomTask(_eMFProfileInitializationTask).build();
      initializer.init();
      DefaultSWIPrologExecutableProvider _defaultSWIPrologExecutableProvider = new DefaultSWIPrologExecutableProvider();
      SWIPrologCLIProverFactory.SWIPrologExecutableProviderStandalone _sWIPrologExecutableProviderStandalone = new SWIPrologCLIProverFactory.SWIPrologExecutableProviderStandalone(_defaultSWIPrologExecutableProvider, 2);
      SWIPrologEmbeddedFallbackExecutableProvider _sWIPrologEmbeddedFallbackExecutableProvider = new SWIPrologEmbeddedFallbackExecutableProvider();
      SWIPrologCLIProverFactory.SWIPrologExecutableProviderStandalone _sWIPrologExecutableProviderStandalone_1 = new SWIPrologCLIProverFactory.SWIPrologExecutableProviderStandalone(_sWIPrologEmbeddedFallbackExecutableProvider, 1);
      List<SWIPrologCLIProverFactory.SWIPrologExecutableProviderStandalone> _asList = Arrays.<SWIPrologCLIProverFactory.SWIPrologExecutableProviderStandalone>asList(_sWIPrologExecutableProviderStandalone, _sWIPrologExecutableProviderStandalone_1);
      SWIPrologCLIProverFactory factory = new SWIPrologCLIProverFactory(_asList);
      PCMConfidentialityAnalysis.proverFactory = factory;
      PCMConfidentialityAnalysis.isInitialized = true;
    } catch (Throwable _e) {
      throw Exceptions.sneakyThrow(_e);
    }
  }
}
