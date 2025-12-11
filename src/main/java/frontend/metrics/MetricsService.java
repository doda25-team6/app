package frontend.metrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    // navigation counts
    private final AtomicLong indexRequests = new AtomicLong(0);   // GET /
    private final AtomicLong smsPageRequests = new AtomicLong(0); // GET /sms or /sms/

    // button clicks → click rate
    private final AtomicLong buttonClicks = new AtomicLong(0);    // POST /sms

    // time on site (reported by frontend)
    private final LongAdder totalTimeOnSiteMs = new LongAdder();
    private final AtomicLong timeOnSiteReports = new AtomicLong(0);

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
        if (timeMillis < 0) return;
        totalTimeOnSiteMs.add(timeMillis);
        timeOnSiteReports.incrementAndGet();
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
        m.append(String.format("time_on_site_seconds total %.3f%n", totalSeconds));

        return m.toString();
    }
}
