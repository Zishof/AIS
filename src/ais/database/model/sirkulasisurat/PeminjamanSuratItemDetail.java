package ais.database.model.sirkulasisurat;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.surat.SuratMasuk;

/**
 * Entitas Hibernate (skema {@code surat}, tabel {@code peminjaman_surat_item_detail}) yang
 * menjadi DETAIL satu dokumen surat masuk ({@link SuratMasuk}) yang dipinjam dalam satu transaksi
 * {@link PeminjamanSuratItem} (header). Satu header bisa memiliki banyak baris detail (satu per
 * dokumen). Menghitung sendiri status keterlambatan ({@link #getJumlahHariTerlambat()}) dan batas
 * waktu pengembalian ({@link #getBatasWaktupengembalian()}), termasuk aturan hari libur yang bisa
 * dikonfigurasi (akhir pekan/libur nasional geser batas waktu).
 *
 * <p>
 * <b>WASPADAI</b>: tidak ada unique constraint pada kolom {@code surat_masuk} — kelas ini
 * (bersama {@link PeminjamanSuratItem}) tidak mencegah dokumen {@link SuratMasuk} yang sama
 * disimpan di lebih dari satu baris detail aktif (lihat catatan double-booking pada javadoc kelas
 * {@link PeminjamanSuratItem}).
 * </p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "surat", name = "peminjaman_surat_item_detail")
public class PeminjamanSuratItemDetail extends GeneralValueObject {

	/**
	 *
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
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

	/** @return representasi ringkas: dokumen {@link #suratMasuk} terkait detail ini. */
	public String toString() {
		return suratMasuk + "";
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

	/** Jumlah eksemplar dokumen yang dipinjam pada baris detail ini; default {@code 1.0} bila belum diisi. */
	private Double jumlah;
	/** Header transaksi peminjaman ({@link PeminjamanSuratItem}) pemilik detail ini. */
	private PeminjamanSuratItem peminjamanSuratItem;
	/** Keterangan bebas untuk detail dokumen ini. */
	private String keterangan;
	/** Dokumen surat masuk ({@link SuratMasuk}) yang dipinjam pada baris detail ini. */
	private SuratMasuk suratMasuk;
	/** Detail pengembalian ({@link KembaliSuratItemDetail}) terkait, bila dokumen ini sudah dikembalikan. */
	private KembaliSuratItemDetail kembaliSuratItemDetail;

	/** Jumlah hari keterlambatan pengembalian, dihitung ulang dari selisih {@link #getJumlahSelisihHari()} dikurangi {@link #getJumlahHariBatas()} (lihat {@link #getJumlahHariTerlambat()}). */
	private Integer jumlahHariTerlambat;
	/** Jumlah kali perpanjangan yang sudah dipakai untuk detail ini; default {@code 0} bila belum diisi. */
	private Integer jumlahPerpanjangan;
	/** Jumlah maksimal perpanjangan yang diizinkan untuk detail ini. */
	private Integer jumlahMaxPerpanjangan;
	/** Batas jumlah hari peminjaman untuk detail ini, dihitung dari batas header dikalikan {@code (jumlahPerpanjangan + 1)} (lihat {@link #getJumlahHariBatas()}). */
	private Integer jumlahHariBatas;
	/** Selisih hari antara tanggal mulai dan tanggal kembali/rencana selesai header (lihat {@link #getJumlahSelisihHari()}). */
	private Integer jumlahSelisihHari;

	/** Batas waktu pengembalian yang dihitung (tanggal kerja + penyesuaian hari libur, lihat {@link #hitungBatasWaktupengembalian()}). */
	private Date batasWaktupengembalian;
	/** Tanggal dokumen ini benar-benar dikembalikan, diselaraskan dari {@link #kembaliSuratItemDetail} bila ada (lihat {@link #getTanggalKembali()}). */
	private Date tanggalKembali;

	/** Konstruktor kosong (wajib untuk Hibernate). */
	public PeminjamanSuratItemDetail() {
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

	/** @return keterangan bebas detail dokumen ini. */
	@Column(name = "keterangan", nullable = true)
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan yang akan diset. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/** @param jumlah jumlah eksemplar dokumen yang akan diset. */
	public void setJumlah(Double jumlah) {
		this.jumlah = jumlah;
	}

	/** @return jumlah eksemplar dokumen yang dipinjam; default {@code 1.0} bila belum diisi. */
	public Double getJumlah() {
		if (jumlah == null) {
			jumlah = 1.0;
		}
		return jumlah;
	}

	/** @param peminjamanSuratItem header transaksi peminjaman pemilik detail ini, akan diset. */
	public void setPeminjamanSuratItem(PeminjamanSuratItem peminjamanSuratItem) {
		this.peminjamanSuratItem = peminjamanSuratItem;
	}

	/** @return header transaksi peminjaman ({@link PeminjamanSuratItem}) pemilik detail ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "peminjaman_surat_item", nullable = false)
	public PeminjamanSuratItem getPeminjamanSuratItem() {
		return peminjamanSuratItem;
	}

	/** @return dokumen surat masuk yang dipinjam pada baris detail ini. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "surat_masuk", nullable = false)
	public SuratMasuk getSuratMasuk() {
		return suratMasuk;
	}

	/** @param suratMasuk dokumen surat masuk yang akan diset. */
	public void setSuratMasuk(SuratMasuk suratMasuk) {
		this.suratMasuk = suratMasuk;
	}

	/**
	 * Menghitung ulang jumlah hari keterlambatan setiap kali dipanggil: selisih hari aktual
	 * ({@link #getJumlahSelisihHari()}) dikurangi batas hari yang diizinkan
	 * ({@link #getJumlahHariBatas()}); nilai negatif (belum lewat batas) di-clamp menjadi 0.
	 *
	 * @return jumlah hari keterlambatan (0 bila belum/tidak terlambat).
	 */
	public Integer getJumlahHariTerlambat() {

		int selisih = getJumlahSelisihHari();
		int batas = getJumlahHariBatas();
		jumlahHariTerlambat = (selisih - batas);

//		System.out.println(
//				"selisih => " + selisih + ", batas = " + batas + ", jumlahHariTerlambat = " + jumlahHariTerlambat);

		if (jumlahHariTerlambat == null || jumlahHariTerlambat < 0) {
			jumlahHariTerlambat = 0;
		}
		return jumlahHariTerlambat;
	}

	/** @param jumlahHariTerlambat jumlah hari keterlambatan yang akan diset (akan dihitung ulang pada pemanggilan {@link #getJumlahHariTerlambat()} berikutnya). */
	public void setJumlahHariTerlambat(Integer jumlahHariTerlambat) {
		this.jumlahHariTerlambat = jumlahHariTerlambat;
	}

	/** @return jumlah kali perpanjangan yang sudah dipakai; default {@code 0} bila belum diisi. */
	public Integer getJumlahPerpanjangan() {
		if (jumlahPerpanjangan == null) {
			jumlahPerpanjangan = 0;
		}
		return jumlahPerpanjangan;
	}

	/** @param jumlahPerpanjangan jumlah kali perpanjangan yang akan diset. */
	public void setJumlahPerpanjangan(Integer jumlahPerpanjangan) {
		this.jumlahPerpanjangan = jumlahPerpanjangan;
	}

	/**
	 * Menghitung ulang batas hari peminjaman dari batas hari header ({@code peminjamanSuratItem})
	 * dikalikan {@code (jumlahPerpanjangan + 1)} setiap kali dipanggil (perpanjangan menambah
	 * batas waktu secara linear).
	 *
	 * @return batas jumlah hari peminjaman untuk detail ini.
	 */
	public Integer getJumlahHariBatas() {

		if (peminjamanSuratItem != null) {
			jumlahHariBatas = ((getJumlahPerpanjangan() + 1) * peminjamanSuratItem.getJumlahHariBatas());
		}

		if (jumlahHariBatas == null) {
			jumlahHariBatas = 0;
		}
		return jumlahHariBatas;
	}

	/** @param jumlahHariBatas batas jumlah hari yang akan diset (akan dihitung ulang pada pemanggilan {@link #getJumlahHariBatas()} berikutnya bila header tersedia). */
	public void setJumlahHariBatas(Integer jumlahHariBatas) {
		this.jumlahHariBatas = jumlahHariBatas;
	}

	/** @return batas waktu pengembalian (dihitung ulang lewat {@link #hitungBatasWaktupengembalian()} setiap kali dipanggil). */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "batas_waktu_pengembalian")
	public Date getBatasWaktupengembalian() {
		hitungBatasWaktupengembalian();
		return batasWaktupengembalian;
	}

	/**
	 * Menghitung batas waktu pengembalian dari tanggal pembuatan header ditambah
	 * {@link #getJumlahHariBatas()} hari kerja, lalu menggeser mundur hasilnya sesuai konfigurasi
	 * yang aktif: {@code sabtu_dan_minggu_hari_libur_tanggal_kembali_mundur} (geser bila jatuh
	 * Sabtu/Minggu), {@code minggu_hari_libur_tanggal_kembali_mundur} (geser bila jatuh Minggu
	 * saja), dan {@code libur_nasional_hari_libur_tanggal_kembali_mundur} (geser melewati tanggal
	 * yang terdaftar di {@code Common.hariLiburPerpustakaans}, maksimal 30 iterasi pencarian).
	 * Hasil akhir disimpan ke {@link #batasWaktupengembalian}.
	 */
	public void hitungBatasWaktupengembalian() {
		Date batas = Common.getDateWorkingDays(peminjamanSuratItem.getTanggalPembuatan(), getJumlahHariBatas());

//		System.out.println("batas default => " + Common.dateFormat1.get().format(batas));
		Calendar c = ais.ui.util.WaktuUtil.getCalendar();
		c.setTime(batas);

		if (Common.bolehKonfigurasi("sabtu_dan_minggu_hari_libur_tanggal_kembali_mundur", Konfigurasi.TIDAK_AKTIF)) {
			if (Calendar.SATURDAY == c.get(Calendar.DAY_OF_WEEK)) {
				c.set(Calendar.DATE, c.get(Calendar.DATE) + 2);
			} else if (Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK)) {
				c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
			}
//			System.out.println(
//					"sabtu_dan_minggu_hari_libur_tanggal_kembali_mundur  => " + Common.dateFormat1.get().format(batas));
		} else if (Common.bolehKonfigurasi("minggu_hari_libur_tanggal_kembali_mundur")) {
			if (Calendar.SUNDAY == c.get(Calendar.DAY_OF_WEEK)) {
				c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
			}
//			System.out.println("minggu_hari_libur_tanggal_kembali_mundur  => " + Common.dateFormat1.get().format(batas));
		}

		if (Common.bolehKonfigurasi("libur_nasional_hari_libur_tanggal_kembali_mundur")) {
			if (!Common.hariLiburPerpustakaans.isEmpty()) {
				List<String> libursNanti = new ArrayList<String>();
				for (Date date : Common.hariLiburPerpustakaans.keySet()) {
					libursNanti.add(Common.dateFormat1.get().format(date));
				}

				for (int i = 1; i < 30; i++) {
					if (!libursNanti.contains(Common.dateFormat1.get().format(c.getTime()))) {
						break;
					}
					c.set(Calendar.DATE, c.get(Calendar.DATE) + 1);
				}
				libursNanti = null;

//				System.out.println(
//						"libur_nasional_hari_libur_tanggal_kembali_mundur => " + Common.dateFormat1.get().format(batas));
			}

		}

		setBatasWaktupengembalian(c.getTime());
	}

	/** @param batasWaktupengembalian batas waktu pengembalian yang akan diset (dipanggil internal oleh {@link #hitungBatasWaktupengembalian()}). */
	public void setBatasWaktupengembalian(Date batasWaktupengembalian) {
		this.batasWaktupengembalian = batasWaktupengembalian;
	}

	/**
	 * Menghitung ulang selisih hari antara tanggal mulai dan tanggal sampai/kembali header setiap
	 * kali dipanggil (format ulang tanggal ke {@code databaseDateFormat} untuk menghindari selisih
	 * akibat komponen waktu/jam); turut memperbarui {@link #tanggalKembali} dari
	 * {@link #kembaliSuratItemDetail} bila sudah ada.
	 *
	 * @return jumlah selisih hari antara tanggal mulai dan tanggal selesai/kembali.
	 */
	public Integer getJumlahSelisihHari() {
		// System.out.println("kembaliSuratItemDetail = " +
		// kembaliSuratItemDetail);

		tanggalKembali = kembaliSuratItemDetail == null || kembaliSuratItemDetail.getTanggal() == null ? null
				: kembaliSuratItemDetail.getTanggal();

		Date tanggalMulai = peminjamanSuratItem.getMulai();
		Date tanggalSelesai = peminjamanSuratItem.getSampai();

		try {
			tanggalMulai = Common.databaseDateFormat.get().parse(Common.databaseDateFormat.get().format(tanggalMulai));
			tanggalSelesai = Common.databaseDateFormat.get().parse(Common.databaseDateFormat.get().format(tanggalSelesai));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			Common.tampilErrorJikaAdmin(e);
		}

		try {
//			System.out.println("tanggalMulai=>" + Common.dateFormat4.get().format(tanggalMulai));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sirkulasisurat/PeminjamanSuratItemDetail.java:278");

		}

		try {
//			System.out.println("tanggalSelesai=>" + Common.dateFormat4.get().format(tanggalSelesai));
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/sirkulasisurat/PeminjamanSuratItemDetail.java:284");

		}

		jumlahSelisihHari = Common.getBetweenTwoDates(tanggalMulai, tanggalSelesai);

		// System.out.println("tanggalMulai = " +
		// Common.dateFormat3.get().format(tanggalMulai) + ", tanggalSelesai = "
		// + Common.dateFormat3.get().format(tanggalSelesai) + ", jumlahSelisihHari =
		// " + jumlahSelisihHari);

		return jumlahSelisihHari;
	}

	/** @param jumlahSelisihHari selisih hari yang akan diset (akan dihitung ulang pada pemanggilan {@link #getJumlahSelisihHari()} berikutnya). */
	public void setJumlahSelisihHari(Integer jumlahSelisihHari) {
		this.jumlahSelisihHari = jumlahSelisihHari;
	}

	/** @return detail pengembalian ({@link KembaliSuratItemDetail}) terkait, bila dokumen ini sudah dikembalikan. */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kembali_surat_item_detail", nullable = true)
	public KembaliSuratItemDetail getKembaliSuratItemDetail() {
		return kembaliSuratItemDetail;
	}

	/**
	 * @param kembaliSuratItemDetail detail pengembalian yang akan diset; diset ke {@code null}
	 *                               bila argumennya null atau belum tersimpan (ID null), mencegah
	 *                               relasi merujuk ke entitas draft yang belum punya identitas DB.
	 */
	public void setKembaliSuratItemDetail(KembaliSuratItemDetail kembaliSuratItemDetail) {
		this.kembaliSuratItemDetail = kembaliSuratItemDetail == null || kembaliSuratItemDetail.getId() == null ? null
				: kembaliSuratItemDetail;
	}

	/** @return jumlah maksimal perpanjangan yang diizinkan untuk detail ini; default {@code 0} bila belum diisi. */
	public Integer getJumlahMaxPerpanjangan() {
		if (jumlahMaxPerpanjangan == null) {
			jumlahMaxPerpanjangan = 0;
		}
		return jumlahMaxPerpanjangan;
	}

	/** @param jumlahMaxPerpanjangan jumlah maksimal perpanjangan yang akan diset. */
	public void setJumlahMaxPerpanjangan(Integer jumlahMaxPerpanjangan) {
		this.jumlahMaxPerpanjangan = jumlahMaxPerpanjangan;
	}

	/** @return tanggal dokumen ini benar-benar dikembalikan, diselaraskan dari {@link #kembaliSuratItemDetail} bila sudah ada. */
	@Temporal(TemporalType.DATE)
	public Date getTanggalKembali() {
		if (kembaliSuratItemDetail != null && kembaliSuratItemDetail.getTanggal() != null) {
			tanggalKembali = kembaliSuratItemDetail.getTanggal();
		}

		return tanggalKembali;
	}

	/** @param tanggalKembali tanggal pengembalian yang akan diset. */
	public void setTanggalKembali(Date tanggalKembali) {
		this.tanggalKembali = tanggalKembali;
	}

}
