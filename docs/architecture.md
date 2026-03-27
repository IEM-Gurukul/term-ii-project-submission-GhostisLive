# ChatApp — Architecture & Design Document

## System Overview
A multi-client real-time chat application built on the Observer Pattern
and Java multithreading. The server acts as the Subject, maintaining a
list of registered observers (clients and loggers) and broadcasting
messages to all of them via a unified interface.

---

## Architecture Diagram
```
┌─────────────────────────────────────────────────────────┐
│                        CLIENT SIDE                       │
│                                                          │
│   ┌─────────────────┐        ┌──────────────────────┐   │
│   │  SwingClient    │        │   ConsoleClient      │   │
│   │  (GUI)          │        │   (Terminal)         │   │
│   └────────┬────────┘        └──────────┬───────────┘   │
│            │  Socket                    │  Socket        │
└────────────┼────────────────────────────┼───────────────┘
             │                            │
             ▼                            ▼
┌─────────────────────────────────────────────────────────┐
│                       SERVER SIDE                        │
│                                                          │
│   ┌──────────────────────────────────────────────────┐  │
│   │                   ChatServer                      │  │
│   │   (Subject — Observer Pattern)                    │  │
│   │                                                   │  │
│   │   - CopyOnWriteArrayList<ChatObserver> observers  │  │
│   │   - LinkedBlockingQueue<String> messageQueue      │  │
│   │   - ExecutorService threadPool (max 50 threads)   │  │
│   │                                                   │  │
│   │   register() / unregister() / broadcast()         │  │
│   └──────┬─────────────────────────────┬──────────────┘  │
│          │                             │                  │
│          ▼                             ▼                  │
│   ┌─────────────────┐       ┌──────────────────────┐     │
│   │  ClientHandler  │       │   MessageLogger      │     │
│   │  extends Thread │       │   implements         │     │
│   │  implements     │       │   ChatObserver       │     │
│   │  ChatObserver   │       │                      │     │
│   │                 │       │  Writes every        │     │
│   │  One per client │       │  message to file     │     │
│   │  socket         │       │  via Persistence     │     │
│   │                 │       │  Manager             │     │
│   └─────────────────┘       └──────────────────────┘     │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

---

## Layer Breakdown

### Observer Layer
| Class | Role |
|-------|------|
| `ChatObserver` | Interface defining `update(String message)` contract |
| `ClientHandler` | Concrete observer — sends message to client socket |
| `MessageLogger` | Concrete observer — writes message to log file |

### Server Layer
| Class | Role |
|-------|------|
| `ChatServer` | Subject — manages observer list and broadcast logic |
| `PersistenceManager` | Handles all file I/O, isolated from server logic |

### Client Layer
| Class | Role |
|-------|------|
| `SwingClient` | Dark-theme GUI client with chat bubbles and sidebar |
| `ConsoleClient` | Terminal-based client with listener thread |

### Infrastructure
| Class | Role |
|-------|------|
| `ClientDisconnectException` | Custom exception for clean disconnect handling |
| `Main` | Entry point — boots ChatServer on configurable port |

---

## Design Patterns Used

### 1. Observer Pattern
`ChatServer` is the Subject. `ClientHandler` and `MessageLogger`
are Observers. When a message arrives, `broadcast()` queues it
via `LinkedBlockingQueue` then notifies all registered observers
by calling `update()` on each.

### 2. Thread Pool (Executor Pattern)
Instead of spawning unlimited threads, `ExecutorService` with
`Executors.newFixedThreadPool(50)` caps concurrent client threads.
New connections queue if all 50 slots are busy.

### 3. Factory Method
All complex UI components in `SwingClient` are created via
dedicated `buildXXX()` methods — clean separation between
component construction and business logic.

---

## Thread Safety Strategy

| Shared Resource | Solution |
|----------------|----------|
| Observer list | `CopyOnWriteArrayList` — lock-free reads, copy-on-write |
| Message queue | `LinkedBlockingQueue` — thread-safe producer/consumer |
| Typing users set | `ConcurrentHashMap.newKeySet()` — concurrent access safe |
| Client thread lifecycle | `ExecutorService` — managed thread pool |

---

## Message Flow
```
Client types message
        ↓
ClientHandler.listenForMessages() reads from socket
        ↓
server.broadcast(message) called
        ↓
message placed into LinkedBlockingQueue
        ↓
processQueue() drains queue
        ↓
observer.update(message) called on every ChatObserver
        ↓
┌──────────────────┬───────────────────┐
│  ClientHandler   │   MessageLogger   │
│  writes to       │   writes to       │
│  client socket   │   log file        │
└──────────────────┴───────────────────┘
```

---

## Scalability Considerations

**Current:** Thread-per-client via `ExecutorService` — supports
up to 50 concurrent users with bounded resource usage.

**Future:** Java NIO with `Selector` and `SocketChannel` would
allow a single thread to manage thousands of connections via
non-blocking I/O — the architecture used by production servers
like Netty.