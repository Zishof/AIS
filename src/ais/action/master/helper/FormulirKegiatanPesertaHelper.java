package ais.action.master.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.poi.ss.usermodel.Hyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFCell;
import org.zkoss.poi.xssf.usermodel.XSSFCellStyle;
import org.zkoss.poi.xssf.usermodel.XSSFColor;
import org.zkoss.poi.xssf.usermodel.XSSFFont;
import org.zkoss.poi.xssf.usermodel.XSSFHyperlink;
import org.zkoss.poi.xssf.usermodel.XSSFRow;
import org.zkoss.poi.xssf.usermodel.XSSFWorkbook;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.SertifikatAction;
import ais.action.master.helper.generic.AmbilDataDosenBanyak;
import ais.action.master.sekolah.helper.AmbilDataGuruBanyak;
import ais.action.master.sekolah.helper.AmbilDataSiswaForFormulirKegiatanHelper;
import ais.action.report.format1.akademik.LaporanPendidikanLingkunganKampus;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonMedia;
import ais.common.ConstantValues;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Dosen;
import ais.database.model.Fakultas;
import ais.database.model.FormulirKegiatan;
import ais.database.model.FormulirKegiatanPeserta;
import ais.database.model.GrupFormulirKegiatan;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKedosenanPunyaDosen;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.KrsMahasiswa;
import ais.database.model.Mahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.VOMahasiswaDosen;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.Guru;
import ais.database.model.sekolah.KelasSiswaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class FormulirKegiatanPesertaHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private FormulirKegiatan formulirKegiatan;
	private Textbox nim;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	private Paging paging;
	private MyCheckboxConfig tampilAcc;
	private MyCheckboxConfig tampilBelumAcc;

	private Tbmuser tbmuser;
	private GrupFormulirKegiatan grupFormulirKegiatan;

	public FormulirKegiatanPesertaHelper() {
		tbmuser = Common.getCurrentUser();
		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);
		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

	}

	class DetailFormulirKegiatanRenderer extends ais.ui.util.MyRowRenderer {

		public DetailFormulirKegiatanRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) data;

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Hbox hbox = new Hbox();
			hbox.setParent(row);

			VOMahasiswaDosen peserta;
			if (formulirKegiatanPeserta.getMahasiswa() != null) {
				CommonMedia.tampilkanGambarKecil(formulirKegiatanPeserta.getMahasiswa()).setParent(hbox);
				peserta = formulirKegiatanPeserta.getMahasiswa();
			} else if (formulirKegiatanPeserta.getDosen() != null) {
				CommonMedia.tampilkanGambarKecil(formulirKegiatanPeserta.getDosen()).setParent(hbox);
				peserta = formulirKegiatanPeserta.getDosen();
			} else if (formulirKegiatanPeserta.getSiswa() != null) {
				CommonMedia.tampilkanGambarKecil(formulirKegiatanPeserta.getSiswa()).setParent(hbox);
				peserta = formulirKegiatanPeserta.getSiswa();
			} else if (formulirKegiatanPeserta.getGuru() != null) {
				CommonMedia.tampilkanGambarKecil(formulirKegiatanPeserta.getGuru()).setParent(hbox);
				peserta = formulirKegiatanPeserta.getGuru();
			} else {
				row.setVisible(false);
				return;
			}

			Vbox vbox = new Vbox();
			vbox.setParent(hbox);

			new Label(formulirKegiatanPeserta.getKode()).setParent(vbox);
			RevisiHelper.createNewRevisi(FormulirKegiatanPeserta.class, formulirKegiatanPeserta, peserta.ambilKode())
					.setParent(vbox);

			new Label(peserta.getNama()).setParent(vbox);

			new Label(formulirKegiatanPeserta.getMahasiswa() != null ? "Mahasiswa"
					: formulirKegiatanPeserta.getDosen() != null ? "Dosen"
							: formulirKegiatanPeserta.getSiswa() != null ? "Siswa"
									: formulirKegiatanPeserta.getGuru() != null ? "Guru" : "")
					.setParent(vbox);

			vbox = new Vbox();
			vbox.setParent(detail);
			hbox = new Hbox();
			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Kartu Peserta", "/img/print.png");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					List<FormulirKegiatanPeserta> formulirKegiatanPesertas = new ArrayList<FormulirKegiatanPeserta>();
					formulirKegiatanPesertas.add(formulirKegiatanPeserta);
					SertifikatAction.cetakFormPendafatranKegiatan(formulirKegiatan, formulirKegiatanPesertas);
				}

			});
			button.setParent(hbox);

			Hbox hbox1 = new Hbox();
			hbox1.setParent(hbox);

			LampiranLain.createDownloadUploadFileLain(hbox1, formulirKegiatanPeserta.getId(),
					FormulirKegiatanPeserta.class.getName(), "Persyaratan", false, new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					});
			hbox.setParent(vbox);

			new Label(formulirKegiatanPeserta.getTanggal_dirubah() == null ? ""
					: Common.dateFormat5.get().format(formulirKegiatanPeserta.getTanggal_dirubah())).setParent(row);

			if (formulirKegiatanPeserta.getSiswa() != null) {
				new Label(formulirKegiatanPeserta.getSiswa().getSekolah() == null ? ""
						: formulirKegiatanPeserta.getSiswa().getSekolah().getNama() + "").setParent(row);
			} else if (formulirKegiatanPeserta.getGuru() != null) {
				new Label(formulirKegiatanPeserta.getGuru().getSekolah() == null ? ""
						: formulirKegiatanPeserta.getGuru().getSekolah().getNama() + "").setParent(row);
			} else if (formulirKegiatanPeserta.getDosen() != null) {
				new Label(formulirKegiatanPeserta.getDosen().getJurusan() == null ? ""
						: formulirKegiatanPeserta.getDosen().getJurusan().getNama() + "").setParent(row);
			} else if (formulirKegiatanPeserta.getMahasiswa() != null) {
				new Label(formulirKegiatanPeserta.getMahasiswa().getJurusan() == null ? ""
						: formulirKegiatanPeserta.getMahasiswa().getJurusan().getNama() + "").setParent(row);
			} else {
				new Label().setParent(row);
			}

			boolean boleh = tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.getSiswa() == null
					&& tbmuser.getSiswa() == null && tbmuser.getBiodataCalonMahasiswa() == null
					&& tbmuser.getCalonSiswa() == null && tbmuser.getSiswa() == null && tbmuser.ambilDosen() == null
					&& tbmuser.ambilGuru() == null;

			if (tbmuser != null && tbmuser.ambilGuru() != null
					&& formulirKegiatanPeserta.getFormulirKegiatan().getGuruPembina() != null && formulirKegiatanPeserta
							.getFormulirKegiatan().getGuruPembina().getId().equals(tbmuser.getGuru().getId())) {
				boleh = true;
			}

			if (tbmuser != null && tbmuser.ambilGuru() != null && formulirKegiatanPeserta.getSiswa() != null
					&& formulirKegiatanPeserta.getSiswa().getGuruPembina() != null
					&& formulirKegiatanPeserta.getSiswa().getGuruPembina().getId().equals(tbmuser.getGuru().getId())) {
				boleh = true;
			}

			if (tbmuser != null && tbmuser.ambilDosen() != null
					&& formulirKegiatanPeserta.getFormulirKegiatan().getDosenPembina() != null
					&& formulirKegiatanPeserta.getFormulirKegiatan().getDosenPembina().getId()
							.equals(tbmuser.getDosen().getId())) {
				boleh = true;
			}

			if (boleh) {

				final MyTextbox nilai = new MyTextbox(formulirKegiatanPeserta.getNilai());
				nilai.setWidth("90%");
				nilai.setRows(2);

				EventListener eventListenerNilai = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						formulirKegiatanPeserta.setNilai(nilai.getValue());
						Common.refreshUpdate(formulirKegiatanPeserta);

					}
				};

				nilai.addEventListener("onChange", eventListenerNilai);
				nilai.setParent(row);

				final MyTextbox keterangan = new MyTextbox(formulirKegiatanPeserta.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {

						formulirKegiatanPeserta.setKeterangan(keterangan.getValue());
						Common.refreshUpdate(formulirKegiatanPeserta);

					}
				};

				keterangan.addEventListener("onChange", eventListener);

				keterangan.setParent(row);
			} else {
				new Label(formulirKegiatanPeserta.getNilai()).setParent(row);
				new Label(formulirKegiatanPeserta.getKeterangan()).setParent(row);
			}

			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			cetakToolbarbuttonSertifikat.setOrient("vertical");
			final MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			deleteButton.setOrient("vertical");

			final MyCheckboxConfig acc = new MyCheckboxConfig("Acc");
			acc.setChecked(formulirKegiatanPeserta.getAcc());
			acc.setParent(row);
			acc.addEventListener("onCheck", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					formulirKegiatanPeserta.setAcc(acc.isChecked());
					Common.refreshSaveOrUpdate(formulirKegiatanPeserta);
					cetakToolbarbuttonSertifikat
							.setVisible(formulirKegiatanPeserta.getAcc() && formulirKegiatan.getSertifikat() != null);
					deleteButton.setVisible(!formulirKegiatanPeserta.getAcc() && tbmuser != null
							&& tbmuser.ambilGuru() == null && tbmuser.ambilDosen() == null);
				}
			});
			cetakToolbarbuttonSertifikat
					.setVisible(formulirKegiatanPeserta.getAcc() && formulirKegiatan.getSertifikat() != null);

			final Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig cetakToolbarbutton = new MyToolbarbuttonConfig("Cetak", "/img/print.png");
			cetakToolbarbutton.setOrient("vertical");
			cetakToolbarbutton.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					LaporanPendidikanLingkunganKampus lingkunganKampus = new LaporanPendidikanLingkunganKampus(
							formulirKegiatanPeserta);
					lingkunganKampus.setClosable(true);
					lingkunganKampus.setTitle("Formulir Kegiatan");
					lingkunganKampus.setWidth("90%");
					lingkunganKampus.setHeight("95%");
					lingkunganKampus.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
					lingkunganKampus.onModal();
				}
			});
			cetakToolbarbutton.setParent(toolbar);
			cetakToolbarbutton
					.setVisible(tbmuser != null && tbmuser.ambilGuru() == null && tbmuser.ambilDosen() == null);

			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(formulirKegiatanPeserta);
				}
			});
			cetakToolbarbuttonSertifikat.setParent(toolbar);

			deleteButton.setVisible(boleh && !formulirKegiatanPeserta.getAcc() && tbmuser != null
					&& tbmuser.ambilGuru() == null && tbmuser.ambilDosen() == null);
			deleteButton.setOrient("vertical");
			deleteButton.setTooltiptext("Hapus Data");
			deleteButton.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {

					MyMessageboxConfig.show("Apakah Anda yakin ingin menghapus data ini? Perlu diperhatikan, tindakan ini bersifat permanen dan data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(formulirKegiatanPeserta);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											MyMessageboxConfig.show(Common.pesan(
													"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang terkait; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala berlanjut.",
													e.getMessage()));
										}

									}

								}
							});

				}

			});
			deleteButton.setParent(toolbar);
			toolbar.setParent(row);

		}

	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Tbmuser tbmuser = Common.getCurrentUser();
		Guru guruPembina = tbmuser == null ? null : tbmuser.ambilGuru();

		List<Long> longs = new ArrayList<Long>();
		if (guruPembina != null) {
			longs = session.createCriteria(KelasSiswaPunyaSiswa.class).setProjection(Projections.property("siswa.id"))
					.createAlias("kelasSiswa", "kelasSiswa").add(Restrictions.eq("kelasSiswa.guruPembina", guruPembina))
					.list();

		}

		Criteria criteria = session.createCriteria(FormulirKegiatanPeserta.class)

				.add(Restrictions.or(Restrictions.isNotNull("siswa"),
						Restrictions.or(Restrictions.isNotNull("guru"),
								Restrictions.or(Restrictions.isNotNull("mahasiswa"), Restrictions.isNotNull("dosen")))))

				.add(longs == null || longs.isEmpty() ? Restrictions.sqlRestriction("true")
						: Restrictions.in("siswa.id", longs))

				.add(tampilAcc.isChecked() ? Restrictions.eq("acc", true) : Restrictions.sqlRestriction("true"))
				.add(tampilBelumAcc.isChecked()
						? Restrictions.or(Restrictions.eq("acc", false), Restrictions.isNull("acc"))
						: Restrictions.sqlRestriction("true"))

				.add(formulirKegiatan != null && !formulirKegiatan.getPesertaDosen() ? Restrictions.isNull("dosen")
						: Restrictions.sqlRestriction("1=1"))
				.add(formulirKegiatan != null && !formulirKegiatan.getPesertaMahasiswa()
						? Restrictions.isNull("mahasiswa")
						: Restrictions.sqlRestriction("1=1"))

				.add(formulirKegiatan != null && !formulirKegiatan.getPesertaSiswa() ? Restrictions.isNull("siswa")
						: Restrictions.sqlRestriction("1=1"))

				.add(formulirKegiatan != null && !formulirKegiatan.getPesertaGuru() ? Restrictions.isNull("guru")
						: Restrictions.sqlRestriction("1=1"))

		;

		if (formulirKegiatan == null) {
			criteria.createAlias("formulirKegiatan", "formulirKegiatan", Criteria.LEFT_JOIN);
		}

		criteria.createAlias("dosen", "dosen", Criteria.LEFT_JOIN)
				.createAlias("dosen.jurusan", "jurusan_dosen", Criteria.LEFT_JOIN)

				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa.jurusan", "jurusan", Criteria.LEFT_JOIN)

				.createAlias("siswa", "siswa", Criteria.LEFT_JOIN).createAlias("guru", "guru", Criteria.LEFT_JOIN)

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						|| searchjurusan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1") :

								Restrictions.or(
										CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false),
										CommonSearchFilterHelper.eqSelectedWithId("dosen.jurusan", searchjurusan, false))

				)

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null
								? Restrictions.sqlRestriction("1=1")
								: Restrictions.or(
										CommonSearchFilterHelper.eqSelectedWithId("jurusan_dosen.fakultas", searchfakultas, false),
										CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				)

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1") :

								Restrictions.or(
										CommonSearchFilterHelper.eqSelectedWithId("siswa.sekolah", searchsekolah, false),
										CommonSearchFilterHelper.eqSelectedWithId("guru.sekolah", searchsekolah, false))

				)

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1") :

								Restrictions.or(
										CommonSearchFilterHelper.eqSelectedWithId("siswa.yayasan", searchyayasan, false),
										CommonSearchFilterHelper.eqSelectedWithId("guru.yayasan", searchyayasan, false))

				)

				.add(

						nim.getValue().trim().isEmpty() ?

								Restrictions.sqlRestriction("1=1")

								: Restrictions.or(

										Restrictions.or(
												Restrictions.ilike("guru.namaGuru", nim.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("siswa.namaSiswa", nim.getValue().trim(),
																MatchMode.ANYWHERE),
														Restrictions.or(
																Restrictions.ilike("dosen.nidn", nim.getValue().trim(),
																		MatchMode.ANYWHERE),
																Restrictions.ilike("dosen.nama", nim.getValue().trim(),
																		MatchMode.ANYWHERE)))),
										Restrictions.or(
												Restrictions.ilike("mahasiswa.nim", nim.getValue().trim(),
														MatchMode.ANYWHERE),
												Restrictions.ilike("mahasiswa.nama", nim.getValue().trim(),
														MatchMode.ANYWHERE)))

				)

				.add(formulirKegiatan != null ? Restrictions.eq("formulirKegiatan", formulirKegiatan)
						: Restrictions.eq("formulirKegiatan.grupFormulirKegiatan", grupFormulirKegiatan));

		if (order)
			criteria.addOrder(Order.asc("kode"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<FormulirKegiatanPeserta> myFormulirKegiatanPesertas = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myFormulirKegiatanPesertas);
				grid.setRowRenderer(new DetailFormulirKegiatanRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(final FormulirKegiatan formulirKegiatan, final GrupFormulirKegiatan grupFormulirKegiatan,
			final Component component, final MyWindow window) {
		this.formulirKegiatan = formulirKegiatan;
		this.grupFormulirKegiatan = grupFormulirKegiatan;
		Common.clear(component);

		boolean[] ptYa = Common.chekPtAtauSekolah();
		boolean pt = ptYa[0];
		boolean ya = ptYa[1];

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);
		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Peserta : ")));
		toolbar.appendChild(nim = new Textbox());
		nim.setCols(10);
		nim.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		if (pt) {
			toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
			toolbar.appendChild(searchfakultas);
			searchfakultas.setCols(10);
			searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			Common.selectComboItem(searchfakultas, formulirKegiatan == null ? null : formulirKegiatan.getFakultas());
			if (formulirKegiatan != null && formulirKegiatan.getFakultas() != null) {
				searchfakultas.setDisabled(true);
			}

			toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
			toolbar.appendChild(searchjurusan);
			searchjurusan.setCols(10);
			searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}

			});
			button.setParent(toolbar);

			if (formulirKegiatan != null && formulirKegiatan.getJurusan() != null) {
				Fakultas selectedFakultas = (Fakultas) (searchfakultas.getSelectedItem() == null
						|| searchfakultas.getSelectedItem().getValue() == null
						|| searchfakultas.getSelectedItem().getValue() == null ? null
								: searchfakultas.getSelectedItem().getValue());
				if (selectedFakultas != null) {
					Common.insertComboDanSemua(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang",
							Jurusan.class, Restrictions.eq("fakultas", selectedFakultas));
					Common.selectComboItem(searchjurusan, formulirKegiatan.getJurusan());
					searchjurusan.setDisabled(true);
				}
			}

			button = new MyToolbarbuttonConfig("Ambil Peserta Mahasiswa", "/img/new.gif");
			button.setVisible(formulirKegiatan != null && formulirKegiatan.getPesertaMahasiswa() && tbmuser != null
					&& tbmuser.ambilDosen() == null && tbmuser.getMahasiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataMahasiswaForFormulirKegiatanHelper dataMahasiswaHelper = new AmbilDataMahasiswaForFormulirKegiatanHelper(
							formulirKegiatan);
					dataMahasiswaHelper.display(getDataloader(), window);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Ambil Peserta Dosen", "/img/new.gif");
			button.setVisible(formulirKegiatan != null && formulirKegiatan.getPesertaDosen());
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					Session session = HibernateUtil.currentSession();
					List<Dosen> daftarDosen = ConstantValues.simpleList(session
							.createCriteria(FormulirKegiatanPeserta.class)
							.add(Restrictions.eq("formulirKegiatan", formulirKegiatan))
							.setProjection(Projections.property("dosen.id")).add(Restrictions.isNotNull("dosen")),
							Dosen.class, false);

					AmbilDataDosenBanyak ambilDataDosenBanyak = new AmbilDataDosenBanyak(daftarDosen);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataDosenBanyak);
					ambilDataDosenBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Dosen> dosens = (List<Dosen>) arg0.getData();
							Session session = HibernateUtil.currentSession();
							Tbmuser tbmuser = Common.getCurrentUser();
							for (Dosen dosen : dosens) {

								if (formulirKegiatan.getGrupFormulirKegiatan() != null) {
									FormulirKegiatanPeserta kegiatanLainSatuGrup = ((FormulirKegiatanPeserta) session
											.createCriteria(FormulirKegiatanPeserta.class)
											.createAlias("formulirKegiatan", "formulirKegiatan")
											.add(Restrictions.eq("formulirKegiatan.grupFormulirKegiatan",
													formulirKegiatan.getGrupFormulirKegiatan()))
											.add(Restrictions.or(Restrictions.isNotNull("siswa"),
													Restrictions.or(Restrictions.isNotNull("guru"),
															Restrictions.or(Restrictions.isNotNull("mahasiswa"),
																	Restrictions.isNotNull("dosen")))))
											.add(Restrictions.ne("formulirKegiatan", formulirKegiatan))

											.add(Restrictions.eq("dosen", dosen))

											.setMaxResults(1).uniqueResult());
									if (dosen != null && kegiatanLainSatuGrup != null) {
											MyMessageboxConfig.showFormat(
												"Mohon maaf, dosen atas nama {V1} tidak dapat didaftarkan karena yang bersangkutan telah terdaftar pada kegiatan \"{V2}\". Langkah yang dapat dilakukan: (1) periksa keikutsertaan pada kegiatan lain dalam grup yang sama; (2) batalkan pendaftaran sebelumnya apabila diperlukan; (3) hubungi administrator untuk informasi lebih lanjut.",
												"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
												dosen.getNama(), kegiatanLainSatuGrup.getFormulirKegiatan().getNama());
										return;
									}
								}

								FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) session
										.createCriteria(FormulirKegiatanPeserta.class)
										.add(Restrictions.eq("dosen", dosen))
										.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).setMaxResults(1)
										.uniqueResult();
								if (formulirKegiatanPeserta == null) {
									formulirKegiatanPeserta = new FormulirKegiatanPeserta();
									int count = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
											.setProjection(Projections.rowCount())
											.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).uniqueResult())
											.intValue();
									count++;
									String kode = "0000000000000" + count;
									kode = kode.substring(kode.length() - 5);
									formulirKegiatanPeserta.setKode(kode);
								}

								formulirKegiatanPeserta.setFormulirKegiatan(formulirKegiatan);
								formulirKegiatanPeserta.setOleh(tbmuser.getUserId());
								formulirKegiatanPeserta.setDosen(dosen);
								Common.refreshSaveOrUpdate(session, formulirKegiatanPeserta);
							}

							loadData(null);
						}
					});
					ambilDataDosenBanyak.setWidth("850px");
					ambilDataDosenBanyak.setHeight("97%");
					ambilDataDosenBanyak.setVisible(true);
					ambilDataDosenBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			List<String> columnHeadersAdding = new ArrayList<String>();
			columnHeadersAdding.add("Bukti");
			columnHeadersAdding.add("Link Bukti");

			final String[] contents = new String[] { "id", "kode", "mahasiswa.nim", "mahasiswa.nama",
					"mahasiswa.jurusan.nama", "dosen", "dosen.nama", "dosen.jurusan.nama", "nilai", "keterangan", "acc",
					"mahasiswa.telp", "mahasiswa.email", "dosen.telp", "dosen.email" };

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) objects[0];

					XSSFRow row = (XSSFRow) objects[2];
					final XSSFCellStyle bodystyle = (XSSFCellStyle) objects[6];
					final XSSFCellStyle hlink_style = (XSSFCellStyle) objects[7];

					class DataAddingHelper {
						public void process(XSSFRow row, int index, FormulirKegiatanPeserta formulirKegiatanPeserta,
								String jenis) throws Exception {
							LampiranLain lam = LampiranLain.ambil(formulirKegiatanPeserta.getId(), jenis);
							XSSFCell cell = row.createCell(index);
							XSSFCell cell1 = row.createCell(index + 1);

							if (lam != null) {

								String nama = lam.getNama();
								cell.setCellStyle(bodystyle);
								cell.setCellValue(nama);
								String url = lam.createLinkUri(false);

								cell1.setCellStyle(bodystyle);
								cell1.setCellStyle(hlink_style);
								cell1.setCellValue(url);
							}

						}
					}

					DataAddingHelper dataAddingHelper = new DataAddingHelper();

					dataAddingHelper.process(row, contents.length, formulirKegiatanPeserta,
							FormulirKegiatanPeserta.class.getName());

				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(FormulirKegiatanPeserta.class, this,
					"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);

			toolbar.appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = Common.uploadData(this, FormulirKegiatanPeserta.class, new EventListener() {

				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] data = (Object[]) arg0.getData();
					FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) data[0];
					formulirKegiatanPeserta.setFormulirKegiatan(formulirKegiatan);
					Map datum = (Map) data[2];
					System.out.println("datum -> " + datum);
					if (datum.get("siswa.nomorInduk") != null
							&& !datum.get("siswa.nomorInduk").toString().trim().isEmpty()
							&& (datum.get("siswa.nim") instanceof String)) {
						Siswa siswa = ConstantValues.ambilByNis(datum.get("siswa.nomorInduk").toString());
						System.out.println("siswa -> " + siswa);
						formulirKegiatanPeserta.setSiswa(siswa);
					}
				}
			}, contents);
			upload.setVisible(formulirKegiatan != null
					&& (tbmuser != null && tbmuser.getMahasiswa() == null && tbmuser.ambilDosen() == null));
			toolbar.appendChild(upload);

		} else if (ya) {

			toolbar.appendChild(new Label(Common.getBahasaConfig("Yayasan") + " : "));
			toolbar.appendChild(searchyayasan);
			searchyayasan.setCols(10);
			searchyayasan.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			Common.selectComboItem(true, searchsekolah,
					formulirKegiatan == null ? null : formulirKegiatan.getSekolah());
			if (formulirKegiatan != null && formulirKegiatan.getSekolah() != null) {
				searchsekolah.setDisabled(true);
			}

			toolbar.appendChild(new Label(Common.getBahasaConfig("Sekolah") + " : "));
			toolbar.appendChild(searchsekolah);
			searchsekolah.setCols(10);
			searchsekolah.addEventListener(Events.ON_CHANGE, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}
			});

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cari", "/img/svg/search.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					loadData(null);
				}

			});
			button.setParent(toolbar);

			if (formulirKegiatan != null && formulirKegiatan.getYayasan() != null) {
				Yayasan selectedYayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
						|| searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? null
								: searchyayasan.getSelectedItem().getValue());
				if (selectedYayasan != null) {
					Common.insertComboDanSemua(searchsekolah, new String[] { "nama" }, "jenisSekolah", Sekolah.class,
							Restrictions.eq("yayasan", selectedYayasan));
					Common.selectComboItem(searchsekolah, formulirKegiatan.getSekolah());
					searchsekolah.setDisabled(true);
				}
			}

			button = new MyToolbarbuttonConfig("Ambil Peserta Siswa", "/img/new.gif");
			button.setVisible(formulirKegiatan != null && formulirKegiatan.getPesertaSiswa() && tbmuser != null
					&& tbmuser.getSiswa() == null);
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					AmbilDataSiswaForFormulirKegiatanHelper dataMahasiswaHelper = new AmbilDataSiswaForFormulirKegiatanHelper(
							formulirKegiatan);
					dataMahasiswaHelper.display(getDataloader(), window);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("Ambil Peserta Guru", "/img/new.gif");
			button.setVisible(formulirKegiatan != null && formulirKegiatan.getPesertaGuru());
			button.addEventListener("onClick", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {

					Session session = HibernateUtil.currentSession();
					List<Guru> daftarGuru = ConstantValues.simpleList(
							session.createCriteria(FormulirKegiatanPeserta.class)
									.add(Restrictions.eq("formulirKegiatan", formulirKegiatan))
									.setProjection(Projections.property("guru.id")).add(Restrictions.isNotNull("guru")),
							Guru.class, false);

					AmbilDataGuruBanyak ambilDataGuruBanyak = new AmbilDataGuruBanyak(daftarGuru);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(ambilDataGuruBanyak);
					ambilDataGuruBanyak.setEventListener(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							List<Guru> gurus = (List<Guru>) arg0.getData();
							Session session = HibernateUtil.currentSession();
							Tbmuser tbmuser = Common.getCurrentUser();
							for (Guru guru : gurus) {

								if (formulirKegiatan.getGrupFormulirKegiatan() != null) {
									FormulirKegiatanPeserta kegiatanLainSatuGrup = ((FormulirKegiatanPeserta) session
											.createCriteria(FormulirKegiatanPeserta.class)
											.createAlias("formulirKegiatan", "formulirKegiatan")
											.add(Restrictions.eq("formulirKegiatan.grupFormulirKegiatan",
													formulirKegiatan.getGrupFormulirKegiatan()))
											.add(Restrictions.or(Restrictions.isNotNull("siswa"),
													Restrictions.or(Restrictions.isNotNull("guru"),
															Restrictions.or(Restrictions.isNotNull("mahasiswa"),
																	Restrictions.isNotNull("dosen")))))
											.add(Restrictions.ne("formulirKegiatan", formulirKegiatan))

											.add(Restrictions.eq("guru", guru))

											.setMaxResults(1).uniqueResult());
									if (guru != null && kegiatanLainSatuGrup != null) {
											MyMessageboxConfig.showFormat(
												"Mohon maaf, guru atas nama {V1} tidak dapat didaftarkan karena yang bersangkutan telah terdaftar pada kegiatan \"{V2}\". Langkah yang dapat dilakukan: (1) periksa keikutsertaan pada kegiatan lain dalam grup yang sama; (2) batalkan pendaftaran sebelumnya apabila diperlukan; (3) hubungi administrator untuk informasi lebih lanjut.",
												"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.EXCLAMATION,
												guru.getNama(), kegiatanLainSatuGrup.getFormulirKegiatan().getNama());
										return;
									}
								}

								FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) session
										.createCriteria(FormulirKegiatanPeserta.class)
										.add(Restrictions.eq("guru", guru))
										.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).setMaxResults(1)
										.uniqueResult();
								if (formulirKegiatanPeserta == null) {
									formulirKegiatanPeserta = new FormulirKegiatanPeserta();
									int count = ((Number) session.createCriteria(FormulirKegiatanPeserta.class)
											.setProjection(Projections.rowCount())
											.add(Restrictions.eq("formulirKegiatan", formulirKegiatan)).uniqueResult())
											.intValue();
									count++;
									String kode = "0000000000000" + count;
									kode = kode.substring(kode.length() - 5);
									formulirKegiatanPeserta.setKode(kode);
								}

								formulirKegiatanPeserta.setFormulirKegiatan(formulirKegiatan);
								formulirKegiatanPeserta.setOleh(tbmuser.getUserId());
								formulirKegiatanPeserta.setGuru(guru);
								Common.refreshSaveOrUpdate(session, formulirKegiatanPeserta);
							}

							loadData(null);
						}
					});
					ambilDataGuruBanyak.setWidth("850px");
					ambilDataGuruBanyak.setHeight("97%");
					ambilDataGuruBanyak.setVisible(true);
					ambilDataGuruBanyak.onModal();
				}

			});
			button.setParent(toolbar);

			List<String> columnHeadersAdding = new ArrayList<String>();
			columnHeadersAdding.add("Bukti");
			columnHeadersAdding.add("IPS");
			columnHeadersAdding.add("IPK");
			columnHeadersAdding.add("SKS");
			columnHeadersAdding.add("SKSK");

			final String[] contents = new String[] { "id", "kode", "siswa.nomorInduk", "siswa.namaSiswa",
					"siswa.sekolah.nama", "guru", "guru.namaGuru", "guru.sekolah.nama", "nilai", "keterangan", "acc",
					"siswa.teleponSiswa", "siswa.alamatEmail", "guru.teleponGuru", "guru.alamatEmail" };

			EventListener dataAdding = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] objects = (Object[]) arg0.getData();
					FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) objects[0];

					XSSFRow row = (XSSFRow) objects[2];
					XSSFWorkbook workbook = (XSSFWorkbook) objects[3];
					XSSFFont hlink_font = workbook.createFont();
					hlink_font.setUnderline(XSSFFont.U_SINGLE);
					hlink_font.setColor(new XSSFColor(Color.BLUE));

					final XSSFCellStyle hlink_style = workbook.createCellStyle();
					hlink_style.setFillPattern(XSSFCellStyle.SOLID_FOREGROUND);
					hlink_style.setFillForegroundColor(new XSSFColor(Color.LIGHT_GRAY));
					hlink_style.setFont(hlink_font);

					class DataAddingHelper {
						public void process(XSSFRow row, int index, FormulirKegiatanPeserta formulirKegiatanPeserta,
								String jenis) throws Exception {
							LampiranLain lam = LampiranLain.ambil(formulirKegiatanPeserta.getId(), jenis);
							XSSFCell cell = row.createCell(index);

							if (lam != null) {

								String nama = lam.getNama();

								cell.setCellStyle(hlink_style);
								cell.setCellValue(nama);
								String url = lam.createLinkUri();
								XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper()
										.createHyperlink(Hyperlink.LINK_URL);
								link.setAddress(url);
								cell.setHyperlink(link);
							}

						}
					}

					DataAddingHelper dataAddingHelper = new DataAddingHelper();

					dataAddingHelper.process(row, contents.length, formulirKegiatanPeserta,
							FormulirKegiatanPeserta.class.getName());

					if (formulirKegiatanPeserta.getMahasiswa() != null) {
						KrsMahasiswa krsMahasiswa = Common
								.singkronkanKrsMahasiswa(formulirKegiatanPeserta.getMahasiswa());

						XSSFCell cell = row.createCell(contents.length + 1);
						cell.setCellValue(krsMahasiswa.getIps());

						cell = row.createCell(contents.length + 2);
						cell.setCellValue(krsMahasiswa.getIpk());

						cell = row.createCell(contents.length + 3);
						cell.setCellValue(krsMahasiswa.getSksYangDiambil());

						cell = row.createCell(contents.length + 4);
						cell.setCellValue(krsMahasiswa.getSksk());
					}

				}
			};

			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(FormulirKegiatanPeserta.class, this,
					"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);

			toolbar.appendChild(cetakToolbarbutton);

			MyToolbarbuttonConfig upload = Common.uploadData(this, FormulirKegiatanPeserta.class, new EventListener() {

				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Object[] data = (Object[]) arg0.getData();
					FormulirKegiatanPeserta formulirKegiatanPeserta = (FormulirKegiatanPeserta) data[0];
					formulirKegiatanPeserta.setFormulirKegiatan(formulirKegiatan);
					Map datum = (Map) data[2];
					System.out.println("datum -> " + datum);
					if (datum.get("mahasiswa.nim") != null && !datum.get("mahasiswa.nim").toString().trim().isEmpty()
							&& (datum.get("mahasiswa.nim") instanceof String)) {
						Mahasiswa mahasiswa = ConstantValues.ambilByNim(datum.get("mahasiswa.nim").toString());
						System.out.println("mahasiswa -> " + mahasiswa);
						formulirKegiatanPeserta.setMahasiswa(mahasiswa);
					}
				}
			}, contents);
			upload.setVisible(
					formulirKegiatan != null && (Common.getApakahAdmin() || Common.getApakahAdminLain()
							|| ((tbmuser.ambilGuru() == null || (formulirKegiatan.getGuruPembina() != null
									&& tbmuser.ambilGuru() != null
									&& tbmuser.getGuru().getId().equals(formulirKegiatan.getGuruPembina().getId()))))));
			toolbar.appendChild(upload);

		}

		MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("Cetak Kartu Peserta", "/img/print.png");
		button.setVisible(formulirKegiatan != null && tbmuser != null && tbmuser.ambilDosen() == null
				&& tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@SuppressWarnings("unchecked")
			@Override
			public void onEvent(Event event) throws Exception {
				List<FormulirKegiatanPeserta> formulirKegiatanPesertas = initCriteria(true).list();
				SertifikatAction.cetakFormPendafatranKegiatan(formulirKegiatan, formulirKegiatanPesertas);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.setVisible(formulirKegiatan != null && tbmuser != null && tbmuser.ambilDosen() == null
				&& tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah Anda yakin ingin menghapus SELURUH data ini? Perlu diperhatikan, tindakan ini bersifat permanen dan seluruh data yang telah dihapus tidak dapat dikembalikan.", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery(
												"delete from formulir_kegiatan_peserta where formulir_kegiatan = "
														+ formulirKegiatan.getId())
												.executeUpdate();

										loadData(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										MyMessageboxConfig.show(Common.pesan(
												"Mohon maaf, data ini tidak dapat dihapus karena masih berelasi dengan data lain. Rincian teknis kesalahan: {V1}. Langkah yang dapat dilakukan: (1) hapus atau lepaskan terlebih dahulu data lain yang terkait; (2) periksa kembali keterkaitan data; (3) hubungi administrator apabila kendala berlanjut.",
												e.getMessage()));
									}

								}

							}
						});

			}

		});
		button.setParent(toolbar);

		MyToolbarbuttonConfig ajukan = new MyToolbarbuttonConfig("Singkronkan dg Kegiatan", "/img/print.png");
		ajukan.setVisible(formulirKegiatan != null && tbmuser != null && tbmuser.ambilDosen() == null
				&& tbmuser.ambilGuru() == null && tbmuser.getSiswa() == null && tbmuser.getMahasiswa() == null
				&& (formulirKegiatan.getKegiatanKemahasiswaan() != null
						|| formulirKegiatan.getKegiatanKedosenan() != null));
		ajukan.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {

				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				});

				final Tbmuser tbmuser = Common.getCurrentUser();

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						List<FormulirKegiatanPeserta> formulirKegiatanPesertas = initCriteria(true)
								.add(Restrictions.eq("acc", true)).add(Restrictions.isNotNull("mahasiswa")).list();
						int i = 0;
						int size = formulirKegiatanPesertas.size();
						Session session = HibernateUtil.currentNativeSession();
						// Thread latar TIDAK lewat FilterJSP -> WAJIB tutup native session sendiri (cegah bocor c3p0).
						try {
							for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
								label.setValue("Sedang memproses data " + formulirKegiatanPeserta.getMahasiswa() + " ( "
										+ Common.numberFormat.get().format(i * 100.0 / size) + " %)");
								KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) session
										.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class)
										.add(Restrictions.eq("kegiatanKemahasiswaan",
												formulirKegiatan.getKegiatanKemahasiswaan()))
										.add(Restrictions.eq("mahasiswa", formulirKegiatanPeserta.getMahasiswa()))
										.setMaxResults(1).uniqueResult();
								if (kegiatanKemahasiswaanPunyaMahasiswa == null) {
									kegiatanKemahasiswaanPunyaMahasiswa = new KegiatanKemahasiswaanPunyaMahasiswa();
									kegiatanKemahasiswaanPunyaMahasiswa.setDiubahDari(
											FormulirKegiatanPesertaHelper.class.getName() + " oleh " + tbmuser.getUserId());
									kegiatanKemahasiswaanPunyaMahasiswa
											.setMahasiswa(formulirKegiatanPeserta.getMahasiswa());
									kegiatanKemahasiswaanPunyaMahasiswa.setKegiatanKemahasiswaan(
											formulirKegiatanPeserta.getFormulirKegiatan().getKegiatanKemahasiswaan());
									kegiatanKemahasiswaanPunyaMahasiswa.setTbmuser(tbmuser);
									kegiatanKemahasiswaanPunyaMahasiswa
											.setMulai(formulirKegiatanPeserta.getFormulirKegiatan().getMulai());
									kegiatanKemahasiswaanPunyaMahasiswa
											.setSampai(formulirKegiatanPeserta.getFormulirKegiatan().getSampai());
									kegiatanKemahasiswaanPunyaMahasiswa.setPersetujuan(true);
									session.getTransaction().begin();
									session.save(kegiatanKemahasiswaanPunyaMahasiswa);
									session.getTransaction().commit();
								}
							}
							formulirKegiatanPesertas = null;

							formulirKegiatanPesertas = initCriteria(true).add(Restrictions.eq("acc", true))
									.add(Restrictions.isNotNull("dosen")).list();
							i = 0;
							size = formulirKegiatanPesertas.size();
							session = HibernateUtil.currentNativeSession();
							for (FormulirKegiatanPeserta formulirKegiatanPeserta : formulirKegiatanPesertas) {
								label.setValue("Sedang memproses data " + formulirKegiatanPeserta.getDosen() + " ( "
										+ Common.numberFormat.get().format(i * 100.0 / size) + " %)");
								KegiatanKedosenanPunyaDosen kegiatanKedosenanPunyaDosen = (KegiatanKedosenanPunyaDosen) session
										.createCriteria(KegiatanKedosenanPunyaDosen.class)
										.add(Restrictions.eq("kegiatanKedosenan", formulirKegiatan.getKegiatanKedosenan()))
										.add(Restrictions.eq("dosen", formulirKegiatanPeserta.getDosen())).setMaxResults(1)
										.uniqueResult();
								if (kegiatanKedosenanPunyaDosen == null) {
									kegiatanKedosenanPunyaDosen = new KegiatanKedosenanPunyaDosen();
									kegiatanKedosenanPunyaDosen.setDiubahDari(
											FormulirKegiatanPesertaHelper.class.getName() + " oleh " + tbmuser.getUserId());
									kegiatanKedosenanPunyaDosen.setDosen(formulirKegiatanPeserta.getDosen());
									kegiatanKedosenanPunyaDosen.setKegiatanKedosenan(
											formulirKegiatanPeserta.getFormulirKegiatan().getKegiatanKedosenan());
									kegiatanKedosenanPunyaDosen.setTbmuser(tbmuser);
									kegiatanKedosenanPunyaDosen
											.setMulai(formulirKegiatanPeserta.getFormulirKegiatan().getMulai());
									kegiatanKedosenanPunyaDosen
											.setSampai(formulirKegiatanPeserta.getFormulirKegiatan().getSampai());
									kegiatanKedosenanPunyaDosen.setPersetujuan(true);
									session.getTransaction().begin();
									session.save(kegiatanKedosenanPunyaDosen);
									session.getTransaction().commit();
								}
							}
							formulirKegiatanPesertas = null;

							label.setValue("");
						} finally {
							try { session.clear(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/helper/FormulirKegiatanPesertaHelper.java:1204");}
							try { session.disconnect(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/helper/FormulirKegiatanPesertaHelper.java:1205");}
							try { session.close(); } catch (Exception eSes) { ais.common.ErrorAuditUtil.record(eSes, "auto-audit(empty-catch) src/ais/action/master/helper/FormulirKegiatanPesertaHelper.java:1206");}
						}
					}
				}).start();

			}
		});
		ajukan.setParent(toolbar);

		tampilAcc = new MyCheckboxConfig("Tampilkan di acc");
		tampilAcc.setParent(toolbar);
		tampilAcc.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
		tampilBelumAcc = new MyCheckboxConfig("Belum di acc");
		tampilBelumAcc.setParent(toolbar);
		tampilBelumAcc.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(50);
		grid.getPagingChild().setMold("os");
		grid.setParent(groupbox);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Peserta");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Waktu daftar");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel(pt ? "Jurusan" : "Sekolah");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nilai");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("ACC");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("12%");

		loadData(null);

	}

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
