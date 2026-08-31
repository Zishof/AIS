package ais.database.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Group;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;

import ais.action.master.helper.profile.ProfileUtil;
import ais.common.BacaTulisUtil;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.file.PertemuanFileContent;
import ais.database.model.file.TugasFileContent;
import ais.database.model.sekolah.GrupKategoriItemPenilaianSiswa;
import ais.database.model.sekolah.GrupPenilaian;
import ais.database.model.sekolah.JenisItemPenilaianSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.streaming.AudioPertemuan;
import ais.database.model.streaming.VideoPertemuan;
import ais.ui.util.MyToolbarbutton;
import ais.ui.util.MyLabelAgakKecilBold;

/**
 * Model data untuk tugas. Tipe ini membawa state yang dipertukarkan oleh lapisan persistence,
 * service, dan UI; makna bisnis utamanya ditentukan oleh field serta relasi yang dideklarasikan.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GeneralValueObject}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code String JSON}, {@code Tbmuser
 * currentUser}, {@code TugasFileContent currentTugasFileContent}; inisialisasi/lifecycle ({@code
 * reInitTugasFileContent()}, {@code reInitTugasFileContent()}); pembacaan/pencarian ({@code
 * getSyaratMengumpulkanTugas()}, {@code getFormatNilai()}, {@code getFormatNilais()}, {@code getProsentase()},
 * {@code getJudultugas()}, {@code getIsitugas()}); mutasi data ({@code setSyaratMengumpulkanTugas()}, {@code
 * setFormatNilai()}, {@code setFormatNilais()}, {@code setProsentase()}, {@code setJudultugas()}, {@code
 * setIsitugas()}); penghapusan/pembatalan ({@code removeTugasFileContent()}); operasi domain lain ({@code
 * tulisLokasiTugasFileContent()}, {@code bersihkanLokasiTugasFileContent()}, {@code populateTugasFileContent()},
 * {@code tugasFileContent()}, {@code apakahAkses()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> selain accessor state, operasi domain yang disebut di atas dapat membaca/mengubah
 * persistence, memicu lifecycle, atau membentuk komponen UI. Jangan menganggap model ini selalu murni;
 * panggil operasi tersebut melalui alur service dengan session, transaksi, dan otorisasi yang sesuai agar
 * perilakunya tidak disalin ke tempat lain.</p>
 *
 * @see GeneralValueObject
 */
public abstract class Tugas extends GeneralValueObject {

	public static String JSON = new JSONObject().toString();

	/**
	 * 
	 */
	private static final long serialVersionUID = 5021212497292420069L;

	public abstract SyaratUjian getSyaratMengumpulkanTugas();

	public abstract void setSyaratMengumpulkanTugas(SyaratUjian syaratMengumpulkanTugas);

	public abstract FormatNilai getFormatNilai();

	public abstract void setFormatNilai(FormatNilai formatNilai);

	public abstract String getFormatNilais();

	public abstract void setFormatNilais(String formatNilais);

	public abstract Double getProsentase();

	public abstract void setProsentase(Double prosentase);

	public abstract String getJudultugas();

	public abstract void setJudultugas(String judultugas);

	public abstract void setIsitugas(String isitugas);

	public abstract String getIsitugas();

	public abstract Date getMulai();

	public abstract void setMulai(Date mulai);

	public abstract Date getSelesai();

	public abstract void setSelesai(Date selesai);

	public abstract String getMhsYgTidakIkut();

	public abstract String getMhsBolehUploadUlang();

	public abstract void setMhsYgTidakIkut(String mhsYgTidakIkut);

	public abstract void setMhsBolehUploadUlang(String mhsBolehUploadUlang);

	public abstract String getSyaratAkses();

	public abstract void setSyaratAkses(String syaratAkses);

	public abstract JenisItemPenilaianSiswa getJenisItemPenilaianSiswa();

	public abstract void setJenisItemPenilaianSiswa(JenisItemPenilaianSiswa jenisItemPenilaianSiswa);

	public abstract GrupKategoriItemPenilaianSiswa getGrupKategoriItemPenilaianSiswa();

	public abstract void setGrupKategoriItemPenilaianSiswa(
			GrupKategoriItemPenilaianSiswa grupKategoriItemPenilaianSiswa);

	public abstract GrupPenilaian getGrupPenilaian();

	public abstract void setGrupPenilaian(GrupPenilaian grupPenilaian);

	public abstract String getKeteranganNilai();

	public abstract void setKeteranganNilai(String keteranganNilai);

	@SuppressWarnings("rawtypes")
	public static String ambilLokasiTugasFileContent(Serializable id, Class clazz) {
		if (id == null || clazz == null) {
			return VOMahasiswa.dataJSON;
		}
		File file = Common.getFileLocation(clazz, id, "tugas_file_content_" + id.toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:120");
		}
		return VOMahasiswa.dataJSON;
	}

	@SuppressWarnings("rawtypes")
	public static void tulisLokasiTugasFileContent(String data, Serializable id, Class clazz) {
		File file = Common.getFileLocation(clazz, id, "tugas_file_content_" + id.toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:130");
			// TODO Auto-generated catch block

		}
	}

	public void bersihkanLokasiTugasFileContent() {
		File file = Common.getFileLocation(this, "tugas_file_content_" + getId().toString());
		BacaTulisUtil.doHapus(file, "tugas_file_content");

	}

	public void reInitTugasFileContent() {
		Session session = StreamingHibernateUtil.getInstance().currentSession();
		reInitTugasFileContent(session);
		StreamingHibernateUtil.getInstance().closeSession();
	}

	@SuppressWarnings("unchecked")
	public void reInitTugasFileContent(Session session) {
		String sqlTambahan = "";
		if (this instanceof TugasPertemuan) {
			sqlTambahan = "class_from ilike '" + TugasPertemuan.class.getName() + "%'";
		} else {
			String clazzs = StringUtils.split(getClass().getName(), "_")[0];
			sqlTambahan = "(class_from is null or class_from ilike '" + clazzs + "%')";
		}
//		System.out.println("sqlTambahan => " + sqlTambahan);
		List<TugasFileContent> tugasFileContents = session.createCriteria(TugasFileContent.class)
				.add(Restrictions.sqlRestriction(sqlTambahan)).addOrder(Order.asc("id"))
				.add(Restrictions.eq("pertemuan", this.getId())).list();
		bersihkanLokasiTugasFileContent();
		tulisLokasiTugasFileContent(new JSONObject().toString(), getId(), getClass());
		for (TugasFileContent tugasFileContent : tugasFileContents) {
			populateTugasFileContent(tugasFileContent, getClass(), true);
		}
		tugasFileContents.clear();
		tugasFileContents = null;
	}

	@SuppressWarnings("rawtypes")
	public static void removeTugasFileContent(Serializable id, Class clazz) {
		try {
			JSONObject c = new JSONObject(ambilLokasiTugasFileContent(id, clazz));
			c.put(id.toString(), "");
			tulisLokasiTugasFileContent(c.toString(), id, clazz);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:176");

		}
	}

	public static void populateTugasFileContent(TugasFileContent tugasFileContent,
			@SuppressWarnings("rawtypes") Class clazz, boolean tulisUlang) {
		try {
			if (tugasFileContent == null) {
				return;
			}

			Long id = tugasFileContent.getMahasiswa() != null && tugasFileContent.getMahasiswa() > 0L
					? tugasFileContent.getMahasiswa()
					: tugasFileContent.getBiodataCalonMahasiswa();

			if (tugasFileContent.getSiswa() != null && tugasFileContent.getSiswa() > 0L) {
				id = tugasFileContent.getSiswa();
			}
			if (tugasFileContent.getCalonSiswa() != null && tugasFileContent.getCalonSiswa() > 0L) {
				id = tugasFileContent.getCalonSiswa();
			}

			JSONObject c = new JSONObject(ambilLokasiTugasFileContent(tugasFileContent.getPertemuan(), clazz));
			c.put(id.toString(), tugasFileContent.getId());
			tulisLokasiTugasFileContent(c.toString(), tugasFileContent.getPertemuan(), clazz);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:202");
		}
	}

	public boolean tugasFileContent() {

		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int ada = tugasFileContentsa.size();
		tugasFileContentsa = null;
		return ada > 0;
	}
	
	public int ambilJumlahTugasFileContent(Siswa siswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int jumlah = 0;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && siswa != null && tugasFileContent.getSiswa() != null
					&& tugasFileContent.getSiswa().equals(siswa.getId())) {
				jumlah++;
			}
			tugasFileContent = null;
		}
		tugasFileContentsa = null;
		return jumlah;
	}

	public int ambilJumlahTugasFileContent(Mahasiswa mahasiswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int jumlah = 0;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && mahasiswa != null && tugasFileContent.getMahasiswa() != null
					&& tugasFileContent.getMahasiswa().equals(mahasiswa.getId())) {
				jumlah++;
			}
			tugasFileContent = null;
		}
		tugasFileContentsa = null;
		return jumlah;
	}

	public int ambilJumlahTugasFileContent(BiodataCalonMahasiswa biodataCalonMahasiswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int jumlah = 0;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && biodataCalonMahasiswa != null
					&& tugasFileContent.getBiodataCalonMahasiswa() != null
					&& tugasFileContent.getBiodataCalonMahasiswa().equals(biodataCalonMahasiswa.getId())) {
				jumlah++;
			}
			tugasFileContent = null;
		}
		tugasFileContentsa = null;
		return jumlah;
	}
	
	public int ambilJumlahTugasFileContent() {
		return ambilJumlahTugasFileContent(false);
	}

	public int ambilJumlahTugasFileContent(boolean refresh) {
		if (getId() == null) {
			return 0;
		}
		if (refresh || !udah("tugas_file_content_" + getClass().getName())) {
			Session session = StreamingHibernateUtil.getInstance().currentSession();
			reInitTugasFileContent(session);
		}
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		int jumlah = tugasFileContentsa.size();
		tugasFileContentsa = null;
		return jumlah;
	}

	public TugasFileContent ambilTugasFileContent(Mahasiswa mahasiswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		TugasFileContent pilih = null;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && mahasiswa != null && tugasFileContent.getMahasiswa() != null
					&& tugasFileContent.getMahasiswa().equals(mahasiswa.getId())) {
				pilih = tugasFileContent;
				break;
			}
		}
		tugasFileContentsa = null;
		return pilih;
	}

	public TugasFileContent ambilTugasFileContent(Siswa siswa) {
		TreeMap<Long, TugasFileContent> tugasFileContentsa = ambilTugasFileContentTotal();
		TugasFileContent pilih = null;

		for (TugasFileContent tugasFileContent : tugasFileContentsa.values()) {
			if (tugasFileContent != null && siswa != null && tugasFileContent.getSiswa() != null
					&& tugasFileContent.getSiswa().equals(siswa.getId())) {
				pilih = tugasFileContent;
				break;
			}
		}
		tugasFileContentsa = null;
		return pilih;
	}

	public TreeMap<Long, TugasFileContent> ambilTugasFileContentTotal() {
		currentUser = null;
		TreeMap<Long, TugasFileContent> treemap = new TreeMap<Long, TugasFileContent>();
		if (getId() == null) {
			return treemap;
		}
		TreeMap<Long, TugasFileContent> d = ambilTugasFileContentTotal(treemap, "", null, 1000);
		treemap = null;
		return d;
	}

	public Tbmuser currentUser = null;
	public TugasFileContent currentTugasFileContent = null;

	public TreeMap<Long, TugasFileContent> ambilTugasFileContentTotal(TreeMap<Long, TugasFileContent> treemap,
			String cari, Paging paging, int banyak) {
		boolean refresh = false;
		return ambilTugasFileContentTotal(treemap, cari, paging, banyak, refresh);
	}

	@SuppressWarnings("unchecked")
	public TreeMap<Long, TugasFileContent> ambilTugasFileContentTotal(TreeMap<Long, TugasFileContent> treemap,
	        String cari, Paging paging, int banyak, boolean refresh) {
	    // FIX NPE: pemanggil (mis. TugasMandiriHelper dari background thread download) bisa
	    // mengirim treemap null (field belum diinisialisasi) -> treemap.keySet() di bawah meledak.
	    // Buat map kosong sendiri bila null, supaya kontrak method tetap "selalu return map valid".
	    if (treemap == null) {
	        treemap = new TreeMap<Long, TugasFileContent>();
	    }
	    if (!udah("tugas_file_content_" + getClass().getName()) || refresh) {
	        if (getId() == null) {
	            return treemap;
	        }
	        Session session = StreamingHibernateUtil.getInstance().currentSession();
	        reInitTugasFileContent(session);
	    }
	    TreeMap<Long, TugasFileContent> tugasFileContentsa = new TreeMap<Long, TugasFileContent>(
	            Collections.reverseOrder());

	    currentTugasFileContent = null;
	    try {
	        String lokasiContent = ambilLokasiTugasFileContent(getId(), getClass());
	        if (lokasiContent == null || lokasiContent.trim().isEmpty()
	                || !lokasiContent.trim().startsWith("{")) {
	            return treemap;
	        }
	        JSONObject c = new JSONObject(lokasiContent);
	        Iterator<String> keys = c.keys();
	        while (keys.hasNext()) {
	            String key = keys.next();
	            try {
	                // KODE YANG DIPERBAIKI: Menggunakan optString untuk menghindari JSONException "not a string"
	                String s = c.optString(key, ""); 
	                
	                if (!s.trim().isEmpty()) {

	                    if (Common.isNumber(s.trim())) {
	                        TugasFileContent tugasFileContent = (TugasFileContent) TugasFileContent.ambil(true,
	                                Long.parseLong(s.trim()), TugasFileContent.DEFAULT_JENIS, TugasFileContent.class);
	                        if (tugasFileContent != null) {
	                            Long id = tugasFileContent.getMahasiswa() != null
	                                    && tugasFileContent.getMahasiswa() > 0L ? tugasFileContent.getMahasiswa()
	                                            : tugasFileContent.getBiodataCalonMahasiswa();

	                            if (tugasFileContent.getSiswa() != null && tugasFileContent.getSiswa() > 0L) {
	                                id = tugasFileContent.getSiswa();
	                            }
	                            if (tugasFileContent.getCalonSiswa() != null && tugasFileContent.getCalonSiswa() > 0L) {
	                                id = tugasFileContent.getCalonSiswa();
	                            }

	                            if (tugasFileContent.getPertemuan() != null && tugasFileContent.getPertemuan() > 0L
	                                    && !getMhsYgTidakIkut().contains("," + id + ",")) {
	                                tugasFileContentsa.put(tugasFileContent.getId(), tugasFileContent);
	                            }
	                        }
	                    } else {
	                        File file = new File(s);
	                        if (file != null && file.exists()) {

	                            TugasFileContent tugasFileContent = (TugasFileContent) Common.convertToObject(
	                                    new JSONObject(ais.common.BacaTulisUtil.baca(file)), TugasFileContent.class);
	                            Long id = tugasFileContent.getMahasiswa() != null
	                                    && tugasFileContent.getMahasiswa() > 0L ? tugasFileContent.getMahasiswa()
	                                            : tugasFileContent.getBiodataCalonMahasiswa();

	                            if (tugasFileContent.getSiswa() != null && tugasFileContent.getSiswa() > 0L) {
	                                id = tugasFileContent.getSiswa();
	                            }
	                            if (tugasFileContent.getCalonSiswa() != null && tugasFileContent.getCalonSiswa() > 0L) {
	                                id = tugasFileContent.getCalonSiswa();
	                            }

	                            if (tugasFileContent.getPertemuan() != null && tugasFileContent.getPertemuan() > 0L
	                                    && !getMhsYgTidakIkut().contains("," + id + ",")) {
	                                tugasFileContentsa.put(tugasFileContent.getId(), tugasFileContent);
	                            }
	                        }
	                    }
	                }
	            } catch (Exception e) {
	                e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:391");
	            }
	        }
	    } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:394");
//	      e.printStackTrace();
	    }

	    for (Long id : tugasFileContentsa.keySet()) {

	        TugasFileContent tugasFileContent = tugasFileContentsa.get(id);
	        tugasFileContent.setPertemuan(this.getId());
	        if (tugasFileContent.getMahasiswa() != null && tugasFileContent.getMahasiswa() > 0L
	                && !treemap.containsKey(tugasFileContent.getMahasiswa()))
	            treemap.put(tugasFileContent.getMahasiswa(), tugasFileContent);
	        else if (tugasFileContent.getBiodataCalonMahasiswa() != null
	                && tugasFileContent.getBiodataCalonMahasiswa() > 0L
	                && !treemap.containsKey(tugasFileContent.getBiodataCalonMahasiswa()))
	            treemap.put(tugasFileContent.getBiodataCalonMahasiswa(), tugasFileContent);
	        else if (tugasFileContent.getSiswa() != null && tugasFileContent.getSiswa() > 0L
	                && !treemap.containsKey(tugasFileContent.getSiswa()))
	            treemap.put(tugasFileContent.getSiswa(), tugasFileContent);
	        else if (tugasFileContent.getCalonSiswa() != null && tugasFileContent.getCalonSiswa() > 0L
	                && !treemap.containsKey(tugasFileContent.getCalonSiswa()))
	            treemap.put(tugasFileContent.getCalonSiswa(), tugasFileContent);

	    }
	    tugasFileContentsa.clear();
	    tugasFileContentsa = null;
	    TreeMap<Long, TugasFileContent> treemapBaru = new TreeMap<Long, TugasFileContent>();
	    int index = 0;
	    int mulai = paging == null ? 0 : banyak * paging.getActivePage();
	    for (Long id : treemap.keySet()) {
	        TugasFileContent fileContent = treemap.get(id);

	        try {
	            String key = "";
	            if (fileContent.getMahasiswa() != null) {
	                key = fileContent.getMahasiswa() + "_mhs";
	            } else if (fileContent.getSiswa() != null) {
	                key = fileContent.getSiswa() + "_siswa";
	            } else if (fileContent.getBiodataCalonMahasiswa() != null) {
	                key = fileContent.getBiodataCalonMahasiswa() + "_cal_mhs";
	            } else if (fileContent.getCalonSiswa() != null) {
	                key = fileContent.getCalonSiswa() + "_cal_siswa";
	            }

	            JSONObject jsonObject = new JSONObject(getKeteranganNilai());
	            if (!jsonObject.isNull(key + "_ket")) {
	                String ket = jsonObject.get(key + "_ket") + "";
	                if (ket != null) {

//	                  System.out.println("key -> "+key+", ket "+ket);

	                    fileContent.setKeterangan(ket);
	                }
	            }

	            if (!jsonObject.isNull(key + "_nilai")) {
	                Double nilai = jsonObject.getDouble(key + "_nilai");
	                if (nilai != null) {

//	                  System.out.println("key -> "+key+", nilai "+nilai);

	                    fileContent.setNilai(nilai);
	                }
	            }
	        } catch (Exception e) {
	            e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:458");
	        }

	        if (fileContent.getMahasiswa() != null && fileContent.getMahasiswa() > 0L && currentUser != null
	                && currentUser.getMahasiswa() != null && currentUser.getMahasiswa().getId() != null
	                && currentUser.getMahasiswa().getId().equals(fileContent.getMahasiswa())) {
	            currentTugasFileContent = fileContent;
	        } else if (fileContent.getBiodataCalonMahasiswa() != null && fileContent.getBiodataCalonMahasiswa() > 0L
	                && currentUser != null && currentUser.getBiodataCalonMahasiswa() != null
	                && currentUser.getBiodataCalonMahasiswa().getId() != null
	                && currentUser.getBiodataCalonMahasiswa().getId().equals(fileContent.getBiodataCalonMahasiswa())) {
	            currentTugasFileContent = fileContent;
	        } else if (fileContent.getSiswa() != null && fileContent.getSiswa() > 0L && currentUser != null
	                && currentUser.getSiswa() != null && currentUser.getSiswa().getId() != null
	                && currentUser.getSiswa().getId().equals(fileContent.getSiswa())) {
	            currentTugasFileContent = fileContent;
	        } else if (fileContent.getCalonSiswa() != null && fileContent.getCalonSiswa() > 0L && currentUser != null
	                && currentUser.getCalonSiswa() != null && currentUser.getCalonSiswa().getId() != null
	                && currentUser.getCalonSiswa().getId().equals(fileContent.getCalonSiswa())) {
	            currentTugasFileContent = fileContent;
	        } else {
	            String realFile = fileContent.getNama();
	            if (cari == null || cari.trim().isEmpty()
	                    || (realFile != null && realFile.toLowerCase().trim().contains(cari.toLowerCase().trim()))) {
	                if (index >= mulai && index < (mulai + banyak)) {
	                    treemapBaru.put(id, fileContent);
	                }
	                index++;
	            }
	        }

	    }

	    if (paging != null) {
	        paging.setTotalSize(index);
	        paging.setVisible(index > banyak);
	        try {
	            paging.setPageSize(banyak);
	        } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/Tugas.java:496");
	        }
	        paging.setPageIncrement(Common.isMobile() ? 5 : 10);
	        paging.setMold("os");
	        try {
	            if (paging.getParent() != null) {
	                paging.getParent().setVisible(index > banyak);
	            }
	        } catch (Exception e) { /* paging/parent detached, safe to ignore */ }
	    }

	    return treemapBaru;
	}

	public List<TugasFileContent> ambilTugasFileContent(TreeMap<Long, TugasFileContent> tugasFileContentsa, int mulai,
			int banyak) {

		int index = 0;
		List<TugasFileContent> tugasFileContents = new ArrayList<TugasFileContent>();
		Iterator<Long> i = tugasFileContentsa.keySet().iterator();
		while (i.hasNext()) {
			TugasFileContent tugasFileContent = tugasFileContentsa.get(i.next());

			if (index >= mulai && index < (mulai + banyak)) {
				tugasFileContents.add(tugasFileContent);
			}
			index++;

		}
		return tugasFileContents;
	}

	public String ambilLink(TugasFileContent tugasFileContent) {

		if (tugasFileContent != null && tugasFileContent.getGdrive() != null) {
			return tugasFileContent.exportGDriveUrl();
		}

		String link = tugasFileContent == null ? null
				: (tugasFileContent.getLink() == null || tugasFileContent.getLink().isEmpty() ? null
						: tugasFileContent.getLink());

		if (link != null && link.toLowerCase().trim().contains("dropbox")) {
			return tugasFileContent.dropboxLinkRaw();
		}

		try {
			if (tugasFileContent != null && (link == null || link.trim().isEmpty() || !link.startsWith("http"))) {
				link = tugasFileContent.createLinkUri(false);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:547");
		}

		return link == null ? "" : link.trim();
	}

	@SuppressWarnings("deprecation")
	public static void tampilanSyarat(Pertemuan pertemuan, final Tugas tugas, final Ujian ujian,
			final PertemuanFileContent pertemuanFileContent, final AudioPertemuan audioPertemuan,
			final VideoPertemuan videoPertemuan, Rows rows, final Set<String> syaratAlert,
			final MyToolbarbutton buttonRefreshSyarat) {
		final VOPembelajaran pembelajaran = pertemuan == null ? null : pertemuan.ambilVOPembelajaran();
		if (pembelajaran != null) {

			Row rowTugas = new Row();
			Row rowUjian = new Row();
			Row rowMateri = new Row();

			final Rows rowsTugas = (Rows) Common.tampilanScroll1(rowTugas).getParent();
			final Rows rowsUjian = (Rows) Common.tampilanScroll1(rowUjian).getParent();
			final Rows rowsMateri = (Rows) Common.tampilanScroll1(rowMateri).getParent();

			final Textbox cariTugas = new Textbox();
			final Textbox cariUjian = new Textbox();
			final Textbox cariMateri = new Textbox();

			EventListener reloadMateri = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(rowsUjian);
					Common.clear(rowsTugas);
					Common.clear(rowsMateri);

					rowsTugas.setAttribute("index", 0);
					rowsUjian.setAttribute("index", 0);
					rowsMateri.setAttribute("index", 0);

					rowsTugas.setAttribute("ditampilkan", 25);
					rowsUjian.setAttribute("ditampilkan", 25);
					rowsMateri.setAttribute("ditampilkan", 25);

					rowsTugas.setAttribute("selesai", false);
					rowsUjian.setAttribute("selesai", false);
					rowsMateri.setAttribute("selesai", false);

					Group row = new Group();
					row.setParent(rowsTugas);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengumpulkan tugas sbb : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengikuti ujian ini, harus mengumpulkan tugas sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses materi ini, harus mengumpulkan tugas sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses audio ini, harus mengumpulkan tugas sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses video ini, harus mengumpulkan tugas sbb : "));

					row = new Group();
					row.setParent(rowsUjian);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengikuti ujian sbb : "));
					else if (ujian != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengikuti ujian ini, harus mengikuti ujian sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses materi ini, harus mengikuti ujian sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses audio ini, harus mengikuti ujian sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses video ini, harus mengikuti ujian sbb : "));

					row = new Group();
					row.setParent(rowsMateri);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengakses materi, audio, atau video sbb : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengikuti ujian ini, harus mengakses materi, audio, atau video sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses materi ini, harus mengakses materi, audio, atau video sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses audio ini, harus mengakses materi, audio, atau video sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses video ini, harus mengakses materi, audio, atau video sbb : "));

					if (tugas != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (tugas.getId() != null) {
											session.refresh(tugas);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(tugas.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										tugas.setSyaratAkses(jsonObject.toString());
										Common.refreshUpdate(session, tugas);
										session.flush();
									}

								}, new JSONObject(tugas.getSyaratAkses()), syaratAlert, tugas, buttonRefreshSyarat);
					} else if (ujian != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (ujian.getId() != null) {
											session.refresh(ujian);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(ujian.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										ujian.setSyaratAkses(jsonObject.toString());
										Common.refreshUpdate(session, ujian);
										session.flush();
									}

								}, new JSONObject(ujian.getSyaratAkses()), syaratAlert, ujian, buttonRefreshSyarat);
					} else if (pertemuanFileContent != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (pertemuanFileContent.getId() != null) {
											session.refresh(pertemuanFileContent);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(pertemuanFileContent.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										pertemuanFileContent.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, pertemuanFileContent);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(pertemuanFileContent.getSyaratAkses()), syaratAlert,
								pertemuanFileContent, buttonRefreshSyarat);
					} else if (audioPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (audioPertemuan.getId() != null) {
											session.refresh(audioPertemuan);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(audioPertemuan.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										audioPertemuan.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, audioPertemuan);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(audioPertemuan.getSyaratAkses()), syaratAlert, audioPertemuan,
								buttonRefreshSyarat);
					} else if (videoPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, true, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (videoPertemuan.getId() != null) {
											session.refresh(videoPertemuan);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(videoPertemuan.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										videoPertemuan.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, videoPertemuan);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(videoPertemuan.getSyaratAkses()), syaratAlert, videoPertemuan,
								buttonRefreshSyarat);
					}
				}
			};

			rowTugas.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowTugas, "2");

			rowUjian.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowUjian, "2");

			rowMateri.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowMateri, "2");

			try {
				reloadMateri.onEvent(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:819");
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void tampilanLain(Pertemuan pertemuan, final Tugas tugas, final Ujian ujian,
			final PertemuanFileContent pertemuanFileContent, final AudioPertemuan audioPertemuan,
			final VideoPertemuan videoPertemuan, Rows rows, final MyToolbarbutton buttonRefreshSyarat) {
		final VOPembelajaran pembelajaran = pertemuan == null ? null : pertemuan.ambilVOPembelajaran();
		if (pembelajaran != null) {

			Row rowTugas = new Row();
			Row rowUjian = new Row();
			Row rowMateri = new Row();

			final Rows rowsTugas = (Rows) Common.tampilanScroll1(rowTugas).getParent();
			final Rows rowsUjian = (Rows) Common.tampilanScroll1(rowUjian).getParent();
			final Rows rowsMateri = (Rows) Common.tampilanScroll1(rowMateri).getParent();

			final Textbox cariTugas = new Textbox();
			final Textbox cariUjian = new Textbox();
			final Textbox cariMateri = new Textbox();

			EventListener reloadMateri = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(rowsUjian);
					Common.clear(rowsTugas);
					Common.clear(rowsMateri);

					rowsTugas.setAttribute("index", 0);
					rowsUjian.setAttribute("index", 0);
					rowsMateri.setAttribute("index", 0);

					rowsTugas.setAttribute("ditampilkan", 25);
					rowsUjian.setAttribute("ditampilkan", 25);
					rowsMateri.setAttribute("ditampilkan", 25);

					rowsTugas.setAttribute("selesai", false);
					rowsUjian.setAttribute("selesai", false);
					rowsMateri.setAttribute("selesai", false);

					Group row = new Group();
					row.setParent(rowsTugas);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold("Tugas lain-nya : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar tugas : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar tugas : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar tugas : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar tugas : "));

					row = new Group();
					row.setParent(rowsUjian);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar ujian : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold("Ujian lain-nya : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar ujian : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar ujian : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar ujian : "));

					row = new Group();
					row.setParent(rowsMateri);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar materi, audio, atau video : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold("Daftar materi, audio, atau video : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold("Materi, audio, atau video lainnya : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Materi, audio, atau video lainnya : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold("Materi, audio, atau video lainnya : "));

					if (tugas != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (tugas.getId() != null) {
											session.refresh(tugas);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(tugas.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										tugas.setSyaratAkses(jsonObject.toString());
										Common.refreshUpdate(session, tugas);
										session.flush();
									}

								}, new JSONObject(tugas.getSyaratAkses()), null, tugas, buttonRefreshSyarat);
					} else if (ujian != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = HibernateUtil.currentSession();
										if (ujian.getId() != null) {
											session.refresh(ujian);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(ujian.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										ujian.setSyaratAkses(jsonObject.toString());
										Common.refreshUpdate(session, ujian);
										session.flush();
									}

								}, new JSONObject(ujian.getSyaratAkses()), null, ujian, buttonRefreshSyarat);
					} else if (pertemuanFileContent != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (pertemuanFileContent.getId() != null) {
											session.refresh(pertemuanFileContent);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(pertemuanFileContent.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										pertemuanFileContent.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, pertemuanFileContent);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(pertemuanFileContent.getSyaratAkses()), null, pertemuanFileContent,
								buttonRefreshSyarat);
					} else if (audioPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (audioPertemuan.getId() != null) {
											session.refresh(audioPertemuan);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(audioPertemuan.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										audioPertemuan.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, audioPertemuan);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(audioPertemuan.getSyaratAkses()), null, audioPertemuan,
								buttonRefreshSyarat);
					} else if (videoPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), true, false, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Session session = StreamingHibernateUtil.getInstance().currentSession();
										if (videoPertemuan.getId() != null) {
											session.refresh(videoPertemuan);
										}

										GeneralValueObject d = (GeneralValueObject) arg0.getData();
										String key = d.getId() + "_" + d.getClass().getSimpleName();

										Checkbox checkbox = (Checkbox) arg0.getTarget();
										JSONObject jsonObject = new JSONObject(videoPertemuan.getSyaratAkses());
										if (checkbox.isChecked()) {
											jsonObject.put(key, d.getClass().getName() + "_" + d.getId());
										} else {
											jsonObject.remove(key);
										}
										videoPertemuan.setSyaratAkses(jsonObject.toString());
										session.getTransaction().begin();
										Common.refreshUpdate(session, videoPertemuan);
										session.getTransaction().commit();
										StreamingHibernateUtil.getInstance().closeSession();

									}

								}, new JSONObject(videoPertemuan.getSyaratAkses()), null, videoPertemuan,
								buttonRefreshSyarat);
					}
				}
			};

			rowTugas.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowTugas, "2");

			rowUjian.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowUjian, "2");

			rowMateri.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowMateri, "2");

			try {
				reloadMateri.onEvent(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:1074");
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static void tampilanSyaratReadonly(Pertemuan pertemuan, final Tugas tugas, final Ujian ujian,
			final PertemuanFileContent pertemuanFileContent, final AudioPertemuan audioPertemuan,
			final VideoPertemuan videoPertemuan, Rows rows, final Set<String> syaratAlert,
			final MyToolbarbutton buttonRefreshSyarat) {
		final VOPembelajaran pembelajaran = pertemuan == null ? null : pertemuan.ambilVOPembelajaran();
		if (pembelajaran != null) {

			Row rowTugas = new Row();
			Row rowUjian = new Row();
			Row rowMateri = new Row();

			final Rows rowsTugas = (Rows) Common.tampilanScroll1(rowTugas).getParent();
			final Rows rowsUjian = (Rows) Common.tampilanScroll1(rowUjian).getParent();
			final Rows rowsMateri = (Rows) Common.tampilanScroll1(rowMateri).getParent();

			final Textbox cariTugas = new Textbox();
			final Textbox cariUjian = new Textbox();
			final Textbox cariMateri = new Textbox();

			EventListener reloadMateri = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(rowsUjian);
					Common.clear(rowsTugas);
					Common.clear(rowsMateri);

					rowsTugas.setAttribute("index", 0);
					rowsUjian.setAttribute("index", 0);
					rowsMateri.setAttribute("index", 0);

					rowsTugas.setAttribute("ditampilkan", 100);
					rowsUjian.setAttribute("ditampilkan", 100);
					rowsMateri.setAttribute("ditampilkan", 100);

					rowsTugas.setAttribute("selesai", false);
					rowsUjian.setAttribute("selesai", false);
					rowsMateri.setAttribute("selesai", false);

					Group row = new Group();
					row.setParent(rowsTugas);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengumpulkan tugas sbb : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengikuti ujian ini, harus mengumpulkan tugas sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses materi ini, harus mengumpulkan tugas sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses audio ini, harus mengumpulkan tugas sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses video ini, harus mengumpulkan tugas sbb : "));

					row = new Group();
					row.setParent(rowsUjian);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengikuti ujian sbb : "));
					else if (ujian != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengikuti ujian ini, harus mengikuti ujian sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses materi ini, harus mengikuti ujian sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses audio ini, harus mengikuti ujian sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(
								new MyLabelAgakKecilBold("Syarat mengakses video ini, harus mengikuti ujian sbb : "));

					row = new Group();
					row.setParent(rowsMateri);

					if (tugas != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengumpulkan tugas ini, harus mengakses materi, audio, atau video sbb : "));
					else if (ujian != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengikuti ujian ini, harus mengakses materi, audio, atau video sbb : "));
					else if (pertemuanFileContent != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses materi ini, harus mengakses materi, audio, atau video sbb : "));
					else if (audioPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses audio ini, harus mengakses materi, audio, atau video sbb : "));
					else if (videoPertemuan != null)
						row.appendChild(new MyLabelAgakKecilBold(
								"Syarat mengakses video ini, harus mengakses materi, audio, atau video sbb : "));

					if (tugas != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(tugas.getSyaratAkses()), syaratAlert, tugas, buttonRefreshSyarat);
					} else if (ujian != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(ujian.getSyaratAkses()), syaratAlert, ujian, buttonRefreshSyarat);
					} else if (pertemuanFileContent != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(pertemuanFileContent.getSyaratAkses()), syaratAlert,
								pertemuanFileContent, buttonRefreshSyarat);
					} else if (audioPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(audioPertemuan.getSyaratAkses()), syaratAlert, audioPertemuan,
								buttonRefreshSyarat);
					} else if (videoPertemuan != null) {
						ProfileUtil.tampilkanMateri(rowsTugas, rowsUjian, rowsMateri, cariTugas, cariUjian, cariMateri,
								pembelajaran.ambilPertemuan(), false, true, null,
								new JSONObject(videoPertemuan.getSyaratAkses()), syaratAlert, videoPertemuan,
								buttonRefreshSyarat);
					}
				}
			};

			rowTugas.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowTugas, "2");

			rowUjian.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowUjian, "2");

			rowMateri.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(rowMateri, "2");

			try {
				reloadMateri.onEvent(null);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:1216");
			}
		}
	}

	public static boolean apakahAkses(Pertemuan pertemuan, String akses, Long id) {
		TreeMap<String, String> d = pertemuan.ambilData(akses, null);
//		System.out.println("id " + id + " d -> " + d);
		for (String user : d.keySet()) {
			try {
				String[] u = user.split("-");
				String kode = u[1];

				Long mhs = Long.parseLong(kode);

//				System.out.println("mhs " + mhs);

				if (id.equals(mhs)) {
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/Tugas.java:1237");
			}
		}

		return false;
	}

}
