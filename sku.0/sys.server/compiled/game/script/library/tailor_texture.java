package script.library;

import script.obj_id;

public class tailor_texture extends script.base_script
{
    public static final String OBJ_TEXTURE_URL = "texture.url";
    public static final String OBJ_TEXTURE_MODE = "texture.mode";
    public static final String MODE_TAILOR_PNG = "TAILOR_PNG";

    public static boolean isArmorTextureCandidate(obj_id item) throws InterruptedException
    {
        if (!isIdValid(item) || !exists(item))
        {
            return false;
        }
        String t = getTemplateName(item);
        return t != null && t.startsWith("object/tangible/wearables/");
    }

    public static boolean hasTailorPngTexture(obj_id item) throws InterruptedException
    {
        if (!hasObjVar(item, OBJ_TEXTURE_MODE) || !hasObjVar(item, OBJ_TEXTURE_URL))
        {
            return false;
        }
        String mode = getStringObjVar(item, OBJ_TEXTURE_MODE);
        if (mode == null || !mode.equalsIgnoreCase(MODE_TAILOR_PNG))
        {
            return false;
        }
        String url = getStringObjVar(item, OBJ_TEXTURE_URL);
        return url != null && url.length() > 0;
    }

    public static String validatePngUrl(String raw) throws InterruptedException
    {
        if (raw == null)
        {
            return null;
        }
        String u = raw.trim();
        if (u.length() < 12)
        {
            return null;
        }
        String low = u.toLowerCase();
        if (!low.startsWith("http://") && !low.startsWith("https://"))
        {
            return null;
        }
        int q = u.indexOf('?');
        String pathPart = q >= 0 ? u.substring(0, q) : u;
        if (!pathPart.toLowerCase().endsWith(".png"))
        {
            return null;
        }
        return u;
    }

    public static void applyTailorPngUrl(obj_id armor, String url) throws InterruptedException
    {
        setObjVar(armor, OBJ_TEXTURE_URL, url);
        setObjVar(armor, OBJ_TEXTURE_MODE, MODE_TAILOR_PNG);
        setObjVar(armor, "texture.displayMode", "CUBE");
        setObjVar(armor, "texture.scrollH", "0");
        setObjVar(armor, "texture.scrollV", "0");
    }

    public static void clearTailorPngUrl(obj_id armor) throws InterruptedException
    {
        if (hasObjVar(armor, OBJ_TEXTURE_URL))
        {
            removeObjVar(armor, OBJ_TEXTURE_URL);
        }
        if (hasObjVar(armor, OBJ_TEXTURE_MODE))
        {
            removeObjVar(armor, OBJ_TEXTURE_MODE);
        }
        if (hasObjVar(armor, "texture.displayMode"))
        {
            removeObjVar(armor, "texture.displayMode");
        }
        if (hasObjVar(armor, "texture.scrollH"))
        {
            removeObjVar(armor, "texture.scrollH");
        }
        if (hasObjVar(armor, "texture.scrollV"))
        {
            removeObjVar(armor, "texture.scrollV");
        }
    }
}
