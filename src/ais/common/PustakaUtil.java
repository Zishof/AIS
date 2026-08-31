package ais.common;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.Clients;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.library.PeminjamanPengadaanItemDetail;
import ais.ui.util.MyMessageboxConfig;

/**
 * Utilitas statis modul perpustakaan (pustaka) AIS yang menegakkan kebijakan penguncian akses
 * login mahasiswa berdasarkan keterlambatan pengembalian pinjaman pustaka (buku maupun item
 * pengadaan pustaka lain). Kelas ini merupakan bagian dari rangkaian pemeriksaan "boleh login
 * atau tidak" yang dijalankan pada alur autentikasi mahasiswa, di mana institusi dapat memilih
 * untuk memblokir mahasiswa masuk ke sistem selama mereka masih memiliki pinjaman pustaka yang
 * terlambat dikembalikan melebihi ambang batas hari tertentu.
 *
 * <p>
 * Perilaku kelas ini sepenuhnya digerbangi oleh dua konfigurasi ({@link Konfigurasi}) yang dapat
 * diatur institusi:
 * </p>
 * <ul>
 * <li>{@code apakah_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat_sebanyak_beberapa_hari}
 * — saklar utama (default {@link Konfigurasi#TIDAK_AKTIF}); bila tidak aktif, seluruh pemeriksaan
 * di kelas ini dilewati dan mahasiswa selalu dianggap boleh login (dari sisi pustaka).</li>
 * <li>{@code jumlah_hari_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat}
 * — ambang batas jumlah hari keterlambatan (default {@code "100"} hari) yang, bila terlampaui
 * oleh salah satu pinjaman aktif mahasiswa, akan memblokir login.</li>
 * </ul>
 *
 * <p>
 * Bila saklar utama aktif, kelas ini menelusuri seluruh baris
 * {@link PeminjamanPengadaanItemDetail} milik mahasiswa yang bersangkutan yang BELUM
 * dikembalikan (kolom {@code kembaliPengadaanItemDetail} bernilai {@code null}), dan
 * mengumpulkan pesan keterangan untuk setiap item yang jumlah hari keterlambatannya sudah
 * mencapai atau melampaui ambang batas. Bila ada satu saja item yang melewati ambang, sebuah
 * kotak pesan informasi ditampilkan kepada mahasiswa berisi rincian item/buku dan perpustakaan
 * asalnya, dan mahasiswa dipaksa keluar (logoff) begitu kotak pesan tersebut ditutup — sehingga
 * proses login dibatalkan secara efektif.
 * </p>
 */
public class PustakaUtil {

	/**
	 * Memeriksa apakah seorang mahasiswa boleh melanjutkan proses login ditinjau dari kepatuhan
	 * pengembalian pinjaman pustaka, sesuai kebijakan yang diatur lewat konfigurasi
	 * {@code apakah_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat_sebanyak_beberapa_hari}
	 * dan ambang hari keterlambatan
	 * {@code jumlah_hari_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat}.
	 *
	 * <p>
	 * Bila saklar konfigurasi tidak aktif, method langsung mengembalikan {@code true} tanpa
	 * menyentuh database. Bila aktif, seluruh pinjaman aktif (belum dikembalikan) milik
	 * {@code mahasiswa} diperiksa; bila ditemukan satu atau lebih pinjaman yang keterlambatannya
	 * mencapai/melampaui ambang batas, ditampilkan kotak pesan informasi berisi rincian
	 * keterlambatan dan mahasiswa dipaksa logoff (lewat {@link Common#goLogoff()}) saat kotak
	 * pesan ditutup, kemudian method mengembalikan {@code false}.
	 * </p>
	 *
	 * @param mahasiswa mahasiswa yang sedang mencoba login dan hendak diperiksa status pinjaman
	 *                  pustakanya
	 * @return {@code true} bila mahasiswa boleh melanjutkan login (tidak ada pinjaman terlambat
	 *         melewati ambang, atau fitur ini tidak aktif); {@code false} bila mahasiswa memiliki
	 *         pinjaman yang terlambat melewati ambang batas hari (login diblokir, kotak pesan
	 *         sudah ditampilkan dan logoff sudah dijadwalkan)
	 * @throws Exception diteruskan apa adanya dari kegagalan yang tidak tertangkap di jalur
	 *                    pemrosesan (dalam praktiknya sebagian besar kegagalan Hibernate sudah
	 *                    ditangkap secara internal dan dicatat lewat
	 *                    {@link ais.common.ErrorAuditUtil#record(Throwable, String)})
	 */
	@SuppressWarnings("unchecked")
	public static boolean checkPeminjamaBuku(Mahasiswa mahasiswa) throws Exception {
		if (Common.bolehKonfigurasi("apakah_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat_sebanyak_beberapa_hari", Konfigurasi.TIDAK_AKTIF)) {

			int jumlahHari = 100;
			try {
				jumlahHari = Integer.parseInt(Common.getKonfigurasi(
						"jumlah_hari_mahasiswa_tidak_bisa_login_sebelum_mengembalikan_buku_perpustakaan_jika_terlambat",
						"100").getNilai().trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			Session session = HibernateUtil.currentNativeSession();
			String content = "";

			try {
				List<PeminjamanPengadaanItemDetail> objects = session
						.createCriteria(PeminjamanPengadaanItemDetail.class)
						.add(Restrictions.isNull("kembaliPengadaanItemDetail"))
						.createAlias("peminjamanPengadaanItem", "peminjamanPengadaanItem")
						.createAlias("peminjamanPengadaanItem.anggota", "anggota")
						.add(Restrictions.eq("anggota.mahasiswa", mahasiswa)).list();

				for (PeminjamanPengadaanItemDetail peminjamanPengadaanItemDetail : objects) {
					if (jumlahHari <= peminjamanPengadaanItemDetail.getJumlahHariTerlambat()) {
						content += "Item atau buku \"" + peminjamanPengadaanItemDetail.getItem().getNama()
								+ "\" terlambat "
								+ Common.numberFormat.get().format(peminjamanPengadaanItemDetail.getJumlahHariTerlambat())
								+ " hari di perpustakaan "
								+ peminjamanPengadaanItemDetail.getPeminjamanPengadaanItem().getPerpustakaan().getNama()
								+ ".\n";
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/PustakaUtil.java:54");
			}
			HibernateUtil.closeSession();

			if (!content.trim().isEmpty()) {
				MyMessageboxConfig.show(
						"Maaf, Anda tidak bisa masuk ke sistem karena alasan belum mengembalikan buku sbb :\n\n"
								+ content,
						"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								// TODO Auto-generated method
								// stub

								Clients.showBusy("Loading ...");

								Common.goLogoff();
							}
						});

				return false;
			}
		}
		return true;
	}

}
