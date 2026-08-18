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
import ais.database.model.asset.JenisAsset;

/**
 * Jenis Pengeluaran — master pilihan pengeluaran pada rincian item
 * Reimbursement Pegawai. Akun biaya DIPETAKAN DI SINI oleh admin, sehingga
 * pegawai cukup memilih jenis pengeluarannya (mis. "BBM / Bensin", "Parkir",
 * "Konsumsi Rapat") tanpa harus memahami kode akun yang rumit.
 *
 * <p>Mapping opsional ke {@link JenisAsset} disediakan untuk pengeluaran yang
 * menghasilkan BARANG/ASET (mis. printer, perkakas) agar proses lanjutan
 * penerimaan aset dapat mengenali jenis asetnya.</p>
 *
 * <p>±55 jenis umum di-seed saat bootstrap Tomcat oleh
 * {@code InitIndex.initDefaultJenisPengeluaran()} (akun dilengkapi admin lewat
 * tab "Jenis Pengeluaran" pada layar Reimbursement Pegawai).</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Table(schema = "akunting", name = "jenis_pengeluaran")
public class JenisPengeluaran extends GeneralValueObject {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nama;
    private String keterangan;
    private Akun akun;
    private JenisAsset jenisAsset;
    private Boolean aktif;
    private Date tanggalDirubah = ais.ui.util.WaktuUtil.getDate();

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", insertable = false, unique = true, nullable = false)
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    @Column(name = "nama", length = 255)
    public String getNama() { return nama == null ? null : nama.trim(); }
    public void setNama(String nama) { this.nama = nama; }

    @Column(name = "keterangan", columnDefinition = "text")
    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    /** Akun biaya untuk jenis pengeluaran ini — dipetakan admin sekali, dipakai semua pengajuan. */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "akun", nullable = true)
    public Akun getAkun() { akun = check(akun); return akun; }
    public void setAkun(Akun akun) { this.akun = akun; }

    /** Mapping opsional ke jenis aset (untuk pengeluaran yang menghasilkan barang/aset). */
    @ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
    @JoinColumn(name = "jenis_asset", nullable = true)
    public JenisAsset getJenisAsset() { jenisAsset = check(jenisAsset); return jenisAsset; }
    public void setJenisAsset(JenisAsset jenisAsset) { this.jenisAsset = jenisAsset; }

    @Column(name = "aktif")
    public Boolean getAktif() { return aktif == null ? Boolean.TRUE : aktif; }
    public void setAktif(Boolean aktif) { this.aktif = aktif; }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "tanggal_dirubah")
    public Date getTanggalDirubah() { return tanggalDirubah; }
    public void setTanggalDirubah(Date tanggalDirubah) { this.tanggalDirubah = tanggalDirubah; }

    @javax.persistence.PreUpdate
    protected void onUpdate() { tanggalDirubah = ais.ui.util.WaktuUtil.getDate(); }

    public String toString() { return nama == null ? "" : nama.trim(); }
}
