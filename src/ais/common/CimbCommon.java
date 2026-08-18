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
import ais.database.model.cimb.CimbRequest;
import ais.database.model.cimb.CimbRequestDetail;
import ais.database.model.cimb.CimbRequestDetailBiaya;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyDoubleboxMin;
import ais.ui.util.MyMessageboxConfig;

public class CimbCommon {

	public static MyButtonConfig createButton() {
		File fileViaCimb = new File(Common.REAL_PATH + "/img/cimb-logo.jpg");
		try {

			LampiranLain lainMahasiswa = LampiranLain.ambil(LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_CIMB,
					LampiranLain.BG_TOMBOL_PEMBAYARAN_VIA_CIMB_STR);

			if (lainMahasiswa != null && lainMahasiswa.ambilFile() != null) {
				fileViaCimb = lainMahasiswa.ambilFile();
				File fileDiImg = new File(Common.REAL_PATH + "/img/" + fileViaCimb.getName());
				boolean ada = fileDiImg.exists();
				System.out.println("fileViaCimb = " + fileViaCimb + ", fileDiImg = " + fileDiImg + ", ada = " + ada);
				if (!ada) {
					FileInputStream fileInputStream = new FileInputStream(fileViaCimb);
					FileOutputStream fileOutputStream = new FileOutputStream(fileDiImg);
					IOUtils.copyLarge(fileInputStream, fileOutputStream);
					fileInputStream.close();
					fileOutputStream.close();
				}
			}

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		MyButtonConfig bayarViaCimb = new MyButtonConfig(
				Common.getKonfigurasi("label_pembayaran_via_cimb", "Bayar via CIMB Niaga").getNilai(),
				"/img/" + fileViaCimb.getName());
		return bayarViaCimb;
	}

	@SuppressWarnings("unchecked")
	public static List<CimbRequestDetailBiaya> populateDetailBiaya(Grid gridss, List<MyDoubleboxMin> pengurangan) {
		List<CimbRequestDetailBiaya> cimbRequestDetailBiayas = new ArrayList<CimbRequestDetailBiaya>();
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
					if (component instanceof Doublebox
							&& detailBiaya.getItemBiaya().getNilaiBisaDiubah()) {
						Doublebox jumlah = (Doublebox) component;
						biaya = jumlah.getValue() == null ? 0.0 : jumlah.getValue();
					} else if (component instanceof Label) {
						Label myLabel = (Label) component;
						// System.out.println("myLabel = " +
						// myLabel.getValue());
						biaya = Common.numberFormat.get().parse(myLabel.getValue()).doubleValue();
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CimbCommon.java:110");
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

				CimbRequestDetailBiaya cimbRequestDetailBiaya = new CimbRequestDetailBiaya();
				cimbRequestDetailBiaya.setDetailBiaya(detailBiaya);
				cimbRequestDetailBiaya.setNilai(biaya);
				cimbRequestDetailBiayas.add(cimbRequestDetailBiaya);
			}
		}
		return cimbRequestDetailBiayas;
	}

	public static List<CimbRequestDetail> populateCimbRequestDetailDariDetailBiaya(
			List<CimbRequestDetailBiaya> cimbRequestDetailBiayas) {
		List<CimbRequestDetail> cimbRequestDetails = new ArrayList<CimbRequestDetail>();

		int i = 1;
		for (CimbRequestDetailBiaya cimbRequestDetailBiaya : cimbRequestDetailBiayas) {
			CimbRequestDetail cimbRequestDetail = new CimbRequestDetail();
			cimbRequestDetail.setPengaturanPembayaranBulanan(null);
			cimbRequestDetail.setItemBiaya(cimbRequestDetailBiaya.getDetailBiaya().getItemBiaya());
			cimbRequestDetail.setKeterangan(cimbRequestDetailBiaya.getKeterangan());
			cimbRequestDetail.setNilai(cimbRequestDetailBiaya.getNilai());
			cimbRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			cimbRequestDetail.setKe(i);
			cimbRequestDetails.add(cimbRequestDetail);
			i++;
		}

		return cimbRequestDetails;
	}

	public static List<CimbRequestDetail> populateCimbRequestDetail(HttpServletRequest request, Mahasiswa mahasiswa,
			String validator, Integer semester) {

		String jenis = request.getParameter("jenis") == null ? "bulanan" : request.getParameter("jenis");
		String data = request.getParameter("data") == null ? "" : request.getParameter("data");
		System.out.println("jenis => " + jenis + ", data => " + data);
		List<CimbRequestDetail> cimbRequestDetails = new ArrayList<CimbRequestDetail>();

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

			CimbRequestDetail cimbRequestDetail = new CimbRequestDetail();
			cimbRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
			cimbRequestDetail.setDetailBiaya(detailBiaya);
			cimbRequestDetail.setItemBiaya(itemBiaya);
			cimbRequestDetail.setKeterangan(keterangan);
			cimbRequestDetail.setNilai(nilai);
			cimbRequestDetail.setTanggal(ais.ui.util.WaktuUtil.getDate());
			cimbRequestDetail.setKe(i);
			cimbRequestDetail.setDenda(0.0);
			cimbRequestDetail.setNilaiAsli(nilai);
			cimbRequestDetails.add(cimbRequestDetail);
			i++;

		}

		HibernateUtil.closeSession();

		return cimbRequestDetails;
	}

	public static List<CimbRequestDetail> populateCimbRequestDetail(Grid gridCicilan, Mahasiswa mahasiswa,
			Integer semester, JadwalPembayaran jadwalPembayaran) {
		@SuppressWarnings("unchecked")
		List<Row> mycicilanrows = gridCicilan.getRows().getChildren();
		List<CimbRequestDetail> cimbRequestDetails = new ArrayList<CimbRequestDetail>();

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

				CimbRequestDetail cimbRequestDetail = new CimbRequestDetail();

				Object jenisBiaya = myItemBiaya.getSelectedItem() == null ? null
						: myItemBiaya.getSelectedItem().getValue();
				PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
						.getPengaturanPembayaranBulanan();
				ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();
				if (jenisBiaya != null && jenisBiaya instanceof PengaturanPembayaranBulanan) {
					pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) jenisBiaya;
					itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
				} else if (jenisBiaya != null && jenisBiaya instanceof ItemBiaya) {
					itemBiaya = (ItemBiaya) jenisBiaya;
				}

				cimbRequestDetail.setIdCicilan(cicilanPembayaran == null ? null : cicilanPembayaran.getId());
				cimbRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
				cimbRequestDetail.setItemBiaya(itemBiaya);
				cimbRequestDetail.setKeterangan(keterangan.getValue());
				cimbRequestDetail.setNilai(jumlahCicilan.getValue());
				cimbRequestDetail.setTanggal(tanggal.getValue());
				cimbRequestDetail.setKe(i);

				cimbRequestDetail.setDenda(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getDenda());
				cimbRequestDetail.setNilaiAsli(cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
						: cicilanPembayaran.getNilaiAsli());

				if (cicilanPembayaran == null || cicilanPembayaran.getId() == null) {
					if (pengaturanPembayaranBulanan != null) {
						JadwalPembayaran jdw = jadwalPembayaran != null && jadwalPembayaran.getKhususUntukNim() != null
								&& jadwalPembayaran.getKhususUntukNim().contains("," + mahasiswa.getNim() + ",")
										? jadwalPembayaran
										: null;
						Double nom = pengaturanPembayaranBulanan.ambilNominalModifikasi(mahasiswa, semester);
						Double denda = pengaturanPembayaranBulanan.checkDenda(nom, cimbRequestDetail.getTanggal(), jdw,
								jadwalPembayaran == null ? null : jadwalPembayaran.getJenisKegiatan())
								- nom;
						cimbRequestDetail.setDenda(denda);
						cimbRequestDetail.setNilaiAsli(nom);
					}
				}

				cimbRequestDetails.add(cimbRequestDetail);
				i++;
			}
		}

		return cimbRequestDetails;
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

				List<CimbRequestDetailBiaya> cimbRequestDetailBiayas = new ArrayList<CimbRequestDetailBiaya>();
				for (DetailBiaya detailBiaya : detailBiayas) {
					CimbRequestDetailBiaya cimbRequestDetailBiaya = new CimbRequestDetailBiaya();
					cimbRequestDetailBiaya.setDetailBiaya(detailBiaya);
					cimbRequestDetailBiaya
							.setNilai((detailBiaya.getNilaiBiayaBaru() == null ? detailBiaya.getNilaiBiaya()
									: detailBiaya.getNilaiBiayaBaru()));
					cimbRequestDetailBiayas.add(cimbRequestDetailBiaya);
					nilaiBiayaHarusDiBayars += cimbRequestDetailBiaya.getNilai();
				}

				Double nilaiYgAkanDibayar = Common.numberFormat.get()
						.parse(Common.numberFormat.get().format(nilaiBiayaHarusDiBayars)).doubleValue();
				CimbCommon.onSaveCimb(nilaiYgAkanDibayar, null, calonMahasiswa, jenisKegiatan, jadwalPembayaran, 1,
						calonMahasiswa.getTahunAkademik(), "Pembayaran Pendaftaran Mahasiswa Baru", 0.0,
						nilaiBiayaHarusDiBayars,
						CimbCommon.populateCimbRequestDetailDariDetailBiaya(cimbRequestDetailBiayas),
						cimbRequestDetailBiayas, null);

			}
		}

	}

	public static boolean onPilihCimb(final Double amn, Mahasiswa mahasiswa,
			BiodataCalonMahasiswa biodataCalonMahasiswa, JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran,
			Integer semester, String tahunAkademik, String keterangan, Double pengurangan,
			Double nilaiBiayaHarusDiBayars, List<CimbRequestDetail> cimbRequestDetails,
			List<CimbRequestDetailBiaya> cimbRequestDetailBiayas, Event event) throws Exception {

		final CimbRequest cimbRequest = CimbCommon.sendRequest(mahasiswa, biodataCalonMahasiswa, jenisKegiatan,
				jadwalPembayaran, semester, tahunAkademik, keterangan, pengurangan, nilaiBiayaHarusDiBayars, amn,
				cimbRequestDetails, cimbRequestDetailBiayas, true);
		if (cimbRequest != null) {

			String myUrl = "/common/cimb/no_va.zul?va=" + URLEncoder.encode(cimbRequest.getTrxId(), "UTF-8")
					+ "&nominal="
					+ URLEncoder.encode("Rp. " + Common.numberFormat.get().format(cimbRequest.getAmount()), "UTF-8");

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
	public static boolean onSaveCimb(final Double amn, final Mahasiswa mahasiswa,
			final BiodataCalonMahasiswa biodataCalonMahasiswa, final JenisKegiatan jenisKegiatan,
			final JadwalPembayaran jadwalPembayaran, final Integer semester, final String tahunAkademik,
			final String keterangan, final Double pengurangan, final Double nilaiBiayaHarusDiBayars,
			final List<CimbRequestDetail> cimbRequestDetails,
			final List<CimbRequestDetailBiaya> cimbRequestDetailBiayas, final Event event) throws Exception {

		if (amn < 0.01) {
			return false;
		}

		onPilihCimb(amn, mahasiswa, biodataCalonMahasiswa, jenisKegiatan, jadwalPembayaran, semester, tahunAkademik,
				keterangan, pengurangan, nilaiBiayaHarusDiBayars, cimbRequestDetails, cimbRequestDetailBiayas, event);

		return true;
	}

	public static CimbRequest sendRequest(Mahasiswa mahasiswa, BiodataCalonMahasiswa biodataCalonMahasiswa,
			JenisKegiatan jenisKegiatan, JadwalPembayaran jadwalPembayaran, Integer semester, String tahunAkademik,
			String keterangan, Double pengurangan, Double nilaiBiayaHarusDiBayars, Double amount,
			List<CimbRequestDetail> cimbRequestDetails, List<CimbRequestDetailBiaya> cimbRequestDetailBiayas,
			Boolean hapusCicilanSebelumnya) throws Exception {

		// Bersihkan detail kegagalan lama agar info transaksi sebelumnya tidak bocor ke alert.
		InfoTeknisPembayaran.bersihkan();

		CimbRequest cimbRequest = new CimbRequest();

		try {

			cimbRequest.setHapusCicilanSebelumnya(hapusCicilanSebelumnya);
			cimbRequest.setNama(mahasiswa == null ? biodataCalonMahasiswa.toString() : mahasiswa.toString());
			cimbRequest.setMahasiswa(mahasiswa);
			cimbRequest.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
			cimbRequest.setJenisKegiatan(jenisKegiatan);
			cimbRequest.setJadwalPembayaran(jadwalPembayaran);
			cimbRequest.setSemester(semester);
			cimbRequest.setTahunAkademik(tahunAkademik);
			cimbRequest.setKeterangan(keterangan);
			cimbRequest.setPengurangan(pengurangan);
			cimbRequest.setNilaiBiayaHarusDiBayars(nilaiBiayaHarusDiBayars);
			cimbRequest.setAmount(amount);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(cimbRequest);
			session.getTransaction().commit();

			for (CimbRequestDetail cimbRequestDetail : cimbRequestDetails) {
				cimbRequestDetail.setCimbRequest(cimbRequest);
				session.getTransaction().begin();
				session.save(cimbRequestDetail);
				session.getTransaction().commit();
			}

			for (CimbRequestDetailBiaya cimbRequestDetailBiaya : cimbRequestDetailBiayas) {
				cimbRequestDetailBiaya.setCimbRequest(cimbRequest);
				session.getTransaction().begin();
				session.save(cimbRequestDetailBiaya);
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			// CIMB tidak memanggil gateway di sini (VA dibuat lokal) — kegagalan berarti
			// request GAGAL disimpan di aplikasi. Catat detailnya agar alert tidak generik.
			InfoTeknisPembayaran.catat("Request CIMB Niaga GAGAL disimpan di aplikasi: "
					+ e.getClass().getSimpleName() + " - " + InfoTeknisPembayaran.potong(e.getMessage(), 200));
			Common.tampilErrorJikaAdmin(e);
		}

		return cimbRequest;
	}

}
