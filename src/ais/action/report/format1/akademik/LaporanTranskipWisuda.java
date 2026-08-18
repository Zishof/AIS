package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Wisuda;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanTranskipWisuda extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;

	private Combobox fakultas;
	private Combobox jurusan;
	// private Combobox program;
	//
	// private Combobox semester;
	private Combobox wisuda;
	// private Intbox angkatan;

	private Center center;

	private Toolbar toolbar;

	// private Label myTahunAngkatan;

	private Wisuda selectedWisuda;

	private MyCheckboxConfig tampilkanHanyaYangSudahDisetujui;

	public LaporanTranskipWisuda() {
		super();
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transkip Wisuda", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTranskipWisuda(Wisuda wisuda) {
		super();
		this.selectedWisuda = wisuda;
		try {
			initKHS();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Transkip Wisuda", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanTranskipWisuda(String title, String border, boolean closable)
			throws Exception {
		super(title, border, closable);
		initKHS();
		init();
	}

	private void initKHS() throws Exception {
		Common.initFakultasDanJurusan(fakultas = new Combobox(),
				jurusan = new Combobox(), null, null);
		Common.insertCombo(wisuda = new Combobox(), "wisudaKe", "moto",
				Wisuda.class);
		wisuda.setReadonly(true);
		// semester = new Combobox();
		// for (int i = 1; i <= 21; i++) {
		// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		// comboitem.setLabel(i + "");
		// comboitem.setValue(i);
		// semester.appendChild(comboitem);
		// }
		// Common.selectComboItem(semester, 1);

		// angkatan = new Intbox(ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR));

	}

	private void init() throws Exception {

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				onKHS(event);

			}
		};

		// program = new Combobox();
		// for (String strProgram : Common.programs.keySet()) {
		// org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		// comboitem.setLabel(strProgram);
		// comboitem.setValue(strProgram);
		// // program.appendChild(comboitem);
		// // if (strProgram.equals("Reguler------")) {
		// // program.setSelectedItem(comboitem);
		// // }
		// }
		// Common.checkProgramString(program);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("30%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", eventListener);

		// row = new MyFormRow();
		//		// row.setParent(rows);
		// row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		// row.appendChild(program);
		// program.setWidth("90%");
		// program.addEventListener("onChange", eventListener);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Wisuda ke"));
		row.appendChild(wisuda);
		wisuda.addEventListener("onChange", eventListener);

		if (selectedWisuda != null) {
			Common.selectComboItem(wisuda, selectedWisuda);
			wisuda.setDisabled(true);
		}
		
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tampilkan hanya yang sudah di setujui"));
		row.appendChild(tampilkanHanyaYangSudahDisetujui = new MyCheckboxConfig());
		tampilkanHanyaYangSudahDisetujui.addEventListener("onClick",
				eventListener);

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(
				new ParameterListener() {

					@SuppressWarnings({ "unchecked", "rawtypes" })
					@Override
					public Map<String, Serializable> generateParameters()
							throws Exception {

						// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
						// MyMessageboxConfig.show(
						// "Pilih "
						// + "Fakultas",
						// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						// return null;
						// }
						//
						// if (jurusan.getSelectedItem() == null||jurusan.getSelectedItem().getValue() == null) {
						// MyMessageboxConfig.show(
						// "Pilih "
						// + Common.getBahasaConfig("Jurusan"),
						// "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						// return null;
						// }

						if (wisuda.getSelectedItem() == null) {
							MyMessageboxConfig.show("Mohon maaf, data Wisuda belum dipilih. Langkah yang dapat dilakukan: (1) Pilih periode wisuda dari daftar dropdown; (2) Pastikan data wisuda sudah tersedia di sistem; (3) Ulangi proses cetak transkrip wisuda. Jika masih mengalami kendala, hubungi Administrator atau tim teknis.", "Peringatan", MyMessageboxConfig.OK,
									MyMessageboxConfig.INFORMATION);
							return null;
						}

						Map parameters = generateParameter();
						return parameters;
					}
				}, "transkrip_wisuda", null, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onKHS(arg0);

					}
				}));

		onKHS(null);

	}

	// @SuppressWarnings({ "unchecked", "rawtypes" })
	// public List generateDataDanImageAlbum() throws Exception {
	//
	// // if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
	// // return null;
	// // }
	// //
	// // if (jurusan.getSelectedItem() == null||jurusan.getSelectedItem().getValue() == null) {
	// // return null;
	// // }
	//
	// if (wisuda.getSelectedItem() == null) {
	// return null;
	// }
	// //
	// // if (tahunAkademik.getValue() == null) {
	// // return null;
	// // }
	//
	// // if (wisuda.getValue().equals("")) {
	// // return null;
	// // }
	//
	// // String tahunAkademik = (String) this.tahunAkademik.getSelectedItem()
	// // .getValue();
	// // Integer tahunAngkatan = Integer.parseInt(myTahunAngkatan.getValue());
	// // Integer semester = (Integer)
	// // this.semester.getSelectedItem().getValue();
	//
	// // String myprogram = (String) (program.getSelectedItem() == null||program.getSelectedItem().getValue() == null ? ""
	// // : program.getSelectedItem().getValue());
	// Wisuda wisuda = (Wisuda) this.wisuda.getSelectedItem().getValue();
	//
	// Session session = HibernateUtil.currentSession();
	//
	// List<PendaftaranWisuda> mahasiswas = session
	// .createCriteria(PendaftaranWisuda.class)
	// .add(Restrictions.eq("wisuda", wisuda))
	// .createCriteria("mahasiswa")
	//
	// .createAlias("jurusan", "jurusan")
	// .createAlias("jurusan.fakultas", "fakultas")
	// .addOrder(Order.asc("fakultas.nama"))
	// .addOrder(Order.asc("jurusan.nama"))
	// .addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"))
	// .add(jurusan.getSelectedItem() == null || jurusan.getSelectedItem().getValue()==null ? Restrictions
	// .sqlRestriction("1=1") : Restrictions.eq("jurusan",
	// jurusan.getSelectedItem().getValue()))
	// .add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue()==null ? Restrictions
	// .sqlRestriction("1=1") : Restrictions.eq(
	// "jurusan.fakultas", fakultas.getSelectedItem()
	// .getValue()))
	// .add(myprogram.equals("") ? Restrictions.sqlRestriction("1=1")
	// : Restrictions.eq("program", myprogram))
	// .addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim")).list();
	//
	// // create directory
	// String strDirectoy = Sessions.getCurrent().getWebApp()
	// .getRealPath("/tmp");
	// (new File(strDirectoy)).mkdir();
	//
	// List list = new ArrayList();
	//
	// try {
	// Session streamingSession = StreamingHibernateUtil.getInstance()
	// .currentSession();
	//
	// for (PendaftaranWisuda pendaftaranWisuda : mahasiswas) {
	// Mahasiswa mahasiswa = pendaftaranWisuda.getMahasiswa();
	//
	// Map map = new java.util.HashMap();
	// map.put("telp", mahasiswa.getTelp());
	// map.put("alamat", mahasiswa.getAlamat());
	// map.put("nim", mahasiswa.getNim());
	// map.put("nama", mahasiswa.getNama());
	// map.put("jurusan", mahasiswa.getJurusan() == null ? ""
	// : mahasiswa.getJurusan().getNama());
	// map.put("program_studi", mahasiswa.getJurusan() == null ? ""
	// : mahasiswa.getJurusan().getNama());
	// map.put("fakultas", mahasiswa.getJurusan() == null
	// || mahasiswa.getJurusan().getFakultas() == null ? ""
	// : mahasiswa.getJurusan().getFakultas().getNama());
	// map.put("tahunAngkatan", mahasiswa.getTahunangkatan() + "");
	// // map.put("tahunAkademik", tahunAkademik);
	// // map.put("semester", semester + "");
	// map.put("ipk", Common.hitungIPK(mahasiswa));
	// map.put("judul_skripsi",
	// pendaftaranWisuda.getSkripsi() == null ? ""
	// : pendaftaranWisuda.getSkripsi().getJudul());
	// // Skripsi skripsi = session.createCriteria(Skripsi)
	//
	// FotoMahasiswa fotobm = (FotoMahasiswa) streamingSession
	// .createCriteria(FotoMahasiswa.class).addOrder(Order.desc("id"))
	// .add(Restrictions.eq("mahasiswa", mahasiswa.getId()))
	// .setMaxResults(1).uniqueResult();
	//
	// // Blob content = fotobm == null ? null : fotobm.getFoto();
	//
	// try {
	//
	// // File blobFile = new File(strDirectoy
	// // + "/"
	// // + (content == null ? "administrator-icon_default.png"
	// // : (fotobm.getId() + "_" + fotobm.getNama())));
	// map.put("foto", Common.getFileFoto(fotobm)
	// .getAbsolutePath());
	//
	// // System.out.println("foto = " + blobFile);
	// //
	// // if (!blobFile.exists() && content != null) {
	// // FileOutputStream outStream = new
	// // FileOutputStream(blobFile);
	// // InputStream inStream = content.// getBinaryStream();
	// //
	// // int length = -1;
	// // int size = 4096;
	// // byte[] buffer = new byte[size];
	// //
	// // while ((length = inStream.read(buffer)) != -1) {
	// // outStream.write(buffer, 0, length);
	// // outStream.flush();
	// // }
	// //
	// // inStream.close();
	// // outStream.close();
	// // }
	// } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/report/format1/akademik/LaporanTranskipWisuda.java:370");
	// Common.tampilErrorJikaAdmin(e); 
	// // System.out
	// // .println("ERROR(djv_exportBlob) Unable to export: "
	// // + fotobm.getNama());
	// }
	//
	// list.add(map);
	// }
	// StreamingHibernateUtil.getInstance().closeSession();
	// } catch (Exception e) {
	// StreamingHibernateUtil.getInstance().rollbackTransaction();
	// Common.tampilErrorJikaAdmin(e); 
	// }
	// return list;
	// }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		// if (fakultas.getSelectedItem() == null||fakultas.getSelectedItem().getValue() == null) {
		// // MyMessageboxConfig.show("Pilih " + "Fakultas",
		// // "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return null;
		// }
		//
		// if (jurusan.getSelectedItem() == null||jurusan.getSelectedItem().getValue() == null) {
		// // MyMessageboxConfig.show("Pilih " + Common.getBahasaConfig("Jurusan"),
		// // "Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
		// return null;
		// }

		if (wisuda.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih semester", "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Wisuda wisuda = (Wisuda) this.wisuda.getSelectedItem().getValue();
		Fakultas fakultas = (Fakultas) (this.fakultas.getSelectedItem() == null || this.fakultas.getSelectedItem().getValue()==null ? null
				: this.fakultas.getSelectedItem().getValue());

		Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null || this.jurusan.getSelectedItem().getValue()==null ? null
				: this.jurusan.getSelectedItem().getValue());

		// if (tahunAkademik.getValue() == null) {
		// return null;
		// }

		// Jurusan jurusan = (Jurusan) (this.jurusan.getSelectedItem() == null || this.jurusan.getSelectedItem().getValue()==null ?
		// null
		// : this.jurusan.getSelectedItem().getValue());

		final Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("wisuda", wisuda == null || wisuda.getId() == null ? -1L : wisuda.getId());
		parameters.put("fakultas", fakultas == null || fakultas.getId() == null ? -1L : fakultas.getId());
		parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		parameters.put("tampilkanHanyaYangSudahDisetujui",
				tampilkanHanyaYangSudahDisetujui.isChecked() ? 1L : 0L);
		// parameters.put("jurusan", jurusan == null || jurusan.getId() == null ? -1L : jurusan.getId());
		// parameters.put("semester", semester.getSelectedItem().getValue());
		// parameters.put("tahunangkatan", angkatan.getValue() == null ? -1
		// : angkatan.getValue());

		// parameters.put("maps", generateDataDanImageAlbum());
		// parameters.put("program", program.getSelectedItem() == null||program.getSelectedItem().getValue() == null ? "-1"
		// : program.getSelectedItem().getValue());

		// parameters.put("tanggal", tanggal.getValue()==null?ais.ui.util.WaktuUtil.getDate():tanggal.getValue());
		parameters.put("kaprodi",
				"(                                          )");
		parameters.put("nip", "");

		return parameters;
	}

	@SuppressWarnings({ })
	public void onKHS(Event event) throws Exception {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF,
					generateParameter(), "transkrip_wisuda", ais.ui.util.WaktuUtil.getDate(),
							toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e); 
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Transkip Wisuda", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
