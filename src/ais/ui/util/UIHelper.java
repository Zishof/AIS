package ais.ui.util;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Vbox;

/**
 * Helper UI reusable — tata letak komponen tombol aksi per baris.
 */
public class UIHelper {

    /**
     * Susun tombol aksi dalam baris-baris. Jika jumlah melebihi maxPerBaris,
     * sisanya turun ke baris berikutnya. Setiap baris rata tengah.
     *
     * @param parent      komponen induk tempat Vbox dipasang (boleh null)
     * @param maxPerBaris batas tombol per baris (3 dianjurkan)
     * @param tombol      daftar komponen tombol
     * @return Vbox hasil penataan
     */
    public static Vbox buatBarisAksi(Component parent, int maxPerBaris,
            List<Component> tombol) {

        Vbox vbox = new Vbox();
        vbox.setAlign("center");
        vbox.setStyle("width:100%;gap:1px;");

        List<Component> copy = new ArrayList<Component>(tombol);

        for (int i = 0; i < copy.size(); i += maxPerBaris) {
            Hbox row = new Hbox();
            row.setSclass("ais-row-action-bar");
            row.setStyle("justify-content:center;");
            for (int j = i; j < Math.min(i + maxPerBaris, copy.size()); j++) {
                copy.get(j).setParent(row);
            }
            row.setParent(vbox);
        }

        if (parent != null) {
            vbox.setParent(parent);
        }
        return vbox;
    }

    /** Overload dengan maxPerBaris default = 3. */
    public static Vbox buatBarisAksi(Component parent, List<Component> tombol) {
        return buatBarisAksi(parent, 3, tombol);
    }
}
