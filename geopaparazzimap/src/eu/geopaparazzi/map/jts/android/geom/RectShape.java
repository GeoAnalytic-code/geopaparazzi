/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.geopaparazzi.map.jts.android.geom;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/**
 * A rectangular {@link DrawableShape shape}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RectShape implements DrawableShape {

    protected RectF rectF;

    public RectShape() {
        this.rectF = new RectF(0f, 0f, 0f, 0f);
    }

    public RectShape( RectF rectF ) {
        this.rectF = rectF;
    }

    public RectShape( float x, float y, float width, float height ) {
        this.rectF = new RectF(x, y, x + width, y + height);
    }

    public void draw( Canvas canvas, Paint paint ) {
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(rectF, paint);
    }

    public void fill( Canvas canvas, Paint paint ) {
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(rectF, paint);
    }

    public void fillAndStroke( Canvas canvas, Paint paint ) {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(rectF, paint);
    }
}
