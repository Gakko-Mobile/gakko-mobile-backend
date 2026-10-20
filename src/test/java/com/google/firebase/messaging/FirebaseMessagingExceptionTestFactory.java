package com.google.firebase.messaging;

import com.google.firebase.ErrorCode;
import com.google.firebase.FirebaseException;

public class FirebaseMessagingExceptionTestFactory {

    public static FirebaseMessagingException createUnregisteredException() {

        FirebaseException baseException = new FirebaseException(
                ErrorCode.NOT_FOUND,
                "Requested entity was not found.",
                null
        );
        return FirebaseMessagingException.withMessagingErrorCode(
                baseException,
                MessagingErrorCode.UNREGISTERED
        );
    }

    public static FirebaseMessagingException createGenericException() {

        FirebaseException baseException = new FirebaseException(
                ErrorCode.INTERNAL,
                "Internal server error",
                null
        );
        return FirebaseMessagingException.withMessagingErrorCode(
                baseException,
                MessagingErrorCode.INTERNAL
        );
    }
}
