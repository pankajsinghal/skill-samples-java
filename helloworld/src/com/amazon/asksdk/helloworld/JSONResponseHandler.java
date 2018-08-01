package com.amazon.asksdk.helloworld;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JSONResponseHandler implements ResponseHandler<Integer> {
	private static final Logger log = LoggerFactory.getLogger(JSONResponseHandler.class);
	private Gson gson;

	public JSONResponseHandler() {
		this.gson = new GsonBuilder().setDateFormat("MM-dd-yyyy HH:mm:ss").create();
		
	}

	@Override
	public Integer handleResponse(HttpResponse response) throws ClientProtocolException, IOException {

		StatusLine statusLine = response.getStatusLine();
		int statusCode = statusLine.getStatusCode();
		final HttpEntity entity = response.getEntity();
		log.info("statusCode : " + statusCode);
		if (statusLine.getStatusCode() >= 300) {
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}

		String responseString = EntityUtils.toString(entity);
		log.info("responseString : " + responseString);
		if (responseString == null || "".equals(responseString)) {
			throw new HttpResponseException(statusLine.getStatusCode(), "received null or empty response");
		}

		return statusCode;

	}

}
