package ais.action.master.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Messagebox;

import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.CommonHelperClass;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.DetailBiaya;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.ui.util.MyMessageboxConfig;

/**
 * Kumpulan pemeriksa (gatekeeper) statis yang menentukan apakah seorang {@link Mahasiswa}
 * boleh mencetak kartu UTS/UAS (dan variannya pada semester pendek) berdasarkan status
 * pembayaran. Dua jenis pemeriksaan independen disediakan, keduanya dapat menampilkan
 * messagebox peringatan/konfirmasi ke pengguna bila syarat belum terpenuhi:
 *
 * <ol>
 * <li><b>Persentase pelunasan total</b> ({@link #checkPembayaranSebelumUjianSudahMemenuhi})
 * — total tagihan {@link Kegiatan kegiatan} pembayaran terkait (mis. SPP semester
 * berjalan) harus lunas sekurang-kurangnya sekian persen, ambang batas diambil dari
 * konfigurasi {@code batas_terendah_persen_pembayaran_boleh_cetak_kartu_uts}/{@code
 * ..._uas} (dengan varian {@code _sp} untuk semester pendek dan
 * {@code _uas_plus_satu} untuk mengecek juga tagihan semester berikutnya sebelum
 * mengizinkan cetak kartu UAS). Ambang batas dapat dikonfigurasi per kombinasi
 * semester/tahun angkatan/jurusan/program/status awal mahasiswa.</li>
 * <li><b>Item biaya wajib</b> ({@link #checkItemBiayaPembayaranSebelumUjianSudahMemenuhi})
 * — item biaya tertentu (kode-kode pada {@code info1} konfigurasi
 * {@code mahasiswa_wajib_bayar_item_biaya_uts_sebelum_ikut_ujian_uts}/{@code
 * ..._uas} dan variannya) harus sudah dicicil melebihi persentase tertentu
 * ({@code info2}) dari total tagihan item tersebut, dihitung lintas seluruh
 * {@link JenisKegiatan} yang terdaftar sebagai syarat ujian
 * ({@code CommonHelperClass#jenisKegiatansUntukSyaratUjian}).</li>
 * </ol>
 *
 * <p>
 * Kedua jalur pemeriksaan menghormati bypass {@link Common#checkBaypassStatusPembayaranMahasiswa}
 * (mis. mahasiswa dengan pengecualian pembayaran) dan gagal-aman ke {@code true}
 * (mengizinkan cetak) bila konfigurasi ambang batas tidak valid/parsing gagal atau
 * terjadi exception tak terduga saat pengecekan — pemeriksaan pembayaran tidak boleh
 * memblokir pencetakan kartu ujian hanya karena kegagalan teknis internal. Bila
 * {@code listener} disediakan, peringatan ditampilkan sebagai dialog konfirmasi
 * ("tetap lanjutkan?") yang memanggil {@code listener} dengan hasil pilihan pengguna;
 * bila {@code null}, hanya messagebox informasi satu tombol OK yang ditampilkan.
 * </p>
 */
public class UtsDanUasCheckerHelper {

	/**
	 * Mengecek syarat persentase pelunasan sebelum mahasiswa boleh mencetak kartu UTS,
	 * dengan ambang batas dari konfigurasi
	 * {@code batas_terendah_persen_pembayaran_boleh_cetak_kartu_uts} (reguler, default
	 * 60%) atau {@code ..._uts_sp} (semester pendek, default 0% — efektif tidak
	 * mengecek). Bila ambang batas reguler {@code <= 0.1}, pengecekan dilewati
	 * (dianggap memenuhi).
	 *
	 * @param mahasiswa      mahasiswa yang dicek
	 * @param semester       semester akademik yang dicek tagihannya
	 * @param semesterPendek status semester pendek; {@code null} berarti reguler
	 * @param listener       callback konfirmasi bila syarat tidak terpenuhi; boleh {@code null} untuk sekadar menampilkan peringatan
	 * @return {@code true} bila syarat terpenuhi (atau pemeriksaan dilewati/gagal parsing konfigurasi)
	 */
	public static Boolean checkPembayaranSebelumUTSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester,
			Integer semesterPendek, EventListener listener) {
		
		if (semesterPendek == null) {
			Konfigurasi konfig = Common.getKonfigurasi("batas_terendah_persen_pembayaran_boleh_cetak_kartu_uts",
					"60", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
					mahasiswa.getStatusAwalMahasiswa());
			
			String batasTerendahPersen = konfig != null && konfig.getNilai() != null ? konfig.getNilai() : "60";
			Double batas = 0.0;
			try {
				batas = Double.parseDouble(batasTerendahPersen.trim());
			} catch (Exception e) {
				return true;
			}

			if (batas > 0.1) {
				return checkPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, batas, listener);
			}
		} else {
			Konfigurasi konfig = Common.getKonfigurasi("batas_terendah_persen_pembayaran_boleh_cetak_kartu_uts_sp", 
					"0", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
					mahasiswa.getStatusAwalMahasiswa());
			
			String batasTerendahPersen = konfig != null && konfig.getNilai() != null ? konfig.getNilai() : "0";
			Double batas = 0.0;
			try {
				batas = Double.parseDouble(batasTerendahPersen.trim());
			} catch (Exception e) {
				return true;
			}

			return checkPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, batas, ConstantValues.PEMBAYARAN_SP, listener);
		}
		return true;
	}

	/**
	 * Implementasi inti pengecekan persentase pelunasan untuk satu {@link JenisKegiatan}
	 * tagihan tertentu. Mengembalikan {@code true} langsung bila {@code batas < 0.01}
	 * atau bypass status pembayaran berlaku. Mengambil persentase lunas dari
	 * {@link Mahasiswa#ambilKegiatans(Integer, JenisKegiatan)}; khusus semester 1 dan
	 * {@code jenisKegiatan} = pendaftaran ulang mahasiswa baru, bila belum lunas dicek
	 * ulang lewat data {@link BiodataCalonMahasiswa} (riwayat sebagai calon mahasiswa)
	 * sebagai fallback. Menampilkan peringatan/konfirmasi bila tidak memenuhi syarat.
	 *
	 * @param mahasiswa    mahasiswa yang dicek
	 * @param semester     semester akademik yang dicek tagihannya
	 * @param batas        ambang batas persentase pelunasan minimal (0-100)
	 * @param jenisKegiatan jenis kegiatan/tagihan yang dicek
	 * @param listener     callback konfirmasi bila syarat tidak terpenuhi; boleh {@code null}
	 * @return {@code true} bila persentase lunas mahasiswa {@code >= batas}
	 */
	public static Boolean checkPembayaranSebelumUjianSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Double batas,
			JenisKegiatan jenisKegiatan, EventListener listener) {

		boolean hasil = true;
		if (batas < 0.01 || Common.checkBaypassStatusPembayaranMahasiswa(semester, null, mahasiswa, jenisKegiatan)) {
			return hasil;
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);

			hasil = kegiatan != null && kegiatan.getPersentaseLunas() != null && kegiatan.getPersentaseLunas() >= batas;
			
			// OPTIMASI: Menggunakan StringBuilder untuk mencetak log agar hemat RAM
			StringBuilder logBuilder = new StringBuilder();
			logBuilder.append("Check Pembayaran Ujian ").append(jenisKegiatan != null ? jenisKegiatan.getNamaKegiatan() : "")
					.append(" -> Mahasiswa ").append(mahasiswa)
					.append(", semester ").append(semester)
					.append(", batasTerendahPersen = ").append(batas).append("%")
					.append(", dibayar ").append(kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas())
					.append(", hasil = ").append(hasil)
					.append(", pengurangan = ").append(kegiatan == null ? 0.0 : kegiatan.getPengurangan());
			System.out.println(logBuilder.toString());

			if (!hasil && semester != null && semester.equals(1) && jenisKegiatan != null
					&& ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU != null
					&& jenisKegiatan.getId().equals(ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU.getId())) {
				
				BiodataCalonMahasiswa biodataCalonMahasiswa = (BiodataCalonMahasiswa) session
						.createCriteria(BiodataCalonMahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.eq("mahasiswa", mahasiswa))
						.addOrder(Order.desc("id"))
						.setMaxResults(1)
						.uniqueResult();
						
				if (biodataCalonMahasiswa != null) {
					kegiatan = biodataCalonMahasiswa.ambilKegiatans(semester, ConstantValues.PENDAFTARAN_ULANG_MAHASISWA_BARU);
					hasil = kegiatan != null && kegiatan.getPersentaseLunas() != null && kegiatan.getPersentaseLunas() >= batas;
					
					StringBuilder logCalon = new StringBuilder();
					logCalon.append("Check Pembayaran Ujian -> Calon Mahasiswa ").append(biodataCalonMahasiswa)
							.append(", semester ").append(semester)
							.append(", batasTerendahPersen = ").append(batas).append("%")
							.append(", dibayar ").append(kegiatan == null ? 0.0 : kegiatan.getPersentaseLunas())
							.append(", hasil = ").append(hasil)
							.append(", pengurangan = ").append(kegiatan == null ? 0.0 : kegiatan.getPengurangan());
					System.out.println(logCalon.toString());
				}
			}

			if (!hasil) {
				try {
					StringBuilder warningBuilder = new StringBuilder();
					warningBuilder.append("Mahasiswa dengan NIM \"").append(mahasiswa.getNim()).append("\"\n")
							.append("belum dapat mencetak kartu ujian karena syarat minimal pembayaran untuk dapat mencetak kartu ujian adalah pelunasan biaya sebesar ")
							.append(Common.numberFormat.get().format(batas)).append("% dari total tagihan ")
							.append(jenisKegiatan != null ? jenisKegiatan.getNamaKegiatan() : "").append(".\n\n")
							.append("Persentase pembayaran yang telah dilunasi sebesar ")
							.append(Common.numberFormat.get().format(kegiatan == null || kegiatan.getPersentaseLunas() == null ? 0.0 : kegiatan.getPersentaseLunas()))
							.append("%\npada semester ").append(semester);

					String warning = warningBuilder.toString();

					if (listener != null) {
						MyMessageboxConfig.showFormatCb(
								"{V1}\n\nApakah Bapak/Ibu berkenan untuk tetap melanjutkan pencetakan kartu ujian?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, listener, warning);
					} else {
						String bahasaConfig = Common.getBahasaConfig(
								"Apabila Bapak/Ibu telah melunasi seluruh pembayaran namun tetap terkendala dalam mencetak kartu ujian, mohon menghubungi pihak kampus agar dapat dilakukan pengecekan ulang.");
						warning = warning + "\n\n" + bahasaConfig;
						MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UtsDanUasCheckerHelper.java:145");
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UtsDanUasCheckerHelper.java:149");
		} finally {
			if (session != null) {
				try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:152");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:153");}
				try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:154");}
			}
		}
		
		return hasil;
	}

	/**
	 * Seperti {@link #checkPembayaranSebelumUjianSudahMemenuhi(Mahasiswa, Integer, Double, JenisKegiatan, EventListener)}
	 * tetapi mengecek SEMUA {@link JenisKegiatan} yang terdaftar sebagai syarat ujian
	 * ({@code CommonHelperClass#jenisKegiatansUntukSyaratUjian}, dimuat ulang bila
	 * belum ada), berhenti pada jenis kegiatan pertama yang belum memenuhi ambang batas.
	 *
	 * @param mahasiswa mahasiswa yang dicek
	 * @param semester  semester akademik yang dicek tagihannya
	 * @param batas     ambang batas persentase pelunasan minimal (0-100)
	 * @param listener  callback konfirmasi bila syarat tidak terpenuhi; boleh {@code null}
	 * @return {@code true} bila seluruh jenis kegiatan syarat ujian memenuhi ambang batas
	 */
	public static Boolean checkPembayaranSebelumUjianSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Double batas,
			EventListener listener) {
		boolean hasil = true;
		
		if (CommonHelperClass.jenisKegiatansUntukSyaratUjian == null) {
			try {
				CommonHelperClass.reloadJenisKegiatans();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:168");}
		}
		
		if (CommonHelperClass.jenisKegiatansUntukSyaratUjian != null) {
			for (JenisKegiatan jenisKegiatan : CommonHelperClass.jenisKegiatansUntukSyaratUjian) {
				hasil = checkPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, batas, jenisKegiatan, listener);
				if (!hasil) {
					return hasil;
				}
			}
		}

		return hasil;
	}

	/**
	 * Mengecek syarat pelunasan item biaya wajib sebelum mahasiswa boleh mencetak kartu
	 * UTS, dikendalikan konfigurasi
	 * {@code mahasiswa_wajib_bayar_item_biaya_uts_sebelum_ikut_ujian_uts} (reguler) atau
	 * {@code ..._uts_sp} (semester pendek). Bila konfigurasi tidak aktif, method
	 * langsung mengembalikan {@code true} tanpa mengecek apa pun.
	 *
	 * @param mahasiswa      mahasiswa yang dicek
	 * @param semester       semester akademik
	 * @param tahap          tahap pembayaran
	 * @param semesterPendek status semester pendek; {@code null} berarti reguler
	 * @param listener       callback konfirmasi bila syarat tidak terpenuhi; boleh {@code null}
	 * @return {@code true} bila syarat terpenuhi atau fitur ini tidak diaktifkan
	 */
	public static Boolean checkItemBiayaPembayaranSebelumUTSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester,
			Integer tahap, Integer semesterPendek, EventListener listener) {
		
		if (semesterPendek == null) {
			Konfigurasi konfigurasi = Common.getKonfigurasi(
					"mahasiswa_wajib_bayar_item_biaya_uts_sebelum_ikut_ujian_uts", Konfigurasi.TIDAK_AKTIF, "508",
					"100", "");
			if (konfigurasi != null && Konfigurasi.AKTIF.equalsIgnoreCase(konfigurasi.getNilai())) {
				return checkItemBiayaPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, tahap, konfigurasi, listener);
			} else {
				return true;
			}
		} else {
			Konfigurasi konfigurasi = Common.getKonfigurasi(
					"mahasiswa_wajib_bayar_item_biaya_uts_sebelum_ikut_ujian_uts_sp", Konfigurasi.TIDAK_AKTIF, "5081",
					"100", "");
			if (konfigurasi != null && Konfigurasi.AKTIF.equalsIgnoreCase(konfigurasi.getNilai())) {
				return checkItemBiayaPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, tahap, konfigurasi, listener);
			} else {
				return true;
			}
		}
	}

	/** Seperti {@link #checkItemBiayaPembayaranSebelumUTSSudahMemenuhi}, versi UAS (konfigurasi {@code ..._uas_sebelum_ikut_ujian_uas} / {@code ..._uas_sp}). */
	public static Boolean checkItemBiayaPembayaranSebelumUASSudahMemenuhi(Mahasiswa mahasiswa, Integer semester,
			Integer tahap, Integer semesterPendek, EventListener listener) {
		
		if (semesterPendek == null) {
			Konfigurasi konfigurasi = Common.getKonfigurasi(
					"mahasiswa_wajib_bayar_item_biaya_uas_sebelum_ikut_ujian_uas", Konfigurasi.TIDAK_AKTIF, "509",
					"100", "");
			if (konfigurasi != null && Konfigurasi.AKTIF.equalsIgnoreCase(konfigurasi.getNilai())) {
				return checkItemBiayaPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, tahap, konfigurasi, listener);
			} else {
				return true;
			}
		} else {
			Konfigurasi konfigurasi = Common.getKonfigurasi(
					"mahasiswa_wajib_bayar_item_biaya_uas_sebelum_ikut_ujian_uas_sp", Konfigurasi.TIDAK_AKTIF, "5091",
					"100", "");
			if (konfigurasi != null && Konfigurasi.AKTIF.equalsIgnoreCase(konfigurasi.getNilai())) {
				return checkItemBiayaPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, tahap, konfigurasi, listener);
			} else {
				return true;
			}
		}
	}

	/**
	 * Implementasi inti pengecekan item biaya wajib: (1) mengumpulkan kode item biaya
	 * dari {@code konfigurasi.getInfo1()} (dipisah koma); (2) menjumlahkan total tagihan
	 * untuk item-item tersebut lintas seluruh {@link JenisKegiatan} syarat ujian, dari
	 * {@link DetailBiaya} maupun {@link PengaturanPembayaranBulanan} (bila mahasiswa
	 * memakai skema cicilan bulanan) yang sedang berlaku bagi mahasiswa; (3)
	 * membandingkan total yang sudah dicicil ({@link Mahasiswa#hitungTotalCicilanPembayaran})
	 * terhadap persentase minimal dari {@code konfigurasi.getInfo2()} (default 100%).
	 * Mengembalikan {@code true} bila tidak ada item biaya relevan, total tagihan nol,
	 * persentase syarat {@code < 0.01}, bypass status pembayaran berlaku, atau terjadi
	 * exception (gagal-aman). Menampilkan peringatan berisi nama item biaya yang belum
	 * lunas bila syarat tidak terpenuhi.
	 *
	 * @param mahasiswa   mahasiswa yang dicek
	 * @param semester    semester akademik
	 * @param tahap       tahap pembayaran
	 * @param konfigurasi baris {@link Konfigurasi} yang {@code info1}-nya berisi kode item biaya (dipisah koma) dan {@code info2} berisi persentase syarat cicilan minimal
	 * @param listener    callback konfirmasi bila syarat tidak terpenuhi; boleh {@code null}
	 * @return {@code true} bila item biaya wajib sudah dicicil melebihi persentase syarat (atau tidak relevan/gagal-aman)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Boolean checkItemBiayaPembayaranSebelumUjianSudahMemenuhi(Mahasiswa mahasiswa, Integer semester,
			Integer tahap, Konfigurasi konfigurasi, EventListener listener) {

		Double totalTagihan = 0.0;
		if (CommonHelperClass.jenisKegiatansUntukSyaratUjian == null) {
			try {
				CommonHelperClass.reloadJenisKegiatans();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:239");}
		}

		if (Common.checkBaypassStatusPembayaranMahasiswa(semester, tahap, mahasiswa, CommonHelperClass.jenisKegiatansUntukSyaratUjian)) {
			return true;
		}

		List<String> kodes = new ArrayList<String>();
		if (konfigurasi != null && konfigurasi.getInfo1() != null) {
			for (String k : konfigurasi.getInfo1().trim().split(",")) {
				if (k != null && !k.trim().isEmpty()) {
					kodes.add(k.trim().toLowerCase());
				}
			}
		}

		Session session = null;
		try {
			// Menggunakan 1 Session terpusat untuk keseluruhan eksekusi di method ini
			session = HibernateUtil.getSessionFactory().openSession();
			
			if (CommonHelperClass.jenisKegiatansUntukSyaratUjian != null) {
				for (JenisKegiatan jenisKegiatan : CommonHelperClass.jenisKegiatansUntukSyaratUjian) {
					Kegiatan kegiatan = mahasiswa.ambilKegiatans(semester, jenisKegiatan);

					PembayaranUtil.getInstance();
					Collection mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
							jenisKegiatan, null, true, false);

					PembayaranUtil.getInstance();
					int countPengaturanBulanan = PembayaranUtilHelper.countBulanan(session, mahasiswa, jenisKegiatan,
							semester, mydetailBiayas, false, true);
							
					if (countPengaturanBulanan > 0) {
						PembayaranUtil.getInstance();
						mydetailBiayas = PembayaranUtilHelper.getDetailBiayaMahasiswa(mahasiswa, semester,
								jenisKegiatan, "-1", true);
					}

					if (mydetailBiayas != null) {
						for (Object o : mydetailBiayas) {
							if (o instanceof DetailBiaya) {
								DetailBiaya detailBiaya = (DetailBiaya) o;
								if (detailBiaya.getItemBiaya() != null && detailBiaya.getItemBiaya().getKode() != null 
										&& kodes.contains(detailBiaya.getItemBiaya().getKode().trim().toLowerCase())) {
									try {
										Double nilai = Kegiatan.ambilJumlahTagihan(kegiatan, detailBiaya, true);
										totalTagihan += (nilai != null ? nilai : 0.0);

										System.out.println("jenisKegiatan = " + jenisKegiatan + ", item = " + detailBiaya.getItemBiaya()
												+ " nilai =" + nilai + ", totalTagihan = " + totalTagihan);
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UtsDanUasCheckerHelper.java:291");
									}
								}
							} else if (o instanceof PengaturanPembayaranBulanan) {
								PengaturanPembayaranBulanan pengaturanPembayaranBulanan = (PengaturanPembayaranBulanan) o;
								if (pengaturanPembayaranBulanan.getDetailBiaya() != null 
										&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya() != null
										&& pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode() != null
										&& kodes.contains(pengaturanPembayaranBulanan.getDetailBiaya().getItemBiaya().getKode().trim().toLowerCase())) {
									try {
										Double nilai = Kegiatan.ambilJumlahTagihan(kegiatan, null, mahasiswa, semester,
												pengaturanPembayaranBulanan);
										totalTagihan += (nilai != null ? nilai : 0.0);

										System.out.println("jenisKegiatan = " + jenisKegiatan + ", pengaturanPembayaranBulanan = "
												+ pengaturanPembayaranBulanan.toString() + " nilai =" + nilai + ", totalTagihan = " + totalTagihan);
									} catch (Exception e) {
										e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UtsDanUasCheckerHelper.java:308");
									}
								}
							}
						}
					}
				}
			}

			System.out.println("totalTagihan => " + totalTagihan + ", kode => " + kodes);

			if (kodes.isEmpty() || totalTagihan < 0.01) {
				return true;
			}

			Double sumCiciclan = mahasiswa.hitungTotalCicilanPembayaran(semester, tahap, kodes);
			if (sumCiciclan == null) {
				sumCiciclan = 0.0;
			}

			Boolean hasil = sumCiciclan.doubleValue() > 0.1;

			Double persen = 1.0;
			Double persenYgDibayar = 0.0;
			
			if (hasil) {
				try {
					persen = konfigurasi.getInfo2() == null || konfigurasi.getInfo2().trim().isEmpty() ? 1.0
							: Double.parseDouble(konfigurasi.getInfo2().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:337");}

				if (totalTagihan > 0) {
					persenYgDibayar = (sumCiciclan.doubleValue() * 100.0) / totalTagihan.doubleValue();
				}
				hasil = persen < persenYgDibayar;
			}

			System.out.println("sumCiciclan => " + sumCiciclan + ", hasil => " + hasil + ", persen syarat " + persen
					+ ", persen ygdibayar " + persenYgDibayar);

			if (persen < 0.01) {
				return true;
			}

			if (!hasil) {
				try {
					List<ItemBiaya> itemBiayas = ConstantValues.simpleList(
							session.createCriteria(ItemBiaya.class).add(Restrictions.in("kode", kodes)), ItemBiaya.class);

					// OPTIMASI: Rebuild String menggunakan StringBuilder untuk efisiensi memori (menggantikan +=)
					StringBuilder itemB = new StringBuilder();
					if (itemBiayas != null) {
						boolean isFirst = true;
						for (ItemBiaya biaya : itemBiayas) {
							if (!isFirst) {
								itemB.append(", dan ");
							}
							itemB.append(biaya.getNama());
							isFirst = false;
						}
					}
					String item = itemB.toString();

					StringBuilder warningB = new StringBuilder();
					warningB.append("Mahasiswa dengan NIM \"").append(mahasiswa.getNim()).append("\"\n")
							.append("belum dapat mencetak kartu ujian karena belum melunasi item biaya \"").append(item).append("\"");

					if (persen > 2.0) {
						warningB.append(" sebesar ").append(Common.numberFormat.get().format(persen)).append("%");
					}

					String warning = warningB.toString();

					if (listener != null) {
						MyMessageboxConfig.showFormatCb(
								"{V1}\n\nApakah Bapak/Ibu berkenan untuk tetap melanjutkan pencetakan kartu ujian?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, listener, warning);
					} else {
						MyMessageboxConfig.show(warning, "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UtsDanUasCheckerHelper.java:389");
				}
			}

			return hasil;
			
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/UtsDanUasCheckerHelper.java:396");
			return true;
		} finally {
			if (session != null) {
				try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:400");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:401");}
				try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/UtsDanUasCheckerHelper.java:402");}
			}
		}
	}

	/** Seperti {@link #checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa, Integer, Integer, boolean)} dengan {@code persetujuan=false}. Delegasi langsung ke {@link CommonHelperClass}, bukan implementasi lokal kelas ini. */
	public static Boolean checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Integer tahap) {
		return checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahap, false);
	}

	/**
	 * Mengecek syarat pembayaran sebelum mahasiswa boleh mengisi/mengubah KRS.
	 * Implementasi didelegasikan sepenuhnya ke
	 * {@link CommonHelperClass#checkPembayaranSebelumKRSSudahMemenuhi} — method ini
	 * hanya wrapper agar pemanggil dapat mengaksesnya lewat namespace checker UTS/UAS.
	 *
	 * @param mahasiswa   mahasiswa yang dicek
	 * @param semester    semester akademik
	 * @param tahap       tahap pembayaran
	 * @param persetujuan diteruskan apa adanya ke implementasi {@link CommonHelperClass}
	 * @return hasil dari {@link CommonHelperClass#checkPembayaranSebelumKRSSudahMemenuhi}
	 */
	public static Boolean checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Integer tahap,
			boolean persetujuan) {
		return CommonHelperClass.checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahap, persetujuan);
	}

	/**
	 * Mengecek syarat persentase pelunasan sebelum mahasiswa boleh mencetak kartu UAS,
	 * dengan ambang batas dari konfigurasi
	 * {@code batas_terendah_persen_pembayaran_boleh_cetak_kartu_uas} (reguler, default
	 * 99%) atau {@code ..._uas_sp} (semester pendek, default 0%). Untuk jalur reguler,
	 * bila syarat semester berjalan terpenuhi, method JUGA mengecek tagihan
	 * <b>semester berikutnya</b> ({@code semester + 1}) terhadap ambang batas terpisah
	 * {@code batas_terendah_persen_pembayaran_boleh_cetak_kartu_uas_plus_satu} (default
	 * 0% — efektif tidak mengecek) sebelum benar-benar mengizinkan cetak kartu UAS.
	 *
	 * @param mahasiswa      mahasiswa yang dicek
	 * @param semester       semester akademik yang dicek tagihannya
	 * @param semesterPendek status semester pendek; {@code null} berarti reguler
	 * @param listener       callback konfirmasi bila syarat tidak terpenuhi; boleh {@code null}
	 * @return {@code true} bila seluruh syarat (semester berjalan, dan bila berlaku semester berikutnya) terpenuhi
	 */
	public static Boolean checkPembayaranSebelumUASSudahMemenuhi(Mahasiswa mahasiswa, Integer semester,
			Integer semesterPendek, EventListener listener) {
		
		if (semesterPendek == null) {
			Konfigurasi konfig = Common.getKonfigurasi("batas_terendah_persen_pembayaran_boleh_cetak_kartu_uas",
					"99", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
					mahasiswa.getStatusAwalMahasiswa());
					
			String batasTerendahPersen = konfig != null && konfig.getNilai() != null ? konfig.getNilai() : "99";
			Double batas = 0.0;
			try {
				batas = Double.parseDouble(batasTerendahPersen.trim());
			} catch (Exception e) {
				return true;
			}

			boolean hasil = checkPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, batas, listener);

			if (!hasil) {
				return hasil;
			}

			Konfigurasi konfigPlusSatu = Common.getKonfigurasi("batas_terendah_persen_pembayaran_boleh_cetak_kartu_uas_plus_satu", 
					"0", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
					mahasiswa.getStatusAwalMahasiswa());
					
			batasTerendahPersen = konfigPlusSatu != null && konfigPlusSatu.getNilai() != null ? konfigPlusSatu.getNilai() : "0";
			batas = 0.0;
			try {
				batas = Double.parseDouble(batasTerendahPersen.trim());
			} catch (Exception e) {
				return true;
			}

			if (batas > 0.1) {
				hasil = checkPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester + 1, batas, listener);
			}

			return hasil;
			
		} else {
			Konfigurasi konfigSp = Common.getKonfigurasi("batas_terendah_persen_pembayaran_boleh_cetak_kartu_uas_sp", 
					"0", semester, mahasiswa.getTahunangkatan(), mahasiswa.getJurusan(), mahasiswa.getProgram(),
					mahasiswa.getStatusAwalMahasiswa());
					
			String batasTerendahPersen = konfigSp != null && konfigSp.getNilai() != null ? konfigSp.getNilai() : "0";
			Double batas = 0.0;
			try {
				batas = Double.parseDouble(batasTerendahPersen.trim());
			} catch (Exception e) {
				return true;
			}

			return checkPembayaranSebelumUjianSudahMemenuhi(mahasiswa, semester, batas, ConstantValues.PEMBAYARAN_SP, listener);
		}
	}
}
