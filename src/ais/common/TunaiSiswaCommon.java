package ais.common;


import ais.action.master.helper.KegiatanPersistenceHelper;
import ais.action.master.sekolah.util.DepositHelper;
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

/**
 * Helper transaksional untuk memproses pembayaran <b>tunai</b> siswa/calon siswa pada modul
 * keuangan sekolah AIS, meliputi pembuatan record {@link PembayaranSiswa} dari sekumpulan
 * {@link Tagihan} yang dipilih pada layar UI (ZKoss {@link Rows}), pencatatan opsional ke
 * {@link DepositSiswa} bila sebagian pembayaran dialokasikan sebagai deposit/titipan, serta
 * penyimpanan rincian biaya per baris lewat {@link PembayaranSiswa#saveDetail(Rows, Session)}.
 *
 * <p>
 * Kelas ini murni statis (tidak menyimpan state), berfungsi sebagai lapisan logika bisnis yang
 * dipanggil dari layar transaksi pembayaran tunai (mis. loket pembayaran sekolah) setelah
 * operator memilih tagihan mana yang dibayar dan menekan tombol simpan. Seluruh operasi database
 * dijalankan dalam beberapa transaksi Hibernate berurutan (pembayaran, lalu deposit bila ada, lalu
 * detail rincian biaya) memakai sesi Hibernate native yang sama, dan sesi tersebut selalu ditutup
 * di blok {@code finally} lewat {@code KegiatanPersistenceHelper#closeNativeSession}.
 * </p>
 *
 * <h2>Alur kerja {@link #onSave}</h2>
 * <ol>
 * <li>Validasi bahwa total nominal dari {@code rowsDetailBiaya} (dihitung lewat
 * {@link PembayaranSiswa#chekDetail(Rows)}) lebih dari nol; bila tidak, tampilkan pesan gagal
 * formal lewat {@link PesanFormalHelper#tampilkanGagal} dan kembalikan {@code null} tanpa
 * menyentuh database.</li>
 * <li>Batasi {@code tabungan} (nominal yang diambil dari tabungan siswa) agar tidak melebihi
 * total tagihan yang dipilih.</li>
 * <li>Tentukan "tagihan bulanan" acuan — diutamakan tagihan pertama yang memiliki nilai
 * {@code bulan} terisi; bila tidak ada satu pun, jatuh ke tagihan pertama dalam koleksi apa
 * adanya — dipakai untuk mengisi kolom bulan/tahun/tahunBulan pada {@link PembayaranSiswa}.</li>
 * <li>Susun teks {@code keterangan} gabungan dari seluruh tagihan yang dibayar (id, nama item
 * biaya, keterangan "ke-N" bila item dibayar bertahap, bulan, tahun).</li>
 * <li>Bila {@code calonSiswa} tidak diberikan tapi {@code siswa} memiliki referensi calon siswa,
 * ambil ulang entitas {@link CalonSiswa} tersebut lewat {@code ConstantValues.ambil}.</li>
 * <li>Bangun dan simpan entitas {@link PembayaranSiswa} baru dengan seluruh atribut di atas.</li>
 * <li>Bila {@code deposit} diberikan dan lebih dari {@code 0.1}, cari (atau buat baru)
 * {@link DepositSiswa} yang terasosiasi dengan pembayaran ini, lalu simpan/perbarui.</li>
 * <li>Simpan rincian per baris tagihan lewat {@link PembayaranSiswa#saveDetail}.</li>
 * </ol>
 *
 * <p>
 * Kegagalan pada blok penyimpanan ditangkap, dicetak ke stack trace, dan direkam lewat
 * {@code ais.common.ErrorAuditUtil#record} tanpa dilempar ulang — pemanggil tetap menerima objek
 * {@link PembayaranSiswa} yang dikembalikan (kemungkinan belum tersimpan penuh bila terjadi
 * galat di tengah proses) dan perlu memeriksa keberhasilannya sendiri bila diperlukan.
 * </p>
 */
public class TunaiSiswaCommon {

	/**
	 * Menyimpan pembayaran topup tunai tanpa mewajibkan adanya baris tagihan. Catatan operasional
	 * sekolah dan buku besar saldo dibuat dalam satu transaksi agar tidak pernah berbeda.
	 */
	public static DepositSiswa onSaveTopup(Siswa siswa, CalonSiswa calonSiswa, Double nominal,
			String validator, AkunPembayaranSiswa akunPembayaranSiswa, Date tanggalTransaksi) throws Exception {
		if (nominal == null || nominal.doubleValue() <= 0.1) {
			PesanFormalHelper.tampilkanGagal("topup tabungan siswa",
					"Nominal Topup belum diisi atau masih nol.",
					new String[] { "Isi nominal Topup lebih dari nol, lalu ulangi proses pembayaran." });
			return null;
		}
		if (siswa == null && calonSiswa != null && calonSiswa.getSiswa() != null) {
			siswa = calonSiswa.getSiswa();
		}
		if (siswa == null || akunPembayaranSiswa == null) {
			PesanFormalHelper.tampilkanGagal("topup tabungan siswa",
					"Data siswa atau cara pembayaran belum tersedia.",
					new String[] { "Pilih siswa dan cara pembayaran yang aktif, lalu ulangi proses Topup." });
			return null;
		}

		Date waktu = tanggalTransaksi == null ? ais.ui.util.WaktuUtil.getDate() : tanggalTransaksi;
		DepositSiswa depositSiswa = new DepositSiswa();
		depositSiswa.setSiswa(siswa);
		depositSiswa.setCalonSiswa(calonSiswa);
		depositSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
		depositSiswa.setSekolah(siswa.getSekolah());
		depositSiswa.setYayasan(siswa.getSekolah() == null ? null : siswa.getSekolah().getYayasan());
		depositSiswa.setInquiryPembayaran("000000");
		depositSiswa.setNominal(nominal);
		depositSiswa.setTanggalBayar(waktu);
		depositSiswa.setWaktu(waktu);
		depositSiswa.setValidator(validator);
		depositSiswa.setKeterangan("Topup Tabungan");

		Session session = null;
		try {
			session = HibernateUtil.currentNativeSession();
			session.getTransaction().begin();
			session.save(depositSiswa);
			session.flush();
			DepositHelper.catatTopupSiswa(session, siswa, calonSiswa, nominal, waktu, "TUNAI",
					String.valueOf(depositSiswa.getId()));
			session.getTransaction().commit();
			return depositSiswa;
		} catch (Exception e) {
			try {
				if (session != null && session.getTransaction().isActive()) {
					session.getTransaction().rollback();
				}
			} catch (Exception rollbackError) {
				ais.common.ErrorAuditUtil.record(rollbackError,
						"auto-audit rollback topup src/ais/common/TunaiSiswaCommon.java");
			}
			throw e;
		} finally {
			KegiatanPersistenceHelper.closeNativeSession(session);
		}
	}

	/**
	 * Memproses satu transaksi pembayaran tunai siswa/calon siswa: memvalidasi rincian biaya yang
	 * dipilih, membangun dan menyimpan record {@link PembayaranSiswa}, mencatat deposit opsional,
	 * dan menyimpan rincian per baris tagihan. Lihat penjelasan alur lengkap pada Javadoc kelas.
	 *
	 * @param siswa                siswa yang membayar; boleh {@code null} bila pembayaran atas
	 *                              nama calon siswa
	 * @param calonSiswa            calon siswa yang membayar (mis. saat proses pendaftaran/PSB);
	 *                              boleh {@code null} — bila {@code null} dan {@code siswa}
	 *                              memiliki referensi calon siswa, akan diambil otomatis
	 * @param tag                   koleksi {@link Tagihan} yang dibayarkan pada transaksi ini
	 * @param deposit               nominal yang dialokasikan sebagai deposit/titipan siswa, boleh
	 *                              {@code null}; hanya dicatat ke {@link DepositSiswa} bila lebih
	 *                              dari {@code 0.1}
	 * @param tabungan              nominal yang diambil dari tabungan manual siswa untuk menutup
	 *                              sebagian/seluruh tagihan, boleh {@code null}; dibatasi agar
	 *                              tidak melebihi total tagihan
	 * @param validator             identitas/kode operator yang memvalidasi pembayaran
	 * @param akunPembayaranSiswa   akun kas/bank tujuan pencatatan pembayaran tunai
	 * @param rowsDetailBiaya       baris-baris rincian biaya pada layar UI (ZKoss {@link Rows})
	 *                              yang sudah dicentang/dipilih operator untuk dibayar
	 * @param tanggalTransaski      tanggal transaksi pembayaran (dipakai untuk kolom tanggal dan
	 *                              tanggal bayar)
	 * @return record {@link PembayaranSiswa} yang dibuat dan disimpan, atau {@code null} bila
	 *         validasi awal gagal (tidak ada tagihan terpilih dengan nominal lebih dari nol)
	 * @throws Exception diteruskan apa adanya dari kegagalan di luar blok simpan-tangkap (mis.
	 *                    kegagalan pada {@link PembayaranSiswa#chekDetail(Rows)})
	 */
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
				session.saveOrUpdate(depositSiswa);
				DepositHelper.catatTopupSiswa(session, siswa, calonSiswa, deposit,
						pembayaranSiswa.getTanggal(), "TUNAI_PEMBAYARAN",
						String.valueOf(pembayaranSiswa.getId()));
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
