package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui.tabs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

public class ConfidentialityTabProlog extends ConfidentialityTab {

    @Override
    public void createControl(final Composite parent) {

        final ModifyListener modifyListener = e -> {
            this.setDirty(true);
            this.updateLaunchConfigurationDialog();
        };

        final Composite container = new Composite(parent, SWT.NONE);
        this.setControl(container);
        container.setLayout(new GridLayout());

        final Group queryInputGroup = new Group(container, SWT.NONE);
        final var glqueryInputGroup = new GridLayout();
        glqueryInputGroup.numColumns = 1;
        queryInputGroup.setLayout(glqueryInputGroup);
        queryInputGroup.setText("Prolog Query");
        final var gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gridData.heightHint = 200;

        queryInputGroup.setLayoutData(gridData);
        this.analysisDefinition = new Text(queryInputGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        this.analysisDefinition.setLayoutData(gridData);
        this.analysisDefinition.addModifyListener(modifyListener);
    }

    @Override
    public String getName() {
        return "Dataflow Prolog";
    }

}
