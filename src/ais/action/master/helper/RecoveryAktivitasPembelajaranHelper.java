package ais.action.master.helper;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Label;
import org.zkoss.zul.Separator;
import org.zkoss.zul.Vbox;

import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.VOPembelajaran;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Pusat akses Recovery untuk aktivitas pembelajaran. Helper ini sengaja tidak
 * membuat tombol apa pun bagi akun peserta agar pembatasan tidak hanya bersifat
 * disabled di browser, melainkan tidak masuk ke component tree ZK sama sekali.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class RecoveryAktivitasPembelajaranHelper {

	/** Kelas utilitas statis murni; tidak boleh diinstansiasi. */
	private RecoveryAktivitasPembelajaranHelper() {
	}

	/**
	 * Recovery hanya untuk pengelola. Empat jenis login peserta yang secara
	 * eksplisit dilarang adalah Siswa, Mahasiswa, CalonSiswa, dan
	 * BiodataCalonMahasiswa.
	 */
	public static boolean bolehTampil(Tbmuser user) {
		return user != null && user.getSiswa() == null && user.getMahasiswa() == null
				&& user.getCalonSiswa() == null && user.getBiodataCalonMahasiswa() == null;
	}

	/**
	 * Membuka dialog pilihan Recovery Ujian dengan dua opsi: mengembalikan relasi ujian yang hilang
	 * dari pertemuan ({@link RevisiPertemuanPunyaUjianHelper}), atau mengembalikan master ujian
	 * beserta seluruh soalnya ({@link RevisiUjianHelper}). Kedua opsi dibatasi ke
	 * {@code pembelajaran} (mata kuliah/kelas yang sedang dibuka) lewat parameter konstruktor
	 * masing-masing helper. Tombol pemicu dialog ini tidak pernah dirender untuk akun peserta
	 * (lihat {@link #bolehTampil(ais.database.model.Tbmuser)}).
	 *
	 * @param pembelajaran konteks mata kuliah/kelas yang membatasi cakupan recovery.
	 * @param callback     callback yang diteruskan ke helper revisi terpilih saat restore berhasil;
	 *                     boleh {@code null}.
	 * @throws Exception diteruskan apa adanya dari konstruktor helper revisi terpilih.
	 */
	public static void bukaRecoveryUjian(final VOPembelajaran pembelajaran, final EventListener callback)
			throws Exception {
		final MyWindow pilihan = buatPilihan("Recovery Ujian",
				"Pilih data yang perlu dikembalikan. Gunakan pilihan pertama bila ujian hilang dari pertemuan. "
						+ "Gunakan pilihan kedua bila master ujian atau susunan soalnya ikut terhapus.");

		tambahPilihan(pilihan, "Jadwal Ujian pada Pertemuan", "/img/jadwal.png",
				"Kembalikan relasi ujian yang terhapus dari pertemuan pada mata kuliah/kelas ini.",
				new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						pilihan.detach();
						tampilkan(new RevisiPertemuanPunyaUjianHelper(pembelajaran, callback));
					}
				});

		tambahPilihan(pilihan, "Master Ujian dan Soal", "/img/refresh.gif",
				"Kembalikan master ujian sekaligus seluruh relasi soal dari snapshot audit yang sama.",
				new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						pilihan.detach();
						tampilkan(new RevisiUjianHelper(callback));
					}
				});

		tampilkan(pilihan);
	}

	/**
	 * Membuka dialog pilihan Recovery Tugas dengan dua opsi: mengembalikan tugas yang melekat
	 * langsung pada pertemuan ({@link RevisiTugasHelper} dengan kelas {@link Pertemuan}), atau tugas
	 * tambahan yang dibuat di dalam pertemuan ({@link RevisiTugasHelper} dengan kelas
	 * {@link TugasPertemuan}). Kedua opsi dibatasi ke {@code pembelajaran} (mata kuliah/kelas yang
	 * sedang dibuka).
	 *
	 * @param pembelajaran konteks mata kuliah/kelas yang membatasi cakupan recovery.
	 * @param callback     callback yang diteruskan ke {@link RevisiTugasHelper} saat restore
	 *                     berhasil; boleh {@code null}.
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link RevisiTugasHelper}.
	 */
	public static void bukaRecoveryTugas(final VOPembelajaran pembelajaran, final EventListener callback)
			throws Exception {
		final MyWindow pilihan = buatPilihan("Recovery Tugas",
				"Pilih jenis tugas yang terhapus. Kedua pilihan dibatasi pada mata kuliah/kelas yang sedang dibuka.");

		tambahPilihan(pilihan, "Tugas Utama Pertemuan", "/img/jadwal.png",
				"Kembalikan tugas yang melekat langsung pada agenda/pertemuan.", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						pilihan.detach();
						tampilkan(new RevisiTugasHelper(Pertemuan.class, pembelajaran, callback));
					}
				});

		tambahPilihan(pilihan, "Tugas Tambahan", "/img/refresh.gif",
				"Kembalikan tugas tambahan yang dibuat di dalam suatu pertemuan.", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						pilihan.detach();
						tampilkan(new RevisiTugasHelper(TugasPertemuan.class, pembelajaran, callback));
					}
				});

		tampilkan(pilihan);
	}

	/**
	 * Membuka langsung jendela Recovery Tugas Kelompok (tanpa dialog pilihan, karena hanya ada satu
	 * jenis entitas untuk tugas kelompok) yang dibatasi ke pembelajaran aktif lewat
	 * {@link RevisiTugasHelper}.
	 *
	 * @param pembelajaran konteks mata kuliah/kelas yang membatasi cakupan recovery.
	 * @param callback     callback yang diteruskan ke {@link RevisiTugasHelper} saat restore
	 *                     berhasil; boleh {@code null}.
	 * @throws Exception diteruskan apa adanya dari konstruktor {@link RevisiTugasHelper}.
	 */
	public static void bukaRecoveryTugasKelompok(VOPembelajaran pembelajaran, EventListener callback)
			throws Exception {
		tampilkan(new RevisiTugasHelper(TugasKelompok.class, pembelajaran, callback));
	}

	/**
	 * Membangun window dialog pilihan (belum ditampilkan) berisi judul, deskripsi, dan wadah vertikal
	 * kosong yang siap diisi tombol pilihan lewat {@link #tambahPilihan}.
	 *
	 * @param judul      judul window.
	 * @param penjelasan kalimat penjelasan singkat yang ditampilkan di atas daftar pilihan.
	 * @return window yang sudah berisi label penjelasan dan separator, belum berisi tombol pilihan
	 *         dan belum ditampilkan (belum dipanggil {@link #tampilkan(MyWindow)}).
	 */
	private static MyWindow buatPilihan(String judul, String penjelasan) {
		MyWindow window = new MyWindow();
		window.setTitle(judul);
		window.setWidth("560px");
		window.setClosable(true);
		window.setSizable(true);
		window.setBorder("normal");
		Vbox isi = new Vbox();
		isi.setWidth("100%");
		isi.setStyle("padding:14px;box-sizing:border-box;");
		isi.setParent(window);
		Label label = new Label(penjelasan);
		label.setMultiline(true);
		label.setWidth("100%");
		label.setParent(isi);
		new Separator().setParent(isi);
		return window;
	}

	/**
	 * Menambahkan satu tombol pilihan (toolbarbutton selebar penuh) ke wadah pertama window dialog
	 * yang dibuat {@link #buatPilihan}.
	 *
	 * @param window   window dialog target, hasil {@link #buatPilihan}.
	 * @param label    teks tombol.
	 * @param image    path ikon tombol (relatif terhadap webapp, mis. {@code "/img/jadwal.png"}).
	 * @param tooltip  teks tooltip yang muncul saat kursor diarahkan ke tombol.
	 * @param listener callback yang dijalankan saat tombol diklik (biasanya menutup dialog lalu
	 *                 membuka helper revisi terkait).
	 * @throws Exception diteruskan apa adanya dari operasi ZK di dalamnya.
	 */
	private static void tambahPilihan(MyWindow window, String label, String image, String tooltip,
			EventListener listener) throws Exception {
		Component parent = window.getFirstChild();
		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig(label, image);
		button.setTooltiptext(tooltip);
		button.setWidth("100%");
		button.setStyle("text-align:left;padding:10px;margin:3px 0;");
		button.addEventListener("onClick", listener);
		button.setParent(parent);
	}

	/**
	 * Menempelkan window ke root halaman ZK saat ini dan menampilkannya sebagai dialog modal.
	 *
	 * @param window window yang akan ditampilkan (dialog pilihan atau helper revisi).
	 * @throws Exception diteruskan apa adanya dari operasi ZK di dalamnya (mis. bila tidak ada
	 *                    konteks eksekusi ZK aktif).
	 */
	private static void tampilkan(MyWindow window) throws Exception {
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
		window.setVisible(true);
		window.onModal();
	}
}
