package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;
import java.util.Date;
import javax.persistence.*;
import ais.database.model.GeneralValueObject;

/**
 * Baris keanggotaan satu produk dalam satu {@link GrupAturanDiskon} ("Grup
 * Aturan Diskon"/"Diskon Grup") -- yaitu satu promo yang nilainya (persentase/
 * nominal/cashback, prioritas, masa berlaku, dst, semuanya disimpan di header
 * {@link GrupAturanDiskon}) berlaku untuk BANYAK produk sekaligus. Class ini
 * TIDAK menyimpan nilai diskon apa pun -- ia murni tabel pivot/junction
 * "produk yang mana saja ikut promo X", satu baris per pasangan
 * (grup, produk).
 *
 * <p><b>Bukan header/detail nilai bertingkat.</b> Berbeda dari pola
 * header-detail transaksi (mis. nota + baris item dengan nilai masing-masing),
 * di sini SEMUA baris detail berbagi persis nilai promo yang sama dari satu
 * header; yang berbeda antar baris hanyalah {@link #getProduk() produk} mana
 * yang tercakup. Lihat juga {@link AturanDiskon}, mesin promo per-produk yang
 * TERPISAH (bukan turunan/detail dari class ini) dengan skema field yang
 * sengaja dibuat mirip agar mudah digabung satu mesin hitung.</p>
 *
 * <p><b>Ditulis ulang penuh tiap simpan.</b> {@code GrupAturanDiskonAction}
 * (form ZK) menghapus SELURUH baris {@code grup_aturan_diskon_detail} milik
 * satu grup lalu meng-insert ulang dari daftar ID produk yang diketik admin,
 * setiap kali form disimpan -- bukan diff/upsert per baris. Kolom
 * {@link GrupAturanDiskon#getDetailJson()} pada header menyimpan salinan JSON
 * dari ID produk yang sama persis pada saat simpan yang sama (denormalisasi
 * sengaja utk tampilan cepat); keduanya HARUS tetap sinkron dan hanya
 * dijamin sinkron selama satu-satunya jalur tulis adalah
 * {@code GrupAturanDiskonAction.onSave}.</p>
 *
 * <p><b>Class entity ini sendiri tidak pernah dimuat lewat Hibernate.</b>
 * Baik penulisan ({@code GrupAturanDiskonAction}) maupun pembacaan saat
 * checkout ({@code KantinHelper}, query gabungan {@code grup_aturan_diskon}
 * JOIN {@code grup_aturan_diskon_detail}) sama-sama memakai SQL mentah lewat
 * {@code java.sql.PreparedStatement} langsung ke tabel
 * {@code koperasi.grup_aturan_diskon_detail}, bukan
 * {@code session.get}/{@code Criteria} atas class ini. Pemetaan JPA di sini
 * eksis untuk kelengkapan/CRUD generik, bukan jalur baca-tulis yang benar-
 * benar dipakai aplikasi -- pola yang sama dgn sejumlah entity "tidur" lain
 * di basis kode ini (lihat entity lain yg terverifikasi tak-terpakai di
 * paket ini/paket lain); tabelnya sendiri AKTIF dipakai, hanya bukan lewat
 * class Java ini.</p>
 *
 * <p><b>FK sebagai {@code Long} mentah, bukan relasi JPA.</b> {@link #getGrupAturanDiskon()}
 * dan {@link #getProduk()} disimpan sbg ID numerik polos (tanpa
 * {@code @ManyToOne}), sama seperti kebiasaan field lain pada
 * {@link GrupAturanDiskon} (mis. {@code toko}, {@code jenisAnggota}). Ini
 * kontras dgn {@link AturanDiskon} yg memakai relasi {@code @ManyToOne} penuh
 * (mis. {@code Produk}, {@code Toko}) utk field serupa -- dua keluarga class
 * "aturan diskon" dlm paket yg sama memakai konvensi pemetaan yg berbeda.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert=true, dynamicUpdate=true)
@Table(schema="koperasi", name="grup_aturan_diskon_detail")
public class GrupAturanDiskonDetail extends GeneralValueObject {
    private static final long serialVersionUID=1L;

    /** ID baris. Auto-increment ({@code IDENTITY}); tidak pernah diisi manual. */
    private Long id;
    /** FK mentah (bukan relasi JPA) ke {@link GrupAturanDiskon#getId()} -- header promo pemilik baris ini. */
    private Long grupAturanDiskon;
    /** FK mentah (bukan relasi JPA) ke {@code koperasi.produk.id} -- produk yang ikut serta dalam promo grup ini. */
    private Long produk;
    /**
     * Status aktif baris ini. {@code null} diperlakukan sebagai aktif oleh
     * {@link #getAktif()} (default fail-open ke TRUE) -- konsisten dgn
     * query checkout di {@code KantinHelper} yg memfilter
     * {@code COALESCE(d.aktif,true)}. Dalam praktiknya baris yg sudah tak
     * relevan biasanya langsung DIHAPUS (bukan dinonaktifkan) krn siklus
     * tulis-ulang penuh saat form disimpan; kolom ini lebih berperan sbg
     * jaring pengaman skema drpd mekanisme yg aktif dipakai UI.
     */
    private Boolean aktif;
    /** Nama/username pengguna yang terakhir mengubah baris ini (audit trail, bebas format). */
    private String oleh;
    /** ID pengguna (mis. NIP/username sistem) yang terakhir mengubah baris ini (audit trail). */
    private String olehId;
    /** Cap waktu perubahan terakhir; diisi otomatis saat konstruksi dan diperbarui via {@link #onUpdate()}. */
    private Date tanggal_dirubah=ais.ui.util.WaktuUtil.getDate();

    /**
     * Callback JPA {@code @PreUpdate}: menyerahkan pembaruan {@code tanggal_dirubah}
     * (dan kolom audit terkait) ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah}
     * setiap kali baris ini di-UPDATE, agar cap waktu selalu konsisten dengan
     * intersepsi Hibernate lain di seluruh basis kode, bukan diatur manual per aksi.
     */
    @PreUpdate protected void onUpdate(){ ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    /**
     * @return ID baris (primary key, auto-increment). {@code null} sebelum baris disimpan.
     */
    @Id @GeneratedValue(strategy = IDENTITY) @Column(name="id", insertable=false, unique=true, nullable=false)
    public Long getId(){ return id; }
    /** @param v ID baris. Umumnya tidak perlu diisi manual -- diisi DB via {@code IDENTITY}. */
    public void setId(Long v){ id=v; }

    /**
     * @return ID {@link GrupAturanDiskon} (header promo) pemilik baris ini. Kolom {@code NOT NULL}
     *         di DB; namun anotasi field bukan relasi JPA sehingga tidak ada validasi referensial
     *         di level ORM -- integritas bergantung pada FK di DB dan disiplin pemanggil.
     */
    @Column(name="grup_aturan_diskon", nullable=false) public Long getGrupAturanDiskon(){ return grupAturanDiskon; }
    /** @param v ID header {@link GrupAturanDiskon}. */
    public void setGrupAturanDiskon(Long v){ grupAturanDiskon=v; }

    /**
     * @return ID produk (koperasi.produk) yang tercakup dalam promo grup ini. Kolom {@code NOT NULL}
     *         di DB. {@code GrupAturanDiskonAction.onSave} memvalidasi produk ini benar-benar ada,
     *         aktif, dan milik toko yang sama dengan header sebelum baris ini ditulis.
     */
    @Column(name="produk", nullable=false) public Long getProduk(){ return produk; } public void setProduk(Long v){ produk=v; }

    /**
     * @return status aktif baris; {@code null} tersimpan diperlakukan sebagai {@code TRUE}
     *         (fail-open, sama seperti default umum entity "aktif" lain di paket koperasi).
     */
    @Column(name="aktif") public Boolean getAktif(){ return aktif == null ? Boolean.TRUE : aktif; }
    /** @param v status aktif baris. */
    public void setAktif(Boolean v){ aktif=v; }

    /** @return nama/username pengubah terakhir (audit trail). */
    public String getOleh(){ return oleh; }
    /** @param v nama/username pengubah. */
    public void setOleh(String v){ oleh=v; }

    /** @return ID pengguna pengubah terakhir (audit trail). */
    public String getOlehId(){ return olehId; }
    /** @param v ID pengguna pengubah. */
    public void setOlehId(String v){ olehId=v; }

    /** @return cap waktu perubahan terakhir baris ini. */
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah(){ return tanggal_dirubah; }
    /** @param v cap waktu perubahan; biasanya diatur otomatis, bukan manual. */
    public void setTanggal_dirubah(Date v){ tanggal_dirubah=v; }
}
