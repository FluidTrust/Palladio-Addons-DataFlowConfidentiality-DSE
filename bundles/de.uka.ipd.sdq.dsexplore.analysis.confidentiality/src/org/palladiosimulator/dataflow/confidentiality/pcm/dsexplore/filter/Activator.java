package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.filter;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Copied from {@link org.palladiosimulator.dataflow.confidentiality.pcm.workflow.Activator}
 * for StandaloneUtil init
 *
 */
public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	public void start(BundleContext bundleContext) throws Exception {
		Activator.context = bundleContext;
	}

	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}