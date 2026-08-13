package ais.common.newui.menu.test;

import java.util.ArrayList;
import java.util.List;

import ais.common.newui.menu.NewUiActionDashboardNavigationService;
import ais.common.newui.menu.NewUiActionDashboardNavigationService.Entry;
import ais.common.newui.menu.NewUiHybridMenuNode;

public final class NewUiActionDashboardNavigationServiceSelfTest {
    private NewUiActionDashboardNavigationServiceSelfTest() { }

    public static void main(String[] args) {
        String koperasi = "ais.action.master.koperasi.DashboardKoperasiAction";
        check(NewUiActionDashboardNavigationService.hasDefinition(koperasi), "adapter koperasi");
        check(NewUiActionDashboardNavigationService.definitionLabels(koperasi).size() == 8,
                "delapan tab koperasi existing");
        check(NewUiActionDashboardNavigationService.definitionLabels(
                "ais.action.master.dashboard.admin.DashboardKegiatanKedosenanAdmin").size() == 4,
                "empat tab dosen existing");
        check(NewUiActionDashboardNavigationService.definitionLabels(
                "ais.action.master.dashboard.sekolah.DashboardKegiatanKesiswaanAdmin").size() == 4,
                "empat tab siswa existing");

        NewUiHybridMenuNode node = new NewUiHybridMenuNode();
        node.setMenuId(Long.valueOf(91L)); node.setLabel("Anggaran Kas Koperasi");
        node.setVisible(true);
        node.setFullPath("Koperasi / Anggaran Kas Koperasi");
        List<NewUiHybridMenuNode> nodes = new ArrayList<NewUiHybridMenuNode>(); nodes.add(node);
        List<Entry> entries = NewUiActionDashboardNavigationService.build(koperasi, nodes);
        boolean matched = false;
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).isAvailable()
                && "Anggaran Kas".equals(entries.get(i).getLabel())) matched = true;
        check(matched, "route diselesaikan dari menu authorized");
        System.out.println("NewUiActionDashboardNavigationServiceSelfTest OK koperasi=" + entries.size());
    }

    private static void check(boolean value, String message) {
        if (!value) throw new IllegalStateException(message);
    }
}
