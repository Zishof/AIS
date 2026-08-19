package ais.action.master.epsbed;


import ais.common.CommonSearchFilterHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URLEncoder;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFSheet;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Beasiswa;
import ais.database.model.BiodataMahasiswa;
import ais.database.model.Fakultas;
import ais.database.model.JenisPenerimaBeasiswa;
import ais.database.model.Jurusan;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.beasiswa.MahasiswaDaftarBeasiswa;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class TransaksiBeasiswaBidikmisi extends MyWindow {

	private static final long serialVersionUID = 790038368339375113L;

	private Center center = new Center();
	private Combobox tahunakademik = new Combobox();
	private Combobox jenisSemester = new Combobox();
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private File file;
	
	// Parameter debug untuk tracing error (aktifkan/matikan dari sini)
	private boolean debug = true;

	public TransaksiBeasiswaBidikmisi() {
		super();
		try {
			init();
		} catch (Exception e) {
			if(debug) e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/epsbed/TransaksiBeasiswaBidikmisi.java:70");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	public TransaksiBeasiswaBidikmisi(String title, String border, boolean closable) {
		super(title, border, closable);
		try {
			init();
		} catch (Exception e) {
			if(debug) e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/epsbed/TransaksiBeasiswaBidikmisi.java:80");
			Common.tampilErrorJikaAdmin(e);
		}
	}

	private void init() throws Exception {

		Common.generateTahunAjaran(tahunakademik);
		tahunakademik.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				// Event disiapkan jika butuh reload otomatis
			}
		});
		
		jenisSemester = new Combobox();
		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setValue(Perkuliahan.GANJIL);
		comboitem.setLabel(Perkuliahan.GANJIL);
		jenisSemester.appendChild(comboitem);
		
		comboitem = new MyComboitemConfig();
		comboitem.setValue(Perkuliahan.GENAP);
		comboitem.setLabel(Perkuliahan.GENAP);
		jenisSemester.appendChild(comboitem);
		Common.selectComboItem(jenisSemester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		
		jenisSemester.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
			}
		});

		searchfakultas = new Combobox();
		searchjurusan = new Combobox();
		Common.insertCombo(searchfakultas, new String[] { "nama", "kode" }, Fakultas.class, Restrictions.eq("aktif", true));

		searchfakultas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(searchjurusan);
				Common.insertCombo(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
						Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
						CommonSearchFilterHelper.eqSelectedWithId("fakultas", searchfakultas, false));
			}
		});
		
		searchjurusan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser.ambilFakultas() != null) {
			Common.selectComboItem(searchfakultas, tbmuser.ambilFakultas());
			if (tbmuser.ambilJurusan() != null) {
				Common.selectComboItem(searchjurusan, tbmuser.ambilJurusan());
			}
		}

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		// FIX toolbar/tombol tidak tampil: pada ZK5 region North memakai tinggi bawaan
		// (+-100px); dengan flex=true isinya diregangkan ke tinggi tersebut sehingga
		// Toolbar yang diletakkan DI BAWAH grid filter ikut terpotong. Disamakan dengan
		// layar sejenis yang sudah benar (DownloadMahasiswa, DownloadKrs, DownloadNilai):
		// flex dimatikan + tinggi eksplisit. Autoscroll sebagai pengaman bila isi bertambah.
		ais.ui.util.ZkCompat.setFlex(north, false);
		north.setHeight("160px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(grid);

		Row row = new Row();
		row.setValign("top");
		row.setParent(rows);
		
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(tahunakademik);
		tahunakademik.setWidth("90%");
		tahunakademik.setReadonly(true);
		
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(jenisSemester);
		jenisSemester.setWidth("90%");
		jenisSemester.setReadonly(true);
		
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");

		row.appendChild(new ais.ui.util.MyLabelConfig("Program Studi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");

		Toolbar toolbar = new Toolbar();
		toolbar.setParent(div);

		MyToolbarbuttonConfig search = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		search.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
		search.setParent(toolbar);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Export Epsbed (Beasiswa.xls)", "/img/excel.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				try {
					if (file != null && file.exists()) {
						Filedownload.save(new FileInputStream(file), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Beasiswa.xlsx");
					} else {
						MyMessageboxConfig.show("File belum di-generate atau sedang diproses. Silakan refresh terlebih dahulu.");
					}
				} catch (Exception e) {
					if(debug) e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/epsbed/TransaksiBeasiswaBidikmisi.java:209");
				}
			}
		});
		print.setParent(toolbar);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		initSpreadsheet();
	}

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {

		if (tahunakademik.getSelectedItem() == null) {
			MyMessageboxConfig.show("Tahun Akademik harus diisi");
			return;
		}

		if (jenisSemester.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Semester harus diisi");
			return;
		}

		Common.clear(center);
		
		final Jurusan jurusanPilihan = searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null ? null
				: (Jurusan) searchjurusan.getSelectedItem().getValue();
				
		final Fakultas fakultasPilihan = searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null ? null
				: (Fakultas) searchfakultas.getSelectedItem().getValue();

		final String thnAkademikVal = tahunakademik.getSelectedItem().getValue().toString();
		final String smstrVal = jenisSemester.getSelectedItem().getValue().toString();

		final String filename = Sessions.getCurrent().getWebApp().getRealPath("/tmp/data_beasiswa_"
				+ URLEncoder.encode(Common.datetimeFormat2s.get().format(ais.ui.util.WaktuUtil.getDate()), "UTF-8") + ".xlsx");

		file = new File(filename);
		file.createNewFile();
		
		final Intbox sizedata = new Intbox(30);
		final Label label = Common.displayLoadBar(this, file, center, sizedata);

		new Thread(new Runnable() {
			@Override
			public void run() {
				
				Session session = null;
				XSSFWorkbook workbook = null;
				
				try {
					// 1. Inisialisasi Workspace Excel
					workbook = new XSSFWorkbook();
					XSSFSheet sheet = workbook.createSheet("DATA");
					sheet.setDefaultColumnWidth(20);

					XSSFRow rowhead = sheet.createRow((short) 0);
					String[] headers = {
							"NPM", "KDPTI", "JENIS_BEASISWA", "NAMA BEASISWA", "COUNTER", "NAMA_MHS", "JK",
							"KODE_PRODI", "ID_JENJANG", "SMT", "IPK", "KODE_PEKERJAAN", "JML_TANGGUNGAN",
							"PENGHASILAN", "PRESTASI", "MULAI_BULAN", "SELESAI_BULAN", "TAHUN",
							"KETERANGAN", "ALAMAT", "TELEPON"
					};
					
					for (int i = 0; i < headers.length; i++) {
						rowhead.createCell(i).setCellValue(headers[i]);
					}

					// 2. Buka Session Aman
					session = HibernateUtil.getSessionFactory().openSession();
					
					// 3. Ambil Data
					org.hibernate.Criteria criteria = session.createCriteria(MahasiswaDaftarBeasiswa.class)
							.add(Restrictions.eq("terima", MahasiswaDaftarBeasiswa.DITERIMA))
							.createAlias("beasiswa", "beasiswa")
							.createAlias("mahasiswa", "mahasiswa")
							.createAlias("mahasiswa.jurusan", "jurusan")
							.add(Restrictions.eq("beasiswa.semester", smstrVal))
							.add(Restrictions.eq("beasiswa.tahunAkademik", thnAkademikVal));

					if (jurusanPilihan != null) {
						criteria.add(Restrictions.eq("mahasiswa.jurusan", jurusanPilihan));
					}
					if (fakultasPilihan != null) {
						criteria.add(Restrictions.eq("jurusan.fakultas", fakultasPilihan));
					}

					List<MahasiswaDaftarBeasiswa> listData = criteria.list();
					int totalData = listData.size();

					// 4. Mulai Looping Pemrosesan (Dengan Optimasi Memori)
					int rowIndex = 1;
					for (int i = 0; i < totalData; i++) {
						MahasiswaDaftarBeasiswa mdb = listData.get(i);
						if (mdb == null) continue;

						Mahasiswa mahasiswa = mdb.getMahasiswa();
						Beasiswa beasiswa = mdb.getBeasiswa();
						
						if (mahasiswa == null || beasiswa == null) continue; // Safety check

						// Mengambil kode PT dengan pengamanan Null Pointer
						String kodePT = "";
						Jurusan jrs = mahasiswa.getJurusan();
						if (jrs != null && jrs.getFakultas() != null) {
							PerguruanTinggi pt = jrs.getFakultas().getPerguruanTinggi();
							if (pt != null && pt.getKodePerguruanTinggi() != null) {
								kodePT = pt.getKodePerguruanTinggi();
							}
						}

						// Jenis Beasiswa & Nama Beasiswa
						String kodeJenis = "";
						JenisPenerimaBeasiswa jpb = beasiswa.getJenisPenerimaBeasiswa();
						if (jpb != null && jpb.getKode() != null) {
							kodeJenis = jpb.getKode();
						}
						
						String namaBeasiswa = beasiswa.getNama() == null ? "" : beasiswa.getNama();

						label.setValue("Sedang memproses data " + mdb.toString() + " ("
								+ Common.numberFormat.get().format(rowIndex * 100.0 / totalData) + " %)");

						XSSFRow row = sheet.createRow(rowIndex);
						
						// C0: NPM
						row.createCell(0).setCellValue(mahasiswa.getNim() == null ? "" : mahasiswa.getNim());
						// C1: KDPTI
						row.createCell(1).setCellValue(kodePT);
						// C2: JENIS_BEASISWA
						row.createCell(2).setCellValue(kodeJenis);
						// C3: NAMA BEASISWA (KOLOM BARU)
						row.createCell(3).setCellValue(namaBeasiswa);
						// C4: COUNTER
						row.createCell(4).setCellValue(rowIndex);
						// C5: NAMA_MHS
						row.createCell(5).setCellValue(mahasiswa.getNama() == null ? "" : mahasiswa.getNama());
						
						// C6: JK
						int jkVal = 1; // Default Laki-laki
						if (mahasiswa.getKelamin() != null && mahasiswa.getKelamin().equalsIgnoreCase("Perempuan")) {
							jkVal = 2;
						}
						row.createCell(6).setCellValue(jkVal);

						// C7: KODE_PRODI & C8: ID_JENJANG
						String kodeEpsbed = "";
						String kodeJenjang = "";
						if (jrs != null) {
							kodeEpsbed = jrs.getKodeEpsbed() == null ? "" : jrs.getKodeEpsbed();
							if (jrs.getJenjang() != null && jrs.getJenjang().getKode() != null) {
								kodeJenjang = jrs.getJenjang().getKode();
							}
						}
						row.createCell(7).setCellValue(kodeEpsbed);
						row.createCell(8).setCellValue(kodeJenjang);

						// C9: SMT & C10: IPK
						Integer smt = Common.getSemester(mahasiswa.getTahunangkatan(), beasiswa.getTahunAkademik(),
								beasiswa.getSemester(), mahasiswa.getPindahKeKampusIniMasukSemester(),
								mahasiswa.getSemesterMulai());
						row.createCell(9).setCellValue(smt == null ? 0 : smt);
						
						String ipkStr = "0.00";
						try {
							KrsMahasiswa krs = Common.singkronkanKrsMahasiswa(mahasiswa, smt, null, null);
							if (krs != null && krs.getIpk() != null) {
								ipkStr = Common.numberFormat.get().format(krs.getIpk());
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiBeasiswaBidikmisi.java:381");}
						row.createCell(10).setCellValue(ipkStr);

						// C11 - C14: Default Kosong
						row.createCell(11).setCellValue("");
						row.createCell(12).setCellValue("");
						row.createCell(13).setCellValue("");
						row.createCell(14).setCellValue("");

						// C15: MULAI_BULAN & C16: SELESAI_BULAN
						row.createCell(15).setCellValue(beasiswa.getTanggalBuka() == null ? ""
								: Common.databaseDateFormat.get().format(beasiswa.getTanggalBuka()));
						row.createCell(16).setCellValue(beasiswa.getTanggalTutup() == null ? ""
								: Common.databaseDateFormat.get().format(beasiswa.getTanggalTutup()));
						
						// C17: TAHUN
						row.createCell(17).setCellValue(beasiswa.getTahun() == null ? "" : beasiswa.getTahun().toString());
						// C18: KETERANGAN
						row.createCell(18).setCellValue(mdb.getKeterangan() == null ? "" : mdb.getKeterangan());
						// C19: ALAMAT
						row.createCell(19).setCellValue(mahasiswa.getAlamat() == null ? "" : mahasiswa.getAlamat());
						
						// C20: TELEPON
						String telp = "";
						try {
							BiodataMahasiswa bio = mahasiswa.ambilBiodata();
							if (bio != null) {
								String hp = bio.getHp();
								String tlpRumah = bio.getTeleponRumah();
								
								boolean hpValid = hp != null && !hp.trim().isEmpty() && !hp.trim().equals("08100000000000000000") && !hp.trim().equals("0000000000");
								boolean rmhValid = tlpRumah != null && !tlpRumah.trim().isEmpty() && !tlpRumah.trim().equals("00000000000000000000") && !tlpRumah.trim().equals("000000000");
								
								if (hpValid) telp += hp.trim();
								if (hpValid && rmhValid) telp += " / ";
								if (rmhValid) telp += tlpRumah.trim();
							}
							if (telp.isEmpty() && mahasiswa.getTelp() != null) {
								telp = mahasiswa.getTelp();
							}
						} catch (Exception e) {
							telp = mahasiswa.getTelp() == null ? "" : mahasiswa.getTelp();
						}
						row.createCell(20).setCellValue(telp);

						rowIndex++;

						// OPTIMASI MEMORI: Flush & Clear Hibernate Cache tiap 100 baris agar RAM tidak meledak
						if (rowIndex % 100 == 0) {
							session.flush();
							session.clear();
						}
					}

					// 5. Simpan ke File Excel
					Common.setStyled(sheet);
					sizedata.setValue(rowIndex + 1);

					FileOutputStream fileOut = new FileOutputStream(file);
					workbook.write(fileOut);
					fileOut.close();

					if(debug) System.out.println("Your excel file has been successfully generated: " + file.getAbsolutePath());
					
					// Clear collections to help GC
					listData.clear();

				} catch (Exception e) {
					if(debug) e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/epsbed/TransaksiBeasiswaBidikmisi.java:449");
					Common.tampilErrorJikaAdmin(e);
				} finally {
					// 6. GARANSI PENUTUPAN KONEKSI DATABASE
					if (session != null) {
						try {
							session.clear();
							session.disconnect();
							session.close();
						} catch (Exception ex) {
							if(debug) ex.printStackTrace(); ais.common.ErrorAuditUtil.record(ex, "auto-audit src/ais/action/master/epsbed/TransaksiBeasiswaBidikmisi.java:459");
						}
					}
					
					// Update UI Component di akhir
					try {
						label.setValue("");
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/epsbed/TransaksiBeasiswaBidikmisi.java:466");}
				}

			}
		}).start();

	}
}