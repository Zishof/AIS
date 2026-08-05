package ais.action.master;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistBaruPenilaianOlehDosen;
import ais.database.model.ChecklistPenilaianDosen;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.GrupChecklistPenilaianDosen;
import ais.database.model.Jurusan;
import ais.database.model.Konfigurasi;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ChecklistPenilaianOlehDosenAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	// private MyToolbarbuttonConfig add;
	private South south;
	private Dosen dosen;
	private boolean masukan_hasuk_diisi;

	public void onIsiAngketDosenSelesai(Event event) {
		execution.sendRedirect(execution.getContextPath() + "/main");

	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		Tbmuser tbmuser = Common.getCurrentUser();

		dosen = tbmuser == null ? null : tbmuser.ambilDosen();

		if (execution.getParameter("dosen") != null && Common.isNumber(execution.getParameter("dosen"))) {
			dosen = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen").trim()))).uniqueResult();
			if (dosen != null) {
				south.setVisible(false);
			}
		}

		if (dosen == null) {
			alert("Anda harus login sebagai dosen");
			execution.sendRedirect(execution.getContextPath() + "/main");
			return;
		}

		onSearchDefault(null);
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});
	}

	class DataRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Object[] data = (Object[]) arg1;
			final MyDetail detail = new MyDetail();
			final String ta = data[0].toString();
			final String smt = data[1].toString();
			detail.setParent(arg0);

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {

						MyGrid grid = new MyGrid();
						grid.setWidth("100%");
						grid.setParent(detail);

						Columns columns = new Columns();
						columns.setParent(grid);
						MyColumnConfig column = new MyColumnConfig("Mata Kuliah");
						column.setParent(columns);
						column.setWidth("40%");

						column = new MyColumnConfig("Nama Dosen");
						column.setParent(columns);

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

						Session session = HibernateUtil.currentSession();
						@SuppressWarnings("unchecked")
						List<Perkuliahan> perkuliahans = session.createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criterion)
								.add(Restrictions.eq("ganjilGenap", smt)).add(Restrictions.eq("tahunAjaran", ta))
								.list();

						ListModel strset = new SimpleListModel(perkuliahans);
						grid.setRowRenderer(new ChecklistRenderer());
						grid.setModelCheckMobile(strset);

					}
				}
			};

			detail.addEventListener("onOpen", eventListener);

			new Label(ta).setParent(arg0);
			new Label(smt).setParent(arg0);

			String semesterMulai = Common.isNowSemensterGanjil() ? Perkuliahan.GANJIL : Perkuliahan.GENAP;
			if (semesterMulai.equals(smt) && Common.getCurrentTahunAkademik().equals(ta)) {
				arg0.setStyle("border:0px;background: #C2FFA3;");
				detail.setOpen(true);
				eventListener.onEvent(null);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

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

		Projection projection = Projections.projectionList().add(Projections.groupProperty("tahunAjaran"))
				.add(Projections.groupProperty("ganjilGenap"));

		List<Object[]> mengajar = HibernateUtil.currentSession().createCriteria(Perkuliahan.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))).add(criterion)
				.setProjection(projection).add(Restrictions.isNotNull("tahunAjaran"))
				.add(Restrictions.isNotNull("ganjilGenap")).addOrder(Order.desc("tahunAjaran"))
				.addOrder(Order.desc("ganjilGenap")).list();

		ListModel strset = new SimpleListModel(mengajar);
		grid.setRowRenderer(new DataRenderer());
		grid.setModelCheckMobile(strset);

	}

	class ChecklistRenderer extends ais.ui.util.MyRowRenderer {

		public ChecklistRenderer() {

		}

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Perkuliahan perkuliahan = (Perkuliahan) arg1;
			arg0.setStyle("background: transparent;");
			arg0.setValign("top");
			new Label(perkuliahan.getMatakuliah().getNama()).setParent(arg0);

			MyGrid myGrid = new MyGrid();
			myGrid.setHeight("100%");
			myGrid.setWidth("100%");
			myGrid.setParent(arg0);

			Rows rows = new Rows();
			rows.setParent(myGrid);

			createNewRowDosen(perkuliahan, rows);

		}

		private void createNewRowDosen(final Perkuliahan perkuliahan, Rows rows) throws Exception {
			final MyFormRow row = new MyFormRow();row.setValign("top");
			row.setParent(rows);

			Fakultas fakultas = perkuliahan.getJurusan().getFakultas();
			Jurusan jurusan = perkuliahan.getJurusan();
			String program = perkuliahan.getProgram();

			Session session = HibernateUtil.currentSession();
			Integer jumlahChecklist = ((Number) session.createCriteria(ChecklistPenilaianDosen.class)
					.createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen")

					.createAlias("grupChecklistPenilaianDosen.angketPenilaianDosen", "angketPenilaianDosen")
					.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.fakultas", fakultas),
							Restrictions.isNull("angketPenilaianDosen.fakultas")))

					.add(Restrictions.eq("angketPenilaianDosen.untukDosen", true))

					.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.jurusan", jurusan),
							Restrictions.isNull("angketPenilaianDosen.jurusan")))

					.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", ""),
							Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", program),
									Restrictions.isNull("angketPenilaianDosen.program"))))

					.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianDosen.aktif", true),
							Restrictions.isNull("grupChecklistPenilaianDosen.aktif")))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();

			String kodeUnik = perkuliahan.getId() + "_" + dosen.getId();
			ChecklistBaruPenilaianOlehDosen checklistBaruPenilaianOlehDosen = (ChecklistBaruPenilaianOlehDosen) session
					.createCriteria(ChecklistBaruPenilaianOlehDosen.class).add(Restrictions.eq("kodeUnik", kodeUnik))
					.setMaxResults(1).uniqueResult();
			Integer jumlahSaved = checklistBaruPenilaianOlehDosen == null ? 0 : checklistBaruPenilaianOlehDosen.count();

			if (jumlahSaved > jumlahChecklist) {
				jumlahChecklist = jumlahSaved;
			}

			row.setVisible(jumlahChecklist.intValue() > 0);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			CommonMedia.tampilkanGambarKecil(dosen).setParent(vbox);
			new Label(dosen.getNama()).setParent(vbox);

			final Label labelSudahTerisi;
			(labelSudahTerisi = new Label((jumlahChecklist.equals(jumlahSaved) ? "Telah diisi" : "Belum terisi") + " - "
					+ (jumlahSaved + " dari " + jumlahChecklist + " isian"))).setParent(row);

			if (jumlahChecklist.equals(jumlahSaved)) {
				row.setStyle("border:0px;background: yellow;");
			}

			Hbox toolbar = new Hbox();
			toolbar.setParent(row);
			MyButtonConfig button = new MyButtonConfig("Lakukan Penilaian", "/img/Check-icon.png");
			button.setOrient("vertical");
			button.setWidth("100%");
			button.setParent(toolbar);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

							init(perkuliahan, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Fakultas fakultas = perkuliahan.getJurusan().getFakultas();
									Jurusan jurusan = perkuliahan.getJurusan();
									String program = perkuliahan.getProgram();

									Session session = HibernateUtil.currentSession();
									Integer jumlahChecklist = ((Number) session
											.createCriteria(ChecklistPenilaianDosen.class)
											.createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen")

											.createAlias("grupChecklistPenilaianDosen.angketPenilaianDosen",
													"angketPenilaianDosen")

											.add(Restrictions.eq("angketPenilaianDosen.untukDosen", true))

											.add(Restrictions.or(
													Restrictions.eq("angketPenilaianDosen.fakultas", fakultas),
													Restrictions.isNull("angketPenilaianDosen.fakultas")))

											.add(Restrictions.or(
													Restrictions.eq("angketPenilaianDosen.jurusan", jurusan),
													Restrictions.isNull("angketPenilaianDosen.jurusan")))

											.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", ""),
													Restrictions.or(
															Restrictions.eq("angketPenilaianDosen.program", program),
															Restrictions.isNull("angketPenilaianDosen.program"))))

											.add(Restrictions.or(
													Restrictions.eq("grupChecklistPenilaianDosen.aktif", true),
													Restrictions.isNull("grupChecklistPenilaianDosen.aktif")))
											.add(Restrictions.or(Restrictions.eq("aktif", true),
													Restrictions.isNull("aktif")))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();

									String kodeUnik = perkuliahan.getId() + "_" + dosen.getId();
									ChecklistBaruPenilaianOlehDosen checklistBaruPenilaianOlehDosen = (ChecklistBaruPenilaianOlehDosen) session
											.createCriteria(ChecklistBaruPenilaianOlehDosen.class)
											.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
									Integer jumlahSaved = checklistBaruPenilaianOlehDosen == null ? 0
											: checklistBaruPenilaianOlehDosen.count();

									if (jumlahSaved > jumlahChecklist) {
										jumlahChecklist = jumlahSaved;
									}

									labelSudahTerisi.setValue(
											(jumlahChecklist.equals(jumlahSaved) ? "Telah diisi" : "Belum terisi")
													+ " - "
													+ (jumlahSaved + " dari " + jumlahChecklist + " telah terisi"));

									if (jumlahChecklist.equals(jumlahSaved)) {
										row.setStyle("border:0px;background: yellow;");
									}

								}
							});
							addWindow.setHeight("95%");
							addWindow.setWidth("95%");
							addWindow.setVisible(true);
							addWindow.onModal();
						}
					});

				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	private void init(final Perkuliahan perkuliahan, final EventListener eventListener) throws Exception {
		addWindow.setTitle("Penilaian Dosen");

		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Rows rows = new Rows();
		rows.setParent(grid);
		Row row;

		row = new MyFormRow();
		row.setParent(rows);

		MyGrid gridDataDosenLagi = new MyGrid();
		gridDataDosenLagi.setParent(row);
		Columns columns = new Columns();
		columns.setParent(gridDataDosenLagi);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);

		Rows rowsDataDosen = new Rows();
		rowsDataDosen.setParent(gridDataDosenLagi);

		Row rowDataDosen;

		rowDataDosen = new MyFormRow();

		rowDataDosen.setParent(rowsDataDosen);

		MyGrid gridDataDosen = new MyGrid();
		gridDataDosen.setParent(rowDataDosen);
		columns = new Columns();
		columns.setParent(gridDataDosen);
		column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");
		column = new MyColumnConfig();
		column.setParent(columns);

		rowsDataDosen = new Rows();
		rowsDataDosen.setParent(gridDataDosen);

		rowDataDosen = new MyFormRow();

		rowDataDosen.setParent(rowsDataDosen);
		rowDataDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Mata Kuliah  ")));
		rowDataDosen.appendChild(
				new Label(perkuliahan.getMatakuliah().getKode() + " - " + perkuliahan.getMatakuliah().getNama()));

		rowDataDosen = new MyFormRow();

		rowDataDosen.setParent(rowsDataDosen);
		rowDataDosen.appendChild(new Label("Semester / Kelas "));
		rowDataDosen.appendChild(new Label(perkuliahan.getSemester() + " / " + perkuliahan.getKelas()));

		Fakultas fakultas = perkuliahan.getJurusan().getFakultas();
		Jurusan jurusan = perkuliahan.getJurusan();
		String program = perkuliahan.getProgram();

		Session session = HibernateUtil.currentSession();
		List<GrupChecklistPenilaianDosen> grupChecklistPenilaianDosens = session
				.createCriteria(GrupChecklistPenilaianDosen.class)

				.createAlias("angketPenilaianDosen", "angketPenilaianDosen", Criteria.LEFT_JOIN)

				.add(Restrictions.eq("angketPenilaianDosen.untukDosen", true))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.fakultas", fakultas),
						Restrictions.isNull("angketPenilaianDosen.fakultas")))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.jurusan", jurusan),
						Restrictions.isNull("angketPenilaianDosen.jurusan")))

				.add(Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", ""),
						Restrictions.or(Restrictions.eq("angketPenilaianDosen.program", program),
								Restrictions.isNull("angketPenilaianDosen.program"))))

				.addOrder(Order.asc("angketPenilaianDosen.kode")).addOrder(Order.asc("isi"))
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).list();

		String kodeUnik = perkuliahan.getId() + "_" + dosen.getId();
		ChecklistBaruPenilaianOlehDosen checklistBaruPenilaianOlehDosen = (ChecklistBaruPenilaianOlehDosen) session
				.createCriteria(ChecklistBaruPenilaianOlehDosen.class).add(Restrictions.eq("kodeUnik", kodeUnik))
				.setMaxResults(1).uniqueResult();

		final Textbox masukan = new Textbox(
				checklistBaruPenilaianOlehDosen == null ? "" : checklistBaruPenilaianOlehDosen.getMasukan());
		masukan.setRows(2);
		masukan.setWidth("90%");

		masukan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				String kodeUnik = perkuliahan.getId() + "_" + dosen.getId();
				ChecklistBaruPenilaianOlehDosen checklistBaruPenilaianOlehDosen = (ChecklistBaruPenilaianOlehDosen) HibernateUtil
						.currentSession().createCriteria(ChecklistBaruPenilaianOlehDosen.class)
						.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
				if (checklistBaruPenilaianOlehDosen != null) {
					checklistBaruPenilaianOlehDosen.setMasukan(masukan.getValue().trim());
					Common.refreshSaveOrUpdate(checklistBaruPenilaianOlehDosen);
				} else {
					checklistBaruPenilaianOlehDosen = new ChecklistBaruPenilaianOlehDosen();
					checklistBaruPenilaianOlehDosen.setDosen(dosen);
					checklistBaruPenilaianOlehDosen.setDosen(dosen);
					checklistBaruPenilaianOlehDosen.setPerkuliahan(perkuliahan);
					checklistBaruPenilaianOlehDosen.setKeterangan("");
					checklistBaruPenilaianOlehDosen.setMasukan(masukan.getValue().trim());
					Common.refreshSaveOrUpdate(checklistBaruPenilaianOlehDosen);
				}
			}
		});

		Long idAngket = null;

		for (GrupChecklistPenilaianDosen g : grupChecklistPenilaianDosens) {

			List<ChecklistPenilaianDosen> checklistPenilaianDosens = session
					.createCriteria(ChecklistPenilaianDosen.class).addOrder(Order.asc("isi"))
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
					.add(Restrictions.eq("grupChecklistPenilaianDosen", g)).list();

			Integer jumlahChecklist = Integer.parseInt(Common
					.getKonfigurasi("jumlah_pilihan_checklist_penilaian_dosen_oleh_dosen", "5").getNilai().trim());
			try {
				jumlahChecklist = g.getAngketPenilaianDosen().getJumlahPilihan();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			try {
				if (!checklistPenilaianDosens.isEmpty()
						&& (idAngket == null || !idAngket.equals(g.getAngketPenilaianDosen().getId()))) {

					row = new MyFormRow();
					row.setStyle("background: transparent;");
					row.setParent(rows);

					MyGroupboxStyled groupbox = new MyGroupboxStyled();
					groupbox.setStyle("min-height: 200px;");
					groupbox.setParent(row);
					groupbox.appendChild(new MyCaptionStyled(g.getAngketPenilaianDosen().getIsi()));

					Vbox vboxText = new Vbox();
					vboxText.setParent(groupbox);
					String content = g.getAngketPenilaianDosen().getPetunjuk();

					content = content.replaceAll("\n", "<br>");

					Html html = new ais.ui.util.MyHtml(content);
					html.setStyle("font-family: sans-serif;font-size: 11px;");
					html.setParent(vboxText);

					idAngket = g.getAngketPenilaianDosen().getId();
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			row = new MyFormRow();
			row.setVisible(!checklistPenilaianDosens.isEmpty());
			row.setParent(rows);

			Groupbox groupbox = new Groupbox();
			groupbox.setParent(row);
			groupbox.appendChild(new Caption(g.getIsi()));

			MyGrid gridChecklist = new MyGrid();
			gridChecklist.setParent(groupbox);
			Rows rowsChecklist = new Rows();
			Columns columnsChecklist = new Columns();
			columnsChecklist.setParent(gridChecklist);
			MyColumnConfig columnChecklist = new MyColumnConfig("Pertanyaan");
			columnChecklist.setWidth("70%");
			columnChecklist.setParent(columnsChecklist);

			if (g.getAngketPenilaianDosen() != null && g.getAngketPenilaianDosen().getTampilKeterangan()) {
				MyColumnConfig columnChecklistKeterangan = new MyColumnConfig("Keterangan");
				columnChecklistKeterangan.setWidth("30%");
				columnChecklistKeterangan.setParent(columnsChecklist);
			}

			Row rowChecklist;

			rowsChecklist.setParent(gridChecklist);
			for (final ChecklistPenilaianDosen c : checklistPenilaianDosens) {
				rowChecklist = new MyFormRow();

				rowChecklist.setParent(rowsChecklist);

				Vbox vbox = new Vbox();
				vbox.setWidth("98%");
				rowChecklist.appendChild(vbox);

				vbox.appendChild(new Label(c.getIsi()));

				Integer checklistPenilaianDosenOlehDosen = checklistBaruPenilaianOlehDosen == null ? 0
						: checklistBaruPenilaianOlehDosen.getValue(c);
				JSONObject pilihan = new JSONObject(c.getPilihan());
				final Radiogroup radiogroup = new Radiogroup();
				for (Integer i = 1; i <= jumlahChecklist; i++) {
					MyRadioConfig radio = new MyRadioConfig(
							pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
					radio.setValue(i.toString());
					radio.setAttribute("value", i);
					if (checklistPenilaianDosenOlehDosen != null) {
						radio.setSelected(checklistPenilaianDosenOlehDosen.equals(i));
					}
					radiogroup.appendChild(radio);

				}
				vbox.appendChild(radiogroup);

				final Textbox keterangan = new Textbox(checklistBaruPenilaianOlehDosen == null ? ""
						: checklistBaruPenilaianOlehDosen.getKeteranganValue(c));
				keterangan.setWidth("90%");
				keterangan.setRows(2);
				if (g.getAngketPenilaianDosen() != null && g.getAngketPenilaianDosen().getTampilKeterangan()) {
					rowChecklist.appendChild(keterangan);
				}

				EventListener listener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						// TODO Auto-generated method stub
						onSave(perkuliahan, c, radiogroup.getSelectedItem(), keterangan.getValue().trim(),
								masukan.getValue().trim());

					}
				};

				radiogroup.addEventListener("onCheck", listener);
				keterangan.addEventListener("onChange", listener);

			}

		}

		masukan_hasuk_diisi = Common.bolehKonfigurasi("masukan_penialain_dosen_harus_diisi", Konfigurasi.TIDAK_AKTIF);

		rowDataDosen = new MyFormRow();

		rowDataDosen.setParent(rowsDataDosen);
		rowDataDosen.appendChild(new Label("Masukan/Saran/Komentar " + (masukan_hasuk_diisi ? "*" : "")));
		rowDataDosen.appendChild(masukan);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);

		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Simpan dan Tutup", "/img/save.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				if (masukan_hasuk_diisi && masukan.getValue().trim().isEmpty()) {
					MyMessageboxConfig.show("Masukan/Saran/Komentar harus diisi", "Pemberitahuan",
							MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION, new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									masukan.focus();
								}
							});
					return;
				}

				Session session = HibernateUtil.currentSession();
				Integer jumlahChecklist = ((Number) session.createCriteria(ChecklistPenilaianDosen.class)
						.createAlias("grupChecklistPenilaianDosen", "grupChecklistPenilaianDosen")

						.createAlias("grupChecklistPenilaianDosen.angketPenilaianDosen", "angketPenilaianDosen")
						.add(Restrictions.eq("angketPenilaianDosen.untukDosen", true))

						.add(Restrictions.or(Restrictions.eq("grupChecklistPenilaianDosen.aktif", true),
								Restrictions.isNull("grupChecklistPenilaianDosen.aktif")))
						.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
						.setProjection(Projections.rowCount()).uniqueResult()).intValue();

				String kodeUnik = perkuliahan.getId() + "_" + dosen.getId();
				ChecklistBaruPenilaianOlehDosen checklistBaruPenilaianOlehDosen = (ChecklistBaruPenilaianOlehDosen) session
						.createCriteria(ChecklistBaruPenilaianOlehDosen.class)
						.add(Restrictions.eq("kodeUnik", kodeUnik)).setMaxResults(1).uniqueResult();
				Integer jumlahSaved = checklistBaruPenilaianOlehDosen == null ? 0
						: checklistBaruPenilaianOlehDosen.count();

				System.out.println("jumlahChecklist = " + jumlahChecklist + ", jumlahSaved = " + jumlahSaved);

				if (jumlahSaved > jumlahChecklist) {
					jumlahChecklist = jumlahSaved;
				}

				eventListener.onEvent(event);

				if (jumlahChecklist.equals(jumlahSaved)) {
					addWindow.setVisible(false);
					MyMessageboxConfig.show("Data berhasil disimpan", "Informasi", 1, MyMessageboxConfig.INFORMATION);
				} else {
					MyMessageboxConfig.show("Isilah data secara lengkap!", "Pemberitahuan", MyMessageboxConfig.OK,
							MyMessageboxConfig.EXCLAMATION);
					return;
				}

			}
		});
		cancel.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Perkuliahan perkuliahan, ChecklistPenilaianDosen checklistPenilaianDosen, Radio radio,
			String keterangan, String masukan) throws Exception {

		Session session = HibernateUtil.currentSession();

		String kodeUnik = perkuliahan.getId() + "_" + dosen.getId();
		ChecklistBaruPenilaianOlehDosen checklistBaruPenilaianOlehDosen = (ChecklistBaruPenilaianOlehDosen) session
				.createCriteria(ChecklistBaruPenilaianOlehDosen.class).add(Restrictions.eq("kodeUnik", kodeUnik))
				.setMaxResults(1).uniqueResult();
		if (checklistBaruPenilaianOlehDosen == null) {
			checklistBaruPenilaianOlehDosen = new ChecklistBaruPenilaianOlehDosen();
		}
		checklistBaruPenilaianOlehDosen.setValue(radio == null ? 0 : Integer.parseInt(radio.getValue().toString()), dosen,
				perkuliahan, checklistPenilaianDosen, keterangan);
		checklistBaruPenilaianOlehDosen.setMasukan(masukan);
		Common.refreshSaveOrUpdate(session, checklistBaruPenilaianOlehDosen);

		return true;
	}
}
