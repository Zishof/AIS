package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataBiodataCalonMahasiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.BiodataCalonMahasiswa;
import ais.database.model.InterviewCalonMahasiswa;
import ais.database.model.InterviewPunyaCalonMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;

public class InterviewPunyaCalonMahasiswaDetailAction extends MyDetail implements DataCriteria, DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private InterviewCalonMahasiswa interviewCalonMahasiswa;
	private MyGrid grid;

	private Textbox nama;

	private boolean edit;

	public InterviewPunyaCalonMahasiswaDetailAction(InterviewCalonMahasiswa interviewCalonMahasiswa, boolean edit) {
		super();
		this.edit = edit;
		this.interviewCalonMahasiswa = interviewCalonMahasiswa;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(InterviewPunyaCalonMahasiswaDetailAction.this);
				if (isOpen()) {
					display();
				}
			}
		});
	}

	class BiodataCalonMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		public BiodataCalonMahasiswaRenderer() {

		}

		@Override
		public void render(final Row arg0, Object data) throws Exception {
			// TODO Auto-generated method stub
			final InterviewPunyaCalonMahasiswa interviewPunyaCalonMahasiswa = (InterviewPunyaCalonMahasiswa) data;
			final BiodataCalonMahasiswa calonMahasiswa = interviewPunyaCalonMahasiswa.getBiodataCalonMahasiswa();

			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);

			detail.addEventListener("onOpen", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CetakRegistrasiAction.bukaRinci(detail, calonMahasiswa);
				}
			});

			CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(arg0);

			RevisiHelper.createNewRevisi(InterviewPunyaCalonMahasiswa.class, interviewPunyaCalonMahasiswa,
					calonMahasiswa.getNama()).setParent(arg0);

			RevisiHelper.createNewRevisi(BiodataCalonMahasiswa.class, calonMahasiswa,
					calonMahasiswa.getTanggalLahir() == null
							? Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
							: Common.dateFormat2.get().format(calonMahasiswa.getTanggalLahir()))
					.setParent(arg0);

			new Label(calonMahasiswa.getAsalSma() == null ? "" : calonMahasiswa.getAsalSma()).setParent(arg0);

			Vbox vbox = new Vbox();
			if (calonMahasiswa.getJenisSeleksi() != null)
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getJenisSeleksi().toString())));
			if (calonMahasiswa.getNoRegistrasi() != null && !calonMahasiswa.getNoRegistrasi().trim().isEmpty())
				vbox.appendChild(new Label("No. Reg.:" + (calonMahasiswa.getNoRegistrasi())));
			if (calonMahasiswa.getNoUjian() != null && !calonMahasiswa.getNoUjian().trim().isEmpty())
				vbox.appendChild(new Label("No. Ujian:" + (calonMahasiswa.getNoUjian())));
			if (calonMahasiswa.getTotalSkor() > 0)
				vbox.appendChild(new Label("Skor :" + Common.numberFormat.get().format((calonMahasiswa.getTotalSkor()))));
			vbox.appendChild(new Label("Login :" + (calonMahasiswa.getTelahLogin() ? "Ya" : "Tidak")));
			if (calonMahasiswa.getWaktuLogin() != null)
				vbox.appendChild(
						new Label("Terakhir Login :" + Common.dateFormat.get().format(calonMahasiswa.getWaktuLogin())));
			if (calonMahasiswa.getNim() != null && !calonMahasiswa.getNim().trim().isEmpty())
				vbox.appendChild(new Label("NIM :" + (calonMahasiswa.getNim())));
			if (calonMahasiswa.getMerupakanPindahan()) {
				vbox.appendChild(new Label("Pindahan dari :" + (calonMahasiswa.getPindahanDariKampus())));
				vbox.appendChild(new Label("Prodi :" + (calonMahasiswa.getPindahanDariProdi())));
				vbox.appendChild(
						new Label("Pindah di semester :" + (calonMahasiswa.getPindahDariKampusLamaDiSemester())));
				vbox.appendChild(new Label("NIM lama :" + (calonMahasiswa.getNimLamaSebelumPindah())));
				vbox.appendChild(new Label("Alasan pindah:" + (calonMahasiswa.getKeteranganPindah())));
			}

			vbox.setParent(arg0);

			vbox = new Vbox();
			if (calonMahasiswa.getProdi1() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi1())));
			}
			if (calonMahasiswa.getProdi2() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi2())));
			}
			if (calonMahasiswa.getProdi3() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi3())));
			}
			if (calonMahasiswa.getProdi4() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi4())));
			}
			if (calonMahasiswa.getProdi5() != null) {
				vbox.appendChild(new Label("" + (calonMahasiswa.getProdi5())));
			}
			if (calonMahasiswa.getProdiLulus() != null) {
				vbox.appendChild(new Label("Lulus di prodi : " + (calonMahasiswa.getProdiLulus())));
			} else {
				vbox.appendChild(new Label("Belum / tidak lulus"));
			}
			vbox.setParent(arg0);

			final MyDatebox mulai;
			arg0.appendChild(mulai = new MyDatebox(interviewPunyaCalonMahasiswa.getMulai()));
			mulai.setReadonly(true);
			mulai.setFormat(Common.dateFormat.get().toPattern());
			mulai.setWidth("95%");
			mulai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					interviewPunyaCalonMahasiswa.setMulai(mulai.getValue());
					Common.refreshUpdate(interviewPunyaCalonMahasiswa);
				}
			});

			final MyDatebox sampai;
			arg0.appendChild(sampai = new MyDatebox(interviewPunyaCalonMahasiswa.getSampai()));
			sampai.setReadonly(true);
			sampai.setFormat(Common.dateFormat.get().toPattern());
			sampai.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					interviewPunyaCalonMahasiswa.setSampai(sampai.getValue());
					Common.refreshUpdate(interviewPunyaCalonMahasiswa);
				}
			});

			final MyCheckboxConfig checkbox = new MyCheckboxConfig("Siap");
			checkbox.setDisabled(!edit);
			checkbox.setChecked(interviewPunyaCalonMahasiswa.getSiap());
			checkbox.setParent(arg0);
			arg0.setAttribute("checkbox", checkbox);
			checkbox.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					interviewPunyaCalonMahasiswa.setSiap(checkbox.isChecked());
					Common.refreshSaveOrUpdate(interviewPunyaCalonMahasiswa);
				}
			});

			final MyTextbox catatan;
			arg0.appendChild(catatan = new MyTextbox(interviewPunyaCalonMahasiswa.getKeterangan()));
			catatan.setWidth("95%");
			catatan.setRows(2);
			catatan.addEventListener("onChange", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					interviewPunyaCalonMahasiswa.setKeterangan(catatan.getValue());
					Common.refreshUpdate(interviewPunyaCalonMahasiswa);
				}
			});

			// kebab popup (⋯) via UIHelper.buatBarisAksi — kolom aksi jadi kecil dan konsisten.
			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setOrient("vertical");
			button.setTooltiptext("Hapus Data");
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
											Common.refreshDelete(interviewPunyaCalonMahasiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													loadData(null);
												}
											});

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

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<InterviewPunyaCalonMahasiswa> biodataCalonMahasiswas = initCriteria(true).list();

		ListModel strset = new SimpleListModel(biodataCalonMahasiswas);
		grid.setRowRenderer(new BiodataCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Calon Mahasiswa", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<BiodataCalonMahasiswa> biodataCalonMahasiswas = ConstantValues.simpleList(
						HibernateUtil.currentSession().createCriteria(InterviewPunyaCalonMahasiswa.class)
								.addOrder(Order.asc("id"))
								.setProjection(Projections.property("biodataCalonMahasiswa.id"))
								.add(Restrictions.eq("interviewCalonMahasiswa", interviewCalonMahasiswa)),
						BiodataCalonMahasiswa.class, false);

				AmbilDataBiodataCalonMahasiswaBanyak window = new AmbilDataBiodataCalonMahasiswaBanyak(
						biodataCalonMahasiswas);

				ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(window);
				window.setWidth("90%");
				window.setHeight("90%");

				window.setEventListener(new EventListener() {

					@Override
					public void onEvent(final Event dataCalonMhs) throws Exception {

						Common.createDefaultTimer(new EventListener() {

							@Override
							public void onEvent(Event arg0) throws Exception {
								List<BiodataCalonMahasiswa> biodataCalonMahasiswas = (List<BiodataCalonMahasiswa>) dataCalonMhs
										.getData();

								if (biodataCalonMahasiswas != null) {
									Session session = HibernateUtil.currentSession();
									for (BiodataCalonMahasiswa biodataCalonMahasiswa : biodataCalonMahasiswas) {

										boolean boleh = interviewCalonMahasiswa.getJurusan() == null
												|| (interviewCalonMahasiswa.getJurusan() != null
														&& biodataCalonMahasiswa.getProdi1() != null
														&& interviewCalonMahasiswa.getJurusan().getId()
																.equals(biodataCalonMahasiswa.getProdi1().getId()))
												|| (interviewCalonMahasiswa.getJurusan() != null
														&& biodataCalonMahasiswa.getProdi2() != null
														&& interviewCalonMahasiswa.getJurusan().getId()
																.equals(biodataCalonMahasiswa.getProdi2().getId()))

												|| (interviewCalonMahasiswa.getJurusan() != null
														&& biodataCalonMahasiswa.getProdi3() != null
														&& interviewCalonMahasiswa.getJurusan().getId()
																.equals(biodataCalonMahasiswa.getProdi3().getId()))

												|| (interviewCalonMahasiswa.getJurusan() != null
														&& biodataCalonMahasiswa.getProdi4() != null
														&& interviewCalonMahasiswa.getJurusan().getId()
																.equals(biodataCalonMahasiswa.getProdi4().getId()))

												|| (interviewCalonMahasiswa.getJurusan() != null
														&& biodataCalonMahasiswa.getProdi5() != null
														&& interviewCalonMahasiswa.getJurusan().getId()
																.equals(biodataCalonMahasiswa.getProdi5().getId()))

										;

										if (!boleh) {
											boleh = interviewCalonMahasiswa.getFakultas() == null
													|| (interviewCalonMahasiswa.getFakultas() != null
															&& biodataCalonMahasiswa.getProdi1() != null
															&& interviewCalonMahasiswa.getFakultas().getId()
																	.equals(biodataCalonMahasiswa.getProdi1()
																			.getFakultas().getId()))
													|| (interviewCalonMahasiswa.getFakultas() != null
															&& biodataCalonMahasiswa.getProdi2() != null
															&& interviewCalonMahasiswa.getFakultas().getId()
																	.equals(biodataCalonMahasiswa.getProdi2()
																			.getFakultas().getId()))

													|| (interviewCalonMahasiswa.getFakultas() != null
															&& biodataCalonMahasiswa.getProdi3() != null
															&& interviewCalonMahasiswa.getFakultas().getId()
																	.equals(biodataCalonMahasiswa.getProdi3()
																			.getFakultas().getId()))

													|| (interviewCalonMahasiswa.getFakultas() != null
															&& biodataCalonMahasiswa.getProdi4() != null
															&& interviewCalonMahasiswa.getFakultas().getId()
																	.equals(biodataCalonMahasiswa.getProdi4()
																			.getFakultas().getId()))

													|| (interviewCalonMahasiswa.getFakultas() != null
															&& biodataCalonMahasiswa.getProdi5() != null
															&& interviewCalonMahasiswa.getFakultas().getId()
																	.equals(biodataCalonMahasiswa.getProdi5()
																			.getFakultas().getId()))

											;
										}

										if (boleh) {

											InterviewPunyaCalonMahasiswa interviewPunyaCalonMahasiswa = (InterviewPunyaCalonMahasiswa) session
													.createCriteria(InterviewPunyaCalonMahasiswa.class)

													.add(Restrictions.eq("biodataCalonMahasiswa",
															biodataCalonMahasiswa))
													.setMaxResults(1).uniqueResult();
											if (interviewPunyaCalonMahasiswa == null) {
												interviewPunyaCalonMahasiswa = new InterviewPunyaCalonMahasiswa();
											}
											interviewPunyaCalonMahasiswa
													.setBiodataCalonMahasiswa(biodataCalonMahasiswa);
											interviewPunyaCalonMahasiswa
													.setInterviewCalonMahasiswa(interviewCalonMahasiswa);
											Common.refreshSaveOrUpdate(session, interviewPunyaCalonMahasiswa);
										}
									}

									loadData(null);
								}
							}
						});

					}
				});

				window.onModal();

			}

		});
		button.setParent(toolbar);

		String[] contents = new String[] { "id", "interviewCalonMahasiswa", "biodataCalonMahasiswa", "mulai", "sampai",
				"keterangan" };
		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(InterviewPunyaCalonMahasiswa.class, this, contents);
		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, InterviewPunyaCalonMahasiswa.class, contents);
		toolbar.appendChild(upload);

		toolbar.appendChild(new Space());
		toolbar.appendChild(new Space());

		toolbar.appendChild(new Label("Nama/No.Reg/Ujian : "));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(8);
		nama.addEventListener("onOK", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		MyToolbarbuttonConfig cari = new MyToolbarbuttonConfig("Refresh", "/img/Button-Refresh-icon.png");
		cari.setParent(toolbar);
		cari.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		cetakToolbarbutton = Common.cetakData(new DataCriteria() {

			@Override
			public Criteria initCriteria(boolean order) {
				Session session = HibernateUtil.currentSession();
				return session.createCriteria(InterviewPunyaCalonMahasiswa.class)
						.setProjection(Projections.property("biodataCalonMahasiswa")).addOrder(Order.asc("id"))
						.add(Restrictions.eq("interviewCalonMahasiswa", interviewCalonMahasiswa));
			}
		}, CetakRegistrasiAction.contents);
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);
		grid.setParent(groupbox);
		grid.getPagingChild().setMold("os");
		grid.getPagingChild().setDetailed(true);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("40px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Foto");
		column.setWidth("70px");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Tanggal Lahir");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Asal Sekolah/Kampus");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("No. Registrasi, Ujian, NIM");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan Prodi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Mulai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sampai");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Siap Intrv.");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Catatan");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(InterviewPunyaCalonMahasiswa.class)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa")
				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("biodataCalonMahasiswa.nama", nama.getValue().trim(),
										MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("biodataCalonMahasiswa.noUjian", nama.getValue().trim(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", nama.getValue().trim(),
												MatchMode.ANYWHERE))))
				.addOrder(Order.asc("id")).add(Restrictions.eq("interviewCalonMahasiswa", interviewCalonMahasiswa));
	}

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
