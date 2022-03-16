package io.flutter.plugins.camera;


import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *  FilterParameters is a DAO for the updatable parameters for the Filters
 */

public final class FilterParameters {

    HueRange chromaKeyRange = new HueRange(0.25, 0.45);
    Double sensitivity = 1.0;
    String   backgroundImage = null;
    // ArrayList maskBounds ;

    public FilterParameters() {}

    public void update(@NonNull HashMap arguments) {
        System.out.println("🤖 updating filter parameters with\n" + arguments);
        String bg = (String) arguments.get("backGroundImage");
        if (bg != null) { this.backgroundImage = bg; }
        ArrayList<Double> hueRange = (ArrayList<Double>) arguments.get("hueRange");
        if (hueRange != null) {
            this.chromaKeyRange = new HueRange(hueRange.get(0), hueRange.get(1));
        }
     }

}

 class HueRange {

        private Double lowValue;
        private Double highValue = 0.0;  //may not be used depending on filter choice

        HueRange(Double lowValue, Double highValue){
            this.lowValue = lowValue;
            this.highValue = highValue;
        }

        Double getHue() {
            return lowValue;
        }

    }
