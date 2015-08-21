import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import sun.misc.BASE64Encoder;

import com.heroku.sdk.jdbc.DatabaseUrl;

public class Main extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getRequestURI().endsWith("/proximo")) {
      showProximo(req,resp);
    } else if (req.getRequestURI().endsWith("/fixie")) {
      showFixie(req,resp);
    } else if (req.getRequestURI().endsWith("/quotaguard")) {
      showFixie(req,resp);
    } else {
      resp.getWriter().print("<p><a href='/proximo'>Proximo</a></p>");
      resp.getWriter().print("<p><a href='/fixie'>Fixie</a></p>");
      resp.getWriter().print("<p><a href='/quotaguard'>QuotaGuard</a></p>");
    }
  }

  private void showProximo(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    String urlStr = "http://httpbin.org/ip";

    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet request = new HttpGet(urlStr);

    CloseableHttpResponse response = httpClient.execute(request);
    try {
      resp.getWriter().print("Hello from Java! " + handleResponse(response));
    } catch (Exception e) {
      resp.getWriter().print(e.getMessage());
    }
  }

  private void showFixie(HttpServletRequest request, HttpServletResponse resp)
      throws ServletException, IOException {

    URL proximo = new URL(System.getenv("FIXIE_URL"));
    String userInfo = proximo.getUserInfo();
    String user = userInfo.substring(0, userInfo.indexOf(':'));
    String password = userInfo.substring(userInfo.indexOf(':') + 1);

    DefaultHttpClient httpclient = new DefaultHttpClient();
    try {
      // HttpHost proxy = new HttpHost(System.getenv("FIXIE_URL"), 80, "http");

      httpclient.getCredentialsProvider().setCredentials(
            new AuthScope(proximo.getHost(), 80),
            new UsernamePasswordCredentials(user, password));
      HttpHost proxy = new HttpHost(proximo.getHost(), 80);
      httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
      String encodedAuth = new BASE64Encoder().encode(userInfo.getBytes());

      HttpHost target = new HttpHost(proximo.getHost(), 80, "http");
      HttpGet req = new HttpGet("/ip");
      req.setHeader("Host", "httpbin.org");
      req.setHeader("Proxy-Authorization", "Basic " + encodedAuth);

      System.out.println("executing request to " + target + " via "
              + proxy);
      HttpResponse rsp = httpclient.execute(target, req);
      HttpEntity entity = rsp.getEntity();

      System.out.println("----------------------------------------");
      System.out.println(rsp.getStatusLine());
      Header[] headers = rsp.getAllHeaders();
      for (int i = 0; i < headers.length; i++) {
          System.out.println(headers[i]);
      }
      System.out.println("----------------------------------------");

      if (entity != null) {
          System.out.println(EntityUtils.toString(entity));
      }
    } finally {
        // When HttpClient instance is no longer needed,
        // shut down the connection manager to ensure
        // immediate deallocation of all system resources
        httpclient.getConnectionManager().shutdown();
    }
    resp.getWriter().print("Hello from Java! ");
  }

  private static String handleResponse(CloseableHttpResponse response) throws IOException {
    StatusLine statusLine = response.getStatusLine();
    HttpEntity entity = response.getEntity();
    if (statusLine.getStatusCode() >= 300) {
      throw new HttpResponseException(
          statusLine.getStatusCode(),
          statusLine.getReasonPhrase());
    }
    if (entity == null) {
      throw new ClientProtocolException("Response contains no content");
    }
    return readStream(entity.getContent());
  }

  private static String readStream(InputStream is) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
    String output = "";
    String tmp = reader.readLine();
    while (tmp != null) {
      output += tmp;
      tmp = reader.readLine();
    }
    return output;
  }

  public static void main(String[] args) throws Exception{
    final Thread mainThread = Thread.currentThread();
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.out.println("Goodbye world");
        try {
          // mainThread.join();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });

    // URL proximo = new URL(System.getenv("FIXIE_URL"));
    // String userInfo = proximo.getUserInfo();
    // String user = userInfo.substring(0, userInfo.indexOf(':'));
    // String password = userInfo.substring(userInfo.indexOf(':') + 1);
    // System.setProperty("socksProxyHost", proximo.getHost());
    // Authenticator.setDefault(new ProxyAuthenticator(user, password));

    Server server = new Server(Integer.valueOf(System.getenv("PORT")));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new Main()),"/*");
    server.start();
    server.join();
  }

  private static class ProxyAuthenticator extends Authenticator {
    private final PasswordAuthentication passwordAuthentication;

    private ProxyAuthenticator(String user, String password) {
      passwordAuthentication = new PasswordAuthentication(user, password.toCharArray());
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return passwordAuthentication;
    }
  }
}
