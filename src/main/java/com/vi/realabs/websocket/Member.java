package com.vi.realabs.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Member {
    private String userId;
    private String userName;
    private int step;
    private int tabs;
    private boolean online;

    public void increaseTabs() {
        tabs += 1;
    }

    public void decreaseTabs() {
        tabs -= 1;
    }
}
