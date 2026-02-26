package script.terminal;

import script.menu_info;
import script.menu_info_types;
import script.obj_id;
import script.string_id;

public class magic_painting_url extends script.base_script
{
    private static final int MENU_MAGIC_PAINTING = menu_info_types.SERVER_MENU14;
    private static final int MENU_MAGIC_PAINTING_MODE = menu_info_types.SERVER_MENU15;
    private static final String OBJVAR_TEXTURE_MODE = "texture.mode";
    private static final String MODE_IMAGE_ONLY = "IMAGE_ONLY";
    private static final String MODE_IMAGE_ONLY_TWO_SIDED = "IMAGE_ONLY_TWO_SIDED";
    private static final String MODE_DEFAULT = "DEFAULT";

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        int root = mi.addRootMenu(MENU_MAGIC_PAINTING, string_id.unlocalized("Magic Painting"));

        String mode = MODE_IMAGE_ONLY;
        if (hasObjVar(self, OBJVAR_TEXTURE_MODE))
        {
            mode = getStringObjVar(self, OBJVAR_TEXTURE_MODE);
        }

        mi.addSubMenu(root, MENU_MAGIC_PAINTING_MODE, string_id.unlocalized("Painting Mode: " + mode));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item == MENU_MAGIC_PAINTING_MODE)
        {
            String mode = MODE_IMAGE_ONLY;
            if (hasObjVar(self, OBJVAR_TEXTURE_MODE))
            {
                mode = getStringObjVar(self, OBJVAR_TEXTURE_MODE);
            }

            if (mode.equalsIgnoreCase(MODE_DEFAULT))
            {
                setObjVar(self, OBJVAR_TEXTURE_MODE, MODE_IMAGE_ONLY);
            }
            else if (mode.equalsIgnoreCase(MODE_IMAGE_ONLY))
            {
                setObjVar(self, OBJVAR_TEXTURE_MODE, MODE_IMAGE_ONLY_TWO_SIDED);
            }
            else
            {
                setObjVar(self, OBJVAR_TEXTURE_MODE, MODE_DEFAULT);
            }
        }

        return SCRIPT_CONTINUE;
    }
}
