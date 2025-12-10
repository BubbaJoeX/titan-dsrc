package script.library;

/**
 * @Origin: dsrc.script.library
 * @Author: BubbaJoeX
 * @Purpose: Dynamic pricing system for static_items with caching and external polling support
 * @Requirements: datatables/item/dynamic_pricing.iff (optional, for default prices)
 * @Notes: Prices are cached in script vars on the planet object. External process updates via objvars.
 * @Created: Tuesday, 12/9/2025
 * @Copyright: SWG: Titan 2025
 */

import script.*;
import script.library.utils;

public class dynamic_pricing extends script.base_script
{
    // ========================================================================
    // CONSTANTS
    // ========================================================================

    public static final String LOG_CHANNEL = "ethereal";

    // External endpoint (for documentation - actual HTTP calls done externally)
    public static final String ENDPOINT = "https://swgtitan.org/price_check.php";

    // Datatable for default/fallback prices
    public static final String PRICE_TABLE = "datatables/item/dynamic_pricing.iff";
    public static final String PRICE_TABLE_COL_ITEM = "item_name";
    public static final String PRICE_TABLE_COL_PRICE = "base_price";
    public static final String PRICE_TABLE_COL_MIN = "min_price";
    public static final String PRICE_TABLE_COL_MAX = "max_price";

    // Objvar paths for price caching (stored on planet object)
    public static final String VAR_PRICE_BASE = "dynamic_pricing";
    public static final String VAR_PRICE_CACHE = VAR_PRICE_BASE + ".cache.";
    public static final String VAR_PRICE_TIMESTAMP = VAR_PRICE_BASE + ".timestamp.";
    public static final String VAR_PRICE_QUEUE = VAR_PRICE_BASE + ".queue";
    public static final String VAR_LAST_POLL = VAR_PRICE_BASE + ".lastPoll";

    // Script var paths for in-memory caching
    public static final String SCRIPTVAR_PRICE_CACHE = "dynamicPricing.cache.";
    public static final String SCRIPTVAR_CACHE_TIME = "dynamicPricing.cacheTime.";

    // Cache settings
    public static final int DEFAULT_PRICE = 999999;
    public static final int CACHE_DURATION_SECONDS = 3600; // 1 hour
    public static final int MIN_POLL_INTERVAL = 300; // 5 minutes between polls

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public dynamic_pricing()
    {
    }

    // ========================================================================
    // MAIN API
    // ========================================================================

    /**
     * Get the current price for a static item.
     * Checks in order: script var cache -> objvar cache -> datatable -> default
     *
     * @param staticItemName The name of the static item (from master_item table)
     * @return The price in credits
     */
    public static int getPrice(String staticItemName) throws InterruptedException
    {
        if (staticItemName == null || staticItemName.length() == 0)
        {
            return DEFAULT_PRICE;
        }

        // Normalize item name
        String itemKey = normalizeItemName(staticItemName);

        // Check in-memory script var cache first (fastest)
        int cachedPrice = getScriptVarCachedPrice(itemKey);
        if (cachedPrice > 0)
        {
            return cachedPrice;
        }

        // Check objvar cache on planet object
        int objvarPrice = getObjVarCachedPrice(itemKey);
        if (objvarPrice > 0)
        {
            // Store in script var cache for faster subsequent lookups
            setScriptVarCachedPrice(itemKey, objvarPrice);
            return objvarPrice;
        }

        // Check datatable for default price
        int datatablePrice = getDatatablePrice(itemKey);
        if (datatablePrice > 0)
        {
            // Cache for future lookups
            setScriptVarCachedPrice(itemKey, datatablePrice);
            return datatablePrice;
        }

        // Queue for external price lookup
        queuePriceLookup(itemKey);

        return DEFAULT_PRICE;
    }

    /**
     * Get the price with min/max bounds from datatable.
     *
     * @param staticItemName The name of the static item
     * @return Array of [price, minPrice, maxPrice] or null if not found
     */
    public static int[] getPriceWithBounds(String staticItemName) throws InterruptedException
    {
        if (staticItemName == null || staticItemName.length() == 0)
        {
            return null;
        }

        String itemKey = normalizeItemName(staticItemName);
        int price = getPrice(itemKey);

        // Try to get bounds from datatable
        int minPrice = 0;
        int maxPrice = Integer.MAX_VALUE;

        if (dataTableOpen(PRICE_TABLE))
        {
            int row = dataTableSearchColumnForString(itemKey, PRICE_TABLE_COL_ITEM, PRICE_TABLE);
            if (row >= 0)
            {
                minPrice = dataTableGetInt(PRICE_TABLE, row, PRICE_TABLE_COL_MIN);
                maxPrice = dataTableGetInt(PRICE_TABLE, row, PRICE_TABLE_COL_MAX);
            }
        }

        return new int[] { price, minPrice, maxPrice };
    }

    /**
     * Set a price for an item (called by external update process).
     *
     * @param staticItemName The name of the static item
     * @param price The new price
     */
    public static void setPrice(String staticItemName, int price) throws InterruptedException
    {
        if (staticItemName == null || staticItemName.length() == 0 || price < 0)
        {
            return;
        }

        String itemKey = normalizeItemName(staticItemName);

        // Update objvar cache
        obj_id planetObj = getPlanetObject();
        if (isValidId(planetObj))
        {
            setObjVar(planetObj, VAR_PRICE_CACHE + itemKey, price);
            setObjVar(planetObj, VAR_PRICE_TIMESTAMP + itemKey, getGameTime());
        }

        // Update script var cache
        setScriptVarCachedPrice(itemKey, price);

        LOG(LOG_CHANNEL, "[Dynamic Pricing]: setPrice: " + itemKey + " = " + price);
    }

    /**
     * Set multiple prices at once (batch update from external process).
     *
     * @param itemNames Array of item names
     * @param prices Array of corresponding prices
     */
    public static void setPrices(String[] itemNames, int[] prices) throws InterruptedException
    {
        if (itemNames == null || prices == null || itemNames.length != prices.length)
        {
            return;
        }

        for (int i = 0; i < itemNames.length; i++)
        {
            setPrice(itemNames[i], prices[i]);
        }
    }

    /**
     * Clear cached price for an item.
     */
    public static void clearPrice(String staticItemName) throws InterruptedException
    {
        if (staticItemName == null || staticItemName.length() == 0)
        {
            return;
        }

        String itemKey = normalizeItemName(staticItemName);

        // Clear objvar cache
        obj_id planetObj = getPlanetObject();
        if (isValidId(planetObj))
        {
            removeObjVar(planetObj, VAR_PRICE_CACHE + itemKey);
            removeObjVar(planetObj, VAR_PRICE_TIMESTAMP + itemKey);
        }

        // Clear script var cache
        clearScriptVarCachedPrice(itemKey);

        LOG(LOG_CHANNEL, "[Dynamic Pricing]: clearPrice: " + itemKey);
    }

    /**
     * Clear all cached prices.
     */
    public static void clearAllPrices() throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();
        if (isValidId(planetObj))
        {
            removeObjVar(planetObj, VAR_PRICE_BASE);
        }

        LOG(LOG_CHANNEL, "[Dynamic Pricing]: clearAllPrices: cache cleared");
    }

    // ========================================================================
    // QUEUE SYSTEM (for external polling)
    // ========================================================================

    /**
     * Queue an item for external price lookup.
     * External process reads this queue and updates prices.
     */
    public static void queuePriceLookup(String staticItemName) throws InterruptedException
    {
        if (staticItemName == null || staticItemName.length() == 0)
        {
            return;
        }

        String itemKey = normalizeItemName(staticItemName);
        obj_id planetObj = getPlanetObject();

        if (!isValidId(planetObj))
        {
            return;
        }

        // Get existing queue
        String queue = "";
        if (hasObjVar(planetObj, VAR_PRICE_QUEUE))
        {
            queue = getStringObjVar(planetObj, VAR_PRICE_QUEUE);
            if (queue == null)
            {
                queue = "";
            }
        }

        // Check if already in queue
        if (queue.contains(itemKey))
        {
            return;
        }

        // Add to queue
        if (queue.length() > 0)
        {
            queue = queue + "," + itemKey;
        }
        else
        {
            queue = itemKey;
        }

        setObjVar(planetObj, VAR_PRICE_QUEUE, queue);
        LOG(LOG_CHANNEL, "[Dynamic Pricing]: queuePriceLookup: queued " + itemKey);
    }

    /**
     * Get the current price lookup queue.
     * Called by external process to know what to look up.
     */
    public static String[] getPriceLookupQueue() throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();

        if (!isValidId(planetObj) || !hasObjVar(planetObj, VAR_PRICE_QUEUE))
        {
            return new String[0];
        }

        String queue = getStringObjVar(planetObj, VAR_PRICE_QUEUE);
        if (queue == null || queue.length() == 0)
        {
            return new String[0];
        }

        return split(queue, ',');
    }

    /**
     * Clear the price lookup queue (after external process has fetched it).
     */
    public static void clearPriceLookupQueue() throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();

        if (isValidId(planetObj))
        {
            removeObjVar(planetObj, VAR_PRICE_QUEUE);
            setObjVar(planetObj, VAR_LAST_POLL, getGameTime());
        }

        LOG(LOG_CHANNEL, "[Dynamic Pricing]: clearPriceLookupQueue: queue cleared");
    }

    // ========================================================================
    // CACHE HELPERS
    // ========================================================================

    /**
     * Get price from in-memory script var cache.
     */
    private static int getScriptVarCachedPrice(String itemKey) throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();
        if (!isValidId(planetObj))
        {
            return -1;
        }

        String cacheKey = SCRIPTVAR_PRICE_CACHE + itemKey;
        String timeKey = SCRIPTVAR_CACHE_TIME + itemKey;

        if (!utils.hasScriptVar(planetObj, cacheKey))
        {
            return -1;
        }

        // Check cache expiry
        if (utils.hasScriptVar(planetObj, timeKey))
        {
            int cacheTime = utils.getIntScriptVar(planetObj, timeKey);
            int currentTime = getGameTime();

            if (currentTime - cacheTime > CACHE_DURATION_SECONDS)
            {
                // Cache expired
                utils.removeScriptVar(planetObj, cacheKey);
                utils.removeScriptVar(planetObj, timeKey);
                return -1;
            }
        }

        return utils.getIntScriptVar(planetObj, cacheKey);
    }

    /**
     * Set price in in-memory script var cache.
     */
    private static void setScriptVarCachedPrice(String itemKey, int price) throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();
        if (!isValidId(planetObj))
        {
            return;
        }

        utils.setScriptVar(planetObj, SCRIPTVAR_PRICE_CACHE + itemKey, price);
        utils.setScriptVar(planetObj, SCRIPTVAR_CACHE_TIME + itemKey, getGameTime());
    }

    /**
     * Clear price from in-memory script var cache.
     */
    private static void clearScriptVarCachedPrice(String itemKey) throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();
        if (!isValidId(planetObj))
        {
            return;
        }

        utils.removeScriptVar(planetObj, SCRIPTVAR_PRICE_CACHE + itemKey);
        utils.removeScriptVar(planetObj, SCRIPTVAR_CACHE_TIME + itemKey);
    }

    /**
     * Get price from objvar cache on planet object.
     */
    private static int getObjVarCachedPrice(String itemKey) throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();
        if (!isValidId(planetObj))
        {
            return -1;
        }

        String cacheVar = VAR_PRICE_CACHE + itemKey;
        String timeVar = VAR_PRICE_TIMESTAMP + itemKey;

        if (!hasObjVar(planetObj, cacheVar))
        {
            return -1;
        }

        // Check cache expiry
        if (hasObjVar(planetObj, timeVar))
        {
            int cacheTime = getIntObjVar(planetObj, timeVar);
            int currentTime = getGameTime();

            if (currentTime - cacheTime > CACHE_DURATION_SECONDS)
            {
                // Cache expired, queue for refresh but return stale value
                queuePriceLookup(itemKey);
            }
        }

        return getIntObjVar(planetObj, cacheVar);
    }

    /**
     * Get price from datatable (default/fallback prices).
     */
    private static int getDatatablePrice(String itemKey) throws InterruptedException
    {
        if (!dataTableOpen(PRICE_TABLE))
        {
            return -1;
        }

        int row = dataTableSearchColumnForString(itemKey, PRICE_TABLE_COL_ITEM, PRICE_TABLE);
        if (row < 0)
        {
            return -1;
        }

        return dataTableGetInt(PRICE_TABLE, row, PRICE_TABLE_COL_PRICE);
    }

    // ========================================================================
    // UTILITY FUNCTIONS
    // ========================================================================

    /**
     * Normalize item name for consistent cache keys.
     */
    private static String normalizeItemName(String itemName) throws InterruptedException
    {
        if (itemName == null)
        {
            return "";
        }

        // Lowercase and trim
        String normalized = itemName.toLowerCase().trim();

        // Replace spaces with underscores
        normalized = normalized.replace(' ', '_');

        // Remove any problematic characters for objvar names
        normalized = normalized.replace('.', '_');
        normalized = normalized.replace('/', '_');

        return normalized;
    }

    /**
     * Get the planet object for storing cached prices.
     * Uses Tatooine as the central storage location.
     */
    private static obj_id getPlanetObject() throws InterruptedException
    {
        return getPlanetByName("tatooine");
    }

    /**
     * Check if a price is considered valid (not default/error).
     */
    public static boolean isValidPrice(int price) throws InterruptedException
    {
        return price > 0 && price < DEFAULT_PRICE;
    }

    /**
     * Get a human-readable price string with formatting.
     */
    public static String formatPrice(int price) throws InterruptedException
    {
        if (price <= 0)
        {
            return "N/A";
        }

        if (price >= 1000000)
        {
            return (price / 1000000) + "M";
        }
        else if (price >= 1000)
        {
            return (price / 1000) + "K";
        }

        return String.valueOf(price);
    }

    /**
     * Calculate a modified price based on player skills/buffs.
     *
     * @param basePrice The base price
     * @param player The player buying/selling
     * @param isBuying True if player is buying, false if selling
     * @return Modified price
     */
    public static int calculateModifiedPrice(int basePrice, obj_id player, boolean isBuying) throws InterruptedException
    {
        if (!isValidId(player) || !isPlayer(player) || basePrice <= 0)
        {
            return basePrice;
        }

        float modifier = 1.0f;

        // Add skill-based modifiers here if needed
        // Example: Traders get better prices
        // if (hasSkill(player, "class_trader_phase4_master"))
        // {
        //     modifier = isBuying ? 0.95f : 1.05f;
        // }

        int modifiedPrice = (int)(basePrice * modifier);

        // Ensure minimum price of 1
        if (modifiedPrice < 1)
        {
            modifiedPrice = 1;
        }

        return modifiedPrice;
    }

    public boolean shouldPriceIncrease(int currentPrice, int threshold) throws InterruptedException
    {
        return currentPrice < threshold;
    }

    public int increasePrice(int currentPrice, float increasePercent) throws InterruptedException
    {
        return (int)(currentPrice * (1 + increasePercent / 100));
    }

    public int decreasePrice(int currentPrice, float decreasePercent) throws InterruptedException
    {
        return (int)(currentPrice * (1 - decreasePercent / 100));
    }

    public int randomizePrice(int basePrice, float variancePercent) throws InterruptedException
    {
        float variance = basePrice * (variancePercent / 100);
        float randomOffset = (float)(Math.random() * variance * 2) - variance;
        return (int)(basePrice + randomOffset);
    }

    public boolean isPriceStale(String itemKey) throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();
        if (!isValidId(planetObj))
        {
            return true;
        }

        String timeVar = VAR_PRICE_TIMESTAMP + itemKey;

        if (!hasObjVar(planetObj, timeVar))
        {
            return true;
        }

        int cacheTime = getIntObjVar(planetObj, timeVar);
        int currentTime = getGameTime();

        return (currentTime - cacheTime > CACHE_DURATION_SECONDS);
    }

    public int kickoffExternalPriceUpdate() throws InterruptedException
    {
        obj_id planetObj = getPlanetObject();
        if (!isValidId(planetObj))
        {
            return -1;
        }

        int lastPollTime = 0;
        if (hasObjVar(planetObj, VAR_LAST_POLL))
        {
            lastPollTime = getIntObjVar(planetObj, VAR_LAST_POLL);
        }

        int currentTime = getGameTime();
        if (currentTime - lastPollTime < MIN_POLL_INTERVAL)
        {
            return MIN_POLL_INTERVAL - (currentTime - lastPollTime);
        }

        setObjVar(planetObj, VAR_LAST_POLL, currentTime);
        return 0;
    }

}
