package ais.common.helper;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hibernate.Session;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;

import ais.common.PesanFormalHelper;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Label;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

/**
 * <h1>DownloadFotoMassalHelper — unduh FOTO MASSAL (ZIP) dengan pembacaan BLOB paralel</h1>
 *
 * <p><b>Untuk apa.</b> Admin mengunduh SEMUA foto suatu jenis (mahasiswa/siswa/dosen/pegawai/guru/
 * calon siswa/pengguna) sekaligus sebagai satu berkas ZIP; nama tiap berkas di dalam ZIP = identitas
 * pemilik (NIM/No.Induk/NIDN/kode/NIP/No.Registrasi/User ID) sehingga langsung siap dipakai untuk
 * upload massal balik atau arsip.</p>
 *
 * <p><b>Kenapa multi-thread.</b> Membaca ratusan/ribuan BLOB foto dari database satu per satu lambat.
 * Helper ini memakai kolam thread (maksimal {@value #MAKS_THREAD}) untuk membaca banyak BLOB
 * <b>bersamaan</b>; tiap worker memakai {@code Session} Hibernate SENDIRI (session tidak thread-safe).
 * Penulisan ke ZIP tetap satu utas (ZipOutputStream tidak thread-safe) namun cepat (ke berkas temp
 * di disk). {@code Semaphore} membatasi jumlah hasil yang menganggur di memori (bounded) agar tidak
 * boros memori pada data besar.</p>
 *
 * <p><b>Alur.</b> (1) satu query ambil daftar {@code {idFoto, namaBerkas}} (TANPA blob). (2) Submitter
 * mengirim tugas baca-blob (dibatasi semaphore). (3) Utas penulis mengambil hasil begitu selesai
 * ({@code CompletionService.take}) lalu menulisnya ke ZIP. (4) ZIP dikirim ke browser via
 * {@code Filedownload}. Semua dibungkus try/catch: satu foto gagal dibaca hanya dilewati.</p>
 */
public final class DownloadFotoMassalHelper {

	/** Batas maksimal thread pembaca BLOB (permintaan: maksimal 50). */
	public static final int MAKS_THREAD = 50;

	private DownloadFotoMassalHelper() {
	}

	// ── API per entitas ─────────────────────────────────────────────────────────────────────

	/** Unduh SEMUA foto mahasiswa (nama berkas = NIM). */
	public static void downloadFotoMahasiswaMassal() throws Exception {
		prosesDownload(daftarFoto("select f.id, m.nim, f.nama from ais.database.model.file.FotoMahasiswa f, "
				+ "ais.database.model.Mahasiswa m where f.mahasiswa = m.id and m.nim is not null and m.nim <> ''"),
				FotoMahasiswa.class, "Foto_Mahasiswa.zip");
	}

	/**
	 * Varian headless untuk New UI. Daftar ID berasal dari scope adapter Mahasiswa,
	 * sehingga ZIP tidak dapat membawa foto milik perguruan tinggi lain.
	 */
	public static File createFotoMahasiswaZip(List<Long> mahasiswaIds) throws Exception {
		List<Object[]> daftar = daftarFotoMahasiswa(mahasiswaIds);
		AtomicInteger diproses = new AtomicInteger(0);
		AtomicInteger berhasil = new AtomicInteger(0);
		return bacaKeZip(daftar, FotoMahasiswa.class, diproses, berhasil);
	}

	/** Unduh SEMUA foto siswa (nama berkas = No. Induk/NIS). */
	public static void downloadFotoSiswaMassal() throws Exception {
		prosesDownload(daftarFoto("select f.id, coalesce(s.nomorInduk, s.nim), f.nama from "
				+ "ais.database.model.file.FotoSiswa f, ais.database.model.sekolah.Siswa s where f.siswa = s.id"),
				FotoSiswa.class, "Foto_Siswa.zip");
	}

	/** Unduh SEMUA foto dosen (nama berkas = NIDN/code). */
	public static void downloadFotoDosenMassal() throws Exception {
		prosesDownload(daftarFoto("select f.id, coalesce(d.nidn, d.code), f.nama from "
				+ "ais.database.model.file.FotoDosen f, ais.database.model.Dosen d where f.dosen = d.id"),
				FotoDosen.class, "Foto_Dosen.zip");
	}

	/** Unduh SEMUA foto pegawai (nama berkas = code/NIP). */
	public static void downloadFotoPegawaiMassal() throws Exception {
		prosesDownload(daftarFoto("select f.id, p.code, f.nama from ais.database.model.file.FotoPegawai f, "
				+ "ais.database.model.Pegawai p where f.pegawai = p.id and p.code is not null and p.code <> ''"),
				FotoPegawai.class, "Foto_Pegawai.zip");
	}

	/** Unduh SEMUA foto guru (nama berkas = NIP/NUPTK). */
	public static void downloadFotoGuruMassal() throws Exception {
		prosesDownload(daftarFoto("select f.id, coalesce(g.nip, g.nuptk), f.nama from "
				+ "ais.database.model.file.FotoGuru f, ais.database.model.sekolah.Guru g where f.guru = g.id"),
				FotoGuru.class, "Foto_Guru.zip");
	}

	/** Unduh SEMUA foto calon siswa (nama berkas = No. Registrasi/NIS). */
	public static void downloadFotoCalonSiswaMassal() throws Exception {
		prosesDownload(daftarFoto("select f.id, coalesce(c.noRegistrasi, c.nis), f.nama from "
				+ "ais.database.model.file.FotoCalonSiswa f, ais.database.model.sekolah.CalonSiswa c "
				+ "where f.calonSiswa = c.id"), FotoCalonSiswa.class, "Foto_CalonSiswa.zip");
	}

	/** Unduh SEMUA foto biodata calon mahasiswa (nama berkas = No. Registrasi). */
	public static void downloadFotoBiodataCalonMahasiswaMassal() throws Exception {
		prosesDownload(daftarFoto("select f.id, b.noRegistrasi, f.nama from "
				+ "ais.database.model.file.FotoBiodataCalonMahasiswa f, ais.database.model.BiodataCalonMahasiswa b "
				+ "where f.biodataCalonMahasiswa = b.id and b.noRegistrasi is not null and b.noRegistrasi <> ''"),
				FotoBiodataCalonMahasiswa.class, "Foto_BiodataCalonMahasiswa.zip");
	}

	/** Unduh SEMUA foto pengguna/admin (nama berkas = User ID). */
	public static void downloadFotoTbmuserMassal() throws Exception {
		prosesDownload(daftarFoto("select f.id, t.userId, f.nama from ais.database.model.file.FotoAdmin f, "
				+ "ais.database.model.Tbmuser t where f.tbmuser = t.userId and t.userId is not null and t.userId <> ''"),
				FotoAdmin.class, "Foto_Pengguna.zip");
	}

	// ── Inti ────────────────────────────────────────────────────────────────────────────────

	/**
	 * Jalankan query daftar foto → {@code List<Object[]>{ Long idFoto, String namaBerkas }}. Query
	 * HARUS mengembalikan {@code (idFoto, identitas, namaAsli)}; namaBerkas dibentuk = identitas +
	 * ekstensi dari namaAsli (default {@code .jpg}). BLOB TIDAK diambil di sini (dibaca paralel nanti).
	 */
	private static List<Object[]> daftarFoto(String hql) {
		return daftarFoto(hql, null);
	}

	private static List<Object[]> daftarFotoMahasiswa(List<Long> mahasiswaIds) {
		if (mahasiswaIds == null || mahasiswaIds.isEmpty()) return new ArrayList<Object[]>();
		return daftarFoto("select f.id, m.nim, f.nama from ais.database.model.file.FotoMahasiswa f, "
				+ "ais.database.model.Mahasiswa m where f.mahasiswa = m.id and m.id in (:scopeIds) "
				+ "and m.nim is not null and m.nim <> ''", mahasiswaIds);
	}

	private static List<Object[]> daftarFoto(String hql, List<Long> mahasiswaIds) {
		Session s = null;
		List<Object[]> hasil = new ArrayList<Object[]>();
		try {
			s = HibernateUtil.getSessionFactory().openSession();
			org.hibernate.Query query = s.createQuery(hql);
			if (mahasiswaIds != null) query.setParameterList("scopeIds", mahasiswaIds);
			List<?> rows = query.list();
			for (Object o : rows) {
				Object[] r = (Object[]) o;
				if (r == null || r[0] == null || r[1] == null) {
					continue;
				}
				Long idFoto = ((Number) r[0]).longValue();
				String identitas = String.valueOf(r[1]).trim();
				if (identitas.isEmpty()) {
					continue;
				}
				String namaAsli = r.length > 2 && r[2] != null ? String.valueOf(r[2]) : null;
				hasil.add(new Object[] { idFoto, identitas + ekstensi(namaAsli) });
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/helper/DownloadFotoMassalHelper.java:169");
				}
			}
		}
		return hasil;
	}

	/**
	 * Baca BLOB tiap foto secara PARALEL (kolam thread, maks {@value #MAKS_THREAD}) lalu tulis ke satu
	 * berkas ZIP temp; kembalikan File-nya. {@code diproses}/{@code berhasil} di-update untuk bar progres
	 * ({@code diproses}=item selesai dicoba, {@code berhasil}=yang benar-benar masuk ZIP). Dipanggil dari
	 * utas latar (lihat {@link #prosesDownload}).
	 */
	private static File bacaKeZip(final List<Object[]> daftar, final Class<?> fotoClass, final AtomicInteger diproses,
			final AtomicInteger berhasil) throws Exception {
		final int total = daftar.size();
		if (total == 0) {
			File empty = File.createTempFile("foto_massal_", ".zip");
			ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(empty)));
			zip.close();
			return empty;
		}
		final int threads = Math.min(MAKS_THREAD, Math.max(1, total));
		final ExecutorService pool = Executors.newFixedThreadPool(threads);
		final CompletionService<Object[]> cs = new ExecutorCompletionService<Object[]>(pool);
		final Semaphore sem = new Semaphore(threads * 2);
		File tempZip = File.createTempFile("foto_massal_", ".zip");
		tempZip.deleteOnExit();

		// Submitter: kirim tugas baca-blob (dibatasi semaphore agar hasil menganggur di memori terbatas).
		Thread submitter = new Thread(new Runnable() {
			@Override
			public void run() {
				for (Object[] row : daftar) {
					final Long idFoto = (Long) row[0];
					final String namaBerkas = (String) row[1];
					try {
						sem.acquire();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
					try {
						cs.submit(new Callable<Object[]>() {
							@Override
							public Object[] call() {
								Session s = null;
								try {
									s = HibernateUtil.getSessionFactory().openSession();
									Object foto = s.get(fotoClass, idFoto);
									if (foto == null) {
										return null;
									}
									Blob blob = (Blob) foto.getClass().getMethod("getFoto").invoke(foto);
									if (blob == null) {
										return null;
									}
									byte[] bytes = bacaBlob(blob);
									return bytes == null || bytes.length == 0 ? null : new Object[] { namaBerkas, bytes };
								} catch (Exception e) {
									return null;
								} finally {
									if (s != null) {
										try {
											s.close();
										} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/helper/DownloadFotoMassalHelper.java:228");
										}
									}
								}
							}
						});
					} catch (Exception eSub) {
						sem.release();
					}
				}
			}
		});
		submitter.setDaemon(true);
		submitter.start();

		// Penulis (utas ini): ambil hasil begitu selesai → tulis ke ZIP.
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tempZip)));
		Set<String> dipakai = new HashSet<String>();
		try {
			for (int i = 0; i < total; i++) {
				Object[] res = null;
				try {
					Future<Object[]> f = cs.take();
					res = f.get();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/helper/DownloadFotoMassalHelper.java:252");
				} finally {
					sem.release();
					diproses.incrementAndGet();
				}
				if (res == null) {
					continue;
				}
				byte[] bytes = (byte[]) res[1];
				if (bytes == null || bytes.length == 0) {
					continue;
				}
				try {
					zos.putNextEntry(new ZipEntry(namaUnik(dipakai, (String) res[0])));
					zos.write(bytes);
					zos.closeEntry();
					berhasil.incrementAndGet();
				} catch (Exception eZip) { ais.common.ErrorAuditUtil.record(eZip, "auto-audit(empty-catch) src/ais/common/helper/DownloadFotoMassalHelper.java:269");
				}
			}
		} finally {
			try {
				zos.close();
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/helper/DownloadFotoMassalHelper.java:275");
			}
			pool.shutdown();
		}
		return tempZip;
	}

	/**
	 * Jalankan unduhan foto massal dengan <b>BAR PROGRES</b>: baca BLOB paralel di UTAS LATAR sambil
	 * bar progres ({@link Progressmeter}) diperbarui oleh {@link Timer} (persen = item selesai / total).
	 * Setelah selesai, jendela progres ditutup lalu ZIP dikirim ke browser ({@code Filedownload} di utas
	 * event). Status antar-utas dipertukarkan lewat {@code Atomic*} (jaminan visibilitas memori).
	 */
	private static void prosesDownload(final List<Object[]> daftar, final Class<?> fotoClass, final String namaZip)
			throws Exception {
		if (daftar == null || daftar.isEmpty()) {
			MyMessageboxConfig.show("Tidak ada foto yang dapat diunduh.", "Informasi", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return;
		}
		final int total = daftar.size();
		final AtomicInteger diproses = new AtomicInteger(0);
		final AtomicInteger berhasil = new AtomicInteger(0);
		final AtomicReference<File> hasilZip = new AtomicReference<File>();
		final AtomicReference<String> pesanError = new AtomicReference<String>();
		final AtomicBoolean selesai = new AtomicBoolean(false);

		// Jendela progres (tampilan modal, non-blok utas event).
		final org.zkoss.zk.ui.Component root = ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot();
		final MyWindow winProgress = new MyWindow("Download Foto Massal", "normal", false);
		winProgress.setWidth("460px");
		winProgress.setClosable(false);
		winProgress.setParent(root);
		Vbox boxP = new Vbox();
		boxP.setWidth("100%");
		boxP.setStyle("padding:16px 18px;");
		boxP.setParent(winProgress);
		new Label("Menyiapkan " + total + " foto ke dalam ZIP, harap tunggu...").setParent(boxP);
		final Progressmeter meter = new Progressmeter();
		meter.setValue(0);
		meter.setWidth("100%");
		meter.setParent(boxP);
		final Label lblPct = new Label("0%  (0 / " + total + ")");
		lblPct.setStyle("font-weight:800;color:#1e3a5f;font-size:14px;");
		lblPct.setParent(boxP);
		winProgress.doHighlighted();

		// Utas latar: baca paralel + rakit ZIP.
		Thread worker = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					hasilZip.set(bacaKeZip(daftar, fotoClass, diproses, berhasil));
				} catch (Exception e) {
					pesanError.set(String.valueOf(e.getMessage()));
				} finally {
					selesai.set(true);
				}
			}
		});
		worker.setDaemon(true);
		worker.start();

		// Timer: perbarui bar progres; saat selesai → tutup jendela + kirim ZIP.
		final Timer timer = new Timer(400);
		timer.setRepeats(true);
		timer.setParent(root);
		timer.addEventListener("onTimer", new EventListener() {
			@Override
			public void onEvent(Event e) throws Exception {
				int d = diproses.get();
				int pct = total > 0 ? (int) ((long) d * 100L / total) : 100;
				if (pct > 100) {
					pct = 100;
				}
				meter.setValue(pct);
				lblPct.setValue(pct + "%  (" + d + " / " + total + ")");
				if (selesai.get()) {
					try {
						timer.stop();
						timer.detach();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/helper/DownloadFotoMassalHelper.java:356");
					}
					try {
						winProgress.detach();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/helper/DownloadFotoMassalHelper.java:360");
					}
					if (pesanError.get() != null) {
						PesanFormalHelper.tampilkanGagal("penyusunan berkas ZIP unduhan foto massal",
								"Sistem gagal menyusun berkas ZIP dari kumpulan foto yang diproses. Keterangan "
										+ "teknis dari sistem: \"" + pesanError.get() + "\".",
								new String[] { "Coba ulangi proses unduh foto massal beberapa saat lagi.",
										"Jika jumlah foto sangat banyak, coba persempit filter data agar diproses "
												+ "secara bertahap." });
						return;
					}
					if (hasilZip.get() == null || berhasil.get() == 0) {
						MyMessageboxConfig.show("Tidak ada foto valid yang berhasil dibaca.", "Informasi",
								MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}
					Filedownload.save(new AMedia(namaZip, "zip", "application/zip", hasilZip.get(), true));
				}
			}
		});
		timer.start();
	}

	/** Baca seluruh isi BLOB menjadi byte[]. */
	private static byte[] bacaBlob(Blob blob) throws Exception {
		InputStream in = null;
		try {
			in = blob.getBinaryStream();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buf = new byte[8192];
			int n;
			while ((n = in.read(buf)) != -1) {
				baos.write(buf, 0, n);
			}
			return baos.toByteArray();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/helper/DownloadFotoMassalHelper.java:395");
				}
			}
		}
	}

	/** Ekstensi dari nama berkas asli (mis. {@code 12345.png} → {@code .png}); default {@code .jpg}. */
	private static String ekstensi(String nama) {
		if (nama != null) {
			String n = nama.replace('\\', '/');
			int slash = n.lastIndexOf('/');
			if (slash >= 0) {
				n = n.substring(slash + 1);
			}
			int dot = n.lastIndexOf('.');
			if (dot > 0 && dot < n.length() - 1) {
				String ext = n.substring(dot).toLowerCase();
				if (ext.length() <= 6) {
					return ext;
				}
			}
		}
		return ".jpg";
	}

	/** Pastikan nama entri ZIP unik (tambah {@code _1}, {@code _2}, ... bila bentrok). */
	private static String namaUnik(Set<String> dipakai, String nama) {
		String kandidat = nama;
		int i = 1;
		while (dipakai.contains(kandidat)) {
			int dot = nama.lastIndexOf('.');
			String base = dot > 0 ? nama.substring(0, dot) : nama;
			String ext = dot > 0 ? nama.substring(dot) : "";
			kandidat = base + "_" + i + ext;
			i++;
		}
		dipakai.add(kandidat);
		return kandidat;
	}
}
