package ais.database.model.koperasi;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.envers.Audited;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import ais.database.model.GeneralValueObject;

/**
 * Header satu promo "Diskon Grup" (Grup Aturan Diskon) -- SATU aturan diskon
 * yang nilainya (persentase/nominal/cashback, prioritas, masa berlaku,
 * sasaran member, dst) berlaku sekaligus untuk BANYAK produk. Daftar produk
 * anggota grup disimpan di dua tempat yang HARUS sinkron: baris pivot
 * {@link GrupAturanDiskonDetail} (tabel {@code koperasi.grup_aturan_diskon_detail},
 * sumber kebenaran yang dipakai mesin checkout) dan {@link #getDetailJson()}
 * (salinan JSON ID produk yang sama, ditulis bersamaan oleh
 * {@code GrupAturanDiskonAction.onSave} pada transaksi tulis yang sama).
 *
 * <p><b>Mekanisme "OR", bukan "AND".</b> Meski namanya "grup multi-produk",
 * promo ini berlaku bagi tiap produk anggota grup SECARA INDEPENDEN saat
 * produk itu ada di keranjang -- bukan mensyaratkan pembeli membeli SEMUA
 * produk anggota grup sekaligus baru diskon aktif. Ini dikonfirmasi dari
 * query checkout di {@code KantinHelper} (join {@code grup_aturan_diskon}
 * dengan {@code grup_aturan_diskon_detail} lalu difilter
 * {@code d.produk IN (<id-id di keranjang>)}) -- satu baris kandidat diskon
 * dihasilkan PER PRODUK yang cocok, dievaluasi terpisah seperti baris
 * {@link AturanDiskon} biasa, bukan sebagai satu paket "beli A+B+C".</p>
 *
 * <p><b>Mesin hitung yang sama dengan {@link AturanDiskon}.</b> Class ini TIDAK
 * mewarisi/berelasi langsung dengan {@link AturanDiskon} (dua tabel dan dua
 * class terpisah), namun field nilai-diskonnya ({@link #getPersentase()},
 * {@link #getNominal()}, {@link #getMaksimalPotongan()},
 * {@link #getPrioritas()}, {@link #getDapatDigabung()},
 * {@link #getDasarPerhitungan()}, {@link #getGrupEksklusif()},
 * {@link #getPotonganLangsung()}) sengaja dibuat semirip mungkin dengan
 * {@link AturanDiskon} karena {@code KantinHelper} menggabungkan kandidat
 * dari kedua sumber (per-produk langsung, per-produk via
 * {@code grup_produk.aturan_diskon}, dan grup ini) ke dalam SATU daftar
 * "rules" yang lalu diurutkan berdasar prioritas dan ditumpuk/dipilih dengan
 * SATU logika kalkulasi yang sama -- lihat javadoc {@link AturanDiskon} utk
 * rincian rumus (persentase vs nominal, batas {@code maksimalPotongan}, basis
 * perhitungan {@code SETELAH_DISKON}/{@code HARGA_AWAL}, aturan penggabungan
 * {@code dapatDigabung}, dan pengecualian {@code grupEksklusif}). Perbedaan
 * khusus grup: field {@link #getCashback()} adalah cashback TETAP per unit
 * (bukan persen/nominal potongan) yang ditambahkan terpisah dari
 * {@code diskon}/{@code potongan harga}, dan bisa berjalan BERSAMAAN dengan
 * potongan langsung pada baris yang sama.</p>
 *
 * <p><b>FK sasaran sebagai {@code Long} mentah, bukan relasi JPA.</b>
 * {@link #getToko()}, {@link #getJenisAnggota()}, {@link #getTipeAnggota()}
 * disimpan sbg ID numerik polos tanpa {@code @ManyToOne}, berbeda dengan
 * {@link AturanDiskon} yang memakai relasi entity penuh utk field serupa.
 * Sasaran member yang lebih kaya (bisa banyak jenis/tipe sekaligus, bukan
 * cuma satu) justru disimpan sbg JSON array di {@link #getJenisMemberJson()}/
 * {@link #getTipeMemberJson()}, diaktifkan lewat flag {@link #getKhususMember()}.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "koperasi", name = "grup_aturan_diskon")
@Audited
public class GrupAturanDiskon extends GeneralValueObject {
    private static final long serialVersionUID = 1L;

    /** ID header (primary key, auto-increment). */
    private Long id;
    /** FK mentah (bukan relasi JPA) ke toko pemilik produk-produk grup ini; {@code null} = tidak dibatasi toko tertentu. */
    private Long toko;
    /** FK mentah ke satu jenis anggota koperasi sasaran promo (dipakai hanya bila tidak memakai {@link #getJenisMemberJson()}). */
    private Long jenisAnggota;
    /** FK mentah ke satu tipe anggota koperasi sasaran promo (dipakai hanya bila tidak memakai {@link #getTipeMemberJson()}). */
    private Long tipeAnggota;

    /** Nama promo yang tampil di kasir dan struk (mis. "Promo Kemerdekaan Sembako"). */
    private String namaGrup;
    /** Catatan bebas untuk admin, tidak tampil ke pembeli. */
    private String keterangan;
    /** CSV hari ISO-8601 aktif (1=Senin..7=Minggu); {@code null}/kosong = berlaku semua hari. Lihat {@link AturanDiskon#getHariAktif()} untuk kontrak lengkap. */
    private String hariAktif;
    /** Salinan JSON ID produk anggota grup, ditulis bersamaan dengan baris {@link GrupAturanDiskonDetail} oleh {@code GrupAturanDiskonAction.onSave}; lihat javadoc kelas untuk risiko sinkronisasi. */
    private String detailJson;
    /** JSON array ID jenis anggota koperasi sasaran (mis. {@code [1,2]}); dipakai bila {@link #getKhususMember()} true. */
    private String jenisMemberJson;
    /** JSON array ID tipe anggota koperasi sasaran (mis. {@code [3,4]}); dipakai bila {@link #getKhususMember()} true. */
    private String tipeMemberJson;
    /** Nama/username pengguna yang terakhir mengubah header ini (audit trail). */
    private String oleh;
    /** ID pengguna yang terakhir mengubah header ini (audit trail). */
    private String olehId;
    /** Basis nominal yang dipakai perhitungan persentase: {@code "SETELAH_DISKON"} (default, dihitung dari sisa setelah diskon lain yang sudah ditumpuk) atau {@code "HARGA_AWAL"} (selalu dari harga sebelum diskon apa pun). Lihat {@link AturanDiskon#getDasarPerhitungan()} untuk rumus penuh. */
    private String dasarPerhitungan;
    /** Kode bebas; promo lain dengan kode {@code grupEksklusif} yang SAMA (dan tidak kosong) tidak boleh diterapkan bersamaan pada item yang sama. */
    private String grupEksklusif;

    /** {@code true} = promo berlaku utk semua member tanpa filter jenis/tipe; field ini adalah negasi otomatis dari {@link #getKhususMember()} saat disimpan lewat form CRUD. */
    private Boolean berlakuSemuaMember;
    /** {@code true} = sasaran promo dibatasi via {@link #getJenisMemberJson()}/{@link #getTipeMemberJson()}; {@code false} (default) = berlaku semua member. */
    private Boolean khususMember;
    /** {@code true} (default) = nilai promo memotong harga struk langsung ({@code diskon}); {@code false} = nilai promo masuk sebagai saldo cashback member (dicairkan lewat {@link PencairanDiskon}). Lihat {@link AturanDiskon#getPotonganLangsung()}. */
    private Boolean potonganLangsung;
    /** Status aktif header; {@code null} diperlakukan sebagai {@code TRUE} oleh {@link #getAktif()} (fail-open). */
    private Boolean aktif;
    /** {@code true} = promo ini boleh ditumpuk dengan promo lain yang JUGA mengizinkan penggabungan pada item yang sama; {@code false} (default) = begitu promo ini terpilih, promo lain di item itu diabaikan. */
    private Boolean dapatDigabung;

    /** Prioritas urutan evaluasi; angka LEBIH BESAR dihitung LEBIH DAHULU (dan menjadi kandidat utama saat ada beberapa promo yang cocok untuk item yang sama). Default 100 bila {@code null}. */
    private Integer prioritas;

    /** Persentase diskon, mis. {@code 10.0} untuk 10%. Dipakai lebih dulu drpd {@link #getNominal()} bila {@code > 0}; keduanya sengaja tidak dijumlahkan (lihat mesin hitung di javadoc kelas / {@link AturanDiskon}). */
    private Double persentase;
    /** Batas atas nominal potongan (Rupiah) hasil {@link #getPersentase()}/{@link #getNominal()}; {@code 0}/{@code null} = tanpa batas. */
    private Double maksimalPotongan;
    /** Diskon nominal tetap per baris (Rupiah), dikalikan jumlah unit sebelum dibatasi {@link #getMaksimalPotongan()} dan total tagihan item. Dipakai hanya bila {@link #getPersentase()} tidak diisi/{@code <= 0}. */
    private Double nominal;
    /** Cashback TETAP per unit (Rupiah), ditambahkan ke saldo cashback member secara terpisah dari potongan harga (persentase/nominal) -- bisa berjalan bersamaan dengan {@link #getPotonganLangsung()} pada baris yang sama. {@code 0}/{@code null} = tidak ada cashback tambahan. */
    private Double cashback;

    /** Tanggal mulai berlaku promo (inklusif); {@code null} = tanpa batas mulai. */
    private Date tanggalMulai;
    /** Tanggal berakhir promo (inklusif); {@code null} = tanpa batas akhir. */
    private Date tanggalSelesai;
    /** Cap waktu perubahan terakhir header ini; diisi otomatis saat konstruksi dan diperbarui via {@link #onUpdate()}. */
    private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

    /**
     * Callback JPA {@code @PreUpdate}: menyerahkan pembaruan cap waktu audit
     * ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah} setiap
     * kali header ini di-UPDATE.
     */
    @javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this); }

    /** @return ID header (primary key, auto-increment). {@code null} sebelum baris disimpan. */
    @Id @GeneratedValue(strategy = IDENTITY) @Column(name="id", insertable=false, unique=true, nullable=false)
    public Long getId(){ return id; } public void setId(Long v){ id=v; }

    /** @return nama promo yang tampil di kasir/struk. Wajib diisi ({@code NOT NULL} di DB). */
    @Column(name="nama_grup", nullable=false, length=255) public String getNamaGrup(){ return namaGrup; }
    /** @param v nama promo. */
    public void setNamaGrup(String v){ namaGrup=v; }

    /** @return catatan bebas admin (tidak tampil ke pembeli). */
    @Column(name="keterangan", columnDefinition="text") public String getKeterangan(){ return keterangan; }
    /** @param v catatan bebas admin. */
    public void setKeterangan(String v){ keterangan=v; }

    /** @return ID toko pemilik produk-produk grup ini (FK mentah); {@code null} = tidak dibatasi toko. */
    @Column(name="toko") public Long getToko(){ return toko; } public void setToko(Long v){ toko=v; }

    /** @return ID jenis anggota koperasi sasaran tunggal (FK mentah); lihat juga {@link #getJenisMemberJson()} untuk sasaran multi-jenis. */
    @Column(name="jenis_anggota") public Long getJenisAnggota(){ return jenisAnggota; } public void setJenisAnggota(Long v){ jenisAnggota=v; }

    /** @return ID tipe anggota koperasi sasaran tunggal (FK mentah); lihat juga {@link #getTipeMemberJson()} untuk sasaran multi-tipe. */
    @Column(name="tipe_anggota") public Long getTipeAnggota(){ return tipeAnggota; } public void setTipeAnggota(Long v){ tipeAnggota=v; }

    /** @return {@code true} bila promo berlaku utk semua member; {@code null} diperlakukan sebagai {@code TRUE} (fail-open). */
    @Column(name="berlaku_semua_member") public Boolean getBerlakuSemuaMember(){ return berlakuSemuaMember == null ? Boolean.TRUE : berlakuSemuaMember; }
    /** @param v flag berlaku semua member. */
    public void setBerlakuSemuaMember(Boolean v){ berlakuSemuaMember=v; }

    /** @return {@code true} bila sasaran promo dibatasi jenis/tipe member tertentu; {@code null} diperlakukan sebagai {@code FALSE}. */
    @Column(name="khusus_member") public Boolean getKhususMember(){ return khususMember == null ? Boolean.FALSE : khususMember; }
    /** @param v flag khusus member. */
    public void setKhususMember(Boolean v){ khususMember=v; }

    /** @return JSON array ID jenis anggota sasaran (mis. {@code [1,2]}); relevan hanya bila {@link #getKhususMember()} true. */
    @Column(name="jenis_member_json", columnDefinition="text") public String getJenisMemberJson(){ return jenisMemberJson; }
    /** @param v JSON array ID jenis anggota sasaran. */
    public void setJenisMemberJson(String v){ jenisMemberJson=v; }

    /** @return JSON array ID tipe anggota sasaran (mis. {@code [3,4]}); relevan hanya bila {@link #getKhususMember()} true. */
    @Column(name="tipe_member_json", columnDefinition="text") public String getTipeMemberJson(){ return tipeMemberJson; }
    /** @param v JSON array ID tipe anggota sasaran. */
    public void setTipeMemberJson(String v){ tipeMemberJson=v; }

    /** @return persentase diskon (mis. {@code 10.0} = 10%); {@code null} tersimpan dikembalikan sebagai {@code 0}. */
    @Column(name="persentase") public Double getPersentase(){ return persentase == null ? 0D : persentase; }
    /** @param v persentase diskon. */
    public void setPersentase(Double v){ persentase=v; }

    /** @return batas atas nominal potongan (Rupiah); {@code 0} = tanpa batas. */
    @Column(name="maksimal_potongan") public Double getMaksimalPotongan(){ return maksimalPotongan == null ? 0D : maksimalPotongan; }
    /** @param v batas atas nominal potongan. */
    public void setMaksimalPotongan(Double v){ maksimalPotongan=v; }

    /** @return diskon nominal tetap per unit (Rupiah); dipakai hanya bila {@link #getPersentase()} tidak diisi. */
    @Column(name="nominal") public Double getNominal(){ return nominal == null ? 0D : nominal; }
    /** @param v diskon nominal tetap. */
    public void setNominal(Double v){ nominal=v; }

    /** @return cashback tetap per unit (Rupiah), ditambahkan ke saldo cashback member terpisah dari potongan harga. */
    @Column(name="cashback") public Double getCashback(){ return cashback == null ? 0D : cashback; }
    /** @param v cashback tetap per unit. */
    public void setCashback(Double v){ cashback=v; }

    /** @return prioritas evaluasi; angka lebih besar dihitung lebih dahulu. Default {@code 100} bila belum diisi. */
    @Column(name="prioritas", nullable=false) public Integer getPrioritas(){ return prioritas == null ? 100 : prioritas; }
    /** @param v prioritas evaluasi. */
    public void setPrioritas(Integer v){ prioritas=v; }

    /** @return {@code true} bila promo ini boleh ditumpuk dengan promo lain yang juga mengizinkan penggabungan; default {@code FALSE} (tidak ditumpuk). */
    @Column(name="dapat_digabung", nullable=false) public Boolean getDapatDigabung(){ return dapatDigabung == null ? Boolean.FALSE : dapatDigabung; }
    /** @param v flag boleh digabung. */
    public void setDapatDigabung(Boolean v){ dapatDigabung=v; }

    /** @return basis perhitungan persentase: {@code "SETELAH_DISKON"} (default) atau {@code "HARGA_AWAL"}; string kosong/{@code null} dikembalikan sebagai default. */
    @Column(name="dasar_perhitungan", nullable=false, length=30) public String getDasarPerhitungan(){ return dasarPerhitungan == null || dasarPerhitungan.trim().isEmpty() ? "SETELAH_DISKON" : dasarPerhitungan; }
    /** @param v basis perhitungan persentase. */
    public void setDasarPerhitungan(String v){ dasarPerhitungan=v; }

    /** @return kode grup eksklusif; promo lain berkode sama tidak boleh dipakai bersamaan pada item yang sama. {@code null}/kosong = tidak ada pembatasan eksklusivitas. */
    @Column(name="grup_eksklusif", length=100) public String getGrupEksklusif(){ return grupEksklusif; }
    /** @param v kode grup eksklusif. */
    public void setGrupEksklusif(String v){ grupEksklusif=v; }

    /** @return {@code true} (default) bila nilai promo memotong harga struk langsung; {@code false} = nilai promo masuk sebagai saldo cashback (dicairkan lewat {@link PencairanDiskon}). */
    @Column(name="potongan_langsung") public Boolean getPotonganLangsung(){ return potonganLangsung == null ? Boolean.TRUE : potonganLangsung; }
    /** @param v flag potongan langsung vs cashback. */
    public void setPotonganLangsung(Boolean v){ potonganLangsung=v; }

    /** @return tanggal mulai berlaku promo (inklusif); {@code null} = tanpa batas mulai. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="tanggal_mulai") public Date getTanggalMulai(){ return tanggalMulai; }
    /** @param v tanggal mulai berlaku. */
    public void setTanggalMulai(Date v){ tanggalMulai=v; }

    /** @return tanggal berakhir promo (inklusif); {@code null} = tanpa batas akhir. */
    @Temporal(TemporalType.TIMESTAMP) @Column(name="tanggal_selesai") public Date getTanggalSelesai(){ return tanggalSelesai; }
    /** @param v tanggal berakhir promo. */
    public void setTanggalSelesai(Date v){ tanggalSelesai=v; }

    /** @return CSV hari ISO-8601 aktif (1=Senin..7=Minggu); {@code null}/kosong = berlaku semua hari. */
    @Column(name="hari_aktif", length=20) public String getHariAktif(){ return hariAktif; } public void setHariAktif(String v){ hariAktif=v; }

    /** @return status aktif header; {@code null} tersimpan diperlakukan sebagai {@code TRUE} (fail-open). */
    @Column(name="aktif") public Boolean getAktif(){ return aktif == null ? Boolean.TRUE : aktif; }
    /** @param v status aktif header. */
    public void setAktif(Boolean v){ aktif=v; }

    /**
     * @return salinan JSON daftar ID produk anggota grup, ditulis bersamaan
     *         dengan baris {@link GrupAturanDiskonDetail} pada transaksi
     *         simpan yang sama oleh {@code GrupAturanDiskonAction.onSave}.
     *         Field ini TIDAK dibaca oleh mesin evaluasi diskon saat checkout
     *         ({@code KantinHelper} membaca langsung dari tabel
     *         {@code grup_aturan_diskon_detail}) -- perannya murni sebagai
     *         cache tampilan cepat (mis. mengisi ulang textbox ID produk di
     *         form CRUD tanpa query terpisah).
     */
    @Column(name="detail_json", columnDefinition="text") public String getDetailJson(){ return detailJson; } public void setDetailJson(String v){ detailJson=v; }

    /** @return nama/username pengguna yang terakhir mengubah header ini (audit trail). */
    public String getOleh(){ return oleh; }
    /** @param v nama/username pengubah. */
    public void setOleh(String v){ oleh=v; }

    /** @return ID pengguna yang terakhir mengubah header ini (audit trail). */
    public String getOlehId(){ return olehId; }
    /** @param v ID pengguna pengubah. */
    public void setOlehId(String v){ olehId=v; }

    /** @return cap waktu perubahan terakhir header ini. */
    @Temporal(TemporalType.TIMESTAMP) public Date getTanggal_dirubah(){ return tanggal_dirubah; }
    /** @param v cap waktu perubahan; biasanya diatur otomatis, bukan manual. */
    public void setTanggal_dirubah(Date v){ tanggal_dirubah=v; }
}
