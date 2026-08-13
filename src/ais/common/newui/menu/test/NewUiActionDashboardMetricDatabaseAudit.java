package ais.common.newui.menu.test;

import java.util.List;
import ais.common.newui.menu.NewUiActionDashboardMetricService;
import ais.common.newui.menu.NewUiActionDashboardMetricService.Metric;

/** Audit read-only metric entity Action terhadap database local. */
public final class NewUiActionDashboardMetricDatabaseAudit {
    private NewUiActionDashboardMetricDatabaseAudit() { }
    public static void main(String[] args) {
        if (args == null || args.length == 0) throw new IllegalArgumentException("source class wajib");
        for (int a = 0; a < args.length; a++) {
            List<Metric> metrics = NewUiActionDashboardMetricService.load(args[a], null);
            int available = 0;
            for (int i = 0; i < metrics.size(); i++) if (metrics.get(i).isAvailable()) available++;
            System.out.println("NewUiActionDashboardMetricDatabaseAudit source=" + args[a]
                    + " metrics=" + metrics.size() + " available=" + available);
            if (metrics.isEmpty() || available != metrics.size()) {
                throw new IllegalStateException("metric Action belum seluruhnya tersedia: " + args[a]);
            }
        }
        System.exit(0);
    }
}
