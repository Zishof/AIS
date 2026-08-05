package ais.action.mobile;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.A;
import org.zkoss.zul.Div;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

/**
 * <h1>MobileUiHelper — Pabrik Komponen Antarmuka Mobile eCampus &amp; eSchool (ZK 5)</h1>
 *
 * <p>Kelas ini adalah <em>pabrik komponen</em> (component factory) terpusat untuk
 * seluruh elemen antarmuka yang dipakai oleh tampilan mobile. Setiap method bersifat
 * <b>statis tanpa-kondisi</b> (stateless): menerima parameter, membuat satu atau lebih
 * komponen ZK 5, lalu mengembalikannya. Pemanggil bebas merakit komponen-komponen itu
 * ke dalam hierarki ZK tanpa perlu memahami detail konstruksi DOM internal.</p>
 *
 * <h2>Mengapa factory statis?</h2>
 * <p>Tampilan mobile memiliki banyak elemen berulang: kartu informasi, baris daftar,
 * ubin aksi cepat, grafik batang, dan sebagainya. Jika setiap controller membangun
 * komponen ini secara lokal, akan ada duplikasi kode yang menyulitkan pemeliharaan —
 * mengubah radius sudut kartu berarti mengubah puluhan tempat. Dengan memusatkan
 * konstruksi di kelas ini, perubahan tampilan cukup dilakukan di satu titik dan
 * langsung berlaku ke seluruh layar mobile.</p>
 *
 * <h2>Prinsip desain</h2>
 * <p><strong>Mobile-first dan ringan.</strong> Semua method menghindari nesting
 * komponen yang terlalu dalam agar DOM ZK di memori tetap kecil. ZK 5 memuat ulang
 * komponen di memori server saat ada event; komponen yang dalam dan banyak memperlambat
 * respons. Setiap komponen diberi CSS class berlabel {@code ais-m-*} agar tidak
 * bentrok dengan gaya desktop di {@code css_utama.css}.</p>
 *
 * <p><strong>Konsistensi visual.</strong> Kartu, baris daftar, dan ubin selalu mengikuti
 * skema yang sama: sudut membulat, bayangan halus, ruang dalam yang cukup. Warna
 * mengikuti variabel CSS {@code --ais-m-primary} sehingga tema yang dipilih pengguna
 * otomatis berlaku ke seluruh tampilan mobile tanpa pengubahan kode.</p>
 *
 * <p><strong>Aksesibilitas dasar.</strong> Setiap tombol ikon diberi {@code tooltiptext}
 * agar pembaca layar mendapat label bermakna. Teks pada komponen tidak pernah {@code null}
 * — selalu diisi paling tidak string kosong — sehingga ZK tidak membuang NPE saat render.</p>
 *
 * <h2>Panduan method yang tersedia</h2>
 * <ul>
 *   <li>{@link #div(String)} &amp; {@link #label(String, String)} — blok bangunan dasar.</li>
 *   <li>{@link #card(String, String)} — kartu informasi putih dengan judul dan deskripsi.</li>
 *   <li>{@link #listItem(String, String, String, EventListener)} — baris daftar dengan
 *       ikon, judul, subjudul, dan panah.</li>
 *   <li>{@link #notifRow(String, String, String, boolean, EventListener)} — baris notifikasi
 *       yang mendukung status "belum dibaca" (tebal) vs. "sudah dibaca" (normal).</li>
 *   <li>{@link #profileHead(String, String, String)} — kartu header profil bergradien
 *       dengan avatar, nama, peran, dan institusi.</li>
 *   <li>{@link #profileRow(String, String, String, EventListener)} — baris menu profil.</li>
 *   <li>{@link #quickTile(String, String, EventListener)} — ubin aksi cepat (ikon + label).</li>
 *   <li>{@link #hero(String, String)} — kartu sapaan bergradien di bagian atas beranda.</li>
 *   <li>{@link #meter(String, int, String)} — bilah kemajuan horizontal.</li>
 *   <li>{@link #simpleTrend(String, int, int, int, int, int)} — grafik batang sparkline
 *       5 titik untuk tren aktivitas.</li>
 *   <li>{@link #trendChart(String, String, int[], String[])} — grafik batang lengkap
 *       dengan label sumbu-X.</li>
 *   <li>{@link #spider(String, String, int, int, int, int)} — diagram donut warna-warni
 *       untuk ringkasan empat dimensi.</li>
 *   <li>{@link #desktopLink(EventListener)} — tombol "Buka Versi Desktop" di profil.</li>
 *   <li>{@link #chip(String, EventListener)} — label oval kecil yang bisa diklik.</li>
 *   <li>{@link #clear(Component)} — menghapus semua anak komponen sebelum render ulang.</li>
 * </ul>
 *
 * <h2>Kompatibilitas</h2>
 * <p>Seluruh kode ditulis untuk Java 1.7 (tanpa lambda, stream, atau try-with-resources)
 * dan ZK 5.x. Tidak ada import dari paket ZK versi 6 ke atas. Semua komponen yang
 * dipakai ({@code Div}, {@code Label}, {@code Toolbarbutton}, {@code Textbox}, dll.)
 * tersedia di ZK 5 dan sudah terbukti stabil di aplikasi AIS yang sedang berjalan.
 * Anonymous inner class digunakan sebagai pengganti lambda untuk {@code EventListener}.</p>
 *
 * <h2>Catatan pemeliharaan</h2>
 * <p>Bila ingin menambah jenis komponen baru, ikuti pola yang sudah ada: method statis,
 * parameter minimal, beri {@code sclass} yang berawalan {@code ais-m-}, dan tambahkan
 * CSS yang sesuai di {@code ais_mobile.css}. Jangan menyimpan state di dalam kelas ini
 * karena ia tidak punya constructor publik dan tidak boleh diinstansiasi.</p>
 *
 * @author eCampus Mobile Team
 * @see ais.action.maintenance.MobileAction
 * @see ais.action.mobile.MobileNotifHelper
 */
public final class MobileUiHelper {

    private MobileUiHelper() {
        /* Utilitas statis — tidak boleh diinstansiasi. */
    }

    /* ===================================================================
     * Blok bangunan dasar
     * =================================================================== */

    /**
     * Membuat {@link Div} dengan CSS class yang diberikan.
     *
     * @param sclass nama CSS class; boleh null.
     * @return div baru.
     */
    public static Div div(String sclass) {
        Div div = new Div();
        if (sclass != null && sclass.trim().length() > 0) {
            div.setSclass(sclass);
        }
        return div;
    }

    /**
     * Membuat {@link Label} dengan teks dan CSS class yang diberikan.
     * Teks {@code null} dikonversi menjadi string kosong agar ZK tidak melempar NPE.
     *
     * @param text   teks label; {@code null} diterima.
     * @param sclass nama CSS class; boleh null.
     * @return label baru.
     */
    public static Label label(String text, String sclass) {
        Label lbl = new Label(text == null ? "" : text);
        if (sclass != null && sclass.trim().length() > 0) {
            lbl.setSclass(sclass);
        }
        return lbl;
    }

    /**
     * Membuat {@link Image} dengan sumber gambar dan CSS class yang diberikan.
     * Bila sumber kosong atau null, diganti dengan {@code /img/logo.png} sebagai fallback.
     *
     * @param src    URL gambar; boleh null.
     * @param sclass nama CSS class; boleh null.
     * @return image baru.
     */
    public static Image image(String src, String sclass) {
        String safeSrc = (src == null || src.trim().length() == 0) ? "/img/logo.png" : src.trim();
        Image img = new Image(safeSrc);
        if (sclass != null && sclass.trim().length() > 0) {
            img.setSclass(sclass);
        }
        return img;
    }

    /* ===================================================================
     * Tombol dan tautan
     * =================================================================== */

    /**
     * Membuat tombol ikon bulat untuk app bar atau bilah overlay.
     * Filter CSS {@code brightness(0) invert(1)} di {@code ais_mobile.css} membuat
     * semua ikon SVG menjadi putih secara otomatis saat berada di atas latar hijau.
     *
     * @param imageSrc URL gambar ikon.
     * @param tooltip  teks tooltip (untuk aksesibilitas).
     * @param listener handler onClick; boleh null.
     * @return toolbarbutton ikon.
     */
    public static Toolbarbutton iconButton(String imageSrc, String tooltip, EventListener listener) {
        Toolbarbutton btn = new Toolbarbutton();
        btn.setImage(imageSrc == null ? "/img/svg/list-task-white.svg" : imageSrc);
        btn.setTooltiptext(tooltip == null ? "" : tooltip);
        btn.setSclass("ais-m-iconbtn");
        if (listener != null) {
            btn.addEventListener("onClick", listener);
        }
        return btn;
    }

    /**
     * Membuat tombol navigasi bawah (bottom-nav) dengan ikon di atas dan label di bawah.
     *
     * @param imageSrc URL ikon.
     * @param lbl      teks label.
     * @param sclass   CSS class (biasanya kombinasi {@code ais-m-bottom-item} + {@code ais-m-bottom-active}).
     * @param listener handler onClick; boleh null.
     * @return toolbarbutton navigasi bawah.
     */
    public static Toolbarbutton bottomButton(String imageSrc, String lbl, String sclass, EventListener listener) {
        Toolbarbutton btn = new Toolbarbutton(lbl == null ? "" : lbl);
        btn.setImage(imageSrc);
        btn.setOrient("vertical");
        btn.setSclass(sclass == null ? "ais-m-bottom-item" : sclass);
        if (listener != null) {
            btn.addEventListener("onClick", listener);
        }
        return btn;
    }

    /**
     * Membuat tombol "Buka Versi Desktop" yang ditampilkan di bagian bawah layar Profil.
     * Tombol ini memberi jalan keluar bagi pengguna yang perlu fitur khusus desktop.
     *
     * @param listener handler onClick; boleh null.
     * @return div tombol desktop.
     */
    public static Div desktopLink(EventListener listener) {
        Div row = div("ais-m-desktop-link");
        row.appendChild(image("/img/svg/list-task-white.svg", "ais-m-desktop-link-ic"));
        row.appendChild(label("Buka Versi Desktop", "ais-m-desktop-link-lbl"));
        if (listener != null) {
            row.addEventListener("onClick", listener);
        }
        return row;
    }

    /**
     * Membuat label chip oval kecil yang bisa diklik, untuk filter, riwayat, atau favorit.
     *
     * @param text     teks chip.
     * @param listener handler onClick; boleh null.
     * @return div chip.
     */
    public static Div chip(String text, EventListener listener) {
        Div chip = div("ais-m-chip");
        chip.appendChild(label(text == null ? "" : text, null));
        if (listener != null) {
            chip.addEventListener("onClick", listener);
        }
        return chip;
    }

    /* ===================================================================
     * Kartu dan daftar
     * =================================================================== */

    /**
     * Membuat kartu putih generik dengan judul dan deskripsi untuk orang awam.
     * Deskripsi sebaiknya ditulis dalam bahasa sehari-hari yang mudah dipahami,
     * tanpa istilah teknis.
     *
     * @param title       judul kartu; boleh null (tidak ditampilkan).
     * @param description deskripsi singkat; boleh null.
     * @return div kartu.
     */
    public static Div card(String title, String description) {
        Div card = div("ais-m-card");
        if (title != null && title.trim().length() > 0) {
            card.appendChild(label(title, "ais-m-card-title"));
        }
        if (description != null && description.trim().length() > 0) {
            card.appendChild(label(description, "ais-m-card-desc"));
        }
        return card;
    }

    /**
     * Membuat baris kepala bagian dengan judul di kiri dan tautan aksi di kanan.
     * Pola ini dipakai untuk memisahkan kelompok konten di beranda.
     *
     * @param title       judul bagian.
     * @param actionLabel teks tautan; boleh null (tidak ditampilkan).
     * @param listener    handler onClick untuk tautan; boleh null.
     * @return div kepala bagian.
     */
    public static Div sectionHead(String title, String actionLabel, EventListener listener) {
        Div head = div("ais-m-section-head");
        head.appendChild(label(title == null ? "" : title, "ais-m-section-title"));
        if (actionLabel != null && actionLabel.trim().length() > 0) {
            A link = new A(actionLabel);
            link.setSclass("ais-m-link");
            if (listener != null) {
                link.addEventListener("onClick", listener);
            }
            head.appendChild(link);
        }
        return head;
    }

    /**
     * Membuat baris daftar menu standar: ikon kiri, judul + subjudul tengah, panah kanan.
     * Setiap klik pada baris ini membuka menu dalam overlay layar penuh.
     *
     * @param imageSrc URL ikon.
     * @param title    judul baris (nama menu).
     * @param subtitle keterangan tambahan; boleh null.
     * @param listener handler onClick; boleh null.
     * @return div baris daftar.
     */
    public static Div listItem(String imageSrc, String title, String subtitle, EventListener listener) {
        Div item = div("ais-m-list-item");
        item.appendChild(image(imageSrc, "ais-m-list-ic"));
        Div body = div("ais-m-list-body");
        body.appendChild(label(title == null ? "" : title, "ais-m-list-title"));
        if (subtitle != null && subtitle.trim().length() > 0) {
            body.appendChild(label(subtitle, "ais-m-list-sub"));
        }
        item.appendChild(body);
        item.appendChild(label("›", "ais-m-chevron"));
        if (listener != null) {
            item.addEventListener("onClick", listener);
        }
        return item;
    }

    /**
     * Membuat baris notifikasi dengan dukungan status "belum dibaca" (teks tebal,
     * titik hijau) versus "sudah dibaca" (teks normal). Waktu relatif ditampilkan
     * di bawah judul agar pengguna tahu seberapa lama notifikasi sudah ada.
     *
     * @param imageSrc URL ikon jenis notifikasi.
     * @param title    judul/subjek notifikasi.
     * @param time     waktu relatif (mis. "5 mnt lalu"); boleh null.
     * @param unread   {@code true} = belum dibaca (tampil tebal + titik).
     * @param listener handler onClick; boleh null.
     * @return div baris notifikasi.
     */
    public static Div notifRow(String imageSrc, String title, String time, boolean unread,
            EventListener listener) {
        Div row = div("ais-m-notif" + (unread ? " ais-m-notif-unread" : ""));
        Div ic = div("ais-m-notif-ic");
        ic.appendChild(image(imageSrc, null));
        row.appendChild(ic);
        Div body = div("ais-m-notif-txt");
        body.appendChild(label(title == null ? "" : title,
                "ais-m-notif-title" + (unread ? " ais-m-notif-bold" : "")));
        if (time != null && time.trim().length() > 0) {
            body.appendChild(label(time, "ais-m-notif-sub"));
        }
        row.appendChild(body);
        if (unread) {
            row.appendChild(div("ais-m-notif-dot"));
        } else {
            row.appendChild(label("›", "ais-m-chevron"));
        }
        if (listener != null) {
            row.addEventListener("onClick", listener);
        }
        return row;
    }

    /* ===================================================================
     * Profil
     * =================================================================== */

    /**
     * Membuat header kartu profil bergradien: avatar (lingkaran), nama besar, dan peran/institusi.
     * Kartu ini menjadi identitas visual pengguna di layar Profil.
     *
     * @param name        nama pengguna.
     * @param role        peran (dosen, mahasiswa, admin, dll.).
     * @param institution nama institusi; boleh null.
     * @return div header profil.
     */
    public static Div profileHead(String name, String role, String institution) {
        Div head = div("ais-m-profile-head");
        Div avaWrap = div("ais-m-profile-ava-wrap");
        Label avaLbl = label(
                (name != null && name.trim().length() > 0)
                        ? String.valueOf(name.trim().charAt(0)).toUpperCase()
                        : "?",
                "ais-m-profile-ava-lbl");
        avaWrap.appendChild(avaLbl);
        head.appendChild(avaWrap);
        Div info = div("ais-m-profile-info");
        info.appendChild(label(name == null ? "Pengguna" : name, "ais-m-profile-nm"));
        info.appendChild(label(role == null ? "" : role, "ais-m-profile-rl"));
        if (institution != null && institution.trim().length() > 0) {
            info.appendChild(label(institution, "ais-m-profile-inst"));
        }
        head.appendChild(info);
        return head;
    }

    /**
     * Membuat baris menu profil: ikon kiri, label teks tengah, panah/simbol kanan.
     * Parameter {@code extraSclass} dapat dipakai untuk menandai baris berbahaya
     * (misalnya "Keluar") dengan class {@code is-danger}.
     *
     * @param imageSrc   URL ikon baris.
     * @param lbl        teks label baris.
     * @param extraSclass CSS class tambahan; boleh null.
     * @param listener   handler onClick; boleh null.
     * @return div baris profil.
     */
    public static Div profileRow(String imageSrc, String lbl, String extraSclass,
            EventListener listener) {
        String sc = "ais-m-prow" + (extraSclass != null ? " " + extraSclass : "");
        Div row = div(sc);
        row.appendChild(image(imageSrc, "ais-m-prow-ic"));
        row.appendChild(label(lbl == null ? "" : lbl, "ais-m-prow-lbl"));
        row.appendChild(label("›", "ais-m-prow-go"));
        if (listener != null) {
            row.addEventListener("onClick", listener);
        }
        return row;
    }

    /* ===================================================================
     * Beranda — hero, hari ini, ringkasan
     * =================================================================== */

    /**
     * Membuat kartu sapaan bergradien di bagian atas beranda.
     * Kartu ini menyapa pengguna dengan nama dan peran agar terasa personal.
     *
     * @param name nama pengguna.
     * @param role peran pengguna; boleh kosong.
     * @return div hero.
     */
    public static Div hero(String name, String role) {
        Div hero = div("ais-m-hero");
        hero.appendChild(label("Selamat datang,", "ais-m-hero-hi"));
        hero.appendChild(label(name == null ? "Pengguna" : name, "ais-m-hero-name"));
        if (role != null && role.trim().length() > 0) {
            hero.appendChild(label(role, "ais-m-hero-role"));
        }
        return hero;
    }

    /**
     * Membuat kartu "Informasi Hari Ini" yang bisa diklik untuk membuka notifikasi.
     *
     * @param text     ringkasan isi hari ini.
     * @param listener handler onClick; boleh null.
     * @return div kartu hari ini.
     */
    public static Div todayCard(String text, EventListener listener) {
        Div card = div("ais-m-today");
        card.appendChild(image("/img/svg/calendar2.svg", "ais-m-today-ic"));
        Div body = div("ais-m-today-txt");
        body.appendChild(label("Informasi Hari Ini", "ais-m-today-lbl"));
        body.appendChild(label(text == null ? "" : text, "ais-m-today-val"));
        card.appendChild(body);
        card.appendChild(label("›", "ais-m-chevron"));
        if (listener != null) {
            card.addEventListener("onClick", listener);
        }
        return card;
    }

    /**
     * Membuat kartu angka ringkasan (metrik tunggal): nilai besar di atas, label kecil di bawah.
     *
     * @param value nilai yang ditampilkan.
     * @param lbl   label keterangan.
     * @return div metrik.
     */
    public static Div metric(String value, String lbl) {
        Div metric = div("ais-m-metric");
        metric.appendChild(label(value == null ? "0" : value, "ais-m-metric-value"));
        metric.appendChild(label(lbl == null ? "" : lbl, "ais-m-metric-label"));
        return metric;
    }

    /* ===================================================================
     * Ubin aksi cepat dan modul utama
     * =================================================================== */

    /**
     * Membuat ubin aksi cepat: ikon persegi di atas, label di bawah.
     * Ubin dipakai untuk shortcut fitur yang sering digunakan di beranda.
     *
     * @param imageSrc URL ikon.
     * @param lbl      teks label.
     * @param listener handler onClick; boleh null.
     * @return div ubin.
     */
    public static Div quickTile(String imageSrc, String lbl, EventListener listener) {
        Div tile = div("ais-m-quick-tile");
        Div icWrap = div("ais-m-quick-ic");
        icWrap.appendChild(image(imageSrc, null));
        tile.appendChild(icWrap);
        tile.appendChild(label(lbl == null ? "" : lbl, "ais-m-quick-label"));
        if (listener != null) {
            tile.addEventListener("onClick", listener);
        }
        return tile;
    }

    /* ===================================================================
     * Pencarian
     * =================================================================== */

    /**
     * Membuat kotak pencarian teks lengkap dengan style mobile.
     * Placeholder ditampilkan sebagai {@code tooltiptext} karena ZK 5 tidak mendukung
     * atribut HTML {@code placeholder} secara langsung.
     *
     * @param placeholder teks petunjuk isi kotak.
     * @return textbox pencarian.
     */
    public static Textbox searchBox(String placeholder) {
        Textbox tb = new Textbox();
        tb.setSclass("ais-m-search");
        tb.setWidth("100%");
        if (placeholder != null && placeholder.trim().length() > 0) {
            tb.setTooltiptext(placeholder);
        }
        return tb;
    }

    /* ===================================================================
     * Grafik HTML/CSS (tanpa JFreeChart)
     * =================================================================== */

    /**
     * Membuat bilah kemajuan horizontal (progress meter) dengan label, persentase, dan hint.
     * Persentase di luar rentang 0–100 dijepit ke nilai terdekat.
     *
     * @param lbl     nama indikator (mis. "Kehadiran").
     * @param percent nilai persen (0–100).
     * @param hint    keterangan singkat untuk orang awam; boleh null.
     * @return div meter.
     */
    public static Div meter(String lbl, int percent, String hint) {
        int p = Math.max(0, Math.min(100, percent));
        Div wrap = div("ais-m-meter");
        Hbox head = new Hbox();
        head.setSclass("ais-m-meter-head");
        head.appendChild(label(lbl == null ? "" : lbl, "ais-m-meter-title"));
        head.appendChild(label(p + "%", "ais-m-meter-value"));
        wrap.appendChild(head);
        Div track = div("ais-m-meter-track");
        Div fill = div("ais-m-meter-fill");
        fill.setStyle("width:" + p + "%");
        track.appendChild(fill);
        wrap.appendChild(track);
        if (hint != null && hint.trim().length() > 0) {
            wrap.appendChild(label(hint, "ais-m-card-desc"));
        }
        return wrap;
    }

    /**
     * Membuat grafik batang sparkline 5 titik untuk tren aktivitas singkat.
     * Tidak ada label sumbu — cocok untuk gambaran cepat di beranda.
     * Untuk grafik lebih lengkap dengan label, gunakan {@link #trendChart(String, String, int[], String[])}.
     *
     * @param title judul grafik.
     * @param a     nilai batang pertama (0–100).
     * @param b     nilai batang kedua.
     * @param c     nilai batang ketiga.
     * @param d     nilai batang keempat.
     * @param e     nilai batang kelima.
     * @return div grafik sparkline.
     */
    public static Div simpleTrend(String title, int a, int b, int c, int d, int e) {
        Div card = card(title, "Naik turunnya aktivitas dari waktu ke waktu, dari kiri (terlama) ke kanan (terbaru).");
        Div bars = div("ais-m-bars");
        appendBar(bars, a);
        appendBar(bars, b);
        appendBar(bars, c);
        appendBar(bars, d);
        appendBar(bars, e);
        card.appendChild(bars);
        return card;
    }

    /**
     * Membuat grafik batang lengkap dengan judul, deskripsi, nilai, dan label sumbu-X.
     * Jumlah nilai dan label harus sama; jika tidak sama, label yang tidak punya pasangan
     * diabaikan secara aman.
     *
     * @param title       judul grafik.
     * @param description keterangan untuk orang awam; boleh null.
     * @param values      array nilai batang (0–100).
     * @param xLabels     array label sumbu-X; boleh null atau kosong.
     * @return div grafik batang.
     */
    public static Div trendChart(String title, String description, int[] values, String[] xLabels) {
        Div card = card(title, description);
        if (values == null || values.length == 0) {
            return card;
        }
        Div bars = div("ais-m-bars ais-m-bars-labeled");
        for (int i = 0; i < values.length; i++) {
            int h = Math.max(8, Math.min(100, values[i]));
            Div bar = div("ais-m-bar");
            bar.setStyle("height:" + h + "%");
            if (xLabels != null && i < xLabels.length && xLabels[i] != null) {
                bar.appendChild(label(xLabels[i], "ais-m-bar-lbl"));
            }
            bars.appendChild(bar);
        }
        card.appendChild(bars);
        return card;
    }

    /**
     * Membuat diagram donut warna-warni untuk meringkas empat dimensi sekaligus
     * (misalnya akademik, kehadiran, tagihan, dan tugas). Setiap segmen diwakili
     * oleh warna berbeda. Angka di tengah menunjukkan skor rata-rata.
     * Nilai tiap dimensi dijepit ke rentang 1–70 agar selalu ada representasi visual.
     *
     * @param title       judul kartu.
     * @param description keterangan singkat untuk orang awam.
     * @param akademik    skor dimensi akademik (0–100).
     * @param kehadiran   skor dimensi kehadiran (0–100).
     * @param tagihan     skor dimensi tagihan (0–100); semakin rendah semakin banyak tunggakan.
     * @param tugas       skor dimensi tugas (0–100).
     * @return div kartu donut.
     */
    public static Div spider(String title, String description, int akademik, int kehadiran,
            int tagihan, int tugas) {
        int a = clamp(akademik);
        int b = clamp(kehadiran);
        int c = clamp(tagihan);
        int d = clamp(tugas);
        int avg = (akademik + kehadiran + tagihan + tugas) / 4;
        avg = Math.max(0, Math.min(100, avg));
        Div card = card(title, description);
        Div donut = div("ais-m-spider");
        donut.setStyle(
                "background:conic-gradient("
                + "var(--ais-m-primary) 0 " + a + "%,"
                + "var(--ais-m-accent) " + a + "% " + (a + b) + "%,"
                + "#f5b942 " + (a + b) + "% " + (a + b + c) + "%,"
                + "#2f7fc1 " + (a + b + c) + "% " + (a + b + c + d) + "%,"
                + "#e8edea " + (a + b + c + d) + "% 100%)");
        donut.appendChild(label(avg + "%", "ais-m-spider-core"));
        card.appendChild(donut);
        Div legend = div("ais-m-legend");
        legend.appendChild(legendDot("var(--ais-m-primary)", "Akademik"));
        legend.appendChild(legendDot("var(--ais-m-accent)", "Kehadiran"));
        legend.appendChild(legendDot("#f5b942", "Tagihan"));
        legend.appendChild(legendDot("#2f7fc1", "Tugas"));
        card.appendChild(legend);
        return card;
    }

    private static Div legendDot(String color, String text) {
        Div row = div("ais-m-legend-row");
        Div dot = div("ais-m-legend-dot");
        dot.setStyle("background:" + color);
        row.appendChild(dot);
        row.appendChild(label(text, "ais-m-card-desc"));
        return row;
    }

    /* ===================================================================
     * Kosong dan utilitas
     * =================================================================== */

    /**
     * Membuat tampilan "kosong" bergambar dan teks penjelasan saat tidak ada data.
     *
     * @param message pesan untuk orang awam (mis. "Belum ada notifikasi.").
     * @param imageSrc URL ikon; boleh null (pakai default folder kosong).
     * @return div empty state.
     */
    public static Div empty(String message, String imageSrc) {
        String src = (imageSrc == null || imageSrc.trim().length() == 0)
                ? "/img/svg/folder2.svg" : imageSrc;
        Div empty = div("ais-m-empty");
        empty.appendChild(image(src, "ais-m-empty-ic"));
        empty.appendChild(label(
                (message == null || message.trim().length() == 0)
                        ? "Belum ada data." : message,
                "ais-m-empty-text"));
        return empty;
    }

    /**
     * Menambah spasi vertikal sebagai pemisah antara elemen.
     *
     * @param parent komponen induk.
     * @param height tinggi spasi (mis. "10px").
     */
    public static void spacer(Component parent, String height) {
        if (parent == null) {
            return;
        }
        Space sp = new Space();
        sp.setHeight(height == null ? "10px" : height);
        parent.appendChild(sp);
    }

    /**
     * Menghapus semua komponen anak sebelum render ulang.
     * Selalu panggil method ini sebelum mengisi ulang sebuah kontainer
     * agar tidak ada komponen lama yang tersisa.
     *
     * @param component kontainer yang akan dibersihkan; aman bila null.
     */
    public static void clear(Component component) {
        if (component != null && !component.getChildren().isEmpty()) {
            component.getChildren().clear();
        }
    }

    /* ===================================================================
     * Helper privat
     * =================================================================== */

    private static void appendBar(Component parent, int heightPct) {
        int h = Math.max(8, Math.min(100, heightPct));
        Div bar = div("ais-m-bar");
        bar.setStyle("height:" + h + "%");
        parent.appendChild(bar);
    }

    private static int clamp(int value) {
        if (value < 1) {
            return 1;
        }
        return Math.min(value, 70);
    }
}
