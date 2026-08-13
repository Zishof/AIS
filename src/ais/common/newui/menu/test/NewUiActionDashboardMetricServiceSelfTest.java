package ais.common.newui.menu.test;

import java.util.List;
import ais.common.newui.menu.NewUiActionDashboardMetricService;

/** Audit bahwa entity dashboard diambil dari Action existing, bukan daftar tebakan. */
public final class NewUiActionDashboardMetricServiceSelfTest {
    private NewUiActionDashboardMetricServiceSelfTest() { }
    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
    public static void main(String[] args) {
        List<String> entities = NewUiActionDashboardMetricService.entityClassNames(
                "ais.action.master.dashboard.sirs.DashboardSirsKomprehensif");
        check(entities.contains("ais.database.model.sirs.Pendaftaran"), "Pendaftaran tidak terdeteksi");
        check(entities.contains("ais.database.model.sirs.TempatTidur"), "TempatTidur tidak terdeteksi");
        check(entities.contains("ais.database.model.sirs.Pembayaran"), "Pembayaran tidak terdeteksi");
        check(NewUiActionDashboardMetricService.entityClassNames("tidak.ada.Action").isEmpty(),
                "class tidak dikenal harus fail-closed");
        System.out.println("NewUiActionDashboardMetricServiceSelfTest OK entities=" + entities.size());
    }
}
