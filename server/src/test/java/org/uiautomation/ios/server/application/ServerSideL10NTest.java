package org.uiautomation.ios.server.application;

import java.io.IOException;

import org.json.JSONException;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.uiautomation.ios.SampleApps;
import org.uiautomation.ios.UIAModels.predicate.NameCriteria;
import org.uiautomation.ios.exceptions.InvalidCriteriaException;

public class ServerSideL10NTest {



  private ServerSideL10NFactory create(Localizable l) {
    IOSApplication app = new IOSApplication(SampleApps.getIntlMountainsFile());
    app.setLanguage(l.getName());
    ServerSideL10NFactory factory = new ServerSideL10NFactory(app);
    return factory;
  }

  @Test(expectedExceptions = InvalidCriteriaException.class)
  public void throwsProperly() throws JSONException {
    ServerSideL10NFactory factory = create(Localizable.en);
    factory.nameCriteria("I don't exist");
  }


  @Test
  public void nameEN() throws JSONException {
    ServerSideL10NFactory factory = create(Localizable.en);
    NameCriteria c = factory.nameCriteria("rootViewNavTitle");
    Assert.assertEquals(c.getValue(), "Mountains");
    System.out.println(c.stringify().toString());
  }

  @Test
  public void nameFR() throws JSONException {
    ServerSideL10NFactory factory = create(Localizable.fr);
    NameCriteria c = factory.nameCriteria("rootViewNavTitle");
    Assert.assertEquals(c.getValue(), "Montagnes");
    System.out.println(c.stringify().toString());
  }

  @Test
  public void nameZH() throws JSONException, IOException {
    ServerSideL10NFactory factory = create(Localizable.zh);
    NameCriteria c = factory.nameCriteria("rootViewNavTitle");
    Assert.assertEquals(c.getValue(), "山");
  }


  @Test
  public void nameWithParametersEN() {
    ServerSideL10NFactory factory = create(Localizable.en);
    NameCriteria c = factory.nameCriteria("footFormat");
    Assert.assertEquals(c.getValue(), "(.*){1} feet");
  }

  @Test
  public void nameWithParametersFR() {
    ServerSideL10NFactory factory = create(Localizable.fr);
    NameCriteria c = factory.nameCriteria("footFormat");
    Assert.assertEquals(c.getValue(), "(.*){1} pieds");
  }

  @Test
  public void nameWithParametersZH() {
    ServerSideL10NFactory factory = create(Localizable.zh);
    NameCriteria c = factory.nameCriteria("footFormat");
    Assert.assertEquals(c.getValue(), "(.*){1}英尺");
  }
}
