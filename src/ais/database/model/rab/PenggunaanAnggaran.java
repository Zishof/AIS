package ais.database.model.rab;
/* ENHANCED_PENGGUNAAN_ANGGARAN_MEMORY_SAFE_2026_06_03 - Java 1.6/1.7 compatible. */

import static javax.persistence.GenerationType.IDENTITY;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Calendar;
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

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.envers.Audited;
import org.json.JSONArray;
import org.json.JSONObject;

import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.akunting.Akun;
import ais.database.model.akunting.GrupTransaksi;
import ais.database.model.akunting.KasBesar;
import ais.database.model.akunting.KasKecil;
import ais.database.model.akunting.Pertangungjawaban;
import ais.database.model.akunting.Transaksi;
import ais.database.model.akunting.UangMuka;
import ais.database.model.asset.PermintaanPengadaanMasterAssetDetail;
import ais.database.model.asset.SaldoAwalMasterAssetDetail;
import ais.database.model.payroll.PembayaranGaji;
import ais.database.model.sop.DisposisiSop;

/**
 * Entitas <b>Penggunaan Anggaran</b> — baris realisasi/serapan anggaran pada
 * modul RAB, tabel {@code rab.penggunaan_anggaran}.
 *
 * <h3>Sifat dasar: tabel proyeksi, bukan tabel yang diisi manusia</h3>
 *
 * <p>Kelas ini bukan dokumen yang diketik pengguna. Ia adalah <b>proyeksi
 * (materialized view) yang ditulis kode</b>: setiap barisnya diturunkan dari
 * satu dokumen sumber di modul lain yang memakai anggaran, lalu dipakai modul
 * RAB untuk menghitung serapan per {@link Workspace} (baris pagu). Hal ini
 * ditegakkan secara eksplisit di lapisan generic CRUD v2 —
 * {@code PenggunaanAnggaranWorkflowGenericCrudAdapter} memaksa definisi CRUD
 * menjadi <i>native-only</i> (create/update/delete/import generik dimatikan
 * walau diaktifkan) dan mendeklarasikan {@code ref} sebagai satu-satunya kunci
 * alami. Invarian itu dijaga
 * {@code ais.action.master.generic.v2.test.PenggunaanAnggaranWorkflowSelfTest}.</p>
 *
 * <h3>Delapan sumber dokumen (union bertanda)</h3>
 *
 * <p>Sebuah baris menunjuk <b>tepat satu</b> dokumen sumber; tujuh relasi
 * lainnya {@code null}. Praktis seluruh getter di kelas ini berupa rantai
 * {@code if/else if} atas kedelapan kemungkinan itu, <b>dalam urutan prioritas
 * yang sama dan harus tetap konsisten</b>:</p>
 * <ol>
 *   <li>{@link GrupTransaksi} — jurnal umum ({@code _JURNAL});</li>
 *   <li>{@link UangMuka} — uang muka ({@code _UANG_MUKA});</li>
 *   <li>{@link PermintaanPengadaanMasterAssetDetail} — detail permintaan
 *       pengadaan/PR ({@code _PR});</li>
 *   <li>{@link SaldoAwalMasterAssetDetail} — pengadaan langsung/rutin
 *       ({@code _RUTIN});</li>
 *   <li>{@link PembayaranGaji} — penggajian ({@code _GAJI});</li>
 *   <li>{@link Pertangungjawaban} — LPJ uang muka ({@code _PJ}, bernilai
 *       <i>negatif</i>, lihat {@link #getNilai()});</li>
 *   <li>{@link KasKecil} — baris formula kas kecil
 *       ({@code <key>_KAS_KECIL_<id>});</li>
 *   <li>{@link KasBesar} — baris formula kas besar
 *       ({@code <key>_KAS_BESAR_<id>}).</li>
 * </ol>
 *
 * <h3>{@code ref} sebagai kunci alami dan sifat idempoten</h3>
 *
 * <p>{@link #getRef()} membentuk kunci alami {@code <id sumber>_<JENIS>} lewat
 * {@link #refData(PenggunaanAnggaran)}. Basis data memiliki constraint unik atas
 * kolom ini ({@code ref_penggunaan_anggaran}). Seluruh penulisan melewati
 * {@code saveOrUpdateByRef(...)} yang bersifat idempoten: kunci advisory
 * PostgreSQL atas {@code ref} (lihat {@link #kunciRef(String)}), pembersihan
 * baris kembar, pencarian baris yang ada berdasarkan {@code ref}, lalu
 * simpan/ubah/hapus. Berkat itu dokumen sumber boleh disimpan berkali-kali tanpa
 * menggandakan serapan.</p>
 *
 * <h3>Penulisan berjalan ASINKRON — dan konsekuensinya</h3>
 *
 * <p>{@link #simpan(Serializable)} — dipanggil dari
 * {@code ais.database.hibernate.AuditListener} untuk setiap penyimpanan entitas
 * yang relevan — <b>tidak</b> menulis di dalam transaksi pemanggil. Ia
 * menjadwalkan thread baru yang tidur 3 detik, membuka sesi Hibernate <i>native</i>
 * sendiri, lalu menulis. Implikasinya penting dan sering disalahpahami:</p>
 * <ul>
 *   <li>jalur ini <b>tidak dapat membatalkan</b> transaksi dokumen sumber. Ia
 *       secara arsitektural tidak berada pada posisi untuk menolak apa pun;</li>
 *   <li>kegagalan penulisan ditelan (dicatat lewat {@code ErrorAuditUtil}) dan
 *       tidak terlihat oleh pengguna yang menyimpan dokumen;</li>
 *   <li>ada jendela beberapa detik ketika dokumen sudah tersimpan tetapi
 *       serapannya belum tercatat — laporan RAB yang dibaca pada jendela itu
 *       menampilkan angka lama;</li>
 *   <li>karena berjalan di luar sesi ZK, getter yang bergantung pada konteks
 *       pengguna tidak dapat diandalkan di jalur ini.</li>
 * </ul>
 *
 * <h3>TIDAK ADA penjaga keseimbangan pagu-versus-realisasi</h3>
 *
 * <p>Tidak ada satu pun kode di kelas ini — maupun di jalur pemanggilnya — yang
 * menjumlahkan realisasi sebuah {@link Workspace} lalu menolak penambahan ketika
 * pagu terlampaui. {@code Workspace.getRealisasiTotal()} hanyalah kolom
 * penyimpan, dan {@code SumberDana.getPagu()} tidak pernah dibandingkan dengan
 * apa pun. Pelampauan pagu <b>dilaporkan setelah kejadian</b>
 * ({@code RealisasiBulananAction} menandai "Realisasi melebihi pagu anggaran" dan
 * menyediakan filter "Over Budget saja"), bukan dicegah. Gabungkan dengan sifat
 * asinkron di atas: pencegahan di titik ini memang tidak mungkin tanpa perubahan
 * rancangan. Jangan menulis kode baru yang mengandaikan entitas ini menjaga
 * pagu.</p>
 *
 * <h3>Penjaga yang benar-benar ada: pencegahan potong-anggaran ganda</h3>
 *
 * <p>Satu-satunya penjaga integritas nilai yang nyata di kelas ini menyangkut
 * uang muka berbasis PR. Bila sebuah {@link UangMuka} merujuk permintaan
 * pengadaan ({@code getPermintaanPengadaanMasterAssets()} tidak kosong), PR-nya
 * sudah memotong anggaran, sehingga uang muka tersebut tidak boleh memiliki baris
 * penggunaan sendiri. Penjaga ini bekerja rangkap tiga:
 * {@code createPenggunaanAnggaranSource(...)} menolak membuat baris,
 * {@link #getAktif()} memaksa {@code false} sehingga baris lama dihapus pada
 * penyimpanan berikutnya, dan {@link #prosesSimpan(Serializable, Session)}
 * menghapus baris uang muka yang terlanjur ada. Lihat masing-masing method.</p>
 *
 * <h3>Getter yang menghitung ulang dan menimpa field</h3>
 *
 * <p>Hampir semua getter berkolom di kelas ini ({@link #getKode()},
 * {@link #getNama()}, {@link #getKeterangan()}, {@link #getAktif()},
 * {@link #getNilai()}, {@link #getRef()}, {@link #getWorkspace()},
 * {@link #getWaktu()}, {@link #getDisposisiSop()}) <b>tidak</b> sekadar membaca
 * field: mereka menurunkan ulang nilainya dari dokumen sumber dan <i>menugaskan
 * kembali</i> ke field. Artinya kolom-kolom bersangkutan di basis data hanyalah
 * <b>cache</b> — sekadar membaca lalu menyimpan entitas ini sudah dapat mengubah
 * isinya mengikuti keadaan dokumen sumber terkini. Ini disengaja (proyeksi selalu
 * mengikuti sumber), tetapi berarti nilai historis tidak diawetkan dan pembacaan
 * bukan operasi bebas efek samping.</p>
 *
 * <h3>Jejak audit</h3>
 *
 * <p>Kelas ditandai {@link Audited} (Envers). Pasangan kolom audit bayangan
 * {@link #getOleh()}/{@link #getOlehId()}/{@link #getTanggal_dirubah()} di
 * sampingnya adalah keharusan teknis, bukan duplikasi keliru: grid ZK dan laporan
 * membaca nama pengubah dari baris terkini tanpa menyentuh tabel {@code _AUD}.</p>
 *
 * @see PenggunaanAnggaranLockSelfTest
 * @see Workspace
 * @see SumberDana
 */
@Entity
@org.hibernate.annotations.Entity(dynamicInsert = true, dynamicUpdate = true)
@Audited
@Table(schema = "rab", name = "penggunaan_anggaran")
public class PenggunaanAnggaran extends GeneralValueObject {

	/** Versi serialisasi Java untuk entitas ini. */
	private static final long serialVersionUID = 2463821577548439808L;
	/** Kunci primer basis data ({@code rab.penggunaan_anggaran.id}), IDENTITY. */
	private Long id;
	/** Nama tampil pengguna terakhir yang mengubah baris ini (audit bayangan). */
	private String oleh;
	/** Identitas pengguna terakhir yang mengubah baris ini (audit bayangan). */
	private String olehId;
	/** Stempel waktu perubahan terakhir; diperbarui oleh {@link #onUpdate()}. */
	private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/** Cache kode dokumen sumber; diturunkan ulang oleh {@link #getKode()}. */
	private String kode;
	/** Kunci alami baris ({@code <id sumber>_<JENIS>}); lihat {@link #getRef()}. */
	private String ref;
	/** Cache label baris; diturunkan ulang oleh {@link #getNama()}. */
	private String nama;
	/** Cache keterangan dokumen sumber; diturunkan ulang oleh {@link #getKeterangan()}. */
	private String keterangan;
	/** Cache status aktif turunan; lihat {@link #getAktif()}. */
	private Boolean aktif;
	/** Cache nilai serapan anggaran; lihat {@link #getNilai()}. */
	private Double nilai;
	/** Baris pagu/anggaran yang diserap baris ini; kolom wajib. */
	private Workspace workspace;
	/** Disposisi SOP dokumen sumber; lihat {@link #getDisposisiSop()}. */
	private DisposisiSop disposisiSop;
	/** Cache tanggal kejadian dokumen sumber; lihat {@link #getWaktu()}. */
	private Date waktu;

	/** Sumber: detail permintaan pengadaan (PR). */
	private PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail;
	/** Sumber: grup transaksi jurnal umum. */
	private GrupTransaksi grupTransaksi;
	/** Sumber: uang muka. */
	private UangMuka uangMuka;
	/** Sumber: detail saldo awal aset (pengadaan langsung/rutin). */
	private SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail;
	/** Sumber: pembayaran gaji. */
	private PembayaranGaji pembayaranGaji;
	/** Sumber: kas kecil (baris di dalam formula JSON-nya). */
	private KasKecil kasKecil;
	/** Sumber: kas besar (baris di dalam formula JSON-nya). */
	private KasBesar kasBesar;
	/** Sumber: pertanggungjawaban (LPJ) uang muka. */
	private Pertangungjawaban pertangungjawaban;

	/**
	 * Konstruktor tanpa argumen yang diwajibkan Hibernate/JPA, sekaligus dipakai
	 * pabrik internal {@code createPenggunaanAnggaranSource(...)} dan
	 * {@code saveOrUpdateByRef(...)} untuk membentuk baris baru.
	 */
	public PenggunaanAnggaran() {
	}

	/**
	 * Mengembalikan kunci primer baris penggunaan anggaran.
	 *
	 * <p>Kolom dipetakan {@code insertable = false}: nilainya sepenuhnya
	 * dibangkitkan basis data (IDENTITY) dan tidak pernah dikirim pada INSERT.</p>
	 *
	 * @return id baris, atau {@code null} bila entitas belum tersimpan
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/**
	 * Menyetel kunci primer baris.
	 *
	 * @param id kunci primer
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Mengembalikan identitas pengguna terakhir yang mengubah baris ini.
	 *
	 * @return identitas pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyetel identitas pengguna pengubah terakhir.
	 *
	 * <p><b>Setter penjaga:</b> nilai kosong diabaikan sehingga nilai lama bertahan.
	 * Untuk entitas ini penjaga tersebut sangat relevan karena hampir semua
	 * penulisannya berasal dari thread latar belakang {@link #simpan(Serializable)}
	 * yang tidak memiliki konteks pengguna — tanpa penjaga ini, jejak pengubah yang
	 * sah akan terhapus setiap kali proyeksi disegarkan.</p>
	 *
	 * @param olehId identitas pengubah; diabaikan bila kosong
	 */
	public void setOlehId(String olehId) {
		if (olehId == null || olehId.trim().isEmpty()) {
			return;
		}
		this.olehId = olehId;
	}

	/**
	 * Mengembalikan nama pengguna terakhir yang mengubah baris ini.
	 *
	 * @return nama pengubah, atau {@code null} bila belum pernah diisi
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Menyetel nama pengguna pengubah terakhir; nilai kosong diabaikan.
	 *
	 * <p>Lihat alasan penjaga pada {@link #setOlehId(String)}.</p>
	 *
	 * @param oleh nama pengubah; diabaikan bila kosong
	 */
	public void setOleh(String oleh) {
		if (oleh == null || oleh.trim().isEmpty()) {
			return;
		}
		this.oleh = oleh;
	}

	/**
	 * Callback JPA {@code @PreUpdate} yang memutakhirkan stempel waktu audit tepat
	 * sebelum baris ditulis ulang.
	 *
	 * <p>Mendelegasikan ke {@code AuditTimestampInterceptor.ubah(this)} agar seluruh
	 * entitas AIS memakai satu sumber kebenaran waktu. Tidak berjalan pada INSERT —
	 * nilai awal berasal dari inisialisasi field {@code tanggal_dirubah}.</p>
	 */
	@javax.persistence.PreUpdate
	protected void onUpdate() {
		ais.database.hibernate.AuditTimestampInterceptor.ubah(this);
	}

	/**
	 * Mengembalikan stempel waktu perubahan terakhir baris proyeksi ini.
	 *
	 * <p>Perhatikan bedanya dengan {@link #getWaktu()}: yang ini adalah waktu
	 * <i>baris proyeksi</i> disentuh, sedangkan {@code getWaktu()} adalah tanggal
	 * kejadian pada dokumen sumber.</p>
	 *
	 * @return waktu perubahan terakhir (presisi TIMESTAMP)
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * Menyetel stempel waktu perubahan terakhir.
	 *
	 * @param tanggal_dirubah stempel waktu perubahan
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * Representasi teks ringkas baris untuk keperluan log dan komponen ZK.
	 *
	 * <p>Membaca field {@code nama} <b>langsung</b>, bukan lewat {@link #getNama()},
	 * sehingga sengaja tidak memicu penurunan ulang dari dokumen sumber maupun
	 * pemuatan malas — aman dipanggil dari penangan kesalahan dan dari luar sesi
	 * Hibernate. Akibatnya nilai yang tampil bisa {@code null} pada entitas yang
	 * belum pernah getternya dipanggil.</p>
	 *
	 * @return gabungan {@code id + "-" + nama}
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/**
	 * Mengembalikan kode dokumen sumber baris ini, diturunkan ulang setiap kali
	 * dipanggil.
	 *
	 * <p><b>Getter yang menghitung ulang dan menimpa field.</b> Method ini menyusuri
	 * kedelapan kemungkinan dokumen sumber dalam urutan prioritas baku (jurnal, uang
	 * muka, PR, saldo awal/rutin, gaji, kas kecil, kas besar, LPJ) dan menugaskan
	 * kode dokumen yang ditemukan ke field {@code kode}. Kolom {@code kode} di basis
	 * data karena itu hanyalah cache: bila dokumen sumber berganti nomor, baris ini
	 * ikut berubah pada penyimpanan berikutnya, dan nilai historis tidak
	 * diawetkan.</p>
	 *
	 * <p><b>Perbedaan halus dari getter kerabatnya.</b> Berbeda dengan
	 * {@link #getNama()} dan {@link #getWorkspace()}, di sini cabang-cabangnya
	 * <i>tidak</i> mensyaratkan dokumen sumber memiliki {@code workspace}. Jadi
	 * sebuah baris yatim — sumbernya sudah kehilangan kaitan anggaran — tetap
	 * memperlihatkan kode, sementara nama dan nilainya bisa kosong. Perbedaan ini
	 * berguna saat menelusuri data: baris dengan kode terisi tetapi nilai kosong
	 * hampir selalu berarti dokumen sumbernya sudah tidak lagi terkait
	 * {@link Workspace}.</p>
	 *
	 * <p>Cabang {@link KasKecil} dan {@link KasBesar} mengambil kode dokumen kas
	 * secara utuh, bukan kode baris di dalam formula JSON-nya — untuk kas, pembeda
	 * antarbaris ada pada {@link #getRef()}, bukan pada kode.</p>
	 *
	 * <p>Nilai kembalian dinormalkan: {@code null} dan string kosong sama-sama
	 * menjadi {@code ""}, selain itu dipangkas spasi tepinya. Jadi getter ini tidak
	 * pernah mengembalikan {@code null} — jangan menulis pemeriksaan {@code null}
	 * terhadapnya, periksa {@code isEmpty()}.</p>
	 *
	 * @return kode dokumen sumber terpangkas, atau {@code ""} bila tidak ada
	 */
	public String getKode() {
		if (getGrupTransaksi() != null) {
			kode = getGrupTransaksi().getKode();
		} else if (getUangMuka() != null) {
			kode = getUangMuka().getKode();
		} else if (getPermintaanPengadaanMasterAssetDetail() != null) {
			kode = getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset().getKode();
		} else if (getSaldoAwalMasterAssetDetail() != null
				&& getSaldoAwalMasterAssetDetail().getSaldoAwal() != null) {
			kode = getSaldoAwalMasterAssetDetail().getSaldoAwal().getKode();
		} else if (getPembayaranGaji() != null) {
			kode = getPembayaranGaji().getKode();
		} else if (getKasKecil() != null) {
			kode = getKasKecil().getKode();
		} else if (getKasBesar() != null) {
			kode = getKasBesar().getKode();
		} else if (getPertangungjawaban() != null) {
			kode = getPertangungjawaban().getKode();
		}

		return kode == null || kode.isEmpty() ? "" : kode.trim();
	}

	/**
	 * Menyetel cache kode dokumen sumber.
	 *
	 * <p>Nyaris tak berguna dipanggil dari luar: {@link #getKode()} akan menurunkan
	 * ulang dan menimpa nilai ini begitu ada dokumen sumber yang menempel. Setter ini
	 * ada terutama agar Hibernate dapat mengisi field saat memuat baris dari basis
	 * data.</p>
	 *
	 * @param kode kode dokumen sumber
	 */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * Mengembalikan label baris penggunaan anggaran, diturunkan ulang dari dokumen
	 * sumber setiap kali dipanggil.
	 *
	 * <p><b>Getter yang menghitung ulang dan menimpa field.</b> Seperti
	 * {@link #getKode()}, method ini menugaskan hasilnya ke field {@code nama},
	 * sehingga kolom {@code nama} di basis data adalah cache, bukan data
	 * otoritatif — meski kolom itu dipetakan {@code nullable = false}.</p>
	 *
	 * <p><b>Format label per jenis sumber</b> (perhatikan awalan yang dipakai UI
	 * untuk mengenali asal baris): {@code "Jurnal " + kode} untuk
	 * {@link GrupTransaksi}; nama uang muka apa adanya untuk {@link UangMuka};
	 * {@code "PR " + kode} untuk permintaan pengadaan; {@code "Langsung " + kode}
	 * untuk saldo awal aset; {@code "GAJI " + kode} untuk penggajian; dan
	 * {@code "PJ " + kode} untuk pertanggungjawaban.</p>
	 *
	 * <p><b>Syarat {@code workspace} pada cabang-cabang tertentu.</b> Cabang jurnal,
	 * uang muka, gaji, dan LPJ hanya berlaku bila dokumen sumbernya masih terkait
	 * {@link Workspace} (untuk LPJ: lewat uang muka induknya). Bila kaitan itu
	 * hilang, tidak ada cabang yang cocok dan field {@code nama} dibiarkan sebagaimana
	 * adanya — nilai lama bertahan, bukan menjadi kosong. Ini menjelaskan baris
	 * "hantu" yang labelnya masih memperlihatkan dokumen lama padahal kaitannya sudah
	 * putus; pembersihannya ditangani {@link #getAktif()} yang membuat baris tersebut
	 * dihapus pada penyimpanan berikutnya.</p>
	 *
	 * <p><b>Cabang kas kecil/kas besar membaca formula JSON.</b> Untuk kedua jenis
	 * ini, satu dokumen kas memuat banyak baris anggaran di dalam kolom
	 * {@code formula} berformat {@code JSONArray}. Method menelusuri array itu,
	 * mencari elemen yang {@code key}-nya sama dengan {@link #getRef()} baris ini,
	 * lalu mengambil atribut {@code nama}-nya. Perhatikan tiga hal: (a) label
	 * di-reset ke {@code ""} lebih dulu, sehingga bila elemen tak ditemukan hasilnya
	 * benar-benar kosong — berbeda dari cabang non-kas yang mempertahankan nilai
	 * lama; (b) pencocokan dilakukan atas {@code ref} <i>utuh</i>, padahal ref kas
	 * yang dibentuk {@code saveKasLine(...)} berformat
	 * {@code <key>_KAS_KECIL_<idDokumen>} — pencocokan hanya cocok bila {@code ref}
	 * kebetulan berisi {@code key} saja, sehingga pada data yang ditulis alur normal
	 * cabang ini umumnya berakhir dengan label kosong; (c) penguraian JSON yang gagal
	 * ditelan dan dicatat lewat {@code ErrorAuditUtil}, sehingga formula rusak tidak
	 * menggagalkan tampilan, hanya mengosongkan label.</p>
	 *
	 * <p>Nilai kembalian dipangkas dan {@code null} dinormalkan menjadi {@code ""},
	 * jadi getter ini tidak pernah mengembalikan {@code null}.</p>
	 *
	 * @return label baris terpangkas, atau {@code ""} bila belum/tidak dapat
	 *         diturunkan
	 */
	@Column(name = "nama", nullable = false, length = 255)
	public String getNama() {
		if (getGrupTransaksi() != null && getGrupTransaksi().getWorkspace() != null) {
			nama = "Jurnal " + getGrupTransaksi().getKode();
		} else if (getUangMuka() != null && getUangMuka().getWorkspace() != null) {
			nama = getUangMuka().getNama();
		} else if (getPermintaanPengadaanMasterAssetDetail() != null
				&& getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset() != null) {
			nama = "PR " + getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset().getKode();
		} else if (getSaldoAwalMasterAssetDetail() != null && getSaldoAwalMasterAssetDetail().getSaldoAwal() != null) {
			nama = "Langsung " + getSaldoAwalMasterAssetDetail().getSaldoAwal().getKode();
		} else if (getPembayaranGaji() != null && getPembayaranGaji().getWorkspace() != null) {
			nama = "GAJI " + getPembayaranGaji().getKode();
		} else if (getPertangungjawaban() != null && getPertangungjawaban().getUangMuka() != null
				&& getPertangungjawaban().getUangMuka().getWorkspace() != null) {
			nama = "PJ " + getPertangungjawaban().getKode();
		}

		else if (getKasKecil() != null && getRef() != null && !getRef().trim().isEmpty()) {
			nama = "";
			try {
				JSONArray array = new JSONArray(getKasKecil().getFormula());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Long key = null;
					if (!jsonObject.isNull("key")) {
						key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
					}
					if (key != null && key.toString().equals(getRef())) {
						if (!jsonObject.isNull("nama")) {
							nama = jsonObject.get("nama") + "";
						}
						break;
					}
				}
				array = null;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:201");
			}
		}

		else if (getKasBesar() != null && getRef() != null && !getRef().trim().isEmpty()) {
			nama = "";
			try {
				JSONArray array = new JSONArray(getKasBesar().getFormula());
				for (int i = 0; i < array.length(); i++) {
					JSONObject jsonObject = array.getJSONObject(i);
					Long key = null;
					if (!jsonObject.isNull("key")) {
						key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
					}
					if (key != null && key.toString().equals(getRef())) {
						if (!jsonObject.isNull("nama")) {
							nama = jsonObject.get("nama") + "";
						}
						break;
					}
				}
				array = null;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:224");
			}
		}

		return this.nama == null ? "" : this.nama.trim();
	}

	/**
	 * Menyetel cache label baris.
	 *
	 * <p>Akan ditimpa {@link #getNama()} bila baris memiliki dokumen sumber yang
	 * masih terkait anggaran; terutama dipakai Hibernate saat memuat baris.</p>
	 *
	 * @param nama label baris
	 */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/**
	 * Mengembalikan keterangan dokumen sumber, diturunkan ulang setiap kali
	 * dipanggil.
	 *
	 * <p><b>Getter yang menghitung ulang dan menimpa field.</b> Menyusuri kedelapan
	 * dokumen sumber dalam urutan prioritas baku dan menugaskan keterangannya ke
	 * field {@code keterangan}, sehingga kolom bersangkutan berperan sebagai
	 * cache.</p>
	 *
	 * <p>Sama seperti {@link #getKode()} dan berbeda dari {@link #getNama()},
	 * cabang-cabangnya <i>tidak</i> mensyaratkan dokumen sumber masih terkait
	 * {@link Workspace}. Untuk kas kecil/kas besar diambil keterangan dokumen kas
	 * secara keseluruhan, bukan keterangan per baris formula.</p>
	 *
	 * <p><b>Berbeda dari getter kerabatnya, hasilnya TIDAK dinormalkan:</b> method
	 * ini mengembalikan field apa adanya, termasuk {@code null}. Pemanggil wajib
	 * memeriksa {@code null} — jangan menyalin pola {@code isEmpty()} dari
	 * {@link #getKode()}/{@link #getNama()} ke sini.</p>
	 *
	 * <p>Kolom dipetakan {@code columnDefinition = "text"} sehingga tidak berbatas
	 * 255 karakter seperti {@code nama}.</p>
	 *
	 * @return keterangan dokumen sumber, atau {@code null}
	 */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		if (getGrupTransaksi() != null) {
			keterangan = getGrupTransaksi().getKeterangan();
		} else if (getUangMuka() != null) {
			keterangan = getUangMuka().getKeterangan();
		} else if (getPermintaanPengadaanMasterAssetDetail() != null) {
			keterangan = getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset().getKeterangan();
		} else if (getSaldoAwalMasterAssetDetail() != null
				&& getSaldoAwalMasterAssetDetail().getSaldoAwal() != null) {
			keterangan = getSaldoAwalMasterAssetDetail().getSaldoAwal().getKeterangan();
		} else if (getPembayaranGaji() != null) {
			keterangan = getPembayaranGaji().getKeterangan();
		} else if (getKasKecil() != null) {
			keterangan = getKasKecil().getKeterangan();
		} else if (getKasBesar() != null) {
			keterangan = getKasBesar().getKeterangan();
		} else if (getPertangungjawaban() != null) {
			keterangan = getPertangungjawaban().getKeterangan();
		}
		return this.keterangan;
	}

	/**
	 * Menyetel cache keterangan baris.
	 *
	 * <p>Akan ditimpa {@link #getKeterangan()} bila ada dokumen sumber yang
	 * menempel.</p>
	 *
	 * @param keterangan keterangan baris
	 */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * Menentukan apakah baris penggunaan anggaran ini masih sah menyerap anggaran,
	 * dengan menurunkan status dari dokumen sumbernya.
	 *
	 * <h4>Mengapa method ini penting</h4>
	 *
	 * <p>Nilai kembalian method ini adalah <b>gerbang hidup-mati baris</b>, bukan
	 * sekadar penanda tampilan. Pada {@code saveOrUpdateByRef(...)}, baris yang
	 * {@code getAktif()}-nya {@code false} tidak disimpan melainkan
	 * <b>dihapus</b> lewat {@code Common.refreshDelete(...)}; hanya baris yang aktif
	 * yang disimpan/diperbarui. Dengan kata lain, setiap kali dokumen sumber disimpan
	 * ulang, method inilah yang memutuskan apakah serapan anggarannya tetap tercatat
	 * atau ditarik kembali. Kekeliruan di sini langsung berubah menjadi kesalahan
	 * angka realisasi RAB.</p>
	 *
	 * <h4>Aturan per jenis sumber</h4>
	 *
	 * <ul>
	 *   <li><b>{@link UangMuka}</b> — aktif bila uang muka aktif <i>dan</i> statusnya
	 *       bukan {@code UangMuka.DITOLAK}. Ini satu-satunya cabang yang memeriksa
	 *       status persetujuan, bukan hanya flag aktif.</li>
	 *   <li><b>{@link PermintaanPengadaanMasterAssetDetail}</b> — mengikuti flag aktif
	 *       dokumen PR induk (bukan detailnya).</li>
	 *   <li><b>{@link SaldoAwalMasterAssetDetail}</b> — mengikuti flag aktif dokumen
	 *       saldo awal induk.</li>
	 *   <li><b>{@link PembayaranGaji}</b> dan <b>{@link KasKecil}</b> — mengikuti flag
	 *       aktif dokumennya.</li>
	 *   <li><b>{@link GrupTransaksi}</b> — <i>tidak</i> memeriksa flag aktif jurnal,
	 *       melainkan memeriksa bahwa {@link Workspace} jurnal sama dengan
	 *       {@code workspace} baris ini. Cabang ini menjaga agar baris proyeksi ikut
	 *       mati bila jurnal dipindahkan ke anggaran lain — pemindahan semacam itu
	 *       menghasilkan baris baru pada anggaran tujuan, sementara baris lama harus
	 *       lenyap.</li>
	 *   <li><b>{@link Pertangungjawaban}</b> — mengikuti flag aktif LPJ.</li>
	 *   <li><b>{@link KasBesar}</b> — <i>tidak memiliki cabang</i>. Baris bersumber kas
	 *       besar karena itu jatuh ke nilai bawaan {@code true} di akhir method
	 *       (kecuali sisa nilai lama pada field). Konsekuensinya, menonaktifkan
	 *       dokumen kas besar tidak dengan sendirinya mematikan baris serapannya;
	 *       pembersihannya bergantung pada {@code prosesKasBesar(...)} yang menghapus
	 *       seluruh baris milik dokumen tersebut lalu menulis ulang dari formula.</li>
	 * </ul>
	 *
	 * <h4>Penjaga potong-anggaran ganda untuk uang muka berbasis PR</h4>
	 *
	 * <p>Setelah rantai di atas, ada satu aturan penimpa: bila sumbernya
	 * {@link UangMuka} dan uang muka itu merujuk permintaan pengadaan
	 * ({@code getPermintaanPengadaanMasterAssets()} tidak kosong), {@code aktif}
	 * dipaksa {@code false}. Alasannya, PR-nya sendiri sudah memotong anggaran; bila
	 * uang mukanya ikut memotong, anggaran terserap <b>dua kali</b> untuk satu
	 * pengeluaran. Aturan ini adalah lapis kedua dari tiga lapis penjaga yang sama —
	 * lapis pertama {@code createPenggunaanAnggaranSource(...)} yang menolak membuat
	 * baris, lapis ketiga {@link #prosesSimpan(Serializable, Session)} yang menghapus
	 * baris yang terlanjur ada. Lapis kedua inilah yang menyembuhkan data lama:
	 * baris uang muka yang dibuat sebelum PR ditambahkan akan menjadi tidak aktif dan
	 * karenanya dihapus pada penyimpanan berikutnya.</p>
	 *
	 * <h4>Perbaikan pemuatan malas yang harus dipertahankan</h4>
	 *
	 * <p>Komentar ROOT CAUSE FIX di dalam badan method mencatat perbaikan yang sudah
	 * diterapkan: sebelumnya {@code getPermintaanPengadaanMasterAssetDetail()} dan
	 * {@code getPermintaanPengadaanMasterAsset()} dipanggil berulang (tiga kali) di
	 * dua ekspresi terpisah. Keduanya memuat malas lewat {@code check(...)};
	 * pemanggilan berulang dari thread latar belakang {@link #simpan(Serializable)}
	 * dapat memberi hasil tak konsisten antarpanggilan dan memicu
	 * {@code NullPointerException} turunan hingga ke pemanggilan
	 * {@code getDisetujuiOleh()}/{@code getAktif()} pada dokumen PR. Perbaikannya:
	 * ambil sekali ke variabel lokal {@code ppmad}/{@code ppma} lalu pakai variabel
	 * itu. Jangan mengembalikan pola pemanggilan berulang saat menyunting method
	 * ini.</p>
	 *
	 * <h4>Fail-open dan normalisasi</h4>
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch} yang menelan kesalahan
	 * (dicatat lewat {@code ErrorAuditUtil}), dan nilai kembalian menormalkan
	 * {@code null} menjadi {@code true}. Gabungan keduanya bersifat
	 * <b>fail-open</b>: bila dokumen sumber gagal dimuat — misalnya karena
	 * {@code LazyInitializationException} di luar sesi — baris dianggap AKTIF dan
	 * serapan anggaran dipertahankan. Pilihan ini lebih aman daripada kebalikannya
	 * (kesalahan sesaat akan menghapus baris serapan yang sah dan membuat laporan
	 * realisasi mendadak turun), tetapi berarti baris yatim bisa bertahan selama
	 * dokumen sumbernya tak dapat dibaca.</p>
	 *
	 * @return {@code true} bila baris masih sah menyerap anggaran; {@code false} bila
	 *         harus ditarik/dihapus
	 */
	public Boolean getAktif() {
		try {
			// ROOT CAUSE FIX: sebelumnya getPermintaanPengadaanMasterAssetDetail() dan
			// .getPermintaanPengadaanMasterAsset() dipanggil ulang (3x) di dua ekspresi
			// terpisah. Keduanya lazy-load via check(); pemanggilan berulang dari thread
			// background (PenggunaanAnggaran$1) bisa memberi hasil tak konsisten dan
			// memicu NPE turunan sampai ke PermintaanPengadaanMasterAsset.getDisetujuiOleh/
			// getAktif. Ambil sekali ke variabel lokal.
			ais.database.model.asset.PermintaanPengadaanMasterAssetDetail ppmad = getPermintaanPengadaanMasterAssetDetail();
			ais.database.model.asset.PermintaanPengadaanMasterAsset ppma = ppmad == null ? null
					: ppmad.getPermintaanPengadaanMasterAsset();

			if (getUangMuka() != null) {
				aktif = getUangMuka().getAktif() && !getUangMuka().getStatus().equals(UangMuka.DITOLAK);
			} else if (ppmad != null && ppma != null) {
				aktif = ppma.getAktif();
			} else if (getSaldoAwalMasterAssetDetail() != null
					&& getSaldoAwalMasterAssetDetail().getSaldoAwal() != null) {
				aktif = getSaldoAwalMasterAssetDetail().getSaldoAwal().getAktif();
			} else if (getPembayaranGaji() != null) {
				aktif = getPembayaranGaji().getAktif();
			} else if (getKasKecil() != null) {
				aktif = getKasKecil().getAktif();
			} else if (getGrupTransaksi() != null && getWorkspace() != null) {
				aktif = getGrupTransaksi().getWorkspace() != null
						&& getGrupTransaksi().getWorkspace().getId().equals(getWorkspace().getId());
			} else if (getPertangungjawaban() != null) {
				aktif = getPertangungjawaban().getAktif();
			}

			if (getUangMuka() != null && !getUangMuka().getPermintaanPengadaanMasterAssets().trim().isEmpty()) {
				aktif = false;
			}

		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:287");
			// TODO: handle exception
		}
		return aktif == null ? true : aktif;
	}

	/**
	 * Menyetel cache status aktif baris.
	 *
	 * <p>Praktis selalu ditimpa {@link #getAktif()} pada pemanggilan berikutnya;
	 * terutama dipakai Hibernate saat memuat baris dari basis data.</p>
	 *
	 * @param aktif status aktif
	 */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

	/**
	 * Mengembalikan nilai rupiah serapan anggaran baris ini, diturunkan ulang dari
	 * dokumen sumber.
	 *
	 * <h4>Peran</h4>
	 *
	 * <p>Inilah angka yang dijumlahkan modul RAB untuk menghitung realisasi per
	 * {@link Workspace}. Sama seperti getter kerabatnya, method ini
	 * <b>menghitung ulang lalu menimpa field</b> {@code nilai}, sehingga kolom
	 * {@code nilai} di basis data adalah cache dari keadaan dokumen sumber terkini,
	 * bukan angka historis yang dibekukan pada saat transaksi terjadi.</p>
	 *
	 * <h4>Sumber angka per jenis dokumen</h4>
	 *
	 * <ul>
	 *   <li><b>{@link GrupTransaksi}</b> — {@code getTotalDebet()} jurnal;</li>
	 *   <li><b>{@link UangMuka}</b> — {@code getNilai()} uang muka;</li>
	 *   <li><b>{@link PermintaanPengadaanMasterAssetDetail}</b> —
	 *       {@code getHargaTotal()} detail PR (per baris detail, bukan total
	 *       dokumen);</li>
	 *   <li><b>{@link SaldoAwalMasterAssetDetail}</b> — {@code getHargaTotal()}
	 *       detail saldo awal;</li>
	 *   <li><b>{@link PembayaranGaji}</b> — {@code getNilai()} pembayaran gaji;</li>
	 *   <li><b>{@link Pertangungjawaban}</b> —
	 *       <b>{@code -getDikembalikan()}</b>, yaitu <i>negatif</i> dari nilai yang
	 *       dikembalikan. Ini disengaja: LPJ mengembalikan sisa uang muka, sehingga
	 *       barisnya berperan sebagai koreksi pengurang atas serapan uang muka yang
	 *       sudah tercatat lebih dulu. Jangan "memperbaiki" tanda ini — tanpa tanda
	 *       negatif, pengembalian justru akan menambah serapan;</li>
	 *   <li><b>{@link KasKecil}/{@link KasBesar}</b> — atribut {@code jumlah} pada
	 *       elemen formula JSON yang {@code key}-nya cocok dengan {@link #getRef()}.</li>
	 * </ul>
	 *
	 * <h4>Perbedaan syarat antarcabang</h4>
	 *
	 * <p>Cabang jurnal, uang muka, saldo awal, gaji, dan LPJ hanya berlaku bila
	 * dokumen sumbernya masih terkait {@link Workspace}; cabang PR tidak
	 * mensyaratkannya. Bila tak ada cabang yang cocok, field {@code nilai}
	 * dipertahankan apa adanya — nilai lama bertahan, tidak menjadi nol. Baris
	 * semacam itu biasanya sudah tidak aktif menurut {@link #getAktif()} dan akan
	 * terhapus pada penyimpanan berikutnya.</p>
	 *
	 * <h4>Cabang kas: hanya menghitung sekali</h4>
	 *
	 * <p>Cabang kas kecil dan kas besar dijaga syarat tambahan {@code nilai == null},
	 * sehingga penurunan dari formula JSON hanya terjadi ketika field masih kosong.
	 * Itu menjaga nilai yang sudah ditetapkan {@code saveKasLine(...)} — yang membaca
	 * {@code jumlah} langsung dari formula saat menulis baris — agar tidak
	 * dihitung ulang oleh pembacaan berikutnya (yang, karena ref kas berformat
	 * {@code <key>_KAS_KECIL_<id>} sedangkan pencocokan dilakukan atas ref utuh,
	 * umumnya tidak menemukan elemen dan akan menghasilkan 0,0). Perhatikan bahwa
	 * begitu masuk cabang ini {@code nilai} lebih dulu diset {@code 0.0}, sehingga
	 * formula yang gagal diurai menghasilkan nol, bukan {@code null}.</p>
	 *
	 * <h4>Fail-open dan tipe data</h4>
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch} yang menelan kesalahan
	 * (dicatat lewat {@code ErrorAuditUtil}), sehingga kegagalan pemuatan dokumen
	 * sumber menyisakan nilai lama alih-alih menggagalkan pembacaan. Nilai
	 * kembaliannya {@code Double} dan <b>boleh {@code null}</b> — pemanggil yang
	 * menjumlahkan wajib menanganinya. Perhatikan pula bahwa uang direpresentasikan
	 * sebagai bilangan pecahan biner {@code double}, bukan
	 * {@link java.math.BigDecimal}, sehingga penjumlahan besar dapat menyisakan
	 * galat pembulatan kecil.</p>
	 *
	 * <p><b>Tidak ada pemeriksaan pagu di sini.</b> Method ini melaporkan berapa yang
	 * terserap, bukan apakah serapan itu boleh terjadi; tidak ada pembandingan
	 * terhadap pagu {@link Workspace} maupun {@link SumberDana} di mana pun pada
	 * jalur ini.</p>
	 *
	 * @return nilai serapan (negatif untuk pengembalian LPJ), atau {@code null} bila
	 *         belum dapat diturunkan
	 */
	public Double getNilai() {

		try {
			if (getGrupTransaksi() != null && getGrupTransaksi().getWorkspace() != null) {
				nilai = getGrupTransaksi().getTotalDebet();
			} else if (getUangMuka() != null && getUangMuka().getWorkspace() != null) {
				nilai = getUangMuka().getNilai();
			} else if (getPermintaanPengadaanMasterAssetDetail() != null) {
				nilai = getPermintaanPengadaanMasterAssetDetail().getHargaTotal();
			} else if (getSaldoAwalMasterAssetDetail() != null
					&& getSaldoAwalMasterAssetDetail().getWorkspace() != null) {
				nilai = getSaldoAwalMasterAssetDetail().getHargaTotal();
			} else if (getPembayaranGaji() != null && getPembayaranGaji().getWorkspace() != null) {
				nilai = getPembayaranGaji().getNilai();
			} else if (getPertangungjawaban() != null && getPertangungjawaban().getUangMuka() != null
					&& getPertangungjawaban().getUangMuka().getWorkspace() != null) {
				nilai = -getPertangungjawaban().getDikembalikan();
			}

			else if (getKasKecil() != null && getRef() != null && !getRef().trim().isEmpty() && nilai == null) {
				nilai = 0.0;
				try {
					JSONArray array = new JSONArray(getKasKecil().getFormula());
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonObject = array.getJSONObject(i);
						Long key = null;
						if (!jsonObject.isNull("key")) {
							key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
						}
						if (key != null && key.toString().equals(getRef())) {
							if (!jsonObject.isNull("jumlah")) {
								nilai = jsonObject.getDouble("jumlah");
							}
							break;
						}
					}
					array = null;
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:335");
				}
			}

			else if (getKasBesar() != null && getRef() != null && !getRef().trim().isEmpty() && nilai == null) {
				nilai = 0.0;
				try {
					JSONArray array = new JSONArray(getKasBesar().getFormula());
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonObject = array.getJSONObject(i);
						Long key = null;
						if (!jsonObject.isNull("key")) {
							key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
						}
						if (key != null && key.toString().equals(getRef())) {
							if (!jsonObject.isNull("jumlah")) {
								nilai = jsonObject.getDouble("jumlah");
							}
							break;
						}
					}
					array = null;
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:358");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:361");
			// TODO: handle exception
		}

		return nilai;
	}

	/**
	 * Menyetel cache nilai serapan baris.
	 *
	 * <p>Dipakai secara berarti oleh {@code saveKasLine(...)} (yang menetapkan nilai
	 * baris kas dari formula) dan oleh {@code copySource(...)}; untuk jenis sumber
	 * lain nilainya akan ditimpa {@link #getNilai()}. Tidak ada validasi tanda
	 * maupun batas.</p>
	 *
	 * @param nilai nilai serapan
	 */
	public void setNilai(Double nilai) {
		this.nilai = nilai;
	}

	/**
	 * Membentuk kunci alami ({@code ref}) sebuah baris penggunaan anggaran dari
	 * dokumen sumber yang menempel padanya.
	 *
	 * <h4>Format dan peran</h4>
	 *
	 * <p>Hasilnya berbentuk {@code <id dokumen sumber>_<JENIS>}, dengan akhiran
	 * {@code _JURNAL}, {@code _UANG_MUKA}, {@code _PR}, {@code _RUTIN},
	 * {@code _GAJI}, atau {@code _PJ}. Nilai inilah yang menjadi kunci alami tunggal
	 * entitas: ia dijamin unik oleh constraint basis data
	 * {@code ref_penggunaan_anggaran}, dipakai {@code saveOrUpdateByRef(...)} untuk
	 * menemukan kembali baris yang sudah ada (sehingga penyimpanan bersifat
	 * idempoten), dan dideklarasikan sebagai satu-satunya {@code naturalKey} oleh
	 * {@code PenggunaanAnggaranWorkflowGenericCrudAdapter}.</p>
	 *
	 * <p>Karena id dokumen sumber dan akhiran jenisnya digabung, ref dari dua jenis
	 * dokumen berbeda tidak mungkin bertabrakan meski id-nya kebetulan sama —
	 * misalnya {@code 17_PR} dan {@code 17_GAJI}.</p>
	 *
	 * <h4>Syarat keterkaitan anggaran</h4>
	 *
	 * <p>Kecuali cabang PR, setiap cabang mensyaratkan dokumen sumber masih terkait
	 * {@link Workspace} (untuk LPJ: lewat uang muka induknya). Bila syarat itu tak
	 * terpenuhi, method mengembalikan {@code null}, dan pemanggil —
	 * {@link #prosesSimpan(Serializable, Session)} — akan berhenti tanpa menulis apa
	 * pun. Pada {@code saveOrUpdateByRef(...)}, ref yang berubah menjadi {@code null}
	 * untuk baris yang sudah tersimpan menjadi salah satu pemicu penghapusan baris.
	 * Jadi "kehilangan ref" adalah mekanisme resmi untuk menarik serapan yang sudah
	 * tidak sah.</p>
	 *
	 * <h4>Kekhususan kas kecil dan kas besar</h4>
	 *
	 * <p>Untuk {@link KasKecil} dan {@link KasBesar} method ini sengaja <b>tidak</b>
	 * membentuk apa pun dan membiarkan {@code ref} bernilai {@code null} (cabang
	 * kosong dengan komentar penjelas). Alasannya, satu dokumen kas memuat banyak
	 * baris anggaran di dalam formula JSON-nya, sehingga id dokumen saja tidak cukup
	 * membedakan baris. Ref untuk kedua jenis itu dibentuk di tempat lain, yaitu
	 * {@code prosesKasKecil(...)}/{@code prosesKasBesar(...)} yang menyusun
	 * {@code <key>_KAS_KECIL_<id>} dan {@code <key>_KAS_BESAR_<id>} lalu
	 * mengirimkannya ke {@code saveKasLine(...)}. Konsekuensi penting: karena
	 * {@link #getRef()} hanya menimpa field bila method ini mengembalikan nilai tak
	 * kosong, ref baris kas yang sudah tersimpan tidak akan tergerus menjadi
	 * {@code null} saat dibaca.</p>
	 *
	 * <h4>Sifat statis</h4>
	 *
	 * <p>Method ini {@code static} dan menerima instansnya sebagai parameter agar
	 * dapat dipakai baik oleh instans ({@link #ambilRef()}) maupun oleh alur
	 * penyimpanan yang bekerja atas objek sementara yang belum ter-persist.
	 * Method ini hanya membaca; ia tidak menugaskan apa pun ke {@code penggunaanAnggaran}.</p>
	 *
	 * @param penggunaanAnggaran baris yang hendak dihitung kunci alaminya; tidak boleh
	 *                           {@code null}
	 * @return kunci alami baris, atau {@code null} bila tidak dapat dibentuk (sumber
	 *         kas, sumber tidak dikenal, atau kaitan {@link Workspace} hilang)
	 */
	public static String refData(PenggunaanAnggaran penggunaanAnggaran) {
		String ref = null;
		if (penggunaanAnggaran.getGrupTransaksi() != null
				&& penggunaanAnggaran.getGrupTransaksi().getWorkspace() != null) {
			ref = penggunaanAnggaran.getGrupTransaksi().getId() + "_JURNAL";
		} else if (penggunaanAnggaran.getUangMuka() != null
				&& penggunaanAnggaran.getUangMuka().getWorkspace() != null) {
			ref = penggunaanAnggaran.getUangMuka().getId() + "_UANG_MUKA";
		} else if (penggunaanAnggaran.getPermintaanPengadaanMasterAssetDetail() != null) {
			ref = penggunaanAnggaran.getPermintaanPengadaanMasterAssetDetail().getId() + "_PR";
		} else if (penggunaanAnggaran.getSaldoAwalMasterAssetDetail() != null
				&& penggunaanAnggaran.getSaldoAwalMasterAssetDetail().getWorkspace() != null) {
			ref = penggunaanAnggaran.getSaldoAwalMasterAssetDetail().getId() + "_RUTIN";
		} else if (penggunaanAnggaran.getPembayaranGaji() != null
				&& penggunaanAnggaran.getPembayaranGaji().getWorkspace() != null) {
			ref = penggunaanAnggaran.getPembayaranGaji().getId() + "_GAJI";
		} else if (penggunaanAnggaran.getPertangungjawaban() != null
				&& penggunaanAnggaran.getPertangungjawaban().getUangMuka() != null
				&& penggunaanAnggaran.getPertangungjawaban().getUangMuka().getWorkspace() != null) {
			ref = penggunaanAnggaran.getPertangungjawaban().getId() + "_PJ";
		} else if (penggunaanAnggaran.getKasKecil() != null || penggunaanAnggaran.getKasBesar() != null) {
			// ref sudah diambil dari metode spesifik di bawah getRef()
		} else {
			ref = null;
		}

		return ref;
	}

	/**
	 * Membentuk kunci alami baris ini, versi instans dari
	 * {@link #refData(PenggunaanAnggaran)}.
	 *
	 * <p>Perhatikan bahwa ini <b>bukan</b> getter properti Hibernate — namanya
	 * sengaja tidak berawalan {@code get} agar tidak dipetakan sebagai kolom.
	 * Bedanya dengan {@link #getRef()}: method ini selalu menghitung ulang dan tidak
	 * pernah mengembalikan ref yang tersimpan, sehingga {@code null} darinya berarti
	 * "baris ini <i>sekarang</i> tidak berhak punya ref". Sifat itulah yang dipakai
	 * {@code saveOrUpdateByRef(...)} sebagai salah satu syarat penghapusan baris.</p>
	 *
	 * @return kunci alami hasil hitung ulang, atau {@code null} bila tak dapat
	 *         dibentuk
	 */
	public String ambilRef() {
		return refData(this);
	}

	/**
	 * Mengembalikan kunci alami baris, memperbarui field bila dapat dihitung ulang.
	 *
	 * <p><b>Perilaku gabungan hitung-ulang dan pertahankan.</b> Method memanggil
	 * {@link #ambilRef()} lebih dulu; bila hasilnya tidak kosong, hasil itu
	 * ditugaskan ke field {@code ref} (sudah dipangkas). Bila hasilnya kosong —
	 * termasuk untuk sumber {@link KasKecil}/{@link KasBesar} yang memang tidak
	 * dilayani {@link #refData(PenggunaanAnggaran)} — nilai ref yang sudah tersimpan
	 * <b>dipertahankan</b>. Kombinasi ini penting: ia membuat ref baris kas (yang
	 * dibentuk di jalur {@code prosesKasKecil}/{@code prosesKasBesar}) tetap utuh,
	 * sekaligus membuat ref baris non-kas selalu selaras dengan dokumen sumbernya.</p>
	 *
	 * <p>Konsekuensi lain yang perlu disadari: karena getter ini menulis ke field,
	 * membaca ref pada entitas terkelola dapat mengubahnya dan karenanya bukan
	 * operasi bebas efek samping.</p>
	 *
	 * <p>Nilai kembalian dinormalkan: string kosong/spasi menjadi {@code null},
	 * selain itu dipangkas. Jadi pemanggil cukup memeriksa {@code null} dan tidak
	 * perlu memeriksa string kosong.</p>
	 *
	 * <p>Kolom dipetakan {@code nullable = true} pada tingkat JPA, namun di basis
	 * data terdapat constraint unik {@code ref_penggunaan_anggaran} atas kolom ini —
	 * pelanggarannya ditangani {@code saveOrUpdateByRef(...)} lewat deteksi
	 * {@code isDuplicateRefException(...)} dan percobaan ulang sekali.</p>
	 *
	 * @return kunci alami baris, atau {@code null} bila tidak ada
	 */
	@Column(nullable = true)
	public String getRef() {
		String calculatedRef = ambilRef();
		if (calculatedRef != null && !calculatedRef.trim().isEmpty()) {
			ref = calculatedRef.trim();
		}
		return ref == null || ref.trim().isEmpty() ? null : ref.trim();
	}

	/**
	 * Menyetel kunci alami baris, dengan normalisasi.
	 *
	 * <p>Berbeda dari kebanyakan setter di kelas ini, setter ini
	 * <b>menormalkan</b>: {@code null} maupun string kosong/spasi sama-sama disimpan
	 * sebagai {@code null}, selain itu dipangkas spasi tepinya. Normalisasi ini
	 * penting karena {@code ref} adalah kunci alami yang dibandingkan persis di
	 * SQL — string kosong dan {@code null} tidak boleh menjadi dua nilai berbeda,
	 * dan spasi tepi tidak boleh membuat dua ref yang sama tampak berbeda.</p>
	 *
	 * <p>Dipanggil dari {@code saveKasLine(...)}, {@code copySource(...)},
	 * {@code saveOrUpdateByRef(...)}, dan {@link #prosesSimpan(Serializable, Session)}.</p>
	 *
	 * @param ref kunci alami; kosong akan disimpan sebagai {@code null}
	 */
	public void setRef(String ref) {
		this.ref = ref == null || ref.trim().isEmpty() ? null : ref.trim();
	}

	/**
	 * Mengembalikan baris pagu ({@link Workspace}) yang diserap baris penggunaan ini,
	 * diturunkan ulang dari dokumen sumber, dengan pengalihan ke anggaran pendahulu
	 * bila ada.
	 *
	 * <h4>Peran</h4>
	 *
	 * <p>Inilah dimensi terpenting entitas ini: {@link Workspace} adalah baris
	 * anggaran/pagu, dan seluruh perhitungan serapan RAB mengelompokkan
	 * {@link #getNilai()} berdasarkan hasil method ini. Kolomnya dipetakan
	 * {@code nullable = false} — sebuah baris penggunaan tanpa baris pagu tidak
	 * bermakna.</p>
	 *
	 * <h4>Tiga tahap</h4>
	 *
	 * <p><b>(1) Bangkitkan ulang proxy.</b> Relasi dipetakan
	 * {@code fetch = FetchType.LAZY}, sehingga nilai yang ada lebih dulu dilewatkan
	 * ke {@code check(...)} milik {@link GeneralValueObject} agar proxy yang sudah
	 * terlepas dari sesi Hibernate dapat dipakai — penting karena entitas ini kerap
	 * dibaca dari thread latar belakang {@link #simpan(Serializable)} dan dari
	 * laporan di luar sesi ZK.</p>
	 *
	 * <p><b>(2) Turunkan ulang dari dokumen sumber.</b> Sama seperti getter kerabatnya,
	 * method ini menyusuri kedelapan kemungkinan dokumen sumber dalam urutan
	 * prioritas baku dan <b>menimpa</b> field {@code workspace} dengan anggaran milik
	 * dokumen sumber terkini (untuk LPJ: anggaran uang muka induknya). Artinya
	 * memindahkan sebuah jurnal atau uang muka ke anggaran lain otomatis memindahkan
	 * serapannya pada pembacaan berikutnya. Untuk {@link KasKecil}/{@link KasBesar},
	 * anggaran dicari di dalam formula JSON dokumen kas: elemen yang {@code key}-nya
	 * cocok dengan {@link #getRef()} dibaca atribut {@code workspace}-nya lalu
	 * di-resolve lewat {@code ConstantValues.ambil(...)}. Kedua cabang kas dijaga
	 * syarat tambahan {@code workspace == null} sehingga hanya berjalan bila field
	 * masih kosong — nilai yang sudah ditetapkan {@code saveKasLine(...)} tidak
	 * dihitung ulang.</p>
	 *
	 * <p><b>(3) Alihkan ke anggaran pendahulu.</b> Nilai yang dikembalikan
	 * <b>bukan</b> selalu field {@code workspace}: bila anggaran tersebut memiliki
	 * {@code getRelasiAnggaranSebelumnya()} yang terisi, yang dikembalikan adalah
	 * anggaran pendahulu itu. Ini mekanisme revisi/carry-over anggaran RAB — ketika
	 * sebuah baris pagu direvisi menjadi baris baru, serapan tetap dilaporkan pada
	 * baris asalnya sehingga riwayat realisasi tidak terpotong oleh revisi.
	 * <b>Perhatikan asimetri yang mudah menjebak:</b> yang tersimpan ke kolom basis
	 * data adalah field {@code workspace} (anggaran langsung), sedangkan yang dilihat
	 * pemanggil adalah anggaran pendahulunya. Kode yang membandingkan hasil getter
	 * ini dengan isi kolom {@code workspace} di SQL karena itu dapat menyimpulkan
	 * hal yang keliru. Pengalihan ini juga hanya satu tingkat — tidak ditelusuri
	 * berantai sampai pendahulu terjauh.</p>
	 *
	 * <h4>Fail-open</h4>
	 *
	 * <p>Tahap (2) dibungkus {@code try/catch} yang menelan kesalahan (dicatat lewat
	 * {@code ErrorAuditUtil}), sehingga kegagalan memuat dokumen sumber menyisakan
	 * anggaran lama alih-alih menggagalkan pembacaan. Bila semuanya gagal dan field
	 * memang kosong, method mengembalikan {@code null} meski kolomnya
	 * {@code nullable = false} — pemanggil tetap harus siap menerimanya.</p>
	 *
	 * @return baris pagu yang diserap (anggaran pendahulu bila ada), atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "workspace", nullable = false)
	public Workspace getWorkspace() {
		workspace = check(workspace);

		try {
			if (getGrupTransaksi() != null && getGrupTransaksi().getWorkspace() != null) {
				workspace = getGrupTransaksi().getWorkspace();
			} else if (getUangMuka() != null && getUangMuka().getWorkspace() != null) {
				workspace = getUangMuka().getWorkspace();
			} else if (getPermintaanPengadaanMasterAssetDetail() != null
					&& getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset() != null
					&& getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset()
							.getWorkspace() != null) {
				workspace = getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset().getWorkspace();
			} else if (getSaldoAwalMasterAssetDetail() != null && getSaldoAwalMasterAssetDetail().getWorkspace() != null) {
				workspace = getSaldoAwalMasterAssetDetail().getWorkspace();
			} else if (getPembayaranGaji() != null && getPembayaranGaji().getWorkspace() != null) {
				workspace = getPembayaranGaji().getWorkspace();
			} else if (getPertangungjawaban() != null && getPertangungjawaban().getUangMuka() != null
					&& getPertangungjawaban().getUangMuka().getWorkspace() != null) {
				workspace = getPertangungjawaban().getUangMuka().getWorkspace();
			}

			else if (getKasKecil() != null && getRef() != null && !getRef().trim().isEmpty() && workspace == null) {
				try {
					JSONArray array = new JSONArray(getKasKecil().getFormula());
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonObject = array.getJSONObject(i);
						Long key = null;
						if (!jsonObject.isNull("key")) {
							key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
						}
						if (key != null && key.toString().equals(getRef())) {
							workspace = (Workspace) (jsonObject.isNull("workspace") ? null
									: ConstantValues.ambil(Workspace.class.getName(),
											new BigDecimal(jsonObject.get("workspace") + "").longValue()));
							break;
						}
					}
					array = null;
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:460");
				}
			}

			else if (getKasBesar() != null && getRef() != null && !getRef().trim().isEmpty() && workspace == null) {
				try {
					JSONArray array = new JSONArray(getKasBesar().getFormula());
					for (int i = 0; i < array.length(); i++) {
						JSONObject jsonObject = array.getJSONObject(i);
						Long key = null;
						if (!jsonObject.isNull("key")) {
							key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
						}
						if (key != null && key.toString().equals(getRef())) {
							workspace = (Workspace) (jsonObject.isNull("workspace") ? null
									: ConstantValues.ambil(Workspace.class.getName(),
											new BigDecimal(jsonObject.get("workspace") + "").longValue()));
							break;
						}
					}
					array = null;
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:482");
				}
			}
		}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:485");
			// TODO: handle exception
		}

		return workspace == null ? null
				: (workspace.getRelasiAnggaranSebelumnya() != null ? workspace.getRelasiAnggaranSebelumnya()
						: workspace);
	}

	/**
	 * Menyetel baris pagu yang diserap baris ini.
	 *
	 * <p>Menyimpan anggaran <i>langsung</i> apa adanya, tanpa pengalihan ke anggaran
	 * pendahulu — pengalihan itu hanya terjadi pada sisi baca
	 * {@link #getWorkspace()}. Dipakai {@code saveKasLine(...)} dan
	 * {@code copySource(...)}; untuk jenis sumber non-kas nilainya akan ditimpa
	 * getter pada pembacaan berikutnya. Tidak ada validasi bahwa anggaran yang
	 * disetel masih aktif atau berada dalam cakupan satuan kerja pengguna.</p>
	 *
	 * @param workspace baris pagu yang diserap
	 */
	public void setWorkspace(Workspace workspace) {
		this.workspace = workspace;
	}

	/**
	 * Mengembalikan dokumen sumber berupa grup transaksi jurnal umum, bila baris ini
	 * bersumber dari jurnal.
	 *
	 * <p>Getter murni tanpa penurunan ulang. Relasi dipetakan EAGER dengan
	 * {@link FetchMode#SELECT} (satu query terpisah, bukan {@code join}), sehingga
	 * tidak perlu {@code check(...)} dan aman dibaca di luar sesi. Hanya baris
	 * bersumber jurnal umum yang mengisi relasi ini —
	 * {@code createPenggunaanAnggaranSource(...)} menyaring
	 * {@code jenisJurnal} agar hanya {@code Transaksi.JURNAL_UMUM} yang diproyeksikan.</p>
	 *
	 * @return grup transaksi sumber, atau {@code null} bila bukan jenis ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "grup_transaksi", nullable = true)
	public GrupTransaksi getGrupTransaksi() {
		return grupTransaksi;
	}

	/**
	 * Menyetel dokumen sumber berupa grup transaksi jurnal umum.
	 *
	 * <p>Tidak ada penjaga yang memastikan hanya satu dari kedelapan relasi sumber
	 * terisi; disiplin itu dipegang {@code copySource(...)} yang mengosongkan
	 * kedelapannya lebih dulu sebelum mengisi satu.</p>
	 *
	 * @param grupTransaksi grup transaksi sumber
	 */
	public void setGrupTransaksi(GrupTransaksi grupTransaksi) {
		this.grupTransaksi = grupTransaksi;
	}

	/**
	 * Mengembalikan dokumen sumber berupa uang muka, bila baris ini bersumber dari
	 * uang muka.
	 *
	 * <p>Getter murni tanpa penurunan ulang; relasi EAGER dengan
	 * {@link FetchMode#SELECT}. Ingat bahwa uang muka yang berbasis permintaan
	 * pengadaan tidak boleh memiliki baris penggunaan sendiri — lihat penjaga
	 * potong-anggaran ganda pada {@link #getAktif()} dan
	 * {@link #prosesSimpan(Serializable, Session)}.</p>
	 *
	 * @return uang muka sumber, atau {@code null} bila bukan jenis ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "uang_muka", nullable = true)
	public UangMuka getUangMuka() {
		return uangMuka;
	}

	/**
	 * Menyetel dokumen sumber berupa uang muka.
	 *
	 * @param uangMuka uang muka sumber
	 */
	public void setUangMuka(UangMuka uangMuka) {
		this.uangMuka = uangMuka;
	}

	/**
	 * Mengembalikan disposisi SOP yang berlaku bagi baris ini, diturunkan ulang dari
	 * dokumen sumber.
	 *
	 * <p><b>Getter yang menghitung ulang dan menimpa field — dan satu-satunya yang
	 * juga MENGOSONGKAN.</b> Berbeda dengan getter turunan lain di kelas ini yang
	 * mempertahankan nilai lama ketika tak ada cabang yang cocok, rantai
	 * {@code if/else} di sini ditutup cabang {@code else} yang menugaskan
	 * {@code null}. Jadi bila tak satu pun dokumen sumber memiliki disposisi — atau
	 * bila tak ada dokumen sumber sama sekali — field {@code disposisiSop} dibuang.
	 * Ini menjaga agar disposisi tidak "menempel" pada baris setelah dokumen
	 * sumbernya berganti, tetapi juga berarti pembacaan biasa dapat menghapus kolom
	 * ini pada penyimpanan berikutnya.</p>
	 *
	 * <p><b>Urutan prioritas berbeda dari getter lain.</b> Perhatikan bahwa rantai di
	 * sini <i>tidak</i> dimulai dari {@link GrupTransaksi} — jurnal umum tidak punya
	 * cabang sama sekali — melainkan dari {@link UangMuka}, lalu PR, saldo awal,
	 * gaji, kas kecil, kas besar, dan LPJ. Setiap cabang juga mensyaratkan disposisi
	 * dokumennya tidak {@code null}, sehingga dokumen sumber tanpa disposisi jatuh ke
	 * cabang berikutnya alih-alih menghentikan pencarian.</p>
	 *
	 * <p>Relasi dipetakan {@code fetch = FetchType.LAZY}, karena itu nilai yang ada
	 * lebih dulu dilewatkan {@code check(...)} agar proxy yang terlepas dari sesi
	 * dapat dipakai. Berbeda dengan {@link #getWorkspace()}, badan method ini
	 * <b>tidak</b> dibungkus {@code try/catch}: kegagalan memuat dokumen sumber di
	 * sini merambat ke pemanggil.</p>
	 *
	 * @return disposisi SOP yang berlaku, atau {@code null}
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);

		if (getUangMuka() != null && getUangMuka().getDisposisiSop() != null) {
			disposisiSop = getUangMuka().getDisposisiSop();
		} else if (getPermintaanPengadaanMasterAssetDetail() != null
				&& getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset() != null
				&& getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset()
						.getDisposisiSop() != null) {
			disposisiSop = getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset()
					.getDisposisiSop();
		} else if (getSaldoAwalMasterAssetDetail() != null
				&& getSaldoAwalMasterAssetDetail().getSaldoAwal().getDisposisiSop() != null) {
			disposisiSop = getSaldoAwalMasterAssetDetail().getSaldoAwal().getDisposisiSop();
		} else if (getPembayaranGaji() != null && getPembayaranGaji().getDisposisiSop() != null) {
			disposisiSop = getPembayaranGaji().getDisposisiSop();
		} else if (getKasKecil() != null && getKasKecil().getDisposisiSop() != null) {
			disposisiSop = getKasKecil().getDisposisiSop();
		} else if (getKasBesar() != null && getKasBesar().getDisposisiSop() != null) {
			disposisiSop = getKasBesar().getDisposisiSop();
		} else if (getPertangungjawaban() != null && getPertangungjawaban().getDisposisiSop() != null) {
			disposisiSop = getPertangungjawaban().getDisposisiSop();
		} else {
			disposisiSop = null;
		}

		return disposisiSop;
	}

	/**
	 * Menyetel disposisi SOP baris ini.
	 *
	 * <p><b>Setter penjaga:</b> {@code null} maupun disposisi yang belum tersimpan
	 * ({@code getId() == null}) diabaikan diam-diam sehingga nilai lama bertahan.
	 * Penjaga ini mencegah relasi menunjuk objek transien yang akan gagal saat flush
	 * — ingat bahwa relasi ini ber-{@code cascade} PERSIST dan MERGE, sehingga
	 * menyetel objek belum tersimpan dapat menyeret penyimpanan tak diinginkan.</p>
	 *
	 * <p>Perhatikan ketidaksimetrisan yang disengaja terhadap {@link #getDisposisiSop()}:
	 * setter menolak mengosongkan, sedangkan getter justru dapat mengosongkan sendiri
	 * lewat cabang {@code else}-nya. Untuk membuang disposisi, ubah dokumen
	 * sumbernya, bukan panggil setter ini dengan {@code null}.</p>
	 *
	 * @param disposisiSop disposisi SOP; diabaikan bila {@code null} atau belum
	 *                     tersimpan
	 */
	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = disposisiSop;
	}

	/**
	 * Mengembalikan dokumen sumber berupa detail saldo awal aset (pengadaan
	 * langsung/rutin), bila baris ini bersumber dari sana.
	 *
	 * <p>Getter murni tanpa penurunan ulang; relasi EAGER dengan
	 * {@link FetchMode#SELECT}. Baris jenis ini diberi label berawalan
	 * {@code "Langsung "} oleh {@link #getNama()} dan ref berakhiran {@code _RUTIN}
	 * oleh {@link #refData(PenggunaanAnggaran)}.</p>
	 *
	 * @return detail saldo awal sumber, atau {@code null} bila bukan jenis ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "saldo_awal_master_asset_detail", nullable = true)
	public SaldoAwalMasterAssetDetail getSaldoAwalMasterAssetDetail() {
		return saldoAwalMasterAssetDetail;
	}

	/**
	 * Menyetel dokumen sumber berupa detail saldo awal aset.
	 *
	 * @param saldoAwalMasterAssetDetail detail saldo awal sumber
	 */
	public void setSaldoAwalMasterAssetDetail(SaldoAwalMasterAssetDetail saldoAwalMasterAssetDetail) {
		this.saldoAwalMasterAssetDetail = saldoAwalMasterAssetDetail;
	}

	/**
	 * Mengembalikan dokumen sumber berupa pembayaran gaji, bila baris ini bersumber
	 * dari penggajian.
	 *
	 * <p>Berbeda dari relasi sumber lain yang dipetakan EAGER, relasi ini
	 * {@code fetch = FetchType.LAZY} — masuk akal karena {@link PembayaranGaji}
	 * adalah dokumen berat. Karena itu nilainya dilewatkan {@code check(...)} agar
	 * proxy yang terlepas dari sesi dapat dibangkitkan ulang; jangan hilangkan
	 * panggilan itu, atau pembacaan dari luar sesi (termasuk dari thread latar
	 * belakang {@link #simpan(Serializable)}) akan melempar
	 * {@code LazyInitializationException}.</p>
	 *
	 * @return pembayaran gaji sumber, atau {@code null} bila bukan jenis ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "pembayaran_gaji", nullable = true)
	public PembayaranGaji getPembayaranGaji() {
		pembayaranGaji = check(pembayaranGaji);
		return pembayaranGaji;
	}

	/**
	 * Menyetel dokumen sumber berupa pembayaran gaji.
	 *
	 * @param pembayaranGaji pembayaran gaji sumber
	 */
	public void setPembayaranGaji(PembayaranGaji pembayaranGaji) {
		this.pembayaranGaji = pembayaranGaji;
	}

	/**
	 * Mengembalikan dokumen sumber berupa kas kecil, bila baris ini bersumber dari
	 * salah satu baris formula kas kecil.
	 *
	 * <p>Getter murni tanpa penurunan ulang; relasi EAGER dengan
	 * {@link FetchMode#SELECT}. Perlu diingat bahwa relasi ini menunjuk
	 * <i>dokumen</i> kas kecil, bukan baris di dalamnya: satu dokumen menghasilkan
	 * banyak baris penggunaan yang dibedakan lewat {@link #getRef()}
	 * ({@code <key>_KAS_KECIL_<id dokumen>}).</p>
	 *
	 * @return kas kecil sumber, atau {@code null} bila bukan jenis ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_kecil", nullable = true)
	public KasKecil getKasKecil() {
		return kasKecil;
	}

	/**
	 * Menyetel dokumen sumber berupa kas kecil.
	 *
	 * @param kasKecil kas kecil sumber
	 */
	public void setKasKecil(KasKecil kasKecil) {
		this.kasKecil = kasKecil;
	}

	/**
	 * Mengembalikan dokumen sumber berupa kas besar, bila baris ini bersumber dari
	 * salah satu baris formula kas besar.
	 *
	 * <p>Getter murni tanpa penurunan ulang; relasi EAGER dengan
	 * {@link FetchMode#SELECT}. Seperti kas kecil, satu dokumen menghasilkan banyak
	 * baris penggunaan yang dibedakan lewat {@link #getRef()}
	 * ({@code <key>_KAS_BESAR_<id dokumen>}). Ingat pula bahwa
	 * {@link #getAktif()} tidak memiliki cabang untuk jenis ini, sehingga
	 * penonaktifan dokumen kas besar tidak dengan sendirinya mematikan barisnya.</p>
	 *
	 * @return kas besar sumber, atau {@code null} bila bukan jenis ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "kas_besar", nullable = true)
	public KasBesar getKasBesar() {
		return kasBesar;
	}

	/**
	 * Menyetel dokumen sumber berupa kas besar.
	 *
	 * @param kasBesar kas besar sumber
	 */
	public void setKasBesar(KasBesar kasBesar) {
		this.kasBesar = kasBesar;
	}

	/**
	 * Mengembalikan dokumen sumber berupa pertanggungjawaban (LPJ) uang muka, bila
	 * baris ini bersumber dari LPJ.
	 *
	 * <p>Getter murni tanpa penurunan ulang; relasi EAGER dengan
	 * {@link FetchMode#SELECT}. Baris jenis ini istimewa karena nilainya
	 * <i>negatif</i> — ia mengoreksi turun serapan uang muka sebesar dana yang
	 * dikembalikan; lihat {@link #getNilai()}.</p>
	 *
	 * @return pertanggungjawaban sumber, atau {@code null} bila bukan jenis ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "pertangungjawaban", nullable = true)
	public Pertangungjawaban getPertangungjawaban() {
		return pertangungjawaban;
	}

	/**
	 * Menyetel dokumen sumber berupa pertanggungjawaban (LPJ) uang muka.
	 *
	 * @param pertangungjawaban pertanggungjawaban sumber
	 */
	public void setPertangungjawaban(Pertangungjawaban pertangungjawaban) {
		this.pertangungjawaban = pertangungjawaban;
	}

	/**
	 * Mengembalikan dokumen sumber berupa detail permintaan pengadaan (PR), bila
	 * baris ini bersumber dari PR.
	 *
	 * <p>Getter murni tanpa penurunan ulang; relasi EAGER dengan
	 * {@link FetchMode#SELECT}. Perhatikan bahwa yang ditunjuk adalah
	 * <i>detail</i>-nya, bukan dokumen PR: setiap baris detail memotong anggaran
	 * sendiri sebesar {@code getHargaTotal()}-nya. Sifat/status seperti kode, nama,
	 * dan flag aktif justru dibaca dari dokumen induknya
	 * ({@code getPermintaanPengadaanMasterAsset()}) — lihat {@link #getAktif()}
	 * yang mencatat perbaikan pemuatan malas terkait hal ini.</p>
	 *
	 * @return detail permintaan pengadaan sumber, atau {@code null} bila bukan jenis
	 *         ini
	 */
	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@Fetch(FetchMode.SELECT)
	@JoinColumn(name = "permintaan_pengadaan_master_asset_detail", nullable = true)
	public PermintaanPengadaanMasterAssetDetail getPermintaanPengadaanMasterAssetDetail() {
		return permintaanPengadaanMasterAssetDetail;
	}

	/**
	 * Menyetel dokumen sumber berupa detail permintaan pengadaan (PR).
	 *
	 * @param permintaanPengadaanMasterAssetDetail detail permintaan pengadaan sumber
	 */
	public void setPermintaanPengadaanMasterAssetDetail(
			PermintaanPengadaanMasterAssetDetail permintaanPengadaanMasterAssetDetail) {
		this.permintaanPengadaanMasterAssetDetail = permintaanPengadaanMasterAssetDetail;
	}

	/**
	 * Mengembalikan tanggal kejadian dokumen sumber, diturunkan ulang setiap kali
	 * dipanggil.
	 *
	 * <p><b>Getter yang menghitung ulang dan menimpa field.</b> Menyusuri kedelapan
	 * dokumen sumber dalam urutan prioritas baku dan menugaskan tanggalnya ke field
	 * {@code waktu}: tanggal transaksi untuk {@link GrupTransaksi}, dan tanggal
	 * pembuatan untuk uang muka, PR (dari dokumen induknya), saldo awal (dari
	 * dokumen induknya), gaji, kas kecil, kas besar, serta LPJ.</p>
	 *
	 * <p>Inilah dimensi waktu yang dipakai laporan realisasi untuk mengelompokkan
	 * serapan ke dalam periode. Perhatikan bahwa untuk tujuh dari delapan jenis
	 * sumber yang dipakai adalah <b>tanggal pembuatan dokumen</b>, bukan tanggal
	 * transaksi maupun tanggal persetujuan — dokumen yang dibuat pada akhir satu
	 * periode dan disetujui pada periode berikutnya tetap tercatat pada periode
	 * pembuatannya. Hanya jurnal umum yang memakai tanggal transaksi sebenarnya.</p>
	 *
	 * <p>Cabang-cabangnya tidak mensyaratkan keterkaitan {@link Workspace}, dan bila
	 * tak ada cabang yang cocok field dipertahankan apa adanya. Badan method tidak
	 * dibungkus {@code try/catch}, sehingga kegagalan memuat dokumen sumber merambat
	 * ke pemanggil. Nilai kembalian boleh {@code null}.</p>
	 *
	 * @return tanggal kejadian dokumen sumber, atau {@code null}
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getWaktu() {
		if (getGrupTransaksi() != null) {
			waktu = getGrupTransaksi().getTanggalTransaksi();
		} else if (getUangMuka() != null) {
			waktu = getUangMuka().getTanggalPembuatan();
		} else if (getPermintaanPengadaanMasterAssetDetail() != null) {
			waktu = getPermintaanPengadaanMasterAssetDetail().getPermintaanPengadaanMasterAsset().getTanggalPembuatan();
		} else if (getSaldoAwalMasterAssetDetail() != null
				&& getSaldoAwalMasterAssetDetail().getSaldoAwal() != null) {
			waktu = getSaldoAwalMasterAssetDetail().getSaldoAwal().getTanggalPembuatan();
		} else if (getPembayaranGaji() != null) {
			waktu = getPembayaranGaji().getTanggalPembuatan();
		} else if (getKasKecil() != null) {
			waktu = getKasKecil().getTanggalPembuatan();
		} else if (getKasBesar() != null) {
			waktu = getKasBesar().getTanggalPembuatan();
		} else if (getPertangungjawaban() != null) {
			waktu = getPertangungjawaban().getTanggalPembuatan();
		}
		return waktu;
	}

	/**
	 * Menyetel cache tanggal kejadian baris.
	 *
	 * <p>Akan ditimpa {@link #getWaktu()} bila ada dokumen sumber yang menempel;
	 * terutama dipakai Hibernate saat memuat baris.</p>
	 *
	 * @param waktu tanggal kejadian
	 */
	public void setWaktu(Date waktu) {
		this.waktu = waktu;
	}

	/**
	 * Memeriksa apakah sebuah string berisi teks bermakna.
	 *
	 * <p>Pembantu {@code null}-aman: mengembalikan {@code true} hanya bila
	 * {@code value} tidak {@code null} dan masih menyisakan karakter setelah
	 * dipangkas. Dipakai sebagai penjaga masuk di hampir seluruh method statis alur
	 * penyimpanan agar {@code ref} kosong tidak pernah mencapai kunci advisory
	 * maupun query — {@code lockRef(...)} bergantung padanya, sehingga cabang
	 * {@code null} pada {@link #kunciRef(String)} tak pernah tercapai di
	 * produksi.</p>
	 *
	 * @param value string yang diperiksa; boleh {@code null}
	 * @return {@code true} bila berisi teks bermakna
	 */
	private static boolean hasText(String value) {
		return value != null && value.trim().length() > 0;
	}

	private static boolean sameTransactionActive(Session session) {
		try {
			return session != null && session.getTransaction() != null && session.getTransaction().isActive();
		} catch (Exception e) {
			return false;
		}
	}

	private static void rollbackQuietly(Transaction tx) {
		try {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:669");
		}
	}

	private static void clearQuietly(Session session) {
		try {
			if (session != null) {
				session.clear();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:678");
		}
	}

	private static void closeManualSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			session.clear();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:688");
		}
		try {
			session.disconnect();
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:692");
		}
		try {
			if (session.isOpen()) {
				session.close();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:698");
		}
	}

	private static boolean isDuplicateRefException(Throwable throwable) {
		Throwable t = throwable;
		while (t != null) {
			String message = t.getMessage();
			if (message != null) {
				String lower = message.toLowerCase();
				if (lower.indexOf("ref_penggunaan_anggaran") >= 0
						|| (lower.indexOf("duplicate key") >= 0 && lower.indexOf("ref") >= 0)) {
					return true;
				}
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * Gap-closure "StaleStateException: Batch update returned unexpected row count ...
	 * actual row count: 0; expected: 1": findPenggunaanAnggaranByRef() memakai
	 * session.get(id) yang bisa mengembalikan objek CACHE (first-level cache) milik
	 * baris yang baru saja dihapus lewat native SQL DELETE di tempat lain (mis.
	 * prosesKasKecil menghapus semua baris ref sebelum menulis ulang) -- Hibernate
	 * tidak tahu baris itu sudah hilang sampai UPDATE-nya dieksekusi & affected-row=0.
	 * Diperlakukan sama seperti konflik ref duplikat: retry SEKALI, memakai session
	 * yang sudah di-clear() oleh clearQuietly() di catch di bawah (persistence context
	 * kosong -> query berikutnya pasti fresh dari DB, bukan cache basi).
	 */
	private static boolean isStaleStateException(Throwable throwable) {
		Throwable t = throwable;
		while (t != null) {
			if (t instanceof org.hibernate.StaleStateException) {
				return true;
			}
			t = t.getCause();
		}
		return false;
	}

	/**
	 * String kunci advisory lock untuk sebuah {@code ref} RAB, ber-namespace.
	 *
	 * <p>Seluruh pemakai {@code hashtext(...)} di basis kode berbagi SATU ruang
	 * kunci advisory {@code bigint} global PostgreSQL — {@code hashtext(teks)}
	 * memetakan string ke 32-bit yang dilebarkan Postgres ke {@code bigint}. Bila
	 * dua fitur memakai string kunci yang sama, keduanya saling memblokir walau
	 * tak berhubungan (berpotensi deadlock). Karena itu tiap fitur memberi prefiks
	 * namespace berbeda: {@code online-bmt:} (OnlineBmt), {@code bast-sinkron:}
	 * (PengadaanPosApiHelper), {@code init:} (InitIndex),
	 * {@code PMB_NO_UJIAN_SAVE_} (CommonPMB).</p>
	 *
	 * <p>Prefiks {@code "rab-ref:"} WAJIB dipertahankan agar {@code ref} mentah —
	 * yang isinya bebas ditentukan modul lain — tak mungkin menabrak kunci fitur
	 * lain. Invarian ini dijaga {@code PenggunaanAnggaranLockSelfTest}. Lihat
	 * docs/pos dok. 83 &amp; 107.</p>
	 */
	static String kunciRef(String ref) {
		return "rab-ref:" + ref;
	}

	private static void lockRef(Session session, String ref) {
		if (!hasText(ref)) {
			return;
		}
		/*
		 * PostgreSQL advisory lock untuk mencegah dua proses bersamaan membuat ref
		 * yang sama. Jika database tidak mendukung fungsi ini, error akan ditangani
		 * oleh rollback transaksi pada pemanggil.
		 *
		 * pg_advisory_xact_lock() mengembalikan tipe void (JDBC type 1111/OTHER)
		 * yang tidak dikenal dialect Hibernate 3.6 saat auto-discovery hasil query
		 * ("No Dialect mapping for JDBC type: 1111"). Hasil di-cast ke text dan
		 * tipe scalar dideklarasikan eksplisit agar auto-discovery tidak berjalan.
		 *
		 * Kunci diberi prefiks namespace lewat kunciRef() supaya ref RAB tidak
		 * menabrak kunci advisory fitur lain yang berbagi ruang kunci global yang
		 * sama (lihat kunciRef()).
		 */
		session.createSQLQuery("select cast(pg_advisory_xact_lock(hashtext(:ref)) as text) as kunci")
				.addScalar("kunci", org.hibernate.Hibernate.STRING)
				.setString("ref", kunciRef(ref)).uniqueResult();
	}

	private static Long findPenggunaanAnggaranIdByRef(Session session, String ref) {
		if (session == null || !hasText(ref)) {
			return null;
		}
		Object id = session.createSQLQuery("select min(id) from rab.penggunaan_anggaran where ref = :ref")
				.setString("ref", ref).uniqueResult();
		if (id == null) {
			return null;
		}
		if (id instanceof Number) {
			return Long.valueOf(((Number) id).longValue());
		}
		try {
			return Long.valueOf(id.toString());
		} catch (Exception e) {
			return null;
		}
	}

	private static PenggunaanAnggaran findPenggunaanAnggaranByRef(Session session, String ref) {
		Long id = findPenggunaanAnggaranIdByRef(session, ref);
		return id == null ? null : (PenggunaanAnggaran) session.get(PenggunaanAnggaran.class, id);
	}

	private static void removeDuplicateRowsByRef(Session session, String ref) {
		if (session == null || !hasText(ref)) {
			return;
		}
		session.createSQLQuery("delete from rab.penggunaan_anggaran "
				+ "where ref = :ref and id not in "
				+ "(select min(id) from rab.penggunaan_anggaran where ref = :ref)")
				.setString("ref", ref).executeUpdate();
	}

	private static PenggunaanAnggaran createPenggunaanAnggaranSource(Serializable serializable) {
		if (serializable instanceof GrupTransaksi) {
			GrupTransaksi data = (GrupTransaksi) serializable;
			if (data.getId() == null || data.getJenisJurnal() == null
					|| !data.getJenisJurnal().equalsIgnoreCase(Transaksi.JURNAL_UMUM) || data.getWorkspace() == null) {
				return null;
			}
			PenggunaanAnggaran penggunaanAnggaran = new PenggunaanAnggaran();
			penggunaanAnggaran.setGrupTransaksi(data);
			return penggunaanAnggaran;
		}

		if (serializable instanceof UangMuka) {
			UangMuka data = (UangMuka) serializable;
			if (data.getId() == null || data.getWorkspace() == null
					|| !data.getPermintaanPengadaanMasterAssets().trim().isEmpty()) {
				return null;
			}
			PenggunaanAnggaran penggunaanAnggaran = new PenggunaanAnggaran();
			penggunaanAnggaran.setUangMuka(data);
			return penggunaanAnggaran;
		}

		if (serializable instanceof PermintaanPengadaanMasterAssetDetail) {
			PermintaanPengadaanMasterAssetDetail data = (PermintaanPengadaanMasterAssetDetail) serializable;
			if (data.getId() == null || data.getPermintaanPengadaanMasterAsset() == null
					|| data.getPermintaanPengadaanMasterAsset().getWorkspace() == null) {
				return null;
			}
			PenggunaanAnggaran penggunaanAnggaran = new PenggunaanAnggaran();
			penggunaanAnggaran.setPermintaanPengadaanMasterAssetDetail(data);
			return penggunaanAnggaran;
		}

		if (serializable instanceof SaldoAwalMasterAssetDetail) {
			SaldoAwalMasterAssetDetail data = (SaldoAwalMasterAssetDetail) serializable;
			if (data.getId() == null || data.getWorkspace() == null) {
				return null;
			}
			PenggunaanAnggaran penggunaanAnggaran = new PenggunaanAnggaran();
			penggunaanAnggaran.setSaldoAwalMasterAssetDetail(data);
			return penggunaanAnggaran;
		}

		if (serializable instanceof PembayaranGaji) {
			PembayaranGaji data = (PembayaranGaji) serializable;
			if (data.getId() == null || data.getWorkspace() == null) {
				return null;
			}
			PenggunaanAnggaran penggunaanAnggaran = new PenggunaanAnggaran();
			penggunaanAnggaran.setPembayaranGaji(data);
			return penggunaanAnggaran;
		}

		if (serializable instanceof Pertangungjawaban) {
			Pertangungjawaban data = (Pertangungjawaban) serializable;
			if (data.getId() == null || data.getUangMuka() == null || data.getUangMuka().getWorkspace() == null) {
				return null;
			}
			PenggunaanAnggaran penggunaanAnggaran = new PenggunaanAnggaran();
			penggunaanAnggaran.setPertangungjawaban(data);
			return penggunaanAnggaran;
		}

		return null;
	}

	private static void copySource(PenggunaanAnggaran target, PenggunaanAnggaran source) {
		if (target == null || source == null) {
			return;
		}
		target.setGrupTransaksi(null);
		target.setUangMuka(null);
		target.setPermintaanPengadaanMasterAssetDetail(null);
		target.setSaldoAwalMasterAssetDetail(null);
		target.setPembayaranGaji(null);
		target.setKasKecil(null);
		target.setKasBesar(null);
		target.setPertangungjawaban(null);

		if (source.getGrupTransaksi() != null) {
			target.setGrupTransaksi(source.getGrupTransaksi());
		} else if (source.getUangMuka() != null) {
			target.setUangMuka(source.getUangMuka());
		} else if (source.getPermintaanPengadaanMasterAssetDetail() != null) {
			target.setPermintaanPengadaanMasterAssetDetail(source.getPermintaanPengadaanMasterAssetDetail());
		} else if (source.getSaldoAwalMasterAssetDetail() != null) {
			target.setSaldoAwalMasterAssetDetail(source.getSaldoAwalMasterAssetDetail());
		} else if (source.getPembayaranGaji() != null) {
			target.setPembayaranGaji(source.getPembayaranGaji());
		} else if (source.getKasKecil() != null) {
			target.setKasKecil(source.getKasKecil());
		} else if (source.getKasBesar() != null) {
			target.setKasBesar(source.getKasBesar());
		} else if (source.getPertangungjawaban() != null) {
			target.setPertangungjawaban(source.getPertangungjawaban());
		}

		if (source.getWorkspace() != null) {
			target.setWorkspace(source.getWorkspace());
		}
		if (source.getNilai() != null) {
			target.setNilai(source.getNilai());
		}
		if (hasText(source.getRef())) {
			target.setRef(source.getRef());
		}
	}

	private static void saveOrUpdateByRef(Session session, PenggunaanAnggaran source, String ref) {
		saveOrUpdateByRef(session, source, ref, true);
	}

	private static void saveOrUpdateByRef(Session session, PenggunaanAnggaran source, String ref,
			boolean retryWhenDuplicate) {
		if (session == null || source == null || !hasText(ref)) {
			return;
		}

		Transaction tx = null;
		boolean startedTransaction = false;
		try {
			tx = session.getTransaction();
			if (tx == null || !tx.isActive()) {
				tx = session.beginTransaction();
				startedTransaction = true;
			}

			lockRef(session, ref);
			removeDuplicateRowsByRef(session, ref);

			PenggunaanAnggaran penggunaanAnggaran = findPenggunaanAnggaranByRef(session, ref);
			if (penggunaanAnggaran == null) {
				penggunaanAnggaran = new PenggunaanAnggaran();
			}
			copySource(penggunaanAnggaran, source);
			penggunaanAnggaran.setRef(ref);

			if (penggunaanAnggaran.getId() != null
					&& (penggunaanAnggaran.ambilRef() == null || !penggunaanAnggaran.getAktif())) {
				Common.refreshDelete(session, penggunaanAnggaran);
			} else if (penggunaanAnggaran.getAktif()) {
				Common.refreshSaveOrUpdate(session, penggunaanAnggaran);
			}

			if (startedTransaction && tx != null && tx.isActive()) {
				tx.commit();
			}
		} catch (Exception e) {
			rollbackQuietly(tx);
			clearQuietly(session);

			/*
			 * Jika masih ada proses lama/paralel yang terlanjur insert ref sama, jangan
			 * biarkan session berada pada kondisi transaction aborted. Rollback, clear,
			 * lalu ulangi sekali dengan re-query berdasarkan ref.
			 */
			if (retryWhenDuplicate && (isDuplicateRefException(e) || isStaleStateException(e))) {
				saveOrUpdateByRef(session, source, ref, false);
			} else {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:931");
			}
		}
	}

	private static void saveKasLine(Session session, String ref, KasKecil kasKecil, KasBesar kasBesar,
			Workspace workspace, Double nilai) {
		if (session == null || !hasText(ref) || workspace == null) {
			return;
		}
		PenggunaanAnggaran penggunaanAnggaran = new PenggunaanAnggaran();
		penggunaanAnggaran.setRef(ref);
		penggunaanAnggaran.setKasKecil(kasKecil);
		penggunaanAnggaran.setKasBesar(kasBesar);
		penggunaanAnggaran.setWorkspace(workspace);
		penggunaanAnggaran.setNilai(nilai == null ? 0.0 : nilai);
		saveOrUpdateByRef(session, penggunaanAnggaran, ref);
	}

	private static void prosesKasKecil(KasKecil kasKecil, Session session) {
		if (kasKecil == null || kasKecil.getId() == null || session == null) {
			return;
		}
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.createSQLQuery("delete from rab.penggunaan_anggaran where kas_kecil=" + kasKecil.getId())
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			rollbackQuietly(tx);
			clearQuietly(session);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:963");
		}

		try {
			JSONArray array = new JSONArray(kasKecil.getFormula());
			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);
				if (jsonObject.isNull("key") || jsonObject.isNull("workspace")) {
					continue;
				}
				Long key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(kasKecil.getTanggal());
				Integer tahunWorkspace = calendar.get(Calendar.YEAR);

				Workspace workspace = (Workspace) ConstantValues.ambil(Workspace.class.getName(),
						new BigDecimal(jsonObject.get("workspace") + "").longValue());
				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject, "akun")));

				if (workspace == null && akunBiaya != null) {
					workspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
							.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
							.add(Restrictions.eq("tahunWorkspace", tahunWorkspace)).add(Restrictions.eq("akun", akunBiaya))
							.addOrder(Order.desc("id")).setMaxResults(1), Workspace.class);

					if (workspace == null || !workspace.getAktif()) {
						workspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
								.add(Restrictions.eq("kode", akunBiaya.getKode())).addOrder(Order.desc("id"))
								.setMaxResults(1), Workspace.class);
					}
				}

				Double nilai = jsonObject.isNull("jumlah") ? 0.0 : Double.valueOf(jsonObject.getDouble("jumlah"));
				saveKasLine(session, key.toString() + "_KAS_KECIL_" + kasKecil.getId(), kasKecil, null, workspace, nilai);
			}
		} catch (Exception e) {
			clearQuietly(session);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:1003");
		}
	}

	private static void prosesKasBesar(KasBesar kasBesar, Session session) {
		if (kasBesar == null || kasBesar.getId() == null || session == null) {
			return;
		}
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.createSQLQuery("delete from rab.penggunaan_anggaran where kas_besar=" + kasBesar.getId())
					.executeUpdate();
			tx.commit();
		} catch (Exception e) {
			rollbackQuietly(tx);
			clearQuietly(session);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:1020");
		}

		try {
			JSONArray array = new JSONArray(kasBesar.getFormula());
			for (int i = 0; i < array.length(); i++) {
				JSONObject jsonObject = array.getJSONObject(i);
				if (jsonObject.isNull("key") || jsonObject.isNull("workspace")) {
					continue;
				}
				Long key = ais.common.CommonJSONUtil.ambilLong(jsonObject, "key");
				Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
				calendar.setTime(kasBesar.getTanggal());
				Integer tahunWorkspace = calendar.get(Calendar.YEAR);

				Workspace workspace = (Workspace) ConstantValues.ambil(Workspace.class.getName(),
						new BigDecimal(jsonObject.get("workspace") + "").longValue());
				Akun akunBiaya = (Akun) (jsonObject.isNull("akun") ? null
						: ConstantValues.ambil(Akun.class.getName(), ais.common.CommonJSONUtil.ambilLong(jsonObject, "akun")));

				if (workspace == null && akunBiaya != null) {
					workspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
							.add(Restrictions.or(Restrictions.eq("carryOver", true),
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
							.add(Restrictions.eq("tahunWorkspace", tahunWorkspace)).add(Restrictions.eq("akun", akunBiaya))
							.addOrder(Order.desc("id")).setMaxResults(1), Workspace.class);

					if (workspace == null || !workspace.getAktif()) {
						workspace = (Workspace) ConstantValues.simpleObject(session.createCriteria(Workspace.class)
								.add(Restrictions.or(Restrictions.eq("carryOver", true),
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))))
								.add(Restrictions.eq("tahunWorkspace", tahunWorkspace))
								.add(Restrictions.eq("kode", akunBiaya.getKode())).addOrder(Order.desc("id"))
								.setMaxResults(1), Workspace.class);
					}
				}

				Double nilai = jsonObject.isNull("jumlah") ? 0.0 : Double.valueOf(jsonObject.getDouble("jumlah"));
				saveKasLine(session, key.toString() + "_KAS_BESAR_" + kasBesar.getId(), null, kasBesar, workspace, nilai);
			}
		} catch (Exception e) {
			clearQuietly(session);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:1062");
		}
	}

	public static void prosesSimpan(Serializable serializable, Session session) {
		if (serializable == null || session == null) {
			return;
		}

		try {
			if (serializable instanceof KasKecil) {
				prosesKasKecil((KasKecil) serializable, session);
				return;
			}
			if (serializable instanceof KasBesar) {
				prosesKasBesar((KasBesar) serializable, session);
				return;
			}

			PenggunaanAnggaran penggunaanAnggaran = createPenggunaanAnggaranSource(serializable);
			if (penggunaanAnggaran == null) {
				// UangMuka BERDASARKAN PR: gate mengembalikan null (tak boleh punya baris penggunaan
				// sendiri, karena PR-nya sudah memotong anggaran). Bersihkan baris penggunaan uang muka
				// yang mungkin tertinggal AKTIF — mis. UM dibuat tanpa PR (baris terlanjur dibuat) lalu
				// diedit menjadi berbasis PR — agar anggaran tidak terpotong DUA KALI.
				if (serializable instanceof UangMuka) {
					UangMuka um = (UangMuka) serializable;
					if (um.getId() != null && um.getPermintaanPengadaanMasterAssets() != null
							&& !um.getPermintaanPengadaanMasterAssets().trim().isEmpty()) {
						try {
							session.createQuery("delete from PenggunaanAnggaran where uangMuka.id = :um")
									.setLong("um", um.getId()).executeUpdate();
						} catch (Exception exHapus) {
							exHapus.printStackTrace(); ais.common.ErrorAuditUtil.record(exHapus, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:1095");
						}
					}
				}
				return;
			}

			String ref = PenggunaanAnggaran.refData(penggunaanAnggaran);
			if (!hasText(ref)) {
				return;
			}
			penggunaanAnggaran.setRef(ref);
			saveOrUpdateByRef(session, penggunaanAnggaran, ref);
		} catch (Exception e) {
			clearQuietly(session);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:1110");
		}
	}

	
	public static void simpan(final Serializable serializable) {

		if (serializable instanceof UangMuka || serializable instanceof PermintaanPengadaanMasterAssetDetail
				|| serializable instanceof SaldoAwalMasterAssetDetail || serializable instanceof PembayaranGaji
				|| serializable instanceof KasKecil || serializable instanceof KasBesar
				|| serializable instanceof GrupTransaksi || serializable instanceof Pertangungjawaban) {
			new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						Thread.sleep(3000);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:1128");
					}

					Session session = null;
					try {
						/*
						 * currentNativeSession harus ditutup manual. Seluruh proses simpan
						 * PenggunaanAnggaran dibuat idempotent berbasis ref agar tidak memicu
						 * duplicate key ref_penggunaan_anggaran.
						 */
						session = HibernateUtil.currentNativeSession();
						prosesSimpan(serializable, session);
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/rab/PenggunaanAnggaran.java:1141");
					} finally {
						closeManualSession(session);
					}
				}
			}).start();
		}
	}
}