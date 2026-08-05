package ais.action.master;

import java.util.Date;
import java.util.GregorianCalendar;

import org.hibernate.Session;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.util.GenericAutowireComposer;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModel;
import org.zkoss.zul.Row;

import org.zkoss.zul.SimpleListModel;

import ais.action.master.helper.ProsesUjianHelper;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.common.MemoryDbUtil;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.GeneralValueObject;
import ais.database.model.HasilUjianMahasiswa;
import ais.database.model.Konfigurasi;
import ais.database.model.Tbmuser;
import ais.ui.util.DataSearchDefault;
import ais.ui.util.MyGrid;
import ais.ui.util.MyLabelBold;
import ais.ui.util.MyLabelConfig;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyToolbarbuttonConfig;

public class HasilUjianMahasiswaAction extends GenericAutowireComposer implements DataSearchDefault {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;
	private MyGrid grid;

	private Intbox searchkuota;
	private MyLabelConfig searchkuotalabel;

	private MyLabelBold searchkuotatersedia;

	private boolean delete = false;

	private MyToolbarbuttonConfig reset;

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

		int kuota = 120;
		try {
			kuota = Integer.parseInt(Common.getKonfigurasi("kuota_ujian", kuota + "").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/HasilUjianMahasiswaAction.java:66");
			// TODO: handle exception
		}
		if (searchkuota != null) { searchkuota.setValue(kuota); }

		searchkuota.addEventListener("onChange", new EventListener() {

			@Override
			public void onEvent(Event arg0) throws Exception {
				Konfigurasi konfigurasi = Common.getKonfigurasi("kuota_ujian", "120");
				konfigurasi.setNilai(searchkuota.getValue() + "");
				Session session = HibernateUtil.currentNativeSession();
				session.getTransaction().begin();
				Common.refreshSaveOrUpdate(session, konfigurasi);
				session.getTransaction().commit();

				HibernateUtil.closeSession();
				MemoryDbUtil.getKonfigurasi().put(konfigurasi.getNama(), konfigurasi);

				int kuota = 120;
				try {
					kuota = Integer.parseInt(Common.getKonfigurasi("kuota_ujian", kuota + "").getNilai().trim());
				} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/HasilUjianMahasiswaAction.java:88");
					// TODO: handle exception
				}
				int hasil = kuota - ProsesUjianHelper.kuotaUjian.size();
				searchkuotatersedia.setValue(Common.numberFormat.get().format(hasil < 0 ? 0 : hasil));
			}
		});

		delete = Common.getApakahAdmin();

		Tbmuser tbmuser = Common.getCurrentUser();
		if (tbmuser != null && (!delete || tbmuser.getMahasiswa() != null || tbmuser.getBiodataCalonMahasiswa() != null
				|| tbmuser.getSiswa() != null || tbmuser.getCalonSiswa() != null)) {
			delete = false;
			if (searchkuota != null) { searchkuota.setVisible(false); }
			if (searchkuotalabel != null) { searchkuotalabel.setVisible(false); }
		}

		if (reset != null) { reset.setVisible(delete); }

		onSearchDefault(null);
	}

	class HasilUjianMahasiswaRenderer extends ais.ui.util.MyRowRenderer {

		@Override
		public void render(final Row arg0, Object arg1) throws Exception {
			arg0.setValign("top");
			// TODO Auto-generated method stub
			final String key = (String) arg1;
			HasilUjianMahasiswa hasilUjianMahasiswa = (HasilUjianMahasiswa) GeneralValueObject
					.ambilDataLangsung(HasilUjianMahasiswa.class, key);
			if (hasilUjianMahasiswa == null) {
				arg0.setVisible(false);
				return;
			}

			new Label(hasilUjianMahasiswa.getPertemuanPunyaUjian().toString()).setParent(arg0);

			String yglalu = hasilUjianMahasiswa.retreive();

			String mhs = "";
			if (hasilUjianMahasiswa.getMahasiswa() != null) {
				mhs = hasilUjianMahasiswa.getMahasiswa().getNim() + " - "
						+ hasilUjianMahasiswa.getMahasiswa().getNama();
			} else if (hasilUjianMahasiswa.getBiodataCalonMahasiswa() != null) {
				mhs = hasilUjianMahasiswa.getBiodataCalonMahasiswa().getNoRegistrasi() + " - "
						+ hasilUjianMahasiswa.getBiodataCalonMahasiswa().getNama();
			} else if (hasilUjianMahasiswa.getSiswa() != null) {
				mhs = hasilUjianMahasiswa.getSiswa().getNomorInduk() + " - " + hasilUjianMahasiswa.getSiswa().getNama();
			} else if (hasilUjianMahasiswa.getCalonSiswa() != null) {
				mhs = hasilUjianMahasiswa.getCalonSiswa().getNomorInduk() + " - "
						+ hasilUjianMahasiswa.getCalonSiswa().getNama();
			}

			RevisiHelper.createNewRevisi(HasilUjianMahasiswa.class, hasilUjianMahasiswa, mhs).setParent(arg0);
			new Label(hasilUjianMahasiswa.getMulaiPada() == null ? ""
					: Common.dateFormat3.get().format(hasilUjianMahasiswa.getMulaiPada())).setParent(arg0);

			Date lamaPengerjaan = null;
			try {
				Date sisaWaktuPengerjaan = Common.databaseDateFormat1.get().parse(yglalu);

				new Label(Common.timeFormat1.get().format(sisaWaktuPengerjaan)).setParent(arg0);

				long durationInMillis = hasilUjianMahasiswa.getPertemuanPunyaUjian().getLama().getTime()
						- sisaWaktuPengerjaan.getTime();

				long second = (durationInMillis / 1000) % 60;
				long minute = (durationInMillis / (1000 * 60)) % 60;
				long hour = (durationInMillis / (1000 * 60 * 60)) % 24;

				lamaPengerjaan = new GregorianCalendar(0, 0, 0, (int) hour, (int) minute, (int) second).getTime();

			} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/HasilUjianMahasiswaAction.java:162");
				// TODO: handle exception
			}

			new Label(lamaPengerjaan == null ? "" : Common.timeFormat1.get().format(lamaPengerjaan)).setParent(arg0);

			MyToolbarbuttonConfig button = new MyToolbarbuttonConfig("", "/img/svg/trash.svg");
			button.setTooltiptext("Hapus Data");
			button.setTooltiptext("Hapus data");
			button.setVisible(delete);
			button.addEventListener("onClick", new EventListener() {
				@Override
				public void onEvent(Event event) throws Exception {
					MyMessageboxConfig.show("Apakah yakin ingin menghapus antrian ujian ini ?", "Pertanyaan",
							MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION,
							new EventListener() {

								@Override
								public void onEvent(Event event) throws Exception {
									int i = Integer.parseInt(event.getData().toString());
									if (i == MyMessageboxConfig.OK) {
										try {
											ProsesUjianHelper.kuotaUjian.remove(key);
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
			button.setParent(arg0);

		}

	}

	public void onReset(Event event) throws Exception {

		MyMessageboxConfig.show("Apakah yakin ingin membersihkan semua antrian ujian ?", "Pertanyaan",
				MyMessageboxConfig.OK | MyMessageboxConfig.CANCEL, MyMessageboxConfig.QUESTION, new EventListener() {

					@Override
					public void onEvent(Event event) throws Exception {
						int i = Integer.parseInt(event.getData().toString());
						if (i == MyMessageboxConfig.OK) {
							try {
								ProsesUjianHelper.kuotaUjian.clear();
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

	public void onSearchDefault(Event event) {

		int kuota = 120;
		try {
			kuota = Integer.parseInt(Common.getKonfigurasi("kuota_ujian", kuota + "").getNilai().trim());
		} catch (Exception e) { ais.common.ErrorAuditUtil.record(e, "auto-audit(empty-catch) src/ais/action/master/HasilUjianMahasiswaAction.java:237");
			// TODO: handle exception
		}
		int hasil = kuota - ProsesUjianHelper.kuotaUjian.size();
		searchkuotatersedia.setValue(Common.numberFormat.get().format(hasil < 0 ? 0 : hasil));

		ListModel strset = new SimpleListModel(ProsesUjianHelper.kuotaUjian.toArray());
		grid.setRowRenderer(new HasilUjianMahasiswaRenderer());
		grid.setModelCheckMobile(strset);

	}

}
