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

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.rab.Workspace;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;

/**
 * Pengajuan penggantian biaya pribadi pegawai (reimbursement).
 *
 * <p>
 * Sejak rework mengikuti pola {@link UangMuka}: dokumen ini digerakkan SOP
 * ({@code implements DataSop} + {@code disposisiSop}), persetujuan berada di
 * AKHIR alur disposisi (status/penyetuju diturunkan dari disposisi, bukan diset
 * manual), memilih Anggaran (Workspace/SatuanKerja) seperti uang muka, dan
 * setelah DISETUJUI otomatis masuk daftar DPC lewat
 * {@link DaftarPengajuanTransfer#simpanReimbursement}. Bedanya dengan uang muka:
 * tidak ada sumber Permintaan Pembelian (PR); rincian barang/biaya disimpan
 * sebagai JSON {@code formula} meniru pola Kas Kecil.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "akunting", name = "reimbursement_pegawai")
public class ReimbursementPegawai extends DataSop {
    private static final long serialVersionUID = 1L;

    public static final String DIAJUKAN = "Diajukan";
    public static final String REVISI = "Revisi";
    public static final String DITOLAK = "Ditolak";
    public static final String DISETUJUI = "Disetujui";
    public static final String LUNAS = "Lunas";

    private Long id;
    private String kode;
    private String kodeUnik;
    private String nama;
    private String keterangan;
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

    // --- Anggaran (pola UangMuka) ---
    private Workspace workspace;
    private SatuanKerja satuanKerja;
    private Akun akun;
    private Boolean tanpaAnggaran;

    // --- Rincian barang/biaya (pola KasKecil: JSON) ---
    private String formula;

    // --- SOP + DPC ---
    private DisposisiSop disposisiSop;
    private DaftarPengajuanTransfer daftarPengajuanTransfer;
    private Tbmuser disetujuiOleh;
    private Date tanggalPersetujuan;

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

    /**
     * Kunci unik per-siklus SOP agar penyimpanan ganda (satu kode dipakai ulang
     * di siklus disposisi berbeda) tetap aman. Meniru {@link UangMuka#getKodeUnik}.
     */
    @Column(unique = true)
    public String getKodeUnik() {
        kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
        return kodeUnik;
    }
    public void setKodeUnik(String kodeUnik) { this.kodeUnik = kodeUnik; }

    @Column(name = "nama", length = 255)
    public String getNama() { return nama == null ? null : nama.trim(); }
    public void setNama(String nama) { this.nama = nama; }

    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    @Column(columnDefinition = "text")
    public String getDeskripsi() { return deskripsi; }
    public void setDeskripsi(String deskripsi) { this.deskripsi = deskripsi; }

    @Column(length = 100)
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    @Column(nullable = false)
    public Double getNominal() { return nominal == null ? 0.0 : nominal; }
    public void setNominal(Double nominal) { this.nominal = nominal; }

    /** Alias {@link #getNominal()} agar seragam dengan form pola UangMuka. */
    public Double getNilai() { return getNominal(); }
    public void setNilai(Double nilai) { this.nominal = nilai; }

    @Column(name = "pajak_persen")
    public Double getPajakPersen() { return pajakPersen == null ? 0.0 : pajakPersen; }
    public void setPajakPersen(Double pajakPersen) { this.pajakPersen = pajakPersen; }

    @Column(name = "dibayar_pegawai")
    public Boolean getDibayarPegawai() { return dibayarPegawai == null ? Boolean.FALSE : dibayarPegawai; }
    public void setDibayarPegawai(Boolean dibayarPegawai) { this.dibayarPegawai = dibayarPegawai; }

    @Temporal(TemporalType.DATE)
    @Column(name = "tanggal_pengeluaran")
    public Date getTanggalPengeluaran() { return tanggalPengeluaran; }
    public void setTanggalPengeluaran(Date tanggalPengeluaran) { this.tanggalPengeluaran = tanggalPengeluaran; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_pengajuan")
    public Date getTanggalPengajuan() { return tanggalPengajuan; }
    public void setTanggalPengajuan(Date tanggalPengajuan) { this.tanggalPengajuan = tanggalPengajuan; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pegawai")
    public Pegawai getPegawai() { pegawai = check(pegawai); return pegawai; }
    public void setPegawai(Pegawai pegawai) { this.pegawai = pegawai; }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atasan")
    public Pegawai getAtasan() { atasan = check(atasan); return atasan; }
    public void setAtasan(Pegawai atasan) { this.atasan = atasan; }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "dibuat_oleh")
    public Tbmuser getDibuatOleh() {
        dibuatOleh = check(dibuatOleh);
        if (getDisposisiSop() != null && getDisposisiSop().getDisposisiStart() != null
                && getDisposisiSop().getDisposisiStart().getDiajukanOleh() != null) {
            dibuatOleh = getDisposisiSop().getDisposisiStart().getDiajukanOleh();
        }
        return dibuatOleh;
    }
    public void setDibuatOleh(Tbmuser dibuatOleh) { this.dibuatOleh = dibuatOleh; }

    @Column(name = "catatan_pengaju", columnDefinition = "text")
    public String getCatatanPengaju() { return catatanPengaju; }
    public void setCatatanPengaju(String catatanPengaju) { this.catatanPengaju = catatanPengaju; }

    @Column(name = "lampiran_id")
    public Long getLampiranId() { return lampiranId; }
    public void setLampiranId(Long lampiranId) { this.lampiranId = lampiranId; }

    /**
     * Status diturunkan dari disposisi SOP (persetujuan di akhir alur): DISETUJUI
     * bila penyetuju terisi, DITOLAK bila langkah akhir adalah titik penolakan,
     * selain itu memakai nilai tersimpan (default DIAJUKAN). Meniru
     * {@link UangMuka#getStatus}.
     */
    @Column(length = 30)
    public String getStatus() {
        if (getDisetujuiOleh() != null) {
            status = DISETUJUI;
        } else if (status != null && status.equals(DISETUJUI)) {
            status = DIAJUKAN;
        }

        DisposisiSop d = getDisposisiSop();
        if (d != null && d.getDisposisiEnd() != null && d.getDisposisiEnd().getAlurSop() != null
                && d.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
            status = DITOLAK;
        }

        return status == null || status.trim().isEmpty() ? DIAJUKAN : status;
    }
    public void setStatus(String status) {
        if (status != null && status.equals(DITOLAK)) {
            setDisetujuiOleh(null);
            setTanggalPersetujuan(null);
        }
        this.status = status;
    }

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

    // =========================================================================
    // Anggaran (pola UangMuka)
    // =========================================================================

    public Boolean getTanpaAnggaran() { return tanpaAnggaran == null ? false : tanpaAnggaran; }
    public void setTanpaAnggaran(Boolean tanpaAnggaran) { this.tanpaAnggaran = tanpaAnggaran; }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace", nullable = true)
    public Workspace getWorkspace() {
        if (getTanpaAnggaran()) {
            workspace = null;
        } else {
            workspace = check(workspace);
        }
        return workspace;
    }
    public void setWorkspace(Workspace workspace) { this.workspace = workspace; }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "satuan_kerja", nullable = true)
    public SatuanKerja getSatuanKerja() {
        if (getWorkspace() != null && getWorkspace().getSatuanKerja() != null) {
            satuanKerja = getWorkspace().getSatuanKerja();
        } else {
            satuanKerja = check(satuanKerja);
        }
        return satuanKerja;
    }
    public void setSatuanKerja(SatuanKerja satuanKerja) { this.satuanKerja = satuanKerja; }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "akun", nullable = true)
    public Akun getAkun() {
        akun = check(akun);
        try {
            if (getWorkspace() != null) {
                akun = getWorkspace().getAkun();
            }
        } catch (Exception e) {
            ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) ReimbursementPegawai.getAkun");
        }
        return akun;
    }
    public void setAkun(Akun akun) { this.akun = akun; }

    // =========================================================================
    // Rincian barang/biaya (pola KasKecil: JSON)
    // =========================================================================

    @Column(columnDefinition = "text")
    public String getFormula() { return formula; }
    public void setFormula(String formula) { this.formula = formula; }

    // =========================================================================
    // SOP + penyetuju turunan + DPC (pola UangMuka)
    // =========================================================================

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "disposisi_sop", nullable = true)
    public DisposisiSop getDisposisiSop() {
        disposisiSop = check(disposisiSop);
        return disposisiSop;
    }
    public void setDisposisiSop(DisposisiSop disposisiSop) {
        if (disposisiSop == null || disposisiSop.getId() == null) {
            return;
        }
        this.disposisiSop = disposisiSop;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "disetujui_oleh", nullable = true)
    public Tbmuser getDisetujuiOleh() {
        disetujuiOleh = check(disetujuiOleh);

        if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
                && getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
            disetujuiOleh = getDisposisiSop().getDisposisiSetuju().getDiajukanOleh();
        }

        if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
                || getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
            disetujuiOleh = null;
        }

        return check(disetujuiOleh);
    }
    public void setDisetujuiOleh(Tbmuser disetujuiOleh) { this.disetujuiOleh = disetujuiOleh; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_persetujuan")
    public Date getTanggalPersetujuan() {
        try {
            if (getDisposisiSop() != null && getDisposisiSop().getDisposisiSetuju() != null
                    && getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() != null) {
                tanggalPersetujuan = getDisposisiSop().getDisposisiSetuju().getWaktu();
            }
            if (getDisposisiSop() != null && (getDisposisiSop().getDisposisiSetuju() == null
                    || getDisposisiSop().getDisposisiSetuju().getDiajukanOleh() == null)) {
                tanggalPersetujuan = null;
            }
        } catch (Exception exLazy) {
            ais.common.ErrorAuditUtil.record(exLazy, "auto-audit(empty-catch) ReimbursementPegawai.getTanggalPersetujuan-lazy");
        }
        return tanggalPersetujuan;
    }
    public void setTanggalPersetujuan(Date tanggalPersetujuan) { this.tanggalPersetujuan = tanggalPersetujuan; }

    /** Aktif kecuali disposisi non-aktif atau ditolak di titik akhir (pola UangMuka). */
    public Boolean getAktif() {
        Boolean aktif = Boolean.TRUE;
        DisposisiSop d = getDisposisiSop();
        if (d != null && !d.getAktif()) {
            aktif = false;
        }
        if (d != null && d.getDisposisiEnd() != null && d.getDisposisiEnd().getAlurSop() != null
                && d.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
            aktif = false;
        }
        return aktif;
    }

    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Fetch(FetchMode.SELECT)
    @JoinColumn(name = "daftar_pengajuan_transfer", nullable = true)
    public DaftarPengajuanTransfer getDaftarPengajuanTransfer() { return daftarPengajuanTransfer; }
    public void setDaftarPengajuanTransfer(DaftarPengajuanTransfer daftarPengajuanTransfer) {
        this.daftarPengajuanTransfer = daftarPengajuanTransfer;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggalDirubah() { return tanggalDirubah; }
    public void setTanggalDirubah(Date tanggalDirubah) { this.tanggalDirubah = tanggalDirubah; }

    @javax.persistence.PreUpdate
    protected void onUpdate() { tanggalDirubah = ais.ui.util.WaktuUtil.getDate(); }

    public String toString() { return kode + " - " + (nama == null ? deskripsi : nama); }
}
