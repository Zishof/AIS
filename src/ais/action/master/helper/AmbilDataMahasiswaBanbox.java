package ais.action.master.helper;


import ais.common.CommonSearchFilterHelper;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Bandbox;
import org.zkoss.zul.Bandpopup;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;

import ais.action.master.helper.util.PerguruanTinggiUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Mahasiswa;
import ais.database.model.PerguruanTinggi;
import ais.database.model.StatusAwalMahasiswa;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.ui.util.AmbilDataPagingHelper;
import ais.ui.util.GetEventListener;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyToolbarbuttonConfig;

public class AmbilDataMahasiswaBanbox extends Bandbox implements GetEventListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	/* Paging server-side per 5 baris (pola AmbilDataPagingHelper). */
	private final AmbilDataPagingHelper pagingHelper = new AmbilDataPagingHelper();

	private EventListener eventListener;
	private Boolean hanyaYangAktif = false;
	private List<Long> indsMhsPerkuliahan;
	private PerguruanTinggi perguruanTinggi;
	private Boolean hanyaAlumni = false;

	protected boolean tidakMelihatDosen = false;

	public AmbilDataMahasiswaBanbox() {

		this(false);
	}

	public AmbilDataMahasiswaBanbox(List<Long> indsMhsPerkuliahan) {
		this(false);
		this.indsMhsPerkuliahan = indsMhsPerkuliahan;
	}

	public AmbilDataMahasiswaBanbox(Boolean hanyaYangAktif) {
		this(false, false);
	}

	public AmbilDataMahasiswaBanbox(Boolean hanyaYangAktif, Boolean hanyaAlumni) {
		super();
		this.hanyaAlumni = hanyaAlumni;
		this.hanyaYangAktif = hanyaYangAktif;
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		perguruanTinggi = PerguruanTinggiUtil.getPerguruanTinggi();
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			setAttribute("mahasiswa", tbmuser.getMahasiswa());
			setAttribute("myValue", tbmuser.getMahasiswa());
			setValue(tbmuser.getMahasiswa().getNim() + "-" + tbmuser.getMahasiswa().getNama());
			setDisabled(true);
		}

		Common.insertCombo(searchstatus, "nama", "kodeEpsbed", StatusMahasiswa.class);

		setReadonly(true);

		addEventListener("onOpen", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				if (getChildren().isEmpty()) {
					display();

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							setOpen(true);
						}
					});
				}

			}
		});
	}

	private Textbox nim;
	private Textbox nama;
	private AmbilDataKelasBanbox kelas;
	private Decimalbox tahunangkatan;
	private Combobox searchstatus = new Combobox();
	private Combobox searchprogram = new Combobox();
	private Combobox searchstatusawal = new Combobox();
	private AmbilDataDosenBanbox searchdosen;
	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	class MahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Mahasiswa mahasiswa = (Mahasiswa) arg1;
			CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(arg0);
			Radio checkbox = new Radio(mahasiswa.getNim());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.setAttribute("mahasiswa", mahasiswa);
			if (hanyaYangAktif) {
				// Hindari query HistoryStatusMahasiswa (lambat): pakai flag aktif (murah).
				checkbox.setDisabled(Boolean.FALSE.equals(mahasiswa.getAktif()));
			}

			checkbox.addEventListener("onCheck", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					AmbilDataMahasiswaBanbox.this.setOpen(false);
					AmbilDataMahasiswaBanbox.this.setAttribute("mahasiswa", mahasiswa);
					AmbilDataMahasiswaBanbox.this.setAttribute("myValue", mahasiswa);
					AmbilDataMahasiswaBanbox.this.setValue(mahasiswa.getNim() + " - " + mahasiswa.getNama());
					// AmbilDataMahasiswaBanbox.this.setId("mhs_"
					// + mahasiswa.getId());

					if (eventListener != null) {
						eventListener.onEvent(event);
					}
				}
			});

			new Label(mahasiswa.getNama()).setParent(arg0);
			new Label(mahasiswa.getTahunangkatan() + "").setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(mahasiswa.getProgram()).setParent(arg0);

		}

	}

	public void display() {
		setReadonly(true);
		Bandpopup bandpopup = new ais.ui.util.MyBandpopup();
		bandpopup.setParent(this);
		bandpopup.setWidth("1200px");
		bandpopup.setHeight("600px");

		final Radiogroup radiogroup = new Radiogroup();
		radiogroup.setWidth("100%");
		radiogroup.setHeight("100%");
		radiogroup.setParent(bandpopup);

		Panel panel = new ais.ui.util.MyPanelConfig();
		panel.setParent(radiogroup);
		panel.setWidth("100%");
		panel.setHeight("100%");
		panel.setTitle("Daftar Mahasiswa");
		panel.setBorder("none");
		panel.setStyle("border:0px;");

		Panelchildren panelchildren = new Panelchildren();
		panelchildren.setParent(panel);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(panelchildren);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		North north = new North();
		north.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(north, false);
		// Banyak field filter (NIM/Nama/Program/Angkatan/Status/Kelas/Dosen PA/Fakultas/Prodi) →
		// beri tinggi cukup + autoscroll agar filter & tombol Cari tampil rapi, tak terpotong.
		north.setHeight("130px");
		north.setAutoscroll(true);

		Div div = new Div();
		div.setParent(north);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(div);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("NIM"));
		row.appendChild(nim = new Textbox());
		nim.setWidth("90%");
		nim.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Nama"));
		row.appendChild(nama = new Textbox());
		nama.setWidth("90%");
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Program"));
		row.appendChild(searchprogram);
		searchprogram.setWidth("90%");
		searchprogram.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Angkatan"));
		row.appendChild(tahunangkatan = new Decimalbox());
		tahunangkatan.setWidth("90%");
		tahunangkatan.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status"));
		row.appendChild(searchstatus);
		searchstatus.setWidth("90%");
		searchstatus.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		if (hanyaAlumni) {
			searchstatus.setDisabled(true);
		}

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Awal"));
		row.appendChild(searchstatusawal);
		searchstatusawal.setWidth("90%");
		searchstatusawal.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		Common.initPrograms(searchprogram);
		Common.insertCombo(searchstatusawal, "nama", StatusAwalMahasiswa.class,
				Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kelas"));
		row.appendChild(kelas = new AmbilDataKelasBanbox());
		kelas.setWidth("90%");
		kelas.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Dosen PA"));
		searchdosen = new AmbilDataDosenBanbox(tidakMelihatDosen, true, false);
		row.appendChild(searchdosen);
		searchdosen.setWidth("90%");
		searchdosen.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		if (indsMhsPerkuliahan != null && !indsMhsPerkuliahan.isEmpty()) {
			searchdosen.setAttribute("myValue", null);
			searchdosen.setAttribute("dosen", null);
			searchdosen.setValue("");
			searchdosen.setDisabled(false);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Fakultas"));
		row.appendChild(searchfakultas);
		searchfakultas.setWidth("90%");
		searchfakultas.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Prodi"));
		row.appendChild(searchjurusan);
		searchjurusan.setWidth("90%");
		searchjurusan.addEventListener("onChange", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});

		// Tombol "Cari" SEBARIS pada baris filter agar SELALU terlihat (tak tergantung tinggi North).
		MyToolbarbuttonConfig btnCariSebaris = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		btnCariSebaris.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		row.appendChild(btnCariSebaris);

		Toolbar toolbar = new Toolbar();
		ais.ui.util.BanboxFilterToggle.pasang(north, searchgrid, toolbar);
		// toolbar.setHeight("25px");
		toolbar.setParent(div);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		toolbar.appendChild(Common.createCleanButton(this, this));

		/* Paging server-side: grid di Center, kontrol paging di South
		 * (pola seragam AmbilDataPagingHelper, menggantikan mold "paging"
		 * client-side yang dibatasi MAX_RESULT_100). */
		grid = new MyGrid();
		pagingHelper.pasangOnPaging(new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);
			}
		});
		pagingHelper.pasangGridDanPaging(center, grid);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tahun Angkatan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Program");


		onSearchDefault(null);

	}

	public Criteria initCriteria(Session session, boolean isOrder) {
		Dosen dosen = (Dosen) searchdosen.getAttribute("myValue");

		StatusMahasiswa statusMahasiswa = (StatusMahasiswa) (searchstatus.getSelectedItem() == null
				|| searchstatus.getSelectedItem().getValue() == null ? null
						: searchstatus.getSelectedItem().getValue());

		Criterion criteriaStatus = Restrictions.sqlRestriction("true");
		if (statusMahasiswa != null) {
			String sql = "this_.id in (select mahasiswa from history_status_mahasiswa where status_mahasiswa="
					+ statusMahasiswa.getId() + " and tahunakademik = '" + Common.getCurrentTahunAkademik()
					+ "' and semester%2=" + (Common.isNowSemensterGanjil() ? 1 : 0) + ")";

			criteriaStatus = Restrictions.sqlRestriction(sql);
		}

		String kel = kelas.getValue();

		Criteria criteria = session.createCriteria(Mahasiswa.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.add(dosen != null ? CommonSearchFilterHelper.eqIdValue("dosenPa", dosen.getId(), false) : Restrictions.sqlRestriction("1=1"))

				.add(indsMhsPerkuliahan == null || indsMhsPerkuliahan.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("id", indsMhsPerkuliahan))

				.add(kel != null && !kel.trim().isEmpty() ? Restrictions.ilike("kelas", kel.trim(), MatchMode.EXACT)
						: Restrictions.sqlRestriction("true"))

				.add(criteriaStatus)

				.add(CommonSearchFilterHelper.eqSelectedWithId("statusAwalMahasiswa", searchstatusawal, false))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(nim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("nim", nim.getValue().trim(), MatchMode.ANYWHERE))

				.add(tahunangkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunangkatan", tahunangkatan.getValue().intValue()))

				.add(searchprogram.getSelectedItem() == null || searchprogram.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("program", searchprogram.getSelectedItem().getValue()))

				.add(CommonSearchFilterHelper.eqSelectedWithId("jurusan", searchjurusan, false))

				.createAlias("jurusan", "jurusan", Criteria.LEFT_JOIN)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		if (isOrder) {
			criteria.addOrder(Order.desc("tahunangkatan")).addOrder(Order.asc("nim"));
		}

		if (hanyaAlumni) {
			criteria.add(Restrictions.eq("statusKeluar.id", 1L));
		}

		if (perguruanTinggi != null) {
			criteria.createAlias("jurusan.fakultas", "fakultas", Criteria.LEFT_JOIN)
					.add(CommonSearchFilterHelper.eqValue("fakultas.perguruanTinggi", perguruanTinggi, false));
		}

		return criteria;
	}

	public void onSearchDefault(Event event) {
		List<Mahasiswa> mahasiswa = pagingHelper.cari(new AmbilDataPagingHelper.CriteriaFactory() {
			@Override
			public Criteria initCriteria(Session session, boolean isOrder) {
				return AmbilDataMahasiswaBanbox.this.initCriteria(session, isOrder);
			}
		}, Mahasiswa.class, new AmbilDataPagingHelper.Inisialisasi<Mahasiswa>() {
			@Override
			public void init(Mahasiswa m) {
				/* Relasi yang dipakai MahasiswaRenderer disiapkan selagi
				 * session masih terbuka (entity detached setelah ini). */
				try {
					Hibernate.initialize(m.getJurusan());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaBanbox.java:517");
				}
				try {
					Hibernate.initialize(m.getStatusKeluar());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaBanbox.java:521");
				}
				try {
					Hibernate.initialize(m.getStatusAwalMahasiswa());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/AmbilDataMahasiswaBanbox.java:525");
				}
			}
		});

		ListModel strset = new SimpleListModel(mahasiswa);
		grid.setRowRenderer(new MahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
