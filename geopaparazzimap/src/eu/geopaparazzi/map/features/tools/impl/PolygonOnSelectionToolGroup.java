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
package eu.geopaparazzi.map.features.tools.impl;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.os.Parcelable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKBReader;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.style.ToolColors;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.map.GPMapPosition;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.MapsSupportService;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.features.Feature;
import eu.geopaparazzi.map.features.FeatureUtilities;
import eu.geopaparazzi.map.features.editing.EditManager;
import eu.geopaparazzi.map.features.editing.EditingView;
import eu.geopaparazzi.map.features.tools.interfaces.Tool;
import eu.geopaparazzi.map.features.tools.interfaces.ToolGroup;
import eu.geopaparazzi.map.gui.FeaturePagerActivity;
import eu.geopaparazzi.map.jts.MapviewPointTransformation;
import eu.geopaparazzi.map.jts.android.PointTransformation;
import eu.geopaparazzi.map.jts.android.ShapeWriter;
import eu.geopaparazzi.map.layers.interfaces.IEditableLayer;
import eu.geopaparazzi.map.proj.OverlayViewProjection;
import eu.geopaparazzi.map.utils.MapUtilities;

/**
 * The group of tools active when a selection has been done.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PolygonOnSelectionToolGroup implements ToolGroup, OnClickListener, OnTouchListener, OnSelectionToolGroup {

    private final GPMapView mapView;

    private int buttonSelectionColor;
    private List<Feature> selectedFeatures = new ArrayList<>();
    private ImageButton deleteFeatureButton;
    private OverlayViewProjection editingViewProjection;

    private Paint geometryPaintStroke = new Paint();
    private Paint geometryPaintFill = new Paint();

    private final Paint selectedGeometryPaintStroke = new Paint();
    private final Paint selectedGeometryPaintFill = new Paint();
    private final Paint selectedPreviewGeometryPaintStroke = new Paint();
    private final Paint selectedPreviewGeometryPaintFill = new Paint();

    private WKBReader wkbReader = new WKBReader();

    /**
     * Stores the top-left map position at which the redraw should happen.
     */
    private final Point point;

    /**
     * Stores the map position after drawing is finished.
     */
    private Point positionBeforeDraw;
    private ImageButton editAttributesButton;

    private boolean isInDeletePreview;

    private ImageButton commitButton;

    private ImageButton undoButton;

    /**
     * Constructor.
     *
     * @param mapView          the map view.
     * @param selectedFeatures the set of selected features.
     */
    public PolygonOnSelectionToolGroup(GPMapView mapView, List<Feature> selectedFeatures) {
        this.mapView = mapView;
        this.selectedFeatures.addAll(selectedFeatures);

        EditingView editingView = EditManager.INSTANCE.getEditingView();
        editingViewProjection = new OverlayViewProjection(mapView, editingView);
        buttonSelectionColor = Compat.getColor(editingView.getContext(), R.color.main_selection);


        int stroke = ColorUtilities.toColor(ToolColors.selection_stroke.getHex());
        int fill = ColorUtilities.toColor(ToolColors.selection_fill.getHex());
        selectedGeometryPaintFill.setAntiAlias(true);
        selectedGeometryPaintFill.setColor(fill);
        selectedGeometryPaintFill.setAlpha(180);
        selectedGeometryPaintFill.setStyle(Paint.Style.FILL);
        selectedGeometryPaintStroke.setAntiAlias(true);
        selectedGeometryPaintStroke.setStrokeWidth(5f);
        selectedGeometryPaintStroke.setColor(stroke);
        selectedGeometryPaintStroke.setStyle(Paint.Style.STROKE);

        stroke = ColorUtilities.toColor(ToolColors.preview_stroke.getHex());
        fill = ColorUtilities.toColor(ToolColors.preview_fill.getHex());
        selectedPreviewGeometryPaintFill.setAntiAlias(true);
        selectedPreviewGeometryPaintFill.setColor(fill);
        selectedPreviewGeometryPaintFill.setAlpha(180);
        selectedPreviewGeometryPaintFill.setStyle(Paint.Style.FILL);
        selectedPreviewGeometryPaintStroke.setAntiAlias(true);
        selectedPreviewGeometryPaintStroke.setStrokeWidth(5f);
        selectedPreviewGeometryPaintStroke.setColor(stroke);
        selectedPreviewGeometryPaintStroke.setStyle(Paint.Style.STROKE);

        geometryPaintFill = selectedGeometryPaintFill;
        geometryPaintStroke = selectedGeometryPaintStroke;

        point = new Point();
        positionBeforeDraw = new Point();
    }

    public void activate() {
        if (mapView != null)
            mapView.setClickable(true);
    }

    public void initUI() {
        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        parent.removeAllViews();

        Context context = parent.getContext();
        IEditableLayer editLayer = EditManager.INSTANCE.getEditLayer();
        int padding = 2;

        if (editLayer != null) {
            deleteFeatureButton = new ImageButton(context);
            deleteFeatureButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            deleteFeatureButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_delete_feature_24dp));
            deleteFeatureButton.setPadding(0, padding, 0, padding);
            deleteFeatureButton.setOnTouchListener(this);
            deleteFeatureButton.setOnClickListener(this);
            parent.addView(deleteFeatureButton);

//            copyFeatureButton = new ImageButton(context);
//            copyFeatureButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//                    LayoutParams.WRAP_CONTENT));
//            copyFeatureButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_copy_geoms_24dp));
//            copyFeatureButton.setPadding(0, padding, 0, padding);
//            copyFeatureButton.setOnTouchListener(this);
//            copyFeatureButton.setOnClickListener(this);
//            parent.addView(copyFeatureButton);

            editAttributesButton = new ImageButton(context);
            editAttributesButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT));
            editAttributesButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_view_attributes_24dp));
            editAttributesButton.setPadding(0, padding, 0, padding);
            editAttributesButton.setOnTouchListener(this);
            editAttributesButton.setOnClickListener(this);
            parent.addView(editAttributesButton);

            undoButton = new ImageButton(context);
            undoButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            undoButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_undo_24dp));
            undoButton.setPadding(0, padding, 0, padding);
            undoButton.setOnTouchListener(this);
            undoButton.setOnClickListener(this);
            parent.addView(undoButton);

            commitButton = new ImageButton(context);
            commitButton.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
            commitButton.setBackground(Compat.getDrawable(context, R.drawable.ic_editing_commit_24dp));
            commitButton.setPadding(0, padding, 0, padding);
            commitButton.setOnTouchListener(this);
            commitButton.setOnClickListener(this);
            parent.addView(commitButton);
            commitButton.setVisibility(View.GONE);
        }
    }

    public void disable() {
        LinearLayout parent = EditManager.INSTANCE.getToolsLayout();
        if (parent != null)
            parent.removeAllViews();
        parent = null;
    }

    public void onClick(View v) {
        if (v == editAttributesButton) {
            if (selectedFeatures.size() > 0) {
                Context context = v.getContext();
                Intent intent = new Intent(context, FeaturePagerActivity.class);
                intent.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
                        (ArrayList<? extends Parcelable>) selectedFeatures);
                intent.putExtra(FeatureUtilities.KEY_READONLY, false);
                if (context instanceof Activity) {
                    Activity activity = (Activity) context;
                    activity.startActivityForResult(intent, MapUtilities.SELECTED_FEATURES_UPDATED_RETURN_CODE);
                } else {
                    context.startActivity(intent);
                }
            }
        } else if (v == deleteFeatureButton) {
            if (!isInDeletePreview) {
                isInDeletePreview = true;
                geometryPaintFill = selectedPreviewGeometryPaintFill;
                geometryPaintStroke = selectedPreviewGeometryPaintStroke;
                commitButton.setVisibility(View.VISIBLE);
                EditManager.INSTANCE.invalidateEditingView();
            }
//        } else if (v == copyFeatureButton) {
//            Context context = v.getContext();
//            GPDialogs.warningDialog(context, "Thsi feature is cuurently disabled.", null);
//            if (selectedFeatures.size() > 0) {
//                List<Feature> copySelectedFeatures = new ArrayList<>(selectedFeatures);
//                Context context = v.getContext();
//                Intent intent = new Intent(context, CopyToLayersListActivity.class);
//                intent.putParcelableArrayListExtra(FeatureUtilities.KEY_FEATURESLIST,
//                        (ArrayList<? extends Parcelable>) copySelectedFeatures);
//                context.startActivity(intent);
//
//                selectedFeatures.clear();
//                EditManager.INSTANCE.setActiveToolGroup(new PolygonMainEditingToolGroup(mapView));
//                EditManager.INSTANCE.setActiveTool(null);
//
//            }
        } else if (v == undoButton) {
            if (isInDeletePreview) {
                /*
                 * if in delete preview, disable it
                 */
                isInDeletePreview = false;
                geometryPaintFill = selectedGeometryPaintFill;
                geometryPaintStroke = selectedGeometryPaintStroke;
                commitButton.setVisibility(View.GONE);
                EditManager.INSTANCE.invalidateEditingView();
            } else if (selectedFeatures.size() > 0) {
                /*
                 * if in selection mode, clear the selection
                 */
                selectedFeatures.clear();
                EditManager.INSTANCE.setActiveToolGroup(new PolygonMainEditingToolGroup(mapView));
                EditManager.INSTANCE.setActiveTool(null);
                commitButton.setVisibility(View.GONE);
            }
        } else if (v == commitButton) {
            if (isInDeletePreview) {
                isInDeletePreview = false;

                try {
                    // delete features
                    IEditableLayer editLayer = EditManager.INSTANCE.getEditLayer();
                    editLayer.deleteFeatures(selectedFeatures);
                    selectedFeatures.clear();

                    // reset drawview
                    EditManager.INSTANCE.setActiveToolGroup(new PolygonMainEditingToolGroup(mapView));
                    EditManager.INSTANCE.setActiveTool(null);

                } catch (Exception e) {
                    GPLog.error(this, null, e);
                }

                geometryPaintFill = selectedGeometryPaintFill;
                geometryPaintStroke = selectedGeometryPaintStroke;
            }
        }
    }

    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                v.getBackground().setColorFilter(buttonSelectionColor, Mode.SRC_ATOP);
                v.invalidate();
                break;
            }
            case MotionEvent.ACTION_UP: {
                v.getBackground().clearColorFilter();
                v.invalidate();
                break;
            }
        }
        return false;
    }

    public void onToolFinished(Tool tool) {
        // nothing
    }


    public void onToolDraw(Canvas canvas) {
        try {
            if (selectedFeatures.size() > 0) {
                OverlayViewProjection projection = editingViewProjection;

                int zoomLevelBeforeDraw;
                synchronized (mapView) {
                    zoomLevelBeforeDraw = mapView.getMapPosition().getZoomLevel();
                    positionBeforeDraw = projection.toPoint(mapView.getMapPosition().getCoordinate(), positionBeforeDraw,
                            (byte) zoomLevelBeforeDraw);
                }

                // calculate the top-left point of the visible rectangle
                point.x = positionBeforeDraw.x - (canvas.getWidth() >> 1);
                point.y = positionBeforeDraw.y - (canvas.getHeight() >> 1);

                GPMapPosition mapPosition = mapView.getMapPosition();
                byte zoomLevel = (byte) mapPosition.getZoomLevel();

                PointTransformation pointTransformer = new MapviewPointTransformation(projection, point, zoomLevel);
                ShapeWriter shapeWriter = new ShapeWriter(pointTransformer);
                shapeWriter.setRemoveDuplicatePoints(true);
                // shapeWriter.setDecimation(spatialTable.getStyle().decimationFactor);

                // draw features
                for (Feature feature : selectedFeatures) {
                    Geometry defaultGeometry = feature.getDefaultGeometry();
                    if (defaultGeometry != null) {
                        try {
                            FeatureUtilities.drawGeometry(defaultGeometry, canvas, shapeWriter, geometryPaintFill, geometryPaintStroke);
                        } catch (Exception e) {
                            GPLog.error(this, null, e); //$NON-NLS-1$
                        }
                    }
                }
            }
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }
    }

    public boolean onToolTouchEvent(MotionEvent event) {
        return false;
    }

    public void onGpsUpdate(double lon, double lat) {
        // ignore
    }

    /**
     * Forces a feature selection.
     * <p/>
     * <p>Previous selections are cleared and a redrawing is triggered.
     *
     * @param features the new features to select.
     */
    public void setSelectedFeatures(List<Feature> features) {
        selectedFeatures.clear();
        selectedFeatures.addAll(features);
        EditManager.INSTANCE.invalidateEditingView();
    }
}