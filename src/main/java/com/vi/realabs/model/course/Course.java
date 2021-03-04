package com.vi.realabs.model.course;

import com.google.gson.annotations.SerializedName;

public class Course {

	@SerializedName("name")
	private String name;

	@SerializedName("section")
	private String section;

	@SerializedName("updateTime")
	private String updateTime;

	@SerializedName("alternateLink")
	private String alternateLink;

	@SerializedName("id")
	private String id;

	@SerializedName("enrollmentCode")
	private String enrollmentCode;

	@SerializedName("room")
	private String room;

	public String getName(){
		return name;
	}

	public String getSection(){
		return section;
	}

	public String getUpdateTime(){
		return updateTime;
	}

	public String getAlternateLink(){
		return alternateLink;
	}

	public String getId(){
		return id;
	}

	public String getEnrollmentCode(){
		return enrollmentCode;
	}

	public String getRoom(){
		return room;
	}
}