package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.North;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.Jurusan;
import ais.database.model.Perkuliahan;
import ais.database.model.file.LampiranLain;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SKMengajarDosenWindow extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 790038368339375113L;

	private Combobox fakultas;
	private Combobox jurusan;
	private Combobox searchTahunAjaran;
	private Combobox jenis_semester;

	private Center center = new Center();

	private String tahunAjaran;

	private String jenisSemester;

	private Jurusan jurusanDosen;

	private String program;

	private Combobox searchprogram;

	private Dosen dosen;

	private Fakultas fakultasDosen;

	private Set<Long> dipilihs = new HashSet<Long>();
	private Map<Long, MyCheckboxConfig> semuas = new HashMap<Long, MyCheckboxConfig>();

	protected LampiranLain suratsk = null;

	private Textbox searchdosen;

	public SKMengajarDosenWindow() {
		super();
		try {
			init();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			PesanFormalHelper.tampilkanGagalException(
					"menampilkan jendela SK Mengajar Dosen",
					e, new String[] {
							"Muat ulang (refresh) halaman ini lalu coba buka jendela kembali.",
							"Periksa koneksi jaringan Anda ke server aplikasi.",
							"Apabila kendala masih berlanjut, hubungi Admin dengan menyertakan tangkapan layar (screenshot) pesan ini."
					});
		}
	}

	public SKMengajarDosenWindow(String title, String border, boolean closable) {
		super(title, border, closable);
		init();
	}

	public SKMengajarDosenWindow(String title, String border, boolean closable, String tahunAjaran,
			String jenisSemester, Jurusan jurusanDosen, Fakultas fakultas, String program, Dosen dosen) {
		super(title, border, closable);
		this.tahunAjaran = tahunAjaran;
		this.jenisSemester = jenisSemester;
		this.jurusanDosen = jurusanDosen;
		this.fakultasDosen = fakultas;
		this.program = program;
		this.dosen = dosen;
		init();
	}

	@SuppressWarnings("deprecation")
	private void init() {

		searchTahunAjaran = Common.generateTahunAjaran(searchTahunAjaran = new Combobox());
		Common.initFakultasDanJurusanDanSemua(fakultas = new Combobox(), jurusan = new Combobox(), null, null);
		jenis_semester = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		jenis_semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		jenis_semester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.SP);
		comboitem.setValue(Perkuliahan.SP);
		jenis_semester.appendChild(comboitem);

		setHeight("100%");
		setWidth("100%");
		setBorder("none");
		setClosable(false);
		// setTitle("Rekap Pembayaran Host to Host");
		// setPosition("center");

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(this);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(north);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(fakultas);
		fakultas.setWidth("90%");
		fakultas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(jurusan);
		jurusan.setWidth("90%");
		jurusan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		row.appendChild(this.searchTahunAjaran);
		searchTahunAjaran.setWidth("90%");
		searchTahunAjaran.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		if (tahunAjaran != null) {
			Common.selectComboItem(true, searchTahunAjaran, tahunAjaran);
			searchTahunAjaran.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Semester"));
		row.appendChild(this.jenis_semester);
		jenis_semester.setWidth("90%");
		Common.selectComboItem(jenis_semester, Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP);
		jenis_semester.setReadonly(true);

		if (jenisSemester != null) {
			Common.selectComboItem(true, jenis_semester, jenisSemester);
			jenis_semester.setDisabled(true);
		}

		jenis_semester.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		searchprogram = Common.initPrograms(null);

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(this.searchprogram);
		searchprogram.setWidth("90%");

		if (program != null) {
			Common.selectComboItem(true, searchprogram, program);
			searchprogram.setDisabled(true);
		}

		searchprogram.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
		row.appendChild(this.searchdosen = new Textbox());
		searchdosen.setWidth("90%");

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "6");
		row.setParent(rows);

		Hbox hboxa = new Hbox();
		hboxa.setParent(row);

		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, -Common.randLong(), "sk_penugasan_pengajaran_dosen_gabungan",
				"SK", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						suratsk = (LampiranLain) arg0.getData();
					}
				}, null, false, false, false, true);
		hbox.setParent(hboxa);

		MyToolbarbuttonConfig print = new MyToolbarbuttonConfig("Masukkan ke SK Dosen",
				"/img/stock_data_edit_table.png");
		print.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (suratsk == null) {
					MyMessageboxConfig.show("File SK harus diupload terlebih dulu", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}
				if (dipilihs.isEmpty()) {
					MyMessageboxConfig.show("Pilihlah minimal satu dosen", "Peringatan", MyMessageboxConfig.OK,
							MyMessageboxConfig.INFORMATION);
					return;
				}

				String tahun = (String) searchTahunAjaran.getSelectedItem().getValue();
				String jenisSemesterNumber = jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.SP) ? "3"
						: jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "2";

				Session session = StreamingHibernateUtil.getInstance().currentSession();
				for (Long idDosen : dipilihs) {

					try {
						String kode = Common.maxPanjangAkhir("000000000000000000000" + idDosen + "00"
								+ StringUtils.split(tahun, "/")[0] + jenisSemesterNumber, 14);
						kode = "1" + kode;
						Long l = Long.parseLong(kode);

						LampiranLain lampiranLain = LampiranLain.ambil(l, "sk_penugasan_pengajaran_dosen_gabungan");
						if (lampiranLain == null) {
							lampiranLain = new LampiranLain();
						}
						lampiranLain.setRef(l);
						lampiranLain.setJenis("sk_penugasan_pengajaran_dosen_gabungan");
						lampiranLain.setCopyDari(suratsk);
						session.getTransaction().begin();
						session.saveOrUpdate(lampiranLain);
						session.getTransaction().commit();
					} catch (Exception e) {
						e.printStackTrace(); ais.common.ErrorAuditUtil.record(e, "auto-audit src/ais/action/master/helper/SKMengajarDosenWindow.java:325");
					}
				}
				StreamingHibernateUtil.getInstance().closeSession();

				Common.createDefaultTimer(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						initSpreadsheet();
					}
				});
			}
		});
		print.setParent(hboxa);

		if (jurusanDosen != null) {
			Common.selectComboItem(true, jurusan, jurusanDosen);
			jurusan.setDisabled(true);

		}
		if (fakultasDosen != null) {
			Common.selectComboItem(true, fakultas, fakultasDosen);
			fakultas.setDisabled(true);
		}

		center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				SKMengajarDosenWindow.this.detach();
			}
		});

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				initSpreadsheet();
			}
		});
	}

	// private void

	@SuppressWarnings("unchecked")
	private void initSpreadsheet() throws Exception {
		Common.clear(center);

		Session session = HibernateUtil.currentSession();

		Criteria criteria = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		Criterion criterion = dosen == null ? Restrictions.sqlRestriction("1=1")
				: Restrictions.or(Restrictions.eq("dosen1", dosen), Restrictions.eq("dosen2", dosen));

		criterion = Restrictions.or(criterion, Restrictions.eq("dosen3", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen4", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen5", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen6", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen7", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen8", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen9", dosen));
		criterion = Restrictions.or(criterion, Restrictions.eq("dosen10", dosen));

		if (!searchdosen.getValue().trim().isEmpty()) {
			criteria.createAlias("dosen1", "dosen1", Criteria.LEFT_JOIN)
					.createAlias("dosen2", "dosen2", Criteria.LEFT_JOIN)
					.createAlias("dosen3", "dosen3", Criteria.LEFT_JOIN)
					.createAlias("dosen4", "dosen4", Criteria.LEFT_JOIN)
					.createAlias("dosen5", "dosen5", Criteria.LEFT_JOIN)
					.createAlias("dosen6", "dosen6", Criteria.LEFT_JOIN)
					.createAlias("dosen7", "dosen7", Criteria.LEFT_JOIN)
					.createAlias("dosen8", "dosen8", Criteria.LEFT_JOIN)
					.createAlias("dosen9", "dosen9", Criteria.LEFT_JOIN)
					.createAlias("dosen10", "dosen10", Criteria.LEFT_JOIN);

			Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");
			criterionNamaDosn = Restrictions.ilike("dosen1.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE);

			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen2.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen3.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen4.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen5.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen6.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen7.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen8.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen9.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));
			criterionNamaDosn = Restrictions.or(criterionNamaDosn,
					Restrictions.ilike("dosen10.nama", searchdosen.getValue().trim(), MatchMode.ANYWHERE));

			criteria.add(criterionNamaDosn);
		}

		List<Perkuliahan> perkuliahans = criteria

				.add(Restrictions.isNull("perkuliahan_paralel"))

				.add(criterion)

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAjaran", searchTahunAjaran.getSelectedItem().getValue()))

				.add(jenis_semester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.SP)
								? Restrictions.eq("statusSemesterPendek", Perkuliahan.SEMESTER_PENDEK)
								: Restrictions.and(Restrictions.isNull("statusSemesterPendek"),
										Restrictions.eq("ganjilGenap", jenis_semester.getSelectedItem().getValue())))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", jurusan, false))

				.createAlias("jurusan", "jurusan")

				.add(fakultas.getSelectedItem() == null || fakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", fakultas, false))

				.setMaxResults(1048576).addOrder(Order.desc("id")).list();

		if (perkuliahans.size() == 0) {
			return;
		}

		MyGrid grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setSclass("dgrid");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(5);
		grid.setParent(center);
		grid.getPagingChild().setMold("os");
		grid.setPagingPosition("top");

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		final MyCheckboxConfig semuaPilih = new MyCheckboxConfig();
		semuaPilih.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				dipilihs.clear();
				if (semuaPilih.isChecked()) {
					dipilihs.addAll(semuas.keySet());
				}

				for (MyCheckboxConfig c : semuas.values()) {
					c.setChecked(semuaPilih.isChecked());
				}
			}
		});
		semuaPilih.setParent(column);
		column.setWidth("30px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Dosen");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("SKS");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pengajaran");

		TreeMap<Dosen, List<Perkuliahan>> dosensMap = new TreeMap<Dosen, List<Perkuliahan>>();
		for (Perkuliahan perkuliahan : perkuliahans) {

			Map<String, Dosen> map = perkuliahan.populateDosen();
			for (Dosen d : map.values()) {
				if (dosensMap.containsKey(d)) {
					dosensMap.get(d).add(perkuliahan);
				} else {
					List<Perkuliahan> itemDetails = new ArrayList<Perkuliahan>();
					itemDetails.add(perkuliahan);
					dosensMap.put(d, itemDetails);
				}
			}

		}

		Rows rows = new Rows();
		rows.setParent(grid);

		String tahun = (String) searchTahunAjaran.getSelectedItem().getValue();
		String jenisSemesterNumber = jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.SP) ? "3"
				: jenis_semester.getSelectedItem().getValue().equals(Perkuliahan.GANJIL) ? "1" : "2";
		semuas.clear();
		Double sksTotal = 0.0;
		for (final Dosen dosen : dosensMap.keySet()) {

			List<Perkuliahan> perkulishsnasDosen = dosensMap.get(dosen);

			String itemYangDipinjam = "";
			Double sks = 0.0;
			for (Perkuliahan perkul : perkulishsnasDosen) {
				Double sksDibagi = perkul.getMatakuliah().getSks().doubleValue()
						/ perkul.getJumlahDosen().doubleValue();

				sks += sksDibagi;
				String s = perkul.getMatakuliah().getKode() + "-" + perkul.getMatakuliah() + "=> jml dosen: "
						+ perkul.getJumlahDosen() + ", sks mk:" + perkul.getMatakuliah().getSks() + " sks, total: "
						+ Common.numberFormat.get().format(sksDibagi) + "sks";
				itemYangDipinjam += itemYangDipinjam.isEmpty() ? s : " ,\n" + s;
			}

			sksTotal += sks;

			MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			final MyCheckboxConfig myCheckboxConfig = new MyCheckboxConfig();
			myCheckboxConfig.setChecked(dipilihs.contains(dosen.getId()));
			myCheckboxConfig.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					if (myCheckboxConfig.isChecked()) {
						dipilihs.add(dosen.getId());
					} else {
						dipilihs.remove(dosen.getId());
					}
				}
			});
			semuas.put(dosen.getId(), myCheckboxConfig);

			row.appendChild(myCheckboxConfig);

			row.appendChild(new Label(dosen.getNama()));
			row.appendChild(new Label(Common.numberFormat.get().format(sks)));
			Vbox vbox = new Vbox();
			row.appendChild(vbox);
			vbox.appendChild(new MyLabelKecil(itemYangDipinjam));

			String kode = Common.maxPanjangAkhir("000000000000000000000" + dosen.getId() + "00"
					+ StringUtils.split(tahun, "/")[0] + jenisSemesterNumber, 14);
			kode = "1" + kode;
			Long l = Long.parseLong(kode);

			System.out.println("l -> " + l);

			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, l, "sk_penugasan_pengajaran_dosen_gabungan", "SK Gabungan",
					false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true);
			hbox.setParent(vbox);

		}

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		row.appendChild(new Label());
		row.appendChild(new Label(ais.common.Common.getBahasaConfig("SKS Total")));
		row.appendChild(new Label(Common.numberFormat.get().format(sksTotal)));
		row.appendChild(new Label());

	}
}
