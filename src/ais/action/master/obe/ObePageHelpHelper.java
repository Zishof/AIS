package ais.action.master.obe;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Div;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;

/** Petunjuk ringkas dan seragam untuk seluruh halaman pada modul OBE. */
public final class ObePageHelpHelper {

    private ObePageHelpHelper() {
    }

    public static void pasangHalamanCrud(Component root, Class<?> actionClass) {
        if (root == null || actionClass == null || actionClass == CapaianLulusanAction.class) return;
        String[] info = informasiCrud(actionClass);
        if (info == null) return;
        Component portal = cariSclass(root, "ais-crud-portal");
        pasangDiAwal(portal == null ? root : portal, info[0], info[1]);
    }

    public static Div pasangPadaNorth(North north, String judul, String penjelasan) {
        if (north == null) return null;
        // Filter matriks lama memakai North 72px. Tambahkan ruang untuk petunjuk.
        north.setHeight("142px");
        Div wrapper = new Div();
        wrapper.setWidth("100%");
        wrapper.setHeight("100%");
        wrapper.setStyle("overflow:hidden;box-sizing:border-box;");
        wrapper.setParent(north);
        pasangDiAwal(wrapper, judul, penjelasan);
        return wrapper;
    }

    public static Div pasangDiAwal(Component parent, String judul, String penjelasan) {
        if (parent == null) return null;
        Div info = buat(judul, penjelasan);
        Component pertama = parent.getFirstChild();
        parent.insertBefore(info, pertama);
        return info;
    }

    public static Div buat(String judul, String penjelasan) {
        Div info = new Div();
        info.setSclass("ais-obe-page-help");
        info.setStyle("margin:8px 10px;padding:10px 14px;border-left:4px solid #2563eb;"
                + "background:#eff6ff;border-radius:6px;box-sizing:border-box;color:#1e3a5f;");

        Label title = new Label(judul);
        title.setStyle("display:block;font-weight:bold;margin-bottom:3px;color:#1d4ed8;");
        title.setParent(info);

        Label description = new Label(penjelasan);
        description.setStyle("display:block;white-space:normal;line-height:1.45;");
        description.setParent(info);
        return info;
    }

    private static String[] informasiCrud(Class<?> cls) {
        if (cls == BahanKajianAction.class) return info("Bahan Kajian",
                "Daftar pokok ilmu atau materi yang digunakan untuk membentuk CPL dan CPMK. Isi kode, nama bahan kajian, prodi, serta referensinya.");
        if (cls == CapaianPembelajaranLulusanAction.class) return info("CPMK dan Sub-CPMK",
                "Daftar kemampuan yang harus dicapai mahasiswa pada mata kuliah. Lengkapi CPMK, Sub-CPMK, bobot, dan batas minimal ketercapaiannya.");
        if (cls == ProfilLulusanAction.class) return info("Profil Lulusan",
                "Gambaran peran atau karakter lulusan setelah menyelesaikan studi. Profil ini menjadi tujuan yang didukung oleh beberapa CPL.");
        if (cls == ProfesiLulusanAction.class) return info("Profesi Lulusan",
                "Daftar profesi atau bidang kerja yang dapat dijalani lulusan. Data ini dapat dipilih saat menyusun Profil Lulusan.");
        if (cls == ReferensiLulusanAction.class) return info("Referensi OBE",
                "Daftar dokumen acuan penyusunan kurikulum dan capaian, misalnya SN-Dikti, asosiasi profesi, visi-misi, atau kebutuhan pemangku kepentingan.");
        if (cls == KategoriCplAction.class) return info("Kategori CPL",
                "Kelompokkan CPL menjadi S (Sikap), P (Pengetahuan), KU (Keterampilan Umum), dan KK (Keterampilan Khusus). Kategori bawaan tersedia otomatis.");
        return null;
    }

    private static String[] info(String judul, String penjelasan) {
        return new String[] { judul, penjelasan };
    }

    private static Component cariSclass(Component root, String sclass) {
        if (root == null) return null;
        if (root instanceof org.zkoss.zk.ui.HtmlBasedComponent) {
            String value = ((org.zkoss.zk.ui.HtmlBasedComponent) root).getSclass();
            if (value != null && value.indexOf(sclass) >= 0) return root;
        }
        for (Object child : root.getChildren()) {
            if (child instanceof Component) {
                Component found = cariSclass((Component) child, sclass);
                if (found != null) return found;
            }
        }
        return null;
    }
}
