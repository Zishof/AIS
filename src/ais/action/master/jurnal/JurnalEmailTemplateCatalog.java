package ais.action.master.jurnal;
import java.util.*;
/** 73 stable email keys verified against the pinned OJS registry. */
public final class JurnalEmailTemplateCatalog{
 private JurnalEmailTemplateCatalog(){}
 private static final String RAW="PASSWORD_RESET_CONFIRM USER_REGISTER USER_VALIDATE_CONTEXT USER_VALIDATE_SITE REVIEWER_REGISTER ISSUE_PUBLISH_NOTIFY SUBMISSION_ACK SUBMISSION_ACK_NOT_USER EDITOR_ASSIGN REVIEW_CANCEL REVIEW_REINSTATE REVIEW_RESEND_REQUEST REVIEW_REQUEST REVIEW_REQUEST_SUBSEQUENT REVIEW_RESPONSE_OVERDUE_AUTO REVIEW_CONFIRM REVIEW_DECLINE REVIEW_ACK REVIEW_REMIND REVIEW_REMIND_AUTO REVIEW_COMPLETE REVIEW_EDIT EDITOR_DECISION_ACCEPT EDITOR_DECISION_SEND_TO_EXTERNAL EDITOR_DECISION_SEND_TO_PRODUCTION EDITOR_DECISION_REVISIONS EDITOR_DECISION_RESUBMIT EDITOR_DECISION_DECLINE EDITOR_DECISION_INITIAL_DECLINE EDITOR_RECOMMENDATION EDITOR_DECISION_NOTIFY_OTHER_AUTHORS EDITOR_DECISION_NOTIFY_REVIEWERS EDITOR_DECISION_NEW_ROUND EDITOR_DECISION_REVERT_DECLINE EDITOR_DECISION_REVERT_INITIAL_DECLINE EDITOR_DECISION_SKIP_REVIEW EDITOR_DECISION_BACK_FROM_PRODUCTION EDITOR_DECISION_BACK_FROM_COPYEDITING EDITOR_DECISION_CANCEL_REVIEW_ROUND SUBSCRIPTION_NOTIFY OPEN_ACCESS_NOTIFY SUBSCRIPTION_BEFORE_EXPIRY SUBSCRIPTION_AFTER_EXPIRY SUBSCRIPTION_AFTER_EXPIRY_LAST SUBSCRIPTION_PURCHASE_INDL SUBSCRIPTION_PURCHASE_INSTL SUBSCRIPTION_RENEW_INDL SUBSCRIPTION_RENEW_INSTL REVISED_VERSION_NOTIFY STATISTICS_REPORT_NOTIFICATION ANNOUNCEMENT DISCUSSION_NOTIFICATION_SUBMISSION DISCUSSION_NOTIFICATION_REVIEW DISCUSSION_NOTIFICATION_COPYEDITING DISCUSSION_NOTIFICATION_PRODUCTION COPYEDIT_REQUEST EDITOR_ASSIGN_SUBMISSION EDITOR_ASSIGN_REVIEW EDITOR_ASSIGN_PRODUCTION LAYOUT_REQUEST LAYOUT_COMPLETE VERSION_CREATED EDITORIAL_REMINDER SUBMISSION_SAVED_FOR_LATER SUBMISSION_NEEDS_EDITOR PAYMENT_REQUEST_NOTIFICATION CHANGE_EMAIL ORCID_COLLECT_AUTHOR_ID ORCID_REQUEST_AUTHOR_AUTHORIZATION USER_ROLE_ASSIGNMENT_INVITATION USER_ROLE_END USER_ROLE_MASTHEAD_UPDATE ORCID_REQUEST_UPDATE_SCOPE";
 public static final List<String> KEYS=Collections.unmodifiableList(Arrays.asList(RAW.split(" ")));
 public static final Set<String> STANDARD_VARIABLES=Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList("recipientName","journalName","submissionTitle","actionUrl")));
 /**
  * Tipe implementasi bersarang {@link Definition} milik {@link JurnalEmailTemplateCatalog}. Kelas ini memberi
  * nama pada state atau perilaku lokal agar tanggung jawabnya tidak tersebar sebagai blok anonim.
  *
  * <p><b>Scope:</b> tipe bersifat {@code static}; instance tidak menangkap object {@link
  * JurnalEmailTemplateCatalog}. Dependensi yang diperlukan harus diberikan secara eksplisit agar aman digunakan
  * dan diuji.</p>
  * <p>Kontrak yang tampak dari deklarasi ini meliputi state utama: {@code String key}, {@code String locale},
  * {@code String subject}, {@code String body}. Aturan bisnis bersama tetap berada pada kelas induk atau
  * service yang dipanggilnya.</p>
  *
  * @see JurnalEmailTemplateCatalog
  */
 public static final class Definition{public final String key,locale,subject,body;private Definition(String k,String l,String s,String b){key=k;locale=l;subject=s;body=b;}}
 public static boolean contains(String key){return key!=null&&KEYS.contains(key.trim());}
 /** AIS-native safe defaults; journal managers may create later immutable versions. */
 public static Definition definition(String key,String locale){if(!contains(key))throw new IllegalArgumentException("Key template tidak dikenal.");String l=locale==null?"":locale.trim();if(!"id_ID".equals(l)&&!"en_US".equals(l))throw new IllegalArgumentException("Default hanya tersedia untuk id_ID/en_US.");String label=label(key);if("id_ID".equals(l))return new Definition(key,l,"{{journalName}} — "+label,"Halo {{recipientName}},\n\nAda pembaruan "+label.toLowerCase(Locale.ENGLISH)+" untuk naskah {{submissionTitle}}.\n\nBuka {{actionUrl}} untuk melihat rincian.\n\nSalam,\n{{journalName}}");return new Definition(key,l,"{{journalName}} — "+label,"Hello {{recipientName}},\n\nThere is an update regarding "+label.toLowerCase(Locale.ENGLISH)+" for submission {{submissionTitle}}.\n\nOpen {{actionUrl}} to view the details.\n\nRegards,\n{{journalName}}");}
 public static List<Definition> definitions(){List<Definition>x=new ArrayList<Definition>(KEYS.size()*2);for(String k:KEYS){x.add(definition(k,"id_ID"));x.add(definition(k,"en_US"));}return Collections.unmodifiableList(x);}
 private static String label(String key){StringBuilder b=new StringBuilder();for(String p:key.toLowerCase(Locale.ENGLISH).split("_")){if(b.length()>0)b.append(' ');b.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));}return b.toString();}
}
