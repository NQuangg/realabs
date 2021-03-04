package com.vi.realabs.model;

import com.google.gson.annotations.SerializedName;

public class UserInfo{

	@SerializedName("sub")
	private String sub;

	@SerializedName("email_verified")
	private boolean emailVerified;

	@SerializedName("name")
	private String name;

	@SerializedName("given_name")
	private String givenName;

	@SerializedName("locale")
	private String locale;

	@SerializedName("family_name")
	private String familyName;

	@SerializedName("picture")
	private String picture;

	@SerializedName("email")
	private String email;

	public String getSub(){
		return sub;
	}

	public boolean isEmailVerified(){
		return emailVerified;
	}

	public String getName(){
		return name;
	}

	public String getGivenName(){
		return givenName;
	}

	public String getLocale(){
		return locale;
	}

	public String getFamilyName(){
		return familyName;
	}

	public String getPicture(){
		return picture;
	}

	public String getEmail(){
		return email;
	}
}