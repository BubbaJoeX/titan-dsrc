    // ...existing code around line 22928...
        _dismountCreature(getLongWithNull(riderId));
    }

    // --- Tangible Object Mounting ---

    private static native boolean _mountTangibleObject(long playerId, long tangibleObjectId, float offsetX, float offsetY, float offsetZ, boolean lockOrientation);
    public static boolean mountTangibleObject(obj_id playerId, obj_id tangibleObjectId, float offsetX, float offsetY, float offsetZ, boolean lockOrientation)
    {
        return _mountTangibleObject(getLongWithNull(playerId), getLongWithNull(tangibleObjectId), offsetX, offsetY, offsetZ, lockOrientation);
    }

    // Overload with default lockOrientation=true
    public static boolean mountTangibleObject(obj_id playerId, obj_id tangibleObjectId, float offsetX, float offsetY, float offsetZ)
    {
        return _mountTangibleObject(getLongWithNull(playerId), getLongWithNull(tangibleObjectId), offsetX, offsetY, offsetZ, true);
    }

    private static native boolean _dismountTangibleObject(long playerId);
    public static boolean dismountTangibleObject(obj_id playerId)
    {
        return _dismountTangibleObject(getLongWithNull(playerId));
    }

    private static native boolean _isMountedOnTangibleObject(long playerId);
    public static boolean isMountedOnTangibleObject(obj_id playerId)
    {
        return _isMountedOnTangibleObject(getLongWithNull(playerId));
    }
    // ...existing code...
