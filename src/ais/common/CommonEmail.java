package ais.common;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.jsoup.Jsoup;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.report.Report;
import ais.action.servlet.Wa;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.CicilanPembayaran;
import ais.database.model.DetailKegiatan;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.MatakuliahPunyaBukuBahanAjar;
import ais.database.model.PengajuanIzinTidakMasukPerkuliahan;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.PertemuanPunyaDiskusi;
import ais.database.model.Tbmuser;
import ais.database.model.Tugas;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;
import ais.database.model.Ujian;
import ais.database.model.VOPembelajaran;
import ais.database.model.bni.BniRequest;
import ais.database.model.bri.BriRequest;
import ais.database.model.bsi.BsiRequest;
import ais.database.model.file.LampiranLain;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.finpay.FinpayRequest;
import ais.database.model.recruitment.CalonPegawai;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.MatapelajaranPunyaBukuBahanAjar;
import ais.database.model.sekolah.Siswa;
import ais.delivery.email.sender.MailSender;

public class CommonEmail {

	// ==========================================
	// UTILITY HELPER METHODS (Memory & Logic Optimized)
	// ==========================================

	private static void appendEmail(StringBuilder emailUser, String email) {
		if (email != null && Common.isValidEmailAddress(email.trim())) {
			if (emailUser.length() > 0) {
				emailUser.append(",");
			}
			emailUser.append(email.trim());
		}
	}

	@SuppressWarnings("unchecked")
	private static void processDosen(Session session, Collection<Dosen> dosens, JSONArray userIds, StringBuilder emailUser) {
		if (dosens == null || dosens.isEmpty()) return;
		for (Dosen dosen : dosens) {
			if (dosen != null) appendEmail(emailUser, dosen.getEmail());
		}
		List<String> emails = session.createCriteria(Tbmuser.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.in("dosen", dosens))
				.setProjection(Projections.groupProperty("userId")).list();
		for (String email : emails) userIds.put(email);
	}

	@SuppressWarnings("unchecked")
	private static void processGuru(Session session, Collection<Guru> gurus, JSONArray userIds, StringBuilder emailUser) {
		if (gurus == null || gurus.isEmpty()) return;
		for (Guru guru : gurus) {
			if (guru != null) appendEmail(emailUser, guru.getAlamatEmail());
		}
		List<String> emails = session.createCriteria(Tbmuser.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(Restrictions.in("guru", gurus))
				.setProjection(Projections.groupProperty("userId")).list();
		for (String email : emails) userIds.put(email);
	}

	private static void processMahasiswa(List<Mahasiswa> mahasiswas, JSONArray userIds, StringBuilder emailUser) {
		if (mahasiswas == null || mahasiswas.isEmpty()) return;
		for (Mahasiswa e : mahasiswas) {
			if (e != null) {
				if (e.getNim() != null) userIds.put(e.getNim());
				appendEmail(emailUser, e.getEmail());
			}
		}
	}

	private static void processSiswa(List<Siswa> siswas, JSONArray userIds, StringBuilder emailUser) {
		if (siswas == null || siswas.isEmpty()) return;
		for (Siswa e : siswas) {
			if (e != null) {
				if (e.getNomorIndukNasional() != null) userIds.put(e.getNomorIndukNasional());
				appendEmail(emailUser, e.getAlamatEmail());
			}
		}
	}

	private static void processPertemuanUsers(Session session, Pertemuan pertemuan, JSONArray userIds, StringBuilder emailUser) {
		if (pertemuan == null) return;
		processDosen(session, pertemuan.ambilDosen(), userIds, emailUser);
		processGuru(session, pertemuan.ambilGuru(), userIds, emailUser);
		processMahasiswa(pertemuan.ambilMahasiswa(), userIds, emailUser);
		processSiswa(pertemuan.ambilSiswa(), userIds, emailUser);
	}

	private static void processTbmUser(Tbmuser tbmuser, JSONArray userIds, StringBuilder emailUser) {
		if (tbmuser != null) {
			if (tbmuser.getUserId() != null) userIds.put(tbmuser.getUserId());
			appendEmail(emailUser, tbmuser.getEmail());
		}
	}

	private static String getWaktuSapaan() {
		int jam = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		if (jam >= 10 && jam < 15) return "Siang";
		if (jam >= 15 && jam < 18) return "Sore";
		if (jam >= 18 && jam <= 24) return "Malam";
		return "Pagi";
	}

	private static void closeHibernateSession(Session session) {
		if (session != null && session.isOpen()) {
			session.disconnect();
			session.close();
		}
		HibernateUtil.closeSession();
	}

	// ==========================================
	// MAIN METHODS
	// ==========================================

	public static void infoAdaFilePerkuliahan(final Pertemuan pertemuan, final PertemuanFileContent pertemuanFileContent) {
		if (pertemuan == null || pertemuanFileContent == null) return;
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				JSONArray userIds = new JSONArray();
				StringBuilder emailUser = new StringBuilder();
				
				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					processTbmUser(tbmuser, userIds, emailUser);
					processPertemuanUsers(session, pertemuan, userIds, emailUser);

					if (emailUser.length() > 0 || userIds.length() > 0) {
						String info = pertemuan.info();
						String subject = "Pemberitahuan Resmi: Penambahan Materi Perkuliahan untuk Pertemuan Ke-" + pertemuan.getPertemuanKe();
						String userName = tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "Sistem";
						
						String body = "Yth. Sivitas Akademika,<br><br>"
								+ "Melalui surel ini, kami menginformasikan bahwa terdapat penambahan atau pembaruan materi pembelajaran pada portal akademik Anda. Rincian materi tersebut adalah sebagai berikut:<br><br>"
								+ "<b>Pertemuan Ke:</b> " + pertemuan.getPertemuanKe() + "<br>"
								+ "<b>Diunggah Oleh:</b> " + userName + "<br>"
								+ "<b>Nama Dokumen:</b> " + pertemuanFileContent.getNama() + "<br>"
								+ "<b>Keterangan:</b> " + info + "<br><br>"
								+ "Kami mengimbau Anda untuk segera mengunduh dan mempelajari materi tersebut guna menunjang kelancaran proses kegiatan belajar mengajar. Silakan akses portal akademik pada tautan berikut: <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a>, kemudian navigasikan ke menu <b>Aktifitas Perkuliahan</b> untuk meninjau detail lampiran yang dimaksud.<br><br>"
								+ "Demikian informasi ini kami sampaikan. Atas perhatian dan kerja sama yang baik, kami ucapkan terima kasih.";
								
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), pertemuanFileContent);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:191");
				} finally {
					closeHibernateSession(session);
				}
			}
		});
	}

	public static void infoAdaTugasPerkuliahan(final Tugas tugas) {
		Pertemuan p = null;
		if (tugas instanceof Pertemuan) {
			p = (Pertemuan) tugas;
		} else if (tugas instanceof TugasPertemuan) {
			p = ((TugasPertemuan) tugas).ambilPertemuan();
		}
		final Pertemuan pertemuan = p;
		if (pertemuan != null) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Tbmuser tbmuser = Common.getCurrentUser();
					JSONArray userIds = new JSONArray();
					StringBuilder emailUser = new StringBuilder();

					Session session = null;
					try {
						session = HibernateUtil.currentNativeSession();
						processTbmUser(tbmuser, userIds, emailUser);
						processPertemuanUsers(session, pertemuan, userIds, emailUser);

						if (emailUser.length() > 0 || userIds.length() > 0) {
							String info = pertemuan.info();
							String userName = tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "Sistem";
							
							String subject = "Pemberitahuan Resmi: Penugasan Akademik Baru pada Topik Pembelajaran \"" + pertemuan.getTopik() + "\"";
							String body = "Yth. Mahasiswa/i,<br><br>"
									+ "Bersama surel ini kami sampaikan pemberitahuan resmi terkait adanya penugasan akademik terbaru yang wajib Anda kerjakan secara saksama. Rincian penugasan tersebut adalah sebagai berikut:<br><br>"
									+ "<b>Topik Pertemuan:</b> " + pertemuan.getTopik() + "<br>"
									+ "<b>Diberikan Oleh:</b> " + userName + "<br>"
									+ "<b>Detail Tugas:</b> " + info + "<br><br>"
									+ "Mohon untuk segera meninjau instruksi penugasan, batas waktu akhir (<i>deadline</i>) pengumpulan, serta kriteria penilaian yang telah ditetapkan oleh staf pengajar. Kegagalan dalam pengumpulan tugas tepat waktu dapat berdampak pada rekapitulasi penilaian akhir Anda.<br><br>"
									+ "Anda dapat mengakses informasi penugasan selengkapnya melalui portal E-Learning di tautan berikut: <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a>, silakan tuju ke menu <b>Aktifitas Perkuliahan</b>.<br><br>"
									+ "Harap maklum dan jadikan perhatian penuh. Selamat mengerjakan dan terima kasih.";
									
							String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
							MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), tugas);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:239");
					} finally {
						closeHibernateSession(session);
					}
				}
			});
		}
	}

	public static void infoAdaIzinAbsensi(final PengajuanIzinTidakMasukPerkuliahan pengajuanIzin) {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Pertemuan pertemuan = pengajuanIzin.getPertemuan();
				Tbmuser tbmuser = Common.getCurrentUser();
				JSONArray userIds = new JSONArray();
				StringBuilder emailUser = new StringBuilder();

				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					processTbmUser(tbmuser, userIds, emailUser);
					processPertemuanUsers(session, pertemuan, userIds, emailUser);

					if (emailUser.length() > 0 || userIds.length() > 0) {
						String info = pengajuanIzin.getPertemuan().getPerkuliahan().info();
						
						String subject = "Informasi Akademik: Pengajuan Izin Ketidakhadiran Perkuliahan pada Topik \"" + pengajuanIzin.getPertemuan().getTopik() + "\"";
						String body = "Yth. Bapak/Ibu Dosen dan Staf Akademik Terkait,<br><br>"
								+ "Sistem manajemen presensi mencatat adanya sebuah formulir pengajuan izin ketidakhadiran perkuliahan yang dikirimkan oleh mahasiswa. Berikut adalah ringkasan data dari pengajuan tersebut:<br><br>"
								+ "<b>Nama Mahasiswa:</b> " + pengajuanIzin.getMahasiswa().getNama() + "<br>"
								+ "<b>Status Kehadiran yang Diajukan:</b> " + Common.getBahasaConfig(pengajuanIzin.getStatusabsensi().getNama()) + "<br>"
								+ "<b>Alasan / Keterangan:</b> " + pengajuanIzin.getKeterangan() + "<br>"
								+ "<b>Informasi Perkuliahan:</b> " + info + "<br><br>"
								+ "Sebagai langkah tertib administrasi, mohon perkenan Bapak/Ibu untuk meninjau, melakukan verifikasi bukti pendukung (jika ada), dan memberikan keputusan persetujuan atas pengajuan absen tersebut di dalam sistem akademik terpadu. Silakan masuk pada tautan <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a> dan periksa secara detail pada menu <b>Aktifitas Perkuliahan</b>.<br><br>"
								+ "Demikian notifikasi pengajuan ini disampaikan. Atas waktu dan perhatian Bapak/Ibu, kami ucapkan terima kasih.";
								
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), pengajuanIzin);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:280");
				} finally {
					closeHibernateSession(session);
				}
			}
		});
	}

	public static void infoAdaTugasKelompokPerkuliahan(final TugasKelompok tugasKelompok) {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Pertemuan pertemuan = tugasKelompok.ambilPertemuan();
				Tbmuser tbmuser = Common.getCurrentUser();
				JSONArray userIds = new JSONArray();
				StringBuilder emailUser = new StringBuilder();

				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					processTbmUser(tbmuser, userIds, emailUser);
					processPertemuanUsers(session, pertemuan, userIds, emailUser);

					if (emailUser.length() > 0 || userIds.length() > 0) {
						// FIX NPE (CommonEmail$4.onEvent) - "pertemuan" boleh null (tugasKelompok
						// tidak selalu terkait Pertemuan; lihat processPertemuanUsers yang sudah
						// menjaga null di atas dan langsung return TANPA membatalkan pengiriman
						// email jika tbmuser sendiri sudah punya alamat email valid). Sebelumnya
						// "pertemuan.info()" dipanggil tanpa cek null sehingga meledak NPE walau
						// proses pengiriman email semestinya tetap bisa lanjut (pakai info dari
						// kelompok KKN/PKL kalau ada, atau placeholder aman kalau tidak ada sama sekali).
						String info = pertemuan != null ? pertemuan.info() : "-";
						if (tugasKelompok.getKelompokKkn() != null) {
							info = tugasKelompok.getKelompokKkn().infoSimple();
						} else if (tugasKelompok.getKelompokPkl() != null) {
							info = tugasKelompok.getKelompokPkl().infoSimple();
						}

						String userName = tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "Sistem";
						
						String subject = "Pemberitahuan Resmi: Penjadwalan dan Pembagian Tugas Kelompok Pembelajaran";
						String body = "Yth. Mahasiswa/i,<br><br>"
								+ "Bersama ini kami menginformasikan bahwa staf pengajar telah menerbitkan instruksi pengerjaan tugas akademik berbasis kerja kelompok. Rincian penugasan yang diberikan kepada Anda adalah sebagai berikut:<br><br>"
								+ "<b>Diberikan Oleh:</b> " + userName + "<br>"
								+ "<b>Deskripsi Singkat / Judul Penugasan:</b> " + tugasKelompok.getNama() + "<br>"
								+ "<b>Informasi Spesifik Kelompok:</b> " + info + "<br><br>"
								+ "Mengingat penugasan ini dinilai secara tim, kami menginstruksikan Anda agar secepatnya berkoordinasi secara efektif dengan seluruh rekan anggota kelompok. Rencanakan strategi penyelesaian, pembagian peran, serta batas tenggat waktu pelaporan dokumen tugas secara saksama.<br><br>"
								+ "Instruksi komprehensif, daftar rujukan materi, serta parameter penilaian kelulusan tugas kelompok dapat Anda tinjau lebih mendalam pada portal E-Learning di menu <b>Aktifitas Perkuliahan</b> melalui tautan berikut: <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a>.<br><br>"
								+ "Selamat berdiskusi, bekerja sama secara produktif, dan kami ucapkan terima kasih.";
								
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), tugasKelompok);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:327");
				} finally {
					closeHibernateSession(session);
				}
			}
		});
	}

	public static void infoAdaUjianPerkuliahan(final Pertemuan pertemuan, final Ujian ujian) {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				JSONArray userIds = new JSONArray();
				StringBuilder emailUser = new StringBuilder();

				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					processTbmUser(tbmuser, userIds, emailUser);
					processPertemuanUsers(session, pertemuan, userIds, emailUser);

					if (emailUser.length() > 0 || userIds.length() > 0) {
						String info = pertemuan.info();
						String userName = tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "Sistem";
						
						String subject = "Pemberitahuan Resmi: Penjadwalan Evaluasi/Ujian Akademik untuk Pertemuan Ke-" + pertemuan.getPertemuanKe();
						String body = "Yth. Mahasiswa/i yang kami banggakan,<br><br>"
								+ "Menindaklanjuti rencana evaluasi pembelajaran yang tercantum dalam silabus, bersama surel ini kami menyampaikan pengumuman resmi mengenai penjadwalan ujian akademik yang wajib Anda hadiri. Rincian ujian adalah sebagai berikut:<br><br>"
								+ "<b>Pertemuan Ke:</b> " + pertemuan.getPertemuanKe() + "<br>"
								+ "<b>Nama/Subjek Ujian:</b> " + ujian.getNama() + "<br>"
								+ "<b>Klasifikasi Ujian:</b> " + ujian.getJenis() + "<br>"
								+ "<b>Diterbitkan Oleh:</b> " + userName + "<br>"
								+ "<b>Informasi Tambahan Mata Kuliah:</b> " + info + "<br><br>"
								+ "Kami menekankan kepada Anda untuk senantiasa mempersiapkan materi pembelajaran dengan matang. Integritas dan kejujuran akademik adalah nilai utama dalam pelaksanaan setiap ujian di institusi ini.<br><br>"
								+ "Untuk meninjau tata tertib teknis pelaksanaan ujian, pedoman penilaian, serta mengakses tautan ruang ujian secara daring, silakan log masuk ke portal akademik melalui <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a> dan navigasikan ke menu <b>Aktifitas Perkuliahan</b>.<br><br>"
								+ "Kami mendoakan kelancaran bagi Anda dalam menghadapi ujian ini. Semoga sukses selalu. Terima kasih.";
								
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), ujian);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:369");
				} finally {
					closeHibernateSession(session);
				}
			}
		});
	}

	public static void infoAdaDiskusiPerkuliahan(final Pertemuan pertemuan, final PertemuanPunyaDiskusi pertemuanPunyaDiskusi) {
		if (Common.bolehKonfigurasi("email_diskusi_aktif", Konfigurasi.TIDAK_AKTIF)) {
			Common.createDefaultTimer(new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Tbmuser tbmuser = Common.getCurrentUser();
					JSONArray userIds = new JSONArray();
					StringBuilder emailUser = new StringBuilder();

					Session session = null;
					try {
						session = HibernateUtil.currentNativeSession();
						processTbmUser(tbmuser, userIds, emailUser);
						processPertemuanUsers(session, pertemuan, userIds, emailUser);

						if (emailUser.length() > 0 || userIds.length() > 0) {
							String isi = pertemuanPunyaDiskusi.getIsi();
							try {
								isi = Jsoup.parse(isi.replaceAll("\n", "<br>")).text();
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:397");
							}

							String info = VOPembelajaran.infoSimple(pertemuan);
							String userName = tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "Sistem";
							
							String subject = "Notifikasi Forum Akademik: Terdapat Interaksi/Tanggapan Baru pada Topik Diskusi Pertemuan Ke-" + pertemuan.getPertemuanKe();
							String body = "Yth. Sivitas Akademika,<br><br>"
									+ "Sistem pembelajaran terintegrasi kami memantau adanya partisipasi baru, tanggapan, atau interaksi gagasan di dalam forum diskusi mata kuliah Anda pada pertemuan ke-" + pertemuan.getPertemuanKe() + ". Rincian umpan balik diskusi tersebut adalah sebagai berikut:<br><br>"
									+ "<b>Topik Diskusi / Ruang Lingkup:</b> " + info + "<br>"
									+ "<b>Disampaikan Oleh:</b> " + (pertemuanPunyaDiskusi.getMahasiswa() != null ? pertemuanPunyaDiskusi.getMahasiswa().getNim() + " - " + pertemuanPunyaDiskusi.getMahasiswa().getNama() : (pertemuanPunyaDiskusi.getTbmuser() == null ? "" : pertemuanPunyaDiskusi.getTbmuser().getUserId())) + "<br>"
									+ "<b>Konten Pesan:</b><br>"
									+ "<i>\"" + isi + "\"</i><br><br>"
									+ "Diskusi forum daring merupakan wadah yang bernilai tinggi untuk saling bertukar argumen kritis dan memperluas wawasan akademis. Oleh karena itu, kami sangat mendorong Bapak/Ibu maupun para Mahasiswa/i untuk ikut serta menanggapi atau mempresentasikan pandangan guna menghidupkan ekosistem pembelajaran yang konstruktif.<br><br>"
									+ "Silakan langsung bertolak ke forum diskusi terkait melalui portal E-Learning di <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a>.<br><br>"
									+ "Terima kasih banyak atas dedikasi dan partisipasi aktif Anda di ruang pembelajaran akademik.";
									
							String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
							MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), pertemuanPunyaDiskusi);
						}
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:418");
					} finally {
						closeHibernateSession(session);
					}
				}
			});
		}
	}

	public static void infoAdaBukuAjar(final Perkuliahan perkuliahan, final MatakuliahPunyaBukuBahanAjar matakuliahPunyaBukuBahanAjar) {
		Common.createDefaultTimer(new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				JSONArray userIds = new JSONArray();
				StringBuilder emailUser = new StringBuilder();

				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					processTbmUser(tbmuser, userIds, emailUser);
					
					Collection<Dosen> dosenpa = perkuliahan.populateDosen().values();
					processDosen(session, dosenpa, userIds, emailUser);

					List<String> emails = session.createCriteria(Detailperkuliahan.class)
							.createAlias("mahasiswa", "mahasiswa")
							.setProjection(Projections.groupProperty("mahasiswa.email"))
							.add(Restrictions.ne("mahasiswa.email", "")).add(Restrictions.isNotNull("mahasiswa.email"))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

					for (String e : emails) {
						appendEmail(emailUser, e);
					}

					List<String> nims = session.createCriteria(Detailperkuliahan.class)
							.createAlias("mahasiswa", "mahasiswa").setProjection(Projections.groupProperty("mahasiswa.nim"))
							.add(Restrictions.eq("perkuliahan", perkuliahan)).list();

					for (String e : nims) {
						userIds.put(e);
					}

					if (emailUser.length() > 0) {
						String info = perkuliahan.info();
						String userName = tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "Sistem";
						
						String subject = "Pemberitahuan Resmi Akademik: Rekomendasi/Unggahan Buku Bahan Ajar dan Referensi Studi";
						String body = "Yth. Bapak/Ibu Dosen dan Mahasiswa/i Terdaftar,<br><br>"
								+ "Sebagai sarana fundamental dalam mengakselerasi pemahaman serta penunjang proses kegiatan belajar mengajar di lingkungan akademik, kami secara resmi mendistribusikan ketersediaan literatur atau referensi buku ajar studi terbaru dengan rincian berikut:<br><br>"
								+ "<b>Identitas Mata Kuliah/Perkuliahan:</b> " + info + "<br>"
								+ "<b>Judul Kepustakaan / Literatur Ajar:</b> " + (matakuliahPunyaBukuBahanAjar.getBukuBahanAjar() == null ? "Belum ada judul/metadata" : matakuliahPunyaBukuBahanAjar.getBukuBahanAjar().getNama()) + "<br>"
								+ "<b>Penambah Informasi/Diinformasikan Oleh:</b> " + userName + "<br><br>"
								+ "Anda dapat melihat pedoman silabus pembacaan, meninjau garis besar modul, maupun mengunduh ringkasan literatur tersebut (apabila staf dosen menyediakan versi lunaknya) secara mandiri melalui fasilitas kepustakaan daring. Akses portal akademik resmi pada tautan <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a>, lalu pilih opsi pada menu <b>Aktifitas Perkuliahan</b>.<br><br>"
								+ "Demikian pemberitahuan ini diutarakan demi terwujudnya efisiensi akademik. Mohon kerjasamanya guna mempergunakan dokumen tersebut selaras dengan regulasi integritas ilmiah (<i>fair use</i>). Atas perhatian seluruh pihak, kami ucapkan terima kasih.";
								
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), matakuliahPunyaBukuBahanAjar);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:479");
				} finally {
					closeHibernateSession(session);
				}
			}
		});
	}

	public static void infoAdaBukuAjar(final JadwalPelajaran jadwalPelajaran, final MatapelajaranPunyaBukuBahanAjar matapelajaranPunyaBukuBahanAjar) {
		Common.createDefaultTimer(new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				JSONArray userIds = new JSONArray();
				StringBuilder emailUser = new StringBuilder();

				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					processTbmUser(tbmuser, userIds, emailUser);
					
					Collection<Guru> gurus = jadwalPelajaran.populateGuru().values();
					processGuru(session, gurus, userIds, emailUser);

					List<String> emails = session.createCriteria(KelasSiswaPunyaSiswa.class)
							.createAlias("siswa", "siswa").setProjection(Projections.groupProperty("siswa.alamatEmail"))
							.add(Restrictions.ne("siswa.alamatEmail", "")).add(Restrictions.isNotNull("siswa.alamatEmail"))
							.add(Restrictions.eq("kelasSiswa", jadwalPelajaran.getKelas())).list();

					for (String e : emails) {
						appendEmail(emailUser, e);
					}

					if (emailUser.length() > 0) {
						String info = jadwalPelajaran.info();
						String userName = tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "Sistem";
						
						String subject = "Pengumuman Resmi Sekolah: Penyediaan Modul dan Referensi Buku Bahan Ajar";
						String body = "Yth. Bapak/Ibu Guru, Siswa/i yang kami hargai, serta Orang Tua/Wali,<br><br>"
								+ "Kami dari pihak penyelenggara satuan pendidikan memberitahukan tentang adanya penambahan materi bahan penunjang. Bertujuan untuk memaksimalkan dan menjaga stabilitas kualitas kegiatan belajar-mengajar bagi peserta didik, kami telah menyediakan rujukan literatur untuk mata pelajaran terkait:<br><br>"
								+ "<b>Informasi Kelas dan Mata Pelajaran:</b> " + info + "<br>"
								+ "<b>Judul Buku / Teks Pelajaran:</b> " + (matapelajaranPunyaBukuBahanAjar.getBukuBahanAjar() == null ? "Belum ada detail" : matapelajaranPunyaBukuBahanAjar.getBukuBahanAjar().getNama()) + "<br>"
								+ "<b>Diumumkan Oleh Staf/Pendidik:</b> " + userName + "<br><br>"
								+ "Silakan mempelajari arahan instruktur kelas bersangkutan atau mengelola akses terhadap rincian buku ajar tersebut dengan mengakses sistem portal administrasi akademik sekolah di <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a>, dan secara leluasa meninjau pada kategori/menu <b>Aktifitas Jadwal Pelajaran</b>.<br><br>"
								+ "Kami mengharapkan atensi dari para siswa untuk segera mempelajari literatur tambahan ini. Terima kasih atas pengertian dan perhatiannya.";
								
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), matapelajaranPunyaBukuBahanAjar);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:530");
				} finally {
					closeHibernateSession(session);
				}
			}
		});
	}

	public static void infoDaftarMahasiswa(final BiodataCalonMahasiswa biodataCalonMahasiswa, final File file) throws Exception {
		infoDaftarMahasiswaBanyakFile(biodataCalonMahasiswa, file);
	}

	public static void infoDaftarMahasiswaBanyakFile(final BiodataCalonMahasiswa biodataCalonMahasiswa, final File... file) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (biodataCalonMahasiswa != null) {
			appendEmail(emailUser, biodataCalonMahasiswa.getEmail());
			if (biodataCalonMahasiswa.getMahasiswa() != null) {
				userIds.put(biodataCalonMahasiswa.getMahasiswa().getNim());
			}

			String emailMonitoring = Common.getKonfigurasi("alamat_email_monitoring_regitrasi", "", null, 
					biodataCalonMahasiswa.getTahun(),
					biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1() : biodataCalonMahasiswa.getProdiLulus(),
					biodataCalonMahasiswa.getProgram(), biodataCalonMahasiswa.getStatusAwalMahasiswa()).getNilai();
			
			if (!emailMonitoring.trim().isEmpty()) {
				appendEmail(emailUser, emailMonitoring);
			}

			String code = biodataCalonMahasiswa.urlLogin();
			String kampus = (biodataCalonMahasiswa.getGelombangPendaftaran() == null || biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi() == null) 
					? "" : biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi().getNama();

			if (emailUser.length() > 0 || userIds.length() > 0) {
				String subject = "Pemberitahuan Resmi: Penerimaan Data Pendaftaran Calon Mahasiswa Baru " + kampus;
				
				String body = "Yth. Sdr/i. <b>" + biodataCalonMahasiswa.getNama() + "</b>,<br><br>"
						+ "Kami, mewakili panitia pelaksana penerimaan, mengucapkan selamat serta melontarkan apresiasi yang setinggi-tingginya atas langkah inisiatif, minat yang terarah, dan komitmen kuat Anda untuk mendaftarkan diri sebagai Calon Mahasiswa di <b>" + kampus + "</b>. Melalui surel resmi ini, kami mengonfirmasi bahwa pendaftaran dan penginputan data administratif tahap awal Anda telah terekam secara sempurna di dalam sistem pangkalan data institusi kami.<br><br>"
						+ "Perlu dipahami bahwa selesainya proses registrasi akun baru merupakan gerbang pembuka dari prosedur rekrutmen. Beragam tahapan krusial senantiasa menanti di depan, mulai dari tes kemampuan dasar, wawancara akademik (jika diwajibkan oleh program studi), serta validasi dokumen pendukung secara ketat.<br><br>"
						+ "Dalam rangka mencegah timbulnya hambatan diskualifikasi, kami mewajibkan Sdr/i untuk segera menyetorkan berkas dan menuntaskan segala komponen penunjang persyaratan, dengan mengacu pada panduan resmi di portal kami. Akses panel manajemen akun Anda melalui tautan eksklusif berikut: <a href='" + code + "' target='_blank'>" + code + "</a>. Segala dokumentasi pendaftaran dipastikan wajib diserahkan mendahului tenggat (<i>deadline</i>) penutupan.<br><br>"
						+ "Adalah suatu kehormatan kami dapat mengakomodasi ketertarikan Anda, dan kami tak sabar menyambut Anda sebagai insan akademis tangguh di jajaran kebanggaan keluarga besar " + kampus + ".<br><br>"
						+ "Semoga keseluruhan tahapan rekrutmen ini dapat Anda jalani dengan kesuksesan yang gilang-gemilang. Terima kasih atas ketelitian Anda dalam melengkapi prosedur ini.<br><br>"
						+ "Hormat Kami,<br>Tim Admisi dan Penerimaan Mahasiswa Baru<br>" + kampus;

				String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
				MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), biodataCalonMahasiswa, false, file);
			}

			if (Common.bolehKonfigurasi("aktifkan_kirim_notif_daftar_peserta_didik_baru_ke_wa")) {
				String from = biodataCalonMahasiswa.getHp();
				if (from == null || from.trim().isEmpty()) from = biodataCalonMahasiswa.getTeleponRumah();
				if (from == null || from.trim().isEmpty()) from = biodataCalonMahasiswa.getNoTelpOrtu();

				if (from != null && !from.trim().isEmpty() && !from.trim().equals("00000000000000000000") && !from.trim().equals("000000000")) {
					code = Common.getRequestHostWithProtocol() + "/pmb";
					
					String info = "Yth. Sdr/i. *" + biodataCalonMahasiswa.getNama() + "*,\n\n"
							+ "Kami menyampaikan ucapan selamat dan apresiasi setinggi-tingginya atas keputusan Anda mendaftar sebagai Calon Mahasiswa di *" + kampus + "*. Pendaftaran awal Anda telah sah terdata di dalam sistem penerimaan kami.\n\n"
							+ "Harap dipahami bahwa ini merupakan tahap pendahuluan. Terdapat kewajiban kelengkapan dokumentasi yang rigid serta proses penyaringan seleksi ketat di tahap-tahap berikutnya.\n\n"
							+ "Kami mengimbau Anda agar tidak menunda untuk segera merampungkan input biodata serta pengunggahan dokumen spesifik sebelum rentang waktu (deadline) yang berlaku habis. Sila navigasikan ke portal pendaftaran melalui pranala berikut: " + code + "\n\n"
							+ "Semoga Tuhan melimpahkan kemudahan pada proses seleksi akademis ini. Kami sungguh menantikan Sdr/i menjadi pilar peradaban sebagai bagian dari " + kampus + ".\n\n"
							+ "Hormat Kami,\nPanitia Seleksi PMB " + kampus;

					String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal", "*Pesan ini bersifat terotomatisasi secara elektronis oleh sistem administratif " + kampus + " guna merekam interaksi Anda*\n\n").getNilai();
					String url = (file != null && file.length > 0) ? buildPublicFileUrlSafe(file[0]) : null;
					Wa.kirimWaViaUltramsg(from, dawal + info, (file != null && file.length > 0) ? file[0].getName() : null, url, Wa.buatProfile(null, biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi()));
				}
			}
		}
	}

	private static String buildPublicFileUrlSafe(File file) {
		try {
			if (file == null || file.getAbsolutePath() == null) {
				return null;
			}
			String path = file.getAbsolutePath();
			int index = path.indexOf("webapps");
			if (index < 0) {
				return null;
			}
			String relative = path.substring(index + "webapps".length());
			return Common.getRequestHostWithProtocolSimple() + relative;
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	public static void infoDaftarMahasiswaDinyatakanDIterima(final BiodataCalonMahasiswa biodataCalonMahasiswa, final File... file) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (biodataCalonMahasiswa != null) {
			appendEmail(emailUser, biodataCalonMahasiswa.getEmail());
			if (biodataCalonMahasiswa.getMahasiswa() != null) {
				userIds.put(biodataCalonMahasiswa.getMahasiswa().getNim());
			}

			String emailMonitoring = Common.getKonfigurasi("alamat_email_monitoring_regitrasi", "", null, 
					biodataCalonMahasiswa.getTahun(),
					biodataCalonMahasiswa.getProdiLulus() == null ? biodataCalonMahasiswa.getProdi1() : biodataCalonMahasiswa.getProdiLulus(),
					biodataCalonMahasiswa.getProgram(), biodataCalonMahasiswa.getStatusAwalMahasiswa()).getNilai();
			
			if (!emailMonitoring.trim().isEmpty()) {
				appendEmail(emailUser, emailMonitoring);
			}

			String kampus = (biodataCalonMahasiswa.getGelombangPendaftaran() == null || biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi() == null) 
					? "" : biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi().getNama();

			if (emailUser.length() > 0 || userIds.length() > 0) {
				String subject = "Pengumuman Keputusan Rektorat: Penetapan Status Diterima Mahasiswa Baru " + kampus;
				
				String body = "Yth. Sdr/i. <b>" + biodataCalonMahasiswa.getNama() + "</b>,<br><br>"
						+ "Merujuk kepada hasil penetapan komite seleksi dan usai melalui serangkaian proses penilaian kapasitas kompetensi yang komprehensif, maka dengan penuh rasa syukur dan kebanggaan, kami menerbitkan Surat Keputusan yang menyatakan bahwa Anda <b>LULUS SELEKSI / DITERIMA</b> sebagai unsur resmi Mahasiswa Baru di <b>" + kampus + "</b>.<br><br>"
						+ "Berikut adalah ketetapan rincian program studi atas kelulusan Anda:<br>"
						+ "<b>Program Studi:</b> " + biodataCalonMahasiswa.getProdiLulus().getNama() + "<br>"
						+ "<b>Program / Jenis Kelas:</b> " + biodataCalonMahasiswa.getProgram() + "<br><br>"
						+ "Pencapaian akademis ini pantas dirayakan sebagai refleksi awal menuju pengembaraan kognitif dan pembentukan masa depan karier Anda yang visioner.<br><br>"
						+ "Sebagai instrumen legalisasi formal dalam meregistrasi status mahasiswanya, Anda kini dimandatkan untuk merampungkan tahapan <b>Registrasi Ulang dan Pembayaran Biaya Penyelenggaraan Pendidikan</b>. Prosedur setoran perbankan, deskripsi komponen pembiayaan yang mendetail, serta regulasi batas tempo daftar ulang (<i>deadline</i>) telah disisipkan di dalam berkas PDF yang terlampir.<br><br>"
						+ "Mohon curahkan perhatian mutlak pada instruksi teknis pendaftaran ulang di dalam lampiran tersebut, dan segeralah tunaikan kewajiban administratif finansial sesuai kerangka jadwal yang ada guna menghindari pembatalan otomatis (<i>void</i>) atas status kelulusan Anda.<br><br>"
						+ "Harapan kami tertuju sepenuhnya pada partisipasi aktif Saudara kelak. Selamat bergabung merajut rekam jejak sukses tiada henti di almamater kebanggaan kita, " + kampus + ".<br><br>"
						+ "Hormat Kami,<br>Pimpinan Rektorat & Badan Pengelola Pendaftaran<br>" + kampus;

				String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
				MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), biodataCalonMahasiswa, false, file);
			}

			if (Common.bolehKonfigurasi("aktifkan_kirim_notif_diterima_peserta_didik_baru_ke_wa")) {
				String from = biodataCalonMahasiswa.getHp();
				if (from == null || from.trim().isEmpty()) from = biodataCalonMahasiswa.getTeleponRumah();
				if (from == null || from.trim().isEmpty()) from = biodataCalonMahasiswa.getNoTelpOrtu();

				if (from != null && !from.trim().isEmpty() && !from.trim().equals("00000000000000000000") && !from.trim().equals("000000000")) {
					String info = "Yth. Sdr/i. *" + biodataCalonMahasiswa.getNama() + "*,\n\n"
							+ "Dengan sukacita yang teramat mendalam, kami menyampaikan bahwa pasca evaluasi yang ketat, Pimpinan Akademik telah menetapkan ketetapan sah bahwa Anda *LULUS* dan berhak menjadi mahasiswa baru di *" + kampus + "*. Anda diterima pada klasifikasi:\n\n"
							+ "*Program Studi:* " + biodataCalonMahasiswa.getProdiLulus().getNama() + "\n"
							+ "*Tipe Kelas:* " + biodataCalonMahasiswa.getProgram() + "\n\n"
							+ "Tonggak sejarah baru pada dimensi kognitif dan karir Anda resmi dimulai hari ini. Sebagai bagian dari validasi konfirmasi keikutsertaan secara definitif, mohon agar secepatnya Sdr/i mencermati berkas pedoman daftar ulang yang kami lampirkan secara saksama.\n\n"
							+ "Jangan menunda pemenuhan administrasi biaya pendidikan dan registrasi sistem karena tenggat waktu akan berlalu sangat presisi. Ketidaktepatan pada jadwal berakibat pencabutan hak kelulusan tanpa ganti.\n\n"
							+ "Sampai jumpa di masa perkenalan kampus, ukirkan prestasi setinggi puncaknya di " + kampus + "!\n\n"
							+ "Hormat Kami,\nTim Akademik PMB";

					String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal", "*Notifikasi Penerimaan ini divalidasi langsung oleh gerbang sistem " + kampus + "*\n\n").getNilai();
					String url = (file != null && file.length > 0) ? buildPublicFileUrlSafe(file[0]) : null;
					Wa.kirimWaViaUltramsg(from, dawal + info, (file != null && file.length > 0) ? file[0].getName() : null, url, Wa.buatProfile(null, biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi()));
				}
			}
		}
	}

	public static void infoDaftarSiswa(final CalonSiswa calonSiswa, final File file) throws Exception {
		infoDaftarSiswaBanyakFile(calonSiswa, file);
	}

	public static void infoDaftarSiswaBanyakFile(final CalonSiswa calonSiswa, final File... file) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (calonSiswa != null) {
			appendEmail(emailUser, calonSiswa.getAlamatEmail());
			if (calonSiswa.getSiswa() != null) {
				userIds.put(calonSiswa.getSiswa().getNomorIndukNasional());
			}

			String code = calonSiswa.urlLogin();
			String kampus = calonSiswa.getSekolah().getNama();

			if (emailUser.length() > 0 || userIds.length() > 0) {
				String subject = "Surat Pemberitahuan Resmi: Penerimaan Pendaftaran Calon Peserta Didik Baru Terdaftar";
				
				String body = "Yth. Bapak/Ibu Orang Tua Wali dan Calon Siswa (Ananda <b>" + calonSiswa.getNama() + "</b>),<br><br>"
						+ "Kami selaku pihak Pengurus Penerimaan Peserta Didik Baru menghaturkan tanda terima kasih yang sebesar-besarnya atas kepercayaan luar biasa yang dititipkan untuk melamar di lembaga pendidikan <b>" + kampus + "</b>.<br><br>"
						+ "Maksud penyuratan via surel elektornik ini adalah guna mensahkan secara presisi bahwa Ananda tercinta telah terekam eksistensi datanya sebagai entitas Calon Siswa Terdaftar di dalam pusat data (<i>database</i>) kesiswaan kami.<br><br>"
						+ "Ini sesungguhnya barulah penanda tahap fundamental. Ananda masih senantiasa harus melewati prosedur pengujian, ujian saringan kompetensi (opsional merujuk kebijakan sekolah), maupun filter penyerahan dokumen prasyarat yang kaku (misalnya berkas mutasi/surat keterangan). Mari kita semai dukungan positif bagi Ananda dalam menjalani seluruh ritme seleksi ini dengan percaya diri.<br><br>"
						+ "Terdapat beberapa formasi kelengkapan berkas esensial yang masih mesti dibereskan. Mohon bantuan Bapak/Ibu Wali untuk secepat mungkin merampungkannya lewat antarmuka profil personal yang tertaut secara spesifik di laman PPDB ini: <a href='" + code + "' target='_blank'>" + code + "</a>.<br><br>"
						+ "Besar asa kami kelak dapat menerima kehadiran Ananda seutuhnya pada hari-hari permulaan tahun ajaran sekolah. Segala progres penerimaan dapat diperiksa mandiri, namun bila muncul kendala hubungilah tenaga pendidik (humas) kami tanpa ragu.<br><br>"
						+ "Hormat Kami,<br>Panitia PPDB<br>" + kampus;

				String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
				MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), calonSiswa, file);
			}

			if (Common.bolehKonfigurasi("aktifkan_kirim_notif_daftar_peserta_didik_baru_ke_wa")) {
				Set<String> strings = calonSiswa.ambilTelp();
				for (String from : strings) {
					if (from != null && !from.trim().isEmpty() && !from.trim().equals("00000000000000000000") && !from.trim().equals("000000000")) {
						code = Common.getRequestHostWithProtocol() + "/ppdb";
						
						String info = "Yth. Wali/Orang Tua Ananda *" + calonSiswa.getNama() + "*,\n\n"
								+ "Bersama warta ini, pihak manajemen *" + kampus + "* melayangkan apresiasi atas keikutsertaan Ananda di dalam proses pendaftaran. Data peminatan berhasil di-enkapsulasi oleh sistem PPDB kami. Sebagai pertinggal yang sah, sistem mencetak Nomor Urut Pendaftaran *" + calonSiswa.getNoRegistrasi() + "* milik Ananda (Tanggal Lahir Tercatat: *" + (calonSiswa.getTanggalLahir() == null ? "Belum validasi" : Common.dateFormat1.get().format(calonSiswa.getTanggalLahir())) + "*).\n\n"
								+ "Menjaga stamina serta persistensi motivasi belajar sungguh diprioritaskan demi menuntaskan alur-alur penyaringan maupun pengarahan selanjutnya yang kelak dijadwalkan.\n\n"
								+ "Guna mengurai kemacetan pengesahan profil pelamar, disarankan agar Bapak/Ibu berkoordinasi guna membereskan asupan administrasi digital selekasnya via antarmuka sekolah: " + code + ".\n\n"
								+ "Semoga kelancaran memihak putra-putri panjenengan. Kami mohon pamit undur dan terima kasih atas preferensinya.\n\n"
								+ "Salam Takzim,\nPanitia PPDB";

						String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal", "*Warta Edukasi Mandiri - Notifikasi ini diarsip oleh mesin manajemen " + kampus + "*\n\n").getNilai();
						String url = (file != null && file.length > 0) ? buildPublicFileUrlSafe(file[0]) : null;
						Wa.kirimWaViaUltramsg(from, dawal + info, (file != null && file.length > 0) ? file[0].getName() : null, url, Wa.buatProfile(calonSiswa.getSekolah(), null));
					}
				}
			}
		}
	}

	public static void infoLengkapMahasiswa(final BiodataMahasiswa biodataMahasiswa, final File file) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (biodataMahasiswa != null && biodataMahasiswa.getMahasiswa() != null) {
			appendEmail(emailUser, biodataMahasiswa.getMahasiswa().getEmail());
			userIds.put(biodataMahasiswa.getMahasiswa().getNim());
		}

		if (emailUser.length() > 0 || userIds.length() > 0) {
			String subject = "Pemberitahuan Pendataan: Dokumentasi Validasi Salinan Biodata Induk Mahasiswa";
			
			String body = "Yth. Sdr/i Mahasiswa,<br><br>"
					+ "Sebagaimana diamanatkan dalam standarisasi pelaporan tata kelola pangkalan data perguruan tinggi, surat ini kami edarkan untuk memberikan transparansi atas rekaman sensus identitas akademis Anda.<br><br>"
					+ "Terlampir pada draf arsip digital surel ini merupakan manuskrip atau lembar salinan komprehensif Biodata Induk Mahasiswa milik Anda. Data administratif kependudukan serta informasi relasi yang tecermin di dalamnya digunakan secara fungsional dalam ragam keperluan pencetakan transkrip, asuransi, ijazah, hingga integrasi beasiswa.<br><br>"
					+ "Kami mewajibkan kesadaran partisipatif Anda guna membaca tuntas dan menginspeksi jikalau terselip kekeliruan cetak kata. Pemutakhiran ejaan, pelurusan nomer NIK, domisili, wajib dikonfirmasikan seketika melalui pihak sekretariat rektorat, membawa segenap presisi pendukung keabsahannya.<br><br>"
					+ "Untuk meretaskan revisi swadaya pada segmentasi profil yang berizin sunting terbuka, log masuk secara mandiri direkomendasikan pada kanal akademik di: <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a>.<br><br>"
					+ "Kelalaian perbaruan profil bukan tanggungjawab kami manakala bermuara pada kesukaran birokrasi legalitas kelulusan di kelak hari. Kerjasama Saudara teramatlah disyukuri.";

			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), biodataMahasiswa, file);
		}
	}

	public static void infoNimMahasiswa(final BiodataCalonMahasiswa biodataCalonMahasiswa, final File file) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (biodataCalonMahasiswa != null) {
			appendEmail(emailUser, biodataCalonMahasiswa.getEmail());
			if (biodataCalonMahasiswa.getMahasiswa() != null) {
				userIds.put(biodataCalonMahasiswa.getMahasiswa().getNim());
			}
		}

		if (emailUser.length() > 0 || userIds.length() > 0) {
			String subject = "Surat Ketetapan Institusi: Pengesahan & Perilisan Nomor Induk Mahasiswa (NIM)";
			
			String body = "Yth. Sdr/i Mahasiswa Terdaftar,<br><br>"
					+ "Suksesnya verifikasi dan asimilasi finansial Anda menandai diakuinya keabsahan Saudara sebagai entitas penuh dalam komunitas kampus kami. Maka berdasarkan hal ini, biro administrasi menyatakan secara paripurna tentang pengesahan pencetakan Nomor Pokok / <b>Nomor Induk Mahasiswa (NIM)</b>.<br><br>"
					+ "NIM ini berfungsi lebih sekadar nomor urut semata, melainkan merupakan nomor identitas permanen kenegaraan yang mengatribusi kehadiran fisik dan intelektual Anda dalam seluruh hierarki operasi akademik—baik pelaporan negara, perpustakaan, pencatatan prestasi IPK, hingga legitimasi wisuda.<br><br>"
					+ "Rincian otorisasi NIM beserta representasi visual/ID Card semu (jika tersedia) dapat didalami lebih lanjut dalam wujud lampiran elektronik pada surat ini. Tolong pelihara rahasia privasinya dan aplikasikan nomor validasi tersebut dalam segenap siklus log masuk portal administrasi di: <a href='" + Common.getRequestHostWithProtocol() + "/pmb.zul'>" + Common.getRequestHostWithProtocol() + "/pmb.zul</a>.<br><br>"
					+ "Sebuah penghormatan dapat menitipkan kunci permulaan rekam jejak ini kepada intelektual masa depan. Doa terbaik teriring demi kegemilangan studi Anda.";

			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), biodataCalonMahasiswa, file);
		}
	}

	public static void infoDaftarUjianMahasiswa(final BiodataCalonMahasiswa biodataCalonMahasiswa, final File[] file) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (biodataCalonMahasiswa != null) {
			appendEmail(emailUser, biodataCalonMahasiswa.getEmail());
			if (biodataCalonMahasiswa.getMahasiswa() != null) {
				userIds.put(biodataCalonMahasiswa.getMahasiswa().getNim());
			}
		}

		if (emailUser.length() > 0 || userIds.length() > 0) {
			String subject = "Perilisan Dokumen Krusial: Kartu Tanda Peserta Ujian Seleksi Masuk Calon Mahasiswa";
			
			String body = "Yth. Calon Mahasiswa Baru,<br><br>"
					+ "Sejurus dengan rangkaian konfirmasi pendaftaran ujian saringan masuk institusional yang Anda registrasikan, sistem telah sukses mengekstraksi dan menyintesiskan rilis dokumen legal berupa <b>Kartu Tanda Peserta Ujian Masuk</b>.<br><br>"
					+ "Kartu lisensi ini bersifat vital, dikarenakan fungsinya yang merupakan tiket definitif Anda menuju altar kompetisi seleksi. Kandungannya melingkupi kode validasi peserta, deskripsi penanggalan kronologis waktu, detail ruang fisik/seksi virtual CBT daring, beserta daftar pantangan kedisiplinan yang tabu untuk dilanggar.<br><br>"
					+ "Dokumen ini tertambat lekat dalam lampiran surel ini. Pemohon sangat dipaksa oleh protap kepanitiaan ujian agar <b>mencetak spesimen kartu ini secara berwarna dan paripurna</b>, untuk direpresentasikan berdampingan dengan identitas diri (KTP / Surat Kependudukan lainnya) kepada otoritas pengawas ujian di hari H pelaksanaan tes.<br><br>"
					+ "Bila terdapat disparitas informasi, tinjau segera dan selesaikan aduan pengaduan ke meja pendaftaran melalui menu panel <a href='" + Common.getRequestHostWithProtocol() + "/pmb.zul'>" + Common.getRequestHostWithProtocol() + "/pmb.zul</a>.<br><br>"
					+ "Dedikasikan segenap kejernihan pikiran yang Anda miliki, berupayalah secara ksatria, dan semoga Tuhan merestui langkah intelektual yang Anda rintis.";

			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), biodataCalonMahasiswa, file);
		}
	}

	public static void infoDaftarUjianSiswa(final CalonSiswa calonSiswa, final File[] file) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (calonSiswa != null) {
			appendEmail(emailUser, calonSiswa.getAlamatEmail());
			if (calonSiswa.getSiswa() != null) {
				userIds.put(calonSiswa.getSiswa().getNomorIndukNasional());
			}
		}

		if (emailUser.length() > 0 || userIds.length() > 0) {
			String subject = "Perilisan Dokumen Penting: Kartu Identitas Ujian Saringan Masuk Calon Peserta Didik Baru";
			
			String body = "Yth. Orang Tua Wali yang kami agungkan beserta Calon Siswa Terpilih,<br><br>"
					+ "Bersinggungan dengan beranjaknya transisi tahap penyaringan, panitia seleksi sekolah telah menetapkan kesiapan administratif kepesertaan. Menandai kepatuhan tersebut, sistem mengeluarkan dan mendistribusikan <b>Kartu Tanda Kepesertaan Ujian Tulis/Seleksi</b> bagi Ananda.<br><br>"
					+ "Warkat tersebut kami sertakan sebagai arsip digital pada korespondensi elektronik ini. Tertera dengan cermat di dalamnya berupa serangkaian matriks penjadwalan, titik perhentian lokasi kelas evaluasi, angka nomor acuan meja, ditambah regulasi larangan seragam maupun bawaan elektronik ketika penyeleksian tengah berjalan.<br><br>"
					+ "Sebagai wali representatif, sudilah kiranya mempersiapkan salinan cetak fisik kertas surat izin ujian tersebut. Kartu akan dirazia oleh komisi penguji sedari depan gerbang ruangan, kepemilikannya merupakan validitas absensi kehadiran mutlak Ananda pada ajang tes. Awasi perubahannya melalui akses portal pada <a href='" + Common.getRequestHostWithProtocol() + "/pmb.zul'>" + Common.getRequestHostWithProtocol() + "/pmb.zul</a>.<br><br>"
					+ "Ketenangan merupakan kunci memecah setiap persoalan ujian. Doa jajaran staf menyertai, niscaya sang buah hati mendapat predikat kelulusan paling prima.";

			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), calonSiswa, file);
		}
	}

	public static void infoDaftarUjianPegawai(final CalonPegawai calonPegawai, final File[] file) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (calonPegawai != null) {
			appendEmail(emailUser, calonPegawai.getAlamatEmail());
		}

		if (emailUser.length() > 0 || userIds.length() > 0) {
			String subject = "Undangan Kepesertaan Asesmen: Penerbitan Kartu Tanda Tes Seleksi Rekrutmen Karyawan Baru";
			
			String body = "Yth. Saudara/i Pelamar Karir Profesional,<br><br>"
					+ "Berdasarkan penilaian penyaringan rekam jejak pelamaran <i>(curriculum screening)</i> awal Saudara yang kami pandang sejalan dengan visi institusional dan deskripsi pekerjaan kami, departemen pengembangan insan memanggil dan meratifikasi kelayakan Anda buat memasuki fase selanjutnya, yakni ujian kapabilitas.<br><br>"
					+ "Keikutsertaan Anda dicerminkan pada instrumen legalisasi <b>Kartu Tanda Tes Ujian Kompetensi/Psikotes Pegawai</b> yang menyertai badan pesan elektronik ini. Kartu krusial ini menghimpun petunjuk tata letak kursi persidangan asesmen Saudara, jam kedatangan kedisiplinan yang rigid (tanpa ampun bagi toleransi keterlambatan), dan detail substansi jenis ujian seleksinya.<br><br>"
					+ "Keberadaan Anda wajib bersanding dengan kartu peserta ujian yang telah diprint dan KTP kependudukan yang sinkron. Selaku seorang perintis tenaga pelaksana profesional, harap maklumi dan peluklah ketegasan prosedur kami. Perhatikan sekiranya terdengar desas-desus pembatalan waktu melalui dasbor rekrutasi institusi yang bermukim di <a href='" + Common.getRequestHostWithProtocol() + "/rekrut.zul'>" + Common.getRequestHostWithProtocol() + "/rekrut.zul</a>.<br><br>"
					+ "Berusahalah sebaik-baiknya menyajikan potensi tersembunyi diri Anda. Sampai bertemu di koridor kompetisi rekrutmen ini. Terimalah penghargaan besar atas minat besarnya pada almamater institusi kerja kita kelak.";

			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), calonPegawai, file);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void infoBatalBayar(final Kegiatan kegiatan, final String kodeAsli) throws Exception {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan.getCalonMahasiswa();
		if (biodataCalonMahasiswa != null) {
			if (biodataCalonMahasiswa.getMahasiswa() != null) userIds.put(biodataCalonMahasiswa.getMahasiswa().getNim());
			appendEmail(emailUser, biodataCalonMahasiswa.getEmail());
		}

		Mahasiswa mahasiswa = kegiatan.getMahasiswa();
		if (mahasiswa != null) {
			userIds.put(mahasiswa.getNim());
			appendEmail(emailUser, mahasiswa.getEmail());
		}

		if (emailUser.length() > 0 || userIds.length() > 0) {
			String subject = "Surat Pemberitahuan Khusus: Tindakan Pembatalan Otomatis (Void) atas Setoran Kode Transaksi " + kodeAsli;
			
			String body = "Yth. Bapak/Ibu/Sdr/i Yang Bersangkutan,<br><br>"
					+ "Departemen Layanan Bendahara dan Sentra Administrasi Keuangan memanjatkan permohonan pemakluman terdalamnya menyoal gangguan asimilasi prosedur penagihan yang telah terjadi di portal pendataan elektronik Anda.<br><br>"
					+ "Karena timbulnya konflik pada validasi teknis (seperti kegagalan rekonsiliasi transfer dengan mitra bank, kekeliruan penyetoran besaran angka nominal tagihan, ataupun habisnya siklus waktu token pembayaran), instrumen finansial kami dipaksa bertindak cepat demi keutuhan neraca institusi guna <b>MEMBATALKAN dan MENGANULIR (<i>VOID</i>)</b> rangkaian pesanan yang berafiliasi pada transaksi kode pengidentifikasi referensi tagihan: <b>" + kodeAsli + "</b>.<br><br>"
					+ "Guna keperluan rekam audit Anda, nota detail dari transaksi yang diredam pembatalannya itu diabadikan dalam bentuk ringkasan berkas fail melampiri rilis pesan ini.<br><br>"
					+ "Akibat anulir ini, status pelunasan kegiatan pendidikan tersebut ditarik ulang ke keadaan asalnya, yakni \"Belum Dibayar\" (Unpaid). Segera cegah tindak setoran penyetoran uang lebih jauh menggunakan ID lama yang inaktif tersebut. Saudara/i dimohon kooperatif menginspeksi pelacakan tagihan terbaru demi kemunculan token bank substitusinya via loket laman akademik: <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a> dan menjelajah menu <b>Informasi Riwayat Pembayaran</b>.<br><br>"
					+ "Penanganan urgensi finansial lebih terinci mohon dilaporkan ke garda terdepan operasional keuangan guna penyelarasan bukti saldo secepatnya. Terimakasih atas ketabahan dalam proses konversi teknis pembayaran ini.";

			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

			File file = null;
			Session session = null;
			try {
				session = HibernateUtil.currentNativeSession();
				int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
						.add(Restrictions.eq("kegiatan", kegiatan))
						.add(Restrictions.isNotNull("pengaturanPembayaranBulanan"))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				String fileReport = "";
				if (kegiatan.getMahasiswa() != null) {
					fileReport = jumlah > 0 ? "Bukti_Pembayaran_Mahasiswa" : "Bukti_Pembayaran_Mahasiswa_tagihan";
				} else if (kegiatan.getCalonMahasiswa() != null) {
					fileReport = jumlah > 0 ? "Bukti_Pembayaran_Calon_Mahasiswa" : "Bukti_Pembayaran_Calon_Mahasiswa_tagihan";
				}

				if (!fileReport.trim().isEmpty()) {
					Map parameters = ais.common.HashMapGenerator.getRand();
					parameters.put("tanggal", kegiatan.getTanggal());
					parameters.put("kegiatan", kegiatan.getId());

					try {
						Fakultas fakultas = kegiatan.getMahasiswa() != null
								? kegiatan.getMahasiswa().getJurusan().getFakultas()
								: kegiatan.getCalonMahasiswa().getProdiLulus() != null
										? kegiatan.getCalonMahasiswa().getProdiLulus().getFakultas()
										: kegiatan.getCalonMahasiswa().getProdi1().getFakultas();
						LampiranLain kop = LampiranLain.ambil(false, fakultas.getId(), LampiranLain.KOP_FAKULTAS);
						if (kop != null) {
							File fileKop = kop.ambilFile();
							if (fileKop != null && fileKop.exists()) {
								parameters.put("KOP_FAKULTAS", fileKop.getAbsolutePath());
								parameters.put("KOP_FAKULTAS_" + fakultas.getId(), fileKop.getAbsolutePath());
								parameters.put("KOP_FAKULTAS_" + fakultas.getNama(), fileKop.getAbsolutePath());
							}
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/CommonEmail.java:925");
						// Ignore silently based on original code
					}

					List<Long> detailKegiatans = new ArrayList<Long>();
					for (DetailKegiatan detailKegiatan : kegiatan.ambilDetailKegiatan()) {
						detailKegiatans.add(detailKegiatan.getId());
					}
					if (detailKegiatans.isEmpty()) detailKegiatans.add(-1L);
					parameters.put("detailKegiatans", detailKegiatans.toArray());

					file = Report.generateFileReportSimple(Report.PDF, parameters, fileReport);
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				closeHibernateSession(session);
			}
			MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), kegiatan, new File[] { file });
		}
	}

	public static void infoBayar(final Kegiatan kegiatan, final File file) throws Exception {
		if (kegiatan == null) {
			return;
		}
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		BiodataCalonMahasiswa biodataCalonMahasiswa = kegiatan.getCalonMahasiswa();
		if (biodataCalonMahasiswa != null) {
			if (biodataCalonMahasiswa.getMahasiswa() != null) userIds.put(biodataCalonMahasiswa.getMahasiswa().getNim());
			appendEmail(emailUser, biodataCalonMahasiswa.getEmail());
		}

		Mahasiswa mahasiswa = kegiatan.getMahasiswa();
		if (mahasiswa != null) {
			userIds.put(mahasiswa.getNim());
			appendEmail(emailUser, mahasiswa.getEmail());
		}

		if (emailUser.length() > 0 || userIds.length() > 0) {
			String namaKegiatan = kegiatan.getJenisKegiatan() == null ? "Pembayaran"
					: kegiatan.getJenisKegiatan().getNamaKegiatan();
			String subject = "Surat Konfirmasi Tanda Terima: Validasi Final Penyetoran Biaya Kegiatan/Pendidikan [" + namaKegiatan + "]";
			String waktu = getWaktuSapaan();

			PerguruanTinggi perguruanTinggi = mahasiswa != null && mahasiswa.getJurusan() != null
					&& mahasiswa.getJurusan().getFakultas() != null && mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() != null
							? mahasiswa.getJurusan().getFakultas().getPerguruanTinggi()
							: biodataCalonMahasiswa != null && biodataCalonMahasiswa.getGelombangPendaftaran() != null
									&& biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi() != null
											? biodataCalonMahasiswa.getGelombangPendaftaran().getPerguruanTinggi() : null;

			if (perguruanTinggi == null) perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

			String pt = perguruanTinggi != null ? perguruanTinggi.getNama() : "Pihak Institusi";
			String url = kegiatan == null || kegiatan.getId() == null ? ""
					: Common.getRequestHostWithProtocol() + "/StrukM?id="
							+ URLEncoder.encode(("EE" + Common.desEncrypter.get().encrypt(kegiatan.getId() + "")), "UTF-8");

			String nama = mahasiswa != null ? mahasiswa.getNama() : biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama() : "";
			String from = mahasiswa != null ? mahasiswa.getTelp() : biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getHp() : "";

			if ((from == null || from.trim().isEmpty()) && mahasiswa != null && mahasiswa.getBiodataCalonMahasiswaData() != null)
				from = mahasiswa.getBiodataCalonMahasiswaData().getHp();
			if ((from == null || from.trim().isEmpty()) && biodataCalonMahasiswa != null)
				from = biodataCalonMahasiswa.getTeleponRumah();
			if ((from == null || from.trim().isEmpty()) && mahasiswa != null && mahasiswa.getBiodataCalonMahasiswaData() != null)
				from = mahasiswa.getBiodataCalonMahasiswaData().getTeleponRumah();

			String body = "Yth. Saudara/i Pembayar (A.n. <b>" + nama + "</b>),<br><br>"
					+ "Salam hangat dan salam sejahtera teriring buat Saudara/i di waktu " + waktu.toLowerCase() + " ini.<br><br>"
					+ "Sebagai wujud respons transparansi kinerja dan dedikasi prima, tata usaha manajerial sentra perbendaharaan bendahara " + pt + " merasa sangat perlu merilis korespondensi sah ini guna menitipkan rasa terimakasih yang hakiki atas penyelesaian kewajiban ikatan tanggungan pembiayaan pendidikan yang Anda emban.<br><br>"
					+ "Melalui surat kawat elektronik ini, diberitahukan bahwa agregat dana nominal setoran Saudara telah seutuhnya merapat, terekonsiliasi valid pada lintas-pembukuan kas institusi utama, serta terhindar dari defisit kekurangan setoran tagihan <i>(full-payment clear)</i>.<br><br>"
					+ "Bentuk pelestarian jejak otentik komersial ini lantas direalisasikan dalam cetak pertinggal faktur nota kuitansi penerimaan (Invoice Receipt) berwujud berkas sisipan yang mendampingi teks edaran email tersebut. Tanda terima otentik ini sanggup Anda gunakan selaku warkat bukti pelegalisiran absensi aktivitas/pemungutan jaminan kartu mahasiswa Anda.<br><br>"
					+ "Anda turut diundang guna mengulas kembali rangkuman rekaman lalu lintas debet-kredit pribadi Anda, menyimak transparansi tagih-sisa via dasbor log masuk laman akademis."
					+ (url == null || url.trim().isEmpty() ? "" : " Demi kepastian terpadu, mekanisme verifikasi silang (cross-check) juga difasilitasi institusi menerusi tautan faktur interaktif sentral ini: <a href='" + url + "'>" + url + "</a>") + "<br><br>"
					+ "Bila mendapati anomali rincian persentase hitungan bayar, mohon panggil divisi loket pengaduan kami secepat mungkin di jam jam operasional kampus. Atas keandalan kontribusi tempo penyelesaian bayar dari Anda, senat manajemen institusi menganggukkan rasa salut sedalamnya.<br><br>"
					+ "Hormat Kami,<br>Direktorat Bendahara Pusat Keuangan " + pt;

			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
			MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), kegiatan, false, file);

			if (Common.bolehKonfigurasi("aktifkan_kirim_notif_pembayaran_ke_wa")) {
				body = "Yth. Sdr/i Pembayar (*" + nama + "*),\n\n"
						+ "Salam sejahtera di " + waktu.toLowerCase() + " ini.\n"
						+ "Berita gembira bahwasanya divisi penerimaan loket perbendaharaan kas *" + pt + "* mengonfirmasi kelunasan dan penyelesaian kewajiban pembiayaan atas kegiatan Anda secara menyeluruh dan komprehensif.\n\n"
						+ "Sinkronisasi buku besar penerimaan institusional kami kini telah seutuhnya menjurnal mutasi uang yang terkirim dari entitas Saudara tanpa mendapati kekeliruan sisa kekurangan tagihan (Clear of Default).\n\n"
						+ "Penyimpanan berkas bukti struk transfer sah turut direkomendasikan dan diekspor ke piranti Saudara, terlampir pada giringan surel utama guna menuntaskan ragam pengesahan kelanjutan administrasi akademik Anda kelak."
						+ (url == null || url.trim().isEmpty() ? "" : "\nBila menghendaki verifikasi faktur pelunasan berbasis peramban laman otentik sentral institusi, silahkan menelusuri ke pautan e-slip digital berikut: " + url) + "\n\n"
						+ "Apresiasi setinggi awan mengiringi konsistensi Saudara dalam melunasi kewajiban finansialnya mendahului waktu penagihan terakhir. Terima kasih nan tulus!\n\n"
						+ "Divisi Keuangan " + pt;

				String dawal = Common.getKonfigurasi("pesan_tambahan_notif_awal", "*Nota Edaran Digital: Pemberitahuan otentikasi setoran bayar rilis via Sistem Akademik " + pt + "*\n\n").getNilai();
				String urlD = buildPublicFileUrlSafe(file);

				if (from != null && !from.trim().isEmpty() && !from.trim().equals("00000000000000000000") && !from.trim().equals("000000000")) {
					if (mahasiswa != null) {
						Wa.kirimWaViaUltramsg(mahasiswa.getTelp(), dawal + body, "Kuitansi_Otentik_Pelunasan.pdf", urlD, Wa.buatProfile(null, perguruanTinggi));
					} else if (biodataCalonMahasiswa != null) {
						Wa.kirimWaViaUltramsg(biodataCalonMahasiswa.getHp(), dawal + body, "Kuitansi_Otentik_Pelunasan.pdf", urlD, Wa.buatProfile(null, perguruanTinggi));
					}
				}
			}
		}
	}

	public static void infoBayarViaFinpay(final FinpayRequest finpayRequest, final File file) throws Exception {
		sendPaymentNotification(finpayRequest, finpayRequest.getBiodataCalonMahasiswa(), finpayRequest.getMahasiswa(), 
				finpayRequest.getJenisKegiatan() != null ? finpayRequest.getJenisKegiatan().getNamaKegiatan() : "Kegiatan Akademik (Mitra Finpay)", file);
	}

	public static void infoBayarViaBni(final BniRequest bniRequest, final File file) throws Exception {
		sendPaymentNotification(bniRequest, bniRequest.getBiodataCalonMahasiswa(), bniRequest.getMahasiswa(), 
				bniRequest.getJenisKegiatan() != null ? bniRequest.getJenisKegiatan().getNamaKegiatan() : "Kegiatan Akademik (Bank BNI)", file);
	}

	public static void infoBayarViaBsi(final BsiRequest bsiRequest, final File file) throws Exception {
		sendPaymentNotification(bsiRequest, bsiRequest.getBiodataCalonMahasiswa(), bsiRequest.getMahasiswa(), 
				bsiRequest.getJenisKegiatan() != null ? bsiRequest.getJenisKegiatan().getNamaKegiatan() : "Kegiatan Akademik (Bank BSI)", file);
	}

	public static void infoBayarViaBri(final BriRequest briRequest, final File file) throws Exception {
		sendPaymentNotification(briRequest, briRequest.getBiodataCalonMahasiswa(), briRequest.getMahasiswa(), 
				briRequest.getJenisKegiatan() != null ? briRequest.getJenisKegiatan().getNamaKegiatan() : "Kegiatan Akademik (Bank BRI)", file);
	}

	// Helper terpusat untuk ke-4 vendor pembayaran online (Finpay, BNI, BSI, BRI)
	private static void sendPaymentNotification(GeneralValueObject requestObj, BiodataCalonMahasiswa biodataCalonMahasiswa, Mahasiswa mahasiswa, String jenisKegiatanNama, File file) {
		StringBuilder emailUser = new StringBuilder();
		JSONArray userIds = new JSONArray();

		if (biodataCalonMahasiswa != null) {
			if (biodataCalonMahasiswa.getMahasiswa() != null) userIds.put(biodataCalonMahasiswa.getMahasiswa().getNim());
			appendEmail(emailUser, biodataCalonMahasiswa.getEmail());
		}

		if (mahasiswa != null) {
			userIds.put(mahasiswa.getNim());
			appendEmail(emailUser, mahasiswa.getEmail());
		}

		if (emailUser.length() > 0 || userIds.length() > 0) {
			String subject = "Notifikasi Kemitraan Bank: Pengesahan & Bukti Tanda Terima Pembayaran [" + jenisKegiatanNama + "]";
			String waktu = getWaktuSapaan();
			String nama = mahasiswa != null ? mahasiswa.getNama() : (biodataCalonMahasiswa != null ? biodataCalonMahasiswa.getNama() : "");

			String body = "Yth. Saudara/i Pembayar (A.n. <b>" + nama + "</b>),<br><br>"
					+ "Salam hangat dan salam sejahtera teriring buat Saudara/i di waktu " + waktu.toLowerCase() + " ini.<br><br>"
					+ "Berdasarkan integrasi perbankan layanan <i>Host-to-Host (H2H)</i> kami via kanal pembayaran mitra, kami menginformasikan validitas bahwa penyelesaian tanggungan setoran <b>" + jenisKegiatanNama + "</b> Saudara telah diverifikasi oleh sentra pengalihan dana antar-bank.<br><br>"
					+ "Nominal uang Saudara dipastikan merapat penuh tanpa sela defisit dan langsung terekonsiliasi ke pangkalan kas induk bendahara kami dengan status 'Sukses/Settled'.<br><br>"
					+ "Guna kelancaran operasional maupun administrasi Anda, rincian kuitansi e-Receipt/Struk bukti pelunasan turut dilampirkan beriringan pada pesan elektrikal ini. Jangan melupakan fungsi arsip dari lampiran tersebut bilamana Saudara di kemudian waktu direkuisisi untuk memverifikasinya kembali kepada pihak tata usaha kami saat menuntut perizinan kartu-ujian.<br><br>"
					+ "Jika kerancuan pendataan muncul pasca validasi ini, kami senantiasa berjaga memperbaikinya jika Anda menghubungi hotline aduan divisi keuangan kami. Penghormatan paling teratas tertuju atas konsistensi ketaatan Saudara menunaikan amanah jatuh tempo finansial institusi.<br><br>"
					+ "Hormat Kami,<br>Pusat Integrasi Bank & Keuangan Institusi";
					
			String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();

			try {
				boolean kirimWa = Konfigurasi.AKTIF.equalsIgnoreCase(Common.getKonfigurasi("aktifkan_kirim_notif_pembayaran_ke_wa", Konfigurasi.AKTIF).getNilai());
				MailSender.sendMailLampiran(userIds, subject, body, sender, emailUser.toString(), requestObj, kirimWa, file);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:1082");
			}
		}
	}

	public static void infoAdaUjianJadwalPelajaran(final Pertemuan pertemuan, final Ujian ujian) {
		Common.createDefaultTimer(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Tbmuser tbmuser = Common.getCurrentUser();
				JSONArray userIds = new JSONArray();
				StringBuilder emailUser = new StringBuilder();

				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					processTbmUser(tbmuser, userIds, emailUser);

					if (pertemuan.getJadwalPelajaran() != null) {
						processGuru(session, pertemuan.getJadwalPelajaran().populateGuru().values(), userIds, emailUser);
						processSiswa(pertemuan.ambilSiswa(), userIds, emailUser);
					}

					if (emailUser.length() > 0 || userIds.length() > 0) {
						String info = pertemuan.info();
						String userName = tbmuser != null ? tbmuser.getUserNama() + " (" + tbmuser.getUserId() + ")" : "Sistem";
						
						String subject = "Maklumat Akademik Sekolah: Proklamasi Penjadwalan Sinkronisasi Ujian/Evaluasi untuk Pertemuan Ke-" + pertemuan.getPertemuanKe();
						String body = "Yth. Seluruh Siswa/i yang kami banggakan dan penuh dengan potensi,<br><br>"
								+ "Mempertimbangkan porsi capaian target edukasi berbasis Kurikulum, beserta penelaahan rancangan pengajaran (RPP) yang telah dirangkai rapi oleh pendidik di ruang kelas, kami menyebarluaskan maklumat perihal keberadaan uji tes formatif/sumatif yang harus diikuti dengan disiplin tinggi. Konstruksi tesnya adalah sebagai berikut:<br><br>"
								+ "<b>Nomor Registrasi Pertemuan:</b> Ke-" + pertemuan.getPertemuanKe() + "<br>"
								+ "<b>Identitas Mata Pelajaran/Topik Kajian:</b> " + info + "<br>"
								+ "<b>Rumpun Tema Evaluasi/Nama Ujian:</b> " + ujian.getNama() + "<br>"
								+ "<b>Bentuk Tipe Soal (Varian):</b> " + ujian.getJenis() + "<br>"
								+ "<b>Penanda Maklumat Atas Nama Staf/Guru:</b> " + userName + "<br><br>"
								+ "Ujian seleksi kognitif yang digagas bukan semata mengejar rapor persentase belaka, tapi memfasilitasi pondasi penyerapan logika mendalam atas ragam pelajaran kalian. Sangat difatwakan buat Ananda guna mengeksploitasi setiap derik jam waktu istirahat yang tersedia menjelang detik-detik hari pertempuran intelektual ini guna melatih latihan bedah studi mendalam.<br><br>"
								+ "Ketahui tata-cara peraturannya serta tautan panel asesmen interaktif soalnya secara <i>online</i> melalui teras muka laman administratif kesiswaan pada titik <a href='" + Common.getRequestHostWithProtocol() + "'>" + Common.getRequestHostWithProtocol() + "</a>, yang tersemat menawan pada tautan fitur bilah menu <b>Aktifitas Jadwal Pelajaran</b>.<br><br>"
								+ "Selamat menggali permata pengetahuan, asah kompetensimu setajam silet, serta senantiasa rawat ruh sportivitas nan luhur di balik setiap tikungan cobaan. Kemuliaan untuk kalian semua!";
								
						String sender = Common.getKonfigurasi("default_email", "info@zishof.com").getNilai();
						MailSender.sendMail(userIds, subject, body, sender, emailUser.toString(), ujian);
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/CommonEmail.java:1125");
				} finally {
					closeHibernateSession(session);
				}
			}
		});
	}
}
