/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ws.billpayment.h2h.bankmandiri.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DendaPembayaran;
import ais.database.model.DendaPembayaranNominal;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.TunggakanMahasiswa;
import ws.billpayment.h2h.bankmandiri.BillDetail;
import ws.billpayment.h2h.bankmandiri.PaymentResponse;
import ws.billpayment.h2h.bankmandiri.util.CommonUtil;
import ws.billpayment.h2h.bankmandiri.util.ConstantUtilBankMandiri;
import ws.billpayment.h2h.bankmandiri.util.DisplayUtil;

/**
 * 
 * @author Fauzi
 */
public class PaymentLogic {

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	public DisplayUtil displayUtil = new DisplayUtil();

	public PaymentResponse pembayaranCalonMahasiswaBaru(String noRegistrasi, String nama, LogHostToHost logHostToHost,
			String paymentAmount) {

		Double nominalTagihan = 0.0;
		try {
			nominalTagihan = Double.parseDouble(paymentAmount.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ws/billpayment/h2h/bankmandiri/logic/PaymentLogic.java:57");

		}

		BiodataCalonMahasiswa biodataCalonMahasiswa = pembayaranUtil.getCalonMahasiswaByNoPendaftaran(noRegistrasi);
		if (biodataCalonMahasiswa == null) {
			PaymentResponse paymentResponse = new PaymentResponse();
			paymentResponse.setBillInfo1(noRegistrasi);
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNoRegistrasiNotFound(logHostToHost, noRegistrasi, pembayaranUtil.getBankHost(), nama,
					ConstantUtil.PAY);
			return paymentResponse;
		}
		biodataCalonMahasiswa.setProgram(
				biodataCalonMahasiswa.getProgram() == null ? "Reguler" : biodataCalonMahasiswa.getProgram());

		Boolean ganjil = true;
		Integer semester = 1;
		List<String[]> data = new ArrayList<String[]>();

		JenisKegiatan jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA);

		Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
				ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, biodataCalonMahasiswa.getJenjang(),
				biodataCalonMahasiswa.getTahunAkademik(),
				biodataCalonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL),
				biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
				biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());

		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
		DendaPembayaran dendaPembayaran = (DendaPembayaran) serializables[1];
		DendaPembayaranNominal dendaPembayaranNominal = (DendaPembayaranNominal) serializables[2];

		if (jadwalPembayaran == null) {
			displayUtil
					.displayPembayaranTerlambat(logHostToHost,
							biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
									: biodataCalonMahasiswa.getNim(),
							pembayaranUtil.getBankHost(), nama, ConstantUtil.PAY);
			PaymentResponse paymentResponse = new PaymentResponse();
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return paymentResponse;
		}

		Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
				: biodataCalonMahasiswa.getProdi1();
		java.util.Collection<DetailBiaya> detailBiayas = pembayaranUtil
				.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, false);

		java.util.Collection<BillDetail> billDetails;
		if (myjurusan1 != null /* || myjurusan2 != null */) {
			billDetails = CommonUtil.generateBillDetailsCalonMahasiswa(dendaPembayaran, dendaPembayaranNominal,
					detailBiayas);
		} else {
			billDetails = CommonUtil.generateBillDetailsCalonMahasiswaTanpaProdi(dendaPembayaran,
					dendaPembayaranNominal, detailBiayas);
		}

		String pemb = "|";
		Long total = 0L;
		for (BillDetail biaya : billDetails) {
			total += Long.parseLong(biaya.getBillAmount());
			pemb += biaya.getBillCode() + "\\" + biaya.getBillName() + "\\" + biaya.getBillShortName() + "\\"
					+ biaya.getBillAmount() + "|";
		}

		PaymentResponse paymentResponse = CommonUtil.constructPaymentResponseCalonMahasiswa(biodataCalonMahasiswa,
				ganjil);

		Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(jenisKegiatan);
		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost,
					biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNim(),
					bankHost, nama, ConstantUtil.PAY);
			paymentResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if (kegiatan == null || kegiatan.getId() == null) {
			kegiatan = pembayaranUtil.simpanPembayaranCalonMahasiswa(bankHost, jadwalPembayaran, jenisKegiatan,
					biodataCalonMahasiswa, detailBiayas, nominalTagihan);
			if (kegiatan == null || kegiatan.getId() == null) {
				displayUtil.displayKesalahanSistem(logHostToHost, biodataCalonMahasiswa.getNoRegistrasi(), bankHost,
						nama, ConstantUtil.PAY);
				paymentResponse.setStatus(ConstantUtilBankMandiri.PROVIDER_DATABASE_PROBLEM);
			} else {
				paymentResponse.setBillInfo6(kegiatan.getRefNumber());
				paymentResponse.setBillInfo7(kegiatan.getRefNumber());
				// menghubungkan log host to host dengan kegiatan
				logHostToHost.setKegiatan(kegiatan);
				paymentResponse
						.setStatus(total.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
								: ConstantUtilBankMandiri.SUCCESS_MANDIRI);
				data.add(new String[] { "response_code", total.equals(0L) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT
						: ConstantUtilBankMandiri.SUCCESS });
				data.add(new String[] { "response_description",
						total.equals(0L) ? "Pembayaran gagal dilakukan" : "Pembayaran Sukses Dilakukan" });
				data.add(new String[] { "nim", biodataCalonMahasiswa.getNoRegistrasi() });
				data.add(new String[] { "kurs", "IDR" });
				data.add(new String[] { "nama", biodataCalonMahasiswa.getNama() });
				data.add(new String[] { "program", biodataCalonMahasiswa.getProgram() });

				String myJurusan = "";
				String myfakultas = "";
				if (biodataCalonMahasiswa.getProdi1() != null) {
					myJurusan += biodataCalonMahasiswa.getProdi1().getNama();
					myfakultas = biodataCalonMahasiswa.getProdi1().getFakultas().getNama();
				}
				if (biodataCalonMahasiswa.getProdi2() != null) {
					myJurusan += !myJurusan.equals("") ? " dan " + biodataCalonMahasiswa.getProdi2().getNama()
							: biodataCalonMahasiswa.getProdi2().getNama();
					myfakultas = biodataCalonMahasiswa.getProdi2().getFakultas().getNama();
				}
				data.add(new String[] { "fakultas", myfakultas });
				data.add(new String[] { "prodi", myJurusan });
				data.add(new String[] { "angkatan", biodataCalonMahasiswa.getTahun() + "" });

				data.add(new String[] { "semester", ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
				data.add(new String[] { "semester_ke", semester + "" });
				data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
				data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
				data.add(new String[] { "amount", pemb });
				data.add(new String[] { "total_amount", total + "" });
				data.add(new String[] { "kode_status_pembayaran",
						ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA });
				data.add(new String[] { "keterangan_status_pembayaran",
						ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
				logHostToHost.setResponseCode((ConstantUtilBankMandiri.SUCCESS));
				logHostToHost.setResponseDescription("Pembayaran Sukses Dilakukan");
			}
		} else {
			paymentResponse.setBillInfo6(kegiatan.getRefNumber());
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_SUDAH_DIBAYAR);
			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID });
			data.add(new String[] { "response_description",
					"Pembayaran gagal dilakukan, karena mahasiswa sudah melakukan pembayaran" });
			data.add(new String[] { "nim",
					biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNim() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", biodataCalonMahasiswa.getNama() });
			data.add(new String[] { "program", biodataCalonMahasiswa.getProgram() });
			String myJurusan = "";
			String myfakultas = "";
			if (biodataCalonMahasiswa.getProdi1() != null) {
				myJurusan += biodataCalonMahasiswa.getProdi1().getNama();
				myfakultas = biodataCalonMahasiswa.getProdi1().getFakultas().getNama();
			}
			if (biodataCalonMahasiswa.getProdi2() != null) {
				myJurusan += !myJurusan.equals("") ? " dan " + biodataCalonMahasiswa.getProdi2().getNama()
						: biodataCalonMahasiswa.getProdi2().getNama();
				myfakultas = biodataCalonMahasiswa.getProdi2().getFakultas().getNama();
			}
			data.add(new String[] { "fakultas", myfakultas });
			data.add(new String[] { "prodi", myJurusan });
			data.add(new String[] { "angkatan", biodataCalonMahasiswa.getTahun() + "" });
			data.add(new String[] { "semester", ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
			data.add(new String[] { "semester_ke", semester + "" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran",
					ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
			logHostToHost.setResponseCode((ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID));
			logHostToHost
					.setResponseDescription("Pembayaran gagal dilakukan, karena mahasiswa sudah melakukan pembayaran");
		}

		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
				: biodataCalonMahasiswa.getNim());
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(ConstantUtil.PAY);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);

		biodataCalonMahasiswa.setPembayaranRegistrasi(kegiatan);
		Common.refreshUpdate(session, biodataCalonMahasiswa);

		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return paymentResponse;
	}

	public PaymentResponse pembayaranMahasiswaBaru(String noUjian, String nama, LogHostToHost logHostToHost,
			String paymentAmount) {

		Double nominalTagihan = 0.0;
		try {
			nominalTagihan = Double.parseDouble(paymentAmount.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ws/billpayment/h2h/bankmandiri/logic/PaymentLogic.java:261");

		}

		BiodataCalonMahasiswa biodataCalonMahasiswa = pembayaranUtil.getCalonMahasiswaByNoUjian(noUjian);
		if (biodataCalonMahasiswa == null) {
			PaymentResponse paymentResponse = new PaymentResponse();
			paymentResponse.setBillInfo1(noUjian);
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNoRegistrasiNotFound(logHostToHost, noUjian, pembayaranUtil.getBankHost(), nama,
					ConstantUtil.PAY);
			return paymentResponse;
		}
		biodataCalonMahasiswa.setProgram(
				biodataCalonMahasiswa.getProgram() == null ? "Reguler" : biodataCalonMahasiswa.getProgram());

		Boolean ganjil = true;
		Integer semester = 1;
		List<String[]> data = new ArrayList<String[]>();

		JenisKegiatan jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_ULANG_MAHASISWA_BARU);

		Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
				ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, biodataCalonMahasiswa.getJenjang(),
				biodataCalonMahasiswa.getTahunAkademik(),
				biodataCalonMahasiswa.getSemesterMulai().equals(Perkuliahan.GANJIL),
				biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
				biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());

		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
		DendaPembayaran dendaPembayaran = (DendaPembayaran) serializables[1];
		DendaPembayaranNominal dendaPembayaranNominal = (DendaPembayaranNominal) serializables[2];

		if (jadwalPembayaran == null) {
			displayUtil
					.displayPembayaranTerlambat(logHostToHost,
							biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
									: biodataCalonMahasiswa.getNim(),
							pembayaranUtil.getBankHost(), nama, ConstantUtil.PAY);
			PaymentResponse paymentResponse = new PaymentResponse();
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return paymentResponse;
		}

		java.util.Collection<DetailBiaya> detailBiayas = pembayaranUtil
				.getDetailBiayaMahasiswaBaru(biodataCalonMahasiswa, jenisKegiatan);
		java.util.Collection<BillDetail> billDetails = CommonUtil.generateBillDetails(dendaPembayaran,
				dendaPembayaranNominal, detailBiayas);

		String pemb = "|";
		Long total = 0L;
		for (BillDetail biaya : billDetails) {
			total += Long.parseLong(biaya.getBillAmount());
			pemb += biaya.getBillCode() + "\\" + biaya.getBillName() + "\\" + biaya.getBillShortName() + "\\"
					+ biaya.getBillAmount() + "|";
		}

		PaymentResponse paymentResponse = CommonUtil.constructPaymentResponseCalonMahasiswa(biodataCalonMahasiswa,
				ganjil);

		Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(jenisKegiatan);
		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost,
					biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNim(),
					bankHost, nama, ConstantUtil.PAY);
			paymentResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if (kegiatan == null || kegiatan.getId() == null) {
			kegiatan = pembayaranUtil.simpanPembayaranCalonMahasiswa(bankHost, jadwalPembayaran, jenisKegiatan,
					biodataCalonMahasiswa, detailBiayas, nominalTagihan);
			if (kegiatan == null || kegiatan.getId() == null) {
				displayUtil.displayKesalahanSistem(logHostToHost,
						biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
								: biodataCalonMahasiswa.getNim(),
						bankHost, nama, ConstantUtil.PAY);
				paymentResponse.setStatus(ConstantUtilBankMandiri.PROVIDER_DATABASE_PROBLEM);
			} else {
				paymentResponse.setBillInfo6(kegiatan.getRefNumber());
				paymentResponse.setBillInfo7(kegiatan.getRefNumber());
				// menghubungkan log host to host dengan kegiatan
				logHostToHost.setKegiatan(kegiatan);
				paymentResponse
						.setStatus(total.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
								: ConstantUtilBankMandiri.SUCCESS_MANDIRI);
				data.add(new String[] { "response_code", total.equals(0L) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT
						: ConstantUtilBankMandiri.SUCCESS });
				data.add(new String[] { "response_description",
						total.equals(0L) ? "Pembayaran gagal dilakukan" : "Pembayaran Sukses Dilakukan" });
				data.add(new String[] { "no_registrasi",
						biodataCalonMahasiswa.getNoUjian() == null
								|| biodataCalonMahasiswa.getNoUjian().trim().equals("")
										? biodataCalonMahasiswa.getNoRegistrasi()
										: biodataCalonMahasiswa.getNoUjian() });
				data.add(new String[] { "kurs", "IDR" });
				data.add(new String[] { "nama", biodataCalonMahasiswa.getNama() });
				data.add(new String[] { "program", biodataCalonMahasiswa.getProgram() });

				String myJurusan = "";
				String myfakultas = "";
				if (biodataCalonMahasiswa.getProdi1() != null) {
					myJurusan += biodataCalonMahasiswa.getProdi1().getNama();
					myfakultas = biodataCalonMahasiswa.getProdi1().getFakultas().getNama();
				}
				if (biodataCalonMahasiswa.getProdi2() != null) {
					myJurusan += !myJurusan.equals("") ? " dan " + biodataCalonMahasiswa.getProdi2().getNama()
							: biodataCalonMahasiswa.getProdi2().getNama();
					myfakultas = biodataCalonMahasiswa.getProdi2().getFakultas().getNama();
				}
				data.add(new String[] { "fakultas", myfakultas });
				data.add(new String[] { "prodi", myJurusan });
				data.add(new String[] { "angkatan", biodataCalonMahasiswa.getTahun() + "" });

				data.add(new String[] { "semester", ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
				data.add(new String[] { "semester_ke", semester + "" });
				data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
				data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
				data.add(new String[] { "amount", pemb });
				data.add(new String[] { "total_amount", total + "" });
				data.add(new String[] { "kode_status_pembayaran",
						ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU });
				data.add(new String[] { "keterangan_status_pembayaran",
						ConstantUtilBankMandiri.PENDAFTARAN_ULANG_MAHASISWA_BARU });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
				logHostToHost.setResponseCode((ConstantUtilBankMandiri.SUCCESS));
				logHostToHost.setResponseDescription("Pembayaran Sukses Dilakukan");
			}
		} else {
			paymentResponse.setBillInfo6(kegiatan.getRefNumber());
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_SUDAH_DIBAYAR);
			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID });
			data.add(new String[] { "response_description",
					"Pembayaran gagal dilakukan, karena mahasiswa sudah melakukan pembayaran" });
			data.add(new String[] { "no_registrasi",
					biodataCalonMahasiswa.getNoUjian() == null || biodataCalonMahasiswa.getNoUjian().trim().equals("")
							? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNoUjian() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", biodataCalonMahasiswa.getNama() });
			data.add(new String[] { "program", biodataCalonMahasiswa.getProgram() });
			String myJurusan = "";
			String myfakultas = "";
			if (biodataCalonMahasiswa.getProdi1() != null) {
				myJurusan += biodataCalonMahasiswa.getProdi1().getNama();
				myfakultas = biodataCalonMahasiswa.getProdi1().getFakultas().getNama();
			}
			if (biodataCalonMahasiswa.getProdi2() != null) {
				myJurusan += !myJurusan.equals("") ? " dan " + biodataCalonMahasiswa.getProdi2().getNama()
						: biodataCalonMahasiswa.getProdi2().getNama();
				myfakultas = biodataCalonMahasiswa.getProdi2().getFakultas().getNama();
			}
			data.add(new String[] { "fakultas", myfakultas });
			data.add(new String[] { "prodi", myJurusan });
			data.add(new String[] { "angkatan", biodataCalonMahasiswa.getTahun() + "" });
			data.add(new String[] { "semester", ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
			data.add(new String[] { "semester_ke", semester + "" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran",
					ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_ULANG_MAHASISWA_BARU });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
			logHostToHost.setResponseCode((ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID));
			logHostToHost
					.setResponseDescription("Pembayaran gagal dilakukan, karena mahasiswa sudah melakukan pembayaran");
		}

		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(
				biodataCalonMahasiswa.getNoUjian() == null || biodataCalonMahasiswa.getNoUjian().trim().equals("")
						? biodataCalonMahasiswa.getNoRegistrasi()
						: biodataCalonMahasiswa.getNoUjian());
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(ConstantUtil.PAY);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);

		biodataCalonMahasiswa.setPembayaranDaftarUlang(kegiatan);
		Common.refreshUpdate(session, biodataCalonMahasiswa);

		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return paymentResponse;
	}

	public PaymentResponse pembayaranMahasiswaLama(String nim, String nama, LogHostToHost logHostToHost,
			String paymentAmount) {

		Double nominalTagihan = 0.0;
		try {
			nominalTagihan = Double.parseDouble(paymentAmount.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ws/billpayment/h2h/bankmandiri/logic/PaymentLogic.java:465");

		}

		Mahasiswa mahasiswa = ConstantValues.ambilByNim(nim);
		if (mahasiswa == null) {
			PaymentResponse paymentResponse = new PaymentResponse();
			paymentResponse.setBillInfo1(nim);
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNimNotFound(logHostToHost, nim, pembayaranUtil.getBankHost(), nama, ConstantUtil.PAY);
			return paymentResponse;
		}

		try {
			Session session = HibernateUtil.currentNativeSession();
			int countMahasiswaPindahan = ((Number) session.createCriteria(Mahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("alihProdiMahasiswa", mahasiswa)).setProjection(Projections.rowCount())
					.uniqueResult()).intValue();
			HibernateUtil.closeSession();

			if (countMahasiswaPindahan > 0) {

				PaymentResponse paymentResponse = new PaymentResponse();
				paymentResponse.setBillInfo1(nim);
				paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_DIBLOKIR);
				displayUtil.displayAlihProdi(logHostToHost, nim, pembayaranUtil.getBankHost(), nama, ConstantUtil.PAY);
				return paymentResponse;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ws/billpayment/h2h/bankmandiri/logic/PaymentLogic.java:493");

		}

		mahasiswa.setProgram(mahasiswa.getProgram() == null ? "Reguler" : mahasiswa.getProgram());

		JenisKegiatan jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA);

		Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaHanyaBerdasarJenisKegiatan(
				ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, mahasiswa.getJenjang(), null,
				mahasiswa.getJenisSeleksi(), mahasiswa.getProgram(), mahasiswa.getNim());

		JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];
		DendaPembayaran dendaPembayaran = (DendaPembayaran) serializables[1];
		DendaPembayaranNominal dendaPembayaranNominal = (DendaPembayaranNominal) serializables[2];

		if (jadwalPembayaran == null) {
			displayUtil.displayPembayaranTerlambat(logHostToHost, mahasiswa.getNim(), pembayaranUtil.getBankHost(),
					nama, ConstantUtil.PAY);
			PaymentResponse paymentResponse = new PaymentResponse();
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return paymentResponse;
		}

		Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil()
				: jadwalPembayaran.getGanjil();
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jadwalPembayaran.getTahunAkademik(),
				ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
				mahasiswa.getSemesterMulai());

		List<String[]> data = new ArrayList<String[]>();

		JenisKegiatan kegiatanDaftarUlangMahasiswaBaru = pembayaranUtil
				.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		List<TunggakanMahasiswa> tunggakanMahasiswas = pembayaranUtil.getTunggakanMahasiswa(
				new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru, jenisKegiatan }, mahasiswa, semester, null);

		if (tunggakanMahasiswas.size() != 0) {

			displayUtil.displayTunggakanMahasiswa(logHostToHost, mahasiswa.getNim(), pembayaranUtil.getBankHost(), nama,
					ConstantUtil.INQUERY, tunggakanMahasiswas);

			PaymentResponse paymentResponse = new PaymentResponse();
			paymentResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
			return paymentResponse;
		}

		@SuppressWarnings("unchecked")
		Collection<DetailBiaya> detailBiayas = pembayaranUtil.getDetailBiayaMahasiswa(mahasiswa, jadwalPembayaran, null,
				false);
		String pemb = "|";
		Long total = 0L;

		Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);
		for (DetailBiaya biaya : detailBiayas) {
			ItemBiaya itemBiaya = biaya.getItemBiaya();
			Double nilai = biaya.hitungTotalKegiatan(kegiatan);
			pemb += itemBiaya.getId() + "\\" + itemBiaya.getNama().trim() + "\\" + itemBiaya.getDeskripsi().trim()
					+ "\\" + (nilai).longValue() + "|";
			total += (nilai).longValue();
		}

		if (kegiatan != null) {
			kegiatan.setJumlahTelahDibayar(kegiatan.getJumlahTelahDibayar() + nominalTagihan);
		}

		PaymentResponse paymentResponse = CommonUtil.constructPaymentResponseMahasiswaLama(mahasiswa, ganjil, semester,
				kegiatan);

		boolean tidakBolehMencicil = Common.bolehKonfigurasi("mahasiswa_tidak_boleh_mencicil_pembayaran_via_h2h");

		// Double telahDibayar = kegiatan == null ? 0.0 : kegiatan
		// .getJumlahTelahDibayar();
		// Double sisaCicilan = total - (telahDibayar + nominalTagihan);
		Double sisaCicilan = 100000.0; // Hanya temporary

		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost, mahasiswa.getNim(), bankHost, nama, ConstantUtil.PAY);
			paymentResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if ((kegiatan == null || kegiatan.getId() == null || kegiatan.getAmount() < 0.01 || !tidakBolehMencicil)
				&& sisaCicilan > 0.001) {
			kegiatan = pembayaranUtil.simpanPembayaranMahasiswa(bankHost, jadwalPembayaran, jenisKegiatan, mahasiswa,
					detailBiayas, nominalTagihan.doubleValue(), null, nim);
			if (kegiatan == null || kegiatan.getId() == null) {
				displayUtil.displayKesalahanSistem(logHostToHost, mahasiswa.getNim(), bankHost, nama, ConstantUtil.PAY);
				paymentResponse.setStatus(ConstantUtilBankMandiri.PROVIDER_DATABASE_PROBLEM);
			} else {
				paymentResponse.setBillInfo6(kegiatan.getRefNumber());
				paymentResponse.setBillInfo7(kegiatan.getRefNumber());
				// menghubungkan log host to host dengan kegiatan
				logHostToHost.setKegiatan(kegiatan);
				paymentResponse.setStatus(
						nominalTagihan.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
								: ConstantUtilBankMandiri.SUCCESS_MANDIRI);
				data.add(new String[] { "response_code",
						nominalTagihan.equals(0.0) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT
								: ConstantUtilBankMandiri.SUCCESS });
				data.add(new String[] { "response_description",
						nominalTagihan.equals(0.0) ? "Pembayaran gagal dilakukan" : "Pembayaran Sukses Dilakukan" });
				data.add(new String[] { "nim", mahasiswa.getNim() });
				data.add(new String[] { "kurs", "IDR" });
				data.add(new String[] { "nama", mahasiswa.getNama() });
				data.add(new String[] { "program", mahasiswa.getProgram() });
				data.add(new String[] { "fakultas", mahasiswa.getJurusan().getFakultas().getNama() });
				data.add(new String[] { "prodi", mahasiswa.getJurusan().getNama() });
				data.add(new String[] { "angkatan", mahasiswa.getTahunangkatan() + "" });
				data.add(new String[] { "semester", ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
				data.add(new String[] { "semester_ke", semester + "" });
				data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
				data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
				data.add(new String[] { "amount", pemb });
				data.add(new String[] { "total_amount", total + "" });
				data.add(new String[] { "kode_status_pembayaran",
						ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG });
				data.add(new String[] { "keterangan_status_pembayaran",
						ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
				logHostToHost.setResponseCode((ConstantUtilBankMandiri.SUCCESS));
				logHostToHost.setResponseDescription("Pembayaran Sukses Dilakukan");
			}
		} else {
			paymentResponse.setBillInfo6(kegiatan.getRefNumber());
			paymentResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_SUDAH_DIBAYAR);
			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID });
			data.add(new String[] { "response_description",
					"Pembayaran gagal dilakukan, karena mahasiswa sudah melakukan pembayaran" });
			data.add(new String[] { "nim", mahasiswa.getNim() });
			data.add(new String[] { "kurs", "IDR" });
			data.add(new String[] { "nama", mahasiswa.getNama() });
			data.add(new String[] { "program", mahasiswa.getProgram() });
			data.add(new String[] { "fakultas", mahasiswa.getJurusan().getFakultas().getNama() });
			data.add(new String[] { "prodi", mahasiswa.getJurusan().getNama() });
			data.add(new String[] { "angkatan", mahasiswa.getTahunangkatan() + "" });
			data.add(new String[] { "semester", ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP });
			data.add(new String[] { "semester_ke", semester + "" });
			data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
			data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
			data.add(new String[] { "amount", pemb });
			data.add(new String[] { "total_amount", total + "" });
			data.add(new String[] { "kode_status_pembayaran", ConstantUtilBankMandiri.PEMBAYARAN_PENDAFTARAN_ULANG });
			data.add(new String[] { "keterangan_status_pembayaran",
					ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA });
			data.add(new String[] { "reference_number",
					(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
			logHostToHost.setResponseCode((ConstantUtilBankMandiri.BILLS_HAVE_BEEN_PAID));
			logHostToHost
					.setResponseDescription("Pembayaran gagal dilakukan, karena mahasiswa sudah melakukan pembayaran");
		}

		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(mahasiswa.getNim());
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(ConstantUtil.PAY);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return paymentResponse;
	}
}
