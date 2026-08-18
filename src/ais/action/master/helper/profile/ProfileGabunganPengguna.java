package ais.action.master.helper.profile;

import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Html;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileGabunganPengguna — Panel Profil Admin yang Mengelola PT dan Sekolah Sekaligus</h3>
 *
 * <p><b>Untuk apa:</b> Menampilkan profil gabungan bagi akun admin/pengguna umum yang
 * mengaktifkan baik modul Perguruan Tinggi maupun modul Sekolah, tetapi akun tersebut
 * tidak melekat sebagai mahasiswa, siswa, dosen, maupun guru. Jenis akun ini muncul pada
 * institusi yang menjalankan PT dan sekolah dalam satu instalasi (misalnya yayasan
 * pendidikan besar).</p>
 *
 * <p><b>Syarat aktif (diperiksa oleh {@link #aktifUntukLoginSaatIni(Tbmuser)}):</b>
 * <ul>
 *   <li>{@code Common.chekPtAtauSekolah()[0] == true} (modul PT aktif)</li>
 *   <li>{@code Common.chekPtAtauSekolah()[1] == true} (modul Sekolah aktif)</li>
 *   <li>{@code tbmuser.getMahasiswa()} / {@code ambilMahasiswa()} &rarr; {@code null}</li>
 *   <li>{@code tbmuser.getSiswa()} / {@code ambilSiswa()} &rarr; {@code null}</li>
 *   <li>{@code tbmuser.getDosen()} / {@code ambilDosen()} &rarr; {@code null}</li>
 *   <li>{@code tbmuser.getGuru()} / {@code ambilGuru()} &rarr; {@code null}</li>
 * </ul>
 * </p>
 *
 * <p><b>Cara kerja:</b> Dari controller profil, panggil:
 * <pre>
 *   if (ProfileGabunganPengguna.tampilkanJikaPerlu(parent)) { return; }
 * </pre>
 * Jika syarat terpenuhi, {@link #tampilkanJikaPerlu(Component)} membuat instansi baru
 * dan memanggil {@link #init(Component)}, yang membangun Grid profil berisi:
 * <ol>
 *   <li>Kartu identitas admin (foto, nama, HP, email).</li>
 *   <li>Panel ringkasan Sekolah via {@link ProfileAdminSekolah#lanjut}.</li>
 *   <li>Panel ringkasan Perguruan Tinggi via {@link ProfileAdminPerguruanTinggi#lanjut}.</li>
 *   <li>Kartu Prestasi Pegawai (satu kali, dikontrol oleh
 *       {@link ProfileUiHelper#setSembunyikanPegawai(boolean)} agar tidak duplikat).</li>
 * </ol>
 * </p>
 *
 * <p><b>Threading:</b> Harus dipanggil dari ZK event thread.</p>
 *
 * <p><b>Pemeliharaan:</b> Kompatibel Java 1.7 dan ZKoss 5.5. Penambahan panel baru
 * harus memperhatikan mekanisme {@code sembunyikanPegawai} agar kartu prestasi pegawai
 * tidak dirender dua kali dari masing-masing lanjut(). Deteksi mahasiswa/siswa/dosen/guru
 * menggunakan refleksi ({@link #ambil(Object, String[])}) untuk menoleransi variasi
 * nama method antar-versi entitas.</p>
 */
public class ProfileGabunganPengguna {

	private Tbmuser tbmuser;
	private boolean tampilkanChart = true;

	/**
	 * Membuat instansi profil gabungan dengan opsi grafik diaktifkan (default {@code true}).
	 *
	 * @param tbmuser pengguna yang sedang login; boleh {@code null} (akan diambil ulang
	 *                dari {@code Common.getCurrentUser()} saat {@link #init(Component)} dipanggil)
	 */
	public ProfileGabunganPengguna(Tbmuser tbmuser) {
		this.tbmuser = tbmuser;
	}

	/**
	 * Membuat instansi profil gabungan dengan kontrol eksplisit atas tampilan grafik.
	 *
	 * @param tbmuser       pengguna yang sedang login; boleh {@code null}
	 * @param tampilkanChart {@code true} untuk menampilkan grafik statistik di panel sekolah
	 *                       dan perguruan tinggi; {@code false} untuk mode ringkasan saja
	 */
	public ProfileGabunganPengguna(Tbmuser tbmuser, boolean tampilkanChart) {
		this.tbmuser = tbmuser;
		this.tampilkanChart = tampilkanChart;
	}

	/**
	 * Factory method: menampilkan profil gabungan jika akun saat ini memenuhi syarat.
	 *
	 * <p><b>Tujuan:</b> Pola early-return yang dipakai di awal {@code ProfileAction.initProfile}.
	 * Jika akun saat ini adalah admin gabungan (PT + Sekolah, bukan mahasiswa/siswa/dosen/guru),
	 * metode ini membangun profil sepenuhnya dan mengembalikan {@code true} sehingga
	 * controller dapat berhenti tanpa merender profil lain.</p>
	 *
	 * <p><b>Cara kerja:</b> Mengambil {@code Tbmuser} dari
	 * {@code Common.getCurrentUser()}, memeriksa via {@link #aktifUntukLoginSaatIni(Tbmuser)},
	 * dan jika lolos memanggil {@code new ProfileGabunganPengguna(user, true).init(parent)}.</p>
	 *
	 * @param parent komponen ZK tujuan rendering profil
	 * @return {@code true} jika profil gabungan sudah dirender; {@code false} jika akun
	 *         tidak memenuhi syarat (controller harus lanjut ke profil lain)
	 * @throws Exception bila {@link #init(Component)} melempar exception
	 */
	public static boolean tampilkanJikaPerlu(Component parent) throws Exception {
		Tbmuser user = Common.getCurrentUser();
		if (!aktifUntukLoginSaatIni(user)) {
			return false;
		}
		new ProfileGabunganPengguna(user, true).init(parent);
		return true;
	}

	/**
	 * Memeriksa apakah pengguna yang sedang login adalah admin gabungan
	 * tanpa entitas mahasiswa/siswa/dosen/guru.
	 *
	 * <p>Delegator ke {@link #aktifUntukLoginSaatIni(Tbmuser)} dengan argumen
	 * {@code Common.getCurrentUser()}. Berguna saat pemanggil tidak memegang
	 * referensi {@code Tbmuser} secara langsung.</p>
	 *
	 * @return {@code true} jika kondisi profil gabungan terpenuhi untuk sesi saat ini
	 */
	public static boolean aktifUntukLoginSaatIni() {
		return aktifUntukLoginSaatIni(Common.getCurrentUser());
	}

	/**
	 * Memeriksa apakah pengguna yang diberikan memenuhi syarat sebagai admin gabungan
	 * (PT + Sekolah keduanya aktif, tanpa entitas mahasiswa/siswa/dosen/guru).
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@code Common.chekPtAtauSekolah()} untuk mendapatkan flag
	 *       {@code pt} (index 0) dan {@code ya/sekolah} (index 1).</li>
	 *   <li>Jika salah satu false, langsung mengembalikan {@code false}.</li>
	 *   <li>Menggunakan refleksi via {@link #ambil(Object, String[])} untuk memeriksa
	 *       keberadaan mahasiswa, siswa, dosen, dan guru; toleran terhadap variasi
	 *       nama method ({@code get*} vs {@code ambil*}).</li>
	 *   <li>Mengembalikan {@code true} hanya jika semua empat entitas {@code null}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Exception diswallow via
	 * {@code Common.tampilErrorJikaAdmin} dan metode mengembalikan {@code false}.</p>
	 *
	 * @param user pengguna yang diperiksa; boleh {@code null} (akan mengembalikan {@code false})
	 * @return {@code true} jika user adalah admin gabungan tanpa entitas akademik terikat
	 */
	public static boolean aktifUntukLoginSaatIni(Tbmuser user) {
		try {
			boolean[] ptYa = Common.chekPtAtauSekolah();
			boolean pt = ptYa != null && ptYa.length > 0 && ptYa[0];
			boolean ya = ptYa != null && ptYa.length > 1 && ptYa[1];

			if (!pt || !ya) {
				return false;
			}

			Object mahasiswa = ambil(user, new String[] { "getMahasiswa", "ambilMahasiswa" });
			Object siswa = ambil(user, new String[] { "getSiswa", "ambilSiswa" });
			Object dosen = ambil(user, new String[] { "ambilDosen", "getDosen" });
			Object guru = ambil(user, new String[] { "ambilGuru", "getGuru" });

			return mahasiswa == null && siswa == null && dosen == null && guru == null;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	/**
	 * Membangun panel profil gabungan admin (PT + Sekolah) ke dalam komponen ZK yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Merender satu halaman profil yang menggabungkan ringkasan sekolah
	 * dan perguruan tinggi dalam urutan panel yang bersih, tanpa duplikat kartu prestasi
	 * pegawai (dikontrol via {@link ProfileUiHelper#setSembunyikanPegawai(boolean)}).</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memanggil {@link ProfileUiHelper#prepareContentParent(Component)} untuk
	 *       membersihkan parent dan menyuntikkan CSS profil.</li>
	 *   <li>Membangun kartu identitas admin via {@link #appendUserInfo(Rows, String)}.</li>
	 *   <li>Menyalakan flag {@code sembunyikanPegawai} via
	 *       {@link ProfileUiHelper#setSembunyikanPegawai(boolean) setSembunyikanPegawai(true)}
	 *       sebelum memanggil kedua {@code lanjut()}, lalu mematikannya di blok
	 *       {@code finally} agar tidak bocor ke request lain.</li>
	 *   <li>Memanggil {@link ProfileAdminSekolah#lanjut(Rows, Tbmuser, boolean)} dan
	 *       {@link ProfileAdminPerguruanTinggi#lanjut(Rows, Tbmuser, boolean)} secara
	 *       berurutan, masing-masing dibungkus try-catch.</li>
	 *   <li>Setelah flag dimatikan, menambahkan kartu Prestasi Pegawai SEKALI via
	 *       {@link ProfileUiHelper#appendPrestasiRingkas(Rows, boolean, boolean, boolean)}
	 *       dengan {@code sekolah=false, pt=false, pegawai=true}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Thread-safety flag sembunyikanPegawai:</b> Menggunakan {@code ThreadLocal}
	 * di {@code ProfileUiHelper}, sehingga aman di lingkungan multi-thread ZK selama
	 * {@code finally} selalu dijalankan (yang dijamin oleh try-finally di sini).</p>
	 *
	 * @param parent komponen ZK tujuan; jika {@code null} metode langsung return
	 * @throws Exception bila terjadi error ZK dalam operasi penyusunan komponen
	 */
	public void init(final Component parent) throws Exception {
		if (parent == null) {
			return;
		}
		if (tbmuser == null) {
			tbmuser = Common.getCurrentUser();
		}

		Component contentParent = ProfileUiHelper.prepareContentParent(parent);
		String waktu = ProfileUiHelper.waktuSapaan();

		if (parent instanceof LayoutRegion) {
			((LayoutRegion) parent).setTitle("Hai, Selamat " + waktu);
			((LayoutRegion) parent).setCollapsible(true);
			((LayoutRegion) parent).setSplittable(true);
		}

		Grid grid = new Grid();
		grid.setSclass("dgrid fgrid");
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setStyle("border:0;background:linear-gradient(180deg,#f8fafc 0%,#eef6ff 100%);padding:2px;");
		grid.setParent(contentParent);

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("80px");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		appendUserInfo(rows, "Hai, Selamat " + waktu);

		// GABUNGAN: sembunyikan kartu Prestasi Pegawai dari kedua lanjut agar TIDAK
		// duplikat (sekolah & PT sama-sama memanggil dgn pegawai=true), lalu tambahkan
		// kartu Prestasi Pegawai SEKALI setelahnya.
		ProfileUiHelper.setSembunyikanPegawai(true);
		try {
			rows.appendChild(new ais.ui.util.MyGroupConfig("Sekolah"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Ringkasan Sekolah",
					"Jumlah siswa, guru, kelas, dan jadwal pelajaran sekolah.");
			try {
				ProfileAdminSekolah.lanjut(rows, tbmuser, tampilkanChart);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			rows.appendChild(new ais.ui.util.MyGroupConfig("Perguruan Tinggi"));
			ProfileUiHelper.appendPanelInfoRow(rows, 2, "Ringkasan Perguruan Tinggi",
					"Jumlah mahasiswa, dosen, lulusan, dan pendaftar baru kampus.");
			try {
				ProfileAdminPerguruanTinggi.lanjut(rows, tbmuser, tampilkanChart);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
		} finally {
			ProfileUiHelper.setSembunyikanPegawai(false);
		}

		// Prestasi Pegawai SEKALI saja untuk admin gabungan.
		ProfileUiHelper.appendPrestasiRingkas(rows, false, false, true);
	}

	/**
	 * Merender baris kartu identitas admin gabungan ke dalam {@code Rows} ZK.
	 *
	 * <p><b>Tujuan:</b> Menyajikan sapaan, foto profil, nama pengguna, nomor HP, dan
	 * email admin dalam satu baris kartu visual di atas panel profil. Kartu ini berfungsi
	 * sebagai pembuka yang hangat dan informatif bagi pengguna.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Memuat foto profil admin via {@code CommonMedia.tampilkanGambarKecil(tbmuser)};
	 *       jika gagal (foto belum ada, file tidak ditemukan), variabel {@code foto}
	 *       dibiarkan {@code null} dan error hanya ditampilkan ke admin.</li>
	 *   <li>Memanggil {@link ProfileUiHelper#mulaiKartuIdentitas(Rows, int, Component, String)}
	 *       dengan kolspan 2 dan teks sapaan (mis. "Hai, Selamat Pagi") untuk mendapatkan
	 *       {@code Div} wrapper konten.</li>
	 *   <li>Menambahkan label nama tebal ({@code MyLabelBoldAja}), HP, dan email ke dalam
	 *       {@code Div} wrapper tersebut.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Foto yang gagal dimuat tidak menghentikan render;
	 * kartu tetap tampil tanpa gambar. Method ini memiliki guard awal: jika {@code tbmuser}
	 * atau {@code rows} adalah {@code null}, metode langsung return tanpa efek samping.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Sapaan diteruskan sebagai parameter dari
	 * {@link #init(Component)} agar tidak menghitung jam dua kali.</p>
	 *
	 * @param rows   baris ZK tujuan penambahan kartu identitas
	 * @param sapaan teks sapaan berdasarkan waktu, mis. "Hai, Selamat Pagi"
	 */
	private void appendUserInfo(Rows rows, String sapaan) {
		if (tbmuser == null || rows == null) {
			return;
		}
		Component foto = null;
		try {
			foto = ais.common.CommonMedia.tampilkanGambarKecil(tbmuser);
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		org.zkoss.zul.Div vbox = ProfileUiHelper.mulaiKartuIdentitas(rows, 2, foto, sapaan);
		vbox.appendChild(new MyLabelBoldAja(tbmuser.getUserNama()));
		tbmuser.tampilkanHp(vbox);
		tbmuser.tampilkanEmail(vbox);
	}

	/**
	 * Mengambil nilai dari objek menggunakan refleksi Java dengan daftar nama method fallback.
	 *
	 * <p><b>Tujuan:</b> Memungkinkan akses ke getter yang namanya bervariasi antar entitas
	 * (mis. {@code getMahasiswa} vs {@code ambilMahasiswa}) tanpa harus menulis casting
	 * atau instanceof untuk setiap kemungkinan. Dipakai oleh
	 * {@link #aktifUntukLoginSaatIni(Tbmuser)} untuk memeriksa apakah {@code Tbmuser}
	 * punya entitas akademik (mahasiswa / siswa / dosen / guru).</p>
	 *
	 * <p><b>Cara kerja:</b> Iterasi array {@code methodNames} secara berurutan; untuk
	 * setiap nama, coba {@code object.getClass().getMethod(name)} dan invoke tanpa argumen.
	 * Jika hasilnya non-{@code null}, langsung kembalikan nilai tersebut (short-circuit).
	 * Exception per percobaan (NoSuchMethodException, InvocationTargetException, dll.)
	 * diswallow supaya iterasi lanjut ke nama berikutnya.</p>
	 *
	 * <p><b>Thread-safety:</b> Refleksi Java bersifat thread-safe untuk read-only lookup.
	 * Tidak ada state yang dimodifikasi.</p>
	 *
	 * <p><b>Batasan:</b> Hanya mendukung getter tanpa parameter (no-arg). Tidak
	 * cocok untuk method yang membutuhkan argumen. Jika semua nama gagal atau
	 * mengembalikan {@code null}, metode ini mengembalikan {@code null}.</p>
	 *
	 * @param object      objek yang akan di-refleksi; jika {@code null} langsung return {@code null}
	 * @param methodNames daftar nama method yang akan dicoba secara berurutan
	 * @return nilai pertama yang non-{@code null} dari salah satu method, atau {@code null}
	 *         jika tidak ada yang berhasil
	 */
	private static Object ambil(Object object, String[] methodNames) {
		if (object == null || methodNames == null) {
			return null;
		}
		for (int i = 0; i < methodNames.length; i++) {
			// FIX noise ErrorLog: NoSuchMethodException per kandidat nama yg TAK
			// ADA di kelas ini (mis. "ambilMahasiswa" saat Tbmuser cuma punya
			// "getMahasiswa") adalah hasil NORMAL/DIHARAPKAN dari mekanisme
			// fallback ini (lihat javadoc di atas), bukan kegagalan nyata --
			// jangan dicatat sbg ErrorLog, cukup lanjut ke kandidat berikutnya.
			java.lang.reflect.Method m;
			try {
				m = object.getClass().getMethod(methodNames[i], new Class[0]);
			} catch (NoSuchMethodException nsme) {
				continue;
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileGabunganPengguna.java:357");
				continue;
			}
			try {
				Object value = m.invoke(object, new Object[0]);
				if (value != null) {
					return value;
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileGabunganPengguna.java:357");
			}
		}
		return null;
	}

	/**
	 * <h3>CommonMediaSafe — Wrapper Tipis untuk Pemuatan Foto Profil</h3>
	 *
	 * <p><b>Untuk apa:</b> Menyembunyikan detail import {@code ais.common.CommonMedia}
	 * dari tubuh class utama, sehingga jika di masa depan terdapat variasi paket atau nama
	 * class media, cukup satu titik yang diubah.</p>
	 *
	 * <p><b>Cara kerja:</b> Memanggil
	 * {@code CommonMedia.tampilkanGambarKecil(user)} dan langsung memasang hasilnya
	 * ke komponen {@code row} yang diberikan. Exception dibiarkan melempar ke pemanggil
	 * agar dapat ditangani sesuai konteks.</p>
	 *
	 * <p><b>Threading:</b> Harus dipanggil dari ZK event thread karena memodifikasi
	 * pohon komponen ZK ({@code setParent}).</p>
	 *
	 * <p><b>Pemeliharaan:</b> Class ini {@code private static final} dan tidak dapat
	 * di-extend. Jika foto tidak diperlukan, cukup tidak memanggil method ini;
	 * tidak ada cleanup yang dibutuhkan.</p>
	 */
	private static final class CommonMediaSafe {
		/**
		 * Menampilkan foto profil pengguna {@code user} ke dalam baris {@code row}.
		 *
		 * <p>Memanggil {@code CommonMedia.tampilkanGambarKecil(user)} untuk mendapatkan
		 * komponen gambar ZK, lalu memasangnya ke {@code row} via {@code setParent}.
		 * Tidak melakukan null-check — pemanggil bertanggung jawab atas validasi input.</p>
		 *
		 * @param user pengguna yang fotonya akan ditampilkan
		 * @param row  baris ZK tujuan pemasangan komponen gambar
		 * @throws Exception bila pemuatan gambar atau pemasangan komponen gagal
		 */
		private static void tampilkanFoto(Tbmuser user, Row row) throws Exception {
			ais.common.CommonMedia.tampilkanGambarKecil(user).setParent(row);
		}
	}
}
