package ais.database.model.spmi;

// Generated Apr 16, 2010 2:27:16 PM by Hibernate Tools 3.2.4.CR1

import static javax.persistence.GenerationType.IDENTITY;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.envers.Audited;

import ais.database.model.GeneralValueObject;

/**
 * Entitas Hibernate yang memetakan tabel {@code public.jenis_spmi} pada modul
 * SPMI (Sistem Penjaminan Mutu Internal) perguruan tinggi. Merepresentasikan
 * <b>jenis instrumen SPMI</b> — simpul paling atas hierarki master data mutu
 * (mis. "Lembar Kerja AMI" / Audit Mutu Internal, lihat konstruksi contoh
 * pada {@link #initDataAmi}) yang menaungi seluruh {@link StandarSPMI}
 * (24 standar mutu Dikti), yang masing-masing dijabarkan lebih rinci menjadi
 * {@link ButirMutuSPMI} (butir/kriteria), {@link IndikatorSPMI} (indikator
 * ketercapaian), dan {@link SkenarioSPMI} (skenario/langkah pemeriksaan bukti)
 * — seluruhnya membentuk rantai data master siklus PPEPP (Penetapan-
 * Pelaksanaan-Evaluasi-Pengendalian-Peningkatan): {@code JenisSPMI} &rarr;
 * {@link StandarSPMI} &rarr; {@link ButirMutuSPMI} &rarr; {@link IndikatorSPMI}
 * &rarr; {@link SkenarioSPMI}.
 *
 * <p>Sesi evaluasi konkret ({@link HasilSPMI}) menunjuk balik ke satu
 * {@code JenisSPMI} untuk menentukan instrumen mana yang dipakai pada sesi
 * audit tersebut. {@code JenisSPMI} sendiri tidak memiliki kolom tenant
 * (perguruan tinggi) — ia adalah data master/rujukan yang dapat dipakai
 * bersama oleh sesi evaluasi dari institusi mana pun; pemisahan lingkup
 * organisasi ditentukan di level {@link HasilSPMI} lewat referensi
 * {@code perguruanTinggi}/{@code fakultas}/{@code jurusan}-nya.</p>
 *
 * <p>Kelas ini juga membawa dua method statis ({@link #initData(Session)}
 * dan {@link #initDataAmi(Session, JenisSPMI)}) yang merupakan skrip
 * <i>seed data</i> satu kali pakai — berisi hardcode teks lengkap 24 Standar
 * SPMI Dikti beserta butir/indikator/skenarionya untuk instrumen "Lembar
 * Kerja AMI" — dan method privat {@link #populate(String)} sebagai parser
 * pembantu format data seed tersebut. Lihat javadoc masing-masing method
 * untuk rincian.</p>
 */
@Entity
@org.hibernate.annotations.Entity(
    dynamicInsert = true,
    dynamicUpdate = true
)
@Audited
@Table(schema = "public", name = "jenis_spmi")
public class JenisSPMI extends GeneralValueObject {

	/**
	 * Nomor versi serialisasi tetap untuk kontrak {@link java.io.Serializable}.
	 * Nilai literal ini disalin dari template hbm2java bersama entitas SPMI
	 * lain di paket ini (bukan dihitung ulang per kelas).
	 */
	private static final long serialVersionUID = 2463821577548439808L;
	private Long id;
	private String oleh;
	private String olehId;

	/**
	 * @return nilai mentah kolom audit shadow {@code olehId} (identitas
	 *         pengguna yang terakhir menyimpan/mengubah baris ini), atau
	 *         {@code null} bila belum pernah diisi.
	 */
	public String getOlehId() {
		return olehId;
	}

	/**
	 * Menyimpan identitas pengguna (kolom audit shadow {@code olehId}). Setter
	 * ini sengaja mengabaikan nilai {@code null} atau kosong (guard di baris
	 * pertama) — kebutuhan teknis (bukan bug): nilai yang sudah tercatat oleh
	 * interceptor audit tidak boleh tertimpa oleh panggilan berikutnya yang
	 * membawa nilai kosong/null.
	 *
	 * @param olehId identitas pengguna; diabaikan bila null/kosong
	 */
	public void setOlehId(String olehId) {if (olehId == null || olehId.trim().isEmpty()) {return;}
		this.olehId = olehId;
	}

	/**
	 * Menyimpan nama pengguna (kolom audit shadow {@code oleh}), dengan guard
	 * yang sama seperti {@link #setOlehId(String)}.
	 *
	 * @param oleh nama pengguna; diabaikan bila null/kosong
	 */
	public void setOleh(String oleh) {if (oleh == null || oleh.trim().isEmpty()) {return;}
		this.oleh = oleh;
	}

	/**
	 * @return nama pengguna yang tercatat pada kolom audit shadow {@code oleh},
	 *         atau {@code null} bila belum pernah diisi.
	 */
	public String getOleh() {
		return oleh;
	}

	/**
	 * Hook JPA {@code @PreUpdate}: dipanggil otomatis oleh provider persistence
	 * sesaat sebelum baris ini di-{@code UPDATE}, mendelegasikan pencatatan
	 * timestamp perubahan ke {@link ais.database.hibernate.AuditTimestampInterceptor#ubah(Object)}.
	 * Bukan API publik — tidak dipanggil manual dari kode aplikasi.
	 */
	@javax.persistence.PreUpdate protected void onUpdate() { ais.database.hibernate.AuditTimestampInterceptor.ubah(this);}     private Date tanggal_dirubah = ais.ui.util.WaktuUtil.getDate();

	/**
	 * @param tanggal_dirubah timestamp perubahan terakhir; biasanya diisi
	 *                        otomatis oleh {@link #onUpdate()}.
	 */
	public void setTanggal_dirubah(Date tanggal_dirubah) {
		this.tanggal_dirubah = tanggal_dirubah;
	}

	/**
	 * @return timestamp terakhir kali baris ini diubah, diinisialisasi ke
	 *         waktu saat objek dibuat dan diperbarui otomatis oleh
	 *         {@link #onUpdate()} saat baris diperbarui di database.
	 */
	@Temporal(TemporalType.TIMESTAMP)
	public Date getTanggal_dirubah() {
		return tanggal_dirubah;
	}

	/**
	 * @return representasi ringkas berupa {@code id + "-" + nama}, dipakai
	 *         untuk log/debug dan tampilan singkat, bukan identitas bisnis.
	 */
	public String toString() {
		return id + "-" + nama;
	}

	/** Kode singkat jenis SPMI ini; kolom terpisah dari {@code nama}, tidak wajib diisi. */
	private String kode;

	/** Nama/judul jenis instrumen SPMI ini (mis. "Lembar Kerja AMI"). */
	private String nama;
	/** Keterangan/deskripsi tambahan bagi jenis SPMI ini. */
	private String keterangan;
	/** Flag aktif/nonaktif (soft delete); {@code null} diperlakukan sebagai aktif oleh {@link #getAktif()}. */
	private Boolean aktif;

	/**
	 * Skrip <i>seed data</i> satu kali pakai untuk mengisi instrumen "Lembar
	 * Kerja AMI" (Audit Mutu Internal) beserta seluruh rantai standar/butir/
	 * indikator/skenarionya. <b>Method ini sengaja dinonaktifkan permanen</b>
	 * lewat {@code if (true) { return; }} di baris pertama — seluruh kode di
	 * bawahnya adalah dead code yang tidak akan pernah dieksekusi selama
	 * guard tersebut tidak dihapus. Pola "guard permanen" ini adalah cara
	 * pengembang menonaktifkan skrip migrasi/seed satu kali pakai tanpa
	 * menghapus riwayat/isi kodenya (mis. untuk referensi format data atau
	 * bila suatu saat perlu dijalankan ulang secara sengaja dengan menghapus
	 * baris guard-nya secara manual) — bukan bug, tapi juga bukan sesuatu
	 * yang dipanggil dari alur aplikasi normal manapun.
	 *
	 * <p>Bila guard dilepas, method ini akan: (1) menghapus <b>seluruh</b> isi
	 * tabel {@code skenario_spmi}, {@code indikator_spmi}, {@code butir_mutu_spmi},
	 * dan {@code jenis_spmi} lewat SQL {@code DELETE} mentah (tanpa syarat
	 * WHERE — menghapus data dari <i>seluruh</i> tenant/institusi, bukan hanya
	 * institusi tertentu); (2) memeriksa apakah tabel {@code jenis_spmi} benar-
	 * benar kosong ({@code count == 0}) setelah penghapusan; (3) bila kosong,
	 * membuat satu baris {@code JenisSPMI} baru bernama "Lembar Kerja AMI" lalu
	 * memanggil {@link #initDataAmi(Session, JenisSPMI)} untuk mengisi seluruh
	 * rantai data di bawahnya. Transaksi Hibernate dibuka dan di-{@code commit}
	 * secara manual per operasi save ({@code session.getTransaction().begin()}/
	 * {@code commit()}) — bukan dibungkus satu transaksi besar.
	 *
	 * @param session sesi Hibernate aktif yang dipakai untuk seluruh operasi
	 *                hapus dan simpan pada method ini (tidak pernah tereksekusi
	 *                selama guard {@code if (true) return;} masih ada)
	 */
	@SuppressWarnings("unused")
	public static void initData(Session session) {

		if (true) {
			return;
		}

		session.createSQLQuery("delete from skenario_spmi").executeUpdate();
		session.createSQLQuery("delete from indikator_spmi").executeUpdate();
		session.createSQLQuery("delete from butir_mutu_spmi").executeUpdate();
		session.createSQLQuery("delete from jenis_spmi").executeUpdate();

		int count = ((Number) session.createCriteria(JenisSPMI.class).setProjection(Projections.rowCount())
				.uniqueResult()).intValue();

		if (count == 0) {

			JenisSPMI AMI = new JenisSPMI();
			AMI.setNama("Lembar Kerja AMI");
			AMI.setKeterangan("INSTRUMEN AUDIT MUTU INTERNAL (AMI)");
			session.getTransaction().begin();
			session.save(AMI);
			session.getTransaction().commit();

			initDataAmi(session, AMI);

		}

	}

	/**
	 * Bagian kedua dari skrip <i>seed data</i> "Lembar Kerja AMI", dipanggil
	 * dari {@link #initData(Session)} (yang sendiri sudah dinonaktifkan
	 * permanen lewat guard {@code if (true) return;} — sehingga method ini
	 * pun tidak pernah tereksekusi lewat jalur pemanggilan normal manapun,
	 * kecuali dipanggil manual langsung, misalnya dari skrip migrasi/console
	 * terpisah). Method ini mengisi <b>empat</b> {@link StandarSPMI} pertama
	 * dari instrumen AMI ("STANDAR KOMPETENSI LULUSAN", "STANDAR DOSEN DAN
	 * TENAGA KEPENDIDIKAN", "STANDAR PENGELOLAAN PEMBELAJARAN", "STANDAR
	 * SARANA DAN PRASARANA") beserta seluruh {@link ButirMutuSPMI},
	 * {@link IndikatorSPMI}, dan {@link SkenarioSPMI} di bawahnya — bukan
	 * ke-24 standar SPMI Dikti secara lengkap; hanya sebagian kecil yang
	 * dihardcode di sini sebagai contoh/starter data.
	 *
	 * <p><b>Format data sumber:</b> untuk tiap standar, tiga blok teks
	 * multi-baris disiapkan secara paralel dan saling berkorespondensi lewat
	 * posisi/nomor urut:</p>
	 * <ul>
	 * <li>{@code bb}/{@code bss} — array/teks berisi daftar butir mutu, satu
	 *     butir per baris, diawali nomor urut manual (mis. "1.\tKetua Sekolah
	 *     Tinggi memastikan...").</li>
	 * <li>{@code Indikators} — teks dengan format baris {@code "<nomorButir>;;<teks indikator>"}
	 *     (dipisah {@code ";;"} sebagai delimiter nomor-vs-teks), diuraikan oleh
	 *     {@link #populate(String)} menjadi {@code Map<Integer, List<String>>}
	 *     yang mengelompokkan indikator berdasarkan nomor butir induknya.</li>
	 * <li>{@code buktis} — teks berisi daftar skenario/langkah pemeriksaan
	 *     bukti, satu skenario per baris, diasumsikan sejajar urutannya dengan
	 *     urutan gabungan seluruh indikator (bukan dikelompokkan per butir).</li>
	 * </ul>
	 *
	 * <p><b>Alur pengisian per standar</b> (diulang identik untuk keempat
	 * standar dengan data yang berbeda): standar disimpan lebih dulu
	 * ({@code session.save(s1)}, dst., masing-masing dalam transaksi sendiri);
	 * lalu untuk setiap baris butir mutu ({@code bb}/{@code bss}, dengan baris
	 * kosong dilewati lewat pengecekan {@code !b.trim().isEmpty()}), satu
	 * {@link ButirMutuSPMI} disimpan; kemudian untuk <b>setiap</b> entri di
	 * {@code dataIndikators} (bukan hanya entri milik butir yang sedang
	 * diproses — perhatikan bahwa loop bagian dalam mengiterasi seluruh
	 * {@code dataIndikators.keySet()} tanpa memfilter berdasarkan
	 * {@code nomor} butir saat ini), setiap {@link IndikatorSPMI} pada entri
	 * tersebut disimpan dan ditautkan ke {@code butirMutuSPMI} yang sedang
	 * diproses; untuk tiap indikator yang baru disimpan, satu
	 * {@link SkenarioSPMI} diambil secara berurutan dari {@code buktisData}
	 * memakai indeks global {@code totalIndex} (bukan indeks per butir) lalu
	 * disimpan dan ditautkan ke indikator tersebut. Setiap {@code save()}
	 * dibungkus transaksi Hibernate sendiri ({@code begin()}/{@code commit()}
	 * per baris, bukan satu transaksi besar per standar).
	 *
	 * <p><b>Penanganan galat:</b> penyimpanan {@link SkenarioSPMI} dibungkus
	 * {@code try/catch} yang menelan seluruh {@link Exception} (direkam lewat
	 * {@link ais.common.ErrorAuditUtil#record}, tanpa dilempar ulang) —
	 * mengantisipasi {@code buktisData[totalIndex]} keluar dari batas array
	 * bila jumlah baris {@code buktis} tidak persis sama dengan total jumlah
	 * indikator yang dihasilkan (rawan terjadi mengingat data disusun manual
	 * sebagai teks literal, bukan berpasangan eksplisit per indikator).</p>
	 *
	 * <p>Karena seluruh method ini tidak pernah tereksekusi dalam kondisi
	 * normal (dipanggil eksklusif dari {@link #initData(Session)} yang sudah
	 * di-guard mati), catatan di atas bersifat dokumentatif atas <i>desain</i>
	 * skrip seed ini, bukan peringatan atas perilaku yang sedang berjalan di
	 * produksi.</p>
	 *
	 * @param session sesi Hibernate aktif untuk seluruh operasi simpan pada method ini
	 * @param AMI     {@link JenisSPMI} induk (mis. "Lembar Kerja AMI") tempat
	 *                seluruh standar yang dibuat di sini ditautkan
	 */
	public static void initDataAmi(Session session, JenisSPMI AMI) {

		StandarSPMI s1 = new StandarSPMI();
		s1.setNama("STANDAR KOMPETENSI LULUSAN");
		s1.setJenisSPMI(AMI);
		s1.setNomorUrut(1);
		session.getTransaction().begin();
		session.save(s1);
		session.getTransaction().commit();

		String[] bb = new String[] {
				"1.	Ketua Sekolah Tinggi memastikan lulusan harus memiliki standar kompetensi yang mengacu pada kemampuan lulusan yang memiliki sikap, pengetahuan dan keterampilan yang dinyatakan dalam rumusan capaian pembelajaran lulusan",
				"2.	Ketua Sekolah Tinggi Lulusan harus memiliki specific softskill sebagai syarat kelulusan",
				"3.	Ketua Sekolah Tinggi wajib memastikan terdapat Pedoman Penilaian Kompetensi Lulusan dalam penentuan kelulusan mahasiswa Program Studi",
				"4.	Ketua Sekolah Tinggi menyusun laporan tracer study setiap akhir tahun ajar",
				"5.	Ketua Sekolah Tinggi memastikan mahasiswa menyelesaikan pembelajaran maksimal lima tahun dengan beban belajar paling sedikit 144 SKS baik yang dilaksanakan di dalam atau di luar program studi" };

		String[] in = new String[] {
				"Memiliki bukti tracer study yang memuat standar kompetensi yang mengacu kepada bidang softskill, hardskill dan keterampilan kemampuan lulusan.",
				"Persentase Tingkat Kesesuaian Bidang Kerja dengan Program Studi",
				"Ketersediaan dokumen Pedoman Penilaian Kompetensi Lulusan", "Ketersediaan Laporan Tracer Study",
				"Persentase lulusan program studi dengan masa studi kurang dari lima tahun dengan Beban Belajar lebih dari 144 sks pada satu tahun akademik." };

		String[] se = new String[] { "memeriksa laporan tracer study terkait dengan capaian kompetensi lulusan",
				"Memeriksa persentase Tingkat Kesesuaian Bidang Kerja dengan Program Studi pada Laporan Tracer Studi.",
				"memeriksa Ketersediaan dokumen Pedoman Penilaian Kompetensi Lulusan",
				"Memeriksa hasil Laporan Tracer Study sesuai dengan uraian minimal berupa: (1) rataan IPK Lulusan, (2) rataan masa studi lulusan, (3) persentase kelulusan tepat waktu, (4) waktu tunggu lulusan, (5) kesesuaian bidang kerja lulusan, (6) tingkat dan ukuran tempat kerja lulusan, (7) tingkat kepuasan pengguna lulusan, dan (8) analisis pemenuhan capaian pembelajaran lulusan (CPL)",
				"Memeriksa dokumen Tracer Study terkait lama masa studi, beban belajar lulusan program studi" };

		int nomor = 1;
		for (String b : bb) {

			ButirMutuSPMI butirMutuSPMI = new ButirMutuSPMI();
			butirMutuSPMI.setStandarSPMI(s1);
			butirMutuSPMI.setNomorUrut(nomor);
			butirMutuSPMI.setNama(b);
			butirMutuSPMI.setKeterangan(b);
			session.getTransaction().begin();
			session.save(butirMutuSPMI);
			session.getTransaction().commit();

			IndikatorSPMI indikatorSPMI = new IndikatorSPMI();
			indikatorSPMI.setNomorUrut(1);
			indikatorSPMI.setNama(in[nomor - 1]);
			indikatorSPMI.setKeterangan(in[nomor - 1]);
			indikatorSPMI.setButirMutuSPMI(butirMutuSPMI);

			session.getTransaction().begin();
			session.save(indikatorSPMI);
			session.getTransaction().commit();

			SkenarioSPMI skenarioSPMI = new SkenarioSPMI();
			skenarioSPMI.setNomorUrut(1);
			skenarioSPMI.setNama(se[nomor - 1]);
			skenarioSPMI.setKeterangan(se[nomor - 1]);
			skenarioSPMI.setIndikatorSPMI(indikatorSPMI);

			session.getTransaction().begin();
			session.save(skenarioSPMI);
			session.getTransaction().commit();

			nomor++;
		}

		StandarSPMI s2 = new StandarSPMI();
		s2.setNama("STANDAR DOSEN DAN TENAGA KEPENDIDIKAN");
		s2.setJenisSPMI(AMI);
		s2.setNomorUrut(2);
		session.getTransaction().begin();
		session.save(s2);
		session.getTransaction().commit();

		bb = new String[] {
				"1.	Ketua Sekolah Tinggi memastikan setiap dosen wajib sehat jasmani dan rohani, serta memiliki kualifikasi akademik dan kompetensi pendidik yang mendukung penyelenggaraan pendidikan",
				"2.	Ketua Sekolah Tinggi memastikan setiap dosen program studi harus minimal berkualifikasi akademik magister atau magister terapan yang relevan dengan program studi",
				"3.	Ketua Sekolah Tinggi memastikan jumlah dosen tetap program studi (DT) minimal  5 orang dengan bidang keahlian yang sesuai",
				"4.	Ketua Sekolah Tinggi memastikan dosen tetap program studi (DT) harus berstatus sebagai pendidik tetap pada 1 perguruan tinggi dan tidak menjadi pegawai tetap pada satuan kerja atau satuan pendidikan lain.",
				"5.	Ketua Sekolah Tinggi memastikan kualifikasi dosen sesuai dengan keahlian Program Studi (PS)",
				"6.	Ketua Sekolah Tinggi memastikan tenaga kependidikan memiliki kualifikasi akademik paling rendah program diploma 3 yang sesuai dengan kualifikasi tugas pokok dan fungsinya, termasuk didalamnya tenaga administrasi.",
				"7.	Ketua Sekolah Tinggi memastikan tenaga kependidikan yang  memerlukan keahlian khusus wajib memiliki sertifikat kompetensi sesuai dengan bidang tugas dan keahliannya." };

		String Indikators = "1;;1.	Ketersediaan dokumen keterangan sehat dosen program studi. \r\n"
				+ "1;;2.	Kelengkapan ijazah magister dan/atau doktoral dosen program studi yang relevan dengan program studi. \r\n"
				+ "1;;3.	Kelengkapan sertifikat pendidik dan/atau profesi\r\n"
				+ "2;;1.	 Kelengkapan ijazah magister atau doktoral dosen program studi. \r\n"
				+ "2;;2.	Seluruh dosen program studi harus memiliki bidang keahlian yang serumpun dengan program studi\r\n"
				+ "3;;Jumlah dosen tetap program studi (DT) program studi minimal 5 orang.\r\n"
				+ "4;;Kesesuaian NIDN (Nomor Induk Dosen Nasional) dan nama perguruan tinggi dosen tetap program studi (DT) \r\n"
				+ "5;;Persentase dosen yang sesuai dengan kualifikasi keilmuan program studi\r\n"
				+ "6;;1.	Ketersediaan ijazah minimal diploma 3 untuk seluruh tenaga kependidikan di program studi. \r\n"
				+ "6;;2.	Kesesuaian ijazah tenaga kependidikan dengan tugas pokok dan fungsinya\r\n"
				+ "6;;Jumlah tendik berkualifikasi D3\r\n" + "6;;Jumlah tendik berkualifikasi S1\r\n"
				+ "6;;Jumlah tendik berkualifikasi S2\r\n"
				+ "7;;1.	Setiap laboratorium program studi memiliki minimal satu orang laboran. \r\n"
				+ "7;;2.	Kesesuaian kualifikasi dan sertifikat kompetensi laboran yang sesuai dengan bidang tugasnya\r\n"
				+ "7;;3.	Persentase laboran dengan sertifikat kompetensi yang sesuai dengan bidang tugasnya";

		String buktis = "memeriksa dokumen lamaran dosen terkait keterangan sehat. \r\n"
				+ "memeriksa kelengkapan ijazah setiap dosen program studi.\r\n"
				+ "memeriksa kelengkapan sertifikat pendidik dan/atau profesi\r\n"
				+ "Memeriksa kelengkapan ijazah setiap dosen program studi. \r\n"
				+ "memeriksa bidang keahlian setiap dosen pada ijazah dan transkrip pendidikan magister dan/atau doktoral dosen program studi\r\n"
				+ "Memeriksa SK Penetapan Dosen Pengampu Mata Kuliah Program Studi\r\n"
				+ "memeriksa status dosen tetap program studi melalui laman Pangkalan Data Pendidikan Tinggi Dirjen Pendidikan Tinggi (pddikti.kemendikbud.go.id) PD Dikti (kepemilikan NIDN dan perguruan tinggi)\r\n"
				+ "\r\n" + "Memeriksa Ijazah S2 dan S3 dosen tetap program studi\r\n"
				+ "memeriksa notulensi rapat Penilaian Kecukupan Tenaga Kependidikan terkait jenjang pendidikan, kesesuaian ijazah dengan jabatan, sertifikat keahlian, kesesuaian ijazah dan sertifikat keahlian dengan tugas pokok dan fungsinya tenaga kependidikan\r\n"
				+ "\r\n" + "\r\n" + "\r\n" + "\r\n" + "\r\n" + "\r\n" + "\r\n" + "";

		String[] buktisData = StringUtils.split(buktis, "\r\n");

		Map<Integer, List<String>> dataIndikators = populate(Indikators);

		nomor = 1;
		int totalIndex = 0;
		for (String b : bb) {
			if (!b.trim().isEmpty()) {
				ButirMutuSPMI butirMutuSPMI = new ButirMutuSPMI();
				butirMutuSPMI.setStandarSPMI(s2);
				butirMutuSPMI.setNomorUrut(nomor);
				butirMutuSPMI.setNama(b);
				butirMutuSPMI.setKeterangan(b);
				session.getTransaction().begin();
				session.save(butirMutuSPMI);
				session.getTransaction().commit();

				for (Integer no : dataIndikators.keySet()) {
					List<String> stringsData = dataIndikators.get(no);

					int noLocal = 1;
					for (String dataLocal : stringsData) {
						IndikatorSPMI indikatorSPMI = new IndikatorSPMI();
						indikatorSPMI.setNomorUrut(noLocal);
						indikatorSPMI.setNama(dataLocal);
						indikatorSPMI.setKeterangan(dataLocal);
						indikatorSPMI.setButirMutuSPMI(butirMutuSPMI);

						session.getTransaction().begin();
						session.save(indikatorSPMI);
						session.getTransaction().commit();

						try {
							SkenarioSPMI skenarioSPMI = new SkenarioSPMI();
							skenarioSPMI.setNomorUrut(1);
							skenarioSPMI.setNama(buktisData[totalIndex]);
							skenarioSPMI.setKeterangan(buktisData[totalIndex]);
							skenarioSPMI.setIndikatorSPMI(indikatorSPMI);

							session.getTransaction().begin();
							session.save(skenarioSPMI);
							session.getTransaction().commit();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/spmi/JenisSPMI.java:265");
							// e.printStackTrace();
						}

						noLocal++;
						totalIndex++;
					}
				}
				nomor++;
			}
		}

		StandarSPMI s3 = new StandarSPMI();
		s3.setNama("STANDAR PENGELOLAAN PEMBELAJARAN");
		s3.setJenisSPMI(AMI);
		s3.setNomorUrut(3);
		session.getTransaction().begin();
		session.save(s3);
		session.getTransaction().commit();

		String bss = "1.	Ketua Sekolah Tinggi wajib melakukan kegiatan sistemik yang menciptakan  suasana akademik dan budaya mutu yang baik\r\n"
				+ "2.	Sekolah Tinggi wajib menjaga dan meningkatkan mutu pengelolaan Program Studi dalam melaksanakan program Pembelajaran secara berkelanjutan dengan sasaran yang sesuai dengan visi dan misi Perguruan Tinggi\r\n"
				+ "3.	Sekolah Tinggi wajib melakukan pemantauan dan evaluasi terhadap kegiatan Program Studi dalam melaksanakan kegiatan Pembelajaran\r\n"
				+ "4.	Sekolah Tinggi wajib memiliki sistem perencanaan, pelaksanaan, evaluasi, pengawasan, penjaminan mutu, dan pengembangan kegiatan Pembelajaran dan Dosen\r\n"
				+ "5.	Ketua Sekolah Tinggi memastikan Sistem Tata Pamong yang digunakan bersifat Kredibel, Transparan, Akuntabel, Bertanggung Jawab, dan Adil\r\n"
				+ "6.	Ketua Sekolah Tinggi memastikan tersedianya dokumen pengelolaan Fungsional  dan Operasional Sekolah Tinggi dan Program Studi\r\n"
				+ "7.	Ketua Sekolah Tinggi wajib melakukan pengelolaan kerja sama\r\n"
				+ "8.	Ketua Sekolah Tinggi wajib menetapkan rencana program kerja Sekolah Tinggi";

		bb = StringUtils.split(bss, "\r\n");

		Indikators = "1;;Ketersediaan kalender akademik. \r\n"
				+ "1;;Program Studi menyelenggarakan kuliah umum yang dapat mendukung pengembangan keilmuan program studi pada setiap semester. \r\n"
				+ "1;;Program studi melalui Himpunan Mahasiswa mengadakan kegiatan penelitian dan pengabdian yang terkait dengan pengembangan keilmuan program studi pada setiap semester\r\n"
				+ "2;;Ketersediaan dokumen terkait perencanaan peningkatan mutu pengelolaan program studi\r\n"
				+ "3;;Ketersediaan dokumen terkait pemantauan dan evaluasi pelaksanaan kegiatan pembelajaran program studi\r\n"
				+ "4;;Ketersediaan unit pelaksana tugas terkait perencanaan, pelaksanaan, evaluasi, pengawasan, penjaminan mutu, dan pengembangan kegiatan Pembelajaran dan Dosen\r\n"
				+ "4;;Ketersediaan panduan perencanaan, pelaksanaan, evaluasi, pengawasan, penjaminan mutu, dan pengembangan kegiatan Pembelajaran dan Dosen\r\n"
				+ "5;;Ketersediaan mekanisme dari pengangkatan personalia pelaksanaan untuk seluruh organ Sekolah Tinggi (Kredibel)\r\n"
				+ "5;;Ketersediaan mekanisme sosialisasi dalam setiap proses pelaksanaan operasional dan fungsional Sekolah Tinggi dan Program Studi (Transparan)\r\n"
				+ "5;;Ketersediaan mekanisme pengukuran indeks kinerja dari Sekolah Tinggi (Akuntabilitas)\r\n"
				+ "5;;Ketersediaan tugas pokok dan fungsi setiap organ pelaksana di Sekolah Tinggi dan Program Studi (Bertanggung Jawab)\r\n"
				+ "5;;Ketersediaan mekanisme penerapan hak dan kewajiban sesuai dengan jabatan fungsional dan struktural tentang jam kerja dosen (Adil)\r\n"
				+ "6;;Ketersediaan dokumen capaian kinerja Sekolah Tinggi dan Program Studi\r\n"
				+ "6;;Ketersediaan dokumen Realisasi anggaran Sekolah Tinggi\r\n"
				+ "6;;Ketersediaan SOP terkait pengelolaan fungsional dan operasional Sekolah Tinggi dan Program Studi\r\n"
				+ "7;;Ketersediaan dokumen SOP Kerja Sama\r\n" + "7;;Jumlah kerja sama dengan pemerintahan \r\n"
				+ "7;;Jumlah kerja sama dengan perusahaan\r\n" + "7;;Jumlah kerja sama dengan institusi\r\n"
				+ "7;;Jumlah kerja sama dengan perguruan tinggi\r\n" + "7;;Jumlah kerja sama dengan rumah sakit\r\n"
				+ "7;;Jumlah kerja sama dengan UMKM\r\n" + "7;;Persentase MoU yang ditindaklanjuti menjadi MoA\r\n"
				+ "7;;Jumlah implementasi kerjasama di tingkat program studi\r\n"
				+ "8;;Ketersediaan rencana program kerja Sekolah Tinggi";

		dataIndikators = populate(Indikators);

		buktis = "memeriksa ketersediaan kalender akademik\r\n" + "memeriksa rekap kegiatan kuliah umum \r\n"
				+ "memeriksa realisasi kerja himpunan mahasiswa program studi\r\n"
				+ "Memeriksa dokumen terkait perencanaan peningkatan mutu pengelolaan program studi\r\n"
				+ "memeriksa ketersediaan dokumen monev\r\n"
				+ "memeriksa SK Penugasan Unit terkait perencanaan, pelaksanaan, evaluasi, pengawasan, penjaminan mutu, dan pengembangan kegiatan Pembelajaran dan Dosen\r\n"
				+ "memeriksa dokumen panduan terkait perencanaan, pelaksanaan, evaluasi, pengawasan, penjaminan mutu, dan pengembangan kegiatan Pembelajaran dan Dosen\r\n"
				+ "memeriksa dokumen terkait mekanisme dari pengangkatan personalia pelaksanaan untuk seluruh organ Sekolah Tinggi \r\n"
				+ "memeriksa dokumen terkait mekanisme sosialisasi dalam setiap proses pelaksanaan operasional dan fungsional Sekolah Tinggi dan Program Studi \r\n"
				+ "memeriksa dokumen terkait mekanisme pengukuran indeks kinerja dari Sekolah Tinggi \r\n"
				+ "memeriksa dokumen terkait tugas pokok dan fungsi setiap organ pelaksana di Sekolah Tinggi dan Program Studi\r\n"
				+ "memeriksa dokumen terkait mekanisme penerapan hak dan kewajiban sesuai dengan jabatan fungsional dan struktural tentang jam kerja dosen (Adil)\r\n"
				+ "memeriksa dokumen capaian kinerja Sekolah Tinggi dan Program Studi\r\n"
				+ "memeriksa dokumen Realisasi anggaran Sekolah Tinggi\r\n"
				+ "memeriksa SOP terkait pengelolaan fungsional dan operasional Sekolah Tinggi dan Program Studi\r\n"
				+ "memeriksa dokumen SOP Kerja Sama\r\n" + "memeriksa jumlah kerja sama dengan pemerintahan \r\n"
				+ "memeriksa jumlah kerja sama dengan perusahaan\r\n"
				+ "memeriksa jumlah kerja sama dengan institusi\r\n"
				+ "memeriksa jumlah kerja sama dengan perguruan tinggi\r\n"
				+ "memeriksa jumlah kerja sama dengan rumah sakit\r\n" + "memeriksa jumlah kerja sama dengan UMKM\r\n"
				+ "memeriksa dokumen MoU yang ditindaklanjuti menjadi MoA\r\n"
				+ "memeriksa bukti implementasi kerja sama. Bentuk implementasi: "
				+ "1) pengembangan kurikulum bersama; "
				+ "2) menyediakan kesempatan pembelajaran berbasis proyek (PBL); "
				+ "3) menyediakan program magang paling sedikit 1 (satu) semester penuh; "
				+ "4) menyediakan kesempatan kerja bagi lulusan; "
				+ "5) mengisi kegiatan pembelajaran dengan dosen tamu praktisi; "
				+ "6) menyediakan pelatihan (upskilling dan reskilling) bagi dosen maupun instruktur; "
				+ "7) menyediakan resource sharing sarana dan prasarana; "
				+ "8) menyelenggarakan teaching factory (TEFA) di kampus; "
				+ "9) menyelenggarakan program double degree atau joint degree;  "
				+ "10) melakukan kemitraan penelitian." + "Memeriksa rencana program kerja Sekolah Tinggi";

		buktisData = StringUtils.split(buktis, "\r\n");

		nomor = 1;
		totalIndex = 0;
		for (String b : bb) {
			if (!b.trim().isEmpty()) {
				ButirMutuSPMI butirMutuSPMI = new ButirMutuSPMI();
				butirMutuSPMI.setStandarSPMI(s3);
				butirMutuSPMI.setNomorUrut(nomor);
				butirMutuSPMI.setNama(b);
				butirMutuSPMI.setKeterangan(b);
				session.getTransaction().begin();
				session.save(butirMutuSPMI);
				session.getTransaction().commit();

				for (Integer no : dataIndikators.keySet()) {
					List<String> stringsData = dataIndikators.get(no);

					int noLocal = 1;
					for (String dataLocal : stringsData) {
						IndikatorSPMI indikatorSPMI = new IndikatorSPMI();
						indikatorSPMI.setNomorUrut(noLocal);
						indikatorSPMI.setNama(dataLocal);
						indikatorSPMI.setKeterangan(dataLocal);
						indikatorSPMI.setButirMutuSPMI(butirMutuSPMI);

						session.getTransaction().begin();
						session.save(indikatorSPMI);
						session.getTransaction().commit();

						try {
							SkenarioSPMI skenarioSPMI = new SkenarioSPMI();
							skenarioSPMI.setNomorUrut(1);
							skenarioSPMI.setNama(buktisData[totalIndex]);
							skenarioSPMI.setKeterangan(buktisData[totalIndex]);
							skenarioSPMI.setIndikatorSPMI(indikatorSPMI);

							session.getTransaction().begin();
							session.save(skenarioSPMI);
							session.getTransaction().commit();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/spmi/JenisSPMI.java:392");
							// e.printStackTrace();
						}

						noLocal++;
						totalIndex++;
					}
				}
				nomor++;
			}
		}

		StandarSPMI s4 = new StandarSPMI();
		s4.setNama("STANDAR SARANA DAN PRASARANA");
		s4.setJenisSPMI(AMI);
		s4.setNomorUrut(3);
		session.getTransaction().begin();
		session.save(s4);
		session.getTransaction().commit();

		bss = "1.	Ketua Sekolah Tinggi mempersiapkan Dokumen Pengembangan dan Pengelolaan Sarana dan Prasarana secara berkala\r\n"
				+ "2.	Ketua Sekolah Tinggi memastikan kecukupan, aksesibilitas dan mutu sarana dan prasarana untuk menjamin pencapaian capaian pembelajaran dan meningkatkan suasana akademik\r\n"
				+ "3.	Ketua Sekolah Tinggi memastikan aksesibilitas dalam sistem informasi dapat diakses melalui jaringan luas (WAN)\r\n"
				+ "4.	Ketua Sekolah Tinggi memastikan kelengkapan dan kemutakhiran pengelolaan sistem informasi Sekolah Tinggi\r\n"
				+ "5.	Ketua Sekolah Tinggi memastikan kepuasan kelengkapan Sistem TIK (Teknologi Informasi dan Komunikasi) untuk mengelola dan menyebarkan hasil kegiatan Penelitian\r\n"
				+ "6.	Ketua Sekolah Tinggi memastikan kepuasan kelengkapan Sistem TIK (Teknologi Informasi dan Komunikasi) untuk mengelola dan menyebarkan hasil kegiatan Pengabdian kepada Masyarakat\r\n"
				+ "7.	Ketua Sekolah Tinggi memastikan jenis layanan terintegrasi dalam sistem informasi dan digunakan untuk pengambilan keputusan di Sekolah Tinggi\r\n"
				+ "8.	Ketua Sekolah Tinggi memastikan ketersediaan lembaga penelitian di tingkat perguruan tinggi\r\n"
				+ "9.	Ketua Sekolah Tinggi memastikan ketersediaan laboratorium terkait dengan bidang kajian setiap prodi \r\n"
				+ "10.	Ketua Sekolah Tinggi memastikan ketersediaan kelompok riset di tingkat perguruan tinggi\r\n"
				+ "11.	Ketua Sekolah TInggi memastikan ketersediaan lembaga Pengabdian kepada Masyarakat (PkM) di tingkat Sekolah TInggi\r\n"
				+ "12.	Ketua memastikan tersedianya sarana dan prasarana pendukung kegiatan PkM yang memenuhi standar mutu, keselamatan & kesehatan kerja, kenyamanan dan keamanan\r\n"
				+ "13.	Ketua memastikan sarana dan prasarana pendukung kegiatan PkM dievaluasi penggunaannya setiap tahun";

		bb = StringUtils.split(bss, "\r\n");

		Indikators = "1;;Ketersediaan Dokumen Pengembangan dan Pengelolaan Sarana dan Prasarana Sekolah Tinggi\r\n"
				+ "2;;Persentase kecukupan sarana dan prasarana\r\n"
				+ "2;;Persentase aksesibilitas sarana dan prasarana\r\n" + "2;;Persentase mutu sarana dan prasarana\r\n"
				+ "3;;Persentase pengelolaan data Sekolah Tinggi melalui sistem informasi dan dapat diakses melalui jaringan luas (WAN)\r\n"
				+ "3;;Persentase kemudahan akses sistem informasi Sekolah Tinggi\r\n"
				+ "3;;Persentase efektivitas Sistem Teknologi Informasi dan Komunikasi \r\n"
				+ "3;;Ketersediaan layanan e-learning dan perpustakaan digital \r\n"
				+ "3;;Persentase kemudahan akses layanan e-learning dan perpustakaan digital \r\n"
				+ "3;;Ketersediaan dokumen evaluasi dan tindak lanjut penyempurnaan sistem informasi Sekolah Tinggi\r\n"
				+ "4;;Persentase kelengkapan dan kemutakhiran pengelolaan sistem informasi Sekolah Tinggi\r\n"
				+ "5;;Persentase kepuasan kelengkapan Sistem TIK (Teknologi Informasi dan Komunikasi) untuk mengelola dan menyebarkan hasil kegiatan Penelitian\r\n"
				+ "6;;Persentase kepuasan kelengkapan Sistem TIK (Teknologi Informasi dan Komunikasi) untuk mengelola dan menyebarkan hasil kegiatan Pengabdian kepada Masyarakat\r\n"
				+ "7;;Ketersediaan dokumen terkait pengambilan keputusan melalui sistem informasi terintegrasi di Sekolah Tinggi \r\n"
				+ "8;;Ketersediaan lembaga penelitian di tingkat perguruan tinggi\r\n"
				+ "8;;Ketersediaan ruang lembaga penelitian di tingkat perguruan tinggi\r\n"
				+ "9;;Jumlah laboratorium pendidikan terkait dengan bidang kajian program studi\r\n"
				+ "9;;Jumlah laboratorium riset terkait dengan bidang kajian program studi\r\n"
				+ "9;;Ketersediaan bukti legal laboratorium riset\r\n"
				+ "10;;Jumlah kelompok riset di tingkat perguruan tinggi\r\n"
				+ "10;;Ketersediaan bukti legal kelompok riset\r\n" + "10;;Jumlah luaran kelompok riset\r\n"
				+ "11;;Ketersediaan lembaga Pengabdian kepada Masyarakat di tingkat perguruan tinggi\r\n"
				+ "11;;Ketersediaan ruang lembaga Pengabdian kepada Masyarakat di tingkat perguruan tinggi\r\n"
				+ "12;;Ketersediaan sarana dan prasarana PkM yang memenuhi standar mutu, keselamatan & kesehatan kerja, kenyamanan dan keamanan\r\n"
				+ "13;;Laporan Evaluasi Sarana dan Prasarana PkM\r\n";

		dataIndikators = populate(Indikators);

		buktis = "Memeriksa ketersediaan Dokumen Pengembangan dan Pengelolaan Sarana dan Prasarana Sekolah Tinggi\r\n"
				+ "Memeriksa bukti terkait persentase kecukupan sarana dan prasarana terlihat dari ketersediaan, kemutakhiran, dan relevansi, mencakup:  "
				+ "fasilitas dan peralatan  " + "untuk pembelajaran,  " + "penelitian, PkM, dan  "
				+ "memfasilitasi yang  " + "berkebutuhan khusus."
				+ "Memeriksa bukti terkait persentase aksesibilitas sarana dan prasarana untuk menjamin pencapaian capaian pembelajaran dan meningkatkan suasana akademik\r\n"
				+ "Memeriksa bukti terkait persentase mutu sarana dan prasarana untuk menjamin pencapaian capaian pembelajaran dan meningkatkan suasana akademik\r\n"
				+ "memeriksa akses sistem informasi terkait data-data berikut: Mahasiswa, Kartu Rencana Studi (KRS), Jadwal Mata Kuliah, Nilai Mata Kuliah, Transkrip Akademik, Lulusan, Dosen, Pegawai, Keuangan, Inventaris, Perpustakaan\r\n"
				+ "memeriksa data-data uamg dapat diakses melalui sistem informasi.Data-data tersebut: Mahasiswa, Kartu Rencana Studi (KRS), Jadwal Mata Kuliah, Nilai Mata Kuliah, Transkrip Akademik, Lulusan, Dosen, Pegawai, Keuangan, Inventaris, Perpustakaan\r\n"
				+ "Memeriksa dokumen monitoring dan evaluasi sistem informasi terkait efektivitas Sistem Teknologi Informasi dan Komunikasi yang memenuhi aspek: "
				+ "1) mencakup layanan  " + "akademik, penelitian, pengabdian, keuangan,  " + "SDM, dan sarana dan  "
				+ "prasarana (aset),  " + "2) mudah diakses oleh  " + "seluruh unit kerja dalam  "
				+ "lingkup institusi,  " + "3) lengkap dan mutakhir,  " + "4) seluruh jenis layanan  "
				+ "telah terintegrasi dan  " + "digunakan untuk  " + "pengambilan keputusan,  " + "dan "
				+ "5) seluruh jenis layanan  "
				+ "yang terintegrasi dievaluasi secara berkala dan hasilnya ditindaklanjuti untuk penyempurnaan sistem  "
				+ "informasi.\r\n" + "memeriksa ketersediaan layanan e-learning dan perpustakaan digital \r\n"
				+ "memeriksa dokumen terkait Persentase kemudahan akses layanan e-learning dan perpustakaan digital \r\n"
				+ "memeriksa dokumen evaluasi dan tindak lanjut penyempurnaan sistem informasi Sekolah Tinggi\r\n"
				+ "memeriksa dokumen terkait Persentase kelengkapan dan kemutakhiran pengelolaan sistem informasi Sekolah Tinggi\r\n"
				+ "Memeriksa data terkait persentase kepuasan kelengkapan Sistem TIK (Teknologi Informasi dan Komunikasi) untuk mengelola dan menyebarkan hasil kegiatan Penelitian\r\n"
				+ "Memeriksa data terkait persentase kepuasan kelengkapan Sistem TIK (Teknologi Informasi dan Komunikasi) untuk mengelola dan menyebarkan hasil kegiatan Pengabdian kepada Masyarakat\r\n"
				+ "memeriksa dokumen terkait pengambilan keputusan melalui sistem informasi terintegrasi di Sekolah Tinggi \r\n"
				+ "memastikan ketersediaan surat penunjukan tim lembaga penelitian di tingkat perguruan tinggi\r\n"
				+ "memeriksa ketersediaan ruang lembaga penelitian di tingkat perguruan tinggi\r\n"
				+ "memeriksa ketersediaan laboratorium pendidikan terkait dengan bidang kajian program studi\r\n"
				+ "memeriksa ketersediaan laboratorium riset terkait dengan bidang kajian program studi\r\n"
				+ "memeriksa ketersediaan bukti legal laboratorium riset\r\n"
				+ "memeriksa data terkait Jumlah kelompok riset di tingkat perguruan tinggi\r\n"
				+ "memeriksa ketersediaan bukti legal kelompok riset\r\n" + "memeriksa jumlah luaran kelompok riset\r\n"
				+ "\r\n" + "\r\n" + "\r\n" + "";

		buktisData = StringUtils.split(buktis, "\r\n");

		nomor = 1;
		totalIndex = 0;
		for (String b : bb) {
			if (!b.trim().isEmpty()) {
				ButirMutuSPMI butirMutuSPMI = new ButirMutuSPMI();
				butirMutuSPMI.setStandarSPMI(s4);
				butirMutuSPMI.setNomorUrut(nomor);
				butirMutuSPMI.setNama(b);
				butirMutuSPMI.setKeterangan(b);
				session.getTransaction().begin();
				session.save(butirMutuSPMI);
				session.getTransaction().commit();

				for (Integer no : dataIndikators.keySet()) {
					List<String> stringsData = dataIndikators.get(no);

					int noLocal = 1;
					for (String dataLocal : stringsData) {
						IndikatorSPMI indikatorSPMI = new IndikatorSPMI();
						indikatorSPMI.setNomorUrut(noLocal);
						indikatorSPMI.setNama(dataLocal);
						indikatorSPMI.setKeterangan(dataLocal);
						indikatorSPMI.setButirMutuSPMI(butirMutuSPMI);

						session.getTransaction().begin();
						session.save(indikatorSPMI);
						session.getTransaction().commit();

						try {
							SkenarioSPMI skenarioSPMI = new SkenarioSPMI();
							skenarioSPMI.setNomorUrut(1);
							skenarioSPMI.setNama(buktisData[totalIndex]);
							skenarioSPMI.setKeterangan(buktisData[totalIndex]);
							skenarioSPMI.setIndikatorSPMI(indikatorSPMI);

							session.getTransaction().begin();
							session.save(skenarioSPMI);
							session.getTransaction().commit();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/spmi/JenisSPMI.java:526");
							// e.printStackTrace();
						}

						noLocal++;
						totalIndex++;
					}
				}
				nomor++;
			}
		}
	}

	/**
	 * Parser pembantu khusus untuk skrip seed {@link #initDataAmi(Session, JenisSPMI)}:
	 * mengurai teks multi-baris dengan format tiap baris
	 * {@code "<nomor>;;<teks>"} (nomor dan teks dipisah delimiter {@code ";;"})
	 * menjadi peta {@code nomor -> daftar teks}, mengelompokkan seluruh baris
	 * yang berbagi nomor awalan yang sama ke dalam satu {@link List}. Baris
	 * yang gagal diuraikan (mis. tidak mengandung {@code ";;"}, atau bagian
	 * nomor bukan angka valid untuk {@link Integer#parseInt(String)}) dilewati
	 * begitu saja lewat {@code try/catch} yang menelan {@link Exception} tanpa
	 * dilempar ulang — baris bermasalah hilang tanpa jejak dari hasil akhir,
	 * bukan menghentikan proses parsing keseluruhan.
	 *
	 * @param data teks sumber multi-baris berformat {@code "<nomor>;;<teks>"} per baris
	 * @return peta nomor (key) ke daftar teks (value) yang berbagi nomor tersebut,
	 *         dalam urutan kemunculan pada {@code data}
	 */
	private static Map<Integer, List<String>> populate(String data) {
		Map<Integer, List<String>> dataIndikators = new HashMap<Integer, List<String>>();
		for (String s : StringUtils.split(data, "\r\n")) {
			try {
				String[] ss = StringUtils.split(s.trim(), ";;");

				Integer key = Integer.parseInt(ss[0].trim());
				List<String> ls = dataIndikators.get(key);
				if (ls == null) {
					ls = new ArrayList<String>();
					dataIndikators.put(key, ls);
				}
				ls.add(ss[1].trim());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/spmi/JenisSPMI.java:552");
				// TODO: handle exception
			}
		}

		return dataIndikators;
	}

	/** Konstruktor kosong wajib bagi Hibernate untuk membentuk proxy/instance entitas. */
	public JenisSPMI() {
	}

	/**
	 * @return primary key baris ini. Kolom {@code id} bertipe {@code IDENTITY}
	 *         (auto-increment oleh database) dan ditandai {@code insertable = false}.
	 */
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", insertable = false, unique = true, nullable = false)
	public Long getId() {
		return this.id;
	}

	/** @param id primary key; jarang dipanggil manual karena {@code id} adalah IDENTITY. */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return kode singkat jenis SPMI ini; tidak dipetakan lewat anotasi
	 *         {@code @Column} eksplisit (mengandalkan konvensi penamaan
	 *         Hibernate bawaan ke kolom {@code kode}), boleh {@code null}.
	 */
	public String getKode() {
		return kode;
	}

	/** @param kode kode singkat jenis SPMI ini; opsional. */
	public void setKode(String kode) {
		this.kode = kode;
	}

	/**
	 * @return nama/judul jenis instrumen SPMI ini (mis. "Lembar Kerja AMI"),
	 *         di-{@code trim()} terlebih dahulu; {@code null} bila belum diisi.
	 */
	@Column(name = "nama", nullable = false, columnDefinition = "text")
	public String getNama() {
		return this.nama == null ? null : this.nama.trim();
	}

	/** @param nama nama/judul jenis SPMI ini; wajib diisi (kolom {@code NOT NULL}). */
	public void setNama(String nama) {
		this.nama = nama;
	}

	/** @return keterangan/deskripsi tambahan bagi jenis SPMI ini; boleh {@code null}. */
	@Column(name = "keterangan", nullable = true, columnDefinition = "text")
	public String getKeterangan() {
		return this.keterangan;
	}

	/** @param keterangan keterangan/deskripsi tambahan; opsional. */
	public void setKeterangan(String keterangan) {
		this.keterangan = keterangan;
	}

	/**
	 * @return {@code true} bila jenis SPMI ini masih aktif/berlaku (dipakai
	 *         untuk sesi evaluasi baru), {@code false} bila dinonaktifkan
	 *         (soft delete). Default {@code true} bila kolom belum pernah
	 *         diisi — pola flag aktif "default aman" yang konsisten dengan
	 *         entitas SPMI lain di paket ini.
	 */
	public Boolean getAktif() {
		return aktif == null ? true : aktif;
	}

	/** @param aktif status aktif/nonaktif jenis SPMI ini; lihat {@link #getAktif()}. */
	public void setAktif(Boolean aktif) {
		this.aktif = aktif;
	}

}
