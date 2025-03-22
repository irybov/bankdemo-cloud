package com.github.irybov.shared;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigDecimal;
import java.sql.Timestamp;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode.CacheStrategy;

@Getter
@Setter
@EqualsAndHashCode(of = "id", cacheStrategy = CacheStrategy.NEVER)
public class BillDTO implements Externalizable {
	
	private static final long serialVersionUID = 1L;
	
	private Integer id;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	@JsonProperty(value="isActive")
	private boolean isActive;
	private BigDecimal balance;
	private String currency;
	
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(id);
		out.writeObject(createdAt);
		out.writeObject(updatedAt);
		out.writeBoolean(isActive);
		out.writeObject(balance);
		out.writeUTF(currency);
	}
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		this.id = in.readInt();
		this.createdAt = (Timestamp) in.readObject();
		this.updatedAt = (Timestamp) in.readObject();
		this.isActive = in.readBoolean();
		this.balance = (BigDecimal) in.readObject();
		this.currency = in.readUTF();
	}

}
