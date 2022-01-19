package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis;

import java.util.List;

import org.eclipse.emf.ecore.util.EcoreUtil;

import de.uka.ipd.sdq.dsexplore.analysis.IAnalysisQualityAttributeDeclaration;
import de.uka.ipd.sdq.dsexplore.launch.DSEConstantsContainer.QualityAttribute;
import de.uka.ipd.sdq.dsexplore.qml.contract.QMLContract.EvaluationAspect;
import de.uka.ipd.sdq.dsexplore.qml.contracttype.QMLContractType.Dimension;
import de.uka.ipd.sdq.dsexplore.qml.handling.QMLConstantsContainer;
import de.uka.ipd.sdq.dsexplore.qml.reader.QMLDimensionReader;

public class ConfidentialityQualityAttributeDeclaration implements IAnalysisQualityAttributeDeclaration {

    private final Dimension confidentiality;

    public ConfidentialityQualityAttributeDeclaration() {
        final var reader = new QMLDimensionReader();
        this.confidentiality = reader
            .getDimension(QMLConstantsContainer.QUALITY_ATTRIBUTE_DIMENSION_CONFIDENTIALITY_PATH);
    }

    @Override
    public QualityAttribute getQualityAttribute() {
        return QualityAttribute.CONFIDENTIALITY_QUALITY;
    }

    @Override
    public List<Dimension> getDimensions() {
        return List.of(this.confidentiality);
    }

    @Override
    public boolean canEvaluateAspectForDimension(final EvaluationAspect aspect, final Dimension dimension) {
        if (EcoreUtil.equals(dimension, this.confidentiality)) {
            return true;
        }
        return false;

    }

}
