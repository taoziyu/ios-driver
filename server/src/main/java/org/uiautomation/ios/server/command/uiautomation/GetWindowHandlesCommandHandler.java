package org.uiautomation.ios.server.command.uiautomation;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;
import org.openqa.selenium.remote.Response;
import org.uiautomation.ios.UIAModels.UIAWebView;
import org.uiautomation.ios.UIAModels.configuration.WorkingMode;
import org.uiautomation.ios.UIAModels.predicate.TypeCriteria;
import org.uiautomation.ios.communication.WebDriverLikeRequest;
import org.uiautomation.ios.server.IOSDriver;
import org.uiautomation.ios.server.ServerSideSession;
import org.uiautomation.ios.server.command.BaseNativeCommandHandler;

public class GetWindowHandlesCommandHandler extends BaseNativeCommandHandler {

  public GetWindowHandlesCommandHandler(IOSDriver driver, WebDriverLikeRequest request) {
    super(driver, request);
  }

  @Override
  public Response handle() throws Exception {

    ServerSideSession sss = getDriver().getSession(getRequest().getSession());
    
    Set<String> handles = new HashSet<String>();
    handles.add(WorkingMode.Native.toString());
    if (sss.getNativeDriver().findElements(new TypeCriteria(UIAWebView.class)).size() > 0) {
      handles.add(WorkingMode.Web.toString());
    }
    Response resp = new Response();
    resp.setSessionId(getSession().getSessionId());
    resp.setStatus(0);
    resp.setValue(handles);
    return resp;
  }
  
  @Override
  public JSONObject configurationDescription() throws JSONException {
    return noConfigDefined();
  }

}
