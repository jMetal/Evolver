package org.uma.evolver;

import org.uma.jmetal.qualityindicator.QualityIndicator;
import org.uma.jmetal.qualityindicator.impl.Epsilon;
import org.uma.jmetal.qualityindicator.impl.InvertedGenerationalDistancePlus;
import org.uma.jmetal.qualityindicator.impl.NormalizedHypervolume;
import org.uma.jmetal.qualityindicator.impl.Spread;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.errorchecking.JMetalException;

import java.io.IOException;

public class QualityIndicatorFactory {
    public static QualityIndicator getIndicator(String name, double[][] referenceFront) {
        QualityIndicator indicator = switch (name) {
            case "NormalizedHypervolume" -> new NormalizedHypervolume(referenceFront);
            case "InvertedGenerationalDistancePlus" -> new InvertedGenerationalDistancePlus(referenceFront);
            case "Epsilon" -> new Epsilon(referenceFront);
            case "Spread" -> new Spread(referenceFront);
            default -> throw new RuntimeException("QualityIndicator not found");
        };

        return indicator;
    }

    public static QualityIndicator getIndicator(String name, String referenceFrontFileName) {
        double[][] referenceFront;
        try {
            referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
        } catch (IOException e) {
            throw new JMetalException("The file does not exist", e);
        }
        return QualityIndicatorFactory.getIndicator(name, referenceFront);
    }


    public static QualityIndicator[] getIndicators(String names, String referenceFrontFileName) {
        return QualityIndicatorFactory.getIndicators(names, referenceFrontFileName, ",");
    }

    public static QualityIndicator[] getIndicators(String names, String referenceFrontFileName, String separator) {
        String[] namesArray = names.split(separator);

        return QualityIndicatorFactory.getIndicators(namesArray, referenceFrontFileName);
    }

    public static QualityIndicator[] getIndicators(String[] indicators, String referenceFrontFileName) {
        QualityIndicator[] qualityIndicators = new QualityIndicator[indicators.length];
        double[][] referenceFront;
        try {
            referenceFront = VectorUtils.readVectors(referenceFrontFileName, ",");
        } catch (IOException e) {
            throw new JMetalException("The file does not exist", e);
        }

        for (int i = 0; i < indicators.length; i++) {
            String indicator = indicators[i];
            qualityIndicators[i] = QualityIndicatorFactory.getIndicator(indicator, referenceFront);
        }

        return qualityIndicators;
    }
}
