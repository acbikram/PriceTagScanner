# PriceTag Scanner — Architecture

## Clean Architecture Layers

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│                                                                 │
│  MainScreen ──► MainViewModel ──► SendJobUseCase                │
│  HistoryScreen ► HistoryViewModel                               │
│  SettingsScreen ► SettingsViewModel                             │
│  ScannerScreen  (CameraX + ML Kit)                              │
└──────────────────────────┬──────────────────────────────────────┘
                           │ StateFlow / Events
┌──────────────────────────▼──────────────────────────────────────┐
│                      DOMAIN LAYER                               │
│                                                                 │
│  SendJobUseCase    BuildPayloadUseCase                          │
│  JobRepository (interface)   SettingsRepository (interface)     │
│  TagType   UnitType   ScanJob   AppSettings                     │
└──────────────────────────┬──────────────────────────────────────┘
                           │ Interface implementations
┌──────────────────────────▼──────────────────────────────────────┐
│                       DATA LAYER                                │
│                                                                 │
│  SocketManager (TCP, heartbeat, auto-reconnect)                 │
│  JobRepositoryImpl ──► Room DB (JobEntity, JobDao)              │
│  SettingsRepositoryImpl ──► DataStore (AppPreferences)          │
└─────────────────────────────────────────────────────────────────┘
```

## Protocol Format

```
BARCODES|TAG_TYPE|UNIT_TYPE|COPIES|TIMESTAMP\n

A4/VEG:      1234567890123|A4|PCS|2|1712345678
4PCS:        111,222,333,444|4PCS|PCS|1|1712345678
4PCS_DATE:   111,222,333,444|4PCS_DATE|CTN|2|1712345678
4PCS_SAME:   111,111,111,111|4PCS_SAME|PCS|1|1712345678
VEG:         69051|VEG|KGS|3|1712345678

Server responses:
  OK|JOB_ID        → success
  ERR|message      → error
  DUP|barcode|msg  → duplicate suppressed
  PONG             → heartbeat acknowledgement
```

## Offline Queue Flow

```
Scan → SendJobUseCase
  ├─ Save to Room DB (PENDING)
  ├─ isConnected?
  │     YES → socketManager.send(payload)
  │             ├─ OK  → markSent()  → SUCCESS
  │             └─ ERR → markFailed() → FAILURE
  └─     NO  → return Queued (DB holds job safely)

Retry loop (every 15s):
  isConnected? → getPendingList() → send each → markSent/Failed
```

## Thread Safety

| Component | Threading approach |
|-----------|-------------------|
| SocketManager | Mutex on send(), Coroutines IO dispatcher |
| Room DB | Suspend functions, coroutines |
| DataStore | Flow (cold stream) + coroutines |
| ViewModels | viewModelScope (Main dispatcher) |
| Scanner | ImageAnalysis on dedicated Executor |
| BarcodeValidator | Main thread only (duplicate window) |
