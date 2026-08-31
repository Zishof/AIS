package ais.action.master.bri;

import java.util.List;
import java.util.TreeMap;

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
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Hbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.master.KegiatanTemporaryAction.DetailKegiatanTemporaryRenderer;
import ais.action.master.sekolah.util.SekolahUtil;
import ais.action.servlet.Briresponse;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Tbmuser;
import ais.database.model.bri.BriRequest;
import ais.database.model.bri.BriRequestDetail;
import ais.database.model.sekolah.CalonSiswa;
import ais.database.model.sekolah.Sekolah;
import ais.database.model.sekolah.Siswa;
import ais.database.model.sekolah.Yayasan;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyComboitemConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk bri request. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchtrxId}, {@code Textbox searchnim}, {@code Combobox tahunAkademik}, {@code Combobox
 * status}, {@code MyToolbarbuttonConfig find}, {@code MyDatebox searchmulai}; inisialisasi/lifecycle ({@code
 * doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
 * onSearchDefault()}). Bagian lain dari kontrak tetap mengikuti kelas induk atau interface yang disebut di
 * atas.</p>
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
public class BriRequestAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchtrxId;
	private Textbox searchnim;
	private Combobox tahunAkademik;
	private Combobox status;

	private MyToolbarbuttonConfig find;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;
	private CalonSiswa selectedCalonSiswa = null;
	private Sekolah selectedSekolah = null;
	private Yayasan selectedYayasan = null;
	private Siswa siswa = null;
	private Tbmuser tbmuser = null;
	private Mahasiswa mahasiswa = null;
	public static TreeMap<String, String> statses = new TreeMap<String, String>();

	static {
		statses.put("00", "Success");
		statses.put("01", "Belum Bayar");
	}

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

		MyComboitemConfig comboitem = new MyComboitemConfig("Semua Status");
		if (comboitem != null) { comboitem.setValue(null); }
		status.appendChild(comboitem);
		if (status != null) { status.setSelectedItem(comboitem); }

		for (String kode : statses.keySet()) {
			comboitem = new MyComboitemConfig(statses.get(kode));
			comboitem.setValue(kode);
			status.appendChild(comboitem);
		}

		tbmuser = Common.getCurrentUser();

		mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();
		siswa = tbmuser == null ? null : tbmuser.getSiswa();

		if (ExecutionsCtrl.getCurrent().getParameter("calon_siswa") != null) {
			selectedCalonSiswa = (CalonSiswa) HibernateUtil.currentSession().createCriteria(CalonSiswa.class).add(Restrictions.isNotNull("gelombangPendaftaranPsb"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("calon_siswa"))))
					.uniqueResult();
		}

		if (ExecutionsCtrl.getCurrent().getParameter("siswa") != null) {
			siswa = (Siswa) HibernateUtil.currentSession().createCriteria(Siswa.class).add(Restrictions.isNotNull("namaSiswa")).add(Restrictions.ne("namaSiswa","")).add(Restrictions.isNotNull("sekolah"))
					.add(Restrictions.idEq(Long.parseLong(ExecutionsCtrl.getCurrent().getParameter("siswa"))))
					.uniqueResult();
		}

		selectedSekolah = SekolahUtil.getSekolah();
		selectedYayasan = SekolahUtil.getYayasan();

		if (siswa != null) {
			selectedSekolah = siswa.getSekolah();
			selectedYayasan = siswa.getYayasan();
		}

		if (status != null) { status.setReadonly(true); }

		Common.generateTahunAjaranDanSemua(tahunAkademik);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "url", "trxId", "merchant_id",
				"merchant", "response_code", "response_desc", "request", "response", "status", "kodeStatus",
				"mahasiswa", "biodataCalonMahasiswa", "siswa", "calonSiswa", "jenisKegiatan", "jadwalPembayaran",
				"semester", "tahunAkademik", "keterangan", "pengurangan", "nilaiBiayaHarusDiBayars", "briResponse",
				"biayaAdministrasi");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	}

	class BriRequestRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final BriRequest briRequest = (BriRequest) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);

						ais.ui.util.MyDiv groupbox = new ais.ui.util.MyDiv();
						groupbox.setStyle("min-height: 200px;");
						groupbox.setParent(detail);
						MyGrid grid = new MyGrid();
						grid.setParent(groupbox);

						Columns columns = new Columns();
						columns.setParent(grid);

						MyColumnConfig column = new MyColumnConfig("Keterangan");
						column.setParent(columns);
						column.setWidth("80%");

						column = new MyColumnConfig("Nominal");
						column.setParent(columns);
						column.setWidth("20%");

						Rows rows = new Rows();
						rows.setParent(grid);

						HibernateUtil.currentSession().refresh(briRequest);
						if (!briRequest.getKegiatanTemporarys().isEmpty()) {
							List<CicilanPembayaran> cicilanPembayarans = HibernateUtil.currentSession()
									.createCriteria(CicilanPembayaran.class)
									.add(Restrictions.in("kegiatanTemporary", briRequest.getKegiatanTemporarys()))
									.list();

							ListModel strset = new SimpleListModel(cicilanPembayarans);
							grid.setRowRenderer(new DetailKegiatanTemporaryRenderer());
							grid.setModelCheckMobile(strset);
						} else {

							List<BriRequestDetail> briRequestDetails = HibernateUtil.currentSession()
									.createCriteria(BriRequestDetail.class).add(Restrictions.isNull("idCicilan"))
									.add(Restrictions.eq("briRequest", briRequest)).list();
							for (BriRequestDetail briRequestDetail : briRequestDetails) {
								Row row = new Row();row.setValign("top");
								row.setParent(rows);
								row.appendChild(new ais.ui.util.MyLabelConfig(briRequestDetail.getKeterangan()));
								row.appendChild(new ais.ui.util.MyLabelConfig(
										Common.numberFormat.get().format(briRequestDetail.getNilai())));
							}
						}
					}
				}
			});

			new Label(briRequest.getVa()).setParent(arg0);
			if (briRequest.getMahasiswa() != null) {
				new Label(briRequest.getMahasiswa().toString()).setParent(arg0);
			} else if (briRequest.getBiodataCalonMahasiswa() != null) {
				new Label(briRequest.getBiodataCalonMahasiswa().toString()).setParent(arg0);
			} else if (briRequest.getSiswa() != null) {
				new Label(briRequest.getSiswa().getNomorInduk() + "-" + briRequest.getSiswa().getNama())
						.setParent(arg0);
			} else if (briRequest.getCalonSiswa() != null) {
				new Label(briRequest.getCalonSiswa().getNomorInduk() + "-" + briRequest.getCalonSiswa().getNama())
						.setParent(arg0);
			}
			new Label(briRequest.getTanggal_dirubah() == null ? ""
					: Common.dateFormat3.get().format(briRequest.getTanggal_dirubah())).setParent(arg0);
			new Label(Common.numberFormat.get().format(briRequest.getAmount())).setParent(arg0);
			new Label(Common.numberFormat.get().format(briRequest.getBiayaAdministrasi())).setParent(arg0);
			new Label(briRequest.getJenisKegiatan() == null ? briRequest.getKeterangan()
					: briRequest.getJenisKegiatan().getNamaKegiatan()).setParent(arg0);
			new Label((briRequest.getTahunAkademik() == null ? "" : briRequest.getTahunAkademik())
					+ (briRequest.getSemester() == null ? "" : "-" + briRequest.getSemester())).setParent(arg0);

			Hbox hbox = new Hbox();
			hbox.setParent(arg0);

			new Label(briRequest.getStatus()).setParent(hbox);

			MyButtonConfig button = new MyButtonConfig("Cek Pembayaran");
			button.setParent(hbox);

			button.addEventListener("onClick", new EventListener() {

				@Override
				public void onEvent(Event arg0) throws Exception {
					Common.createDefaultTimer(new EventListener() {

						@SuppressWarnings("unchecked")
						@Override
						public void onEvent(Event arg0) throws Exception {

							Session session = HibernateUtil.currentNativeSession();

							session.refresh(briRequest);
							if (!briRequest.getKegiatanTemporarys().isEmpty()) {
								briRequest.setHapusCicilanSebelumnya(true);
								briRequest.setCheckUlang(true);
								session.getTransaction().begin();
								Common.refreshUpdate(session, briRequest);
								session.getTransaction().commit();

								BriBackandProsess.checkSatu(briRequest, session);
								HibernateUtil.closeSession();
								if (Common.getApakahAdmin())
									MyMessageboxConfig.show("Cek ulang telah dilakukan", "Peringatan",
											MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION, new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
							} else {

								if (briRequest.getSiswa() == null && briRequest.getCalonSiswa() == null) {
									Kegiatan kegiatan = Briresponse.createKegiatan(briRequest, session);

									List<CicilanPembayaran> cicilanPembayarans = session
											.createCriteria(CicilanPembayaran.class)
											.add(Restrictions.isNotNull("itemBiaya"))
											.add(Restrictions.eq("kegiatan", kegiatan)).addOrder(Order.asc("tanggal"))
											.addOrder(Order.asc("ke")).list();

									for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
										int count = ((Number) session.createCriteria(BriRequestDetail.class)
												.add(Restrictions.eq("briRequest", briRequest))
												.add(Restrictions.eq("pengaturanPembayaranBulanan",
														cicilanPembayaran.getPengaturanPembayaranBulanan()))
												.setProjection(Projections.rowCount()).uniqueResult()).intValue();
										if (count == 0) {
											BriRequestDetail briRequestDetail = new BriRequestDetail();
											briRequestDetail.setBriRequest(briRequest);
											briRequestDetail.setPengaturanPembayaranBulanan(
													cicilanPembayaran.getPengaturanPembayaranBulanan());

											PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
													.getPengaturanPembayaranBulanan();
											ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();

											briRequestDetail.setIdCicilan(
													cicilanPembayaran == null ? null : cicilanPembayaran.getId());
											briRequestDetail
													.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
											briRequestDetail.setItemBiaya(itemBiaya);
											briRequestDetail.setKeterangan(cicilanPembayaran.getKeterangan());
											briRequestDetail.setNilai(cicilanPembayaran.getNilai());
											briRequestDetail.setTanggal(cicilanPembayaran.getTanggal());
											briRequestDetail.setKe(0);

											briRequestDetail.setDenda(
													cicilanPembayaran == null || cicilanPembayaran.getId() == null
															? null
															: cicilanPembayaran.getDenda());
											briRequestDetail.setNilaiAsli(
													cicilanPembayaran == null || cicilanPembayaran.getId() == null
															? null
															: cicilanPembayaran.getNilaiAsli());

											session.getTransaction().begin();
											Common.refreshSaveOrUpdate(session, briRequestDetail);
											session.getTransaction().commit();
										}
									}

								}

								session.refresh(briRequest);
								briRequest.setHapusCicilanSebelumnya(true);
								briRequest.setCheckUlang(true);
								session.getTransaction().begin();
								Common.refreshUpdate(session, briRequest);
								session.getTransaction().commit();

								BriBackandProsess.checkSatu(briRequest, session);
								HibernateUtil.closeSession();
								if (Common.getApakahAdmin())
									MyMessageboxConfig.show(
											"Cek ulang telah dilakukan, status pembayaran adalah "
													+ briRequest.getStatus(),
											"Informasi", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
							}
						}
					});
				}
			});

		}

	}

	public Criteria initCriteria(boolean order) {

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(BriRequest.class)
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa) : Restrictions.sqlRestriction("true"))
				.add(siswa != null ? Restrictions.eq("siswa", siswa) : Restrictions.sqlRestriction("true"))
				.add(selectedCalonSiswa != null ? Restrictions.eq("calonSiswa", selectedCalonSiswa)
						: Restrictions.sqlRestriction("true"))
				.createAlias("briResponse", "briResponse", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN)
				.createAlias("siswa", "siswa", Criteria.LEFT_JOIN)
				.createAlias("calonSiswa", "calonSiswa", Criteria.LEFT_JOIN)
				.add(selectedSekolah != null && selectedSekolah.getId() != null
						? Restrictions.or(Restrictions.eq("siswa.sekolah", selectedSekolah),
								Restrictions.eq("calonSiswa.sekolah", selectedSekolah))
						: Restrictions.sqlRestriction("true"))
				.add(selectedYayasan != null && selectedYayasan.getId() != null
						? Restrictions.or(Restrictions.eq("siswa.yayasan", selectedYayasan),
								Restrictions.eq("calonSiswa.yayasan", selectedYayasan))
						: Restrictions.sqlRestriction("true"));
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(
								Restrictions.ilike("calonSiswa.nomorInduk", searchnim.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("siswa.nomorInduk", searchnim.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.or(
												Restrictions.ilike("mahasiswa.nim", searchnim.getValue(),
														MatchMode.ANYWHERE),
												Restrictions.or(
														Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi",
																searchnim.getValue(), MatchMode.ANYWHERE),
														Restrictions.ilike("biodataCalonMahasiswa.noUjian",
																searchnim.getValue(), MatchMode.ANYWHERE))))))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_dirubah) >= date('"
								+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "')")))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_dirubah) <= date('"
								+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))

				.add(status.getSelectedItem() == null || status.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("kodeStatus", status.getSelectedItem().getValue()))
				.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue().toString()))
				.add(searchtrxId.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("va", searchtrxId.getValue(), MatchMode.ANYWHERE),
								Restrictions.ilike("trxId", searchtrxId.getValue(), MatchMode.ANYWHERE)));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<BriRequest> briRequest = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(briRequest);
		grid.setRowRenderer(new BriRequestRenderer());
		grid.setModelCheckMobile(strset);

	}

}
