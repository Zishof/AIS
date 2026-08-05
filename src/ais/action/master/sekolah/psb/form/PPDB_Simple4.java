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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

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
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Pegawai;
import ais.database.model.Tbmuser;
import ais.database.model.employ.Keluarga;
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

public class PPDB_Simple4 extends PPDB {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Textbox nama;
	private Textbox tempatLahir;
	private MyDatebox tanggalLahir;
	private Textbox namaAyah;
	private Textbox namaIbu;

	private MyCheckboxConfig pernyataan;

	private Textbox hp1ayah;

	private Textbox hp1ibu;

	private Textbox teleponSiswa;

	protected TreeSet<Long> selectedKelasLesSiswa;

	private Combobox penjurusanSekolah;

	private Row rowParameterTambahan;

	private ArrayList<Row> parameterRows;

	private ParameterTambahanPsbListener parameterTambahanListener;

	private Rows subRowsVerifikasiKelengkapanCalonSiswa;

	private Rows subRowsVerifikasiNilaiRapor;

	private List<Rows> subRowsVerifikasiNilaiParameter;

	private Map<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();

	protected TreeSet<Long> hapusKelasLesSiswa;

	private Textbox alamatSiswa;

	private Combobox jenisKelamin;

	private MyDatebox tanggalLahirAyah;

	private MyDatebox tanggalLahirIbu;

	private Combobox paketPsb;

	private Box infoKampusDariMana;

	private Textbox namaTemanInfoKampusDariMana;

	private Textbox keteranganInfoKampusDariMana;

	private MyCheckboxConfig merupakanPindahan;

	private Textbox pindahanDariSekolah;

	private Textbox alamatSekolahPindahan;

	private Textbox keteranganPindah;

	private MyDatebox tanggalPindah;

	private Textbox alamatEmail;

	private Textbox kelasSekolahPindahan;

	private AmbilDataSiswaBanbox siswaAlumni;

	private Combobox keluarga;
	
	
	private EventListener checkKesamaan = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			CalonSiswaAction.CheckKesamaan checkKesamaan = new CalonSiswaAction.CheckKesamaan(calonSiswa,
					gelombangPendaftaranPsb, tanggalLahir, nama, namaIbu, PPDB_Simple4.this, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							CalonSiswa calonSiswa1 = (CalonSiswa) arg0.getData();
							PPDB window = new PPDB_Simple4();
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

	public PPDB_Simple4() {
		super();
	}

	public PPDB_Simple4(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb,
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
		column.setWidth("35%");

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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple4.java:263");
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
				if (teleponSiswa != null && teleponSiswa.getValue().trim().isEmpty()) {
					teleponSiswa.setValue(s.getTeleponSiswa());
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

		MyGroupConfig myRowStyled = new MyGroupConfig("A. REGISTRASI PESERTA DIDIK");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("1. Jenis pendaftaran *"));
		Label lbl;
		row.appendChild(lbl = new Label(gelombangPendaftaranPsb.getNama()));

		penjurusanSekolah = new Combobox();

		if (gelombangPendaftaranPsb.getPenjurusanSekolah() != null) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig(" Penjurusan"));
			row.appendChild(new Label(gelombangPendaftaranPsb.getPenjurusanSekolah().getNama()));
		} else {
			row = new MyFormRow();
			row.setVisible(false);
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("1. Penjurusan *"));
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

				lbl.getParent().setVisible(!penjurusanSekolah.getParent().isVisible());
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

		myRowStyled = new MyGroupConfig("B. DATA PESERTA DIDIK");
		myRowStyled.setParent(rows);

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.appendChild(new MyLabelAgakKecilBoldBiru("Isi data sesuai dengan dokumen yang berlaku (Akte Kelahiran)"));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("2. Nama lengkap *"));
		row.appendChild(nama = new Textbox(calonSiswa.getNama()));
		nama.setWidth("90%");
		nama.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("3. Tempat lahir *"));
		row.appendChild(tempatLahir = new Textbox(calonSiswa.getTempatLahir()));
		tempatLahir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("4. Tanggal lahir *"));
		row.appendChild(tanggalLahir = new MyDatebox(calonSiswa.getTanggalLahir()));

		EventListener eventListenerTanggalLahir = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				GelombangPendaftaranPsb.chekUmur(gelombangPendaftaranPsb, tanggalLahir);
				checkKesamaan.onEvent(arg0); 
			}
		};

		tanggalLahir.addEventListener("onChange", eventListenerTanggalLahir);

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
		row.appendChild(new ais.ui.util.MyLabelConfig("5. Jenis kelamin *"));
		Common.selectComboItem(jenisKelamin, calonSiswa.getJenisKelamin());
		row.appendChild(jenisKelamin);
		jenisKelamin.setWidth("90%");
		jenisKelamin.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("6. Nama ayah kandung *"));
		row.appendChild(namaAyah = new Textbox(calonSiswa.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("7. Tanggal lahir ayah kandung *"));
		row.appendChild(tanggalLahirAyah = new MyDatebox(calonSiswa.getTanggalLahirAyah()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("8. Nama ibu kandung *"));
		row.appendChild(namaIbu = new Textbox(calonSiswa.getNamaIbu()));
		namaIbu.setWidth("90%");
		namaIbu.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("9. Tanggal lahir ibu kandung *"));
		row.appendChild(tanggalLahirIbu = new MyDatebox(calonSiswa.getTanggalLahirIbu()));

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("10. Telp/WA ayah *"));
		row.appendChild(hp1ayah = new Textbox(calonSiswa.getHp1ayah()));
		hp1ayah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("11. Telp/WA ibu *"));
		row.appendChild(hp1ibu = new Textbox(calonSiswa.getHp1ibu()));
		hp1ibu.setWidth("90%");

		Common.initKeterangan(rows, "Jika ibu tidak memiliki telp/WA, isi dengan nomor telpon ayah");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("12. Telp/WA siswa *"));
		row.appendChild(teleponSiswa = new Textbox(calonSiswa.getTeleponSiswa()));
		teleponSiswa.setWidth("90%");

		Common.initKeterangan(rows, "Jika siswa tidak memiliki telp/WA, isi dengan nomor telpon ayah/ibu");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("13. Alamat *"));
		row.appendChild(alamatSiswa = new Textbox(calonSiswa.getAlamatSiswa()));
		alamatSiswa.setWidth("90%");
		alamatSiswa.setRows(3);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("14. Alamat Email *"));
		row.appendChild(alamatEmail = new Textbox(calonSiswa.getAlamatEmail()));
		alamatEmail.setWidth("90%");

		final MyGroupConfig myRowStyledKelas = new MyGroupConfig("C. KELAS YANG PILIH");
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
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple4.java:626");
			}

		}

		try {
			parameterTambahanListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple4.java:635");
		}

		try {
			Component[] c = CalonSiswaAction.infoPindahan(rows, calonSiswa);
			merupakanPindahan = (MyCheckboxConfig) c[0];
			pindahanDariSekolah = (Textbox) c[1];
			alamatSekolahPindahan = (Textbox) c[2];
			keteranganPindah = (Textbox) c[3];
			tanggalPindah = (MyDatebox) c[4];
			kelasSekolahPindahan = (Textbox) c[5];

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple4.java:649");
		}

		try {
			Component[] c = CalonSiswaAction.infoDariMana(rows, calonSiswa);
			infoKampusDariMana = (Box) c[0];
			namaTemanInfoKampusDariMana = (Textbox) c[1];
			keteranganInfoKampusDariMana = (Textbox) c[2];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple4.java:659");
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
				PPDB_Simple4.this.detach();
			}
		});
		cancel.setParent(toolbar);

		final boolean daftar = tbmuser == null;
		final MyButtonConfig save = new MyButtonConfig(daftar ? "D A F T A R" : "S I M P A N", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				baru = false;
				if (onSave(event)) {
					PPDB_Simple4.this.detach();

					if (PPDB_Simple4.this.eventListener != null) {
						PPDB_Simple4.this.eventListener.onEvent(new Event("", save, PPDB_Simple4.this.calonSiswa));
					}

					if (baru) {
						String informasi = Common.getKonfigurasi("informasi_registrasi_psb_berhasil_login",
								"Proses pendaftaran peserta didik baru berhasil dilakukan dengan nomor pendaftaran : [no_reg]. Silahkan catat nomor pendaftaran tersebut dan selanjutnya akan diarahkan ke Login.")
								.getNilai();
						informasi = org.apache.commons.lang3.StringUtils.replace(informasi, "[no_reg]",
								PPDB_Simple4.this.calonSiswa.getNoRegistrasi());
						MyMessageboxConfig.show(informasi, "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												if (baru && PPDB_Simple4.this.calonSiswa.getGelombangPendaftaranPsb()
														.getOtomatisLoginSetelahDaftar()) {
													PPDB_Simple4.this.calonSiswa.setTelahLogin(true);
													PPDB_Simple4.this.calonSiswa
															.setWaktuLogin(ais.ui.util.WaktuUtil.getDate());
													Common.refreshUpdate(PPDB_Simple4.this.calonSiswa);

													Common.setLogin(PPDB_Simple4.this.calonSiswa);
													Sessions.getCurrent(true).setAttribute("cetak", true);
													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Executions.getCurrent().sendRedirect("");
														}
													});
												} else {
													CalonSiswaAction.onCetakKartu(PPDB_Simple4.this.calonSiswa, daftar);
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

	private EventListener masukkanPerubahan = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			setdata();
		}
	};

	private boolean baru;

	public boolean onSave(Event event) throws Exception {

		Sekolah s = gelombangPendaftaranPsb == null ? null : gelombangPendaftaranPsb.getSekolah();
		if (s != null && s.getPenjurusanBolehDipilihSaatPsb()) {
			if (penjurusanSekolah.getParent() != null && penjurusanSekolah.getParent().isVisible()
					&& (penjurusanSekolah.getSelectedItem() == null
							|| penjurusanSekolah.getSelectedItem().getValue() == null)) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu memilih Penjurusan sebelum melanjutkan pendaftaran. Langkah yang dapat dilakukan: (1) buka pilihan Penjurusan; (2) pilih salah satu penjurusan yang tersedia; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								penjurusanSekolah.focus();
								penjurusanSekolah.select();
								Clients.scrollIntoView(penjurusanSekolah);
							}
						});
				return false;
			}
		}

		if (gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getHarusSebagaiAlumni()) {
			Siswa alumni = (Siswa) (siswaAlumni == null ? null : siswaAlumni.getAttribute("siswa"));
			if (alumni == null) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi data Siswa Alumni sebelum melanjutkan pendaftaran. Langkah yang dapat dilakukan: (1) klik kolom Siswa Alumni; (2) pilih data siswa alumni yang sesuai; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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

		if (keluarga != null && keluarga.getParent() != null && keluarga.getParent().isVisible()) {
			Keluarga k = (Keluarga) (keluarga.getSelectedItem() == null ? null : keluarga.getSelectedItem().getValue());
			if (k == null) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu memilih data Anak Pegawai sebelum melanjutkan pendaftaran. Langkah yang dapat dilakukan: (1) buka pilihan Anak Pegawai; (2) pilih data yang sesuai; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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

		if (paketPsb.getParent() != null && paketPsb.getParent().isVisible()
				&& (paketPsb.getSelectedItem() == null || paketPsb.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu memilih Paket sebelum melanjutkan pendaftaran. Langkah yang dapat dilakukan: (1) buka pilihan Paket; (2) pilih paket yang tersedia; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Nama Lengkap calon siswa. Langkah yang dapat dilakukan: (1) klik kolom Nama Lengkap; (2) isikan nama sesuai dokumen resmi (Akte Kelahiran); (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							nama.focus();
							nama.select();
							Clients.scrollIntoView(nama);
						}
					});
			return false;
		}

		if (tempatLahir.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Tempat Lahir calon siswa. Langkah yang dapat dilakukan: (1) klik kolom Tempat Lahir; (2) isikan tempat lahir sesuai dokumen resmi (Akte Kelahiran); (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							tempatLahir.focus();
							tempatLahir.select();
							Clients.scrollIntoView(tempatLahir);
						}
					});
			return false;
		}

		if (tanggalLahir.getValue() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Tanggal Lahir calon siswa. Langkah yang dapat dilakukan: (1) klik kolom Tanggal Lahir; (2) pilih tanggal lahir sesuai dokumen resmi (Akte Kelahiran); (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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

		if (jenisKelamin.getSelectedItem() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu memilih Jenis Kelamin calon siswa. Langkah yang dapat dilakukan: (1) buka pilihan Jenis Kelamin; (2) pilih Laki-laki atau Perempuan sesuai dokumen resmi; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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

		if (namaAyah.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Nama Ayah Kandung. Langkah yang dapat dilakukan: (1) klik kolom Nama Ayah Kandung; (2) isikan nama sesuai dokumen resmi; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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

		if (tanggalLahirAyah.getValue() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Tanggal Lahir Ayah Kandung. Langkah yang dapat dilakukan: (1) klik kolom Tanggal Lahir Ayah Kandung; (2) pilih tanggal lahir sesuai dokumen resmi; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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

		if (namaIbu.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Nama Ibu Kandung. Langkah yang dapat dilakukan: (1) klik kolom Nama Ibu Kandung; (2) isikan nama sesuai dokumen resmi; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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

		if (tanggalLahirIbu.getValue() == null) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Tanggal Lahir Ibu Kandung. Langkah yang dapat dilakukan: (1) klik kolom Tanggal Lahir Ibu Kandung; (2) pilih tanggal lahir sesuai dokumen resmi; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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

		if (hp1ayah.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Telepon/Nomor WhatsApp Ayah. Apabila Ayah tidak memiliki telepon/WhatsApp, mohon diisi dengan nomor Ibu atau anak yang dapat dihubungi. Langkah yang dapat dilakukan: (1) klik kolom Telepon/WA Ayah; (2) isikan nomor yang aktif dan dapat dihubungi; (3) lanjutkan menyimpan pendaftaran.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							hp1ayah.focus();
							hp1ayah.select();
							Clients.scrollIntoView(hp1ayah);
						}
					});
			return false;
		}

		if (hp1ibu.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Telepon/Nomor WhatsApp Ibu. Apabila Ibu tidak memiliki telepon/WhatsApp, mohon diisi dengan nomor Ayah yang dapat dihubungi. Langkah yang dapat dilakukan: (1) klik kolom Telepon/WA Ibu; (2) isikan nomor yang aktif dan dapat dihubungi; (3) lanjutkan menyimpan pendaftaran.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							hp1ibu.focus();
							hp1ibu.select();
							Clients.scrollIntoView(hp1ibu);
						}
					});
			return false;
		}

		if (teleponSiswa.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Mohon Bapak/Ibu terlebih dahulu mengisi Telepon/Nomor WhatsApp Siswa. Apabila siswa tidak memiliki telepon/WhatsApp, mohon diisi dengan nomor Ayah/Ibu yang dapat dihubungi. Langkah yang dapat dilakukan: (1) klik kolom Telepon/WA Siswa; (2) isikan nomor yang aktif dan dapat dihubungi; (3) lanjutkan menyimpan pendaftaran.",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							teleponSiswa.focus();
							teleponSiswa.select();
							Clients.scrollIntoView(teleponSiswa);
						}
					});
			return false;
		}

		if (alamatEmail.getValue().trim().equals("") || !Common.isValidEmailAddress(alamatEmail.getValue().trim())) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu mengisi Alamat Email dengan benar dan lengkap. Langkah yang dapat dilakukan: (1) klik kolom Alamat Email; (2) isikan alamat email yang valid, misalnya nama@domain.com; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							alamatEmail.focus();
							alamatEmail.select();
							Clients.scrollIntoView(alamatEmail);
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
								"Mohon maaf, umur maksimal calon siswa yang diperbolehkan untuk mendaftar adalah {V1} tahun, sedangkan umur berdasarkan data yang Bapak/Ibu masukkan adalah {V2} tahun. Langkah yang dapat dilakukan: (1) periksa kembali Tanggal Lahir yang telah diisi; (2) pastikan tanggal lahir sesuai dokumen resmi (Akte Kelahiran); (3) perbaiki bila terdapat kekeliruan.",
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
								"Mohon maaf, umur minimal calon siswa yang diperbolehkan untuk mendaftar adalah {V1} tahun, sedangkan umur berdasarkan data yang Bapak/Ibu masukkan adalah {V2} tahun. Langkah yang dapat dilakukan: (1) periksa kembali Tanggal Lahir yang telah diisi; (2) pastikan tanggal lahir sesuai dokumen resmi (Akte Kelahiran); (3) perbaiki bila terdapat kekeliruan.",
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

		if (merupakanPindahan.isChecked()) {
			if (pindahanDariSekolah != null && pindahanDariSekolah.getParent() != null
					&& pindahanDariSekolah.getParent().isVisible()
					&& pindahanDariSekolah.getValue().trim().equals("")) {
				MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Asal Sekolah sebelumnya karena calon siswa ditandai sebagai pindahan. Langkah yang dapat dilakukan: (1) klik kolom Asal Sekolah; (2) isikan nama sekolah asal secara lengkap; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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
				MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Kelas Terakhir pada sekolah sebelumnya. Langkah yang dapat dilakukan: (1) klik kolom Kelas Terakhir; (2) isikan kelas terakhir yang ditempuh; (3) lanjutkan menyimpan pendaftaran.", "Peringatan",
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
				MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Alamat Sekolah sebelumnya. Langkah yang dapat dilakukan: (1) klik kolom Alamat Sekolah; (2) isikan alamat sekolah asal secara lengkap; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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
				MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu mengisi Tanggal Pindah. Langkah yang dapat dilakukan: (1) klik kolom Tanggal Pindah; (2) pilih tanggal kepindahan; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
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
						"Mohon maaf, calon siswa atas nama \"{V1}\" dengan tanggal lahir \"{V2}\" sudah terdaftar pada tahun pelajaran \"{V3}\". Untuk informasi lebih lanjut, mohon Bapak/Ibu menghubungi {V4}. Langkah yang dapat dilakukan: (1) periksa kembali data nama dan tanggal lahir; (2) pastikan calon siswa belum pernah didaftarkan sebelumnya; (3) hubungi admin apabila memerlukan bantuan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								tanggalLahir.focus();
								tanggalLahir.select();
								Clients.scrollIntoView(tanggalLahir);
							}
						}, nama.getValue().trim(), Common.dateFormat2.get().format(tanggalLahir.getValue()),
						gelombangPendaftaranPsb.getTahunAjaran(),
						Common.getKonfigurasi("hubungi_admin_calon_mhs",
								"Silahkan hubungi admin di Nomor \r\n" + "WA : ...... / ..... \r\n"
										+ "atau email : .....\r\n" + "")
								.getNilai());

				return false;
			}
		}

		if (!CalonSiswaAction.checkInfoDariMana(infoKampusDariMana, namaTemanInfoKampusDariMana,
				keteranganInfoKampusDariMana)) {
			return false;
		}

		if (!pernyataan.isChecked()) {
			MyMessageboxConfig.show("Mohon Bapak/Ibu terlebih dahulu menyetujui Pernyataan yang tersedia sebelum menyimpan pendaftaran. Langkah yang dapat dilakukan: (1) baca Pernyataan dengan saksama; (2) centang kotak Pernyataan sebagai tanda persetujuan; (3) lanjutkan menyimpan pendaftaran.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							pernyataan.focus();
							Clients.scrollIntoView(pernyataan);
						}
					});
			pernyataan.focus();
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple4.java:1255");
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
			calonSiswa.setPenjurusanSekolah((PenjurusanSekolah) (penjurusanSekolah.getSelectedItem() == null ? null
					: penjurusanSekolah.getSelectedItem().getValue()));
			calonSiswa.setGelombangPendaftaranPsb(gelombangPendaftaranPsb);
			calonSiswa.setNamaSiswa(nama.getValue());
			calonSiswa.setNamaAyah(namaAyah.getValue());
			calonSiswa.setNamaIbu(namaIbu.getValue());
			calonSiswa.setHp1ayah(hp1ayah.getValue());
			calonSiswa.setHp2ibu(hp1ibu.getValue());
			calonSiswa.setAlamatSiswa(alamatSiswa.getValue().trim());
			calonSiswa.setTeleponSiswa(teleponSiswa.getValue());
			calonSiswa.setTempatLahir(tempatLahir.getValue());
			calonSiswa.setTanggalLahir(tanggalLahir.getValue());
			calonSiswa.setJenisKelamin((String) (jenisKelamin.getSelectedItem() == null ? null
					: jenisKelamin.getSelectedItem().getValue()));
			if (calonSiswa.getId() == null) {
				calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
			}

			String jenisS = "";
			if (this.selectedKelasLesSiswa != null) {
				for (Long kelasLesSiswa : this.selectedKelasLesSiswa) {
					jenisS += jenisS.isEmpty() ? kelasLesSiswa.toString() : "," + kelasLesSiswa;
				}
			}
			calonSiswa.setKelasLesDipilih(jenisS);
			calonSiswa.setPernyataan(pernyataan.isChecked());
			calonSiswa.setPaketPsb(
					(PaketPsb) (paketPsb.getSelectedItem() == null ? null : paketPsb.getSelectedItem().getValue()));
			calonSiswa.setTanggalLahirAyah(tanggalLahirAyah.getValue());
			calonSiswa.setTanggalLahirIbu(tanggalLahirIbu.getValue());
			calonSiswa.setAlamatEmail(alamatEmail.getValue().trim());
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Simple4.java:1330");
		}
	}

}
