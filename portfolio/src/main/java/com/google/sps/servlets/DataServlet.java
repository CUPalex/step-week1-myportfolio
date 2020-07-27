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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;
import com.google.sps.data.Comment;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/** Servlet that stores and returns comments */
@WebServlet("/comments")
public class DataServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    final int DEFAULT_COMMENTS_NUMBER = 3;
    // try to get maxComments parameter from request and check if it is valid
    // if not valid - set to default
    int maxNumberOfComments;
    try {
        maxNumberOfComments = Integer.parseInt(request.getParameter("maxcomments"));
    } catch (NumberFormatException e) {
        maxNumberOfComments = DEFAULT_COMMENTS_NUMBER;
    }
    if (maxNumberOfComments < 0) {
        maxNumberOfComments = DEFAULT_COMMENTS_NUMBER;
    }

    // get comments from datastore
    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    
    // put comments into arraylist
    ArrayList<Comment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable(FetchOptions.Builder.withLimit(maxNumberOfComments))) {
      String  commentText = (String) entity.getProperty("commentText");
      String  commentOwner = (String) entity.getProperty("commentOwner");
      long timestamp = (long) entity.getProperty("timestamp");
      Comment comment = new Comment(commentText, commentOwner, timestamp);
      comments.add(comment);
    }

    // convert arraylist to json string
    Gson gson = new Gson();
    String json = gson.toJson(comments);

    // send response
    response.setContentType("text/html;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // get comment fields from form
    String comment = request.getParameter("comment-text");
    String owner = request.getParameter("comment-owner");
    // if comment or owner is empty - redirect back and do nothing
    // TODO : do something apart from redirect
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
