// palcolor_custom_var.java

package script;

public class palcolor_custom_var extends ranged_int_custom_var
{
	/**
	 * Retrieve the color currently selected for this variable.
	 *
	 * The caller could also use ranged_int_custom_var.getValue(), which returns
	 * the index of the color.  The index can be used to look up the color
	 * from the color palette associated with the variable.
	 *
	 * @return  the currently-selected color for this variable instance.
	 *
	 * @see color
	 * @see getAllColors()
	 */
	public color getSelectedColor()
		{
			return base_class.getPalcolorCustomVarSelectedColor(getObjId(), getVarName());
		}

	/**
	 * Set the customization variable to the index of the palette color most closely
	 * matching the specified targetColor.
	 *
	 * @param targetColor  the desired color for the customization variable.
	 */
	public void setToClosestColor(color targetColor)
		{
			base_class.setPalcolorCustomVarClosestColor(getObjId(), getVarName(), targetColor.getR(), targetColor.getG(), targetColor.getB(), targetColor.getA());
		}

	/**
	 * Retrieve all colors available in the palette associated with this variable.
	 *
	 * The caller should not call this frequently as it is an expensive function.
	 * If the caller doesn't need to work directly with color information, it is
	 * far more efficient to select colors using the ranged_int_custom_var functions
	 * for setting and getting the color index as an integer.  The integer represents
	 * the index into the color palette.
	 *
	 * @return  an array containing one color entry per entry in the custom_var's
	 *          color palette.  The first entry in the returned array corresponds to
	 *          a palette index of 0.
	 */
	public color[] getAllColors()
		{
			return base_class.getPalcolorCustomVarColors(getObjId(), getVarName());
		}

	// ========================================================================
	// Direct Color Support (HTML hex codes and RGB values)
	// ========================================================================

	/**
	 * Set a direct RGB color, bypassing the palette.
	 * The color will be auto-matched to the closest palette entry for rendering.
	 *
	 * @param c  the color to set
	 * @return  true if the color was set successfully
	 */
	public boolean setDirectColor(color c)
		{
			return base_class.setCustomizationColor(getObjId(), getVarName(), c);
		}

	/**
	 * Set a direct color from RGB components.
	 * The color will be auto-matched to the closest palette entry for rendering.
	 *
	 * @param r  red component (0-255)
	 * @param g  green component (0-255)
	 * @param b  blue component (0-255)
	 * @return  true if the color was set successfully
	 */
	public boolean setDirectColorRGB(int r, int g, int b)
		{
			return base_class.setCustomizationColorRGB(getObjId(), getVarName(), r, g, b);
		}

	/**
	 * Set a direct color from an HTML hex color string.
	 * The color will be auto-matched to the closest palette entry for rendering.
	 *
	 * @param htmlColor  the HTML color string (e.g., "#FF5500")
	 * @return  true if the color was set successfully
	 */
	public boolean setDirectColorHtml(String htmlColor)
		{
			return base_class.setCustomizationColorHtml(getObjId(), getVarName(), htmlColor);
		}

	/**
	 * Get the direct color value.
	 * This returns the actual selected color, which may differ from the
	 * matched palette color used for rendering.
	 *
	 * @return  the direct color, or the palette color if not using direct color
	 */
	public color getDirectColor()
		{
			return base_class.getCustomizationColor(getObjId(), getVarName());
		}

	/**
	 * Retrieve a String representation of the instance suitable for
	 * a debug dump.
	 *
	 * The caller should not assume anything about the format of this
	 * output.  It may change at any time.
	 *
	 * @return  a String representation of the instance suitable for
	 *          a debug dump.
	 */
	public String toString()
		{
			return "[palcolor_custom_var: ranged_int_custom_var = " + super.toString() + "]";
		}

	/**
	 * Construct a palcolor_custom_var instance.
	 *
	 * Scripters should not try to construct these by hand.  These
	 * are created from the C++ native code.
	 *
	 * @param objId              the obj_id for the Object containing the variable.
	 * @param varName            the name of the customization variable.
	 * @param minRangeInclusive  the minimum value that this variable's value can take on.
	 * @param maxRangeExclusive  one greater than the maximum value this variable can take on.
	 */
	public palcolor_custom_var(long objId, String varName, int varTypeId, int minRangeInclusive, int maxRangeExclusive)
		{
			super(objId, varName, varTypeId, minRangeInclusive, maxRangeExclusive);
		}
	public palcolor_custom_var(obj_id objId, String varName, int varTypeId, int minRangeInclusive, int maxRangeExclusive)
		{
			super(objId, varName, varTypeId, minRangeInclusive, maxRangeExclusive);
		}
}
