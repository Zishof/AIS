package ais.action.ws.logic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;

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
import ais.database.model.LogHostToHost;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Perkuliahan;
import ais.ui.util.WaktuUtil;

/**
 * Implementasi logika <b>reversal</b> (pembatalan tagihan yang sudah tercatat lunas/terproses)
 * untuk integrasi Host-to-Host (H2H) modul web service pembayaran bank. Dipanggil dari servlet H2H
 * saat bank mengirim perintah reversal atas transaksi VA yang sebelumnya sukses — biasanya karena
 * pembayaran dibatalkan/gagal di sisi bank setelah sempat dilaporkan berhasil ke AIS. Ketiga method
 * publik menangani tiga jenis debitur (calon mahasiswa PMB, mahasiswa baru daftar ulang, mahasiswa
 * lama semester berjalan) namun mengikuti pola yang sama:
 * <ol>
 * <li>Menentukan {@link Kegiatan} (tagihan) yang menjadi sasaran reversal, berdasarkan jenis
 * kegiatan pembayaran yang relevan (registrasi, daftar ulang, atau kode kegiatan bebas untuk
 * mahasiswa lama).</li>
 * <li>Mengambil jadwal pembayaran yang berlaku ({@link JadwalPembayaran}) — bila tidak ditemukan
 * (di luar periode pembayaran), mengembalikan respons "pembayaran terlambat" tanpa melakukan
 * reversal apa pun.</li>
 * <li>Menyusun kembali rincian tagihan ({@link DetailBiaya}/{@link DetailKegiatan}) persis seperti
 * saat inquiry/pembayaran (memakai sumber rincian yang sama agar nominal reversal konsisten dengan
 * nominal yang sebelumnya dibayar), lalu menjumlahkannya menjadi total tagihan.</li>
 * <li>Bila tagihan ({@code kegiatan}) tidak ditemukan sama sekali, mengembalikan respons gagal
 * ({@code BILLS_NOT_FOUND}) TANPA mengubah data apa pun.</li>
 * <li>Bila ditemukan, <b>benar-benar membatalkan status pembayaran</b> lewat
 * {@link PembayaranUtil#dropKegiatan} — inilah efek nyata reversal: kegiatan/tagihan yang
 * sebelumnya tercatat lunas dikembalikan menjadi belum lunas. Bila operasi ini gagal, mengembalikan
 * respons kesalahan sistem tanpa mengklaim reversal berhasil.</li>
 * <li>Menyusun respons H2H standar (kode+deskripsi respons, rincian debitur, rincian tagihan per
 * item biaya, total, jadwal berlaku) dan mencatat seluruh permintaan+respons ke
 * {@link LogHostToHost} untuk audit — log ini SELALU disimpan, baik reversal berhasil maupun
 * gagal.</li>
 * </ol>
 * Seluruh method membungkus proses dalam try-catch tunggal yang menelan exception (dicatat lewat
 * {@code Common.tampilErrorJikaAdmin}), mengembalikan data yang sudah terkumpul sejauh proses
 * berjalan — pemanggil perlu memeriksa isi {@code response_code} pada hasil, bukan mengandalkan
 * exception, untuk mengetahui keberhasilan reversal.
 */
public class ReversalLogic {
	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	public DisplayUtil displayUtil = new DisplayUtil();

	/**
	 * Memproses reversal tagihan pendaftaran calon mahasiswa (PMB) baru. Menyusun kembali rincian
	 * biaya pendaftaran calon mahasiswa dari {@link PembayaranUtil#getDetailBiayaCalonMahasiswa},
	 * lalu — bila tagihan ditemukan — membatalkan status lunasnya lewat
	 * {@link PembayaranUtil#dropKegiatan}. Lihat javadoc kelas untuk alur lengkap dan efek
	 * finansial. Seluruh permintaan/respons dicatat ke {@code logHostToHost} baik sukses maupun
	 * gagal.
	 *
	 * @param biodataCalonMahasiswa calon mahasiswa yang tagihannya di-reversal
	 * @param bankHost              identitas bank pengirim perintah H2H
	 * @param nama                  nama pemanggil/terminal H2H untuk keperluan log
	 * @param logHostToHost         entitas log transaksi H2H yang akan diisi dan disimpan
	 * @param nominalTagihan        nominal yang diklaim bank (saat ini tidak divalidasi terhadap nominal server — lihat blok validasi yang dikomentari)
	 * @return array pasangan kunci-nilai (format respons H2H) berisi hasil reversal; kosong bila terjadi exception tak tertangani di jalur non-tagihan-ditemukan
	 */
	public String[][] reversalCalonMahasiswa(BiodataCalonMahasiswa biodataCalonMahasiswa, BankHost bankHost,
			String nama, LogHostToHost logHostToHost, Double nominalTagihan) {
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
						}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/logic/ReversalLogic.java:100");
							// TODO: handle exception
						}
						HibernateUtil.closeSession();
					}
				}

				DetailKegiatan detailKegiatan = tempdata;
				Double nilai = Kegiatan.ambilJumlahTagihan(detailKegiatan, kegiatan, biaya, false);

				if (myjurusan1 != null /* || myjurusan2 != null */) {
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

			if (kegiatan.getAmountTerhutang() != null && kegiatan.getAmountTerhutang() > 0.1
					&& kegiatan.getAmountTerhutang() < total) {
				pemb += "00\\Diskon\\Potongan\\" + (kegiatan.getAmountTerhutang().longValue() - total) + "|";
				total = kegiatan.getAmountTerhutang().longValue();
			}

			if (kegiatan == null || kegiatan.getId() == null) {
				data.add(new String[] { "response_code", ConstantUtil.BILLS_NOT_FOUND });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai"
								: "Reversal gagal dilakukan, karena tagihan tidak ditemukan" });
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
				logHostToHost.setResponseCode((ConstantUtil.BILLS_NOT_FOUND));
				logHostToHost.setResponseDescription("Reversal gagal dilakukan, karena tagihan tidak ditemukan");
			} else {
				boolean b = pembayaranUtil.dropKegiatan(kegiatan, null, null);
				if (!b) {
					return displayUtil.displayKesalahanSistem(logHostToHost, biodataCalonMahasiswa.getNoRegistrasi(),
							bankHost, nama, ConstantUtil.PAY).toArray(new String[][] { null });
				}
				data.add(new String[] { "response_code",
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS) });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai"
								: "Reversal sukses dilakukan" });
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
				logHostToHost.setResponseCode(
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS));
				logHostToHost.setResponseDescription(
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai" : "Reversal sukses dilakukan");

			}

			logHostToHost.setBankHost(bankHost);
			logHostToHost.setIp(bankHost.getIp());
			logHostToHost.setNama(nama);
			logHostToHost.setNim(biodataCalonMahasiswa.getNoRegistrasi());
			logHostToHost.setKeterangan(CommonUtil.convertToString(data));
			CommonUtil.setRequestAndresponse(logHostToHost);
			logHostToHost.setTransactionType(ConstantUtil.REVERSAL);

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

	/**
	 * Memproses reversal tagihan daftar ulang mahasiswa baru. Serupa dengan
	 * {@link #reversalCalonMahasiswa}, tetapi memakai jenis kegiatan
	 * {@code PENDAFTARAN_ULANG_MAHASISWA_BARU} dan rincian biaya dari program studi yang sudah
	 * lulus seleksi ({@code getProdiLulus()}) bila tersedia; jatuh ke rincian dari {@code kegiatan}
	 * yang sudah ada ({@link PembayaranUtil#getDetailBiayaDariKegiatan}) bila pencarian rincian
	 * biaya standar kosong — memastikan reversal memakai sumber rincian yang sama dengan yang
	 * dipakai saat inquiry/pembayaran berlangsung. Lihat javadoc kelas untuk alur lengkap dan efek
	 * finansial ({@link PembayaranUtil#dropKegiatan}).
	 *
	 * @param biodataCalonMahasiswa mahasiswa baru (masih berstatus calon mahasiswa) yang tagihannya di-reversal
	 * @param bankHost              identitas bank pengirim perintah H2H
	 * @param nama                  nama pemanggil/terminal H2H untuk keperluan log
	 * @param logHostToHost         entitas log transaksi H2H yang akan diisi dan disimpan
	 * @param nominalTagihan        nominal yang diklaim bank (saat ini tidak divalidasi terhadap nominal server)
	 * @return array pasangan kunci-nilai (format respons H2H) berisi hasil reversal
	 */
	public String[][] reversalMahasiswaBaru(BiodataCalonMahasiswa biodataCalonMahasiswa, BankHost bankHost, String nama,
			LogHostToHost logHostToHost, Double nominalTagihan) {
		List<String[]> data = new ArrayList<String[]>();
		try {
			JenisKegiatan jenisKegiatan = pembayaranUtil
					.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_ULANG_MAHASISWA_BARU);
			int smt = 1;
			Kegiatan kegiatan = biodataCalonMahasiswa.getPembayaranDaftarUlang();
			if (kegiatan == null) {
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
			java.util.Collection<DetailBiaya> detailBiayas;
			if (myjurusan1 == null || myjurusan1.getId() == null) {
				myjurusan1 = biodataCalonMahasiswa.getProdi1() == null ? biodataCalonMahasiswa.getProdi2()
						: biodataCalonMahasiswa.getProdi1();
				java.util.Collection<DetailBiaya> detailBiayas1 = pembayaranUtil
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, smt, true);

				detailBiayas = detailBiayas1;
			} else {
				java.util.Collection<DetailBiaya> detailBiayas1 = pembayaranUtil
						.getDetailBiayaCalonMahasiswa(biodataCalonMahasiswa, jenisKegiatan, myjurusan1, smt, true);
				detailBiayas = detailBiayas1;
			}

			// Reversal wajib memakai sumber rincian yang sama dengan inquiry/payment.
			if (detailBiayas.isEmpty()
					&& !ais.action.master.helper.PengecualianTagihanList.adalah(detailBiayas)) {
				detailBiayas.addAll(pembayaranUtil.getDetailBiayaDariKegiatan(kegiatan));
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
						}catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/ws/logic/ReversalLogic.java:314");
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

			if (kegiatan.getAmountTerhutang() != null && kegiatan.getAmountTerhutang() > 0.1
					&& kegiatan.getAmountTerhutang() < total) {
				pemb += "00\\Diskon\\Potongan\\" + (kegiatan.getAmountTerhutang().longValue() - total) + "|";
				total = kegiatan.getAmountTerhutang().longValue();
			}

			// if (Common
			// .getKonfigurasi(
			// "nominal_pembayaran_h2h_harus_sama_dengan_tagihan",
			// Konfigurasi.AKTIF).getNilai()
			// .equals(Konfigurasi.AKTIF)) {
			// if (!nominalTagihan.equals(total.doubleValue())) {
			// return displayUtil.displayNominalTagihanTidakMencukupi(
			// logHostToHost,
			// biodataCalonMahasiswa.getNoRegistrasi(), bankHost,
			// nama, ConstantUtil.REVERSAL).toArray(
			// new String[][] { null });
			// }
			// }

			if (kegiatan == null || kegiatan.getId() == null) {
				data.add(new String[] { "response_code", ConstantUtil.BILLS_NOT_FOUND });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai"
								: "Reversal gagal dilakukan, karena tagihan tidak ditemukan" });
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
				data.add(new String[] { "semester_ke", "0" });
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
				logHostToHost.setResponseCode((ConstantUtil.BILLS_NOT_FOUND));
				logHostToHost.setResponseDescription("Reversal gagal dilakukan, karena tagihan tidak ditemukan");
			} else {
				boolean b = pembayaranUtil.dropKegiatan(kegiatan, null, null);
				if (!b) {
					return displayUtil.displayKesalahanSistem(logHostToHost, biodataCalonMahasiswa.getNoRegistrasi(),
							bankHost, nama, ConstantUtil.PAY).toArray(new String[][] { null });
				}
				data.add(new String[] { "response_code",
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS) });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai"
								: "Reversal sukses dilakukan" });
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
				data.add(new String[] { "semester_ke", "0" });
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
				logHostToHost.setResponseCode(
						(total.toString().equals("0") ? ConstantUtil.NOT_VALID_AMOUNT : ConstantUtil.SUCCESS));
				logHostToHost.setResponseDescription(
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai" : "Reversal sukses dilakukan");

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

	/** Seperti {@link #reversalMahasiswaLama(Mahasiswa, BankHost, String, LogHostToHost, Double, String, String, String)} dengan jenis kegiatan default (SPP/pembayaran semester berjalan, {@code kode} dan {@code bulan} {@code null}). */
	public String[][] reversalMahasiswaLama(Mahasiswa mahasiswa, BankHost bankHost, String nama,
			LogHostToHost logHostToHost, Double nominalTagihan, String kodeAsli) {
		return reversalMahasiswaLama(mahasiswa, bankHost, nama, logHostToHost, nominalTagihan, null, null, kodeAsli);
	}

	/**
	 * Memproses reversal tagihan mahasiswa lama (aktif, semester berjalan) — jenis reversal paling
	 * fleksibel di kelas ini karena mendukung {@code kode} jenis kegiatan pembayaran bebas (bukan
	 * hanya SPP standar {@code PENDAFTARAN_MAHASISWA_LAMA}) dan {@code bulan} untuk tagihan bulanan
	 * (mis. asrama/uang makan). Bila {@code kode} diberikan tapi tidak dikenali sistem, langsung
	 * mengembalikan respons "kode pembayaran tidak ditemukan" tanpa reversal.
	 *
	 * <p>
	 * Rincian tagihan disusun dari kombinasi {@link DetailBiaya} biasa dan, bila berlaku,
	 * {@link PengaturanPembayaranBulanan} (tagihan bulanan berulang) — nilai masing-masing dihitung
	 * lewat {@link Kegiatan#ambilJumlahTagihan}. Efek finansial nyata terjadi lewat
	 * {@link PembayaranUtil#dropKegiatan(Kegiatan, String, String)} (parameter {@code bulan} dan
	 * {@code kodeAsli} menentukan cakupan pembatalan pada tagihan bulanan), yang mengembalikan
	 * status kegiatan/tagihan yang sebelumnya lunas menjadi belum lunas. Lihat javadoc kelas untuk
	 * alur lengkap.
	 * </p>
	 *
	 * @param mahasiswa      mahasiswa aktif yang tagihannya di-reversal
	 * @param bankHost       identitas bank pengirim perintah H2H
	 * @param nama           nama pemanggil/terminal H2H untuk keperluan log
	 * @param logHostToHost  entitas log transaksi H2H yang akan diisi dan disimpan
	 * @param nominalTagihan nominal yang diklaim bank (saat ini tidak divalidasi terhadap nominal server)
	 * @param kode           kode jenis kegiatan pembayaran spesifik, atau {@code null} untuk default SPP
	 * @param bulan          bulan tagihan (untuk pembayaran bulanan berulang), harus numerik atau diabaikan
	 * @param kodeAsli       kode transaksi asli yang di-reversal, diteruskan ke {@link PembayaranUtil#dropKegiatan}
	 * @return array pasangan kunci-nilai (format respons H2H) berisi hasil reversal
	 */
	@SuppressWarnings({ "rawtypes" })
	public String[][] reversalMahasiswaLama(Mahasiswa mahasiswa, BankHost bankHost, String nama,
			LogHostToHost logHostToHost, Double nominalTagihan, String kode, String bulan, String kodeAsli) {

		bulan = Common.isNumber(bulan) ? bulan : null;

		List<String[]> data = new ArrayList<String[]>();
		try {
			JenisKegiatan jenisKegiatan = null;
			if (kode == null) {
				jenisKegiatan = pembayaranUtil.generateJenisKegiatan(ConstantUtil.PENDAFTARAN_MAHASISWA_LAMA);
			} else {
				jenisKegiatan = pembayaranUtil.generateJenisKegiatanByKode(kode);

				if (jenisKegiatan == null) {
					return displayUtil.displayKodePembayaranTidakDitemukan(logHostToHost, mahasiswa.getNim(), bankHost,
							nama, ConstantUtil.REVERSAL).toArray(new String[][] { null });
				}

			}

			Serializable[] serializables = pembayaranUtil.getJadwalPembayaranDanDendaHanyaBerdasarJenisKegiatan(
					ais.ui.util.WaktuUtil.getDate(), jenisKegiatan, mahasiswa.getJenjang(), bulan,
					mahasiswa.getJenisSeleksi(), mahasiswa.getProgram(), mahasiswa.getNim());

			JadwalPembayaran jadwalPembayaran = (JadwalPembayaran) serializables[0];

			if (jadwalPembayaran == null) {
				return displayUtil.displayPembayaranTerlambat(logHostToHost, mahasiswa.getNim(), bankHost, nama,
						ConstantUtil.REVERSAL).toArray(new String[][] { null });
			}

			Boolean ganjil = jadwalPembayaran.getGanjil() == null ? Common.isNowSemensterGanjil()
					: jadwalPembayaran.getGanjil();
			String ta = jadwalPembayaran.getTahunAkademik() == null ? Common.getCurrentTahunAkademik()
					: jadwalPembayaran.getTahunAkademik();
			Integer semester = Common.getSemester(mahasiswa.getTahunangkatan(), ta,
					ganjil ? Perkuliahan.GANJIL : Perkuliahan.GENAP, mahasiswa.getPindahKeKampusIniMasukSemester(),
					mahasiswa.getSemesterMulai());

			Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan, true);

			String pemb = "|";

			Collection<DetailKegiatan> detailKegiatans = kegiatan == null || kegiatan.getId() == null ? null
					: kegiatan.ambilDetailKegiatan();
			Collection mydetailBiayas = PembayaranUtil.getInstance().getDetailBiayaMahasiswa(mahasiswa, semester,
					jadwalPembayaran.getJenisKegiatan(), true);
			Double nilaiBiayaHarusDiBayars = 0.0;
			try {
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

			if (kegiatan == null || kegiatan.getId() == null) {
				data.add(new String[] { "response_code", ConstantUtil.BILLS_NOT_FOUND });
				data.add(new String[] { "response_description",
						total.toString().equals("0") ? "Jumlah pembayaran tidak sesuai"
								: "Reversal gagal dilakukan, karena tagihan tidak ditemukan" });
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
				data.add(new String[] { "kode_status_pembayaran", jenisKegiatan.getKode() });
				data.add(new String[] { "keterangan_status_pembayaran", jenisKegiatan.getNamaKegiatan() });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
				logHostToHost.setResponseCode((ConstantUtil.BILLS_NOT_FOUND));
				logHostToHost.setResponseDescription("Reversal gagal dilakukan, karena tagihan tidak ditemukan");
			} else {
				boolean b = pembayaranUtil.dropKegiatan(kegiatan, bulan, kodeAsli);
				if (!b) {
					return displayUtil
							.displayKesalahanSistem(logHostToHost, mahasiswa.getNim(), bankHost, nama, ConstantUtil.PAY)
							.toArray(new String[][] { null });
				}

				data.add(new String[] { "response_code", ConstantUtil.SUCCESS });
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
				data.add(new String[] { "kode_status_pembayaran", jenisKegiatan.getKode() });
				data.add(new String[] { "keterangan_status_pembayaran", jenisKegiatan.getNamaKegiatan() });
				data.add(new String[] { "reference_number",
						(kegiatan == null || kegiatan.getRefNumber() == null ? "" : kegiatan.getRefNumber()) });
				logHostToHost.setResponseCode((ConstantUtil.SUCCESS));
				logHostToHost.setResponseDescription("Reversal sukses dilakukan");

			}

			logHostToHost.setBankHost(bankHost);
			logHostToHost.setIp(bankHost.getIp());
			logHostToHost.setNama(nama);
			logHostToHost.setNim(mahasiswa.getNim());
			logHostToHost.setKeterangan(CommonUtil.convertToString(data));
			CommonUtil.setRequestAndresponse(logHostToHost);
			logHostToHost.setTransactionType(ConstantUtil.REVERSAL);

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
