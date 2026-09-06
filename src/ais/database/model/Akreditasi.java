package ais.database.model;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.envers.Audited;

import ais.common.Common;
import ais.database.model.rab.SatuanKerja;

/**
 * Entitas Hibernate untuk tabel {@code public.akreditasi}, merepresentasikan satu baris
 * akreditasi/sertifikasi/audit eksternal yang diperoleh institusi, satuan kerja, program
 * studi (jurusan), atau dosen perorangan. Satu baris mencakup lembaga pemberi
 * ({@link #getLembaga()}), lingkup ({@link #getLingkup()}: PT/Fakultas/Unit), tingkat
 * ({@link #getTingkat()}: Lokal/Nasional/Internasional), peringkat yang diperoleh, masa
 * berlaku ({@link #getMulai()}&ndash;{@link #getSampai()}), serta jenis dokumen — lihat
 * konstanta {@link #JENIS_EKSTERNAL}, {@link #JENIS_INTERNASIONAL},
 * {@link #JENIS_EKSTERNAL_KEUANGAN}, dan {@link #DOKUMEN} (nilai default {@link #getJenis()}
 * bila kosong, dan dipaksa bila baris ini terkait {@link #getDosen()}).
 * <p>
 * Relasi {@code @ManyToOne} (lazy) opsional ke {@link Jurusan} (program studi terkait),
 * {@link Dosen} (bila akreditasi/sertifikasi ini melekat pada dosen perorangan, bukan
 * institusi/unit), dan {@link ais.database.model.rab.SatuanKerja} (unit kerja terkait, modul
 * RAB/anggaran).
 * <p>
 * Perubahan (create/update) tercatat historisnya lewat anotasi {@link Audited} (Hibernate
 * Envers), dan setiap update otomatis memperbarui {@link #getTanggal_dirubah()} lewat callback
 * {@link javax.persistence.PreUpdate} yang memanggil
 * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "public", name = "akreditasi")
public class Akreditasi extends GeneralValueObject {

    private static final long serialVersionUID = 2463821577548439808L;

    public static final String JENIS_EKSTERNAL = "Sertifikasi/Akreditasi Eksternal";
    public static final String JENIS_INTERNASIONAL = "Akreditasi Internasional Program Studi";
    public static final String JENIS_EKSTERNAL_KEUANGAN = "Audit Eksternal Keuangan";
    public static final String DOKUMEN = "Dokumen";

    public static final List<String> LINGKUP = new ArrayList<String>();
    public static final List<String> TINGKAT = new ArrayList<String>();
    public static final List<String> JENIS = new ArrayList<String>();

    static {
        TINGKAT.add("Lokal");
        TINGKAT.add("Nasional");
        TINGKAT.add("Internasional");

        LINGKUP.add("PT");
        LINGKUP.add("Fakultas");
        LINGKUP.add("Unit");

        JENIS.add(JENIS_EKSTERNAL);
        JENIS.add(JENIS_INTERNASIONAL);
        JENIS.add(JENIS_EKSTERNAL_KEUANGAN);
        JENIS.add(DOKUMEN);
    }

    /** Primary key entity (kolom {@code id}, identity/auto-increment). */
    private Long id;
    /**
     * Nama pengguna pengubah terakhir. Field ini MENIMPA (shadow) field bernama sama pada
     * {@link GeneralValueObject}; getter/setter di bawah beroperasi pada field lokal ini.
     */
    private String oleh;
    /** Id pengguna pengubah terakhir; shadow dari field sama pada {@link GeneralValueObject}. */
    private String olehId;
    /** Stempel waktu perubahan terakhir; diinisialisasi ke saat object dibuat. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /** Nama/judul akreditasi atau sertifikasi. */
    private String nama;
    /** Nama lembaga pemberi akreditasi/sertifikasi. */
    private String lembaga;
    /** Lingkup akreditasi; salah satu nilai {@link #LINGKUP} (PT/Fakultas/Unit). */
    private String lingkup;
    /** Tingkat akreditasi; salah satu nilai {@link #TINGKAT} (Lokal/Nasional/Internasional). */
    private String tingkat;
    /** Peringkat/predikat yang diperoleh (mis. "A", "Unggul"). */
    private String peringkat;
    /** Daftar kode grup pengguna berwenang, dipisah koma; lihat {@link #getKodeGrupPengguna()}. */
    private String kodeGrupPengguna;
    /** Masa berlaku dalam teks bebas (pelengkap {@link #mulai}/{@link #sampai}). */
    private String masaberlaku;
    /** Tahun perolehan; lihat {@link #getTahun()} untuk perilaku penjagaan nilai usang. */
    private Integer tahun;
    /** Opini/catatan hasil audit (relevan untuk {@link #JENIS_EKSTERNAL_KEUANGAN}). */
    private String opini;
    /** Keterangan bebas. */
    private String keterangan;
    /** Tanggal mulai berlaku. */
    private Date mulai;
    /** Tanggal akhir berlaku. */
    private Date sampai;
    /** Menandai baris ini masih aktif/berlaku; lihat {@link #getAktif()} untuk perilaku default. */
    private Boolean aktif;
    /** Jurusan/program studi terkait (opsional, relevan untuk lingkup Program Studi). */
    private Jurusan jurusan;
    /** Dosen pemilik akreditasi/sertifikasi ini bila melekat pada perorangan (bukan institusi/unit). */
    private Dosen dosen;
    /** Jenis dokumen/akreditasi; lihat {@link #getJenis()} untuk perilaku default dan override. */
    private String jenis;
    /** Satuan kerja terkait (modul RAB/anggaran), opsional. */
    private SatuanKerja satuanKerja;

    /** Konstruktor kosong, dipakai Hibernate. */
    public Akreditasi() {
    }

    /**
     * Membangun daftar jenis dokumen DMS: dimulai dari konstanta {@link #JENIS} baku, lalu
     * ditambahkan jenis TAMBAHAN yang dikonfigurasi lewat {@code Common.getKonfigurasi
     * ("jenis_dokumen_dms_tambahan")} (dipisah {@code ;}, {@code ,}, {@code |}, atau baris baru).
     * Duplikat (case-insensitive) diabaikan lewat {@link #tambahJikaBelumAda(List, String)}.
     * Kegagalan membaca konfigurasi dicatat lewat {@code ErrorAuditUtil} dan diabaikan -- daftar
     * baku {@link #JENIS} tetap dikembalikan.
     *
     * @return daftar jenis dokumen DMS, tidak pernah {@code null}.
     */
    public static List<String> jenisDokumenDms() {
        List<String> data = new ArrayList<String>();
        for (String s : JENIS) {
            tambahJikaBelumAda(data, s);
        }
        try {
            String tambahan = Common.getKonfigurasi("jenis_dokumen_dms_tambahan", "").getNilai();
            if (tambahan != null && !tambahan.trim().isEmpty()) {
                String normalized = tambahan.replace('\n', ';').replace('\r', ';').replace('|', ';').replace(',', ';');
                String[] values = normalized.split(";");
                for (int i = 0; i < values.length; i++) {
                    tambahJikaBelumAda(data, values[i]);
                }
            }
        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Akreditasi.java:98");
        }
        return data;
    }

    /**
     * Menambahkan {@code value} (di-{@code trim}) ke {@code data} bila belum ada entri yang sama
     * secara case-insensitive; nilai kosong/{@code null} diabaikan diam-diam.
     *
     * @param data  daftar tujuan, dimutasi langsung.
     * @param value nilai kandidat yang akan ditambahkan.
     */
    private static void tambahJikaBelumAda(List<String> data, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        String clean = value.trim();
        for (String s : data) {
            if (s != null && s.equalsIgnoreCase(clean)) {
                return;
            }
        }
        data.add(clean);
    }

    /** @return primary key entity, atau {@code null} bila belum tersimpan. */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() {
        return id;
    }

    /** @param id primary key baru. */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return id pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi.
     * @see GeneralValueObject#getOlehId()
     */
    public String getOlehId() {
        return olehId;
    }

    /**
     * Menyetel id pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
     * sama seperti {@link GeneralValueObject#setOlehId(String)}.
     */
    public void setOlehId(String olehId) {
        if (olehId == null || olehId.trim().isEmpty()) {
            return;
        }
        this.olehId = olehId;
    }

    /** @return nama pengguna pengubah terakhir, atau {@code null} bila belum pernah terisi. */
    public String getOleh() {
        return oleh;
    }

    /**
     * Menyetel nama pengguna pengubah terakhir. Nilai {@code null}/kosong diabaikan diam-diam,
     * sama seperti {@link #setOlehId(String)}.
     */
    public void setOleh(String oleh) {
        if (oleh == null || oleh.trim().isEmpty()) {
            return;
        }
        this.oleh = oleh;
    }

    /**
     * Callback JPA sebelum UPDATE: memperbarui {@link #tanggal_dirubah} lewat
     * {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() {
        ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
    }

    /** @return stempel waktu perubahan terakhir. */
    @Temporal(TemporalType.TIMESTAMP)
    public Date getTanggal_dirubah() {
        return tanggal_dirubah;
    }

    /** @param tanggal_dirubah stempel waktu perubahan terakhir yang baru. */
    public void setTanggal_dirubah(Date tanggal_dirubah) {
        this.tanggal_dirubah = tanggal_dirubah;
    }

    /** @return representasi ringkas untuk debug/log: {@code "<id>-<nama>"}. */
    @Override
    public String toString() {
        return (id == null ? "" : id.toString()) + "-" + safe(nama);
    }

    /** @return nama/judul akreditasi, sudah di-{@code trim}; {@code null} bila belum diisi. */
    @Column(name = "nama", columnDefinition = "text")
    public String getNama() {
        return nama == null ? null : nama.trim();
    }

    /** @param nama nama/judul akreditasi baru. */
    public void setNama(String nama) {
        this.nama = nama;
    }

    /** @return keterangan bebas, string kosong (bukan {@code null}) bila belum diisi. */
    @Column(name = "keterangan", nullable = true, columnDefinition = "text")
    public String getKeterangan() {
        return keterangan == null ? "" : keterangan;
    }

    /** @param keterangan keterangan bebas yang baru. */
    public void setKeterangan(String keterangan) {
        this.keterangan = keterangan;
    }

    /** @return nama lembaga pemberi, string kosong (bukan {@code null}) bila belum diisi. */
    @Column(columnDefinition = "text")
    public String getLembaga() {
        return lembaga == null ? "" : lembaga;
    }

    /** @param lembaga nama lembaga pemberi yang baru. */
    public void setLembaga(String lembaga) {
        this.lembaga = lembaga;
    }

    /** @return tingkat akreditasi (lihat {@link #TINGKAT}), string kosong bila belum diisi. */
    @Column(columnDefinition = "text")
    public String getTingkat() {
        return tingkat == null ? "" : tingkat;
    }

    /** @param tingkat tingkat akreditasi baru. */
    public void setTingkat(String tingkat) {
        this.tingkat = tingkat;
    }

    /** @return lingkup akreditasi (lihat {@link #LINGKUP}), string kosong bila belum diisi. */
    @Column(columnDefinition = "text")
    public String getLingkup() {
        return lingkup == null ? "" : lingkup;
    }

    /** @param lingkup lingkup akreditasi baru. */
    public void setLingkup(String lingkup) {
        this.lingkup = lingkup;
    }

    /** @return masa berlaku (teks bebas), string kosong bila belum diisi. */
    public String getMasaberlaku() {
        return masaberlaku == null ? "" : masaberlaku;
    }

    /** @param masaberlaku masa berlaku (teks bebas) baru. */
    public void setMasaberlaku(String masaberlaku) {
        this.masaberlaku = masaberlaku;
    }

    /** @return tanggal mulai berlaku, boleh {@code null}. */
    @Temporal(TemporalType.DATE)
    public Date getMulai() {
        return mulai;
    }

    /** @param mulai tanggal mulai berlaku yang baru. */
    public void setMulai(Date mulai) {
        this.mulai = mulai;
    }

    /** @return tanggal akhir berlaku, boleh {@code null}. */
    @Temporal(TemporalType.DATE)
    public Date getSampai() {
        return sampai;
    }

    /** @param sampai tanggal akhir berlaku yang baru. */
    public void setSampai(Date sampai) {
        this.sampai = sampai;
    }

    /** Jenis akreditasi/dokumen (lihat konstanta {@link #JENIS}); default {@link #DOKUMEN}, dan selalu {@link #DOKUMEN} bila baris ini terkait {@link #getDosen()}. */
    public String getJenis() {
        if (dosen != null) {
            return DOKUMEN;
        }
        return jenis == null || jenis.trim().isEmpty() ? DOKUMEN : jenis.trim();
    }

    /** @param jenis jenis akreditasi/dokumen baru; diabaikan efeknya bila {@link #getDosen()} terisi. */
    public void setJenis(String jenis) {
        this.jenis = jenis;
    }

    /** @return tahun perolehan; {@code null} bila belum diisi ATAU nilainya {@code <= 1800} (dianggap data usang/tidak valid). */
    public Integer getTahun() {
        return tahun == null || tahun.intValue() <= 1800 ? null : tahun;
    }

    /** @param tahun tahun perolehan baru. */
    public void setTahun(Integer tahun) {
        this.tahun = tahun;
    }

    /** @return opini/catatan hasil audit, string kosong bila belum diisi. */
    @Column(columnDefinition = "text")
    public String getOpini() {
        return opini == null ? "" : opini;
    }

    /** @param opini opini/catatan hasil audit yang baru. */
    public void setOpini(String opini) {
        this.opini = opini;
    }

    /** @return jurusan/program studi terkait, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "jurusan")
    public Jurusan getJurusan() {
        jurusan = check(jurusan);
        return jurusan;
    }

    /** @param jurusan jurusan/program studi terkait yang baru. */
    public void setJurusan(Jurusan jurusan) {
        this.jurusan = jurusan;
    }

    /** @return peringkat/predikat yang diperoleh, string kosong bila belum diisi. */
    public String getPeringkat() {
        return peringkat == null ? "" : peringkat;
    }

    /** @param peringkat peringkat/predikat baru. */
    public void setPeringkat(String peringkat) {
        this.peringkat = peringkat;
    }

    /** @return {@code true} (default) bila baris ini masih aktif/berlaku. */
    public Boolean getAktif() {
        return aktif == null ? Boolean.TRUE : aktif;
    }

    /** @param aktif penanda aktif yang baru. */
    public void setAktif(Boolean aktif) {
        this.aktif = aktif;
    }

    /** @return dosen pemilik akreditasi ini (bila melekat perorangan), boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "dosen", nullable = true)
    public Dosen getDosen() {
        dosen = check(dosen);
        return dosen;
    }

    /** @param dosen dosen pemilik yang baru. */
    public void setDosen(Dosen dosen) {
        this.dosen = dosen;
    }

    /** Daftar kode grup pengguna (dipisah koma, dinormalisasi lewat {@link #normalizeCommaText(String)}) yang berhak melihat/mengelola baris ini; kosong bila terkait {@link #getDosen()}. */
    public String getKodeGrupPengguna() {
        if (getDosen() != null) {
            return "";
        }
        kodeGrupPengguna = normalizeCommaText(kodeGrupPengguna);
        return kodeGrupPengguna;
    }

    /** @param kodeGrupPengguna daftar kode grup pengguna baru; akan dinormalkan ulang pada pemanggilan {@link #getKodeGrupPengguna()} berikutnya. */
    public void setKodeGrupPengguna(String kodeGrupPengguna) {
        this.kodeGrupPengguna = kodeGrupPengguna;
    }

    /** @return satuan kerja terkait, boleh {@code null}; dimuat lewat {@link GeneralValueObject#check(Object)}. */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "satuan_kerja", nullable = true)
    public SatuanKerja getSatuanKerja() {
        satuanKerja = check(satuanKerja);
        return satuanKerja;
    }

    /** @param satuanKerja satuan kerja terkait yang baru. */
    public void setSatuanKerja(SatuanKerja satuanKerja) {
        this.satuanKerja = satuanKerja;
    }

    /**
     * null-safe trim, dipakai {@link #toString()} agar nama {@code null} tidak memunculkan literal
     * {@code "null"} pada representasi ringkas.
     *
     * @param value teks masukan; boleh {@code null}.
     * @return teks hasil {@code trim}, atau string kosong bila masukannya {@code null}.
     */
    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Menormalkan daftar kode dipisah koma: mengganti {@code ;}/{@code |} menjadi {@code ,},
     * membuang entri kosong, dan menghapus duplikat, lalu membungkus hasil dengan koma di kedua
     * ujung (mis. {@code ",a,b,"}) agar pencarian substring {@code ",kode,"} pada pemanggil aman
     * dari kecocokan parsial.
     *
     * @param value teks daftar kode mentah; boleh {@code null}/kosong.
     * @return teks ternormalisasi dibungkus koma di kedua ujung, atau string kosong bila hasilnya
     *         tidak berisi kode apa pun.
     */
    private static String normalizeCommaText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String trimmed = value.trim().replace(';', ',').replace('|', ',');
        String[] items = trimmed.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            String item = items[i] == null ? "" : items[i].trim();
            if (item.length() == 0) {
                continue;
            }
            if (sb.indexOf("," + item + ",") < 0) {
                sb.append(',').append(item).append(',');
            }
        }
        String result = sb.toString().replace(",,", ",");
        if (",".equals(result)) {
            return "";
        }
        return result;
    }
}
