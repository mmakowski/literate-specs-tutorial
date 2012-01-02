package com.mmakowski.tutorial.literatespecs;

import static org.apache.http.client.utils.URIUtils.createURI;
import static org.apache.http.client.utils.URLEncodedUtils.format;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.google.common.collect.ImmutableList;

/**
 * A plug-in into our authorisation system that authorises with DPC.
 */
public class DpcAuthorisation {
    private static final String OUR_SYSTEM_ID = "42";
    
    private final String dpcServiceHost;
    private final int dpcServicePort;
    
    /**
     * @param dpcServiceHost the name of the host where DPC authorisation service runs
     * @param dpcServicePort the port on which the service listens
     */
    public DpcAuthorisation(String dpcServiceHost, int dpcServicePort) {
        this.dpcServiceHost = dpcServiceHost;
        this.dpcServicePort = dpcServicePort;
    }
    
    /**
     * @param userId the id of the user who attempts the access
     * @param documentId the id of the document user tries to access
     * @return {@code true} if the authorisation has been granted
     * @throws RuntimeException if any error is encountered during authorisation
     */
    public boolean request(String userId, String documentId) {
        try {
            return parse(authorisationResponseFor(userId, documentId));
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String authorisationResponseFor(String userId, String documentId) throws ClientProtocolException, IOException, URISyntaxException {
        final HttpClient httpClient = new DefaultHttpClient();
        return httpClient.execute(requestWith(userId, documentId), new BasicResponseHandler());
    }

    private HttpUriRequest requestWith(String userId, String documentId) throws URISyntaxException {
        final List<NameValuePair> requestParams = ImmutableList.of(
                nv("systemId"  , OUR_SYSTEM_ID),
                nv("userId"    , userId),
                nv("documentId", documentId)
        );
        final String formattedParams = format(requestParams, "UTF-8");
        final URI uri = createURI("http", dpcServiceHost, dpcServicePort, "/authorise", formattedParams, null);
        return new HttpGet(uri);
    }

    private boolean parse(String response) {
        return response.trim().equals("ALLOW");
    }
    
    // reduces verbosity of request params list construction
    private static final NameValuePair nv(String name, String value) {
        return new BasicNameValuePair(name, value);
    }
}
