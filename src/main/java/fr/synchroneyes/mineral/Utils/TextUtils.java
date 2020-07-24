package fr.synchroneyes.mineral.Utils;

import org.apache.commons.lang.WordUtils;

import java.util.Arrays;
import java.util.List;

public class TextUtils {

    /**
     * Permet de convertir une chaine de caractère en une description type "lore", item meta
     *
     * @param text
     */
    public static List<String> textToLore(String text) {
        int nbCaractereAvantNouvelleLigne = 70;
        String wrapped = WordUtils.wrap(text, nbCaractereAvantNouvelleLigne);
        wrapped = wrapped.replace("\r", "");
        return Arrays.asList(wrapped.split("\n"));

    }
}
