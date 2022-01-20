package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui.tabs;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public abstract class ConfidentialityTab extends AbstractLaunchConfigurationTab {

    public static final String analysisModelID = "org.palladiosimulator.dataflow.pcm.dsexplore.analysis.definition";

    protected Text analysisDefinition;

    @Override
    public abstract void createControl(Composite parent);

    @Override
    public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
        if (this.analysisDefinition != null) {
            this.analysisDefinition.setText("");
        }
    }

    @Override
    public void initializeFrom(final ILaunchConfiguration configuration) {
        try {
            this.analysisDefinition.setText(configuration.getAttribute(analysisModelID, ""));
        } catch (final CoreException e) {
            this.setErrorMessage("Error restoring values");
        }

    }

    @Override
    public void performApply(final ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(analysisModelID, this.analysisDefinition.getText());

    }

    @Override
    public abstract String getName();

}
