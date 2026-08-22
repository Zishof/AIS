package ais.action.master.helper.generic;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Div;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AktifitasPerkuliahanHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.FormulirKegiatan;
import ais.database.model.GeneralValueObject;
import ais.database.model.MahasiswaRequestTugasAkhir;
import ais.database.model.Perkuliahan;
import ais.database.model.Pertemuan;
import ais.database.model.Skripsi;
import ais.database.model.Tbmuser;
import ais.database.model.Wisuda;
import ais.database.model.kkn.KelompokKkn;
import ais.database.model.pkl.KelompokPkl;
import ais.database.model.sekolah.JadwalPelajaran;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class AmbilDataTemplatePembelajaran extends MyWindow {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6452461056684904810L;
	private MyGrid grid;
	private EventListener eventListener;

	private MyTextbox nama;
	private Perkuliahan perkuliahan;
	private KelompokKkn kelompokKkn;
	private KelompokPkl kelompokPkl;
	private MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir;
	private Skripsi skripsi;
	private Combobox tahunAkademik;
	private Combobox semesterAbsensi;
	private JadwalPelajaran jadwalPelajaran;
	private FormulirKegiatan formulirKegiatan;
	private Wisuda wisuda;
	private MyTextbox searchnamadsn;

	public AmbilDataTemplatePembelajaran(final Perkuliahan perkuliahan, final KelompokKkn kelompokKkn,
			final KelompokPkl kelompokPkl, final MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir,
			final Skripsi skripsi, FormulirKegiatan formulirKegiatan, Wisuda wisuda) {
		super();
		this.perkuliahan = perkuliahan;
		this.kelompokKkn = kelompokKkn;
		this.kelompokPkl = kelompokPkl;
		this.mahasiswaRequestTugasAkhir = mahasiswaRequestTugasAkhir;
		this.skripsi = skripsi;
		this.formulirKegiatan = formulirKegiatan;
		this.wisuda = wisuda;
		display();
		onSearchDefault(null);
	}

	public AmbilDataTemplatePembelajaran(JadwalPelajaran jadwalPelajaran) {
		this.jadwalPelajaran = jadwalPelajaran;
		display();
		onSearchDefault(null);
	}

	class TemplatePembelajaranRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final GeneralValueObject templatePembelajaran = (GeneralValueObject) arg1;
			arg0.setAttribute("templatePembelajaran", templatePembelajaran);

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						ais.ui.util.MyDiv myGroupbox = new ais.ui.util.MyDiv();
						myGroupbox.setStyle("min-height: 500px;");
						myGroupbox.setParent(detail);

						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(myGroupbox);
						grid.setWidth("100%");
						grid.setHeight("100%");

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Materi");
						column.setParent(columns);

						JadwalPelajaran jadwalPelajaran = null;
						if (templatePembelajaran instanceof JadwalPelajaran) {
							jadwalPelajaran = (JadwalPelajaran) templatePembelajaran;
						}
						Perkuliahan perkuliahan = null;
						if (templatePembelajaran instanceof Perkuliahan) {
							perkuliahan = (Perkuliahan) templatePembelajaran;
						}
						KelompokKkn kelompokKkn = null;
						if (templatePembelajaran instanceof KelompokKkn) {
							kelompokKkn = (KelompokKkn) templatePembelajaran;
						}
						KelompokPkl kelompokPkl = null;
						if (templatePembelajaran instanceof KelompokPkl) {
							kelompokPkl = (KelompokPkl) templatePembelajaran;
						}
						FormulirKegiatan formulirKegiatan = null;
						if (templatePembelajaran instanceof FormulirKegiatan) {
							formulirKegiatan = (FormulirKegiatan) templatePembelajaran;
						}
						MahasiswaRequestTugasAkhir mahasiswaRequestTugasAkhir = null;
						if (templatePembelajaran instanceof MahasiswaRequestTugasAkhir) {
							mahasiswaRequestTugasAkhir = (MahasiswaRequestTugasAkhir) templatePembelajaran;
						}
						Skripsi skripsi = null;
						if (templatePembelajaran instanceof Skripsi) {
							skripsi = (Skripsi) templatePembelajaran;
						}
						Wisuda wisuda = null;
						if (templatePembelajaran instanceof Wisuda) {
							wisuda = (Wisuda) templatePembelajaran;
						}

						Rows rows = new Rows();
						rows.setParent(grid);
						@SuppressWarnings("unchecked")
						List<Pertemuan> pertemuans = HibernateUtil.currentSession().createCriteria(Pertemuan.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.addOrder(Order.asc("tanggal")).addOrder(Order.asc("id"))

								.add(jadwalPelajaran == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("jadwalPelajaran", jadwalPelajaran))

								.add(perkuliahan == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("perkuliahan", perkuliahan))

								.add(kelompokKkn == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("kelompokKkn", kelompokKkn))

								.add(kelompokPkl == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("kelompokPkl", kelompokPkl))

								.add(formulirKegiatan == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("formulirKegiatan", formulirKegiatan))

								.add(mahasiswaRequestTugasAkhir == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("mahasiswaRequestTugasAkhir", mahasiswaRequestTugasAkhir))

								.add(skripsi == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("skripsi", skripsi))

								.add(wisuda == null ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("wisuda", wisuda))

								.list();

						for (final Pertemuan pertemuan : pertemuans) {
							MyFormRow row = new MyFormRow();row.setValign("top");
							row.setParent(rows);

							Vbox vbox = new Vbox();
							vbox.setParent(row);

							new MyLabelAgakKecil(pertemuan.getTopik()).setParent(vbox);
							new MyLabelAgakKecil(pertemuan.getMetodePembelajaran()).setParent(vbox);
							new MyLabelAgakKecil(pertemuan.getBukuRujukan1()).setParent(vbox);
							new MyLabelAgakKecil(pertemuan.getBukuRujukan2()).setParent(vbox);
							new MyLabelAgakKecil(pertemuan.getDosenTamu()).setParent(vbox);
							new MyLabelAgakKecil(pertemuan.getDosenTamu2()).setParent(vbox);

							AktifitasPerkuliahanHelper.createKeterangan(pertemuan, null).setParent(vbox);
						}
					}

				}
			});

			final Radio checkbox = new Radio(templatePembelajaran.getNama());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			if (jadwalPelajaran != null && jadwalPelajaran.getId().equals(templatePembelajaran.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			} else if (perkuliahan != null && perkuliahan.getId().equals(templatePembelajaran.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			} else if (kelompokKkn != null && kelompokKkn.getId().equals(templatePembelajaran.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			} else if (kelompokPkl != null && kelompokPkl.getId().equals(templatePembelajaran.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			} else if (formulirKegiatan != null && formulirKegiatan.getId().equals(templatePembelajaran.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			} else if (mahasiswaRequestTugasAkhir != null
					&& mahasiswaRequestTugasAkhir.getId().equals(templatePembelajaran.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			} else if (skripsi != null && skripsi.getId().equals(templatePembelajaran.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			} else if (wisuda != null && wisuda.getId().equals(templatePembelajaran.getId())) {
				checkbox.setChecked(true);
				checkbox.setDisabled(true);
			}

			if (templatePembelajaran instanceof JadwalPelajaran) {
				checkbox.setLabel(((JadwalPelajaran) templatePembelajaran).infoSimple());
			}

			if (templatePembelajaran instanceof Perkuliahan) {
				checkbox.setLabel(((Perkuliahan) templatePembelajaran).infoSimple());
			}

			if (templatePembelajaran instanceof KelompokKkn) {
				checkbox.setLabel(((KelompokKkn) templatePembelajaran).getNama_kelompok());
			}

			if (templatePembelajaran instanceof KelompokPkl) {
				checkbox.setLabel(((KelompokPkl) templatePembelajaran).getNama_kelompok());
			}

			if (templatePembelajaran instanceof FormulirKegiatan) {
				checkbox.setLabel(((FormulirKegiatan) templatePembelajaran).getNama());
			}

			if (templatePembelajaran instanceof MahasiswaRequestTugasAkhir) {
				checkbox.setLabel(((MahasiswaRequestTugasAkhir) templatePembelajaran).getMahasiswa().getNim() + "-"
						+ ((MahasiswaRequestTugasAkhir) templatePembelajaran).getMahasiswa().getNama() + "-"
						+ ((MahasiswaRequestTugasAkhir) templatePembelajaran).getJudul());
			}

			if (templatePembelajaran instanceof Skripsi) {
				checkbox.setLabel(((Skripsi) templatePembelajaran).getMahasiswa().getNim() + "-"
						+ ((Skripsi) templatePembelajaran).getMahasiswa().getNama() + "-"
						+ ((Skripsi) templatePembelajaran).getJudul());
			}

			if (templatePembelajaran instanceof Wisuda) {
				checkbox.setLabel(((Wisuda) templatePembelajaran).getNama());
			}
		}

	}

	public void display() {

		Radiogroup radiogroup = new Radiogroup();
		radiogroup.setParent(this);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(radiogroup);
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		org.zkoss.zul.Grid gridUtama = new org.zkoss.zul.Grid();
		gridUtama.setWidth("100%");
		ais.ui.util.ZkCompat.setFlex(gridUtama, true);
		gridUtama.setParent(center);
		Rows rowsUtama = new Rows();
		rowsUtama.setParent(gridUtama);

		Row rowUtama = new Row();
		rowUtama.setParent(rowsUtama);

		MyGrid searchgrid = new MyGrid();
		searchgrid.setWidth("100%");
		searchgrid.setParent(rowUtama);

		Rows rows = new Rows();
		rows.setParent(searchgrid);

		MyFormRow row = new MyFormRow();row.setValign("top");
		row.setParent(rows);

		if (perkuliahan != null) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Matakuliah"));
			row.appendChild(nama = new MyTextbox(perkuliahan == null || perkuliahan.getMatakuliah() == null ? ""
					: perkuliahan.getMatakuliah().getNama()));
		} else if (jadwalPelajaran != null) {
			row.appendChild(new ais.ui.util.MyLabelConfig("Matapelajaran"));
			row.appendChild(
					nama = new MyTextbox(jadwalPelajaran == null || jadwalPelajaran.getMatapelajaran() == null ? ""
							: jadwalPelajaran.getMatapelajaran().getNama()));
		} else {
			row.appendChild(new ais.ui.util.MyLabelConfig("Cari"));
			row.appendChild(nama = new MyTextbox());
		}
		nama.addEventListener(Events.ON_OK, new EventListener() {
			public void onEvent(Event event) throws Exception {
				onSearchDefault(event);
			}
		});

		if (jadwalPelajaran != null) {

			Tbmuser tbmuser = Common.getCurrentUser();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Guru"));
			row.appendChild(searchnamadsn = new MyTextbox(tbmuser == null || tbmuser.getGuru() == null
					? (jadwalPelajaran.getGuru() == null ? "" : jadwalPelajaran.getGuru().getNama())
					: tbmuser.getGuru().getNama()));

			searchnamadsn.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Ajaran"));
			row.appendChild(tahunAkademik = new Combobox());
			Common.generateTahunAjaranDanSemua(tahunAkademik);
			tahunAkademik.setWidth("90%");
			tahunAkademik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
			tahunAkademik.setReadonly(true);

			if (jadwalPelajaran == null)
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
			row.appendChild(semesterAbsensi = new Combobox());
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			semesterAbsensi.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			semesterAbsensi.appendChild(comboitem);
			semesterAbsensi.setWidth("90%");

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			semesterAbsensi.appendChild(comboitem);
			semesterAbsensi.setSelectedItem(comboitem);

			semesterAbsensi.setReadonly(true);
			semesterAbsensi.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
		}

		else if (perkuliahan != null) {

			Tbmuser tbmuser = Common.getCurrentUser();

			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Dosen"));
			row.appendChild(searchnamadsn = new MyTextbox(tbmuser == null || tbmuser.getDosen() == null
					? (perkuliahan.getDosen1() == null ? "" : perkuliahan.getDosen1().getNama())
					: tbmuser.getDosen().getNama()));

			searchnamadsn.addEventListener(Events.ON_OK, new EventListener() {
				public void onEvent(Event event) throws Exception {
					onSearchDefault(event);
				}
			});

			row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
			row.appendChild(tahunAkademik = new Combobox());
			Common.generateTahunAjaranDanSemua(tahunAkademik);
			tahunAkademik.setWidth("90%");
			tahunAkademik.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});
			tahunAkademik.setReadonly(true);

			if (jadwalPelajaran == null)
				row.appendChild(new ais.ui.util.MyLabelConfig("Semester"));
			row.appendChild(semesterAbsensi = new Combobox());
			org.zkoss.zul.Comboitem comboitem = new org.zkoss.zul.Comboitem();
			comboitem.setLabel(Perkuliahan.GENAP);
			comboitem.setValue(Perkuliahan.GENAP);
			semesterAbsensi.appendChild(comboitem);
			comboitem = new MyComboitemConfig();
			comboitem.setLabel(Perkuliahan.GANJIL);
			comboitem.setValue(Perkuliahan.GANJIL);
			semesterAbsensi.appendChild(comboitem);
			semesterAbsensi.setWidth("90%");

			comboitem = new MyComboitemConfig();
			comboitem.setLabel("Semua");
			comboitem.setValue(null);
			semesterAbsensi.appendChild(comboitem);
			semesterAbsensi.setSelectedItem(comboitem);

			semesterAbsensi.setReadonly(true);
			semesterAbsensi.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					onSearchDefault(arg0);
				}
			});

		}

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		Row rowKedua = new Row();
		rowKedua.setParent(rowsUtama);
		toolbar.setHeight("32px");
		toolbar.setParent(rowKedua);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
		button.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				onSearchDefault(null);
			}
		});
		button.setParent(toolbar);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(150);
		grid.getPagingChild().setMold("os");
		Row rowKetiga = new Row();
		rowKetiga.setParent(rowsUtama);
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(rowKetiga);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Rencana Pembelajaran");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataTemplatePembelajaran.this.detach();
			}
		});
		cancel.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		button.addEventListener("onClick", new EventListener() {
			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				if (eventListener != null && grid.getRows() != null && grid.getRows().getChildren() != null) {
					List<GeneralValueObject> templatePembelajarans = new ArrayList<GeneralValueObject>();
					List<Row> rows = grid.getRows().getChildren();
					for (Row row : rows) {
						try {
							Radio checkbox = (Radio) row.getAttribute("checkbox");
							if (checkbox != null && checkbox.isChecked() && !checkbox.isDisabled()) {
								GeneralValueObject myTemplatePembelajaran = (GeneralValueObject) row
										.getAttribute("templatePembelajaran");
								templatePembelajarans.add(myTemplatePembelajaran);
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/generic/AmbilDataTemplatePembelajaran.java:537");
							// TODO: handle exception
						}
					}
					Event myEvent = new Event("myEvent", event.getTarget(), templatePembelajarans);
					eventListener.onEvent(myEvent);
				}
				AmbilDataTemplatePembelajaran.this.detach();
			}
		});
		button.setParent(toolbar);

	}
	
	
	
	
	

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		Session session = HibernateUtil.currentSession();

		Order order = Order.desc("id");
		Projection projection = Projections.groupProperty("id");
		if (jadwalPelajaran != null) {
			projection = Projections.groupProperty("jadwalPelajaran");
			order = Order.desc("jadwalPelajaran");
		} else if (perkuliahan != null) {
			projection = Projections.groupProperty("perkuliahan");
			order = Order.desc("perkuliahan");
		} else if (kelompokKkn != null) {
			projection = Projections.groupProperty("kelompokKkn");
			order = Order.desc("kelompokKkn");
		} else if (kelompokPkl != null) {
			projection = Projections.groupProperty("kelompokPkl");
			order = Order.desc("kelompokPkl");
		} else if (formulirKegiatan != null) {
			projection = Projections.groupProperty("formulirKegiatan");
			order = Order.desc("formulirKegiatan");
		} else if (wisuda != null) {
			projection = Projections.groupProperty("wisuda");
			order = Order.desc("wisuda");
		} else if (skripsi != null) {
			projection = Projections.groupProperty("skripsi");
			order = Order.desc("skripsi");
		} else if (mahasiswaRequestTugasAkhir != null) {
			projection = Projections.groupProperty("mahasiswaRequestTugasAkhir");
			order = Order.desc("mahasiswaRequestTugasAkhir");
		}

		Criteria criteria = session.createCriteria(Pertemuan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				.setProjection(projection)

				.addOrder(order)

				.add(jadwalPelajaran != null ? Restrictions.isNotNull("jadwalPelajaran")
						: Restrictions.sqlRestriction("1=1"))

				.add(perkuliahan != null ? Restrictions.isNotNull("perkuliahan") : Restrictions.sqlRestriction("1=1"))

				.add(kelompokKkn != null ? Restrictions.isNotNull("kelompokKkn") : Restrictions.sqlRestriction("1=1"))

				.add(kelompokPkl != null ? Restrictions.isNotNull("kelompokPkl") : Restrictions.sqlRestriction("1=1"))

				.add(formulirKegiatan != null ? Restrictions.isNotNull("formulirKegiatan")
						: Restrictions.sqlRestriction("1=1"))

				.add(wisuda != null ? Restrictions.isNotNull("wisuda") : Restrictions.sqlRestriction("1=1"))

				.add(skripsi != null ? Restrictions.isNotNull("skripsi") : Restrictions.sqlRestriction("1=1"))

				.add(mahasiswaRequestTugasAkhir != null ? Restrictions.isNotNull("mahasiswaRequestTugasAkhir")
						: Restrictions.sqlRestriction("1=1"));

		if (jadwalPelajaran != null) {

			Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");

			if (searchnamadsn != null && !searchnamadsn.getValue().trim().isEmpty()) {
				criteria.createAlias("jadwalPelajaran.guru", "guru", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru2", "guru2", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru3", "guru3", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru4", "guru4", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru5", "guru5", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru6", "guru6", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru7", "guru7", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru8", "guru8", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru9", "guru9", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru10", "guru10", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru11", "guru11", Criteria.LEFT_JOIN)
						.createAlias("jadwalPelajaran.guru12", "guru12", Criteria.LEFT_JOIN);

				criterionNamaDosn = Restrictions.ilike("guru.nama", searchnamadsn.getValue().trim(),
						MatchMode.ANYWHERE);

				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru2.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru3.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru4.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru5.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru6.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru7.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru8.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru9.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru10.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru11.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("guru12.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			}

			criteria.createAlias("jadwalPelajaran", "jadwalPelajaran").add(criterionNamaDosn)
					.createAlias("jadwalPelajaran.matapelajaran", "matapelajaran")
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("matapelajaran.kode", nama.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.ilike("matapelajaran.nama", nama.getValue().trim(),
											MatchMode.ANYWHERE)));

			if (tahunAkademik != null && tahunAkademik.getSelectedItem() != null
					&& tahunAkademik.getSelectedItem().getValue() != null) {
				criteria.add(
						Restrictions.eq("jadwalPelajaran.tahunAjaran", tahunAkademik.getSelectedItem().getValue()));
			}

			if (semesterAbsensi != null && semesterAbsensi.getSelectedItem() != null
					&& semesterAbsensi.getSelectedItem().getValue() != null) {
				criteria.add(Restrictions.eq("jadwalPelajaran.semester",
						semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP) ? 2 : 1));
			}
		}

		if (perkuliahan != null) {

			Criterion criterionNamaDosn = Restrictions.sqlRestriction("1=1");

			if (searchnamadsn != null && !searchnamadsn.getValue().trim().isEmpty()) {
				criteria.createAlias("perkuliahan.dosen1", "dosen1", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen2", "dosen2", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen3", "dosen3", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen4", "dosen4", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen5", "dosen5", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen6", "dosen6", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen7", "dosen7", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen8", "dosen8", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen9", "dosen9", Criteria.LEFT_JOIN)
						.createAlias("perkuliahan.dosen10", "dosen10", Criteria.LEFT_JOIN);

				criterionNamaDosn = Restrictions.ilike("dosen1.nama", searchnamadsn.getValue().trim(),
						MatchMode.ANYWHERE);

				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen2.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen3.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen4.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen5.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen6.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen7.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen8.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen9.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
				criterionNamaDosn = Restrictions.or(criterionNamaDosn,
						Restrictions.ilike("dosen10.nama", searchnamadsn.getValue().trim(), MatchMode.ANYWHERE));
			}

			criteria.createAlias("perkuliahan", "perkuliahan").add(criterionNamaDosn)
					.createAlias("perkuliahan.matakuliah", "matakuliah")
					.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
							: Restrictions.or(
									Restrictions.ilike("matakuliah.kode", nama.getValue().trim(), MatchMode.ANYWHERE),
									Restrictions.ilike("matakuliah.nama", nama.getValue().trim(), MatchMode.ANYWHERE)));

			if (tahunAkademik != null && tahunAkademik.getSelectedItem() != null
					&& tahunAkademik.getSelectedItem().getValue() != null) {
				criteria.add(Restrictions.eq("perkuliahan.tahunAjaran", tahunAkademik.getSelectedItem().getValue()));
			}

			if (semesterAbsensi != null && semesterAbsensi.getSelectedItem() != null
					&& semesterAbsensi.getSelectedItem().getValue() != null) {
				criteria.add(Restrictions.in("perkuliahan.semester",
						semesterAbsensi.getSelectedItem().getValue().equals(Perkuliahan.GENAP) ? Common.genap
								: Common.ganjil));
			}
		}

		List<GeneralValueObject> myTemplatePembelajaran = criteria.setMaxResults(Common.MAX_RESULT).list();

		ListModel strset = new SimpleListModel(myTemplatePembelajaran);
		grid.setRowRenderer(new TemplatePembelajaranRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void setEventListener(EventListener eventListener) {
		this.eventListener = eventListener;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}
