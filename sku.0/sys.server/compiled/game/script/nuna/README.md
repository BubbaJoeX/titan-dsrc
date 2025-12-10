# Nuna — Player entitlement & currency tracker

Location: `sku.0/sys.server/compiled/game/script/nuna/README.md`

## Purpose
Nuna tracks player/account persistent attributes required for web-code redemptions and in-game currency/account state:
- name
- money (primary currency)
- stationId
- accountName
- heroic token currencies
- account flags

An `entitlements` attachment is stored per-player (or per-account where applicable). Codes redeemed on the website are represented in `entitlements` and are used to validate and grant rewards in-game.

## Data model (recommended objvar keys)
Store values as objvars on the player or account object. Recommended keys:
- `nuna.name` (string)
- `nuna.money` (long/int or numeric type)
- `nuna.stationId` (string)
- `nuna.accountName` (string)
- `nuna.heroic_tokens` (int)
- `nuna.flags` (int bitmask or JSON object)
- `nuna.entitlements` (JSON string / serialized list)

Note: Where account-level state is required, attach to the account/character container or global account object. For planet-level/feature defaults, use a planet object such as `getPlanetByName("tatooine")` as a source object for default configuration.

## `entitlements` format
Use a list of entitlement objects. Store as JSON string in `nuna.entitlements` or as structured objvars per-entry.

Example JSON:
{
"entitlements": [
{
"code": "SUMMER2025-ABC123",
"rewardType": "heroic_token",
"amount": 10,
"redeemedAt": 1710000000,
"granted": false,
"source": "web",
"meta": {}
}
]
}

Fields:
- `code`: unique redemption code
- `rewardType`: e.g. `heroic_token`, `currency`, `item`
- `amount`: numeric reward size
- `redeemedAt`: epoch seconds when code was redeemed on website
- `granted`: boolean indicating if in-game reward has been applied
- `source` / `meta`: optional for tracking

## Flow: website -> server -> player
1. Website redeems code and POSTs to the redemption API.
2. Redemption service validates and writes an entitlement entry to the player's `nuna.entitlements`.
3. On player login or via an in-game event, server checks `nuna.entitlements`:
    - Iterate ungranted entries, validate them, apply reward (add tokens/currency/flags/items).
    - Mark entry `granted = true` and update `redeemedAt` or `processedAt`.
4. Optionally reply to website with final status.

## Java API (guideline)
- NunaManager.load(playerId)
- NunaManager.getMoney(playerId)
- NunaManager.addMoney(playerId, amount)
- NunaManager.getEntitlements(playerId) -> List<Entitlement>
- NunaManager.grantEntitlement(playerId, entitlement)

Implement reading/writing using the engine's objvar helpers (getIntObjVar/getStringObjVar/getLocationObjVar/setObjVar). For defaults/config values, read from a planet object:
- `obj_id config = getPlanetByName("tatooine");`
- read e.g. `getIntObjVar(config, "bonus.rls.maxLevelsAbove")`

## Concurrency & persistence
- Update `nuna.entitlements` atomically where possible; persist after marking `granted`.
- Use server-safe serialization (JSON or engine serialization).
- Avoid duplicate grants by checking `granted` boolean and by using idempotent grant operations.

## Notes
- Keep entitlements small per-player; purge or archive old entries.
- `entitlements` is the canonical place to check redeemed website codes before granting rewards.
- Use logging for auditability when codes are applied or rejected.
