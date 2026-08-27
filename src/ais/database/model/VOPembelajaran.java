package ais.database.model;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.CascadeType;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import ais.action.master.helper.KrsDetailHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.kursus.KomponenDataProdukKursus;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.recruitment.JadwalUjianPegawai;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.database.model.sekolah.JadwalPertemuanPSB;
import ais.database.model.sekolah.JadwalUjianPSB;
import ais.database.model.sekolah.KelasLesSiswa;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sop.DisposisiSop;

public abstract class VOPembelajaran extends VoKunci {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4193008320801300777L;

	public abstract String getCourse();

	public abstract void setCourse(String course);

	public abstract Boolean getUrutkanotomatis();

	public abstract void setUrutkanotomatis(Boolean urutkanotomatis);

	private DisposisiSop disposisiSop;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE }, fetch = FetchType.LAZY)
	@JoinColumn(name = "disposisi_sop", nullable = true)
	public DisposisiSop getDisposisiSop() {
		disposisiSop = check(disposisiSop);
		return disposisiSop;
	}

	public void setDisposisiSop(DisposisiSop disposisiSop) {
		if (disposisiSop == null || disposisiSop.getId() == null) {
			return;
		}
		this.disposisiSop = (this.disposisiSop != null && (disposisiSop == null || disposisiSop.getId() == null))
				? this.disposisiSop
				: disposisiSop;
	}

	public String ambilLokasiPertemuan() {
		File file = Common.getFileLocation(this, "pertemuan_" + getId().toString());
		try {

			String data = ais.common.BacaTulisUtil.baca(file);
			return data == null || data.trim().isEmpty() ? VOMahasiswa.dataJSON : data;
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:83");
		}
		return VOMahasiswa.dataJSON;
	}

	public void tulisLokasiPertemuan(String data) {
		File file = Common.getFileLocation(this, "pertemuan_" + getId().toString());
		try {
			ais.common.BacaTulisUtil.tulis(file, data);
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:92");
			// TODO Auto-generated catch block

		}
	}

	public void removePertemuan(Serializable id) {
		try {
			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			c.put(id.toString(), "");
			tulisLokasiPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:103");

		}
	}

	public void populatePertemuan(Pertemuan pertemuan) {
		try {
			if (pertemuan == null) {
				return;
			}

			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			c.put(pertemuan.getId().toString(), pertemuan.getId().toString());
			tulisLokasiPertemuan(c.toString());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:117");
		}
	}

	public Integer ambilJumlahPertemuan() {
		TreeMap<String, Long> pertemuans = ambilPertemuan();
		int size = pertemuans != null ? pertemuans.size() : 0;
		if (pertemuans != null) {
			pertemuans.clear();
			pertemuans = null;
		}
		return size;
	}

	public int ambilJumlahPertemuan(boolean refresh) {
		TreeMap<String, Long> pertemuans = ambilPertemuan(refresh);
		int size = pertemuans != null ? pertemuans.size() : 0;
		if (pertemuans != null) {
			pertemuans.clear();
			pertemuans = null;
		}
		return size;
	}

	public Object[] ambilJumlahPertemuanStatistik(boolean termasukDiskusi, boolean termasukUjian) throws Exception {
		TreeMap<String, Long> pertemuanss = this.ambilPertemuan();
		List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
		
		if (pertemuanss != null && !pertemuanss.isEmpty()) {
			for (Long pertemuanid : pertemuanss.values()) {
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null) {
					pertemuans.add(pertemuan);
				}
			}
			pertemuanss.clear();
		}
		pertemuanss = null;

		return ambilJumlahPertemuanStatistik(pertemuans, null, null, termasukDiskusi, termasukUjian);
	}

	public Object[] ambilJumlahPertemuanStatistik(List<Pertemuan> pertemuans, Mahasiswa mahasiswa, Dosen dosen,
			boolean termasukDiskusi, boolean termasukUjian) throws Exception {

		int size = (pertemuans != null) ? pertemuans.size() : 0;
		int jumlahBerlalu = 0;
		int jumlahAbsensi = 0;
		int jumlahAbsensiTotal = 0;
		int jumlahUjianTotal = 0;
		int jumlahDiskusiTotal = 0;
		Map<String, Integer> semuaStatuses = new HashMap<String, Integer>();

		int jumlahAbsensiTotalDosen = 0;
		Map<String, Integer> semuaStatusesDosen = new HashMap<String, Integer>();

		Date sekarang = ais.ui.util.WaktuUtil.getDate();
		
		if (pertemuans != null) {
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null && pertemuan.getAktif() != null && pertemuan.getAktif()) {
					if (termasukUjian) {
						jumlahUjianTotal += pertemuan.ambilJumlahPertemuanPunyaUjian(mahasiswa, null);
					}

					if (termasukDiskusi) {
						jumlahDiskusiTotal += pertemuan.ambilJumlahPertemuanPunyaDiskusi(mahasiswa, dosen);
					}

					if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
						jumlahBerlalu++;
					}
					
					if (pertemuan.getAbsensi() != null && !pertemuan.getAbsensi().isEmpty()) {
						jumlahAbsensi++;
						
						Map<String, Integer> statuses = pertemuan.hitungStatus(mahasiswa);
						Map<String, Integer> statusesDosen = pertemuan.hitungStatusDosen(dosen);

						if (statuses != null) {
							for (Map.Entry<String, Integer> entry : statuses.entrySet()) {
								String key = entry.getKey();
								Integer value = entry.getValue();
								jumlahAbsensiTotal += value;
								
								if (semuaStatuses.containsKey(key)) {
									semuaStatuses.put(key, semuaStatuses.get(key) + value);
								} else {
									semuaStatuses.put(key, value);
								}
							}
						}

						if (statusesDosen != null) {
							for (Map.Entry<String, Integer> entry : statusesDosen.entrySet()) {
								String key = entry.getKey();
								Integer value = entry.getValue();
								jumlahAbsensiTotalDosen += value;
								
								if (semuaStatusesDosen.containsKey(key)) {
									semuaStatusesDosen.put(key, semuaStatusesDosen.get(key) + value);
								} else {
									semuaStatusesDosen.put(key, value);
								}
							}
						}
					}
				}
			}
		}

		return new Object[] { size, jumlahBerlalu, jumlahAbsensi, jumlahAbsensiTotal, semuaStatuses,
				jumlahAbsensiTotalDosen, semuaStatusesDosen, pertemuans, jumlahUjianTotal, jumlahDiskusiTotal };
	}

	public Object[] ambilJumlahPertemuanStatistik(Pertemuan pertemuan, Collection<Long> detailperkuliahans, Dosen dosen) {

		int size = (detailperkuliahans != null) ? detailperkuliahans.size() : 0;
		int jumlahBerlalu = 0;
		int jumlahAbsensi = 0;
		int jumlahAbsensiTotal = 0;
		int jumlahUjianTotal = 0;
		int jumlahDiskusiTotal = 0;
		Map<String, Integer> semuaStatuses = new HashMap<String, Integer>();

		int jumlahAbsensiTotalDosen = 0;
		Map<String, Integer> semuaStatusesDosen = new HashMap<String, Integer>();

		Date sekarang = ais.ui.util.WaktuUtil.getDate();
		
		if (detailperkuliahans != null && pertemuan != null) {
			for (Long detailperkuliahanid : detailperkuliahans) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				
				if (detailperkuliahan != null && detailperkuliahan.getMahasiswa() != null) {
					Mahasiswa mahasiswa = detailperkuliahan.getMahasiswa();
					
					jumlahUjianTotal += pertemuan.ambilJumlahPertemuanPunyaUjian(mahasiswa, null);
					jumlahDiskusiTotal += pertemuan.ambilJumlahPertemuanPunyaDiskusi(mahasiswa, dosen);

					if (pertemuan.getTanggal() != null && pertemuan.getTanggal().before(sekarang)) {
						jumlahBerlalu++;
					}
					
					if (pertemuan.getAbsensi() != null && !pertemuan.getAbsensi().isEmpty()) {
						jumlahAbsensi++;
						Map<String, Integer> statuses = pertemuan.hitungStatus(mahasiswa);
						
						if (statuses != null) {
							for (Map.Entry<String, Integer> entry : statuses.entrySet()) {
								String key = entry.getKey();
								Integer value = entry.getValue();
								jumlahAbsensiTotal += value;
								
								if (semuaStatuses.containsKey(key)) {
									semuaStatuses.put(key, semuaStatuses.get(key) + value);
								} else {
									semuaStatuses.put(key, value);
								}
							}
						}
					}
				}
			}
			
			if (pertemuan.getPerkuliahan() != null) {
				List<Dosen> dosens = pertemuan.getPerkuliahan().populateDosenBuNama();
				if (dosens != null) {
					for (Dosen d : dosens) {
						if (dosen == null || (d.getId() != null && d.getId().equals(dosen.getId()))) {
							jumlahDiskusiTotal += pertemuan.ambilJumlahPertemuanPunyaDiskusi(null, d);
						}

						Map<String, Integer> statusesDosen = pertemuan.hitungStatusDosen(d);
						if (statusesDosen != null) {
							for (Map.Entry<String, Integer> entry : statusesDosen.entrySet()) {
								String key = entry.getKey();
								Integer value = entry.getValue();
								jumlahAbsensiTotalDosen += value;
								
								if (semuaStatusesDosen.containsKey(key)) {
									semuaStatusesDosen.put(key, semuaStatusesDosen.get(key) + value);
								} else {
									semuaStatusesDosen.put(key, value);
								}
							}
						}
					}
				}
			}
		}

		return new Object[] { size, jumlahBerlalu, jumlahAbsensi, jumlahAbsensiTotal, semuaStatuses,
				jumlahAbsensiTotalDosen, semuaStatusesDosen, detailperkuliahans, jumlahUjianTotal, jumlahDiskusiTotal };
	}

	public Object[] ambilPertemuan(int mulai, int banyak, boolean tampilHal) {
		TreeMap<String, Long> pertemuansTemp = ambilPertemuan();
		return ambilPertemuan(mulai, banyak, tampilHal, pertemuansTemp);
	}

	public Object[] ambilPertemuan(int mulai, int banyak, boolean tampilHal, TreeMap<String, Long> pertemuansTemp) {
		int index = 0;
		List<Long> pertemuans = new ArrayList<Long>();
		int aktifKe = 0;

		if (pertemuansTemp == null) {
			return new Object[] { pertemuans, 0, mulai };
		}

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) - 1);
		
		for (String a : pertemuansTemp.keySet()) {
			try {
				if (a == null || a.indexOf("_") < 0 || a.split("_")[0] == null
						|| a.split("_")[0].trim().length() < 8) {
					continue;
				}
				Date tgl = Common.dateFormat8.get().parse(a.split("_")[0]);
				if (tgl.before(calendar.getTime())) {
					aktifKe++;
				}
			} catch (Exception e) {
				// Ignored
			}
		}

		if (tampilHal) {
			mulai = banyak == 1 ? aktifKe : ((int) (aktifKe / banyak)) * banyak;
		}

		if (mulai >= pertemuansTemp.size()) {
			mulai = mulai - banyak;
		}

		if (mulai < 0) {
			mulai = 0;
		}

		for (Long pertemuanid : pertemuansTemp.values()) {
			if (index >= mulai && index < (mulai + banyak)) {
				pertemuans.add(pertemuanid);
			}
			index++;
		}
		
		int size = pertemuansTemp.size();
		pertemuansTemp = null;
		
		return new Object[] { pertemuans, size, mulai };
	}

	public List<Pertemuan> ambilPertemuanList() {
		return ambilPertemuanList(false);
	}

	public List<Pertemuan> ambilPertemuanList(boolean refresh) {
		TreeMap<String, Long> pertemuanss = ambilPertemuan(refresh);
		List<Pertemuan> pertemuans = new ArrayList<Pertemuan>();
		
		if (pertemuanss != null) {
			for (Long pertemuanid : pertemuanss.values()) {
				if (pertemuanid == null) continue; // KE-8/9: nilai map bisa null -> toString() NPE
				Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, pertemuanid.toString());
				if (pertemuan != null && pertemuan.getAktif() != null && pertemuan.getAktif()) {
					pertemuans.add(pertemuan);
				}
			}
		}
		return pertemuans;
	}

	private void masukkanPertemuanLocal(TreeMap<String, Long> pertemuansTemp, Pertemuan pertemuan) {
		if (pertemuan != null && pertemuan.getId() != null && pertemuansTemp != null) {
			try {
				VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
				String urt;
				
				if (pembelajaran != null && !pembelajaran.getUrutkanotomatis()) {
					urt = "0000000000000000000000" + pertemuan.getPertemuanKe();
					urt = urt.substring(urt.length() - 4) + "_" + pertemuan.getId();
				} else {
					urt = (pertemuan.getTanggal() == null ? "" : Common.dateFormat8.get().format(pertemuan.getTanggal()))
							+ "_" + pertemuan.getId();
				}

				pertemuansTemp.put(urt, pertemuan.getId());
				
				// Setiap cabang di bawah HANYA valid bila runtime type `this` memang subclass
				// terkait — sebelumnya kondisi hanya mengecek pertemuan.getXxx() != null lalu
				// cast `this` tanpa syarat. Sebuah baris Pertemuan bisa punya lebih dari satu
				// FK non-null (mis. kelompok_kkn_id DAN formulir_kegiatan_id terisi bersamaan),
				// sehingga saat `this` sebenarnya instance FormulirKegiatan tapi cabang
				// getKelompokKkn()/getMahasiswaRequestTugasAkhir() kena duluan, cast paksa ke
				// KelompokKkn/MahasiswaRequestTugasAkhir melempar ClassCastException. Tambahkan
				// instanceof agar cabang yang tak cocok dilewati (bukan crash).
				if (pertemuan.getPerkuliahan() != null && this instanceof Perkuliahan) {
					pertemuan.setPerkuliahan((Perkuliahan) this);
				} else if (pertemuan.getKelompokKkn() != null && this instanceof KelompokKkn) {
					pertemuan.setKelompokKkn((KelompokKkn) this);
				} else if (pertemuan.getKelompokPkl() != null && this instanceof KelompokPkl) {
					pertemuan.setKelompokPkl((KelompokPkl) this);
				} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null
						&& this instanceof MahasiswaRequestTugasAkhir) {
					pertemuan.setMahasiswaRequestTugasAkhir((MahasiswaRequestTugasAkhir) this);
				} else if (pertemuan.getSkripsi() != null && this instanceof Skripsi) {
					pertemuan.setSkripsi((Skripsi) this);
				} else if (pertemuan.getKrsMahasiswa() != null && this instanceof KrsMahasiswa) {
					pertemuan.setKrsMahasiswa((KrsMahasiswa) this);
				} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null
						&& this instanceof PertemuanPunyaGrupPertemuan) {
					pertemuan.setPertemuanPunyaGrupPertemuan((PertemuanPunyaGrupPertemuan) this);
				} else if (pertemuan.getJadwalUjianPMB() != null && this instanceof JadwalUjianPMB) {
					pertemuan.setJadwalUjianPMB((JadwalUjianPMB) this);
				} else if (pertemuan.getJadwalUjianPSB() != null && this instanceof JadwalUjianPSB) {
					pertemuan.setJadwalUjianPSB((JadwalUjianPSB) this);
				} else if (pertemuan.getFormulirKegiatan() != null && this instanceof FormulirKegiatan) {
					pertemuan.setFormulirKegiatan((FormulirKegiatan) this);
				} else if (pertemuan.getJadwalPelajaran() != null && this instanceof JadwalPelajaran) {
					pertemuan.setJadwalPelajaran((JadwalPelajaran) this);
				} else if (pertemuan.getJadwalPertemuanPSB() != null && this instanceof JadwalPertemuanPSB) {
					pertemuan.setJadwalPertemuanPSB((JadwalPertemuanPSB) this);
				} else if (pertemuan.getWisuda() != null && this instanceof Wisuda) {
					pertemuan.setWisuda((Wisuda) this);
				} else if (pertemuan.getKelasLesSiswa() != null && this instanceof KelasLesSiswa) {
					pertemuan.setKelasLesSiswa((KelasLesSiswa) this);
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:432");
				// Ignored safely
			}
		}
	}

	public TreeMap<String, Long> ambilPertemuan() {
		return ambilPertemuan(false);
	}

	@SuppressWarnings("unchecked")
	public TreeMap<String, Long> ambilPertemuan(boolean refresh) {
		Session sessionTemp = null;
		
		if (!udah() || refresh) {
			try {
				sessionTemp = HibernateUtil.getSessionFactory().openSession();
				reInitPertemuan(sessionTemp);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:451");
			} finally {
				if (sessionTemp != null) {
					try { if (sessionTemp.isOpen()) sessionTemp.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:454");}
					try { sessionTemp.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:455");}
					try { if (sessionTemp.isOpen()) sessionTemp.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:456");}
				}
			}
		}
		
		TreeMap<String, Long> pertemuansTemp = new TreeMap<String, Long>();
		List<Long> idsBelumAda = new ArrayList<Long>();
		
		try {
			JSONObject c = new JSONObject(ambilLokasiPertemuan());
			Iterator<String> keys = c.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				try {
					String s = c.getString(key);
					if (s != null && !s.trim().isEmpty()) {
						Pertemuan pertemuan = null;
						GeneralValueObject generalValueObject = ambilData(Pertemuan.class, key);
						if (generalValueObject != null) {
							pertemuan = ((Pertemuan) generalValueObject);
						} else {
							idsBelumAda.add(Long.parseLong(key));
						}
						
						if (pertemuan != null && pertemuan.getAktif() != null && pertemuan.getAktif()) {
							masukkanPertemuanLocal(pertemuansTemp, pertemuan);
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:484");
					// Ignored
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:488");
			// Ignored
		}

		if (!idsBelumAda.isEmpty()) {
			Session session = null;
			try {
				session = HibernateUtil.getSessionFactory().openSession();
				List<Pertemuan> pertemuans = session.createCriteria(Pertemuan.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.in("id", idsBelumAda)).list();
						
				for (Pertemuan pertemuan : pertemuans) {
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {
						GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
						masukkanPertemuanLocal(pertemuansTemp, pertemuan);
					}
				}
				pertemuans.clear();
				pertemuans = null;
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:509");
			} finally {
				if (session != null) {
					try { if (session.isOpen()) session.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:512");}
					try { session.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:513");}
					try { if (session.isOpen()) session.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:514");}
				}
			}
		}

		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			if (perkuliahan.getUrutkanotomatis() != null && !perkuliahan.getUrutkanotomatis()) {
				TreeMap<String, Long> pertemuansTempBaru = new TreeMap<String, Long>();
				int index = 1;
				int maks = perkuliahan.getJumlahMaksimalPertemuan() != null ? perkuliahan.getJumlahMaksimalPertemuan() : 0;
				for (Map.Entry<String, Long> entry : pertemuansTemp.entrySet()) {
					if (index <= maks) {
						pertemuansTempBaru.put(entry.getKey(), entry.getValue());
					}
					index++;
				}
				pertemuansTemp.clear();
				pertemuansTemp = pertemuansTempBaru;
			}
		}

		idsBelumAda.clear();
		idsBelumAda = null;

		return pertemuansTemp;
	}

	public void reInitPertemuan(List<Pertemuan> pertemuans, Session session) {
		if (pertemuans == null || session == null) return;
		
		tulisLokasiPertemuan(new JSONObject().toString());
		int pertemuanKe = 1;
		boolean localTransaction = false;
		
		try {
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				localTransaction = true;
			}
			
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null && pertemuan.getAktif() != null && pertemuan.getAktif()) {
					VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
					if (pembelajaran != null && pembelajaran.getUrutkanotomatis() != null && pembelajaran.getUrutkanotomatis()) {
						if (pertemuan.getPertemuanKe() == null || !pertemuan.getPertemuanKe().equals(pertemuanKe)) {
							pertemuan.setPertemuanKe(pertemuanKe);
							// KE-FIX (NonUniqueObjectException "a different object with the same
							// identifier value was already associated with the session"): pertemuan
							// bisa saja bukan instance yang sama dgn yang sudah managed session utk
							// id yang sama (mis. termuat via ambilVOPembelajaran()/populatePertemuan()
							// sebelumnya). Evict instance lama dulu sebelum update() bila beda instance.
							if (!session.contains(pertemuan)) {
								Object existing = session.get(Pertemuan.class, pertemuan.getId());
								if (existing != null && existing != pertemuan) {
									session.evict(existing);
								}
							}
							session.update(pertemuan);
						}
					}
					pertemuanKe++;
					populatePertemuan(pertemuan);
				}
			}
			
			if (localTransaction) {
				session.getTransaction().commit();
			}
		} catch (Exception e) {
			if (localTransaction && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:574");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:576");
		}
	}

	@SuppressWarnings("unchecked")
	public void reInitPertemuan(Session session) {
		reInitPertemuan(session, true);
	}

	@SuppressWarnings("unchecked")
	private void reInitPertemuan(Session session, boolean bolehUlangSaatLock) {
		if (session == null) return;
		
		boolean localTransaction = false;
		
		try {
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				localTransaction = true;
			}
			
			List<Long> pertemuansId = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("id"))
					.addOrder((getUrutkanotomatis() != null && !getUrutkanotomatis()) ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal"))
					.addOrder(Order.asc("id"))
					.add(this instanceof Perkuliahan ? Restrictions.eq("perkuliahan", this)
							: this instanceof KelompokKkn ? Restrictions.eq("kelompokKkn", this)
							: this instanceof FormulirKegiatan ? Restrictions.eq("formulirKegiatan", this)
							: this instanceof KelompokPkl ? Restrictions.eq("kelompokPkl", this)
							: this instanceof JadwalUjianPMB ? Restrictions.eq("jadwalUjianPMB", this)
							: this instanceof JadwalUjianPegawai ? Restrictions.eq("jadwalUjianPegawai", this)
							: this instanceof MahasiswaRequestTugasAkhir ? Restrictions.eq("mahasiswaRequestTugasAkhir", this)
							: this instanceof Skripsi ? Restrictions.eq("skripsi", this)
							: this instanceof KomponenDataProdukKursus ? Restrictions.eq("komponenDataProdukKursus", this)
							: this instanceof Wisuda ? Restrictions.eq("wisuda", this)
							: this instanceof KrsMahasiswa ? Restrictions.eq("krsMahasiswa", this)
							: this instanceof JadwalUjianPSB ? Restrictions.eq("jadwalUjianPSB", this)
							: this instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", this)
							: this instanceof PertemuanPunyaGrupPertemuan ? Restrictions.eq("pertemuanPunyaGrupPertemuan", this)
							: Restrictions.sqlRestriction("false"))
					.list();

			if (pertemuansId == null || pertemuansId.isEmpty()) {
				if (localTransaction) session.getTransaction().commit();
				return;
			}

			List<Pertemuan> pertemuans = ambilDataBanyak(Pertemuan.class, pertemuansId);
			tulisLokasiPertemuan(new JSONObject().toString());
			
			int pertemuanKe = 1;
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null) {
					session.refresh(pertemuan);
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {
						VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
						if (pembelajaran != null && pembelajaran.getUrutkanotomatis() != null && pembelajaran.getUrutkanotomatis()) {
							if (pertemuan.getPertemuanKe() == null || !pertemuan.getPertemuanKe().equals(pertemuanKe)) {
								pertemuan.setPertemuanKe(pertemuanKe);
								session.update(pertemuan);
							}
						}
						pertemuanKe++;
						populatePertemuan(pertemuan);

						pertemuan.reInitPertemuanPunyaDiskusi(session);
						pertemuan.reInitPertemuanPunyaUjian(session);
						pertemuan.reInitTugasPertemuan(session);
						pertemuan.reInitTugasKelompok(session);
						pertemuan.reInitPengajuanIzinTidakMasukPerkuliahan(session);
						pertemuan.reInitKelompokParameterTambahanPertemuan(session);

						Collection<PengajuanIzinTidakMasukPerkuliahan> pengajuanIzinTidakMasukPerkuliahans = pertemuan.ambilPengajuanIzinTidakMasukPerkuliahanTotal();
						if (pengajuanIzinTidakMasukPerkuliahans != null) {
							for (PengajuanIzinTidakMasukPerkuliahan izin : pengajuanIzinTidakMasukPerkuliahans) {
								if (izin != null && izin.getDiizinkan() != null && izin.getDiizinkan()) {
									Mahasiswa mahasiswa = izin.getMahasiswa();
									if (mahasiswa != null) {
										pertemuan.populate(mahasiswa.getId(), izin.getStatusabsensi(),
												izin.getKeterangan(), null,
												pertemuan.retreiveAbsensiMulai(mahasiswa.getId()),
												pertemuan.retreiveAbsensiSampai(mahasiswa.getId()), "Mahasiswa");
										Common.refreshUpdate(session, pertemuan);
									}
								}
							}
							pengajuanIzinTidakMasukPerkuliahans.clear();
							pengajuanIzinTidakMasukPerkuliahans = null;
						}

						Session sessionSreaming = null;
						try {
							sessionSreaming = ais.database.hibernate.StreamingHibernateUtil.getInstance().getSessionFactory().openSession();
							pertemuan.reInitPertemuanFileContent(sessionSreaming);
							pertemuan.reInitTugasFileContent(sessionSreaming);
							pertemuan.reInitVideoPertemuan(sessionSreaming);
							pertemuan.reInitAudioPertemuan(sessionSreaming);
						} catch (Exception e) {
							e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:671");
						} finally {
							if (sessionSreaming != null) {
								try { if (sessionSreaming.isOpen()) sessionSreaming.clear(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:674");}
								try { sessionSreaming.disconnect(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:675");}
								try { if (sessionSreaming.isOpen()) sessionSreaming.close(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:676");}
							}
						}

						GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
					}
				}
			}
			
			pertemuans.clear();
			pertemuans = null;
			pertemuansId.clear();
			pertemuansId = null;

			if (localTransaction && session.isOpen()) {
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			// Guard session.isOpen(): bila session sudah ditutup (mis. helper nested memanggil
			// closeSession di tengah proses), memanggil getTransaction() di sini akan melempar
			// "Session is closed!" yang MENUTUPI error asli. Cek isOpen dulu.
			if (localTransaction && session.isOpen() && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:699");}
			}
			/* Penyusunan ulang nomor pertemuan dapat berbarengan dengan absensi atau
			 * sinkronisasi kelas yang mengubah row sama. PostgreSQL membatalkan transaksi
			 * dengan lock_timeout. Ulangi seluruh unit kerja sekali pada Session baru;
			 * transaksi yang gagal tidak boleh dipakai kembali. */
			if (bolehUlangSaatLock && adalahLockTimeoutPertemuan(e)) {
				Session sessionUlang = null;
				try {
					try { Thread.sleep(200L); } catch (InterruptedException terputus) {
						Thread.currentThread().interrupt();
					}
					sessionUlang = HibernateUtil.getSessionFactory().openSession();
					reInitPertemuan(sessionUlang, false);
					return;
				} catch (Exception ulangGagal) {
					ais.common.ErrorAuditUtil.record(ulangGagal,
							"retry reInitPertemuan setelah lock timeout");
				} finally {
					if (sessionUlang != null) {
						try { if (sessionUlang.isOpen()) sessionUlang.clear(); } catch (Exception abaikan) { }
						try { sessionUlang.disconnect(); } catch (Exception abaikan) { }
						try { if (sessionUlang.isOpen()) sessionUlang.close(); } catch (Exception abaikan) { }
					}
				}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:701");
		}
	}

	private static boolean adalahLockTimeoutPertemuan(Throwable error) {
		Throwable cek = error;
		while (cek != null) {
			String pesan = cek.getMessage();
			if (pesan != null) {
				String kecil = pesan.toLowerCase();
				if (kecil.indexOf("lock timeout") >= 0
						|| kecil.indexOf("canceling statement due to lock timeout") >= 0) {
					return true;
				}
			}
			cek = cek.getCause();
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public void reInitTugas(Session session) {
		if (session == null) return;
		
		boolean localTransaction = false;
		
		try {
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				localTransaction = true;
			}
			
			List<Long> pertemuansId = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("id"))
					.addOrder((getUrutkanotomatis() != null && !getUrutkanotomatis()) ? Order.asc("pertemuanKe") : Order.asc("tanggal"))
					.add(Restrictions.isNotNull("tanggal"))
					.addOrder(Order.asc("id"))
					.add(this instanceof Perkuliahan ? Restrictions.eq("perkuliahan", this)
							: this instanceof KelompokKkn ? Restrictions.eq("kelompokKkn", this)
							: this instanceof FormulirKegiatan ? Restrictions.eq("formulirKegiatan", this)
							: this instanceof KelompokPkl ? Restrictions.eq("kelompokPkl", this)
							: this instanceof JadwalUjianPMB ? Restrictions.eq("jadwalUjianPMB", this)
							: this instanceof JadwalUjianPegawai ? Restrictions.eq("jadwalUjianPegawai", this)
							: this instanceof MahasiswaRequestTugasAkhir ? Restrictions.eq("mahasiswaRequestTugasAkhir", this)
							: this instanceof Skripsi ? Restrictions.eq("skripsi", this)
							: this instanceof KomponenDataProdukKursus ? Restrictions.eq("komponenDataProdukKursus", this)
							: this instanceof Wisuda ? Restrictions.eq("wisuda", this)
							: this instanceof KrsMahasiswa ? Restrictions.eq("krsMahasiswa", this)
							: this instanceof JadwalUjianPSB ? Restrictions.eq("jadwalUjianPSB", this)
							: this instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", this)
							: this instanceof PertemuanPunyaGrupPertemuan ? Restrictions.eq("pertemuanPunyaGrupPertemuan", this)
							: Restrictions.sqlRestriction("false"))
					.list();

			if (pertemuansId == null || pertemuansId.isEmpty()) {
				if (localTransaction) session.getTransaction().commit();
				return;
			}

			List<Pertemuan> pertemuans = ambilDataBanyak(Pertemuan.class, pertemuansId);
			tulisLokasiPertemuan(new JSONObject().toString());
			
			int pertemuanKe = 1;
			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null) {
					session.refresh(pertemuan);
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {
						VOPembelajaran pembelajaran = pertemuan.ambilVOPembelajaran();
						if (pembelajaran != null && pembelajaran.getUrutkanotomatis() != null && pembelajaran.getUrutkanotomatis()) {
							if (pertemuan.getPertemuanKe() == null || !pertemuan.getPertemuanKe().equals(pertemuanKe)) {
								pertemuan.setPertemuanKe(pertemuanKe);
								session.update(pertemuan);
							}
						}
						pertemuanKe++;
						populatePertemuan(pertemuan);

						pertemuan.reInitTugasPertemuan(session);
						pertemuan.reInitTugasKelompok(session);

						GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
					}
				}
			}
			
			pertemuans.clear();
			pertemuans = null;
			pertemuansId.clear();
			pertemuansId = null;

			if (localTransaction) {
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			if (localTransaction && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:782");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:784");
		}
	}

	/**
	 * Menyinkronkan (membangun ulang) SELURUH data UJIAN milik objek pembelajaran ini —
	 * mencakup DAFTAR ujian per pertemuan, DATA PESERTA ujian, serta SOAL-SOAL ujian — lalu
	 * menuliskan hasilnya ke "tabel flag" (flag store berbasis berkas dengan cermin basis data
	 * melalui {@link ais.common.BacaTulisUtil}). Method ini adalah mesin di balik tombol toolbar
	 * "Syn.Ujian" pada halaman Manajemen Jadwal Perkuliahan, sekaligus dipanggil oleh proses
	 * "Singkronkan" (sinkronisasi menyeluruh) agar ringkasan jumlah peserta, status
	 * keikutsertaan, dan ketersediaan soal selalu konsisten dengan kondisi sebenarnya di basis
	 * data.
	 *
	 * <p><b>Latar belakang.</b> Sistem menyimpan relasi berat (pertemuan, ujian, peserta, dan
	 * soal) di dalam flag store agar pembacaan pada grid/laporan tidak perlu melakukan query
	 * berat berulang-ulang. Flag store tersebut bisa menjadi usang bila data diubah dari jalur
	 * lain (impor, ujian remedial, penggantian bank soal, pembatalan keikutsertaan, penambahan
	 * soal, dsb.). Tanpa proses sinkronisasi eksplisit, kolom "Jml Mhs", ringkasan
	 * keikutsertaan ujian, atau daftar soal dapat menampilkan angka lama. Method ini membangun
	 * ulang flag store dari sumber kebenaran (basis data) sehingga tampilannya kembali akurat.
	 *
	 * <p><b>Alur kerja.</b> (1) Bila belum ada transaksi aktif pada {@code session}, method
	 * membuka transaksi lokal sendiri dan bertanggung jawab melakukan commit/rollback — sehingga
	 * aman dipanggil baik dari {@code SyncHelper} (yang menyediakan session terisolasi per-thread
	 * tanpa transaksi) maupun dari alur lain yang sudah memegang transaksi. (2) Mengambil seluruh
	 * id {@link Pertemuan} AKTIF (aktif = null/true) yang bertanggal dan merujuk ke objek
	 * pembelajaran ini (Perkuliahan/KelompokKkn/Skripsi/JadwalPelajaran, dst.), memakai restriksi
	 * tipe yang identik dengan {@link #reInitPertemuan(Session)} dan {@link #reInitTugas(Session)}.
	 * (3) Untuk setiap pertemuan aktif: memuat instance-nya, memanggil
	 * {@link Pertemuan#reInitPertemuanPunyaUjian(Session)} untuk membangun ulang DAFTAR ujian
	 * (PertemuanPunyaUjian) pada pertemuan tersebut, lalu mengiterasi setiap
	 * {@link PertemuanPunyaUjian} untuk (a) membangun ulang DATA PESERTA melalui
	 * {@link PertemuanPunyaUjian#reInitHasilUjianMahasiswa(Session)} dan (b) membangun ulang
	 * SOAL-SOAL ujian melalui {@link Ujian#reInitUjianPunyaSoal(Session)} pada objek {@link Ujian}
	 * terkait. Setiap entitas yang telah diproses dicatat ke cache data global
	 * ({@link GeneralValueObject#masukkanData}). (4) Menutup transaksi lokal (commit) bila dibuka
	 * oleh method ini.
	 *
	 * <p><b>Manajemen memori &amp; session.</b> Koleksi id dan entitas dibebaskan (clear + null)
	 * setelah dipakai untuk menekan jejak memori saat sinkronisasi massal ribuan kelas secara
	 * paralel. Method ini TIDAK menutup {@code session} yang diberikan; penutupan session tetap
	 * tanggung jawab pemanggil ({@code SyncHelper} menutup session per-thread di blok finally-nya).
	 * Bila terjadi kegagalan dan transaksi lokal masih aktif, dilakukan rollback agar tidak
	 * meninggalkan transaksi menggantung. Kegagalan ditelan secara terkendali (dicetak ke log)
	 * agar satu kelas bermasalah tidak menghentikan sinkronisasi kelas lain pada thread pool.
	 *
	 * <p><b>Idempoten.</b> Pemanggilan berulang menghasilkan keadaan flag store yang sama: setiap
	 * sub-proses selalu membersihkan lokasi flag terlebih dahulu (bersihkan/tulisLokasi kosong)
	 * sebelum menuliskan ulang dari basis data, sehingga tidak terjadi duplikasi maupun sisa data
	 * usang.
	 *
	 * @param session session Hibernate untuk query &amp; penulisan flag; bila {@code null} method
	 *                langsung keluar tanpa efek.
	 */
	public void reInitUjian(Session session) {
		if (session == null) return;

		boolean localTransaction = false;

		try {
			if (!session.getTransaction().isActive()) {
				session.beginTransaction();
				localTransaction = true;
			}

			List<Long> pertemuansId = session.createCriteria(Pertemuan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.setProjection(Projections.property("id"))
					.add(Restrictions.isNotNull("tanggal"))
					.addOrder(Order.asc("id"))
					.add(this instanceof Perkuliahan ? Restrictions.eq("perkuliahan", this)
							: this instanceof KelompokKkn ? Restrictions.eq("kelompokKkn", this)
							: this instanceof FormulirKegiatan ? Restrictions.eq("formulirKegiatan", this)
							: this instanceof KelompokPkl ? Restrictions.eq("kelompokPkl", this)
							: this instanceof JadwalUjianPMB ? Restrictions.eq("jadwalUjianPMB", this)
							: this instanceof JadwalUjianPegawai ? Restrictions.eq("jadwalUjianPegawai", this)
							: this instanceof MahasiswaRequestTugasAkhir ? Restrictions.eq("mahasiswaRequestTugasAkhir", this)
							: this instanceof Skripsi ? Restrictions.eq("skripsi", this)
							: this instanceof KomponenDataProdukKursus ? Restrictions.eq("komponenDataProdukKursus", this)
							: this instanceof Wisuda ? Restrictions.eq("wisuda", this)
							: this instanceof KrsMahasiswa ? Restrictions.eq("krsMahasiswa", this)
							: this instanceof JadwalUjianPSB ? Restrictions.eq("jadwalUjianPSB", this)
							: this instanceof JadwalPelajaran ? Restrictions.eq("jadwalPelajaran", this)
							: this instanceof PertemuanPunyaGrupPertemuan ? Restrictions.eq("pertemuanPunyaGrupPertemuan", this)
							: Restrictions.sqlRestriction("false"))
					.list();

			if (pertemuansId == null || pertemuansId.isEmpty()) {
				if (localTransaction) session.getTransaction().commit();
				return;
			}

			List<Pertemuan> pertemuans = ambilDataBanyak(Pertemuan.class, pertemuansId);

			for (Pertemuan pertemuan : pertemuans) {
				if (pertemuan != null) {
					session.refresh(pertemuan);
					if (pertemuan.getAktif() != null && pertemuan.getAktif()) {

						// (a) Bangun ulang DAFTAR ujian (PertemuanPunyaUjian) pada pertemuan ini
						pertemuan.reInitPertemuanPunyaUjian(session);

						// (b) Untuk tiap ujian pada pertemuan: sinkron PESERTA + SOAL
						List<PertemuanPunyaUjian> pertemuanPunyaUjians = session
								.createCriteria(PertemuanPunyaUjian.class)
								.add(Restrictions.eq("pertemuan", pertemuan)).addOrder(Order.asc("id")).list();
						for (PertemuanPunyaUjian pertemuanPunyaUjian : pertemuanPunyaUjians) {
							if (pertemuanPunyaUjian == null) {
								continue;
							}

							// Peserta ujian (HasilUjianMahasiswa) -> flag store
							pertemuanPunyaUjian.reInitHasilUjianMahasiswa(session);

							// Soal-soal ujian (UjianPunyaSoal) pada objek Ujian terkait -> flag store
							Ujian ujian = pertemuanPunyaUjian.getUjian();
							if (ujian != null) {
								ujian.reInitUjianPunyaSoal(session);
							}

							GeneralValueObject.masukkanData(PertemuanPunyaUjian.class, pertemuanPunyaUjian);
						}

						GeneralValueObject.masukkanData(Pertemuan.class, pertemuan);
					}
				}
			}

			pertemuans.clear();
			pertemuans = null;
			pertemuansId.clear();
			pertemuansId = null;

			if (localTransaction) {
				session.getTransaction().commit();
			}

		} catch (Exception e) {
			if (localTransaction && session.getTransaction().isActive()) {
				try { session.getTransaction().rollback(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:924");}
			}
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:926");
		}
	}



	public String infoDosen(Dosen dosen) {
		if (dosen == null || dosen.getId() == null) {
			return "";
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null
					&& krsMahasiswa.getDosenPa().getId().equals(dosen.getId())) {
				return "Dosen Pembimbing Akademik";
			}
		} else if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			if (perkuliahan.getDosen1() != null && perkuliahan.getDosen1().getId().equals(dosen.getId())) {
				return "Dosen Utama";
			} else if (perkuliahan.getDosen2() != null && perkuliahan.getDosen2().getId().equals(dosen.getId())) {
				return "Dosen ke-2";
			} else if (perkuliahan.getDosen3() != null && perkuliahan.getDosen3().getId().equals(dosen.getId())) {
				return "Dosen ke-3";
			} else if (perkuliahan.getDosen4() != null && perkuliahan.getDosen4().getId().equals(dosen.getId())) {
				return "Dosen ke-4";
			} else if (perkuliahan.getDosen5() != null && perkuliahan.getDosen5().getId().equals(dosen.getId())) {
				return "Dosen ke-5";
			} else if (perkuliahan.getDosen6() != null && perkuliahan.getDosen6().getId().equals(dosen.getId())) {
				return "Dosen ke-6";
			} else if (perkuliahan.getDosen7() != null && perkuliahan.getDosen7().getId().equals(dosen.getId())) {
				return "Dosen ke-7";
			} else if (perkuliahan.getDosen8() != null && perkuliahan.getDosen8().getId().equals(dosen.getId())) {
				return "Dosen ke-8";
			} else if (perkuliahan.getDosen9() != null && perkuliahan.getDosen9().getId().equals(dosen.getId())) {
				return "Dosen ke-9";
			} else if (perkuliahan.getDosen10() != null && perkuliahan.getDosen10().getId().equals(dosen.getId())) {
				return "Dosen ke-10";
			}
		} else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			if (kelompokKkn.getDosen_pembimbing1() != null
					&& kelompokKkn.getDosen_pembimbing1().getId().equals(dosen.getId())) {
				return "Pembimbing Utama";
			} else if (kelompokKkn.getDosen_pembimbing2() != null
					&& kelompokKkn.getDosen_pembimbing2().getId().equals(dosen.getId())) {
				return "Pembimbing ke-2";
			} else if (kelompokKkn.getDosen_pembimbing3() != null
					&& kelompokKkn.getDosen_pembimbing3().getId().equals(dosen.getId())) {
				return "Pembimbing ke-3";
			} else if (kelompokKkn.getDosen_pembimbing4() != null
					&& kelompokKkn.getDosen_pembimbing4().getId().equals(dosen.getId())) {
				return "Pembimbing ke-4";
			} else if (kelompokKkn.getDosen_pembimbing5() != null
					&& kelompokKkn.getDosen_pembimbing5().getId().equals(dosen.getId())) {
				return "Pembimbing ke-5";
			}
		} else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			if (kelompokPkl.getDosen_pembimbing1() != null
					&& kelompokPkl.getDosen_pembimbing1().getId().equals(dosen.getId())) {
				return "Pembimbing Utama";
			} else if (kelompokPkl.getDosen_pembimbing2() != null
					&& kelompokPkl.getDosen_pembimbing2().getId().equals(dosen.getId())) {
				return "Pembimbing ke-2";
			} else if (kelompokPkl.getDosen_pembimbing3() != null
					&& kelompokPkl.getDosen_pembimbing3().getId().equals(dosen.getId())) {
				return "Pembimbing ke-3";
			} else if (kelompokPkl.getDosen_pembimbing4() != null
					&& kelompokPkl.getDosen_pembimbing4().getId().equals(dosen.getId())) {
				return "Pembimbing ke-4";
			} else if (kelompokPkl.getDosen_pembimbing5() != null
					&& kelompokPkl.getDosen_pembimbing5().getId().equals(dosen.getId())) {
				return "Pembimbing ke-5";
			}
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			FormatNilaiSkripsi f = skripsi.getFormatNilaiSkripsi();
			if (f != null) {
				if (skripsi.getKetuaSidang() != null) {
					return f.getDosen1();
				}
				if (skripsi.getPembimbing() != null) {
					return f.getDosen2();
				}

				if (skripsi.getPembimbing3() != null) {
					return "Pembimbing III";
				}

				if (skripsi.getPenguji1() != null) {
					return f.getDosen3();
				}
				if (skripsi.getPenguji2() != null) {
					return f.getDosen4();
				}
				if (skripsi.getPenguji3() != null) {
					return f.getDosen5();
				}
				if (skripsi.getPenguji4() != null) {
					return f.getDosen6();
				}
			}
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			FormatNilaiProposalSkripsi f = mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi();
			if (f != null) {
				if (mahasiswaRequestTugasAkhir.getDosen1() != null) {
					return f.getDosen1();
				}
				if (mahasiswaRequestTugasAkhir.getDosen2() != null) {
					return f.getDosen2();
				}

				if (mahasiswaRequestTugasAkhir.getDosen3() != null) {
					return f.getDosen3();
				}

				if (mahasiswaRequestTugasAkhir.getDosen4() != null) {
					return f.getDosen4();
				}

				if (mahasiswaRequestTugasAkhir.getDosen5() != null) {
					return f.getDosen5();
				}
				if (mahasiswaRequestTugasAkhir.getDosen6() != null) {
					return f.getDosen6();
				}
			}
		} else if (this instanceof GrupPertemuan) {
			GrupPertemuan grupPertemuan = (GrupPertemuan) this;
			if (grupPertemuan != null && grupPertemuan.getDosen() != null
					&& grupPertemuan.getDosen().getId().equals(dosen.getId())) {
				return "Dosen Pembimbing " + grupPertemuan.getJenis();
			}
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null
					&& pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen() != null
					&& pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen().getId().equals(dosen.getId())) {
				return "Dosen Pembimbing " + pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenis();
			}
		} else if (this instanceof FormulirKegiatan) {
			return "";
		}

		return "Bukan dosen pengajar / pembimbing";
	}

	public static String infoSimple(Pertemuan pertemuan) {
		String key = pertemuan.info();
		if (pertemuan.getPerkuliahan() != null) {
			key = pertemuan.getPerkuliahan().infoSimple();
		} else if (pertemuan.getKrsMahasiswa() != null) {
			key = pertemuan.getKrsMahasiswa().infoSimple();
		} else if (pertemuan.getJadwalUjianPMB() != null) {
			key = pertemuan.getJadwalUjianPMB().infoSimple();
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			key = pertemuan.getMahasiswaRequestTugasAkhir().infoSimple();
		} else if (pertemuan.getKelompokKkn() != null) {
			key = pertemuan.getKelompokKkn().infoSimple();
		} else if (pertemuan.getKelompokPkl() != null) {
			key = pertemuan.getKelompokPkl().infoSimple();
		} else if (pertemuan.getSkripsi() != null) {
			key = pertemuan.getSkripsi().infoSimple();
		} else if (pertemuan.getJadwalUjianPSB() != null) {
			key = pertemuan.getJadwalUjianPSB().infoSimple();
		} else if (pertemuan.getJadwalPelajaran() != null) {
			key = pertemuan.getJadwalPelajaran().infoSimple();
		} else if (pertemuan.getPertemuanPunyaGrupPertemuan() != null) {
			key = pertemuan.getPertemuanPunyaGrupPertemuan().infoSimple();
		} else if (pertemuan.getFormulirKegiatan() != null) {
			key = pertemuan.getFormulirKegiatan().infoSimple();
		}
		return key;
	}

	public static List<String> getOrganizer(Pertemuan pertemuan) {
		List<String> emails = new ArrayList<String>();
		List<Dosen> dosens = new ArrayList<Dosen>();
		if (pertemuan.getPerkuliahan() != null) {
			dosens = pertemuan.getPerkuliahan().populateDosenBuNama();
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			dosens = pertemuan.getMahasiswaRequestTugasAkhir().populateDosenBuNama();
		} else if (pertemuan.getSkripsi() != null) {
			dosens = pertemuan.getSkripsi().populateDosenBuNama();
		} else if (pertemuan.getKelompokKkn() != null) {
			dosens = pertemuan.getKelompokKkn().populateDosenBuNama();
		} else if (pertemuan.getKelompokPkl() != null) {
			dosens = pertemuan.getKelompokPkl().populateDosenBuNama();
		} else if (pertemuan.getKrsMahasiswa() != null) {
			dosens = pertemuan.getKrsMahasiswa().populateDosenBuNama();
		}
		for (Dosen dosen : dosens) {
			if (dosen.getEmail() != null && !dosen.getEmail().trim().isEmpty()) {
				String email = dosen.getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}
		dosens = null;
		String email = Common.getKonfigurasi("alamat_email_monitoring", "").getNilai();
		for (String e : email.split(",")) {
			if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
				emails.add(e.trim());
			}
		}
		return emails;
	}

	@SuppressWarnings("unchecked")
	public static Set<String> getAttendee(Pertemuan pertemuan) {

		List<Dosen> dosens = new ArrayList<Dosen>();
		Set<String> emails = new HashSet<String>();
		if (pertemuan.getPerkuliahan() != null) {
			dosens = pertemuan.getPerkuliahan().populateDosenBuNama();
			for (Long detailperkuliahanid : pertemuan.getPerkuliahan().ambilDetailperkuliahan()) {
				Detailperkuliahan detailperkuliahan = (Detailperkuliahan) GeneralValueObject
						.ambilData(Detailperkuliahan.class, detailperkuliahanid.toString());
				if (detailperkuliahan != null) {
					if (detailperkuliahan.getMahasiswa().getEmail() != null
							&& !detailperkuliahan.getMahasiswa().getEmail().trim().isEmpty()) {

						String email = detailperkuliahan.getMahasiswa().getEmail();
						for (String e : email.split(",")) {
							if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
								emails.add(e.trim());
							}
						}
					}
				}
			}
		} else if (pertemuan.getJadwalUjianPMB() != null && pertemuan.getJadwalUjianPMB().getUjianPMB() != null
				&& pertemuan.getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran() != null) {
			Session session = HibernateUtil.currentNativeSession();
			List<String> d = session.createCriteria(BiodataCalonMahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.eq("gelombangPendaftaran",
							pertemuan.getJadwalUjianPMB().getUjianPMB().getGelombangPendaftaran()))
					.add(pertemuan.getJadwalUjianPMB().getPaket() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("paket", pertemuan.getJadwalUjianPMB().getPaket()))
					.setProjection(Projections.groupProperty("email")).add(Restrictions.isNotNull("email"))
					.add(Restrictions.ne("email", "")).list();
			HibernateUtil.closeSession();
			for (String email : d) {
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (pertemuan.getFormulirKegiatan() != null) {
			Session session = HibernateUtil.currentNativeSession();
			List<Object[]> d = session.createCriteria(FormulirKegiatanPeserta.class)
					.add(Restrictions.eq("formulirKegiatan", pertemuan.getFormulirKegiatan()))
					.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
					.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
					.setProjection(Projections.projectionList().add(Projections.property("dosen.email"))
							.add(Projections.property("mahasiswa.email")))
					.add(Restrictions.or(Restrictions.isNotNull("dosen.email"),
							Restrictions.isNotNull("mahasiswa.email")))
					.add(Restrictions.or(Restrictions.ne("dosen.email", ""), Restrictions.ne("mahasiswa.email", "")))
					.list();
			HibernateUtil.closeSession();
			for (Object[] a : d) {
				try {
					String email = a[0] == null ? a[1].toString() : a[0].toString();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}
				} catch (Exception e) {
					e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:1202");
				}
			}
		} else if (pertemuan.getMahasiswaRequestTugasAkhir() != null) {
			dosens = pertemuan.getMahasiswaRequestTugasAkhir().populateDosenBuNama();
			if (pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getEmail() != null
					&& !pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getEmail().trim().isEmpty()) {

				String email = pertemuan.getMahasiswaRequestTugasAkhir().getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (pertemuan.getSkripsi() != null) {
			dosens = pertemuan.getSkripsi().populateDosenBuNama();
			if (pertemuan.getSkripsi().getMahasiswa().getEmail() != null
					&& !pertemuan.getSkripsi().getMahasiswa().getEmail().trim().isEmpty()) {
				String email = pertemuan.getSkripsi().getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		} else if (pertemuan.getKelompokKkn() != null) {
			dosens = pertemuan.getKelompokKkn().populateDosenBuNama();
			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : pertemuan.getKelompokKkn()
					.ambilMahasiswaDapatKelompokKkn(false)) {
				if (mahasiswaDapatKelompokKkn.getMahasiswa().getEmail() != null
						&& !mahasiswaDapatKelompokKkn.getMahasiswa().getEmail().trim().isEmpty()) {

					String email = mahasiswaDapatKelompokKkn.getMahasiswa().getEmail();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}

				}
			}
		} else if (pertemuan.getKelompokPkl() != null) {
			dosens = pertemuan.getKelompokPkl().populateDosenBuNama();
			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : pertemuan.getKelompokPkl()
					.ambilMahasiswaDapatKelompokPkl(false)) {
				if (mahasiswaDapatKelompokPkl.getMahasiswa().getEmail() != null
						&& !mahasiswaDapatKelompokPkl.getMahasiswa().getEmail().trim().isEmpty()) {
					String email = mahasiswaDapatKelompokPkl.getMahasiswa().getEmail();
					for (String e : email.split(",")) {
						if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
							emails.add(e.trim());
						}
					}
				}
			}
		} else if (pertemuan.getKrsMahasiswa() != null) {
			dosens = pertemuan.getKrsMahasiswa().populateDosenBuNama();
			if (pertemuan.getKrsMahasiswa().getMahasiswa().getEmail() != null
					&& !pertemuan.getKrsMahasiswa().getMahasiswa().getEmail().trim().isEmpty()) {
				String email = pertemuan.getKrsMahasiswa().getMahasiswa().getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}

			}
		}

		for (Dosen dosen : dosens) {
			if (dosen.getEmail() != null && !dosen.getEmail().trim().isEmpty()) {
				String email = dosen.getEmail();
				for (String e : email.split(",")) {
					if (!e.trim().isEmpty() && Common.isValidEmailAddress(e)) {
						emails.add(e.trim());
					}
				}
			}
		}
		dosens = null;
		return emails;
	}

	public TreeMap<String, Dosen> populateDosen() {
		TreeMap<String, Dosen> dosens = new TreeMap<String, Dosen>();

		if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			if (krsMahasiswa != null) {
				dosens.put(getId() + "-" + krsMahasiswa.getDosenPa().getId(), krsMahasiswa.getDosenPa());
			}
		} else if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			if (perkuliahan.getDosen1() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen1().getId(), perkuliahan.getDosen1());
			}
			if (perkuliahan.getDosen2() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen2().getId(), perkuliahan.getDosen2());
			}
			if (perkuliahan.getDosen3() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen3().getId(), perkuliahan.getDosen3());
			}
			if (perkuliahan.getDosen4() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen4().getId(), perkuliahan.getDosen4());
			}
			if (perkuliahan.getDosen5() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen5().getId(), perkuliahan.getDosen5());
			}
			if (perkuliahan.getDosen6() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen6().getId(), perkuliahan.getDosen6());
			}
			if (perkuliahan.getDosen7() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen7().getId(), perkuliahan.getDosen7());
			}
			if (perkuliahan.getDosen8() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen8().getId(), perkuliahan.getDosen8());
			}
			if (perkuliahan.getDosen9() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen9().getId(), perkuliahan.getDosen9());
			}
			if (perkuliahan.getDosen10() != null) {
				dosens.put(getId() + "-" + perkuliahan.getDosen10().getId(), perkuliahan.getDosen10());
			}
		}

		else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			if (kelompokKkn.getDosen_pembimbing1() != null) {
				dosens.put("Pembimbing I", kelompokKkn.getDosen_pembimbing1());
			}
			if (kelompokKkn.getDosen_pembimbing2() != null) {
				dosens.put("Pembimbing II", kelompokKkn.getDosen_pembimbing2());
			}
			if (kelompokKkn.getDosen_pembimbing3() != null) {
				dosens.put("Pembimbing III", kelompokKkn.getDosen_pembimbing3());
			}
			if (kelompokKkn.getDosen_pembimbing4() != null) {
				dosens.put("Pembimbing IV", kelompokKkn.getDosen_pembimbing4());
			}
			if (kelompokKkn.getDosen_pembimbing5() != null) {
				dosens.put("Pembimbing V", kelompokKkn.getDosen_pembimbing5());
			}
		}

		else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			if (kelompokPkl.getDosen_pembimbing1() != null) {
				dosens.put("Pembimbing I", kelompokPkl.getDosen_pembimbing1());
			}
			if (kelompokPkl.getDosen_pembimbing2() != null) {
				dosens.put("Pembimbing II", kelompokPkl.getDosen_pembimbing2());
			}
			if (kelompokPkl.getDosen_pembimbing3() != null) {
				dosens.put("Pembimbing III", kelompokPkl.getDosen_pembimbing3());
			}
			if (kelompokPkl.getDosen_pembimbing4() != null) {
				dosens.put("Pembimbing IV", kelompokPkl.getDosen_pembimbing4());
			}
			if (kelompokPkl.getDosen_pembimbing5() != null) {
				dosens.put("Pembimbing V", kelompokPkl.getDosen_pembimbing5());
			}
		}

		else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			FormatNilaiSkripsi formatNilaiSkripsi = skripsi.getFormatNilaiSkripsi();

			if (skripsi.getPembimbing() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen1()
						: getId() + "-" + skripsi.getPembimbing().getId(), skripsi.getPembimbing());
			}
			if (skripsi.getKetuaSidang() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen2()
						: getId() + "-" + skripsi.getKetuaSidang().getId(), skripsi.getKetuaSidang());
			}
			if (skripsi.getPembimbing3() != null) {
				dosens.put("Pembimbing III", skripsi.getPembimbing3());
			}

			if (skripsi.getPenguji1() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen3()
						: getId() + "-" + skripsi.getPenguji1().getId(), skripsi.getPenguji1());
			}
			if (skripsi.getPenguji2() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen4()
						: getId() + "-" + skripsi.getPenguji2().getId(), skripsi.getPenguji2());
			}
			if (skripsi.getPenguji3() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen5()
						: getId() + "-" + skripsi.getPenguji3().getId(), skripsi.getPenguji3());
			}
			if (skripsi.getPenguji4() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen6()
						: getId() + "-" + skripsi.getPenguji4().getId(), skripsi.getPenguji4());
			}
			if (skripsi.getPenguji5() != null) {
				dosens.put(formatNilaiSkripsi != null ? formatNilaiSkripsi.getDosen7()
						: getId() + "-" + skripsi.getPenguji5().getId(), skripsi.getPenguji5());
			}
		}

		else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			FormatNilaiProposalSkripsi formatNilaiProposalSkripsi = mahasiswaRequestTugasAkhir
					.getFormatNilaiProposalSkripsi();
			if (mahasiswaRequestTugasAkhir.getDosen1() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen1()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen1().getId(),
						mahasiswaRequestTugasAkhir.getDosen1());
			}
			if (mahasiswaRequestTugasAkhir.getDosen2() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen2()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen2().getId(),
						mahasiswaRequestTugasAkhir.getDosen2());
			}
			if (mahasiswaRequestTugasAkhir.getDosen3() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen3()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen3().getId(),
						mahasiswaRequestTugasAkhir.getDosen3());
			}
			if (mahasiswaRequestTugasAkhir.getDosen4() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen4()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen4().getId(),
						mahasiswaRequestTugasAkhir.getDosen4());
			}
			if (mahasiswaRequestTugasAkhir.getDosen5() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen5()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen5().getId(),
						mahasiswaRequestTugasAkhir.getDosen5());
			}
			if (mahasiswaRequestTugasAkhir.getDosen6() != null) {
				dosens.put(
						formatNilaiProposalSkripsi != null ? formatNilaiProposalSkripsi.getDosen6()
								: getId() + "-" + mahasiswaRequestTugasAkhir.getDosen6().getId(),
						mahasiswaRequestTugasAkhir.getDosen6());
			}
		} else if (this instanceof GrupPertemuan) {
			GrupPertemuan grupPertemuan = (GrupPertemuan) this;
			if (grupPertemuan != null && grupPertemuan.getDosen() != null) {
				dosens.put(getId() + "-" + grupPertemuan.getDosen().getId(), grupPertemuan.getDosen());
			}
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null
					&& pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen() != null) {
				dosens.put(getId() + "-" + pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen().getId(),
						pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen());
			}
		}

		return dosens;
	}

	@SuppressWarnings("unchecked")
	public List<Long> ambilSiswaById() {
		List<Long> siswas = new ArrayList<Long>();
		if (this instanceof JadwalPelajaran) {
			Session session = HibernateUtil.currentNativeSession();
			siswas = session.createCriteria(KelasSiswaPunyaSiswa.class).createAlias("siswa", "siswa")
					.setProjection(Projections.property("siswa.id"))
					.add(Restrictions.eq("kelasSiswa", ((JadwalPelajaran) this).getKelas()))
					.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama")).list();
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		return siswas;
	}

	@SuppressWarnings("unchecked")
	public List<Siswa> ambilSiswa() {
		List<Siswa> siswas = new ArrayList<Siswa>();
		if (this instanceof JadwalPelajaran) {
			Session session = HibernateUtil.currentNativeSession();
			siswas = ais.common.ConstantValues.simpleList(session.createCriteria(KelasSiswaPunyaSiswa.class)
					.createAlias("siswa", "siswa").setProjection(Projections.property("siswa.id"))
					.add(Restrictions.eq("kelasSiswa", ((JadwalPelajaran) this).getKelas()))
					.addOrder(Order.asc("nomorUrut")).addOrder(Order.asc("siswa.nama")), Siswa.class, false);
			// session.disconnect();
			if (session.isOpen()) {
				session.disconnect();
				session.close();
			}
			HibernateUtil.closeSession();
		}
		return siswas;
	}

	public List<Long> ambilMahasiswaById() {
		return ambilMahasiswaById(false);
	}

	public List<Long> ambilMahasiswaById(boolean refresh) {
		List<Long> mhs = new ArrayList<Long>();

		if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			if (krsMahasiswa != null) {
				mhs.add(krsMahasiswa.getMahasiswa().getId());
			}
		} else if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			mhs.addAll(perkuliahan.ambilMahasiswaId(refresh));
		}

		else if (this instanceof KelompokKkn) {

			KelompokKkn kelompokKkn = (KelompokKkn) this;
			Collection<MahasiswaDapatKelompokKkn> mahasiswaDapatKelompokKkns = kelompokKkn
					.ambilMahasiswaDapatKelompokKkn(refresh);
			for (MahasiswaDapatKelompokKkn mahasiswaDapatKelompokKkn : mahasiswaDapatKelompokKkns) {
				mhs.add(mahasiswaDapatKelompokKkn.getMahasiswa().getId());
			}
		}

		else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			Collection<MahasiswaDapatKelompokPkl> mahasiswaDapatKelompokPkls = kelompokPkl
					.ambilMahasiswaDapatKelompokPkl(refresh);
			for (MahasiswaDapatKelompokPkl mahasiswaDapatKelompokPkl : mahasiswaDapatKelompokPkls) {
				mhs.add(mahasiswaDapatKelompokPkl.getMahasiswa().getId());
			}
		}

		else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			if (skripsi != null) {
				mhs.add(skripsi.getMahasiswa().getId());
			}
		}

		else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			if (mahasiswaRequestTugasAkhir != null) {
				mhs.add(mahasiswaRequestTugasAkhir.getMahasiswa().getId());
			}
		} else if (this instanceof GrupPertemuan) {
			@SuppressWarnings("unused")
			GrupPertemuan grupPertemuan = (GrupPertemuan) this;

		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			@SuppressWarnings("unused")
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;

		}

		return mhs;
	}

	public String ambilNamaDosens() {
		List<Dosen> dosens = populateDosenBuNama();
		String d = "";
		for (Dosen dosen : dosens) {
			d += d.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
		}
		dosens = null;
		return d;
	}

	public List<Guru> populateGuruBuNama() {
		List<Guru> gurus = new ArrayList<Guru>();
		if (this instanceof FormulirKegiatan) {
			FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
			if (formulirKegiatan.getGuruPembina() != null) {
				gurus.add(formulirKegiatan.getGuruPembina());
			}

			if (formulirKegiatan.getGuruPembina2() != null) {
				gurus.add(formulirKegiatan.getGuruPembina2());
			}

			if (formulirKegiatan.getGuruPembina3() != null) {
				gurus.add(formulirKegiatan.getGuruPembina3());
			}
		} else if (this instanceof JadwalPelajaran) {

			JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) this;
			if (jadwalPelajaran.getGuru() != null) {
				gurus.add(jadwalPelajaran.getGuru());
			}
			if (jadwalPelajaran.getGuru2() != null && !gurus.contains(jadwalPelajaran.getGuru2())) {
				gurus.add(jadwalPelajaran.getGuru2());
			}
			if (jadwalPelajaran.getGuru3() != null && !gurus.contains(jadwalPelajaran.getGuru3())) {
				gurus.add(jadwalPelajaran.getGuru3());
			}
			if (jadwalPelajaran.getGuru4() != null && !gurus.contains(jadwalPelajaran.getGuru4())) {
				gurus.add(jadwalPelajaran.getGuru4());
			}
			if (jadwalPelajaran.getGuru5() != null && !gurus.contains(jadwalPelajaran.getGuru5())) {
				gurus.add(jadwalPelajaran.getGuru5());
			}

			if (jadwalPelajaran.getGuru6() != null && !gurus.contains(jadwalPelajaran.getGuru6())) {
				gurus.add(jadwalPelajaran.getGuru6());
			}
			if (jadwalPelajaran.getGuru7() != null && !gurus.contains(jadwalPelajaran.getGuru7())) {
				gurus.add(jadwalPelajaran.getGuru7());
			}
			if (jadwalPelajaran.getGuru8() != null && !gurus.contains(jadwalPelajaran.getGuru8())) {
				gurus.add(jadwalPelajaran.getGuru8());
			}
			if (jadwalPelajaran.getGuru9() != null && !gurus.contains(jadwalPelajaran.getGuru9())) {
				gurus.add(jadwalPelajaran.getGuru9());
			}
			if (jadwalPelajaran.getGuru10() != null && !gurus.contains(jadwalPelajaran.getGuru10())) {
				gurus.add(jadwalPelajaran.getGuru10());
			}
			if (jadwalPelajaran.getGuru11() != null && !gurus.contains(jadwalPelajaran.getGuru11())) {
				gurus.add(jadwalPelajaran.getGuru11());
			}
			if (jadwalPelajaran.getGuru12() != null && !gurus.contains(jadwalPelajaran.getGuru12())) {
				gurus.add(jadwalPelajaran.getGuru12());
			}
		}

		return gurus;
	}

	public String ambilNamaGurus() {
		List<Guru> gurus = populateGuruBuNama();
		String d = "";
		for (Guru guru : gurus) {
			d += d.isEmpty() ? guru.getNama() : ", " + guru.getNama();
		}
		gurus = null;
		return d;
	}

	// private List<Dosen> daftarDosen = null;

	public boolean ada(Dosen dosenSelected) {
		if (dosenSelected == null || dosenSelected.getId() == null) {
			return false;
		}
		List<Dosen> dosens = populateDosenBuNama();
		boolean ada = false;
		for (Dosen dosen : dosens) {
			if (dosen != null && dosen.getId() != null && dosen.getId().equals(dosenSelected.getId())) {
				ada = true;
				break;
			}
		}
		dosens = null;
		return ada;
	}

	public List<Long> populateDosenBuId() {
		List<Dosen> dosens = populateDosenBuNama();
		List<Long> ids = new ArrayList<Long>();
		for (Dosen dosen : dosens) {
			ids.add(dosen.getId());
		}
		dosens.clear();
		dosens = null;
		return ids;
	}

	public List<Long> populateGuruBuId() {
		List<Guru> gurus = populateGuruBuNama();
		List<Long> ids = new ArrayList<Long>();
		for (Guru guru : gurus) {
			ids.add(guru.getId());
		}
		gurus.clear();
		gurus = null;
		return ids;
	}

	public List<Dosen> populateDosenBuNama() {
		List<Dosen> dosens = new ArrayList<Dosen>();

		try {
			if (this instanceof FormulirKegiatan) {
				FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
				if (formulirKegiatan.getDosenPembina() != null) {
					dosens.add(formulirKegiatan.getDosenPembina());
				}
				if (formulirKegiatan.getDosenPembina2() != null) {
					dosens.add(formulirKegiatan.getDosenPembina2());
				}
				if (formulirKegiatan.getDosenPembina3() != null) {
					dosens.add(formulirKegiatan.getDosenPembina3());
				}
			} else if (this instanceof KrsMahasiswa) {
				KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
				if (krsMahasiswa != null && krsMahasiswa.getDosenPa() != null) {
					dosens.add(krsMahasiswa.getDosenPa());
				}
			} else if (this instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) this;
				if (perkuliahan.getDosen1() != null) {
					dosens.add(perkuliahan.getDosen1());
				}
				if (perkuliahan.getDosen2() != null) {
					dosens.add(perkuliahan.getDosen2());
				}
				if (perkuliahan.getDosen3() != null) {
					dosens.add(perkuliahan.getDosen3());
				}
				if (perkuliahan.getDosen4() != null) {
					dosens.add(perkuliahan.getDosen4());
				}
				if (perkuliahan.getDosen5() != null) {
					dosens.add(perkuliahan.getDosen5());
				}
				if (perkuliahan.getDosen6() != null) {
					dosens.add(perkuliahan.getDosen6());
				}
				if (perkuliahan.getDosen7() != null) {
					dosens.add(perkuliahan.getDosen7());
				}
				if (perkuliahan.getDosen8() != null) {
					dosens.add(perkuliahan.getDosen8());
				}
				if (perkuliahan.getDosen9() != null) {
					dosens.add(perkuliahan.getDosen9());
				}
				if (perkuliahan.getDosen10() != null) {
					dosens.add(perkuliahan.getDosen10());
				}
			}

			else if (this instanceof KelompokKkn) {
				KelompokKkn kelompokKkn = (KelompokKkn) this;
				if (kelompokKkn.getDosen_pembimbing1() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing1());
				}
				if (kelompokKkn.getDosen_pembimbing2() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing2());
				}
				if (kelompokKkn.getDosen_pembimbing3() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing3());
				}
				if (kelompokKkn.getDosen_pembimbing4() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing4());
				}
				if (kelompokKkn.getDosen_pembimbing5() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing5());
				}
				if (kelompokKkn.getDosen_pembimbing6() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing6());
				}
				if (kelompokKkn.getDosen_pembimbing7() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing7());
				}
				if (kelompokKkn.getDosen_pembimbing8() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing8());
				}
				if (kelompokKkn.getDosen_pembimbing9() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing9());
				}
				if (kelompokKkn.getDosen_pembimbing10() != null) {
					dosens.add(kelompokKkn.getDosen_pembimbing10());
				}
			}

			else if (this instanceof KelompokPkl) {
				KelompokPkl kelompokPkl = (KelompokPkl) this;
				if (kelompokPkl.getDosen_pembimbing1() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing1());
				}
				if (kelompokPkl.getDosen_pembimbing2() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing2());
				}
				if (kelompokPkl.getDosen_pembimbing3() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing3());
				}
				if (kelompokPkl.getDosen_pembimbing4() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing4());
				}
				if (kelompokPkl.getDosen_pembimbing5() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing5());
				}
				if (kelompokPkl.getDosen_pembimbing5() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing5());
				}
				if (kelompokPkl.getDosen_pembimbing6() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing6());
				}
				if (kelompokPkl.getDosen_pembimbing7() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing7());
				}
				if (kelompokPkl.getDosen_pembimbing8() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing8());
				}
				if (kelompokPkl.getDosen_pembimbing9() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing9());
				}
				if (kelompokPkl.getDosen_pembimbing10() != null) {
					dosens.add(kelompokPkl.getDosen_pembimbing10());
				}
			}

			else if (this instanceof Skripsi) {
				Skripsi skripsi = (Skripsi) this;
				if (skripsi.getKetuaSidang() != null) {
					dosens.add(skripsi.getKetuaSidang());
				}
				if (skripsi.getPembimbing() != null) {
					dosens.add(skripsi.getPembimbing());
				}

				if (skripsi.getPembimbing3() != null) {
					dosens.add(skripsi.getPembimbing3());
				}

				if (skripsi.getPenguji1() != null) {
					dosens.add(skripsi.getPenguji1());
				}
				if (skripsi.getPenguji2() != null) {
					dosens.add(skripsi.getPenguji2());
				}
				if (skripsi.getPenguji3() != null) {
					dosens.add(skripsi.getPenguji3());
				}
				if (skripsi.getPenguji4() != null) {
					dosens.add(skripsi.getPenguji4());
				}
				if (skripsi.getPenguji5() != null) {
					dosens.add(skripsi.getPenguji5());
				}
			}

			else if (this instanceof MahasiswaRequestTugasAkhir) {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
				if (mahasiswaRequestTugasAkhir.getDosen1() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen1());
				}
				if (mahasiswaRequestTugasAkhir.getDosen2() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen2());
				}
				if (mahasiswaRequestTugasAkhir.getDosen3() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen3());
				}
				if (mahasiswaRequestTugasAkhir.getDosen4() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen4());
				}
				if (mahasiswaRequestTugasAkhir.getDosen5() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen5());
				}
				if (mahasiswaRequestTugasAkhir.getDosen6() != null) {
					dosens.add(mahasiswaRequestTugasAkhir.getDosen6());
				}

			} else if (this instanceof GrupPertemuan) {
				GrupPertemuan grupPertemuan = (GrupPertemuan) this;
				if (grupPertemuan != null && grupPertemuan.getDosen() != null) {
					dosens.add(grupPertemuan.getDosen());
				}
			} else if (this instanceof PertemuanPunyaGrupPertemuan) {
				PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
				if (pertemuanPunyaGrupPertemuan != null && pertemuanPunyaGrupPertemuan.getGrupPertemuan() != null
						&& pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen() != null) {
					dosens.add(pertemuanPunyaGrupPertemuan.getGrupPertemuan().getDosen());
				}
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:1869");
		}

		return dosens;
	}

	public Integer getJumlahDosen() {
		List<Dosen> dosens = populateDosenBuNama();
		int jumlahDosen = dosens.size();
		dosens = null;
		return jumlahDosen;
	}

	public String toIdSmt() {
		String tahunAjaran = ambilTahunAjaran();
		String jenisSemester = ambilJenisSemester();
		String tahun = tahunAjaran == null || tahunAjaran.trim().isEmpty() ? "-"
				: tahunAjaran.split("/")[0];
		String id_smt = tahun + (Boolean.TRUE.equals(ambilMerupakanSP()) ? "3"
				: (Perkuliahan.GENAP.equals(jenisSemester) ? "2" : "1"));
		return id_smt;
	}

	@Override
	public int compareTo(GeneralValueObject arg0) {
		try {
			if (arg0 != null && arg0 instanceof Perkuliahan) {
				Perkuliahan arg = (Perkuliahan) arg0;
				Perkuliahan ini = (Perkuliahan) this;

				int indexHari1 = 10;
				int indexHari2 = 10;
				int i = 10;
				for (String s : Common.haris) {
					if (arg.getHari() != null && s.equalsIgnoreCase(arg.getHari())) {
						indexHari1 = i;
					}
					if (ini.getHari() != null && s.equalsIgnoreCase(ini.getHari())) {
						indexHari2 = i;
					}
					i--;
				}

				String waktu1 = (100.0 - arg.getWaktuMulaiD()) + "_" + (100.0 - arg.getWaktuSelesaiD());
				String waktu2 = (100.0 - ini.getWaktuMulaiD()) + "_" + (100.0 - ini.getWaktuSelesaiD());

				String w1 = arg.toIdSmt() + (arg.getMerupakanPraPerkuliahan() ? "_0_pra" : "") + "_" + indexHari1 + "_"
						+ waktu1;
				String w2 = ini.toIdSmt() + (ini.getMerupakanPraPerkuliahan() ? "_0_pra" : "") + "_" + indexHari2 + "_"
						+ waktu2;

				return w2.compareTo(w1);

			} else if (arg0 instanceof VOPembelajaran) {
				VOPembelajaran voPembelajaran = (VOPembelajaran) arg0;
				return (toIdSmt() + ambilJenis()).compareTo(voPembelajaran.toIdSmt() + voPembelajaran.ambilJenis());
			} else {
				super.compareTo(arg0);
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:1924");
		}

		return 0;
	}

	public abstract Integer ambilJumlahDetailperkuliahanLangsung();

	public String infoSimple() {
		Dosen d = null;
		return this.infoSimple(d);
	}

	public String infoSimple(Dosen dosenTambahan) {
		try {

			if (this instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) this;
				String matkul1 = perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama();

				String semester1 = perkuliahan.getSemester() == null ? "" : perkuliahan.getSemester().toString();

				if (perkuliahan.getStatusSemesterPendek() != null
						&& perkuliahan.getStatusSemesterPendek().equals(Perkuliahan.SEMESTER_PENDEK)) {
					semester1 = semester1 + " (" + Common.getBahasaConfig(Perkuliahan.SP) + ")";
				}

				Integer sks = perkuliahan.getMatakuliah() == null ? 0 : perkuliahan.getMatakuliah().getSks();

				String kelas1 = perkuliahan.getKelas();

				String dosen1 = "";
				for (Dosen dosen : populateDosen().values()) {
					dosen1 += dosen1.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
				}

				if (dosenTambahan != null) {
					dosen1 += dosen1.isEmpty() ? dosenTambahan.getNama() : ", " + dosenTambahan.getNama();
				}

				String ruang = perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama();

				String harijam = (perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan() == null ? false
						: perkuliahan.getMerupakan_tanpa_jadwal_perkuliahan()) ? ""
								: (", " + perkuliahan.getHari() + ", " + perkuliahan.getWaktuMulai() + " s.d "
										+ perkuliahan.getWaktuSelesai());

				String groupTxt = matkul1 + " (" + sks + " SKS) " + semester1 + " " + kelas1
						+ (dosen1.equals("") ? "" : " " + dosen1) + (ruang.equals("") ? "" : " " + ruang) + harijam;
				return groupTxt;
			} else if (this instanceof KelompokKkn) {
				KelompokKkn kelompokKkn = (KelompokKkn) this;
				return kelompokKkn.getNama() + " (" + kelompokKkn.getKkn().getNama() + ")";
			} else if (this instanceof KelompokPkl) {
				KelompokPkl kelompokPkl = (KelompokPkl) this;
				return kelompokPkl.getNama() + " (" + kelompokPkl.getPkl().getNama() + ")";
			} else if (this instanceof KrsMahasiswa) {
				KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
				Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
				if (mahasiswa != null) {
					return mahasiswa.getNama() + " TA " + krsMahasiswa.getTahunAkademik()
							+ (krsMahasiswa.getSemesterPendek() == null ? "" : " (SP)") + ", " +

							(mahasiswa.getStatusKeluar() == null
									? KrsDetailHelper.rubahKeteranganPengambilanKRSBersih(mahasiswa, krsMahasiswa.getSemester(),
											krsMahasiswa.getTahapan(), krsMahasiswa.getSemesterPendek(), krsMahasiswa,
											false)
									: (mahasiswa.getStatusKeluar().getNama()
											+ (mahasiswa.getPredikatKelulusan() == null ? ""
													: " / " + mahasiswa.getPredikatKelulusan().getNama())

											+ (mahasiswa.getStatusSetelahLulus() == null ? ""
													: " / " + mahasiswa.getStatusSetelahLulus().getNama())

											+ (mahasiswa.getStatusPekerjaanSetelahLulus() == null ? ""
													: " / " + mahasiswa.getStatusPekerjaanSetelahLulus().getNama())

											+ (mahasiswa.getStatusDomisiliSetelahLulus() == null ? ""
													: " / " + mahasiswa.getStatusDomisiliSetelahLulus().getNama())));
				}
			} else if (this instanceof Skripsi) {
				Skripsi skripsi = (Skripsi) this;
				return skripsi.getJudul()
						+ (skripsi.getFormatNilaiSkripsi() == null ? ""
								: " (" + skripsi.getFormatNilaiSkripsi().getNama() + ")")
						+ "-" + skripsi.getMahasiswa().getNim() + "-" + skripsi.getMahasiswa().getNama();
			} else if (this instanceof MahasiswaRequestTugasAkhir) {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
				return (mahasiswaRequestTugasAkhir.getJudul().isEmpty() ? mahasiswaRequestTugasAkhir.getJudul1()
						: mahasiswaRequestTugasAkhir.getJudul())
						+ (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null ? ""
								: " (" + mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getNama() + ")")
						+ "-" + mahasiswaRequestTugasAkhir.getMahasiswa().getNim() + "-"
						+ mahasiswaRequestTugasAkhir.getMahasiswa().getNama();
			} else if (this instanceof PertemuanPunyaGrupPertemuan) {
				PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
				return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getNama();
			} else if (this instanceof JadwalUjianPMB) {
				JadwalUjianPMB jadwalUjianPMB = (JadwalUjianPMB) this;
				return jadwalUjianPMB.getNama();
			} else if (this instanceof JadwalUjianPSB) {
				JadwalUjianPSB jadwalUjianPSB = (JadwalUjianPSB) this;
				return jadwalUjianPSB.getNama();
			} else if (this instanceof FormulirKegiatan) {
				FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
				return formulirKegiatan.getNama();
			} else if (this instanceof Wisuda) {
				Wisuda wisuda = (Wisuda) this;
				return (wisuda.getMoto().isEmpty() ? "" : wisuda.getMoto() + " | ") + "Wisuda ke-"
						+ wisuda.getWisudaKe();
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/database/model/VOPembelajaran.java:2036");
		}
		return "-";
	}

	public String infoSangatSimple() {
		Dosen d = null;
		return this.infoSangatSimple(d);
	}

	public String infoSangatSimple(Dosen dosenTambahan) {

		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			String matkul1 = perkuliahan.getMatakuliah() == null ? "" : perkuliahan.getMatakuliah().getNama();

			String dosen1 = "";
			for (Dosen dosen : populateDosen().values()) {
				dosen1 += dosen1.isEmpty() ? dosen.getNama() : ", " + dosen.getNama();
			}

			if (dosenTambahan != null) {
				dosen1 += dosen1.isEmpty() ? dosenTambahan.getNama() : ", " + dosenTambahan.getNama();
			}

			String groupTxt = matkul1 + (dosen1.equals("") ? "" : " " + dosen1);
			return groupTxt;
		} else if (this instanceof JadwalPelajaran) {
			JadwalPelajaran jadwalPelajaran = (JadwalPelajaran) this;
			String matkul1 = jadwalPelajaran.getMatapelajaran() == null ? ""
					: jadwalPelajaran.getMatapelajaran().getNama();

			String dosen1 = "";
			if (jadwalPelajaran.getGuru() != null) {
				dosen1 = jadwalPelajaran.getGuru().getNama();
			}

			String groupTxt = matkul1 + (dosen1.equals("") ? "" : " " + dosen1);
			return groupTxt;
		} else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			return kelompokKkn.getNama();
		} else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			return kelompokPkl.getNama();
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			Mahasiswa mahasiswa = krsMahasiswa.getMahasiswa();
			if (mahasiswa != null) {
				return "KRS " + mahasiswa.getNama();
			}
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			return (skripsi.getFormatNilaiSkripsi() == null ? "" : skripsi.getFormatNilaiSkripsi().getNama());
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			return (mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi() == null ? ""
					: mahasiswaRequestTugasAkhir.getFormatNilaiProposalSkripsi().getNama());
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getNama();
		} else if (this instanceof JadwalUjianPMB) {
			JadwalUjianPMB jadwalUjianPMB = (JadwalUjianPMB) this;
			return jadwalUjianPMB.getNama();
		} else if (this instanceof JadwalUjianPSB) {
			JadwalUjianPSB jadwalUjianPSB = (JadwalUjianPSB) this;
			return jadwalUjianPSB.getNama();
		} else if (this instanceof FormulirKegiatan) {
			FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
			return formulirKegiatan.getNama();
		}

		return "-";
	}

	public String ambilTahunAjaran() {
		try {
			if (this instanceof Perkuliahan) {
				Perkuliahan perkuliahan = (Perkuliahan) this;
				return perkuliahan.getTahunAjaran();
			} else if (this instanceof KelompokKkn) {
				KelompokKkn kelompokKkn = (KelompokKkn) this;
				return kelompokKkn.getKkn() == null ? "-" : kelompokKkn.getKkn().getTahunAkademik();
			} else if (this instanceof KelompokPkl) {
				KelompokPkl kelompokPkl = (KelompokPkl) this;
				return kelompokPkl.getPkl() == null ? "-" : kelompokPkl.getPkl().getTahunAkademik();
			} else if (this instanceof KrsMahasiswa) {
				KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
				return krsMahasiswa.getTahunAkademik();
			} else if (this instanceof FormulirKegiatan) {
				FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
				return formulirKegiatan.getTahunAkademik();
			} else if (this instanceof Skripsi) {
				Skripsi skripsi = (Skripsi) this;
				return skripsi.getTahunAkademik();
			} else if (this instanceof MahasiswaRequestTugasAkhir) {
				MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
				return mahasiswaRequestTugasAkhir.getTahunAkademik();
			} else if (this instanceof PertemuanPunyaGrupPertemuan) {
				PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
				return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getTahunAkademik();
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/database/model/VOPembelajaran.java:2138");
			// TODO: handle exception
		}
		return "-";
	}

	public Boolean ambilMerupakanSP() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getStatusSemesterPendek() != null;
		} else if (this instanceof KelompokKkn) {
			return false;
		} else if (this instanceof KelompokPkl) {
			return false;
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			return krsMahasiswa.getSemesterPendek() != null;
		} else if (this instanceof Skripsi) {
			return false;
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			return false;
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getSemesterPendek() != null;
		}
		return false;
	}

	public String ambilJenis() {
		if (this instanceof Perkuliahan) {
			return "Perkuliahan";
		} else if (this instanceof KelompokKkn) {
			return "KKN";
		} else if (this instanceof KelompokPkl) {
			return "PKL";
		} else if (this instanceof KrsMahasiswa) {
			return "Bimbingan Akademik";
		} else if (this instanceof Skripsi) {
			return "Sidang Skripsi / TA";
		} else if (this instanceof FormulirKegiatan) {
			return "Kegiatan";
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			return "Bimbingan Skripsi / TA";
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenis();
		}
		return "-";
	}

	public String ambilTahunAkademik() {
		return ambilTahunAjaran();
	}

	public Integer ambilSemester() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getSemester();
		} else if (this instanceof KelompokKkn) {
			return 1;
		} else if (this instanceof KelompokPkl) {
			return 1;
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			return krsMahasiswa.getSemester();
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			return skripsi.getSemester();
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			return mahasiswaRequestTugasAkhir.getSemester();
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			return 1;
		}
		return -1;
	}

	public String ambilJenisSemester() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getGanjilGenap();
		} else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			return kelompokKkn.getKkn() == null ? null : kelompokKkn.getKkn().getSemester();
		} else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			return kelompokPkl.getPkl() == null ? null : kelompokPkl.getPkl().getSemester();
		} else if (this instanceof FormulirKegiatan) {
			FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
			return formulirKegiatan.getSemester();
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			return krsMahasiswa.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			return skripsi.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			return mahasiswaRequestTugasAkhir.getSemester() % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL;
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			return pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenisSemester();
		}
		return "-";
	}

	public String ambilKeyword() {

		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			String keyword = "";

			List<Dosen> dosens = perkuliahan.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += perkuliahan.getMatakuliah().getNama() + " ";
			keyword += perkuliahan.getMatakuliah().getKode() + " ";
			return keyword.trim();
		} else if (this instanceof KelompokKkn) {
			KelompokKkn kelompokKkn = (KelompokKkn) this;
			String keyword = "";
			List<Dosen> dosens = kelompokKkn.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += kelompokKkn.getNama() + " ";
			keyword += kelompokKkn.getKkn().getNama() + " ";
			return keyword.trim();
		} else if (this instanceof KelompokPkl) {
			KelompokPkl kelompokPkl = (KelompokPkl) this;
			String keyword = "";
			List<Dosen> dosens = kelompokPkl.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += kelompokPkl.getNama() + " ";
			keyword += kelompokPkl.getPkl().getNama() + " ";
			return keyword.trim();
		} else if (this instanceof FormulirKegiatan) {
			FormulirKegiatan formulirKegiatan = (FormulirKegiatan) this;
			String keyword = "";

			keyword += formulirKegiatan.getNama() + " ";
			return keyword.trim();
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			String keyword = "";

			keyword += krsMahasiswa.getMahasiswa().getNim() + " ";
			keyword += krsMahasiswa.getMahasiswa().getNama() + " ";
			keyword += krsMahasiswa.getCatatan() + " ";
			keyword += krsMahasiswa.getCatatanKhs() + " ";

			List<Dosen> dosens = krsMahasiswa.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;
//			System.out.println("keyword => " + keyword);
			return keyword.trim();
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			String keyword = "";
			List<Dosen> dosens = skripsi.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += skripsi.getJudul() + " ";
			keyword += skripsi.getKeyword() + " ";

			keyword += skripsi.getMahasiswa().getNim() + " ";
			keyword += skripsi.getMahasiswa().getNama() + " ";

			return keyword.trim();
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			String keyword = "";
			List<Dosen> dosens = mahasiswaRequestTugasAkhir.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += mahasiswaRequestTugasAkhir.getMahasiswa().getNim() + " ";
			keyword += mahasiswaRequestTugasAkhir.getMahasiswa().getNama() + " ";

			keyword += mahasiswaRequestTugasAkhir.getJudul() + " ";
			return keyword.trim();
		} else if (this instanceof PertemuanPunyaGrupPertemuan) {
			PertemuanPunyaGrupPertemuan pertemuanPunyaGrupPertemuan = (PertemuanPunyaGrupPertemuan) this;
			String keyword = "";
			List<Dosen> dosens = pertemuanPunyaGrupPertemuan.populateDosenBuNama();
			for (Dosen dosen : dosens) {
				keyword += dosen.getNama() + " ";
			}
			dosens = null;

			keyword += pertemuanPunyaGrupPertemuan.getGrupPertemuan().getNama() + " ";
			keyword += pertemuanPunyaGrupPertemuan.getGrupPertemuan().getJenis() + " ";
			return keyword.trim();
		}
		return "";
	}

	public String ambilKelas() {

		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getKelas().trim();
		} else if (this instanceof KrsMahasiswa) {
			KrsMahasiswa krsMahasiswa = (KrsMahasiswa) this;
			return krsMahasiswa.getMahasiswa().getKelas().trim();
		} else if (this instanceof Skripsi) {
			Skripsi skripsi = (Skripsi) this;
			return skripsi.getMahasiswa().getKelas().trim();
		} else if (this instanceof MahasiswaRequestTugasAkhir) {
			MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) this;
			return mahasiswaRequestTugasAkhir.getMahasiswa().getKelas().trim();
		}
		return "";
	}

	public String ambilHari() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getHari();
		} else {
			return null;
		}
	}

	public Integer ambilExtraKulikuler() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getMatakuliah().getExtraKulikuler() ? Perkuliahan.EKSTRA : null;
		} else {
			return null;
		}
	}

	public Boolean ambilMerupakanPraPerkuliahan() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getMerupakanPraPerkuliahan();
		} else {
			return false;
		}
	}

	public Boolean ambilMerupakanRemedial() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getMerupakanRemedial();
		} else {
			return false;
		}
	}

	public Boolean ambilMerupakanParalel() {
		if (this instanceof Perkuliahan) {
			Perkuliahan perkuliahan = (Perkuliahan) this;
			return perkuliahan.getMerupakan_paralel() || perkuliahan.flagParalel;
		} else {
			return false;
		}
	}

}
