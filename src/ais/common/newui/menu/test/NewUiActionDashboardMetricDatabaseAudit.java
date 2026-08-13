package ais.common.newui.menu.test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ais.common.newui.menu.NewUiActionDashboardMetricService;
import ais.common.newui.menu.NewUiActionDashboardMetricService.Metric;
import ais.common.newui.menu.NewUiModuleFunctionService;

/** Audit read-only metric entity Action terhadap database local. */
public final class NewUiActionDashboardMetricDatabaseAudit {
    private NewUiActionDashboardMetricDatabaseAudit() { }
    public static void main(String[] args) {
        List<String> sources = new ArrayList<String>();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) sources.add(args[i]);
        } else {
            Set<String> unique = new LinkedHashSet<String>();
            List<String> modules = NewUiModuleFunctionService.moduleKeys();
            for (int i = 0; i < modules.size(); i++) {
                unique.addAll(NewUiModuleFunctionService.definitionSourceClasses(modules.get(i)));
            }
            sources.addAll(unique);
        }
        int metricCount = 0, availableCount = 0, adaptedCount = 0;
        for (int a = 0; a < sources.size(); a++) {
            String source = sources.get(a);
            List<Metric> metrics = NewUiActionDashboardMetricService.load(source, null);
            int available = 0;
            for (int i = 0; i < metrics.size(); i++) if (metrics.get(i).isAvailable()) available++;
            boolean adapted = metrics.isEmpty()
                    && NewUiActionDashboardMetricService.hasNativeAdapter(source);
            if (adapted) adaptedCount++;
            metricCount += metrics.size(); availableCount += available;
            System.out.println("NewUiActionDashboardMetricDatabaseAudit source=" + source
                    + " metrics=" + metrics.size() + " available=" + available
                    + " adapted=" + adapted);
            if ((!adapted && metrics.isEmpty()) || available != metrics.size()) {
                throw new IllegalStateException("metric Action belum seluruhnya tersedia: " + source);
            }
        }
        System.out.println("NewUiActionDashboardMetricDatabaseAudit OK sources=" + sources.size()
                + " metrics=" + metricCount + " available=" + availableCount
                + " adapted=" + adaptedCount);
        System.exit(0);
    }
}
