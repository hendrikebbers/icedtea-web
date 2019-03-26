package net.sourceforge.jnlp.util;

import net.sourceforge.jnlp.JNLPFile;

import java.net.URL;

public class JNLPFileUtilities {
    public static URL guessCodeBase(JNLPFile file) {
        if (file.getCodeBase() != null) {
            return file.getCodeBase();
        } else {
            //Fixme: codebase should be the codebase of the Main Jar not
            //the location. Although, it still works in the current state.
            return file.getResources().getMainJAR().getLocation();
        }
    }
}
