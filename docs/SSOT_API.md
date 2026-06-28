# ControlCom API Contract (SSOT)

Single source of truth for the Android client and Windows PC agent.

## Base URL

```
http://{pc_ip}:{port}
```

Default port: `7847`

## Authentication

All endpoints except the public endpoints below require:

```
Authorization: Bearer {token}
```

**Public endpoints (no auth):**
- `GET /api/health`
- `GET /api/pairing/info`
- `GET /pair` (HTML QR page)
- `POST /api/auth/pair` (when not yet paired)

## Endpoints

### GET /api/health

Connection check. No auth required.

**Response 200**
```json
{
  "ok": true,
  "service": "controlcom-agent",
  "version": "1.0.0"
}
```

### GET /api/pairing/info

Pairing status and QR payload. No auth required.

**Response 200 (not paired)**
```json
{
  "paired": false,
  "ip": "192.168.0.15",
  "port": 7847,
  "code": "123456",
  "qrPayload": "controlcom://pair?ip=192.168.0.15&port=7847&code=123456"
}
```

**Response 200 (already paired)**
```json
{
  "paired": true,
  "ip": "192.168.0.15",
  "port": 7847
}
```

### GET /pair

HTML page with pairing QR code. No auth required.

### POST /api/auth/pair

Pair a new client. Requires pairing code shown on PC pairing page.

**Request**
```json
{
  "code": "123456"
}
```

**Response 200**
```json
{
  "token": "uuid-token-string"
}
```

**Response 401**
```json
{
  "error": "invalid_code"
}
```

### POST /api/power/sleep

Put the PC into Sleep mode.

**Response 200**
```json
{
  "ok": true
}
```

### POST /api/power/shutdown

Best-effort save all visible windows, wait, then schedule shutdown.

**Response 200**
```json
{
  "ok": true,
  "message": "shutdown_scheduled",
  "windowsNotified": 5,
  "delaySeconds": 10
}
```

**Limitations**
- Not all applications honor WM_SAVE or Ctrl+S.
- Save dialogs may still appear on the PC.
- Unity, Photoshop, and similar apps may not save silently.

### GET /api/audio/mute

**Response 200**
```json
{
  "muted": false
}
```

### POST /api/audio/mute/toggle

**Response 200**
```json
{
  "muted": true
}
```

### GET /api/display/mode

**Response 200**
```json
{
  "mode": "dual"
}
```

`mode` is either `single` (primary monitor only) or `dual` (extend).

### POST /api/display/mode

**Request**
```json
{
  "mode": "single"
}
```

**Response 200**
```json
{
  "mode": "single"
}
```

## Error format

**Response 4xx/5xx**
```json
{
  "error": "error_code",
  "message": "Human readable message"
}
```

Common errors:
- `unauthorized` — missing or invalid token
- `invalid_mode` — display mode must be `single` or `dual`
- `operation_failed` — handler execution failed

## Responsibility ownership

| Layer | Owns |
|-------|------|
| Android app | UI, settings, HTTP requests |
| PC agent | Sleep, mute, display, shutdown execution |
| This document | Request/response schemas |

Visual actions on Android MUST NOT change PC logical state directly.
