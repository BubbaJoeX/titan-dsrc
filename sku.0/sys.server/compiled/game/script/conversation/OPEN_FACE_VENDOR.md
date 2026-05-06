# Open Face Vendor - Designer Guide

This document explains how to set up **NPC shelf vendors** that use **`conversation.open_face_vendor`** with **cinematic conversation**. Players browse goods as conversation lines; the **camera pans to each physical prop**, then **Buy** / **Cancel** completes the sale.

Stock is **discovered automatically**: all objects **within range** of the NPC that have **`open_face_vendor.price`** and the **same challenge key** as the NPC are listed (no manual object-id list).

Implementation:

- Conversation: `script/conversation/open_face_vendor.java`
- Helpers: `script/library/open_face_vendor_lib.java`

---

## What you build in the world

1. **Vendor NPC** - conversable creature/NPC with script **`conversation.open_face_vendor`**.
2. **Challenge key** - same non-empty string on the NPC and on every sellable prop (`open_face_vendor.vendor_key`) so another stall's priced props are never picked up.
3. **Props** - tangibles placed in the world (shelf, table, floor) **within the scan radius** (default **64 m** from the NPC).
4. **Pricing** - `open_face_vendor.price` (credits) on each prop you want listed.

Display props are **not** removed on purchase; the buyer gets a **new** inventory instance.

---

## NPC setup

### Required script

- **`conversation.open_face_vendor`**

### Required objvar

| Objvar | Type | Purpose |
|--------|------|---------|
| `open_face_vendor.vendor_key` | **string** | Unique id for **this** vendor's stock (e.g. `naboo_gungan_market_01`). Must match every prop you intend to sell here. |

If missing or empty -> "This vendor is not set up yet."

### Optional objvars (NPC)

| Objvar | Type | Purpose |
|--------|------|---------|
| `open_face_vendor.greeting` | **string** | Replaces the default opening line ("Hello! How are you?"). |
| `open_face_vendor.scan_range_m` | **float** | Scan radius in meters. Default **64**. Clamped to **0.5-512** if set outside that band (see code). |

---

## Sellable props (world objects)

### Placement

- Must be **within** `scan_range_m` (default **64 m**) of the **NPC** (spherical `getObjectsInRange`).
- They do **not** need to be in a container.

### Required objvars (each sellable object)

| Objvar | Type | Purpose |
|--------|------|---------|
| `open_face_vendor.vendor_key` | **string** | **Must equal** the NPC's key (same spelling/trim). |
| `open_face_vendor.price` | **int** | Credits; **>= 1** or the object is skipped. |

### Optional (each object)

| Objvar | Type | Purpose |
|--------|------|---------|
| `open_face_vendor.custom_description` | **string** | Inspect dialog text. If omitted, static items use **`static_item_d`** STF; others get a short generic line. |

### Item types

- **Static items** - normal placement; name/description from tables unless overridden.
- **Non-static tangibles** - purchase clones **`getTemplateName`** into inventory.

### Who is ignored in the scan

- The **vendor NPC** itself.
- **Players**.

Everything else in range that passes **key + price** can appear (including other NPCs if someone mis-tags them - avoid key collisions).

---

## Staging crate (bulk tagging)

Use **`item.container.open_face_vendor_staging`** when you want to **apply vendor key, price, and custom description** to every item inside a crate via **radial -> [OFV] Staging** and **SUI input boxes** (no per-object objvar editing). Move the crate with **god client** like any container.

### Steps

1. Spawn or place any normal **tangible container** (for example `object/tangible/container/loot/large_container.iff`).
2. Attach script **`item.container.open_face_vendor_staging`**.
3. Optional: set **`open_face_vendor.public_stamp_menu`** on the crate so **non-gods** may use the staging radial (default: **gods only**).
4. Put sellable props **inside** the crate (drop / transfer).
5. Radial -> **[OFV] Staging** -> choose **Set vendor key**, **Set price**, or **Set custom description**. Each dialog applies to **all contents** and mirrors values onto the crate (`open_face_vendor.vendor_key`, `open_face_vendor.stamp_price`, `open_face_vendor.stamp_custom_description`).
   - **Price:** empty or **0** removes `open_face_vendor.price` from contents.
   - **Description:** empty clears `open_face_vendor.custom_description` on contents.
6. Pull props out and place them **in-world** within scan range of your vendor NPC.

Implementation: `script/item/container/open_face_vendor_staging.java`.

---

## Conversation flow (player-facing)

1. **Greeting** - optional custom greeting, or default.
2. **"I would like to browse your goods."** -> vendor lists stock (re-scanned from the world).
3. **"This is what I have to choose from:"** - one button per item (name + price) + **Goodbye.**
4. Choosing an item **pans the cinematic camera** to that object -> detail + **Buy** / **Cancel**.
5. **Buy** - `money.requestPayment`, grant item, camera back to speaker -> **"Anything else?"**
6. **Cancel** - camera back to vendor -> browse list again.

---

## Limits

- **At most five** sellable entries after sort (`MAX_LISTED_ITEMS` in `open_face_vendor_lib.java`). Others in range are omitted (sorted by display name).
- Default **64 m** scan; tune with `open_face_vendor.scan_range_m` if needed.
- Combat blocks starting the conversation.

---

## Client / cinematic

Enable **cinematic conversation** for scripted camera (`npcConversationCameraLookAtTarget` / `npcConversationCameraReturnToSpeaker`).

---

## Troubleshooting

| Problem | Things to check |
|---------|------------------|
| "Not set up yet" | NPC has non-empty **`open_face_vendor.vendor_key`**. |
| "Nothing for sale" | Props in **range**; **`vendor_key`** matches NPC **exactly**; **`price` >= 1** on each. |
| Wrong stall's items appear | Two vendors used the **same key** - use unique keys per stall. |
| Item missing | Out of **range**; wrong key; no price; over **five-item** cap after sort. |
| Purchase fails | Inventory full; bad template/static name. |

---

## Migration from older setups

- **Explicit list** (`open_face_vendor.stock_objects`) - removed; use **vendor_key** on NPC + props and rely on range scan.
- **Container-based** (`stock_container`) - removed long ago; same migration: place props in world, set **vendor_key** + **price**.

---

## Related docs

- **`SERVER_SIDE_CONVERSATIONS.md`** - STF-less conversation API.

---

## Quick checklist

- [ ] NPC: script **`conversation.open_face_vendor`**
- [ ] NPC: **`open_face_vendor.vendor_key`** (unique string)
- [ ] Each prop: **same** **`open_face_vendor.vendor_key`** + **`open_face_vendor.price`**
- [ ] Props within **64 m** (or your **`scan_range_m`**)
- [ ] Cinematic conversation enabled on the client for camera behaviour
- [ ] *(Optional)* Bulk-tag props with **`item.container.open_face_vendor_staging`** radial **[OFV] Staging** before placing them in-world
