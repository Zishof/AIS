package ais.action.master.sekolah.psb.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Box;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.PSBAction;
import ais.action.master.pmb.BiodataCalonMahasiswaAction;
import ais.action.master.sekolah.CalonSiswaAction;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.psb.ParameterTambahanPsbListener;
import ais.action.master.sekolah.psb.VerifikasiMatapelajaranPSBHelper;
import ais.action.master.sekolah.psb.VerifikasiPSBHelper;
import ais.action.master.sekolah.psb.VerifikasiParameterPSBHelper;
import ais.action.master.sekolah.psb.nis.DefaultNisGenerator;
import ais.action.master.sekolah.psb.nis.NisGenerator;
import ais.common.Common;
import ais.common.CommonPSB;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.Keluarga;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.PaketPsb;
import ais.database.model.sekolah.PaketPsbPunyaGelombangPendaftaranPsb;
import ais.database.model.sekolah.PenjurusanSekolah;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupConfig;
import ais.ui.util.MyLabelAgakKecilBoldBiru;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyMessageboxConfig;

public class PPDB3 extends PPDB {

	private static final long serialVersionUID = 1L;

	// A. Data Calon Siswa
	private Textbox nama;
	private Intbox anakKe;
	private Intbox dariAnakKe;
	private Textbox tempatLahir;
	private MyDatebox tanggalLahir;
	private Combobox statusDalamKeluarga;
	private Textbox sekolahAsal;
	private Textbox masukKeKelas;
	private Textbox infoMempunyaiSaudaraKandung;

	// B.1 Ayah
	private Textbox namaAyah;
	private Textbox tempatLahirAyah;
	private MyDatebox tanggalLahirAyah;
	private Textbox pendidikanAyahTeks;
	private Textbox pekerjaanAyahTeks;
	private Textbox instansiAyah;
	private Textbox alamatAyahTeks;
	private Textbox hp1ayah;
	private Combobox penghasilanAyahTeks;

	// B.2 Ibu
	private Textbox namaIbu;
	private Textbox tempatLahirIbu;
	private MyDatebox tanggalLahirIbu;
	private Textbox pendidikanIbuTeks;
	private Textbox pekerjaanIbuTeks;
	private Textbox instansiIbu;
	private Textbox alamatIbuTeks;
	private Textbox hp1ibu;
	private Combobox penghasilanIbuTeks;

	// Hubungan Ortu-Anak
	private Combobox hubOrtu1;
	private Combobox hubOrtu2;
	private Combobox hubOrtu3;
	private Combobox hubOrtu4;

	// Data Perkembangan A - Riwayat Kelahiran
	private Textbox perkA1;
	private Textbox perkA2;
	private Textbox perkA3;
	private Textbox perkA4;
	private Textbox perkA5;
	private Textbox perkA6;
	private Textbox perkA7;
	private Textbox perkA8;

	// Data Perkembangan B - Masa Balita
	private Textbox perkB1;
	private Textbox perkB2;
	private Textbox perkB3;
	private Textbox perkB4;
	private Textbox perkB5;

	// Data Perkembangan C - Perkembangan Fisik (Motorik)
	private Textbox perkC1;
	private Textbox perkC2;
	private Textbox perkC3;
	private Textbox perkC4;
	private Textbox perkC5;
	private Textbox perkC6;
	private Textbox perkC7;
	private Textbox perkC8;
	private Textbox perkC9;
	private Textbox perkC10;
	private Textbox perkC11;
	private Textbox perkC12;
	private Textbox perkC13;
	private Textbox perkC14;
	private Textbox perkC15;
	private Textbox perkC16;
	private Textbox perkC17;
	private Textbox perkC18;
	private Textbox perkC19;
	private Textbox perkC20;
	private Textbox perkC21;
	private Textbox perkC22;
	private Textbox perkC23;
	private Textbox perkC24;

	// Data Perkembangan D - Sosial
	private Textbox perkD1;
	private Textbox perkD2;
	private Textbox perkD3;
	private Textbox perkD4;
	private Textbox perkD5;
	private Textbox perkD6;

	// Data Perkembangan E - Bermain
	private Textbox perkE1;
	private Textbox perkE2;
	private Textbox perkE3;
	private Textbox perkE4;
	private Textbox perkE5;

	// Data Perkembangan F - Kebiasaan Sehari-hari
	private Textbox perkF1;
	private Textbox perkF2;
	private Textbox perkF3;
	private Textbox perkF4;
	private Textbox perkF5;
	private Textbox perkF6;
	private Textbox perkF7;
	private Textbox perkF8;
	private Textbox perkF9;

	// Data Perkembangan G - Pendidikan
	private Textbox perkG1;
	private Textbox perkG2;
	private Textbox perkG3;
	private Textbox perkG4;
	private Textbox perkG5;
	private Textbox perkG6;
	private Textbox perkG7;
	private Textbox perkG8;
	private Textbox perkG9;

	private MyCheckboxConfig pernyataan;

	protected boolean baru;

	private Row rowParameterTambahan;
	private ArrayList<Row> parameterRows;
	private ParameterTambahanPsbListener parameterTambahanListener;
	private Rows subRowsVerifikasiKelengkapanCalonSiswa;
	private Rows subRowsVerifikasiNilaiRapor;
	private List<Rows> subRowsVerifikasiNilaiParameter;
	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
	protected TreeSet<Long> hapusKelasLesSiswa = null;
	protected TreeSet<Long> selectedKelasLesSiswa;
	private Combobox paketPsb;
	private Box infoKampusDariMana;
	private Textbox keteranganInfoKampusDariMana;
	private Textbox namaTemanInfoKampusDariMana;
	private MyCheckboxConfig merupakanPindahan;
	private Textbox pindahanDariSekolah;
	private Textbox alamatSekolahPindahan;
	private Textbox keteranganPindah;
	private MyDatebox tanggalPindah;
	private Textbox kelasSekolahPindahan;
	private AmbilDataSiswaBanbox siswaAlumni;
	private Combobox keluarga;
	private FotoCalonSiswa fotoCalonSiswa;
	private Vbox vboxfotoCalonSiswa;
	private Combobox penjurusanSekolah;

	private EventListener checkKesamaan = new EventListener() {
		@Override
		public void onEvent(Event arg0) throws Exception {
			CalonSiswaAction.CheckKesamaan checkKesamaan = new CalonSiswaAction.CheckKesamaan(calonSiswa,
					gelombangPendaftaranPsb, tanggalLahir, nama, namaIbu, PPDB3.this, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							CalonSiswa calonSiswa1 = (CalonSiswa) arg0.getData();
							PPDB window = new PPDB3();
							window.setCalonSiswa(calonSiswa1);
							window.setGelombangPendaftaranPsb(gelombangPendaftaranPsb);
							window.setEventListener(eventListener);
							window.init();
							ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
							window.setHeight("95%");
							window.setWidth("750px");
							window.setVisible(true);
							window.onModal();
						}
					});
			checkKesamaan.onEvent(arg0);
		}
	};

	public PPDB3() {
		super();
	}

	public PPDB3(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb,
			EventListener eventListener) {
		super(calonSiswa, gelombangPendaftaranPsb, eventListener);
		init();
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	public void init() {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("40%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		try {
			if ((calonSiswa != null && calonSiswa.getGelombangPendaftaranPsb() != null)) {
				GelombangPendaftaranPsb gel = calonSiswa.getGelombangPendaftaranPsb();
				LampiranLain kop = LampiranLain.ambil(gel.getId(), LampiranLain.KOP_GELOMBANG_PSB);
				if (kop != null && kop.getId() != null) {
					Image image = new Image(kop.createLinkUri());
					image.setWidth("100%");
					MyFormRow rowUtama1 = new MyFormRow();
					rowUtama1.setSclass("headerHbox");
					rowUtama1.appendChild(image);
					ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
					rowUtama1.setValign("top");
					rowUtama1.setParent(rows);
				} else {
					Hbox hbox = PSBAction.headerBox();
					MyFormRow rowUtama1 = new MyFormRow();
					rowUtama1.setSclass("headerHbox");
					rowUtama1.appendChild(hbox);
					ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
					rowUtama1.setValign("top");
					rowUtama1.setParent(rows);
				}
			} else {
				Hbox hbox = PSBAction.headerBox();
				MyFormRow rowUtama1 = new MyFormRow();
				rowUtama1.setSclass("headerHbox");
				rowUtama1.appendChild(hbox);
				ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
				rowUtama1.setValign("top");
				rowUtama1.setParent(rows);
			}
		} catch (Exception e) {
			Hbox hbox = PSBAction.headerBox();
			MyFormRow rowUtama1 = new MyFormRow();
			rowUtama1.setSclass("headerHbox");
			rowUtama1.appendChild(hbox);
			ais.ui.util.ZkCompat.setSpans(rowUtama1, "2");
			rowUtama1.setValign("top");
			rowUtama1.setParent(rows);
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:init-header");
		}

		CalonSiswaAction.initBg(center, gelombangPendaftaranPsb);

		final Tbmuser tbmuser = Common.getCurrentUser();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelAgakKecilBoldBiru(
				Common.getBahasaConfig("FORMULIR WAWANCARA ORANG TUA") + " - "
						+ gelombangPendaftaranPsb.getSekolah().getNama().toUpperCase()
						+ Common.getBahasaConfig(" TAHUN PELAJARAN ") + gelombangPendaftaranPsb.getTahunAjaran()));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new Html(gelombangPendaftaranPsb.getInformasi().replaceAll("\n", "<br>")));

		final LampiranLain lampiranLain = LampiranLain.ambil(gelombangPendaftaranPsb.getId(), "INFO_PPDB");
		if (lampiranLain != null && lampiranLain.getId() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			ais.ui.util.ZkCompat.setSpans(row, "2");
			A a = new A(lampiranLain.getNama());
			a.setParent(row);
			a.setWidth("95%");
			a.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.display(lampiranLain);
				}
			});
		}

		// ===== A. DATA CALON SISWA =====

		org.json.JSONObject gJson = new org.json.JSONObject();
		try {
			String fg = calonSiswa.getFieldsGeneric();
			if (fg != null && !fg.trim().isEmpty()) {
				gJson = new org.json.JSONObject(fg);
			}
		} catch (Exception e) {
			// kosong / rusak - pakai objek kosong
		}
		final org.json.JSONObject fg = gJson;

		MyGroupConfig myRowStyled = new MyGroupConfig("A. DATA CALON SISWA");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("1. Nama Lengkap Calon Siswa"));
		row.appendChild(nama = new Textbox(calonSiswa.getNamaSiswa() == null ? "" : calonSiswa.getNamaSiswa()));
		nama.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("2. Anak Ke- / Dari"));
		Hbox hboxAnakKe = new Hbox();
		anakKe = new Intbox(calonSiswa.getAnakKe() == null ? 1 : calonSiswa.getAnakKe());
		anakKe.setWidth("60px");
		anakKe.setParent(hboxAnakKe);
		Label lblDari = new Label(" dari ");
		lblDari.setParent(hboxAnakKe);
		dariAnakKe = new Intbox(calonSiswa.getDariAnakKe() == null ? 1 : calonSiswa.getDariAnakKe());
		dariAnakKe.setWidth("60px");
		dariAnakKe.setParent(hboxAnakKe);
		row.appendChild(hboxAnakKe);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("3. Tempat Lahir"));
		row.appendChild(tempatLahir = new Textbox(
				calonSiswa.getTempatLahir() == null ? "" : calonSiswa.getTempatLahir()));
		tempatLahir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("4. Tanggal Lahir"));
		row.appendChild(tanggalLahir = new MyDatebox());
		tanggalLahir.setValue(calonSiswa.getTanggalLahir());
		tanggalLahir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("5. Status Dalam Keluarga"));
		row.appendChild(statusDalamKeluarga = new Combobox());
		statusDalamKeluarga.setWidth("90%");
		statusDalamKeluarga.setReadonly(true);
		for (String s : new String[]{"Kandung", "Angkat", "Tiri"}) {
			MyComboitemConfig ci = new MyComboitemConfig(s);
			ci.setValue(s);
			ci.setParent(statusDalamKeluarga);
			if (s.equals(calonSiswa.getStatusDalamKeluarga())) {
				statusDalamKeluarga.setSelectedItem(ci);
			}
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("6. Asal Sekolah Sebelumnya"));
		row.appendChild(sekolahAsal = new Textbox(
				calonSiswa.getSekolahAsal() == null ? "" : calonSiswa.getSekolahAsal()));
		sekolahAsal.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("7. Masuk / Pindah ke Kelas"));
		row.appendChild(masukKeKelas = new Textbox(fg.optString("masukKeKelas", "")));
		masukKeKelas.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("8. Nama Saudara Kandung & Kelasnya"));
		row.appendChild(infoMempunyaiSaudaraKandung = new Textbox(
				calonSiswa.getInfoMempunyaiSaudaraKandung() == null ? ""
						: calonSiswa.getInfoMempunyaiSaudaraKandung()));
		infoMempunyaiSaudaraKandung.setWidth("90%");
		infoMempunyaiSaudaraKandung.setRows(2);

		// Foto Calon Siswa
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Foto Calon Siswa"));
		fotoCalonSiswa = null;
		Vbox vbox = new Vbox();
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(row);
		vboxfotoCalonSiswa = vbox;
		try {
			Common.createDownloadUploadFoto(vbox, calonSiswa, FotoCalonSiswa.class, new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					fotoCalonSiswa = (FotoCalonSiswa) arg0.getData();
				}
			}, true);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:foto");
		}

		// ===== B.1 DATA AYAH =====

		myRowStyled = new MyGroupConfig("B.1 DATA AYAH");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("9. Nama Ayah"));
		row.appendChild(namaAyah = new Textbox(
				calonSiswa.getNamaAyah() == null ? "" : calonSiswa.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("10. Tempat Lahir Ayah"));
		row.appendChild(tempatLahirAyah = new Textbox(
				calonSiswa.getTempatLahirAyah() == null ? "" : calonSiswa.getTempatLahirAyah()));
		tempatLahirAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("11. Tanggal Lahir Ayah"));
		row.appendChild(tanggalLahirAyah = new MyDatebox());
		tanggalLahirAyah.setValue(calonSiswa.getTanggalLahirAyah());
		tanggalLahirAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("12. Pendidikan Terakhir Ayah"));
		row.appendChild(pendidikanAyahTeks = new Textbox(fg.optString("pendidikanAyah", "")));
		pendidikanAyahTeks.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("13. Pekerjaan Ayah"));
		row.appendChild(pekerjaanAyahTeks = new Textbox(fg.optString("pekerjaanAyah", "")));
		pekerjaanAyahTeks.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("14. Nama Instansi / Kantor Ayah"));
		row.appendChild(instansiAyah = new Textbox(fg.optString("instansiAyah", "")));
		instansiAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("15. Alamat Kantor / Tempat Kerja Ayah"));
		row.appendChild(alamatAyahTeks = new Textbox(
				calonSiswa.getAlamatAyah() == null ? "" : calonSiswa.getAlamatAyah()));
		alamatAyahTeks.setWidth("90%");
		alamatAyahTeks.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("16. No. HP / Telepon Ayah"));
		row.appendChild(hp1ayah = new Textbox(
				calonSiswa.getHp1ayah() == null ? "" : calonSiswa.getHp1ayah()));
		hp1ayah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("17. Penghasilan Rata-rata Ayah"));
		row.appendChild(penghasilanAyahTeks = new Combobox());
		penghasilanAyahTeks.setWidth("90%");
		penghasilanAyahTeks.setReadonly(true);
		String curPenghasilanAyah = fg.optString("penghasilanAyah", "");
		for (String s : new String[]{
				"< Rp 1.000.000",
				"Rp 1.000.000 - Rp 2.000.000",
				"Rp 2.000.000 - Rp 3.000.000",
				"Rp 3.000.000 - Rp 5.000.000",
				"> Rp 5.000.000"}) {
			MyComboitemConfig ci = new MyComboitemConfig(s);
			ci.setValue(s);
			ci.setParent(penghasilanAyahTeks);
			if (s.equals(curPenghasilanAyah)) {
				penghasilanAyahTeks.setSelectedItem(ci);
			}
		}

		// ===== B.2 DATA IBU =====

		myRowStyled = new MyGroupConfig("B.2 DATA IBU");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("18. Nama Ibu"));
		row.appendChild(namaIbu = new Textbox(
				calonSiswa.getNamaIbu() == null ? "" : calonSiswa.getNamaIbu()));
		namaIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("19. Tempat Lahir Ibu"));
		row.appendChild(tempatLahirIbu = new Textbox(
				calonSiswa.getTempatLahirIbu() == null ? "" : calonSiswa.getTempatLahirIbu()));
		tempatLahirIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("20. Tanggal Lahir Ibu"));
		row.appendChild(tanggalLahirIbu = new MyDatebox());
		tanggalLahirIbu.setValue(calonSiswa.getTanggalLahirIbu());
		tanggalLahirIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("21. Pendidikan Terakhir Ibu"));
		row.appendChild(pendidikanIbuTeks = new Textbox(fg.optString("pendidikanIbu", "")));
		pendidikanIbuTeks.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("22. Pekerjaan Ibu"));
		row.appendChild(pekerjaanIbuTeks = new Textbox(fg.optString("pekerjaanIbu", "")));
		pekerjaanIbuTeks.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("23. Nama Instansi / Kantor Ibu"));
		row.appendChild(instansiIbu = new Textbox(fg.optString("instansiIbu", "")));
		instansiIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("24. Alamat Kantor / Tempat Kerja Ibu"));
		row.appendChild(alamatIbuTeks = new Textbox(
				calonSiswa.getAlamatIbu() == null ? "" : calonSiswa.getAlamatIbu()));
		alamatIbuTeks.setWidth("90%");
		alamatIbuTeks.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("25. No. HP / Telepon Ibu"));
		row.appendChild(hp1ibu = new Textbox(
				calonSiswa.getHp1ibu() == null ? "" : calonSiswa.getHp1ibu()));
		hp1ibu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("26. Penghasilan Rata-rata Ibu"));
		row.appendChild(penghasilanIbuTeks = new Combobox());
		penghasilanIbuTeks.setWidth("90%");
		penghasilanIbuTeks.setReadonly(true);
		String curPenghasilanIbu = fg.optString("penghasilanIbu", "");
		for (String s : new String[]{
				"< Rp 1.000.000",
				"Rp 1.000.000 - Rp 2.000.000",
				"Rp 2.000.000 - Rp 3.000.000",
				"Rp 3.000.000 - Rp 5.000.000",
				"> Rp 5.000.000"}) {
			MyComboitemConfig ci = new MyComboitemConfig(s);
			ci.setValue(s);
			ci.setParent(penghasilanIbuTeks);
			if (s.equals(curPenghasilanIbu)) {
				penghasilanIbuTeks.setSelectedItem(ci);
			}
		}

		// ===== C. HUBUNGAN ORANG TUA DAN ANAK =====

		myRowStyled = new MyGroupConfig("C. HUBUNGAN ORANG TUA DAN ANAK");
		myRowStyled.setParent(rows);

		String[] yaTidak = new String[]{"Ya", "Tidak"};

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("27. Apakah ayah/ibu sering membacakan cerita atau dongeng kepada anak?"));
		row.appendChild(hubOrtu1 = buatComboYaTidak(yaTidak, fg.optString("hubOrtu1", "")));
		hubOrtu1.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("28. Apakah ayah/ibu sering bermain bersama anak di rumah?"));
		row.appendChild(hubOrtu2 = buatComboYaTidak(yaTidak, fg.optString("hubOrtu2", "")));
		hubOrtu2.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("29. Apakah anak lebih banyak menghabiskan waktu bersama pengasuh/pembantu?"));
		row.appendChild(hubOrtu3 = buatComboYaTidak(yaTidak, fg.optString("hubOrtu3", "")));
		hubOrtu3.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("30. Apakah anak memiliki kebiasaan tidur bersama orang tua?"));
		row.appendChild(hubOrtu4 = buatComboYaTidak(yaTidak, fg.optString("hubOrtu4", "")));
		hubOrtu4.setWidth("90%");

		// ===== D. DATA PERKEMBANGAN A: RIWAYAT KELAHIRAN =====

		myRowStyled = new MyGroupConfig("D. DATA PERKEMBANGAN ANAK - A. Riwayat Kelahiran");
		myRowStyled.setParent(rows);

		perkA1 = tambahRowTeks(rows, "A1. Proses Kelahiran (Normal/Caesar/Vakum/dll)", fg.optString("perkA1", ""));
		perkA2 = tambahRowTeks(rows, "A2. Usia Kehamilan Saat Lahir (bulan)", fg.optString("perkA2", ""));
		perkA3 = tambahRowTeks(rows, "A3. Berat Badan Lahir (kg)", fg.optString("perkA3", ""));
		perkA4 = tambahRowTeks(rows, "A4. Panjang/Tinggi Badan Saat Lahir (cm)", fg.optString("perkA4", ""));
		perkA5 = tambahRowTeks(rows, "A5. Apakah Ada Masalah Saat Kelahiran? (keterangan)", fg.optString("perkA5", ""));
		perkA6 = tambahRowTeks(rows, "A6. Apakah Anak Diberi ASI? (berapa lama)", fg.optString("perkA6", ""));
		perkA7 = tambahRowTeks(rows, "A7. Awal Pemberian Makanan Tambahan (usia)", fg.optString("perkA7", ""));
		perkA8 = tambahRowTeks(rows, "A8. Keterangan Lain Riwayat Kelahiran", fg.optString("perkA8", ""));
		perkA8.setRows(2);

		// ===== DATA PERKEMBANGAN B: MASA BALITA =====

		myRowStyled = new MyGroupConfig("D. DATA PERKEMBANGAN ANAK - B. Masa Balita");
		myRowStyled.setParent(rows);

		perkB1 = tambahRowTeks(rows, "B1. Usia Pertama Kali Duduk Mandiri", fg.optString("perkB1", ""));
		perkB2 = tambahRowTeks(rows, "B2. Usia Pertama Kali Berdiri", fg.optString("perkB2", ""));
		perkB3 = tambahRowTeks(rows, "B3. Usia Pertama Kali Berjalan", fg.optString("perkB3", ""));
		perkB4 = tambahRowTeks(rows, "B4. Usia Pertama Kali Berbicara (kata bermakna)", fg.optString("perkB4", ""));
		perkB5 = tambahRowTeks(rows, "B5. Keterangan Lain Masa Balita", fg.optString("perkB5", ""));
		perkB5.setRows(2);

		// ===== DATA PERKEMBANGAN C: PERKEMBANGAN FISIK / MOTORIK =====

		myRowStyled = new MyGroupConfig("D. DATA PERKEMBANGAN ANAK - C. Perkembangan Fisik & Motorik");
		myRowStyled.setParent(rows);

		perkC1  = tambahRowTeks(rows, "C1.  Kemampuan memegang benda kecil dengan jari", fg.optString("perkC1", ""));
		perkC2  = tambahRowTeks(rows, "C2.  Kemampuan menggambar/mewarnai", fg.optString("perkC2", ""));
		perkC3  = tambahRowTeks(rows, "C3.  Kemampuan menggunting kertas", fg.optString("perkC3", ""));
		perkC4  = tambahRowTeks(rows, "C4.  Kemampuan membuka/menutup baju sendiri", fg.optString("perkC4", ""));
		perkC5  = tambahRowTeks(rows, "C5.  Kemampuan makan sendiri dengan sendok/garpu", fg.optString("perkC5", ""));
		perkC6  = tambahRowTeks(rows, "C6.  Kemampuan menulis/meniru huruf", fg.optString("perkC6", ""));
		perkC7  = tambahRowTeks(rows, "C7.  Kemampuan berjalan (stabil/tidak)", fg.optString("perkC7", ""));
		perkC8  = tambahRowTeks(rows, "C8.  Kemampuan berlari", fg.optString("perkC8", ""));
		perkC9  = tambahRowTeks(rows, "C9.  Kemampuan melompat", fg.optString("perkC9", ""));
		perkC10 = tambahRowTeks(rows, "C10. Kemampuan memanjat", fg.optString("perkC10", ""));
		perkC11 = tambahRowTeks(rows, "C11. Kemampuan menendang bola", fg.optString("perkC11", ""));
		perkC12 = tambahRowTeks(rows, "C12. Kemampuan melempar dan menangkap bola", fg.optString("perkC12", ""));
		perkC13 = tambahRowTeks(rows, "C13. Kemampuan naik-turun tangga", fg.optString("perkC13", ""));
		perkC14 = tambahRowTeks(rows, "C14. Kemampuan mengendarai sepeda roda tiga/dua", fg.optString("perkC14", ""));
		perkC15 = tambahRowTeks(rows, "C15. Koordinasi mata dan tangan", fg.optString("perkC15", ""));
		perkC16 = tambahRowTeks(rows, "C16. Keseimbangan tubuh secara umum", fg.optString("perkC16", ""));
		perkC17 = tambahRowTeks(rows, "C17. Penyakit yang pernah diderita (misal: campak, asma)", fg.optString("perkC17", ""));
		perkC17.setRows(2);
		perkC18 = tambahRowTeks(rows, "C18. Alergi (makanan, obat, atau lainnya)", fg.optString("perkC18", ""));
		perkC19 = tambahRowTeks(rows, "C19. Riwayat operasi atau rawat inap", fg.optString("perkC19", ""));
		perkC20 = tambahRowTeks(rows, "C20. Kondisi penglihatan", fg.optString("perkC20", ""));
		perkC21 = tambahRowTeks(rows, "C21. Kondisi pendengaran", fg.optString("perkC21", ""));
		perkC22 = tambahRowTeks(rows, "C22. Kebutuhan khusus / keterbatasan fisik (jika ada)", fg.optString("perkC22", ""));
		perkC23 = tambahRowTeks(rows, "C23. Tinggi dan berat badan saat ini", fg.optString("perkC23", ""));
		perkC24 = tambahRowTeks(rows, "C24. Keterangan lain kondisi fisik", fg.optString("perkC24", ""));
		perkC24.setRows(2);

		// ===== DATA PERKEMBANGAN D: SOSIAL =====

		myRowStyled = new MyGroupConfig("D. DATA PERKEMBANGAN ANAK - D. Perkembangan Sosial");
		myRowStyled.setParent(rows);

		perkD1 = tambahRowTeks(rows, "D1. Hubungan dengan saudara kandung", fg.optString("perkD1", ""));
		perkD2 = tambahRowTeks(rows, "D2. Hubungan dengan teman sebaya", fg.optString("perkD2", ""));
		perkD3 = tambahRowTeks(rows, "D3. Hubungan dengan orang tua / wali", fg.optString("perkD3", ""));
		perkD4 = tambahRowTeks(rows, "D4. Sikap anak terhadap orang yang baru dikenal", fg.optString("perkD4", ""));
		perkD5 = tambahRowTeks(rows, "D5. Hobi / minat anak", fg.optString("perkD5", ""));
		perkD6 = tambahRowTeks(rows, "D6. Keterangan lain perkembangan sosial", fg.optString("perkD6", ""));
		perkD6.setRows(2);

		// ===== DATA PERKEMBANGAN E: BERMAIN =====

		myRowStyled = new MyGroupConfig("D. DATA PERKEMBANGAN ANAK - E. Kegiatan Bermain");
		myRowStyled.setParent(rows);

		perkE1 = tambahRowTeks(rows, "E1. Jenis permainan favorit anak", fg.optString("perkE1", ""));
		perkE2 = tambahRowTeks(rows, "E2. Apakah lebih suka bermain sendiri atau bersama teman?", fg.optString("perkE2", ""));
		perkE3 = tambahRowTeks(rows, "E3. Berapa jam waktu bermain dalam sehari?", fg.optString("perkE3", ""));
		perkE4 = tambahRowTeks(rows, "E4. Media bermain yang digunakan (gadget, outdoor, dll)", fg.optString("perkE4", ""));
		perkE5 = tambahRowTeks(rows, "E5. Keterangan lain kegiatan bermain", fg.optString("perkE5", ""));
		perkE5.setRows(2);

		// ===== DATA PERKEMBANGAN F: KEBIASAAN SEHARI-HARI =====

		myRowStyled = new MyGroupConfig("D. DATA PERKEMBANGAN ANAK - F. Kebiasaan Sehari-hari");
		myRowStyled.setParent(rows);

		perkF1 = tambahRowTeks(rows, "F1. Jam tidur malam (pukul berapa biasanya?)", fg.optString("perkF1", ""));
		perkF2 = tambahRowTeks(rows, "F2. Jam bangun pagi (pukul berapa biasanya?)", fg.optString("perkF2", ""));
		perkF3 = tambahRowTeks(rows, "F3. Pola makan (3x sehari/pilih-pilih/dll)", fg.optString("perkF3", ""));
		perkF4 = tambahRowTeks(rows, "F4. Frekuensi mandi per hari", fg.optString("perkF4", ""));
		perkF5 = tambahRowTeks(rows, "F5. Kebiasaan belajar di rumah (frekuensi, durasi)", fg.optString("perkF5", ""));
		perkF6 = tambahRowTeks(rows, "F6. Durasi menonton TV / penggunaan gadget per hari", fg.optString("perkF6", ""));
		perkF7 = tambahRowTeks(rows, "F7. Kegiatan ekstrakurikuler / les yang diikuti", fg.optString("perkF7", ""));
		perkF8 = tambahRowTeks(rows, "F8. Kebiasaan ibadah / keagamaan di rumah", fg.optString("perkF8", ""));
		perkF9 = tambahRowTeks(rows, "F9. Keterangan lain kebiasaan sehari-hari", fg.optString("perkF9", ""));
		perkF9.setRows(2);

		// ===== DATA PERKEMBANGAN G: PENDIDIKAN =====

		myRowStyled = new MyGroupConfig("D. DATA PERKEMBANGAN ANAK - G. Pendidikan");
		myRowStyled.setParent(rows);

		perkG1 = tambahRowTeks(rows, "G1. Pengalaman pendidikan anak sebelumnya (TK/KB/dll)", fg.optString("perkG1", ""));
		perkG2 = tambahRowTeks(rows, "G2. Mata pelajaran / kegiatan yang paling disukai anak", fg.optString("perkG2", ""));
		perkG3 = tambahRowTeks(rows, "G3. Prestasi atau kemampuan yang menonjol", fg.optString("perkG3", ""));
		perkG4 = tambahRowTeks(rows, "G4. Kesulitan belajar yang dialami anak (jika ada)", fg.optString("perkG4", ""));
		perkG5 = tambahRowTeks(rows, "G5. Harapan orang tua terhadap perkembangan anak", fg.optString("perkG5", ""));
		perkG5.setRows(2);
		perkG6 = tambahRowTeks(rows, "G6. Harapan anak sendiri (jika dapat menyampaikan)", fg.optString("perkG6", ""));
		perkG7 = tambahRowTeks(rows, "G7. Motivasi/alasan mendaftar ke sekolah ini", fg.optString("perkG7", ""));
		perkG7.setRows(2);
		perkG8 = tambahRowTeks(rows, "G8. Mengetahui informasi sekolah ini dari mana?", fg.optString("perkG8", ""));
		perkG9 = tambahRowTeks(rows, "G9. Keterangan lain", fg.optString("perkG9", ""));
		perkG9.setRows(2);

		// Parameter Tambahan
		rowParameterTambahan = new MyFormRow();
		rowParameterTambahan.setVisible(false);
		rowParameterTambahan.setStyle("border:0px;background: transparent;");
		rowParameterTambahan.setParent(rows);
		rowParameterTambahan.appendChild(new MyLabelBolder("Form Tambahan"));

		parameterRows = new ArrayList<Row>();

		parameterTambahanListener = new ParameterTambahanPsbListener(calonSiswa, parameterRows, lampiranLains,
				gelombangPendaftaranPsb, false, rows);

		/*
		 * Verifikasi Kelengkapan Berkas ikut tampil pada PENDAFTARAN BARU supaya calon
		 * siswa bisa mengunggah berkas sekaligus. Penautan berkas ke baris penghubung
		 * diselesaikan VerifikasiPSBHelper.simpanVerifikasi() setelah data tersimpan.
		 */
		try {
			subRowsVerifikasiKelengkapanCalonSiswa = VerifikasiPSBHelper.tampilkanVerifikasi(calonSiswa, rows, null,
					calonSiswa.getId() != null ? calonSiswa.getGelombangPendaftaranPsb() : gelombangPendaftaranPsb);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PPDB3.tampilkanVerifikasiBerkas");
		}

		if (calonSiswa.getId() != null) {
			try {
				subRowsVerifikasiNilaiRapor = VerifikasiMatapelajaranPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
						gelombangPendaftaranPsb, null);
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:verif2");
			}
			try {
				subRowsVerifikasiNilaiParameter = VerifikasiParameterPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
						null, calonSiswa.getGelombangPendaftaranPsb());
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:verif3");
			}
		}

		try {
			parameterTambahanListener.onEvent(null);
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:param");
		}

		// Info Pindahan
		try {
			Component[] cc = CalonSiswaAction.infoPindahan(rows, calonSiswa);
			merupakanPindahan = (MyCheckboxConfig) cc[0];
			pindahanDariSekolah = (Textbox) cc[1];
			alamatSekolahPindahan = (Textbox) cc[2];
			keteranganPindah = (Textbox) cc[3];
			tanggalPindah = (MyDatebox) cc[4];
			kelasSekolahPindahan = (Textbox) cc[5];
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:infoPindahan");
		}

		// Info Dari Mana
		try {
			Component[] cs = CalonSiswaAction.infoDariMana(rows, calonSiswa);
			infoKampusDariMana = (Box) cs[0];
			namaTemanInfoKampusDariMana = (Textbox) cs[1];
			keteranganInfoKampusDariMana = (Textbox) cs[2];
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:infoDariMana");
		}

		// Pernyataan
		pernyataan = Common.tambahKeteranganRowHtml(rows,
				"Dengan ini saya menyatakan bahwa data yang saya berikan dalam formulir wawancara ini benar adanya. "
						+ "Apabila dikemudian hari ditemukan kesalahan data, saya bersedia bertanggung jawab atas segala konsekuensinya.");
		pernyataan.setChecked(calonSiswa.getPernyataan());

		if (calonSiswa.getId() != null) {
			pernyataan.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					Session session = HibernateUtil.currentSession();
					session.refresh(calonSiswa);
					calonSiswa.setPernyataan(pernyataan.isChecked());
					Common.refreshUpdate(session, calonSiswa);
					session.flush();
				}
			});
		}

		// South: tombol BATAL / SIMPAN
		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		toolbar.setAlign("center");
		toolbar.setParent(south);

		MyButtonConfig cancel = new MyButtonConfig("B A T A L", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				PPDB3.this.detach();
			}
		});
		cancel.setParent(toolbar);

		final boolean daftar = tbmuser == null;
		final MyButtonConfig save = new MyButtonConfig(daftar ? "D A F T A R" : "S I M P A N   D A T A",
				"/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				baru = false;
				if (onSave(event)) {
					PPDB3.this.detach();
					if (PPDB3.this.eventListener != null) {
						PPDB3.this.eventListener.onEvent(new Event("", save, PPDB3.this.calonSiswa));
					}
					if (baru) {
						String informasi = Common.getKonfigurasi("informasi_registrasi_psb_berhasil_login",
								"Proses pendaftaran peserta didik baru berhasil dilakukan dengan nomor pendaftaran : [no_reg]. Silahkan catat nomor pendaftaran tersebut dan selanjutnya akan diarahkan ke Login.")
								.getNilai();
						informasi = org.apache.commons.lang3.StringUtils.replace(informasi, "[no_reg]",
								PPDB3.this.calonSiswa.getNoRegistrasi());
						MyMessageboxConfig.show(informasi, "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {
									@Override
									public void onEvent(Event arg0) throws Exception {
										Common.createDefaultTimer(new EventListener() {
											@Override
											public void onEvent(Event arg0) throws Exception {
												if (baru && PPDB3.this.calonSiswa.getGelombangPendaftaranPsb()
														.getOtomatisLoginSetelahDaftar()) {
													PPDB3.this.calonSiswa.setTelahLogin(true);
													PPDB3.this.calonSiswa
															.setWaktuLogin(ais.ui.util.WaktuUtil.getDate());
													Common.refreshUpdate(PPDB3.this.calonSiswa);
													Common.setLogin(PPDB3.this.calonSiswa);
													Sessions.getCurrent(true).setAttribute("cetak", true);
													Common.createDefaultTimer(new EventListener() {
														@Override
														public void onEvent(Event arg0) throws Exception {
															Executions.getCurrent().sendRedirect("");
														}
													});
												} else {
													CalonSiswaAction.onCetakKartu(PPDB3.this.calonSiswa, daftar);
												}
											}
										});
									}
								});
					}
				}
			}
		});
		save.setParent(toolbar);

		Common.masukkanListener(rows, masukkanPerubahan);
	}

	private Combobox buatComboYaTidak(String[] pilihan, String nilai) {
		Combobox cb = new Combobox();
		cb.setReadonly(true);
		for (String s : pilihan) {
			MyComboitemConfig ci = new MyComboitemConfig(s);
			ci.setValue(s);
			ci.setParent(cb);
			if (s.equals(nilai)) {
				cb.setSelectedItem(ci);
			}
		}
		return cb;
	}

	private Textbox tambahRowTeks(Rows rows, String label, String nilai) {
		MyFormRow row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(label));
		Textbox tb = new Textbox(nilai == null ? "" : nilai);
		tb.setWidth("90%");
		row.appendChild(tb);
		return tb;
	}

	public boolean check() throws Exception {
		if (nama.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Nama Calon Siswa harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Calon Siswa; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							nama.focus();
							Clients.scrollIntoView(nama);
						}
					});
			return false;
		}

		if (tempatLahir.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Tempat Lahir harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tempat Lahir; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							tempatLahir.focus();
							Clients.scrollIntoView(tempatLahir);
						}
					});
			return false;
		}

		if (tanggalLahir.getValue() == null) {
			MyMessageboxConfig.show(
					"Tanggal Lahir harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tanggal Lahir; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							tanggalLahir.focus();
							Clients.scrollIntoView(tanggalLahir);
						}
					});
			return false;
		}

		if (namaAyah.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Nama Ayah harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Ayah; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							namaAyah.focus();
							Clients.scrollIntoView(namaAyah);
						}
					});
			return false;
		}

		if (hp1ayah.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"No. HP/Telepon Ayah harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom HP Ayah; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							hp1ayah.focus();
							Clients.scrollIntoView(hp1ayah);
						}
					});
			return false;
		}

		if (namaIbu.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"Nama Ibu harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Ibu; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							namaIbu.focus();
							Clients.scrollIntoView(namaIbu);
						}
					});
			return false;
		}

		if (hp1ibu.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show(
					"No. HP/Telepon Ibu harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom HP Ibu; (2) pastikan data telah benar; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							hp1ibu.focus();
							Clients.scrollIntoView(hp1ibu);
						}
					});
			return false;
		}

		if (!pernyataan.isChecked()) {
			MyMessageboxConfig.show(
					"Pernyataan persetujuan harus dicentang. Langkah yang dapat dilakukan: (1) baca pernyataan yang tersedia; (2) centang kolom pernyataan; (3) simpan kembali formulir.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							pernyataan.focus();
							Clients.scrollIntoView(pernyataan);
						}
					});
			return false;
		}

		if (!CalonSiswaAction.checkInfoDariMana(infoKampusDariMana, namaTemanInfoKampusDariMana,
				keteranganInfoKampusDariMana)) {
			return false;
		}

		return true;
	}

	private EventListener masukkanPerubahan = new EventListener() {
		@Override
		public void onEvent(Event arg0) throws Exception {
			setdata();
			if (calonSiswa.getId() != null) {
				Common.refreshUpdate(calonSiswa);
			}
		}
	};

	public boolean onSave(Event event) throws Exception {
		if (!check()) {
			return false;
		}

		Session session = HibernateUtil.currentSession();

		if (calonSiswa.getId() != null && calonSiswa.getId() > 0L) {
			calonSiswa = (CalonSiswa) session.load(CalonSiswa.class, calonSiswa.getId());
		}
		if (calonSiswa.getId() != null && calonSiswa.getId() < 0L) {
			calonSiswa.setId(null);
		}

		setdata();

		baru = false;
		if (calonSiswa.getId() == null) {
			session.save(calonSiswa);
			baru = true;
		} else {
			Common.refreshSaveOrUpdate(session, calonSiswa);
		}

		if (fotoCalonSiswa != null) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.refresh(fotoCalonSiswa);
			fotoCalonSiswa.setCalonSiswa(calonSiswa.getId());
			streamingSession.getTransaction().begin();
			streamingSession.update(fotoCalonSiswa);
			streamingSession.getTransaction().commit();
			StreamingHibernateUtil.getInstance().closeSession();
		}

		/*
		 * Simpan penghubung berkas verifikasi sekaligus menautkan berkas yang diunggah
		 * sebelum calon siswa punya id (penautan tertunda).
		 */
		VerifikasiPSBHelper.simpanVerifikasi(calonSiswa, subRowsVerifikasiKelengkapanCalonSiswa);

		Common.hapusSession(CalonSiswa.class);

		return true;
	}

	private void setdata() {
		try {
			String info = BiodataCalonMahasiswaAction.infoMahasiswaBaru(infoKampusDariMana);
			Siswa alumni = (Siswa) (siswaAlumni == null ? null : siswaAlumni.getAttribute("siswa"));

			calonSiswa.setGelombangPendaftaranPsb(gelombangPendaftaranPsb);
			calonSiswa.setNamaSiswa(nama.getValue());
			calonSiswa.setAnakKe(anakKe.getValue());
			calonSiswa.setDariAnakKe(dariAnakKe.getValue());
			calonSiswa.setTempatLahir(tempatLahir.getValue());
			calonSiswa.setTanggalLahir(tanggalLahir.getValue());
			calonSiswa.setStatusDalamKeluarga(
					statusDalamKeluarga.getSelectedItem() == null ? null
							: (String) statusDalamKeluarga.getSelectedItem().getValue());
			calonSiswa.setSekolahAsal(sekolahAsal.getValue());
			calonSiswa.setInfoMempunyaiSaudaraKandung(infoMempunyaiSaudaraKandung.getValue());

			calonSiswa.setNamaAyah(namaAyah.getValue());
			calonSiswa.setTempatLahirAyah(tempatLahirAyah.getValue());
			calonSiswa.setTanggalLahirAyah(tanggalLahirAyah.getValue());
			calonSiswa.setAlamatAyah(alamatAyahTeks.getValue());
			calonSiswa.setHp1ayah(hp1ayah.getValue());
			calonSiswa.setWaAyah(hp1ayah.getValue());

			calonSiswa.setNamaIbu(namaIbu.getValue());
			calonSiswa.setTempatLahirIbu(tempatLahirIbu.getValue());
			calonSiswa.setTanggalLahirIbu(tanggalLahirIbu.getValue());
			calonSiswa.setAlamatIbu(alamatIbuTeks.getValue());
			calonSiswa.setHp1ibu(hp1ibu.getValue());
			calonSiswa.setWaIbu(hp1ibu.getValue());

			calonSiswa.setInfoKampusDariMana(info);
			calonSiswa.setKeteranganInfoKampusDariMana(
					keteranganInfoKampusDariMana == null ? "" : keteranganInfoKampusDariMana.getValue());
			calonSiswa.setNamaTemanInfoKampusDariMana(
					namaTemanInfoKampusDariMana == null ? "" : namaTemanInfoKampusDariMana.getValue());

			if (merupakanPindahan != null) {
				calonSiswa.setMerupakanPindahan(merupakanPindahan.isChecked());
				calonSiswa.setPindahanDariSekolah(pindahanDariSekolah.getValue());
				calonSiswa.setAlamatSekolahPindahan(alamatSekolahPindahan.getValue());
				calonSiswa.setKeteranganPindah(keteranganPindah.getValue());
				calonSiswa.setTanggalPindah(tanggalPindah.getValue());
				calonSiswa.setKelasSekolahPindahan(kelasSekolahPindahan.getValue());
			}

			calonSiswa.setPernyataan(pernyataan.isChecked());

			if (calonSiswa.getId() == null) {
				calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
			}

			parameterTambahanListener.onSave(calonSiswa);

			if (calonSiswa.getGelombangPendaftaranPsb() != null
					&& calonSiswa.getGelombangPendaftaranPsb().getHanyaUntukAnakPegawai()
					&& calonSiswa.getOrangTuaPegawai() == null) {
				Tbmuser tbmuserCur = Common.getCurrentUser();
				if (tbmuserCur != null && tbmuserCur.getPegawai() != null) {
					calonSiswa.setOrangTuaPegawai(tbmuserCur.getPegawai());
				}
			}

			if (alumni != null && alumni.getId() != null) {
				Common.copyDataJikaKosong(alumni, calonSiswa, Siswa.class, CalonSiswa.class);
			}

			// Simpan fieldsGeneric sebagai JSON
			calonSiswa.setFieldsGeneric(buildFieldsGenericJson());

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:setdata");
		}
	}

	private String buildFieldsGenericJson() {
		try {
			org.json.JSONObject obj = new org.json.JSONObject();

			obj.put("masukKeKelas", masukKeKelas.getValue());

			// Data Ayah (fieldsGeneric)
			obj.put("pendidikanAyah", pendidikanAyahTeks.getValue());
			obj.put("pekerjaanAyah", pekerjaanAyahTeks.getValue());
			obj.put("instansiAyah", instansiAyah.getValue());
			obj.put("penghasilanAyah", penghasilanAyahTeks.getSelectedItem() == null ? ""
					: (String) penghasilanAyahTeks.getSelectedItem().getValue());

			// Data Ibu (fieldsGeneric)
			obj.put("pendidikanIbu", pendidikanIbuTeks.getValue());
			obj.put("pekerjaanIbu", pekerjaanIbuTeks.getValue());
			obj.put("instansiIbu", instansiIbu.getValue());
			obj.put("penghasilanIbu", penghasilanIbuTeks.getSelectedItem() == null ? ""
					: (String) penghasilanIbuTeks.getSelectedItem().getValue());

			// Hubungan Ortu-Anak
			obj.put("hubOrtu1", hubOrtu1.getSelectedItem() == null ? "" : (String) hubOrtu1.getSelectedItem().getValue());
			obj.put("hubOrtu2", hubOrtu2.getSelectedItem() == null ? "" : (String) hubOrtu2.getSelectedItem().getValue());
			obj.put("hubOrtu3", hubOrtu3.getSelectedItem() == null ? "" : (String) hubOrtu3.getSelectedItem().getValue());
			obj.put("hubOrtu4", hubOrtu4.getSelectedItem() == null ? "" : (String) hubOrtu4.getSelectedItem().getValue());

			// Perkembangan A
			obj.put("perkA1", perkA1.getValue()); obj.put("perkA2", perkA2.getValue());
			obj.put("perkA3", perkA3.getValue()); obj.put("perkA4", perkA4.getValue());
			obj.put("perkA5", perkA5.getValue()); obj.put("perkA6", perkA6.getValue());
			obj.put("perkA7", perkA7.getValue()); obj.put("perkA8", perkA8.getValue());

			// Perkembangan B
			obj.put("perkB1", perkB1.getValue()); obj.put("perkB2", perkB2.getValue());
			obj.put("perkB3", perkB3.getValue()); obj.put("perkB4", perkB4.getValue());
			obj.put("perkB5", perkB5.getValue());

			// Perkembangan C
			obj.put("perkC1",  perkC1.getValue());  obj.put("perkC2",  perkC2.getValue());
			obj.put("perkC3",  perkC3.getValue());  obj.put("perkC4",  perkC4.getValue());
			obj.put("perkC5",  perkC5.getValue());  obj.put("perkC6",  perkC6.getValue());
			obj.put("perkC7",  perkC7.getValue());  obj.put("perkC8",  perkC8.getValue());
			obj.put("perkC9",  perkC9.getValue());  obj.put("perkC10", perkC10.getValue());
			obj.put("perkC11", perkC11.getValue()); obj.put("perkC12", perkC12.getValue());
			obj.put("perkC13", perkC13.getValue()); obj.put("perkC14", perkC14.getValue());
			obj.put("perkC15", perkC15.getValue()); obj.put("perkC16", perkC16.getValue());
			obj.put("perkC17", perkC17.getValue()); obj.put("perkC18", perkC18.getValue());
			obj.put("perkC19", perkC19.getValue()); obj.put("perkC20", perkC20.getValue());
			obj.put("perkC21", perkC21.getValue()); obj.put("perkC22", perkC22.getValue());
			obj.put("perkC23", perkC23.getValue()); obj.put("perkC24", perkC24.getValue());

			// Perkembangan D
			obj.put("perkD1", perkD1.getValue()); obj.put("perkD2", perkD2.getValue());
			obj.put("perkD3", perkD3.getValue()); obj.put("perkD4", perkD4.getValue());
			obj.put("perkD5", perkD5.getValue()); obj.put("perkD6", perkD6.getValue());

			// Perkembangan E
			obj.put("perkE1", perkE1.getValue()); obj.put("perkE2", perkE2.getValue());
			obj.put("perkE3", perkE3.getValue()); obj.put("perkE4", perkE4.getValue());
			obj.put("perkE5", perkE5.getValue());

			// Perkembangan F
			obj.put("perkF1", perkF1.getValue()); obj.put("perkF2", perkF2.getValue());
			obj.put("perkF3", perkF3.getValue()); obj.put("perkF4", perkF4.getValue());
			obj.put("perkF5", perkF5.getValue()); obj.put("perkF6", perkF6.getValue());
			obj.put("perkF7", perkF7.getValue()); obj.put("perkF8", perkF8.getValue());
			obj.put("perkF9", perkF9.getValue());

			// Perkembangan G
			obj.put("perkG1", perkG1.getValue()); obj.put("perkG2", perkG2.getValue());
			obj.put("perkG3", perkG3.getValue()); obj.put("perkG4", perkG4.getValue());
			obj.put("perkG5", perkG5.getValue()); obj.put("perkG6", perkG6.getValue());
			obj.put("perkG7", perkG7.getValue()); obj.put("perkG8", perkG8.getValue());
			obj.put("perkG9", perkG9.getValue());

			return obj.toString();
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB3.java:buildJson");
			return "{}";
		}
	}
}
