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

    // Prediction confidence tracking
    private final LongAdder totalConfidence = new LongAdder(); // Sum of confidence scores
    private final AtomicLong confidencePredictions = new AtomicLong(0);
    private final AtomicLong lowConfidencePredictions = new AtomicLong(0); // confidence < 0.7

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
     * Record prediction confidence score
     * 
     * @param confidence - confidence score from 0.0 to 1.0
     */
    public void recordPredictionConfidence(double confidence) {
        if (confidence < 0.0 || confidence > 1.0)
            return;

        // Store as integer (multiply by 1000) to use LongAdder
        totalConfidence.add((long) (confidence * 1000));
        confidencePredictions.incrementAndGet();

        // Track low confidence predictions (< 70%)
        if (confidence < 0.7) {
            lowConfidencePredictions.incrementAndGet();
        }
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
        if (reports > 0) {
            avgSeconds = (double) totalTimeOnSiteMs.sum() / 1000.0 / reports;
        }
        m.append(String.format("time_on_site_seconds %.3f%n%n", avgSeconds));

        // 4) Prediction confidence metrics
        m.append("# HELP prediction_confidence_avg Average prediction confidence score (0-1).\n");
        m.append("# TYPE prediction_confidence_avg gauge\n");

        long confCount = confidencePredictions.get();
        double avgConf = 0.0;
        if (confCount > 0) {
            avgConf = (double) totalConfidence.sum() / 1000.0 / confCount;
        }
        m.append(String.format("prediction_confidence_avg %.3f%n%n", avgConf));

        m.append("# HELP low_confidence_predictions_total Number of predictions with confidence < 0.7.\n");
        m.append("# TYPE low_confidence_predictions_total counter\n");
        m.append(String.format("low_confidence_predictions_total %d%n%n", lowConfidencePredictions.get()));

        // 5) Page load time histogram (with labels)
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
