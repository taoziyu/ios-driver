/*
 * Copyright 2012 ios-driver committers.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.uiautomation.ios.ide;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.uiautomation.ios.ide.controllers.IDECommandController;
import org.uiautomation.ios.ide.controllers.IDEController;
import org.uiautomation.ios.ide.controllers.NotImplementedIDEController;
import org.uiautomation.ios.ide.controllers.RefreshController;
import org.uiautomation.ios.ide.controllers.ResourceController;
import org.uiautomation.ios.ide.controllers.SessionGuesserController;
import org.uiautomation.ios.ide.controllers.TreeController;
import org.uiautomation.ios.ide.model.Cache;
import org.uiautomation.ios.ide.model.CacheDefaultImpl;
import org.uiautomation.ios.ide.views.View;
import org.uiautomation.ios.server.IOSDriver;
import org.uiautomation.ios.server.IOSServer;


public class IDEServlet extends HttpServlet {

  // TODO freynaud
  String path;
  String url;
  // JSONObject cache = null;
  private final Model model = new Model();

  private static final long serialVersionUID = 333974658353413397L;

  private final List<IDECommandController> controllers = new ArrayList<IDECommandController>();

  @Override
  public void init() throws ServletException {
    super.init();
    URL u = null;
   
    try {
      IOSDriver driver = (IOSDriver) getServletContext().getAttribute(IOSServer.DRIVER);
      int port = driver.getPort();
      u = new URL(System.getProperty("endpoint", "http://localhost:"+port+"/wd/hub"));
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    Cache cache = (Cache) getServletContext().getAttribute(Cache.KEY);
    if (cache == null) {
      cache = new CacheDefaultImpl(u);
    }

    controllers.add(new SessionGuesserController(cache));
    controllers.add(new IDEController(cache));
    controllers.add(new ResourceController(cache));
    controllers.add(new RefreshController(cache));
    controllers.add(new TreeController(cache));


  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {
    response.setCharacterEncoding("UTF-8");
    try {
      IDECommandController controller = getController(req.getPathInfo());
      View view = controller.handle(req);
      view.render(response);
    } catch (Exception e) {
      // TODO freynaud
      throw new RuntimeException(e);
    }
  }



  private IDECommandController getController(String pathInfo) {
    for (IDECommandController c : controllers) {
      if (c.canHandle(pathInfo)) {
        return c;
      }
    }
    return new NotImplementedIDEController();
  }



}
