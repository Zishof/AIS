package ais.action.master.sekolah.helper;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Vbox;

import ais.action.master.SertifikatAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.database.model.sekolah.DetailKelompokKegiatanKesiswaan;
import ais.database.model.sekolah.JabatanKegiatanKesiswaan;
import ais.database.model.sekolah.KegiatanKesiswaan;
import ais.database.model.sekolah.KegiatanKesiswaanPunyaSiswa;
import ais.database.model.sekolah.PrestasiSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.SkalaKegiatanKesiswaan;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyCaptionStyled;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;

/**
 * Helper ZK yang menampilkan dan mengelola daftar siswa peserta satu {@link KegiatanKesiswaan}
 * ({@link KegiatanKesiswaanPunyaSiswa}, relasi "punya banyak"), pada modul kesiswaan. Setiap baris
 * dapat diperluas ({@link MyDetail}) untuk menampilkan jabatan/status/tugas dan skala kegiatan
 * (dipilih dari daftar yang diizinkan {@link DetailKelompokKegiatanKesiswaan} milik kegiatan) serta
 * unggahan sertifikat/dokumen pendukung. Menyediakan pencarian (nama/angkatan/yayasan/sekolah),
 * cetak sertifikat massal ({@link SertifikatAction}), dan ekspor daftar peserta ke Excel
 * (menyertakan hyperlink dan pewarnaan sel khusus per baris lewat Apache POI/ZK POI). Mengimplementasikan
 * {@link DataLoader}, {@link DataCriteria}, dan {@link DataSearchDefault} agar dapat dipasang
 * langsung ke komponen baku (paging, tombol cetak/ekspor) yang mengharapkan ketiga kontrak tersebut.
 */
public class KegiatanKesiswaanPunyaSiswaHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private KegiatanKesiswaan kegiatanKesiswaan;
	private Textbox nama;
	private Intbox angkatan;

	private Combobox searchyayasan = new Combobox();
	private Combobox searchsekolah = new Combobox();

	private Paging paging;
	private Tbmuser tbmuser;
	private AmbilDataSiswaBanbox searchsiswa;

	public KegiatanKesiswaanPunyaSiswaHelper() {

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

	}

	/** Renderer baris grid daftar peserta kegiatan: baris dapat diperluas untuk memilih jabatan/skala kegiatan dan mengunggah dokumen pendukung, dengan hak hapus dievaluasi sekali per instance renderer. */
	class DetailKegiatanKesiswaanRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;
		private List<JabatanKegiatanKesiswaan> jabatanKegiatanKesiswaans;
		private List<SkalaKegiatanKesiswaan> skalaKegiatanKesiswaans;

		public DetailKegiatanKesiswaanRenderer() {
			DetailKelompokKegiatanKesiswaan detailKelompokKegiatanKesiswaan = (DetailKelompokKegiatanKesiswaan) HibernateUtil
					.currentSession().createCriteria(DetailKelompokKegiatanKesiswaan.class)
					.add(Restrictions.idEq(kegiatanKesiswaan.getDetailKelompokKegiatanKesiswaan().getId()))
					.uniqueResult();
			jabatanKegiatanKesiswaans = new ArrayList<JabatanKegiatanKesiswaan>(
					detailKelompokKegiatanKesiswaan.getJabatanKegiatanKesiswaans());
			skalaKegiatanKesiswaans = new ArrayList<SkalaKegiatanKesiswaan>(
					detailKelompokKegiatanKesiswaan.getSkalaKegiatanKesiswaans());

			Collections.sort(jabatanKegiatanKesiswaans);
			Collections.sort(skalaKegiatanKesiswaans);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa = (KegiatanKesiswaanPunyaSiswa) data;

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			RevisiHelper.createNewRevisi(KegiatanKesiswaanPunyaSiswa.class, kegiatanKesiswaanPunyaSiswa,
					kegiatanKesiswaanPunyaSiswa.getSiswa().getNim()).setParent(row);

			new Label(kegiatanKesiswaanPunyaSiswa.getSiswa().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKesiswaanPunyaSiswa.getId(),
					KegiatanKesiswaanPunyaSiswa.class.getName(), "Bukti Kegiatan Kesiswaan", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true);

			hbox.setParent(vbox);

			new Label(kegiatanKesiswaanPunyaSiswa.getSiswa().getTahunMasuk() + "").setParent(row);

			new Label(kegiatanKesiswaanPunyaSiswa.getSiswa().getSekolah() == null ? ""
					: kegiatanKesiswaanPunyaSiswa.getSiswa().getSekolah().getNama() + "").setParent(row);

			final MyTextbox keterangan = new MyTextbox(kegiatanKesiswaanPunyaSiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);

			final MyDatebox mulai = new MyDatebox(kegiatanKesiswaanPunyaSiswa.getMulai());
			mulai.setWidth("90%");
			final MyDatebox sampai = new MyDatebox(kegiatanKesiswaanPunyaSiswa.getSampai());
			sampai.setWidth("90%");

			mulai.setParent(row);
			sampai.setParent(row);

			final Combobox jabatanKegiatanKesiswaan = new Combobox();
			jabatanKegiatanKesiswaan.setVisible(!jabatanKegiatanKesiswaans.isEmpty());
			Common.insertComboItems(jabatanKegiatanKesiswaan, "nama", jabatanKegiatanKesiswaans);
			Common.selectComboItem(true, jabatanKegiatanKesiswaan,
					kegiatanKesiswaanPunyaSiswa.getJabatanKegiatanKesiswaan());
			jabatanKegiatanKesiswaan.setParent(row);
			jabatanKegiatanKesiswaan.setReadonly(true);
			jabatanKegiatanKesiswaan.setWidth("97%");

			final Combobox skalaKegiatanKesiswaan = new Combobox();
			skalaKegiatanKesiswaan.setVisible(!skalaKegiatanKesiswaans.isEmpty());
			Common.insertComboItems(skalaKegiatanKesiswaan, "nama", skalaKegiatanKesiswaans);
			Common.selectComboItem(true, skalaKegiatanKesiswaan,
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
							(SkalaKegiatanKesiswaan) (skalaKegiatanKesiswaan.getSelectedItem() == null ? null
									: skalaKegiatanKesiswaan.getSelectedItem().getValue()));
					kegiatanKesiswaanPunyaSiswa.setKeterangan(keterangan.getValue());
					kegiatanKesiswaanPunyaSiswa.setJabatanKegiatanKesiswaan(
							((JabatanKegiatanKesiswaan) (jabatanKegiatanKesiswaan.getSelectedItem() == null ? null
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

			jabatanKegiatanKesiswaan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
			skalaKegiatanKesiswaan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
			keterangan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
			mulai.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
			sampai.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());

			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			cetakToolbarbuttonSertifikat.setOrient("vertical");
			final MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			deleteButton.setOrient("vertical");

			cetakToolbarbuttonSertifikat.setVisible(kegiatanKesiswaanPunyaSiswa.getPersetujuan()
					&& kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getSertifikat() != null);

			final java.util.List<org.zkoss.zk.ui.Component> aksiButtons =
					new java.util.ArrayList<org.zkoss.zk.ui.Component>();
			deleteButton.setVisible(!kegiatanKesiswaanPunyaSiswa.getPersetujuan());
			if (tbmuser.getSiswa() == null && kegiatanKesiswaan.getStatus().equals(PrestasiSiswa.DISETUJUI)) {
				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
				checkbox.setChecked(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
				checkbox.setParent(row);
				row.setValign("top");row.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatanKesiswaanPunyaSiswa.setPersetujuan(checkbox.isChecked());
						Common.refreshSaveOrUpdate(kegiatanKesiswaanPunyaSiswa);
						deleteButton.setVisible(!kegiatanKesiswaanPunyaSiswa.getPersetujuan());

						cetakToolbarbuttonSertifikat.setVisible(kegiatanKesiswaanPunyaSiswa.getPersetujuan()
								&& kegiatanKesiswaanPunyaSiswa.getKegiatanKesiswaan().getSertifikat() != null);

						jabatanKegiatanKesiswaan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
						skalaKegiatanKesiswaan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
						keterangan.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
						mulai.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
						sampai.setDisabled(kegiatanKesiswaanPunyaSiswa.getPersetujuan());
					}
				});
			} else {
				Label label;
				(label = new Label(kegiatanKesiswaanPunyaSiswa.getPersetujuan() == null
						|| kegiatanKesiswaanPunyaSiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(kegiatanKesiswaanPunyaSiswa);
				}
			});
			aksiButtons.add(cetakToolbarbuttonSertifikat);

			deleteButton.setOrient("vertical");
			deleteButton.setVisible(delete);
			deleteButton.setTooltiptext("Hapus Data");
			deleteButton.addEventListener("onClick", new EventListener() {
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

											Common.refreshDelete(kegiatanKesiswaanPunyaSiswa);
											loadData(null);

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
			aksiButtons.add(deleteButton);

			ais.ui.util.UIHelper.buatBarisAksi(row, 3, aksiButtons);

		}

	}

	/**
	 * Menyusun kriteria pencarian {@link KegiatanKesiswaanPunyaSiswa} milik kegiatan kesiswaan aktif,
	 * difilter nama/angkatan/yayasan/sekolah siswa, diurutkan bila diminta.
	 *
	 * @param order {@code true} untuk menyertakan pengurutan
	 * @return kriteria Hibernate siap dieksekusi
	 */
	public Criteria initCriteria(boolean order) {

		Siswa siswa = (Siswa) searchsiswa.getAttribute("siswa");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKesiswaanPunyaSiswa.class);

		criteria.createAlias("siswa", "siswa")

				.add(siswa != null ? Restrictions.eq("siswa.siswa", siswa.getId()) : Restrictions.sqlRestriction("1=1"))

				.createAlias("siswa.sekolah", "sekolah")

				.add(searchsekolah.getSelectedItem() == null || searchsekolah.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("siswa.sekolah", searchsekolah, false))

				.add(searchyayasan.getSelectedItem() == null || searchyayasan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("sekolah.yayasan", searchyayasan, false))

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("siswa.tahunangkatan", angkatan.getValue()))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("siswa.nomorInduk", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("siswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))
				.add(Restrictions.eq("kegiatanKesiswaan", kegiatanKesiswaan));

		if (order)
			criteria.addOrder(Order.asc("siswa.nim"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	/** Memuat halaman peserta kegiatan sesuai kriteria pencarian dan halaman paging aktif, lalu merender hasilnya ke grid. */
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<KegiatanKesiswaanPunyaSiswa> myKegiatanKesiswaanPunyaSiswas = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myKegiatanKesiswaanPunyaSiswas);
				grid.setRowRenderer(new DetailKegiatanKesiswaanRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	/**
	 * Membangun panel daftar peserta kegiatan kesiswaan di dalam komponen target: filter pencarian,
	 * toolbar cetak sertifikat massal dan ekspor Excel, serta grid daftar berpaging.
	 *
	 * @param kegiatanKesiswaan kegiatan kesiswaan yang daftar pesertanya ditampilkan/dikelola
	 * @param component         komponen kontainer target, dibersihkan dan diisi ulang oleh method ini
	 * @param window            jendela pemanggil (konteks tambahan untuk dialog terkait)
	 */
	public void display(final KegiatanKesiswaan kegiatanKesiswaan, final Component component, final MyWindow window) {
		this.kegiatanKesiswaan = kegiatanKesiswaan;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		groupbox.setStyle("min-height: 200px;");
		groupbox.setParent(component);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar siswa yang mengikuti organisasi " + kegiatanKesiswaan.getNama()));

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

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		toolbar.appendChild(nama = new Textbox());
		nama.setCols(10);
		nama.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Angkatan : ")));
		toolbar.appendChild(angkatan = new Intbox());
		angkatan.setCols(4);
		angkatan.addEventListener(Events.ON_OK, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		toolbar.appendChild(new Label(Common.getBahasaConfig("Yayasan") + " : "));
		toolbar.appendChild(searchyayasan);
		searchyayasan.setCols(10);
		searchyayasan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		Common.selectComboItem(searchyayasan, kegiatanKesiswaan.getYayasan());
		if (kegiatanKesiswaan.getYayasan() != null) {
			searchyayasan.setDisabled(true);
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

		if (kegiatanKesiswaan.getSekolah() != null) {
			Yayasan selectedYayasan = (Yayasan) (searchyayasan.getSelectedItem() == null
					|| searchyayasan.getSelectedItem().getValue() == null
					|| searchyayasan.getSelectedItem().getValue() == null ? null
							: searchyayasan.getSelectedItem().getValue());
			if (selectedYayasan != null) {
				Common.insertComboDanSemua(searchsekolah, new String[] { "nama", }, "jenisSekolah", Sekolah.class,
						Restrictions.eq("yayasan", selectedYayasan));
				Common.selectComboItem(searchsekolah, kegiatanKesiswaan.getSekolah());
				searchsekolah.setDisabled(true);
			}
		}

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Siswa : ")));
		toolbar.appendChild(searchsiswa = new AmbilDataSiswaBanbox());
		searchsiswa.setCols(10);
		searchsiswa.setEventListener(new EventListener() {

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

		button = new MyToolbarbuttonConfig("Ambil Siswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataSiswaForKegiatanKesiswaanHelper dataSiswaHelper = new AmbilDataSiswaForKegiatanKesiswaanHelper(
						kegiatanKesiswaan);
				dataSiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.setVisible(tbmuser.getSiswa() == null && tbmuser.getSiswa() == null);
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin menghapus semua data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {
									try {

										Session session = HibernateUtil.currentSession();

										session.createSQLQuery(
												"delete from sekolah.kegiatan_kesiswaan_punya_siswa where (persetujuan is null or persetujuan = false) and kegiatan_kesiswaan = "
														+ kegiatanKesiswaan.getId())
												.executeUpdate();

										loadData(null);

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
		button.setParent(toolbar);

		List<String> columnHeadersAdding = new ArrayList<String>();
		columnHeadersAdding.add("Bukti");

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

				/**
				 * Helper implementasi bersarang milik {@link KegiatanKesiswaanPunyaSiswaHelper} untuk data adding helper.
				 * Kelas ini mengemas langkah lokal yang dipakai kelas induk dan bukan service domain alternatif.
				 *
				 * <p><b>Scope:</b> setiap instance terikat pada instance {@link KegiatanKesiswaanPunyaSiswaHelper} dan dapat
				 * mengakses state kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
				 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code process}(). Aturan bisnis bersama
				 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
				 * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
				 * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
				 * tambahkan perilaku lintas domain pada service bersama.</p>
				 *
				 * @see KegiatanKesiswaanPunyaSiswaHelper
				 */
				class DataAddingHelper {
					public void process(XSSFRow row, int index, KegiatanKesiswaanPunyaSiswa kegiatanKesiswaanPunyaSiswa,
							String jenis) throws Exception {
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

		String[] contents = new String[] { "id", "kegiatanKesiswaan", "siswa", "mulai", "sampai",
				"jabatanKegiatanKesiswaan", "skalaKegiatanKesiswaan", "persetujuan", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(KegiatanKesiswaanPunyaSiswa.class, this,
				"Download", "/img/print.png", columnHeadersAdding, dataAdding, contents);

		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KegiatanKesiswaanPunyaSiswa.class, contents);
		upload.setVisible(Common.getApakahAdmin() || Common.getApakahAdminLain());
		toolbar.appendChild(upload);

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
		column.setLabel("NIM");
		column.setWidth("10%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Nama");
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Angkatan");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Sekolah");

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
		column.setWidth("15%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Persetujuan");
		column.setWidth("8%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("");
		column.setWidth("7%");

		loadData(null);
		
	}

	
	@Override
	/** Menjalankan pencarian default (memakai kriteria sekarang, halaman pertama) dan merender hasilnya ke grid. */
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
