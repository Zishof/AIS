package ais.action.master.sekolah;


import ais.common.CommonSearchFilterHelper;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.sys.ExecutionsCtrl;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Center;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Comboitem;
import org.zkoss.zul.Doublebox;
import org.zkoss.zul.East;
import org.zkoss.zul.Group;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.LayoutRegion;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.North;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;
import ais.ui.util.MyFormRow;
import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.South;
import org.zkoss.zul.Space;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Vbox;

import ais.action.master.helper.RevisiDetailPembayaranSiswaHelper;
import ais.action.master.helper.RevisiHelper;
import ais.action.master.sekolah.helper.AmbilDataCalonSiswaBanbox;
import ais.action.master.sekolah.helper.AmbilDataSiswaBanbox;
import ais.action.master.sekolah.helper.AnalisisTagihanSekolahHelper;
import ais.action.master.sekolah.helper.DetailTagihanSiswaHelper;
import ais.action.master.sekolah.helper.RevisiPembayaranSiswaHelper;
import ais.action.master.sekolah.helper.TagihanUtil;
import ais.action.master.sekolah.helper.TagihanUtilCalonSiswa;
import ais.action.master.sekolah.util.PembayaranSiswaUtil;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.common.Common;
import ais.common.CommonMedia;
import ais.common.CommonPrivilages;
import ais.common.ConstantValues;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.database.model.akunting.DaftarPengajuanTransfer;
import ais.database.model.sekolah.AkunPembayaranSiswa;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.DepositSiswa;
import ais.database.model.sekolah.ItemBiayaSekolah;
import ais.database.model.sekolah.JenisBiayaSekolah;
import ais.database.model.sekolah.NominalBiaya;
import ais.database.model.sekolah.PembayaranSiswa;
import ais.database.model.sekolah.PembayaranSiswaDetail;
import ais.database.model.sekolah.PengaturanBiaya;
import ais.database.model.sekolah.PengaturanBiayaItemBiaya;
import ais.database.model.sekolah.PengaturanBiayaPunyaSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Tagihan;
import ais.ui.util.DataCriteria;
import ais.ui.util.DataInitDefault;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelBoldAja;
import ais.ui.util.MyLabelBolder;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyTextbox;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.WaktuUtil;
import ais.action.master.helper.FilterLanjutHelper;

/**
 * Controller/action ZK untuk pembayaran siswa. Tipe ini merupakan titik masuk UI yang
 * menghubungkan event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh
 * kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyWindow addWindow}, {@code Paging
 * paging}, {@code MyGrid grid}, {@code Combobox searchjenis}, {@code Combobox searchjenisBiaya}, {@code
 * MyDatebox start}, {@code MyDatebox end}, {@code Combobox searchjenisPembayaran}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code init()}, {@code init()}, {@code
 * initSubCriteria()}, {@code initCriteria()}); pembacaan/pencarian ({@code reloadPembayaranSiswa()}, {@code
 * reloadPembayaranSiswa()}, {@code reloadPembayaran()}, {@code onSearchDefault()}); mutasi data ({@code
 * onSave()}); operasi domain lain ({@code onAdd()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau
 * interface yang disebut di atas.</p>
 * <p><b>Efek samping:</b> nama operasi di atas menunjukkan batas orkestrasi kelas ini. Method baca harus tetap
 * bebas dari mutasi tersembunyi; method simpan/hapus/posting wajib memakai transaksi dan otorisasi yang sama
 * dengan alur induknya. Pemanggil baru sebaiknya menggunakan method yang sudah ada atau service bersama, bukan
 * membuat salinan query dan validasi di action lain.</p>
 * <p><b>Lifecycle:</b> instance mengikuti lifecycle komponen ZK dan menyimpan state layar; jangan digunakan
 * sebagai singleton atau dibagikan antar desktop/session. Event handler harus tetap memakai konteks pengguna
 * serta session Hibernate milik request yang aktif.</p>
 *
 * @see GenericAutowireComposer
 */
public class PembayaranSiswaAction extends GenericAutowireComposer
		implements DataCriteria, DataSearchDefault, DataInitDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyWindow addWindow;
	private Paging paging;
	private MyGrid grid;

	private Combobox searchjenis;
	private Combobox searchjenisBiaya;
	private MyDatebox start;
	private MyDatebox end;
	private Combobox searchjenisPembayaran;
	private Combobox searchta;
	private Textbox searchsiswa;
	private Textbox searchketerangan;
	private Textbox searchvalidator;
	private Combobox searchyayasan;
	private Combobox searchsekolah;
	private Siswa selectedSiswa = null;

	private Textbox keterangan;

	private boolean edit = false;
	private boolean approve = false;
	private boolean delete = false;

	protected boolean pembayaranCalonSiswa = false;

	private PembayaranSiswa pembayaranSiswa;
	private MyToolbarbuttonConfig add;

	private AmbilDataSiswaBanbox siswa;
	private AmbilDataCalonSiswaBanbox calonSiswa;
	private Combobox jenisBiayaSekolah;
	private MyDatebox tanggal;
	private Combobox bulan;
	private Combobox tahun;
	private Combobox akunPembayaranSiswa;
	private LayoutRegion east;
	private MyDoublebox deposit;
	private Double total;
	private Double sisaDeposit = 0.0;
	private Double sisaDepositDitambah = 0.0;
	private Double sisaDepositMenjadi;
	private Rows rowsDetailBiaya;
	private MyLabelBoldAja sisaDepositdata;
	private MyLabelBold sisaDepositdatamenjadi;
	private MyLabelBoldAja totalLabel;
	private MyLabelBoldAja tabunganSaatIni;

	private Label siswatampil, nimsiswatampil, kelassiswatampil;
	private Tbmuser tbmuser;

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

		if (ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
			selectedSiswa = (Siswa) ConstantValues.simpleObject(
					HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa"))
							.add(Restrictions.ne("namaSiswa", "")).add(Restrictions.isNotNull("sekolah"))
							.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa")))),
					Siswa.class);

			searchsiswa.setValue(selectedSiswa == null ? "" : selectedSiswa.getNomorInduk());
			searchsiswa.setDisabled(selectedSiswa != null);
		}

		tbmuser = Common.getCurrentUser();

		if (start != null) start.setReadonly(true);
		if (end != null) end.setReadonly(true);

		Calendar calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH) - 6);
		if (start != null) start.setValue(calendar.getTime());
		calendar = ais.ui.util.WaktuUtil.getCalendar();
		calendar.set(Calendar.DATE, calendar.get(Calendar.DATE) + 1);
		if (end != null) end.setValue(calendar.getTime());

		Common.generateTahunAjaranDanSemua(searchta);

		if (tbmuser != null && tbmuser.getSiswa() != null) {
			selectedSiswa = tbmuser.getSiswa();
			searchsiswa.setValue(selectedSiswa == null ? "" : selectedSiswa.getNomorInduk());
			searchsiswa.setDisabled(selectedSiswa != null);
		}

		if (selectedSiswa != null) {
			if (siswatampil != null)
				siswatampil.setValue("Nama Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNama()));
			if (nimsiswatampil != null)
				nimsiswatampil.setValue("NIS Siswa : " + (selectedSiswa == null ? "" : selectedSiswa.getNim()));
			if (kelassiswatampil != null)
				kelassiswatampil.setValue("Kelas Siswa : " + (selectedSiswa == null ? ""
						: (selectedSiswa.getKelas() == null ? "" : selectedSiswa.getKelas().getNama())));
		}

		EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah curr = (Sekolah) (searchsekolah.getSelectedItem() == null ? null
						: searchsekolah.getSelectedItem().getValue());

				String ta = (String) (searchta == null || searchta.getSelectedItem() == null ? null
						: searchta.getSelectedItem().getValue());

				Common.insertComboDanSemua(searchjenis, new String[] { "nama", "sekolah" }, "akun",
						AkunPembayaranSiswa.class,
						Restrictions.and(
								curr == null || curr.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.isNull("sekolah"),
												Restrictions.eq("sekolah", curr)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))

				);
				Common.insertComboDanSemua(searchjenisBiaya, new String[] { "nama", "sekolah" }, "periode",
						JenisBiayaSekolah.class,

						Restrictions.and(
								curr == null || curr.getId() == null ? Restrictions.sqlRestriction("true")
										: Restrictions.or(Restrictions.isNull("sekolah"),
												Restrictions.eq("sekolah", curr)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));

				Common.insertComboDanSemua(searchjenisPembayaran,
						new String[] { "jenisBiayaSekolah", "tahunAngkatan", "kelasSiswa", "kelasLesSiswa",
								"penjurusanSekolah", "sekolah" },
						"tahunAjaran", PengaturanBiaya.class,
						Restrictions.and(
								ta == null || ta.trim().isEmpty() ? Restrictions.sqlRestriction("true")
										: Restrictions.eq("tahunAjaran", ta),
								Restrictions.and(
										curr == null || curr.getId() == null ? Restrictions.sqlRestriction("true")
												: Restrictions.or(Restrictions.isNull("sekolah"),
														Restrictions.eq("sekolah", curr)),
										Restrictions.or(Restrictions.isNull("aktif"),
												Restrictions.eq("aktif", true)))));

			}
		};

		Common.initYayasanDanSekolahDanSemua(null, null, searchyayasan, searchsekolah);

		Common.createDefaultTimerNoBusy(eventListener, "", false, 1000);

		searchsekolah.addEventListener("onChange", eventListener);

		if (searchta != null)
			searchta.addEventListener("onChange", eventListener);

		if (add != null) {
		add.setVisible(CommonPrivilages.checkPrevilages(CommonPrivilages.CREATE));
		add.setTooltiptext("Tambah");
		}

		edit = CommonPrivilages.checkPrevilages(CommonPrivilages.UPDATE);
		delete = CommonPrivilages.checkPrevilages(CommonPrivilages.DELETE);
		approve = CommonPrivilages.checkPrevilages(CommonPrivilages.APPROVE);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		if (tbmuser != null && tbmuser.getSiswa() == null && tbmuser.getCalonSiswa() == null) {
			String[] contents = new String[] { "id", "itemBiayaSekolah", "tagihan.tahunbulan", "tagihan.tahunAjaran",
					"tagihan.siswa", "tagihan.calonSiswa", "nominal", "pembayaranSiswa.tanggal", "keterangan",
					"pembayaranSiswa.validator||oleh||pembayaranSiswa.oleh", "tanggal_dirubah",
					"pembayaranSiswa.akunPembayaranSiswa" };
			MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(PembayaranSiswaDetail.class,
					new DataCriteria() {

						@SuppressWarnings("unchecked")
						@Override
						public Object initCriteria(boolean order) {
							Session session = HibernateUtil.currentSession();

							PengaturanBiaya pengaturanBiaya = (PengaturanBiaya) (searchjenisPembayaran == null
									|| searchjenisPembayaran.getSelectedItem() == null ? null
											: searchjenisPembayaran.getSelectedItem().getValue());
							List<Long> pemId = new ArrayList<Long>();
							if (pengaturanBiaya != null) {
								pemId = session.createCriteria(PembayaranSiswaDetail.class)
										.setProjection(Projections.groupProperty("pembayaranSiswa.id"))
										.createAlias("nominalBiaya", "nominalBiaya")
										.add(Restrictions.eq("nominalBiaya.pengaturanBiaya", pengaturanBiaya)).list();
							}

							String ta = (String) (searchta == null || searchta.getSelectedItem() == null ? null
									: searchta.getSelectedItem().getValue());

							List<Long> pemIds = new ArrayList<Long>();
							if (ta != null && !ta.trim().isEmpty()) {
								pemIds = session.createCriteria(PembayaranSiswaDetail.class)
										.setProjection(Projections.groupProperty("pembayaranSiswa.id"))
										.createAlias("nominalBiaya", "nominalBiaya")
										.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
										.add(Restrictions.eq("pengaturanBiaya.tahunAjaran", ta)).list();
							}

							JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) (searchjenisBiaya == null
									|| searchjenisBiaya.getSelectedItem() == null ? null
											: searchjenisBiaya.getSelectedItem().getValue());
							List<Long> pemIdss = new ArrayList<Long>();
							if (jenisBiayaSekolah != null) {
								pemIdss = session.createCriteria(PembayaranSiswaDetail.class)
										.setProjection(Projections.groupProperty("pembayaranSiswa.id"))
										.createAlias("nominalBiaya", "nominalBiaya")
										.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
										.add(Restrictions.eq("pengaturanBiaya.jenisBiayaSekolah", jenisBiayaSekolah))
										.list();
							}

							Criteria criteria = session.createCriteria(PembayaranSiswaDetail.class)

									.add(pemId.isEmpty() && pengaturanBiaya != null
											? Restrictions.sqlRestriction("false")
											: pemId.isEmpty() ? Restrictions.sqlRestriction("true")
													: Restrictions.in("pembayaranSiswa.id", pemId))

									.add(pemIdss.isEmpty() && jenisBiayaSekolah != null
											? Restrictions.sqlRestriction("false")
											: pemIdss.isEmpty() ? Restrictions.sqlRestriction("true")
													: Restrictions.in("pembayaranSiswa.id", pemIdss))

									.add(pemIds.isEmpty() && (ta != null && !ta.trim().isEmpty())
											? Restrictions.sqlRestriction("false")
											: pemIds.isEmpty() ? Restrictions.sqlRestriction("true")
													: Restrictions.in("pembayaranSiswa.id", pemIds))

									.createCriteria("pembayaranSiswa");

							initSubCriteria(criteria, order);
							return criteria;
						}
					}, contents);
			Common.appendKeToolbar(cetakToolbarbutton, add, comp);

			final Sekolah currSekolahDef = tbmuser == null || tbmuser.ambilSekolah() == null ? SekolahUtil.getSekolah()
					: tbmuser.ambilSekolah();

			MyToolbarbuttonConfig upload = Common.uploadData(this, PembayaranSiswaDetail.class, new EventListener() {

				@SuppressWarnings({ "unchecked", "rawtypes" })
				@Override
				public void onEvent(Event arg0) throws Exception {

					PengaturanBiaya pengaturanBiaya = (PengaturanBiaya) (searchjenisPembayaran == null
							|| searchjenisPembayaran.getSelectedItem() == null ? null
									: searchjenisPembayaran.getSelectedItem().getValue());

					Sekolah currSekolah = (Sekolah) (searchsekolah == null || searchsekolah.getSelectedItem() == null
							? null
							: searchsekolah.getSelectedItem().getValue());

					if (currSekolah == null) {
						currSekolah = currSekolahDef;
					}

					Object[] data = (Object[]) arg0.getData();
					PembayaranSiswaDetail pembayaranSiswaDetail = null;
					List<String> warnings = (List<String>) data[6];
					Map datum = null;
					Session session = HibernateUtil.getSessionFactory().openSession();
					try {

						datum = (Map) data[2];
						List apakahSimpan = (List) data[3];
						String tanggal = datum.get("pembayaranSiswa.tanggal") != null
								? datum.get("pembayaranSiswa.tanggal").toString().trim()
								: "";
						String sis = datum.get("tagihan.siswa") != null ? datum.get("tagihan.siswa").toString().trim()
								: "";
						String calsis = datum.get("tagihan.calonSiswa") != null
								? datum.get("tagihan.calonSiswa").toString().trim()
								: "";
						Double nominal = datum.get("nominal") != null ? (Double) datum.get("nominal") : 0.0;
						String tahunbulan = datum.get("tagihan.tahunbulan") != null
								? datum.get("tagihan.tahunbulan").toString().trim()
								: "1";

						String tahunAjaran = datum.get("tagihan.tahunAjaran") != null
								? datum.get("tagihan.tahunAjaran").toString().trim()
								: "";

						String id = datum.get("id") != null ? datum.get("id").toString().trim() : "";

						if (id != null && !id.trim().isEmpty()) {
							id = datum.get("ID") != null ? datum.get("ID").toString().trim() : "";
						}

						try {
							if (!id.trim().isEmpty()) {
								pembayaranSiswaDetail = (PembayaranSiswaDetail) session
										.createCriteria(PembayaranSiswaDetail.class)
										.add(Restrictions.idEq(Long.parseLong(id.trim()))).uniqueResult();
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:417");
							// TODO: handle exception
						}

						String keterangan = datum.get("keterangan") != null ? datum.get("keterangan").toString().trim()
								: "";

						String akunBayar = datum.get("pembayaranSiswa.akunPembayaranSiswa") != null
								? datum.get("pembayaranSiswa.akunPembayaranSiswa").toString().trim()
								: "";

						for (Object key : datum.keySet()) {
							if (key.toString().trim().equalsIgnoreCase("pembayaranSiswa.tanggal")) {
								tanggal = (datum.get(key) + "").trim();
							} else if (key.toString().trim().equalsIgnoreCase("tagihan.siswa")) {
								sis = (datum.get(key) + "").trim();
							} else if (key.toString().trim().equalsIgnoreCase("tagihan.calonSiswa")) {
								calsis = (datum.get(key) + "").trim();
							} else if (key.toString().trim().equalsIgnoreCase("nominal")) {

								try {
									// Contoh nilai string yang masuk dari input/database
									String nominalStr = datum.get(key) + "";

									// Lalu parse ke Double
									nominal = Double.parseDouble(nominalStr);
								} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:443");
									// TODO: handle exception
								}

							} else if (key.toString().trim().equalsIgnoreCase("tagihan.tahunbulan")) {
								tahunbulan = (datum.get(key) + "").trim();
							} else if (key.toString().trim().equalsIgnoreCase("tagihan.tahunAjaran")) {
								tahunAjaran = (datum.get(key) + "").trim();
							} else if (key.toString().trim().equalsIgnoreCase("keterangan")) {
								keterangan = (datum.get(key) + "").trim();
							} else if (key.toString().trim().equalsIgnoreCase("pembayaranSiswa.akunPembayaranSiswa")) {
								akunBayar = (datum.get(key) + "").trim();
							}
						}

						System.out.println("sis -> " + sis + ", calsis -> " + calsis);

						if (tahunAjaran == null || tahunAjaran.trim().isEmpty()) {

							warnings.add("Tahun ajaran tidak ditemukan. Data sbb : " + datum);

							apakahSimpan.add(false);
							return;
						}

						else if (tahunAjaran != null && !tahunAjaran.trim().isEmpty()) {
							Siswa siswa = (Siswa) Common.getContentAsObject(sis, Siswa.class, Restrictions.and(
									Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)),
									currSekolah != null && currSekolah.getId() != null
											? Restrictions.eq("sekolah", currSekolah)
											: Restrictions.sqlRestriction("true")));

							if (siswa == null) {
								siswa = (Siswa) Common.getContentAsObject(sis, Siswa.class,
										Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)));
							}

							CalonSiswa calonSiswa = null;
							if (siswa == null) {
								calonSiswa = (CalonSiswa) Common.getContentAsObject(calsis, CalonSiswa.class,
										Restrictions.and(
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)),
												currSekolah != null && currSekolah.getId() != null
														? Restrictions.eq("sekolah", currSekolah)
														: Restrictions.sqlRestriction("true")));

								if (calonSiswa == null) {
									calonSiswa = (CalonSiswa) Common.getContentAsObject(calsis, CalonSiswa.class,
											Restrictions.or(Restrictions.isNull("aktif"),
													Restrictions.eq("aktif", true)));
								}
							}

							if (siswa == null && calonSiswa == null) {

								warnings.add("Data siswa tidak ditemukan. calsis " + calsis + " sis " + sis
										+ ". Data sbb : " + datum);

								apakahSimpan.add(false);
								return;
							}

							else if (siswa != null || calonSiswa != null) {

								Sekolah sekolah = siswa != null ? siswa.getSekolah() : calonSiswa.getSekolah();

								ItemBiayaSekolah itemBiayaSekolah = null;
								AkunPembayaranSiswa akunPembayaranSiswa = null;
								String item = datum.get("itemBiayaSekolah") != null
										? datum.get("itemBiayaSekolah").toString().trim()
										: "";
								itemBiayaSekolah = (ItemBiayaSekolah) Common.getContentAsObject(item,
										ItemBiayaSekolah.class,
										Restrictions.and(
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)),
												sekolah != null ? Restrictions.eq("sekolah", sekolah) : null),
										false);

								akunPembayaranSiswa = (AkunPembayaranSiswa) Common.getContentAsObject(akunBayar,
										AkunPembayaranSiswa.class,
										Restrictions.and(
												Restrictions.or(Restrictions.isNull("aktif"),
														Restrictions.eq("aktif", true)),
												sekolah != null ? Restrictions.eq("sekolah", sekolah) : null),
										false);

								System.out.println("siswa -> " + siswa + ", calonSiswa -> " + calonSiswa
										+ ", itemBiayaSekolah -> " + itemBiayaSekolah + " item " + item
										+ ", pengaturanBiaya " + pengaturanBiaya + ", nominal " + nominal
										+ ", tahunbulan " + tahunbulan + ", tahunAjaran " + tahunAjaran + ", id " + id
										+ ", akunBayar -> " + akunBayar + ", akunPembayaranSiswa -> "
										+ akunPembayaranSiswa);

								if (itemBiayaSekolah == null || itemBiayaSekolah.getId() == null) {

									warnings.add("Item biaya Sekolah tidak ditemukan. item->" + item + ", Data sbb : "
											+ datum);

									apakahSimpan.add(false);
									return;
								}

								else if (itemBiayaSekolah != null && itemBiayaSekolah.getId() != null) {

									NominalBiaya nominalBiaya = null;
//								Number maks = null;
									Tagihan tagihan = pembayaranSiswaDetail == null ? null
											: pembayaranSiswaDetail.getTagihan();
									try {
										int bayarKe = 1;
										tagihan = Tagihan.buatAtauLoadTagihan(tagihan, pengaturanBiaya, siswa,
												calonSiswa, itemBiayaSekolah, tahunbulan, nominal, tahunAjaran,
												bayarKe);
									} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:558");
										// TODO: handle exception
									}

									System.out.println("1. tagihan -> " + tagihan);

									if (pengaturanBiaya != null && pengaturanBiaya.getKhususBuatSiswaTertentu()
											&& itemBiayaSekolah != null) {

										if (siswa != null) {
											int pengaturanBiayaPunyaSiswaCount = ((Number) session
													.createCriteria(PengaturanBiayaPunyaSiswa.class)
													.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))
													.add(Restrictions.eq("siswa", siswa))
													.setProjection(Projections.rowCount()).uniqueResult()).intValue();
											System.out.println("pengaturanBiayaPunyaSiswaCount -> "
													+ pengaturanBiayaPunyaSiswaCount);
											if (pengaturanBiayaPunyaSiswaCount == 0) {
												PengaturanBiayaPunyaSiswa pengaturanBiayaPunyaSiswa = new PengaturanBiayaPunyaSiswa();
												pengaturanBiayaPunyaSiswa.setSiswa(siswa);
												pengaturanBiayaPunyaSiswa.setPengaturanBiaya(pengaturanBiaya);
												session.getTransaction().begin();
												session.save(pengaturanBiayaPunyaSiswa);
												session.getTransaction().commit();
											}

											PengaturanBiayaItemBiaya pengaturanBiayaItemBiaya = (PengaturanBiayaItemBiaya) ConstantValues
													.simpleObject(session.createCriteria(PengaturanBiayaItemBiaya.class)
															.createAlias("itemBiayaSekolah", "itemBiayaSekolah")
															.add(Restrictions.or(
																	Restrictions.isNull("itemBiayaSekolah.aktif"),
																	Restrictions.eq("itemBiayaSekolah.aktif", true)))
															.add(Restrictions.eq("pengaturanBiaya", pengaturanBiaya))
															.setMaxResults(1)
															.add(Restrictions.eq("itemBiayaSekolah", itemBiayaSekolah)),
															PengaturanBiayaItemBiaya.class);

											System.out
													.println("pengaturanBiayaItemBiaya -> " + pengaturanBiayaItemBiaya);
											if (pengaturanBiayaItemBiaya != null
													&& pengaturanBiayaItemBiaya.getId() != null) {

												if (pengaturanBiaya.getJenisBiayaSekolah().getPeriode()
														.equalsIgnoreCase("Bulanan")
														|| pengaturanBiaya.getJenisBiayaSekolah().getPeriode()
																.equalsIgnoreCase("Insidentil")) {

													if (nominalBiaya == null) {
														nominalBiaya = TagihanUtil.ambilNominalBiaya(
																pengaturanBiayaItemBiaya, siswa, session);
													}

													if (nominalBiaya != null) {
//													if (maks == null) {
//														maks = (Number) session.createCriteria(Tagihan.class)
//																.add(Restrictions.eq("nominalBiaya", nominalBiaya))
//																.setProjection(Projections.rowCount())
//																.add(Restrictions.gt("nominal", 0.1)).uniqueResult();
//													}
//													int bayarKe = (maks == null ? 1 : maks.intValue());
														int bayarKe = 1;
														tagihan = Tagihan.ambilAtauBuat(session,
																nominalBiaya.getItemBiayaSekolah(),
																nominalBiaya.getPengaturanBiaya(),
																nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(),
																bayarKe, nominalBiaya, tahunbulan.isEmpty() ? null
																		: Integer.parseInt(tahunbulan),
																nominal, true);
													}

												}
											}
											System.out.println("2. tagihan -> " + tagihan);
										}
									}

									if (tagihan == null || tagihan.getId() == null) {

										warnings.add("Tagihan tidak ditemukan. Data sbb : " + datum);

										apakahSimpan.add(false);
										return;
									}

									else if (tagihan != null && tagihan.getId() != null) {

										List<Date> tanggalSebelumnyas = session
												.createCriteria(PembayaranSiswaDetail.class)
												.createAlias("tagihan", "tagihan")
												.createAlias("pembayaranSiswa", "pembayaranSiswa")
												.setProjection(Projections.property("pembayaranSiswa.tanggalBayar"))
												.add(Restrictions.isNotNull("tagihan.pengaturanBiaya"))
												.add(Restrictions.isNotNull("pembayaranSiswa.tanggalBayar"))
												.add(calonSiswa != null && calonSiswa.getId() != null
														? Restrictions.eq("tagihan.calonSiswa.id", calonSiswa.getId())
														: siswa != null && siswa.getId() != null
																? Restrictions.eq("tagihan.siswa.id", siswa.getId())
																: Restrictions.sqlRestriction("false"))
												.add(Restrictions.eq("tagihan.itemBiayaSekolah.id",
														itemBiayaSekolah.getId()))
												.add(Restrictions.eq("tagihan.tahunAjaran", tahunAjaran))
												.add(tahunbulan == null || tahunbulan.isEmpty()
														? Restrictions.sqlRestriction("true")
														: Restrictions.eq("tagihan.tahunbulan",
																Integer.parseInt(tahunbulan)))
												.list();

										List<String> date1 = new ArrayList<String>();
										for (Date d : tanggalSebelumnyas) {
											date1.add(Common.dateFormat83.get().format(d));
										}

										Date t = WaktuUtil.getDate();
										try {
											String content = tanggal;
											if (content.trim().split("-")[2].split(" ")[0].length() == 4
													&& content.trim().split("-")[0].length() == 2
													&& content.trim().split(" ").length == 2) {
												t = Common.dateFormat3.get().parse(content.trim());
											} else if (content.trim().split(":").length == 3
													&& content.trim().length() == 8) {
												t = Common.dateFormat1.get().parse(content.trim());
											} else if (content.trim().split("-")[0].length() == 4) {
												t = Common.databaseDateFormat.get().parse(content.trim());
											} else if (content.trim().contains("/")) {
												t = Common.dateFormat112.get().parse(content.trim());
											} else {
												t = Common.dateFormat1.get().parse(content.trim());
											}
										} catch (Exception e) {
			ais.common.Common.tampilErrorJikaAdmin(e);
		}

										String date2 = Common.dateFormat83.get().format(t);

										boolean buatBaru = tagihan.getNominal().intValue() > nominal.intValue()
												&& !date1.contains(date2);

										if (buatBaru) {

											Tagihan maksTagihan = (Tagihan) session.createCriteria(Tagihan.class)
													.add(Restrictions.eq("nominalBiaya", tagihan.getNominalBiaya()))
													.addOrder(Order.desc("bayarKe")).setMaxResults(1).uniqueResult();
											Double tag = maksTagihan == null ? tagihan.getNominal()
													: maksTagihan.getNominal();

											nominalBiaya = tagihan.getNominalBiaya();
											session.refresh(nominalBiaya);

											Number maksBayar = (Number) session.createCriteria(Tagihan.class)
													.add(Restrictions.isNotNull("pembayaranSiswaDetail"))
													.add(Restrictions.eq("nominalBiaya", nominalBiaya))
													.setProjection(Projections.rowCount())
													.add(Restrictions.gt("nominal", 0.1)).uniqueResult();

											int jml = (maksBayar == null ? 1 : maksBayar.intValue() + 1);

											if (!tagihan.getItemBiayaSekolah().getAngsuranSeragam()) {
												nominalBiaya.setDibayarSebayakManual(jml + 1);
												nominalBiaya.setDibayarSebayak(jml + 1);
												session.getTransaction().begin();
												Common.refreshUpdate(session, nominalBiaya);
												session.getTransaction().commit();

												tagihan.setNominalBiaya(nominalBiaya);
											}
											tagihan.setNominal(nominal);
											session.getTransaction().begin();
											Common.refreshUpdate(session, tagihan);
											session.getTransaction().commit();

											Double sisaYgBelum = tag - nominal;
											if (sisaYgBelum > 0.1) {

												Tagihan tagihan1 = ((Tagihan) session.createCriteria(Tagihan.class)
														.add(Restrictions.eq("nominalBiaya", nominalBiaya))
														.add(Restrictions.eq("bayarKe", jml + 1)).uniqueResult());

												if (tagihan1 == null) {
													try {
														Tagihan tagihanBaru = new Tagihan();
														tagihanBaru.setNominalBiaya(nominalBiaya);
														tagihanBaru.setBulan(nominalBiaya.getPengaturanBiaya()
																.getJenisBiayaSekolah().getUntukBulan());
														tagihanBaru.setTahun(nominalBiaya.getPengaturanBiaya()
																.getJenisBiayaSekolah().getUntukTahun());
														tagihanBaru.setSiswa(nominalBiaya.getSiswa());
														tagihanBaru.setCalonSiswa(nominalBiaya.getCalonSiswa());
														tagihanBaru.setItemBiayaSekolah(tagihan.getItemBiayaSekolah());
														tagihanBaru.setBayarKe(jml + 1);
														tagihanBaru.setNominal(sisaYgBelum);

														tagihanBaru.setInformasi("Tambahan tagihan dari upload");
														session.getTransaction().begin();
														session.save(tagihanBaru);
														session.getTransaction().commit();
													} catch (Exception e) {
														Common.tampilErrorJikaAdmin(e);
													}
												} else {
													tagihan1.setInformasi("Tambahan tagihan dari upload");
													tagihan1.setAktif(true);
													tagihan1.setNominal(sisaYgBelum);
													session.getTransaction().begin();
													Common.refreshUpdate(session, tagihan1);
													session.getTransaction().commit();
												}
											}
										}

										boolean simpan = tanggalSebelumnyas.isEmpty() || !date1.contains(date2);

										System.out.println("N1 -> " + tagihan.getNominal().intValue() + ", N2 -> "
												+ nominal.intValue() + ", d1 = " + date1 + ", d2 = " + date2
												+ ", buatBaru = " + buatBaru + ", simpan = " + simpan);

										if (simpan) {
											pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();
											if (pembayaranSiswaDetail == null) {
												pembayaranSiswaDetail = new PembayaranSiswaDetail();
												pembayaranSiswaDetail.setItemBiayaSekolah(itemBiayaSekolah);
												pembayaranSiswaDetail.setKeterangan(keterangan);
												pembayaranSiswaDetail.setNominal(nominal);
												pembayaranSiswaDetail.setNominalManual(nominal);
												pembayaranSiswaDetail.setNominalBiaya(tagihan.getNominalBiaya());
												pembayaranSiswaDetail.setTagihan(tagihan);
											}

											PembayaranSiswa pembayaranSiswa = pembayaranSiswaDetail
													.getPembayaranSiswa();
											if (pembayaranSiswa == null) {
												pembayaranSiswa = PembayaranSiswaDetail.buatPembayaran(sekolah, tagihan,
														siswa, calonSiswa, tbmuser, nominal, tanggal, keterangan,
														akunPembayaranSiswa, warnings, datum);
												if (pembayaranSiswa == null) {
													return;
												}
												pembayaranSiswaDetail.setPembayaranSiswa(pembayaranSiswa);
											}

											pembayaranSiswaDetail.setKeterangan(keterangan);
											pembayaranSiswaDetail.setNominal(nominal);
											pembayaranSiswaDetail.setNominalManual(nominal);
											pembayaranSiswaDetail.setTagihan(tagihan);
											pembayaranSiswaDetail.setItemBiayaSekolah(itemBiayaSekolah);
											pembayaranSiswaDetail.setNominalBiaya(tagihan.getNominalBiaya());
											session.getTransaction().begin();
											if (pembayaranSiswaDetail.getId() == null) {
												session.save(pembayaranSiswaDetail);
											} else {
												Common.refreshUpdate(session, pembayaranSiswaDetail);
											}
											session.getTransaction().commit();

											tagihan.setPembayaranSiswaDetail(pembayaranSiswaDetail);

											session.getTransaction().begin();
											Common.refreshUpdate(session, tagihan);
											session.getTransaction().commit();

											if (pembayaranSiswa != null) {
												pembayaranSiswa.setNominal(nominal);
												pembayaranSiswa.setNominalManual(nominal);
												pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
												session.getTransaction().begin();
												Common.refreshUpdate(session, pembayaranSiswa);
												session.getTransaction().commit();
											}

											if (tagihan.getDiskonSiswa() != null
													&& !tagihan.getDiskonSiswa().getMemotongTagihan()) {
												DaftarPengajuanTransfer.simpanDiskonPembayaran(tagihan);
											}
										} else {
											try {
												pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();
												if (pembayaranSiswaDetail != null) {
													session.refresh(pembayaranSiswaDetail);
													PembayaranSiswa pembayaranSiswa = pembayaranSiswaDetail
															.getPembayaranSiswa();
													session.refresh(pembayaranSiswa);
													if (pembayaranSiswa != null && (pembayaranSiswa.getNominal()
															.intValue() != nominal.intValue()
															|| pembayaranSiswa.getTambahanDeposit()
																	.intValue() != nominal.intValue())) {
														pembayaranSiswa.setNominal(nominal);
														pembayaranSiswa.setNominalManual(nominal);
														pembayaranSiswa.setTambahanDeposit(nominal);
														pembayaranSiswa.setAkunPembayaranSiswa(akunPembayaranSiswa);
														session.getTransaction().begin();
														Common.refreshUpdate(session, pembayaranSiswa);
														session.getTransaction().commit();
													}
												}
											} catch (Exception e) {
												ais.common.Common.tampilErrorJikaAdmin(e);
											}
										}
									}
								}
								apakahSimpan.add("ya");
							}

							if (calonSiswa != null && calonSiswa.getId() != null) {
								calonSiswa.populatePembayaran();
							}

							if (siswa != null && siswa.getId() != null) {
								siswa.populatePembayaran();
							}

						}
					} catch (Exception e) {
						warnings.add("Error " + e.getMessage() + ". Data sbb : " + datum);
						ais.common.Common.tampilErrorJikaAdmin(e);
					} finally {

						try {
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:877");
							// TODO: handle exception
						}

						try {
							session.disconnect();
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:883");
							// TODO: handle exception
						}

					}
				}

			}, new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					String sql = "delete from sekolah.pembayaran_siswa where id not in (select pembayaran_siswa_id from sekolah.pembayaran_siswa_detail where pembayaran_siswa_id is not null group by pembayaran_siswa_id);";
					Session session = HibernateUtil.currentNativeSession();
					session.getTransaction().begin();
					session.createSQLQuery(sql).executeUpdate();
					session.getTransaction().commit();
					// session.disconnect();
					ais.common.Common.closeOpenedSession(session);
				}
			}, contents);
			upload.setVisible((add != null && add.isVisible()) && edit && delete);
			Common.appendKeToolbar(upload, add, comp);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("History", "/img/jadwal.png");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					RevisiPembayaranSiswaHelper revisiHelper = new RevisiPembayaranSiswaHelper(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.createDefaultTimer(new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									onSearchDefault(arg0);
								}
							});
						}
					});
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();

				}

			});
			// Guard: tombol autowire `add` bisa null (zul tanpa tombol simpan / belum
			// ter-autowire). appendKeToolbar null-safe (pakai add.getParent() bila ada,
			// jika tidak cari toolbar via comp) — hindari NPE add.getParent().
			Common.appendKeToolbar(button, add, comp);

		} else {
			if (add != null) {
				add.setVisible(false);
			}
			edit = false;
			delete = false;
		}

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		}, "", false, 2000);
	        FilterLanjutHelper.setup(comp);
}

	class PembayaranSiswaRenderer extends ais.ui.util.MyRowRenderer {

		@SuppressWarnings("unchecked")
		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final PembayaranSiswa pembayaranSiswa = (PembayaranSiswa) arg1;
			if (pembayaranSiswa.getSiswa() != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(pembayaranSiswa.getSiswa()).setParent(hbox);
				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				vbox.appendChild(new Label(pembayaranSiswa.getSiswa().getNomorInduk()));
				vbox.appendChild(new Label(pembayaranSiswa.getSiswa().getNamaSiswa()));
				vbox.appendChild(new Label(pembayaranSiswa.getSiswa().getSekolah().getNama()));
			} else if (pembayaranSiswa.getCalonSiswa() != null) {
				Hbox hbox = new Hbox();
				hbox.setParent(arg0);
				CommonMedia.tampilkanGambarKecil(pembayaranSiswa.getCalonSiswa()).setParent(hbox);
				Vbox vbox = new Vbox();
				vbox.setParent(hbox);
				vbox.appendChild(new Label(pembayaranSiswa.getCalonSiswa().getNomorInduk()));
				vbox.appendChild(new Label(pembayaranSiswa.getCalonSiswa().getNamaSiswa()));
				vbox.appendChild(new Label(pembayaranSiswa.getCalonSiswa().getSekolah().getNama()));
			}

			Vbox a = RevisiHelper.createNewRevisi(PembayaranSiswa.class, pembayaranSiswa, pembayaranSiswa.getNama());
			a.setParent(arg0);
			new Label(pembayaranSiswa.getAkunPembayaranSiswa() == null ? ""
					: pembayaranSiswa.getAkunPembayaranSiswa().getNama()).setParent(a);

			if (pembayaranSiswa != null && pembayaranSiswa.getId() != null) {
				Session session = null;
				try {
					session = HibernateUtil.currentNativeSession();
					
					// Gunakan Projection SUM agar database yang menjumlahkan, bukan RAM aplikasi
					Number sumResult = (Number) session.createCriteria(PembayaranSiswaDetail.class)
							.add(Restrictions.isNotNull("tagihan"))
							.add(Restrictions.eq("pembayaranSiswa", pembayaranSiswa))
							.setProjection(Projections.sum("nominal"))
							.uniqueResult();

					// Antisipasi null pointer jika tidak ada data yang ditemukan
					Double total = sumResult != null ? sumResult.doubleValue() : 0.0;
					Double currentNominal = pembayaranSiswa.getNominal() != null ? pembayaranSiswa.getNominal() : 0.0;

					if (total.intValue() != currentNominal.intValue()) {
						pembayaranSiswa.setNominal(total);
						pembayaranSiswa.setNominalManual(total); 
						pembayaranSiswa.setTambahanDeposit(total);
						
						// Catatan: Jika objek pembayaranSiswa tidak berada dalam transaksi yang aktif,
						// Anda mungkin perlu memanggil session.update(pembayaranSiswa); di sini.
					}
				} catch (Exception e) {
					ais.common.Common.tampilErrorJikaAdmin(e); // Minimal print error agar terbaca di log server
				} finally {
					// Hati-hati: currentNativeSession biasanya dikendalikan oleh thread/transaksi utama.
					// Menutupnya secara paksa di sini bisa menyebabkan "Session is closed" di method lain.
					// Jika memang wajib ditutup manual sesuai arsitektur lama Anda, gunakan blok try-catch yang aman:
					try {
						ais.common.Common.closeOpenedSession(session);
					} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:1020");
						// Abaikan jika sudah ditutup
					}
				}
			}

			if (Common.isMobile()) {
				new Label(("Waktu : ") + (pembayaranSiswa.getTanggal() == null ? ""
						: Common.dateFormat5.get().format(pembayaranSiswa.getTanggal()))).setParent(a);
				new Label(("Tagihan : ") + (Common.numberFormat.get().format(pembayaranSiswa.getNominal())))
						.setParent(a);
				new Label(("Dibayar : ") + (Common.numberFormat.get().format(pembayaranSiswa.getTambahanDeposit())))
						.setParent(a);
//				new ais.ui.util.MyHtml(dataTagihan + "</ol>").setParent(a);
				new Label(pembayaranSiswa.getKeterangan()).setParent(a);
			}

			Vbox hbox = new Vbox();
			hbox.setParent(arg0);

			new Label(pembayaranSiswa.getTanggal() == null ? ""
					: Common.dateFormat5.get().format(pembayaranSiswa.getTanggal())).setParent(hbox);

			new Label(pembayaranSiswa.getValidator()).setParent(hbox);

			String data = "<ol>";
			data += "<li>Tagihan : Rp. " + Common.numberFormat.get().format(pembayaranSiswa.getNominal()) + "</li>";
			data += "<li>Dibayar : Rp. "
					+ Common.numberFormat.get().format(pembayaranSiswa.getTambahanDeposit()
							- (pembayaranSiswa.getDariTabungan() == null ? 0.0 : pembayaranSiswa.getDariTabungan()))
					+ "</li>";
			if ((pembayaranSiswa.getDariTabungan() == null ? 0.0 : pembayaranSiswa.getDariTabungan()) > 0.1) {
				data += "<li>Tabungan : Rp. "
						+ Common.numberFormat.get().format(
								(pembayaranSiswa.getDariTabungan() == null ? 0.0 : pembayaranSiswa.getDariTabungan()))
						+ "</li>";
			}
			new ais.ui.util.MyHtml(data + "</ol>").setParent(arg0);

//			new ais.ui.util.MyHtml(dataTagihan + "</ol>").setParent(arg0);
			new Label(pembayaranSiswa.getKeterangan()).setParent(arg0);

			// Common.copyEditDeleteButtons(edit, delete, pembayaranSiswa,
			// PembayaranSiswaAction.this).setParent(arg0);

			Hbox toolbar = new Hbox();

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/print.png");
			button.setTooltiptext("Cetak Data");
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					PembayaranSiswaUtil.cetakStruk(pembayaranSiswa);
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/eye-icon.png");
			button.setTooltiptext("Ubah Data");
			button.setVisible(false);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					init(pembayaranSiswa);
					addWindow.setVisible(true);
					addWindow.onModal();
				}

			});
			button.setParent(toolbar);

			button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setVisible(delete);

			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin membatalkan pembayaran ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {

											Common.refreshDelete(pembayaranSiswa);

											Common.createDefaultTimer(new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {

													if (pembayaranSiswa.getSiswa() != null) {
														Siswa.populate(pembayaranSiswa.getSiswa());
													} else if (pembayaranSiswa.getCalonSiswa() != null) {
														CalonSiswa.populate(pembayaranSiswa.getCalonSiswa());
													}

													onSearchDefault(arg0);
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
			button.setParent(toolbar);
			ais.ui.util.MenuAksiBaris.pasang(toolbar);
			toolbar.setParent(arg0);

			button = new MyToolbarbuttonConfig("", "/img/svg/clock-history.svg");
			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event event) throws Exception {
					RevisiDetailPembayaranSiswaHelper revisiHelper = new RevisiDetailPembayaranSiswaHelper(
							new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									Common.createDefaultTimer(new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											onSearchDefault(arg0);
										}
									});
								}
							}, pembayaranSiswa);
					ExecutionsCtrl.getCurrentCtrl().getCurrentPage().getFirstRoot().appendChild(revisiHelper);
					revisiHelper.setVisible(true);
					revisiHelper.onModal();

				}

			});
			button.setParent(toolbar);
		}

	}

	public void onAdd(Event event) throws Exception {
		init(new PembayaranSiswa());
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	@Override
	public void init(GeneralValueObject obj) throws Exception {
		pembayaranSiswa = (PembayaranSiswa) obj;
		init(pembayaranSiswa);
		addWindow.setVisible(true);
		addWindow.onModal();
	}

	private void reloadPembayaranSiswa(final Siswa s, final CalonSiswa cs) throws Exception {
		reloadPembayaranSiswa(s, cs, false);
	}

	@SuppressWarnings({ "unchecked" })
	private void reloadPembayaranSiswa(final Siswa s, final CalonSiswa cs, final boolean refresh) throws Exception {

		JenisBiayaSekolah jenisBiaya = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null ? null
				: jenisBiayaSekolah.getSelectedItem().getValue());
		// if (jenisBiaya == null) {
		// east.appendChild(new MyLabelBolder("Jenis Pembayaran harus
		// dipilih"));
		// return;
		// }
		sisaDeposit = s == null ? 0.0 : s.hitungSisaDeposit(tanggal.getValue());
		sisaDepositDitambah = sisaDeposit;

		List<Tagihan> tagihans;

		int bulantahun = 0;

		if (pembayaranSiswa.getId() == null) {
			Integer bulan = (Integer) (this.bulan.getSelectedItem() == null ? null
					: this.bulan.getSelectedItem().getValue());
			Integer tahun = (Integer) (this.tahun.getSelectedItem() == null ? null
					: this.tahun.getSelectedItem().getValue());
			bulantahun = PembayaranSiswa.convert(tahun, bulan);
			tagihans = cs == null ? TagihanUtil.getTagihan(jenisBiaya, null, s, bulan, tahun, refresh)
					: TagihanUtilCalonSiswa.getTagihan(jenisBiaya, null, cs, bulan, tahun, refresh);
			// Sembunyikan tagihan di bulan setelah tanggal/tahun lulus untuk siswa yang sudah keluar
			if (s != null && cs == null && s.getStatusKeluar() != null) {
				try {
					Integer lulusYYYYMM = null;
					if (s.getTanggalLulus() != null) {
						Calendar calLulus = Calendar.getInstance();
						calLulus.setTime(s.getTanggalLulus());
						lulusYYYYMM = calLulus.get(Calendar.YEAR) * 100
								+ (calLulus.get(Calendar.MONTH) + 1);
					} else if (s.getTahunLulus() != null) {
						lulusYYYYMM = s.getTahunLulus() * 100 + 12;
					}
					if (lulusYYYYMM != null) {
						List<Tagihan> filtered = new ArrayList<Tagihan>();
						for (Tagihan tagihan : tagihans) {
							if (tagihan.getTahunbulan() == null || tagihan.getTahunbulan() <= lulusYYYYMM) {
								filtered.add(tagihan);
							}
						}
						tagihans = filtered;
					}
				} catch (Exception eignore) { ais.common.ErrorAuditUtil.record(eignore, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:1237");
				}
			}
		} else {
			tagihans = HibernateUtil.currentSession().createCriteria(Tagihan.class)
					.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
					.createAlias("pembayaranSiswaDetail", "pembayaranSiswaDetail")
					.add(Restrictions.eq("pembayaranSiswaDetail.pembayaranSiswa", pembayaranSiswa)).list();
		}

//		System.out.println("tagihans => " + tagihans);

		Common.clear(east);

		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
		borderlayout.setParent(east);

		if (pembayaranSiswa.getId() == null) {
			Center center = new Center();
			center.setTitle("Informasi Tagihan Siswa");
			center.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(center, true);

			North north = new North();
			north.setParent(borderlayout);
			MyGrid grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(north);
			grid.setWidth("100%");
			grid.setHeight("100%");
			grid.setSclass("fgrid");

			Columns columns = new Columns();
			columns.setParent(grid);

			MyColumnConfig column = new MyColumnConfig();
			column.setParent(columns);
			column.setWidth("80px");

			column = new MyColumnConfig();
			column.setParent(columns);

			Rows rows = new Rows();
			rows.setParent(grid);

			MyFormRow row = new MyFormRow();
			row.setValign("top");
			row.setValign("top");
			row.setParent(rows);
			row.appendChild(cs != null ? CommonMedia.tampilkanGambarKecil(cs) : CommonMedia.tampilkanGambarKecil(s));

			grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(row);
			grid.setWidth("100%");
			grid.setHeight("100%");
			grid.setSclass("fgrid");

			rows = new Rows();
			rows.setParent(grid);

			if (cs != null) {
				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new MyLabelBoldAja("No. Reg"));
				row.appendChild(new MyLabelBoldAja(cs.getNomorInduk()));
				row.appendChild(new MyLabelBoldAja("Gelombang"));
				row.appendChild(new MyLabelBoldAja(
						(cs.getGelombangPendaftaranPsb() == null ? "" : cs.getGelombangPendaftaranPsb().getNama())));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new MyLabelBoldAja("Nama"));
				row.appendChild(new MyLabelBoldAja(cs.getNamaSiswa()));
				row.appendChild(new MyLabelBoldAja("Sekolah"));
				row.appendChild(new MyLabelBoldAja((cs.getSekolah() == null ? "" : cs.getSekolah().getNama())));
			} else {

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new MyLabelBoldAja("NIS"));
				row.appendChild(new MyLabelBoldAja(s.getNomorInduk()));
				row.appendChild(new MyLabelBoldAja("Kelas"));
				row.appendChild(new MyLabelBoldAja((s.getKelas() == null ? "" : s.getKelas().getNama())));

				row = new MyFormRow();
				row.setParent(rows);
				row.appendChild(new MyLabelBoldAja("Nama"));
				row.appendChild(new MyLabelBoldAja(s.getNamaSiswa()));
				row.appendChild(new MyLabelBoldAja("Sekolah"));
				row.appendChild(new MyLabelBoldAja((s.getSekolah() == null ? "" : s.getSekolah().getNama())));
			}

			MyGrid gridDetailBiaya = new MyGrid();
			gridDetailBiaya.setWidth("100%");
			gridDetailBiaya.setParent(center);
			gridDetailBiaya.setWidth("100%");
			gridDetailBiaya.setHeight("100%");
			gridDetailBiaya.setSclass("dgrid");

			columns = new Columns();
			columns.setParent(gridDetailBiaya);

			column = new MyColumnConfig();
			final MyCheckboxConfig semua = new MyCheckboxConfig("Item Biaya");
			column.appendChild(semua);
			column.setParent(columns);

			column = new MyColumnConfig("Thn/Bln");
			column.setParent(columns);
			column.setWidth("20%");

			column = new MyColumnConfig("Nominal Tagihan");
			column.setParent(columns);
			column.setAlign("right");
			column.setWidth("25%");

			column = new MyColumnConfig("Aktif");
			column.setParent(columns);
			if (Common.getApakahAdmin()) {
				column.setWidth("8%");
			} else {
				column.setWidth("0px");
			}

			rowsDetailBiaya = new Rows();
			rowsDetailBiaya.setParent(gridDetailBiaya);

			if (tagihans == null || tagihans.isEmpty()) {
				Row infoKosong = new Row();
				infoKosong.setSpans("4");
				infoKosong.setStyle("background:#fff7ed;border-left:4px solid #f59e0b;padding:8px");
				Vbox panduan = new Vbox();
				panduan.setWidth("100%");
				panduan.appendChild(new Label("Belum ada tagihan yang dapat ditampilkan. Gunakan Analisis Data untuk mengetahui kriteria Pengaturan Biaya yang belum cocok."));
				final JenisBiayaSekolah jenisBiayaAnalisis = jenisBiaya;
				MyToolbarbuttonConfig analisis = new MyToolbarbuttonConfig("Analisis Data", "/img/svg/search.svg");
				analisis.setTooltiptext("Periksa sekolah, jenis pengguna, angkatan, kelas, asrama, jurusan, status awal, gelombang, item biaya, dan hasil proses tagihan");
				analisis.setParent(panduan);
				analisis.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						Integer bln = PembayaranSiswaAction.this.bulan.getSelectedItem() == null ? null
								: (Integer) PembayaranSiswaAction.this.bulan.getSelectedItem().getValue();
						Integer thn = PembayaranSiswaAction.this.tahun.getSelectedItem() == null ? null
								: (Integer) PembayaranSiswaAction.this.tahun.getSelectedItem().getValue();
						AnalisisTagihanSekolahHelper.buka(s, cs, jenisBiayaAnalisis, bln, thn);
					}
				});
				panduan.setParent(infoKosong);
				infoKosong.setParent(rowsDetailBiaya);
			}

			final EventListener hitungNominalBiaya = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					@SuppressWarnings("rawtypes")
					List rows = rowsDetailBiaya.getChildren();
					total = 0.0;
					for (Object o : rows) {
						try {
							if (o instanceof Row) {
								Row row = (Row) o;
								MyCheckboxConfig checkboxConfig = (MyCheckboxConfig) row.getAttribute("pilih");
								if (checkboxConfig != null && checkboxConfig.isChecked()) {
									Tagihan tagihan = (Tagihan) row.getAttribute("tagihan");
									if (tagihan != null) {
										Doublebox n = (Doublebox) row.getAttribute("nominal");

										Double nominal = tagihan.getNominalBiaya().getItemBiayaSekolah()
												.getNilaiBiayaBisaDiubahSaatPembayaran()
														? (n == null ? 0.0 : n.getValue())
														: tagihan.getNominal();
										nominal += tagihan.getDenda();

										total += nominal;
									}
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:1392");
							// TODO: handle exception
						}
					}
					totalLabel.setValue(Common.numberFormat.get().format(total));
					sisaDeposit = s == null ? 0.0 : s.hitungSisaDeposit(tanggal.getValue());
					sisaDepositDitambah = sisaDeposit;
					tabunganSaatIni.setValue(Common.numberFormat.get().format(sisaDeposit));
					sisaDepositDitambah = (deposit.getValue() == null ? 0.0 : deposit.getValue()) + sisaDeposit;
					sisaDepositdata.setValue(Common.numberFormat.get().format(sisaDepositDitambah));

					sisaDepositMenjadi = sisaDepositDitambah - total;
					sisaDepositdatamenjadi.setValue(Common.numberFormat.get().format(sisaDepositDitambah - total));

				}
			};

			total = 0.0;
			final List<MyCheckboxConfig> listCheck = new ArrayList<MyCheckboxConfig>();

			EventListener groupEvent = new EventListener() {

				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Checkbox bulan = (Checkbox) arg0.getTarget();
					Tagihan tagihan = (Tagihan) bulan.getAttribute("tagihan");
					JenisBiayaSekolah jenisBiayaSekolah = tagihan.getPengaturanBiaya()
							.getJenisBiayaSekolah();
					Integer bulanTahun = tagihan.getTahunbulan();
					List dataRow = rowsDetailBiaya.getChildren();
					for (Object o : dataRow) {
						try {
							if (o instanceof Row) {
								Tagihan tagihan2 = (Tagihan) ((Row) o).getAttribute("tagihan");
								JenisBiayaSekolah jenisBiayaSekolah2 = tagihan2.getPengaturanBiaya()
										.getJenisBiayaSekolah();
								Integer bulanTahun2 = tagihan2.getTahunbulan();

								if (jenisBiayaSekolah.getId().equals(jenisBiayaSekolah2.getId())
										&& bulanTahun.equals(bulanTahun2)) {
									MyCheckboxConfig checkboxConfig = (MyCheckboxConfig) ((Row) o)
											.getAttribute("pilih");
									checkboxConfig.setChecked(bulan.isChecked());
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:1438");
							// TODO: handle exception
						}
					}
					hitungNominalBiaya.onEvent(arg0);
				}
			};

			EventListener groupEvent1 = new EventListener() {

				@SuppressWarnings("rawtypes")
				@Override
				public void onEvent(Event arg0) throws Exception {
					Checkbox bulan = (Checkbox) arg0.getTarget();
					Tagihan tagihan = (Tagihan) bulan.getAttribute("tagihan");
					JenisBiayaSekolah jenisBiayaSekolah = tagihan.getPengaturanBiaya()
							.getJenisBiayaSekolah();
					List dataRow = rowsDetailBiaya.getChildren();
					for (Object o : dataRow) {
						try {
							if (o instanceof Row) {
								Tagihan tagihan2 = (Tagihan) ((Row) o).getAttribute("tagihan");
								JenisBiayaSekolah jenisBiayaSekolah2 = tagihan2.getPengaturanBiaya()
										.getJenisBiayaSekolah();

								if (jenisBiayaSekolah.getId().equals(jenisBiayaSekolah2.getId())) {
									MyCheckboxConfig checkboxConfig = (MyCheckboxConfig) ((Row) o)
											.getAttribute("pilih");
									checkboxConfig.setChecked(bulan.isChecked());
								}
							}
						} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:1469");
							// TODO: handle exception
						}
					}
					hitungNominalBiaya.onEvent(arg0);
				}
			};

			Collections.sort(tagihans);
			Integer bulantahunCurrent = -2;
			Long idP = -7L;
			for (final Tagihan tagihan : tagihans) {
				if (((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
						&& !tagihan.getNominalBiaya().getBukanTagihan())) {
					if (tagihan.getTahunbulan() == null || (tagihan.getTahunbulan() <= bulantahun)) {

						if (tagihan.getTahunbulan() != null) {
							Integer bulantahunlangsung = tagihan.getTahunbulan() == null ? -1 : tagihan.getTahunbulan();
							if (!bulantahunCurrent.equals(bulantahunlangsung)) {

								Checkbox bulan = new Checkbox(tagihan.getPengaturanBiaya().toString()
										+ (tagihan.getBulan() == null ? "" : ", bulan " + tagihan.getBulan())
										+ (tagihan.getTahun() == null ? "" : ", tahun " + tagihan.getTahun()));

								Group group = new ais.ui.util.MyGroupConfig();
								bulan.setAttribute("tagihan", tagihan);
								group.setParent(rowsDetailBiaya);
								group.appendChild(bulan);
								bulan.addEventListener("onClick", groupEvent);

								bulantahunCurrent = bulantahunlangsung;
							}
						} else {
							if (!idP.equals(tagihan.getPengaturanBiaya().getId())) {

								Checkbox bulan = new Checkbox(
										tagihan.getPengaturanBiaya().toString());

								Group group = new ais.ui.util.MyGroupConfig();
								bulan.setAttribute("tagihan", tagihan);
								group.setParent(rowsDetailBiaya);
								Hbox hbox = new Hbox();
								group.appendChild(hbox);

								hbox.appendChild(bulan);
								bulan.addEventListener("onClick", groupEvent1);

								idP = tagihan.getPengaturanBiaya().getId();

								if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
										.getGunakanCalonSiswa()) {

								} else {

									Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Tagihan",
											"/img/Finance-Invoice-icon.png");
									toolbarbutton.setParent(hbox);

									toolbarbutton.addEventListener("onClick", new EventListener() {

										@Override
										public void onEvent(Event arg0) throws Exception {
											final MyWindow addWindow = new MyWindow("Tambah Pembayaran/Angsuran",
													"none", false);
											page.getFirstRoot().appendChild(addWindow);
											addWindow.setHeight("95%");
											addWindow.setWidth("90%");

											Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
											Center center = new Center();
											center.setParent(borderlayout);
											ais.ui.util.ZkCompat.setFlex(center, true);

											new DetailTagihanSiswaHelper(tagihan.getSiswa(), edit, approve)
													.display(tagihan.getPengaturanBiaya(), center);

											South south = new South();
											ais.ui.util.ZkCompat.setFlex(south, true);
											south.setParent(borderlayout);

											Toolbar toolbar = new Toolbar();
											// toolbar.setHeight("25px");
											toolbar.setParent(south);
											MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup",
													"/img/cancel.gif");
											cancel.setTooltiptext("Tutup");
											cancel.addEventListener("onClick", new EventListener() {
												@Override
												public void onEvent(Event event) throws Exception {
													addWindow.detach();
													reloadPembayaranSiswa(s, cs);
												}
											});
											cancel.setParent(toolbar);

											borderlayout.setParent(addWindow);

											addWindow.onModal();
										}
									});

								}

								if (tagihan.getNominalBiaya().getItemBiayaSekolah().getBolehDiangsur()
										&& tagihan.getPengaturanBiaya().getJenisBiayaSekolah()
												.getBolehAngsurBerapapun()) {

									if (!tagihan.getNominalBiaya().getItemBiayaSekolah().getAngsuranSeragam()) {

										Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("Tambah",
												"/img/add_item.png");
										toolbarbutton.setParent(hbox);

										toolbarbutton.addEventListener("onClick", new EventListener() {

											@Override
											public void onEvent(Event arg0) throws Exception {

												final MyWindow addWindow = new MyWindow("Tambah Pembayaran/Angsuran",
														"none", false);
												page.getFirstRoot().appendChild(addWindow);
												addWindow.setHeight("300px");
												addWindow.setWidth("500px");

												Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
												Center center = new Center();
												center.setParent(borderlayout);
												ais.ui.util.ZkCompat.setFlex(center, true);
												MyGrid grid = new MyGrid();
												grid.setWidth("100%");
												grid.setParent(center);
												grid.setWidth("100%");
												grid.setHeight("100%");

												Columns columns = new Columns();
												columns.setParent(grid);

												MyColumnConfig column = new MyColumnConfig();
												column.setParent(columns);
												column.setWidth("40%");

												column = new MyColumnConfig();
												column.setParent(columns);

												Rows rows = new Rows();
												rows.setParent(grid);

												List<PengaturanBiayaItemBiaya> pengaturanBiayaItemBiayas = ConstantValues
														.simpleList(
																HibernateUtil.currentSession()
																		.createCriteria(PengaturanBiayaItemBiaya.class)
																		.createAlias("itemBiayaSekolah",
																				"itemBiayaSekolah")
																		.addOrder(Order.asc("itemBiayaSekolah.nama"))
																		.add(Restrictions.eq("pengaturanBiaya",
																				tagihan.getNominalBiaya()
																						.getPengaturanBiaya())),
																PengaturanBiayaItemBiaya.class);

												MyFormRow row = new MyFormRow();
												row.setValign("top");
												row.setParent(rows);
												row.appendChild(new ais.ui.util.MyLabelConfig("Item Biaya *"));

												final Combobox itemBiaya = new Combobox();
												for (PengaturanBiayaItemBiaya pembayaranSiswaDetail : pengaturanBiayaItemBiayas) {
													Comboitem comboitem = new Comboitem(
															pembayaranSiswaDetail.getItemBiayaSekolah().getNama());
													comboitem.setValue(pembayaranSiswaDetail.getItemBiayaSekolah());
													itemBiaya.appendChild(comboitem);
												}
												itemBiaya.setReadonly(true);
												row.appendChild(itemBiaya);
												itemBiaya.setWidth("90%");

												row = new MyFormRow();
												row.setParent(rows);
												row.appendChild(
														new ais.ui.util.MyLabelConfig("Nominal yang akan dibayar *"));
												final MyDoublebox dibayar;
												row.appendChild(dibayar = new MyDoublebox(0.0));
												dibayar.setWidth("90%");

												row = new MyFormRow();
												row.setParent(rows);
												row.appendChild(new ais.ui.util.MyLabelConfig("Catatan / Informasi"));
												final MyTextbox informasi;
												row.appendChild(informasi = new MyTextbox());
												informasi.setWidth("90%");
												informasi.setRows(5);

												South south = new South();
												ais.ui.util.ZkCompat.setFlex(south, true);
												south.setParent(borderlayout);

												Toolbar toolbar = new Toolbar();
												// toolbar.setHeight("25px");
												toolbar.setParent(south);
												MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal",
														"/img/cancel.gif");
												cancel.setTooltiptext("Tutup");
												cancel.addEventListener("onClick", new EventListener() {
													@Override
													public void onEvent(Event event) throws Exception {
														addWindow.detach();
													}
												});
												cancel.setParent(toolbar);
												MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Simpan",
														"/img/save.gif");
												save.setTooltiptext("Simpan");
												save.addEventListener("onClick", new EventListener() {
													@Override
													public void onEvent(Event event) throws Exception {

														ItemBiayaSekolah itemBiayaSekolah = (ItemBiayaSekolah) (itemBiaya
																.getSelectedItem() == null ? null
																		: itemBiaya.getSelectedItem().getValue());
														if (itemBiayaSekolah == null) {
															MyMessageboxConfig.show("Item Biaya harus diisi",
																	"Peringatan", MyMessageboxConfig.OK,
																	MyMessageboxConfig.INFORMATION);
															return;
														}

														if (dibayar.getValue() < 0.01) {
															MyMessageboxConfig.show("Nominal Biaya harus diisi",
																	"Peringatan", MyMessageboxConfig.OK,
																	MyMessageboxConfig.INFORMATION);
															return;
														}

														addWindow.detach();

														Session session = HibernateUtil.currentSession();

														String kodeUnik = NominalBiaya.genCode(itemBiayaSekolah,
																tagihan.getPengaturanBiaya(),
																tagihan.getSiswa(), tagihan.getCalonSiswa());

														NominalBiaya nominalBiaya = (NominalBiaya) session
																.createCriteria(NominalBiaya.class)
																.add(Restrictions.eq("kodeUnik", kodeUnik))
																.setMaxResults(1).uniqueResult();
														// Fallback: cari by PB+item+siswa/calonSiswa tanpa kodeUnik
														// (satu NominalBiaya per kombinasi; menghindari duplikat)
														if (nominalBiaya == null) {
															org.hibernate.Criteria critFallback = session
																	.createCriteria(NominalBiaya.class)
																	.add(Restrictions.eq("pengaturanBiaya",
																			tagihan.getPengaturanBiaya()))
																	.add(Restrictions.eq("itemBiayaSekolah",
																			itemBiayaSekolah));
															if (tagihan.getSiswa() != null) {
																critFallback.add(Restrictions.eq("siswa",
																		tagihan.getSiswa()));
															} else if (tagihan.getCalonSiswa() != null) {
																critFallback.add(Restrictions.eq("calonSiswa",
																		tagihan.getCalonSiswa()));
															}
															nominalBiaya = (NominalBiaya) critFallback
																	.addOrder(Order.asc("id"))
																	.setMaxResults(1).uniqueResult();
														}
														if (nominalBiaya == null) {
															nominalBiaya = new NominalBiaya();
															nominalBiaya.setNominal(0.0);
															nominalBiaya.setItemBiayaSekolah(itemBiayaSekolah);
															nominalBiaya.setPengaturanBiaya(
																	tagihan.getPengaturanBiaya());
															nominalBiaya.setSiswa(tagihan.getSiswa());
															nominalBiaya.setCalonSiswa(tagihan.getCalonSiswa());
															session.save(nominalBiaya);
														}

														nominalBiaya.setPengaturanBiayaItemBiaya(tagihan
																.getNominalBiaya().getPengaturanBiayaItemBiaya());

														int bayar = nominalBiaya.getDibayarSebayak();

														nominalBiaya.setDibayarSebayakManual(bayar + 1);
														nominalBiaya.setDibayarSebayak(bayar + 1);

														Common.refreshUpdate(session, nominalBiaya);
														session.flush();

														Integer tahunbulan = nominalBiaya.getTahunbulan() != null
																? nominalBiaya.getTahunbulan()
																: PembayaranSiswa.convert(
																		nominalBiaya.getPengaturanBiaya()
																				.getJenisBiayaSekolah().getUntukTahun(),
																		nominalBiaya.getPengaturanBiaya()
																				.getJenisBiayaSekolah()
																				.getUntukBulan());

														Integer bayarKe = nominalBiaya.getDibayarSebayak();
														kodeUnik = Tagihan.genCode(itemBiayaSekolah,
																nominalBiaya.getPengaturanBiaya(), tahunbulan,
																nominalBiaya.getSiswa(), nominalBiaya.getCalonSiswa(),
																bayarKe);

														Tagihan tagihan = ((Tagihan) session
																.createCriteria(Tagihan.class)
																.add(Restrictions.eq("kodeUnik", kodeUnik))
																.addOrder(Order.asc("pembayaranSiswaDetail"))
																.addOrder(Order.desc("nominal")).setMaxResults(1)
																.uniqueResult());

//												System.out.println("siswa " + nominalBiaya.getSiswa()
//														+ ", pembayaranTerakhir => " + tahunbulan + ", kodeUnik "
//														+ kodeUnik + ", tagihan " + tagihan);
														PembayaranSiswaDetail pembayaranSiswaDetail = null;
														if (tagihan == null) {
															try {

																pembayaranSiswaDetail = (PembayaranSiswaDetail) session
																		.createCriteria(PembayaranSiswaDetail.class)

																		.createAlias("tagihan", "tagihan")
																		.add(Restrictions.eq("tagihan.bayarKe",
																				bayarKe))
																		.add(Restrictions.eq("nominalBiaya",
																				nominalBiaya))

																		.add(Restrictions.eq("itemBiayaSekolah",
																				nominalBiaya.getItemBiayaSekolah()))
																		.createCriteria("pembayaranSiswa")
																		.add(Restrictions.eq("siswa",
																				nominalBiaya.getSiswa()))
																		.add(Restrictions.eq("jenisBiayaSekolah",
																				nominalBiaya.getPengaturanBiaya()
																						.getJenisBiayaSekolah()))
																		.add(Restrictions.or(
																				Restrictions.isNull("bulan"),
																				Restrictions.eq("bulan",
																						nominalBiaya
																								.getPengaturanBiaya()
																								.getJenisBiayaSekolah()
																								.getUntukBulan())))
																		.add(Restrictions.or(
																				Restrictions.isNull("tahun"),
																				Restrictions.eq("tahun",
																						nominalBiaya
																								.getPengaturanBiaya()
																								.getJenisBiayaSekolah()
																								.getUntukTahun())))

																		.setMaxResults(1).addOrder(Order.desc("id"))
																		.uniqueResult();

																if (pembayaranSiswaDetail == null) {
																	pembayaranSiswaDetail = (PembayaranSiswaDetail) session
																			.createCriteria(PembayaranSiswaDetail.class)

																			.createAlias("tagihan", "tagihan")
																			.add(Restrictions.eq("tagihan.bayarKe",
																					bayarKe))
																			.add(Restrictions.eq("nominalBiaya",
																					nominalBiaya))

																			.add(Restrictions.eq("itemBiayaSekolah",
																					nominalBiaya.getItemBiayaSekolah()))
																			.createCriteria("pembayaranSiswa")
																			.add(Restrictions.eq("siswa",
																					nominalBiaya.getSiswa()))
																			.add(Restrictions.eq("jenisBiayaSekolah",
																					nominalBiaya.getPengaturanBiaya()
																							.getJenisBiayaSekolah()))

																			.setMaxResults(1).addOrder(Order.desc("id"))
																			.uniqueResult();
																}
																if (pembayaranSiswaDetail == null
																		|| pembayaranSiswaDetail.getTagihan() == null) {
																	tagihan = new Tagihan();
																	tagihan.setNominalBiaya(nominalBiaya);
																	tagihan.setBulan(nominalBiaya.getPengaturanBiaya()
																			.getJenisBiayaSekolah().getUntukBulan());
																	tagihan.setTahun(nominalBiaya.getPengaturanBiaya()
																			.getJenisBiayaSekolah().getUntukTahun());
																	tagihan.setPembayaranSiswaDetail(
																			pembayaranSiswaDetail);
																	tagihan.setSiswa(nominalBiaya.getSiswa());
																	tagihan.setItemBiayaSekolah(itemBiayaSekolah);
																	tagihan.setBayarKe(bayarKe);
																	tagihan.setNominal((nominalBiaya
																			.getItemBiayaSekolah().getBolehDiangsur()
																			&& nominalBiaya.getPengaturanBiaya()
																					.getJenisBiayaSekolah()
																					.getBolehAngsurBerapapun()
																							? (dibayar.getValue()
																									/ nominalBiaya
																											.getDibayarSebayak())
																							: dibayar.getValue()));

																	tagihan.setInformasi(informasi.getValue());
																	session.save(tagihan);
																	session.flush();

																	if (tagihan.getDiskonSiswa() != null && !tagihan
																			.getDiskonSiswa().getMemotongTagihan()) {
																		DaftarPengajuanTransfer
																				.simpanDiskonPembayaran(tagihan);
																	}

																} else {
																	tagihan = pembayaranSiswaDetail.getTagihan();
																}
															} catch (Exception e) {
																Common.tampilErrorJikaAdmin(e);
															}
														} else {
															tagihan.setInformasi(informasi.getValue());
															tagihan.setAktif(true);
															tagihan.setNominal((nominalBiaya.getPengaturanBiaya()
																	.getJenisBiayaSekolah().getBolehAngsurBerapapun()
																			? (dibayar.getValue()
																					/ nominalBiaya.getDibayarSebayak())
																			: dibayar.getValue()));
															Common.refreshUpdate(session, tagihan);
															session.flush();
														}

														if (pembayaranSiswaDetail != null
																&& pembayaranSiswaDetail.getId() != null) {
															pembayaranSiswaDetail.setTagihan(tagihan);
															session.update(pembayaranSiswaDetail);
															session.flush();
														}

														Common.createDefaultTimer(new EventListener() {

															@Override
															public void onEvent(Event arg0) throws Exception {
																reloadPembayaranSiswa(s, cs);
															}
														});
													}
												});
												save.setParent(toolbar);
												borderlayout.setParent(addWindow);

												addWindow.onModal();

											}
										});
									}
								}
							}
						}

						row = new MyFormRow();
						row.setParent(rowsDetailBiaya);
						row.setValign("top");
						row.setAttribute("tagihan", tagihan);
						row.setValign("top");
						row.setAttribute("nominalBiaya", tagihan.getNominalBiaya());

						MyCheckboxConfig checkboxConfig = new MyCheckboxConfig(tagihan.getItemBiayaSekolah().getNama()
								+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1
										? " (ke " + tagihan.getBayarKe() + ")"
										: ""));

						listCheck.add(checkboxConfig);
						row.setValign("top");
						row.setAttribute("pilih", checkboxConfig);
						row.appendChild(checkboxConfig);

						checkboxConfig.addEventListener("onClick", hitungNominalBiaya);

						String ta = (tagihan.getTahun() == null ? "" : tagihan.getTahun().toString())
								+ (tagihan.getBulan() == null ? "" : "-" + tagihan.getBulan().toString());

						new Label(ta).setParent(row);

						if (tagihan.getItemBiayaSekolah().getNilaiBiayaBisaDiubahSaatPembayaran()) {
							MyDoublebox nominal = new MyDoublebox(tagihan.getNominal());
							nominal.setWidth("90%");
							row.appendChild(nominal);
							row.setValign("top");
							row.setAttribute("nominal", nominal);
							nominal.addEventListener("onChange", hitungNominalBiaya);
						} else {
							row.appendChild(new Label(
									Common.numberFormat.get().format((tagihan.getNominal() + tagihan.getDenda()))));
						}
						if (checkboxConfig.isChecked()) {
							total += (tagihan.getNominal() + tagihan.getDenda());
						}

						if (Common.getApakahAdmin()) {
							final MyCheckboxConfig checkbox = new MyCheckboxConfig("");
							checkbox.setChecked(((tagihan.getAktif() && !tagihan.ambilBukanTagihanData())
									&& !tagihan.getNominalBiaya().getBukanTagihan())
									|| (tagihan.getAktifkanmanual() != null && tagihan.getAktifkanmanual()));
							checkbox.setParent(row);
							row.setValign("top");
							row.setAttribute("checkbox", checkbox);
							checkbox.addEventListener("onCheck", new EventListener() {

								@Override
								public void onEvent(Event arg0) throws Exception {
									final boolean cek = checkbox.isChecked();
									// Muat ulang instance MANAGED (objek dari render sudah detached)
									// agar uncheck pasti tersimpan.
									Session s = HibernateUtil.currentSession();
									Tagihan t = tagihan.getId() == null ? tagihan
											: (Tagihan) s.get(Tagihan.class, tagihan.getId());
									if (t == null) t = tagihan;
									t.setAktif(cek);
									t.setAktifkanmanual(cek);   // true = paksa AKTIF
									t.setNonaktifManual(!cek);  // true = paksa NON-AKTIF (revisi keliru)
									Common.refreshSaveOrUpdate(s, t);
									try { s.flush(); } catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/sekolah/PembayaranSiswaAction.java:1982"); }
								}
							});
						} else {
							row.appendChild(new MyLabelBoldAja(""));
						}

					}
				}
			}
			MyFormRow foot = new MyFormRow();
			foot.setParent(rowsDetailBiaya);
			foot.appendChild(new MyLabelBoldAja("Tagihan Total"));
			foot.appendChild(new MyLabelBoldAja(""));
			foot.appendChild(totalLabel = new MyLabelBoldAja(Common.numberFormat.get().format(total)));
			foot.appendChild(new MyLabelBoldAja(""));

			foot = new MyFormRow();
			foot.setParent(rowsDetailBiaya);
			foot.appendChild(new MyLabelBoldAja("Tabungan Siswa saat ini"));
			foot.appendChild(new MyLabelBoldAja(""));
			foot.appendChild(tabunganSaatIni = new MyLabelBoldAja(Common.numberFormat.get().format(sisaDeposit)));
			foot.appendChild(new MyLabelBoldAja(""));

			foot = new MyFormRow();
			foot.setParent(rowsDetailBiaya);

			boolean tampilkan_tabungan_siswa = Common.bolehKonfigurasi("tampilkan_tabungan_siswa");
			Hbox hbox = new Hbox();
			hbox.appendChild(new MyLabelBoldAja("Tambahan Tabungan"));
			hbox.appendChild(new Space());
			MyButtonConfig sesuaikan;
			hbox.appendChild(sesuaikan = new MyButtonConfig("Samakan dg tagihan"));
			sesuaikan.setStyle("font-size:7px;font-weight: bolder;");
			foot.appendChild(hbox);
			foot.appendChild(new MyLabelBoldAja(""));
			foot.appendChild(deposit = new MyDoublebox(
					pembayaranSiswa.getId() != null ? pembayaranSiswa.getTambahanDeposit() : 0.0));
			foot.appendChild(new MyLabelBoldAja(""));

			deposit.setWidth("90%");
			deposit.setDisabled(!tampilkan_tabungan_siswa);

			foot = new MyFormRow();
			foot.setParent(rowsDetailBiaya);
			foot.appendChild(new MyLabelBoldAja("Total Tabungan"));
			foot.appendChild(new MyLabelBoldAja(""));
			foot.appendChild(
					sisaDepositdata = new MyLabelBoldAja(Common.numberFormat.get().format(sisaDepositDitambah)));
			foot.appendChild(new MyLabelBoldAja(""));

			foot = new MyFormRow();
			foot.setParent(rowsDetailBiaya);
			foot.appendChild(new MyLabelBoldAja("Tabungan Ditambah Deposit"));
			foot.appendChild(new MyLabelBoldAja(""));
			foot.appendChild(sisaDepositdatamenjadi = new MyLabelBold(
					Common.numberFormat.get().format(sisaDepositDitambah - total)));
			foot.appendChild(new MyLabelBoldAja(""));

			final EventListener eventListener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					sisaDepositDitambah = (deposit.getValue() == null ? 0.0 : deposit.getValue()) + sisaDeposit;
					sisaDepositdata.setValue(Common.numberFormat.get().format(sisaDepositDitambah));

					sisaDepositMenjadi = sisaDepositDitambah - total;
					sisaDepositdatamenjadi.setValue(Common.numberFormat.get().format(sisaDepositMenjadi));

				}
			};

			eventListener.onEvent(null);
			deposit.addEventListener("onChange", eventListener);

			sesuaikan.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					deposit.setValue(total);
					eventListener.onEvent(null);
				}
			});

			EventListener listener = new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					for (MyCheckboxConfig checkboxConfig : listCheck) {
						checkboxConfig.setChecked(semua.isChecked());
					}
					hitungNominalBiaya.onEvent(arg0);
				}
			};

			semua.addEventListener("onClick", listener);

			if (Common.getApakahAdmin()) {
				foot = new MyFormRow();
				foot.setParent(rowsDetailBiaya);
			}
		}

		LayoutRegion myEast;

		if (pembayaranSiswa.getId() == null) {
			myEast = new East();
			myEast.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(myEast, true);
			myEast.setWidth("65%");
		} else {
			myEast = new Center();
			myEast.setParent(borderlayout);
			ais.ui.util.ZkCompat.setFlex(myEast, true);
		}

		Borderlayout myBorderlayout = new ais.ui.util.MyBorderlayout();
		myBorderlayout.setParent(myEast);

		Center dataCenter = new Center();
		dataCenter.setTitle("Info Pembayaran Terakhir Siswa");
		dataCenter.setParent(myBorderlayout);

		grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(dataCenter);
		grid.setWidth("100%");
		grid.setHeight("100%");
		grid.setSclass("dgrid");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig("Tanggal/Waktu");
		column.setParent(columns);

		column = new MyColumnConfig("Tahun/Bulan");
		column.setParent(columns);
		column.setWidth("10%");

		column = new MyColumnConfig("Item Biaya");
		column.setParent(columns);

		column = new MyColumnConfig("Cara Pembayaran");
		column.setParent(columns);

		column = new MyColumnConfig("Nominal Pembayaran");
		column.setParent(columns);
		column.setAlign("right");
		column.setWidth("20%");

		column = new MyColumnConfig("Hapus");
		column.setParent(columns);
		column.setWidth("15%");

		Rows rows = new Rows();
		rows.setParent(grid);

		Session session = HibernateUtil.currentSession();
		List<Tagihan> tagihans2 = session.createCriteria(Tagihan.class)
				.add(Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))
				.createAlias("pembayaranSiswaDetail", "pembayaranSiswaDetail")
				.add(Restrictions.isNotNull("pembayaranSiswaDetail.pembayaranSiswa"))
				.addOrder(Order.desc("pembayaranSiswaDetail.id"))
				.createAlias("pembayaranSiswaDetail.pembayaranSiswa", "pembayaranSiswa")

				.add(Restrictions.or(Restrictions.eq("pembayaranSiswa.siswa", s),
						Restrictions.eq("pembayaranSiswa.calonSiswa", cs)))
				.add(jenisBiaya == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("pembayaranSiswa.jenisBiayaSekolah", jenisBiaya))
				.addOrder(Order.desc("pembayaranSiswaDetail.id")).list();

		for (final Tagihan tagihan : tagihans2) {
			try {
				final PembayaranSiswaDetail pembayaranSiswaDetail = tagihan.getPembayaranSiswaDetail();
				MyFormRow row = new MyFormRow();
				row.setValign("top");

				row.appendChild(new Label(
						Common.dateFormat3.get().format(pembayaranSiswaDetail.getPembayaranSiswa().getTanggal())));

				String ta = (tagihan.getTahun() == null ? "" : tagihan.getTahun().toString())
						+ (tagihan.getBulan() == null ? "" : "-" + tagihan.getBulan().toString());

				row.appendChild(new Label(ta));
				row.appendChild(new Label(pembayaranSiswaDetail.getItemBiayaSekolah().getNama()
						+ (tagihan.getNominalBiaya().getDibayarSebayak() > 1 ? " (ke " + tagihan.getBayarKe() + ")"
								: "")));
				row.appendChild(
						new Label(pembayaranSiswaDetail.getPembayaranSiswa().getAkunPembayaranSiswa().getNama()));
				row.appendChild(new Label(Common.numberFormat.get().format(pembayaranSiswaDetail.getNominal())));

				Hbox hbox1 = new Hbox();
				hbox1.setParent(row);

				if (tagihan.getPengaturanBiaya().getJenisBiayaSekolah().getGunakanCalonSiswa()) {

				} else {

					Toolbarbutton toolbarbutton = new MyToolbarbuttonConfig("", "/img/Finance-Invoice-icon.png");
					toolbarbutton.setParent(hbox1);

					toolbarbutton.addEventListener("onClick", new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							final MyWindow addWindow = new MyWindow("Tagihan", "none", false);
							page.getFirstRoot().appendChild(addWindow);
							addWindow.setHeight("95%");
							addWindow.setWidth("90%");

							Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();
							Center center = new Center();
							center.setParent(borderlayout);
							ais.ui.util.ZkCompat.setFlex(center, true);

							new DetailTagihanSiswaHelper(tagihan.getSiswa(), edit, approve)
									.display(tagihan.getPengaturanBiaya(), center);

							South south = new South();
							ais.ui.util.ZkCompat.setFlex(south, true);
							south.setParent(borderlayout);

							Toolbar toolbar = new Toolbar();
							// toolbar.setHeight("25px");
							toolbar.setParent(south);
							MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Tutup", "/img/cancel.gif");
							cancel.setTooltiptext("Tutup");
							cancel.addEventListener("onClick", new EventListener() {
								@Override
								public void onEvent(Event event) throws Exception {
									addWindow.detach();
									reloadPembayaranSiswa(s, cs);
								}
							});
							cancel.setParent(toolbar);

							borderlayout.setParent(addWindow);

							addWindow.onModal();
						}
					});

				}

				MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
				button.setTooltiptext("Hapus Data");
				button.setVisible(delete);
				button.addEventListener("onClick", new EventListener() {
					@Override
					public void onEvent(Event event) throws Exception {
						MyMessageboxConfig.show("Apakah yakin ingin menghapus pembayaran ini ?", "Pertanyaan",
								MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
								new EventListener() {

									@Override
									public void onEvent(Event event) throws Exception {
										int i = Integer.parseInt(event.getData().toString());
										if (i == MyMessageboxConfig.OK) {
											try {

												Common.refreshDelete(pembayaranSiswaDetail);

												reloadPembayaranSiswa(s, cs);
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
				button.setParent(hbox1);

				row.setParent(rows);
			} catch (Exception e) {
				ais.common.Common.tampilErrorJikaAdmin(e);
			}

		}

		if (s != null && s.getId() != null) {
			South dataSouth = new South();
			dataSouth.setTitle("Informasi Tabungan Terakhir Siswa");
			dataSouth.setParent(myBorderlayout);
			dataSouth.setHeight("40%");

			grid = new MyGrid();
			grid.setWidth("100%");
			grid.setParent(dataSouth);
			grid.setWidth("100%");
			grid.setHeight("100%");
			grid.setSclass("dgrid");

			columns = new Columns();
			columns.setParent(grid);

			column = new MyColumnConfig("Tanggal/Waktu");
			column.setParent(columns);

			column = new MyColumnConfig("Cara Pembayaran");
			column.setParent(columns);

			column = new MyColumnConfig("Nominal Deposit");
			column.setParent(columns);
			column.setAlign("right");
			column.setWidth("30%");

			rows = new Rows();
			rows.setParent(grid);

			List<DepositSiswa> depositSiswas = session.createCriteria(DepositSiswa.class).addOrder(Order.desc("id"))
					.add(Restrictions.eq("siswa", s)).list();

			for (DepositSiswa depositSiswa : depositSiswas) {
				MyFormRow row = new MyFormRow();
				row.setValign("top");
				row.setParent(rows);
				row.appendChild(new Label(Common.dateFormat3.get().format(depositSiswa.getWaktu())));

				row.appendChild(new Label(depositSiswa.getAkunPembayaranSiswa().getNama()));
				row.appendChild(new Label(Common.numberFormat.get().format(depositSiswa.getNominal())));
			}
		}
	}

	@SuppressWarnings({})
	private void reloadPembayaran() throws Exception {
		Common.clear(east);

		if (pembayaranCalonSiswa) {
			CalonSiswa cs = (CalonSiswa) calonSiswa.getAttribute("calonSiswa");
			if (cs == null) {
				east.appendChild(new MyLabelBolder("Calon Siswa harus dipilih"));
				return;
			} else {
				reloadPembayaranSiswa(cs.getSiswa(), cs);
			}
		} else {
			Siswa s = (Siswa) siswa.getAttribute("siswa");
			if (s == null) {
				east.appendChild(new MyLabelBolder("Siswa harus dipilih"));
				return;
			} else {
				reloadPembayaranSiswa(s, null);
			}
		}
	}

	private void init(final PembayaranSiswa pembayaranSiswa) throws Exception {
		this.pembayaranSiswa = pembayaranSiswa;
		addWindow.setTitle(pembayaranSiswa.getId() == null ? "Tambah Pembayaran Siswa" : "Ubah Pembayaran Siswa");
		Common.clear(addWindow);
		Borderlayout borderlayout = new ais.ui.util.MyBorderlayout();

		east = new East();
		east.setParent(borderlayout);
		east.setWidth("75%");

		Center center = new Center();
		center.setParent(borderlayout);
		ais.ui.util.ZkCompat.setFlex(center, true);
		MyGrid grid = new MyGrid();
		grid.setWidth("100%");
		grid.setParent(center);
		grid.setWidth("100%");
		grid.setHeight("100%");

		Columns columns = new Columns();
		columns.setParent(grid);

		MyColumnConfig column = new MyColumnConfig();
		column.setParent(columns);
		column.setWidth("30%");

		column = new MyColumnConfig();
		column.setParent(columns);

		Rows rows = new Rows();
		rows.setParent(grid);

		MyFormRow row = new MyFormRow();
		row.setValign("top");
		row.setVisible(!pembayaranCalonSiswa);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Siswa *"));
		row.appendChild(siswa = new AmbilDataSiswaBanbox());
		siswa.setAttribute("siswa", pembayaranSiswa.getSiswa());
		siswa.setValue(pembayaranSiswa.getSiswa() == null ? "" : pembayaranSiswa.getSiswa().getNamaSiswa());
		siswa.setWidth("90%");

		row = new MyFormRow();
		row.setVisible(pembayaranCalonSiswa);
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Pilih Calon Siswa *"));
		row.appendChild(calonSiswa = new AmbilDataCalonSiswaBanbox());
		calonSiswa.setAttribute("calonSiswa", pembayaranSiswa.getCalonSiswa());
		calonSiswa.setValue(
				pembayaranSiswa.getCalonSiswa() == null ? "" : pembayaranSiswa.getCalonSiswa().getNamaSiswa());
		calonSiswa.setWidth("90%");

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Jenis Pembayaran *"));
		row.appendChild(jenisBiayaSekolah = new Combobox());
		jenisBiayaSekolah.setWidth("90%");
		jenisBiayaSekolah.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Cara Pembayaran *"));
		row.appendChild(akunPembayaranSiswa = new Combobox());
		akunPembayaranSiswa.setWidth("90%");
		akunPembayaranSiswa.setReadonly(true);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Tanggal dan Waktu Pembayaran *"));
		row.appendChild(tanggal = new MyDatebox(pembayaranSiswa.getTanggal()));
		tanggal.setFormat(Common.dateFormat3.get().toPattern());
		tanggal.setWidth("90%");
		tanggal.setReadonly(true);

		MyFormRow rowBulan = new MyFormRow();
		rowBulan.setParent(rows);
		rowBulan.setStyle("border:0px;background: transparent;");
		rowBulan.appendChild(new Label(ais.common.Common.getBahasaConfig("Sd. Bulan *")));
		rowBulan.appendChild(bulan = new Combobox());
		bulan.setReadonly(true);

		MyFormRow rowTahun = new MyFormRow();
		rowTahun.setParent(rows);
		rowTahun.setStyle("border:0px;background: transparent;");
		rowTahun.appendChild(new Label(ais.common.Common.getBahasaConfig("Sd. Tahun *")));
		rowTahun.appendChild(tahun = new Combobox());
		tahun.setReadonly(true);

		Comboitem comboitem;
		for (int i = 0; i < 12; i++) {
			comboitem = new Comboitem(Common.BULAN[i]);
			comboitem.setValue(i + 1);
			bulan.appendChild(comboitem);
		}

		Common.selectComboItem(bulan, pembayaranSiswa.getBulan());

		Integer currTahun = ais.ui.util.WaktuUtil.getCalendar().get(Calendar.YEAR);
		for (int i = currTahun - 10; i < currTahun + 10; i++) {
			comboitem = new Comboitem(i + "");
			comboitem.setValue(i);
			tahun.appendChild(comboitem);
		}

		Common.selectComboItem(tahun, pembayaranSiswa.getTahun());

		final EventListener eventListener = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				Sekolah s = null;

				CalonSiswa cal = (CalonSiswa) calonSiswa.getAttribute("calonSiswa");
				if (cal != null) {
					s = cal.getSekolah();
				} else {
					Siswa ss = (Siswa) siswa.getAttribute("siswa");
					s = ss == null ? null : ss.getSekolah();
				}

				Common.insertComboDanSemua(jenisBiayaSekolah, new String[] { "kode", "nama", "periode" }, "sekolah",
						JenisBiayaSekolah.class,
						Restrictions.and(Restrictions.eq("gunakanCalonSiswa", pembayaranCalonSiswa), Restrictions.and(
								Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true)))));
				Common.selectComboItem(jenisBiayaSekolah, pembayaranSiswa.getJenisBiayaSekolah());

				Common.insertCombo(akunPembayaranSiswa, new String[] { "nama", "akun", "bank" },
						AkunPembayaranSiswa.class,
						Restrictions.and(Restrictions.or(Restrictions.isNull("sekolah"), Restrictions.eq("sekolah", s)),
								Restrictions.or(Restrictions.isNull("aktif"), Restrictions.eq("aktif", true))));
				Common.selectComboItem(akunPembayaranSiswa, pembayaranSiswa.getAkunPembayaranSiswa());

				reloadPembayaran();
			}
		};

		tahun.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadPembayaran();
			}
		});

		bulan.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadPembayaran();
			}
		});

		if (selectedSiswa != null) {

			siswa.setAttribute("siswa", selectedSiswa);
			siswa.setValue(selectedSiswa == null ? "" : selectedSiswa.getNama());
			siswa.setDisabled(true);
		}

		siswa.setEventListener(eventListener);
		calonSiswa.setEventListener(eventListener);
		tanggal.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				reloadPembayaran();
			}
		});
		eventListener.onEvent(null);

		final EventListener eventListenerJenis = new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {

				JenisBiayaSekolah jbs = (JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null ? null
						: jenisBiayaSekolah.getSelectedItem().getValue());
				if (jbs != null) {

					if (jbs.getUntukTahun() != null) {
						Common.selectComboItem(tahun, jbs.getUntukTahun());
					}

					if (jbs.getUntukBulan() != null) {
						Common.selectComboItem(bulan, jbs.getUntukBulan());
					}
				}

				reloadPembayaran();
			}
		};

		jenisBiayaSekolah.addEventListener("onChange", eventListenerJenis);

		row = new MyFormRow();
		row.setParent(rows);
		row.appendChild(new ais.ui.util.MyLabelConfig("Keterangan"));
		row.appendChild(keterangan = new Textbox(pembayaranSiswa.getKeterangan()));
		keterangan.setWidth("90%");
		keterangan.setRows(3);

		South south = new South();
		ais.ui.util.ZkCompat.setFlex(south, true);
		south.setParent(borderlayout);

		Toolbar toolbar = new Toolbar();
		// toolbar.setHeight("25px");
		toolbar.setParent(south);
		final MyToolbarbuttonConfig cancel = new MyToolbarbuttonConfig("Batal", "/img/cancel.gif");
		cancel.setTooltiptext("Tutup");
		cancel.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				addWindow.setVisible(false);
			}
		});
		cancel.setParent(toolbar);
		MyToolbarbuttonConfig save = new MyToolbarbuttonConfig("Bayar", "/img/save.gif");
		save.setTooltiptext("Simpan");
		save.setVisible(pembayaranSiswa.getId() == null);
		save.addEventListener("onClick", new EventListener() {
			@Override
			public void onEvent(Event event) throws Exception {
				if (onSave(event)) {
					onSearchDefault(null);
					addWindow.setVisible(false);

					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							PembayaranSiswaUtil.cetakStruk(PembayaranSiswaAction.this.pembayaranSiswa);
						}
					});
				}
			}
		});
		save.setParent(toolbar);
		borderlayout.setParent(addWindow);

		Common.createDefaultTimer(new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				eventListener.onEvent(null);
				eventListenerJenis.onEvent(null);

				if (pembayaranSiswa.getId() != null) {
					Common.createDefaultTimer(new EventListener() {

						@Override
						public void onEvent(Event arg0) throws Exception {
							Common.freeze(addWindow, true);
							cancel.setLabel("Tutup");
							cancel.setDisabled(false);
						}
					});
				}
			}
		});

	}

	public boolean onSave(Event event) throws Exception {

		if (total.intValue() == 0) {
			MyMessageboxConfig.show("Pilih tagihan yang akan Anda bayarkan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		if (pembayaranCalonSiswa) {
			if (calonSiswa.getAttribute("calonSiswa") == null) {
				MyMessageboxConfig.show("Calon Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		} else {
			if (siswa.getAttribute("siswa") == null) {
				MyMessageboxConfig.show("Siswa harus diisi", "Peringatan", MyMessageboxConfig.OK,
						MyMessageboxConfig.INFORMATION);
				return false;
			}
		}
		// if (jenisBiayaSekolah.getSelectedItem() == null ||
		// jenisBiayaSekolah.getSelectedItem().getValue() == null) {
		// MyMessageboxConfig.show("Jenis pembayaran harus diisi", "Peringatan",
		// MyMessageboxConfig.OK,
		// MyMessageboxConfig.INFORMATION);
		// return false;
		// }
		if (akunPembayaranSiswa.getSelectedItem() == null || akunPembayaranSiswa.getSelectedItem().getValue() == null) {
			MyMessageboxConfig.show("Cara pembayaran harus diisi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		Integer bln = (Integer) (bulan.getSelectedItem() == null ? null : bulan.getSelectedItem().getValue());
		Integer thn = (Integer) (tahun.getSelectedItem() == null ? null : tahun.getSelectedItem().getValue());

		if (total < 0.01) {
			MyMessageboxConfig.show("Tagihan tidak ditemukan", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}
		if (Common.bolehKonfigurasi("jika_tabungan_minus_tidak_boleh_membayar", Konfigurasi.TIDAK_AKTIF) && sisaDepositMenjadi < -0.1) {
			MyMessageboxConfig.show("Tabungan tidak mencukupi", "Peringatan", MyMessageboxConfig.OK,
					MyMessageboxConfig.INFORMATION);
			return false;
		}

		Session session = HibernateUtil.currentSession();

		if (pembayaranSiswa.getId() != null) {
			pembayaranSiswa = (PembayaranSiswa) session.load(PembayaranSiswa.class, pembayaranSiswa.getId());
		} else {
			pembayaranSiswa = new PembayaranSiswa();
		}
		pembayaranSiswa.setBulan(bln);
		pembayaranSiswa.setTahun(thn);
		pembayaranSiswa.setSiswa((Siswa) siswa.getAttribute("siswa"));
		pembayaranSiswa.setCalonSiswa((CalonSiswa) calonSiswa.getAttribute("calonSiswa"));
		pembayaranSiswa.setJenisBiayaSekolah((JenisBiayaSekolah) (jenisBiayaSekolah.getSelectedItem() == null ? null
				: jenisBiayaSekolah.getSelectedItem().getValue()));
		pembayaranSiswa.setTanggal(tanggal.getValue());
		pembayaranSiswa.setKeterangan(keterangan.getValue());
		pembayaranSiswa.setAkunPembayaranSiswa((AkunPembayaranSiswa) akunPembayaranSiswa.getSelectedItem().getValue());
		pembayaranSiswa.setNominal(total);
		pembayaranSiswa.setTotalDeposit(sisaDepositDitambah);
		pembayaranSiswa.setTambahanDeposit(deposit.getValue());

		if (pembayaranSiswa.getId() == null) {
			session.save(pembayaranSiswa);
		} else {
			Common.refreshUpdate(session, pembayaranSiswa);
		}

		pembayaranSiswa.saveOrUpdateDeposit(session);
		pembayaranSiswa.saveDetail(rowsDetailBiaya, session);
		return true;
	}

	private void initSubCriteria(Criteria criteria, boolean order) {
		Tbmuser tbmuser = Common.getCurrentUser();

		List<Long> anak = tbmuser != null && tbmuser.getOrangTua() != null ? tbmuser.getOrangTua().ambilAnakSiswa()
				: new ArrayList<Long>();

		criteria

				.add(anak.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("siswa.id", anak))

				.add(tbmuser != null && tbmuser.getSiswa() != null && tbmuser.getSiswa().getId() != null ?

						Restrictions.eq("siswa", tbmuser.getSiswa()) :

						tbmuser != null && tbmuser.getCalonSiswa() != null && tbmuser.getCalonSiswa().getId() != null ?

								Restrictions.eq("calonSiswa", tbmuser.getCalonSiswa()) :

								selectedSiswa != null ? Restrictions.eq("siswa", selectedSiswa)
										: Restrictions.sqlRestriction("true"))
				.add(pembayaranCalonSiswa ? Restrictions.isNotNull("calonSiswa") : Restrictions.isNotNull("siswa"));

		if (searchketerangan != null && !searchketerangan.getValue().trim().isEmpty()) {
			criteria.add(Restrictions.ilike("keterangan", searchketerangan.getValue().trim(), MatchMode.ANYWHERE));
		}

		if (searchvalidator != null && !searchvalidator.getValue().trim().isEmpty()) {
			criteria.add(Restrictions.ilike("validator", searchvalidator.getValue().trim(), MatchMode.ANYWHERE));
		}

		if (searchsiswa != null && !searchsiswa.getValue().trim().isEmpty()) {
			criteria.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
					.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)

					.add(Restrictions.or(
							Restrictions.ilike("siswa.nomorIndukNasional", searchsiswa.getValue().trim(),
									MatchMode.ANYWHERE),
							Restrictions.or(
									Restrictions.ilike("calonSiswa.nomorIndukNasional", searchsiswa.getValue().trim(),
											MatchMode.ANYWHERE),
									Restrictions.or(
											Restrictions.ilike("calonSiswa.namaSiswa", searchsiswa.getValue().trim(),
													MatchMode.ANYWHERE),
											Restrictions.or(
													Restrictions.ilike("siswa.namaSiswa", searchsiswa.getValue().trim(),
															MatchMode.ANYWHERE),
													Restrictions.or(
															Restrictions.ilike("siswa.nomorInduk",
																	searchsiswa.getValue().trim(), MatchMode.ANYWHERE),
															Restrictions.ilike("calonSiswa.nomorInduk",
																	searchsiswa.getValue().trim(),
																	MatchMode.ANYWHERE)))))));
		}

		if (order)
			criteria.addOrder(Order.desc("tanggal")).addOrder(Order.desc("id"));
		criteria.add(searchjenis == null || searchjenis.getSelectedItem() == null
				|| searchjenis.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("true")
						: Restrictions.eq("akunPembayaranSiswa", searchjenis.getSelectedItem().getValue()))

				.add(searchsekolah == null || searchsekolah.getSelectedItem() == null
						|| searchsekolah.getSelectedItem().getValue() == null
						|| searchsekolah.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("sekolah", searchsekolah, false))

				.add(searchyayasan == null || searchyayasan.getSelectedItem() == null
						|| searchyayasan.getSelectedItem().getValue() == null
						|| searchyayasan.getSelectedItem().getValue() == null ? Restrictions.sqlRestriction("1=1")
								: CommonSearchFilterHelper.eqSelectedWithId("yayasan", searchyayasan, false))

		;

	}

	@SuppressWarnings("unchecked")
	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();

		PengaturanBiaya pengaturanBiaya = (PengaturanBiaya) (searchjenisPembayaran == null
				|| searchjenisPembayaran.getSelectedItem() == null ? null
						: searchjenisPembayaran.getSelectedItem().getValue());
		List<Long> pemId = new ArrayList<Long>();
		if (pengaturanBiaya != null) {
			pemId = session.createCriteria(PembayaranSiswaDetail.class)
					.setProjection(Projections.groupProperty("pembayaranSiswa.id"))
					.createAlias("nominalBiaya", "nominalBiaya")
					.add(Restrictions.eq("nominalBiaya.pengaturanBiaya", pengaturanBiaya)).list();
		}

		String ta = (String) (searchta == null || searchta.getSelectedItem() == null ? null
				: searchta.getSelectedItem().getValue());

		List<Long> pemIds = new ArrayList<Long>();
		if (ta != null && !ta.trim().isEmpty()) {
			pemIds = session.createCriteria(PembayaranSiswaDetail.class)
					.setProjection(Projections.groupProperty("pembayaranSiswa.id"))
					.createAlias("nominalBiaya", "nominalBiaya")
					.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
					.add(Restrictions.eq("pengaturanBiaya.tahunAjaran", ta)).list();
		}

		JenisBiayaSekolah jenisBiayaSekolah = (JenisBiayaSekolah) (searchjenisBiaya == null
				|| searchjenisBiaya.getSelectedItem() == null ? null : searchjenisBiaya.getSelectedItem().getValue());
		List<Long> pemIdss = new ArrayList<Long>();
		if (jenisBiayaSekolah != null) {
			pemIdss = session.createCriteria(PembayaranSiswaDetail.class)
					.setProjection(Projections.groupProperty("pembayaranSiswa.id"))
					.createAlias("nominalBiaya", "nominalBiaya")
					.createAlias("nominalBiaya.pengaturanBiaya", "pengaturanBiaya")
					.add(Restrictions.eq("pengaturanBiaya.jenisBiayaSekolah", jenisBiayaSekolah)).list();
		}

		// Fallback: 6 bulan ke belakang s/d besok jika datebox tidak ter-wire
		Calendar calStart = ais.ui.util.WaktuUtil.getCalendar();
		calStart.set(Calendar.MONTH, calStart.get(Calendar.MONTH) - 6);
		java.util.Date startDate = (start != null && start.getValue() != null) ? start.getValue() : calStart.getTime();
		Calendar calEnd = ais.ui.util.WaktuUtil.getCalendar();
		calEnd.set(Calendar.DATE, calEnd.get(Calendar.DATE) + 1);
		java.util.Date endDate = (end != null && end.getValue() != null) ? end.getValue() : calEnd.getTime();

		Criteria criteria = session.createCriteria(PembayaranSiswa.class)

				.add(Restrictions.sqlRestriction("date(this_.tanggal_bayar) between date('"
						+ Common.databaseDateFormat.get().format(startDate) + "') and date('"
						+ Common.databaseDateFormat.get().format(endDate) + "')"))

				.add(pemId.isEmpty() && pengaturanBiaya != null ? Restrictions.sqlRestriction("false")
						: pemId.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", pemId))

				.add(pemIdss.isEmpty() && jenisBiayaSekolah != null ? Restrictions.sqlRestriction("false")
						: pemIdss.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", pemIdss))

				.add(pemIds.isEmpty() && (ta != null && !ta.trim().isEmpty()) ? Restrictions.sqlRestriction("false")
						: pemIds.isEmpty() ? Restrictions.sqlRestriction("true") : Restrictions.in("id", pemIds));
		initSubCriteria(criteria, order);
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<PembayaranSiswa> pembayaranSiswa = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(pembayaranSiswa);
		grid.setRowRenderer(new PembayaranSiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
