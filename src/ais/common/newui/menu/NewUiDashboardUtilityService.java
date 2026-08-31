package ais.common.newui.menu;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ais.action.master.helper.UserOnlineCounter;
import ais.common.Common;
import ais.common.ConstantValues;
import ais.common.SecurityFilter;
import ais.database.model.CustomerService;
import ais.database.model.OnlineUsers;

/** Data native untuk kontrol dashboard yang dahulu dirender langsung oleh MainAction. */
@SuppressWarnings({ "rawtypes", "unchecked" })
public final class NewUiDashboardUtilityService {
    private NewUiDashboardUtilityService() { }

    public static Number accessCount() {
        refreshCounters(); return UserOnlineCounter.count == null ? Integer.valueOf(0) : UserOnlineCounter.count;
    }

    public static Number onlineCount() {
        refreshCounters(); return UserOnlineCounter.countOnline == null
                ? Integer.valueOf(uniqueOnlineCount()) : UserOnlineCounter.countOnline;
    }

    public static List onlineUsers() {
        List result = new ArrayList(); Set names = new HashSet();
        try {
            Iterator iterator = new ArrayList(SecurityFilter.dataOnline.values()).iterator();
            while (iterator.hasNext()) {
                OnlineUsers online = (OnlineUsers) iterator.next();
                if (online == null) continue;
                String name = safeName(online);
                if (name.length() == 0 || !names.add(name)) continue;
                result.add(new OnlineUserInfo(name, role(online), unit(online), subUnit(online), login(online)));
            }
        } catch (Exception error) { ais.common.ErrorAuditUtil.record(error, "NewUiDashboardUtilityService.onlineUsers"); }
        Collections.sort(result, new Comparator() {
            public int compare(Object one, Object two) {
                return ((OnlineUserInfo) one).getName().compareToIgnoreCase(((OnlineUserInfo) two).getName());
            }
        });
        return result;
    }

    public static List customerServices() {
        List result = new ArrayList();
        try {
            Map values = ConstantValues.ambilBerdasarClass(CustomerService.class);
            Iterator iterator = values.values().iterator();
            while (iterator.hasNext()) {
                CustomerService service = (CustomerService) iterator.next();
                if (service == null || !Boolean.TRUE.equals(service.getAktif())) continue;
                String[] contacts = (service.getKeterangan() == null ? "" : service.getKeterangan()).split(",");
                for (int i = 0; i < contacts.length; i++) {
                    String raw = contacts[i] == null ? "" : contacts[i].trim(); if (raw.length() == 0) continue;
                    String[] pair = raw.split(":", 2); String phone = pair[0].trim();
                    String person = pair.length > 1 ? pair[1].trim() : "";
                    String international = normalizePhone(phone);
                    result.add(new CustomerContact(service.getNama(), person, phone, international));
                }
            }
        } catch (Exception error) { ais.common.ErrorAuditUtil.record(error, "NewUiDashboardUtilityService.customerServices"); }
        return result;
    }

    private static void refreshCounters() {
        try { if (UserOnlineCounter.count == null || UserOnlineCounter.countOnline == null) UserOnlineCounter.check(); }
        catch (Exception error) { ais.common.ErrorAuditUtil.record(error, "NewUiDashboardUtilityService.refreshCounters"); }
    }
    private static int uniqueOnlineCount() { try { return new HashSet(SecurityFilter.dataOnline.keySet()).size(); } catch (Exception ignored) { return 0; } }
    private static String safeName(OnlineUsers value) { try { return text(value.getNama()); } catch (Exception ignored) { return ""; } }
    private static String role(OnlineUsers value) {
        try {
            if (value.getMahasiswa() != null) return Common.getBahasa("label_mahasiswa");
            if (value.getSiswa() != null) return Common.getBahasa("label_siswa");
            if (value.getDosen() != null) return Common.getBahasa("label_dosen");
            return value.getTbmuser() == null || value.getTbmuser().hakAkses() == null ? "" : text(value.getTbmuser().hakAkses().getRoleName());
        } catch (Exception ignored) { return ""; }
    }
    private static String unit(OnlineUsers value) {
        try { if (value.getSekolah() != null) return text(value.getSekolah().getNama()); if (value.getFakultas() != null) return text(value.getFakultas().getNama()); }
        catch (Exception ignored) { }
        return "";
    }
    private static String subUnit(OnlineUsers value) { try { return value.getJurusan() == null ? "" : text(value.getJurusan().getNama()); } catch (Exception ignored) { return ""; } }
    private static java.util.Date login(OnlineUsers value) { try { return value.getLogin() == null ? null : value.getLogin().getLogin(); } catch (Exception ignored) { return null; } }
    private static String text(Object value) { return value == null ? "" : String.valueOf(value).trim(); }
    private static String normalizePhone(String phone) {
        String value = phone == null ? "" : phone.replaceAll("[^0-9+]", "");
        if (value.startsWith("08")) value = "+62" + value.substring(1);
        else if (value.startsWith("0")) value = "+62" + value.substring(1);
        else if (!value.startsWith("+")) value = "+62" + value;
        return value;
    }

    /**
     * Tipe implementasi bersarang {@link OnlineUserInfo} milik {@link NewUiDashboardUtilityService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiDashboardUtilityService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String name}, {@code String role},
     * {@code String unit}, {@code String subUnit}, {@code java.util.Date login}; operasi lokal: {@code getName()},
     * {@code getRole()}, {@code getUnit()}, {@code getSubUnit()}, {@code getLogin}(). Aturan bisnis bersama tetap
     * berada pada kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiDashboardUtilityService
     */
    public static final class OnlineUserInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String name, role, unit, subUnit; private final java.util.Date login;
        OnlineUserInfo(String name, String role, String unit, String subUnit, java.util.Date login) { this.name=name;this.role=role;this.unit=unit;this.subUnit=subUnit;this.login=login; }
        public String getName(){return name;} public String getRole(){return role;} public String getUnit(){return unit;} public String getSubUnit(){return subUnit;} public java.util.Date getLogin(){return login;}
    }

    /**
     * Tipe implementasi bersarang {@link CustomerContact} milik {@link NewUiDashboardUtilityService}. Kelas ini
     * memberi nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
     *
     * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
     * NewUiDashboardUtilityService}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman
     * digunakan dan diuji.</p>
     * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String group}, {@code String person},
     * {@code String phone}, {@code String internationalPhone}; operasi lokal: {@code getGroup()}, {@code
     * getPerson()}, {@code getPhone()}, {@code getInternationalPhone}(). Aturan bisnis bersama tetap berada pada
     * kelas induk atau service yang dipanggilnya.</p>
     * <p><b>Efek samping:</b> operasi dapat mengubah state lokal dan, sesuai nama methodnya, komponen UI atau
     * persistence melalui konteks kelas induk. Gunakan transaksi, otorisasi, dan session milik alur induk;
     * tambahkan perilaku lintas domain pada service bersama.</p>
     *
     * @see NewUiDashboardUtilityService
     */
    public static final class CustomerContact implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String group, person, phone, internationalPhone;
        CustomerContact(String group,String person,String phone,String internationalPhone){this.group=group;this.person=person;this.phone=phone;this.internationalPhone=internationalPhone;}
        public String getGroup(){return group;} public String getPerson(){return person;} public String getPhone(){return phone;} public String getInternationalPhone(){return internationalPhone;}
    }
}
