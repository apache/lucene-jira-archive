#!/bin/bash

# Create distance Package
mkdir contrib/spatial/src/java/org/apache/lucene/spatial/distance
svn add contrib/spatial/src/java/org/apache/lucene/spatial/distance/

# Create util Package
mkdir contrib/spatial/src/java/org/apache/lucene/spatial/util
svn add contrib/spatial/src/java/org/apache/lucene/spatial/util/

# Move GeoHashUtils from geohash package to utils package
svn move contrib/spatial/src/java/org/apache/lucene/spatial/geohash/GeoHashUtils.java contrib/spatial/src/java/org/apache/lucene/spatial/util/GeoHashUtils.java

# Remove geohash package since the classes are redundant
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/geohash

# Move Point2D class from geometry.shape to geometry and rename it as Point
svn move contrib/spatial/src/java/org/apache/lucene/spatial/geometry/shape/Point2D.java contrib/spatial/src/java/org/apache/lucene/spatial/geometry/Point.java

# Remove geometry.shape package as all classes are unnecessary
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/geometry/shape

# Move DistanceUnits to util package and rename as DistanceUnit
svn move contrib/spatial/src/java/org/apache/lucene/spatial/geometry/DistanceUnits.java contrib/spatial/src/java/org/apache/lucene/spatial/util/DistanceUnit.java

# Clean out geometry package
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/geometry/CartesianPoint.java
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/geometry/FixedLatLng.java
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/geometry/FloatLatLng.java

# Rename tier.projections to tier.projection
svn move contrib/spatial/src/java/org/apache/lucene/spatial/tier/projections/ contrib/spatial/src/java/org/apache/lucene/spatial/tier/projection

# Move CartesianTierPlotter up on package level
svn move contrib/spatial/src/java/org/apache/lucene/spatial/tier/projection/CartesianTierPlotter.java contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianTierPlotter.java

# Rename IProjector to Projector
svn move contrib/spatial/src/java/org/apache/lucene/spatial/tier/projection/IProjector.java contrib/spatial/src/java/org/apache/lucene/spatial/tier/projection/Projector.java

# Rename CartesianPolyFilterBuilder to CartesianShapeFilterBuilder
svn move contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianPolyFilterBuilder.java contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShapeFilterBuilder.java

# Move DistanceFieldComparatorSource up one package level
svn move contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceFieldComparatorSource.java contrib/spatial/src/java/org/apache/lucene/spatial/DistanceFieldComparatorSource.java

# Move DistanceFilter to distance package
svn move contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceFilter.java contrib/spatial/src/java/org/apache/lucene/spatial/distance/DistanceFilter.java

# Clear out tier package of redundant classes
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceHandler.java
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceQueryBuilder.java
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/tier/DistanceUtils.java
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/tier/InvalidGeoException.java
svn delete contrib/spatial/src/java/org/apache/lucene/spatial/tier/LatLongDistanceFilter.java

# Rename Shape to CartesianShape to avoid confusion
svn move contrib/spatial/src/java/org/apache/lucene/spatial/tier/Shape.java contrib/spatial/src/java/org/apache/lucene/spatial/tier/CartesianShape.java

# Bring test packaging and classes upto date
svn move contrib/spatial/src/test/org/apache/lucene/spatial/tier/TestCartesian.java contrib/spatial/src/test/org/apache/lucene/spatial/TestCartesian.java
svn delete contrib/spatial/src/test/org/apache/lucene/spatial/tier/DistanceCheck.java
svn delete contrib/spatial/src/test/org/apache/lucene/spatial/tier/PolyShape.java



