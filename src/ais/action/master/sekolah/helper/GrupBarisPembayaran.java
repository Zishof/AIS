package ais.action.master.sekolah.helper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Group;
import org.zkoss.zul.Rows;
import ais.database.model.sekolah.GrupItemBiayaSekolah;
import ais.database.model.sekolah.Tagihan;

/** Mengelompokkan baris yang sudah lolos filter tanpa membuat ulang kontrol pembayaran. */
final class GrupBarisPembayaran {
    private GrupBarisPembayaran() { }

    static void terapkan(Rows rows) {
        Map<Object, Group> headers = new LinkedHashMap<Object, Group>();
        Map<Object, List<Component>> groups = new LinkedHashMap<Object, List<Component>>();
        Group pengaturan = null;
        List<Component> original = new ArrayList<Component>(rows.getChildren());
        for (Component component : original) {
            if (Boolean.TRUE.equals(component.getAttribute("kepalaGrupItem"))) continue;
            if (component instanceof Group) {
                pengaturan = (Group) component;
                continue;
            }
            Object data = component.getAttribute("tagihan");
            if (!(data instanceof Tagihan) || pengaturan == null) {
                throw new IllegalArgumentException("Pengelompokan hanya untuk baris tagihan dan kepala pengaturan");
            }
            Tagihan tagihan = (Tagihan) data;
            GrupItemBiayaSekolah grup = tagihan.getItemBiayaSekolah() == null ? null
                    : tagihan.getItemBiayaSekolah().getGrupItemBiayaSekolah();
            boolean punyaGrup = grup != null && grup.getId() != null;
            Object key = punyaGrup ? grup.getId() : pengaturan;
            if (!groups.containsKey(key)) {
                Group header = pengaturan;
                if (punyaGrup) {
                    header = new Group(grup.getLabelTampilan());
                    header.setStyle("background:#0f4c5c;color:white;font-weight:bold;");
                }
                headers.put(key, header);
                groups.put(key, new ArrayList<Component>());
            }
            groups.get(key).add(component);
        }
        // Identitas baris, checkbox, nilai input dan listener tetap dipertahankan.
        for (Component component : original) component.detach();
        for (Map.Entry<Object, List<Component>> entry : groups.entrySet()) {
            rows.appendChild(headers.get(entry.getKey()));
            for (Component component : entry.getValue()) rows.appendChild(component);
        }
    }
}
