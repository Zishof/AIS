package ais.action.master.helper;

import java.util.ArrayList;
import java.util.List;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Vlayout;

import ais.action.master.dashboard.admin.DashboardAkademikHtmlCssHelper;
import ais.common.AbsensiTrenCache;
import ais.common.ConstantValues;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.Statusabsensi;

/**
 * Kumpulan utilitas TAMPILAN (UI/UX) bersama untuk modul Kehadiran &amp; Absensi, dipakai ulang oleh
 * tampilan absensi mahasiswa ({@code AbsensiHelper}), siswa ({@code AbsensiSiswaHelper}), dan turunan
 * lain. Tujuannya satu sumber kebenaran untuk gaya kartu presensi modern, lencana status berwarna,
 * dan ringkasan komposisi kehadiran &mdash; sehingga perbaikan tampilan di kemudian hari cukup
 * dilakukan di satu tempat (memudahkan pemeliharaan dan menjaga konsistensi antar jenjang).
 *
 * <p>
 * <b>Mengapa dipisah.</b> Sebelumnya potongan gaya dan badge ditulis terpisah pada tiap kelas
 * absensi sehingga rawan tidak konsisten dan menyulitkan pemeliharaan. Dengan memusatkannya di sini,
 * tampilan absensi semua jenjang (perguruan tinggi maupun sekolah) memakai komponen yang sama:
 * kartu ber-bayangan halus dengan sudut membulat dan efek hover, lencana status berwarna (Hadir
 * hijau, Alpa merah, Izin biru, Sakit oranye, dan abu-abu untuk yang belum diabsen), serta grafik
 * donut komposisi kehadiran. Semua keluaran berupa HTML/CSS murni (tanpa pustaka grafik eksternal)
 * agar ringan saat dirender di dalam komponen {@code Html} ZK dan otomatis responsif di layar mobile
 * maupun desktop.
 * </p>
 *
 * <p>
 * <b>Cara pakai.</b> Tempelkan {@link #gayaKartuPresensi()} satu kali di bagian atas daftar
 * presensi (lewat komponen {@code Html}) agar kelas CSS tersedia; bungkus tiap kartu peserta dengan
 * kelas {@code ais-absn-card}; ganti label status polos dengan {@link #badgeStatus(Statusabsensi)};
 * dan tampilkan {@link #htmlKomposisiKehadiran(Pertemuan, List)} di atas daftar untuk memberi
 * gambaran sekilas berapa peserta yang hadir, izin, sakit, alpa, dan belum diabsen.
 * </p>
 *
 * <p>
 * <b>Keamanan &amp; efisiensi.</b> Seluruh metode bersifat statis dan stateless (thread-safe),
 * tidak membuka sesi basis data, dan menghitung komposisi dari data absensi yang sudah dimuat
 * (memanfaatkan cache {@code ConstantValues}) sehingga tidak menambah kueri. Setiap masukan
 * di-cek-null sehingga aman dipanggil pada kondisi data belum lengkap.
 * </p>
 */
public final class AbsensiUiHelper {

    private AbsensiUiHelper() {
    }

    /**
     * Mengembalikan blok {@code <style>} (sekali tempel) untuk tampilan kartu presensi modern:
     * kartu ber-bayangan halus, sudut membulat, efek hover, lencana status berwarna, dan responsif
     * di layar kecil. Kelas yang disediakan: {@code .ais-absn-card} dan {@code .ais-absn-badge}
     * (varian {@code hadir/alpa/izin/sakit/belum}).
     *
     * @return potongan HTML berisi {@code <style>}.
     */
    public static String gayaKartuPresensi() {
        return "<style>"
                + ".ais-absn-card{border:1px solid #e6e8ee !important;border-radius:14px !important;"
                + "box-shadow:0 2px 10px rgba(16,24,40,.06) !important;margin:8px 0 !important;overflow:hidden;"
                + "transition:box-shadow .18s ease,transform .18s ease;}"
                + ".ais-absn-card:hover{box-shadow:0 6px 18px rgba(16,24,40,.12) !important;transform:translateY(-1px);}"
                + ".ais-absn-badge{display:inline-block;padding:2px 10px;border-radius:999px;font-size:10px;"
                + "font-weight:800;letter-spacing:.3px;color:#fff;white-space:nowrap;}"
                + ".ais-absn-badge.hadir{background:#16a34a;}.ais-absn-badge.alpa{background:#dc2626;}"
                + ".ais-absn-badge.izin{background:#2563eb;}.ais-absn-badge.sakit{background:#d97706;}"
                + ".ais-absn-badge.belum{background:#9ca3af;}"
                // Wadah ringkasan atas (donut + tren): berdampingan di layar lebar, MENUMPUK di mobile.
                + ".ais-absn-ringkas{display:flex;flex-wrap:wrap;gap:12px;align-items:stretch;width:100%;box-sizing:border-box;}"
                + ".ais-absn-ringkas-item{flex:1 1 300px;min-width:260px;box-sizing:border-box;}"
                + ".ais-absn-ringkas-item>.ais-akad-card{margin:0 !important;height:100%;box-sizing:border-box;}"
                // Toolbar presensi: biarkan tombol membungkus (wrap) agar tidak terpotong di layar sempit.
                + ".ais-absn-toolbar,.ais-absn-toolbar .z-toolbar-body{display:flex !important;flex-wrap:wrap !important;"
                + "gap:6px;align-items:center;width:100%;box-sizing:border-box;}"
                + ".ais-absn-toolbar .z-toolbarbutton{flex:0 0 auto;}"
                + "@media (max-width:480px){.ais-absn-card{border-radius:10px !important;}"
                + ".ais-absn-ringkas-item{flex:1 1 100%;min-width:0;}}"
                + "</style>";
    }

    /**
     * Membuat WADAH VERTIKAL (full-width) untuk bagian ringkasan atas daftar presensi
     * (gaya, donut komposisi, tren, dan toolbar aksi). Tujuannya agar komponen-komponen
     * tersebut MENUMPUK ke bawah dan melebar penuh, BUKAN berdesakan di sel-sel horizontal
     * ZK {@code Row} (penyebab tampilan "berantakan" sebelumnya). Tetap responsif: di dalam
     * wadah ini, donut &amp; tren dibungkus flex yang membungkus sendiri di layar mobile.
     *
     * @param parent komponen induk (mis. {@code Row} hasil {@code Common.tampilanScroll1}).
     * @return {@link Vlayout} full-width yang sudah ter-parent; tempat menempelkan isi ringkasan.
     */
    public static Vlayout wadahRingkasanAtas(Component parent) {
        Vlayout v = new Vlayout();
        v.setWidth("100%");
        v.setStyle("width:100%;box-sizing:border-box;");
        if (parent != null) {
            v.setParent(parent);
        }
        return v;
    }

    /**
     * Menggabungkan HTML donut komposisi dan tren kehadiran ke dalam SATU wadah responsif
     * ({@code .ais-absn-ringkas}) sehingga berdampingan di layar lebar dan menumpuk di mobile.
     * Bagian yang kosong/null otomatis dilewati. Mengembalikan string kosong bila keduanya kosong.
     *
     * @param komposisiHtml HTML donut komposisi kehadiran (boleh kosong/null).
     * @param trenHtml      HTML tren kehadiran antar-pertemuan (boleh kosong/null).
     * @return HTML wadah ringkasan, atau string kosong bila tidak ada isi.
     */
    public static String htmlRingkasanGabung(String komposisiHtml, String trenHtml) {
        boolean adaK = komposisiHtml != null && komposisiHtml.trim().length() > 0;
        boolean adaT = trenHtml != null && trenHtml.trim().length() > 0;
        if (!adaK && !adaT) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        sb.append("<div class=\"ais-absn-ringkas\">");
        if (adaK) {
            sb.append("<div class=\"ais-absn-ringkas-item\">").append(komposisiHtml).append("</div>");
        }
        if (adaT) {
            sb.append("<div class=\"ais-absn-ringkas-item\">").append(trenHtml).append("</div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Membuat lencana (badge) HTML berwarna untuk sebuah status kehadiran: Hadir (hijau),
     * Alpa (merah), Izin (biru), Sakit (oranye), selain itu abu-abu (Belum absen). Warna dipetakan
     * dari kode status (M/A/I/S) sehingga konsisten meski nama status dikonfigurasi ulang.
     *
     * @param s status kehadiran; boleh {@code null} (dianggap "Belum absen").
     * @return potongan HTML {@code <span>} badge.
     */
    public static String badgeStatus(Statusabsensi s) {
        String kode = (s == null || s.getKode() == null) ? "" : s.getKode().trim().toUpperCase();
        String cls;
        if ("M".equals(kode)) {
            cls = "hadir";
        } else if ("A".equals(kode)) {
            cls = "alpa";
        } else if ("I".equals(kode)) {
            cls = "izin";
        } else if ("S".equals(kode)) {
            cls = "sakit";
        } else {
            cls = "belum";
        }
        String txt = (s != null && s.getNama() != null && s.getNama().trim().length() > 0) ? s.getNama() : "Belum absen";
        return "<span class='ais-absn-badge " + cls + "'>" + txt + "</span>";
    }

    /**
     * Menghitung KOMPOSISI kehadiran peserta pada satu pertemuan (Hadir/Izin/Sakit/Alpa/Belum)
     * lalu menyajikannya sebagai grafik donut HTML/CSS (lihat
     * {@link DashboardAkademikHtmlCssHelper#donutChart}). Status tiap peserta dibaca dari data
     * absensi pertemuan yang sudah dimuat (memakai cache {@code ConstantValues}), sehingga tidak
     * menambah kueri ke basis data. Warna donut konsisten dengan {@link #badgeStatus(Statusabsensi)}.
     *
     * @param pertemuan pertemuan yang dinilai.
     * @param peserta   daftar peserta (mahasiswa/siswa/asisten) pada pertemuan tersebut.
     * @return potongan HTML grafik donut; string kosong bila tidak ada peserta.
     */
    public static String htmlKomposisiKehadiran(Pertemuan pertemuan, List<? extends GeneralValueObject> peserta) {
        if (pertemuan == null || peserta == null || peserta.isEmpty()) {
            return "";
        }
        int hadir = 0, izin = 0, sakit = 0, alpa = 0, belum = 0;
        for (GeneralValueObject g : peserta) {
            if (g == null) {
                continue;
            }
            Statusabsensi s = (Statusabsensi) ConstantValues.ambil(Statusabsensi.class.getName(),
                    pertemuan.retreiveAbsensiId(g.getId()));
            String kode = (s == null || s.getKode() == null) ? "" : s.getKode().trim().toUpperCase();
            if ("M".equals(kode)) {
                hadir++;
            } else if ("I".equals(kode)) {
                izin++;
            } else if ("S".equals(kode)) {
                sakit++;
            } else if ("A".equals(kode)) {
                alpa++;
            } else {
                belum++;
            }
        }
        List<String> labels = new ArrayList<String>();
        labels.add("Hadir");
        labels.add("Izin");
        labels.add("Sakit");
        labels.add("Alpa");
        labels.add("Belum absen");
        int[] values = { hadir, izin, sakit, alpa, belum };
        String[] colors = { "#16a34a", "#2563eb", "#d97706", "#dc2626", "#9ca3af" };
        return DashboardAkademikHtmlCssHelper.donutChart("Komposisi Kehadiran Pertemuan Ini",
                "Sekilas berapa peserta yang hadir, izin, sakit, alpa, dan belum diabsen pada pertemuan ini.",
                labels, values, colors);
    }

    /**
     * Menyajikan grafik garis TREN KEHADIRAN antar-pertemuan untuk sebuah perkuliahan/kelas,
     * dengan sumber data ber-cache bertingkat ({@link AbsensiTrenCache}) sehingga sangat cepat
     * dibuka berulang kali. Tiap titik adalah persentase kehadiran peserta didik pada satu
     * pertemuan, berurutan dari pertemuan awal hingga akhir; grafik baru ditampilkan bila minimal
     * ada dua pertemuan yang sudah diabsen. Memakai ulang
     * {@link DashboardAkademikHtmlCssHelper#trendLineChart} agar tampilan konsisten dan ringan.
     *
     * @param perkuliahanId id perkuliahan/kelas; boleh {@code null} (mengembalikan string kosong).
     * @return potongan HTML grafik; string kosong bila data belum cukup.
     */
    public static String htmlTrenKehadiran(Long perkuliahanId) {
        if (perkuliahanId == null) {
            return "";
        }
        AbsensiTrenCache.TrenKehadiran t = AbsensiTrenCache.ambil(perkuliahanId);
        if (t == null || t.kosong() || t.labels.size() < 2) {
            return "";
        }
        List<String> names = new ArrayList<String>();
        names.add("% Hadir");
        List<int[]> values = new ArrayList<int[]>();
        values.add(t.persenHadir);
        return DashboardAkademikHtmlCssHelper.trendLineChart("Tren Kehadiran Antar-Pertemuan",
                "Naik-turunnya persentase kehadiran dari pertemuan ke pertemuan, membantu melihat kapan "
                        + "kelas mulai ramai atau sepi.",
                t.labels, names, values);
    }
}
