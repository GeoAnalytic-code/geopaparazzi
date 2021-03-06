/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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

package eu.geopaparazzi.library.forms.constraints;

/**
 * An interface for constraints.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IConstraint {

    /**
     * Applies the current filter to the supplied value.
     * 
     * @param value the value to check.
     */
    void applyConstraint(Object value);

    /**
     * Getter for the constraint's result.
     * 
     * @return <code>true</code> if the constraint applies.
     */
    boolean isValid();
    
    /**
     * Getter for the description of the constraint.
     * 
     * @return the description of the constraint.
     */
    String getDescription();
}
