package ais.database.model.sirs;

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

import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Konsumsi batch obat oleh penjualan apotik (FASE A) -- {@code sisa batch =
 * Kadaluarsa.qty - SUM(konsumsi.qty)}.
 *
 * <p>TABEL BARU (bukan mengurangi {@link Kadaluarsa#getQty()} langsung) DISENGAJA:
 * {@code sirs.kadaluarsa} existing bermakna "qty per batch SAAT DITERIMA" dan dipakai
 * MonitorKadaluarsaItemAction/dashboard -- mengubah maknanya menjadi "sisa" merusak data &amp;
 * laporan lama. Ledger konsumsi append-only ini membuat sisa selalu bisa dihitung ulang
 * dan tiap pengurangan tertaut ke baris penjualan yang menyebabkannya (auditable).</p>
 *
 * <h3>Mengapa buku besar, bukan pengurangan langsung</h3>
 *
 * <p>Pilihan antara "kurangi saldo" dan "catat setiap pergerakan" adalah keputusan
 * arsitektur yang menentukan apa yang masih bisa dijawab enam bulan kemudian.
 * Kalau {@link Kadaluarsa#getQty()} dikurangi langsung setiap kali obat terjual,
 * yang tersisa hanyalah satu angka: sisa hari ini. Pertanyaan "batch ini habis ke
 * mana" tidak punya jawaban, dan bila angkanya ternyata salah tidak ada cara
 * menemukan pengurangan mana yang keliru selain menebak. Untuk stok makanan
 * kantin itu mungkin dapat diterima; untuk obat — yang lotnya dapat ditarik dari
 * peredaran dan yang penelusurannya diminta pemeriksa — tidak.</p>
 *
 * <p>Dengan bentuk buku besar seperti di sini, setiap pengurangan adalah satu
 * baris yang menyebutkan berapa banyak, kapan, oleh siapa, dan — lewat
 * {@link #getTransaksiDetail()} — karena penjualan yang mana. Ketika satu lot
 * ditarik ({@link Kadaluarsa#LOT_RECALL}), daftar pasien yang menerima obat dari
 * lot itu dapat disusun dengan menelusuri baris-baris konsumsi ke transaksi
 * penjualannya. Itu tidak mungkin dilakukan dari sebuah angka saldo.</p>
 *
 * <h3>Sifat append-only dan bagaimana ia dijaga</h3>
 *
 * <p>Buku besar hanya bermakna bila barisnya tidak dicabut belakangan. Di sini
 * sifat itu ditegakkan dengan dua lapis, dan penting membedakan keduanya karena
 * kekuatannya tidak sama.</p>
 *
 * <p>Lapis pertama, yang sesungguhnya menahan: TIDAK ADA satu pun aksi yang
 * mengubah atau menghapus baris konsumsi. Dispatcher {@code ApotikApiDispatcher}
 * mengenal aksi untuk membuat penjualan, meracik, menerima barang, opname, retur,
 * dan sederet laporan — tidak ada {@code apotik_batch_konsumsi_ubah} maupun
 * {@code _hapus}. Baris hanya lahir di dalam transaksi penjualan
 * ({@code ApotikApiHelper.bayar}) dan produksi racikan
 * ({@code ApotikRacikanProduksiHelper}), lalu tidak pernah disentuh lagi.</p>
 *
 * <p>Lapis kedua, yang merekam tetapi tidak menahan: {@code @Audited}. Envers
 * menulis salinan tiap revisi ke {@code new_audit.apotik_batch_konsumsi__audit},
 * sehingga andaikan suatu saat jalur ubah/hapus ditambahkan, bentuk lama baris
 * masih dapat dibaca dari tabel audit. Yang perlu dipahami jujur: Envers
 * MENCATAT, ia tidak MENOLAK. Ia tidak menghalangi UPDATE atau DELETE, dan ia
 * tidak berlaku sama sekali bagi perubahan yang dikerjakan langsung lewat SQL ke
 * basis data. Jadi jaminan sesungguhnya berasal dari lapis pertama — ketiadaan
 * jalur — bukan dari anotasi.</p>
 *
 * <p><b>Akibatnya untuk perubahan berikutnya:</b> menambahkan aksi "koreksi
 * konsumsi" akan meruntuhkan asas entity ini secara diam-diam, karena rumus sisa
 * batch akan berubah surut tanpa ada yang tampak berbeda pada barisnya. Koreksi
 * yang benar berbentuk baris BARU — retur atau penyesuaian opname — bukan
 * penyuntingan baris lama.</p>
 *
 * <h3>Batas yang tidak dijaga entity</h3>
 *
 * <p>Entity ini tidak dapat memeriksa bahwa jumlah seluruh konsumsi sebuah batch
 * tidak melebihi {@link Kadaluarsa#getQty()} — satu baris tidak tahu apa-apa
 * tentang saudara-saudaranya. Penegakan itu ada di
 * {@code ApotikApiHelper.bayar}, yang menghitung sisa tiap batch di dalam
 * transaksi yang sama sebelum menulis konsumsinya dan menolak penjualan bila
 * kurang. Siapa pun yang menulis jalur pembuatan konsumsi BARU wajib menyalin
 * pemeriksaan itu; tanpa ia, stok batch dapat menjadi negatif dan tidak ada
 * apa pun di lapisan ini yang akan mengeluh.</p>
 *
 * @see Kadaluarsa batch/lot yang dikonsumsi, sekaligus pemegang status kelayakan lot
 * @see ApotikNarkotikaLog register terpisah untuk golongan obat terkendali
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "sirs", name = "apotik_batch_konsumsi")
public class ApotikBatchKonsumsi extends GeneralValueObject {

	/** Versi serialisasi; tetap 1 selama bentuk field tidak berubah maknanya. */
	private static final long serialVersionUID = 1L;

	/** Kunci baris; dibangkitkan basis data. */
	private Long id;

	/** Batch/lot yang dikurangi baris ini. Wajib. */
	private Kadaluarsa kadaluarsa;

	/** Baris penjualan penyebab pengurangan — penghubung ke pasien/transaksi. Wajib. */
	private TransaksiMedisDetail transaksiDetail;

	/** Banyaknya yang diambil dari batch, dalam satuan batch tersebut. */
	private Double qty;

	/** Waktu pengambilan dari batch. */
	private Date waktu;

	/** Nama tampil pelaku (bayangan audit). */
	private String oleh;

	/** Identitas akun pelaku (bayangan audit). */
	private String olehId;

	/**
	 * Kait JPA sebelum UPDATE: menyegarkan {@link #getTanggal_dirubah()}.
	 *
	 * <p>Untuk entity append-only ini kait tersebut sebenarnya tidak pernah
	 * berjalan pada pemakaian normal, karena baris tidak pernah di-UPDATE. Ia
	 * dipertahankan demi keseragaman: kalau suatu saat ada jalur yang mengubah
	 * baris — meski seharusnya tidak ada — stempel waktunya tetap ikut bergerak
	 * dan perubahan itu tidak lewat tanpa tanda.</p>
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * Konstruktor tanpa argumen yang dituntut JPA.
	 *
	 * <p>Objek yang dihasilkan belum sah untuk disimpan: {@link #getKadaluarsa()}
	 * dan {@link #getTransaksiDetail()} keduanya {@code nullable = false}, jadi
	 * pemanggil wajib mengisi keduanya sebelum {@code save}.</p>
	 */
	public ApotikBatchKonsumsi() {
	}

	/**
	 * Kunci baris, dibangkitkan basis data ({@code IDENTITY}).
	 *
	 * @return kunci baris, atau {@code null} bila belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menetapkan kunci baris; dipakai Hibernate, bukan kode aplikasi.
	 *
	 * @param id kunci baris
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Batch/lot yang dikurangi baris ini.
	 *
	 * <p>Getter ini bersifat DESTRUKTIF terhadap field: hasil {@code check(...)}
	 * ditulis balik ke {@code kadaluarsa} sebelum dikembalikan. {@code check}
	 * milik {@code GeneralValueObject} menormalkan proksi malas Hibernate yang
	 * sudah tidak terpakai menjadi {@code null} alih-alih membiarkannya meledak
	 * dengan {@code LazyInitializationException} ketika objek dibaca di luar
	 * sesi — misalnya saat diserialkan ke JSON. Pola ini seragam di seluruh
	 * paket dan bukan kekhususan entity ini.</p>
	 *
	 * <p>Yang perlu diketahui pembacanya: memanggil getter ini dapat MENGUBAH
	 * keadaan objek. Dua panggilan berturut-turut tidak dijamin mengembalikan
	 * hal yang sama bila di antaranya sesi ditutup. Jangan menganggapnya
	 * pembacaan murni, dan jangan memanggilnya di dalam kondisi yang
	 * mengasumsikan objek tidak bergerak.</p>
	 *
	 * <p>Relasi ini {@code nullable = false} — sebuah konsumsi yang tidak
	 * menunjuk batch tidak punya arti; rumus sisa batch tidak akan dapat
	 * menghitungnya, dan kuantitasnya akan hilang dari pembukuan lot mana pun.
	 * {@code CascadeType.PERSIST}/{@code MERGE} ada supaya batch yang baru
	 * dibuat dalam transaksi yang sama ikut tersimpan; TIDAK ada
	 * {@code REMOVE}, sehingga menghapus baris konsumsi tidak akan pernah ikut
	 * menghapus batchnya.</p>
	 *
	 * @return batch yang dikonsumsi, atau {@code null} bila proksinya sudah lepas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "kadaluarsa", nullable = false)
	public Kadaluarsa getKadaluarsa() {
		kadaluarsa = check(kadaluarsa);
		return kadaluarsa;
	}

	/**
	 * Menetapkan batch/lot yang dikonsumsi.
	 *
	 * @param kadaluarsa batch sumber; wajib terisi sebelum disimpan
	 */
	public void setKadaluarsa(Kadaluarsa kadaluarsa) {
		this.kadaluarsa = kadaluarsa;
	}

	/**
	 * Baris penjualan yang menyebabkan pengurangan ini.
	 *
	 * <p>Inilah yang membuat buku besar ini dapat ditelusuri. Dari sebuah baris
	 * konsumsi, {@code TransaksiMedisDetail} membawa ke transaksinya, dan dari
	 * transaksi ke pasien serta waktunya. Rantai itulah yang dipakai ketika satu
	 * lot harus ditarik dari peredaran dan pertanyaannya berubah menjadi "siapa
	 * saja yang sudah menerima obat dari lot ini".</p>
	 *
	 * <p>Berlaku catatan getter destruktif yang sama seperti
	 * {@link #getKadaluarsa()}.</p>
	 *
	 * <p>{@code nullable = false}: konsumsi tanpa sebab bukan konsumsi. Bila
	 * suatu saat perlu mencatat pengurangan yang bukan berasal dari penjualan —
	 * pemusnahan obat rusak, misalnya — jangan melonggarkan kolom ini menjadi
	 * boleh kosong, karena itu akan mendiamkan seluruh baris tak bersebab yang
	 * sudah ada maupun yang akan datang. Bentuk yang benar adalah menambahkan
	 * kolom jenis pergerakan yang tegas, sehingga baris tanpa penjualan tetap
	 * menyatakan alasannya.</p>
	 *
	 * @return baris penjualan penyebab, atau {@code null} bila proksinya lepas
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "transaksi_detail", nullable = false)
	public TransaksiMedisDetail getTransaksiDetail() {
		transaksiDetail = check(transaksiDetail);
		return transaksiDetail;
	}

	/**
	 * Menetapkan baris penjualan penyebab.
	 *
	 * @param transaksiDetail baris penjualan; wajib terisi sebelum disimpan
	 */
	public void setTransaksiDetail(TransaksiMedisDetail transaksiDetail) {
		this.transaksiDetail = transaksiDetail;
	}

	/**
	 * Banyaknya yang diambil dari batch, dalam satuan batch tersebut.
	 *
	 * <p>Mengembalikan {@code 0} bila field kosong, bukan {@code null}. Untuk
	 * kolom yang selalu dijumlahkan itu pilihan yang tepat: rumus sisa batch
	 * berbentuk {@code Kadaluarsa.qty - SUM(konsumsi.qty)}, dan satu {@code null}
	 * yang lolos ke penjumlahan Java akan melempar {@code NullPointerException}
	 * di tengah perhitungan stok, bukan sekadar menghasilkan angka yang salah.
	 * Nilai nol menyatakan hal yang benar secara semantik pula — baris tanpa
	 * kuantitas memang tidak mengurangi apa pun.</p>
	 *
	 * <p><b>Selalu positif menurut konvensi, tidak menurut penegakan.</b> Tidak
	 * ada pemeriksaan di sini maupun di basis data yang mencegah nilai negatif.
	 * Baris berkuantitas negatif akan MENAMBAH sisa batch ketika rumus dijalankan
	 * — pintu belakang untuk menggelembungkan stok tanpa dokumen penerimaan.
	 * Pemanggil yang ada tidak pernah menulis nilai negatif, tetapi jaminannya
	 * berhenti di situ. Jalur pembuatan baru wajib menolaknya sendiri.</p>
	 *
	 * @return kuantitas yang dikonsumsi; {@code 0} bila field kosong
	 */
	@Column(name = "qty")
	public Double getQty() {
		return qty == null ? Double.valueOf(0) : qty;
	}

	/**
	 * Menetapkan kuantitas yang dikonsumsi.
	 *
	 * <p>Menyimpan apa adanya, termasuk {@code null} dan nilai negatif;
	 * penyaringan adalah urusan pemanggil.</p>
	 *
	 * @param qty kuantitas
	 */
	public void setQty(Double qty) {
		this.qty = qty;
	}

	/**
	 * Waktu pengambilan dari batch.
	 *
	 * <p>Mengembalikan waktu SEKARANG bila field kosong, bukan {@code null}.
	 * Perilaku itu perlu dipahami dengan tepat: nilai pengganti hanya hidup di
	 * memori dan berubah setiap kali dipanggil — ia tidak ditulis balik ke field
	 * dan tidak akan tersimpan kecuali baris memang disimpan setelahnya.
	 * Akibatnya baris lama yang kolom {@code waktu}-nya kosong akan tampak
	 * "terjadi barusan" setiap kali dibaca, dan tampak berpindah setiap
	 * pembacaan berikutnya.</p>
	 *
	 * <p>Karena itu jangan pernah memakai getter ini untuk menyaring rentang
	 * tanggal di Java atas kumpulan baris yang mungkin memuat nilai kosong —
	 * hasilnya akan berubah-ubah tanpa sebab yang tampak. Penyaringan periode
	 * yang benar dikerjakan di basis data atas kolom {@code waktu} apa adanya,
	 * di mana kosong tetap kosong. Untuk baris yang ditulis pemanggil normal
	 * persoalan ini tidak muncul: {@code bayar} dan produksi racikan selalu
	 * mengisi waktu secara eksplisit.</p>
	 *
	 * @return waktu konsumsi, atau waktu sekarang bila field kosong
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "waktu")
	public Date getWaktu() {
		return waktu == null ? ais.ui.util.WaktuUtil.getDate() : waktu;
	}

	/**
	 * Menetapkan waktu konsumsi.
	 *
	 * @param waktu waktu pengambilan dari batch
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Nama tampil pelaku pencatatan (bayangan audit).
	 *
	 * @return nama pelaku, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menetapkan nama pelaku — MENGABAIKAN nilai kosong, tidak menimpanya.
	 *
	 * <p>Setter ini mengembalikan diri tanpa berbuat apa-apa bila diberi
	 * {@code null} atau teks yang hanya berisi spasi. Bentuk ini seragam di
	 * seluruh basis kode dan merupakan KEHARUSAN TEKNIS, bukan kelalaian yang
	 * menunggu diperbaiki.</p>
	 *
	 * <p>Kolom {@code oleh}/{@code oleh_id} adalah bayangan audit yang menempel
	 * pada barisnya sendiri — jawaban atas "siapa mencatat ini" yang tetap
	 * terbaca tanpa perlu membuka tabel Envers. Entity di basis kode ini
	 * melewati banyak jalur yang menyalin properti secara membabi buta:
	 * pengikatan formulir ZK, pemetaan dari JSON, penyalinan objek untuk
	 * disunting. Sebagian di antaranya memanggil setter dengan string kosong
	 * dari kolom yang tidak diisi. Kalau setter menurut, satu penyalinan lugu
	 * sudah cukup untuk mengganti nama pelaku yang benar dengan ruang kosong —
	 * dan tidak ada tempat lain di baris itu yang menyimpan nilai sebelumnya.
	 * Untuk buku besar stok obat, kehilangan itu berarti satu pengurangan stok
	 * kehilangan penanggung jawabnya secara permanen.</p>
	 *
	 * <p>Harga dari pilihan ini: nilai tidak dapat dikosongkan kembali lewat
	 * setter. Untuk kolom yang hanya boleh bertambah jelas, itu harga yang
	 * benar. Jangan mengubahnya menjadi penetapan lugas karena tampak seperti
	 * anomali — perubahan itu akan menghapus jejak pelaku di seluruh modul
	 * sekaligus, bukan hanya di entity ini.</p>
	 *
	 * @param oleh nama pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Identitas akun pelaku pencatatan (bayangan audit).
	 *
	 * @return id akun pelaku, atau {@code null}
	 */
	@Column(name = "oleh_id")
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menetapkan id akun pelaku — MENGABAIKAN nilai kosong.
	 *
	 * <p>Berlaku seluruh pertimbangan pada {@link #setOleh(String)}.</p>
	 *
	 * @param olehId id akun pelaku; diabaikan bila {@code null} atau kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Stempel perubahan terakhir.
	 *
	 * <p>Pada entity append-only ini nilainya praktis selalu sama dengan waktu
	 * pembuatan baris, karena tidak ada jalur yang mengubahnya.</p>
	 *
	 * @return waktu ubah terakhir
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menetapkan stempel perubahan terakhir.
	 *
	 * @param tanggal_dirubah waktu ubah
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}
}
