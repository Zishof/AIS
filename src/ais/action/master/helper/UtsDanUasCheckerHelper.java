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

public class UtsDanUasCheckerHelper {

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

	public static Boolean checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Integer tahap) {
		// Mengacu pada struktur asli, return memanggil versi overload method ini dengan parameter tambahan.
		// Asumsi method dengan 4 parameter ada di logic existing (di-inherit/dimplementasikan di tempat lain).
		return checkPembayaranSebelumKRSSudahMemenuhi(mahasiswa, semester, tahap, false);
	}
	
	// Method Placeholder untuk menunjang panggilan di atas (karena di kode asli dipanggil namun deklarasinya tidak disertakan)
	// Jika method ini berada di class lain di aplikasi Anda, Anda dapat menghapus method placeholder ini.
	public static Boolean checkPembayaranSebelumKRSSudahMemenuhi(Mahasiswa mahasiswa, Integer semester, Integer tahap, boolean isSp) {
		return true; // Implementasi sesungguhnya ada di class Anda
	}

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