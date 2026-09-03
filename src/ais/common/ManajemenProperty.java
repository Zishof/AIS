package ais.common;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.proxy.HibernateProxyHelper;
import org.json.JSONObject;

import ais.action.servlet.api.DaftarDataService;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Program;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFotoLain;
import ais.database.model.file.FotoAdmin;
import ais.database.model.file.LampiranLain;
import ais.database.model.inventory.Pedagang;
import ais.database.model.inventory.Produk;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JenisCatatanSiswa;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.AlurPersetujuanSuratMasukStatus;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.SuratMasuk;

// Pastikan import package ini sesuai dengan struktur project Anda
// import ais.database.model.*; 
// import ais.util.*;

/**
 * Utilitas bersama AIS untuk manajemen property. Kelas ini mengonsolidasikan operasi lintas
 * layar/service yang benar-benar satu domain agar pemanggil tidak membuat helper dengan fungsi
 * paralel.
 *
 * <p><b>Batas tanggung jawab:</b> gunakan tipe ini hanya untuk state dan operasi yang sesuai dengan nama
 * domainnya. Logika lintas domain harus didelegasikan ke service atau helper bersama supaya tidak muncul
 * implementasi paralel dengan hasil berbeda.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String IMAGE_EXTENSIONS};
 * pembacaan/pencarian ({@code getKey()}); operasi domain lain ({@code safeToString()}, {@code insertProperty()},
 * {@code isImageFile()}, {@code formatDate()}, {@code formatNumber()}, {@code processSignature()}). Bagian lain
 * dari kontrak tetap mengikuti kelas induk atau interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> sesuai operasi yang dipanggil, utilitas dapat mengubah komponen UI, membaca/menulis
 * persistence atau berkas, dan memanggil layanan lain. Gunakan method kanonik di kelas ini melalui konteks
 * request/transaksi yang tepat, bukan menyalin implementasinya.</p>
 */
public class ManajemenProperty {

	private static final String[] IMAGE_EXTENSIONS = { ".jpg", ".png", ".jpeg", ".gif", ".tif", ".bmp" };

	// FIX LazyInitializationException/NPE rutin: val.toString() pada Set/proxy
	// Hibernate lazy (mis. Sekolah.penjurusanSekolahs) yang belum di-initialize
	// & sesi sudah tertutup (thread report/background) melempar exception yg
	// membatalkan SISA pemrosesan properti ini (termasuk blok Handle Set/Number/
	// Date/Entities di bawahnya). Cek isInitialized dulu agar downstream tetap jalan.
	private static String safeToString(Object val) {
		if (val == null) {
			return "";
		}
		try {
			if (!Hibernate.isInitialized(val)) {
				return "";
			}
			return val.toString();
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:safeToString");
			return "";
		}
	}

	@SuppressWarnings("rawtypes")
	public static void insertProperty(Class clazz, GeneralValueObject generalValueObject,
			Map<String, Object> parameters, String nama) {
		insertProperty(clazz, generalValueObject, parameters, nama, 1);
	}

	/**
	 * Helper untuk membuat key map dengan notasi titik (dot notation).
	 */
	private static String getKey(String prefix, String property) {
		return (prefix == null || prefix.isEmpty()) ? property : prefix + "." + property;
	}

	/**
	 * Helper untuk cek ekstensi gambar.
	 */
	private static boolean isImageFile(String filename) {
		if (filename == null)
			return false;
		String lowerName = filename.toLowerCase();
		for (String ext : IMAGE_EXTENSIONS) {
			if (lowerName.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Helper formatting tanggal untuk Map.
	 */
	public static void formatDate(Map<String, Object> parameters, String nama, String property, Object val) {
		String key = getKey(nama, property);
		parameters.put(key, Common.dateFormat5.get().format(val));
		parameters.put(key + ".formated1", Common.dateFormat6.get().format(val));
		parameters.put(key + ".formated2", Common.dateFormat2.get().format(val));
		parameters.put(key + ".formated3", Common.dateFormat51.get().format(val));
		parameters.put(key + ".formated4", Common.timeFormat.get().format(val));
		parameters.put(key + ".formated5", Common.dateFormat1.get().format(val));
		parameters.put(key + ".datetime_format", Common.databaseDateFormat.get().format(val));
		parameters.put(key + ".iso", Common.dateFormatInput.get().format(val));

		// Tambahan khusus untuk JSONObject logic yang kadang membutuhkan format
		// database
		if (val instanceof Date) {
			// Opsional: tambahkan jika diperlukan konsistensi dengan method JSON
			// parameters.put(key + ".datetime_format",
			// Common.databaseDateFormat.get().format(val));
		}
	}

	/**
	 * Helper formatting angka.
	 */
	private static void formatNumber(Map<String, Object> parameters, String key, Number val) {
		parameters.put(key + ".formated", Common.numberFormat.get().format(val));
		parameters.put(key + ".terbilang", IndonesianNumberToWords.convert((long) Math.abs(val.longValue())));
	}

	/**
	 * Helper Generik untuk memproses Tanda Tangan (TTD).
	 */
	private static void processSignature(Map<String, Object> parameters, String baseKey, Object entity,
			String lampiranType, String suffixKey) {
		try {
			if (entity == null)
				return;

			// 1. QR Code Logic (Menggunakan reflection/interface jika method ttdQr ada di
			// GeneralValueObject,
			// jika tidak, casting manual seperti di kode asli)
			Session signatureSession = null;
			try {
				Object target = entity;
				if (entity instanceof GeneralValueObject && ((GeneralValueObject) entity).getId() != null) {
					signatureSession = HibernateUtil.openSession();
					Class entityClass = HibernateProxyHelper.getClassWithoutInitializingProxy(entity);
					Object fresh = signatureSession.get(entityClass, ((GeneralValueObject) entity).getId());
					if (fresh != null) target = fresh;
				}
				java.lang.reflect.Method method = target.getClass().getMethod("ttdQr");
				Object qrVal = method.invoke(target);
				parameters.put(baseKey + ".ttd.qr." + suffixKey, qrVal);
			} catch (NoSuchMethodException e) {
				// Entity ini memang tidak menyediakan tanda tangan QR.
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "ManajemenProperty.processSignature");
			} finally {
				try {
					if (signatureSession != null && signatureSession.isOpen()) {
						signatureSession.clear();
						signatureSession.disconnect();
						signatureSession.close();
					}
				} catch (Exception abaikan) {
				}
			}

			// 2. File TTD Logic
			if (lampiranType != null && entity instanceof GeneralValueObject) {
				Long id = ((GeneralValueObject) entity).getId();
				LampiranLain lam = LampiranLain.ambil(id, lampiranType);

				if (lam != null && isImageFile(lam.getNama())) {
					File file = lam.ambilFile();
					String path = (file == null || !file.exists()) ? "" : file.getAbsolutePath();
					if (path.length() > 0) {
						parameters.put(baseKey + ".ttd." + suffixKey, path);
					}
				}
			}
		} catch (Exception e) {
			// Silent catch seperti kode asli
		}
	}

	// ==================================================================================
	// LOGIC UTAMA: insertProperty (MAP)
	// ==================================================================================
	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	public static void insertProperty(Class clazz, GeneralValueObject generalValueObject,
			Map<String, Object> parameters, String nama, int deep, String... kecuali) {

		if (deep > 4 || generalValueObject == null || clazz == null) {
			return;
		}

		// BUG FIX (ClassCastException di getter id, mis. UangMuka vs SumberDana):
		// sejumlah pemanggil insertProperty() mem-passing parameter clazz statis
		// yang TIDAK sesuai dengan tipe runtime generalValueObject sebenarnya
		// (typo copy-paste di caller). Jika dibiarkan, classMetadata dibangun utk
		// clazz yang salah lalu classMetadata.getIdentifier()/getPropertyValue()
		// memanggil getter via reflection pada instance yang bukan miliknya ->
		// java.lang.ClassCastException terbungkus PropertyAccessException. Di sini
		// clazz diselaraskan ke kelas runtime sesungguhnya (unwrap proxy Hibernate
		// dgn Hibernate.getClass) bila tidak cocok, sehingga metadata & reflection
		// yang dipakai konsisten dengan objeknya.
		if (!clazz.isInstance(generalValueObject)) {
			try {
				Class actualClazz = Hibernate.getClass(generalValueObject);
				if (actualClazz != null) {
					clazz = actualClazz;
				}
			} catch (Exception e) {
				ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ManajemenProperty.java:insertProperty-clazzMismatch");
			}
		}

		ClassMetadata classMetadata = (clazz.getName().startsWith("ais.database.model.file.")
				|| clazz.getName().startsWith("ais.database.model.streaming."))
						? StreamingHibernateUtil.getInstance().getClassMetadata(clazz)
						: HibernateUtil.getClassMetadata(clazz);

		// FIX NPE: getClassMetadata() balik null utk class yg tak ter-mapping
		// Hibernate (pola sama dgn guard classMetadata==null di overload
		// JSONObject & processNestedJsonProperties di bawah). Tanpa ini,
		// classMetadata.getIdentifierPropertyName()/getPropertyNames() di bawah
		// melempar NPE mentah yg membatalkan SELURUH properti utk entity ini.
		if (classMetadata == null) {
			return;
		}

		// Put Identifier (ID)
		try {
			String idname = classMetadata.getIdentifierPropertyName();
			Object val = classMetadata.getIdentifier(generalValueObject, EntityMode.POJO);
			parameters.put(getKey(nama, idname), val == null ? "" : val.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:170");
		}

		try {
			String[] properties = classMetadata.getPropertyNames();
			for (String property : properties) {
				try {
					// Pengecekan Exclude
					if ("fileLocation".equals(property))
						continue;
					if (kecuali != null) {
						boolean skip = false;
						for (String kec : kecuali) {
							if (kec != null && kec.equalsIgnoreCase(property)) {
								skip = true;
								break;
							}
						}
						if (skip)
							continue;
					}

					Object val = classMetadata.getPropertyValue(generalValueObject, property, EntityMode.POJO);
					String currentKey = getKey(nama, property);

					parameters.put(currentKey, safeToString(val));

					// Recursive
					if (deep > 0 && val instanceof GeneralValueObject) {
						insertProperty(val.getClass(), (GeneralValueObject) val, parameters, currentKey, ++deep,
								kecuali);
					}

					// Handle Set (Collection) dengan StringBuilder
					if (val instanceof Set) {
						try {
							// FIX LazyInitializationException: koleksi lazy (mis.
							// Sekolah.penjurusanSekolahs) yang diakses dari thread
							// background (report generator) setelah sesi Hibernate
							// ditutup. instanceof Set di atas TIDAK memicu init,
							// tapi .iterator() di bawah memicu -- cek isInitialized
							// dulu sebelum iterasi, jangan andalkan catch semata
							// karena exception di sini bisa lolos lewat unwind
							// re-throw pada versi deploy lama (stale deploy).
							if (!Hibernate.isInitialized(val)) {
								parameters.put(currentKey + ".nama", "");
								parameters.put(currentKey + ".kode", "");
								parameters.put(currentKey + ".id", "");
								continue;
							}

							StringBuilder sbNama = new StringBuilder();
							StringBuilder sbKode = new StringBuilder();
							StringBuilder sbId = new StringBuilder();

							Set<GeneralValueObject> listObj = (Set<GeneralValueObject>) val;
							boolean first = true;
							for (GeneralValueObject obj : listObj) {
								if (!first) {
									sbNama.append(", ");
									sbKode.append(", ");
									sbId.append(", ");
								}
								sbNama.append(obj.getNama());
								sbKode.append(obj.getKode());
								sbId.append(obj.getId());
								first = false;
							}
							parameters.put(currentKey + ".nama", sbNama.toString());
							parameters.put(currentKey + ".kode", sbKode.toString());
							parameters.put(currentKey + ".id", sbId.toString());
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ManajemenProperty.java:227");
						}
					}
					// Handle Numbers
					else if (val instanceof Number) {
						if (val instanceof Integer)
							parameters.put(currentKey, val); // Preserve Integer type
						else
							parameters.put(currentKey, ((Number) val).doubleValue());

						formatNumber(parameters, currentKey, (Number) val);
					}
					// Handle Date
					else if (val instanceof Date) {
						formatDate(parameters, nama, property, val);
					}
					// Handle Specific Entities (Refactored logic)
					else {
						if (val instanceof GeneralValueObject && !Hibernate.isInitialized(val)) {
							parameters.put(currentKey + ".id", "");
							parameters.put(currentKey + ".nama", "");
							parameters.put(currentKey + ".kode", "");
							continue;
						}

						// Logic untuk TTD & QR
						if (val instanceof SuratKeluar) {
							parameters.put(currentKey + ".qr.surat_keluar", ((SuratKeluar) val).ttdQr());
						} else if (val instanceof SuratMasuk) {
							parameters.put(currentKey + ".qr.surat_masuk", ((SuratMasuk) val).ttdQr());
						} else if (val instanceof AlurPersetujuanSuratKeluarStatus) {
							parameters.put(currentKey + ".disposisi.qr.surat_keluar",
									((AlurPersetujuanSuratKeluarStatus) val).ttdQr());
						} else if (val instanceof AlurPersetujuanSuratMasukStatus) {
							parameters.put(currentKey + ".disposisi.qr.surat_masuk",
									((AlurPersetujuanSuratMasukStatus) val).ttdQr());
						} else if (val instanceof Guru) {
							processSignature(parameters, currentKey, val, LampiranLain.TTD_GURU, "guru");
						} else if (val instanceof Dosen) {
							processDosenHierarki(parameters, currentKey, (Dosen) val);
						} else if (val instanceof Mahasiswa) {
							Mahasiswa mhs = (Mahasiswa) val;
							processSignature(parameters, currentKey, mhs, LampiranLain.TTD_MAHASISWA, "mahasiswa");
							// Program Study Logic
							if (mhs.getProgram() != null) {
								for (Program program : Common.programs.values()) {
									if (program.getNama() != null
											&& mhs.getProgram().equalsIgnoreCase(program.getNama())) {
										parameters.put("program", program.getNamaBaru());
										break;
									}
								}
							}
							// Hierarki Mahasiswa -> Jurusan
							if (mhs.getJurusan() != null) {
								processJurusanHierarki(parameters, currentKey, mhs.getJurusan());
							}
						} else if (val instanceof Siswa) {
							Siswa siswa = (Siswa) val;
							processSignature(parameters, currentKey, siswa, LampiranLain.TTD_SISWA, "siswa");
							KelasSiswa ks = siswa.getKelas();
							parameters.put(currentKey + ".kelas.siswa", ks == null ? "" : ks.getNama());
							parameters.put(currentKey + ".kelassiswa", ks == null ? "" : ks.getNama());
						} else if (val instanceof Pegawai) {
							processSignature(parameters, currentKey, val, LampiranLain.TTD_PEGAWAI, "pegawai");
						} else if (val instanceof Tbmuser) {
							Tbmuser user = (Tbmuser) val;
							if (user.getPegawai() != null)
								processSignature(parameters, currentKey, user.getPegawai(), LampiranLain.TTD_PEGAWAI,
										"pegawai");
							if (user.getGuru() != null)
								processSignature(parameters, currentKey, user.getGuru(), LampiranLain.TTD_GURU, "guru");
							if (user.getDosen() != null)
								processDosenHierarki(parameters, currentKey, user.getDosen());
						} else if (val instanceof Jurusan) {
							((Jurusan) val).putFile(parameters);
							processJurusanHierarki(parameters, currentKey, (Jurusan) val);
						}

						// Handle File Loading Methods (Polymorphism manual)
						if (val instanceof Fakultas)
							((Fakultas) val).putFile(parameters);
						if (val instanceof PerguruanTinggi)
							((PerguruanTinggi) val).putFile(parameters);
						if (val instanceof Sekolah)
							((Sekolah) val).putFile(parameters);
						if (val instanceof Yayasan)
							((Yayasan) val).putFile(parameters);
					}

					// Recursive Sub-properties check (Legacy logic preserved)
					if (val instanceof GeneralValueObject) {
						ClassMetadata cmLocal = HibernateUtil.getClassMetadata(val.getClass());
						// FIX NPE: cmLocal (dan getPropertyNames()) bisa null utk class yg
						// tak ter-mapping Hibernate -- pola sama dgn guard classMetadata di
						// atas & di overload JSONObject/processNestedJsonProperties. Tanpa
						// ini, NPE mentah di sini bisa terpicu berulang saat rekursi deep
						// (val instanceof GeneralValueObject di level manapun).
						String[] pLocalNames = (cmLocal == null) ? null : cmLocal.getPropertyNames();
						if (pLocalNames != null) {
							for (String pLocal : pLocalNames) {
								try {
									Object vLocal = cmLocal.getPropertyValue(val, pLocal, EntityMode.POJO);
									String subKey = currentKey + "." + pLocal;
									parameters.put(subKey, safeToString(vLocal));

									if (vLocal instanceof Number)
										formatNumber(parameters, subKey, (Number) vLocal);
									if (vLocal instanceof Date)
										formatDate(parameters, currentKey, pLocal, vLocal);
								} catch (Exception e) {
									parameters.put(currentKey + "." + pLocal, "");
									if (!merupakanProxyAtauSessionTertutup(e)) {
										ais.common.ErrorAuditUtil.record(e,
											"auto-audit src/ais/common/ManajemenProperty.java:sub-property");
									}
								}
							}
						}
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:328");
					// Continue loop
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:332");
			// e.printStackTrace();
		}
	}

	private static boolean merupakanProxyAtauSessionTertutup(Throwable error) {
		Throwable current = error;
		while (current != null) {
			if (current instanceof org.hibernate.LazyInitializationException
					|| current instanceof org.hibernate.SessionException) {
				return true;
			}
			String pesan = current.getMessage();
			if (pesan != null) {
				String lower = pesan.toLowerCase();
				if (lower.indexOf("closed connection") >= 0 || lower.indexOf("session is closed") >= 0
						|| lower.indexOf("no session") >= 0) {
					return true;
				}
			}
			current = current.getCause();
		}
		return false;
	}

	// --- Helper khusus untuk Dosen dan Jurusan yang logic-nya kompleks dan
	// berulang ---

	private static void processDosenHierarki(Map<String, Object> params, String key, Dosen dosen) {
		processSignature(params, key, dosen, LampiranLain.TTD_DOSEN, "dosen");
		if (dosen.getJurusan() != null) {
			// Re-use logic jurusan tapi tanpa memanggil putFile jurusan (karena ini konteks
			// dosen)
			processJurusanHierarki(params, key, dosen.getJurusan());
		}

		// Logic Fakultas direct access dari Dosen (sama dengan Jurusan tapi via Dosen)
		// Note: Logic asli menduplikasi pengecekan Fakultas via Dosen dan via Jurusan.
		// Disini kita jalankan via Jurusan di atas, tapi jika Dosen punya fakultas
		// langsung:
		if (dosen.getFakultas() != null) {
			processFakultasHierarki(params, key, dosen.getFakultas());
		}
		if (dosen.getPerguruanTinggi() != null && dosen.getPerguruanTinggi().getKepala() != null) {
			processSignature(params, key, dosen.getPerguruanTinggi().getKepala(), LampiranLain.TTD_PEGAWAI, "rektor");
		}
	}

	private static void processJurusanHierarki(Map<String, Object> params, String key, Jurusan jurusan) {
		if (jurusan.getKaprodi() != null) {
			processSignature(params, key, jurusan.getKaprodi(), LampiranLain.TTD_DOSEN, "kaprodi");
		}
		if (jurusan.getFakultas() != null) {
			processFakultasHierarki(params, key, jurusan.getFakultas());
			// Rektor via Fakultas
			if (jurusan.getFakultas().getPerguruanTinggi() != null
					&& jurusan.getFakultas().getPerguruanTinggi().getKepala() != null) {
				processSignature(params, key, jurusan.getFakultas().getPerguruanTinggi().getKepala(),
						LampiranLain.TTD_PEGAWAI, "rektor");
			}
		}
	}

	private static void processFakultasHierarki(Map<String, Object> params, String key, Fakultas fak) {
		if (fak.getDekan() != null)
			processSignature(params, key, fak.getDekan(), LampiranLain.TTD_DOSEN, "dekan");
		if (fak.getPudek1() != null)
			processSignature(params, key, fak.getPudek1(), LampiranLain.TTD_DOSEN, "pudek1");
		if (fak.getPudek2() != null)
			processSignature(params, key, fak.getPudek2(), LampiranLain.TTD_DOSEN, "pudek2");
		if (fak.getPudek3() != null)
			processSignature(params, key, fak.getPudek3(), LampiranLain.TTD_DOSEN, "pudek3");
	}

	// ==================================================================================
	// LOGIC UTAMA: insertProperty (JSONObject)
	// ==================================================================================

	@SuppressWarnings("rawtypes")
	public static void insertProperty(Class clazz, GeneralValueObject generalValueObject, JSONObject parameters,
			String nama, int max, String... pengecualian) {
		insertProperty(clazz, generalValueObject, parameters, nama, 0, max, pengecualian);
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	public static void insertProperty(Class clazz, GeneralValueObject generalValueObject, JSONObject parameters,
			String nama, int deep, int max, String... pengecualian) {
		// FIX NPE: sejalan dengan guard yg sudah ada di overload Map (deep > 4 ||
		// generalValueObject == null || clazz == null). Tanpa ini, clazz.getName()
		// di bawah melempar NPE mentah (di luar try manapun) saat clazz/generalValueObject
		// null, membatalkan pemrosesan properti nested di level recursion manapun.
		if (deep >= max || clazz == null || generalValueObject == null)
			return;

		ClassMetadata classMetadata = (clazz.getName().startsWith("ais.database.model.file.")
				|| clazz.getName().startsWith("ais.database.model.streaming."))
						? StreamingHibernateUtil.getInstance().getClassMetadata(clazz)
						: HibernateUtil.getClassMetadata(clazz);

		// FIX NPE: getClassMetadata balik null utk class yg tak ter-mapping di
		// Hibernate (mis. field nested berupa POJO biasa). Tanpa guard ini,
		// classMetadata.getPropertyNames() di bawah melempar NPE yg lolos dari
		// catch per-properti & MEMBATALKAN SISA loop utk objek ini (bukan cuma
		// properti yg gagal) -- dampak lebih besar dari sekadar noise.
		if (classMetadata == null) {
			return;
		}

		// Put Identifier
		try {
			String idname = classMetadata.getIdentifierPropertyName();
			Object val = classMetadata.getIdentifier(generalValueObject, EntityMode.POJO);
			String keyId = getKey(nama, idname);
			parameters.put(keyId, val == null ? "" : val.toString());
			parameters.put(keyId + "_type", val == null ? "" : val.getClass().getName());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:414");
		}

		// Handle Photo Loading (Logic terpusat)
		if (generalValueObject != null) {
			handleEntityPhoto(parameters, generalValueObject);
		}

		try {
			String[] properties = classMetadata.getPropertyNames();
			for (String property : properties) {
				try {
					// Check exclusions
					if (property == null || isExcluded(property, pengecualian))
						continue;

					// Handle Special Case: JenisCatatanSiswa
					if (generalValueObject instanceof JenisCatatanSiswa
							&& "kelompokParameterTambahanCatatanSiswas".equalsIgnoreCase(property)) {
						Set<?> data = JenisCatatanSiswa.mapParameters.get(generalValueObject.getId());
						parameters.put(getKey(nama, property), data == null ? "" : data.toString());
						continue;
					}

					Object val = null;
					String currentKey = getKey(nama, property);

					// Fetch Value with Safe Session Handling (Try-with-resources for Java 7)
					try {
						val = classMetadata.getPropertyValue(generalValueObject, property, EntityMode.POJO);
						putJsonValue(parameters, currentKey, val);
					} catch (Exception e) {
						// Jika gagal, coba reload dari DB
						Session session = null;
						try {
							session = HibernateUtil.currentNativeSession();
							// Menggunakan Criteria untuk reload object
							Criteria criteria = session.createCriteria(clazz)
									.add(Restrictions.idEq(generalValueObject.getId()));
							GeneralValueObject reloadedObj = (GeneralValueObject) criteria.uniqueResult();

							if (reloadedObj != null) {
								val = classMetadata.getPropertyValue(reloadedObj, property, EntityMode.POJO);
								putJsonValue(parameters, currentKey, val);
							}
						} catch (Exception ee) { ais.common.ErrorAuditUtil.record(ee, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:459");
							// ignore
						} finally {
							if (session != null && session.isOpen()) {
								session.disconnect();
								session.close();
							}
							HibernateUtil.closeSession();
						}
					}

					// Recursion
					if (val != null && val instanceof GeneralValueObject) {
						GeneralValueObject generalValueObjectData = (GeneralValueObject) val;
						insertProperty(val.getClass(), generalValueObjectData, parameters, currentKey, ++deep, max);
						parameters.put(currentKey + ".id", generalValueObjectData.getId());
						parameters.put(currentKey + ".nama", generalValueObjectData.getNama());
						parameters.put(currentKey + ".kode", generalValueObjectData.getKode());
						parameters.put(currentKey + ".keterangan", generalValueObjectData.getKeterangan());
					} else if (val instanceof Date) {
						// Date formatting logic untuk JSON (agak duplikat dengan formatDate map, tapi
						// struktur key beda)
						putJsonDateExtras(parameters, currentKey, (Date) val);
					}

					// Deep traversal for simple objects inside complex object
					if (val instanceof GeneralValueObject) {
						processNestedJsonProperties((GeneralValueObject) val, parameters, currentKey, pengecualian);
					}

				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:489");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:492");
		}
	}

	// --- Helpers untuk JSONObject ---

	private static boolean isExcluded(String property, String[] pengecualian) {
		if ("fileLocation".equals(property) || "penjurusanSekolahs".equals(property)
				|| "tanggal_dirubah".equals(property)) {
			return true;
		}
		if (pengecualian != null) {
			String propLower = property.trim().toLowerCase();
			for (String ex : pengecualian) {
				String exLower = ex.trim().toLowerCase();
				if (exLower.equalsIgnoreCase(propLower) || exLower.startsWith(propLower) || exLower.endsWith(propLower)
						|| propLower.endsWith("." + exLower)) {
					return true;
				}
			}
		}
		return false;
	}

	private static void putJsonValue(JSONObject params, String key, Object val) throws Exception {
		// FIX LazyInitializationException: val bisa berupa proxy Hibernate lazy
		// (atau entity yg toString()-nya sendiri menyentuh field lazy lain, mis.
		// JenjangProgramStudi -> Jenjang) yg belum di-initialize saat sesi sudah
		// ditutup (thread API/login). Reuse safeToString() (sudah cek
		// Hibernate.isInitialized & catch exception dari toString() itu sendiri)
		// alih-alih panggil val.toString() mentah di sini.
		params.put(key, (val instanceof Date) ? Common.dateFormat5.get().format(val) : safeToString(val));
		params.put(key + "_type", val == null ? "" : val.getClass().getName());
		
		if (val != null && val instanceof GeneralValueObject) {
			GeneralValueObject generalValueObjectData = (GeneralValueObject) val;
			params.put(key + ".id", generalValueObjectData.getId());
			params.put(key + ".nama", generalValueObjectData.getNama());
			params.put(key + ".kode", generalValueObjectData.getKode());
			params.put(key + ".keterangan", generalValueObjectData.getKeterangan());
		}
	}

	private static void putJsonDateExtras(JSONObject params, String key, Date val) throws Exception {
		params.put(key + ".time", Common.timeFormat.get().format(val));
		params.put(key + ".formated1", Common.dateFormat6.get().format(val));
		params.put(key + ".formated2", Common.dateFormat2.get().format(val));
		params.put(key + ".formated3", Common.dateFormat51.get().format(val));
		params.put(key + ".formated4", Common.timeFormat.get().format(val));
		params.put(key + ".formated5", Common.dateFormat1.get().format(val));
		params.put(key + ".datetime_format", Common.databaseDateFormat.get().format(val));
		params.put(key + ".iso", Common.dateFormatInput.get().format(val));
	}

	private static void handleEntityPhoto(JSONObject params, GeneralValueObject obj) {
		// 1. Validasi awal untuk mencegah error null
		if (params == null || obj == null) {
			return;
		}

		try {
			// 2. Ambil class foto berdasarkan class object entity
			// Menggunakan obj.getClass() menggantikan deretan instanceof
			Class<?> fotoClass = DaftarDataService.MAP_ENTITY_TO_FOTO.get(obj.getClass().getName());

			if (fotoClass != null) {

				if (obj instanceof Tbmuser) {
					Tbmuser tbmuserDa = (Tbmuser) obj;
					if (tbmuserDa != null && tbmuserDa.getDosen() != null) {
						fotoClass = DaftarDataService.MAP_ENTITY_TO_FOTO.get(Dosen.class.getName());
						obj = tbmuserDa.getDosen();
					} else if (tbmuserDa != null && tbmuserDa.getGuru() != null) {
						fotoClass = DaftarDataService.MAP_ENTITY_TO_FOTO.get(Guru.class.getName());
						obj = tbmuserDa.getGuru();
					} else if (tbmuserDa != null && tbmuserDa.getPegawai() != null) {
						fotoClass = DaftarDataService.MAP_ENTITY_TO_FOTO.get(Pegawai.class.getName());
						obj = tbmuserDa.getPegawai();
					}
				}

				// 3. Ambil nilai static DEFAULT_JENIS menggunakan Reflection
				// Ini menghilangkan kebutuhan if-else untuk setiap tipe
				Field fieldJenis = fotoClass.getField("DEFAULT_JENIS");
				String jenis = (String) fieldJenis.get(null);
				Serializable idVal = obj.getId();

				if (obj.getClass().getName().equalsIgnoreCase(Pedagang.class.getName())) {
					Pedagang pedagang = (Pedagang) ConstantValues.ambil(Pedagang.class.getName(), idVal);
					if (pedagang != null && pedagang.getUserid() != null) {
						idVal = pedagang.getUserid();
						jenis = FotoAdmin.DEFAULT_JENIS;
					}
				} else if (obj.getClass().getName().equalsIgnoreCase(Produk.class.getName())) {
					jenis = Produk.class.getName();
				}

				// 4. Logika khusus (Special Case)
				// Hanya jalankan ini jika object benar-benar Mahasiswa
				if (obj instanceof Mahasiswa) {
					params.put("semesterSaatIni", ((Mahasiswa) obj).getSemesterSaatIni());
				}

				// 5. Logika pengambilan foto (Generic)
				if (jenis != null) {
					FileFotoLain fileFotoLain = FileFotoLain.ambil(idVal, jenis, fotoClass);

					if (fileFotoLain != null) {
						// Menggunakan boolean literal agar lebih mudah dibaca
						boolean isThumbnail = true;
						boolean isOriginal = false;

						String small = FileFotoLain.ambilLinkLampiranLain(fileFotoLain, false, false, fotoClass, true,
								false, isThumbnail);

						String big = FileFotoLain.ambilLinkLampiranLain(fileFotoLain, false, false, fotoClass, true,
								false, isOriginal);

						params.put("foto_kecil", small);
						params.put("foto_besar", big);
						params.put("foto_clazz", fotoClass.getName());
						params.put("foto_jenis", jenis);
					} else {

						String link = Common.getRequestHostWithProtocol() + "/img/"
								+ FileFotoLain.iconNggakAda(fotoClass);
						params.put("foto_kecil", link);
						params.put("foto_besar", link);
						params.put("foto_clazz", fotoClass.getName());
						params.put("foto_jenis", jenis);

					}
				}
			}
		} catch (NoSuchFieldException e) {
			System.err
					.println("Error: Class " + obj.getClass().getSimpleName() + " tidak memiliki field DEFAULT_JENIS.");
			// e.printStackTrace(); // Optional: uncomment untuk debug
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/common/ManajemenProperty.java:625");
		}
	}

	@SuppressWarnings("deprecation")
	private static void processNestedJsonProperties(GeneralValueObject val, JSONObject parameters, String currentKey,
			String[] pengecualian) {
		try {
			ClassMetadata meta = (val.getClass().getName().startsWith("ais.database.model.file.")
					|| val.getClass().getName().startsWith("ais.database.model.streaming."))
							? StreamingHibernateUtil.getInstance().getClassMetadata(val.getClass())
							: HibernateUtil.getClassMetadata(val.getClass());

			// FIX NPE: sama seperti insertProperty di atas -- meta bisa null utk
			// class yg tak ter-mapping, tanpa guard meta.getPropertyNames() di
			// bawah melempar NPE.
			if (meta == null) {
				return;
			}

			// Nested ID
			try {
				String idName = meta.getIdentifierPropertyName();
				Object idVal = meta.getIdentifier(val, EntityMode.POJO);
				parameters.put(currentKey + "." + idName, idVal == null ? "" : idVal.toString());
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:643");
			}

			// FIX NPE: getPropertyNames() bisa balik null utk metadata class tertentu
			// (distinct dari guard meta==null di atas) -- tanpa cek ini, for-each di
			// bawah melempar NPE mentah yg lolos ke caller (insertProperty), membatalkan
			// SISA properti sibling di level recursion pemanggil. subProp null juga
			// di-skip agar isExcluded(...) tak NPE saat .trim().
			String[] subProps = meta.getPropertyNames();
			if (subProps != null) {
				for (String subProp : subProps) {
					if (subProp == null || isExcluded(subProp, pengecualian))
						continue;

					try {
						Object subVal = meta.getPropertyValue(val, subProp, EntityMode.POJO);
						String subKey = currentKey + "." + subProp;
						// FIX LazyInitializationException: subVal bisa berupa proxy Hibernate lazy
						// (mis. Jenjang via JenjangProgramStudi.toString()) yg belum initialized &
						// sesi sudah tertutup. Reuse safeToString() (sudah cek Hibernate.isInitialized)
						// alih-alih panggil .toString() mentah.
						parameters.put(subKey,
								(subVal instanceof Date) ? Common.dateFormat5.get().format(subVal) : safeToString(subVal));

						if (subVal instanceof Date) {
							putJsonDateExtras(parameters, subKey, (Date) subVal);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:659");
					}
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/common/ManajemenProperty.java:662");
		}
	}
}
