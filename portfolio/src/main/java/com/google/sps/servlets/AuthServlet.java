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
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gson.Gson;
import com.google.sps.data.AuthSend;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.List;

/* Servlet for authentification */
@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

    /* expects nothing
     * returns AuthSend json object which consist of
     *.        String url (login or logout) and bool isLoggedIn
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // initialize variables to send them in response
        String url;
        Boolean isLoggedIn;

        // get reference to UserService
        UserService userService = UserServiceFactory.getUserService();

        // set url and isLoggedIn vars
        if (userService.isUserLoggedIn()) {
            String urlToRedirectToAfterUserLogsOut = "/";
            url = userService.createLogoutURL(urlToRedirectToAfterUserLogsOut);
            isLoggedIn = true;
        } else {
            String urlToRedirectToAfterUserLogsIn = "/";
            url = userService.createLoginURL(urlToRedirectToAfterUserLogsIn);
            isLoggedIn = false;
        }
        
        // create object to send
        AuthSend responseObj = new AuthSend(isLoggedIn, url);

        // convert it to json
        Gson gson = new Gson();
        String json = gson.toJson(responseObj);

        // send response
        response.setContentType("application/json");
        response.getWriter().println(json);
    }
}
