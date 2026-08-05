package ais.action.master.surat.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Rows;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Mahasiswa;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Tbmuser;
import ais.database.model.file.FileFoto;
import ais.database.model.file.FotoGambarTandaTanganPejabat;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.rab.SatuanKerja;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Yayasan;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarUntuk;
import ais.database.model.surat.OpsiSuratKeluar;
import ais.database.model.surat.OpsiSuratMasuk;
import ais.database.model.surat.SuratKeluar;

@SuppressWarnings("unchecked")
public class SuratUtil {

	public static KlasifikasiSuratKeluarUntuk MAHASISWA;
	public static KlasifikasiSuratKeluarUntuk DOSEN;
	public static KlasifikasiSuratKeluarUntuk SISWA;
	public static KlasifikasiSuratKeluarUntuk GURU;
	public static KlasifikasiSuratKeluarUntuk PEGAWAI;
	public static KlasifikasiSuratKeluarUntuk UMUM;

	public static OpsiSuratMasuk simpan;
	public static OpsiSuratMasuk balas;
	public static OpsiSuratMasuk perbanyak;
	public static OpsiSuratMasuk teliti;
	public static OpsiSuratMasuk ikutiPerkembangan;
	public static OpsiSuratMasuk harapPenjelasanMasalah;
	public static OpsiSuratMasuk untukDiproses;
	public static OpsiSuratMasuk saranSaran;
	public static OpsiSuratMasuk pakaiSebagaiPedoman;

	public static OpsiSuratKeluar balasan;
	public static OpsiSuratKeluar penting;

	// FIX: HAPUS variabel `public final static Map parameters`. 
	// Kita akan menggunakan Map lokal yang dilahirkan dan dimatikan secara instan
	// di dalam setiap method untuk mencegah Memory Leak dan Data Tertukar.

	static {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.getTransaction().begin();

			balasan = initOpsiKeluar(session, "Balasan");
			penting = initOpsiKeluar(session, "Penting");
			initOpsiKeluar(session, "Lain-lain");

			initOpsiMasuk(session, "Lain-lain");
			pakaiSebagaiPedoman = initOpsiMasuk(session, "Pakai Sebagai Pedoman");
			saranSaran = initOpsiMasuk(session, "Saran Saran");
			untukDiproses = initOpsiMasuk(session, "Untuk Diproses");
			harapPenjelasanMasalah = initOpsiMasuk(session, "Harap Penjelasan Masalah");
			simpan = initOpsiMasuk(session, "Simpan");
			balas = initOpsiMasuk(session, "Balas");
			perbanyak = initOpsiMasuk(session, "Perbanyak");
			teliti = initOpsiMasuk(session, "Teliti");
			ikutiPerkembangan = initOpsiMasuk(session, "Ikuti Perkembangan");

			MAHASISWA = initKlasifikasi(session, "Mahasiswa");
			SISWA = initKlasifikasi(session, "Siswa");
			DOSEN = initKlasifikasi(session, "Dosen");
			GURU = initKlasifikasi(session, "Guru");
			PEGAWAI = initKlasifikasi(session, "Pegawai");
			UMUM = initKlasifikasi(session, "Umum");

			session.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/util/SuratUtil.java:99");
			if (session != null && session.getTransaction().isActive()) {
				try {
					session.getTransaction().rollback();
				} catch (Exception ex) {
					ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/surat/util/SuratUtil.java:104");
				}
			}
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtil.java:111");
				}
			}
		}
	}

	private static OpsiSuratKeluar initOpsiKeluar(Session session, String nama) {
		OpsiSuratKeluar o = (OpsiSuratKeluar) session.createCriteria(OpsiSuratKeluar.class)
				.add(Restrictions.eq("nama", nama)).setMaxResults(1).uniqueResult();
		if (o == null) {
			o = new OpsiSuratKeluar();
			o.setNama(nama);
			o.setKeterangan(nama);
			session.save(o);
		}
		return o;
	}

	private static OpsiSuratMasuk initOpsiMasuk(Session session, String nama) {
		OpsiSuratMasuk o = (OpsiSuratMasuk) session.createCriteria(OpsiSuratMasuk.class)
				.add(Restrictions.eq("nama", nama)).setMaxResults(1).uniqueResult();
		if (o == null) {
			o = new OpsiSuratMasuk();
			o.setNama(nama);
			o.setKeterangan(nama);
			session.save(o);
		}
		return o;
	}

	private static KlasifikasiSuratKeluarUntuk initKlasifikasi(Session session, String nama) {
		KlasifikasiSuratKeluarUntuk o = (KlasifikasiSuratKeluarUntuk) session
				.createCriteria(KlasifikasiSuratKeluarUntuk.class).add(Restrictions.eq("nama", nama)).setMaxResults(1)
				.uniqueResult();
		if (o == null) {
			o = new KlasifikasiSuratKeluarUntuk();
			o.setNama(nama);
			o.setKeterangan(nama);
			session.save(o);
		}
		return o;
	}

	// FIX: Hapus kata kunci 'synchronized' karena dengan Map lokal, method ini sudah 100% Thread-Safe
	public static Map<String, Object> ubahIsiSuratKeluar(Rows rows,
			KlasifikasiSuratKeluar myKlasifikasiSuratKeluar, Mahasiswa mahasiswa, Dosen dosen, Pegawai pegawai,
			Tbmuser tbmuser, String mycode, SuratKeluar suratKeluar, Groupbox west) {
		
		// Membuat Map yang aman secara lokal
		Map<String, Object> localParams = new HashMap<String, Object>();
		
		return SuratUtilHelper.ubahIsiSuratKeluar(rows, myKlasifikasiSuratKeluar, mahasiswa, dosen, pegawai, tbmuser,
				mycode, suratKeluar, west, localParams);
	}

	@SuppressWarnings("rawtypes")
	public static void initDefaultKop(Map parameters) {
		initDefaultKop(parameters, Common.getCurrentUser(), null);
	}

	@SuppressWarnings("rawtypes")
	public static void initDefaultKop(Map parameters, Tbmuser tbmuser) {
		initDefaultKop(parameters, tbmuser, null);
	}

	@SuppressWarnings("rawtypes")
	public static void initDefaultKop(Map parameters, SatuanKerja satuanKerja) {
		initDefaultKop(parameters, Common.getCurrentUser(), null);
	}

	@SuppressWarnings("rawtypes")
	public static void initDefaultKop(Map parameters, Tbmuser tbmuser, SatuanKerja satuanKerja) {
		for (Object o : ConstantValues.ambilBerdasarClass(Sekolah.class).values()) {
			Sekolah sekolah = (Sekolah) o;
			parameters.put("NAMA_SEKOLAH_" + sekolah.getId(), sekolah.getNama());
			parameters.put("ALAMAT_SEKOLAH_" + sekolah.getId(), sekolah.getAlamat());
			parameters.put("TELP_SEKOLAH_" + sekolah.getId(), sekolah.getTelp());
			parameters.put("FAX_SEKOLAH_" + sekolah.getId(), sekolah.getFax());
			parameters.put("EMAIL_SEKOLAH_" + sekolah.getId(), sekolah.getEmail());
			sekolah.putFile(parameters);
		}

		for (Object o : ConstantValues.ambilBerdasarClass(Yayasan.class).values()) {
			((Yayasan) o).putFile(parameters);
		}

		Sekolah sekolah = SekolahUtil.getSekolah();
		if (sekolah != null && sekolah.getId() != null) {
			Common.insertProperty(Sekolah.class, sekolah, parameters, "sekolah");
			parameters.put("NAMA_SEKOLAH", sekolah.getNama());
			parameters.put("ALAMAT_SEKOLAH", sekolah.getAlamat());
			parameters.put("TELP_SEKOLAH", sekolah.getTelp());
			parameters.put("FAX_SEKOLAH", sekolah.getFax());
			parameters.put("EMAIL_SEKOLAH", sekolah.getEmail());
			sekolah.putFile(parameters);
		}

		for (Object o : ConstantValues.ambilBerdasarClass(PerguruanTinggi.class).values()) {
			((PerguruanTinggi) o).putFile(parameters);
		}

		for (Object o : ConstantValues.ambilBerdasarClass(Fakultas.class).values()) {
			((Fakultas) o).putFile(parameters);
		}

		for (Object o : ConstantValues.ambilBerdasarClass(Jurusan.class).values()) {
			((Jurusan) o).putFile(parameters);
		}

		String kop = ambilKop(tbmuser);
		parameters.put("kop", kop);
		parameters.put("kop_surat", kop);
		parameters.put("kop_surat_local", kop);

		String kopBawah = ambilKopBawah(tbmuser);
		parameters.put("kop_bawah", kopBawah);
		parameters.put("kop_bawah_surat", kopBawah);
		parameters.put("kop_bawah_surat_local", kopBawah);

		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		if (perguruanTinggi != null && perguruanTinggi.getId() != null) {
			Common.insertProperty(PerguruanTinggi.class, perguruanTinggi, parameters, "pt");
		}

		initDefaultKopAja(satuanKerja == null ? (tbmuser == null ? null : tbmuser.getSatuanKerja()) : satuanKerja,
				perguruanTinggi, parameters, "kop_pengaju");
	}

	public static String ambilKop(Tbmuser tbmuser) {
		return resolveKopAtauBawah(tbmuser, LampiranLain.KOP_SEKOLAH, LampiranLain.KOP_PT);
	}

	public static String ambilKopBawah(Tbmuser tbmuser) {
		return resolveKopAtauBawah(tbmuser, LampiranLain.KOP_BAWAH_SEKOLAH, LampiranLain.KOP_BAWAH_PT);
	}

	private static String resolveKopAtauBawah(Tbmuser tbmuser, String tipeSekolah, String tipePT) {
		Sekolah sekolah = SekolahUtil.getSekolah();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		if (tbmuser != null) {
			if (tbmuser.ambilSekolah() != null) {
				sekolah = tbmuser.ambilSekolah();
			}
			if (tbmuser.ambilFakultas() != null && tbmuser.ambilFakultas().getPerguruanTinggi() != null) {
				perguruanTinggi = tbmuser.ambilFakultas().getPerguruanTinggi();
			}
		}

		if (sekolah != null && sekolah.getId() != null) {
			String path = getPathFileLampiran(sekolah.getId(), tipeSekolah, false);
			if (!path.isEmpty())
				return path;
		}

		if (perguruanTinggi != null) {
			if (perguruanTinggi.getPendaftar() != null) {
				String path = getPathFileLampiran(perguruanTinggi.getPendaftar().getId(), tipePT + "_Pendaftar", false);
				if (!path.isEmpty())
					return path;
			}
			String path = getPathFileLampiran(perguruanTinggi.getId(), tipePT, false);
			if (!path.isEmpty())
				return path;
		}

		return "";
	}

	public static LampiranLain ambilKopLampiranLain() {
		Sekolah sekolah = SekolahUtil.getSekolah();
		PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();

		if (sekolah != null && sekolah.getId() != null) {
			LampiranLain lampiran = LampiranLain.ambil(sekolah.getId(), LampiranLain.KOP_SEKOLAH);
			if (lampiran != null)
				return lampiran;
		}

		if (perguruanTinggi != null) {
			if (perguruanTinggi.getPendaftar() != null) {
				LampiranLain lampiran = LampiranLain.ambil(perguruanTinggi.getPendaftar().getId(),
						LampiranLain.KOP_PT + "_Pendaftar");
				if (lampiran != null)
					return lampiran;
			}
			return LampiranLain.ambil(perguruanTinggi.getId(), LampiranLain.KOP_PT);
		}

		return null;
	}

	public static void initSatker(SatuanKerja satuanKerja, Map<String, Object> parameters) {
		if (satuanKerja != null && satuanKerja.getId() != null) {
			parameters.put("kop_satker", getPathFileLampiran(satuanKerja.getId(), LampiranLain.KOP_SATKER, true));
			parameters.put("kop_bawah_satker",
					getPathFileLampiran(satuanKerja.getId(), LampiranLain.KOP_BAWAH_SATKER, true));
		} else {
			parameters.put("kop_satker", "");
			parameters.put("kop_bawah_satker", "");
		}
	}

	public static void initDefaultKopAja(SatuanKerja satuanKerja, PerguruanTinggi perguruanTinggi,
			Map<String, Object> parameters, String nama) {
		initSatker(satuanKerja, parameters);

		if (satuanKerja != null) {
			parameters.put("unit_pengaju", satuanKerja.getNama());
		}

		if (!parameters.containsKey(nama)) {
			Sekolah sekolah = null;
			if (satuanKerja != null) {
				for (Object o : ConstantValues.ambilBerdasarClass(Sekolah.class).values()) {
					Sekolah s = (Sekolah) o;
					if (s != null && s.getSatuanKerja() != null
							&& s.getSatuanKerja().getId().equals(satuanKerja.getId())) {
						sekolah = s;
						break;
					}
				}
			}

			if (sekolah != null && sekolah.getId() != null) {
				parameters.put(nama, getPathFileLampiran(sekolah.getId(), LampiranLain.KOP_SEKOLAH, false));
				parameters.put(nama + "_bawah",
						getPathFileLampiran(sekolah.getId(), LampiranLain.KOP_BAWAH_SEKOLAH, false));
			} else if (perguruanTinggi != null) {
				if (perguruanTinggi.getPendaftar() != null) {
					parameters.put(nama, getPathFileLampiran(perguruanTinggi.getPendaftar().getId(),
							LampiranLain.KOP_PT + "_Pendaftar", false));
					parameters.put(nama + "_bawah", getPathFileLampiran(perguruanTinggi.getPendaftar().getId(),
							LampiranLain.KOP_BAWAH_PT + "_Pendaftar", false));
				} else {
					parameters.put(nama, getPathFileLampiran(perguruanTinggi.getId(), LampiranLain.KOP_PT, false));
					parameters.put(nama + "_bawah",
							getPathFileLampiran(perguruanTinggi.getId(), LampiranLain.KOP_BAWAH_PT, false));
				}
			} else {
				parameters.put(nama, "");
				parameters.put(nama + "_bawah", "");
			}
		}
	}

	private static String getPathFileLampiran(Long id, String lampiranType, boolean useBooleanParam) {
		if (id == null)
			return "";

		File file = FileFoto.fileAdaDiFolder(lampiranType, id);
		if (file != null && file.exists()) {
			return file.getAbsolutePath();
		}

		LampiranLain lampiran = useBooleanParam ? LampiranLain.ambil(false, id, lampiranType)
				: LampiranLain.ambil(id, lampiranType);
		if (lampiran != null) {
			File f = lampiran.ambilFile();
			if (f != null && f.exists()) {
				return f.getAbsolutePath();
			}
		}
		return "";
	}

	public static Map<String, Object> ubahIsiSuratKeluar(SuratKeluar suratKeluar) {
		return ubahIsiSuratKeluar(suratKeluar, null);
	}

	// FIX: Hapus synchronized, implementasikan Map lokal
	public static Map<String, Object> ubahIsiSuratKeluar(SuratKeluar suratKeluar, Groupbox west) {
		return ubahIsiSuratKeluar(suratKeluar, Common.getCurrentUser(), west);
	}

	// FIX: Hapus synchronized, implementasikan Map lokal
	public static Map<String, Object> ubahIsiSuratKeluar(SuratKeluar suratKeluar, Tbmuser tbmuser,
			Groupbox west) {
		
		Map<String, Object> localParams = new HashMap<String, Object>();
		
		return SuratUtilHelper.ubahIsiSuratKeluar(suratKeluar, tbmuser, west, localParams);
	}

	public static void ttdpejabat(Pejabat pejabat, Map<String, Object> parameters, String prefix) {
		String key = pejabat.getJenisJabatan() != null ? pejabat.getJenisJabatan().getKey() : "";
		String nama = getNamaPejabat(pejabat);
		String nip = getNipPejabat(pejabat);
		String gelarDepan = getGelarDepan(pejabat);
		String gelarBelakang = getGelarBelakang(pejabat);

		parameters.put(prefix + "pejabat." + key, nama);
		parameters.put(prefix + "pejabat.nip." + key, nip);
		// Tambahan: code & mycode dari object Pegawai penanda tangan (kosong bila pejabat bukan
		// Pegawai / null). Pola sama dengan pejabat.nip.<key>.
		parameters.put(prefix + "pejabat.code." + key,
				pejabat.getPegawai() == null ? "" : pejabat.getPegawai().getCode());
		parameters.put(prefix + "pejabat.mycode." + key,
				pejabat.getPegawai() == null ? "" : pejabat.getPegawai().getMycode());
		parameters.put(prefix + "pejabat.gelarDepan." + key, gelarDepan);
		parameters.put(prefix + "pejabat.gelarBelakang." + key, gelarBelakang);
		parameters.put(prefix + "pejabat.nama", nama);

		if (prefix != null && !prefix.isEmpty()) {
			parameters.put(prefix + "pejabat", nama);
			parameters.put(prefix + "pejabat.nip", nip);
			parameters.put(prefix + "pejabat.code",
					pejabat.getPegawai() == null ? "" : pejabat.getPegawai().getCode());
			parameters.put(prefix + "pejabat.mycode",
					pejabat.getPegawai() == null ? "" : pejabat.getPegawai().getMycode());
		}

		putTtdPath(parameters, prefix + "pejabat.ttd.peg", key,
				pejabat.getPegawai() != null ? pejabat.getPegawai().getId() : null, LampiranLain.TTD_PEGAWAI, prefix);
		putTtdPath(parameters, prefix + "pejabat.ttd.dsn", key,
				pejabat.getDosen() != null ? pejabat.getDosen().getId() : null, LampiranLain.TTD_DOSEN, prefix);
		putTtdPath(parameters, prefix + "pejabat.ttd.guru", key,
				pejabat.getGuru() != null ? pejabat.getGuru().getId() : null, LampiranLain.TTD_GURU, prefix);

		ttdpejabat(parameters, pejabat.getJenisPengguna(), pejabat.getUsernamePengguna(), prefix);
	}

	private static void putTtdPath(Map<String, Object> parameters, String baseKey, String key, Long id,
			String typeLampiran, String prefix) {
		String path = "";
		if (id != null) {
			try {
				LampiranLain lampiran = LampiranLain.ambil(id, typeLampiran);
				if (lampiran != null && lampiran.ambilFile() != null) {
					path = lampiran.ambilFile().getAbsolutePath();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/util/SuratUtil.java:443");
			}
		}
		parameters.put(baseKey + "." + key, path);
		if (prefix != null && !prefix.isEmpty()) {
			parameters.put(baseKey, path);
		}
	}

	private static String getNamaPejabat(Pejabat p) {
		if (p.getPegawai() != null)
			return p.getPegawai().getNama();
		if (p.getDosen() != null)
			return p.getDosen().getNama();
		return "";
	}

	private static String getNipPejabat(Pejabat p) {
		if (p.getPegawai() != null)
			return p.getPegawai().getCode() == null ? "" : p.getPegawai().getCode();
		if (p.getDosen() != null)
			return p.getDosen().getCode() == null ? "" : p.getDosen().getCode();
		return "";
	}

	private static String getGelarDepan(Pejabat p) {
		if (p.getPegawai() != null)
			return p.getPegawai().getGelarDepan() == null ? "" : p.getPegawai().getGelarDepan();
		if (p.getDosen() != null)
			return p.getDosen().getGelarDepan() == null ? "" : p.getDosen().getGelarDepan();
		return "";
	}

	private static String getGelarBelakang(Pejabat p) {
		if (p.getPegawai() != null)
			return p.getPegawai().getGelarBelakang() == null ? "" : p.getPegawai().getGelarBelakang();
		if (p.getDosen() != null)
			return p.getDosen().getGelarBelakang() == null ? "" : p.getDosen().getGelarBelakang();
		return "";
	}

	public static void ttdpejabat(Map<String, Object> parameters, String jenisPengguna, String khususUser,
			String prefix) {
		Map<String, Tbmuser> datasAktor = new HashMap<String, Tbmuser>();

		if (jenisPengguna != null && !jenisPengguna.trim().isEmpty()) {
			String[] rolesDicari = jenisPengguna.toLowerCase().split(",");
			try {
				for (Object o : ConstantValues.ambilBerdasarClass(Tbmuser.class).values()) {
					Tbmuser tbmuser = (Tbmuser) o;
					if (tbmuser != null && tbmuser.ambilRolesIdLower() != null) {
						Set<String> userRoles = tbmuser.ambilRolesIdLower();
						for (String username : rolesDicari) {
							if (userRoles.contains(username.trim())) {
								masukanAktorKeMap(datasAktor, tbmuser, username.trim());
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/surat/util/SuratUtil.java:503");
			}
		}

		if (khususUser != null && !khususUser.trim().isEmpty()) {
			for (String username : khususUser.split(",")) {
				Tbmuser tbmuser = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), username.trim());
				if (tbmuser != null) {
					masukanAktorKeMap(datasAktor, tbmuser, username.trim());
				}
			}
		}

		int index = 0;
		for (Tbmuser pejabat : datasAktor.values()) {
			index++;
			ttdpejabat(parameters, pejabat, prefix, index);
		}
	}

	private static void masukanAktorKeMap(Map<String, Tbmuser> datasAktor, Tbmuser tbmuser, String fallbackKey) {
		if (tbmuser.ambilPegawai() != null)
			datasAktor.put(tbmuser.getPegawai().getId().toString(), tbmuser);
		else if (tbmuser.getMahasiswa() != null)
			datasAktor.put(tbmuser.getMahasiswa().getId().toString(), tbmuser);
		else if (tbmuser.getSiswa() != null)
			datasAktor.put(tbmuser.getSiswa().getId().toString(), tbmuser);
		else if (tbmuser.ambilDosen() != null)
			datasAktor.put(tbmuser.getDosen().getId().toString(), tbmuser);
		else
			datasAktor.put(fallbackKey, tbmuser);
	}

	public static void ttdpejabat(Map<String, Object> parameters, Tbmuser pejabat, String prefix, int index) {
		String suffix = (index == 1) ? "" : "." + index;
		String nama = (pejabat.getPegawai() == null) ? (pejabat.getDosen() == null ? "" : pejabat.getDosen().getNama())
				: pejabat.getPegawai().getNama();
		String nip = (pejabat.getPegawai() == null) ? (pejabat.getDosen() == null ? "" : pejabat.getDosen().getCode())
				: (pejabat.getPegawai().getCode() == null ? "" : pejabat.getPegawai().getCode());

		parameters.put(prefix + "pejabat." + pejabat.getUserId() + suffix, nama);
		parameters.put(prefix + "pejabat.nip." + pejabat.getUserId() + suffix, nip);

		if (prefix != null && !prefix.isEmpty()) {
			parameters.put(prefix + "pejabat", nama);
			parameters.put(prefix + "pejabat.nip", nip);
		}

		putTtdPath(parameters, prefix + "pejabat.ttd.peg", pejabat.getUserId() + suffix,
				pejabat.getPegawai() != null ? pejabat.getPegawai().getId() : null, LampiranLain.TTD_PEGAWAI, prefix);
		putTtdPath(parameters, prefix + "pejabat.ttd.dsn", pejabat.getUserId() + suffix,
				pejabat.getDosen() != null ? pejabat.getDosen().getId() : null, LampiranLain.TTD_DOSEN, prefix);
		putTtdPath(parameters, prefix + "pejabat.ttd.guru", pejabat.getUserId() + suffix,
				pejabat.getGuru() != null ? pejabat.getGuru().getId() : null, LampiranLain.TTD_GURU, prefix);
	}

	public static void initGambarTandaTangan(SuratKeluar suratKeluar, Map<String, Object> parameters) {
		if (suratKeluar == null || suratKeluar.getId() == null)
			return;

		parameters.put("perihal", suratKeluar.getPerihal());

		if (suratKeluar.getSekolah() != null) {
			Sekolah sekolah = suratKeluar.getSekolah();
			Common.insertProperty(Sekolah.class, sekolah, parameters, "sekolah");
			parameters.put("NAMA_SEKOLAH", sekolah.getNama());
			parameters.put("ALAMAT_SEKOLAH", sekolah.getAlamat());
			parameters.put("TELP_SEKOLAH", sekolah.getTelp());
			parameters.put("FAX_SEKOLAH", sekolah.getFax());
			parameters.put("EMAIL_SEKOLAH", sekolah.getEmail());
			sekolah.putFile(parameters);
		}

		if (suratKeluar.getYayasan() != null) {
			Yayasan yayasan = suratKeluar.getYayasan();
			Common.insertProperty(Yayasan.class, yayasan, parameters, "yayasan");
			parameters.put("NAMA_YAYASAN", yayasan.getNama());
			parameters.put("ALAMAT_YAYASAN", yayasan.getAlamat());
			parameters.put("TELP_YAYASAN", yayasan.getTelp());
			parameters.put("FAX_YAYASAN", yayasan.getFax());
			parameters.put("EMAIL_YAYASAN", yayasan.getEmail());
			yayasan.putFile(parameters);
		}

		if (suratKeluar.getJurusan() != null) {
			Jurusan jurusan = suratKeluar.getJurusan();
			Common.insertProperty(Jurusan.class, jurusan, parameters, "jurusan");
			jurusan.putFile(parameters);
		}

		if (suratKeluar.getFakultas() != null) {
			Fakultas fakultas = suratKeluar.getFakultas();
			Common.insertProperty(Fakultas.class, fakultas, parameters, "fakultas");
			fakultas.putFile(parameters);
		}

		if (suratKeluar.getSatuanKerja() != null) {
			SatuanKerja satuanKerja = suratKeluar.getSatuanKerja();
			Common.insertProperty(SatuanKerja.class, satuanKerja, parameters, "satuanKerja");
		}

		Session session = HibernateUtil.currentSession();
		List<AlurPersetujuanSuratKeluarStatus> alurPersetujuanSuratKeluarStatuss = session
				.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
				.add(Restrictions.eq("disetujui", true)).add(Restrictions.eq("suratKeluar", suratKeluar)).list();

		if (!alurPersetujuanSuratKeluarStatuss.isEmpty()) {
			Session sessionStream = null;
			try {
				sessionStream = StreamingHibernateUtil.getInstance().currentSession();

				for (AlurPersetujuanSuratKeluarStatus status : alurPersetujuanSuratKeluarStatuss) {
					Pejabat pejabat = status.getPejabat();
					if (pejabat != null && pejabat.getJenisJabatan() != null) {
						FotoGambarTandaTanganPejabat foto = (FotoGambarTandaTanganPejabat) sessionStream
								.createCriteria(FotoGambarTandaTanganPejabat.class)
								.add(Restrictions.eq("pejabat", pejabat.getId())).setMaxResults(1)
								.addOrder(Order.desc("id")).uniqueResult();

						if (foto != null) {
							File file = foto.ambilFile();
							if (file != null && file.exists()) {
								parameters.put("ttd." + pejabat.getJenisJabatan().getKey(), file.getAbsolutePath());
							}
						}
					}
				}
			} catch (Exception e) {
				Common.tampilErrorJikaAdmin(e);
			} finally {
				if (sessionStream != null) {
					try {
						sessionStream.disconnect();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtil.java:636");}
					try {
						StreamingHibernateUtil.getInstance().closeSession();
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtil.java:639");}
				}
			}
		}
	}
}