package ais.common.test;

import java.lang.reflect.Method;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Group;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import ais.database.model.sekolah.GrupItemBiayaSekolah;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.Tagihan;

/** Offline ZK component test; no database or payment writes. */
public final class GrupBarisPembayaranSelfTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static Row row(Rows rows, final Long groupId) {
        final GrupItemBiayaSekolah group = new GrupItemBiayaSekolah() {
            public String getLabelTampilan() { return "Nama sama"; }
        };
        group.setId(groupId);
        final ItemBiayaSekolah item = new ItemBiayaSekolah() {
            public GrupItemBiayaSekolah getGrupItemBiayaSekolah() { return groupId == null ? null : group; }
        };
        Tagihan tagihan = new Tagihan() {
            public ItemBiayaSekolah getItemBiayaSekolah() { return item; }
        };
        Row row = new Row();
        row.setAttribute("tagihan", tagihan);
        Checkbox checkbox = new Checkbox("Item");
        checkbox.setChecked(true);
        row.appendChild(checkbox);
        rows.appendChild(row);
        return row;
    }
    public static void main(String[] args) throws Exception {
        Rows rows = new Rows();
        Group pb1 = new Group("Pengaturan 1");
        rows.appendChild(pb1);
        Row a = row(rows, 10L);
        Row ungrouped = row(rows, null);
        rows.appendChild(new Group("Pengaturan 2"));
        Row b = row(rows, 20L);
        Row c = row(rows, 10L);
        Method method = Class.forName("ais.action.master.sekolah.helper.GrupBarisPembayaran")
                .getDeclaredMethod("terapkan", Rows.class);
        method.setAccessible(true);
        method.invoke(null, rows);
        check(rows.getChildren().size() == 7, "Four bills and three headers, no duplicates");
        check(rows.getChildren().get(1) == a && rows.getChildren().get(2) == c,
                "Same group across settings must be adjacent");
        check(rows.getChildren().get(3) == pb1 && rows.getChildren().get(4) == ungrouped,
                "Ungrouped bill retains original setting header");
        check(rows.getChildren().get(6) == b, "Same labels with different IDs remain separate");
        check(((Checkbox) a.getFirstChild()).isChecked(), "Selection must survive regrouping");
        check(a.getAttribute("tagihan") != null, "Original bill reference must survive");
        ((Group) rows.getFirstChild()).setOpen(false);
        check(a.getParent() == rows && c.getParent() == rows, "Collapse must not remove payment rows");
        method.invoke(null, new Rows());
        System.out.println("PASS GrupBarisPembayaranSelfTest");
    }
}
