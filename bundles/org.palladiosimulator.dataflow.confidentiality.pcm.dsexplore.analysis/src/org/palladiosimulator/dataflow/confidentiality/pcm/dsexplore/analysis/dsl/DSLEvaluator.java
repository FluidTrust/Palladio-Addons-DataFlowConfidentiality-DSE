package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.dsl;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ConfidentialityEvaluator;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ConfidentialityResult;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.dcp.workflow.TransformPCMDFDWithConstraintsToPrologWorkflowFactory;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.dcp.workflow.jobs.TransformPCMDFDWithConstraintsToPrologJobBuilder;

import de.uka.ipd.sdq.dsexplore.analysis.AnalysisFailedException;
import de.uka.ipd.sdq.dsexplore.launch.DSEWorkflowConfiguration;

public class DSLEvaluator extends ConfidentialityEvaluator {

    private URI dcpdslURI;

    @Override
    protected void internalInit(final DSEWorkflowConfiguration configuration) throws CoreException {
        final var modelURI = this.getQuery(configuration);
        this.dcpdslURI = URI.createURI(modelURI);
    }

    @Override
    protected ConfidentialityResult runAnalysis(final URI uriusage, final URI uriAllocation)
            throws AnalysisFailedException {

        final var results = new ArrayList<String>();

        final var transformJob = TransformPCMDFDWithConstraintsToPrologJobBuilder.create()
            .addAllocationModelByURI(uriAllocation)
            .addUsageModelsByURI(uriusage)
            .addDCPDSL(this.dcpdslURI)
            .setSerializeResultHandler(results::add)
            .setSerializeFlowTree(false)
            .build();
        final var workflow = TransformPCMDFDWithConstraintsToPrologWorkflowFactory.createWorkflow(transformJob);
        workflow.run();

        return new ConfidentialityResult(this.successfull(results));

    }

    private boolean successfull(List<String> list) {

        var matcher = Pattern.compile("Violations found: \\d+")
            .matcher(list.get(0));
        if (matcher.find()) {
            var outputString = matcher.group();
            return Integer.parseInt(outputString.replace("Violations found:", "")
                .trim()) != 0;
        }
        throw new IllegalStateException();
    }

}
