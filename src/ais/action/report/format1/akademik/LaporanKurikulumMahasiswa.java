package ais.action.report.format1.akademik;
import ais.common.PesanFormalHelper;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
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

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.report.Report;
import ais.action.report.helper.CommonReport;
import ais.action.report.helper.ParameterListener;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Detailperkuliahan;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GeneralValueObject;
import ais.database.model.Jurusan;
import ais.database.model.Kurikulum;
import ais.database.model.KurikulumPunyaMatakuliah;
import ais.database.model.Mahasiswa;
import ais.database.model.Matakuliah;
import ais.database.model.MatakuliahEkivalen;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class LaporanKurikulumMahasiswa extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3331244819198611604L;
	// Untuk Laporan Kurikulum
	private Combobox kurikulumJenis;

	private Center center;
	private Toolbar toolbar;
	private AmbilDataMahasiswaBanbox mhs;
	private Mahasiswa mahasiswa = null;
	private Combobox smt;
	private Integer s = null;

	public LaporanKurikulumMahasiswa() {
		super();
		try {
			Tbmuser tbmuser = Common.getCurrentUser();
			this.mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
			initKurikulum();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kurikulum Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKurikulumMahasiswa(Mahasiswa mahasiswa, Integer s) {
		super();
		this.s = s;
		try {
			this.mahasiswa = mahasiswa;
			initKurikulum();
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pemuatan data awal layar Laporan Kurikulum Mahasiswa", "Sistem mengalami kendala teknis saat memuat data awal untuk layar laporan ini, kemungkinan karena data referensi (mis. periode, program studi/unit, atau parameter filter terkait) belum lengkap, atau terjadi gangguan sementara pada koneksi ke basis data.", e,
					new String[] {
						"Muat ulang (refresh) halaman ini dan coba akses kembali layar laporan.",
						"Periksa kembali parameter/filter (mis. periode, program studi/unit) yang Bapak/Ibu pilih sebelum membuka layar ini.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public LaporanKurikulumMahasiswa(String title, String border, boolean closable) throws Exception {
		super(title, border, closable);
		Tbmuser tbmuser = Common.getCurrentUser();
		this.mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		initKurikulum();
		init();
	}

	private void initKurikulum() throws Exception {

		kurikulumJenis = new Combobox();

	}

	@SuppressWarnings("deprecation")
	private void init() throws Exception {

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		West west = new West();
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
		column.setWidth("20%");
		column.setParent(columns);
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Mahasiswa *"));
		row.appendChild(mhs = new AmbilDataMahasiswaBanbox());
		mhs.setWidth("90%");
		mhs.setAttribute("mahasiswa", mahasiswa);

		EventListener eventListenerData = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				mahasiswa = (Mahasiswa) mhs.getAttribute("mahasiswa");
				Jurusan myJurusan = mahasiswa == null ? null : mahasiswa.getJurusan();

				Common.clear(kurikulumJenis);
				kurikulumJenis.setSelectedItem(null);

				List<Kurikulum> kurikulums = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(Kurikulum.class)
								.createAlias("program", "program", Criteria.LEFT_JOIN)

								.add(mahasiswa == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.eq("program.nama", mahasiswa.getProgram()),
												Restrictions.isNull("program.nama")))

								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.addOrder(Order.desc("tahun")).add(Restrictions.eq("jurusan", myJurusan)),
						Kurikulum.class);

				for (Kurikulum kurikulum : kurikulums) {
					if (mahasiswa != null && !kurikulum.bolehAmbil(mahasiswa)) {
						continue;
					}
					org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
					comboitem.setLabel(kurikulum.getId() + "-" + kurikulum.getNama());
					comboitem.setValue(kurikulum);
					comboitem.setDescription(kurikulum.getNamaAsli() + " " + kurikulum.getTahun() + " "
							+ kurikulum.getTahunAkademik() + " " + kurikulum.getJenisSemester());
					kurikulumJenis.appendChild(comboitem);
				}

				if (!kurikulumJenis.getChildren().isEmpty()) {
					kurikulumJenis.setSelectedIndex(0);
				}
			}
		};

		mhs.setEventListener(eventListenerData);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kurikulum *"));
		row.appendChild(kurikulumJenis);
		kurikulumJenis.setWidth("90%");
		kurikulumJenis.setReadonly(true);

		if (mahasiswa != null) {
			mhs.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
			mhs.setDisabled(true);
			eventListenerData.onEvent(null);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
		row.appendChild(smt = new Combobox());
		smt.setWidth("90%");
		smt.setReadonly(true);

		org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
		comboitem.setLabel("Semua Smt");
		comboitem.setValue(null);
		smt.appendChild(comboitem);
		smt.setSelectedItem(comboitem);

		for (int i = 1; i <= 14; i++) {
			comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel("Smt " + i);
			comboitem.setValue(i);
			smt.appendChild(comboitem);
		}

		if (s != null) {
			Common.selectComboItem(smt, s);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.North north = new org.zkoss.zul.North();
		north.setParent(borderlayout);
		north.appendChild(toolbar = CommonReport.exportReport(new ParameterListener() {

			@SuppressWarnings({ "unchecked", "rawtypes" })
			@Override
			public Map<String, Serializable> generateParameters() throws Exception {

				if (kurikulumJenis.getSelectedItem() == null) {
					MyMessageboxConfig.show("Pilih salah satu kurikulum", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Mahasiswa mahasiswa = (Mahasiswa) mhs.getAttribute("mahasiswa");
				if (mahasiswa == null) {
					MyMessageboxConfig.show("Pilih salah satu mahasiswa", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return null;
				}
				Map parameters = generateParameter();
				return parameters;
			}
		}, "daftar_riwayat_mk_berdasar_kurikulum", null, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onKurikulum(arg0);
			}
		}));

		row = new MyFormRow();
		row.setParent(rows);
		ais.ui.util.ZkCompat.setSpans(row, "2");
		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Tampilkan", "/img/print.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onKurikulum(event);
			}
		});
		print.setParent(row);

		if (kurikulumJenis.getSelectedItem() != null && kurikulumJenis.getSelectedItem().getValue() != null
				&& mahasiswa != null) {
			onKurikulum(null);
		}

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map generateParameter() throws Exception {

		if (kurikulumJenis.getSelectedItem() == null) {
			// MyMessageboxConfig.show("Pilih salah satu kurikulum",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}
		Mahasiswa mahasiswa = (Mahasiswa) mhs.getAttribute("mahasiswa");
		if (mahasiswa == null) {
			// MyMessageboxConfig.show("Pilih salah satu kurikulum",
			// "Peringatan", MyMessageboxConfig.OK,
			// MyMessageboxConfig.INFORMATION);
			return null;
		}

		Fakultas fakultas = mahasiswa.getJurusan().getFakultas();
		Jurusan jurusan = mahasiswa.getJurusan();
		Kurikulum kurikulum = (Kurikulum) kurikulumJenis.getSelectedItem().getValue();
		Map parameters = ais.common.HashMapGenerator.getRand();
		parameters.put("fakultas", fakultas.getNama());
		parameters.put("jurusan", jurusan.getNama());
		parameters.put("kurikulum", kurikulum.getId());
		parameters.put("mahasiswa", mahasiswa.getId());

		mahasiswa.masukkanData("lihat_kurikulum");

		Collection<Long> detailperkuliahans = mahasiswa.saringBerdasarNilaiDan0(mahasiswa.ambilDetailperkuliahan());
		List<KurikulumPunyaMatakuliah> kurikulumPunyaMatakuliahs = ConstantValues.simpleList(
				HibernateUtil.currentSession().createCriteria(KurikulumPunyaMatakuliah.class)
						.add(smt.getSelectedItem() == null || smt.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("true")
								: Restrictions.eq("semester", smt.getSelectedItem().getValue()))
						.add(Restrictions.eq("kurikulum", kurikulum)).createAlias("matakuliah", "matakuliah")
						.addOrder(Order.asc("semester")).addOrder(Order.asc("matakuliah.nama")),
				KurikulumPunyaMatakuliah.class);

		Dosen dosenpa = mahasiswa.getDosen() == null ? null
				: (Dosen) ConstantValues.ambil(Dosen.class.getName(), mahasiswa.getDosen());
		parameters.put("nuptkosenpa", dosenpa == null ? "" : dosenpa.getNuptk());
		parameters.put("dosenpa", dosenpa == null ? "" : dosenpa.getNama());
		parameters.put("nipdosenpa", dosenpa == null ? "......................."
				: (dosenpa.getCode().isEmpty() ? dosenpa.getNidn() : dosenpa.getCode()));

		List<Map> maps = new ArrayList<Map>();
		for (KurikulumPunyaMatakuliah kurikulumPunyaMatakuliah : kurikulumPunyaMatakuliahs) {
			if (kurikulumPunyaMatakuliah.getAktif()) {

				Matakuliah mkur = kurikulumPunyaMatakuliah.getMatakuliah();

				Map map = new java.util.HashMap();
				map.put("nama_mahasiswa", mahasiswa.getNama());
				map.put("nim", mahasiswa.getNim());
				map.put("nama_jurusan", mahasiswa.getJurusan().getNama());
				map.put("tahunangkatan", mahasiswa.getTahunangkatan());
				map.put("jenjang", mahasiswa.getJenjang().getNama());
				map.put("dosen_pa", dosenpa == null ? "" : dosenpa.getNama());

				map.put("kode_mk", mkur.getKode());
				map.put("nama_mk", mkur.getNama());
				map.put("jenis_mk", mkur.getStatus());
				map.put("sks_mk", mkur.getSks());
				map.put("semester", kurikulumPunyaMatakuliah.getSemester());

				List<MatakuliahEkivalen> matakuliahEkivalensData = mkur.ambilEkivalen(mahasiswa.getNim());
				Double totalNilai = 0.0;
				Detailperkuliahan detailperkuliahan = null;
				Matakuliah mk = null;
				for (MatakuliahEkivalen matakuliahEkivalen : matakuliahEkivalensData) {
					for (Long detailperkuliahansubid : detailperkuliahans) {

						Detailperkuliahan detailperkuliahansub = (Detailperkuliahan) GeneralValueObject
								.ambilData(Detailperkuliahan.class, detailperkuliahansubid.toString());
						if (detailperkuliahansub != null) {
							Matakuliah matakuliahsub = detailperkuliahansub.getMatakuliahKonversi() != null
									? detailperkuliahansub.getMatakuliahKonversi()
									: detailperkuliahansub.getPerkuliahan() != null
											? detailperkuliahansub.getPerkuliahan().getMatakuliah()
											: null;
							if (matakuliahsub != null) {
								if (matakuliahsub.getKode()
										.equalsIgnoreCase(matakuliahEkivalen.getMatakuliah().getKode())
										|| matakuliahsub.getKode().equalsIgnoreCase(
												matakuliahEkivalen.getMatakuliahEkivalen().getKode())) {
									if (totalNilai < detailperkuliahansub.getTotalNilai()) {
										totalNilai = detailperkuliahansub.getTotalNilai();
										detailperkuliahan = detailperkuliahansub;
										mk = matakuliahsub;
									}
								}
							}
						}
					}
				}

				if (detailperkuliahan == null) {

					for (Long detailperkuliahanid : detailperkuliahans) {
						Detailperkuliahan d = (Detailperkuliahan) GeneralValueObject.ambilData(Detailperkuliahan.class,
								detailperkuliahanid.toString());
						if (d != null) {
							Matakuliah matakuliah = d.getMatakuliahKonversi() != null ? d.getMatakuliahKonversi()
									: d.getPerkuliahan() != null ? d.getPerkuliahan().getMatakuliah() : null;
							if (matakuliah != null && matakuliah.getKode() != null && mkur != null
									&& mkur.getKode() != null
									&& mkur.getKode().equalsIgnoreCase(matakuliah.getKode())) {
								detailperkuliahan = d;
								mk = matakuliah;
								break;
							}
						}
					}
				}

				map.put("sks", mk == null ? 0 : mk.getSks());
				map.put("nilai_huruf", detailperkuliahan == null ? "" : detailperkuliahan.getNilaiHuruf());
				map.put("nilai_ipk", detailperkuliahan == null ? 0.0 : detailperkuliahan.getTotalIP());
				map.put("nilai_total", detailperkuliahan == null ? 0.0 : detailperkuliahan.getTotalNilai());

				Perkuliahan perkuliahan = detailperkuliahan == null ? null : detailperkuliahan.getPerkuliahan();

				map.put("nama_mahasiswa", mahasiswa.getNama());
				map.put("nama", mahasiswa.getNama());
				map.put("kelas", mahasiswa.getKelas());
				map.put("hari", perkuliahan == null ? "" : perkuliahan.getHari());
				map.put("tahunangkatan", mahasiswa.getTahunangkatan());
				map.put("nim", mahasiswa.getNim());
				map.put("jurusan", mahasiswa.getJurusan().getNama());
				map.put("nama_jurusan", mahasiswa.getJurusan().getNama());
				map.put("id_fakultas", mahasiswa.getJurusan().getFakultas().getId());
				map.put("fakultas_id", mahasiswa.getJurusan().getFakultas().getId());
				map.put("fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				map.put("nama_fakultas", mahasiswa.getJurusan().getFakultas().getNama());
				map.put("jenjang", mahasiswa.getJurusan().getJenjang().getNama());
				map.put("tahun_ajaran",
						detailperkuliahan == null || detailperkuliahan.getTahunAkademik() == null
								|| detailperkuliahan.getTahunAkademik().trim().isEmpty()
										? (perkuliahan == null ? "" : perkuliahan.getTahunAjaran())
										: detailperkuliahan.getTahunAkademik());
				map.put("kode_mata_kuliah", mk == null ? "" : mk.getKode());
				map.put("mata_kuliah", mk == null ? "" : mk.getNama());
				map.put("nama_matakuliah", mk == null ? "" : mk.getNama());
				map.put("kode_matakuliah", mk == null ? "" : mk.getKode());
				map.put("semester_pk", perkuliahan == null ? null : perkuliahan.getSemester());

				map.put("sks", mk == null ? 0 : mk.getSks());

				map.put("waktu_mulai", perkuliahan == null ? "" : perkuliahan.getWaktuMulai());
				map.put("waktu_selesai", perkuliahan == null ? "" : perkuliahan.getWaktuSelesai());
				map.put("kelas", perkuliahan == null ? "" : perkuliahan.getKelas());
				map.put("ruang",
						perkuliahan == null || perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
				map.put("ruangan",
						perkuliahan == null || perkuliahan.getRuang() == null ? "" : perkuliahan.getRuang().getNama());
				map.put("dosen_pa", dosenpa == null ? "" : dosenpa.getNama());
				map.put("nip_dosen_pa", dosenpa == null ? "" : dosenpa.getCode());
				map.put("nidn_dosen_pa", dosenpa == null ? "" : dosenpa.getNidn());

				map.put("nama_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNama());
				map.put("nip_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getCode());
				map.put("nidn_kaprodi", mahasiswa.getJurusan().getKaprodi() == null ? ""
						: mahasiswa.getJurusan().getKaprodi().getNidn());

				map.put("nama_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNama());
				map.put("nip_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getCode());
				map.put("nidn_dekan", mahasiswa.getJurusan().getFakultas().getDekan() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getDekan().getNidn());

				map.put("nama_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNama());
				map.put("nip_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getCode());
				map.put("nidn_pudek1", mahasiswa.getJurusan().getFakultas().getPudek1() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek1().getNidn());

				map.put("nama_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNama());
				map.put("nip_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getCode());
				map.put("nidn_pudek2", mahasiswa.getJurusan().getFakultas().getPudek2() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek2().getNidn());

				map.put("nama_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNama());
				map.put("nip_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getCode());
				map.put("nidn_pudek3", mahasiswa.getJurusan().getFakultas().getPudek3() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPudek3().getNidn());

				map.put("nama_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNama());
				map.put("nip_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getCode());
				map.put("nidn_kajur",
						mahasiswa.getJurusan().getGrupJurusan() == null
								|| mahasiswa.getJurusan().getGrupJurusan().getKajur() == null ? ""
										: mahasiswa.getJurusan().getGrupJurusan().getKajur().getNidn());

				map.put("dosen", perkuliahan == null ? "" : perkuliahan.ambilNamaDosens());
				map.put("merupakan_paralel", perkuliahan == null ? false : perkuliahan.getMerupakan_paralel());
				map.put("nama_perguruan_tinggi", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getNama());
				map.put("alamat1", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat1());
				map.put("alamat2", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getAlamat2());
				map.put("telepon", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getTelepon());
				map.put("faksimili", mahasiswa.getJurusan().getFakultas().getPerguruanTinggi() == null ? ""
						: mahasiswa.getJurusan().getFakultas().getPerguruanTinggi().getFaksimili());

				map.put("perkuliahandimulai", perkuliahan == null ? null : perkuliahan.getPerkuliahanDimulai());
				map.put("perkuliahansampai", perkuliahan == null ? null : perkuliahan.getPerkuliahanSampai());

				map.put("keteranganjadwal", perkuliahan == null ? null : perkuliahan.getKeteranganJadwal());

				map.put("total_nilai", detailperkuliahan == null ? 0.0 : detailperkuliahan.getTotalNilai());
				map.put("nilai_huruf", detailperkuliahan == null ? "" : detailperkuliahan.getNilaiHuruf());
				map.put("nilai_ip", detailperkuliahan == null ? 0.0 : detailperkuliahan.getTotalIP());
				map.put("nilai_ipk", detailperkuliahan == null ? 0.0 : detailperkuliahan.getTotalIP());
				map.put("lulus",
						detailperkuliahan == null ? "" : detailperkuliahan.getLulus() ? "Lulus" : "Tidak Lulus");

				maps.add(map);
			}
		}

		parameters.put("maps", maps);

		return parameters;
	}

	@SuppressWarnings({})
	public void onKurikulum(Event event) {

		try {

			File file = Report.generateFileReportWithProgress(Report.PDF, generateParameter(),
					"daftar_riwayat_mk_berdasar_kurikulum", ais.ui.util.WaktuUtil.getDate(), toolbar);
			CommonReport.tampilkanReportPDF(center, file);

		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException("pembuatan berkas PDF Laporan Kurikulum Mahasiswa", "Sistem mengalami kendala teknis saat menyusun berkas PDF laporan ini, kemungkinan karena salah satu data sumber laporan tidak lengkap, format datanya tidak sesuai dengan yang diharapkan oleh template laporan, atau terjadi gangguan sementara pada proses pembuatan berkas.", e,
					new String[] {
						"Periksa kembali filter/kriteria/periode yang Bapak/Ibu pilih sebelum mencetak laporan ini.",
						"Pastikan data yang menjadi sumber laporan ini (mis. data akademik/keuangan/pegawai terkait) sudah lengkap dan benar, kemudian coba cetak ulang.",
						"Jika kendala terus berulang, silakan hubungi Administrator atau laporkan ke Pengembang Sistem disertai tangkapan layar (screenshot) pesan ini."
					});
		}

	}

}
