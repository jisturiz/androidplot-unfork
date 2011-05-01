package com.androidplot.xy;

import android.graphics.*;
import com.androidplot.series.XYSeries;
import com.androidplot.exception.PlotRenderException;
import com.androidplot.util.ValPixConverter;

import java.util.ArrayList;

/**
 * Renders a point as a line with the vertices marked.  Requires 2 or more points to
 * be rendered.
 */
public class LineAndPointRenderer<FormatterType extends LineAndPointFormatter> extends XYSeriesRenderer<FormatterType> {

    //private PointF lastPoint;

    public LineAndPointRenderer(XYPlot plot) {
        super(plot);
    }

    @Override
    public void onRender(Canvas canvas, RectF plotArea) throws PlotRenderException {



        for(XYSeries series : getPlot().getSeriesListForRenderer(this.getClass())) {

            drawSeries(canvas, plotArea, series, getFormatter(series));
        }
    }

    @Override
    public void doDrawLegendIcon(Canvas canvas, RectF rect, LineAndPointFormatter formatter) {
        // horizontal icon:
        float centerY = rect.centerY();
        float centerX = rect.centerX();

        if(formatter.getFillPaint() != null) {
            canvas.drawRect(rect, formatter.getFillPaint());
        }
        if(formatter.getLinePaint() != null) {
            canvas.drawLine(rect.left, rect.bottom, rect.right, rect.top, formatter.getLinePaint());
        }

        if(formatter.getVertexPaint() != null) {
            canvas.drawPoint(centerX, centerY, formatter.getVertexPaint());
        }
    }

    /**
     * This method exists for StepRenderer to override without having to duplicate any
     * additional code.
     */
    protected void appendToPath(Path path, PointF thisPoint, PointF lastPoint) {

        path.lineTo(thisPoint.x, thisPoint.y);
    }


    private void drawSeries(Canvas canvas, RectF plotArea, XYSeries series, LineAndPointFormatter formatter) throws PlotRenderException {
        PointF thisPoint = null;
        PointF lastPoint = null;
        PointF firstPoint = null;
        //PointF lastDrawn = null;
        Path path = null;
        ArrayList<PointF> points = new ArrayList<PointF>(series.size());
        for (int i = 0; i < series.size(); i++) {
            Number y = series.getY(i);
            Number x = series.getX(i);

            if (y != null && x != null) {
                thisPoint = ValPixConverter.valToPix(
                        x,
                        y,
                        plotArea,
                        getPlot().getCalculatedMinX(),
                        getPlot().getCalculatedMaxX(),
                        getPlot().getCalculatedMinY(),
                        getPlot().getCalculatedMaxY());
                points.add(thisPoint);
                //appendToPath(path, thisPoint, lastPoint);
            } else {
                thisPoint = null;
            }

            if(thisPoint != null) {

                // record the first point of the new Path
                if(firstPoint == null) {
                    path = new Path();
                    firstPoint = thisPoint;
                    // create our first point at the bottom/x position so filling
                    // will look good
                    path.moveTo(firstPoint.x, firstPoint.y);
                }

                if(lastPoint != null) {
                    appendToPath(path, thisPoint, lastPoint);
                }

                lastPoint = thisPoint;
            } else {
                if(lastPoint != null) {
                    renderPath(canvas, plotArea, path, firstPoint, lastPoint, formatter);
                }
                firstPoint = null;
                lastPoint = null;
            }
        }
        if(firstPoint != null) {
            renderPath(canvas, plotArea, path, firstPoint, lastPoint, formatter);
        }

        // TODO: benchmark this against drawPoints(float[]);
        if (formatter.getVertexPaint() != null) {
            for (PointF p : points) {
                canvas.drawPoint(p.x, p.y, formatter.getVertexPaint());
            }
        }
    }

    protected void renderPath(Canvas canvas, RectF plotArea, Path path, PointF firstPoint, PointF lastPoint, LineAndPointFormatter formatter) {
        Path outlinePath = new Path(path);
            // create our last point at the bottom/x position so filling
            // will look good
            path.lineTo(lastPoint.x, plotArea.bottom);
            path.lineTo(firstPoint.x, plotArea.bottom);
            path.close();
        if (formatter.getFillPaint() != null) {
            canvas.drawPath(path, formatter.getFillPaint());
        }


        //}

        // draw any visible regions on top of the base region:
        double minX = getPlot().getCalculatedMinX().doubleValue();
        double maxX = getPlot().getCalculatedMaxX().doubleValue();
        double minY = getPlot().getCalculatedMinY().doubleValue();
        double maxY = getPlot().getCalculatedMaxY().doubleValue();

        // TODO: move to class decl
        //Paint regionPaint = new Paint();
        //regionPaint.setStyle(Paint.Style.FILL);

        // draw each region:
        for (XYRegion r : XYRegion.regionsWithin(formatter.getRegions().elements(), minX, maxX, minY, maxY)) {
            XYRegionFormatter f = formatter.getRegionFormatter(r);
            RectF regionRect = r.getRectF(plotArea, minX, maxX, minY, maxY);
            if (regionRect != null) {
                canvas.save();
                canvas.clipPath(path);
                canvas.drawRect(regionRect, f.getPaint());
                canvas.restore();
            }
        }

        // finally we draw the outline path on top of everything else:
        canvas.drawPath(outlinePath, formatter.getLinePaint());

        if (path != null) {
            path.rewind();
        }
    }
}
