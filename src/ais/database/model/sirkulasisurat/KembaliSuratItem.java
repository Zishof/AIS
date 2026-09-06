package ais.database.model.sirkulasisurat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

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
import org.hibernate.envers.Audited;

import ais.database.model.Tbmuser;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entitas Hibernate (skema {@code surat}, tabel {@code kembali_surat_item}) yang menjadi HEADER
 * satu transaksi pengembalian dokumen surat pada modul sirkulasi surat: pasangan dari
 * {@link PeminjamanSuratItem}, mencatat kapan dan oleh siapa suatu peminjaman dikembalikan/
 * diselesaikan, bisa melalui alur disposisi/persetujuan SOP yang sama seperti header peminjaman
 * (lihat kelas induk {@link DataSop}). Dokumen yang dikembalikan secara rinci (per surat, dengan
 * status kelengkapan/denda) dicatat di baris {@link KembaliSuratItemDetail} anak.
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "kembali_surat_item")
public class KembaliSuratItem extends DataSop {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	/** Urutan tampil/nomor indeks baris ini (dipakai untuk pengurutan pada grid UI, bukan bagian dari kunci bisnis). */
	private Long index;
	/** Field audit shadow (bukan kolom Hibernate): nama pemroses terakhir, diisi lewat {@link #setOleh(String)}. */
	private String oleh;
	/** Field audit shadow (bukan kolom Hibernate): ID pemroses terakhir, diisi lewat {@link #setOlehId(String)}. */
	private String olehId;

	/** @return ID pemroses terakhir yang mengubah baris ini (field audit shadow, lihat {@link #setOlehId(String)}). */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel ID pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed): bila
	 * {@code olehId} null atau hanya berisi spasi, method ini langsung {@code return} tanpa
	 * mengubah field, mempertahankan nilai audit sebelumnya.
	 *
	 * @param olehId ID pemroses yang akan diset; diabaikan bila null/kosong.
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/** @return representasi ringkas: {@link #kode} transaksi pengembalian. */
	public String toString() {
		return kode;
	}

	/**
	 * Menyetel nama pemroses terakhir — SETTER MENOLAK nilai kosong/null (guard fail-closed) dengan
	 * pola yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pemroses yang akan diset; diabaikan bila null/kosong.
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/** @return nama pemroses terakhir yang mengubah baris ini (field audit shadow, lihat {@link #setOleh(String)}). */
	public String getOleh() {
		return oleh;
	}

	/** Callback JPA {@code @PreUpdate}: memperbarui {@link #tanggal_dirubah} lewat {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)} setiap kali baris ini di-update. */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** @param tanggal_dirubah waktu perubahan terakhir yang akan diset. */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/** @return waktu perubahan terakhir baris ini (diperbarui otomatis lewat {@link #onUpdate()}). */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/** Kode unik transaksi pengembalian (nomor referensi yang terlihat pengguna). */
	private String kode;
	/** Keterangan bebas untuk transaksi pengembalian ini. */
	private String keterangan;
	/** Header peminjaman ({@link PeminjamanSuratItem}) yang dikembalikan lewat transaksi ini. */
	private PeminjamanSuratItem peminjamanSuratItem;
	/** Tanggal transaksi pengembalian dibuat/diajukan (dapat diselaraskan dari waktu disposisi awal SOP, lihat {@link #getTanggalPembuatan()}). */
	private Date tanggalPembuatan;
	/** Tanggal transaksi pengembalian disetujui (diselaraskan dari waktu disposisi setuju SOP, lihat {@link #getTanggalPersetujuan()}). */
	private Date tanggalPersetujuan;
	/** Pengguna yang membuat/mengajukan transaksi ini (dapat diselaraskan dari pengaju disposisi awal SOP, lihat {@link #getDibuatOleh()}). */
	private Tbmuser dibuatOleh;
	/** Pengguna yang menyetujui transaksi ini (diselaraskan dari pengaju disposisi setuju SOP, lihat {@link #getDisetujuiOleh()}). */
	private Tbmuser disetujuiOleh;

	/** Alur disposisi SOP terkait transaksi ini, sumber penyelarasan otomatis untuk {@link #dibuatOleh}/{@link #disetujuiOleh}/{@link #tanggalPembuatan}/{@link #tanggalPersetujuan}. */
	private DisposisiSop disposisiSop;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public KembaliSuratItem() {
	}

	/** @return ID baris (primary key, auto-increment). */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id ID baris (primary key) yang akan diset. */
	public void setId(Long id) {
		this.id = id;
	}

	/** @return kode unik transaksi pengembalian. */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/** @param kode kode unik transaksi yang akan diset. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return keterangan bebas transaksi pengembalian ini. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan yang akan diset. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param dibuatOleh pengguna pembuat transaksi yang akan diset. */
	public void setDibuatOleh(Tbmuser dibuatOleh) {
		this.dibuatOleh = dibuatOleh;
	}

	/**
	 * @return pengguna yang membuat/mengajukan transaksi ini; bila {@link #disposisiSop} terisi
	 *         dan disposisi awalnya (start) sudah diajukan, nilai diselaraskan dari pengaju
	 *         disposisi tersebut (menggantikan nilai kolom {@code dibuat_oleh} langsung).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "dibuat_oleh", nullable = false)
	public Tbmuser getDibuatOleh() {
		dibuatOleh = check(dibuatOleh);

		if (getDisposisiSop() != null) {
			DisposisiAlurSop disposisiAlurSop = getDisposisiSop().getDisposisiStart();

			if (disposisiAlurSop != null && disposisiAlurSop.getDiajukanOleh() != null) {
				dibuatOleh = disposisiAlurSop.getDiajukanOleh();
			}
		}

		return dibuatOleh;
	}

	/** @param disetujuiOleh pengguna penyetuju transaksi yang akan diset. */
	public void setDisetujuiOleh(Tbmuser disetujuiOleh) {
		this.disetujuiOleh = disetujuiOleh;
	}

	/**
	 * @return pengguna yang menyetujui transaksi ini; bila {@link #disposisiSop} terisi, nilai
	 *         diselaraskan dari pengaju disposisi setuju bila ada, atau dikosongkan (null) bila
	 *         disposisi ada tetapi belum ada disposisi setuju (fail-closed terhadap SOP).
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disetujui_oleh", nullable = true)
	public Tbmuser getDisetujuiOleh() {
		disetujuiOleh = check(disetujuiOleh);
		if (getDisposisiSop() != null) {
			DisposisiAlurSop disposisiAlurSop = getDisposisiSop().getDisposisiSetuju() == null ? null
					: getDisposisiSop().getDisposisiSetuju();

			if (disposisiAlurSop != null && disposisiAlurSop.getDiajukanOleh() != null) {
				disetujuiOleh = disposisiAlurSop.getDiajukanOleh();
			} else {
				disetujuiOleh = null;
			}
		}
		return disetujuiOleh;
	}

	/** @param tanggalPembuatan tanggal pembuatan transaksi yang akan diset. */
	public void setTanggalPembuatan(Date tanggalPembuatan) {
		this.tanggalPembuatan = tanggalPembuatan;
	}

	/**
	 * @return tanggal transaksi dibuat; default tanggal saat ini bila belum diisi, lalu
	 *         diselaraskan dari waktu disposisi awal SOP bila {@link #disposisiSop} terisi.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_pembuatan")
	public Date getTanggalPembuatan() {
		if (tanggalPembuatan == null) {
			tanggalPembuatan = ais.ui.util.WaktuUtil.getDate();
		}

		if (getDisposisiSop() != null) {
			DisposisiAlurSop disposisiAlurSop = getDisposisiSop().getDisposisiStart();

			if (disposisiAlurSop != null && disposisiAlurSop.getDiajukanOleh() != null) {
				tanggalPembuatan = disposisiAlurSop.getWaktu();
			}
		}

		return tanggalPembuatan == null ? WaktuUtil.getDate() : tanggalPembuatan;
	}

	/** @param tanggalPersetujuan tanggal persetujuan transaksi yang akan diset. */
	public void setTanggalPersetujuan(Date tanggalPersetujuan) {
		this.tanggalPersetujuan = tanggalPersetujuan;
	}

	/**
	 * @return tanggal transaksi disetujui; diselaraskan dari waktu disposisi setuju SOP bila
	 *         {@link #disposisiSop} terisi (null bila disposisi ada tapi belum disetujui).
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "tanggal_persetujuan")
	public Date getTanggalPersetujuan() {
		if (getDisposisiSop() != null) {
			DisposisiAlurSop disposisiAlurSop = getDisposisiSop().getDisposisiSetuju() == null ? null
					: getDisposisiSop().getDisposisiSetuju();

			if (disposisiAlurSop != null && disposisiAlurSop.getDiajukanOleh() != null) {
				tanggalPersetujuan = disposisiAlurSop.getWaktu();
			} else {
				tanggalPersetujuan = null;
			}
		}
		return tanggalPersetujuan;
	}

	/**
	 * Menyetel header peminjaman yang dikembalikan — SETTER MENOLAK nilai {@code null} (guard
	 * fail-closed sederhana): hanya mengubah field bila argumen tidak null, mempertahankan relasi
	 * yang sudah ada bila dipanggil dengan null.
	 *
	 * @param peminjamanSuratItem header peminjaman yang akan diset; diabaikan bila null.
	 */
	public void setPeminjamanSuratItem(PeminjamanSuratItem peminjamanSuratItem) {
		if (peminjamanSuratItem != null) {
			this.peminjamanSuratItem = peminjamanSuratItem;
		}
	}

	/** @return header peminjaman ({@link PeminjamanSuratItem}) yang dikembalikan lewat transaksi ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjaman_surat_item", nullable = true)
	public PeminjamanSuratItem getPeminjamanSuratItem() {
		return peminjamanSuratItem;
	}

	/** @param index urutan tampil baris ini yang akan diset. */
	public void setIndex(Long index) {
		this.index = index;
	}

	/** @return urutan tampil/nomor indeks baris ini. */
	public Long getIndex() {
		return index;
	}

	/** @return alur disposisi SOP terkait transaksi ini (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP — SETTER MENOLAK bila {@code disposisiSop} null atau belum tersimpan
	 * (ID null): dalam kasus tersebut method langsung {@code return} tanpa mengubah field,
	 * mempertahankan disposisi yang sudah tersimpan sebelumnya.
	 *
	 * @param disposisiSop alur disposisi SOP yang akan diset; diabaikan bila null/belum tersimpan.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}
}
