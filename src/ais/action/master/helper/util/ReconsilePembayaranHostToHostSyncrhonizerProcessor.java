package ais.action.master.helper.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Blob;
import java.util.TimerTask;

import org.apache.commons.io.IOUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import ais.action.master.helper.DefaultJenisParsingReconsile;
import ais.action.master.helper.JenisParsingReconsile;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.JenisRekonsiliasiHostToHost;
import ais.database.model.Konfigurasi;
import ais.database.model.file.LampiranLain;

public class ReconsilePembayaranHostToHostSyncrhonizerProcessor extends TimerTask {

	@Override
	public void run() {
		doProcess();
	}

	@SuppressWarnings("deprecation")
	private void doProcess() {

		Konfigurasi aktifkan_auto_reconsile_biaya_host_to_host = Common
				.getKonfigurasi("aktifkan_auto_reconsile_biaya_host_to_host", Konfigurasi.TIDAK_AKTIF);

		System.out.println("============== ReconsilePembayaranHostToHostSyncrhonizerProcessor "
				+ aktifkan_auto_reconsile_biaya_host_to_host.getNilai() + " ============");

		if (aktifkan_auto_reconsile_biaya_host_to_host.getNilai().equals(Konfigurasi.AKTIF)) {
			File folder = new File(
					Common.getKonfigurasi("direktori_folder_tempat_file_auto_reconsile", "/opt").getNilai());
			String jenisFile = Common.getKonfigurasi("jenis_file_auto_reconsile", "csv").getNilai();
			Konfigurasi kelas = Common.getKonfigurasi(
					"default_class_yang_digunakan_untuk_memproses_reconsile_pembayaran_host_to_host",
					DefaultJenisParsingReconsile.class.getName());
			Session session = HibernateUtil.currentNativeSession();
			try {
				JenisParsingReconsile jenisParsingReconsile = (JenisParsingReconsile) Class.forName(kelas.getNilai())
						.newInstance();

				File[] files = folder.listFiles();
				if (files != null) {
					for (File file : files) {
						System.out.println("File reconsile " + file.getAbsolutePath());

						if (file.getName().toLowerCase().endsWith(jenisFile.toLowerCase())) {

							LampiranLain lainMahasiswa = null;
							try {

								folder = new File(
										Common.getKonfigurasi("lokasi_penyimpanan_file", "/opt/lampiran_file_lain")
												.getNilai() + "/hasil_reconsile/");
								if (!folder.exists()) {
									folder.mkdirs();
								}

								FileInputStream fileInputStream = new FileInputStream(file);

								File f = new File(folder.getAbsolutePath() + "/" + file.getName());

								f.createNewFile();
								FileOutputStream fileOutputStream = new FileOutputStream(f);
								IOUtils.copyLarge(fileInputStream, fileOutputStream);
								fileOutputStream.close();
								fileInputStream.close();

								lainMahasiswa = new LampiranLain();
								lainMahasiswa.setNama(file.getName());
								lainMahasiswa.setKeterangan(file.getAbsolutePath());
								lainMahasiswa.setJenis(LampiranLain.REKONSILIASI_HOST_TO_HOST);

								Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
								streamingSession.getTransaction().begin();
								streamingSession.save(lainMahasiswa);
								streamingSession.getTransaction().commit();

								try {
									Blob blob = new javax.sql.rowset.serial.SerialBlob(IOUtils.toByteArray(new FileInputStream(f)));
									lainMahasiswa.setFoto(blob);
									streamingSession.getTransaction().begin();
									streamingSession.update(lainMahasiswa);
									streamingSession.getTransaction().commit();

								} catch (Exception e) {
									Common.tampilErrorJikaAdmin(e);
								}

							} catch (Exception e) {
								StreamingHibernateUtil.getInstance().rollbackTransaction();
								Common.tampilErrorJikaAdmin(e);

							}

							StreamingHibernateUtil.getInstance().closeSession();

							if (lainMahasiswa != null) {
								JenisRekonsiliasiHostToHost jenisRekonsiliasiHostToHost = (JenisRekonsiliasiHostToHost) session
										.createCriteria(JenisRekonsiliasiHostToHost.class)
										.add(Restrictions.eq("namaKelas", kelas.getNilai())).setMaxResults(1)
										.uniqueResult();
								if (jenisRekonsiliasiHostToHost == null) {
									jenisRekonsiliasiHostToHost = new JenisRekonsiliasiHostToHost();
									jenisRekonsiliasiHostToHost.setNama(kelas.getNilai());
									jenisRekonsiliasiHostToHost.setNamaKelas(kelas.getNilai());
									Common.refreshSaveOrUpdate(jenisRekonsiliasiHostToHost);
								}
								jenisParsingReconsile.parsing(lainMahasiswa, jenisRekonsiliasiHostToHost);

								file.delete();
							}
						}
					}
				}

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Common.tampilErrorJikaAdmin(e);
			}

			HibernateUtil.closeSession();
		}
	}
}
