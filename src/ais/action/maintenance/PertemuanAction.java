package ais.action.maintenance;

import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.GenericAutowireComposer;

import ais.action.master.helper.PertemuanHelper;
import ais.common.Common;
import ais.common.listener.DataLoader;
import ais.database.model.GeneralValueObject;
import ais.database.model.Pertemuan;
import ais.database.model.Tbmuser;
import ais.database.model.TugasKelompok;
import ais.database.model.TugasPertemuan;

public class PertemuanAction extends GenericAutowireComposer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5779730267402400328L;

	public void doAfterCompose(Component comp) throws Exception {
		// TODO Auto-generated method stub
		super.doAfterCompose(comp);
		Common.initBahasaParameter(execution.getParameter("lang"));
		Pertemuan pertemuan = (Pertemuan) GeneralValueObject.ambilData(Pertemuan.class, execution.getParameter("id"));
		Tbmuser tbmuser = Common.getCurrentUser();
		if (pertemuan != null && tbmuser != null) {

//			if (pertemuan.getKomponenDataProdukKursus() != null) {
//
//				Session session = HibernateUtil.currentSession();
//				List<PesertaKursus> mahasiswas = ConstantValues.simpleList(session.createCriteria(ProdukPeserta.class)
//						.setProjection(Projections.property("pesertaKursus.id"))
//						.add(Restrictions.ilike("komponens",
//								"," + pertemuan.getKomponenDataProdukKursus().getId() + ",", MatchMode.ANYWHERE))
//
//						.createAlias("pesertaKursus", "pesertaKursus")
//						.add(tbmuser.getSiswa() != null ? Restrictions.eq("pesertaKursus.siswa", tbmuser.getSiswa())
//								: tbmuser.getMahasiswa() != null
//										? Restrictions.eq("pesertaKursus.mahasiswa", tbmuser.getMahasiswa())
//										: Restrictions.eq("pesertaKursus.tbmuser", tbmuser)),
//						PesertaKursus.class, false);
//
//				if (mahasiswas.isEmpty()) {
//
//					new MyLabelBoldMerah("Anda belum menjadi peserta di \""
//							+ pertemuan.getKomponenDataProdukKursus().getKomponenProdukKursus() + "\" ini.")
//							.setParent(page.getFirstRoot());
//
//					return;
//				}
//
//			}

			TugasPertemuan tugasPertemuan = null;
			if (execution.getParameter("tugas") != null) {
				for (TugasPertemuan tgs : pertemuan.ambilTugasPertemuanTotal().values()) {
					if (tgs.getId().equals(Long.parseLong(execution.getParameter("tugas")))) {
						tugasPertemuan = tgs;
						break;
					}
				}
			}

			TugasKelompok tugasKelompok = null;
			if (execution.getParameter("tugas_kelompok") != null) {
				for (TugasKelompok tgs : pertemuan.ambilTugasKelompokTotal().values()) {
					if (tgs.getId().equals(Long.parseLong(execution.getParameter("tugas_kelompok")))) {
						tugasKelompok = tgs;
						break;
					}
				}
			}

			boolean tampilSelesai = execution.getParameter("tampilSelesai") == null ? true
					: Boolean.parseBoolean(execution.getParameter("tampilSelesai"));

			new PertemuanHelper(tbmuser == null ? null : tbmuser.getMahasiswa(),
					tbmuser == null ? null : tbmuser.getBiodataCalonMahasiswa(), tampilSelesai)
					.display(pertemuan, new DataLoader() {

						@Override
						public void loadData(Object value) {
							Clients.evalJavaScript("window.close();");
						}
					}, Integer.parseInt(execution.getParameter("info")), tugasPertemuan, tugasKelompok, null, null,
							null);

		}
	}

}
