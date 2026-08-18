package ais.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import com.bni.encrypt.BNIHash;

import ais.action.master.bsi.BsiRequestAction;
import ais.action.ws.util.PembayaranUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.bsi.BsiRequest;
import ais.database.model.bsi.BsiRequestDetail;
import ais.database.model.bsi.BsiRequestDetailBiaya;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

public class BsiCommon {

	public static MyButtonConfig createButton() {
		File fileViaBsi = new File(Common.REAL_PATH + "/img/bsi.png");
		try {
			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BSI,
					LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_BSI_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileViaBsi = lainMahasiswa.ambilFile();
				File fileDiImg = new File(Common.REAL_PATH + "/img/" + fileViaBsi.getName());
				boolean ada = fileDiImg.exists();
				System.out.println("fileViaBsi = " + fileViaBsi + ", fileDiImg = " + fileDiImg + ", ada = " + ada);
				if (!ada) {
					FileInputStream fileInputStream = new FileInputStream(fileViaBsi);
					FileOutputStream fileOutputStream = new FileOutputStream(fileDiImg);
					IOUtils.copyLarge(fileInputStream, fileOutputStream);
					fileInputStream.close();
					fileOutputStream.close();
				}
			}
		} catch (Exception e) {

			Common.tampilErrorJikaAdmin(e);
		}

		MyButtonConfig bayarViaBsi = new MyButtonConfig(
				Common.getKonfigurasi("label_pembayaran_via_bsi", "Bayar via Bsi").getNilai(),
				"/img/" + fileViaBsi.getName());
		return bayarViaBsi;
	}

	@SuppressWarnings("unchecked")
	public static List<BsiRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<BsiRequestDetailBiaya> bsiRequestDetailBiayas = new ArrayList<BsiRequestDetailBiaya>();
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
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BsiCommon.java:125");
				}

				if (detailBiaya.getItemBiaya().getPenghitungan().equals(ItemBiaya.DIKALI_NILAI_MINUS)) {
					for (MyDoubleboxMin kurang : pengurangan) {
						DetailBiaya penguranganItemBiaya = (DetailBiaya) kurang.getAttribute("itemBiaya");
						if (penguranganItemBiaya != null && penguranganItemBiaya.getId().equals(detailBiaya.getId())) {
							biaya = kurang.getValue() == null ? 0.0 : kurang.getValue();
							break;
						}
					}
				}

				BsiRequestDetailBiaya bsiRequestDetailBiaya = new BsiRequestDetailBiaya();
				bsiRequestDetailBiaya.setDetailBiaya(detailBiaya);
				bsiRequestDetailBiaya.setNilai(biaya);
				bsiRequestDetailBiayas.add(bsiRequestDetailBiaya);
			}
		}
		return bsiRequestDetailBiayas;
	}

	public static List<BsiRequestDetail> populateBsiRequestDetailDariDetailBiaya(
			List<BsiRequestDetailBiaya> bsiRequestDetailBiayas) {
		List<BsiRequestDetail> bsiRequestDetails = new ArrayList<BsiRequestDetail>();

		int i = 1;
		for (BsiRequestDetailBiaya bsiRequestDetailBiaya : bsiRequestDetailBiayas) {
			BsiRequestDetail bsiRequestDetail = new BsiRequestDetail();
			bsiRequestDetail.setPengaturanPembayaranBulanan(null);
			bsiRequestDetail.setItemBiaya(bsiRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			bsiRequestDetail.setKeterangan(bsiRequestDetailBiaya.getKeterangan());
			bsiRequestDetail.setNilai(bsiRequestDetailBiaya.getNilai());
			bsiRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			bsiRequestDetail.setKe(i);
			bsiRequestDetails.add(bsiRequestDetail);
			i++;
		}

		return bsiRequestDetails;
	}

	public static List<BsiRequestDetail> populateBsiRequestDetail(HttpServletRequest request, Mahasiswa mahasiswa,
			String validator, Integer semester) {

		String jenis = request.getParameter("jenis") == null ? "bulanan" : request.getParameter("jenis");
		String data = request.getParameter("data") == null ? "" : request.getParameter("data");
		System.out.println("jenis => " + jenis + ", data => " + data);
		List<BsiRequestDetail> bsiRequestDetails = new ArrayList<BsiRequestDetail>();

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

			BsiRequestDetail bsiRequestDetail = new BsiRequestDetail();
			bsiRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			bsiRequestDetail.setDetailBiaya(detailBiaya);
			bsiRequestDetail.setItemBiaya(itemBiaya);
			bsiRequestDetail.setKeterangan(keterangan);
			bsiRequestDetail.setNilai(nilai);
			bsiRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			bsiRequestDetail.setKe(i);
			bsiRequestDetail.setDenda(0.0);
			bsiRequestDetail.setNilaiAsli(nilai);
			bsiRequestDetails.add(bsiRequestDetail);
			i++;

		}

		HibernateUtil.closeSession();

		return bsiRequestDetails;
	}

	public static List<BsiRequestDetail> populateBsiRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<BsiRequestDetail> bsiRequestDetails = new ArrayList<BsiRequestDetail>();

		int i = 1;
		for (Row row : mycicilanrows) {
			MyDoublebox jumlahCicilan = (MyDoublebox) row.getAttribute("jumlahCicilan");

			if (jumlahCicilan.getValue() != null
					&& (jumlahCicilan.getValue() > 0.01 || jumlahCicilan.getValue() < -0.01)) {

				CicilanPembayaran cicilanPembayaran = (CicilanPembayaran) row.getAttribute("cicilanPembayaran");
				MyDatebox tanggal = (MyDatebox) row.getAttribute("tanggal");
				Combobox myItemBiaya = (Combobox) row.getAttribute("itemBiaya");
				Textbox keterangan = (Textbox) ((row.getAttribute("keterangan") != null
						&& row.getAttribute("keterangan") instanceof Textbox) ? row.getAttribute("keterangan") : null);

				String val = cicilanPembayaran == null ? null : cicilanPembayaran.getValidator();
				if (val == null || val.trim().isEmpty() || val.trim().equalsIgnoreCase("null")) {
					Tbmuser tbmuser = Common.getCurrentUser();
					val = (tbmuser == null ? "" : tbmuser.toString());

				}

				BsiRequestDetail bsiRequestDetail = new BsiRequestDetail();

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

				bsiRequestDetail.setDetailBiaya(detailBiaya);
				bsiRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				bsiRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				bsiRequestDetail.setItemBiaya(itemBiaya);
				bsiRequestDetail.setKeterangan(keterangan.getValue());
				bsiRequestDetail.setNilai(jumlahCicilan.getValue());
				bsiRequestDetail.setTanggal(tanggal.getValue());
				bsiRequestDetail.setKe(i);

				bsiRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				bsiRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, bsiRequestDetail.getTanggal(), jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan()) - nom;
						bsiRequestDetail.setDenda(denda);
						bsiRequestDetail.setNilaiAsli(nom);
					}
				}

				bsiRequestDetails.add(bsiRequestDetail);
				i++;
			}
		}

		return bsiRequestDetails;
	}

	public static BsiRequest bayarCalonMahasiswa(BiodataCalonMahasiswa calonMahasiswa, JenisKegiatan jenisKegiatan,
			boolean tampilInfo) throws Exception {
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

//		System.out.println("detailBiayas => " + detailBiayas.size());

		if (!detailBiayas.isEmpty()) {

			Serializable[] serializables = PembayaranUtil.getInstance()
					.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(calonMahasiswa.getTanggalDaftar(),
							jenisKegiatan, calonMahasiswa.getJenjang(), calonMahasiswa.getTahunAkademik(),
							calonMahasiswa.getGelombangPendaftaran().getJenisSemester().equalsIgnoreCase(
									Perkuliahan.GANJIL),
							calonMahasiswa.getJenisSeleksi(), calonMahasiswa.getProgram(),
							calonMahasiswa.getNoRegistrasi(), calonMahasiswa.getGelombangPendaftaran());
			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
			System.out.println("jadwalPembayaran => " + jadwalPembayaran);
			if (jadwalPembayaran != null) {
				Double nilaiBiayaHarusDiBayars = 0.0;
				String cicilan = "";
				List<BsiRequestDetailBiaya> bsiRequestDetailBiayas = new ArrayList<BsiRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					BsiRequestDetailBiaya bsiRequestDetailBiaya = new BsiRequestDetailBiaya();
					bsiRequestDetailBiaya.setDetailBiaya(detailBiaya);
					bsiRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					bsiRequestDetailBiayas.add(bsiRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += bsiRequestDetailBiaya.getNilai();

					ItemBiaya itemBiaya = detailBiaya.getItemBiaya();

					cicilan += cicilan.isEmpty()
							? ("Item-" + itemBiaya.getId().toString() + "-" + bsiRequestDetailBiaya.getNilai() + "-"
									+ detailBiaya.getBayarKe() + "-" + detailBiaya.getId())
							: "," + ("Item-" + itemBiaya.getId().toString() + "-" + bsiRequestDetailBiaya.getNilai()
									+ "-" + detailBiaya.getBayarKe() + "-" + detailBiaya.getId());
				}

				Double nilaiYgAkanDibayar = Common.numberFormat.get()
						.parse(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars)).doubleValue();
				return BsiCommon.onSaveBsi(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1,
						calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru", 0.0,
						nilaiBiayaHarusDiBayars,
						BsiCommon.populateBsiRequestDetailDariDetailBiaya(bsiRequestDetailBiayas),
						bsiRequestDetailBiayas, tampilInfo, null, cicilan);

			}

		}
		return null;
	}

	public static BsiRequest onPilihBsi(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<BsiRequestDetail> bsiRequestDetails,
			List<BsiRequestDetailBiaya> bsiRequestDetailBiayas, boolean tampilInfo, Event event, String cicilan)
			throws Exception {

		String merchant_id = Common.getKonfigurasi("bsi_merchant_id", "000").getNilai().trim();
		String Password = Common.getKonfigurasi("bsi_password", "685dedd9f045787873794ead6276f8bf").getNilai().trim();

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();

		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);

		String jam_terakhir_pembayaran = Common.getKonfigurasi("jam_terakhir_pembayaran", "").getNilai();
		if (jam_terakhir_pembayaran != null && !jam_terakhir_pembayaran.trim().isEmpty()) {
			try {
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.set(Calendar.HOUR_OF_DAY,
						calendar1.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(jam_terakhir_pembayaran));
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				calendar = calendar1;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BsiCommon.java:403");
			}
		}

		String bill_no = Common.getGeneratedBarCode();

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran = Common.getKonfigurasi("tanggal_terakhir_pembayaran", "").getNilai();
		if (tanggal_terakhir_pembayaran != null && !tanggal_terakhir_pembayaran.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran);
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.setTime(tanggalExpired);
				calendar1.set(Calendar.HOUR_OF_DAY, 23);
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				tanggalExpired = calendar1.getTime();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiCommon.java:420");

			}
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		String virtual_account = "";

		boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bsi");

		if (Common.bolehKonfigurasi("angka_va_bsi_menggunakan_nim")) {
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bsi", "8").getNilai()
					+ (tambahkanMerchanId ? merchant_id : "") + (mahasiswa != null ? mahasiswa.getNim()
							: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNoRegistrasi() : ""));
		} else {
			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_bsi", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiCommon.java:441");

			}
			if (Common.bolehKonfigurasi("menggunakan_prefix_bsi")) {
				virtual_account = Common.getKonfigurasi("angka_prefix_va_bsi", "8").getNilai()
						+ (tambahkanMerchanId ? merchant_id : "") + Common.getGeneratedAngkaDigit(generatedAngkaDigit);
			} else {
				virtual_account = Common.getGeneratedAngkaDigit(generatedAngkaDigit);
			}
		}

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bsi_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiCommon.java:455");

		}

		String description = "";
		if (!tambahkanMerchanId) {
			description = ",\"description\":\"" + keterangan + "\"";
		}

		int generatedAngkaDigit = 16;
		try {
			generatedAngkaDigit = Integer
					.parseInt(Common.getKonfigurasi("virtual_account_angka_digit_bsi", "16").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiCommon.java:468");

		}
		virtual_account = (virtual_account + "00000000000000000").substring(0, generatedAngkaDigit);

		// BNIHash hash = new BNIHash();
		String data = "{\"customer_email\":\""
				+ (mahasiswa != null ? mahasiswa.getEmail().split(",")[0].trim()
						: (biodataCalonMahasiswa == null ? "test@email.com"
								: biodataCalonMahasiswa.getEmail().split(",")[0].trim()))
				+ "\",\"trx_id\":\"" + bill_no + "\",\"datetime_expired\":\"" + datetime_expired + "\",\"client_id\":\""
				+ merchant_id + "\",\"customer_phone\":\""
				+ (mahasiswa != null ? mahasiswa.getTelp()
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getHp() : ""))
				+ "\",\"customer_name\":\""
				+ (mahasiswa != null ? (mahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") + " " + mahasiswa.getNim())
						: (biodataCalonMahasiswa != null
								? (biodataCalonMahasiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") + " " + biodataCalonMahasiswa.getNoRegistrasi())
								: ""))
				+ "\",\"type\":\"createbilling\",\"virtual_account\":\"" + virtual_account + "\",\"trx_amount\":\""
				+ (biayaAdministrasi.intValue() + amn.intValue()) + "\",\"billing_type\":\"c\" " + description + " }";

		String cid = merchant_id; // from BNI
		String key = Password; // from BNI

		String parsedData = BNIHash.hashData(data, cid, key);
		String decodeData = BNIHash.parseData(parsedData, cid, key);

		System.out.println("parsedData = " + parsedData);
		System.out.println("decodeData = " + decodeData);

		String postData = "{ \"client_id\":\"" + merchant_id + "\", \"data\":\"" + parsedData + "\"}";
		final BsiRequest bsiRequest = BsiCommon.sendRequest(postData, mahasiswa, biodataCalonMahasiswa, jenisKegiatan,
				jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, amn,
				merchant_id, data, bill_no, virtual_account, key, bsiRequestDetails, bsiRequestDetailBiayas, true,
				tanggalExpired, cicilan);
		if (tampilInfo) {
			if (bsiRequest != null && bsiRequest.getVa() != null && !bsiRequest.getVa().trim().isEmpty()) {

				String code = bsiRequest.getVa();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + bsiRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String nama = mahasiswa != null ? mahasiswa.getNama()
						: (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama() : "");
				String myUrl = "/common/bsi/no_va.zul?va=" + URLEncoder.encode(bsiRequest.getVa(), "UTF-8")
						+ "&nominal=" + URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8") + "&nama="
						+ URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
						+ URLEncoder.encode(Common.dateFormat.get().format(bsiRequest.getBillExpired()), "UTF-8")
						+ "&biayaTotal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn + biayaAdministrasi), "UTF-8")
						+ "&qr="
						+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(),
								"UTF-8")
						+ "&terbilang="
						+ URLEncoder.encode(IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)), "UTF-8")
						+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

				Common.displayWindow(myUrl, true, "65%");

			} else {
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}

		return bsiRequest;
	}

	@SuppressWarnings({})
	public static BsiRequest onSaveBsi(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan,
			final JadwalPembayaran jadwalPembayaran, final Integer semester, final String tahunAkademik,
			final String keterangan, final Double pengurangan, final Double nilaiBiayaHarusDiBayars,
			final List<BsiRequestDetail> bsiRequestDetails, final List<BsiRequestDetailBiaya> bsiRequestDetailBiayas,
			final boolean tampilInfo, final Event event, String cicilan) throws Exception {

		if (amn < 0.01) {
			return null;
		}

		return onPilihBsi(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester,
				tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, bsiRequestDetails,
				bsiRequestDetailBiayas, tampilInfo, event, cicilan);

	}

	/**
	 * Catat "Informasi Teknis" kegagalan request BSI sesuai jenis exception — kanal
	 * bersama {@link InfoTeknisPembayaran} agar alert "Transaksi Gagal Dilakukan"
	 * menjelaskan penyebabnya. TIDAK mengubah alur: pemanggil tetap menangani
	 * exception seperti semula, method ini hanya mencatat. Dipakai juga oleh
	 * {@link BsiKeranjangPembayaran}.
	 */
	static void catatKegagalanBsi(Exception e, String strURL, boolean simpanKeDb) {
		String url = strURL == null ? "" : " (" + strURL + ")";
		if (e instanceof java.net.ConnectException) {
			// Gateway BSI tak bisa dihubungi (unreachable/koneksi ditolak).
			InfoTeknisPembayaran.catat("Tidak dapat terhubung ke gateway BSI" + url + ": "
					+ InfoTeknisPembayaran.potong(e.getMessage(), 200) + ". Periksa koneksi/whitelist IP server.");
		} else if (e instanceof java.net.SocketTimeoutException) {
			// Gateway BSI tidak merespons dalam batas waktu baca.
			InfoTeknisPembayaran.catat("Gateway BSI" + url + " tidak merespons dalam batas waktu (timeout): "
					+ InfoTeknisPembayaran.potong(e.getMessage(), 200) + ". Coba beberapa saat lagi.");
		} else if (e instanceof org.apache.commons.httpclient.ConnectTimeoutException) {
			// Awas: subclass InterruptedIOException, BUKAN ConnectException.
			InfoTeknisPembayaran.catat("Koneksi ke gateway BSI" + url + " tidak tersambung dalam batas waktu: "
					+ InfoTeknisPembayaran.potong(e.getMessage(), 200) + ". Periksa jaringan/firewall server.");
		} else if (simpanKeDb) {
			// Respons BSI sudah diterima; kegagalan terjadi saat memproses/menyimpan hasil ke DB.
			InfoTeknisPembayaran.catat("VA/transaksi diterima gateway BSI namun GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
		} else {
			InfoTeknisPembayaran.catat("Gagal memproses request/respons BSI" + url + ": "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
		}
	}

	@SuppressWarnings("deprecation")
	public static BsiRequest sendRequest(String postData, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, Double amount, String merchant_id, String signature, String bill_no,
			String virtual_account, String key, List<BsiRequestDetail> bsiRequestDetails,
			List<BsiRequestDetailBiaya> bsiRequestDetailBiayas, Boolean hapusCicilanSebelumnya, Date billExpired,
			String cicilan) throws Exception {
		// Bersihkan "Informasi Teknis" lama agar kegagalan sebelumnya tidak bocor ke alert transaksi ini.
		InfoTeknisPembayaran.bersihkan();
		Collections.sort(bsiRequestDetails);

		keterangan = "";
		for (BsiRequestDetail bsiRequestDetail : bsiRequestDetails) {
			keterangan += bsiRequestDetail.toString();
		}

		Session session = HibernateUtil.currentNativeSession();
		BsiRequest bsiRequestData = (BsiRequest) session.createCriteria(BsiRequest.class)
				.add(Restrictions.ne("status", "Payment Sukses"))
				.add(Restrictions.or(Restrictions.eq("mahasiswa", mahasiswa),
						Restrictions.eq("biodataCalonMahasiswa", biodataCalonMahasiswa)))
				.add(Restrictions.gt("billExpired", ais.ui.util.WaktuUtil.getDate()))
				.add(Restrictions.eq("semester", semester)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(Restrictions.eq("jenisKegiatan", jenisKegiatan)).add(Restrictions.eq("keterangan", keterangan))

				.setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		System.out.println("bsiRequestData => " + bsiRequestData);
		if (bsiRequestData != null) {
			HibernateUtil.closeSession();
			return bsiRequestData;
		}

		postData = postData.replaceAll("&", "dan");

		// curl_init and url
		String ipClient = (Common.getKonfigurasi("bsi_ip_client", "").getNilai());
		if (!ipClient.trim().isEmpty()) {
			ipClient = ipClient + "/BsiForwarder";
		}
		String strURL = !ipClient.trim().isEmpty() ? ipClient
				: (Common.getKonfigurasi("bsi_gateway_url", "https://apibeta.bsi-ecollection.com/").getNilai());

		BsiRequest bsiRequest = new BsiRequest(cicilan);
		System.out.println("postData = " + postData);

		PostMethod post = new PostMethod(strURL);
		try {
			StringRequestEntity requestEntity = new StringRequestEntity(postData);
			post.setRequestEntity(requestEntity);
			post.setRequestHeader("Content-type", "application/json");
			HttpClient httpclient = new HttpClient();

			int result = httpclient.executeMethod(post);
			System.out.println("Response status code: " + result);
			System.out.println("Response body: ");

			String hasil = post.getResponseBodyAsString();

			System.out.println(hasil);

			JSONObject bsi = new JSONObject(hasil);
			System.out.println("jSONObject = " + bsi);

			String status = bsi.isNull("status") ? "" : bsi.getString("status");

			if (status.trim().equals("102")) {

				bsiRequest = (BsiRequest) session.createCriteria(BsiRequest.class).addOrder(Order.desc("id"))
						.add(Restrictions.eq("va", virtual_account)).setMaxResults(1).uniqueResult();
				HibernateUtil.closeSession();
				if (bsiRequest == null) {
					// Status 102 = VA sudah pernah dibuat di BSI, namun datanya tak ditemukan di aplikasi.
					InfoTeknisPembayaran.catat("Server BSI menjawab status=102 (VA sudah pernah dibuat) namun data "
							+ "request VA " + virtual_account + " tidak ditemukan di aplikasi. URL: " + strURL);
				}
				return bsiRequest;
			}

			if (!status.trim().equals("000")) {
				// BSI menolak permintaan — catat kode status + pesan server agar alert tidak generik.
				String pesanBsi = bsi.optString("message", "");
				if (pesanBsi == null || pesanBsi.trim().isEmpty()) pesanBsi = bsi.optString("description", "");
				InfoTeknisPembayaran.catat("Server BSI menolak permintaan, kode status=" + status
						+ (pesanBsi == null || pesanBsi.trim().isEmpty() ? "" : ", pesan=" + pesanBsi.trim())
						+ ". Respons server: " + InfoTeknisPembayaran.potong(hasil, 300) + ". URL: " + strURL);
				return null;
			}

			String data = bsi.isNull("data") ? "" : bsi.getString("data");

			// String decodeData = "";
			String decodeData = BNIHash.parseData(data, merchant_id, key);
			try {
				JSONObject responseData = new JSONObject(decodeData);
				System.out.println("responseData = " + responseData);

				bsiRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
				bsiRequest.setNama(
						responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));

				bsiRequest.setTrxId(responseData.isNull("trx_id") ? "" : responseData.getString("trx_id"));
				bsiRequest
						.setVa(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
				bsiRequest.setBillNo(bill_no);
				bsiRequest.setMerchant_id(merchant_id);
				bsiRequest.setData(
						responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
				bsiRequest.setMerchant(merchant_id);
				bsiRequest.setResponse_code(status);
				bsiRequest.setResponse_desc(BsiRequestAction.statses.get(status));
				bsiRequest.setMahasiswa(mahasiswa);
				bsiRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
				bsiRequest.setJenisKegiatan(jenisKegiatan);
				bsiRequest.setJadwalPembayaran(jadwalPembayaran);
				bsiRequest.setSemester(semester);
				bsiRequest.setTahunAkademik(tahunAkademik);
				bsiRequest.setKeterangan(keterangan);
				bsiRequest.setPengurangan(pengurangan);
				bsiRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
				bsiRequest.setAmount(amount);
				bsiRequest.setResponse(bsi.toString());
				bsiRequest.setRequest(postData);
				bsiRequest.setBillExpired(billExpired);

				session.getTransaction().begin();
				session.save(bsiRequest);

				for (BsiRequestDetail bsiRequestDetail : bsiRequestDetails) {
					bsiRequestDetail.setBsiRequest(bsiRequest);
					session.save(bsiRequestDetail);
				}

				for (BsiRequestDetailBiaya bsiRequestDetailBiaya : bsiRequestDetailBiayas) {
					bsiRequestDetailBiaya.setBsiRequest(bsiRequest);
					session.save(bsiRequestDetailBiaya);
				}

				session.getTransaction().commit();
			} catch (Exception e) {
				// VA sudah diterima BSI; kegagalan di blok ini = gagal parse/simpan hasil di aplikasi.
				InfoTeknisPembayaran.catat("VA/transaksi diterima gateway BSI namun GAGAL disimpan di aplikasi: "
						+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
				Common.tampilErrorJikaAdmin(e);
				try {
					HibernateUtil.closeSession();
					post.releaseConnection();
				} catch (Exception ee) {
					ee.printStackTrace(); ais.common.ErrorAuditUtil.record(ee, "auto-audit src/ais/common/BsiCommon.java:692");
				}
				return null;
				// System.out.println("Error = " + e.getMessage());
				//
				// if (mahasiswa != null) {
				// bsiRequest = (BsiRequest)
				// session.createCriteria(BsiRequest.class)
				// .add(Restrictions.eq("mahasiswa",
				// mahasiswa)).addOrder(Order.desc("id"))
				// .add(Restrictions.eq("status", "Belum
				// diproses")).setMaxResults(1).uniqueResult();
				// } else if (biodataCalonMahasiswa != null) {
				// bsiRequest = (BsiRequest)
				// session.createCriteria(BsiRequest.class)
				// .add(Restrictions.eq("biodataCalonMahasiswa",
				// biodataCalonMahasiswa))
				// .addOrder(Order.desc("id")).add(Restrictions.eq("status",
				// "Belum diproses"))
				// .setMaxResults(1).uniqueResult();
				// }

			}

		} catch (Exception e) {
			// Catat penyebab kegagalan request untuk alert "Informasi Teknis" (alur tetap).
			catatKegagalanBsi(e, strURL, false);
			Common.tampilErrorJikaAdmin(e);
		} finally {
			post.releaseConnection();
		}

		HibernateUtil.closeSession();

		return bsiRequest;
	}

	@SuppressWarnings("deprecation")
	public static BsiRequest onSaveBsi(Siswa siswa, CalonSiswa calonSiswa, Collection<Tagihan> tag, final Double amn,
			boolean tampilkanFormPembayaran, Double deposit) throws Exception {
		// Bersihkan "Informasi Teknis" lama agar kegagalan sebelumnya tidak bocor ke alert transaksi ini.
		InfoTeknisPembayaran.bersihkan();

		if (amn == null || amn.intValue() == 0) {
			return null;
		}

		List<Tagihan> tagihans = new ArrayList<Tagihan>(tag);
		Collections.sort(tagihans);
		String cicilan = "";
		String keterangan = "";
		for (Tagihan tagihan : tagihans) {
			keterangan += tagihan.getId() + "-" + tagihan.getItemBiayaSekolah().getNama()
					+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")" : "")
					+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
					+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()) + ", ";

			Double nilai = tagihan.getNominal() + tagihan.getDenda();
			cicilan += cicilan.isEmpty() ? ("Bulanan-" + tagihan.getId().toString() + "-" + nilai)
					: "," + ("Bulanan-" + tagihan.getId().toString() + "-" + nilai);
		}

		String[] h = Sekolah.checkCidDanPasswordBsi(siswa, calonSiswa);

		String merchant_id = h[0]; // from BNI
		String Password = h[1]; // from BNI

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) + 1);
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);

		String bill_no = Common.getGeneratedBarCode();

		String jam_terakhir_pembayaran = Common.getKonfigurasi("jam_terakhir_pembayaran", "").getNilai();
		if (jam_terakhir_pembayaran != null && !jam_terakhir_pembayaran.trim().isEmpty()) {
			try {
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.set(Calendar.HOUR_OF_DAY,
						calendar1.get(Calendar.HOUR_OF_DAY) + Integer.parseInt(jam_terakhir_pembayaran));
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				calendar = calendar1;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/BsiCommon.java:773");
			}
		}

		Date tanggalExpired = null;
		String tanggal_terakhir_pembayaran = Common.getKonfigurasi("tanggal_terakhir_pembayaran", "").getNilai();
		if (tanggal_terakhir_pembayaran != null && !tanggal_terakhir_pembayaran.trim().isEmpty()) {
			try {
				tanggalExpired = Common.dateFormat1.get().parse(tanggal_terakhir_pembayaran);
				Calendar calendar1 = ais.ui.util.WaktuUtil.getCalendar();
				calendar1.setTime(tanggalExpired);
				calendar1.set(Calendar.HOUR_OF_DAY, 23);
				calendar1.set(Calendar.MINUTE, 59);
				calendar1.set(Calendar.SECOND, 59);
				tanggalExpired = calendar1.getTime();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiCommon.java:788");

			}
		}

		String datetime_expired = tanggalExpired != null ? Common.databaseDateFormat1.get().format(tanggalExpired)
				: Common.databaseDateFormat1.get().format(calendar.getTime());

		if (tanggalExpired == null) {
			tanggalExpired = calendar.getTime();
		}

		String virtual_account = "";

		Session session = HibernateUtil.currentNativeSession();

		boolean tambahkanMerchanId = Common.bolehKonfigurasi("tambahkan_merchan_id_di_bsi");

		if (Common.bolehKonfigurasi("angka_va_bsi_menggunakan_nim")) {
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bsi", "8").getNilai()
					+ (tambahkanMerchanId ? merchant_id : "")
					+ (siswa != null ? siswa.getNim() : (calonSiswa != null ? calonSiswa.getNoRegistrasi() : ""));
		} else {
			int generatedAngkaDigit = 8;
			try {
				generatedAngkaDigit = Integer
						.parseInt(Common.getKonfigurasi("generated_angka_digit_bsi", "8").getNilai());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiCommon.java:815");

			}
			virtual_account = Common.getKonfigurasi("angka_prefix_va_bsi", "8").getNilai()
					+ (tambahkanMerchanId ? merchant_id : "") + Common.getGeneratedAngkaDigit(generatedAngkaDigit);
		}

		int generatedAngkaDigit = 16;
		try {
			generatedAngkaDigit = Integer
					.parseInt(Common.getKonfigurasi("virtual_account_angka_digit_bsi", "16").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiCommon.java:826");

		}
		virtual_account = (virtual_account + "00000000000000000").substring(0, generatedAngkaDigit);
		// }

		Double biayaAdministrasi = 0.0;
		try {
			biayaAdministrasi = Double.parseDouble(Common.getKonfigurasi("bsi_biaya_administrasi", "0.0").getNilai());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/BsiCommon.java:835");

		}

		String description = "";
		if (!tambahkanMerchanId) {
			description = ",\"description\":\"" + keterangan + "\"";
		}

		// BNIHash hash = new BNIHash();
		String data = "{\"customer_email\":\""
				+ (siswa != null ? siswa.getAlamatEmail().split(",")[0].trim()
						: (calonSiswa == null ? "test@email.com" : calonSiswa.getAlamatEmail().split(",")[0].trim()))
				+ "\",\"trx_id\":\"" + bill_no + "\",\"datetime_expired\":\"" + datetime_expired + "\",\"client_id\":\""
				+ merchant_id + "\",\"customer_phone\":\""
				+ (siswa != null ? siswa.getTeleponSiswa()
						: (calonSiswa != null ? calonSiswa.getTeleponOrangTua() : ""))
				+ "\",\"customer_name\":\""
				+ (siswa != null ? siswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") : (calonSiswa != null ? calonSiswa.getNama().replaceAll("[^\\sa-zA-Z0-9]", "") : ""))
				+ "\",\"type\":\"createbilling\",\"virtual_account\":\"" + virtual_account + "\",\"trx_amount\":\""
				+ (biayaAdministrasi.intValue() + amn.intValue()) + "\",\"billing_type\":\"c\"  " + description + " }";

		String cid = merchant_id; // from BNI
		String key = Password; // from BNI

		String parsedData = BNIHash.hashData(data, cid, key);
		String decodeData = BNIHash.parseData(parsedData, cid, key);

		System.out.println("parsedData = " + parsedData);
		System.out.println("decodeData = " + decodeData);

		String postData = "{ \"client_id\":\"" + merchant_id + "\", \"data\":\"" + parsedData + "\"}";

		// BsiRequest bsiRequestData = (BsiRequest)
		// session.createCriteria(BsiRequest.class)
		// .add(Restrictions.ne("status", "Payment Sukses"))
		// .add(Restrictions.or(Restrictions.eq("siswa", siswa),
		// Restrictions.eq("calonSiswa", calonSiswa)))
		// .add(Restrictions.gt("billExpired", new
		// Date())).add(Restrictions.eq("keterangan", keterangan))
		// .setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		// System.out.println("bsiRequestData => " + bsiRequestData);
		// if (bsiRequestData != null) {
		// HibernateUtil.closeSession();
		// return bsiRequestData;
		// }
		//
		// BsiRequest bsiRequestTemp = (BsiRequest)
		// session.createCriteria(BsiRequest.class)
		// .add(Restrictions.ne("status", "Payment Sukses"))
		// .add(Restrictions.or(Restrictions.eq("siswa", siswa),
		// Restrictions.eq("calonSiswa", calonSiswa)))
		// .add(Restrictions.eq("va",
		// virtual_account)).add(Restrictions.eq("keterangan", keterangan))
		// .setMaxResults(1).addOrder(Order.desc("id")).uniqueResult();
		// System.out.println("bsiRequestTemp => " + bsiRequestTemp);
		// final BsiRequest bsiRequest = bsiRequestTemp == null ? new
		// BsiRequest() : bsiRequestTemp;
		//
		BsiRequest bsiRequest = new BsiRequest(cicilan);

		try {

//			System.out.println("postData -> " + postData);

			// curl_init and url
			String ipClient = (Common.getKonfigurasi("bsi_ip_client", "").getNilai());
			if (!ipClient.trim().isEmpty()) {
				ipClient = ipClient + "/BsiForwarder";
			}
			String strURL = siswa != null && siswa.getSekolah() != null
					&& !siswa.getSekolah().getBsiGatewayUrl().isEmpty()
							? siswa.getSekolah().getBsiGatewayUrl()
							: !ipClient.trim().isEmpty() ? ipClient
									: (Common.getKonfigurasi("bsi_gateway_url", "https://apibeta.bsi-ecollection.com/")
											.getNilai());

			System.out.println("url = " + strURL);
			System.out.println("postData = " + postData);

			// Penanda: respons BSI sudah diterima, kegagalan berikutnya berarti gagal simpan ke DB.
			boolean simpanKeDb = false;
			PostMethod post = new PostMethod(strURL);
			try {
				StringRequestEntity requestEntity = new StringRequestEntity(postData);
				post.setRequestEntity(requestEntity);
				post.setRequestHeader("Content-type", "application/json");
				HttpClient httpclient = new HttpClient();

				int result = httpclient.executeMethod(post);
				System.out.println("Response status code: " + result);
				System.out.println("Response body: ");

				String hasil = post.getResponseBodyAsString();

				System.out.println(hasil);

				JSONObject bsi = new JSONObject(hasil);
				System.out.println("jSONObject = " + bsi);

				String status = bsi.isNull("status") ? "" : bsi.getString("status");

				if (status.trim().equals("102")) {
					session = HibernateUtil.currentNativeSession();
					BsiRequest bsiRequesttemp = (BsiRequest) session.createCriteria(BsiRequest.class)
							.addOrder(Order.desc("id")).add(Restrictions.eq("va", virtual_account)).setMaxResults(1)
							.uniqueResult();
					HibernateUtil.closeSession();
					if (bsiRequesttemp == null) {
						// Status 102 = VA sudah pernah dibuat di BSI, namun datanya tak ditemukan di aplikasi.
						InfoTeknisPembayaran.catat("Server BSI menjawab status=102 (VA sudah pernah dibuat) namun data "
								+ "request VA " + virtual_account + " tidak ditemukan di aplikasi. URL: " + strURL);
					}
					return bsiRequesttemp;
				}

				if (!status.trim().equals("000")) {
					// BSI menolak permintaan — catat kode status + pesan server agar alert tidak generik.
					String pesanBsi = bsi.optString("message", "");
					if (pesanBsi == null || pesanBsi.trim().isEmpty()) pesanBsi = bsi.optString("description", "");
					InfoTeknisPembayaran.catat("Server BSI menolak permintaan, kode status=" + status
							+ (pesanBsi == null || pesanBsi.trim().isEmpty() ? "" : ", pesan=" + pesanBsi.trim())
							+ ". Respons server: " + InfoTeknisPembayaran.potong(hasil, 300) + ". URL: " + strURL);
					return null;
				}

				data = bsi.isNull("data") ? "" : bsi.getString("data");

				// String decodeData = "";
				decodeData = BNIHash.parseData(data, merchant_id, key);

				JSONObject responseData = new JSONObject(decodeData);
				System.out.println("responseData = " + responseData);

				bsiRequest.setHapusCicilanSebelumnya(true);
				bsiRequest.setNama(responseData.isNull("custCode") ? "" : responseData.getString("custCode"));

				bsiRequest.setHapusCicilanSebelumnya(true);
				bsiRequest.setNama(
						responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));

				bsiRequest.setTrxId(responseData.isNull("trx_id") ? "" : responseData.getString("trx_id"));
				bsiRequest
						.setVa(responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
				bsiRequest.setBillNo(bill_no);
				bsiRequest.setMerchant_id(merchant_id);
				bsiRequest.setData(
						responseData.isNull("virtual_account") ? "" : responseData.getString("virtual_account"));
				bsiRequest.setMerchant(merchant_id);
				bsiRequest.setResponse_code(status);
				bsiRequest.setResponse_desc(BsiRequestAction.statses.get(status));

				bsiRequest.setSiswa(siswa);
				bsiRequest.setCalonSiswa(calonSiswa);
				bsiRequest.setNilaiBiayaHarusDiBayars(amn);
				bsiRequest.setAmount(amn);
				bsiRequest.setResponse(hasil);
				bsiRequest.setRequest(postData);
				bsiRequest.setBillExpired(tanggalExpired);

				bsiRequest.setKeterangan(keterangan);

				bsiRequest.setDeposit(deposit);

				// Respons BSI sudah diterima & diparse; kegagalan setelah ini berarti gagal simpan ke DB.
				simpanKeDb = true;

				session.getTransaction().begin();
				session.saveOrUpdate(bsiRequest);
				session.getTransaction().commit();

				session.createSQLQuery("delete from bsi_request_detail where bsi_request=" + bsiRequest.getId())
						.executeUpdate();
				for (Tagihan tagihan : tagihans) {
					BsiRequestDetail bsiRequestDetail = new BsiRequestDetail();
					bsiRequestDetail.setBsiRequest(bsiRequest);
					bsiRequestDetail.setTagihan(tagihan);
					bsiRequestDetail.setNilai(tagihan.getNominal() + tagihan.getDenda());
					bsiRequestDetail.setKeterangan(tagihan.getItemBiayaSekolah().getNama()
							+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")"
									: "")
							+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
							+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()));
					session.getTransaction().begin();
					session.save(bsiRequestDetail);
					session.getTransaction().commit();

					tagihan.setVa(virtual_account);
					tagihan.setExpired(tanggalExpired);

					session.getTransaction().begin();
					session.update(tagihan);
					session.getTransaction().commit();
				}

			} catch (Exception e) {
				// Catat penyebab kegagalan untuk alert "Informasi Teknis" (alur tetap seperti semula).
				catatKegagalanBsi(e, strURL, simpanKeDb);
				Common.tampilErrorJikaAdmin(e);
				System.out.println("Error = " + e.getMessage());
			}

		} catch (Exception e) {
			// Catat penyebab kegagalan untuk alert "Informasi Teknis" (strURL belum tentu terisi di sini).
			catatKegagalanBsi(e, null, false);
			Common.tampilErrorJikaAdmin(e);
		}

		HibernateUtil.closeSession();
		if (tampilkanFormPembayaran) {
			if (bsiRequest != null && bsiRequest.getVa() != null && !bsiRequest.getVa().trim().isEmpty()) {

				String code = bsiRequest.getVa();

				File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + bsiRequest.getId() + ".png");

				BarcodeCommon.generateCRCode(code, myfilebarcode1);

				String nama = siswa != null ? siswa.getNama() : (calonSiswa != null ? calonSiswa.getNama() : "");
				String myUrl = "/common/bsi/no_va.zul?va=" + URLEncoder.encode(bsiRequest.getVa(), "UTF-8")
						+ "&nominal=" + URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn), "UTF-8")
						+ "&biayaAdministrasi="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(biayaAdministrasi), "UTF-8") + "&nama="
						+ URLEncoder.encode(nama, "UTF-8") + "&kadalurasa="
						+ URLEncoder.encode(Common.dateFormat.get().format(bsiRequest.getBillExpired()), "UTF-8")
						+ "&biayaTotal="
						+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(amn + biayaAdministrasi), "UTF-8")
						+ "&qr="
						+ URLEncoder.encode(Common.getRequestHostWithProtocol() + "/report/" + myfilebarcode1.getName(),
								"UTF-8")
						+ "&terbilang="
						+ URLEncoder.encode(IndonesianNumberToWords.convert((long) (amn + biayaAdministrasi)), "UTF-8")
						+ "&tampilBiayaAdministrasi=" + (biayaAdministrasi > 0.1);

				Common.displayWindow(myUrl, true, "65%");

			} else {
				MyMessageboxConfig.show(InfoTeknisPembayaran.pesanGagal(), "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.EXCLAMATION);

			}
		}
		return bsiRequest;
	}
}
