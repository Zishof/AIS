package ais.database.model.asset;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

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
import org.zkoss.zul.Row;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.CommonVO;
import ais.database.model.ParameterTambahan;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.WaktuUtil;

/**
 * Catatan PERBAIKAN / SERVIS satu unit fisik aset ({@link AssetDetail}) -- riwayat perawatan
 * yang berjalan lewat alur disposisi SOP ({@link DisposisiSop}), bukan riwayat akuntansi.
 *
 * <h3>Kapitalisasi biaya: TIDAK ADA -- diverifikasi dari struktur entitas</h3>
 *
 * <p>Kelas ini TIDAK memiliki field biaya/nilai uang apa pun (tidak ada {@code biaya},
 * {@code harga}, atau sejenisnya), dan TIDAK memiliki relasi ke {@link
 * ais.database.model.akunting.PostingHistory} -- berbeda dari {@link PenyusutanAsset} yang
 * memegang {@code postingHistory} untuk jurnal penyusutan. Pencarian atas seluruh paket
 * {@code ais.action.master.asset} juga tidak menemukan kelas {@code PostingPerbaikanAssetAction}
 * (bandingkan dengan {@code PostingPenyusutanAssetAction} yang ADA untuk {@link
 * PenyusutanAsset}). Kesimpulannya: biaya perbaikan, sejauh ada, hanya bisa dicatat lewat
 * mekanisme parameter tambahan dinamis ({@link #populateParameterTambahan(List)}) sebagai teks
 * bebas -- TIDAK PERNAH menambah {@link AssetDetail#getHargaBeli()} atau nilai buku di {@link
 * PenyusutanAsset#getNilaiBuku()}, dan TIDAK PERNAH memicu posting jurnal. Perbaikan aset di AIS
 * karena itu murni biaya operasional/administratif yang terpisah dari akuntansi aset tetap --
 * bukan pilihan desain yang salah, tetapi berarti setiap kebutuhan pelaporan biaya perbaikan
 * bergantung sepenuhnya pada isi bebas kolom parameter tambahan, tanpa validasi tipe numerik
 * ataupun rekonsiliasi dengan nilai buku aset.</p>
 *
 * <h3>Alur kerja: mengikuti SOP, bukan Generic CRUD sederhana</h3>
 *
 * <p>Kelas induk {@link DataSop} mewajibkan {@link #getDisposisiSop()}/{@link
 * #setDisposisiSop(DisposisiSop)}, sehingga setiap baris perbaikan terikat pada satu simpul
 * disposisi dalam alur SOP (persetujuan/penolakan berjenjang). Status {@link #getAktif()}
 * DITURUNKAN dari status disposisi tersebut -- lihat javadoc getter itu -- bukan disimpan bebas
 * seperti flag aktif pada entitas master data biasa.</p>
 *
 * <h3>Parameter tambahan dinamis</h3>
 *
 * <p>Sama seperti pola berulang di modul lain (mis. penggajian/koperasi), form perbaikan bisa
 * memiliki field tambahan yang dikonfigurasi lewat {@link JenisPerbaikanAsset} &rarr; {@link
 * KelompokParameterTambahanPerbaikanAsset} &rarr; {@link ParameterTambahanPerbaikanAsset} &rarr;
 * {@link ais.database.model.ParameterTambahan}. Kunci atribut baris ZK yang dipakai di sini --
 * {@code "parameterTambahan"} dan {@code "kelompokParameterTambahanPerbaikanAsset"} pada {@link
 * #populateParameterTambahan(List)} -- SUDAH DIVERIFIKASI SAMA PERSIS dengan kunci yang ditulis
 * oleh {@link ais.action.master.helper.ParameterTambahanPerbaikanAssetListener#onEvent} (baris
 * {@code row.setAttribute("parameterTambahan", ...)} dan
 * {@code row.setAttribute("kelompokParameterTambahanPerbaikanAsset", ...)}) dan dengan kunci yang
 * dibaca listener itu sendiri di {@code validate()}. TIDAK ADA tabrakan kunci-ZK-salah-modul di
 * sini seperti yang pernah ditemukan pada lokasi lain (bug {@code task_fe6517bf}, yang terisolasi
 * di lokasi tersebut) -- ketiga titik baca/tulis di klaster {@code PerbaikanAsset} ini konsisten.</p>
 *
 * @see AssetDetail unit fisik yang diperbaiki
 * @see JenisPerbaikanAsset katalog jenis perbaikan sekaligus akar konfigurasi parameter tambahan
 * @see PenyusutanAsset jadwal penyusutan unit yang sama (jalur akuntansi terpisah)
 * @see ais.action.master.helper.ParameterTambahanPerbaikanAssetListener pengelola baris parameter tambahan di form
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "asset", name = "perbaikan_asset")
public class PerbaikanAsset extends DataSop {

	/**
	 * Penanda versi serialisasi Java; sama dengan entitas sepaket lain karena berasal dari
	 * templat hbm2java yang sama.
	 */
	private static final long serialVersionUID = 2463821577548439808L;

	/** Kunci utama baris, kolom {@code id}; di-generate database (IDENTITY). */
	private Long id;

	/** Nama tampil pengguna terakhir yang menyunting baris ini. */
	private String oleh;

	/** Id pengguna terakhir yang menyunting baris ini. */
	private String olehId;

	/**
	 * Id pengguna terakhir yang menyunting baris ini.
	 *
	 * @return id pengguna penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Mengisi id pengguna penyunting terakhir, MENGABAIKAN nilai kosong agar jejak audit lama
	 * tidak tertimpa oleh proses batch tanpa konteks pengguna aktif.
	 *
	 * @param olehId id pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengisi nama pengguna penyunting terakhir, MENGABAIKAN nilai kosong.
	 *
	 * @param oleh nama pengguna penyunting; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Nama tampil pengguna terakhir yang menyunting baris ini.
	 *
	 * @return nama penyunting terakhir, atau {@code null} bila belum terisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Kait JPA {@code @PreUpdate}: memperbarui stempel waktu audit sebelum UPDATE dikirim.
	 * Didelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar aturannya terpusat.
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Stempel waktu penyuntingan terakhir, bernilai awal waktu server saat objek dibuat. Bidang
	 * audit ini diulang di tiap entitas AIS sebagai KEHARUSAN TEKNIS: kelas induk tidak
	 * mewariskan pemetaan kolom apa pun untuknya.
	 */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Mengisi stempel waktu penyuntingan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu baru
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Stempel waktu penyuntingan terakhir baris ini.
	 *
	 * @return stempel waktu; tidak pernah {@code null} untuk objek hasil konstruktor
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Representasi teks berupa id digabung nama, dipakai label komponen ZK dan pesan log.
	 *
	 * @return {@code "<id>-<nama>"}; bagian nama bisa berupa string {@code "null"} literal bila
	 *         field {@code nama} belum terisi (dipakai apa adanya, bukan lewat getter)
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Nama baris perbaikan; diwarisi dari {@link JenisPerbaikanAsset} bila kosong, lihat {@link #getNama()}. */
	private String nama;

	/** Keterangan/uraian perbaikan yang dilakukan. */
	private String keterangan;

	/** Unit fisik aset ({@link AssetDetail}) yang diperbaiki. */
	private AssetDetail assetDetail;

	/** Waktu perbaikan dilakukan/dicatat. */
	private Date waktu;

	/** Jenis perbaikan (katalog); juga akar konfigurasi parameter tambahan yang berlaku. */
	private JenisPerbaikanAsset jenisPerbaikanAsset;

	/** Satuan kerja yang mengajukan/menanggung perbaikan ini. */
	private SatuanKerja satuanKerja;

	/** Isian parameter tambahan berformat teks, kunci berupa label; lihat {@link #getParameterTambahan()}. */
	private String parameterTambahan;

	/** Isian parameter tambahan berformat teks, kunci berupa id (lebih stabil terhadap perubahan label); lihat {@link #getParameterTambahanInds()}. */
	private String parameterTambahanInds;

	/** Simpul disposisi SOP yang menaungi baris perbaikan ini; menentukan {@link #getAktif()}. */
	private DisposisiSop disposisiSop;

	/** Penanda apakah pemberitahuan (broadcast/email) perlu dikirim untuk perbaikan ini. */
	private Boolean broadcast;

	/** Tahun pencatatan, dipakai penomoran/pengelompokan per tahun. */
	private Integer tahun;

	/** Bulan pencatatan, dipakai penomoran/pengelompokan per bulan. */
	private Integer bulan;

	/** Kode/nomor dokumen perbaikan. */
	private String kode;

	/** Nomor urut tampilan pada daftar (bukan bagian dari identitas basis data). */
	private Long index;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate dan ZK data binding.
	 */
	public PerbaikanAsset() {
	}

	/**
	 * Kunci utama baris.
	 *
	 * <p>{@code insertable = false} karena nilainya di-generate database.</p>
	 *
	 * @return id baris, atau {@code null} bila objek belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Mengisi kunci utama; umumnya hanya dipanggil Hibernate seusai INSERT.
	 *
	 * @param id kunci utama baru
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Kode/nomor dokumen perbaikan.
	 *
	 * @return kode hasil {@code trim()}, atau {@code null} bila belum terisi ATAU hasil trim
	 *         kosong (berbeda dari kebanyakan getter kode lain di paket ini yang mengembalikan
	 *         string kosong, bukan {@code null})
	 */
	@Column(name = "kode")
	public String getKode() {
		return this.kode == null || kode.trim().isEmpty() ? null : kode.trim();
	}

	/**
	 * Mengisi kode/nomor dokumen perbaikan.
	 *
	 * @param kode kode baru
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Nama baris perbaikan -- getter DESTRUKTIF yang mewarisi nama dari {@link
	 * JenisPerbaikanAsset} bila kosong.
	 *
	 * <p>Bila field {@code nama} belum terisi (atau string kosong) DAN {@link
	 * #getJenisPerbaikanAsset()} mengembalikan objek bukan {@code null}, nama jenis perbaikan
	 * DITULISKAN ke field {@code nama} in-memory saat getter ini dipanggil. Karena entitas memakai
	 * akses properti (anotasi diletakkan di getter), pemanggilan getter oleh Hibernate saat
	 * pemeriksaan dirty-checking membuat nilai turunan itu ikut ter-flush ke basis data --
	 * mengikuti pola yang sama dengan getter destruktif lain di paket aset ini (lihat javadoc
	 * {@code AssetDetail}). Efek sampingnya: memanggil getter ini SEBELUM
	 * {@code jenisPerbaikanAsset} diisi menghasilkan {@code null}, tetapi memanggilnya SESUDAH
	 * {@code jenisPerbaikanAsset} diisi bisa MENIMPA nama kustom yang sengaja dikosongkan
	 * pengguna dengan nama jenis perbaikan.</p>
	 *
	 * @return nama hasil {@code trim()}, atau {@code null} bila field kosong dan
	 *         {@code jenisPerbaikanAsset} juga tidak tersedia
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if ((nama == null || nama.isEmpty()) && getJenisPerbaikanAsset() != null) {
			nama = getJenisPerbaikanAsset().getNama();
		}
		return this.nama == null ? null : this.nama.trim();
	}

	/**
	 * Mengisi nama baris perbaikan.
	 *
	 * @param nama nama baru
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengisi nomor urut tampilan pada daftar.
	 *
	 * @param index nomor urut baru; bukan bagian dari identitas basis data
	 */
	public void setIndex(Long index) {
		this.index = index;
	}

	/**
	 * Nomor urut tampilan pada daftar.
	 *
	 * @return nomor urut, atau {@code null} bila belum diisi
	 */
	public Long getIndex() {
		return index;
	}

	/**
	 * Keterangan/uraian perbaikan.
	 *
	 * <p>Mengembalikan teks kosong (bukan {@code null}) bila belum terisi, agar komponen ZK yang
	 * terikat langsung ke properti ini tidak menampilkan "null".</p>
	 *
	 * @return keterangan, atau {@code ""} bila belum terisi
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan == null ? "" : keterangan;
	}

	/**
	 * Mengisi keterangan/uraian perbaikan.
	 *
	 * @param keterangan teks keterangan baru
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Waktu perbaikan dilakukan/dicatat.
	 *
	 * @return waktu tersimpan, atau waktu server saat ini ({@link WaktuUtil#getDate()}) bila
	 *         belum diisi -- nilai fallback ini TIDAK ditulis balik ke field, berbeda dari
	 *         beberapa getter destruktif lain di kelas ini
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		return waktu == null ? WaktuUtil.getDate() : waktu;
	}

	/**
	 * Mengisi waktu perbaikan.
	 *
	 * @param waktu waktu baru
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Jenis perbaikan (katalog) yang berlaku untuk baris ini; sekaligus akar konfigurasi
	 * parameter tambahan lewat {@link JenisPerbaikanAsset#getKelompokParameterTambahanPerbaikanAssets()}.
	 *
	 * <p>Nama kolom FK basis data ({@code jenis_catatan_administrasi}) berbeda dari nama properti
	 * Java ({@code jenisPerbaikanAsset}) -- sisa penamaan dari domain "catatan administrasi" yang
	 * lebih umum sebelum kolom ini dikhususkan untuk perbaikan aset; tidak memengaruhi perilaku
	 * karena Hibernate memetakan lewat anotasi {@code @JoinColumn}, bukan penyesuaian nama.
	 * Dilewatkan lewat {@link #check(Object)} untuk memastikan proxy lazy sudah teresolusi
	 * (atau diambil dari cache identitas entitas) sebelum dipakai.</p>
	 *
	 * @return jenis perbaikan, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "jenis_catatan_administrasi")
	public JenisPerbaikanAsset getJenisPerbaikanAsset() {
		jenisPerbaikanAsset = check(jenisPerbaikanAsset);
		return jenisPerbaikanAsset;
	}

	/**
	 * Menetapkan jenis perbaikan.
	 *
	 * @param jenisPerbaikanAsset jenis baru
	 */
	public void setJenisPerbaikanAsset(JenisPerbaikanAsset jenisPerbaikanAsset) {
		this.jenisPerbaikanAsset = jenisPerbaikanAsset;
	}

	/**
	 * Isian parameter tambahan berformat teks, kunci per baris berupa ID kelompok dan ID
	 * parameter (bukan label) -- lebih stabil terhadap perubahan label tampilan dibanding
	 * {@link #getParameterTambahan()}.
	 *
	 * <p>Format satu baris: {@code "kelompokId->parameterId<=>nilai<=>url<=>keterangan"},
	 * dipisah newline antar parameter. Ditulis oleh {@link #populateParameterTambahan(List)} dan
	 * dibaca ulang oleh {@link ais.action.master.helper.ParameterTambahanPerbaikanAssetListener}
	 * untuk memulihkan isian form saat mode edit.</p>
	 *
	 * @return string terenkode, atau {@code ""} bila belum ada parameter tambahan terisi
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahanInds() {
		if (parameterTambahanInds == null) {
			parameterTambahanInds = "";
		}

		return parameterTambahanInds;
	}

	/**
	 * Mengisi string parameter tambahan berbasis ID.
	 *
	 * @param parameterTambahanInds string terenkode baru
	 */
	public void setParameterTambahanInds(String parameterTambahanInds) {
		this.parameterTambahanInds = parameterTambahanInds;
	}

	/**
	 * Mengurai {@link #getParameterTambahan()} (format berbasis label) menjadi daftar {@link
	 * CommonVO} siap tampil, satu {@code CommonVO} per baris parameter.
	 *
	 * <p>Setiap baris berformat {@code "label<=>nilai<=>url<=>nomorUrut<=>id"} (dipisah newline
	 * antar-parameter, dipisah {@code "<=>"} antar-kolom dalam satu parameter); label sendiri
	 * memuat prefiks {@code "namaKelompok->labelParameter"} yang dipecah lagi lewat pemisah
	 * {@code "->"} untuk mengambil nama kelompok murni ke {@code CommonVO.name5}. Kolom nomor urut
	 * dan id yang gagal di-parse (bukan angka) DIABAIKAN diam-diam (blok {@code catch} kosong,
	 * fallback ke {@code 1} dan {@code 1L}) -- baris tetap tampil, hanya urutan/identitasnya yang
	 * memakai nilai default. Hasil akhir diurutkan lewat {@link Collections#sort(List)} memakai
	 * {@link CommonVO#compareTo}, yang mengurutkan berdasarkan nomor urut.</p>
	 *
	 * @return daftar {@link CommonVO} terurut, satu per parameter tambahan yang tersimpan
	 */
	public List<CommonVO> ambilDataParameterTambahan() {
		List<CommonVO> commonVOs = new ArrayList<CommonVO>();
		String[] splNama = getParameterTambahan().split("\n");
		for (int j = 0; j < splNama.length; j++) {
			CommonVO commonVO = new CommonVO();
			String namaCol = splNama.length > j ? splNama[j] : "";

			String[] value = namaCol.split("<=>");
			String lbl = value.length > 0 ? value[0].trim() : "";
			String url = value.length > 2 ? value[2].trim() : "";
			String val = value.length > 1 ? value[1].trim() : "";
			Integer nomorUrut = 1;
			try {
				nomorUrut = value.length > 3 ? Integer.parseInt(value[3].trim()) : 1;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/asset/PerbaikanAsset.java:213");

			}
			Long id = 1L;
			try {
				id = value.length > 4 ? Long.parseLong(value[4].trim()) : 1L;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/asset/PerbaikanAsset.java:219");

			}

			// System.out.println("namaCol=> " + namaCol + ", lbl=> " + lbl + ", val=> " +
			// val + ", url=>" + url);

			String[] param = lbl.split("->");

			commonVO.setId(id.toString());
			commonVO.setName(lbl);
			commonVO.setName1(val);
			commonVO.setName2(url);
			commonVO.setName5(param[0]);
			commonVO.setNomorUrut(nomorUrut);
			commonVOs.add(commonVO);
		}
		Collections.sort(commonVOs);
		return commonVOs;
	}

	/**
	 * Menulis nilai isian parameter tambahan dari baris-baris form ({@code parameterRows}) ke
	 * kedua string terenkode entitas ini ({@link #getParameterTambahan()} dan {@link
	 * #getParameterTambahanInds()}) -- inilah satu-satunya cara isian dinamis form perbaikan aset
	 * benar-benar tersimpan.
	 *
	 * <h3>Sumber data dan kunci atribut baris</h3>
	 *
	 * <p>Untuk tiap {@link Row} pada {@code parameterRows}, method ini membaca dua atribut yang
	 * SEBELUMNYA ditulis oleh {@link ais.action.master.helper.ParameterTambahanPerbaikanAssetListener#onEvent}
	 * saat baris dibangun: {@code "parameterTambahan"} (instance {@link
	 * ais.database.model.ParameterTambahan}) dan {@code "kelompokParameterTambahanPerbaikanAsset"}
	 * (instance {@link KelompokParameterTambahanPerbaikanAsset}). KUNCI INI SUDAH DIVERIFIKASI
	 * SAMA PERSIS dengan yang ditulis listener -- lihat javadoc kelas ini untuk perbandingan
	 * eksplisit dengan bug kunci-ZK-salah-modul yang pernah ditemukan di lokasi lain
	 * ({@code task_fe6517bf}). Baris yang salah satu dari kedua atributnya {@code null} (baris
	 * judul kelompok, atau baris yang belum sempat diisi listener) dilewati tanpa efek.</p>
	 *
	 * <h3>Nilai per parameter</h3>
	 *
	 * <p>Untuk tiap baris valid: nilai isian diambil lewat {@link
	 * ais.database.model.ParameterTambahan#ambilVal(Row, ais.database.model.ParameterTambahan)}
	 * (membaca komponen ZK aktual di baris -- textbox, combobox, dsb. tergantung tipe
	 * parameter); jenis lampiran dihitung lewat {@link
	 * LampiranLain#resolveJenisParameterTambahan(Class, Long, String)} memakai kunci komposit
	 * {@code "kelompokId->parameterId"}; dan bila parameter mensyaratkan lampiran ({@link
	 * ais.database.model.ParameterTambahan#getHarusMenyertakanLampiran()}), URI unduhan lampiran
	 * yang sudah tersimpan (bila ada) diambil lewat {@link LampiranLain#ambil(Long, String)} --
	 * kegagalan membuat URI (mis. berkas hilang dari storage) DITELAN oleh {@link
	 * ais.common.Common#tampilErrorJikaAdmin(Exception)} dan tidak menghentikan penyimpanan
	 * baris lain.</p>
	 *
	 * <h3>Dua format keluaran</h3>
	 *
	 * <p>Setiap baris menghasilkan DUA representasi yang digabung terpisah dengan newline ke dua
	 * field berbeda: {@code parameterTambahanStr} (format berbasis LABEL --
	 * {@code "namaKelompok->labelInputan<=>nilai<=>url<=>nomorUrut<=>parameterId<=>kelompokId<=>keterangan"},
	 * dipakai tampilan/laporan yang butuh teks terbaca manusia) dan {@code sIds} (format berbasis
	 * ID -- {@code "kelompokId->parameterId<=>nilai<=>url<=>keterangan"}, dipakai
	 * pemulihan isian form karena stabil terhadap perubahan label). Kegagalan mengurai satu baris
	 * (exception apa pun saat membaca atribut/komponen) DITANGKAP per-baris oleh {@link
	 * ais.common.Common#tampilErrorJikaAdmin(Exception)} sehingga baris bermasalah dilewati tanpa
	 * menggagalkan penyimpanan baris lain -- baik string label maupun string id ditulis lewat
	 * {@link #setParameterTambahanInds(String)} dan {@link #setParameterTambahan(String)} SETELAH
	 * seluruh baris selesai diproses (satu kali tulis di akhir, bukan per-baris).</p>
	 *
	 * @param parameterRows daftar baris form parameter tambahan; method langsung kembali tanpa
	 *                       efek bila {@code null} atau kosong (isian lama TIDAK dikosongkan
	 *                       dalam kasus ini -- hanya diabaikan)
	 */
	public void populateParameterTambahan(List<Row> parameterRows) {
		if (parameterRows == null || parameterRows.isEmpty()) {
			return;
		}

		String parameterTambahanStr = "";
		String parameterTambahanInds = "";
		for (Row row : parameterRows) {
			try {
				ParameterTambahan parameterTambahan = (ParameterTambahan) row.getAttribute("parameterTambahan");
				KelompokParameterTambahanPerbaikanAsset kelompokParameterTambahanPerbaikanAsset = (KelompokParameterTambahanPerbaikanAsset) row
						.getAttribute("kelompokParameterTambahanPerbaikanAsset");
				if (parameterTambahan != null && kelompokParameterTambahanPerbaikanAsset != null) {
					String jenis = LampiranLain.resolveJenisParameterTambahan(PerbaikanAsset.class, getId(),
							kelompokParameterTambahanPerbaikanAsset.getId() + "->" + parameterTambahan.getId());

					String val = ParameterTambahan.ambilVal(row, parameterTambahan);
					Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
							&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan")
									: null);
					String url = "";
					if (parameterTambahan.getHarusMenyertakanLampiran()) {

						LampiranLain lam = LampiranLain.ambil(getId(), jenis);
						if (lam != null) {
							try {
								url = lam.createLinkUri();
							} catch (Exception e) {
								Common.tampilErrorJikaAdmin(e);
							}
						}

					}

					String s = kelompokParameterTambahanPerbaikanAsset.getNama() + "->"
							+ parameterTambahan.getLabelInputan() + "<=>" + val + "<=>" + url + "<=>"
							+ parameterTambahan.getNomorUrut() + "<=>" + parameterTambahan.getId() + "<=>"
							+ kelompokParameterTambahanPerbaikanAsset.getId() + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());

					parameterTambahanStr += parameterTambahanStr.isEmpty() ? s : "\n" + s;

					String sIds = kelompokParameterTambahanPerbaikanAsset.getId() + "->" + parameterTambahan.getId()
							+ "<=>" + val + "<=>" + url + "<=>"
							+ (keterangan == null ? "" : keterangan.getValue().trim());
					parameterTambahanInds += parameterTambahanInds.isEmpty() ? sIds : "\n" + sIds;
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		}
		// System.out.println("parameterTambahanStr => " + parameterTambahanStr);
		// System.out.println("parameterTambahanInds => " + parameterTambahanInds);
		setParameterTambahanInds(parameterTambahanInds);
		setParameterTambahan(parameterTambahanStr);
	}

	/**
	 * Isian parameter tambahan berformat teks, kunci berupa LABEL kelompok/parameter (bukan ID) --
	 * lebih mudah dibaca manusia (dipakai laporan/tampilan), tetapi rapuh bila label dikonfigurasi
	 * ulang di master data karena string lama tidak ikut berubah. Lihat {@link
	 * #getParameterTambahanInds()} untuk varian berbasis ID yang dipakai pemulihan form.
	 *
	 * @return string terenkode, atau {@code ""} bila belum ada parameter tambahan terisi
	 */
	@Column(columnDefinition = "text")
	public String getParameterTambahan() {
		if (parameterTambahan == null) {
			parameterTambahan = "";
		}

		return parameterTambahan;
	}

	/**
	 * Mengisi string parameter tambahan berbasis label.
	 *
	 * @param parameterTambahan string terenkode baru
	 */
	public void setParameterTambahan(String parameterTambahan) {
		this.parameterTambahan = parameterTambahan;
	}

	/**
	 * Satuan kerja yang mengajukan/menanggung perbaikan ini.
	 *
	 * <p>Dilewatkan lewat {@link #check(Object)} untuk memastikan proxy lazy sudah teresolusi.
	 * Berbeda dari beberapa entitas aset lain (mis. {@code AssetDetail}), kolom ini TIDAK
	 * diturunkan otomatis dari {@link #getAssetDetail()} -- nilainya independen dan harus diisi
	 * eksplisit saat pembuatan baris.</p>
	 *
	 * @return satuan kerja, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "satuan_kerja")
	public SatuanKerja getSatuanKerja() {
		satuanKerja = check(satuanKerja);
		return satuanKerja;
	}

	/**
	 * Menetapkan satuan kerja penanggung jawab perbaikan.
	 *
	 * @param satuanKerja satuan kerja baru
	 */
	public void setSatuanKerja(SatuanKerja satuanKerja) {
		this.satuanKerja = satuanKerja;
	}

	/** Kode unik hasil kombinasi {@link #getKode()} dan id disposisi/baris; lihat {@link #getKodeUnik()}. */
	private String kodeUnik;

	/** Status aktif; DITURUNKAN dari status disposisi SOP, lihat {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Kode unik baris -- getter DESTRUKTIF yang MENGHITUNG ULANG nilainya setiap kali dipanggil
	 * dari {@link #getKode()} digabung id disposisi SOP (atau id baris sendiri bila belum
	 * berdisposisi), lalu MENULIS BALIK ke field {@code kodeUnik}. Karena kolom ini
	 * {@code unique = true} di basis data, kombinasi kode+disposisi/id inilah yang mencegah dua
	 * baris perbaikan dengan kode dokumen sama tetapi disposisi SOP berbeda saling bertabrakan.
	 * Efek turunan-tertulis ini mengikuti pola getter destruktif yang berulang di kelas-kelas
	 * ber-{@code check()}/turunan lain pada paket ini.
	 *
	 * @return {@code "<kode>_<idDisposisiSopAtauId>"}
	 */
	@Column(unique = true)
	public String getKodeUnik() {
		kodeUnik = getKode() + "" + (getDisposisiSop() == null ? "_" + getId() : "_" + getDisposisiSop().getId());
		return kodeUnik;
	}

	/**
	 * Mengisi kode unik secara manual; umumnya tidak perlu dipanggil karena {@link
	 * #getKodeUnik()} selalu menghitung ulang nilainya sendiri.
	 *
	 * @param kodeUnik nilai baru (akan tertimpa pada pemanggilan getter berikutnya)
	 */
	public void setKodeUnik(String kodeUnik) {
		this.kodeUnik = kodeUnik;
	}

	/**
	 * Simpul disposisi SOP yang menaungi baris perbaikan ini -- inti kontrak {@link DataSop} yang
	 * diwarisi kelas ini. Menentukan status {@link #getAktif()}: bila disposisi ini nonaktif,
	 * atau bila simpul akhir disposisi berada pada alur SOP yang menandai
	 * {@code penolakanAdaDiSini}, baris perbaikan ikut dianggap tidak aktif.
	 *
	 * <p>Dilewatkan lewat {@link #check(Object)} untuk memastikan proxy lazy sudah teresolusi.</p>
	 *
	 * @return simpul disposisi SOP, atau {@code null} bila belum diikat ke alur SOP mana pun
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	/**
	 * Menetapkan simpul disposisi SOP, MENGABAIKAN nilai yang {@code null} atau belum tersimpan
	 * (id {@code null}) -- perilaku fail-safe yang mencegah baris perbaikan yang sudah terikat
	 * pada satu disposisi ditulis-ulang ke disposisi kosong secara tidak sengaja. Perhatikan
	 * bahwa cek kedua di badan method ({@code this.disposisiSop != null && (disposisiSop == null
	 * || ...)}) tidak akan pernah tercapai dengan {@code disposisiSop} bernilai {@code null}
	 * karena guard clause di atasnya sudah return lebih dulu untuk kasus itu -- baris tersebut
	 * secara efektif redundan, sisa dari refactor sebelumnya; TIDAK berbahaya karena hasil akhirnya
	 * sama (disposisi lama dipertahankan) baik lewat guard clause maupun lewat cabang redundan itu.
	 *
	 * @param disposisiSop simpul disposisi baru; diabaikan bila {@code null} atau id-nya
	 *                      {@code null}
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	/**
	 * Penanda apakah pemberitahuan (broadcast/email) perlu dikirim untuk perbaikan ini; dipakai
	 * {@link ais.action.master.helper.BroadcastHelper#kirimEmailPerbaikanAsset(PerbaikanAsset)}
	 * sebagai salah satu syarat pengiriman (syarat lain: {@link #getKeterangan()} tidak kosong).
	 *
	 * @return {@code true} bila broadcast diminta; {@code false} bila belum diisi atau memang
	 *         dinonaktifkan
	 */
	public Boolean getBroadcast() {
		return broadcast == null ? false : broadcast;
	}

	/**
	 * Mengisi penanda broadcast.
	 *
	 * @param broadcast nilai baru
	 */
	public void setBroadcast(Boolean broadcast) {
		this.broadcast = broadcast;
	}

	/**
	 * Tahun pencatatan -- getter DESTRUKTIF yang mengisi field dengan tahun kalender berjalan
	 * (server) bila belum diisi, lalu menulis balik nilai tersebut ke field {@code tahun}.
	 *
	 * @return tahun tersimpan, atau tahun berjalan bila baru pertama kali dipanggil dan field
	 *         masih kosong
	 */
	public Integer getTahun() {
		if (tahun == null) {
			tahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		}
		return tahun;
	}

	/**
	 * Mengisi tahun pencatatan.
	 *
	 * @param tahun tahun baru
	 */
	public void setTahun(Integer tahun) {
		this.tahun = tahun;
	}

	/**
	 * Bulan pencatatan (1-12) -- getter DESTRUKTIF yang mengisi field dengan bulan kalender
	 * berjalan (server) bila belum diisi, lalu menulis balik nilai tersebut ke field
	 * {@code bulan}. Nilai {@link Calendar#MONTH} (basis 0) dikonversi ke basis 1 dengan
	 * menambahkan 1.
	 *
	 * @return bulan tersimpan (1-12), atau bulan berjalan bila baru pertama kali dipanggil dan
	 *         field masih kosong
	 */
	public Integer getBulan() {
		if (bulan == null) {
			bulan = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.MONTH) + 1;
		}
		return bulan;
	}

	/**
	 * Mengisi bulan pencatatan.
	 *
	 * @param bulan bulan baru (1-12)
	 */
	public void setBulan(Integer bulan) {
		this.bulan = bulan;
	}

	/**
	 * Unit fisik aset ({@link AssetDetail}) yang diperbaiki.
	 *
	 * <p>Dipetakan {@code Fetch(FetchMode.SELECT)} sehingga Hibernate menerbitkan SELECT terpisah
	 * untuk relasi ini alih-alih ikut dalam JOIN. Berbeda dari sebagian besar relasi lain di
	 * kelas ini, getter ini TIDAK memanggil {@link #check(Object)}, sehingga nilai yang
	 * dikembalikan bisa berupa proxy lazy yang belum teresolusi.</p>
	 *
	 * @return unit fisik yang diperbaiki, atau {@code null} bila belum diisi
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "asset_detail", nullable = true)
	public AssetDetail getAssetDetail() {
		return assetDetail;
	}

	/**
	 * Menetapkan unit fisik yang diperbaiki.
	 *
	 * @param assetDetail unit fisik baru
	 */
	public void setAssetDetail(AssetDetail assetDetail) {
		this.assetDetail = assetDetail;
	}

	/**
	 * Status aktif baris -- getter DESTRUKTIF yang MENURUNKAN status dari disposisi SOP terkait.
	 *
	 * <h3>Aturan yang dijalankan</h3>
	 *
	 * <p>Dipanggil ulang, field {@code disposisiSop} pertama-tama disegarkan lewat {@link
	 * #getDisposisiSop()}. Bila disposisi tersebut ada dan TIDAK aktif ({@code
	 * !disposisiSop.getAktif()}), field {@code aktif} entitas ini dipaksa {@code false}. Bila
	 * disposisi tersebut memiliki simpul akhir ({@code getDisposisiEnd()}) yang alur SOP-nya
	 * ({@code getAlurSop()}) menandai {@code getPenolakanAdaDiSini()} bernilai {@code true},
	 * {@code aktif} juga dipaksa {@code false} -- ini menangkap kasus alur SOP yang berakhir
	 * pada simpul penolakan, yang secara desain berarti pengajuan perbaikan ditolak. Kedua
	 * pemeriksaan bersifat SATU ARAH: tidak ada cabang yang MENGEMBALIKAN {@code aktif} ke
	 * {@code true} setelah pernah dipaksa {@code false} pada pemanggilan sebelumnya, dan
	 * default bila {@code aktif} belum pernah diisi/dipaksa adalah {@code true}. Karena akses
	 * properti dipakai Hibernate, nilai turunan ini TERTULIS ke basis data pada flush
	 * berikutnya begitu getter ini dipanggil.</p>
	 *
	 * @return {@code true} bila disposisi masih aktif dan tidak berakhir di simpul penolakan
	 *         (atau bila belum pernah dievaluasi/diisi manual); {@code false} begitu salah satu
	 *         kondisi nonaktif di atas terpenuhi
	 */
	public Boolean getAktif() {
		disposisiSop = getDisposisiSop();
		if (disposisiSop != null && !disposisiSop.getAktif()) {
			aktif = false;
		}
		if (disposisiSop != null && disposisiSop.getDisposisiEnd() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop() != null
				&& disposisiSop.getDisposisiEnd().getAlurSop().getPenolakanAdaDiSini()) {
			aktif = false;
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Mengisi status aktif secara manual.
	 *
	 * <p>Nilai ini bisa TERTIMPA oleh {@link #getAktif()} pada pemanggilan berikutnya bila
	 * disposisi SOP terkait ternyata nonaktif atau berakhir pada simpul penolakan -- lihat
	 * javadoc getter tersebut.</p>
	 *
	 * @param aktif nilai baru
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}
}
