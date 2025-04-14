package Extractors;

import Core.Letter;
import JsonDTO.CaseFile;

public class LetterExtractor {
  public static void loadLetter(CaseFile caseFile, Letter letter) {
    letter.setInvitation(caseFile.getInvitation());
    letter.setCaseDescription(caseFile.getDescription());
  }
}
