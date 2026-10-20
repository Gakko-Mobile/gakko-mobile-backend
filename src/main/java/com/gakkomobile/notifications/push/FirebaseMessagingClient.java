package com.gakkomobile.notifications.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Component;

@Component
class FirebaseMessagingClient  {

    public String send(Message message) throws FirebaseMessagingException {
        return FirebaseMessaging.getInstance().send(message);
    }
}
