package ais.common;

import java.io.File;
import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.doku.DokuRequest;
import ais.database.model.doku.DokuRequestDetail;
import ais.database.model.doku.DokuRequestDetailBiaya;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

public class DokuCommon {

	@SuppressWarnings("unchecked")
	public static List<DokuRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<DokuRequestDetailBiaya> dokuRequestDetailBiayas = new ArrayList<DokuRequestDetailBiaya>();
		Rows rows = (Rows) gridss.getRows();
		if (rows != null && rows.getChildren() != null) {
			List<Row> myRows = rows.getChildren();
			for (Row row : myRows) {
				if (!row.isVisible()) {
					continue;
				}
				DetailBiaya detailBiaya = (DetailBiaya) row.getAttribute("myValue");

				Double biaya = detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
						: detailBiaya.getNilaiBiayaBaru();
				try {
					Component component = (Component) row.getAttribute("tag");
					if (component instanceof Doublebox && detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
						Doublebox jumlah = (Doublebox) component;
						biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
					} else if (component instanceof Label) {
						Label myLabel = (Label) component;
						// System.out.println("myLabel = " +
						// myLabel.getValue());
						biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/DokuCommon.java:72");
				}

				if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
					for (MyDoubleboxMin kurang : pengurangan) {
						DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
						if (penguranganItemBiaya != null
								&& penguranganItemBiaya.getId().equals(detailBiaya.getId())) {
							biaya = kurang.getValue() == null ? 0.0 : kurang.getValue();
							break;
						}
					}
				}

				DokuRequestDetailBiaya dokuRequestDetailBiaya = new DokuRequestDetailBiaya();
				dokuRequestDetailBiaya.setDetailBiaya(detailBiaya);
				dokuRequestDetailBiaya.setNilai(biaya);
				dokuRequestDetailBiayas.add(dokuRequestDetailBiaya);
			}
		}
		return dokuRequestDetailBiayas;
	}

	public static List<DokuRequestDetail> populateDokuRequestDetailDariDetailBiaya(
			List<DokuRequestDetailBiaya> dokuRequestDetailBiayas) {
		List<DokuRequestDetail> dokuRequestDetails = new ArrayList<DokuRequestDetail>();

		int i = 1;
		for (DokuRequestDetailBiaya dokuRequestDetailBiaya : dokuRequestDetailBiayas) {
			DokuRequestDetail dokuRequestDetail = new DokuRequestDetail();
			dokuRequestDetail.setPengaturanPembayaranBulanan(null);
			dokuRequestDetail.setItemBiaya(dokuRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			dokuRequestDetail.setKeterangan(dokuRequestDetailBiaya.getKeterangan());
			dokuRequestDetail.setNilai(dokuRequestDetailBiaya.getNilai());
			dokuRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			dokuRequestDetail.setKe(i);
			dokuRequestDetails.add(dokuRequestDetail);
			i++;
		}

		return dokuRequestDetails;
	}

	public static List<DokuRequestDetail> populateDokuRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<DokuRequestDetail> dokuRequestDetails = new ArrayList<DokuRequestDetail>();

		int i = 1;
		for (Row row : mycicilanrows) {
			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

			if (jumlahCicilan.getValue() != null
					&& (jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01)) {

				CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
				MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
				Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null && row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);

				String val = cicilanPembayaran == null ? null : cicilanPembayaran.getValidator();
				if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null")) {
					Tbmuser tbmuser = Common.getCurrentUser();
					val = (tbmuser == null ? "" : tbmuser.toString());

				}

				DokuRequestDetail dokuRequestDetail = new DokuRequestDetail();

				Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
						: myItemBiaya.getSelectedItem().getValue();
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
						.getPengaturanPembayaranBulanan();
				ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();
				DetailBiaya detailBiaya = null;
				if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) jenisBiaya;
					itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				} else if (jenisBiaya != null && jenisBiaya instanceof DetailBiaya) {
					detailBiaya = (DetailBiaya) jenisBiaya;
					itemBiaya = detailBiaya.getItemBiaya();
				}

				dokuRequestDetail.setDetailBiaya(detailBiaya);
				dokuRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				dokuRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				dokuRequestDetail.setItemBiaya(itemBiaya);
				dokuRequestDetail.setKeterangan(keterangan.getValue());
				dokuRequestDetail.setNilai(jumlahCicilan.getValue());
				dokuRequestDetail.setTanggal(tanggal.getValue());
				dokuRequestDetail.setKe(i);

				dokuRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				dokuRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, dokuRequestDetail.getTanggal(), jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						dokuRequestDetail.setDenda(denda);
						dokuRequestDetail.setNilaiAsli(nom);
					}
				}

				dokuRequestDetails.add(dokuRequestDetail);
				i++;
			}
		}

		return dokuRequestDetails;
	}

	public static void bayarCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa, JenisKegiatan jenisKegiatan)
			throws Exception {
		Jurusan prodiLulus = calonMahasiswa.getProdiLulus();
		List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
		if (prodiLulus == null || prodiLulus.getId() == null) {
			Jurusan myjurusan1 = calonMahasiswa.getProdi1() == null ? calonMahasiswa.getProdi2()
					: calonMahasiswa.getProdi1();
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
					.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, myjurusan1, false);
			detailBiayas.addAll(detailBiayas1);
		} else {
			java.util.Collection<DetailBiaya> detailBiayas1 = PembayaranUtil.getInstance()
					.getDetailBiayaCalonMahasiswa(calonMahasiswa, jenisKegiatan, prodiLulus, false);
			detailBiayas.addAll(detailBiayas1);
		}

		if (!detailBiayas.isEmpty()) {

			Serializable[] serializables = PembayaranUtil.getInstance()
					.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(calonMahasiswa.getTanggalDaftar(),
							jenisKegiatan, calonMahasiswa.getJenjang(), calonMahasiswa.getTahunAkademik(),
							calonMahasiswa.getGelombangPendaftaran().getJenisSemester().equalsIgnoreCase(
									Perkuliahan.GANJIL),
							calonMahasiswa.getJenisSeleksi(), calonMahasiswa.getProgram(),
							calonMahasiswa.getNoRegistrasi(), calonMahasiswa.getGelombangPendaftaran());
			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

			if (jadwalPembayaran != null) {
				Double nilaiBiayaHarusDiBayars = 0.0;

				List<DokuRequestDetailBiaya> dokuRequestDetailBiayas = new ArrayList<DokuRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					DokuRequestDetailBiaya dokuRequestDetailBiaya = new DokuRequestDetailBiaya();
					dokuRequestDetailBiaya.setDetailBiaya(detailBiaya);
					dokuRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					dokuRequestDetailBiayas.add(dokuRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += dokuRequestDetailBiaya.getNilai();
				}

				DokuCommon.onSaveDoku(nilaiBiayaHarusDiBayars, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1,
						calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru", 0.0,
						nilaiBiayaHarusDiBayars,
						DokuCommon.populateDokuRequestDetailDariDetailBiaya(dokuRequestDetailBiayas),
						dokuRequestDetailBiayas, null);

			}
		}

	}

	@SuppressWarnings({})
	public static boolean onSaveDoku(final Double amn, Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran, Integer semester, String tahunAkademik,
			String keterangan, Double pengurangan, Double nilaiBiayaHarusDiBayars,
			List<DokuRequestDetail> dokuRequestDetails, List<DokuRequestDetailBiaya> dokuRequestDetailBiayas,
			Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		String add_info2 = "";

		for (DokuRequestDetail detail : dokuRequestDetails) {
			if (detail.getIdCicilan() == null) {
				String ket = detail.getItemBiaya().getNama() + "," + (detail.getNilai().intValue() + ".00") + ",1,"
						+ (detail.getNilai().intValue() + ".00");
				add_info2 += add_info2.isEmpty() ? ket : ";" + ket;
			}
		}

		String key = Common.getKonfigurasi("doku_key", "w6Z2y3F2q5j6").getNilai();

		String TRANSIDMERCHANT = Common.getGeneratedBarCode();
		String STOREID = Common.getKonfigurasi("doku_merchant_id", "10444535").getNilai();
		String AMOUNT = amn.intValue() + ".00";
		String WORDS = AMOUNT + key + TRANSIDMERCHANT;
		WORDS = AeSimpleSHA1.SHA1(WORDS);

		final DokuRequest dokuRequest = DokuCommon.sendRequest(TRANSIDMERCHANT, WORDS, mahasiswa, biodataCalonMahasiswa,
				jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan,
				nilaiBiayaHarusDiBayars, amn, add_info2, dokuRequestDetails, dokuRequestDetailBiayas);
		if (dokuRequest != null && dokuRequest.getNama() != null && !dokuRequest.getNama().trim().isEmpty()) {

			String CNAME = "";
			String CEMAIL = "";
			String PHONE = "";
			String CADDRESS = "";
			String CZIPCODE = "";
			String BIRTHDATE = "";
			String CCITY = "";
			String CSTATE = "";
			if (mahasiswa != null) {
				Session session = HibernateUtil.currentNativeSession();
				BiodataMahasiswa biodataMahasiswa = (BiodataMahasiswa) session.createCriteria(BiodataMahasiswa.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1)
						.uniqueResult();
				CNAME = mahasiswa.getNama();
				CEMAIL = mahasiswa.getEmail().split(",")[0];
				PHONE = mahasiswa.getTelp();
				CADDRESS = mahasiswa.getAlamat();
				CZIPCODE = biodataMahasiswa == null ? "" : biodataMahasiswa.getKodepos();
				BIRTHDATE = mahasiswa.getTanggallahir() == null ? ""
						: Common.databaseDateFormat.get().format(mahasiswa.getTanggallahir());
				CCITY = biodataMahasiswa == null || biodataMahasiswa.getKota() == null ? ""
						: biodataMahasiswa.getKota().getNama();
				CSTATE = biodataMahasiswa == null || biodataMahasiswa.getPropinsi() == null ? ""
						: biodataMahasiswa.getPropinsi().getNama();
			}

			String url = Common
					.getKonfigurasi("doku_gateway_url", "https://apps.myshortcart.com/payment/request-payment/")
					.getNilai();

			String html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
					+ "<html lang=\"en\">" + "<head>" + "	<title>Form Transaksi</title>" + "</head>" + "<body>" +

					"Loading...<FORM ID=\"order" + WORDS + "\" NAME=\"order\" METHOD=\"Post\" ACTION=\"" + url + "\" >"
					+ "	<input type=hidden name=\"BASKET\" value=\"" + add_info2 + "\">"
					+ "	<input type=hidden name=\"STOREID\" value=\"" + STOREID + "\"> "
					+ "	<input type=hidden name=\"TRANSIDMERCHANT\" value=\"" + TRANSIDMERCHANT + "\">"
					+ "	<input type=hidden name=\"AMOUNT\" value=\"" + AMOUNT + "\">"
					+ "	<input type=hidden name=\"URL\" value=\"" + Common.getRequestHostWithProtocol() + "\">"
					+ "	<input type=hidden name=\"WORDS\" value=\"" + WORDS + "\">"
					+ "	<input type=hidden name=\"CNAME\" value=\"" + CNAME + "\">"
					+ "	<input type=hidden name=\"CEMAIL\" value=\"" + CEMAIL + "\">"
					+ "	<input type=hidden name=\"CWPHONE\" value=\"" + PHONE + "\">"
					+ "	<input type=hidden name=\"CHPHONE\" value=\"" + PHONE + "\"> "
					+ "	<input type=hidden name=\"CMPHONE\" value=\"" + PHONE + "\">"
					+ "	<input type=hidden name=\"CADDRESS\" value=\"" + CADDRESS + "\">"
					+ "	<input type=hidden name=\"CZIPCODE\" value=\"" + CZIPCODE + "\">"
					+ "	<input type=hidden name=\"BIRTHDATE\" value=\"" + BIRTHDATE + "\">"
					+ "	<input type=hidden name=\"CCITY\" value=\"" + CCITY + "\">"
					+ "	<input type=hidden name=\"CSTATE\" value=\"" + CSTATE + "\">"
					+ "	<input type=hidden name=\"CCOUNTRY\" value=\"360\">"
					+ "	<input type=hidden name=\"SADDRESS\" value=\"" + CADDRESS + "\">"
					+ "	<input type=hidden name=\"SZIPCODE\" value=\"" + CZIPCODE + "\">"
					+ "	<input type=hidden name=\"SCITY\" value=\"" + CCITY + "\">"
					+ "	<input type=hidden name=\"SSTATE\" value=\"" + CSTATE + "\">"
					+ "	<input type=hidden name=\"SCOUNTRY\" value=\"360\"></FORM>" +

					"<script>document.getElementById(\"order" + WORDS + "\").submit();</script></body>" + "</html>";

			File myFile = new File(Common.REAL_PATH + "/tmp/" + WORDS + ".html");
			System.out.println("Report file = " + myFile.getAbsolutePath());
			myFile.getParentFile().mkdirs();
			FileWriter fileWriter = new FileWriter(myFile);
			fileWriter.write(html);
			fileWriter.close();

			Common.displayWindow("/tmp/" + WORDS + ".html", true, "95%");

		} else {
			// Tampilkan alert kegagalan BESERTA "Informasi Teknis" yang dicatat sendRequest
			// (pola bersama seluruh payment gateway, lihat InfoTeknisPembayaran).
			MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	public static DokuRequest sendRequest(String TRANSIDMERCHANT, String WORDS, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, String comments, List<DokuRequestDetail> dokuRequestDetails,
			List<DokuRequestDetailBiaya> dokuRequestDetailBiayas) throws Exception {
		// Pola "Informasi Teknis" kegagalan payment gateway (lihat InfoTeknisPembayaran):
		// bersihkan detail lama agar kegagalan transaksi sebelumnya tidak bocor ke alert ini.
		InfoTeknisPembayaran.bersihkan();

		DokuRequest dokuRequest = new DokuRequest();
		dokuRequest.setNama(TRANSIDMERCHANT);
		dokuRequest.setTrxId(WORDS);
		dokuRequest.setMahasiswa(mahasiswa);
		dokuRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
		dokuRequest.setJenisKegiatan(jenisKegiatan);
		dokuRequest.setJadwalPembayaran(jadwalPembayaran);
		dokuRequest.setSemester(semester);
		dokuRequest.setTahunAkademik(tahunAkademik);
		dokuRequest.setKeterangan(keterangan);
		dokuRequest.setPengurangan(pengurangan);
		dokuRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
		dokuRequest.setAmount(amount);
		dokuRequest.setComments(comments);

		Session session = HibernateUtil.currentNativeSession();
		try {
			session.getTransaction().begin();
			session.save(dokuRequest);
			session.getTransaction().commit();

			for (DokuRequestDetail dokuRequestDetail : dokuRequestDetails) {
				dokuRequestDetail.setDokuRequest(dokuRequest);
				session.getTransaction().begin();
				session.save(dokuRequestDetail);
				session.getTransaction().commit();
			}

			for (DokuRequestDetailBiaya dokuRequestDetailBiaya : dokuRequestDetailBiayas) {
				dokuRequestDetailBiaya.setDokuRequest(dokuRequest);
				session.getTransaction().begin();
				session.save(dokuRequestDetailBiaya);
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			// Gagal simpan data transaksi Doku (Doku memakai form-post browser, request ke
			// gateway baru terjadi SETELAH data tersimpan) → catat detailnya lalu kembalikan
			// null agar pemanggil menampilkan alert kegagalan beserta informasi teknisnya.
			InfoTeknisPembayaran.catat("Data transaksi Doku (" + TRANSIDMERCHANT
					+ ") GAGAL disimpan di aplikasi sebelum diteruskan ke gateway: " + e.getClass().getSimpleName()
					+ " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			ais.common.ErrorAuditUtil.record(e, "Doku sendRequest gagal simpan; transId=" + TRANSIDMERCHANT);
			try {
				session.getTransaction().rollback();
			} catch (Exception ignore) {
				ais.common.ErrorAuditUtil.record(ignore, "Doku sendRequest rollback gagal; transId=" + TRANSIDMERCHANT);
			}
			return null;
		}

		return dokuRequest;
	}

}
