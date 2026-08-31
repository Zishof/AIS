package ais.common;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.image.AImage;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.A;
import org.zkoss.zul.Html;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Image;
import org.zkoss.zul.Row;
import org.zkoss.zul.Vbox;

import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.Dosen;
import ais.database.model.GeneralValueObject;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.FotoBiodataCalonMahasiswa;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.FotoAnggotaKoperasi;
import ais.database.model.file.FotoDosen;
import ais.database.model.file.FotoGuru;
import ais.database.model.file.FotoMahasiswa;
import ais.database.model.file.FotoPegawai;
import ais.database.model.file.FotoSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.koperasi.AnggotaKoperasi;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;

public class ProfileImageUtil {

	private static final class TargetFotoProfil {
		final Serializable ref;
		final String jenis;
		final Class<? extends FileFotoLain> clazz;
		final String field;

		TargetFotoProfil(Serializable ref, String jenis,
				Class<? extends FileFotoLain> clazz, String field) {
			this.ref = ref;
			this.jenis = jenis;
			this.clazz = clazz;
			this.field = field;
		}
	}

	private static TargetFotoProfil targetFotoProfil(GeneralValueObject object) {
		GeneralValueObject base = ekstrakEntitasUtama(object);
		if (base == null) return null;
		if (base instanceof Mahasiswa)
			return new TargetFotoProfil(base.getId(), FotoMahasiswa.DEFAULT_JENIS, FotoMahasiswa.class, "mahasiswa");
		if (base instanceof Siswa)
			return new TargetFotoProfil(base.getId(), FotoSiswa.DEFAULT_JENIS, FotoSiswa.class, "siswa");
		if (base instanceof CalonSiswa)
			return new TargetFotoProfil(base.getId(), FotoCalonSiswa.DEFAULT_JENIS, FotoCalonSiswa.class, "calonSiswa");
		if (base instanceof Dosen)
			return new TargetFotoProfil(base.getId(), FotoDosen.DEFAULT_JENIS, FotoDosen.class, "dosen");
		if (base instanceof Guru)
			return new TargetFotoProfil(base.getId(), FotoGuru.DEFAULT_JENIS, FotoGuru.class, "guru");
		if (base instanceof BiodataCalonMahasiswa)
			return new TargetFotoProfil(base.getId(), FotoBiodataCalonMahasiswa.DEFAULT_JENIS,
					FotoBiodataCalonMahasiswa.class, "biodataCalonMahasiswa");
		if (base instanceof Pegawai)
			return new TargetFotoProfil(base.getId(), FotoPegawai.DEFAULT_JENIS, FotoPegawai.class, "pegawai");
		if (base instanceof Tbmuser) {
			String userId = ((Tbmuser) base).getUserId();
			return userId == null ? null
					: new TargetFotoProfil(userId, FotoAdmin.DEFAULT_JENIS, FotoAdmin.class, "tbmuser");
		}
		// Member POS mandiri (tanpa tautan siswa/mahasiswa/pengguna) punya rumah fotonya
		// sendiri sejak 31-08 -- sebelumnya SELALU ditolak "belum ditautkan".
		if (base instanceof AnggotaKoperasi)
			return new TargetFotoProfil(base.getId(), FotoAnggotaKoperasi.DEFAULT_JENIS,
					FotoAnggotaKoperasi.class, "anggotaKoperasi");
		return null;
	}

	/**
	 * Mengganti atau menghapus satu foto profil memakai tabel foto profil lama
	 * yang sama dengan web eCampus. {@code bytes == null} berarti hapus.
	 * Dengan demikian foto yang diunggah POS langsung terbaca di semua layar
	 * lama dan tidak membuat silo media khusus aplikasi desktop.
	 */
	@SuppressWarnings("deprecation")
	public static String simpanFotoDariObject(GeneralValueObject object, byte[] bytes,
			String namaFile, String mimeType, Tbmuser oleh) throws Exception {
		TargetFotoProfil target = targetFotoProfil(object);
		if (target == null || target.ref == null) {
			throw new IllegalArgumentException(
					"Member belum ditautkan ke data pengguna/sivitas sehingga foto profil belum dapat disimpan.");
		}

		Session session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
		try {
			FileFotoLain lama = (FileFotoLain) session.createCriteria(target.clazz)
					.add(Restrictions.eq(target.field, target.ref))
					.setMaxResults(1).uniqueResult();
			session.beginTransaction();
			if (lama != null) session.delete(lama);
			if (bytes != null) {
				FileFotoLain baru = target.clazz.newInstance();
				target.clazz.getMethod("set" + Character.toUpperCase(target.field.charAt(0))
						+ target.field.substring(1), target.ref instanceof String ? String.class : Long.class)
						.invoke(baru, target.ref);
				target.clazz.getMethod("setNama", String.class).invoke(baru,
						(namaFile == null || namaFile.trim().isEmpty()) ? "foto-profil.jpg" : namaFile.trim());
				target.clazz.getMethod("setKeterangan", String.class).invoke(baru,
						(mimeType == null || mimeType.trim().isEmpty()) ? "image/jpeg" : mimeType.trim());
				target.clazz.getMethod("setFoto", java.sql.Blob.class).invoke(baru, Hibernate.createBlob(bytes));
				if (oleh != null) {
					target.clazz.getMethod("setOleh", String.class).invoke(baru, oleh.getUserNama());
					target.clazz.getMethod("setOlehId", String.class).invoke(baru, String.valueOf(oleh.getUserId()));
				}
				session.save(baru);
			}
			session.getTransaction().commit();
		} catch (Exception e) {
			if (session.getTransaction() != null && session.getTransaction().isActive())
				session.getTransaction().rollback();
			throw e;
		} finally {
			if (session.isOpen()) session.close();
		}

		// Paksa refresh cache FileFotoLain; tanpa ini cache negatif/lama dapat
		// membuat foto baru belum tampak sampai proses server di-restart.
		FileFotoLain.ambil(target.ref, target.jenis, target.clazz, true);
		return bytes == null ? "" : getUrlFotoDariObject(object, true);
	}

	private static boolean isProtectedEcampusLampiranUrl(String url) {
		if (url == null) {
			return false;
		}
		String lower = url.trim().toLowerCase();
		return lower.contains("/al?d=") || lower.contains("ambillampiran");
	}

	private static String escapeAttr(String value) {
		if (value == null) {
			return "";
		}
		return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String protectedOfficePreviewInfo(String link) {
		return "<div style='margin:8px 0;padding:12px 14px;font-family:Arial,sans-serif;"
				+ "color:#334155;background:#f8fafc;border:1px solid #cbd5e1;border-radius:8px;line-height:1.45;'>"
				+ "<b>Preview dokumen Office tidak tersedia di sini.</b><br/>"
				+ "Berkas ini dilindungi login eCampus. Google Viewer tidak membawa sesi login pengguna, "
				+ "sehingga yang terbaca bisa halaman login, bukan isi dokumen."
				+ "<div style='margin-top:8px;'><a href='" + escapeAttr(link)
				+ "' target='_blank' rel='noopener noreferrer' "
				+ "style='display:inline-block;padding:6px 10px;border-radius:4px;background:#1d4ed8;"
				+ "color:#fff;text-decoration:none;font-weight:600;'>Buka / unduh lewat eCampus</a></div>"
				+ "</div>";
	}

	public static A tampilkanGambar(final String url, String ukuran, String align) throws Exception {
		A a = new A();

		Image image = new Image(url);
		image.setHeight(ukuran);
		image.setAlign(align);
		image.setWidth("100%");
		image.setSclass("gambar_profile");

		a.appendChild(image);
		return a;
	}

	public static A tampilkanGambarKecil(GeneralValueObject object, String ukuran, String align) throws Exception {
		if (object == null) {
			return new A();
		}

		// Langsung generate URL tanpa membuat instance Tbmuser baru.
		// Jadikan final agar bisa diakses di dalam anonymous class EventListener (Java
		// 1.6/1.7)
		String urlFotoKecil = getUrlFotoDariObject(object, true);
		final String urlFotoBesar = getUrlFotoDariObject(object, false);

		A a = new A();
		Image image = new Image(urlFotoKecil);
		image.setHeight(ukuran);
		image.setStyle("max-width: 70px !important;min-width: 40px !important;min-height: 70px !important;");
		image.setSclass("gambar_profile");
		try {
			image.setWidgetListener("onError",
					"if(this.src!='" + urlFotoBesar + "'){this.src='" + urlFotoBesar + "';}");
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ProfileImageUtil.java:tampilkanGambarKecil-onError"); }

		a.appendChild(image);
		a.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.previewGambar(urlFotoBesar);
			}
		});

		return a;
	}

	/**
	 * Method baru yang sangat efisien memori. Berfungsi mengekstrak target (ID dan
	 * Class) langsung dari Object untuk mencari Foto tanpa perlu membungkusnya
	 * menjadi Tbmuser terlebih dahulu.
	 */
	public static String getUrlFotoDariObject(GeneralValueObject object, boolean kecil) throws Exception {
		if (object == null) {
			return Common.ROOT + "/img/user_default.png";
		}

		GeneralValueObject baseEntity = ekstrakEntitasUtama(object);
		if (baseEntity == null) {
			return Common.ROOT + "/img/user_default.png";
		}

		Serializable targetId = null;
		String targetJenis = null;
		Class<?> targetClass = null;

		if (baseEntity instanceof Mahasiswa) {
			targetId = baseEntity.getId();
			targetJenis = FotoMahasiswa.DEFAULT_JENIS;
			targetClass = FotoMahasiswa.class;
		} else if (baseEntity instanceof Siswa) {
			targetId = baseEntity.getId();
			targetJenis = FotoSiswa.DEFAULT_JENIS;
			targetClass = FotoSiswa.class;
		} else if (baseEntity instanceof CalonSiswa) {
			targetId = baseEntity.getId();
			targetJenis = FotoCalonSiswa.DEFAULT_JENIS;
			targetClass = FotoCalonSiswa.class;
		} else if (baseEntity instanceof Dosen ) {
			targetId = baseEntity.getId();
			targetJenis = FotoDosen.DEFAULT_JENIS;
			targetClass = FotoDosen.class;
		} else if (baseEntity instanceof Guru ) {
			targetId = baseEntity.getId();
			targetJenis = FotoGuru.DEFAULT_JENIS;
			targetClass = FotoGuru.class;
		} else if (baseEntity instanceof BiodataCalonMahasiswa) {
			targetId = baseEntity.getId();
			targetJenis = FotoBiodataCalonMahasiswa.DEFAULT_JENIS;
			targetClass = FotoBiodataCalonMahasiswa.class;
		} else if (baseEntity instanceof Pegawai) { // Hanya fallback jika bukan Guru/Dosen
			targetId = baseEntity.getId();
			targetJenis = FotoPegawai.DEFAULT_JENIS;
			targetClass = FotoPegawai.class;
		} else if (baseEntity instanceof Tbmuser) { // Kasus untuk FotoAdmin murni (tanpa relasi)
			targetId = ((Tbmuser) baseEntity).getUserId();
			targetJenis = FotoAdmin.DEFAULT_JENIS;
			targetClass = FotoAdmin.class;
		} else if (baseEntity instanceof AnggotaKoperasi) { // member POS mandiri (31-08)
			targetId = baseEntity.getId();
			targetJenis = FotoAnggotaKoperasi.DEFAULT_JENIS;
			targetClass = FotoAnggotaKoperasi.class;
		} else if (baseEntity instanceof PenyediaAsset) {
			// Logika Khusus Penyedia Asset
			Long idAsset = (Long) baseEntity.getId();
			if (idAsset != null) {
				Map<String, String> data = PenyediaAsset.galeries.get(idAsset);
				if (data == null) {
					PenyediaAsset.reloadGaleries((PenyediaAsset) baseEntity);
					data = PenyediaAsset.galeries.get(idAsset);
				}
				if (data != null && !data.isEmpty()) {
					return data.keySet().iterator().next();
				}
			}
			return Common.getRequestHostWithProtocol() + "/img/" + FileFotoLain.iconNggakAda(PenyediaAsset.class);
		}

		// Eksekusi pencarian foto
		if (targetId != null && targetJenis != null && targetClass != null) {
			FileFotoLain fileFotoLain = FileFotoLain.ambil(targetId, targetJenis, targetClass);
			if (fileFotoLain != null) {
				return urlFotoProfil(fileFotoLain, targetClass, kecil);
			}
			// Fallback: jika foto Mahasiswa tidak ditemukan (fileFotoLain==null) dan object
			// aslinya BiodataCalonMahasiswa, gunakan foto yang diupload saat pendaftaran
			// (FotoBiodataCalonMahasiswa). Kasus umum: Calon sudah dapat NIM tapi foto belum
			// dipindah ke tabel FotoMahasiswa.
			if (fileFotoLain == null && object instanceof BiodataCalonMahasiswa
					&& baseEntity instanceof Mahasiswa) {
				BiodataCalonMahasiswa bcm = (BiodataCalonMahasiswa) object;
				if (bcm.getId() != null) {
					try {
						FileFotoLain fotocalon = FileFotoLain.ambil(bcm.getId(),
								FotoBiodataCalonMahasiswa.DEFAULT_JENIS, FotoBiodataCalonMahasiswa.class);
						if (fotocalon != null) {
							return urlFotoProfil(fotocalon, FotoBiodataCalonMahasiswa.class, kecil);
						}
					} catch (Exception _exFallback) { ais.common.ErrorAuditUtil.record(_exFallback, "auto-audit(empty-catch) src/ais/common/ProfileImageUtil.java:184"); /* abaikan, lanjut ke default */ }
				}
			}
			if (baseEntity instanceof Mahasiswa) {
				FileFotoLain fotoCalonDariMahasiswa = cariFotoCalonDariMahasiswa((Mahasiswa) baseEntity);
				if (fotoCalonDariMahasiswa != null) {
					return urlFotoProfil(fotoCalonDariMahasiswa, FotoBiodataCalonMahasiswa.class, kecil);
				}
			}
			return FileFotoLain.ambilLinkLampiranLain(fileFotoLain, false, false, targetClass, true, false, false);
		}

		return Common.ROOT + "/img/user_default.png";
	}

	private static String urlFotoProfil(FileFotoLain fileFotoLain, Class<?> targetClass, boolean kecil) throws Exception {
		if (fileFotoLain == null) {
			return Common.ROOT + "/img/user_default.png";
		}
		if (fileFotoLain.getGdrive() != null && !fileFotoLain.getGdrive().isEmpty()) {
			// URL export/download Google Drive sering menolak dirender langsung sebagai
			// <img>, walaupun tetap berhasil ketika dibuka pada popup. Untuk kartu/list
			// gunakan endpoint thumbnail; preview klik tetap memakai URL ukuran penuh.
			return kecil ? fileFotoLain.thumbnailGDriveUrl() : fileFotoLain.createLinkUri();
		}
		return FileFotoLain.ambilLinkLampiranLain(fileFotoLain, false, false, targetClass, true, false, false);
	}

	private static FileFotoLain cariFotoCalonDariMahasiswa(Mahasiswa mahasiswa) {
		if (mahasiswa == null || mahasiswa.getId() == null) {
			return null;
		}
		try {
			Long biodataCalonMahasiswaId = mahasiswa.getBiodataCalonMahasiswa();
			if (biodataCalonMahasiswaId != null && biodataCalonMahasiswaId.longValue() > 0L) {
				FileFotoLain foto = FileFotoLain.ambil(biodataCalonMahasiswaId,
						FotoBiodataCalonMahasiswa.DEFAULT_JENIS, FotoBiodataCalonMahasiswa.class);
				if (foto != null) {
					return foto;
				}
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/common/ProfileImageUtil.java:cariFotoCalonDariMahasiswa-biodataId");
		}
		Session session = null;
		try {
			session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
			BiodataCalonMahasiswa biodata = (BiodataCalonMahasiswa) session
					.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mahasiswa))
					.setMaxResults(1)
					.uniqueResult();
			if (biodata != null && biodata.getId() != null) {
				return FileFotoLain.ambil(biodata.getId(), FotoBiodataCalonMahasiswa.DEFAULT_JENIS,
						FotoBiodataCalonMahasiswa.class);
			}
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit src/ais/common/ProfileImageUtil.java:cariFotoCalonDariMahasiswa");
		} finally {
			if (session != null && session.isOpen()) {
				try {
					session.close();
				} catch (Exception e) {
					ais.common.ErrorAuditUtil.record(e,
							"auto-audit(empty-catch) src/ais/common/ProfileImageUtil.java:cariFotoCalonDariMahasiswa-close");
				}
			}
		}
		return null;
	}

	/**
	 * Mencari baris {@link FileFotoLain} yang cocok untuk {@code object}, lewat rantai resolusi
	 * "bongkar wrapper" YANG SAMA dengan {@link #getUrlFotoDariObject} (mis. {@code AnggotaKoperasi}
	 * -&gt; {@code Siswa}/{@code Mahasiswa}/{@code Tbmuser} berantai lewat {@link #ekstrakEntitasUtama}).
	 *
	 * <p><b>Kenapa method terpisah, bukan mengubah {@link #getUrlFotoDariObject}.</b> Pemanggil yang
	 * hanya butuh METADATA berkas (nama, ukuran -- utk deteksi-perubahan sinkron offline Kasir
	 * Desktop, lihat {@code KantinHelper.anggotaSyncList}) tidak butuh URL siap-pakai. Method ini
	 * SENGAJA punya salinan kecil pemetaan instanceof-ke-kelas-Foto yang sama (bukan memanggil
	 * {@code getUrlFotoDariObject} lalu mem-parse baliknya) supaya {@code getUrlFotoDariObject} --
	 * dipakai luas di seluruh JSP/ZK sistem -- TIDAK perlu disentuh/direfactor sama sekali, nol risiko
	 * regresi pada method yang sudah stabil.</p>
	 *
	 * <p>TIDAK menangani kasus khusus {@link PenyediaAsset} (foto galeri banyak, bukan satu baris
	 * {@code FileFotoLain}) -- kembalikan {@code null} untuk kasus itu, sama seperti objek yang
	 * memang tidak punya foto sama sekali.</p>
	 *
	 * @return baris {@code FileFotoLain} yang cocok, atau {@code null} bila objek tidak punya foto.
	 */
	public static FileFotoLain cariFileFotoLain(GeneralValueObject object) throws Exception {
		if (object == null) {
			return null;
		}

		GeneralValueObject baseEntity = ekstrakEntitasUtama(object);
		if (baseEntity == null) {
			return null;
		}

		Serializable targetId = null;
		String targetJenis = null;
		Class<?> targetClass = null;

		if (baseEntity instanceof Mahasiswa) {
			targetId = baseEntity.getId();
			targetJenis = FotoMahasiswa.DEFAULT_JENIS;
			targetClass = FotoMahasiswa.class;
		} else if (baseEntity instanceof Siswa) {
			targetId = baseEntity.getId();
			targetJenis = FotoSiswa.DEFAULT_JENIS;
			targetClass = FotoSiswa.class;
		} else if (baseEntity instanceof CalonSiswa) {
			targetId = baseEntity.getId();
			targetJenis = FotoCalonSiswa.DEFAULT_JENIS;
			targetClass = FotoCalonSiswa.class;
		} else if (baseEntity instanceof Dosen) {
			targetId = baseEntity.getId();
			targetJenis = FotoDosen.DEFAULT_JENIS;
			targetClass = FotoDosen.class;
		} else if (baseEntity instanceof Guru) {
			targetId = baseEntity.getId();
			targetJenis = FotoGuru.DEFAULT_JENIS;
			targetClass = FotoGuru.class;
		} else if (baseEntity instanceof BiodataCalonMahasiswa) {
			targetId = baseEntity.getId();
			targetJenis = FotoBiodataCalonMahasiswa.DEFAULT_JENIS;
			targetClass = FotoBiodataCalonMahasiswa.class;
		} else if (baseEntity instanceof Pegawai) {
			targetId = baseEntity.getId();
			targetJenis = FotoPegawai.DEFAULT_JENIS;
			targetClass = FotoPegawai.class;
		} else if (baseEntity instanceof Tbmuser) {
			targetId = ((Tbmuser) baseEntity).getUserId();
			targetJenis = FotoAdmin.DEFAULT_JENIS;
			targetClass = FotoAdmin.class;
		}

		if (targetId != null && targetJenis != null && targetClass != null) {
			return FileFotoLain.ambil(targetId, targetJenis, targetClass);
		}
		return null;
	}

	/**
	 * Membongkar bungkus (wrapper) objek untuk mendapatkan entitas spesifik di
	 * dalamnya
	 */
	private static GeneralValueObject ekstrakEntitasUtama(GeneralValueObject object) {
		if (object instanceof AnggotaKoperasi) {
			AnggotaKoperasi ak = (AnggotaKoperasi) object;
			if (ak.getSiswa() != null)
				return ak.getSiswa();
			if (ak.getMahasiswa() != null)
				return ak.getMahasiswa();
			if (ak.getTbmuser() != null)
				return ekstrakEntitasUtama(ak.getTbmuser());
			return ak; // member mandiri: fotonya disimpan atas nama anggota itu sendiri
		} else if (object instanceof BiodataCalonMahasiswa) {
			BiodataCalonMahasiswa bcm = GeneralValueObject.check((BiodataCalonMahasiswa) object);
			if (bcm != null && bcm.getMahasiswa() != null)
				return bcm.getMahasiswa();
			return bcm;
		} else if (object instanceof Pegawai) {
			Pegawai peg = GeneralValueObject.check((Pegawai) object);
			if (peg != null) {
				if (peg.getGuru() != null)
					return peg.getGuru();
				if (peg.getDosen() != null)
					return peg.getDosen();
				return peg;
			}
		} else if (object instanceof Tbmuser) {
			Tbmuser tu = GeneralValueObject.check((Tbmuser) object);
			if (tu != null) {
				if (tu.getMahasiswa() != null)
					return tu.getMahasiswa();
				if (tu.getSiswa() != null)
					return tu.getSiswa();
				if (tu.getDosen() != null)
					return tu.getDosen();
				if (tu.getGuru() != null)
					return tu.getGuru();
				if (tu.getPegawai() != null)
					return ekstrakEntitasUtama(tu.getPegawai());
				if (tu.getBiodataCalonMahasiswa() != null)
					return ekstrakEntitasUtama(tu.getBiodataCalonMahasiswa());
				if (tu.getCalonSiswa() != null)
					return tu.getCalonSiswa();
				if (tu.getCalonPegawai() != null)
					return tu.getCalonPegawai();
				return tu; // Kembalikan Tbmuser itu sendiri jika ini adalah Admin murni
			}
		}
		return object;
	}

	/**
	 * Dipertahankan untuk kompatibilitas mundur (Backward Compatibility) Meneruskan
	 * panggilan langsung ke helper yang baru.
	 */
	public static String getUrlFotoPengguna(Tbmuser tbmuser, Integer height, Integer width) throws Exception {
		boolean kecil = (height != null && height < 160);
		return getUrlFotoDariObject(tbmuser, kecil);
	}

	// DRY helper method untuk mendapatkan URL preview dengan aman
	private static String getPreviewUrlHelper(FileFoto fileFoto) throws Exception {
		if (fileFoto != null && fileFoto instanceof FileFotoLain) {
			return ((FileFotoLain) fileFoto).createLinkUri();
		} else if (fileFoto != null) {
			File file = fileFoto.ambilFile();
			return LampiranLain.ambilLinkLampiranLain(file);
		}
		return "";
	}

	public static String preview(FileFoto fileFoto) throws Exception {
		if (!fileFoto.bisaPreview())
			return "";

		String nama = fileFoto.getNama();
		if (fileFoto.getGdrive() != null && !fileFoto.getGdrive().isEmpty()) {
			return "<iframe src=\"https://drive.google.com/file/d/" + fileFoto.getGdrive() + "/preview\" "
					+ Common.getStyleContent() + "></iframe>";
		}

		String u = getPreviewUrlHelper(fileFoto);

		if (fileFoto.merupakanGambar()) {
			return "<img style=\"width:95%;\" src='" + u + "'/>";
		} else if (nama.toLowerCase().endsWith(".pdf")) {
			return "<iframe src=\"" + u + "\" " + Common.getStyleContent() + "></iframe>";
		} else if (isProtectedEcampusLampiranUrl(u)) {
			return protectedOfficePreviewInfo(u);
		} else {
			String link = "https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(u, "UTF-8");
			return "<iframe src=\"" + link + "\" " + Common.getStyleContent() + "></iframe>";
		}
	}

	public static void preview(FileFoto fileFoto, Component parent) throws Exception {
		if (!fileFoto.bisaPreview())
			return;

		String nama = fileFoto.getNama();
		boolean isMobileLayout = parent instanceof Vbox || parent instanceof Row || Common.isMobile();

		if (fileFoto.getGdrive() != null && !fileFoto.getGdrive().isEmpty()) {
			Html html = new ais.ui.util.MyHtml("<iframe src=\"https://drive.google.com/file/d/" + fileFoto.getGdrive()
					+ "/preview\" " + Common.getStyleContent() + "></iframe>");
			html.setAttribute("lampiran_tambahan", true);
			html.setParent(parent);
			return;
		}

		if (fileFoto.merupakanGambar()) {
			String u = getPreviewUrlHelper(fileFoto);
			Image include = new Image(u);
			if (isMobileLayout) {
				include.setWidth("100%");
			} else {
				include.setHeight("256px");
			}
			include.setStyle("border:none;");
			parent.appendChild(include);

		} else if (nama.toLowerCase().endsWith(".pdf") && !Common.isMobile()) {
			String u;
			if (fileFoto instanceof FileFotoLain) {
				u = ((FileFotoLain) fileFoto).createLinkUri();
				File file = ((FileFotoLain) fileFoto).ambilFile();
				if (file != null && file.exists()) {
					u = LampiranLain.ambilLinkLampiranLain(file);
				}
			} else {
				File file = getFileFotoLangsungOld(fileFoto, false);
				u = LampiranLain.ambilLinkLampiranLain(file);
			}

			Iframe iframe = new Iframe(u);
			if (isMobileLayout) {
				iframe.setWidth("100%");
				iframe.setHeight("600px");
			} else {
				iframe.setWidth("640px");
				iframe.setHeight("400px");
			}
			iframe.setStyle("border:none;width:100%;");
			parent.appendChild(iframe);

		} else {
			String u;
			if (fileFoto instanceof FileFotoLain) {
				u = ((FileFotoLain) fileFoto).createLinkUri();
				File file = ((FileFotoLain) fileFoto).ambilFile();
				if (file != null && file.exists()) {
					u = LampiranLain.ambilLinkLampiranLain(file);
				}
			} else {
				File file = getFileFotoLangsungOld(fileFoto, false);
				HttpServletRequest request = null;
				if (ExecutionsCtrl.getCurrent() != null) {
					request = (HttpServletRequest) ExecutionsCtrl.getCurrent().getNativeRequest();
				}

				if (request == null) {
					request = RequestContext.get();
				}
				if (request != null && file != null) {
					u = (Common.isSecure(request) ? "https://" : "http://") + request.getServerName()
							+ (request.getServerPort() == 80 || request.getServerPort() == 443 ? ""
									: ":" + request.getServerPort())
							+ "/media/" + file.getName();
				} else {
					u = "";
				}

				if (file != null && file.exists()) {
					u = LampiranLain.ambilLinkLampiranLain(file);
				}
			}

			if (isProtectedEcampusLampiranUrl(u)) {
				Html info = new ais.ui.util.MyHtml(protectedOfficePreviewInfo(u));
				info.setAttribute("lampiran_tambahan", true);
				info.setParent(parent);
			} else {
				Iframe iframe = new Iframe(
						"https://docs.google.com/gview?embedded=true&url=" + URLEncoder.encode(u, "UTF-8"));
				if (isMobileLayout) {
					iframe.setWidth("100%");
					iframe.setHeight("600px");
				} else {
					iframe.setWidth("640px");
					iframe.setHeight("400px");
				}
				iframe.setStyle("border:none;width:100%;");
				parent.appendChild(iframe);
			}
		}
	}

	public static String loadFileFotoLangsung(Tbmuser tbmuser, Integer height, Integer width, Boolean berupaGambar)
			throws Exception {
		File file = new File(Common.REAL_PATH + "/img/administrator-icon.png");

		try {
			if (tbmuser == null) {
				return file.getAbsolutePath();
			}
			if (tbmuser.getMahasiswa() != null && tbmuser.getMahasiswa().getId() != null) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(tbmuser.getMahasiswa().getId(),
						FotoMahasiswa.DEFAULT_JENIS, FotoMahasiswa.class);
				if (fileFotoLain == null) return file.getAbsolutePath();
				if (fileFotoLain.getGdrive() != null)
					return fileFotoLain.thumbnailGDriveUrl();
				file = fileFotoLain.ambilFile();

			} else if (tbmuser.ambilDosen() != null && tbmuser.getDosen().getId() != null) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(tbmuser.getDosen().getId(), FotoDosen.DEFAULT_JENIS,
						FotoDosen.class);
				if (fileFotoLain == null) return file.getAbsolutePath();
				if (fileFotoLain.getGdrive() != null)
					return fileFotoLain.thumbnailGDriveUrl();
				file = fileFotoLain.ambilFile();

			} else if (tbmuser.getSiswa() != null && tbmuser.getSiswa().getId() != null) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(tbmuser.getSiswa().getId(), FotoSiswa.DEFAULT_JENIS,
						FotoSiswa.class);
				if (fileFotoLain == null) return file.getAbsolutePath();
				if (fileFotoLain.getGdrive() != null)
					return fileFotoLain.thumbnailGDriveUrl();
				file = fileFotoLain.ambilFile();

			} else if (tbmuser.ambilGuru() != null && tbmuser.getGuru().getId() != null) {
				FileFotoLain fileFotoLain = FileFotoLain.ambil(tbmuser.getGuru().getId(), FotoGuru.DEFAULT_JENIS,
						FotoGuru.class);
				if (fileFotoLain == null) return file.getAbsolutePath();
				if (fileFotoLain.getGdrive() != null)
					return fileFotoLain.thumbnailGDriveUrl();
				file = fileFotoLain.ambilFile();

			} else if (tbmuser.ambilPegawai() != null && tbmuser.getPegawai().getId() != null) {
				if (tbmuser.ambilPegawai().getDosen() != null) {
					FileFotoLain fileFotoLain = FileFotoLain.ambil(tbmuser.ambilPegawai().getDosen().getId(),
							FotoDosen.DEFAULT_JENIS, FotoDosen.class);
					if (fileFotoLain == null) return file.getAbsolutePath();
					if (fileFotoLain.getGdrive() != null)
						return fileFotoLain.thumbnailGDriveUrl();
					file = fileFotoLain.ambilFile();
				} else {
					FileFotoLain fileFotoLain = FileFotoLain.ambil(tbmuser.getPegawai().getId(),
							FotoPegawai.DEFAULT_JENIS, FotoPegawai.class);
					if (fileFotoLain == null) return file.getAbsolutePath();
					if (fileFotoLain.getGdrive() != null)
						return fileFotoLain.thumbnailGDriveUrl();
					file = fileFotoLain.ambilFile();
				}

			} else if (tbmuser != null && tbmuser.getUserId() != null) {
				Session streamingSession = null;
				try {
					streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					FotoAdmin fotoAdmin = (FotoAdmin) streamingSession.createCriteria(FotoAdmin.class)
							.add(Restrictions.eq("tbmuser", tbmuser.getUserId())).setMaxResults(1).uniqueResult();
					file = getFileFotoLangsungOld(fotoAdmin, height, width, berupaGambar);
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
					try {
						StreamingHibernateUtil.getInstance().rollbackTransaction();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/ProfileImageUtil.java:439");
					} // suppress rollback error
				} finally {
					// PERBAIKAN: Penutupan session secara ketat (Java 1.6 style)
					if (streamingSession != null && streamingSession.isOpen()) {
						try {
							streamingSession.disconnect();
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/ProfileImageUtil.java:446");
						}
						try {
							streamingSession.close();
						} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/ProfileImageUtil.java:450");
						}
					}
					try {
						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/common/ProfileImageUtil.java:455");
					}
				}
			}

			if (file == null || !file.exists() || (berupaGambar && !Common.isImage(file))) {
				return new File(Common.REAL_PATH + "/img/administrator-icon.png").getAbsolutePath();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ProfileImageUtil.java:463");
			// Handle silent fallback
		}

		return file == null ? new File(Common.REAL_PATH + "/img/administrator-icon.png").getAbsolutePath()
				: file.getAbsolutePath();
	}

	public static Image loadFotoPenggunaLangsung(Tbmuser tbmuser, Image foto, Integer height, Integer width)
			throws Exception {
		if (foto == null) {
			foto = new Image("/img/administrator-icon.png");
		}
		if (height != null)
			foto.setHeight(height + "px");
		if (width != null)
			foto.setWidth(width + "px");

		// PERINGATAN PERFORMA: new AImage(...) memuat byte gambar secara utuh ke
		// Memory/RAM.
		foto.setContent(new AImage(loadFileFotoLangsung(tbmuser, height, width, true)));
		return foto;
	}

	public static File getFileFotoLangsungOld(FileFoto foto, boolean berupaGambar) throws Exception {
		return getFileFotoLangsungOld(foto, null, null, berupaGambar);
	}

	public static File getFileFotoLangsungOld(FileFoto foto, Integer height, Integer width, boolean berupaGambar)
			throws Exception {
		if (foto == null) {
			return new File(Common.REAL_PATH + "/img/administrator-icon.png");
		}

		String safeName = foto.getNama() == null ? "" : foto.getNama().replaceAll(" ", "_");
		File file = new File(CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + foto.getId() + "__"
				+ foto.getClass().getName() + "_" + URLEncoder.encode(safeName, "UTF-8"));

		if (foto.getGdrive() != null) {
			try {
				FileUtils.copyURLToFile(new URL(foto.downloadGDriveUrl()), file);
				return file;
			} catch (Exception e) {
				ais.common.BacaTulisUtil.tulis(file = new File(file.getAbsolutePath() + ".txt"),
						foto.downloadGDriveUrl());
				return file;
			}
		} else if (foto.ambilLink() != null && !foto.ambilLink().isEmpty()) {
			if (foto.ambilLink().toLowerCase().contains("dropbox")) {
				try {
					FileUtils.copyURLToFile(new URL(foto.dropboxLinkRaw()), file);
					return file;
				} catch (Exception e) {
					ais.common.BacaTulisUtil.tulis(file = new File(file.getAbsolutePath() + ".txt"), foto.ambilLink());
					return file;
				}
			} else {
				ais.common.BacaTulisUtil.tulis(file = new File(file.getAbsolutePath() + ".txt"), foto.ambilLink());
				return file;
			}
		}

		if (file == null || !file.exists()) {
			Common.copy(foto.ambilFile(), file);
		}

		try {
			if (height != null && width != null) {
				File filekecil = new File(CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + height + "px_"
						+ width + "px_" + foto.getId() + "_" + foto.getClass().getName() + "_" + safeName);

				// PERINGATAN PERFORMA: Proses resize ini berjalan synchronous dan membebani CPU
				if (!filekecil.exists()) {
					CommonMedia.resizeImage(file, width, height, filekecil);
				}
				file = filekecil;
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
		}

		if (berupaGambar && !Common.isImage(file)) {
			return new File(Common.REAL_PATH + "/img/administrator-icon_default.png");
		}

		return file;
	}
}
