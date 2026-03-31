package com.example.springclaudeturtorial.phase1.patterns;

import java.util.*;

/**
 * TOPIC: Design Patterns quan trọng cho Spring
 *
 * Chỉ học các pattern Spring sử dụng nội tại hoặc bạn sẽ tự dùng.
 * Singleton / Factory / Builder / Strategy / Decorator / Proxy
 */
public class DesignPatternsDemo {

    // ════════════════════════════════════════════════════════════════════════
    // 1. SINGLETON — 1 instance duy nhất trong toàn ứng dụng
    //    Spring: mọi @Bean mặc định là Singleton trong ApplicationContext
    // ════════════════════════════════════════════════════════════════════════

    static class DatabaseConnectionPool {
        private static volatile DatabaseConnectionPool instance; // volatile: thread-safe visibility
        private final String url;
        private int activeConnections = 0;

        private DatabaseConnectionPool(String url) {
            this.url = url;
            System.out.println("  [Singleton] Pool created for: " + url);
        }

        // Double-checked locking — thread-safe lazy initialization
        public static DatabaseConnectionPool getInstance(String url) {
            if (instance == null) {
                synchronized (DatabaseConnectionPool.class) {
                    if (instance == null) {
                        instance = new DatabaseConnectionPool(url);
                    }
                }
            }
            return instance;
        }

        public void getConnection() {
            activeConnections++;
            System.out.println("  [Singleton] Connection #" + activeConnections + " from " + url);
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // 2. FACTORY — tạo object mà không lộ logic tạo ra ngoài
    //    Spring: @Bean method trong @Configuration là Factory method
    // ════════════════════════════════════════════════════════════════════════

    interface Notification {
        void send(String message);
    }

    static class EmailNotification implements Notification {
        private final String to;
        EmailNotification(String to) { this.to = to; }
        public void send(String message) {
            System.out.println("  [Factory] Email to " + to + ": " + message);
        }
    }

    static class SmsNotification implements Notification {
        private final String phone;
        SmsNotification(String phone) { this.phone = phone; }
        public void send(String message) {
            System.out.println("  [Factory] SMS to " + phone + ": " + message);
        }
    }

    static class PushNotification implements Notification {
        private final String deviceId;
        PushNotification(String deviceId) { this.deviceId = deviceId; }
        public void send(String message) {
            System.out.println("  [Factory] Push to " + deviceId + ": " + message);
        }
    }

    // Factory — caller không biết cách tạo, chỉ biết type
    static class NotificationFactory {
        public static Notification create(String type, String target) {
            return switch (type.toUpperCase()) {
                case "EMAIL" -> new EmailNotification(target);
                case "SMS"   -> new SmsNotification(target);
                case "PUSH"  -> new PushNotification(target);
                default      -> throw new IllegalArgumentException("Unknown type: " + type);
            };
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // 3. BUILDER — tạo object phức tạp step-by-step, tránh telescoping constructor
    //    Spring: dùng nhiều trong configuration (WebClient.builder(), etc.)
    // ════════════════════════════════════════════════════════════════════════

    static class HttpRequest {
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final String body;
        private final int timeoutMs;

        private HttpRequest(Builder builder) {
            this.method    = builder.method;
            this.url       = builder.url;
            this.headers   = Collections.unmodifiableMap(builder.headers);
            this.body      = builder.body;
            this.timeoutMs = builder.timeoutMs;
        }

        @Override
        public String toString() {
            return method + " " + url + " | headers=" + headers
                + " | body=" + body + " | timeout=" + timeoutMs + "ms";
        }

        static class Builder {
            private final String method;
            private final String url;
            private final Map<String, String> headers = new LinkedHashMap<>();
            private String body      = null;
            private int    timeoutMs = 5000; // default

            Builder(String method, String url) {
                this.method = method;
                this.url    = url;
            }

            Builder header(String key, String value) {
                headers.put(key, value);
                return this; // method chaining
            }

            Builder body(String body) {
                this.body = body;
                return this;
            }

            Builder timeout(int ms) {
                this.timeoutMs = ms;
                return this;
            }

            HttpRequest build() {
                if (url == null || url.isBlank()) throw new IllegalStateException("URL required");
                return new HttpRequest(this);
            }
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // 4. STRATEGY — đóng gói algorithm, swap runtime
    //    Spring: inject interface, Spring chọn implementation theo @Profile/@Qualifier
    // ════════════════════════════════════════════════════════════════════════

    interface SortStrategy {
        void sort(int[] arr);
        String name();
    }

    static class BubbleSortStrategy implements SortStrategy {
        public void sort(int[] arr) {
            int[] copy = arr.clone();
            // simplified bubble sort
            for (int i = 0; i < copy.length - 1; i++)
                for (int j = 0; j < copy.length - i - 1; j++)
                    if (copy[j] > copy[j+1]) { int t = copy[j]; copy[j] = copy[j+1]; copy[j+1] = t; }
            System.out.println("  [Strategy] BubbleSort result: " + Arrays.toString(copy));
        }
        public String name() { return "BubbleSort"; }
    }

    static class QuickSortStrategy implements SortStrategy {
        public void sort(int[] arr) {
            int[] copy = arr.clone();
            Arrays.sort(copy); // simplified
            System.out.println("  [Strategy] QuickSort result:  " + Arrays.toString(copy));
        }
        public String name() { return "QuickSort"; }
    }

    static class Sorter {
        private SortStrategy strategy;

        Sorter(SortStrategy strategy) { this.strategy = strategy; }

        void setStrategy(SortStrategy strategy) { this.strategy = strategy; } // swap runtime

        void sort(int[] arr) {
            System.out.println("  Using strategy: " + strategy.name());
            strategy.sort(arr);
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // 5. DECORATOR — thêm behavior mà không sửa class gốc
    //    Spring: Filter chain, Spring Security FilterChain
    // ════════════════════════════════════════════════════════════════════════

    interface DataReader {
        String read(String source);
    }

    // Base implementation
    static class FileDataReader implements DataReader {
        public String read(String source) {
            return "raw_data_from:" + source;
        }
    }

    // Decorator base
    static abstract class DataReaderDecorator implements DataReader {
        protected final DataReader wrapped;
        DataReaderDecorator(DataReader wrapped) { this.wrapped = wrapped; }
    }

    // Decorator 1: Compression
    static class CompressionDecorator extends DataReaderDecorator {
        CompressionDecorator(DataReader wrapped) { super(wrapped); }
        public String read(String source) {
            String data = wrapped.read(source);
            return "[DECOMPRESSED]" + data;
        }
    }

    // Decorator 2: Encryption
    static class EncryptionDecorator extends DataReaderDecorator {
        EncryptionDecorator(DataReader wrapped) { super(wrapped); }
        public String read(String source) {
            String data = wrapped.read(source);
            return "[DECRYPTED]" + data;
        }
    }

    // Decorator 3: Logging
    static class LoggingDecorator extends DataReaderDecorator {
        LoggingDecorator(DataReader wrapped) { super(wrapped); }
        public String read(String source) {
            System.out.println("  [Decorator] Reading from: " + source);
            String data = wrapped.read(source);
            System.out.println("  [Decorator] Read complete");
            return data;
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // 6. PROXY — kiểm soát truy cập vào object thật
    //    Spring: @Transactional, @Cacheable, @PreAuthorize đều dùng Proxy
    // ════════════════════════════════════════════════════════════════════════

    interface UserService {
        String getUser(Long id);
        void saveUser(String name);
    }

    static class UserServiceImpl implements UserService {
        public String getUser(Long id) {
            System.out.println("    [Real] getUser(" + id + ")");
            return "User-" + id;
        }
        public void saveUser(String name) {
            System.out.println("    [Real] saveUser(" + name + ")");
        }
    }

    // Proxy: thêm caching mà không sửa UserServiceImpl
    static class CachingUserServiceProxy implements UserService {
        private final UserService real;
        private final Map<Long, String> cache = new HashMap<>();

        CachingUserServiceProxy(UserService real) { this.real = real; }

        public String getUser(Long id) {
            if (cache.containsKey(id)) {
                System.out.println("    [Proxy] Cache HIT for id=" + id);
                return cache.get(id);
            }
            System.out.println("    [Proxy] Cache MISS — delegating to real service");
            String result = real.getUser(id);
            cache.put(id, result);
            return result;
        }

        public void saveUser(String name) {
            real.saveUser(name);
            // clear cache sau khi save
            cache.clear();
            System.out.println("    [Proxy] Cache cleared after save");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MAIN
    // ════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  DESIGN PATTERNS DEMO");
        System.out.println("═══════════════════════════════");

        // 1. Singleton
        System.out.println("\n[1] Singleton Pattern");
        DatabaseConnectionPool pool1 = DatabaseConnectionPool.getInstance("jdbc:postgresql://prod/db");
        DatabaseConnectionPool pool2 = DatabaseConnectionPool.getInstance("jdbc:postgresql://prod/db");
        System.out.println("  Same instance: " + (pool1 == pool2)); // true
        pool1.getConnection();
        pool2.getConnection(); // dùng cùng pool

        // 2. Factory
        System.out.println("\n[2] Factory Pattern");
        Notification[] notifications = {
            NotificationFactory.create("EMAIL", "hieu@example.com"),
            NotificationFactory.create("SMS",   "0901234567"),
            NotificationFactory.create("PUSH",  "device-abc123")
        };
        for (Notification n : notifications) n.send("Your order is confirmed!");

        // 3. Builder
        System.out.println("\n[3] Builder Pattern");
        HttpRequest request = new HttpRequest.Builder("POST", "https://api.example.com/orders")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer token123")
            .body("{\"product\":\"Laptop\",\"qty\":1}")
            .timeout(3000)
            .build();
        System.out.println("  Request: " + request);

        HttpRequest simpleGet = new HttpRequest.Builder("GET", "https://api.example.com/users/1")
            .build(); // dùng default timeout 5000ms
        System.out.println("  Simple:  " + simpleGet);

        // 4. Strategy
        System.out.println("\n[4] Strategy Pattern");
        int[] data = {5, 2, 8, 1, 9, 3};
        Sorter sorter = new Sorter(new BubbleSortStrategy());
        sorter.sort(data);
        sorter.setStrategy(new QuickSortStrategy()); // swap strategy
        sorter.sort(data);

        // 5. Decorator
        System.out.println("\n[5] Decorator Pattern");
        // Wrap theo thứ tự: file → encrypt → compress → log
        DataReader reader = new LoggingDecorator(
                                new CompressionDecorator(
                                    new EncryptionDecorator(
                                        new FileDataReader())));
        String data2 = reader.read("secret-file.bin");
        System.out.println("  Final data: " + data2);

        // 6. Proxy
        System.out.println("\n[6] Proxy Pattern (@Transactional/@Cacheable tương tự thế này)");
        UserService service = new CachingUserServiceProxy(new UserServiceImpl());
        System.out.println("  Call 1: " + service.getUser(1L)); // cache miss
        System.out.println("  Call 2: " + service.getUser(1L)); // cache hit
        System.out.println("  Call 3: " + service.getUser(1L)); // cache hit
        service.saveUser("Hieu");                               // clear cache
        System.out.println("  Call 4: " + service.getUser(1L)); // cache miss lại

        System.out.println("\n── Key Takeaways ──");
        System.out.println("  Singleton:  Spring Bean scope mặc định → 1 instance/ApplicationContext");
        System.out.println("  Factory:    @Bean trong @Configuration → Spring tạo bean theo factory method");
        System.out.println("  Builder:    WebClient.builder(), MockMvcRequestBuilders trong Spring tests");
        System.out.println("  Strategy:   @Qualifier chọn implementation → swap không sửa code");
        System.out.println("  Decorator:  Spring Security FilterChain — filter bọc filter bọc controller");
        System.out.println("  Proxy:      @Transactional/@Cacheable — Spring bọc Bean trong CGLIB Proxy");
    }
}
