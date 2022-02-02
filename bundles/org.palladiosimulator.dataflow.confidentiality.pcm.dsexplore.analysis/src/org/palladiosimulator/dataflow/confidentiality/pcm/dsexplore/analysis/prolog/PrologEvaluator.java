package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.prolog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ConfidentialityEvaluator;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ConfidentialityResult;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui.tabs.ConfidentialityTabProlog;
import org.palladiosimulator.dataflow.confidentiality.pcm.model.confidentiality.characteristics.CharacteristicTypeDictionary;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.TransformPCMDFDToPrologWorkflowFactory;
import org.palladiosimulator.dataflow.confidentiality.pcm.workflow.jobs.TransformPCMDFDToPrologJobBuilder;
import org.prolog4j.swicli.DefaultSWIPrologExecutableProvider;
import org.prolog4j.swicli.SWIPrologCLIProverFactory;
import org.prolog4j.swicli.SWIPrologCLIProverFactory.SWIPrologExecutableProviderStandalone;
import org.prolog4j.swicli.enabler.SWIPrologEmbeddedFallbackExecutableProvider;

import de.uka.ipd.sdq.dsexplore.launch.DSEWorkflowConfiguration;

public class PrologEvaluator extends ConfidentialityEvaluator {

    private SWIPrologCLIProverFactory factory;

    private String query;

    private Map<String, String> mapping;
    private Map<String, String> mappingEnum;

    @Override
    protected void internalInit(final DSEWorkflowConfiguration configuration) throws CoreException {

        this.query = this.getQuery(configuration);

        this.mapping = getMapping(configuration, ConfidentialityTabProlog.MAPPING_ID);
        this.mappingEnum = getMapping(configuration, ConfidentialityTabProlog.ENUM_MAPPING_ID);

        this.factory = new SWIPrologCLIProverFactory(Arrays.asList(
                new SWIPrologExecutableProviderStandalone(new DefaultSWIPrologExecutableProvider(), 2),
                new SWIPrologExecutableProviderStandalone(new SWIPrologEmbeddedFallbackExecutableProvider(), 1)));
    }

    @Override
    protected ConfidentialityResult runAnalysis(final URI uriusage, final URI uriAllocation) {

        final var job = new TransformPCMDFDToPrologJobBuilder().addSerializeModelToString()
            .addUsageModelsByURI(uriusage)
            .addAllocationModelByURI(uriAllocation)
            .build();
        final var workflow = TransformPCMDFDToPrologWorkflowFactory.createWorkflow(job);
        workflow.run();

        final var program = workflow.getPrologProgram();

        final var traceWrapper = workflow.getTrace();
        final var trace = traceWrapper.get();

        final var prover = this.factory.createProver();

        prover.addTheory(program.get());

        final var query = prover.query(this.query);

        for (var variable : this.mapping.entrySet()) {

            var key = variable.getKey();
            String factID;
            if (this.mappingEnum.containsKey(key)) {

                var rs =  new ResourceSetImpl();

                var folder = getFolderURI(uriAllocation);
                var segements = new ArrayList<>(folder.segmentsList());
                segements.add("CharacteristicTypes.characteristics");
                var characteristicURI = URI.createURI("platform:/" + String.join("/", segements));


                var characteristicTypeDict = (CharacteristicTypeDictionary) rs.getResource(characteristicURI, true)
                    .getContents()
                    .get(0);


                var enumID = characteristicTypeDict.getCharacteristicEnumerations()
                    .stream()
                    .filter(e -> e.getName()
                        .equals(this.mappingEnum.get(key)))
                    .findFirst().get();

                factID = trace.getLiteralFactIdsBySemantic(enumID.getLiterals()
                    .stream()
                    .filter(e -> e.getName()
                        .equals(key))
                    .findFirst()
                    .get())
                    .iterator()
                    .next();


            } else {
                factID = trace.getFactId(e -> e.getName()
                    .equals(variable.getKey()))
                    .stream()
                    .findFirst()
                    .get();
            }

            query.bind(variable.getValue(), factID);
        }

        final var solution = query.solve();
        final var result = new ConfidentialityResult(solution.isSuccess());
        return result;
    }

    private Map<String, String> getMapping(DSEWorkflowConfiguration configuration, String mappingID)
            throws CoreException {
        return configuration.getRawConfiguration()
            .getAttribute(mappingID, new LinkedHashMap<>());
    }

}
