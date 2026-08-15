package ais.action.master.surat.util;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.hibernate.EntityMode;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.metadata.ClassMetadata;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbarbutton;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.action.master.surat.KlasifikasiSuratKeluarAction;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormatNilai;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Matakuliah;
import ais.database.model.Pegawai;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.rab.Pejabat;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswa;
import ais.database.model.sekolah.Siswa;
import ais.database.model.surat.AlurPersetujuanSuratKeluarStatus;
import ais.database.model.surat.KlasifikasiSuratKeluar;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeter;
import ais.database.model.surat.KlasifikasiSuratKeluarParemeterValue;
import ais.database.model.surat.SuratKeluar;
import ais.database.model.surat.VariableSuratKeluar;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyToolbarbuttonConfig;

public class SuratUtilHelper {

	/* public agar SuratKeluarAction.onSave memakai konversi aman yang sama
	 * (attribute "nilai" bisa berisi java.io.File untuk parameter gambar). */
	public static String nilaiParameterAman(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof File) {
			return ((File) value).getAbsolutePath();
		}
		return String.valueOf(value).trim();
	}


	// =========================================================================================
	// 1. METHOD DELEGATOR (Penggabungan & Reuse agar tidak ada redundansi)
	// =========================================================================================
	@SuppressWarnings({ })
	public synchronized static Map<String, Object> ubahIsiSuratKeluar(SuratKeluar suratKeluar, Tbmuser tbmuser,
			Groupbox west, final Map<String, Object> parameters) {

		if (suratKeluar == null) {
			return parameters == null ? new HashMap<String, Object>() : parameters;
		}

		// Jangan memanggil getter relasi lazy di delegator.
		// Semua relasi akan diambil ulang di method utama menggunakan session baru yang terisolasi.
		return ubahIsiSuratKeluar(null, null, null, null, null, tbmuser, null, suratKeluar, west, parameters);
	}

	// =========================================================================================
	// 2. METHOD UTAMA (CORE ENGINE)
	// =========================================================================================
	@SuppressWarnings({ "deprecation", "unchecked" })
	public synchronized static Map<String, Object> ubahIsiSuratKeluar(Rows rows,
			KlasifikasiSuratKeluar myKlasifikasiSuratKeluar, Mahasiswa mahasiswa, Dosen dosen, Pegawai pegawai,
			Tbmuser tbmuser, String mycode, SuratKeluar suratKeluar, Groupbox west,
			final Map<String, Object> parameters) {

		Session session = null;
		
		try {
			session = openIsolatedSession();
			
			if (!isSessionUsable(session)) {
				throw new org.hibernate.SessionException("Session surat keluar tidak dapat dibuka.");
			}

			if (parameters == null) {
				return new HashMap<String, Object>();
			}

			if (rows == null) {
				suratKeluar = resolveSuratKeluar(session, suratKeluar);
			}
			if (suratKeluar != null) {
				if (myKlasifikasiSuratKeluar == null) {
					myKlasifikasiSuratKeluar = suratKeluar.getKlasifikasiSuratKeluar();
				}
				if (mahasiswa == null) {
					mahasiswa = suratKeluar.getMahasiswa();
				}
				if (dosen == null) {
					dosen = suratKeluar.getDosen();
				}
				if (pegawai == null) {
					pegawai = suratKeluar.getPegawai();
				}
				if (mycode == null || mycode.trim().length() == 0) {
					mycode = suratKeluar.getKode();
				}
			}

			parameters.clear();
			parameters.put("tidak_usah_pakai_connection", true);

			Guru guru = suratKeluar != null ? suratKeluar.getGuru() : null;
			Siswa siswa = suratKeluar != null ? suratKeluar.getSiswa() : null;

			// --- PROSES SURAT KELUAR UMUM (DATES, QR, KOP) ---
			if (suratKeluar != null) {
				parameters.put("qr.surat", suratKeluar.ttdQr());
				parameters.put("nomor.surat", mycode != null ? mycode : suratKeluar.getKode());
				parameters.put("perihal", suratKeluar.getPerihal() != null ? suratKeluar.getPerihal() : "");

				Date tgl = suratKeluar.getTanggal();
				String strTanggalQr = "";

				if (tgl != null) {
					strTanggalQr = Common.dateFormat2.get().format(tgl);
					parameters.put("tanggal", Common.dateFormat5.get().format(tgl));
					parameters.put("tanggal.format1", Common.dateFormat2.get().format(tgl));
					parameters.put("tanggal.formated1", Common.dateFormat6.get().format(tgl));
					parameters.put("tanggal.formated2", Common.dateFormat2.get().format(tgl));
					parameters.put("tanggal.formated3", Common.dateFormat51.get().format(tgl));
					parameters.put("tanggal.formated4", Common.timeFormat.get().format(tgl));
					parameters.put("tanggal.formated5", Common.dateFormat1.get().format(tgl));
					parameters.put("tanggal.iso", Common.dateFormatInput.get().format(tgl));
					parameters.put("tanggal.datetime_format", Common.databaseDateFormat.get().format(tgl));
				}

				try {
					String code = (suratKeluar.getKode() != null ? suratKeluar.getKode() : "") + "\n" + strTanggalQr
							+ "\n" + parameters.get("perihal");
					File myfilebarcode = new File(
							Common.ambilREAL_PATH_REPORT() + "/crcode_surat_" + suratKeluar.getId() + ".png");
					BarcodeCommon.generateCRCode(code, myfilebarcode);
					parameters.put("qr_code_surat", myfilebarcode.getAbsolutePath());
				} catch (Exception e) {
					safeError(e);
				}

				PerguruanTinggi perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
				if (suratKeluar.getAlurPersetujuanSuratKeluar() != null
						&& suratKeluar.getAlurPersetujuanSuratKeluar().getSatuanKerja() != null) {
					SuratUtil.initDefaultKopAja(suratKeluar.getAlurPersetujuanSuratKeluar().getSatuanKerja(),
							perguruanTinggi, parameters, "kop_alur");
				} else if (suratKeluar.getSatuanKerja() != null) {
					SuratUtil.initDefaultKopAja(suratKeluar.getSatuanKerja(), perguruanTinggi, parameters, "kop_alur");
				} else {
					parameters.put("kop_alur", "");
				}

				SuratUtil.initGambarTandaTangan(suratKeluar, parameters);
			}

			// --- PROSES IDENTITAS PENGGUNA (Menggunakan Helper) ---
			prosesDataPerson(pegawai, "pegawai", LampiranLain.TTD_PEGAWAI, parameters);
			if (pegawai != null && pegawai.getId() != null) {
				salinClassMetadata(pegawai, Pegawai.class, "pegawai", parameters);
				Common.insertProperty(Pegawai.class, pegawai, parameters, "pegawai");
			}

			prosesDataPerson(siswa, "siswa", LampiranLain.TTD_SISWA, parameters);
			if (siswa != null && siswa.getId() != null) {
				try {
					if (siswa.getKelas() != null)
						Common.insertProperty(KelasSiswa.class, siswa.getKelas(), parameters, "kelas", 1, "sekolah", "yayasan");
					if (siswa.getGuruBk() != null)
						Common.insertProperty(Guru.class, siswa.getGuruBk(), parameters, "guru_bk", 1, "sekolah", "yayasan");
					if (siswa.getGuruPembina() != null)
						Common.insertProperty(Guru.class, siswa.getGuruPembina(), parameters, "guru_wali", 1, "sekolah", "yayasan");
					Common.insertProperty(Siswa.class, siswa, parameters, "siswa", 4);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:216");}
				parameters.put("siswa.kelas", siswa.getKelas() == null ? "-" : siswa.getKelas().getNama());
			}

			prosesDataPerson(guru, "guru", LampiranLain.TTD_GURU, parameters);
			if (guru != null && guru.getId() != null) {
				try {
					Common.insertProperty(Guru.class, guru, parameters, "guru", 4);
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:224");}
			}

			prosesDataPerson(dosen, "dosen", LampiranLain.TTD_DOSEN, parameters);
			if (dosen != null && dosen.getId() != null) {
				if (dosen.getFakultas() != null) {
					Fakultas f = dosen.getFakultas();
					if (f.getPegawai1() != null) Common.insertProperty(Pegawai.class, f.getPegawai1(), parameters, "pejabat1", 1);
					if (f.getPegawai2() != null) Common.insertProperty(Pegawai.class, f.getPegawai2(), parameters, "pejabat2", 1);
					if (f.getPegawai3() != null) Common.insertProperty(Pegawai.class, f.getPegawai3(), parameters, "pejabat3", 1);
				}
				salinClassMetadata(dosen, Dosen.class, "dosen", parameters);
				Common.insertProperty(Dosen.class, dosen, parameters, "dosen");
			}

			prosesDataPerson(mahasiswa, "mahasiswa", LampiranLain.TTD_MAHASISWA, parameters);
			if (mahasiswa != null && mahasiswa.getId() != null) {
				Common.insertProperty(Mahasiswa.class, mahasiswa, parameters, "mahasiswa", 4);

				if (mahasiswa.getJurusan() != null && mahasiswa.getJurusan().getFakultas() != null) {
					Fakultas fak = mahasiswa.getJurusan().getFakultas();
					if (fak.getPegawai1() != null) Common.insertProperty(Pegawai.class, fak.getPegawai1(), parameters, "pejabat1", 1);
					if (fak.getPegawai2() != null) Common.insertProperty(Pegawai.class, fak.getPegawai2(), parameters, "pejabat2", 1);
					if (fak.getPegawai3() != null) Common.insertProperty(Pegawai.class, fak.getPegawai3(), parameters, "pejabat3", 1);
					Common.insertProperty(Fakultas.class, fak, parameters, "fakultas", 1);
					parameters.put("mahasiswa.fakultas", fak.getNama());
					parameters.put("mahasiswa.fakultas.nama", fak.getNama());
				}

				BiodataMahasiswa biodataMahasiswa = mahasiswa.ambilBiodata();
				if (biodataMahasiswa != null) Common.insertProperty(BiodataMahasiswa.class, biodataMahasiswa, parameters, "biodataMahasiswa", 3);

				parameters.put("mahasiswa.semester", mahasiswa.currentSemester());
				parameters.put("mahasiswa.tahap", mahasiswa.currentTahapan());

				try {
					List<Long> detailperkuliahans = mahasiswa.ambilDetailperkuliahan();
					if (detailperkuliahans != null) {
						for (Long detailId : detailperkuliahans) {
							Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class, detailId.toString());
							if (d != null && Detailperkuliahan.DISETUJUI.equals(d.getPersetujuan())) {
								Matakuliah mk = d.getMatakuliahKonversi() == null ? d.getPerkuliahan().getMatakuliah() : d.getMatakuliahKonversi();
								String mkPfx = "mahasiswa.mk." + mk.getKode() + ".";
								parameters.put(mkPfx + "ta", d.getTahunAkademik());
								parameters.put(mkPfx + "smt", d.getSemester());
								parameters.put(mkPfx + "nama", mk.getNama());
								parameters.put(mkPfx + "kode", mk.getKode());
								parameters.put(mkPfx + "nilai", d.getTotalNilai());
								parameters.put(mkPfx + "ip", d.getTotalIP());
								parameters.put(mkPfx + "huruf", d.getNilaiHuruf());
								parameters.put(mkPfx + "info", d.getPerkuliahan() == null ? "" : d.getPerkuliahan().info());
								parameters.put(mkPfx + "info_simple", d.getPerkuliahan() == null ? "" : d.getPerkuliahan().infoSimple());

								if (d.getPerkuliahan() != null) {
									for (FormatNilai fn : Common.getFormatNilais(session, d.getPerkuliahan())) {
										parameters.put(mkPfx + fn.getNama(), d.retreiveDetailNilai(fn));
									}
								}
							}
						}
					}
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:285");}

				if (suratKeluar != null && suratKeluar.getTahunAkademik() != null) {
					Integer tahun = 0;
					try {
						if (suratKeluar.getTahunAkademik().contains("/")) {
							tahun = Integer.parseInt(StringUtils.split(suratKeluar.getTahunAkademik(), "/")[0]);
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:293");}

					Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), suratKeluar.getSemester(),
							mahasiswa.getPindahKeKampusIniMasukSemester(), tahun, mahasiswa.getSemesterMulai());
					KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null);
					Common.insertProperty(KrsMahasiswa.class, krs, parameters, "mahasiswa.krs", 1, "mahasiswa");
					Common.insertProperty(HistoryStatusMahasiswa.class, ais.action.master.helper.HistoryStatusMahasiswaUtil.getHistoryStatusMahasiswa(krs),
							parameters, "mahasiswa.status", 1, "mahasiswa");
				}

				Skripsi skripsi = (Skripsi) ConstantValues.simpleObject(session.createCriteria(Skripsi.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.desc("id")).setMaxResults(1), Skripsi.class);
				if (skripsi != null) {
					parameters.put("mahasiswa.skripsi.judul", skripsi.getJudul());
					parameters.put("mahasiswa.skripsi.judulen", skripsi.getJudulen());
					int idx = 1;
					for (Dosen d : skripsi.populateDosenBuNama()) {
						parameters.put("mahasiswa.skripsi.dosen" + idx + ".nama", d.getNama());
						parameters.put("mahasiswa.skripsi.dosen" + idx + ".nidn", d.getNidn());
						parameters.put("mahasiswa.skripsi.dosen" + idx + ".nip", d.getMycode());
						idx++;
					}
				}

				List<MahasiswaRequestTugasAkhir> reqTA = ConstantValues.simpleList(session.createCriteria(MahasiswaRequestTugasAkhir.class)
						.add(Restrictions.eq("mahasiswa", mahasiswa)).addOrder(Order.asc("id")), MahasiswaRequestTugasAkhir.class);
				if (reqTA != null) {
					int idxTA = 0;
					for (MahasiswaRequestTugasAkhir req : reqTA) {
						parameters.put("mahasiswa.bimbingan.judul", req.getJudul());
						int idxDsn = 1;
						String pfxBim = "mahasiswa.bimbingan" + (idxTA > 0 ? String.valueOf(idxTA) : "");
						for (Dosen d : req.populateDosenBuNama()) {
							parameters.put(pfxBim + ".dosen" + idxDsn + ".nama", d.getNama());
							parameters.put(pfxBim + ".dosen" + idxDsn + ".nidn", d.getNidn());
							parameters.put(pfxBim + ".dosen" + idxDsn + ".nip", d.getMycode());
							idxDsn++;
						}
						idxTA++;
					}
				}
			}

			// --- VARIABLES & ALUR STATUS ---
			Map<Long, VariableSuratKeluar> varSurat = ConstantValues.ambilBerdasarClass(VariableSuratKeluar.class);
			for (VariableSuratKeluar v : varSurat.values()) {
				parameters.put(v.getKey(), v.getNilai());
			}

			if (suratKeluar != null && suratKeluar.getId() != null) {
				List<AlurPersetujuanSuratKeluarStatus> alurStatusList = session
						.createCriteria(AlurPersetujuanSuratKeluarStatus.class).add(Restrictions.isNotNull("kodeUnik"))
						.add(Restrictions.eq("suratKeluar", suratKeluar)).addOrder(Order.asc("id")).list();

				int index = 1;
				for (AlurPersetujuanSuratKeluarStatus alur : alurStatusList) {
					if (alur.getPejabat() != null) {
						String status = alur.getDisetujui() ? "setujui." : (alur.getDitolak() ? "tolak." : "belum.");
						SuratUtil.ttdpejabat(alur.getPejabat(), parameters, status);
						SuratUtil.ttdpejabat(alur.getPejabat(), parameters, status + index + ".");
						SuratUtil.ttdpejabat(alur.getPejabat(), parameters, "semua.");
						SuratUtil.ttdpejabat(alur.getPejabat(), parameters, "semua." + index + ".");
					}
					if (alur.getKonseptor() != null) {
						SuratUtil.ttdpejabat(parameters, alur.getKonseptor(), "disposisi." + index + ".oleh", 1);
					}
					parameters.put("disposisi." + index + ".qr.surat", alur.ttdQr());
					try {
						LampiranLain lamp = LampiranLain.ambil(alur.getId(), AlurPersetujuanSuratKeluarStatus.class.getName());
						if (lamp != null) {
							parameters.put("disposisi." + index + ".lampiran.nama", lamp.getNama());
							parameters.put("disposisi." + index + ".lampiran.file", lamp.ambilFile().getAbsolutePath());
							parameters.put("disposisi." + index + ".lampiran.url", lamp.createLinkUri(false));
						}
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:367");}
					index++;
				}
			}

			List<Pejabat> pejabats = ConstantValues.simpleList(session.createCriteria(Pejabat.class).add(Restrictions.isNotNull("jenisJabatan")), Pejabat.class);
			for (Pejabat p : pejabats) {
				SuratUtil.ttdpejabat(p, parameters, "");
			}

			// =========================================================================
			// --- PEMROSESAN KLASIFIKASI PARAMETER (LOGIKA PENYATUAN) ---
			// =========================================================================
			
			if (rows != null) { 
				// MODE UI: Mengambil parameter dari komponen layar pengguna
				for (Row row : (List<Row>) (java.util.List) rows.getChildren()) {
					KlasifikasiSuratKeluarParemeter klasifikasiParam = (KlasifikasiSuratKeluarParemeter) row.getAttribute("klasifikasiSuratKeluarParemeter");
					if (klasifikasiParam == null) continue;

					try {
						Object comp = row.getAttribute("komponen") != null ? row.getAttribute("komponen") : row.getChildren().get(1);
						String h = "";
						if (comp instanceof Combobox) h = ((Combobox) comp).getValue().trim();
						else if (comp instanceof Textbox) h = ((Textbox) comp).getValue().trim();
						else if (comp instanceof Intbox) h = String.valueOf(((Intbox) comp).getValue());
						else if (comp instanceof Doublebox) h = String.valueOf(((Doublebox) comp).getValue());
						else if (comp instanceof Datebox) h = Common.dateFormat2.get().format(((Datebox) comp).getValue() == null ? ais.ui.util.WaktuUtil.getDate() : ((Datebox) comp).getValue());
						else if (comp instanceof Hbox) h = nilaiParameterAman(((Hbox) comp).getAttribute("nilai"));
						else if (comp instanceof MyButtonConfig) h = nilaiParameterAman(((MyButtonConfig) comp).getAttribute("nilai"));

						String keyParam = klasifikasiParam.getKey();
						parameters.put(keyParam, h);
						String tipe = klasifikasiParam.getTipe();
						
						if (KlasifikasiSuratKeluarParemeter.DATA.equals(tipe)) {
							KlasifikasiSuratKeluarAction.masukkanTabelKeParameter(parameters, h);
						} else if (KlasifikasiSuratKeluarParemeter.GAMBAR.equals(tipe)) {
							try {
								LampiranLain l = LampiranLain.ambil(klasifikasiParam.getId(), KlasifikasiSuratKeluarParemeter.class.getName());
								if (l != null) parameters.put(keyParam, l.ambilFile().getAbsolutePath());
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:408");}
						} else if (KlasifikasiSuratKeluarParemeter.DAFTAR_MAHASISWA.equals(tipe)) {
							prosesDaftarMahasiswa(h, keyParam, parameters);
						} else if (KlasifikasiSuratKeluarParemeter.DAFTAR_SISWA.equals(tipe)) {
							prosesDaftarSiswa(h, keyParam, parameters);
						} else if (KlasifikasiSuratKeluarParemeter.DAFTAR_PENGGUNA.equals(tipe)) {
							prosesDaftarPengguna(h, keyParam, dosen, guru, pegawai, parameters);
						}
					} catch (Exception e) {
						safeError(e);
					}
				}
			} else {
				// MODE DATABASE: Mengambil parameter tersimpan dari database (Background Task)
				if (suratKeluar != null && suratKeluar.getId() != null) {
					// Load parameter defaults dari setup klasifikasi
					if (suratKeluar.getKlasifikasiSuratKeluar() != null) {
						List<KlasifikasiSuratKeluarParemeter> paramsKlasifikasi = session
								.createCriteria(KlasifikasiSuratKeluarParemeter.class).addOrder(Order.asc("nomorUrut"))
								.add(Restrictions.eq("klasifikasiSuratKeluar", suratKeluar.getKlasifikasiSuratKeluar())).list();

						for (KlasifikasiSuratKeluarParemeter param : paramsKlasifikasi) {
							if (param != null) {
								try { parameters.put(param.getKey(), param.getNilai()); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:431");}

								if (KlasifikasiSuratKeluarParemeter.DATA.equals(param.getTipe())) {
									KlasifikasiSuratKeluarAction.masukkanTabelKeParameter(parameters, param.getNilai());
								} else if (KlasifikasiSuratKeluarParemeter.GAMBAR.equals(param.getTipe())) {
									try {
										// Sama seperti mode UI (lihat baris ~406): baris parameter GAMBAR bisa
										// tak punya lampiran (template/data surat belum lengkap), sehingga
										// LampiranLain.ambil(...) mengembalikan null. Akses .ambilFile() langsung
										// tanpa guard memicu NullPointerException; lewati baris seperti ini.
										LampiranLain l = LampiranLain.ambil(param.getId(), KlasifikasiSuratKeluarParemeter.class.getName());
										if (l != null && l.ambilFile() != null) {
											parameters.put(param.getKey(), l.ambilFile().getAbsolutePath());
										}
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:438");}
								}
							}
						}
					}

					// Load data inputan spesifik surat dari KlasifikasiSuratKeluarParemeterValue
					List<KlasifikasiSuratKeluarParemeterValue> parameterValues = session
							.createCriteria(KlasifikasiSuratKeluarParemeterValue.class)
							.add(Restrictions.eq("suratKeluar", suratKeluar)).list();

					for (KlasifikasiSuratKeluarParemeterValue paramValue : parameterValues) {
						// PENTING: KlasifikasiSuratKeluarParemeterValue bisa jadi baris yatim -
						// relasi klasifikasiSuratKeluarParemeter-nya null bila template parameter
						// sudah dihapus/diubah setelah value tersimpan (data surat/template belum
						// lengkap). Akses getKlasifikasiSuratKeluarParemeter() langsung tanpa guard
						// memicu NullPointerException; skip baris seperti ini.
						KlasifikasiSuratKeluarParemeter parameterTemplate = paramValue.getKlasifikasiSuratKeluarParemeter();
						if (parameterTemplate == null) {
							continue;
						}

						String keyParam = parameterTemplate.getKey();
						String h = paramValue.getNama() != null ? paramValue.getNama().trim() : "";
						parameters.put(keyParam, h);

						String tipeParam = parameterTemplate.getTipe();

						if (KlasifikasiSuratKeluarParemeter.GAMBAR.equals(tipeParam)) {
							try {
								// LampiranLain.ambil(...) mengembalikan null bila belum ada
								// file yang diupload untuk parameter GAMBAR ini (kasus normal
								// untuk parameter yang belum diisi) - jangan panggil
								// .ambilFile() pada hasil null.
								LampiranLain lampiranGambar = LampiranLain.ambil(parameterTemplate.getId(), KlasifikasiSuratKeluarParemeter.class.getName());
								if (lampiranGambar != null) {
									parameters.put(keyParam, lampiranGambar.ambilFile().getAbsolutePath());
								}
							} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:459");}
						} else if (KlasifikasiSuratKeluarParemeter.DAFTAR_MAHASISWA.equals(tipeParam)) {
							prosesDaftarMahasiswa(h, keyParam, parameters);
						} else if (KlasifikasiSuratKeluarParemeter.DAFTAR_SISWA.equals(tipeParam)) {
							prosesDaftarSiswa(h, keyParam, parameters);
						} else if (KlasifikasiSuratKeluarParemeter.DAFTAR_PENGGUNA.equals(tipeParam)) {
							prosesDaftarPengguna(h, keyParam, dosen, guru, pegawai, parameters);
						}
					}
				}
			}

			SuratUtil.initDefaultKop(parameters, tbmuser);

			// --- UI BUILDING (West Layout) ---
			if (west != null) {
				Common.clear(west);
				west.setVisible(Common.getApakahAdmin());
				west.appendChild(new Caption("Parameter Surat Keluar"));

				if (west.isVisible()) {
					MyGrid grid = new MyGrid();
					grid.setWidth("100%");
					grid.setHeight("100%");
					grid.setParent(west);

					Columns columns = new Columns();
					columns.setParent(grid);
					MyColumnConfig col;
					(col = new MyColumnConfig("Key")).setParent(columns);
					col.setWidth("40%");
					new MyColumnConfig("Nilai").setParent(columns);

					final Rows rowsCari = new Rows();
					rowsCari.setParent(grid);

					MyFormRow row = new MyFormRow();
					row.setValign("top");
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.setParent(rowsCari);

					Hbox hbox = new Hbox();
					row.appendChild(hbox);

					final Textbox cari = new Textbox("");
					cari.setParent(hbox);
					cari.setCols(20);

					EventListener cariAkun = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.clear(rowsCari, null, 1);
							String cariStr = cari.getValue().trim().toLowerCase();
							for (Map.Entry<String, Object> entry : parameters.entrySet()) {
								try {
									String key = entry.getKey();
									String val = entry.getValue() == null ? "" : entry.getValue().toString();
									if (cariStr.isEmpty() || val.toLowerCase().contains(cariStr) || key.toLowerCase().contains(cariStr)) {
										MyFormRow r = new MyFormRow();
										r.setValign("top");
										r.setParent(rowsCari);
										r.appendChild(new MyLabelKecil(key));
										r.appendChild(new MyLabelKecil(val));
									}
								} catch (Exception e) {
									safeError(e);
								}
							}
						}
					};

					cari.addEventListener("onOK", cariAkun);
					Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/svg/search.svg");
					toolbarbutton.setParent(hbox);
					toolbarbutton.addEventListener("onClick", cariAkun);

					try {
						cariAkun.onEvent(null);
					} catch (Exception e) {
						Common.tampilErrorJikaAdmin(e);
					}
				}
			}

			return parameters;

		} catch (Exception e) {
			safeError(e);
			return parameters == null ? new HashMap<String, Object>() : parameters;
		} finally {
			// Session dibuat manual melalui openSession(), jadi hanya session ini yang ditutup.
			// Jangan memanggil HibernateUtil.closeSession() di sini karena dapat menutup
			// currentSession milik request ZK yang sedang aktif.
			closeOpenedSession(session);
		}
	}


	private static Session openIsolatedSession() {
		try {
			return HibernateUtil.getSessionFactory().openSession();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private static SuratKeluar resolveSuratKeluar(Session session, SuratKeluar suratKeluar) {
		if (suratKeluar == null || suratKeluar.getId() == null || !isSessionUsable(session)) {
			return suratKeluar;
		}
		try {
			SuratKeluar managed = (SuratKeluar) session.get(SuratKeluar.class, suratKeluar.getId());
			return managed == null ? suratKeluar : managed;
		} catch (Exception e) {
			safeError(e);
			return suratKeluar;
		}
	}

	private static boolean isSessionUsable(Session session) {
		try {
			return session != null && session.isOpen() && session.isConnected();
		} catch (Exception e) {
			return false;
		}
	}

	private static void closeOpenedSession(Session session) {
		if (session == null) {
			return;
		}
		try {
			if (session.isOpen()) {
				try {
					session.clear();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:595");
				}
				try {
					session.disconnect();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:599");
				}
				try {
					session.close();
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:603");
				}
			}
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:606");
		}
	}

	private static String safeString(Object value) {
		return value == null ? "" : value.toString();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	private static void safePut(Map<String, Object> parameters, String key, Object value) {
		if (parameters != null && key != null) {
			parameters.put(key, value == null ? "" : value);
		}
	}

	private static void safeError(Exception e) {
		try {
			Common.tampilErrorJikaAdmin(e);
		} catch (Exception ignored) { ais.common.ErrorAuditUtil.record(ignored, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:627");
		}
	}

	// =========================================================================
	// HELPER METHODS (Disederhanakan untuk Efisiensi)
	// =========================================================================

	private static void prosesDataPerson(Object personEntity, String prefix, String tipeLampiran, Map<String, Object> parameters) {
		if (personEntity == null) {
			parameters.put(prefix + ".ttd", "");
			return;
		}
		try {
			personEntity.getClass().getMethod("putPhoto", Map.class).invoke(personEntity, parameters);
			Long id = (Long) personEntity.getClass().getMethod("getId").invoke(personEntity);
			String ttdPath = "";
			if (id != null) {
				LampiranLain lampiranLain = LampiranLain.ambil(id, tipeLampiran);
				if (lampiranLain != null) {
					// GATE (sama seperti SuratUtil.putTtdPath): jangan taruh path gambar TTD yang
					// rusak/kosong/bukan format gambar ke parameter laporan -- kalau lolos, JasperReports/
					// iText melempar "The byte array is not a recognized imageformat" saat export PDF
					// (Report$ReportGenerationException) dan membatalkan seluruh pembuatan laporan.
					File fileTtd = lampiranLain.ambilFile();
					if (Common.isGambarLaporanValid(fileTtd)) {
						ttdPath = fileTtd.getAbsolutePath();
					}
				}
			}
			parameters.put(prefix + ".ttd", ttdPath);
		} catch (Exception e) {
			parameters.put(prefix + ".ttd", "");
		}
	}

	@SuppressWarnings("rawtypes")
	private static void salinClassMetadata(Object entity, Class clazz, String prefix, Map<String, Object> parameters) {
		ClassMetadata metadata = HibernateUtil.getClassMetadata(clazz);
		if (metadata == null) return;

		String[] properties = metadata.getPropertyNames();
		for (String property : properties) {
			try {
				Object val = metadata.getPropertyValue(entity, property, EntityMode.POJO);
				parameters.put(prefix + "." + property, val == null ? "" : val.toString());

				if (val instanceof GeneralValueObject) {
					ClassMetadata localMeta = HibernateUtil.getClassMetadata(val.getClass());
					if (localMeta != null) {
						for (String localProp : localMeta.getPropertyNames()) {
							Object valLocal = localMeta.getPropertyValue(val, localProp, EntityMode.POJO);
							parameters.put(prefix + "." + property + "." + localProp, valLocal == null ? "" : valLocal.toString());
						}
					}
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:672");}
		}
	}

	private static void gabungNilaiMap(Map<String, String> map, String key, Object value) {
		String valStr = (value == null || value.toString().trim().isEmpty()) ? "-" : value.toString();
		String existing = map.get(key);
		map.put(key, existing == null ? valStr : existing + ";" + valStr);
	}

	private static void prosesDaftarMahasiswa(String stringData, String paramKey, Map<String, Object> parameters) {
		try {
			int i = 1;
			ClassMetadata meta = HibernateUtil.getClassMetadata(Mahasiswa.class);
			Map<String, String> mapData = new HashMap<String, String>();

			for (String m : stringData.split(",")) {
				Mahasiswa mhs = (m == null || m.trim().isEmpty()) ? null : (Mahasiswa) ConstantValues.ambilByNim(m);
				if (mhs != null) {
					for (String property : meta.getPropertyNames()) {
						Object val = meta.getPropertyValue(mhs, property, EntityMode.POJO);
						gabungNilaiMap(mapData, "daftar.mhs." + property, val);
					}
					Common.insertProperty(Mahasiswa.class, mhs, parameters, paramKey + "." + m.trim());
					Common.insertProperty(Mahasiswa.class, mhs, parameters, paramKey + "." + i++);
				}
			}
			for (Map.Entry<String, String> entry : mapData.entrySet()) {
				parameters.put(paramKey + "." + entry.getKey().replace("daftar.mhs.", ""), entry.getValue());
			}
			for (Object k : mapData.keySet())
				parameters.put(paramKey + "." + k, mapData.get(k));
		} catch (Exception e) {
			safeError(e);
		}
	}

	private static void prosesDaftarSiswa(String stringData, String paramKey, Map<String, Object> parameters) {
		try {
			int i = 1;
			ClassMetadata meta = HibernateUtil.getClassMetadata(Siswa.class);
			Map<String, String> mapData = new HashMap<String, String>();

			for (String m : stringData.split(",")) {
				Siswa sis = (m == null || m.trim().isEmpty()) ? null : (Siswa) ConstantValues.ambilByNis(m);
				if (sis != null) {
					for (String property : meta.getPropertyNames()) {
						Object val = meta.getPropertyValue(sis, property, EntityMode.POJO);
						gabungNilaiMap(mapData, "daftar.sis." + property, val);
					}
					gabungNilaiMap(mapData, "daftar.siswa.kelas", sis.getKelas() == null ? "-" : sis.getKelas().getNama());
					Common.insertProperty(Siswa.class, sis, parameters, paramKey + "." + m.trim());
					Common.insertProperty(Siswa.class, sis, parameters, paramKey + "." + i++);
				}
			}
			for (Map.Entry<String, String> entry : mapData.entrySet()) {
				parameters.put(paramKey + "." + entry.getKey().replace("daftar.sis.", ""), entry.getValue());
			}
			for (Object k : mapData.keySet())
				parameters.put(paramKey + "." + k, mapData.get(k));
		} catch (Exception e) {
			safeError(e);
		}
	}

	private static void prosesDaftarPengguna(String stringData, String paramKey, Dosen dosen, Guru guru, Pegawai pegawai, Map<String, Object> parameters) {
		try {
			ClassMetadata metaMhs = HibernateUtil.getClassMetadata(Mahasiswa.class);
			ClassMetadata metaSiswa = HibernateUtil.getClassMetadata(Siswa.class);
			ClassMetadata metaUser = HibernateUtil.getClassMetadata(Tbmuser.class);
			ClassMetadata metaDosen = HibernateUtil.getClassMetadata(Dosen.class);
			ClassMetadata metaGuru = HibernateUtil.getClassMetadata(Guru.class);
			ClassMetadata metaPegawai = HibernateUtil.getClassMetadata(Pegawai.class);

			TreeSet<String> stringsAll = new TreeSet<String>();
			if (metaSiswa != null) stringsAll.addAll(Arrays.asList(metaSiswa.getPropertyNames()));
			if (metaMhs != null) stringsAll.addAll(Arrays.asList(metaMhs.getPropertyNames()));
			if (metaDosen != null) stringsAll.addAll(Arrays.asList(metaDosen.getPropertyNames()));
			if (metaGuru != null) stringsAll.addAll(Arrays.asList(metaGuru.getPropertyNames()));
			if (metaPegawai != null) stringsAll.addAll(Arrays.asList(metaPegawai.getPropertyNames()));

			Map<String, String> mapData = new HashMap<String, String>();

			prosesPengaju(dosen, metaDosen, stringsAll, mapData, "dosen");
			prosesPengaju(guru, metaGuru, stringsAll, mapData, "guru");
			prosesPengaju(pegawai, metaPegawai, stringsAll, mapData, "pegawai");

			if (stringData != null && !stringData.trim().isEmpty()) {
				int i = 1;
				for (String m : stringData.split(",")) {
					m = m.trim();
					if (m.isEmpty()) continue;

					Tbmuser user = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), m);
					if (user != null) {
						prosesPengikutUser(user, metaDosen, metaGuru, metaPegawai, metaUser, stringsAll, mapData);
						Common.insertProperty(Tbmuser.class, user, parameters, paramKey + "." + m, 2);
						Common.insertProperty(Tbmuser.class, user, parameters, paramKey + "." + i++, 2);
					} else {
						Mahasiswa mhsData = (Mahasiswa) ConstantValues.ambilByNim(m);
						if (mhsData != null && mhsData.getId() != null) {
							prosesPengikutMahasiswa(mhsData, metaMhs, stringsAll, mapData);
							Common.insertProperty(Mahasiswa.class, mhsData, parameters, paramKey + "." + m, 2);
							Common.insertProperty(Mahasiswa.class, mhsData, parameters, paramKey + "." + i++, 2);
						} else {
							Siswa siswaData = (Siswa) ConstantValues.ambilByNis(m);
							if (siswaData != null && siswaData.getId() != null) {
								prosesPengikutSiswa(siswaData, metaSiswa, stringsAll, mapData);
								gabungNilaiMap(mapData, "daftar.siswa.kelas", siswaData.getKelas() == null ? "-" : siswaData.getKelas().getNama());
								Common.insertProperty(Siswa.class, siswaData, parameters, paramKey + "." + m, 2);
								Common.insertProperty(Siswa.class, siswaData, parameters, paramKey + "." + i++, 2);
							}
						}
					}
				}
			}

			for (Map.Entry<String, String> entry : mapData.entrySet()) {
				parameters.put(paramKey + "." + entry.getKey(), entry.getValue());
			}
			for (Object k : mapData.keySet())
				parameters.put(paramKey + "." + k, mapData.get(k));
		} catch (Exception e) {
			safeError(e);
		}
	}

	private static void prosesPengaju(Object entity, ClassMetadata meta, Set<String> propsAll, Map<String, String> mapData, String tipe) {
		if (entity == null || meta == null) return;
		for (String prop : propsAll) {
			Object val = null;
			try { val = meta.getPropertyValue(entity, prop, EntityMode.POJO); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:803");}
			gabungNilaiMap(mapData, "daftar." + tipe + "." + prop, val);
			gabungNilaiMap(mapData, "pengaju." + tipe + "." + prop, val);
		}
	}

	private static void prosesPengikutUser(Tbmuser user, ClassMetadata metaDosen, ClassMetadata metaGuru, ClassMetadata metaPegawai, ClassMetadata metaUser, Set<String> propsAll, Map<String, String> mapData) {
		if (user.getDosen() != null && metaDosen != null) {
			for (String prop : propsAll) {
				Object val = null;
				try { val = metaDosen.getPropertyValue(user.getDosen(), prop, EntityMode.POJO); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:813");}
				gabungNilaiMap(mapData, "daftar.dosen." + prop, val);
				gabungNilaiMap(mapData, "pengikut.dosen." + prop, val);
			}
		}
		if (user.getGuru() != null && metaGuru != null) {
			for (String prop : propsAll) {
				Object val = null;
				try { val = metaGuru.getPropertyValue(user.getGuru(), prop, EntityMode.POJO); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:821");}
				gabungNilaiMap(mapData, "daftar.guru." + prop, val);
				gabungNilaiMap(mapData, "pengikut.guru." + prop, val);
			}
		}
		if (user.getPegawai() != null && metaPegawai != null) {
			for (String prop : propsAll) {
				Object val = null;
				try { val = metaPegawai.getPropertyValue(user.getPegawai(), prop, EntityMode.POJO); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:829");}
				String valStr = (val == null || val.toString().trim().isEmpty()) ? "-" : val.toString();
				if (user.getPegawai().getDosen() != null) {
					if (prop.equalsIgnoreCase("kode")) valStr = user.getPegawai().getDosen().getNidn();
					if (valStr.equals("-") && prop.equalsIgnoreCase("jabatan")) valStr = "Dosen";
				}
				gabungNilaiMap(mapData, "daftar.pegawai." + prop, valStr);
				gabungNilaiMap(mapData, "pengikut.pegawai." + prop, valStr);
				gabungNilaiMap(mapData, "pengikut.semua." + prop, valStr);
			}
		}
		if (metaUser != null) {
			for (String prop : metaUser.getPropertyNames()) {
				Object val = null;
				try { val = metaUser.getPropertyValue(user, prop, EntityMode.POJO); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:843");}
				gabungNilaiMap(mapData, "daftar.pengguna." + prop, val);
				gabungNilaiMap(mapData, "pengikut.pengguna." + prop, val);
				gabungNilaiMap(mapData, "pengikut.semua." + prop, val);
			}
		}
	}

	private static void prosesPengikutMahasiswa(Mahasiswa mhs, ClassMetadata metaMhs, Set<String> propsAll, Map<String, String> mapData) {
		if (metaMhs == null) return;
		for (String prop : propsAll) {
			Object val = null;
			try {
				String actualProp = (prop.equalsIgnoreCase("kode") || prop.equalsIgnoreCase("nidn")) ? "nim" : prop;
				val = metaMhs.getPropertyValue(mhs, actualProp, EntityMode.POJO);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:858");}
			String valStr = (val == null || val.toString().trim().isEmpty()) ? "-" : val.toString();
			if (valStr.equals("-") && prop.equalsIgnoreCase("jabatan")) valStr = "Mahasiswa";
			gabungNilaiMap(mapData, "daftar.mahasiswa." + prop, valStr);
			gabungNilaiMap(mapData, "pengikut.pegawai." + prop, valStr);
			gabungNilaiMap(mapData, "pengikut.semua." + prop, valStr);
		}
	}

	private static void prosesPengikutSiswa(Siswa siswa, ClassMetadata metaSiswa, Set<String> propsAll, Map<String, String> mapData) {
		if (metaSiswa == null) return;
		for (String prop : propsAll) {
			Object val = null;
			try {
				String actualProp = (prop.equalsIgnoreCase("kode") || prop.equalsIgnoreCase("nidn")) ? "nomorInduk" : prop;
				val = metaSiswa.getPropertyValue(siswa, actualProp, EntityMode.POJO);
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/surat/util/SuratUtilHelper.java:874");}
			String valStr = (val == null || val.toString().trim().isEmpty()) ? "-" : val.toString();
			if (valStr.equals("-") && prop.equalsIgnoreCase("jabatan")) valStr = "Siswa";
			gabungNilaiMap(mapData, "daftar.siswa." + prop, valStr);
			gabungNilaiMap(mapData, "pengikut.pegawai." + prop, valStr);
			gabungNilaiMap(mapData, "pengikut.semua." + prop, valStr);
		}
	}
}