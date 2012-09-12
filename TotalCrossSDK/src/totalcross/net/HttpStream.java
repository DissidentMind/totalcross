/*********************************************************************************
 *  TotalCross Software Development Kit                                          *
 *  Copyright (C) 2003-2004 Pierre G. Richard                                    *
 *  Copyright (C) 2003-2012 SuperWaba Ltda.                                      *
 *  All Rights Reserved                                                          *
 *                                                                               *
 *  This library and virtual machine is distributed in the hope that it will     *
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of    *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.                         *
 *                                                                               *
 *  This file is covered by the GNU LESSER GENERAL PUBLIC LICENSE VERSION 3.0    *
 *  A copy of this license is located in file license.txt at the root of this    *
 *  SDK or can be downloaded here:                                               *
 *  http://www.gnu.org/licenses/lgpl-3.0.txt                                     *
 *                                                                               *
 *********************************************************************************/

package totalcross.net;

import totalcross.io.*;
import totalcross.net.mail.*;
import totalcross.net.ssl.SSLSocket;
import totalcross.sys.*;
import totalcross.ui.image.Image;
import totalcross.ui.image.ImageException;
import totalcross.util.Hashtable;

/**
 * A HttpStream HAS-A totalcross.net.Socket and takes care of exchange protocol. It starts reading (in a buffer) at the
 * message-body. If you are having read problems, try to increase the timeout with Options.readtimeout.
 *
 * <p>
 * Here is an example showing data being read from records in a PDBFile:
 *
 * <pre>
 * public boolean verifyLogin()
 * {
 *    String url = &quot;http://www.totalcross.com.br/tests/legalizePalm.jsp?login=&quot; + login + &quot;&amp;passwd=&quot; + passwd;
 *    HttpStream hs = new HttpStream(new URI(url));
 *    byte[] buf = new byte[hs.contentLength];
 *    hs.readBytes(buf, 0, hs.contentLength);
 *    String str = new String(buf);
 *    return (str.equals(&quot;yes&quot;));
 * }
 * </pre>
 *
 * At this case, the page legalizePalm verifies login and passwd, returning "yes" if it's ok, and "no" otherwise.
 */

public class HttpStream extends Stream
{
   /** Set to true to print the header to the debug console. */
   public static boolean debugHeader;
   /** READ-ONLY should be one of the 20x. Used in the response. */
   public int responseCode;
   /** READ-ONLY encoding. Used in the response. */
   public String contentEncoding; // flsobral@tc110_102: Content encoding support.
   /** READ-ONLY the size of the returned data (-1 if unknown). Used in the response. */
   public int contentLength;
   /** READ-ONLY number of bytes read from the response's content. Initialized with 0 and incremented whenever the readBytes method is executed. */
   public int contentRead; // flsobral@tc110_103: May be useful when content length is not provided.
   /** READ-ONLY see the xxx_TYPE enum below. Used in the response. */
   public byte contentType;
   /** READ-ONLY HTTP Version. Used in the response. */
   public ByteString version;
   /** READ-ONLY connection status. Used in the response. */
   public String connection; // guich@570_32
   /** READ-ONLY location. Used in the response. */
   public URI location;
   /** READ-ONLY cookies. Null if response returned no cookies.
    * You can get all returned cookies by iterating in the keys returned by
    * <code>cookies.getKeys()</code>.
    * If you want to persist a session, just store the cookies sent by the server
    * and then send them back in each request you make, like this:
    * <pre>
    * public class SessionTest extends MainWindow
    * {
    *    String url = "http://localhost:8080/servlet/sessionTest";
    *    Button btnGO;
    *    Hashtable cookies;
    *    ...
    *    if (event.target == btnGO)
    *    {
    *       HttpStream.Options options = new HttpStream.Options();
    *
    *       // set cookies if they already exist
    *       if (cookies != null)
    *          options.setCookies(cookies);
    *
    *       HttpStream st = new HttpStream(new URI(url),options);
    *
    *       // Save cookies sent by server
    *       if (st.cookies != null)
    *          cookies = st.cookies;
    *    }
    *    ...
    * }
    * </pre>
    * Note that its important that you call <code>Socket.disconnect()</code>
    * before exiting your application.
    * @see Options#setCookies(Hashtable)
    */
   public Hashtable cookies; // guich@570_32
   
   /** This Hashtable contains all headers that came in the response that don't belong to the public fields.
    * @since TotalCross 1.62
    */
   public Hashtable headers = new Hashtable(10);

   /** Used in the contentType property. */
   public static final byte UNKNOWN_TYPE    = 0;
   /** Used in the contentType property. */
   public static final byte TEXT_HTML_TYPE  = 1;
   /** Used in the contentType property. */
   public static final byte IMAGE_TYPE      = 2;
   /** Used in the contentType property. */
   public static final byte MULTIPART_TYPE  = 3;

   /** Used in the httpType field
    * @see Options#httpType
    */
   public static final String GET = "GET ";
   /** Used in the httpType field
    * @see Options#httpType
    */
   public static final String POST = "POST ";
   
   /**
   * This static class is used by one the constructor methods.
   * It allows to tune the Stream to behave in variant ways.
   */
   public static class Options
   {
      /**
      * Basic support for proxy servers on HttpStream.
      * You set the variables <var>proxyAddress</var> and <var>proxyPort</var>
      * for this HttpStream instance to get the data thru your proxy.
      *<p>
      * There is yet the SSL part and the "don't use proxy for
      * these address" part. I'm not interested in doing it, but a
      * good resource is:
      * http://www.innovation.ch/java/HTTPClient/advanced_info.html#proxies
      * <p>
      * I've just tested on my work proxy, using a bluetooth
      * connection, so I'm not sure if it will work with
      * other proxies.
      */
      public String proxyAddress;    // hsoares@421_63

      /**
      * Associated to the <var>proxyAddress</var>,
      * <var>proxyPort</var> must match your proxy's port.
      * Defaults to -1.
      */
      public int proxyPort = -1;          // hsoares@421_63

      /** HTTP request headers. Default header:<br>
       * User-Agent: <Setings.platform>/<Settings.deviceId><br>
       * Note that <code>host</code>, appended later, is all lower-case.
       */
      public Hashtable requestHeaders = new Hashtable(13); // flsobral@tc110_104: hash table for request headers.
      /**
       * Set this with your post commands to send a post. The final data string will be the concatenation of postPrefix+postDataSB+postData+postSuffix.
       * @see #postPrefix
       * @see #postSuffix
       * @see #postDataSB
       */
      public String postData;
      /**
       * Set this with your post commands to send a post. The final data string will be the concatenation of postPrefix+postDataSB+postData+postSuffix.
       * @see #postPrefix
       * @see #postSuffix
       * @since TotalCross 1.23
       */
      public StringBuffer postDataSB;
      /**
       * Set this with your post commands to send a post. The final data string will be the concatenation of postPrefix+postDataSB+postData+postSuffix.
       * @see #postSuffix
       * @see #postData
       * @see #postDataSB
       * @since TotalCross 1.23
       */
      public String postPrefix; // guich@tc123_39
      /**
       * Set this with your post commands to send a post. The final data string will be the concatenation of postPrefix+postDataSB+postData+postSuffix.
       * @see #postPrefix
       * @see #postData
       * @see #postDataSB
       * @since TotalCross 1.23
       */
      public String postSuffix; // guich@tc123_39

      /** The headers used in POST. These are the default:<br>
       * Content-Type: application/x-www-form-urlencoded<br>
       */
      public Hashtable postHeaders = new Hashtable(13);

      /** The read timeout. The default value is 5 seconds. */
      public int readTimeOut = 5000;

      /** The write timeout. The default value is to be the same value of readTimeOut. */
      public int writeTimeOut = -1; // guich@tc114_1

      /** The timeout to open. The default value is 25 seconds. */
      public int openTimeOut = 25000; // guich@570_32

      /** Set to false to not issue a GET, but a POST.
       * @deprecated Use httpType instead
       */
      public boolean doGet = true;

      /** Set to true to issue a post instead of a get. Note that the doGet is automatically set to false. 
         <br>Here's a sample code: <pre>
         String conn = hostUrl+"survey/RequestArticle.aspx";
         HttpStream.Options options = new HttpStream.Options();
         options.readTimeOut = 60000; // 1 minute
         options.writeTimeOut = 50000; // 50 seconds
         options.openTimeOut = 120000; // 2 minutes
         options.doPost = true;
         options.postData = "xml=<root><update login=\""+state.login+"\" password=\""+state.pass+"\" dayofsync=\""+state.dayOfSync+"\"/></root>";
         XmlReadableSocket stream = new XmlReadableSocket(new URI(conn),options);
         </pre>
         @deprecated Use httpType instead
       */
      public boolean doPost;
      
      /** Defines the http type that will be used. To issue a POST, do:
       * <pre>
       * String conn = hostUrl+"survey/RequestArticle.aspx";
       * HttpStream.Options options = new HttpStream.Options();
       * options.readTimeOut = 60000; // 1 minute
       * options.writeTimeOut = 50000; // 50 seconds
       * options.openTimeOut = 120000; // 2 minutes
       * options.httpType = HttpStream.POST;
       * options.postData = "xml=<root><update login=\""+state.login+"\" password=\""+state.pass+"\" dayofsync=\""+state.dayOfSync+"\"/></root>";
       * XmlReadableSocket stream = new XmlReadableSocket(new URI(conn),options);
       * </pre>       
       * The default type is GET.<br><br>
       * You can also define a custom type, like if you want to use restful services. In this case,
       * the header will be set to what you store in the httpType String.
       * @see #GET
       * @see #POST
       * @since TotalCross 1.23
       */ 
      public String httpType = GET;
      
      /** Number of bytes to write at once. Defaults to 1024. */
      public int writeBytesSize = 1024;

      /**
       * Socket factory used to create the underlying connection. You may replace it with a SSLSocketFactory to make a
       * HTTPS connection over a secure socket, or with your own subclass of SocketFactory.
       * 
       * @since TotalCross 1.6
       */
      public SocketFactory socketFactory = SocketFactory.getDefault();

      /** Constructs a new Options class, from where you change the behaviour of an Http connection.
       * Sets the <code>postHeaders</code> to:
       * <pre>
       * Content-Type: application/x-www-form-urlencoded
       * User-Agent: platform / deviceId
       * </pre>
       * You can override these if you want, just put the new values in the Hashtable or use
       * other methods available in this class.
       */
      public Options()
      {
         requestHeaders.put("User-Agent",Settings.platform+" / "+Settings.deviceId); // flsobral@tc110_104: this is a request header, not a post header.
         postHeaders.put("Content-Type","application/x-www-form-urlencoded");
      }

      /** Replaces the default Content-Type (<code>application/x-www-form-urlencoded</code>) by the given one. */
      public void setContentType(String newContentType)
      {
         if (newContentType != null)
            postHeaders.put("Content-Type", newContentType);
      }

      /** Sets the cookies to the ones stored in the given Hashtable. */
      public void setCookies(Hashtable cookies) // guich@570_32
      {
         postHeaders.put("Cookie",cookies.dumpKeysValues(new StringBuffer(512), "=","; ").toString());
      }

      /**
       * Base64 encodes the username and password given for basic server authentication
       * 
       * @param user
       *           The username for the server. Passing null disables authentication.
       * @param password
       *           The password for the username account on the server. Passing null disables authentication.
       */
      public void setBasicAuthentication(String user, String password)
      {
         if (user == null || password == null)
            postHeaders.remove("Authorization");
         else
            postHeaders.put("Authorization", Base64.encode((user + ":" + password).getBytes()));
      }
      
      /**
       * Encodes the given username and password in Base64 for basic proxy authorization
       * 
       * @param user
       *           the username for the proxy. Passing null disables proxy authorization.
       * @param password
       *           the password for the proxy. Passing null disables proxy authorization.
       * @since TotalCross 1.27
       */
      public void setBasicProxyAuthorization(String user, String password)
      {
         if (user == null || password == null)
            postHeaders.remove("Proxy-Authorization");
         else
            postHeaders.put("Proxy-Authorization", "Basic "+ Base64.encode((user + ":" + password).getBytes()));
      }
      
      /** Part that contains the Multipart content to be used by HttpStream */
      Part partContent;
      
      /**
       * Set the given MIME multipart to be used as the HttpStream POST data.<br>
       * This method may not be used with post data fields. These fields are ignored by the HttpStream after this method is used.
       * 
       * @param multipart the multipart to be set as POST data
       * @since TotalCross 1.25
       */
      public void setContent(Multipart multipart) //flsobral@tc125_37: created the method setContent which receives a MIME multipart to be used as the HTTP POST data.
      {
         if (partContent == null)
            partContent = new Part();
         partContent.setContent(multipart);
      }
   }

   /** This makes a sleep during the send of a file.
    * Important: when using softick, you must set this to 500(ms) or more, or softick will
    * starve to death.
    */
   public int sendSleep; // guich@568_6

   /** The delimiter used when reading data using readTokens.
    * Once the readTokens method is called, changing the delimiter field will be useless.
    * @since TotalCross 1.25
    * @see #readTokens
    */
   public char readTokensDelimiter;

   /** This value is passed to the TokenReader created when readTokens is called.
    * @since TotalCross 1.25
    * @see TokenReader#doTrim
    * @see #readTokens
    */
   public boolean readTokensDoTrim;

   private static final byte [] contentEncodingFieldName = "Content-Encoding:".getBytes(); // // flsobral@tc110_102: content encoding support.
   private static final byte [] contentTypeFieldName = "Content-Type:".getBytes();
   private static final byte [] contentLengthFieldName = "Content-Length:".getBytes();
   private static final byte [] textType = "text/".getBytes();
   private static final byte [] htmlType = "html".getBytes();
   private static final byte [] imageType = "image/".getBytes();
   private static final byte [] multipartType = "multipart/".getBytes();
   private static final byte [] boundaryAtt = "boundary=".getBytes();
   private static final byte [] connectionFieldName = "Connection:".getBytes();
   private static final byte [] cookiesFieldName = "Set-Cookie:".getBytes();
   private static final byte [] locationFieldName = "Location:".getBytes();

   private static final ByteString bsContentEncodingFieldName = new ByteString(0, contentEncodingFieldName.length, contentEncodingFieldName); // flsobral@tc110_102: content encoding support.
   private static final ByteString bsContentTypeFieldName = new ByteString(0, contentTypeFieldName.length, contentTypeFieldName);
   private static final ByteString bsContentLengthFieldName = new ByteString(0, contentLengthFieldName.length, contentLengthFieldName);
   private static final ByteString bsConnectionFieldName = new ByteString(0, connectionFieldName.length, connectionFieldName);
   private static final ByteString bsCookiesFieldName = new ByteString(0, cookiesFieldName.length, cookiesFieldName);
   private static final ByteString bsLocationFieldName = new ByteString(0, locationFieldName.length, locationFieldName);

   protected Socket socket;
   protected int ofsStart;
   protected int ofsEnd;
   protected byte[] buffer;
   protected int readPos;
   protected LineReader lr;
   protected TokenReader tr; 

   private URI uri;
   private int state;
   private int ofsCur;
   private byte[] multipartSep;

   // Warning: make it enough big to hold the request!
   private static final int BUFSIZE = 1024;

   private int writeBytesSize;

   private boolean badResponseCode; // flsobral@tc115_65: Must be an instance field, otherwise the HttpStream will always return ok.

   /** Can be overloaded by classes to late-init the data. */
   protected HttpStream()
   {
   }
   
   /**
    * Constructor for a HttpStream with the default options.
    *
    * @param uri to connect to
    * @see Options
    */
   public HttpStream(URI uri) throws totalcross.net.UnknownHostException, totalcross.io.IOException
   {
      init(uri, new Options());
   }

   /**
   * Constructor for a HttpStream with specific variant options.
   * Options must have been filled before called this constructor.
   *
   * @param uri to connect to
   * @param options the specific options for this HttpStream
   * @see Options
   */
   public HttpStream(URI uri, Options options) throws totalcross.net.UnknownHostException, totalcross.io.IOException
   {
      init(uri, options);
   }

   public int readBytes(byte buf[], int start, int count) throws totalcross.io.IOException
   {
      int bytesRead;
      if (contentLength != -1)
      {
         if (contentRead == contentLength)
            return 0;
         if (contentLength - contentRead < count)
            count = contentLength - contentRead;
      }

      if (ofsStart < ofsEnd)
      {
         int len = ofsEnd - ofsStart;
         if (count < len)
         {
            Vm.arrayCopy(buffer, ofsStart, buf, start, count);
            ofsStart += count;
            bytesRead = count;
         }
         else
         {
            Vm.arrayCopy(buffer, ofsStart, buf, start, len);
            ofsStart = ofsEnd;
            buffer = null;
            bytesRead = len;
            if (count > len)
            {
               int n = socket.readBytes(buf, start+len, count-len);
               bytesRead += (n == -1 ? 0 : n);
            }
         }
      }
      else
      {
         bytesRead = socket.readBytes(buf,start,count);
      }

      contentRead += (bytesRead == -1 ? 0 : bytesRead);
      return bytesRead;
   }

   public int writeBytes(byte buf[], int start, int count) throws totalcross.io.IOException
   {
      int startPos = start;
      int bytesLeft = count;
      int sentBytes = 0;

      while (bytesLeft > 0)
      {
         int ret = socket.writeBytes(buf, startPos, (bytesLeft >= writeBytesSize ? writeBytesSize : bytesLeft));
         sentBytes += ret;
         bytesLeft -= ret;
         startPos += ret;
         if (sendSleep > 0)
            Vm.sleep(sendSleep); // this is needed for softick
      }

      return sentBytes;
   }

   public void close() throws totalcross.io.IOException
   {
      socket.close();
   }

   /**
    * Internal method to initialize this HttpStream
    *
    * @param URI uri to connect to
    * @param options the specific options for this HttpStream
    * @throws totalcross.io.IOException
    * @throws totalcross.net.UnknownHostException
    */
   protected void init(URI uri, Options options) throws totalcross.net.UnknownHostException, totalcross.io.IOException
   {
      int port;
      String strUri;

      this.uri = uri;

      if (options.proxyAddress != null) // hsoares@421_63
      {
         port = options.proxyPort;
         strUri = options.proxyAddress;
      }
      else
      {
         port = uri.port;
         if (uri.host == null)
            throw new UnknownHostException("Please prefix the host with http:// or the correct protocol");
         strUri = uri.host.toString();
      }
      contentLength = -1;
      contentType = UNKNOWN_TYPE;
      buffer = new byte[BUFSIZE];

      if (port <= 0) port = 80;
      state = -1;

      socket = options.socketFactory.createSocket(strUri, port, options.openTimeOut);
      socket.readTimeout = options.readTimeOut;
      socket.writeTimeout = options.writeTimeOut == -1 ? options.readTimeOut : options.writeTimeOut;
      if (socket instanceof SSLSocket)
         ((SSLSocket) socket).startHandshake();
      writeBytesSize = options.writeBytesSize;
      getResponse(options);
   }

   /**
    * Initialize the HTTP dialog to get the response, and process the headers.
    *
    * @param options
    */
   private void getResponse(Options options) throws totalcross.io.IOException
   {
      boolean useProxy = options.proxyAddress != null;
      StringBuffer sb = new StringBuffer(2048);
      
      if (options.partContent != null)
      {
         options.postHeaders.remove("Content-Type");
         String header = (String) options.postHeaders.remove("Cookie");
         if (header != null)
            options.partContent.addHeader("Cookie", header);
      }
      
      if (GET.equals(options.httpType))
         {options.doGet = true; options.doPost = false;}
      else
      if (POST.equals(options.httpType))
         {options.doPost = true; options.doGet = false;}
      
      if (!options.doGet) // if user set to false, make sure doPost is true.
         options.doPost = true;

      // Header Method
      if (options.httpType != null)
         sb.append(options.httpType);
      
      String serverPath = useProxy ? uri.toString() : uri.path.toString(); //flsobral@tc126: had problems with a proxy that would only reply if we send the whole url on the post/get line.
      if (POST.equals(options.httpType) || options.doPost)
      {
         if (options.httpType == null) sb.append("POST ");
         // server path
         sb.append(serverPath);
         if (uri.query != null) // vitorhc@570_38: if the URI has also GET options, send them to the server
            sb.append("?").append(uri.query.toString());         
         options.doGet = false;
      }
      if (GET.equals(options.httpType) || options.doGet)
      {
         useProxy = options.proxyAddress != null;
         if (options.httpType == null) sb.append("GET ");
         if (useProxy)
            sb.append(serverPath);
         else
         {
            if (uri.path != null && uri.path.len > 0)
               sb.append(uri.path.toString());
            else
               sb.append("/");
            if (uri.query != null && uri.query.len > 0)
            {
               sb.append("?");
               sb.append(uri.query.toString());
            }            
         }
      }

      // absolute URI
      sb.append(" HTTP/1.0\r\n");
      // Header Host
      if (!options.requestHeaders.exists("Host")) // hsoares@421_63: only when no proxy
         options.requestHeaders.put("Host", uri.host != null ? uri.host.toString() : ""); //flsobral@tc126: Host must always be provided, empty if not available. // guich@570_32: check if its already set

      options.requestHeaders.dumpKeysValues(sb, ": ", Convert.CRLF);
      sb.append(Convert.CRLF);

      if (options.partContent == null && (POST.equals(options.httpType) || options.doPost))
      {
         int len1 = options.postPrefix == null ? 0 : options.postPrefix.length();
         int len2 = options.postDataSB == null ? 0 : options.postDataSB.length();
         int len3 = options.postData   == null ? 0 : options.postData.length();
         int len4 = options.postSuffix == null ? 0 : options.postSuffix.length();
         int len = len1+len2+len3+len4; // note: this can lead to problems if the Strings contains unicode characters, because in this case, string.length != string.getBytes.length
         if (len > 0) //luciana@570_119: avoids null pointer exception if the post has no data
            options.postHeaders.put("Content-Length", Convert.toString(len));
      }
      // get the post headers in a single string
      options.postHeaders.dumpKeysValues(sb, ": ", Convert.CRLF); //flsobral@tc126: send post headers also on GET. Temporary fix to fix proxy and authorization support for both GET and POST.
      sb.append(Convert.CRLF);

      if (options.partContent == null)
         sb.append(Convert.CRLF); // append the last line separator
      if (debugHeader)
          Vm.debug(sb.toString());
      
      if (options.partContent == null)
         writeResponseRequest(sb, options); //flsobral@tc120_17: fixed bug with HttpStream connection over BIS transport on BlackBerry.
      else
      {
         byte[] bytes = Convert.getBytes(sb);
         writeBytes(bytes, 0, bytes.length);
         try
         {
            options.partContent.writeTo(socket); //flsobral@tc125_XX: added support to set a MIME part as the POST data.
            socket.writeBytes(Convert.CRLF + "0" + Convert.CRLF + Convert.CRLF);
         }
         catch (MessagingException e)
         {
            throw new IOException(e.getMessage());
         }
      }

      ofsEnd = socket.readBytes(buffer,0,BUFSIZE);
      state = 0;
      while ((ofsCur < ofsEnd) && !readHttpHeader() && refill()) {}
      if (state != 6 && Settings.onJavaSE) // flsobral@tc115: some cleaning.
         Vm.debug("HTTP: " + getStatus()); // flsobral@tc110_95: No longer stop reading the header when a bad response code is found, so we can get the error cause.
   }

   protected void writeResponseRequest(StringBuffer sb, Options options) throws totalcross.io.IOException
   {
      byte[] bytes = Convert.getBytes(sb);
      writeBytes(bytes, 0, bytes.length);
      bytes = null;

      if (POST.equals(options.httpType) || options.doPost)
      {
         if (options.postPrefix != null)
         {
            bytes = options.postPrefix.getBytes();
            writeBytes(bytes, 0, bytes.length);
            bytes = null;
         }
         if (options.postDataSB != null)
         {
            bytes = Convert.getBytes(options.postDataSB);
            writeBytes(bytes, 0, bytes.length);
            bytes = null;
         }
         if (options.postData != null)
         {
            bytes = options.postData.getBytes();
            writeBytes(bytes, 0, bytes.length);
            bytes = null;
         }
         if (options.postSuffix != null)
         {
            bytes = options.postSuffix.getBytes();
            writeBytes(bytes, 0, bytes.length);
            bytes = null;
         }
      }
   }
   
   protected void writeResponseRequest4B(StringBuffer sb, Options options) throws totalcross.io.IOException
   {
      if (POST.equals(options.httpType) || options.doPost)
      {
         if (options.postPrefix != null)
            sb.append(options.postPrefix);
         if (options.postDataSB != null)
            sb.append(options.postDataSB.toString());
         if (options.postData != null)
            sb.append(options.postData);
         if (options.postSuffix != null)
            sb.append(options.postSuffix);
      }
      
      //flsobral@tc120_17
      // Here is the real difference between the regular and the BlackBerry implementation:
      // All data is written over the socket at once, no buffer is used, that's all!
      //
      // Apparently some BlackBerry devices have problems when sockets are used in a write-write-read sequence.
      // This problem is probably related with the Nagle algorithm, and seems to occur more often with socket connections over BIS.
      // It does not happen on BlackBerry Bold though, maybe it happens only on previous OS versions or only on devices that support networks slower than 3G.
      // I tried using the socket options to disable the Nagle algorithm, but (just like the keep-alive) it didn't work.
      socket.writeBytes(sb);      
   }

   /**
   * Tell if this HttpStream is functioning properly.
   *
   * @return true, if this HttpStream is functionning properly and the socket is open;
   *         false otherwise.
   */
   public boolean isOk()
   {
      return (state == 6 && socket != null); // guich@570_70: now it also depends on socket be open
   }

   /**
   * Get a human readable text to describe the current status
   * of this HttpStream
   */
   public String getStatus()
   {
      switch (state)
      {
         case -1:
            return "Open error";
         case 0:
            return "Read error";
         case 1:
            return "Unknown version: " + version;
         case 2:
            return "Bad response: " + responseCode;
         case 6:
            return (socket != null) ? "OK." : "Socket closed"; // guich@553_12: is nonsense return "Ok" if the socket is closed
         case 7:
            return "Missing multipart boundary separator.";
         default:
            return "Premature end";
      }
   }

   /**
    * Refill the buffer, assuming that ofsCur is at ofsEnd.
    */
   public final boolean refill() throws totalcross.io.IOException
   {
      if (ofsEnd == buffer.length)    // sigh.  no more room
      {
         if (ofsStart > 0)            // oh, good! tidy still possible
         {
            Vm.arrayCopy(buffer, ofsStart, buffer, 0, ofsEnd-ofsStart);
            readPos += ofsStart;
            ofsCur -= ofsStart;
            ofsStart = 0;
         }
         else                        // plenty full: extend!
         {
            byte oldBuffer[] = buffer;
            buffer = new byte[oldBuffer.length << 1];
            Vm.arrayCopy(buffer, 0, oldBuffer, 0, ofsEnd);
         }
      }
      ofsEnd = ofsCur + socket.readBytes(buffer,ofsCur,buffer.length-ofsCur);
      return (ofsEnd >= ofsCur);
   }

   /**
   * Create the image instance from this HttpStream.
   *
   * @return the resulting totalcross.ui.image.Image
   */
   public Image makeImage() throws ImageException, IOException // flsobral@tc100: throw the specific exceptions instead of the generic Exception
   {
      return new Image(this);
   }

   /**
    * Position the stream until we find the beginning of the message-body.
    * Fill-up the HTTP version and response code.
    * <P>
    * If the message-body was found, state is 6 and the message-body starts at
    * ofsCur.
    *
    * @return true if we are done (either b/c an error occurred, or the
    *         message-body was found); false if the buffer needs to be refilled;
    */
   private final boolean readHttpHeader() throws totalcross.io.IOException
   {
      while (ofsCur < ofsEnd)
      {
         byte ch = buffer[ofsCur];
         switch (state)
         {
         case 0:      // Status Line - skip leading spaces
            while (ch == 0x20)
            {
               if (++ofsCur >= ofsEnd) return false;
               ch = buffer[ofsCur];
            }
            ofsStart = ofsCur;
            state = 1;
            break;
        case 1:       // Status Line: HTTP-version
            while (ch != 0x20)
            {
               if (++ofsCur >= ofsEnd) return false;
               ch = buffer[ofsCur];
            }
            version = new ByteString(buffer, ofsStart, ofsCur-ofsStart);
            ofsStart = ++ofsCur;
            state = 2;
            break;
         case 2:     // Status Line: Status-Code
            while (ch != 0x20)
            {
               if (++ofsCur >= ofsEnd) return false;
               ch = buffer[ofsCur];
            }
            responseCode = ByteString.convertToInt(buffer, ofsStart, ofsCur);
            if ((responseCode < 200) || (responseCode >= 400))
               badResponseCode = true; // not an HTTP response, or a bad one
            ofsStart = ++ofsCur;
            state = 3;
            break;
         case 3:  // Looking for (CR)LF, ending non-fields (status line, etc)
         case 4:  // Looking for (CR)LF, ending a field
            while (ch != (byte)'\n')
            {
               if (++ofsCur >= ofsEnd) return false;
               ch = buffer[ofsCur];
            }
            if (state == 4)
            {
               setFields();
               if (contentType == MULTIPART_TYPE)
               {
                  contentType = UNKNOWN_TYPE;
                  if (!skipToNextMimePart())
                  {
                     state = 7;
                     return true;   // can't get the mime part
                  }
                  else
                  {
                     state = 3;     // take care of the (CR)LF
                     continue;
                  }
               }
            }
            state = 5;
            ofsStart = ++ofsCur;
            break;
         case 5:     // (CR)LF found.  Followed by another (CR)LF?
            ofsStart = ofsCur++; // empty the buffer
            switch (ch)
            {
               case 0x0d:
                  break;
               case 0x0a:
                  state = 6;        // Yes!  Success!
                  break;
               default:
                  state = 4;
                  break;
            }
            break;
         case 6:
            {
               if (badResponseCode)
                  state = 2;
               ofsStart = ofsCur;
               return true; // Yes!  Success!
            }
         }
      }
      return false;
   }

   /**
   * Analyse and set a few relevant fields from the response,
   */
   void setFields()
   {
      int type = -1;
      int start = ofsStart;
      int end,end1=0,start1=0;

      if ((contentType == UNKNOWN_TYPE) && bsContentTypeFieldName.equalsIgnoreCase(buffer, ofsStart, bsContentTypeFieldName.len))
      {
         type = 0;
         start += bsContentTypeFieldName.len;
      }
      else
      if ((connection == null) && bsConnectionFieldName.equalsIgnoreCase(buffer, ofsStart, bsConnectionFieldName.len))
      {
         type = 1;
         start += bsConnectionFieldName.len;
      }
      else
      if ((cookies == null) && bsCookiesFieldName.equalsIgnoreCase(buffer, ofsStart, bsCookiesFieldName.len))
      {
         type = 2;
         start += bsCookiesFieldName.len;
      }
      else
      if ((contentLength == -1) && bsContentLengthFieldName.equalsIgnoreCase(buffer, ofsStart, bsContentLengthFieldName.len))
      {
         type = 3;
         start += bsContentLengthFieldName.len;
      }
      else
      if ((location == null) && bsLocationFieldName.equalsIgnoreCase(buffer, ofsStart, bsLocationFieldName.len))
      {
         type = 4;
         start += bsLocationFieldName.len;
      }
      else
      if ((contentEncoding == null) && bsContentEncodingFieldName.equalsIgnoreCase(buffer, ofsStart, bsContentEncodingFieldName.len))
      {
         type = 5; // flsobral@tc110_102: content encoding support.
         start += bsContentEncodingFieldName.len;
      }
      else
      {
         start1 = start;
         while (buffer[start] != ':' && buffer[start] >= 32)
            start++;
         end1 = start;
         if (buffer[start] == ':') start++;
      }
      // trim the string
      byte b;
      end = ofsCur;
      if (buffer[end-1] == (byte)'\r') --end;
      while (((b=buffer[start]) == (byte)' ') || (b == (byte)'\t'))
      {
         ++start;
      }
      while ((end > start) && (((b=buffer[end-1]) == (byte)' ') || (b == (byte)'\t')))
      {
         --end;
      }
      // now set the properties
      switch (type)
      {
         case -1:
            headers.put(new String(buffer,start1,end1-start1), new String(buffer,start, end-start));
            break;
         case 0:
            contentType = getContentType(start, end-start);
            break;
         case 1:
            connection = new String(buffer,start, end-start);
            break;
         case 2:
            cookies = new Hashtable(31);
            String s = new String(buffer, start, end-start);
            String []cs = Convert.tokenizeString(s,';');
            for (int i =0; i < cs.length; i++)
            {
               String tok = cs[i].trim();
               int eq = tok.indexOf('=',0);
               if (eq == -1)
                  continue;
               String key = tok.substring(0,eq);
               String value = tok.substring(eq+1);
               cookies.put(key,value);
            }
            break;
         case 3:
            contentLength = ByteString.convertToInt(buffer, start, end);
            break;
         case 4:
            location = new URI(new String(buffer, start, end-start));
            break;
         case 5:
            contentEncoding = new String(buffer,start, end-start); // flsobral@tc110_102: content encoding support.
            break;
      }
   }

   /**
   * Translate the content type into one of the xxx_TYPE values
   *
   * @param start start of the content-type value inside this buffer
   * @param len length of the content-type value inside this buffer
   * @return one of the corresponding xxx_TYPE values
   */
   byte getContentType(int start, int len)
   {
      ByteString bsCntType = new ByteString(start, len, buffer);
      if (bsCntType.equalsAtIgnoreCase(imageType, 0))
         return IMAGE_TYPE;
      else
      if (bsCntType.equalsAtIgnoreCase(textType, 0))
      {
         if (bsCntType.equalsAtIgnoreCase(htmlType, imageType.length))
         {
            return TEXT_HTML_TYPE;
         }
      }
      else
      if (bsCntType.equalsAtIgnoreCase(multipartType, 0))
      {
         // Look for the "boundary=" attribute
         int begBnd = bsCntType.indexOf(boundaryAtt, multipartType.length);
         if (begBnd >= 0)
         {
            // get the value of this attribute
            begBnd += boundaryAtt.length;
            int endBnd = bsCntType.indexOf((byte) ';', begBnd);
            if (endBnd == -1)
               endBnd = len;
            multipartSep = new byte[3 + endBnd - begBnd];
            multipartSep[0] = (byte) '\n';
            multipartSep[1] = (byte) '-';
            multipartSep[2] = (byte) '-';
            Vm.arrayCopy(buffer, start + begBnd, multipartSep, 3, endBnd - begBnd);
         }
         return MULTIPART_TYPE;
      }
      return UNKNOWN_TYPE;
   }

   /**
    * Skip to the next mime part.
    * <ul>
    * <li>ofsCur points after the '\n' ending the content-type parameter
    * <li>multipartSep can be null if no mime separator (boundary) was found
    * </ul>
    *
    * @return true if we were able to position the stream on the '\n' that ends
    *         the mime-separator signaling the beginning of the mime part.
    * @throws totalcross.io.IOException
    *
    * Actually, we only handle the first mime part This code might later be
    * extended for other parts.
    */
   protected boolean skipToNextMimePart() throws totalcross.io.IOException
   {
      if (multipartSep == null)
         return false;
      while (true)
      {
         ByteString bs;
         int ix;
         if ((ofsCur + multipartSep.length) > ofsEnd)
         {
            ofsStart = ofsCur;  // make room
            if (!refill()) return false;
         }
         bs = new ByteString(ofsCur, ofsEnd-ofsCur, buffer);
         if ((ix = bs.indexOf(multipartSep, 0)) >= 0)
         {
            ofsCur += (ix+multipartSep.length);
            break;
         }
         ofsCur = ofsEnd - multipartSep.length - 1;
      }
      ofsStart = ofsCur;  // make room
      return true;
   }

   /**
    * Reads a line of text comming from the socket attached to this HttpStream.
    * This method correctly handles newlines with \\n or \\r\\n.
    * If you're reading tokens, use the readTokens method. 
    * Note that both readLine and readTokens cannot be used at the same time.
    *
    * @return the read line or <code>null</code> if nothing was read.
    * @since SuperWaba 5.7
    * @see #readTokens
    */
   public String readLine() throws totalcross.io.IOException
   {
      if (lr == null)
         lr = new LineReader(socket, buffer, ofsCur, ofsEnd-ofsCur);
      return lr.readLine();   
   }
   
   /**
    * Reads a line of text comming from the socket attached to this HttpStream.
    * This method correctly handles newlines with \\n or \\r\\n.
    * If you're reading lines, use the readLines method. 
    * Note that both readLine and readTokens cannot be used at the same time.
    * The delimiter must be set using the delimiter field, prior to using this method.
    *
    * @return the read line or <code>null</code> if nothing was read.
    * @since TotalCross 1.25
    * @see #readTokensDelimiter
    * @see #readTokensDoTrim
    * @see #readLine
    */
   public String[] readTokens() throws totalcross.io.IOException // guich@tc125_16
   {
      if (tr == null)
      {
         tr = new TokenReader(socket, readTokensDelimiter, buffer, ofsCur, ofsEnd-ofsCur);
         tr.doTrim = readTokensDoTrim;
      }
      return tr.readTokens();
   }
}