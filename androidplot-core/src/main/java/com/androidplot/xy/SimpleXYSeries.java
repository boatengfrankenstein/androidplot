/*
 * Copyright 2015 AndroidPlot.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.androidplot.xy;

import android.graphics.Canvas;
import com.androidplot.Plot;
import com.androidplot.PlotListener;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A convenience class used to create instances of XYPlot generated from Lists of Numbers.
 */
public class SimpleXYSeries implements XYSeries, PlotListener {

    private static final String TAG = SimpleXYSeries.class.getName();

    @Override
    public void onBeforeDraw(Plot source, Canvas canvas) {
        lock.readLock().lock();
    }

    @Override
    public void onAfterDraw(Plot source, Canvas canvas) {
        lock.readLock().unlock();
    }

    public enum ArrayFormat {
        Y_VALS_ONLY,
        XY_VALS_INTERLEAVED
    }

    private volatile LinkedList<Number> xVals = new LinkedList<Number>();
    private volatile LinkedList<Number> yVals = new LinkedList<Number>();
    private volatile String title = null;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);


    public SimpleXYSeries(String title) {
        this.title = title;
    }

    public SimpleXYSeries(ArrayFormat format, String title, Number... model) {
        this(asNumberList(model), format, title);
    }

    protected static List<Number> asNumberList(Number... model) {
        List<Number> numbers = new ArrayList<>();
        for(Number n : model) {
            numbers.add(n);
        }
        return numbers;
    }

    /**
     * Generates an XYSeries instance from the List of numbers passed in.  This is a convenience class
     * and should only be used for static data models; it is not suitable for representing dynamically
     * changing data.
     *
     * @param model  A List of Number elements comprising the data model.
     * @param format Format of the model.  A format of Y_VALS_ONLY means that the array only contains y-values.
     *               For this format x values are autogenerated using values of 0 through n-1 where n is the size of the model.
     * @param title  Title of the series
     */
    public SimpleXYSeries(List<? extends Number> model, ArrayFormat format, String title) {
        this(title);
        setModel(model, format);
    }

    public SimpleXYSeries(List<? extends Number> xVals, List<? extends Number> yVals, String title) {
        this(title);
        if(xVals == null || yVals == null) {
            throw new IllegalArgumentException("Neither the xVals nor the yVals parameters may be null.");
        }

        if(xVals.size() != yVals.size()) {
            throw new IllegalArgumentException("xVals and yVals List parameters must be of the same size.");
        }

        this.xVals.addAll(xVals);
        this.yVals.addAll(yVals);
    }

    /**
     * Use index value as xVal, instead of explicit, user provided xVals.
     */
    public void useImplicitXVals() {
        lock.writeLock().lock();
        try {
            xVals = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Use the provided list of Numbers as yVals and their corresponding indexes as xVals.
     * @param model A List of Number elements comprising the data model.
     * @param format Format of the model.  A format of Y_VALS_ONLY means that the array only contains y-values.
     *               For this format x values are autogenerated using values of 0 through n-1 where n is the size of the model.
     */
    public void setModel(List<? extends Number> model, ArrayFormat format) {

        lock.writeLock().lock();
        try {
            // empty the current values:
            xVals = null;
            yVals.clear();

            // make sure the new model has data:
            if (model == null || model.size() == 0) {
                return;
            }

            switch (format) {

                // array containing only y-vals. assume x = index:
                case Y_VALS_ONLY:
                    for(Number n : model) {
                        yVals.add(n);
                    }
                    break;

                // xy interleaved array:
                case XY_VALS_INTERLEAVED:
                    if (xVals == null) {
                        xVals = new LinkedList<Number>();
                    }
                    if (model.size() % 2 != 0) {
                        throw new IndexOutOfBoundsException("Cannot auto-generate series from odd-sized xy List.");
                    }
                    // always need an x and y array so init them now:
                    int sz = model.size() / 2;
                    for (int i = 0, j = 0; i < sz; i++, j += 2) {
                        xVals.add(model.get(j));
                        yVals.add(model.get(j + 1));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected enum value: " + format);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets individual x value based on index
     * @param value
     * @param index
     */
    public void setX(Number value, int index) {
        lock.writeLock().lock();
        try {
            xVals.set(index, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets individual y value based on index
     * @param value
     * @param index
     */
    public void setY(Number value, int index) {
        lock.writeLock().lock();
        try {
            yVals.set(index, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sets xy values based on index
     * @param xVal
     * @param yVal
     * @param index
     */
    public void setXY(Number xVal, Number yVal, int index) {
        lock.writeLock().lock();
        try {
            yVals.set(index, yVal);
            xVals.set(index, xVal);
        } finally {lock.writeLock().unlock();}
    }

    public void addFirst(Number x, Number y) {
        lock.writeLock().lock();
        try {
            if (xVals != null) {
                xVals.addFirst(x);
            }
            yVals.addFirst(y);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     *
     * @return {@link XYCoords} with first equal to x-val and second equal to y-val.
     */
    public XYCoords removeFirst() {
        lock.writeLock().lock();
        try {
            if (size() <= 0) {
                throw new NoSuchElementException();
            }
            return new XYCoords(xVals != null ? xVals.removeFirst() : 0, yVals.removeFirst());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void addLast(Number x, Number y) {
        lock.writeLock().lock();
        try {
            if (xVals != null) {
                xVals.addLast(x);
            }
            yVals.addLast(y);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     *
     * @return {@link XYCoords} with first equal to x-val and second equal to y-val.
     */
    public XYCoords removeLast() {
        lock.writeLock().lock();
        try {
            if (size() <= 0) {
                throw new NoSuchElementException();
            }
            return new XYCoords(xVals != null ? xVals.removeLast() : yVals.size() - 1, yVals.removeLast());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        lock.writeLock().lock();
        try {
            this.title = title;
        } finally {lock.writeLock().unlock();}
    }

    @Override
    public int size() {
        return yVals != null ? yVals.size() : 0;
    }

    @Override
    public Number getX(int index) {
        return xVals != null ? xVals.get(index) : index;
    }

    @Override
    public Number getY(int index) {
        return yVals.get(index);
    }

    public LinkedList<Number> getxVals() {
        return xVals;
    }

    public LinkedList<Number> getyVals() {
        return yVals;
    }

    /**
     * Remove all values from the series
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            xVals.clear();
            yVals.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
