package script.terminal;

import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

public class magic_painting_url extends script.base_script
{
    private static final int MENU_MAGIC_PAINTING = menu_info_types.SERVER_MENU14;
    private static final int MENU_MAGIC_PAINTING_MODE = menu_info_types.SERVER_MENU15;
    private static final int MENU_MAGIC_PAINTING_DISPLAY = menu_info_types.SERVER_MENU16;
    private static final int MENU_MAGIC_PAINTING_SCROLL_H = menu_info_types.SERVER_MENU17;
    private static final int MENU_MAGIC_PAINTING_SCROLL_V = menu_info_types.SERVER_MENU18;

    private static final String OBJVAR_TEXTURE_MODE = "texture.mode";
    private static final String OBJVAR_TEXTURE_DISPLAY_MODE = "texture.displayMode";
    private static final String OBJVAR_TEXTURE_SCROLL_H = "texture.scrollH";
    private static final String OBJVAR_TEXTURE_SCROLL_V = "texture.scrollV";

    private static final String MODE_IMAGE_ONLY = "IMAGE_ONLY";
    private static final String MODE_IMAGE_ONLY_TWO_SIDED = "IMAGE_ONLY_TWO_SIDED";
    private static final String MODE_DEFAULT = "DEFAULT";

    private static final String DISPLAY_CUBE = "CUBE";
    private static final String DISPLAY_FLAT = "FLAT";
    private static final String DISPLAY_DOUBLE_SIDED = "DOUBLE_SIDED";

    private static final String[] SCROLL_VALUES = {"0", "0.1", "0.25", "0.5", "1.0", "-0.1", "-0.25", "-0.5", "-1.0"};

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        int root = mi.addRootMenu(MENU_MAGIC_PAINTING, string_id.unlocalized("Magic Painting"));

        String mode = MODE_IMAGE_ONLY;
        if (hasObjVar(self, OBJVAR_TEXTURE_MODE))
            mode = getStringObjVar(self, OBJVAR_TEXTURE_MODE);

        String displayMode = DISPLAY_CUBE;
        if (hasObjVar(self, OBJVAR_TEXTURE_DISPLAY_MODE))
            displayMode = getStringObjVar(self, OBJVAR_TEXTURE_DISPLAY_MODE);

        String scrollH = "0";
        if (hasObjVar(self, OBJVAR_TEXTURE_SCROLL_H))
            scrollH = getStringObjVar(self, OBJVAR_TEXTURE_SCROLL_H);

        String scrollV = "0";
        if (hasObjVar(self, OBJVAR_TEXTURE_SCROLL_V))
            scrollV = getStringObjVar(self, OBJVAR_TEXTURE_SCROLL_V);

        mi.addSubMenu(root, MENU_MAGIC_PAINTING_MODE, string_id.unlocalized("Painting Mode: " + mode));
        mi.addSubMenu(root, MENU_MAGIC_PAINTING_DISPLAY, string_id.unlocalized("Display: " + displayMode));
        mi.addSubMenu(root, MENU_MAGIC_PAINTING_SCROLL_H, string_id.unlocalized("Scroll H: " + scrollH));
        mi.addSubMenu(root, MENU_MAGIC_PAINTING_SCROLL_V, string_id.unlocalized("Scroll V: " + scrollV));

        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == MENU_MAGIC_PAINTING_MODE)
        {
            String mode = MODE_IMAGE_ONLY;
            if (hasObjVar(self, OBJVAR_TEXTURE_MODE))
                mode = getStringObjVar(self, OBJVAR_TEXTURE_MODE);

            if (mode.equalsIgnoreCase(MODE_DEFAULT))
                setObjVar(self, OBJVAR_TEXTURE_MODE, MODE_IMAGE_ONLY);
            else if (mode.equalsIgnoreCase(MODE_IMAGE_ONLY))
                setObjVar(self, OBJVAR_TEXTURE_MODE, MODE_IMAGE_ONLY_TWO_SIDED);
            else
                setObjVar(self, OBJVAR_TEXTURE_MODE, MODE_DEFAULT);
        }
        else if (item == MENU_MAGIC_PAINTING_DISPLAY)
        {
            String displayMode = DISPLAY_CUBE;
            if (hasObjVar(self, OBJVAR_TEXTURE_DISPLAY_MODE))
                displayMode = getStringObjVar(self, OBJVAR_TEXTURE_DISPLAY_MODE);

            if (displayMode.equalsIgnoreCase(DISPLAY_CUBE))
                setObjVar(self, OBJVAR_TEXTURE_DISPLAY_MODE, DISPLAY_FLAT);
            else if (displayMode.equalsIgnoreCase(DISPLAY_FLAT))
                setObjVar(self, OBJVAR_TEXTURE_DISPLAY_MODE, DISPLAY_DOUBLE_SIDED);
            else
                setObjVar(self, OBJVAR_TEXTURE_DISPLAY_MODE, DISPLAY_CUBE);
        }
        else if (item == MENU_MAGIC_PAINTING_SCROLL_H)
        {
            cycleScrollValue(self, OBJVAR_TEXTURE_SCROLL_H);
        }
        else if (item == MENU_MAGIC_PAINTING_SCROLL_V)
        {
            cycleScrollValue(self, OBJVAR_TEXTURE_SCROLL_V);
        }

        return SCRIPT_CONTINUE;
    }

    private void cycleScrollValue(obj_id self, String objvarName) throws InterruptedException
    {
        String current = "0";
        if (hasObjVar(self, objvarName))
            current = getStringObjVar(self, objvarName);

        int currentIdx = -1;
        for (int i = 0; i < SCROLL_VALUES.length; i++)
        {
            if (SCROLL_VALUES[i].equals(current))
            {
                currentIdx = i;
                break;
            }
        }

        int nextIdx = (currentIdx + 1) % SCROLL_VALUES.length;
        setObjVar(self, objvarName, SCROLL_VALUES[nextIdx]);
    }
}
