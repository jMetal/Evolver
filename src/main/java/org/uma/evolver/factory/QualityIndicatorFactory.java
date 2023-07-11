package org.uma.evolver.factory;

import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.io.IOException;

public class QualityIndicatorFactory {
    public static QualityIndicator getIndicator(String name) {
        QualityIndicator indicator = QualityIndicatorUtils.getIndicatorFromName(
                name,
                // Dummy reference front, is changed later on the execution of each problem
                QualityIndicatorUtils.getAvailableIndicators(new double[0][0])
        );

        return indicator;
    }

    public static QualityIndicator[] getIndicators(String names) {
        return QualityIndicatorFactory.getIndicators(names, ",");
    }

    public static QualityIndicator[] getIndicators(String names, String separator) {
        String[] namesArray = names.split(separator);

        return QualityIndicatorFactory.getIndicators(namesArray);
    }

    public static QualityIndicator[] getIndicators(String[] indicators) {
        QualityIndicator[] qualityIndicators = new QualityIndicator[indicators.length];

        for (int i = 0; i < indicators.length; i++) {
            String indicator = indicators[i];
            qualityIndicators[i] = QualityIndicatorFactory.getIndicator(indicator);
        }

        return qualityIndicators;
    }
}
