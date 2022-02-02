package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis.ui.tabs;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

public class ConfidentialityTabProlog extends ConfidentialityTab {

    public final static String MAPPING_ID = "org.palladiosimulator.dataflow.pcm.dsexplore.analysis.mapping";
    public final static String ENUM_MAPPING_ID = "org.palladiosimulator.dataflow.pcm.dsexplore.analysis.enum";

    private Text mappingDefintion;
    private Text enumMapping;
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

        final Group mappingInputGroup = new Group(container, SWT.NONE);
        final var glmappingInputGroup = new GridLayout();
        glmappingInputGroup.numColumns = 1;
        mappingInputGroup.setLayout(glmappingInputGroup);
        mappingInputGroup.setText("Mapping Input");
        final var gridInputData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        gridInputData.heightHint = 200;
        mappingInputGroup.setLayoutData(gridInputData);

        this.mappingDefintion = new Text(mappingInputGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        this.mappingDefintion.setLayoutData(gridInputData);
        this.mappingDefintion.addModifyListener(modifyListener);

        final Group enumMappingInputGroup = new Group(container, SWT.NONE);
        final var enumglmappingInputGroup = new GridLayout();
        enumglmappingInputGroup.numColumns = 1;
        enumMappingInputGroup.setLayout(enumglmappingInputGroup);
        enumMappingInputGroup.setText("Enum Mapping Input");
        final var enumgridInputData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        enumgridInputData.heightHint = 200;
        enumMappingInputGroup.setLayoutData(enumgridInputData);

        this.enumMapping = new Text(enumMappingInputGroup, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        this.enumMapping.setLayoutData(enumgridInputData);
        this.enumMapping.addModifyListener(modifyListener);
    }

    @Override
    public String getName() {
        return "Dataflow Prolog";
    }

    @Override
    public void performApply(final ILaunchConfigurationWorkingCopy configuration) {
        super.performApply(configuration);
        var mapping = getMapping(this.mappingDefintion.getText());
        configuration.setAttribute(MAPPING_ID, mapping);

        if (!this.enumMapping.getText()
            .isEmpty()) {
            var mappingEnum = getMapping(this.enumMapping.getText());
            configuration.setAttribute(ENUM_MAPPING_ID, mappingEnum);
        }

    }

    @Override
    public void initializeFrom(final ILaunchConfiguration configuration) {
        super.initializeFrom(configuration);
        var defaultMap = new LinkedHashMap<String, String>();
        try {
            var mapping = configuration.getAttribute(MAPPING_ID, defaultMap);
            this.mappingDefintion.setText(restoreMappingString(mapping));

            var mappingEnum = configuration.getAttribute(ENUM_MAPPING_ID, defaultMap);
            this.enumMapping.setText(restoreMappingString(mappingEnum));
        } catch (CoreException e) {
            this.setErrorMessage(e.getMessage());
        }
    }

    @Override
    public void setDefaults(final ILaunchConfigurationWorkingCopy configuration) {
        super.setDefaults(configuration);
        if (this.mappingDefintion != null) {
            this.mappingDefintion.setText("");
        }
        if (this.enumMapping != null) {
            this.enumMapping.setText("");
        }
    }

    private Map<String, String> getMapping(String mappingString) {
        var mappings = mappingString.split(System.lineSeparator());
        var variableMap = new LinkedHashMap<String, String>();
        if (mappings.length == 1 && mappings[0].isEmpty()) {
            return variableMap;
        }
        for (String variable : mappings) {
            var tmpVariable = variable.split(":");
            if (tmpVariable.length != 2) {
                continue;
            }
            variableMap.put(tmpVariable[0], tmpVariable[1]);
        }

        return variableMap;
    }

    private String restoreMappingString(Map<String, String> mapping) {


        var output = new StringBuffer();
        for(var variable: mapping.entrySet()) {
            var mappingString = String.join(":", variable.getKey(), variable.getValue());
            output.append(mappingString);
            output.append(System.lineSeparator());
        }
        return output.toString();
    }

}

