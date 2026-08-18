package ais.common;


import ais.action.master.helper.KegiatanPersistenceHelper;
import java.util.Collection;
import java.util.Date;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Rows;

import ais.database.hibernate.HibernateUtil;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.MyMessageboxConfig;

public class TunaiSiswaCommon {

	public static PembayaranSiswa onSave(Siswa siswa, CalonSiswa calonSiswa, Collection<Tagihan> tag, Double deposit,
			Double tabungan, String validator, AkunPembayaranSiswa akunPembayaranSiswa, Rows rowsDetailBiaya,
			Date tanggalTransaski) throws Exception {

		Double amn = PembayaranSiswa.chekDetail(rowsDetailBiaya);
		if (amn.intValue() == 0) {
			PesanFormalHelper.tampilkanGagal("pembayaran tunai siswa",
					"Belum ada tagihan yang dipilih/dicentang pada daftar rincian biaya, sehingga sistem tidak "
							+ "dapat menentukan jumlah yang harus dibayarkan.",
					new String[] {
							"Silakan centang minimal satu baris tagihan pada daftar rincian biaya yang ingin dibayar.",
							"Pastikan nilai tagihan yang dipilih lebih dari nol sebelum menyimpan pembayaran." });
			return null;
		}

		if (tabungan != null && tabungan > amn) {
			tabungan = amn;
		}

		Tagihan tagihanBulanan = null;
		for (Tagihan tagihan : tag) {
			if (tagihan != null && tagihan.getBulan() != null) {
				tagihanBulanan = tagihan;
				break;
			}
		}

		if (tagihanBulanan == null) {
			for (Tagihan tagihan : tag) {
				tagihanBulanan = tagihan;
				break;
			}
		}

		StringBuilder keteranganBuilder = new StringBuilder();
		for (Tagihan tagihan : tag) {
			if (tagihan == null || tagihan.getItemBiayaSekolah() == null || tagihan.getNominalBiaya() == null) {
				continue;
			}
			keteranganBuilder.append(tagihan.getId()).append("-").append(tagihan.getItemBiayaSekolah().getNama())
					.append(tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")" : "")
					.append(tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
					.append(tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()).append(", ");
		}
		String keterangan = keteranganBuilder.toString();

		if (calonSiswa == null) {
			if (siswa != null && siswa.getCalonSiswa() != null) {
				calonSiswa = (CalonSiswa) ConstantValues.ambil(CalonSiswa.class.getName(), siswa.getCalonSiswa());
			}
		}

		PembayaranSiswa pembayaranSiswa = new PembayaranSiswa();
		pembayaranSiswa.setBulan(tagihanBulanan == null ? null : tagihanBulanan.getBulan());
		pembayaranSiswa.setTahun(tagihanBulanan == null ? null : tagihanBulanan.getTahun());
		pembayaranSiswa.setTahunDanBulan(tagihanBulanan == null ? null : tagihanBulanan.getTahunbulan());
		pembayaranSiswa.setSiswa(siswa);
		pembayaranSiswa.setCalonSiswa(calonSiswa);
		pembayaranSiswa.setJenisBiayaSekolah(tagihanBulanan == null ? null
				: tagihanBulanan.getPengaturanBiaya().getJenisBiayaSekolah());
		pembayaranSiswa.setTanggal(tanggalTransaski);
		pembayaranSiswa.setTanggalBayar(tanggalTransaski);
		pembayaranSiswa.setKeterangan(keterangan);
		pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
		pembayaranSiswa.setNominal(amn);
		pembayaranSiswa.setTambahanDeposit(amn);
		pembayaranSiswa.setValidator(validator);
		pembayaranSiswa.setDariTabunganManual(tabungan);
		pembayaranSiswa.setDariTabungan(tabungan);

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(pembayaranSiswa);
			session.getTransaction().commit();

			System.out.println("pembayaranSiswa -> " + pembayaranSiswa);

			if (deposit != null && deposit > 0.1) {
				DepositSiswa depositSiswa = (DepositSiswa) session.createCriteria(DepositSiswa.class)
						.add(Restrictions.eq("pembayaranSiswa", pembayaranSiswa)).setMaxResults(1).uniqueResult();
				if (depositSiswa == null) {
					depositSiswa = new DepositSiswa();
				}
				depositSiswa.setPembayaranSiswa(pembayaranSiswa);
				depositSiswa.setYayasan(pembayaranSiswa.getYayasan());
				depositSiswa.setSekolah(pembayaranSiswa.getSekolah());
				depositSiswa.setInquiryPembayaran("000000");
				depositSiswa.setNominal(deposit);
				depositSiswa.setSiswa(siswa);
				depositSiswa.setTanggalBayar(pembayaranSiswa.getTanggalBayar());
				depositSiswa.setWaktu(pembayaranSiswa.getTanggal());

				session.getTransaction().begin();
				session.save(depositSiswa);
				session.getTransaction().commit();
			}

			session.getTransaction().begin();
			pembayaranSiswa.saveDetail(rowsDetailBiaya, session);
			session.getTransaction().commit();

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/TunaiSiswaCommon.java:127");
		} finally {
			KegiatanPersistenceHelper.closeNativeSession(session);
		}

		return pembayaranSiswa;
	}
}
