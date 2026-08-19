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
import org.zkoss.zul.Messagebox;
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

public class PPDB_Alumni extends PPDB {

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

	private Combobox paketPsb;

	private Box infoKampusDariMana;

	private Textbox namaTemanInfoKampusDariMana;

	private Textbox keteranganInfoKampusDariMana;

	private MyCheckboxConfig merupakanPindahan;

	private Textbox pindahanDariSekolah;

	private Textbox alamatSekolahPindahan;

	private Textbox keteranganPindah;

	private MyDatebox tanggalPindah;

	private Textbox kelasSekolahPindahan;

	private Combobox jenisKelamin;

	private AmbilDataSiswaBanbox siswaAlumni;

	private Combobox keluarga;

	private AmbilDataSiswaBanbox siswaSibling;

	private EventListener checkKesamaan = new EventListener() {

		@Override
		public void onEvent(Event arg0) throws Exception {
			CalonSiswaAction.CheckKesamaan checkKesamaan = new CalonSiswaAction.CheckKesamaan(calonSiswa,
					gelombangPendaftaranPsb, tanggalLahir, nama, namaIbu, PPDB_Alumni.this, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							CalonSiswa calonSiswa1 = (CalonSiswa) arg0.getData();
							PPDB window = new PPDB_Alumni();
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

	public PPDB_Alumni() {
		super();
	}

	public PPDB_Alumni(CalonSiswa calonSiswa, GelombangPendaftaranPsb gelombangPendaftaranPsb,
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Alumni.java:259");
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

		siswaAlumni = PPDB.alumni(calonSiswa, gelombangPendaftaranPsb, rows, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Siswa s = (Siswa) arg0.getData();
				if (nama != null) {
					nama.setValue(s.getNama());
					nama.setDisabled(true);
				}

				if (siswaSibling != null && siswaSibling.getParent() != null) {
					siswaSibling.getParent().setVisible(s == null || s.getId() == null);
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

		siswaSibling = PPDB.sibling(calonSiswa, gelombangPendaftaranPsb, rows, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Siswa s = (Siswa) arg0.getData();

				if (siswaAlumni != null && siswaAlumni.getParent() != null) {
					siswaAlumni.getParent().setVisible(s == null || s.getId() == null);
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
		row.appendChild(new ais.ui.util.MyLabelConfig("3. Jenis Kelamin *"));
		Common.selectComboItem(jenisKelamin, calonSiswa.getJenisKelamin());
		row.appendChild(jenisKelamin);
		jenisKelamin.setWidth("90%");
		jenisKelamin.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("4. Tempat lahir *"));
		row.appendChild(tempatLahir = new Textbox(calonSiswa.getTempatLahir()));
		tempatLahir.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("5. Tanggal lahir *"));
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
		row.appendChild(new ais.ui.util.MyLabelConfig("6. Nama ayah kandung *"));
		row.appendChild(namaAyah = new Textbox(calonSiswa.getNamaAyah()));
		namaAyah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("7. Nama ibu kandung *"));
		row.appendChild(namaIbu = new Textbox(calonSiswa.getNamaIbu()));
		namaIbu.setWidth("90%");
		namaIbu.addEventListener("onChange", checkKesamaan);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("8. Telp/WA ayah *"));
		row.appendChild(hp1ayah = new Textbox(calonSiswa.getHp1ayah()));
		hp1ayah.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("9. Telp/WA ibu *"));
		row.appendChild(hp1ibu = new Textbox(calonSiswa.getHp1ibu()));
		hp1ibu.setWidth("90%");

		Common.initKeterangan(rows, "Jika ibu tidak memiliki telp/WA, isi dengan nomor telpon ayah");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("10. Telp/WA siswa *"));
		row.appendChild(teleponSiswa = new Textbox(calonSiswa.getTeleponSiswa()));
		teleponSiswa.setWidth("90%");

		Common.initKeterangan(rows, "Jika siswa tidak memiliki telp/WA, isi dengan nomor telpon ayah/ibu");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("11. Alamat *"));
		row.appendChild(alamatSiswa = new Textbox(calonSiswa.getAlamatSiswa()));
		alamatSiswa.setWidth("90%");
		alamatSiswa.setRows(3);

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

		/*
		 * Verifikasi Kelengkapan Berkas ikut tampil pada PENDAFTARAN BARU supaya calon
		 * siswa bisa mengunggah berkas sekaligus. Penautan berkas ke baris penghubung
		 * diselesaikan VerifikasiPSBHelper.simpanVerifikasi() setelah data tersimpan.
		 */
		try {
			subRowsVerifikasiKelengkapanCalonSiswa = VerifikasiPSBHelper.tampilkanVerifikasi(calonSiswa, rows, null,
					calonSiswa.getId() != null ? calonSiswa.getGelombangPendaftaranPsb() : gelombangPendaftaranPsb);
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e, "PPDB_Alumni.tampilkanVerifikasiBerkas");
		}

		if (calonSiswa.getId() != null) {
			try {
				subRowsVerifikasiNilaiRapor = VerifikasiMatapelajaranPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
						gelombangPendaftaranPsb, null);

				subRowsVerifikasiNilaiParameter = VerifikasiParameterPSBHelper.tampilkanVerifikasi(calonSiswa, rows,
						null, calonSiswa.getGelombangPendaftaranPsb());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Alumni.java:641");
			}

		}

		try {
			parameterTambahanListener.onEvent(null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Alumni.java:650");
		}

		try {
			Component[] c = CalonSiswaAction.infoDariMana(rows, calonSiswa);
			infoKampusDariMana = (Box) c[0];
			namaTemanInfoKampusDariMana = (Textbox) c[1];
			keteranganInfoKampusDariMana = (Textbox) c[2];
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Alumni.java:660");
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Alumni.java:673");
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
				PPDB_Alumni.this.detach();
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
					PPDB_Alumni.this.detach();

					if (PPDB_Alumni.this.eventListener != null) {
						PPDB_Alumni.this.eventListener.onEvent(new Event("", save, PPDB_Alumni.this.calonSiswa));
					}

					if (baru) {
						String informasi = Common.getKonfigurasi("informasi_registrasi_psb_berhasil_login",
								"Proses pendaftaran peserta didik baru berhasil dilakukan dengan nomor pendaftaran : [no_reg]. Silahkan catat nomor pendaftaran tersebut dan selanjutnya akan diarahkan ke Login.")
								.getNilai();
						informasi = org.apache.commons.lang3.StringUtils.replace(informasi, "[no_reg]",
								PPDB_Alumni.this.calonSiswa.getNoRegistrasi());
						MyMessageboxConfig.show(informasi, "Informasi", MyMessageboxConfig.OK,
								MyMessageboxConfig.INFORMATION, new EventListener() {

									@Override
									public void onEvent(Event arg0) throws Exception {

										Common.createDefaultTimer(new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												if (baru && PPDB_Alumni.this.calonSiswa.getGelombangPendaftaranPsb()
														.getOtomatisLoginSetelahDaftar()) {
													PPDB_Alumni.this.calonSiswa.setTelahLogin(true);
													PPDB_Alumni.this.calonSiswa
															.setWaktuLogin(ais.ui.util.WaktuUtil.getDate());
													Common.refreshUpdate(PPDB_Alumni.this.calonSiswa);

													Common.setLogin(PPDB_Alumni.this.calonSiswa);
													Sessions.getCurrent(true).setAttribute("cetak", true);
													Common.createDefaultTimer(new EventListener() {

														@Override
														public void onEvent(Event arg0) throws Exception {
															Executions.getCurrent().sendRedirect("");
														}
													});
												} else {
													CalonSiswaAction.onCetakKartu(PPDB_Alumni.this.calonSiswa, daftar);
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
				MyMessageboxConfig.show("Penjurusan harus dipilih. Langkah yang dapat dilakukan: (1) pilih Penjurusan yang sesuai; (2) pastikan pilihan telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
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

		Siswa alumni = (Siswa) (siswaAlumni == null ? null : siswaAlumni.getAttribute("siswa"));
		Siswa sibling = (Siswa) (siswaSibling == null ? null : siswaSibling.getAttribute("siswa"));

		if (gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getHarusSebagaiAlumni() && sibling == null) {

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

		if (gelombangPendaftaranPsb != null && gelombangPendaftaranPsb.getHarusSebagaiSaudara() && alumni == null) {

			if (sibling == null) {
				MyMessageboxConfig.show("Data Siswa Saudara (Sibling) harus diisi. Langkah yang dapat dilakukan: (1) pilih data siswa saudara yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION, new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								siswaSibling.focus();
								siswaSibling.select();
								Clients.scrollIntoView(siswaSibling);
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

		if (paketPsb.getParent() != null && paketPsb.getParent().isVisible()
				&& (paketPsb.getSelectedItem() == null || paketPsb.getSelectedItem().getValue() == null)) {
			MyMessageboxConfig.show("Paket harus dipilih. Langkah yang dapat dilakukan: (1) pilih Paket yang sesuai; (2) pastikan pilihan telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (nama.getValue().trim().equals("")) {
			MyMessageboxConfig.show("Nama Calon Siswa harus diisi. Langkah yang dapat dilakukan: (1) lengkapi kolom Nama Calon Siswa; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
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

		if (jenisKelamin.getSelectedItem() == null) {
			MyMessageboxConfig.show("Jenis Kelamin harus diisi. Langkah yang dapat dilakukan: (1) pilih Jenis Kelamin yang sesuai; (2) pastikan data telah benar; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							jenisKelamin.focus();
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
							tempatLahir.select();
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

		if (namaAyah.getValue().trim().equals("")) {
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

		if (namaIbu.getValue().trim().equals("")) {
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

		if (hp1ayah.getValue().trim().equals("")) {
			MyMessageboxConfig.show(
					"Nomor Telepon/WA Ayah harus diisi. Apabila ayah tidak memiliki telepon/WA, isi dengan nomor telepon ibu/anak atau nomor lain yang dapat dihubungi. Langkah yang dapat dilakukan: (1) lengkapi kolom Telepon/WA Ayah; (2) pastikan nomor dapat dihubungi; (3) simpan kembali formulir.",
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
					"Nomor Telepon/WA Ibu harus diisi. Apabila ibu tidak memiliki telepon/WA, isi dengan nomor telepon ayah atau nomor lain yang dapat dihubungi. Langkah yang dapat dilakukan: (1) lengkapi kolom Telepon/WA Ibu; (2) pastikan nomor dapat dihubungi; (3) simpan kembali formulir.",
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
					"Nomor Telepon/WA Siswa harus diisi. Apabila siswa tidak memiliki telepon/WA, isi dengan nomor telepon ayah/ibu atau nomor lain yang dapat dihubungi. Langkah yang dapat dilakukan: (1) lengkapi kolom Telepon/WA Siswa; (2) pastikan nomor dapat dihubungi; (3) simpan kembali formulir.",
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

		if (!pernyataan.isChecked()) {
			MyMessageboxConfig.show("Pernyataan persetujuan harus dicentang. Langkah yang dapat dilakukan: (1) baca pernyataan yang tersedia; (2) centang kolom pernyataan persetujuan; (3) simpan kembali formulir.", "Peringatan", MyMessageboxConfig.OK,
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Alumni.java:1243");
		}

		return true;
	}

	private void setdata() {

		try {
			String info = BiodataCalonMahasiswaAction.infoMahasiswaBaru(infoKampusDariMana);
			Siswa alumni = (Siswa) (siswaAlumni == null ? null : siswaAlumni.getAttribute("siswa"));
			Siswa sibling = (Siswa) (siswaSibling == null ? null : siswaSibling.getAttribute("siswa"));
			calonSiswa.setSiswaAlumni(alumni);
			calonSiswa.setSiswaSibling(sibling);
			calonSiswa.setKelasSekolahPindahan(kelasSekolahPindahan.getValue().trim());
			calonSiswa.setMerupakanPindahan(merupakanPindahan.isChecked());
			calonSiswa.setPindahanDariSekolah(pindahanDariSekolah.getValue());
			calonSiswa.setAlamatSekolahPindahan(alamatSekolahPindahan.getValue());
			calonSiswa.setKeteranganPindah(keteranganPindah.getValue());
			calonSiswa.setTanggalPindah(tanggalPindah.getValue());
			calonSiswa.setJenisKelamin(jenisKelamin.getValue());
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
			if (calonSiswa.getId() == null) {
				calonSiswa.setNoRegistrasi(CommonPSB.generateNoRegistrasi(calonSiswa));
			}
			calonSiswa.setPaketPsb(
					(PaketPsb) (paketPsb.getSelectedItem() == null ? null : paketPsb.getSelectedItem().getValue()));
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
			e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/sekolah/psb/form/PPDB_Alumni.java:1313");
		}
	}

}
