package io.flutter.plugins.camera.aardman;


import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Filter;

/**
 *  FilterParameters is a DAO for the updatable parameters for the Filters
 */

public final class FilterParameters {

    float  []  replacementColour = {0.0f, 1.0f, 0.0f} ;
    String   backgroundImage = null;

    public FilterParameters(){}

    public FilterParameters(HashMap arguments) {
         ArrayList<Double> colourDoubles = (ArrayList<Double>) arguments.get("colour");
         if(colourDoubles != null){
             setReplacementColour(colourDoubles);
         }
         String backgroundImagePath = (String) arguments.get("background");
         if(backgroundImagePath != null){
             backgroundImage = backgroundImagePath;
         }
    }

     public float[] getColorToReplace(){
         return replacementColour;
     }

     public void setReplacementColour(ArrayList<Double> colours){
        if (colours != null) {
            float [] newColours = new float[3];
            for (int i = 0; i < colours.size() && i <= 2; i++) {
                newColours[i] = colours.get(i).floatValue() / 255.0f;
            }
            replacementColour = newColours;
        }
     }


}

