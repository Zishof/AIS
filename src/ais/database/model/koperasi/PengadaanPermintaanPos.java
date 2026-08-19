package ais.database.model.koperasi;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;
import ais.database.model.Tbmuser;
import ais.database.model.inventory.Toko;

/**
 * <h3>Permintaan Pembelian (PR) versi POS -- lingkup TOKO.</h3>
 *
 * <p>Padanan {@code asset.PermintaanPengadaanMasterAsset} (ZKoss, dipakai umum untuk aset
 * yayasan) tetapi DISEDERHANAKAN sesuai keputusan produk 2026-08-19: POS hanya mengenal
 * {@link Toko} dan {@link Produk}, sehingga dimensi {@code Workspace}/{@code PemilikAsset}/
 * {@code Lokasi}/{@code Ruang}/{@code SatuanKerja} milik versi umum TIDAK dibawa.</p>
 *
 * <p><b>Kenapa entitas baru, bukan memakai tabel ZKoss.</b> Tabel PR umum tidak memiliki kolom
 * toko dan detailnya menunjuk {@code MasterAsset}, sedangkan seluruh alur POS (Kulakan, stok,
 * HPP) berbasis {@code koperasi.produk} -- jembatan {@code produk.master_asset} ada tetapi
 * TIDAK terpakai (0 dari 8.676 produk). Memaksakan pemakaian bersama berarti mengubah entitas
 * yang dipakai modul umum sekaligus mengisi jembatan itu massal; risikonya jauh lebih besar
 * daripada manfaatnya. Alur/istilah/status tetap MENGIKUTI versi ZKoss agar pengguna yang
 * sudah terbiasa tidak perlu belajar ulang.</p>
 *
 * <p><b>Status PR</b> (sama persis semantik ZKoss): belum disetujui = {@code tanggalPersetujuan}
 * dan {@code tanggalDitolak} kosong; disetujui = {@code tanggalPersetujuan} terisi; ditolak =
 * {@code tanggalDitolak} terisi; {@code tutup} menandai PR tidak dapat diproses lebih lanjut.</p>
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "koperasi", name = "pengadaan_permintaan_pos")
public class PengadaanPermintaanPos extends GeneralValueObject {

	private static final long serialVersionUID = 4821577548439811001L;

	private Long id;
	private String kode;
	private String keterangan;
	private Toko toko;
	private Date tanggalPembuatan;
	private Date tanggalPersetujuan;
	private Date tanggalDitolak;
	private String alasanDitolak;
	private Tbmuser dibuatOleh;
	private Tbmuser disetujuiOleh;
	private Tbmuser ditolakOleh;
	private Double nilai;
	private Boolean tutup;
	private Boolean aktif;
	private String oleh;
	private String olehId;
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	public PengadaanPermintaanPos() {
	}

	public String toString() {
		return (kode == null ? "" : kode) + (keterangan == null ? "" : " - " + keterangan);
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	/** Nomor PR, unik. Dihasilkan server (pola tahun-bulan-urut) bila klien tidak mengirim. */
	@Column(name = "kode", nullable = true, length = 100)
	public String getKode() {
		return kode;
	}

	public void setKode(String kode) {
		this.kode = kode;
	}

	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return keterangan;
	}

	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	@ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@JoinColumn(name = "toko", nullable = true)
	public Toko getToko() {
		return toko;
	}

	public void setToko(Toko toko) {
		this.toko = toko;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan", nullable = true)
	public Date getTanggalPembuatan() {
		return tanggalPembuatan;
	}

	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan", nullable = true)
	public Date getTanggalPersetujuan() {
		return tanggalPersetujuan;
	}

	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_ditolak", nullable = true)
	public Date getTanggalDitolak() {
		return tanggalDitolak;
	}

	public void setTanggalDitolak(Date tanggalDitolak) {
		this.tanggalDitolak = tanggalDitolak;
	}

	@Column(name = "alasan_ditolak", nullable = true)
	public String getAlasanDitolak() {
		return alasanDitolak;
	}

	public void setAlasanDitolak(String alasanDitolak) {
		this.alasanDitolak = alasanDitolak;
	}

	@ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = true)
	public Tbmuser getDibuatOleh() {
		return dibuatOleh;
	}

	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	@ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		return disetujuiOleh;
	}

	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	@ManyToOne(fetch = javax.persistence.FetchType.LAZY)
	@JoinColumn(name = "ditolak_oleh", nullable = true)
	public Tbmuser getDitolakOleh() {
		return ditolakOleh;
	}

	public void setDitolakOleh(Tbmuser ditolakOleh) {
		this.ditolakOleh = ditolakOleh;
	}

	/** Total nilai PR (jumlah seluruh baris detail); disimpan agar daftar tidak perlu agregasi. */
	@Column(name = "nilai", nullable = true)
	public Double getNilai() {
		return nilai;
	}

	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	@Column(name = "tutup", nullable = true)
	public Boolean getTutup() {
		return tutup == null ? Boolean.FALSE : tutup;
	}

	public void setTutup(Boolean tutup) {
		this.tutup = tutup;
	}

	@Column(name = "aktif", nullable = true)
	public Boolean getAktif() {
		return aktif == null ? Boolean.TRUE : aktif;
	}

	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	public String getOleh() {
		return oleh;
	}

	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	public String getOlehId() {
		return olehId;
	}

	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
