package ais.action.master.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFMergerUtility;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.surat.SuratKeluarAction;
import ais.action.master.surat.SuratMasukAction;
import ais.action.master.surat.util.SuratUtil;
import ais.action.report.Report;
import ais.action.servlet.Wa;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.CatatanAdministrasi;
import ais.database.model.DiskusiPengumumanAkademis;
import ais.database.model.JenisCatatanAdministrasi;
import ais.database.model.KelompokParameterTambahanCatatanAdministrasi;
import ais.database.model.Mahasiswa;
import ais.database.model.ParameterTambahan;
import ais.database.model.ParameterTambahanCatatanAdministrasi;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.PengumumanAkademis;
import ais.database.model.Tbmuser;
import ais.database.model.asset.JenisPerbaikanAsset;
import ais.database.model.asset.KelompokParameterTambahanPerbaikanAsset;
import ais.database.model.asset.ParameterTambahanPerbaikanAsset;
import ais.database.model.asset.PerbaikanAsset;
import ais.database.model.file.FotoGambarSuratKeluar;
import ais.database.model.file.FotoGambarSuratMasuk;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sop.AktorSop;
import ais.database.model.sop.AlurSop;
import ais.database.model.sop.DisposisiAlurSop;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;
import ais.delivery.email.sender.MailSender;

/**
 * Helper class khusus untuk menangani proses pengiriman Email, Broadcast, dan WhatsApp.
 * Dioptimalkan untuk efisiensi memori (StringBuilder & HashSet) dan kueri database.
 */
public class BroadcastHelper {

	@SuppressWarnings("unchecked")
	public static void kirimEmail(final DiskusiPengumumanAkademis diskusiPengumumanAkademis) {
		if (!diskusiPengumumanAkademis.getCatatan().trim().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						Tbmuser tbmuser = Common.getCurrentUser();
						Set<String> emailSet = new HashSet<String>();
						JSONArray userIds = new JSONArray();
						userIds.put(tbmuser.getUserId());
						
						if (tbmuser != null && tbmuser.getEmail() != null && Common.isValidEmailAddress(tbmuser.getEmail())) {
							emailSet.add(tbmuser.getEmail().trim());
						}

						String kores = diskusiPengumumanAkademis.getPengumumanAkademis().getKorespondensi().trim();
						if (!kores.isEmpty()) {
							List<String> emails = session.createCriteria(Tbmuser.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.in("userId", kores.split(",")))
									.setProjection(Projections.groupProperty("email")).list();
							for (String email : emails) {
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
							}
							for (String s : kores.split(",")) {
								if (!s.trim().isEmpty()) userIds.put(s.trim());
							}
						}

						// ProjectionList untuk efisiensi query database
						List<Object[]> mhsResults = session.createCriteria(DiskusiPengumumanAkademis.class)
								.add(Restrictions.eq("pengumumanAkademis", diskusiPengumumanAkademis.getPengumumanAkademis()))
								.createAlias("mahasiswa", "mahasiswa")
								.setProjection(Projections.projectionList()
										.add(Projections.groupProperty("mahasiswa.email"))
										.add(Projections.groupProperty("mahasiswa.nim"))).list();
						for (Object[] row : mhsResults) {
							String email = (String) row[0];
							String nim = (String) row[1];
							if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
							if (nim != null && !nim.trim().isEmpty()) userIds.put(nim.trim());
						}

						List<Object[]> siswaResults = session.createCriteria(DiskusiPengumumanAkademis.class)
								.add(Restrictions.eq("pengumumanAkademis", diskusiPengumumanAkademis.getPengumumanAkademis()))
								.createAlias("siswa", "siswa")
								.setProjection(Projections.projectionList()
										.add(Projections.groupProperty("siswa.alamatEmail"))
										.add(Projections.groupProperty("siswa.nomorIndukNasional"))).list();
						for (Object[] row : siswaResults) {
							String email = (String) row[0];
							String nin = (String) row[1];
							if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
							if (nin != null && !nin.trim().isEmpty()) userIds.put(nin.trim());
						}

						List<Object[]> tbmuserResults = session.createCriteria(DiskusiPengumumanAkademis.class)
								.add(Restrictions.eq("pengumumanAkademis", diskusiPengumumanAkademis.getPengumumanAkademis()))
								.createAlias("tbmuser", "tbmuser")
								.setProjection(Projections.projectionList()
										.add(Projections.groupProperty("tbmuser.email"))
										.add(Projections.groupProperty("tbmuser.userId"))).list();
						for (Object[] row : tbmuserResults) {
							String email = (String) row[0];
							String uid = (String) row[1];
							if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
							if (uid != null && !uid.trim().isEmpty()) userIds.put(uid.trim());
						}

						StringBuilder emailUserBuilder = new StringBuilder();
						for(String email : emailSet) {
							if(emailUserBuilder.length() > 0) emailUserBuilder.append(",");
							emailUserBuilder.append(email);
						}
						String emailUser = emailUserBuilder.toString();

						if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
							String subject = "Komentar pengumuman akademik => " + diskusiPengumumanAkademis.getPengumumanAkademis().getJudul();
							Mahasiswa mahasiswa = diskusiPengumumanAkademis.getMahasiswa();
							
							StringBuilder body = new StringBuilder();
							body.append("Komentar dari ").append(tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : (mahasiswa == null ? "" : mahasiswa.getNim() + " " + mahasiswa.getNama()));
							body.append(diskusiPengumumanAkademis.getCatatan()).append("<br><br>Isi pengumuman<hr>")
								.append(diskusiPengumumanAkademis.getPengumumanAkademis().getCatatan())
								.append("<br><br>Komentar Lainnya<hr>");

							List<DiskusiPengumumanAkademis> komentars = session.createCriteria(DiskusiPengumumanAkademis.class)
									.add(Restrictions.eq("pengumumanAkademis", diskusiPengumumanAkademis.getPengumumanAkademis())).list();
							body.append("<ul>");
							for (DiskusiPengumumanAkademis komentar : komentars) {
								Tbmuser tbmu = komentar.getTbmuser();
								Mahasiswa mhs = komentar.getMahasiswa();
								body.append("<li>").append(tbmu != null ? tbmu.getUserNama() + " (" + tbmu.getUserId() + ")" : (mhs == null ? "" : mhs.getNim() + " " + mhs.getNama()));
								body.append(" : ").append(komentar.getCatatan()).append(" ").append(Common.dateFormat.get().format(komentar.getTanggal())).append("</li>");
							}
							body.append("</ul><br><br><hr>Untuk informasi lebih lanjut bisa dilihat di ")
								.append(Common.getRequestHostWithProtocol()).append(", kemudian click pengumuman.<br><br>Terima Kasih");

							String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
							MailSender.sendMail(userIds, subject, body.toString(), sender, emailUser, diskusiPengumumanAkademis);
						}
					} finally {
						if (session != null) {
							try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:174");}
							try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:175");}
							try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:176");}
						}
					}
				}
			});
		}
	}

	public static void broadcastEmail(final PengumumanAkademis pengumumanAkademis) {
		if (!pengumumanAkademis.getCatatan().trim().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						Set<String> emailSet = new HashSet<String>();
						JSONArray userIds = new JSONArray();

						if (pengumumanAkademis.getBroadcastKeSiswaAktif()) {
							List<Object[]> results = session.createCriteria(Siswa.class)
									.add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
									.add(pengumumanAkademis.getSekolah() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("sekolah", pengumumanAkademis.getSekolah()))
									.add(pengumumanAkademis.getYayasan() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("yayasan", pengumumanAkademis.getYayasan()))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("alamatEmail"))
											.add(Projections.groupProperty("nomorIndukNasional"))).list();
							for (Object[] row : results) {
								String email = (String) row[0];
								String nin = (String) row[1];
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
								if (nin != null && !nin.trim().isEmpty()) userIds.put(nin.trim());
							}
						}

						if (pengumumanAkademis.getBroadcastKeMahasiswaAktif()) {
							List<Object[]> results = session.createCriteria(Mahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.isNull("statusKeluar")).createAlias("jurusan", "jurusan")
									.add(pengumumanAkademis.getFakultas() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan.fakultas", pengumumanAkademis.getFakultas()))
									.add(pengumumanAkademis.getJurusan() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", pengumumanAkademis.getJurusan()))
									.add(pengumumanAkademis.getProgram() == null || pengumumanAkademis.getProgram().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", pengumumanAkademis.getProgram()))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("email"))
											.add(Projections.groupProperty("nim"))).list();
							for (Object[] row : results) {
								String email = (String) row[0];
								String nim = (String) row[1];
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
								if (nim != null && !nim.trim().isEmpty()) userIds.put(nim.trim());
							}
						}

						if (pengumumanAkademis.getBroadcastKeMahasiswaCuti()) {
							List<Object[]> results = session.createCriteria(PendaftaranCutiMahasiswa.class)
									.add(Restrictions.eq("ganjilGenap", Common.isNowSemensterGanjil()
											? Perkuliahan.GANJIL : Perkuliahan.GENAP))
									.add(Restrictions.eq("tahunAkademik", Common.getCurrentTahunAkademik()))
									.add(Restrictions.eq("persetujuan", true)).createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")
									.add(pengumumanAkademis.getFakultas() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan.fakultas", pengumumanAkademis.getFakultas()))
									.add(pengumumanAkademis.getJurusan() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("mahasiswa.jurusan", pengumumanAkademis.getJurusan()))
									.add(pengumumanAkademis.getProgram() == null || pengumumanAkademis.getProgram().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("mahasiswa.program", pengumumanAkademis.getProgram()))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("mahasiswa.email"))
											.add(Projections.groupProperty("mahasiswa.nim"))).list();
							for (Object[] row : results) {
								String email = (String) row[0];
								String nim = (String) row[1];
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
								if (nim != null && !nim.trim().isEmpty()) userIds.put(nim.trim());
							}
						}

						if (pengumumanAkademis.getBroadcastKeMahasiswaAlumni()) {
							List<Object[]> results = session.createCriteria(Mahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("statusKeluar.id", 1L)).createAlias("jurusan", "jurusan")
									.add(pengumumanAkademis.getFakultas() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan.fakultas", pengumumanAkademis.getFakultas()))
									.add(pengumumanAkademis.getJurusan() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", pengumumanAkademis.getJurusan()))
									.add(pengumumanAkademis.getProgram() == null || pengumumanAkademis.getProgram().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", pengumumanAkademis.getProgram()))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("email"))
											.add(Projections.groupProperty("nim"))).list();
							for (Object[] row : results) {
								String email = (String) row[0];
								String nim = (String) row[1];
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
								if (nim != null && !nim.trim().isEmpty()) userIds.put(nim.trim());
							}
						}

						if (pengumumanAkademis.getBroadcastCalonMahasiswa()) {
							List<String> emails = session.createCriteria(BiodataCalonMahasiswa.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.add(Restrictions.eq("tahunAkademik", pengumumanAkademis.getTahunAjaran()))
									.createAlias("prodiLulus", "prodiLulus", Criteria.LEFT_JOIN)
									.add(pengumumanAkademis.getFakultas() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("prodiLulus.fakultas", pengumumanAkademis.getFakultas()))
									.add(pengumumanAkademis.getJurusan() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("prodiLulus", pengumumanAkademis.getJurusan()))
									.add(pengumumanAkademis.getProgram() == null || pengumumanAkademis.getProgram().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program", pengumumanAkademis.getProgram()))
									.setProjection(Projections.groupProperty("email")).list();
							for (String email : emails) {
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
							}
						}

						if (pengumumanAkademis.getBroadcastKeDosen()) {
							List<Object[]> results = session.createCriteria(Tbmuser.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createAlias("dosen", "dosen")
									.add(pengumumanAkademis.getFakultas() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("dosen.fakultas", pengumumanAkademis.getFakultas()))
									.add(pengumumanAkademis.getJurusan() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("dosen.jurusan", pengumumanAkademis.getJurusan()))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("dosen.email"))
											.add(Projections.groupProperty("userId"))).list();
							for (Object[] row : results) {
								String email = (String) row[0];
								String uid = (String) row[1];
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
								if (uid != null && !uid.trim().isEmpty()) userIds.put(uid.trim());
							}
						}

						if (pengumumanAkademis.getBroadcastKeGuru()) {
							List<Object[]> results = session.createCriteria(Tbmuser.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createAlias("guru", "guru")
									.add(pengumumanAkademis.getSekolah() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("guru.sekolah", pengumumanAkademis.getSekolah()))
									.add(pengumumanAkademis.getYayasan() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("guru.yayasan", pengumumanAkademis.getYayasan()))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("guru.alamatEmail"))
											.add(Projections.groupProperty("userId"))).list();
							for (Object[] row : results) {
								String email = (String) row[0];
								String uid = (String) row[1];
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
								if (uid != null && !uid.trim().isEmpty()) userIds.put(uid.trim());
							}
						}

						if (pengumumanAkademis.getBroadcastAdmin()) {
							List<Object[]> results = session.createCriteria(Tbmuser.class)
									.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
									.createAlias("program", "program", Criteria.LEFT_JOIN)
									.add(pengumumanAkademis.getProgram() == null || pengumumanAkademis.getProgram().trim().isEmpty() ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("program.nama", pengumumanAkademis.getProgram()))
									.add(pengumumanAkademis.getFakultas() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("fakultas", pengumumanAkademis.getFakultas()))
									.add(pengumumanAkademis.getJurusan() == null ? Restrictions.sqlRestriction("1=1") : Restrictions.eq("jurusan", pengumumanAkademis.getJurusan()))
									.add(Restrictions.isNull("dosen"))
									.setProjection(Projections.projectionList()
											.add(Projections.groupProperty("email"))
											.add(Projections.groupProperty("userId"))).list();
							for (Object[] row : results) {
								String email = (String) row[0];
								String uid = (String) row[1];
								if (email != null && Common.isValidEmailAddress(email)) emailSet.add(email.trim());
								if (uid != null && !uid.trim().isEmpty()) userIds.put(uid.trim());
							}
						}

						StringBuilder emailUserBuilder = new StringBuilder();
						for(String email : emailSet) {
							if(emailUserBuilder.length() > 0) emailUserBuilder.append(",");
							emailUserBuilder.append(email);
						}
						String emailUser = emailUserBuilder.toString();

						if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
							String subject = "Pengumuman akademik => " + pengumumanAkademis.getJudul();
							Tbmuser tbmuser = Common.getCurrentUser();
							StringBuilder body = new StringBuilder();
							body.append("Anda mendapatkan informasi pengumuman akademik \"").append(pengumumanAkademis.getJudul()).append("\" oleh ").append(tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "")
								.append("<br>Isi pengumuman<hr>").append(pengumumanAkademis.getCatatan())
								.append("<br><br>Komentar<hr>");

							List<DiskusiPengumumanAkademis> komentars = session.createCriteria(DiskusiPengumumanAkademis.class)
									.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).list();
							body.append("<ul>");
							for (DiskusiPengumumanAkademis komentar : komentars) {
								Tbmuser tbmu = komentar.getTbmuser();
								Mahasiswa mhs = komentar.getMahasiswa();
								body.append("<li>").append(tbmu != null ? tbmu.getUserNama() + " (" + tbmu.getUserId() + ")" : (mhs == null ? "" : mhs.getNim() + " " + mhs.getNama()));
								body.append(" : ").append(komentar.getCatatan()).append(" ").append(Common.dateFormat.get().format(komentar.getTanggal())).append("</li>");
							}
							body.append("</ul><br><br><hr>Untuk informasi lebih lanjut bisa dilihat di ").append(Common.getRequestHostWithProtocol()).append(", kemudian click pengumuman.<br><br>Terima Kasih");

							String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
							MailSender.sendMail(userIds, subject, body.toString(), sender, emailUser, pengumumanAkademis);
						}
					} finally {
						if (session != null) {
							try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:365");}
							try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:366");}
							try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:367");}
						}
					}
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	public static void kirimEmailCatatanAdministrasi(final CatatanAdministrasi catatanAdministrasi) throws Exception {
		if (!catatanAdministrasi.getKeterangan().trim().isEmpty() && catatanAdministrasi.getBroadcast()) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<String> emails = session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.in("userId", catatanAdministrasi.getKeterangan().trim().split(",")))
						.setProjection(Projections.groupProperty("email")).list();
				
				StringBuilder emailUserBuilder = new StringBuilder();
				for (String email : emails) {
					if (email != null && Common.isValidEmailAddress(email)) {
						if (emailUserBuilder.length() > 0) emailUserBuilder.append(",");
						emailUserBuilder.append(email.trim());
					}
				}
				String emailUser = emailUserBuilder.toString();
				JSONArray userIds = new JSONArray();

				for (String email : catatanAdministrasi.getKeterangan().trim().split(",")) {
					if (!email.trim().isEmpty()) userIds.put(email);
				}

				if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
					String subject = catatanAdministrasi.getNama();
					StringBuilder body = new StringBuilder();

					JenisCatatanAdministrasi j = catatanAdministrasi.getJenisCatatanAdministrasi();
					session.refresh(j);

					for (KelompokParameterTambahanCatatanAdministrasi kel : j.getKelompokParameterTambahanCatatanAdministrasis()) {
						List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
								session.createCriteria(ParameterTambahanCatatanAdministrasi.class)
										.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi", kel))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanCatatanAdministrasi", "kelompokParameterTambahanCatatanAdministrasi")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanCatatanAdministrasi.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);

						for (ParameterTambahan parameterTambahan : parameterTambahans) {
							String jenis = kel.getId() + "->" + parameterTambahan.getId();
							String val = "";
							String[] spl = catatanAdministrasi.getParameterTambahanInds().split("\n");
							for (String d : spl) {
								String[] value = d.split("<=>");
								if (value[0].trim().equalsIgnoreCase(jenis)) {
									val = value.length > 1 ? value[1].trim() : "";
								}
							}

							body.append("<br><strong>").append(parameterTambahan.getLabelInputan()).append("</strong>");
							LampiranLain lampiranLain = LampiranLain.ambil(catatanAdministrasi.getId(), jenis);
							if (lampiranLain != null) {
								String u = lampiranLain.getUrl();
								body.append("<br><a href='").append(u).append("' target='_blank'>").append(val.trim().isEmpty() ? u : val).append("</a>");
							} else {
								body.append("<br>").append(val);
							}
						}
					}
					String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
					MailSender.sendMail(userIds, subject, body.toString(), sender, emailUser, catatanAdministrasi);
				}
			} finally {
				if (session != null) {
					try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:444");}
					try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:445");}
					try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:446");}
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static void kirimEmailPerbaikanAsset(final PerbaikanAsset perbaikanAsset) throws Exception {
		if (!perbaikanAsset.getKeterangan().trim().isEmpty() && perbaikanAsset.getBroadcast()) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<String> emails = session.createCriteria(Tbmuser.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.in("userId", perbaikanAsset.getKeterangan().trim().split(",")))
						.setProjection(Projections.groupProperty("email")).list();
				
				StringBuilder emailUserBuilder = new StringBuilder();
				for (String email : emails) {
					if (email != null && Common.isValidEmailAddress(email)) {
						if (emailUserBuilder.length() > 0) emailUserBuilder.append(",");
						emailUserBuilder.append(email.trim());
					}
				}
				String emailUser = emailUserBuilder.toString();
				JSONArray userIds = new JSONArray();

				for (String email : perbaikanAsset.getKeterangan().trim().split(",")) {
					if (!email.trim().isEmpty()) userIds.put(email);
				}

				if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
					String subject = perbaikanAsset.getNama();
					StringBuilder body = new StringBuilder();

					JenisPerbaikanAsset j = perbaikanAsset.getJenisPerbaikanAsset();
					session.refresh(j);

					for (KelompokParameterTambahanPerbaikanAsset kel : j.getKelompokParameterTambahanPerbaikanAssets()) {
						List<ParameterTambahan> parameterTambahans = ConstantValues.simpleList(
								session.createCriteria(ParameterTambahanPerbaikanAsset.class)
										.add(Restrictions.eq("kelompokParameterTambahanPerbaikanAsset", kel))
										.createAlias("parameterTambahan", "parameterTambahan")
										.createAlias("kelompokParameterTambahanPerbaikanAsset", "kelompokParameterTambahanPerbaikanAsset")
										.add(Restrictions.eq("parameterTambahan.aktif", true))
										.add(Restrictions.eq("kelompokParameterTambahanPerbaikanAsset.aktif", true))
										.setProjection(Projections.groupProperty("parameterTambahan.id")),
								ParameterTambahan.class, false);

						for (ParameterTambahan parameterTambahan : parameterTambahans) {
							String jenis = kel.getId() + "->" + parameterTambahan.getId();
							String val = "";
							String[] spl = perbaikanAsset.getParameterTambahanInds().split("\n");
							for (String d : spl) {
								String[] value = d.split("<=>");
								if (value[0].trim().equalsIgnoreCase(jenis)) {
									val = value.length > 1 ? value[1].trim() : "";
								}
							}

							body.append("<br><strong>").append(parameterTambahan.getLabelInputan()).append("</strong>");
							LampiranLain lampiranLain = LampiranLain.ambil(perbaikanAsset.getId(), jenis);
							if (lampiranLain != null) {
								String u = lampiranLain.getUrl();
								body.append("<br><a href='").append(u).append("' target='_blank'>").append(val.trim().isEmpty() ? u : val).append("</a>");
							} else {
								body.append("<br>").append(val);
							}
						}
					}
					String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
					MailSender.sendMail(userIds, subject, body.toString(), sender, emailUser, perbaikanAsset);
				}
			} finally {
				if (session != null) {
					try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:521");}
					try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:522");}
					try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:523");}
				}
			}
		}
	}

	/** Tambah userId ke JSONArray penerima notifikasi HANYA bila belum ada (dedup, case-insensitive). */
	private static void tambahUserIdUnik(JSONArray userIds, String userId) {
		if (userId == null || userId.trim().isEmpty()) {
			return;
		}
		try {
			for (int i = 0; i < userIds.length(); i++) {
				if (userId.trim().equalsIgnoreCase(String.valueOf(userIds.get(i)))) {
					return;
				}
			}
			userIds.put(userId.trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:tambahUserIdUnik");
		}
	}

	public static void kirimEmailDisposisi(DisposisiAlurSop disposisiAlurSopSetelah) throws Exception {
		Set<String> emails = new HashSet<String>();
		JSONArray userIds = new JSONArray();
		DisposisiAlurSop sebelumnya = disposisiAlurSopSetelah.getSebelumnya();

		while (sebelumnya != null) {
			try {
				AlurSop alurSopSebelumnya = sebelumnya.getAlurSop();
				AktorSop aktorSopSebelumnya = alurSopSebelumnya != null ? alurSopSebelumnya.getAktorSop() : null;
				String usernamePenggunaSebelumnya = aktorSopSebelumnya != null ? aktorSopSebelumnya.getUsernamePengguna() : null;
				if (usernamePenggunaSebelumnya != null && !usernamePenggunaSebelumnya.trim().isEmpty()) {
					for (String email : usernamePenggunaSebelumnya.trim().split(",")) {
						if (!email.trim().isEmpty()) {
							userIds.put(email);
							Tbmuser tbmuser = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), email);
							if (tbmuser != null && tbmuser.getEmail() != null && !tbmuser.getEmail().trim().isEmpty()) {
								emails.add(tbmuser.getEmail());
							}
						}
					}
				} else {
					/* BUKAN error: AktorSop.usernamePengguna memang KOSONG untuk aktor berbasis
					 * ROLE/jabatan/atasan/kaprodi/dekan -- itu keadaan normal, dan penerimanya
					 * sudah diselesaikan secara dinamis oleh SopUtil.resolveAktor di bawah.
					 * Dulu kondisi wajar ini dicatat sebagai error (lengkap dengan Exception
					 * buatan) untuk SETIAP langkah pada SETIAP disposisi, sehingga membanjiri
					 * log dan menenggelamkan error yang sebenarnya. Cukup 1 baris informasi;
					 * kegagalan yang benar-benar perlu diketahui (tidak ada penerima sama
					 * sekali) dicatat sekali di akhir method ini. */
					System.out.println("BroadcastHelper: usernamePengguna aktor SOP kosong pada "
							+ "DisposisiAlurSop(sebelumnya) id=" + sebelumnya.getId()
							+ " -- penerima akan ditentukan lewat resolusi aktor dinamis.");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:545");}
			sebelumnya = sebelumnya.getSebelumnya();
		}

		if (disposisiAlurSopSetelah.getAlurSop() != null) {
			try {
				AktorSop aktorSopSetelah = disposisiAlurSopSetelah.getAlurSop().getAktorSop();
				String usernamePenggunaSetelah = aktorSopSetelah != null ? aktorSopSetelah.getUsernamePengguna() : null;
				if (usernamePenggunaSetelah != null && !usernamePenggunaSetelah.trim().isEmpty()) {
					for (String email : usernamePenggunaSetelah.trim().split(",")) {
						if (!email.trim().isEmpty()) {
							userIds.put(email);
							Tbmuser tbmuser = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), email);
							if (tbmuser != null && tbmuser.getEmail() != null && !tbmuser.getEmail().trim().isEmpty()) {
								emails.add(tbmuser.getEmail());
							}
						}
					}
				} else {
					// Lihat penjelasan pada blok "sebelumnya" di atas: kondisi ini normal untuk
					// aktor berbasis role/jabatan dan sudah ditangani resolusi aktor dinamis.
					System.out.println("BroadcastHelper: usernamePengguna aktor SOP kosong pada "
							+ "DisposisiAlurSop(setelah) id=" + disposisiAlurSopSetelah.getId()
							+ " -- penerima akan ditentukan lewat resolusi aktor dinamis.");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:560");}
		}

		// FIX MULTI-USER: konfigurasi statis AktorSop.usernamePengguna sering KOSONG untuk
		// aktor berbasis ROLE/jabatan/atasan/kaprodi/dekan — akibatnya penerima langkah
		// berikutnya tidak menerima notifikasi lonceng/email sama sekali (hanya tercatat
		// "dilewati" di audit). Resolusi dinamis di bawah memakai logika yang SAMA dengan
		// tampilan daftar aktor (SopUtil.resolveAktor) sehingga notifikasi selalu sampai
		// ke SEMUA aktor langkah berikutnya, apa pun basis penentuan aktornya.
		try {
			ais.database.model.sop.AlurSop alurSetelah = disposisiAlurSopSetelah.getAlurSop();
			if (alurSetelah != null) {
				ais.action.master.sop.helper.SopUtil.AktorResolusi resolusi =
						ais.action.master.sop.helper.SopUtil.resolveAktor(null, alurSetelah.getKhususUsername(),
								alurSetelah.getAktorSop() != null ? alurSetelah.getAktorSop().getJenisPengguna() : "",
								disposisiAlurSopSetelah.getDisposisiSop(), alurSetelah);
				for (Tbmuser aktor : resolusi.aktors) {
					tambahUserIdUnik(userIds, aktor.getUserId());
					if (aktor.getEmail() != null && !aktor.getEmail().trim().isEmpty()) {
						emails.add(aktor.getEmail());
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:resolveAktorSetelah");
		}

		// Sertakan pula PELAKU NYATA langkah-langkah sebelumnya (DiajukanOleh) — lebih
		// akurat daripada seluruh aktor terkonfigurasi, agar pengaju/pemroses sebelumnya
		// ikut mengetahui disposisi berjalan.
		try {
			DisposisiAlurSop s = disposisiAlurSopSetelah.getSebelumnya();
			while (s != null) {
				Tbmuser pelaku = s.getDiajukanOleh();
				if (pelaku != null) {
					tambahUserIdUnik(userIds, pelaku.getUserId());
					if (pelaku.getEmail() != null && !pelaku.getEmail().trim().isEmpty()) {
						emails.add(pelaku.getEmail());
					}
				}
				s = s.getSebelumnya();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:pelakuSebelumnya");
		}

		StringBuilder emailUserBuilder = new StringBuilder();
		for (String email : emails) {
			if (email != null && Common.isValidEmailAddress(email)) {
				if (emailUserBuilder.length() > 0) emailUserBuilder.append(",");
				emailUserBuilder.append(email.trim());
			}
		}
		String emailUser = emailUserBuilder.toString();

		if (emailUser.trim().isEmpty() && userIds.length() == 0) {
			/* INI kegagalan yang benar-benar perlu diketahui: setelah aktor terkonfigurasi,
			 * resolusi aktor dinamis, DAN pelaku langkah sebelumnya semuanya dicoba, tidak ada
			 * satu pun penerima -- artinya notifikasi lonceng/email disposisi ini TIDAK terkirim
			 * ke siapa pun. Sebelumnya kondisi ini lolos tanpa jejak sama sekali (yang tercatat
			 * justru kondisi wajar "usernamePengguna kosong"), sehingga SOP yang salah konfigurasi
			 * sulit ditemukan. */
			ais.common.ErrorAuditUtil.record(
					new Exception("Tidak ada penerima notifikasi disposisi SOP pada DisposisiAlurSop id="
							+ disposisiAlurSopSetelah.getId()),
					"kirimEmailDisposisi: notifikasi disposisi SOP tidak terkirim karena tidak ada aktor yang dapat "
							+ "diselesaikan - src/ais/action/master/helper/BroadcastHelper.java:kirimEmailDisposisi");
		}

		if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
			String catat = disposisiAlurSopSetelah.getAlurSop().getKeterangan() + " " + disposisiAlurSopSetelah.getKeterangan();
			// (A.1) Notifikasi aplikasi yang clickable + email (bila aktif), satu record.
			// Klik notifikasi membuka rincian disposisi SOP (lihat dispatch DisposisiAlurSop
			// pada lonceng MainAction/MainAction2).
			ais.common.CommonNotifikasi.disposisiSopMasuk(userIds, emailUser,
					disposisiAlurSopSetelah.getAlurSop().getSop().getNama(),
					disposisiAlurSopSetelah.getAlurSop().getNama(), disposisiAlurSopSetelah.getAlurSop().getKode(), catat,
					disposisiAlurSopSetelah, null, "/baru?p=pagesmastersoppengajuananda&s=index");
		}
	}

	@SuppressWarnings("unchecked")
	public static void kirimEmailSuratKeluar(SuratKeluar suratKeluar, AlurPersetujuanSuratKeluarStatus alurPersetujuanSuratKeluarStatus, Tbmuser user) throws Exception {
		Set<String> emails = new HashSet<String>();
		JSONArray userIds = new JSONArray();
		HashMap<String, String> telps = new HashMap<String, String>();
		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Iterator<String> enumeration = new JSONObject(suratKeluar.getJenisSurats()).keys();
			while (enumeration.hasNext()) {
				try {
					Long idJenis = Long.parseLong(enumeration.next());
					if (idJenis != null && !idJenis.equals(-1L)) {
						Pejabat pejabat = (Pejabat) ConstantValues.ambil(Pejabat.class.getName(), idJenis);
						if (pejabat != null) {
							if (pejabat.getPegawai() != null) {
								if (pejabat.getPegawai().getEmail() != null && !pejabat.getPegawai().getEmail().isEmpty()) emails.add(pejabat.getPegawai().getEmail());
								List<String> userids = session.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.setProjection(Projections.groupProperty("userId"))
										.add(Restrictions.eq("pegawai", pejabat.getPegawai())).list();
								for (String userid : userids) userIds.put(userid);
							}
							if (pejabat.getDosen() != null) {
								if (pejabat.getDosen().getEmail() != null && !pejabat.getDosen().getEmail().isEmpty()) emails.add(pejabat.getDosen().getEmail());
								List<String> userids = session.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.setProjection(Projections.groupProperty("userId"))
										.add(Restrictions.eq("dosen", pejabat.getDosen())).list();
								for (String userid : userids) userIds.put(userid);
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:618");}
			}

			if (suratKeluar.getBroadcast()) {
				for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
					Tbmuser tbmuser = (Tbmuser) o;
					try {
						if (tbmuser.getAktif() && tbmuser.getUserId() != null && suratKeluar.getUsernamePengguna().contains("," + tbmuser.getUserId() + ",")) {
							if (tbmuser.getPegawai() != null) {
								if (tbmuser.getPegawai().getHp() != null && !tbmuser.getPegawai().getHp().trim().isEmpty()) telps.put(tbmuser.getPegawai().getHp(), tbmuser.getPegawai().getNama());
								if (tbmuser.getPegawai().getTelp() != null && !tbmuser.getPegawai().getTelp().trim().isEmpty()) telps.put(tbmuser.getPegawai().getTelp(), tbmuser.getPegawai().getNama());
							}
							if (tbmuser.getDosen() != null) {
								if (tbmuser.getDosen().getHp() != null && !tbmuser.getDosen().getHp().trim().isEmpty()) telps.put(tbmuser.getDosen().getHp(), tbmuser.getDosen().getNama());
								if (tbmuser.getDosen().getTelp() != null && !tbmuser.getDosen().getTelp().trim().isEmpty()) telps.put(tbmuser.getDosen().getTelp(), tbmuser.getDosen().getNama());
							}
							if (tbmuser.getGuru() != null) {
								if (tbmuser.getGuru().getHp() != null && !tbmuser.getGuru().getHp().trim().isEmpty()) telps.put(tbmuser.getGuru().getHp(), tbmuser.getGuru().getNama());
								if (tbmuser.getGuru().getTeleponGuru() != null && !tbmuser.getGuru().getTeleponGuru().trim().isEmpty()) telps.put(tbmuser.getGuru().getTeleponGuru(), tbmuser.getGuru().getNama());
							}
							if (tbmuser.getHp() != null && !tbmuser.getHp().isEmpty()) telps.put(tbmuser.getHp(), tbmuser.getUserNama());
							if (tbmuser.getEmail() != null && !tbmuser.getEmail().isEmpty()) {
								emails.add(tbmuser.getEmail());
								userIds.put(tbmuser.getUserId());
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:644");}
				}
			}

			if (suratKeluar != null && suratKeluar.getId() != null) {
				try {
					if (suratKeluar.getPegawai() != null) {
						if (suratKeluar.getPegawai().getHp() != null && !suratKeluar.getPegawai().getHp().trim().isEmpty()) telps.put(suratKeluar.getPegawai().getHp(), suratKeluar.getPegawai().getNama());
						if (suratKeluar.getPegawai().getTelp() != null && !suratKeluar.getPegawai().getTelp().trim().isEmpty()) telps.put(suratKeluar.getPegawai().getTelp(), suratKeluar.getPegawai().getNama());
						if (suratKeluar.getPegawai().getEmail() != null && !suratKeluar.getPegawai().getEmail().isEmpty()) emails.add(suratKeluar.getPegawai().getEmail());
					}
					if (suratKeluar.getDosen() != null) {
						if (suratKeluar.getDosen().getHp() != null && !suratKeluar.getDosen().getHp().trim().isEmpty()) telps.put(suratKeluar.getDosen().getHp(), suratKeluar.getDosen().getNama());
						if (suratKeluar.getDosen().getTelp() != null && !suratKeluar.getDosen().getTelp().trim().isEmpty()) telps.put(suratKeluar.getDosen().getTelp(), suratKeluar.getDosen().getNama());
						if (suratKeluar.getDosen().getEmail() != null && !suratKeluar.getDosen().getEmail().isEmpty()) emails.add(suratKeluar.getDosen().getEmail());
					}
					if (suratKeluar.getGuru() != null) {
						if (suratKeluar.getGuru().getHp() != null && !suratKeluar.getGuru().getHp().trim().isEmpty()) telps.put(suratKeluar.getGuru().getHp(), suratKeluar.getGuru().getNama());
						if (suratKeluar.getGuru().getTeleponGuru() != null && !suratKeluar.getGuru().getTeleponGuru().trim().isEmpty()) telps.put(suratKeluar.getGuru().getTeleponGuru(), suratKeluar.getGuru().getNama());
						if (suratKeluar.getGuru().getAlamatEmail() != null && !suratKeluar.getGuru().getAlamatEmail().isEmpty()) emails.add(suratKeluar.getGuru().getAlamatEmail());
					}
					if (suratKeluar.getMahasiswa() != null) {
						if (suratKeluar.getMahasiswa().getTelp() != null && !suratKeluar.getMahasiswa().getTelp().trim().isEmpty()) telps.put(suratKeluar.getMahasiswa().getTelp(), suratKeluar.getMahasiswa().getNama());
						if (suratKeluar.getMahasiswa().getBiodataCalonMahasiswaData() != null) {
							if (suratKeluar.getMahasiswa().getBiodataCalonMahasiswaData().getHp() != null && !suratKeluar.getMahasiswa().getBiodataCalonMahasiswaData().getHp().trim().isEmpty()) telps.put(suratKeluar.getMahasiswa().getBiodataCalonMahasiswaData().getHp(), suratKeluar.getMahasiswa().getNama());
							if (suratKeluar.getMahasiswa().getBiodataCalonMahasiswaData().getTeleponRumah() != null && !suratKeluar.getMahasiswa().getBiodataCalonMahasiswaData().getTeleponRumah().trim().isEmpty()) telps.put(suratKeluar.getMahasiswa().getBiodataCalonMahasiswaData().getTeleponRumah(), suratKeluar.getMahasiswa().getNama());
						}
						if (suratKeluar.getMahasiswa().getEmail() != null && !suratKeluar.getMahasiswa().getEmail().isEmpty()) emails.add(suratKeluar.getMahasiswa().getEmail());
					}
					if (suratKeluar.getSiswa() != null) {
						for (String n : suratKeluar.getSiswa().ambilTelp()) telps.put(n, suratKeluar.getSiswa().getNama());
						if (suratKeluar.getSiswa().getAlamatEmail() != null && !suratKeluar.getSiswa().getAlamatEmail().isEmpty()) emails.add(suratKeluar.getSiswa().getAlamatEmail());
					}
					if (suratKeluar.getKonseptor() != null) {
						if (suratKeluar.getKonseptor().getHp() != null && !suratKeluar.getKonseptor().getHp().trim().isEmpty()) telps.put(suratKeluar.getKonseptor().getHp(), suratKeluar.getKonseptor().getUserNama());
						if (suratKeluar.getKonseptor().getEmail() != null && !suratKeluar.getKonseptor().getEmail().isEmpty()) emails.add(suratKeluar.getKonseptor().getEmail());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:681");}
			}

			List<Pejabat> pejabats = ConstantValues.simpleList(
					session.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
							.add(Restrictions.isNotNull("pejabat")).setProjection(Projections.groupProperty("pejabat.id"))
							.add(Restrictions.eq("suratKeluar", suratKeluar)),
					Pejabat.class, false);

			for (Pejabat pejabat : pejabats) {
				try {
					if (pejabat.getPegawai() != null) {
						if (pejabat.getPegawai().getHp() != null && !pejabat.getPegawai().getHp().trim().isEmpty()) telps.put(pejabat.getPegawai().getHp(), pejabat.getPegawai().getNama());
						if (pejabat.getPegawai().getTelp() != null && !pejabat.getPegawai().getTelp().trim().isEmpty()) telps.put(pejabat.getPegawai().getTelp(), pejabat.getPegawai().getNama());
						if (pejabat.getPegawai().getEmail() != null && !pejabat.getPegawai().getEmail().isEmpty()) emails.add(pejabat.getPegawai().getEmail());
						List<String> userids = session.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.groupProperty("userId"))
								.add(Restrictions.eq("pegawai", pejabat.getPegawai())).list();
						for (String userid : userids) userIds.put(userid);
					}
					if (pejabat.getDosen() != null) {
						if (pejabat.getDosen().getHp() != null && !pejabat.getDosen().getHp().trim().isEmpty()) telps.put(pejabat.getDosen().getHp(), pejabat.getDosen().getNama());
						if (pejabat.getDosen().getTelp() != null && !pejabat.getDosen().getTelp().trim().isEmpty()) telps.put(pejabat.getDosen().getTelp(), pejabat.getDosen().getNama());
						if (pejabat.getDosen().getEmail() != null && !pejabat.getDosen().getEmail().isEmpty()) emails.add(pejabat.getDosen().getEmail());
						List<String> userids = session.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.groupProperty("userId"))
								.add(Restrictions.eq("dosen", pejabat.getDosen())).list();
						for (String userid : userids) userIds.put(userid);
					}
					if (pejabat.getGuru() != null) {
						if (pejabat.getGuru().getHp() != null && !pejabat.getGuru().getHp().trim().isEmpty()) telps.put(pejabat.getGuru().getHp(), pejabat.getGuru().getNama());
						if (pejabat.getGuru().getTeleponGuru() != null && !pejabat.getGuru().getTeleponGuru().trim().isEmpty()) telps.put(pejabat.getGuru().getTeleponGuru(), pejabat.getGuru().getNama());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:716");}
			}

			StringBuilder emailUserBuilder = new StringBuilder();
			for (String email : emails) {
				if (email != null && Common.isValidEmailAddress(email)) {
					if (emailUserBuilder.length() > 0) emailUserBuilder.append(",");
					emailUserBuilder.append(email.trim());
				}
			}
			String emailUser = emailUserBuilder.toString();
			List<File> files = new ArrayList<File>();
			String fileAtt = null;

			try {
				Map<String, Object> parameters = SuratUtil.ubahIsiSuratKeluar(suratKeluar, null);
				for (int index = 1; index <= 15; index++) {
					try {
						LampiranLain lampiranLain = LampiranLain.ambil(suratKeluar.getKlasifikasiSuratKeluar().getId(),
								LampiranLain.FILE_JRXML_LAYOUT_SURAT + (index == 1 ? "" : "_" + index));
						if (lampiranLain != null && lampiranLain.getId() != null) {
							String t = lampiranLain.ambilFile().getAbsolutePath();
							File myfile = Report.generateCompileFileReport(Report.PDF, parameters, t, ais.ui.util.WaktuUtil.getDate());
							if (myfile != null && myfile.exists()) files.add(myfile);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:741");}
				}

				if (alurPersetujuanSuratKeluarStatus != null) {
					File f = SuratKeluarAction.cetakDisposisi(parameters, alurPersetujuanSuratKeluarStatus, false, user);
					if (f != null && f.exists()) files.add(f);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:748");}

			Session sSession = null;
			try {
				sSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
				List<FotoGambarSuratKeluar> fotoGambarSuratKeluars = suratKeluar == null || suratKeluar.getId() == null
						? new ArrayList<FotoGambarSuratKeluar>()
						: sSession.createCriteria(FotoGambarSuratKeluar.class)
								.add(Restrictions.eq("suratKeluar", suratKeluar.getId())).addOrder(Order.desc("id")).list();

				for (FotoGambarSuratKeluar fotoGambarSuratKeluar : fotoGambarSuratKeluars) {
					File file = fotoGambarSuratKeluar.ambilFile();
					if (file != null && file.exists()) files.add(file);
				}
			} finally {
				if (sSession != null) {
					try { if (sSession.isOpen()) sSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:764");}
					try { sSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:765");}
					try { if (sSession.isOpen()) sSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:766");}
				}
			}

			JSONArray attachmentsData = new JSONArray();
			try {
				fileAtt = Common.getRequestHostWithProtocolSimple() + Common.ROOT + "/Api?suratKeluar=" + URLEncoder.encode(("EE" + Common.desEncrypter.get().encrypt(suratKeluar.getId() + "")), "UTF-8");
				JSONObject attachment = new JSONObject();
				attachment.put("url", fileAtt);
				attachment.put("name", "Surat Keluar");
				attachmentsData.put(attachment);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:777");}

			if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
				String subject = suratKeluar.getNama() + " (" + suratKeluar.getKode() + ")";
				StringBuilder body = new StringBuilder("Yth. Bapak/Ibu/Sdr/i<br><br>Dengan hormat,<br><br>Bersama ini, kami memberitahukan bahwa Anda telah menerima "
						+ (alurPersetujuanSuratKeluarStatus != null ? "disposisi" : "") + " " + suratKeluar.getTipe() + " dengan detail sebagai berikut:"
						+ "<br>* <b>Nomor " + suratKeluar.getTipe() + ":</b> " + suratKeluar.getKode()
						+ "<br>* <b>Tanggal " + suratKeluar.getTipe() + ":</b> " + Common.dateFormat2.get().format(suratKeluar.getTanggal())
						+ "<br>* <b>Judul " + suratKeluar.getTipe() + ":</b> " + suratKeluar.getPerihal()
						+ (alurPersetujuanSuratKeluarStatus == null || alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan() == null ? "" : "<br>* <b>Tanggal Disposisi:</b> " + Common.dateFormat2.get().format(alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan()))
						+ (alurPersetujuanSuratKeluarStatus == null || alurPersetujuanSuratKeluarStatus.getPejabat() == null ? "" : "<br>* <b>Disposisi dari:</b> " + alurPersetujuanSuratKeluarStatus.getPejabat().getNama() + " (" + alurPersetujuanSuratKeluarStatus.getPejabat().getJenisJabatan().getNama() + ")"));

				body.append(alurPersetujuanSuratKeluarStatus != null ? "<br><br>Dimohon untuk segera menindaklanjuti disposisi tersebut sesuai dengan arahan yang tertera dalam " + suratKeluar.getTipe() + ".<br>" : "")
					.append("<br>Demikian notifikasi ini kami sampaikan, atas perhatian dan kerjasamanya kami ucapkan terima kasih.");

				String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
				// (C.1) Notifikasi aplikasi clickable (lonceng men-dispatch
				// AlurPersetujuanSuratKeluarStatus) + email (bila aktif), satu record,
				// mempertahankan isi surat yang sudah dirakit di atas.
				MailSender.simpanNotifikasiHalaman(userIds, emailUser, subject, body.toString(), sender,
						alurPersetujuanSuratKeluarStatus, null,
						"/baru?p=pagesmastersuratalurpersetujuansuratkeluarstatuszul&s=index",
						alurPersetujuanSuratKeluarStatus != null ? ais.common.CommonNotifikasi.STATUS_WARNING
								: ais.common.CommonNotifikasi.STATUS_INFO,
						false, false, false, files.toArray(new File[] {}));
			}

			if (Common.getKonfigurasi("aktifkan_kirim_notif_surat_ke_wa", "Aktif").getNilai().equalsIgnoreCase("Aktif") && !telps.isEmpty()) {
				String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal", "*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n").getNilai();
				File file = null;
				if (!files.isEmpty()) {
					PDFMergerUtility ut = new PDFMergerUtility();
					File f = null;
					for (File fs : files) {
						f = fs;
						// Sebagian lampiran PDF bisa TERENKRIPSI; PDFMergerUtility akan gagal dgn
						// "destination PDF is encrypted, can't append encrypted PDF documents".
						// Buka proteksinya lebih dulu (pdfSiapGabung) agar penggabungan sukses.
						if (f.getName().toLowerCase().endsWith("pdf")) ut.addSource(pdfSiapGabung(f));
					}
					if (f != null) {
						file = new File(f.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
						try {
							ut.setDestinationStream(new FileOutputStream(file));
							ut.mergeDocuments();
						} catch (Exception eMerge) {
							// Bila masih gagal (mis. PDF rusak), lewati lampiran gabungan agar
							// notifikasi tetap terkirim tanpa file.
							file = null;
						}
					}
				}

				for (String telp : telps.keySet()) {
					String nama = telps.get(telp);
					StringBuilder bodyWA = new StringBuilder("Yth. Bapak/Ibu/Sdr/i *" + nama + "*,\r\n\r\nDengan hormat,\r\n\r\nBersama ini, kami memberitahukan bahwa Anda telah menerima "
							+ (alurPersetujuanSuratKeluarStatus != null ? "disposisi" : "") + " " + suratKeluar.getTipe() + " dengan detail sebagai berikut:\n\n");
					bodyWA.append("-   *Nomor ").append(suratKeluar.getTipe()).append(":* ").append(suratKeluar.getKode()).append("\r\n")
						.append("-   *Tanggal ").append(suratKeluar.getTipe()).append(":* ").append(Common.dateFormat2.get().format(suratKeluar.getTanggal())).append("\r\n")
						.append("-   *Judul ").append(suratKeluar.getTipe()).append(":* ").append(suratKeluar.getPerihal()).append("\r\n")
						.append(alurPersetujuanSuratKeluarStatus == null || alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan() == null ? "" : "-   *Tanggal Disposisi:* " + Common.dateFormat2.get().format(alurPersetujuanSuratKeluarStatus.getWaktuPersetujuan()) + "\r\n")
						.append(alurPersetujuanSuratKeluarStatus == null || alurPersetujuanSuratKeluarStatus.getPejabat() == null ? "" : "-   *Disposisi dari:* " + alurPersetujuanSuratKeluarStatus.getPejabat().getNama() + " (" + alurPersetujuanSuratKeluarStatus.getPejabat().getJenisJabatan().getNama() + ")" + "\r\n")
						.append(alurPersetujuanSuratKeluarStatus != null ? "\nDimohon untuk segera menindaklanjuti disposisi tersebut sesuai dengan arahan yang tertera dalam " + suratKeluar.getTipe() + "." : "")
						.append("\n\nDemikian notifikasi ini kami sampaikan, atas perhatian dan kerjasamanya kami ucapkan terima kasih.\n\n\nLink ")
						.append(suratKeluar.getTipe()).append(" : ").append(fileAtt);

					String urlD = file != null ? Common.getRequestHostWithProtocolSimple() + file.getAbsolutePath().split("webapps")[1] : null;
					Wa.kirimWaViaUltramsg(telp, dawal + bodyWA.toString(), "Disposisi_Surat.pdf", urlD);
				}
			}

		} finally {
			if (session != null) {
				try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:850");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:851");}
				try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:852");}
			}
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * Mengembalikan berkas PDF yang SIAP digabung {@link PDFMergerUtility}. Bila PDF sumber
	 * terenkripsi, proteksinya dibuka (password kosong) lalu disimpan sebagai salinan
	 * tak-terenkripsi; salinan itulah yang dikembalikan. Selain itu berkas asli dikembalikan apa
	 * adanya. Mencegah IOException "destination PDF is encrypted, can't append encrypted PDF
	 * documents" saat penggabungan lampiran notifikasi.
	 */
	private static File pdfSiapGabung(File f) {
		if (f == null) {
			return f;
		}
		PDDocument doc = null;
		try {
			doc = PDDocument.load(f);
			if (doc.isEncrypted()) {
				try {
					doc.decrypt("");
				} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:875");
					// password non-kosong tak diketahui → tetap coba buang proteksi di bawah
				}
				doc.setAllSecurityToBeRemoved(true);
				File out = new File(f.getParentFile(), Common.getGeneratedBarCode() + "_dec.pdf");
				doc.save(out.getAbsolutePath());
				return out;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:883");
			// Gagal load/parse → pakai berkas asli; kegagalan merge sudah ditangani pemanggil.
		} finally {
			if (doc != null) {
				try {
					doc.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:889");
				}
			}
		}
		return f;
	}

	public static void kirimEmailSuratMasuk(SuratMasuk suratMasuk, AlurPersetujuanSuratMasukStatus alurPersetujuanSuratMasukStatus, Tbmuser user) throws Exception {
		Set<String> emails = new HashSet<String>();
		JSONArray userIds = new JSONArray();
		HashMap<String, String> telps = new HashMap<String, String>();
		Session session = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			Iterator<String> enumeration = new JSONObject(suratMasuk.getJenisSurats()).keys();
			while (enumeration.hasNext()) {
				try {
					Long idJenis = Long.parseLong(enumeration.next());
					if (idJenis != null && !idJenis.equals(-1L)) {
						Pejabat pejabat = (Pejabat) ConstantValues.ambil(Pejabat.class.getName(), idJenis);
						if (pejabat != null) {
							if (pejabat.getPegawai() != null) {
								if (pejabat.getPegawai().getEmail() != null && !pejabat.getPegawai().getEmail().isEmpty()) emails.add(pejabat.getPegawai().getEmail());
								List<String> userids = session.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.setProjection(Projections.groupProperty("userId"))
										.add(Restrictions.eq("pegawai", pejabat.getPegawai())).list();
								for (String userid : userids) userIds.put(userid);
							}
							if (pejabat.getDosen() != null) {
								if (pejabat.getDosen().getEmail() != null && !pejabat.getDosen().getEmail().isEmpty()) emails.add(pejabat.getDosen().getEmail());
								List<String> userids = session.createCriteria(Tbmuser.class)
										.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
										.setProjection(Projections.groupProperty("userId"))
										.add(Restrictions.eq("dosen", pejabat.getDosen())).list();
								for (String userid : userids) userIds.put(userid);
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:929");}
			}

			if (suratMasuk.getBroadcast()) {
				for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
					Tbmuser tbmuser = (Tbmuser) o;
					try {
						if (tbmuser.getAktif() && tbmuser.getUserId() != null && suratMasuk.getUsernamePengguna().contains("," + tbmuser.getUserId() + ",")) {
							if (tbmuser.getPegawai() != null) {
								if (tbmuser.getPegawai().getHp() != null && !tbmuser.getPegawai().getHp().trim().isEmpty()) telps.put(tbmuser.getPegawai().getHp(), tbmuser.getPegawai().getNama());
								if (tbmuser.getPegawai().getTelp() != null && !tbmuser.getPegawai().getTelp().trim().isEmpty()) telps.put(tbmuser.getPegawai().getTelp(), tbmuser.getPegawai().getNama());
							}
							if (tbmuser.getDosen() != null) {
								if (tbmuser.getDosen().getHp() != null && !tbmuser.getDosen().getHp().trim().isEmpty()) telps.put(tbmuser.getDosen().getHp(), tbmuser.getDosen().getNama());
								if (tbmuser.getDosen().getTelp() != null && !tbmuser.getDosen().getTelp().trim().isEmpty()) telps.put(tbmuser.getDosen().getTelp(), tbmuser.getDosen().getNama());
							}
							if (tbmuser.getGuru() != null) {
								if (tbmuser.getGuru().getHp() != null && !tbmuser.getGuru().getHp().trim().isEmpty()) telps.put(tbmuser.getGuru().getHp(), tbmuser.getGuru().getNama());
								if (tbmuser.getGuru().getTeleponGuru() != null && !tbmuser.getGuru().getTeleponGuru().trim().isEmpty()) telps.put(tbmuser.getGuru().getTeleponGuru(), tbmuser.getGuru().getNama());
							}
							if (tbmuser.getHp() != null && !tbmuser.getHp().isEmpty()) telps.put(tbmuser.getHp(), tbmuser.getUserNama());
							if (tbmuser.getEmail() != null && !tbmuser.getEmail().isEmpty()) {
								emails.add(tbmuser.getEmail());
								userIds.put(tbmuser.getUserId());
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:955");}
				}
			}

			List<Pejabat> pejabats = session.createCriteria(AlurPersetujuanSuratMasukStatus.class)
					.add(Restrictions.isNotNull("kodeUnik")).add(Restrictions.isNotNull("pejabat"))
					.setProjection(Projections.groupProperty("pejabat")).add(Restrictions.eq("suratMasuk", suratMasuk)).list();
					
			for (Pejabat pejabat : pejabats) {
				try {
					if (pejabat.getPegawai() != null) {
						if (pejabat.getPegawai().getHp() != null && !pejabat.getPegawai().getHp().trim().isEmpty()) telps.put(pejabat.getPegawai().getHp(), pejabat.getPegawai().getNama());
						if (pejabat.getPegawai().getTelp() != null && !pejabat.getPegawai().getTelp().trim().isEmpty()) telps.put(pejabat.getPegawai().getTelp(), pejabat.getPegawai().getNama());
						if (pejabat.getPegawai().getEmail() != null && !pejabat.getPegawai().getEmail().isEmpty()) emails.add(pejabat.getPegawai().getEmail());
						List<String> userids = session.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.groupProperty("userId"))
								.add(Restrictions.eq("pegawai", pejabat.getPegawai())).list();
						for (String userid : userids) userIds.put(userid);
					}
					if (pejabat.getDosen() != null) {
						if (pejabat.getDosen().getHp() != null && !pejabat.getDosen().getHp().trim().isEmpty()) telps.put(pejabat.getDosen().getHp(), pejabat.getDosen().getNama());
						if (pejabat.getDosen().getTelp() != null && !pejabat.getDosen().getTelp().trim().isEmpty()) telps.put(pejabat.getDosen().getTelp(), pejabat.getDosen().getNama());
						if (pejabat.getDosen().getEmail() != null && !pejabat.getDosen().getEmail().isEmpty()) emails.add(pejabat.getDosen().getEmail());
						List<String> userids = session.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.setProjection(Projections.groupProperty("userId"))
								.add(Restrictions.eq("dosen", pejabat.getDosen())).list();
						for (String userid : userids) userIds.put(userid);
					}
					if (pejabat.getGuru() != null) {
						if (pejabat.getGuru().getHp() != null && !pejabat.getGuru().getHp().trim().isEmpty()) telps.put(pejabat.getGuru().getHp(), pejabat.getGuru().getNama());
						if (pejabat.getGuru().getTeleponGuru() != null && !pejabat.getGuru().getTeleponGuru().trim().isEmpty()) telps.put(pejabat.getGuru().getTeleponGuru(), pejabat.getGuru().getNama());
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:989");}
			}

			StringBuilder emailUserBuilder = new StringBuilder();
			for (String email : emails) {
				if (email != null && Common.isValidEmailAddress(email)) {
					if (emailUserBuilder.length() > 0) emailUserBuilder.append(",");
					emailUserBuilder.append(email.trim());
				}
			}
			String emailUser = emailUserBuilder.toString();
			
			List<File> files = new ArrayList<File>();
			Session sSession = null;
			try {
				sSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
				List<FotoGambarSuratMasuk> fotoGambarSuratMasuks = suratMasuk == null || suratMasuk.getId() == null ? new ArrayList<FotoGambarSuratMasuk>()
						: sSession.createCriteria(FotoGambarSuratMasuk.class).add(Restrictions.eq("suratMasuk", suratMasuk.getId())).addOrder(Order.desc("id")).list();
				for (FotoGambarSuratMasuk fotoGambarSuratMasuk : fotoGambarSuratMasuks) {
					File file = fotoGambarSuratMasuk.ambilFile();
					if (file != null && file.exists()) files.add(file);
				}
			} finally {
				if (sSession != null) {
					try { if (sSession.isOpen()) sSession.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1013");}
					try { sSession.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1014");}
					try { if (sSession.isOpen()) sSession.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1015");}
				}
			}

			if (alurPersetujuanSuratMasukStatus != null) {
				File f = SuratMasukAction.cetakDisposisi(alurPersetujuanSuratMasukStatus, false, user);
				if (f != null && f.exists()) files.add(f);
			}

			if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
				String subject = suratMasuk.getNama() + " (" + suratMasuk.getKode() + ")";
				StringBuilder body = new StringBuilder("Yth. Bapak/Ibu/Sdr/i<br><br>Dengan hormat,<br><br>Bersama ini, kami memberitahukan bahwa Anda telah menerima disposisi surat dengan detail sebagai berikut:"
						+ "<br>* <b>Nomor Surat:</b> " + suratMasuk.getNoSurat()
						+ "<br>* <b>Nomor Agenda:</b> " + suratMasuk.getKode()
						+ "<br>* <b>Tanggal Surat:</b> " + Common.dateFormat2.get().format(suratMasuk.getTanggalSurat())
						+ "<br>* <b>Judul Surat:</b> " + suratMasuk.getPerihal()
						+ "<br>* <b>Surat Dari:</b> " + suratMasuk.getAsal()
						+ (alurPersetujuanSuratMasukStatus == null || alurPersetujuanSuratMasukStatus.getWaktuPersetujuan() == null ? "" : "<br>* <b>Tanggal Disposisi:</b> " + Common.dateFormat2.get().format(alurPersetujuanSuratMasukStatus.getWaktuPersetujuan()))
						+ (alurPersetujuanSuratMasukStatus == null || alurPersetujuanSuratMasukStatus.getPejabat() == null ? "" : "<br>* <b>Disposisi dari:</b> " + alurPersetujuanSuratMasukStatus.getPejabat().getNama() + " (" + alurPersetujuanSuratMasukStatus.getPejabat().getJenisJabatan().getNama() + ")"));
				body.append("<br><br>Dimohon untuk segera menindaklanjuti disposisi tersebut sesuai dengan arahan yang tertera dalam surat.<br><br>Demikian notifikasi ini kami sampaikan, atas perhatian dan kerjasamanya kami ucapkan terima kasih.");

				String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
				// (C.1) Notifikasi aplikasi clickable (lonceng men-dispatch
				// AlurPersetujuanSuratMasukStatus) + email (bila aktif), satu record.
				MailSender.simpanNotifikasiHalaman(userIds, emailUser, subject, body.toString(), sender,
						alurPersetujuanSuratMasukStatus, null,
						"/baru?p=pagesmastersuratalurpersetujuansuratmasukstatuszul&s=index",
						alurPersetujuanSuratMasukStatus != null ? ais.common.CommonNotifikasi.STATUS_WARNING
								: ais.common.CommonNotifikasi.STATUS_INFO,
						false, false, false, files.toArray(new File[] {}));
			}

			if (Common.getKonfigurasi("aktifkan_kirim_notif_surat_ke_wa", "Aktif").getNilai().equalsIgnoreCase("Aktif") && !telps.isEmpty()) {
				String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal", "*Pesan ini dibuat secara otomatis oleh sistem sebagai notifikasi/pemberitahuan kepada Anda*\n\n").getNilai();
				File file = null;
				if (!files.isEmpty()) {
					PDFMergerUtility ut = new PDFMergerUtility();
					File f = null;
					for (File fs : files) {
						f = fs;
						// Sebagian lampiran PDF bisa TERENKRIPSI; PDFMergerUtility akan gagal dgn
						// "destination PDF is encrypted, can't append encrypted PDF documents".
						// Buka proteksinya lebih dulu (pdfSiapGabung) agar penggabungan sukses.
						if (f.getName().toLowerCase().endsWith("pdf")) ut.addSource(pdfSiapGabung(f));
					}
					if (f != null) {
						file = new File(f.getParentFile().getAbsolutePath() + "/" + Common.getGeneratedBarCode() + ".pdf");
						try {
							ut.setDestinationStream(new FileOutputStream(file));
							ut.mergeDocuments();
						} catch (Exception eMerge) {
							// Bila masih gagal (mis. PDF rusak), lewati lampiran gabungan agar
							// notifikasi tetap terkirim tanpa file.
							file = null;
						}
					}
				}

				for (String telp : telps.keySet()) {
					String nama = telps.get(telp);
					StringBuilder bodyWA = new StringBuilder("Yth. Bapak/Ibu/Sdr/i *" + nama + "*,\r\n\r\nDengan hormat,\r\n\r\nBersama ini, kami memberitahukan bahwa Anda telah menerima disposisi surat dengan detail sebagai berikut:\n\n");
					bodyWA.append("-   *Nomor Surat:* ").append(suratMasuk.getNoSurat()).append("\r\n")
						.append("-   *Nomor Agenda:* ").append(suratMasuk.getKode()).append("\r\n")
						.append("-   *Tanggal Surat:* ").append(Common.dateFormat2.get().format(suratMasuk.getTanggalSurat())).append("\r\n")
						.append("-   *Judul Surat:* ").append(suratMasuk.getPerihal()).append("\r\n")
						.append("-   *Surat Dari:* ").append(suratMasuk.getAsal()).append("\r\n")
						.append(alurPersetujuanSuratMasukStatus == null || alurPersetujuanSuratMasukStatus.getWaktuPersetujuan() == null ? "" : "-   *Tanggal Disposisi:* " + Common.dateFormat2.get().format(alurPersetujuanSuratMasukStatus.getWaktuPersetujuan()) + "\r\n")
						.append(alurPersetujuanSuratMasukStatus == null || alurPersetujuanSuratMasukStatus.getPejabat() == null ? "" : "-   *Disposisi dari:* " + alurPersetujuanSuratMasukStatus.getPejabat().getNama() + " (" + alurPersetujuanSuratMasukStatus.getPejabat().getJenisJabatan().getNama() + ")\r\n")
						.append("\nDimohon untuk segera menindaklanjuti disposisi tersebut sesuai dengan arahan yang tertera dalam surat.\n\nDemikian notifikasi ini kami sampaikan, atas perhatian dan kerjasamanya kami ucapkan terima kasih.");

					String urlD = file != null ? Common.getRequestHostWithProtocolSimple() + file.getAbsolutePath().split("webapps")[1] : null;
					Wa.kirimWaViaUltramsg(telp, dawal + bodyWA.toString(), "Disposisi_Surat.pdf", urlD);
				}
			}
		} finally {
			if (session != null) {
				try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1091");}
				try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1092");}
				try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1093");}
			}
		}
	}

	public static void kirimEmailKeKorespondensi(final PengumumanAkademis pengumumanAkademis) {
		if (!pengumumanAkademis.getCatatan().trim().isEmpty() && !pengumumanAkademis.getKorespondensi().isEmpty()) {
			Common.createDefaultTimer(new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						List<String> emails = session.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.in("userId", pengumumanAkademis.getKorespondensi().trim().split(",")))
								.setProjection(Projections.groupProperty("email")).list();
						
						StringBuilder emailUserBuilder = new StringBuilder();
						for (String email : emails) {
							if (email != null && Common.isValidEmailAddress(email)) {
								if (emailUserBuilder.length() > 0) emailUserBuilder.append(",");
								emailUserBuilder.append(email.trim());
							}
						}
						String emailUser = emailUserBuilder.toString();
						JSONArray userIds = new JSONArray();
						for (String s : pengumumanAkademis.getKorespondensi().trim().split(",")) {
							if (!s.trim().isEmpty()) userIds.put(s.trim());
						}

						if (!emailUser.trim().isEmpty() || userIds.length() > 0) {
							String subject = "Korespondensi pengumuman akademik => " + pengumumanAkademis.getJudul();
							Tbmuser tbmuser = Common.getCurrentUser();
							StringBuilder body = new StringBuilder("Anda ditugaskan sebagai koresponsi pada pengumuman akademik \"")
									.append(pengumumanAkademis.getJudul()).append("\" oleh ").append(tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "")
									.append("<br>Isi pengumuman<hr>").append(pengumumanAkademis.getCatatan())
									.append("<br><br>Komentar<hr>");

							List<DiskusiPengumumanAkademis> komentars = session.createCriteria(DiskusiPengumumanAkademis.class)
									.add(Restrictions.eq("pengumumanAkademis", pengumumanAkademis)).list();
							body.append("<ul>");
							for (DiskusiPengumumanAkademis komentar : komentars) {
								Tbmuser tbmu = komentar.getTbmuser();
								Mahasiswa mhs = komentar.getMahasiswa();
								body.append("<li>").append(tbmu != null ? tbmu.getUserNama() + " (" + tbmu.getUserId() + ")" : (mhs == null ? "" : mhs.getNim() + " " + mhs.getNama()))
									.append(" : ").append(komentar.getCatatan()).append(" ").append(Common.dateFormat.get().format(komentar.getTanggal())).append("</li>");
							}
							body.append("</ul><br><br><hr>Untuk informasi lebih lanjut bisa dilihat di ").append(Common.getRequestHostWithProtocol()).append(", kemudian click pengumuman.<br><br>Terima Kasih");

							String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
							MailSender.sendMail(userIds, subject, body.toString(), sender, emailUser, pengumumanAkademis);
						}
					} finally {
						if (session != null) {
							try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1149");}
							try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1150");}
							try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/BroadcastHelper.java:1151");}
						}
					}
				}
			});
		}
	}
}
