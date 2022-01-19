package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.palladiosimulator.analyzer.workflow.runconfig.ConfigurationTab;
import org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui.tabs.ConfidentialityTabProlog;

public class ConfidentialityTabGroupProlog extends AbstractLaunchConfigurationTabGroup {

    @Override
    public void createTabs(final ILaunchConfigurationDialog dialog, final String mode) {
        final ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] { new ConfidentialityTabProlog(),
                new ConfigurationTab() };
        this.setTabs(tabs);

    }

}
