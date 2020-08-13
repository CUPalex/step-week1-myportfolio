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

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.IllegalArgumentException;

import java.util.logging.Logger;

/* Servlet to use Blobstore - API to upload and store images.
 * To use blobstore you should add a special url to the form,
 * which you use to submit images. On that url blobstore takes care of the submitted image
 * and then redirects to your servlet, which will handle post request.
 * You will be able to get submitted image url from that servlet.
 */
@WebServlet("/blobstore-upload-url")
public class BlobstoreUploadServlet extends HttpServlet {
    static Logger log = Logger.getLogger(BlobstoreUploadServlet.class.getName());

    /**
     * Expects forwardurl parameter - url to handle post request.
     * Returns an url to set to form's action attribute.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String forwardUrl = request.getParameter("forwardurl");

        // if forwardurl is not provided - throw error
        if (forwardUrl == null) {
            throw400error(response);
            return;
        }
        
        BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
        String uploadUrl;

        // Exception is thrown if forwardurl is invalid. In this case throw error
        try {
            uploadUrl = blobstoreService.createUploadUrl(forwardUrl);
        } catch (IllegalArgumentException e) {
            throw400error(response);
            return;
        }

        response.setContentType("text/html");
        response.getWriter().println(uploadUrl);
    }

    /**
     * Changes response so that it will return  400 error
     */
    private static void throw400error(HttpServletResponse response) throws IOException {
        response.setContentType("text/html;");
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.getWriter().println("<html><body><h1>HTTP 400 error</h1>" +
                "<h2>Invalid request parameters</h2>" +
                "<a href='/'>return to homepage</a></body></html>");
    }
}
