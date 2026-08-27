package ais.action.master;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.A;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.AmbilDataMahasiswaBanbox;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sop.TampilanAlurSopAction;
import ais.action.report.Report;
import ais.action.ws.util.PembayaranUtil;
import ais.common.Common;
import ais.common.PesanFormalHelper;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.common.UIClassHelper;
import ais.database.dao.DaoFactory;
import ais.database.dao.PendaftaranCutiMahasiswaDao;
import ais.database.hibernate.HibernateUtil;
import ais.database.hibernate.StreamingHibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.HistoryStatusMahasiswa;
import ais.database.model.JenisKegiatan;
import ais.database.model.Kegiatan;
import ais.database.model.Konfigurasi;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.PendaftaranCutiMahasiswa;
import ais.database.model.Perkuliahan;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sop.DataSop;
import ais.database.model.sop.DisposisiSop;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.FormSop;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ws.billpayment.h2h.bankmandiri.util.ConstantUtilBankMandiri;

public class PendaftaranCutiMahasiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, FormSop {

	public PembayaranUtil pembayaranUtil = PembayaranUtil.getInstance();
	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Textbox searchnama;
	private Textbox searchnim;
	private Combobox searchfakultas;
	private Combobox searchjurusan;
	protected Combobox searchTahunAjaran;

	protected Combobox jenisSemester;
	private MyCheckboxConfig persetujuan;
	private MyCheckboxConfig semesterPendek;

	private DisposisiSop disposisiSop;

	private AmbilDataMahasiswaBanbox mahasiswa;
	private MyDatebox tanggal;
	private Textbox keterangan;
	private Combobox tahunAkademik;
	private Combobox ganjilGenap;
	private Label lblSemester;

	private boolean edit = false;
	private boolean delete = false;

	private PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa;
	private MyToolbarbuttonConfig add;

	@Override
	public org.zkoss.zk.ui.metainfo.ComponentInfo doBeforeCompose(org.zkoss.zk.ui.Page page,
			org.zkoss.zk.ui.Component parent, org.zkoss.zk.ui.metainfo.ComponentInfo compInfo) {
		Common.doCheckSecurity();
		return super.doBeforeCompose(page, parent, compInfo);
	}

	private Tbmuser tbmuser = null;
	private Combobox tahap;
	private Mahasiswa mhs = null;
	protected LampiranLain lainMahasiswa;
	private boolean persetujuanD = false;
	private Textbox kode;

	public static String[] contents = new String[] { "id", "mahasiswa", "semester", "kode", "tahap", "tahunAkademik",
			"ganjilGenap", "semesterPendek", "keterangan", "persetujuan" };

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initLaguage();
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		tbmuser = Common.getCurrentUser();

		if (execution.getParameter("mahasiswa") != null) {
			mhs = (Mahasiswa) HibernateUtil.currentSession().createCriteria(Mahasiswa.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.add(Restrictions.idEq(Long.parseLong(execution.getParameter("mahasiswa")))).uniqueResult();
			searchnama.setValue(mhs.getNama());
			searchnim.setValue(mhs.getNim());
			searchnama.setDisabled(true);
			searchnim.setDisabled(true);
		}

		else if (tbmuser.getMahasiswa() != null) {
			searchnama.setValue(tbmuser.getMahasiswa().getNama());
			searchnim.setValue(tbmuser.getMahasiswa().getNim());
			searchnama.setDisabled(true);
			searchnim.setDisabled(true);
		}

		Common.generateTahunAjaranDanSemua(searchTahunAjaran);
		Common.selectComboItem(searchTahunAjaran, Common.getCurrentTahunAkademik());

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GANJIL); }
		jenisSemester.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		if (comboitem != null) { comboitem.setValue(Perkuliahan.GENAP); }
		jenisSemester.appendChild(comboitem);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, contents);
		Common.appendKeToolbar(cetakToolbarbutton, add, comp);

		MyToolbarbuttonConfig upload = Common.uploadData(this, PendaftaranCutiMahasiswa.class, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] data = (Object[]) arg0.getData();
				PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = (PendaftaranCutiMahasiswa) data[0];
				Session session = (Session) data[1];

				HistoryStatusMahasiswa historyStatusMahasiswa = updateStatus(pendaftaranCutiMahasiswa, session);
				session.getTransaction().begin();
				session.saveOrUpdate(historyStatusMahasiswa);
				session.getTransaction().commit();
				Integer tahap = pendaftaranCutiMahasiswa.getTahap();
				Integer semester = pendaftaranCutiMahasiswa.getSemester();

				System.out.println(pendaftaranCutiMahasiswa.getMahasiswa() + ", " + semester + ", " + tahap);

				historyStatusMahasiswa.write("tulis ulang dari " + this.getClass().getName());
			}
		}, contents);
		upload.setVisible(
				(add != null && add.isVisible()) && edit && delete && Common.getCurrentUser().getMahasiswa() == null && mhs == null);
		Common.appendKeToolbar(upload, add, comp);
	}

	class PendaftaranCutiMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = (PendaftaranCutiMahasiswa) arg1;

			final Mahasiswa mahasiswa = pendaftaranCutiMahasiswa.getMahasiswa();

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);
			try {
				CommonMedia.tampilkanGambarKecil(mahasiswa).setParent(hbox);
			} catch (Exception e) {
				new MyLabelKecil().setParent(arg0);
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

			Vbox a;
			(a = RevisiHelper.createNewRevisi(PendaftaranCutiMahasiswa.class, pendaftaranCutiMahasiswa,
					mahasiswa.getNim())).setParent(hbox);
			a.appendChild(new Label(mahasiswa.getNama()));
			a.appendChild(new Label(pendaftaranCutiMahasiswa.getKode()));

			hbox = new Hbox();
			hbox.setParent(a);
			LampiranLain.createDownloadUploadFileLain(hbox, pendaftaranCutiMahasiswa.getId(),
					PendaftaranCutiMahasiswa.class.getName(), "Permohonan", false, null, null, false, false, false,
					false);

			new Label(pendaftaranCutiMahasiswa.getSemester() + ""
					+ (pendaftaranCutiMahasiswa.getTahap() == null ? ""
							: " / Tahap " + pendaftaranCutiMahasiswa.getTahap())
					+ (pendaftaranCutiMahasiswa.getSemesterPendek() ? "(SP)" : "")).setParent(arg0);
			new Label(pendaftaranCutiMahasiswa.getTahunAkademik()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null ? "" : mahasiswa.getJurusan().getNama()).setParent(arg0);
			new Label(mahasiswa.getJurusan() == null || mahasiswa.getJurusan().getFakultas() == null ? ""
					: mahasiswa.getJurusan().getFakultas().getNama()).setParent(arg0);

			Vbox vbox2 = new Vbox();
			vbox2.setParent(arg0);
			vbox2.appendChild(new Label(pendaftaranCutiMahasiswa.getKeterangan()));

			if (pendaftaranCutiMahasiswa.getDisposisiSop() != null) {
				A aa;
				(aa = new A()).setParent(vbox2);
				aa.setStyle("font-size:9px;");
				UIClassHelper.applyReadMore(aa, "SOP " + pendaftaranCutiMahasiswa.getDisposisiSop().getKeterangan() + " ("
						+ pendaftaranCutiMahasiswa.getDisposisiSop().getSop().getNama() + ")");
				aa.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						TampilanAlurSopAction.prosess(pendaftaranCutiMahasiswa.getDisposisiSop().getId(), null, null,
								true, arg0.getTarget());
					}
				});
			}

			new Label(pendaftaranCutiMahasiswa.getTanggal() == null ? ""
					: Common.dateFormat1.get().format(pendaftaranCutiMahasiswa.getTanggal())).setParent(arg0);

			if (tbmuser != null && tbmuser.getMahasiswa() != null) {
				new Label(pendaftaranCutiMahasiswa.getPersetujuan() ? "Sudah" : "Belum").setParent(arg0);
			} else {

				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setuju");
				checkbox.setDisabled(!edit);
				checkbox.setChecked(pendaftaranCutiMahasiswa.getPersetujuan());
				checkbox.setParent(arg0);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						pendaftaranCutiMahasiswa.setPersetujuan(checkbox.isChecked());
						Common.refreshSaveOrUpdate(pendaftaranCutiMahasiswa);
					}
				});

			}
			// Kolom aksi rapi (pola MahasiswaAction): semua tombol dikumpulkan lalu dibungkus
			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Permohonan", "/img/print.png");
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Permohonan");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
					Common.insertProperty(PendaftaranCutiMahasiswa.class, pendaftaranCutiMahasiswa, parameters, "", 1,
							"mahasiswa");
					parameters.put("id", pendaftaranCutiMahasiswa.getId());

					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(pendaftaranCutiMahasiswa.getMahasiswa(),
							pendaftaranCutiMahasiswa.getSemester(), pendaftaranCutiMahasiswa.getTahap(),
							pendaftaranCutiMahasiswa.getSemesterPendek() ? Perkuliahan.SEMESTER_PENDEK : null);
					HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
					Common.insertProperty(HistoryStatusMahasiswa.class, historyStatusMahasiswa, parameters, "status");

					Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");

					int jumlahPernahCuti = ((Number) HibernateUtil.currentSession()
							.createCriteria(PendaftaranCutiMahasiswa.class)
							.add(Restrictions.eq("mahasiswa", pendaftaranCutiMahasiswa.getMahasiswa()))
							.add(Restrictions.eq("persetujuan", true))
							.add(Restrictions.le("semester", pendaftaranCutiMahasiswa.getSemester()))
							.add(Restrictions.ne("id", pendaftaranCutiMahasiswa.getId()))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					parameters.put("jumlah_pernah_cuti", jumlahPernahCuti + 1);

					Report.generatePDFReport(Report.PDF, parameters, "Keterangan_Cuti",
							ais.ui.util.WaktuUtil.getDate());
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Edit", "/img/svg/edit-box-line.svg");
			button.setOrient("vertical");
			if ((tbmuser.getMahasiswa() != null || mhs != null) && pendaftaranCutiMahasiswa.getPersetujuan()) {
				button.setDisabled(true);
			}
			button.setTooltiptext("Ubah Data");
			button.setVisible(edit);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pendaftaranCutiMahasiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Persetujuan", "/img/print.png");
			button.setVisible(pendaftaranCutiMahasiswa.getPersetujuan());
			button.setOrient("vertical");
			button.setTooltiptext("Cetak Persetujuan");
			button.addEventListener("onClick", new EventListener() {
				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void onEvent(Event event) throws Exception {
					Map parameters = ais.common.HashMapGenerator.getRandStringObject();
					Common.insertProperty(PendaftaranCutiMahasiswa.class, pendaftaranCutiMahasiswa, parameters, "", 1,
							"mahasiswa");
					parameters.put("id", pendaftaranCutiMahasiswa.getId());
					KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(pendaftaranCutiMahasiswa.getMahasiswa(),
							pendaftaranCutiMahasiswa.getSemester(), pendaftaranCutiMahasiswa.getTahap(),
							pendaftaranCutiMahasiswa.getSemesterPendek() ? Perkuliahan.SEMESTER_PENDEK : null);
					HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
					Common.insertProperty(HistoryStatusMahasiswa.class, historyStatusMahasiswa, parameters, "status");

					Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");
					int jumlahPernahCuti = ((Number) HibernateUtil.currentSession()
							.createCriteria(PendaftaranCutiMahasiswa.class)
							.add(Restrictions.eq("mahasiswa", pendaftaranCutiMahasiswa.getMahasiswa()))
							.add(Restrictions.eq("persetujuan", true))
							.add(Restrictions.le("semester", pendaftaranCutiMahasiswa.getSemester()))
							.add(Restrictions.ne("id", pendaftaranCutiMahasiswa.getId()))
							.setProjection(Projections.rowCount()).uniqueResult()).intValue();
					parameters.put("jumlah_pernah_cuti", jumlahPernahCuti + 1);
					Report.generatePDFReport(Report.PDF, parameters, "Persetujuan_Cuti",
							ais.ui.util.WaktuUtil.getDate());
				}

			});
			aksiButtons.add(button);

			button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);
			if (pendaftaranCutiMahasiswa.getPersetujuan()) {
				button.setDisabled(true);
			}
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus data ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Session session = HibernateUtil.currentSession();

											Common.refreshDelete(session, pendaftaranCutiMahasiswa);

											Integer tahap = pendaftaranCutiMahasiswa.getTahap();
											Integer semester = pendaftaranCutiMahasiswa.getSemester();

											Criterion criterionSemester = tahap == null || tahap.equals(0)
													? Restrictions.eq("semester", semester)
													: Restrictions.sqlRestriction("true");

											Criterion criterionTahapan = tahap == null || tahap.equals(0)
													? Restrictions.sqlRestriction("true")
													: Restrictions.eq("tahap", tahap);

											HistoryStatusMahasiswa historyStatusMahasiswa = (HistoryStatusMahasiswa) session
													.createCriteria(HistoryStatusMahasiswa.class)
													.add(pendaftaranCutiMahasiswa.getSemesterPendek()
															? Restrictions.eq("sp", Perkuliahan.SEMESTER_PENDEK)
															: Restrictions.isNull("sp"))
													.add(criterionSemester).add(criterionTahapan)
													.add(Restrictions.eq("mahasiswa", mahasiswa))
													.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
											if (historyStatusMahasiswa == null) {
												historyStatusMahasiswa = new HistoryStatusMahasiswa(
														pendaftaranCutiMahasiswa.getSemesterPendek()
																? Perkuliahan.SEMESTER_PENDEK
																: null);
											}
											historyStatusMahasiswa
													.setTahunAkademik(pendaftaranCutiMahasiswa.getTahunAkademik());
											historyStatusMahasiswa.setMahasiswa(mahasiswa);
											historyStatusMahasiswa.setSemester(pendaftaranCutiMahasiswa.getSemester());
											historyStatusMahasiswa.setStatusMahasiswa(ConstantValues.AKTIF);
											Common.refreshUpdate(session, historyStatusMahasiswa);
											historyStatusMahasiswa
													.write("tulis ulang dari " + this.getClass().getName());
											onSearchDefault(event);
										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(
													"Data ini tidak dapat dihapus .., karena berelasi dengan data lainnya, error-nya adalah sbagai berikut:"
															+ e.getMessage());
										}

									}

								}
							});

				}
			});
			aksiButtons.add(button);

			ais.ui.util.UIHelper.buatBarisAksi(arg0, 3, aksiButtons);

		}

	}

	public void onAdd(Event event) throws Exception {
		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && tbmuser.getMahasiswa() != null) {
			if (!checkMahasiswa(tbmuser.getMahasiswa())) {
				return;
			}
		}

		init(new PendaftaranCutiMahasiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private boolean checkMahasiswa(Mahasiswa mahasiswa) throws Exception {
		return checkMahasiswa(mahasiswa.currentSemester(), (String) searchTahunAjaran.getSelectedItem().getValue());
	}

	private boolean checkMahasiswa(Integer smt, String tahunAjaran) throws Exception {

		Konfigurasi konfigurasi = Common.getKonfigurasi("aktivasi_cuti", tahunAjaran,
				smt % 2 == 0 ? Perkuliahan.GENAP : Perkuliahan.GANJIL);
		System.out.println("aktivasi_cuti => " + mahasiswa + ", konfigurasi = " + konfigurasi.getNilai()
				+ ", tahun akademik " + tahunAjaran);
		if (konfigurasi.getNilai().equals(Konfigurasi.TIDAK_AKTIF)) {
			MyMessageboxConfig.show(
					"Permohonan cuti untuk semester " + smt + " tahun akademik " + tahunAjaran
							+ " tidak diaktifkan, harap segera menghubungi Admin",
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		} else {
			return true;
		}
	}

	@Override
	public MyGrid form(GeneralValueObject generalValueObject, DisposisiSop disposisiSop,
			final MyToolbarbuttonConfig save, EventListener setujui) throws Exception {
		tbmuser = Common.getCurrentUser();
		tahunAkademik = new Combobox();
		Common.generateTahunAjaranDanSemua(tahunAkademik);
		ganjilGenap = new Combobox();
		MyComboitemConfig comboitem = new MyComboitemConfig(Perkuliahan.GANJIL);
		comboitem.setValue(Perkuliahan.GANJIL);
		ganjilGenap.appendChild(comboitem);
		comboitem = new MyComboitemConfig(Perkuliahan.GENAP);
		comboitem.setValue(Perkuliahan.GENAP);
		ganjilGenap.appendChild(comboitem);

		this.disposisiSop = disposisiSop;
		this.pendaftaranCutiMahasiswa = (PendaftaranCutiMahasiswa) generalValueObject;

		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setHeight("100%");

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(Common.getBahasa("label_mahasiswa")));

		mahasiswa = new AmbilDataMahasiswaBanbox();
		if (persetujuanD) {
			row.appendChild(new Label(pendaftaranCutiMahasiswa.getMahasiswa() == null ? ""
					: pendaftaranCutiMahasiswa.getMahasiswa().getNama()));
		} else {
			row.appendChild(mahasiswa);
		}

		mahasiswa.setAttribute("mahasiswa", pendaftaranCutiMahasiswa.getMahasiswa());
		mahasiswa.setValue(pendaftaranCutiMahasiswa.getMahasiswa() == null ? ""
				: pendaftaranCutiMahasiswa.getMahasiswa().getNama());
		mahasiswa.setWidth("90%");

		if (tbmuser.getMahasiswa() != null) {
			mahasiswa.setValue(tbmuser.getMahasiswa().toString());
			mahasiswa.setAttribute("mahasiswa", tbmuser.getMahasiswa());
			mahasiswa.setDisabled(true);
		} else if (mhs != null) {
			mahasiswa.setValue(mhs.toString());
			mahasiswa.setAttribute("mahasiswa", mhs);
			mahasiswa.setDisabled(true);
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tahun Akademik"));
		if (persetujuanD) {
			row.appendChild(new Label(pendaftaranCutiMahasiswa.getTahunAkademik()));
		} else {
			row.appendChild(tahunAkademik);
		}
		Common.selectComboItem(tahunAkademik, pendaftaranCutiMahasiswa.getTahunAkademik());
		tahunAkademik.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Ganjil / Genap"));
		if (persetujuanD) {
			row.appendChild(new Label(pendaftaranCutiMahasiswa.getGanjilGenap()));
		} else {
			row.appendChild(ganjilGenap);
		}
		Common.selectComboItem(ganjilGenap, pendaftaranCutiMahasiswa.getGanjilGenap());
		ganjilGenap.setWidth("90%");
		ganjilGenap.setReadonly(true);

		final MyFormRow rowSemester = new MyFormRow();
		rowSemester.setStyle("border:0px;background: transparent;");
		rowSemester.setParent(rows);
		rowSemester.appendChild(new Label(ais.common.Common.getBahasaConfig("Semester")));
		rowSemester.appendChild(lblSemester = new Label(pendaftaranCutiMahasiswa.getSemester() == null ? ""
				: pendaftaranCutiMahasiswa.getSemester().toString()));

		class SemesterEventListener implements EventListener {

			@Override
			public void onEvent(Event event) throws Exception {
				// TODO Auto-generated method stub
				Common.clear(lblSemester);
				Integer semesterInt = 0;
				Integer tahun = Integer
						.parseInt(StringUtils.split((String) tahunAkademik.getSelectedItem().getValue(), "/")[0]);

				if (ganjilGenap.getSelectedItem().getValue().equals(Perkuliahan.GANJIL)) {

					if (mahasiswa.getAttribute("mahasiswa") != null) {
						Mahasiswa mahasiswaSelected = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
						if (tahun.equals(mahasiswaSelected.getTahunangkatan())) {
							semesterInt = 1;
						} else {
							semesterInt = Common.getSemester(mahasiswaSelected.getTahunangkatan(), Perkuliahan.GANJIL,
									mahasiswaSelected.getPindahKeKampusIniMasukSemester(), tahun,
									mahasiswaSelected.getSemesterMulai());
						}
					}

				} else if (ganjilGenap.getSelectedItem().getValue().equals(Perkuliahan.GENAP)) {

					if (mahasiswa.getAttribute("mahasiswa") != null) {
						Mahasiswa mahasiswaSelected = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
						semesterInt = Common.getSemester(mahasiswaSelected.getTahunangkatan(), Perkuliahan.GENAP,
								mahasiswaSelected.getPindahKeKampusIniMasukSemester(), tahun,
								mahasiswaSelected.getSemesterMulai());
					}
				}
				System.out.println("tahun : " + tahun);
				System.out.println("Mhass : " + semesterInt);

				Common.clear(lblSemester);
				lblSemester.setValue(semesterInt + "");

			}
		}

		SemesterEventListener semesterEventListener = new SemesterEventListener();

		ganjilGenap.addEventListener("onChange", semesterEventListener);
		tahunAkademik.addEventListener("onChange", semesterEventListener);
		mahasiswa.setEventListener(semesterEventListener);

		if (ConstantValues.aktifkanTahapan) {
			row = new MyFormRow();
			row.setParent(rows);
			row.appendChild(new ais.ui.util.MyLabelConfig("Tahap"));
			tahap = new Combobox();
			if (persetujuanD) {
				row.appendChild(new Label(pendaftaranCutiMahasiswa.getTahap() == null ? ""
						: pendaftaranCutiMahasiswa.getTahap().toString()));
			} else {
				row.appendChild(tahap);
			}

			int jumlahTahapan = 3;

			for (int i = 1; i <= (jumlahTahapan * 7); i++) {
				comboitem = new MyComboitemConfig("Tahap " + i);
				comboitem.setValue(i);
				tahap.appendChild(comboitem);
			}
			tahap.setReadonly(true);

			Common.selectComboItem(tahap, pendaftaranCutiMahasiswa.getTahap());
		}

		row = new MyFormRow();
		row.setVisible(Common.bolehKonfigurasi("terdapat_cuti_sp", Konfigurasi.TIDAK_AKTIF));
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig(""));
		semesterPendek = new MyCheckboxConfig("Semester Pendek");

		if (persetujuanD) {
			row.appendChild(new Label(pendaftaranCutiMahasiswa.getSemesterPendek() ? "Ya" : "Tidak"));
		} else {
			row.appendChild(semesterPendek);
		}

		semesterPendek.setChecked(pendaftaranCutiMahasiswa.getSemesterPendek());

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Kode / Nomor"));
		kode = new Textbox(pendaftaranCutiMahasiswa.getKode());

		if (persetujuanD) {
			row.appendChild(new Label(pendaftaranCutiMahasiswa.getKode()));
		} else {
			row.appendChild(kode);
		}
		kode.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan / Alasan"));
		keterangan = new Textbox(
				pendaftaranCutiMahasiswa.getKeterangan() == null ? "" : pendaftaranCutiMahasiswa.getKeterangan());

		if (persetujuanD) {
			row.appendChild(new Label(pendaftaranCutiMahasiswa.getKeterangan()));
		} else {
			row.appendChild(keterangan);
		}

		keterangan.setWidth("90%");
		keterangan.setRows(4);

		lainMahasiswa = null;
		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Lampiran Permohonan Cuti"));
		Hbox hbox = new Hbox();
		LampiranLain.createDownloadUploadFileLain(hbox, pendaftaranCutiMahasiswa.getId(),
				PendaftaranCutiMahasiswa.class.getName(), "Lampiran Permohonan Cuti", false, new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						lainMahasiswa = (LampiranLain) arg0.getData();
					}
				});
		hbox.setParent(row);

		Common.initKeterangan(rows,
				"Jika file lampiran permohonan cuti lebih dari satu file, zip dulu semua file tersebut");

		row = new MyFormRow();
		row.setVisible(disposisiSop == null || disposisiSop.getId() == null);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Status Persetujuan"));

		if (tbmuser.getMahasiswa() != null || mhs != null) {
			if (pendaftaranCutiMahasiswa.getId() == null) {
				pendaftaranCutiMahasiswa.setPersetujuan(false);
			}
			row.appendChild(
					new Label(pendaftaranCutiMahasiswa.getPersetujuan() ? "Sudah disetujui" : "Belum mensetujui"));
		} else {
			row.appendChild(persetujuan = new MyCheckboxConfig());
			persetujuan.setChecked(pendaftaranCutiMahasiswa.getPersetujuan());
		}

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal Permohonan"));
		tanggal = new MyDatebox(pendaftaranCutiMahasiswa.getTanggal());
		if (persetujuanD) {
			row.appendChild(new Label(pendaftaranCutiMahasiswa.getTanggal() == null ? ""
					: Common.dateFormat4.get().format(pendaftaranCutiMahasiswa.getTanggal())));
		} else {
			row.appendChild(tanggal);
		}
		tanggal.setWidth("90%");
		semesterEventListener.onEvent(null);
		return grid;
	}

	private void init(PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa) throws Exception {

		addWindow.setTitle(pendaftaranCutiMahasiswa.getId() == null ? "Tambah Pendaftaran Cuti Mahasiswa" : "Ubah Pendaftaran Cuti Mahasiswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);

		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan", "/img/save.gif");
		disposisiSop = null;
		center.appendChild(form(pendaftaranCutiMahasiswa, disposisiSop, save, null));

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		save.setTooltiptext("Simpan");
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

	}

	public boolean onSave(Event event) throws Exception {
		if (mahasiswa.getAttribute("mahasiswa") == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Data mahasiswa",
					"Kolom Data mahasiswa belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Data mahasiswa.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}
		if (tahunAkademik.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Tahun Akademik",
					"Kolom Tahun Akademik belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Tahun Akademik.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (ganjilGenap.getSelectedItem() == null) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Jenis Semester",
					"Kolom Jenis Semester belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Jenis Semester.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		if (ConstantValues.aktifkanTahapan) {
			if (tahap.getSelectedItem() == null) {
				PesanFormalHelper.tampilkanGagal("penyimpanan data Tahap",
						"Kolom Tahap belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
						new String[] {
								"Isi/pilih terlebih dahulu Tahap.",
								"Ulangi proses penyimpanan setelah kolom tersebut terisi."
						});
				return false;
			}
		}

		if (keterangan.getValue().trim().isEmpty()) {
			PesanFormalHelper.tampilkanGagal("penyimpanan data Keterangan atau alasan",
					"Kolom Keterangan atau alasan belum Bapak/Ibu isi, padahal kolom ini wajib diisi sebelum data dapat disimpan.",
					new String[] {
							"Isi/pilih terlebih dahulu Keterangan atau alasan.",
							"Ulangi proses penyimpanan setelah kolom tersebut terisi."
					});
			return false;
		}

		Boolean i = checkNamaPendaftaranCutiMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"),
				Integer.parseInt(lblSemester.getValue()));
		if (i) {
			MyMessageboxConfig.show("Data cuti mahasiswa ini sudah ada untuk semester " + lblSemester.getValue(),
					"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		Mahasiswa mhs = (Mahasiswa) mahasiswa.getAttribute("mahasiswa");
		int smt = Integer
				.parseInt(Common.getKonfigurasi("mahasiswa_bisa_cuti_minimal_di_semester", "1").getNilai().trim());
		if (smt > Integer.parseInt(lblSemester.getValue())) {
			MyMessageboxConfig.show("Mahasiswa bisa melakukan cuti minimal di semester " + smt, "Peringatan",
					MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (Common.getKonfigurasi("mahasiswa_bisa_melakukan_cuti_berturut_turut", Konfigurasi.AKTIF).getNilai().trim()
				.equals(Konfigurasi.TIDAK_AKTIF)) {
			int semesterLalu = Integer.parseInt(lblSemester.getValue()) - 1;
			int count = ((Number) HibernateUtil.currentSession().createCriteria(PendaftaranCutiMahasiswa.class)
					.add(Restrictions.eq("mahasiswa", mhs)).add(Restrictions.eq("semester", semesterLalu))
					.setProjection(Projections.rowCount()).uniqueResult()).intValue();
			if (count > 0) {
				MyMessageboxConfig.show(
						"Mahasiswa tidak diperbolehkan melakukan cuti berturut-turut atau cuti kembali di semester "
								+ lblSemester.getValue() + ", karena mahasiswa dengan NIM " + mhs.getNim()
								+ " telah melakukan cuti di semester " + semesterLalu,
						"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		if (!checkMahasiswa(Integer.parseInt(lblSemester.getValue().trim()),
				(String) tahunAkademik.getSelectedItem().getValue())) {
			return false;
		}

		if (Common.getKonfigurasi("mahasiswa_bisa_melakukan_cuti_jika_sudah_bayar_daftar_ulang", Konfigurasi.AKTIF)
				.getNilai().trim().equals(Konfigurasi.TIDAK_AKTIF)) {
			JenisKegiatan jenisKegiatan = pembayaranUtil
					.generateJenisKegiatan(ConstantUtilBankMandiri.PENDAFTARAN_MAHASISWA_LAMA);
			Kegiatan kegiatan = ((Mahasiswa) mahasiswa.getAttribute("mahasiswa"))
					.ambilKegiatans(Integer.parseInt(lblSemester.getValue()), jenisKegiatan);

			if (kegiatan != null && kegiatan.getId() != null && kegiatan.getAmount() > 0.1) {
				MyMessageboxConfig.show(
						"Mahasiswa ini sudah melakukan pembayaran di semester " + lblSemester.getValue(), "Peringatan",
						MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION);
				return false;
			}
		}

		PendaftaranCutiMahasiswaDao pendaftaranCutiMahasiswaDao = DaoFactory.getInstance()
				.getPendaftaranCutiMahasiswaDao();
		if (pendaftaranCutiMahasiswa.getId() != null) {
			pendaftaranCutiMahasiswa = pendaftaranCutiMahasiswaDao.load(pendaftaranCutiMahasiswa.getId());
		}

		try {
			pendaftaranCutiMahasiswa.setSemester(Integer.parseInt(lblSemester.getValue()));
		} catch (Exception e) {
			pendaftaranCutiMahasiswa = new PendaftaranCutiMahasiswa();
			pendaftaranCutiMahasiswa.setSemester(Integer.parseInt(lblSemester.getValue()));
		}

		pendaftaranCutiMahasiswa.setTahunAkademik((String) tahunAkademik.getSelectedItem().getValue());
		pendaftaranCutiMahasiswa.setGanjilGenap((String) ganjilGenap.getSelectedItem().getValue());
		pendaftaranCutiMahasiswa.setMahasiswa((Mahasiswa) mahasiswa.getAttribute("mahasiswa"));
		String keteranganCuti = keterangan.getValue();
		// Skema instalasi lama masih memakai varchar(255). Batasi di boundary form
		// agar permohonan tetap tersimpan dan tidak menggagalkan transaksi lain.
		if (keteranganCuti != null && keteranganCuti.length() > 255) {
			keteranganCuti = keteranganCuti.substring(0, 255);
		}
		pendaftaranCutiMahasiswa.setKeterangan(keteranganCuti);
		pendaftaranCutiMahasiswa.setTanggal(tanggal.getValue());
		pendaftaranCutiMahasiswa.setTahap((Integer) (tahap == null || tahap.getSelectedItem() == null ? null
				: tahap.getSelectedItem().getValue()));
		pendaftaranCutiMahasiswa.setSemesterPendek(semesterPendek.isChecked());
		pendaftaranCutiMahasiswa.setKode(kode.getValue());

		if (persetujuan != null) {
			pendaftaranCutiMahasiswa.setPersetujuan(persetujuan.isChecked());
		} else {
			pendaftaranCutiMahasiswa.setPersetujuan(false);
		}

		if (pendaftaranCutiMahasiswa.getId() != null) {
			pendaftaranCutiMahasiswaDao.update(pendaftaranCutiMahasiswa);
		} else {
			pendaftaranCutiMahasiswaDao.save(pendaftaranCutiMahasiswa);
		}

		Session session = HibernateUtil.currentSession();

		HistoryStatusMahasiswa historyStatusMahasiswa = updateStatus(pendaftaranCutiMahasiswa, session);

		session.saveOrUpdate(historyStatusMahasiswa);

		historyStatusMahasiswa.write("tulis ulang dari " + this.getClass().getName());

		if (lainMahasiswa != null && lainMahasiswa.getId() != null) {
			try {
				session = StreamingHibernateUtil.getInstance().currentSession();

				session.refresh(lainMahasiswa);
				lainMahasiswa.setRef(pendaftaranCutiMahasiswa.getId());

				session.getTransaction().begin();
				session.update(lainMahasiswa);
				session.getTransaction().commit();

				StreamingHibernateUtil.getInstance().closeSession();
			} catch (Exception e) {
				StreamingHibernateUtil.getInstance().rollbackTransaction();
				Common.tampilErrorJikaAdmin(e);
			}

		}

		return true;
	}

	public HistoryStatusMahasiswa updateStatus(PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa, Session session) {
		Mahasiswa mahasiswa = pendaftaranCutiMahasiswa.getMahasiswa();

		Integer tahap = pendaftaranCutiMahasiswa.getTahap();
		Integer semester = pendaftaranCutiMahasiswa.getSemester();

		Criterion criterionSemester = tahap == null || tahap.equals(0) ? Restrictions.eq("semester", semester)
				: Restrictions.sqlRestriction("true");

		Criterion criterionTahapan = tahap == null || tahap.equals(0) ? Restrictions.sqlRestriction("true")
				: Restrictions.eq("tahap", tahap);

		HistoryStatusMahasiswa historyStatusMahasiswa = (HistoryStatusMahasiswa) session
				.createCriteria(HistoryStatusMahasiswa.class)
				.add(pendaftaranCutiMahasiswa.getSemesterPendek() ? Restrictions.eq("sp", Perkuliahan.SEMESTER_PENDEK)
						: Restrictions.isNull("sp"))
				.add(criterionSemester).add(criterionTahapan).add(Restrictions.eq("mahasiswa", mahasiswa))
				.addOrder(Order.desc("id")).setMaxResults(1).uniqueResult();
		if (historyStatusMahasiswa == null) {
			historyStatusMahasiswa = new HistoryStatusMahasiswa(
					pendaftaranCutiMahasiswa.getSemesterPendek() ? Perkuliahan.SEMESTER_PENDEK : null);
		}
		historyStatusMahasiswa.setTahunAkademik(pendaftaranCutiMahasiswa.getTahunAkademik());
		historyStatusMahasiswa.setMahasiswa(mahasiswa);
		historyStatusMahasiswa.setSemester(pendaftaranCutiMahasiswa.getSemester());
		historyStatusMahasiswa.setTahap(tahap);
		historyStatusMahasiswa.setStatusMahasiswa(
				pendaftaranCutiMahasiswa.getPersetujuan() ? ConstantValues.CUTI : ConstantValues.AKTIF);
		return historyStatusMahasiswa;
	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(PendaftaranCutiMahasiswa.class)

				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))

				.add(jenisSemester.getSelectedItem() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("ganjilGenap", jenisSemester.getSelectedItem().getValue()))

				.createAlias("mahasiswa", "mahasiswa").createAlias("mahasiswa.jurusan", "jurusan")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false));

		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria.add(Restrictions.ilike("mahasiswa.nama", searchnama.getValue(), MatchMode.ANYWHERE))
				.add(Restrictions.ilike("mahasiswa.nim", searchnim.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {

		if (searchnama == null) {
			return;
		}

		Common.initPaging(initCriteria(false), paging);

		List<PendaftaranCutiMahasiswa> pendaftaranCutiMahasiswa = initCriteria(true)
				.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pendaftaranCutiMahasiswa);
		grid.setRowRenderer(new PendaftaranCutiMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public Boolean checkNamaPendaftaranCutiMahasiswa(Mahasiswa mahasiswa, Integer semester) {

		Integer kotaCount = null;
		Session session = HibernateUtil.currentSession();
		kotaCount = ((Number) session.createCriteria(PendaftaranCutiMahasiswa.class)
				.setProjection(Projections.rowCount()).add(Restrictions.eq("mahasiswa", mahasiswa))

				.add(semesterPendek.isChecked() ? Restrictions.eq("semesterPendek", true)
						: Restrictions.or(Restrictions.isNull("semesterPendek"),
								Restrictions.eq("semesterPendek", false)))

				.add(tahap == null || tahap.getSelectedItem() == null ? Restrictions.eq("semester", semester)
						: Restrictions.eq("tahap", tahap.getSelectedItem().getValue()))
				.add(this.pendaftaranCutiMahasiswa.getId() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ne("id", this.pendaftaranCutiMahasiswa.getId()))
				.uniqueResult()).intValue();

		return !kotaCount.equals(0);
	}

	@Override
	public String istilah() throws Exception {
		// TODO Auto-generated method stub
		return "Pengajuan Cuti Mahasiswa";
	}

	@Override
	public DataSop ambil() throws Exception {
		// TODO Auto-generated method stub
		return pendaftaranCutiMahasiswa;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class ambilClass() throws Exception {
		// TODO Auto-generated method stub
		return PendaftaranCutiMahasiswa.class;
	}

	@Override
	public void setPersetujuan(boolean persetujuan) {
		this.persetujuanD = persetujuan;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public File cetakData(GeneralValueObject generalValueObject) throws Exception {
		// TODO Auto-generated method stub
		PendaftaranCutiMahasiswa pendaftaranCutiMahasiswa = (PendaftaranCutiMahasiswa) generalValueObject;
		if (!pendaftaranCutiMahasiswa.getPersetujuan()) {
			Map<String, Object> parameters = ais.common.HashMapGenerator.getRandStringObject();
			parameters.put("id", pendaftaranCutiMahasiswa.getId());

			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(pendaftaranCutiMahasiswa.getMahasiswa(),
					pendaftaranCutiMahasiswa.getSemester(), pendaftaranCutiMahasiswa.getTahap(),
					pendaftaranCutiMahasiswa.getSemesterPendek() ? Perkuliahan.SEMESTER_PENDEK : null);
			HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
			Common.insertProperty(HistoryStatusMahasiswa.class, historyStatusMahasiswa, parameters, "status");

			Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");

			File file = Report.generateFileReport("Keterangan_Cuti", Report.PDF, parameters, "Keterangan_Cuti",
					new Date());

			return file;
		} else {
			Map parameters = ais.common.HashMapGenerator.getRandStringObject();
			parameters.put("id", pendaftaranCutiMahasiswa.getId());
			KrsMahasiswa krsMahasiswa = Common.singkronkanKrsMahasiswa(pendaftaranCutiMahasiswa.getMahasiswa(),
					pendaftaranCutiMahasiswa.getSemester(), pendaftaranCutiMahasiswa.getTahap(),
					pendaftaranCutiMahasiswa.getSemesterPendek() ? Perkuliahan.SEMESTER_PENDEK : null);
			HistoryStatusMahasiswa historyStatusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(krsMahasiswa);
			Common.insertProperty(HistoryStatusMahasiswa.class, historyStatusMahasiswa, parameters, "status");

			Common.insertProperty(KrsMahasiswa.class, krsMahasiswa, parameters, "krs");

			File file = Report.generateFileReport("Persetujuan_Cuti", Report.PDF, parameters, "Persetujuan_Cuti",
					new Date());

			return file;
		}
	}

}
