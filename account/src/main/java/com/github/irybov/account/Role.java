package com.github.irybov.account;

public enum Role {
	ADMIN("ADMIN"), CLIENT("CLIENT");
	private final String name;
	Role(String name){this.name = name;}
	public String getName() {return "ROLE_" + name;}
}
