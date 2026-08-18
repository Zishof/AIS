package ais.action.master.helper.profile;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Html;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.DashboardCacheUtil;
import ais.ui.util.MyRowStyled;

/**
 * <h3>ProfileSekolahLanjutanDashboard — Ringkasan Data Sekolah (Detail Menyeluruh)</h3>
 *
 * <p><b>Untuk apa:</b> Kelas utilitas statik yang menambahkan panel ringkasan data
 * sekolah menyeluruh ke halaman profil admin sekolah atau admin gabungan. Panel ini
 * menyajikan sebelas kartu angka clickable yang mencakup aspek operasional sekolah
 * secara lengkap: jumlah siswa aktif, calon siswa (PPDB), guru, kelas yang tersedia,
 * mata pelajaran terdaftar, penugasan guru mengajar, jadwal pelajaran, transaksi
 * pembayaran siswa, pelanggaran siswa yang tercatat, catatan siswa, dan kegiatan
 * siswa. Setiap kartu dapat diklik untuk membuka popup modal yang menjelaskan makna
 * angka tersebut beserta informasi menu navigasi terkait.</p>
 *
 * <p><b>Perbandingan dengan ProfileAkademikLanjutanDashboard:</b> Kelas ini adalah
 * mitra sejajar untuk konteks sekolah (TK/SD/SMP/SMA), sedangkan
 * {@link ProfileAkademikLanjutanDashboard} ditujukan untuk konteks perguruan tinggi
 * (skripsi, KKN, wisuda, dsb.). Keduanya menggunakan pola arsitektur yang sama:
 * asinkron dengan Timer ZK, cache tiga lapis, dan komponen UI yang dibangun
 * lewat {@link ProfileUiHelper}.</p>
 *
 * <p><b>Desain asinkron (non-blocking):</b> Metode {@link #append(Rows)} segera
 * memasang indikator "loading" ke dalam grid profil, lalu mengaktifkan {@link Timer}
 * ZK (delay 120 ms, non-repeating). Saat timer terpicu di event thread ZK, semua
 * query COUNT ke database dijalankan dan HTML kartu dirakit, kemudian hasilnya
 * menggantikan konten slot loading. Pola ini memastikan halaman profil dapat
 * dirender dan ditampilkan lebih dulu, sementara angka-angka menyusul secepat
 * mungkin tanpa menghalangi komponen profil lainnya.</p>
 *
 * <p><b>Strategi cache tiga lapis:</b> Setelah HTML selesai dihitung, disimpan
 * ke {@link DashboardCacheUtil} pada dua level — L2 (per-sesi, TTL lebih pendek)
 * dan L3 (JVM-shared, TTL lebih panjang). Kunci cache bersifat global (tidak
 * per-pengguna) karena statistik yang ditampilkan adalah data agregat seluruh
 * sekolah. Pertama kali dibuka atau setelah invalidasi cache, perhitungan berjalan
 * penuh; selanjutnya pembukaan berikutnya langsung menggunakan nilai cache.</p>
 *
 * <p><b>Kartu yang disediakan (11 kartu):</b> Siswa Aktif, Calon Siswa, Guru,
 * Kelas, Mata Pelajaran, Guru Mengajar, Jadwal Pelajaran, Pembayaran Siswa,
 * Pelanggaran Siswa, Catatan Siswa, dan Kegiatan Siswa.</p>
 *
 * <p><b>Threading dan pemeliharaan:</b> Semua metode statik, kelas final,
 * tidak ada state instance. Cache eksternal diasumsikan thread-safe.
 * Java 1.7, ZKoss 5.5.</p>
 */
public final class ProfileSekolahLanjutanDashboard {

	/**
	 * Konstruktor privat — mencegah instansiasi kelas utilitas statik ini.
	 * Semua fungsionalitas tersedia melalui metode {@code public static}.
	 * Pola ini konsisten dengan {@link ProfileAkademikLanjutanDashboard},
	 * {@link ProfileUiHelper}, dan kelas utilitas statik lain dalam paket profil.
	 */
	private ProfileSekolahLanjutanDashboard() {
	}

	/**
	 * Menambahkan panel "Ringkasan Data Sekolah" ke baris grid profil dengan
	 * pola asinkron dua-tahap: loading instan, perhitungan di latar.
	 *
	 * <p><b>Tujuan:</b> Memasang blok kartu angka operasional sekolah (siswa, guru,
	 * kelas, mapel, jadwal, pembayaran, pelanggaran, catatan, kegiatan) ke dalam
	 * struktur grid profil admin sekolah tanpa memblokir render halaman.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Guard {@code null} pada {@code rows} — jika null, return langsung.</li>
	 *   <li>Tambahkan header kelompok ({@code MyGroupConfig}) bertuliskan
	 *       "Ringkasan Data Sekolah" sebagai pemisah visual dalam grid.</li>
	 *   <li>Tambahkan baris info ({@link ProfileUiHelper#appendPanelInfoRow})
	 *       berisi deskripsi panel untuk orientasi pengguna.</li>
	 *   <li>Buat {@code Row} baru dengan span 2 berisi {@code Vbox} sebagai
	 *       wadah slot loading dan konten akhir.</li>
	 *   <li>Pasang komponen {@code Html} slot berisi spinner CSS dan teks
	 *       "Menghitung ringkasan data sekolah…".</li>
	 *   <li>Buat {@link Timer} non-repeating (delay 120 ms). Handler {@code onTimer}
	 *       memanggil {@link #htmlBercache()} dan menyetel {@code slot.setContent()}
	 *       dengan HTML akhir. Jika terjadi error, slot diisi pesan fallback.
	 *       Timer selalu diberhentikan (stop + detach) setelah terpicu.</li>
	 *   <li>Aktifkan timer dengan {@code timer.start()}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Penanganan error:</b> Error dalam handler timer ditangkap dan
	 * ditampilkan via {@link Common#tampilErrorJikaAdmin}; slot diisi pesan
	 * fallback agar pengguna tidak melihat spinner tak terbatas. Timer selalu
	 * diberhentikan di blok terpisah agar tidak bocor meski terjadi exception.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Untuk menambah kartu baru, ubah {@link #hitungDanRender()},
	 * bukan metode ini. Metode ini hanya mengelola infrastruktur tampilan asinkron.</p>
	 *
	 * @param rows wadah {@link Rows} grid profil tempat panel ditambahkan;
	 *             jika {@code null}, metode tidak melakukan apa-apa
	 */
	public static void append(final Rows rows) {
		if (rows == null) {
			return;
		}
		rows.appendChild(new ais.ui.util.MyGroupConfig("Ringkasan Data Sekolah"));
		ProfileUiHelper.appendPanelInfoRow(rows, 2, "Ringkasan Data Sekolah",
				"Siswa, guru, kelas, mata pelajaran, jadwal, pembayaran, pelanggaran, catatan, dan kegiatan. Dihitung di latar (cache cepat). Klik tiap angka untuk rincian.");

		Row row = new MyRowStyled();
		try {
			ais.ui.util.ZkCompat.setSpans(row, "2");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileSekolahLanjutanDashboard.java:122");
		}
		row.setParent(rows);

		final Vbox container = new Vbox();
		container.setWidth("100%");
		container.setParent(row);

		final Html slot = new Html("");

		// Tombol "Refresh": muat ulang dari DATABASE & perbarui cache parameter sama.
		org.zkoss.zul.Hbox bar = new org.zkoss.zul.Hbox();
		bar.setWidth("100%");
		bar.setPack("end");
		ais.ui.util.MyToolbarbuttonConfig btnRefresh = new ais.ui.util.MyToolbarbuttonConfig("Refresh",
				"/img/svg/refresh.svg");
		btnRefresh.setTooltiptext("Muat ulang data dari database & perbarui cache ringkasan data sekolah.");
		btnRefresh.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					String k = cacheKey();
					DashboardCacheUtil.invalidateL2(k);
					DashboardCacheUtil.invalidateL3(k);
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileSekolahLanjutanDashboard.java:146");
				}
				muatDiLatar(slot, container);
			}
		});
		btnRefresh.setParent(bar);
		bar.setParent(container);
		slot.setParent(container);

		// Cache parameter sama ADA → tampil LANGSUNG tanpa "Loading"; kosong → "Loading" lalu hitung di latar.
		String cached = ambilCache();
		if (cached != null) {
			slot.setContent(cached);
		} else {
			muatDiLatar(slot, container);
		}
	}

	/** Tampilkan "Loading", lalu hitung HTML DI LATAR (Timer ZK) dan tampilkan ke slot. */
	private static void muatDiLatar(final Html slot, final Vbox container) {
		slot.setContent("<div class=\"ais-profile-modern ais-profile-loading\">"
				+ "<span class=\"ais-profile-spinner\"></span><b>Menghitung ringkasan data sekolah…</b>"
				+ "<br/><span style=\"font-size:11px;\">Mohon tunggu sebentar.</span></div>");
		final Timer timer = new Timer();
		timer.setDelay(120);
		timer.setRepeats(false);
		timer.setParent(container);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					slot.setContent(htmlBercache());
				} catch (Exception ex) {
					Common.tampilErrorJikaAdmin(ex);
					slot.setContent("<div class=\"ais-profile-modern ais-profile-loading\">Ringkasan data sekolah gagal dimuat.</div>");
				}
				try {
					timer.stop();
					timer.detach();
				} catch (Exception ig) { ais.common.ErrorAuditUtil.record(ig, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileSekolahLanjutanDashboard.java:185");
				}
			}
		});
		timer.start();
	}

	/** Kunci cache di-scope per HOST (tenant) agar tidak bocor antar-tenant (L3 app-wide). */
	private static String cacheKey() {
		String host = "";
		try {
			String h = Common.getRequestHost();
			if (h != null) {
				host = h;
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileSekolahLanjutanDashboard.java:200");
		}
		return DashboardCacheUtil.keyWithFilter("ProfileSekolahLanjutan", "SEKOLAH", null, host);
	}

	/** Peek cache (L2 lalu L3) TANPA menghitung; null bila belum ada. */
	private static String ambilCache() {
		String key = cacheKey();
		try {
			Object l2 = DashboardCacheUtil.getL2(key);
			if (l2 instanceof String) {
				return (String) l2;
			}
			Object l3 = DashboardCacheUtil.getL3(key);
			if (l3 instanceof String) {
				DashboardCacheUtil.putL2(key, l3); // pemanasan L2
				return (String) l3;
			}
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileSekolahLanjutanDashboard.java:218");
		}
		return null;
	}

	/**
	 * Mengambil HTML kartu ringkasan data sekolah dari cache tiga lapis, atau
	 * menghitung ulang bila cache belum tersedia.
	 *
	 * <p><b>Tujuan:</b> Memisahkan logika pengambilan cache dari logika perhitungan,
	 * menjaga {@link #append(Rows)} tetap bersih. Menjadi satu-satunya pintu masuk
	 * untuk HTML akhir panel, baik dari cache maupun perhitungan baru.</p>
	 *
	 * <p><b>Cara kerja (prioritas cache):</b>
	 * <ol>
	 *   <li>Buat kunci cache deterministik via {@link DashboardCacheUtil#key}
	 *       ("ProfileSekolahLanjutan", "SEKOLAH"). Kunci global (tidak per-pengguna)
	 *       karena data yang ditampilkan adalah statistik agregat seluruh sekolah.</li>
	 *   <li>Periksa cache L2 (per-sesi, TTL pendek). Jika ada, kembalikan langsung.</li>
	 *   <li>Periksa cache L3 (JVM-shared, TTL panjang). Jika ada, simpan juga ke
	 *       L2 ("pemanasan") lalu kembalikan — ini menghindari query ke L3 berulang
	 *       dalam satu sesi.</li>
	 *   <li>Jika keduanya miss, panggil {@link #hitungDanRender()} untuk perhitungan
	 *       penuh, simpan hasilnya ke L2 dan L3, lalu kembalikan.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Keamanan thread:</b> Dalam kondisi race, dua thread bisa sama-sama
	 * menghitung ulang, namun hasilnya identik sehingga tidak ada inkonsistensi.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Untuk invalidasi paksa, hapus cache L2 dan L3 dengan
	 * kunci "ProfileSekolahLanjutan:SEKOLAH:null" di {@link DashboardCacheUtil}.</p>
	 *
	 * @return String HTML lengkap panel kartu angka sekolah; tidak pernah {@code null}
	 */
	private static String htmlBercache() {
		String cached = ambilCache();
		if (cached != null) {
			return cached;
		}
		String html = hitungDanRender();
		try {
			String key = cacheKey();
			DashboardCacheUtil.putL2(key, html);
			DashboardCacheUtil.putL3(key, html);
		} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/profile/ProfileSekolahLanjutanDashboard.java:263");
		}
		return html;
	}

	/**
	 * Menjalankan semua query COUNT ke database dan merakit HTML panel ringkasan
	 * data sekolah beserta modal popup untuk setiap kartu angka.
	 *
	 * <p><b>Tujuan:</b> Inti komputasi kelas ini — semua yang mahal secara I/O
	 * dilakukan di sini. Hasilnya dikembalikan sebagai String HTML yang siap
	 * dimasukkan ke komponen ZK {@code Html}.</p>
	 *
	 * <p><b>Cara kerja (urutan kartu — 11 kartu):</b>
	 * <ol>
	 *   <li><b>Siswa Aktif</b>: COUNT {@code Siswa} di mana {@code aktif = true}.
	 *       Menampilkan siswa yang saat ini terdaftar aktif.</li>
	 *   <li><b>Calon Siswa</b>: COUNT {@code CalonSiswa} di mana {@code aktif = true}.
	 *       Menampilkan proses PPDB yang sedang berjalan.</li>
	 *   <li><b>Guru</b>: COUNT total semua entitas {@code Guru} tercatat.</li>
	 *   <li><b>Kelas</b>: COUNT total {@code KelasSiswa} — semua kelas terdaftar.</li>
	 *   <li><b>Mata Pelajaran</b>: COUNT total {@code Matapelajaran}.</li>
	 *   <li><b>Guru Mengajar</b>: COUNT total {@code GuruMengajar} — penugasan
	 *       guru ke mata pelajaran dan kelas tertentu.</li>
	 *   <li><b>Jadwal Pelajaran</b>: COUNT total {@code JadwalPelajaran} —
	 *       slot waktu mengajar yang terjadwal.</li>
	 *   <li><b>Pembayaran Siswa</b>: COUNT total {@code PembayaranSiswa} —
	 *       semua transaksi pembayaran siswa yang pernah tercatat.</li>
	 *   <li><b>Pelanggaran Siswa</b>: COUNT total {@code PelanggaranSiswa}.</li>
	 *   <li><b>Catatan Siswa</b>: COUNT total {@code CatatanSiswa}.</li>
	 *   <li><b>Kegiatan Siswa</b>: COUNT total {@code KegiatanSiswa}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Perakitan HTML:</b> Sama seperti {@link ProfileAkademikLanjutanDashboard}:
	 * setiap kartu dibuat lewat {@link #kartu}, hasilnya dikumpulkan ke list
	 * {@code stats} dan buffer {@code modals}. Setelah semua selesai, dibungkus
	 * oleh {@link ProfileUiHelper#statsWrap} dan {@link ProfileUiHelper#panel}.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Untuk menambah kategori data sekolah baru (misalnya
	 * "Ekskul" atau "Absensi"), tambahkan satu baris pemanggilan {@link #kartu}
	 * dengan entitas model yang sesuai.</p>
	 *
	 * @return String HTML lengkap panel beserta semua modal; tidak pernah {@code null}
	 */
	private static String hitungDanRender() {
		List<String> stats = new ArrayList<String>();
		StringBuffer modals = new StringBuffer();

		kartu(stats, modals, "Siswa Aktif",
				ProfileUiHelper.hitung(ais.database.model.sekolah.Siswa.class, satu(Restrictions.eq("aktif", Boolean.TRUE))),
				"Siswa berstatus aktif.", "Daftar rinci ada di menu Siswa.");
		kartu(stats, modals, "Calon Siswa",
				ProfileUiHelper.hitung(ais.database.model.sekolah.CalonSiswa.class, satu(Restrictions.eq("aktif", Boolean.TRUE))),
				"Calon siswa (PSB) aktif.", "Daftar rinci ada di menu Calon Siswa / PPDB.");
		kartu(stats, modals, "Guru", ProfileUiHelper.hitung(ais.database.model.sekolah.Guru.class, null),
				"Total guru tercatat.", "Daftar rinci ada di menu Guru.");
		kartu(stats, modals, "Kelas", ProfileUiHelper.hitung(ais.database.model.sekolah.KelasSiswa.class, null),
				"Total kelas tercatat.", "Daftar rinci ada di menu Kelas.");
		kartu(stats, modals, "Mata Pelajaran", ProfileUiHelper.hitung(ais.database.model.sekolah.Matapelajaran.class, null),
				"Total mata pelajaran.", "Daftar rinci ada di menu Mata Pelajaran.");
		kartu(stats, modals, "Guru Mengajar", ProfileUiHelper.hitung(ais.database.model.sekolah.GuruMengajar.class, null),
				"Penugasan guru mengajar.", "Daftar rinci ada di menu Guru Mengajar.");
		kartu(stats, modals, "Jadwal Pelajaran", ProfileUiHelper.hitung(ais.database.model.sekolah.JadwalPelajaran.class, null),
				"Total jadwal pelajaran.", "Daftar rinci ada di menu Jadwal Pelajaran.");
		kartu(stats, modals, "Pembayaran Siswa", ProfileUiHelper.hitung(ais.database.model.sekolah.PembayaranSiswa.class, null),
				"Total transaksi pembayaran siswa.", "Daftar rinci ada di menu Pembayaran Siswa.");
		kartu(stats, modals, "Pelanggaran Siswa", ProfileUiHelper.hitung(ais.database.model.sekolah.PelanggaranSiswa.class, null),
				"Total pelanggaran siswa tercatat.", "Daftar rinci ada di menu Pelanggaran Siswa.");
		kartu(stats, modals, "Catatan Siswa", ProfileUiHelper.hitung(ais.database.model.sekolah.CatatanSiswa.class, null),
				"Total catatan siswa.", "Daftar rinci ada di menu Catatan Siswa.");
		kartu(stats, modals, "Kegiatan Siswa", ProfileUiHelper.hitung(ais.database.model.sekolah.KegiatanSiswa.class, null),
				"Total kegiatan siswa.", "Daftar rinci ada di menu Kegiatan Siswa.");

		String isi = ProfileUiHelper.statsWrap(stats.toArray(new String[stats.size()]));
		return ProfileUiHelper.panel("Data Sekolah Menyeluruh",
				"Angka untuk pimpinan sekolah: siswa, guru, kelas, mapel, jadwal, pembayaran, pelanggaran, catatan, kegiatan. Klik tiap angka untuk rincian.",
				isi) + modals.toString();
	}

	/**
	 * Membuat satu kartu angka clickable beserta modal popup penjelasannya untuk
	 * konteks sekolah, lalu menambahkan keduanya ke struktur output yang diberikan.
	 *
	 * <p><b>Tujuan:</b> Helper privat yang memusatkan boilerplate pembentukan
	 * kartu + modal agar {@link #hitungDanRender()} tetap ringkas. Identik dalam
	 * cara kerja dengan metode {@code kartu} di {@link ProfileAkademikLanjutanDashboard},
	 * tetapi menggunakan prefix ID "aisSekLanjut" agar ID modal tidak bertabrakan
	 * dengan kartu dari panel lain yang mungkin muncul di halaman yang sama.</p>
	 *
	 * <p><b>Cara kerja:</b>
	 * <ol>
	 *   <li>Bangkitkan ID modal unik via {@link ProfileUiHelper#nextModalId}
	 *       dengan prefix "aisSekLanjut". Keunikan dijamin oleh {@code AtomicLong}
	 *       yang tidak direset selama JVM berjalan.</li>
	 *   <li>Format angka {@code jumlah} dengan {@link ProfileUiHelper#fmt}
	 *       (pemisah ribuan lokal Indonesia).</li>
	 *   <li>Buat HTML kartu clickable via {@link ProfileUiHelper#statClickable}
	 *       dan tambahkan ke {@code stats}.</li>
	 *   <li>Buat HTML modal via {@link ProfileUiHelper#modal} dengan konten
	 *       dari {@link ProfileUiHelper#modalAngka} dan tambahkan ke {@code modals}.</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah kecuali ada perubahan pada API
	 * {@link ProfileUiHelper}. Untuk menambah kartu baru, cukup tambahkan
	 * pemanggilan metode ini di {@link #hitungDanRender()} dengan parameter
	 * yang sesuai.</p>
	 *
	 * @param stats  list HTML kartu yang akan dikumpulkan untuk dibungkus statsWrap
	 * @param modals buffer HTML modal yang akan digabung di akhir panel
	 * @param label  judul kartu yang ditampilkan kepada pengguna
	 * @param jumlah angka hasil COUNT dari database untuk ditampilkan di kartu
	 * @param hint   keterangan singkat tentang apa yang dihitung (tampil di modal)
	 * @param panduan teks panduan navigasi menu terkait (tampil di modal)
	 */
	private static void kartu(List<String> stats, StringBuffer modals, String label, long jumlah, String hint,
			String panduan) {
		String mid = ProfileUiHelper.nextModalId("aisSekLanjut");
		String nilai = ProfileUiHelper.fmt(Long.valueOf(jumlah));
		stats.add(ProfileUiHelper.statClickable(label, nilai, hint, mid));
		modals.append(ProfileUiHelper.modal(mid, "Rincian " + label,
				ProfileUiHelper.modalAngka(label, nilai, hint, panduan)));
	}

	/**
	 * Membungkus satu {@link Criterion} menjadi array satu elemen agar kompatibel
	 * dengan signature {@link ProfileUiHelper#hitung(Class, Criterion[])}.
	 *
	 * <p><b>Tujuan:</b> Menghilangkan boilerplate {@code new Criterion[]{ c }}
	 * yang berulang setiap kali hanya satu criterion dibutuhkan. Identik dengan
	 * metode {@code satu} di {@link ProfileAkademikLanjutanDashboard} dan
	 * dipertahankan secara terpisah agar masing-masing kelas bersifat mandiri
	 * tanpa saling bergantung.</p>
	 *
	 * <p><b>Cara kerja:</b> Membuat dan mengembalikan {@code new Criterion[]{ c }}.
	 * Tidak ada validasi null karena {@link ProfileUiHelper#hitung} sudah
	 * menanganinya secara aman.</p>
	 *
	 * <p><b>Pemeliharaan:</b> Tidak perlu diubah selama signature
	 * {@link ProfileUiHelper#hitung} tidak berubah. Untuk multi-criterion,
	 * gunakan langsung {@code new Criterion[]{ c1, c2 }} di pemanggil.</p>
	 *
	 * @param c criterion Hibernate tunggal yang akan dibungkus; boleh null
	 * @return array satu elemen berisi {@code c}
	 */
	private static Criterion[] satu(Criterion c) {
		return new Criterion[] { c };
	}
}
