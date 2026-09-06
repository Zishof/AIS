package ais.database.model.sirkulasisurat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.text.ParseException;
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

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Entitas Hibernate (skema {@code surat}, tabel {@code peminjaman_surat_item}) yang menjadi
 * HEADER satu transaksi peminjaman surat/dokumen fisik pada modul sirkulasi surat: satu baris
 * mewakili satu pengajuan peminjaman oleh {@link #getPeminjamSurat()} (peminjam), dengan rentang
 * tanggal ({@link #getMulai()}/{@link #getSampai()}) dan bisa melalui alur disposisi/persetujuan
 * SOP ({@link #getDisposisiSop()}, lihat kelas induk {@link DataSop}). Dokumen surat yang
 * benar-benar dipinjam ada di baris {@link PeminjamanSuratItemDetail} anak (satu header bisa
 * memuat banyak dokumen), dan pengembaliannya dicatat lewat {@link KembaliSuratItem}.
 *
 * <p>
 * <b>Penjaga anti-tabrakan (double-booking)</b>: tidak ada constraint unik di level tabel pada
 * dokumen surat masuk ({@code SuratMasuk}) yang dipilih di {@link PeminjamanSuratItemDetail},
 * tetapi setiap titik simpan detail baru (picker massal, scan barcode, maupun commit form) WAJIB
 * memanggil gerbang aplikatif {@link PeminjamanSuratItemDetail#sedangDipinjamAktif} terlebih
 * dahulu (pola sama dengan {@code PeminjamanMasterAssetHelper.sedangDipinjamAktif} pada modul
 * peminjaman aset) sehingga satu dokumen fisik tidak bisa dipinjam pada dua transaksi aktif
 * sekaligus. Jangan menambah jalur simpan detail baru tanpa memanggil gerbang ini.
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "peminjaman_surat_item")
public class PeminjamanSuratItem extends DataSop {

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

	/** @return representasi ringkas: {@link #kode} transaksi peminjaman. */
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

	/** Kode unik transaksi peminjaman (nomor referensi yang terlihat pengguna). */
	private String kode;
	/** Keterangan bebas untuk transaksi peminjaman ini. */
	private String keterangan;
	/** Peminjam ({@link PeminjamSurat}) yang mengajukan transaksi peminjaman ini. */
	private PeminjamSurat peminjamSurat;
	/** Tanggal transaksi peminjaman dibuat/diajukan (dapat diselaraskan dari waktu disposisi awal SOP, lihat {@link #getTanggalPembuatan()}). */
	private Date tanggalPembuatan;
	/** Tanggal transaksi peminjaman disetujui (diselaraskan dari waktu disposisi setuju SOP, lihat {@link #getTanggalPersetujuan()}). */
	private Date tanggalPersetujuan;
	/** Pengguna yang membuat/mengajukan transaksi ini (dapat diselaraskan dari pengaju disposisi awal SOP, lihat {@link #getDibuatOleh()}). */
	private Tbmuser dibuatOleh;
	/** Pengguna yang menyetujui transaksi ini (diselaraskan dari pengaju disposisi setuju SOP, lihat {@link #getDisetujuiOleh()}). */
	private Tbmuser disetujuiOleh;

	/** Header pengembalian ({@link KembaliSuratItem}) terkait transaksi peminjaman ini, bila sudah/sedang dikembalikan. */
	private KembaliSuratItem kembaliSuratItem;

	/** Label jenis item yang dipinjam (mis. "surat"), default {@code "surat"} bila belum diisi (lihat {@link #getTipe()}). */
	private String tipe;
	/** Batas jumlah hari peminjaman yang diizinkan, dihitung ulang dari selisih {@link #getMulai()} dan {@link #getSampai()} setiap dipanggil (lihat {@link #getJumlahHariBatas()}). */
	private Integer jumlahHariBatas;
	/** Jumlah maksimal dokumen yang boleh dipinjam sekaligus pada transaksi ini. */
	private Integer jumlahMaksimalPeminjaman;
	/** Tanggal mulai peminjaman; default besok-lusa bila belum diisi (lihat {@link #getMulai()}). */
	private Date mulai;
	/** Tanggal batas akhir peminjaman; default seminggu ke depan bila belum diisi (lihat {@link #getSampai()}). */
	private Date sampai;
	/** Tujuan/alasan peminjaman dokumen ini (kolom {@code text}). */
	private String tujuanPeminjaman;
	/** Alur disposisi SOP terkait transaksi ini, sumber penyelarasan otomatis untuk {@link #dibuatOleh}/{@link #disetujuiOleh}/{@link #tanggalPembuatan}/{@link #tanggalPersetujuan}. */
	private DisposisiSop disposisiSop;
	/** Satuan kerja pemohon/peminjam. */
	private SatuanKerja satuanKerja;
	/** Satuan kerja tujuan peminjaman (pemilik dokumen yang dipinjam). */
	private SatuanKerja kepadaSatuanKerja;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public PeminjamanSuratItem() {
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

	/** @return kode unik transaksi peminjaman. */
	@Column(name = "kode", nullable = false, unique = true)
	public String getKode() {
		return this.kode;
	}

	/** @param kode kode unik transaksi yang akan diset. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/** @return keterangan bebas transaksi peminjaman ini. */
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

	/** @param index urutan tampil baris ini yang akan diset. */
	public void setIndex(Long index) {
		this.index = index;
	}

	/** @return urutan tampil/nomor indeks baris ini. */
	public Long getIndex() {
		return index;
	}

	/** @return peminjam ({@link PeminjamSurat}) yang mengajukan transaksi ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjam_surat", nullable = true)
	public PeminjamSurat getPeminjamSurat() {
		return peminjamSurat;
	}

	/** @param peminjamSurat peminjam yang akan diset. */
	public void setPeminjamSurat(PeminjamSurat peminjamSurat) {
		this.peminjamSurat = peminjamSurat;
	}

	/** @return satuan kerja pemohon/peminjam (di-refresh via {@code check()} sebelum dikembalikan). */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja", nullable = true)
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/** @param satuanKerja satuan kerja pemohon yang akan diset. */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** @return header pengembalian ({@link KembaliSuratItem}) terkait transaksi ini, bila ada. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kembali_surat_item", nullable = true)
	public KembaliSuratItem getKembaliSuratItem() {
		return kembaliSuratItem;
	}

	/** @param kembaliSuratItem header pengembalian yang akan diset. */
	public void setKembaliSuratItem(KembaliSuratItem kembaliSuratItem) {
		this.kembaliSuratItem = kembaliSuratItem;
	}

	/**
	 * Menghitung ulang batas jumlah hari peminjaman dari selisih {@link #getMulai()} dan
	 * {@link #getSampai()} (dengan format ulang tanggal ke {@code databaseDateFormat} untuk
	 * menghindari selisih akibat komponen waktu/jam) setiap kali dipanggil — nilai field
	 * {@link #jumlahHariBatas} lama ditimpa hasil perhitungan ini.
	 *
	 * @return jumlah hari batas peminjaman (selisih hari antara tanggal mulai dan tanggal sampai).
	 */
	public Integer getJumlahHariBatas() {
		if (jumlahHariBatas == null) {
			jumlahHariBatas = 0;
		}

		Date tanggalMulai = getMulai();
		Date tanggalSelesai = getSampai();

		try {
			tanggalMulai = Common.databaseDateFormat.get().parse(Common.databaseDateFormat.get().format(tanggalMulai));
			tanggalSelesai = Common.databaseDateFormat.get().parse(Common.databaseDateFormat.get().format(tanggalSelesai));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		try {
//			System.out.println("tanggalMulai=>" + Common.dateFormat4.get().format(tanggalMulai));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sirkulasisurat/PeminjamanSuratItem.java:284");

		}

		try {
//			System.out.println("tanggalSelesai=>" + Common.dateFormat4.get().format(tanggalSelesai));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sirkulasisurat/PeminjamanSuratItem.java:290");

		}

		jumlahHariBatas = Common.getBetweenTwoDates(tanggalMulai, tanggalSelesai);

		return jumlahHariBatas;
	}

	/** @param jumlahHariBatas batas jumlah hari yang akan diset (akan dihitung ulang pada pemanggilan {@link #getJumlahHariBatas()} berikutnya). */
	public void setJumlahHariBatas(Integer jumlahHariBatas) {
		this.jumlahHariBatas = jumlahHariBatas;
	}

	/** @return jumlah maksimal dokumen yang boleh dipinjam sekaligus pada transaksi ini. */
	public Integer getJumlahMaksimalPeminjaman() {
		return jumlahMaksimalPeminjaman;
	}

	/** @param jumlahMaksimalPeminjaman jumlah maksimal dokumen yang akan diset. */
	public void setJumlahMaksimalPeminjaman(Integer jumlahMaksimalPeminjaman) {
		this.jumlahMaksimalPeminjaman = jumlahMaksimalPeminjaman;
	}

	/** @return label jenis item yang dipinjam; default {@code "surat"} bila belum diisi. */
	public String getTipe() {
		return tipe == null ? "surat" : tipe;
	}

	/** @param tipe label jenis item yang akan diset. */
	public void setTipe(String tipe) {
		this.tipe = tipe;
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
	 * mempertahankan disposisi yang sudah tersimpan sebelumnya (mencegah alur SOP tertimpa
	 * referensi kosong/draft).
	 *
	 * @param disposisiSop alur disposisi SOP yang akan diset; diabaikan bila null/belum tersimpan.
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {if(disposisiSop==null||disposisiSop.getId()==null) {return;}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null)) ? this.disposisiSop : disposisiSop;
	}

	/** @return tanggal mulai peminjaman; default besok-lusa ({@link WaktuUtil#besoklusa()}) bila belum diisi. */
	@Temporal(TemporalType.DATE)
	public Date getMulai() {
		return mulai == null ? WaktuUtil.besoklusa() : mulai;
	}

	/** @param mulai tanggal mulai peminjaman yang akan diset. */
	public void setMulai(Date mulai) {
		this.mulai = mulai;
	}

	/** @return tanggal batas akhir peminjaman; default seminggu ke depan ({@link WaktuUtil#minggudepan()}) bila belum diisi. */
	@Temporal(TemporalType.DATE)
	public Date getSampai() {
		return sampai == null ? WaktuUtil.minggudepan() : sampai;
	}

	/** @param sampai tanggal batas akhir peminjaman yang akan diset. */
	public void setSampai(Date sampai) {
		this.sampai = sampai;
	}

	/** @return tujuan/alasan peminjaman; string kosong (bukan null) bila belum diisi. */
	@Column(columnDefinition = "text")
	public String getTujuanPeminjaman() {
		return tujuanPeminjaman == null ? "" : tujuanPeminjaman;
	}

	/** @param tujuanPeminjaman tujuan peminjaman yang akan diset. */
	public void setTujuanPeminjaman(String tujuanPeminjaman) {
		this.tujuanPeminjaman = tujuanPeminjaman;
	}

	/** @return satuan kerja tujuan peminjaman (pemilik dokumen), di-refresh via {@code check()} sebelum dikembalikan. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kepada_satuan_kerja", nullable = true)
	public SatuanKerja getKepadaSatuanKerja() {
		kepadaSatuanKerja = check(kepadaSatuanKerja);
		return kepadaSatuanKerja;
	}

	/** @param kepadaSatuanKerja satuan kerja tujuan peminjaman yang akan diset. */
	public void setKepadaSatuanKerja(SatuanKerja kepadaSatuanKerja) {
		this.kepadaSatuanKerja = kepadaSatuanKerja;
	}
}
