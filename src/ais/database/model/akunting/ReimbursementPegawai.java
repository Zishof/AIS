package ais.database.model.akunting;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Date;

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

import ais.database.model.GeneralValueObject;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;

/**
 * Pengajuan penggantian biaya pribadi pegawai. Status dan dua referensi jurnal
 * disimpan pada satu dokumen agar approval dan pembayaran tidak dapat diposting
 * dua kali.
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "akunting", name = "reimbursement_pegawai")
public class ReimbursementPegawai extends GeneralValueObject {
    private static final long serialVersionUID = 1L;

    public static final String DIAJUKAN = "Diajukan";
    public static final String REVISI = "Revisi";
    public static final String DITOLAK = "Ditolak";
    public static final String DISETUJUI = "Disetujui";
    public static final String LUNAS = "Lunas";

    private Long id;
    private String kode;
    private String deskripsi;
    private String kategori;
    private Double nominal;
    private Double pajakPersen;
    private Boolean dibayarPegawai;
    private Date tanggalPengeluaran;
    private Date tanggalPengajuan;
    private Pegawai pegawai;
    private Pegawai atasan;
    private Tbmuser dibuatOleh;
    private String catatanPengaju;
    private Long lampiranId;
    private String status;

    private String catatanAtasan;
    private Tbmuser diputuskanOleh;
    private Date tanggalKeputusan;
    private Date tanggalAkuntansi;
    private Akun akunBiaya;
    private PostingHistory postingPengeluaran;

    private String metodePembayaran;
    private String bankPenerima;
    private String rekeningPenerima;
    private Date tanggalPembayaran;
    private String catatanPembayaran;
    private Akun akunPembayaran;
    private Tbmuser dibayarOleh;
    private PostingHistory postingPembayaran;
    private Date tanggalDirubah = ais.ui.util.WaktuUtil.getDate();

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(nullable = false, length = 80)
    public String getKode() { return kode; }
    public void setKode(String kode) { this.kode = kode; }

    @Column(nullable = false, columnDefinition = "text")
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    @Column(nullable = false, length = 100)
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    @Column(nullable = false)
    public Double getNominal() { return nominal == null ? 0.0 : nominal; }
    public void setNominal(Double nominal) { this.nominal = nominal; }

    @Column(name = "pajak_persen")
    public Double getPajakPersen() { return pajakPersen == null ? 0.0 : pajakPersen; }
    public void setPajakPersen(Double pajakPersen) { this.pajakPersen = pajakPersen; }

    @Column(name = "dibayar_pegawai")
    public Boolean getDibayarPegawai() { return dibayarPegawai == null ? Boolean.FALSE : dibayarPegawai; }
    public void setDibayarPegawai(Boolean dibayarPegawai) { this.dibayarPegawai = dibayarPegawai; }

    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_pengeluaran", nullable = false)
    public Date getTanggalPengeluaran() { return tanggalPengeluaran; }
    public void setTanggalPengeluaran(Date tanggalPengeluaran) { this.tanggalPengeluaran = tanggalPengeluaran; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_pengajuan", nullable = false)
    public Date getTanggalPengajuan() { return tanggalPengajuan; }
    public void setTanggalPengajuan(Date tanggalPengajuan) { this.tanggalPengajuan = tanggalPengajuan; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pegawai", nullable = false)
    public Pegawai getPegawai() { pegawai = check(pegawai); return pegawai; }
    public void setPegawai(Pegawai pegawai) { this.pegawai = pegawai; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atasan", nullable = false)
    public Pegawai getAtasan() { atasan = check(atasan); return atasan; }
    public void setAtasan(Pegawai atasan) { this.atasan = atasan; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dibuat_oleh", nullable = false)
    public Tbmuser getDibuatOleh() { dibuatOleh = check(dibuatOleh); return dibuatOleh; }
    public void setDibuatOleh(Tbmuser dibuatOleh) { this.dibuatOleh = dibuatOleh; }

    @Column(name = "catatan_pengaju", columnDefinition = "text")
    public String getCatatanPengaju() { return catatanPengaju; }
    public void setCatatanPengaju(String catatanPengaju) { this.catatanPengaju = catatanPengaju; }

    @Column(name = "lampiran_id")
    public Long getLampiranId() { return lampiranId; }
    public void setLampiranId(Long lampiranId) { this.lampiranId = lampiranId; }

    @Column(nullable = false, length = 30)
    public String getStatus() { return status == null ? DIAJUKAN : status; }
    public void setStatus(String status) { this.status = status; }

    @Column(name = "catatan_atasan", columnDefinition = "text")
    public String getCatatanAtasan() { return catatanAtasan; }
    public void setCatatanAtasan(String catatanAtasan) { this.catatanAtasan = catatanAtasan; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diputuskan_oleh")
    public Tbmuser getDiputuskanOleh() { diputuskanOleh = check(diputuskanOleh); return diputuskanOleh; }
    public void setDiputuskanOleh(Tbmuser diputuskanOleh) { this.diputuskanOleh = diputuskanOleh; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_keputusan")
    public Date getTanggalKeputusan() { return tanggalKeputusan; }
    public void setTanggalKeputusan(Date tanggalKeputusan) { this.tanggalKeputusan = tanggalKeputusan; }

    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_akuntansi")
    public Date getTanggalAkuntansi() { return tanggalAkuntansi; }
    public void setTanggalAkuntansi(Date tanggalAkuntansi) { this.tanggalAkuntansi = tanggalAkuntansi; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "akun_biaya")
    public Akun getAkunBiaya() { akunBiaya = check(akunBiaya); return akunBiaya; }
    public void setAkunBiaya(Akun akunBiaya) { this.akunBiaya = akunBiaya; }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_pengeluaran")
    public PostingHistory getPostingPengeluaran() { postingPengeluaran = check(postingPengeluaran); return postingPengeluaran; }
    public void setPostingPengeluaran(PostingHistory postingPengeluaran) { this.postingPengeluaran = postingPengeluaran; }

    @Column(name = "metode_pembayaran", length = 20)
    public String getMetodePembayaran() { return metodePembayaran; }
    public void setMetodePembayaran(String metodePembayaran) { this.metodePembayaran = metodePembayaran; }

    @Column(name = "bank_penerima", length = 150)
    public String getBankPenerima() { return bankPenerima; }
    public void setBankPenerima(String bankPenerima) { this.bankPenerima = bankPenerima; }

    @Column(name = "rekening_penerima", length = 100)
    public String getRekeningPenerima() { return rekeningPenerima; }
    public void setRekeningPenerima(String rekeningPenerima) { this.rekeningPenerima = rekeningPenerima; }

    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_pembayaran")
    public Date getTanggalPembayaran() { return tanggalPembayaran; }
    public void setTanggalPembayaran(Date tanggalPembayaran) { this.tanggalPembayaran = tanggalPembayaran; }

    @Column(name = "catatan_pembayaran", columnDefinition = "text")
    public String getCatatanPembayaran() { return catatanPembayaran; }
    public void setCatatanPembayaran(String catatanPembayaran) { this.catatanPembayaran = catatanPembayaran; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "akun_pembayaran")
    public Akun getAkunPembayaran() { akunPembayaran = check(akunPembayaran); return akunPembayaran; }
    public void setAkunPembayaran(Akun akunPembayaran) { this.akunPembayaran = akunPembayaran; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dibayar_oleh")
    public Tbmuser getDibayarOleh() { dibayarOleh = check(dibayarOleh); return dibayarOleh; }
    public void setDibayarOleh(Tbmuser dibayarOleh) { this.dibayarOleh = dibayarOleh; }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "posting_pembayaran")
    public PostingHistory getPostingPembayaran() { postingPembayaran = check(postingPembayaran); return postingPembayaran; }
    public void setPostingPembayaran(PostingHistory postingPembayaran) { this.postingPembayaran = postingPembayaran; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggalDirubah() { return tanggalDirubah; }
    public void setTanggalDirubah(Date tanggalDirubah) { this.tanggalDirubah = tanggalDirubah; }

    @javax.persistence.PreUpdate
    protected void onUpdate() { tanggalDirubah = ais.ui.util.WaktuUtil.getDate(); }

    public String toString() { return kode + " - " + deskripsi; }
}
