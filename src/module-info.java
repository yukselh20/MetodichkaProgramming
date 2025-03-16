/**
 * 
 */
/**
 * 
 */
module MyDetectiveGame {
    requires com.fasterxml.jackson.databind;
    exports Core;
    opens Core; // Add this line for reflection
}