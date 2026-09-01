package ais.action.master.helper;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmrole;
import ais.database.model.Tbmuser;
import ais.ui.util.MyMessageboxConfig;

/**
 * Subclass generik-lepas dari {@link ais.action.master.helper.GenericRevisiHelper} yang berfungsi
 * ganda sebagai (1) cara cepat membuka window riwayat revisi untuk SATU ID entity apa pun (lewat
 * konstruktor yang membangun {@link GenericRevisiHelper.EntityIdFilter} secara otomatis dari
 * {@code Class} + id/entity yang diberikan — tanpa perlu subclass khusus per entity), dan (2)
 * kumpulan method static utilitas UI/data seputar fitur "lihat riwayat revisi" yang dipakai luas
 * dari banyak Action/layar lain di codebase (tombol/link "Revisi", cek hak akses, dan update satu
 * property data aktif langsung tanpa lewat window revisi penuh). Lihat Javadoc
 * {@link GenericRevisiHelper} untuk penjelasan lengkap arsitektur window, alur Envers, dan fitur
 * restore — class ini tidak menambah tab/fitur baru pada window, hanya menyediakan jalur
 * pembuatan yang lebih generik daripada subclass {@code Revisi*Helper} lain (yang masing-masing
 * terikat pada satu entity) serta beberapa utilitas independen.
 *
 * <p>Dua konstruktor tersedia: yang pertama untuk pemakaian terprogram biasa (tanpa border/closable
 * kustom, tanpa dipasang ke komponen ZK apa pun); yang kedua dipakai oleh
 * {@link #createNewRevisi(Class, GeneralValueObject, Long, String, String)} untuk membangun window
 * yang otomatis muncul modal saat komponen {@link A} (link) tertentu diklik.
 *
 * <p><b>Utilitas static penting:</b>
 * <ul>
 *   <li>{@link #createNewRevisi(Class, GeneralValueObject, String)} (dan overload-nya) — membangun
 *       {@link Vbox} berisi link "Revisi" yang membuka window {@link RevisiHelper} modal saat
 *       diklik; bila pengguna tidak punya hak akses (lihat {@link #bolehLihatRevisi()}), yang
 *       ditampilkan hanya {@link Label} biasa tanpa link.</li>
 *   <li>{@link #bolehLihatRevisi()} — mengecek hak akses lihat revisi lewat konfigurasi
 *       {@code boleh_lihat_revisi} (daftar role/user id dipisah koma, default {@code "am,amp"}).</li>
 *   <li>{@link #updatePropertyAndSave(GeneralValueObject, ClassMetadata, String, Object)} — menulis
 *       satu nilai property langsung ke data AKTIF (bukan riwayat revisi) dalam transaksi Hibernate
 *       tersendiri, dipakai oleh window "Edit" pada {@link GenericRevisiHelper} untuk fitur
 *       "Pakai"/edit manual satu kolom.</li>
 * </ul>
 */
@SuppressWarnings({ "rawtypes" })
public class RevisiHelper extends GenericRevisiHelper {

    private static final long serialVersionUID = 6589578552710016753L;

    /**
     * Membuka window riwayat revisi untuk satu entity, tanpa kustomisasi border/closable dan tanpa
     * dipasang otomatis ke komponen ZK manapun (pemanggil bertanggung jawab menambahkannya ke
     * halaman, mis. lewat {@code appendChild}).
     *
     * @param myClass class entity Hibernate yang diaudit
     * @param serializable id entity secara langsung, atau instance {@link GeneralValueObject} yang
     *                     id-nya diambil otomatis lewat {@link #resolveId(Serializable)}; boleh
     *                     {@code null} untuk menampilkan riwayat SEMUA id (tanpa
     *                     {@link GenericRevisiHelper.EntityIdFilter})
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiHelper(Class<?> myClass, Serializable serializable) throws Exception {
        super(myClass, buildTitle(myClass, null), null, null,
                serializable == null ? null : new GenericRevisiHelper.EntityIdFilter(resolveId(serializable)));
    }

    /**
     * Membuka window riwayat revisi untuk satu entity dan langsung memasang listener klik pada
     * komponen {@link A} (link) {@code a} agar window ditampilkan modal ({@link #tampilkanModal})
     * saat link diklik. Dipakai oleh {@link #createNewRevisi(Class, GeneralValueObject, Long,
     * String, String)}.
     *
     * @param a komponen link yang saat diklik akan menampilkan window ini secara modal; boleh
     *          {@code null} (tidak ada listener yang dipasang)
     * @param myClass class entity Hibernate yang diaudit
     * @param serializable id entity atau {@link GeneralValueObject} sumber id, lihat
     *                     {@link #RevisiHelper(Class, Serializable)}
     * @param title judul tambahan yang digabung dengan nama class (lihat {@link #buildTitle});
     *              bila kosong dipakai "Riwayat Revisi Data"
     * @param border nilai CSS class border ZK; bila {@code null} dipakai {@code "normal"}
     * @param closable apakah window bisa ditutup pengguna (tombol close ditampilkan)
     * @throws Exception diteruskan apa adanya dari konstruktor {@link GenericRevisiHelper}
     */
    public RevisiHelper(A a, Class<?> myClass, Serializable serializable, String title, String border, boolean closable)
            throws Exception {
        super(myClass, buildTitle(myClass, title), null, null,
                serializable == null ? null : new GenericRevisiHelper.EntityIdFilter(resolveId(serializable)));
        setBorder(border == null ? "normal" : border);
        setClosable(closable);
        if (a != null) {
            a.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    tampilkanModal(RevisiHelper.this, event);
                }
            });
        }
    }


    /**
     * Membentuk judul window: {@code title} (atau "Riwayat Revisi Data" bila kosong) digabung
     * dengan nama lengkap {@code myClass} (mis. {@code "ais.database.model.Mahasiswa"}) bila
     * tersedia. Kegagalan mengambil nama class ditelan dan judul dasar tetap dikembalikan.
     */
    private static String buildTitle(Class<?> myClass, String title) {
        String base = title == null || title.trim().length() == 0 ? "Riwayat Revisi Data" : title.trim();
        try {
            if (myClass != null && myClass.getName() != null && myClass.getName().trim().length() > 0) {
                return base + " - " + myClass.getName();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:63");
        }
        return base;
    }

    /**
     * Mengambil id dari {@code serializable}: bila berupa {@link GeneralValueObject}, dikembalikan
     * {@link GeneralValueObject#getId()}-nya; selain itu {@code serializable} itu sendiri dianggap
     * sudah berupa id dan dikembalikan apa adanya. Kegagalan ditelan dan {@code serializable}
     * dikembalikan sebagai fallback.
     */
    private static Serializable resolveId(Serializable serializable) {
        try {
            if (serializable instanceof GeneralValueObject) {
                return ((GeneralValueObject) serializable).getId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:73");
        }
        return serializable;
    }

    /** Lihat {@link #createNewRevisi(Class, GeneralValueObject, Long, String, String)} (tanpa {@code refIdLagi}/{@code style}). */
    public static Vbox createNewRevisi(final Class class1, GeneralValueObject generalValueObject, String label) {
        return createNewRevisi(class1, generalValueObject, null, label, null);
    }

    /** Lihat {@link #createNewRevisi(Class, GeneralValueObject, Long, String, String)} (tanpa {@code refIdLagi}). */
    public static Vbox createNewRevisi(final Class class1, GeneralValueObject generalValueObject, String label,
            String style) {
        return createNewRevisi(class1, generalValueObject, null, label, style);
    }

    /** Lihat {@link #createNewRevisi(Class, GeneralValueObject, Long, String, String)} (tanpa {@code style} kustom). */
    public static Vbox createNewRevisi(final Class class1, GeneralValueObject generalValueObject, Long refIdLagi,
            String label) {
        return createNewRevisi(class1, generalValueObject, refIdLagi, label, null);
    }

    /**
     * Membangun {@link Vbox} berisi tombol/link "Revisi" siap tempel ke halaman ZK mana pun. Bila
     * {@link #bolehLihatRevisi()} bernilai {@code false}, isi {@link Vbox} hanya {@link Label} teks
     * biasa (bukan link, tidak bisa diklik) — mencegah pengguna tanpa hak akses membuka window
     * revisi. Bila boleh, dibangun komponen {@link A} bergaya tombol kecil yang saat diklik:
     * memvalidasi {@code generalValueObject} sudah tersimpan (punya id, jika belum menampilkan
     * pesan lewat {@code MyMessageboxConfig}), lalu membuka {@link RevisiHelper} baru secara modal
     * ({@link #tampilkanModal}) dengan id yang dipakai adalah {@code refIdLagi} bila diberikan,
     * atau id {@code generalValueObject} sendiri.
     *
     * <p>Kegagalan tak terduga di seluruh proses ditangkap dan ditampilkan lewat
     * {@code PesanFormalHelper.tampilkanGagalException}, dengan {@link Vbox} kosong dikembalikan
     * sebagai fallback aman.
     *
     * @param class1 class entity Hibernate yang diaudit saat link diklik
     * @param generalValueObject data yang sedang ditampilkan/diedit di layar pemanggil; sumber id
     *                           default serta syarat "sudah tersimpan" sebelum window dibuka
     * @param refIdLagi id alternatif untuk dipakai sebagai filter revisi, menggantikan id
     *                  {@code generalValueObject}; boleh {@code null} untuk memakai id
     *                  {@code generalValueObject} apa adanya (kasus umum: entity yang diaudit
     *                  berbeda dari entity yang sedang ditampilkan, tapi berbagi id yang sama)
     * @param label teks tombol/link; bila kosong dipakai "Revisi"
     * @param style CSS style {@link Vbox} pembungkus; bila kosong dipakai {@code "max-width:100%;"}
     * @return {@link Vbox} berisi link/label revisi, siap ditambahkan sebagai child komponen ZK lain
     */
    public static Vbox createNewRevisi(final Class class1, final GeneralValueObject generalValueObject,
            final Long refIdLagi, String label, String style) {

        Vbox vbox = new Vbox();

        try {
            if (style != null && style.trim().length() > 0) {
                vbox.setStyle(style);
            } else {
                vbox.setStyle("max-width:100%;");
            }

            final String labelRevisi = label == null || label.trim().length() == 0 ? "Revisi" : label.trim();

            if (!bolehLihatRevisi()) {
                Label labelBiasa = new Label(labelRevisi);

                labelBiasa.setStyle(
                        "font-size:11px;"
                        + "color:black;"
                        + "white-space:normal;"
                        + "word-wrap:break-word;"
                        + "overflow-wrap:break-word;"
                        + "word-break:break-word;"
                        + "line-height:15px;"
                        + "max-width:100%;"
                        + "display:block;"
                );

                labelBiasa.setMultiline(true);
                labelBiasa.setParent(vbox);

                return vbox;
            }

            A link = new A();
            link.setLabel(labelRevisi);
            link.setTooltiptext("Lihat riwayat revisi data");

            link.setStyle(
                    "display:inline-block;"
                    + "font-size:11px;"
                    + "font-weight:bold;"
                    + "color:#2563eb;"
                    + "text-decoration:none;"
                    + "border:1px solid #bfdbfe;"
                    + "background:#eff6ff;"
                    + "border-radius:5px;"
                    + "padding:3px 7px;"
                    + "line-height:16px;"
                    + "white-space:normal;"
                    + "word-wrap:break-word;"
                    + "overflow-wrap:break-word;"
                    + "word-break:break-word;"
                    + "max-width:100%;"
                    + "cursor:pointer;"
            );

            link.setParent(vbox);

            link.addEventListener("onClick", new EventListener() {
                public void onEvent(Event event) throws Exception {
                    if (generalValueObject == null || generalValueObject.getId() == null) {
                        MyMessageboxConfig.show("Data belum tersimpan sehingga belum memiliki riwayat revisi.");
                        return;
                    }

                    Serializable id = refIdLagi == null ? generalValueObject.getId() : refIdLagi;

                    RevisiHelper helper = new RevisiHelper(class1, id);
                    tampilkanModal(helper, event);
                }
            });

        } catch (Exception e) {
            Common.tampilErrorJikaAdmin(e);
            PesanFormalHelper.tampilkanGagalException(
                    "menampilkan tautan riwayat revisi",
                    e, new String[] {
                            "Muat ulang (refresh) halaman ini lalu coba kembali.",
                            "Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
                    });
        }

        return vbox;
    }

    /**
     * Mengecek apakah pengguna saat ini berhak melihat window riwayat revisi, berdasarkan
     * konfigurasi {@code boleh_lihat_revisi} (daftar role id dan/atau user id dipisah koma,
     * default {@code "am,amp"} bila konfigurasi belum ada/gagal dibaca). Cocok bila role ATAU
     * user id pengguna saat ini ada dalam daftar (lihat {@link #containsToken(String, String)}).
     * Daftar kosong berarti tidak ada yang boleh melihat revisi.
     */
    private static boolean bolehLihatRevisi() {
        String daftar = "am,amp";
        try {
            daftar = Common.getKonfigurasi("boleh_lihat_revisi", "am,amp").getNilai().trim();
        } catch (Exception e) {
            daftar = "am,amp";
        }
        if (daftar == null || daftar.trim().length() == 0) {
            return false;
        }

        Tbmuser tbmuser = null;
        String userId = null;
        String roleId = null;
        try {
            tbmuser = Common.getCurrentUser();
            if (tbmuser != null) {
                userId = tbmuser.getUserId();
                Tbmrole role = tbmuser.hakAkses();
                roleId = role == null ? null : role.getRoleId();
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:194");
        }

        return containsToken(daftar, roleId) || containsToken(daftar, userId);
    }

    /**
     * Mengecek apakah {@code value} (setelah di-trim) muncul sebagai salah satu token dalam
     * {@code csv} (dipisah koma), perbandingan case-insensitive. Dipakai oleh
     * {@link #bolehLihatRevisi()} untuk mencocokkan role id/user id terhadap daftar konfigurasi.
     */
    private static boolean containsToken(String csv, String value) {
        if (csv == null || value == null) {
            return false;
        }
        String cleanedValue = value.trim();
        if (cleanedValue.length() == 0) {
            return false;
        }
        String[] tokens = csv.split(",");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i] == null ? "" : tokens[i].trim();
            if (token.length() > 0 && token.equalsIgnoreCase(cleanedValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Menampilkan {@code helper} sebagai window modal: bila belum punya parent/page, dicari root
     * component halaman saat ini (lewat {@code ExecutionsCtrl}, atau sebagai fallback lewat
     * {@code event.getTarget().getPage()}) untuk ditempeli window ini; bila root tidak ditemukan
     * sama sekali, ditampilkan pesan kegagalan dan window tidak jadi tampil. Setelah terpasang,
     * window dibuat visible dan {@code onModal()} dipanggil.
     */
    private static void tampilkanModal(RevisiHelper helper, Event event) throws Exception {
        if (helper == null) {
            return;
        }

        if (helper.getParent() == null && helper.getPage() == null) {
            Component root = null;
            try {
                Page page = ExecutionsCtrl.getCurrentCtrl() == null ? null
                        : ExecutionsCtrl.getCurrentCtrl().getCurrentPage();
                if (page != null) {
                    root = page.getFirstRoot();
                }
            } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:231");
            }

            if (root == null && event != null && event.getTarget() != null) {
                try {
                    Page page = event.getTarget().getPage();
                    if (page != null) {
                        root = page.getFirstRoot();
                    }
                } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:240");
                }
            }

            if (root != null) {
                root.appendChild(helper);
            } else {
                MyMessageboxConfig.show("Jendela riwayat revisi belum bisa dibuka karena halaman utama belum siap.");
                return;
            }
        }

        helper.setVisible(true);
        helper.onModal();
    }

    /**
     * Utilitas legacy: mengosongkan {@code detail} lalu menambahkan satu baris info berisi
     * label "Tanggal revisi: &lt;tanggal&gt; oleh &lt;olehId&gt;" ke dalam {@code rows}, kemudian
     * memasang {@code rows} sebagai isi {@code detail}. {@code currentObj} tidak dipakai langsung
     * di badan method saat ini (diteruskan untuk kompatibilitas signature/pemanggil lama).
     * Kegagalan ditelan dan ditampilkan lewat {@code Common.tampilErrorJikaAdmin} bila memungkinkan.
     *
     * @param detail komponen {@link MyDetail} yang akan diisi ulang; no-op bila {@code null}
     * @param rows baris ZK tujuan; no-op bila {@code null}
     * @param tanggal tanggal revisi yang ditampilkan, boleh {@code null} (dicetak kosong)
     * @param olehId id pengguna yang melakukan revisi, boleh {@code null} (dicetak kosong)
     * @param currentObj data terkait, saat ini tidak dipakai di badan method
     */
    @SuppressWarnings("deprecation")
	public static void tampilDetail(final MyDetail detail, Rows rows, final java.util.Date tanggal, final String olehId,
            final GeneralValueObject currentObj) {
        if (detail == null || rows == null) {
            return;
        }
        try {
            Common.clear(detail);
            Row info = new Row();
            info.setParent(rows);
            ais.ui.util.ZkCompat.setSpans(info, "4");
            info.appendChild(new org.zkoss.zul.Label("Tanggal revisi: "
                    + (tanggal == null ? "" : Common.datetimeFormat2s.get().format(tanggal)) + " oleh "
                    + (olehId == null ? "" : olehId)));
            rows.setParent(detail);
        } catch (Exception e) {
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:272");}
        }
    }

    /**
     * Menulis satu nilai property langsung ke data AKTIF di database (BUKAN ke riwayat revisi),
     * dalam transaksi Hibernate tersendiri yang dibuka dan ditutup sendiri oleh method ini (session
     * lokal, pola {@code openSession()} + {@code finally clear/disconnect/close} yang sama dengan
     * {@link GenericRevisiHelper}). Entity dimuat ulang lewat {@code session.get} berdasarkan
     * {@code obj.getId()} sebelum property-nya diubah lewat {@link ClassMetadata#setPropertyValue}
     * — memastikan perubahan diterapkan pada state terkini, bukan pada instance {@code obj} yang
     * mungkin sudah detached. Dipakai oleh fitur "Pakai"/edit manual satu kolom pada window
     * {@link GenericRevisiHelper} (lihat {@code restoreOneProperty}/{@code saveManualPropertyValue}
     * di kelas induk).
     *
     * <p>Bila {@code obj}, id-nya, {@code meta}, atau {@code prop} {@code null}, method langsung
     * kembali tanpa melakukan apa pun. Kegagalan menyebabkan transaksi di-rollback dan pesan error
     * ditampilkan lewat {@code Common.tampilErrorJikaAdmin}; exception tidak dilempar ke pemanggil.
     *
     * @param obj entity yang property-nya akan diubah (dipakai untuk class dan id, bukan instance
     *            yang langsung disimpan)
     * @param meta metadata Hibernate entity tersebut, dipakai untuk {@code setPropertyValue}
     * @param prop nama property yang diubah
     * @param newVal nilai baru untuk property tersebut
     */
    public static void updatePropertyAndSave(GeneralValueObject obj, ClassMetadata meta, String prop, Object newVal) {
        Session session = null;
        Transaction tx = null;
        try {
            if (obj == null || obj.getId() == null || meta == null || prop == null) {
                return;
            }
            session = HibernateUtil.getSessionFactory().openSession();
            tx = session.beginTransaction();
            Object current = session.get(obj.getClass(), obj.getId());
            if (current != null) {
                meta.setPropertyValue(current, prop, newVal, EntityMode.POJO);
                session.saveOrUpdate(current);
            }
            tx.commit();
        } catch (Exception e) {
            try { if (tx != null && tx.isActive()) tx.rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:292");}
            try { Common.tampilErrorJikaAdmin(e); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:293");}
        } finally {
            if (session != null) {
                try { session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:296");}
                try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:297");}
                try { session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/RevisiHelper.java:298");}
            }
        }
    }
}
