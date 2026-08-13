package ais.common.newui.menu.test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ais.common.newui.menu.NewUiActionDashboardMetricService;
import ais.common.newui.menu.NewUiModuleFunctionService;

/** Coverage seluruh source Action toolbar tanpa membuka database. */
public final class NewUiActionDashboardMetricCoverageAudit {
    private NewUiActionDashboardMetricCoverageAudit() { }
    public static void main(String[] args) throws Exception {
        Set<String> sources = new LinkedHashSet<String>();
        List<String> modules = NewUiModuleFunctionService.moduleKeys();
        for (int i = 0; i < modules.size(); i++) {
            sources.addAll(NewUiModuleFunctionService.definitionSourceClasses(modules.get(i)));
        }
        int withEntities = 0, entityReferences = 0, withoutEntities = 0;
        for (String source : sources) {
            Class.forName(source, false, NewUiActionDashboardMetricCoverageAudit.class.getClassLoader());
            List<String> entities = NewUiActionDashboardMetricService.entityClassNames(source);
            if (entities.isEmpty()) { withoutEntities++; System.out.println("NO_DIRECT_ENTITY " + source); }
            else { withEntities++; entityReferences += entities.size(); }
        }
        System.out.println("NewUiActionDashboardMetricCoverageAudit sources=" + sources.size()
                + " withEntities=" + withEntities + " withoutEntities=" + withoutEntities
                + " entityReferences=" + entityReferences);
        if (sources.size() < 75) throw new IllegalStateException("source Action belum lengkap");
    }
}
