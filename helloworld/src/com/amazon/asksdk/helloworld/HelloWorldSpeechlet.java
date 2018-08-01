/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package com.amazon.asksdk.helloworld;

import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.http.client.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.OutputSpeech;

/**
 * This sample shows how to create a simple speechlet for handling speechlet requests.
 */
public class HelloWorldSpeechlet implements SpeechletV2 {
    private static final Logger log = LoggerFactory.getLogger(HelloWorldSpeechlet.class);

    int count = 0;
    String meetingName = null;

    PooledHttpRequestMaker pooledHttpRequestMaker = new PooledHttpRequestMaker();

    @Override
    public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
        log.info("onSessionStarted requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
        log.info("onLaunch requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
        IntentRequest request = requestEnvelope.getRequest();
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                requestEnvelope.getSession().getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;
        log.info("onIntent intentName :"  + intentName) ;
        if ("StartMeetingIntent".equals(intentName)) {
            return getStartResponse(intent);
        }else if ("DuringMeeting".equals(intentName)) {
            return getDuringMeeting(intent);
        }else if ("EndMeetingIntent".equals(intentName)) {
            return getEndResponse(intent);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();
        } else {
            return getAskResponse("HelloWorld", "This is unsupported.  Please try something else.");
        }
    }

    @Override
    public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
        log.info("onSessionEnded requestId={}, sessionId={}", requestEnvelope.getRequest().getRequestId(),
                requestEnvelope.getSession().getSessionId());
        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse() {
        String speechText = "Welcome to the Hike Meet. You can make meeting notes with me.";
        return getAskResponse("HelloWorld", speechText);
    }

    /**
     * Creates a {@code SpeechletResponse} for the hello intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getStartResponse(Intent intent) {

        meetingName = intent.getSlot("meetingName").getValue();
        String speechText;
        if(meetingName == null || meetingName.equalsIgnoreCase("dummy")){
            speechText = "I couldn't catch it. Please repeat.";
            meetingName = null;
        }
        else {
            JsonObject map = new JsonObject();
            map.addProperty("meeting_name",meetingName);
            Integer responseCode = -1;
            try {
                responseCode = pooledHttpRequestMaker
                    .executeHttpPost("http://staging.im.hike.in/v1/alexa/start-meeting", map.toString(),
                        new JSONResponseHandler(),
                        "application/json");

                if(responseCode == 200){
                    speechText = "I've started the meeting " + meetingName + ". You can, now, start making notes for this meeting.";
                }
                else {
                    speechText = "There was some problem starting the meeting. Please try again.";
                    meetingName = null;
                }
            }
            catch (Exception e){
                log.error("problem starting the meeting", e);
                speechText = "There was some problem starting the meeting. Please try again.";
                meetingName = null;
            }
        }



        // Create the Simple card content.
        SimpleCard card = getSimpleCard("Start meeting notes", speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the hello intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     * @param intent
     */
    private SpeechletResponse getEndResponse(Intent intent) {
        String speechText;
        Integer responseCode = -1;
        try {
            responseCode = pooledHttpRequestMaker
                .executeHttpPost("http://staging.im.hike.in/v1/alexa/end-meeting", null, null, null,
                    new JSONResponseHandler(),
                    "application/json");

            if(responseCode == 200){
                speechText = "I've sent you a hike checklist for the tasks assigned. I've also mailed you the minutes of meeting";
            }
            else {
                speechText = "There was some problem starting the meeting. Please try again.";
                meetingName = null;
            }
        }
        catch (Exception e){
            log.error("problem finishing the meeting", e);
            speechText = "There was some problem finishing the meeting. Please try again.";
        }

        // Create the Simple card content.
        SimpleCard card = getSimpleCard("Meeting Finished", speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        count = 0;
        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the hello intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     * @param intent
     */
    private SpeechletResponse getDuringMeeting(Intent intent) {
        String speechText;
        if(meetingName == null || meetingName.equalsIgnoreCase("dummy") || meetingName.equalsIgnoreCase("")){
            speechText = "Please start a meeting before making notes.";
        }
        else {
            String note = intent.getSlot("task").getValue();
            log.error(note);

            Integer responseCode = -1;
            try {
                JsonObject map = new JsonObject();
                map.addProperty("note",note);
                responseCode = pooledHttpRequestMaker
                    .executeHttpPost("http://staging.im.hike.in/v1/alexa/note", map.toString(),
                        new JSONResponseHandler(),
                        "application/json");

                if(responseCode == 200){
                    count++;
                    speechText = "I've noted task " + count;
                }
                else {
                    speechText = "There was some problem noting the task. Please try again.";
                    meetingName = null;
                }
            }
            catch (Exception e){
                log.error("problem noting the task", e);
                speechText = "There was some problem noting the task. Please try again.";
            }
        }

        // Create the Simple card content.
        SimpleCard card = getSimpleCard("RESPONSE", speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "You can say hello to me!";
        return getAskResponse("HelloWorld", speechText);
    }

    /**
     * Helper method that creates a card object.
     * @param title title of the card
     * @param content body of the card
     * @return SimpleCard the display card to be sent along with the voice response.
     */
    private SimpleCard getSimpleCard(String title, String content) {
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
        card.setContent(content);

        return card;
    }

    /**
     * Helper method for retrieving an OutputSpeech object when given a string of TTS.
     * @param speechText the text that should be spoken out to the user.
     * @return an instance of SpeechOutput.
     */
    private PlainTextOutputSpeech getPlainTextOutputSpeech(String speechText) {
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return speech;
    }

    /**
     * Helper method that returns a reprompt object. This is used in Ask responses where you want
     * the user to be able to respond to your speech.
     * @param outputSpeech The OutputSpeech object that will be said once and repeated if necessary.
     * @return Reprompt instance.
     */
    private Reprompt getReprompt(OutputSpeech outputSpeech) {
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(outputSpeech);

        return reprompt;
    }

    /**
     * Helper method for retrieving an Ask response with a simple card and reprompt included.
     * @param cardTitle Title of the card that you want displayed.
     * @param speechText speech text that will be spoken to the user.
     * @return the resulting card and speech text.
     */
    private SpeechletResponse getAskResponse(String cardTitle, String speechText) {
        SimpleCard card = getSimpleCard(cardTitle, speechText);
        PlainTextOutputSpeech speech = getPlainTextOutputSpeech(speechText);
        Reprompt reprompt = getReprompt(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }
}
