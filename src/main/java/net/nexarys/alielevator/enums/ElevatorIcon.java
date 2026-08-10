package net.nexarys.alielevator.enums;

public enum ElevatorIcon {
    CIRCLE,
    DIAMOND,
    CLOUD,
    FIRE,
    HEART,
    KEY,
    LEAF,
    BELL,
    BOLT,
    CAT,
    CROSS,
    CROWN,
    EYE,
    FLOWER,
    HOURGLASS,
    INFINITY,
    MOON,
    MUSIC,
    SNOWFLAKE,
    STAR,
    TRIANGLE,
    COMPASS;

    private String fileName;

    public String getFileName() {
        if (fileName != null) return fileName;
        String name = name();
        return fileName = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }

    public static ElevatorIcon fromName(String icon) {
        try {
            return ElevatorIcon.valueOf(icon.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ElevatorIcon.HEART;
        }
    }
}
