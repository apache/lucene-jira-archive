
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Locale;
import java.io.*;


/**
 * Fetch third party libs.
 */
public class Fetch {
  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.err.println("java " + Fetch.class.getSimpleName() + " deps.txt");
      System.exit(1);
    }

    Path saveTo = Paths.get("./");
    int lineNum = 0;
    for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
      lineNum++; 
      line = line.trim();

      if (line.startsWith("#@saveTo")) {
        saveTo = Paths.get(line.replace("#@saveTo", "").trim());
        saveTo = Files.createDirectories(saveTo);
      } else if (line.startsWith("#") || line.isEmpty()) {
        // Ignore comments.
      } else {
        // This has to be an URL to fetch.
        URI uri = URI.create(line);
        try (InputStream is = uri.toURL().openStream()) {
          String fileName = uri.getPath().replaceAll(".+/", "");
          System.out.println("Fetching: " + fileName);

          Path target = saveTo.resolve(fileName);
          Files.deleteIfExists(target);
          Files.copy(new BufferedInputStream(new FilterInputStream(is) {
            int bytesRead = 0;
            public int read(byte b[], int off, int len) throws IOException {
              int chunk = in.read(b, off, len);
              bytesRead += chunk;
              System.out.print(String.format(Locale.ROOT, "\rBytes fetched: %,d", bytesRead));
              return chunk;
            }

            public int read() throws IOException {
              throw new IOException("BufferedInputStream above?!");
            }
          }, 1024 * 32), target);
          System.out.print("\r");
        }
      }
    }
    
    System.out.println("Done.                          " /* (padding to clear line) */);
    System.exit(0);
  }

  private static void fatal(String message, Object... args) {
    System.err.println(String.format(Locale.ROOT, message, args));
    System.exit(1);
  }
}