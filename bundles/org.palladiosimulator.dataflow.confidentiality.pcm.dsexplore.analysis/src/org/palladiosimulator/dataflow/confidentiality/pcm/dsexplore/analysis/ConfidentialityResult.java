package org.palladiosimulator.dataflow.confidentiality.pcm.dsexplore.analysis;

import org.opt4j.core.Criterion;

import de.uka.ipd.sdq.dsexplore.analysis.IAnalysisResult;

public class ConfidentialityResult implements IAnalysisResult {

    private final boolean solution;

    public ConfidentialityResult(final boolean solution) {
        this.solution = solution;
    }

    @Override
    public double getValueFor(final Criterion criterion) {
        if (this.solution) {
            return 1.0;
        }
        return -1.0;
    }

}
