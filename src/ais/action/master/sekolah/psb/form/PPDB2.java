package ais.action.master.sekolah.psb.form;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.joda.time.Years;
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
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.maintenance.PSBAction;
import ais.action.master.helper.AmbilDataKecamatanBanbox;
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
import ais.database.model.Agama;
import ais.database.model.Kota;
import ais.database.model.Pegawai;
import ais.database.model.Pekerjaan;
import ais.database.model.Propinsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wilayah;
import ais.database.model.employ.Keluarga;
import ais.database.model.employ.Pendidikan;
import ais.database.model.file.FotoCalonSiswa;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.GelombangPendaftaranPsb;
import ais.database.model.sekolah.KebutuhanKhususSiswa;
import ais.database.model.sekolah.PaketPsb;
import ais.database.model.sekolah.PaketPsbPunyaGelombangPendaftaranPsb;
import ais.database.model.sekolah.PenghasilanOrangTuaSiswa;
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
import ais.ui.util.MyRadioConfig;

public class PPDB2 extends PPDB {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Textbox sekolahAsal;
	private Textbox nomorIndukNasional;
	private Textbox nama;
	private Combobox jenisKelamin;
	private Textbox tempatLahir;
	private MyDatebox tanggalLahir;
	protected LampiranLain akteKelahiran;
	private Textbox nik;
	private Textbox kk;
	protected LampiranLain kartuKeluarga;
	private Combobox statusDalamKeluarga;
	private Intbox anakKe;
	private Intbox dariAnakKe;
	private Textbox infoMempunyaiSaudaraKandung;
	private Vbox infoKebutuhanKhusus;
	private Textbox koordinat;
	private Combobox jenisTinggal;
	private Combobox alatTransportasi;
	private Radiogroup apakahMempunyaiSaudaraKandung;
	private Textbox noKip;
	private Combobox layakPip;
	private Radiogroup penerimaBantuan;
	private Textbox noAktaKelahiran;

	private Combobox tidakLayakPip;

	private Textbox namaAyah;

	private Textbox nikAyah;

	private Textbox tempatLahirAyah;

	private MyDatebox tanggalLahirAyah;

	private Combobox pendidikanAyah;

	private Combobox pekerjaanAyah;

	private Combobox penghasilanAyah;

	private Textbox hp1ayah;

	private Textbox namaIbu;

	private Textbox nikIbu;

	private Textbox tempatLahirIbu;

	private MyDatebox tanggalLahirIbu;

	private Combobox pendidikanIbu;

	private Combobox pekerjaanIbu;

	private Combobox penghasilanIbu;

	private Textbox hp1ibu;

	private Textbox namaWali;

	private Textbox nikWali;

	private Textbox tempatLahirWali;

	private MyDatebox tanggalLahirWali;

	private Combobox pendidikanWali;

	private Combobox pekerjaanWali;

	private Combobox penghasilanWali;

	private Textbox hp1wali;

	protected FotoCalonSiswa fotoCalonSiswa;

	private MyCheckboxConfig pernyataan;

	private Textbox nis;

	private Textbox nomorSeriIjazah;

	private Textbox nomorSeriSkhun;

	private Textbox nomorUjianNasional;

	private Textbox nomorPokokSekolahNasional;

	private Combobox agama;

	private Textbox alamatSiswa;

	private Textbox rt;

	private Textbox rw;

	private Textbox dusunCalon;

	private Textbox kelurahanCalon;

	private AmbilDataKecamatanBanbox kecamatanCalon;

	private Label kotaCalon;

	private Label propinsiCalon;

	private Textbox kodePos;

	private Radiogroup mempunyaiWali;

	private Hbox hboxakteKelahiran;

	private Hbox hboxkartuKeluarga;

	protected TreeSet<Long> selectedKelasLesSiswa;

	private Combobox penjurusanSekolah;

	protected boolean baru;

	private Row rowParameterTambahan;

	private ArrayList<Row> parameterRows;

	private ParameterTambahanPsbListener parameterTambahanListener;

	private Rows subRowsVerifikasiKelengkapanCalonSiswa;

	private Rows subRowsVerifikasiNilaiRapor;

	private List<Rows> subRowsVerifikasiNilaiParameter;

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();

	protected TreeSet<Long> hapusKelasLesSiswa;

	private Combobox paketPsb;

	private Box infoKampusDariMana;

	private Textbox namaTemanInfoKampusDariMana;

	private Textbox keteranganInfoKampusDariMana;

	private MyCheckboxConfig merupakanPindahan;

	private Textbox pindahanDariSekolah;

	private Textbox alamatSekolahPindahan;

	private MyDatebox tanggalPindah;

	private Textbox keteranganPindah;

	private Textbox kelasSekolahPindahan;

	private Textbox alamatEmail;

	private AmbilDataSiswaBanbox siswaAlumni;

	private Combobox keluarga;

	private EventListener checkKesamaan = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			CalonSiswaAction.CheckKesamaan checkKesamaan = new CalonSiswaAction.CheckKesamaan(calonSiswa,
					gelombangPendaftaranPsb, tanggalLahir, nama, namaIbu, PPDB2.this, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							CalonSiswa calonSiswa1 = (CalonSiswa) arg0.getData();
							PPDB window = new PPDB2();
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

	public PPDB2() {
		super();
	}

	public PPDB2(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb, EventListener eventListener) {
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:372");
		}

		CalonSiswaAction.initBg(center, gelombangPendaftaranPsb);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelAgakKecilBoldBiru(Common.getBahasaConfig("PENERIMAAN PESERTA DIDIK BARU") + " "
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

		siswaAlumni = PPDB.alumni(calonSiswa, gelombangPendaftaranPsb, rows, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Siswa s = (Siswa) arg0.getData();
				if (nama != null) {
					nama.setValue(s.getNama());
					nama.setDisabled(true);
				}

				if (jenisKelamin != null) {
					Common.selectComboItem(jenisKelamin, s.getJenisKelamin());
				}

				if (tempatLahir != null && tempatLahir.getValue().trim().isEmpty()) {
					tempatLahir.setValue(s.getTempatLahir());
				}
				if (tanggalLahir != null && tanggalLahir.getValue() == null) {
					tanggalLahir.setValue(s.getTanggalLahir());
				}
				if (namaAyah != null && namaAyah.getValue().trim().isEmpty()) {
					namaAyah.setValue(s.getNamaAyah());
				}
				if (namaIbu != null && namaIbu.getValue().trim().isEmpty()) {
					namaIbu.setValue(s.getNamaIbu());
				}
				if (hp1ayah != null && hp1ayah.getValue().trim().isEmpty()) {
					hp1ayah.setValue(s.getHp1ayah());
				}
				if (hp1ibu != null && hp1ibu.getValue().trim().isEmpty()) {
					hp1ibu.setValue(s.getHp1ibu());
				}
				if (alamatSiswa != null && alamatSiswa.getValue().trim().isEmpty()) {
					alamatSiswa.setValue(s.getAlamatSiswa());
				}
			}
		});

		Tbmuser tbmuser = Common.getCurrentUser();
		Pegawai pegawai = tbmuser == null ? null : tbmuser.getPegawai();
		keluarga = PPDB.anakPegawai(calonSiswa, gelombangPendaftaranPsb, pegawai, rows, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Keluarga s = (Keluarga) arg0.getData();
				if (nama != null) {
					nama.setValue(s.getNama());
					nama.setDisabled(true);
				}
				Pegawai pegawai = s.getPegawai();
				if (jenisKelamin != null) {
					Common.selectComboItem(jenisKelamin, s.getJenisKelamin());
				}

				if (tempatLahir != null && tempatLahir.getValue().trim().isEmpty()) {
					tempatLahir.setValue(s.getTempatLahir());
				}
				if (tanggalLahir != null && tanggalLahir.getValue() == null) {
					tanggalLahir.setValue(s.getTanggalLahir());
				}
				if (namaAyah != null && namaAyah.getValue().trim().isEmpty()
						&& pegawai.getKelamin().equalsIgnoreCase("Laki-laki")) {
					namaAyah.setValue(pegawai.getNama());
				}
				if (namaIbu != null && namaIbu.getValue().trim().isEmpty()
						&& pegawai.getKelamin().equalsIgnoreCase("Perempuan")) {
					namaIbu.setValue(pegawai.getNama());
				}
				if (hp1ayah != null && hp1ayah.getValue().trim().isEmpty()
						&& pegawai.getKelamin().equalsIgnoreCase("Laki-laki")) {
					hp1ayah.setValue(pegawai.getHp());
				}
				if (hp1ibu != null && hp1ibu.getValue().trim().isEmpty()
						&& pegawai.getKelamin().equalsIgnoreCase("Perempuan")) {
					hp1ibu.setValue(pegawai.getHp());
				}

				if (alamatSiswa != null && alamatSiswa.getValue().trim().isEmpty()) {
					alamatSiswa.setValue(s.getAlamat());
				}
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelAgakKecilBoldBiru("* Menunjukkan pertanyaan yang wajib diisi"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis pendaftaran *"));
		row.appendChild(new Label(gelombangPendaftaranPsb.getNama()));

		penjurusanSekolah = new Combobox();

		if (gelombangPendaftaranPsb.getPenjurusanSekolah() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Penjurusan"));
			row.appendChild(new Label(gelombangPendaftaranPsb.getPenjurusanSekolah().getNama()));
		} else {
			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Penjurusan *"));
			row.appendChild(penjurusanSekolah);

			penjurusanSekolah.setWidth("90%");
			penjurusanSekolah.setReadonly(true);

			Sekolah s = gelombangPendaftaranPsb.getSekolah();
			System.out.println("s => " + s);

			if (s != null && s.getPenjurusanBolehDipilihSaatPsb()) {
				HibernateUtil.currentSession().refresh(s);
				Set<PenjurusanSekolah> selectedPenjurusanSekolah = s.getPenjurusanSekolahs();
				for (PenjurusanSekolah o : selectedPenjurusanSekolah) {
					if (o.getAktif() && o.getTampilkanDiPpdb()) {
						Comboitem comboitem = new Comboitem();
						comboitem.setLabel(o.getNama());
						comboitem.setDescription(o.getKeterangan());
						comboitem.setValue(o);
						penjurusanSekolah.appendChild(comboitem);
					}
				}
				penjurusanSekolah.getParent().setVisible(!selectedPenjurusanSekolah.isEmpty());
				Common.selectComboItem(penjurusanSekolah, calonSiswa.getPenjurusanSekolah());

			}
		}

		row = new MyFormRow();
		row.setVisible(false);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Paket *"));
		row.appendChild(paketPsb = new Combobox());
		paketPsb.setWidth("90%");
		paketPsb.setReadonly(true);

		if (gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getId() != null) {
			List<Long> paketPsbPunyaGelombangPendaftaranPsbs = HibernateUtil.currentSession()
					.createCriteria(PaketPsbPunyaGelombangPendaftaranPsb.class)
					.setProjection(Projections.groupProperty("paketPsb.id"))
					.add(Restrictions.eq("gelombangPendaftaranPsb", gelombangPendaftaranPsb)).list();

			Criterion criterion = Restrictions.sqlRestriction("false");
			if (!paketPsbPunyaGelombangPendaftaranPsbs.isEmpty()) {
				criterion = Restrictions.in("id", paketPsbPunyaGelombangPendaftaranPsbs);
			}

			paketPsb.getParent().setVisible(!paketPsbPunyaGelombangPendaftaranPsbs.isEmpty());
			Common.clear(paketPsb);
			Common.insertComboDanSemua(paketPsb, new String[] { "nama" }, "keterangan", PaketPsb.class, "Pilih Paket",
					criterion);
			Common.selectComboItem(true, paketPsb, calonSiswa.getPaketPsb());
		}

		MyGroupConfig myRowStyled = new MyGroupConfig("IDENTITAS PESERTA DIDIK (WAJIB DI ISI)");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelAgakKecilBoldBiru("Isi data sesuai dengan dokumen yang berlaku (Akte Kelahiran)"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("a. Nama lengkap *"));
		row.appendChild(nama = new Textbox(calonSiswa.getNama()));
		nama.setWidth("90%");
		nama.addEventListener("onChange", checkKesamaan);

		jenisKelamin = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig();
		comboitem.setLabel("Laki-laki");
		comboitem.setValue("Laki-laki");
		jenisKelamin.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Perempuan");
		comboitem.setValue("Perempuan");
		jenisKelamin.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("b. Jenis kelamin *"));
		Common.selectComboItem(jenisKelamin, calonSiswa.getJenisKelamin());
		row.appendChild(jenisKelamin);
		jenisKelamin.setWidth("90%");
		jenisKelamin.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("c. NISN (Nomor Induk Siswa Nasional) *"));
		row.appendChild(nomorIndukNasional = new Textbox(calonSiswa.getNomorIndukNasional()));
		nomorIndukNasional.setWidth("90%");

		Common.initKeterangan(rows,
				"(Bagi yang belum memiliki NISN, mohon diisi dengan angka 0) Untuk memeriksa NISN, dapat mengunjungi laman)");
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		A link = new A("https://nisn.data.kemdikbud.go.id/index.php/Cindex/formcaribynama");
		link.setTarget("_blank");
		link.setHref("https://nisn.data.kemdikbud.go.id/index.php/Cindex/formcaribynama");
		row.appendChild(link);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("d. NIS (Nomor Induk Siswa)"));
		row.appendChild(nis = new Textbox(calonSiswa.getNis()));
		nis.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("e. Nomor Seri Ijazah"));
		row.appendChild(nomorSeriIjazah = new Textbox(calonSiswa.getNomorSeriIjazah()));
		nomorSeriIjazah.setWidth("90%");

		Common.initKeterangan(rows, "*) diisikan data dari jenjang sebelumnya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("f. Nomor Seri SKHUN"));
		row.appendChild(nomorSeriSkhun = new Textbox(calonSiswa.getNomorSeriSkhun()));
		nomorSeriSkhun.setWidth("90%");

		Common.initKeterangan(rows, "*) diisikan data dari jenjang sebelumnya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("g. Nomor Ujian Nasional"));
		row.appendChild(nomorUjianNasional = new Textbox(calonSiswa.getNomorUjianNasional()));
		nomorUjianNasional.setWidth("90%");

		Common.initKeterangan(rows, "*) diisikan data dari jenjang sebelumnya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("h. NIK (Nomor Induk Kependudukan) peserta didik 16 digit *"));
		row.appendChild(nik = new Textbox(calonSiswa.getNik()));
		nik.setWidth("90%");

		Common.initKeterangan(rows, "(Contoh: 3317xxxxxxxxxxxx)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("i. NPSN Sekolah Asal"));
		row.appendChild(nomorPokokSekolahNasional = new Textbox(calonSiswa.getNomorPokokSekolahNasional()));
		nomorPokokSekolahNasional.setWidth("90%");

		Common.initKeterangan(rows, "*) diisikan data dari jenjang sebelumnya");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("j. Nama asal sekolah *"));
		row.appendChild(sekolahAsal = new Textbox(calonSiswa.getSekolahAsal()));
		sekolahAsal.setWidth("90%");

		Common.initKeterangan(rows,
				"(Diisi nama sekolah pada jenjang sebelumnya, bagi peserta didik pindahan atau mutasi diisi dengan nama sekolah sebelumnya)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("k. Tempat lahir *"));
		row.appendChild(tempatLahir = new Textbox(calonSiswa.getTempatLahir()));
		tempatLahir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("l. Tanggal lahir *"));
		row.appendChild(tanggalLahir = new MyDatebox(calonSiswa.getTanggalLahir()));

		EventListener eventListenerTanggalLahir = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				GelombangPendaftaranPsb.chekUmur(gelombangPendaftaranPsb, tanggalLahir);
				checkKesamaan.onEvent(arg0);
			}
		};

		tanggalLahir.addEventListener("onChange", eventListenerTanggalLahir);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("m. Agama *"));
		row.appendChild(agama = new Combobox());
		Common.insertCombo(agama, "nama", "keterangan", Agama.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
		Common.selectComboItem(agama, calonSiswa.getAgama());
		agama.setWidth("90%");
		agama.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("n. Apakah peserta didik berkebutuhan khusus ?"));

		row.setParent(rows);
		infoKebutuhanKhusus = new Vbox();
		row.appendChild(infoKebutuhanKhusus);

		List<KebutuhanKhususSiswa> kebutuhanKhusus = ConstantValues
				.simpleList(HibernateUtil.currentSession().createCriteria(KebutuhanKhususSiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.addOrder(Order.asc("nama")), KebutuhanKhususSiswa.class);

		for (KebutuhanKhususSiswa khusus : kebutuhanKhusus) {

			if (khusus != null) {
				Checkbox check = new Checkbox();
				check.setLabel(khusus.getNama());
				check.setValue(khusus.getId() + "");
				infoKebutuhanKhusus.appendChild(check);
				if (calonSiswa.getKebutuhanKhusus().contains(";" + khusus.getId() + ";")) {
					check.setChecked(true);
				}
			}
		}
		Common.initKeterangan(rows, "(Boleh dikosongkan jika tidak ada kebutuhan khusus)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("o. Alamat Siswa"));
		row.appendChild(alamatSiswa = new Textbox(calonSiswa.getAlamatSiswa()));
		alamatSiswa.setWidth("90%");
		alamatSiswa.setRows(2);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(" - RT"));
		row.appendChild(rt = new Textbox(calonSiswa.getRt() == null ? "" : calonSiswa.getRt()));
		rt.setWidth("90%");
		rt.setMaxlength(3);
		rt.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(" - RW"));
		row.appendChild(rw = new Textbox(calonSiswa.getRw() == null ? "" : calonSiswa.getRw()));
		rw.setWidth("90%");
		rw.setMaxlength(3);
		rw.setCols(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(" - Dusun / Kampung"));
		row.appendChild(dusunCalon = new Textbox(calonSiswa.getDusunCalon()));
		dusunCalon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(" - Kelurahan / Desa"));
		row.appendChild(kelurahanCalon = new Textbox(
				calonSiswa.getKelurahanCalon() == null ? "" : calonSiswa.getKelurahanCalon()));
		kelurahanCalon.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);

		row.appendChild(new ais.ui.util.MyLabelConfig(" - Kecamatan"));
		row.appendChild(kecamatanCalon = new AmbilDataKecamatanBanbox());
		kecamatanCalon.setValue(calonSiswa.getKecamatanCalon() == null ? "" : calonSiswa.getKecamatanCalon().getNama());
		kecamatanCalon.setAttribute("wilayah", calonSiswa.getKecamatanCalon());
		kecamatanCalon.setWidth("90%");
		// kecamatanCalon.//setConstraint("no empty");

		propinsiCalon = new Label();
		Common.createFieldKota(rows, " - Kota/Kabupaten", kotaCalon = new Label(), propinsiCalon,
				calonSiswa.getKotaCalon(), true);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(" - Propinsi"));

		row.appendChild(propinsiCalon);
		propinsiCalon.setWidth("90%");

		propinsiCalon.setAttribute("wilayah", calonSiswa.getPropinsiCalon());

		// propinsiCalon.//setConstraint("no empty");

		try {
			Common.createKotaPropinsiListenerBerdasarkanKecamatan(propinsiCalon, kotaCalon, kecamatanCalon);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:795");
		}

		kotaCalon.setAttribute("wilayah", calonSiswa.getKotaCalon());

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(" - Kode Pos"));
		row.appendChild(kodePos = new Textbox(calonSiswa.getKodePos() == null ? "" : calonSiswa.getKodePos()));
		kodePos.setWidth("90%");
		kodePos.setMaxlength(8);
		kodePos.setCols(8);

		String[] c = Common.getKonfigurasi("moda_transportasi_calon_siswa",
				"Jalan kaki;Sepeda / Sepeda Listrik;Motor;Mobil;Ojek;Kendaraan umum").getNilai().split(";");
		alatTransportasi = new Combobox();
		for (String s : c) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(s);
			comboitem.setValue(s);
			alatTransportasi.appendChild(comboitem);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("p. Alat Transportasi ke Sekolah *"));
		Common.selectComboItem(alatTransportasi, calonSiswa.getAlatTransportasi());
		row.appendChild(alatTransportasi);
		alatTransportasi.setWidth("90%");
		alatTransportasi.setReadonly(true);

		c = Common.getKonfigurasi("jenis_tinggal_calon_siswa",
				"Bersama orang tua;Bersama wali;Pondok pesantren;Panti asuhan").getNilai().split(";");
		jenisTinggal = new Combobox();
		for (String s : c) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(s);
			comboitem.setValue(s);
			jenisTinggal.appendChild(comboitem);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("q. Jenis Tempat Tinggal *"));
		Common.selectComboItem(jenisTinggal, calonSiswa.getJenisTinggal());
		row.appendChild(jenisTinggal);
		jenisTinggal.setWidth("90%");
		jenisTinggal.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("9. Nomor registrasi akta kelahiran *"));
		row.appendChild(noAktaKelahiran = new Textbox(calonSiswa.getNoAktaKelahiran()));
		noAktaKelahiran.setWidth("90%");

		Common.initKeterangan(rows, "(Contoh: 3317-LT-01012020)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("10. Unggah dokumen akte kelahiran (Dalam bentuk PDF) *"));
		akteKelahiran = null;
		hboxakteKelahiran = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hboxakteKelahiran, calonSiswa.getId(), "AKTE_KELAHIRAN_CALON_SISWA",
				"Akte Kelahiran", true, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						akteKelahiran = (LampiranLain) arg0.getData();
					}
				});
		hboxakteKelahiran.setParent(row);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("12. Nomor Kartu Keluarga (16 Digit) *"));
		row.appendChild(kk = new Textbox(calonSiswa.getKk()));
		kk.setWidth("90%");

		Common.initKeterangan(rows, "(Contoh: 3317xxxxxxxxxxxx)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("13. Unggah dokumen kartu keluarga (dalam bentuk PDF) *"));
		kartuKeluarga = null;
		hboxkartuKeluarga = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hboxkartuKeluarga, calonSiswa.getId(), "KK_CALON_SISWA",
				"Kartu Keluarga", true, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kartuKeluarga = (LampiranLain) arg0.getData();
					}
				});
		hboxkartuKeluarga.setParent(row);

		statusDalamKeluarga = new Combobox();
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Anak Kandung");
		comboitem.setValue("Anak Kandung");
		statusDalamKeluarga.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Anak Angkat");
		comboitem.setValue("Anak Angkat");
		statusDalamKeluarga.appendChild(comboitem);
		comboitem = new MyComboitemConfig();
		comboitem.setLabel("Anak Tiri");
		comboitem.setValue("Anak Tiri");
		statusDalamKeluarga.appendChild(comboitem);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("14. Status peserta didik *"));
		Common.selectComboItem(statusDalamKeluarga, calonSiswa.getStatusDalamKeluarga());
		row.appendChild(statusDalamKeluarga);
		statusDalamKeluarga.setWidth("90%");
		statusDalamKeluarga.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("15. Peserto Didik adalah anak ke...*"));
		row.appendChild(anakKe = new Intbox(calonSiswa.getAnakKe()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("16. Jumlah saudara kandung *"));
		row.appendChild(dariAnakKe = new Intbox(calonSiswa.getDariAnakKe()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("17. Apakah peserta didik mempunyai saudara kandung yang sekolah di "
						+ gelombangPendaftaranPsb.getSekolah().getYayasan().getNama() + " *"));

		apakahMempunyaiSaudaraKandung = new Radiogroup();

		MyRadioConfig radio = new MyRadioConfig("Ya, memiliki saudara kandung");
		radio.setAttribute("nilai", true);
		radio.setChecked(calonSiswa.getApakahMempunyaiSaudaraKandung());
		apakahMempunyaiSaudaraKandung.appendChild(radio);

		radio = new MyRadioConfig("Tidak memiliki saudara kandung");
		radio.setAttribute("nilai", false);
		radio.setChecked(!calonSiswa.getApakahMempunyaiSaudaraKandung());
		apakahMempunyaiSaudaraKandung.appendChild(radio);

		row.appendChild(apakahMempunyaiSaudaraKandung);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("18. Apabila mempunyai saudara kandung yang sekolah di "
				+ gelombangPendaftaranPsb.getSekolah().getYayasan().getNama() + ", sebutkan unit, nama dan kelasnya"));
		row.appendChild(infoMempunyaiSaudaraKandung = new Textbox(calonSiswa.getInfoMempunyaiSaudaraKandung()));
		infoMempunyaiSaudaraKandung.setWidth("90%");
		infoMempunyaiSaudaraKandung.setRows(2);

		Common.initKeterangan(rows,
				"(Sebutkan semua saudara apabila lebih dari satu, Contoh: SD, Almira Saputri, Kelas LA)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(
				new ais.ui.util.MyLabelConfig("20. Titik Koordinat Tempat Tinggal (Garis Lintang, Garis Bujur) *"));
		row.appendChild(koordinat = new Textbox(calonSiswa.getKoordinat()));
		koordinat.setWidth("90%");
		Common.initKeteranganHtml(rows,
				"Titik koordinat tempat tinggal peserta didik bisa dilihat dengan membuka google map dari rumah tempat tinggal, kemudian catat silk koordinatnya<br>"
						+ "Cara Mendapatkan Titik Koordinat Di Ponsel<br>"
						+ "1. Buka aplikast Google Maps pada ponsel<br>"
						+ "2. Sentuh lama area tempat tinggal pada peta untuk memasang pin merah<br>"
						+ "3. Lihat titik koordinat pada kotak penelusuran Contoh:-6.700632, 111.411358");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig());
		link = new A("http://www.google.com/maps");
		link.setTarget("_blank");
		link.setHref("http://www.google.com/maps");
		row.appendChild(link);

		Common.initKeterangan(rows, "contoh :  6.712830, 111.358844");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("23. Apakah Peserta Didik Penerima KPS/PKH/PIP? *"));

		row.appendChild(penerimaBantuan = new Radiogroup());

		radio = new MyRadioConfig("Ya, penerima KPS/PKH/PIP?");
		radio.setAttribute("nilai", true);
		radio.setChecked(calonSiswa.getPenerimaBantuan());
		penerimaBantuan.appendChild(radio);

		radio = new MyRadioConfig("Bukan penerima KPS/PKH/PIP?");
		radio.setAttribute("nilai", false);
		radio.setChecked(!calonSiswa.getPenerimaBantuan());
		penerimaBantuan.appendChild(radio);

		row.appendChild(penerimaBantuan);

		Common.initKeterangan(rows,
				"KPS (Kartu Perlindungan Sosial), PKH (Program Keluarga Harapan), PIP (Program Indonesia Pintar)");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"24. Apakah Peserta Didik Memiliki KIP (Kartu Indonesia Pintar) ? Sebutkan Nomor KIP"));
		row.appendChild(noKip = new Textbox(calonSiswa.getNoKip()));
		noKip.setWidth("90%");

		Common.initKeterangan(rows, "Contoh : Ya, 6013xxxxxxxxxxxx");

		c = Common.getKonfigurasi("layak_pip_calon_siswa_baru",
				"Pemegang PKH/KPS/KIP;Penerima BSM;Siswa miskin/rentan miskin;Yatim piatu/panti asuhan/panti social;Dampak bencana alam;Daerah konflik;Keluarga terpidana/berada di LAPAS;Kelainan fisik")
				.getNilai().split(";");
		layakPip = new Combobox();
		for (String s : c) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(s);
			comboitem.setValue(s);
			layakPip.appendChild(comboitem);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"25. Peserta Didik yang memiliki KIP (Kartu Indonesia Pintar), BERSEDIA diusulkan sebagai Penerima PIP (Program Indonesia Pintar). Alasan layak sebagai penerima PIP (Program Indonesia Pintar) *"));
		Common.selectComboItem(layakPip, calonSiswa.getLayakPip());
		row.appendChild(layakPip);
		layakPip.setWidth("90%");
		layakPip.setReadonly(true);

		c = Common.getKonfigurasi("tidak_layak_pip_calon_siswa_baru",
				"Dilarang pemda karena menerima bantuan serupa;Menolak;Sudah mampu").getNilai().split(";");
		tidakLayakPip = new Combobox();
		for (String s : c) {
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(s);
			comboitem.setValue(s);
			tidakLayakPip.appendChild(comboitem);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(
				"26. Peserta Didik yang memiliki KIP (Kartu Indonesia Pintar), TIDAK BERSEDIA diusulkan sebagai Penerima PIP (Program Indonesia Pintar). Alasan tidak layak sebagai penerima PIP (Program Indonesia Pintar) *"));
		Common.selectComboItem(tidakLayakPip, calonSiswa.getTidakLayakPip());
		row.appendChild(tidakLayakPip);
		tidakLayakPip.setWidth("90%");
		tidakLayakPip.setReadonly(true);

		myRowStyled = new MyGroupConfig("C. DATA AYAH KANDUNG");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelAgakKecilBoldBiru("Isi data sesuai dengan dokumen yang berlaku (Akte Kelahiran)"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("27. Nama ayah kandung (Tulis dengan huruf kapital) *"));
		row.appendChild(namaAyah = new Textbox(calonSiswa.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("28. NIK Ayah *"));
		row.appendChild(nikAyah = new Textbox(calonSiswa.getNikAyah()));
		nikAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("29. Tempat lahir ayah *"));
		row.appendChild(tempatLahirAyah = new Textbox(calonSiswa.getTempatLahirAyah()));
		tempatLahirAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("30. Tanggal lahir ayah *"));
		row.appendChild(tanggalLahirAyah = new MyDatebox(calonSiswa.getTanggalLahirAyah()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("31. Pendidikan ayah *"));
		row.appendChild(pendidikanAyah = new Combobox());
		Common.insertCombo(pendidikanAyah, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikanAyah, calonSiswa.getPendidikanAyah());
		pendidikanAyah.setWidth("90%");
		pendidikanAyah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("32. Pekerjaan ayah *"));
		row.appendChild(pekerjaanAyah = new Combobox());
		Common.insertCombo(pekerjaanAyah, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanAyah, calonSiswa.getPekerjaanAyah());
		pekerjaanAyah.setWidth("90%");
		pekerjaanAyah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("33. Penghasilan ayah *"));
		row.appendChild(penghasilanAyah = new Combobox());
		Common.insertCombo(penghasilanAyah, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanAyah, calonSiswa.getPenghasilanAyah());
		penghasilanAyah.setWidth("90%");
		penghasilanAyah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("34. No Telpon/Hp (aktif) ayah *"));
		row.appendChild(hp1ayah = new Textbox(calonSiswa.getHp1ayah()));
		hp1ayah.setWidth("90%");

		myRowStyled = new MyGroupConfig("D. DATA IBU KANDUNG");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelAgakKecilBoldBiru("Isi data sesuai dengan dokumen yang berlaku (Akte Kelahiran)"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("35. Nama ibu kandung (Tulis dengan huruf kapital) *"));
		row.appendChild(namaIbu = new Textbox(calonSiswa.getNamaIbu()));
		namaIbu.setWidth("90%");
		namaIbu.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("36. NIK Ibu *"));
		row.appendChild(nikIbu = new Textbox(calonSiswa.getNikIbu()));
		nikIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("37. Tempat lahir ibu *"));
		row.appendChild(tempatLahirIbu = new Textbox(calonSiswa.getTempatLahirIbu()));
		tempatLahirIbu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("38. Tanggal lahir ibu *"));
		row.appendChild(tanggalLahirIbu = new MyDatebox(calonSiswa.getTanggalLahirIbu()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("39. Pendidikan ibu *"));
		row.appendChild(pendidikanIbu = new Combobox());
		Common.insertCombo(pendidikanIbu, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikanIbu, calonSiswa.getPendidikanIbu());
		pendidikanIbu.setWidth("90%");
		pendidikanIbu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("32. Pekerjaan ibu *"));
		row.appendChild(pekerjaanIbu = new Combobox());
		Common.insertCombo(pekerjaanIbu, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanIbu, calonSiswa.getPekerjaanIbu());
		pekerjaanIbu.setWidth("90%");
		pekerjaanIbu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("33. Penghasilan ibu *"));
		row.appendChild(penghasilanIbu = new Combobox());
		Common.insertCombo(penghasilanIbu, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanIbu, calonSiswa.getPenghasilanIbu());
		penghasilanIbu.setWidth("90%");
		penghasilanIbu.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("34. No Telpon/Hp (aktif) ibu *"));
		row.appendChild(hp1ibu = new Textbox(calonSiswa.getHp1ibu()));
		hp1ibu.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("35. Alamat Email"));
		row.appendChild(alamatEmail = new Textbox(calonSiswa.getAlamatEmail()));
		alamatEmail.setWidth("90%");

		myRowStyled = new MyGroupConfig("E. DATA WALI");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Apakah Mempunya Wali ? *"));
		row.appendChild(mempunyaiWali = new Radiogroup());

		radio = new MyRadioConfig("Ya, mempunya wali");
		radio.setAttribute("nilai", true);
		radio.setChecked(calonSiswa.getMempunyaiWali());
		mempunyaiWali.appendChild(radio);

		radio = new MyRadioConfig("Tidak mempunya wali");
		radio.setAttribute("nilai", false);
		radio.setChecked(!calonSiswa.getMempunyaiWali());
		mempunyaiWali.appendChild(radio);

		row.appendChild(mempunyaiWali);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("36. Nama wali (Tulis dengan huruf kapital) *"));
		row.appendChild(namaWali = new Textbox(calonSiswa.getNamaWali()));
		namaWali.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("37. NIK Wali *"));
		row.appendChild(nikWali = new Textbox(calonSiswa.getNikWali()));
		nikWali.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("38. Tempat lahir wali *"));
		row.appendChild(tempatLahirWali = new Textbox(calonSiswa.getTempatLahirWali()));
		tempatLahirWali.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("39. Tanggal lahir wali *"));
		row.appendChild(tanggalLahirWali = new MyDatebox(calonSiswa.getTanggalLahirWali()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("40. Pendidikan wali *"));
		row.appendChild(pendidikanWali = new Combobox());
		Common.insertCombo(pendidikanWali, "nama", Pendidikan.class);
		Common.selectComboItem(pendidikanWali, calonSiswa.getPendidikanWali());
		pendidikanWali.setWidth("90%");
		pendidikanWali.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("41. Pekerjaan wali *"));
		row.appendChild(pekerjaanWali = new Combobox());
		Common.insertCombo(pekerjaanWali, "nama", Pekerjaan.class,
				Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")));
		Common.selectComboItem(pekerjaanWali, calonSiswa.getPekerjaanWali());
		pekerjaanWali.setWidth("90%");
		pekerjaanWali.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("42. Penghasilan wali *"));
		row.appendChild(penghasilanWali = new Combobox());
		Common.insertCombo(penghasilanWali, "nama", PenghasilanOrangTuaSiswa.class);
		Common.selectComboItem(penghasilanWali, calonSiswa.getPenghasilanWali());
		penghasilanWali.setWidth("90%");
		penghasilanWali.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("43. No Telpon/Hp (aktif) wali *"));
		row.appendChild(hp1wali = new Textbox(calonSiswa.getHp1wali()));
		hp1wali.setWidth("90%");

		EventListener mempunyaiWaliEventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Boolean punyaWali = (Boolean) (mempunyaiWali.getSelectedItem() == null ? false
						: mempunyaiWali.getSelectedItem().getAttribute("nilai"));
				namaWali.getParent().setVisible(punyaWali);
				nikWali.getParent().setVisible(punyaWali);
				tempatLahirWali.getParent().setVisible(punyaWali);
				tanggalLahirWali.getParent().setVisible(punyaWali);
				pendidikanWali.getParent().setVisible(punyaWali);
				pekerjaanWali.getParent().setVisible(punyaWali);
				penghasilanWali.getParent().setVisible(punyaWali);
				hp1wali.getParent().setVisible(punyaWali);
			}
		};

		mempunyaiWali.addEventListener("onClick", mempunyaiWaliEventListener);
		try {
			mempunyaiWaliEventListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:1282");
		}

		final MyGroupConfig myRowStyledKelas = new MyGroupConfig("F. KELAS YANG PILIH");
		myRowStyledKelas.setVisible(false);
		myRowStyledKelas.setParent(rows);

		final MyFormRow rowDataG = new MyFormRow();
		rowDataG.setVisible(false);
		rowDataG.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(rowDataG, "2");
		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				rowDataG.setVisible(false);

				selectedKelasLesSiswa = new TreeSet<Long>();
				hapusKelasLesSiswa = new TreeSet<Long>();
				CalonSiswaAction.initKelasLes(rowDataG, gelombangPendaftaranPsb, selectedKelasLesSiswa,
						hapusKelasLesSiswa, calonSiswa);
				myRowStyledKelas.setVisible(rowDataG.isVisible());
			}

		};

		Common.createDefaultTimer(eventListener);

		row = new MyFormRow();

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Foto Siswa"));

		fotoCalonSiswa = null;

		Vbox vbox = new Vbox();
		vbox.setHeight("100%");
		vbox.setWidth("100%");
		vbox.setParent(row);

		try {
			Common.createDownloadUploadFoto(vbox, calonSiswa, FotoCalonSiswa.class, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					fotoCalonSiswa = (FotoCalonSiswa) arg0.getData();
				}
			}, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:1333");
		}

		rowParameterTambahan = new MyFormRow();
		rowParameterTambahan.setVisible(false);
		rowParameterTambahan.setStyle("border:0px;background: transparent;");
		rowParameterTambahan.setParent(rows);
		rowParameterTambahan.appendChild(new MyLabelBolder("Form Tambahan"));

		parameterRows = new ArrayList<Row>();

		parameterTambahanListener = new ParameterTambahanPsbListener(calonSiswa, parameterRows, lampiranLains,
				gelombangPendaftaranPsb, false, rows);

		if (calonSiswa.getId() != null) {
			try {
				subRowsVerifikasiKelengkapanCalonSiswa = VerifikasiPSBHelper.tampilkanVerifikasi(calonSiswa, rows, null,
						calonSiswa.getGelombangPendaftaranPsb());
				subRowsVerifikasiNilaiRapor = VerifikasiMatapelajaranPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
						gelombangPendaftaranPsb, null);

				subRowsVerifikasiNilaiParameter = VerifikasiParameterPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
						null, calonSiswa.getGelombangPendaftaranPsb());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:1358");
			}

		}

		try {
			parameterTambahanListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:1367");
		}

		try {
			Component[] cc = CalonSiswaAction.infoPindahan(rows, calonSiswa);
			merupakanPindahan = (MyCheckboxConfig) cc[0];
			pindahanDariSekolah = (Textbox) cc[1];
			alamatSekolahPindahan = (Textbox) cc[2];
			keteranganPindah = (Textbox) cc[3];
			tanggalPindah = (MyDatebox) cc[4];
			kelasSekolahPindahan = (Textbox) cc[5];

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:1381");
		}

		try {
			Component[] cs = CalonSiswaAction.infoDariMana(rows, calonSiswa);
			infoKampusDariMana = (Box) cs[0];
			namaTemanInfoKampusDariMana = (Textbox) cs[1];
			keteranganInfoKampusDariMana = (Textbox) cs[2];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:1391");
		}

		pernyataan = Common.tambahKeteranganRowHtml(rows,
				"Dengan ini saya menyatakan bahwa data yang saya masukkan benar adanya, dan jika ternyata dikemudian hari ditemukan kesalahan pada data ini baik yang disengaja ataupun tidak disengaja maka saya bersedia menerima sanksi dan resiko yang ditimbulkan karenanya");

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
				PPDB2.this.detach();
			}
		});
		cancel.setParent(toolbar);

		final boolean daftar = tbmuser == null;
		final MyButtonConfig save = new MyButtonConfig(daftar ? "D A F T A R" : "S I M P A N  D A T A",
				"/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				baru = false;
				if (onSave(event)) {
					PPDB2.this.detach();

					if (PPDB2.this.eventListener != null) {
						PPDB2.this.eventListener.onEvent(new Event("", save, PPDB2.this.calonSiswa));
					}
					if (baru) {
						String informasi = Common.getKonfigurasi("informasi_registrasi_psb_berhasil_login",
								"Proses pendaftaran peserta didik baru berhasil dilakukan dengan nomor pendaftaran : [no_reg]. Silahkan catat nomor pendaftaran tersebut dan selanjutnya akan diarahkan ke Login.")
								.getNilai();
						informasi = org.apache.commons.lang3.StringUtils.replace(informasi, "[no_reg]",
								PPDB2.this.calonSiswa.getNoRegistrasi());
						MyMessageboxConfig.show(informasi, "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												if (baru && PPDB2.this.calonSiswa.getGelombangPendaftaranPsb()
														.getOtomatisLoginSetelahDaftar()) {
													PPDB2.this.calonSiswa.setTelahLogin(true);
													PPDB2.this.calonSiswa
															.setWaktuLogin(ais.ui.util.WaktuUtil.getDate());
													Common.refreshUpdate(PPDB2.this.calonSiswa);

													Common.setLogin(PPDB2.this.calonSiswa);
													Sessions.getCurrent(true).setAttribute("cetak", true);
													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Executions.getCurrent().sendRedirect("");
														}
													});
												} else {
													CalonSiswaAction.onCetakKartu(PPDB2.this.calonSiswa, daftar);
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

	public boolean check() throws Exception {
		if (penjurusanSekolah.getParent() != null && penjurusanSekolah.getParent().isVisible()
				&& (penjurusanSekolah.getSelectedItem() == null
						|| penjurusanSekolah.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Penjurusan harus dipilih. Langkah yang dapat dilakukan: (1) pilih Penjurusan yang sesuai; (2) pastikan pilihan telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							penjurusanSekolah.focus();
							Clients.scrollIntoView(penjurusanSekolah);
						}
					});
			return false;
		}

		if (nik != null && nik.isVisible() && nik.getParent() != null && nik.getParent().isVisible()) {
			if (!nik.getValue().trim().isEmpty() && nik.getValue().trim().length() != 16) {
				MyMessageboxConfig.show("NIK harus terdiri atas 16 digit. Langkah yang dapat dilakukan: (1) periksa kembali NIK yang dimasukkan; (2) pastikan jumlah digit tepat 16 angka; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								nik.focus();
								Clients.scrollIntoView(nik);
							}
						});
				return false;
			}
		}

		if (keluarga != null && keluarga.getParent() != null && keluarga.getParent().isVisible()) {
			Keluarga k = (Keluarga) (keluarga.getSelectedItem() == null ? null : keluarga.getSelectedItem().getValue());
			if (k == null) {
				MyMessageboxConfig.show("Data Anak Pegawai harus diisi. Langkah yang dapat dilakukan: (1) pilih data anak pegawai yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								keluarga.focus();
								keluarga.select();
								Clients.scrollIntoView(keluarga);
							}
						});
				return false;
			}
		}

		if (gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getHarusSebagaiAlumni()) {
			Siswa alumni = (Siswa) (siswaAlumni == null ? null : siswaAlumni.getAttribute("siswa"));
			if (alumni == null) {
				MyMessageboxConfig.show("Data Siswa Alumni harus diisi. Langkah yang dapat dilakukan: (1) pilih data siswa alumni yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								siswaAlumni.focus();
								siswaAlumni.select();
								Clients.scrollIntoView(siswaAlumni);
							}
						});
				return false;
			}
		}

		if (paketPsb.getParent() != null && paketPsb.getParent().isVisible()
				&& (paketPsb.getSelectedItem() == null || paketPsb.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Paket harus dipilih. Langkah yang dapat dilakukan: (1) pilih Paket yang sesuai; (2) pastikan pilihan telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							paketPsb.focus();
							Clients.scrollIntoView(paketPsb);
						}
					});
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Calon Siswa harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Calon Siswa; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							nama.focus();
							Clients.scrollIntoView(nama);
						}
					});

			return false;
		}

		if (nomorIndukNasional.getValue().trim().equals("")) {
			MyMessageboxConfig.show("NISN Calon Siswa harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom NISN Calon Siswa; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							nomorIndukNasional.focus();
							Clients.scrollIntoView(nomorIndukNasional);
						}
					});

			return false;
		}

		if (jenisKelamin.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Kelamin harus diisi. Langkah yang dapat dilakukan: (1) pilih Jenis Kelamin yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							jenisKelamin.focus();
							jenisKelamin.select();
							Clients.scrollIntoView(jenisKelamin);
						}
					});

			return false;
		}

		if (tempatLahir.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Tempat Lahir harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tempat Lahir; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tempatLahir.focus();
							Clients.scrollIntoView(tempatLahir);
						}
					});

			return false;
		}

		if (tanggalLahir.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Lahir harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tanggal Lahir; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tanggalLahir.focus();
							tanggalLahir.select();
							Clients.scrollIntoView(tanggalLahir);
						}
					});

			return false;
		}
		if (agama.getSelectedItem() == null) {
			MyMessageboxConfig.show("Agama harus diisi. Langkah yang dapat dilakukan: (1) pilih Agama yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							agama.focus();
							agama.select();

							Clients.scrollIntoView(agama);
						}
					});

			return false;
		}

		if (nik.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("NIK harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom NIK; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							nik.focus();
							nik.select();
							Clients.scrollIntoView(nik);
						}
					});

			return false;
		}

		if (kk.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Nomor Kartu Keluarga (KK) harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom KK; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							kk.focus();
							kk.select();
							Clients.scrollIntoView(kk);
						}
					});

			return false;
		}

		if (statusDalamKeluarga.getSelectedItem() == null) {
			MyMessageboxConfig.show("Status Dalam Keluarga harus diisi. Langkah yang dapat dilakukan: (1) pilih Status Dalam Keluarga yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							statusDalamKeluarga.focus();
							statusDalamKeluarga.select();
							Clients.scrollIntoView(statusDalamKeluarga);
						}
					});

			return false;
		}

		if (anakKe.getValue() == null) {
			MyMessageboxConfig.show("Anak Ke harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Anak Ke; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							anakKe.focus();
							anakKe.select();
							Clients.scrollIntoView(anakKe);
						}
					});

			return false;
		}
		if (dariAnakKe.getValue() == null) {
			MyMessageboxConfig.show("Dari Anak Ke harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Dari Anak Ke; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							dariAnakKe.focus();
							dariAnakKe.select();
							Clients.scrollIntoView(dariAnakKe);
						}
					});

			return false;
		}

		if (koordinat.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Koordinat harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Koordinat; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							koordinat.focus();
							koordinat.select();
							Clients.scrollIntoView(koordinat);
						}
					});

			return false;
		}

		if (jenisTinggal.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Tinggal harus diisi. Langkah yang dapat dilakukan: (1) pilih Jenis Tinggal yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							jenisTinggal.focus();
							jenisTinggal.select();
							Clients.scrollIntoView(jenisTinggal);
						}
					});

			return false;
		}

		if (alatTransportasi.getSelectedItem() == null) {
			MyMessageboxConfig.show("Alat Transportasi harus diisi. Langkah yang dapat dilakukan: (1) pilih Alat Transportasi yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							alatTransportasi.focus();
							alatTransportasi.select();
							Clients.scrollIntoView(alatTransportasi);
						}
					});

			return false;
		}

		if (namaAyah.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Nama Ayah harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Ayah; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							namaAyah.focus();
							namaAyah.select();
							Clients.scrollIntoView(namaAyah);
						}
					});

			return false;
		}

		if (nikAyah.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("NIK Ayah harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom NIK Ayah; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							nikAyah.focus();
							nikAyah.select();
							Clients.scrollIntoView(nikAyah);
						}
					});

			return false;
		}

		if (tempatLahirAyah.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Tempat Lahir Ayah harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tempat Lahir Ayah; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tempatLahirAyah.focus();
							tempatLahirAyah.select();
							Clients.scrollIntoView(tempatLahirAyah);
						}
					});

			return false;
		}

		if (tanggalLahirAyah.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Lahir Ayah harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tanggal Lahir Ayah; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tanggalLahirAyah.focus();
							tanggalLahirAyah.select();
							Clients.scrollIntoView(tanggalLahirAyah);
						}
					});

			return false;
		}

		if (pendidikanAyah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pendidikan Terakhir Ayah harus diisi. Langkah yang dapat dilakukan: (1) pilih Pendidikan Terakhir Ayah yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pendidikanAyah.focus();
							pendidikanAyah.select();
							Clients.scrollIntoView(pendidikanAyah);
						}
					});

			return false;
		}

		if (pekerjaanAyah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pekerjaan Ayah harus diisi. Langkah yang dapat dilakukan: (1) pilih Pekerjaan Ayah yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pekerjaanAyah.focus();
							pekerjaanAyah.select();
							Clients.scrollIntoView(pekerjaanAyah);
						}
					});

			return false;
		}

		if (penghasilanAyah.getSelectedItem() == null) {
			MyMessageboxConfig.show("Penghasilan Ayah harus diisi. Langkah yang dapat dilakukan: (1) pilih Penghasilan Ayah yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							penghasilanAyah.focus();
							penghasilanAyah.select();
							Clients.scrollIntoView(penghasilanAyah);
						}
					});

			return false;
		}

		if (hp1ayah.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Nomor HP/Telepon Ayah harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom HP/Telepon Ayah; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							hp1ayah.focus();
							hp1ayah.select();
							Clients.scrollIntoView(hp1ayah);
						}
					});

			return false;
		}

		if (namaIbu.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Nama Ibu harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Ibu; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							namaIbu.focus();
							namaIbu.select();
							Clients.scrollIntoView(namaIbu);
						}
					});

			return false;
		}

		if (nikIbu.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("NIK Ibu harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom NIK Ibu; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							nikIbu.focus();
							nikIbu.select();
							Clients.scrollIntoView(nikIbu);
						}
					});

			return false;
		}

		if (tempatLahirIbu.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Tempat Lahir Ibu harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tempat Lahir Ibu; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tempatLahirIbu.focus();
							tempatLahirIbu.select();
							Clients.scrollIntoView(tempatLahirIbu);
						}
					});

			return false;
		}

		if (tanggalLahirIbu.getValue() == null) {
			MyMessageboxConfig.show("Tanggal Lahir Ibu harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tanggal Lahir Ibu; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tanggalLahirIbu.focus();
							tanggalLahirIbu.select();
							Clients.scrollIntoView(tanggalLahirIbu);
						}
					});

			return false;
		}

		if (pendidikanIbu.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pendidikan Terakhir Ibu harus diisi. Langkah yang dapat dilakukan: (1) pilih Pendidikan Terakhir Ibu yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pendidikanIbu.focus();
							pendidikanIbu.select();
							Clients.scrollIntoView(pendidikanIbu);
						}
					});

			return false;
		}

		if (pekerjaanIbu.getSelectedItem() == null) {
			MyMessageboxConfig.show("Pekerjaan Ibu harus diisi. Langkah yang dapat dilakukan: (1) pilih Pekerjaan Ibu yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pekerjaanIbu.focus();
							pekerjaanIbu.select();
							Clients.scrollIntoView(pekerjaanIbu);
						}
					});

			return false;
		}

		if (penghasilanIbu.getSelectedItem() == null) {
			MyMessageboxConfig.show("Penghasilan Ibu harus diisi. Langkah yang dapat dilakukan: (1) pilih Penghasilan Ibu yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							penghasilanIbu.focus();
							penghasilanIbu.select();
							Clients.scrollIntoView(penghasilanIbu);
						}
					});

			return false;
		}

		if (hp1ibu.getValue().trim().isEmpty()) {
			MyMessageboxConfig.show("Nomor HP/Telepon Ibu harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom HP/Telepon Ibu; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							hp1ibu.focus();
							hp1ibu.select();
							Clients.scrollIntoView(hp1ibu);
						}
					});

			return false;
		}

		if (gelombangPendaftaranPsb != null && tanggalLahir != null && tanggalLahir.getValue() != null) {

			if (!GelombangPendaftaranPsb.chekUmur(gelombangPendaftaranPsb, tanggalLahir)) {
				return false;
			}

		}

		if (penjurusanSekolah != null && penjurusanSekolah.getSelectedItem() != null
				&& penjurusanSekolah.getSelectedItem().getValue() != null && tanggalLahir != null
				&& tanggalLahir.getValue() != null) {
			PenjurusanSekolah penjurusan = (PenjurusanSekolah) penjurusanSekolah.getSelectedItem().getValue();
			if (penjurusan.getDibatasiUmur()) {
				try {
					int umur = penjurusan.getUmurmaksimal();
					int umurMin = penjurusan.getUmurminimal();

					int umurCalonSiswa = Years
							.yearsBetween(new org.joda.time.DateTime(tanggalLahir.getValue()),
									new org.joda.time.DateTime(penjurusan.getUmurDihitungTanggal() != null
											? penjurusan.getUmurDihitungTanggal()
											: ais.ui.util.WaktuUtil.getDate()))
							.getYears();
					System.out.println("umur => " + umur + ", umurCalonSiswa =>" + umurCalonSiswa);
					if (umurCalonSiswa > umur) {
						MyMessageboxConfig.showFormatCb(
								"Batas umur maksimal calon siswa yang diperbolehkan untuk mendaftar adalah {V1} tahun, sedangkan umur yang Anda masukkan adalah {V2} tahun. Langkah yang dapat dilakukan: (1) periksa kembali Tanggal Lahir yang dimasukkan; (2) sesuaikan dengan persyaratan umur pendaftaran; (3) hubungi panitia pendaftaran apabila terdapat perbedaan data.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										tanggalLahir.focus();
										tanggalLahir.select();
										Clients.scrollIntoView(tanggalLahir);
									}
								}, umur, umurCalonSiswa);
						return false;
					}
					if (umurCalonSiswa < umurMin) {
						MyMessageboxConfig.showFormatCb(
								"Batas umur minimal calon siswa yang diperbolehkan untuk mendaftar adalah {V1} tahun, sedangkan umur yang Anda masukkan adalah {V2} tahun. Langkah yang dapat dilakukan: (1) periksa kembali Tanggal Lahir yang dimasukkan; (2) sesuaikan dengan persyaratan umur pendaftaran; (3) hubungi panitia pendaftaran apabila terdapat perbedaan data.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
								new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {
										tanggalLahir.focus();
										tanggalLahir.select();
										Clients.scrollIntoView(tanggalLahir);
									}
								}, umurMin, umurCalonSiswa);
						return false;
					}
				} catch (Exception e) {
					Common.tampilErrorJikaAdmin(e);
				}
			}

		}

		Boolean punyaWali = (Boolean) (mempunyaiWali.getSelectedItem() == null ? false
				: mempunyaiWali.getSelectedItem().getAttribute("nilai"));

		if (punyaWali) {
			if (namaWali.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Nama Wali harus diisi (jika tidak ada wali, isi dengan nama ayah). Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Wali; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								namaWali.focus();
								namaWali.select();
								Clients.scrollIntoView(namaWali);
							}
						});

				return false;
			}

			if (nikWali.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("NIK Wali harus diisi (jika tidak ada wali, isi dengan NIK ayah). Langkah yang dapat dilakukan: (1) lengkapi kolom NIK Wali; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								nikWali.focus();
								nikWali.select();
								Clients.scrollIntoView(nikWali);
							}
						});

				return false;
			}

			if (tempatLahirWali.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Tempat Lahir Wali harus diisi (jika tidak ada wali, isi dengan tempat lahir ayah). Langkah yang dapat dilakukan: (1) lengkapi kolom Tempat Lahir Wali; (2) pastikan data telah benar; (3) simpan kembali formulir.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tempatLahirWali.focus();
								tempatLahirWali.select();
								Clients.scrollIntoView(tempatLahirWali);
							}
						});

				return false;
			}

			if (tanggalLahirWali.getValue() == null) {
				MyMessageboxConfig.show("Tanggal Lahir Wali harus diisi (jika tidak ada wali, isi dengan tanggal lahir ayah). Langkah yang dapat dilakukan: (1) lengkapi kolom Tanggal Lahir Wali; (2) pastikan data telah benar; (3) simpan kembali formulir.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tanggalLahirWali.focus();
								tanggalLahirWali.select();
								Clients.scrollIntoView(tanggalLahirWali);
							}
						});

				return false;
			}

			if (pendidikanWali.getSelectedItem() == null) {
				MyMessageboxConfig.show(
						"Pendidikan Terakhir Wali harus diisi (jika tidak ada wali, isi dengan pendidikan terakhir ayah). Langkah yang dapat dilakukan: (1) pilih Pendidikan Terakhir Wali yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pendidikanWali.focus();
								pendidikanWali.select();
								Clients.scrollIntoView(pendidikanWali);
							}
						});

				return false;
			}

			if (pekerjaanWali.getSelectedItem() == null) {
				MyMessageboxConfig.show("Pekerjaan Wali harus diisi (jika tidak ada wali, isi dengan pekerjaan ayah). Langkah yang dapat dilakukan: (1) pilih Pekerjaan Wali yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pekerjaanWali.focus();
								pekerjaanWali.select();
								Clients.scrollIntoView(pekerjaanWali);
							}
						});

				return false;
			}

			if (penghasilanWali.getSelectedItem() == null) {
				MyMessageboxConfig.show("Penghasilan Wali harus diisi (jika tidak ada wali, isi dengan penghasilan ayah). Langkah yang dapat dilakukan: (1) pilih Penghasilan Wali yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								penghasilanWali.focus();
								penghasilanWali.select();
								Clients.scrollIntoView(penghasilanWali);
							}
						});

				return false;
			}

			if (hp1wali.getValue().trim().isEmpty()) {
				MyMessageboxConfig.show("Nomor HP/Telepon Wali harus diisi (jika tidak ada wali, isi dengan HP/telepon ayah). Langkah yang dapat dilakukan: (1) lengkapi kolom HP/Telepon Wali; (2) pastikan data telah benar; (3) simpan kembali formulir.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								hp1wali.focus();
								hp1wali.select();
								Clients.scrollIntoView(hp1wali);
							}
						});

				return false;
			}
		}

		try {

			if (calonSiswa != null && calonSiswa.getId() != null) {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				int jumlah = ((Number) streamingSession.createCriteria(LampiranLain.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("ref", calonSiswa.getId()))
						.add(Restrictions.eq("jenis", "AKTE_KELAHIRAN_CALON_SISWA")).uniqueResult()).intValue();

				StreamingHibernateUtil.getInstance().closeSession();

				if (jumlah == 0) {
					MyMessageboxConfig.show("File scan/foto akta kelahiran harus diunggah. Langkah yang dapat dilakukan: (1) siapkan file scan/foto akta kelahiran; (2) unggah file pada kolom yang tersedia; (3) simpan kembali formulir.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.scrollIntoView(hboxakteKelahiran);
								}
							});
					return false;
				}
			} else {
				if (akteKelahiran == null) {
					MyMessageboxConfig.show("File scan/foto akta kelahiran harus diunggah. Langkah yang dapat dilakukan: (1) siapkan file scan/foto akta kelahiran; (2) unggah file pada kolom yang tersedia; (3) simpan kembali formulir.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.scrollIntoView(hboxakteKelahiran);
								}
							});
					return false;
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:2249");
		}

		try {

			if (calonSiswa != null && calonSiswa.getId() != null) {
				Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();

				int jumlah = ((Number) streamingSession.createCriteria(LampiranLain.class)
						.setProjection(Projections.rowCount()).add(Restrictions.eq("ref", calonSiswa.getId()))
						.add(Restrictions.eq("jenis", "KK_CALON_SISWA")).uniqueResult()).intValue();

				StreamingHibernateUtil.getInstance().closeSession();

				if (jumlah == 0) {
					MyMessageboxConfig.show("File scan/foto kartu keluarga harus diunggah. Langkah yang dapat dilakukan: (1) siapkan file scan/foto kartu keluarga; (2) unggah file pada kolom yang tersedia; (3) simpan kembali formulir.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.scrollIntoView(hboxkartuKeluarga);
								}
							});
					return false;
				}
			} else {
				if (kartuKeluarga == null) {
					MyMessageboxConfig.show("File scan/foto kartu keluarga harus diunggah. Langkah yang dapat dilakukan: (1) siapkan file scan/foto kartu keluarga; (2) unggah file pada kolom yang tersedia; (3) simpan kembali formulir.", "Peringatan",
							MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Clients.scrollIntoView(hboxkartuKeluarga);
								}
							});
					return false;
				}
			}

		} catch (Exception e1) {
			StreamingHibernateUtil.getInstance().rollbackTransaction();
			e1.printStackTrace(); ais.common.ErrorAuditUtil.record(e1, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:2290");
		}

		if (!nomorIndukNasional.getValue().trim().isEmpty()) {

			if (!Common.isNumber(nomorIndukNasional.getValue().trim())) {
				MyMessageboxConfig.showFormatCb(
						"NISN pendidikan sebelumnya \"{V1}\" harus berupa angka. Silakan hubungi {V2}. Langkah yang dapat dilakukan: (1) periksa kembali NISN yang dimasukkan; (2) pastikan NISN hanya terdiri atas angka; (3) hubungi panitia pendaftaran untuk konfirmasi data.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								nomorIndukNasional.focus();
								nomorIndukNasional.select();
								Clients.scrollIntoView(nomorIndukNasional);
							}
						}, nomorIndukNasional.getValue().trim(), Common.getKonfigurasi("hubungi_admin_calon_mhs",
								"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
										+ "atau email : .....\r\n" + "").getNilai());

				return false;
			}

			int jml = ((Number) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
					.add(Restrictions.eq("tahunMasuk", gelombangPendaftaranPsb.getTahunMasuk()))
					.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setProjection(Projections.rowCount())
					.add(calonSiswa.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.ne("id", calonSiswa.getId()))
					.add(Restrictions.ilike("nomorIndukNasional", nomorIndukNasional.getValue().trim(),
							MatchMode.EXACT))
					.uniqueResult()).intValue();

			if (jml > 0) {

				MyMessageboxConfig.showFormatCb(
						"NISN pendidikan sebelumnya \"{V1}\" sudah terdaftar. Silakan hubungi {V2}. Langkah yang dapat dilakukan: (1) periksa kembali NISN yang dimasukkan; (2) pastikan NISN belum pernah didaftarkan; (3) hubungi panitia pendaftaran untuk konfirmasi data.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								nomorIndukNasional.focus();
								nomorIndukNasional.select();
								Clients.scrollIntoView(nomorIndukNasional);
							}
						}, nomorIndukNasional.getValue().trim(), Common.getKonfigurasi("hubungi_admin_calon_mhs",
								"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
										+ "atau email : .....\r\n" + "").getNilai());

				return false;
			}
		}

		if (merupakanPindahan.isChecked()) {
			if (pindahanDariSekolah != null && pindahanDariSekolah.getParent() != null
					&& pindahanDariSekolah.getParent().isVisible()
					&& pindahanDariSekolah.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Asal Sekolah Sebelumnya harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Asal Sekolah Sebelumnya; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								pindahanDariSekolah.focus();
								pindahanDariSekolah.select();
								Clients.scrollIntoView(pindahanDariSekolah);
							}
						});
				return false;
			}
			if (kelasSekolahPindahan != null && kelasSekolahPindahan.getParent() != null
					&& kelasSekolahPindahan.getParent().isVisible()
					&& kelasSekolahPindahan.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Kelas Terakhir Sekolah Sebelumnya harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Kelas Terakhir Sekolah Sebelumnya; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								kelasSekolahPindahan.focus();
								kelasSekolahPindahan.select();
								Clients.scrollIntoView(kelasSekolahPindahan);
							}
						});
				return false;
			}
			if (alamatSekolahPindahan != null && alamatSekolahPindahan.getParent() != null
					&& alamatSekolahPindahan.getParent().isVisible()
					&& alamatSekolahPindahan.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Alamat Sekolah Sebelumnya harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Alamat Sekolah Sebelumnya; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								alamatSekolahPindahan.focus();
								alamatSekolahPindahan.select();
								Clients.scrollIntoView(alamatSekolahPindahan);
							}
						});
				return false;
			}
			if (tanggalPindah != null && tanggalPindah.getParent() != null && tanggalPindah.getParent().isVisible()
					&& tanggalPindah.getValue() == null) {
				MyMessageboxConfig.show("Tanggal Pindah harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Tanggal Pindah; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tanggalPindah.focus();
								tanggalPindah.select();
								Clients.scrollIntoView(tanggalPindah);
							}
						});
				return false;
			}
		}

		if (gelombangPendaftaranPsb != null && !nama.getValue().trim().isEmpty() && tanggalLahir.getValue() != null) {
			int jml = ((Number) HibernateUtil.currentSession().createCriteria(CalonSiswa.class)
					.add(Restrictions.eq("tahunMasuk", gelombangPendaftaranPsb.getTahunMasuk()))
					.add(Restrictions.isNotNull("gelombangPendaftaranPsb")).setProjection(Projections.rowCount())
					.add(calonSiswa.getId() == null ? Restrictions.sqlRestriction("true")
							: Restrictions.ne("id", calonSiswa.getId()))
					.add(Restrictions.ilike("namaSiswa", nama.getValue().trim(), MatchMode.EXACT))
					.add(Restrictions.eq("tanggalLahir", tanggalLahir.getValue()))
					.createAlias("gelombangPendaftaranPsb", "gelombangPendaftaranPsb").add(Restrictions
							.eq("gelombangPendaftaranPsb.tahunAjaran", gelombangPendaftaranPsb.getTahunAjaran()))
					.uniqueResult()).intValue();

			if (jml > 0) {

				MyMessageboxConfig.showFormatCb(
						"Nama siswa \"{V1}\" dan tanggal lahir \"{V2}\" sudah terdaftar di tahun pelajaran \"{V3}\". Silakan hubungi {V4}. Langkah yang dapat dilakukan: (1) periksa kembali data nama dan tanggal lahir; (2) pastikan calon siswa belum pernah didaftarkan; (3) hubungi panitia pendaftaran untuk konfirmasi data.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tanggalLahir.focus();
								tanggalLahir.select();
								Clients.scrollIntoView(tanggalLahir);
							}
						}, nama.getValue().trim(), Common.dateFormat2.get().format(tanggalLahir.getValue()),
						gelombangPendaftaranPsb.getTahunAjaran(), Common.getKonfigurasi("hubungi_admin_calon_mhs",
								"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
										+ "atau email : .....\r\n" + "").getNilai());

				return false;
			}
		}

		if (!CalonSiswaAction.checkInfoDariMana(infoKampusDariMana, namaTemanInfoKampusDariMana,
				keteranganInfoKampusDariMana)) {
			return false;
		}

		if (!pernyataan.isChecked()) {
			MyMessageboxConfig.show("Pernyataan persetujuan harus dicentang. Langkah yang dapat dilakukan: (1) baca pernyataan yang tersedia; (2) centang kolom pernyataan persetujuan; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pernyataan.focus();
							Clients.scrollIntoView(pernyataan);
						}
					});
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

		if (akteKelahiran != null) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.refresh(akteKelahiran);
			akteKelahiran.setRef(calonSiswa.getId());
			streamingSession.getTransaction().begin();
			streamingSession.update(akteKelahiran);
			streamingSession.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();
		}

		if (kartuKeluarga != null) {
			Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
			streamingSession.refresh(kartuKeluarga);
			kartuKeluarga.setRef(calonSiswa.getId());
			streamingSession.getTransaction().begin();
			streamingSession.update(kartuKeluarga);
			streamingSession.getTransaction().commit();

			StreamingHibernateUtil.getInstance().closeSession();
		}

		VerifikasiParameterPSBHelper.simpanVerifikasi(calonSiswa, subRowsVerifikasiNilaiParameter);
		VerifikasiMatapelajaranPSBHelper.simpanVerifikasi(calonSiswa, subRowsVerifikasiNilaiRapor);
		VerifikasiPSBHelper.simpanVerifikasi(calonSiswa, subRowsVerifikasiKelengkapanCalonSiswa);

		if (!lampiranLains.isEmpty()) {
			Common.createDefaultTimer(new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Session streamingSession = StreamingHibernateUtil.getInstance().currentSession();
					streamingSession.getTransaction().begin();
					for (LampiranLain lampiranLain : lampiranLains.values()) {
						streamingSession.refresh(lampiranLain);
						lampiranLain.setRef(calonSiswa.getId());
						streamingSession.update(lampiranLain);
					}
					streamingSession.getTransaction().commit();
					StreamingHibernateUtil.getInstance().closeSession();
				}
			});
		}

		if (this.hapusKelasLesSiswa != null) {
			for (Long kelasLesSiswa : this.hapusKelasLesSiswa) {
				session.createSQLQuery("delete from sekolah.kelas_les_punya_siswa where kelas_id=" + kelasLesSiswa
						+ " and calon_siswa=" + calonSiswa.getId()).executeUpdate();
			}
		}

		Common.hapusSession(CalonSiswa.class);

		if (baru && calonSiswa.getGelombangPendaftaranPsb().getMunculkanTagihanSetelahDaftar()
				&& !calonSiswa.getGelombangPendaftaranPsb().getOtomatisLoginSetelahDaftar()) {
			calonSiswa.munculkanFormPembayaran(eventListener);
		}

		try {
			if (calonSiswa.getGelombangPendaftaranPsb().getLangsungDapatNisSaatDaftar()
					&& calonSiswa.getSiswa() == null) {
				NisGenerator nisGenerator = (NisGenerator) Class
						.forName(Common.getKonfigurasi("class_untuk_generate_nis", DefaultNisGenerator.class.getName())
								.getNilai().trim())
						.newInstance();
				calonSiswa.setTelahDiterima(true);
				CommonPSB.onGenerateNis(calonSiswa, nisGenerator, false);
			}
		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:2575");
		}

		return true;
	}

	private void setdata() {

		try {
			String info = BiodataCalonMahasiswaAction.infoMahasiswaBaru(infoKampusDariMana);
			Siswa alumni = (Siswa) (siswaAlumni == null ? null : siswaAlumni.getAttribute("siswa"));
			calonSiswa.setSiswaAlumni(alumni);
			calonSiswa.setKelasSekolahPindahan(kelasSekolahPindahan.getValue().trim());
			calonSiswa.setMerupakanPindahan(merupakanPindahan.isChecked());
			calonSiswa.setPindahanDariSekolah(pindahanDariSekolah.getValue());
			calonSiswa.setAlamatSekolahPindahan(alamatSekolahPindahan.getValue());
			calonSiswa.setKeteranganPindah(keteranganPindah.getValue());
			calonSiswa.setTanggalPindah(tanggalPindah.getValue());
			calonSiswa.setInfoKampusDariMana(info);
			calonSiswa.setKeteranganInfoKampusDariMana(
					keteranganInfoKampusDariMana == null ? "" : keteranganInfoKampusDariMana.getValue());
			calonSiswa.setNamaTemanInfoKampusDariMana(
					namaTemanInfoKampusDariMana == null ? "" : namaTemanInfoKampusDariMana.getValue());
			info = CalonSiswa.kebutuhanKhusus(infoKebutuhanKhusus);
			calonSiswa.setPenjurusanSekolah((PenjurusanSekolah) (penjurusanSekolah.getSelectedItem() == null ? null
					: penjurusanSekolah.getSelectedItem().getValue()));
			calonSiswa.setGelombangPendaftaranPsb(gelombangPendaftaranPsb);
			calonSiswa.setNamaSiswa(nama.getValue());
			calonSiswa.setNis(nis.getValue());
			calonSiswa.setNomorSeriIjazah(nomorSeriIjazah.getValue());
			calonSiswa.setNomorSeriSkhun(nomorSeriSkhun.getValue());

			calonSiswa.setJenisKelamin(jenisKelamin.getValue());
			calonSiswa.setTempatLahir(tempatLahir.getValue());
			calonSiswa.setTanggalLahir(tanggalLahir.getValue());
			calonSiswa.setAnakKe(anakKe.getValue());
			calonSiswa.setDariAnakKe(dariAnakKe.getValue());
			calonSiswa.setNomorPokokSekolahNasional(nomorPokokSekolahNasional.getValue());
			calonSiswa.setSekolahAsal(sekolahAsal.getValue());

			calonSiswa.setPaketPsb(
					(PaketPsb) (paketPsb.getSelectedItem() == null ? null : paketPsb.getSelectedItem().getValue()));
			calonSiswa.setStatusDalamKeluarga((String) (statusDalamKeluarga.getSelectedItem() == null ? null
					: statusDalamKeluarga.getSelectedItem().getValue()));

			calonSiswa.setAgama((Agama) (agama.getSelectedItem() == null ? null : agama.getSelectedItem().getValue()));

			if (calonSiswa.getId() == null) {
				calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
			}
			calonSiswa.setNomorIndukNasional(nomorIndukNasional.getValue());

			calonSiswa.setNik(nik.getValue());
			calonSiswa.setKk(kk.getValue());

			calonSiswa.setSekolahAsal(sekolahAsal.getValue());
			calonSiswa.setNomorIndukNasional(nomorIndukNasional.getValue());
			calonSiswa.setNama(nama.getValue());
			calonSiswa.setNomorUjianNasional(nomorUjianNasional.getValue());
			calonSiswa.setJenisKelamin((String) (jenisKelamin.getSelectedItem() == null ? null
					: jenisKelamin.getSelectedItem().getValue()));
			calonSiswa.setTempatLahir(tempatLahir.getValue());
			calonSiswa.setTanggalLahir(tanggalLahir.getValue());
			calonSiswa.setNik(nik.getValue());
			calonSiswa.setKk(kk.getValue());
			calonSiswa.setAnakKe(anakKe.getValue());
			calonSiswa.setDariAnakKe(dariAnakKe.getValue());
			calonSiswa.setApakahMempunyaiSaudaraKandung(
					(Boolean) (apakahMempunyaiSaudaraKandung.getSelectedItem() == null ? false
							: apakahMempunyaiSaudaraKandung.getSelectedItem().getAttribute("nilai")));
			calonSiswa.setInfoMempunyaiSaudaraKandung(infoMempunyaiSaudaraKandung.getValue());
			calonSiswa.setKebutuhanKhusus(info);

			calonSiswa.setKoordinat(koordinat.getValue());
			calonSiswa.setJenisTinggal((String) (jenisTinggal.getSelectedItem() == null ? null
					: jenisTinggal.getSelectedItem().getValue()));

			calonSiswa.setAlatTransportasi((String) (alatTransportasi.getSelectedItem() == null ? null
					: alatTransportasi.getSelectedItem().getValue()));

			calonSiswa.setPenerimaBantuan((Boolean) (penerimaBantuan.getSelectedItem() == null ? false
					: penerimaBantuan.getSelectedItem().getAttribute("nilai")));

			calonSiswa.setNoKip(noKip.getValue());

			calonSiswa.setLayakPip(
					(String) (layakPip.getSelectedItem() == null ? null : layakPip.getSelectedItem().getValue()));

			calonSiswa.setTidakLayakPip((String) (tidakLayakPip.getSelectedItem() == null ? null
					: tidakLayakPip.getSelectedItem().getValue()));

			calonSiswa.setNoAktaKelahiran(noAktaKelahiran.getValue());

			calonSiswa.setNamaAyah(namaAyah.getValue());
			calonSiswa.setNikAyah(nikAyah.getValue());
			calonSiswa.setTempatLahirAyah(tempatLahirAyah.getValue());
			calonSiswa.setTanggalLahirAyah(tanggalLahirAyah.getValue());
			calonSiswa.setPendidikanAyah((Pendidikan) (pendidikanAyah.getSelectedItem() == null ? null
					: pendidikanAyah.getSelectedItem().getValue()));
			calonSiswa.setPekerjaanAyah((Pekerjaan) (pekerjaanAyah.getSelectedItem() == null ? null
					: pekerjaanAyah.getSelectedItem().getValue()));

			calonSiswa.setPenghasilanAyah((PenghasilanOrangTuaSiswa) (penghasilanAyah.getSelectedItem() == null ? null
					: penghasilanAyah.getSelectedItem().getValue()));

			calonSiswa.setHp1ayah(hp1ayah.getValue());

			calonSiswa.setNamaIbu(namaIbu.getValue());
			calonSiswa.setNikIbu(nikIbu.getValue());
			calonSiswa.setTempatLahirIbu(tempatLahirIbu.getValue());
			calonSiswa.setTanggalLahirIbu(tanggalLahirIbu.getValue());
			calonSiswa.setPendidikanIbu((Pendidikan) (pendidikanIbu.getSelectedItem() == null ? null
					: pendidikanIbu.getSelectedItem().getValue()));
			calonSiswa.setPekerjaanIbu((Pekerjaan) (pekerjaanIbu.getSelectedItem() == null ? null
					: pekerjaanIbu.getSelectedItem().getValue()));

			calonSiswa.setPenghasilanIbu((PenghasilanOrangTuaSiswa) (penghasilanIbu.getSelectedItem() == null ? null
					: penghasilanIbu.getSelectedItem().getValue()));

			calonSiswa.setHp1ibu(hp1ibu.getValue());

			calonSiswa.setNamaWali(namaWali.getValue());
			calonSiswa.setNikWali(nikWali.getValue());
			calonSiswa.setTempatLahirWali(tempatLahirWali.getValue());
			calonSiswa.setTanggalLahirWali(tanggalLahirWali.getValue());
			calonSiswa.setPendidikanWali((Pendidikan) (pendidikanWali.getSelectedItem() == null ? null
					: pendidikanWali.getSelectedItem().getValue()));
			calonSiswa.setPekerjaanWali((Pekerjaan) (pekerjaanWali.getSelectedItem() == null ? null
					: pekerjaanWali.getSelectedItem().getValue()));

			calonSiswa.setPenghasilanWali((PenghasilanOrangTuaSiswa) (penghasilanWali.getSelectedItem() == null ? null
					: penghasilanWali.getSelectedItem().getValue()));

			calonSiswa.setHp1wali(hp1wali.getValue());
			calonSiswa.setAlamatSiswa(alamatSiswa.getValue());
			calonSiswa.setDusunCalon(dusunCalon.getValue());
			calonSiswa.setRt(rt.getValue());
			calonSiswa.setRw(rw.getValue());
			calonSiswa.setKodePos(kodePos.getValue());
			calonSiswa.setKelurahanCalon(kelurahanCalon.getValue());
			calonSiswa.setKecamatanCalon((Wilayah) kecamatanCalon.getAttribute("wilayah"));
			calonSiswa.setPropinsiCalon((Propinsi) (propinsiCalon.getAttribute("wilayah")));
			calonSiswa.setKotaCalon((Kota) (kotaCalon.getAttribute("wilayah")));
			if (calonSiswa.getId() == null) {
				calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
			}
			Boolean punyaWali = (Boolean) (mempunyaiWali.getSelectedItem() == null ? false
					: mempunyaiWali.getSelectedItem().getAttribute("nilai"));

			calonSiswa.setMempunyaiWali(punyaWali);

			calonSiswa.setAlamatEmail(alamatEmail.getValue().trim());

			String jenisS = "";
			if (this.selectedKelasLesSiswa != null) {
				for (Long kelasLesSiswa : this.selectedKelasLesSiswa) {
					jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;
				}
			}
			calonSiswa.setKelasLesDipilih(jenisS);
			calonSiswa.setPernyataan(pernyataan.isChecked());
			parameterTambahanListener.onSave(calonSiswa);

			if (calonSiswa.getGelombangPendaftaranPsb() != null
					&& calonSiswa.getGelombangPendaftaranPsb().getHanyaUntukAnakPegawai()
					&& calonSiswa.getOrangTuaPegawai() == null) {
				Tbmuser tbmuser = Common.getCurrentUser();
				if (tbmuser != null && tbmuser.getPegawai() != null) {
					calonSiswa.setOrangTuaPegawai(tbmuser.getPegawai());
				}
			}
			if (alumni != null && alumni.getId() != null) {
				Common.copyDataJikaKosong(alumni, calonSiswa, Siswa.class, CalonSiswa.class);
			}

			if (keluarga != null && keluarga.getParent() != null && keluarga.getParent().isVisible()) {
				Keluarga k = (Keluarga) (keluarga.getSelectedItem() == null ? null
						: keluarga.getSelectedItem().getValue());
				calonSiswa.setKeluarga(k);
			}

		} catch (Exception e) {
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB2.java:2757");
		}
	}

}
