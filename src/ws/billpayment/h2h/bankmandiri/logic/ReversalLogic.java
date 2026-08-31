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

/**
 * Implementasi transaksi REVERSAL pada layanan web service Host-to-Host (H2H) Bank Mandiri —
 * dipanggil ketika bank membatalkan/menarik-kembali pembayaran yang sebelumnya berhasil dicatat
 * lewat jalur PAY H2H yang sama (mis. karena kesalahan nasabah di teller/ATM, timeout yang
 * di-retry pihak bank, atau rekonsiliasi harian bank yang menemukan transaksi ganda). Kelas ini
 * adalah padanan Bank Mandiri dari {@code ais.action.ws.logic.ReversalLogic} (jalur H2H generik
 * lain di paket {@code ais.action.ws}) — polanya identik: temukan {@link Kegiatan} yang tercatat
 * lunas untuk tagihan bersangkutan, panggil {@code PembayaranUtil#dropKegiatan} untuk
 * mengembalikannya ke status belum-lunas, lalu catat hasilnya sebagai baris audit
 * {@link LogHostToHost}.
 *
 * <h2>Tiga kategori pembayar</h2>
 * <p>
 * Bank Mandiri H2H melayani tiga kategori tagihan yang masing-masing punya method reversal
 * sendiri karena skema pencarian entitas dan perhitungan jadwal pembayarannya berbeda:
 * </p>
 * <ul>
 * <li>{@link #reversalCalonMahasiswaBaru} — tagihan pendaftaran CALON mahasiswa baru (PMB),
 * dicari lewat nomor registrasi pendaftaran.</li>
 * <li>{@link #reversalMahasiswaBaru} — tagihan pendaftaran ULANG mahasiswa baru (setelah
 * dinyatakan lulus seleksi, sebelum resmi jadi mahasiswa aktif), dicari lewat nomor ujian
 * (fallback ke nomor registrasi bila nomor ujian kosong).</li>
 * <li>{@link #reversalMahasiswaLama} — tagihan her-registrasi/SPP mahasiswa yang SUDAH aktif
 * (sudah punya NIM), dicari langsung lewat NIM.</li>
 * </ul>
 * <p>
 * Ketiga method berbagi struktur yang sama persis (duplikasi kode yang disengaja, mengikuti gaya
 * kelas {@code Logic} H2H lain di modul ini): (1) cari entitas pembayar, kembalikan
 * {@code TAGIHAN_TIDAK_DITEMUKAN} bila tidak ada; (2) tentukan {@link JenisKegiatan} dan jadwal
 * pembayaran/denda yang berlaku; (3) kembalikan {@code TAGIHAN_TIDAK_DITEMUKAN} bila di luar
 * jadwal pembayaran manapun; (4) hitung rincian tagihan ({@link BillDetail}) untuk disertakan di
 * log; (5) cari {@link Kegiatan} yang tercatat lunas — bila tidak ada, tandai
 * {@code BILLS_NOT_FOUND}/{@code TAGIHAN_TIDAK_DITEMUKAN} TANPA memanggil {@code dropKegiatan}
 * (tidak ada yang perlu dibatalkan); (6) bila ada, panggil {@code pembayaranUtil.dropKegiatan}
 * untuk membatalkannya — gagal jadi {@code PROVIDER_DATABASE_PROBLEM}, berhasil jadi
 * {@code SUCCESS_MANDIRI} (atau {@code SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI} bila total tagihan
 * kebetulan nol); (7) simpan {@code logHostToHost} sebagai baris audit dalam transaksi Hibernate
 * native tersendiri, lengkap dengan rincian tagihan yang diserialisasi ke {@code keterangan} lewat
 * {@code CommonUtil.convertToString(data)}.
 * </p>
 *
 * <p>
 * <b>Catatan perilaku (bukan diperbaiki di sini):</b> pada {@link #reversalCalonMahasiswaBaru} dan
 * {@link #reversalMahasiswaBaru}, cabang {@code bankHost == null} (IP pemanggil tidak dikenali)
 * TIDAK melakukan {@code return} lebih awal — eksekusi tetap jatuh ke blok penyimpanan log di
 * akhir method yang memanggil {@code bankHost.getIp()} tanpa null-check, sehingga permintaan dari
 * IP yang tidak terdaftar akan melempar {@link NullPointerException} alih-alih mengembalikan
 * respons {@code SYSTEM_TIDAK_BISA_MELAYANI_TRANSAKSI} yang sudah di-set ke
 * {@code reversalResponse} sesaat sebelumnya. {@link #reversalMahasiswaLama} memiliki cabang yang
 * sama tetapi TIDAK memanggil {@code logHostToHost.setBankHost}/{@code setIp} di jalur manapun
 * setelah percabangan itu dengan cara yang berbeda — periksa langsung isi method bila menelusuri
 * perilaku persisnya lebih lanjut.
 * </p>
 */
public class ReversalLogic {

	/** Singleton berisi query/mutasi umum seputar tagihan &amp; kegiatan pembayaran, dibagi lintas seluruh logic H2H Bank Mandiri. */
	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	/** Kumpulan helper pencatat {@link LogHostToHost} untuk skenario gagal umum (NIM tidak ditemukan, IP tidak diizinkan, tagihan lewat jadwal, kesalahan sistem). */
	public DisplayUtil displayUtil = new DisplayUtil();

	/**
	 * Reversal tagihan pendaftaran CALON mahasiswa baru. Entitas dicari lewat
	 * {@code pembayaranUtil.getCalonMahasiswaByNoPendaftaran(noregistrasi)}; jadwal pembayaran
	 * diambil untuk {@link ais.database.model.Perkuliahan#GANJIL semester ganjil ke-1} secara
	 * tetap (pendaftaran calon mahasiswa baru selalu dianggap semester pertama). Lihat Javadoc
	 * kelas untuk alur lengkap tujuh langkah yang diikuti method ini.
	 *
	 * @param noregistrasi   nomor registrasi pendaftaran calon mahasiswa
	 * @param nama           nama pemohon reversal (dari payload H2H bank, untuk pencatatan log)
	 * @param logHostToHost  baris audit yang akan diisi lalu disimpan sebelum method kembali
	 * @return status reversal ({@code SUCCESS_MANDIRI}, {@code TAGIHAN_TIDAK_DITEMUKAN},
	 *         {@code PROVIDER_DATABASE_PROBLEM}, dsb. — lihat {@link ConstantUtilBankMandiri})
	 */
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

	/**
	 * Reversal tagihan pendaftaran ULANG mahasiswa baru (pasca-lulus seleksi, sebelum resmi
	 * menjadi mahasiswa aktif). Entitas dicari lewat
	 * {@code pembayaranUtil.getCalonMahasiswaByNoPendaftaran(noUjian)} — parameter bernama
	 * {@code noUjian} tetapi query pencariannya SAMA dengan
	 * {@link #reversalCalonMahasiswaBaru} (lewat nomor pendaftaran), BUKAN pencarian khusus nomor
	 * ujian. Lihat Javadoc kelas untuk alur lengkap tujuh langkah.
	 *
	 * @param noUjian        nomor ujian/nomor pendaftaran calon mahasiswa yang bersangkutan
	 * @param nama           nama pemohon reversal, untuk pencatatan log
	 * @param logHostToHost  baris audit yang akan diisi lalu disimpan sebelum method kembali
	 * @return status reversal — lihat {@link ConstantUtilBankMandiri}
	 */
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

	/**
	 * Reversal tagihan her-registrasi/SPP mahasiswa yang SUDAH aktif (punya NIM). Berbeda dari
	 * dua method saudaranya: entitas dicari langsung lewat {@link ConstantValues#ambilByNim(String)},
	 * dan jadwal pembayaran dihitung berdasarkan semester berjalan mahasiswa (lewat
	 * {@code Common.getSemester(...)}, mempertimbangkan {@code tahunangkatan},
	 * {@code pindahKeKampusIniMasukSemester}, dan {@code semesterMulai}) alih-alih semester
	 * ganjil-pertama tetap. Rincian tagihan dihitung dari {@link DetailBiaya#hitungTotalKegiatan}
	 * per baris {@code detailBiayas}, bukan lewat {@code CommonUtil.generateBillDetails*} seperti
	 * dua method lain. Lihat Javadoc kelas untuk alur tujuh langkah secara umum.
	 *
	 * @param nim            NIM mahasiswa
	 * @param nama           nama pemohon reversal, untuk pencatatan log
	 * @param logHostToHost  baris audit yang akan diisi lalu disimpan sebelum method kembali
	 * @return status reversal — lihat {@link ConstantUtilBankMandiri}
	 */
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
