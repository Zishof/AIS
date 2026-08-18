package ais.action.master.pmb;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiHelper;
import ais.action.master.helper.generic.AmbilDataBiodataCalonMahasiswaBanyak;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.AfiliasiCalonMahasiswa;
import ais.database.model.BiodataCalonMahasiswa;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class AfiliasiCalonMahasiswaDetailAction extends MyDetail implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5086031585928643232L;

	private AfiliasiCalonMahasiswa afiliasiCalonMahasiswa;
	private MyGrid grid;

	private Textbox pencarian;
	private Combobox searchTahunAjaran;

	public AfiliasiCalonMahasiswaDetailAction(AfiliasiCalonMahasiswa afiliasiCalonMahasiswa,
			Combobox searchTahunAjaran) {
		super();
		this.afiliasiCalonMahasiswa = afiliasiCalonMahasiswa;
		this.searchTahunAjaran = searchTahunAjaran;
		this.addEventListener(Events.ON_OPEN, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Common.clear(AfiliasiCalonMahasiswaDetailAction.this);
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
			final BiodataCalonMahasiswa calonMahasiswa = (BiodataCalonMahasiswa) data;

			CommonMedia.tampilkanGambarKecil(calonMahasiswa).setParent(arg0);

			RevisiHelper.createNewRevisi(BiodataCalonMahasiswa.class, calonMahasiswa, calonMahasiswa.getNama())
					.setParent(arg0);

			new Label(calonMahasiswa.getTanggalLahir() == null
					? Common.dateFormat2.get().format(ais.ui.util.WaktuUtil.getDate())
					: Common.dateFormat2.get().format(calonMahasiswa.getTanggalLahir())).setParent(arg0);
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
			if (calonMahasiswa.getMundur()) {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Mengundurkan diri")));
			} else if (calonMahasiswa.getDitolak()) {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Tidak diterima (ditolak)")));
			} else if (calonMahasiswa.getProdiLulus() != null) {
				vbox.appendChild(new Label("Lulus di prodi : " + (calonMahasiswa.getProdiLulus())));
			} else {
				vbox.appendChild(new Label(ais.common.Common.getBahasaConfig("Belum lulus")));
			}
			vbox.setParent(arg0);

			new Label(calonMahasiswa.getAfiliasiCalonMahasiswa() == null ? "Tidak" : "Ya").setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			button.setVisible(calonMahasiswa.getAfiliasiCalonMahasiswa() != null);
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
											calonMahasiswa.setAfiliasiCalonMahasiswa(null);
											Common.refreshSaveOrUpdate(calonMahasiswa);

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
			button.setParent(arg0);
		}
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		List<BiodataCalonMahasiswa> biodataCalonMahasiswas = initCriteria(true).setMaxResults(500).list();

		ListModel strset = new SimpleListModel(biodataCalonMahasiswas);
		grid.setRowRenderer(new BiodataCalonMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

	public void display() {

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(this);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar calon mahasiswa yang masuk afiliasi " + afiliasiCalonMahasiswa.getNama()));
		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Ambil Data Calon Mahasiswa", "/img/add_item.png");
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {

				List<BiodataCalonMahasiswa> biodataCalonMahasiswas = initCriteria(false).list();

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
										biodataCalonMahasiswa.setAfiliasiCalonMahasiswa(afiliasiCalonMahasiswa);
										Common.refreshUpdate(session, biodataCalonMahasiswa);
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

		pencarian = new Textbox();
		pencarian.setCols(8);
		pencarian.setParent(toolbar);
		pencarian.addEventListener("onOK", new EventListener() {

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

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(BiodataCalonMahasiswa.class, this,
				CetakRegistrasiAction.contents);
		toolbar.appendChild(cetakToolbarbutton);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
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
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Pilihan Prodi");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Manual");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);
	}

	@Override
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		return session.createCriteria(BiodataCalonMahasiswa.class).add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.add(searchTahunAjaran.getSelectedItem() == null
						|| searchTahunAjaran.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
								: Restrictions.eq("tahunAkademik", searchTahunAjaran.getSelectedItem().getValue()))
				.add(pencarian.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.or(
								Restrictions.ilike("noUjian", pencarian.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("nama", pencarian.getValue().trim(), MatchMode.ANYWHERE),
										Restrictions.ilike("noRegistrasi", pencarian.getValue().trim(),
												MatchMode.ANYWHERE))))

				.addOrder(Order.desc("id")).add(Restrictions.eq("afiliasiCalonMahasiswa", afiliasiCalonMahasiswa));
	}

}
