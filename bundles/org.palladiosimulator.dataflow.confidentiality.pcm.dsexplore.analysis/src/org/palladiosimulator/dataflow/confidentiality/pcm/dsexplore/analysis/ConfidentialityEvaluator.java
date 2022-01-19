package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.opt4j.core.Criterion;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui.tabs.ConfidentialityTab;

import de.uka.ipd.sdq.dsexplore.analysis.AbstractAnalysis;
import de.uka.ipd.sdq.dsexplore.analysis.AnalysisFailedException;
import de.uka.ipd.sdq.dsexplore.analysis.IAnalysis;
import de.uka.ipd.sdq.dsexplore.analysis.IAnalysisResult;
import de.uka.ipd.sdq.dsexplore.analysis.PCMPhenotype;
import de.uka.ipd.sdq.dsexplore.helper.EMFHelper;
import de.uka.ipd.sdq.dsexplore.launch.DSEWorkflowConfiguration;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public abstract class ConfidentialityEvaluator extends AbstractAnalysis implements IAnalysis {

    private final Map<Long, ConfidentialityResult> results = new HashMap<>();

    protected Logger logger = Logger.getLogger("org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis");

    public ConfidentialityEvaluator() {
        super(new ConfidentialityQualityAttributeDeclaration());
    }

    @Override
    public void analyse(final PCMPhenotype pheno, final IProgressMonitor monitor)
            throws CoreException, UserCanceledException, JobFailedException, AnalysisFailedException {

        // Create a copy, because the transformation might change models
        final var allocation = EcoreUtil.copy(pheno.getPCMInstance()
            .getAllocation());
        final var usagemodel = EcoreUtil.copy(pheno.getPCMInstance()
            .getUsageModel());

        // save the copy in a new model to get a resource URI
        final var uriusage = this.getUri(pheno.getPCMInstance()
            .getUsageModel());
        EMFHelper.saveToXMIFile(usagemodel, uriusage);
        final var uriAllocation = this.getUri(pheno.getPCMInstance()
            .getAllocation());
        EMFHelper.saveToXMIFile(allocation, uriAllocation);

        final var result = this.runAnalysis(uriusage, uriAllocation);

        this.cleanup(uriusage);
        this.cleanup(uriAllocation);

        this.results.put(pheno.getNumericID(), result);

    }

    protected abstract ConfidentialityResult runAnalysis(URI uriusage, URI uriAllocation)
            throws AnalysisFailedException;

    protected String getQuery(final DSEWorkflowConfiguration configuration) throws CoreException {
        final var modelURI = configuration.getRawConfiguration()
            .getAttribute(ConfidentialityTab.analysisModelID, "");
        if (modelURI.isBlank()) {
            this.logger.error("No Querry loaded");
            throw new IllegalStateException("No querry could be loaded");
        }
        return modelURI;
    }

    private void cleanup(final URI uri) {

        try {
            final var url = new URL(uri.toString());

            final var test = Platform.asLocalURL(url);
            final var file = new File(test.toURI());
            if (!file.delete()) {
                this.logger.error("Temporary files not deleted");
            } else {
                this.logger.info("Temporary files deleted");
            }
        } catch (IOException | URISyntaxException e) {
            this.logger.error(e);
        }

    }

    private URI getUri(final EObject object) {
        final var uri = object.eResource()
            .getURI();
        final var extension = uri.fileExtension();
        final var segmentList = new ArrayList<>(uri.segmentsList());
        segmentList.remove(uri.segmentCount() - 1);
        segmentList.add("tmpTest" + System.currentTimeMillis() + "." + extension);
        return URI.createURI("platform:/" + String.join("/", segmentList));
    }

    @Override
    public void initialise(final DSEWorkflowConfiguration configuration) throws CoreException {

        this.internalInit(configuration);
        this.initialiseCriteria(configuration);

    }

    protected abstract void internalInit(DSEWorkflowConfiguration configuration) throws CoreException;

    @Override
    public IAnalysisResult retrieveResultsFor(final PCMPhenotype pheno, final Criterion criterion)
            throws CoreException, AnalysisFailedException {
        return this.results.get(pheno.getNumericID());
    }

    @Override
    public boolean hasObjectivePerUsageScenario() throws CoreException {
        return false;
    }

    @Override
    public void setBlackboard(final MDSDBlackboard blackboard) {
        this.blackboard = blackboard;

    }

}
