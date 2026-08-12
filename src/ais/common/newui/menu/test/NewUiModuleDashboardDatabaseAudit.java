package ais.common.newui.menu.test;

import java.util.List;
import java.util.Arrays;

import ais.common.newui.menu.NewUiModuleDashboardService;
import ais.common.newui.menu.NewUiModuleDashboardService.Dashboard;
import ais.common.newui.menu.NewUiModuleDashboardService.Metric;
import ais.common.newui.menu.NewUiModuleFunctionService;

/** Audit read-only bahwa seluruh dashboard dapat mengevaluasi metric database. */
public final class NewUiModuleDashboardDatabaseAudit {
    private NewUiModuleDashboardDatabaseAudit() { }

    public static void main(String[] args) {
        List<String> modules = args == null || args.length == 0
                ? NewUiModuleFunctionService.moduleKeys() : Arrays.asList(args);
        int metrics = 0, available = 0, status = 0;
        for (int i = 0; i < modules.size(); i++) {
            Dashboard dashboard = NewUiModuleDashboardService.load(modules.get(i), null);
            if (dashboard == null) throw new IllegalStateException("Dashboard hilang: " + modules.get(i));
            List<Metric> values = dashboard.getMetrics();
            metrics += values.size();
            for (int m = 0; m < values.size(); m++) {
                Metric value = values.get(m);
                if (value.isAvailable()) available++;
                else System.out.println("UNAVAILABLE module=" + modules.get(i)
                        + " entity=" + value.getEntityClass());
                if (value.isStatusAvailable()) status++;
            }
        }
        System.out.println("NewUiModuleDashboardDatabaseAudit modules=" + modules.size()
                + " metrics=" + metrics + " available=" + available + " status=" + status);
        if (metrics != modules.size() * 4) {
            throw new IllegalStateException("Kontrak metric dashboard tidak lengkap.");
        }
        System.exit(0);
    }
}
