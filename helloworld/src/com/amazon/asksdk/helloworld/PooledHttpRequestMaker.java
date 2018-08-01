package com.amazon.asksdk.helloworld;


import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PooledHttpRequestMaker {

	private static final Logger logger = LoggerFactory.getLogger(PooledHttpRequestMaker.class);

	private final PoolingHttpClientConnectionManager pooledConnectionManager;
	private final HttpClient httpclient;

	public PooledHttpRequestMaker() {
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", SSLConnectionSocketFactory.getSocketFactory())
				.build();

		this.pooledConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		//need to make these configurable
		this.pooledConnectionManager.setDefaultMaxPerRoute(50);
		this.pooledConnectionManager.setMaxTotal(200);

		this.httpclient = HttpClientBuilder.create().setConnectionManager(pooledConnectionManager).build();
	}

	public final <H extends ResponseHandler<T>, T> T executeHttpPost(final String url, final String dataToBeWritten,
			final H responseHandler, final String contentType) throws NetworkCommunicationException {
		T response = null;
		try {
			final HttpPost httpPost = new HttpPost(url);
			final StringEntity stringEntity = new StringEntity(dataToBeWritten);
			stringEntity.setContentType(new BasicHeader("Content-Type", contentType));
			httpPost.setEntity(stringEntity);

			response = this.httpclient.execute(httpPost, responseHandler);

		} catch (final ClientProtocolException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		} catch (final IOException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		}

		return response;
	}
	

	public <H extends ResponseHandler<T>, T> T executeHttpPost(final String url, final String dataToBeWritten, Map<String, String> headers,
			final H responseHandler, final String contentType) throws NetworkCommunicationException{
		T response = null;
		try {
			final HttpPost httpPost = new HttpPost(url);
			final StringEntity stringEntity = new StringEntity(dataToBeWritten);
			stringEntity.setContentType(new BasicHeader("Content-Type", contentType));
			httpPost.setEntity(stringEntity);

			if(headers != null && headers.size() > 0){
				for(Entry<String, String> entry : headers.entrySet()){
					httpPost.setHeader(entry.getKey(), entry.getValue());
				}
			}

			response = this.httpclient.execute(httpPost, responseHandler);

		} catch (final ClientProtocolException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		} catch (final IOException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		}
		return response;
	}

	public final <H extends ResponseHandler<T>, T> T executeHttpGet(final String url, final Map<String, String> requestParameters,
																	final Map<String, String> headerParameters, final H responseHandler) throws NetworkCommunicationException {
		T response = null;

		try {
			String requestUrl = url;
			if(requestParameters.size() > 0){
				requestUrl += "?";
			}

			for (final Entry<String, String> entry : requestParameters.entrySet()) {
				requestUrl += URLEncoder.encode(entry.getKey(), "UTF-8") + "=" + URLEncoder.encode(entry.getValue(), "UTF-8") + "&";
			}

			if(requestUrl.charAt(requestUrl.length()-1) == '&'){
				requestUrl = requestUrl.substring(0, requestUrl.length()-1);
			}

			final HttpGet httpGet = new HttpGet(requestUrl);

			for (final String headerKey : headerParameters.keySet()) {
				httpGet.setHeader(headerKey, headerParameters.get(headerKey));
			}
			response = this.httpclient.execute(httpGet, responseHandler);

		} catch (final ClientProtocolException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		} catch (final IOException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		}

		return response;
	}

	public final <H extends ResponseHandler<T>, T> T executeHttpGet(final String url, final Map<String, String> headerParameters,
																	final H responseHandler) throws NetworkCommunicationException {
		T response = null;

		try {
			String requestUrl = url;
			final HttpGet httpGet = new HttpGet(requestUrl);

			for (final String headerKey : headerParameters.keySet()) {
				httpGet.setHeader(headerKey, headerParameters.get(headerKey));
			}
			response = this.httpclient.execute(httpGet, responseHandler);

		} catch (final ClientProtocolException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		} catch (final IOException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		}

		return response;
	}

	public <H extends ResponseHandler<T>, T> T executeHttpPost(final String url, Map<String,String> requestParameters, Map<String,String> queryParameters, Map<String, String> headers,
															   final H responseHandler, final String contentType) throws NetworkCommunicationException{
		T response = null;
		try {
			List<NameValuePair> postParameters = new LinkedList<>();

			if (requestParameters != null && requestParameters.size() > 0) {
				for (final Entry<String, String> entry : requestParameters.entrySet()) {
					postParameters.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
				}
			}

			URIBuilder uriBuilder = new URIBuilder(url);
			if (queryParameters != null && queryParameters.size() > 0){
				queryParameters.forEach(uriBuilder::setParameter);
			}


			HttpPost httpPost = new HttpPost(uriBuilder.build());
			httpPost.setEntity(new UrlEncodedFormEntity(postParameters, StandardCharsets.UTF_8));
			httpPost.setHeader("Content-type", contentType);


			if(headers != null && headers.size() > 0){
				for(Entry<String, String> entry : headers.entrySet()){
					httpPost.setHeader(entry.getKey(), entry.getValue());
				}
			}

			response = this.httpclient.execute(httpPost, responseHandler);

		} catch (URISyntaxException e) {
			logger.error("URISyntaxException during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		} catch (final ClientProtocolException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		} catch (final IOException e) {
			logger.error("Exception during http post request : " + e.getMessage(), e);
			throw new NetworkCommunicationException(e);
		}

		return response;
	}
}
