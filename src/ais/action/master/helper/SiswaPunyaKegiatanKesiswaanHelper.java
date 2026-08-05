package ais.action.master.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
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
import org.zkoss.zul.A;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.SertifikatAction;
import ais.action.master.sekolah.KegiatanKesiswaanAction;
import ais.action.master.sekolah.helper.AmbilDataKegiatanForKegiatanKesiswaanHelper;
import ais.action.report.CommonReportHelper;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.PesanFormalHelper;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.DetailKelompokKegiatanKesiswaan;
import ais.database.model.sekolah.JabatanKegiatanKesiswaan;
import ais.database.model.sekolah.KegiatanKesiswaan;
import ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa;
import ais.database.model.sekolah.KelompokKegiatanKesiswaan;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.SkalaKegiatanKesiswaan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelAgakKecil;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

public class SiswaPunyaKegiatanKesiswaanHelper implements DataLoader, DataCriteria {

	private MyGrid grid;
	private Siswa siswa;
	private Textbox nama;

	private Paging paging;
	private Tbmuser tbmuser;
	private KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan = null;
	private DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan = null;
	private JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan = null;
	private SkalaKegiatanKesiswaan skalaKegiatanKesiswaan = null;
	private String tahunAkademik = null;
	private KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa;

	public SiswaPunyaKegiatanKesiswaanHelper() {

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	public SiswaPunyaKegiatanKesiswaanHelper(KelompokKegiatanKesiswaan kelompokKegiatanKesiswaan,
			DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan,
			JabatanKegiatanKesiswaan jabatanKegiatanKesiswaan,
			SkalaKegiatanKesiswaan skalaKegiatanKesiswaan, String tahunAkademik) {

		this.kelompokKegiatanKesiswaan = kelompokKegiatanKesiswaan;
		this.detailKelompokKegiatanKesiswaan = detailKelompokKegiatanKesiswaan;
		this.jabatanKegiatanKesiswaan = jabatanKegiatanKesiswaan;
		this.skalaKegiatanKesiswaan = skalaKegiatanKesiswaan;
		this.tahunAkademik = tahunAkademik;

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});
	}

	class DetailSiswaRenderer extends ais.ui.util.MyRowRenderer {

		public DetailSiswaRenderer() {

		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa = (KegiatanKesiswaanPunyaSiswa) data;
			final KegiatanKesiswaan kegiatanKesiswaan = kegiatanKesiswaanPunyaSiswa
					.getKegiatanKesiswaan();

			try {
				if (SiswaPunyaKegiatanKesiswaanHelper.this.kegiatanKesiswaanPunyaSiswa != null
						&& SiswaPunyaKegiatanKesiswaanHelper.this.kegiatanKesiswaanPunyaSiswa.getId()
								.equals(kegiatanKesiswaanPunyaSiswa.getId())) {
					row.setStyle("background-color:yellow");
				}
			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/SiswaPunyaKegiatanKesiswaanHelper.java:139");
				// TODO: handle exception
			}

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			Vbox vbox = new Vbox();
			vbox.setParent(row);
			A a = CommonMedia.tampilkanGambarKecil(kegiatanKesiswaanPunyaSiswa.getSiswa());
			a.setParent(vbox);
			vbox.appendChild(new MyLabelAgakKecil(kegiatanKesiswaanPunyaSiswa.getSiswa().getNama()));
			vbox.appendChild(new MyLabelAgakKecil(kegiatanKesiswaanPunyaSiswa.getSiswa().getNim()));
			vbox.appendChild(
					new MyLabelAgakKecil(kegiatanKesiswaanPunyaSiswa.getSiswa().getSekolah().getNama()));

			Vbox aa = RevisiHelper.createNewRevisi(KegiatanKesiswaanPunyaSiswa.class,
					kegiatanKesiswaanPunyaSiswa,
					kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getNama());
			aa.setParent(row);
			aa.appendChild(new MyLabelAgakKecil(kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan()
					.getKelompokKegiatanKesiswaan().getNama()));
			aa.appendChild(new MyLabelAgakKecil(kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan()
					.getDetailKelompokKegiatanKesiswaan().getNama()));
			aa.appendChild(new MyLabelAgakKecil(
					kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getTahunAkademik() + "/"
							+ kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getJenisSemester()));

			vbox = new Vbox();
			vbox.setParent(detail);

			boolean bolehEdit = tbmuser != null && tbmuser.getSiswa() != null
					&& tbmuser.getSiswa().getId().equals(kegiatanKesiswaanPunyaSiswa.getSiswa().getId())
					&& !kegiatanKesiswaanPunyaSiswa.getPersetujuan();

			Hbox hbox = new Hbox();
			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKesiswaanPunyaSiswa.getId(),
					KegiatanKesiswaanPunyaSiswa.class.getName(), "Bukti Kegiatan Siswa", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, bolehEdit, null);
			hbox.setParent(vbox);

			Hbox toolbar = new Hbox();
			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			cetakToolbarbuttonSertifikat.setOrient("vertical");
			cetakToolbarbuttonSertifikat.setVisible(kegiatanKesiswaanPunyaSiswa.getPersetujuan()
					&& kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getSertifikat() != null);

			if (bolehEdit) {

				final MyTextbox keterangan = new MyTextbox(kegiatanKesiswaanPunyaSiswa.getKeterangan());
				keterangan.setWidth("90%");
				keterangan.setRows(2);

				final MyDatebox mulai = new MyDatebox(kegiatanKesiswaanPunyaSiswa.getMulai());
				mulai.setWidth("90%");
				final MyDatebox sampai = new MyDatebox(kegiatanKesiswaanPunyaSiswa.getSampai());
				sampai.setWidth("90%");

				mulai.setParent(row);
				sampai.setParent(row);

				DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan = (DetailKelompokKegiatanKesiswaan) HibernateUtil
						.currentSession().createCriteria(DetailKelompokKegiatanKesiswaan.class)
						.add(Restrictions.idEq(kegiatanKesiswaan.getDetailKelompokKegiatanKesiswaan().getId()))
						.uniqueResult();
				List<JabatanKegiatanKesiswaan> jabatanKegiatanKesiswaans = new ArrayList<JabatanKegiatanKesiswaan>(
						detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans());
				List<SkalaKegiatanKesiswaan> skalaKegiatanKesiswaans = new ArrayList<SkalaKegiatanKesiswaan>(
						detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans());

				Collections.sort(jabatanKegiatanKesiswaans);
				Collections.sort(skalaKegiatanKesiswaans);

				final Combobox jabatanKegiatanKesiswaan = new Combobox();
				jabatanKegiatanKesiswaan.setVisible(!jabatanKegiatanKesiswaans.isEmpty());
				Common.insertComboItems(jabatanKegiatanKesiswaan, "nama", jabatanKegiatanKesiswaans);
				Common.selectComboItem(jabatanKegiatanKesiswaan,
						kegiatanKesiswaanPunyaSiswa.getJabatanKegiatanKesiswaan());
				jabatanKegiatanKesiswaan.setParent(row);
				jabatanKegiatanKesiswaan.setReadonly(true);
				jabatanKegiatanKesiswaan.setWidth("97%");

				final Combobox skalaKegiatanKesiswaan = new Combobox();
				skalaKegiatanKesiswaan.setVisible(!skalaKegiatanKesiswaans.isEmpty());
				Common.insertComboItems(skalaKegiatanKesiswaan, "nama", skalaKegiatanKesiswaans);
				Common.selectComboItem(skalaKegiatanKesiswaan,
						kegiatanKesiswaanPunyaSiswa.getSkalaKegiatanKesiswaan());
				skalaKegiatanKesiswaan.setParent(row);
				skalaKegiatanKesiswaan.setReadonly(true);
				skalaKegiatanKesiswaan.setWidth("97%");

				EventListener eventListener = new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatanKesiswaanPunyaSiswa.setMulai(mulai.getValue());
						kegiatanKesiswaanPunyaSiswa.setSampai(sampai.getValue());
						kegiatanKesiswaanPunyaSiswa.setSkalaKegiatanKesiswaan(
								(SkalaKegiatanKesiswaan) (skalaKegiatanKesiswaan.getSelectedItem() == null
										? null
										: skalaKegiatanKesiswaan.getSelectedItem().getValue()));
						kegiatanKesiswaanPunyaSiswa.setKeterangan(keterangan.getValue());
						kegiatanKesiswaanPunyaSiswa.setJabatanKegiatanKesiswaan(
								((JabatanKegiatanKesiswaan) (jabatanKegiatanKesiswaan.getSelectedItem() == null
										? null
										: jabatanKegiatanKesiswaan.getSelectedItem().getValue())));
						Common.refreshUpdate(kegiatanKesiswaanPunyaSiswa);

					}
				};

				skalaKegiatanKesiswaan.addEventListener("onChange", eventListener);
				jabatanKegiatanKesiswaan.addEventListener("onChange", eventListener);
				keterangan.addEventListener("onChange", eventListener);
				mulai.addEventListener("onChange", eventListener);
				sampai.addEventListener("onChange", eventListener);
				keterangan.setParent(row);

				final MyToolbarbuttonConfig buttonDelete = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");

				buttonDelete.setVisible(!kegiatanKesiswaanPunyaSiswa.getPersetujuan());
				jabatanKegiatanKesiswaan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
				skalaKegiatanKesiswaan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
				keterangan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
				mulai.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
				sampai.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
				if (tbmuser.getSiswa() == null) {
					final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
					checkbox.setChecked(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
					checkbox.setParent(row);
					row.setValign("top");row.setAttribute("checkbox", checkbox);
					checkbox.addEventListener("onCheck", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							kegiatanKesiswaanPunyaSiswa.setPersetujuan(checkbox.isChecked());
							Common.refreshSaveOrUpdate(kegiatanKesiswaanPunyaSiswa);
							buttonDelete.setVisible(!kegiatanKesiswaanPunyaSiswa.getPersetujuan());

							jabatanKegiatanKesiswaan
									.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
							skalaKegiatanKesiswaan
									.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
							keterangan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
							mulai.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
							sampai.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());

							cetakToolbarbuttonSertifikat.setVisible(kegiatanKesiswaanPunyaSiswa.getPersetujuan()
									&& kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan()
											.getSertifikat() != null);
						}
					});
				} else {
					Label label;
					(label = new Label(kegiatanKesiswaanPunyaSiswa.getPersetujuan() == null
							|| kegiatanKesiswaanPunyaSiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
					label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
					label.setParent(row);
				}

				buttonDelete.setOrient("vertical");
				buttonDelete.setVisible(!kegiatanKesiswaanPunyaSiswa.getPersetujuan());
				buttonDelete.setTooltiptext("Hapus Data");
				buttonDelete.addEventListener("onClick", new EventListener() {
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
												
												try {
													if (SiswaPunyaKegiatanKesiswaanHelper.this.kegiatanKesiswaanPunyaSiswa != null
															&& SiswaPunyaKegiatanKesiswaanHelper.this.kegiatanKesiswaanPunyaSiswa.getId()
																	.equals(kegiatanKesiswaanPunyaSiswa.getId())) {
														SiswaPunyaKegiatanKesiswaanHelper.this.kegiatanKesiswaanPunyaSiswa = null;
													}
												} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/helper/SiswaPunyaKegiatanKesiswaanHelper.java:330");
													// TODO: handle exception
												}

												Common.refreshDelete(kegiatanKesiswaanPunyaSiswa);
												loadData(null);

											} catch (Exception e) {
												Common.tampilErrorJikaAdmin(e);
												PesanFormalHelper.tampilkanGagalException(
														"menghapus data keikutsertaan siswa pada kegiatan kesiswaan ini",
														e,
														new String[] {
																"Periksa apakah data ini masih berelasi dengan data lain (misalnya data penilaian atau absensi kegiatan) sehingga tidak dapat dihapus.",
																"Hapus atau lepaskan terlebih dahulu data terkait yang masih berelasi, lalu ulangi proses penghapusan.",
																"Jika data tetap tidak dapat dihapus, konfirmasikan kebutuhan penghapusan ini kepada Administrator." });
											}

										}

									}
								});

					}

				});
				buttonDelete.setParent(toolbar);

				jabatanKegiatanKesiswaans = null;
				skalaKegiatanKesiswaans = null;
			} else {
				new Label(kegiatanKesiswaanPunyaSiswa.getMulai() == null ? ""
						: Common.dateFormat1.get().format(kegiatanKesiswaanPunyaSiswa.getMulai())).setParent(row);
				new Label(kegiatanKesiswaanPunyaSiswa.getSampai() == null ? ""
						: Common.dateFormat1.get().format(kegiatanKesiswaanPunyaSiswa.getSampai())).setParent(row);
				new Label(kegiatanKesiswaanPunyaSiswa.getJabatanKegiatanKesiswaan() == null ? ""
						: kegiatanKesiswaanPunyaSiswa.getJabatanKegiatanKesiswaan().getNama())
								.setParent(row);
				new Label(kegiatanKesiswaanPunyaSiswa.getSkalaKegiatanKesiswaan() == null ? ""
						: kegiatanKesiswaanPunyaSiswa.getSkalaKegiatanKesiswaan().getNama()).setParent(row);
				new Label(kegiatanKesiswaanPunyaSiswa.getKeterangan()).setParent(row);
				Label label;
				(label = new Label(kegiatanKesiswaanPunyaSiswa.getPersetujuan() == null
						|| kegiatanKesiswaanPunyaSiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

			toolbar.setParent(row);
			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(kegiatanKesiswaanPunyaSiswa);
				}
			});
			cetakToolbarbuttonSertifikat.setParent(toolbar);
		}

	}

	public Criteria initCriteria(boolean order) {
		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKesiswaanPunyaSiswa.class);

		criteria.createAlias("kegiatanKesiswaan", "kegiatanKesiswaan")

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("kegiatanKesiswaan.nama", nama.getValue().trim(), MatchMode.ANYWHERE))

				.add(siswa == null ? Restrictions.sqlRestriction("true") : Restrictions.eq("siswa", siswa))

				.add(kelompokKegiatanKesiswaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKesiswaan.kelompokKegiatanKesiswaan",
								kelompokKegiatanKesiswaan))

				.add(detailKelompokKegiatanKesiswaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKesiswaan.detailKelompokKegiatanKesiswaan",
								detailKelompokKegiatanKesiswaan))

				.add(jabatanKegiatanKesiswaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("jabatanKegiatanKesiswaan", jabatanKegiatanKesiswaan))

				.add(skalaKegiatanKesiswaan == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("skalaKegiatanKesiswaan", skalaKegiatanKesiswaan))

				.add(tahunAkademik == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("kegiatanKesiswaan.tahunAkademik", tahunAkademik));

		if (order)
			criteria.addOrder(Order.desc("id"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<KegiatanKesiswaanPunyaSiswa> myKegiatanKesiswaanPunyaSiswas;

				if (kegiatanKesiswaanPunyaSiswa != null) {
					myKegiatanKesiswaanPunyaSiswas = new ArrayList<KegiatanKesiswaanPunyaSiswa>();
					myKegiatanKesiswaanPunyaSiswas.add(kegiatanKesiswaanPunyaSiswa);
					myKegiatanKesiswaanPunyaSiswas.addAll(initCriteria(true)
							.add(Restrictions.ne("id", kegiatanKesiswaanPunyaSiswa.getId()))
							.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list());
				} else {
					myKegiatanKesiswaanPunyaSiswas = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
							.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
							.list();
				}

				ListModel strset = new SimpleListModel(myKegiatanKesiswaanPunyaSiswas);
				grid.setRowRenderer(new DetailSiswaRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(Siswa siswa, Component component) {
		display(siswa, component, null);
	}

	public void display(final Siswa siswa, Component component,
			KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa) {
		this.siswa = siswa;
		this.kegiatanKesiswaanPunyaSiswa = kegiatanKesiswaanPunyaSiswa;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(Common.tampilanScroll(component));

		final boolean mobileTampil = Common.isMobile();
		org.zkoss.zk.ui.HtmlBasedComponent toolbar;
		if (mobileTampil) {
			org.zkoss.zul.Div barMobile = new org.zkoss.zul.Div();
			barMobile.setStyle("display:flex;flex-wrap:wrap;align-items:center;gap:6px;padding:6px 4px;width:100%;box-sizing:border-box;");
			toolbar = barMobile;
		} else {
			toolbar = new Toolbar();
		}
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Nama : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

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

		button = new MyToolbarbuttonConfig("Ajukan Kegiatan Baru", "/img/new.gif");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				KegiatanKesiswaan kegiatanKesiswaan = new KegiatanKesiswaan();
				kegiatanKesiswaan.setDiajukanOleh(siswa);
				KegiatanKesiswaanAction.onAddExternal(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						loadData(null);
					}
				}, kegiatanKesiswaan);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Ikut Kegiatan", "/img/new.gif");
		button.setVisible(tbmuser != null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				MyWindow window = new MyWindow();
				window.setHeight("97%");
				window.setWidth("800px");
				window.setParent(ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot());
				AmbilDataKegiatanForKegiatanKesiswaanHelper dataSiswaHelper = new AmbilDataKegiatanForKegiatanKesiswaanHelper(
						siswa);
				dataSiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("SK");

		EventListener dataAdding = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Object[] objects = (Object[]) arg0.getData();
				KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa = (KegiatanKesiswaanPunyaSiswa) objects[0];

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
					public void process(XSSFRow row, int index,
							KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa, String jenis)
							throws Exception {
						LampiranLain lam = LampiranLain.ambil(kegiatanKesiswaanPunyaSiswa.getId(), jenis);

						XSSFCell cell = row.createCell(index);

						if (lam != null) {

							String nama = lam.getNama();

							cell.setCellStyle(hlink_style);
							cell.setCellValue(nama);
							String url = lam.createLinkUri();
							XSSFHyperlink link = row.getSheet().getWorkbook().getCreationHelper().createHyperlink(Hyperlink.LINK_URL);
							link.setAddress(url);
							cell.setHyperlink(link);
						}

					}
				}

				DataAddingHelper dataAddingHelper = new DataAddingHelper();

				dataAddingHelper.process(row, 9, kegiatanKesiswaanPunyaSiswa,
						KegiatanKesiswaanPunyaSiswa.class.getName());

			}
		};

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				KegiatanKesiswaanPunyaSiswa.class, this, "Download", "/img/print.png", columnHeadersAdding,
				dataAdding, "id", "kegiatanKesiswaan", "siswa", "jabatanKegiatanKesiswaan",
				"skalaKegiatanKesiswaan", "persetujuan", "mulai", "sampai", "keterangan");
		toolbar.appendChild(cetakToolbarbutton);

		if (siswa != null) {

			MyToolbarbuttonConfig cetak = new MyToolbarbuttonConfig("Cetak Angka Kredit", "/img/print.png");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakAngkaKreditSiswa(siswa);
				}
			});
			cetak.setParent(toolbar);

			cetak = new MyToolbarbuttonConfig("Rekap Angka Kredit", "/img/print.png");
			cetak.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					CommonReportHelper.onCetakRekapAngkaKreditSiswa(siswa);
				}
			});
			cetak.setParent(toolbar);
		}

		// SCROLL (pola Center->Grid->Rows->Row): grid dibungkus Borderlayout -> Center(autoscroll)
		// dgn tinggi terikat agar baris banyak / tabel lebar memunculkan scrollbar. Caption+toolbar
		// tetap di luar borderlayout (hindari North-collapse ZK5.5).
		ais.ui.util.MyBorderlayout blScroll = new ais.ui.util.MyBorderlayout();
		blScroll.setHeight("60vh");
		blScroll.setWidth("100%");
		blScroll.setStyle("min-height:280px;");
		blScroll.setParent(groupbox);
		org.zkoss.zul.Center centerScroll = new org.zkoss.zul.Center();
		centerScroll.setBorder("none");
		centerScroll.setAutoscroll(true);
		centerScroll.setParent(blScroll);

		grid = new MyGrid();// grid.setOddRowSclass("non-odd");
		grid.setWidth("100%");
		grid.setMold("paging");
		grid.setPageSize(10);grid.getPagingChild().setMold("os");
		grid.setParent(centerScroll);

		paging.setParent(groupbox);

		Columns columns = new Columns();

		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("0%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Siswa");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Kegiatan");

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
		column.setLabel("Jabatan/Status");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Skala");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Keterangan");
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("5%");

		loadData(null);

	}

}
