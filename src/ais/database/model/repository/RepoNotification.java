package ais.database.model.repository;

import static javax.persistence.GenerationType.IDENTITY;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/** In-app repository notification; external mail remains an optional adapter. */
@Entity @org.hibernate.annotations.Entity(dynamicInsert=true,dynamicUpdate=true)
@Table(schema="public",name="repo_notification")
public class RepoNotification implements Serializable {
    private static final long serialVersionUID=1L;
    private Long id,itemId; private String recipientId,recipientRole,type,message; private Date readAt,createdAt;
    @Id @GeneratedValue(strategy=IDENTITY) @Column(name="id",insertable=false,nullable=false) public Long getId(){return id;} public void setId(Long v){id=v;}
    @Column(name="item_id",nullable=false) public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
    @Column(name="recipient_id",length=255) public String getRecipientId(){return recipientId;} public void setRecipientId(String v){recipientId=v;}
    @Column(name="recipient_role",length=60) public String getRecipientRole(){return recipientRole;} public void setRecipientRole(String v){recipientRole=v;}
    @Column(name="type",nullable=false,length=40) public String getType(){return type;} public void setType(String v){type=v;}
    @Column(name="message",nullable=false,length=1000) public String getMessage(){return message;} public void setMessage(String v){message=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="read_at") public Date getReadAt(){return readAt;} public void setReadAt(Date v){readAt=v;}
    @Temporal(TemporalType.TIMESTAMP) @Column(name="created_at",nullable=false) public Date getCreatedAt(){return createdAt;} public void setCreatedAt(Date v){createdAt=v;}
}
