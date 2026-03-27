
# UML Class Diagram
```
┌─────────────────────────────┐
│       <<interface>>         │
│        ChatObserver         │
│─────────────────────────────│
│ + update(message: String)   │
└─────────────┬───────────────┘
              │ implements
    ┌─────────┴──────────┐
    │                    │
    ▼                    ▼
┌────────────────┐  ┌───────────────────┐
│ ClientHandler  │  │   MessageLogger   │
│────────────────│  │───────────────────│
│ - socket       │  │ - persistence     │
│ - server       │  │   Manager         │
│ - out          │  │───────────────────│
│ - in           │  │ + update()        │
│ - username     │  └───────────────────┘
│────────────────│           │ uses
│ + run()        │           ▼
│ + update()     │  ┌───────────────────┐
│ + getUsername()│  │ PersistenceManager│
│ - disconnect() │  │───────────────────│
│ - listenFor    │  │ - logFilePath     │
│   Messages()   │  │───────────────────│
│ - handlePrivate│  │ + saveMessage()   │
│   Message()    │  │ - writeToFile()   │
└───────┬────────┘  │ - ensureFile      │
        │ uses      │   Exists()        │
        ▼           └───────────────────┘
┌────────────────────────────────────┐
│            ChatServer              │
│────────────────────────────────────│
│ - port: int                        │
│ - observers: CopyOnWriteArrayList  │
│ - threadPool: ExecutorService      │
│ - messageQueue: LinkedBlockingQueue│
│ - serverSocket: ServerSocket       │
│────────────────────────────────────│
│ + register(observer)               │
│ + unregister(observer)             │
│ + broadcast(message)               │
│ + sendPrivateMessage()             │
│ + broadcastTyping()                │
│ + getOnlineUsers()                 │
│ + start()                          │
│ + stop()                           │
│ - acceptClients()                  │
│ - processQueue()                   │
└───────────────┬────────────────────┘
                │ creates
                ▼
┌──────────────────────────────┐
│           Main               │
│──────────────────────────────│
│ - DEFAULT_PORT: int = 5000   │
│──────────────────────────────│
│ + main(args: String[])       │
└──────────────────────────────┘

┌─────────────────────────────────────┐
│      ClientDisconnectException      │
│─────────────────────────────────────│
│ - username: String                  │
│─────────────────────────────────────│
│ + ClientDisconnectException(user)   │
│ + getUsername(): String             │
└─────────────────────────────────────┘

┌─────────────────────────────────────┐
│   <<Test>> MockObserver             │
│─────────────────────────────────────│
│ - receivedMessages: List<String>    │
│ - name: String                      │
│─────────────────────────────────────│
│ + update(message)                   │
│ + received(message): boolean        │
│ + getLastMessage(): String          │
│ + getMessageCount(): int            │
│ + clear()                           │
└─────────────────────────────────────┘
```