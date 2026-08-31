package ais.common;

import java.io.File;
import java.net.URLEncoder;
import java.util.Set;

import org.hibernate.Session;
import org.zkoss.zk.ui.event.Event;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.KegiatanTemporary;
import ais.database.model.Mahasiswa;
import ais.database.model.jatelindo.JatelindoRequest;
import ais.ui.util.MyMessageboxConfig;

/**
 * Integrasi pembayaran mahasiswa lewat gateway <b>Jatelindo</b> (Virtual Account Bank Mandiri)
 * pada keranjang pembayaran AIS: membuat request VA lokal (tanpa panggilan HTTP sinkron ke
 * gateway eksternal saat pembuatan — VA dibuat/di-generate di sisi aplikasi berdasarkan
 * {@code merchant_id} + digit acak), menghasilkan kode barcode/QR terkait, lalu menampilkan
 * jendela instruksi pembayaran ke mahasiswa/calon mahasiswa.
 *
 * <p>
 * Kelas ini murni statis dan menjadi salah satu dari beberapa implementasi payment gateway di
 * paket {@code ais.common} (bandingkan dengan {@link BsiKeranjangPembayaran} untuk BSI dan
 * {@link BJBUtil} untuk BJB) yang mengikuti pola serupa: nomor Virtual Account dibentuk dari
 * kombinasi {@code merchant_id} (dibaca dari konfigurasi {@code jatelindo_merchant_id}, default
 * {@code "31503"}) dan digit acak yang panjangnya diatur lewat konfigurasi
 * {@code generated_angka_digit_jatelindo} (default 8 digit); biaya administrasi dibaca dari
 * konfigurasi {@code jatelindo_biaya_administrasi} (default {@code 0.0}); request disimpan sebagai
 * entitas {@link JatelindoRequest} dalam transaksi Hibernate tersendiri; dan kegagalan dicatat
 * lewat mekanisme bersama {@link InfoTeknisPembayaran} sehingga pesan error teknis dapat
 * ditampilkan konsisten ke pengguna/administrator di semua kanal payment gateway.
 * </p>
 *
 * <p>
 * Tidak ditemukan kredensial/API key tertanam pada kelas ini — {@code merchant_id} yang dipakai
 * sebagai default konfigurasi ({@code "31503"}) adalah kode merchant, bukan rahasia autentikasi,
 * dan seluruh nilai konfigurasi dibaca lewat {@code Common.getKonfigurasi} saat runtime.
 * </p>
 */
public class JatelindoKeranjangPembayaran {

	/**
	 * Titik masuk validasi + eksekusi pembuatan Virtual Account Jatelindo untuk satu transaksi
	 * keranjang pembayaran mahasiswa. Menolak permintaan dengan nominal kurang dari {@code 0.01}
	 * (dianggap tidak valid/nol), lalu mendelegasikan pembuatan VA dan tampilan instruksi
	 * pembayaran ke {@link #onPilihJatelindo}.
	 *
	 * @param amn                        nominal yang harus dibayar
	 * @param mahasiswa                  mahasiswa yang membayar, boleh {@code null} bila
	 *                                   pembayaran atas nama calon mahasiswa
	 * @param biodataCalonMahasiswa      biodata calon mahasiswa (jalur PMB), boleh {@code null}
	 * @param selectedKegiatanTemporary  kumpulan item kegiatan/tagihan sementara yang dipilih
	 *                                   untuk dibayar pada transaksi ini
	 * @param event                      event ZK pemicu aksi (diteruskan untuk konteks UI)
	 * @return {@code true} bila permintaan diteruskan untuk diproses (nominal valid);
	 *         {@code false} bila nominal kurang dari {@code 0.01} dan permintaan ditolak lebih
	 *         awal tanpa membuat VA
	 * @throws Exception diteruskan dari {@link #onPilihJatelindo} bila terjadi kegagalan tak
	 *                    terduga di luar penanganan internalnya
	 */
	@SuppressWarnings({})
	public static boolean onSaveJatelindo(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		onPilihJatelindo(amn, mahasiswa, biodataCalonMahasiswa, selectedKegiatanTemporary, event);

		return true;
	}

	/**
	 * Membuat request Virtual Account Jatelindo lewat {@link #sendRequest}, menghasilkan barcode
	 * (lewat {@link BarcodeCommon#generateCRCode}) untuk kode transaksi VA, lalu menampilkan
	 * jendela instruksi pembayaran ({@code /common/jatelindo/no_va.zul}) berisi nomor VA, nominal,
	 * biaya administrasi, total biaya, URL QR, dan terbilang (lewat
	 * {@link IndonesianNumberToWords#convert(long)}). Bila pembuatan request gagal
	 * (mengembalikan {@code null}), menampilkan pesan gagal generik lewat
	 * {@link InfoTeknisPembayaran#pesanGagal()} pada {@link MyMessageboxConfig}.
	 *
	 * @param amn                        nominal yang harus dibayar
	 * @param mahasiswa                  mahasiswa yang membayar, boleh {@code null}
	 * @param biodataCalonMahasiswa      biodata calon mahasiswa, boleh {@code null}
	 * @param selectedKegiatanTemporary  item kegiatan/tagihan sementara yang dipilih
	 * @param event                      event ZK pemicu aksi
	 * @return selalu {@code true} (nilai kembalian tidak membedakan sukses/gagal — status gagal
	 *         disampaikan lewat dialog pesan, bukan lewat nilai balik)
	 * @throws Exception secara praktis tidak pernah keluar dari method ini karena seluruh
	 *                    kegagalan sudah ditangkap secara internal dan dialihkan ke
	 *                    {@link Common#tampilErrorJikaAdmin(Exception)}; dideklarasikan mengikuti
	 *                    signature method yang dipanggil
	 */
	public static boolean onPilihJatelindo(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, final Set<KegiatanTemporary> selectedKegiatanTemporary,
			Event event) throws Exception {

		String merchant_id = Common.getKonfigurasi("jatelindo_merchant_id", "31503").getNilai().trim();

		try {

			final JatelindoRequest jatelindoRequest = JatelindoKeranjangPembayaran.sendRequest(mahasiswa,
					biodataCalonMahasiswa, selectedKegiatanTemporary, amn, merchant_id, true);
			if (jatelindoRequest != null) {

				Double biayaAdministrasi = jatelindoRequest.getBiayaAdministrasi();

				String code = jatelindoRequest.getTrxId();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_"
						+ jatelindoRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String myUrl = "/common/jatelindo/no_va.zul?va="
						+ URLEncoder.encode(jatelindoRequest.getTrxId(), "UTF-8") + "&nominal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(jatelindoRequest.getAmount()), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8")
						+ "&biayaTotal="
						+ URLEncoder.encode(
								"Rp. " + Common.numberFormat.get().format(jatelindoRequest.getAmount() + biayaAdministrasi),
								"UTF-8")
						+ "&qr="
						+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(),
								"UTF-8")
						+ "&terbilang="
						+ URLEncoder.encode(IndonesianNumberToWords
								.convert((long) (jatelindoRequest.getAmount() + biayaAdministrasi)), "UTF-8")
						+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

				Common.displayWindow(myUrl, true, "65%");

			} else {
				// Tampilkan alert + "Informasi Teknis" yang dicatat sendRequest (pola bersama
				// seluruh payment gateway via InfoTeknisPembayaran).
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();

		return true;
	}

	/**
	 * Membangun dan menyimpan satu entitas {@link JatelindoRequest} yang merepresentasikan nomor
	 * Virtual Account Bank Mandiri (via Jatelindo) baru untuk suatu transaksi pembayaran. Nomor VA
	 * dibentuk secara lokal (tanpa memanggil API gateway eksternal pada tahap ini) sebagai
	 * gabungan {@code merchant_id} dan digit acak yang panjangnya dikendalikan konfigurasi
	 * {@code generated_angka_digit_jatelindo} (default 8 digit); biaya administrasi diambil dari
	 * konfigurasi {@code jatelindo_biaya_administrasi} (default {@code 0.0}). Sebelum memulai,
	 * memanggil {@link InfoTeknisPembayaran#bersihkan()} agar detail kegagalan transaksi
	 * sebelumnya tidak ikut tampil pada alert transaksi baru.
	 *
	 * <p>
	 * Kegagalan penyimpanan ke database dicatat secara spesifik lewat
	 * {@link InfoTeknisPembayaran#catat(String)} (menyertakan nama kelas exception dan cuplikan
	 * pesan), transaksi Hibernate di-rollback bila masih aktif, lalu exception dilempar ulang.
	 * Kegagalan lain (mis. saat menyiapkan data sebelum simpan) ditangkap secara umum,
	 * mempertahankan info teknis yang lebih spesifik bila sudah tercatat dari blok simpan-DB, dan
	 * diteruskan ke {@link Common#tampilErrorJikaAdmin(Exception)} tanpa dilempar ulang — sehingga
	 * pada kasus ini method mengembalikan objek {@link JatelindoRequest} yang belum tentu tersimpan
	 * (id {@code null}), dan pemanggil ({@link #onPilihJatelindo}) perlu memeriksa hal ini sendiri.
	 * </p>
	 *
	 * @param mahasiswa                  mahasiswa yang membayar, boleh {@code null}
	 * @param biodataCalonMahasiswa      biodata calon mahasiswa, boleh {@code null}
	 * @param selectedKegiatanTemporary  item kegiatan/tagihan sementara yang dipilih; diambil satu
	 *                                   elemen pertamanya untuk menentukan semester dan tahun
	 *                                   akademik pada request
	 * @param amount                     nominal yang harus dibayar (sebelum biaya administrasi)
	 * @param merchant_id                kode merchant Jatelindo yang menjadi awalan nomor VA
	 * @param hapusCicilanSebelumnya     tandai apakah request cicilan/VA sebelumnya untuk
	 *                                   transaksi terkait perlu dihapus/digantikan
	 * @return entitas {@link JatelindoRequest} yang dibangun, tersimpan bila proses berhasil penuh;
	 *         bila terjadi kegagalan non-DB, objek tetap dikembalikan namun mungkin belum tersimpan
	 * @throws Exception dilempar ulang khusus dari kegagalan penyimpanan ke database (lihat
	 *                    penjelasan penanganan galat di atas)
	 */
	public static JatelindoRequest sendRequest(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			final Set<KegiatanTemporary> selectedKegiatanTemporary, Double amount, String merchant_id,
			Boolean hapusCicilanSebelumnya) throws Exception {
		// Bersihkan detail kegagalan lama agar info transaksi sebelumnya tidak bocor ke alert.
		InfoTeknisPembayaran.bersihkan();

		JatelindoRequest jatelindoRequest = new JatelindoRequest();

		try {
			KegiatanTemporary kegiatanTemporary = selectedKegiatanTemporary.iterator().next();
			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_jatelindo", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JatelindoKeranjangPembayaran.java:99");

			}
			String virtual_account = merchant_id + Common.getGeneratedAngkaDigit(generatedAngkaDigit);

			Double biayaAdministrasi = 0.0;
			try {
				biayaAdministrasi = Double
						.parseDouble(Common.getKonfigurasi("jatelindo_biaya_administrasi", "0.0").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JatelindoKeranjangPembayaran.java:108");

			}

			jatelindoRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			jatelindoRequest.setNama(virtual_account);
			jatelindoRequest.setTrxId(virtual_account);
			jatelindoRequest.setMerchant_id(merchant_id);
			jatelindoRequest.setMerchant("Mandiri");
			jatelindoRequest.setMahasiswa(mahasiswa);
			jatelindoRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			// jatelindoRequest.setJenisKegiatan(jenisKegiatan);
			// jatelindoRequest.setJadwalPembayaran(jadwalPembayaran);
			jatelindoRequest.setSemester(kegiatanTemporary.getSemster());
			jatelindoRequest.setTahunAkademik(kegiatanTemporary.getTahunAkademik());
			// jatelindoRequest.setKeterangan(keterangan);
			// jatelindoRequest.setPengurangan(pengurangan);
			jatelindoRequest.setNilaiBiayaHarusDiBayars(amount);
			jatelindoRequest.setAmount(amount);
			jatelindoRequest.setKegiatanTemporarys(selectedKegiatanTemporary);
			jatelindoRequest.setBiayaAdministrasi(biayaAdministrasi);

			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				session.save(jatelindoRequest);
				session.getTransaction().commit();
			} catch (Exception se) {
				// Titik simpan DB — request Jatelindo GAGAL disimpan di aplikasi.
				InfoTeknisPembayaran.catat("Request Jatelindo (VA Mandiri) GAGAL disimpan di aplikasi: "
						+ se.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(se.getMessage(), 200));
				try {
					if (session != null && session.getTransaction() != null && session.getTransaction().isActive()) {
						session.getTransaction().rollback();
					}
				} catch (Exception re) { ais.common.ErrorAuditUtil.record(re, "auto-audit(empty-catch) src/ais/common/JatelindoKeranjangPembayaran.java:141");
				}
				throw se;
			} finally {
				Common.closeNativeSessionQuietly(session);
			}

		} catch (Exception e) {
			// Kegagalan umum menyiapkan request Jatelindo (VA dibuat lokal, tanpa HTTP ke gateway).
			// Pertahankan detail lebih spesifik dari catch simpan-DB di atas bila sudah tercatat.
			String infoSebelumnya = InfoTeknisPembayaran.ambil();
			InfoTeknisPembayaran.catat(infoSebelumnya != null && !infoSebelumnya.trim().isEmpty() ? infoSebelumnya
					: "Gagal memproses request Jatelindo (VA Mandiri): " + e.getClass().getSimpleName() + " - "
							+ InfoTeknisPembayaran.potong(e.getMessage(), 200));
			Common.tampilErrorJikaAdmin(e);
		}

		return jatelindoRequest;
	}

}
