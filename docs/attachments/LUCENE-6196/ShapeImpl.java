/**
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements. See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.lucene.whatever;

import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Rectangle;
import com.spatial4j.core.shape.SpatialRelation;
import com.spatial4j.core.shape.impl.RectangleImpl;
import com.spatial4j.core.context.SpatialContext;

import org.apache.lucene.geo3d.GeoPoint;
import org.apache.lucene.geo3d.GeoShape;
import org.apache.lucene.geo3d.GeoArea;
import org.apache.lucene.geo3d.GeoAreaWorld;
import org.apache.lucene.geo3d.GeoAreaLatitudeZone;
import org.apache.lucene.geo3d.GeoAreaRectangle;
import org.apache.lucene.geo3d.Bounds;

public class GeoShapeIImpl implements Shape {

    public final SpatialContext ctx;
    public final GeoShape shape;
    
    public Rectangle boundingBox = null;

    public final static double RADIANS_PER_DEGREE = Math.PI / 180.0;
    public final static double DEGREES_PER_RADIAN = 1.0 / RADIANS_PER_DEGREE;

    public PathImpl(GeoShape shape,
                    SpatialContext ctx) {
        this.ctx = ctx;
        this.shape = shape;
    }

    @Override
    public SpatialRelation relate(Shape other) {
        if (other instanceof Rectangle)
            return relate((Rectangle)other);
        else if (other instanceof Point)
            return relate((Point)other);
        else
            throw new RuntimeException("Unimplemented shape relationship determination");
    }

    protected SpatialRelation relate(Rectangle r) {
        // Construct the right kind of GeoArea first
        GeoArea geoArea = GeoAreaFactory.makeGeoArea(r.getMaxY() * RADIANS_PER_DEGREE,
            r.getMinY() * RADIANS_PER_DEGREE,
            r.getMinX() * RADIANS_PER_DEGREE,
            r.getMaxX() * RADIANS_PER_DEGREE);
        int relationship = geoArea.getRelationship(shape);
        if (relationship == GeoArea.WITHIN)
            return SpatialRelation.WITHIN;
        else if (relationship == GeoArea.CONTAINS)
            return SpatialRelation.CONTAINS;
        else if (relationship == GeoArea.OVERLAPS)
            return SpatialRelation.INTERSECTS;
        else if (relationship == GeoArea.DISJOINT)
            return SpatialRelation.DISJOINT;
        else
            throw new RuntimeException("Unknown relationship returned: "+relationship);
    }


    protected SpatialRelation relate(Point p) {
        // Create a GeoPoint
        GeoPoint point = new GeoPoint(p.getY()*RADIANS_PER_DEGREE,p.getX()*RADIANS_PER_DEGREE);
        if (shape.isWithin(point)) {
            // Point within shape
            return SpatialRelation.CONTAINS;
        }
        return SpatialRelation.DISJOINT;
    }

    @Override
    public Rectangle getBoundingBox() {
        if (boundingBox == null) {
            Bounds bounds = shape.getBounds();
            double leftLon;
            double rightLon;
            if (bounds.checkNoLongitudeBound()) {
                leftLon = -180.0;
                rightLon = 180.0;
            } else {
                leftLon = bounds.getLeftLongitude().doubleValue() * DEGREES_PER_RADIAN;
                rightLon = bounds.getRightLongitude().doubleValue() * DEGREES_PER_RADIAN;
            }
            double minLat;
            if (bounds.checkNoBottomLatitudeBound()) {
                minLat = -90.0;
            } else {
                minLat = bounds.getMinLatitude().doubleValue() * DEGREES_PER_RADIAN;
            }
            double maxLat;
            if (bounds.checkNoTopLatitudeBound()) {
                maxLat = 90.0;
            } else {
                maxLat = bounds.getMaxLatitude().doubleValue() * DEGREES_PER_RADIAN;
            }
            boundingBox = new RectangleImpl(leftLon, rightLon, minLat, maxLat, ctx);
        }
        return boundingBox;
    }

    @Override
    public boolean hasArea() {
        return true;
    }

    @Override
    public double getArea(SpatialContext ctx) {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Point getCenter() {
        throw new RuntimeException("Unimplemented");
    }

    @Override
    public Shape getBuffered(double distance, SpatialContext ctx) {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof GeoShapeImpl))
            return false;
        GeoShapeImpl tr = (GeoShapeImpl)other;
        return tr.shape.equals(shape);
    }

}
