package ais.action.master.helper;
import ais.common.PesanFormalHelper;

import java.awt.Color;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.json.JSONObject;
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

import ais.action.master.JurusanAction;
import ais.action.master.LogLoginAction;
import ais.action.master.SertifikatAction;
import ais.common.Common;
import ais.common.CommonSearchFilterHelper;
import ais.common.CommonPrivilages;
import ais.common.Html2Text;
import ais.common.listener.DataLoader;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.DetailKelompokKegiatanKemahasiswaan;
import ais.database.model.DspaceInformation;
import ais.database.model.Fakultas;
import ais.database.model.JabatanKegiatanKemahasiswaan;
import ais.database.model.Jurusan;
import ais.database.model.KegiatanKemahasiswaan;
import ais.database.model.KegiatanKemahasiswaanPunyaMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Mahasiswa;
import ais.database.model.PrestasiMahasiswa;
import ais.database.model.SkalaKegiatanKemahasiswaan;
import ais.database.model.StatusMahasiswa;
import ais.database.model.Tbmuser;
import ais.database.model.file.LampiranLain;
import ais.ui.dspace.DspaceCommon;
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

public class KegiatanKemahasiswaanPunyaMahasiswaHelper implements DataLoader, DataCriteria, DataSearchDefault {

	private MyGrid grid;
	private KegiatanKemahasiswaan kegiatanKemahasiswaan;
	private Textbox nama;
	private Intbox angkatan;

	private Combobox searchfakultas = new Combobox();
	private Combobox searchjurusan = new Combobox();
	/** Filter status persetujuan PESERTA (Semua / Disetujui / Belum) — melengkapi filter status kegiatan. */
	private Combobox searchPersetujuan = new Combobox();

	private Paging paging;
	private Tbmuser tbmuser;
	private AmbilDataMahasiswaBanbox searchmahasiswa;

	public KegiatanKemahasiswaanPunyaMahasiswaHelper() {

		Common.initFakultasDanJurusanDanSemua(null, null, searchfakultas, searchjurusan);

		tbmuser = Common.getCurrentUser();

		paging = new Paging();
		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

	}

	class DetailKegiatanKemahasiswaanRenderer extends ais.ui.util.MyRowRenderer {

		private boolean delete = false;
		private List<JabatanKegiatanKemahasiswaan> jabatanKegiatanKemahasiswaans;
		private List<SkalaKegiatanKemahasiswaan> skalaKegiatanKemahasiswaans;

		public DetailKegiatanKemahasiswaanRenderer() {
			DetailKelompokKegiatanKemahasiswaan detailKelompokKegiatanKemahasiswaan = (DetailKelompokKegiatanKemahasiswaan) HibernateUtil
					.currentSession().createCriteria(DetailKelompokKegiatanKemahasiswaan.class)
					.add(Restrictions.idEq(kegiatanKemahasiswaan.getDetailKelompokKegiatanKemahasiswaan().getId()))
					.uniqueResult();
			jabatanKegiatanKemahasiswaans = new ArrayList<JabatanKegiatanKemahasiswaan>(
					detailKelompokKegiatanKemahasiswaan.getJabatanKegiatanKemahasiswaans());
			skalaKegiatanKemahasiswaans = new ArrayList<SkalaKegiatanKemahasiswaan>(
					detailKelompokKegiatanKemahasiswaan.getSkalaKegiatanKemahasiswaans());

			Collections.sort(jabatanKegiatanKemahasiswaans);
			Collections.sort(skalaKegiatanKemahasiswaans);
			delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		}

		@Override
		public void render(final Row row, Object data) throws Exception {row.setValign("top");
			final KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) data;

			MyDetail detail = new MyDetail();
			detail.setParent(row);
			detail.setOpen(true);

			RevisiHelper.createNewRevisi(KegiatanKemahasiswaanPunyaMahasiswa.class, kegiatanKemahasiswaanPunyaMahasiswa,
					kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getNim()).setParent(row);

			new Label(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getNama()).setParent(row);

			Vbox vbox = new Vbox();
			vbox.setParent(detail);
			Hbox hbox = new Hbox();

			LampiranLain.createDownloadUploadFileLain(hbox, kegiatanKemahasiswaanPunyaMahasiswa.getId(),
					KegiatanKemahasiswaanPunyaMahasiswa.class.getName(), "Bukti Kegiatan Kemahasiswaan", false,
					new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {

						}
					}, null, false, false, false, true);

			hbox.setParent(vbox);

			new Label(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getTahunangkatan() + "").setParent(row);

			StatusMahasiswa statusMahasiswa = ais.action.master.helper.HistoryStatusMahasiswaUtil.currentStatus(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa())
					.getStatusMahasiswa();
			new Label(statusMahasiswa.getNama()).setParent(row);

			new Label(kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan() == null ? ""
					: kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getNama() + "").setParent(row);

			final MyTextbox keterangan = new MyTextbox(kegiatanKemahasiswaanPunyaMahasiswa.getKeterangan());
			keterangan.setWidth("90%");
			keterangan.setRows(2);

			final MyDatebox mulai = new MyDatebox(kegiatanKemahasiswaanPunyaMahasiswa.getMulai());
			mulai.setWidth("90%");
			final MyDatebox sampai = new MyDatebox(kegiatanKemahasiswaanPunyaMahasiswa.getSampai());
			sampai.setWidth("90%");

			mulai.setParent(row);
			sampai.setParent(row);

			final Combobox jabatanKegiatanKemahasiswaan = new Combobox();
			jabatanKegiatanKemahasiswaan.setVisible(!jabatanKegiatanKemahasiswaans.isEmpty());
			Common.insertComboItems(jabatanKegiatanKemahasiswaan, "nama", jabatanKegiatanKemahasiswaans);
			Common.selectComboItem(true, jabatanKegiatanKemahasiswaan,
					kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan());
			jabatanKegiatanKemahasiswaan.setParent(row);
			jabatanKegiatanKemahasiswaan.setReadonly(true);
			jabatanKegiatanKemahasiswaan.setWidth("97%");

			final Combobox skalaKegiatanKemahasiswaan = new Combobox();
			skalaKegiatanKemahasiswaan.setVisible(!skalaKegiatanKemahasiswaans.isEmpty());
			Common.insertComboItems(skalaKegiatanKemahasiswaan, "nama", skalaKegiatanKemahasiswaans);
			Common.selectComboItem(true, skalaKegiatanKemahasiswaan,
					kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan());
			skalaKegiatanKemahasiswaan.setParent(row);
			skalaKegiatanKemahasiswaan.setReadonly(true);
			skalaKegiatanKemahasiswaan.setWidth("97%");

			EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					kegiatanKemahasiswaanPunyaMahasiswa.setMulai(mulai.getValue());
					kegiatanKemahasiswaanPunyaMahasiswa.setSampai(sampai.getValue());
					kegiatanKemahasiswaanPunyaMahasiswa.setSkalaKegiatanKemahasiswaan(
							(SkalaKegiatanKemahasiswaan) (skalaKegiatanKemahasiswaan.getSelectedItem() == null ? null
									: skalaKegiatanKemahasiswaan.getSelectedItem().getValue()));
					kegiatanKemahasiswaanPunyaMahasiswa.setKeterangan(keterangan.getValue());
					kegiatanKemahasiswaanPunyaMahasiswa.setJabatanKegiatanKemahasiswaan(
							((JabatanKegiatanKemahasiswaan) (jabatanKegiatanKemahasiswaan.getSelectedItem() == null
									? null
									: jabatanKegiatanKemahasiswaan.getSelectedItem().getValue())));
					Common.refreshUpdate(kegiatanKemahasiswaanPunyaMahasiswa);

				}
			};

			skalaKegiatanKemahasiswaan.addEventListener("onChange", eventListener);
			jabatanKegiatanKemahasiswaan.addEventListener("onChange", eventListener);
			keterangan.addEventListener("onChange", eventListener);
			mulai.addEventListener("onChange", eventListener);
			sampai.addEventListener("onChange", eventListener);
			keterangan.setParent(row);

			jabatanKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			skalaKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			keterangan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			mulai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			sampai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());

			final MyToolbarbuttonConfig cetakToolbarbuttonSertifikat = new MyToolbarbuttonConfig("Sertifikat",
					"/img/certificate-icon.png");
			cetakToolbarbuttonSertifikat.setOrient("vertical");
			final MyToolbarbuttonConfig deleteButton = new MyToolbarbuttonConfig("Hapus", "/img/svg/trash.svg");
			deleteButton.setOrient("vertical");

			cetakToolbarbuttonSertifikat.setVisible(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan()
					&& kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getSertifikat() != null);

			Hbox toolbar = new Hbox();
			deleteButton.setVisible(!kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
			if (tbmuser.getMahasiswa() == null
					&& kegiatanKemahasiswaan.getStatus().equals(PrestasiMahasiswa.DISETUJUI)) {
				final MyCheckboxConfig checkbox = new MyCheckboxConfig("Setujui");
				checkbox.setChecked(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
				checkbox.setParent(row);
				row.setValign("top");row.setAttribute("checkbox", checkbox);
				checkbox.addEventListener("onCheck", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						kegiatanKemahasiswaanPunyaMahasiswa.setPersetujuan(checkbox.isChecked());
						Common.refreshSaveOrUpdate(kegiatanKemahasiswaanPunyaMahasiswa);
						deleteButton.setVisible(!kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());

						cetakToolbarbuttonSertifikat.setVisible(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan()
								&& kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan()
										.getSertifikat() != null);

						jabatanKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
						skalaKegiatanKemahasiswaan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
						keterangan.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
						mulai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
						sampai.setDisabled(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan());
					}
				});
			} else {
				Label label;
				(label = new Label(kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() == null
						|| kegiatanKemahasiswaanPunyaMahasiswa.getPersetujuan() ? "Ya" : "Belum")).setParent(row);
				label.setStyle(label.getValue().equals("Belum") ? "color:red;" : "color:blue");
				label.setParent(row);
			}

			cetakToolbarbuttonSertifikat.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					SertifikatAction.cetakSertifikat(kegiatanKemahasiswaanPunyaMahasiswa);
				}
			});
			cetakToolbarbuttonSertifikat.setParent(toolbar);

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

											Common.refreshDelete(kegiatanKemahasiswaanPunyaMahasiswa);
											loadData(null);

										} catch (Exception e) {
											Common.tampilErrorJikaAdmin(e);
											PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
										}

									}

								}
							});

				}

			});
			deleteButton.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(row);

		}

	}

	/** Nilai filter persetujuan peserta yang dipilih: {@code null}=Semua, TRUE=Disetujui, FALSE=Belum. */
	private Boolean persetujuanTerpilih() {
		if (searchPersetujuan == null || searchPersetujuan.getSelectedItem() == null) {
			return null;
		}
		Object v = searchPersetujuan.getSelectedItem().getValue();
		return v instanceof Boolean ? (Boolean) v : null;
	}

	public Criteria initCriteria(boolean order) {

		Mahasiswa mahasiswa = (Mahasiswa) searchmahasiswa.getAttribute("mahasiswa");

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(KegiatanKemahasiswaanPunyaMahasiswa.class);

		criteria.createAlias("mahasiswa", "mahasiswa")

				.add(mahasiswa != null ? Restrictions.eq("mahasiswa.id", mahasiswa.getId())
						: Restrictions.sqlRestriction("1=1"))

				.createAlias("mahasiswa.jurusan", "jurusan")

				.add(searchjurusan.getSelectedItem() == null || searchjurusan.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("mahasiswa.jurusan", searchjurusan, false))

				.add(searchfakultas.getSelectedItem() == null || searchfakultas.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: CommonSearchFilterHelper.eqSelectedWithId("jurusan.fakultas", searchfakultas, false))

				.add(angkatan.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("mahasiswa.tahunangkatan", angkatan.getValue()))

				.add(nama.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("mahasiswa.nim", nama.getValue().trim(), MatchMode.ANYWHERE),
								Restrictions.ilike("mahasiswa.nama", nama.getValue().trim(), MatchMode.ANYWHERE)))
				// Filter status persetujuan PESERTA. "Belum" mencakup nilai null (belum diproses).
				.add(persetujuanTerpilih() == null ? Restrictions.sqlRestriction("1=1")
						: (Boolean.TRUE.equals(persetujuanTerpilih()) ? Restrictions.eq("persetujuan", Boolean.TRUE)
								: Restrictions.or(Restrictions.isNull("persetujuan"),
										Restrictions.eq("persetujuan", Boolean.FALSE))))
				.add(Restrictions.eq("kegiatanKemahasiswaan", kegiatanKemahasiswaan));

		if (order)
			criteria.addOrder(Order.asc("mahasiswa.nim"));

		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void loadData(Object value) {

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Common.initPaging(initCriteria(false), paging);
				List<KegiatanKemahasiswaanPunyaMahasiswa> myKegiatanKemahasiswaanPunyaMahasiswas = initCriteria(true)
						.setMaxResults(Common.ROWS_COUNT_ON_PAGE)
						.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage()))
						.list();
				ListModel strset = new SimpleListModel(myKegiatanKemahasiswaanPunyaMahasiswas);
				grid.setRowRenderer(new DetailKegiatanKemahasiswaanRenderer());
				grid.setModelCheckMobile(strset);
			}
		});

	}

	private DataLoader getDataloader() {
		return this;
	}

	public void display(final KegiatanKemahasiswaan kegiatanKemahasiswaan, final Component component,
			final MyWindow window) {
		this.kegiatanKemahasiswaan = kegiatanKemahasiswaan;
		Common.clear(component);

		ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
		// FIX (scroll tidak ada): beri tinggi maksimum + gulir (vertikal & horizontal) supaya kolom
		// paling kanan (Persetujuan/Hapus) tidak terpotong dan daftar panjang tetap bisa digulir,
		// baik di layar lebar (desktop) maupun sempit (mobile).
		groupbox.setStyle("min-height:200px;box-sizing:border-box;");
		groupbox.setParent(component);
		groupbox.appendChild(
				new MyCaptionStyled("Daftar mahasiswa yang mengikuti organisasi " + kegiatanKemahasiswaan.getNama()));

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(groupbox);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mahasiswa : ")));
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

		toolbar.appendChild(new Label(Common.getBahasaConfig("Fakultas") + " : "));
		toolbar.appendChild(searchfakultas);
		searchfakultas.setCols(10);
		searchfakultas.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		// FIX (mahasiswa dari prodi lain tidak muncul sebagai peserta): JANGAN memfilter otomatis ke
		// fakultas/prodi kegiatan. Peserta suatu kegiatan bisa berasal dari prodi mana pun; jika
		// penyaring dikunci ke prodi kegiatan, peserta lintas-prodi tersembunyi. Default "Semua" agar
		// SEMUA peserta tampil; pengguna tetap dapat menyaring manual bila memang diperlukan.
		Common.selectComboItem(searchfakultas, null);

		toolbar.appendChild(new Label(Common.getBahasaConfig("Jurusan") + " : "));
		toolbar.appendChild(searchjurusan);
		searchjurusan.setCols(10);
		searchjurusan.addEventListener(Events.ON_CHANGE, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		// Isi pilihan prodi dengan SELURUH prodi (bukan hanya prodi kegiatan) + opsi "Semua", lalu
		// default ke "Semua" agar peserta dari prodi mana pun tetap tampil.
		Common.insertComboDanSemua(searchjurusan, new String[] { "nama", "kodeEpsbed" }, "jenjang", Jurusan.class,
				Restrictions.sqlRestriction("1=1"));
		Common.selectComboItem(searchjurusan, null);

		toolbar.appendChild(new Label(ais.common.Common.getBahasaConfig("Mahasiswa : ")));
		toolbar.appendChild(searchmahasiswa = new AmbilDataMahasiswaBanbox());
		searchmahasiswa.setCols(10);
		searchmahasiswa.setEventListener(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				loadData(null);
			}
		});

		// FIX (tidak bisa melihat status persetujuan sebagai peserta): filter status persetujuan
		// PESERTA — melengkapi filter Status yang di layar utama hanya menyaring status KEGIATAN.
		toolbar.appendChild(new Label(Common.getBahasaConfig("Persetujuan") + " : "));
		toolbar.appendChild(searchPersetujuan);
		searchPersetujuan.setCols(8);
		searchPersetujuan.setReadonly(true);
		searchPersetujuan.getChildren().clear();
		org.zkoss.zul.Comboitem ci = new org.zkoss.zul.Comboitem("Semua");
		ci.setValue(null);
		searchPersetujuan.appendChild(ci);
		org.zkoss.zul.Comboitem ciYa = new org.zkoss.zul.Comboitem("Disetujui");
		ciYa.setValue(Boolean.TRUE);
		searchPersetujuan.appendChild(ciYa);
		org.zkoss.zul.Comboitem ciTidak = new org.zkoss.zul.Comboitem("Belum disetujui");
		ciTidak.setValue(Boolean.FALSE);
		searchPersetujuan.appendChild(ciTidak);
		searchPersetujuan.setSelectedItem(ci);
		searchPersetujuan.addEventListener(Events.ON_CHANGE, new EventListener() {
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

		button = new MyToolbarbuttonConfig("Ambil Mahasiswa", "/img/new.gif");
		button.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event event) throws Exception {
				AmbilDataMahasiswaForKegiatanKemahasiswaanHelper dataMahasiswaHelper = new AmbilDataMahasiswaForKegiatanKemahasiswaanHelper(
						kegiatanKemahasiswaan);
				dataMahasiswaHelper.display(getDataloader(), window);
			}

		});
		button.setParent(toolbar);

		button = new MyToolbarbuttonConfig("Bersihkan", "/img/svg/trash.svg");
		button.setVisible(tbmuser.getMahasiswa() == null && tbmuser.getMahasiswa() == null);
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
												"delete from kegiatan_kemahasiswaan_punya_mahasiswa where (persetujuan is null or persetujuan = false) and kegiatan_kemahasiswaan = "
														+ kegiatanKemahasiswaan.getId())
												.executeUpdate();

										loadData(null);

									} catch (Exception e) {
										Common.tampilErrorJikaAdmin(e);
										PesanFormalHelper.tampilkanGagalException("Menghapus data", "Data yang Bapak/Ibu coba hapus kemungkinan besar masih memiliki keterkaitan/relasi dengan data lain pada tabel terkait (misalnya digunakan sebagai referensi oleh transaksi, detail, atau riwayat lain), sehingga sistem basis data menolak proses penghapusan ini demi menjaga integritas data secara keseluruhan.", e, new String[]{"Periksa kembali apakah data ini masih digunakan atau direferensikan oleh data lain yang berelasi.", "Hapus atau lepaskan terlebih dahulu keterkaitan/relasi data tersebut sebelum mencoba menghapus data ini kembali.", "Jika Bapak/Ibu yakin data ini seharusnya sudah tidak digunakan lagi, hubungi Administrator untuk pengecekan lebih lanjut."});
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
				KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa = (KegiatanKemahasiswaanPunyaMahasiswa) objects[0];

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
							KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa, String jenis)
							throws Exception {
						LampiranLain lam = LampiranLain.ambil(kegiatanKemahasiswaanPunyaMahasiswa.getId(), jenis);

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

				dataAddingHelper.process(row, 9, kegiatanKemahasiswaanPunyaMahasiswa,
						KegiatanKemahasiswaanPunyaMahasiswa.class.getName());

			}
		};

		String[] contents = new String[] { "id", "kegiatanKemahasiswaan", "mahasiswa", "mahasiswa.jurusan.nama", "mulai", "sampai",
				"jabatanKegiatanKemahasiswaan", "skalaKegiatanKemahasiswaan", "persetujuan", "keterangan" };

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakDataCustomButton(
				KegiatanKemahasiswaanPunyaMahasiswa.class, this, "Download", "/img/print.png", columnHeadersAdding,
				dataAdding, contents);

		toolbar.appendChild(cetakToolbarbutton);

		MyToolbarbuttonConfig upload = Common.uploadData(this, KegiatanKemahasiswaanPunyaMahasiswa.class, contents);
		upload.setVisible(Common.getApakahAdmin() || Common.getApakahAdminLain());
		toolbar.appendChild(upload);

		// SCROLL (permintaan user): bungkus grid dalam Borderlayout -> Center(autoscroll) -> Grid ->
		// Rows -> Row. Center diberi TINGGI TERIKAT (via Borderlayout height) sehingga bila baris
		// peserta banyak atau tabel lebar, muncul scrollbar (menegak & mendatar). Caption + toolbar
		// sengaja DILUAR borderlayout agar tidak kena jebakan North-collapse ZK 5.5.
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
		column.setLabel("Status");
		column.setWidth("5%");

		column = new MyColumnConfig();
		column.setParent(columns);
		column.setLabel("Jurusan");

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
		MyToolbarbuttonConfig exportKeOjs = new MyToolbarbuttonConfig("Ekspor", "/img/corner.gif");
		toolbar.appendChild(exportKeOjs);
		exportKeOjs.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("kegiatan_mahasiswa_terhubung_ke_dspace"));
		exportKeOjs.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				final Label label = Common.displayLoadBar(new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						onSearchDefault(arg0);
						LogLoginAction.tampilDpsaceLog();
					}
				});

				new Thread(new Runnable() {

					@SuppressWarnings("unchecked")
					@Override
					public void run() {
						try {
							String cookie = DspaceCommon.login();
							List<KegiatanKemahasiswaanPunyaMahasiswa> kegiatanKemahasiswaanPunyaMahasiswas = initCriteria(
									true).add(Restrictions.isNotNull("mahasiswa.jurusan"))
											.add(Restrictions.eq("persetujuan", true)).list();

							int rowIndex = 1;
							for (KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa : kegiatanKemahasiswaanPunyaMahasiswas) {
								label.setValue("Sedang memproses data " + kegiatanKemahasiswaanPunyaMahasiswa.toString()
										+ " ("
										+ Common.numberFormat.get().format(
												(rowIndex++) * 100.0 / kegiatanKemahasiswaanPunyaMahasiswas.size())
										+ " %)");
								KegiatanKemahasiswaanPunyaMahasiswaHelper.getDspace(cookie,
										kegiatanKemahasiswaanPunyaMahasiswa, true);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							Common.tampilErrorJikaAdmin(e);
						}
						label.setValue("");
					}
				}).start();
			}
		});

		MyToolbarbuttonConfig batalExport = new MyToolbarbuttonConfig("Batalkan Ekspor", "/img/svg/trash.svg");
		toolbar.appendChild(batalExport);
		batalExport.setVisible(Common.bolehKonfigurasi("terhubung_ke_dspace", Konfigurasi.TIDAK_AKTIF)
				&& Common.bolehKonfigurasi("kegiatan_mahasiswa_terhubung_ke_dspace"));
		batalExport.addEventListener("onClick", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				MyMessageboxConfig.show("Apakah yakin ingin membatalkan ekspor data ini ?", "Pertanyaan",
						MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
						new EventListener() {

							@Override
							public void onEvent(Event event) throws Exception {
								int i = Integer.parseInt(event.getData().toString());
								if (i == MyMessageboxConfig.OK) {

									final Label label = Common.displayLoadBar(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
											LogLoginAction.tampilDpsaceLog();
										}
									});

									new Thread(new Runnable() {

										@SuppressWarnings("unchecked")
										@Override
										public void run() {
											try {
											try {
												String cookie = DspaceCommon.login();
												List<KegiatanKemahasiswaanPunyaMahasiswa> kegiatanKemahasiswaanPunyaMahasiswas = initCriteria(
														true).add(Restrictions.isNotNull("mahasiswa.jurusan"))
																.add(Restrictions.eq("persetujuan", true)).list();

												int rowIndex = 1;
												for (KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa : kegiatanKemahasiswaanPunyaMahasiswas) {
													label.setValue("Sedang memproses data "
															+ kegiatanKemahasiswaanPunyaMahasiswa.toString() + " ("
															+ Common.numberFormat.get().format((rowIndex++) * 100.0
																	/ kegiatanKemahasiswaanPunyaMahasiswas.size())
															+ " %)");
													DspaceInformation dspaceInformation = DspaceInformation
															.getDspaceInformation(
																	KegiatanKemahasiswaanPunyaMahasiswa.class.getName(),
																	kegiatanKemahasiswaanPunyaMahasiswa.getId());
													if (dspaceInformation != null) {
														int i = DspaceInformation.delete(cookie,
																"items/" + dspaceInformation.getUuid(),
																dspaceInformation.getPostInfo());
														if (i == 200) {

															Session session = HibernateUtil.currentNativeSession();
															session.getTransaction().begin();
															session.delete(dspaceInformation);
															session.getTransaction().commit();
															HibernateUtil.closeSession();
														}
													}
												}
											} catch (Exception e) {
												// TODO Auto-generated catch
												// block
												Common.tampilErrorJikaAdmin(e);
											}
											label.setValue("");
																					} finally {
												ais.database.hibernate.HibernateUtil.closeSession();
											}
										}
									}).start();

								}

							}
						});
			}
		});
	}

	public static DspaceInformation getDspace(String cookie,
			KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa, boolean update) throws Exception {

		JSONArray jsonArray = new JSONArray();

		String nama = "";
		if (kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa() != null) {
			nama = kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getNama();
		}

		JSONObject jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.author");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.contributor.editor");
		jsonMetadata.put("value", nama);
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.date.copyright");
		jsonMetadata.put("value",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonArray.put(jsonMetadata);

		Html2Text parser = new Html2Text();
		parser.parse(new StringReader(kegiatanKemahasiswaanPunyaMahasiswa.getKeterangan()));

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.description.abstract");
		jsonMetadata.put("value", parser.getText());
		jsonArray.put(jsonMetadata);

		if (kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.type");
			jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getJabatanKegiatanKemahasiswaan().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.title");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama());
		jsonArray.put(jsonMetadata);

		if (kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.subject");
			jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getSkalaKegiatanKemahasiswaan().getNama());
			jsonArray.put(jsonMetadata);
		}

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.publisher");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getTempat());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.uri");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getUrl());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.identifier.issn");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getKode());
		jsonArray.put(jsonMetadata);

		jsonMetadata = new JSONObject();
		jsonMetadata.put("key", "dc.language");
		jsonMetadata.put("value", kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getBahasa());
		jsonArray.put(jsonMetadata);

		if (kegiatanKemahasiswaanPunyaMahasiswa.getMulai() != null) {
			jsonMetadata = new JSONObject();
			jsonMetadata.put("key", "dc.date.issued");
			jsonMetadata.put("value", Common.databaseDateFormat.get().format(kegiatanKemahasiswaanPunyaMahasiswa.getMulai()));
			jsonArray.put(jsonMetadata);
		}

		LampiranLain lampiranLain = LampiranLain.ambil(kegiatanKemahasiswaanPunyaMahasiswa.getId(),
				KegiatanKemahasiswaanPunyaMahasiswa.class.getName());
		if (lampiranLain != null) {
			String uri = lampiranLain.createLinkUri(false);
			if (uri != null && !uri.trim().isEmpty()) {
				jsonMetadata = new JSONObject();
				jsonMetadata.put("key", "dc.identifier.uri");
				jsonMetadata.put("value", uri);
				jsonArray.put(jsonMetadata);
			}
		}

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("metadata", jsonArray);

		DspaceInformation dspaceInformation = DspaceInformation.dspaceProcess(cookie,
				kegiatanKemahasiswaanPunyaMahasiswa, jsonPost.toString(), jsonArray.toString(), update, "items",
				"collections/"
						+ getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswa(cookie, kegiatanKemahasiswaanPunyaMahasiswa)
						+ "/items",
				"items/{uuid}/metadata");

		if (lampiranLain != null) {
			DspaceInformation.upload(cookie, dspaceInformation.getUuid(), lampiranLain,
					"Sertifikat / Lampiran Bukti Ikut Kegiatan");
		}

		return dspaceInformation;
	}

	public static DspaceInformation getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswa(String cookie,
			KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa) throws Exception {

		String description = "Kegiatan mahasiswa yang berupa "
				+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama() + " pada kelompok "
				+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan()
						.getDetailKelompokKegiatanKemahasiswaan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama());
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription", "Kegiatan Mahasiswa "
				+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getNama() + " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_kegiatanKemahasiswaanPunyaMahasiswa_jurusan_"
						+ kegiatanKemahasiswaanPunyaMahasiswa.getKegiatanKemahasiswaan().getId() + "_"
						+ kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), true, "collections",
				"communities/" + getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswaJurusan(cookie,
						kegiatanKemahasiswaanPunyaMahasiswa) + "/collections");
	}

	public static DspaceInformation getDspaceTipeKegiatanKemahasiswaanPunyaMahasiswaJurusan(String cookie,
			KegiatanKemahasiswaanPunyaMahasiswa kegiatanKemahasiswaanPunyaMahasiswa) throws Exception {
		Jurusan jurusan = kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan();

		String description = "Prestasi mahasiswa untuk " + Common.getBahasaConfig("Jurusan") + " "
				+ kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getNama();

		JSONObject jsonPost = new JSONObject();
		jsonPost.put("name", "Prestasi Mahasiswa");
		jsonPost.put("copyrightText",
				"Semua hak cipta dilindungi oleh " + Common.getKonfigurasi("label_universitas", "").getNilai());
		jsonPost.put("introductoryText", description);
		jsonPost.put("shortDescription",
				"Prestasi Mahasiswa "
						+ kegiatanKemahasiswaanPunyaMahasiswa.getMahasiswa().getJurusan().getJenjang().getNama()
						+ " Repository");
		jsonPost.put("sidebarText", description);

		Konfigurasi uuidKonfigurasi = Common
				.getKonfigurasi("dspace_label_collection_kegiatanKemahasiswaanPunyaMahasiswa_" + jurusan.getId(), "");
		return DspaceInformation.dspaceProcess(cookie, uuidKonfigurasi, jsonPost.toString(), false, "collections",
				"communities/" + JurusanAction.getDspace(cookie, jurusan, false) + "/collections");

	}

	@Override
	public void onSearchDefault(Event event) {
		loadData(null);
	}

}
