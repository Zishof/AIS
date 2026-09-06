package ais.action.master.helper;

import java.util.Calendar;

import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.common.Common;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Judisium;
import ais.database.model.Mahasiswa;
import ais.database.model.StatusDomisiliSetelahLulus;
import ais.database.model.StatusKeluar;
import ais.database.model.StatusPekerjaanSetelahLulus;
import ais.database.model.StatusSetelahLulus;
import ais.database.model.Tbmuser;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyFormRow;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyTextbox;

/**
 * Helper PEMBANGUN-FORM "Informasi Kelulusan Mahasiswa" yang dapat dipakai-ulang (reuse) oleh
 * beberapa halaman, terutama {@code MahasiswaAction} (form tambah/ubah Mahasiswa, tab "Informasi
 * Kelulusan") dan {@code SkripsiAction} (form ubah Tugas Akhir, tab "Informasi Kelulusan
 * Mahasiswa"). Sebelum helper ini dibuat, KEDUA halaman menyusun ulang deretan field kelulusan
 * yang nyaris sama dengan kode terduplikasi, sehingga keduanya BERBEDA (mis. SkripsiAction tidak
 * memiliki field "Email Atasan", "Non Aktif di semester mana", "Tanggal Wisuda", dan "Nomor SK
 * Drop Out"). Dengan helper ini, satu sumber kebenaran (mengikuti susunan field MahasiswaAction)
 * dipakai bersama sehingga tampilan SELALU seragam dan perubahan cukup dilakukan di satu tempat.
 *
 * <p><b>Pola pemakaian.</b> Helper TIDAK menyimpan komponen sebagai field statis (agar aman dari
 * akses multi-pengguna). Sebaliknya, {@link #build(Rows, Mahasiswa, Tbmuser)} membangun seluruh
 * baris field ke dalam {@link Rows} yang diberikan, lalu mengembalikan sebuah objek pembawa
 * (holder) {@link Komponen} yang berisi seluruh komponen ZK yang baru dibuat (Combobox/Textbox/
 * Datebox). Halaman pemanggil kemudian:
 * <ol>
 *   <li>Memasang field instance-nya sendiri dari holder bila logika simpan/validasi lamanya masih
 *       mereferensikan field tersebut (mis. MahasiswaAction yang punya logika simpan kompleks);
 *       atau</li>
 *   <li>Memanggil {@link #terapkan(Komponen, Mahasiswa, BiodataMahasiswa)} untuk memetakan nilai
 *       form ke entity {@link Mahasiswa} (dan {@link BiodataMahasiswa} untuk Email Atasan) secara
 *       langsung (mis. SkripsiAction).</li>
 * </ol>
 *
 * <p><b>Perilaku yang dipertahankan PERSIS dari MahasiswaAction.</b>
 * <ul>
 *   <li>Combobox terkait master (Status Keluar, Predikat Kelulusan/Judisium, Status Setelah
 *       Lulus, Status Pekerjaan, Status Domisili) diisi via {@code Common.insertComboDanSemua}
 *       dengan opsi default "== Mahasiswa Belum ... ==" dan diseleksi sesuai nilai mahasiswa.</li>
 *   <li>Predikat Kelulusan disaring berdasar jenjang mahasiswa dan hanya yang aktif.</li>
 *   <li>Hak akses: bila yang login adalah <i>mahasiswa</i> atau <i>siswa</i> (bukan pengelola),
 *       field ditampilkan sebagai {@link Label} read-only (bukan input), persis seperti aslinya.</li>
 *   <li>Field dinonaktifkan (disabled) bila mahasiswa sudah terikat
 *       {@code KelompokStatusKeluarMahasiswa} (kelulusan kolektif), agar tidak diubah perorangan.</li>
 *   <li>Field "Tahun Lulus" tetap ada namun barisnya disembunyikan (mengikuti aslinya), karena
 *       tahun lulus mengikuti tanggal lulus secara otomatis.</li>
 *   <li>Teks keterangan/bantuan di bawah field tertentu dipertahankan apa adanya.</li>
 * </ul>
 *
 * <p><b>Cakupan field (inti kelulusan).</b> Status Keluar dari Kampus, Predikat Kelulusan, Status
 * Setelah Lulus, Status Pekerjaan Setelah Lulus, Alamat Email Pimpinan/Atasan, Status Domisili
 * Setelah Lulus, No. Ijazah I, No. Ijazah II, No. Akta, No. Surat Pendamping Ijazah, Tahun Lulus
 * (tersembunyi), Tanggal Lulus, Semester Lulus, Non Aktif di semester mana, Tahun Wisuda, Tanggal
 * Wisuda, Tanggal Yudisium, No. SK, Tanggal SK, dan Nomor SK Drop Out. Field khas masing-masing
 * halaman (mis. "Nama/Tgl lahir untuk Ijazah Sebelumnya" + tombol "Preview Ijazah" di Skripsi,
 * atau Judul Skripsi/Pembimbing di Mahasiswa) TETAP disusun oleh halaman pemanggil di luar helper
 * ini, karena bersifat kontekstual.
 *
 * <p><b>Catatan kompatibilitas.</b> Ditulis kompatibel Java 1.7 (tanpa lambda/stream/diamond) dan
 * ZK 5.5, mengikuti gaya kode existing. Helper ini murni membangun UI + memetakan nilai; ia tidak
 * membuka/menutup Session Hibernate sendiri — persistensi diserahkan ke pemanggil.
 */
public class FormKelulusanHelper {

	/**
	 * Pembawa (holder) seluruh komponen form kelulusan yang dibuat oleh
	 * {@link FormKelulusanHelper#build(Rows, Mahasiswa, Tbmuser)}. Bersifat publik agar mudah
	 * diakses pemanggil (assign ke field instance atau dibaca saat simpan).
	 */
	public static class Komponen {
		/** Combobox "Status Keluar dari Kampus" ({@link StatusKeluar}); {@code null} bila mode read-only. */
		public Combobox statusKeluar;
		/** Combobox "Predikat Kelulusan" ({@link Judisium}); {@code null} bila mode read-only. */
		public Combobox predikatKelulusan;
		/** Combobox "Status Setelah Lulus" ({@link StatusSetelahLulus}); {@code null} bila mode read-only. */
		public Combobox statusSetelahLulus;
		/** Combobox "Status Pekerjaan Setelah Lulus" ({@link StatusPekerjaanSetelahLulus}); {@code null} bila mode read-only. */
		public Combobox statusPekerjaanSetelahLulus;
		/** Combobox "Status Domisili Setelah Lulus" ({@link StatusDomisiliSetelahLulus}); {@code null} bila mode read-only. */
		public Combobox statusDomisiliSetelahLulus;
		/** Textbox "Alamat Email Pimpinan / Atasan Setelah Bekerja", selalu dipasang (tidak bergantung mode editable). */
		public Textbox emailAtasan;
		/** Textbox "No. Ijazah I". */
		public Textbox noIjazah1;
		/** Textbox "No. Ijazah II". */
		public Textbox noIjazah2;
		/** Textbox "No. Akta". */
		public Textbox noAkta1;
		/** Textbox "No. SK" (disimpan pada properti {@code noAkta2} entity {@link Mahasiswa}). */
		public Textbox noAkta2;
		/** Textbox "No. Surat Pendamping Ijazah" (SKPI). */
		public Textbox nomorSkpi;
		/** Textbox "Nomor SK Drop Out (jika DO)", selalu dipasang (tidak bergantung mode editable). */
		public Textbox skDo;
		/** Combobox "Tahun Lulus" (baris disembunyikan; mengikuti tanggal lulus otomatis). */
		public Combobox tahunLulus;
		/** Combobox "Semester Lulus / Keluar / Mengundurkan Diri / DO". */
		public Combobox semesterLulus;
		/** Textbox "Memaksa Non Aktif di semester mana saja ?" (daftar nomor semester dipisah koma). */
		public MyTextbox batasStudi;
		/** Textbox "Memaksa Aktif di semester mana saja ?" (daftar nomor semester dipisah koma). */
		public MyTextbox paksaAktifSemester;
		/** Intbox "Tahun Wisuda". */
		public Intbox tahunWisuda;
		/** Datebox "Tanggal Lulus / Keluar / Mengundurkan Diri / DO". */
		public MyDatebox tanggalLulus;
		/** Datebox "Tanggal Wisuda". */
		public MyDatebox tanggalWisuda;
		/** Datebox "Tanggal Yudisium". */
		public MyDatebox tanggalYudisium;
		/** Datebox "Tanggal SK" (SK Rektor, disimpan pada properti {@code tanggalSkRektor} entity {@link Mahasiswa}). */
		public MyDatebox tanggalSkRektor;
	}

	/** Kelas utilitas statis; tidak dimaksudkan untuk diinstansiasi. */
	private FormKelulusanHelper() {
	}

	/**
	 * Membangun seluruh baris field "Informasi Kelulusan" ke dalam {@code rows} dan mengembalikan
	 * holder berisi komponen yang dibuat. Mengikuti PERSIS susunan & perilaku MahasiswaAction.
	 *
	 * @param rows      kontainer baris (umumnya {@code Rows} dari sebuah {@code MyGrid} 2 kolom).
	 * @param mahasiswa entity mahasiswa yang nilai-nilainya mengisi form (tidak boleh {@code null}).
	 * @param tbmuser   pengguna yang login (untuk menentukan editable vs read-only).
	 * @return holder {@link Komponen} berisi seluruh komponen yang dibuat.
	 */
	public static Komponen build(Rows rows, final Mahasiswa mahasiswa, Tbmuser tbmuser) {
		Komponen k = new Komponen();

		boolean editable = tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null;
		boolean adaKelompokKeluar = mahasiswa.getKelompokStatusKeluarMahasiswa() != null;
		boolean adaKelompokKeluarTglLulus = adaKelompokKeluar
				&& mahasiswa.getKelompokStatusKeluarMahasiswa().getTanggalLulus() != null;

		BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();

		// --- Status Keluar dari Kampus ---
		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Status Keluar dari Kampus"));
		k.statusKeluar = new Combobox();
		if (editable) {
			row.appendChild(k.statusKeluar);
		} else {
			row.appendChild(new Label(mahasiswa.getStatusKeluar() == null ? "" : mahasiswa.getStatusKeluar().getNama()));
		}
		Common.insertComboDanSemua(k.statusKeluar, new String[] { "nama", "feeder" }, "keterangan", StatusKeluar.class,
				"== Mahasiswa Belum Keluar / Masih Aktif ==", Restrictions.sqlRestriction("true"));
		Common.selectComboItem(k.statusKeluar, mahasiswa.getStatusKeluar());
		k.statusKeluar.setWidth("90%");
		k.statusKeluar.setReadonly(true);
		if (adaKelompokKeluar) {
			k.statusKeluar.setDisabled(true);
		}

		// --- Predikat Kelulusan ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Predikat Kelulusan"));
		k.predikatKelulusan = new Combobox();
		if (editable) {
			row.appendChild(k.predikatKelulusan);
		} else {
			row.appendChild(new Label(
					mahasiswa.getPredikatKelulusan() == null ? "" : mahasiswa.getPredikatKelulusan().getNama()));
		}
		Common.insertComboDanSemua(k.predikatKelulusan,
				new String[] { "nama", "jenjang", "nilaiMulai", "nilaiSampai" }, "keterangan", Judisium.class,
				"== Tanpa Predikat Kelulusan (otomatis) ==",
				Restrictions.and(
						Restrictions.or(Restrictions.isNull("jenjang"),
								mahasiswa.getJenjang() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("jenjang", mahasiswa.getJenjang())),
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
		Common.selectComboItem(k.predikatKelulusan, mahasiswa.getPredikatKelulusan());
		k.predikatKelulusan.setWidth("90%");
		k.predikatKelulusan.setReadonly(true);

		// --- Status Setelah Lulus ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Status Setelah Lulus"));
		k.statusSetelahLulus = new Combobox();
		if (editable) {
			row.appendChild(k.statusSetelahLulus);
		} else {
			row.appendChild(new Label(
					mahasiswa.getStatusSetelahLulus() == null ? "" : mahasiswa.getStatusSetelahLulus().getNama()));
		}
		Common.insertComboDanSemua(k.statusSetelahLulus, new String[] { "nama", "kode" }, "keterangan",
				StatusSetelahLulus.class, "== Mahasiswa Belum Lulus / Masih Aktif ==",
				Restrictions.sqlRestriction("true"));
		Common.selectComboItem(k.statusSetelahLulus, mahasiswa.getStatusSetelahLulus());
		k.statusSetelahLulus.setWidth("90%");
		k.statusSetelahLulus.setReadonly(true);

		// --- Status Pekerjaan Setelah Lulus ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Status Pekerjaan Setelah Lulus"));
		k.statusPekerjaanSetelahLulus = new Combobox();
		if (editable) {
			row.appendChild(k.statusPekerjaanSetelahLulus);
		} else {
			row.appendChild(new Label(mahasiswa.getStatusPekerjaanSetelahLulus() == null ? ""
					: mahasiswa.getStatusPekerjaanSetelahLulus().getNama()));
		}
		Common.insertComboDanSemua(k.statusPekerjaanSetelahLulus, new String[] { "nama", "kode" }, "keterangan",
				StatusPekerjaanSetelahLulus.class, "== Mahasiswa Belum Lulus / Masih Aktif ==",
				Restrictions.sqlRestriction("true"));
		Common.selectComboItem(k.statusPekerjaanSetelahLulus, mahasiswa.getStatusPekerjaanSetelahLulus());
		k.statusPekerjaanSetelahLulus.setWidth("90%");
		k.statusPekerjaanSetelahLulus.setReadonly(true);

		// --- Alamat Email Pimpinan / Atasan Setelah Bekerja (tersimpan di BiodataMahasiswa) ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Alamat Email Pimpinan / Atasan Setelah Bekerja"));
		k.emailAtasan = new Textbox(biodataMahasiswa == null ? null : biodataMahasiswa.getEmailAtasan());
		row.appendChild(k.emailAtasan);
		k.emailAtasan.setWidth("90%");
		Common.initKeterangan(rows,
				"Jika email lebih dari satu, gunakan tanda koma (,) sebagai pemisah, misal-nya :  atasan@mail.com,atasan1@oke.com,atasan3@mail.com. Sedangkan untuk email utama, tempatkan di urutan paling depan.");

		// --- Status Domisili Setelah Lulus ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Status Domisili Setelah Lulus"));
		k.statusDomisiliSetelahLulus = new Combobox();
		if (editable) {
			row.appendChild(k.statusDomisiliSetelahLulus);
		} else {
			row.appendChild(new Label(mahasiswa.getStatusDomisiliSetelahLulus() == null ? ""
					: mahasiswa.getStatusDomisiliSetelahLulus().getNama()));
		}
		Common.insertComboDanSemua(k.statusDomisiliSetelahLulus, new String[] { "nama", "kode" }, "keterangan",
				StatusDomisiliSetelahLulus.class, "== Mahasiswa Belum Lulus / Masih Aktif ==",
				Restrictions.sqlRestriction("true"));
		Common.selectComboItem(k.statusDomisiliSetelahLulus, mahasiswa.getStatusDomisiliSetelahLulus());
		k.statusDomisiliSetelahLulus.setWidth("90%");
		k.statusDomisiliSetelahLulus.setReadonly(true);

		// --- No. Ijazah I ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Ijazah I"));
		k.noIjazah1 = new Textbox(mahasiswa.getNoIjazah1());
		if (editable) {
			row.appendChild(k.noIjazah1);
		} else {
			row.appendChild(new Label(mahasiswa.getNoIjazah1()));
		}
		k.noIjazah1.setWidth("90%");

		// --- No. Ijazah II ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Ijazah II"));
		k.noIjazah2 = new Textbox(mahasiswa.getNoIjazah2());
		if (editable) {
			row.appendChild(k.noIjazah2);
		} else {
			row.appendChild(new Label(mahasiswa.getNoIjazah2()));
		}
		k.noIjazah2.setWidth("90%");

		// --- No. Akta ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Akta"));
		k.noAkta1 = new Textbox(mahasiswa.getNoAkta1());
		if (editable) {
			row.appendChild(k.noAkta1);
		} else {
			row.appendChild(new Label(mahasiswa.getNoAkta1()));
		}
		k.noAkta1.setWidth("90%");

		// --- No. Surat Pendamping Ijazah (SKPI) ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. Surat Pendamping Ijazah"));
		k.nomorSkpi = new Textbox(mahasiswa.getNomorSkpi());
		if (editable) {
			row.appendChild(k.nomorSkpi);
		} else {
			row.appendChild(new Label(mahasiswa.getNomorSkpi()));
		}
		k.nomorSkpi.setWidth("90%");

		// --- Tahun Lulus (baris DISEMBUNYIKAN; mengikuti tanggal lulus otomatis) ---
		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Lulus"));
		k.tahunLulus = new Combobox();
		Comboitem comboitem = new Comboitem("Belum Ada");
		comboitem.setValue(0);
		k.tahunLulus.appendChild(comboitem);
		int tahun = Calendar.getInstance().get(Calendar.YEAR);
		for (int i = (tahun - 25); i < (tahun + 25); i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			k.tahunLulus.appendChild(comboitem);
		}
		Common.selectComboItem(k.tahunLulus, mahasiswa.getTahunLulus() == null ? 0 : mahasiswa.getTahunLulus(), true);
		if (editable) {
			row.appendChild(k.tahunLulus);
		} else {
			row.appendChild(new Label(mahasiswa.getTahunLulus() == null || mahasiswa.getTahunLulus().equals(0) ? ""
					: mahasiswa.getTahunLulus().toString()));
		}
		k.tahunLulus.setCols(5);
		k.tahunLulus.setReadonly(true);

		// --- Tanggal Lulus / Keluar / Mengundurkan Diri / DO ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tanggal Lulus / Keluar / Mengundurkan Diri / DO"));
		k.tanggalLulus = new MyDatebox(mahasiswa.getTanggalLulus());
		if (editable) {
			row.appendChild(k.tanggalLulus);
		} else {
			row.appendChild(new Label(mahasiswa.getTanggalLulus() == null ? ""
					: Common.dateFormat2.get().format(mahasiswa.getTanggalLulus())));
		}
		if (adaKelompokKeluarTglLulus) {
			k.tahunLulus.setDisabled(true);
			k.tanggalLulus.setDisabled(true);
		}
		Common.initKeterangan(rows, "Tahun lulus secara otomatis mengikuti tanggal lulus");

		// --- Semester Lulus / Keluar / Mengundurkan Diri / DO ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Semester Lulus / Keluar / Mengundurkan Diri / DO"));
		k.semesterLulus = new Combobox();
		comboitem = new Comboitem("Belum Ada");
		comboitem.setValue(0);
		k.semesterLulus.appendChild(comboitem);
		for (int i = 0; i < 30; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			k.semesterLulus.appendChild(comboitem);
		}
		Common.selectComboItem(k.semesterLulus, mahasiswa.getSemesterLulus() == null ? 0 : mahasiswa.getSemesterLulus(),
				true);
		if (editable) {
			row.appendChild(k.semesterLulus);
		} else {
			row.appendChild(new Label(mahasiswa.getSemesterLulus() == null || mahasiswa.getSemesterLulus().equals(0) ? ""
					: mahasiswa.getSemesterLulus().toString()));
		}
		k.semesterLulus.setCols(5);
		k.semesterLulus.setReadonly(true);
		Common.initKeterangan(rows,
				"Semester lulus jika tidak sesuai dengan tahun lulus, maka secara otmatis sistem akan mensesuikan");
		Common.initKeterangan(rows,
				"Semester Lulus / Keluar / Mengundurkan Diri / DO harus diisi jika mahasiswa yang bersangkutan telah mempunyai status keluar");
		if (adaKelompokKeluar) {
			k.semesterLulus.setDisabled(true);
		}

		// --- Non Aktif di semester mana saja ? (batas studi) ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Memaksa Non Aktif di semester mana saja ?"));
		k.batasStudi = new MyTextbox(mahasiswa.getBatasStudi());
		k.batasStudi.setWidth("90%");
		if (editable) {
			row.appendChild(k.batasStudi);
		} else {
			row.appendChild(new Label(mahasiswa.getBatasStudi()));
		}
		Common.initKeterangan(rows,
				"Jika mahasiswa ini pernah non aktif, tuliskan semester nya di pisahkan dengan tanda koma (contoh : 3,4,5)");

		// --- Memaksa Aktif di semester mana saja ? (paksa status AKTIF) ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Memaksa Aktif di semester mana saja ?"));
		k.paksaAktifSemester = new MyTextbox(mahasiswa.getPaksaAktifSemester());
		k.paksaAktifSemester.setWidth("90%");
		if (editable) {
			row.appendChild(k.paksaAktifSemester);
		} else {
			row.appendChild(new Label(mahasiswa.getPaksaAktifSemester()));
		}
		Common.initKeterangan(rows,
				"Kebalikan dari kolom di atas: jika mahasiswa ini harus DIPAKSA AKTIF di semester tertentu, tuliskan semesternya dipisah koma (contoh : 3,4,5). Berlaku walau status pembayaran belum lunas.");

		// --- Tahun Wisuda ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tahun Wisuda"));
		k.tahunWisuda = new Intbox(mahasiswa.getTahunWisuda() == null ? 0 : mahasiswa.getTahunWisuda());
		if (editable) {
			row.appendChild(k.tahunWisuda);
		} else {
			row.appendChild(new Label(mahasiswa.getTahunWisuda() == null || mahasiswa.getTahunWisuda().equals(0) ? ""
					: mahasiswa.getTahunWisuda().toString()));
		}

		// --- Tanggal Wisuda ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tanggal Wisuda"));
		k.tanggalWisuda = new MyDatebox(mahasiswa.getTanggalWisuda());
		if (editable) {
			row.appendChild(k.tanggalWisuda);
		} else {
			row.appendChild(new Label(mahasiswa.getTanggalWisuda() == null ? ""
					: Common.dateFormat2.get().format(mahasiswa.getTanggalWisuda())));
		}

		// --- Tanggal Yudisium ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tanggal Yudisium"));
		k.tanggalYudisium = new MyDatebox(mahasiswa.getTanggalYudisium());
		if (editable) {
			row.appendChild(k.tanggalYudisium);
		} else {
			row.appendChild(new Label(mahasiswa.getTanggalYudisium() == null ? ""
					: Common.dateFormat2.get().format(mahasiswa.getTanggalYudisium())));
		}

		// --- No. SK (disimpan di noAkta2) ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("No. SK"));
		k.noAkta2 = new Textbox(mahasiswa.getNoAkta2());
		if (editable) {
			row.appendChild(k.noAkta2);
		} else {
			row.appendChild(new Label(mahasiswa.getNoAkta2()));
		}
		k.noAkta2.setWidth("90%");

		// --- Tanggal SK (SK Rektor) ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Tanggal SK"));
		k.tanggalSkRektor = new MyDatebox(mahasiswa.getTanggalSkRektor());
		if (editable) {
			row.appendChild(k.tanggalSkRektor);
		} else {
			row.appendChild(new Label(mahasiswa.getTanggalSkRektor() == null ? ""
					: Common.dateFormat2.get().format(mahasiswa.getTanggalSkRektor())));
		}
		if (adaKelompokKeluarTglLulus) {
			k.noAkta2.setDisabled(true);
			k.tanggalSkRektor.setDisabled(true);
		}

		// --- Nomor SK Drop Out (jika DO) ---
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Nomor SK Drop Out (jika DO)"));
		k.skDo = new Textbox(mahasiswa.getSkDo());
		row.appendChild(k.skDo);
		k.skDo.setWidth("90%");

		return k;
	}

	/**
	 * Memetakan nilai komponen form (dari {@link #build(Rows, Mahasiswa, Tbmuser)}) ke entity
	 * {@link Mahasiswa} (dan {@link BiodataMahasiswa} untuk Email Atasan). Hanya pemetaan
	 * langsung field → setter; logika bisnis lanjutan (mis. penyesuaian semester dari tanggal
	 * lulus, validasi status keluar) tetap menjadi tanggung jawab pemanggil bila diperlukan.
	 * Memanggil method ini sebaiknya hanya dilakukan pada konteks PENGELOLA (editable), karena
	 * pada mode read-only komponen input tidak dipasang ke layar.
	 *
	 * @param k         holder komponen hasil {@code build}.
	 * @param mahasiswa entity tujuan penyimpanan nilai kelulusan.
	 * @param biodata   biodata mahasiswa untuk menyimpan Email Atasan (boleh {@code null}).
	 */
	@SuppressWarnings("deprecation")
	public static void terapkan(Komponen k, Mahasiswa mahasiswa, BiodataMahasiswa biodata) {
		if (k == null || mahasiswa == null) {
			return;
		}

		mahasiswa.setStatusKeluar((StatusKeluar) (k.statusKeluar == null || k.statusKeluar.getSelectedItem() == null
				? null : k.statusKeluar.getSelectedItem().getValue()));
		mahasiswa.setPredikatKelulusan((Judisium) (k.predikatKelulusan == null
				|| k.predikatKelulusan.getSelectedItem() == null ? null
						: k.predikatKelulusan.getSelectedItem().getValue()));
		mahasiswa.setStatusSetelahLulus(
				(StatusSetelahLulus) (k.statusSetelahLulus == null || k.statusSetelahLulus.getSelectedItem() == null
						? null : k.statusSetelahLulus.getSelectedItem().getValue()));
		mahasiswa.setStatusPekerjaanSetelahLulus((StatusPekerjaanSetelahLulus) (k.statusPekerjaanSetelahLulus == null
				|| k.statusPekerjaanSetelahLulus.getSelectedItem() == null ? null
						: k.statusPekerjaanSetelahLulus.getSelectedItem().getValue()));
		mahasiswa.setStatusDomisiliSetelahLulus((StatusDomisiliSetelahLulus) (k.statusDomisiliSetelahLulus == null
				|| k.statusDomisiliSetelahLulus.getSelectedItem() == null ? null
						: k.statusDomisiliSetelahLulus.getSelectedItem().getValue()));

		if (k.noIjazah1 != null) {
			mahasiswa.setNoIjazah1(k.noIjazah1.getValue() == null ? null : k.noIjazah1.getValue().trim());
		}
		if (k.noIjazah2 != null) {
			mahasiswa.setNoIjazah2(k.noIjazah2.getValue() == null ? null : k.noIjazah2.getValue().trim());
		}
		if (k.noAkta1 != null) {
			mahasiswa.setNoAkta1(k.noAkta1.getValue() == null ? null : k.noAkta1.getValue().trim());
		}
		if (k.noAkta2 != null) {
			mahasiswa.setNoAkta2(k.noAkta2.getValue() == null ? null : k.noAkta2.getValue().trim());
		}
		if (k.nomorSkpi != null) {
			mahasiswa.setNomorSkpi(k.nomorSkpi.getValue());
		}
		if (k.skDo != null) {
			mahasiswa.setSkDo(k.skDo.getValue());
		}
		if (k.batasStudi != null) {
			mahasiswa.setBatasStudi(k.batasStudi.getValue());
		}
		if (k.paksaAktifSemester != null) {
			mahasiswa.setPaksaAktifSemester(k.paksaAktifSemester.getValue());
		}

		if (k.tahunLulus != null && k.tahunLulus.getSelectedItem() != null) {
			mahasiswa.setTahunLulus((Integer) k.tahunLulus.getSelectedItem().getValue());
		}
		if (k.semesterLulus != null && k.semesterLulus.getSelectedItem() != null) {
			mahasiswa.setSemesterLulus((Integer) k.semesterLulus.getSelectedItem().getValue());
		}
		if (k.tahunWisuda != null) {
			mahasiswa.setTahunWisuda(k.tahunWisuda.getValue());
		}
		if (k.tanggalLulus != null) {
			mahasiswa.setTanggalLulus(k.tanggalLulus.getValue());
		}
		if (k.tanggalWisuda != null) {
			mahasiswa.setTanggalWisuda(k.tanggalWisuda.getValue());
		}
		if (k.tanggalYudisium != null) {
			mahasiswa.setTanggalYudisium(k.tanggalYudisium.getValue());
		}
		if (k.tanggalSkRektor != null) {
			mahasiswa.setTanggalSkRektor(k.tanggalSkRektor.getValue());
		}

		if (biodata != null && k.emailAtasan != null) {
			biodata.setEmailAtasan(k.emailAtasan.getValue());
		}
	}
}
