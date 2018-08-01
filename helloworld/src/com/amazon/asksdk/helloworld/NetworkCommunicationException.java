package com.amazon.asksdk.helloworld;


public class NetworkCommunicationException extends Exception{
	
	private static final long serialVersionUID = -6858423624208724972L;

	public NetworkCommunicationException(Throwable t){
		super(t);
	}
	
	public NetworkCommunicationException(String message, Throwable t){
		super(message, t);
	}

}
