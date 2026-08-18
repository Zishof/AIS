package ais.action.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
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
import org.zkoss.zul.Window;

import ais.common.ChecklistPenilaianHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.ChecklistPenilaianUmum;
import ais.database.model.Dosen;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.JadwalChecklistPenilaianUmum;
import ais.database.model.Mahasiswa;
import ais.database.model.SubGrupChecklistPenilaianUmum;
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

public class ChecklistPenilaianUmumGrupOlehPesertaAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Mahasiswa mahasiswa;

	private Tbmuser tbmuser = Common.getCurrentUser();

	public void onIsiAngketDosenSelesai(Event event) {
		execution.sendRedirect(Common.getRequestHostWithProtocol() + "/main");

	}

	private South southTutup;
	private Dosen dosen;

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

		if (execution.getParameter("mahasiswa") != null) {
			mahasiswa = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa")))).uniqueResult();
			tbmuser = new Tbmuser(mahasiswa);
		} else if (execution.getParameter("dosen") != null) {
			dosen = (Dosen) HibernateUtil.currentSession().createCriteria(Dosen.class)
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen")))).uniqueResult();
			tbmuser = new Tbmuser(dosen);
		} else {
			tbmuser = Common.getCurrentUser();
			mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
			dosen = tbmuser == null ? null : tbmuser.ambilDosen();
		}

		if (southTutup != null) {
			southTutup.setVisible(mahasiswa == null && dosen == null);
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

		int index = 0;

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final Object[] data = (Object[]) arg1;
			// final Detail detail = new Detail();
			final String tahunAkademik = data[0] == null ? "" : data[0].toString();
			final String semester = data[1] == null ? "" : data[1].toString();

			Tbmuser tbmuserDinilai = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(), data[2].toString());

			CommonMedia.tampilkanGambarKecil(tbmuserDinilai).setParent(arg0);

			new Label(tbmuserDinilai.getUserNama()).setParent(arg0);
			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			tbmuserDinilai.tampilkanHp(vbox2);
			tbmuserDinilai.tampilkanEmail(vbox2);

			new Label(tahunAkademik).setParent(arg0);

			new Label(semester).setParent(arg0);

			final Label labelSudahTerisi;
			(labelSudahTerisi = new Label()).setParent(arg0);
			labelSudahTerisi.setWidth("90%");
			labelSudahTerisi.setHeight("95%");

			final EventListener grupChecklistPenilaianUmum = new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event ar) throws Exception {
					Tbmuser tbmuserDinilai = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(),
							data[2].toString());
					Object[] data = ChecklistPenilaianHelper.getJumlahStatusChecklistUmumGrup(tahunAkademik, semester, tbmuserDinilai,
							true);

					Set<Long> checklistPenilaianUmumTerjadwal = (Set<Long>) data[7];
					Set<Long> checklistParameterTerjadwalDipilihUserTsb = (Set<Long>) data[6];

					int jumlahChecklist = checklistPenilaianUmumTerjadwal.size();
					int jumlahSaved = checklistParameterTerjadwalDipilihUserTsb.size();

					arg0.setVisible(jumlahChecklist > 0);

					labelSudahTerisi.setValue((jumlahChecklist == jumlahSaved ? "Telah diisi" : "Belum terisi") + " - "
							+ (jumlahSaved + " dari " + jumlahChecklist + " telah terisi"));
					if (jumlahChecklist == jumlahSaved) {
						arg0.setStyle("border:0px;background: yellow;");
					}
				}
			};

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyButtonConfig button = new MyButtonConfig("Lakukan Penilaian", "/img/Check-icon.png");
			button.setOrient("vertical");
			button.setWidth("100%");
			button.setParent(toolbar);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String diperuntukkan = null;
					Tbmuser tbmuserDinilai = (Tbmuser) ConstantValues.ambil(Tbmuser.class.getName(),
							data[2].toString());
					init(tahunAkademik, semester, tbmuserDinilai, diperuntukkan, grupChecklistPenilaianUmum);
					addWindow.setHeight("95%");
					addWindow.setWidth("95%");
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});

			Common.createDefaultTimer(grupChecklistPenilaianUmum);
			index++;
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Tbmuser tbmuser = Common.getCurrentUser();
		List<Object[]> jadwalChecklistPenilaianUmums = HibernateUtil.currentSession()
				.createSQLQuery("select a.tahunakademik, a.semester, f.tbmuser from jadwal_checklist_penilaian_umum a "
						+ " inner join grup_kuosioner_umum_detail f on (f.grup_kuesioner_umum=a.grup_kuesioner_umum) "
						+ "where a.sampai >= date('" + Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())
						+ "') and a.mulai <= date('" + Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate())
						+ "') and f.tbmuser != '" + (tbmuser == null ? "" : tbmuser.getUserId())
						+ "' group by a.tahunakademik,a.semester,f.tbmuser order by a.tahunakademik desc,a.semester,f.tbmuser desc ")
				.list();

		ListModel strset = new SimpleListModel(jadwalChecklistPenilaianUmums);
		grid.setRowRenderer(new DataRenderer());
		grid.setModelCheckMobile(strset);
	}

	private void init(String tahunAkademik, String semester, Tbmuser tbmuserDinilai, String diperuntukkan,
			final EventListener eventListener) throws Exception {
		addWindow.setTitle("Angket Penilaian");

		Common.clear(addWindow);

		initData(tahunAkademik, semester, tbmuserDinilai, diperuntukkan, addWindow, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(null);
			}
		}, true, false).setParent(createSingleScrollContainer(addWindow));
	}

	private Div createSingleScrollContainer(Window targetWindow) {
		Div container = new Div();
		container.setWidth("100%");
		container.setHeight("100%");
		container.setStyle("height:100%; max-height:100%; overflow-y:auto; overflow-x:hidden; "
				+ "box-sizing:border-box; padding:12px; background:#f8fafc;");
		if (targetWindow != null) {
			targetWindow.appendChild(container);
		}
		return container;
	}

	private void applyQuestionCardStyle(Row row, Vbox container) {
		if (row != null) {
			row.setValign("top");
			ais.ui.util.ZkCompat.setSpans(row, "1");
			row.setStyle("background:#ffffff; border-bottom:1px solid #e5e7eb;");
		}
		if (container != null) {
			container.setWidth("100%");
			container.setStyle("box-sizing:border-box; padding:10px 12px; background:#ffffff; border-radius:8px;");
		}
	}

	private Label createQuestionLabel(String text) {
		Label label = new Label(text == null ? "" : text);
		label.setWidth("100%");
		label.setStyle("font-size:12px; font-weight:bold; color:#334155; line-height:18px; white-space:normal;");
		return label;
	}

	private void styleKeteranganTextbox(Textbox textbox) {
		if (textbox == null) {
			return;
		}
		textbox.setWidth("100%");
		textbox.setRows(2);
		textbox.setStyle("box-sizing:border-box; min-height:46px; border:1px solid #cbd5e1; border-radius:6px; "
				+ "padding:6px; margin-top:6px; background:#f8fafc;");
	}

	private void styleRadioGroup(Radiogroup radiogroup) {
		if (radiogroup != null) {
			radiogroup.setStyle("display:block; padding-top:6px; color:#334155;");
		}
	}

	private Long pertemuanId = null;

	public Div initData(Mahasiswa mahasiswa, Dosen dosen, Tbmuser tbmuser, Tbmuser tbmuserDinilai, String tahunAkademik,
			String semester, String diperuntukkan, Window addWindow, EventListener eventListener, boolean tampilSimpan,
			Long pertemuanId, boolean refresh) throws Exception {
		this.mahasiswa = mahasiswa;
		this.dosen = dosen;
		this.tbmuser = tbmuser;
		this.pertemuanId = pertemuanId;
		return initData(tahunAkademik, semester, tbmuserDinilai, diperuntukkan, addWindow, eventListener, tampilSimpan,
				refresh);
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void dataChecklist(final String tahunAkademik, final String semester, final Tbmuser tbmuserDinilai,
			Row groupboxRow, String diperuntukkan, final EventListener eventListener, boolean tampilSimpan,
			boolean refresh) throws Exception {
		Common.clear(groupboxRow);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(groupboxRow);
		grid.setWidth("100%");
		grid.setHeight("100%");
		Rows rows = new Rows();
		rows.setParent(grid);
		Row row;

		row = new MyFormRow();
		ais.ui.util.ZkCompat.setSpans(row, "2");
		row.setParent(rows);

		MyGrid gridDataDosen = new MyGrid();
		gridDataDosen.setParent(row);
		Columns columns = new Columns();
		columns.setParent(gridDataDosen);
		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("20%");
		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rowsDataDosen = new Rows();
		rowsDataDosen.setParent(gridDataDosen);

		if (tbmuserDinilai != null) {
			Image image = new Image(CommonMedia.getUrlFotoPengguna(tbmuserDinilai));
			image.setWidth("128px");
			A a = new A();
			a.appendChild(image);
			a.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.previewGambar(CommonMedia.getUrlFotoPengguna(tbmuserDinilai));
				}
			});
			MyFormRow rowDataDosen = new MyFormRow();
			ais.ui.util.ZkCompat.setSpans(rowDataDosen, "2");

			rowDataDosen.setParent(rowsDataDosen);
			a.setParent(rowDataDosen);

			rowDataDosen = new MyFormRow();

			rowDataDosen.setParent(rowsDataDosen);
			rowDataDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama")));
			rowDataDosen.appendChild(new Label(tbmuserDinilai.getUserNama()));

			rowDataDosen = new MyFormRow();

			rowDataDosen.setParent(rowsDataDosen);
			rowDataDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Jenis Pengguna")));
			rowDataDosen.appendChild(
					new Label(tbmuserDinilai.hakAkses() == null ? "" : tbmuserDinilai.hakAkses().getRoleName()));
		}

		Row rowDataDosen;
		if (diperuntukkan == null) {
			rowDataDosen = new MyFormRow();

			rowDataDosen.setParent(rowsDataDosen);
			rowDataDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Tahun Akademik  ")));
			rowDataDosen.appendChild(new Label(tahunAkademik));

			rowDataDosen = new MyFormRow();

			rowDataDosen.setParent(rowsDataDosen);
			rowDataDosen.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester ")));
			rowDataDosen.appendChild(new Label(semester));
		}

		Criterion criterion = Restrictions.sqlRestriction(
				"this_.grup_kuesioner_umum in (select grup_kuesioner_umum from grup_kuosioner_umum_detail where tbmuser='"
						+ tbmuserDinilai.getUserId() + "' group by grup_kuesioner_umum ) ");

		Session session = HibernateUtil.currentSession();
		List<JadwalChecklistPenilaianUmum> grupChecklistPenilaianUmums = session
				.createCriteria(JadwalChecklistPenilaianUmum.class).add(Restrictions.eq("tahunAkademik", tahunAkademik))
				.add(Restrictions.eq("semester", semester)).createCriteria("grupChecklistPenilaianUmum").add(criterion)
				.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).list();

		Long idAngket = null;

		for (JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum : grupChecklistPenilaianUmums) {

			GrupChecklistPenilaianUmum g = jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum();
			List<ChecklistPenilaianUmum> checklistPenilaianUmums =

					ConstantValues.simpleList(session.createCriteria(ChecklistPenilaianUmum.class)

							.createAlias("subGrupChecklistPenilaianUmum", "subGrupChecklistPenilaianUmum",
									Criteria.LEFT_JOIN)

							.addOrder(Order.asc("subGrupChecklistPenilaianUmum.nama")).addOrder(Order.asc("isi"))

							.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
							.add(Restrictions.eq("grupChecklistPenilaianUmum", g)), ChecklistPenilaianUmum.class);

			Integer jumlahChecklist = Integer
					.parseInt(Common.getKonfigurasi("jumlah_pilihan_checklist_penilaian_umum", "5").getNilai().trim());
			try {
				jumlahChecklist = g.getAngketPenilaianUmum().getJumlahPilihan();
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

			try {
				if (!checklistPenilaianUmums.isEmpty()
						&& (idAngket == null || !idAngket.equals(g.getAngketPenilaianUmum().getId()))) {

					row = new MyFormRow();
					ais.ui.util.ZkCompat.setSpans(row, "2");
					row.setStyle("background: transparent;");
					row.setParent(rows);

					MyGroupboxStyled groupbox = new MyGroupboxStyled();
					groupbox.setParent(row);
					groupbox.appendChild(new MyCaptionStyled(g.getAngketPenilaianUmum().getIsi()));

					Vbox vboxText = new Vbox();
					vboxText.setParent(groupbox);
					String content = g.getAngketPenilaianUmum().getPetunjuk();

					content = content.replaceAll("\n", "<br>");

					Html html = new ais.ui.util.MyHtml(content);
					html.setStyle("font-family: sans-serif;font-size: 11px;");
					html.setParent(vboxText);

					idAngket = g.getAngketPenilaianUmum().getId();
				}
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			row = new MyFormRow();
			row.setVisible(!checklistPenilaianUmums.isEmpty());
			ais.ui.util.ZkCompat.setSpans(row, "2");
			row.setParent(rows);

			Groupbox groupbox = new Groupbox();
			groupbox.setParent(row);
			groupbox.setStyle("border:1px solid #e5e7eb; border-radius:10px; background:#ffffff; margin:8px 0; padding:0;");
			groupbox.appendChild(new Caption(g.getIsi()));

			MyGrid gridChecklist = new MyGrid();
			gridChecklist.setParent(groupbox);
			gridChecklist.setWidth("100%");
			gridChecklist.setStyle("border:0; background:#ffffff; table-layout:fixed;");
			Rows rowsChecklist = new Rows();
			Columns columnsChecklist = new Columns();
			columnsChecklist.setParent(gridChecklist);
			MyColumnConfig columnChecklist = new MyColumnConfig("Pertanyaan dan Penilaian");
			columnChecklist.setParent(columnsChecklist);
			Row rowChecklist;

			rowsChecklist.setParent(gridChecklist);

			Collection<ChecklistHasilPenilaianUmum> checklistHasilPenilaianUmums = new ArrayList<ChecklistHasilPenilaianUmum>();
			if (mahasiswa != null) {
				checklistHasilPenilaianUmums = mahasiswa.ambilChecklistHasilPenilaianUmum(session, pertemuanId,
						tbmuserDinilai.getUserId(), refresh);
			} else if (dosen != null) {
				checklistHasilPenilaianUmums = dosen.ambilChecklistHasilPenilaianUmum(session, pertemuanId,
						tbmuserDinilai.getUserId(), refresh);
			} else if (tbmuser != null) {
				checklistHasilPenilaianUmums = tbmuser.ambilChecklistHasilPenilaianUmum(session, pertemuanId,
						tbmuserDinilai.getUserId(), refresh);
			}

			Map<Long, ChecklistHasilPenilaianUmum> maps = new HashMap<Long, ChecklistHasilPenilaianUmum>();
			for (ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum : checklistHasilPenilaianUmums) {
				if (checklistHasilPenilaianUmum.getTahunAkademik().equals(tahunAkademik)

						&& (pertemuanId == null || (checklistHasilPenilaianUmum.getPertemuanId() != null
								&& pertemuanId.equals(checklistHasilPenilaianUmum.getPertemuanId())))

						&& (tbmuserDinilai == null || (checklistHasilPenilaianUmum.getTbmuserDinilai() != null
								&& tbmuserDinilai.getUserId() != null
								&& tbmuserDinilai.getUserId()
										.equals(checklistHasilPenilaianUmum.getTbmuserDinilai().getUserId())))

						&& checklistHasilPenilaianUmum.getSemesterStr().equals(semester)) {
					maps.put(checklistHasilPenilaianUmum.getChecklistPenilaianUmum().getId(),
							checklistHasilPenilaianUmum);
				}
			}
			checklistHasilPenilaianUmums = null;

			System.out.println("maps -> " + maps);

			SubGrupChecklistPenilaianUmum subGrupChecklistPenilaianUmum = null;

			for (final ChecklistPenilaianUmum c : checklistPenilaianUmums) {

				if (c.getSubGrupChecklistPenilaianUmum() != null) {

					if (subGrupChecklistPenilaianUmum == null
							|| (subGrupChecklistPenilaianUmum != null && !subGrupChecklistPenilaianUmum.getId()
									.equals(c.getSubGrupChecklistPenilaianUmum().getId()))) {
						Group group = new Group(c.getSubGrupChecklistPenilaianUmum().getNama());
						group.setParent(rowsChecklist);

						subGrupChecklistPenilaianUmum = c.getSubGrupChecklistPenilaianUmum();
					}

				}

				rowChecklist = new MyFormRow();

				rowChecklist.setParent(rowsChecklist);

				Vbox vbox = new Vbox();
				applyQuestionCardStyle(rowChecklist, vbox);
				rowChecklist.appendChild(vbox);

				vbox.appendChild(createQuestionLabel(c.getIsi()));

				ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = maps.get(c.getId());

				Integer nilai = checklistHasilPenilaianUmum == null ? null : checklistHasilPenilaianUmum.getNilai();

				final Radiogroup radiogroup = new Radiogroup();
				final Textbox keterangan = new Textbox(
						checklistHasilPenilaianUmum == null ? "" : checklistHasilPenilaianUmum.getKeterangan());
				JSONObject pilihan = new JSONObject(c.getPilihan());
				for (Integer i = 1; i <= jumlahChecklist; i++) {

					MyRadioConfig radio = new MyRadioConfig(
							pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
					radio.setValue(i.toString());
					radio.setAttribute("value", i);
					radiogroup.appendChild(radio);

					if (nilai != null) {
						radio.setSelected(nilai.equals(i));
						if (nilai.equals(i)) {
							radiogroup.setSelectedItem(radio);
						}
					}

				}
				vbox.appendChild(radiogroup);
				if (g.getAngketPenilaianUmum() != null && g.getAngketPenilaianUmum().getTampilKeterangan()) {
					keterangan.setWidth("90%");
					keterangan.setRows(2);
					rowChecklist.appendChild(keterangan);
				}

				EventListener listener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSave(tahunAkademik, semester, tbmuserDinilai, c, radiogroup.getSelectedItem(),
								keterangan.getValue());

					}
				};
				keterangan.addEventListener("onChange", listener);
				radiogroup.addEventListener("onCheck", listener);
			}

		}
	}

	@SuppressWarnings({ "unchecked" })
	private Div initData(final String tahunAkademik, final String semester, final Tbmuser tbmuserDinilai,
			final String diperuntukkan, final Window addWindow, final EventListener eventListener,
			final boolean tampilSimpan, boolean refresh) throws Exception {

		Div groupbox = new Div();

		final Row groupboxRow = Common.tampilanScroll1(groupbox);

		dataChecklist(tahunAkademik, semester, tbmuserDinilai, groupboxRow, diperuntukkan, eventListener, tampilSimpan,
				refresh);

		if (tampilSimpan) {

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(groupbox);

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Simpan dan Tutup", "/img/save.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					Object[] data = ChecklistPenilaianHelper.getJumlahStatusChecklistUmumGrup(tahunAkademik, semester, tbmuserDinilai,
							true);

					// PENTING: index array HARUS sesuai buildReturnStatusChecklistUmumGrup(...) di
					// ChecklistPenilaianHelper: index 7 = checklistPenilaianUmumTerjadwal (Set<Long>,
					// total pertanyaan aktif), index 6 = checklistParameterTerjadwalDipilihUserTsb
					// (Set<Long>, jumlah yang sudah diisi tbmuser ybs). Sebelumnya kode ini memakai
					// data[2]/data[3] yang sebenarnya bertipe Set<String> (bukan Set<Long>) dan tidak
					// merepresentasikan jumlah yang benar -- itulah sebab pesan "X dari Y" bisa tampil
					// tidak masuk akal (X > Y).
					Set<Long> checklistPenilaianUmumTerjadwal = (Set<Long>) data[7];
					Set<Long> checklistPenilaianUmumTerjadwalDipilih = (Set<Long>) data[6];
					int jumlahChecklist = checklistPenilaianUmumTerjadwal.size();
					int jumlahSaved = checklistPenilaianUmumTerjadwalDipilih.size();

					System.out.println("jumlahChecklist -> " + checklistPenilaianUmumTerjadwal);
					System.out.println("jumlahSaved -> " + checklistPenilaianUmumTerjadwalDipilih);

					eventListener.onEvent(event);

					MyMessageboxConfig.showFormat(
							"Terima kasih atas kesediaan Bapak/Ibu memberikan penilaian. Penilaian Anda telah berhasil kami simpan sebanyak {V1} dari total {V2} pertanyaan yang tersedia. Mohon pastikan seluruh pertanyaan telah terisi agar penilaian dapat kami proses secara lengkap.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, jumlahSaved, jumlahChecklist);

					if (addWindow != null)
						addWindow.setVisible(false);
				}
			});
			cancel.setParent(toolbar);

			cancel = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
			cancel.setTooltiptext("Refresh");
			cancel.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					dataChecklist(tahunAkademik, semester, tbmuserDinilai, groupboxRow, diperuntukkan, eventListener,
							tampilSimpan, true);
				}
			});
			cancel.setParent(toolbar);
		}

		return groupbox;
	}

	private boolean onSave(String tahunAkademik, String semester, Tbmuser tbmuserDinilai,
			ChecklistPenilaianUmum checklistPenilaianUmum, Radio radio, String keterangan) throws Exception {

		if (mahasiswa != null) {
			mahasiswa.belum("ChecklistHasilPenilaianUmum_baru"
					+ (tbmuserDinilai == null ? "" : "_" + tbmuserDinilai.getUserId())
					+ (pertemuanId == null ? "" : "_" + pertemuanId));
		} else if (dosen != null) {
			dosen.belum("ChecklistHasilPenilaianUmum_baru"
					+ (tbmuserDinilai == null ? "" : "_" + tbmuserDinilai.getUserId())
					+ (pertemuanId == null ? "" : "_" + pertemuanId));
		} else if (tbmuser != null) {
			tbmuser.belum("ChecklistHasilPenilaianUmum_baru"
					+ (tbmuserDinilai == null ? "" : "_" + tbmuserDinilai.getUserId())
					+ (pertemuanId == null ? "" : "_" + pertemuanId));
		}
		Session session = HibernateUtil.currentNativeSession();

		try {
			ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) session
					.createCriteria(ChecklistHasilPenilaianUmum.class)

					.add(tbmuserDinilai == null ? Restrictions.isNull("tbmuserDinilai")
							: Restrictions.eq("tbmuserDinilai", tbmuserDinilai))

					.add(pertemuanId == null ? Restrictions.isNull("pertemuanId")
							: Restrictions.eq("pertemuanId", pertemuanId))

					.add(mahasiswa == null || mahasiswa.getId() == null ? Restrictions.isNull("mahasiswa")
							: Restrictions.eq("mahasiswa", mahasiswa))
					.add(dosen == null || dosen.getId() == null ? Restrictions.isNull("dosen")
							: Restrictions.eq("dosen", dosen))
					.add(tbmuser == null || tbmuser.getUserId() == null ? Restrictions.isNull("tbmuser")
							: Restrictions.eq("tbmuser", tbmuser))

					.add(Restrictions.eq("checklistPenilaianUmum", checklistPenilaianUmum))
					.add(Restrictions.eq("semesterStr", semester)).add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.setMaxResults(1).uniqueResult();

			if (checklistHasilPenilaianUmum == null) {
				checklistHasilPenilaianUmum = new ChecklistHasilPenilaianUmum();
			}
			checklistHasilPenilaianUmum.setTbmuserDinilai(tbmuserDinilai);
			checklistHasilPenilaianUmum.setPertemuanId(pertemuanId);
			checklistHasilPenilaianUmum.setMahasiswa(mahasiswa);
			checklistHasilPenilaianUmum.setDosen(dosen);
			if (tbmuser != null && tbmuser.getUserId() != null && !tbmuser.getUserId().trim().isEmpty()
					&& tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null
					&& tbmuser.ambilGuru() == null) {
				checklistHasilPenilaianUmum.setTbmuser(tbmuser);
			} else {
				checklistHasilPenilaianUmum.setTbmuser(null);
			}
			checklistHasilPenilaianUmum.setChecklistPenilaianUmum(checklistPenilaianUmum);
			checklistHasilPenilaianUmum
					.setNilai(radio == null || radio.getValue() == null ? 0 : Integer.parseInt(radio.getValue().toString()));
			checklistHasilPenilaianUmum.setSemesterStr(semester);
			checklistHasilPenilaianUmum.setTahunAkademik(tahunAkademik);
			checklistHasilPenilaianUmum.setKeterangan(keterangan);

			session.getTransaction().begin();
			Common.refreshSaveOrUpdate(session, checklistHasilPenilaianUmum);
			session.getTransaction().commit();

			System.out.println("Simpan Penilaian " + checklistPenilaianUmum.getIsi() + ", tbmuserDinilai "
					+ tbmuserDinilai + ", nilai = " + checklistHasilPenilaianUmum.getNilai() + " dosen " + dosen
					+ " tbmuser " + tbmuser);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		HibernateUtil.closeSession();
		return true;
	}
}
