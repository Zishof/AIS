package ais.action.master;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Center;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Group;
import org.zkoss.zul.Groupbox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Html;
import org.zkoss.zul.Image;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Radio;
import org.zkoss.zul.Radiogroup;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;
import org.zkoss.zul.Window;

import ais.action.master.helper.IsiAngketParameterUmumListener;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.ChecklistPenilaianHelper;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.ChecklistHasilPenilaianUmum;
import ais.database.model.ChecklistPenilaianUmum;
import ais.database.model.Dosen;
import ais.database.model.GrupChecklistPenilaianUmum;
import ais.database.model.IsiAngketParameterUmum;
import ais.database.model.JadwalChecklistPenilaianUmum;
import ais.database.model.Mahasiswa;
import ais.database.model.MahasiswaJadiAsisten;
import ais.database.model.OrangTua;
import ais.database.model.StatusMahasiswa;
import ais.database.model.SubGrupChecklistPenilaianUmum;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyGroupboxStyled;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRadioConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class ChecklistPenilaianUmumOlehPesertaAction extends GenericAutowireComposer {

	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;
	private Mahasiswa mahasiswa;

	private Borderlayout pustakaLayout;
	private South southTutup;
	private Dosen dosen = null;
	private Guru guru = null;
	private Siswa siswa = null;
	private OrangTua orangTua = null;
	private Tbmuser tbmuser = Common.getCurrentUser();
	private Map<IsiAngketParameterUmum, Object[]> data = new HashMap<IsiAngketParameterUmum, Object[]>();
	private Long pertemuanId = null;

	private void initHeader() {

		Sekolah sekolah = SekolahUtil.getSekolah();
		Yayasan yayasan = SekolahUtil.getYayasan();
		String image = SekolahUtil.getSekolahMedia("logo_sekolah_");
		if (image == null) {
			image = SekolahUtil.getYayasanMedia("logo_yayasan_");
		}
		if (image == null) {
			image = ais.action.master.helper.util.PerguruanTinggiUtil.getPerguruanTinggiMedia("logo_perguruanTinggi_");
		}

		String label_instansi_sekolah = sekolah != null && sekolah.getId() != null ? sekolah.getNama()
				: yayasan != null && yayasan.getId() != null ? yayasan.getNama() : null;

		if (Common.isMobile()) {
			North north = new North();
			north.setBorder("none");
			north.setSclass("headerHbox");
			pustakaLayout.appendChild(north);
			north.setHeight("230px");

			Grid gridHeader = new Grid();
			gridHeader.setSclass("dgrid fgrid");
			gridHeader.setHeight("100%");
			gridHeader.setStyle("border:0px;background: transparent;");
			gridHeader.setParent(north);

			Columns columns = new Columns();
			columns.setParent(gridHeader);

			Column column = new Column();
			column.setWidth("100%");
			column.setAlign("center");
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(gridHeader);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Image imgLogo = new Image(image == null ? "img/logo.png" : image);
			row.appendChild(imgLogo);
			imgLogo.setHeight("58px");

			row = new MyFormRow();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Label namaSeleksi = new Label(label_instansi_sekolah == null
					? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
					: label_instansi_sekolah);
			row.appendChild(namaSeleksi);

			row = new MyFormRow();
			row.setStyle("border:0px;background: transparent;");
			row.setParent(rows);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_kuesioner_header", "Sistem Informasi Kuesioner").getNilai());
			row.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1pmb");
			namaSekolah.setSclass("mottopmb");

		} else {

			North north = new North();
			north.setBorder("none");
			north.setSclass("headerHbox");
			pustakaLayout.appendChild(north);

			Hbox hbox = new Hbox();
			hbox.appendChild(new Space());
			hbox.appendChild(new Space());

			Image imgLogo = new Image(image == null ? "img/logo.png" : image);
			hbox.appendChild(imgLogo);
			hbox.appendChild(new Space());
			hbox.setWidth("100%");
			hbox.setHeight("90px");
			north.appendChild(hbox);

			Vbox vbox = new Vbox();
			vbox.setWidth("100%");
			vbox.setPack("center");
			hbox.appendChild(vbox);

			imgLogo.setHeight("50px");

			Label namaSeleksi = new Label(label_instansi_sekolah == null
					? Common.getKonfigurasi("label_universitas", "Nama Instansi Kampus").getNilai()
					: label_instansi_sekolah);
			vbox.appendChild(namaSeleksi);

			Label namaSekolah = new Label(
					Common.getKonfigurasi("label_kuesioner_header", "Sistem Informasi Kuesioner").getNilai());
			vbox.appendChild(namaSekolah);

			namaSeleksi.setSclass("title1");
			namaSekolah.setSclass("motto");
		}
	}

	public void onIsiAngketDosenSelesai(Event event) {
		execution.sendRedirect(Common.getRequestHostWithProtocol() + "/main");
	}

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		Common.initLaguage();

		applyPageStyle();
		initHeader();

		Session initSession = null;
		try {
			initSession = HibernateUtil.getSessionFactory().openSession();

			if (execution.getParameter("mahasiswa") != null) {
				mahasiswa = (Mahasiswa) initSession.createCriteria(Mahasiswa.class)
						.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
						.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa")))).uniqueResult();
				tbmuser = new Tbmuser(mahasiswa);
			} else if (execution.getParameter("siswa") != null) {
				siswa = (Siswa) initSession.createCriteria(Siswa.class)
						.add(Restrictions.isNotNull("namaSiswa"))
						.add(Restrictions.ne("namaSiswa", ""))
						.add(Restrictions.isNotNull("sekolah"))
						.add(Restrictions.idEq(Long.parseLong(execution.getParameter("siswa")))).uniqueResult();
				tbmuser = new Tbmuser(siswa);
			} else if (execution.getParameter("dosen") != null) {
				dosen = (Dosen) initSession.createCriteria(Dosen.class)
						.add(Restrictions.idEq(Long.parseLong(execution.getParameter("dosen")))).uniqueResult();
				tbmuser = new Tbmuser(dosen);
			} else if (execution.getParameter("guru") != null) {
				guru = (Guru) initSession.createCriteria(Guru.class)
						.add(Restrictions.isNotNull("sekolah"))
						.add(Restrictions.idEq(Long.parseLong(execution.getParameter("guru")))).uniqueResult();
				tbmuser = new Tbmuser(guru);
			} else {
				tbmuser = Common.getCurrentUser();
				mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
				dosen = tbmuser == null ? null : tbmuser.ambilDosen();
				siswa = tbmuser == null ? null : tbmuser.getSiswa();
				guru = tbmuser == null ? null : tbmuser.ambilGuru();
				orangTua = tbmuser == null ? null : tbmuser.getOrangTua();
			}

		} finally {
			if (initSession != null && initSession.isOpen()) {
				try { initSession.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}

		if (southTutup != null) {
			/*
			 * Tombol selesai sudah dipindahkan ke area header/card atas pada ZUL.
			 * South lama dipertahankan hanya untuk kompatibilitas autowire,
			 * tetapi disembunyikan agar tidak menambah tinggi dan tidak memicu scrollbar ganda.
			 */
			southTutup.setVisible(false);
			southTutup.setHeight("0px");
			southTutup.setStyle("border:0; padding:0; margin:0; overflow:hidden; background:transparent;");
		}

		if (tbmuser == null || tbmuser.getUserId() == null) {

			final MyWindow inputWindow = new MyWindow();

			ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(inputWindow);
			inputWindow.setHeight("200px");
			inputWindow.setWidth("400px");

			Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
			borderlayout.setParent(inputWindow);
			borderlayout.setWidth("100%");
			borderlayout.setHeight("100%");

			Center center = new Center();
			((Window) inputWindow).setTitle("Untuk mengisi kuesioner, masukkan email");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			MyGrid gridInput = new MyGrid();
			gridInput.setParent(center);
			gridInput.setWidth("100%");
			gridInput.setHeight("100%");

			Columns columns = new Columns();
			columns.setParent(gridInput);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("40%");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(gridInput);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Email *"));
			final Textbox email = new Textbox();
			row.appendChild(email);
			email.setWidth("90%");

			South south = new South();
			south.setParent(borderlayout);

			Toolbar toolbar = new Toolbar();
			toolbar.setHeight("40px");
			toolbar.setParent(south);

			MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Mulai Isi Kuesioner", "/img/save.gif");
			save.setTooltiptext("Simpan");
			save.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					if (email.getValue().trim().equals("")) {
						MyMessageboxConfig.show(
								"Mohon maaf, alamat email Bapak/Ibu belum diisi. Alamat email diperlukan untuk memulai pengisian kuesioner. Langkah yang dapat dilakukan: (1) ketikkan alamat email Anda yang aktif pada kolom yang tersedia; (2) pastikan format penulisan email sudah benar; (3) tekan kembali tombol Mulai Isi Kuesioner.",
								"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
						return;
					}

					inputWindow.detach();

					Session session = null;
					Transaction tx = null;
					try {
						session = HibernateUtil.getSessionFactory().openSession();
						tx = session.beginTransaction();

						tbmuser = (Tbmuser) session.createCriteria(Tbmuser.class)
								.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
								.add(Restrictions.eq("userId", email.getValue().trim())).uniqueResult();
								
						if (tbmuser == null || tbmuser.getUserId() == null) {
							tbmuser = new Tbmuser();
							tbmuser.setUserId(email.getValue().trim());
							tbmuser.setEmail(email.getValue().trim());
							tbmuser.setIs_encripted(true);
							tbmuser.setRoot(false);
							tbmuser.setUserNama(email.getValue().trim());
							String passw = RandomStringUtils.randomNumeric(5);
							tbmuser.setUserPassword(Common.desEncrypter.get().encrypt(passw.trim()));
							tbmuser.setUserRole(ConstantValues.tbmroleUmum);
							tbmuser.setUserShow(1);

							session.save(tbmuser);
						}
						tx.commit();
					} catch (Exception e) {
						if (tx != null) tx.rollback();
						ais.common.Common.tampilErrorJikaAdmin(e);
					} finally {
						if (session != null && session.isOpen()) {
							try { session.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/ChecklistPenilaianUmumOlehPesertaAction.java:378");}
						}
					}

					onSearchDefault(null);
				}
			});
			save.setParent(toolbar);

			inputWindow.setVisible(true);
			inputWindow.onModal();

		} else {
			onSearchDefault(null);
		}
		
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
			arg0.setSclass("angket-row-card");
			arg0.setStyle("background:#ffffff; border-bottom:1px solid #e5e7eb;");
			final Object[] rowData = (Object[]) arg1;
			
			final String tahunAkademik = rowData[0] == null ? "" : rowData[0].toString();
			final String semester = rowData[1] == null ? "" : rowData[1].toString();

			new Label(tahunAkademik).setParent(arg0);
			new Label(semester).setParent(arg0);

			final Label labelSudahTerisi = new Label();
			labelSudahTerisi.setParent(arg0);
			labelSudahTerisi.setWidth("90%");
			labelSudahTerisi.setHeight("95%");

			final EventListener grupChecklistPenilaianUmum = new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event ar) throws Exception {

					Object[] dataObj = ChecklistPenilaianHelper.getJumlahStatusChecklistUmum(tahunAkademik, semester, tbmuser, true);

					Set<Long> checklistPenilaianUmumTerjadwal = (Set<Long>) dataObj[2];
					Set<Long> checklistPenilaianUmumTerjadwalDipilih = (Set<Long>) dataObj[3];

					int jumlahChecklist = checklistPenilaianUmumTerjadwal.size();
					int jumlahSaved = checklistPenilaianUmumTerjadwalDipilih.size();

					arg0.setVisible(jumlahChecklist > 0);

					labelSudahTerisi.setValue((jumlahChecklist == jumlahSaved ? "Telah diisi" : "Belum terisi") + " - "
							+ (jumlahSaved + " dari " + jumlahChecklist + " telah terisi"));
					if (jumlahChecklist > 0 && jumlahChecklist == jumlahSaved) {
						arg0.setStyle("border:0px;background:#ecfdf5;");
					}
				}
			};

			Hbox toolbar = new Hbox();
			toolbar.setParent(arg0);
			MyButtonConfig button = new MyButtonConfig("Lakukan Penilaian", "/img/Check-icon.png");
			button.setOrient("vertical");
			/* FIX 20-08-2026: setWidth("100%") memaksa tombol selebar induknya, sehingga label ikut terpotong di sel/kolom sempit. Lebar dilepas agar tombol menyesuaikan isi, dan white-space:nowrap menjaga teks tetap satu baris. */
			button.setStyle("white-space:nowrap; border-radius:8px; font-weight:bold; padding:6px 10px;");
			button.setParent(toolbar);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event arg0) throws Exception {
					String diperuntukkan = null;
					init(tahunAkademik, semester, diperuntukkan, grupChecklistPenilaianUmum);
					addWindow.setHeight("95%");
					addWindow.setWidth("95%");
					addWindow.setVisible(true);
					addWindow.onModal();
				}
			});

			grupChecklistPenilaianUmum.onEvent(null);
		}
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			List<Object[]> jadwalChecklistPenilaianUmums = session.createSQLQuery(
					"select tahunakademik, semester from jadwal_checklist_penilaian_umum "
					+ "where sampai >= date('" + Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + "') "
					+ "and mulai <= date('" + Common.databaseDateFormat.get().format(ais.ui.util.WaktuUtil.getDate()) + "') " 
					+ "group by tahunakademik,semester order by tahunakademik desc,semester desc ")
					.list();

			ListModel strset = new SimpleListModel(jadwalChecklistPenilaianUmums);
			grid.setRowRenderer(new DataRenderer());
			grid.setModelCheckMobile(strset);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
	}

	private void init(String tahunAkademik, String semester, String diperuntukkan, final EventListener eventListener) {
		addWindow.setTitle("Angket Penilaian");
		Common.clear(addWindow);

		initData(tahunAkademik, semester, diperuntukkan, addWindow, new EventListener() {
			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(null);
			}
		}, true, false).setParent(createSingleScrollContainer(addWindow));
	}


	public Div initData(Mahasiswa mahasiswa, Dosen dosen, Tbmuser tbmuser, String tahunAkademik, String semester,
			String diperuntukkan, Window addWindow, EventListener eventListener, boolean tampilSimpan, Long pertemuanId,
			boolean refresh) {
		this.mahasiswa = mahasiswa;
		this.dosen = dosen;
		this.tbmuser = tbmuser;
		this.pertemuanId = pertemuanId;
		return initData(tahunAkademik, semester, diperuntukkan, addWindow, eventListener, tampilSimpan, refresh);
	}

	private void applyPageStyle() {
		if (pustakaLayout != null) {
			pustakaLayout.setWidth("100%");
			pustakaLayout.setHeight("100%");
			pustakaLayout.setStyle("border:0; background:#f5f7fb; overflow:hidden;");
		}
		if (grid != null) {
			grid.setStyle("border:0; background:transparent;");
		}
		if (addWindow != null) {
			addWindow.setStyle("border-radius:10px; overflow:hidden;");
			try {
				addWindow.setContentStyle("overflow:hidden; padding:0; margin:0; background:#f8fafc;");
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}
	}

	private Div createSingleScrollContainer(Window targetWindow) {
		Div container = new Div();
		container.setWidth("100%");
		container.setHeight("100%");
		container.setSclass("angket-form-scroll");
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
		textbox.setStyle("box-sizing:border-box; min-height:46px; border:1px solid #cbd5e1; "
				+ "border-radius:6px; padding:6px; margin-top:6px; background:#f8fafc;");
	}

	private void styleRadioGroup(Radiogroup radiogroup) {
		if (radiogroup != null) {
			radiogroup.setStyle("display:block; padding-top:6px; color:#334155;");
		}
	}

	private StatusMahasiswa getStatusMahasiswaAman(Mahasiswa mahasiswa) {
		try {
			return mahasiswa == null ? null
					: ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(mahasiswa).getStatusMahasiswa();
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
			return null;
		}
	}

	private Criterion buildDiperuntukkanCriterion(String diperuntukkan, StatusMahasiswa statusMahasiswa) {
		if (diperuntukkan != null && !diperuntukkan.trim().isEmpty()) {
			return Restrictions.sqlRestriction("diperuntukkan='" + escapeSql(diperuntukkan.trim()) + "'");
		}

		if (isUserUmum()) {
			return Restrictions.sqlRestriction("diperuntukkan='"
					+ escapeSql(GrupChecklistPenilaianUmum.UNTUK_LINK_UMUM) + "'");
		}

		if (mahasiswa != null && mahasiswa.getId() != null) {
			return Restrictions.sqlRestriction(buildMahasiswaCriterion(statusMahasiswa));
		}

		if (siswa != null && siswa.getId() != null) {
			return Restrictions.sqlRestriction(buildSiswaCriterion());
		}

		if (dosen != null && dosen.getId() != null) {
			return Restrictions.sqlRestriction("(diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_DOSEN)
					+ "' or diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM) + "')");
		}

		if (guru != null && guru.getId() != null) {
			return Restrictions.sqlRestriction("(diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_GURU)
					+ "' or diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM) + "')");
		}

		if (orangTua != null && orangTua.getId() != null) {
			return Restrictions.sqlRestriction("(diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_ORANG_TUA)
					+ "' or diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM) + "')");
		}

		if (tbmuser != null && tbmuser.getUserId() != null) {
			return Restrictions.sqlRestriction("(diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_ADMIN)
					+ "' or diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM) + "')");
		}

		return Restrictions.sqlRestriction("false");
	}

	private String buildMahasiswaCriterion(StatusMahasiswa statusMahasiswa) {
		boolean alumni = statusMahasiswa != null && statusMahasiswa.getNama() != null
				&& statusMahasiswa.getNama().toLowerCase().trim().contains("lulus");

		StringBuilder sql = new StringBuilder();
		if (alumni) {
			sql.append("(diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_ALUMNI)).append("' ");
		} else {
			sql.append("((diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_MAHASISWA)).append("' ");
		}

		appendLongFilter(sql, "status_mahasiswa", statusMahasiswa == null ? null : statusMahasiswa.getId());
		appendAngkatanFilter(sql, mahasiswa.getTahunangkatan());
		appendLongFilter(sql, "fakultas", getFakultasId());
		appendLongFilter(sql, "jurusan", getJurusanId());

		if (alumni) {
			sql.append(")");
		} else {
			sql.append(") or diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM)).append("')");
		}
		return sql.toString();
	}

	private String buildSiswaCriterion() {
		StringBuilder sql = new StringBuilder("((diperuntukkan='")
				.append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_SISWA)).append("' ");
		appendAngkatanFilter(sql, siswa.getTahunMasuk());
		appendLongFilter(sql, "yayasan", getYayasanId());
		appendLongFilter(sql, "sekolah", getSekolahId());
		sql.append(") or diperuntukkan='").append(escapeSql(GrupChecklistPenilaianUmum.UNTUK_UMUM)).append("')");
		return sql.toString();
	}

	private boolean isUserUmum() {
		return tbmuser != null && tbmuser.hakAkses() != null && tbmuser.hakAkses().getRoleId() != null
				&& ConstantValues.tbmroleUmum != null && ConstantValues.tbmroleUmum.getRoleId() != null
				&& ConstantValues.tbmroleUmum.getRoleId().equals(tbmuser.hakAkses().getRoleId());
	}

	private boolean isMahasiswaAsisten(String tahunAkademik, String semester) {
		try {
			Map<Long, MahasiswaJadiAsisten> map = ConstantValues.ambilBerdasarClass(MahasiswaJadiAsisten.class);
			if (map == null || map.isEmpty()) {
				return false;
			}
			for (MahasiswaJadiAsisten mahasiswaJadiAsisten : map.values()) {
				if (mahasiswaJadiAsisten == null || !Boolean.TRUE.equals(mahasiswaJadiAsisten.getAktif())
						|| mahasiswaJadiAsisten.getPerkuliahan() == null
						|| mahasiswaJadiAsisten.getMahasiswa() == null
						|| mahasiswaJadiAsisten.getMahasiswa().getId() == null
						|| !mahasiswaJadiAsisten.getMahasiswa().getId().equals(mahasiswa.getId())) {
					continue;
				}
				if (safeEquals(mahasiswaJadiAsisten.getPerkuliahan().getTahunAjaran(), tahunAkademik)
						&& safeEquals(mahasiswaJadiAsisten.getPerkuliahan().getGanjilGenap(), semester)) {
					return true;
				}
			}
		} catch (Exception e) {
			Common.tampilErrorJikaAdmin(e);
		}
		return false;
	}

	private static void appendLongFilter(StringBuilder sb, String column, Long value) {
		if (value == null) {
			sb.append(" and ").append(column).append(" is null ");
		} else {
			sb.append(" and (").append(column).append("=").append(value).append(" or ").append(column).append(" is null) ");
		}
	}

	private static void appendAngkatanFilter(StringBuilder sb, Integer tahunAngkatan) {
		if (tahunAngkatan == null) {
			sb.append(" and mulai_angkatan is null and sampai_angkatan is null ");
		} else {
			sb.append(" and (mulai_angkatan<=").append(tahunAngkatan).append(" or mulai_angkatan is null) ")
					.append(" and (sampai_angkatan>=").append(tahunAngkatan).append(" or sampai_angkatan is null) ");
		}
	}

	private Long getFakultasId() {
		try {
			return mahasiswa == null || mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null
					? null : mahasiswa.getJurusan().getFakultas().getId();
		} catch (Exception e) {
			return null;
		}
	}

	private Long getJurusanId() {
		try {
			return mahasiswa == null || mahasiswa.getJurusan() == null ? null : mahasiswa.getJurusan().getId();
		} catch (Exception e) {
			return null;
		}
	}

	private Long getYayasanId() {
		try {
			return siswa == null || siswa.getSekolah() == null || siswa.getSekolah().getYayasan() == null ? null
					: siswa.getSekolah().getYayasan().getId();
		} catch (Exception e) {
			return null;
		}
	}

	private Long getSekolahId() {
		try {
			return siswa == null || siswa.getSekolah() == null ? null : siswa.getSekolah().getId();
		} catch (Exception e) {
			return null;
		}
	}

	private static boolean isTampilKeterangan(GrupChecklistPenilaianUmum grup) {
		try {
			return grup != null && grup.getAngketPenilaianUmum() != null
					&& Boolean.TRUE.equals(grup.getAngketPenilaianUmum().getTampilKeterangan());
		} catch (Exception e) {
			return false;
		}
	}

	private static JSONObject buildPilihanJson(ChecklistPenilaianUmum checklist) {
		try {
			String pilihan = checklist == null ? null : checklist.getPilihan();
			return pilihan == null || pilihan.trim().isEmpty() ? new JSONObject() : new JSONObject(pilihan);
		} catch (Exception e) {
			return new JSONObject();
		}
	}

	private boolean isChecklistSesuaiJadwal(ChecklistHasilPenilaianUmum hasil, String tahunAkademik, String semester) {
		if (hasil == null || hasil.getChecklistPenilaianUmum() == null || hasil.getChecklistPenilaianUmum().getId() == null) {
			return false;
		}
		return safeEquals(hasil.getTahunAkademik(), tahunAkademik)
				&& safeEquals(hasil.getSemesterStr(), semester)
				&& (pertemuanId == null || (hasil.getPertemuanId() != null && pertemuanId.equals(hasil.getPertemuanId())));
	}

	private static boolean safeEquals(String a, String b) {
		return a == null ? b == null : a.equals(b);
	}

	private static String safeString(String value) {
		return value == null ? "" : value;
	}

	private static String escapeSql(String value) {
		return value == null ? "" : value.replace("'", "''");
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	private void dataChecklist(final String tahunAkademik, final String semester, Row groupboxRow, String diperuntukkan,
			final EventListener eventListener, boolean tampilSimpan, boolean refresh) throws Exception {
		
		Common.clear(groupboxRow);
		MyGrid gridData = new MyGrid();
		gridData.setWidth("100%");
		gridData.setParent(groupboxRow);
		gridData.setHeight("100%");
		gridData.setStyle("border:0; background:transparent;");
		
		Rows rows = new Rows();
		rows.setParent(gridData);
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

		StatusMahasiswa statusMahasiswa = getStatusMahasiswaAman(mahasiswa);
		Criterion criterion = buildDiperuntukkanCriterion(diperuntukkan, statusMahasiswa);

		if (diperuntukkan == null && mahasiswa != null && mahasiswa.getId() != null
				&& isMahasiswaAsisten(tahunAkademik, semester)) {
			criterion = Restrictions.or(criterion, Restrictions.sqlRestriction(
					"diperuntukkan='" + escapeSql(GrupChecklistPenilaianUmum.UNTUK_ASISTEN + "") + "'"));
		}

		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			
			List<JadwalChecklistPenilaianUmum> grupChecklistPenilaianUmums = session
					.createCriteria(JadwalChecklistPenilaianUmum.class).add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.add(Restrictions.eq("semester", semester)).createCriteria("grupChecklistPenilaianUmum").add(criterion)
					.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif"))).list();

			Long idAngket = null;

			for (JadwalChecklistPenilaianUmum jadwalChecklistPenilaianUmum : grupChecklistPenilaianUmums) {

				GrupChecklistPenilaianUmum g = jadwalChecklistPenilaianUmum.getGrupChecklistPenilaianUmum();
				if (g == null || g.getId() == null) {
					continue;
				}
				List<ChecklistPenilaianUmum> checklistPenilaianUmums = ConstantValues.simpleList(
								session.createCriteria(ChecklistPenilaianUmum.class)
										.createAlias("subGrupChecklistPenilaianUmum", "subGrupChecklistPenilaianUmum", Criteria.LEFT_JOIN)
										.addOrder(Order.asc("subGrupChecklistPenilaianUmum.nama"))
										.addOrder(Order.asc("isi"))
										.add(Restrictions.or(Restrictions.eq("aktif", true), Restrictions.isNull("aktif")))
										.add(Restrictions.eq("grupChecklistPenilaianUmum", g)),
								ChecklistPenilaianUmum.class);
				if (checklistPenilaianUmums == null) {
					checklistPenilaianUmums = new ArrayList<ChecklistPenilaianUmum>();
				}

				Integer jumlahChecklist = 5;
				try {
					jumlahChecklist = Integer.parseInt(Common.getKonfigurasi("jumlah_pilihan_checklist_penilaian_umum", "5").getNilai().trim());
				} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
				
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
						groupbox.setSclass("angket-section-card");
						groupbox.setStyle("border:1px solid #dbeafe; border-radius:10px; background:#ffffff; margin:8px 0; padding:8px;");
						groupbox.appendChild(new MyCaptionStyled(g.getAngketPenilaianUmum().getIsi()));

						Vbox vboxText = new Vbox();
						vboxText.setParent(groupbox);
						String content = safeString(g.getAngketPenilaianUmum().getPetunjuk());

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
				groupbox.setStyle("border:1px solid #e5e7eb; border-radius:10px; background:#ffffff; margin:8px 0;");
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
					checklistHasilPenilaianUmums = mahasiswa.ambilChecklistHasilPenilaianUmum(session, pertemuanId, null, refresh);
				} else if (siswa != null) {
					checklistHasilPenilaianUmums = siswa.ambilChecklistHasilPenilaianUmum(session, pertemuanId, null, refresh);
				} else if (dosen != null) {
					checklistHasilPenilaianUmums = dosen.ambilChecklistHasilPenilaianUmum(session, pertemuanId, null, refresh);
				} else if (guru != null) {
					checklistHasilPenilaianUmums = guru.ambilChecklistHasilPenilaianUmum(session, pertemuanId, null, refresh);
				} else if (tbmuser != null) {
					checklistHasilPenilaianUmums = tbmuser.ambilChecklistHasilPenilaianUmum(session, pertemuanId, null, refresh);
				}

				Map<Long, ChecklistHasilPenilaianUmum> maps = new HashMap<Long, ChecklistHasilPenilaianUmum>();
				for (ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum : checklistHasilPenilaianUmums) {
					if (isChecklistSesuaiJadwal(checklistHasilPenilaianUmum, tahunAkademik, semester)) {
						maps.put(checklistHasilPenilaianUmum.getChecklistPenilaianUmum().getId(), checklistHasilPenilaianUmum);
					}
				}
				checklistHasilPenilaianUmums = null;

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
					final Textbox keterangan = new Textbox(checklistHasilPenilaianUmum == null ? "" : checklistHasilPenilaianUmum.getKeterangan());

					JSONObject pilihan = buildPilihanJson(c);
					for (Integer i = 1; i <= jumlahChecklist; i++) {
						MyRadioConfig radio = new MyRadioConfig(pilihan.isNull(i + "") ? i + "" : pilihan.getString(i + ""));
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
					styleRadioGroup(radiogroup);
					vbox.appendChild(radiogroup);

					if (isTampilKeterangan(g)) {
						styleKeteranganTextbox(keterangan);
						vbox.appendChild(keterangan);
					}

					EventListener listener = new EventListener() {
						@Override
						public void onEvent(Event arg0) throws Exception {
							onSave(tahunAkademik, semester, c, radiogroup.getSelectedItem(), keterangan.getValue());
						}
					};
					keterangan.addEventListener("onChange", listener);
					radiogroup.addEventListener("onCheck", listener);
				}

				IsiAngketParameterUmum isiAngketParameterUmum = (IsiAngketParameterUmum) session
						.createCriteria(IsiAngketParameterUmum.class)
						.add(mahasiswa != null && mahasiswa.getId() != null ? Restrictions.eq("mahasiswa", mahasiswa)
								: dosen != null && dosen.getId() != null ? Restrictions.eq("dosen", dosen)
										: siswa != null && siswa.getId() != null ? Restrictions.eq("siswa", siswa)
												: guru != null && guru.getId() != null ? Restrictions.eq("guru", guru)
														: Restrictions.eq("tbmuser", tbmuser))
						.add(Restrictions.eq("jadwalChecklistPenilaianUmum", jadwalChecklistPenilaianUmum)).setMaxResults(1)
						.uniqueResult();
						
				if (isiAngketParameterUmum == null) {
					isiAngketParameterUmum = new IsiAngketParameterUmum();
					isiAngketParameterUmum.setDosen(dosen);
					isiAngketParameterUmum.setMahasiswa(mahasiswa);
					isiAngketParameterUmum.setSiswa(siswa);
					isiAngketParameterUmum.setGuru(guru);
					if (bolehSimpanKeTbmuser()) {
						isiAngketParameterUmum.setTbmuser(tbmuser);
					} else {
						isiAngketParameterUmum.setTbmuser(null);
					}
					isiAngketParameterUmum.setJadwalChecklistPenilaianUmum(jadwalChecklistPenilaianUmum);
					
					Transaction tx = null;
					try {
						tx = session.beginTransaction();
						session.save(isiAngketParameterUmum);
						tx.commit();
					} catch(Exception e) {
						if(tx != null) tx.rollback();
					}
				}
				
				ArrayList<Row> parameterRows = new ArrayList<Row>();
				HashMap<String, LampiranLain> lampiranLains = new HashMap<String, LampiranLain>();
				IsiAngketParameterUmumListener isiAngketParameterUmumListener = new IsiAngketParameterUmumListener(
						isiAngketParameterUmum, parameterRows, lampiranLains, rows);

				data.put(isiAngketParameterUmum, new Object[] { isiAngketParameterUmumListener, lampiranLains });

				try {
					isiAngketParameterUmumListener.onEvent(null);
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e);
				}
			}

		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
	}

	private boolean bolehSimpanKeTbmuser() {
		if (tbmuser == null || tbmuser.getUserId() == null || tbmuser.getUserId().trim().isEmpty()) {
			return false;
		}
		if (mahasiswa != null || siswa != null || dosen != null || guru != null) {
			return false;
		}
		try {
			if (tbmuser.getMahasiswa() != null || tbmuser.getSiswa() != null || tbmuser.ambilDosen() != null
					|| tbmuser.ambilGuru() != null) {
				return false;
			}
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		return true;
	}

	private Div initData(final String tahunAkademik, final String semester, final String diperuntukkan,
			final Window addWindow, final EventListener eventListener, final boolean tampilSimpan, boolean refresh) {

		data.clear();
		Div groupbox = new Div();
		groupbox.setWidth("100%");
		groupbox.setStyle("overflow:visible; box-sizing:border-box;");
		final Row groupboxRow = Common.tampilanScroll1(groupbox);

		try {
			dataChecklist(tahunAkademik, semester, groupboxRow, diperuntukkan, eventListener, tampilSimpan, refresh);
		} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

		if (tampilSimpan) {

			Toolbar toolbar = new Toolbar();
			toolbar.setParent(groupbox);
			toolbar.setStyle("border:0; background:#ffffff; padding:8px; margin-bottom:10px; "
					+ "box-shadow:0 3px 12px rgba(15,23,42,0.08); border-radius:8px; "
					+ "position:sticky; top:0; z-index:20;");

			MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Simpan dan Tutup", "/img/save.gif");
			cancel.setTooltiptext("Tutup");
			cancel.addEventListener("onClick", new EventListener() {
				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					if (!data.isEmpty()) {
						for (IsiAngketParameterUmum isiAngketParameterUmum : data.keySet()) {
							Object[] objects = data.get(isiAngketParameterUmum);
							IsiAngketParameterUmumListener isiAngketParameterUmumListener = (IsiAngketParameterUmumListener) objects[0];
							if (!isiAngketParameterUmumListener.validate()) {
								return;
							}
							isiAngketParameterUmumListener.onSave(isiAngketParameterUmum);

							HashMap<String, LampiranLain> lampiranLains = (HashMap<String, LampiranLain>) objects[1];
							if (!lampiranLains.isEmpty()) {
								Session localSession = null;
								Transaction tx = null;
								try {
									localSession = HibernateUtil.getSessionFactory().openSession();
									tx = localSession.beginTransaction();
									for (LampiranLain lampiranLain : lampiranLains.values()) {
										localSession.refresh(lampiranLain);
										lampiranLain.setRef(isiAngketParameterUmum.getId());
										localSession.update(lampiranLain);
									}
									tx.commit();
								} catch(Exception e) {
									if(tx != null) tx.rollback();
								} finally {
									if(localSession != null && localSession.isOpen()) {
										try { localSession.close(); } catch (Exception ex) { ais.common.ErrorAuditUtil.record(ex, "auto-audit(empty-catch) src/ais/action/master/ChecklistPenilaianUmumOlehPesertaAction.java:1153");}
									}
								}
							}
						}
					}

					Object[] paramData = ChecklistPenilaianHelper.getJumlahStatusChecklistUmum(tahunAkademik, semester, tbmuser, true);

					Set<Long> checklistPenilaianUmumTerjadwal = (Set<Long>) paramData[2];
					Set<Long> checklistPenilaianUmumTerjadwalDipilih = (Set<Long>) paramData[3];
					int jumlahChecklist = checklistPenilaianUmumTerjadwal.size();
					int jumlahSaved = checklistPenilaianUmumTerjadwalDipilih.size();

					eventListener.onEvent(event);

					MyMessageboxConfig.showFormat(
							"Terima kasih atas kesediaan Bapak/Ibu memberikan penilaian. Penilaian Anda telah berhasil kami simpan sebanyak {V1} dari total {V2} pertanyaan yang tersedia. Mohon pastikan seluruh pertanyaan telah terisi agar penilaian dapat kami proses secara lengkap.",
							"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, jumlahSaved, jumlahChecklist);

					data.clear();

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
					dataChecklist(tahunAkademik, semester, groupboxRow, diperuntukkan, eventListener, tampilSimpan, true);
				}
			});
			cancel.setParent(toolbar);

			try {
				if (groupbox.getFirstChild() != null && groupbox.getFirstChild() != toolbar) {
					groupbox.insertBefore(toolbar, groupbox.getFirstChild());
				}
			} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
		}

		return groupbox;
	}

	private boolean onSave(String tahunAkademik, String semester, ChecklistPenilaianUmum checklistPenilaianUmum,
			Radio radio, String keterangan) throws Exception {

		if (siswa != null) {
			siswa.belum("ChecklistHasilPenilaianUmum_baru" + (pertemuanId == null ? "" : "_" + pertemuanId));
		} else if (mahasiswa != null) {
			mahasiswa.belum("ChecklistHasilPenilaianUmum_baru" + (pertemuanId == null ? "" : "_" + pertemuanId));
		} else if (dosen != null) {
			dosen.belum("ChecklistHasilPenilaianUmum_baru" + (pertemuanId == null ? "" : "_" + pertemuanId));
		} else if (guru != null) {
			guru.belum("ChecklistHasilPenilaianUmum_baru" + (pertemuanId == null ? "" : "_" + pertemuanId));
		} else if (tbmuser != null) {
			tbmuser.belum("ChecklistHasilPenilaianUmum_baru" + (pertemuanId == null ? "" : "_" + pertemuanId));
		}
		
		Session session = null;
		Transaction tx = null;

		try {
			session = HibernateUtil.getSessionFactory().openSession();
			tx = session.beginTransaction();
			
			ChecklistHasilPenilaianUmum checklistHasilPenilaianUmum = (ChecklistHasilPenilaianUmum) session
					.createCriteria(ChecklistHasilPenilaianUmum.class)
					.add(Restrictions.isNull("tbmuserDinilai"))
					.add(pertemuanId == null ? Restrictions.isNull("pertemuanId") : Restrictions.eq("pertemuanId", pertemuanId))
					.add(mahasiswa == null || mahasiswa.getId() == null ? Restrictions.isNull("mahasiswa") : Restrictions.eq("mahasiswa", mahasiswa))
					.add(siswa == null || siswa.getId() == null ? Restrictions.isNull("siswa") : Restrictions.eq("siswa", siswa))
					.add(dosen == null || dosen.getId() == null ? Restrictions.isNull("dosen") : Restrictions.eq("dosen", dosen))
					.add(guru == null || guru.getId() == null ? Restrictions.isNull("guru") : Restrictions.eq("guru", guru))
					.add(siswa != null || mahasiswa != null || tbmuser == null || tbmuser.getUserId() == null
							? Restrictions.isNull("tbmuser") : Restrictions.eq("tbmuser", tbmuser))
					.add(Restrictions.eq("checklistPenilaianUmum", checklistPenilaianUmum))
					.add(Restrictions.eq("semesterStr", semester))
					.add(Restrictions.eq("tahunAkademik", tahunAkademik))
					.setMaxResults(1).uniqueResult();

			if (checklistHasilPenilaianUmum == null) {
				checklistHasilPenilaianUmum = new ChecklistHasilPenilaianUmum();
			}
			checklistHasilPenilaianUmum.setPertemuanId(pertemuanId);
			checklistHasilPenilaianUmum.setMahasiswa(mahasiswa);
			checklistHasilPenilaianUmum.setDosen(dosen);
			checklistHasilPenilaianUmum.setGuru(guru);
			checklistHasilPenilaianUmum.setSiswa(siswa);

			if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null && tbmuser.getUserId() != null
					&& !tbmuser.getUserId().trim().isEmpty()) {
				checklistHasilPenilaianUmum.setTbmuser(tbmuser);
			}
			
			checklistHasilPenilaianUmum.setChecklistPenilaianUmum(checklistPenilaianUmum);
			
			// PENCEGAHAN NumberFormatException jika radio.getValue() tidak valid/bukan angka
			int valRadio = 0;
			if (radio != null && radio.getValue() != null) {
				try {
					valRadio = Integer.parseInt(radio.getValue().toString());
				} catch (NumberFormatException ex) {
					valRadio = 0;
				}
			}
			checklistHasilPenilaianUmum.setNilai(valRadio);
			
			checklistHasilPenilaianUmum.setSemesterStr(semester);
			checklistHasilPenilaianUmum.setTahunAkademik(tahunAkademik);
			checklistHasilPenilaianUmum.setKeterangan(keterangan);

			session.saveOrUpdate(checklistHasilPenilaianUmum);
			tx.commit();

		} catch (Exception e) {
			if (tx != null) tx.rollback();
			ais.common.Common.tampilErrorJikaAdmin(e);
		} finally {
			if (session != null && session.isOpen()) {
				try { session.close(); } catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}
			}
		}
		
		return true;
	}
}