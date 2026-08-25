package com.mas6y6.musmeta.utils;

import java.awt.*;

public class ColorWrapper extends Color {
    public ColorWrapper(Color color) {
        super(color.getRGB());
    }
    public ColorWrapper(int r, int g, int b, int a) {
        super(r,g,b,a);
    }

    public static ColorWrapper addWrapper(Color color) {
        return new ColorWrapper(color);
    }

    public ColorWrapper brighter(float factor) {
        int r = getRed();
        int g = getGreen();
        int b = getBlue();
        int alpha = getAlpha();

        int i = (int)(1.0/(1.0-factor));
        if ( r == 0 && g == 0 && b == 0) {
            return new ColorWrapper(i, i, i, alpha);
        }
        if ( r > 0 && r < i ) r = i;
        if ( g > 0 && g < i ) g = i;
        if ( b > 0 && b < i ) b = i;

        return addWrapper(new Color(Math.min((int)(r/factor), 255),
                Math.min((int)(g/factor), 255),
                Math.min((int)(b/factor), 255),
                alpha));
    }

    public ColorWrapper darker(float factor) {
        return addWrapper(new Color(Math.max((int)(getRed()  *factor), 0),
                Math.max((int)(getGreen()*factor), 0),
                Math.max((int)(getBlue() *factor), 0),
                getAlpha()));
    }

}
