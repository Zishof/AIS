package ws.billpayment.h2h.bankmandiri.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;

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
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.Perkuliahan;
import ws.billpayment.h2h.bankmandiri.BillDetail;
import ws.billpayment.h2h.bankmandiri.ReversalResponse;
import ws.billpayment.h2h.bankmandiri.util.CommonUtil;
import ws.billpayment.h2h.bankmandiri.util.ConstantUtilBankMandiri;
import ws.billpayment.h2h.bankmandiri.util.DisplayUtil;

public class ReversalLogic {

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	public DisplayUtil displayUtil = new DisplayUtil();

	public ReversalResponse reversalCalonMahasiswaBaru(String noregistrasi, String nama, LogHostToHost logHostToHost) {
		BiodataCalonMahasiswa biodataCalonMahasiswa = pembayaranUtil.getCalonMahasiswaByNoPendaftaran(noregistrasi);
		if (biodataCalonMahasiswa == null) {
			ReversalResponse reversalResponse = new ReversalResponse();
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNimNotFound(logHostToHost, noregistrasi, pembayaranUtil.getBankHost(), nama,
					ConstantUtil.REVERSAL);
			return reversalResponse;
		}
		biodataCalonMahasiswa.setProgram(
				biodataCalonMahasiswa.getProgram() == null ? "Reguler" : biodataCalonMahasiswa.getProgram());

		Boolean ganjil = true;
		Integer semester = 1;

		JenisKegiatan jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_CALON_MAHASISWA);

		Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);

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
			displayUtil.displayPembayaranTerlambat(logHostToHost,
					biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNim(),
					pembayaranUtil.getBankHost(), nama, ConstantUtil.REVERSAL);
			ReversalResponse reversalResponse = new ReversalResponse();
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return reversalResponse;
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

		ReversalResponse reversalResponse = new ReversalResponse();
		List<String[]> data = new ArrayList<String[]>();
		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost,
					biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNim(),
					bankHost, nama, ConstantUtil.REVERSAL);
			reversalResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if (kegiatan == null || kegiatan.getId() == null) {
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);

			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_NOT_FOUND });
			data.add(new String[] { "response_description",
					"Reversal gagal dilakukan, karena tagihan tidak ditemukan" });
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
			logHostToHost.setResponseDescription("Reversal gagal dilakukan, karena tagihan tidak ditemukan");
		} else {
			boolean b = pembayaranUtil.dropKegiatan(kegiatan, null, null);
			if (!b) {
				displayUtil.displayKesalahanSistem(logHostToHost,
						biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
								: biodataCalonMahasiswa.getNim(),
						bankHost, nama, ConstantUtil.PAY);
				reversalResponse.setStatus(ConstantUtilBankMandiri.PROVIDER_DATABASE_PROBLEM);
			} else {
				reversalResponse
						.setStatus(total.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
								: ConstantUtilBankMandiri.SUCCESS_MANDIRI);

				data.add(new String[] { "response_code", total.equals(0L) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT
						: ConstantUtilBankMandiri.SUCCESS });
				data.add(new String[] { "response_description", "Reversal sukses dilakukan" });
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
				logHostToHost.setResponseCode((ConstantUtilBankMandiri.SUCCESS));
				logHostToHost.setResponseDescription("Reversal sukses dilakukan");
			}

		}

		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
				: biodataCalonMahasiswa.getNim());
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(ConstantUtil.REVERSAL);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return reversalResponse;
	}

	public ReversalResponse reversalMahasiswaBaru(String noUjian, String nama, LogHostToHost logHostToHost) {
		BiodataCalonMahasiswa biodataCalonMahasiswa = pembayaranUtil.getCalonMahasiswaByNoPendaftaran(noUjian);
		if (biodataCalonMahasiswa == null) {
			ReversalResponse reversalResponse = new ReversalResponse();
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNimNotFound(logHostToHost, noUjian, pembayaranUtil.getBankHost(), nama,
					ConstantUtil.REVERSAL);
			return reversalResponse;
		}
		biodataCalonMahasiswa.setProgram(
				biodataCalonMahasiswa.getProgram() == null ? "Reguler" : biodataCalonMahasiswa.getProgram());

		Boolean ganjil = true;
		Integer semester = 1;

		JenisKegiatan jenisKegiatan = pembayaranUtil
				.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_ULANG_MAHASISWA_BARU);
		Kegiatan kegiatan = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);

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
			displayUtil.displayPembayaranTerlambat(logHostToHost,
					biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNim(),
					pembayaranUtil.getBankHost(), nama, ConstantUtil.REVERSAL);
			ReversalResponse reversalResponse = new ReversalResponse();
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return reversalResponse;
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

		ReversalResponse reversalResponse = new ReversalResponse();
		List<String[]> data = new ArrayList<String[]>();
		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost,
					biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNim(),
					bankHost, nama, ConstantUtil.REVERSAL);
			reversalResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if (kegiatan == null || kegiatan.getId() == null) {
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);

			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_NOT_FOUND });
			data.add(new String[] { "response_description",
					"Reversal gagal dilakukan, karena tagihan tidak ditemukan" });
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
			logHostToHost.setResponseDescription("Reversal gagal dilakukan, karena tagihan tidak ditemukan");
		} else {
			boolean b = pembayaranUtil.dropKegiatan(kegiatan, null, null);
			if (!b) {
				displayUtil.displayKesalahanSistem(logHostToHost,
						biodataCalonMahasiswa.getNim() == null ? biodataCalonMahasiswa.getNoRegistrasi()
								: biodataCalonMahasiswa.getNim(),
						bankHost, nama, ConstantUtil.PAY);
				reversalResponse.setStatus(ConstantUtilBankMandiri.PROVIDER_DATABASE_PROBLEM);
			} else {
				reversalResponse
						.setStatus(total.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
								: ConstantUtilBankMandiri.SUCCESS_MANDIRI);

				data.add(new String[] { "response_code", total.equals(0L) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT
						: ConstantUtilBankMandiri.SUCCESS });
				data.add(new String[] { "response_description", "Reversal sukses dilakukan" });
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
				logHostToHost.setResponseDescription("Reversal sukses dilakukan");
			}

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
		logHostToHost.setTransactionType(ConstantUtil.REVERSAL);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return reversalResponse;
	}

	public ReversalResponse reversalMahasiswaLama(String nim, String nama, LogHostToHost logHostToHost) {
		Mahasiswa mahasiswa = ConstantValues.ambilByNim(nim);
		if (mahasiswa == null) {
			ReversalResponse reversalResponse = new ReversalResponse();
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			displayUtil.displayNimNotFound(logHostToHost, nim, pembayaranUtil.getBankHost(), nama,
					ConstantUtil.REVERSAL);
			return reversalResponse;
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
					nama, ConstantUtil.REVERSAL);
			ReversalResponse reversalResponse = new ReversalResponse();
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);
			return reversalResponse;
		}

		Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil()
				: jadwalPembayaran.getGanjil();
		Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), jadwalPembayaran.getTahunAkademik(),
				ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
				mahasiswa.getSemesterMulai());

		Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);

		@SuppressWarnings("unchecked")
		Collection<DetailBiaya> detailBiayas = pembayaranUtil.getDetailBiayaMahasiswa(mahasiswa, jadwalPembayaran, null,
				false);
		String pemb = "|";
		Long total = 0L;

		for (DetailBiaya biaya : detailBiayas) {
			ItemBiaya itemBiaya = biaya.getItemBiaya();
			Double nilai = biaya.hitungTotalKegiatan(kegiatan);
			pemb += itemBiaya.getId() + "\\" + itemBiaya.getNama().trim() + "\\" + itemBiaya.getDeskripsi().trim()
					+ "\\" + (nilai).longValue() + "|";
			total += (nilai).longValue();
		}

		ReversalResponse reversalResponse = new ReversalResponse();
		List<String[]> data = new ArrayList<String[]>();
		BankHost bankHost = pembayaranUtil.getBankHost();
		if (bankHost == null) {
			displayUtil.displayIpNotAllowed(logHostToHost, mahasiswa.getNim(), bankHost, nama, ConstantUtil.REVERSAL);
			reversalResponse.setStatus(ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI);
		} else if (kegiatan == null || kegiatan.getId() == null) {
			reversalResponse.setStatus(ConstantUtilBankMandiri.TAGIHAN_TIDAK_DITEMUKAN);

			data.add(new String[] { "response_code", ConstantUtilBankMandiri.BILLS_NOT_FOUND });
			data.add(new String[] { "response_description",
					"Reversal gagal dilakukan, karena tagihan tidak ditemukan" });
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
			logHostToHost.setResponseDescription("Reversal gagal dilakukan, karena tagihan tidak ditemukan");
		} else {
			boolean b = pembayaranUtil.dropKegiatan(kegiatan, null, null);
			if (!b) {
				displayUtil.displayKesalahanSistem(logHostToHost, mahasiswa.getNim(), bankHost, nama, ConstantUtil.PAY);
				reversalResponse.setStatus(ConstantUtilBankMandiri.PROVIDER_DATABASE_PROBLEM);
			} else {
				reversalResponse
						.setStatus(total.equals(0L) ? ConstantUtilBankMandiri.SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI
								: ConstantUtilBankMandiri.SUCCESS_MANDIRI);

				data.add(new String[] { "response_code", total.equals(0L) ? ConstantUtilBankMandiri.NOT_VALID_AMOUNT
						: ConstantUtilBankMandiri.SUCCESS });
				data.add(new String[] { "response_description", "Reversal sukses dilakukan" });
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
				logHostToHost.setResponseDescription("Reversal sukses dilakukan");
			}

		}

		logHostToHost.setBankHost(bankHost);
		logHostToHost.setIp(bankHost.getIp());
		logHostToHost.setNama(nama);
		logHostToHost.setNim(mahasiswa.getNim());
		logHostToHost.setKeterangan(CommonUtil.convertToString(data));
		CommonUtil.setRequestAndresponse(logHostToHost);
		logHostToHost.setTransactionType(ConstantUtil.REVERSAL);

		Session session = HibernateUtil.currentNativeSession();
		session.getTransaction().begin();
		session.save(logHostToHost);
		session.getTransaction().commit();

		HibernateUtil.closeSession();

		return reversalResponse;
	}

}
