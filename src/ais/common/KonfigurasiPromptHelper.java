package ais.common;

import java.util.Map;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Kelas utilitas statis (tidak dapat diinstansiasi) yang menyediakan dialog peringatan ZK
 * berbasis {@link ais.database.model.Konfigurasi}: menampilkan pesan peringatan ke pengguna, dan
 * — khusus untuk pengguna yang bukan mahasiswa/dosen (dianggap staf/admin, lihat
 * {@link #bolehUbahKonfigurasi()}) — menawarkan opsi untuk langsung mengubah nilai konfigurasi
 * terkait dari dalam dialog yang sama, tanpa perlu berpindah ke layar manajemen konfigurasi
 * terpisah.
 *
 * <p>
 * Kelas ini dipakai pada titik-titik di aplikasi yang perilakunya dikendalikan oleh suatu baris
 * {@link ais.database.model.Konfigurasi} dan ingin memberi jalan pintas perbaikan kepada admin
 * saat kondisi tidak sesuai harapan (mis. "fitur X memerlukan konfigurasi Y yang belum diisi") —
 * alih-alih hanya menampilkan pesan statis, pengguna admin langsung ditawari untuk mengubah nilai
 * konfigurasi tersebut di tempat.
 * </p>
 *
 * <h2>Alur dua method publik</h2>
 * <ol>
 * <li>{@link #tampilkanPeringatanDenganOpsiUbah(String, String, String, String)} — titik masuk
 * utama. Bila pengguna saat ini TIDAK berhak mengubah konfigurasi (mahasiswa/dosen, atau
 * {@code namaKonfigurasi} tidak diberikan), hanya menampilkan {@link MyMessageboxConfig} biasa
 * dengan tombol OK. Bila berhak, menampilkan pesan yang sama namun dengan tombol Ya/Tidak yang
 * menanyakan apakah ingin mengubah konfigurasi sekarang; memilih "Ya" memanggil
 * {@link #tampilkanFormUbahKonfigurasi(String, String)}.</li>
 * <li>{@link #tampilkanFormUbahKonfigurasi(String, String)} — membangun dan menampilkan window ZK
 * modal berisi form sederhana (nama konfigurasi read-only, nilai, keterangan) yang saat disimpan
 * melakukan {@code save-or-update} pada baris {@link ais.database.model.Konfigurasi} terkait lewat
 * Hibernate, lalu memperbarui cache konfigurasi in-memory ({@link MemoryDbUtil#getKonfigurasi()})
 * agar perubahan langsung berlaku tanpa perlu me-restart aplikasi.</li>
 * </ol>
 */
public class KonfigurasiPromptHelper {

	/** Konstruktor privat — kelas ini murni kumpulan method statis, tidak dimaksudkan untuk diinstansiasi. */
	private KonfigurasiPromptHelper() {
	}

	/**
	 * Menentukan apakah pengguna yang sedang login berhak mengubah konfigurasi lewat dialog ini.
	 * Hanya pengguna yang bukan mahasiswa dan bukan dosen (dianggap staf administratif/admin
	 * sistem) yang diberi opsi ubah; mahasiswa dan dosen hanya melihat pesan peringatan biasa.
	 *
	 * @return {@code true} bila pengguna saat ini ada dan bukan mahasiswa maupun dosen
	 */
	private static boolean bolehUbahKonfigurasi() {
		Tbmuser user = Common.getCurrentUser();
		return user != null && user.getMahasiswa() == null && user.ambilDosen() == null;
	}

	/**
	 * Menampilkan dialog peringatan kepada pengguna; bila pengguna berhak mengubah konfigurasi
	 * (lihat {@link #bolehUbahKonfigurasi()}) dan {@code namaKonfigurasi} diberikan, dialog
	 * menyertakan opsi Ya/Tidak untuk langsung membuka form ubah konfigurasi
	 * ({@link #tampilkanFormUbahKonfigurasi(String, String)}) bila pengguna memilih "Ya". Bila
	 * tidak berhak atau {@code namaKonfigurasi} kosong, hanya pesan biasa (tombol OK) yang
	 * ditampilkan.
	 *
	 * @param pesan           isi pesan peringatan yang ditampilkan ke pengguna
	 * @param judul           judul dialog
	 * @param namaKonfigurasi kunci {@link ais.database.model.Konfigurasi} yang relevan dengan
	 *                        peringatan ini; boleh {@code null}/kosong bila tidak ada konfigurasi
	 *                        terkait yang dapat diubah langsung
	 * @param defaultNilai    nilai default yang dipakai saat membuka form ubah bila konfigurasi
	 *                        dengan {@code namaKonfigurasi} belum ada di database
	 */
	public static void tampilkanPeringatanDenganOpsiUbah(final String pesan, final String judul,
			final String namaKonfigurasi, final String defaultNilai) {
		if (!bolehUbahKonfigurasi() || namaKonfigurasi == null || namaKonfigurasi.trim().isEmpty()) {
			try {
				MyMessageboxConfig.show(pesan, judul, MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}
			return;
		}

		try {
			MyMessageboxConfig.show(pesan + "\n\nApakah ingin mengubah konfigurasi ini sekarang?",
					judul, MyMessageboxConfig.YES | MyMessageboxConfig.NO, MyMessageboxConfig.QUESTION,
					new EventListener() {
						@Override
						public void onEvent(Event event) throws Exception {
							int pilihan = Integer.parseInt(event.getData().toString());
							if (pilihan == MyMessageboxConfig.YES) {
								tampilkanFormUbahKonfigurasi(namaKonfigurasi, defaultNilai);
							}
						}
					});
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
	}

	/**
	 * Membangun dan menampilkan window ZK modal ("Ubah Konfigurasi") berisi form untuk mengubah
	 * nilai dan keterangan satu baris {@link ais.database.model.Konfigurasi}. Bila konfigurasi
	 * dengan {@code namaKonfigurasi} belum ada di database, {@code defaultNilai} dipakai sebagai
	 * nilai awal form (lewat {@link Common#getKonfigurasi(String, String)}) dan baris baru akan
	 * dibuat saat disimpan; bila sudah ada, baris yang ada dimuat ulang lewat
	 * {@code session.load(...)} dan diperbarui.
	 *
	 * <p>
	 * Saat tombol "Simpan" ditekan: nilai wajib diisi (kosong akan ditolak dengan pesan
	 * peringatan), baris {@link ais.database.model.Konfigurasi} disimpan/diperbarui dalam sesi
	 * Hibernate lalu di-{@code flush}, cache in-memory {@link MemoryDbUtil#getKonfigurasi()}
	 * diperbarui agar perubahan langsung terlihat tanpa restart aplikasi (bila pembaruan cache
	 * gagal, seluruh referensi lokal cache di-reset lewat
	 * {@link MemoryDbUtil#resetLocalReferences()} sebagai fallback), window ditutup, dan pesan
	 * konfirmasi ditampilkan.
	 * </p>
	 *
	 * @param namaKonfigurasi kunci konfigurasi yang akan diubah/dibuat
	 * @param defaultNilai    nilai awal yang ditampilkan di form bila konfigurasi belum ada
	 * @throws Exception diteruskan dari kegagalan pembangunan komponen ZK atau operasi Hibernate
	 */
	public static void tampilkanFormUbahKonfigurasi(final String namaKonfigurasi, final String defaultNilai)
			throws Exception {
		final Konfigurasi konfigurasi = Common.getKonfigurasi(namaKonfigurasi, defaultNilai);
		final MyWindow window = new MyWindow();
		window.setTitle("Ubah Konfigurasi");
		window.setWidth("560px");
		window.setHeight("310px");
		ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(window);

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		Grid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Jenis Konfigurasi"));
		row.appendChild(new Label(konfigurasi.getNama()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Nilai Konfigurasi"));
		final Textbox nilai = new Textbox(konfigurasi.getNilai());
		nilai.setWidth("95%");
		row.appendChild(nilai);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new MyLabelConfig("Keterangan"));
		final Textbox keterangan = new Textbox(konfigurasi.getKeterangan() == null ? "" : konfigurasi.getKeterangan());
		keterangan.setRows(4);
		keterangan.setWidth("95%");
		row.appendChild(keterangan);

		South south = new South();
		south.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(south, true);

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(south);

		MyToolbarbuttonConfig batal = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		batal.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				window.detach();
			}
		});
		batal.setParent(toolbar);

		MyToolbarbuttonConfig simpan = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		simpan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (nilai.getValue() == null || nilai.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Nilai konfigurasi belum diisi", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				Session session = HibernateUtil.currentSession();
				Konfigurasi target = konfigurasi.getId() == null ? konfigurasi
						: (Konfigurasi) session.load(Konfigurasi.class, konfigurasi.getId());
				target.setNama(namaKonfigurasi);
				target.setNilai(nilai.getValue().trim());
				target.setKeterangan(keterangan.getValue());
				Common.refreshSaveOrUpdate(session, target);
				session.flush();

				try {
					Map<String, Konfigurasi> cache = MemoryDbUtil.getKonfigurasi();
					if (cache != null) {
						cache.put(target.getNama(), target);
					}
				} catch (Throwable t) {
					MemoryDbUtil.resetLocalReferences();
				}

				window.detach();
				MyMessageboxConfig.show("Konfigurasi berhasil disimpan", "Informasi", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
			}
		});
		simpan.setParent(toolbar);

		window.onModal();
	}
}
