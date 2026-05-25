<div id="top"></div>

<br />
<div align="center">
  <h3 align="center">Jira Hour Logger</h3>
  <p align="center">A floating desktop tool that logs work hours to Jira by reading your markdown note files.</p>
</div>

---

## About The Project

Jira Hour Logger is a JavaFX desktop application that lives in the corner of your screen as a small floating sphere. When you click it, a radial menu appears with action bubbles (e.g. "Notes"). Clicking "Notes" opens a window where you can drag and drop markdown files containing your daily work log. The app parses the time blocks and ticket URLs from those files and automatically posts the work log entries to Jira via its REST API.

**The goal:** remove the friction of manually logging hours in Jira at the end of the day.

---

## How It Works

### Note file format

Drop a `.md` file into the Notes window. The app expects time blocks like this:

```
Date: 07/05/2026

09:00 - 11:00

- https://yourcompany.atlassian.net/browse/PROJ-123
    - Optional description of what you did

13:00 - 14:30

- Call w ...
- https://yourcompany.atlassian.net/browse/PROJ-456
```

- **`Date:`** — the date the work was done (DD/MM/YYYY). Defaults to today if missing.
- **Time range** — `HH:mm - HH:mm` on its own line starts a new work block.
- **URL bullet** — a `- https://...` line identifies the Jira ticket for that block.
- **Description bullet** — any other `- text` line is treated as a work description and included in the Jira worklog comment.

### Submit flow

When you click **Submit**:
1. Every time block is collected from all dropped files.
2. For each block, the app fetches the subtasks of the Jira ticket.
   - **0 or 1 subtask** → work is logged automatically.
   - **2+ subtasks** → a picker window opens so you can choose which subtask to log to.
3. A live progress window shows the status of each entry (pending → running → success / error).
4. On full success the note files are deleted and a `worklog_DD_MM_YYYY_HH_mm.md` summary file is written to `.jirahourlogger/`.

---

## Project Structure

```
src/main/java/com/jirahourlogger/
│
├── Launcher.java          ← Real entry point — hands off to App.main()
├── App.java               ← JavaFX Application; hides the default window, shows the sphere
│
├── api/
│   └── JiraClient.java    ← All Jira REST API calls (fetch subtasks, post worklog)
│
├── helper/
│   ├── AnimationHelper.java ← Bubble show/hide animations (scale + fade)
│   ├── PaneHelper.java      ← Builder wrapper around JavaFX Pane
│   ├── SphereHelper.java    ← Builds the main sphere circle and leaf bubble positions
│   └── StageHelper.java     ← Builder wrapper around JavaFX Stage (OS window)
│
├── model/
│   ├── JiraIssue.java     ← Record: key, summary, self URL for a Jira issue
│   ├── LogEntry.java      ← Record: time range + ticket URL + optional description
│   ├── Note.java          ← POJO: a single dropped file with its parsed log entries
│   └── NoteParser.java    ← Parses raw markdown text into LogEntry objects
│
├── storage/
│   └── NotesManager.java  ← Reads/writes note files from .jirahourlogger/notes/
│
└── ui/
    ├── BubbleDef.java           ← Record: label, angle, colour for one leaf bubble
    ├── BubbleItem.java          ← Circle + label node for a leaf bubble
    ├── LoggingProgressView.java ← Live status window shown during submit
    ├── MainFloatingSphere.java  ← The main floating sphere + radial menu
    ├── NotesView.java           ← Drag-and-drop notes window
    └── SubtaskPickerView.java   ← Subtask selection window (shown when > 1 subtask)
```

---

## Getting Started

### Prerequisites

- **Java 21** or later
- **Maven** (bundled via `mvnw` if you don't have it globally)
- A Jira account with an API token ([create one here](https://id.atlassian.com/manage-profile/security/api-tokens))

### Credentials setup

Create a `.env` file in the project root (next to `pom.xml`):

```
JIRA_EMAIL=you@yourcompany.com
JIRA_API_TOKEN=your_api_token_here
```

This file is listed in `.gitignore` and will not be committed.

### Run

```bash
mvn javafx:run
```

Or run the `Launcher` class directly from IntelliJ IDEA.

---

## Built With

- [Java 21](https://openjdk.org/) — core language
- [JavaFX 21](https://openjfx.io/) — desktop UI framework
- [Gson](https://github.com/google/gson) — JSON parsing for Jira API responses
- [Maven](https://maven.apache.org/) — build and dependency management

---

## Programming Concepts Applied

This section documents the Java and software engineering concepts used throughout the project. It serves as a personal reference for understanding what the code does and why it was written that way.

---

### Object-Oriented Programming (OOP)

**Inheritance** — `BubbleItem extends Circle`. BubbleItem *is* a Circle and inherits all its properties (centre, radius, fill colour, etc.), while adding its own label on top. Any place in the code that expects a Circle can receive a BubbleItem without changes.

**Encapsulation** — Fields in `Note` are `private final` with public getters. Other classes can read values but cannot change them directly. This protects the data from accidental modification.

**Packages as layers** — The code is split into `api`, `helper`, `model`, `storage`, and `ui` packages. Each layer has one responsibility and doesn't bleed into the others. For example, the UI never touches the file system directly — it always goes through `NotesManager`.

---

### Design Patterns

**Builder / Method Chaining** — `StageHelper` and `PaneHelper` return `this` from every setter so multiple setup calls can be written as one readable chain:

```java
stageHelper
    .initStyle(StageStyle.TRANSPARENT)
    .setAlwaysOnTop(true)
    .setScene(scene)
    .show();
```

**Utility class** — `NoteParser` has only `static` methods and a `private` constructor. You cannot create an instance of it — it is just a toolbox of functions that belong together logically.

**Callback / Runnable** — `LoggingProgressView.setOnSuccess(Runnable)` and the `onComplete` parameter in `SubtaskPickerView`. You pass a block of code to run *later* when an async operation finishes, rather than blocking and waiting.

---

### Java Language Features

**Records** — `JiraIssue`, `LogEntry`, `BubbleDef`, and `LogEntry.TimeRange` are all records. A `record` is a shorthand for a class that only holds data — the compiler automatically generates the constructor, getters, `equals()`, `hashCode()`, and `toString()`, removing boilerplate.

```java
public record JiraIssue(String key, String summary, String self) {}
```

**`Optional<T>`** — Used for `description` in `LogEntry` and `loggedDate` in `Note`. Instead of returning `null` when a value might be absent, `Optional` forces callers to consciously handle both cases. This prevents `NullPointerExceptions`.

```java
entry.description().ifPresent(desc -> System.out.println(desc));
```

**Enums** — `LoggingProgressView.Status` defines a fixed set of named states: `PENDING`, `RUNNING`, `SUCCESS`, `ERROR`. Enums give compile-time safety — the compiler will warn you if you forget to handle a case.

**Nested classes** — `LogEntry.TimeRange` and `EntryCard` (inside `LoggingProgressView`) are defined inside another class because they only make sense in that context. Keeping them nested avoids polluting the package with classes that have no standalone use.

**`final` fields** — All fields in `Note` are `final`, meaning they can only be assigned once in the constructor. This makes the object immutable — its data never changes after creation, which is safer to reason about.

**Varargs** (`Node... nodes`) — `PaneHelper.addChildren()` accepts any number of arguments separated by commas. The compiler wraps them into an array behind the scenes:

```java
paneHelper.addChildren(sphere, label, icon); // pass as many as you need
```

---

### Functional-Style Programming

**Lambdas** — Short anonymous functions passed as arguments instead of writing a full separate method:

```java
notes.forEach(n -> System.out.println(n.getTitle()));
```

**Streams + method references** — `NoteParser` uses the Stream API to process lines as a pipeline:

```java
Arrays.stream(lines)
      .map(DATE_LINE::matcher)   // run the regex on each line
      .filter(Matcher::matches)  // keep only lines that matched
      .findFirst()               // take the first match
```

Each step transforms or filters the data without a manual loop.

---

### Concurrency & Threading

**Background threads** — Jira API calls run on a new `Thread` so the UI does not freeze while waiting for a network response. The app stays interactive during the request.

**`Platform.runLater()`** — JavaFX requires all UI updates to happen on its dedicated "JavaFX Application Thread". After a background thread receives an API response, it hands the UI update back to that thread via `Platform.runLater()`. Skipping this would cause crashes or visual glitches.

---

### Regular Expressions (Regex)

`NoteParser` uses `Pattern` and `Matcher` to detect time ranges and URL bullets from raw markdown text. A `Pattern` is a compiled search rule written in regex syntax. A `Matcher` runs that rule against a specific string and lets you extract the matched groups:

```java
// Matches lines like "09:00 - 11:00"
Pattern TIME_RANGE = Pattern.compile("^\\s*(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})\\s*$");
```

---

### File I/O with Java NIO

`NotesManager` uses `java.nio.file` — the modern Java file API — to copy files, read text content, list directory contents, and read file metadata like creation time. Everything is done with the standard library; no external dependency is needed.

---

### HTTP & REST APIs

`JiraClient` uses Java 21's built-in `HttpClient` to make GET and POST requests to Jira's REST API. Key concepts used:

- **HTTP Basic Auth** — email and API token are joined with `:`, encoded as Base64, and sent in an `Authorization` header on every request.
- **JSON parsing** — Jira returns JSON responses. The Gson library parses them into `JsonObject` and `JsonArray` objects so individual fields can be read by name.
- **HTTP status codes** — the code checks the response status code (`200 OK`, `201 Created`) to know whether the request succeeded, and throws a descriptive error if not.

---

### JavaFX UI Framework

**Stage & Scene** — A `Stage` is an OS window. A `Scene` is the content (nodes) inside that window. You must have both — a Stage without a Scene is an empty window.

**Layout containers** — These arrange child nodes automatically:
- `VBox` — stacks children vertically (top to bottom)
- `HBox` — arranges children horizontally (left to right)
- `Pane` — absolute positioning; you set each child's x/y coordinate manually
- `StackPane` — layers children on top of each other (used for the spinner indicator)
- `ScrollPane` — wraps content and adds a scrollbar when it overflows

**CSS styling** — JavaFX uses a CSS-like syntax to style nodes without writing separate files:

```java
node.setStyle("-fx-background-color: #1E1E2E; -fx-background-radius: 8;");
```

**Event handlers** — Lambdas attached to nodes that fire when the user interacts:

```java
button.setOnMouseClicked(e -> stageHelper.close());
zone.setOnDragOver(e -> e.acceptTransferModes(TransferMode.COPY));
```

**Animations** — Each class smoothly changes one property of a node over a set duration:
- `ScaleTransition` — changes size (used for the bubble pop-in/out)
- `FadeTransition` — changes opacity (used to fade labels in/out)
- `TranslateTransition` — moves a node (used for the slide-up entrance of the progress window)
- `SequentialTransition` — plays animations one after another (overshoot → settle = bounce)
- `ParallelTransition` — plays animations at the same time (fade + slide together)
- `Timeline` — the most flexible; animates any property using `KeyFrame` checkpoints

**Properties & Listeners** — `DoubleProperty` in `toggleMenu()` drives the stage resize, stage position, and sphere position all from a single 0→1 value. A listener fires on every change and updates all three in sync, ensuring the sphere never visually jumps during the window resize.

---

## Contact

Acdaling Edusei — [@accietheking](https://x.com/accietheking) — acdaling@gmail.com

Project Link: [https://github.com/AccieTheKing/JiraHourLogger](https://github.com/AccieTheKing/JiraHourLogger)

<p align="right">(<a href="#top">back to top</a>)</p>
