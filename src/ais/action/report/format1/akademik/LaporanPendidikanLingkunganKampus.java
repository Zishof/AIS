package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.LogicalExpression;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.West;

import ais.action.master.SertifikatAction;
import ais.action.master.SyaratUjianAction;
import ais.action.master.helper.AmbilDataDosenBanbox;
import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataGuruBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.BarcodeCommon;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.Dosen;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.ItemBiaya;
import ais.database.model.JenisFormulirKegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Siswa;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyWindow;

public class LaporanPendidikanLingkunganKampus extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = -397946194166101691L;
	private AmbilDataMahasiswaBanbox bandboxMahasiswa;
	private AmbilDataDosenBanbox bandboxDosen;

	private AmbilDataSiswaBanbox bandboxSiswa;
	private AmbilDataGuruBanbox bandboxGuru;

	private Radiogroup formulirKegiatan;
	private Center center;
	private Toolbar toolbar;
	private FormulirKegiatanPeserta formulirKegiatanPeserta = null;
	private FormulirKegiatanPeserta formulirKegiatanPesertamy = null;
	private boolean pt;
	private boolean ya;
	private JenisFormulirKegiatan jenisFormulirKegiatan = null;
	private MyButtonConfig tombolPilih;
	private FormulirKegiatan myFormulirKegiatan;
	private MyButtonConfig tombol;

	public LaporanPendidikanLingkunganKampus() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pendidikan Lingkungan Kampus", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPendidikanLingkunganKampus(JenisFormulirKegiatan jenisFormulirKegiatan) {
		super();
		this.jenisFormulirKegiatan = jenisFormulirKegiatan;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pendidikan Lingkungan Kampus", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPendidikanLingkunganKampus(FormulirKegiatanPeserta formulirKegiatanPeserta) {
		super();
		this.formulirKegiatanPeserta = formulirKegiatanPeserta;
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Pendidikan Lingkungan Kampus", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanPendidikanLingkunganKampus(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		boolean[] ptYa = Common.chekPtAtauSekolah();
		pt = ptYa[0];
		ya = ptYa[1];

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				if (bandboxDosen.getAttribute("dosen") != null) {
					bandboxMahasiswa.getParent().setVisible(false);
					Dosen dosen = (Dosen) bandboxDosen.getAttribute("dosen");
					Criterion criterion = Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true));
					criterion = Restrictions.and(criterion,
							Restrictions.or(Restrictions.eq("fakultas", dosen.getJurusan().getFakultas()),
									Restrictions.isNull("fakultas")));
					criterion = Restrictions.and(criterion, Restrictions
							.or(Restrictions.eq("jurusan", dosen.getJurusan()), Restrictions.isNull("jurusan")));

					criterion = Restrictions.and(criterion, Restrictions.or(
							Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()), Restrictions.isNull("mulai")));

					criterion = Restrictions.and(criterion, Restrictions.or(
							Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate()), Restrictions.isNull("sampai")));

					criterion = Restrictions.and(criterion, Restrictions.or(Restrictions.eq("pesertaDosen", true),
							Restrictions.isNull("pesertaDosen")));

					criterion = Restrictions.and(criterion,
							jenisFormulirKegiatan == null ? Restrictions.isNull("jenisFormulirKegiatan")
									: Restrictions.eq("jenisFormulirKegiatan", jenisFormulirKegiatan));

					Common.insertRadio(formulirKegiatan, new String[] { "nama" }, "keterangan", FormulirKegiatan.class,
							criterion);
				} else if (bandboxMahasiswa.getAttribute("mahasiswa") != null) {
					bandboxDosen.getParent().setVisible(false);
					Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
					Criterion criterion = Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true));
					criterion = Restrictions.and(criterion,
							Restrictions.or(Restrictions.eq("fakultas", mahasiswa.getJurusan().getFakultas()),
									Restrictions.isNull("fakultas")));
					criterion = Restrictions.and(criterion, Restrictions
							.or(Restrictions.eq("jurusan", mahasiswa.getJurusan()), Restrictions.isNull("jurusan")));

					criterion = Restrictions.and(criterion, Restrictions
							.or(Restrictions.eq("program", mahasiswa.getProgram()), Restrictions.isNull("program")));

					criterion = Restrictions.and(criterion, Restrictions.or(
							Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()), Restrictions.isNull("mulai")));

					criterion = Restrictions.and(criterion, Restrictions.or(
							Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate()), Restrictions.isNull("sampai")));

					criterion = Restrictions.and(criterion, Restrictions.or(Restrictions.eq("pesertaMahasiswa", true),
							Restrictions.isNull("pesertaMahasiswa")));

					if (!mahasiswa.getMerupakanPindahan() && !mahasiswa.getMerupakanAlihProdi()) {
						LogicalExpression tambahan = Restrictions.or(Restrictions.isNull("hanyaUntukAngkatan"),
								Restrictions.ilike("hanyaUntukAngkatan", "," + mahasiswa.getTahunangkatan() + ",",
										MatchMode.ANYWHERE));
						tambahan = Restrictions.or(tambahan, Restrictions.eq("hanyaUntukAngkatan", ""));
						tambahan = Restrictions.or(tambahan, Restrictions.isNull("hanyaUntukAngkatan"));
						criterion = Restrictions.and(criterion, tambahan);
					}

					criterion = Restrictions.and(criterion,
							jenisFormulirKegiatan == null ? Restrictions.isNull("jenisFormulirKegiatan")
									: Restrictions.eq("jenisFormulirKegiatan", jenisFormulirKegiatan));

					Common.insertRadio(formulirKegiatan, new String[] { "nama" }, "keterangan", FormulirKegiatan.class,
							criterion);
				}

				else if (bandboxGuru.getAttribute("guru") != null) {
					bandboxSiswa.getParent().setVisible(false);
					Guru guru = (Guru) bandboxGuru.getAttribute("guru");
					Criterion criterion = Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true));
					criterion = Restrictions.and(criterion,
							Restrictions.or(Restrictions.eq("yayasan", guru.getSekolah().getYayasan()),
									Restrictions.isNull("yayasan")));
					criterion = Restrictions.and(criterion, Restrictions
							.or(Restrictions.eq("sekolah", guru.getSekolah()), Restrictions.isNull("sekolah")));

					criterion = Restrictions.and(criterion, Restrictions.or(
							Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()), Restrictions.isNull("mulai")));

					criterion = Restrictions.and(criterion, Restrictions.or(
							Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate()), Restrictions.isNull("sampai")));

					criterion = Restrictions.and(criterion,
							Restrictions.or(Restrictions.eq("pesertaGuru", true), Restrictions.isNull("pesertaGuru")));

					criterion = Restrictions.and(criterion,
							jenisFormulirKegiatan == null ? Restrictions.isNull("jenisFormulirKegiatan")
									: Restrictions.eq("jenisFormulirKegiatan", jenisFormulirKegiatan));

					Common.insertRadio(formulirKegiatan, new String[] { "nama" }, "keterangan", FormulirKegiatan.class,
							criterion);
				} else if (bandboxSiswa.getAttribute("siswa") != null) {
					bandboxGuru.getParent().setVisible(false);
					Siswa siswa = (Siswa) bandboxSiswa.getAttribute("siswa");
					Criterion criterion = Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true));
					criterion = Restrictions.and(criterion,
							Restrictions.or(Restrictions.eq("yayasan", siswa.getSekolah().getYayasan()),
									Restrictions.isNull("yayasan")));
					criterion = Restrictions.and(criterion, Restrictions
							.or(Restrictions.eq("sekolah", siswa.getSekolah()), Restrictions.isNull("sekolah")));

					criterion = Restrictions.and(criterion, Restrictions
							.or(Restrictions.eq("program", siswa.getProgram()), Restrictions.isNull("program")));

					criterion = Restrictions.and(criterion, Restrictions.or(
							Restrictions.le("mulai", ais.ui.util.WaktuUtil.getDate()), Restrictions.isNull("mulai")));

					criterion = Restrictions.and(criterion, Restrictions.or(
							Restrictions.ge("sampai", ais.ui.util.WaktuUtil.getDate()), Restrictions.isNull("sampai")));

					criterion = Restrictions.and(criterion, Restrictions.or(Restrictions.eq("pesertaSiswa", true),
							Restrictions.isNull("pesertaSiswa")));

					LogicalExpression tambahan = Restrictions.or(Restrictions.isNull("hanyaUntukAngkatan"), Restrictions
							.ilike("hanyaUntukAngkatan", "," + siswa.getTahunMasuk() + ",", MatchMode.ANYWHERE));
					tambahan = Restrictions.or(tambahan, Restrictions.eq("hanyaUntukAngkatan", ""));
					tambahan = Restrictions.or(tambahan, Restrictions.isNull("hanyaUntukAngkatan"));
					criterion = Restrictions.and(criterion, tambahan);

					criterion = Restrictions.and(criterion,
							jenisFormulirKegiatan == null ? Restrictions.isNull("jenisFormulirKegiatan")
									: Restrictions.eq("jenisFormulirKegiatan", jenisFormulirKegiatan));

					Common.insertRadio(formulirKegiatan, new String[] { "nama" }, "keterangan", FormulirKegiatan.class,
							criterion);
				}

				onKHS(event);

			}
		};

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
		west.setVisible(formulirKegiatanPeserta == null);
		west.setTitle("Menu");
		west.setCollapsible(true);
		west.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(west, true);
		west.setWidth("350px");

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(west);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);
		MyColumnConfig column = new MyColumnConfig();
		column.setWidth("25%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);
		Tbmuser tbmuser = Common.getCurrentUser();

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(tbmuser != null && tbmuser.ambilDosen() == null && pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa"));
		row.appendChild(bandboxMahasiswa = new AmbilDataMahasiswaBanbox());
		bandboxMahasiswa.setWidth("90%");

		if (formulirKegiatanPeserta != null && formulirKegiatanPeserta.getMahasiswa() != null) {
			Mahasiswa mahasiswa = formulirKegiatanPeserta.getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			bandboxMahasiswa.setDisabled(true);
		}

		else if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
			bandboxMahasiswa.setAttribute("mahasiswa", mahasiswa);
			bandboxMahasiswa.setAttribute("myValue", mahasiswa);
			bandboxMahasiswa.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			bandboxMahasiswa.setId("mhs_" + mahasiswa.getId());
			bandboxMahasiswa.setDisabled(true);
		}

		bandboxMahasiswa.setEventListener(eventListener);

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && pt);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(bandboxDosen = new AmbilDataDosenBanbox());
		bandboxDosen.setWidth("90%");

		if (formulirKegiatanPeserta != null && formulirKegiatanPeserta.getDosen() != null) {
			Dosen dosen = formulirKegiatanPeserta.getDosen();
			bandboxDosen.setAttribute("dosen", dosen);
			bandboxDosen.setAttribute("myValue", dosen);
			bandboxDosen.setValue(dosen.getNim() + " - " + dosen.getNama());
			bandboxDosen.setDisabled(true);
		}

		else if (tbmuser != null && tbmuser.ambilDosen() != null
				&& tbmuser.hakAkses().getRoleId().equalsIgnoreCase("dosen")) {
			Dosen dosen = tbmuser == null ? null : tbmuser.ambilDosen();
			bandboxDosen.setAttribute("dosen", dosen);
			bandboxDosen.setAttribute("myValue", dosen);
			bandboxDosen.setValue(dosen.getNim() + " - " + dosen.getNama());
			bandboxDosen.setDisabled(true);
		}

		bandboxDosen.setEventListener(eventListener);

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.ambilGuru() == null && ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Siswa"));
		row.appendChild(bandboxSiswa = new AmbilDataSiswaBanbox());
		bandboxSiswa.setWidth("90%");

		if (formulirKegiatanPeserta != null && formulirKegiatanPeserta.getSiswa() != null) {
			Siswa siswa = formulirKegiatanPeserta.getSiswa();
			bandboxSiswa.setAttribute("siswa", siswa);
			bandboxSiswa.setAttribute("myValue", siswa);
			bandboxSiswa.setValue(siswa.getNim() + " - " + siswa.getNama());
			bandboxSiswa.setId("mhs_" + siswa.getId());
			bandboxSiswa.setDisabled(true);
		}

		else if (tbmuser != null && tbmuser.getSiswa() != null) {
			Siswa siswa = tbmuser == null ? null : tbmuser.getSiswa();
			bandboxSiswa.setAttribute("siswa", siswa);
			bandboxSiswa.setAttribute("myValue", siswa);
			bandboxSiswa.setValue(siswa.getNim() + " - " + siswa.getNama());
			bandboxSiswa.setId("mhs_" + siswa.getId());
			bandboxSiswa.setDisabled(true);
		}

		bandboxSiswa.setEventListener(eventListener);

		row = new MyFormRow();
		row.setVisible(tbmuser != null && tbmuser.getSiswa() == null && ya);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Guru"));
		row.appendChild(bandboxGuru = new AmbilDataGuruBanbox());
		bandboxGuru.setWidth("90%");

		if (formulirKegiatanPeserta != null && formulirKegiatanPeserta.getGuru() != null) {
			Guru guru = formulirKegiatanPeserta.getGuru();
			bandboxGuru.setAttribute("guru", guru);
			bandboxGuru.setAttribute("myValue", guru);
			bandboxGuru.setValue(guru.getNim() + " - " + guru.getNama());
			bandboxGuru.setDisabled(true);
		}

		else if (tbmuser != null && tbmuser.ambilGuru() != null) {
			Guru guru = tbmuser == null ? null : tbmuser.ambilGuru();
			bandboxGuru.setAttribute("guru", guru);
			bandboxGuru.setAttribute("myValue", guru);
			bandboxGuru.setValue(guru.getNim() + " - " + guru.getNama());
			bandboxGuru.setDisabled(true);
		}

		bandboxGuru.setEventListener(eventListener);

		final MyButtonConfig sertifikat = new MyButtonConfig("Cetak Sertifikat", "/img/svg/trophy.svg");
		sertifikat.setVisible(false);

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Acara/Formulir"));

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(formulirKegiatan = new Radiogroup());
		formulirKegiatan.setOrient("vertical");
		formulirKegiatan.setWidth("90%");

		formulirKegiatan.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (!chek()) {
					formulirKegiatan.setSelectedItem(null);
				}

				if (formulirKegiatanPesertamy != null && formulirKegiatanPesertamy.getAcc()) {
					sertifikat.setVisible(true);
				} else {
					sertifikat.setVisible(false);
				}

				if (formulirKegiatanPesertamy != null && formulirKegiatanPesertamy.getId() != null) {
					tombolPilih.setVisible(false);
				} else {
					tombolPilih.setVisible(myFormulirKegiatan != null);
				}

				tombol.setVisible(formulirKegiatanPesertamy != null);
			}
		});

		Hbox hbox = new Hbox();

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);
		row.appendChild(hbox);

		hbox.appendChild(tombolPilih = new MyButtonConfig("Pilih", "/img/svg/check-circled-outline.svg"));
		tombolPilih.setVisible(false);
		tombolPilih.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				if (!chek()) {
					return;
				}

				MyMessageboxConfig.showFormatCb(
						"Apakah Bapak/Ibu yakin akan memilih kegiatan/formulir acara \"{V1}\"? Silakan tekan OK untuk melanjutkan, atau Batal apabila Bapak/Ibu ingin membatalkan pilihan ini.",
						"Pertanyaan", MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									onKHS(event);
								}

							}
						}, myFormulirKegiatan.getNama());

			}
		});

		hbox.appendChild(sertifikat);

		hbox.appendChild(tombol = new MyButtonConfig("Cetak Ulang", "/img/svg/printer.svg"));
		tombol.setVisible(false);
		tombol.addEventListener("onClick", eventListener);

		sertifikat.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				SertifikatAction.cetakSertifikat(formulirKegiatanPesertamy);
			}
		});

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		// row = new MyFormRow();
		//		// row.setParent(rows);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
				Dosen dosen = (Dosen) bandboxMahasiswa.getAttribute("dosen");

				if (mahasiswa == null && dosen == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih peserta. Silakan pilih peserta (Mahasiswa/Dosen/Siswa/Guru) terlebih dahulu sebelum mencetak laporan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				if (formulirKegiatan.getSelectedItem() == null) {
					MyMessageboxConfig.show(
							"Mohon maaf, Bapak/Ibu belum memilih formulir kegiatan. Silakan pilih formulir/acara terlebih dahulu sebelum mencetak laporan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
					return null;
				}

				Map parameters = generateParameter();
				return parameters;
			}
		}, "form_pendidikan_lingkungan_kampus", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKHS(arg0);

			}
		}));

		if (formulirKegiatanPeserta != null) {
			Radio comboitem = new Radio(formulirKegiatanPeserta.getFormulirKegiatan().getNama());
			comboitem.setAttribute("value", formulirKegiatanPeserta.getFormulirKegiatan());
			formulirKegiatan.appendChild(comboitem);
			comboitem.setDisabled(true);
			formulirKegiatan.setSelectedItem(comboitem);
			onKHS(null);
		} else {
			eventListener.onEvent(null);
		}
	}

	private boolean chek() throws Exception {
		formulirKegiatanPesertamy = null;
		myFormulirKegiatan = null;
		final Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
		final Dosen dosen = (Dosen) bandboxDosen.getAttribute("dosen");

		final Siswa siswa = (Siswa) bandboxSiswa.getAttribute("siswa");
		final Guru guru = (Guru) bandboxGuru.getAttribute("guru");

		if (mahasiswa == null && dosen == null && siswa == null && guru == null) {
			return false;
		}
		if (formulirKegiatan.getSelectedItem() == null
				|| formulirKegiatan.getSelectedItem().getAttribute("value") == null) {
			return false;
		}

		myFormulirKegiatan = (FormulirKegiatan) formulirKegiatan.getSelectedItem().getAttribute("value");

		Integer smtTemp = null;
		if (mahasiswa != null) {
			Integer tahunAngkatanMhs = mahasiswa.getTahunangkatan();
			String semesterMulai = myFormulirKegiatan.getSemester();
			String ta = myFormulirKegiatan.getTahunAkademik();
			Integer tahun = Integer.parseInt(StringUtils.split(ta, "/")[0]);
			smtTemp = Common.getSemester(tahunAngkatanMhs, semesterMulai, mahasiswa.getPindahKeKampusIniMasukSemester(),
					tahun, mahasiswa.getSemesterMulai());
		}

		if (myFormulirKegiatan.getSyaratUjian() != null) {
			if (mahasiswa != null && !SyaratUjianAction.checkSyaratSyaratUjian(myFormulirKegiatan.getSyaratUjian(),
					myFormulirKegiatan, mahasiswa, smtTemp, myFormulirKegiatan.getNama())) {
				return false;
			}
		} else if (mahasiswa != null && !myFormulirKegiatan.getKodeItemBiaya().trim().isEmpty()) {

			final Integer smt = smtTemp;

			Session session = HibernateUtil.currentSession();

			if (myFormulirKegiatan.getKodeItemBiayaMenggunakanAtau()) {
				String nama = "";
				boolean ada = false;
				for (String kode : myFormulirKegiatan.getKodeItemBiaya().trim().split(",")) {
					if (!kode.trim().isEmpty()) {

						String[] spl = StringUtils.split(kode.trim(), ":");
						String code = spl.length > 0 ? spl[0] : "";
						String tahunAngkatan = spl.length > 1 ? spl[1] : "";

						if (tahunAngkatan.trim().isEmpty() || (mahasiswa.getTahunangkatan() != null
								&& mahasiswa.getTahunangkatan().toString().equalsIgnoreCase(tahunAngkatan.trim()))) {

							ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
									.simpleObject(
											session.createCriteria(ItemBiaya.class)
													.add(Restrictions.eq("kode", code.trim())).setMaxResults(1),
											ItemBiaya.class);
							if (itemBiaya != null) {
								int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
										.createAlias("kegiatan", "kegiatan")
										.add(myFormulirKegiatan.getSekaliBayar() ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("kegiatan.semster", smt))
										.add(Restrictions.eq("itemBiaya", itemBiaya))
										.add(Restrictions.or(
												Restrictions.eq("kegiatan.mahasiswa.id", mahasiswa.getId()),
												Restrictions.eq("kegiatan.calonMahasiswa.id",
														mahasiswa.getBiodataCalonMahasiswa())))
										.setProjection(Projections.rowCount()).uniqueResult()).intValue();
								if (jumlah > 0) {
									ada = true;
								} else {
									String k = itemBiaya.getKode() + " " + itemBiaya.getNama();
									nama += nama.isEmpty() ? k : " atau " + k;
								}
							}
						}
					}
				}

				if (!ada) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} belum melunasi biaya {V3}{V4}. Langkah yang dapat dilakukan: (1) mohon mahasiswa yang bersangkutan menghubungi bagian keuangan; (2) melakukan pelunasan biaya yang tertunda tersebut; (3) mengulangi proses pendaftaran setelah pembayaran terkonfirmasi.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNim(),
							mahasiswa.getNama(), nama,
							(myFormulirKegiatan.getSekaliBayar() ? "" : " pada semester " + smt));
					return false;
				}

			} else {

				for (String kode : myFormulirKegiatan.getKodeItemBiaya().trim().split(",")) {
					if (!kode.trim().isEmpty()) {
						String[] spl = StringUtils.split(kode.trim(), ":");
						String code = spl.length > 0 ? spl[0] : "";
						String tahunAngkatan = spl.length > 1 ? spl[1] : "";

						if (tahunAngkatan.trim().isEmpty() || (mahasiswa.getTahunangkatan() != null
								&& mahasiswa.getTahunangkatan().toString().equalsIgnoreCase(tahunAngkatan.trim()))) {
							ItemBiaya itemBiaya = (ItemBiaya) ConstantValues
									.simpleObject(
											session.createCriteria(ItemBiaya.class)
													.add(Restrictions.eq("kode", code.trim())).setMaxResults(1),
											ItemBiaya.class);
							if (itemBiaya != null) {
								int jumlah = ((Number) session.createCriteria(CicilanPembayaran.class)
										.createAlias("kegiatan", "kegiatan")
										.add(myFormulirKegiatan.getSekaliBayar() ? Restrictions.sqlRestriction("true")
												: Restrictions.eq("kegiatan.semster", smt))
										.add(Restrictions.eq("itemBiaya", itemBiaya))
										.add(Restrictions.or(
												Restrictions.eq("kegiatan.mahasiswa.id", mahasiswa.getId()),
												Restrictions.eq("kegiatan.calonMahasiswa.id",
														mahasiswa.getBiodataCalonMahasiswa())))
										.setProjection(Projections.rowCount()).uniqueResult()).intValue();
								if (jumlah == 0) {
									MyMessageboxConfig.showFormat(
											"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} belum melunasi biaya {V3} - {V4}{V5}. Langkah yang dapat dilakukan: (1) mohon mahasiswa yang bersangkutan menghubungi bagian keuangan; (2) melakukan pelunasan biaya yang tertunda tersebut; (3) mengulangi proses pendaftaran setelah pembayaran terkonfirmasi.",
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
											mahasiswa.getNim(), mahasiswa.getNama(), itemBiaya.getKode(),
											itemBiaya.getNama(),
											(myFormulirKegiatan.getSekaliBayar() ? "" : " pada semester " + smt));
									return false;
								}
							}
						}
					}
				}

			}
		}

		if (mahasiswa != null && !Common.checkStatusPembayaranKegiatanMahasiswa(myFormulirKegiatan, mahasiswa)) {
			return false;
		}

		Session session = ais.action.report.Report.openNativeSession();

		if (myFormulirKegiatan.getGrupFormulirKegiatan() != null) {
			FormulirKegiatanPeserta kegiatanLainSatuGrup = ((FormulirKegiatanPeserta) session
					.createCriteria(FormulirKegiatanPeserta.class).createAlias("formulirKegiatan", "formulirKegiatan")
					.add(Restrictions.eq("formulirKegiatan.grupFormulirKegiatan",
							myFormulirKegiatan.getGrupFormulirKegiatan()))
					.add(Restrictions.or(Restrictions.isNotNull("siswa"),
							Restrictions.or(Restrictions.isNotNull("guru"),
									Restrictions.or(Restrictions.isNotNull("mahasiswa"),
											Restrictions.isNotNull("dosen")))))
					.add(Restrictions.ne("formulirKegiatan", myFormulirKegiatan))

					.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
							: Restrictions.eq("mahasiswa", mahasiswa))
					.add(dosen == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen", dosen))

					.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa))
					.add(guru == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("guru", guru))
					.setMaxResults(1).uniqueResult());
			if (mahasiswa != null && kegiatanLainSatuGrup != null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} tidak dapat mendaftar karena telah terdaftar pada kegiatan \"{V3}\" yang berada dalam grup yang sama. Langkah yang dapat dilakukan: (1) memeriksa kembali pendaftaran kegiatan sebelumnya; (2) menghubungi bagian admin untuk informasi lebih lanjut; (3) memilih kegiatan lain yang belum terdaftar apabila diperlukan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNim(),
						mahasiswa.getNama(), kegiatanLainSatuGrup.getFormulirKegiatan().getNama());
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				ais.action.report.Report.closeCurrentSessionQuietly();
				return false;
			} else if (dosen != null && kegiatanLainSatuGrup != null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, Bapak/Ibu Dosen {V1} tidak dapat mendaftar karena telah terdaftar pada kegiatan \"{V2}\" yang berada dalam grup yang sama. Langkah yang dapat dilakukan: (1) memeriksa kembali pendaftaran kegiatan sebelumnya; (2) menghubungi bagian admin untuk informasi lebih lanjut; (3) memilih kegiatan lain yang belum terdaftar apabila diperlukan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, dosen.getNama(),
						kegiatanLainSatuGrup.getFormulirKegiatan().getNama());
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				ais.action.report.Report.closeCurrentSessionQuietly();
				return false;
			} else if (siswa != null && kegiatanLainSatuGrup != null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, siswa atas nama {V1} tidak dapat mendaftar karena telah terdaftar pada kegiatan \"{V2}\" yang berada dalam grup yang sama. Langkah yang dapat dilakukan: (1) memeriksa kembali pendaftaran kegiatan sebelumnya; (2) menghubungi bagian admin untuk informasi lebih lanjut; (3) memilih kegiatan lain yang belum terdaftar apabila diperlukan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, siswa.getNama(),
						kegiatanLainSatuGrup.getFormulirKegiatan().getNama());
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				ais.action.report.Report.closeCurrentSessionQuietly();
				return false;
			} else if (guru != null && kegiatanLainSatuGrup != null) {
				MyMessageboxConfig.showFormat(
						"Mohon maaf, Bapak/Ibu Guru {V1} tidak dapat mendaftar karena telah terdaftar pada kegiatan \"{V2}\" yang berada dalam grup yang sama. Langkah yang dapat dilakukan: (1) memeriksa kembali pendaftaran kegiatan sebelumnya; (2) menghubungi bagian admin untuk informasi lebih lanjut; (3) memilih kegiatan lain yang belum terdaftar apabila diperlukan.",
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, guru.getNama(),
						kegiatanLainSatuGrup.getFormulirKegiatan().getNama());
				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
				ais.action.report.Report.closeCurrentSessionQuietly();
				return false;
			}
		}

		formulirKegiatanPesertamy = (FormulirKegiatanPeserta) session.createCriteria(FormulirKegiatanPeserta.class)

				.add(Restrictions.or(Restrictions.isNotNull("siswa"),
						Restrictions.or(Restrictions.isNotNull("guru"),
								Restrictions.or(Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("dosen")))))

				.add(Restrictions.eq("formulirKegiatan", myFormulirKegiatan))
				.add(mahasiswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("mahasiswa", mahasiswa))
				.add(dosen == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("dosen", dosen))

				.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa))
				.add(guru == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("guru", guru))

				.setMaxResults(1).uniqueResult();

		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {
		formulirKegiatanPesertamy = null;
		myFormulirKegiatan = null;
		final Mahasiswa mahasiswa = (Mahasiswa) bandboxMahasiswa.getAttribute("mahasiswa");
		final Dosen dosen = (Dosen) bandboxDosen.getAttribute("dosen");

		final Siswa siswa = (Siswa) bandboxSiswa.getAttribute("siswa");
		final Guru guru = (Guru) bandboxGuru.getAttribute("guru");

		if (mahasiswa == null && dosen == null && siswa == null && guru == null) {
			return null;
		}

		if (!chek()) {
			return null;
		}

		if (formulirKegiatanPesertamy == null) {
			Session session = ais.action.report.Report.openNativeSession();
			try {
				int jumlahdaftar = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
						.add(Restrictions.or(Restrictions.isNotNull("siswa"),
								Restrictions.or(Restrictions.isNotNull("guru"),
										Restrictions.or(Restrictions.isNotNull("mahasiswa"),
												Restrictions.isNotNull("dosen")))))
						.add(Restrictions.eq("formulirKegiatan", myFormulirKegiatan))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				if (mahasiswa != null && myFormulirKegiatan.getKuota() <= jumlahdaftar) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, mahasiswa dengan NIM {V1} atas nama {V2} belum dapat mendaftar karena kuota kegiatan telah terpenuhi. Langkah yang dapat dilakukan: (1) menunggu apabila terdapat penambahan kuota; (2) menghubungi bagian admin untuk informasi lebih lanjut; (3) memilih kegiatan lain yang masih tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, mahasiswa.getNim(),
							mahasiswa.getNama());
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					ais.action.report.Report.closeCurrentSessionQuietly();
					return null;
				} else if (dosen != null && myFormulirKegiatan.getKuota() <= jumlahdaftar) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu Dosen {V1} belum dapat mendaftar karena kuota kegiatan telah terpenuhi. Langkah yang dapat dilakukan: (1) menunggu apabila terdapat penambahan kuota; (2) menghubungi bagian admin untuk informasi lebih lanjut; (3) memilih kegiatan lain yang masih tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, dosen.getNama());
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					ais.action.report.Report.closeCurrentSessionQuietly();
					return null;
				} else if (siswa != null && myFormulirKegiatan.getKuota() <= jumlahdaftar) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, siswa atas nama {V1} belum dapat mendaftar karena kuota kegiatan telah terpenuhi. Langkah yang dapat dilakukan: (1) menunggu apabila terdapat penambahan kuota; (2) menghubungi bagian admin untuk informasi lebih lanjut; (3) memilih kegiatan lain yang masih tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, siswa.getNama());
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					ais.action.report.Report.closeCurrentSessionQuietly();
					return null;
				} else if (guru != null && myFormulirKegiatan.getKuota() <= jumlahdaftar) {
					MyMessageboxConfig.showFormat(
							"Mohon maaf, Bapak/Ibu Guru {V1} belum dapat mendaftar karena kuota kegiatan telah terpenuhi. Langkah yang dapat dilakukan: (1) menunggu apabila terdapat penambahan kuota; (2) menghubungi bagian admin untuk informasi lebih lanjut; (3) memilih kegiatan lain yang masih tersedia.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, guru.getNama());
					// session.disconnect();
					if (session.isOpen()) {
						session.disconnect();
						session.close();
					}
					ais.action.report.Report.closeCurrentSessionQuietly();
					return null;
				}

				formulirKegiatanPesertamy = new FormulirKegiatanPeserta();
				formulirKegiatanPesertamy.setFormulirKegiatan(myFormulirKegiatan);
				formulirKegiatanPesertamy.setMahasiswa(mahasiswa);
				formulirKegiatanPesertamy.setDosen(dosen);
				formulirKegiatanPesertamy.setSiswa(siswa);
				formulirKegiatanPesertamy.setGuru(guru);

				int count = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
						.add(Restrictions.or(Restrictions.isNotNull("siswa"),
								Restrictions.or(Restrictions.isNotNull("guru"),
										Restrictions.or(Restrictions.isNotNull("mahasiswa"),
												Restrictions.isNotNull("dosen")))))
						.setProjection(Projections.rowCount())
						.add(Restrictions.eq("formulirKegiatan", myFormulirKegiatan)).uniqueResult()).intValue();
				count++;
				String kode = "0000000000000" + count;
				kode = kode.substring(kode.length() - 5);
				formulirKegiatanPesertamy.setKode(kode);
				session.getTransaction().begin();
				session.save(formulirKegiatanPesertamy);
				session.getTransaction().commit();

				// session.disconnect();
				if (session.isOpen()) {
					session.disconnect();
					session.close();
				}
			} catch (Exception e) {
				e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/report/format1/akademik/LaporanPendidikanLingkunganKampus.java:868");
				PesanFormalHelper.tampilkanGagalException("pemrosesan Laporan Pendidikan Lingkungan Kampus", "Sistem mengalami kendala teknis saat memproses permintaan pada layar laporan ini, kemungkinan disebabkan oleh data yang tidak lengkap, parameter/filter yang tidak sesuai, atau gangguan sementara pada sistem.", e,
					new String[] {
						"Periksa kembali data/parameter/filter yang Bapak/Ibu masukkan pada layar ini.",
						"Ulangi kembali proses yang tadi dijalankan beberapa saat lagi.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
			}
			ais.action.report.Report.closeCurrentSessionQuietly();
		}

		String nim = "";
		String nama = "";

		if (mahasiswa != null) {
			nim = mahasiswa.getNim();
			nama = mahasiswa.getNama();
		} else if (dosen != null) {
			nim = dosen.getNidn();
			nama = dosen.getNama();
		} else if (siswa != null) {
			nim = siswa.getNomorInduk();
			nama = siswa.getNamaSiswa();
		} else if (guru != null) {
			nim = guru.getNuptk();
			nama = guru.getNamaGuru();
		}

		Map parameters = ais.common.HashMapGenerator.getRand();

		parameters.put("kode", formulirKegiatanPesertamy.getKode());
		parameters.put("nama_formulir", myFormulirKegiatan.getNama());
		parameters.put("keterangan_formulir", myFormulirKegiatan.getKeterangan());
		parameters.put("ttdKananOleh", myFormulirKegiatan.getTtdKananOleh());
		parameters.put("ttdKiriOleh", myFormulirKegiatan.getTtdKiriOleh());
		parameters.put("ttdKananNama", myFormulirKegiatan.getTtdKananNama());
		parameters.put("ttdKiriNama", myFormulirKegiatan.getTtdKiriNama());
		parameters.put("ttdKananNip", myFormulirKegiatan.getTtdKananNip());
		parameters.put("ttdKiriNip", myFormulirKegiatan.getTtdKiriNip());

		parameters.put("mahasiswa", mahasiswa == null || mahasiswa.getId() == null ? -1L : mahasiswa.getId());
		parameters.put("dosen", dosen == null || dosen.getId() == null ? -1L : dosen.getId());

		parameters.put("siswa", siswa == null || siswa.getId() == null ? -1L : siswa.getId());
		parameters.put("guru", guru == null || guru.getId() == null ? -1L : guru.getId());

		String code = formulirKegiatanPesertamy.getKode() + "\n" + myFormulirKegiatan.getNama() + "\n" + nim + "\n"
				+ nama;

		File myfilebarcode = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_formulirKegiatanPesertamy_"
				+ formulirKegiatanPesertamy.getId() + ".png");

		BarcodeCommon.generateCRCode(code, myfilebarcode);
		parameters.put("qr_code", myfilebarcode.getAbsolutePath());
		code = parameters.get("qr_code") + "";
		File myfilebarcode1 = new File(Common.ambilREAL_PATH_REPORT() + "/crcode_" + Common.randLong() + ".png");
		BarcodeCommon.generateCRCode(code, myfilebarcode1);
		parameters.put("qr_code_img", myfilebarcode1.getAbsolutePath());
		return parameters;
	}

	/**
	 * Apakah pengguna memang BELUM memilih apa-apa (peserta dan/atau formulir kegiatan) sehingga
	 * laporan memang belum bisa dibuat. Dipakai {@link #onKHS(Event)} untuk membedakan antara
	 * "belum memilih" (perlu pesan yang jelas) dan "sudah memilih tapi gagal validasi/kuota"
	 * (pesan sudah ditampilkan oleh {@code chek()} / {@link #generateParameter()}, jangan sampai
	 * pengguna menerima dua pesan beruntun).
	 */
	private boolean belumMemilihDataLaporan() {
		try {
			boolean adaPeserta = bandboxMahasiswa.getAttribute("mahasiswa") != null
					|| bandboxDosen.getAttribute("dosen") != null || bandboxSiswa.getAttribute("siswa") != null
					|| bandboxGuru.getAttribute("guru") != null;
			boolean adaFormulir = formulirKegiatan.getSelectedItem() != null
					&& formulirKegiatan.getSelectedItem().getAttribute("value") != null;
			return !adaPeserta || !adaFormulir;
		} catch (Exception e) {
			ais.common.ErrorAuditUtil.record(e,
					"auto-audit(belumMemilihDataLaporan) src/ais/action/report/format1/akademik/LaporanPendidikanLingkunganKampus.java");
			return true;
		}
	}

	@SuppressWarnings({ "rawtypes" })
	public void onKHS(Event event) throws Exception {

		try {

			// FIX "parameters null saat generateFileReportCore,
			// laporan=form_pendidikan_lingkungan_kampus" + error susulan "Berkas hasil laporan
			// tidak ditemukan": generateParameter() SENGAJA mengembalikan null pada kondisi yang
			// wajar -- (a) peserta (mahasiswa/dosen/siswa/guru) atau formulir kegiatan belum
			// dipilih, (b) chek() gagal (syarat ujian/biaya belum terpenuhi), (c) kuota kegiatan
			// sudah penuh. Kondisi (a) PASTI terjadi saat layar pertama kali dibuka, karena
			// init() ditutup dengan eventListener.onEvent(null) yang berujung memanggil onKHS(...)
			// SEBELUM pengguna sempat mengisi apa pun. Sebelumnya null itu diteruskan mentah ke
			// Report.generateFileReportWithProgress sehingga muncul DUA error beruntun yang
			// membingungkan. Sekarang: dihentikan di sini; pesan yang jelas hanya ditampilkan bila
			// benar-benar dipicu aksi pengguna (event != null) dan penyebabnya memang "belum
			// memilih" (bila penyebabnya validasi/kuota, pesannya sudah tampil lebih dulu).
			Map parameters = generateParameter();
			if (parameters == null) {
				if (event != null && belumMemilihDataLaporan()) {
					MyMessageboxConfig.show(
							"Mohon maaf, laporan belum dapat dicetak karena data yang diperlukan belum lengkap. Silakan pilih peserta (Mahasiswa/Dosen/Siswa/Guru) dan formulir/kegiatan terlebih dahulu, kemudian ulangi proses pencetakan.",
							"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				}
				return;
			}

			File file = Report.generateFileReportWithProgress(Report.PDF, parameters,
					"form_pendidikan_lingkungan_kampus", ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Pendidikan Lingkungan Kampus", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
