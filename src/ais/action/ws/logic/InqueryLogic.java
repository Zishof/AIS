package ais.action.ws.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import ais.action.ws.util.CommonUtil;
import ais.action.ws.util.ConstantUtil;
import ais.action.ws.util.DisplayUtil;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BankHost;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.DetailKegiatan;
import ais.database.model.ItemBiaya;
import ais.database.model.JadwalPembayaran;
import ais.database.model.JenisKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.database.model.TunggakanMahasiswa;
import ais.ui.util.WaktuUtil;

public class InqueryLogic {
	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	public DisplayUtil displayUtil = new DisplayUtil();

	public String[][] inqueryCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa, BankHost bankHost, String nama,
			LogHostToHost logHostToHost) {

		System.out.println(" ============== Inquery calon mahasiswa ==================");
		List<String[]> data = new ArrayList<String[]>();
		try {

			JenisKegiatan jenisKegiatan = pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_CALON_MAHASISWA);

			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranRegistrasi();
			if (kegiatan == null) {
				kegiatan = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);
			}
			JadwalPembayaran jadwalPembayaran;
			if (kegiatan != null && kegiatan.getJadwalPembayaran() != null) {
				jadwalPembayaran = kegiatan.getJadwalPembayaran();
			} else {

				Date tanggal = WaktuUtil.getDate();
				Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
						tanggal, jenisKegiatan, biodataCalonMahasiswa.getJenjang(),
						biodataCalonMahasiswa.getTahunAkademik(),
						biodataCalonMahasiswa.getSemesterMulai().equalsIgnoreCase(Perkuliahan.GANJIL),
						biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
						biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());

				jadwalPembayaran = (JadwalPembayaran) serializables[0];
				if (jadwalPembayaran == null) {

					return displayUtil.displayPembayaranTerlambat(logHostToHost,
							biodataCalonMahasiswa.getNoRegistrasi(), bankHost, nama, ConstantUtil.INQUERY)
							.toArray(new String[][] { null });

				}
			}

			Jurusan myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
					: biodataCalonMahasiswa.getProdi1();
			java.util.Collection<DetailBiaya> detailBiayas = pembayaranUtil
					.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, false);

			String pemb = "|";
			Long total = 0L;

			for (DetailBiaya biaya : detailBiayas) {
				ItemBiaya itemBiaya = biaya.getItemBiaya();
//				Double nilai = biaya.hitungTotalKegiatan(kegiatan);
//				Double nilai = (biaya.getNilaiBiayaBaru() == null ? biaya.getNilaiBiaya() : biaya.getNilaiBiayaBaru());

				DetailKegiatan tempdata =kegiatan==null?null: null;
				if (kegiatan != null && kegiatan.getId() != null && tempdata == null) {

					tempdata = kegiatan.ambilSatuDetailKegiatan(biaya);

					if (tempdata == null) {
						tempdata = new DetailKegiatan();
						tempdata.setUraian("");
						tempdata.setDetailBiaya(biaya);

						tempdata.setKeterangan(biaya == null ? "" : biaya.getKeterangan());
						tempdata.setKegiatan(kegiatan);
						try {
							Session session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							session.save(tempdata);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}
						}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/logic/InqueryLogic.java:109");
							// TODO: handle exception
						}
						HibernateUtil.closeSession();
					}
				}

				DetailKegiatan detailKegiatan = tempdata;
				Double nilai = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, biaya, false);

				if (myjurusan1 != null) {
					pemb += itemBiaya.getKode().trim()
							+ (biaya.getJurusan() == null ? "" : "-" + biaya.getJurusan().getKode()) + "\\"
							+ itemBiaya.getNama().trim()
							+ (biaya.getJurusan() == null ? "" : "-" + biaya.getJurusan().getNama()) + "\\"
							+ itemBiaya.getDeskripsi().trim()
							+ (biaya.getJurusan() == null ? "" : "-" + biaya.getJurusan().getNama()) + "\\"
							+ (nilai).longValue() + "|";
				} else {
					pemb += itemBiaya.getKode().trim() + "\\" + itemBiaya.getNama().trim() + "\\"
							+ itemBiaya.getDeskripsi().trim() + "\\" + (nilai).longValue() + "|";
				}
				total += (nilai).longValue();
			}

			if (kegiatan != null && kegiatan.getAmountTerhutang() != null && kegiatan.getAmountTerhutang() > 0.1
					&& kegiatan.getAmountTerhutang() < total) {
				pemb += "00\\Diskon\\Potongan\\" + (kegiatan.getAmountTerhutang().longValue() - total) + "|";
				total = kegiatan.getAmountTerhutang().longValue();
			}

			boolean tidakBolehMencicil = Common.bolehKonfigurasi("calon_mahasiswa_tidak_boleh_mencicil_pembayaran_no_reg_via_h2h");

			// Double telahDibayar = kegiatan == null ? 0.0 : kegiatan
			// .getJumlahTelahDibayar();
			// Double sisaCicilan = total - telahDibayar;
			Double sisaCicilan = 100000.0;// hanya temporary

			if ((kegiatan == null || kegiatan.getId() == null || kegiatan.getAmount() < 0.01 || !tidakBolehMencicil)
					&& sisaCicilan > 0.001) {
				data.add(new String[] { "response_code",
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS) });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai" : "Inquiry Sukses Dilakukan" });
				data.add(new String[] { "no_registrasi", biodataCalonMahasiswa.getNoRegistrasi() });
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

				data.add(new String[] { "semester", Perkuliahan.GANJIL });
				data.add(new String[] { "semester_ke", "0" });
				data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
				data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
				data.add(new String[] { "amount", pemb });
				data.add(new String[] { "total_amount", total + "" });
				data.add(
						new String[] { "kode_status_pembayaran", ConstantUtil.PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA });
				data.add(new String[] { "keterangan_status_pembayaran", ConstantUtil.PENDAFTARAN_CALON_MAHASISWA });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

				data.add(new String[] { "jumlah_yang_telah_dibayar",
						kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

				logHostToHost.setResponseCode(
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS));
				logHostToHost.setResponseDescription(
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai" : "Inquiry Sukses Dilakukan");
			} else {
				data.add(new String[] { "response_code", ConstantUtil.BILLS_HAVE_BEEN_PAID });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai"
								: "Inquiry gagal dilakukan, karena mahasiswa sudah melakukan pembayaran" });
				data.add(new String[] { "no_registrasi", biodataCalonMahasiswa.getNoRegistrasi() });
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

				data.add(new String[] { "semester", Perkuliahan.GANJIL });
				data.add(new String[] { "semester_ke", "0" });
				data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
				data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
				data.add(new String[] { "amount", pemb });
				data.add(new String[] { "total_amount", total + "" });
				data.add(
						new String[] { "kode_status_pembayaran", ConstantUtil.PEMBAYARAN_PENDAFTARAN_CALON_MAHASISWA });
				data.add(new String[] { "keterangan_status_pembayaran", ConstantUtil.PENDAFTARAN_CALON_MAHASISWA });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

				data.add(new String[] { "jumlah_yang_telah_dibayar",
						kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

				logHostToHost.setResponseCode((ConstantUtil.BILLS_HAVE_BEEN_PAID));
				logHostToHost.setResponseDescription(
						"Inquiry gagal dilakukan, karena calon mahasiswa sudah melakukan pembayaran");
			}

			logHostToHost.setBankHost(bankHost);
			logHostToHost.setIp(bankHost.getIp());
			logHostToHost.setNama(nama);
			logHostToHost.setNim(biodataCalonMahasiswa.getNoRegistrasi());
			logHostToHost.setKeterangan(CommonUtil.convertToString(data));
			logHostToHost.setTransactionType(ConstantUtil.INQUERY);
			CommonUtil.setRequestAndresponse(logHostToHost);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(logHostToHost);
			session.getTransaction().commit();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return data.toArray(new String[][] { null });
	}

	public String[][] inqueryMahasiswaBaru(BiodataCalonMahasiswa biodataCalonMahasiswa, BankHost bankHost, String nama,
			LogHostToHost logHostToHost) {

		System.out.println(" ============== Inquery calon mahasiswa pembayaran mahasiswa baru ==================");
		List<String[]> data = new ArrayList<String[]>();
		try {

			JenisKegiatan jenisKegiatan = pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
			int smt = 1;
			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranDaftarUlang();
			if (kegiatan == null) {
				// H2H tidak membawa parameter semester. Dahulukan kegiatan/tagihan yang
				// benar-benar sudah dibuat dari layar pembayaran (bisa semester 0).
				kegiatan = biodataCalonMahasiswa.ambilKegiatans(null, jenisKegiatan);
			}
			if (kegiatan != null) {
				smt = kegiatan.getSemster();
			} else {
				kegiatan = biodataCalonMahasiswa.ambilKegiatans(smt, jenisKegiatan);
			}
			JadwalPembayaran jadwalPembayaran;
			if (kegiatan != null && kegiatan.getJadwalPembayaran() != null) {
				jadwalPembayaran = kegiatan.getJadwalPembayaran();
			} else {

				Date tanggal = WaktuUtil.getDate();
				Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaBerdasarkanTahunAkademik(
						tanggal, jenisKegiatan, biodataCalonMahasiswa.getJenjang(),
						biodataCalonMahasiswa.getTahunAkademik(),
						biodataCalonMahasiswa.getSemesterMulai().equalsIgnoreCase(Perkuliahan.GANJIL),
						biodataCalonMahasiswa.getJenisSeleksi(), biodataCalonMahasiswa.getProgram(),
						biodataCalonMahasiswa.getNoRegistrasi(), biodataCalonMahasiswa.getGelombangPendaftaran());

				jadwalPembayaran = (JadwalPembayaran) serializables[0];
				if (jadwalPembayaran == null) {

					return displayUtil.displayPembayaranTerlambat(logHostToHost,
							biodataCalonMahasiswa.getNoRegistrasi(), bankHost, nama, ConstantUtil.INQUERY)
							.toArray(new String[][] { null });

				}
			}

			Jurusan myjurusan1 = biodataCalonMahasiswa.getProdiLulus();
			List<DetailBiaya> detailBiayas = new ArrayList<DetailBiaya>();
			if (myjurusan1 == null || myjurusan1.getId() == null) {
				myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
						: biodataCalonMahasiswa.getProdi1();
				java.util.Collection<DetailBiaya> detailBiayas1 = pembayaranUtil
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, smt, true);

				detailBiayas.addAll(detailBiayas1);
			} else {
				java.util.Collection<DetailBiaya> detailBiayas1 = pembayaranUtil
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, smt, true);
				detailBiayas.addAll(detailBiayas1);
			}

			// Tagihan yang sudah terbentuk adalah sumber paling akurat untuk inquiry.
			// Khusus RPL, semester kegiatan dapat berbeda dari semester template biaya;
			// akibatnya pencarian template di atas kosong walaupun billing menampilkan
			// tagihan. Pulihkan item dari DetailKegiatan aktual agar total H2H sama
			// dengan layar pembayaran.
			if (detailBiayas.isEmpty()) {
				detailBiayas.addAll(pembayaranUtil.getDetailBiayaDariKegiatan(kegiatan));
			}

			// Konfigurasi lama sering menaruh tagihan daftar ulang pada semester 0,
			// sedangkan kode H2H lama selalu meminta semester 1. Jika pilihan utama
			// kosong, coba semester pasangannya agar hasil inquiry sama dengan layar.
			if (detailBiayas.isEmpty()) {
				int smtAlternatif = smt == 0 ? 1 : 0;
				java.util.Collection<DetailBiaya> alternatif = pembayaranUtil
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1,
								smtAlternatif, true);
				if (alternatif != null && !alternatif.isEmpty()) {
					detailBiayas.addAll(alternatif);
					smt = smtAlternatif;
					Kegiatan kegiatanAlternatif = biodataCalonMahasiswa.ambilKegiatans(smt, jenisKegiatan);
					if (kegiatanAlternatif != null) {
						kegiatan = kegiatanAlternatif;
					}
				}
			}

			String pemb = "|";
			Long total = 0L;

			for (DetailBiaya biaya : detailBiayas) {
				ItemBiaya itemBiaya = biaya.getItemBiaya();
//				Double nilai = biaya.hitungTotalKegiatan(kegiatan);
//				Double nilai = (biaya.getNilaiBiayaBaru() == null ? biaya.getNilaiBiaya() : biaya.getNilaiBiayaBaru());

				DetailKegiatan tempdata =kegiatan==null?null: null;
				if (kegiatan != null && kegiatan.getId() != null && tempdata == null) {

					tempdata = kegiatan.ambilSatuDetailKegiatan(biaya);

					if (tempdata == null) {
						tempdata = new DetailKegiatan();
						tempdata.setUraian("");
						tempdata.setDetailBiaya(biaya);

						tempdata.setKeterangan(biaya == null ? "" : biaya.getKeterangan());
						tempdata.setKegiatan(kegiatan);
						try {
							Session session = HibernateUtil.currentNativeSession();
							session.getTransaction().begin();
							session.save(tempdata);
							session.getTransaction().commit();
							// session.disconnect();
							if (session.isOpen()) {session.disconnect();session.close();}
						}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/logic/InqueryLogic.java:335");
							// TODO: handle exception
						}
						HibernateUtil.closeSession();
					}
				}

				DetailKegiatan detailKegiatan = tempdata;
				Double nilai = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, biaya, false);

				if (myjurusan1 != null) {
					pemb += itemBiaya.getKode().trim()
							+ (biaya.getJurusan() == null ? "" : "-" + biaya.getJurusan().getKode()) + "\\"
							+ itemBiaya.getNama().trim()
							+ (biaya.getJurusan() == null ? "" : "-" + biaya.getJurusan().getNama()) + "\\"
							+ itemBiaya.getDeskripsi().trim()
							+ (biaya.getJurusan() == null ? "" : "-" + biaya.getJurusan().getNama()) + "\\"
							+ (nilai).longValue() + "|";
				} else {
					pemb += itemBiaya.getKode().trim() + "\\" + itemBiaya.getNama().trim() + "\\"
							+ itemBiaya.getDeskripsi().trim() + "\\" + (nilai).longValue() + "|";
				}
				total += (nilai).longValue();
			}

			if (kegiatan != null && kegiatan.getAmountTerhutang() != null && kegiatan.getAmountTerhutang() > 0.1
					&& kegiatan.getAmountTerhutang() < total) {
				pemb += "00\\Diskon\\Potongan\\" + (kegiatan.getAmountTerhutang().longValue() - total) + "|";
				total = kegiatan.getAmountTerhutang().longValue();
			}

			boolean tidakBolehMencicil = Common.bolehKonfigurasi("calon_mahasiswa_tidak_boleh_mencicil_pembayaran_daftar_ulang_via_h2h");

			// Double telahDibayar = kegiatan == null ? 0.0 : kegiatan
			// .getJumlahTelahDibayar();
			// Double sisaCicilan = total - telahDibayar;
			Double sisaCicilan = 100000.0;// hanya temporary

			if ((kegiatan == null || kegiatan.getId() == null || kegiatan.getAmount() < 0.01 || !tidakBolehMencicil)
					&& sisaCicilan > 0.001) {

				data.add(new String[] { "response_code",
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS) });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai" : "Inquiry Sukses Dilakukan" });
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

				data.add(new String[] { "semester", Perkuliahan.GANJIL });
				data.add(new String[] { "semester_ke", smt + "" });
				data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
				data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
				data.add(new String[] { "amount", pemb });
				data.add(new String[] { "total_amount", total + "" });
				data.add(new String[] { "kode_status_pembayaran",
						ConstantUtil.PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU });
				data.add(
						new String[] { "keterangan_status_pembayaran", ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
				data.add(new String[] { "jumlah_yang_telah_dibayar",
						kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

				logHostToHost.setResponseCode(
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS));
				logHostToHost.setResponseDescription(
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai" : "Inquiry Sukses Dilakukan");
			} else {
				data.add(new String[] { "response_code", ConstantUtil.BILLS_HAVE_BEEN_PAID });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai"
								: "Inquiry gagal dilakukan, karena mahasiswa sudah melakukan pembayaran" });
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

				data.add(new String[] { "semester", Perkuliahan.GANJIL });
				data.add(new String[] { "semester_ke", smt + "" });
				data.add(new String[] { "tanggal_max", Common.dateFormat2.get().format(jadwalPembayaran.getEndDate()) });
				data.add(new String[] { "tanggal_min", Common.dateFormat2.get().format(jadwalPembayaran.getStartDate()) });
				data.add(new String[] { "amount", pemb });
				data.add(new String[] { "total_amount", total + "" });
				data.add(new String[] { "kode_status_pembayaran",
						ConstantUtil.PEMBAYARAN_PENDAFTARAN_ULANG_MAHASISWA_BARU });
				data.add(
						new String[] { "keterangan_status_pembayaran", ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

				data.add(new String[] { "jumlah_yang_telah_dibayar",
						kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

				logHostToHost.setResponseCode((ConstantUtil.BILLS_HAVE_BEEN_PAID));
				logHostToHost.setResponseDescription(
						"Inquiry gagal dilakukan, karena calon mahasiswa sudah melakukan pembayaran");
			}

			logHostToHost.setBankHost(bankHost);
			logHostToHost.setIp(bankHost.getIp());
			logHostToHost.setNama(nama);
			logHostToHost.setNim(
					biodataCalonMahasiswa.getNoUjian() == null || biodataCalonMahasiswa.getNoUjian().trim().equals("")
							? biodataCalonMahasiswa.getNoRegistrasi()
							: biodataCalonMahasiswa.getNoUjian());
			logHostToHost.setKeterangan(CommonUtil.convertToString(data));
			logHostToHost.setTransactionType(ConstantUtil.INQUERY);
			CommonUtil.setRequestAndresponse(logHostToHost);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(logHostToHost);
			session.getTransaction().commit();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return data.toArray(new String[][] { null });
	}

	public String[][] inqueryMahasiswaLama(Mahasiswa mahasiswa, BankHost bankHost, String nama,
			LogHostToHost logHostToHost) {
		return inqueryMahasiswaLama(mahasiswa, bankHost, nama, logHostToHost, null, null);
	}

	@SuppressWarnings({ "rawtypes" })
	public String[][] inqueryMahasiswaLama(Mahasiswa mahasiswa, BankHost bankHost, String nama,
			LogHostToHost logHostToHost, String kode, String bulan) {
		System.out.println(" ============== Inquery mahasiswa lama ==================");

		bulan = Common.isNumber(bulan) ? bulan : null;

		try {
			Session session = HibernateUtil.currentNativeSession();
			int countMahasiswaPindahan = ((Number) session.createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(Restrictions.eq("alihProdiMahasiswa", mahasiswa))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			HibernateUtil.closeSession();

			if (countMahasiswaPindahan > 0) {
				return displayUtil
						.displayAlihProdi(logHostToHost, mahasiswa.getNim(), bankHost, nama, ConstantUtil.INQUERY)
						.toArray(new String[][] { null });
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}

		List<String[]> data = new ArrayList<String[]>();
		try {

			JenisKegiatan jenisKegiatan = null;
			if (kode == null) {
				jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
			} else {
				jenisKegiatan = pembayaranUtil.generateJenisKegiatanByKode(kode);

				if (jenisKegiatan == null) {
					return displayUtil.displayKodePembayaranTidakDitemukan(logHostToHost, mahasiswa.getNim(), bankHost,
							nama, ConstantUtil.INQUERY).toArray(new String[][] { null });
				}

			}

			Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaHanyaBerdasarJenisKegiatan(
					ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, mahasiswa.getJenjang(), bulan,
					mahasiswa.getJenisSeleksi(), mahasiswa.getProgram(), mahasiswa.getNim());

			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

			if (jadwalPembayaran == null) {
				return displayUtil.displayPembayaranTerlambat(logHostToHost, mahasiswa.getNim(), bankHost, nama,
						ConstantUtil.INQUERY).toArray(new String[][] { null });
			}

			Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil()
					: jadwalPembayaran.getGanjil();
			String ta = jadwalPembayaran.getTahunAkademik() == null ? Common.getCurrentTahunAkademik()
					: jadwalPembayaran.getTahunAkademik();
			Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta,
					ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
					mahasiswa.getSemesterMulai());

			JenisKegiatan kegiatanDaftarUlangMahasiswaBaru = pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
			List<TunggakanMahasiswa> tunggakanMahasiswas = pembayaranUtil.getTunggakanMahasiswa(
					new JenisKegiatan[] { kegiatanDaftarUlangMahasiswaBaru, jenisKegiatan }, mahasiswa, semester, null);

			if (tunggakanMahasiswas.size() != 0) {
				return displayUtil.displayTunggakanMahasiswa(logHostToHost, mahasiswa.getNim(), bankHost, nama,
						ConstantUtil.INQUERY, tunggakanMahasiswas).toArray(new String[][] { null });
			}

			Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan, true);

			String pemb = "|";

			Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
					: kegiatan.ambilDetailKegiatan();

			Double nilaiBiayaHarusDiBayars = 0.0;
			try {

				Collection mydetailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, semester,
						jadwalPembayaran.getJenisKegiatan(), true);
				Session session = HibernateUtil.currentNativeSession();
				int countPengaturanBulanan = PembayaranUtil.getInstance().countBulanan(session, mahasiswa,
						jadwalPembayaran.getJenisKegiatan(), semester, mydetailBiayas, true, true);
				HibernateUtil.closeSession();
				if (countPengaturanBulanan > 0) {
					mydetailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, semester,
							jadwalPembayaran.getJenisKegiatan(), "-1", true);
				}

				for (Object o : mydetailBiayas) {
					if (o instanceof DetailBiaya) {
						DetailBiaya detailBiaya = (DetailBiaya) o;
//						Double nilai = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya);
						DetailKegiatan detailKegiatan = null;
						if (detailKegiatans != null && !detailKegiatans.isEmpty()) {
							for (DetailKegiatan d : detailKegiatans) {
								if (d.getDetailBiaya() != null
										&& d.getDetailBiaya().getId().equals(detailBiaya.getId())) {
									detailKegiatan = d;
									break;
								}
							}
						}
						Double nilai = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, detailBiaya, true);
						nilaiBiayaHarusDiBayars += nilai;
						ItemBiaya itemBiaya = detailBiaya.getItemBiaya();
						System.out.println("biaya = " + itemBiaya + ", nilai " + nilai);
						pemb += itemBiaya.getKode().trim() + "\\" + itemBiaya.getNama().trim() + "\\"
								+ (nilai).longValue() + "|";

					} else if (o instanceof PengaturanPembayaranBulanan) {
						PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
						Double nilai = Kegiatan.ambilJumlahTagihan(kegiatan, detailKegiatans, mahasiswa, semester,
								pengaturanPembayaranBulanan);
						nilaiBiayaHarusDiBayars += nilai;
						ItemBiaya itemBiaya = pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya();
						System.out.println("biaya = " + itemBiaya + ", nilai " + nilai);
						pemb += itemBiaya.getKode().trim() + "\\" + itemBiaya.getNama().trim() + "\\"
								+ (nilai).longValue() + "|";
					}
				}

				System.out.println("mahasiswa " + mahasiswa + ", nilaiBiayaHarusDiBayars " + nilaiBiayaHarusDiBayars);

			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			}

			Long total = nilaiBiayaHarusDiBayars.longValue();
			Double jumlahDibayar = kegiatan == null ? Double.valueOf(0.0) : kegiatan.getJumlahTelahDibayar();
			Double sisaTagihan = CommonUtil.hitungSisaTagihan(nilaiBiayaHarusDiBayars, jumlahDibayar);
			boolean tagihanLunas = total.longValue() > 0L && sisaTagihan.doubleValue() <= 0.001;

			if (!tagihanLunas) {

				data.add(new String[] { "response_code",
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS) });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai" : "Inquiry Sukses Dilakukan" });
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
				data.add(new String[] { "amount", CommonUtil.sesuaikanRincianDenganSisa(pemb, sisaTagihan) });
				data.add(new String[] { "total_amount", sisaTagihan.longValue() + "" });
				data.add(new String[] { "kode_status_pembayaran", jenisKegiatan.getKode() });
				data.add(new String[] { "keterangan_status_pembayaran", jenisKegiatan.getNamaKegiatan() });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });

				data.add(new String[] { "jumlah_yang_telah_dibayar",
						kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

				System.out.println("================================= total " + total.toString().equals("0"));
				logHostToHost.setResponseCode(
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS));
				logHostToHost.setResponseDescription(
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai" : "Inquiry Sukses Dilakukan");
			} else {
				data.add(new String[] { "response_code", ConstantUtil.BILLS_HAVE_BEEN_PAID });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai"
								: "Inquiry gagal dilakukan, karena mahasiswa sudah melakukan pembayaran" });
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
				data.add(new String[] { "amount", CommonUtil.sesuaikanRincianDenganSisa(pemb, sisaTagihan) });
				data.add(new String[] { "total_amount", sisaTagihan.longValue() + "" });
				data.add(new String[] { "kode_status_pembayaran", jenisKegiatan.getKode() });
				data.add(new String[] { "keterangan_status_pembayaran", jenisKegiatan.getNamaKegiatan() });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
				data.add(new String[] { "jumlah_yang_telah_dibayar",
						kegiatan == null ? "0.0" : kegiatan.getJumlahTelahDibayar().toString() });

				logHostToHost.setResponseCode((ConstantUtil.BILLS_HAVE_BEEN_PAID));
				logHostToHost
						.setResponseDescription("Inquiry gagal dilakukan, karena mahasiswa sudah melakukan pembayaran");
			}

			logHostToHost.setBankHost(bankHost);
			logHostToHost.setIp(bankHost.getIp());
			logHostToHost.setNama(nama);
			logHostToHost.setNim(mahasiswa.getNim());
			logHostToHost.setKeterangan(CommonUtil.convertToString(data));
			CommonUtil.setRequestAndresponse(logHostToHost);
			logHostToHost.setTransactionType(ConstantUtil.INQUERY);
			CommonUtil.setRequestAndresponse(logHostToHost);

			Session session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(logHostToHost);
			session.getTransaction().commit();

			HibernateUtil.closeSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return data.toArray(new String[][] { null });
	}
}
