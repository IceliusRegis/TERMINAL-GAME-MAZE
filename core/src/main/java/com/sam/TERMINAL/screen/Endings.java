package com.sam.TERMINAL.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

/**
 * Central place for ending titles, colors, and log lines. All three endings can share the same
 * rolling-credits UI by calling one credits screen and passing {@link Kind}; only the heading
 * block needs to differ.
 */
public final class Endings {

    public enum Kind {
        NEUTRAL,
        GOOD,
        BAD
    }

    private Endings() {
    }

    public static Kind fromMenuKey(String endingType) {
        if ("bad".equalsIgnoreCase(endingType))
            return Kind.BAD;
        if ("good".equalsIgnoreCase(endingType))
            return Kind.GOOD;
        return Kind.NEUTRAL;
    }

    public static String titleText(Kind kind) {
        switch (kind) {
            case BAD:
                return "BAD END\nThe Curse Continues";
            case GOOD:
                return "GOOD END\nSouls At Rest";
            default:
                return "NEUTRAL END\nYou Left Them Behind";
        }
    }

    public static Color titleColor(Kind kind) {
        switch (kind) {
            case BAD:
                return Color.RED;
            case GOOD:
                return Color.GREEN;
            default:
                return Color.WHITE;
        }
    }

    public static void logEnding(Kind kind) {
        switch (kind) {
            case BAD:
                Gdx.app.log("TERMINAL", "[BAD END] You destroyed the spirit. Another will take its place.");
                break;
            case GOOD:
                Gdx.app.log("TERMINAL", "[GOOD END] You showed mercy. The ghost is finally at peace.");
                break;
            default:
                Gdx.app.log("TERMINAL", "[NEUTRAL END] You escaped, but the ghost remains trapped forever.");
                break;
        }
    }

    /** Shared rolling credits body (neutral / bad / good use the same script). */
    public static String rollingCreditsText() {
        return "TERMINAL\n"
                + "April 2026\n"
                + "A Game by Team Gateway\n"
                + "Development Team\n"
                + "Lead Developer Samuel Rei Santiago\n"
                + "Lead Artist Sophia Mae Aguila\n"
                + "Co-developer Abygael Canaynay\n"
                + "Co-developer Christen Nicole Garcia\n\n"
                + "Fonts\n"
                + "Nimble Beasts Collective (itch.io)\n"
                + "Caffinate (itch.io)\n\n"
                + "User Interface Assets\n"
                + "2bitcrook (itch.io)\n"
                + "Sr. Toasty (itch.io)\n"
                + "JennPixel (itch.io)\n"
                + "Chris Perich (itch.io)\n"
                + "ELV Games (itch.io)\n"
                + "Pavel Sevryukov (itch.io)\n\n"
                + "Sound Effects\n"
                + "bedsideseraphim (itch.io)\n"
                + "Alterier Magicae (itch.io)\n\n"
                + "Music\n"
                + "Josh James Lim (itch.io)\n"
                + "Raudokyubu (itch.io)\n\n"
                + "Softwares\n"
                + "Sora's Pixel Converter by Ameniwa (X/Twitter)\n"
                + "Pixel it by giventofly (Github)\n\n"
                + "Special thanks to all of our blockmates for being supportive of development.\n"
                + "To our lovely furbabies for giving us the energy to push through this project.\n"
                + "To Mobile Legends:Bang Bang for giving us enough leisure to balance with the work done.\n"
                + "And to you, player, for taking your time to complete this game.";
    }
}
