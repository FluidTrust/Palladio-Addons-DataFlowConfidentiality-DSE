package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui.tabs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.uka.ipd.sdq.workflow.launchconfig.tabs.TabHelper;

public class ConfidentialityTabDSL extends ConfidentialityTab {

    private final static String[] extensionsAnalysisDefinition = { "*.DCPDSL" };

    @Override
    public void createControl(final Composite parent) {
        final ModifyListener modifyListener = e -> {
            this.setDirty(true);
            this.updateLaunchConfigurationDialog();
        };

        final Composite container = new Composite(parent, SWT.NONE);
        this.setControl(container);
        container.setLayout(new GridLayout());

        this.analysisDefinition = new Text(container, SWT.SINGLE | SWT.BORDER);
        TabHelper.createFileInputSection(container, modifyListener, "Analysis Definition",
                ConfidentialityTabDSL.extensionsAnalysisDefinition, this.analysisDefinition, this.getShell(), "");

    }

    @Override
    public String getName() {

        return "Dataflow DSL";

    }

}
