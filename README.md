<div align="center">

<!-- Animated Typing Header -->
<a href="#">
  <img src="https://readme-typing-svg.demolab.com?font=Orbitron&weight=700&size=34&duration=3200&pause=900&color=00E5FF&center=true&vCenter=true&width=760&height=70&lines=STARFALL%3A+SPACE+MYSTERY;PURE+JAVA+2D+SOCIAL+DEDUCTION+GAME;MULTITHREADING+%E2%80%A2+AI+%E2%80%A2+MULTIPLAYER" alt="STARFALL: SPACE MYSTERY" />
</a>

<p align="center">
  <b>Game 2D Multiplayer Social Deduction berbasis Pure Java 17+ tanpa game engine eksternal.</b><br/>
  Dibangun dengan Java Swing, Java 2D Graphics, Java Sound API, TCP Sockets, dan Multithreading.
</p>

<!-- Tech & Feature Badges -->
<p align="center">
  <img src="https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17+" />
  <img src="https://img.shields.io/badge/GUI-Java%20Swing%20%26%202D-00B4D8?style=for-the-badge" alt="Java Swing" />
  <img src="https://img.shields.io/badge/Network-TCP%20Client--Server-2EC4B6?style=for-the-badge" alt="TCP Multiplayer" />
  <img src="https://img.shields.io/badge/AI-State%20Machine%20%2B%20BFS-9B5DE5?style=for-the-badge" alt="State Machine AI" />
  <img src="https://img.shields.io/badge/Audio-Java%20Sound%20API-F15BB5?style=for-the-badge" alt="Java Sound API" />
  <img src="https://img.shields.io/badge/Dependencies-Zero%20External%20Libs-46D573?style=for-the-badge" alt="Zero Dependencies" />
</p>

<!-- Quick Navigation -->
<p align="center">
  <a href="#-identitas-pengembang">Identitas</a> •
  <a href="#-alur-interaktif--arsitektur">Diagram Interaktif</a> •
  <a href="#-galeri-output-program">Output Program</a> •
  <a href="#-fitur--eksplorasi-sistem">Fitur Lengkap</a> •
  <a href="#-cara-kompilasi--menjalankan">Cara Menjalankan</a>
</p>

</div>

---

## 👨‍💻 Identitas Pengembang

| Informasi | Detail |
| :--- | :--- |
| **Nama Author** | `[Isi Nama Lengkap Anda di Sini]` |
| **NIM** | `[Isi NIM Anda di Sini]` |
| **Universitas** | Universitas Mataram |
| **Fakultas / Prodi** | Fakultas Teknik / Program Studi Teknik Informatika |
| **Mata Kuliah** | Pemrograman Berorientasi Objek (PBO) |
| **Versi Rilis** | `v1.0.0-Release` |

---

## 🌌 Tentang Proyek

**STARFALL: SPACE MYSTERY** adalah game *social deduction* 2D *top-down* berlatar kapal luar angkasa **STARFALL-I** dengan **14 ruangan terhubung**. Pemain dibagi menjadi **Crewmate** (yang harus menyelesaikan berbagai mini-game *task*, memantau CCTV/Admin, dan menemukan penyusup) dan **Impostor** (yang menyabotase sistem kapal, berpindah lewat ventilasi, dan mengeliminasi kru tanpa saksi).

Seluruh sistem—mulai dari *rendering* 60 FPS *delta-time*, animasi karakter & kustomisasi, audio prosedural, kecerdasan buatan (*state-machine AI*), hingga server multiplayer otoritatif—diimplementasikan murni menggunakan pustaka standar **Java SE**.

---

## 🔄 Alur Interaktif & Arsitektur

> 💡 **Tips Interaktif (GitHub):** Arahkan kursor ke pojok kanan atas setiap diagram di bawah ini untuk menggunakan tombol **Zoom In (`+`)**, **Zoom Out (`-`)**, **Geser/Pan**, dan **Reset** agar alur sistem terlihat lebih detail.

### 1. Alur Gameplay & State Machine
```mermaid
stateDiagram-v2
    [*] --> MAIN_MENU
    MAIN_MENU --> SETTINGS : Pengaturan (Audio, Resolusi, Bahasa)
    MAIN_MENU --> HOW_TO_PLAY : Panduan
    MAIN_MENU --> CREDITS : Kredit
    MAIN_MENU --> LOBBY : Play Online / Host / Join
    MAIN_MENU --> PLAYING : Play Offline (vs AI)

    LOBBY --> PLAYING : Host klik Start Match
    LOBBY --> MAIN_MENU : Leave Lobby

    state PLAYING {
        [*] --> Exploring
        Exploring --> MiniGameTask : Tekan [E] di Terminal Task
        MiniGameTask --> Exploring : Selesai / [ESC]
        Exploring --> Surveillance : Tekan [E] di Admin / Security
        Surveillance --> Exploring : Tutup / [ESC]
        Exploring --> Venting : Impostor tekan [V]
        Venting --> Exploring : Keluar Vent [V]
    }

    PLAYING --> MEETING : Emergency Button [E] / Report Body [R]
    MEETING --> EJECTION : Voting Selesai / Waktu Habis
    EJECTION --> PLAYING : Permainan Berlanjut
    EJECTION --> GAME_OVER : Kondisi Menang/Kalah Terpenuhi
    PLAYING --> GAME_OVER : Semua Task Selesai / Sabotase Kritis / Parity
    GAME_OVER --> MAIN_MENU : Kembali ke Menu [ESC]
    GAME_OVER --> PLAYING : Main Lagi [SPACE]
```

### 2. Arsitektur Multithreading & Client-Server Networking
```mermaid
flowchart LR
    subgraph ClientApp["🖥️ Client Application (Pure Java)"]
        EDT["Swing EDT\n(GameWindow & GamePanel)"]
        GL["Thread: Starfall-GameLoop\n(60 FPS Delta-Time)"]
        AUD["Threads: Starfall-Music & SFX\n(Java Sound API)"]
        CLI["Threads: ClientReader & Writer\n(Non-Blocking TCP Socket)"]
    end

    subgraph ServerApp["🌐 Authoritative GameServer (Port 25565)"]
        ACC["Thread: ServerAcceptor\n(ServerSocket)"]
        CH["Threads: ClientHandler\n(Per-Client Reader/Writer)"]
        TICK["ScheduledExecutorService\n(20 Hz State Sync & Validation)"]
    end

    EDT <-->|Double-Buffered Render| GL
    GL -->|Event Audio| AUD
    GL <-->|ConcurrentLinkedQueue| CLI
    CLI <==>|Custom Protocol\nMove, Kill, Task, Vote, Chat| CH
    ACC --> CH
    CH <-->|Synchronized Lock| TICK
```

### 3. Peta Konektivitas 14 Ruangan & Jaringan Vent
```mermaid
graph LR
    UE["Upper Engine"] --- MED["MedBay"]
    UE --- CAFE["Cafeteria\n(Emergency Button)"]
    UE --- REA["Reactor"]
    UE --- SEC["Security\n(CCTV Cams)"]
    UE --- LE["Lower Engine"]
    REA --- SEC
    REA --- LE
    SEC --- LE
    LE --- ELEC["Electrical"]
    LE --- STOR["Storage"]
    ELEC --- STOR
    CAFE --- STOR
    CAFE --- ADM["Admin\n(Sensor Table)"]
    STOR --- ADM
    CAFE --- WEAP["Weapons"]
    WEAP --- O2["O2"]
    WEAP --- NAV["Navigation"]
    O2 --- NAV
    NAV --- SHD["Shields"]
    O2 --- SHD
    STOR --- COMM["Communications"]
    STOR --- SHD
    COMM --- SHD

    %% Jaringan Vent (Garis Putus-putus)
    MED -. "Vent" .- ELEC
    ELEC -. "Vent" .- SEC
    SEC -. "Vent" .- MED
    CAFE -. "Vent" .- ADM
    ADM -. "Vent" .- WEAP
    WEAP -. "Vent" .- CAFE
    REA -. "Vent" .- UE
    SHD -. "Vent" .- NAV
```

---

## 📸 Galeri Output Program

> 📁 **Cara Mengisi Gambar:** Simpan tangkapan layar (*screenshot*) hasil programmu di dalam folder **`assets/screenshots/`** dengan nama file sesuai tabel di bawah (atau sesuaikan path-nya).

| Menu Utama & Kustomisasi Karakter | Eksplorasi Kapal (14 Ruangan & Fog of War) |
| :---: | :---: |
| <img src="assets/screenshots/01-main-menu.png" alt="Main Menu STARFALL" width="420"/><br/><sub><i>Tampilan 8 menu utama dengan latar bintang & pratinjau karakter</i></sub> | <img src="assets/screenshots/02-gameplay-map.png" alt="Gameplay Map" width="420"/><br/><sub><i>Peta kapal STARFALL-I, pencahayaan dinamis, dan Minimap</i></sub> |
| **Mini-Game Task & Surveillance** | **Emergency Meeting, Chat & Voting** |
| <img src="assets/screenshots/03-task-minigame.png" alt="Task Mini-Game" width="420"/><br/><sub><i>Mini-game interaktif (Wiring, Swipe Card, Asteroids, Reactor)</i></sub> | <img src="assets/screenshots/04-meeting-voting.png" alt="Meeting & Voting" width="420"/><br/><sub><i>Layar diskusi chat, konfirmasi voting, dan anonimitas suara</i></sub> |
| **Multiplayer Lobby & Room Code** | **Ejection Cutscene / Victory & Defeat** |
| <img src="assets/screenshots/05-multiplayer-lobby.png" alt="Multiplayer Lobby" width="420"/><br/><sub><i>Lobi LAN/Online dengan 6-digit Room Code dan Roster sinkron</i></sub> | <img src="assets/screenshots/06-game-over.png" alt="Game Over Screen" width="420"/><br/><sub><i>Pengungkapan peran asli seluruh pemain pada layar hasil akhir</i></sub> |

---

## 🧩 Fitur & Eksplorasi Sistem

Klik masing-masing panel di bawah untuk membuka detail sistem:

<details>
<summary><b>🎮 Kontrol Permainan Lengkap</b></summary>
<br/>

| Tombol / Input | Fungsi |
| :--- | :--- |
| `W A S D` / `Arrow Keys` | Menggerakkan karakter / Berpindah jaringan Vent (`A` / `D`) |
| `E` | Interaksi (Buka Task, Tombol Darurat Cafeteria, Perbaiki Sabotase, Admin/CCTV) |
| `R` | Melaporkan mayat terdekat (*Dead Body Report*) |
| `Q` | Mengeliminasi Crewmate dalam jangkauan (*Khusus Impostor*) |
| `V` | Masuk / Keluar ventilasi (*Khusus Impostor*) |
| `1` – `5` | Memicu Sabotase: `[1]` Reactor, `[2]` O2, `[3]` Lights, `[4]` Comms, `[5]` Doors |
| `C` / `H` / `K` / `P` | Ganti Warna (12), Topi (7), Skin (5), dan Pet (3) |
| `F3` / `F11` | Tampilkan/Sembunyikan FPS Debug HUD / Toggle Fullscreen |
| `ESC` | Tutup modal aktif atau kembali ke Main Menu |

</details>

<details>
<summary><b>🛠️ Sistem Task (14 Tugas & Mini-Game Interaktif)</b></summary>
<br/>

| Mini-Game Class | Nama Tugas | Lokasi Ruangan | Mekanik Permainan |
| :--- | :--- | :--- | :--- |
| `WiringTask` | Fix Wiring | Electrical | Sambungkan 4 kabel warna acak kiri ke kanan |
| `CardTask` | Swipe Card | Admin | Gesek kartu ID dengan kecepatan pas (*Too Fast/Slow*) |
| `DownloadTask` | Download & Upload Data | Communications, Admin | Transfer paket data terenkripsi dengan progres animasi |
| `AsteroidsTask` | Clear Asteroids | Weapons | Tembak jatuh 10 asteroid bergerak di radar plasma |
| `ReactorTask` | Start Reactor | Reactor | Uji memori pola keypad 3×3 (4 tahap) |
| `CalibrationTask` | Empty Garbage, Clean O2, Stabilize Steering, Prime Shields, Fuel Engines, Align Engine, Unlock Manifolds, Submit Scan | Cafeteria, O2, Navigation, Shields, Storage, Lower/Upper Engine, MedBay | Kalibrasi 4 modul frekuensi sistem kapal |

</details>

<details>
<summary><b>🚨 5 Jenis Sabotase & Sistem Peran (Crewmate, Impostor, Ghost)</b></summary>
<br/>

- **Reactor Meltdown (`[1]`):** Hitung mundur 30 detik; Impostor langsung menang jika tidak diperbaiki di **Reactor**.
- **Oxygen Depletion (`[2]`):** Hitung mundur 30 detik; Impostor langsung menang jika tidak diperbaiki di **O2**.
- **Lights Sabotage (`[3]`):** Memperkecil radius jarak pandang Crewmate secara drastis hingga diperbaiki di **Electrical**.
- **Communications Sabotage (`[4]`):** Mematikan Minimap dan daftar tugas Crewmate hingga diperbaiki di **Communications**.
- **Door Lockdown (`[5]`):** Mengunci pintu ruangan-ruangan utama selama 10 detik.
- **Ghost Mode:** Pemain yang tereliminasi otomatis menjadi hantu transparan yang dapat menembus tembok untuk memantau permainan dan tetap membantu menyelesaikan sisa tugas.

</details>

<details>
<summary><b>🧠 Kecerdasan Buatan (State-Machine AI)</b></summary>
<br/>

- **Navigasi BFS (`AINavigation`):** Pencarian jalur terpendek lintas 14 ruangan dengan titik aman agar tidak tersangkut meja/peti.
- **`CrewmateAI`:** Menjalankan tugas nyata, bergegas memperbaiki sabotase mematikan, melaporkan mayat yang terlihat, serta memberikan kesaksian chat dan *voting* berbasis bukti visual (tidak curang mengetahui peran pemain lain).
- **`ImpostorAI`:** Memeriksa keberadaan saksi sebelum membunuh, kabur lewat *Vent* terdekat, memicu sabotase secara strategis, berpura-pura mengerjakan tugas (*fake task*), dan membuat alibi saat diskusi.

</details>

<details>
<summary><b>📚 Penerapan Konsep PBO & Java Concurrency</b></summary>
<br/>

- **OOP & SOLID:** Enkapsulasi penuh, pewarisan & polimorfisme pada hierarki `Character` (`Crewmate`, `Impostor`, `Ghost`), kelas abstrak `Task` & `AIController`, serta pemisahan tanggung jawab per paket (*Separation of Concerns*).
- **Java Swing & Java 2D:** `JFrame`, `JPanel` *double-buffered*, *Event Dispatch Thread* (`SwingUtilities.invokeLater`), `Graphics2D`, *Area-union collision*, pencahayaan *Fog of War*, dan `ParticleSystem`.
- **Multithreading & Thread Safety:** `GameLoop` terpisah (60 FPS *delta-time*), `ConcurrentHashMap` & `ConcurrentLinkedQueue` pada `InputManager` dan `GameClient`, `ExecutorService` untuk audio, serta `ScheduledExecutorService` dan blok `synchronized` pada `GameServer`.
- **File I/O:** Penyimpanan otomatis preferensi pemain ke `config/starfall.properties`.

</details>

<details>
<summary><b>📂 Struktur Direktori Proyek</b></summary>
<br/>

```text
STARFALL/
├── assets/
│   └── screenshots/              # Tempat menyimpan gambar output program
├── config/
│   └── starfall.properties       # File konfigurasi otomatis (File I/O)
├── README.md
└── src/
    └── starfall/
        ├── Main.java             # Entry point aplikasi (Swing EDT)
        ├── ai/                   # AIController, AINavigation, AIState, CrewmateAI, ImpostorAI
        ├── audio/                # AudioManager (Java Sound API)
        ├── core/                 # GameConfig, GameEngine, GameLoop, GameState, InputManager
        ├── entities/             # Character, Crewmate, Impostor, Ghost, Player, Cosmetics, PlayerColor
        ├── gameplay/             # TaskManager, KillManager, VentManager, SabotageManager, MeetingManager, VotingManager, WinConditionManager
        ├── graphics/             # Renderer, Camera, SpriteManager, ParticleSystem
        ├── map/                  # GameMap, Room, RoomId, Wall, Door, Vent, InteractiveObject, CollisionManager
        ├── network/              # GameServer, ClientHandler, GameClient, NetworkManager, NetworkMessage
        ├── tasks/                # Task, TaskRegistry, WiringTask, CardTask, DownloadTask, AsteroidsTask, ReactorTask, CalibrationTask
        ├── ui/                   # GameWindow, GamePanel, MainMenu, SettingsScreen, LobbyScreen, MeetingScreen, EjectionScreen, GameOverScreen, SurveillanceOverlay
        └── utils/                # Logger
```

</details>

---

## 🚀 Cara Kompilasi & Menjalankan

### Prasyarat
- **Java Development Kit (JDK) 17 atau lebih baru**

### 1. Kompilasi & Jalankan Game Client
Jalankan perintah berikut dari direktori utama `STARFALL`:

**Windows (PowerShell):**
```powershell
mkdir out -Force
javac -d out (Get-ChildItem -Recurse -Filter *.java src).FullName
java -cp out starfall.Main
```

**Linux / macOS / Git Bash:**
```bash
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out starfall.Main
```

### 2. Mode Multiplayer (LAN / Online Dedicated Server)
- **Host langsung dari game:** Buka **Main Menu → 2. CREATE PRIVATE ROOM**, catat **6-digit Room Code**, lalu teman lain masuk melalui **3. JOIN ROOM** menggunakan `IP_HOST:25565` dan kode ruangan tersebut.
- **Menjalankan Dedicated Server mandiri (Online):**
```bash
java -cp out starfall.network.GameServer 25565
```

---

<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:080B14,50:00B4D8,100:7209B7&height=100&section=footer" width="100%" alt="Footer Wave"/>
  <p><b>STARFALL: SPACE MYSTERY</b> — Crafted with Pure Java ☕</p>
</div>