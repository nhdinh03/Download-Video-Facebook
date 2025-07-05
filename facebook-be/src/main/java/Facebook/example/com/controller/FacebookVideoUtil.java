package Facebook.example.com.controller;
import java.io.IOException;

public class FacebookVideoUtil {
    public static String downloadVideoUsingYtDlp(String fbUrl) throws IOException, InterruptedException {
        String outputPath = System.getProperty("java.io.tmpdir") + "/" + java.util.UUID.randomUUID() + ".mp4";
        ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-f", "best", "-o", outputPath, fbUrl);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = -1;
        try (java.io.InputStream is = process.getInputStream()) {
            // Đọc hết output để tránh treo process (dù không cần lấy output)
            while (is.read() != -1) {}
            exitCode = process.waitFor();
        } finally {
            process.destroyForcibly();
        }
        if (exitCode == 0) {
            return outputPath;
        } else {
            throw new RuntimeException("yt-dlp failed");
        }
    }
}