package com.vi.realabs.model;

import com.google.gson.annotations.SerializedName;

public class UserInfo {

	@SerializedName("name")
	private String name;

	@SerializedName("id")
	private String id;

	@SerializedName("verified_email")
	private boolean verifiedEmail;

	@SerializedName("given_name")
	private String givenName;

	@SerializedName("locale")
	private String locale;

	@SerializedName("family_name")
	private String familyName;

	@SerializedName("email")
	private String email;

	@SerializedName("picture")
	private String picture;

	public String getName(){
		return name;
	}

	public String getId(){
		return id;
	}

	public boolean isVerifiedEmail(){
		return verifiedEmail;
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

	public String getEmail(){
		return email;
	}

	public String getPicture(){
		return picture;
	}
}