package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
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
import ais.database.model.file.LampiranLain;
import ais.database.model.jatelindo.JatelindoRequest;
import ais.database.model.jatelindo.JatelindoRequestDetail;
import ais.database.model.jatelindo.JatelindoRequestDetailBiaya;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

public class JatelindoCommon {

	public static MyButtonConfig createButton() {
		File fileViaJatelindo = new File(Common.REAL_PATH + "/img/mandiri.jpg");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO,
					LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_JATELINDO_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileViaJatelindo = lainMahasiswa.ambilFile();
				File fileDiImg = new File(Common.REAL_PATH + "/img/" + fileViaJatelindo.getName());
				boolean ada = fileDiImg.exists();
				System.out.println(
						"fileViaJatelindo = " + fileViaJatelindo + ", fileDiImg = " + fileDiImg + ", ada = " + ada);
				if (!ada) {
					FileInputStream fileInputStream = new FileInputStream(fileViaJatelindo);
					FileOutputStream fileOutputStream = new FileOutputStream(fileDiImg);
					IOUtils.copyLarge(fileInputStream, fileOutputStream);
					fileInputStream.close();
					fileOutputStream.close();
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		MyButtonConfig bayarViaJatelindo = new MyButtonConfig(
				Common.getKonfigurasi("label_pembayaran_via_jatelindo", "Bayar via Mandiri").getNilai(),
				"/img/" + fileViaJatelindo.getName());
		return bayarViaJatelindo;
	}

	@SuppressWarnings("unchecked")
	public static List<JatelindoRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas = new ArrayList<JatelindoRequestDetailBiaya>();
		Rows rows = (Rows) gridss.getRows();
		if (rows != null && rows.getChildren() != null) {
			List<Row> myRows = rows.getChildren();
			System.out.println("myRows -> " + myRows.size());
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/JatelindoCommon.java:110");
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

				JatelindoRequestDetailBiaya jatelindoRequestDetailBiaya = new JatelindoRequestDetailBiaya();
				jatelindoRequestDetailBiaya.setDetailBiaya(detailBiaya);
				jatelindoRequestDetailBiaya.setNilai(biaya);
				jatelindoRequestDetailBiayas.add(jatelindoRequestDetailBiaya);
			}
		}
		return jatelindoRequestDetailBiayas;
	}

	public static List<JatelindoRequestDetail> populateJatelindoRequestDetailDariDetailBiaya(
			List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas) {
		List<JatelindoRequestDetail> jatelindoRequestDetails = new ArrayList<JatelindoRequestDetail>();

		int i = 1;
		for (JatelindoRequestDetailBiaya jatelindoRequestDetailBiaya : jatelindoRequestDetailBiayas) {
			JatelindoRequestDetail jatelindoRequestDetail = new JatelindoRequestDetail();
			jatelindoRequestDetail.setPengaturanPembayaranBulanan(null);
			jatelindoRequestDetail.setItemBiaya(jatelindoRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			jatelindoRequestDetail.setKeterangan(jatelindoRequestDetailBiaya.getKeterangan());
			jatelindoRequestDetail.setNilai(jatelindoRequestDetailBiaya.getNilai());
			jatelindoRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			jatelindoRequestDetail.setKe(i);
			jatelindoRequestDetails.add(jatelindoRequestDetail);
			i++;
		}

		return jatelindoRequestDetails;
	}

	public static List<JatelindoRequestDetail> populateJatelindoRequestDetail(HttpServletRequest request,
			Mahasiswa mahasiswa, String validator, Integer semester) {

		String jenis = request.getParameter("jenis") == null ? "bulanan" : request.getParameter("jenis");
		String data = request.getParameter("data") == null ? "" : request.getParameter("data");
		System.out.println("jenis => " + jenis + ", data => " + data);
		List<JatelindoRequestDetail> jatelindoRequestDetails = new ArrayList<JatelindoRequestDetail>();

		Session session = HibernateUtil.currentNativeSession();
		int i = 1;
		for (String d : data.split(",")) {

			PengaturanPembayaranBulanan pengaturanPembayaranBulanan = null;
			ItemBiaya itemBiaya = null;
			DetailBiaya detailBiaya = null;
			Double nilai = 0.0;

			if (jenis.equalsIgnoreCase(jenis)) {
				pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) session
						.createCriteria(PengaturanPembayaranBulanan.class)
						.add(Restrictions.idEq(Long.parseLong(d.trim()))).uniqueResult();
				detailBiaya = pengaturanPembayaranBulanan.getDetailBiaya();
				itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				nilai = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
			} else {
				detailBiaya = (DetailBiaya) session.createCriteria(DetailBiaya.class)
						.add(Restrictions.idEq(Long.parseLong(d.trim()))).uniqueResult();
				itemBiaya = detailBiaya.getItemBiaya();
				nilai = (detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
						: detailBiaya.getNilaiBiayaBaru());
			}

			String keterangan = "";
			if ((keterangan == null || keterangan.trim().isEmpty()) && pengaturanPembayaranBulanan != null) {
				keterangan = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() + "-"
						+ pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getNama() + ", bulan "
						+ pengaturanPembayaranBulanan.getNamaBulan() + " " + ", nominal Rp. "
						+ Common.numberFormat.get().format(nilai)
						+ (validator.trim().isEmpty() ? "" : ", validator : " + validator);
			} else if ((keterangan == null || keterangan.trim().isEmpty()) && itemBiaya != null && nilai != null) {
				keterangan = itemBiaya.getKode() + "-" + itemBiaya.getNama() + ", nominal Rp. "
						+ Common.numberFormat.get().format(nilai)
						+ (validator.trim().isEmpty() ? "" : ", validator : " + validator);

			}

			JatelindoRequestDetail jatelindoRequestDetail = new JatelindoRequestDetail();
			jatelindoRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			jatelindoRequestDetail.setDetailBiaya(detailBiaya);
			jatelindoRequestDetail.setItemBiaya(itemBiaya);
			jatelindoRequestDetail.setKeterangan(keterangan);
			jatelindoRequestDetail.setNilai(nilai);
			jatelindoRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			jatelindoRequestDetail.setKe(i);
			jatelindoRequestDetail.setDenda(0.0);
			jatelindoRequestDetail.setNilaiAsli(nilai);
			jatelindoRequestDetails.add(jatelindoRequestDetail);
			i++;

		}

		HibernateUtil.closeSession();

		return jatelindoRequestDetails;
	}

	public static List<JatelindoRequestDetail> populateJatelindoRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<JatelindoRequestDetail> jatelindoRequestDetails = new ArrayList<JatelindoRequestDetail>();

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

				JatelindoRequestDetail jatelindoRequestDetail = new JatelindoRequestDetail();

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

				jatelindoRequestDetail.setDetailBiaya(detailBiaya);
				jatelindoRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				jatelindoRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				jatelindoRequestDetail.setItemBiaya(itemBiaya);
				jatelindoRequestDetail.setKeterangan(keterangan.getValue());
				jatelindoRequestDetail.setNilai(jumlahCicilan.getValue());
				jatelindoRequestDetail.setTanggal(tanggal.getValue());
				jatelindoRequestDetail.setKe(i);

				jatelindoRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				jatelindoRequestDetail
						.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
								: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, jatelindoRequestDetail.getTanggal(),
								jdw, jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						jatelindoRequestDetail.setDenda(denda);
						jatelindoRequestDetail.setNilaiAsli(nom);
					}
				}

				jatelindoRequestDetails.add(jatelindoRequestDetail);
				i++;
			}
		}

		return jatelindoRequestDetails;
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

				List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas = new ArrayList<JatelindoRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					JatelindoRequestDetailBiaya jatelindoRequestDetailBiaya = new JatelindoRequestDetailBiaya();
					jatelindoRequestDetailBiaya.setDetailBiaya(detailBiaya);
					jatelindoRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					jatelindoRequestDetailBiayas.add(jatelindoRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += jatelindoRequestDetailBiaya.getNilai();
				}

				Double nilaiYgAkanDibayar = Common.numberFormat.get()
						.parse(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars)).doubleValue();
				JatelindoCommon.onSaveJatelindo(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan,
						jadwalPembayaran, 1, calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru",
						0.0, nilaiBiayaHarusDiBayars,
						JatelindoCommon.populateJatelindoRequestDetailDariDetailBiaya(jatelindoRequestDetailBiayas),
						jatelindoRequestDetailBiayas, null);

			}
		}

	}

	public static boolean onPilihJatelindo(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<JatelindoRequestDetail> jatelindoRequestDetails,
			List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas, Event event) throws Exception {

		String merchant_id = Common.getKonfigurasi("jatelindo_merchant_id", "129").getNilai().trim();

		final JatelindoRequest jatelindoRequest = JatelindoCommon.sendRequest(mahasiswa, biodataCalonMahasiswa,
				jenisKegiatan, jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan,
				nilaiBiayaHarusDiBayars, amn, merchant_id, jatelindoRequestDetails, jatelindoRequestDetailBiayas, true);
		if (jatelindoRequest != null) {

			Double biayaAdministrasi = jatelindoRequest.getBiayaAdministrasi();

			String code = jatelindoRequest.getTrxId();

			File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + jatelindoRequest.getId() + ".png");

			BarcodeCommon.generateCRCode(code, myfilebarcode1);

			String myUrl = "/common/jatelindo/no_va.zul?va=" + URLEncoder.encode(jatelindoRequest.getTrxId(), "UTF-8")
					+ "&nominal="
					+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(jatelindoRequest.getAmount()), "UTF-8")
					+ "&biayaAdministrasi="
					+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8")
					+ "&biayaTotal="
					+ URLEncoder.encode(
							"Rp. " + Common.numberFormat.get().format(jatelindoRequest.getAmount() + biayaAdministrasi),
							"UTF-8")
					+ "&qr="
					+ URLEncoder.encode(
							Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(), "UTF-8")
					+ "&terbilang="
					+ URLEncoder.encode(
							IndonesianNumberToWords.convert((long) (jatelindoRequest.getAmount() + biayaAdministrasi)),
							"UTF-8")
					+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

			Common.displayWindow(myUrl, true, "65%");

		} else {
			// Tampilkan alert + "Informasi Teknis" yang dicatat sendRequest (pola bersama
			// seluruh payment gateway via InfoTeknisPembayaran).
			MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.EXCLAMATION);

		}

		return true;
	}

	@SuppressWarnings({})
	public static boolean onSaveJatelindo(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan,
			final JadwalPembayaran jadwalPembayaran, final Integer semester, final String tahunAkademik,
			final String keterangan, final Double pengurangan, final Double nilaiBiayaHarusDiBayars,
			final List<JatelindoRequestDetail> jatelindoRequestDetails,
			final List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas, final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		onPilihJatelindo(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester,
				tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, jatelindoRequestDetails,
				jatelindoRequestDetailBiayas, event);

		return true;
	}

	public static JatelindoRequest sendRequest(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran, Integer semester, String tahunAkademik,
			String keterangan, Double pengurangan, Double nilaiBiayaHarusDiBayars, Double amount, String merchant_id,
			List<JatelindoRequestDetail> jatelindoRequestDetails,
			List<JatelindoRequestDetailBiaya> jatelindoRequestDetailBiayas, Boolean hapusCicilanSebelumnya)
			throws Exception {

		// Bersihkan detail kegagalan lama agar info transaksi sebelumnya tidak bocor ke alert.
		InfoTeknisPembayaran.bersihkan();

		JatelindoRequest jatelindoRequest = new JatelindoRequest();

		try {

			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_jatelindo", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JatelindoCommon.java:435");

			}
			String virtual_account = merchant_id + Common.getGeneratedAngkaDigit(generatedAngkaDigit);

			Double biayaAdministrasi = 0.0;
			try {
				biayaAdministrasi = Double
						.parseDouble(Common.getKonfigurasi("jatelindo_biaya_administrasi", "0.0").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/JatelindoCommon.java:444");

			}

			jatelindoRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			jatelindoRequest.setNama(virtual_account);
			jatelindoRequest.setTrxId(virtual_account);
			jatelindoRequest.setMerchant_id(merchant_id);
			jatelindoRequest.setMerchant("Mandiri");
			jatelindoRequest.setMahasiswa(mahasiswa);
			jatelindoRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			jatelindoRequest.setJenisKegiatan(jenisKegiatan);
			jatelindoRequest.setJadwalPembayaran(jadwalPembayaran);
			jatelindoRequest.setSemester(semester);
			jatelindoRequest.setTahunAkademik(tahunAkademik);
			jatelindoRequest.setKeterangan(keterangan);
			jatelindoRequest.setPengurangan(pengurangan);
			jatelindoRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
			jatelindoRequest.setAmount(amount);
			jatelindoRequest.setBiayaAdministrasi(biayaAdministrasi);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(jatelindoRequest);
			session.getTransaction().commit();

			for (JatelindoRequestDetail jatelindoRequestDetail : jatelindoRequestDetails) {
				jatelindoRequestDetail.setJatelindoRequest(jatelindoRequest);
				session.getTransaction().begin();
				session.save(jatelindoRequestDetail);
				session.getTransaction().commit();
			}

			for (JatelindoRequestDetailBiaya jatelindoRequestDetailBiaya : jatelindoRequestDetailBiayas) {
				jatelindoRequestDetailBiaya.setJatelindoRequest(jatelindoRequest);
				session.getTransaction().begin();
				session.save(jatelindoRequestDetailBiaya);
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			// Jatelindo tidak memanggil gateway di sini (VA dibuat lokal) — kegagalan berarti
			// request GAGAL disimpan di aplikasi. Catat detailnya agar alert tidak generik.
			InfoTeknisPembayaran.catat("Request Jatelindo (VA Mandiri) GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			Common.tampilErrorJikaAdmin(e);
		}
		return jatelindoRequest;
	}

}
