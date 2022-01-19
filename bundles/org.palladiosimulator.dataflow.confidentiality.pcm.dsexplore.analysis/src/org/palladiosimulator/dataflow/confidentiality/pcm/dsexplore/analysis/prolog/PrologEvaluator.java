package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.prolog;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ConfidentialityEvaluator;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ConfidentialityResult;
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

    @Override
    protected void internalInit(final DSEWorkflowConfiguration configuration) throws CoreException {

        this.query = this.getQuery(configuration);

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

        final var ctRights = trace.getFactId(e -> e.getName()
            .equals("GrantedRoles"))
            .stream()
            .findFirst()
            .get();
        final var ctRoles = trace.getFactId(e -> e.getName()
            .equals("AssignedRoles"))
            .stream()
            .findFirst()
            .get();

        final var prover = this.factory.createProver();

        prover.addTheory(program.get());

        final var query = prover.query(this.query);

        query.bind("CTROLES", ctRoles);
        query.bind("CTRIGHTS", ctRights);
        final var solution = query.solve();
        final var result = new ConfidentialityResult(solution.isSuccess());
        return result;
    }

}
