package dev.arclyx0

enum class Messages(val key: String) {
    NO_PERMISSION_RELOAD("no-permission-reload"),
    RELOAD_SUCCESS("reload-success"),
    NO_PERMISSION_TELEPORT_OTHERS("no-permission-teleport-others"),
    NO_PERMISSION_TELEPORT_SELF("no-permission-teleport-self"),
    PLAYER_NOT_ONLINE("player-not-online"),
    CONSOLE_MUST_SPECIFY_PLAYER("console-must-specify-player"),
    WRONG_WORLD("wrong-world"),
    TELEPORT_SUCCESS("teleport-success"),
    TELEPORT_DEFAULT_SPAWN("teleport-default-spawn"),
    TELEPORT_FAILED("teleport-failed"),
    TELEPORT_OTHERS_SUCCESS("teleport-others-success"),
    TELEPORT_OTHERS_DEFAULT_SPAWN("teleport-others-default-spawn"),
    TELEPORT_OTHERS_FAILED("teleport-others-failed"),
    DEFAULT_WORLD_NOT_FOUND("default-world-not-found"),
    SAVE_FAILED("save-failed"),
    MISSING_DEPENDENCY("missing-dependency"),
    LOCATION_SAVED_TEMP("location-saved-temp");
}
