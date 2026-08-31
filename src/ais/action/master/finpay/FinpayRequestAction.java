package ais.action.master.finpay;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import ais.ui.util.MyDetail;
import ais.ui.util.MyGrid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Row;

import org.zkoss.zul.Rows;
import org.zkoss.zul.SimpleListModel;
import org.zkoss.zul.Textbox;

import ais.action.servlet.FinPayResponse;
import ais.common.Common;
import ais.common.CommonPrivilages;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.CicilanPembayaran;
import ais.database.model.ItemBiaya;
import ais.database.model.Kegiatan;
import ais.database.model.Mahasiswa;
import ais.database.model.PengaturanPembayaranBulanan;
import ais.database.model.Tbmuser;
import ais.database.model.finpay.FinpayRequest;
import ais.database.model.finpay.FinpayRequestDetail;
import ais.ui.util.DataCriteria;
import ais.ui.util.MyButtonConfig;
import ais.ui.util.MyColumnConfig;
import ais.ui.util.MyDatebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

/**
 * Controller/action ZK untuk finpay request. Tipe ini merupakan titik masuk UI yang menghubungkan
 * event layar dengan perilaku domain yang diwarisi atau dikonfigurasi khusus oleh kelas ini.
 *
 * <p><b>Batas tanggung jawab:</b> perilaku umum, validasi, akses data, serta lifecycle tetap dimiliki {@link
 * GenericAutowireComposer}. Kelas ini hanya boleh memuat perbedaan yang benar-benar spesifik untuk variasi ini;
 * perubahan yang berlaku bagi seluruh keluarga harus ditempatkan di kelas induk agar fungsi tidak bercabang atau
 * tumpang tindih.</p>
 * <p>Perbedaan lokal yang dapat diamati adalah state lokal utama: {@code MyGrid grid}, {@code Paging paging},
 * {@code Textbox searchpaymentCode}, {@code Textbox searchnim}, {@code Textbox searchinvoice}, {@code Combobox
 * tahunAkademik}, {@code Textbox responseCode}, {@code MyToolbarbuttonConfig find}; inisialisasi/lifecycle
 * ({@code doBeforeCompose()}, {@code doAfterCompose()}, {@code initCriteria()}); pembacaan/pencarian ({@code
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
public class FinpayRequestAction extends GenericAutowireComposer implements DataCriteria {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730217402400328L;
	private MyGrid grid;
	private Paging paging;

	private Textbox searchpaymentCode;
	private Textbox searchnim;
	private Textbox searchinvoice;
	private Combobox tahunAkademik;
	private Textbox responseCode;

	private MyToolbarbuttonConfig find;

	private MyDatebox searchmulai;
	private MyDatebox searchsampai;

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
		if (session.getAttribute("usersTemp") == null || !CommonPrivilages.checkPrevilages(CommonPrivilages.READ)) {
			session.removeAttribute("usersTemp");
			Common.goLogoff();
			return;
		}

		Common.generateTahunAjaranDanSemua(tahunAkademik);

		onSearchDefault(null);

		Common.initPaging(paging, new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				onSearchDefault(null);

			}
		});

		MyToolbarbuttonConfig cetakToolbarbutton = Common.cetakData(this, "id", "nama", "tipe", "merchant", "invoice",
				"paymentCode", "mahasiswa", "biodataCalonMahasiswa", "jenisKegiatan", "jadwalPembayaran", "semester",
				"tahunAkademik", "keterangan", "pengurangan", "nilaiBiayaHarusDiBayars", "finpayResponse");
		Common.appendKeToolbar(cetakToolbarbutton, find, comp);
	}

	/**
	 * Renderer lokal untuk layar/komponen {@link FinpayRequestAction}. Kelas ini menerjemahkan satu item data
	 * menjadi baris atau komponen ZK dengan memakai state dan aturan tampilan milik kelas induk.
	 *
	 * <p><b>Scope:</b> setiap instance terikat pada instance {@link FinpayRequestAction} dan dapat mengakses state
	 * kelas induk. Jangan menyimpan atau membagikannya lintas desktop/session.</p>
	 * <p>Kontrak yang tampak dari deklarasi ini meliputi operasi lokal: {@code render}(). Aturan bisnis bersama
	 * tetap berada pada kelas induk atau service yang dipanggilnya.</p>
	 * <p><b>Efek samping:</b> operasi dapat mengubah komponen ZK dan memanggil alur kelas induk. Jalankan pada
	 * event thread dengan konteks pengguna/session aktif; jangan menyalin query atau validasi domain ke
	 * renderer/listener ini.</p>
	 *
	 * @see FinpayRequestAction
	 */
	class FinpayRequestRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final FinpayRequest finpayRequest = (FinpayRequest) arg1;
			final MyDetail detail = new MyDetail();
			detail.setParent(arg0);
			detail.addEventListener("onOpen", new EventListener() {

				@SuppressWarnings("unchecked")
				@Override
				public void onEvent(Event event) throws Exception {
					Common.clear(detail);
					if (detail.isOpen()) {
						Common.clear(detail);

						List<FinpayRequestDetail> finpayRequestDetails = HibernateUtil.currentSession()
								.createCriteria(FinpayRequestDetail.class).add(Restrictions.isNull("idCicilan"))
								.add(Restrictions.eq("finpayRequest", finpayRequest)).list();

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
						for (FinpayRequestDetail finpayRequestDetail : finpayRequestDetails) {
							Row row = new Row();row.setValign("top");
							row.setParent(rows);
							row.appendChild(new ais.ui.util.MyLabelConfig(finpayRequestDetail.getKeterangan()));
							row.appendChild(new ais.ui.util.MyLabelConfig(
									Common.numberFormat.get().format(finpayRequestDetail.getNilai())));
						}
					}
				}
			});

			new Label(finpayRequest.getPaymentCode()).setParent(arg0);
			new Label(finpayRequest.getInvoice()).setParent(arg0);
			if (finpayRequest.getMahasiswa() != null) {
				new Label(finpayRequest.getMahasiswa().toString()).setParent(arg0);
			} else if (finpayRequest.getBiodataCalonMahasiswa() != null) {
				new Label(finpayRequest.getBiodataCalonMahasiswa().toString()).setParent(arg0);
			}
			new Label(finpayRequest.getTanggal_dirubah() == null ? ""
					: Common.dateFormat3.get().format(finpayRequest.getTanggal_dirubah())).setParent(arg0);
			new Label(Common.numberFormat.get().format(finpayRequest.getAmount())).setParent(arg0);
			new Label(
					finpayRequest.getJenisKegiatan() == null ? "" : finpayRequest.getJenisKegiatan().getNamaKegiatan())
					.setParent(arg0);
			new Label(finpayRequest.getTahunAkademik() + "-" + finpayRequest.getSemester()).setParent(arg0);
			new Label(finpayRequest.getFinpayResponse() == null ? ""
					: finpayRequest.getFinpayResponse().getPaymentSource()).setParent(arg0);
			new Label(finpayRequest.getFinpayResponse() == null ? "" : finpayRequest.getFinpayResponse().getLogNo())
					.setParent(arg0);
			new Label(
					finpayRequest.getFinpayResponse() == null ? "" : finpayRequest.getFinpayResponse().getResultCode())
					.setParent(arg0);
			new Label(
					finpayRequest.getFinpayResponse() == null ? "" : finpayRequest.getFinpayResponse().getResultDesc())
					.setParent(arg0);

			if (finpayRequest.getFinpayResponse() == null || finpayRequest.getFinpayResponse().getResultCode() == null
					|| !finpayRequest.getFinpayResponse().getResultCode().equals("00")) {
				MyButtonConfig button = new MyButtonConfig("Cek Pembayaran");
				button.setParent(arg0);

				button.addEventListener("onClick", new EventListener() {

					@Override
					public void onEvent(Event arg0) throws Exception {
						Common.createDefaultTimer(new EventListener() {

							@SuppressWarnings("unchecked")
							@Override
							public void onEvent(Event arg0) throws Exception {

								Session session = HibernateUtil.currentNativeSession();

								session.refresh(finpayRequest);

								Kegiatan kegiatan = FinPayResponse.createKegiatan(finpayRequest, null, session);

								List<CicilanPembayaran> cicilanPembayarans = session
										.createCriteria(CicilanPembayaran.class)
										.add(Restrictions.isNotNull("itemBiaya"))
										.add(Restrictions.eq("kegiatan", kegiatan)).addOrder(Order.asc("tanggal"))
										.addOrder(Order.asc("ke")).list();

								for (CicilanPembayaran cicilanPembayaran : cicilanPembayarans) {
									int count = ((Number) session.createCriteria(FinpayRequestDetail.class)
											.add(Restrictions.eq("finpayRequest", finpayRequest))
											.add(Restrictions.eq("pengaturanPembayaranBulanan",
													cicilanPembayaran.getPengaturanPembayaranBulanan()))
											.setProjection(Projections.rowCount()).uniqueResult()).intValue();
									if (count == 0) {
										FinpayRequestDetail finpayRequestDetail = new FinpayRequestDetail();
										finpayRequestDetail.setFinpayRequest(finpayRequest);
										finpayRequestDetail.setPengaturanPembayaranBulanan(
												cicilanPembayaran.getPengaturanPembayaranBulanan());

										PengaturanPembayaranBulanan pengaturanPembayaranBulanan = cicilanPembayaran
												.getPengaturanPembayaranBulanan();
										ItemBiaya itemBiaya = cicilanPembayaran.getItemBiaya();

										finpayRequestDetail.setIdCicilan(
												cicilanPembayaran == null ? null : cicilanPembayaran.getId());
										finpayRequestDetail.setPengaturanPembayaranBulanan(pengaturanPembayaranBulanan);
										finpayRequestDetail.setItemBiaya(itemBiaya);
										finpayRequestDetail.setKeterangan(cicilanPembayaran.getKeterangan());
										finpayRequestDetail.setNilai(cicilanPembayaran.getNilai());
										finpayRequestDetail.setTanggal(cicilanPembayaran.getTanggal());
										finpayRequestDetail.setKe(0);

										finpayRequestDetail.setDenda(
												cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
														: cicilanPembayaran.getDenda());
										finpayRequestDetail.setNilaiAsli(
												cicilanPembayaran == null || cicilanPembayaran.getId() == null ? null
														: cicilanPembayaran.getNilaiAsli());

										session.getTransaction().begin();
										Common.refreshSaveOrUpdate(session, finpayRequestDetail);
										session.getTransaction().commit();
									}
								}

								session.refresh(finpayRequest);
								session.getTransaction().begin();
								Common.refreshUpdate(session, finpayRequest);
								session.getTransaction().commit();

								JSONObject jsonObject = FinpayBackandProsess.check(finpayRequest, session);
								HibernateUtil.closeSession();

								if (Common.getApakahAdmin())
									MyMessageboxConfig.show(
											"Cek ulang telah dilakukan..\n\n\nInformasi lebih lanjut : \n"
													+ (jsonObject == null ? "" : jsonObject.toString()),
											"Peringatan", MyMessageboxConfig.OK, MyMessageboxConfig.INFORMATION,
											new EventListener() {

												@Override
												public void onEvent(Event arg0) throws Exception {
													onSearchDefault(arg0);
												}
											});
							}

						});
					}
				});
			} else {
				new Label("Pembayaran telah dilakukan / tidak bisa dibatalkan").setParent(arg0);
			}
		}

	}

	public Criteria initCriteria(boolean order) {

		Tbmuser tbmuser = Common.getCurrentUser();
		Mahasiswa mahasiswa = tbmuser == null ? null : tbmuser.getMahasiswa();

		Session session = HibernateUtil.currentSession();
		Criteria criteria = session.createCriteria(FinpayRequest.class)
				.add(mahasiswa != null ? Restrictions.eq("mahasiswa", mahasiswa) : Restrictions.sqlRestriction("true"))
				.createAlias("finpayResponse", "finpayResponse", Criteria.LEFT_JOIN)
				.createAlias("mahasiswa", "mahasiswa", Criteria.LEFT_JOIN)
				.createAlias("biodataCalonMahasiswa", "biodataCalonMahasiswa", Criteria.LEFT_JOIN);
		if (order)
			criteria.addOrder(Order.desc("id"));
		criteria

				.add(searchnim.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.or(Restrictions.ilike("mahasiswa.nim", searchnim.getValue(), MatchMode.ANYWHERE),
								Restrictions.or(
										Restrictions.ilike("biodataCalonMahasiswa.noRegistrasi", searchnim.getValue(),
												MatchMode.ANYWHERE),
										Restrictions.ilike("biodataCalonMahasiswa.noUjian", searchnim.getValue(),
												MatchMode.ANYWHERE))))

				.add((searchmulai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchmulai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_dirubah) >= date('"
								+ Common.databaseDateFormat.get().format(searchmulai.getValue()) + "')")))

				.add((searchsampai == null) ? org.hibernate.criterion.Restrictions.sqlRestriction("1=1") : (searchsampai.getValue() == null ? Restrictions.sqlRestriction("1=1")
						: Restrictions.sqlRestriction("date(this_.tanggal_dirubah) <= date('"
								+ Common.databaseDateFormat.get().format(searchsampai.getValue()) + "')")))

				.add(responseCode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("finpayResponse.resultCode", responseCode.getValue().trim(),
								MatchMode.ANYWHERE))
				.add(tahunAkademik.getSelectedItem() == null || tahunAkademik.getSelectedItem().getValue() == null
						? Restrictions.sqlRestriction("1=1")
						: Restrictions.eq("tahunAkademik", tahunAkademik.getSelectedItem().getValue().toString()))
				.add(searchinvoice.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("invoice", searchinvoice.getValue(), MatchMode.ANYWHERE))
				.add(searchpaymentCode.getValue().trim().isEmpty() ? Restrictions.sqlRestriction("1=1")
						: Restrictions.ilike("paymentCode", searchpaymentCode.getValue(), MatchMode.ANYWHERE));
		return criteria;
	}

	@SuppressWarnings("unchecked")
	public void onSearchDefault(Event event) {
		Common.initPaging(initCriteria(false), paging);

		List<FinpayRequest> finpayRequest = initCriteria(true).setMaxResults(Common.ROWS_COUNT_ON_PAGE)
				.setFirstResult(Common.ROWS_COUNT_ON_PAGE * (paging == null ? 0 : paging.getActivePage())).list();
		ListModel strset = new SimpleListModel(finpayRequest);
		grid.setRowRenderer(new FinpayRequestRenderer());
		grid.setModelCheckMobile(strset);

	}

}
