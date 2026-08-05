package ais.action.master.dashboard.admin;

/**
 * Perangkat (kit) TAMPILAN KARTU RINGKASAN yang dapat dipakai ulang oleh dashboard mana pun untuk
 * menyajikan angka-angka kunci dengan gaya kartu modern &mdash; persis seperti dashboard e-Learning
 * yang sudah disukai: kartu ber-bayangan halus dengan garis aksen di atas, angka besar, bilah
 * kemajuan (progress bar), kelompok statistik kecil (pill), bilah komposisi bertumpuk, dan tata
 * letak responsif yang rapi di layar mobile maupun desktop. Semua keluaran berupa HTML/CSS murni
 * (tanpa pustaka grafik eksternal seperti JFreeChart), sehingga ringan saat dirender di dalam
 * komponen {@code Html} ZK dan otomatis menyesuaikan diri ke mode terang/gelap pemanggil.
 *
 * <p>
 * <b>Mengapa dipisahkan menjadi kit.</b> Sebelumnya gaya kartu ini hanya ada di dalam helper
 * dashboard e-Learning sehingga dashboard lain tidak bisa ikut tampil serupa tanpa menyalin kode.
 * Dengan memusatkan seluruh "bahan bangunan" tampilan di satu tempat, dashboard apa pun &mdash;
 * absensi, keuangan, kepegawaian, profil kampus, dan seterusnya &mdash; cukup memanggil metode di
 * sini dengan DATA-NYA SENDIRI (yang sudah ada) untuk mendapatkan tampilan yang konsisten dan
 * modern. Bila suatu saat desain ingin diperbarui (warna, sudut, bayangan, kerapatan), cukup ubah
 * di kelas ini sekali dan semua dashboard ikut berubah &mdash; memudahkan pemeliharaan jangka
 * panjang.
 * </p>
 *
 * <p>
 * <b>Cara memakai.</b> Susun satu blok ringkasan dengan urutan: (1) {@link #bukaPanel(String, String)}
 * untuk judul + kalimat penjelas ramah orang awam beserta gaya CSS dan pembuka kisi kartu;
 * (2) sejumlah kartu &mdash; {@link #kartuProgres} untuk perbandingan tercapai/target lengkap dengan
 * bilah kemajuan, {@link #kartuAngka} untuk beberapa angka sejenis (mis. Materi/Audio/Video),
 * {@link #kartuBreakdown} untuk rincian berwarna per kategori (mis. Hadir/Alpa/Izin/Sakit), dan
 * {@link #kartuTotal} untuk satu angka besar; (3) {@link #tutupGrid()} untuk menutup kisi;
 * (4) opsional {@link #seksiBar(String, String)} berisi satu atau lebih
 * {@link #barKomposisi(String, String[], int[], String[])} untuk membandingkan proporsi secara
 * visual; lalu (5) {@link #tutupPanel()}. Seluruh teks angka diformat ribuan dengan titik (gaya
 * Indonesia) dan seluruh teks bebas di-escape agar aman dari karakter HTML.
 * </p>
 *
 * <p>
 * <b>Deskripsi panel.</b> Setiap kartu menerima satu kalimat deskripsi singkat berbahasa sehari-hari
 * &mdash; tanpa istilah teknis dan tanpa frasa berpola "Panel ini ..." &mdash; agar pengguna yang
 * sama sekali tidak memahami dunia teknologi informasi tetap langsung paham maksud angka yang
 * ditampilkan. Judul panel pun disertai satu sampai dua kalimat penjelas yang sederhana.
 * </p>
 *
 * <p>
 * <b>Keamanan &amp; kinerja.</b> Semua metode statis, tanpa keadaan (stateless) sehingga aman
 * dipanggil dari banyak thread, tidak membuka koneksi basis data, dan tidak melakukan kueri apa pun
 * &mdash; kit ini hanya MERANGKAI tampilan dari angka yang sudah disiapkan pemanggil. Dengan
 * demikian dashboard lama cukup mengambil datanya seperti biasa, lalu menyerahkan angka jadi ke kit
 * ini untuk dirender. Gaya CSS diberi awalan {@code .el-rk} dan ditanam sekali per blok sehingga
 * tidak "bocor" memengaruhi elemen halaman lain.
 * </p>
 */
public final class KartuRingkasanKit {

    private KartuRingkasanKit() {
    }

    /** Penghasil id panel tab unik (agar banyak tabbox dalam satu halaman tidak bentrok). */
    private static final java.util.concurrent.atomic.AtomicInteger TSEQ = new java.util.concurrent.atomic.AtomicInteger();

    /**
     * Bangun TABBOX ringan (HTML/CSS/JS murni) untuk MENGHEMAT RUANG: beberapa potongan HTML
     * (mis. grafik spider, tren, corong) ditaruh dalam tab sehingga hanya satu yang tampil pada satu
     * waktu. Pergantian tab murni di sisi klien (tanpa permintaan ke server), bilah tab mengikuti
     * warna tema aktif, dan tab dengan konten kosong otomatis dilewati. Cocok untuk area
     * linimasa/dashboard yang sempit. Karena isinya HTML/SVG (bukan komponen ZK yang butuh hitung
     * tinggi), grafik dalam tab tersembunyi tetap tampil benar saat tab-nya dipilih.
     *
     * @param labels   judul tiap tab (selaras indeks dengan {@code contents}).
     * @param contents potongan HTML tiap tab; yang kosong/{@code null} dilewati.
     * @return HTML tabbox; string kosong bila tidak ada konten.
     */
    public static String tabbox(String[] labels, String[] contents) {
        if (labels == null || contents == null) {
            return "";
        }
        String base = "elrktab_" + TSEQ.incrementAndGet();
        StringBuilder bar = new StringBuilder();
        StringBuilder panels = new StringBuilder();
        int shown = 0;
        for (int i = 0; i < labels.length && i < contents.length; i++) {
            if (contents[i] == null || contents[i].trim().length() == 0) {
                continue;
            }
            String pid = base + "_" + i;
            boolean active = shown == 0;
            bar.append("<button type='button' class='el-rk-tab").append(active ? " active" : "").append("' onclick=\"")
                    .append("var w=this.closest('.el-rk-tabs');var ps=w.querySelectorAll('.el-rk-tabpanel');")
                    .append("for(var k=0;k<ps.length;k++)ps[k].style.display='none';")
                    .append("var t=document.getElementById('").append(pid).append("');if(t)t.style.display='block';")
                    .append("var ts=w.querySelectorAll('.el-rk-tab');for(var k=0;k<ts.length;k++)ts[k].className='el-rk-tab';")
                    .append("this.className='el-rk-tab active';\">").append(esc(labels[i])).append("</button>");
            panels.append("<div id='").append(pid).append("' class='el-rk-tabpanel' style='display:")
                    .append(active ? "block" : "none").append("'>").append(contents[i]).append("</div>");
            shown++;
        }
        if (shown == 0) {
            return "";
        }
        return css() + "<div class='el-rk-tabs'><div class='el-rk-tabbar'>" + bar + "</div>" + panels + "</div>";
    }

    /**
     * Penanda warna "ikuti tema aktif" untuk parameter {@code warna} pada kartu. Bila dipakai,
     * kartu memakai warna utama tema yang sedang dipilih pengguna (variabel CSS
     * {@code --ais-theme-primary} yang didefinisikan setiap file tema seperti hijau.css/ytb.css),
     * sehingga dashboard otomatis senada dengan tema institusi &mdash; bukan warna tetap. Bila warna
     * heksadesimal biasa (mis. "#16a34a") yang diberikan, warna itu dipakai apa adanya (cocok untuk
     * makna tetap seperti hijau=baik, merah=buruk).
     */
    public static final String TEMA = "@tema";

    /** Seperti {@link #TEMA} tetapi memakai warna AKSEN tema ({@code --ais-theme-accent}). */
    public static final String TEMA_AKSEN = "@temaAksen";

    /** Ubah nilai warna kartu menjadi ekspresi CSS: penanda tema → variabel tema; selain itu apa adanya. */
    private static String warnaColor(String w) {
        if (TEMA.equals(w)) {
            return "var(--ais-theme-primary,#0d6efd)";
        }
        if (TEMA_AKSEN.equals(w)) {
            return "var(--ais-theme-accent,#16a34a)";
        }
        return w;
    }

    /** Latar lencana (tint terang) untuk sebuah warna: tema → rgba variabel; heksadesimal → +alpha. */
    private static String warnaBadgeBg(String w) {
        if (TEMA.equals(w)) {
            return "rgba(var(--ais-theme-primary-rgb,13,110,253),.12)";
        }
        if (TEMA_AKSEN.equals(w)) {
            return "rgba(var(--ais-theme-accent-rgb,22,163,74),.12)";
        }
        return w + "1a";
    }

    // ════════════════════════════════════════════════════════════════════════
    // Kerangka panel
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Buka satu blok ringkasan: menanam gaya CSS, judul, kalimat penjelas sederhana, dan membuka
     * kisi kartu yang responsif. Pasangkan dengan {@link #tutupGrid()} lalu {@link #tutupPanel()}.
     *
     * @param judul     judul blok (mis. "Ringkasan Aktivitas Pembelajaran Digital").
     * @param deskripsi satu-dua kalimat penjelas ramah orang awam (boleh {@code null}).
     * @return potongan HTML pembuka.
     */
    public static String bukaPanel(String judul, String deskripsi) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("<div class='el-rk'>");
        sb.append(css());
        sb.append("<div class='el-rk-head-wrap'>");
        sb.append("<div class='el-rk-h1'>").append(esc(judul)).append("</div>");
        if (deskripsi != null && deskripsi.trim().length() > 0) {
            sb.append("<div class='el-rk-sub'>").append(esc(deskripsi)).append("</div>");
        }
        sb.append("</div>");
        sb.append("<div class='el-rk-grid'>");
        return sb.toString();
    }

    /** Tutup kisi kartu (pasangan {@link #bukaPanel(String, String)}). */
    public static String tutupGrid() {
        return "</div>";
    }

    /** Tutup blok ringkasan (penutup terluar {@code .el-rk}). */
    public static String tutupPanel() {
        return "</div>";
    }

    /**
     * Bungkus satu atau lebih bilah komposisi ke dalam kotak ber-judul penjelas.
     *
     * @param deskripsi kalimat penjelas (mis. "Perbandingan kehadiran — ...").
     * @param isiBar    gabungan HTML hasil {@link #barKomposisi(String, String[], int[], String[])}.
     * @return potongan HTML; string kosong bila {@code isiBar} kosong.
     */
    public static String seksiBar(String deskripsi, String isiBar) {
        if (isiBar == null || isiBar.trim().length() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-bars'>");
        if (deskripsi != null && deskripsi.trim().length() > 0) {
            sb.append("<div class='el-rk-bars-title'>").append(esc(deskripsi)).append("</div>");
        }
        sb.append(isiBar).append("</div>");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Kartu
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Kartu KEMAJUAN: angka besar "tercapai / target", bilah kemajuan berwarna, dan dua pill
     * (tercapai &amp; sisa). Cocok untuk hal seperti "Akses Pertemuan 314/316" atau "Tagihan Lunas".
     *
     * @param judul     judul kartu.
     * @param warna     warna aksen (hex, mis. "#0d6efd").
     * @param done      nilai tercapai.
     * @param target    nilai target/total.
     * @param lblDone   label untuk pill tercapai (mis. "Sudah dibuka").
     * @param deskripsi satu kalimat penjelas sederhana.
     * @return potongan HTML kartu.
     */
    public static String kartuProgres(String judul, String warna, int done, int target, String lblDone,
            String deskripsi) {
        int belum = Math.max(0, target - done);
        int pct = target <= 0 ? 0 : (int) Math.round(done * 100.0 / target);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-card' style='border-top-color:").append(warnaColor(warna)).append(";'>");
        sb.append(head(judul, pct + "%", warna));
        sb.append("<div class='el-rk-big' style='color:").append(warnaColor(warna)).append(";'>").append(fmt(done))
                .append("<span class='el-rk-of'>/ ").append(fmt(target)).append("</span></div>");
        sb.append("<div class='el-rk-progress'><span style='width:").append(pct).append("%;background:")
                .append(warnaColor(warna)).append(";'></span></div>");
        sb.append("<div class='el-rk-foot'>");
        sb.append(pill(lblDone, fmt(done), "#16a34a"));
        sb.append(pill("Belum", fmt(belum), "#dc2626"));
        sb.append("</div>");
        sb.append("<div class='el-rk-ket'>").append(esc(deskripsi)).append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Kartu BEBERAPA ANGKA sejenis (satu warna), mis. "Materi &amp; Media: Materi/Audio/Video".
     * Lencana kepala menampilkan total seluruh angka diikuti satuan.
     */
    public static String kartuAngka(String judul, String warna, String[] label, int[] nilai, String satuan,
            String deskripsi) {
        int total = 0;
        for (int i = 0; i < nilai.length; i++) {
            total += nilai[i];
        }
        String badge = fmt(total) + (satuan != null && satuan.length() > 0 ? (" " + satuan) : "");
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-card' style='border-top-color:").append(warnaColor(warna)).append(";'>");
        sb.append(head(judul, badge, warna));
        sb.append("<div class='el-rk-foot el-rk-wrap'>");
        for (int i = 0; i < label.length; i++) {
            sb.append(pill(label[i], fmt(nilai[i]), warnaColor(warna)));
        }
        sb.append("</div>");
        sb.append("<div class='el-rk-ket'>").append(esc(deskripsi)).append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    /**
     * Kartu RINCIAN BERWARNA per kategori (warna berbeda tiap pill), mis. kehadiran
     * Hadir/Alpa/Izin/Sakit. Lencana kepala menampilkan jumlah seluruh kategori.
     *
     * @param warnaAksen warna garis aksen kartu &amp; badge.
     * @param label      nama tiap kategori.
     * @param nilai      nilai tiap kategori (selaras indeks dengan {@code label}).
     * @param warna      warna tiap pill (selaras indeks); kategori bernilai 0 dilewati.
     * @param satuan     satuan untuk lencana total (mis. "data").
     */
    public static String kartuBreakdown(String judul, String warnaAksen, String[] label, int[] nilai,
            String[] warna, String satuan, String deskripsi) {
        int total = 0;
        for (int i = 0; i < nilai.length; i++) {
            total += nilai[i];
        }
        String badge = fmt(total) + (satuan != null && satuan.length() > 0 ? (" " + satuan) : "");
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-card' style='border-top-color:").append(warnaColor(warnaAksen)).append(";'>");
        sb.append(head(judul, badge, warnaAksen));
        sb.append("<div class='el-rk-foot el-rk-wrap'>");
        for (int i = 0; i < label.length; i++) {
            if (nilai[i] <= 0) {
                continue;
            }
            sb.append(pill(label[i], fmt(nilai[i]), warna[i % warna.length]));
        }
        sb.append("</div>");
        sb.append("<div class='el-rk-ket'>").append(esc(deskripsi)).append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    /** Kartu SATU ANGKA besar (mis. "Tugas: 96"). */
    public static String kartuTotal(String judul, String warna, int total, String deskripsi) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-card' style='border-top-color:").append(warnaColor(warna)).append(";'>");
        sb.append(head(judul, "", warna));
        sb.append("<div class='el-rk-big' style='color:").append(warnaColor(warna)).append(";'>").append(fmt(total))
                .append("</div>");
        sb.append("<div class='el-rk-ket'>").append(esc(deskripsi)).append("</div></div>");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Bilah komposisi (stacked bar) HTML/CSS murni
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Bilah komposisi bertumpuk untuk memvisualkan proporsi beberapa kategori dalam satu garis,
     * lengkap dengan legenda. Kategori bernilai 0 dilewati. Cocok mis. komposisi kehadiran atau
     * komposisi status pembayaran.
     *
     * @param judul judul baris bilah.
     * @param label nama tiap kategori.
     * @param nilai nilai tiap kategori (selaras indeks).
     * @param warna warna tiap kategori (selaras indeks).
     * @return potongan HTML; string kosong bila total &le; 0.
     */
    public static String barKomposisi(String judul, String[] label, int[] nilai, String[] warna) {
        int total = 0;
        for (int i = 0; i < nilai.length; i++) {
            total += nilai[i] > 0 ? nilai[i] : 0;
        }
        if (total <= 0) {
            return "";
        }
        StringBuilder seg = new StringBuilder();
        StringBuilder leg = new StringBuilder();
        for (int i = 0; i < label.length; i++) {
            int v = nilai[i] > 0 ? nilai[i] : 0;
            if (v <= 0) {
                continue;
            }
            double persen = v * 100.0 / total;
            String c = warna[i % warna.length];
            seg.append("<span title='").append(esc(label[i])).append(": ").append(fmt(v)).append("' style='width:")
                    .append(String.format("%.1f", persen)).append("%;background:").append(c).append(";'></span>");
            leg.append("<span class='el-rk-leg'><i style='background:").append(c).append(";'></i>")
                    .append(esc(label[i])).append(" ").append(fmt(v)).append("</span>");
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-barrow'>");
        sb.append("<div class='el-rk-barlabel'>").append(esc(judul)).append("</div>");
        sb.append("<div class='el-rk-bar'>").append(seg).append("</div>");
        sb.append("<div class='el-rk-barleg'>").append(leg).append("</div>");
        sb.append("</div>");
        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════════════════
    // Bisa-klik + popup rincian (HTML/CSS/JS murni, tanpa round-trip server)
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Jadikan sebuah kartu (hasil {@code kartu*}) BISA DIKLIK untuk membuka popup rincian
     * ber-id {@code modalId}. Menyisipkan kelas penanda, perilaku klik (membuka popup), dan
     * keterangan kecil "Klik untuk rincian" di bawah kartu. Tidak ada permintaan ke server &mdash;
     * popup yang ditampilkan adalah elemen tersembunyi yang sudah disiapkan lewat
     * {@link #modal(String, String, String)}. Bila {@code modalId} kosong, kartu dikembalikan
     * apa adanya (tidak bisa diklik).
     *
     * @param kartuHtml HTML kartu yang akan dibuat bisa diklik.
     * @param modalId   id popup tujuan (harus cocok dengan id pada {@link #modal}).
     * @return HTML kartu yang sudah bisa diklik.
     */
    public static String klikable(String kartuHtml, String modalId) {
        if (kartuHtml == null || modalId == null || modalId.trim().length() == 0) {
            return kartuHtml == null ? "" : kartuHtml;
        }
        // Saat dibuka, pindahkan popup ke <body> agar position:fixed-nya relatif ke layar penuh
        // (tidak "terjebak" oleh panel ber-transform/overflow) sehingga selalu tampil terpusat di
        // atas seluruh halaman dengan latar gelap.
        String onclick = " onclick=\"var m=document.getElementById('" + modalId
                + "');if(m){if(m.parentNode!==document.body){document.body.appendChild(m);}m.style.display='flex';}\"";
        String h = kartuHtml.replaceFirst("<div class='el-rk-card'",
                "<div class='el-rk-card el-rk-klik'" + onclick);
        int idx = h.lastIndexOf("</div>");
        if (idx >= 0) {
            h = h.substring(0, idx) + "<div class='el-rk-klikhint'>&#128190; Klik untuk rincian</div>" + h.substring(idx);
        }
        return h;
    }

    /**
     * Seperti {@link #klikable(String, String)} tetapi membungkus SEMBARANG potongan HTML (mis. panel
     * grafik spider/garis/corong yang bukan kartu {@code .el-rk-card}) menjadi bisa diklik untuk
     * membuka popup rincian. Cocok untuk menjadikan grafik apa pun bisa diklik tanpa mengubah kode
     * grafiknya.
     *
     * @param html    potongan HTML grafik/panel.
     * @param modalId id popup tujuan.
     * @return HTML yang sudah bisa diklik.
     */
    public static String bungkusKlikable(String html, String modalId) {
        if (html == null || modalId == null || modalId.trim().length() == 0) {
            return html == null ? "" : html;
        }
        String onclick = "var m=document.getElementById('" + modalId
                + "');if(m){if(m.parentNode!==document.body){document.body.appendChild(m);}m.style.display='flex';}";
        return "<div class='el-rk-klik' style='border-radius:14px;' onclick=\"" + onclick + "\">" + html
                + "<div class='el-rk-klikhint' style='padding:2px 14px 10px;margin-top:-4px;'>"
                + "&#128190; Klik untuk rincian</div></div>";
    }

    /**
     * Popup (overlay) rincian ber-id {@code modalId}, ditampilkan saat kartu pasangannya diklik.
     * Isi {@code isiHtml} sudah berupa HTML (tidak di-escape) agar pemanggil bebas menyusun tabel
     * rincian, penjelasan, atau bilah. Klik latar gelap atau tombol &times; menutup popup. Tempelkan
     * keluaran ini sekali di mana saja dalam blok ringkasan (mis. setelah {@link #tutupPanel()}).
     *
     * @param modalId id unik popup (cocokkan dengan {@link #klikable(String, String)}).
     * @param judul   judul popup.
     * @param isiHtml isi rincian (HTML siap-pakai).
     * @return HTML popup.
     */
    public static String modal(String modalId, String judul, String isiHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div id='").append(modalId).append("' class='el-rk-modal' onclick=\"")
                .append("if(event.target===this){this.style.display='none';}\">");
        sb.append("<div class='el-rk-modal-box'>");
        sb.append("<div class='el-rk-modal-head'><span>").append(esc(judul)).append("</span>");
        sb.append("<span class='el-rk-modal-x' onclick=\"document.getElementById('").append(modalId)
                .append("').style.display='none';\">&times;</span></div>");
        sb.append("<div class='el-rk-modal-body'>").append(isiHtml == null ? "" : isiHtml).append("</div>");
        sb.append("</div></div>");
        return sb.toString();
    }

    /**
     * Baris rincian sederhana (label kiri, nilai kanan) untuk isi popup &mdash; memudahkan menyusun
     * "tabel" rincian yang konsisten tanpa menulis HTML berulang.
     */
    public static String barisRincian(String label, String nilai) {
        return "<div class='el-rk-mrow'><span>" + esc(label) + "</span><b>" + esc(nilai) + "</b></div>";
    }

    // ════════════════════════════════════════════════════════════════════════
    // Util render (dipakai ulang kartu & pemanggil)
    // ════════════════════════════════════════════════════════════════════════

    /** Kepala kartu: judul + (opsional) lencana berwarna senada aksen. */
    public static String head(String judul, String badge, String warna) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='el-rk-cardhead'><span class='el-rk-title'>").append(esc(judul)).append("</span>");
        if (badge != null && badge.length() > 0) {
            sb.append("<span class='el-rk-badge' style='color:").append(warnaColor(warna)).append(";background:")
                    .append(warnaBadgeBg(warna)).append(";'>").append(esc(badge)).append("</span>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    /** Satu pill kecil: nilai tebal berwarna di atas, label kecil di bawah. */
    public static String pill(String label, String nilai, String warna) {
        return "<span class='el-rk-pill'><b style='color:" + warna + ";'>" + esc(nilai) + "</b>"
                + "<small>" + esc(label) + "</small></span>";
    }

    /** Format angka ribuan gaya Indonesia (pemisah titik). */
    public static String fmt(int n) {
        return String.format("%,d", n).replace(',', '.');
    }

    /** Escape karakter HTML dasar agar teks aman dirender. */
    public static String esc(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Blok gaya {@code <style>} (sekali tanam per blok). Diberi awalan {@code .el-rk} agar tidak
     * memengaruhi elemen lain di halaman.
     */
    public static String css() {
        return "<style>"
                + ".el-rk{font-family:Arial,Helvetica,sans-serif;color:#0f172a;}"
                + ".el-rk-head-wrap{padding:12px 16px 4px;}"
                + ".el-rk-h1{font-size:15px;font-weight:800;color:#0f172a;letter-spacing:.2px;}"
                + ".el-rk-sub{font-size:12px;color:#64748b;margin-top:3px;line-height:1.45;}"
                + ".el-rk-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));"
                + "gap:12px;padding:10px 16px 6px;box-sizing:border-box;}"
                + ".el-rk-card{background:#fff;border:1px solid #e8edf3;border-top:4px solid #ccc;"
                + "border-radius:14px;padding:13px 14px;box-shadow:0 6px 16px rgba(15,23,42,.05);"
                + "box-sizing:border-box;min-width:0;display:flex;flex-direction:column;transition:box-shadow .15s;}"
                + ".el-rk-card:hover{box-shadow:0 10px 24px rgba(15,23,42,.10);}"
                + ".el-rk-cardhead{display:flex;justify-content:space-between;align-items:center;gap:8px;margin-bottom:6px;}"
                + ".el-rk-title{font-size:11px;font-weight:800;color:#334155;text-transform:uppercase;letter-spacing:.4px;}"
                + ".el-rk-badge{font-size:10px;font-weight:800;padding:2px 8px;border-radius:999px;white-space:nowrap;}"
                + ".el-rk-big{font-size:27px;font-weight:800;line-height:1.1;margin:2px 0 7px;}"
                + ".el-rk-of{font-size:13px;font-weight:700;color:#94a3b8;margin-left:4px;}"
                + ".el-rk-progress{height:7px;background:#eef2f7;border-radius:999px;overflow:hidden;margin:0 0 9px;}"
                + ".el-rk-progress span{display:block;height:100%;border-radius:999px;}"
                + ".el-rk-foot{display:flex;gap:6px;}"
                + ".el-rk-wrap{flex-wrap:wrap;}"
                + ".el-rk-pill{display:flex;flex-direction:column;align-items:center;min-width:44px;flex:1;"
                + "background:#f8fafc;border:1px solid #eef2f7;border-radius:10px;padding:5px 6px;}"
                + ".el-rk-pill b{font-size:15px;line-height:1.1;}"
                + ".el-rk-pill small{font-size:8px;color:#94a3b8;text-transform:uppercase;margin-top:2px;letter-spacing:.3px;}"
                + ".el-rk-ket{font-size:10.5px;color:#94a3b8;margin-top:8px;line-height:1.4;}"
                + ".el-rk-bars{margin:6px 16px 14px;background:#fff;border:1px solid #e8edf3;border-radius:14px;"
                + "padding:12px 14px;box-shadow:0 6px 16px rgba(15,23,42,.05);}"
                + ".el-rk-bars-title{font-size:11.5px;color:#64748b;margin-bottom:10px;line-height:1.45;}"
                + ".el-rk-barrow{margin-bottom:11px;}"
                + ".el-rk-barlabel{font-size:11px;font-weight:700;color:#334155;margin-bottom:4px;}"
                + ".el-rk-bar{display:flex;height:14px;border-radius:999px;overflow:hidden;background:#eef2f7;}"
                + ".el-rk-bar span{display:block;height:100%;}"
                + ".el-rk-barleg{margin-top:5px;display:flex;flex-wrap:wrap;gap:10px;}"
                + ".el-rk-leg{font-size:10px;color:#64748b;display:inline-flex;align-items:center;gap:4px;}"
                + ".el-rk-leg i{width:9px;height:9px;border-radius:3px;display:inline-block;}"
                // bisa-klik + keterangan
                + ".el-rk-klik{cursor:pointer;position:relative;}"
                + ".el-rk-klik:hover{box-shadow:0 12px 26px rgba(15,23,42,.14);transform:translateY(-1px);}"
                + ".el-rk-klikhint{font-size:9px;color:var(--ais-theme-primary,#94a3b8);margin-top:7px;font-weight:700;"
                + "opacity:.85;letter-spacing:.2px;}"
                // popup rincian
                + ".el-rk-modal{display:none;position:fixed;top:0;left:0;right:0;bottom:0;z-index:99990;"
                + "background:rgba(15,23,42,.45);align-items:center;justify-content:center;padding:16px;box-sizing:border-box;}"
                + ".el-rk-modal-box{background:#fff;border-radius:16px;width:100%;max-width:520px;max-height:82vh;"
                + "overflow:auto;box-shadow:0 24px 60px rgba(15,23,42,.30);}"
                + ".el-rk-modal-head{display:flex;justify-content:space-between;align-items:center;gap:10px;"
                + "padding:14px 16px;border-bottom:1px solid #eef2f7;position:sticky;top:0;background:#fff;}"
                + ".el-rk-modal-head span{font-size:14px;font-weight:800;color:var(--ais-theme-primary,#0f172a);}"
                + ".el-rk-modal-x{cursor:pointer;font-size:22px;line-height:1;color:#94a3b8;font-weight:400;padding:0 4px;}"
                + ".el-rk-modal-x:hover{color:#dc2626;}"
                + ".el-rk-modal-body{padding:14px 16px 18px;font-size:12.5px;color:#334155;line-height:1.6;}"
                + ".el-rk-mrow{display:flex;justify-content:space-between;gap:12px;padding:7px 0;border-bottom:1px dashed #eef2f7;}"
                + ".el-rk-mrow span{color:#64748b;}.el-rk-mrow b{color:#0f172a;}"
                // tabbox hemat ruang
                + ".el-rk-tabs{margin:6px 16px 14px;background:#fff;border:1px solid #e8edf3;border-radius:14px;"
                + "box-shadow:0 6px 16px rgba(15,23,42,.05);overflow:hidden;}"
                + ".el-rk-tabbar{display:flex;flex-wrap:wrap;gap:3px;padding:8px 8px 0;background:#f8fafc;"
                + "border-bottom:1px solid #eef2f7;}"
                + ".el-rk-tab{border:0;background:transparent;font-size:11.5px;font-weight:800;color:#64748b;"
                + "padding:8px 14px;border-radius:10px 10px 0 0;cursor:pointer;font-family:inherit;}"
                + ".el-rk-tab:hover{color:var(--ais-theme-primary,#0d6efd);background:#fff;}"
                + ".el-rk-tab.active{color:var(--ais-theme-primary,#0d6efd);background:#fff;"
                + "box-shadow:inset 0 -2px 0 var(--ais-theme-primary,#0d6efd);}"
                + ".el-rk-tabpanel{padding:4px 8px 6px;}"
                // baris kelas aktivitas (popup spider)
                + ".el-rk-krow{padding:8px 0;border-bottom:1px dashed #eef2f7;}"
                + ".el-rk-krow:last-child{border-bottom:0;}"
                + ".el-rk-krow-nm{font-size:12px;font-weight:700;color:#0f172a;margin-bottom:5px;}"
                + ".el-rk-krow-stats{display:flex;flex-wrap:wrap;gap:4px;}"
                + ".el-rk-kstat{font-size:10.5px;background:#f1f5f9;color:#475569;border-radius:99px;"
                + "padding:2px 9px;font-weight:600;}"
                + "@media(max-width:480px){.el-rk-grid{grid-template-columns:1fr;}.el-rk-big{font-size:24px;}"
                + ".el-rk-tab{padding:7px 10px;}}"
                + "</style>";
    }
}
