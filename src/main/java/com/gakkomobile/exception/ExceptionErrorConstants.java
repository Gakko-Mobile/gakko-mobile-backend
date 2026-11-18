package com.gakkomobile.exception;

public class ExceptionErrorConstants {
    public static String VALIDATION_FAILED = "Validation failed.";
    public static String BAD_CREDENTIALS = "Invalid email or password.";
    public static String USER_NOT_FOUND = "User not found.";
    public static String ACADEMIC_GROUP_NOT_FOUND = "Group not found.";
    public static String INVALID_REFRESH_TOKEN = "Invalid refresh token.";
    public static String REVOKED_REFRESH_TOKEN = "Refresh token has been revoked.";
    public static String EMAIL_NOT_PROVIDED = "User email not provided.";
    public static String ACADEMIC_GROUP_NAME_NOT_PROVIDED = "Group name not provided.";
    public static String FORBIDDEN = "Action forbidden.";
    public static String SOMETHING_WENT_WRONG_WITH_AUTH = "Something went wrong with authentication.";
    public static String SUBJECT_NOT_FOUND = "Subject not found.";
    public static String ROOM_NOT_FOUND = "Room not found.";
    public static String GROUP_CLASS_NOT_FOUND = "Group class not found.";
    public static String ONE_TIME_CLASS_MULTIPLE_SLOTS = "A one-time class can have only one slot.";
    public static String DUPLICATE_SLOT_DATES = "Slot dates must be unique.";
    public static String CLASS_SLOT_NOT_FOUND = "Class slot not found.";
    public static String CLASS_SLOT_ALREADY_EXISTS = "The class already has a slot on this date.";
    public static String ONE_TIME_CLASS_ALREADY_HAS_SLOT = "A one-time class can have only one slot.";
    public static String SCHEDULE_END_DATE_BEFORE_START_DATE = "Schedule end date must not be before start date.";
    public static String SCHEDULE_SLOT_END_TIME_NOT_AFTER_START_TIME = "Slot end time must be after its start time.";
    public static String SCHEDULE_DATE_RANGE_TOO_LONG = "Schedule date range must not exceed %d months.";}
