package ais.database.model.file;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.sql.Blob;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.imgscalr.Scalr;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Box;
import org.zkoss.zul.Button;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.generic.AmbilDataLampiranFileLain;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswa;
import ais.database.model.asset.PenyediaAsset;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyJSONObject;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.SetelahUpload;
import ais.ui.util.WaktuUtil;

/**
 * Model data untuk file foto lain. Tipe ini membawa state yang dipertukarkan oleh lapisan
 * persistence, service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang
 * dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * FileFoto}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini; perubahan yang
 * berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau tumpang
 * tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code Map RELASI_MAP}, {@code long
 * SOFT_DELETE_ID}; inisialisasi/lifecycle ({@code setupJurusanCombo()}, {@code setupDownloadButtonAction()});
 * pembacaan/pencarian ({@code ambilRef()}, {@code ambilClazz()}, {@code getJenis()}, {@code getLink()}, {@code
 * getOlehId()}, {@code getOleh()}); mutasi data ({@code setUrl()}, {@code resetLokasi()}, {@code
 * hapusAtauUpdate()}); penghapusan/pembatalan ({@code delete()}, {@code performDelete()}); operasi domain lain
 * ({@code createLinkUri()}, {@code createLinkUri()}, {@code createLinkUri()}, {@code iconNggakAda()}, {@code
 * iconNggakAda()}, {@code tulisLokasi()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface
 * yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see FileFoto
 */
@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
public abstract class FileFotoLain extends FileFoto {

	// Konfigurasi Mapping Kelas ke Nama Field Relasi (Menggantikan if-else raksasa)
	private static final Map<Class<?>, String> RELASI_MAP = new HashMap<Class<?>, String>();
	private static final long SOFT_DELETE_ID = -111111119L;

	static {
		RELASI_MAP.put(LampiranLain.class, "ref");
		RELASI_MAP.put(LampiranLainMahasiswa.class, "mahasiswa");
		RELASI_MAP.put(LampiranLainBiodataCalonMahasiswa.class, "biodataCalonMahasiswa");
		RELASI_MAP.put(FotoMahasiswa.class, "mahasiswa");
		RELASI_MAP.put(FotoMahasiswaLulus.class, "mahasiswa");
		RELASI_MAP.put(FotoBiodataCalonMahasiswa.class, "biodataCalonMahasiswa");
		RELASI_MAP.put(FotoDosen.class, "dosen");
		RELASI_MAP.put(FotoPegawai.class, "pegawai");
		RELASI_MAP.put(FotoGuru.class, "guru");
		RELASI_MAP.put(FotoCalonSiswa.class, "calonSiswa");
		RELASI_MAP.put(FotoCalonPegawai.class, "calonPegawai");
		RELASI_MAP.put(FotoSiswa.class, "siswa");
		RELASI_MAP.put(FotoAdmin.class, "tbmuser"); // Khusus string, ditangani terpisah
		RELASI_MAP.put(LampiranPklMahasiswa.class, "persyaratanPkl");
		RELASI_MAP.put(LampiranKknMahasiswa.class, "persyaratanKkn");
		RELASI_MAP.put(LampiranBeasiswaMahasiswa.class, "persyaratanBeasiswa");

		// Class yang menggunakan ID langsung atau tidak punya field relasi spesifik
		RELASI_MAP.put(TugasFileContent.class, "id");
		RELASI_MAP.put(PertemuanFileContent.class, "id");
		RELASI_MAP.put(AudioPertemuan.class, "id");
		RELASI_MAP.put(VideoPertemuan.class, "id");
		RELASI_MAP.put(FotoGambarProduk.class, "produk");
	}

	public abstract Long ambilRef();

	public abstract Class ambilClazz();

	public abstract String getJenis();

	public abstract String getLink();

	public abstract String getOlehId();

	public abstract String getOleh();

	public abstract Date getTanggal_dirubah();

	public abstract String getUrl();

	public abstract void setUrl(String url);

	public static Toolbarbutton tampilkanTombolUploadGdrive(final Button downloadButton, final Button hapusButton,
			final EventListener eventListener, final SetelahUpload setelahUpload, FileFotoLain fileFotoLain,
			final String jenis, final Boolean hanyaIcon, final Integer cutomUkuranUpload, final String keterangan,
			final Combobox jurusan, final Boolean harusPdf, final Long ref, final Boolean usingId, final Class clazz) {
		AmbilDataLampiranFileLain ambilDataLampiranFileLain = new AmbilDataLampiranFileLain(jenis, cutomUkuranUpload,
				keterangan, jurusan, harusPdf, ref, usingId, clazz, false);
		ambilDataLampiranFileLain.setEventListener(eventListener);
		Toolbarbutton ambil = ambilDataLampiranFileLain.tampilkanTombolUploadGDrive("");
		ambil.setStyle("font-size:9px");
		ambil.setAttribute("janganDisabled", true);
		return ambil;
	}

	// --- Helper Methods ---

	/** Mendapatkan nama field relasi berdasarkan class */
	private static String getRefField(Class clazz) {
		String field = RELASI_MAP.get(clazz);
		return field != null ? field : "id";
	}

	/** Mengambil nama properti setter (misal: "mahasiswa" -> "setMahasiswa") */
	private static String getSetterName(String fieldName) {
		if ("id".equalsIgnoreCase(fieldName))
			return "setId";
		return "set" + StringUtils.capitalize(fieldName);
	}

	// --- Core Logic ---

	public String createLinkUri() throws Exception {
		return createLinkUri(true, false);
	}

	public String createLinkUri(boolean ketemu) throws Exception {
		return createLinkUri(ketemu, false);
	}

	public String createLinkUri(boolean ketemu, boolean relative) throws Exception {
		if (getGdrive() != null && !getGdrive().trim().isEmpty()) {
			return exportGDriveUrl();
		}

		try {
			File f = ambilFile();
			if (f != null && f.exists() && f.length() > 0L) {
				String ex = FilenameUtils.getExtension(getNama());
				String safeId = Common.desEncrypter.get().encrypt(getId() + this.getClass().getSimpleName())
						.replaceAll("[\\p{Punct}&&[^_-]]+", "");
				String fileName = URLEncoder.encode(safeId + "." + ex, "UTF-8");
				File fileTujuan = new File(CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + fileName);

				if (!fileTujuan.exists()) {
					FileUtils.copyFile(f, fileTujuan);
				}
			}
		} catch (Exception e) {
			// Berkas fisik bisa hilang/berupa link lama. Link lampiran tetap dibangun
			// lewat metadata agar halaman tidak gagal hanya karena cache file tidak ada.
		}

		String uri = LampiranLain.ambilLinkLampiranLain(this, false, true, ambilClazz(), ketemu, relative);

		return uri;
	}

	public void refreshFotoTemporaryGDrive() {
		// Menggunakan reflection ringan untuk melakukan re-attachment entity
		// Menggantikan blok if-else raksasa
		try {
			String fieldName = getRefField(this.getClass());
			if (!"id".equals(fieldName) && !"tbmuser".equals(fieldName)) {
				String getterName = "get" + StringUtils.capitalize(fieldName);
				String setterName = "set" + StringUtils.capitalize(fieldName);
				Method getter = this.getClass().getMethod(getterName);
				Method setter = this.getClass().getMethod(setterName, getter.getReturnType());
				Object value = getter.invoke(this);
				setter.invoke(this, value);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:194");
			// Ignore if method not found, fallback to silent fail similar to original code
		}
	}

	public static String ambilLokasi(String prefix, Serializable ref, String jenis, Class clazz) {
		try {
			File file = Common.getFileLocation(clazz, ref,
					prefix + "_lampiran_" + URLEncoder.encode(jenis, "UTF-8") + "_" + ref.toString());
			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) {
			return VOMahasiswa.dataJSON;
		}
	}

	public static void resetLokasi(Boolean usingId, Serializable ref, String jenis, Class clazz) {
		String keyFilePrefix = "data_baru_" + (usingId ? "_id" : "");
		tulisLokasi("", keyFilePrefix, ref, jenis, clazz);
	}

	private static String ambilLinkLampiranLainLink(Serializable ref, String jenis, Boolean usingId, Boolean download,
			Class clazz, boolean relative, boolean rezise) throws Exception {
		MyJSONObject jsonObject = new MyJSONObject();
		jsonObject.put("ref", ref);
		jsonObject.put("rezise", rezise);
		jsonObject.put("jurusan", "");
		jsonObject.put("jenis", jenis);
		jsonObject.put("usingId", usingId.toString());
		jsonObject.put("download", download.toString());
		jsonObject.put("clazz", clazz.getName());
		String encript = Common.desEncrypter.get().encrypt(jsonObject.toString());
		return (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/al?d="
				+ URLEncoder.encode(encript, "UTF-8");
	}

	public static String ambilLinkLampiranLain(File file) throws Exception {
		MyJSONObject jsonObject = new MyJSONObject();
		jsonObject.put("file", file.getAbsolutePath());
		String encript = Common.desEncrypter.get().encrypt(jsonObject.toString());
		return Common.getRequestHostWithProtocol() + "/al?d=" + URLEncoder.encode(encript, "UTF-8");
	}

	// Overloads for ambilLinkLampiranLain
	public static String ambilLinkLampiranLain(FileFotoLain fileFoto, Boolean usingId, Boolean download, Class clazz)
			throws Exception {
		return ambilLinkLampiranLain(fileFoto, usingId, download, clazz, true, false, false);
	}

	public static String ambilLinkLampiranLain(FileFotoLain fileFoto, Boolean usingId, Boolean download, Class clazz,
			boolean ketemu) throws Exception {
		return ambilLinkLampiranLain(fileFoto, usingId, download, clazz, ketemu, false, false);
	}

	public static String ambilLinkLampiranLain(FileFotoLain fileFoto, Boolean usingId, Boolean download, Class clazz,
			boolean ketemu, boolean relative) throws Exception {
		return ambilLinkLampiranLain(fileFoto, usingId, download, clazz, ketemu, relative, false);
	}

	public static String ambilLinkLampiranLain(FileFotoLain fileFoto, Boolean usingId, Boolean download, Class clazz,
			boolean ketemu, boolean relative, boolean rezise) throws Exception {
		try {
			if (fileFoto == null) {
				return (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/img/" + iconNggakAda(clazz);
			}

			String namaAsli = fileFoto.getNama();
			if (namaAsli == null || namaAsli.trim().length() == 0) {
				namaAsli = "lampiran";
			}
			String namaFile = StringUtils.replace(
					StringUtils.replace(StringUtils.replace(namaAsli, " ", "_"), "%", "_"), "#", "_");
//			boolean isImage = namaFile.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif)$");
//			boolean resi = rezise && isImage;

			String link = ambilLinkLampiranLainLink(fileFoto.ambilRef(), fileFoto.getJenis(), usingId, download, clazz,
					relative, rezise);

			if ("Berupa link file".equalsIgnoreCase(namaAsli)) {
				link = fileFoto.getLink();
			} else if (fileFoto.getGdrive() != null && !fileFoto.getGdrive().isEmpty()) {
				link = "https://drive.google.com/file/d/" + fileFoto.getGdrive() + "/preview";
			} else if (ketemu) {
				// Tata letak berkas kini DIPISAH per entitas: <media>/<NamaKelas>/<id>/<nama>.
				// Sebelumnya hanya <media>/<id>/ sehingga entitas berbeda dgn PK sama berbagi
				// folder dan berkasnya bisa tertukar (lihat FileFoto.segmenFolderBerkas()).
				String segmen = fileFoto.segmenFolderBerkas();
				String fileDiMedia = CommonMedia.getMediaDirectory().getAbsolutePath() + "/" + segmen + "/"
						+ namaFile;
				Path filePath = Paths.get(fileDiMedia);

				if (Files.exists(filePath) && Files.size(filePath) > 0) {
					if (rezise) {
						File fileKecil = new File(CommonMedia.getMediaDirectory(),
								segmen + "/thumbnail_" + namaFile);
						if (!fileKecil.exists()) {
							BufferedImage originalImage = ais.common.CommonFileMediaHelper.bacaGambarAman(filePath.toFile());
							if (originalImage != null) {
								BufferedImage thumbnail = Scalr.resize(originalImage, 128);
								String ext = Common.getFileExtension(filePath.toFile());
								ImageIO.write(thumbnail, ext, fileKecil);
							}
						}
						link = (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/f"
								+ CommonMedia.prefix + "/" + segmen + "/" + fileKecil.getName();
					} else {
						link = (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/f"
								+ CommonMedia.prefix + "/" + segmen + "/" + namaFile;
					}
				}
			}
			return link;
		} catch (Exception e) {
//			e.printStackTrace();
			return (relative ? Common.ROOT : Common.getRequestHostWithProtocol()) + "/img/" + iconNggakAda(clazz);
		}
	}

	public static String iconNggakAda(Class clazz) {
		return iconNggakAda(clazz.getName());
	}

	public static String iconNggakAda(String clazzName) {
		if (clazzName.equals(FotoAdmin.class.getName()) || clazzName.equals(FotoMahasiswa.class.getName())
				|| clazzName.equals(FotoSiswa.class.getName()) || clazzName.equals(FotoDosen.class.getName())
				|| clazzName.equals(FotoGuru.class.getName())
				|| clazzName.equals(FotoBiodataCalonMahasiswa.class.getName())
				|| clazzName.equals(FotoCalonSiswa.class.getName()) || clazzName.equals(PenyediaAsset.class.getName())
				|| clazzName.equals(FotoPegawai.class.getName())) {
			return "user_default.png";
		}
		return "administrator-icon_default.png";
	}

	private static void tulisLokasi(String data, String prefix, Serializable ref, String jenis, Class clazz) {
		try {
			// FIX NPE rutin: ref bisa null (mis. Tbmuser baru tanpa entitas
			// terkait spt Pegawai/Guru/Dosen -- terjadi tiap proses login).
			// ref.toString() sebelumnya melempar NPE sehingga cache lokasi
			// GAGAL ditulis & query DB + percobaan tulis ini terulang tiap
			// panggilan berikutnya. Pakai placeholder aman utk ref null.
			File file = Common.getFileLocation(clazz, ref, prefix + "_lampiran_" + URLEncoder.encode(jenis, "UTF-8")
					+ "_" + (ref == null ? "0" : ref.toString()));
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:325");
		}
	}

	// --- Ambil Methods (Simplified) ---

	public static FileFotoLain ambil(Serializable ref, String jenis, Class clazz) {
		return ambil(false, ref, jenis, clazz, false);
	}

	public static FileFotoLain ambil(Serializable ref, String jenis, Class clazz, boolean refresh) {
		return ambil(false, ref, jenis, clazz, refresh);
	}

	public static FileFotoLain ambil(Boolean usingId, Serializable ref, String jenis, Class clazz) {
		return ambil(usingId, ref, jenis, 0, clazz, false);
	}

	public static FileFotoLain ambil(Boolean usingId, Serializable ref, String jenis, Class clazz, boolean refresh) {
		return ambil(usingId, ref, jenis, 0, clazz, refresh);
	}

	public void delete() {
		Boolean usingId = false;
		String keyFilePrefix = "data_baru_" + (usingId ? "_id" : "");
		tulisLokasi("", keyFilePrefix, ambilRef(), getJenis(), ambilClazz());
		super.delete();
	}

	public static FileFotoLain ambil(Boolean usingId, Serializable ref, String jenis, int jumlahCoba, Class clazz,
			boolean refresh) {
		String kondisiTambahan = "";
		return ambil(usingId, ref, jenis, jumlahCoba, clazz, refresh, kondisiTambahan);
	}

	public static FileFotoLain ambil(Boolean usingId, Serializable ref, String jenis, int jumlahCoba, Class clazz,
			boolean refresh, String kondisiTambahan) {
		String keyFilePrefix = "data_baru_" + (usingId ? "_id" : "");
		String udah = ambilLokasi(keyFilePrefix, ref, jenis, clazz);

		if (udah == null || udah.trim().isEmpty() || VOMahasiswa.dataJSON.equalsIgnoreCase(udah) || refresh) {
			Session session = null;
			Transaction transaction = null;
			try {
				String refName = getRefField(clazz);
				boolean adaJenis = !refName.equals("id") && !refName.equals("tbmuser")
						&& !clazz.getSimpleName().startsWith("Foto") && !clazz.getSimpleName().endsWith("FileContent");

				// Special handling for legacy hardcoded string ref in FotoAdmin
				if (clazz.equals(FotoAdmin.class) && ref instanceof String) {
					refName = "tbmuser";
					usingId = false;
				}
				// FotoAdmin: kolom "tbmuser" bertipe String (userid). Untuk user BARU, ref berupa
				// Long placeholder (-randLong dari createDownloadUpload) sehingga jalur
				// Restrictions.eq("tbmuser", ref) melempar ClassCastException Long->String. Ubah ref
				// ke String agar query AMAN (tidak ada yang cocok -> null, memang belum ada foto).
				if (clazz.equals(FotoAdmin.class) && !usingId && "tbmuser".equals(refName)
						&& ref != null && !(ref instanceof String)) {
					ref = String.valueOf(ref);
				}

				session = StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
				transaction = session.beginTransaction();
				FileFotoLain result = (FileFotoLain) session.createCriteria(clazz)
						.add(kondisiTambahan.trim().isEmpty() ? Restrictions.sqlRestriction("true")
								: Restrictions.sqlRestriction(kondisiTambahan))
						.addOrder(Order.desc("id"))
						.add(usingId || !adaJenis ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("jenis", jenis))
						.add(usingId || "id".equals(refName) ? Restrictions.idEq(ref) : Restrictions.eq(refName, ref))
						.setMaxResults(1).uniqueResult();

				if (result != null) {
					JSONObject jsonObject = Common.convertToJsonObject(result);
					jsonObject.put("class", clazz.getName());
					jsonObject.put("id", result.getId());
					tulisLokasi(jsonObject.toString(), keyFilePrefix, ref, jenis, clazz);
					transaction.commit();
					transaction = null;
					return result;
				} else {
					tulisLokasi("0", keyFilePrefix, ref, jenis, clazz);
					transaction.commit();
					transaction = null;
					return null;
				}
			} catch (Exception e) {
				rollbackQuietly(transaction);
				transaction = null;
				tulisLokasi("0", keyFilePrefix, ref, jenis, clazz);
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:408");
				return null;
			} finally {
				rollbackQuietly(transaction);
				if (session != null && session.isOpen())
					session.close();
			}
		} else if ("0".equals(udah.trim())) {
			return null;
		} else {
			try {
				JSONObject jsonObject = new JSONObject(udah);
				String fileLokasi = jsonObject.optString("lokasi_file_absolut");
				if (fileLokasi != null && Files.notExists(Paths.get(fileLokasi)) && jumlahCoba == 0) {
					return ambil(usingId, ref, jenis, 1, clazz, refresh);
				}
				return (FileFotoLain) Common.convertToObject(jsonObject);
			} catch (Exception e) {
				tulisLokasi("", keyFilePrefix, ref, jenis, clazz);
				return jumlahCoba == 0 ? ambil(usingId, ref, jenis, 1, clazz, refresh) : null;
			}
		}
	}

	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Class clazz) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUpload(row, ref, jenis, keterangan, harusPdf, eventListener, null, false, false, false, true,
				null, clazz);
	}

	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Integer cutomUkuranUpload, Class clazz) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUpload(row, ref, jenis, keterangan, harusPdf, eventListener, null, cutomUkuranUpload, clazz);
	}

	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Map<String, FileFotoLain> lampiranLains, Integer cutomUkuranUpload,
			Class clazz) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUpload(row, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains, false, false, false,
				true, cutomUkuranUpload, clazz);
	}

	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Map<String, FileFotoLain> lampiranLains, Class clazz) {
		Long ref = myref == null ? Common.refSementara() : myref;
		createDownloadUpload(row, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains, false, false, false,
				true, null, clazz);
	}

	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Map<String, FileFotoLain> lampiranLains, Boolean tidakTampilJurusan,
			Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload, Class clazz) {
		createDownloadUpload(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains, tidakTampilJurusan,
				hanyaIcon, usingId, tampilUpload, null, clazz);
	}

	public static void createDownloadUpload(Hbox row, Long myref, String jenis, String keterangan, Boolean harusPdf,
			EventListener eventListener, Map<String, FileFotoLain> lampiranLains, Boolean tidakTampilJurusan,
			Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload, Integer cutomUkuranUpload, Class clazz) {
		createDownloadUpload(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains, tidakTampilJurusan,
				hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, false, true, clazz);

	}

	public static void createDownloadUpload(Hbox row, Serializable myref, String jenis, String keterangan,
			Boolean harusPdf, EventListener eventListener, Map<String, FileFotoLain> lampiranLains,
			Boolean tidakTampilJurusan, Boolean hanyaIcon, Boolean usingId, Boolean tampilUpload,
			Integer cutomUkuranUpload, Boolean vertical, Boolean janganPreviewDiLayarUtama, Class clazz) {
		Component parentPreview = null;
		createDownloadUpload(row, myref, jenis, keterangan, harusPdf, eventListener, lampiranLains, tidakTampilJurusan,
				hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical, janganPreviewDiLayarUtama, parentPreview,
				clazz);
	}

	@SuppressWarnings({})
	public static void createDownloadUpload(final Component rowUtama, Serializable myrefId, final String jenis,
			final String keterangan, final Boolean harusPdf, final EventListener eventListener, final Map lampiranLains,
			final Boolean tidakTampilJurusan, final Boolean hanyaIcon, final Boolean usingId,
			final Boolean tampilUpload, final Integer cutomUkuranUpload, final Boolean vertical,
			final Boolean janganPreviewDiLayarUtama, final Component parentPreviewAja, final Class clazz) {
		createDownloadUpload(rowUtama, myrefId, jenis, keterangan, harusPdf, eventListener, lampiranLains,
				tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
				janganPreviewDiLayarUtama, parentPreviewAja, clazz, false);
	}

	public static void createDownloadUpload(final Component rowUtama, Serializable myrefId, final String jenis,
			final String keterangan, final Boolean harusPdf, final EventListener eventListener, final Map lampiranLains,
			final Boolean tidakTampilJurusan, final Boolean hanyaIcon, final Boolean usingId,
			final Boolean tampilUpload, final Integer cutomUkuranUpload, final Boolean vertical,
			final Boolean janganPreviewDiLayarUtama, final Component parentPreviewAja, final Class clazz,
			final boolean refresh) {

		final Serializable ref = myrefId == null ? Common.refSementara() : myrefId;

		// Layout setup
		Vbox vbox = new Vbox();
		vbox.setHflex("1");
		vbox.setVflex("1");
		rowUtama.appendChild(vbox);

		Box containerTombol = (hanyaIcon || !vertical) ? new Hbox() : new Vbox();
		containerTombol.setHflex("1");
		containerTombol.setVflex("1");
		vbox.appendChild(containerTombol);
		rowUtama.setAttribute("tombol", containerTombol);

		final Component vbPreview = parentPreviewAja == null ? new Vbox() : parentPreviewAja;
		if (parentPreviewAja == null)
			vbox.appendChild(vbPreview);

		// Reset Handler
		final EventListener resetAfterUploadData = new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				Common.clear(rowUtama);
				if (parentPreviewAja != null) {
					Common.clear(parentPreviewAja);
					parentPreviewAja.setVisible(true);
				}
				Common.createDefaultTimer(new EventListener() {
					public void onEvent(Event arg0) throws Exception {
						createDownloadUpload(rowUtama, ref, jenis, keterangan, harusPdf, eventListener, lampiranLains,
								tidakTampilJurusan, hanyaIcon, usingId, tampilUpload, cutomUkuranUpload, vertical,
								janganPreviewDiLayarUtama, parentPreviewAja, clazz, true);
						Clients.scrollIntoView(parentPreviewAja != null ? parentPreviewAja : rowUtama);
					}
				});
			}
		};

		// UI Components
		setupJurusanCombo(containerTombol, tidakTampilJurusan);
		final MyToolbarbuttonConfig downloadButton = createButton(hanyaIcon, "Download " + keterangan,
				"/img/svg/attachment-2.svg");
		containerTombol.appendChild(downloadButton);
		final MyToolbarbuttonConfig hapusButton = createButton(hanyaIcon, "Hapus", "/img/svg/trash.svg");

		final SetelahUpload setelahUpload = new SetelahUpload(downloadButton, janganPreviewDiLayarUtama, ref, jenis,
				usingId, vbPreview, hapusButton, clazz);
		final List<MyToolbarbuttonConfig> gdriveButtons = new ArrayList<MyToolbarbuttonConfig>();

		// Logic Load Data
		FileFotoLain fileFotoLain = FileFotoLain.ambil(usingId, ref, jenis, clazz, refresh);
		int jumlah = fileFotoLain != null ? 1 : 0;
		rowUtama.setAttribute("jumlah_upload", jumlah);

		if (jumlah == 0 && !tampilUpload)
			rowUtama.setVisible(false);

		downloadButton.setVisible(jumlah > 0);
		hapusButton.setVisible(jumlah > 0);
		if (parentPreviewAja != null)
			parentPreviewAja.setVisible(jumlah > 0);

		if (jumlah > 0) {
			setupDownloadButtonAction(downloadButton, fileFotoLain, usingId, clazz, keterangan);
			if (lampiranLains != null)
				lampiranLains.put(jenis, fileFotoLain);

			downloadButton.setImage(fileFotoLain.iconDonwload());
			if (!hanyaIcon) {
				String nama = fileFotoLain.getNama();
				if (nama.contains("_"))
					nama = nama.substring(nama.lastIndexOf("_") + 1);
				if (!tampilUpload)
					nama = StringUtils.abbreviate(nama, 20);

				downloadButton.setLabel(nama);
				try {
					setelahUpload.onEvent(new Event("langsung", downloadButton, fileFotoLain));
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:580");
				}

				// GDrive Check
				// PENJAGA: fileFotoLain.ambilFile() bisa saja berkas fisiknya hilang atau
				// tak cocok dgn baris ini (lihat FileFoto.ambilFile: berkas-salah-baris /
				// file-tidak-ditemukan) -> method tsb SUDAH mencatat IllegalStateException /
				// FileNotFoundException ke ErrorAuditUtil lalu mengembalikan placeholder
				// logo.png, tak pernah melempar exception itu ke sini. Namun logika
				// Common.simpanKeDrive() di bawah bisa saja melempar RuntimeException lain
				// (mis. IO ke Google Drive) yg sebelumnya TIDAK ditangkap sama sekali,
				// sehingga 1 baris lampiran bermasalah menghentikan seluruh render grid
				// upload (ItemAction, buku perpustakaan) maupun beranda PMB (PMBAction).
				// Bungkus supaya baris ini di-skip dgn peringatan, baris lain tetap tampil.
				if (AmbilDataLampiranFileLain.bolehDriveAtauLink(clazz, jenis, keterangan)
						&& fileFotoLain.getGdrive() == null) {
					try {
						File f = fileFotoLain.ambilFile();
						if (f != null && f.exists()) {
							if ("logo.png".equals(f.getName())) {
								// Placeholder dari ambilFile() -> berkas asli hilang/tak cocok.
								// Jangan lanjut ke Google Drive dgn placeholder, cukup beri tanda.
								containerTombol.appendChild(new ais.ui.util.MyLabelAgakKecilBoldMerah(
										"Berkas tidak ditemukan"));
							} else {
								MyToolbarbuttonConfig gdrive = Common.simpanKeDrive(fileFotoLain, f, keterangan,
										resetAfterUploadData);
								gdrive.setParent(containerTombol);
								gdriveButtons.add(gdrive);
							}
						}
					} catch (RuntimeException e) {
						ais.common.ErrorAuditUtil.record(e,
								"lampiran-lain-gdrive-check-gagal-diabaikan src/ais/database/model/file/FileFotoLain.java:595");
					}
				}
			}
		}

		// Upload Button Logic
		if (tampilUpload) {
			Tbmuser tbmuser = Common.getCurrentUser();
			if (tbmuser != null && ref != null) {
				containerTombol.appendChild(tampilkanTombolUpload(downloadButton, hapusButton, eventListener,
						setelahUpload, fileFotoLain, jenis, hanyaIcon, cutomUkuranUpload, keterangan, null, harusPdf,
						ref, usingId, resetAfterUploadData, clazz));
			} else {
				// Fallback legacy upload button if needed
				AmbilDataLampiranFileLain adl = new AmbilDataLampiranFileLain(jenis, cutomUkuranUpload, keterangan,
						null, harusPdf, ref, usingId, clazz, false);
				adl.setEventListener(wrapListener(eventListener, resetAfterUploadData));
				Toolbarbutton btn = adl.tampilkanTombolUpload("");
				btn.setStyle("font-size:9px");
				containerTombol.appendChild(btn);
				if (fileFotoLain != null)
					btn.setLabel(btn.getLabel().replace("Upload", "Ganti"));
			}

			containerTombol.appendChild(hapusButton);
			hapusButton.addEventListener("onClick", new EventListener() {
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.showFormatCb(
							"Apakah Bapak/Ibu yakin ingin menghapus \"{V1}\"? Tindakan ini bersifat permanen dan tidak dapat dibatalkan. Silakan pilih OK untuk melanjutkan penghapusan, atau Batal untuk membatalkan.",
							"Konfirmasi", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {
								public void onEvent(Event e) throws Exception {
									if (Integer.parseInt(e.getData().toString()) == MyMessageboxConfig.OK) {
										performDelete(usingId, ref, jenis, clazz);
										resetAfterUploadData.onEvent(e);
									}
								}
							}, keterangan);
				}
			});
		}
	}

	// --- Helper UI Methods ---

	private static EventListener wrapListener(final EventListener original, final EventListener reset) {
		return new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				if (original != null)
					original.onEvent(arg0);
				reset.onEvent(arg0);
			}
		};
	}

	private static void performDelete(Boolean usingId, Serializable ref, String jenis, Class clazz) {
		Session session = StreamingHibernateUtil.getInstance().currentSession();
		try {
			FileFotoLain.resetLokasi(usingId, ref, jenis, clazz);
			FileFotoLain temp = (FileFotoLain) clazz.newInstance();
			hapusAtauUpdate(temp, session, usingId, ref, jenis);
		} catch (Exception ex) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			Common.tampilErrorJikaAdmin(ex);
		} finally {
			StreamingHibernateUtil.getInstance().closeSession();
		}
	}

	private static MyToolbarbuttonConfig createButton(boolean iconOnly, String label, String iconPath) {
		MyToolbarbuttonConfig btn = iconOnly ? new MyToolbarbuttonConfig("", iconPath)
				: new MyToolbarbuttonConfig(label, iconPath);
		if (!iconOnly)
			btn.setTooltiptext(label);
		btn.setStyle("font-size:9px");
		btn.setAttribute("janganDisabled", true);
		return btn;
	}

	private static void setupJurusanCombo(Component parent, Boolean tidakTampil) {
		if (tidakTampil && "Y".equals(
				Common.getKonfigurasi("upload_file_di_konfigurasi_tiap_jurusan_bisa_beda", Konfigurasi.TIDAK_AKTIF)
						.getNilai())) {
			Combobox jurusan = new Combobox();
			Common.insertComboDanSemua(jurusan, "nama", Jurusan.class,
					Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
			jurusan.setReadonly(true);
			parent.appendChild(jurusan);
		}
	}

	private static void setupDownloadButtonAction(Toolbarbutton btn, final FileFotoLain file, final Boolean usingId,
			final Class clazz, final String ket) {
		btn.addEventListener("onClick", new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				if (file.getNama().endsWith(".jrxml") || file.getNama().endsWith(".xml")) {
					Filedownload.save(file.ambilFile(), file.getKeterangan());
				} else if (file.getGdrive() != null) {
					file.tampilGDrive(null);
				} else {
					String link = file.getLink();
					if (link == null || link.isEmpty())
						// ketemu=false: selalu pakai URL /al?d=... agar melalui servlet AmbilLampiran
						// yang terautentikasi. URL media (/f/ais/...) bisa return 404 dan memicu
						// broken.jsp yang memiliki reload loop sehingga preview tidak pernah selesai.
						link = FileFotoLain.ambilLinkLampiranLain(file, usingId, true, clazz, false);

					if (link != null && !link.isEmpty()) {
						if (file.bisaPreview()) {
							Common.displayWindow(file.merupakanGambar(), link, true, "95%", "95%", true, file);
						} else {
							if (link.contains("AmbilLampiran") || Common.isMobile()) {
								ExecutionsCtrl.getCurrent().sendRedirect(link, "_blank");
							} else {
								Clients.evalJavaScript(
										"popupCenter({url: '" + link + "', title: 'data', w: 1200, h: 600});");
							}
						}
					} else {
						MyMessageboxConfig.show(
								"Mohon maaf, Bapak/Ibu. Berkas yang diminta tidak ditemukan. Langkah yang dapat dilakukan: (1) pastikan berkas telah diunggah dengan benar; (2) muat ulang halaman lalu coba kembali; (3) apabila masalah masih berlanjut, mohon menghubungi Administrator.",
								"Error", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION);
					}
				}
			}
		});
	}

	// --- Create & Update Logic (Generic) ---

	public static MyToolbarbuttonConfig tampilkanTombolUpload(final Button downloadButton, final Button hapusButton,
			final EventListener eventListener, final SetelahUpload setelahUpload, FileFotoLain fileFotoLain,
			final String jenis, final Boolean hanyaIcon, final Integer cutomUkuranUpload, final String keterangan,
			final Combobox jurusan, final Boolean harusPdf, final Serializable ref, final Boolean usingId,
			final EventListener resetAfterUpload, final Class clazz) {

		EventListener eventListenerUpload = new EventListener() {
			public void onEvent(Event arg0) throws Exception {
				AmbilDataLampiranFileLain dialog = new AmbilDataLampiranFileLain(jenis, cutomUkuranUpload, keterangan,
						jurusan, harusPdf, ref, usingId, clazz, true);
				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(dialog);
				dialog.onModal();
				dialog.setEventListener(new EventListener() {
					public void onEvent(Event ev) throws Exception {
						FileFotoLain.resetLokasi(usingId, ref, jenis, clazz);
						final FileFotoLain hasilUpload = (FileFotoLain) ev.getData();

						if (hasilUpload != null) {
							Session session = StreamingHibernateUtil.getInstance().currentSession();
							try {
								String key = "data_baru_" + (usingId ? "_id" : "");
								FileFotoLain finalObj = hasilUpload;

								if (!"Baru".equalsIgnoreCase(ev.getName())) {
									Tbmuser user = Common.getCurrentUser();
									finalObj = FileFotoLain.createFileFotoLain(user, session, clazz, usingId, ref,
											jenis, hasilUpload, null, hasilUpload.getNama());
								}

								// Update UI
								if (downloadButton != null) {
									downloadButton.setImage(finalObj.iconDonwload());
									if (!hanyaIcon)
										downloadButton.setLabel(finalObj.getNama());
									if (setelahUpload != null)
										setelahUpload.onEvent(new Event("", downloadButton, finalObj));
								}
								if (hapusButton != null)
									hapusButton.setVisible(true);

								JSONObject json = Common.convertToJsonObject(finalObj);
								json.put("class", clazz.getName());
								json.put("id", finalObj.getId());
								tulisLokasi(json.toString(), key, ref, jenis, clazz);

								if (eventListener != null) {
									final FileFotoLain callbackObj = finalObj;
									Common.createDefaultTimer(new EventListener() {
										public void onEvent(Event e) throws Exception {
											eventListener.onEvent(new Event("", downloadButton, callbackObj));
										}
									});
								}
							} catch (Exception e) {
								e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:776");
							} finally {
								StreamingHibernateUtil.getInstance().closeSession();
								if (resetAfterUpload != null)
									resetAfterUpload.onEvent(null);
							}
						}
					}
				});
			}
		};

		String lbl = (fileFotoLain == null ? "Upload " : "Ganti ") + StringUtils.abbreviate(keterangan, 10);
		MyToolbarbuttonConfig btn = new MyToolbarbuttonConfig(hanyaIcon ? "" : lbl, "/img/svg/upload.svg");
		if (!hanyaIcon)
			btn.setTooltiptext(lbl + " " + keterangan);
		btn.setStyle("font-size:9px");
		btn.setAttribute("janganDisabled", true);
		btn.addEventListener("onClick", eventListenerUpload);
		return btn;
	}

	@SuppressWarnings("deprecation")
	public static FileFotoLain createFileFotoLain(Tbmuser tbmuser, Session session, Class clazz, Boolean usingId,
			Serializable ref, String jenis, FileFotoLain source, File file, String nama) throws Exception {

		// Lapis kedua (chokepoint tunggal): apa pun pemanggilnya, hanya subkelas FileFotoLain
		// yang boleh dikonstruksi. Diperiksa SEBELUM newInstance sehingga konstruktor kelas
		// asing tidak pernah berjalan meski nama kelasnya lolos dari pemanggil. Lihat
		// DoUpload.resolveKelasLampiran untuk lapis pertama pada input klien.
		if (clazz == null || !FileFotoLain.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException("Kelas lampiran tidak sah: "
					+ (clazz == null ? "null" : clazz.getName()));
		}
		FileFotoLain target = (FileFotoLain) clazz.newInstance();

		if (file != null && file.exists()) {
			try {
				FileInputStream fis = new FileInputStream(file);
				target.setFoto(Hibernate.createBlob(IOUtils.toByteArray(fis)));
				target.setNama(file.getName());
				fis.close();
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:811");
			}
		} else {
			target.setNama(nama);
		}
		if (target.getNama() == null || target.getNama().trim().length() == 0) {
			String namaDasar = jenis == null || jenis.trim().length() == 0 ? clazz.getSimpleName() : jenis.trim();
			target.setNama(namaDasar.replace('/', '_').replace('\\', '_') + "_" + System.currentTimeMillis());
		}

		/*
		 * Acuan sementara WAJIB negatif. Sebelumnya dipakai Math.abs(Common.randLong())
		 * yang menghasilkan angka POSITIF <= 99_999_998 - persis rentang id asli. Acuan
		 * seperti itu bisa menunjuk ke baris milik entitas lain yang benar-benar ada,
		 * sehingga hapusAtauUpdate() di bawah menimpa lampiran milik data lain dan berkas
		 * yang baru diunggah muncul di data tersebut.
		 */
		if (ref == null || (ref instanceof Long && (Long) ref == -1L)) {
			ref = Common.refSementara();
		}

		// 1. Hapus atau "Soft Delete" data lama
		FileFotoLain.hapusAtauUpdate(target, session, usingId, ref, jenis);

		// 2. Set Data Utama Menggunakan Reflection (Generic)
		String refFieldName = getRefField(clazz);

		// Set Ref (Foreign Key)
		if (!"id".equals(refFieldName)) {
			if ("tbmuser".equals(refFieldName)) {
				// Khusus FotoAdmin yang fieldnya String tbmuser
				try {
					Method setTbmuser = clazz.getMethod("setTbmuser", String.class);
					setTbmuser.invoke(target, ref.toString());
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/file/FileFotoLain.java:835");
				}
			} else {
				try {
					// Cari setter yang menerima Long atau Serializable
					Method setter = clazz.getMethod(getSetterName(refFieldName), Long.class);
					setter.invoke(target, ref);
				} catch (NoSuchMethodException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:842");
					// Coba tipe parameter lain jika Long gagal (jarang terjadi di struktur ini)
				}
			}
		}

		// Set Jenis (jika ada)
		try {
			Method setJenis = clazz.getMethod("setJenis", String.class);
			setJenis.invoke(target, jenis);
		} catch (NoSuchMethodException e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:852");
			/* Ignore if no jenis field */ }

		// Set CopyDari (Copy properti lain dari source)
		try {
			// Asumsi: method setCopyDari menerima parameter bertipe Class itu sendiri
			Method setCopyDari = clazz.getMethod("setCopyDari", clazz);
			setCopyDari.invoke(target, source);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:860");
			/* Ignore */ }
		if (file == null) {
			copySourceBlobToTarget(target, source);
		}

		// 3. Set Audit Info
		target.setTanggal_dirubah(WaktuUtil.getDate());
		target.setOlehId(Common.generateOlehId(tbmuser));
		target.setOleh(getNamaOleh(tbmuser));

		// 4. Save — LampiranLain memakai kolom PostgreSQL Large Object (oid) +
		// hibernate.jdbc.use_streams_for_binary=true, sehingga INSERT-nya menulis Large Object yang
		// WAJIB di dalam transaksi (non-autocommit). Pada SessionFactory streaming, pool c3p0
		// (hibernate.c3p0.*) menjadi ConnectionProvider dan MELEPAS koneksi antar-statement (release
		// agresif), sehingga doWork()/beginTransaction() hanya menyetel auto-commit pada koneksi
		// SEMENTARA yang langsung dilepas — saat save() menjalankan insert IDENTITY, ia mengambil
		// koneksi BARU dari c3p0 yang autoCommit=true → gagal "Large Objects may not be used in
		// auto-commit mode".
		//
		// SOLUSI: ambil SATU koneksi langsung dari ConnectionProvider streaming, set autoCommit=false,
		// lalu buka session DI ATAS koneksi itu via openSession(Connection). Karena koneksi disuplai
		// pengguna, Hibernate TIDAK melepasnya antar-statement, sehingga seluruh insert + penulisan
		// Large Object terjadi pada koneksi yang SAMA (non-autocommit). Tetap memakai StreamingHibernateUtil.
		org.hibernate.engine.SessionFactoryImplementor sfStreaming = (org.hibernate.engine.SessionFactoryImplementor) StreamingHibernateUtil
				.getInstance().getSessionFactory();
		org.hibernate.connection.ConnectionProvider connProvider = sfStreaming.getConnectionProvider();
		java.sql.Connection blobConn = null;
		Session blobSession = null;
		try {
			blobConn = connProvider.getConnection();
			blobConn.setAutoCommit(false);
			blobSession = StreamingHibernateUtil.getInstance().getSessionFactory().openSession(blobConn);
			blobSession.save(target);
			blobSession.flush();
			blobConn.commit();
		} catch (Exception eBlob) {
			if (blobConn != null) {
				try {
					blobConn.rollback();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:900");
				}
			}
			throw eBlob;
		} finally {
			if (blobSession != null) {
				ais.database.hibernate.HibernateUtil.closeSessionQuietly(blobSession);
			}
			if (blobConn != null) {
				try {
					blobConn.setAutoCommit(true);
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:916");
				}
				try {
					connProvider.closeConnection(blobConn);
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:920");
				}
			}
		}

		return target;
	}

	private static void copySourceBlobToTarget(FileFotoLain target, FileFotoLain source) throws Exception {
		if (target == null || source == null) {
			return;
		}
		byte[] bytes = readSourceBlobBytes(source);
		if (bytes != null && bytes.length > 0) {
			target.setFoto(Hibernate.createBlob(bytes));
		}
	}

	/**
	 * Membaca isi Large Object satu berkas DENGAN AMAN, untuk pemanggil di luar kelas ini.
	 *
	 * <p>Kolom foto memakai PostgreSQL Large Object (oid). Membacanya lewat
	 * {@code getFoto().getBinaryStream()} begitu saja akan gagal dengan
	 * <i>"Large Objects may not be used in auto-commit mode"</i>, karena koneksi
	 * harus berada DI DALAM transaksi. Aturan itu mudah terlewat -- dan memang pernah
	 * terlewat pada aksi unduh lampiran Pengadaan (2026-08-22). Metode ini menyediakan
	 * satu jalan masuk yang benar supaya tidak perlu diulang-ulang di tiap pemanggil.</p>
	 *
	 * @return isi berkas, atau {@code null} bila berkasnya disimpan di Google Drive
	 *         (isinya tidak ada di basis data) atau tidak dapat dibaca.
	 */
	public static byte[] ambilIsiBlob(FileFotoLain berkas) throws Exception {
		return readSourceBlobBytes(berkas);
	}

	private static byte[] readSourceBlobBytes(FileFotoLain source) throws Exception {
		if (source == null) {
			return null;
		}
		try {
			if (source.getGdrive() != null && source.getGdrive().trim().length() > 0) {
				return null;
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:946");
		}
		if (source.getId() == null) {
			return readBlobBytes(source.getFoto());
		}
		Session readSession = null;
		Transaction transaction = null;
		try {
			readSession = StreamingHibernateUtil.getInstance().openSession();
			transaction = readSession.beginTransaction();
			Object loaded = readSession.get(source.getClass(), source.getId());
			if (!(loaded instanceof FileFotoLain)) {
				rollbackQuietly(transaction);
				transaction = null;
				return null;
			}
			byte[] bytes = readBlobBytes(((FileFotoLain) loaded).getFoto());
			rollbackQuietly(transaction);
			transaction = null;
			return bytes;
		} finally {
			rollbackQuietly(transaction);
			if (readSession != null) {
				try {
					if (readSession.isOpen()) {
						readSession.close();
					}
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:973");
				}
			}
		}
	}

	private static byte[] readBlobBytes(Blob blob) throws Exception {
		if (blob == null) {
			return null;
		}
		InputStream in = null;
		try {
			in = blob.getBinaryStream();
			return in == null ? null : IOUtils.toByteArray(in);
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:991");
				}
			}
		}
	}

	private static void rollbackQuietly(Transaction transaction) {
		if (transaction != null) {
			try {
				transaction.rollback();
			} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:1001");
			}
		}
	}

	private static String getNamaOleh(Tbmuser user) {
		if (user == null)
			return "external_update";
		if (user.getMahasiswa() != null)
			return user.getMahasiswa().getNama();
		if (user.ambilDosen() != null)
			return user.ambilDosen().getNama();
		if (user.ambilPegawai() != null)
			return user.ambilPegawai().getNama();
		return user.getUserNama();
	}

	public static void hapusAtauUpdate(FileFotoLain a, Session session, boolean usingId, Serializable ref,
			String jenis) {
		try {
			FileFotoLain.resetLokasi(usingId, ref, jenis, a.ambilClazz());

			String refField = getRefField(a.ambilClazz());
			// boolean useHql = true; // Use HQL for safer, database-agnostic queries

			String entityName = a.ambilClazz().getName();
			boolean hasJenis = true;
			try {
				a.ambilClazz().getMethod("getJenis");
			} catch (NoSuchMethodException e) {
				hasJenis = false;
			}

			session.getTransaction().begin();

			if (usingId) {
				// Delete by ID
				String hql = "delete from " + entityName + " where id = :ref" + (hasJenis ? " and jenis = :jenis" : "");
				Query q = session.createQuery(hql);
				q.setParameter("ref", ref);
				if (hasJenis)
					q.setParameter("jenis", jenis);
				q.executeUpdate();
			} else {
				// Soft Delete (Update Ref to -111...)
				if ("id".equals(refField)) {
					// Jika ref field adalah ID, kita tidak bisa update ID-nya sembarangan biasanya,
					// tapi logic asli melakukan delete jika usingId=true.
					// Jika usingId=false tapi refField="id", ini logic yang aneh di code asli,
					// tapi kita ikuti pola update FK nya.
					// Untuk case "id", biasanya tidak ada soft delete update FK, jadi skip atau
					// delete real.
					// Asumsi: Logic asli hanya melakukan update pada field relasi, bukan PK.
				} else {
					String hql = "update " + entityName + " set " + refField + " = :softDel where " + refField
							+ " = :ref";
					if (hasJenis)
						hql += " and jenis = :jenis";

					Query q = session.createQuery(hql);
					if ("tbmuser".equals(refField)) {
						q.setParameter("softDel", String.valueOf(SOFT_DELETE_ID));
						q.setParameter("ref", ref.toString());
					} else {
						q.setParameter("softDel", SOFT_DELETE_ID);
						q.setParameter("ref", ref);
					}

					if (hasJenis)
						q.setParameter("jenis", jenis);
					q.executeUpdate();
				}
			}
			session.getTransaction().commit();

		} catch (Exception e) {
			try {
				session.getTransaction().rollback();
			} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/database/model/file/FileFotoLain.java:1079");
			}
			// e.printStackTrace(); // Optional logging
		}
	}
}
