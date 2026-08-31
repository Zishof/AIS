package ais.action.master.koperasi;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.json.JSONArray;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.Label;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Toolbar;

import ais.action.master.generic.GenericCrudAction;
import ais.action.master.helper.RevisiHelper;
import ais.common.Common;
import ais.database.hibernate.HibernateUtil;
import ais.database.model.inventory.Toko;
import ais.database.model.koperasi.GrupAturanDiskon;
import ais.ui.util.FormBuilder;
import ais.ui.util.MyBorderlayout;
import ais.ui.util.MyCheckboxConfig;
import ais.ui.util.MyDoublebox;
import ais.ui.util.MyMessageboxConfig;
import ais.ui.util.MyRowRenderer;
import ais.ui.util.MyToolbarbuttonConfig;
import ais.ui.util.MyWindow;
import ais.ui.util.ZkCompat;

/** CRUD ZK untuk header dan daftar produk Grup Aturan Diskon. */
public class GrupAturanDiskonAction extends GenericCrudAction<GrupAturanDiskon> {
    private static final long serialVersionUID = 1L;
    private org.zkoss.zul.Textbox nama, keterangan, produkIds, jenisJson, tipeJson, grupEksklusif;
    private Combobox toko, dasar;
    private Checkbox khusus, langsung, aktif, gabung;
    private MyDoublebox persen, nominal, cashback, maksimal;
    private Intbox prioritas;
    private Datebox mulai, selesai;

    protected Class<GrupAturanDiskon> getEntityClass(){ return GrupAturanDiskon.class; }
    protected GrupAturanDiskon createNewEntity(){ return new GrupAturanDiskon(); }
    protected String getWindowTitle(){ return "Grup Aturan Diskon"; }
    protected String getIntroTitle(){ return "Grup Aturan Diskon"; }
    protected String getIntroDescription(){ return "Kelola satu promo untuk banyak produk. Prioritas terbesar dihitung lebih dahulu; secara bawaan promo tidak digabung."; }
    public Criteria initCriteria(boolean order){ Criteria c=HibernateUtil.currentSession().createCriteria(GrupAturanDiskon.class); if(searchaktif.isChecked())c.add(Restrictions.or(Restrictions.isNull("aktif"),Restrictions.eq("aktif",true))); if(order)c.addOrder(Order.desc("prioritas")).addOrder(Order.desc("id")); return c; }
    protected MyRowRenderer createRenderer(){ return new Renderer(); }

    protected void buildFormContent(MyWindow window,final GrupAturanDiskon g)throws Exception{
        org.zkoss.zul.Borderlayout bl=new MyBorderlayout(); org.zkoss.zul.Center center=new org.zkoss.zul.Center();
        center.setStyle("overflow:auto;padding:12px;background:#f0f4f8;"); ZkCompat.setFlex(center,true); center.setParent(bl);
        org.zkoss.zul.Div wrap=new org.zkoss.zul.Div();wrap.setStyle(FormBuilder.STYLE_CARD_WRAP);wrap.setParent(center);
        Grid grid=new Grid();grid.setStyle("border:none;width:100%;");grid.setParent(wrap);Rows rows=new Rows();rows.setParent(grid);FormBuilder fb=new FormBuilder(rows);
        nama=new org.zkoss.zul.Textbox(g.getNamaGrup());nama.setWidth("100%");fb.addRow("Nama Grup *",nama,"Nama promo yang tampil pada kasir dan struk");
        toko=new Combobox();toko.setReadonly(true);toko.setWidth("100%");Common.insertComboDanSemua(toko,"nama",Toko.class,Restrictions.eq("aktif",true));pilihToko(g.getToko());fb.addRow("Toko",toko,"Pilih toko pemilik produk");
        produkIds=new org.zkoss.zul.Textbox(idsProduk(g.getId()));produkIds.setWidth("100%");produkIds.setRows(3);fb.addRow("ID Produk",produkIds,"Pisahkan dengan koma. Hanya produk aktif dari toko yang dipilih yang disimpan.");
        fb.addSectionHeader("Nilai Promo");persen=new MyDoublebox(g.getPersentase());fb.addRow("Persentase (%)",persen,"");nominal=new MyDoublebox(g.getNominal());fb.addRow("Nominal (Rp)",nominal,"");cashback=new MyDoublebox(g.getCashback());fb.addRow("Cashback (Rp per unit)",cashback,"");maksimal=new MyDoublebox(g.getMaksimalPotongan());fb.addRow("Maksimal Potongan",maksimal,"0 berarti tanpa batas");
        langsung=new Checkbox("Potong langsung di struk");langsung.setChecked(Boolean.TRUE.equals(g.getPotonganLangsung()));fb.addRow("Jenis Manfaat",langsung,"Tidak dicentang berarti nilai promo menjadi cashback");
        fb.addSectionHeader("Prioritas & Benturan");prioritas=new Intbox(g.getPrioritas());prioritas.setConstraint("no negative");prioritas.setWidth("100%");fb.addRow("Prioritas",prioritas,"Angka lebih besar dihitung dahulu");gabung=new Checkbox("Boleh digabung");gabung.setChecked(Boolean.TRUE.equals(g.getDapatDigabung()));fb.addRow("Kombinasi",gabung,"Kedua promo harus mengizinkan penggabungan");
        dasar=new Combobox();dasar.setReadonly(true);dasar.setWidth("100%");dasar.appendItem("Setelah diskon sebelumnya").setValue("SETELAH_DISKON");dasar.appendItem("Harga awal").setValue("HARGA_AWAL");dasar.setSelectedIndex("HARGA_AWAL".equals(g.getDasarPerhitungan())?1:0);fb.addRow("Dasar Perhitungan",dasar,"Disarankan setelah diskon sebelumnya");
        grupEksklusif=new org.zkoss.zul.Textbox(g.getGrupEksklusif());grupEksklusif.setWidth("100%");fb.addRow("Grup Eksklusif",grupEksklusif,"Kode sama tidak boleh dipakai bersamaan");
        fb.addSectionHeader("Sasaran & Periode");khusus=new Checkbox("Khusus member");khusus.setChecked(Boolean.TRUE.equals(g.getKhususMember()));fb.addRow("Sasaran",khusus,"Jika dicentang, filter JSON di bawah diterapkan");jenisJson=new org.zkoss.zul.Textbox(g.getJenisMemberJson());jenisJson.setWidth("100%");fb.addRow("Jenis Member (JSON)",jenisJson,"Contoh [1,2]");tipeJson=new org.zkoss.zul.Textbox(g.getTipeMemberJson());tipeJson.setWidth("100%");fb.addRow("Tipe Member (JSON)",tipeJson,"Contoh [3,4]");mulai=new Datebox(g.getTanggalMulai());mulai.setFormat("dd-MM-yyyy");mulai.setWidth("100%");fb.addRow("Mulai",mulai,"");selesai=new Datebox(g.getTanggalSelesai());selesai.setFormat("dd-MM-yyyy");selesai.setWidth("100%");fb.addRow("Selesai",selesai,"");aktif=new Checkbox("Aktif");aktif.setChecked(Boolean.TRUE.equals(g.getAktif()));fb.addRow("Status",aktif,"");keterangan=new org.zkoss.zul.Textbox(g.getKeterangan());keterangan.setRows(3);keterangan.setWidth("100%");fb.addRow("Keterangan",keterangan,"");
        org.zkoss.zul.South south=new org.zkoss.zul.South();ZkCompat.setFlex(south,true);south.setStyle(FormBuilder.STYLE_TOOLBAR_AREA);south.setParent(bl);Toolbar tb=new Toolbar();tb.setParent(south);MyToolbarbuttonConfig cancel=new MyToolbarbuttonConfig("Batal","/img/cancel.gif");cancel.addEventListener("onClick",new EventListener(){public void onEvent(Event e){addWindow.setVisible(false);}});cancel.setParent(tb);MyToolbarbuttonConfig save=new MyToolbarbuttonConfig("Simpan","/img/save.gif");save.addEventListener("onClick",new EventListener(){public void onEvent(Event e)throws Exception{if(onSave(e)){onSearchDefault(null);addWindow.setVisible(false);}}});save.setParent(tb);bl.setParent(window);
    }

    public boolean onSave(Event e)throws Exception{
        if(nama.getValue().trim().length()==0){MyMessageboxConfig.show("Nama grup diskon wajib diisi.","Peringatan",MyMessageboxConfig.OK,MyMessageboxConfig.INFORMATION);return false;}
        Long tokoId=toko.getSelectedItem()==null?null:(Long)((Toko)toko.getSelectedItem().getValue()).getId();if(tokoId==null){MyMessageboxConfig.show("Toko wajib dipilih agar produk tidak tercampur antar toko.","Peringatan",MyMessageboxConfig.OK,MyMessageboxConfig.INFORMATION);return false;}
        Set<Long> ids=parseIds(produkIds.getValue());if(ids.isEmpty()){MyMessageboxConfig.show("Isi minimal satu ID produk.","Peringatan",MyMessageboxConfig.OK,MyMessageboxConfig.INFORMATION);return false;}
        Session s=HibernateUtil.currentSession();GrupAturanDiskon g=currentEntity;if(g.getId()!=null){g=(GrupAturanDiskon)s.load(GrupAturanDiskon.class,g.getId());currentEntity=g;}
        g.setNamaGrup(nama.getValue().trim());g.setKeterangan(keterangan.getValue());g.setToko(tokoId);g.setKhususMember(Boolean.valueOf(khusus.isChecked()));g.setBerlakuSemuaMember(Boolean.valueOf(!khusus.isChecked()));g.setJenisMemberJson(validJson(jenisJson.getValue()));g.setTipeMemberJson(validJson(tipeJson.getValue()));g.setPersentase(persen.getValue());g.setNominal(nominal.getValue());g.setCashback(cashback.getValue());g.setMaksimalPotongan(maksimal.getValue());g.setPotonganLangsung(Boolean.valueOf(langsung.isChecked()));g.setPrioritas(prioritas.getValue()==null?Integer.valueOf(100):prioritas.getValue());g.setDapatDigabung(Boolean.valueOf(gabung.isChecked()));g.setDasarPerhitungan(dasar.getSelectedItem()==null?"SETELAH_DISKON":String.valueOf(dasar.getSelectedItem().getValue()));g.setGrupEksklusif(kosong(grupEksklusif.getValue()));g.setTanggalMulai(mulai.getValue());g.setTanggalSelesai(selesai.getValue());g.setAktif(Boolean.valueOf(aktif.isChecked()));g.setDetailJson(new JSONArray(ids).toString());Common.refreshSaveOrUpdate(s,g);s.flush();
        PreparedStatement cek=s.connection().prepareStatement("SELECT id FROM koperasi.produk WHERE id=? AND toko=? AND COALESCE(aktif,true)");for(Long id:ids){cek.setLong(1,id);cek.setLong(2,tokoId);ResultSet rs=cek.executeQuery();boolean ok=rs.next();rs.close();if(!ok){cek.close();throw new IllegalArgumentException("Produk ID "+id+" tidak ditemukan/aktif pada toko terpilih.");}}cek.close();PreparedStatement del=s.connection().prepareStatement("DELETE FROM koperasi.grup_aturan_diskon_detail WHERE grup_aturan_diskon=?");del.setLong(1,g.getId());del.executeUpdate();del.close();PreparedStatement ins=s.connection().prepareStatement("INSERT INTO koperasi.grup_aturan_diskon_detail(grup_aturan_diskon,produk,aktif,tanggal_dirubah) VALUES(?,?,true,now())");for(Long id:ids){ins.setLong(1,g.getId());ins.setLong(2,id);ins.addBatch();}ins.executeBatch();ins.close();return true;
    }
    private void pilihToko(Long id){if(id==null)return;for(Object o:toko.getItems()){org.zkoss.zul.Comboitem i=(org.zkoss.zul.Comboitem)o;if(i.getValue() instanceof Toko&&id.equals(((Toko)i.getValue()).getId())){toko.setSelectedItem(i);return;}}}
    private String idsProduk(Long id)throws Exception{if(id==null)return "";PreparedStatement p=HibernateUtil.currentSession().connection().prepareStatement("SELECT produk FROM koperasi.grup_aturan_diskon_detail WHERE grup_aturan_diskon=? AND COALESCE(aktif,true) ORDER BY id");p.setLong(1,id);ResultSet r=p.executeQuery();StringBuilder b=new StringBuilder();while(r.next()){if(b.length()>0)b.append(',');b.append(r.getLong(1));}r.close();p.close();return b.toString();}
    private static Set<Long> parseIds(String v){Set<Long>s=new LinkedHashSet<Long>();if(v!=null)for(String x:v.split("[,;\\s]+"))if(x.trim().length()>0)s.add(Long.valueOf(x.trim()));return s;}
    private static String validJson(String v){try{return new JSONArray(v==null||v.trim().length()==0?"[]":v).toString();}catch(Exception e){throw new IllegalArgumentException("Filter member harus berupa JSON array, contoh [1,2].");}}
    private static String kosong(String v){return v==null||v.trim().length()==0?null:v.trim();}
    /**
     * Renderer baris grup aturan diskon. Operasi menampilkan toko, nilai promo, prioritas/kombinasi, status aktif,
     * serta tombol revisi/edit/hapus menggunakan privilege dan lifecycle layar {@link GrupAturanDiskonAction}.
     */
    class Renderer extends MyRowRenderer{public void render(Row row,Object value)throws Exception{GrupAturanDiskon g=(GrupAturanDiskon)value;RevisiHelper.createNewRevisi(GrupAturanDiskon.class,g,g.getNamaGrup()).setParent(row);new Label(String.valueOf(g.getToko())).setParent(row);new Label(g.getPersentase()+"% / Rp "+Common.numberFormat.get().format(g.getNominal())).setParent(row);new Label("Prioritas "+g.getPrioritas()+(Boolean.TRUE.equals(g.getDapatDigabung())?" · Gabung":" · Tunggal")).setParent(row);MyCheckboxConfig c=new MyCheckboxConfig("Aktif");c.setChecked(Boolean.TRUE.equals(g.getAktif()));c.setDisabled(!edit);c.setParent(row);Common.copyEditDeleteButtons(edit,delete,g,GrupAturanDiskonAction.this).setParent(row);}}
}
