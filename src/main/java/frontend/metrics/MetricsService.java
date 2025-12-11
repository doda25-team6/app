package frontend.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    // navigation counts
    private final AtomicLong indexRequests = new AtomicLong(0); // GET /
    private final AtomicLong smsPageRequests = new AtomicLong(0); // GET /sms or /sms/

    // button clicks → click rate
    private final AtomicLong buttonClicks = new AtomicLong(0); // POST /sms

    // time on site (reported by frontend)
    private final LongAdder totalTimeOnSiteMs = new LongAdder();
    private final AtomicLong timeOnSiteReports = new AtomicLong(0);

    // Histogram: page load time buckets in seconds
    // Buckets: 0.05, 0.1, 0.25, 0.5, 1.0, +Inf
    private static final double[] BUCKETS = { 0.05, 0.1, 0.25, 0.5, 1.0 };

    // Map of page path -> bucket counts
    private final Map<String, long[]> pageLoadBuckets = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> pageLoadSum = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> pageLoadCount = new ConcurrentHashMap<>();

    // ---- record methods ----
    public void recordIndexVisit() {
        indexRequests.incrementAndGet();
    }

    public void recordSmsPageVisit() {
        smsPageRequests.incrementAndGet();
    }

    public void recordButtonClick() {
        buttonClicks.incrementAndGet();
    }

    /**
     * timeMillis: time spent on site by a user (frontend should send this)
     */
    public void recordTimeOnSite(long timeMillis) {
        if (timeMillis < 0)
            return;
        totalTimeOnSiteMs.add(timeMillis);
        timeOnSiteReports.incrementAndGet();
    }

    /**
     * Record page load time for histogram
     * 
     * @param durationSeconds - page load time in seconds
     * @param page            - page path like "/" or "/sms"
     */
    public void recordPageLoad(double durationSeconds, String page) {
        // Initialize if not exists
        pageLoadBuckets.putIfAbsent(page, new long[BUCKETS.length + 1]);
        pageLoadSum.putIfAbsent(page, new LongAdder());
        pageLoadCount.putIfAbsent(page, new AtomicLong(0));

        // Update sum and count
        pageLoadSum.get(page).add((long) (durationSeconds * 1000));
        pageLoadCount.get(page).incrementAndGet();

        // Update buckets
        long[] buckets = pageLoadBuckets.get(page);
        for (int i = 0; i < BUCKETS.length; i++) {
            if (durationSeconds <= BUCKETS[i]) {
                buckets[i]++;
            }
        }
        buckets[BUCKETS.length]++; // +Inf bucket (total count)
    }

    // ---- build Prometheus metrics text ----
    public String buildMetrics() {
        StringBuilder m = new StringBuilder();

        // 1) Click rate (counter)
        m.append("# HELP click_rate_total Total number of button clicks (predictions requested).\n");
        m.append("# TYPE click_rate_total counter\n");
        m.append(String.format("click_rate_total %d%n%n", buttonClicks.get()));

        // 2) Navigation paths (counter with labels)
        m.append("# HELP navigation_requests_total Number of page requests, by path.\n");
        m.append("# TYPE navigation_requests_total counter\n");
        m.append(String.format("navigation_requests_total{page=\"/\"} %d%n", indexRequests.get()));
        m.append(String.format("navigation_requests_total{page=\"/sms\"} %d%n%n", smsPageRequests.get()));

        // 3) Time on site (average gauge)
        m.append("# HELP time_on_site_seconds Average reported time on site in seconds.\n");
        m.append("# TYPE time_on_site_seconds gauge\n");

        long reports = timeOnSiteReports.get();
        double avgSeconds = 0.0;
        double totalSeconds = totalTimeOnSiteMs.sum() / 1000.0;
        if (reports > 0) {
            avgSeconds = totalSeconds / reports;
        }
        m.append(String.format("time_on_site_seconds avg %.3f%n", avgSeconds));
        m.append(String.format("time_on_site_seconds total %.3f%n%n", totalSeconds));

        // 4) Page load time histogram (with labels)
        m.append("# HELP page_load_seconds Page load time distribution.\n");
        m.append("# TYPE page_load_seconds histogram\n");

        for (Map.Entry<String, long[]> entry : pageLoadBuckets.entrySet()) {
            String page = entry.getKey();
            long[] buckets = entry.getValue();

            // Output each bucket
            for (int i = 0; i < BUCKETS.length; i++) {
                m.append(String.format(
                        "page_load_seconds_bucket{page=\"%s\",le=\"%.2f\"} %d%n",
                        page, BUCKETS[i], buckets[i]));
            }
            // +Inf bucket
            m.append(String.format(
                    "page_load_seconds_bucket{page=\"%s\",le=\"+Inf\"} %d%n",
                    page, buckets[BUCKETS.length]));

            // Sum and count
            long sum = pageLoadSum.get(page).sum();
            long count = pageLoadCount.get(page).get();
            m.append(String.format(
                    "page_load_seconds_sum{page=\"%s\"} %.3f%n",
                    page, sum / 1000.0));
            m.append(String.format(
                    "page_load_seconds_count{page=\"%s\"} %d%n",
                    page, count));
        }

        return m.toString();
    }
}
