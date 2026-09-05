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

	/**
	 * Memeriksa apakah sebuah sesi Hibernate sedang berada di dalam transaksi aktif.
	 *
	 * <p><b>Saat ini tidak dipanggil dari mana pun</b> — penelusuran seluruh basis
	 * kode hanya menemukan deklarasinya, sehingga method ini kode mati. Perannya
	 * digantikan pemeriksaan {@code tx == null || !tx.isActive()} yang ditulis
	 * langsung di dalam {@code saveOrUpdateByRef(...)}. Method dipertahankan karena
	 * tidak berbahaya dan mendokumentasikan pola pemeriksaan yang aman: pemanggilan
	 * {@code getTransaction()} pada sesi yang sudah tertutup dapat melempar, sehingga
	 * seluruhnya dibungkus {@code try/catch} yang mengembalikan {@code false}.</p>
	 *
	 * @param session sesi yang diperiksa; boleh {@code null}
	 * @return {@code true} bila sesi ada dan transaksinya aktif
	 */
	private static boolean sameTransactionActive(Session session) {
		try {
			return session != null && session.getTransaction() != null && session.getTransaction().isActive();
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Membatalkan sebuah transaksi tanpa pernah melempar.
	 *
	 * <p>Dipakai di jalur penanganan kesalahan, tempat melempar pengecualian baru
	 * akan menutupi pengecualian asli. Transaksi hanya dibatalkan bila tidak
	 * {@code null} dan masih aktif; kegagalan pembatalan itu sendiri ditelan dan
	 * dicatat lewat {@code ErrorAuditUtil}.</p>
	 *
	 * @param tx transaksi yang dibatalkan; boleh {@code null} atau sudah selesai
	 */
	private static void rollbackQuietly(Transaction tx) {
		try {
			if (tx != null && tx.isActive()) {
				tx.rollback();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:669");
		}
	}

	/**
	 * Mengosongkan konteks persistensi sebuah sesi tanpa pernah melempar.
	 *
	 * <p>Perannya lebih dari sekadar kerapian. Setelah sebuah transaksi gagal,
	 * cache tingkat pertama sesi masih memuat objek-objek dengan keadaan yang tidak
	 * lagi sesuai basis data — termasuk objek milik baris yang sudah dihapus proses
	 * lain. {@code session.clear()} membuang seluruhnya sehingga query berikutnya
	 * dijamin membaca ulang dari basis data, bukan dari cache basi. Inilah yang
	 * membuat percobaan ulang di {@code saveOrUpdateByRef(...)} bermakna: tanpa
	 * pengosongan ini, percobaan kedua akan mengulangi kesalahan yang sama. Lihat
	 * penjelasan pada {@link #isStaleStateException(Throwable)}.</p>
	 *
	 * @param session sesi yang dikosongkan; boleh {@code null}
	 */
	private static void clearQuietly(Session session) {
		try {
			if (session != null) {
				session.clear();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/rab/PenggunaanAnggaran.java:678");
		}
	}

	/**
	 * Menutup sesi Hibernate yang dibuka manual, selangkah demi selangkah dan tanpa
	 * pernah melempar.
	 *
	 * <p>Sesi yang diperoleh {@code HibernateUtil.currentNativeSession()} di dalam
	 * {@link #simpan(Serializable)} <b>tidak</b> dikelola kontainer dan wajib
	 * ditutup sendiri; bila tidak, koneksinya bocor dan kumpulan koneksi
	 * (<i>connection pool</i>) akan habis setelah cukup banyak penyimpanan dokumen.
	 * Karena itu method ini dipanggil dari blok {@code finally}.</p>
	 *
	 * <p>Tiga langkahnya — {@code clear()}, {@code disconnect()}, lalu {@code close()}
	 * bila masih terbuka — masing-masing dibungkus {@code try/catch} terpisah,
	 * <b>bukan</b> satu blok bersama. Ini disengaja: kegagalan satu langkah tidak
	 * boleh mencegah langkah berikutnya dijalankan, karena justru langkah terakhirlah
	 * yang mengembalikan koneksi ke kumpulan. Semua kegagalan dicatat lewat
	 * {@code ErrorAuditUtil}.</p>
	 *
	 * @param session sesi yang ditutup; {@code null} diabaikan
	 */
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

	/**
	 * Mengenali apakah sebuah pengecualian berasal dari pelanggaran keunikan
	 * {@code ref}.
	 *
	 * <p>Menelusuri seluruh rantai {@code getCause()} — pelanggaran constraint basis
	 * data biasanya terbungkus beberapa lapis di dalam {@code ConstraintViolationException}
	 * dan {@code SQLException} — lalu mencocokkan pesan secara <i>case-insensitive</i>
	 * terhadap dua pola: nama constraint {@code ref_penggunaan_anggaran}, atau
	 * kombinasi {@code "duplicate key"} bersama kata {@code "ref"}. Pola kedua ada
	 * sebagai jaring pengaman untuk pesan driver/basis data yang tidak menyebut nama
	 * constraint.</p>
	 *
	 * <p>Bila {@code true}, {@code saveOrUpdateByRef(...)} mencoba ulang sekali
	 * dengan sesi yang sudah dikosongkan. Perhatikan keterbatasan pendekatan ini:
	 * pengenalan berbasis <i>teks pesan</i> bersifat rapuh terhadap perubahan versi
	 * basis data maupun lokalisasi pesan; bila pengenalan gagal, akibatnya bukan
	 * korupsi data melainkan hilangnya percobaan ulang — kesalahan sekadar dicatat
	 * dan baris proyeksi tidak diperbarui pada siklus itu.</p>
	 *
	 * @param throwable pengecualian yang diperiksa; boleh {@code null}
	 * @return {@code true} bila berasal dari tabrakan {@code ref} duplikat
	 */
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
	 *
	 * <p>Berbeda dengan {@link #isDuplicateRefException(Throwable)} yang mencocokkan
	 * teks pesan, deteksi di sini berbasis <i>tipe</i>
	 * ({@code instanceof org.hibernate.StaleStateException}) menyusuri seluruh rantai
	 * {@code getCause()}, sehingga kebal terhadap perubahan kata-kata pesan.</p>
	 *
	 * @param throwable pengecualian yang diperiksa; boleh {@code null}
	 * @return {@code true} bila rantai penyebabnya memuat
	 *         {@code StaleStateException}
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
	 *
	 * <p>Cakupan jaminannya perlu dipahami dengan tepat: yang dijamin prefiks adalah
	 * ketidaksamaan <b>string</b> kunci antarfitur. {@code hashtext()} tetap memetakan
	 * string ke ruang 32-bit, sehingga tabrakan hash secara teoretis masih mungkin
	 * dan berada di luar jangkauan mekanisme ini. Akibat terburuk sebuah tabrakan
	 * hash pun terbatas: dua penulisan yang tak berhubungan saling menunggu sesaat,
	 * bukan korupsi data.</p>
	 *
	 * <p>Bersifat {@code static} dengan visibilitas <i>package-private</i> secara
	 * sengaja — cukup terbuka untuk diuji {@link PenggunaanAnggaranLockSelfTest}
	 * yang berada di paket yang sama, tanpa menjadi bagian API publik.
	 * {@code ref} bernilai {@code null} menghasilkan {@code "rab-ref:null"} dan tidak
	 * melempar; dalam alur produksi kasus itu tak tercapai karena
	 * {@code lockRef(...)} sudah menyaringnya lewat {@link #hasText(String)}.</p>
	 *
	 * @param ref kunci alami baris RAB; boleh {@code null}
	 * @return string kunci advisory ber-namespace, tidak pernah {@code null}
	 * @see PenggunaanAnggaranLockSelfTest
	 */
	static String kunciRef(String ref) {
		return "rab-ref:" + ref;
	}

	/**
	 * Mengambil kunci advisory transaksional PostgreSQL atas sebuah {@code ref} RAB.
	 *
	 * <h4>Masalah yang dipecahkan</h4>
	 *
	 * <p>Baris penggunaan anggaran ditulis dari thread latar belakang yang
	 * dijadwalkan {@link #simpan(Serializable)}. Bila satu dokumen sumber disimpan
	 * dua kali beruntun — atau bila dua proses menyentuh dokumen yang sama — dua
	 * thread dapat menjalankan urutan "cari baris berdasarkan ref, tidak ada, buat
	 * baru" secara berselang, sehingga keduanya menyisipkan baris dengan {@code ref}
	 * yang sama. Kunci advisory ini menserialkan seluruh urutan tersebut per
	 * {@code ref}: thread kedua menunggu sampai transaksi thread pertama selesai,
	 * lalu menemukan baris yang sudah ada dan memperbaruinya alih-alih membuat
	 * kembar.</p>
	 *
	 * <h4>Rincian teknis yang mudah dirusak</h4>
	 *
	 * <ul>
	 *   <li><b>Transaksional, bukan sesi.</b> Dipakai
	 *       {@code pg_advisory_xact_lock()}, bukan {@code pg_advisory_lock()},
	 *       sehingga kunci dilepas otomatis saat transaksi selesai (commit maupun
	 *       rollback). Tidak ada jalur pelepasan manual, dan karenanya tidak ada
	 *       risiko kunci tertinggal ketika penulisan gagal.</li>
	 *   <li><b>Cast ke {@code text} bukan hiasan.</b> {@code pg_advisory_xact_lock()}
	 *       mengembalikan tipe {@code void} (JDBC type 1111/OTHER) yang tidak dikenali
	 *       dialect Hibernate 3.6 saat menebak tipe hasil query, memunculkan
	 *       "No Dialect mapping for JDBC type: 1111". Karena itu hasilnya di-cast ke
	 *       {@code text} dan tipe skalarnya dideklarasikan eksplisit lewat
	 *       {@code addScalar("kunci", Hibernate.STRING)} agar penebakan otomatis tidak
	 *       berjalan. Jangan menyederhanakan query ini.</li>
	 *   <li><b>Prefiks namespace wajib.</b> Yang dikunci bukan {@code ref} mentah
	 *       melainkan {@link #kunciRef(String)}, agar ref RAB — yang isinya ditentukan
	 *       modul lain — tidak menabrak kunci advisory fitur lain yang berbagi ruang
	 *       kunci global {@code hashtext()} yang sama. Invarian ini dijaga
	 *       {@link PenggunaanAnggaranLockSelfTest}.</li>
	 *   <li><b>Parameter terikat.</b> Nilai {@code ref} dikirim lewat
	 *       {@code setString(...)}, bukan disambung ke dalam SQL, sehingga aman dari
	 *       penyisipan SQL meski isi ref berasal dari modul lain.</li>
	 * </ul>
	 *
	 * <p>Ref kosong disaring lebih dulu dan menyebabkan method keluar tanpa
	 * melakukan apa pun — memanggil kunci untuk ref kosong akan menserialkan seluruh
	 * penulisan yang kebetulan ber-ref kosong menjadi satu antrean. Kegagalan
	 * eksekusi (mis. basis data tanpa fungsi tersebut) sengaja <b>tidak</b> ditangani
	 * di sini; ia merambat ke {@code saveOrUpdateByRef(...)} yang membatalkan
	 * transaksi.</p>
	 *
	 * @param session sesi Hibernate yang transaksinya sedang aktif
	 * @param ref     kunci alami baris; kosong berarti tanpa penguncian
	 * @see #kunciRef(String)
	 */
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

	/**
	 * Mencari id baris penggunaan anggaran berdasarkan kunci alami {@code ref}, lewat
	 * SQL native.
	 *
	 * <p>Memakai {@code select min(id) ... where ref = :ref}. Pemilihan
	 * <b>{@code min(id)}</b> disengaja dan penting: bila karena suatu hal terdapat
	 * beberapa baris ber-ref sama (data lama sebelum constraint unik terpasang, atau
	 * hasil balapan), yang dipertahankan selalu baris <i>tertua</i>. Aturan yang sama
	 * dipakai {@link #removeDuplicateRowsByRef(Session, String)} saat menghapus
	 * kembarannya, sehingga keduanya konsisten dan hasilnya deterministik. Bila tak
	 * ada baris, agregat mengembalikan {@code NULL} — bukan baris kosong — sehingga
	 * {@code uniqueResult()} aman.</p>
	 *
	 * <p>Query ditulis native, bukan HQL, agar tidak menyentuh konteks persistensi:
	 * hasilnya id mentah dari basis data, bukan objek dari cache tingkat pertama yang
	 * mungkin sudah basi. Nilai yang kembali dikonversi secara defensif — cabang
	 * {@code Number} untuk tipe numerik apa pun yang dikembalikan driver, lalu
	 * cadangan penguraian dari {@code toString()} yang bila gagal menghasilkan
	 * {@code null} alih-alih melempar.</p>
	 *
	 * @param session sesi Hibernate; {@code null} menghasilkan {@code null}
	 * @param ref     kunci alami yang dicari; kosong menghasilkan {@code null}
	 * @return id baris tertua ber-ref tersebut, atau {@code null} bila tidak ada
	 */
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

	/**
	 * Memuat baris penggunaan anggaran berdasarkan kunci alami {@code ref}.
	 *
	 * <p>Dua langkah: cari id lewat
	 * {@link #findPenggunaanAnggaranIdByRef(Session, String)} (SQL native, bebas
	 * cache), lalu muat entitasnya dengan {@code session.get(...)}.</p>
	 *
	 * <p><b>Peringatan cache tingkat pertama.</b> Langkah kedua dapat mengembalikan
	 * objek dari konteks persistensi sesi, termasuk objek milik baris yang sementara
	 * itu sudah dihapus proses lain lewat DELETE native — misalnya
	 * {@code prosesKasKecil(...)} yang menghapus semua baris sebuah dokumen sebelum
	 * menulisnya ulang. Hibernate baru menyadarinya ketika UPDATE dijalankan dan
	 * jumlah baris terpengaruh nol, lalu melempar {@code StaleStateException}. Itulah
	 * mengapa {@code saveOrUpdateByRef(...)} memperlakukan pengecualian tersebut sama
	 * seperti tabrakan ref duplikat: kosongkan sesi, ulangi sekali. Lihat
	 * {@link #isStaleStateException(Throwable)} dan
	 * {@link #clearQuietly(Session)}.</p>
	 *
	 * @param session sesi Hibernate
	 * @param ref     kunci alami yang dicari
	 * @return baris yang ditemukan, atau {@code null} bila tidak ada
	 */
	private static PenggunaanAnggaran findPenggunaanAnggaranByRef(Session session, String ref) {
		Long id = findPenggunaanAnggaranIdByRef(session, ref);
		return id == null ? null : (PenggunaanAnggaran) session.get(PenggunaanAnggaran.class, id);
	}

	/**
	 * Menghapus baris-baris kembar yang berbagi {@code ref} yang sama, menyisakan
	 * yang tertua.
	 *
	 * <p>Menjalankan {@code delete ... where ref = :ref and id not in (select min(id)
	 * ... where ref = :ref)} lewat SQL native. Aturan "yang bertahan adalah
	 * {@code min(id)}" identik dengan yang dipakai
	 * {@link #findPenggunaanAnggaranIdByRef(Session, String)}, sehingga baris yang
	 * disisakan di sini persis baris yang akan ditemukan dan diperbarui
	 * sesudahnya.</p>
	 *
	 * <p><b>Mengapa masih perlu meski ada constraint unik.</b> Method ini dipanggil
	 * {@code saveOrUpdateByRef(...)} setelah kunci advisory diambil, sebagai
	 * pembersih data yang sudah terlanjur kembar — dari periode sebelum constraint
	 * {@code ref_penggunaan_anggaran} dipasang, dari impor, atau dari proses lama
	 * yang berjalan paralel. Tanpa pembersihan ini, satu baris kembar akan membuat
	 * anggaran terserap berlipat pada laporan realisasi, dan penulisan berikutnya
	 * akan terus gagal pada constraint. Karena dijalankan di bawah kunci advisory
	 * per-ref, ia tidak berlomba dengan proses lain atas ref yang sama.</p>
	 *
	 * <p>Nilai {@code ref} dikirim sebagai parameter terikat, bukan disambung ke
	 * dalam SQL.</p>
	 *
	 * @param session sesi Hibernate dengan transaksi aktif; {@code null} diabaikan
	 * @param ref     kunci alami yang dibersihkan; kosong diabaikan
	 */
	private static void removeDuplicateRowsByRef(Session session, String ref) {
		if (session == null || !hasText(ref)) {
			return;
		}
		session.createSQLQuery("delete from rab.penggunaan_anggaran "
				+ "where ref = :ref and id not in "
				+ "(select min(id) from rab.penggunaan_anggaran where ref = :ref)")
				.setString("ref", ref).executeUpdate();
	}

	/**
	 * Pabrik yang mengubah sebuah dokumen sumber menjadi objek
	 * {@link PenggunaanAnggaran} sementara — sekaligus <b>gerbang kelayakan</b> yang
	 * menentukan dokumen mana yang boleh memotong anggaran.
	 *
	 * <h4>Peran ganda</h4>
	 *
	 * <p>Selain merakit objek, method ini adalah tempat seluruh aturan kelayakan
	 * berada. Nilai kembalian {@code null} berarti "dokumen ini tidak boleh punya
	 * baris penggunaan anggaran", dan {@link #prosesSimpan(Serializable, Session)}
	 * akan berhenti (atau, untuk uang muka, justru membersihkan baris lama).
	 * Objek yang dihasilkan hanya menempelkan dokumen sumbernya; seluruh atribut
	 * lain — nilai, workspace, nama, ref — diturunkan belakangan oleh getter
	 * masing-masing.</p>
	 *
	 * <h4>Aturan kelayakan per jenis</h4>
	 *
	 * <ul>
	 *   <li><b>{@link GrupTransaksi}</b> — hanya diterima bila sudah tersimpan,
	 *       {@code jenisJurnal}-nya {@code Transaksi.JURNAL_UMUM}, dan terkait
	 *       {@link Workspace}. Jurnal jenis lain (mis. jurnal otomatis dari modul
	 *       lain) sengaja tidak diproyeksikan agar serapan tidak dihitung dua kali
	 *       dari dokumen asalnya <i>dan</i> dari jurnalnya.</li>
	 *   <li><b>{@link UangMuka}</b> — diterima bila sudah tersimpan, terkait
	 *       {@link Workspace}, dan <b>tidak</b> merujuk permintaan pengadaan.
	 *       Syarat terakhir adalah lapis pertama penjaga potong-anggaran ganda:
	 *       bila uang muka dibuat berdasarkan PR, PR-nya sudah memotong anggaran,
	 *       sehingga uang mukanya tidak boleh memotong lagi. Lapis kedua ada di
	 *       {@link #getAktif()}, lapis ketiga di
	 *       {@link #prosesSimpan(Serializable, Session)}.</li>
	 *   <li><b>{@link PermintaanPengadaanMasterAssetDetail}</b> — diterima bila
	 *       detailnya sudah tersimpan dan dokumen PR induknya ada serta terkait
	 *       {@link Workspace}.</li>
	 *   <li><b>{@link SaldoAwalMasterAssetDetail}</b> dan <b>{@link PembayaranGaji}</b>
	 *       — diterima bila sudah tersimpan dan terkait {@link Workspace}.</li>
	 *   <li><b>{@link Pertangungjawaban}</b> — diterima bila sudah tersimpan dan uang
	 *       muka induknya ada serta terkait {@link Workspace}. Perhatikan bahwa
	 *       keterkaitan anggaran diperiksa lewat uang muka, bukan lewat LPJ
	 *       sendiri.</li>
	 * </ul>
	 *
	 * <h4>Yang sengaja tidak dilayani</h4>
	 *
	 * <p>{@link KasKecil} dan {@link KasBesar} <b>tidak</b> ditangani di sini dan
	 * jatuh ke {@code return null} di akhir. Keduanya disaring lebih dulu oleh
	 * {@link #prosesSimpan(Serializable, Session)} dan diproses jalur khusus
	 * {@code prosesKasKecil(...)}/{@code prosesKasBesar(...)}, karena satu dokumen kas
	 * memuat banyak baris anggaran di dalam formula JSON-nya sehingga tidak dapat
	 * dipetakan satu-dokumen-satu-baris. Jenis {@link Serializable} lain juga
	 * menghasilkan {@code null}.</p>
	 *
	 * <p>Syarat {@code getId() != null} yang muncul di setiap cabang menjaga agar
	 * proyeksi tidak dibuat untuk dokumen yang belum memiliki identitas — tanpa id,
	 * {@link #refData(PenggunaanAnggaran)} tak dapat membentuk kunci alami yang
	 * stabil dan idempotensi penyimpanan akan hilang.</p>
	 *
	 * @param serializable dokumen sumber yang baru disimpan; boleh jenis apa pun
	 * @return objek penggunaan anggaran sementara, atau {@code null} bila dokumen
	 *         tidak layak memotong anggaran
	 */
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

	/**
	 * Menyalin dokumen sumber dan atribut turunannya dari objek sementara ke baris
	 * yang akan disimpan.
	 *
	 * <h4>Mengosongkan dulu, baru mengisi satu</h4>
	 *
	 * <p>Langkah pertama method ini mengosongkan <b>kedelapan</b> relasi sumber pada
	 * {@code target}, baru kemudian mengisi tepat satu sesuai isi {@code source}.
	 * Inilah satu-satunya tempat yang menegakkan invarian "satu baris menunjuk tepat
	 * satu dokumen sumber" — setter individual tidak memeriksa apa pun. Pengosongan
	 * ini penting untuk baris yang sudah ada: bila sebuah dokumen berubah jenis
	 * kaitannya, relasi lama harus benar-benar dilepas, bukan sekadar tertimpa oleh
	 * relasi baru.</p>
	 *
	 * <h4>Penyalinan bersyarat untuk atribut turunan</h4>
	 *
	 * <p>Tiga atribut berikutnya — {@code workspace}, {@code nilai}, dan {@code ref} —
	 * hanya disalin bila terisi pada {@code source}. Sifat "salin bila ada" ini
	 * disengaja dan menopang jalur kas: {@code saveKasLine(...)} membentuk objek
	 * sementara yang <i>sudah</i> membawa ketiganya (karena baris kas tidak dapat
	 * menurunkannya sendiri dari dokumen), sementara untuk jenis sumber lain
	 * ketiganya dibiarkan kosong dan akan diturunkan sendiri oleh
	 * {@link #getWorkspace()}, {@link #getNilai()}, dan {@link #getRef()}. Efek
	 * sampingnya: nilai atau workspace yang sudah tersimpan pada baris lama tidak
	 * akan dikosongkan oleh penyalinan ini — hanya bisa berubah menjadi nilai baru,
	 * tidak menjadi kosong.</p>
	 *
	 * <p>Perhatikan bahwa pembacaan {@code source.getWorkspace()} di sini melewati
	 * getter, sehingga nilai yang disalin sudah melalui pengalihan ke anggaran
	 * pendahulu bila ada — lihat catatan asimetri pada {@link #getWorkspace()}.
	 * Parameter {@code null} pada salah satu sisi membuat method keluar tanpa
	 * melakukan apa pun.</p>
	 *
	 * @param target baris yang akan disimpan (baru atau hasil pencarian ref)
	 * @param source objek sementara pembawa dokumen sumber
	 */
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

	/**
	 * Menyimpan atau memperbarui baris penggunaan anggaran berdasarkan kunci alami
	 * {@code ref}, dengan satu kali percobaan ulang bila terjadi konflik.
	 *
	 * <p>Bentuk ringkas dari
	 * {@link #saveOrUpdateByRef(Session, PenggunaanAnggaran, String, boolean)} dengan
	 * {@code retryWhenDuplicate = true}. Inilah bentuk yang dipakai seluruh pemanggil
	 * di dalam kelas ini.</p>
	 *
	 * @param session sesi Hibernate
	 * @param source  objek sementara pembawa dokumen sumber
	 * @param ref     kunci alami baris
	 */
	private static void saveOrUpdateByRef(Session session, PenggunaanAnggaran source, String ref) {
		saveOrUpdateByRef(session, source, ref, true);
	}

	/**
	 * Inti penulisan proyeksi: menyimpan, memperbarui, atau menghapus satu baris
	 * penggunaan anggaran secara idempoten berdasarkan kunci alami {@code ref}.
	 *
	 * <h4>Urutan kerja</h4>
	 *
	 * <ol>
	 *   <li><b>Pastikan ada transaksi.</b> Bila sesi sudah berada dalam transaksi
	 *       aktif, transaksi itu dipakai apa adanya dan <b>tidak</b> di-commit di
	 *       akhir — kepemilikan transaksi tetap pada pemanggil. Bila belum,
	 *       transaksi baru dibuka dan ditandai {@code startedTransaction} sehingga
	 *       hanya transaksi milik sendiri yang di-commit. Pola ini membuat method
	 *       aman dipanggil dari dalam maupun luar transaksi berjalan.</li>
	 *   <li><b>Kunci advisory.</b> {@code lockRef(...)} menserialkan seluruh langkah
	 *       berikutnya per {@code ref}, sehingga urutan "cari, tidak ada, buat" tidak
	 *       dapat berselang dengan proses lain atas ref yang sama.</li>
	 *   <li><b>Bersihkan kembar.</b> {@code removeDuplicateRowsByRef(...)} menyisakan
	 *       baris tertua bila terdapat baris ber-ref sama.</li>
	 *   <li><b>Cari atau buat.</b> Baris dicari lewat
	 *       {@code findPenggunaanAnggaranByRef(...)}; bila tak ada, dibuat instans
	 *       baru. Lalu {@code copySource(...)} memindahkan dokumen sumber dan atribut
	 *       turunannya, dan {@code ref} diset ulang secara eksplisit.</li>
	 *   <li><b>Putuskan simpan atau hapus.</b> Baris yang sudah punya id
	 *       <i>dihapus</i> ({@code Common.refreshDelete}) bila
	 *       {@link #ambilRef()}-nya kini {@code null} — dokumen sumber sudah tidak
	 *       berhak punya proyeksi — <b>atau</b> bila {@link #getAktif()}-nya
	 *       {@code false}. Selain itu, baris yang aktif disimpan/diperbarui lewat
	 *       {@code Common.refreshSaveOrUpdate}. Perhatikan kombinasi yang tidak
	 *       tertangani: baris <i>baru</i> (belum ber-id) yang tidak aktif tidak
	 *       masuk cabang mana pun dan sekadar diabaikan — itu memang perilaku yang
	 *       diinginkan, tak ada yang perlu ditulis.</li>
	 *   <li><b>Commit</b> hanya bila transaksi dibuka sendiri.</li>
	 * </ol>
	 *
	 * <h4>Penanganan kegagalan dan percobaan ulang</h4>
	 *
	 * <p>Setiap kegagalan memicu {@code rollbackQuietly(...)} lalu
	 * {@code clearQuietly(...)}. Pengosongan konteks persistensi adalah syarat mutlak
	 * agar percobaan ulang bermakna: tanpanya, query berikutnya masih akan menemukan
	 * objek basi dari cache tingkat pertama dan gagal dengan cara yang sama.</p>
	 *
	 * <p>Percobaan ulang dilakukan <b>sekali saja</b> — rekursi memanggil dirinya
	 * dengan {@code retryWhenDuplicate = false} sehingga tidak mungkin berulang tanpa
	 * batas — dan hanya untuk dua jenis kegagalan yang memang dapat disembuhkan
	 * dengan membaca ulang: tabrakan {@code ref} duplikat
	 * ({@link #isDuplicateRefException(Throwable)}, artinya proses lain menyisipkan
	 * lebih dulu — percobaan kedua akan menemukan barisnya dan memperbarui) dan
	 * {@code StaleStateException} ({@link #isStaleStateException(Throwable)}, artinya
	 * baris yang hendak diperbarui ternyata sudah dihapus proses lain — percobaan
	 * kedua akan membuat baris baru). Kegagalan jenis lain hanya dicatat lewat
	 * {@code ErrorAuditUtil}.</p>
	 *
	 * <p><b>Gagal secara senyap.</b> Method ini tidak pernah melempar ke pemanggil.
	 * Bila percobaan ulang pun gagal, baris proyeksi sekadar tidak diperbarui pada
	 * siklus itu dan pengguna tidak diberi tahu apa pun — konsisten dengan sifat
	 * asinkron seluruh jalur ini. Konsekuensinya, angka realisasi dapat tertinggal
	 * dari dokumen sumbernya sampai dokumen itu disimpan ulang. Ini bukan penjaga
	 * integritas nilai: tidak ada pemeriksaan pagu, batas, maupun tanda di sepanjang
	 * jalur ini.</p>
	 *
	 * @param session            sesi Hibernate; {@code null} membuat method keluar
	 * @param source             objek sementara pembawa dokumen sumber;
	 *                           {@code null} membuat method keluar
	 * @param ref                kunci alami baris; kosong membuat method keluar
	 * @param retryWhenDuplicate {@code true} pada pemanggilan pertama; {@code false}
	 *                           pada percobaan ulang agar rekursi berhenti
	 */
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

	/**
	 * Menulis satu baris penggunaan anggaran yang berasal dari sebuah baris formula
	 * kas kecil atau kas besar.
	 *
	 * <p>Berbeda dari jenis sumber lain, baris kas <b>tidak dapat menurunkan sendiri</b>
	 * atribut-atributnya dari dokumen: satu dokumen kas memuat banyak baris anggaran
	 * di dalam kolom {@code formula} berformat JSON. Karena itu method ini merakit
	 * objek sementara yang sudah lengkap — {@code ref}, dokumen kas, {@code workspace},
	 * dan {@code nilai} — lalu menyerahkannya ke
	 * {@link #saveOrUpdateByRef(Session, PenggunaanAnggaran, String)} yang menangani
	 * penguncian, pembersihan kembar, serta simpan/perbarui secara idempoten. Sifat
	 * "salin bila terisi" pada {@code copySource(...)} memastikan ketiga atribut itu
	 * benar-benar sampai ke baris tujuan.</p>
	 *
	 * <p>Parameter {@code kasKecil} dan {@code kasBesar} bersifat saling
	 * meniadakan — pemanggil mengisi salah satunya dan mengirim {@code null} untuk
	 * yang lain. Method ini <b>tidak memeriksa</b> bahwa hanya satu yang terisi;
	 * disiplin itu dipegang kedua pemanggilnya, dan {@code copySource(...)} yang
	 * mendahulukan kas kecil akan mengabaikan kas besar bila keduanya terlanjur
	 * terisi.</p>
	 *
	 * <p>Penjaga masuk menolak {@code session} {@code null}, {@code ref} kosong, dan
	 * — yang paling penting — {@code workspace} {@code null}. Syarat terakhir
	 * mencegah baris serapan tanpa baris pagu: elemen formula yang anggarannya tidak
	 * dapat ditentukan sekadar dilewati, bukan disimpan sebagai baris yatim. Nilai
	 * {@code null} dinormalkan menjadi {@code 0.0} sehingga kolom nilai tidak pernah
	 * kosong untuk baris kas.</p>
	 *
	 * @param session   sesi Hibernate
	 * @param ref       kunci alami baris, berformat {@code <key>_KAS_KECIL_<id>} atau
	 *                  {@code <key>_KAS_BESAR_<id>}
	 * @param kasKecil  dokumen kas kecil sumber, atau {@code null}
	 * @param kasBesar  dokumen kas besar sumber, atau {@code null}
	 * @param workspace baris pagu yang diserap; wajib, {@code null} membatalkan
	 * @param nilai     nilai serapan; {@code null} diperlakukan sebagai {@code 0.0}
	 */
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

	/**
	 * Membangun ulang seluruh baris penggunaan anggaran milik sebuah dokumen kas
	 * kecil: hapus semuanya, lalu tulis kembali dari formula JSON-nya.
	 *
	 * <h4>Strategi hapus-lalu-tulis-ulang</h4>
	 *
	 * <p>Method dibuka dengan DELETE native atas seluruh baris yang
	 * {@code kas_kecil}-nya sama dengan dokumen ini, di dalam transaksinya sendiri
	 * yang langsung di-commit. Baru setelah itu formula diuraikan dan tiap elemennya
	 * ditulis ulang lewat {@link #saveKasLine(Session, String, KasKecil, KasBesar, Workspace, Double)}.
	 * Pendekatan ini dipilih karena baris formula dapat <i>dihapus</i> pengguna —
	 * penulisan idempoten per-ref saja tidak akan pernah membuang baris yang tidak
	 * lagi ada di formula, sehingga serapannya akan tertinggal selamanya.</p>
	 *
	 * <p><b>Konsekuensi yang perlu disadari.</b> Karena penghapusan di-commit lebih
	 * dulu dan penulisan ulang berjalan sesudahnya sebagai rangkaian transaksi
	 * terpisah, terdapat jendela waktu ketika serapan dokumen kas ini <b>hilang
	 * seluruhnya</b> dari laporan realisasi. Bila penguraian formula gagal di
	 * tengah jalan, sebagian baris tidak akan tertulis kembali sampai dokumen
	 * disimpan ulang. DELETE ini pula yang menjadi asal-usul
	 * {@code StaleStateException} yang ditangani
	 * {@link #isStaleStateException(Throwable)}: proses lain yang sedang memegang
	 * objek baris tersebut di cache sesinya baru menyadari barisnya lenyap saat
	 * UPDATE dijalankan.</p>
	 *
	 * <h4>Penentuan baris pagu tiap elemen</h4>
	 *
	 * <p>Elemen tanpa {@code key} atau tanpa {@code workspace} dilewati. Anggaran
	 * mula-mula di-resolve langsung dari id pada atribut {@code workspace}. Bila
	 * hasilnya kosong sementara elemen menyebut {@code akun} biaya, dilakukan
	 * pencarian cadangan dua tahap terhadap {@link Workspace} pada tahun yang diambil
	 * dari tanggal dokumen kas: pertama berdasarkan objek {@link Akun} yang sama,
	 * lalu — bila tetap tak ketemu atau yang ketemu tidak aktif — berdasarkan
	 * <i>kode</i> akun. Keduanya menerima anggaran yang {@code aktif}-nya
	 * {@code true} atau {@code null}, diurutkan {@code id} menurun dan diambil satu.
	 * Pencarian berbasis kode itu penting untuk anggaran tahun berjalan yang dibuat
	 * ulang tiap tahun sehingga objek akunnya berbeda meski kodenya sama.</p>
	 *
	 * <p>Ref tiap baris dibentuk {@code <key>_KAS_KECIL_<id dokumen>} — id dokumen
	 * disertakan agar {@code key} yang kebetulan sama pada dua dokumen kas berbeda
	 * tidak bertabrakan.</p>
	 *
	 * <p><b>Perbedaan dari {@code prosesKasBesar(...)}.</b> Kedua method hampir
	 * kembar, dengan satu beda yang disengaja: di sini pencarian cadangan
	 * <i>tidak</i> menyertakan syarat {@code carryOver}, sedangkan pada kas besar
	 * anggaran ber-{@code carryOver} ikut diterima. Bila salah satu method disunting,
	 * periksa apakah perubahan yang sama berlaku untuk kembarannya.</p>
	 *
	 * <p>Kegagalan pada tahap penghapusan maupun penguraian ditelan dan dicatat lewat
	 * {@code ErrorAuditUtil}; method tidak pernah melempar ke pemanggil.</p>
	 *
	 * @param kasKecil dokumen kas kecil yang baru disimpan; {@code null} atau belum
	 *                 ber-id diabaikan
	 * @param session  sesi Hibernate; {@code null} diabaikan
	 */
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

	/**
	 * Membangun ulang seluruh baris penggunaan anggaran milik sebuah dokumen kas
	 * besar: hapus semuanya, lalu tulis kembali dari formula JSON-nya.
	 *
	 * <p>Kembaran {@link #prosesKasKecil(KasKecil, Session)} dengan alur yang sama
	 * persis — DELETE native seluruh baris milik dokumen di dalam transaksi
	 * tersendiri, lalu penguraian formula {@code JSONArray} dan penulisan ulang tiap
	 * elemen lewat
	 * {@link #saveKasLine(Session, String, KasKecil, KasBesar, Workspace, Double)}
	 * dengan ref {@code <key>_KAS_BESAR_<id dokumen>}. Seluruh catatan pada method
	 * kembarannya berlaku di sini, termasuk jendela waktu ketika serapan dokumen ini
	 * hilang seluruhnya dari laporan, dan perannya sebagai asal-usul
	 * {@code StaleStateException} yang ditangani jalur percobaan ulang.</p>
	 *
	 * <p><b>Satu perbedaan yang disengaja.</b> Pencarian cadangan {@link Workspace}
	 * di sini menerima anggaran ber-{@code carryOver} bernilai {@code true}
	 * <i>selain</i> anggaran yang aktif — {@code Restrictions.or(eq("carryOver", true),
	 * or(isNull("aktif"), eq("aktif", true)))} — sedangkan pada kas kecil syarat
	 * {@code carryOver} tidak ada. Ini mencerminkan kenyataan bahwa pengeluaran kas
	 * besar kerap membebani anggaran luncuran dari tahun sebelumnya. Bila salah satu
	 * dari kedua method disunting, periksa apakah perubahan yang sama perlu berlaku
	 * di kembarannya.</p>
	 *
	 * <p>Perhatikan pula bahwa {@link #getAktif()} tidak memiliki cabang untuk kas
	 * besar, sehingga penonaktifan dokumen kas besar tidak dengan sendirinya
	 * mematikan baris-barisnya; pembersihan sepenuhnya bergantung pada method ini
	 * dijalankan ulang.</p>
	 *
	 * <p>Kegagalan ditelan dan dicatat lewat {@code ErrorAuditUtil}; method tidak
	 * pernah melempar ke pemanggil.</p>
	 *
	 * @param kasBesar dokumen kas besar yang baru disimpan; {@code null} atau belum
	 *                 ber-id diabaikan
	 * @param session  sesi Hibernate; {@code null} diabaikan
	 */
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

	/**
	 * Penyalur (dispatcher) yang menyegarkan proyeksi penggunaan anggaran untuk satu
	 * dokumen sumber, memakai sesi Hibernate yang diberikan.
	 *
	 * <h4>Tiga jalur</h4>
	 *
	 * <ol>
	 *   <li><b>Kas kecil / kas besar</b> disaring lebih dulu dan diserahkan ke
	 *       {@link #prosesKasKecil(KasKecil, Session)} atau
	 *       {@link #prosesKasBesar(KasBesar, Session)}, yang membangun ulang seluruh
	 *       baris dokumen dari formula JSON-nya.</li>
	 *   <li><b>Enam jenis dokumen lain</b> melewati gerbang kelayakan
	 *       {@code createPenggunaanAnggaranSource(...)}, kemudian kunci alaminya
	 *       dibentuk {@link #refData(PenggunaanAnggaran)} dan diserahkan ke
	 *       {@code saveOrUpdateByRef(...)}. Bila kunci alami tak dapat dibentuk,
	 *       method berhenti tanpa menulis apa pun.</li>
	 *   <li><b>Dokumen yang ditolak gerbang</b> ({@code createPenggunaanAnggaranSource}
	 *       mengembalikan {@code null}) umumnya berhenti begitu saja — kecuali satu
	 *       kasus khusus di bawah.</li>
	 * </ol>
	 *
	 * <h4>Lapis ketiga penjaga potong-anggaran ganda</h4>
	 *
	 * <p>Kasus khusus itu menyangkut {@link UangMuka} berbasis permintaan pengadaan.
	 * Uang muka semacam itu ditolak gerbang karena PR-nya sudah memotong anggaran.
	 * Namun sekadar menolak tidaklah cukup: bila uang muka mula-mula dibuat
	 * <i>tanpa</i> PR — sehingga baris penggunaannya terlanjur tertulis — lalu
	 * disunting menjadi berbasis PR, baris lama akan tertinggal dan anggaran terpotong
	 * <b>dua kali</b> untuk satu pengeluaran. Karena itu, khusus untuk uang muka yang
	 * ditolak gerbang <i>karena</i> merujuk PR, method ini menjalankan
	 * {@code delete from PenggunaanAnggaran where uangMuka.id = :um} untuk
	 * membersihkan sisa baris tersebut. Ini lapis ketiga dari penjaga yang sama —
	 * lapis pertama {@code createPenggunaanAnggaranSource(...)} yang mencegah
	 * pembuatan, lapis kedua {@link #getAktif()} yang membuat baris lama dihapus saat
	 * disimpan ulang lewat jalur biasa. Kegagalan penghapusan ditelan dan dicatat
	 * tersendiri agar tidak menggagalkan sisa proses.</p>
	 *
	 * <p><b>Cakupan penjaga ini terbatas pada satu kasus.</b> Ia menutup jalur
	 * potong-ganda uang muka&ndash;PR, bukan potong-ganda secara umum: tidak ada
	 * pemeriksaan bahwa dua dokumen berbeda membebani pagu yang sama melebihi
	 * kapasitasnya, dan sekali lagi tidak ada pembandingan terhadap pagu di titik mana
	 * pun.</p>
	 *
	 * <p>Seluruh badan method dibungkus {@code try/catch} yang mengosongkan sesi lalu
	 * mencatat kesalahan; method tidak pernah melempar ke pemanggil. Bersifat publik
	 * dan menerima sesi dari luar sehingga dapat dipakai alur sinkron atau perkakas
	 * pemulihan data, berbeda dari {@link #simpan(Serializable)} yang selalu
	 * asinkron.</p>
	 *
	 * @param serializable dokumen sumber yang baru disimpan; {@code null} diabaikan
	 * @param session      sesi Hibernate yang dipakai menulis; {@code null} diabaikan
	 */
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

	
	/**
	 * Titik masuk publik yang menjadwalkan penyegaran proyeksi penggunaan anggaran
	 * secara <b>asinkron</b> setelah sebuah dokumen sumber disimpan.
	 *
	 * <h4>Siapa yang memanggil</h4>
	 *
	 * <p>Dipanggil dari {@code ais.database.hibernate.AuditListener} untuk setiap
	 * entitas yang disimpan aplikasi. Method ini sendiri yang menyaring: hanya
	 * delapan jenis dokumen yang diproses ({@link UangMuka},
	 * {@link PermintaanPengadaanMasterAssetDetail},
	 * {@link SaldoAwalMasterAssetDetail}, {@link PembayaranGaji}, {@link KasKecil},
	 * {@link KasBesar}, {@link GrupTransaksi}, {@link Pertangungjawaban}); jenis lain
	 * tidak menimbulkan efek apa pun. Penyaringan di depan ini penting karena
	 * listener memanggilnya sangat sering.</p>
	 *
	 * <h4>Rancangan asinkron dan seluruh konsekuensinya</h4>
	 *
	 * <p>Method membuat {@link Thread} baru, <b>menunggu 3 detik</b>, lalu membuka
	 * sesi Hibernate <i>native</i> sendiri lewat
	 * {@code HibernateUtil.currentNativeSession()} dan memanggil
	 * {@link #prosesSimpan(Serializable, Session)}. Jeda itu memberi waktu transaksi
	 * dokumen sumber untuk benar-benar ter-commit, sehingga sesi baru membaca keadaan
	 * final dokumen, bukan keadaan setengah jadi. Sesi ditutup pada blok
	 * {@code finally} lewat {@link #closeManualSession(Session)} — wajib, karena sesi
	 * native tidak dikelola kontainer dan kebocorannya akan menghabiskan kumpulan
	 * koneksi.</p>
	 *
	 * <p>Rancangan ini menentukan banyak hal yang harus dipahami sebelum menyunting
	 * kelas ini:</p>
	 * <ul>
	 *   <li><b>Tidak dapat menolak apa pun.</b> Karena berjalan setelah transaksi
	 *       sumber selesai, jalur ini secara arsitektural tidak berada pada posisi
	 *       untuk membatalkan penyimpanan dokumen. Menambahkan pemeriksaan pagu di
	 *       sini tidak akan mencegah pelampauan pagu — hanya akan membuat baris
	 *       serapannya tidak tercatat, yang justru memperburuk keadaan. Pencegahan
	 *       pagu, bila dikehendaki, harus dipasang di alur penyimpanan dokumen
	 *       sumbernya.</li>
	 *   <li><b>Kegagalan tak terlihat.</b> Seluruh kesalahan ditelan dan dicatat
	 *       lewat {@code ErrorAuditUtil}; pengguna yang menyimpan dokumen tidak
	 *       menerima peringatan apa pun.</li>
	 *   <li><b>Ada jendela ketidaksesuaian.</b> Selama sekitar tiga detik (lebih lama
	 *       bila basis data sibuk) dokumen sudah tersimpan sementara serapannya belum
	 *       tercatat; laporan RAB yang dibaca pada jendela itu menampilkan angka
	 *       lama.</li>
	 *   <li><b>Thread tanpa konteks.</b> Berjalan di luar sesi ZK, sehingga getter
	 *       yang bergantung pada pengguna aktif tidak dapat diandalkan — inilah
	 *       alasan setter {@link #setOleh(String)}/{@link #setOlehId(String)}
	 *       mengabaikan nilai kosong, dan alasan perbaikan pemuatan malas pada
	 *       {@link #getAktif()} diperlukan.</li>
	 *   <li><b>Thread tak terbatas.</b> Setiap penyimpanan dokumen yang relevan
	 *       membuat satu thread baru tanpa kumpulan (<i>thread pool</i>); pada impor
	 *       massal, jumlahnya sebanyak dokumen yang disimpan. Idempotensi berbasis
	 *       {@code ref} dan kunci advisory-lah yang menjaga agar penulisan bersamaan
	 *       tetap menghasilkan satu baris per ref.</li>
	 * </ul>
	 *
	 * <p>Parameter dideklarasikan {@code final} agar dapat ditangkap kelas dalam
	 * anonim {@link Runnable}-nya — persyaratan Java 1.6/1.7 yang menjadi target
	 * kompilasi berkas ini.</p>
	 *
	 * @param serializable dokumen sumber yang baru disimpan; jenis di luar delapan
	 *                     yang didukung diabaikan tanpa efek
	 * @see #prosesSimpan(Serializable, Session)
	 */
	public static void simpan(final Serializable serializable) {

		if (serializable instanceof UangMuka || serializable instanceof PermintaanPengadaanMasterAssetDetail
				|| serializable instanceof SaldoAwalMasterAssetDetail || serializable instanceof PembayaranGaji
				|| serializable instanceof KasKecil || serializable instanceof KasBesar
				|| serializable instanceof GrupTransaksi || serializable instanceof Pertangungjawaban) {
			new Thread(new Runnable() {

				/**
				 * Menunggu transaksi dokumen sumber ter-commit, lalu menyegarkan
				 * proyeksi penggunaan anggaran pada sesi native tersendiri yang
				 * ditutup manual di blok {@code finally}.
				 */
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