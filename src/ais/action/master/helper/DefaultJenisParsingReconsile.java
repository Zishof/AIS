package ais.action.master.helper;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.library.util.BigFile;
import ais.action.ws.util.ConstantUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.CicilanPembayaranGagal;
import ais.database.model.JenisRekonsiliasiHostToHost;
import ais.database.model.LogHostToHost;
import ais.database.model.RekonsiliasiHostToHost;
import ais.database.model.file.LampiranLain;

/**
 * Implementasi <b>default/baku</b> {@link JenisParsingReconsile} yang dipakai bila konfigurasi
 * {@code default_class_yang_digunakan_untuk_memproses_reconsile_pembayaran_host_to_host} tidak
 * diarahkan ke kelas lain (lihat
 * {@link ais.action.master.helper.util.ReconsilePembayaranHostToHostSyncrhonizerProcessor}).
 *
 * <p>
 * <b>Catatan</b>: isi/logika kelas ini identik baris-demi-baris dengan
 * {@link EdupayJenisParsingReconsile} — sama-sama memparsing file rekonsiliasi Host-to-Host
 * berformat teks dipisah titik-koma dengan posisi kolom tetap (indeks 1 = waktu
 * {@code yyyyMMddHHmmss}, 6 = kode transaksi, 9 = nilai, 10 = nama, 11 = status), lalu
 * menyinkronkan status {@link CicilanPembayaran}/{@link CicilanPembayaranGagal} terhadap
 * {@link LogHostToHost} yang cocok. Lihat javadoc {@link EdupayJenisParsingReconsile#parsing}
 * untuk uraian lengkap alur per baris — tidak diulang di sini karena isinya sama persis
 * (kemungkinan duplikasi kode historis; tidak diubah sesuai instruksi menjaga kode fungsional).
 * </p>
 *
 * <p>
 * <b>Catatan keamanan</b>: tidak ditemukan kredensial tertanam di kelas ini.
 * </p>
 */
public class DefaultJenisParsingReconsile implements JenisParsingReconsile {

	/** Format tanggal/waktu baku pada kolom waktu file rekonsiliasi ({@code yyyyMMddHHmmss}). */
	private SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

	/**
	 * Memparsing seluruh baris file lampiran {@code lampiranLain} sebagai data rekonsiliasi
	 * Host-to-Host dan menyinkronkan status pembayaran cicilan mahasiswa terkait. Identik dengan
	 * {@link EdupayJenisParsingReconsile#parsing(LampiranLain, JenisRekonsiliasiHostToHost)} —
	 * lihat javadoc method tersebut untuk uraian lengkap.
	 *
	 * @param lampiranLain               file mentah hasil rekonsiliasi yang akan diparsing
	 * @param jenisRekonsiliasiHostToHost jenis/kategori rekonsiliasi yang ditautkan ke setiap baris
	 *                                    {@link RekonsiliasiHostToHost} yang dibuat
	 * @throws Exception diteruskan dari kegagalan I/O saat membaca file atau kegagalan transaksi
	 *                    Hibernate
	 */
	@Override
	public void parsing(LampiranLain lampiranLain,
			JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost)
			throws Exception {
		// TODO Auto-generated method stub
		Session session = HibernateUtil.currentNativeSession();

		BigFile bigFile = new BigFile(lampiranLain.ambilFile().getAbsolutePath());
		Iterator<String> iterator = bigFile.iterator();
		while (iterator.hasNext()) {
			String data = iterator.next();
			String[] s = data.split(";");
			RekonsiliasiHostToHost rekonsiliasiHostToHost = (RekonsiliasiHostToHost) session
					.createCriteria(RekonsiliasiHostToHost.class)
					.add(Restrictions.eq("keterangan", data)).setMaxResults(1)
					.uniqueResult();
			if (rekonsiliasiHostToHost == null) {
				rekonsiliasiHostToHost = new RekonsiliasiHostToHost();
			}
			rekonsiliasiHostToHost.setKeterangan(data);
			rekonsiliasiHostToHost
					.setJenisRekonsiliasiHostToHost(jenisRekonsiliasiHostToHost);
			rekonsiliasiHostToHost.setLampiranId(lampiranLain.getId());

			try {
				rekonsiliasiHostToHost.setWaktu(format.parse(s[1].trim()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			try {
				rekonsiliasiHostToHost.setKode(s[6].trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			try {
				rekonsiliasiHostToHost.setNama(s[10].trim());
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			try {
				rekonsiliasiHostToHost
						.setNilai(Double.parseDouble(s[9].trim()));
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			try {
				rekonsiliasiHostToHost
						.setStatus(Integer.parseInt(s[11].trim()) == 1 ? RekonsiliasiHostToHost.SUKSES
								: RekonsiliasiHostToHost.GAGAL);
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e); 
			}

			String nim = rekonsiliasiHostToHost.getKode().substring(0,
					rekonsiliasiHostToHost.getKode().length() - 2);

			LogHostToHost logHostToHost = (LogHostToHost) session
					.createCriteria(LogHostToHost.class)
					.add(Restrictions.eq("responseCode", "00"))
					.add(Restrictions.eq("transactionType", ConstantUtil.PAY))
					.add(Restrictions.eq("kode",
							rekonsiliasiHostToHost.getKode()))
					.add(Restrictions
							.sqlRestriction("DATE(this_.tanggal) = DATE('"
									+ Common.databaseDateFormat.get()
											.format(rekonsiliasiHostToHost
													.getWaktu()) + "')"))
					.setMaxResults(1).uniqueResult();

			rekonsiliasiHostToHost.setLogHostToHost(logHostToHost);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, rekonsiliasiHostToHost);
			session.getTransaction().commit();

			if (logHostToHost != null) {
				logHostToHost.setRekonsiliasiHostToHost(rekonsiliasiHostToHost);
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, logHostToHost);
				session.getTransaction().commit();

				List<CicilanPembayaran> cicilanPembayarans = Common
						.ambilCicilanPembayarans(session, logHostToHost,
								rekonsiliasiHostToHost.getKode(), nim,
								rekonsiliasiHostToHost.getWaktu());

				if (rekonsiliasiHostToHost.getStatus() != null
						&& rekonsiliasiHostToHost.getStatus().equals(
								RekonsiliasiHostToHost.SUKSES)) {

					if (!cicilanPembayarans.isEmpty()) {
						for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
							cicilanPembayaran
									.setRekonsiliasiHostToHost(rekonsiliasiHostToHost);
							session.getTransaction().begin();
							Common.refreshUpdate(session, cicilanPembayaran);
							session.getTransaction().commit();
						}
					} else {

						List<CicilanPembayaranGagal> cicilanPembayaranGagals = Common
								.ambilCicilanPembayaranGagals(session,
										logHostToHost,
										rekonsiliasiHostToHost.getKode(), nim,
										rekonsiliasiHostToHost.getWaktu());

						for (CicilanPembayaranGagal cicilanPembayaranGagal : cicilanPembayaranGagals) {
							cicilanPembayaranGagal
									.setRekonsiliasiHostToHost(rekonsiliasiHostToHost);

							CicilanPembayaran cicilanPembayaran = Common
									.copyCicilanPembayaranKeSukses(cicilanPembayaranGagal);

							session.getTransaction().begin();
							Common.refreshSaveOrUpdate(cicilanPembayaran);
							session.getTransaction().commit();

							session.getTransaction().begin();
							session.createSQLQuery(
									"delete from cicilan_pembayaran_gagal where id="
											+ cicilanPembayaranGagal.getId())
									.executeUpdate();
							session.getTransaction().commit();
						}

					}
				} else {
					for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
						cicilanPembayaran
								.setRekonsiliasiHostToHost(rekonsiliasiHostToHost);

						CicilanPembayaranGagal cicilanPembayaranGagal = Common
								.copyCicilanPembayaranKeGagal(cicilanPembayaran);

						session.getTransaction().begin();
						Common.refreshSaveOrUpdate(session,
								cicilanPembayaranGagal);
						session.getTransaction().commit();

						session.getTransaction().begin();
						session.createSQLQuery(
								"delete from cicilan_pembayaran where id="
										+ cicilanPembayaran.getId())
								.executeUpdate();
						session.getTransaction().commit();

					}
				}
			}
		}

		HibernateUtil.closeSession();
	}
}
