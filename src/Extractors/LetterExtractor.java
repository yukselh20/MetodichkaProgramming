package Extractors;

import Core.CaseFile;
import Core.Letter;

public class LetterExtractor {
    public static void loadLetter(CaseFile caseFile, Letter letter) {
        letter.setInvitation(caseFile.getInvitation());
        letter.setCaseDescription(caseFile.getDescription());
    }
}