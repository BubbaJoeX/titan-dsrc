package script.space.atmo;

import script.*;
import script.library.*;

/**
 * R3 astromech tied to an {@link atmo_landing_point} egg: periodic barks + radial "Get Landing Information".
 */
public class atmo_landing_pad_droid extends script.base_script
{
    public static final String SCRIPT_NAME = "space.atmo.atmo_landing_pad_droid";

    /** Objvar on this droid: obj_id of the landing pad egg. */
    public static final String OBJVAR_PAD = "atmo.pad.landing_point";

    private static final String SCRIPTVAR_LAST_PAD_STATE = "atmo.pad_droid.lastState";

    private static final int STATE_CLEAR = 0;
    private static final int STATE_ENROUTE = 1;
    private static final int STATE_LANDED = 2;

    private static final int MENU_GET_LANDING_INFO = menu_info_types.SERVER_MENU16;

    private static final int BARK_MIN_S = 35;
    private static final int BARK_MAX_S = 85;

    private static final String PALETTE_COLOR_1 = "private/index_color_1";
    private static final String PALETTE_COLOR_2 = "private/index_color_2";

    /** HTML-style accent pairs (R,G,B) — closest palette match applied per channel. */
    private static final int[][] HTML_TONES = new int[][]
    {
        { 0x00, 0x88, 0xff },
        { 0xff, 0x44, 0x44 },
        { 0x44, 0xff, 0x88 },
        { 0xaa, 0x88, 0xff },
        { 0xff, 0xaa, 0x00 },
        { 0x00, 0xcc, 0xaa },
        { 0xff, 0x66, 0xcc },
        { 0x66, 0xcc, 0xff },
        { 0x99, 0xff, 0x33 },
        { 0xff, 0x33, 0x99 },
        { 0x33, 0x99, 0xff },
        { 0xcc, 0xcc, 0xff }
    };

    public int OnAttach(obj_id self) throws InterruptedException
    {
        setInvulnerable(self, true);
        messageTo(self, "padDroidBarkTick", null, rand(BARK_MIN_S, BARK_MAX_S), false);
        return SCRIPT_CONTINUE;
    }

    public int OnInitialize(obj_id self) throws InterruptedException
    {
        setInvulnerable(self, true);
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuRequest(obj_id self, obj_id player, menu_info mi) throws InterruptedException
    {
        mi.addRootMenu(MENU_GET_LANDING_INFO, string_id.unlocalized("Get Landing Information"));
        return SCRIPT_CONTINUE;
    }

    public int OnObjectMenuSelect(obj_id self, obj_id player, int item) throws InterruptedException
    {
        if (item != MENU_GET_LANDING_INFO)
            return SCRIPT_CONTINUE;
        faceTo(self, player);
        showLandingInformation(self, player);
        return SCRIPT_CONTINUE;
    }

    private void showLandingInformation(obj_id self, obj_id player) throws InterruptedException
    {
        obj_id pad = getObjIdObjVar(self, OBJVAR_PAD);
        if (!isIdValid(pad) || !exists(pad) || !atmo_landing_registry.isLandingPoint(pad))
        {
        chat.chat(self, player, "\\#ffaa44*beep* Landing platform data unavailable.", chat.ChatFlag_targetOnly);
        return;
        }

        String name = atmo_landing_registry.getLandingPointName(pad);
        if (name == null || name.isEmpty())
            name = "(unnamed platform)";

        StringBuilder sb = new StringBuilder();
        sb.append("\\#00ccff[Landing platform] ").append(name).append("\n");

        location loc = atmo_landing_registry.getLandingLocation(pad);
        if (loc != null)
            sb.append("\\#aaddff  Pad position: ~[").append(Math.round(loc.x)).append(", ").append(Math.round(loc.y)).append(", ").append(Math.round(loc.z)).append("]\n");

        int fee = atmo_landing_manager.resolveBaseLandingFeeCredits(pad, atmo_landing_point.MINIMUM_LANDING_FEE);
        if (atmo_landing_manager.shouldWaiveLandingFee(pad))
            sb.append("\\#aaddff  Landing fee: waived\n");
        else
            sb.append("\\#aaddff  Landing fee: ").append(fee).append(" cr (before city tax)\n");

        int dock = atmo_landing_manager.getEffectiveDockDurationSeconds(pad);
        sb.append("\\#aaddff  Mooring time: ").append(dock < 0 ? "unlimited" : (dock + " s")).append("\n");
        sb.append("\\#aaddff  Post-mooring grace: ").append(atmo_landing_manager.getDockGraceSeconds(pad)).append(" s\n");

        if (atmo_landing_registry.isLanded(pad))
            sb.append("\\#88ddaa  Status: vessel on pad\n");
        else if (atmo_landing_registry.isEnRoute(pad))
            sb.append("\\#ffaa44  Status: inbound traffic on approach\n");
        else
            sb.append("\\#88ddaa  Status: pad clear\n");

        chat.chat(self, player, sb.toString(), chat.ChatFlag_targetOnly);
    }

    public int padDroidBarkTick(obj_id self, dictionary params) throws InterruptedException
    {
        obj_id pad = getObjIdObjVar(self, OBJVAR_PAD);
        if (!isIdValid(pad) || !exists(pad))
        {
            destroyObject(self);
            return SCRIPT_CONTINUE;
        }

        int prev = utils.hasScriptVar(self, SCRIPTVAR_LAST_PAD_STATE) ? utils.getIntScriptVar(self, SCRIPTVAR_LAST_PAD_STATE) : STATE_CLEAR;
        int cur = STATE_CLEAR;
        if (atmo_landing_registry.isLanded(pad))
            cur = STATE_LANDED;
        else if (atmo_landing_registry.isEnRoute(pad))
            cur = STATE_ENROUTE;

        String name = atmo_landing_registry.getLandingPointName(pad);
        if (name == null || name.isEmpty())
            name = "this pad";

        String line;
        if (prev == STATE_LANDED && cur == STATE_CLEAR)
            line = pick(new String[]
            {
                "*whistle* Traffic clear — previous occupant has departed " + name + ".",
                "Pad open. All clear at " + name + ".",
                "Departure logged. Standing by at " + name + "."
            });
        else if (cur == STATE_ENROUTE)
            line = pick(new String[]
            {
                "*beep* Inbound signature on approach to " + name + ".",
                "Traffic alert: vessel on vector for " + name + ".",
                "Tracking incoming traffic — " + name + "."
            });
        else if (cur == STATE_LANDED)
            line = pick(new String[]
            {
                "*happy beep* Contact down — " + name + " is occupied.",
                "Pad secured. Vessel on deck at " + name + ".",
                "Landing complete at " + name + ". Mooring clock running."
            });
        else
            line = pick(new String[]
            {
                "*idle chirp* " + name + " — no traffic. Systems nominal.",
                "Scanning approaches for " + name + ". All quiet.",
                "Standing by at " + name + ". Clear skies."
            });

        chat.chat(self, line);

        utils.setScriptVar(self, SCRIPTVAR_LAST_PAD_STATE, cur);
        messageTo(self, "padDroidBarkTick", null, rand(BARK_MIN_S, BARK_MAX_S), false);
        return SCRIPT_CONTINUE;
    }

    private static String pick(String[] lines) throws InterruptedException
    {
        return lines[rand(0, lines.length - 1)];
    }

    /** Called when creating the droid from {@link atmo_landing_point}. */
    public static void applyR3AppearanceAndName(obj_id droid) throws InterruptedException
    {
        char letter = (char) ('A' + rand(0, 25));
        int digit = rand(0, 9);
        setName(droid, "R3-" + letter + digit);

        int i1 = rand(0, HTML_TONES.length - 1);
        int i2 = rand(0, HTML_TONES.length - 1);
        int[] c1 = HTML_TONES[i1];
        int[] c2 = HTML_TONES[i2];
        setPalcolorCustomVarClosestColor(droid, PALETTE_COLOR_1, c1[0], c1[1], c1[2], 255);
        setPalcolorCustomVarClosestColor(droid, PALETTE_COLOR_2, c2[0], c2[1], c2[2], 255);
    }
}
