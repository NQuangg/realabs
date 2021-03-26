package com.vi.realabs.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Chat {
    private String userId;
    private String userImage;
    private String message;
}
