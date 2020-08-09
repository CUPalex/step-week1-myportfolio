// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A class to find slot for a meeting.
 */
public final class FindMeetingQuery {
    /**
     * Function to find all possible slots for a meeting of group of people, with fixed duration.
     * @return collection of TimeRanges of all availible slots.
     */
    public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
        // make an array for getEmptyTimeRanges function
        // put there all significant events' START ad END points assuming that event is a segment on time line.
        ArrayList<Point> meetingsPoints = new ArrayList<>();

        for (Event event : events) {
            if (doIntersect(event.getAttendees(), request.getAttendees())) {
                meetingsPoints.add(new Point(Point.Type.START, event));
                meetingsPoints.add(new Point(Point.Type.END, event));
            }
        }
        
        return getEmptyTimeRanges(meetingsPoints, TimeRange.START_OF_DAY, TimeRange.END_OF_DAY, request.getDuration());
    }

    /**
     * @return all segments from firstPoint to lastPoint inclusive which are no shorter than minLength
     *.        and do not intersect with segments from intersectingSegments
     * @param intersectingSegments - array of Points. To put segment in this array, you should put its
     *.                              START and END Points there.
     * @param firstPoint - beginning of all time. Returned collection will not contain time before it.
     * @param lastPoint - end of all time. Returned collection will not contain time after it.
     * @param minLength - minimal length for each of the returned intervals
     * Algorithm:
     *.        All empty slots can start only where some segment ends and end only where some segment starts (or at
               firstPoint or lastPoint)
     *.        There cannot be any other points from intersectingSegments inside the empty slot - only its start end end.
     *         Sort all points in intersectingSegments in ascending order and look at them one by one.
     *.        Count how many segments are currently covering the point you are looking at: get a counter
     *.        which increments every time you look at START point and decrements every time you look at END point.
     *.        If the point is covered with 0 segments - it is a start of segment which can be one of the answer
     *.        segments if it has appropriate length.
     *.        So every time we look at a point, we check if the previous interval was segment-free and if so, we check
     *.        the length of that interval to be at leash minLength.
     */
    private Collection<TimeRange> getEmptyTimeRanges(ArrayList<Point> intersectingSegments, int firstPoint, int lastPoint, long minLength) {
        Collections.sort(intersectingSegments, Point.ORDER_START_BEFORE_END);

        int openSegments = 0;
        int lastTime = firstPoint;

        // the array we will return
        ArrayList<TimeRange> emptyTimeRanges = new ArrayList<>();

        for (Point point : intersectingSegments) {
            if (point.type == Point.Type.START) {
                if (openSegments == 0 && point.time - lastTime >= minLength) {
                    emptyTimeRanges.add(TimeRange.fromStartEnd(lastTime, point.time, false));
                }
                openSegments++;
            } else {
                openSegments--;
            }
            lastTime = point.time;
        }

        // check if the segment ending at lastPoint fits
        if (lastPoint - lastTime >= minLength) {
            emptyTimeRanges.add(TimeRange.fromStartEnd(lastTime, lastPoint, true));
        }

        return emptyTimeRanges;
    }

    /**
     * @return true if collections have any common items and false otherwise
     */
    private static boolean doIntersect(Collection<String> collection1, Collection<String> collection2) {
        for (String item : collection1) {
            if (collection2.contains(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Point of segment. Can be START or END, have its coors on timeline - time, and Comparator by time.
     * Has only Point(Type, Event) constructor - makes a point of given type, assuming that event is a segment on timeline.
     */
    private static final class Point {
        public static enum Type {
            START, END
        }
        public final Type type;
        public final int time;
        Point(Type type, Event event) {
            this.type = type;
            if (type == Type.START) {
                time = event.getWhen().start();
            } else {
                time = event.getWhen().end();
            }
        }
        /**
         * Sorts by time. If times are equal - START point is less than END point.
         */
        public static final Comparator<Point> ORDER_START_BEFORE_END = new Comparator<Point>() {
            @Override
            public int compare(Point a, Point b) {
                if (a.time == b.time) {
                    if (a.type == b.type) {
                        return 0;
                    }
                    if (a.type == Type.START) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                return Long.compare(a.time, b.time);
            }
        };
    }
}
