/*
 * @(#)ClientCommunicator.java
 *
 * Copyright 2013 openmolecules.org, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains the property
 * of openmolecules.org.  The intellectual and technical concepts contained
 * herein are proprietary to openmolecules.org.
 * Actelion Pharmaceuticals Ltd. is granted a non-exclusive, non-transferable
 * and timely unlimited usage license.
 *
 * @author Thomas Sander
 */

package org.openmolecules.comm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;

public abstract class ClientCommunicator extends CommunicationHelper {
	private static final int CONNECT_TIME_OUT = 5000;
	private static final int READ_TIME_OUT = 600000;

    private final boolean	mWithSessions;
	private boolean mUsePostMethod;
	private int mConnectTimeOut,mReadTimeOut;
	private String	mSessionID,mSessionServerURL;
	private final String mAppicationName;

	public abstract String getPrimaryServerURL();

	/**
	 * @return null or URL of fallback server in case the primary server is not available
	 */
	public abstract String getSecondaryServerURL();

	/**
	 * Whether the service received a setUseSecondaryServer() call earlier,
	 * because the primary server could not be reached.
	 * @return whether URL was switched to fallback server
	 */
	public abstract boolean isUseSecondaryServer();

	/**
	 * If the primary server is not available, then this method is called to switch
	 * to the secondary server for the rest of life of the client application.
	 */
	public abstract void setUseSecondaryServer();

	public abstract void showBusyMessage(String message);
	public abstract void showErrorMessage(String message);

	public ClientCommunicator(boolean withSessions, String applicationName) {
		mWithSessions = withSessions;
		mAppicationName = (applicationName == null) ? "unknown" : applicationName;
		mConnectTimeOut = CONNECT_TIME_OUT;
		mReadTimeOut = READ_TIME_OUT;
		mUsePostMethod = true;    // this is the default
		}

	public void setConnectTimeOut(int timeOut) {
		mConnectTimeOut = timeOut;
	}

	public void setReadTimeOut(int timeOut) {
		mReadTimeOut = timeOut;
	}

	public void setUsePostMethod(boolean usePost) {
		mUsePostMethod = usePost;
		}

	private URLConnection getConnection(String serverURL) throws IOException {
		try {
			URL urlServlet = new URI(serverURL).toURL();
			HttpURLConnection con = (HttpURLConnection)urlServlet.openConnection();

			// konfigurieren
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setConnectTimeout(mConnectTimeOut);
			con.setReadTimeout(mReadTimeOut);
			con.setRequestProperty("User-Agent", "Mozilla/5.0");
			con.setRequestProperty("Content-Type", "application/x-java-serialized-object");

			if (mWithSessions && mSessionID != null)
				con.addRequestProperty(KEY_SESSION_ID, mSessionID);

			return con;
			}
		catch (URISyntaxException use) {
			return null;
			}
        }

	private void convertToPostRequest(HttpURLConnection con, String request, String... keyValuePair) throws IOException {
		StringBuilder postData = new StringBuilder();

		postData.append(URLEncoder.encode(KEY_REQUEST, StandardCharsets.UTF_8));
		postData.append('=');
		postData.append(URLEncoder.encode(request, StandardCharsets.UTF_8));

		postData.append('&');
		postData.append(URLEncoder.encode(KEY_APP_NAME, StandardCharsets.UTF_8));
		postData.append('=');
		postData.append(URLEncoder.encode(mAppicationName, StandardCharsets.UTF_8));

		if (keyValuePair != null) {
			for (int i=0; i<keyValuePair.length; i+=2) {
				postData.append('&');
				postData.append(URLEncoder.encode(keyValuePair[i], StandardCharsets.UTF_8));
				postData.append('=');
				postData.append(URLEncoder.encode(String.valueOf(keyValuePair[i+1]), StandardCharsets.UTF_8));
				}
			}

		byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);

		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		con.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));

		con.getOutputStream().write(postDataBytes); // does make the connection
		}

    private String getResponse(URLConnection con) throws IOException {
        final int BUFFER_SIZE = 1024;
        StringBuilder sb = new StringBuilder();
        BufferedInputStream is = new BufferedInputStream(con.getInputStream());
        byte[] buf = new byte[BUFFER_SIZE];
        while (true) {
            int size = is.read(buf, 0, BUFFER_SIZE);
            if (size == -1)
                break;

            sb.append(new String(buf, 0, size));
            }

        return !sb.isEmpty() ? sb.toString() : null;
        }

    public void closeConnection() {
		if (mSessionID != null) {
			showBusyMessage("Closing Communication Channel ...");

	        try {
	            URLConnection con = getConnection(mSessionServerURL);
                con.addRequestProperty(KEY_REQUEST, REQUEST_END_SESSION);

                getResponse(con);
	            }
	        catch (Exception ex) {
				showErrorMessage(ex.toString());
	            }

			mSessionID = null;

			showBusyMessage("");
			}
		}

	private void getNewSession() {
		if (mSessionID == null) {
			showBusyMessage("Opening session...");

			mSessionID = (String)getResponse(REQUEST_NEW_SESSION);
			if (mSessionID != null)
				mSessionServerURL = isUseSecondaryServer() ? getSecondaryServerURL() : getPrimaryServerURL();

			showBusyMessage("");
			}
		}

	/**
	 * Tries to get a proper response or search result from the primary server.
	 * If the primary server cannot be contacted and a secondary server exists,
	 * then the secondary server is contacted and in case of a successful completion
	 * used for further getResponse() calls. In case of connection problems or other
	 * errors a proper error message is shown through showErrorMessage().
	 * Different from getResponse() this method does not decode the original result,
	 * it just returns the body of the HTML result.
	 * @param request
	 * @param keyValuePair
	 * @return null in case of any error
	 */
	public String getPlainResponse(String request, String... keyValuePair) {
		return (String)getResponse(request, true, keyValuePair);
		}

	/**
	 * Tries to get a proper response or search result from the primary server.
	 * If the primary server cannot be contacted and a secondary server exists,
	 * then the secondary server is contacted and in case of a successful completion
	 * used for further getResponse() calls. In case of connection problems or other
	 * errors a proper error message is shown through showErrorMessage().
	 * @param request
	 * @param keyValuePair
	 * @return null in case of any error
	 */
	public Object getResponse(String request, String... keyValuePair) {
		return getResponse(request, false, keyValuePair);
		}

	private Object getResponse(String request, boolean plainResult, String... keyValuePair) {
		boolean mayUseSecondaryServer = (getSecondaryServerURL() != null && mSessionServerURL == null);

		if (!isUseSecondaryServer() || mSessionServerURL != null) {
			try {
				String url = (mSessionServerURL != null) ? mSessionServerURL : getPrimaryServerURL();
				Object response = getResponseWithURL(url, request, plainResult, keyValuePair);
				if (response != null)
					return response;
				}
			catch (ServerErrorException see) {  // server reached, but could not satisfy request
				reportException(see);
				showErrorMessage(see.getMessage());
				return null;
				}
			catch (ConnectException ce) {  // connection refused
				reportException(ce);
				if (!mayUseSecondaryServer) {
					showErrorMessage(ce.toString());
					return null;
					}
				showBusyMessage("Connection refused. Trying alternative server...");
				}
			catch (SocketTimeoutException ste) {  // timed out
				reportException(ste);
				if (!mayUseSecondaryServer) {
					showErrorMessage(ste.toString());
					return null;
					}
				showBusyMessage("Connection timed out. Trying alternative server...");
				}
			catch (IOException ioe) {
				reportException(ioe);
				showErrorMessage(ioe.toString());
				return null;
				}
			}

		if (mayUseSecondaryServer) {
			try {
				Object response = getResponseWithURL(getSecondaryServerURL(), request, plainResult, keyValuePair);
				if (response != null) {
					setUseSecondaryServer();
					return response;
					}
				showErrorMessage("No response from neither primary nor fail-over server.");
				return null;
				}
			catch (IOException ioe) {
				showErrorMessage(ioe.toString());
				return null;
				}
			}

		showErrorMessage("No response from server.");
		return null;
		}

	/**
	 * Override this, if you need information about exceptions happening in the getResponse() method
	 */
	public void reportException(Exception e) {}

	private Object getResponseWithURL(String serverURL, String request, boolean plainResponse, String... keyValuePair) throws IOException {
		if (mWithSessions && mSessionID == null) {
			getNewSession();
			if (mSessionID == null)
				return null;
			}

		showBusyMessage("Requesting data ...");
        URLConnection con = getConnection(serverURL);

		if (mUsePostMethod) {
			// The default is a GET request, which is limited on Apache to 8700 characters.
			// As long as we use Apache as entry door to distribute our requests to virtual
			// servers, this may be a problem.
			convertToPostRequest((HttpURLConnection)con, request, keyValuePair);
			}
		else {
			con.addRequestProperty(KEY_REQUEST, request);
			con.addRequestProperty(KEY_APP_NAME, mAppicationName);
			for (int i = 0; i<keyValuePair.length; i += 2)
				con.addRequestProperty(keyValuePair[i], keyValuePair[i + 1]);
			}

        String response = getResponse(con);

        if (BODY_ERROR_INVALID_SESSION.equals(response))
			mSessionID = null;

		showBusyMessage("");

		if (response == null)
        	return null;
		else if (plainResponse)
			return response;
		else if (response.startsWith(BODY_MESSAGE))
			return response.substring(BODY_MESSAGE.length() + 1);
        else if (response.startsWith(BODY_OBJECT))
	        return decode(response.substring(BODY_OBJECT.length() + 1));
		else if (response.startsWith("SERVER_MESSAGE"))    // rest server prefix, e.g. hyperspace server
			return response.substring("SERVER_MESSAGE".length() + 1);
		else if (response.startsWith(BODY_ERROR))
			throw new ServerErrorException(response);
        else
	        throw new ServerErrorException("Unexpected response:" + (response.length()<40 ? response : response.substring(0, 40) + "..."));
		}
	}