package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Copied from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.Activator} for
 * StandaloneUtil init
 *
 */
public class Activator implements BundleActivator {

    private static BundleContext context;

    static BundleContext getContext() {
        return context;
    }

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        Activator.context = bundleContext;
    }

    @Override
    public void stop(final BundleContext bundleContext) throws Exception {
        Activator.context = null;
    }

}
