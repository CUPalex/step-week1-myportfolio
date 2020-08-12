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
    /* expects maxcomments parameter (int); default: MAX_COMMENTS_NUMBER
     *.        timestamp parameter (long); default: current time + const
     *.        direction (next or previous : string); default: next
     *.        commentsonpage (int) (currently); default: maxcomments
     * if parameters are invalid - redirects to '/'
     * returns json of Commeend object
     * CommentsSend.comments consists of maxcomments (or less) comments, which timestamp >
     * lastTimestamp if direction = next, or <= lastTimestamp (- maxcomments comments) if direction = previous
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
                response.sendRedirect("/");
                return;
            }
        }
        if (maxNumberOfComments < 0 || maxNumberOfComments > MAX_COMMENTS_NUMBER) {
            response.sendRedirect("/");
            return;
        }

        // get lastTimestamp parameter
        String lastTimestampString = request.getParameter("timestamp");
        long lastTimestamp;
        if (lastTimestampString == null) {
            lastTimestamp = System.currentTimeMillis() + 10;
        } else {
            try {
                lastTimestamp = Long.parseLong(lastTimestampString);
            } catch (NumberFormatException e) {
                response.sendRedirect("/");
                return;
            }
        }

        // get direction parameter
        String direction = request.getParameter("direction");
        if (direction == null) {
            direction = "next";
        }
        if (!direction.equals("next") && !direction.equals("previous")) {
            response.sendRedirect("/");
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
                response.sendRedirect("/");
                return;
            }
        }

        // query options
        Filter timeFilter;
        SortDirection sortDirection;
        FetchOptions fetchOptions;

        // set query options depending on whether the user want to see next or previous comments
        if (direction.equals("next")) {
            timeFilter =
                    new FilterPredicate("timestamp", FilterOperator.LESS_THAN, lastTimestamp);
            sortDirection = SortDirection.DESCENDING;
            fetchOptions = FetchOptions.Builder.withLimit(maxNumberOfComments);
        } else {
            // if previous - sort query in other direction and get comments that are
            // currently on page and those we will load
            timeFilter =
                    new FilterPredicate("timestamp", FilterOperator.GREATER_THAN_OR_EQUAL, lastTimestamp);
            sortDirection = SortDirection.ASCENDING;
            fetchOptions = FetchOptions.Builder.withLimit(maxNumberOfComments + commentsOnPage);
        }

        // get datastore
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        // check if user wants next comments, and current comment is already the last one
        // if so, return current comments
        long minTimestamp = getMinTimestamp(datastore);
        if (minTimestamp == lastTimestamp && direction.equals("next")) {
            timeFilter =
                    new FilterPredicate("timestamp", FilterOperator.GREATER_THAN_OR_EQUAL, lastTimestamp);
            sortDirection = SortDirection.ASCENDING;
            fetchOptions = FetchOptions.Builder.withLimit(commentsOnPage);
            direction = "stay";
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

        // reverse comments and offset current comments if direction==previous
        if (direction.equals("previous")) {
            Collections.reverse(comments);
            comments = new ArrayList<Comment>(
                comments.subList(0, Math.min(maxNumberOfComments, comments.size())));
        }

        // reverse comments if direction==stay
        if (direction.equals("stay")) {
            Collections.reverse(comments);
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

    /* expects comment-text, comment-owner, comment-image parameters
     * returns redirect
     * puts comment to the database
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
