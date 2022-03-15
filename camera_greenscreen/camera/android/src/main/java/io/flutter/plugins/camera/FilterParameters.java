package io.flutter.plugins.camera;


import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *  FilterParameters is a DAO for the updatable parameters for the Filters
 */

public final class FilterParameters {

    HueRange chromaKeyRange;
    String   backgroundImage;
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
        private Double highValue;

        HueRange(Double lowValue, Double highValue){
            this.lowValue = lowValue;
            this.highValue = highValue;
        }

        public Double getHue(){
            return lowValue;
        }

    }