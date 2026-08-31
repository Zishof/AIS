package ais.action.master.sosial.helper;
import ais.common.Common; import ais.database.model.*;
/**
 * Penjaga hak akses (privilege guard) untuk modul Portal Sosial: memeriksa apakah pengguna pada
 * {@link SocialRequestContext} tertentu berhak melakukan satu kapabilitas ({@link #VIEW},
 * {@link #OPERATE}, {@link #APPROVE}, {@link #FINANCE}, {@link #AUDIT}, {@link #ADMIN}).
 *
 * <p>
 * Aturan pemeriksaan {@link #require(SocialRequestContext, String)}: pengguna harus sudah login;
 * admin sistem ({@code Common#getApakahAdmin()}) selalu lolos tanpa pengecekan lebih lanjut;
 * selain itu, peran (role) pengguna dicocokkan terhadap daftar role yang diizinkan untuk
 * kapabilitas tersebut, dibaca dari konfigurasi {@code sosial_roles_<kapabilitas>} (huruf kecil,
 * dipisah koma). Bila tidak lolos salah satu syarat, method melempar {@link SecurityException}.
 * </p>
 */
public final class SocialPrivilegeGuard { public static final String VIEW="VIEW",OPERATE="OPERATE",APPROVE="APPROVE",FINANCE="FINANCE",AUDIT="AUDIT",ADMIN="ADMIN";
	/**
	 * Memastikan pengguna pada {@code c} sudah login dan memiliki salah satu role yang diizinkan
	 * untuk {@code capability}; melempar {@link SecurityException} bila tidak.
	 *
	 * @param c          konteks permintaan Portal Sosial, berisi status login dan data pengguna
	 * @param capability nama kapabilitas yang diperiksa (mis. {@link #VIEW}, {@link #APPROVE})
	 * @throws SecurityException bila pengguna belum login atau tidak memiliki role yang diizinkan
	 */
	public void require(SocialRequestContext c,String capability){if(c==null||!c.isAuthenticated())throw new SecurityException("Login diperlukan.");try{if(Common.getApakahAdmin())return;}catch(Exception ignored){}String allowed=config("sosial_roles_"+capability.toLowerCase());for(Tbmrole role:c.getUser().ambilRoles()){if(role!=null&&contains(allowed,role.getRoleId()))return;}throw new SecurityException("Hak akses Sosial tidak mencukupi.");} /** Membaca nilai konfigurasi berkunci {@code k}, atau string kosong bila tidak ada/gagal dibaca. */
 private String config(String k){try{return Common.getKonfigurasi(k,"").getNilai();}catch(Exception e){return "";}}
 /** Mengecek apakah {@code v} muncul (tanpa membedakan besar-kecil huruf) di antara nilai-nilai {@code csv} yang dipisah koma. */
 private boolean contains(String csv,String v){if(v==null)return false;for(String x:csv.split(","))if(v.equalsIgnoreCase(x.trim()))return true;return false;} }
