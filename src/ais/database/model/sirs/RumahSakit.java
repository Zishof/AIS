package ais.database.model.sirs;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.envers.Audited;

import ais.database.model.VoKunci;
import ais.database.model.Tbmuser;
import ais.database.model.sop.DisposisiSop;

/**
 * Profil tenant fasilitas kesehatan untuk website dan branding eMedic.
 * Nama historis RumahSakit mencakup RS, Puskesmas, Posyandu, Klinik,
 * praktik mandiri, laboratorium, apotek, dan fasilitas kesehatan lain.
 *
 * <p><b>Bukan discriminator tenant modul {@code sirs}:</b> dikonfirmasi pada batch 100 (klaster
 * Pasien) dan diverifikasi ulang di sini — TIDAK ADA satu pun entity di paket
 * {@code ais.database.model.sirs} yang memiliki foreign key ke {@link RumahSakit}. Entity ini
 * murni data profil/branding satu fasilitas untuk keperluan tampilan website publik (nama,
 * alamat, kontak, CSS/warna tema, deskripsi), TIDAK dipakai sebagai sekat data multi-tenant untuk
 * data klinis (pasien, pendaftaran, tempat tidur, dsb.). Modul {@code sirs} secara keseluruhan
 * tidak memiliki sumbu tenant/satuan-kerja sama sekali (pola berulang yang sudah dikonfirmasi
 * pada audit sebelumnya, {@code task_90bbdd51}); {@link RumahSakit} tidak mengubah kesimpulan itu
 * karena memang tidak direferensikan oleh entity data klinis mana pun.</p>
 *
 * <p>Berbeda dari kebanyakan entity {@code sirs} lain di paket ini yang mewarisi
 * {@link ais.database.model.GeneralValueObject} (dengan pola getter {@code check(...)} dan field
 * generik {@code oleh}/{@code olehId}/{@code tanggal_dirubah} yang identik), class ini mewarisi
 * {@link VoKunci} — varian value object yang membawa tambahan mekanisme "kunci" baris
 * ({@link #getDikunci()}) untuk mencegah pengeditan bersamaan oleh lebih dari satu pengguna.</p>
 *
 * @see VoKunci induk value object yang membawa mekanisme kunci baris {@link #getDikunci()}
 * @see Tbmuser pengguna yang sedang mengunci baris profil ini untuk diedit
 * @see DisposisiSop SOP disposisi terkait fasilitas ini
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "rumah_sakit")
public class RumahSakit extends VoKunci {
    private static final long serialVersionUID = 1L;

    /** Nilai {@link #getJenisFasilitas()} untuk fasilitas berjenis Rumah Sakit (juga default bila kosong). */
    public static final String JENIS_RUMAH_SAKIT = "RUMAH_SAKIT";
    /** Nilai {@link #getJenisFasilitas()} untuk fasilitas berjenis Puskesmas. */
    public static final String JENIS_PUSKESMAS = "PUSKESMAS";
    /** Nilai {@link #getJenisFasilitas()} untuk fasilitas berjenis Posyandu. */
    public static final String JENIS_POSYANDU = "POSYANDU";
    /** Nilai {@link #getJenisFasilitas()} untuk fasilitas berjenis Klinik. */
    public static final String JENIS_KLINIK = "KLINIK";
    /** Nilai {@link #getJenisFasilitas()} untuk fasilitas berjenis praktik mandiri (dokter/bidan perorangan). */
    public static final String JENIS_PRAKTIK_MANDIRI = "PRAKTIK_MANDIRI";
    /** Nilai {@link #getJenisFasilitas()} untuk fasilitas berjenis laboratorium kesehatan. */
    public static final String JENIS_LABORATORIUM = "LABORATORIUM";
    /** Nilai {@link #getJenisFasilitas()} untuk fasilitas berjenis apotek. */
    public static final String JENIS_APOTEK = "APOTEK";
    /** Nilai {@link #getJenisFasilitas()} untuk fasilitas jenis lain yang tidak tercakup konstanta di atas. */
    public static final String JENIS_LAINNYA = "LAINNYA";

    /** Nilai {@link #getPiilhanTampilan()} untuk tema tampilan default (juga default bila kolom kosong). */
    public static final String TAMPILAN_DEFAULT = "default";
    /** Nilai {@link #getPiilhanTampilan()} untuk tema tampilan klasik. */
    public static final String TAMPILAN_KLASIK = "klasik";
    /** Nilai {@link #getPiilhanTampilan()} untuk tema tampilan baru. */
    public static final String TAMPILAN_BARU = "baru";

    /** Primary key tabel {@code sirs.rumah_sakit}. Lihat {@link #getId()}. */
    private Long id;
    /** Kode singkat fasilitas. Lihat {@link #getKode()}. */
    private String kode;
    /** Jenis fasilitas kesehatan (RS/Puskesmas/Posyandu/dst). Lihat {@link #getJenisFasilitas()}. */
    private String jenisFasilitas;
    /** Nama lengkap fasilitas. Lihat {@link #getNama()}. */
    private String nama;
    /** Nama singkat/akronim fasilitas. Lihat {@link #getNamaSingkat()}. */
    private String namaSingkat;
    /** Alamat lengkap fasilitas. Lihat {@link #getAlamat()}. */
    private String alamat;
    /** Nomor telepon fasilitas. Lihat {@link #getTelepon()}. */
    private String telepon;
    /** Nomor WhatsApp fasilitas. Lihat {@link #getWhatsapp()}. */
    private String whatsapp;
    /** Alamat email fasilitas. Lihat {@link #getEmail()}. */
    private String email;
    /** Alamat website fasilitas. Lihat {@link #getWebsite()}. */
    private String website;
    /** Domain kustom fasilitas untuk website eMedic, harus unik antar fasilitas. Lihat {@link #getDomain()}. */
    private String domain;
    /** Motto/slogan fasilitas. Lihat {@link #getMotto()}. */
    private String motto;
    /** Deskripsi panjang fasilitas untuk halaman profil website. Lihat {@link #getDeskripsi()}. */
    private String deskripsi;
    /** Nomor izin operasional resmi fasilitas. Lihat {@link #getNomorIzinOperasional()}. */
    private String nomorIzinOperasional;
    /** CSS kustom tambahan untuk tema website fasilitas. Lihat {@link #getCss()}. */
    private String css;
    /** Kode warna tema utama website fasilitas. Lihat {@link #getWarna()}. */
    private String warna;
    /** Pilihan tema tampilan website. Lihat {@link #getPiilhanTampilan()}. */
    private String pilihanTampilan;
    /** Flag aktif/nonaktif fasilitas, default {@code true} bila kosong. Lihat {@link #getAktif()}. */
    private Boolean aktif;
    /** Nama pengguna yang terakhir mengubah baris ini. Lihat {@link #getOleh()}. */
    private String oleh;
    /** Identifier pengguna yang terakhir mengubah baris ini. Lihat {@link #getOlehId()}. */
    private String olehId;
    /** Cap waktu perubahan terakhir, default waktu objek dibuat di memori. Lihat {@link #getTanggal_dirubah()}. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();
    /** Pengguna yang sedang mengunci baris ini untuk diedit (warisan {@link VoKunci}). Lihat {@link #getDikunci()}. */
    private Tbmuser dikunci;
    /** SOP disposisi terkait fasilitas ini. Lihat {@link #getDisposisiSop()}. */
    private DisposisiSop disposisiSop;

    /**
     * Mengembalikan primary key baris ini.
     *
     * @return ID fasilitas, atau {@code null} untuk instance yang belum tersimpan
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    /**
     * Menetapkan primary key baris ini. Kolom bertanda {@code insertable = false} pada
     * pemetaan — nilai sesungguhnya berasal dari {@code IDENTITY} basis data saat
     * {@code INSERT}.
     *
     * @param id ID baru
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Mengembalikan kode singkat fasilitas, dibersihkan lewat {@link #clean(String)} sehingga
     * tidak pernah {@code null} (mengembalikan string kosong bila belum diisi).
     *
     * @return kode fasilitas, tidak pernah {@code null}
     */
    @Column(name = "kode", length = 50)
    public String getKode() { return clean(kode); }
    /**
     * Menetapkan kode singkat fasilitas.
     *
     * @param kode kode baru, boleh {@code null}, maksimal 50 karakter di kolom basis data
     */
    public void setKode(String kode) { this.kode = kode; }

    /**
     * Mengembalikan jenis fasilitas kesehatan, dinormalisasi ke huruf kapital dan di-default ke
     * {@link #JENIS_RUMAH_SAKIT} bila kolom kosong/belum diisi. Nilai konstan yang valid:
     * {@link #JENIS_RUMAH_SAKIT}, {@link #JENIS_PUSKESMAS}, {@link #JENIS_POSYANDU},
     * {@link #JENIS_KLINIK}, {@link #JENIS_PRAKTIK_MANDIRI}, {@link #JENIS_LABORATORIUM},
     * {@link #JENIS_APOTEK}, {@link #JENIS_LAINNYA} — namun kolom ini adalah {@code String}
     * bebas, tidak ada enum Java maupun constraint basis data yang membatasi nilai lain.
     *
     * @return jenis fasilitas dalam huruf kapital, tidak pernah {@code null}/kosong
     */
    @Column(name = "jenis_fasilitas", nullable = false, length = 40)
    public String getJenisFasilitas() {
        return clean(jenisFasilitas).length() == 0 ? JENIS_RUMAH_SAKIT : jenisFasilitas.trim().toUpperCase();
    }
    /**
     * Menetapkan jenis fasilitas kesehatan. Nilai disimpan apa adanya (tanpa normalisasi huruf
     * kapital) — normalisasi baru terjadi saat dibaca lewat {@link #getJenisFasilitas()}.
     *
     * @param jenisFasilitas jenis baru, sebaiknya salah satu konstanta {@code JENIS_*}
     */
    public void setJenisFasilitas(String jenisFasilitas) { this.jenisFasilitas = jenisFasilitas; }

    /**
     * Mengembalikan nama lengkap fasilitas, dibersihkan lewat {@link #clean(String)}.
     *
     * @return nama fasilitas, tidak pernah {@code null}
     */
    @Column(name = "nama", nullable = false, length = 180)
    public String getNama() { return clean(nama); }
    /**
     * Menetapkan nama lengkap fasilitas. Kolom wajib diisi di basis data
     * ({@code nullable = false}), tetapi setter ini sendiri tidak melakukan validasi apa pun.
     *
     * @param nama nama baru, maksimal 180 karakter di kolom basis data
     */
    public void setNama(String nama) { this.nama = nama; }

    /**
     * Mengembalikan nama singkat/akronim fasilitas, dibersihkan lewat {@link #clean(String)}.
     *
     * @return nama singkat, tidak pernah {@code null}
     */
    @Column(name = "nama_singkat", length = 80)
    public String getNamaSingkat() { return clean(namaSingkat); }
    /**
     * Menetapkan nama singkat/akronim fasilitas.
     *
     * @param namaSingkat nama singkat baru, boleh {@code null}, maksimal 80 karakter
     */
    public void setNamaSingkat(String namaSingkat) { this.namaSingkat = namaSingkat; }

    /**
     * Mengembalikan alamat lengkap fasilitas, dibersihkan lewat {@link #clean(String)}.
     *
     * @return alamat, tidak pernah {@code null}
     */
    @Column(name = "alamat", length = 300)
    public String getAlamat() { return clean(alamat); }
    /**
     * Menetapkan alamat lengkap fasilitas.
     *
     * @param alamat alamat baru, boleh {@code null}, maksimal 300 karakter
     */
    public void setAlamat(String alamat) { this.alamat = alamat; }

    /**
     * Mengembalikan nomor telepon fasilitas, dibersihkan lewat {@link #clean(String)}.
     *
     * @return nomor telepon, tidak pernah {@code null}
     */
    @Column(name = "telepon", length = 80)
    public String getTelepon() { return clean(telepon); }
    /**
     * Menetapkan nomor telepon fasilitas.
     *
     * @param telepon nomor telepon baru, boleh {@code null}, maksimal 80 karakter
     */
    public void setTelepon(String telepon) { this.telepon = telepon; }

    /**
     * Mengembalikan nomor WhatsApp fasilitas, dibersihkan lewat {@link #clean(String)}.
     *
     * @return nomor WhatsApp, tidak pernah {@code null}
     */
    @Column(name = "whatsapp", length = 80)
    public String getWhatsapp() { return clean(whatsapp); }
    /**
     * Menetapkan nomor WhatsApp fasilitas.
     *
     * @param whatsapp nomor WhatsApp baru, boleh {@code null}, maksimal 80 karakter
     */
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }

    /**
     * Mengembalikan alamat email fasilitas, dibersihkan lewat {@link #clean(String)}.
     *
     * @return alamat email, tidak pernah {@code null}
     */
    @Column(name = "email", length = 255)
    public String getEmail() { return clean(email); }
    /**
     * Menetapkan alamat email fasilitas.
     *
     * @param email email baru, boleh {@code null}, maksimal 255 karakter
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Mengembalikan alamat website fasilitas, dibersihkan lewat {@link #clean(String)}.
     *
     * @return alamat website, tidak pernah {@code null}
     */
    @Column(name = "website", length = 200)
    public String getWebsite() { return clean(website); }
    /**
     * Menetapkan alamat website fasilitas.
     *
     * @param website alamat website baru, boleh {@code null}, maksimal 200 karakter
     */
    public void setWebsite(String website) { this.website = website; }

    /**
     * Mengembalikan domain kustom fasilitas untuk website eMedic, dibersihkan lewat
     * {@link #clean(String)}. Kolom ini bertanda {@code unique} di basis data — dua fasilitas
     * tidak boleh berbagi domain yang sama, meski method ini sendiri tidak melakukan
     * pengecekan duplikasi (constraint ditegakkan basis data saat simpan).
     *
     * @return domain, tidak pernah {@code null}
     */
    @Column(name = "domain", unique = true, length = 500)
    public String getDomain() { return clean(domain); }
    /**
     * Menetapkan domain kustom fasilitas.
     *
     * @param domain domain baru, boleh {@code null}, maksimal 500 karakter, harus unik di
     *               basis data
     */
    public void setDomain(String domain) { this.domain = domain; }

    /**
     * Mengembalikan motto/slogan fasilitas, dibersihkan lewat {@link #clean(String)}.
     *
     * @return motto, tidak pernah {@code null}
     */
    @Column(name = "motto", length = 300)
    public String getMotto() { return clean(motto); }
    /**
     * Menetapkan motto/slogan fasilitas.
     *
     * @param motto motto baru, boleh {@code null}, maksimal 300 karakter
     */
    public void setMotto(String motto) { this.motto = motto; }

    /**
     * Mengembalikan deskripsi panjang fasilitas untuk halaman profil website, dibersihkan lewat
     * {@link #clean(String)}. Kolom bertipe {@code text} di basis data (tanpa batas panjang
     * praktis).
     *
     * @return deskripsi, tidak pernah {@code null}
     */
    @Column(name = "deskripsi", columnDefinition = "text")
    public String getDeskripsi() { return clean(deskripsi); }
    /**
     * Menetapkan deskripsi panjang fasilitas.
     *
     * @param deskripsi deskripsi baru, boleh {@code null}
     */
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    /**
     * Mengembalikan nomor izin operasional resmi fasilitas, dibersihkan lewat
     * {@link #clean(String)}.
     *
     * @return nomor izin operasional, tidak pernah {@code null}
     */
    @Column(name = "nomor_izin_operasional", length = 100)
    public String getNomorIzinOperasional() { return clean(nomorIzinOperasional); }
    /**
     * Menetapkan nomor izin operasional resmi fasilitas.
     *
     * @param nomorIzinOperasional nomor izin baru, boleh {@code null}, maksimal 100 karakter
     */
    public void setNomorIzinOperasional(String nomorIzinOperasional) { this.nomorIzinOperasional = nomorIzinOperasional; }

    /**
     * Mengembalikan CSS kustom tambahan untuk tema website fasilitas, dibersihkan lewat
     * {@link #clean(String)}.
     *
     * @return CSS kustom, tidak pernah {@code null}
     */
    @Column(name = "css", length = 150)
    public String getCss() { return clean(css); }
    /**
     * Menetapkan CSS kustom tambahan untuk tema website fasilitas.
     *
     * @param css CSS baru, boleh {@code null}, maksimal 150 karakter
     */
    public void setCss(String css) { this.css = css; }

    /**
     * Mengembalikan kode warna tema utama website fasilitas, dibersihkan lewat
     * {@link #clean(String)}.
     *
     * @return kode warna, tidak pernah {@code null}
     */
    @Column(name = "warna", length = 20)
    public String getWarna() { return clean(warna); }
    /**
     * Menetapkan kode warna tema utama website fasilitas.
     *
     * @param warna kode warna baru, boleh {@code null}, maksimal 20 karakter
     */
    public void setWarna(String warna) { this.warna = warna; }

    /**
     * Mengembalikan pilihan tema tampilan website, di-default ke {@link #TAMPILAN_DEFAULT} bila
     * kolom kosong. Nilai konstan yang tersedia: {@link #TAMPILAN_DEFAULT},
     * {@link #TAMPILAN_KLASIK}, {@link #TAMPILAN_BARU} — kolom ini {@code String} bebas, tidak
     * dibatasi enum Java maupun constraint basis data.
     *
     * <p><b>Catatan penamaan:</b> nama method ini {@code getPiilhanTampilan()}/
     * {@code setPiilhanTampilan(...)} mengandung salah ketik ("Piilhan" alih-alih "Pilihan") yang
     * sudah menjadi bagian kontrak API kelas ini — jangan mengganti ejaannya tanpa menelusuri
     * seluruh pemanggil.</p>
     *
     * @return pilihan tampilan, tidak pernah {@code null}/kosong
     */
    @Column(name = "pilihan_tampilan", length = 30)
    public String getPiilhanTampilan() {
        return clean(pilihanTampilan).length() == 0 ? TAMPILAN_DEFAULT : pilihanTampilan.trim();
    }
    /**
     * Menetapkan pilihan tema tampilan website.
     *
     * @param pilihanTampilan pilihan baru, sebaiknya salah satu konstanta {@code TAMPILAN_*}
     */
    public void setPiilhanTampilan(String pilihanTampilan) { this.pilihanTampilan = pilihanTampilan; }

    /**
     * Mengembalikan flag aktif/nonaktif fasilitas, di-default {@code true} bila kolom
     * {@code null} (fasilitas dianggap aktif secara default).
     *
     * @return {@code true} bila fasilitas aktif atau belum pernah diisi, {@code false} bila
     *         eksplisit dinonaktifkan
     */
    @Column(name = "aktif")
    public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
    /**
     * Menetapkan flag aktif/nonaktif fasilitas.
     *
     * @param aktif nilai baru, boleh {@code null} (akan diperlakukan sebagai aktif oleh
     *              {@link #getAktif()})
     */
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    /**
     * Mengembalikan nama pengguna yang terakhir mengubah baris ini.
     *
     * @return nama pengguna, atau {@code null} bila belum pernah diisi
     */
    @Column(name = "oleh")
    public String getOleh() { return oleh; }
    /**
     * Menetapkan nama pengguna yang terakhir mengubah baris ini. Nilai kosong/blank sengaja
     * DIABAIKAN (bukan di-set menjadi kosong) agar jejak audit sebelumnya tidak tertimpa oleh
     * pemanggilan yang tidak membawa identitas pengguna.
     *
     * @param oleh nama pengguna baru; diabaikan bila {@code null} atau hanya berisi spasi
     */
    public void setOleh(String oleh) {
        if (oleh != null && !oleh.trim().isEmpty()) this.oleh = oleh;
    }

    /**
     * Mengembalikan identifier pengguna yang terakhir mengubah baris ini.
     *
     * @return ID pengguna, atau {@code null} bila belum pernah diisi
     */
    @Column(name = "oleh_id")
    public String getOlehId() { return olehId; }
    /**
     * Menetapkan identifier pengguna yang terakhir mengubah baris ini. Nilai kosong/blank
     * sengaja DIABAIKAN, simetris dengan {@link #setOleh(String)}.
     *
     * @param olehId ID pengguna baru; diabaikan bila {@code null} atau hanya berisi spasi
     */
    public void setOlehId(String olehId) {
        if (olehId != null && !olehId.trim().isEmpty()) this.olehId = olehId;
    }

    /**
     * Mengembalikan pengguna yang sedang mengunci baris profil ini untuk diedit, melewati
     * resolusi proxy lazy {@code check(...)} warisan {@link ais.database.model.GeneralValueObject}
     * agar aman dipanggil meski entity ini sudah lepas dari session Hibernate yang memuatnya.
     * Mekanisme kunci baris ini adalah bagian dari kontrak {@link VoKunci}, bukan sesuatu yang
     * ditambahkan class ini sendiri.
     *
     * @return pengguna yang mengunci baris, atau {@code null} bila tidak sedang dikunci
     */
    @ManyToOne(fetch = javax.persistence.FetchType.LAZY,
            cascade = { javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE })
    @JoinColumn(name = "dikunci")
    public Tbmuser getDikunci() {
        dikunci = check(dikunci);
        return dikunci;
    }
    /**
     * Menetapkan pengguna yang mengunci baris profil ini.
     *
     * @param dikunci pengguna pengunci baru, boleh {@code null} untuk melepas kunci
     */
    public void setDikunci(Tbmuser dikunci) { this.dikunci = dikunci; }

    /**
     * Mengembalikan SOP disposisi terkait fasilitas ini, melewati resolusi proxy lazy
     * {@code check(...)} warisan {@link ais.database.model.GeneralValueObject}.
     *
     * @return SOP disposisi, atau {@code null} bila belum terpasang
     */
    @ManyToOne(fetch = javax.persistence.FetchType.LAZY,
            cascade = { javax.persistence.CascadeType.PERSIST, javax.persistence.CascadeType.MERGE })
    @JoinColumn(name = "disposisi_sop")
    public DisposisiSop getDisposisiSop() {
        disposisiSop = check(disposisiSop);
        return disposisiSop;
    }
    /**
     * Menetapkan SOP disposisi terkait fasilitas ini.
     *
     * @param disposisiSop SOP disposisi baru, boleh {@code null}
     */
    public void setDisposisiSop(DisposisiSop disposisiSop) { this.disposisiSop = disposisiSop; }

    /**
     * Callback JPA {@code @PreUpdate} yang mendelegasikan ke
     * {@code AuditTimestampInterceptor.ubah(this)} untuk memutakhirkan {@link #tanggal_dirubah}
     * setiap kali baris ini di-{@code UPDATE}. Pola shadow-audit-field standar AIS.
     */
    @javax.persistence.PreUpdate
    protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    /**
     * Mengembalikan cap waktu perubahan terakhir baris ini.
     *
     * @return tanggal/jam perubahan terakhir; default konstruksi objek adalah waktu objek
     *         dibuat di memori ({@code ais.ui.util.WaktuUtil.getDate()}), sebelum baris pernah
     *         tersimpan
     */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggal_dirubah() { return tanggal_dirubah; }
    /**
     * Menetapkan cap waktu perubahan terakhir secara manual. Dalam alur normal nilai ini
     * dimutakhirkan otomatis oleh {@link #onUpdate()}.
     *
     * @param tanggalDirubah cap waktu baru
     */
    public void setTanggal_dirubah(Date tanggalDirubah) { this.tanggal_dirubah = tanggalDirubah; }

    /**
     * Menerjemahkan {@link #getJenisFasilitas()} menjadi label yang layak tampil ke pengguna
     * dalam Bahasa Indonesia. Method ini {@code @Transient} — bukan kolom basis data, dihitung
     * setiap kali dipanggil dari nilai {@link #jenisFasilitas} saat itu.
     *
     * @return label jenis fasilitas: "Puskesmas", "Posyandu", "Klinik", "Praktik Mandiri",
     *         "Laboratorium Kesehatan", "Apotek", atau "Fasilitas Kesehatan" untuk
     *         {@link #JENIS_LAINNYA}; default "Rumah Sakit" untuk nilai lain (termasuk
     *         {@link #JENIS_RUMAH_SAKIT} maupun nilai tidak dikenal apa pun yang tersimpan di
     *         kolom {@link #jenisFasilitas})
     */
    @Transient
    public String getLabelJenisFasilitas() {
        String jenis = getJenisFasilitas();
        if (JENIS_PUSKESMAS.equals(jenis)) return "Puskesmas";
        if (JENIS_POSYANDU.equals(jenis)) return "Posyandu";
        if (JENIS_KLINIK.equals(jenis)) return "Klinik";
        if (JENIS_PRAKTIK_MANDIRI.equals(jenis)) return "Praktik Mandiri";
        if (JENIS_LABORATORIUM.equals(jenis)) return "Laboratorium Kesehatan";
        if (JENIS_APOTEK.equals(jenis)) return "Apotek";
        if (JENIS_LAINNYA.equals(jenis)) return "Fasilitas Kesehatan";
        return "Rumah Sakit";
    }

    /**
     * Helper internal yang menormalkan {@code String} agar aman dipakai getter kolom teks: nilai
     * {@code null} dijadikan string kosong, nilai lain di-{@code trim()}. Dipakai hampir semua
     * getter kolom teks di kelas ini agar tidak ada satu pun yang mengembalikan {@code null}
     * mentah ke pemanggil.
     *
     * @param value nilai mentah field, boleh {@code null}
     * @return {@code value.trim()}, atau string kosong bila {@code value} adalah {@code null}
     */
    private String clean(String value) { return value == null ? "" : value.trim(); }

    /**
     * Representasi string fasilitas ini, dipakai komponen ZK (combobox/label) yang memanggil
     * {@code toString()} secara implisit.
     *
     * @return {@link #getNama()} (nama yang sudah dibersihkan, tidak pernah {@code null})
     */
    @Override
    public String toString() { return getNama(); }
}
