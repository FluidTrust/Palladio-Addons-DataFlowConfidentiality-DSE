package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.opt4j.core.Criterion;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui.tabs.ConfidentialityTab;

import de.uka.ipd.sdq.dsexplore.analysis.AbstractAnalysis;
import de.uka.ipd.sdq.dsexplore.analysis.AnalysisFailedException;
import de.uka.ipd.sdq.dsexplore.analysis.IAnalysis;
import de.uka.ipd.sdq.dsexplore.analysis.IAnalysisResult;
import de.uka.ipd.sdq.dsexplore.analysis.PCMPhenotype;
import de.uka.ipd.sdq.dsexplore.launch.DSEWorkflowConfiguration;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

public abstract class ConfidentialityEvaluator extends AbstractAnalysis implements IAnalysis {

    private final Map<Long, ConfidentialityResult> results = new HashMap<>();

    private final static String tmpFolder = "tmpTest";

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
        final var system = EcoreUtil.copy(pheno.getPCMInstance()
            .getSystem());

        final var uriAllocation = this.getUri(pheno.getPCMInstance()
            .getAllocation());
        saveToXMIFile(allocation, uriAllocation, true);

        final var uriSystem = this.getUri(pheno.getPCMInstance()
            .getSystem());
        saveToXMIFile(system, uriSystem, true);

        // copy uri model to keep profiles
        final var uriusage = this.getUri(pheno.getPCMInstance()
            .getUsageModel());
        this.copy(pheno.getPCMInstance()
            .getUsageModel()
            .eResource()
            .getURI(), uriusage);

        try {
            final var newFolder = this.getFolder(uriAllocation);

            final var files = Files.walk(this.getFolder(pheno.getPCMInstance()
                .getAllocation()
                .eResource()
                .getURI()));
            files.filter(this::isCorrectFileEnding)
                .forEach(e -> {
                    try {
                        final var newFile = Paths.get(newFolder.toString(), e.getFileName()
                            .toString());
                        Files.copy(e, newFile, StandardCopyOption.REPLACE_EXISTING);
                    } catch (final IOException e1) {
                        this.logger.error(e1);
                    }
                });
            files.close();
        } catch (IOException | URISyntaxException e) {
            this.logger.error(e);
        }

        final var result = this.runAnalysis(uriusage, uriAllocation);

        this.cleanup(uriusage);

        this.results.put(pheno.getNumericID(), result);

    }

    private boolean isCorrectFileEnding(final Path file) {
        final var filename = file.toString();
        final var list = List.of("pddc", "characteristics", "repository", "resourceenvironment");
        return list.stream()
            .anyMatch(filename::endsWith);
    }

    private Path getFolder(final URI uri) throws MalformedURLException, URISyntaxException, IOException {
        final var folderURI = this.getFolderURI(uri);
        return this.getPath(new URL(folderURI.toString()));
    }

    protected abstract ConfidentialityResult runAnalysis(URI uriusage, URI uriAllocation)
            throws CoreException, UserCanceledException, JobFailedException, AnalysisFailedException;

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
        final var tmpFolder = this.getFolderURI(uri);
        Path path;
        try {
            path = this.getPath(new URL(tmpFolder.toString()));
            Files.walk(path)
                .forEach(this::delete);

            Files.delete(path);
        } catch (URISyntaxException | IOException e) {
            this.logger.error(e);
        }

    }

    private void delete(final Path path) {
        try {
            FileUtils.forceDelete(path.toFile());
        } catch (final IOException e) {
            this.logger.error(e);
        }
    }

    protected URI getFolderURI(final URI uri) {
        final var list = new ArrayList<>(uri.segmentsList());
        list.remove(list.size() - 1);
        final var tmpFolder = URI.createURI("platform:/" + String.join("/", list));
        return tmpFolder;
    }

    protected void copy(final URI source, final URI target) throws CoreException {
        try {
            final var urlSource = new URL(source.toString());
            final var urlTarget = new URL(target.toString());
            final var sourceFile = this.getPath(urlSource);
            final var targetFile = this.getPath(urlTarget);
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException | URISyntaxException e) {
            this.logger.error(e);
        }
    }

    protected Path getPath(final URL url) throws URISyntaxException, IOException {
        return Paths.get(Platform.asLocalURL(url)
            .toURI()
            .getSchemeSpecificPart());
    }

    private URI getUri(final EObject object) {
        final var uri = object.eResource()
            .getURI();
        return this.getTmpURI(uri);
    }

    protected URI getTmpURI(final URI uri) {
        final var segmentList = new ArrayList<>(uri.segmentsList());
        final var tmpSegment = segmentList.get(uri.segmentCount() - 1);
        segmentList.remove(uri.segmentCount() - 1);
        segmentList.add(tmpFolder);
        segmentList.add(tmpSegment);

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

    // copied from EMFHelper, we need a custom URLHandler to manipulate the url resolving
    private void saveToXMIFile(final EObject modelToSave, final URI myURI, final boolean mayRetry) {
        final Logger logger = Logger.getLogger("de.uka.ipd.sdq.dsexplore");

        logger.debug("Saving " + modelToSave.toString() + " to " + myURI);

        // Create a resource set.
        final ResourceSet resourceSet = new ResourceSetImpl();

        // Register the default resource factory -- only needed for stand-alone!
        resourceSet.getResourceFactoryRegistry()
            .getExtensionToFactoryMap()
            .put(Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());

        final Resource resource = resourceSet.createResource(myURI);
        resource.getContents()
            .add(modelToSave);

        try {
            // custom url handler that the url shows at the tmpFolder
            final var handler = new XMLResource.URIHandler() {
                URI base;

                @Override
                public void setBaseURI(final URI uri) {
                    if (uri == null) {
                        return;
                    }
                    final var list = new ArrayList<>(uri.segmentsList());
                    list.remove(list.size() - 2);
                    this.base = URI.createURI("platform:/" + String.join("/", list));
                }

                @Override
                public URI resolve(final URI uri) {
                    return uri.resolve(this.base);
                }

                @Override
                public URI deresolve(final URI uri) {
                    return uri.deresolve(this.base);
                }
            };

            final Map<Object, Object> saveOptions = new HashMap<>();
            saveOptions.put(XMLResource.OPTION_URI_HANDLER, handler);
            resource.save(saveOptions);
        } catch (final FileNotFoundException e) {
            if (mayRetry && myURI.toFileString()
                .length() > 250) {
                // try again with a shorter filename, but just one more try (mayRetry = false).
                final String lastSegment = myURI.segment(myURI.segmentCount() - 1);
                final int lengthOfShortenedSegment = lastSegment.length() > 25 ? 25 : lastSegment.length() / 2;
                final String lastSegmentShortened = lastSegment.substring(0, lengthOfShortenedSegment);
                URI myShorterURI = myURI.trimSegments(1);
                myShorterURI = myShorterURI.appendSegment(lastSegmentShortened + "-shortened-" + myURI.toString()
                    .hashCode());
                saveToXMIFile(modelToSave, myShorterURI, false);
            }
        } catch (final IOException e) {
            logger.error("Caught IOException:" + e.getClass() + ": " + e.getMessage() + " when trying to save to file "
                    + myURI.toString());
        }
    }

}
