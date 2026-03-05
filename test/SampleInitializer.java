import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads test/sample-data.txt and injects each row into the running stake API:
 * 1) GET /&lt;customerId&gt;/session to obtain (and cache) session key per customer
 * 2) POST /&lt;betOfferId&gt;/stake?sessionkey=&lt;key&gt; with body = stake
 *
 * Usage: run from project root (so that test/sample-data.txt exists).
 *   javac -d classes test/SampleInitializer.java
 *   java -cp classes SampleInitializer [baseUrl]
 *
 * Default baseUrl: http://localhost:8001
 */
public class SampleInitializer {

    private final String baseUrl;
    private final Map<Integer, String> sessionByCustomer = new HashMap<>();

    public SampleInitializer(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public static void main(String[] args) throws Exception {
        String base = args.length > 0 ? args[0] : "http://localhost:8001";
        Path dataFile = Paths.get("test", "sample-data.txt");
        if (!Files.isRegularFile(dataFile)) {
            System.err.println("Data file not found: " + dataFile.toAbsolutePath());
            System.err.println("Run from project root.");
            System.exit(1);
        }
        SampleInitializer init = new SampleInitializer(base);
        int count = init.load(dataFile);
        System.out.println("Done. Injected " + count + " stakes.");
    }

    public int load(Path dataFile) throws Exception {
        int count = 0;
        try (BufferedReader r = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            String header = r.readLine();
            if (header == null || !header.contains("betOfferId")) {
                System.err.println("Unexpected header: " + header);
                return 0;
            }
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if(line.startsWith("#")){
                    System.err.println("Skip disabled line: " + line);
                    continue;
                }
                
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    System.err.println("Skip invalid line: " + line);
                    continue;
                }
                int betOfferId, customerId, stake;
                try {
                    betOfferId = Integer.parseInt(parts[0].trim());
                    customerId = Integer.parseInt(parts[1].trim());
                    stake = Integer.parseInt(parts[2].trim());
                } catch (NumberFormatException e) {
                    continue; // skip header or malformed line
                }
                String sessionKey = getOrCreateSession(customerId);
                postStake(betOfferId, sessionKey, stake);
                count++;
                if (count % 100 == 0) {
                    System.out.println("Injected " + count + " ...");
                }
            }
        }
        return count;
    }

    private String getOrCreateSession(int customerId) throws Exception {
        String cached = sessionByCustomer.get(customerId);
        if (cached != null) return cached;
        URL url = new URL(baseUrl + "/" + customerId + "/session");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        int code = conn.getResponseCode();
        if (code != 200) {
            throw new RuntimeException("GET session failed: " + code + " for customer " + customerId);
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String key = r.readLine();
            if (key != null) key = key.trim();
            if (key == null || key.isEmpty()) {
                throw new RuntimeException("Empty session key for customer " + customerId);
            }
            sessionByCustomer.put(customerId, key);
            return key;
        }
    }

    private void postStake(int betOfferId, String sessionKey, int stake) throws Exception {
        URL url = new URL(baseUrl + "/" + betOfferId + "/stake?sessionkey=" + sessionKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "text/plain; charset=UTF-8");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        byte[] body = String.valueOf(stake).getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body);
        }
        int code = conn.getResponseCode();
        if (code != 200) {
            String msg = "POST stake failed: " + code + " for offer " + betOfferId + " stake " + stake;
            try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line = err.readLine();
                if (line != null) msg += " " + line;
            }
            throw new RuntimeException(msg);
        }
    }
}
