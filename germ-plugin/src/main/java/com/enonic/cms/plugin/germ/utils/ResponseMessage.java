package com.enonic.cms.plugin.germ.utils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by rfo on 19/03/14.
 */
public class ResponseMessage {
    public static enum MessageType{INFO, WARNING, ERROR};

    MessageType messageType;
    String message;

    public ResponseMessage(String message, MessageType messageType){
        this.message = message;
        this.messageType = messageType;
    }

    public String getMessage(){
        return this.message;
    }

    public MessageType getMessageType(){
        return this.messageType;
    }
}
