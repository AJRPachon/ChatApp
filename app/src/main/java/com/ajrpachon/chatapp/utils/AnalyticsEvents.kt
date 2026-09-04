package com.ajrpachon.chatapp.utils

/**
 * Canonical event/param names logged via [com.ajrpachon.chatapp.domain.repository.AnalyticsTracker].
 * Centralized to avoid magic strings scattered across use cases/repositories and to keep naming
 * consistent in the Firebase Analytics dashboard.
 *
 * Deliberately does NOT log message content, conversation ids or user-identifying values — only
 * event/type metadata. Instrumented at each feature's single "operation succeeded" chokepoint, not
 * from the UI layer, so every code path (including background/service-triggered sends) is covered.
 */
object AnalyticsEvents {
    // Screen views — names match Firebase Analytics' reserved screen_view event/params so it
    // populates the built-in "Screens" report automatically (see MainActivity).
    const val SCREEN_VIEW = "screen_view"
    const val PARAM_SCREEN_NAME = "screen_name"
    const val PARAM_SCREEN_CLASS = "screen_class"

    // Auth
    const val SIGN_UP = "sign_up"
    const val LOGIN = "login"
    const val LOGOUT = "logout"
    const val PARAM_METHOD = "method"
    const val METHOD_EMAIL = "email"
    const val METHOD_GOOGLE = "google"

    // Messaging
    const val MESSAGE_SENT = "message_sent"
    const val PARAM_MESSAGE_TYPE = "message_type"
    const val TYPE_TEXT = "text"
    const val TYPE_IMAGE = "image"
    const val TYPE_VIDEO = "video"
    const val TYPE_AUDIO = "audio"
    const val TYPE_GIF = "gif"
    const val TYPE_STICKER = "sticker"
    const val TYPE_FILE = "file"

    // Calls — logged from CallViewModel itself (not the chat message layer) so both call
    // directions (outgoing/incoming) and both call kinds (1:1/group) are covered symmetrically,
    // once per device in the call.
    const val CALL_STARTED = "call_started"
    const val CALL_ENDED = "call_ended"
    const val PARAM_CALL_TYPE = "call_type"
    const val PARAM_CALL_STATUS = "call_status"
    const val PARAM_CALL_DURATION_SECONDS = "duration_seconds"
    const val PARAM_IS_GROUP = "is_group"

    // Groups
    const val GROUP_CREATED = "group_created"
    const val PARAM_PARTICIPANT_COUNT = "participant_count"

    // Invitations
    const val INVITATION_SENT = "invitation_sent"
    const val INVITATION_ACCEPTED = "invitation_accepted"

    // Status/Stories
    const val STATUS_POSTED = "status_posted"
    const val PARAM_STATUS_TYPE = "status_type"

    // Backup
    const val BACKUP_CREATED = "backup_created"
    const val BACKUP_RESTORED = "backup_restored"

    // Search
    const val SEARCH_PERFORMED = "search_performed"
    const val PARAM_RESULT_COUNT = "result_count"

    // AI assistant
    const val AI_ASSISTANT_USED = "ai_assistant_used"
    const val PARAM_ACTION = "action"
    const val ACTION_SUMMARIZE = "summarize"
    const val ACTION_SUGGEST_REPLY = "suggest_reply"
    const val ACTION_FREEFORM = "freeform"

    // Translation
    const val TRANSLATION_USED = "translation_used"

    // Polls
    const val POLL_CREATED = "poll_created"
    const val POLL_VOTED = "poll_voted"

    // Safety
    const val CONTACT_BLOCKED = "contact_blocked"

    // App lock
    const val APP_LOCK_ENABLED = "app_lock_enabled"

    // Stickers
    const val STICKER_PACK_INSTALLED = "sticker_pack_installed"

    // Generic settings/preference toggles — one event shape instead of one event per toggle,
    // so a new setting never needs a new event name to show up in the dashboard.
    const val SETTING_CHANGED = "setting_changed"
    const val PARAM_SETTING_NAME = "setting_name"
    const val PARAM_SETTING_VALUE = "setting_value"
    const val SETTING_THEME = "theme"
    const val SETTING_CHAT_THEME = "chat_theme"
    const val SETTING_CHAT_WALLPAPER = "chat_wallpaper"
    const val SETTING_NOTIFICATION_SOUND = "notification_sound"
    const val SETTING_INCOGNITO_MODE = "incognito_mode"
    const val SETTING_MESSAGE_PIN = "message_pin"
    const val SETTING_MESSAGE_SAVE = "message_save"

    // Notifications
    const val NOTIFICATION_OPENED = "notification_opened"

    // Security / onboarding
    const val MFA_ENROLLED = "mfa_enrolled"
    const val USERNAME_SET = "username_set"
}
