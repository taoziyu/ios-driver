package org.uiautomation.ios.mobileSafari;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.json.JSONObject;
import org.uiautomation.ios.server.ServerSideSession;
import org.uiautomation.ios.webInspector.DOM.RemoteExceptionException;

public class DebugProtocol {

  private Socket socket;
  private ByteArrayOutputStream buf = new ByteArrayOutputStream();
  private final PlistManager plist = new PlistManager();
  private final MessageHandler handler;

  private final String LOCALHOST_IPV6 = "::1";
  private final int port = 27753;

  private int commandId = 0;

  private final boolean displayPerformance = false;
  private Thread listen;
  private volatile boolean keepGoing = true;
  private final String bundleId;

  /**
   * connect to the webview
   * 
   * @param handler
   *          for server initiated notifications
   * @throws UnknownHostException
   * @throws IOException
   * @throws InterruptedException
   */
  public DebugProtocol(EventListener listener, String bundleId, ServerSideSession session) throws Exception,
      InterruptedException {
    this.handler = new DefaultMessageHandler(listener);
    this.bundleId = bundleId;

    init();

    listen = new Thread(new Runnable() {

      @Override
      public void run() {
        try {
          while (keepGoing) {
            Thread.sleep(50);
            listenOnce();
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    listen.start();
  }

  public void init() throws Exception {
    if (socket != null && (socket.isConnected() || !socket.isClosed())) {
      socket.close();
    }
    socket = new Socket(LOCALHOST_IPV6, port);
    sendCommand(PlistManager.SET_CONNECTION_KEY);
    sendCommand(PlistManager.CONNECT_TO_APP);
    sendCommand(PlistManager.SET_SENDER_KEY);
  }

  /**
   * sends the json formated command.
   * 
   * @param command
   *          . For command format, read
   *          https://www.webkit.org/blog/?p=1875&preview=true.
   * @return
   * @throws Exception
   */
  public JSONObject sendCommand(JSONObject command) throws Exception {
    commandId++;
    command.put("id", commandId);

    long start = System.currentTimeMillis();

    String xml = plist.JSONCommand(command);
    // perf("got xml \t" + (System.currentTimeMillis() - start) + "ms.");
    xml = xml.replace("$bundleId", this.bundleId);

    byte[] bytes = plist.plistXmlToBinary(xml);
    // perf("prepared request \t" + (System.currentTimeMillis() - start) +
    // "ms.");
    sendBinaryMessage(bytes);
    // perf("sent request \t\t" + (System.currentTimeMillis() - start) + "ms.");
    JSONObject response = handler.getResponse(command.getInt("id"));
    // perf("got response\t\t" + (System.currentTimeMillis() - start) + "ms.");
    JSONObject error = response.optJSONObject("error");
    if (error != null) {
      throw new RemoteExceptionException(error, command);
    } else if (response.optBoolean("wasThrown", false)) {
      throw new Exception("remote JS exception " + response.toString(2));
    } else {
      perf(System.currentTimeMillis()+ "\t\t"+(System.currentTimeMillis() - start) + "ms\t" + command.getString("method") + " " + command);
      return response.getJSONObject("result");
    }
  }

  private void perf(String msg) {
    if (displayPerformance) {
      System.out.println(msg);
    }
  }

  /**
   * Some commands do not follow the Remote Debugging protocol. For instance the
   * ones that initialize the connection between the webview and the remote
   * debugger do not have json content, they're just an exchange of keys.
   * 
   * @param command
   * @throws IOException
   * @throws InterruptedException
   */
  private void sendCommand(String command) throws Exception {
    String xml = plist.loadFromTemplate(command);
    xml = xml.replace("$bundleId", bundleId);
    byte[] bytes = plist.plistXmlToBinary(xml);
    sendBinaryMessage(bytes);
  }

  /**
   * sends the message to the AUT.
   * 
   * @param bytes
   * @throws IOException
   */
  private void sendBinaryMessage(byte[] bytes) throws IOException {
    OutputStream os = socket.getOutputStream();
    os.write((byte) ((bytes.length >> 24) & 0xFF));
    os.write((byte) ((bytes.length >> 16) & 0xFF));
    os.write((byte) ((bytes.length >> 8) & 0xFF));
    os.write((byte) (bytes.length & 0xFF));
    // System.err.println("about to send " + bytes.length + " bytes.");
    os.write(bytes);
    // System.err.println("Sending " + bytes.length + " bytes.");
  }

  /**
   * reads the messages from the AUT.
   * 
   * @param inputBytes
   * @throws IOException
   * @throws InterruptedException
   */
  private void pushInput(byte[] inputBytes) throws Exception {
    buf.write(inputBytes);
    while (buf.size() >= 4) {
      byte[] bytes = buf.toByteArray();
      int size = 0;
      size = (size << 8) + byteToInt(bytes[0]);
      size = (size << 8) + byteToInt(bytes[1]);
      size = (size << 8) + byteToInt(bytes[2]);
      size = (size << 8) + byteToInt(bytes[3]);
      if (bytes.length >= 4 + size) {
        String message = plist.plistBinaryToXml(Arrays.copyOfRange(bytes, 4, size + 4));
        handler.handle(message);
        buf = new ByteArrayOutputStream();
        buf.write(bytes, 4 + size, bytes.length - size - 4);
      } else {
        // System.err.println("Expecting " + size + " + 4 bytes. Buffered " +
        // bytes.length + ".");
        break;
      }
    }
  }

  /**
   * listen for a complete message.
   * 
   * @throws IOException
   * @throws InterruptedException
   */
  private void listenOnce() throws Exception {
    InputStream is = socket.getInputStream();
    while (is.available() > 0) {
      byte[] bytes = new byte[is.available()];
      is.read(bytes);
      // System.err.println("Received " + bytes.length + " bytes.");
      pushInput(bytes);
    }
  }

  private int byteToInt(byte b) {
    int i = (int) b;
    return i >= 0 ? i : i + 256;
  }

  public void stop() {
    if (handler != null) {
      handler.stop();
    }

    try {
      socket.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    keepGoing = false;
    if (listen != null) {
      listen.interrupt();
    }
    keepGoing = true;

  }

}
