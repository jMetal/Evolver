package org.uma.evolver.problemfamilyinfo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DTLZ3DProblemFamilyInfoTest {
    @Test
    void checkInfo() {
        var familyInfo = new DTLZ3DProblemFamilyInfo() ;

        assertEquals("resources/referenceFronts/DTLZ1.3D.csv", familyInfo.referenceFronts().get(0)) ;
        assertEquals("resources/referenceFronts/DTLZ2.3D.csv", familyInfo.referenceFronts().get(1)) ;
        assertEquals("resources/referenceFronts/DTLZ3.3D.csv", familyInfo.referenceFronts().get(2)) ;
        assertEquals("resources/referenceFronts/DTLZ4.3D.csv", familyInfo.referenceFronts().get(3)) ;
        assertEquals("resources/referenceFronts/DTLZ5.3D.csv", familyInfo.referenceFronts().get(4)) ;
        assertEquals("resources/referenceFronts/DTLZ6.3D.csv", familyInfo.referenceFronts().get(5)) ;
        assertEquals("resources/referenceFronts/DTLZ7.3D.csv", familyInfo.referenceFronts().get(6)) ;

        assertEquals("DTLZ1", familyInfo.problemList().get(0).name());
        assertEquals("DTLZ2", familyInfo.problemList().get(1).name());
        assertEquals("DTLZ3", familyInfo.problemList().get(2).name());
        assertEquals("DTLZ4", familyInfo.problemList().get(3).name());
        assertEquals("DTLZ5", familyInfo.problemList().get(4).name());
        assertEquals("DTLZ6", familyInfo.problemList().get(5).name());
        assertEquals("DTLZ7", familyInfo.problemList().get(6).name());
    }

}
