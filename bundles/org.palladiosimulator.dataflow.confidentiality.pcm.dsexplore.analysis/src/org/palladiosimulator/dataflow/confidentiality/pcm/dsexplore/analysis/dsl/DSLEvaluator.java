package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.dsl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ConfidentialityEvaluator;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ConfidentialityResult;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.dcp.workflow.TransformPCMDFDWithConstraintsToPrologWorkflowFactory;
import org.palladiosimulator.dataflow.confidentiality.pcm.transformation.dcp.workflow.jobs.TransformPCMDFDWithConstraintsToPrologJobBuilder;

import de.uka.ipd.sdq.dsexplore.analysis.AnalysisFailedException;
import de.uka.ipd.sdq.dsexplore.launch.DSEWorkflowConfiguration;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;

public class DSLEvaluator extends ConfidentialityEvaluator {

    private URI dcpdslURI;

    @Override
    protected void internalInit(final DSEWorkflowConfiguration configuration) throws CoreException {
        final var modelURI = this.getQuery(configuration);
        this.dcpdslURI = URI.createURI(modelURI);
    }

    @Override
    protected ConfidentialityResult runAnalysis(final URI uriusage, final URI uriAllocation)
            throws CoreException, UserCanceledException, JobFailedException, AnalysisFailedException {
        final var tmpURI = this.getTmpURI(this.dcpdslURI);

        this.copy(this.dcpdslURI, tmpURI);

        try {
            final var path = this.getPath(new URL(this.dcpdslURI.toString()));
            final var stream = Files.lines(path)
                .map(e -> this.convertURI(e, tmpURI))
                .collect(Collectors.toList());
            final var targetPath = this.getPath(new URL(tmpURI.toString()));
            Files.write(targetPath, stream);

        } catch (IOException | URISyntaxException e) {
            this.logger.error(e);
        }

        final var results = new ArrayList<String>();

        final var transformJob = TransformPCMDFDWithConstraintsToPrologJobBuilder.create()
            .addAllocationModelByURI(uriAllocation)
            .addUsageModelsByURI(uriusage)
            .addDCPDSL(tmpURI)
            .setSerializeResultHandler(results::add)
            .build();
        final var workflow = TransformPCMDFDWithConstraintsToPrologWorkflowFactory.createWorkflow(transformJob);
        workflow.run();
        return new ConfidentialityResult(this.successfull(results));

    }

    private String convertURI(final String string, final URI uri) {
        var segments = new LinkedList<>(uri.segmentsList());
        segments.removeLast();
        segments.removeLast();

        final var urlString = "platform:/" + String.join("/", segments);

        segments = new LinkedList<>(uri.segmentsList());
        segments.removeLast();

        final var newURLString = "platform:/" + String.join("/", segments);

        return string.replaceAll(urlString, newURLString);

    }

    private boolean successfull(final List<String> list) {

        final var matcher = Pattern.compile("Violations found: \\d+")
            .matcher(list.get(0));
        if (matcher.find()) {
            final var outputString = matcher.group();
            return Integer.parseInt(outputString.replace("Violations found:", "")
                .trim()) != 0;
        }
        throw new IllegalStateException();
    }

}
