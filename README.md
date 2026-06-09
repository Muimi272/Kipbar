# Kipbar

Kipbar is a small Java progress bar utility for console applications.

Maven Central:

- https://central.sonatype.com/artifact/club.muimi/Kipbar

It provides:

- Configurable progress bar width and fill character
- Optional task name, percentage display, and elapsed time display
- Success and failure completion messages
- A simple task runner API for wrapping interruptible work

## Requirements

- JDK 25
- Maven 3.9+

## Installation

Add Kipbar from Maven Central:

```xml
<dependency>
    <groupId>club.muimi</groupId>
    <artifactId>Kipbar</artifactId>
    <version>1.1.1</version>
</dependency>
```

Or clone the project and build it locally:

```bash
mvn test
```

If you want to install it into your local Maven repository:

```bash
mvn install
```

## Usage

```java
import club.muimi.Kipbar.Kipbar;
import club.muimi.Kipbar.KipbarOptions;

public class Example {
    public static void main(String[] args) throws InterruptedException {
        KipbarOptions options = KipbarOptions.builder()
                .taskName("Download")
                .progressChar('=')
                .showPercentage(true)
                .showElapsedTime(true)
                .failureMessage("Download failed.")
                .build();

        try (Kipbar bar = new Kipbar(10, options)) {
            bar.run(task -> {
                for (int i = 0; i < bar.getLength(); i++) {
                    Thread.sleep(100);
                    task.step();
                }
            });
        }
    }
}
```

You can also control the lifecycle manually:

```java
Kipbar bar = new Kipbar(5);
bar.start();
bar.step();
bar.update(2);
bar.finish();
bar.close();
```

## API Overview

`KipbarOptions` supports these main settings:

- `progressChar`
- `printDoneMessage`
- `showPercentage`
- `showElapsedTime`
- `output`
- `taskName`
- `failureMessage`

`Kipbar` supports these main operations:

- `start()`
- `step()`
- `update(int n)`
- `finish()`
- `fail()`
- `run(InterruptibleTask task)`
- `getElapsed()`

## Testing

Run the full test suite with:

```bash
mvn test
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
