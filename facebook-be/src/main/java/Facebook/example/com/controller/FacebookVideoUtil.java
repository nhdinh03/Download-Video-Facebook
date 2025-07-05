package Facebook.example.com.controller;
import java.io.IOException;

public class FacebookVideoUtil {
    public static String downloadVideoUsingYtDlp(String fbUrl) throws IOException, InterruptedException {
        String outputPath = System.getProperty("java.io.tmpdir") + "/" + java.util.UUID.randomUUID() + ".mp4";
        ProcessBuilder pb = new ProcessBuilder("yt-dlp", "-f", "best", "-o", outputPath, fbUrl);
        pb.redirectErrorStream(true);
        Process process = null;
        int exitCode = -1;
        StringBuilder output = new StringBuilder();
        try {
            process = pb.start();
            try (java.io.InputStream is = process.getInputStream()) {
                int b;
                while ((b = is.read()) != -1) {
                    output.append((char) b);
                }
            }
            exitCode = process.waitFor();
        } finally {
            if (process != null) process.destroyForcibly();
        }
        if (exitCode == 0) {
            return outputPath;
        } else {
            StringBuilder debugInfo = new StringBuilder();
            debugInfo.append("yt-dlp failed.\n");
            debugInfo.append("Mã thoát: ").append(exitCode).append("\n");
            debugInfo.append("Output: ").append(output.toString()).append("\n");
            debugInfo.append("PATH: ").append(System.getenv("PATH")).append("\n");
            debugInfo.append("Command: yt-dlp -f best -o ").append(outputPath).append(" ").append(fbUrl).append("\n");
            throw new RuntimeException(debugInfo.toString());
        }
    }
}