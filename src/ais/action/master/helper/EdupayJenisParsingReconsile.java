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
 * Implementasi {@link JenisParsingReconsile} untuk memparsing file rekonsiliasi pembayaran
 * Host-to-Host bergaya <b>Edupay</b> (payment gateway pihak ketiga): baris teks dipisah titik-koma
 * ({@code ;}), tanpa header, dengan posisi kolom TETAP (indeks 1 = waktu {@code yyyyMMddHHmmss},
 * 6 = kode transaksi, 9 = nilai nominal, 10 = nama, 11 = status 1/lainnya). Untuk setiap baris,
 * kelas ini: (1) menyimpan/memperbarui satu baris {@link RekonsiliasiHostToHost} (idempoten,
 * dicari lebih dulu berdasarkan kecocokan {@code keterangan} = baris mentah); (2) mencari
 * {@link LogHostToHost} yang cocok (kode + tanggal + {@code responseCode="00"} +
 * {@code transactionType=PAY}) sebagai log transaksi asal di sisi AIS; (3) bila status baris SUKSES,
 * menandai {@link CicilanPembayaran} terkait sebagai sudah terekonsiliasi, atau — bila cicilan
 * sukses tidak ditemukan — memindahkan baris dari {@link CicilanPembayaranGagal} ke sukses
 * ({@code copyCicilanPembayaranKeSukses}, lalu menghapus baris gagal); (4) bila status GAGAL,
 * kebalikannya: memindahkan {@link CicilanPembayaran} sukses menjadi
 * {@link CicilanPembayaranGagal} lalu menghapus baris sukses tersebut.
 *
 * <p>
 * Kegagalan parsing satu kolom (mis. format tanggal tidak sesuai, indeks kolom hilang) ditangkap
 * per-kolom dan hanya dilaporkan lewat {@code Common.tampilErrorJikaAdmin} — tidak menghentikan
 * pemrosesan baris tersebut maupun baris berikutnya, sehingga field yang gagal diparsing bisa
 * tertinggal {@code null} pada objek {@link RekonsiliasiHostToHost} yang tersimpan.
 * </p>
 *
 * <p>
 * <b>Catatan keamanan</b>: tidak ditemukan kredensial (API key, secret, user/password) tertanam
 * langsung di kelas ini. Kelas ini murni memparsing file lokal yang sudah diunduh/ditaruh
 * sebelumnya (lihat {@link ais.action.master.helper.util.ReconsilePembayaranHostToHostSyncrhonizerProcessor})
 * — tidak melakukan pemanggilan jaringan ke Edupay sendiri.
 * </p>
 */
public class EdupayJenisParsingReconsile implements JenisParsingReconsile {

	/** Format tanggal/waktu baku pada kolom waktu file rekonsiliasi Edupay ({@code yyyyMMddHHmmss}). */
	private SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

	/**
	 * Memparsing seluruh baris file lampiran {@code lampiranLain} (dibaca baris-demi-baris via
	 * {@link BigFile} agar aman untuk file besar) sebagai data rekonsiliasi Edupay, dan
	 * menyinkronkan status pembayaran cicilan mahasiswa terkait. Lihat javadoc kelas untuk uraian
	 * lengkap alur per baris.
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
