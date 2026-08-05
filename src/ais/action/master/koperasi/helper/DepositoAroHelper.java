package ais.action.master.koperasi.helper;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;

import ais.common.ConstantValues;
import ais.database.model.koperasi.DepositoRolloverKoperasi;
import ais.database.model.koperasi.TransaksiKoperasi;

/**
 * <h2>DepositoAroHelper — Mesin Otomasi ARO (Automatic Roll Over) Simpanan Berjangka</h2>
 *
 * <p>
 * Utilitas ini menjalankan otomasi <b>perpanjangan otomatis (ARO)</b> untuk simpanan berjangka
 * (deposito) koperasi. Deposito adalah simpanan bertenor; ketika jatuh tempo, deposito ber-ARO
 * diperpanjang otomatis untuk satu tenor berikutnya, sedangkan yang tidak ber-ARO ditandai jatuh
 * tempo agar dicairkan pengurus. Sebelumnya proses ini harus dipantau manual; kini dapat berjalan
 * sendiri lewat penjadwal (scheduler) maupun dipicu manual dari layar pemantauan.
 * </p>
 *
 * <h3>Apa yang dikerjakan {@link #proses(Session, Date)}</h3>
 * <ol>
 * <li><b>Pendaftaran (seeding).</b> Setiap {@link TransaksiKoperasi} simpanan berjangka (produk
 * bernama mengandung "berjangka"/"deposito" dan bertenor) yang belum memiliki catatan
 * {@link DepositoRolloverKoperasi} akan didaftarkan dengan tanggal jatuh tempo = tanggal setor +
 * tenor, dan ARO menyala secara bawaan.</li>
 * <li><b>Perpanjangan.</b> Catatan berstatus berjalan yang tanggal jatuh temponya sudah lewat: bila
 * ARO menyala, tanggal jatuh tempo digulung maju satu atau beberapa tenor hingga melewati hari ini
 * dan jumlah perpanjangan dicatat; bila ARO mati, statusnya menjadi jatuh tempo (menunggu
 * pencairan).</li>
 * </ol>
 *
 * <p>
 * Perpanjangan hanya menggeser tanggal jatuh tempo (menggulung pokok) dan <b>tidak mengubah nilai
 * akad</b> deposito; perhitungan bunga mengikuti mekanisme bunga simpanan yang sudah ada. Dengan
 * begitu otomasi ini aman dan tidak merusak data transaksi.
 * </p>
 *
 * <h3>Transaksi &amp; sesi</h3>
 * <p>
 * Metode {@link #proses(Session, Date)} melakukan operasi tulis melalui {@code Session} yang
 * diberikan pemanggil; <b>pengelolaan transaksi (commit) diserahkan ke pemanggil</b> agar cocok baik
 * untuk penjadwal (memakai {@code openSession()} + {@code Transaction}, ditutup di {@code finally})
 * maupun pemicu manual. Kelas final berkonstruktor privat, aman-null, dan kompatibel Java 1.7.
 * </p>
 *
 * @see DepositoRolloverKoperasi
 */
public final class DepositoAroHelper {

	/** Batas aman jumlah gulungan dalam satu proses (mencegah loop tak wajar pada data lama). */
	private static final int BATAS_GULUNGAN = 240;

	private DepositoAroHelper() {
		// util statis
	}

	/**
	 * Jalankan satu siklus otomasi ARO memakai {@code session} yang diberikan.
	 *
	 * @param session sesi Hibernate aktif (transaksi/commit dikelola pemanggil)
	 * @param now     tanggal acuan (biasanya waktu sekarang)
	 * @return ringkasan {@code [didaftarkan, diperpanjang, ditandaiJatuhTempo]}
	 */
	@SuppressWarnings("unchecked")
	public static int[] proses(Session session, Date now) {
		int didaftarkan = 0, diperpanjang = 0, jatuhTempo = 0;
		if (session == null) {
			return new int[] { 0, 0, 0 };
		}
		Date acuan = now == null ? new Date() : now;
		Long tipeSimpanan = ConstantValues.SIMPANAN != null ? ConstantValues.SIMPANAN.getId() : null;

		try {
			// (1) Pendaftaran deposito berjangka yang belum punya catatan rollover.
			if (tipeSimpanan != null) {
				Set<Long> sudahAda = new HashSet<Long>();
				List<Long> ids = session.createQuery(
						"select r.transaksiKoperasiId from DepositoRolloverKoperasi r "
								+ "where r.transaksiKoperasiId is not null").list();
				for (Long id : ids) {
					if (id != null) {
						sudahAda.add(id);
					}
				}

				List<TransaksiKoperasi> deposito = session.createQuery(
						"select t from TransaksiKoperasi t left join fetch t.produkKoperasi p "
								+ "where p.tipeProdukKoperasi.id = :tipe").setParameter("tipe", tipeSimpanan).list();
				for (TransaksiKoperasi t : deposito) {
					try {
						if (t == null || t.getId() == null || sudahAda.contains(t.getId())) {
							continue;
						}
						if (t.getProdukKoperasi() == null) {
							continue;
						}
						String nm = t.getProdukKoperasi().getNama() == null ? ""
								: t.getProdukKoperasi().getNama().toLowerCase();
						if (!(nm.contains("berjangka") || nm.contains("deposito"))) {
							continue;
						}
						int tenor = t.getProdukKoperasi().getJangkaWaktuBulan() == null ? 0
								: (int) Math.round(t.getProdukKoperasi().getJangkaWaktuBulan().doubleValue());
						Date setor = t.getTanggalTransaksi() != null ? t.getTanggalTransaksi() : t.getTanggal();
						if (tenor <= 0 || setor == null) {
							continue;
						}
						DepositoRolloverKoperasi r = new DepositoRolloverKoperasi();
						r.setTransaksiKoperasiId(t.getId());
						r.setJangkaWaktuBulan(tenor);
						r.setTanggalJatuhTempo(tambahBulan(setor, tenor));
						r.setAroOtomatis(true);
						r.setStatus(DepositoRolloverKoperasi.STATUS_BERJALAN);
						session.save(r);
						didaftarkan++;
					} catch (Exception e) {
						System.err.println("[DepositoAro] " + e.getMessage());
					}
				}
			}

			// (2) Proses catatan yang jatuh tempo.
			List<DepositoRolloverKoperasi> berjalan = session.createQuery(
					"from DepositoRolloverKoperasi r where r.status = :st "
							+ "and (r.aktif is null or r.aktif = true)")
					.setParameter("st", DepositoRolloverKoperasi.STATUS_BERJALAN).list();
			for (DepositoRolloverKoperasi r : berjalan) {
				try {
					Date jt = r.getTanggalJatuhTempo();
					if (jt == null || !jt.before(acuan)) {
						continue; // belum jatuh tempo
					}
					int tenor = r.getJangkaWaktuBulan();
					if (Boolean.TRUE.equals(r.getAroOtomatis()) && tenor > 0) {
						int guling = 0;
						Date baru = jt;
						while (baru.before(acuan) && guling < BATAS_GULUNGAN) {
							baru = tambahBulan(baru, tenor);
							guling++;
						}
						if (guling > 0) {
							r.setTanggalJatuhTempo(baru);
							r.setJumlahPerpanjangan(r.getJumlahPerpanjangan() + guling);
							r.setTanggalRolloverTerakhir(acuan);
							session.update(r);
							diperpanjang++;
						}
					} else {
						r.setStatus(DepositoRolloverKoperasi.STATUS_JATUH_TEMPO);
						session.update(r);
						jatuhTempo++;
					}
				} catch (Exception e) {
					System.err.println("[DepositoAro] " + e.getMessage());
				}
			}
		} catch (Exception e) {
			System.err.println("[DepositoAro] " + e.getMessage());
		}
		return new int[] { didaftarkan, diperpanjang, jatuhTempo };
	}

	/** Tambahkan sejumlah bulan ke sebuah tanggal. */
	private static Date tambahBulan(Date dasar, int bulan) {
		Calendar c = Calendar.getInstance();
		c.setTime(dasar);
		c.add(Calendar.MONTH, bulan);
		return c.getTime();
	}
}
