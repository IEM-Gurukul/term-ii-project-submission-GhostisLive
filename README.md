[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/pG3gvzt-)
# PCCCS495 – Term II Project

## Project Title
Real-Time Chat Application with Observer-Based Messaging

---

## Problem Statement (max 150 words)
Students and developers working in groups often find that basic messaging demos fail to reflect how real communication software behaves. Most introductory-level implementations handle only a single client at a time and rely on polling loops, obscuring the underlying event-driven logic. The proposed Real-Time Chat Application addresses this by building a multi-client server where each connected user runs on a dedicated thread and receives messages through the Observer Pattern. Every inbound message triggers a broadcast through a registered observer list, demonstrating event propagation, thread lifecycle management, and clean separation between connection handling and notification delivery.

---

## Target User
Students collaborating on group projects who need a lightweight, self-hosted messaging tool; small development teams requiring internal chat without third-party service dependencies.

---

## Core Features
- Connect multiple clients simultaneously over a local network
- Broadcast messages in real time to all registered observers
- Username registration and active session management
- Private direct messaging between specific users
- Persistent chat log saved to file across sessions
- Graceful disconnect handling and robust exception recovery

---

## OOP Concepts Used

- **Abstraction:** `ChatObserver` interface defines the `update(message)` contract — server depends only on this abstraction, never on concrete implementations
- **Inheritance:** `ClientHandler` extends `Thread`, overriding `run()` for per-client processing; specialized variants extend the base without modifying it
- **Polymorphism:** `notifyObservers()` calls `update()` on every `ChatObserver` reference — socket, logger, or UI panel — identical call regardless of type
- **Encapsulation:** `ChatServer` keeps its observer list and socket logic private; external code interacts only via public methods like `register()`, `broadcast()`
- **Exception Handling:** `SocketException`, `IOException`, and custom `ClientDisconnectException` caught at separate layers; dropped clients removed, server continues
- **Collections:** `CopyOnWriteArrayList<ChatObserver>` holds all active observers — thread-safe to iterate while clients connect or disconnect simultaneously
- **Threads:** Each `ClientHandler` runs on its own `Thread` via `ExecutorService` thread pool — multiple clients served concurrently without blocking
- **Design Pattern:** Built on the Observer Pattern — `ChatServer` is the Subject; `ClientHandler` and `MessageLogger` are registered Observers

---

## Proposed Architecture Description
The system is built around the Subject–Observer design pattern layered over Java sockets and threads. `ChatServer` acts as the Subject, maintaining a registered observer list and handling all broadcast logic. Each client connection is wrapped in a `ClientHandler` that extends `Thread` and implements `ChatObserver`, running independently and receiving broadcasts via `update()`. A `MessageLogger` observer persists every message to disk without coupling to client code, while a `PersistenceManager` isolates all file I/O from the server layer. New observer types can be added by implementing the interface alone — no existing classes need modification.
```
ChatServer → ClientHandler (Thread + Observer) → ChatObserver (Interface) → MessageLogger / PersistenceManager
```

### Scalability Note
Current model uses thread-per-client managed via `ExecutorService` thread pool, capping resource usage and reusing threads efficiently. For very high user counts, Java NIO with `Selector` and `SocketChannel` would allow a single thread to manage thousands of connections — this is noted as a future direction.

---

## How to Run
*Will be updated as implementation progresses.*

---

## Git Discipline Notes
Minimum 10 meaningful commits required.