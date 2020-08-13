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

package com.google.sps.servlets;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.images.ServingUrlOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import com.google.sps.data.CommentsSend;

import java.io.IOException;
import java.lang.ClassCastException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/* Servlet that stores and returns comments */
@WebServlet("/comments")
public class DataServlet extends HttpServlet {
    /* Expects maxcomments parameter of type int
     *.        timestamp parameter of type long
     *.        direction of type string. Can be either "next" or "previous"
     *.        commentsonpage of type int. Means how many comments are now on page
     * If parameters are not set in the request - sets them to default. Default parameters are:
     *         for maxcomments - MAX_COMMENTS_NUMBER
     *.        for timestamp - current time
     *.        for direction - "next"
     *.        for commentsonpage - maxcomments
     * If parameters are invalid - returns 400 error.
     * Returns json of CommentSend object.
     * CommentsSend.comments consists of maxcomments (or less) comments, which timestamp >
     * lastTimestamp if direction = next, or <= lastTimestamp (- maxcomments number comments) if direction = previous.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        final int MAX_COMMENTS_NUMBER = 1000;

        // get maxComments parameter
        String maxNumberOfCommentsString = request.getParameter("maxcomments");
        int maxNumberOfComments;
        if (maxNumberOfCommentsString == null) {
            maxNumberOfComments = MAX_COMMENTS_NUMBER;
        } else {
            try {
                maxNumberOfComments = Integer.parseInt(maxNumberOfCommentsString);
            } catch (NumberFormatException e) {
                throw400error(response);
                return;
            }
        }
        if (maxNumberOfComments < 0 || maxNumberOfComments > MAX_COMMENTS_NUMBER) {
            throw400error(response);
            return;
        }

        // get lastTimestamp parameter
        String lastTimestampString = request.getParameter("timestamp");
        long lastTimestamp;
        if (lastTimestampString == null) {
            lastTimestamp = System.currentTimeMillis();
        } else {
            try {
                lastTimestamp = Long.parseLong(lastTimestampString);
            } catch (NumberFormatException e) {
                throw400error(response);
                return;
            }
        }

        // get direction parameter
        String direction = request.getParameter("direction");
        if (direction == null) {
            direction = "next";
        }
        if (!direction.equals("next") && !direction.equals("previous")) {
            throw400error(response);
            return;
        }

        // get commentsOnPage parameter (on current page)
        String commentsOnPageString = request.getParameter("commentsonpage");
        int commentsOnPage;
        if (commentsOnPageString == null) {
            commentsOnPage = maxNumberOfComments;
        } else {
            try {
                commentsOnPage = Integer.parseInt(commentsOnPageString);
            } catch (NumberFormatException e) {
                throw400error(response);
                return;
            }
        }

        // query options
        Filter timeFilter;
        SortDirection sortDirection;
        FetchOptions fetchOptions;

        // set query options depending on whether the user wants to see next or previous comments
        if (direction.equals("next")) {
            timeFilter =
                    new FilterPredicate("timestamp", FilterOperator.LESS_THAN, lastTimestamp);
            sortDirection = SortDirection.DESCENDING;
            fetchOptions = FetchOptions.Builder.withLimit(maxNumberOfComments);
        } else {
            /* If user wants to see previous comments - sort query in other direction and get comments
             * that are currently on page + those we will load on page.
             * We get comments that are currently on page, because they are earlier in the query,
             * than the comments we need. We will offset them later.
             */
            timeFilter =
                    new FilterPredicate("timestamp", FilterOperator.GREATER_THAN_OR_EQUAL, lastTimestamp);
            sortDirection = SortDirection.ASCENDING;
            fetchOptions = FetchOptions.Builder.withLimit(maxNumberOfComments + commentsOnPage);
        }

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        /* Check if user wants next comments, and current comment is already the last one.
         * If so, return current comments.
         * We change direction to previous here, because we will get the comments
         * from reversed query, the same way we get previous comments.
         * We just won't offset them later.
         */
        long minTimestamp = getMinTimestamp(datastore);
        if (minTimestamp == lastTimestamp && direction.equals("next")) {
            timeFilter =
                    new FilterPredicate("timestamp", FilterOperator.GREATER_THAN_OR_EQUAL, lastTimestamp);
            sortDirection = SortDirection.ASCENDING;
            fetchOptions = FetchOptions.Builder.withLimit(commentsOnPage);
            direction = "previous";
        }

        // make prepared query of comments from datastore
        Query query = new Query("Comment").addSort("timestamp", sortDirection).setFilter(timeFilter);
        PreparedQuery pq = datastore.prepare(query);

        // put comments into arraylist
        ArrayList<Comment> comments = new ArrayList<>();
        for (Entity entity : pq.asIterable(fetchOptions)) {
            String commentText = (String) entity.getProperty("commentText");
            String commentOwner = (String) entity.getProperty("commentOwner");
            long timestamp = (long) entity.getProperty("timestamp");
            String commentImageUrl = (String) entity.getProperty("commentImageUrl");
            Comment comment = new Comment(commentText, commentOwner, timestamp, commentImageUrl);
            comments.add(comment);
        }

        /* If direction==previous, reverse comments and offset comments that are currently on page.
         * Here we also take care of the case when user wants to see previous comments,
         * and the comments currently on page are already the first ones.
         * In this case, comment ArrayList consists only of comments that are currently on page,
         * and we won't offset them.
         */
        if (direction.equals("previous")) {
            Collections.reverse(comments);
            /* Math.min function takes care of edge case when there are less than
             * maxNumberOfComments comments in database
             */
            comments = new ArrayList<Comment>(
                comments.subList(0, Math.min(maxNumberOfComments, comments.size())));
        }

        // object to send back
        long newTimestamp;
        try {
            newTimestamp = comments.get(comments.size() - 1).getTimestamp();
        } catch (IndexOutOfBoundsException e) {
            newTimestamp = 0;
        }
        CommentsSend commentsSend = new CommentsSend(newTimestamp, comments);

        // convert commentSend object to json string
        Gson gson = new Gson();
        String json = gson.toJson(commentsSend);

        // send response
        response.setContentType("application/json;");
        response.getWriter().println(json);
    }

    /* This method is used to submit a form with new comment and put it to the database.
     * Expects comment-text, comment-owner string and commet-image file parameters from form.
     * Returns redirect to '/#comments'
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        UserService userService = UserServiceFactory.getUserService();
        
        // if users are not logged in, they can't post comments - send them to "/"
        if (!userService.isUserLoggedIn()) {
            response.sendRedirect("/");
            return;
        }
        
        // get comment fields from form
        String comment = request.getParameter("comment-text");
        String owner = request.getParameter("comment-owner");
        // if comment or owner is empty - redirect back and do nothing
        if (comment == null || owner == null) {
            response.sendRedirect("/#comments");
        }

        // get the URL of the image parameter via Blobstore
        String commentImageUrl = getUploadedFileUrl(request, "comment-image");

        // get time for comment entity
        long timestamp = System.currentTimeMillis();

        // create comment entity
        Entity commentEntity = new Entity("Comment");
        commentEntity.setProperty("commentText", comment);
        commentEntity.setProperty("commentOwner", owner);
        commentEntity.setProperty("timestamp", timestamp);
        commentEntity.setProperty("commentImageUrl", commentImageUrl);

        // put comment entity int the database
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(commentEntity);

        // send back to index page
        response.sendRedirect("/#comments");
    }

    /* gets min timestamp from datastore of comments
     */
    private static long getMinTimestamp(DatastoreService datastore) {
        Query query = new Query("Comment").addSort("timestamp", SortDirection.ASCENDING);
        FetchOptions fetchOptions = FetchOptions.Builder.withLimit(1);
        PreparedQuery pq = datastore.prepare(query);
        long minTimestamp;
        try {
            minTimestamp = (long) pq.asList(fetchOptions).get(0).getProperty("timestamp");
        } catch (IndexOutOfBoundsException e) {
            minTimestamp = 0;
        }
        return minTimestamp;
    }

    /* changes response so that it will return  400 error */
    private static void throw400error(HttpServletResponse response) throws IOException {
        response.setContentType("text/html;");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().println("<html><body><h1>HTTP 400 error</h1>" +
                "<h2>Invalid request parameters</h2>" +
                "<a href='/'>return to homepage</a></body></html>");
    }
    
    /** Returns a URL that points to the uploaded file, or null if the user didn't upload a file. */
    private String getUploadedFileUrl(HttpServletRequest request, String formInputElementName) {
        BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
        Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
        List<BlobKey> blobKeys = blobs.get(formInputElementName);

        // User submitted form without selecting a file, so we can't get a URL. (dev server)
        if (blobKeys == null || blobKeys.isEmpty()) {
            return null;
        }

        // Our form only contains a single file input, so get the first index.
        BlobKey blobKey = blobKeys.get(0);

        // User submitted form without selecting a file, so we can't get a URL. (live server)
        BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
        if (blobInfo.getSize() == 0) {
            blobstoreService.delete(blobKey);
            return null;
        }

        // We could check the validity of the file here, e.g. to make sure it's an image file
        // https://stackoverflow.com/q/10779564/873165

        // Use ImagesService to get a URL that points to the uploaded file.
        ImagesService imagesService = ImagesServiceFactory.getImagesService();
        ServingUrlOptions options = ServingUrlOptions.Builder.withBlobKey(blobKey);

        // To support running in Google Cloud Shell with AppEngine's dev server, we must use the relative
        // path to the image, rather than the path returned by imagesService which contains a host.
        try {
            URL url = new URL(imagesService.getServingUrl(options));
            return url.getPath();
        } catch (MalformedURLException e) {
            return imagesService.getServingUrl(options);
        }
    }
}
