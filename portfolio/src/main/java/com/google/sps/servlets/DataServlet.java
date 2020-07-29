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

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import com.google.sps.data.CommentsSend;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/* Servlet that stores and returns comments */
@WebServlet("/comments")
public class DataServlet extends HttpServlet {

  /* expects maxcomments parameter (int) and 
   *.        cursor parameter (string)
   * if parameters don't exist, returns MAX_COMMENTS_NUMBER of first comments
   * if parameters are invalid - redirects to '/'
   * returns json of CommentsSend object
   * the number of comments equals to maxcomments (or less if maxcomments is
   * greater than total number of comments in database)
   * cursor sets the point in query from which to start returning data (similar to offset,
   * but doesn't load data before cursor point)
   */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final int MAX_COMMENTS_NUMBER = 1000;
    // try to get maxComments parameter from request and check if it is valid
    // if parameter is empty - set to MAX_COMMENTS_NUMBER, i.e. return practically all comments
    // if parameter is invalid - redirect
    String maxCommentsString = request.getParameter("maxcomments");
    if (maxCommentsString == null) {
        maxCommentsString = Integer.toString(MAX_COMMENTS_NUMBER);
    }
    int maxNumberOfComments;
    try {
        maxNumberOfComments = Integer.parseInt(maxCommentsString);
    } catch (NumberFormatException e) {
        response.sendRedirect("/");
        return;
    }
    if (maxNumberOfComments < 0 || maxNumberOfComments > MAX_COMMENTS_NUMBER) {
        response.sendRedirect("/");
        return;
    }

    // make prepared query of comments from datastore
    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery pq = datastore.prepare(query);
    // additional options for query - limit of entities
    FetchOptions fetchOptions = FetchOptions.Builder.withLimit(maxNumberOfComments);
    // try to get cursor parameter
    // if empty - think like it is set to beginning
    // if invalid - redirect
    String startCursor = request.getParameter("cursor");
    if (startCursor != null) {
      fetchOptions.startCursor(Cursor.fromWebSafeString(startCursor));
    }
    QueryResultList<Entity> results;
    try {
      results = pq.asQueryResultList(fetchOptions);
    } catch (IllegalArgumentException e) {
      // invalid cursor
      response.sendRedirect("/");
      return;
    }
    
    // put comments into arraylist. request from prepared query only maxNumber of first comments
    ArrayList<Comment> comments = new ArrayList<>();
    for (Entity entity : results) {
      String  commentText = (String) entity.getProperty("commentText");
      String  commentOwner = (String) entity.getProperty("commentOwner");
      long timestamp = (long) entity.getProperty("timestamp");
      Comment comment = new Comment(commentText, commentOwner, timestamp);
      comments.add(comment);
    }

    // to send cursor back
    String cursorString = results.getCursor().toWebSafeString();

    // object to send back
    CommentsSend commentsSend = new CommentsSend(cursorString, comments);

    // convert commentSend object to json string
    Gson gson = new Gson();
    String json = gson.toJson(commentsSend);

    // send response
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /* expects comment-text, comment-owner parameters
   * returns redirect
   * puts comment to the database
   */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // get comment fields from form
    String comment = request.getParameter("comment-text");
    String owner = request.getParameter("comment-owner");
    // if comment or owner is empty - redirect back and do nothing
    if (comment == null || owner == null) {
        response.sendRedirect("/#comments");
    }
    // get time for comment entity
    long timestamp = System.currentTimeMillis();

    // create comment entity
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("commentText", comment);
    commentEntity.setProperty("commentOwner", owner);
    commentEntity.setProperty("timestamp", timestamp);

    // put comment entity int the database
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);

    // send back to index page
    response.sendRedirect("/#comments");
  }
}
