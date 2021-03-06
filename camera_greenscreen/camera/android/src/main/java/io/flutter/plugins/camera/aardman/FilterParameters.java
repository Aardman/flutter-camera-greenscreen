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

    float  []  replacementColour = null ;
    String   backgroundImage = null;
    float  sensitivity = Constants.FLOAT_NOT_SET;

    public FilterParameters(){}

    public FilterParameters(HashMap arguments) {
         ArrayList<Double> colourDoubles = (ArrayList<Double>) arguments.get("colour");
         if(colourDoubles != null){
             setReplacementColour(colourDoubles);
         }
         Double thresholdSensitivity = (Double) arguments.get("sensitivity");
         if (thresholdSensitivity != null){
             sensitivity =  thresholdSensitivity.floatValue();
         }
         String backgroundImagePath = (String) arguments.get("backgroundPath");
         if(backgroundImagePath != null){
             backgroundImage = backgroundImagePath;
         }
    }

     public float[] getColorToReplace(){
         return replacementColour;
     }

     public float getSensitivity(){
        return sensitivity;
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

     public void updateWith(FilterParameters newParameters) {
        if (newParameters.backgroundImage != null){
            backgroundImage = newParameters.backgroundImage;
        }
        if (newParameters.replacementColour != null){
            replacementColour = newParameters.replacementColour;
        }
        if (newParameters.getSensitivity() != Constants.FLOAT_NOT_SET) {
            sensitivity = newParameters.sensitivity;
        }
     }



}

