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

public class JatelindoKeranjangPembayaran {

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
